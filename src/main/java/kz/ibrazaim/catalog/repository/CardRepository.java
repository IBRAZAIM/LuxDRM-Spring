package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Card;
import kz.ibrazaim.catalog.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findAllByUser(User user);
    Optional<Card> findFirstByUserOrderByIdDesc(User user);
}
