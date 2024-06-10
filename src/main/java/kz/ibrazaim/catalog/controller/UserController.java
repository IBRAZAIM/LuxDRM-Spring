package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.service.ProductService;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class UserController {
    private final UserService userService;
    private final ProductService productService;

    @GetMapping("/cart")
    public String getCartPage(Model model){
        model.addAttribute("cartItems", userService.findAllCartItems());
        return "cart";
    }

    @PostMapping("/cart/{productId}")
    public String addItemToCart(@PathVariable long productId){
        Product product = productService.findById(productId);
        if (userService.isProductInCart(productId)) {
            userService.updateCartItem(productId);
        } else {
            userService.addItemToCart(productId);
        }
        return "redirect:/cart";
    }

}


