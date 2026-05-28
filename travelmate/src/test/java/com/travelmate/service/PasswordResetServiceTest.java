package com.travelmate.service;

import com.travelmate.entity.PasswordResetToken;
import com.travelmate.entity.User;
import com.travelmate.repository.PasswordResetTokenRepository;
import com.travelmate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService - Token dat lai mat khau")
class PasswordResetServiceTest {

    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(tokenRepository, userRepository, passwordEncoder, true);
    }

    @Test
    @DisplayName("Email ton tai tao token co thoi han va vo hieu token cu")
    void requestingResetCreatesSingleUseExpiringToken() {
        User user = user("user@travelmate.vn");
        PasswordResetToken previous = validToken(user, "old-token");
        when(userRepository.findByEmail("user@travelmate.vn")).thenReturn(Optional.of(user));
        when(tokenRepository.findAllByUserAndUsedFalse(user)).thenReturn(List.of(previous));

        passwordResetService.requestReset(" User@TravelMate.vn ");

        assertThat(previous.getUsed()).isTrue();
        verify(tokenRepository).saveAll(List.of(previous));
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken generated = tokenCaptor.getValue();
        assertThat(generated.getToken()).isNotBlank();
        assertThat(generated.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(28));
    }

    @Test
    @DisplayName("Khong tiet lo email khong ton tai bang cach khong tao token")
    void unknownEmailDoesNotCreateToken() {
        when(userRepository.findByEmail("unknown@travelmate.vn")).thenReturn(Optional.empty());

        passwordResetService.requestReset("unknown@travelmate.vn");
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dat lai mat khau dung token ma hoa mat khau va khoa token")
    void successfulResetChangesPasswordAndConsumesToken() {
        User user = user("user@travelmate.vn");
        PasswordResetToken token = validToken(user, "valid-token");
        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded-new-password");

        passwordResetService.resetPassword("valid-token", "newPassword123", "newPassword123");

        assertThat(user.getPassword()).isEqualTo("encoded-new-password");
        assertThat(token.getUsed()).isTrue();
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    @DisplayName("Token het han khong the dat lai mat khau")
    void expiredTokenIsRejected() {
        PasswordResetToken token = validToken(user("user@travelmate.vn"), "expired-token");
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("expired-token", "newPassword123", "newPassword123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hết hạn");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Token da dung khong the dung lai lan hai")
    void usedTokenIsRejected() {
        PasswordResetToken token = validToken(user("user@travelmate.vn"), "used-token");
        token.setUsed(true);
        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("used-token", "newPassword123", "newPassword123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không hợp lệ");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Mat khau moi qua ngan bi chan truoc khi cap nhat tai khoan")
    void weakPasswordIsRejected() {
        assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token", "1234567", "1234567"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ít nhất 8");
        verify(tokenRepository, never()).findByToken(any());
        verify(userRepository, never()).save(any());
    }

    private static User user(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("encoded-password");
        return user;
    }

    private static PasswordResetToken validToken(User user, String value) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken(value);
        token.setUsed(false);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        return token;
    }
}
