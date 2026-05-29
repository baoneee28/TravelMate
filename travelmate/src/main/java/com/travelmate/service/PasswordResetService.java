package com.travelmate.service;

import com.travelmate.entity.PasswordResetToken;
import com.travelmate.entity.User;
import com.travelmate.repository.PasswordResetTokenRepository;
import com.travelmate.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int EXPIRATION_MINUTES = 30;

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String appBaseUrl;
    private final EmailDeliveryMode emailDeliveryMode;
    private final boolean demoLinkEnabled;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService,
                                @Value("${travelmate.app-base-url:http://localhost:8080}") String appBaseUrl,
                                @Value("${travelmate.password-reset.email-enabled:auto}") String emailDeliveryMode,
                                @Value("${travelmate.password-reset.show-demo-link:false}") boolean demoLinkEnabled) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.appBaseUrl = appBaseUrl;
        this.emailDeliveryMode = EmailDeliveryMode.from(emailDeliveryMode);
        this.demoLinkEnabled = demoLinkEnabled;
    }

    @Transactional
    public PasswordResetRequestResult requestReset(String email) {
        String normalizedEmail = normalizeEmail(email);
        Optional<User> user = userRepository.findByEmail(normalizedEmail);
        if (user.isEmpty()) {
            return new PasswordResetRequestResult(PasswordResetRequestStatus.ACCEPTED, maskEmail(normalizedEmail));
        }

        boolean smtpConfigured = emailService.isConfigured();
        boolean shouldSendEmail = shouldSendEmail(smtpConfigured);
        if (!shouldSendEmail && !demoLinkEnabled) {
            LOGGER.warn("Password reset requested for {}, but SMTP is not configured and demo reset links are disabled.",
                    user.get().getEmail());
            return new PasswordResetRequestResult(PasswordResetRequestStatus.ACCEPTED, maskEmail(normalizedEmail));
        }

        invalidatePreviousTokens(user.get());

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user.get());
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES));
        resetToken.setUsed(false);
        tokenRepository.save(resetToken);

        String resetPath = "/auth/reset-password?token=" + resetToken.getToken();
        String resetUrl = buildResetUrl(resetPath);
        if (shouldSendEmail) {
            try {
                emailService.sendPasswordResetEmail(user.get().getEmail(), resetUrl);
                return new PasswordResetRequestResult(PasswordResetRequestStatus.EMAIL_SENT, maskEmail(normalizedEmail));
            } catch (RuntimeException ex) {
                LOGGER.warn("Failed to send reset-password email to {}: {}", user.get().getEmail(), ex.getMessage());
                resetToken.setUsed(true);
                tokenRepository.save(resetToken);
                return new PasswordResetRequestResult(PasswordResetRequestStatus.EMAIL_SEND_FAILED, maskEmail(normalizedEmail));
            }
        }
        if (demoLinkEnabled) {
            LOGGER.info("Local reset-password link for {}: {}", user.get().getEmail(), resetUrl);
            return new PasswordResetRequestResult(PasswordResetRequestStatus.DEMO_LINK_CREATED, maskEmail(normalizedEmail));
        }

        return new PasswordResetRequestResult(PasswordResetRequestStatus.ACCEPTED, maskEmail(normalizedEmail));
    }

    public boolean isValidToken(String token) {
        return loadUsableToken(token).isPresent();
    }

    @Transactional
    public void resetPassword(String token, String password, String confirmPassword) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 8 ký tự.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }

        PasswordResetToken resetToken = loadUsableToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn."));

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    private Optional<PasswordResetToken> loadUsableToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return tokenRepository.findByToken(token)
                .filter(value -> value.isUsableAt(LocalDateTime.now()));
    }

    private void invalidatePreviousTokens(User user) {
        List<PasswordResetToken> activeTokens = tokenRepository.findAllByUserAndUsedFalse(user);
        activeTokens.forEach(token -> token.setUsed(true));
        if (!activeTokens.isEmpty()) {
            tokenRepository.saveAll(activeTokens);
        }
    }

    private boolean shouldSendEmail(boolean smtpConfigured) {
        return switch (emailDeliveryMode) {
            case ENABLED, AUTO -> smtpConfigured;
            case DISABLED -> false;
        };
    }

    private String buildResetUrl(String resetPath) {
        String base = appBaseUrl == null || appBaseUrl.isBlank()
                ? "http://localhost:8080"
                : appBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + resetPath;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "email này";
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + (at >= 0 ? email.substring(at) : "");
        }
        String prefix = email.substring(0, Math.min(2, at));
        return prefix + "***" + email.substring(at);
    }

    public record PasswordResetRequestResult(PasswordResetRequestStatus status, String maskedEmail) {
    }

    public enum PasswordResetRequestStatus {
        ACCEPTED,
        EMAIL_SENT,
        EMAIL_SEND_FAILED,
        DEMO_LINK_CREATED
    }

    private enum EmailDeliveryMode {
        AUTO,
        ENABLED,
        DISABLED;

        static EmailDeliveryMode from(String value) {
            if (value == null || value.isBlank()) {
                return AUTO;
            }
            return switch (value.trim().toLowerCase()) {
                case "true", "enabled", "on", "yes" -> ENABLED;
                case "false", "disabled", "off", "no" -> DISABLED;
                default -> AUTO;
            };
        }
    }
}
