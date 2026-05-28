-- =============================================
-- FIX: Seed amenities cho TẤT CẢ rooms đang thiếu tiện nghi
-- Chạy 1 lần vào DB đang chạy để bổ sung data
-- Script an toàn: INSERT IGNORE để không bị lỗi duplicate
-- =============================================

USE travelmate_db;

-- ── Bước 1: Đảm bảo bảng amenities có đủ 32 tiện nghi ─────────────────────
INSERT IGNORE INTO amenities (name, icon, category) VALUES
-- Nhóm 1: Tiện ích chung
('WiFi miễn phí',      '📶', 'Tiện ích chung'),
('Hồ bơi chung',       '🏊', 'Tiện ích chung'),
('Bãi đỗ xe',          '🚗', 'Tiện ích chung'),
('Nhà hàng',           '🍽️', 'Tiện ích chung'),
('Spa / Massage',      '💆', 'Tiện ích chung'),
('Phòng gym',          '💪', 'Tiện ích chung'),
('Bar / Café',         '☕', 'Tiện ích chung'),
('Thang máy',          '🛗', 'Tiện ích chung'),
-- Nhóm 2: Phòng ngủ
('Máy lạnh',           '❄️', 'Phòng ngủ'),
('TV màn hình phẳng',  '📺', 'Phòng ngủ'),
('Tủ lạnh mini',       '🧊', 'Phòng ngủ'),
('Minibar',            '🍾', 'Phòng ngủ'),
('Bàn làm việc',       '🖥️', 'Phòng ngủ'),
('Két an toàn',        '🔐', 'Phòng ngủ'),
('Ổ cắm quốc tế',      '🔌', 'Phòng ngủ'),
-- Nhóm 3: Phòng tắm
('Phòng tắm riêng',    '🚿', 'Phòng tắm'),
('Bồn tắm nằm',        '🛁', 'Phòng tắm'),
('Vòi sen đứng',       '🚿', 'Phòng tắm'),
('Máy sấy tóc',        '💨', 'Phòng tắm'),
('Đồ dùng cá nhân',    '🧴', 'Phòng tắm'),
-- Nhóm 4: Bếp & Ăn uống
('Bếp riêng',          '👨‍🍳', 'Bếp & Ăn uống'),
('Tủ lạnh đầy đủ',     '🧊', 'Bếp & Ăn uống'),
('Máy pha cà phê',     '☕', 'Bếp & Ăn uống'),
('Lò vi sóng',         '📡', 'Bếp & Ăn uống'),
('Ấm đun nước',        '🫖', 'Bếp & Ăn uống'),
('Bữa sáng miễn phí',  '🍳', 'Bếp & Ăn uống'),
-- Nhóm 5: Đặc biệt
('Ban công riêng',     '🌅', 'Đặc biệt'),
('View biển',          '🌊', 'Đặc biệt'),
('View núi / đồi',     '⛰️', 'Đặc biệt'),
('Hồ bơi riêng',       '🏊', 'Đặc biệt'),
('BBQ / Bếp nướng',    '🔥', 'Đặc biệt'),
('Sân vườn riêng',     '🌿', 'Đặc biệt');

-- ── Bước 2: Seed room_amenities cho TẤT CẢ rooms ──────────────────────────
-- Dùng INSERT IGNORE để không bị lỗi nếu đã có sẵn

-- ── LATA-STD: Tiêu chuẩn ──────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'LATA-STD' AND a.name IN (
    'WiFi miễn phí', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
    'Tủ lạnh mini', 'Phòng tắm riêng', 'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bàn làm việc');

-- ── LATA-DLX: Deluxe ──────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'LATA-DLX' AND a.name IN (
    'WiFi miễn phí', 'Nhà hàng', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
    'Minibar', 'Phòng tắm riêng', 'Bồn tắm nằm', 'Máy sấy tóc',
    'Bữa sáng miễn phí', 'Ban công riêng', 'Bàn làm việc');

-- ── LATA-FAM: Gia đình ────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'LATA-FAM' AND a.name IN (
    'WiFi miễn phí', 'Hồ bơi chung', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
    'Tủ lạnh mini', 'Phòng tắm riêng', 'Bồn tắm nằm', 'Máy sấy tóc', 'Đồ dùng cá nhân');

