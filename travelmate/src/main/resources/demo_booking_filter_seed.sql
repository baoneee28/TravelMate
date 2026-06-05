-- =======================================================================
-- TRAVELMATE - DU LIEU BO LOC BOOKING CHO ADMIN / PARTNER
-- =======================================================================
-- Chay sau travelmate_db.sql khi can bo du lieu trinh bay tap trung vao
-- cac tab va badge tren man hinh booking. File nay dung ma booking tu nhien
-- dang BK-<ma phong>-00xxx va co the chay lai nhieu lan.
--
-- Cach chay:
--   mysql -u root -p travelmate_db < demo_booking_filter_seed.sql
-- =======================================================================

USE travelmate_db;

-- Neu lan chay truoc bi dung giua chung, dua session ve trang thai an toan
-- truoc khi luu baseline va chay lai seed.
SET FOREIGN_KEY_CHECKS = 1;
SET SQL_SAFE_UPDATES = 1;

SET @OLD_FOREIGN_KEY_CHECKS_DEMO_FILTER = @@FOREIGN_KEY_CHECKS;
SET @OLD_SQL_SAFE_UPDATES_DEMO_FILTER = @@SQL_SAFE_UPDATES;
SET @OLD_FILTER_CODE_PATTERN = CONCAT('DEMO', '-FILTER', '-%');
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

-- Hoan lai quota tu lan chay truoc truoc khi xoa data theo prefix.
UPDATE rooms r
JOIN (
    SELECT b.room_id, SUM(b.room_quantity) AS hold_qty
    FROM bookings b
    WHERE b.booking_code LIKE @OLD_FILTER_CODE_PATTERN
      AND b.booking_status NOT IN ('COMPLETED', 'CANCELLED', 'NO_SHOW')
    GROUP BY b.room_id
) old_demo ON old_demo.room_id = r.id
SET r.available_quantity = r.available_quantity + old_demo.hold_qty;

DELETE FROM reviews
WHERE booking_id IN (
    SELECT id FROM bookings WHERE booking_code LIKE @OLD_FILTER_CODE_PATTERN
);

DELETE FROM payments
WHERE booking_id IN (
    SELECT id FROM bookings WHERE booking_code LIKE @OLD_FILTER_CODE_PATTERN
);

DELETE FROM notifications
WHERE title LIKE CONCAT('%', @OLD_FILTER_CODE_PATTERN)
   OR message LIKE CONCAT('%', @OLD_FILTER_CODE_PATTERN)
   OR target_url LIKE CONCAT('%', @OLD_FILTER_CODE_PATTERN);

DELETE FROM admin_action_logs
WHERE description LIKE CONCAT('%', @OLD_FILTER_CODE_PATTERN)
   OR note LIKE CONCAT('%', @OLD_FILTER_CODE_PATTERN);

DELETE FROM bookings
WHERE booking_code LIKE @OLD_FILTER_CODE_PATTERN;

-- Chuan hoa nhanh mot so booking da import tu ban truoc ve luong VNPAY moi:
-- thanh toan/coc thanh cong -> TravelMate tu dong giu phong/can.
UPDATE bookings
SET booking_status = 'CONFIRMED',
    payment_status = 'APPROVED',
    partner_status = 'PARTNER_CONFIRMED',
    note = CASE booking_code
        WHEN 'BK-LATA-STD-0001' THEN 'VNPAY ghi nhận thành công — khách đã cọc 30%, TravelMate tự động giữ phòng.'
        WHEN 'BK-TLP-SUP-0001' THEN 'VNPAY ghi nhận thành công — khách thanh toán 100%, TravelMate tự động giữ phòng.'
        WHEN 'BK-ANM-GDN-0001' THEN 'VNPAY ghi nhận thành công — cọc 30% Anam Garden Villa, TravelMate tự động giữ villa.'
        WHEN 'BK-VNT-VIL-CAL01' THEN 'Vinpearl Pool Villa tháng tới — VNPAY ghi nhận cọc 30%, TravelMate tự động giữ villa.'
        ELSE note
    END,
    updated_at = NOW()
