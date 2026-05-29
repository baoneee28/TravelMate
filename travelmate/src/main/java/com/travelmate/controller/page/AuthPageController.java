package com.travelmate.controller.page;

import com.travelmate.service.PasswordResetService;
import com.travelmate.service.PasswordResetService.PasswordResetRequestResult;
import com.travelmate.service.UserService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AuthPageController - Page Controller cho các trang đăng nhập / đăng ký.
 *
 * Các route này PUBLIC (không cần đăng nhập).
 * Spring Security xử lý POST /auth/login nên controller này chỉ cần GET /auth/login.
 * POST /auth/register được controller này xử lý thủ công.
 */
@Controller
@RequestMapping("/auth")
public class AuthPageController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository;
    private final MessageSource messageSource;
    private final boolean googleOAuthEnabled;

    public AuthPageController(UserService userService,
                              PasswordResetService passwordResetService,
                              ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
                              MessageSource messageSource,
                              @Value("${travelmate.oauth2.google.enabled:true}") boolean googleOAuthEnabled) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.messageSource = messageSource;
        this.googleOAuthEnabled = googleOAuthEnabled;
    }

    /**
     * Trang đăng nhập - route: GET /auth/login
     * Template: templates/auth/login.html
     */
    @GetMapping("/login")
    public String loginPage(Model model) {
        addOAuth2Model(model);
        return "auth/login";
    }

    /**
     * Trang đăng ký - route: GET /auth/register
     * Template: templates/auth/register.html
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        addOAuth2Model(model);
        return "auth/register";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email,
                                       RedirectAttributes redirectAttributes) {
        PasswordResetRequestResult result = passwordResetService.requestReset(email);
        addResetRequestFeedback(redirectAttributes, result);
        return "redirect:/auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(value = "token", required = false) String token,
                                    Model model) {
        model.addAttribute("token", token);
        model.addAttribute("tokenValid", passwordResetService.isValidToken(token));
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam("token") String token,
                                      @RequestParam("password") String password,
                                      @RequestParam("confirmPassword") String confirmPassword,
                                      RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.resetPassword(token, password, confirmPassword);
            redirectAttributes.addFlashAttribute("success", message("auth.reset.success"));
            return "redirect:/auth/login";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", message("auth.reset.failure"));
            redirectAttributes.addAttribute("token", token);
            return "redirect:/auth/reset-password";
        }
    }

    /**
     * Xử lý đăng ký - route: POST /auth/register
     * Nhận dữ liệu form, validate cơ bản, lưu user vào DB, rồi redirect về trang đăng nhập.
     */
    @PostMapping("/register")
    public String handleRegister(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes
    ) {
        // Kiểm tra mật khẩu khớp
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", message("auth.register.error.passwordMismatch"));
            return "redirect:/auth/register";
        }

        // Kiểm tra độ dài mật khẩu
        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute("error", message("auth.register.error.passwordLength"));
            return "redirect:/auth/register";
        }

        // Kiểm tra email đã tồn tại
        if (userService.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("error", message("auth.register.error.emailUsed"));
            return "redirect:/auth/register";
        }

        // Tạo tài khoản mới
        userService.register(firstName, lastName, email, phone, password);

        // Redirect về trang đăng nhập với thông báo thành công
        redirectAttributes.addFlashAttribute("success", message("auth.register.success"));
        return "redirect:/auth/login";
    }

    private void addOAuth2Model(Model model) {
        boolean configured = googleOAuthEnabled && clientRegistrationRepository.getIfAvailable() != null;
        model.addAttribute("googleOAuthConfigured", configured);
        model.addAttribute("googleOAuthDisabledReason",
                configured ? "" : message("auth.google.disabledReason"));
    }

    private String message(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    private String message(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    private void addResetRequestFeedback(RedirectAttributes redirectAttributes,
                                         PasswordResetRequestResult result) {
        String maskedEmail = result == null ? "email này" : result.maskedEmail();
        redirectAttributes.addFlashAttribute("resetSubmitted", true);
        redirectAttributes.addFlashAttribute("resetMessageType", "success");
        redirectAttributes.addFlashAttribute("resetIcon", "fa-solid fa-envelope-circle-check");
        redirectAttributes.addFlashAttribute("resetTitle", message("auth.forgot.acceptedTitle"));
        redirectAttributes.addFlashAttribute("resetBody", message("auth.forgot.acceptedBody", maskedEmail));
    }
}
