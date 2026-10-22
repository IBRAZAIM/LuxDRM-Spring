package kz.ibrazaim.catalog.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;

    @Column(name = "serial_number")
    String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 255)
    OrderStatus status;

    String email;

    String phone;

    String fullName;

    String country;

    String city;

    String address;

    String postalCode;

    @Column(name = "delivery_method")
    String deliveryMethod;

    @Column(name = "payment_method")
    String paymentMethod;

    LocalDateTime date;

    @Column(name = "total_price", precision = 12, scale = 2)
    BigDecimal totalPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<OrderProduct> orderProducts = new ArrayList<>();
}
