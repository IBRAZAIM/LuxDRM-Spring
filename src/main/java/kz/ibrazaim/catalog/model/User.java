package kz.ibrazaim.catalog.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @Column(nullable = false)
    String login;

    @Column(unique = true)
    String email;

    @Column(nullable = false)
    String password;

    String name;

    @Convert(converter = RoleConverter.class)
    @Column(length = 50)
    Role role;

    String surname;

    @Column(length = 40)
    String phone;

    @Column(length = 100)
    String country;

    @Column(length = 100)
    String city;

    @Column(length = 255)
    String address;

    @Column(name = "postal_code", length = 32)
    String postalCode;

    @Column(name = "reset_token", length = 128)
    String resetToken;

    @Column(name = "reset_token_expiry")
    LocalDateTime resetTokenExpiry;

    @Column(name = "registration_date", updatable = false)
    @CreationTimestamp
    LocalDateTime registrationDate;

    @OneToMany(mappedBy = "user")
    List<CartItem> cartItem;
}
