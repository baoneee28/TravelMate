package com.travelmate.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * AdminPageController - Controller xu ly cac trang ADMIN cua TravelMate.
 *
 * Tat ca URL bat dau bang /admin/** chi cho phep role ADMIN truy cap.
 * Spring Security se tu dong chan neu user khong co quyen.
 *
 * Cac trang admin gom:
 * - Dashboard: tong quan he thong
 * - Bookings: quan ly dat phong
 * - Hotels/Listings: quan ly noi luu tru
 * - Users: quan ly nguoi dung
 * - Places: quan ly dia diem
 * - Rooms: quan ly phong
 *
 * Annotation:
 * - @Controller: tra ve view name
 * - @RequestMapping("/admin"): prefix cho tat ca URL admin
 */
@Controller
@RequestMapping("/admin")
public class AdminPageController {

    // ============================================================
    // DASHBOARD - Tong quan he thong cho Admin
    // URL: http://localhost:8080/admin/dashboard
    // Hien thi so lieu thong ke: booking, user, doanh thu
    // ============================================================
    @GetMapping("/dashboard")
    public String dashboardPage() {
        // Sau nay: model.addAttribute("stats", statsService.getDashboardStats())
        return "admin/dashboard";
    }

    // ============================================================
    // QUAN LY DAT PHONG
    // URL: http://localhost:8080/admin/bookings
    // Admin xem va quan ly tat ca booking trong he thong
    // ============================================================
    @GetMapping("/bookings")
    public String bookingsPage() {
        return "admin/bookings";
    }

    // ============================================================
    // QUAN LY NƠI LUU TRU (Listings)
    // URL: http://localhost:8080/admin/listings
    // Admin duyet / tu choi listing cua Partner
    // Tam thoi dung file hotels.html, se rename sau
    // ============================================================
    @GetMapping("/listings")
    public String listingsPage() {
        // Tra ve trang quan ly listings (tat ca loai luu tru)
        return "admin/listings";
    }

    // ============================================================
    // QUAN LY NGUOI DUNG
    // URL: http://localhost:8080/admin/users
    // Admin xem danh sach user, partner; khoa/mo tai khoan
    // ============================================================
    @GetMapping("/users")
    public String usersPage() {
        return "admin/users";
    }

    // ============================================================
    // QUAN LY DIA DIEM
    // URL: http://localhost:8080/admin/places
    // Admin quan ly danh sach dia diem du lich
    // ============================================================
    @GetMapping("/places")
    public String placesPage() {
        return "admin/places";
    }

    // ============================================================
    // QUAN LY PHONG
    // URL: http://localhost:8080/admin/rooms
    // Admin xem tat ca phong trong he thong
    // ============================================================
    @GetMapping("/rooms")
    public String roomsPage() {
        return "admin/rooms";
    }
}
