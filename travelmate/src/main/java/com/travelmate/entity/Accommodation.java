package com.travelmate.entity;

import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.PropertyType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Accommodation — Entity ánh xạ bảng `accommodations` trong MySQL.
 *
 * Đại diện cho 1 nơi lưu trú (khách sạn, villa, homestay, resort).
 * Hiện tại task này chỉ dùng propertyType = HOTEL.
 *
 * Quan hệ:
 *   - 1 Accommodation có nhiều Room (OneToMany)
 *   - 1 Accommodation thuộc về 1 Partner (ManyToOne → User có role PARTNER)
 */
@Entity
@Table(name = "accommodations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Accommodation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tên khách sạn / nơi lưu trú */
    @Column(nullable = false, length = 200)
    private String name;

    /** Địa chỉ chi tiết */
    @Column(length = 500)
    private String address;

    /** Thành phố — dùng để search/filter */
    @Column(nullable = false, length = 100)
    private String city;

    /** Mô tả chi tiết về nơi lưu trú */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Loại hình: HOTEL, VILLA, HOMESTAY, RESORT */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PropertyType propertyType;

    /** Trạng thái duyệt: PENDING → APPROVED / REJECTED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    /** URL ảnh đại diện (thumbnail) */
    @Column(length = 500)
    private String thumbnailUrl;

    /** Điểm đánh giá trung bình (1.0 – 10.0) */
    @Column
    private Double rating;

    /** Số lượt đánh giá */
    @Column
    private Integer reviewCount = 0;

    /** Trường legacy cho dữ liệu cũ / import seed; UI hiện dùng điểm review 1-10. */
    @Column
    private Integer starRating;

    /**
     * Giá phòng thấp nhất (tính tự động từ DB bằng Hibernate @Formula).
     * Dùng để hiển thị giá trên trang danh sách mà KHÔNG cần load lazy collection rooms.
     * Không ánh xạ vào cột DB → chỉ là derived value khi SELECT.
     */
    @Formula("(SELECT MIN(r.price_per_night) FROM rooms r WHERE r.accommodation_id = id AND r.approval_status = 'APPROVED' AND r.available_for_booking = true)")
    private Double minPrice;

    /**
     * Partner sở hữu nơi lưu trú này.
     * Dùng để kiểm tra ownership: partner chỉ xem booking thuộc accommodation của mình.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    /**
     * Danh sách phòng thuộc khách sạn này.
     * cascade ALL: khi lưu/xóa Accommodation → tự động lưu/xóa Room.
     * orphanRemoval: khi remove Room khỏi list → xóa luôn trong DB.
     */
    @OneToMany(mappedBy = "accommodation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Room> rooms = new ArrayList<>();

    /** Thời gian tạo */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Thời gian cập nhật */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
