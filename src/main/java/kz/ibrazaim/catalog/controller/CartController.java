package kz.ibrazaim.catalog.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.CartService;
import kz.ibrazaim.catalog.service.PromoCodeService;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {
    private final CartService cartService;
    private final UserService userService;
    private final PromoCodeService promoCodeService;

    @PostMapping("/delete")
    public Object deleteItem(
            @RequestParam("id") Long itemId,
            HttpServletRequest request,
            HttpSession session
    ) {
        User user = userService.getUser();
        if (user != null) {
            cartService.removeCartItem(user, itemId);
        } else {
            cartService.removeGuestItem(session, itemId);
        }
        if (isAjax(request)) {
            return ResponseEntity.ok(buildCartPayload(user, session, null));
        }
        return "redirect:/cart";
    }

    @PostMapping("/update/{cartItemId}")
    public Object updateCartItem(
            @PathVariable Long cartItemId,
            @RequestParam("quantity") int quantity,
            HttpServletRequest request,
            HttpSession session
    ) {
        User user = userService.getUser();
        if (user != null) {
            cartService.updateQuantity(user, cartItemId, quantity);
        } else {
            cartService.updateGuestQuantity(session, cartItemId, quantity);
        }
        if (isAjax(request)) {
            return ResponseEntity.ok(buildCartPayload(user, session, cartItemId));
        }
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(HttpSession session) {
        User user = userService.getUser();
        if (user != null) {
            List<CartItem> cartItems = userService.findCartItemsByUser(user);
            if (!cartItems.isEmpty()) {
                cartService.clearCart(user);
            }
        } else {
            cartService.clearGuestCart(session);
        }
        promoCodeService.clearPromo(session);
        return "redirect:/cart";
    }

    @PostMapping("/promo/apply")
    public Object applyPromoCode(
            @RequestParam("code") String code,
            @RequestParam(value = "redirectTo", required = false, defaultValue = "/cart") String redirectTo,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.getUser();
        PromoCodeService.PromoInfo promo = promoCodeService.applyPromo(session, code);

        if (isAjax(request)) {
            if (promo == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "ok", false,
                        "message", "Промокод не найден. Попробуйте LUX10, WELCOME15 или VIP20."
                ));
            }
            Map<String, Object> payload = buildCartPayload(user, session, null);
            payload.put("message", promo.getDescription());
            return ResponseEntity.ok(payload);
        }

        String target = resolveSafeRedirect(redirectTo);
        if (promo == null) {
            redirectAttributes.addFlashAttribute("promoError", "Промокод не найден. Доступны: LUX10, WELCOME15, VIP20.");
        } else {
            redirectAttributes.addFlashAttribute("promoSuccess", promo.getDescription() + " применён!");
        }
        return "redirect:" + target;
    }

    @PostMapping("/promo/remove")
    public Object removePromoCode(
            @RequestParam(value = "redirectTo", required = false, defaultValue = "/cart") String redirectTo,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.getUser();
        promoCodeService.clearPromo(session);

        if (isAjax(request)) {
            Map<String, Object> payload = buildCartPayload(user, session, null);
            payload.put("message", "Промокод отменён.");
            return ResponseEntity.ok(payload);
        }

        redirectAttributes.addFlashAttribute("promoSuccess", "Промокод отменён.");
        return "redirect:" + resolveSafeRedirect(redirectTo);
    }

    private Map<String, Object> buildCartPayload(User user, HttpSession session, Long updatedItemId) {
        List<CartItem> cartItems = (user != null)
                ? userService.findCartItemsByUser(user)
                : cartService.getGuestCartItems(session);

        if (cartItems.isEmpty()) {
            promoCodeService.clearPromo(session);
        }

        int cartItemsCount = cartItems.stream()
                .mapToInt(item -> Math.max(0, item.getQuantity()))
                .sum();
        BigDecimal subtotalPrice = cartService.subtotal(cartItems);
        BigDecimal productDiscount = cartService.discountAmount(cartItems);
        BigDecimal afterProductDiscount = subtotalPrice.subtract(productDiscount)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        int discountPercent = subtotalPrice.compareTo(BigDecimal.ZERO) > 0
                ? productDiscount.multiply(BigDecimal.valueOf(100))
                        .divide(subtotalPrice, 0, RoundingMode.HALF_UP)
                        .intValue()
                : 0;

        PromoCodeService.PromoInfo promo = promoCodeService.getActivePromo(session);
        BigDecimal promoDiscount = promoCodeService.calculatePromoDiscount(afterProductDiscount, promo);
        BigDecimal finalPrice = afterProductDiscount.subtract(promoDiscount)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ok", true);
        payload.put("empty", cartItems.isEmpty());
        payload.put("cartItemsCount", cartItemsCount);
        payload.put("cartRowsCount", cartItems.size());
        payload.put("cartItemsLabel", cartItemsCount + " " + pluralize(cartItemsCount));
        payload.put("subtotalPrice", subtotalPrice);
        payload.put("discountAmount", productDiscount);
        payload.put("discountPercent", discountPercent);
        payload.put("afterProductDiscount", afterProductDiscount);
        payload.put("promoCode", promo != null ? promo.getCode() : "");
        payload.put("promoPercent", promo != null ? promo.getPercent() : 0);
        payload.put("promoDiscount", promoDiscount);
        payload.put("finalPrice", finalPrice);

        if (updatedItemId != null) {
            cartItems.stream()
                    .filter(item -> updatedItemId.equals(item.getId()))
                    .findFirst()
                    .ifPresent(item -> {
                        payload.put("itemId", item.getId());
                        payload.put("itemQuantity", item.getQuantity());
                        payload.put("itemLineSubtotal", cartService.lineSubtotal(item));
                        payload.put("itemLineFinalPrice", cartService.lineFinalPrice(item));
                    });
        }
        return payload;
    }

    private boolean isAjax(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.contains("application/json"));
    }

    private String resolveSafeRedirect(String redirectTo) {
        if ("/checkout".equals(redirectTo)) {
            return "/checkout";
        }
        return "/cart";
    }

    private String pluralize(int count) {
        int abs = Math.abs(count);
        int mod100 = abs % 100;
        int mod10 = abs % 10;
        if (mod100 >= 11 && mod100 <= 14) {
            return "товаров";
        }
        if (mod10 == 1) {
            return "товар";
        }
        if (mod10 >= 2 && mod10 <= 4) {
            return "товара";
        }
        return "товаров";
    }
}
