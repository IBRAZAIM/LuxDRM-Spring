package kz.ibrazaim.catalog.config;

import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailCaseInsensitive(normalized)
                .or(() -> userRepository.findByLoginCaseInsensitive(normalized))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Пользователь с email=" + normalized + " не найден."
                ));

        return new UserDetailsImpl(user);
    }
}
