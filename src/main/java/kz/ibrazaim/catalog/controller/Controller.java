package kz.ibrazaim.catalog.controller;

import jakarta.servlet.http.HttpSession;
import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.ProductAudience;
import kz.ibrazaim.catalog.model.ProductImage;
import kz.ibrazaim.catalog.model.SiteBanner;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.CartService;
import kz.ibrazaim.catalog.service.ProductService;
import kz.ibrazaim.catalog.service.SiteBannerService;
import kz.ibrazaim.catalog.service.UserService;
import kz.ibrazaim.catalog.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    private final SiteBannerService siteBannerService;

    @GetMapping
    public String getProducts(Model model, HttpSession session) {
        User user = userService.getUser();
        List<Product> products = productService.findAll();
        List<SiteBanner> sectionBanners = siteBannerService.getSectionBanners();
        List<SiteBanner> categoryBanners = siteBannerService.getCategoryBanners();

        Map<String, String> slotFallbackImages = new HashMap<>();
        Map<String, Long> slotModelCounts = new HashMap<>();
        populateBannerMeta(products, sectionBanners, categoryBanners, slotFallbackImages, slotModelCounts);

        List<Product> homeProducts = products.stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .limit(8)
                .toList();

        model.addAttribute("products", homeProducts);
        model.addAttribute("heroBanner", siteBannerService.getHeroBanner());
        model.addAttribute("sectionBanners", sectionBanners);
        model.addAttribute("categoryBanners", categoryBanners);
        model.addAttribute("slotFallbackImages", slotFallbackImages);
        model.addAttribute("slotModelCounts", slotModelCounts);
        int cartItemsCount = cartService.getCartItemsCount(user, session);
        model.addAttribute("cartItemsCount", cartItemsCount);
        return "LUXDRM";
    }

    private void populateBannerMeta(
            List<Product> products,
            List<SiteBanner> sectionBanners,
            List<SiteBanner> categoryBanners,
            Map<String, String> fallbackImages,
            Map<String, Long> modelCounts
    ) {
        if (products == null || products.isEmpty()) {
            return;
        }
        for (SiteBanner sb : sectionBanners) {
            if (sb == null || sb.getSlotKey() == null) continue;
            long count = 0;
            String firstImg = null;
            for (Product p : products) {
                if (matchesBannerSection(p, sb.getSlotKey())) {
                    count++;
                    if (firstImg == null) {
                        firstImg = extractFirstProductImage(p);
                    }
                }
            }
            modelCounts.put(sb.getSlotKey(), count);
            if (!sb.hasImage() && firstImg != null) {
                fallbackImages.put(sb.getSlotKey(), firstImg);
            }
        }
        for (SiteBanner cb : categoryBanners) {
            if (cb == null || cb.getSlotKey() == null) continue;
            String titleKey = cb.getTitle() != null ? cb.getTitle().trim().toLowerCase(Locale.ROOT) : "";
            long count = 0;
            String firstImg = null;
            for (Product p : products) {
                String catName = (p.getCategory() != null && p.getCategory().getName() != null)
                        ? p.getCategory().getName().trim().toLowerCase(Locale.ROOT)
                        : "";
                if (!titleKey.isEmpty() && !catName.isEmpty() && (catName.equals(titleKey) || catName.contains(titleKey))) {
                    count++;
                    if (firstImg == null) {
                        firstImg = extractFirstProductImage(p);
                    }
                }
            }
            modelCounts.put(cb.getSlotKey(), count);
            if (!cb.hasImage() && firstImg != null) {
                fallbackImages.put(cb.getSlotKey(), firstImg);
            }
        }
    }

    private boolean matchesBannerSection(Product p, String slotKey) {
        ProductAudience aud = p.getSafeAudience();
        return switch (slotKey) {
            case "section_men" -> aud == ProductAudience.MEN || aud == ProductAudience.UNISEX;
            case "section_women" -> aud == ProductAudience.WOMEN || aud == ProductAudience.UNISEX;
            case "section_kids" -> aud == ProductAudience.KIDS;
            default -> false;
        };
    }

    private String extractFirstProductImage(Product p) {
        if (p == null || p.getImages() == null || p.getImages().isEmpty()) {
            return null;
        }
        for (ProductImage img : p.getImages()) {
            if (img != null && img.getImage() != null && !img.getImage().isBlank()) {
                return img.getImage().trim();
            }
        }
        return null;
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
