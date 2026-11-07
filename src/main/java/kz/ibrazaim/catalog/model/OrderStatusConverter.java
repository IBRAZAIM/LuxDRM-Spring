package kz.ibrazaim.catalog.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {

    @Override
    public String convertToDatabaseColumn(OrderStatus status) {
        return status == null ? OrderStatus.PENDING.name() : status.name();
    }

    @Override
    public OrderStatus convertToEntityAttribute(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            return OrderStatus.PENDING;
        }
        try {
            return OrderStatus.fromValue(dbValue);
        } catch (IllegalArgumentException ex) {
            return OrderStatus.PENDING;
        }
    }
}

