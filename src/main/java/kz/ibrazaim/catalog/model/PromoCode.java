package kz.ibrazaim.catalog.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "promo_codes")
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true, length = 64)
    String code;

    @Column(name = "discount_percent", nullable = false)
    int discountPercent;

    @Column(length = 255)
    String description;

    @Column(nullable = false)
    boolean active = true;

    @Column(name = "max_uses")
    Integer maxUses;

    @Column(name = "used_count", nullable = false)
    int usedCount = 0;

    @Column(name = "min_order_amount", precision = 12, scale = 2, nullable = false)
    BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "expires_at")
    LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt = LocalDateTime.now();
}
