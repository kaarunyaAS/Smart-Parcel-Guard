package com.smartparcelguard.user;

import com.smartparcelguard.exception.ApiException;
import com.smartparcelguard.user.dto.CreateUserRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void rejectsADuplicateEmailBeforeSaving() {
        when(userRepository.existsByUsername("new.operator")).thenReturn(false);
        when(userRepository.existsByEmail("operator@example.com")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> userService.create(new CreateUserRequest(
                "new.operator",
                "operator@example.com",
                "ParcelTest1",
                "New Operator",
                UserRole.OPERATOR
        )));

        assertEquals("Email address is already in use", exception.getMessage());
    }
}
