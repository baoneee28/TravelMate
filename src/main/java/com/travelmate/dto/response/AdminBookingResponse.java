package com.travelmate.dto.response;

import com.travelmate.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBookingResponse {
    private Long id;
    private String userName;
    private String userEmail;
    private String userPhone;
    private Long accommodationId;
    private String accommodationName;
    private String accommodationCity;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private long numNights;
    private int numAdults;
    private int numChildren;
    private BigDecimal totalPrice;
    private String totalPriceFormatted;
    private BookingStatus bookingStatus;
    private String statusLabel;
    private String statusBadgeClass;
    private String notes;
    private LocalDateTime createdAt;
}
