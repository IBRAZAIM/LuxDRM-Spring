package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface VisitArchiveRepository extends JpaRepository<Visit, Long> {

    @Query("SELECT COUNT(v) FROM Visit v WHERE v.visitDate BETWEEN :start AND :end")
    Long countVisitsInPeriod(@Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);
}

