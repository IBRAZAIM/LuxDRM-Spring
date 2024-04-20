package kz.ibrazaim.catalog.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "values")
public class Value {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    String name;

    @ManyToOne
    @JoinColumn(name = "product_id")
    Product product;

    @OneToOne
    @JoinColumn(name = "options_id")
    Option option;
}
