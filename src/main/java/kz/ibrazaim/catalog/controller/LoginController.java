package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Controller
@RequiredArgsConstructor
public class LoginController {
    private final UserService userService;

    @InitBinder("user")
    public void initUserBinder(WebDataBinder binder) {
        binder.setAllowedFields("name", "surname", "email", "login", "password");
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        if (userService.getUser() != null) {
            return "redirect:/";
        }
        model.addAttribute("user", new User());
        return "registerForm";
    }

    @PostMapping("/register")
    public String registerUser(
            @ModelAttribute("user") User user,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (result.hasErrors()) {
            return "registerForm";
        }
        try {
            userService.create(user, confirmPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Регистрация прошла успешно! Теперь вы можете войти.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "registerForm";
        } catch (Exception e) {
            model.addAttribute("error", "Регистрация не удалась. Попробуйте снова.");
            return "registerForm";
        }
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "redirect", required = false) String redirect,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "email", required = false) String email,
            HttpServletRequest request,
            Model model
    ) {
        if (userService.getUser() != null) {
            if (isSafeInternalRedirect(redirect)) {
                return "redirect:" + redirect;
            }
            return "redirect:/";
        }

        if (redirect == null || redirect.isBlank()) {
            redirect = extractRedirectFromReferer(request.getHeader("Referer"));
        }
        if (isSafeInternalRedirect(redirect)) {
            model.addAttribute("redirect", redirect);
        }
        model.addAttribute("loginError", error != null);
        if (email != null && !email.isBlank()) {
            model.addAttribute("email", email.trim());
        }
        return "loginForm";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        if (userService.getUser() != null) {
            return "redirect:/profile";
        }
        return "forgotPasswordForm";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam("email") String email,
            Model model
    ) {
        if (email == null || email.isBlank()) {
            model.addAttribute("error", "Введите email или логин.");
            return "forgotPasswordForm";
        }
        try {
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            userService.initiatePasswordReset(email, baseUrl);
        } catch (IllegalArgumentException ignored) {
            // Не раскрываем существование email в базе (защита от User Enumeration)
        }
        model.addAttribute("successMessage", "Если аккаунт с указанным email существует, ссылка для восстановления пароля отправлена.");
        return "forgotPasswordForm";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam(value = "token", required = false) String token, Model model) {
        if (!userService.isResetTokenValid(token)) {
            model.addAttribute("tokenInvalid", true);
            model.addAttribute("error", "Ссылка для сброса пароля недействительна или устарела.");
            return "resetPasswordForm";
        }
        model.addAttribute("token", token);
        return "resetPasswordForm";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam("token") String token,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        try {
            userService.resetPasswordWithToken(token, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Пароль успешно изменён! Войдите с новым паролем.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("token", token);
            model.addAttribute("tokenInvalid", !userService.isResetTokenValid(token));
            model.addAttribute("error", e.getMessage());
            return "resetPasswordForm";
        }
    }

    private String extractRedirectFromReferer(String referer) {
        if (referer == null || referer.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(referer);
            String path = uri.getPath();
            if (path == null || path.isBlank() || "/login".equals(path) || "/register".equals(path)) {
                return null;
            }

            String query = uri.getQuery();
            return query == null || query.isBlank() ? path : path + "?" + query;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isSafeInternalRedirect(String redirect) {
        return redirect != null
                && !redirect.isBlank()
                && redirect.startsWith("/")
                && !redirect.startsWith("//")
                && !redirect.contains("://")
                && !redirect.contains("\\")
                && !redirect.contains("\r")
                && !redirect.contains("\n");
    }
}
