# TravelMate — Đặc Tả Yêu Cầu Phần Mềm (SRS)

**Phiên bản:** 1.0  
**Ngày:** 2026-05-20  
**Môn học:** Phân Tích Thiết Kế Hệ Thống / Công Nghệ Phần Mềm  
**Nền tảng:** Spring Boot 3.5 + Thymeleaf + MySQL + VNPAY Sandbox

---

## 1. Giới Thiệu

### 1.1 Mục Đích
TravelMate là hệ thống đặt phòng khách sạn trực tuyến, hỗ trợ ba nhóm người dùng: Khách hàng (User), Đối tác chỗ lưu trú (Partner) và Quản trị viên (Admin). Hệ thống tích hợp thanh toán VNPAY, quản lý quyết toán doanh thu, chatbot tư vấn và gợi ý du lịch.

### 1.2 Phạm Vi
- Tìm kiếm và đặt phòng trực tuyến
- Thanh toán qua VNPAY (cọc 30% hoặc 100%)
- Quản lý vòng đời booking: PENDING → CONFIRMED → CHECKED_IN → COMPLETED
- Quản lý đối tác và quyết toán doanh thu hàng tháng
- Chatbot tư vấn du lịch theo ngân sách
- Hệ thống voucher giảm giá (ADMIN/PARTNER)

### 1.3 Định Nghĩa và Viết Tắt

| Thuật ngữ | Ý nghĩa |
|---|---|
| Booking | Đơn đặt phòng |
| DEPOSIT_30 | Hình thức cọc trước 30% qua VNPAY |
| FULL_PAYMENT | Thanh toán toàn bộ qua VNPAY |
| Settlement | Quyết toán doanh thu cho Partner |
| No-show | Khách đặt phòng nhưng không đến check-in |
| Commission | Hoa hồng TravelMate thu từ Partner |
| VNPAY | Cổng thanh toán trực tuyến |

---

## 2. Mô Tả Tổng Thể

### 2.1 Perspective

```
User/Browser ──► TravelMate Web App (Spring Boot / Thymeleaf)
                          │
              ┌───────────┼───────────────────┐
              ▼           ▼                   ▼
          MySQL DB    VNPAY Sandbox     File Storage
                                        (uploads/)
```

### 2.2 Chức Năng Chính

| Nhóm | Chức năng |
|---|---|
| **Công khai** | Tìm kiếm chỗ lưu trú, xem chi tiết phòng, đọc bài gợi ý du lịch, chatbot |
| **User** | Đăng ký/đăng nhập, đặt phòng, thanh toán VNPAY, xem lịch sử, đánh giá, dùng voucher |
| **Partner** | Quản lý cơ sở lưu trú, xác nhận booking, check-in/out, xem doanh thu, tạo voucher |
| **Admin** | Duyệt booking/accommodation, quản lý người dùng, tạo voucher, quyết toán, báo cáo |

### 2.3 Người Dùng

| Actor | Mô tả |
|---|---|
| **Guest** | Khách vãng lai, chỉ xem, không đặt phòng |
| **User** | Khách hàng đã đăng ký, có thể đặt phòng và thanh toán |
| **Partner** | Chủ cơ sở lưu trú, quản lý phòng và xác nhận booking |
| **Admin** | Quản trị viên hệ thống, toàn quyền |

---

## 3. Yêu Cầu Chức Năng

### 3.1 Module Tìm Kiếm & Xem (UC-01 → UC-05)

**UC-01: Tìm kiếm chỗ lưu trú**
- Lọc theo: thành phố, loại (Hotel/Villa/Homestay/Resort), giá
- Sắp xếp: rating, giá, tên
- Hiển thị: thumbnail, rating, giá/đêm, số phòng còn

**UC-02: Xem chi tiết chỗ lưu trú**
- Thông tin: mô tả, địa chỉ, ảnh, tiện nghi
- Danh sách phòng với giá, số lượng còn

**UC-03: Xem chi tiết phòng**
- Thông tin: mô tả, ảnh, tiện nghi, giá/đêm, loại phòng

**UC-04: Xem bài gợi ý du lịch**
- Danh sách bài viết, lọc theo điểm đến

**UC-05: Chatbot tư vấn**
- Hỏi theo ngân sách ("5 triệu đi đâu?")
- Hỏi theo điểm đến ("Đà Lạt có gì?")
- Gợi ý phòng phù hợp

### 3.2 Module Đặt Phòng & Thanh Toán (UC-06 → UC-12)

**UC-06: Đặt phòng**
- Pre-condition: User đã đăng nhập
- Input: check-in/out, số người lớn/trẻ em, số phòng, paymentOption, voucherCode
- Validation: ngày hợp lệ, phòng còn đủ, voucher hợp lệ
- Process: tính tiền → tạo Booking (PENDING_PAYMENT) → redirect VNPAY

