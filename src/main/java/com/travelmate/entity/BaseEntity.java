package com.travelmate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * BaseEntity - Entity cha dung chung cho tat ca cac entity trong he thong.
 *
 * Muc dich: tranh lap lai cac field chung nhu id, createdAt, updatedAt
 * trong moi entity. Cac entity khac chi can ke thua (extends) BaseEntity.
 *
 * Annotation giai thich:
 * - @MappedSuperclass: danh dau day la class cha, KHONG tao bang rieng trong DB,
 *   nhung cac field cua no se duoc ke thua xuong cac entity con.
 * - @Getter/@Setter: Lombok tu dong tao getter/setter cho tat ca field
 * - @Id: danh dau field la khoa chinh (primary key)
 * - @GeneratedValue(IDENTITY): MySQL tu dong tang id (auto_increment)
 * - @CreationTimestamp: Hibernate tu dong set thoi gian khi tao ban ghi
 * - @UpdateTimestamp: Hibernate tu dong cap nhat thoi gian khi sua ban ghi
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    /**
     * Khoa chinh, tu dong tang trong MySQL (auto_increment).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Thoi gian tao ban ghi.
     * Tu dong duoc set boi Hibernate khi INSERT, khong can set thu cong.
     * updatable = false: khong cho phep cap nhat lai sau khi da tao.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thoi gian cap nhat lan cuoi.
     * Tu dong duoc set boi Hibernate moi khi UPDATE.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
