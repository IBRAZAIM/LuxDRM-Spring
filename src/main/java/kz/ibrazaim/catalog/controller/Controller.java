package kz.ibrazaim.catalog.controller;

import jakarta.servlet.http.HttpSession;
import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.CartService;
import kz.ibrazaim.catalog.service.ProductService;
import kz.ibrazaim.catalog.service.UserService;
import kz.ibrazaim.catalog.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@org.springframework.stereotype.Controller
@RequiredArgsConstructor
@RequestMapping("/")
public class Controller {
    private final ProductService productService;
    private final CartService cartService;
    private final UserService userService;
    private final WishlistService wishlistService;

    @GetMapping
    public String getProducts(Model model, HttpSession session) {
        User user = userService.getUser();
        List<Product> products = productService.findAll();
        model.addAttribute("products", products);
        int cartItemsCount = cartService.getCartItemsCount(user, session);
        model.addAttribute("cartItemsCount", cartItemsCount);
        return "LUXDRM";
    }

    @GetMapping("/wishlist")
    public String getWishlist(Model model, HttpSession session) {
        User user = userService.getUser();
        if (user != null) {
            wishlistService.mergeGuestWishlistToUser(session, user);
        }
        List<Product> products = wishlistService.getWishlistProducts(user, session);
        model.addAttribute("products", products);
        int cartItemsCount = cartService.getCartItemsCount(user, session);
        model.addAttribute("cartItemsCount", cartItemsCount);
        return "wishlist";
    }

    @PostMapping("/wishlist/toggle/{productId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleWishlist(
            @PathVariable long productId,
            HttpSession session
    ) {
        User user = userService.getUser();
        boolean liked = wishlistService.toggleProduct(user, session, productId);
        List<String> ids = wishlistService.getWishlistProductIds(user, session)
                .stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "liked", liked,
                "ids", ids,
                "count", ids.size()
        ));
    }

    @PostMapping("/wishlist/clear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clearWishlist(HttpSession session) {
        User user = userService.getUser();
        wishlistService.clearWishlist(user, session);
        return ResponseEntity.ok(Map.of(
                "ids", List.of(),
                "count", 0
        ));
    }
}
