package kz.ibrazaim.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "cards")
public class Card {

    @jakarta.persistence.Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "card_number", nullable = false, columnDefinition = "TEXT")
    String cardNumber;

    @Column(name = "card_holder", nullable = false, columnDefinition = "TEXT")
    String cardHolder;

    @Column(name = "card_month", nullable = false, columnDefinition = "TEXT")
    String month;

    @Column(name = "card_year", nullable = false, columnDefinition = "TEXT")
    String year;

    @Column(name = "cvv_code", columnDefinition = "TEXT")
    String cvv;
}
