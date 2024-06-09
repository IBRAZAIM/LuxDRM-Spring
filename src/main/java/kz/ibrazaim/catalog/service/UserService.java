package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.Role;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.repository.CartItemRepository;
import kz.ibrazaim.catalog.repository.ProductRepository;
import kz.ibrazaim.catalog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService{
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final PasswordEncoder encoder;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    public void create(User user){
        user.setPassword(encoder.encode(user.getPassword()));
        user.setRole(Role.USER);
        user.setRegistrationDate(LocalDateTime.now());
        userRepository.save(user);
    }

    public boolean authenticate(String login, String password) {
        logger.debug("Аутентификация пользователя с помощью логина: {}", login);
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь с логином=" + login + " не найден!"));
        boolean matches = encoder.matches(password, user.getPassword());
        logger.debug("Совпадения паролей: {}", matches);
        return matches;
    }

    public List<CartItem> findALlCartItems() {
        User user = getUser();
        return user.getCartItem();
    }

    public void addItemToCart(long productId) {
        CartItem cartItem = new CartItem();
        cartItem.setUser(getUser());

        Product product = productRepository.findById(productId).orElseThrow();
        cartItem.setProduct(product);
        cartItem.setQuantity(1);

        cartItemRepository.save(cartItem);
    }

    private User getUser() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        return  userRepository.findByLogin(authentication.getName()).orElseThrow();
    }

    public User findById(Long userId) {
        return userRepository.findById(userId).orElseThrow();
    }
}
