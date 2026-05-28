-- ====================================================================
-- DỮ LIỆU ĐỐI SOÁT & QUYẾT TOÁN THỬ NGHIỆM
-- ====================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Dọn dẹp dữ liệu thử nghiệm cũ
DELETE FROM partner_wallet_transactions WHERE transaction_code LIKE 'WTX-PL-%' OR transaction_code LIKE 'WTX-RT-%' OR partner_id IN (SELECT id FROM users WHERE email IN ('contact@dalatpalacehotel.vn', 'info@rungthongdalat.vn'));
DELETE FROM partner_withdrawal_requests WHERE request_code LIKE 'WD-PL-%' OR partner_id IN (SELECT id FROM users WHERE email IN ('contact@dalatpalacehotel.vn', 'info@rungthongdalat.vn'));
DELETE FROM partner_wallets WHERE partner_id IN (SELECT id FROM users WHERE email IN ('contact@dalatpalacehotel.vn', 'info@rungthongdalat.vn'));
DELETE FROM partner_settlements WHERE partner_id IN (SELECT id FROM users WHERE email IN ('contact@dalatpalacehotel.vn', 'info@rungthongdalat.vn'));
DELETE FROM payments WHERE transaction_code IN ('TXN-PL-DIRECT-001', 'TXN-PL-PENDING-001', 'TXN-PL-FULL-001', 'TXN-PL-NOSHOW-001');
DELETE FROM bookings WHERE booking_code LIKE 'BK-PL-%';
DELETE FROM rooms WHERE room_code = 'PL-STD-01';
DELETE FROM accommodations WHERE name = 'Đà Lạt Palace Hotel';
DELETE FROM users WHERE email IN ('nguyenhuuan92@gmail.com', 'contact@dalatpalacehotel.vn', 'info@rungthongdalat.vn');

SET FOREIGN_KEY_CHECKS = 1;

-- 2. Định nghĩa biến và nạp dữ liệu thử nghiệm mới
SET @admin_id = (SELECT id FROM users WHERE email = 'admin@travelmate.vn' LIMIT 1);

INSERT INTO users (email, password, full_name, name, phone, role, status, partner_property_type,
                bank_account_number, bank_name, bank_account_holder, bank_branch,
                created_at, updated_at) VALUES
('nguyenhuuan92@gmail.com',
 '$2a$10$FsFOdcKQPqCnlIPA3j82ZeY7R6tMpdhavFO2bfqWUfJqF1Z4nloO2',
 'Nguyễn Hữu An', 'Nguyễn Hữu An', '0908 111 222', 'USER', 'ACTIVE', NULL,
 NULL, NULL, NULL, NULL, NOW(), NOW()),
('contact@dalatpalacehotel.vn',
 '$2a$10$dCikCIiksr/Ne1Xpv40vKOMFwpiY751Cb1kdIVDwobiMzImbLC3oq',
 'Khách Sạn Palace Đà Lạt', 'Khách Sạn Palace Đà Lạt', '0908 333 444', 'PARTNER', 'ACTIVE', 'HOTEL',
 '123456789012', 'Vietcombank', 'KHACH SAN PALACE DA LAT', 'CN Đà Lạt', NOW(), NOW()),
('info@rungthongdalat.vn',
 '$2a$10$dCikCIiksr/Ne1Xpv40vKOMFwpiY751Cb1kdIVDwobiMzImbLC3oq',
 'Homestay Rừng Thông Đà Lạt', 'Homestay Rừng Thông Đà Lạt', '0908 555 666', 'PARTNER', 'ACTIVE', 'HOTEL',
 NULL, NULL, NULL, NULL, NOW(), NOW());

SET @wallet_user_id = (SELECT id FROM users WHERE email = 'nguyenhuuan92@gmail.com' LIMIT 1);
SET @wallet_partner_id = (SELECT id FROM users WHERE email = 'contact@dalatpalacehotel.vn' LIMIT 1);
SET @wallet_nobank_partner_id = (SELECT id FROM users WHERE email = 'info@rungthongdalat.vn' LIMIT 1);

INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
VALUES (
    'Đà Lạt Palace Hotel',
    'Khách sạn Palace Đà Lạt mang vẻ đẹp cổ điển châu Âu sang trọng, tọa lạc ngay trung tâm thành phố Đà Lạt, mang lại trải nghiệm nghỉ dưỡng hoàn hảo.',
    '12 Trần Phú, Phường 3',
    'Đà Lạt',
    'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&q=80',
    4, 9.1, 12, 'HOTEL', 'APPROVED', @wallet_partner_id, NOW(), NOW()
);
SET @wallet_acc_id = (SELECT id FROM accommodations WHERE name = 'Đà Lạt Palace Hotel' LIMIT 1);

INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night,
                   available_quantity, description, image_url, room_category, commission_rate_override)
VALUES (
    @wallet_acc_id, 'PL-STD-01', 'Phòng Standard Double', '1 giường Queen',
    2, 850000, 8,
    'Phòng Standard rộng rãi, trang bị đầy đủ tiện nghi hiện đại, hướng ra đồi thông hoặc thành phố.',
    'https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=400&q=70',
    'STANDARD', NULL
);
SET @wallet_room_id = (SELECT id FROM rooms WHERE room_code = 'PL-STD-01' LIMIT 1);

-- 2 booking đủ điều kiện đối soát kỳ tháng 04/2026
INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
    check_in, check_out, adults, children, room_quantity,
    customer_name, customer_phone, customer_email,
    total_amount, paid_amount, remaining_amount,
    booking_status, payment_option, payment_status, partner_status,
    note, voucher_code, discount_amount, voucher_cost_bearer, total_before_discount,
    booking_source, created_at, updated_at)
VALUES
('BK-PL-FULL-001', @wallet_user_id, @wallet_acc_id, @wallet_room_id,
 '2026-04-11', '2026-04-13', 2, 0, 1,
 'Nguyễn Hữu An', '0908 111 222', 'nguyenhuuan92@gmail.com',
 1500000, 1500000, 0,
 'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
 'Khách yêu cầu nhận phòng sớm lúc 11h.',
 'WELCOME200', 200000, 'PARTNER', 1700000,
 'ONLINE', '2026-04-10 09:00:00', '2026-04-13 12:00:00'),
('BK-PL-NOSHOW-001', @wallet_user_id, @wallet_acc_id, @wallet_room_id,
 '2026-04-18', '2026-04-20', 2, 0, 1,
 'Nguyễn Hữu An', '0908 111 222', 'nguyenhuuan92@gmail.com',
 2500000, 750000, 1750000,
 'NO_SHOW', 'DEPOSIT_30', 'DEPOSIT_FORFEITED', NULL,
 'Khách đi công tác cần xuất hóa đơn VAT.',
 NULL, 0, NULL, 2500000,
 'ONLINE', '2026-04-17 09:00:00', '2026-04-20 12:00:00');

INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code,
                      payment_status, paid_at, approved_at, note)
SELECT id, 'VNPAY', 'FULL_PAYMENT', 1500000, 'TXN-PL-FULL-001',
       'APPROVED', '2026-04-10 09:05:00', '2026-04-10 09:10:00',
       'Thanh toán qua cổng VNPAY thành công.'
FROM bookings WHERE booking_code = 'BK-PL-FULL-001';

INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code,
                      payment_status, paid_at, approved_at, note)
SELECT id, 'VNPAY', 'DEPOSIT_30', 750000, 'TXN-PL-NOSHOW-001',
       'DEPOSIT_FORFEITED', '2026-04-18 09:05:00', '2026-04-20 12:10:00',
       'Thanh toán cọc 30% qua VNPAY.'
FROM bookings WHERE booking_code = 'BK-PL-NOSHOW-001';

-- 2 booking không đủ điều kiện đối soát
INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
    check_in, check_out, adults, children, room_quantity,
    customer_name, customer_phone, customer_email,
    total_amount, paid_amount, remaining_amount,
    booking_status, payment_option, payment_status, partner_status,
    note, voucher_code, discount_amount, voucher_cost_bearer, total_before_discount,
    booking_source, created_at, updated_at)
