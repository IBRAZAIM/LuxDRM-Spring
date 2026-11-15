package kz.ibrazaim.catalog.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    AWAITING_PAYMENT("Ожидает онлайн-оплаты"),
    PAID("Оплачен"),
    COD("Оплата при получении"),
    FAILED("Ошибка оплаты"),
    REFUNDED("Возврат средств");

    private final String displayName;
}

