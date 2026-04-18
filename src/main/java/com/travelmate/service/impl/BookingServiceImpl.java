package com.travelmate.service.impl;

import com.travelmate.dto.request.BookingRequest;
import com.travelmate.dto.response.AvailabilityResponse;
import com.travelmate.dto.response.BookingResponse;
import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Booking;
import com.travelmate.entity.Payment;
import com.travelmate.entity.User;
import com.travelmate.enums.ApprovalStatus;
import com.travelmate.enums.BookingStatus;
import com.travelmate.enums.PaymentMethod;
import com.travelmate.enums.PaymentStatus;
import com.travelmate.enums.PropertyType;
import com.travelmate.exception.BadRequestException;
import com.travelmate.exception.ResourceNotFoundException;
import com.travelmate.repository.AccommodationRepository;
import com.travelmate.repository.BookingRepository;
import com.travelmate.repository.PaymentRepository;
import com.travelmate.repository.ReviewRepository;
import com.travelmate.repository.UserRepository;
import com.travelmate.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final PaymentRepository paymentRepository;

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

        // Bước 4b: [CHỐNG TRÙNG NGÀY] Kiểm tra overlap với booking đang active
        // Truyền -1L để không loại trừ booking nào (đây là booking mới, chưa có ID)
        boolean isOverlap = bookingRepository.hasOverlappingBooking(
                request.getAccommodationId(),
                request.getCheckIn(),
                request.getCheckOut(),
                -1L
        );
        if (isOverlap) {
            // Lấy thêm danh sách ngày bận để message rõ ràng hơn
            List<Object[]> bookedRanges = bookingRepository
                    .findBookedDateRanges(request.getAccommodationId());
            String bookedInfo = bookedRanges.stream()
                    .map(row -> formatDate((LocalDate) row[0]) + " → " + formatDate((LocalDate) row[1]))
                    .collect(Collectors.joining(", "));
            throw new BadRequestException(
                    "Nơi lưu trú đã được đặt trong khoảng ngày bạn chọn. " +
                    "Các ngày đã bận: " + bookedInfo + ". Vui lòng chọn ngày khác."
            );
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

        // Bước 7: Tạo Payment record tương ứng (UNPAID, QR_INVOICE)
        // Luôn tạo payment ngay khi booking được tạo để DB có record đối chiếu.
        // Mock transactionCode dạng: QR{yyyyMMddHHmm}TM{bookingId}
        String txCode = "QR" +
                java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmm")) +
                "TM" + savedBooking.getId();

        Payment payment = Payment.builder()
                .booking(savedBooking)
                .amount(savedBooking.getTotalPrice())
                .paymentMethod(PaymentMethod.QR_INVOICE)
                .paymentStatus(PaymentStatus.UNPAID)
                .transactionCode(txCode)
                .build();
        Payment savedPayment = paymentRepository.save(payment);

        // Bước 8: Sinh bookingCode dạng TM20260418-001
        // Dùng LocalDate.now() thay vì createdAt để tránh NPE (createdAt chưa flush tại đây).
        // Format: TM{yyyyMMdd}-{id padded 3 chữ số}
        String bookingCode = "TM" +
                java.time.LocalDate.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "-" +
                String.format("%03d", savedBooking.getId());
        savedBooking.setBookingCode(bookingCode);
        bookingRepository.save(savedBooking);


        // Bước 9: Trả về response kèm thông tin payment
        return toResponse(savedBooking, Collections.emptySet(), savedPayment);
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

        // [POLICY HỦY] Không cho hủy nếu ngày check-in còn < 2 ngày
        long daysUntilCheckIn = ChronoUnit.DAYS.between(LocalDate.now(), booking.getCheckIn());
        if (daysUntilCheckIn < 2) {
            throw new BadRequestException(
                "Không thể hủy đặt phòng trong vòng 2 ngày trước ngày nhận phòng " +
                "(ngày nhận phòng: " + formatDate(booking.getCheckIn()) + "). " +
                "Vui lòng liên hệ hỗ trợ: support@travelmate.vn"
            );
        }

        // Chuyển trạng thái → CANCELLED
        booking.setBookingStatus(BookingStatus.CANCELLED);
        Booking updatedBooking = bookingRepository.save(booking);

        // Cập nhật Payment khi hủy booking:
        // - Nếu đã PAID → REFUNDED (user hoàn tiền)
        // - Nếu UNPAID  → FAILED (chưa thanh toán, hủy luôn)
        paymentRepository.findByBookingId(updatedBooking.getId()).ifPresent(p -> {
            if (p.getPaymentStatus() == PaymentStatus.PAID) {
                p.setPaymentStatus(PaymentStatus.REFUNDED);
            } else {
                p.setPaymentStatus(PaymentStatus.FAILED);
            }
            paymentRepository.save(p);
        });

        Payment updatedPayment = paymentRepository.findByBookingId(updatedBooking.getId()).orElse(null);
        return toResponse(updatedBooking, Collections.emptySet(), updatedPayment);
    }

    // ========================================================================
    // 4. XÁC NHẬN ĐÃ THANH TOÁN (MOCK FLOW)
    // ========================================================================

    /**
     * User bấm "Tôi đã chuyển khoản" → cập nhật payment UNPAID → PAID.
     *
     * Luồng:
     * 1. Kiểm tra booking của user
     * 2. Tìm payment record
     * 3. Nếu đã PAID → trả về luôn (idempotent)
     * 4. Cập nhật paymentStatus = PAID, paidAt = now()
     * 5. Cập nhật bookingStatus = CONFIRMED
     */
    @Override
    public BookingResponse confirmPayment(Long bookingId, String userEmail) {

        // Tìm user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tài khoản: " + userEmail));

        // Tìm booking — bảo mật: phải thuộc về user
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đặt phòng hoặc bạn không có quyền thao tác"));

        // Không xác nhận booking đã CANCELLED hay COMPLETED
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking đã bị hủy, không thể xác nhận thanh toán");
        }
        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Booking đã hoàn thành");
        }

        // Tìm payment record
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thông tin thanh toán cho đặt phòng này"));

        // Nếu đã PAID → idempotent, trả về luôn
        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            return toResponse(booking, Collections.emptySet(), payment);
        }

        // Tạo mock transactionCode nếu chưa có (hoặc vẫn là code QR)
        String txCode = "QR" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")) +
                "TM" + bookingId;

        // Cập nhật Payment: UNPAID → PAID
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setTransactionCode(txCode);
        Payment savedPayment = paymentRepository.save(payment);

        // Cập nhật Booking: PENDING → CONFIRMED
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        Booking savedBooking = bookingRepository.save(booking);

        return toResponse(savedBooking, Collections.emptySet(), savedPayment);
    }

    // ========================================================================
    // 5. KIỂM TRA TÌNH TRẠNG PHÒNG TRỐNG (AVAILABILITY)
    // ========================================================================

    /**
     * Kiểm tra accommodation còn trống trong khoảng ngày yêu cầu.
     *
     * Luồng:
     * 1. Lấy danh sách bookedRanges (tất cả ngày đang bận) — dùng cho UI
     * 2. Nếu client cung cấp checkIn & checkOut: kiểm tra overlap
     * 3. Trả về AvailabilityResponse với kết quả + bookedRanges
     *
     * @Transactional(readOnly = true): chỉ đọc — tối ưu performance
     */
    @Override
    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(Long accommodationId,
                                                   LocalDate checkIn,
                                                   LocalDate checkOut) {
        // Lấy danh sách ngày đang bận để UI hiển thị (luôn trả về)
        List<Object[]> rawRanges = bookingRepository.findBookedDateRanges(accommodationId);
        List<AvailabilityResponse.BookedRange> bookedRanges = rawRanges.stream()
                .map(row -> new AvailabilityResponse.BookedRange(
                        (LocalDate) row[0],
                        (LocalDate) row[1]))
                .toList();

        // Nếu không cung cấp ngày → chỉ trả về danh sách ngày bận, không check overlap
        if (checkIn == null || checkOut == null) {
            return AvailabilityResponse.builder()
                    .available(true)
                    .bookedRanges(bookedRanges)
                    .build();
        }

        // Kiểm tra overlap với bookings đang active
        boolean isOverlap = bookingRepository.hasOverlappingBooking(
                accommodationId, checkIn, checkOut, -1L);

        if (isOverlap) {
            String conflictMsg = "Nơi lưu trú đã được đặt trong khoảng ngày này. " +
                    "Các ngày đã bận: " +
                    bookedRanges.stream()
                            .map(r -> formatDate(r.getCheckIn()) + " → " + formatDate(r.getCheckOut()))
                            .collect(Collectors.joining(", "));
            return AvailabilityResponse.builder()
                    .available(false)
                    .conflictMessage(conflictMsg)
                    .bookedRanges(bookedRanges)
                    .build();
        }

        return AvailabilityResponse.builder()
                .available(true)
                .bookedRanges(bookedRanges)
                .build();
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
    /**
     * toResponse — phiên bản đầy đủ kèm Payment.
     * Dùng khi đã có Payment object (tạo mới hoặc load từ DB).
     */
    private BookingResponse toResponse(Booking booking, Set<Long> reviewedBookingIds, Payment payment) {
        Accommodation acc = booking.getAccommodation();
        long numNights = ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut());

        BookingResponse.BookingResponseBuilder builder = BookingResponse.builder()
                // Thông tin booking
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
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
                .propertyTypeLabel(getPropertyTypeLabel(acc.getPropertyType()));

        // Gắn thông tin payment nếu có
        if (payment != null) {
            builder.paymentId(payment.getId())
                   .paymentStatusLabel(getPaymentStatusLabel(payment.getPaymentStatus()))
                   .paymentStatusCssClass(getPaymentStatusCssClass(payment.getPaymentStatus()))
                   .paymentMethodLabel(getPaymentMethodLabel(payment.getPaymentMethod()))
                   .transactionCode(payment.getTransactionCode());
        }

        return builder.build();
    }

    /**
     * toResponse — phiên bản load payment từ DB (dùng trong getMyBookings).
     * Tself-load payment để tránh phải truyền từ ngoài vào.
     */
    private BookingResponse toResponse(Booking booking, Set<Long> reviewedBookingIds) {
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        return toResponse(booking, reviewedBookingIds, payment);
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

    /** Nhãn phương thức thanh toán tiếng Việt. */
    private String getPaymentMethodLabel(PaymentMethod method) {
        if (method == null) return "Không xác định";
        return switch (method) {
            case QR_INVOICE -> "Chuyển khoản QR";
            case VNPAY      -> "VNPay";
            case MOMO       -> "MoMo";
            case CASH       -> "Tiền mặt";
        };
    }

    /** Nhãn trạng thái thanh toán tiếng Việt. */
    private String getPaymentStatusLabel(PaymentStatus status) {
        if (status == null) return "Chưa xác định";
        return switch (status) {
            case UNPAID   -> "Chờ thanh toán";
            case PAID     -> "Đã thanh toán";
            case FAILED   -> "Thanh toán thất bại";
            case REFUNDED -> "Đã hoàn tiền";
        };
    }

    /** CSS class badge cho trạng thái thanh toán. */
    private String getPaymentStatusCssClass(PaymentStatus status) {
        if (status == null) return "badge-secondary";
        return switch (status) {
            case UNPAID   -> "badge-unpaid";
            case PAID     -> "badge-paid";
            case FAILED   -> "badge-failed";
            case REFUNDED -> "badge-refunded";
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

    /**
     * Format ngày theo dạng dd/MM/yyyy (Việt Nam).
     * Dùng trong message lỗi chống trùng ngày.
     */
    private String formatDate(LocalDate date) {
        if (date == null) return "?";
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
