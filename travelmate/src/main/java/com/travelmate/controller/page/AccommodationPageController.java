package com.travelmate.controller.page;

import com.travelmate.dto.RoomAvailabilityDto;
import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Booking;
import com.travelmate.entity.Review;
import com.travelmate.entity.Room;
import com.travelmate.entity.RoomImage;
import com.travelmate.entity.TravelPost;
import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.repository.BookingRepository;
import com.travelmate.service.AccommodationService;
import com.travelmate.service.AvailabilityService;
import com.travelmate.service.ReviewService;
import com.travelmate.service.RoomImageService;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private static final int DETAIL_GALLERY_SIZE = 5;
    private static final int DETAIL_ROOM_UPLOAD_LIMIT = 3;
    private static final String LATA_HOTEL_NAME = "LATA Hotel & Apartments";
    private static final List<Map.Entry<String, String>> LATA_DETAIL_GALLERY_IMAGES = List.of(
            Map.entry("/assets/images/accommodations/lata/cover/room-hero.jpg",
                    "LATA Hotel & Apartments - phòng ngủ"),
            Map.entry("/assets/images/accommodations/lata/shared/main-lounge.jpg",
                    "LATA Hotel & Apartments - khu lounge"),
            Map.entry("/assets/images/accommodations/lata/shared/dining-area.jpg",
                    "LATA Hotel & Apartments - khu ăn uống"),
            Map.entry("/assets/images/accommodations/lata/shared/open-kitchen.jpg",
                    "LATA Hotel & Apartments - bếp chung"),
            Map.entry("/assets/images/accommodations/lata/rooms/standard-king/interior.jpg",
                    "LATA Hotel & Apartments - phòng Standard King"));
    private static final Map<PropertyType, List<String>> DETAIL_GALLERY_FALLBACK_URLS = Map.of(
            PropertyType.HOTEL, List.of(
                    "/assets/images/accommodations/catalog/hotel-exterior.jpg",
                    "/assets/images/accommodations/amenities/hotel/lobby.jpg",
                    "/assets/images/accommodations/amenities/hotel/bedroom.jpg",
                    "/assets/images/accommodations/amenities/hotel/pool.jpg",
                    "/assets/images/accommodations/amenities/hotel/restaurant.jpg"),
            PropertyType.VILLA, List.of(
                    "/assets/images/accommodations/catalog/villa-exterior.jpg",
                    "/assets/images/accommodations/amenities/villa/bedroom.jpg",
                    "/assets/images/accommodations/amenities/villa/private-pool.jpg",
                    "/assets/images/accommodations/amenities/villa/kitchen.jpg",
                    "/assets/images/accommodations/amenities/villa/panoramic-view.jpg"),
            PropertyType.HOMESTAY, List.of(
                    "/assets/images/accommodations/catalog/homestay-exterior.jpg",
                    "/assets/images/accommodations/amenities/homestay/bedroom.jpg",
                    "/assets/images/accommodations/amenities/homestay/garden.jpg",
                    "/assets/images/accommodations/amenities/homestay/shared-kitchen.jpg",
                    "/assets/images/accommodations/amenities/homestay/breakfast.jpg"),
            PropertyType.RESORT, List.of(
                    "/assets/images/accommodations/catalog/resort-exterior.jpg",
                    "/assets/images/accommodations/catalog/resort-room.jpg",
                    "/assets/images/accommodations/amenities/resort/private-beach.jpg",
                    "/assets/images/accommodations/amenities/resort/infinity-pool.jpg",
                    "/assets/images/accommodations/amenities/resort/spa.jpg"));

    private final AccommodationService accommodationService;
    private final ReviewService reviewService;
    private final BookingRepository bookingRepository;
    private final AvailabilityService availabilityService;
    private final TravelPostService travelPostService;
    private final RoomImageService roomImageService;

    public AccommodationPageController(AccommodationService accommodationService,
                                       ReviewService reviewService,
                                       BookingRepository bookingRepository,
                                       AvailabilityService availabilityService,
                                       TravelPostService travelPostService,
                                       RoomImageService roomImageService) {
        this.accommodationService = accommodationService;
        this.reviewService = reviewService;
        this.bookingRepository = bookingRepository;
        this.availabilityService = availabilityService;
        this.travelPostService = travelPostService;
        this.roomImageService = roomImageService;
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
            @RequestParam(required = false, defaultValue = "") String voucherCode,
            Model model) {

        // Parse type string → PropertyType enum, fallback HOTEL nếu sai
        PropertyType propertyType = parsePropertyType(type);
        String safeVoucherCode = normalizeVoucherCode(voucherCode);
        String effectiveKeyword = (keyword == null || keyword.trim().isBlank())
                ? (safeVoucherCode.isBlank() ? "Đà Lạt" : "")
                : keyword.trim();

        // Tìm kiếm theo loại hình + keyword
        List<Accommodation> hotels = accommodationService.searchByType(propertyType, effectiveKeyword);

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
        String cleanKeyword = effectiveKeyword.trim();
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

        Map<Long, List<Map<String, String>>> listingCardImagesByAccommodationId = buildListingCardImages(hotels);
        Map<Long, Map<String, String>> cardMainImageByAccommodationId = new HashMap<>();
        Map<Long, List<Map<String, String>>> cardSideImagesByAccommodationId = new HashMap<>();
        for (Map.Entry<Long, List<Map<String, String>>> entry : listingCardImagesByAccommodationId.entrySet()) {
            List<Map<String, String>> images = entry.getValue();
            if (images == null || images.isEmpty()) {
                continue;
            }
            cardMainImageByAccommodationId.put(entry.getKey(), images.get(0));
            cardSideImagesByAccommodationId.put(entry.getKey(), images.stream().skip(1).limit(2).toList());
        }

        // Truyền dữ liệu vào template
        model.addAttribute("hotels", hotels);
        model.addAttribute("cardMainImageByAccommodationId", cardMainImageByAccommodationId);
        model.addAttribute("cardSideImagesByAccommodationId", cardSideImagesByAccommodationId);
        model.addAttribute("keyword", effectiveKeyword);
        model.addAttribute("keywordDisplay", keywordDisplay);
        model.addAttribute("currentType", propertyType.name());
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("checkInDisplay", formatDateVN(checkIn));
        model.addAttribute("checkOutDisplay", formatDateVN(checkOut));
        model.addAttribute("adults", adults);
        model.addAttribute("children", children);
        model.addAttribute("rooms", rooms);
        model.addAttribute("voucherCode", safeVoucherCode);
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

    public String listHotels(String type, String keyword, String checkIn, String checkOut,
                             int adults, int children, int rooms, Model model) {
        return listHotels(type, keyword, checkIn, checkOut, adults, children, rooms, "", model);
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
            @RequestParam(required = false, defaultValue = "") String voucherCode,
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

        List<Room> galleryRooms = accommodationService.getAllApprovedRooms(hotel);
        if (galleryRooms.isEmpty()) {
            PropertyType type = hotel.getPropertyType() == null ? PropertyType.HOTEL : hotel.getPropertyType();
            return "redirect:/accommodations?type=" + type.name();
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
                    displayRooms = galleryRooms;
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

        // Gallery luôn dùng toàn bộ phòng/căn đang mở bán, kể cả loại phòng đã hết suất ở ngày đang chọn.
        Map<Long, List<RoomImage>> roomImagesMap = roomImageService.getImagesForRooms(galleryRooms);
        List<Map<String, String>> galleryImages = buildDetailGallery(hotel, galleryRooms, roomImagesMap);

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
        model.addAttribute("roomImagesMap", roomImagesMap);
        model.addAttribute("galleryImages", galleryImages);
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
        model.addAttribute("voucherCode", normalizeVoucherCode(voucherCode));
        model.addAttribute("bookedRanges", bookedRanges);
        model.addAttribute("roomTotalQtyMap", roomTotalQtyMap);
        model.addAttribute("travelSuggestions", travelPostService.getTopVisibleByDestination(hotel.getCity()));
        model.addAttribute("travelDestinationLabel", travelPostService.getDestinationDisplayName(hotel.getCity()));
        model.addAttribute("travelDestinationSlug", travelPostService.normalizeDestination(hotel.getCity()));

        return "user/hotel-detail";
    }

    public String hotelDetail(Long id, String checkIn, String checkOut,
                              int adults, int children, int rooms, Model model) {
        return hotelDetail(id, checkIn, checkOut, adults, children, rooms, "", model);
    }

    private List<Map<String, String>> buildDetailGallery(Accommodation hotel, List<Room> galleryRooms,
                                                          Map<Long, List<RoomImage>> roomImagesMap) {
        List<Map<String, String>> gallery = new ArrayList<>();
        Set<String> usedUrls = new LinkedHashSet<>();
        if (LATA_HOTEL_NAME.equalsIgnoreCase(hotel.getName())) {
            for (Map.Entry<String, String> image : LATA_DETAIL_GALLERY_IMAGES) {
                addGalleryImage(gallery, usedUrls, image.getKey(), image.getValue());
            }
            return gallery;
        }

        for (Room room : galleryRooms) {
            for (RoomImage image : roomImagesMap.getOrDefault(room.getId(), List.of())) {
                if (gallery.size() >= DETAIL_ROOM_UPLOAD_LIMIT) {
                    return gallery;
                }
                String alt = image.getCaption() == null || image.getCaption().isBlank()
                        ? hotel.getName() + " - " + room.getRoomName()
                        : image.getCaption();
                addGalleryImage(gallery, usedUrls, image.getImageUrl(), alt);
            }
        }

        if (!gallery.isEmpty()) {
            return gallery;
        }

        for (Room room : galleryRooms) {
            if (gallery.size() >= DETAIL_GALLERY_SIZE) {
                break;
            }
            addGalleryImage(gallery, usedUrls, room.getImageUrl(),
                    hotel.getName() + " - " + room.getRoomName());
        }

        if (!gallery.isEmpty()) {
            return gallery;
        }

        addGalleryImage(gallery, usedUrls, hotel.getThumbnailUrl(), hotel.getName());

        PropertyType propertyType = hotel.getPropertyType() == null ? PropertyType.HOTEL : hotel.getPropertyType();
        int fallbackIndex = 1;
        for (String fallbackUrl : DETAIL_GALLERY_FALLBACK_URLS.get(propertyType)) {
            if (gallery.size() >= DETAIL_GALLERY_SIZE) {
                break;
            }
            addGalleryImage(gallery, usedUrls, fallbackUrl,
                    hotel.getName() + " - hình ảnh " + fallbackIndex++);
        }
        return gallery;
    }

    private Map<Long, List<Map<String, String>>> buildListingCardImages(List<Accommodation> hotels) {
        Map<Long, List<Map<String, String>>> result = new HashMap<>();
        if (hotels == null) {
            return result;
        }
        for (Accommodation hotel : hotels) {
            List<Map<String, String>> images = new ArrayList<>();
            Set<String> usedUrls = new LinkedHashSet<>();
            List<Room> approvedRooms = accommodationService.getAllApprovedRooms(hotel);
            Map<Long, List<RoomImage>> roomImagesMap = roomImageService.getImagesForRooms(approvedRooms);

            for (Room room : approvedRooms) {
                for (RoomImage image : roomImagesMap.getOrDefault(room.getId(), List.of())) {
                    String alt = image.getCaption() == null || image.getCaption().isBlank()
                            ? hotel.getName() + " - " + room.getRoomName()
                            : image.getCaption();
                    addListingCardImage(images, usedUrls, image.getImageUrl(), alt);
                    if (images.size() >= DETAIL_ROOM_UPLOAD_LIMIT) {
                        break;
                    }
                }
                if (images.size() >= DETAIL_ROOM_UPLOAD_LIMIT) {
                    break;
                }
            }

            if (!images.isEmpty()) {
                result.put(hotel.getId(), images);
                continue;
            }

            for (Room room : approvedRooms) {
                addListingCardImage(images, usedUrls, room.getImageUrl(),
                        hotel.getName() + " - " + room.getRoomName());
                if (images.size() >= 3) {
                    break;
                }
            }

            if (!images.isEmpty()) {
                result.put(hotel.getId(), images);
                continue;
            }

            addListingCardImage(images, usedUrls, hotel.getThumbnailUrl(), hotel.getName());

            PropertyType propertyType = hotel.getPropertyType() == null ? PropertyType.HOTEL : hotel.getPropertyType();
            int fallbackIndex = 1;
            for (String fallbackUrl : DETAIL_GALLERY_FALLBACK_URLS.get(propertyType)) {
                addListingCardImage(images, usedUrls, fallbackUrl,
                        hotel.getName() + " - hình ảnh " + fallbackIndex++);
                if (images.size() >= 3) {
                    break;
                }
            }
            result.put(hotel.getId(), images);
        }
        return result;
    }

    private static void addListingCardImage(List<Map<String, String>> images, Set<String> usedUrls,
                                            String source, String alt) {
        if (images.size() >= 3) {
            return;
        }
        addGalleryImage(images, usedUrls, source, alt);
    }

    private static void addGalleryImage(List<Map<String, String>> gallery, Set<String> usedUrls,
                                        String source, String alt) {
        if (source == null || source.isBlank()) {
            return;
        }
        String cleanSource = source.trim();
        if (usedUrls.add(cleanSource)) {
            gallery.add(Map.of("src", cleanSource, "alt", alt == null ? "" : alt));
        }
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

    private String normalizeVoucherCode(String voucherCode) {
        return voucherCode == null ? "" : voucherCode.trim().toUpperCase();
    }
}
