package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.Review;
import kz.ibrazaim.catalog.model.Role;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.CartService;
import kz.ibrazaim.catalog.service.ReviewService;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ReviewController {
    private final ReviewService reviewService;
    private final UserService userService;
    private final CartService cartService;

    @GetMapping
    public String allOrders(Model model) {
        User user = userService.getUser();
        if (user == null) {
            return "redirect:/login";
        }
        if (!userService.hasAnyRole(user, Role.ADMIN, Role.MODER)) {
            return "redirect:/";
        }

        List<Review> reviews = reviewService.findALl();
        model.addAttribute("reviews", reviews);
        model.addAttribute("cartItemsCount", cartService.getCartItemsCountByUser(user));
        return "reviews";
    }

    @PostMapping("/updateStatus")
    public String updateOrderStatus(
            @RequestParam("reviewId") Long reviewId,
            @RequestParam("status") String status,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.getUser();
        if (user == null || !userService.hasAnyRole(user, Role.ADMIN, Role.MODER)) {
            return "redirect:/login";
        }

        reviewService.updateReviewStatus(reviewId, status);
        redirectAttributes.addFlashAttribute("successMessage", "Статус отзыва №" + reviewId + " успешно обновлён");
        return "redirect:/reviews";
    }
}
