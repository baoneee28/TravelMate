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

import static com.travelmate.service.PasswordResetService.PasswordResetRequestStatus.ACCEPTED;
import static com.travelmate.service.PasswordResetService.PasswordResetRequestStatus.DEMO_LINK_CREATED;
import static com.travelmate.service.PasswordResetService.PasswordResetRequestStatus.EMAIL_SEND_FAILED;
import static com.travelmate.service.PasswordResetService.PasswordResetRequestStatus.EMAIL_SENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService - Token dat lai mat khau")
class PasswordResetServiceTest {

    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                tokenRepository,
                userRepository,
                passwordEncoder,
                emailService,
                "https://demo.travelmate.vn",
                "auto",
                false);
    }

    @Test
    @DisplayName("Email ton tai va SMTP san sang thi gui mail that, tao token co thoi han va vo hieu token cu")
    void requestingResetSendsEmailAndCreatesSingleUseExpiringToken() {
        User user = user("user@travelmate.vn");
        PasswordResetToken previous = validToken(user, "old-token");
        when(userRepository.findByEmail("user@travelmate.vn")).thenReturn(Optional.of(user));
        when(emailService.isConfigured()).thenReturn(true);
        when(tokenRepository.findAllByUserAndUsedFalse(user)).thenReturn(List.of(previous));

        PasswordResetService.PasswordResetRequestResult result =
                passwordResetService.requestReset(" User@TravelMate.vn ");

        assertThat(result.status()).isEqualTo(EMAIL_SENT);
        assertThat(result.maskedEmail()).isEqualTo("us***@travelmate.vn");
        assertThat(previous.getUsed()).isTrue();
        verify(tokenRepository).saveAll(List.of(previous));
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken generated = tokenCaptor.getValue();
        assertThat(generated.getToken()).isNotBlank();
        assertThat(generated.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(28));
        verify(emailService).sendPasswordResetEmail(eq("user@travelmate.vn"),
                org.mockito.ArgumentMatchers.startsWith("https://demo.travelmate.vn/auth/reset-password?token="));
    }

    @Test
    @DisplayName("Khong tiet lo email khong ton tai bang cach khong tao token")
    void unknownEmailDoesNotCreateToken() {
        when(userRepository.findByEmail("unknown@travelmate.vn")).thenReturn(Optional.empty());

        PasswordResetService.PasswordResetRequestResult result =
                passwordResetService.requestReset("unknown@travelmate.vn");

        assertThat(result.status()).isEqualTo(ACCEPTED);
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    @DisplayName("Chua cau hinh SMTP va khong bat demo thi khong tao token, UI van tra thong bao chung")
    void missingSmtpWithoutDemoDoesNotCreateToken() {
        User user = user("user@travelmate.vn");
        when(userRepository.findByEmail("user@travelmate.vn")).thenReturn(Optional.of(user));
        when(emailService.isConfigured()).thenReturn(false);

        PasswordResetService.PasswordResetRequestResult result =
                passwordResetService.requestReset("user@travelmate.vn");

        assertThat(result.status()).isEqualTo(ACCEPTED);
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    @DisplayName("Che do demo duoc bat thi tao token va log link local, khong gui mail")
    void demoModeCanCreateLocalResetLink() {
        PasswordResetService demoService = new PasswordResetService(
                tokenRepository,
                userRepository,
                passwordEncoder,
                emailService,
                "https://demo.travelmate.vn/",
                "disabled",
                true);
        User user = user("user@travelmate.vn");
        when(userRepository.findByEmail("user@travelmate.vn")).thenReturn(Optional.of(user));
        when(tokenRepository.findAllByUserAndUsedFalse(user)).thenReturn(List.of());

        PasswordResetService.PasswordResetRequestResult result =
                demoService.requestReset("user@travelmate.vn");

        assertThat(result.status()).isEqualTo(DEMO_LINK_CREATED);
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    @DisplayName("Gui email that bai thi token vua tao bi vo hieu hoa")
    void emailFailureConsumesGeneratedToken() {
        User user = user("user@travelmate.vn");
        when(userRepository.findByEmail("user@travelmate.vn")).thenReturn(Optional.of(user));
        when(emailService.isConfigured()).thenReturn(true);
        when(tokenRepository.findAllByUserAndUsedFalse(user)).thenReturn(List.of());
        doThrow(new IllegalStateException("SMTP rejected"))
                .when(emailService).sendPasswordResetEmail(eq("user@travelmate.vn"), any());

        PasswordResetService.PasswordResetRequestResult result =
                passwordResetService.requestReset("user@travelmate.vn");

        assertThat(result.status()).isEqualTo(EMAIL_SEND_FAILED);
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository, times(2)).save(tokenCaptor.capture());
        PasswordResetToken consumed = tokenCaptor.getAllValues().get(1);
        assertThat(consumed.getUsed()).isTrue();
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

    @Test
    @DisplayName("Mat khau xac nhan khong khop thi bi chan")
    void mismatchedPasswordConfirmationIsRejected() {
        assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token", "newPassword123", "otherPassword123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không khớp");
        verify(tokenRepository, never()).findByToken(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Token rong khong hop le va khong truy van repository")
    void blankTokenIsInvalid() {
        assertThat(passwordResetService.isValidToken(" ")).isFalse();
        verify(tokenRepository, never()).findByToken(any());
    }

    @Test
    @DisplayName("Token con han va chua dung duoc xem la hop le")
    void usableTokenIsValid() {
        PasswordResetToken token = validToken(user("user@travelmate.vn"), "valid-token");
        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        assertThat(passwordResetService.isValidToken("valid-token")).isTrue();
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
