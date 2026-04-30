package kz.ibrazaim.catalog.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import kz.ibrazaim.catalog.exception.ProductCreateException;
import kz.ibrazaim.catalog.exception.UserNotFoundException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ModelAttribute("currentRequestPath")
    public String currentRequestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        return query == null || query.isBlank() ? path : path + "?" + query;
    }

    @ModelAttribute("canonicalUrl")
    public String canonicalUrl(HttpServletRequest request) {
        return request.getRequestURL().toString();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public String noSuchElementException(EntityNotFoundException ex, Model model){
        model.addAttribute("message",ex.getMessage());
        return "exception";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "exception";
    }

    @ExceptionHandler(NullPointerException.class)
    public String nullPointerException(NullPointerException ex, Model model){
        model.addAttribute("message", ex.getMessage());
        return "exception";
    }

    @ExceptionHandler(UserNotFoundException.class)
    public String handleUserNotFoundException(UserNotFoundException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "exception";
    }

    @ExceptionHandler(ProductCreateException.class)
    public String handleProductCreate(
            ProductCreateException ex,
            RedirectAttributes redirectAttributes
    ) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/products";
    }
}
