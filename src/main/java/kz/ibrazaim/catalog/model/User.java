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
    String login;
    String password;
    String name;
    String role;
    String surname;
    @JoinColumn(name = "registration_date")
    @CreationTimestamp
    LocalDateTime registrationDate;

    @OneToMany(mappedBy = "user")
    List<CartItem> cartItem;
}