-- ── LATA-SUI: Suite ───────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'LATA-SUI' AND a.name IN (
    'WiFi miễn phí', 'Nhà hàng', 'Spa / Massage', 'Thang máy', 'Máy lạnh',
    'TV màn hình phẳng', 'Minibar', 'Két an toàn', 'Ổ cắm quốc tế',
    'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
    'Bữa sáng miễn phí', 'Ban công riêng', 'Bàn làm việc');

-- ── TLP-STD ───────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'TLP-STD' AND a.name IN (
    'WiFi miễn phí', 'Máy lạnh', 'TV màn hình phẳng',
    'Phòng tắm riêng', 'Máy sấy tóc', 'Đồ dùng cá nhân');

-- ── TLP-SUP ───────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'TLP-SUP' AND a.name IN (
    'WiFi miễn phí', 'Máy lạnh', 'TV màn hình phẳng', 'Tủ lạnh mini',
    'Phòng tắm riêng', 'Máy sấy tóc', 'Bữa sáng miễn phí',
    'Ban công riêng', 'View núi / đồi');

-- ── TLP-FAM ───────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'TLP-FAM' AND a.name IN (
    'WiFi miễn phí', 'Máy lạnh', 'TV màn hình phẳng',
    'Tủ lạnh mini', 'Phòng tắm riêng', 'Máy sấy tóc', 'Đồ dùng cá nhân');

-- ── TLP-VIP ───────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'TLP-VIP' AND a.name IN (
    'WiFi miễn phí', 'Spa / Massage', 'Máy lạnh', 'TV màn hình phẳng',
    'Minibar', 'Két an toàn', 'Phòng tắm riêng', 'Bồn tắm nằm',
    'Máy sấy tóc', 'Ban công riêng', 'View núi / đồi', 'Bàn làm việc');

-- ── TMG-DLX ───────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'TMG-DLX' AND a.name IN (
    'WiFi miễn phí', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
    'Minibar', 'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng',
    'Bàn làm việc', 'Ổ cắm quốc tế');

-- ── TMG-PRE ───────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'TMG-PRE' AND a.name IN (
    'WiFi miễn phí', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
    'Minibar', 'Két an toàn', 'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng',
    'Máy sấy tóc', 'Bữa sáng miễn phí', 'Ban công riêng', 'View núi / đồi', 'Bàn làm việc');

-- ── TMG-FAM ───────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'TMG-FAM' AND a.name IN (
    'WiFi miễn phí', 'Hồ bơi chung', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
    'Tủ lạnh đầy đủ', 'Bếp riêng', 'Phòng tắm riêng', 'Bồn tắm nằm', 'Máy pha cà phê');

-- ── TMG-PRE2: Presidential Suite ──────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'TMG-PRE2' AND a.name IN (
    'WiFi miễn phí', 'Nhà hàng', 'Spa / Massage', 'Bar / Café', 'Thang máy',
    'Máy lạnh', 'TV màn hình phẳng', 'Minibar', 'Két an toàn', 'Ổ cắm quốc tế',
    'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
    'Bữa sáng miễn phí', 'Ban công riêng', 'View núi / đồi', 'Bàn làm việc');

-- ── ANM-GDN: Garden Pool Villa ────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'ANM-GDN' AND a.name IN (
    'WiFi miễn phí', 'Máy lạnh', 'TV màn hình phẳng', 'Phòng tắm riêng',
    'Bồn tắm nằm', 'Bữa sáng miễn phí', 'Ban công riêng',
    'Hồ bơi riêng', 'Sân vườn riêng', 'Bếp riêng', 'Máy pha cà phê');

-- ── ANM-BCH: Beachfront Pool Villa ────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'ANM-BCH' AND a.name IN (
    'WiFi miễn phí', 'Máy lạnh', 'TV màn hình phẳng', 'Minibar',
    'Phòng tắm riêng', 'Bồn tắm nằm', 'Bữa sáng miễn phí',
    'Ban công riêng', 'View biển', 'Hồ bơi riêng', 'Sân vườn riêng',
    'Bếp riêng', 'Tủ lạnh đầy đủ');

-- ── ANM-FAM: Family Grand Villa ───────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'ANM-FAM' AND a.name IN (
    'WiFi miễn phí', 'Máy lạnh', 'TV màn hình phẳng', 'Phòng tắm riêng',
    'Bồn tắm nằm', 'Ban công riêng', 'Hồ bơi riêng', 'BBQ / Bếp nướng',
    'Sân vườn riêng', 'Bếp riêng', 'Tủ lạnh đầy đủ', 'Lò vi sóng');

