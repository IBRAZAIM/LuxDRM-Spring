package kz.ibrazaim.catalog.service;

import org.springframework.transaction.annotation.Transactional;
import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public BigDecimal totalPrice(List<CartItem> cartItems) {
        return subtotal(cartItems);
    }

    public BigDecimal subtotal(List<CartItem> cartItems) {
        return safeCartItems(cartItems).stream()
                .map(this::lineSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal discountAmount(List<CartItem> cartItems) {
        return safeCartItems(cartItems).stream()
                .map(this::lineDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public int discountPercent(List<CartItem> cartItems) {
        BigDecimal subtotal = subtotal(cartItems);
        if (subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return discountAmount(cartItems)
                .multiply(BigDecimal.valueOf(100))
                .divide(subtotal, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    public BigDecimal finalPrice(List<CartItem> cartItems) {
        return subtotal(cartItems)
                .subtract(discountAmount(cartItems))
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal lineSubtotal(CartItem cartItem) {
        if (cartItem == null || cartItem.getProduct() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal unitPrice = cartItem.getProduct().getPrice();
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            unitPrice = BigDecimal.ZERO;
        }
        int quantity = Math.max(MIN_QTY, cartItem.getQuantity());
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal lineDiscountAmount(CartItem cartItem) {
        if (cartItem == null || cartItem.getProduct() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discountPercent = clampPercent(cartItem.getProduct().getDiscount());
        if (discountPercent.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return lineSubtotal(cartItem)
                .multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal lineFinalPrice(CartItem cartItem) {
        return lineSubtotal(cartItem)
                .subtract(lineDiscountAmount(cartItem))
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
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

    private BigDecimal clampPercent(BigDecimal discountPercent) {
        if (discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return discountPercent.min(BigDecimal.valueOf(100));
    }

    public int normalizeQuantity(int quantity) {
        return Math.max(MIN_QTY, Math.min(quantity, MAX_QTY));
    }
}
