package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository  extends JpaRepository<User, Long> {
    @Query(value = "SELECT pg_advisory_xact_lock(hashtext(lower(:login)))", nativeQuery = true)
    void lockLoginForRegistration(@Param("login") String login);

    @Query("select u from User u where lower(u.login) = lower(:login)")
    Optional<User> findByLoginCaseInsensitive(@Param("login") String login);

    @Query("select case when count(u) > 0 then true else false end from User u where lower(u.login) = lower(:login)")
    boolean existsByLoginCaseInsensitive(@Param("login") String login);
}
