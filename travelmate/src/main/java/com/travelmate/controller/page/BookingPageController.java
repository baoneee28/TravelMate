package com.travelmate.controller.page;

import com.travelmate.dto.RoomAvailabilityDto;
import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Booking;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.PaymentOption;
import com.travelmate.repository.UserRepository;
import com.travelmate.service.AccommodationService;
import com.travelmate.service.AvailabilityService;
import com.travelmate.service.BookingService;
import com.travelmate.service.ReviewService;
import com.travelmate.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.Optional;

/**
 * BookingPageController — Điều khiển trang đặt phòng và lịch sử booking.
 *
 * Routes:
 *   GET  /booking         → Trang form đặt phòng (cần đăng nhập)
 *   POST /booking/confirm → Xử lý tạo booking (cần đăng nhập)
 *   GET  /my-bookings     → Trang danh sách booking của user (cần đăng nhập)
 */
@Controller
public class BookingPageController {

    private final AccommodationService accommodationService;
    private final BookingService bookingService;
    private final ReviewService reviewService;
    private final UserRepository userRepository;
    private final AvailabilityService availabilityService;

    public BookingPageController(AccommodationService accommodationService,
                                  BookingService bookingService,
                                  ReviewService reviewService,
                                  UserRepository userRepository,
                                  AvailabilityService availabilityService) {
        this.accommodationService = accommodationService;
        this.bookingService = bookingService;
        this.reviewService = reviewService;
        this.userRepository = userRepository;
        this.availabilityService = availabilityService;
    }

    /**
     * Trang đặt phòng — hiển thị form điền thông tin + chi tiết giá.
     *
     * URL: /booking?roomId=1&checkIn=2026-05-01&checkOut=2026-05-02&adults=2&children=0&rooms=1
     *
     * Nếu chưa đăng nhập → redirect về login.
     */
    @GetMapping("/booking")
    public String bookingPage(
            @RequestParam Long roomId,
            @RequestParam(required = false, defaultValue = "") String checkIn,
            @RequestParam(required = false, defaultValue = "") String checkOut,
            @RequestParam(required = false, defaultValue = "2") int adults,
            @RequestParam(required = false, defaultValue = "0") int children,
            @RequestParam(required = false, defaultValue = "1") int rooms,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            Model model) {

        // Kiểm tra đăng nhập
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        // Tìm Room theo ID
        Optional<Room> optRoom = accommodationService.getRoomById(roomId);
        if (optRoom.isEmpty()) {
            return "redirect:/accommodations";
        }

        Room room = optRoom.get();

        // Guard: phòng phải APPROVED mới hiển thị form đặt phòng
        if (room.getApprovalStatus() != ApprovalStatus.APPROVED) {
            return "redirect:/accommodations";
        }

        Accommodation hotel = room.getAccommodation();

        // Guard: accommodation cũng phải APPROVED
        if (hotel.getApprovalStatus() != ApprovalStatus.APPROVED) {
            return "redirect:/accommodations";
        }

        // === CLAMP tham số URL để tránh dữ liệu rác từ query string ===
        rooms    = Math.max(1, Math.min(rooms, 20));
        adults   = Math.max(1, adults);
        children = Math.max(0, children);

        // Parse ngày check-in/check-out (nếu có)
        LocalDate checkInDate = parseDate(checkIn);
        LocalDate checkOutDate = parseDate(checkOut);

        // Nếu không truyền ngày → mặc định hôm nay + ngày mai
        if (checkInDate == null || checkInDate.isBefore(LocalDate.now())) {
            checkInDate = LocalDate.now();
        }
        if (checkOutDate == null || !checkOutDate.isAfter(checkInDate)) {
            checkOutDate = checkInDate.plusDays(1);
        }

        // Tính tiền
        long numberOfNights = bookingService.calculateNights(checkInDate, checkOutDate);
        BigDecimal totalAmount = bookingService.calculateTotalAmount(room, numberOfNights, rooms);
        BigDecimal paidFull = bookingService.calculatePaidAmount(totalAmount, PaymentOption.FULL_PAYMENT);
        BigDecimal paidDeposit = bookingService.calculatePaidAmount(totalAmount, PaymentOption.DEPOSIT_30);

        // Kiểm tra tình trạng phòng theo ngày — hiển thị warning FULL/LIMITED trên form
        RoomAvailabilityDto roomAvailability = availabilityService.checkSingleRoom(room, checkInDate, checkOutDate);

        // Truyền vào template
        model.addAttribute("room", room);
        model.addAttribute("hotel", hotel);
        model.addAttribute("roomAvailability", roomAvailability);
        model.addAttribute("checkIn", checkInDate.toString());
        model.addAttribute("checkOut", checkOutDate.toString());
        model.addAttribute("checkInFormatted", formatDateVN(checkInDate));
        model.addAttribute("checkOutFormatted", formatDateVN(checkOutDate));
        model.addAttribute("adults", adults);
        model.addAttribute("children", children);
        model.addAttribute("rooms", rooms);
        model.addAttribute("numberOfNights", numberOfNights);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("paidFull", paidFull);
        model.addAttribute("paidDeposit", paidDeposit);

        // isVilla dùng cả trong <main> lẫn modal QR (ngoài <main>) nên cần thêm vào model
        boolean isVilla = hotel.getPropertyType() != null
                && "VILLA".equals(hotel.getPropertyType().name());
        model.addAttribute("isVilla", isVilla);

        // Thông tin user đã đăng nhập
        model.addAttribute("userName", currentUser.getFullName());
        model.addAttribute("userEmail", currentUser.getUsername()); // email
        model.addAttribute("userPhone", currentUser.getPhone());

        return "user/booking";
    }

