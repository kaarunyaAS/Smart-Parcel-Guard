package com.smartparcelguard.user;

import com.smartparcelguard.auth.AuthService;
import com.smartparcelguard.common.PageResponse;
import com.smartparcelguard.common.TextSanitizer;
import com.smartparcelguard.exception.ApiException;
import com.smartparcelguard.user.dto.CreateUserRequest;
import com.smartparcelguard.user.dto.UpdateUserRequest;
import com.smartparcelguard.user.dto.UserResponse;
import com.smartparcelguard.user.dto.UserStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UserService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "username", "email", "fullName", "role");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String username = TextSanitizer.lower(request.username());
        String email = TextSanitizer.lower(request.email());
        if (userRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "Username is already in use");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email address is already in use");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(TextSanitizer.required(request.fullName(), "Full name"));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setEnabled(true);
        return AuthService.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(int page, int size, String sort) {
        Page<UserResponse> users = userRepository.findAll(pageRequest(page, size, sort))
                .map(AuthService::toResponse);
        return PageResponse.from(users);
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return AuthService.toResponse(requireUser(id));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = requireUser(id);
        String email = TextSanitizer.lower(request.email());
        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Email address is already in use");
                });
        guardLastAdmin(user, request.role(), user.isEnabled());
        user.setEmail(email);
        user.setFullName(TextSanitizer.required(request.fullName(), "Full name"));
        user.setRole(request.role());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return AuthService.toResponse(user);
    }

    @Transactional
    public UserResponse updateStatus(Long id, UserStatusRequest request) {
        User user = requireUser(id);
        guardLastAdmin(user, user.getRole(), request.enabled());
        user.setEnabled(request.enabled());
        return AuthService.toResponse(user);
    }

    private void guardLastAdmin(User user, UserRole nextRole, boolean nextEnabled) {
        boolean remainsActiveAdmin = nextRole == UserRole.ADMIN && nextEnabled;
        boolean currentlyActiveAdmin = user.getRole() == UserRole.ADMIN && user.isEnabled();
        if (currentlyActiveAdmin && !remainsActiveAdmin
                && userRepository.countByRoleAndEnabled(UserRole.ADMIN, true) <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "The last active administrator cannot be removed");
        }
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private PageRequest pageRequest(int page, int size, String sort) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        String[] parts = sort == null ? new String[]{"createdAt", "desc"} : sort.split(",");
        String field = parts[0];
        if (!SORT_FIELDS.contains(field)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported sort field");
        }
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, field));
    }
}
