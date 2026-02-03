package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.*;
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
        model.addAttribute("user",user);
        List<CartItem> cartItems = userService.findCartItemsByUser(user);
        model.addAttribute("orderItems", cartItems);
        int totalPrice = cartService.totalPrice(cartItems);
        model.addAttribute("totalPrice", totalPrice);
        return "checkout";
    }

    @PostMapping("/checkout")
    public String processCheckout(
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("fullName") String fullName,
            @RequestParam("country") String country,
            @RequestParam("city") String city,
            @RequestParam("address") String address,
            @RequestParam("postalCode") String postalCode,
            @RequestParam("totalPrice") int totalPrice,
            @RequestParam String cardNumber,
            @RequestParam String cardHolderName,
            @RequestParam String expiryMonth,
            @RequestParam String expiryYear,
            @RequestParam String cvv
    ) {
        User user = userService.getUser();
        List<CartItem> cartItems = userService.findCartItemsByUser(user);
        orderService.create(user, email, phone, fullName, country, city, address, postalCode, cartItems, totalPrice);
        cardService.saveCard(user, cardNumber, cardHolderName, expiryMonth, expiryYear, cvv);
        cartService.clearCart(user);
        return "redirect:/orders";
    }

    @GetMapping("/orders")
    public String showOrders(Model model) {
        User user = userService.getUser();
        int cartItemsCount = cartService.getCartItemsCountByUser(user); // Метод для получения количества товаров в корзине
        model.addAttribute("cartItemsCount", cartItemsCount);
        // Проверка на авторизованного пользователя
        if (user == null) {
            System.out.println("Пользователь не авторизован, перенаправление на страницу входа.");
            return "redirect:/login";  // Перенаправление на страницу входа, если пользователь не авторизован
        }

        List<Order> orders;

        // Если роль пользователя - ADMIN или MODER, показываем все заказы
        if (user.getRole().equals(Role.ADMIN.getServiceName()) || user.getRole().equals(Role.MODER.getServiceName())) {
            orders = orderService.getAllOrders();
            model.addAttribute("orders", orders);
            return "orders";
        } else {
            // Для обычных пользователей показываем только его заказы
            orders = orderService.getOrdersForUser(user);
            model.addAttribute("orders", orders);

            // Если у пользователя нет заказов, можно показать сообщение
            if (orders.isEmpty()) {
                model.addAttribute("message", "У вас нет заказов.");
            }

            return "myOrders";
        }
    }

    @PostMapping("/orders/updateStatus")
    public String updateOrderStatus(
            @RequestParam("orderId") Long orderId,
            @RequestParam("status") String status
    ) {
        orderService.updateOrderStatus(orderId, status);
        return "redirect:/orders";
    }

    @GetMapping("/orders/{id}")
    public String viewOrder(Model model, @PathVariable long id) {
        User user = userService.getUser();

        // Проверка на авторизованного пользователя
        if (user == null) {
            return "redirect:/login";  // Перенаправление на страницу входа, если пользователь не авторизован
        }

        Order order = orderService.getOrderById(id);//Находим order;

        if (order == null || order.getUser().getId() != user.getId()) {
            return "redirect:/orders";  // Перенаправление на список заказов
        }

        model.addAttribute("order", order);
        int cartItemsCount = cartService.getCartItemsCountByUser(user); // Метод для получения количества товаров в корзине
        model.addAttribute("cartItemsCount", cartItemsCount);
        return "orderPage";
    }
}

