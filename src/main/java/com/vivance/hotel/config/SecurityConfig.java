package com.vivance.hotel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivance.hotel.security.ApiKeyBearerAuthFilter;
import com.vivance.hotel.security.AuthGatewayJwtService;
import com.vivance.hotel.security.HotelAuthenticationProvider;
import com.vivance.hotel.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // ApiKeyBearerAuthFilter is a @Component — auto-registered as a servlet filter at
    // HIGHEST_PRECEDENCE (before Spring Security). Injected here only to wire the
    // HotelAuthenticationProvider; it is NOT added to the security filter chain.
    private final ApiKeyBearerAuthFilter apiKeyBearerAuthFilter;
    private final AuthGatewayJwtService jwtService;
    private final HotelAuthenticationProvider authenticationProvider;
    private final ObjectMapper objectMapper;

    private static final String[] PUBLIC_ENDPOINTS = {
        "/api/v1/auth/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/actuator/health",
        "/api/v1/admin/tbo/static/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.FORBIDDEN)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/hotels/**", "/api/v1/hotels/**").permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider)
                // JwtAuthenticationFilter is not a @Component — safe to add here without
                // double-registration. ApiKeyBearerAuthFilter runs before this chain
                // (servlet level) and sets the JwtAuthToken request attribute.
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, objectMapper),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
