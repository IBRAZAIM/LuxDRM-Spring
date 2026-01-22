package kz.ibrazaim.catalog.controller;

import jakarta.persistence.EntityNotFoundException;
import kz.ibrazaim.catalog.exception.ProductCreateException;
import kz.ibrazaim.catalog.exception.UserNotFoundException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    public String noSuchElementException(EntityNotFoundException ex, Model model){
        model.addAttribute("message",ex.getMessage());
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
