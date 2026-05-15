package com.travelmate.repository;

import com.travelmate.entity.PartnerSettlement;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * PartnerSettlementRepository — Giao tiếp với bảng `partner_settlements`.
 */
@Repository
public interface PartnerSettlementRepository extends JpaRepository<PartnerSettlement, Long> {

    /** Lấy tất cả settlement, mới nhất trước — admin xem toàn bộ */
    List<PartnerSettlement> findAllByOrderByCreatedAtDesc();

    /** Lấy settlement của 1 partner — partner chỉ thấy của mình */
    List<PartnerSettlement> findByPartnerOrderByCreatedAtDesc(User partner);

    /**
     * Kiểm tra đã có settlement cho partner trong kỳ này chưa.
     * Dùng để tránh tạo trùng khi admin bấm Generate nhiều lần.
     */
    boolean existsByPartnerAndPeriodStartAndPeriodEnd(User partner, LocalDate periodStart, LocalDate periodEnd);

    /** Đếm theo trạng thái — admin dashboard */
    long countBySettlementStatus(SettlementStatus status);
}
