-- TravelMate database health checks after importing src/main/resources/travelmate_db.sql

-- 1. Schema VNPAY and booking expiry fields
SHOW COLUMNS FROM bookings LIKE 'expire_at';
SHOW COLUMNS FROM payments LIKE 'gateway';
SHOW COLUMNS FROM payments LIKE 'vnp_txn_ref';
SHOW COLUMNS FROM payments LIKE 'raw_return_payload';
SHOW COLUMNS FROM payments LIKE 'raw_ipn_payload';
SHOW COLUMNS FROM payments LIKE 'confirmed_from_gateway_at';
SHOW COLUMNS FROM payments LIKE 'expire_at';

-- 2. Accounts by role and partner property type
SELECT role, partner_property_type, COUNT(*) AS total
FROM users
GROUP BY role, partner_property_type
ORDER BY role, partner_property_type;

-- 3. Accommodations and rooms by type/status
SELECT property_type, approval_status, COUNT(*) AS total
FROM accommodations
GROUP BY property_type, approval_status
ORDER BY property_type, approval_status;

SELECT a.property_type, r.approval_status, COUNT(*) AS total_rooms
FROM rooms r
JOIN accommodations a ON a.id = r.accommodation_id
GROUP BY a.property_type, r.approval_status
ORDER BY a.property_type, r.approval_status;

-- 4. Booking and payment status coverage
SELECT booking_source, booking_status, payment_option, payment_status, COUNT(*) AS total
FROM bookings
GROUP BY booking_source, booking_status, payment_option, payment_status
ORDER BY booking_source, booking_status, payment_option, payment_status;

SELECT payment_option, payment_status, COUNT(*) AS total
FROM payments
GROUP BY payment_option, payment_status
ORDER BY payment_option, payment_status;

-- 5. Voucher scope and cost bearer coverage
SELECT voucher_scope, cost_bearer, active, COUNT(*) AS total
FROM vouchers
GROUP BY voucher_scope, cost_bearer, active
ORDER BY voucher_scope, cost_bearer, active;

-- 6. Settlement integrity
SELECT id, partner_id, period_start, period_end, DATEDIFF(period_end, period_start) AS days
FROM partner_settlements
WHERE DATEDIFF(period_end, period_start) < 27;

SELECT partner_id, period_start, period_end, COUNT(*) AS total
FROM partner_settlements
GROUP BY partner_id, period_start, period_end
HAVING COUNT(*) > 1;

SELECT id, partner_id, gross_amount, commission_amount, voucher_deduction_amount, payout_amount,
       gross_amount - commission_amount - voucher_deduction_amount AS expected_payout
FROM partner_settlements
WHERE payout_amount <> gross_amount - commission_amount - voucher_deduction_amount;

-- 7. Wallet integrity
SELECT settlement_id, transaction_type, COUNT(*) AS total
FROM partner_wallet_transactions
WHERE transaction_type = 'SETTLEMENT_CREDIT'
  AND settlement_id IS NOT NULL
GROUP BY settlement_id, transaction_type
HAVING COUNT(*) > 1;

SELECT withdrawal_request_id, transaction_type, COUNT(*) AS total
FROM partner_wallet_transactions
WHERE withdrawal_request_id IS NOT NULL
GROUP BY withdrawal_request_id, transaction_type
HAVING COUNT(*) > 1;

SELECT *
FROM partner_wallets
WHERE available_balance < 0
   OR pending_withdrawal_amount < 0
   OR total_earned_amount < 0
   OR total_withdrawn_amount < 0;

-- 8. User-facing notes should not expose implementation wording
SELECT 'bookings' AS table_name, id, booking_code, note
FROM bookings
WHERE LOWER(note) LIKE '%demo%'
   OR LOWER(note) LIKE '%seed%'
   OR note LIKE '%giảng viên%'

UNION ALL

SELECT 'partner_settlements' AS table_name, id, CAST(id AS CHAR), note
FROM partner_settlements
WHERE LOWER(note) LIKE '%demo%'
   OR LOWER(note) LIKE '%seed%'
   OR note LIKE '%giảng viên%';
