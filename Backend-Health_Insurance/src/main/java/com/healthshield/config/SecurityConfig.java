package com.healthshield.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ── Public endpoints ──
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/plans/**").permitAll()
                        .requestMatchers("/api/premium/calculate").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/v3/api-docs/**").permitAll()

                        // ── FIX 2: SSE notifications — all authenticated roles ──
                        // Must be BEFORE the role-based rules below
                        .requestMatchers("/api/notifications/**").authenticated()

                        // ── Role-based endpoints ──
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/underwriter/**").hasRole("UNDERWRITER")
                        .requestMatchers("/api/claims-officer/**").hasRole("CLAIMS_OFFICER")

                        // ── Documents ──
                        .requestMatchers("/api/claims/*/documents/*/view").authenticated()
                        .requestMatchers("/api/claims/*/documents/*/download").authenticated()

                        // ── Aadhaar + KYC — underwriter + admin ──
                        .requestMatchers("/api/policies/*/aadhaar/download")
                        .hasAnyRole("ADMIN", "UNDERWRITER")
                        .requestMatchers("/api/policies/*/kyc/**")
                        .hasRole("UNDERWRITER")

                        // ── Other authenticated endpoints ──
                        .requestMatchers("/api/payments/**").authenticated()
                        .requestMatchers("/api/razorpay/**").authenticated()
                        .requestMatchers("/api/profile/**").authenticated()
                        .requestMatchers("/api/claims/*/settle").authenticated()
                        .requestMatchers("/api/agent-requests/**").authenticated()
                        .requestMatchers("/api/underwriter/customers")
                        .hasRole("UNDERWRITER")
                        .requestMatchers("/api/underwriter/agent-requests/**")
                        .hasRole("UNDERWRITER")
                        .requestMatchers("/api/underwriter/apply-direct/**")
                        .hasRole("UNDERWRITER")
                        .requestMatchers("/api/chat/**").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "http://localhost:3000",
                "http://localhost:55983",
                "http://localhost:62486"
        ));
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // FIX 2: Expose Content-Type so Angular can read it from blob responses
        config.setExposedHeaders(List.of(
                "Content-Type",
                "Content-Disposition",
                "Authorization"
        )); 
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}