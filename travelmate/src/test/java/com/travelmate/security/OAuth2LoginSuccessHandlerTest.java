package com.travelmate.security;

import com.travelmate.entity.User;
import com.travelmate.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2LoginSuccessHandler - Google Login USER only")
class OAuth2LoginSuccessHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private OAuth2LoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2LoginSuccessHandler(userRepository, passwordEncoder);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Google email moi duoc tao tai khoan USER va dang nhap")
    void newGoogleEmailCreatesUserAccount() throws Exception {
        when(userRepository.findByEmail("google.user@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-random-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, oauthAuthentication(
                Map.of("email", " Google.User@gmail.com ", "name", "Nguyen Google", "picture", "/avatar.png")));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("google.user@gmail.com");
        assertThat(savedUser.getName()).isEqualTo("Nguyen Google");
        assertThat(savedUser.getRole()).isEqualTo(User.Role.USER);
        assertThat(savedUser.getStatus()).isEqualTo("ACTIVE");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-random-password");
        assertThat(response.getRedirectedUrl()).isEqualTo("/");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isInstanceOf(CustomUserDetails.class);
    }

    @Test
    @DisplayName("Email Google trung Partner/Admin bi chan, khong tu leo quyen")
    void googleLoginCannotAuthenticatePartnerOrAdminEmail() throws Exception {
        User partner = user("partner@travelmate.vn", User.Role.PARTNER, "ACTIVE");
        when(userRepository.findByEmail("partner@travelmate.vn")).thenReturn(Optional.of(partner));

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, oauthAuthentication(
                Map.of("email", "partner@travelmate.vn", "name", "Partner")));

        verify(userRepository, never()).save(any());
        assertThat(response.getRedirectedUrl()).isEqualTo("/auth/login?oauth2Error=role");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Tai khoan USER bi khoa khong dang nhap duoc bang Google")
    void inactiveUserIsRejected() throws Exception {
        User user = user("locked@travelmate.vn", User.Role.USER, "LOCKED");
        when(userRepository.findByEmail("locked@travelmate.vn")).thenReturn(Optional.of(user));

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, oauthAuthentication(
                Map.of("email", "locked@travelmate.vn", "name", "Locked User")));

        assertThat(response.getRedirectedUrl()).isEqualTo("/auth/login?oauth2Error=inactive");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private Authentication oauthAuthentication(Map<String, Object> attributes) {
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "email");
        return new TestingAuthenticationToken(principal, null, "ROLE_USER");
    }

    private User user(String email, User.Role role, String status) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setName("Existing User");
        user.setShortName("Existing User");
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
