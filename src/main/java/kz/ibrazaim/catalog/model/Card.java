package kz.ibrazaim.catalog.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "cards")
public class Card {

    @jakarta.persistence.Id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    User user; // Связь с таблицей пользователей

    @Column(name = "card_number", nullable = false, length = 19)
    String cardNumber;

    @Column(name = "card_holder", nullable = false)
    String cardHolder;

    @Column(name = "card_month", nullable = false)
    String month;

    @Column(name = "card_year", nullable = false)
    String year;

    @Column(name = "cvv_code", nullable = false)
    String cvv;
}

