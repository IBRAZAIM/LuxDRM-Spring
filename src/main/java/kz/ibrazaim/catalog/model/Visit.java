package kz.ibrazaim.catalog.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@Entity
@Table(name = "visits", uniqueConstraints = {@UniqueConstraint(columnNames = {"session_id", "visit_date"})})
public class Visit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate visitDate;

    private Long userId;

    private String page;

    private String sessionId;
}

