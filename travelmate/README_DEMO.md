# TravelMate — Hướng Dẫn Chạy Demo

> **Đồ án cơ sở** — Spring Boot MVC + Thymeleaf + MySQL + VNPAY Sandbox  
> Dành cho giảng viên/người chấm điểm và thành viên nhóm chạy trên máy khác.

---

## 1. Yêu Cầu Hệ Thống

| Thành phần | Phiên bản khuyến nghị |
|---|---|
| Java (JDK) | 21 |
| Maven | 3.9+ (hoặc dùng `./mvnw` đi kèm) |
| MySQL | 8.0+ |
| Trình duyệt | Chrome / Edge (VNPAY Sandbox hoạt động tốt nhất) |

> `travelmate_db.sql` dùng một số window function để khởi tạo lịch sử ví Partner, nên khuyến nghị MySQL 8.0+.

---

## 2. Import Database

**File demo chính:** `travelmate/src/main/resources/travelmate_db.sql`

> ⚠️ Chỉ dùng file này, **không** dùng file `_archive_*` trong thư mục `archive/`.

### Cách 1 — MySQL CLI
```bash
mysql -u root -p < travelmate/src/main/resources/travelmate_db.sql
```

### Cách 2 — DBeaver / MySQL Workbench
1. Kết nối tới MySQL server (port 3306)
2. Menu **File → Open SQL Script** (hoặc Ctrl+Shift+O)
3. Chọn `travelmate_db.sql`
4. Nhấn **Run Script** (F5 hoặc nút Execute)

### Kết quả sau import
```
users              : 11 records (1 admin, 4 user, 6 partner)
accommodations     : 14+ records (APPROVED + PENDING/REJECTED + ví đối tác)
rooms              : 31+ records
vouchers           : 5 records
bookings           : 32+ records (đủ trạng thái + quyết toán/ví)
payments           : tương ứng bookings
partner_settlements: 15+ kỳ quyết toán tháng (PAID + PENDING + tự động khởi tạo)
partner_wallets     : ví quyết toán nội bộ cho 6 Partner
partner_withdrawals : yêu cầu rút tiền PENDING/PAID/REJECTED + dữ liệu mẫu
wallet_transactions : lịch sử tiền vào/ra ví
reviews            : 16 records
support_tickets    : 10 records
travel_destinations: dữ liệu điểm đến du lịch
travel_posts       : bài viết gợi ý du lịch
```

---

## 3. Cấu Hình Ứng Dụng

File cấu hình: `travelmate/src/main/resources/application.properties`

### Điều chỉnh kết nối MySQL (nếu máy khác)
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/travelmate_db?...
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD_HERE
```

### VNPAY Sandbox
Cấu hình VNPAY hiện tại là **Sandbox** và lấy thông tin từ biến môi trường:
```properties
VNPAY_TMN_CODE=your_sandbox_tmn_code
VNPAY_HASH_SECRET=your_sandbox_hash_secret
vnpay.pay-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.return-url=http://localhost:8080/payment/vnpay-return
```

**Lưu ý khi test VNPAY:**
- Xem mẫu biến môi trường tại `docs/demo-env.example`
- Dùng thẻ test sandbox của VNPAY: số thẻ `9704198526191432198`, tên `NGUYEN VAN A`, ngày hết hạn `07/15`, OTP `123456`
- Return URL hoạt động trên `localhost:8080` không cần ngrok
- IPN URL cần ngrok nếu muốn test server-to-server callback (không bắt buộc cho demo local)

### Quên mật khẩu khi demo offline
Tính năng quên mật khẩu tạo token dùng một lần, hết hạn sau 30 phút và gửi email thật qua SMTP khi cấu hình đủ:

App tự đọc file `travelmate/.env.local` nếu có. File này đã được `.gitignore`, nên có thể dùng để cấu hình local mà không lộ secret khi push Git.

```properties
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_gmail_app_password
TRAVELMATE_PASSWORD_RESET_EMAIL_ENABLED=auto
TRAVELMATE_PASSWORD_RESET_DEMO_LINK=false
```

Khi cần demo offline không có SMTP, bật riêng link console:

```properties
TRAVELMATE_PASSWORD_RESET_DEMO_LINK=true
```

Thông báo trên giao diện luôn giữ dạng bảo mật: “Nếu email tồn tại trong hệ thống...”, không tiết lộ email có tồn tại hay không.

---

## 4. Khởi Chạy Ứng Dụng

```bash
# Vào thư mục project
cd travelmate

# Windows/VS Code: khuyến nghị dùng lệnh này để terminal hiển thị tiếng Việt đúng UTF-8
.\run-dev-utf8.cmd

