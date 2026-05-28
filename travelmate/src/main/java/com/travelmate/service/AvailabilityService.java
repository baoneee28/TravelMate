package com.travelmate.service;

import com.travelmate.dto.RoomAvailabilityDto;
import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Booking;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.BookingSource;
import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.repository.AccommodationRepository;
import com.travelmate.repository.BookingRepository;
import com.travelmate.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AvailabilityService — Kiểm tra tình trạng phòng trống theo khoảng ngày.
 *
 * Công thức lõi:
 *   totalQuantity   = room.availableQuantity + sumAllActiveBookingsForRoom
 *   occupiedInRange = sumOverlappingBookingsForRoom(checkIn, checkOut)
 *   availableInRange = totalQuantity - occupiedInRange
 *
 * Booking được tính là "giữ phòng" nếu status ∈ {PENDING_ADMIN_APPROVAL, CONFIRMED, CHECKED_IN}.
 * CANCELLED, NO_SHOW, COMPLETED không giữ phòng.
 *
 * Dùng chung cho Admin (toàn bộ) và Partner (chỉ cơ sở của mình).
 */
@Service
public class AvailabilityService {

    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(
            BookingStatus.PENDING_PAYMENT,
            BookingStatus.PENDING_ADMIN_APPROVAL,
            BookingStatus.CONFIRMED,
            BookingStatus.CHECKED_IN
    );

    private final BookingRepository bookingRepository;
    private final AccommodationRepository accommodationRepository;
    private final RoomRepository roomRepository;

    public AvailabilityService(BookingRepository bookingRepository,
                               AccommodationRepository accommodationRepository,
                               RoomRepository roomRepository) {
        this.bookingRepository = bookingRepository;
        this.accommodationRepository = accommodationRepository;
        this.roomRepository = roomRepository;
    }

    private boolean isManagedByPartner(Accommodation accommodation, User partner) {
        if (accommodation == null || partner == null || partner.getId() == null) {
            return false;
        }
        if (partner.getPartnerPropertyType() == null || accommodation.getPropertyType() == null) {
            return false;
        }
        return accommodation.getOwner() != null
                && accommodation.getOwner().getId() != null
                && accommodation.getOwner().getId().equals(partner.getId())
                && accommodation.getPropertyType() == partner.getPartnerPropertyType();
    }

    private boolean isRoomOpenForOnlineBooking(Room room) {
        return room != null && !Boolean.FALSE.equals(room.getAvailableForBooking());
    }

    /**
     * Kiểm tra tình trạng phòng toàn hệ thống — dùng cho Admin.
     *
     * @param checkIn       Ngày check-in yêu cầu
     * @param checkOut      Ngày check-out yêu cầu
     * @param city          Lọc theo thành phố (null = tất cả)
     * @param propertyType  Lọc theo loại lưu trú (null = tất cả)
     * @param ownerId       Lọc theo partner owner (null = tất cả)
     */
    public List<RoomAvailabilityDto> checkAvailabilityForAdmin(
            LocalDate checkIn, LocalDate checkOut,
            String city, String propertyType, Long ownerId) {

        List<Room> rooms = getAllApprovedRooms(city, propertyType, ownerId);
        return buildAvailabilityList(rooms, checkIn, checkOut);
    }

    /**
     * Kiểm tra tình trạng phòng cho Partner — chỉ phòng thuộc cơ sở của partner.
     *
     * @param partner  Partner đang đăng nhập
     * @param checkIn  Ngày check-in yêu cầu
     * @param checkOut Ngày check-out yêu cầu
     */
    public List<RoomAvailabilityDto> checkAvailabilityForPartner(
            User partner, LocalDate checkIn, LocalDate checkOut) {

        List<Accommodation> partnerAccommodations =
                accommodationRepository.findByOwnerAndApprovalStatus(partner, ApprovalStatus.APPROVED)
                        .stream()
                        .filter(acc -> isManagedByPartner(acc, partner))
                        .collect(Collectors.toList());

        List<Room> rooms = partnerAccommodations.stream()
                .flatMap(acc -> acc.getRooms().stream()
                        .filter(r -> r.getApprovalStatus() == ApprovalStatus.APPROVED)
                        .filter(this::isRoomOpenForOnlineBooking))
                .collect(Collectors.toList());

        return buildAvailabilityList(rooms, checkIn, checkOut);
    }

