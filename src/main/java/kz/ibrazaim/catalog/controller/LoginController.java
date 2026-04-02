package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "registerForm";
        } catch (Exception e) {
            model.addAttribute("error", "Регистрация не удалась. Попробуйте снова.");
            return "registerForm";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "redirect", required = false) String redirect,
            @RequestParam(value = "error", required = false) String error,
            Model model
    ) {
        if (redirect != null) {
            model.addAttribute("redirect", redirect);
        }
        if (error != null) {
            model.addAttribute("loginError", true);
        }
        return "loginForm";
    }
}
