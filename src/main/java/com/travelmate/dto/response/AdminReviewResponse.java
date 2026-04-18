package com.travelmate.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReviewResponse {
    private Long id;
    private String userName;
    private String userEmail;
    private Long accommodationId;
    private String accommodationName;
    private Long bookingId;
    private int rating;
    private String starsDisplay;
    private String comment;
    private LocalDateTime createdAt;
}
