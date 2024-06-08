package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

}
