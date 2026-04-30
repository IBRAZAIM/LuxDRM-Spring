package kz.ibrazaim.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler =
                new SavedRequestAwareAuthenticationSuccessHandler() {
                    @Override
                    public void onAuthenticationSuccess(
                            HttpServletRequest request,
                            HttpServletResponse response,
                            Authentication authentication
                    ) throws IOException, ServletException {
                        String redirect = request.getParameter("redirect");
                        if (isSafeInternalRedirect(redirect)) {
                            clearAuthenticationAttributes(request);
                            new HttpSessionRequestCache().removeRequest(request, response);
                            getRedirectStrategy().sendRedirect(request, response, redirect);
                            return;
                        }

                        super.onAuthenticationSuccess(request, response, authentication);
                    }

                    private boolean isSafeInternalRedirect(String redirect) {
                        return redirect != null
                                && !redirect.isBlank()
                                && redirect.startsWith("/")
                                && !redirect.startsWith("//")
                                && !redirect.contains("://")
                                && !redirect.contains("\\")
                                && !redirect.contains("\r")
                                && !redirect.contains("\n");
                    }
                };
        successHandler.setDefaultTargetUrl("/");

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/products/create",
                                "/products/create/**",
                                "/products/update/**",
                                "/products/delete/**",
                                "/categories/create",
                                "/reviews",
                                "/reviews/**",
                                "/sales",
                                "/sales/**",
                                "/orders/updateStatus"
                        ).hasAnyRole("admin", "moder")
                        .requestMatchers(
                                "/products/addComment",
                                "/cart/**",
                                "/checkout",
                                "/orders",
                                "/orders/**"
                        ).authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .failureUrl("/login?error")
                        .successHandler(successHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
