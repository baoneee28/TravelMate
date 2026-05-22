-- ====================================================================
-- IDEMPOTENT SEED DATA FOR TRAVELMATE WALLET & SETTLEMENT TESTING
-- ====================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Clean up existing seed data
DELETE FROM partner_wallet_transactions WHERE transaction_code LIKE 'WTX-SEED-%' OR partner_id IN (SELECT id FROM users WHERE email IN ('seedpartner_wallet@travelmate.vn', 'seedpartner_nobank@travelmate.vn'));
DELETE FROM partner_withdrawal_requests WHERE request_code LIKE 'WD-SEED-%' OR partner_id IN (SELECT id FROM users WHERE email IN ('seedpartner_wallet@travelmate.vn', 'seedpartner_nobank@travelmate.vn'));
DELETE FROM partner_wallets WHERE partner_id IN (SELECT id FROM users WHERE email IN ('seedpartner_wallet@travelmate.vn', 'seedpartner_nobank@travelmate.vn'));
DELETE FROM partner_settlements WHERE partner_id IN (SELECT id FROM users WHERE email IN ('seedpartner_wallet@travelmate.vn', 'seedpartner_nobank@travelmate.vn'));
DELETE FROM payments WHERE transaction_code IN ('TXN-SEED-WALLET-DIRECT-001', 'TXN-SEED-WALLET-PENDING-001', 'TXN-SEED-WALLET-FULL-001', 'TXN-SEED-WALLET-NOSHOW-001');
DELETE FROM bookings WHERE booking_code LIKE 'BK-SEED-WALLET-%';
DELETE FROM rooms WHERE room_code = 'SEED-WALLET-STD';
DELETE FROM accommodations WHERE name = '[SEED WALLET] Đà Lạt Settlement Hotel';
DELETE FROM users WHERE email IN ('seeduser_wallet@travelmate.vn', 'seedpartner_wallet@travelmate.vn', 'seedpartner_nobank@travelmate.vn');

SET FOREIGN_KEY_CHECKS = 1;

-- 2. Define variables and insert seed data
SET @admin_id = (SELECT id FROM users WHERE email = 'admin@travelmate.vn' LIMIT 1);

INSERT INTO users (email, password, full_name, name, phone, role, status, partner_property_type,
                bank_account_number, bank_name, bank_account_holder, bank_branch,
                created_at, updated_at) VALUES
('seeduser_wallet@travelmate.vn',
 '$2a$10$FsFOdcKQPqCnlIPA3j82ZeY7R6tMpdhavFO2bfqWUfJqF1Z4nloO2',
 'Seed User Wallet', 'Seed User Wallet', '0908 111 222', 'USER', 'ACTIVE', NULL,
 NULL, NULL, NULL, NULL, NOW(), NOW()),
('seedpartner_wallet@travelmate.vn',
 '$2a$10$dCikCIiksr/Ne1Xpv40vKOMFwpiY751Cb1kdIVDwobiMzImbLC3oq',
 'SEED Partner Wallet Hotel', 'SEED Partner Wallet Hotel', '0908 333 444', 'PARTNER', 'ACTIVE', 'HOTEL',
 '123456789012', 'Vietcombank', 'SEED PARTNER WALLET HOTEL', 'CN Đà Lạt', NOW(), NOW()),
('seedpartner_nobank@travelmate.vn',
 '$2a$10$dCikCIiksr/Ne1Xpv40vKOMFwpiY751Cb1kdIVDwobiMzImbLC3oq',
 'SEED Partner No Bank', 'SEED Partner No Bank', '0908 555 666', 'PARTNER', 'ACTIVE', 'HOTEL',
 NULL, NULL, NULL, NULL, NOW(), NOW());

SET @seed_user_id = (SELECT id FROM users WHERE email = 'seeduser_wallet@travelmate.vn' LIMIT 1);
SET @seed_partner_id = (SELECT id FROM users WHERE email = 'seedpartner_wallet@travelmate.vn' LIMIT 1);
SET @seed_nobank_partner_id = (SELECT id FROM users WHERE email = 'seedpartner_nobank@travelmate.vn' LIMIT 1);

INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
VALUES (
    '[SEED WALLET] Đà Lạt Settlement Hotel',
    'Khách sạn seed dành riêng để demo quyết toán tháng, ví Partner, rút tiền và export Excel. Không dùng cho production.',
    '88 Seed Demo, Phường 1',
    'Đà Lạt',
    'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&q=80',
    4, 9.1, 12, 'HOTEL', 'APPROVED', @seed_partner_id, NOW(), NOW()
);
SET @seed_acc_id = (SELECT id FROM accommodations WHERE name = '[SEED WALLET] Đà Lạt Settlement Hotel' LIMIT 1);

INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night,
                   available_quantity, description, image_url, room_category, commission_rate_override)
VALUES (
    @seed_acc_id, 'SEED-WALLET-STD', 'Seed Standard Settlement Room', '1 giường Queen',
    2, 850000, 8,
    'Phòng seed dùng để kiểm tra công thức settlement: gross, commission 15%, voucher partner chịu và payout.',
    'https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=400&q=70',
    'STANDARD', NULL
);
SET @seed_room_id = (SELECT id FROM rooms WHERE room_code = 'SEED-WALLET-STD' LIMIT 1);

-- 2 booking đủ điều kiện settlement tháng 04/2026
INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
    check_in, check_out, adults, children, room_quantity,
    customer_name, customer_phone, customer_email,
    total_amount, paid_amount, remaining_amount,
    booking_status, payment_option, payment_status, partner_status,
    note, voucher_code, discount_amount, voucher_cost_bearer, total_before_discount,
    booking_source, created_at, updated_at)
VALUES
('BK-SEED-WALLET-FULL-001', @seed_user_id, @seed_acc_id, @seed_room_id,
 '2026-04-11', '2026-04-13', 2, 0, 1,
 'Seed User Wallet', '0908 111 222', 'seeduser_wallet@travelmate.vn',
 1500000, 1500000, 0,
 'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
 'SEED: Booking FULL_PAYMENT đã hoàn tất, đủ điều kiện settlement tháng 04/2026.',
 'SEED200', 200000, 'PARTNER', 1700000,
 'ONLINE', '2026-04-10 09:00:00', '2026-04-13 12:00:00'),
('BK-SEED-WALLET-NOSHOW-001', @seed_user_id, @seed_acc_id, @seed_room_id,
 '2026-04-18', '2026-04-20', 2, 0, 1,
 'Seed User Wallet', '0908 111 222', 'seeduser_wallet@travelmate.vn',
 2500000, 750000, 1750000,
 'NO_SHOW', 'DEPOSIT_30', 'DEPOSIT_FORFEITED', NULL,
 'SEED: Booking cọc 30% no-show, chỉ phần cọc online được đưa vào settlement.',
 'SEED200', 0, NULL, 2500000,
 'ONLINE', '2026-04-17 09:00:00', '2026-04-20 12:00:00');

INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code,
                      payment_status, paid_at, approved_at, note)
SELECT id, 'VNPAY_DEMO', 'FULL_PAYMENT', 1500000, 'TXN-SEED-WALLET-FULL-001',
       'APPROVED', '2026-04-10 09:05:00', '2026-04-10 09:10:00',
       'SEED: Payment FULL_PAYMENT đủ điều kiện settlement.'
FROM bookings WHERE booking_code = 'BK-SEED-WALLET-FULL-001';

INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code,
                      payment_status, paid_at, approved_at, note)
SELECT id, 'VNPAY_DEMO', 'DEPOSIT_30', 750000, 'TXN-SEED-WALLET-NOSHOW-001',
       'DEPOSIT_FORFEITED', '2026-04-18 09:05:00', '2026-04-20 12:10:00',
       'SEED: Payment cọc 30% bị giữ do no-show, đủ điều kiện settlement.'
FROM bookings WHERE booking_code = 'BK-SEED-WALLET-NOSHOW-001';

-- 2 booking không đủ điều kiện settlement để giảng viên nhìn thấy rule lọc
INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
    check_in, check_out, adults, children, room_quantity,
    customer_name, customer_phone, customer_email,
    total_amount, paid_amount, remaining_amount,
    booking_status, payment_option, payment_status, partner_status,
    note, voucher_code, discount_amount, voucher_cost_bearer, total_before_discount,
    booking_source, created_at, updated_at)
