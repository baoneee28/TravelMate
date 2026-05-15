package com.travelmate.controller.page;

import com.travelmate.service.UserService;
import org.springframework.stereotype.Controller;
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

    public AuthPageController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Trang đăng nhập - route: GET /auth/login
     * Template: templates/auth/login.html
     */
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    /**
     * Trang đăng ký - route: GET /auth/register
     * Template: templates/auth/register.html
     */
    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
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
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "redirect:/auth/register";
        }

        // Kiểm tra độ dài mật khẩu
        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu phải có ít nhất 8 ký tự!");
            return "redirect:/auth/register";
        }

        // Kiểm tra email đã tồn tại
        if (userService.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "Email này đã được đăng ký. Vui lòng đăng nhập!");
            return "redirect:/auth/register";
        }

        // Tạo tài khoản mới
        userService.register(firstName, lastName, email, phone, password);

        // Redirect về trang đăng nhập với thông báo thành công
        redirectAttributes.addFlashAttribute("success", "Tạo tài khoản thành công! Vui lòng đăng nhập.");
        return "redirect:/auth/login";
    }
}
