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

    public static final int MIN_QTY = 1;
    public static final int MAX_QTY = 20;

    private final CartItemRepository cartItemRepository;

    public boolean removeCartItem(User user, Long itemId) {
        if (user == null || itemId == null) {
            return false;
        }

        return cartItemRepository.findByIdAndUser(itemId, user)
                .map(cartItem -> {
                    cartItemRepository.delete(cartItem);
                    return true;
                })
                .orElse(false);
    }

    public int totalPrice(List<CartItem> cartItems) {
        return subtotal(cartItems);
    }

    public int subtotal(List<CartItem> cartItems) {
        return safeCartItems(cartItems).stream()
                .mapToInt(this::lineSubtotal)
                .sum();
    }

    public int discountAmount(List<CartItem> cartItems) {
        return safeCartItems(cartItems).stream()
                .mapToInt(this::lineDiscountAmount)
                .sum();
    }

    public int discountPercent(List<CartItem> cartItems) {
        int subtotal = subtotal(cartItems);
        if (subtotal <= 0) {
            return 0;
        }
        return (int) Math.round(discountAmount(cartItems) * 100.0 / subtotal);
    }

    public int finalPrice(List<CartItem> cartItems) {
        return Math.max(0, subtotal(cartItems) - discountAmount(cartItems));
    }

    public int lineSubtotal(CartItem cartItem) {
        if (cartItem == null || cartItem.getProduct() == null) {
            return 0;
        }
        int unitPrice = Math.max(0, cartItem.getProduct().getPrice());
        int quantity = Math.max(MIN_QTY, cartItem.getQuantity());
        return unitPrice * quantity;
    }

    public int lineDiscountAmount(CartItem cartItem) {
        if (cartItem == null || cartItem.getProduct() == null) {
            return 0;
        }

        int discountPercent = clampPercent(cartItem.getProduct().getDiscount());
        if (discountPercent == 0) {
            return 0;
        }

        return (int) Math.round(lineSubtotal(cartItem) * discountPercent / 100.0);
    }

    public int lineFinalPrice(CartItem cartItem) {
        return Math.max(0, lineSubtotal(cartItem) - lineDiscountAmount(cartItem));
    }

    public boolean updateQuantity(User user, Long cartItemId, int quantity) {
        if (user == null || cartItemId == null) {
            return false;
        }
        final int normalizedQuantity = normalizeQuantity(quantity);

        return cartItemRepository.findByIdAndUser(cartItemId, user)
                .map(cartItem -> {
                    cartItem.setQuantity(normalizedQuantity);
                    cartItemRepository.save(cartItem);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public void clearCart(User user) {
        if (user == null) {
            return;
        }
        cartItemRepository.deleteAllByUser(user);
    }

    public int getCartItemsCountByUser(User user) {
        if (user == null) {
            return 0;
        }
        return cartItemRepository.sumQuantityByUser(user);
    }

    public int getCartItemsCountById(Long userId) {
        if (userId == null) {
            return 0;
        }
        return cartItemRepository.sumQuantityByUserId(userId);
    }

    public int getCartRowsCountByUser(User user) {
        if (user == null) {
            return 0;
        }
        return cartItemRepository.countByUser(user);
    }

    private List<CartItem> safeCartItems(List<CartItem> cartItems) {
        return cartItems == null ? List.of() : cartItems;
    }

    private int clampPercent(int discountPercent) {
        return Math.max(0, Math.min(discountPercent, 100));
    }

    public int normalizeQuantity(int quantity) {
        return Math.max(MIN_QTY, Math.min(quantity, MAX_QTY));
    }
}
