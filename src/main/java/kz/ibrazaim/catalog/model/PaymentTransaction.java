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
@Table(name = "payment_transactions")
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    Order order;

    @Column(name = "external_transaction_id", nullable = false, unique = true, length = 120)
    String externalTransactionId;

    @Column(name = "provider", nullable = false, length = 64)
    String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 40)
    PaymentStatus paymentStatus;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    String currency = "KZT";

    @Column(name = "payment_url", length = 512)
    String paymentUrl;

    @Column(name = "masked_pan", length = 64)
    String maskedPan;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "paid_at")
    LocalDateTime paidAt;

    public String getProviderDisplayName() {
        if (provider == null) return "Онлайн-оплата";
        return switch (provider.toLowerCase()) {
            case "kaspi_qr", "kaspi" -> "Kaspi Pay / Kaspi QR";
            case "card_gateway", "card", "visa" -> "Банковская карта (Visa / Mastercard)";
            case "apple_pay" -> "Apple Pay";
            case "google_pay" -> "Google Pay";
            case "installment" -> "Рассрочка 0 · 0 · 12";
            case "cod", "other" -> "При получении курьеру";
            default -> provider;
        };
    }
}

