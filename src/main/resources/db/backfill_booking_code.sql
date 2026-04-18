-- =============================================================
-- BACKFILL booking_code cho các booking cũ trong DB
-- Chạy script này 1 lần sau khi restart app (Hibernate đã tạo column)

USE travelmate_db;
--
-- Format: TM{yyyyMMdd}-{id padded 3 chữ số}
-- Ví dụ: TM20260418-001
-- =============================================================

-- Bước 1: Hibernate đã tự tạo column booking_code (nullable) khi restart.
--          Khi đó tất cả booking cũ có booking_code = NULL.

-- Bước 2: Backfill cho booking cũ
UPDATE bookings
SET booking_code = CONCAT(
    'TM',
    DATE_FORMAT(created_at, '%Y%m%d'),
    '-',
    LPAD(id, 3, '0')
)
WHERE booking_code IS NULL;

-- Bước 3: Kiểm tra kết quả
SELECT id, booking_code, created_at FROM bookings ORDER BY id;