-- ── BNH-BNG: Forest Bungalow ──────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'BNH-BNG' AND a.name IN (
    'WiFi miễn phí', 'Máy lạnh', 'TV màn hình phẳng', 'Phòng tắm riêng',
    'Bồn tắm nằm', 'Ban công riêng', 'View núi / đồi', 'Sân vườn riêng');

-- ── BNH-TWN: Twin Cabin ───────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'BNH-TWN' AND a.name IN (
    'WiFi miễn phí', 'Máy lạnh', 'TV màn hình phẳng', 'Phòng tắm riêng',
    'Máy sấy tóc', 'Bữa sáng miễn phí', 'Ban công riêng',
    'View núi / đồi', 'BBQ / Bếp nướng');

-- ── BNH-SUI: Treetop Suite ────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'BNH-SUI' AND a.name IN (
    'WiFi miễn phí', 'Máy lạnh', 'TV màn hình phẳng', 'Minibar',
    'Phòng tắm riêng', 'Bồn tắm nằm', 'Ban công riêng',
    'View núi / đồi', 'Sân vườn riêng');

-- ── HLR-STD: Phòng Truyền Thống ──────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'HLR-STD' AND a.name IN (
    'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng',
    'Đồ dùng cá nhân', 'Bữa sáng miễn phí', 'Ấm đun nước');

-- ── HLR-DLX ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'HLR-DLX' AND a.name IN (
    'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng',
    'Phòng tắm riêng', 'Máy sấy tóc', 'Đồ dùng cá nhân',
    'Bữa sáng miễn phí', 'Ban công riêng');

-- ── HLR-FAM ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'HLR-FAM' AND a.name IN (
    'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng',
    'Phòng tắm riêng', 'Bếp riêng', 'Bữa sáng miễn phí', 'Ban công riêng');

-- ── MND-STD ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'MND-STD' AND a.name IN (
    'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng',
    'Đồ dùng cá nhân', 'Bữa sáng miễn phí', 'Ấm đun nước');

-- ── MND-ATT ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'MND-ATT' AND a.name IN (
    'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng',
    'Phòng tắm riêng', 'Máy sấy tóc', 'Đồ dùng cá nhân',
    'Bữa sáng miễn phí', 'View núi / đồi', 'Ban công riêng');

-- ── MND-FAM ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'MND-FAM' AND a.name IN (
    'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng',
    'Phòng tắm riêng', 'Bếp riêng', 'Bữa sáng miễn phí',
    'Sân vườn riêng', 'Ban công riêng');

-- ── VNT-DLX ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'VNT-DLX' AND a.name IN (
    'WiFi miễn phí', 'Hồ bơi chung', 'Nhà hàng', 'Spa / Massage', 'Thang máy',
    'Máy lạnh', 'TV màn hình phẳng', 'Minibar', 'Phòng tắm riêng',
    'Bồn tắm nằm', 'Vòi sen đứng', 'Bữa sáng miễn phí', 'View biển');

-- ── VNT-SUI ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'VNT-SUI' AND a.name IN (
    'WiFi miễn phí', 'Nhà hàng', 'Spa / Massage', 'Thang máy',
    'Máy lạnh', 'TV màn hình phẳng', 'Minibar', 'Két an toàn',
    'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
    'Bữa sáng miễn phí', 'Ban công riêng', 'View biển');

-- ── VNT-VIL ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'VNT-VIL' AND a.name IN (
    'WiFi miễn phí', 'Nhà hàng', 'Máy lạnh', 'TV màn hình phẳng',
    'Tủ lạnh đầy đủ', 'Bếp riêng', 'Lò vi sóng', 'Phòng tắm riêng',
    'Bồn tắm nằm', 'Bữa sáng miễn phí', 'Ban công riêng',
    'View biển', 'Hồ bơi riêng', 'Sân vườn riêng');

-- ── FDN-DLX ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'FDN-DLX' AND a.name IN (
    'WiFi miễn phí', 'Hồ bơi chung', 'Nhà hàng', 'Spa / Massage', 'Thang máy',
    'Máy lạnh', 'TV màn hình phẳng', 'Minibar', 'Phòng tắm riêng',
    'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc', 'Bữa sáng miễn phí');

