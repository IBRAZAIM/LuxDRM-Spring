package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.dto.OverviewMetrics;
import kz.ibrazaim.catalog.dto.PeriodRange;
import kz.ibrazaim.catalog.dto.SalesDashboardResponse;
import kz.ibrazaim.catalog.model.Period;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OverviewService {
    private record RangeContext(LocalDate start, LocalDateTime end, Period effectivePeriod) {}

    private final VisitService visitService;
    private final OrderService orderService;

    public OverviewService(VisitService visitService, OrderService orderService) {
        this.visitService = visitService;
        this.orderService = orderService;
    }

    @Cacheable(value = "overviewMetrics", key = "#period")
    public OverviewMetrics getOverviewMetrics(Period period) {
        RangeContext range = resolveRange(period, null, null);
        VisitService.Aggregation aggregation = aggregationFor(
                range.effectivePeriod(),
                range.start(),
                range.end().toLocalDate()
        );
        return buildOverview(range.start(), range.end(), aggregation);
    }

    private OverviewMetrics buildOverview(LocalDate start, LocalDateTime end, VisitService.Aggregation aggregation) {
        long visits = visitService.getVisitsCount(start, end.toLocalDate());
        long orders = orderService.getOrdersCount(start.atStartOfDay(), end);
        double sales = orderService.getTotalSales(start.atStartOfDay(), end);
        double conversion = visits > 0 ? orders * 100.0 / visits : 0;
        Map<String, Long> chart = visitService.aggregateVisits(start, end.toLocalDate(), aggregation);
        return new OverviewMetrics(sales, orders, visits, conversion, chart);
    }

    private VisitService.Aggregation aggregationFor(Period period, LocalDate start, LocalDate end) {
        if (period == Period.CUSTOM) {
            long days = ChronoUnit.DAYS.between(start, end) + 1;
            if (days <= 62) {
                return VisitService.Aggregation.DAY;
            }
            if (days <= 420) {
                return VisitService.Aggregation.WEEK;
            }
            return VisitService.Aggregation.MONTH;
        }

        return switch (period) {
            case TODAY, WEEK -> VisitService.Aggregation.DAY;
            case MONTH, QUARTER -> VisitService.Aggregation.WEEK;
            case YEAR, ALL -> VisitService.Aggregation.MONTH;
            case CUSTOM -> VisitService.Aggregation.WEEK;
        };
    }

    private RangeContext resolveRange(Period period, LocalDate customStart, LocalDate customEnd) {
        Period effectivePeriod = period != null ? period : Period.TODAY;
        LocalDate start;
        LocalDateTime end;

        if (effectivePeriod == Period.CUSTOM) {
            if (customStart == null || customEnd == null) {
                PeriodRange fallback = PeriodRange.of(Period.TODAY);
                start = fallback.start().toLocalDate();
                end = fallback.end();
                effectivePeriod = Period.TODAY;
            } else {
                LocalDate normalizedStart = customStart;
                LocalDate normalizedEnd = customEnd;
                if (normalizedEnd.isBefore(normalizedStart)) {
                    LocalDate tmp = normalizedStart;
                    normalizedStart = normalizedEnd;
                    normalizedEnd = tmp;
                }
                start = normalizedStart;
                end = normalizedEnd.plusDays(1).atStartOfDay().minusNanos(1);
            }
        } else {
            PeriodRange range = PeriodRange.of(effectivePeriod);
            start = range.start().toLocalDate();
            end = range.end();
        }

        return new RangeContext(start, end, effectivePeriod);
    }

    private String toWeekKey(LocalDate date) {
        int weekYear = date.get(IsoFields.WEEK_BASED_YEAR);
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return String.format(Locale.ROOT, "%d-%02d", weekYear, week);
    }

    private List<String> buildLabels(LocalDate start, LocalDate end, VisitService.Aggregation aggregation) {
        List<String> labels = new ArrayList<>();
        switch (aggregation) {
            case DAY -> {
                for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                    labels.add(date.toString());
                }
            }
            case WEEK -> {
                LocalDate cursor = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate limit = end.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                while (!cursor.isAfter(limit)) {
                    labels.add(toWeekKey(cursor));
                    cursor = cursor.plusWeeks(1);
                }
            }
            case MONTH -> {
                LocalDate cursor = start.withDayOfMonth(1);
                LocalDate limit = end.withDayOfMonth(1);
                while (!cursor.isAfter(limit)) {
                    labels.add(String.format(Locale.ROOT, "%d-%02d", cursor.getYear(), cursor.getMonthValue()));
                    cursor = cursor.plusMonths(1);
                }
            }
        }
        return labels;
    }

    public OverviewMetrics getOverviewMetrics(Period period, LocalDate customStart, LocalDate customEnd) {
        RangeContext range = resolveRange(period, customStart, customEnd);
        VisitService.Aggregation aggregation = aggregationFor(
                range.effectivePeriod(),
                range.start(),
                range.end().toLocalDate()
        );
        return buildOverview(range.start(), range.end(), aggregation);
    }

    public SalesDashboardResponse getDashboard(Period period, LocalDate customStart, LocalDate customEnd) {
        RangeContext range = resolveRange(period, customStart, customEnd);
        VisitService.Aggregation aggregation = aggregationFor(
                range.effectivePeriod(),
                range.start(),
                range.end().toLocalDate()
        );
        OverviewMetrics overview = buildOverview(range.start(), range.end(), aggregation);

        Map<String, Long> visitsMap = overview.visitsByDay();
        Map<String, OrderService.BucketStats> orderMap = orderService.aggregateByPeriod(
                range.start().atStartOfDay(),
                range.end(),
                aggregation
        );

        List<String> labels = buildLabels(range.start(), range.end().toLocalDate(), aggregation);
        List<Long> visitsSeries = new ArrayList<>(labels.size());
        List<Long> ordersSeries = new ArrayList<>(labels.size());
        List<Double> salesSeries = new ArrayList<>(labels.size());
        List<Double> conversionSeries = new ArrayList<>(labels.size());

        for (String label : labels) {
            long visits = visitsMap.getOrDefault(label, 0L);
            OrderService.BucketStats bucket = orderMap.get(label);
            long orders = bucket != null ? bucket.orders() : 0L;
            double sales = bucket != null ? bucket.sales() : 0.0;
            double conversion = visits > 0 ? orders * 100.0 / visits : 0.0;

            visitsSeries.add(visits);
            ordersSeries.add(orders);
            salesSeries.add(sales);
            conversionSeries.add(conversion);
        }

        double averageOrderValue = overview.ordersToday() > 0
                ? overview.totalSales() / overview.ordersToday()
                : 0.0;
        double revenuePerVisit = overview.visitsToday() > 0
                ? overview.totalSales() / overview.visitsToday()
                : 0.0;

        return new SalesDashboardResponse(
                overview,
                labels,
                visitsSeries,
                ordersSeries,
                salesSeries,
                conversionSeries,
                averageOrderValue,
                revenuePerVisit
        );
    }
}
