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
users              : 8 records (1 admin, 3 user, 4 partner)
accommodations     : 13 records (11 APPROVED + 2 PENDING/REJECTED)
rooms              : 30 records
vouchers           : 5 records
bookings           : 28+ records (đủ trạng thái)
payments           : tương ứng bookings
partner_settlements: có sẵn PAID + PENDING để demo
reviews            : 2 records
support_tickets    : 10 records
travel_destinations: seed dữ liệu du lịch
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
Cấu hình VNPAY hiện tại là **Sandbox** (chỉ dùng để test):
```properties
vnpay.tmn-code=JQLMS7O0
# vnpay.hash-secret đã được cấu hình trong application.properties
vnpay.pay-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.return-url=http://localhost:8080/payment/vnpay-return
```

**Lưu ý khi test VNPAY:**
- Dùng thẻ test sandbox của VNPAY: số thẻ `9704198526191432198`, tên `NGUYEN VAN A`, ngày hết hạn `07/15`, OTP `123456`
- Return URL hoạt động trên `localhost:8080` không cần ngrok
- IPN URL cần ngrok nếu muốn test server-to-server callback (không bắt buộc cho demo local)

---

## 4. Khởi Chạy Ứng Dụng

```bash
# Vào thư mục project
cd travelmate

# Build và chạy
./mvnw spring-boot:run
# hoặc:
mvn spring-boot:run

# Truy cập tại
# http://localhost:8080
```

---

## 5. Tài Khoản Demo

| Role | Email | Password | Mô tả |
|---|---|---|---|
| **ADMIN** | admin@travelmate.vn | admin123 | Quản trị hệ thống toàn bộ |
| **USER** | user@travelmate.vn | user123 | Khách đặt phòng (có sẵn bookings) |
| **USER 2** | user2@travelmate.vn | user123 | Tài khoản user phụ |
| **PARTNER** | partner@travelmate.vn | partner123 | Partner HOTEL — Đà Lạt (3 khách sạn) |
| **PARTNER 2** | partner2@travelmate.vn | partner123 | Partner RESORT — Nha Trang/Đà Nẵng |
| **PARTNER 3** | partner3@travelmate.vn | partner123 | Partner VILLA |
| **PARTNER 4** | partner4@travelmate.vn | partner123 | Partner HOMESTAY |

---

## 6. Kịch Bản Demo Chính

### 6.1 Flow Đặt Phòng + Thanh Toán VNPAY
1. Đăng nhập user@travelmate.vn
2. Vào trang chủ → chọn khách sạn → chọn phòng → **Đặt phòng**
3. Chọn **Cọc 30%** hoặc **Thanh toán toàn bộ**
4. Nhập mã voucher (thử: `SUMMER10`, `WELCOME50`, `TRAVEL15`)
5. Thanh toán qua VNPAY Sandbox → dùng thẻ test
6. Đăng nhập admin → **Quản lý đặt phòng** → Duyệt booking

### 6.2 Flow Admin
- `/admin/bookings` — Danh sách booking, duyệt/từ chối, đánh dấu No-Show
- `/admin/accommodations` — Duyệt cơ sở lưu trú PENDING (id=10)
- `/admin/settlements` — Tạo quyết toán tháng → xem báo cáo
- `/admin/vouchers` — Quản lý voucher toàn hệ thống
- `/admin/revenue` — Biểu đồ doanh thu 5 tuần

### 6.3 Flow Partner
- `/partner/bookings` — Xác nhận giữ phòng / Check-in / Check-out
- `/partner/revenue` — Doanh thu cá nhân
- `/partner/settlements` — Lịch sử quyết toán
- `/partner/vouchers` — Tạo voucher cho cơ sở/phòng

### 6.4 Demo Dữ Liệu Có Sẵn
| Booking code | Trạng thái | Kịch bản demo |
|---|---|---|
| BK-LATA-STD-0001 | PENDING_ADMIN_APPROVAL | Admin duyệt — cọc 30% |
| BK-TLP-SUP-0001 | PENDING_ADMIN_APPROVAL | Admin từ chối — 100% |
| BK-TLP-STD-0001 | NO_SHOW / DEPOSIT_FORFEITED | Demo mất cọc no-show |
| BK-LATA-FAM-0001 | COMPLETED | Đã có review |
| BK-TMG-PRE-0001 | CHECKED_IN | Đang lưu trú |
| BK-TLP-SUP-0002 | CONFIRMED / PARTNER_CANCELLED | Demo admin xử lý partner hủy |

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
- `BookingCalculationTest` — Tính tiền DEPOSIT_30 / FULL_PAYMENT
- `VoucherCalculationTest` — Logic voucher PERCENT / FIXED_AMOUNT / VNPAY guard
- `SettlementEligibilityTest` — Điều kiện quyết toán
- `NoShowDepositTest` — No-show DEPOSIT_30 → DEPOSIT_FORFEITED + mở phòng
- `AccommodationServiceSearchTest` — Tìm kiếm theo thành phố/loại
- `ChatbotServiceTest` — Intent chatbot budget/travel

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
