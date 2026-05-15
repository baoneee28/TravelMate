package com.travelmate.repository;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
import com.travelmate.entity.Voucher;
import com.travelmate.entity.enums.VoucherScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * VoucherRepository — Giao tiếp với bảng `vouchers`.
 */
@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    /** Tìm voucher theo mã — dùng khi user nhập code để đặt phòng */
    Optional<Voucher> findByCode(String code);

    /** Lấy tất cả voucher theo phạm vi — admin xem USER_GLOBAL */
    List<Voucher> findByVoucherScopeOrderByCreatedAtDesc(VoucherScope scope);

    /** Lấy voucher do 1 partner tạo (owner) */
    List<Voucher> findByOwnerOrderByCreatedAtDesc(User owner);

    /** Lấy voucher gắn với 1 accommodation cụ thể */
    List<Voucher> findByAccommodationOrderByCreatedAtDesc(Accommodation accommodation);

    /** Lấy voucher gắn với 1 room cụ thể */
    List<Voucher> findByRoomOrderByCreatedAtDesc(Room room);

    /** Lấy tất cả, mới nhất trước — admin xem toàn bộ */
    List<Voucher> findAllByOrderByCreatedAtDesc();
}
