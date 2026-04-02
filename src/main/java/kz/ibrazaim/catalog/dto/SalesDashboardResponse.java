package kz.ibrazaim.catalog.dto;

import java.util.List;

public record SalesDashboardResponse(
        OverviewMetrics overview,
        List<String> labels,
        List<Long> visitsSeries,
        List<Long> ordersSeries,
        List<Double> salesSeries,
        List<Double> conversionSeries,
        Double averageOrderValue,
        Double revenuePerVisit
) {
}
