package kz.ibrazaim.catalog.controller;

import jakarta.servlet.http.HttpSession;
import kz.ibrazaim.catalog.model.Role;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.CartService;
import kz.ibrazaim.catalog.service.PromoCodeService;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@RequestMapping("/promos")
public class PromoCodeAdminController {

    private final PromoCodeService promoCodeService;
    private final UserService userService;
    private final CartService cartService;

    @GetMapping
    public String listPromos(Model model, HttpSession session) {
        User user = userService.getUser();
        if (user == null || !userService.hasAnyRole(user, Role.ADMIN, Role.MODER)) {
            return "redirect:/login";
        }
        var promos = promoCodeService.findAllPromos();
        long activeCount = promos.stream().filter(p -> p.isActive()).count();
        long inactiveCount = promos.size() - activeCount;
        long totalUses = promos.stream().mapToLong(p -> Math.max(0, p.getUsedCount())).sum();

        model.addAttribute("promos", promos);
        model.addAttribute("activePromosCount", activeCount);
        model.addAttribute("inactivePromosCount", inactiveCount);
        model.addAttribute("totalPromoUses", totalUses);
        model.addAttribute("cartItemsCount", cartService.getCartItemsCount(user, session));
        return "promos";
    }

    @PostMapping("/save")
    public String savePromo(
            @RequestParam("code") String code,
            @RequestParam("discountPercent") int discountPercent,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "maxUses", required = false) Integer maxUses,
            @RequestParam(value = "minOrderAmount", required = false) BigDecimal minOrderAmount,
            @RequestParam(value = "expiresAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresAt,
            @RequestParam(value = "active", defaultValue = "false") boolean active,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.getUser();
        if (user == null || !userService.hasAnyRole(user, Role.ADMIN, Role.MODER)) {
            return "redirect:/login";
        }
        try {
            promoCodeService.createOrUpdatePromo(code, discountPercent, description, maxUses, minOrderAmount, expiresAt, active);
            redirectAttributes.addFlashAttribute("successMessage", "Промокод успешно сохранён.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/promos";
    }

    @PostMapping("/{id}/toggle")
    public String togglePromo(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.getUser();
        if (user == null || !userService.hasAnyRole(user, Role.ADMIN, Role.MODER)) {
            return "redirect:/login";
        }
        try {
            promoCodeService.togglePromo(id);
            redirectAttributes.addFlashAttribute("successMessage", "Статус промокода изменён.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/promos";
    }

    @PostMapping("/{id}/delete")
    public String deletePromo(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.getUser();
        if (user == null || !userService.hasAnyRole(user, Role.ADMIN, Role.MODER)) {
            return "redirect:/login";
        }
        promoCodeService.deletePromo(id);
        redirectAttributes.addFlashAttribute("successMessage", "Промокод удалён.");
        return "redirect:/promos";
    }
}
