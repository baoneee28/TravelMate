package com.travelmate.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử logic overlap khoảng ngày dùng trong anti-overbooking.
 *
 * Công thức overlap chuẩn:
 *   existing.checkIn < requested.checkOut AND existing.checkOut > requested.checkIn
 *
 * Không kết nối database — kiểm thử thuần logic (pure unit test).
 *
 * @see com.travelmate.repository.BookingRepository#sumOverlappingQtyForRoom
 */
@DisplayName("Availability — Overlap khoảng ngày đặt phòng")
class AvailabilityOverlapTest {

    /**
     * Kiểm tra overlap giữa 2 khoảng ngày.
     * Sử dụng đúng điều kiện JPQL trong BookingRepository:
     *   b.checkIn < :checkOut AND b.checkOut > :checkIn
     */
    private boolean isOverlapping(LocalDate existCheckIn, LocalDate existCheckOut,
                                  LocalDate requestCheckIn, LocalDate requestCheckOut) {
        return existCheckIn.isBefore(requestCheckOut) && existCheckOut.isAfter(requestCheckIn);
    }

    // ─── Overlap cases ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Có overlap — phải chặn đặt phòng")
    class OverlapCases {

        @Test
        @DisplayName("Overlap chéo: existing(1-5) vs requested(3-7) → overlap 2 ngày")
        void overlap_partialRight() {
            boolean result = isOverlapping(
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5),
                    LocalDate.of(2026, 6, 3), LocalDate.of(2026, 6, 7));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Overlap chéo trái: existing(3-7) vs requested(1-5) → overlap 2 ngày")
        void overlap_partialLeft() {
            boolean result = isOverlapping(
                    LocalDate.of(2026, 6, 3), LocalDate.of(2026, 6, 7),
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Requested nằm hoàn toàn bên trong existing: existing(1-10) vs requested(3-5)")
        void overlap_containedWithin() {
            boolean result = isOverlapping(
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10),
                    LocalDate.of(2026, 6, 3), LocalDate.of(2026, 6, 5));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Existing nằm hoàn toàn bên trong requested: existing(3-5) vs requested(1-10)")
        void overlap_existingInsideRequested() {
            boolean result = isOverlapping(
                    LocalDate.of(2026, 6, 3), LocalDate.of(2026, 6, 5),
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Trùng hoàn toàn: existing(1-5) vs requested(1-5)")
        void overlap_exactSameDates() {
            boolean result = isOverlapping(
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5),
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5));

            assertThat(result).isTrue();
        }
    }

    // ─── No overlap cases ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Không overlap — cho phép đặt phòng")
    class NoOverlapCases {

        @Test
        @DisplayName("Tiếp nối đúng: existing(1-5) vs requested(5-7) → checkOut == checkIn → NO overlap")
        void noOverlap_checkOutEqualsCheckIn() {
            // Ngày 5 là ngày checkout (trả phòng), khách mới checkIn ngày 5 → không trùng
            boolean result = isOverlapping(
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5),
                    LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 7));

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Hoàn toàn trước: existing(1-3) vs requested(5-7) → NO overlap")
        void noOverlap_completelyBefore() {
            boolean result = isOverlapping(
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3),
                    LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 7));

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Hoàn toàn sau: existing(5-7) vs requested(1-3) → NO overlap")
        void noOverlap_completelyAfter() {
            boolean result = isOverlapping(
                    LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 7),
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3));

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Tiếp nối ngược: existing(5-7) vs requested(1-5) → checkOut == checkIn → NO overlap")
        void noOverlap_reverseAdjacent() {
            boolean result = isOverlapping(
                    LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 7),
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5));

            assertThat(result).isFalse();
        }
    }
}
