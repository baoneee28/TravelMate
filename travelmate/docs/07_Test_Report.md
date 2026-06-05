# TravelMate — Test Report

**Phiên bản:** 2.0
**Ngày kiểm thử:** 2026-06-05
**Môi trường:** Spring Boot 3.5 + JUnit 5 + Mockito + MockMvc + Surefire

---

## 1. Tổng Quan

Kết quả được lấy từ `travelmate/target/surefire-reports/TEST-*.xml`.

| Hạng mục | Số lượng |
|---|---:|
| Tổng test cases | 279 |
| PASS | 279 |
| FAIL | 0 |
| ERROR | 0 |
| SKIPPED | 0 |
| Tỷ lệ pass | 100% |

**Kết quả surefire:** `Tests run: 279, Failures: 0, Errors: 0, Skipped: 0`.

---

## 2. Danh Sách Test Tự Động

| # | Test class | Số test | Trạng thái |
|---:|---|---:|---|
| 1 | `DataInitializerTest` | 5 | PASS |
| 2 | `AccommodationPageControllerTravelSuggestionTest` | 7 | PASS |
| 3 | `HomePageControllerNewsTest` | 1 | PASS |
| 4 | `PartnerWalletAndSettlementIntegrationTest` | 15 | PASS |
| 5 | `PaymentControllerTest` | 2 | PASS |
| 6 | `TravelDestinationSqlTest` | 1 | PASS |
| 7 | `TravelPostSqlTest` | 6 | PASS |
| 8 | `UserPortalSqlSeedTest` | 10 | PASS |
| 9 | `GoogleOAuthClientConfigTest` | 4 | PASS |
| 10 | `OAuth2LoginSuccessHandlerTest` | 4 | PASS |
| 11 | `AccommodationServiceSearchTest` | 9 | PASS |
| 12 | `AccommodationServiceTest` | 3 | PASS |
| 13 | `AvailabilityOverlapTest` | 9 | PASS |
| 14 | `AvailabilityServicePartnerTypeGuardTest` | 3 | PASS |
| 15 | `BookingCalculationTest` | 47 | PASS |
| 16 | `ChatbotServiceTest` | 14 | PASS |
| 17 | `CommissionServiceTest` | 3 | PASS |
| 18 | `EmailServiceTest` | 1 | PASS |
| 19 | `ExcelExportServiceTest` | 5 | PASS |
| 20 | `FileStorageServiceTest` | 3 | PASS |
| 21 | `NoShowDepositTest` | 9 | PASS |
| 22 | `PartnerWalletServiceTest` | 11 | PASS |
| 23 | `PasswordResetServiceTest` | 12 | PASS |
| 24 | `PaymentServiceTest` | 6 | PASS |
| 25 | `RevenueServiceHomestayTest` | 5 | PASS |
| 26 | `ReviewServiceTest` | 5 | PASS |
| 27 | `RoomImageServiceTest` | 3 | PASS |
| 28 | `SettlementEligibilityTest` | 9 | PASS |
| 29 | `SettlementServiceTest` | 12 | PASS |
| 30 | `TravelPostServiceTest` | 4 | PASS |
| 31 | `VoucherCalculationTest` | 26 | PASS |
| 32 | `TravelmateApplicationTests` | 1 | PASS |
| 33 | `AdminTravelPostSidebarTest` | 1 | PASS |
| 34 | `TravelSuggestionHotelsTemplateTest` | 7 | PASS |
| 35 | `UserPortalFlowTemplateTest` | 13 | PASS |
| 36 | `BusinessLabelUtilTest` | 3 | PASS |
| | **Tổng cộng** | **279** | **PASS** |

---

## 3. Phạm Vi Đã Bao Phủ

