package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.CartService;
import kz.ibrazaim.catalog.service.ProductService;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@org.springframework.stereotype.Controller
@RequiredArgsConstructor
@RequestMapping("/main")
public class Controller {
    private final ProductService productService;
    private final CartService cartService;
    private final UserService userService;
    @GetMapping
    public String getProducts(Model model) {
        User user = userService.getUser();
        List<Product> products = productService.findAll();
        model.addAttribute("products", products);
        int cartItemsCount = cartService.getCartItemsCountByUser(user); // Метод для получения количества товаров в корзине
        model.addAttribute("cartItemsCount", cartItemsCount); // Передаем количество в модель
        return "LUXDRM";
    }
}
