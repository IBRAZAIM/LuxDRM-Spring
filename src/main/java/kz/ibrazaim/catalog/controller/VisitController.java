package kz.ibrazaim.catalog.controller;

import jakarta.servlet.http.HttpSession;
import kz.ibrazaim.catalog.service.VisitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    // Вызываем при заходе на страницу
    @GetMapping("/analytics/register")
    public void registerVisit(@RequestParam(required = false) Long userId, HttpSession session) {
        visitService.recordVisit(userId, session);
    }
}
