# TravelMate — Đặc Tả Yêu Cầu Phần Mềm (SRS)

**Phiên bản:** 1.4  
**Ngày cập nhật:** 2026-05-25  
**Nền tảng:** Spring Boot 3.5 + Thymeleaf + MySQL + VNPAY Sandbox

---

## 1. Giới Thiệu

### 1.1 Mục Đích
TravelMate là hệ thống đặt phòng trực tuyến quy mô đồ án cơ sở, hỗ trợ ba nhóm người dùng: Khách hàng, Đối tác lưu trú và Quản trị viên. Hệ thống tập trung vào trải nghiệm đặt phòng, thanh toán VNPAY, xác nhận giữ phòng từ đối tác, voucher, quyết toán doanh thu, ví nội bộ đối tác và báo cáo đối soát.

### 1.2 Phạm Vi
- Tìm kiếm và xem chi tiết khách sạn, resort, villa, homestay.
- Đặt phòng trực tuyến với hai hình thức: cọc 30% hoặc thanh toán 100% qua VNPAY Sandbox.
- VNPAY thành công thì hệ thống tự xác nhận thanh toán, booking chuyển sang `CONFIRMED` và chờ đối tác xác nhận giữ phòng.
- Đối tác xác nhận giữ phòng, check-in, xác nhận thu 70% tại cơ sở nếu booking cọc 30%, check-out hoặc no-show.
- Admin quản lý người dùng, cơ sở lưu trú, phòng, bài viết, voucher, đối soát ngoại lệ, hoàn tiền, quyết toán và yêu cầu rút tiền.
- Voucher do Admin phát hành; `costBearer = ADMIN/PARTNER` quyết định bên chịu chi phí giảm giá.
- Quyết toán tháng và ví nội bộ cho đối tác, bao gồm lịch sử tiền vào/ra và yêu cầu rút tiền.
- Chatbot tư vấn du lịch theo ngân sách và điểm đến.

### 1.3 Thuật Ngữ

| Thuật ngữ | Ý nghĩa |
|---|---|
| Booking | Đơn đặt phòng/căn |
| DEPOSIT_30 | User thanh toán trước 30% qua VNPAY, 70% còn lại trả tại cơ sở |
| FULL_PAYMENT | User thanh toán 100% qua VNPAY |
| Partner status | Trạng thái xác nhận giữ phòng từ phía đối tác |
| Settlement | Bản quyết toán doanh thu theo tháng cho đối tác |
| Partner wallet | Ví quyết toán nội bộ của đối tác |
| Voucher cost bearer | Bên chịu chi phí voucher: TravelMate/Admin hoặc Partner |
| No-show | Khách không đến check-in |

---

## 2. Mô Tả Tổng Thể

### 2.1 Kiến Trúc Tổng Quan

```text
Browser/User
    |
    v
TravelMate Spring Boot MVC + Thymeleaf
    |
    +-- MySQL Database
    +-- VNPAY Sandbox
    +-- Local file storage uploads/
```

### 2.2 Nhóm Chức Năng

| Nhóm | Chức năng chính |
|---|---|
| Công khai | Trang chủ, tìm kiếm, chi tiết lưu trú, bài viết du lịch, chatbot |
| User | Đăng ký/đăng nhập, đặt phòng, thanh toán VNPAY, xem lịch sử, hủy, đánh giá, dùng voucher |
| Partner | Quản lý cơ sở/phòng, gắn voucher được cấp, xác nhận giữ phòng, check-in/out, xem doanh thu, ví và rút tiền |
| Admin | Duyệt listing, quản lý booking/đối soát, voucher, doanh thu, settlement, withdrawal, user, support |

---

## 3. Yêu Cầu Chức Năng

### 3.1 Tìm Kiếm, Nội Dung Và Chatbot
- User/Guest tìm kiếm lưu trú theo điểm đến, loại hình, giá, rating và số khách.
- Trang chi tiết hiển thị ảnh, mô tả, tiện nghi, phòng/căn, giá và số lượng còn.
- Bài viết du lịch lọc theo điểm đến, có empty state khi không có kết quả.
- Chatbot hỗ trợ hỏi theo ngân sách, điểm đến và từ chối lịch sự câu hỏi ngoài phạm vi du lịch.

### 3.2 Đặt Phòng Và Thanh Toán
- User chọn phòng/căn, ngày nhận/trả, số khách, số lượng, voucher và hình thức thanh toán.
- Hệ thống kiểm tra ngày hợp lệ, phòng còn đủ, voucher hợp lệ và tổng sau giảm không thấp hơn mức tối thiểu VNPAY.
- Khi tạo booking, trạng thái ban đầu là `PENDING_PAYMENT`, payment là `PENDING_PAYMENT`, phòng được giữ tạm.
- Nếu user không thanh toán trong thời gian cấu hình, booking hết hạn và phòng được mở lại.
- Nếu VNPAY thành công và chữ ký hợp lệ:
  - `PaymentStatus = APPROVED`
  - `BookingStatus = CONFIRMED`
  - `PartnerBookingStatus = PENDING_PARTNER_CONFIRMATION`
  - Hệ thống gửi booking sang đối tác để xác nhận giữ phòng.
