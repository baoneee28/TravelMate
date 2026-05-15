package com.travelmate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "travel_posts",
       indexes = {
           @Index(name = "idx_tp_category", columnList = "category"),
           @Index(name = "idx_tp_status",   columnList = "status")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TravelPost {

    public enum Category {
        GUIDE, ATTRACTION, ESSENTIAL;

        public String getDisplayName() {
            return switch (this) {
                case GUIDE      -> "Cẩm nang";
                case ATTRACTION -> "Địa điểm tham quan";
                case ESSENTIAL  -> "Cần thiết";
            };
        }
    }

    public enum Status { VISIBLE, HIDDEN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category = Category.GUIDE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.VISIBLE;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
