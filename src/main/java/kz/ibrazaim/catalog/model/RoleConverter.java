package kz.ibrazaim.catalog.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role role) {
        return role == null ? null : role.name();
    }

    @Override
    public Role convertToEntityAttribute(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            return null;
        }
        String normalized = dbValue.trim();
        for (Role role : Role.values()) {
            if (role.name().equalsIgnoreCase(normalized)
                    || role.getServiceName().equalsIgnoreCase(normalized)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + dbValue);
    }
}
