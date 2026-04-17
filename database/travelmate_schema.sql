-- =============================================================================
-- TRAVELMATE — DATABASE SCHEMA & SEED DATA
-- Stack : Spring Boot MVC + Thymeleaf + MySQL
-- File  : database/travelmate_schema.sql
-- Mục đích:
--   1. Tài liệu hoá toàn bộ schema DB (dùng khi báo cáo)
--   2. Chạy tay để tạo bảng + seed data demo nếu cần reset DB
--   3. Mỗi lần thêm bảng / cột mới → cập nhật file này
--
-- Cách chạy:
--   mysql -u root -p < database/travelmate_schema.sql
--   Hoặc mở trong MySQL Workbench → chọn tất cả → Execute
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 0. KHỞI TẠO DATABASE
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS travelmate_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE travelmate_db;

-- Tắt kiểm tra FK tạm thời để DROP / INSERT không lỗi thứ tự
SET FOREIGN_KEY_CHECKS = 0;


-- =============================================================================
-- PHẦN 1 — SCHEMA (CẤU TRÚC BẢNG)
-- Hibernate ddl-auto=update sẽ tự tạo bảng khi app khởi động.
-- File này là bản ghi tay để tham khảo và chạy thủ công khi cần.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- BẢNG 1: users
-- Hibernate tự tạo. Ghi lại ở đây để tham khảo.
-- Entity: com.travelmate.entity.User
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT          NOT NULL AUTO_INCREMENT   COMMENT 'Khoá chính tự tăng',
    full_name   VARCHAR(100)    NOT NULL                  COMMENT 'Họ và tên đầy đủ',
    email       VARCHAR(150)    NOT NULL                  COMMENT 'Email đăng nhập — unique',
    password    TEXT            NOT NULL                  COMMENT 'Mật khẩu mã hoá BCrypt',
    phone       VARCHAR(20)     NULL                      COMMENT 'Số điện thoại (tuỳ chọn)',
    role        VARCHAR(20)     NOT NULL                  COMMENT 'USER | ADMIN | PARTNER',
    status      VARCHAR(20)     NOT NULL                  COMMENT 'ACTIVE | INACTIVE | BLOCKED',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tài khoản người dùng — 3 role: USER / ADMIN / PARTNER';


