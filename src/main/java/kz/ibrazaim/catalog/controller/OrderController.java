package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.Order;
import kz.ibrazaim.catalog.model.OrderStatus;
import kz.ibrazaim.catalog.model.Role;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.CardService;
import kz.ibrazaim.catalog.service.CartService;
import kz.ibrazaim.catalog.service.OrderService;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {
    private final UserService userService;
    private final CartService cartService;
    private final OrderService orderService;
    private final CardService cardService;

    @GetMapping("/checkout")
    public String getCheckout(Model model, jakarta.servlet.http.HttpSession session) {
        User user = userService.getUser();
        if (user == null) {
            return "redirect:/login";
        }
        cartService.mergeGuestCartToUser(session, user);
        model.addAttribute("user", user);
        List<CartItem> cartItems = userService.findCartItemsByUser(user);
        int cartItemsCount = cartItems.stream()
                .mapToInt(item -> Math.max(0, item.getQuantity()))
                .sum();
        model.addAttribute("cartItemsCount", cartItemsCount);
        model.addAttribute("orderItems", cartItems);
        BigDecimal subtotalPrice = cartService.subtotal(cartItems);
        BigDecimal discountAmount = cartService.discountAmount(cartItems);
        int discountPercent = subtotalPrice.compareTo(BigDecimal.ZERO) > 0
                ? discountAmount.multiply(BigDecimal.valueOf(100))
                        .divide(subtotalPrice, 0, java.math.RoundingMode.HALF_UP)
                        .intValue()
                : 0;
        BigDecimal finalPrice = subtotalPrice.subtract(discountAmount)
                .max(BigDecimal.ZERO)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        model.addAttribute("subtotalPrice", subtotalPrice);
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("discountPercent", discountPercent);
        model.addAttribute("totalPrice", finalPrice);
        model.addAttribute("finalPrice", finalPrice);
        return "checkout";
    }

    @PostMapping("/checkout")
    @Transactional
    public String processCheckout(
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("fullName") String fullName,
            @RequestParam("country") String country,
            @RequestParam("city") String city,
            @RequestParam("address") String address,
            @RequestParam("postalCode") String postalCode,
            @RequestParam(value = "delivery", required = false, defaultValue = "standard") String deliveryMethod,
            @RequestParam(value = "paymentMethod", required = false, defaultValue = "card") String paymentMethod,
            @RequestParam(required = false, defaultValue = "") String cardNumber,
            @RequestParam(required = false, defaultValue = "") String cardHolderName,
            @RequestParam(required = false, defaultValue = "") String expiryMonth,
            @RequestParam(required = false, defaultValue = "") String expiryYear
    ) {
        User user = userService.getUser();
        if (user == null) {
            return "redirect:/login";
        }

        List<CartItem> cartItems = userService.findCartItemsByUser(user);
        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        orderService.create(user, email, phone, fullName, country, city, address, postalCode, deliveryMethod, paymentMethod, cartItems);
        if ("card".equals(paymentMethod)) {
            cardService.saveCard(user, cardNumber, cardHolderName, expiryMonth, expiryYear);
        }
        cartService.clearCart(user);
        return "redirect:/orders";
    }

    @GetMapping("/orders")
    public String showOrders(Model model) {
        User user = userService.getUser();
        if (user == null) {
            return "redirect:/login";
        }

        int cartItemsCount = cartService.getCartItemsCountByUser(user);
        model.addAttribute("cartItemsCount", cartItemsCount);
        model.addAttribute("user", user);

        List<Order> orders;
        if (userService.hasAnyRole(user, Role.ADMIN, Role.MODER)) {
            orders = orderService.getAllOrders();
            long pendingCount = orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
            long shippedCount = orders.stream().filter(o -> o.getStatus() == OrderStatus.SHIPPED).count();
            long deliveredCount = orders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();
            long canceledCount = orders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELED).count();
            BigDecimal totalRevenue = orders.stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELED && o.getTotalPrice() != null)
                    .map(Order::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("orders", orders);
            model.addAttribute("totalOrdersCount", orders.size());
            model.addAttribute("pendingCount", pendingCount);
            model.addAttribute("shippedCount", shippedCount);
            model.addAttribute("deliveredCount", deliveredCount);
            model.addAttribute("canceledCount", canceledCount);
            model.addAttribute("totalRevenue", totalRevenue);
            return "orders";
        }

        orders = orderService.getOrdersForUser(user);
        model.addAttribute("orders", orders);

        if (orders.isEmpty()) {
            model.addAttribute("message", "У вас нет заказов.");
        }

        return "myOrders";
    }

    @PostMapping("/orders/updateStatus")
    public String updateOrderStatus(
            @RequestParam("orderId") Long orderId,
            @RequestParam("status") String status,
            @RequestParam(value = "redirectTo", required = false) String redirectTo,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.getUser();
        if (user == null || !userService.hasAnyRole(user, Role.ADMIN, Role.MODER)) {
            return "redirect:/login";
        }

        try {
            orderService.updateOrderStatus(orderId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Статус заказа №00" + orderId + " успешно обновлён");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при обновлении статуса: " + ex.getMessage());
        }

        if (redirectTo != null && redirectTo.startsWith("/orders")) {
            return "redirect:" + redirectTo;
        }
        return "redirect:/orders";
    }

    @GetMapping("/orders/{id}")
    public String viewOrder(Model model, @PathVariable("id") long id) {
        User user = userService.getUser();
        if (user == null) {
            return "redirect:/login";
        }

        Order order = orderService.getOrderById(id);
        if (order == null) {
            return "redirect:/orders";
        }

        // Разрешаем просмотр только владельцу заказа или администратору/модератору
        boolean isOwner = order.getUser() != null && order.getUser().getId() == user.getId();
        boolean isStaff = userService.hasAnyRole(user, Role.ADMIN, Role.MODER);
        if (!isOwner && !isStaff) {
            return "redirect:/orders";
        }

        model.addAttribute("order", order);
        model.addAttribute("isStaff", isStaff);
        int cartItemsCount = cartService.getCartItemsCountByUser(user);
        model.addAttribute("cartItemsCount", cartItemsCount);
        return "orderPage";
    }
}
