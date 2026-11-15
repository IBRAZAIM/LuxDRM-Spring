package kz.ibrazaim.catalog.payment;

import kz.ibrazaim.catalog.model.Order;
import kz.ibrazaim.catalog.model.PaymentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * Реализация платёжного шлюза с поддержкой:
 * 1) Встроенного тестового шлюза (sandbox) — работает сразу «из коробки».
 * 2) Внешнего платёжного провайдера (Freedom Pay / CloudPayments / Halyk ePay / Kaspi Pay)
 *    через настройки payment.gateway.* в application.properties.
 */
@Component
public class ConfigurablePaymentGateway implements PaymentGateway {

    @Value("${payment.gateway.mode:sandbox}")
    private String mode;

    @Value("${payment.gateway.external-checkout-url:}")
    private String externalCheckoutUrl;

    @Value("${payment.gateway.merchant-id:LUXDRM_MERCHANT_01}")
    private String merchantId;

    @Value("${payment.gateway.webhook-secret:luxdrm-webhook-secret-key}")
    private String webhookSecret;

    @Value("${payment.gateway.return-base-url:http://localhost:8080}")
    private String returnBaseUrl;

    @Override
    public PaymentInitResult initiatePayment(Order order, String provider) {
        String safeProvider = (provider == null || provider.isBlank()) ? "card_gateway" : provider.trim().toLowerCase(Locale.ROOT);
        String extId = "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase(Locale.ROOT);

        // Оплата при получении не требует перехода на платёжный шлюз
        if ("cod".equals(safeProvider) || "other".equals(safeProvider)) {
            return new PaymentInitResult(extId, "/orders/" + order.getId(), PaymentStatus.COD);
        }

        // Если настроен внешний URL платёжного провайдера (production / external)
        if ("external".equalsIgnoreCase(mode) && externalCheckoutUrl != null && !externalCheckoutUrl.isBlank()) {
            String redirectUrl = UriComponentsBuilder.fromHttpUrl(externalCheckoutUrl)
                    .queryParam("merchant_id", merchantId)
                    .queryParam("transaction_id", extId)
                    .queryParam("order_id", order.getId())
                    .queryParam("amount", order.getTotalPrice())
                    .queryParam("currency", "KZT")
                    .queryParam("provider", safeProvider)
                    .queryParam("success_url", returnBaseUrl + "/orders/" + order.getId())
                    .queryParam("fail_url", returnBaseUrl + "/payment/checkout/" + extId)
                    .queryParam("callback_url", returnBaseUrl + "/api/payments/webhook")
                    .toUriString();
            return new PaymentInitResult(extId, redirectUrl, PaymentStatus.AWAITING_PAYMENT);
        }

        // По умолчанию: переход на встроенную страницу платёжного шлюза (Sandbox Gateway)
        String sandboxUrl = "/payment/checkout/" + extId;
        return new PaymentInitResult(extId, sandboxUrl, PaymentStatus.AWAITING_PAYMENT);
    }

    @Override
    public boolean verifyWebhookSignature(String rawPayload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return true;
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(keySpec);
            byte[] digest = hmac.doFinal((rawPayload != null ? rawPayload : "").getBytes(StandardCharsets.UTF_8));
            String expectedHex = HexFormat.of().formatHex(digest);
            return MessageDigest.isEqual(
                    expectedHex.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                    signatureHeader.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }
}