    /**
     * Kiểm tra tình trạng tất cả phòng APPROVED của 1 accommodation — dùng cho trang chi tiết nơi lưu trú.
     *
     * @param accommodation Nơi lưu trú cần kiểm tra
     * @param checkIn       Ngày check-in yêu cầu
     * @param checkOut      Ngày check-out yêu cầu
     */
    public List<RoomAvailabilityDto> checkAvailabilityForAccommodation(
            Accommodation accommodation, LocalDate checkIn, LocalDate checkOut) {
        List<Room> rooms = roomRepository.findByAccommodationAndApprovalStatus(
                accommodation, ApprovalStatus.APPROVED)
                .stream()
                .filter(this::isRoomOpenForOnlineBooking)
                .toList();
        return buildAvailabilityList(rooms, checkIn, checkOut);
    }

    /**
     * Tính tình trạng phòng cho một room cụ thể — dùng cho User booking form.
     */
    public RoomAvailabilityDto checkSingleRoom(Room room, LocalDate checkIn, LocalDate checkOut) {
        if (!isRoomOpenForOnlineBooking(room)) {
            return null;
        }
        List<Room> single = List.of(room);
        List<RoomAvailabilityDto> result = buildAvailabilityList(single, checkIn, checkOut);
        return result.isEmpty() ? null : result.get(0);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private List<RoomAvailabilityDto> buildAvailabilityList(
            List<Room> rooms, LocalDate checkIn, LocalDate checkOut) {

        if (rooms.isEmpty()) return new ArrayList<>();

        // Map roomId → sumRoomQuantity cho TẤT CẢ active bookings (để tính total)
        Map<Long, Integer> allActiveMap = buildActiveMap();

        // Map roomId → sumRoomQuantity cho bookings OVERLAP khoảng ngày yêu cầu
        Map<Long, Integer> overlappingMap = buildOverlappingMap(checkIn, checkOut);

        List<RoomAvailabilityDto> result = new ArrayList<>();
        for (Room room : rooms) {
            Long rid = room.getId();
            int currentAvailable = room.getAvailableQuantity();
            int allActiveForRoom  = allActiveMap.getOrDefault(rid, 0);
            int totalQuantity     = currentAvailable + allActiveForRoom;
            int occupiedInRange   = overlappingMap.getOrDefault(rid, 0);
            int availableInRange  = Math.max(0, totalQuantity - occupiedInRange);

            Accommodation acc = room.getAccommodation();
            String ownerName = (acc.getOwner() != null)
                    ? (acc.getOwner().getName() != null
                            ? acc.getOwner().getName()
                            : acc.getOwner().getEmail())
                    : "—";

            result.add(new RoomAvailabilityDto(
                    rid,
                    room.getRoomCode(),
                    room.getRoomName(),
                    room.getRoomCategory() != null ? room.getRoomCategory().name() : "STANDARD",
                    acc.getId(),
                    acc.getName(),
                    acc.getCity(),
                    acc.getPropertyType() != null ? acc.getPropertyType().name() : "HOTEL",
                    ownerName,
                    room.getPricePerNight(),
                    totalQuantity,
                    occupiedInRange,
                    availableInRange
            ));
        }

        // Sắp xếp: FULL trước, rồi LIMITED, rồi AVAILABLE — dễ nhìn nhất cho demo
        result.sort((a, b) -> {
            int rankA = statusRank(a.getStatus());
            int rankB = statusRank(b.getStatus());
            if (rankA != rankB) return Integer.compare(rankA, rankB);
            return a.getAccommodationName().compareTo(b.getAccommodationName());
        });

        return result;
    }

    private int statusRank(String status) {
        return switch (status) {
            case "FULL"      -> 0;
            case "LIMITED"   -> 1;
            default          -> 2; // AVAILABLE
        };
    }

    private Map<Long, Integer> buildActiveMap() {
        List<Object[]> rows = bookingRepository.sumActiveBookingsByRoom(ACTIVE_STATUSES);
        Map<Long, Integer> map = new HashMap<>();
        for (Object[] row : rows) {
            Long roomId = ((Number) row[0]).longValue();
            int qty = row[1] == null ? 0 : ((Number) row[1]).intValue();
            map.put(roomId, qty);
        }
        return map;
    }

    private Map<Long, Integer> buildOverlappingMap(LocalDate checkIn, LocalDate checkOut) {
        List<Object[]> rows = bookingRepository.sumOverlappingBookingsByRoom(
                checkIn, checkOut, ACTIVE_STATUSES);
        Map<Long, Integer> map = new HashMap<>();
        for (Object[] row : rows) {
            Long roomId = ((Number) row[0]).longValue();
            int qty = row[1] == null ? 0 : ((Number) row[1]).intValue();
            map.put(roomId, qty);
        }
        return map;
    }

    private List<Room> getAllApprovedRooms(String city, String propertyType, Long ownerId) {
        List<Accommodation> accommodations;

        if (ownerId != null) {
            accommodations = accommodationRepository.findAll().stream()
                    .filter(a -> a.getApprovalStatus() == ApprovalStatus.APPROVED
                            && a.getOwner() != null
                            && a.getOwner().getId().equals(ownerId))
                    .collect(Collectors.toList());
        } else {
            accommodations = accommodationRepository.findAll().stream()
                    .filter(a -> a.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .collect(Collectors.toList());
        }

        // Lọc thêm city / propertyType nếu có
        if (city != null && !city.isBlank()) {
            String cityLower = city.trim().toLowerCase();
            accommodations = accommodations.stream()
                    .filter(a -> a.getCity() != null && a.getCity().toLowerCase().contains(cityLower))
                    .collect(Collectors.toList());
        }
        if (propertyType != null && !propertyType.isBlank()) {
            accommodations = accommodations.stream()
                    .filter(a -> a.getPropertyType() != null
                            && a.getPropertyType().name().equalsIgnoreCase(propertyType))
                    .collect(Collectors.toList());
        }

        return accommodations.stream()
                .flatMap(acc -> acc.getRooms().stream()
                        .filter(r -> r.getApprovalStatus() == ApprovalStatus.APPROVED))
                .collect(Collectors.toList());
    }

    /**
     * Xây dựng bảng tình trạng phòng hôm nay cho partner — dùng cho trang room-status.
     *
     * Với mỗi room APPROVED của partner, tính:
     *   - platformQuantity = availableQuantity (còn trống DB) + tất cả active bookings
     *   - heldOnline    = booking ONLINE đang PENDING/CONFIRMED/CHECKED_IN hôm nay
     *   - directOccupied = booking DIRECT đang CONFIRMED/CHECKED_IN hôm nay
     *   - blocked       = MANUAL_BLOCK đang CONFIRMED hôm nay
     *   - freeToday     = platformQuantity - heldOnline - directOccupied - blocked
     */
    public List<RoomStatusDto> buildRoomStatusForPartner(User partner) {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        List<Accommodation> partnerAccommodations =
                accommodationRepository.findByOwnerAndApprovalStatus(partner, ApprovalStatus.APPROVED)
                        .stream()
                        .filter(acc -> isManagedByPartner(acc, partner))
                        .collect(Collectors.toList());

        List<Room> rooms = partnerAccommodations.stream()
                .flatMap(acc -> acc.getRooms().stream()
                        .filter(r -> r.getApprovalStatus() == ApprovalStatus.APPROVED))
                .collect(Collectors.toList());

        List<RoomStatusDto> result = new ArrayList<>();
        for (Room room : rooms) {
            // Tất cả active bookings cho room này hôm nay
            List<Booking> todayActive = bookingRepository.findOverlappingForRoom(
                    room.getId(), today, tomorrow, ACTIVE_STATUSES);

            int sumAllActive = bookingRepository.sumActiveQtyForRoom(room.getId(), ACTIVE_STATUSES);
            int platformQty = room.getAvailableQuantity() + sumAllActive;

            // ONLINE đang PENDING/CONFIRMED (chờ check-in)
            int heldOnline = todayActive.stream()
                    .filter(b -> b.getBookingSource() == null || b.getBookingSource() == BookingSource.ONLINE)
                    .filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED
                            || b.getBookingStatus() == BookingStatus.PENDING_ADMIN_APPROVAL)
                    .mapToInt(Booking::getRoomQuantity).sum();

            // ONLINE đang CHECKED_IN (chỉ online, để không đếm trùng với directOccupied)
            int checkedIn = todayActive.stream()
                    .filter(b -> b.getBookingSource() == null || b.getBookingSource() == BookingSource.ONLINE)
                    .filter(b -> b.getBookingStatus() == BookingStatus.CHECKED_IN)
                    .mapToInt(Booking::getRoomQuantity).sum();

            // DIRECT đang CONFIRMED hoặc CHECKED_IN (không tính cancelled)
            // Tách biệt khỏi checkedIn để tránh đếm trùng
            int directOccupied = todayActive.stream()
                    .filter(b -> b.getBookingSource() == BookingSource.DIRECT)
                    .filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED
                            || b.getBookingStatus() == BookingStatus.CHECKED_IN)
                    .mapToInt(Booking::getRoomQuantity).sum();

            // MANUAL_BLOCK đang CONFIRMED (chặn phòng / bảo trì)
            int blocked = todayActive.stream()
                    .filter(b -> b.getBookingSource() == BookingSource.MANUAL_BLOCK)
                    .filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED)
                    .mapToInt(Booking::getRoomQuantity).sum();

            // freeToday = quota - heldOnline - checkedInOnline - directOccupied - blocked
            boolean openForBooking = isRoomOpenForOnlineBooking(room);
            int freeToday = openForBooking
                    ? Math.max(0, platformQty - heldOnline - checkedIn - directOccupied - blocked)
                    : 0;

            result.add(new RoomStatusDto(
                    room.getId(), room.getRoomCode(), room.getRoomName(),
                    room.getAccommodation().getId(), room.getAccommodation().getName(),
                    room.getAccommodation().getPropertyType() != null
                            ? room.getAccommodation().getPropertyType().name() : "HOTEL",
                    platformQty, heldOnline, checkedIn, directOccupied, blocked, freeToday, openForBooking,
                    room.getPricePerNight()
            ));
        }

