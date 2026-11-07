package kz.ibrazaim.catalog.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.transaction.annotation.Transactional;
import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.repository.CartItemRepository;
import kz.ibrazaim.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CartService {

    public static final int MIN_QTY = 1;
    public static final int MAX_QTY = 20;
    public static final String GUEST_CART_SESSION_KEY = "UXD_GUEST_CART";

    public record GuestCartEntry(long id, long productId, String size, int quantity) implements Serializable {}

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @SuppressWarnings("unchecked")
    private List<GuestCartEntry> getSessionEntries(HttpSession session, boolean create) {
        if (session == null) {
            return new ArrayList<>();
        }
        Object attr = session.getAttribute(GUEST_CART_SESSION_KEY);
        if (attr instanceof List<?>) {
            return (List<GuestCartEntry>) attr;
        }
        if (create) {
            List<GuestCartEntry> list = new ArrayList<>();
            session.setAttribute(GUEST_CART_SESSION_KEY, list);
            return list;
        }
        return new ArrayList<>();
    }

    public void addGuestItem(HttpSession session, long productId, int quantity, String size) {
        if (session == null) {
            return;
        }
        int normalizedQty = normalizeQuantity(quantity);
        String normalizedSize = (size == null || size.trim().isEmpty()) ? null : size.trim();
        List<GuestCartEntry> entries = getSessionEntries(session, true);

        for (int i = 0; i < entries.size(); i++) {
            GuestCartEntry e = entries.get(i);
            if (e.productId() == productId && Objects.equals(e.size(), normalizedSize)) {
                int newQty = normalizeQuantity(e.quantity() + normalizedQty);
                entries.set(i, new GuestCartEntry(e.id(), productId, normalizedSize, newQty));
                session.setAttribute(GUEST_CART_SESSION_KEY, entries);
                return;
            }
        }

        long nextId = entries.stream().mapToLong(GuestCartEntry::id).max().orElse(0L) + 1L;
        entries.add(new GuestCartEntry(nextId, productId, normalizedSize, normalizedQty));
        session.setAttribute(GUEST_CART_SESSION_KEY, entries);
    }

    public List<CartItem> getGuestCartItems(HttpSession session) {
        List<GuestCartEntry> entries = getSessionEntries(session, false);
        if (entries.isEmpty()) {
            return List.of();
        }
        List<CartItem> items = new ArrayList<>(entries.size());
        for (GuestCartEntry entry : entries) {
            Product product = productRepository.findByIdAndDeletedFalse(entry.productId()).orElse(null);
            if (product != null) {
                CartItem ci = new CartItem();
                ci.setId(entry.id());
                ci.setProduct(product);
                ci.setSize(entry.size());
                ci.setQuantity(entry.quantity());
                items.add(ci);
            }
        }
        return items;
    }

    public boolean updateGuestQuantity(HttpSession session, Long itemId, int quantity) {
        if (session == null || itemId == null) {
            return false;
        }
        List<GuestCartEntry> entries = getSessionEntries(session, false);
        int normalizedQty = normalizeQuantity(quantity);
        for (int i = 0; i < entries.size(); i++) {
            GuestCartEntry e = entries.get(i);
            if (e.id() == itemId) {
                entries.set(i, new GuestCartEntry(e.id(), e.productId(), e.size(), normalizedQty));
                session.setAttribute(GUEST_CART_SESSION_KEY, entries);
                return true;
            }
        }
        return false;
    }

    public boolean removeGuestItem(HttpSession session, Long itemId) {
        if (session == null || itemId == null) {
            return false;
        }
        List<GuestCartEntry> entries = getSessionEntries(session, false);
        Iterator<GuestCartEntry> it = entries.iterator();
        while (it.hasNext()) {
            if (it.next().id() == itemId) {
                it.remove();
                session.setAttribute(GUEST_CART_SESSION_KEY, entries);
                return true;
            }
        }
        return false;
    }

    public void clearGuestCart(HttpSession session) {
        if (session != null) {
            session.removeAttribute(GUEST_CART_SESSION_KEY);
        }
    }

    public int getGuestCartItemsCount(HttpSession session) {
        return getSessionEntries(session, false).stream()
                .mapToInt(e -> Math.max(0, e.quantity()))
                .sum();
    }

    @Transactional
    public void mergeGuestCartToUser(HttpSession session, User user) {
        if (session == null || user == null) {
            return;
        }
        List<GuestCartEntry> entries = getSessionEntries(session, false);
        if (entries.isEmpty()) {
            return;
        }
        for (GuestCartEntry entry : entries) {
            Product product = productRepository.findByIdAndDeletedFalse(entry.productId()).orElse(null);
            if (product == null) {
                continue;
            }
            CartItem existing = cartItemRepository.findByUserAndProductIdAndSize(user, entry.productId(), entry.size())
                    .orElse(null);
            if (existing != null) {
                existing.setQuantity(normalizeQuantity(existing.getQuantity() + entry.quantity()));
                cartItemRepository.save(existing);
            } else {
                CartItem ci = new CartItem();
                ci.setUser(user);
                ci.setProduct(product);
                ci.setSize(entry.size());
                ci.setQuantity(normalizeQuantity(entry.quantity()));
                cartItemRepository.save(ci);
            }
        }
        clearGuestCart(session);
    }

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

    public int getCartItemsCount(User user, HttpSession session) {
        if (user != null) {
            mergeGuestCartToUser(session, user);
            return cartItemRepository.sumQuantityByUser(user);
        }
        return getGuestCartItemsCount(session);
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
