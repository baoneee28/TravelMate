package com.travelmate.entity;

import com.travelmate.enums.BookingStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity Booking - ánh xạ vào bảng "bookings" trong MySQL.
 *
 * Đây là entity đại diện cho một đơn đặt chỗ của USER:
 * - USER chọn accommodation trên trang chi tiết → nhấn "Đặt phòng"
 * - Hệ thống tạo Booking với trạng thái PENDING
 * - Sau khi thanh toán / xác nhận → CONFIRMED
 * - USER có thể hủy → CANCELLED
 * - Sau khi trải nghiệm xong → COMPLETED (để review)
 *
 * Kế thừa BaseEntity để có sẵn: id, createdAt, updatedAt
 *
 * Annotation giải thích:
 * - @Entity: đánh dấu class này là entity JPA, ánh xạ vào bảng trong DB
 * - @Table(name = "bookings"): tên bảng trong MySQL
 * - @Getter/@Setter: Lombok tự tạo getter/setter
 * - @NoArgsConstructor: constructor không tham số (JPA bắt buộc cần)
 * - @AllArgsConstructor: constructor đầy đủ tham số
 * - @Builder: Lombok tạo builder pattern
 */
@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_user",          columnList = "user_id"),
        @Index(name = "idx_booking_accommodation", columnList = "accommodation_id"),
        @Index(name = "idx_booking_status",        columnList = "booking_status"),
        @Index(name = "idx_booking_checkin",        columnList = "check_in"),
        @Index(name = "idx_booking_code",           columnList = "booking_code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    /**
     * USER đã đặt phòng này.
     * - @ManyToOne: nhiều booking có thể thuộc 1 user
     * - FetchType.LAZY: chỉ load user khi thực sự cần (tiết kiệm query)
     * - @JoinColumn: cột user_id trong bảng bookings
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Nơi lưu trú mà user đã đặt.
     * - @ManyToOne: nhiều booking có thể đặt cùng 1 accommodation
     * - FetchType.LAZY: chỉ load accommodation khi cần
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    /**
     * Ngày nhận phòng (check-in).
     * Dùng LocalDate vì chỉ cần ngày, không cần giờ.
     */
    @NotNull(message = "Ngày nhận phòng không được để trống")
    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    /**
     * Ngày trả phòng (check-out).
     * Phải sau ngày check-in (kiểm tra ở tầng Service).
     */
    @NotNull(message = "Ngày trả phòng không được để trống")
    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    /**
     * Số người lớn.
     * Mặc định là 1, phải >= 1.
     */
    @Min(value = 1, message = "Số người lớn phải >= 1")
    @Column(name = "num_adults", nullable = false)
    @Builder.Default
    private Integer numAdults = 1;

    /**
     * Số trẻ em.
     * Mặc định là 0, phải >= 0.
     */
    @Min(value = 0, message = "Số trẻ em phải >= 0")
    @Column(name = "num_children", nullable = false)
    @Builder.Default
    private Integer numChildren = 0;

    /**
     * Tổng tiền = pricePerNight × số đêm.
     * Dùng BigDecimal để đảm bảo chính xác cho tiền tệ.
     */
    @NotNull
    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    /**
     * Trạng thái booking:
     * - PENDING: mới đặt, chờ xác nhận (mặc định)
     * - CONFIRMED: đã xác nhận
     * - CANCELLED: đã hủy
     * - COMPLETED: đã hoàn tất (có thể review)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false, length = 20)
    @Builder.Default
    private BookingStatus bookingStatus = BookingStatus.PENDING;

    /**
     * Mã đặt phòng duy nhất — dạng TM20260418-001.
     * Format: TM{yyyyMMdd}-{sequence 3 chữ số}.
     * Được sinh trong BookingServiceImpl sau khi save (có ID).
     * UNIQUE trong DB để đảm bảo không trùng.
     */
    @Column(name = "booking_code", length = 20, unique = true)
    private String bookingCode;

    /**
     * Ghi chú của khách (tuỳ chọn).
     * Ví dụ: "Cần phòng tầng cao", "Giường phụ cho trẻ em"
     */
    @Column(columnDefinition = "TEXT")
    private String notes;
}