        result.sort((a, b) -> {
            if (a.freeToday == 0 && b.freeToday > 0) return -1;
            if (a.freeToday > 0 && b.freeToday == 0) return 1;
            return a.accommodationName.compareTo(b.accommodationName);
        });

        return result;
    }

    /** DTO cho tình trạng phòng hôm nay */
    public static class RoomStatusDto {
        public final Long roomId;
        public final String roomCode;
        public final String roomName;
        public final Long accommodationId;
        public final String accommodationName;
        public final String propertyType;
        public final int platformQuantity;
        public final int heldOnline;
        public final int checkedIn;
        public final int directOccupied;
        public final int blocked;
        public final int freeToday;
        public final boolean availableForBooking;
        public final java.math.BigDecimal pricePerNight;

        public RoomStatusDto(Long roomId, String roomCode, String roomName,
                             Long accommodationId, String accommodationName, String propertyType,
                             int platformQuantity, int heldOnline, int checkedIn,
                             int directOccupied, int blocked, int freeToday, boolean availableForBooking,
                             java.math.BigDecimal pricePerNight) {
            this.roomId = roomId;
            this.roomCode = roomCode;
            this.roomName = roomName;
            this.accommodationId = accommodationId;
            this.accommodationName = accommodationName;
            this.propertyType = propertyType;
            this.platformQuantity = platformQuantity;
            this.heldOnline = heldOnline;
            this.checkedIn = checkedIn;
            this.directOccupied = directOccupied;
            this.blocked = blocked;
            this.freeToday = freeToday;
            this.availableForBooking = availableForBooking;
            this.pricePerNight = pricePerNight;
        }

        public String getStatusLabel() {
            if (!availableForBooking) return "STOPPED";
            if (freeToday == 0) return "FULL";
            if (freeToday <= Math.ceil(platformQuantity * 0.3)) return "LIMITED";
            return "AVAILABLE";
        }
    }

    /** Thống kê tóm tắt — dùng cho cards ở đầu trang */
    public AvailabilitySummary buildSummary(List<RoomAvailabilityDto> list) {
        int total     = list.size();
        int full      = (int) list.stream().filter(r -> "FULL".equals(r.getStatus())).count();
        int limited   = (int) list.stream().filter(r -> "LIMITED".equals(r.getStatus())).count();
        int available = (int) list.stream().filter(r -> "AVAILABLE".equals(r.getStatus())).count();
        int totalRooms  = list.stream().mapToInt(RoomAvailabilityDto::getTotalQuantity).sum();
        int occupiedRooms = list.stream().mapToInt(RoomAvailabilityDto::getOccupiedQuantity).sum();
        int freeRooms  = list.stream().mapToInt(RoomAvailabilityDto::getAvailableQuantity).sum();
        return new AvailabilitySummary(total, full, limited, available, totalRooms, occupiedRooms, freeRooms);
    }

    /** Tóm tắt thống kê tình trạng phòng */
    public static class AvailabilitySummary {
        public final int roomTypes;
        public final int fullTypes;
        public final int limitedTypes;
        public final int availableTypes;
        public final int totalRooms;
        public final int occupiedRooms;
        public final int freeRooms;

        public AvailabilitySummary(int roomTypes, int fullTypes, int limitedTypes, int availableTypes,
                                   int totalRooms, int occupiedRooms, int freeRooms) {
            this.roomTypes     = roomTypes;
            this.fullTypes     = fullTypes;
            this.limitedTypes  = limitedTypes;
            this.availableTypes = availableTypes;
            this.totalRooms    = totalRooms;
            this.occupiedRooms = occupiedRooms;
            this.freeRooms     = freeRooms;
        }
    }
}
