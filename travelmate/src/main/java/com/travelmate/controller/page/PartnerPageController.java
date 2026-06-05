package com.travelmate.controller.page;

import com.travelmate.dto.PartnerRevenueSummaryDto;
import com.travelmate.dto.RevenueItemDto;
import com.travelmate.dto.RoomAvailabilityDto;
import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Amenity;
import com.travelmate.entity.Booking;
import com.travelmate.entity.PartnerWallet;
import com.travelmate.entity.PartnerWalletTransaction;
import com.travelmate.entity.PartnerWithdrawalRequest;
import com.travelmate.entity.PartnerSettlement;
import com.travelmate.entity.Review;
import com.travelmate.entity.Room;
import com.travelmate.entity.RoomVoucherAssignment;
import com.travelmate.entity.SupportTicket;
import com.travelmate.entity.User;
import com.travelmate.entity.Voucher;
import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.BookingSource;
import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.entity.enums.PartnerBookingStatus;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.entity.enums.RoomCategory;
import com.travelmate.entity.enums.VoucherCostBearer;
import com.travelmate.service.AvailabilityService.RoomStatusDto;
import com.travelmate.repository.UserRepository;
import com.travelmate.security.CustomUserDetails;
import com.travelmate.service.AccommodationService;
import com.travelmate.service.AvailabilityService;
import com.travelmate.service.BookingService;
import com.travelmate.service.CommissionService;
import com.travelmate.service.RevenueService;
import com.travelmate.service.ReviewService;
import com.travelmate.service.SettlementService;
import com.travelmate.service.PartnerWalletService;
import com.travelmate.service.FileStorageService;
import com.travelmate.service.RoomImageService;
import com.travelmate.service.SupportTicketService;
import com.travelmate.service.VoucherService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.travelmate.entity.AdminActionLog;
import com.travelmate.entity.enums.PaymentOption;
import com.travelmate.entity.enums.RemainingPaymentStatus;
import com.travelmate.repository.AdminActionLogRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * PartnerPageController - Page Controller cho tất cả trang của PARTNER.
 *
 * Tất cả route /partner/** được bảo vệ bởi Spring Security.
 * Chỉ user có role PARTNER mới được vào.
 *
 * Ownership rule: tất cả query đều filter theo accommodation.owner = currentPartner.
 */
@SuppressWarnings("null")
@Controller
@RequestMapping("/partner")
public class PartnerPageController {

    private static final String BANK_ACCOUNT_PATTERN = "\\d{6,30}";

    private final BookingService bookingService;
    private final AccommodationService accommodationService;
    private final UserRepository userRepository;
    private final RevenueService revenueService;
    private final ReviewService reviewService;
    private final VoucherService voucherService;
    private final SettlementService settlementService;
    private final SupportTicketService supportTicketService;
    private final FileStorageService fileStorageService;
    private final RoomImageService roomImageService;
    private final AvailabilityService availabilityService;
    private final AdminActionLogRepository actionLogRepository;
    private final CommissionService commissionService;
    private final PartnerWalletService partnerWalletService;

    public PartnerPageController(BookingService bookingService,
                                 AccommodationService accommodationService,
                                 UserRepository userRepository,
                                 RevenueService revenueService,
                                 ReviewService reviewService,
                                 VoucherService voucherService,
                                 SettlementService settlementService,
                                 SupportTicketService supportTicketService,
                                 FileStorageService fileStorageService,
                                 RoomImageService roomImageService,
                                 AvailabilityService availabilityService,
                                 AdminActionLogRepository actionLogRepository,
                                 CommissionService commissionService,
                                 PartnerWalletService partnerWalletService) {
        this.bookingService = bookingService;
        this.accommodationService = accommodationService;
        this.userRepository = userRepository;
        this.revenueService = revenueService;
        this.reviewService = reviewService;
        this.voucherService = voucherService;
        this.settlementService = settlementService;
        this.supportTicketService = supportTicketService;
        this.fileStorageService = fileStorageService;
        this.roomImageService = roomImageService;
        this.availabilityService = availabilityService;
        this.actionLogRepository = actionLogRepository;
        this.commissionService = commissionService;
        this.partnerWalletService = partnerWalletService;
    }

    /** Helper: ghi audit log cho thao tác Partner */
    private void logPartnerAction(User partner, String actionType, String targetType,
                                  Long targetId, String description, String note) {
        String email = partner != null ? partner.getEmail() : "partner";
        actionLogRepository.save(new AdminActionLog(email, actionType, targetType, targetId, description, note));
    }

    /**
     * Helper: thêm dynamic UI labels vào Model theo loại lưu trú của partner.
     * VILLA  → quản lý theo căn.
     * RESORT → quản lý theo loại phòng / suite.
     * Others → quản lý theo phòng.
     */
    private void addUnitLabels(Model model, User partner) {
        PropertyType propType = partner.getPartnerPropertyType();
        boolean isVilla = propType == PropertyType.VILLA;
        boolean isResort = propType == PropertyType.RESORT;
        model.addAttribute("partnerPropertyType", propType);
        model.addAttribute("isVillaPartner",  isVilla);
        model.addAttribute("isResortPartner", isResort);
        model.addAttribute("unitLabel",       isVilla ? "căn"        : "phòng");
        model.addAttribute("unitLabelCap",    isVilla ? "Căn"        : "Phòng");
        model.addAttribute("unitTypeLabel",   isVilla ? "loại căn"   : (isResort ? "loại phòng / suite" : "loại phòng"));
        model.addAttribute("unitTypeLabelCap", isVilla ? "Căn villa" : (isResort ? "Phòng / suite" : "Phòng"));
        model.addAttribute("inventoryLabel",  isVilla ? "căn villa" : (isResort ? "phòng / suite Resort" : "phòng"));
        model.addAttribute("addUnitLabel",    isVilla ? "Thêm căn"   : (isResort ? "Thêm loại phòng / suite" : "Thêm phòng"));
        model.addAttribute("amenityMenuLabel", isVilla ? "Tiện nghi căn" : (isResort ? "Tiện nghi phòng / suite" : "Tiện nghi phòng"));
        model.addAttribute("blockUnitLabel",  isVilla ? "Chặn căn"   : "Chặn phòng");
        model.addAttribute("bookingUnitLabel", isVilla ? "đặt căn"   : "đặt phòng");
        model.addAttribute("unitStatusTitle", isVilla ? "Tình trạng căn hôm nay"    : "Tình trạng phòng hôm nay");
        model.addAttribute("quotaLabel",      isVilla ? "Tổng quota căn TravelMate" : "Tổng quota phòng TravelMate");
        model.addAttribute("checkinLabel",    isVilla ? "Chờ nhận căn"              : "Chờ check-in");
        model.addAttribute("propertyWord",    isVilla ? "villa"
                : (propType == PropertyType.HOMESTAY ? "homestay"
                : (propType == PropertyType.RESORT   ? "resort" : "cơ sở")));
    }

    private String getUnitLabel(User partner) {
        return partner.getPartnerPropertyType() == PropertyType.VILLA ? "căn" : "phòng";
    }

    private String getUnitLabelCap(User partner) {
        return partner.getPartnerPropertyType() == PropertyType.VILLA ? "Căn" : "Phòng";
    }

