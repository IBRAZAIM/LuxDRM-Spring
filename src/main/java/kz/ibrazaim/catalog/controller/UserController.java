package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.CartService;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
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
        int cartItemsCount = cartService.getCartItemsCountByUser(user);
        int cartRowsCount = cartItems.size();
        BigDecimal subtotalPrice = cartService.subtotal(cartItems);
        BigDecimal discountAmount = cartService.discountAmount(cartItems);
        int discountPercent = cartService.discountPercent(cartItems);
        BigDecimal finalPrice = cartService.finalPrice(cartItems);

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartItemsCount", cartItemsCount);
        model.addAttribute("cartRowsCount", cartRowsCount);
        model.addAttribute("cartQuantityTotal", cartItemsCount);
        model.addAttribute("subtotalPrice", subtotalPrice);
        model.addAttribute("totalPrice", subtotalPrice);
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("discountPercent", discountPercent);
        model.addAttribute("averageDiscount", discountPercent);
        model.addAttribute("finalPrice", finalPrice);
        model.addAttribute("cartItemsLabel", cartItemsCount + " " + pluralize(cartItemsCount, "товар", "товара", "товаров"));

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

    private String pluralize(int value, String one, String few, String many) {
        int absValue = Math.abs(value);
        int mod100 = absValue % 100;
        int mod10 = absValue % 10;

        if (mod100 >= 11 && mod100 <= 19) {
            return many;
        }
        if (mod10 == 1) {
            return one;
        }
        if (mod10 >= 2 && mod10 <= 4) {
            return few;
        }
        return many;
    }
}
