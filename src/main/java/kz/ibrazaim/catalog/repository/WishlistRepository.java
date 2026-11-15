package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findAllByUserOrderByIdDesc(User user);

    Optional<WishlistItem> findByUserAndProductId(User user, long productId);

    void deleteByUserAndProductId(User user, long productId);

    void deleteAllByUser(User user);
}