    private List<String> storePartnerRoomImages(MultipartFile[] roomImages) throws Exception {
        List<String> urls = new ArrayList<>();
        if (roomImages == null) {
            return urls;
        }
        int selectedCount = 0;
        for (MultipartFile file : roomImages) {
            if (file != null && !file.isEmpty()) {
                selectedCount++;
            }
        }
        if (selectedCount > 3) {
            throw new IllegalArgumentException("Mỗi phòng/căn chỉ được upload tối đa 3 ảnh.");
        }
        for (MultipartFile file : roomImages) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String storedUrl = fileStorageService.storeRoomImage(file);
            if (storedUrl != null && !storedUrl.isBlank()) {
                urls.add(storedUrl);
            }
        }
        return urls;
    }

    private String getAccommodationNamePlaceholder(PropertyType propertyType) {
        if (propertyType == null) {
            return "VD: LATA Hotel & Apartments";
        }
        return switch (propertyType) {
            case RESORT -> "VD: Vinpearl Resort & Spa Nha Trang";
            case VILLA -> "VD: The Anam Villa Nha Trang";
            case HOMESTAY -> "VD: Mộc Nhiên Garden Homestay";
            case HOTEL -> "VD: LATA Hotel & Apartments";
        };
    }

    // ─── Lấy partner hiện tại từ Security context ────────────────────────────

    private User getCurrentPartner(CustomUserDetails userDetails) {
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản partner!"));
    }

    // ─── Dashboard ────────────────────────────────────────────────────────────

    /**
     * GET /partner/dashboard — Tổng quan cho partner.
     */
    @GetMapping({"/dashboard", ""})
    public String dashboard(Model model,
                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User partner = getCurrentPartner(userDetails);
        List<Booking> bookings = bookingService.getBookingsForPartner(partner);
        List<Accommodation> accommodations = accommodationService.getAccommodationsByOwner(partner);

        // Stats booking
        long totalBookings = bookings.size();
        long pendingConfirmCount = bookings.stream()
                .filter(b -> b.getPartnerStatus() == PartnerBookingStatus.PENDING_PARTNER_CONFIRMATION)
                .count();
        long confirmedCount = bookings.stream()
                .filter(b -> b.getPartnerStatus() == PartnerBookingStatus.PARTNER_CONFIRMED)
                .count();
        long completedCount = bookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED)
                .count();
        long noShowCount = bookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.NO_SHOW)
                .count();

        // Breakdown by booking source
        long onlineBookingCount = bookings.stream()
                .filter(b -> b.getBookingSource() == BookingSource.ONLINE)
                .count();
        long directBookingCount = bookings.stream()
                .filter(b -> b.getBookingSource() == BookingSource.DIRECT)
                .count();
        long blockCount = bookings.stream()
                .filter(b -> b.getBookingSource() == BookingSource.MANUAL_BLOCK)
                .count();

        // "Việc cần xử lý hôm nay"
        LocalDate today = LocalDate.now();
        // checkinToday: chỉ tính ONLINE (không tính MANUAL_BLOCK)
        long checkinTodayCount = bookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED
                          && today.equals(b.getCheckIn())
                          && b.getBookingSource() != BookingSource.MANUAL_BLOCK)
                .count();
        long checkoutTodayCount = bookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.CHECKED_IN
                          && today.equals(b.getCheckOut()))
                .count();
        // depositPendingCollect: chỉ đếm CHECKED_IN + DEPOSIT_30 + chưa thu 70%
        long depositPendingCollectCount = bookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.CHECKED_IN)
                .filter(b -> b.getPaymentOption() == PaymentOption.DEPOSIT_30)
                .filter(b -> b.getRemainingPaymentStatus() == null
                          || b.getRemainingPaymentStatus() != RemainingPaymentStatus.PAID_AT_PROPERTY)
                .count();

        // Stats accommodation
        long totalAccom = accommodations.size();
        long approvedAccom = accommodations.stream()
                .filter(a -> a.getApprovalStatus() == ApprovalStatus.APPROVED).count();
        long pendingAccom = accommodations.stream()
                .filter(a -> a.getApprovalStatus() == ApprovalStatus.PENDING).count();
        long rejectedAccom = accommodations.stream()
                .filter(a -> a.getApprovalStatus() == ApprovalStatus.REJECTED).count();

        // Tính doanh thu tháng này
        YearMonth thisMonth = YearMonth.now();
        BigDecimal monthlyRevenue = revenueService.getRevenueItemsForPartner(partner).stream()
                .filter(item -> item.getCreatedAt() != null
                        && YearMonth.from(item.getCreatedAt()).equals(thisMonth))
                .map(item -> item.getPartnerNetAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Đếm quyết toán đang chờ thanh toán
        long pendingSettlementCount = settlementService.getSettlementsForPartner(partner).stream()
                .filter(s -> s.getSettlementStatus().name().equals("PENDING")).count();
        BigDecimal pendingSettlementAmount = settlementService.getSettlementsForPartner(partner).stream()
                .filter(s -> s.getSettlementStatus().name().equals("PENDING"))
                .map(s -> s.getPayoutAmount() != null ? s.getPayoutAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Room status summary for dashboard widget
        List<RoomStatusDto> roomStatuses = availabilityService.buildRoomStatusForPartner(partner);
        int dashTotalRooms   = roomStatuses.stream().mapToInt(r -> r.platformQuantity).sum();
        int dashFreeRooms    = roomStatuses.stream().mapToInt(r -> r.freeToday).sum();
        int dashCheckedIn    = roomStatuses.stream().mapToInt(r -> r.checkedIn).sum();
        int dashHeldOnline   = roomStatuses.stream().mapToInt(r -> r.heldOnline).sum();
        int dashBlocked      = roomStatuses.stream().mapToInt(r -> r.blocked + r.directOccupied).sum();

        model.addAttribute("partnerName", partner.getName());
        addUnitLabels(model, partner);
        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("pendingConfirmCount", pendingConfirmCount);
        model.addAttribute("confirmedCount", confirmedCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("noShowCount", noShowCount);
        model.addAttribute("totalAccom", totalAccom);
        model.addAttribute("approvedAccom", approvedAccom);
        model.addAttribute("pendingAccom", pendingAccom);
        model.addAttribute("rejectedAccom", rejectedAccom);
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute("pendingSettlementCount", pendingSettlementCount);
        model.addAttribute("pendingSettlementAmount", pendingSettlementAmount);
        model.addAttribute("dashTotalRooms",  dashTotalRooms);
        model.addAttribute("dashFreeRooms",   dashFreeRooms);
        model.addAttribute("dashCheckedIn",   dashCheckedIn);
        model.addAttribute("dashHeldOnline",  dashHeldOnline);
        model.addAttribute("dashBlocked",     dashBlocked);

        // Booking source breakdown
        model.addAttribute("onlineBookingCount",  onlineBookingCount);
        model.addAttribute("directBookingCount",  directBookingCount);
        model.addAttribute("blockCount",          blockCount);

        // Việc cần xử lý hôm nay
        model.addAttribute("checkinTodayCount",          checkinTodayCount);
        model.addAttribute("checkoutTodayCount",         checkoutTodayCount);
        model.addAttribute("depositPendingCollectCount", depositPendingCollectCount);

        // Lấy 5 booking gần nhất để preview
        model.addAttribute("recentBookings", bookings.stream().limit(5).toList());

        return "partner/dashboard";
    }

    // ─── Accommodation Management ─────────────────────────────────────────────

    /**
     * GET /partner/accommodations — Danh sách accommodation của partner từ MySQL.
     */
    @GetMapping("/accommodations")
    public String accommodations(Model model,
                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        User partner = getCurrentPartner(userDetails);
        List<Accommodation> accommodations = accommodationService.getAccommodationsByOwner(partner);

        long approvedCount = accommodations.stream().filter(a -> a.getApprovalStatus() == ApprovalStatus.APPROVED).count();
        long pendingCount  = accommodations.stream().filter(a -> a.getApprovalStatus() == ApprovalStatus.PENDING).count();
        long rejectedCount = accommodations.stream().filter(a -> a.getApprovalStatus() == ApprovalStatus.REJECTED).count();

        // Tính room count theo từng accommodation
        Map<Long, Long> roomCountMap = new HashMap<>();
        Map<Long, Long> approvedRoomCountMap = new HashMap<>();
        Map<Long, Long> pendingRoomCountMap = new HashMap<>();
        for (Accommodation acc : accommodations) {
            if (acc.getId() != null) {
                long cnt = acc.getRooms() != null ? acc.getRooms().size() : 0L;
                long approvedRooms = acc.getRooms() != null
                        ? acc.getRooms().stream()
                                .filter(r -> r.getApprovalStatus() == ApprovalStatus.APPROVED)
                                .filter(r -> Boolean.TRUE.equals(r.getAvailableForBooking()))
                                .count()
                        : 0L;
                long pendingRooms = acc.getRooms() != null
                        ? acc.getRooms().stream()
                                .filter(r -> r.getApprovalStatus() == ApprovalStatus.PENDING)
                                .count()
                        : 0L;
                roomCountMap.put(acc.getId(), cnt);
                approvedRoomCountMap.put(acc.getId(), approvedRooms);
                pendingRoomCountMap.put(acc.getId(), pendingRooms);
            }
        }

        model.addAttribute("accommodations", accommodations);
        model.addAttribute("partnerName", partner.getName());
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("rejectedCount", rejectedCount);
        addUnitLabels(model, partner);
        model.addAttribute("roomCountMap", roomCountMap);
        model.addAttribute("approvedRoomCountMap", approvedRoomCountMap);
        model.addAttribute("pendingRoomCountMap", pendingRoomCountMap);

        return "partner/accommodations";
    }

    /**
     * GET /partner/accommodations/new — Form tạo mới accommodation.
     */
    @GetMapping("/accommodations/new")
    public String newAccommodationForm(Model model,
                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        User partner = getCurrentPartner(userDetails);
        model.addAttribute("partnerName", partner.getName());
        model.addAttribute("partnerPropertyType", partner.getPartnerPropertyType());
        addUnitLabels(model, partner);
        model.addAttribute("accommodationNamePlaceholder",
                getAccommodationNamePlaceholder(partner.getPartnerPropertyType()));
        return "partner/accommodation-form";
    }

    /**
     * POST /partner/accommodations/new — Xử lý submit form tạo mới accommodation.
     * approvalStatus = PENDING → chờ quản trị viên duyệt.
     */
    @PostMapping("/accommodations/new")
    public String createAccommodation(
            @RequestParam String name,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile thumbnailFile,
            @RequestParam(required = false, defaultValue = "HOTEL") String propertyType,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {

        User partner = getCurrentPartner(userDetails);
        try {
            // Xử lý file ảnh upload
            String thumbnailUrl = null;
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                try {
                    thumbnailUrl = fileStorageService.storeThumbnail(thumbnailFile);
                } catch (Exception e) {
                    // Không chặn submit nếu upload ảnh lỗi, chỉ log
                    ra.addFlashAttribute("warnMessage",
                        "⚠️ Không thể lưu ảnh (" + e.getMessage() + "). Nơi lưu trú được tạo không có ảnh.");
                }
            }

            PropertyType type = PropertyType.valueOf(propertyType.toUpperCase());
            accommodationService.createAccommodation(
                    partner, name, address, city, description, thumbnailUrl, type);
            ra.addFlashAttribute("successMessage",
                "✅ Đã gửi đăng ký nơi lưu trú thành công! Vui lòng chờ Admin duyệt.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", "❌ Loại lưu trú không hợp lệ!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/partner/accommodations";
    }

    /**
     * GET /partner/accommodations/{id}/rooms/new — Form tạo phòng mới cho 1 accommodation.
     * Accommodation phải APPROVED và thuộc partner.
     */
    @GetMapping("/accommodations/{id}/rooms/new")
    public String newRoomForm(@PathVariable Long id,
                              Model model,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        User partner = getCurrentPartner(userDetails);
        Accommodation acc = accommodationService.getById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ sở lưu trú!"));

        // Ownership check
        if (!accommodationService.canManageAccommodation(partner, acc)) {
            return "redirect:/partner/accommodations";
        }
        // Phải APPROVED mới cho thêm phòng
        if (acc.getApprovalStatus() != ApprovalStatus.APPROVED) {
            model.addAttribute("errorMessage",
                "Chỉ có thể thêm " + getUnitLabel(partner) + " cho cơ sở đã được Admin duyệt!");
            return "redirect:/partner/accommodations";
        }

        model.addAttribute("accommodation", acc);
        model.addAttribute("partnerName", partner.getName());
        addUnitLabels(model, partner);
        model.addAttribute("roomCategories", RoomCategory.values());
        // Truyền tất cả tiện nghi để render checkbox, nhóm theo category
        List<Amenity> allAmenities = accommodationService.getAllAmenities();
        model.addAttribute("allAmenities", allAmenities);
        // Nhóm theo category để render từng section
        java.util.Map<String, List<Amenity>> amenityGroups = allAmenities.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        a -> a.getCategory() != null ? a.getCategory() : "Khác",
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));
        model.addAttribute("amenityGroups", amenityGroups);
        return "partner/room-form";
    }

    /**
     * POST /partner/accommodations/{id}/rooms/new — Xử lý tạo phòng mới.
     */
    @PostMapping("/accommodations/{id}/rooms/new")
    public String createRoom(
            @PathVariable Long id,
            @RequestParam String roomCode,
            @RequestParam String roomName,
            @RequestParam(required = false) String bedType,
            @RequestParam Integer capacity,
            @RequestParam BigDecimal pricePerNight,
            @RequestParam Integer availableQuantity,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(name = "roomImages", required = false) MultipartFile[] roomImages,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "STANDARD") String roomCategory,
            @RequestParam(required = false) BigDecimal commissionRateOverride,
            @RequestParam(required = false) List<Long> amenityIds,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {

        User partner = getCurrentPartner(userDetails);
        try {
            List<String> uploadedImageUrls = storePartnerRoomImages(roomImages);
            String primaryImageUrl = !uploadedImageUrls.isEmpty()
                    ? uploadedImageUrls.get(0)
                    : (imageUrl != null ? imageUrl.trim() : "");

            RoomCategory category;
            try {
                category = RoomCategory.valueOf(roomCategory.toUpperCase());
            } catch (IllegalArgumentException e) {
                category = RoomCategory.STANDARD;
            }

            Room room = accommodationService.createRoom(partner, id, roomCode, roomName, bedType,
                    capacity, pricePerNight, availableQuantity, primaryImageUrl, description,
                    category, commissionRateOverride, amenityIds);

            if (!uploadedImageUrls.isEmpty()) {
                List<String> captions = uploadedImageUrls.stream()
                        .map(url -> getUnitLabelCap(partner) + " " + roomName + " do Partner upload")
                        .toList();
                roomImageService.replaceImagesByAdmin(room.getId(), uploadedImageUrls, captions, 0);
            }
            ra.addFlashAttribute("successMessage", "✅ Đã thêm " + getUnitLabel(partner) + " " + roomName + " thành công!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ Không thể lưu ảnh " + getUnitLabel(partner) + ": " + e.getMessage());
        }
        return "redirect:/partner/accommodations";
    }

    // ─── Rooms (legacy routes redirect về accommodations) ─────────────────────

    /** Legacy: /partner/rooms → redirect về accommodations */
    @GetMapping("/rooms")
    public String rooms(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        return "redirect:/partner/accommodations";
    }

    @GetMapping("/rooms/add")
    public String addRoom() {
        return "redirect:/partner/accommodations";
    }

    @GetMapping("/rooms/edit")
    public String editRoom() {
        return "redirect:/partner/accommodations";
    }

    /**
     * GET /partner/amenities — Trang quản lý tiện nghi phòng (chuyển từ add-furniture).
     * Partner chọn phòng và cập nhật tiện nghi.
     */
    @GetMapping({"/rooms/furniture", "/amenities"})
    public String amenitiesPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User partner = getCurrentPartner(userDetails);
        List<Room> myRooms = accommodationService.getApprovedRoomsForPartner(partner);
        List<Amenity> allAmenities = accommodationService.getAllAmenities();
        java.util.Map<String, List<Amenity>> amenityGroups = allAmenities.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        a -> a.getCategory() != null ? a.getCategory() : "Khác",
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));
        model.addAttribute("partnerName", partner.getName());
        addUnitLabels(model, partner);
        model.addAttribute("myRooms", myRooms);
        model.addAttribute("allAmenities", allAmenities);
        model.addAttribute("amenityGroups", amenityGroups);
        return "partner/amenities";
    }

    /**
     * POST /partner/amenities/{roomId} — Cập nhật tiện nghi cho 1 phòng cụ thể.
     */
    @PostMapping("/amenities/{roomId}")
    public String updateAmenities(@PathVariable Long roomId,
                                   @RequestParam(required = false) List<Long> amenityIds,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        try {
            Room updated = accommodationService.updateRoomAmenities(partner, roomId, amenityIds);
            if (updated.getApprovalStatus() != null && updated.getApprovalStatus().name().equals("PENDING")) {
                ra.addFlashAttribute("successMessage",
                        "✅ Đã ghi nhận thay đổi tiện nghi quan trọng cho " + getUnitLabel(partner) + " "
                                + updated.getRoomName() + ". TravelMate đã chuyển sang trạng thái chờ Admin xem xét lại trước khi hiển thị cho khách.");
            } else {
                ra.addFlashAttribute("successMessage",
                        "✅ Đã cập nhật tiện nghi cho " + getUnitLabel(partner) + " " + updated.getRoomName() + " thành công!");
            }
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/partner/amenities";
    }

    @GetMapping("/rooms/pending")
    public String pendingRooms() {
        return "redirect:/partner/accommodations";
    }

    @PostMapping("/rooms/{roomId}/toggle-selling")
    public String toggleRoomSelling(@PathVariable Long roomId,
                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                    RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        try {
            Room room = accommodationService.toggleRoomBookingAvailability(partner, roomId);
            boolean open = Boolean.TRUE.equals(room.getAvailableForBooking());
            logPartnerAction(partner,
                    open ? "PARTNER_REOPEN_ROOM_SELLING" : "PARTNER_STOP_ROOM_SELLING",
                    "ROOM", room.getId(),
                    (open ? "Partner mở bán lại " : "Partner tạm ngừng bán ")
                            + getUnitLabel(partner) + ": " + room.getRoomName(),
                    null);
            ra.addFlashAttribute("successMessage",
                    (open ? "✅ Đã mở bán lại " : "⏸️ Đã tạm ngừng bán ")
                            + getUnitLabel(partner) + " " + room.getRoomName()
                            + ". Quota gốc không bị thay đổi.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/partner/room-status";
    }

    // ─── Bookings ─────────────────────────────────────────────────────────────

    /**
     * GET /partner/bookings — Danh sách booking từ MySQL.
     *
     * Hiển thị tất cả booking: ONLINE (đã admin duyệt), DIRECT và MANUAL_BLOCK.
     * Ownership: chỉ booking thuộc accommodation.owner = currentPartner.
     */
    @GetMapping("/bookings")
    public String bookings(Model model,
                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        User partner = getCurrentPartner(userDetails);

        // Lấy TẤT CẢ booking của partner (kể cả DIRECT, BLOCK)
        List<Booking> allBookings = bookingService.getAllBookingsForPartner(partner);

        // Booking online VNPAY hợp lệ; loại trừ giao dịch ngoại lệ đang cần đối soát.

        // Booking trực tiếp
        List<Booking> directBookings = allBookings.stream()
                .filter(b -> b.getBookingSource() == BookingSource.DIRECT)
                .filter(b -> b.getBookingStatus() != BookingStatus.CANCELLED)
                .collect(Collectors.toList());

        // Chặn phòng
        List<Booking> blockBookings = allBookings.stream()
                .filter(b -> b.getBookingSource() == BookingSource.MANUAL_BLOCK)
                .filter(b -> b.getBookingStatus() != BookingStatus.CANCELLED)
                .collect(Collectors.toList());

        // Gộp tất cả để hiển thị (online + direct + block), đã có đủ
        List<Booking> displayBookings = allBookings.stream()
                .filter(b -> {
                    // Online: chỉ hiển thị sau khi TravelMate/Admin đã ghi nhận thanh toán
                    if (b.getBookingSource() == null || b.getBookingSource() == BookingSource.ONLINE) {
                        return b.getBookingStatus() == BookingStatus.CONFIRMED
                                || b.getBookingStatus() == BookingStatus.CHECKED_IN
                                || b.getBookingStatus() == BookingStatus.COMPLETED
                                || b.getBookingStatus() == BookingStatus.NO_SHOW
                                || b.getBookingStatus() == BookingStatus.CANCELLED;
                    }
                    // Direct/Block: hiển thị tất cả (kể cả đã hủy để partner xem lịch sử)
                    return true;
                })
                .collect(Collectors.toList());

        // Thống kê
        addUnitLabels(model, partner);
        model.addAttribute("bookings", displayBookings);
        model.addAttribute("totalCount", (long) displayBookings.size());
        model.addAttribute("confirmedCount",
                displayBookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED
                        && b.getPartnerStatus() == PartnerBookingStatus.PARTNER_CONFIRMED
                        && (b.getBookingSource() == null || b.getBookingSource() == BookingSource.ONLINE)).count());
        model.addAttribute("completedCount",
                displayBookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED).count());
        model.addAttribute("checkedInCount",
                displayBookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.CHECKED_IN).count());
        model.addAttribute("pendingPartnerCount",
                displayBookings.stream()
                        .filter(b -> b.getPartnerStatus() == PartnerBookingStatus.PENDING_PARTNER_CONFIRMATION)
                        .count());
        model.addAttribute("directCount", (long) directBookings.size());
        model.addAttribute("blockCount", (long) blockBookings.size());
        model.addAttribute("partnerName", partner.getName());

        return "partner/bookings";
    }

    /**
     * POST /partner/bookings/{id}/confirm-hold — dự phòng cho booking legacy cần kiểm tra giữ phòng.
     */
    @PostMapping("/bookings/{id}/confirm-hold")
    public String confirmHold(@PathVariable Long id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        try {
            Booking b = bookingService.confirmBookingHoldByPartner(id, partner);
            logPartnerAction(partner, "PARTNER_CONFIRM_HOLD", "BOOKING", id,
                    "Đối tác giữ " + getUnitLabel(partner) + ": " + b.getBookingCode(), null);
            ra.addFlashAttribute("successMessage",
                    "✅ Đã ghi nhận trạng thái giữ " + getUnitLabel(partner) + ". TravelMate đã giữ trên hệ thống.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/partner/bookings";
    }

    /**
     * POST /partner/bookings/{id}/check-in — Partner thực hiện check-in khi khách đến.
     */
    @PostMapping("/bookings/{id}/check-in")
    public String checkIn(@PathVariable Long id,
                          @AuthenticationPrincipal CustomUserDetails userDetails,
                          RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        try {
            Booking b = bookingService.checkInByPartner(id, partner);
            logPartnerAction(partner, "PARTNER_CHECK_IN", "BOOKING", id,
                    "Partner check-in khách: " + b.getBookingCode()
                    + " — " + b.getCustomerName(), null);
            ra.addFlashAttribute("successMessage",
                    "✅ Đã check-in thành công cho khách " + b.getCustomerName() + "!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/partner/bookings";
    }

    /**
     * POST /partner/bookings/{id}/check-out — Partner check-out / hoàn tất khi khách trả phòng.
     */
    @PostMapping("/bookings/{id}/check-out")
    public String checkOut(@PathVariable Long id,
                           @AuthenticationPrincipal CustomUserDetails userDetails,
                           RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        try {
            Booking b = bookingService.markCompletedByPartner(id, partner);
            logPartnerAction(partner, "PARTNER_CHECK_OUT", "BOOKING", id,
                    "Partner check-out / hoàn tất: " + b.getBookingCode()
                    + " — " + b.getCustomerName(), null);
            ra.addFlashAttribute("successMessage",
                    "✅ Check-out thành công! Đơn " + b.getBookingCode() + " đã hoàn tất.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/partner/bookings";
    }

    /**
     * POST /partner/bookings/{id}/report-no-show — Partner báo khách không đến.
     */
    @PostMapping("/bookings/{id}/report-no-show")
    public String reportNoShow(@PathVariable Long id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        try {
            Booking b = bookingService.markNoShowByPartner(id, partner);
            logPartnerAction(partner, "PARTNER_REPORT_NO_SHOW", "BOOKING", id,
                    "Partner báo no-show: " + b.getBookingCode()
                    + " — " + b.getCustomerName(), null);
            boolean isDeposit = b.getPaymentOption() != null
                    && b.getPaymentOption() == com.travelmate.entity.enums.PaymentOption.DEPOSIT_30;
            String msg = isDeposit
                    ? "✅ Đã ghi nhận khách không đến (No-show). Cọc 30% bị giữ lại theo chính sách và quota " + getUnitLabel(partner) + " đã được mở lại trên TravelMate."
                    : "✅ Đã ghi nhận khách không đến (No-show). Booking đã được cập nhật và quota " + getUnitLabel(partner) + " đã được mở lại trên TravelMate.";
            ra.addFlashAttribute("successMessage", msg);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/partner/bookings";
    }

    /**
     * GET /partner/bookings/{id} — Chi tiết booking.
     */
    @GetMapping("/bookings/{id}")
    public String bookingDetail(@PathVariable Long id,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model) {
        User partner = getCurrentPartner(userDetails);
        Booking booking;
        try {
            booking = bookingService.getVisibleBookingForPartner(id, partner);
        } catch (RuntimeException ex) {
            return "redirect:/partner/bookings";
        }
        model.addAttribute("booking", booking);
        model.addAttribute("partnerName", partner.getName());
        addUnitLabels(model, partner);

        if (booking.getBookingStatus() == BookingStatus.NO_SHOW) {
            BigDecimal forfeited = booking.getPaidAmount() != null ? booking.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal commissionBase = resolveNoShowCommissionBase(booking, forfeited);
            BigDecimal rateDecimal = resolveNoShowCommissionRate(booking);
            BigDecimal commission = resolveNoShowCommission(booking, commissionBase, rateDecimal);
            BigDecimal partnerVoucher = resolveNoShowPartnerVoucher(booking);
            BigDecimal partnerPayout = forfeited.subtract(commission).subtract(partnerVoucher);
            BigDecimal ratePercent = rateDecimal.multiply(BigDecimal.valueOf(100))
                    .setScale(1, java.math.RoundingMode.HALF_UP);
            boolean isRoomOverride = "ROOM_OVERRIDE".equals(booking.getCommissionSourceSnapshot())
                    || commissionService.isRoomOverride(booking.getRoom());
            model.addAttribute("noShowForfeited", forfeited);
            model.addAttribute("noShowCommissionBase", commissionBase);
            model.addAttribute("noShowCommission", commission);
            model.addAttribute("noShowPartnerVoucher", partnerVoucher);
            model.addAttribute("noShowPartnerPayout", partnerPayout);
            model.addAttribute("noShowCommissionRate", ratePercent);
            model.addAttribute("noShowIsRoomOverride", isRoomOverride);
        }

        return "partner/booking-detail";
    }

    private BigDecimal resolveNoShowCommissionBase(Booking booking, BigDecimal fallback) {
        BigDecimal snapshot = positiveOrNull(booking.getCommissionBaseAmount());
        if (snapshot != null) return snapshot;
        BigDecimal beforeDiscount = positiveOrNull(booking.getTotalBeforeDiscount());
        if (beforeDiscount != null) return beforeDiscount;
        BigDecimal total = positiveOrNull(booking.getTotalAmount());
        return total != null ? total : fallback;
    }

    private BigDecimal resolveNoShowCommissionRate(Booking booking) {
        return booking.getCommissionRateSnapshot() != null
                ? booking.getCommissionRateSnapshot()
                : commissionService.getEffectiveCommissionRate(booking.getRoom());
    }

    private BigDecimal resolveNoShowCommission(Booking booking,
                                               BigDecimal commissionBase,
                                               BigDecimal rateDecimal) {
        BigDecimal snapshot = positiveOrNull(booking.getCommissionAmountSnapshot());
        if (snapshot != null) return snapshot;
        return commissionBase.multiply(rateDecimal).setScale(0, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal resolveNoShowPartnerVoucher(Booking booking) {
        if (booking.getPartnerVoucherAmountSnapshot() != null) {
            return booking.getPartnerVoucherAmountSnapshot();
        }
        return booking.getVoucherCostBearer() == VoucherCostBearer.PARTNER
                && booking.getDiscountAmount() != null
                ? booking.getDiscountAmount() : BigDecimal.ZERO;
    }

    private BigDecimal positiveOrNull(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0 ? value : null;
    }

    /**
     * POST /partner/bookings/{id}/mark-completed — Legacy endpoint, redirect sang check-out.
     * Giữ lại để tương thích với link cũ trong HTML.
     */
    @PostMapping("/bookings/{id}/mark-completed")
    public String markCompleted(@PathVariable Long id,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        try {
            Booking b = bookingService.markCompletedByPartner(id, partner);
            logPartnerAction(partner, "PARTNER_CHECK_OUT", "BOOKING", id,
                    "Partner check-out / hoàn tất: " + b.getBookingCode()
                    + " — " + b.getCustomerName(), null);
            ra.addFlashAttribute("successMessage",
                    "✅ Check-out thành công! Đơn " + b.getBookingCode() + " đã hoàn tất.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/partner/bookings";
    }

    /**
     * POST /partner/bookings/{id}/cancel-hold — Partner hủy phía partner.
     * Chỉ dùng khi booking đang CONFIRMED. Admin sẽ xem xét.
     */
    @PostMapping("/bookings/{id}/cancel-hold")
    public String cancelHold(@PathVariable Long id,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        try {
            Booking b = bookingService.cancelByPartner(id, partner);
            logPartnerAction(partner, "PARTNER_REJECT_HOLD", "BOOKING", id,
                    "Partner hủy giữ " + getUnitLabel(partner) + ": " + b.getBookingCode(), null);
            ra.addFlashAttribute("successMessage", "⚠️ Đã hủy xác nhận. Admin sẽ xem xét đơn này.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/partner/bookings";
    }

    /**
     * POST /partner/bookings/{id}/add-note — Partner ghi chú cho booking.
     */
    @PostMapping("/bookings/{id}/add-note")
    public String addNote(@PathVariable Long id,
                           @RequestParam String note,
                           @AuthenticationPrincipal CustomUserDetails userDetails,
                           RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        try {
            bookingService.addPartnerNote(id, partner, note);
            ra.addFlashAttribute("successMessage", "✅ Đã thêm ghi chú thành công!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/partner/bookings";
    }

    // ─── Revenue ────────────────────────────────────────────────────────────

    @GetMapping("/revenue")
    public String revenue(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User partner = getCurrentPartner(userDetails);
        PartnerRevenueSummaryDto summary = revenueService.calculatePartnerRevenueSummary(partner);
        List<RevenueItemDto> items       = revenueService.getRevenueItemsForPartner(partner);
        addUnitLabels(model, partner);
        model.addAttribute("summary",      summary);
        model.addAttribute("revenueItems", items);
        model.addAttribute("partnerName",  partner.getName());
        return "partner/revenue";
    }

    // ─── Vouchers ────────────────────────────────────────────────────────────

    @GetMapping("/vouchers")
    public String vouchers(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User partner = getCurrentPartner(userDetails);
        List<Room> myRooms = accommodationService.getApprovedRoomsForPartner(partner);
        List<Voucher> catalog = voucherService.getPartnerRoomCatalog(partner);
        List<RoomVoucherAssignment> assignments = voucherService.getAssignmentsForPartner(partner);
        addUnitLabels(model, partner);
        model.addAttribute("catalog", catalog);
        model.addAttribute("assignments", assignments);
        model.addAttribute("rooms", myRooms);
        model.addAttribute("partnerName", partner.getName());
        return "partner/vouchers";
    }

    /** Partner chỉ chọn voucher do Admin cấp và gắn vào phòng/căn được duyệt của mình. */
    @PostMapping("/vouchers/assign")
    public String assignVoucher(
            @RequestParam Long voucherId,
            @RequestParam Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        try {
            RoomVoucherAssignment assignment = voucherService.assignVoucherToRoom(partner, voucherId, roomId);
            logPartnerAction(partner, "ASSIGN_VOUCHER", "ROOM", roomId,
                    "Gắn voucher " + assignment.getVoucher().getCode() + " vào " + assignment.getRoom().getRoomName(), null);
            ra.addFlashAttribute("successMessage", "Đã gắn voucher vào " + getUnitLabel(partner) + " thành công.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/partner/vouchers";
    }

    @PostMapping("/vouchers/assignments/{id}/remove")
    public String removeVoucherAssignment(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        try {
            voucherService.removeAssignment(partner, id);
            logPartnerAction(partner, "REMOVE_VOUCHER_ASSIGNMENT", "VOUCHER_ASSIGNMENT", id,
                    "Ngừng áp dụng voucher trên phòng/căn", null);
            ra.addFlashAttribute("successMessage", "Đã ngừng áp dụng voucher cho " + getUnitLabel(partner) + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/partner/vouchers";
    }

    // ─── Settlements ────────────────────────────────────────────────────────────

    @GetMapping("/settlements")
    public String settlements(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User partner = getCurrentPartner(userDetails);
        List<PartnerSettlement> settlements = settlementService.getSettlementsForPartner(partner);
        long pendingCount = settlements.stream().filter(s -> s.getSettlementStatus().name().equals("PENDING")).count();
        long paidCount    = settlements.stream().filter(s -> s.getSettlementStatus().name().equals("PAID")).count();
        BigDecimal totalPending = settlements.stream()
                .filter(s -> s.getSettlementStatus().name().equals("PENDING"))
                .map(s -> s.getPayoutAmount() != null ? s.getPayoutAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("settlements",    settlements);
        model.addAttribute("pendingCount",   pendingCount);
        model.addAttribute("paidCount",      paidCount);
        model.addAttribute("totalPending",   totalPending);
        model.addAttribute("partnerName",    partner.getName());
        return "partner/settlements";
    }

    /**
     * GET /partner/settlements/{id} — Chi tiết 1 kỳ quyết toán.
     *
     * Sử dụng chung getBreakdownForSettlement() với Admin → đồng bộ dữ liệu tài chính.
     */
    @GetMapping("/settlements/{id}")
    public String settlementDetail(@PathVariable Long id,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {
        User partner = getCurrentPartner(userDetails);
        List<PartnerSettlement> settlements = settlementService.getSettlementsForPartner(partner);
        Optional<PartnerSettlement> settlementOpt = settlements.stream()
                .filter(s -> s.getId().equals(id)).findFirst();
        if (settlementOpt.isEmpty()) return "redirect:/partner/settlements";
        PartnerSettlement settlement = settlementOpt.get();

        // Dùng chung logic với Admin — đồng bộ dữ liệu tài chính
        var breakdown = settlementService.getBreakdownForSettlement(settlement);
        var detailTotals = settlementService.calculateBreakdownTotals(breakdown);

        model.addAttribute("settlement",     settlement);
        model.addAttribute("breakdown",      breakdown);
        model.addAttribute("detailTotals",   detailTotals);
        model.addAttribute("partnerName",    partner.getName());
        return "partner/settlement-detail";
    }

    // ─── Partner Wallet ──────────────────────────────────────────────────────

    @GetMapping("/wallet")
    public String wallet(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User partner = getCurrentPartner(userDetails);
        PartnerWallet wallet = partnerWalletService.ensureWalletWithPaidSettlements(partner);
        List<PartnerWalletTransaction> transactions = partnerWalletService.getTransactions(partner);
        List<PartnerWithdrawalRequest> withdrawals = partnerWalletService.getWithdrawalsForPartner(partner);

        boolean hasBankInfo = hasPartnerBankInfo(partner);
        addUnitLabels(model, partner);
        model.addAttribute("partner", partner);
        model.addAttribute("wallet", wallet);
        model.addAttribute("transactions", transactions);
        model.addAttribute("withdrawals", withdrawals);
        model.addAttribute("hasBankInfo", hasBankInfo);
        model.addAttribute("maskedBankAccountNumber", maskBankAccountNumber(partner.getBankAccountNumber()));
        model.addAttribute("partnerName", partner.getName());
        return "partner/wallet";
    }

    @PostMapping("/wallet/bank")
    public String updateWalletBankInfo(@RequestParam(required = false) String bankAccountNumber,
                                       @RequestParam(required = false) String bankName,
                                       @RequestParam(required = false) String bankAccountHolder,
                                       @RequestParam(required = false) String bankBranch,
                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                       RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        String cleanedBankName = trimToNull(bankName);
        String cleanedAccountNumber = trimToNull(bankAccountNumber);
        String cleanedAccountHolder = trimToNull(bankAccountHolder);
        String cleanedBankBranch = trimToNull(bankBranch);

        if (!hasText(cleanedBankName) || !hasText(cleanedAccountNumber) || !hasText(cleanedAccountHolder)) {
            ra.addFlashAttribute("errorMessage",
                    "❌ Vui lòng nhập đầy đủ ngân hàng, số tài khoản và chủ tài khoản.");
            return "redirect:/partner/wallet";
        }
        if (!cleanedAccountNumber.matches(BANK_ACCOUNT_PATTERN)) {
            ra.addFlashAttribute("errorMessage",
                    "❌ Số tài khoản chỉ được gồm chữ số, từ 6 đến 30 ký tự.");
            return "redirect:/partner/wallet";
        }

        partner.setBankAccountNumber(cleanedAccountNumber);
        partner.setBankName(cleanedBankName);
        partner.setBankAccountHolder(cleanedAccountHolder);
        partner.setBankBranch(cleanedBankBranch);
        userRepository.save(partner);
        ra.addFlashAttribute("successMessage", "✅ Đã cập nhật tài khoản nhận tiền cho ví quyết toán!");
        return "redirect:/partner/wallet";
    }

    @PostMapping("/wallet/withdrawals")
    public String requestWithdrawal(@RequestParam BigDecimal amount,
                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                    RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        try {
            PartnerWithdrawalRequest request = partnerWalletService.requestWithdrawal(partner, amount);
            logPartnerAction(partner, "PARTNER_REQUEST_WITHDRAWAL", "WITHDRAWAL", request.getId(),
                    "Partner gửi yêu cầu rút tiền " + request.getRequestCode(), null);
            ra.addFlashAttribute("successMessage",
                    "✅ Đã gửi yêu cầu rút tiền " + request.getRequestCode() + ". Admin sẽ xử lý chuyển khoản ngoài hệ thống.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/partner/wallet";
    }

    private boolean hasPartnerBankInfo(User partner) {
        return hasText(partner.getBankName())
                && hasText(partner.getBankAccountNumber())
                && partner.getBankAccountNumber().trim().matches(BANK_ACCOUNT_PATTERN)
                && hasText(partner.getBankAccountHolder());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String maskBankAccountNumber(String accountNumber) {
        if (!hasText(accountNumber)) {
            return "—";
        }
        String cleaned = accountNumber.trim();
        if (cleaned.length() <= 4) {
            return cleaned;
        }
        return "****" + cleaned.substring(cleaned.length() - 4);
    }

    /**
     * GET /partner/profile — Hồ sơ thanh toán của partner.
     */
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User partner = getCurrentPartner(userDetails);
        model.addAttribute("partner", partner);
        model.addAttribute("partnerName", partner.getName());
        return "partner/profile";
    }

    /**
     * POST /partner/profile — Cập nhật thông tin hồ sơ.
     */
    @PostMapping("/profile")
    public String updateProfile(@RequestParam(required = false) String phone,
                                @RequestParam(required = false) String bankAccountNumber,
                                @RequestParam(required = false) String bankName,
                                @RequestParam(required = false) String bankAccountHolder,
                                @RequestParam(required = false) String bankBranch,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        if (phone != null && !phone.isBlank()) partner.setPhone(phone.trim());
        if (bankAccountNumber != null) partner.setBankAccountNumber(bankAccountNumber.trim());
        if (bankName != null) partner.setBankName(bankName.trim());
        if (bankAccountHolder != null) partner.setBankAccountHolder(bankAccountHolder.trim());
        if (bankBranch != null) partner.setBankBranch(bankBranch.trim());
        userRepository.save(partner);
        ra.addFlashAttribute("successMessage", "✅ Đã cập nhật thông tin hồ sơ!");
        return "redirect:/partner/profile";
    }

    // ─── Reviews ─────────────────────────────────────────────────────────────

    /**
     * GET /partner/reviews — Partner xem tất cả đánh giá của cơ sở mình.
     * Partner chỉ được xem, không được xóa (admin mới xóa được).
     */
    @GetMapping("/reviews")
    public String reviews(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User partner = getCurrentPartner(userDetails);
        List<Review> reviews = reviewService.getReviewsForPartner(partner);

        double avgRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        double avgRatingRounded = Math.round(avgRating * 10.0) / 10.0;

        model.addAttribute("reviews",     reviews);
        model.addAttribute("totalCount",  (long) reviews.size());
        model.addAttribute("avgRating",   avgRatingRounded);
        model.addAttribute("partnerName", partner.getName());
        return "partner/reviews";
    }

    @GetMapping("/support")
    public String support(@AuthenticationPrincipal CustomUserDetails userDetails,
                          @RequestParam(required = false) String action,
                          @RequestParam(required = false) Long accommodationId,
                          Model model) {
        User partner = getCurrentPartner(userDetails);
        List<SupportTicket> tickets = supportTicketService.getTicketsForPartner(partner);
        long openCount      = tickets.stream().filter(t -> "OPEN".equals(t.getStatus())).count();
        long respondedCount = tickets.stream().filter(t -> "RESPONDED".equals(t.getStatus())).count();
        long closedCount    = tickets.stream().filter(t -> "CLOSED".equals(t.getStatus())).count();

        String supportCategory = "";
        String supportSubject = "";
        String supportPriority = "Trung bình";
        String supportDescription = "";
        if ("listing-change".equals(action) && accommodationId != null) {
            Optional<Accommodation> accOpt = accommodationService.getById(accommodationId);
            if (accOpt.isPresent()
                    && accommodationService.canManageAccommodation(partner, accOpt.get())) {
                Accommodation acc = accOpt.get();
                String unitLabel = getUnitLabel(partner);
                supportCategory = "Kỹ thuật";
                supportSubject = "Yêu cầu chỉnh sửa/ngừng bán: " + acc.getName();
                supportDescription =
                        "Nơi lưu trú: " + acc.getName() + " (#" + acc.getId() + ")\n" +
                        "Loại lưu trú: " + (acc.getPropertyType() != null ? acc.getPropertyType().name() : "N/A") + "\n\n" +
                        "Nội dung cần Admin hỗ trợ:\n" +
                        "- Chỉnh sửa thông tin đã duyệt / hoặc ngừng bán dài hạn.\n" +
                        "- Lý do và thời gian áp dụng:\n\n" +
                        "Lưu ý: TravelMate không xóa cứng nơi lưu trú/" + unitLabel +
                        " đã có lịch sử booking để giữ đúng dữ liệu vận hành và quyết toán.";
            }
        }

        model.addAttribute("tickets",        tickets);
        model.addAttribute("openCount",      openCount);
        model.addAttribute("respondedCount", respondedCount);
        model.addAttribute("closedCount",    closedCount);
        model.addAttribute("partnerName",    partner.getName());
        addUnitLabels(model, partner);
        model.addAttribute("supportCategory", supportCategory);
        model.addAttribute("supportSubject", supportSubject);
        model.addAttribute("supportPriority", supportPriority);
        model.addAttribute("supportDescription", supportDescription);
        return "partner/support";
    }

    /** POST /partner/support/new — Partner gửi ticket hỗ trợ mới */
    @PostMapping("/support/new")
    public String createTicket(@AuthenticationPrincipal CustomUserDetails userDetails,
                               @RequestParam String category,
                               @RequestParam String subject,
                               @RequestParam(required = false) String priority,
                               @RequestParam(required = false) String description,
                               RedirectAttributes ra) {
        try {
            User partner = getCurrentPartner(userDetails);
            supportTicketService.createTicket(partner, category, subject, priority, description);
            ra.addFlashAttribute("successMessage", "✅ Đã gửi yêu cầu hỗ trợ thành công! Admin sẽ phản hồi sớm.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/partner/support";
    }

    // ─── Availability ─────────────────────────────────────────────────────────

    @GetMapping("/availability")
    public String availability(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String checkIn,
            @RequestParam(required = false) String checkOut,
            Model model) {

        User partner = getCurrentPartner(userDetails);
        String partnerName = partner.getName() != null ? partner.getName() : partner.getEmail();

        java.time.LocalDate ciDate = null;
        java.time.LocalDate coDate = null;
        List<RoomAvailabilityDto> results = null;
        AvailabilityService.AvailabilitySummary summary = null;
        String errorMsg = null;

        if (checkIn != null && !checkIn.isBlank() && checkOut != null && !checkOut.isBlank()) {
            try {
                ciDate = java.time.LocalDate.parse(checkIn);
                coDate = java.time.LocalDate.parse(checkOut);
                if (!coDate.isAfter(ciDate)) {
                    errorMsg = "Ngày trả " + getUnitLabel(partner) + " phải sau ngày nhận " + getUnitLabel(partner) + "!";
                } else {
                    results = availabilityService.checkAvailabilityForPartner(partner, ciDate, coDate);
                    summary = availabilityService.buildSummary(results);
                }
            } catch (Exception e) {
                errorMsg = "Ngày không hợp lệ. Vui lòng chọn lại!";
            }
        }

        addUnitLabels(model, partner);
        model.addAttribute("partnerName", partnerName);
        model.addAttribute("checkIn",     checkIn);
        model.addAttribute("checkOut",    checkOut);
        model.addAttribute("results",     results);
        model.addAttribute("summary",     summary);
        model.addAttribute("errorMsg",    errorMsg);
        return "partner/availability";
    }

    // ─── Confirm remaining payment (DEPOSIT_30) ───────────────────────────────

    /**
     * POST /partner/bookings/{id}/confirm-remaining — Xác nhận đã thu 70% tại cơ sở.
     */
    @PostMapping("/bookings/{id}/confirm-remaining")
    public String confirmRemainingPayment(@PathVariable Long id,
                                          @RequestParam(required = false) String paymentNote,
                                          @AuthenticationPrincipal CustomUserDetails userDetails,
                                          RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        try {
            Booking b = bookingService.confirmRemainingPaymentByPartner(id, partner, paymentNote);
            logPartnerAction(partner, "PARTNER_CONFIRM_REMAINING", "BOOKING", id,
                    "Đối tác xác nhận thu 70% tại cơ sở: " + b.getBookingCode(), paymentNote);
            ra.addFlashAttribute("successMessage",
                    "✅ Đã xác nhận thu đủ khoản còn lại cho đơn " + b.getBookingCode() + "!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/partner/bookings";
    }

    /**
     * POST /partner/bookings/{id}/check-in-with-confirm — Check-in kèm xác nhận thu 70%.
     */
    @PostMapping("/bookings/{id}/check-in-with-confirm")
    public String checkInWithConfirm(@PathVariable Long id,
                                      @RequestParam(required = false, defaultValue = "false") boolean remainingConfirmed,
                                      @RequestParam(required = false) String remainingNote,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        try {
            Booking b = bookingService.checkInWithRemainingConfirmByPartner(id, partner, remainingConfirmed, remainingNote);
            logPartnerAction(partner, "PARTNER_CHECK_IN", "BOOKING", id,
                    "Partner check-in" + (remainingConfirmed ? " + xác nhận thu 70%" : "") + ": " + b.getBookingCode(), null);
            String msg = "✅ Đã check-in thành công cho khách " + b.getCustomerName() + "!";
            if (remainingConfirmed) msg += " Đã ghi nhận thu đủ khoản còn lại tại cơ sở.";
            ra.addFlashAttribute("successMessage", msg);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/partner/bookings";
    }

    // ─── Room Status — Tình trạng phòng hôm nay ──────────────────────────────

    /**
     * GET /partner/room-status — Tình trạng vận hành phòng hôm nay.
     */
    @GetMapping("/room-status")
    public String roomStatus(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User partner = getCurrentPartner(userDetails);
        List<RoomStatusDto> roomStatuses = availabilityService.buildRoomStatusForPartner(partner);

        int totalRooms   = roomStatuses.stream().mapToInt(r -> r.platformQuantity).sum();
        int freeRooms    = roomStatuses.stream().mapToInt(r -> r.freeToday).sum();
        int checkedInNow = roomStatuses.stream().mapToInt(r -> r.checkedIn).sum();
        int heldOnline   = roomStatuses.stream().mapToInt(r -> r.heldOnline).sum();
        int blocked      = roomStatuses.stream().mapToInt(r -> r.blocked + r.directOccupied).sum();

        // Lấy danh sách phòng/cơ sở cho form direct booking và block
        List<Accommodation> myAccommodations = accommodationService.getAccommodationsByOwner(partner)
                .stream().filter(a -> a.getApprovalStatus() == ApprovalStatus.APPROVED)
                .collect(Collectors.toList());
        List<Room> myRooms = accommodationService.getApprovedRoomsForPartner(partner);

        addUnitLabels(model, partner);
        model.addAttribute("roomStatuses",      roomStatuses);
        model.addAttribute("totalRooms",        totalRooms);
        model.addAttribute("freeRooms",         freeRooms);
        model.addAttribute("checkedInNow",      checkedInNow);
        model.addAttribute("heldOnline",        heldOnline);
        model.addAttribute("blocked",           blocked);
        model.addAttribute("myAccommodations",  myAccommodations);
        model.addAttribute("myRooms",           myRooms);
        model.addAttribute("partnerName",       partner.getName() != null ? partner.getName() : partner.getEmail());
        model.addAttribute("today",             java.time.LocalDate.now());
        return "partner/room-status";
    }

    // ─── Direct Booking ───────────────────────────────────────────────────────

    /**
     * GET /partner/direct-booking — Form tạo booking trực tiếp / chặn phòng.
     */
    @GetMapping("/direct-booking")
    public String directBookingForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User partner = getCurrentPartner(userDetails);
        List<Accommodation> myAccommodations = accommodationService.getAccommodationsByOwner(partner)
                .stream().filter(a -> a.getApprovalStatus() == ApprovalStatus.APPROVED)
                .collect(Collectors.toList());
        List<Room> myRooms = accommodationService.getApprovedRoomsForPartner(partner);
        addUnitLabels(model, partner);
        model.addAttribute("myAccommodations", myAccommodations);
        model.addAttribute("myRooms",          myRooms);
        model.addAttribute("partnerName",      partner.getName());
        model.addAttribute("today",            java.time.LocalDate.now().toString());
        return "partner/direct-booking";
    }

    /**
     * POST /partner/direct-booking — Tạo booking trực tiếp.
     */
    @PostMapping("/direct-booking")
    public String createDirectBooking(
            @RequestParam Long accommodationId,
            @RequestParam Long roomId,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            @RequestParam(defaultValue = "1") Integer adults,
            @RequestParam(defaultValue = "0") Integer children,
            @RequestParam(defaultValue = "1") Integer roomQuantity,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String customerPhone,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        try {
            Accommodation accommodation = accommodationService.getById(accommodationId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ sở lưu trú!"));
            Room room = accommodationService.getRoomById(roomId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng/căn!"));
            LocalDate ciDate = LocalDate.parse(checkIn);
            LocalDate coDate = LocalDate.parse(checkOut);

            Booking b = bookingService.createDirectBooking(partner, room, accommodation,
                    customerName, customerPhone, customerEmail,
                    ciDate, coDate, adults, children, roomQuantity, note);

            logPartnerAction(partner, "PARTNER_CREATE_DIRECT_BOOKING", "BOOKING", b.getId(),
                    "Partner tạo booking trực tiếp: " + b.getBookingCode(), note);
            ra.addFlashAttribute("successMessage",
                    "✅ Đã tạo booking trực tiếp " + b.getBookingCode() + " thành công! Lịch " + getUnitLabel(partner) + " đã được cập nhật.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/partner/room-status";
    }

    /**
     * POST /partner/block-room — Chặn phòng/căn.
     */
    @PostMapping("/block-room")
    public String blockRoom(
            @RequestParam Long accommodationId,
            @RequestParam Long roomId,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            @RequestParam(defaultValue = "1") Integer roomQuantity,
            @RequestParam(required = false) String blockReason,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        try {
            Accommodation accommodation = accommodationService.getById(accommodationId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ sở lưu trú!"));
            Room room = accommodationService.getRoomById(roomId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng/căn!"));
            LocalDate ciDate = LocalDate.parse(checkIn);
            LocalDate coDate = LocalDate.parse(checkOut);

            Booking b = bookingService.blockRoom(partner, room, accommodation,
                    ciDate, coDate, roomQuantity, blockReason);

            logPartnerAction(partner, "PARTNER_BLOCK_ROOM", "BOOKING", b.getId(),
                    "Partner chặn " + getUnitLabel(partner) + ": " + b.getBookingCode()
                    + " — " + room.getRoomName() + " (" + roomQuantity + " " + getUnitLabel(partner) + ")", blockReason);
            ra.addFlashAttribute("successMessage",
                    "✅ Đã chặn " + roomQuantity + " " + getUnitLabel(partner) + " " + room.getRoomName()
                    + " từ " + checkIn + " đến " + checkOut + " thành công!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/partner/room-status";
    }

    /**
     * POST /partner/bookings/{id}/cancel-block — Hủy chặn phòng.
     */
    @PostMapping("/bookings/{id}/cancel-block")
    public String cancelBlock(@PathVariable Long id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        try {
            Booking b = bookingService.cancelBlock(id, partner);
            logPartnerAction(partner, "PARTNER_CANCEL_BLOCK", "BOOKING", id,
                    "Partner hủy chặn " + getUnitLabel(partner) + ": " + b.getBookingCode(), null);
            ra.addFlashAttribute("successMessage", "✅ Đã hủy chặn " + getUnitLabel(partner) + " thành công!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/partner/room-status";
    }

    /**
     * POST /partner/bookings/{id}/cancel-direct — Hủy booking trực tiếp.
     */
    @PostMapping("/bookings/{id}/cancel-direct")
    public String cancelDirectBooking(@PathVariable Long id,
                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                       RedirectAttributes ra) {
        User partner = getCurrentPartner(userDetails);
        try {
            Booking b = bookingService.cancelDirectBooking(id, partner);
            logPartnerAction(partner, "PARTNER_CANCEL_DIRECT", "BOOKING", id,
                    "Partner hủy booking trực tiếp: " + b.getBookingCode(), null);
            ra.addFlashAttribute("successMessage", "✅ Đã hủy booking trực tiếp thành công!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/partner/room-status";
    }
}
