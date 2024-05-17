package kz.ibrazaim.catalog.controller;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

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


}
