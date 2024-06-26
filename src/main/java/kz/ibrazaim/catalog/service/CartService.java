package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartItemRepository cartItemRepository;

    public void removeCartItem(Long itemId) {
        cartItemRepository.deleteById(itemId);
    }

    public int returnTotalPrice(List<CartItem> cartItems) {
        int totalPrice = 0;
        for (CartItem item : cartItems) {
            totalPrice += item.getProduct().getPrice() * item.getQuantity();
        }
        return totalPrice;
    }

    @Transactional
    public void clearCart(User user) {
        cartItemRepository.deleteAllByUser(user);
        System.out.println("Корзина очищена");
    }
}
