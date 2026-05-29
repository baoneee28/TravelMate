# TravelMate — Test Report

**Phiên bản:** 1.6  
**Ngày kiểm thử:** 2026-05-29  
**Môi trường:** Spring Boot 3.5 + JUnit 5 + Mockito + MockMvc + Surefire

---

## 1. Tổng Quan

Kết quả được lấy từ `travelmate/target/surefire-reports/TEST-*.xml`.

| Hạng mục | Số lượng |
|---|---:|
| Tổng test cases | 247 |
| PASS | 247 |
| FAIL | 0 |
| ERROR | 0 |
| SKIPPED | 0 |
| Tỷ lệ pass | 100% |

**Kết quả surefire:** `Tests run: 247, Failures: 0, Errors: 0, Skipped: 0`.

---

## 2. Danh Sách Test Tự Động

| # | Test class | Số test | Trạng thái |
|---:|---|---:|---|
| 1 | `AccommodationPageControllerTravelSuggestionTest` | 7 | PASS |
| 2 | `PartnerWalletAndSettlementIntegrationTest` | 13 | PASS |
| 3 | `HomePageControllerNewsTest` | 1 | PASS |
| 4 | `TravelDestinationSqlTest` | 1 | PASS |
| 5 | `TravelPostSqlTest` | 6 | PASS |
| 6 | `UserPortalSqlSeedTest` | 9 | PASS |
| 7 | `GoogleOAuthClientConfigTest` | 4 | PASS |
| 8 | `OAuth2LoginSuccessHandlerTest` | 3 | PASS |
| 9 | `AccommodationServiceSearchTest` | 9 | PASS |
| 10 | `AvailabilityOverlapTest` | 9 | PASS |
| 11 | `AvailabilityServicePartnerTypeGuardTest` | 3 | PASS |
| 12 | `BookingCalculationTest` | 37 | PASS |
| 13 | `ChatbotServiceTest` | 14 | PASS |
| 14 | `CommissionServiceTest` | 3 | PASS |
| 15 | `ExcelExportServiceTest` | 5 | PASS |
| 16 | `FileStorageServiceTest` | 3 | PASS |
| 17 | `NoShowDepositTest` | 9 | PASS |
| 18 | `PartnerWalletServiceTest` | 11 | PASS |
| 19 | `PasswordResetServiceTest` | 12 | PASS |
| 20 | `PaymentServiceTest` | 6 | PASS |
| 21 | `RevenueServiceHomestayTest` | 4 | PASS |
| 22 | `ReviewServiceTest` | 3 | PASS |
| 23 | `RoomImageServiceTest` | 3 | PASS |
| 24 | `SettlementEligibilityTest` | 9 | PASS |
| 25 | `SettlementServiceTest` | 12 | PASS |
| 26 | `TravelPostServiceTest` | 4 | PASS |
| 27 | `VoucherCalculationTest` | 25 | PASS |
| 28 | `TravelmateApplicationTests` | 1 | PASS |
| 29 | `AdminTravelPostSidebarTest` | 1 | PASS |
| 30 | `TravelSuggestionHotelsTemplateTest` | 7 | PASS |
| 31 | `UserPortalFlowTemplateTest` | 10 | PASS |
| 32 | `DataInitializerTest` | 2 | PASS |
| 33 | `BusinessLabelUtilTest` | 1 | PASS |
| | **Tổng cộng** | **247** | **PASS** |

---

## 3. Phạm Vi Đã Bao Phủ

### Booking, Payment Và VNPAY
- Tính tổng tiền nhiều phòng/nhiều đêm.
- `DEPOSIT_30`: tính cọc 30%, remaining 70%, làm tròn xuống.
- `FULL_PAYMENT`: paidAmount bằng tổng tiền, remaining bằng 0.
- VNPAY success auto-confirm: Payment `APPROVED`, Booking `CONFIRMED`, Partner status `PENDING_PARTNER_CONFIRMATION`.
- VNPAY fail/cancel/expired: booking được hủy hoặc không giữ phòng sai.
- Guard không cho voucher làm số tiền thanh toán thấp hơn mức tối thiểu VNPAY.

### Partner Booking Và Availability
- Partner chỉ thấy booking đúng loại lưu trú đã đăng ký.
- Partner không xác nhận booking sai `partnerPropertyType`.
- Kiểm tra overlap ngày nhận/trả phòng.
- Direct booking và manual block không bị tính nhầm như booking online.

### Voucher
- Voucher percent/fixed amount.
- Cap giảm giá tối đa.
- Voucher Admin chịu và Partner chịu được ghi nhận đúng.
- Partner chỉ gắn voucher từ kho Admin cấp, không tạo voucher mới trực tiếp.

