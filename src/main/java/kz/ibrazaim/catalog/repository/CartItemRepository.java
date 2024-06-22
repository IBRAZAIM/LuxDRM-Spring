package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUser(User user);
    CartItem findByUserAndProductId(User user, long productId);

    void deleteAllByUser(User user);
}

