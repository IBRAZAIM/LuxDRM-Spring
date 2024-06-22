package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class UserController {
    private final UserService userService;

    @GetMapping("/cart")
    public String getCartPage(Model model) {
        User user = userService.getUser();
        if (user == null) {
            return "redirect:/login";
        }
        List<CartItem> cartItems = userService.findCartItemsByUser(user);
        model.addAttribute("cartItems", cartItems);
        return "cart";
    }

    @PostMapping("/cart/{productId}")
    public String addItemToCart(@PathVariable long productId, @RequestParam("quantity") int quantity) {
        User user = userService.getUser();
        if (user == null) {
            return "redirect:/login";
        }
        userService.addItemToCart(productId, quantity);
        return "redirect:/cart";
    }
}