VALUES
('BK-SEED-WALLET-DIRECT-001', @seed_user_id, @seed_acc_id, @seed_room_id,
 '2026-04-22', '2026-04-24', 2, 0, 1,
 'Seed User Wallet', '0908 111 222', 'seeduser_wallet@travelmate.vn',
 900000, 900000, 0,
 'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
 'SEED: Booking DIRECT tại cơ sở, không phải doanh thu online TravelMate nên không settlement.',
 NULL, 0, NULL, 900000,
 'DIRECT', '2026-04-22 09:00:00', '2026-04-24 11:00:00'),
('BK-SEED-WALLET-PENDING-001', @seed_user_id, @seed_acc_id, @seed_room_id,
 '2026-04-27', '2026-04-29', 2, 0, 1,
 'Seed User Wallet', '0908 111 222', 'seeduser_wallet@travelmate.vn',
 1000000, 1000000, 0,
 'CONFIRMED', 'FULL_PAYMENT', 'APPROVED', 'PENDING_PARTNER_CONFIRMATION',
 'SEED: Payment đã approved nhưng booking chưa COMPLETED/NO_SHOW nên không settlement.',
 NULL, 0, NULL, 1000000,
 'ONLINE', '2026-04-26 09:00:00', '2026-04-26 09:30:00');

INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code,
                      payment_status, paid_at, approved_at, note)
SELECT id, 'VNPAY_DEMO', 'FULL_PAYMENT', 900000, 'TXN-SEED-WALLET-DIRECT-001',
       'APPROVED', '2026-04-22 09:05:00', '2026-04-22 09:10:00',
       'SEED: DIRECT booking bị loại khỏi settlement dù payment approved.'
FROM bookings WHERE booking_code = 'BK-SEED-WALLET-DIRECT-001';

INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code,
                      payment_status, paid_at, approved_at, note)
SELECT id, 'VNPAY_DEMO', 'FULL_PAYMENT', 1000000, 'TXN-SEED-WALLET-PENDING-001',
       'APPROVED', '2026-04-26 09:05:00', '2026-04-26 09:10:00',
       'SEED: Booking chưa hoàn tất nên bị loại khỏi settlement.'
FROM bookings WHERE booking_code = 'BK-SEED-WALLET-PENDING-001';

-- Settlement PAID cũ để Partner có ví và lịch sử giao dịch ngay khi đăng nhập
INSERT INTO partner_settlements
    (partner_id, period_start, period_end,
     gross_amount, commission_amount, voucher_deduction_amount, payout_amount,
     scheduled_payout_date, settlement_status, settlement_date, note, created_at)
VALUES
(@seed_partner_id, '2026-03-01', '2026-03-31',
 4800000, 720000, 80000, 4000000,
 '2026-04-10', 'PAID', '2026-04-10 10:00:00',
 'SEED: Settlement cũ đã paid để demo ví có tiền vào, rút tiền và export Excel.',
 '2026-03-31 18:30:00'),
(@seed_nobank_partner_id, '2026-03-01', '2026-03-31',
 1200000, 180000, 20000, 1000000,
 '2026-04-10', 'PAID', '2026-04-10 10:05:00',
 'SEED: Partner có số dư nhưng chưa có bank info, dùng test chặn rút tiền.',
 '2026-03-31 18:35:00');

SET @seed_paid_settlement_id = (
    SELECT id FROM partner_settlements
    WHERE partner_id = @seed_partner_id
      AND period_start = '2026-03-01'
      AND period_end = '2026-03-31'
    LIMIT 1
);
SET @seed_nobank_settlement_id = (
    SELECT id FROM partner_settlements
    WHERE partner_id = @seed_nobank_partner_id
      AND period_start = '2026-03-01'
      AND period_end = '2026-03-31'
    LIMIT 1
);

INSERT INTO partner_wallets
    (partner_id, available_balance, pending_withdrawal_amount,
     total_earned_amount, total_withdrawn_amount, updated_at)
VALUES
(@seed_partner_id, 2200000, 800000, 4000000, 1000000, NOW()),
(@seed_nobank_partner_id, 1000000, 0, 1000000, 0, NOW());

INSERT INTO partner_withdrawal_requests
    (partner_id, request_code, amount,
     bank_name, bank_account_number, bank_account_holder, bank_branch,
     withdrawal_status, requested_at, processed_at, processed_by_admin_id, admin_note)