VALUES
('BK-PL-DIRECT-001', @wallet_user_id, @wallet_acc_id, @wallet_room_id,
 '2026-04-22', '2026-04-24', 2, 0, 1,
 'Nguyễn Hữu An', '0908 111 222', 'nguyenhuuan92@gmail.com',
 900000, 900000, 0,
 'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
 'Đã thanh toán bằng tiền mặt.',
 NULL, 0, NULL, 900000,
 'DIRECT', '2026-04-22 09:00:00', '2026-04-24 11:00:00'),
('BK-PL-PENDING-001', @wallet_user_id, @wallet_acc_id, @wallet_room_id,
 '2026-04-27', '2026-04-29', 2, 0, 1,
 'Nguyễn Hữu An', '0908 111 222', 'nguyenhuuan92@gmail.com',
 1000000, 1000000, 0,
 'CONFIRMED', 'FULL_PAYMENT', 'APPROVED', 'PENDING_PARTNER_CONFIRMATION',
 'Khách yêu cầu thêm nước suối.',
 NULL, 0, NULL, 1000000,
 'ONLINE', '2026-04-26 09:00:00', '2026-04-26 09:30:00');

INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code,
                      payment_status, paid_at, approved_at, note)
SELECT id, 'VNPAY', 'FULL_PAYMENT', 900000, 'TXN-PL-DIRECT-001',
       'APPROVED', '2026-04-22 09:05:00', '2026-04-22 09:10:00',
       'Thanh toán trực tiếp tại quầy.'
FROM bookings WHERE booking_code = 'BK-PL-DIRECT-001';

INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code,
                      payment_status, paid_at, approved_at, note)
SELECT id, 'VNPAY', 'FULL_PAYMENT', 1000000, 'TXN-PL-PENDING-001',
       'APPROVED', '2026-04-26 09:05:00', '2026-04-26 09:10:00',
       'Thanh toán qua cổng VNPAY thành công.'
FROM bookings WHERE booking_code = 'BK-PL-PENDING-001';

-- Quyết toán cũ để đối tác có ví và lịch sử giao dịch ngay khi đăng nhập
INSERT INTO partner_settlements
    (partner_id, period_start, period_end,
     gross_amount, commission_amount, voucher_deduction_amount, payout_amount,
     scheduled_payout_date, settlement_status, settlement_date, note, created_at)
VALUES
(@wallet_partner_id, '2026-03-01', '2026-03-31',
 4800000, 720000, 80000, 4000000,
 '2026-04-10', 'PAID', '2026-04-10 10:00:00',
 'Thanh toán doanh thu tháng 03/2026.',
 '2026-03-31 18:30:00'),
(@wallet_nobank_partner_id, '2026-03-01', '2026-03-31',
 1200000, 180000, 20000, 1000000,
 '2026-04-10', 'PAID', '2026-04-10 10:05:00',
 'Thanh toán doanh thu tháng 03/2026.',
 '2026-03-31 18:35:00');

SET @wallet_paid_settlement_id = (
    SELECT id FROM partner_settlements
    WHERE partner_id = @wallet_partner_id
      AND period_start = '2026-03-01'
      AND period_end = '2026-03-31'
    LIMIT 1
);
SET @wallet_nobank_settlement_id = (
    SELECT id FROM partner_settlements
    WHERE partner_id = @wallet_nobank_partner_id
      AND period_start = '2026-03-01'
      AND period_end = '2026-03-31'
    LIMIT 1
);

INSERT INTO partner_wallets
    (partner_id, available_balance, pending_withdrawal_amount,
     total_earned_amount, total_withdrawn_amount, updated_at)
VALUES
(@wallet_partner_id, 2200000, 800000, 4000000, 1000000, NOW()),
(@wallet_nobank_partner_id, 1000000, 0, 1000000, 0, NOW());

INSERT INTO partner_withdrawal_requests
    (partner_id, request_code, amount,
     bank_name, bank_account_number, bank_account_holder, bank_branch,
     withdrawal_status, requested_at, processed_at, processed_by_admin_id, admin_note)
