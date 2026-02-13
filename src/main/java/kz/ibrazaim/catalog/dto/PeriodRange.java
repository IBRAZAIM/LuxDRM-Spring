package kz.ibrazaim.catalog.dto;

import kz.ibrazaim.catalog.model.Period;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PeriodRange(LocalDateTime start, LocalDateTime end) {

    public static PeriodRange of(Period period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start;

        switch (period) {
            case TODAY -> start = now.toLocalDate().atStartOfDay();
            case WEEK -> start = now.minusWeeks(1).toLocalDate().atStartOfDay();
            case MONTH -> start = now.minusMonths(1).toLocalDate().atStartOfDay();
            case QUARTER -> start = now.minusMonths(3).toLocalDate().atStartOfDay();
            case YEAR -> start = now.minusYears(1).toLocalDate().atStartOfDay();
            case ALL -> start = LocalDate.of(1970, 1, 1).atStartOfDay();
            case CUSTOM -> throw new IllegalArgumentException("CUSTOM period requires explicit start and end");
            default -> start = LocalDate.of(1970, 1, 1).atStartOfDay();
        }

        return new PeriodRange(start, now);
    }
}