WHERE booking_code IN ('BK-LATA-STD-0001', 'BK-TLP-SUP-0001', 'BK-ANM-GDN-0001', 'BK-VNT-VIL-CAL01');

UPDATE payments p
JOIN bookings b ON b.id = p.booking_id
SET p.payment_status = 'APPROVED',
    p.approved_at = COALESCE(p.approved_at, NOW()),
    p.note = CASE b.booking_code
        WHEN 'BK-LATA-STD-0001' THEN 'VNPAY ghi nhận thành công — cọc 30%, TravelMate tự động giữ phòng.'
        WHEN 'BK-TLP-SUP-0001' THEN 'VNPAY ghi nhận thành công — thanh toán 100%, TravelMate tự động giữ phòng.'
        WHEN 'BK-ANM-GDN-0001' THEN 'Cọc 30% Anam Garden Villa — VNPAY ghi nhận thành công, TravelMate tự động giữ villa.'
        WHEN 'BK-VNT-VIL-CAL01' THEN 'Lịch Vinpearl VIL +40→+45 — VNPAY ghi nhận thành công, TravelMate tự động giữ villa.'
        ELSE p.note
    END
WHERE b.booking_code IN ('BK-LATA-STD-0001', 'BK-TLP-SUP-0001', 'BK-ANM-GDN-0001', 'BK-VNT-VIL-CAL01');

DROP TEMPORARY TABLE IF EXISTS tmp_demo_filter_types;
CREATE TEMPORARY TABLE tmp_demo_filter_types (
    demo_type     VARCHAR(20)  NOT NULL,
    type_label    VARCHAR(80)  NOT NULL,
    partner_email VARCHAR(100) NOT NULL,
    acc_name      VARCHAR(255) NOT NULL,
    room_code_1   VARCHAR(50)  NOT NULL,
    room_code_2   VARCHAR(50)  NOT NULL,
    room_code_3   VARCHAR(50)  NOT NULL
);

INSERT INTO tmp_demo_filter_types
    (demo_type, type_label, partner_email, acc_name, room_code_1, room_code_2, room_code_3)
VALUES
    ('HOTEL',    'Khach san', 'partner@travelmate.vn',  'TravelMate Grand Hotel',              'TMG-DLX', 'TMG-PRE', 'TMG-FAM'),
    ('RESORT',   'Resort',    'resort@travelmate.vn',   'Terracotta Hotel & Resort Đà Lạt',     'TCR-GDN', 'TCR-LAK', 'TCR-FAM'),
    ('VILLA',    'Villa',     'villa@travelmate.vn',    'Tràng An River Villa',                 'TRV-DLX', 'TRV-FAM', 'TRV-SUI'),
    ('HOMESTAY', 'Homestay',  'homestay@travelmate.vn', 'Cầu Đất Farm Homestay',                'CDF-STD', 'CDF-DLX', 'CDF-FAM');

DROP TEMPORARY TABLE IF EXISTS tmp_demo_filter_scenarios;
CREATE TEMPORARY TABLE tmp_demo_filter_scenarios (
    suffix                   VARCHAR(30)   NOT NULL,
    booking_seq              VARCHAR(5)    NOT NULL,
    user_email               VARCHAR(100)  NOT NULL,
    room_slot                TINYINT       NOT NULL,
    checkin_offset           INT           NOT NULL,
    checkout_offset          INT           NOT NULL,
    adults                   INT           NOT NULL,
    children                 INT           NOT NULL,
    booking_status           VARCHAR(50)   NOT NULL,
    payment_option           VARCHAR(50)   NOT NULL,
    payment_status           VARCHAR(50)   NOT NULL,
    partner_status           VARCHAR(40)   NULL,
    booking_source           VARCHAR(20)   NOT NULL,
    remaining_payment_status VARCHAR(30)   NOT NULL,
    paid_ratio               DECIMAL(5,2)  NOT NULL,
    refund_ratio             DECIMAL(5,2)  NOT NULL,
    cancellation_fee_ratio   DECIMAL(5,2)  NOT NULL,
    created_offset           INT           NOT NULL,
    updated_offset           INT           NOT NULL,
    block_reason             VARCHAR(300)  NULL,
    note_template            VARCHAR(300)  NOT NULL,
    review_rating            INT           NULL,
    review_comment           VARCHAR(500)  NULL
);

