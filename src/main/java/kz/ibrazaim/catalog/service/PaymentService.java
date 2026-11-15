package kz.ibrazaim.catalog.service;

import jakarta.annotation.PostConstruct;
import kz.ibrazaim.catalog.model.Order;
import kz.ibrazaim.catalog.model.PaymentStatus;
import kz.ibrazaim.catalog.model.PaymentTransaction;
import kz.ibrazaim.catalog.payment.PaymentGateway;
import kz.ibrazaim.catalog.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentGateway paymentGateway;
    private final JdbcTemplate jdbcTemplate;
    private final NotificationService notificationService;

    @PostConstruct
    public void ensurePaymentTableExists() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS payment_transactions (
                    id BIGSERIAL PRIMARY KEY,
                    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                    external_transaction_id VARCHAR(120) NOT NULL UNIQUE,
                    provider VARCHAR(64) NOT NULL,
                    payment_status VARCHAR(40) NOT NULL,
                    amount NUMERIC(12, 2) NOT NULL,
                    currency VARCHAR(10) NOT NULL DEFAULT 'KZT',
                    payment_url VARCHAR(512),
                    masked_pan VARCHAR(64),
                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    paid_at TIMESTAMP
                )
            """);
        } catch (Exception e) {
            logger.warn("Не удалось автоматически проверить таблицу payment_transactions: {}", e.getMessage());
        }
    }

    @Transactional
    public PaymentTransaction createPaymentForOrder(Order order, String provider) {
        PaymentGateway.PaymentInitResult initResult = paymentGateway.initiatePayment(order, provider);

        PaymentTransaction tx = new PaymentTransaction();
        tx.setOrder(order);
        tx.setExternalTransactionId(initResult.externalTransactionId());
        tx.setProvider(provider != null ? provider : "card_gateway");
        tx.setPaymentStatus(initResult.initialStatus());
        tx.setAmount(order.getTotalPrice());
        tx.setCurrency("KZT");
        tx.setPaymentUrl(initResult.redirectUrl());
        tx.setCreatedAt(LocalDateTime.now());

        return paymentTransactionRepository.save(tx);
    }

    public Optional<PaymentTransaction> findByExternalId(String externalId) {
        return paymentTransactionRepository.findByExternalTransactionId(externalId);
    }

    public Optional<PaymentTransaction> findLatestByOrder(Order order) {
        if (order == null) return Optional.empty();
        return paymentTransactionRepository.findFirstByOrderOrderByCreatedAtDesc(order);
    }

    public Map<Long, PaymentTransaction> findLatestByOrders(List<Order> orders) {
        Map<Long, PaymentTransaction> map = new LinkedHashMap<>();
        if (orders == null || orders.isEmpty()) {
            return map;
        }
        for (Order order : orders) {
            if (order != null) {
                map.put(order.getId(), findLatestByOrder(order).orElse(null));
            }
        }
        return map;
    }

    @Transactional
    public PaymentTransaction completePayment(String externalId, boolean success, String maskedPan) {
        PaymentTransaction tx = paymentTransactionRepository.findByExternalTransactionId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("Платёжная транзакция не найдена: " + externalId));

        if (success) {
            tx.setPaymentStatus(PaymentStatus.PAID);
            tx.setPaidAt(LocalDateTime.now());
            if (maskedPan != null && !maskedPan.isBlank()) {
                tx.setMaskedPan(maskedPan.trim());
            }
        } else {
            tx.setPaymentStatus(PaymentStatus.FAILED);
        }

        PaymentTransaction saved = paymentTransactionRepository.save(tx);
        if (success) {
            notificationService.notifyPaymentCompleted(saved);
        }
        return saved;
    }
}

