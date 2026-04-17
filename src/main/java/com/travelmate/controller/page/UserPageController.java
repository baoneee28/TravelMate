package com.travelmate.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * UserPageController - Controller xu ly cac trang danh cho USER da dang nhap.
 *
 * Tat ca URL bat dau bang /user/** se duoc bao ve boi Spring Security:
 * - Phai dang nhap moi truy cap duoc
 * - Role USER (hoac bat ky role da dang nhap) deu co the vao
 *
 * Sau nay khi co du lieu thuc tu DB, cac method se them:
 * - @ModelAttribute hoac Model de truyen data sang Thymeleaf
 * - Goi Service de lay booking, thong tin user, v.v.
 *
 * Annotation:
 * - @Controller: tra ve view name (khong phai JSON)
 * - @RequestMapping("/user"): tat ca URL trong controller bat dau bang /user
 */
@Controller
@RequestMapping("/user")
public class UserPageController {

    // ============================================================
    // TRANG DAT PHONG
    // URL: http://localhost:8080/user/booking
    // Chi user da dang nhap moi xem duoc
    // ============================================================
    @GetMapping("/booking")
    public String bookingPage() {
        // Sau nay se truyen thong tin user, danh sach phong vao Model
        return "user/booking";
    }

    // ============================================================
    // TRANG DAT PHONG CUA TOI
    // URL: http://localhost:8080/user/mybooking
    // Hien thi danh sach booking cua user dang dang nhap
    // ============================================================
    @GetMapping("/mybooking")
    public String myBookingPage() {
        // Sau nay: model.addAttribute("bookings", bookingService.getMyBookings(userId))
        return "user/mybooking";
    }
}