INSERT INTO tmp_demo_filter_scenarios
    (suffix, booking_seq, user_email, room_slot, checkin_offset, checkout_offset,
     adults, children, booking_status, payment_option, payment_status,
     partner_status, booking_source, remaining_payment_status,
     paid_ratio, refund_ratio, cancellation_fee_ratio,
     created_offset, updated_offset, block_reason, note_template,
     review_rating, review_comment)
VALUES
    ('DEP-TODAY', '00231', 'user@travelmate.vn', 1, 0, 1, 2, 0,
     'CONFIRMED', 'DEPOSIT_30', 'APPROVED', 'PARTNER_CONFIRMED', 'ONLINE', 'UNPAID',
     0.30, 0.00, 0.00, -2, 0, NULL,
     'Khach da coc 30%, check-in hom nay va can doi tac thuc hien thao tac nhan phong/no-show.',
     NULL, NULL),
    ('FULL-TODAY', '00232', 'user2@travelmate.vn', 1, 0, 1, 2, 0,
     'CONFIRMED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED', 'ONLINE', 'NOT_REQUIRED',
     1.00, 0.00, 0.00, -3, 0, NULL,
     'Khach da thanh toan 100%, check-in hom nay va doi tac can chuan bi nhan phong.',
     NULL, NULL),
    ('CHECKED-IN', '00233', 'family@travelmate.vn', 1, -1, 1, 2, 2,
     'CHECKED_IN', 'DEPOSIT_30', 'APPROVED', 'PARTNER_CONFIRMED', 'ONLINE', 'PAID_AT_PROPERTY',
     0.30, 0.00, 0.00, -4, 0, NULL,
     'Khach da check-in va doi tac da xac nhan thu phan con lai tai co so.',
     NULL, NULL),
    ('COMPLETED', '00234', 'couple@travelmate.vn', 2, -3, -2, 2, 0,
     'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_COMPLETED', 'ONLINE', 'NOT_REQUIRED',
     1.00, 0.00, 0.00, -8, -2, NULL,
     'Khach da checkout, booking hoan tat va co danh gia sau luu tru.',
     9, 'Ky nghi gon gang, dung thong tin hien thi va nhan phong nhanh. Dich vu tai co so rat on dinh.'),
    ('NO-SHOW', '00235', 'user@travelmate.vn', 2, -1, 0, 2, 0,
     'NO_SHOW', 'DEPOSIT_30', 'DEPOSIT_FORFEITED', 'PARTNER_CONFIRMED', 'ONLINE', 'UNPAID',
     0.30, 0.00, 0.00, -3, 0, NULL,
     'Khach khong den trong ngay nhan phong, coc 30% duoc giu theo chinh sach.',
     NULL, NULL),
    ('CANCELLED', '00236', 'user2@travelmate.vn', 2, 1, 2, 2, 0,
     'CANCELLED', 'DEPOSIT_30', 'CANCELLED', NULL, 'ONLINE', 'NOT_REQUIRED',
     0.30, 0.00, 0.00, -1, -1, NULL,
     'Khach huy giao dich tren cong thanh toan, booking da huy va khong tao doanh thu.',
     NULL, NULL),
    ('REFUND', '00237', 'family@travelmate.vn', 3, 1, 2, 2, 1,
     'CANCELLED', 'FULL_PAYMENT', 'REFUND_PENDING', NULL, 'ONLINE', 'NOT_REQUIRED',
     1.00, 0.70, 0.30, -2, 0, NULL,
     'Khach huy sau khi da thanh toan 100%, Admin can ghi nhan hoan tien ngoai he thong.',
     NULL, NULL),
    ('DIRECT', '00238', 'couple@travelmate.vn', 2, 0, 1, 2, 0,
     'CONFIRMED', 'FULL_PAYMENT', 'NOT_REQUIRED', 'PARTNER_CONFIRMED', 'DIRECT', 'NOT_REQUIRED',
     1.00, 0.00, 0.00, -1, 0, NULL,
     'Khach dat truc tiep tai co so, doi tac ghi vao he thong de quan ly lich phong.',
     NULL, NULL),
    ('BLOCK', '00239', 'user@travelmate.vn', 2, 0, 2, 0, 0,
     'CONFIRMED', 'FULL_PAYMENT', 'NOT_REQUIRED', 'PARTNER_CONFIRMED', 'MANUAL_BLOCK', 'NOT_REQUIRED',
     0.00, 0.00, 0.00, -1, 0,
     'Bao tri dinh ky va giu phong noi bo trong 2 ngay.',
     'Doi tac chan phong/can de bao tri, khong phat sinh thanh toan hay doanh thu.',
     NULL, NULL),
    ('UPCOMING', '00240', 'user2@travelmate.vn', 3, 2, 3, 2, 0,
     'CONFIRMED', 'DEPOSIT_30', 'APPROVED', 'PARTNER_CONFIRMED', 'ONLINE', 'UNPAID',
     0.30, 0.00, 0.00, -1, 0, NULL,
     'Khach da coc 30%, TravelMate da tu dong giu phong/can cho ngay nhan phong sap toi.',
     NULL, NULL);

