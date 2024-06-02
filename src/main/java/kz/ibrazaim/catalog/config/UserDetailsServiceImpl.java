package kz.ibrazaim.catalog.config;

import kz.ibrazaim.catalog.exception.EntityNotFoundException;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        String password = userRepository.findByLogin(login).orElseThrow().getPassword();
        System.out.println("username: " + login);
        System.out.println("password: " + password);
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь с login=" + login + " не найден!"));

        return new UserDetailsImpl(user);
    }

}
