package com.example.wavifybe.service;

import com.example.wavifybe.dto.request.AuthRequest;
import com.example.wavifybe.dto.request.RegisterRequest;
import com.example.wavifybe.dto.response.AuthResponse;
import com.example.wavifybe.dto.response.UserResponse;
import com.example.wavifybe.entity.RefreshToken;
import com.example.wavifybe.entity.User;
import com.example.wavifybe.entity.UserRole;
import com.example.wavifybe.exception.AppException;
import com.example.wavifybe.exception.ErrorCode;
import com.example.wavifybe.mapper.UserMapper;
import com.example.wavifybe.repository.RefreshTokenRepository;
import com.example.wavifybe.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    UserMapper userMapper;
    @Mock
    RefreshTokenRepository refreshTokenRepository;
    @Mock
    PlatformTransactionManager transactionManager;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());

        passwordEncoder = spy(new BCryptPasswordEncoder(10));
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                userMapper,
                refreshTokenRepository,
                transactionTemplate
        );
        ReflectionTestUtils.setField(authService, "SIGNER_KEY",
                "your-random-secret-key-with-more-than-64-characters-length-wavify-be-secret-key-2026");
        ReflectionTestUtils.setField(authService, "VALID_DURATION", 900L);
        ReflectionTestUtils.setField(authService, "REFRESHABLE_DURATION", 604800L);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setUsername("wavifyuser");
        user.setRole(UserRole.USER);
        user.setPassword(passwordEncoder.encode("Secret123!"));
    }

    @Test
    void login_validPassword_returnsTokensAndVerifiesPasswordOutsideTransaction() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.login(new AuthRequest(user.getEmail(), "Secret123!"));

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        InOrder order = inOrder(passwordEncoder, transactionManager, refreshTokenRepository);
        order.verify(passwordEncoder).matches("Secret123!", user.getPassword());
        order.verify(transactionManager).getTransaction(any(TransactionDefinition.class));
        order.verify(refreshTokenRepository).save(any(RefreshToken.class));
        order.verify(transactionManager).commit(any());
    }

    @Test
    void login_wrongPassword_throwsUnauthenticatedWithoutOpeningWriteTransaction() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new AuthRequest(user.getEmail(), "WrongPass1!")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);

        verify(refreshTokenRepository, never()).save(any());
        verify(transactionManager, never()).getTransaction(any());
    }

    @Test
    void login_supportsLegacyBcryptCost12HashesWithCost10Encoder() {
        String legacyCost12Hash = new BCryptPasswordEncoder(12).encode("LegacyPass1!");
        user.setPassword(legacyCost12Hash);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.login(new AuthRequest(user.getEmail(), "LegacyPass1!"));

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
    }

    @Test
    void register_hashesPasswordWithCost10() {
        RegisterRequest request = new RegisterRequest("new@example.com", "Secret123!", "newuser");
        User mappedUser = new User();
        mappedUser.setEmail(request.getEmail());
        mappedUser.setUsername(request.getUsername());
        UserResponse expected = UserResponse.builder().email(request.getEmail()).username(request.getUsername()).build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userMapper.toUser(request)).thenReturn(mappedUser);
        when(userRepository.save(mappedUser)).thenReturn(mappedUser);
        when(userMapper.toUserResponse(mappedUser)).thenReturn(expected);

        UserResponse result = authService.register(request);

        assertThat(result).isEqualTo(expected);
        assertThat(mappedUser.getPassword()).startsWith("$2a$10$");
        assertThat(passwordEncoder.matches("Secret123!", mappedUser.getPassword())).isTrue();
    }
}
