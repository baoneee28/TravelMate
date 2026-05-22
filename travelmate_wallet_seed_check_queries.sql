-- QUICK CHECK QUERIES SAU KHI IMPORT travelmate_wallet_settlement_test_seed.sql

-- 1. Tài khoản seed
SELECT id, email, role, partner_property_type, bank_name, bank_account_number
FROM users
WHERE email IN ('seeduser_wallet@travelmate.vn','seedpartner_wallet@travelmate.vn','seedpartner_nobank@travelmate.vn');

-- 2. Booking đủ điều kiện và không đủ điều kiện cho settlement tháng trước
SELECT b.booking_code, b.booking_source, b.booking_status, b.payment_option, b.payment_status,
       p.amount, p.payment_status AS payment_entity_status, p.approved_at
FROM bookings b
LEFT JOIN payments p ON p.booking_id = b.id
WHERE b.booking_code LIKE 'BK-SEED-WALLET-%'
ORDER BY b.booking_code;

-- 3. Settlement mới sau khi Admin bấm "Tạo quyết toán tháng trước"
-- Expected cho seedpartner_wallet: gross=2250000, commission=337500, voucher=200000, payout=1712500
SELECT ps.id, u.email, ps.period_start, ps.period_end, ps.scheduled_payout_date,
       ps.gross_amount, ps.commission_amount, ps.voucher_deduction_amount, ps.payout_amount,
       ps.settlement_status, ps.settlement_date, ps.note
FROM partner_settlements ps
JOIN users u ON u.id = ps.partner_id
WHERE u.email = 'seedpartner_wallet@travelmate.vn'
ORDER BY ps.period_start DESC;

-- 4. Ví của partner test
SELECT u.email, w.available_balance, w.pending_withdrawal_amount,
       w.total_earned_amount, w.total_withdrawn_amount, w.updated_at
FROM partner_wallets w
JOIN users u ON u.id = w.partner_id
WHERE u.email IN ('seedpartner_wallet@travelmate.vn','seedpartner_nobank@travelmate.vn');

-- 5. Withdrawal demo đủ 3 trạng thái
SELECT u.email, wr.request_code, wr.amount, wr.withdrawal_status,
       wr.requested_at, wr.processed_at, wr.admin_note
FROM partner_withdrawal_requests wr
JOIN users u ON u.id = wr.partner_id
WHERE wr.request_code LIKE 'WD-SEED-%'
ORDER BY wr.requested_at DESC;

-- 6. Lịch sử giao dịch ví demo
SELECT u.email, tx.transaction_code, tx.transaction_type, tx.direction, tx.amount,
       tx.balance_before, tx.balance_after, tx.description, tx.created_at
FROM partner_wallet_transactions tx
JOIN users u ON u.id = tx.partner_id
WHERE tx.transaction_code LIKE 'WTX-SEED-%'
ORDER BY tx.created_at DESC;

-- 7. Kiểm tra không có double credit trên cùng settlement
SELECT settlement_id, transaction_type, COUNT(*) AS total
FROM partner_wallet_transactions
WHERE transaction_type = 'SETTLEMENT_CREDIT' AND settlement_id IS NOT NULL
GROUP BY settlement_id, transaction_type
HAVING COUNT(*) > 1;
