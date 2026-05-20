package com.travelmate.controller.page;

import com.travelmate.dto.RoomAvailabilityDto;
import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Booking;
import com.travelmate.entity.Review;
import com.travelmate.entity.Room;
import com.travelmate.entity.TravelPost;
import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.repository.BookingRepository;
import com.travelmate.service.AccommodationService;
import com.travelmate.service.AvailabilityService;
import com.travelmate.service.ReviewService;
import com.travelmate.service.TravelPostService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AccommodationPageController — Điều khiển trang tìm kiếm và chi tiết khách sạn.
 *
 * Routes:
 *   GET /accommodations          → Trang danh sách nơi lưu trú (có filter theo type)
 *   GET /accommodations/{id}     → Trang chi tiết 1 nơi lưu trú + danh sách phòng
 */
@Controller
public class AccommodationPageController {
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AccommodationService accommodationService;
    private final ReviewService reviewService;
    private final BookingRepository bookingRepository;
    private final AvailabilityService availabilityService;
    private final TravelPostService travelPostService;

    public AccommodationPageController(AccommodationService accommodationService,
                                       ReviewService reviewService,
                                       BookingRepository bookingRepository,
                                       AvailabilityService availabilityService,
                                       TravelPostService travelPostService) {
        this.accommodationService = accommodationService;
        this.reviewService = reviewService;
        this.bookingRepository = bookingRepository;
        this.availabilityService = availabilityService;
        this.travelPostService = travelPostService;
    }

    /**
     * Trang danh sách nơi lưu trú.
     *
     * URL: /accommodations?type=HOTEL&keyword=Đà Lạt&checkIn=2026-05-01&checkOut=2026-05-02&adults=2&children=0&rooms=1
     *
     * @param type     loại lưu trú: HOTEL, VILLA, HOMESTAY, RESORT (mặc định HOTEL)
     * @param keyword  từ khóa tìm kiếm (tên KS hoặc thành phố)
     * @param checkIn  ngày nhận phòng (truyền qua URL, hiển thị trên search bar)
     * @param checkOut ngày trả phòng
     * @param adults   số người lớn
     * @param children số trẻ em
     * @param rooms    số phòng
     */
    @GetMapping("/accommodations")
    public String listHotels(
            @RequestParam(required = false, defaultValue = "HOTEL") String type,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String checkIn,
            @RequestParam(required = false, defaultValue = "") String checkOut,
            @RequestParam(required = false, defaultValue = "2") int adults,
            @RequestParam(required = false, defaultValue = "0") int children,
            @RequestParam(required = false, defaultValue = "1") int rooms,
            Model model) {

        // Parse type string → PropertyType enum, fallback HOTEL nếu sai
        PropertyType propertyType = parsePropertyType(type);

        // Tìm kiếm theo loại hình + keyword
        List<Accommodation> hotels = accommodationService.searchByType(propertyType, keyword);

        // ── Tính tình trạng phòng theo ngày cho từng accommodation (hiện badge trên card) ──
        Map<Long, Map<String, Object>> accAvailMap = new HashMap<>();
        boolean datesSelectedListing = false;
        if (!checkIn.isEmpty() && !checkOut.isEmpty()) {
            try {
                LocalDate ciDate = LocalDate.parse(checkIn);
                LocalDate coDate = LocalDate.parse(checkOut);
                if (coDate.isAfter(ciDate)) {
                    datesSelectedListing = true;
                    for (Accommodation acc : hotels) {
                        List<RoomAvailabilityDto> avails =
                                availabilityService.checkAvailabilityForAccommodation(acc, ciDate, coDate);
                        int totalAvail = avails.stream().mapToInt(RoomAvailabilityDto::getAvailableQuantity).sum();
                        int totalRooms = avails.stream().mapToInt(RoomAvailabilityDto::getTotalQuantity).sum();
                        String accStatus;
                        if (totalAvail <= 0) accStatus = "FULL";
                        else if (totalRooms > 0 && (totalAvail * 100 / totalRooms) <= 30) accStatus = "LIMITED";
                        else accStatus = "AVAILABLE";
                        Map<String, Object> info = new HashMap<>();
                        info.put("status", accStatus);
                        info.put("availableRooms", totalAvail);
                        accAvailMap.put(acc.getId(), info);
                    }
                }
            } catch (Exception ignored) {}
        }

        // ── Gợi ý du lịch theo điểm đến đang tìm ──────────────────────────────
        // Nếu user nhập tên nơi lưu trú (VD: "Tulip Hotel") không khớp destination_slug,
        // fallback sang city của kết quả đầu tiên có bài viết để demo vẫn đúng nghiệp vụ.
        String cleanKeyword = keyword != null ? keyword.trim() : "";
        String travelKeyword = cleanKeyword;
        String requestedTravelSlug = travelPostService.normalizeDestination(cleanKeyword);
        List<TravelPost> travelPosts = cleanKeyword.isBlank()
                ? List.of()
                : travelPostService.getVisibleByDestination(cleanKeyword);

        if (travelPosts.isEmpty() && !cleanKeyword.isBlank() && hotels != null && !hotels.isEmpty()) {
            String cityLabelFallback = "";
            for (Accommodation hotel : hotels) {
                String city = hotel.getCity();
                if (city == null || city.isBlank()) {
                    continue;
                }
                String citySlug = travelPostService.normalizeDestination(city);
                if (cityLabelFallback.isBlank()) {
                    cityLabelFallback = city;
                }
                if (!citySlug.isBlank() && citySlug.equals(requestedTravelSlug)) {
                    travelKeyword = city;
                    break;
                }
                List<TravelPost> cityPosts = travelPostService.getVisibleByDestination(city);
                if (!cityPosts.isEmpty()) {
                    travelKeyword = city;
                    travelPosts = cityPosts;
                    break;
                }
            }
            if (travelPosts.isEmpty() && !cityLabelFallback.isBlank()) {
                travelKeyword = cityLabelFallback;
            }
        }

        String travelDestSlug = travelPostService.normalizeDestination(travelKeyword);
        String travelDestLabel = travelPostService.getDestinationDisplayName(travelKeyword);
        String keywordDisplay = cleanKeyword.isBlank()
                ? ""
                : travelPostService.getDestinationDisplayName(cleanKeyword);
        boolean showTravelSuggestSection = !cleanKeyword.isBlank();

        // Truyền dữ liệu vào template
        model.addAttribute("hotels", hotels);
        model.addAttribute("keyword", keyword);
        model.addAttribute("keywordDisplay", keywordDisplay);
        model.addAttribute("currentType", type.toUpperCase());
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("checkInDisplay", formatDateVN(checkIn));
        model.addAttribute("checkOutDisplay", formatDateVN(checkOut));
        model.addAttribute("adults", adults);
        model.addAttribute("children", children);
        model.addAttribute("rooms", rooms);
        model.addAttribute("totalResults", hotels.size());
        model.addAttribute("accAvailMap", accAvailMap);
        model.addAttribute("datesSelectedListing", datesSelectedListing);
        // ── Travel suggestions ──
        model.addAttribute("travelPosts", travelPosts);
        model.addAttribute("travelKeyword", travelKeyword);
        model.addAttribute("travelDestSlug", travelDestSlug);
        model.addAttribute("travelDestLabel", travelDestLabel);
        model.addAttribute("hasTravelPosts", !travelPosts.isEmpty());
        model.addAttribute("showTravelSuggestSection", showTravelSuggestSection);

        return "user/hotels";
    }

