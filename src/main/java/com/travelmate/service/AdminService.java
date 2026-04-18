package com.travelmate.service;

import com.travelmate.dto.response.*;

import java.util.List;

public interface AdminService {

    AdminStatsResponse getDashboardStats();

    List<AdminBookingResponse> getAllBookings();
    void updateBookingStatus(Long id, String status);

    List<AdminAccommodationResponse> getAllAccommodations();
    void approveAccommodation(Long id);
    void rejectAccommodation(Long id);

    List<AdminUserResponse> getAllUsers();
    void toggleUserStatus(Long id);

    List<AdminReviewResponse> getAllReviews();
    void deleteReview(Long id);
}