### Booking, Payment Và VNPAY
- Tính tổng tiền nhiều phòng/nhiều đêm.
- `DEPOSIT_30`: tính cọc 30%, remaining 70%, làm tròn xuống.
- `FULL_PAYMENT`: paidAmount bằng tổng tiền, remaining bằng 0.
- VNPAY success auto-confirm: Payment `APPROVED`, Booking `CONFIRMED`, Partner status `PARTNER_CONFIRMED`.
- VNPAY fail/cancel/expired: booking được hủy hoặc không giữ phòng sai.
- Guard không cho voucher làm số tiền thanh toán thấp hơn mức tối thiểu VNPAY.
- Return URL VNPAY success tới muộn sau khi payment đã EXPIRED/CANCELLED/FAILED không hiển thị thành công giả.
- Admin chỉ được ghi nhận hoàn tiền khi booking đang `REFUND_PENDING`; trạng thái khác bị chặn.

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
- Theo Hướng A: `DEPOSIT_30` tính hoa hồng theo tổng đơn gốc, nhưng payout chỉ lấy từ phần tiền TravelMate đã thu online; thanh toán tại cơ sở không vào ví/quyết toán TravelMate.
- Footer settlement được cộng từ các dòng chi tiết; settlement cũ đang chờ được đối soát trước khi chi trả.
- Mark settlement `PAID` cộng payout vào ví Partner.
- Chống cộng ví trùng settlement.
- Partner rút tiền: yêu cầu bank info, chặn rút vượt số dư, phong tỏa số tiền đang chờ.
- Admin đánh dấu withdrawal `PAID` hoặc `REJECTED`, không xử lý lại yêu cầu đã xử lý.

### UI, Template, SQL Seed Và File
- Template user flow có link/route đúng.
- Luồng đặt lại từ `CANCELLED`/`NO_SHOW` có guard chủ đơn, prefill ngày/số khách/số lượng và không chuyển voucher/thanh toán cũ sang đơn mới.
- Review/support user input dùng `th:text`, không dùng `th:utext`, tránh render HTML/script từ nội dung người dùng nhập.
- SQL seed có dữ liệu du lịch, bài viết, voucher, booking, ví, withdrawal và room images.
- File upload/storage được kiểm tra đường dẫn và ràng buộc cơ bản.
- Export Excel settlement/withdrawal trả file XLSX, có sheet/header và mask tài khoản ngân hàng.

### Authentication, Google Login Và Quên Mật Khẩu
- Google OAuth chỉ tạo/đăng nhập tài khoản `USER`, chặn `ADMIN`/`PARTNER` và user bị khóa.
- OAuth client chỉ được đăng ký khi có đủ Client ID/Secret; nếu thiếu app vẫn khởi động bình thường.
- Quên mật khẩu tạo token UUID dùng một lần, hết hạn sau 30 phút và vô hiệu token cũ.
- Nếu SMTP chưa cấu hình và không bật demo link, hệ thống không tạo token và vẫn trả thông báo chung để tránh lộ email.
- Nếu gửi email thất bại, token vừa tạo được vô hiệu hóa ngay.
- Review chỉ cho booking online đã hoàn tất; user chấm 1-5 sao và hệ thống quy đổi điểm hiển thị của cơ sở sang thang 10.
- Route security: `USER` không vào được `/admin` và `/partner`; `PARTNER` không vào được `/admin`; `ADMIN` không vào được workspace `/partner`.
- Doanh thu/quyết toán sau rà soát 2026-06-04: mọi booking online dùng tổng đơn gốc trước voucher làm cơ sở tính hoa hồng; phần tiền TravelMate giữ online, voucher Partner chịu và 70% trả tại cơ sở được tách rõ trên UI.

---

## 4. Chiến Lược Kiểm Thử

| Loại | Công cụ | Mục tiêu |
|---|---|---|
| Unit test | JUnit 5 + Mockito | Kiểm tra business logic thuần |
| Integration test | Spring Boot Test + MockMvc | Kiểm route, phân quyền, form, export Excel |
| SQL/static test | AssertJ + đọc file | Kiểm dữ liệu khởi tạo và template không lệch nghiệp vụ |
| Manual test | Browser + MySQL local | Kiểm UI responsive, VNPAY Sandbox và trải nghiệm demo |