### Settlement, Wallet Và Withdrawal
- Điều kiện settlement:
  - `ONLINE + APPROVED + COMPLETED` hợp lệ.
  - `ONLINE + DEPOSIT_FORFEITED + NO_SHOW` hợp lệ.
  - `ONLINE + DEPOSIT_FORFEITED + CANCELLED` hợp lệ khi khách hủy và mất cọc.
  - `DIRECT`, `MANUAL_BLOCK`, `CONFIRMED`, `CHECKED_IN` và đơn hủy không giữ cọc không hợp lệ.
- Generate settlement tháng, ngày chi trả dự kiến mùng 10.
- Theo Hướng A: `DEPOSIT_30` chỉ tính hoa hồng trên tiền cọc online; thanh toán tại cơ sở không vào ví/quyết toán TravelMate.
- Footer settlement được cộng từ các dòng chi tiết; settlement cũ đang chờ được đối soát trước khi chi trả.
- Mark settlement `PAID` cộng payout vào ví Partner.
- Chống cộng ví trùng settlement.
- Partner rút tiền: yêu cầu bank info, chặn rút vượt số dư, phong tỏa số tiền đang chờ.
- Admin đánh dấu withdrawal `PAID` hoặc `REJECTED`, không xử lý lại yêu cầu đã xử lý.

### UI, Template, SQL Seed Và File
- Template user flow có link/route đúng.
- SQL seed có dữ liệu du lịch, bài viết, voucher, booking, ví, withdrawal và room images.
- File upload/storage được kiểm tra đường dẫn và ràng buộc cơ bản.
- Export Excel settlement/withdrawal trả file XLSX, có sheet/header và mask tài khoản ngân hàng.

### Authentication, Google Login Và Quên Mật Khẩu
- Google OAuth chỉ tạo/đăng nhập tài khoản `USER`, chặn `ADMIN`/`PARTNER` và user bị khóa.
- OAuth client chỉ được đăng ký khi có đủ Client ID/Secret; nếu thiếu app vẫn khởi động bình thường.
- Quên mật khẩu tạo token UUID dùng một lần, hết hạn sau 30 phút và vô hiệu token cũ.
- Nếu SMTP chưa cấu hình và không bật demo link, hệ thống không tạo token và vẫn trả thông báo chung để tránh lộ email.
- Nếu gửi email thất bại, token vừa tạo được vô hiệu hóa ngay.
- Review chỉ cho booking online đã hoàn tất và cập nhật rating thang 10.

---

## 4. Chiến Lược Kiểm Thử

| Loại | Công cụ | Mục tiêu |
|---|---|---|
| Unit test | JUnit 5 + Mockito | Kiểm tra business logic thuần |
| Integration test | Spring Boot Test + MockMvc | Kiểm route, phân quyền, form, export Excel |
| SQL/static test | AssertJ + đọc file | Kiểm dữ liệu khởi tạo và template không lệch nghiệp vụ |
| Manual test | Browser + MySQL local | Kiểm UI responsive, VNPAY Sandbox và trải nghiệm demo |

### Happy Path Chính
- User đặt phòng -> VNPAY thành công -> hệ thống auto-confirm -> Partner xác nhận giữ phòng -> check-in -> check-out -> settlement -> ví Partner.

### Alternative Path
- User hủy/thanh toán thất bại -> booking hủy, phòng mở lại.
- Giao dịch cần đối soát -> Admin xử lý ngoại lệ.
- Đối tác từ chối giữ phòng -> Admin xử lý hủy/hoàn tiền.
- No-show cọc 30% -> giữ cọc, đưa vào settlement nếu đủ điều kiện.

---

## 5. Checklist Manual Trước Demo

| Kịch bản | Trạng thái |
|---|---|
| Import `src/main/resources/travelmate_db.sql` vào MySQL 8.0+ | Cần chạy trên máy demo |
| User booking + VNPAY Sandbox success | Cần chạy browser |
| Payment result hiển thị "Đã thanh toán / Chờ đối tác xác nhận" | Cần chạy browser |
| Partner xác nhận giữ phòng, check-in, check-out | Cần chạy browser |
| Admin settlements, settlement detail, export Excel | Cần chạy browser |
| Partner wallet, cập nhật ngân hàng, yêu cầu rút tiền | Cần chạy browser |
| Admin withdrawals, export Excel, paid/reject | Cần chạy browser |
| UI desktop 1366px zoom 100% không dùng CSS `zoom` | Cần chạy browser |

---

## 6. Cách Chạy Test

```bash
cd travelmate
./mvnw test
```

Hoặc trên Windows:

```bash
cd travelmate
mvnw.cmd test
```

Kỳ vọng:

```text
Tests run: 247, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 7. Lưu Ý

- VNPAY gateway là hệ thống ngoài nên không mock toàn bộ gateway thật trong unit test.
- Luồng VNPAY Sandbox, responsive UI và Excel mở bằng phần mềm thật vẫn cần test thủ công trước buổi bảo vệ.
- Khi production, cần bật CSRF và đưa secret VNPAY/DB ra biến môi trường.
