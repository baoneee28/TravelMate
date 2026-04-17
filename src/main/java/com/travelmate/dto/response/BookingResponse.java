package com.travelmate.dto.response;

import com.travelmate.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO truyền thông tin booking từ Service → Controller → Template.
 *
 * Chứa cả thông tin booking lẫn thông tin accommodation liên quan,
 * để template không cần truy cập trực tiếp vào entity.
 *
 * Ví dụ sử dụng trong Thymeleaf:
 *   th:text="${booking.accommodationName}"  → tên nơi lưu trú
 *   th:text="${booking.totalPriceFormatted}" → "3.600.000"
 *   th:text="${booking.statusLabel}"        → "Đang chờ xác nhận"
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    // ========== THÔNG TIN BOOKING ==========

    /** ID booking */
    private Long id;

    /** Ngày nhận phòng */
    private LocalDate checkIn;

    /** Ngày trả phòng */
    private LocalDate checkOut;

    /** Số đêm lưu trú (tính từ checkIn đến checkOut) */
    private long numNights;

    /** Số người lớn */
    private Integer numAdults;

    /** Số trẻ em */
    private Integer numChildren;

    /** Tổng số khách = người lớn + trẻ em */
    private Integer totalGuests;

    /** Tổng tiền dạng số */
    private BigDecimal totalPrice;

    /** Tổng tiền đã format: "3.600.000" */
    private String totalPriceFormatted;

    /** Trạng thái booking (enum) */
    private BookingStatus bookingStatus;

    /** Nhãn trạng thái tiếng Việt: "Đang chờ xác nhận" */
    private String statusLabel;

    /**
     * CSS class cho badge trạng thái.
     * Ví dụ: "status-pending", "status-confirmed", "status-cancelled"
     * Dùng trong template: th:class="${booking.statusCssClass}"
     */
    private String statusCssClass;

    /** Booking đã được review chưa (dùng cho nút "Đánh giá" trên UI) */
    private boolean reviewed;

    /** Ghi chú của khách */
    private String notes;

    /** Thời gian tạo booking */
    private LocalDateTime createdAt;

    // ========== THÔNG TIN ACCOMMODATION ==========

    /** ID accommodation (nơi lưu trú) */
    private Long accommodationId;

    /** Tên nơi lưu trú */
    private String accommodationName;

    /** Thành phố */
    private String accommodationCity;

    /** Địa chỉ cụ thể */
    private String accommodationAddress;

    /** URL ảnh đại diện */
    private String accommodationThumbnail;

    /** Giá mỗi đêm dạng số */
    private BigDecimal pricePerNight;

    /** Giá mỗi đêm đã format: "1.200.000" */
    private String pricePerNightFormatted;

    /** Nhãn loại lưu trú: "Khách sạn", "Villa"... */
    private String propertyTypeLabel;
}
