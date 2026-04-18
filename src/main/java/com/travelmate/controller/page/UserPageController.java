package com.travelmate.controller.page;

import com.travelmate.dto.response.AccommodationResponse;
import com.travelmate.dto.response.BookingResponse;
import com.travelmate.entity.User;
import com.travelmate.exception.ResourceNotFoundException;
import com.travelmate.repository.UserRepository;
import com.travelmate.service.AccommodationService;
import com.travelmate.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * UserPageController - Controller xử lý các trang dành cho USER đã đăng nhập.
 *
 * Tất cả URL bắt đầu bằng /user/** được bảo vệ bởi Spring Security:
 * - Phải đăng nhập mới truy cập được
 * - Role USER hoặc ADMIN đều có thể vào (cấu hình trong SecurityConfig)
 *
 * Các trang chính:
 * - /user/booking?accommodationId=X  → Trang tạo đơn đặt phòng mới
 * - /user/mybooking                   → Trang danh sách đặt phòng của tôi
 *
 * Annotation:
 * - @Controller: trả về view name (HTML), không phải JSON
 * - @RequestMapping("/user"): tất cả URL bắt đầu bằng /user
 * - @RequiredArgsConstructor: Lombok inject dependencies qua constructor
 */
@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserPageController {

    private final AccommodationService accommodationService;
    private final BookingService bookingService;
    private final UserRepository userRepository;

    // ============================================================
    // TRANG ĐẶT PHÒNG (Form tạo booking)
    // URL: /user/booking?accommodationId=1
    //
    // Khi user nhấn "Đặt phòng" trên trang chi tiết accommodation:
    // → Redirect đến /user/booking?accommodationId={id}
    // → Controller load thông tin accommodation và truyền vào template
    // → Template hiển thị form đặt phòng với thông tin nơi lưu trú
    // ============================================================
    @GetMapping("/booking")
    public String bookingPage(
            @RequestParam(required = false) Long accommodationId,
            Model model,
            Authentication auth) {

        // Nếu không có accommodationId → hiển thị trang lỗi
        if (accommodationId == null) {
            model.addAttribute("errorTitle", "Không tìm thấy thông tin");
            model.addAttribute("errorMessage",
                    "Vui lòng chọn nơi lưu trú trước khi đặt phòng.");
            return "error";
        }

        try {
            // Lấy thông tin accommodation từ DB (chỉ APPROVED)
            AccommodationResponse accommodation =
                    accommodationService.findApprovedById(accommodationId);
            model.addAttribute("accommodation", accommodation);

            // Load thông tin user đang đăng nhập từ DB
            // → điền sẵn vào form (readonly) để chuyên nghiệp và tránh bị hỏi vặn khi demo
            if (auth != null) {
                userRepository.findByEmail(auth.getName()).ifPresent(user -> {
                    model.addAttribute("userFullName", user.getFullName());
                    model.addAttribute("userPhone",    user.getPhone() != null ? user.getPhone() : "");
                    model.addAttribute("userEmail",    user.getEmail());
                });
            }

            return "user/booking";

        } catch (ResourceNotFoundException e) {
            model.addAttribute("errorTitle", "Không tìm thấy nơi lưu trú");
            model.addAttribute("errorMessage",
                    "Nơi lưu trú không tồn tại hoặc chưa được duyệt.");
            return "error";
        }
    }

    // ============================================================
    // TRANG ĐẶT PHÒNG CỦA TÔI (Danh sách bookings)
    // URL: /user/mybooking
    //
    // Hiển thị tất cả booking của user đang đăng nhập.
    // Dữ liệu lấy từ DB (không còn dùng localStorage demo).
    // ============================================================
    @GetMapping("/mybooking")
    public String myBookingPage(Model model, Authentication auth) {

        // Lấy email user đang đăng nhập từ Spring Security
        String userEmail = auth.getName();

        // Gọi service lấy danh sách booking từ DB
        List<BookingResponse> bookings = bookingService.getMyBookings(userEmail);

        // Truyền data vào template
        model.addAttribute("bookings", bookings);
        model.addAttribute("totalBookings", bookings.size());

        return "user/mybooking";
    }
}

