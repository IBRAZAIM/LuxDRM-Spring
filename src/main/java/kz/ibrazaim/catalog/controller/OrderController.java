package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.Order;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {
    private final UserService userService;
    private final CartService cartService;
    private final OrderService orderService;
    private final CardService cardService;

    @GetMapping("/checkout")
    public String getCheckout(Model model) {
        User user = userService.getUser();
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("cartItemsCount", cartService.getCartItemsCountByUser(user));
        List<CartItem> cartItems = userService.findCartItemsByUser(user);
        model.addAttribute("orderItems", cartItems);
        int subtotalPrice = cartService.subtotal(cartItems);
        int discountAmount = cartService.discountAmount(cartItems);
        int discountPercent = cartService.discountPercent(cartItems);
        int finalPrice = cartService.finalPrice(cartItems);
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
            @RequestParam(required = false, defaultValue = "") String expiryYear,
            @RequestParam(required = false, defaultValue = "") String cvv
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
            cardService.saveCard(user, cardNumber, cardHolderName, expiryMonth, expiryYear, cvv);
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

        List<Order> orders;
        if (userService.hasAnyRole(user, Role.ADMIN, Role.MODER)) {
            orders = orderService.getAllOrders();
            model.addAttribute("orders", orders);
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
            @RequestParam("status") String status
    ) {
        User user = userService.getUser();
        if (user == null || !userService.hasAnyRole(user, Role.ADMIN, Role.MODER)) {
            return "redirect:/login";
        }

        orderService.updateOrderStatus(orderId, status);
        return "redirect:/orders";
    }

    @GetMapping("/orders/{id}")
    public String viewOrder(Model model, @PathVariable("id") long id) {
        User user = userService.getUser();
        if (user == null) {
            return "redirect:/login";
        }

        Order order = orderService.getOrderById(id);
        if (order == null || order.getUser().getId() != user.getId()) {
            return "redirect:/orders";
        }

        model.addAttribute("order", order);
        int cartItemsCount = cartService.getCartItemsCountByUser(user);
        model.addAttribute("cartItemsCount", cartItemsCount);
        return "orderPage";
    }
}
