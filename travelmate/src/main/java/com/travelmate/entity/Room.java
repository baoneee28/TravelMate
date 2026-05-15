package com.travelmate.entity;

import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.RoomCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Room — Entity ánh xạ bảng `rooms` trong MySQL.
 *
 * Đại diện cho 1 loại phòng trong khách sạn.
 * Ví dụ: Standard King Room, Deluxe Double Room, Family Room, Suite Room.
 *
 * Quan hệ:
 *   - Nhiều Room thuộc 1 Accommodation (ManyToOne)
 *
 * Lưu ý:
 *   - roomCode dùng để sinh bookingCode: BK-<roomCode>-<sequence>
 *   - availableQuantity giảm tạm khi user đặt (đơn giản hóa cho đồ án)
 */
@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã phòng — dùng để sinh booking code.
     * VD: R101, R202, R301, R401
     */
    @Column(nullable = false, unique = true, length = 20)
    private String roomCode;

    /** Tên loại phòng hiển thị cho user */
    @Column(nullable = false, length = 100)
    private String roomName;

    /** Loại giường: "1 giường King", "2 giường đơn", v.v. */
    @Column(length = 100)
    private String bedType;

    /** Sức chứa tối đa (số người) */
    @Column(nullable = false)
    private Integer capacity;

    /**
     * Giá mỗi đêm (VND).
     * Dùng BigDecimal để tính toán chính xác, tránh lỗi floating point.
     */
    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal pricePerNight;

    /**
     * Số phòng còn trống.
     * ⚠️ Đơn giản hóa cho đồ án: giảm trực tiếp khi user đặt phòng.
     * Production thực tế cần cơ chế lock/reserve phức tạp hơn.
     */
    @Column(nullable = false)
    private Integer availableQuantity;

    /** URL ảnh phòng (placeholder trước, ảnh thật sau) */
    @Column(length = 500)
    private String imageUrl;

    /** Mô tả ngắn về phòng */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Trạng thái duyệt phòng.
     * - PENDING  : Partner vừa tạo, chờ Admin duyệt.
     * - APPROVED : Admin đã duyệt, User có thể đặt.
     * - REJECTED : Admin từ chối, Partner thấy badge "Từ chối".
     *
     * Luồng: Partner tạo phòng → PENDING → Admin duyệt → APPROVED → User thấy.
     * Phòng từ SQL seed / DataInitializer được backfill = APPROVED tự động.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 50)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    /**
     * Loại phòng trong cơ sở lưu trú.
     * VD: STANDARD, DELUXE, FAMILY, VIP, SUITE, OTHER
     *
     * Dùng để phân biệt commission riêng nếu commissionRateOverride != null.
     * Mặc định STANDARD nếu không chọn.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "room_category", length = 30)
    private RoomCategory roomCategory = RoomCategory.STANDARD;

    /**
     * Tỷ lệ hoa hồng riêng cho phòng này (đơn vị %).
     *
     * - null    : dùng commission mặc định theo PropertyType của Accommodation.
     * - có giá trị: dùng rate này thay vì default.
     *
     * Ví dụ:
     *   STANDARD → null → HOTEL 15%
     *   DELUXE   → 16.00 → 16%
     *   VIP      → 18.00 → 18%
     *   SUITE    → 20.00 → 20%
     *
     * ⚠️ DB lưu theo %, ví dụ: 18.00 nghĩa là 18%.
     * Khi tính: commissionAmount = gross × (commissionRateOverride / 100)
     */
    @Column(name = "commission_rate_override", precision = 5, scale = 2)
    private BigDecimal commissionRateOverride;

    /**
     * Khách sạn chứa phòng này.
     * LAZY: chỉ load khi cần (tối ưu performance).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    /**
     * Tiện nghi của phòng/căn.
     * EAGER: load ngay khi lấy Room — Thymeleaf template dùng trực tiếp không cần session.
     * Bảng join: room_amenities (room_id, amenity_id).
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "room_amenities",
        joinColumns = @JoinColumn(name = "room_id"),
        inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    private List<Amenity> amenities = new ArrayList<>();
}
