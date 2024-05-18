package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository  extends JpaRepository<User, Long> {
    void findUserById(long id);
}
