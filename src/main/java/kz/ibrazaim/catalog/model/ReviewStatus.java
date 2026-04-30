package kz.ibrazaim.catalog.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewStatus {
    PENDING("\u041d\u0435 \u043f\u0440\u043e\u0432\u0435\u0440\u0435\u043d\u043e"),
    APPROVED("\u041f\u0440\u043e\u0432\u0435\u0440\u0435\u043d\u043e");

    private final String displayName;

    public static ReviewStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }

        String normalized = value.trim();
        for (ReviewStatus status : values()) {
            if (status.name().equalsIgnoreCase(normalized)
                    || status.displayName.equalsIgnoreCase(normalized)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Invalid review status: " + value);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
