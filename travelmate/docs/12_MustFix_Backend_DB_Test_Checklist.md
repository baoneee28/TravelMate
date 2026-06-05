# TravelMate - Must Fix Backend/DB Test Checklist

Tai lieu nay gom cac buoc kiem tra sau khi da sua 5 diem quan trong:
VNPAY auto confirm, voucher Admin-only, huy booking da thanh toan, dong bo SQL
va lam sach du lieu hien thi.

## 1. Chay test tu dong

Chay tai thu muc `travelmate`:

```powershell
.\mvnw.cmd test
```

Nhom test tu dong dang bao ve cac luong sau:

| Nhom | File test | Noi dung chinh |
| --- | --- | --- |
| VNPAY | `PaymentServiceTest` | FULL_PAYMENT/DEPOSIT_30 thanh cong tu dong `APPROVED + CONFIRMED`, F5 khong double update, fail/cancel/expired mo lai quota |
| Cancel booking | `BookingCalculationTest` | Chua thanh toan thi `CANCELLED`; thanh toan du thi `REFUND_PENDING`; don coc da thu thi `DEPOSIT_FORFEITED`, huy lan 2 bi chan, booking da huy khong cho Partner thao tac |
| Voucher | `VoucherCalculationTest` | Admin tao voucher global/partner-room, Partner chi gan voucher kho, voucher sai phong/het han/chua toi ngay/chua du min order bi tu choi |
| SQL seed/schema | `UserPortalSqlSeedTest` | `travelmate_db.sql` co cot VNPAY/expire, co unique index, settlement monthly, payout dung cong thuc, khong lo wording nhay cam |
| Wallet/settlement | `PartnerWalletServiceTest`, `SettlementServiceTest` | Khong double credit, withdrawal paid/rejected dung so du, settlement theo thang |

## 2. Test VNPAY sandbox tren UI

Can cau hinh truoc khi test:

```powershell
$env:VNPAY_TMN_CODE="ma_sandbox_cua_ban"
$env:VNPAY_HASH_SECRET="secret_sandbox_cua_ban"
```

Checklist:

| Case | Buoc chinh | Ket qua can thay |
| --- | --- | --- |
| FULL_PAYMENT success | User dat phong, chon thanh toan 100%, thanh toan VNPAY thanh cong | User thay da thanh toan 100%, don `CONFIRMED`, partner status `PARTNER_CONFIRMED`; Partner co the check-in khi khach den |
| DEPOSIT_30 success | User dat phong khac, chon coc 30%, thanh toan VNPAY thanh cong | User/Partner thay da coc 30%, 70% con lai thanh toan tai co so |
| Fail/cancel | User sang VNPAY roi huy/that bai | Booking `CANCELLED`, payment `CANCELLED/FAILED/EXPIRED`, phong duoc mo lai |
| Refresh result | Sau thanh toan thanh cong bam F5 3-5 lan | Khong sinh payment/notification/revenue trung |

SQL doi chieu nhanh:

```sql
SELECT booking_code, booking_status, partner_status, payment_option,
       payment_status, total_amount, paid_amount, remaining_amount
FROM bookings
WHERE booking_code = 'MA_BOOKING_VUA_TEST';

SELECT COUNT(*) AS total_payment
FROM payments
WHERE booking_id = (
    SELECT id FROM bookings WHERE booking_code = 'MA_BOOKING_VUA_TEST'
);
```

## 3. Test voucher Admin-only tren UI

| Case | Buoc chinh | Ket qua can thay |
| --- | --- | --- |
| Partner vouchers | Partner vao `/partner/vouchers` | Khong co form tao voucher; chi co kho voucher Admin phat hanh va lien ket phong/can |
| Route tao voucher cu cua Partner | Thu mo lai cac duong dan tao voucher cu neu con bookmark | Bi chan/redirect, khong tao du lieu moi |
| Admin global | Admin tao `USER_GLOBAL`, cost bearer `ADMIN`; User ap dung | User duoc giam, settlement Partner khong bi tru voucher |
| Admin partner-room | Admin tao `PARTNER_ROOM`, cost bearer `PARTNER`; Partner gan vao phong; User ap dung dung phong | Settlement co `voucher_deduction_amount > 0` va payout dung cong thuc |
| Sai phong/co so | User dung voucher cua phong A cho phong B | Bi bao voucher khong ap dung cho phong/can nay |

SQL doi chieu:

```sql
SELECT booking_code, voucher_discount_amount, voucher_cost_bearer
FROM bookings
WHERE booking_code = 'MA_BOOKING_TEST_VOUCHER';

SELECT gross_amount, commission_amount, voucher_deduction_amount, payout_amount,
       gross_amount - commission_amount - voucher_deduction_amount AS expected_payout
FROM partner_settlements
WHERE partner_id = ID_PARTNER_TEST
ORDER BY id DESC
LIMIT 1;
```

## 4. Test huy booking

| Case | Buoc chinh | Ket qua can thay |
| --- | --- | --- |
| Chua thanh toan | User tao booking roi huy truoc khi thanh toan | Booking `CANCELLED`, payment `CANCELLED`, khong hien cho hoan tien |
| Da thanh toan 100% | User thanh toan xong roi huy truoc check-in | Booking `CANCELLED`, payment `REFUND_PENDING`, Admin thay can xu ly hoan tien |
| Da coc 30% | User coc xong roi huy | Booking `CANCELLED`, payment `DEPOSIT_FORFEITED`, refund `0`, tien coc duoc quyet toan theo hoa hong tren khoan online |
| Huy lan 2 | Refresh va thu huy lai | Nut huy khong con; backend khong doi trang thai/quota lan nua |
| Partner sau khi huy | Partner vao booking da huy | Khong co nut check-in, no-show hoac thao tac van hanh |

## 5. Test import SQL moi

```sql
DROP DATABASE IF EXISTS travelmate_test;
CREATE DATABASE travelmate_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE travelmate_test;
SOURCE path/to/travelmate_db.sql;
```

Sau khi import, chay file:

```text
docs/sql/travelmate_db_health_checks.sql
```

Can dat:

- Tat ca cot VNPAY/expire deu co.
- `vnp_txn_ref` co unique.
- Settlement khong co ky duoi 28 ngay.
- Payout = gross - commission - voucher_deduction.
- Khong double credit vi.
- Khong con chu `demo`, `seed`, `ChatGPT`, `AI seed`, `giang vien`, `thay` trong du lieu hien thi.

## 6. Luong demo tong hop

1. User dat FULL_PAYMENT, VNPAY success.
2. TravelMate tu giu phong sau khi ghi nhan thanh toan.
3. Partner check-in khi khach den.
4. Partner check-out.
5. Admin generate settlement thang.
6. Admin mark settlement `PAID`.
7. Partner vao vi va thay tien duoc cong sau khi settlement da `PAID`.

Ket qua cuoi: booking `COMPLETED`, payment `APPROVED`, khong tinh direct/manual block vao settlement,
voucher Admin chiu khong tru Partner, voucher Partner chiu co tru dung payout.