DROP TEMPORARY TABLE IF EXISTS tmp_demo_filter_booking_codes;
CREATE TEMPORARY TABLE tmp_demo_filter_booking_codes AS
SELECT CONCAT('BK-',
        CASE s.room_slot
            WHEN 1 THEN t.room_code_1
            WHEN 2 THEN t.room_code_2
            ELSE t.room_code_3
        END,
        '-', s.booking_seq
    ) AS booking_code
FROM tmp_demo_filter_types t
CROSS JOIN tmp_demo_filter_scenarios s;

-- Hoan lai quota neu file nay da tung duoc chay voi ma booking tu nhien.
UPDATE rooms r
JOIN (
    SELECT b.room_id, SUM(b.room_quantity) AS hold_qty
    FROM bookings b
    JOIN tmp_demo_filter_booking_codes c ON c.booking_code = b.booking_code
    WHERE b.booking_status NOT IN ('COMPLETED', 'CANCELLED', 'NO_SHOW')
    GROUP BY b.room_id
) old_natural ON old_natural.room_id = r.id
SET r.available_quantity = r.available_quantity + old_natural.hold_qty;

DELETE rv
FROM reviews rv
JOIN bookings b ON b.id = rv.booking_id
JOIN tmp_demo_filter_booking_codes c ON c.booking_code = b.booking_code;

DELETE p
FROM payments p
JOIN bookings b ON b.id = p.booking_id
JOIN tmp_demo_filter_booking_codes c ON c.booking_code = b.booking_code;

DELETE n
FROM notifications n
JOIN tmp_demo_filter_booking_codes c
  ON n.title LIKE CONCAT('%', c.booking_code, '%')
  OR n.message LIKE CONCAT('%', c.booking_code, '%')
  OR n.target_url LIKE CONCAT('%', c.booking_code, '%');

DELETE l
FROM admin_action_logs l
JOIN tmp_demo_filter_booking_codes c
  ON l.description LIKE CONCAT('%', c.booking_code, '%')
  OR l.note LIKE CONCAT('%', c.booking_code, '%');

DELETE b
FROM bookings b
JOIN tmp_demo_filter_booking_codes c ON c.booking_code = b.booking_code;

