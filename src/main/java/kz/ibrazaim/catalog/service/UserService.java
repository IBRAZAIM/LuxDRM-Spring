package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.Role;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService{
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    public void create(User user){
        user.setPassword(encoder.encode(user.getPassword()));
        user.setRole(Role.USER);
        user.setRegistrationDate(LocalDateTime.now());
        userRepository.save(user);
    }
}
