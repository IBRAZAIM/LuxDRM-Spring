package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class UserController {
    private final UserService userService;

    @GetMapping("/cart")
    public String getCartPage(Model model){
        model.addAttribute("cartItems", userService.findAllCartItems());
        return "cart";
    }

    @PostMapping("/cart/{productId}")
    public String addItemToCart(Principal principal, @PathVariable long productId){
        if (principal == null) {
            return "redirect:/login";
        }
        if (userService.isProductInCart(productId)) {
            userService.updateCartItem(productId);
        } else {
            userService.addItemToCart(productId);
        }
        return "redirect:/cart";
    }

}