### Happy Path Chính
- User đặt phòng -> VNPAY thành công -> hệ thống auto-confirm và auto-hold -> Partner check-in -> check-out -> settlement -> ví Partner.

### Alternative Path
- User hủy/thanh toán thất bại -> booking hủy, phòng mở lại.
- Giao dịch cần đối soát -> Admin xử lý ngoại lệ.
- Đối tác báo không thể tiếp nhận khách -> Admin xử lý hủy/hoàn tiền.
- No-show cọc 30% -> giữ cọc, đưa vào settlement nếu đủ điều kiện.
- Full payment no-show tự động bị chặn ở luồng Admin override; không chuyển booking thành CHECKED_IN giả, để Admin xử lý theo chính sách hoàn tiền/giữ phí riêng.

---

## 5. Checklist Manual Trước Demo

| Kịch bản | Trạng thái |
|---|---|
| Import `src/main/resources/travelmate_db.sql` vào MySQL 8.0+ | Cần chạy trên máy demo |
| User booking + VNPAY Sandbox success | Cần chạy browser |
| Payment result hiển thị "Đã thanh toán / Đã giữ phòng/căn" | Cần chạy browser |
| User bấm đặt lại từ đơn đã hủy/no-show, form booking prefill đúng và hiện ghi chú đơn mới độc lập | PASS browser 2026-06-03, Chrome headless desktop + mobile |
| Partner check-in, check-out | Cần chạy browser |
| Admin settlements, settlement detail, export Excel | Cần chạy browser |
| Partner wallet, cập nhật ngân hàng, yêu cầu rút tiền | Cần chạy browser |
| Admin withdrawals, export Excel, paid/reject | Cần chạy browser |
| UI desktop 1366px zoom 100% không dùng CSS `zoom` | Cần chạy browser |

**Smoke test session 2026-06-02:** app local start được ở `http://127.0.0.1:18080` và HTTP/session smoke test pass `23/23` checks cho public pages, login 3 role, User/Partner/Admin pages và cross-role guard.

**Browser re-test session 2026-06-03:** app local start được ở `http://127.0.0.1:18085`. Chrome headless đăng nhập `user@travelmate.vn`, mở `/my-bookings`, thấy 5 nút đặt lại, bấm/tap `/my-bookings/95/rebook` trên desktop và mobile, redirect đúng sang `/booking?roomId=3&checkIn=2026-06-03&checkOut=2026-06-05&adults=2&children=2&rooms=1`, có banner đơn mới độc lập và đủ query prefill. Screenshot lưu tại `C:\TravelMate\browser-desktop-rebook-booking-form.png` và `C:\TravelMate\browser-mobile-rebook-booking-form.png`.

**Browser re-test session 2026-06-04:** app local được kiểm tra ở `http://localhost:18085`. Browser login thật bằng 3 role `admin@travelmate.vn`, `partner@travelmate.vn`, `user@travelmate.vn` pass `16/16` checks: Admin/Partner revenue-settlement hiển thị quy tắc tài chính mới, Admin settlement mở sẵn nguyên tắc "tổng đơn gốc trước voucher" và "70% tại cơ sở", Partner form không còn trường hạng sao/số sao, User voucher/my-bookings/rebook hoạt động, cross-role guard trả về trang không có quyền. Không còn câu cũ kiểu Admin giữ 100% phần user đã thanh toán.

Checklist chi tiết để nhóm tự bấm lại trước bảo vệ nằm ở `docs/20_Defense_Browser_Test_Checklist.md`.

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
Tests run: 279, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 7. Lưu Ý

- VNPAY gateway là hệ thống ngoài nên không mock toàn bộ gateway thật trong unit test.
- Luồng VNPAY Sandbox, responsive UI và Excel mở bằng phần mềm thật vẫn cần test thủ công trước buổi bảo vệ.
- Khi production, cần bật CSRF và đưa secret VNPAY/DB ra biến môi trường.
