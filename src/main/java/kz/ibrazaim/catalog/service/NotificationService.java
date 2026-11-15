package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.Order;
import kz.ibrazaim.catalog.model.PaymentTransaction;
import kz.ibrazaim.catalog.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final boolean enabled;
    private final String botToken;
    private final String chatId;
    private final HttpClient httpClient;

    public NotificationService(
            @Value("${app.notifications.enabled:true}") boolean enabled,
            @Value("${app.telegram.bot-token:}") String botToken,
            @Value("${app.telegram.chat-id:}") String chatId
    ) {
        this.enabled = enabled;
        this.botToken = botToken != null ? botToken.trim() : "";
        this.chatId = chatId != null ? chatId.trim() : "";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void notifyOrderCreated(Order order) {
        if (!enabled || order == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Новый заказ #00").append(order.getId());
        if (order.getSerialNumber() != null) {
            sb.append(" (").append(order.getSerialNumber()).append(")");
        }
        sb.append("\nПокупатель: ").append(safe(order.getFullName(), "—"));
        sb.append("\nТелефон: ").append(safe(order.getPhone(), "—"));
        sb.append("\nГород/Адрес: ").append(safe(order.getCity(), "")).append(" ").append(safe(order.getAddress(), ""));
        sb.append("\nСумма: ").append(order.getTotalPrice() != null ? order.getTotalPrice() : "0").append(" KZT");
        sb.append("\nОплата: ").append(safe(order.getPaymentMethod(), "—"));
        if (order.getPromoCode() != null && !order.getPromoCode().isBlank()) {
            sb.append("\nПромокод: ").append(order.getPromoCode());
        }

        String text = sb.toString();
        logger.info("[NOTIFY][ORDER_CREATED] {}", text.replace("\n", " | "));
        sendTelegramAsync(text);
    }

    public void notifyPaymentCompleted(PaymentTransaction tx) {
        if (!enabled || tx == null) {
            return;
        }
        long orderId = tx.getOrder() != null ? tx.getOrder().getId() : 0L;
        String text = "Оплата подтверждена по заказу #00" + orderId
                + "\nТранзакция: " + safe(tx.getExternalTransactionId(), "—")
                + "\nПровайдер: " + safe(tx.getProviderDisplayName(), safe(tx.getProvider(), "—"))
                + "\nСумма: " + (tx.getAmount() != null ? tx.getAmount() : "0") + " " + safe(tx.getCurrency(), "KZT");

        logger.info("[NOTIFY][PAYMENT_PAID] {}", text.replace("\n", " | "));
        sendTelegramAsync(text);
    }

    public void notifyOrderStatusChanged(Order order) {
        if (!enabled || order == null) {
            return;
        }
        String statusText = order.getStatus() != null ? order.getStatus().getDisplayName() : "Обновлён";
        StringBuilder sb = new StringBuilder();
        sb.append("Статус заказа #00").append(order.getId()).append(": ").append(statusText);
        if (order.getEmail() != null && !order.getEmail().isBlank()) {
            sb.append("\nEmail клиента: ").append(order.getEmail());
        }
        if (order.getTrackingNumber() != null && !order.getTrackingNumber().isBlank()) {
            sb.append("\nТрек-номер: ").append(order.getTrackingNumber());
        }

        String text = sb.toString();
        logger.info("[NOTIFY][ORDER_STATUS] {}", text.replace("\n", " | "));
        sendTelegramAsync(text);
    }

    public void notifyPasswordReset(User user, String resetLink) {
        if (!enabled || user == null) {
            return;
        }
        String email = user.getEmail() != null ? user.getEmail() : user.getLogin();
        String text = "Запрос сброса пароля для " + safe(email, "unknown")
                + "\nСсылка: " + safe(resetLink, "—");

        logger.info("[NOTIFY][PASSWORD_RESET] email={}, resetLink={}", email, resetLink);
        sendTelegramAsync(text);
    }

    private void sendTelegramAsync(String text) {
        if (botToken.isEmpty() || chatId.isEmpty() || text == null || text.isBlank()) {
            return;
        }
        try {
            String formBody = "chat_id=" + URLEncoder.encode(chatId, StandardCharsets.UTF_8)
                    + "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.telegram.org/bot" + botToken + "/sendMessage"))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(resp -> {
                        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                            logger.warn("Telegram API ответил статусом {}: {}", resp.statusCode(), resp.body());
                        }
                    })
                    .exceptionally(ex -> {
                        logger.warn("Ошибка отправки Telegram-уведомления: {}", ex.getMessage());
                        return null;
                    });
        } catch (Exception ex) {
            logger.warn("Не удалось сформировать Telegram-уведомление: {}", ex.getMessage());
        }
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