# macOS/Linux hoặc terminal đã cấu hình UTF-8
./mvnw spring-boot:run

# Truy cập tại
# http://localhost:8080
```

Nếu bạn vẫn thấy chữ Việt bị vỡ trong VS Code, hãy mở terminal mới sau khi pull code.
Project đã cấu hình profile terminal UTF-8 trong `.vscode/settings.json`, còn file `logs/travelmate.log`
vẫn được ghi UTF-8 để đối soát khi cần.

---

## 5. Tài Khoản Demo

| Role | Email | Password | Mô tả |
|---|---|---|---|
| **ADMIN** | admin@travelmate.vn | admin123 | Quản trị hệ thống toàn bộ |
| **USER** | user@travelmate.vn | user123 | Khách đặt phòng (có sẵn bookings) |
| **USER 2** | user2@travelmate.vn | user123 | Tài khoản user phụ |
| **PARTNER** | partner@travelmate.vn | partner123 | Đối tác HOTEL — Đà Lạt (3 khách sạn) |
| **PARTNER 2** | partner2@travelmate.vn | partner123 | Đối tác RESORT — Nha Trang/Đà Nẵng |
| **PARTNER 3** | partner3@travelmate.vn | partner123 | Đối tác VILLA |
| **PARTNER 4** | partner4@travelmate.vn | partner123 | Đối tác HOMESTAY |
| **USER VÍ MẪU** | nguyenhuuan92@gmail.com | user123 | Tài khoản khách hàng chạy thử ví & quyết toán |
| **PARTNER PALACE** | contact@dalatpalacehotel.vn | partner123 | Đối tác (Đà Lạt Palace) có đầy đủ ví, ngân hàng và các yêu cầu rút tiền |
| **PARTNER RỪNG THÔNG** | info@rungthongdalat.vn | partner123 | Đối tác có doanh thu nhưng chưa cấu hình tài khoản ngân hàng (dùng để test chặn rút tiền) |

---

## 6. Kịch Bản Demo Chính

### 6.1 Flow Đặt Phòng + Thanh Toán VNPAY
1. Đăng nhập user@travelmate.vn
2. Vào trang chủ → chọn khách sạn → chọn phòng → **Đặt phòng**
3. Chọn **Cọc 30%** hoặc **Thanh toán toàn bộ**
4. Nhập mã voucher (thử: `SUMMER10`, `WELCOME50`, `TRAVEL15`)
5. Thanh toán qua VNPAY Sandbox → dùng thẻ test
6. VNPAY thành công → TravelMate tự xác nhận thanh toán, booking chuyển sang **Đã thanh toán / Chờ đối tác xác nhận giữ phòng**
7. Đăng nhập Partner → `/partner/bookings` → xác nhận giữ phòng, check-in/check-out

### 6.2 Flow Admin
- `/admin/bookings` — Danh sách booking, theo dõi trạng thái, xử lý đối soát ngoại lệ, partner hủy, hoàn tiền/no-show
- `/admin/accommodations` — Duyệt cơ sở lưu trú PENDING (id=10)
- `/admin/settlements` — Tạo quyết toán tháng, ngày chi trả dự kiến mùng 10, cộng payout vào ví Partner, xuất Excel chi tiết settlement
- `/admin/withdrawals` — Duyệt/từ chối yêu cầu rút tiền Partner, xuất Excel danh sách withdrawal
- `/admin/vouchers` — Quản lý voucher toàn hệ thống
- `/admin/revenue` — Tổng quan doanh thu, commission và số tiền chờ quyết toán

### 6.3 Flow Partner
- `/partner/bookings` — Xác nhận giữ phòng / Check-in / Check-out
- `/partner/revenue` — Doanh thu cá nhân
- `/partner/settlements` — Lịch sử quyết toán
- `/partner/wallet` — Ví quyết toán, lịch sử tiền vào/ra, cập nhật ngân hàng, yêu cầu rút tiền
- `/partner/vouchers` — Xem/gắn voucher được Admin cấp cho phòng/căn

### 6.4 Flow Ví Quyết Toán Partner
1. Admin vào `/admin/settlements` → xác nhận chi trả một settlement PENDING
2. Hệ thống chuyển settlement sang PAID và cộng payout vào ví Partner
3. Partner vào `/partner/wallet` → thấy số dư có thể rút, tổng đã nhận, tổng đã rút
4. Partner cập nhật tài khoản ngân hàng nếu thiếu
5. Partner gửi yêu cầu rút tiền
6. Admin vào `/admin/withdrawals` → xác nhận đã chuyển khoản hoặc từ chối
7. Nếu từ chối, hệ thống hoàn tiền về số dư khả dụng của Partner

> Ví này là sổ quyết toán nội bộ, không tích hợp chuyển khoản ngân hàng thật.

### 6.4.1 Kịch bản dữ liệu đối soát & quyết toán thực tế
Login `admin@travelmate.vn`, vào `/admin/settlements`, bấm **Tạo quyết toán tháng trước**.

Với đối tác `contact@dalatpalacehotel.vn`, settlement được tạo từ 2 booking đủ điều kiện:
- `BK-PL-FULL-001`: `ONLINE + COMPLETED + APPROVED`
- `BK-PL-NOSHOW-001`: `ONLINE + NO_SHOW + DEPOSIT_FORFEITED`

Hai booking mẫu sau bị loại khỏi settlement để kiểm tra điều kiện lọc:
- `BK-PL-DIRECT-001`: `booking_source = DIRECT` (Thanh toán tại quầy)
- `BK-PL-PENDING-001`: booking `CONFIRMED`, chưa hoàn tất

Số liệu kỳ vọng:
```text
grossAmount            = 2.250.000đ
commissionAmount       =   337.500đ
voucherDeductionAmount =   200.000đ
payoutAmount           = 1.712.500đ
status                 = PENDING
```

Login `contact@dalatpalacehotel.vn` vào `/partner/wallet` sẽ thấy ví đối tác:
```text
Số dư có thể rút = 2.200.000đ
Đang chờ rút     =   800.000đ
Tổng đã nhận     = 4.000.000đ
Tổng đã rút      = 1.000.000đ
```

Login `info@rungthongdalat.vn` vào `/partner/wallet` để test case có tiền nhưng chưa có tài khoản ngân hàng nên không được rút.

### 6.5 Flow Xuất Excel Đối Soát
1. Admin vào `/admin/settlements/{id}` → bấm **Xuất Excel**
2. File `settlement-{id}.xlsx` có sheet `Tong quan` và `Chi tiet booking`
3. Admin vào `/admin/withdrawals` → bấm **Xuất Excel**
4. File `partner-withdrawals.xlsx` có danh sách yêu cầu rút tiền, số tài khoản được mask

> Excel export dùng Apache POI streaming (`SXSSFWorkbook`) và tạo file nội bộ, không gọi dịch vụ online.  
> Khi dữ liệu lớn, streaming giúp ghi file theo từng cụm dòng thay vì giữ toàn bộ workbook trong RAM.

### 6.6 Google Login Thật Trên Localhost
Google Login là tính năng optional để demo điểm cộng. Hệ thống tự bật OAuth2 khi có đủ Google Client ID/Secret; nếu thiếu thì app vẫn chạy và nút Google ở trạng thái disabled.

1. Tạo OAuth Client trên Google Cloud Console
2. Cấu hình:
   - Authorized JavaScript origins: `http://localhost:8080`
   - Authorized redirect URIs: `http://localhost:8080/login/oauth2/code/google`
