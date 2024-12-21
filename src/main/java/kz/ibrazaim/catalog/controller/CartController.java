package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.repository.CartItemRepository;
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
    public String deleteItem(@RequestParam("id") Long itemId) {
        cartService.removeCartItem(itemId);
        return "redirect:/cart";
    }

    @PostMapping("/update/{cartItemId}")
    public String updateCartItem(
            @PathVariable Long cartItemId,
            @RequestParam("quantity") int quantity
    ) {
        cartService.updateQuantity(cartItemId, quantity);
        return "redirect:/cart";
    }

    // Функция удаления всех товаров
    @PostMapping("/clear")
    public String clearCart(){
        User user = userService.getUser();
        List<CartItem> cartItems = userService.findCartItemsByUser(user);
        if (!cartItems.isEmpty()){
            cartService.clearCart(user);
        }
        return "redirect:/cart";
    }
}

