package kz.ibrazaim.catalog.intercepcor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.VisitService;
import kz.ibrazaim.catalog.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class VisitInterceptor implements HandlerInterceptor {

    private final VisitService visitService;
    private final UserService userService;

    public VisitInterceptor(VisitService visitService, UserService userService) {
        this.visitService = visitService;
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        User user = userService.getUser();
        Long userId = user != null ? user.getId() : null;
        visitService.recordVisit(userId, request.getSession());

        return true;
    }

}