3. Set biến môi trường:
   - `GOOGLE_CLIENT_ID=...apps.googleusercontent.com`
   - `GOOGLE_CLIENT_SECRET=...`
   - `GOOGLE_OAUTH_ENABLED=false` chỉ dùng khi muốn tắt chủ động
   - Hoặc ghi các dòng này vào `travelmate/.env.local` để app tự đọc khi chạy local
4. Chạy app và bấm **Đăng nhập với Google** tại `/auth/login`

Rule bảo vệ:
- Google Login chỉ tạo/đăng nhập tài khoản `USER`
- Nếu email Google trùng `ADMIN` hoặc `PARTNER`, hệ thống chặn và yêu cầu đăng nhập bằng tài khoản hệ thống
- Nếu thiếu Client ID hoặc Client Secret, app vẫn chạy và nút Google hiển thị dạng disabled để không làm vỡ demo

### 6.7 Demo Dữ Liệu Có Sẵn
| Booking code | Trạng thái | Kịch bản demo |
|---|---|---|
| BK-LATA-STD-0001 | CONFIRMED / PENDING_PARTNER_CONFIRMATION | Đã cọc 30% — chờ đối tác xác nhận giữ phòng |
| BK-TLP-SUP-0001 | CONFIRMED / PENDING_PARTNER_CONFIRMATION | Đã thanh toán 100% — chờ đối tác xác nhận giữ phòng |
| BK-ANM-GDN-0001 | PENDING_ADMIN_APPROVAL | Ngoại lệ đối soát để Admin xử lý thủ công |
| BK-TLP-STD-0001 | NO_SHOW / DEPOSIT_FORFEITED | Demo mất cọc no-show |
| BK-LATA-FAM-0001 | COMPLETED | Đã có review |
| BK-TMG-PRE-0001 | CHECKED_IN | Đang lưu trú |
| BK-TLP-SUP-0002 | CONFIRMED / PARTNER_CANCELLED | Demo Admin xử lý đối tác từ chối giữ phòng |