-- ── FDN-BCH ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'FDN-BCH' AND a.name IN (
    'WiFi miễn phí', 'Hồ bơi chung', 'Nhà hàng', 'Spa / Massage', 'Thang máy',
    'Máy lạnh', 'TV màn hình phẳng', 'Minibar', 'Két an toàn',
    'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
    'Bữa sáng miễn phí', 'Ban công riêng', 'View biển');

-- ── FDN-FAM ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'FDN-FAM' AND a.name IN (
    'WiFi miễn phí', 'Hồ bơi chung', 'Nhà hàng', 'Spa / Massage', 'Thang máy',
    'Máy lạnh', 'TV màn hình phẳng', 'Tủ lạnh đầy đủ', 'Bếp riêng', 'Lò vi sóng',
    'Phòng tắm riêng', 'Bồn tắm nằm', 'Bữa sáng miễn phí',
    'Ban công riêng', 'View biển', 'Sân vườn riêng');

-- ── PHV-DLX ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'PHV-DLX' AND a.name IN (
    'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng', 'Bếp riêng',
    'Tủ lạnh đầy đủ', 'Lò vi sóng', 'Ấm đun nước', 'Bồn tắm nằm', 'Máy sấy tóc',
    'Ban công riêng', 'View núi / đồi', 'BBQ / Bếp nướng', 'Sân vườn riêng');

-- ── PHV-FAM ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'PHV-FAM' AND a.name IN (
    'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng', 'Bếp riêng',
    'Tủ lạnh đầy đủ', 'Lò vi sóng', 'Ấm đun nước', 'Bồn tắm nằm', 'Máy sấy tóc',
    'Ban công riêng', 'View núi / đồi', 'BBQ / Bếp nướng', 'Sân vườn riêng');

-- ── SBV-SEA ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'SBV-SEA' AND a.name IN (
    'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng', 'Bếp riêng',
    'Tủ lạnh đầy đủ', 'Lò vi sóng', 'Bồn tắm nằm', 'Máy sấy tóc', 'Ban công riêng',
    'View biển', 'BBQ / Bếp nướng', 'Sân vườn riêng');

-- ── SBV-POOL ─────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'SBV-POOL' AND a.name IN (
    'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng', 'Bếp riêng',
    'Tủ lạnh đầy đủ', 'Bồn tắm nằm', 'Máy sấy tóc', 'Ban công riêng', 'View biển',
    'Hồ bơi riêng', 'BBQ / Bếp nướng', 'Sân vườn riêng');

-- ── TCG-STD ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'TCG-STD' AND a.name IN (
    'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng', 'Tủ lạnh mini',
    'Phòng tắm riêng', 'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bữa sáng miễn phí',
    'Sân vườn riêng');

-- ── TCG-FAM ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'TCG-FAM' AND a.name IN (
    'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng', 'Tủ lạnh mini',
    'Phòng tắm riêng', 'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bữa sáng miễn phí',
    'Ban công riêng', 'View núi / đồi', 'Sân vườn riêng');

-- ── SVH-STD ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'SVH-STD' AND a.name IN (
    'WiFi miễn phí', 'Bãi đỗ xe', 'Phòng tắm riêng', 'Máy sấy tóc', 'Đồ dùng cá nhân',
    'Bữa sáng miễn phí', 'View núi / đồi');

-- ── SVH-DLX ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'SVH-DLX' AND a.name IN (
    'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng', 'Tủ lạnh mini',
    'Phòng tắm riêng', 'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bữa sáng miễn phí',
    'Ban công riêng', 'View núi / đồi');

-- ── LBR-DLX ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'LBR-DLX' AND a.name IN (
    'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
    'Phòng gym', 'Bar / Café', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
    'Minibar', 'Bàn làm việc', 'Két an toàn', 'Phòng tắm riêng', 'Bồn tắm nằm',
    'Vòi sen đứng', 'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bữa sáng miễn phí',
    'Ban công riêng', 'View biển');

-- ── LBR-SUI ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'LBR-SUI' AND a.name IN (
    'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
    'Phòng gym', 'Bar / Café', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
    'Minibar', 'Bàn làm việc', 'Két an toàn', 'Ổ cắm quốc tế', 'Phòng tắm riêng',
    'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc', 'Đồ dùng cá nhân',
    'Bữa sáng miễn phí', 'Ban công riêng', 'View biển');

