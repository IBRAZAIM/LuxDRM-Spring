package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.exception.UserNotFoundException;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final PasswordEncoder encoder;

    public void create(User user) {
        user.setPassword(encoder.encode(user.getPassword()));
        user.setRole(Role.USER.getServiceName());
        user.setRegistrationDate(LocalDateTime.now());
        userRepository.save(user);
    }

    public void addItemToCart(long productId, int quantity) {
        User user = getUser();
        CartItem cartItem = cartItemRepository.findByUserAndProductId(user, productId);
        if (cartItem != null) {
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
        } else {
            cartItem = new CartItem();
            cartItem.setUser(user);
            Product product = productRepository.findById(productId).orElseThrow();
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
        }
        cartItemRepository.save(cartItem);
    }

    public User getUser() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        String username = authentication != null ? authentication.getName() : null;
        return username != null ? userRepository.findByLogin(username).orElse(null) : null;
    }

    public List<CartItem> findCartItemsByUser(User user) {
        return cartItemRepository.findByUser(user);
    }
}

