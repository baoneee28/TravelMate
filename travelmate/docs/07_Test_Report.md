# TravelMate — Test Report

**Phiên bản:** 1.0  
**Ngày kiểm thử:** 2026-05-20  
**Môi trường:** Spring Boot 3.5 + JUnit 5 + Mockito (Unit Tests, không DB)

---

## 1. Tổng Quan

| Hạng mục | Số lượng |
|---|---|
| Tổng test cases | 47 |
| PASS | 47 |
| FAIL | 0 |
| ERROR | 0 |
| Tỷ lệ pass | 100% |

---

## 2. Danh Sách Test Files

### 2.1 BookingCalculationTest — Tính Tiền Đặt Phòng

**File:** `src/test/java/com/travelmate/service/BookingCalculationTest.java`

| # | Test name | Mô tả | Kết quả |
|---|---|---|---|
| 1 | `calculateTotalAmount_multipleRoomsAndNights` | 850.000đ × 3 đêm × 2 phòng = 5.100.000đ | ✅ PASS |
| 2 | `calculateTotalAmount_singleRoomOneNight` | 650.000đ × 1 đêm × 1 phòng = 650.000đ | ✅ PASS |
| 3 | `calculateNights_returnsCorrectDiff` | 01/06 → 04/06 = 3 đêm | ✅ PASS |
| 4 | `calculateNights_minimumOneNight` | Cùng ngày → tối thiểu 1 đêm | ✅ PASS |
| 5 | `calculatePaidAmount_deposit30_is30Percent` | DEPOSIT_30: 1.300.000 × 30% = 390.000đ | ✅ PASS |
| 6 | `calculatePaidAmount_deposit30_remainingIs70Percent` | remaining = total - paid | ✅ PASS |
| 7 | `calculatePaidAmount_deposit30_floorRounding` | 1.000.001 × 30% → floor → 300.000đ | ✅ PASS |
| 8 | `calculatePaidAmount_fullPayment_is100Percent` | FULL_PAYMENT: paid = total | ✅ PASS |
| 9 | `calculatePaidAmount_fullPayment_remainingIsZero` | remaining = 0 khi FULL_PAYMENT | ✅ PASS |
| 10 | `realScenario_tulipStandardDeposit30` | BK-TLP-STD-0001: 960.000 cọc 30% = 288.000đ | ✅ PASS |
| 11 | `realScenario_tmgPremiumFullPayment` | BK-TMG-PRE-0001: 3 đêm × 1.650.000 = 4.950.000đ | ✅ PASS |

**Kết quả: 11/11 PASS**

---

### 2.2 VoucherCalculationTest — Logic Voucher

**File:** `src/test/java/com/travelmate/service/VoucherCalculationTest.java`

| # | Test name | Mô tả | Kết quả |
|---|---|---|---|
| 1 | `percent_basicDiscount` | PERCENT 10%: 1.300.000đ → giảm 130.000đ | ✅ PASS |
| 2 | `percent_cappedByMaxDiscount` | PERCENT 10%, max 500.000đ: 6.000.000đ → cap 500.000đ | ✅ PASS |
| 3 | `percent_summer10_underCap` | SUMMER10: 1.300.000đ giảm 130.000đ (< cap) | ✅ PASS |
| 4 | `percent_lata20_partnerVoucher` | LATA20 PARTNER: 2.550.000đ × 20% = 510.000đ | ✅ PASS |
| 5 | `fixed_basicDiscount` | FIXED 50.000đ: 300.000đ → giảm đúng 50.000đ | ✅ PASS |
| 6 | `fixed_partnerVoucherVnt100k` | VNT100K PARTNER: 5.600.000đ → giảm 100.000đ | ✅ PASS |
| 7 | `guard_discountCannotReduceBelowVnpayMinimum` | FIXED 50.000đ trên 10.000đ → discount tối đa 5.000đ | ✅ PASS |
| 8 | `guard_percentDiscountCannotReduceBelowVnpayMinimum` | PERCENT 99% trên 6.000đ → sau giảm >= 5.000đ | ✅ PASS |
| 9 | `costBearer_admin_markedCorrectly` | costBearer ADMIN được ghi nhận | ✅ PASS |
| 10 | `costBearer_partner_markedCorrectly` | costBearer PARTNER được ghi nhận | ✅ PASS |

