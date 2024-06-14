package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {
    void deleteAllByProductId(long productId);
}