    /**
     * Khởi tạo booking và redirect sang VNPAY Sandbox để thanh toán.
     *
     * POST /booking/confirm
     *
     * Luồng mới (VNPAY thật):
     *   1. Validate + tạo booking PENDING_PAYMENT + payment PENDING_PAYMENT
     *   2. Phòng được giữ tạm 15 phút
     *   3. Redirect sang GET /payment/vnpay/create/{bookingId}
     *   4. VNPAY xử lý → trả kết quả qua IPN + Return URL
     */
    @PostMapping("/booking/confirm")
    public String confirmBooking(
            @RequestParam Long roomId,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            @RequestParam(defaultValue = "2") int adults,
            @RequestParam(defaultValue = "0") int children,
            @RequestParam(defaultValue = "1") int rooms,
            @RequestParam String customerName,
            @RequestParam String customerPhone,
            @RequestParam String customerEmail,
            @RequestParam String paymentOption,
            @RequestParam(required = false, defaultValue = "") String voucherCode,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes redirectAttributes) {

        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        try {
            User user = userRepository.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản!"));

            Room room = accommodationService.getRoomById(roomId)
                    .orElseThrow(() -> new RuntimeException("Phòng không tồn tại!"));
            Accommodation accommodation = room.getAccommodation();

            LocalDate checkInDate  = LocalDate.parse(checkIn);
            LocalDate checkOutDate = LocalDate.parse(checkOut);
            PaymentOption option   = PaymentOption.valueOf(paymentOption);

            // Tạo booking PENDING_PAYMENT + payment PENDING_PAYMENT (giữ phòng 15 phút)
            Booking booking = bookingService.createBooking(
                    user, room, accommodation,
                    customerName, customerPhone, customerEmail,
                    checkInDate, checkOutDate,
                    adults, children, rooms,
                    option,
                    (voucherCode != null && !voucherCode.isBlank()) ? voucherCode : null);

            // Redirect sang controller VNPAY để tạo URL và chuyển hướng sang cổng thanh toán
            return "redirect:/payment/vnpay/create/" + booking.getId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Lỗi đặt phòng: " + e.getMessage());
            return "redirect:/booking?roomId=" + roomId
                    + "&checkIn=" + checkIn + "&checkOut=" + checkOut
                    + "&adults=" + adults + "&children=" + children + "&rooms=" + rooms;
        }
    }

    /**
     * Trang danh sách booking của user đang đăng nhập.
     *
     * GET /my-bookings
     */
    @GetMapping("/my-bookings")
    public String myBookings(
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) Long reviewBookingId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            Model model) {

        // Nếu chưa đăng nhập → redirect
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        // Lấy User entity từ DB
        Optional<User> optUser = userRepository.findByEmail(currentUser.getUsername());
        if (optUser.isEmpty()) {
            return "redirect:/auth/login";
        }

        // Lấy danh sách booking
        List<Booking> bookings = bookingService.getBookingsByUser(optUser.get());

        // Lấy set booking IDs đã review — để hiện badge "Đã đánh giá" trên template
        Set<Long> reviewedBookingIds = reviewService.getReviewedBookingIds(bookings);

        model.addAttribute("bookings", bookings);
        model.addAttribute("reviewedBookingIds", reviewedBookingIds);
        model.addAttribute("userName", currentUser.getFullName());
        // Truyền tab và reviewBookingId để FE auto-scroll và mở form đánh giá
        model.addAttribute("initialTab", tab != null ? tab : "ALL");
        model.addAttribute("reviewBookingId", reviewBookingId);

        return "user/mybooking";
    }

    /**
     * Hủy đặt phòng — chỉ user sở hữu và đang PENDING_ADMIN_APPROVAL mới được hủy.
     *
     * POST /my-bookings/{id}/cancel
     */
    @PostMapping("/my-bookings/{id}/cancel")
    public String cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes redirectAttributes) {

        // Chưa đăng nhập → redirect về login
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        try {
            // Lấy User entity từ DB
            User user = userRepository.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản!"));

            // Gọi service hủy booking (service validate quyền + trạng thái, trả về booking đã hủy)
            com.travelmate.entity.Booking cancelled = bookingService.cancelBooking(id, user);

            if (cancelled.getPaymentStatus() == com.travelmate.entity.enums.PaymentStatus.REFUND_PENDING) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "✅ Đã ghi nhận yêu cầu hủy. TravelMate đã nhận tiền thanh toán của bạn và sẽ xử lý hoàn tiền theo chính sách trong 3–5 ngày làm việc.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "✅ Đã hủy đặt phòng thành công! Phòng đã được giải phóng.");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ " + e.getMessage());
        }

        return "redirect:/my-bookings";
    }

    // ===== REVIEW =====

    /**
     * User gửi đánh giá cho booking đã COMPLETED.
     *
     * POST /my-bookings/{bookingId}/review
     * Form data: rating (1-5), comment
     */
    @PostMapping("/my-bookings/{bookingId}/review")
    public String submitReview(
            @PathVariable Long bookingId,
            @RequestParam int rating,
            @RequestParam String comment,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes redirectAttributes) {

        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        try {
            User user = userRepository.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản!"));

            reviewService.createReview(user, bookingId, rating, comment);

            redirectAttributes.addFlashAttribute("successMessage",
                    "⭐ Cảm ơn bạn đã đánh giá! Nhận xét của bạn đã được ghi nhận.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ " + e.getMessage());
        }

        return "redirect:/my-bookings";
    }

    // ===== HELPER METHODS =====

    /**
     * Parse ngày từ String (yyyy-MM-dd) → LocalDate.
     * Trả về null nếu parse lỗi.
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Format ngày sang tiếng Việt: "01 Th. 5 2026"
     */
    private String formatDateVN(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return date.format(formatter);
    }
}
