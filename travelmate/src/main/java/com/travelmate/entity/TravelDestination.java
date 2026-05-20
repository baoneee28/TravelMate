package com.travelmate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "travel_destinations",
       indexes = {
           @Index(name = "idx_td_region_active_order", columnList = "region,active,display_order")
       })
@Getter
@Setter
@NoArgsConstructor
public class TravelDestination {

    public enum Region {
        NORTH, CENTRAL, SOUTH
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Region region;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "short_description", length = 255)
    private String shortDescription;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
