package kz.ibrazaim.catalog.service;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.model.WishlistItem;
import kz.ibrazaim.catalog.repository.ProductRepository;
import kz.ibrazaim.catalog.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WishlistService {
    private static final Logger logger = LoggerFactory.getLogger(WishlistService.class);
    private static final String GUEST_WISHLIST_SESSION_KEY = "UXD_GUEST_WISHLIST";

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureWishlistTableExists() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wishlist (
                    id BIGSERIAL PRIMARY KEY,
                    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                    created_at TIMESTAMP DEFAULT NOW(),
                    CONSTRAINT uq_wishlist_user_product UNIQUE (user_id, product_id)
                )
                """);
        } catch (Exception e) {
            logger.warn("Could not auto-verify wishlist table: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Set<Long> getGuestSet(HttpSession session, boolean create) {
        if (session == null) return new LinkedHashSet<>();
        Object attr = session.getAttribute(GUEST_WISHLIST_SESSION_KEY);
        if (attr instanceof Set<?>) {
            return (Set<Long>) attr;
        }
        Set<Long> set = new LinkedHashSet<>();
        if (create) {
            session.setAttribute(GUEST_WISHLIST_SESSION_KEY, set);
        }
        return set;
    }

    @Transactional
    public void mergeGuestWishlistToUser(HttpSession session, User user) {
        if (session == null || user == null) return;
        Set<Long> guestIds = getGuestSet(session, false);
        if (guestIds.isEmpty()) return;

        for (Long productId : guestIds) {
            if (productId == null) continue;
            if (wishlistRepository.findByUserAndProductId(user, productId).isEmpty()) {
                productRepository.findByIdAndDeletedFalse(productId).ifPresent(product -> {
                    WishlistItem item = new WishlistItem();
                    item.setUser(user);
                    item.setProduct(product);
                    item.setCreatedAt(LocalDateTime.now());
                    wishlistRepository.save(item);
                });
            }
        }
        session.removeAttribute(GUEST_WISHLIST_SESSION_KEY);
    }

    @Transactional(readOnly = true)
    public List<Long> getWishlistProductIds(User user, HttpSession session) {
        if (user != null) {
            List<WishlistItem> items = wishlistRepository.findAllByUserOrderByIdDesc(user);
            List<Long> ids = new ArrayList<>(items.size());
            for (WishlistItem item : items) {
                if (item.getProduct() != null && !item.getProduct().isDeleted()) {
                    ids.add(item.getProduct().getId());
                }
            }
            return ids;
        }
        return new ArrayList<>(getGuestSet(session, false));
    }

    @Transactional(readOnly = true)
    public List<Product> getWishlistProducts(User user, HttpSession session) {
        if (user != null) {
            List<WishlistItem> items = wishlistRepository.findAllByUserOrderByIdDesc(user);
            List<Product> result = new ArrayList<>(items.size());
            for (WishlistItem item : items) {
                Product p = item.getProduct();
                if (p != null && !p.isDeleted()) {
                    result.add(p);
                }
            }
            return result;
        }
        Set<Long> guestIds = getGuestSet(session, false);
        if (!guestIds.isEmpty()) {
            List<Product> result = new ArrayList<>(guestIds.size());
            for (Long id : guestIds) {
                if (id != null) {
                    productRepository.findByIdAndDeletedFalse(id).ifPresent(result::add);
                }
            }
            return result;
        }
        List<Product> fallback = new ArrayList<>(productRepository.findByDeletedFalse());
        fallback.sort(Comparator.comparingLong(Product::getId));
        return fallback;
    }

    @Transactional
    public boolean toggleProduct(User user, HttpSession session, long productId) {
        if (user != null) {
            mergeGuestWishlistToUser(session, user);
            Optional<WishlistItem> existing = wishlistRepository.findByUserAndProductId(user, productId);
            if (existing.isPresent()) {
                wishlistRepository.delete(existing.get());
                return false;
            }
            Product product = productRepository.findByIdAndDeletedFalse(productId).orElse(null);
            if (product == null) {
                return false;
            }
            WishlistItem item = new WishlistItem();
            item.setUser(user);
            item.setProduct(product);
            item.setCreatedAt(LocalDateTime.now());
            wishlistRepository.save(item);
            return true;
        } else {
            Set<Long> guestIds = getGuestSet(session, true);
            if (guestIds.contains(productId)) {
                guestIds.remove(productId);
                session.setAttribute(GUEST_WISHLIST_SESSION_KEY, guestIds);
                return false;
            } else {
                guestIds.add(productId);
                session.setAttribute(GUEST_WISHLIST_SESSION_KEY, guestIds);
                return true;
            }
        }
    }

    @Transactional
    public void clearWishlist(User user, HttpSession session) {
        if (user != null) {
            wishlistRepository.deleteAllByUser(user);
        }
        if (session != null) {
            session.removeAttribute(GUEST_WISHLIST_SESSION_KEY);
        }
    }
}
