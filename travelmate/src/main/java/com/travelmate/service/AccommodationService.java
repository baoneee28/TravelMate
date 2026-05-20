package com.travelmate.service;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Amenity;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.entity.enums.RoomCategory;
import com.travelmate.repository.AccommodationRepository;
import com.travelmate.repository.AmenityRepository;
import com.travelmate.repository.RoomRepository;
import com.travelmate.util.DestinationAliasUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * AccommodationService — Xử lý nghiệp vụ liên quan đến nơi lưu trú.
 *
 * Chức năng chính:
 *   1. Tìm kiếm khách sạn (theo keyword, loại hình) — User
 *   2. Xem chi tiết khách sạn + danh sách phòng còn trống — User
 *   3. Partner tạo mới Accommodation/Room — PENDING
 *   4. Admin duyệt/từ chối Accommodation
 *   5. Partner xem danh sách accommodation của mình
 */
@SuppressWarnings("null")
@Service
public class AccommodationService {

    private final AccommodationRepository accommodationRepository;
    private final RoomRepository roomRepository;
    private final AmenityRepository amenityRepository;

    public AccommodationService(AccommodationRepository accommodationRepository,
                                 RoomRepository roomRepository,
                                 AmenityRepository amenityRepository) {
        this.accommodationRepository = accommodationRepository;
        this.roomRepository = roomRepository;
        this.amenityRepository = amenityRepository;
    }

    private static boolean ownerIsActive(Accommodation accommodation) {
        return accommodation.getOwner() == null || "ACTIVE".equals(accommodation.getOwner().getStatus());
    }

    private static boolean matchesKeyword(Accommodation accommodation, String keyword) {
        return DestinationAliasUtil.matchesTextOrDestination(accommodation.getName(), keyword)
                || DestinationAliasUtil.matchesTextOrDestination(accommodation.getCity(), keyword)
                || DestinationAliasUtil.matchesTextOrDestination(accommodation.getAddress(), keyword);
    }

    // ─── AMENITIES ────────────────────────────────────────────────────────────

    /** Lấy tất cả tiện nghi, nhóm theo category để render checkbox trên form */
    public List<Amenity> getAllAmenities() {
        return amenityRepository.findAllByOrderByCategoryAscNameAsc();
    }

