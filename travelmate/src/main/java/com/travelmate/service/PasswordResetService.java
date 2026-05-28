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
    private final boolean demoLinkEnabled;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${travelmate.password-reset.show-demo-link:true}") boolean demoLinkEnabled) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoLinkEnabled = demoLinkEnabled;
    }

    @Transactional
    public void requestReset(String email) {
        Optional<User> user = userRepository.findByEmail(email == null ? "" : email.trim().toLowerCase());
        if (user.isEmpty()) {
            return;
        }

        invalidatePreviousTokens(user.get());

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user.get());
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES));
        resetToken.setUsed(false);
        tokenRepository.save(resetToken);

        String resetPath = "/auth/reset-password?token=" + resetToken.getToken();
        if (demoLinkEnabled) {
            LOGGER.info("Local reset-password link for {}: {}", user.get().getEmail(), resetPath);
        }
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
}
