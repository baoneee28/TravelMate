package com.travelmate.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatsResponse {
    private long totalAccommodations;
    private long pendingAccommodations;
    private long approvedAccommodations;

    private long totalBookings;
    private long pendingBookings;
    private long confirmedBookings;
    private long completedBookings;
    private long cancelledBookings;

    private long totalUsers;
    private long totalPartners;

    private long totalReviews;
}