-- ── NWS-STD ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'NWS-STD' AND a.name IN (
    'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
    'Phòng gym', 'Bar / Café', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
    'Tủ lạnh mini', 'Minibar', 'Bàn làm việc', 'Két an toàn', 'Ổ cắm quốc tế',
    'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
    'Đồ dùng cá nhân', 'Bữa sáng miễn phí');

-- ── NWS-DLX ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'NWS-DLX' AND a.name IN (
    'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
    'Phòng gym', 'Bar / Café', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
    'Tủ lạnh mini', 'Minibar', 'Bàn làm việc', 'Két an toàn', 'Ổ cắm quốc tế',
    'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
    'Đồ dùng cá nhân', 'Bữa sáng miễn phí', 'Ban công riêng');

-- ── PVT-STD ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'PVT-STD' AND a.name IN (
    'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
    'Phòng gym', 'Bar / Café', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
    'Tủ lạnh mini', 'Minibar', 'Bàn làm việc', 'Két an toàn', 'Ổ cắm quốc tế',
    'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
    'Đồ dùng cá nhân', 'Bữa sáng miễn phí', 'Ban công riêng', 'View biển');

-- ── PVT-DLX ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'PVT-DLX' AND a.name IN (
    'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
    'Phòng gym', 'Bar / Café', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
    'Tủ lạnh mini', 'Minibar', 'Bàn làm việc', 'Két an toàn', 'Ổ cắm quốc tế',
    'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
    'Đồ dùng cá nhân', 'Bữa sáng miễn phí', 'Ban công riêng');

-- ── AZC-DLX ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'AZC-DLX' AND a.name IN (
    'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
    'Phòng gym', 'Bar / Café', 'Máy lạnh', 'TV màn hình phẳng', 'Minibar',
    'Bàn làm việc', 'Két an toàn', 'Phòng tắm riêng', 'Bồn tắm nằm',
    'Vòi sen đứng', 'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bữa sáng miễn phí',
    'Ban công riêng', 'Sân vườn riêng');

-- ── AZC-SUI ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'AZC-SUI' AND a.name IN (
    'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
    'Phòng gym', 'Bar / Café', 'Máy lạnh', 'TV màn hình phẳng', 'Minibar',
    'Bàn làm việc', 'Két an toàn', 'Ổ cắm quốc tế', 'Phòng tắm riêng',
    'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc', 'Đồ dùng cá nhân',
    'Bữa sáng miễn phí', 'Ban công riêng', 'Hồ bơi riêng', 'Sân vườn riêng');

-- ── TTC-DLX ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'TTC-DLX' AND a.name IN (
    'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
    'Phòng gym', 'Bar / Café', 'Máy lạnh', 'TV màn hình phẳng', 'Minibar',
    'Bàn làm việc', 'Két an toàn', 'Phòng tắm riêng', 'Bồn tắm nằm',
    'Vòi sen đứng', 'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bữa sáng miễn phí',
    'Ban công riêng', 'View biển');

-- ── TTC-FAM ──────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'TTC-FAM' AND a.name IN (
    'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
    'Phòng gym', 'Bar / Café', 'Máy lạnh', 'TV màn hình phẳng', 'Tủ lạnh đầy đủ',
    'Bếp riêng', 'Lò vi sóng', 'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng',
    'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bữa sáng miễn phí', 'Ban công riêng',
    'Sân vườn riêng');

-- ── PL-STD-01 ─────────────────────────────────────────────────────────────
INSERT IGNORE INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
WHERE r.room_code = 'PL-STD-01' AND a.name IN (
    'WiFi miễn phí', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage', 'Bar / Café',
    'Thang máy', 'Máy lạnh', 'TV màn hình phẳng', 'Tủ lạnh mini', 'Minibar',
    'Bàn làm việc', 'Két an toàn', 'Phòng tắm riêng', 'Bồn tắm nằm',
    'Vòi sen đứng', 'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bữa sáng miễn phí',
    'View núi / đồi');

-- ── Cập nhật available_for_booking cho các rows bị NULL ──────────────────
UPDATE rooms SET available_for_booking = 1 WHERE available_for_booking IS NULL;

-- ── Kiểm tra kết quả ────────────────────────────────────────────────────
SELECT r.room_code, r.room_name, COUNT(ra.amenity_id) AS amenity_count
FROM rooms r
LEFT JOIN room_amenities ra ON r.id = ra.room_id
GROUP BY r.id, r.room_code, r.room_name
ORDER BY amenity_count ASC, r.room_code;
