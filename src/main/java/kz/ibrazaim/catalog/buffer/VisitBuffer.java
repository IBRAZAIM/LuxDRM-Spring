package kz.ibrazaim.catalog.buffer;

import kz.ibrazaim.catalog.model.Visit;
import kz.ibrazaim.catalog.repository.VisitRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VisitBuffer {

    private final VisitRepository visitRepository;

    // ключ = sessionId+visitDate или userId+visitDate
    private final Map<String, Visit> buffer = new ConcurrentHashMap<>();

    public VisitBuffer(VisitRepository visitRepository) {
        this.visitRepository = visitRepository;
    }

    /** Добавляем визит в буфер только если его ещё нет */
    public void addVisit(Visit visit) {
        String key = buildKey(visit.getUserId(), visit.getSessionId(), visit.getVisitDate());
        buffer.putIfAbsent(key, visit); // уникально
    }

    public boolean contains(Long userId, String sessionId, java.time.LocalDate visitDate) {
        return buffer.containsKey(buildKey(userId, sessionId, visitDate));
    }

    private String buildKey(Long userId, String sessionId, java.time.LocalDate visitDate) {
        if (userId != null) {
            return "U:" + userId + ":" + visitDate;
        }
        return "S:" + sessionId + ":" + visitDate;
    }

    /** Батчевое сохранение каждые 10 секунд */
    @Scheduled(fixedDelay = 10000)
    public void flush() {
        if (buffer.isEmpty()) return;
        // Атомарно забираем текущие визиты и очищаем буфер,
        // чтобы новые визиты, пришедшие во время saveAll, не потерялись
        Map<String, Visit> snapshot = new ConcurrentHashMap<>(buffer);
        buffer.keySet().removeAll(snapshot.keySet());
        visitRepository.saveAll(snapshot.values());
    }
}

