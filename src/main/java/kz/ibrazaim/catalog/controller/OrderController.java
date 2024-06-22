package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.Order;
import kz.ibrazaim.catalog.model.Role;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.CartService;
import kz.ibrazaim.catalog.service.OrderService;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {
    private final UserService userService;
    private final CartService cartService;
    private final OrderService orderService;

    @GetMapping("/checkout")
    public String getCheckout(Model model) {
        User user = userService.getUser();
        model.addAttribute("user",user);
        List<CartItem> cartItems = userService.findCartItemsByUser(user);
        model.addAttribute("orderItems", cartItems);
        int totalPrice = cartService.returnTotalPrice(cartItems);
        model.addAttribute("totalPrice", totalPrice);
        return "checkout";
    }

    @PostMapping("/checkout")
    public String processCheckout(@RequestParam("address") String address) {
        User user = userService.getUser();
        List<CartItem> cartItems = userService.findCartItemsByUser(user);
        orderService.create(user, address,cartItems);
        cartService.clearCart(user);
        return "redirect:/orders";
    }

    @GetMapping("/orders")
    public String showOrders(Model model) {
        User user = userService.getUser();
        List<Order> orders;

        if (user.getRole().equals(Role.ADMIN.getServiceName()) || user.getRole().equals(Role.MODER.getServiceName())) {
            orders = orderService.getAllOrders();
            model.addAttribute("orders", orders);
            return "orders";
        } else {
            orders = orderService.getOrdersForUser(user);
            model.addAttribute("orders", orders);
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
}

