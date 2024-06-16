package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class UserController {
    private final UserService userService;

    @GetMapping("/cart")
    public String getCartPage(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        User user = userService.findUserByLogin(principal.getName());
        List<CartItem> cartItems = userService.findCartItemsByUser(user);
        model.addAttribute("cartItems", cartItems);
        return "cart";
    }

    @PostMapping("/cart/{productId}")
    public String addItemToCart(Principal principal, @PathVariable long productId, @RequestParam("quantity") int quantity) {
        if (principal == null) {
            return "redirect:/login";
        }
        User user = userService.findUserByLogin(principal.getName());
        userService.addItemToCart(productId, quantity, user);
        userService.updateCartItem(user, productId, quantity);
        return "redirect:/cart";
    }
}


