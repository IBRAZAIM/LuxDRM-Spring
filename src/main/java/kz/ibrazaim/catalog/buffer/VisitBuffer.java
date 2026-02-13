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
        String key;
        if (visit.getUserId() != null) {
            key = "U:" + visit.getUserId() + ":" + visit.getVisitDate();
        } else {
            key = "S:" + visit.getSessionId() + ":" + visit.getVisitDate();
        }
        buffer.putIfAbsent(key, visit); // уникально
    }

    /** Батчевое сохранение каждые 10 секунд */
    @Scheduled(fixedDelay = 10000)
    public void flush() {
        if (buffer.isEmpty()) return;
        visitRepository.saveAll(buffer.values());
        buffer.clear();
    }
}

