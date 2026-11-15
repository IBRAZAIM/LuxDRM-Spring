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
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 72;

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final PasswordEncoder encoder;
    private final NotificationService notificationService;

    @Transactional
    public void create(User user, String confirmPassword) {
        if (confirmPassword == null || !confirmPassword.equals(user.getPassword())) {
            throw new IllegalArgumentException("Пароли не совпадают.");
        }
        create(user);
    }

    @Transactional
    public void create(User user) {
        String email = normalizeEmail(user.getEmail() != null ? user.getEmail() : user.getLogin());
        String password = normalizePassword(user.getPassword());

        userRepository.lockEmailForRegistration(email);
        if (userRepository.existsByEmailCaseInsensitive(email)
                || userRepository.existsByLoginCaseInsensitive(email)) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует.");
        }

        user.setEmail(email);
        user.setLogin(email);
        user.setPassword(encoder.encode(password));
        user.setRole(Role.USER);
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
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof kz.ibrazaim.catalog.config.UserDetailsImpl userDetails) {
            return userDetails.getUser();
        }
        String principal = authentication.getName();
        if (principal == null || principal.isBlank() || "anonymousUser".equals(principal)) {
            return null;
        }
        return userRepository.findByEmailCaseInsensitive(principal)
                .or(() -> userRepository.findByLoginCaseInsensitive(principal))
                .orElse(null);
    }

    @Transactional
    public User updateProfile(User user, String name, String surname, String email) {
        return updateProfile(
                user,
                name,
                surname,
                email,
                user != null ? user.getPhone() : null,
                user != null ? user.getCountry() : null,
                user != null ? user.getCity() : null,
                user != null ? user.getAddress() : null,
                user != null ? user.getPostalCode() : null
        );
    }

    @Transactional
    public User updateProfile(
            User user,
            String name,
            String surname,
            String email,
            String phone,
            String country,
            String city,
            String address,
            String postalCode
    ) {
        if (user == null) {
            throw new IllegalStateException("Пользователь не авторизован.");
        }
        String normalizedEmail = normalizeEmail(email);
        String cleanName = trimNullable(name);
        if (cleanName == null) {
            throw new IllegalArgumentException("Имя не может быть пустым.");
        }
        String cleanSurname = trimNullable(surname);

        userRepository.findByEmailCaseInsensitive(normalizedEmail).ifPresent(existing -> {
            if (existing.getId() != user.getId()) {
                throw new IllegalArgumentException("Этот email уже используется другим аккаунтом.");
            }
        });

        User managed = userRepository.findById(user.getId()).orElse(user);
        managed.setName(cleanName);
        managed.setSurname(cleanSurname);
        managed.setEmail(normalizedEmail);
        managed.setLogin(normalizedEmail);
        managed.setPhone(trimNullable(phone));
        managed.setCountry(trimNullable(country));
        managed.setCity(trimNullable(city));
        managed.setAddress(trimNullable(address));
        managed.setPostalCode(trimNullable(postalCode));
        User saved = userRepository.save(managed);

        user.setName(saved.getName());
        user.setSurname(saved.getSurname());
        user.setEmail(saved.getEmail());
        user.setLogin(saved.getLogin());
        user.setPhone(saved.getPhone());
        user.setCountry(saved.getCountry());
        user.setCity(saved.getCity());
        user.setAddress(saved.getAddress());
        user.setPostalCode(saved.getPostalCode());
        return saved;
    }

    @Transactional
    public void saveDeliveryDetailsIfBlank(
            User user,
            String phone,
            String country,
            String city,
            String address,
            String postalCode
    ) {
        if (user == null) {
            return;
        }
        User managed = userRepository.findById(user.getId()).orElse(null);
        if (managed == null) {
            return;
        }
        boolean changed = false;
        if (isBlank(managed.getPhone()) && !isBlank(phone)) {
            managed.setPhone(phone.trim());
            changed = true;
        }
        if (isBlank(managed.getCountry()) && !isBlank(country)) {
            managed.setCountry(country.trim());
            changed = true;
        }
        if (isBlank(managed.getCity()) && !isBlank(city)) {
            managed.setCity(city.trim());
            changed = true;
        }
        if (isBlank(managed.getAddress()) && !isBlank(address)) {
            managed.setAddress(address.trim());
            changed = true;
        }
        if (isBlank(managed.getPostalCode()) && !isBlank(postalCode)) {
            managed.setPostalCode(postalCode.trim());
            changed = true;
        }
        if (changed) {
            User saved = userRepository.save(managed);
            user.setPhone(saved.getPhone());
            user.setCountry(saved.getCountry());
            user.setCity(saved.getCity());
            user.setAddress(saved.getAddress());
            user.setPostalCode(saved.getPostalCode());
        }
    }

    @Transactional
    public String initiatePasswordReset(String rawEmail, String baseUrl) {
        String lookup = trimNullable(rawEmail);
        if (lookup == null) {
            throw new IllegalArgumentException("Введите email или логин.");
        }
        User user = userRepository.findByEmailCaseInsensitive(lookup)
                .or(() -> userRepository.findByLoginCaseInsensitive(lookup))
                .orElseThrow(() -> new IllegalArgumentException("Пользователь с таким email не найден."));

        String token = UUID.randomUUID().toString().replace("-", "");
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(2));
        userRepository.save(user);

        String resetLink = baseUrl + "/reset-password?token=" + token;
        notificationService.notifyPasswordReset(user, resetLink);
        return resetLink;
    }

    public boolean isResetTokenValid(String token) {
        String cleanToken = trimNullable(token);
        if (cleanToken == null) {
            return false;
        }
        return userRepository.findByResetToken(cleanToken)
                .filter(u -> u.getResetTokenExpiry() != null && u.getResetTokenExpiry().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    @Transactional
    public void resetPasswordWithToken(String token, String newPassword, String confirmPassword) {
        String cleanToken = trimNullable(token);
        if (cleanToken == null) {
            throw new IllegalArgumentException("Недействительная ссылка для сброса пароля.");
        }
        User user = userRepository.findByResetToken(cleanToken)
                .filter(u -> u.getResetTokenExpiry() != null && u.getResetTokenExpiry().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException("Ссылка для сброса пароля устарела или недействительна."));

        if (confirmPassword == null || !confirmPassword.equals(newPassword)) {
            throw new IllegalArgumentException("Пароли не совпадают.");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Пароль должен содержать не менее 6 символов.");
        }
        if (newPassword.length() > PASSWORD_MAX_LENGTH) {
            throw new IllegalArgumentException("Пароль слишком длинный.");
        }

        user.setPassword(encoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    private boolean isBlank(String val) {
        return val == null || val.trim().isEmpty();
    }

    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (user == null) {
            throw new IllegalStateException("Пользователь не авторизован.");
        }
        if (currentPassword == null || currentPassword.isEmpty()) {
            throw new IllegalArgumentException("Введите текущий пароль.");
        }
        User managed = userRepository.findById(user.getId()).orElse(user);
        if (!encoder.matches(currentPassword, managed.getPassword())) {
            throw new IllegalArgumentException("Текущий пароль указан неверно.");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Новый пароль должен содержать не менее 6 символов.");
        }
        if (newPassword.length() > PASSWORD_MAX_LENGTH) {
            throw new IllegalArgumentException("Новый пароль слишком длинный.");
        }
        String encoded = encoder.encode(newPassword);
        managed.setPassword(encoded);
        userRepository.save(managed);
        user.setPassword(encoded);
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

        for (Role role : roles) {
            if (user.getRole() == role) {
                return true;
            }
        }
        return false;
    }

    private String normalizeEmail(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
        if (email.isBlank()) {
            throw new IllegalArgumentException("Введите email.");
        }
        if (email.length() > 255 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Введите корректный email.");
        }
        return email;
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
