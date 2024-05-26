package kz.ibrazaim.catalog.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
@Getter
@RequiredArgsConstructor
public enum Role {
    USER("Пользователь", "user"),
    ADMIN("Администратор", "admin"),
    MODER("Модератор", "moder");

    private final String displayName;
    private final String serviceName;
}
