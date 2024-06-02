package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.Role;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService{
    private final UserRepository userRepository;
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

}
