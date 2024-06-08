package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.Banner;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelExtensionsKt;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class UserController {
    private final UserService userService;
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        return "registerForm";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user, BindingResult result, Model model) {
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
    public String showLoginForm(Model model) {
        model.addAttribute("loginError", false);
        return "loginForm";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String login, @RequestParam String password, Model model) {
        boolean isAuthenticated = userService.authenticate(login, password);
        if (isAuthenticated) {
            return "redirect:/main";
        } else {
            model.addAttribute("loginError", true);
        }
        return "loginForm";
    }
    @GetMapping("/cart")
    public String getCartPage(Model model){
        model.addAttribute("cartItems", userService.findALlCartItems());
        return "cart";
    }

    @PostMapping("/cart/{productId}")
    public String addItemToCart(@PathVariable long productId){
        userService.addItemToCart(productId);
        return "redirect:/cart";
    }

}


