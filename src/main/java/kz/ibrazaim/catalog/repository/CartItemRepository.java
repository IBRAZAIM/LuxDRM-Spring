package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Найти все элементы корзины для пользователя
    List<CartItem> findByUser(User user);

    // Найти конкретный товар пользователя
    Optional<CartItem> findByUserAndProductId(User user, long productId);

    // Найти товар с определенным размером
    Optional<CartItem> findByUserAndProductIdAndSize(User user, long productId, String size);

    // Удалить все товары пользователя
    void deleteAllByUser(User user);

    // Посчитать количество товаров пользователя
    int countByUser(User user);
}

