package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.dto.OverviewMetrics;
import kz.ibrazaim.catalog.dto.PeriodRange;
import kz.ibrazaim.catalog.model.Period;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class OverviewService {

    private final VisitService visitService;
    private final OrderService orderService;

    public OverviewService(VisitService visitService, OrderService orderService) {
        this.visitService = visitService;
        this.orderService = orderService;
    }

    @Cacheable(value = "overviewMetrics", key = "#period")
    public OverviewMetrics getOverviewMetrics(Period period) {

        // Получаем диапазон времени
        LocalDateTime now = LocalDate.now().atStartOfDay();
        LocalDate start = LocalDate.from(PeriodRange.of(period).start());
        LocalDateTime end = now;

        // Метрики
        long visits = visitService.getVisitsCount(start, LocalDate.from(end));
        long orders = orderService.getOrdersCount(start.atStartOfDay(), end);
        double sales = orderService.getTotalSales(start.atStartOfDay(), end);
        double conversion = visits > 0 ? orders * 100.0 / visits : 0;

        // График (агрегация сразу в SQL)
        Map<String, Long> chart = visitService.aggregateVisits(start, LocalDate.from(end), aggregationFor(period));

        return new OverviewMetrics(sales, orders, visits, conversion, chart);
    }

    private VisitService.Aggregation aggregationFor(Period period) {
        return switch (period) {
            case TODAY, WEEK -> VisitService.Aggregation.DAY;
            case MONTH, QUARTER -> VisitService.Aggregation.WEEK;
            case YEAR, ALL -> VisitService.Aggregation.MONTH;
            case CUSTOM -> null;
        };
    }

    public OverviewMetrics getOverviewMetrics(Period period, LocalDate customStart, LocalDate customEnd) {
        LocalDate start;
        LocalDateTime end = LocalDate.now().atStartOfDay();

        if (period == Period.CUSTOM && customStart != null && customEnd != null) {
            start = customStart;
            end = customEnd.atStartOfDay();
        } else {
            PeriodRange range = PeriodRange.of(period);
            start = LocalDate.from(range.start());
            end = range.end();
        }

        long visits = visitService.getVisitsCount(start, LocalDate.from(end));
        Long orders = orderService.getOrdersCount(start.atStartOfDay(), end);
        Double sales = orderService.getTotalSales(start.atStartOfDay(), end);

        double conversion = visits > 0 ? orders * 100.0 / visits : 0;

        Map<String, Long> chart = visitService.aggregateVisits(
                start,
                LocalDate.from(end),
                aggregationFor(period)
        );

        return new OverviewMetrics(
                sales,
                orders,
                visits,
                conversion,
                chart
        );
    }

}
