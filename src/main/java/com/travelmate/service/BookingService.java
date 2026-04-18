package com.travelmate.service;

import com.travelmate.dto.request.BookingRequest;
import com.travelmate.dto.response.AvailabilityResponse;
import com.travelmate.dto.response.BookingResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface định nghĩa các nghiệp vụ liên quan đến Booking.
 *
 * Dùng Interface + Implementation là pattern chuẩn trong Spring Boot:
 * - Dễ test (mock interface khi viết unit test)
 * - Dễ thay implementation nếu cần
 * - Tách biệt contract (giao diện) và logic xử lý
 *
 * 4 chức năng:
 * 1. createBooking       — Tạo đơn đặt phòng mới (có kiểm tra trùng ngày)
 * 2. getMyBookings       — Lấy danh sách booking của user đang đăng nhập
 * 3. cancelBooking       — Hủy booking (chuyển trạng thái → CANCELLED)
 * 4. checkAvailability   — Kiểm tra accommodation còn trống trong khoảng ngày
 */
public interface BookingService {

    /**
     * Tạo đơn đặt phòng mới.
     *
     * Luồng xử lý:
     * 1. Kiểm tra accommodation có tồn tại và đã APPROVED không
     * 2. Kiểm tra ngày check-out phải sau check-in
     * 3. Kiểm tra tổng khách không vượt quá maxGuests
     * 4. [MỚI] Kiểm tra không trùng ngày với booking PENDING/CONFIRMED khác
     * 5. Tính totalPrice = pricePerNight × số đêm
     * 6. Tạo Booking entity với status = PENDING
     * 7. Lưu vào DB và trả về BookingResponse
     *
     * @param request DTO chứa thông tin đặt phòng từ form
     * @param userEmail email của user đang đăng nhập (lấy từ Spring Security)
     * @return BookingResponse thông tin booking vừa tạo
     */
    BookingResponse createBooking(BookingRequest request, String userEmail);

    /**
     * Lấy danh sách tất cả booking của user đang đăng nhập.
     *
     * Sắp xếp mới nhất trước (ORDER BY createdAt DESC).
     * Dùng cho trang "Đặt phòng của tôi" (/user/mybooking).
     *
     * @param userEmail email của user đang đăng nhập
     * @return Danh sách BookingResponse
     */
    List<BookingResponse> getMyBookings(String userEmail);

    /**
     * Hủy booking.
     *
     * Điều kiện hủy:
     * - Booking phải thuộc về user đang đăng nhập (bảo mật)
     * - Chỉ hủy được khi status = PENDING hoặc CONFIRMED
     * - Không thể hủy booking đã COMPLETED hoặc đã CANCELLED
     *
     * Khi hủy: chuyển bookingStatus → CANCELLED
     *
     * @param bookingId ID booking cần hủy
     * @param userEmail email user đang đăng nhập
     * @return BookingResponse thông tin booking sau khi hủy
     */
    BookingResponse cancelBooking(Long bookingId, String userEmail);

    /**
     * Kiểm tra accommodation còn trống trong khoảng ngày yêu cầu.
     *
     * Được gọi từ UI (AJAX) khi user chọn ngày trên form đặt phòng,
     * để cảnh báo ngay lập tức thay vì chờ đến khi submit.
     *
     * Trả về:
     * - available: true/false
     * - conflictMessage: mô tả lỗi nếu đã bận
     * - bookedRanges: danh sách ngày đã bận (để hiển thị trên date-picker)
     *
     * @param accommodationId ID accommodation cần kiểm tra
     * @param checkIn         Ngày nhận phòng yêu cầu (nullable: chỉ lấy bookedRanges)
     * @param checkOut        Ngày trả phòng yêu cầu (nullable)
     * @return AvailabilityResponse
     */
    AvailabilityResponse checkAvailability(Long accommodationId,
                                           LocalDate checkIn,
                                           LocalDate checkOut);

    /**
     * Xác nhận đã thanh toán (mock flow).
     *
     * Luồng:
     * 1. Kiểm tra booking thuộc về user
     * 2. Kiểm tra payment tồn tại và đang UNPAID
     * 3. Cập nhật paymentStatus → PAID, paidAt = now()
     * 4. Cập nhật bookingStatus → CONFIRMED
     *
     * @param bookingId  ID booking cần xác nhận thanh toán
     * @param userEmail  email user đang đăng nhập (kiểm tra quyền)
     * @return BookingResponse sau khi cập nhật
     */
    BookingResponse confirmPayment(Long bookingId, String userEmail);
}