- Nếu VNPAY thất bại/hủy/hết hạn:
  - Booking chuyển `CANCELLED` hoặc trạng thái lỗi phù hợp.
  - Phòng được mở lại.
- Admin không duyệt thủ công các giao dịch VNPAY thành công thông thường; Admin chỉ xử lý ngoại lệ đối soát, từ chối, hoàn tiền hoặc khiếu nại.

### 3.3 Voucher
- Admin tạo voucher toàn hệ thống hoặc voucher thuộc kho Partner/Room.
- Partner không tự tạo voucher mới; Partner chỉ xem/gắn voucher được Admin cấp vào phòng/căn thuộc quyền quản lý nếu hệ thống cho phép.
- `costBearer = ADMIN` nghĩa là TravelMate chịu chi phí giảm giá, không trừ payout Partner.
- `costBearer = PARTNER` nghĩa là phần giảm giá được trừ vào quyết toán của Partner.
- Booking lưu `voucherCode`, `discountAmount`, `voucherCostBearer` để phục vụ đối soát.

### 3.4 Vòng Đời Booking Sau Thanh Toán
- `CONFIRMED + PENDING_PARTNER_CONFIRMATION`: đã thanh toán, chờ đối tác xác nhận giữ phòng.
- `CONFIRMED + PARTNER_CONFIRMED`: đối tác đã xác nhận giữ phòng.
- `CHECKED_IN`: khách đang lưu trú.
- `COMPLETED`: khách đã trả phòng, booking đủ điều kiện quyết toán nếu payment hợp lệ.
- `NO_SHOW + DEPOSIT_FORFEITED`: khách không đến với booking cọc 30%, cọc bị giữ và có thể đưa vào quyết toán.
- `CANCELLED + DEPOSIT_FORFEITED`: khách hủy booking cọc sau khi đã thanh toán, cọc bị giữ và có thể đưa vào quyết toán.
- `CANCELLED + REFUND_PENDING`: booking đã thanh toán bị hủy, chờ Admin xử lý hoàn tiền.

### 3.5 Admin
- Duyệt/từ chối cơ sở lưu trú, phòng và nội dung cần kiểm duyệt.
- Theo dõi booking, lọc theo trạng thái, xử lý đối soát ngoại lệ, partner hủy, hoàn tiền và no-show.
- Tạo và quản lý voucher, phạm vi áp dụng và bên chịu chi phí.
- Tạo quyết toán tháng, xem chi tiết booking đủ điều kiện, xuất Excel đối soát.
- Xác nhận settlement đã chuyển khoản để cộng ví nội bộ Partner.
- Xử lý yêu cầu rút tiền: chuyển khoản ngoài hệ thống, sau đó cập nhật `PAID` hoặc `REJECTED`.

### 3.6 Partner
- Quản lý cơ sở lưu trú và phòng/căn trong phạm vi loại hình đã đăng ký.
- Xác nhận hoặc từ chối giữ phòng sau khi VNPAY thành công.
- Check-in khách; với booking cọc 30%, xác nhận đã thu 70% còn lại tại cơ sở.
- Check-out để hoàn tất booking.
- Đánh dấu no-show cho booking cọc 30% theo quyền/hành động được hệ thống cho phép.
- Xem doanh thu, lịch sử quyết toán, ví, transaction và gửi yêu cầu rút tiền.
- Gửi ticket hỗ trợ hoặc yêu cầu hỗ trợ khuyến mãi khi cần.

---

## 4. Yêu Cầu Phi Chức Năng

| Loại | Yêu cầu |
|---|---|
| Bảo mật | BCrypt password, phân quyền route theo role, secrets production lấy từ môi trường |
| Tính đúng nghiệp vụ | Không quyết toán booking chưa hoàn tất; không trừ voucher Admin chịu khỏi payout Partner |
| Khả dụng demo | Import một file SQL gốc chạy được trên máy khác, dữ liệu đủ trạng thái để giảng viên quan sát |
| UI/UX | Bảng dài cuộn ngang trong vùng bảng, không dùng CSS `zoom`, trạng thái hiển thị rõ bằng badge |
| Kiểm thử | Maven test pass, report ghi đúng số test thực tế từ surefire |
| Tương thích | Chrome/Edge/Firefox mới, MySQL 8.0+, Java 21 |

---

## 5. Ràng Buộc

- VNPAY chỉ dùng Sandbox trong phạm vi đồ án.
- Ví Partner là sổ quyết toán nội bộ, không chuyển khoản ngân hàng tự động.
- Admin chuyển khoản ngoài hệ thống rồi cập nhật trạng thái trong TravelMate.
- CSRF đang tắt cho demo local; khi production cần bật lại và cấu hình token cho form Thymeleaf.
