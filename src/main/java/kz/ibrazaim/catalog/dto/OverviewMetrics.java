package kz.ibrazaim.catalog.dto;


import java.util.Map;

public record OverviewMetrics(
        Double totalSales,
        Long ordersToday,
        Long visitsToday,
        Double conversion,
        Map<String, Long> visitsByDay
) {}

