# TravelMate — Demo Script Bảo Vệ

**Thời gian:** 15-20 phút  
**URL:** http://localhost:8080  
**Chuẩn bị:** Import `travelmate_db.sql`, chạy app, mở Chrome zoom 100%

---

## Chuẩn Bị

1. Import database từ `travelmate/src/main/resources/travelmate_db.sql`.
2. Chạy ứng dụng:
   ```bash
   cd travelmate
   ./mvnw spring-boot:run
   ```
3. Chuẩn bị thẻ VNPAY Sandbox:
   - Số thẻ: `9704198526191432198`
   - Tên: `NGUYEN VAN A`
   - Ngày: `07/15`
   - OTP: `123456`
4. Tài khoản dùng khi demo:
   - User: `user@travelmate.vn / user123`
   - Admin: `admin@travelmate.vn / admin123`
   - Partner Hotel: `partner@travelmate.vn / partner123`
   - Partner wallet mẫu: `contact@dalatpalacehotel.vn / partner123`
   - Partner thiếu bank info: `info@rungthongdalat.vn / partner123`

---

## 1. Trang Chủ, Search Và Chi Tiết Lưu Trú

**Mục tiêu:** Cho giảng viên thấy dữ liệu thật, UI trực quan, không chỉ là form trống.

1. Vào trang chủ.
2. Tìm "Đà Lạt".
3. Mở một khách sạn như LATA/Tulip/TM Grand.
4. Chỉ rõ ảnh, tiện nghi, phòng, giá, số lượng còn.
5. Resize nhanh hoặc dùng laptop 1366px để thấy layout vẫn ổn.

---

## 2. Chatbot Và Bài Viết Du Lịch

1. Mở chatbot.
2. Hỏi: `Tôi có 3 triệu đi đâu được?`
3. Hỏi: `5 triệu đi Đà Lạt 3 ngày`
4. Hỏi ngoài phạm vi: `Viết code Java cho tôi`
5. Mở `/travel` hoặc `/news`, click tag điểm đến để thấy lọc và empty state.

---

## 3. User Đặt Phòng + VNPAY Auto Confirm

### 3.1 Đăng nhập và đặt phòng
1. Đăng nhập `user@travelmate.vn / user123`.
2. Vào `/accommodations`, chọn phòng còn trống.
3. Chọn ngày nhận/trả phòng.
4. Chọn `Cọc 30%` hoặc `Thanh toán 100%`.
5. Nhập voucher như `SUMMER10`, `WELCOME50`, `TRAVEL15`.
6. Nhấn xác nhận đặt phòng.

### 3.2 Thanh toán VNPAY
1. Chuyển sang VNPAY Sandbox.
2. Nhập thẻ test và OTP.
3. Sau khi thanh toán thành công, TravelMate hiển thị:
   - VNPAY đã xác minh.
   - TravelMate tự ghi nhận thanh toán.
   - Booking chờ đối tác xác nhận giữ phòng.

**Điểm nhấn khi bảo vệ:** Không còn flow cũ phải đợi Admin sau khi VNPAY thành công. Admin chỉ xử lý đối soát ngoại lệ, hoàn tiền hoặc khiếu nại.

---

## 4. User Xem Lịch Sử Đặt Phòng

1. Vào `/my-bookings`.
2. Lọc tab "Đã thanh toán".
3. Chỉ rõ các nhãn:
   - `Đã thanh toán — Chờ đối tác xác nhận giữ phòng`
   - `Đối tác đã xác nhận giữ phòng`
   - `Đang lưu trú`
   - `Hoàn thành`
4. Với booking cọc 30%, chỉ rõ dòng `Còn 70% thanh toán tại cơ sở`.
5. Mở chi tiết/hóa đơn để thấy trạng thái thanh toán và trạng thái đối tác.

---

## 5. Partner Xác Nhận, Check-In, Check-Out

1. Đăng nhập `partner@travelmate.vn / partner123`.
2. Vào `/partner/bookings`.
3. Lọc hoặc quan sát booking đang `Chờ xác nhận giữ phòng`.
4. Bấm `Xác nhận giữ phòng`.
5. Với booking đã xác nhận:
   - Check-in khách.
   - Nếu là cọc 30%, xác nhận thu 70% tại cơ sở.
   - Check-out để hoàn tất.
6. Chỉ rõ bảng dài có scroll ngang trong card, action không chồng chữ.

---

## 6. Admin Quản Lý Booking Và Đối Soát

