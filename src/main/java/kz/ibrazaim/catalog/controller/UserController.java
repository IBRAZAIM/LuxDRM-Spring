package kz.ibrazaim.catalog.controller;


import kz.ibrazaim.catalog.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class UserController {
    @GetMapping("/register")
    public String showregisterForm(Model model){
        model.addAttribute("user", new User());
        return "registerForm";
    }
}
