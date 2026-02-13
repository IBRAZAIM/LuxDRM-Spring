package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    // проверка, был ли уже визит сегодня
    boolean existsBySessionIdAndVisitDate(String sessionId, LocalDate visitDate);

    boolean existsByUserIdAndVisitDate(Long userId, LocalDate visitDate);

    // подсчёт уникальных визитов за период
    long countDistinctByVisitDateBetween(LocalDate visitDate, LocalDate visitDate2);

    // группировка по дню
    @Query("SELECT v.visitDate, COUNT(v) " +
            "FROM Visit v WHERE v.visitDate BETWEEN :start AND :end " +
            "GROUP BY v.visitDate ORDER BY v.visitDate")
    List<Object[]> countByDay(@Param("start") LocalDate start, @Param("end") LocalDate end);

    // группировка по неделе (ISO week)
    @Query("SELECT FUNCTION('to_char', v.visitDate, 'IYYY-IW'), COUNT(v) " +
            "FROM Visit v WHERE v.visitDate BETWEEN :start AND :end " +
            "GROUP BY FUNCTION('to_char', v.visitDate, 'IYYY-IW') ORDER BY 1")
    List<Object[]> countByWeek(@Param("start") LocalDate start, @Param("end") LocalDate end);

    // группировка по месяцу
    @Query("SELECT FUNCTION('to_char', v.visitDate, 'YYYY-MM'), COUNT(v) " +
            "FROM Visit v WHERE v.visitDate BETWEEN :start AND :end " +
            "GROUP BY FUNCTION('to_char', v.visitDate, 'YYYY-MM') ORDER BY 1")
    List<Object[]> countByMonth(@Param("start") LocalDate start, @Param("end") LocalDate end);
}

