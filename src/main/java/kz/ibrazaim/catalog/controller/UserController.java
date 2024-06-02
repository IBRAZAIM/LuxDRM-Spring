package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class UserController {
    private final UserService userService;
    private final PasswordEncoder encoder;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

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
        user.setPassword(encoder.encode(user.getPassword()));
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
        try {
            logger.debug("Попытка аутентификации пользователя с помощью логина: {}", login);
            boolean isAuthenticated = userService.authenticate(login, password);
            if (isAuthenticated) {
                logger.debug("Аутентификация пользователя с логином прошла успешно: {}", login);
                return "redirect:/main";
            } else {
                logger.debug("Не удалось выполнить аутентификацию пользователя с логином: {}", login);
                model.addAttribute("loginError", true);
            }
        } catch (UsernameNotFoundException e) {
            logger.error("Пользователь не найден с помощью логина: {}", login);
            model.addAttribute("loginError", true);
        }
        return "loginForm";
    }
}


