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
    public String getCartPage(Model model, jakarta.servlet.http.HttpSession session) {
        User user = userService.getUser();
        List<CartItem> cartItems;
        if (user != null) {
            cartService.mergeGuestCartToUser(session, user);
            cartItems = userService.findCartItemsByUser(user);
        } else {
            cartItems = cartService.getGuestCartItems(session);
        }

        int cartItemsCount = cartItems.stream()
                .mapToInt(item -> Math.max(0, item.getQuantity()))
                .sum();
        int cartRowsCount = cartItems.size();
        BigDecimal subtotalPrice = cartService.subtotal(cartItems);
        BigDecimal discountAmount = cartService.discountAmount(cartItems);
        int discountPercent = subtotalPrice.compareTo(BigDecimal.ZERO) > 0
                ? discountAmount.multiply(BigDecimal.valueOf(100))
                        .divide(subtotalPrice, 0, java.math.RoundingMode.HALF_UP)
                        .intValue()
                : 0;
        BigDecimal finalPrice = subtotalPrice.subtract(discountAmount)
                .max(BigDecimal.ZERO)
                .setScale(2, java.math.RoundingMode.HALF_UP);

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
    public Object addItemToCart(
            @PathVariable long productId,
            @RequestParam("quantity") int quantity,
            @RequestParam("size") String size,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpSession session
    ) {
        User user = userService.getUser();
        if (user != null) {
            cartService.mergeGuestCartToUser(session, user);
            userService.addItemToCart(productId, quantity, size);
        } else {
            cartService.addGuestItem(session, productId, quantity, size);
        }

        int cartItemsCount = cartService.getCartItemsCount(user, session);
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.contains("application/json"))) {
            return org.springframework.http.ResponseEntity.ok(
                    java.util.Map.of("ok", true, "cartItemsCount", cartItemsCount)
            );
        }
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