    /** Cập nhật tiện nghi cho phòng đã có */
    @Transactional
    public Room updateRoomAmenities(User partner, Long roomId, List<Long> amenityIds) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng!"));
        // Ownership check
        if (room.getAccommodation().getOwner() == null
                || !room.getAccommodation().getOwner().getId().equals(partner.getId())) {
            throw new RuntimeException("Bạn không có quyền cập nhật phòng này!");
        }
        List<Amenity> amenities = amenityIds == null ? new ArrayList<>()
                : amenityRepository.findAllById(amenityIds);
        room.setAmenities(amenities);
        return roomRepository.save(room);
    }

    // ─── USER: Tìm kiếm & xem accommodation ─────────────────────────────────

    /**
     * Tìm kiếm khách sạn đã được duyệt.
     *
     * @param keyword từ khóa tìm kiếm (tên hoặc thành phố), có thể null/rỗng
     * @return danh sách hotel đã APPROVED
     */
    public List<Accommodation> searchHotels(String keyword) {
        return searchByType(PropertyType.HOTEL, keyword);
    }

    /**
     * Lấy chi tiết 1 accommodation theo ID.
     */
    public Optional<Accommodation> getById(Long id) {
        return accommodationRepository.findById(id);
    }

    /**
     * Lấy danh sách phòng còn trống của 1 khách sạn.
     */
    public List<Room> getAvailableRooms(Accommodation accommodation) {
        // Chỉ trả phòng APPROVED còn slot — user không thấy phòng chờ duyệt
        return roomRepository.findByAccommodationAndAvailableQuantityGreaterThanAndApprovalStatus(
                accommodation, 0, ApprovalStatus.APPROVED);
    }

    /**
     * Lấy tất cả phòng APPROVED của 1 accommodation — dùng khi user đã chọn ngày (hiện cả phòng FULL).
     */
    public List<Room> getAllApprovedRooms(Accommodation accommodation) {
        return roomRepository.findByAccommodationAndApprovalStatus(accommodation, ApprovalStatus.APPROVED);
    }

    /**
     * Lấy phòng theo ID.
     */
    public Optional<Room> getRoomById(Long roomId) {
        return roomRepository.findById(roomId);
    }

    /**
     * Lấy tất cả phòng APPROVED của partner — dùng cho form tạo voucher theo phòng.
     */
    public List<Room> getApprovedRoomsForPartner(User partner) {
        return roomRepository.findByAccommodation_OwnerAndApprovalStatus(partner, ApprovalStatus.APPROVED);
    }

    /**
     * Tìm kiếm nơi lưu trú đã duyệt THEO LOẠI HÌNH.
     */
    public List<Accommodation> searchByType(PropertyType type, String keyword) {
        List<Accommodation> approvedActive = accommodationRepository
                .findByPropertyTypeAndApprovalStatus(type, ApprovalStatus.APPROVED)
                .stream()
                .filter(AccommodationService::ownerIsActive)
                .collect(java.util.stream.Collectors.toList());

        String normalizedKeyword = DestinationAliasUtil.normalizeText(keyword);
        if (normalizedKeyword.isBlank()) {
            return approvedActive;
        }

        return approvedActive.stream()
                .filter(a -> matchesKeyword(a, keyword))
                .collect(java.util.stream.Collectors.toList());
    }

    // ─── PARTNER: Quản lý accommodation của mình ─────────────────────────────

    /**
     * Lấy tất cả accommodation thuộc 1 partner.
     * Partner xem toàn bộ listing của mình (PENDING, APPROVED, REJECTED).
     */
    public List<Accommodation> getAccommodationsByOwner(User partner) {
        return accommodationRepository.findByOwner(partner);
    }

    /**
     * Partner tạo mới Accommodation.
     *
     * Rule:
     *   - approvalStatus = PENDING (chờ admin duyệt)
     *   - propertyType phải khớp với partner.partnerPropertyType
     *   - owner = partner hiện tại
     *
     * @return Accommodation vừa tạo
     * @throws RuntimeException nếu vi phạm business rule
     */
    @Transactional
    public Accommodation createAccommodation(User partner,
                                              String name, String address, String city,
                                              String description, String thumbnailUrl,
                                              PropertyType propertyType, Integer starRating) {
        // Validate: partner phải có partnerPropertyType được set
        if (partner.getPartnerPropertyType() == null) {
            throw new RuntimeException("Tài khoản partner chưa đăng ký loại lưu trú. Liên hệ Admin.");
        }

        // Business rule: partner chỉ tạo đúng loại lưu trú đã đăng ký
        if (propertyType != partner.getPartnerPropertyType()) {
            throw new RuntimeException(
                "Partner chỉ được tạo loại lưu trú đã đăng ký: "
                + partner.getPartnerPropertyType().name()
                + ". Bạn đang cố tạo: " + propertyType.name());
        }

        Accommodation acc = new Accommodation();
        acc.setName(name.trim());
        acc.setAddress(address != null ? address.trim() : "");
        acc.setCity(city != null ? city.trim() : "");
        acc.setDescription(description != null ? description.trim() : "");
        acc.setThumbnailUrl(thumbnailUrl != null ? thumbnailUrl.trim() : "");
        acc.setPropertyType(propertyType);
        acc.setApprovalStatus(ApprovalStatus.PENDING); // chờ admin duyệt
        acc.setOwner(partner);
        acc.setStarRating(starRating);
        acc.setRating(0.0);
        acc.setReviewCount(0);

        return accommodationRepository.save(acc);
    }

    /**
     * Partner tạo mới Room cho accommodation đã APPROVED của mình.
     *
     * Rule:
     *   - Accommodation phải thuộc partner (kiểm tra owner)
     *   - Accommodation phải APPROVED — không tạo phòng cho listing chưa duyệt
     *
     * @param roomCategory           Loại phòng (STANDARD/DELUXE/VIP/...), mặc định STANDARD nếu null
     * @param commissionRateOverride Commission riêng (%), null = dùng mặc định theo PropertyType
     * @return Room vừa tạo
     */
    @Transactional
    public Room createRoom(User partner, Long accommodationId,
                           String roomCode, String roomName, String bedType,
                           Integer capacity, BigDecimal pricePerNight,
                           Integer availableQuantity, String imageUrl, String description,
                           RoomCategory roomCategory, BigDecimal commissionRateOverride,
                           List<Long> amenityIds) {

        // Kiểm tra accommodation tồn tại và thuộc partner
        Accommodation acc = accommodationRepository.findByIdAndOwner(accommodationId, partner)
                .orElseThrow(() -> new RuntimeException(
                    "Không tìm thấy cơ sở lưu trú hoặc bạn không có quyền thêm phòng!"));

        // Accommodation phải APPROVED mới được thêm phòng
        if (acc.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new RuntimeException(
                "Cơ sở lưu trú chưa được Admin duyệt! Vui lòng chờ Admin duyệt trước khi thêm phòng.");
        }

        // Kiểm tra roomCode unique
        if (roomRepository.existsByRoomCode(roomCode.trim())) {
            throw new RuntimeException("Mã phòng '" + roomCode + "' đã tồn tại! Vui lòng dùng mã khác.");
        }

        // Validate commissionRateOverride: 0 <= rate <= 100
        if (commissionRateOverride != null) {
            if (commissionRateOverride.compareTo(BigDecimal.ZERO) < 0
                    || commissionRateOverride.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new RuntimeException("Commission override phải từ 0% đến 100%!");
            }
        }

        Room room = new Room();
        room.setRoomCode(roomCode.trim().toUpperCase());
        room.setRoomName(roomName.trim());
        room.setBedType(bedType != null ? bedType.trim() : "");
        room.setCapacity(capacity);
        room.setPricePerNight(pricePerNight);
        room.setAvailableQuantity(availableQuantity);
        room.setImageUrl(imageUrl != null ? imageUrl.trim() : "");
        room.setDescription(description != null ? description.trim() : "");
        room.setAccommodation(acc);
        room.setApprovalStatus(ApprovalStatus.PENDING); // Partner tạo → chờ Admin duyệt

        // v2: RoomCategory và commissionRateOverride
        room.setRoomCategory(roomCategory != null ? roomCategory : RoomCategory.STANDARD);
        room.setCommissionRateOverride(commissionRateOverride); // null = dùng default

        // Gắn tiện nghi nếu có
        if (amenityIds != null && !amenityIds.isEmpty()) {
            List<Amenity> amenities = amenityRepository.findAllById(amenityIds);
            room.setAmenities(amenities);
        }

        return roomRepository.save(room);
    }

    // ─── ADMIN: Duyệt/từ chối accommodation ──────────────────────────────────

    /**
     * Admin lấy danh sách tất cả accommodation (có thể lọc theo status).
     */
    public List<Accommodation> getAllAccommodationsForAdmin() {
        return accommodationRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Admin lấy danh sách accommodation đang PENDING.
     */
    public List<Accommodation> getPendingAccommodations() {
        return accommodationRepository.findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus.PENDING);
    }

    /**
     * Admin duyệt accommodation → APPROVED.
     * Sau khi duyệt, User mới thấy listing này trên /accommodations.
     */
    @Transactional
    public Accommodation approveAccommodation(Long id) {
        Accommodation acc = accommodationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nơi lưu trú!"));
        if (acc.getApprovalStatus() == ApprovalStatus.APPROVED) {
            throw new RuntimeException("Nơi lưu trú này đã được duyệt rồi!");
        }
        acc.setApprovalStatus(ApprovalStatus.APPROVED);
        return accommodationRepository.save(acc);
    }

    /**
     * Admin từ chối accommodation → REJECTED (không lý do).
     */
    @Transactional
    public Accommodation rejectAccommodation(Long id) {
        return rejectAccommodation(id, null);
    }

    /**
     * Admin từ chối accommodation → REJECTED (có lý do).
     * Lưu lý do vào description nếu có.
     */
    @Transactional
    public Accommodation rejectAccommodation(Long id, String rejectReason) {
        Accommodation acc = accommodationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nơi lưu trú!"));
        acc.setApprovalStatus(ApprovalStatus.REJECTED);
        if (rejectReason != null && !rejectReason.isBlank()) {
            String existing = acc.getDescription() != null ? acc.getDescription() : "";
            acc.setDescription(existing + " [Admin từ chối: " + rejectReason.trim() + "]");
        }
        return accommodationRepository.save(acc);
    }

    /**
     * Thống kê accommodation cho admin.
     */
    public long countPendingAccommodations() {
        return accommodationRepository.countByApprovalStatus(ApprovalStatus.PENDING);
    }

    public long countApprovedAccommodations() {
        return accommodationRepository.countByApprovalStatus(ApprovalStatus.APPROVED);
    }

    public long countRejectedAccommodations() {
        return accommodationRepository.countByApprovalStatus(ApprovalStatus.REJECTED);
    }

    // ─── ADMIN: Duyệt/từ chối Room ────────────────────────────────────────────

    /**
     * Admin lấy tất cả phòng (mọi trạng thái).
     */
    public List<Room> getAllRoomsForAdmin() {
        return roomRepository.findAllWithAccommodationOrderByIdDesc();
    }

    /**
     * Admin duyệt phòng → APPROVED.
     * Sau khi duyệt, User mới thấy và đặt được phòng này.
     */
    @Transactional
    public Room approveRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng!"));
        if (room.getApprovalStatus() == ApprovalStatus.APPROVED) {
            throw new RuntimeException("Phòng này đã được duyệt rồi!");
        }
        // Guard: accommodation cha phải đã APPROVED mới được duyệt phòng
        if (room.getAccommodation() == null
                || room.getAccommodation().getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new RuntimeException(
                "Chỉ được duyệt phòng khi cơ sở lưu trú cha đã được Admin duyệt! " +
                "Cơ sở '" + room.getAccommodation().getName() + "' hiện chưa được duyệt.");
        }
        room.setApprovalStatus(ApprovalStatus.APPROVED);
        return roomRepository.save(room);
    }

    /**
     * Admin từ chối phòng → REJECTED (không lý do).
     */
    @Transactional
    public Room rejectRoom(Long roomId) {
        return rejectRoom(roomId, null);
    }

    /**
     * Admin từ chối phòng → REJECTED (có lý do).
     */
    @Transactional
    public Room rejectRoom(Long roomId, String rejectReason) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng!"));
        room.setApprovalStatus(ApprovalStatus.REJECTED);
        if (rejectReason != null && !rejectReason.isBlank()) {
            String existing = room.getDescription() != null ? room.getDescription() : "";
            room.setDescription(existing + " [Admin từ chối: " + rejectReason.trim() + "]");
        }
        return roomRepository.save(room);
    }

    /**
     * Thống kê phòng cho admin dashboard.
     */
    public long countPendingRooms() {
        return roomRepository.countByApprovalStatus(ApprovalStatus.PENDING);
    }

    public long countApprovedRooms() {
        return roomRepository.countByApprovalStatus(ApprovalStatus.APPROVED);
    }
}
