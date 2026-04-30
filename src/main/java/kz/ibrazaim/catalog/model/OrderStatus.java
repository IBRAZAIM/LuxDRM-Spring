package kz.ibrazaim.catalog.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    PENDING("\u0412 \u043e\u0436\u0438\u0434\u0430\u043d\u0438\u0438"),
    SHIPPED("\u041e\u0442\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u043e"),
    DELIVERED("\u0414\u043e\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u043e"),
    CANCELED("\u041e\u0442\u043c\u0435\u043d\u0435\u043d\u043e");

    private final String displayName;

    public static OrderStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }

        String normalized = value.trim();
        for (OrderStatus status : values()) {
            if (status.name().equalsIgnoreCase(normalized)
                    || status.displayName.equalsIgnoreCase(normalized)) {
                return status;
            }
        }

        if ("Оформлено".equalsIgnoreCase(normalized)) {
            return PENDING;
        }
        if ("completed".equalsIgnoreCase(normalized)) {
            return DELIVERED;
        }
        if ("pending".equalsIgnoreCase(normalized)) {
            return PENDING;
        }
        if ("cancelled".equalsIgnoreCase(normalized)
                || "canceled".equalsIgnoreCase(normalized)) {
            return CANCELED;
        }

        throw new IllegalArgumentException("Invalid order status: " + value);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