VALUES
(@seed_partner_id, 'WD-SEED-PENDING-001', 800000,
 'Vietcombank', '123456789012', 'SEED PARTNER WALLET HOTEL', 'CN Đà Lạt',
 'PENDING', '2026-05-18 09:00:00', NULL, NULL, NULL),
(@seed_partner_id, 'WD-SEED-PAID-001', 1000000,
 'Vietcombank', '123456789012', 'SEED PARTNER WALLET HOTEL', 'CN Đà Lạt',
 'PAID', '2026-05-12 09:00:00', '2026-05-12 15:00:00', @admin_id,
 'SEED: Admin đã chuyển khoản ngoài hệ thống, mã GD SEED-VCB-1000.'),
(@seed_partner_id, 'WD-SEED-REJECTED-001', 500000,
 'Vietcombank', '123456789012', 'SEED PARTNER WALLET HOTEL', 'CN Đà Lạt',
 'REJECTED', '2026-05-14 09:00:00', '2026-05-14 15:00:00', @admin_id,
 'SEED: Từ chối để demo hoàn tiền về ví Partner.');

SET @wd_seed_pending_id = (SELECT id FROM partner_withdrawal_requests WHERE request_code = 'WD-SEED-PENDING-001' LIMIT 1);
SET @wd_seed_paid_id = (SELECT id FROM partner_withdrawal_requests WHERE request_code = 'WD-SEED-PAID-001' LIMIT 1);
SET @wd_seed_rejected_id = (SELECT id FROM partner_withdrawal_requests WHERE request_code = 'WD-SEED-REJECTED-001' LIMIT 1);

INSERT INTO partner_wallet_transactions
    (partner_id, settlement_id, withdrawal_request_id, transaction_code,
     transaction_type, direction, amount, balance_before, balance_after,
     description, created_at, created_by_admin_id)
VALUES
(@seed_partner_id, @seed_paid_settlement_id, NULL, 'WTX-SEED-STL-CREDIT-001',
 'SETTLEMENT_CREDIT', 'IN', 4000000, 0, 4000000,
 'SEED: Cộng tiền settlement tháng 03/2026 vào ví Partner', '2026-04-10 10:00:00', @admin_id),
(@seed_partner_id, NULL, @wd_seed_pending_id, 'WTX-SEED-WDREQ-PENDING-001',
 'WITHDRAWAL_REQUEST', 'OUT', 800000, 4000000, 3200000,
 'SEED: Partner gửi yêu cầu rút 800.000đ đang chờ Admin xử lý', '2026-05-18 09:00:00', NULL),
(@seed_partner_id, NULL, @wd_seed_paid_id, 'WTX-SEED-WDREQ-PAID-001',
 'WITHDRAWAL_REQUEST', 'OUT', 1000000, 3200000, 2200000,
 'SEED: Partner gửi yêu cầu rút 1.000.000đ', '2026-05-12 09:00:00', NULL),
(@seed_partner_id, NULL, @wd_seed_paid_id, 'WTX-SEED-WDPAID-001',
 'WITHDRAWAL_PAID', 'INFO', 1000000, 2200000, 2200000,
 'SEED: Admin xác nhận đã chuyển khoản, không trừ availableBalance lần 2', '2026-05-12 15:00:00', @admin_id),
(@seed_partner_id, NULL, @wd_seed_rejected_id, 'WTX-SEED-WDREQ-REJECTED-001',
 'WITHDRAWAL_REQUEST', 'OUT', 500000, 2200000, 1700000,
 'SEED: Partner gửi yêu cầu rút 500.000đ để demo từ chối', '2026-05-14 09:00:00', NULL),
(@seed_partner_id, NULL, @wd_seed_rejected_id, 'WTX-SEED-WDREJECTED-001',
 'WITHDRAWAL_REJECTED', 'IN', 500000, 1700000, 2200000,
 'SEED: Admin từ chối, hệ thống hoàn tiền về ví Partner', '2026-05-14 15:00:00', @admin_id),
(@seed_nobank_partner_id, @seed_nobank_settlement_id, NULL, 'WTX-SEED-NOBANK-STL-CREDIT-001',
 'SETTLEMENT_CREDIT', 'IN', 1000000, 0, 1000000,
 'SEED: Partner có tiền nhưng chưa có tài khoản ngân hàng', '2026-04-10 10:05:00', @admin_id);
