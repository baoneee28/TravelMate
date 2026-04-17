package com.travelmate.service.impl;

import com.travelmate.dto.request.BookingRequest;
import com.travelmate.dto.response.BookingResponse;
import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Booking;
import com.travelmate.entity.User;
import com.travelmate.enums.ApprovalStatus;
import com.travelmate.enums.BookingStatus;
import com.travelmate.enums.PropertyType;
import com.travelmate.exception.BadRequestException;
import com.travelmate.exception.ResourceNotFoundException;
import com.travelmate.repository.AccommodationRepository;
import com.travelmate.repository.BookingRepository;
import com.travelmate.repository.ReviewRepository;
import com.travelmate.repository.UserRepository;
import com.travelmate.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation của BookingService — xử lý toàn bộ nghiệp vụ đặt phòng.
 *
 * Luật TravelMate áp dụng ở đây:
 * - Luật 13: Booking phải đủ dữ liệu cốt lõi (checkIn, checkOut, người lớn, trẻ em...)
 * - Luật 10: Phân quyền — chỉ USER mới đặt phòng, kiểm tra bảo mật booking thuộc về user
 * - Luật 7: Không phá code cũ — chỉ thêm mới, không sửa service/entity đã có
 *
 * @Transactional: mặc định readOnly = false vì có thao tác ghi (tạo, cập nhật booking)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final AccommodationRepository accommodationRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    // ========================================================================
    // 1. TẠO BOOKING MỚI
    // ========================================================================

    /**
     * Tạo đơn đặt phòng mới.
     *
     * Luồng xử lý chi tiết:
     * 1. Tìm user trong DB theo email (từ Spring Security principal)
     * 2. Tìm accommodation theo ID, kiểm tra đã APPROVED chưa
     * 3. Validate: checkOut > checkIn
     * 4. Validate: tổng khách <= maxGuests của accommodation
     * 5. Tính totalPrice = pricePerNight × số đêm
     * 6. Tạo Booking entity, save vào DB
     * 7. Trả về BookingResponse
     */
    @Override
    public BookingResponse createBooking(BookingRequest request, String userEmail) {

        // Bước 1: Tìm user đang đăng nhập
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tài khoản: " + userEmail));

        // Bước 2: Tìm accommodation — chỉ cho đặt nếu đã APPROVED
        Accommodation accommodation = accommodationRepository
                .findByIdAndApprovalStatus(request.getAccommodationId(), ApprovalStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nơi lưu trú không tồn tại hoặc chưa được duyệt"));

        // Bước 3: Validate ngày
        validateDates(request.getCheckIn(), request.getCheckOut());

        // Bước 4: Validate số khách
        int totalGuests = request.getNumAdults() + request.getNumChildren();
        if (totalGuests > accommodation.getMaxGuests()) {
            throw new BadRequestException(
                    "Số khách (" + totalGuests + ") vượt quá sức chứa tối đa ("
                            + accommodation.getMaxGuests() + " khách)");
        }

        // Bước 5: Tính tổng tiền
        // Số đêm = số ngày giữa checkIn và checkOut
        long numNights = ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        BigDecimal totalPrice = accommodation.getPricePerNight()
                .multiply(BigDecimal.valueOf(numNights));

        // Bước 6: Tạo và lưu Booking entity
        Booking booking = Booking.builder()
                .user(user)
                .accommodation(accommodation)
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .numAdults(request.getNumAdults())
                .numChildren(request.getNumChildren())
                .totalPrice(totalPrice)
                .bookingStatus(BookingStatus.PENDING)
                .notes(request.getNotes())
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // Bước 7: Trả về response (booking mới tạo chưa được review)
        return toResponse(savedBooking, Collections.emptySet());
    }

    // ========================================================================
    // 2. LẤY DANH SÁCH BOOKING CỦA USER
    // ========================================================================

    /**
     * Lấy tất cả booking của user đang đăng nhập.
     *
     * @Transactional(readOnly = true): method này chỉ đọc, không ghi
     * → Hibernate tối ưu performance (không cần flush session)
     */
    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String userEmail) {

        // Tìm user theo email
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tài khoản: " + userEmail));

        // Lấy danh sách booking kèm accommodation (JOIN FETCH)
        List<Booking> bookings = bookingRepository.findByUserIdWithAccommodation(user.getId());

        // Lấy danh sách booking ID đã review để đánh dấu trên UI
        Set<Long> reviewedBookingIds = new HashSet<>(
                reviewRepository.findReviewedBookingIdsByUserId(user.getId()));

        // Chuyển đổi entity → DTO (kèm thông tin reviewed)
        return bookings.stream()
                .map(b -> toResponse(b, reviewedBookingIds))
                .toList();
    }

    // ========================================================================
    // 3. HỦY BOOKING
    // ========================================================================

    /**
     * Hủy booking: chuyển trạng thái → CANCELLED.
     *
     * Bảo mật: kiểm tra booking thuộc về user đang đăng nhập.
     * Chỉ hủy được khi status = PENDING hoặc CONFIRMED.
     */
    @Override
    public BookingResponse cancelBooking(Long bookingId, String userEmail) {

        // Tìm user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tài khoản: " + userEmail));

        // Tìm booking — BẢO MẬT: phải kiểm tra booking thuộc về user
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đặt phòng hoặc bạn không có quyền thao tác"));

        // Kiểm tra trạng thái: chỉ hủy được PENDING và CONFIRMED
        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Không thể hủy đặt phòng đã hoàn thành");
        }
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Đặt phòng này đã được hủy trước đó");
        }

        // Chuyển trạng thái → CANCELLED
        booking.setBookingStatus(BookingStatus.CANCELLED);
        Booking updatedBooking = bookingRepository.save(booking);

        return toResponse(updatedBooking, Collections.emptySet());
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    /**
     * Validate ngày check-in và check-out.
     *
     * Quy tắc:
     * - Check-in phải >= hôm nay (không đặt ngày trong quá khứ)
     * - Check-out phải sau check-in ít nhất 1 ngày
     */
    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        LocalDate today = LocalDate.now();

        if (checkIn.isBefore(today)) {
            throw new BadRequestException("Ngày nhận phòng không được trước hôm nay");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BadRequestException(
                    "Ngày trả phòng phải sau ngày nhận phòng ít nhất 1 ngày");
        }
    }

    /**
     * Chuyển đổi entity Booking → DTO BookingResponse.
     *
     * Gộp thông tin booking + accommodation vào 1 response duy nhất,
     * để Thymeleaf template chỉ cần 1 object là hiển thị đầy đủ.
     */
    private BookingResponse toResponse(Booking booking, Set<Long> reviewedBookingIds) {
        Accommodation acc = booking.getAccommodation();
        long numNights = ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut());

        return BookingResponse.builder()
                // Thông tin booking
                .id(booking.getId())
                .checkIn(booking.getCheckIn())
                .checkOut(booking.getCheckOut())
                .numNights(numNights)
                .numAdults(booking.getNumAdults())
                .numChildren(booking.getNumChildren())
                .totalGuests(booking.getNumAdults() + booking.getNumChildren())
                .totalPrice(booking.getTotalPrice())
                .totalPriceFormatted(formatPrice(booking.getTotalPrice()))
                .bookingStatus(booking.getBookingStatus())
                .statusLabel(getStatusLabel(booking.getBookingStatus()))
                .statusCssClass(getStatusCssClass(booking.getBookingStatus()))
                .reviewed(reviewedBookingIds.contains(booking.getId()))
                .notes(booking.getNotes())
                .createdAt(booking.getCreatedAt())
                // Thông tin accommodation
                .accommodationId(acc.getId())
                .accommodationName(acc.getName())
                .accommodationCity(acc.getCity())
                .accommodationAddress(acc.getAddress())
                .accommodationThumbnail(acc.getThumbnailUrl())
                .pricePerNight(acc.getPricePerNight())
                .pricePerNightFormatted(formatPrice(acc.getPricePerNight()))
                .propertyTypeLabel(getPropertyTypeLabel(acc.getPropertyType()))
                .build();
    }

    /**
     * Trả về nhãn trạng thái booking bằng tiếng Việt.
     */
    private String getStatusLabel(BookingStatus status) {
        return switch (status) {
            case PENDING   -> "Chờ xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case CANCELLED -> "Đã hủy";
            case COMPLETED -> "Hoàn thành";
        };
    }

    /**
     * Trả về CSS class tương ứng với trạng thái.
     * Dùng trong template: th:class="${booking.statusCssClass}"
     */
    private String getStatusCssClass(BookingStatus status) {
        return switch (status) {
            case PENDING   -> "status-pending";
            case CONFIRMED -> "status-confirmed";
            case CANCELLED -> "status-cancelled";
            case COMPLETED -> "status-completed";
        };
    }

    /**
     * Nhãn loại lưu trú tiếng Việt (giống AccommodationServiceImpl).
     */
    private String getPropertyTypeLabel(PropertyType type) {
        if (type == null) return "";
        return switch (type) {
            case HOTEL     -> "Khách sạn";
            case HOMESTAY  -> "Homestay";
            case VILLA     -> "Villa";
            case APARTMENT -> "Căn hộ";
        };
    }

    /**
     * Format số tiền theo định dạng Việt Nam: 1200000 → "1.200.000"
     */
    private String formatPrice(BigDecimal price) {
        if (price == null) return "0";
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(price);
    }
}
