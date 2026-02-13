package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.buffer.VisitBuffer;
import kz.ibrazaim.catalog.model.Visit;
import kz.ibrazaim.catalog.repository.VisitRepository;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VisitService {

    public enum Aggregation {DAY, WEEK, MONTH}

    private final VisitRepository visitRepository;
    private final VisitBuffer visitBuffer;

    public VisitService(VisitRepository visitRepository, VisitBuffer visitBuffer) {
        this.visitRepository = visitRepository;
        this.visitBuffer = visitBuffer;
    }

    /** Подсчёт визитов за период */
    public long getVisitsCount(LocalDate start, LocalDate end) {
        return visitRepository.countDistinctByVisitDateBetween(start, end);
    }

    /** Агрегация визитов по дню/неделе/месяцу */
    public Map<String, Long> aggregateVisits(LocalDate start, LocalDate end, Aggregation agg) {
        Map<String, Long> map = new LinkedHashMap<>();
        List<Object[]> rows;

        switch (agg) {
            case DAY -> rows = visitRepository.countByDay(start, end);
            case WEEK -> rows = visitRepository.countByWeek(start, end);
            case MONTH -> rows = visitRepository.countByMonth(start, end);
            default -> throw new IllegalArgumentException("Unknown aggregation: " + agg);
        }

        for (Object[] row : rows) {
            map.put(row[0].toString(), ((Number) row[1]).longValue());
        }
        return map;
    }

    /** Регистрируем визит один раз в день */
    public void recordVisit(Long userId, HttpSession session) {
        String sessionId = session.getId();
        LocalDate today = LocalDate.now();

        // проверка уникальности визита
        boolean exists = (userId != null)
                ? visitRepository.existsByUserIdAndVisitDate(userId, today)
                : visitRepository.existsBySessionIdAndVisitDate(sessionId, today);

        if (!exists) {
            Visit visit = new Visit();
            visit.setSessionId(sessionId);
            visit.setUserId(userId);
            visit.setVisitDate(today);

            // добавляем в буфер (или сразу сохраняем)
            visitBuffer.addVisit(visit);
        }
    }
}
