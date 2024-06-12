package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Order;
import kz.ibrazaim.catalog.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
}