package kz.ibrazaim.catalog.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;
    @ManyToOne
    @JoinColumn(name = "product_id")
    Product product;
    @JoinColumn(name = "publication_status")
    String status;
    @JoinColumn(name = "estimation")
    int estimation;
    @JoinColumn(name = "estimation_text")
    String text;
    @JoinColumn(name = "estimation_data")
    LocalDateTime date;
}