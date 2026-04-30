package kz.ibrazaim.catalog.controller;

import jakarta.servlet.http.HttpSession;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.UserService;
import kz.ibrazaim.catalog.service.VisitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VisitController {

    private final VisitService visitService;
    private final UserService userService;

    public VisitController(VisitService visitService, UserService userService) {
        this.visitService = visitService;
        this.userService = userService;
    }

    @GetMapping("/analytics/register")
    public void registerVisit(HttpSession session) {
        User user = userService.getUser();
        Long userId = user != null ? user.getId() : null;
        visitService.recordVisit(userId, session);
    }
}
