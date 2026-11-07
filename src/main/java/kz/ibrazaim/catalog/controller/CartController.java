package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.CartService;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Controller
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {
    private final CartService cartService;
    private final UserService userService;

    @PostMapping("/delete")
    public String deleteItem(@RequestParam("id") Long itemId, jakarta.servlet.http.HttpSession session) {
        User user = userService.getUser();
        if (user != null) {
            cartService.removeCartItem(user, itemId);
        } else {
            cartService.removeGuestItem(session, itemId);
        }
        return "redirect:/cart";
    }

    @PostMapping("/update/{cartItemId}")
    public String updateCartItem(
            @PathVariable Long cartItemId,
            @RequestParam("quantity") int quantity,
            jakarta.servlet.http.HttpSession session
    ) {
        User user = userService.getUser();
        if (user != null) {
            cartService.updateQuantity(user, cartItemId, quantity);
        } else {
            cartService.updateGuestQuantity(session, cartItemId, quantity);
        }
        return "redirect:/cart";
    }

    // Функция удаления всех товаров
    @PostMapping("/clear")
    public String clearCart(jakarta.servlet.http.HttpSession session) {
        User user = userService.getUser();
        if (user != null) {
            List<CartItem> cartItems = userService.findCartItemsByUser(user);
            if (!cartItems.isEmpty()) {
                cartService.clearCart(user);
            }
        } else {
            cartService.clearGuestCart(session);
        }
        return "redirect:/cart";
    }
}


