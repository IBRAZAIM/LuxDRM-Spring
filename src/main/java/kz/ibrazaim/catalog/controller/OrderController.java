package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.Order;
import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.OrderService;
import kz.ibrazaim.catalog.service.ProductService;
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
    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;

    @GetMapping("/checkout")
    public String getCheckoutForm(Model model) {
        List<CartItem> cartItems = productService.getAllProductsInCart(); // Получение списка элементов корзины
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("order", new Order());
        return "checkout";
    }


    @PostMapping("/checkout")
    public String createOrder(
            @RequestParam Long userId,
            @RequestParam String address,
            @RequestParam List<Long> productIds,
            @RequestParam List<Integer> quantities,
            Model model
    ) {
        User user = userService.findById(userId);
        if (user == null) {
            return "redirect:/exception";
        }

        List<Product> products = productService.findAllById(productIds);
        // Создание заказа
        orderService.createOrder(user, address, products, quantities);
        model.addAttribute("user", user);
        return "checkout";
    }







}