INSERT INTO bookings (
    booking_code, user_id, accommodation_id, room_id,
    check_in, check_out, adults, children, room_quantity,
    customer_name, customer_phone, customer_email,
    total_amount, paid_amount, remaining_amount,
    booking_status, payment_option, payment_status, partner_status,
    note, booking_source, block_reason, remaining_payment_status,
    remaining_paid_at, remaining_payment_note,
    total_before_discount,
    commission_rate_snapshot, commission_source_snapshot, commission_base_amount,
    commission_amount_snapshot, partner_voucher_amount_snapshot, admin_voucher_amount_snapshot,
    partner_payout_snapshot, online_paid_amount_snapshot, onsite_amount_snapshot,
    refund_amount, cancellation_fee, created_at, updated_at
)
SELECT
    CONCAT('BK-',
        CASE s.room_slot
            WHEN 1 THEN t.room_code_1
            WHEN 2 THEN t.room_code_2
            ELSE t.room_code_3
        END,
        '-', s.booking_seq
    ),
    u.id,
    a.id,
    r.id,
    DATE_ADD(CURDATE(), INTERVAL s.checkin_offset DAY),
    DATE_ADD(CURDATE(), INTERVAL s.checkout_offset DAY),
    s.adults,
    s.children,
    1,
    CASE s.user_email
        WHEN 'family@travelmate.vn' THEN 'Gia đình Minh Anh'
        WHEN 'couple@travelmate.vn' THEN 'Linh & Khánh'
        WHEN 'user2@travelmate.vn' THEN 'Trần Thị Mai'
        ELSE 'Nguyễn Văn An'
    END,
    CASE s.user_email
        WHEN 'family@travelmate.vn' THEN '0968 111 222'
        WHEN 'couple@travelmate.vn' THEN '0979 333 444'
        WHEN 'user2@travelmate.vn' THEN '0923 456 789'
        ELSE '0912 345 678'
    END,
    CASE WHEN s.booking_source = 'MANUAL_BLOCK' THEN t.partner_email ELSE s.user_email END,
    CASE
        WHEN s.booking_source = 'MANUAL_BLOCK' THEN 0
        ELSE ROUND(r.price_per_night * DATEDIFF(
            DATE_ADD(CURDATE(), INTERVAL s.checkout_offset DAY),
            DATE_ADD(CURDATE(), INTERVAL s.checkin_offset DAY)
        ), 0)
    END,
    CASE
        WHEN s.booking_source = 'MANUAL_BLOCK' THEN 0
        WHEN s.booking_source = 'DIRECT' THEN ROUND(r.price_per_night * DATEDIFF(
            DATE_ADD(CURDATE(), INTERVAL s.checkout_offset DAY),
            DATE_ADD(CURDATE(), INTERVAL s.checkin_offset DAY)
        ), 0)
        WHEN s.payment_status = 'CANCELLED' THEN 0
        ELSE ROUND(r.price_per_night * DATEDIFF(
            DATE_ADD(CURDATE(), INTERVAL s.checkout_offset DAY),
            DATE_ADD(CURDATE(), INTERVAL s.checkin_offset DAY)
        ) * s.paid_ratio, 0)
    END,
    CASE
        WHEN s.booking_source IN ('DIRECT', 'MANUAL_BLOCK') THEN 0
        WHEN s.payment_option = 'DEPOSIT_30'
             AND s.payment_status IN ('APPROVED', 'DEPOSIT_FORFEITED')
            THEN ROUND(r.price_per_night * DATEDIFF(
                DATE_ADD(CURDATE(), INTERVAL s.checkout_offset DAY),
                DATE_ADD(CURDATE(), INTERVAL s.checkin_offset DAY)
            ) * (1 - s.paid_ratio), 0)
        ELSE 0
    END,
    s.booking_status,
    s.payment_option,
    s.payment_status,
    s.partner_status,
    CONCAT('[', t.type_label, '] ', s.note_template),
    s.booking_source,
    s.block_reason,
    s.remaining_payment_status,
    CASE WHEN s.remaining_payment_status = 'PAID_AT_PROPERTY' THEN DATE_SUB(NOW(), INTERVAL 4 HOUR) ELSE NULL END,
    CASE WHEN s.remaining_payment_status = 'PAID_AT_PROPERTY' THEN 'Doi tac da thu du phan con lai tai co so.' ELSE NULL END,
    CASE
        WHEN s.booking_source = 'MANUAL_BLOCK' THEN 0
        ELSE ROUND(r.price_per_night * DATEDIFF(
            DATE_ADD(CURDATE(), INTERVAL s.checkout_offset DAY),
            DATE_ADD(CURDATE(), INTERVAL s.checkin_offset DAY)
        ), 0)
    END,
    0.0000, 'NOT_APPLICABLE', 0,
    0, 0, 0,
    0, 0, 0,
    CASE
        WHEN s.refund_ratio > 0 THEN ROUND(r.price_per_night * DATEDIFF(
            DATE_ADD(CURDATE(), INTERVAL s.checkout_offset DAY),
            DATE_ADD(CURDATE(), INTERVAL s.checkin_offset DAY)
        ) * s.refund_ratio, 0)
        ELSE 0
    END,
    CASE
        WHEN s.cancellation_fee_ratio > 0 THEN ROUND(r.price_per_night * DATEDIFF(
            DATE_ADD(CURDATE(), INTERVAL s.checkout_offset DAY),
            DATE_ADD(CURDATE(), INTERVAL s.checkin_offset DAY)
        ) * s.cancellation_fee_ratio, 0)
        ELSE 0
    END,
    DATE_ADD(NOW(), INTERVAL s.created_offset DAY),
    DATE_ADD(NOW(), INTERVAL s.updated_offset DAY)
