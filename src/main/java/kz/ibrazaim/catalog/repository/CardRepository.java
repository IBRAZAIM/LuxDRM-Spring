package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Card;
import kz.ibrazaim.catalog.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    Card findByUser(User user);
}
