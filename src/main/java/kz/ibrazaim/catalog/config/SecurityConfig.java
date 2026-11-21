package kz.ibrazaim.catalog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.security.remember-me-key:uxd-dev-remember-me-key-change-in-prod}")
    private String rememberMeKey;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UserDetailsServiceImpl userDetailsService,
            PasswordEncoder passwordEncoder
    ) throws Exception {
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

        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);

        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/payments/webhook")
                )
                .authenticationProvider(authProvider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/products/create",
                                "/products/create/**",
                                "/products/update/**",
                                "/products/delete/**",
                                "/categories",
                                "/categories/**",
                                "/reviews",
                                "/reviews/**",
                                "/sales",
                                "/sales/**",
                                "/orders/updateStatus",
                                "/promos",
                                "/promos/**",
                                "/admin/banners",
                                "/admin/banners/**"
                        ).hasAnyRole("admin", "moder")
                        .requestMatchers(
                                "/products/addComment",
                                "/checkout",
                                "/payment/**",
                                "/orders",
                                "/orders/**",
                                "/profile",
                                "/profile/**"
                        ).authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .failureHandler(authenticationFailureHandler())
                        .successHandler(successHandler)
                        .permitAll()
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .referrerPolicy(ref -> ref.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
                        ))
                )
                .rememberMe(remember -> remember
                        .key(rememberMeKey)
                        .tokenValiditySeconds(86400 * 30)
                        .rememberMeParameter("remember-me")
                        .userDetailsService(userDetailsService)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) -> {
            String email = request.getParameter("email");
            String redirect = request.getParameter("redirect");

            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/login")
                    .queryParam("error", "true");

            if (email != null && !email.isBlank()) {
                builder.queryParam("email", email.trim());
            }
            if (redirect != null
                    && !redirect.isBlank()
                    && redirect.startsWith("/")
                    && !redirect.startsWith("//")
                    && !redirect.contains("://")
                    && !redirect.contains("\\")
                    && !redirect.contains("\r")
                    && !redirect.contains("\n")) {
                builder.queryParam("redirect", redirect);
            }

            String target = builder.build().encode(StandardCharsets.UTF_8).toUriString();
            response.sendRedirect(request.getContextPath() + target);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