-- -----------------------------------------------------------------------------
-- BẢNG 2: accommodations  (Nơi lưu trú / Listing)
-- Entity: com.travelmate.entity.Accommodation  ← CẦN TẠO MỚI
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS accommodations (
    id              BIGINT          NOT NULL AUTO_INCREMENT   COMMENT 'Khoá chính',
    partner_id      BIGINT          NOT NULL                  COMMENT 'FK → users.id (PARTNER sở hữu listing)',
    name            VARCHAR(200)    NOT NULL                  COMMENT 'Tên nơi lưu trú',
    property_type   VARCHAR(20)     NOT NULL                  COMMENT 'HOTEL | HOMESTAY | VILLA | APARTMENT',
    description     TEXT            NULL                      COMMENT 'Mô tả chi tiết',
    address         VARCHAR(300)    NULL                      COMMENT 'Địa chỉ cụ thể',
    city            VARCHAR(100)    NOT NULL                  COMMENT 'Thành phố / tỉnh',
    price_per_night DECIMAL(12,2)   NOT NULL                  COMMENT 'Giá mỗi đêm (VNĐ)',
    max_guests      INT             NOT NULL DEFAULT 2        COMMENT 'Số khách tối đa',
    approval_status VARCHAR(20)     NOT NULL DEFAULT 'PENDING'COMMENT 'PENDING | APPROVED | REJECTED',
    thumbnail_url   VARCHAR(500)    NULL                      COMMENT 'Đường dẫn ảnh đại diện',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_accommodation_partner
        FOREIGN KEY (partner_id) REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_accom_city            (city),
    INDEX idx_accom_approval_status (approval_status),
    INDEX idx_accom_property_type   (property_type),
    INDEX idx_accom_price           (price_per_night)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Listing nơi lưu trú do PARTNER tạo, ADMIN duyệt, USER xem';


-- -----------------------------------------------------------------------------
-- BẢNG 3: bookings  (Đặt chỗ)
-- Entity: com.travelmate.entity.Booking  ← CẦN TẠO MỚI
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS bookings (
    id               BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'Khoá chính',
    user_id          BIGINT          NOT NULL                 COMMENT 'FK → users.id (USER đặt phòng)',
    accommodation_id BIGINT          NOT NULL                 COMMENT 'FK → accommodations.id',
    check_in         DATE            NOT NULL                 COMMENT 'Ngày nhận phòng',
    check_out        DATE            NOT NULL                 COMMENT 'Ngày trả phòng',
    num_adults       INT             NOT NULL DEFAULT 1       COMMENT 'Số người lớn',
    num_children     INT             NOT NULL DEFAULT 0       COMMENT 'Số trẻ em',
    total_price      DECIMAL(12,2)   NOT NULL                 COMMENT 'Tổng tiền = price_per_night × số đêm',
    booking_status   VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                                                              COMMENT 'PENDING | CONFIRMED | CANCELLED | COMPLETED',
    notes            TEXT            NULL                     COMMENT 'Ghi chú của khách (tuỳ chọn)',
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_booking_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_booking_accommodation
        FOREIGN KEY (accommodation_id) REFERENCES accommodations(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    -- Ràng buộc check_out phải sau check_in (MySQL 8+)
    CONSTRAINT chk_booking_dates CHECK (check_out > check_in),
    INDEX idx_booking_user          (user_id),
    INDEX idx_booking_accommodation (accommodation_id),
    INDEX idx_booking_status        (booking_status),
    INDEX idx_booking_checkin       (check_in)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Lịch đặt phòng của USER — trạng thái: PENDING→CONFIRMED→COMPLETED';


-- -----------------------------------------------------------------------------
-- BẢNG 4: payments  (Thanh toán)
-- Entity: com.travelmate.entity.Payment  ← CẦN TẠO MỚI
-- Ghi chú: 1 booking chỉ có đúng 1 bản ghi payment (UNIQUE booking_id)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payments (
    id               BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'Khoá chính',
    booking_id       BIGINT          NOT NULL                 COMMENT 'FK → bookings.id (1-1)',
    amount           DECIMAL(12,2)   NOT NULL                 COMMENT 'Số tiền cần thanh toán',
    payment_method   VARCHAR(20)     NULL                     COMMENT 'VNPAY | MOMO | QR_INVOICE | CASH',
    payment_status   VARCHAR(20)     NOT NULL DEFAULT 'UNPAID'COMMENT 'UNPAID | PAID | FAILED | REFUNDED',
    transaction_code VARCHAR(100)    NULL                     COMMENT 'Mã giao dịch từ cổng thanh toán / mock',
    paid_at          DATETIME        NULL                     COMMENT 'Thời điểm thanh toán thành công',
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- 1 booking chỉ có đúng 1 payment record
    UNIQUE KEY uk_payment_booking (booking_id),
    CONSTRAINT fk_payment_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_payment_status (payment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Thông tin thanh toán cho mỗi booking — mock flow cho đồ án';


-- -----------------------------------------------------------------------------
-- BẢNG 5: reviews  (Đánh giá sau trải nghiệm)
-- Entity: com.travelmate.entity.Review  ← CẦN TẠO MỚI
-- Ràng buộc nghiệp vụ: chỉ review khi booking.status = COMPLETED
--   → Kiểm tra ở tầng Service, không dùng DB constraint vì cần đọc status
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reviews (
    id               BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'Khoá chính',
    user_id          BIGINT          NOT NULL                 COMMENT 'FK → users.id',
    accommodation_id BIGINT          NOT NULL                 COMMENT 'FK → accommodations.id',
    booking_id       BIGINT          NOT NULL                 COMMENT 'FK → bookings.id (bắt buộc có booking)',
    rating           INT             NOT NULL                 COMMENT 'Số sao: 1 đến 5',
    comment          TEXT            NULL                     COMMENT 'Nội dung đánh giá (tuỳ chọn)',
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- 1 booking chỉ được review đúng 1 lần
    UNIQUE KEY uk_review_booking    (booking_id),
    CONSTRAINT fk_review_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_review_accommodation
        FOREIGN KEY (accommodation_id) REFERENCES accommodations(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_review_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5),
    INDEX idx_review_accommodation  (accommodation_id),
    INDEX idx_review_user           (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Đánh giá của USER sau khi booking hoàn thành';


-- =============================================================================
-- PHẦN 2 — SEED DATA (Dữ liệu mẫu để demo)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- SEED 1: USERS
-- -----------------------------------------------------------------------------
-- Cách tạo tài khoản (KHUYẾN NGHỊ cho lần đầu):
--   Bước 1: Chạy app, vào http://localhost:8080/register
--   Bước 2: Đăng ký lần lượt các email dưới đây với password bất kỳ
--   Bước 3: Chạy các lệnh UPDATE bên dưới để đổi role thành ADMIN / PARTNER
--
-- Hoặc INSERT thẳng vào DB:
--   Password của tất cả tài khoản demo: Travelmate@123
--   Hash BCrypt dưới đây được tạo sẵn — nếu không đăng nhập được,
--   hãy dùng cách đăng ký qua app rồi UPDATE role.
-- -----------------------------------------------------------------------------

INSERT IGNORE INTO users (full_name, email, password, phone, role, status) VALUES
-- ADMIN — quản trị hệ thống
('Admin TravelMate',
 'admin@travelmate.vn',
 '$2a$10$slYQmyNdgzFoDeloNFfkA.62Yq3G8lGiYpDPH5vOMEZj8bm3MRJIG',
 '0901111111', 'ADMIN', 'ACTIVE'),

-- PARTNER — chủ nơi lưu trú
('Nguyễn Văn Đối Tác',
 'partner@travelmate.vn',
 '$2a$10$slYQmyNdgzFoDeloNFfkA.62Yq3G8lGiYpDPH5vOMEZj8bm3MRJIG',
 '0902222222', 'PARTNER', 'ACTIVE'),

-- USER — khách đặt phòng
('Trần Thị Bình',
 'user1@gmail.com',
 '$2a$10$slYQmyNdgzFoDeloNFfkA.62Yq3G8lGiYpDPH5vOMEZj8bm3MRJIG',
 '0903333333', 'USER', 'ACTIVE'),

('Lê Văn Minh',
 'user2@gmail.com',
 '$2a$10$slYQmyNdgzFoDeloNFfkA.62Yq3G8lGiYpDPH5vOMEZj8bm3MRJIG',
 '0904444444', 'USER', 'ACTIVE');

-- Nếu dùng cách đăng ký qua app, chạy 2 lệnh này để cập nhật role:
-- UPDATE users SET role = 'ADMIN'   WHERE email = 'admin@travelmate.vn';
-- UPDATE users SET role = 'PARTNER' WHERE email = 'partner@travelmate.vn';


-- -----------------------------------------------------------------------------
-- SEED 2: ACCOMMODATIONS
-- Giả định: PARTNER (id=2) đã tồn tại trong bảng users
-- Nếu partner_id khác 2, thay bằng id thực tế:
--   SELECT id FROM users WHERE email = 'partner@travelmate.vn';
-- Đủ loại property_type và approval_status để demo đầy đủ kịch bản
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO accommodations
    (partner_id, name, property_type, description, address, city,
     price_per_night, max_guests, approval_status, thumbnail_url)
VALUES

-- 1. HOTEL — Đà Nẵng — APPROVED
(2,
 'Khách Sạn Bình Minh Đà Nẵng',
 'HOTEL',
 'Khách sạn 4 sao nằm ngay trung tâm Đà Nẵng, cách bãi biển Mỹ Khê 500m. Phòng rộng rãi, thoáng mát, có hồ bơi ngoài trời và nhà hàng hải sản. View biển từ tầng 10 trở lên.',
 '123 Trần Phú, Hải Châu', 'Đà Nẵng',
 1200000.00, 4, 'APPROVED',
 '/assets/images/MienTrung/hotel-danang.jpg'),

-- 2. HOMESTAY — Quảng Bình — APPROVED
(2,
 'Homestay Phong Nha View',
 'HOMESTAY',
 'Homestay ấm cúng giữa rừng nguyên sinh Phong Nha. Thích hợp cho nhóm nhỏ muốn trải nghiệm thiên nhiên hoang dã. Chủ nhà thân thiện, bữa sáng miễn phí, xe đạp cho thuê.',
 '45 Đường Vào Hang, Sơn Trạch', 'Quảng Bình',
 450000.00, 6, 'APPROVED',
 '/assets/images/MienTrung/homestay-phongnha.jpg'),

-- 3. VILLA — Huế — APPROVED
(2,
 'Villa Sông Hương - Huế',
 'VILLA',
 'Villa sang trọng bên bờ sông Hương thơ mộng. Sân vườn riêng, hồ bơi riêng, bếp đầy đủ tiện nghi. Phù hợp gia đình hoặc nhóm bạn muốn không gian riêng tư và yên tĩnh.',
 '78 Lê Lợi, Phú Hội', 'Huế',
 3500000.00, 8, 'APPROVED',
 '/assets/images/MienTrung/villa-hue.jpg'),

-- 4. APARTMENT — TP. Hồ Chí Minh — APPROVED
(2,
 'Apartment Vinhomes Landmark 81 - HCM',
 'APARTMENT',
 'Căn hộ cao cấp tầng 30 với view toàn cảnh TP.HCM về đêm. Full nội thất, máy giặt, bếp đầy đủ, Internet tốc độ cao. Ngay trung tâm Bình Thạnh, cách Landmark 81 chỉ 5 phút đi bộ.',
 '720A Điện Biên Phủ, Bình Thạnh', 'TP. Hồ Chí Minh',
 2200000.00, 4, 'APPROVED',
 '/assets/images/MienNam/apartment-hcm.jpg'),

-- 5. HOTEL — Hà Nội — APPROVED
(2,
 'Khách Sạn Bảo Sơn Hà Nội',
 'HOTEL',
 'Khách sạn truyền thống nằm trong khu phố cổ Hà Nội. Gần Hồ Hoàn Kiếm và các điểm tham quan lịch sử. Phong cách thiết kế cổ điển Bắc Bộ, phòng tắm đá marble cao cấp.',
 '50 Hàng Bài, Hoàn Kiếm', 'Hà Nội',
 950000.00, 2, 'APPROVED',
 '/assets/images/MienBac/hotel-hanoi.jpg'),

-- 6. VILLA — Đà Lạt — APPROVED
(2,
 'Villa Đồi Thông Đà Lạt',
 'VILLA',
 'Villa giữa rừng thông Đà Lạt, không gian lãng mạn và yên bình. Lò sưởi, ban công ngắm thành phố, bếp BBQ ngoài trời. Gần chợ Đà Lạt 2km, thuận tiện di chuyển.',
 '15 Hùng Vương, Phường 9', 'Đà Lạt',
 2800000.00, 6, 'APPROVED',
 '/assets/images/MienNam/villa-dalat.jpg'),

-- 7. HOMESTAY — Sapa — PENDING (demo: listing chờ duyệt)
(2,
 'Homestay Núi Rừng Sapa',
 'HOMESTAY',
 'Homestay của gia đình người H''Mông, trải nghiệm văn hoá bản địa độc đáo. Ngủ nhà sàn, ăn cơm lam, ngắm ruộng bậc thang mùa lúa vàng. Hướng dẫn viên bản địa đi kèm.',
 'Bản Cát Cát, Sa Pa', 'Lào Cai',
 380000.00, 6, 'PENDING',
 '/assets/images/MienBac/homestay-sapa.jpg'),

-- 8. APARTMENT — Hội An — REJECTED (demo: listing bị từ chối)
(2,
 'Apartment Phố Cổ Hội An',
 'APARTMENT',
 'Căn hộ nằm ngay trong lòng phố cổ Hội An. Thiết kế theo phong cách Nhật-Việt kết hợp. Gần chùa Cầu, phù hợp couple hoặc du khách solo.',
 '22 Nguyễn Thái Học, Minh An', 'Hội An',
 1100000.00, 2, 'REJECTED',
 '/assets/images/MienTrung/apartment-hoian.jpg');


-- -----------------------------------------------------------------------------
-- SEED 3: BOOKINGS
-- Đủ 4 trạng thái để demo toàn bộ flow khi báo cáo:
--   COMPLETED → có thể review
--   CONFIRMED → đang có hiệu lực
--   PENDING   → chờ xác nhận
--   CANCELLED → đã huỷ
--
-- user_id = 3 (user1@gmail.com), user_id = 4 (user2@gmail.com)
-- Nếu id thực tế khác, thay bằng: SELECT id FROM users WHERE email = '...';
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO bookings
    (user_id, accommodation_id, check_in, check_out,
     num_adults, num_children, total_price, booking_status, notes)
VALUES

-- Booking 1: COMPLETED — user1 đặt KS Đà Nẵng (3 đêm × 1.200.000)
-- → Đủ điều kiện để viết review
(3, 1,
 '2025-03-10', '2025-03-13',
 2, 0, 3600000.00, 'COMPLETED',
 'Phòng tầng cao, view biển nếu được. Cảm ơn!'),

-- Booking 2: CONFIRMED — user1 đặt Villa Huế (3 đêm × 3.500.000)
-- → Đã xác nhận, sắp đến
(3, 3,
 '2026-05-01', '2026-05-04',
 2, 1, 10500000.00, 'CONFIRMED',
 'Cần giường phụ cho trẻ em, và crib nếu có'),

-- Booking 3: PENDING — user2 đặt Homestay Phong Nha (3 đêm × 450.000)
-- → Mới đặt, chờ xác nhận
(4, 2,
 '2026-06-15', '2026-06-18',
 4, 2, 1350000.00, 'PENDING',
 NULL),

-- Booking 4: CANCELLED — user1 đặt Apartment HCM (3 đêm × 2.200.000)
-- → Đã huỷ vì có việc bận
(3, 4,
 '2025-12-20', '2025-12-23',
 2, 0, 6600000.00, 'CANCELLED',
 'Huỷ vì có việc bận đột xuất');


-- -----------------------------------------------------------------------------
-- SEED 4: PAYMENTS
-- Mỗi booking tạo 1 payment record tương ứng
-- transaction_code dạng: {METHOD}{date}TM{id} — mock, không thật
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO payments
    (booking_id, amount, payment_method, payment_status, transaction_code, paid_at)
VALUES

-- Payment cho Booking 1 (COMPLETED): đã thanh toán qua MoMo
(1, 3600000.00, 'MOMO',  'PAID',
 'MOMO20250310TM001', '2025-03-10 09:15:22'),

-- Payment cho Booking 2 (CONFIRMED): đã thanh toán qua VNPay
(2, 10500000.00, 'VNPAY', 'PAID',
 'VNP20260428TM002',  '2026-04-28 14:30:45'),

-- Payment cho Booking 3 (PENDING): chưa chọn phương thức, chưa thanh toán
(3, 1350000.00,  NULL,    'UNPAID',
 NULL, NULL),

-- Payment cho Booking 4 (CANCELLED): đã hoàn tiền
(4, 6600000.00, 'MOMO',  'REFUNDED',
 'MOMO20251218TM004', '2025-12-18 16:45:10');


-- -----------------------------------------------------------------------------
-- SEED 5: REVIEWS
-- Chỉ Booking 1 (COMPLETED) mới được review
-- Booking 2, 3, 4 chưa / không được review
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO reviews
    (user_id, accommodation_id, booking_id, rating, comment)
VALUES
(3, 1, 1,
 5,
 'Khách sạn rất tuyệt! Phòng sạch sẽ, view biển từ tầng 12 đẹp không thể tả. Nhân viên thân thiện và nhiệt tình. Bãi biển Mỹ Khê đi bộ chỉ 5 phút. Bữa sáng buffet đa dạng. Chắc chắn sẽ quay lại lần sau!');


-- =============================================================================
-- PHẦN 3 — KIỂM TRA SAU KHI CHẠY SCRIPT
-- =============================================================================
SET FOREIGN_KEY_CHECKS = 1;

-- Đếm số bản ghi mỗi bảng — kết quả đúng:
--   users=4, accommodations=8, bookings=4, payments=4, reviews=1
SELECT 'users'          AS `Bảng`,  COUNT(*) AS `Số bản ghi` FROM users
UNION ALL
SELECT 'accommodations',            COUNT(*) FROM accommodations
UNION ALL
SELECT 'bookings',                  COUNT(*) FROM bookings
UNION ALL
SELECT 'payments',                  COUNT(*) FROM payments
UNION ALL
SELECT 'reviews',                   COUNT(*) FROM reviews;

-- Xem nhanh danh sách accommodation đã APPROVED (USER sẽ thấy):
SELECT id, name, property_type, city, price_per_night, approval_status
FROM   accommodations
WHERE  approval_status = 'APPROVED'
ORDER BY id;

-- Xem booking của user1 kèm trạng thái payment:
SELECT
    b.id            AS booking_id,
    b.booking_status,
    b.check_in,
    b.check_out,
    b.total_price,
    p.payment_status,
    p.payment_method,
    a.name          AS accommodation_name
FROM bookings b
JOIN accommodations a ON b.accommodation_id = a.id
LEFT JOIN payments  p ON p.booking_id = b.id
WHERE b.user_id = 3
ORDER BY b.created_at DESC;


-- =============================================================================
-- PHẦN 4 — GHI CHÚ MỞ RỘNG (cập nhật khi thêm bảng mới)
-- =============================================================================
-- Phase USER flow (file này): users, accommodations, bookings, payments, reviews
--
-- Phase PARTNER flow (thêm sau nếu cần):
--   → Không cần bảng mới — PARTNER dùng lại accommodations + bookings
--
-- Phase ADMIN flow (thêm sau nếu cần):
--   → Không cần bảng mới — ADMIN đọc/cập nhật approval_status của accommodations
--
-- Hướng mở rộng tương lai (KHÔNG làm trong đồ án cơ sở):
--   → vouchers / discount_codes (bảng riêng nếu cần)
--   → amenities / accommodation_amenities (quan hệ nhiều-nhiều nếu cần)
--   → accommodation_images (nhiều ảnh thay vì 1 thumbnail nếu cần)
--   → notifications (nếu cần thông báo)
-- =============================================================================
