package com.travelmate.controller.page;

import com.travelmate.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public String dashboardPage(Model model) {
        model.addAttribute("stats", adminService.getDashboardStats());
        model.addAttribute("recentBookings", adminService.getAllBookings()
                .stream().limit(5).toList());
        return "admin/dashboard";
    }

    @GetMapping("/bookings")
    public String bookingsPage(Model model) {
        model.addAttribute("bookings", adminService.getAllBookings());
        return "admin/bookings";
    }

    @GetMapping("/listings")
    public String listingsPage(Model model) {
        model.addAttribute("accommodations", adminService.getAllAccommodations());
        return "admin/listings";
    }

    @GetMapping("/users")
    public String usersPage(Model model) {
        model.addAttribute("users", adminService.getAllUsers());
        return "admin/users";
    }

    @GetMapping("/reviews")
    public String reviewsPage(Model model) {
        model.addAttribute("reviews", adminService.getAllReviews());
        return "admin/reviews";
    }

    @GetMapping("/places")
    public String placesPage() {
        return "admin/places";
    }

    @GetMapping("/rooms")
    public String roomsPage() {
        return "admin/rooms";
    }
}
