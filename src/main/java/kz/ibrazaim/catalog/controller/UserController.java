package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.CartService;
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
    private final CartService cartService;

    @GetMapping("/cart")
    public String getCartPage(Model model) {
        User user = userService.getUser();
        if (user == null) {
            return "redirect:/login";
        }
        List<CartItem> cartItems = userService.findCartItemsByUser(user);
        model.addAttribute("cartItems", cartItems);
        double averageDiscount = cartItems.stream()
                .mapToInt(cartItem -> cartItem.getProduct().getDiscount())
                .average()
                .orElse(0.0); // Если список пуст, вернуть 0.0
        model.addAttribute("averageDiscount", Math.round(averageDiscount)); // Округление
        model.addAttribute("totalPrice", cartService.totalPrice(cartItems));
        int totalPrice = cartService.totalPrice(cartItems);  // Получаем полную цену товаров
        int finalPrice = (int)(totalPrice * (1 - averageDiscount / 100));  // Применяем скидку
        model.addAttribute("finalPrice", finalPrice);

        return "cart";
    }

    @PostMapping("/cart/{productId}")
    public String addItemToCart(
            @PathVariable long productId,
            @RequestParam("quantity") int quantity,
            @RequestParam("size") String size
    ) {
        User user = userService.getUser();
        if (user == null) {
            return "redirect:/login";
        }
        userService.addItemToCart(productId, quantity, size);
        return "redirect:/cart";
    }
}