FROM tmp_demo_filter_types t
CROSS JOIN tmp_demo_filter_scenarios s
JOIN accommodations a ON a.name = t.acc_name
JOIN rooms r ON r.accommodation_id = a.id
    AND r.room_code = CASE s.room_slot
        WHEN 1 THEN t.room_code_1
        WHEN 2 THEN t.room_code_2
        ELSE t.room_code_3
    END
JOIN users u ON u.email = CASE
    WHEN s.booking_source = 'MANUAL_BLOCK' THEN t.partner_email
    ELSE s.user_email
END;

-- Cap nhat snapshot tai chinh: DIRECT/BLOCK khong di vao doanh thu TravelMate.
UPDATE bookings b
JOIN tmp_demo_filter_booking_codes c ON c.booking_code = b.booking_code
JOIN rooms r ON r.id = b.room_id
JOIN accommodations a ON a.id = b.accommodation_id
SET
    b.commission_rate_snapshot = CASE
        WHEN b.booking_source = 'ONLINE' THEN COALESCE(r.commission_rate_override / 100,
            CASE a.property_type
                WHEN 'HOTEL' THEN 0.1500
                WHEN 'RESORT' THEN 0.1800
                WHEN 'VILLA' THEN 0.1200
                WHEN 'HOMESTAY' THEN 0.1000
                ELSE 0.1500
            END)
        ELSE 0.0000
    END,
    b.commission_source_snapshot = CASE
        WHEN b.booking_source <> 'ONLINE' THEN 'NOT_APPLICABLE'
        WHEN r.commission_rate_override IS NOT NULL THEN 'ROOM_OVERRIDE'
        ELSE 'PROPERTY_TYPE_DEFAULT'
    END,
    b.commission_base_amount = CASE WHEN b.booking_source = 'ONLINE' THEN b.total_before_discount ELSE 0 END,
    b.commission_amount_snapshot = CASE
        WHEN b.booking_source = 'ONLINE' THEN ROUND(b.total_before_discount * COALESCE(r.commission_rate_override / 100,
            CASE a.property_type
                WHEN 'HOTEL' THEN 0.1500
                WHEN 'RESORT' THEN 0.1800
                WHEN 'VILLA' THEN 0.1200
                WHEN 'HOMESTAY' THEN 0.1000
                ELSE 0.1500
            END), 0)
        ELSE 0
    END,
    b.online_paid_amount_snapshot = CASE
        WHEN b.booking_source = 'ONLINE'
         AND b.payment_status IN ('APPROVED', 'DEPOSIT_FORFEITED', 'REFUND_PENDING')
            THEN b.paid_amount
        ELSE 0
    END,
    b.onsite_amount_snapshot = CASE
        WHEN b.remaining_payment_status = 'PAID_AT_PROPERTY' THEN b.remaining_amount
        ELSE 0
    END;

UPDATE bookings b
JOIN tmp_demo_filter_booking_codes c ON c.booking_code = b.booking_code
SET b.partner_payout_snapshot = CASE
    WHEN b.booking_source <> 'ONLINE' THEN 0
    WHEN b.payment_status IN ('APPROVED', 'DEPOSIT_FORFEITED')
        THEN GREATEST(b.paid_amount - b.commission_amount_snapshot, 0)
    ELSE 0
END;

