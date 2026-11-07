package com.smartparcelguard.auth;

import com.smartparcelguard.auth.dto.AuthResponse;
import com.smartparcelguard.auth.dto.LoginRequest;
import com.smartparcelguard.auth.dto.RegisterRequest;
import com.smartparcelguard.common.TextSanitizer;
import com.smartparcelguard.exception.ApiException;
import com.smartparcelguard.security.JwtService;
import com.smartparcelguard.security.RevokedToken;
import com.smartparcelguard.security.RevokedTokenRepository;
import com.smartparcelguard.user.User;
import com.smartparcelguard.user.UserRepository;
import com.smartparcelguard.user.UserRole;
import com.smartparcelguard.user.dto.UserResponse;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RevokedTokenRepository revokedTokenRepository;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RevokedTokenRepository revokedTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = TextSanitizer.lower(request.username());
        String email = TextSanitizer.lower(request.email());
        String fullName = TextSanitizer.required(request.fullName(), "Full name");
        rejectDuplicates(username, email, null);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.setRole(userRepository.count() == 0 ? UserRole.ADMIN : UserRole.OPERATOR);
        return tokenFor(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String login = TextSanitizer.required(request.username(), "Username");
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Account is disabled");
        }
        return tokenFor(user);
    }

    @Transactional
    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        Claims claims = jwtService.parse(authorizationHeader.substring(7).trim());
        revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
        if (revokedTokenRepository.existsByTokenId(claims.getId())) {
            return;
        }
        RevokedToken revoked = new RevokedToken();
        revoked.setTokenId(claims.getId());
        revoked.setExpiresAt(claims.getExpiration().toInstant());
        revoked.setRevokedAt(Instant.now());
        revokedTokenRepository.save(revoked);
    }

    private AuthResponse tokenFor(User user) {
        String token = jwtService.generateToken(user.getUsername(), user.getRole().name(), user.getId());
        return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds(), toResponse(user));
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isEnabled()
        );
    }

    private void rejectDuplicates(String username, String email, Long currentId) {
        userRepository.findByUsername(username)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Username is already in use");
                });
        userRepository.findByEmail(email)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Email address is already in use");
                });
    }
}