VALUES
(@wallet_partner_id, 'WD-PL-PENDING-001', 800000,
 'Vietcombank', '123456789012', 'KHACH SAN PALACE DA LAT', 'CN Đà Lạt',
 'PENDING', '2026-05-18 09:00:00', NULL, NULL, NULL),
(@wallet_partner_id, 'WD-PL-PAID-001', 1000000,
 'Vietcombank', '123456789012', 'KHACH SAN PALACE DA LAT', 'CN Đà Lạt',
 'PAID', '2026-05-12 09:00:00', '2026-05-12 15:00:00', @admin_id,
 'Admin đã thực hiện chuyển khoản thanh toán qua dịch vụ Internet Banking.'),
(@wallet_partner_id, 'WD-PL-REJECTED-001', 500000,
 'Vietcombank', '123456789012', 'KHACH SAN PALACE DA LAT', 'CN Đà Lạt',
 'REJECTED', '2026-05-14 09:00:00', '2026-05-14 15:00:00', @admin_id,
 'Thông tin tài khoản ngân hàng không chính xác, hoàn tiền về ví đối tác.');

SET @wd_wl_pending_id = (SELECT id FROM partner_withdrawal_requests WHERE request_code = 'WD-PL-PENDING-001' LIMIT 1);
SET @wd_wl_paid_id = (SELECT id FROM partner_withdrawal_requests WHERE request_code = 'WD-PL-PAID-001' LIMIT 1);
SET @wd_wl_rejected_id = (SELECT id FROM partner_withdrawal_requests WHERE request_code = 'WD-PL-REJECTED-001' LIMIT 1);

INSERT INTO partner_wallet_transactions
    (partner_id, settlement_id, withdrawal_request_id, transaction_code,
     transaction_type, direction, amount, balance_before, balance_after,
     description, created_at, created_by_admin_id)
VALUES
(@wallet_partner_id, @wallet_paid_settlement_id, NULL, 'WTX-PL-STL-CREDIT-001',
 'SETTLEMENT_CREDIT', 'IN', 4000000, 0, 4000000,
 'Cộng doanh thu quyết toán kỳ tháng 03/2026 vào ví đối tác.', '2026-04-10 10:00:00', @admin_id),
(@wallet_partner_id, NULL, @wd_wl_pending_id, 'WTX-PL-WDREQ-PENDING-001',
 'WITHDRAWAL_REQUEST', 'OUT', 800000, 4000000, 3200000,
 'Rút tiền về tài khoản ngân hàng.', '2026-05-18 09:00:00', NULL),
(@wallet_partner_id, NULL, @wd_wl_paid_id, 'WTX-PL-WDREQ-PAID-001',
 'WITHDRAWAL_REQUEST', 'OUT', 1000000, 3200000, 2200000,
 'Rút tiền về tài khoản ngân hàng.', '2026-05-12 09:00:00', NULL),
(@wallet_partner_id, NULL, @wd_wl_paid_id, 'WTX-PL-WDPAID-001',
 'WITHDRAWAL_PAID', 'INFO', 1000000, 2200000, 2200000,
 'Chuyển khoản thanh toán thành công.', '2026-05-12 15:00:00', @admin_id),
(@wallet_partner_id, NULL, @wd_wl_rejected_id, 'WTX-PL-WDREQ-REJECTED-001',
 'WITHDRAWAL_REQUEST', 'OUT', 500000, 2200000, 1700000,
 'Rút tiền về tài khoản ngân hàng.', '2026-05-14 09:00:00', NULL),
(@wallet_partner_id, NULL, @wd_wl_rejected_id, 'WTX-PL-WDREJECTED-001',
 'WITHDRAWAL_REJECTED', 'IN', 500000, 1700000, 2200000,
 'Yêu cầu rút tiền bị từ chối.', '2026-05-14 15:00:00', @admin_id),
(@wallet_nobank_partner_id, @wallet_nobank_settlement_id, NULL, 'WTX-RT-STL-CREDIT-001',
 'SETTLEMENT_CREDIT', 'IN', 1000000, 0, 1000000,
 'Quyết toán tháng 03/2026.', '2026-04-10 10:05:00', @admin_id);