INSERT INTO payments (
    booking_id, payment_method, payment_option, amount,
    transaction_code, payment_status, paid_at, approved_at, note,
    gateway, vnp_txn_ref, vnp_response_code, vnp_transaction_status,
    confirmed_from_gateway_at
)
SELECT
    b.id,
    'VNPAY',
    b.payment_option,
    CASE
        WHEN b.payment_status = 'CANCELLED' THEN ROUND(b.total_amount * 0.30, 0)
        ELSE b.paid_amount
    END,
    CONCAT('TXN-', b.booking_code),
    b.payment_status,
    CASE
        WHEN b.payment_status IN ('APPROVED', 'DEPOSIT_FORFEITED', 'REFUND_PENDING')
            THEN DATE_SUB(NOW(), INTERVAL 2 HOUR)
        ELSE NULL
    END,
    CASE
        WHEN b.payment_status IN ('APPROVED', 'DEPOSIT_FORFEITED', 'REFUND_PENDING')
            THEN DATE_SUB(NOW(), INTERVAL 2 HOUR)
        ELSE NULL
    END,
    CASE b.payment_status
        WHEN 'APPROVED' THEN 'VNPAY ghi nhan thanh cong, TravelMate da giu phong/can tren he thong.'
        WHEN 'DEPOSIT_FORFEITED' THEN 'Coc 30% bi giu do khach khong den theo chinh sach.'
        WHEN 'REFUND_PENDING' THEN 'Giao dich da thanh toan, dang cho Admin ghi nhan ket qua hoan tien.'
        WHEN 'CANCELLED' THEN 'Khach huy giao dich tren cong thanh toan, khong phat sinh doanh thu.'
        ELSE 'Giao dich online duoc ghi nhan thanh cong.'
    END,
    'VNPAY',
    CONCAT('TM-', b.booking_code),
    CASE WHEN b.payment_status = 'CANCELLED' THEN '24' ELSE '00' END,
    CASE WHEN b.payment_status = 'CANCELLED' THEN '02' ELSE '00' END,
    CASE
        WHEN b.payment_status IN ('APPROVED', 'DEPOSIT_FORFEITED', 'REFUND_PENDING')
            THEN DATE_SUB(NOW(), INTERVAL 2 HOUR)
        ELSE NULL
    END
FROM bookings b
JOIN tmp_demo_filter_booking_codes c ON c.booking_code = b.booking_code
WHERE b.booking_source = 'ONLINE';

INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, is_hidden, created_at)
SELECT
    b.user_id,
    b.accommodation_id,
    b.id,
    s.review_rating,
    CONCAT(s.review_comment, ' ', t.type_label, ' nay mang lai trai nghiem dung ky vong.'),
    0,
    DATE_ADD(b.check_out, INTERVAL 1 DAY)
FROM bookings b
JOIN tmp_demo_filter_booking_codes c ON c.booking_code = b.booking_code
CROSS JOIN tmp_demo_filter_types t
JOIN tmp_demo_filter_scenarios s
  ON b.booking_code = CONCAT('BK-',
        CASE s.room_slot
            WHEN 1 THEN t.room_code_1
            WHEN 2 THEN t.room_code_2
            ELSE t.room_code_3
        END,
        '-', s.booking_seq
    )
WHERE s.review_rating IS NOT NULL;

INSERT INTO notifications (user_id, title, message, type, target_url, is_read, created_at)
SELECT
    b.user_id,
    CONCAT('Booking ', b.booking_code, ' da duoc cap nhat'),
    CONCAT('Trang thai hien tai: ', b.booking_status, ' - ', a.name, '.'),
    CASE WHEN b.booking_status = 'COMPLETED' THEN 'REVIEW_REMINDER' ELSE 'BOOKING_CONFIRMED' END,
    CONCAT('/my-bookings?code=', b.booking_code),
    0,
    DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM bookings b
JOIN tmp_demo_filter_booking_codes c ON c.booking_code = b.booking_code
JOIN accommodations a ON a.id = b.accommodation_id
WHERE b.booking_source = 'ONLINE'
  AND b.booking_status IN ('CONFIRMED', 'CHECKED_IN', 'COMPLETED');

