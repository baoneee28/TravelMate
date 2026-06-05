-- =======================================================================
-- TRAVELMATE - KIEM TRA NHANH DU LIEU BOOKING FILTER
-- =======================================================================
-- Chay sau:
--   1) src/main/resources/travelmate_db.sql
--   2) src/main/resources/demo_booking_filter_seed.sql
-- =======================================================================

USE travelmate_db;

DROP TEMPORARY TABLE IF EXISTS tmp_booking_filter_check_codes;
CREATE TEMPORARY TABLE tmp_booking_filter_check_codes (
    booking_code VARCHAR(50) PRIMARY KEY
);

INSERT INTO tmp_booking_filter_check_codes (booking_code) VALUES
    ('BK-TMG-DLX-00231'), ('BK-TMG-DLX-00232'), ('BK-TMG-DLX-00233'),
    ('BK-TMG-PRE-00234'), ('BK-TMG-PRE-00235'), ('BK-TMG-PRE-00236'),
    ('BK-TMG-FAM-00237'), ('BK-TMG-PRE-00238'), ('BK-TMG-PRE-00239'), ('BK-TMG-FAM-00240'),
    ('BK-TCR-GDN-00231'), ('BK-TCR-GDN-00232'), ('BK-TCR-GDN-00233'),
    ('BK-TCR-LAK-00234'), ('BK-TCR-LAK-00235'), ('BK-TCR-LAK-00236'),
    ('BK-TCR-FAM-00237'), ('BK-TCR-LAK-00238'), ('BK-TCR-LAK-00239'), ('BK-TCR-FAM-00240'),
    ('BK-TRV-DLX-00231'), ('BK-TRV-DLX-00232'), ('BK-TRV-DLX-00233'),
    ('BK-TRV-FAM-00234'), ('BK-TRV-FAM-00235'), ('BK-TRV-FAM-00236'),
    ('BK-TRV-SUI-00237'), ('BK-TRV-FAM-00238'), ('BK-TRV-FAM-00239'), ('BK-TRV-SUI-00240'),
    ('BK-CDF-STD-00231'), ('BK-CDF-STD-00232'), ('BK-CDF-STD-00233'),
    ('BK-CDF-DLX-00234'), ('BK-CDF-DLX-00235'), ('BK-CDF-DLX-00236'),
    ('BK-CDF-FAM-00237'), ('BK-CDF-DLX-00238'), ('BK-CDF-DLX-00239'), ('BK-CDF-FAM-00240');

-- 1. Tong quan: ky vong 4 loai hinh x 10 booking = 40.
SELECT
    a.property_type,
    COUNT(*) AS total_bookings,
    SUM(b.booking_source = 'ONLINE') AS online_rows,
    SUM(b.booking_source = 'DIRECT') AS direct_rows,
    SUM(b.booking_source = 'MANUAL_BLOCK') AS block_rows
FROM tmp_booking_filter_check_codes c
JOIN bookings b ON b.booking_code = c.booking_code
JOIN accommodations a ON a.id = b.accommodation_id
GROUP BY a.property_type
ORDER BY a.property_type;

-- 2. Phu trang thai Admin/Partner, khong co dong cho thanh toan trong nhom nay.
SELECT
    b.booking_status,
    b.payment_status,
    b.payment_option,
    b.booking_source,
    b.remaining_payment_status,
    COUNT(*) AS total_rows
FROM tmp_booking_filter_check_codes c
JOIN bookings b ON b.booking_code = c.booking_code
GROUP BY b.booking_status, b.payment_status, b.payment_option, b.booking_source, b.remaining_payment_status
ORDER BY b.booking_status, b.payment_status, b.booking_source;

-- 3. Doi chieu tung booking voi partner so huu co so.
SELECT
    b.booking_code,
    a.property_type,
    owner.email AS partner_email,
    a.name AS accommodation_name,
    r.room_code,
    b.check_in,
    b.check_out,
    b.booking_status,
    b.payment_status,
    b.partner_status,
    b.booking_source
FROM tmp_booking_filter_check_codes c
JOIN bookings b ON b.booking_code = c.booking_code
JOIN accommodations a ON a.id = b.accommodation_id
JOIN rooms r ON r.id = b.room_id
JOIN users owner ON owner.id = a.owner_id
ORDER BY a.property_type, b.booking_code;

-- 4. Direct/block khong di vao doanh thu TravelMate.
SELECT
    b.booking_code,
    b.booking_source,
    b.total_amount,
    b.paid_amount,
    b.commission_amount_snapshot,
    b.partner_payout_snapshot,
    b.online_paid_amount_snapshot,
    b.onsite_amount_snapshot
FROM tmp_booking_filter_check_codes c
JOIN bookings b ON b.booking_code = c.booking_code
WHERE b.booking_source IN ('DIRECT', 'MANUAL_BLOCK')
ORDER BY b.booking_code;

-- 5. Du lieu lien quan: payment/review/notification/log.
SELECT
    (SELECT COUNT(*) FROM payments p JOIN bookings b ON b.id = p.booking_id JOIN tmp_booking_filter_check_codes c ON c.booking_code = b.booking_code) AS payment_rows,
    (SELECT COUNT(*) FROM reviews rv JOIN bookings b ON b.id = rv.booking_id JOIN tmp_booking_filter_check_codes c ON c.booking_code = b.booking_code) AS review_rows,
    (SELECT COUNT(*) FROM notifications n JOIN tmp_booking_filter_check_codes c
        ON n.title LIKE CONCAT('%', c.booking_code, '%')
        OR n.message LIKE CONCAT('%', c.booking_code, '%')
        OR n.target_url LIKE CONCAT('%', c.booking_code, '%')) AS notification_rows,
    (SELECT COUNT(*) FROM admin_action_logs l JOIN tmp_booking_filter_check_codes c
        ON l.description LIKE CONCAT('%', c.booking_code, '%')
        OR l.note LIKE CONCAT('%', c.booking_code, '%')) AS admin_log_rows;

DROP TEMPORARY TABLE IF EXISTS tmp_booking_filter_check_codes;
