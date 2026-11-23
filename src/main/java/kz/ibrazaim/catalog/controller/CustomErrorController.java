package kz.ibrazaim.catalog.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, HttpServletResponse response, Model model) {
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = 500;
        if (statusObj != null) {
            try {
                statusCode = Integer.parseInt(statusObj.toString());
            } catch (NumberFormatException ignored) {
            }
        }

        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "error/404";
        }

        response.setStatus(statusCode);
        String msg = switch (statusCode) {
            case 403 -> "Доступ к этой странице ограничен.";
            case 400 -> "Некорректный запрос.";
            default -> "Произошла временная ошибка. Попробуйте обновить страницу или вернуться на главную.";
        };
        model.addAttribute("message", msg);
        return "exception";
    }
}
