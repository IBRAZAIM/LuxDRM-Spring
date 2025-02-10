package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.service.CartService;
import kz.ibrazaim.catalog.service.ProductService;
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
    @GetMapping
    public String getProducts(Model model) {
        List<Product> products = productService.findAll();
        model.addAttribute("products", products);
        int cartItemsCount = cartService.getCartItemsCount(); // Метод для получения количества товаров в корзине
        model.addAttribute("cartItemsCount", cartItemsCount); // Передаем количество в модель
        return "LUXDRM";
    }
}
