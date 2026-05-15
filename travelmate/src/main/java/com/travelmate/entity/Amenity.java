package com.travelmate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Amenity — Tiện nghi phòng/căn lưu trú.
 *
 * Ví dụ: WiFi miễn phí, Máy lạnh, Hồ bơi riêng, Ban công, View biển...
 * Quan hệ: nhiều Room ↔ nhiều Amenity (ManyToMany qua bảng room_amenities).
 */
@Entity
@Table(name = "amenities")
@Getter
@Setter
@NoArgsConstructor
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tên tiện nghi hiển thị cho user */
    @Column(nullable = false, length = 100)
    private String name;

    /** Emoji icon hiển thị kèm tên */
    @Column(length = 20)
    private String icon;

    /** Nhóm tiện nghi: Tiện ích chung | Phòng ngủ | Phòng tắm | Bếp & Ăn uống | Đặc biệt */
    @Column(length = 50)
    private String category;

    public Amenity(String name, String icon, String category) {
        this.name = name;
        this.icon = icon;
        this.category = category;
    }
}
