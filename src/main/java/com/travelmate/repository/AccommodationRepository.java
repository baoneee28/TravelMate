package com.travelmate.repository;

import com.travelmate.entity.Accommodation;
import com.travelmate.enums.ApprovalStatus;
import com.travelmate.enums.PropertyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác với bảng "accommodations".
 * Spring Data JPA tự tạo implementation khi app khởi động.
 */
@Repository
public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

    /**
     * Tìm accommodation theo id VÀ trạng thái duyệt.
     *
     * Dùng cho trang chi tiết: chỉ cho USER xem listing đã APPROVED.
     * Nếu listing PENDING hoặc REJECTED → trả về Optional.empty() → 404.
     *
     * Tại sao không dùng findById() rồi check? Vì gộp 2 điều kiện vào 1 query
     * gọn hơn, ít lỗi hơn.
     */
    Optional<Accommodation> findByIdAndApprovalStatus(Long id, ApprovalStatus approvalStatus);

    /**
     * Tìm kiếm danh sách accommodation APPROVED với filter tuỳ chọn.
     *
     * JPQL query (Java Persistence Query Language) — viết bằng tên class/field Java,
     * không phải tên bảng/cột SQL thô.
     *
     * Cách hoạt động của filter tuỳ chọn:
     *   :city IS NULL OR ...  →  nếu không truyền city → bỏ qua filter city
     *   LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%'))  →  tìm không phân biệt hoa/thường
     *   :propertyType IS NULL OR ...  →  tương tự, không truyền → bỏ qua filter type
     *
     * ORDER BY a.createdAt DESC: mới nhất hiện trước.
     */
    @Query("""
            SELECT a FROM Accommodation a
            WHERE a.approvalStatus = 'APPROVED'
              AND (:city         IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%')))
              AND (:propertyType IS NULL OR a.propertyType = :propertyType)
            ORDER BY a.createdAt DESC
            """)
    List<Accommodation> searchApproved(
            @Param("city")         String       city,
            @Param("propertyType") PropertyType propertyType
    );

    /**
     * Đếm số listing của một partner theo trạng thái.
     * Dùng cho partner dashboard (phase sau).
     */
    long countByPartnerIdAndApprovalStatus(Long partnerId, ApprovalStatus approvalStatus);
}
