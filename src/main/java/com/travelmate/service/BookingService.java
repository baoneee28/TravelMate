package com.travelmate.service;

import com.travelmate.dto.request.BookingRequest;
import com.travelmate.dto.response.BookingResponse;

import java.util.List;

/**
 * Interface định nghĩa các nghiệp vụ liên quan đến Booking.
 *
 * Dùng Interface + Implementation là pattern chuẩn trong Spring Boot:
 * - Dễ test (mock interface khi viết unit test)
 * - Dễ thay implementation nếu cần
 * - Tách biệt contract (giao diện) và logic xử lý
 *
 * 3 chức năng chính trong Bước 2:
 * 1. createBooking   — Tạo đơn đặt phòng mới
 * 2. getMyBookings   — Lấy danh sách booking của user đang đăng nhập
 * 3. cancelBooking   — Hủy booking (chuyển trạng thái → CANCELLED)
 */
public interface BookingService {

    /**
     * Tạo đơn đặt phòng mới.
     *
     * Luồng xử lý:
     * 1. Kiểm tra accommodation có tồn tại và đã APPROVED không
     * 2. Kiểm tra ngày check-out phải sau check-in
     * 3. Kiểm tra tổng khách không vượt quá maxGuests
     * 4. Tính totalPrice = pricePerNight × số đêm
     * 5. Tạo Booking entity với status = PENDING
     * 6. Lưu vào DB và trả về BookingResponse
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
}
