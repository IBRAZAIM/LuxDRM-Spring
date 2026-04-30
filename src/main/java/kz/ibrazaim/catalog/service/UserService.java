package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.Role;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.repository.CartItemRepository;
import kz.ibrazaim.catalog.repository.ProductRepository;
import kz.ibrazaim.catalog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final Pattern LOGIN_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,50}$");
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 72;

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final PasswordEncoder encoder;

    @Transactional
    public void create(User user) {
        String login = normalizeLogin(user.getLogin());
        String password = normalizePassword(user.getPassword());

        userRepository.lockLoginForRegistration(login);
        if (userRepository.existsByLoginCaseInsensitive(login)) {
            throw new IllegalArgumentException("Пользователь с таким логином уже существует.");
        }

        user.setLogin(login);
        user.setPassword(encoder.encode(password));
        user.setRole(Role.USER.getServiceName());
        user.setName(trimNullable(user.getName()));
        user.setSurname(trimNullable(user.getSurname()));
        user.setRegistrationDate(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void addItemToCart(long productId, int quantity, String size) {
        User user = getUser();
        if (user == null) {
            throw new IllegalStateException("User is not authenticated.");
        }
        int normalizedQuantity = Math.max(1, Math.min(quantity, kz.ibrazaim.catalog.service.CartService.MAX_QTY));
        String normalizedSize = normalizeSize(size);

        CartItem cartItem = cartItemRepository.findByUserAndProductIdAndSize(user, productId, normalizedSize)
                .orElse(null);

        if (cartItem != null) {
            cartItem.setQuantity(Math.min(
                    kz.ibrazaim.catalog.service.CartService.MAX_QTY,
                    cartItem.getQuantity() + normalizedQuantity
            ));
        } else {
            cartItem = new CartItem();
            cartItem.setUser(user);
            cartItem.setSize(normalizedSize);
            cartItem.setQuantity(normalizedQuantity);

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            cartItem.setProduct(product);
        }
        cartItemRepository.save(cartItem);
    }

    public User getUser() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        String login = authentication != null ? authentication.getName() : null;
        if (login == null || login.isBlank()) {
            return null;
        }
        return userRepository.findByLoginCaseInsensitive(login).orElse(null);
    }

    public List<CartItem> findCartItemsByUser(User user) {
        if (user == null) {
            return List.of();
        }
        return cartItemRepository.findByUser(user);
    }

    public boolean hasRole(User user, Role role) {
        return hasAnyRole(user, role);
    }

    public boolean hasAnyRole(User user, Role... roles) {
        if (user == null || user.getRole() == null || roles == null) {
            return false;
        }

        String actualRole = user.getRole().trim();
        for (Role role : roles) {
            if (role != null && actualRole.equalsIgnoreCase(role.getServiceName())) {
                return true;
            }
        }
        return false;
    }

    private String normalizeLogin(String rawLogin) {
        String login = rawLogin == null ? "" : rawLogin.trim().toLowerCase(Locale.ROOT);
        if (login.isBlank()) {
            throw new IllegalArgumentException("Введите логин.");
        }
        if (!LOGIN_PATTERN.matcher(login).matches()) {
            throw new IllegalArgumentException("Логин: 3-50 символов, только латиница, цифры, '.', '_' или '-'.");
        }
        return login;
    }

    private String normalizePassword(String rawPassword) {
        String password = rawPassword == null ? "" : rawPassword;
        if (password.isBlank()) {
            throw new IllegalArgumentException("Введите пароль.");
        }
        if (password.length() < PASSWORD_MIN_LENGTH) {
            throw new IllegalArgumentException("Пароль должен содержать минимум 8 символов.");
        }
        if (password.length() > PASSWORD_MAX_LENGTH) {
            throw new IllegalArgumentException("Пароль слишком длинный.");
        }
        return password;
    }

    private String trimNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeSize(String size) {
        if (size == null) {
            return null;
        }
        String trimmed = size.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
