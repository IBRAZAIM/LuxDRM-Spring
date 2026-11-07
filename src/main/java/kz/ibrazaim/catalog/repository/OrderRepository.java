package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Order;
import kz.ibrazaim.catalog.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Override
    @EntityGraph(attributePaths = {"user", "orderProducts", "orderProducts.product"})
    List<Order> findAll();

    @EntityGraph(attributePaths = {"user", "orderProducts", "orderProducts.product"})
    List<Order> findAllByOrderByDateDescIdDesc();

    @EntityGraph(attributePaths = {"user", "orderProducts", "orderProducts.product"})
    List<Order> findByUser(User user);

    @EntityGraph(attributePaths = {"user", "orderProducts", "orderProducts.product"})
    List<Order> findByUserOrderByDateDescIdDesc(User user);

    @EntityGraph(attributePaths = {"user", "orderProducts", "orderProducts.product"})
    Order getOrderById(long id);

    // Количество заказов за период
    @Query("SELECT COUNT(o) FROM Order o WHERE o.date BETWEEN :start AND :end")
    Long countOrdersInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Сумма всех заказов за период
    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.date BETWEEN :start AND :end")
    Double sumTotalInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT FUNCTION('to_char', o.date, 'YYYY-MM-DD'), COUNT(o), COALESCE(SUM(o.totalPrice), 0) " +
            "FROM Order o WHERE o.date BETWEEN :start AND :end " +
            "GROUP BY FUNCTION('to_char', o.date, 'YYYY-MM-DD') ORDER BY 1")
    List<Object[]> aggregateByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT FUNCTION('to_char', o.date, 'IYYY-IW'), COUNT(o), COALESCE(SUM(o.totalPrice), 0) " +
            "FROM Order o WHERE o.date BETWEEN :start AND :end " +
            "GROUP BY FUNCTION('to_char', o.date, 'IYYY-IW') ORDER BY 1")
    List<Object[]> aggregateByWeek(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT FUNCTION('to_char', o.date, 'YYYY-MM'), COUNT(o), COALESCE(SUM(o.totalPrice), 0) " +
            "FROM Order o WHERE o.date BETWEEN :start AND :end " +
            "GROUP BY FUNCTION('to_char', o.date, 'YYYY-MM') ORDER BY 1")
    List<Object[]> aggregateByMonth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