INSERT INTO notifications (user_id, title, message, type, target_url, is_read, created_at)
SELECT
    owner.id,
    CONCAT('Can xu ly ', b.booking_code),
    CONCAT('Booking ', b.booking_code, ' tai ', a.name, ' dang nam trong nhom can theo doi cua doi tac.'),
    'BOOKING_CHECKIN_READY',
    CONCAT('/partner/bookings?filter=needs-action&code=', b.booking_code),
    0,
    DATE_SUB(NOW(), INTERVAL 30 MINUTE)
FROM bookings b
JOIN tmp_demo_filter_booking_codes c ON c.booking_code = b.booking_code
JOIN accommodations a ON a.id = b.accommodation_id
JOIN users owner ON owner.id = a.owner_id
WHERE b.booking_status IN ('CONFIRMED', 'CHECKED_IN')
  AND b.booking_source IN ('ONLINE', 'DIRECT');

INSERT INTO notifications (user_id, title, message, type, target_url, is_read, created_at)
SELECT
    admin_user.id,
    CONCAT('Admin can xem ', b.booking_code),
    CONCAT('Booking ', b.booking_code, ' co payment_status=', b.payment_status, ', can Admin theo doi va xu ly dung quy trinh.'),
    'SYSTEM',
    CONCAT('/admin/bookings?code=', b.booking_code),
    0,
    DATE_SUB(NOW(), INTERVAL 20 MINUTE)
FROM bookings b
JOIN tmp_demo_filter_booking_codes c ON c.booking_code = b.booking_code
JOIN users admin_user ON admin_user.email = 'admin@travelmate.vn'
WHERE b.payment_status = 'REFUND_PENDING';

INSERT INTO admin_action_logs (admin_email, action_type, target_type, target_id, description, note, created_at)
SELECT
    'admin@travelmate.vn',
    CASE
        WHEN b.booking_source = 'DIRECT' THEN 'CREATE_DIRECT_BOOKING'
        WHEN b.booking_source = 'MANUAL_BLOCK' THEN 'CREATE_ROOM_BLOCK'
        WHEN b.payment_status = 'REFUND_PENDING' THEN 'FLAG_REFUND'
        WHEN b.booking_status = 'CHECKED_IN' THEN 'CHECKIN_BOOKING'
        WHEN b.booking_status = 'COMPLETED' THEN 'COMPLETE_BOOKING'
        WHEN b.booking_status = 'NO_SHOW' THEN 'NOSHOW_BOOKING'
        WHEN b.booking_status = 'CANCELLED' THEN 'CANCEL_BOOKING'
        ELSE 'APPROVE_BOOKING'
    END,
    'BOOKING',
    b.id,
    CONCAT('Ghi nhan booking van hanh ', b.booking_code, ' - ', a.name),
    CONCAT('Nguon=', b.booking_source, ', booking_status=', b.booking_status, ', payment_status=', b.payment_status),
    b.updated_at
FROM bookings b
JOIN tmp_demo_filter_booking_codes c ON c.booking_code = b.booking_code
JOIN accommodations a ON a.id = b.accommodation_id;

-- Tru quota cho cac booking dang giu phong/can trong bo data moi.
UPDATE rooms r
JOIN (
    SELECT b.room_id, SUM(b.room_quantity) AS hold_qty
    FROM bookings b
    JOIN tmp_demo_filter_booking_codes c ON c.booking_code = b.booking_code
    WHERE b.booking_status NOT IN ('COMPLETED', 'CANCELLED', 'NO_SHOW')
    GROUP BY b.room_id
) new_demo ON new_demo.room_id = r.id
SET r.available_quantity = GREATEST(r.available_quantity - new_demo.hold_qty, 0);

DROP TEMPORARY TABLE IF EXISTS tmp_demo_filter_booking_codes;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_filter_scenarios;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_filter_types;

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS_DEMO_FILTER;
SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES_DEMO_FILTER;

-- Kiem tra nhanh sau khi chay:
-- SELECT a.property_type, COUNT(*) AS total_rows
-- FROM bookings b JOIN accommodations a ON a.id = b.accommodation_id
-- WHERE b.booking_code REGEXP '^BK-(TMG|TCR|TRV|CDF)-[A-Z]+-002(3[1-9]|40)$'
-- GROUP BY a.property_type
-- ORDER BY a.property_type;