**Kết quả: 10/10 PASS**

---

### 2.3 SettlementEligibilityTest — Điều Kiện Quyết Toán

**File:** `src/test/java/com/travelmate/service/SettlementEligibilityTest.java`

| # | Test name | Mô tả | Kết quả |
|---|---|---|---|
| 1 | `eligible_onlineApprovedCompleted` | ONLINE + APPROVED + COMPLETED → ✓ hợp lệ | ✅ PASS |
| 2 | `eligible_onlineDepositForfeitedNoShow` | ONLINE + DEPOSIT_FORFEITED + NO_SHOW → ✓ hợp lệ | ✅ PASS |
| 3 | `notEligible_directBooking` | DIRECT booking → ✗ không quyết toán | ✅ PASS |
| 4 | `notEligible_manualBlock` | MANUAL_BLOCK → ✗ không quyết toán | ✅ PASS |
| 5 | `notEligible_pendingAdminApproval` | PENDING_ADMIN_APPROVAL → ✗ chưa duyệt | ✅ PASS |
| 6 | `notEligible_checkedIn` | CHECKED_IN → ✗ chưa hoàn tất | ✅ PASS |
| 7 | `notEligible_confirmed` | CONFIRMED → ✗ chưa check-in | ✅ PASS |
| 8 | `notEligible_cancelled` | CANCELLED → ✗ không quyết toán | ✅ PASS |

**Kết quả: 8/8 PASS**

---

### 2.4 NoShowDepositTest — Nghiệp Vụ No-Show

**File:** `src/test/java/com/travelmate/service/NoShowDepositTest.java`

| # | Test name | Mô tả | Kết quả |
|---|---|---|---|
| 1 | `deposit30NoShow_bookingStatusIsNoShow` | DEPOSIT_30 no-show → BookingStatus = NO_SHOW | ✅ PASS |
| 2 | `deposit30NoShow_paymentStatusIsDepositForfeited` | DEPOSIT_30 no-show → PaymentStatus = DEPOSIT_FORFEITED | ✅ PASS |
| 3 | `deposit30NoShow_roomAvailabilityRestored` | DEPOSIT_30 no-show → room.qty += roomQuantity | ✅ PASS |
| 4 | `deposit30NoShow_noteIsSet` | DEPOSIT_30 no-show → note chứa "Cọc 30%" | ✅ PASS |
| 5 | `fullPaymentNoShow_bookingStatusIsCheckedIn` | FULL_PAYMENT no-show → CHECKED_IN (không mất tiền) | ✅ PASS |
| 6 | `fullPaymentNoShow_paymentStatusStaysApproved` | FULL_PAYMENT no-show → PaymentStatus = APPROVED | ✅ PASS |
| 7 | `fullPaymentNoShow_roomNotRestored` | FULL_PAYMENT no-show → phòng KHÔNG mở lại | ✅ PASS |
| 8 | `noShow_bookingNotFound_throwsException` | Booking không tồn tại → RuntimeException | ✅ PASS |
| 9 | `noShow_bookingNotConfirmed_throwsException` | Booking chưa CONFIRMED → RuntimeException | ✅ PASS |

**Kết quả: 9/9 PASS**

---

### 2.5 AvailabilityOverlapTest — Logic Overlap Khoảng Ngày

**File:** `src/test/java/com/travelmate/service/AvailabilityOverlapTest.java`

