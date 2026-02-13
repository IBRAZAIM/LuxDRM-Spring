package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Order;
import kz.ibrazaim.catalog.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
    Order getOrderById(long id);

    // Количество заказов за период
    @Query("SELECT COUNT(o) FROM Order o WHERE o.date BETWEEN :start AND :end")
    Long countOrdersInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Сумма всех заказов за период
    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.date BETWEEN :start AND :end")
    Double sumTotalInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