**UC-07: Thanh toán VNPAY (cọc 30%)**
- User chọn DEPOSIT_30
- paidAmount = floor(totalAmount × 30%)
- VNPAY xử lý → return URL → Booking chờ admin duyệt

**UC-08: Thanh toán VNPAY (100%)**
- User chọn FULL_PAYMENT
- paidAmount = totalAmount
- VNPAY xử lý → return URL → Booking chờ admin duyệt

**UC-09: Xem lịch sử đặt phòng**
- Danh sách booking của User đang đăng nhập
- Lọc theo trạng thái

**UC-10: Đánh giá sau lưu trú**
- Pre-condition: booking COMPLETED
- Input: rating (1–5), comment
- Cập nhật rating trung bình của accommodation

**UC-11: Dùng voucher**
- User nhập mã voucher khi đặt phòng
- Hệ thống validate và áp giảm giá
- Ghi nhận costBearer (ADMIN/PARTNER) cho quyết toán

**UC-12: Hủy booking**
- User hủy booking PENDING hoặc CONFIRMED
- Cập nhật trạng thái, mở lại phòng

### 3.3 Module Admin (UC-13 → UC-20)

**UC-13: Duyệt booking**
- Admin xem danh sách PENDING_ADMIN_APPROVAL
- Approve → CONFIRMED + notify partner
- Reject → CANCELLED + hoàn tiền

**UC-14: Duyệt cơ sở lưu trú**
- Admin duyệt/từ chối accommodation PENDING

**UC-15: Đánh dấu No-Show**
- DEPOSIT_30: NO_SHOW + DEPOSIT_FORFEITED + mở lại phòng
- FULL_PAYMENT: CHECKED_IN (xử lý theo chính sách)

**UC-16: Quản lý voucher**
- Tạo voucher USER_GLOBAL (PERCENT/FIXED_AMOUNT)
- Quản lý voucher của Partner

**UC-17: Tạo quyết toán tháng**
- Tính gross, commission, voucher deduction, payout cho từng partner
- Chỉ tính booking ONLINE + COMPLETED + APPROVED
- Chỉ tính booking ONLINE + NO_SHOW + DEPOSIT_FORFEITED

**UC-18: Xem báo cáo doanh thu**
- Tổng quan doanh thu, commission và số tiền chờ quyết toán
- Phân tích theo loại accommodation

**UC-19: Quản lý người dùng**
- Xem/sửa thông tin user, partner
- Thay đổi role, trạng thái

**UC-20: Quản lý yêu cầu hỗ trợ**
- Xem và phản hồi support tickets từ Partner/User/Guest

### 3.4 Module Partner (UC-21 → UC-27)

**UC-21: Quản lý cơ sở lưu trú**
- Thêm/sửa accommodation (chờ admin duyệt)
- Upload ảnh thumbnail

**UC-22: Quản lý phòng**
- Thêm/sửa/xóa phòng (chỉ với accommodation APPROVED)
- Cài đặt commission_rate_override

**UC-23: Xác nhận booking**
- Partner xác nhận giữ phòng cho booking CONFIRMED

**UC-24: Check-in khách**
- Đánh dấu khách đã check-in → CHECKED_IN

**UC-25: Check-out khách**
- Đánh dấu hoàn tất → COMPLETED
- Xác nhận thu 70% còn lại (nếu DEPOSIT_30)

**UC-26: Tạo voucher**
- Voucher cho accommodation cụ thể (PARTNER_ACCOMMODATION)
- Voucher cho phòng cụ thể (PARTNER_ROOM)

**UC-27: Xem doanh thu và quyết toán**
- Doanh thu vận hành theo khoảng thời gian và theo tháng
- Lịch sử quyết toán

---

## 4. Yêu Cầu Phi Chức Năng

| Loại | Yêu cầu |
|---|---|
| **Bảo mật** | BCrypt password, phân quyền theo role, HTTPS production |
| **Hiệu năng** | Trang chủ tải < 3s, search < 2s trên data demo |
| **Tính sẵn sàng** | Single server, no HA requirement (phạm vi đồ án) |
| **Tính bảo trì** | MVC layered architecture, clear package structure |
| **Tương thích** | Chrome, Edge, Firefox mới nhất |
| **Dữ liệu** | MySQL 8.0+, UTF-8MB4, backup thủ công |

---

## 5. Ràng Buộc

- **Công nghệ:** Java 21, Spring Boot 3.5, Thymeleaf, MySQL 8.0
- **Thanh toán:** Chỉ VNPAY Sandbox (không tích hợp production)
- **Session:** Server-side HttpSession (không dùng JWT)
- **CSRF:** Tắt trong demo local, cần bật khi production
- **Phạm vi:** Single-server deployment, không microservices
