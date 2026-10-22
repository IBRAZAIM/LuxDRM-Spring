package kz.ibrazaim.catalog.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "order_products")
public class OrderProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    Order order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    Product product;

    String size;

    @Column(name = "unit_price", precision = 12, scale = 2)
    BigDecimal unitPrice;

    @Column(name = "discount_percent")
    int discountPercent;

    @Column(name = "line_total", precision = 12, scale = 2)
    BigDecimal lineTotal;

    int quantity;
}
