package kz.ibrazaim.catalog.payment;

import kz.ibrazaim.catalog.model.Order;
import kz.ibrazaim.catalog.model.PaymentStatus;

import java.math.BigDecimal;

/**
 * Контракт интеграции с внешним платёжным шлюзом
 * (Freedom Pay, CloudPayments, Halyk ePay, Kaspi Pay, Stripe и др.).
 */
public interface PaymentGateway {

    record PaymentInitResult(
            String externalTransactionId,
            String redirectUrl,
            PaymentStatus initialStatus
    ) {}

    record WebhookPayload(
            String externalTransactionId,
            Long orderId,
            String status,
            BigDecimal amount,
            String maskedPan
    ) {}

    /**
     * Создаёт платёжную сессию (Payment Intent / Invoice) и возвращает URL для редиректа покупателя.
     */
    PaymentInitResult initiatePayment(Order order, String provider);

    /**
     * Проверяет HMAC-SHA256 подпись входящего webhook-уведомления от платёжного провайдера.
     */
    boolean verifyWebhookSignature(String rawPayload, String signatureHeader);
}

