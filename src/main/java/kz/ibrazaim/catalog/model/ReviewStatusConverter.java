package kz.ibrazaim.catalog.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ReviewStatusConverter implements AttributeConverter<ReviewStatus, String> {

    @Override
    public String convertToDatabaseColumn(ReviewStatus status) {
        return status == null ? ReviewStatus.PENDING.name() : status.name();
    }

    @Override
    public ReviewStatus convertToEntityAttribute(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            return ReviewStatus.PENDING;
        }
        try {
            return ReviewStatus.fromValue(dbValue);
        } catch (IllegalArgumentException ex) {
            return ReviewStatus.PENDING;
        }
    }
}

