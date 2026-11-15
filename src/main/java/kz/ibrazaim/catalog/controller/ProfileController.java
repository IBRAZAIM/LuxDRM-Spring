package kz.ibrazaim.catalog.controller;

import jakarta.servlet.http.HttpSession;
import kz.ibrazaim.catalog.model.Order;
import kz.ibrazaim.catalog.model.PaymentTransaction;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.CartService;
import kz.ibrazaim.catalog.service.OrderService;
import kz.ibrazaim.catalog.service.PaymentService;
import kz.ibrazaim.catalog.service.UserService;
import kz.ibrazaim.catalog.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final OrderService orderService;
    private final WishlistService wishlistService;
    private final CartService cartService;
    private final PaymentService paymentService;

    @GetMapping
    public String profilePage(Model model, HttpSession session) {
        User user = userService.getUser();
        if (user == null) {
            return "redirect:/login";
        }

        List<Order> allOrders = orderService.getOrdersForUser(user);
        List<Order> recentOrders = allOrders.size() > 5 ? allOrders.subList(0, 5) : allOrders;
        Map<Long, PaymentTransaction> paymentTxMap = paymentService.findLatestByOrders(recentOrders);

        int wishlistCount = wishlistService.getWishlistProductIds(user, session).size();
        int cartCount = cartService.getCartItemsCount(user, session);

        model.addAttribute("user", user);
        model.addAttribute("ordersCount", allOrders.size());
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("paymentTxMap", paymentTxMap);
        model.addAttribute("wishlistCount", wishlistCount);
        model.addAttribute("cartCount", cartCount);
        return "profile";
    }

    @PostMapping("/update")
    public String updateProfile(
            @RequestParam("name") String name,
            @RequestParam(value = "surname", required = false) String surname,
            @RequestParam("email") String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "country", required = false) String country,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "postalCode", required = false) String postalCode,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.getUser();
        if (user == null) {
            return "redirect:/login";
        }

        try {
            userService.updateProfile(user, name, surname, email, phone, country, city, address, postalCode);
            redirectAttributes.addFlashAttribute("profileSuccess", "Личные данные и адрес доставки успешно обновлены.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("profileError", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.getUser();
        if (user == null) {
            return "redirect:/login";
        }

        if (confirmPassword == null || !confirmPassword.equals(newPassword)) {
            redirectAttributes.addFlashAttribute("passwordError", "Новый пароль и подтверждение не совпадают.");
            return "redirect:/profile";
        }

        try {
            userService.changePassword(user, currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("passwordSuccess", "Пароль успешно изменён.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
        }
        return "redirect:/profile";
    }
}