    /**
     * Trang chi tiết khách sạn + danh sách phòng còn trống.
     *
     * URL: /accommodations/{id}?checkIn=...&checkOut=...&adults=2&children=0&rooms=1
     *
     * @param id ID của accommodation
     */
    @GetMapping("/accommodations/{id}")
    public String hotelDetail(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "") String checkIn,
            @RequestParam(required = false, defaultValue = "") String checkOut,
            @RequestParam(required = false, defaultValue = "2") int adults,
            @RequestParam(required = false, defaultValue = "0") int children,
            @RequestParam(required = false, defaultValue = "1") int rooms,
            Model model) {

        // Tìm khách sạn theo ID
        Optional<Accommodation> optHotel = accommodationService.getById(id);
        if (optHotel.isEmpty()) {
            // Nếu không tìm thấy → redirect về trang danh sách
            return "redirect:/accommodations";
        }

        Accommodation hotel = optHotel.get();

        // A4: Chặn truy cập nếu accommodation chưa được duyệt
        if (hotel.getApprovalStatus() != ApprovalStatus.APPROVED) {
            return "redirect:/accommodations";
        }

        // ── Phòng hiển thị + tình trạng theo ngày ──────────────────────────────
        List<Room> displayRooms;
        Map<Long, RoomAvailabilityDto> availabilityMap = new HashMap<>();
        boolean datesSelected = false;

        if (!checkIn.isEmpty() && !checkOut.isEmpty()) {
            try {
                LocalDate ciDate = LocalDate.parse(checkIn);
                LocalDate coDate = LocalDate.parse(checkOut);
                if (coDate.isAfter(ciDate)) {
                    // Tính tình trạng từng phòng theo khoảng ngày đã chọn
                    List<RoomAvailabilityDto> availList =
                            availabilityService.checkAvailabilityForAccommodation(hotel, ciDate, coDate);
                    for (RoomAvailabilityDto dto : availList) {
                        availabilityMap.put(dto.getRoomId(), dto);
                    }
                    // Khi có ngày: hiển thị TẤT CẢ phòng APPROVED (kể cả FULL) để user thấy rõ tình trạng
                    displayRooms = accommodationService.getAllApprovedRooms(hotel);
                    datesSelected = true;
                } else {
                    displayRooms = accommodationService.getAvailableRooms(hotel);
                }
            } catch (Exception e) {
                displayRooms = accommodationService.getAvailableRooms(hotel);
            }
        } else {
            // Chưa chọn ngày: chỉ hiện phòng còn slot hiện tại
            displayRooms = accommodationService.getAvailableRooms(hotel);
        }

        // Lấy danh sách đánh giá từ DB
        List<Review> reviews = reviewService.getReviewsByAccommodation(hotel);

        // ── Lịch đặt phòng: lấy các booking đang active để hiển thị ngày đã đặt ──
        List<BookingStatus> activeStatuses = List.of(
            BookingStatus.PENDING_ADMIN_APPROVAL,
            BookingStatus.CONFIRMED,
            BookingStatus.CHECKED_IN
        );
        List<Booking> activeBookings = bookingRepository.findByAccommodationAndBookingStatusIn(hotel, activeStatuses);

        // Tạo map: roomId → list of {checkIn, checkOut, qty} date ranges
        Map<Long, List<Map<String, String>>> bookedRanges = new HashMap<>();
        Map<Long, Integer> activeQtyPerRoom = new HashMap<>();
        for (Booking b : activeBookings) {
            if (b.getRoom() != null && b.getCheckIn() != null && b.getCheckOut() != null) {
                Long roomId = b.getRoom().getId();
                bookedRanges.computeIfAbsent(roomId, k -> new ArrayList<>());
                Map<String, String> range = new HashMap<>();
                range.put("checkIn", b.getCheckIn().toString());
                range.put("checkOut", b.getCheckOut().toString());
                range.put("status", b.getBookingStatus().name());
                int qty = b.getRoomQuantity() != null ? b.getRoomQuantity() : 1;
                range.put("qty", String.valueOf(qty));
                bookedRanges.get(roomId).add(range);
                activeQtyPerRoom.merge(roomId, qty, (x, y) -> x + y);
            }
        }

        // Tính totalQuantity mỗi phòng cho calendar 3-state display
        Map<Long, Integer> roomTotalQtyMap = new HashMap<>();
        for (Room r : displayRooms) {
            int activeQty = activeQtyPerRoom.getOrDefault(r.getId(), 0);
            int totalQty = Math.max(1, r.getAvailableQuantity() + activeQty);
            roomTotalQtyMap.put(r.getId(), totalQty);
        }

        // Truyền vào template
        model.addAttribute("hotel", hotel);
        model.addAttribute("availableRooms", displayRooms);
        model.addAttribute("availabilityMap", availabilityMap);
        model.addAttribute("datesSelected", datesSelected);
        model.addAttribute("reviews", reviews);
        model.addAttribute("reviewCount", reviews.size());
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("checkInDisplay", formatDateVN(checkIn));
        model.addAttribute("checkOutDisplay", formatDateVN(checkOut));
        model.addAttribute("adults", adults);
        model.addAttribute("children", children);
        model.addAttribute("rooms", rooms);
        model.addAttribute("bookedRanges", bookedRanges);
        model.addAttribute("roomTotalQtyMap", roomTotalQtyMap);
        model.addAttribute("travelSuggestions", travelPostService.getTopVisibleByDestination(hotel.getCity()));
        model.addAttribute("travelDestinationLabel", travelPostService.getDestinationDisplayName(hotel.getCity()));
        model.addAttribute("travelDestinationSlug", travelPostService.normalizeDestination(hotel.getCity()));

        return "user/hotel-detail";
    }

    /**
     * Helper: parse string → PropertyType enum.
     * Fallback về HOTEL nếu giá trị không hợp lệ.
     */
    private PropertyType parsePropertyType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return PropertyType.HOTEL;
        }
        try {
            return PropertyType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PropertyType.HOTEL;
        }
    }

    private String formatDateVN(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) {
            return "";
        }
        try {
            return LocalDate.parse(isoDate).format(DISPLAY_DATE_FORMATTER);
        } catch (Exception e) {
            return "";
        }
    }
}
