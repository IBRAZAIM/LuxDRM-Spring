package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class LoginController {
    private final UserService userService;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        return "registerForm";
    }

    @PostMapping("/register")
    public String registerUser(
            @ModelAttribute("user") User user,
            BindingResult result,
            Model model
    ) {
        if (result.hasErrors()) {
            return "registerForm";
        }
        try {
            userService.create(user);
        } catch (Exception e) {
            model.addAttribute("error", "Регистрация не удалась. Пожалуйста, попробуйте снова.");
            return "registerForm";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(Model model){
        model.addAttribute("loginError", false);
        return "loginForm";
    }

    @PostMapping("/login")
    public String loginUser(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:" ;
        } else {
            model.addAttribute("loginError", true);
        }
        return "loginForm";
    }
}
