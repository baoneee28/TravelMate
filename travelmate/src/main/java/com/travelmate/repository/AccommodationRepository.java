package com.travelmate.repository;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.PropertyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AccommodationRepository — Giao tiếp với bảng `accommodations`.
 *
 * Spring Data JPA tự generate SQL dựa trên tên method.
 */
@Repository
public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

    /**
     * Tìm nơi lưu trú theo loại hình và trạng thái duyệt.
     * VD: tìm tất cả HOTEL đã APPROVED.
     */
    List<Accommodation> findByPropertyTypeAndApprovalStatus(
            PropertyType propertyType, ApprovalStatus approvalStatus);

    /**
     * Tìm nơi lưu trú theo loại hình, trạng thái duyệt, và keyword (tên hoặc thành phố).
     * Dùng cho search bar: user nhập "Đà Lạt" → tìm hotel ở Đà Lạt.
     */
    List<Accommodation> findByPropertyTypeAndApprovalStatusAndCityContainingIgnoreCase(
            PropertyType propertyType, ApprovalStatus approvalStatus, String city);

    /**
     * Tìm nơi lưu trú theo loại hình, trạng thái, và keyword trong tên HOẶC thành phố.
     * Mở rộng search rộng hơn.
     */
    List<Accommodation> findByPropertyTypeAndApprovalStatusAndNameContainingIgnoreCaseOrPropertyTypeAndApprovalStatusAndCityContainingIgnoreCase(
            PropertyType type1, ApprovalStatus status1, String name,
            PropertyType type2, ApprovalStatus status2, String city);

    /** Đếm số nơi lưu trú theo loại hình — dùng kiểm tra dữ liệu khởi tạo */
    long countByPropertyType(PropertyType propertyType);

    boolean existsByNameIgnoreCase(String name);

    Optional<Accommodation> findFirstByNameIgnoreCase(String name);

    /** Đếm accommodation theo trạng thái duyệt — dùng cho admin dashboard */
    long countByApprovalStatus(ApprovalStatus approvalStatus);

    /** Tìm accommodation thuộc 1 partner — dùng cho partner page */
    List<Accommodation> findByOwner(User owner);

    /** Tìm accommodation thuộc 1 partner theo approvalStatus — lọc theo trạng thái duyệt */
    List<Accommodation> findByOwnerAndApprovalStatus(User owner, ApprovalStatus approvalStatus);

    /** Tìm accommodation theo approvalStatus — admin xem danh sách PENDING */
    List<Accommodation> findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus approvalStatus);

    /** Tìm tất cả accommodation, sắp xếp theo ngày tạo — admin xem toàn bộ */
    List<Accommodation> findAllByOrderByCreatedAtDesc();

    /** Tìm accommodation chưa có owner — dùng cho backfill DataInitializer (legacy) */
    List<Accommodation> findByOwnerIsNull();

    /** Tìm accommodation chưa có owner THEO loại hình — backfill an toàn, chỉ gán đúng type */
    List<Accommodation> findByOwnerIsNullAndPropertyType(PropertyType propertyType);

    /** Tìm accommodation theo ID và owner — kiểm tra ownership an toàn */
    Optional<Accommodation> findByIdAndOwner(Long id, User owner);
}
