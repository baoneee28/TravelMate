package com.travelmate.entity;

import com.travelmate.enums.ApprovalStatus;
import com.travelmate.enums.PropertyType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entity Accommodation - ánh xạ vào bảng "accommodations" trong MySQL.
 *
 * Đây là entity trung tâm của TravelMate:
 * - PARTNER tạo listing (nơi lưu trú)
 * - ADMIN duyệt listing (PENDING → APPROVED/REJECTED)
 * - USER tìm kiếm và đặt chỗ tại listing đã APPROVED
 *
 * Kế thừa BaseEntity để có sẵn: id, createdAt, updatedAt
 */
@Entity
@Table(name = "accommodations", indexes = {
        @Index(name = "idx_accom_city",            columnList = "city"),
        @Index(name = "idx_accom_approval_status", columnList = "approval_status"),
        @Index(name = "idx_accom_property_type",   columnList = "property_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Accommodation extends BaseEntity {

    /**
     * PARTNER sở hữu listing này.
     * - @ManyToOne: nhiều accommodation có thể thuộc 1 partner
     * - FetchType.LAZY: chỉ load dữ liệu partner khi thực sự cần (tiết kiệm query)
     * - @JoinColumn: cột partner_id trong bảng accommodations
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private User partner;

    /**
     * Tên nơi lưu trú.
     * Ví dụ: "Khách Sạn Bình Minh Đà Nẵng"
     */
    @NotBlank(message = "Tên nơi lưu trú không được để trống")
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Loại hình lưu trú: HOTEL | HOMESTAY | VILLA | APARTMENT
     * Lưu dạng String trong DB để dễ đọc (EnumType.STRING)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 20)
    private PropertyType propertyType;

    /**
     * Mô tả chi tiết về nơi lưu trú.
     * Dùng TEXT trong DB vì nội dung có thể dài.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Địa chỉ cụ thể.
     * Ví dụ: "123 Trần Phú, Hải Châu"
     */
    @Column(length = 300)
    private String address;

    /**
     * Thành phố / tỉnh của nơi lưu trú.
     * Dùng để filter tìm kiếm theo địa điểm.
     * Ví dụ: "Đà Nẵng", "Hà Nội", "TP. Hồ Chí Minh"
     */
    @NotBlank(message = "Thành phố không được để trống")
    @Column(nullable = false, length = 100)
    private String city;

    /**
     * Giá mỗi đêm (đơn vị: VNĐ).
     * Dùng BigDecimal để đảm bảo tính chính xác cho phép tính tiền.
     * TRÁNH dùng double/float cho tiền tệ vì có thể gây sai số làm tròn.
     */
    @NotNull
    @Positive(message = "Giá phải lớn hơn 0")
    @Column(name = "price_per_night", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerNight;

    /**
     * Sức chứa tối đa (số khách).
     * Dùng để filter khi user nhập số người.
     */
    @NotNull
    @Column(name = "max_guests", nullable = false)
    private Integer maxGuests;

    /**
     * Trạng thái duyệt listing:
     * - PENDING: mới tạo, chờ admin duyệt (mặc định)
     * - APPROVED: đã duyệt, USER thấy được
     * - REJECTED: bị từ chối, partner có thể chỉnh sửa và gửi lại
     *
     * @Builder.Default: cần thiết khi dùng Lombok @Builder để đặt giá trị mặc định
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    /**
     * Đường dẫn ảnh đại diện.
     * Có thể là đường dẫn local (/assets/images/...) hoặc URL bên ngoài.
     */
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;
}
