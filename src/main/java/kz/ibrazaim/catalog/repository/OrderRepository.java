package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Order;
import kz.ibrazaim.catalog.model.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long > {
}