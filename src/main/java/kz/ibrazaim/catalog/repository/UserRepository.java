package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository  extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);
}
