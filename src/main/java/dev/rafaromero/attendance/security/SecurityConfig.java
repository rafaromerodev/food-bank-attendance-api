package dev.rafaromero.attendance.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP Basic over a stateless API, with two roles.
 *
 * <p>CSRF protection is disabled because there is no session or cookie: every request carries its
 * own Authorization header. To keep it that way, a 401 never includes a {@code WWW-Authenticate:
 * Basic} challenge, so browsers never show their login prompt and never cache credentials that a
 * third-party page could ride on. Requests must be made over HTTPS in any real deployment.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    private static final String COORDINATOR = AppUser.Role.COORDINATOR.name();
    private static final String FRONT_DESK = AppUser.Role.FRONT_DESK.name();

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.authenticationEntryPoint((request, response, e) ->
                        problem(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "Authentication required")))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, e) ->
                                problem(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "Authentication required"))
                        .accessDeniedHandler((request, response, e) ->
                                problem(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden", "Your role cannot perform this action")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Coordinator-only actions: they change files, cards or expose aggregated figures.
                        .requestMatchers(HttpMethod.DELETE, "/api/beneficiaries/*").hasRole(COORDINATOR)
                        .requestMatchers(HttpMethod.POST,
                                "/api/beneficiaries/*/reactivation",
                                "/api/beneficiaries/*/renewal",
                                "/api/beneficiaries/*/cards/replacement").hasRole(COORDINATOR)
                        .requestMatchers("/api/reports/**").hasRole(COORDINATOR)
                        // Everything else in the API is front desk work.
                        .requestMatchers("/api/**").hasAnyRole(FRONT_DESK, COORDINATOR)
                        .anyRequest().denyAll())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .headers(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(AppUserRepository users) {
        return username -> users.findByUsername(username)
                .map(user -> User.withUsername(user.getUsername())
                        .password(user.getPasswordHash())
                        .roles(user.getRole().name())
                        .disabled(!user.isEnabled())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }

    /** Same RFC 9457 shape as the rest of the API's errors. */
    private static void problem(HttpServletResponse response, int status, String title, String detail)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("{\"status\":" + status + ",\"title\":\"" + title + "\",\"detail\":\"" + detail + "\"}");
    }
}