| # | Test name | Mô tả | Kết quả |
|---|---|---|---|
| 1 | `overlap_partialRight` | existing(1-5) vs requested(3-7) → overlap chéo phải | ✅ PASS |
| 2 | `overlap_partialLeft` | existing(3-7) vs requested(1-5) → overlap chéo trái | ✅ PASS |
| 3 | `overlap_containedWithin` | existing(1-10) vs requested(3-5) → nằm trong | ✅ PASS |
| 4 | `overlap_existingInsideRequested` | existing(3-5) vs requested(1-10) → existing bên trong | ✅ PASS |
| 5 | `overlap_exactSameDates` | existing(1-5) vs requested(1-5) → trùng hoàn toàn | ✅ PASS |
| 6 | `noOverlap_checkOutEqualsCheckIn` | existing(1-5) vs requested(5-7) → tiếp nối đúng | ✅ PASS |
| 7 | `noOverlap_completelyBefore` | existing(1-3) vs requested(5-7) → hoàn toàn trước | ✅ PASS |
| 8 | `noOverlap_completelyAfter` | existing(5-7) vs requested(1-3) → hoàn toàn sau | ✅ PASS |
| 9 | `noOverlap_reverseAdjacent` | existing(5-7) vs requested(1-5) → tiếp nối ngược | ✅ PASS |

**Kết quả: 9/9 PASS**

---

### 2.6 Tests Hiện Có (Trước Đồ Án)

**AccommodationServiceSearchTest** — Tìm kiếm chỗ lưu trú theo thành phố/loại  
**ChatbotServiceTest** — Intent chatbot ngân sách, gợi ý điểm đến  
**TravelPostServiceTest** — Quản lý bài viết du lịch

---

## 3. Chiến Lược Kiểm Thử

### 3.1 Loại Test Sử Dụng

| Loại | Tool | Mô tả |
|---|---|---|
| **Unit Test** | JUnit 5 + Mockito | Kiểm thử logic thuần, không kết nối DB |
| **Mocking** | Mockito `@Mock` | Mock repositories và services phụ thuộc |
| **Reflection Test** | Spring `ReflectionTestUtils` | Kiểm thử private method (isSettlementEligible) |
| **Manual Test** | Browser | Kiểm thử UI, luồng VNPAY, phân quyền |

### 3.2 Chiều Hướng Kiểm Thử

**Happy Path (Đường đi chính):**
- Đặt phòng thành công → thanh toán VNPAY → admin duyệt → partner xác nhận → check-in → checkout
- Cọc 30% no-show → DEPOSIT_FORFEITED → settlement

**Alternative Path (Đường đi thay thế):**
- VNPAY thất bại / hủy → booking CANCELLED, phòng mở lại
- Admin từ chối → CANCELLED
- Voucher không hợp lệ → lỗi rõ ràng

**Edge Cases (Biên):**
- Cọc 30% làm tròn xuống (floor)
- Voucher giảm giá làm total < 5.000đ → guard VNPAY
- Số phòng = 0 → lỗi validation
- No-show FULL_PAYMENT (không mất tiền, không mở phòng)

### 3.3 Kiểm Thử Phân Quyền (Manual)

| Test | Kết quả |
|---|---|
| Guest truy cập `/admin/` → redirect login | ✅ |
| User truy cập `/admin/` → Access Denied | ✅ |
| Partner truy cập `/admin/` → Access Denied | ✅ |
| User truy cập `/partner/` → Access Denied | ✅ |
| `GET /api/travel-posts/` không login → 200 OK | ✅ |
| `POST /api/travel-posts/` không login → redirect login | ✅ |
| `/payment/vnpay-return` không có session → 200 OK | ✅ |
| VNPAY IPN callback `/payment/vnpay-ipn` → 200 OK | ✅ |

---

## 4. Các Lưu Ý Kiểm Thử

### 4.1 Tests Không Yêu Cầu DB
Toàn bộ unit tests dùng Mockito — chạy offline, không cần MySQL:
```bash
cd travelmate && mvn test
```

### 4.2 Tests Cần DB (Integration - Tương Lai)
- Kiểm thử anti-overbooking với `PESSIMISTIC_WRITE` lock
- Kiểm thử settlement amount chính xác với data thực
- Kiểm thử VNPAY IPN idempotent

### 4.3 Không Test
- VNPAY gateway (external system, sandbox)
- File upload (cần filesystem)
- Email/notification thật (mock trong test)