1. Đăng nhập `admin@travelmate.vn / admin123`.
2. Vào `/admin/bookings`.
3. Chỉ rõ:
   - Booking VNPAY success đã `CONFIRMED`.
   - Cột đối tác cho biết `Chờ đối tác` hoặc `Đối tác đã xác nhận`.
   - Filter `Cần đối soát` chỉ dành cho ngoại lệ.
4. Demo dữ liệu có sẵn:
   - `BK-ANM-GDN-0001`: ngoại lệ cần Admin đối soát.
   - `BK-TLP-STD-0001`: no-show, cọc bị giữ.
   - `BK-TMG-PRE-0001`: đang lưu trú.
   - `BK-LATA-FAM-0001`: hoàn tất.
   - `BK-TLP-SUP-0002`: partner từ chối, Admin xử lý.

---

## 7. Admin Voucher

1. Vào `/admin/vouchers`.
2. Chỉ rõ Admin tạo voucher và chọn:
   - Phạm vi: toàn hệ thống hoặc kho Partner/Room.
   - Bên chịu chi phí: `ADMIN` hoặc `PARTNER`.
3. Nhấn mạnh Partner không tự tạo voucher mới; Partner chỉ xem/gắn voucher được cấp hoặc gửi yêu cầu hỗ trợ khuyến mãi.
4. Nói rõ settlement chỉ trừ Partner khi `costBearer = PARTNER`.

---

## 8. Settlement, Wallet Và Withdrawal

### 8.1 Admin settlement
1. Vào `/admin/settlements`.
2. Nhấn `Tạo quyết toán tháng trước`.
3. Mở một settlement detail và bấm `Xuất Excel`.
4. Chỉ rõ công thức:
   ```text
   payout = tiền TravelMate thu online - commission - voucher Partner chịu
   ```
5. Dữ liệu mẫu `contact@dalatpalacehotel.vn`:
   - Gross: 2.250.000đ
   - Commission: 337.500đ
   - Voucher Partner chịu: 200.000đ
   - Payout: 1.712.500đ

### 8.2 Partner wallet
1. Đăng nhập `contact@dalatpalacehotel.vn / partner123`.
2. Vào `/partner/wallet`.
3. Chỉ rõ KPI:
   - Số dư có thể rút: 2.200.000đ
   - Đang chờ rút: 800.000đ
   - Tổng đã nhận: 4.000.000đ
   - Tổng đã rút: 1.000.000đ
4. Xem transaction history và withdrawal history.
5. Đăng nhập `info@rungthongdalat.vn / partner123`, thử rút tiền để thấy hệ thống bắt cập nhật bank info.

### 8.3 Admin withdrawal
1. Vào `/admin/withdrawals`.
2. Xuất Excel danh sách withdrawal.
3. Với request `PENDING`, nhập mã giao dịch demo rồi bấm đã chuyển.
4. Hoặc từ chối để hệ thống hoàn tiền về ví Partner.

---

## 9. Admin Listing, Revenue Và Support

1. `/admin/accommodations`: duyệt/từ chối cơ sở lưu trú pending.
2. `/admin/revenue`: xem doanh thu, commission, payment online, quyết toán.
3. `/admin/support`: xem ticket từ User/Partner.
4. `/admin/travel-posts`: quản lý bài viết du lịch dùng cho trang nội dung và chatbot.

---

## 10. Testing

Khi trình bày phần kiểm thử, dùng số liệu mới:

```text
Tests run: 234
Failures: 0
Errors: 0
Skipped: 0
```

Nhóm test nổi bật:
- Booking/payment/VNPAY auto-confirm.
- Voucher `ADMIN/PARTNER costBearer`.
- Settlement eligibility và wallet/withdrawal.
- Partner property type guard.
- SQL seed, UI template, room image, Excel export.

---

## Câu Hỏi Bảo Vệ Gợi Ý

**Vì sao VNPAY success được tự xác nhận?**  
Vì hệ thống đã xác thực chữ ký VNPAY và mã phản hồi thành công. Admin chỉ can thiệp khi có ngoại lệ đối soát, hoàn tiền hoặc khiếu nại.

**Voucher Partner chịu khác gì Admin chịu?**  
Voucher Admin chịu là chi phí marketing của TravelMate nên không trừ payout Partner. Voucher Partner chịu sẽ được trừ vào settlement của Partner.

**Ví Partner có chuyển khoản thật không?**  
Không. Đây là ví quyết toán nội bộ. Admin chuyển khoản ngoài hệ thống rồi cập nhật trạng thái trong TravelMate.

**Cọc 30% xử lý no-show thế nào?**  
Nếu khách không đến, cọc 30% bị giữ và có thể đưa vào settlement sau khi trừ commission và voucher Partner chịu.