---

## 7. Ghi Chú Bảo Mật (Demo)

### CSRF
CSRF hiện đang **tắt** (`csrf.disable()`) trong `SecurityConfig.java` để thuận tiện cho:
- Test form Thymeleaf nhanh khi demo local
- Tránh vỡ luồng POST khi trình bày đồ án

> **Câu trả lời bảo vệ nếu giảng viên hỏi:**  
> *"Trong phạm vi demo local của đồ án cơ sở, CSRF đang tắt để đơn giản hóa kiểm thử.  
> Trong môi trường production, nhóm sẽ bật CSRF và thêm `th:action` tự động inject  
> `_csrf` token vào mọi form Thymeleaf theo cơ chế chuẩn của Spring Security."*

### Secrets trong application.properties
Các thông tin cấu hình trong `application.properties` là **sandbox/demo**:
- VNPAY TMN Code và Hash Secret là tài khoản sandbox (không phải production)
- DB password `root` là cấu hình local default
- Khi triển khai production: load từ biến môi trường hoặc file config ngoài source

### Phân quyền Route
| Route | Quyền |
|---|---|
| `/admin/**` | Chỉ ADMIN |
| `/partner/**` | Chỉ PARTNER |
| `/oauth2/**`, `/login/oauth2/**` | Public cho Google OAuth2 callback khi bật cấu hình |
| `/booking/**`, `/my-bookings/**` | Chỉ USER |
| `/payment/vnpay/create/**` | Chỉ USER |
| `/payment/vnpay-return`, `/payment/vnpay-ipn` | Public (VNPAY callback) |
| `/api/chatbot/**` | Public |
| `GET /api/travel-posts/**` | Public |
| `POST/PUT/DELETE /api/travel-posts/**` | Chỉ ADMIN |
| `/`, `/accommodations/**`, `/travel`, `/news`, `/contact` | Public |

---

## 8. Chạy Test

```bash
cd travelmate
./mvnw test
# hoặc:
mvn test
```

Test bao gồm:
- Tổng hiện tại: **247 tests PASS** (247 pass, 0 fail, 0 error, 0 skipped theo surefire, chạy ngày 29/05/2026)
- `DataInitializerTest` — Phục hồi snapshot tiền tại cơ sở của đơn cọc cũ theo Hướng A
- `BookingCalculationTest` — Tính tiền DEPOSIT_30 / FULL_PAYMENT
- `VoucherCalculationTest` — Logic voucher PERCENT / FIXED_AMOUNT / VNPAY guard
- `SettlementEligibilityTest` — Điều kiện quyết toán
- `NoShowDepositTest` — No-show DEPOSIT_30 → DEPOSIT_FORFEITED + mở phòng
- `AccommodationServiceSearchTest` — Tìm kiếm theo thành phố/loại
- `ChatbotServiceTest` — Intent chatbot budget/travel
- `PartnerWalletServiceTest` — Cộng ví, chống cộng trùng, rút tiền, paid/reject withdrawal
- `SettlementServiceTest` — Generate settlement tháng, ngày chi trả mùng 10, mark PAID cộng ví
- `ExcelExportServiceTest` — Xuất Excel settlement/withdrawal, kiểm sheet/header/mask tài khoản
- `PartnerWalletAndSettlementIntegrationTest` — Phân quyền, wallet route, export Excel settlement/withdrawal và 404 khi export settlement ID sai

---

## 9. Cấu Trúc Project

```
travelmate/
├── src/main/java/com/travelmate/
│   ├── controller/          # MVC Controllers (page + API)
│   ├── service/             # Business Logic Layer
│   ├── repository/          # Spring Data JPA Repositories
│   ├── entity/              # JPA Entities + Enums
│   ├── dto/                 # Data Transfer Objects
│   ├── security/            # Spring Security Config
│   ├── config/              # App Config, DataInitializer
│   └── scheduler/           # BookingExpiryScheduler
├── src/main/resources/
│   ├── templates/           # Thymeleaf HTML templates
│   ├── static/              # CSS / JS / Images
│   ├── travelmate_db.sql    # ← FILE DEMO CHÍNH
│   └── application.properties
├── src/test/java/           # Unit tests
└── docs/                   # Tài liệu UML, SRS, Test Report
```

---

*TravelMate — Đồ án Cơ sở CNPM/PTTKHT | Spring Boot 3.5 + MySQL + VNPAY Sandbox*
