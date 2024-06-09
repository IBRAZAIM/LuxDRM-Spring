package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {
}
