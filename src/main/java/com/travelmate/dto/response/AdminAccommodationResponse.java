package com.travelmate.dto.response;

import com.travelmate.enums.ApprovalStatus;
import com.travelmate.enums.PropertyType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAccommodationResponse {
    private Long id;
    private String name;
    private PropertyType propertyType;
    private String propertyTypeLabel;
    private String city;
    private String address;
    private BigDecimal pricePerNight;
    private String priceFormatted;
    private Integer maxGuests;
    private String thumbnailUrl;
    private ApprovalStatus approvalStatus;
    private String approvalStatusLabel;
    private String approvalStatusBadgeClass;
    private String partnerName;
    private String partnerEmail;
    private LocalDateTime createdAt;
}
