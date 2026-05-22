package com.travelmate.controller.page;

import com.travelmate.dto.AdminRevenueSummaryDto;
import com.travelmate.dto.RevenueItemDto;
import com.travelmate.dto.RoomAvailabilityDto;
import com.travelmate.dto.SettlementDetailItemDto;
import com.travelmate.entity.*;
import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.BookingSource;
import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.entity.enums.DiscountType;
import com.travelmate.entity.enums.PaymentStatus;
import com.travelmate.entity.enums.PartnerWithdrawalStatus;
import com.travelmate.repository.AdminActionLogRepository;
import com.travelmate.repository.PaymentRepository;
import com.travelmate.repository.UserRepository;
import com.travelmate.service.*;
import com.travelmate.entity.TravelPost;
import com.travelmate.service.CommissionService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

    private final BookingService bookingService;
    private final AccommodationService accommodationService;
    private final ReviewService reviewService;
    private final RevenueService revenueService;
    private final VoucherService voucherService;
    private final SettlementService settlementService;
    private final SupportTicketService supportTicketService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AdminActionLogRepository actionLogRepository;
    private final PaymentRepository paymentRepository;
    private final AvailabilityService availabilityService;
    private final CommissionService commissionService;
    private final TravelPostService travelPostService;
    private final PartnerWalletService partnerWalletService;
    private final ExcelExportService excelExportService;

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    public AdminPageController(BookingService bookingService,
                               AccommodationService accommodationService,
                               ReviewService reviewService,
                               RevenueService revenueService,
                               VoucherService voucherService,
                               SettlementService settlementService,
                               SupportTicketService supportTicketService,
                               UserRepository userRepository,
                               UserService userService,
                               AdminActionLogRepository actionLogRepository,
                               PaymentRepository paymentRepository,
                               AvailabilityService availabilityService,
                               CommissionService commissionService,
                               TravelPostService travelPostService,
                               PartnerWalletService partnerWalletService,
                               ExcelExportService excelExportService) {
        this.bookingService = bookingService;
        this.accommodationService = accommodationService;
        this.reviewService = reviewService;
        this.revenueService = revenueService;
        this.voucherService = voucherService;
        this.settlementService = settlementService;
        this.supportTicketService = supportTicketService;
        this.userRepository = userRepository;
        this.userService = userService;
        this.actionLogRepository = actionLogRepository;
        this.paymentRepository = paymentRepository;
        this.availabilityService = availabilityService;
        this.commissionService = commissionService;
        this.travelPostService = travelPostService;
        this.partnerWalletService = partnerWalletService;
        this.excelExportService = excelExportService;
    }

    /** Helper: ghi audit log */
    private void logAction(Authentication auth, String actionType, String targetType,
                           Long targetId, String description, String note) {
        String email = auth != null ? auth.getName() : "admin";
        actionLogRepository.save(new AdminActionLog(email, actionType, targetType, targetId, description, note));
    }

    private User getCurrentAdmin(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new IllegalArgumentException("Không xác định được tài khoản admin!");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản admin!"));
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalBookings = bookingService.getAllBookingsForAdmin().size();
        long pendingBookings = bookingService.countByStatus(BookingStatus.PENDING_ADMIN_APPROVAL);
        long confirmedBookings = bookingService.countByStatus(BookingStatus.CONFIRMED);
        long checkedInBookings = bookingService.countByStatus(BookingStatus.CHECKED_IN);
        long noShowBookings = bookingService.countByStatus(BookingStatus.NO_SHOW);
        long completedBookings = bookingService.countByStatus(BookingStatus.COMPLETED);

        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("pendingBookings", pendingBookings);
        model.addAttribute("confirmedBookings", confirmedBookings);
        model.addAttribute("checkedInBookings", checkedInBookings);
        model.addAttribute("noShowBookings", noShowBookings);
        model.addAttribute("completedBookings", completedBookings);

        model.addAttribute("totalApprovedHotels", bookingService.countApprovedAccommodations());
        model.addAttribute("totalAvailableRooms", bookingService.countTotalAvailableRooms());
        model.addAttribute("approvedRevenue", bookingService.calculateDemoRevenue());

        // Thống kê accommodation & room chờ duyệt
        model.addAttribute("pendingAccommodations", accommodationService.countPendingAccommodations());
        model.addAttribute("pendingRooms", accommodationService.countPendingRooms());

        // Thống kê người dùng từ MySQL
        List<User> allUsers = userRepository.findAll();
        long totalUsers    = allUsers.stream().filter(u -> u.getRole() == User.Role.USER).count();
        long totalPartners = allUsers.stream().filter(u -> u.getRole() == User.Role.PARTNER).count();
        model.addAttribute("totalUsers",    totalUsers);
        model.addAttribute("totalPartners", totalPartners);

        // Thống kê support ticket
        List<SupportTicket> allTickets = supportTicketService.getAllTicketsForAdmin();
        long openTickets = allTickets.stream().filter(t -> "OPEN".equals(t.getStatus())).count();
        model.addAttribute("openSupportTickets", openTickets);

        // Thống kê review
        model.addAttribute("totalReviews", reviewService.countAllReviews());
        model.addAttribute("pendingWithdrawals", partnerWalletService.countPendingWithdrawals());

        return "admin/dashboard";
    }

    /** GET /admin/bookings — danh sách tất cả booking với thống kê. */
    @GetMapping("/bookings")
    public String bookings(Model model) {
        List<Booking> bookings;
        try {
            bookings = bookingService.getAllBookingsForAdmin();
        } catch (Exception e) {
            model.addAttribute("bookings", java.util.Collections.emptyList());
            model.addAttribute("errorMessage", "❌ Lỗi khi tải danh sách đặt phòng: " + e.getMessage());
            model.addAttribute("totalCount", 0L);
            model.addAttribute("pendingCount", 0L);
            model.addAttribute("pendingPaymentCount", 0L);
            model.addAttribute("confirmedCount", 0L);
            model.addAttribute("completedCount", 0L);
            model.addAttribute("noShowCount", 0L);
            model.addAttribute("checkedInCount", 0L);
            model.addAttribute("cancelledCount", 0L);
            model.addAttribute("refundPendingCount", 0L);
            model.addAttribute("refundedCount", 0L);
            model.addAttribute("directCount", 0L);
            model.addAttribute("blockCount", 0L);
            return "admin/bookings";
        }
        model.addAttribute("bookings", bookings);
        model.addAttribute("totalCount",          (long) bookings.size());
        model.addAttribute("pendingPaymentCount", bookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.PENDING_PAYMENT).count());
        model.addAttribute("pendingCount",        bookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.PENDING_ADMIN_APPROVAL).count());
        model.addAttribute("confirmedCount",      bookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED).count());
        model.addAttribute("checkedInCount",      bookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.CHECKED_IN).count());
        model.addAttribute("completedCount",      bookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED).count());
        model.addAttribute("noShowCount",         bookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.NO_SHOW).count());
        model.addAttribute("cancelledCount",      bookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.CANCELLED).count());
        model.addAttribute("refundPendingCount",  bookings.stream().filter(b -> b.getPaymentStatus() == PaymentStatus.REFUND_PENDING).count());
        model.addAttribute("refundedCount",       bookings.stream().filter(b -> b.getPaymentStatus() == PaymentStatus.REFUNDED).count());
        model.addAttribute("directCount",         bookings.stream().filter(b -> b.getBookingSource() == BookingSource.DIRECT).count());
        model.addAttribute("blockCount",          bookings.stream().filter(b -> b.getBookingSource() == BookingSource.MANUAL_BLOCK).count());
        return "admin/bookings";
    }

    /** GET /admin/bookings/{id} — Chi tiết booking */
    @GetMapping("/bookings/{id}")
    public String bookingDetail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            Booking booking = bookingService.findById(id);
            Payment payment = paymentRepository.findByBooking(booking).orElse(null);
            List<AdminActionLog> logs = actionLogRepository
                    .findByTargetTypeAndTargetIdOrderByCreatedAtDesc("BOOKING", id);
            model.addAttribute("booking", booking);
            model.addAttribute("payment", payment);
            model.addAttribute("actionLogs", logs);

            if (booking.getBookingStatus() == BookingStatus.NO_SHOW) {
                BigDecimal forfeited = booking.getPaidAmount() != null ? booking.getPaidAmount() : BigDecimal.ZERO;
                BigDecimal commission = commissionService.calculateCommission(forfeited, booking.getRoom());
                BigDecimal partnerPayout = forfeited.subtract(commission);
                BigDecimal rateDecimal = commissionService.getEffectiveCommissionRate(booking.getRoom());
                BigDecimal ratePercent = rateDecimal.multiply(BigDecimal.valueOf(100))
                        .setScale(1, java.math.RoundingMode.HALF_UP);
                boolean isRoomOverride = commissionService.isRoomOverride(booking.getRoom());
                model.addAttribute("noShowForfeited", forfeited);
                model.addAttribute("noShowCommission", commission);
                model.addAttribute("noShowPartnerPayout", partnerPayout);
                model.addAttribute("noShowCommissionRate", ratePercent);
                model.addAttribute("noShowIsRoomOverride", isRoomOverride);
            }

            return "admin/booking-detail";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/bookings";
        }
    }

    @PostMapping("/bookings/{id}/approve")
    public String approveBooking(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            Booking b = bookingService.approveBookingByAdmin(id);
            logAction(auth, "APPROVE_BOOKING", "BOOKING", id,
                    "Xác nhận thanh toán booking " + b.getBookingCode(), null);
            ra.addFlashAttribute("successMessage", "✅ Đã xác nhận thanh toán booking. Đơn đã chuyển sang Partner để xác nhận giữ phòng.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    @PostMapping("/bookings/{id}/reject")
    public String rejectBooking(@PathVariable Long id,
                                @RequestParam(required = false) String rejectReason,
                                Authentication auth, RedirectAttributes ra) {
        try {
            Booking b = bookingService.rejectBookingByAdmin(id, rejectReason);
            logAction(auth, "REJECT_BOOKING", "BOOKING", id,
                    "Từ chối xác nhận thanh toán booking " + b.getBookingCode(), rejectReason);
            ra.addFlashAttribute("successMessage", "Đã từ chối xác nhận thanh toán. Booking đã bị hủy và quota phòng đã được mở lại.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    @PostMapping("/bookings/{id}/check-in")
    public String checkInBooking(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            Booking b = bookingService.checkInBookingByAdmin(id);
            logAction(auth, "ADMIN_OVERRIDE_CHECK_IN", "BOOKING", id,
                    "[Override] Admin ghi đè check-in: " + b.getBookingCode(),
                    "Admin override — Partner không thao tác được hoặc can thiệp khẩn");
            ra.addFlashAttribute("successMessage", "✅ [Override] Đã ghi đè check-in thành công!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/bookings/{id}".replace("{id}", id.toString());
    }

    @PostMapping("/bookings/{id}/complete")
    public String completeBooking(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            Booking b = bookingService.completeBookingByAdmin(id);
            logAction(auth, "ADMIN_OVERRIDE_CHECK_OUT", "BOOKING", id,
                    "[Override] Admin ghi đè check-out: " + b.getBookingCode(),
                    "Admin override — Partner không thao tác được hoặc can thiệp khẩn");
            ra.addFlashAttribute("successMessage", "✅ [Override] Đã ghi đè hoàn tất đơn!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/bookings/{id}".replace("{id}", id.toString());
    }

    @PostMapping("/bookings/{id}/mark-no-show")
    public String markNoShow(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            Booking b = bookingService.markNoShow(id);
            logAction(auth, "ADMIN_OVERRIDE_NO_SHOW", "BOOKING", id,
                    "[Override] Admin xử lý no-show: " + b.getBookingCode(),
                    "Admin override — Partner báo cáo không đến hoặc can thiệp khẩn");
            ra.addFlashAttribute("successMessage",
                "✅ [Override] Đã xử lý no-show. Kiểm tra chi tiết booking để xem kết quả.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/bookings/{id}".replace("{id}", id.toString());
    }

    /** POST /admin/bookings/{id}/handle-partner-cancelled — Admin xử lý khi partner hủy giữ phòng */
    @PostMapping("/bookings/{id}/handle-partner-cancelled")
    public String handlePartnerCancelled(@PathVariable Long id,
                                         @RequestParam(required = false) String reason,
                                         Authentication auth, RedirectAttributes ra) {
        try {
            Booking b = bookingService.handlePartnerCancelledByAdmin(id, reason);
            logAction(auth, "PARTNER_CANCELLED_RESOLVED", "BOOKING", id,
                    "Hủy đơn do partner từ chối giữ phòng: " + b.getBookingCode(), reason);
            ra.addFlashAttribute("successMessage",
                "✅ Đã hủy đơn và trả lại phòng do partner từ chối!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    /** POST /admin/bookings/{id}/mark-refunded — Admin xác nhận đã hoàn tiền cho khách */
    @PostMapping("/bookings/{id}/mark-refunded")
    public String markRefunded(@PathVariable Long id,
                               @RequestParam(required = false) String note,
                               Authentication auth, RedirectAttributes ra) {
        try {
            Booking b = bookingService.markRefundedByAdmin(id, note);
            logAction(auth, "REFUND_CONFIRMED", "BOOKING", id,
                    "Xác nhận hoàn tiền: " + b.getBookingCode(), note);
            ra.addFlashAttribute("successMessage",
                "✅ Đã xác nhận hoàn tiền thành công cho đơn " + b.getBookingCode() + "!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/admin/bookings/" + id;
    }

    // ─── Accommodation Management ────────────────────────────────────────────
    @GetMapping("/accommodations")
    public String accommodations(Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        List<Accommodation> accommodations = accommodationService.getAllAccommodationsForAdmin();
        model.addAttribute("accommodations", accommodations);
        model.addAttribute("totalCount",    (long) accommodations.size());
        model.addAttribute("pendingCount",  accommodationService.countPendingAccommodations());
        model.addAttribute("approvedCount", accommodationService.countApprovedAccommodations());
        model.addAttribute("rejectedCount", accommodationService.countRejectedAccommodations());
        return "admin/accommodations";
    }

    /**
     * POST /admin/accommodations/{id}/approve — Admin duyệt accommodation.
     * Sau khi duyệt, User mới thấy listing này trên /accommodations.
     */
    @PostMapping("/accommodations/{id}/approve")
    public String approveAccommodation(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            Accommodation a = accommodationService.approveAccommodation(id);
            logAction(auth, "APPROVE_LISTING", "ACCOMMODATION", id,
                    "Duyệt listing: " + a.getName(), null);
            ra.addFlashAttribute("successMessage", "✅ Đã duyệt nơi lưu trú thành công!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/admin/accommodations";
    }

    @PostMapping("/accommodations/{id}/reject")
    public String rejectAccommodation(@PathVariable Long id,
                                      @RequestParam(required = false) String rejectReason,
                                      Authentication auth, RedirectAttributes ra) {
        try {
            Accommodation a = accommodationService.rejectAccommodation(id, rejectReason);
            logAction(auth, "REJECT_LISTING", "ACCOMMODATION", id,
                    "Từ chối listing: " + a.getName(), rejectReason);
            ra.addFlashAttribute("successMessage", "Đã từ chối nơi lưu trú.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/admin/accommodations";
    }

    @GetMapping("/users")
    public String users(Model model) {
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        long totalUsers    = users.stream().filter(u -> u.getRole() == User.Role.USER).count();
        long totalPartners = users.stream().filter(u -> u.getRole() == User.Role.PARTNER).count();
        long totalAdmins   = users.stream().filter(u -> u.getRole() == User.Role.ADMIN).count();
        model.addAttribute("users",         users);
        model.addAttribute("totalUsers",    totalUsers);
        model.addAttribute("totalPartners", totalPartners);
        model.addAttribute("totalAdmins",   totalAdmins);
        return "admin/users";
    }

    /** POST /admin/users/{id}/lock — Admin khóa tài khoản (không khóa được Admin) */
    @PostMapping("/users/{id}/lock")
    public String lockUser(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            User u = userService.lockUser(id);
            logAction(auth, "LOCK_USER", "USER", id,
                    "Khóa tài khoản: " + u.getEmail(), null);
            ra.addFlashAttribute("successMessage", "🔒 Đã khóa tài khoản #" + id + "!");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/unlock")
    public String unlockUser(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            User u = userService.unlockUser(id);
            logAction(auth, "UNLOCK_USER", "USER", id,
                    "Mở khóa tài khoản: " + u.getEmail(), null);
            ra.addFlashAttribute("successMessage", "🔓 Đã mở khóa tài khoản #" + id + "!");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/listings")
    public String listings() {
        return "redirect:/admin/accommodations";
    }

    @GetMapping("/rooms")
    public String rooms(Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        List<Room> rooms = accommodationService.getAllRoomsForAdmin();
        long pendingCount  = accommodationService.countPendingRooms();
        long approvedCount = accommodationService.countApprovedRooms();
        long rejectedCount = rooms.stream()
                .filter(r -> r.getApprovalStatus() == ApprovalStatus.REJECTED).count();

        model.addAttribute("rooms",         rooms);
        model.addAttribute("totalCount",    (long) rooms.size());
        model.addAttribute("pendingCount",  pendingCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        return "admin/rooms";
    }

    /** POST /admin/rooms/{id}/approve — Admin duyệt phòng */
    @PostMapping("/rooms/{id}/approve")
    public String approveRoom(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            Room r = accommodationService.approveRoom(id);
            logAction(auth, "APPROVE_ROOM", "ROOM", id,
                    "Duyệt phòng: " + r.getRoomCode(), null);
            ra.addFlashAttribute("successMessage", "✅ Đã duyệt phòng thành công!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/admin/rooms";
    }

    @PostMapping("/rooms/{id}/reject")
    public String rejectRoom(@PathVariable Long id,
                             @RequestParam(required = false) String rejectReason,
                             Authentication auth, RedirectAttributes ra) {
        try {
            Room r = accommodationService.rejectRoom(id, rejectReason);
            logAction(auth, "REJECT_ROOM", "ROOM", id,
                    "Từ chối phòng: " + r.getRoomCode(), rejectReason);
            ra.addFlashAttribute("successMessage", "Đã từ chối phòng.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/admin/rooms";
    }

    @GetMapping("/vouchers")
    public String vouchers(Model model) {
        List<Voucher> vouchers = voucherService.getAllVouchersForAdmin();
        long totalCount  = vouchers.size();
        long activeCount = vouchers.stream().filter(v -> Boolean.TRUE.equals(v.getActive())).count();
        long inactiveCount = totalCount - activeCount;
        model.addAttribute("vouchers", vouchers);
        model.addAttribute("totalCount",    totalCount);
        model.addAttribute("activeCount",   activeCount);
        model.addAttribute("inactiveCount", inactiveCount);
        return "admin/vouchers";
    }

    /** POST /admin/vouchers/create — Admin tạo voucher USER_GLOBAL */
    @PostMapping("/vouchers/create")
    public String createVoucher(
            @RequestParam String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam String discountType,
            @RequestParam BigDecimal discountValue,
            @RequestParam(required = false) BigDecimal maxDiscountAmount,
            @RequestParam(required = false) BigDecimal minOrderAmount,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication auth, RedirectAttributes ra) {
        try {
            LocalDate start = (startDate != null && !startDate.isBlank()) ? LocalDate.parse(startDate) : null;
            LocalDate end   = (endDate   != null && !endDate.isBlank())   ? LocalDate.parse(endDate)   : null;
            Voucher v = voucherService.createAdminVoucher(code, name, description,
                    DiscountType.valueOf(discountType), discountValue,
                    maxDiscountAmount, minOrderAmount, start, end);
            logAction(auth, "CREATE_VOUCHER", "VOUCHER", v.getId(),
                    "Tạo voucher '" + code + "' (" + discountType + " " + discountValue + ")", null);
            ra.addFlashAttribute("successMessage", "✅ Tạo voucher '" + code + "' thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/admin/vouchers";
    }

    /** POST /admin/vouchers/{id}/toggle — bật/tắt voucher */
    @PostMapping("/vouchers/{id}/toggle")
    public String toggleVoucher(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            Voucher v = voucherService.toggleActive(id);
            boolean isActive = Boolean.TRUE.equals(v.getActive());
            logAction(auth, "TOGGLE_VOUCHER", "VOUCHER", id,
                    (isActive ? "Bật" : "Tắt") + " voucher '" + v.getCode() + "'", null);
            ra.addFlashAttribute("successMessage",
                    (isActive ? "✅ Đã bật" : "⏸ Đã tắt") + " voucher '" + v.getCode() + "'");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/admin/vouchers";
    }

    @GetMapping("/revenue")
    public String revenue(Model model) {
        AdminRevenueSummaryDto summary = revenueService.calculateAdminRevenueSummary();
        List<RevenueItemDto> items     = revenueService.getRevenueItemsForAdmin();
        model.addAttribute("summary", summary);
        model.addAttribute("revenueItems", items);
        return "admin/revenue";
    }

    // ─── Settlements ──────────────────────────────────────────────────────────

    @GetMapping("/settlements")
    public String settlements(Model model) {
        List<PartnerSettlement> settlements = settlementService.getAllSettlementsForAdmin();
        long pendingCount = settlements.stream().filter(s -> s.getSettlementStatus().name().equals("PENDING")).count();
        long paidCount    = settlements.stream().filter(s -> s.getSettlementStatus().name().equals("PAID")).count();
        model.addAttribute("settlements",  settlements);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("paidCount",    paidCount);
        return "admin/settlements";
    }

    /** GET /admin/settlements/{id} — Trang chi tiết từng booking trong settlement */
    @GetMapping("/settlements/{id}")
    public String settlementDetail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            PartnerSettlement settlement = settlementService.findById(id);
            List<SettlementDetailItemDto> breakdown = settlementService.getBreakdownForSettlement(settlement);
            model.addAttribute("settlement", settlement);
            model.addAttribute("breakdown", breakdown);
            return "admin/settlement-detail";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
            return "redirect:/admin/settlements";
        }
    }

    /** GET /admin/settlements/{id}/export-excel — Xuất file đối soát settlement */
    @GetMapping("/settlements/{id}/export-excel")
    public ResponseEntity<byte[]> exportSettlementExcel(@PathVariable Long id) {
        try {
            PartnerSettlement settlement = settlementService.findById(id);
            List<SettlementDetailItemDto> breakdown = settlementService.getBreakdownForSettlement(settlement);
            byte[] bytes = excelExportService.exportSettlementDetail(settlement, breakdown);

            return ResponseEntity.ok()
                    .contentType(XLSX_MEDIA_TYPE)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"settlement-" + id + ".xlsx\"")
                    .body(bytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** POST /admin/settlements/generate-monthly — Tạo settlement cho tất cả partner trong tháng trước */
    @PostMapping("/settlements/generate-monthly")
    public String generateMonthlySettlements(Authentication auth, RedirectAttributes ra) {
        try {
            List<PartnerSettlement> created = settlementService.generateMonthlySettlements();
            if (created.isEmpty()) {
                ra.addFlashAttribute("successMessage",
                        "ℹ️ Không có partner nào có doanh thu đủ điều kiện quyết toán trong tháng trước, hoặc settlement đã được tạo rồi.");
            } else {
                logAction(auth, "GENERATE_SETTLEMENT", "SETTLEMENT", null,
                        "Generate monthly settlement: tạo " + created.size() + " settlement mới cho tháng trước", null);
                ra.addFlashAttribute("successMessage",
                        "✅ Đã tạo " + created.size() + " settlement mới cho tháng trước!");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/admin/settlements";
    }

    /** POST /admin/settlements/{id}/mark-paid — Admin đánh dấu đã thanh toán */
    @PostMapping("/settlements/{id}/mark-paid")
    public String markSettlementPaid(
            @PathVariable Long id,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) String transactionCode,
            @RequestParam(required = false) String transferDate,
            Authentication auth, RedirectAttributes ra) {
        try {
            // Tổng hợp ghi chú từ các trường nhập
            String combinedNote = buildSettlementNote(note, transactionCode, transferDate);
            User admin = getCurrentAdmin(auth);
            PartnerSettlement s = settlementService.markSettlementPaid(id, combinedNote, admin);
            logAction(auth, "MARK_SETTLEMENT_PAID", "SETTLEMENT", id,
                    "Thanh toán quyết toán #" + id + " cho partner: " + s.getPartner().getName(), combinedNote);
            ra.addFlashAttribute("successMessage", "✅ Đã thanh toán settlement #" + id + " và cộng tiền vào ví Partner!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/admin/settlements";
    }

    /** Helper: xây dựng ghi chú quyết toán từ form nhập */
    private String buildSettlementNote(String note, String transactionCode, String transferDate) {
        StringBuilder sb = new StringBuilder();
        if (transactionCode != null && !transactionCode.isBlank()) {
            sb.append("Mã GD: ").append(transactionCode.trim());
        }
        if (transferDate != null && !transferDate.isBlank()) {
            if (!sb.isEmpty()) sb.append(" | ");
            sb.append("Ngày CK: ").append(transferDate.trim());
        }
        if (note != null && !note.isBlank()) {
            if (!sb.isEmpty()) sb.append(" | ");
            sb.append(note.trim());
        }
        return sb.isEmpty() ? "Admin đã chuyển khoản." : sb.toString();
    }

    // ─── Partner Withdrawals ─────────────────────────────────────────────────

    @GetMapping("/withdrawals")
    public String withdrawals(Model model) {
        List<PartnerWithdrawalRequest> withdrawals = partnerWalletService.getAllWithdrawalsForAdmin();
        long pendingCount = withdrawals.stream()
                .filter(w -> w.getWithdrawalStatus() == PartnerWithdrawalStatus.PENDING).count();
        long paidCount = withdrawals.stream()
                .filter(w -> w.getWithdrawalStatus() == PartnerWithdrawalStatus.PAID).count();
        long rejectedCount = withdrawals.stream()
                .filter(w -> w.getWithdrawalStatus() == PartnerWithdrawalStatus.REJECTED).count();
        BigDecimal pendingAmount = withdrawals.stream()
                .filter(w -> w.getWithdrawalStatus() == PartnerWithdrawalStatus.PENDING)
                .map(w -> w.getAmount() != null ? w.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("withdrawals", withdrawals);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("paidCount", paidCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("pendingAmount", pendingAmount);
        return "admin/withdrawals";
    }

    /** GET /admin/withdrawals/export-excel — Xuất danh sách yêu cầu rút tiền */
    @GetMapping("/withdrawals/export-excel")
    public ResponseEntity<byte[]> exportWithdrawalsExcel() {
        List<PartnerWithdrawalRequest> withdrawals = partnerWalletService.getAllWithdrawalsForAdmin();
        byte[] bytes = excelExportService.exportWithdrawals(withdrawals);

        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"partner-withdrawals.xlsx\"")
                .body(bytes);
    }

    @PostMapping("/withdrawals/{id}/mark-paid")
    public String markWithdrawalPaid(@PathVariable Long id,
                                     @RequestParam(required = false) String adminNote,
                                     Authentication auth,
                                     RedirectAttributes ra) {
        try {
            User admin = getCurrentAdmin(auth);
            PartnerWithdrawalRequest request = partnerWalletService.markWithdrawalPaid(id, admin, adminNote);
            logAction(auth, "MARK_WITHDRAWAL_PAID", "WITHDRAWAL", id,
                    "Xác nhận đã chuyển khoản yêu cầu rút " + request.getRequestCode(), adminNote);
            ra.addFlashAttribute("successMessage",
                    "✅ Đã xác nhận chuyển khoản cho yêu cầu " + request.getRequestCode() + "!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/admin/withdrawals";
    }

    @PostMapping("/withdrawals/{id}/reject")
    public String rejectWithdrawal(@PathVariable Long id,
                                   @RequestParam(required = false) String adminNote,
                                   Authentication auth,
                                   RedirectAttributes ra) {
        try {
            User admin = getCurrentAdmin(auth);
            PartnerWithdrawalRequest request = partnerWalletService.rejectWithdrawal(id, admin, adminNote);
            logAction(auth, "REJECT_WITHDRAWAL", "WITHDRAWAL", id,
                    "Từ chối yêu cầu rút " + request.getRequestCode(), adminNote);
            ra.addFlashAttribute("successMessage",
                    "✅ Đã từ chối yêu cầu " + request.getRequestCode() + " và hoàn tiền về ví Partner!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/admin/withdrawals";
    }

    // ─── Travel Posts Management ──────────────────────────────────────────────

    /**
     * GET /admin/travel-posts — Trang quản lý nội dung du lịch (CMS mini).
     * Admin xem toàn bộ bài viết (cả ẩn), có thể thêm/ẩn/xóa.
     * User/Guest không thấy trang này — bảo vệ bởi Spring Security.
     */
    @GetMapping("/travel-posts")
    public String travelPosts(Model model) {
        List<TravelPost> posts = travelPostService.getAllForAdmin();
        long visibleCount = posts.stream()
                .filter(p -> p.getStatus() == TravelPost.Status.VISIBLE).count();
        long hiddenCount  = posts.stream()
                .filter(p -> p.getStatus() == TravelPost.Status.HIDDEN).count();

        model.addAttribute("posts",        posts);
        model.addAttribute("totalCount",   (long) posts.size());
        model.addAttribute("visibleCount", visibleCount);
        model.addAttribute("hiddenCount",  hiddenCount);
        return "admin/travel-posts";
    }

    @GetMapping("/places")
    public String places() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/support")
    public String support(@RequestParam(required = false, defaultValue = "all") String tab,
                          Model model) {
        List<SupportTicket> allTickets = supportTicketService.getAllTicketsForAdmin();

        // Tab filter
        List<SupportTicket> tickets = switch (tab) {
            case "partner"   -> supportTicketService.getTicketsByRole("PARTNER");
            case "user"      -> {
                List<SupportTicket> u = new java.util.ArrayList<>();
                u.addAll(supportTicketService.getTicketsByRole("USER"));
                u.addAll(supportTicketService.getTicketsByRole("GUEST"));
                u.sort(java.util.Comparator.comparing(SupportTicket::getCreatedAt).reversed());
                yield u;
            }
            case "open"      -> allTickets.stream().filter(t -> "OPEN".equals(t.getStatus())).toList();
            case "responded" -> allTickets.stream().filter(t -> "RESPONDED".equals(t.getStatus())).toList();
            case "closed"    -> allTickets.stream().filter(t -> "CLOSED".equals(t.getStatus())).toList();
            default          -> allTickets;
        };

        long openCount       = allTickets.stream().filter(t -> "OPEN".equals(t.getStatus())).count();
        long respondedCount  = allTickets.stream().filter(t -> "RESPONDED".equals(t.getStatus())).count();
        long closedCount     = allTickets.stream().filter(t -> "CLOSED".equals(t.getStatus())).count();
        long partnerCount    = allTickets.stream().filter(t -> "PARTNER".equals(t.getRequesterRole())).count();
        long userGuestCount  = allTickets.stream().filter(t -> !"PARTNER".equals(t.getRequesterRole())).count();

        model.addAttribute("tickets",        tickets);
        model.addAttribute("allCount",       (long) allTickets.size());
        model.addAttribute("openCount",      openCount);
        model.addAttribute("respondedCount", respondedCount);
        model.addAttribute("closedCount",    closedCount);
        model.addAttribute("partnerCount",   partnerCount);
        model.addAttribute("userGuestCount", userGuestCount);
        model.addAttribute("activeTab",      tab);
        return "admin/support";
    }

    /** POST /admin/support/{id}/respond — Admin phản hồi ticket */
    @PostMapping("/support/{id}/respond")
    public String respondToTicket(@PathVariable Long id,
                                  @RequestParam String adminResponse,
                                  Authentication auth,
                                  RedirectAttributes ra) {
        try {
            supportTicketService.respondToTicket(id, adminResponse);
            logAction(auth, "RESPOND_TICKET", "TICKET", id, "Phản hồi ticket #" + id, null);
            ra.addFlashAttribute("successMessage", "✅ Đã gửi phản hồi cho ticket #" + id + "!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/admin/support";
    }


    /** POST /admin/support/{id}/close — Admin đóng ticket */
    @PostMapping("/support/{id}/close")
    public String closeTicket(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            supportTicketService.closeTicket(id);
            logAction(auth, "CLOSE_TICKET", "TICKET", id, "Đóng ticket #" + id, null);
            ra.addFlashAttribute("successMessage", "✅ Đã đóng ticket #" + id + "!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/admin/support";
    }

    /** POST /admin/support/{id}/reopen — Admin mở lại ticket đã đóng */
    @PostMapping("/support/{id}/reopen")
    public String reopenTicket(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            supportTicketService.reopenTicket(id);
            logAction(auth, "REOPEN_TICKET", "TICKET", id, "Mở lại ticket #" + id, null);
            ra.addFlashAttribute("successMessage", "🔄 Đã mở lại ticket #" + id + "!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/admin/support";
    }

    // ─── Review Management ────────────────────────────────────────────────────

    @GetMapping("/reviews")
    public String reviews(Model model) {
        List<Review> reviews = reviewService.getAllReviewsForAdmin();
        long visibleCount = reviews.stream().filter(r -> !Boolean.TRUE.equals(r.getIsHidden())).count();
        long hiddenCount  = reviews.stream().filter(r ->  Boolean.TRUE.equals(r.getIsHidden())).count();
        double avgRating  = reviews.stream().filter(r -> !Boolean.TRUE.equals(r.getIsHidden()))
                .mapToInt(Review::getRating).average().orElse(0.0);
        model.addAttribute("reviews",      reviews);
        model.addAttribute("totalCount",   (long) reviews.size());
        model.addAttribute("visibleCount", visibleCount);
        model.addAttribute("hiddenCount",  hiddenCount);
        model.addAttribute("avgRating",    Math.round(avgRating * 10.0) / 10.0);
        return "admin/reviews";
    }

    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            reviewService.hideReview(id);
            logAction(auth, "HIDE_REVIEW", "REVIEW", id, "Ẩn đánh giá #" + id, null);
            ra.addFlashAttribute("successMessage", "✅ Đã ẩn đánh giá thành công!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/admin/reviews";
    }

    @PostMapping("/reviews/{id}/unhide")
    public String unhideReview(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            reviewService.unhideReview(id);
            logAction(auth, "UNHIDE_REVIEW", "REVIEW", id, "Hiện lại đánh giá #" + id, null);
            ra.addFlashAttribute("successMessage", "✅ Đã hiện lại đánh giá!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/admin/reviews";
    }

    // ─── Audit Log ────────────────────────────────────────────────────────────

    @GetMapping("/audit-log")
    public String auditLog(Model model) {
        List<AdminActionLog> logs = actionLogRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("logs", logs);
        model.addAttribute("totalCount", (long) logs.size());
        return "admin/audit-log";
    }

    // ─── Availability ─────────────────────────────────────────────────────────

    @GetMapping("/availability")
    public String availability(
            @RequestParam(required = false) String checkIn,
            @RequestParam(required = false) String checkOut,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String propertyType,
            @RequestParam(required = false) Long ownerId,
            Model model) {

        LocalDate ciDate = null;
        LocalDate coDate = null;
        List<RoomAvailabilityDto> results = null;
        AvailabilityService.AvailabilitySummary summary = null;
        String errorMsg = null;

        if (checkIn != null && !checkIn.isBlank() && checkOut != null && !checkOut.isBlank()) {
            try {
                ciDate = LocalDate.parse(checkIn);
                coDate = LocalDate.parse(checkOut);
                if (!coDate.isAfter(ciDate)) {
                    errorMsg = "Ngày trả phòng phải sau ngày nhận phòng!";
                } else {
                    results = availabilityService.checkAvailabilityForAdmin(
                            ciDate, coDate, city, propertyType, ownerId);
                    summary = availabilityService.buildSummary(results);
                }
            } catch (Exception e) {
                errorMsg = "Ngày không hợp lệ. Vui lòng chọn lại!";
            }
        }

        // Danh sách partner cho bộ lọc
        List<User> partners = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.PARTNER)
                .sorted((a, b) -> {
                    String na = a.getName() != null ? a.getName() : a.getEmail();
                    String nb = b.getName() != null ? b.getName() : b.getEmail();
                    return na.compareToIgnoreCase(nb);
                })
                .collect(java.util.stream.Collectors.toList());

        long pendingCount = bookingService.countByStatus(BookingStatus.PENDING_ADMIN_APPROVAL);

        model.addAttribute("checkIn",      checkIn);
        model.addAttribute("checkOut",     checkOut);
        model.addAttribute("city",         city);
        model.addAttribute("propertyType", propertyType);
        model.addAttribute("ownerId",      ownerId);
        model.addAttribute("results",      results);
        model.addAttribute("summary",      summary);
        model.addAttribute("errorMsg",     errorMsg);
        model.addAttribute("partners",     partners);
        model.addAttribute("pendingCount", pendingCount);
        return "admin/availability";
    }
}
