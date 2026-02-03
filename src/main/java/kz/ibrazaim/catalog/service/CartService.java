package kz.ibrazaim.catalog.service;

import jakarta.transaction.Transactional;
import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;

    public void removeCartItem(Long itemId) {
        cartItemRepository.deleteById(itemId);
    }

    // Подсчет общей цены корзины
    public int totalPrice(List<CartItem> cartItems) {
        return cartItems.stream()
                .mapToInt(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();
    }

    public void updateQuantity(Long cartItemId, int quantity){
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("CartItem not found"));
        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
    }

    @Transactional
    public void clearCart(User user) {
        cartItemRepository.deleteAllByUser(user);
        System.out.println("Корзина очищена");
    }

    //  Получение количества товаров в корзине для конкретного пользователя
    public int getCartItemsCountByUser(User user) {
        if (user == null) return 0;
        return cartItemRepository.countByUser(user);
    }

    // Если нужен count по userId
    public int getCartItemsCountById(Long userId) {
        // Получаем User по userId (например через UserService)
        User user = new User();
        user.setId(userId);
        return cartItemRepository.countByUser(user);
    }
}
