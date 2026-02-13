package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.dto.OverviewMetrics;
import kz.ibrazaim.catalog.model.Period;
import kz.ibrazaim.catalog.service.OverviewService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;

@Controller
@RequestMapping("/sales")
public class SalesController {

    private final OverviewService overviewService;

    public SalesController(OverviewService overviewService) {
        this.overviewService = overviewService;
    }

    /**
     * SSR — первая загрузка страницы
     */
    @GetMapping
    public String salesPage(
            @RequestParam(defaultValue = "TODAY") Period period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate customStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate customEnd,
            Model model
    ) {
        OverviewMetrics overview = overviewService.getOverviewMetrics(period, customStart, customEnd);
        model.addAttribute("overview", overview);
        model.addAttribute("period", period);
        model.addAttribute("customStart", customStart);
        model.addAttribute("customEnd", customEnd);
        return "otchet";
    }

    /**
     * AJAX API (JSON) — динамическое обновление метрик
     */
    @GetMapping("/api/overview")
    @ResponseBody
    public OverviewMetrics overviewApi(
            @RequestParam Period period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate customStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate customEnd
    ) {
        return overviewService.getOverviewMetrics(period, customStart, customEnd);
    }
}
