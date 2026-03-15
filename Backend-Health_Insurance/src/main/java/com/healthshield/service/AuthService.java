package com.healthshield.service;

import com.healthshield.dto.request.CustomerRegisterRequest;
import com.healthshield.dto.request.LoginRequest;
import com.healthshield.dto.response.AuthResponse;
import com.healthshield.entity.Customer;
import com.healthshield.entity.User;
import com.healthshield.enums.Gender;
import com.healthshield.exception.BadRequestException;
import com.healthshield.exception.UnauthorizedException;
import com.healthshield.repository.CustomerRepository;
import com.healthshield.repository.UserRepository;
import com.healthshield.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService; // ← NEW

    public AuthResponse register(CustomerRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered: " + request.getEmail());
        }

        Customer customer = new Customer();
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setPassword(passwordEncoder.encode(request.getPassword()));
        customer.setPhone(request.getPhone());
        customer.setIsActive(true);
        customer.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) {
            customer.setGender(Gender.valueOf(request.getGender().toUpperCase()));
        }
        customer.setAddress(request.getAddress());
        customer.setCity(request.getCity());
        customer.setState(request.getState());
        customer.setPincode(request.getPincode());

        Customer saved = customerRepository.save(customer);
        String token = jwtUtil.generateToken(saved);
        String refreshToken = refreshTokenService.createRefreshToken(saved.getEmail()); // ← NEW

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(saved.getUserId())
                .firstName(saved.getFirstName())
                .email(saved.getEmail())
                .role(extractRole(saved))
                .message("Registration successful")
                .refreshToken(refreshToken) // ← NEW
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is deactivated. Contact admin.");
        }

        String token = jwtUtil.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user.getEmail()); // ← NEW

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .email(user.getEmail())
                .role(extractRole(user))
                .message("Login successful")
                .refreshToken(refreshToken) // ← NEW
                .build();
    }

    private String extractRole(User user) {
        jakarta.persistence.DiscriminatorValue dv =
                user.getClass().getAnnotation(jakarta.persistence.DiscriminatorValue.class);
        return (dv != null) ? dv.value() : "USER";
    }
}