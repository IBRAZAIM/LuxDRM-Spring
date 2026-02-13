package kz.ibrazaim.catalog.intercepcor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kz.ibrazaim.catalog.model.Visit;
import kz.ibrazaim.catalog.service.VisitService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDate;

@Component
public class VisitInterceptor implements HandlerInterceptor {

    private final VisitService visitService;

    public VisitInterceptor(VisitService visitService) {
        this.visitService = visitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object userIdObj = request.getSession().getAttribute("userId");
        Long userId = userIdObj != null ? (Long) userIdObj : null;

        visitService.recordVisit(userId, request.getSession());

        return true;
    }

}
