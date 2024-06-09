package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.Order;
import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.OrderService;
import kz.ibrazaim.catalog.service.ProductService;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class OrderController {
    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;

    @PostMapping("/checkout")
    public String createOrder(@RequestParam Long productId,
                              @RequestParam Long userId,
                              @RequestParam String address,
                              @RequestParam int quantity,
                              Model model
    ) {
        User user = userService.findById(userId);
        Product product = productService.findById(productId);
        Order order = orderService.createOrder(user, address, quantity, List.of(product));
        model.addAttribute("user", user);
        model.addAttribute("order", order);
        return "checkout";
    }
}

