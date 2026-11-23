package kz.ibrazaim.catalog.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kz.ibrazaim.catalog.exception.EntityNotFoundException;
import kz.ibrazaim.catalog.exception.ProductCreateException;
import kz.ibrazaim.catalog.exception.UserNotFoundException;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.CartService;
import kz.ibrazaim.catalog.service.UserService;
import kz.ibrazaim.catalog.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final CartService cartService;
    private final UserService userService;
    private final WishlistService wishlistService;

    @ModelAttribute("cartItemsCount")
    public int populateCartItemsCount(HttpSession session) {
        User user = userService.getUser();
        return cartService.getCartItemsCount(user, session);
    }

    @ModelAttribute("wishlistUserKey")
    public String populateWishlistUserKey() {
        User user = userService.getUser();
        return user != null ? "user_" + user.getId() : "guest";
    }

    @ModelAttribute("wishlistServerIds")
    public String populateWishlistServerIds(HttpSession session) {
        User user = userService.getUser();
        if (user == null) {
            return "";
        }
        try {
            wishlistService.mergeGuestWishlistToUser(session, user);
            List<Long> ids = wishlistService.getWishlistProductIds(user, session);
            return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        } catch (Exception e) {
            return "";
        }
    }

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

    @ExceptionHandler(BindException.class)
    public String handleBindException(BindException ex, Model model, HttpServletRequest request) {
        logger.error("Ошибка привязки данных формы (BindException) на {}: {}", request.getRequestURI(), ex.getMessage());
        StringBuilder sb = new StringBuilder("Ошибка в данных формы: ");
        ex.getFieldErrors().forEach(fe ->
                sb.append(fe.getField()).append(" (").append(fe.getDefaultMessage()).append("); ")
        );
        model.addAttribute("message", sb.toString());
        return "exception";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleValidationException(MethodArgumentNotValidException ex, Model model) {
        logger.error("Ошибка валидации: {}", ex.getMessage());
        model.addAttribute("message", "Некорректные параметры запроса: " + ex.getMessage());
        return "exception";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleTypeMismatch(MethodArgumentTypeMismatchException ex, Model model) {
        logger.error("Несовпадение типов: параметр {} имеет неверный формат '{}'", ex.getName(), ex.getValue());
        model.addAttribute("message", "Параметр '" + ex.getName() + "' имеет неверный формат");
        return "exception";
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public String handleMissingParam(MissingServletRequestParameterException ex, Model model) {
        logger.error("Отсутствует обязательный параметр: {}", ex.getParameterName());
        model.addAttribute("message", "Отсутствует обязательный параметр: " + ex.getParameterName());
        return "exception";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(MaxUploadSizeExceededException ex, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        logger.warn("Превышен максимальный размер файла: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", "Размер загружаемых файлов превышает допустимый лимит (максимум 10 МБ на файл)");
        String referer = request.getHeader("Referer");
        return referer != null ? "redirect:" + referer : "redirect:/products";
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noSuchElementException(EntityNotFoundException ex, Model model){
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "exception";
    }

    @ExceptionHandler(NullPointerException.class)
    public String nullPointerException(NullPointerException ex, Model model){
        logger.error("NullPointerException", ex);
        model.addAttribute("message", "Произошла непредвиденная ошибка на сервере.");
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
