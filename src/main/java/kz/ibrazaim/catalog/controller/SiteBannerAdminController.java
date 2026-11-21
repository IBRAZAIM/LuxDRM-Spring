package kz.ibrazaim.catalog.controller;

import jakarta.servlet.http.HttpSession;
import kz.ibrazaim.catalog.model.Role;
import kz.ibrazaim.catalog.model.SiteBanner;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.CartService;
import kz.ibrazaim.catalog.service.SiteBannerService;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/banners")
public class SiteBannerAdminController {

    private final SiteBannerService siteBannerService;
    private final UserService userService;
    private final CartService cartService;

    @GetMapping
    public String showBannersAdmin(Model model, HttpSession session) {
        User user = userService.getUser();
        if (user == null || !userService.hasAnyRole(user, Role.ADMIN, Role.MODER)) {
            return "redirect:/login";
        }
        model.addAttribute("heroBanner", siteBannerService.getHeroBanner());
        model.addAttribute("sectionBanners", siteBannerService.getSectionBanners());
        model.addAttribute("categoryBanners", siteBannerService.getCategoryBanners());
        model.addAttribute("cartItemsCount", cartService.getCartItemsCount(user, session));
        return "admin-banners";
    }

    @PostMapping("/{slotKey}")
    public String updateBanner(
            @PathVariable("slotKey") String slotKey,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "subtitle", required = false) String subtitle,
            @RequestParam(value = "targetUrl", required = false) String targetUrl,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.getUser();
        if (user == null || !userService.hasAnyRole(user, Role.ADMIN, Role.MODER)) {
            return "redirect:/login";
        }
        try {
            SiteBanner saved = siteBannerService.updateBanner(slotKey, title, subtitle, targetUrl, imageFile);
            redirectAttributes.addFlashAttribute("successMessage", "Блок «" + saved.getTitle() + "» успешно обновлён");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/banners";
    }

    @PostMapping("/{slotKey}/reset-image")
    public String resetBannerImage(
            @PathVariable("slotKey") String slotKey,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.getUser();
        if (user == null || !userService.hasAnyRole(user, Role.ADMIN, Role.MODER)) {
            return "redirect:/login";
        }
        try {
            SiteBanner saved = siteBannerService.resetBannerImage(slotKey);
            redirectAttributes.addFlashAttribute("successMessage", "Фон блока «" + saved.getTitle() + "» сброшен на стандартный");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/banners";
    }
}
