package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.*;
import kz.ibrazaim.catalog.service.CartService;
import kz.ibrazaim.catalog.service.OrderProductService;
import kz.ibrazaim.catalog.service.OrderService;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {
    private final UserService userService;
    private final CartService cartService;
    private final OrderService orderService;
    @GetMapping("/checkout")
    public String getCheckout(
            Principal principal,
            Model model
    ){
        User user = userService.findUserByLogin(principal.getName());
        model.addAttribute("user",user);
        List<CartItem> cartItems = userService.findALlCartItems();
        model.addAttribute("orderItems", cartItems);
        int totalPrice = cartService.returnTotalPrice(cartItems);
        model.addAttribute("totalPrice", totalPrice);
        return "checkout";
    }

    @PostMapping("/checkout")
    public String processCheckout(
            Principal principal,
            @RequestParam("address") String address
    ){
        User user = userService.findUserByLogin(principal.getName());
        List<CartItem> cartItems = userService.findALlCartItems();
        orderService.create(user, address,cartItems);
        cartService.clearCart();
        return "redirect:/myOrders";
    }

    @GetMapping("/orders")
    public String showOrders(Principal principal, Model model) {
        User user = userService.findUserByLogin(principal.getName());
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
    public String updateOrderStatus(@RequestParam("orderId") Long orderId, @RequestParam("status") String status) {
        orderService.updateOrderStatus(orderId, status);
        return "redirect:/orders";
    }



}

