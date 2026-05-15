package com.travelmate.repository;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.ApprovalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import java.util.List;

/**
 * RoomRepository — Giao tiếp với bảng `rooms`.
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    /**
     * Tìm các phòng còn trống VÀ đã được admin duyệt của 1 khách sạn.
     * Dùng cho User: chỉ thấy APPROVED rooms khi xem chi tiết khách sạn.
     */
    List<Room> findByAccommodationAndAvailableQuantityGreaterThanAndApprovalStatus(
            Accommodation accommodation, int minQuantity, ApprovalStatus status);

    /** Tìm tất cả phòng thuộc 1 khách sạn (bao gồm mọi trạng thái) */
    List<Room> findByAccommodation(Accommodation accommodation);

    /** Kiểm tra roomCode đã tồn tại chưa — validate trước khi tạo phòng mới */
    boolean existsByRoomCode(String roomCode);

    /** Lấy tất cả phòng cho Admin, mới nhất trước — JOIN FETCH để tránh LazyInitializationException khi Thymeleaf render */
    @Query("SELECT r FROM Room r JOIN FETCH r.accommodation ORDER BY r.id DESC")
    List<Room> findAllWithAccommodationOrderByIdDesc();

    /** Lấy tất cả phòng cho Admin, mới nhất trước */
    List<Room> findAllByOrderByIdDesc();

    /** Lọc phòng theo trạng thái duyệt — Admin dùng để xem queue PENDING */
    List<Room> findByApprovalStatus(ApprovalStatus status);

    /** Đếm phòng theo trạng thái — Admin dashboard stats */
    long countByApprovalStatus(ApprovalStatus status);

    /** Tìm phòng chưa có approvalStatus (backfill) */
    List<Room> findByApprovalStatusIsNull();

    /** Lấy tất cả phòng APPROVED thuộc partner (qua accommodation.owner) — dùng cho form tạo voucher theo phòng */
    List<Room> findByAccommodation_OwnerAndApprovalStatus(User owner, ApprovalStatus status);

    /** Tìm tất cả phòng của 1 accommodation theo trạng thái duyệt — dùng cho availability check theo ngày */
    List<Room> findByAccommodationAndApprovalStatus(Accommodation accommodation, ApprovalStatus status);

    /**
     * Lấy Room với PESSIMISTIC_WRITE lock để chống overbooking đồng thời.
     * Dùng trong BookingService.createBooking() bên trong @Transactional.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
}


