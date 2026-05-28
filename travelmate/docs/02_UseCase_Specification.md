# TravelMate — Đặc Tả Ca Sử Dụng Chi Tiết

**Phiên bản:** 1.4  
**Ngày cập nhật:** 2026-05-25

---

## UC-06: Đặt Phòng Và Thanh Toán VNPAY

| Trường | Giá trị |
|---|---|
| Actor chính | User |
| Pre-condition | User đã đăng nhập; phòng/căn đang mở bán và còn số lượng |
| Post-condition thành công | Booking `CONFIRMED`, Payment `APPROVED`, Partner status `PENDING_PARTNER_CONFIRMATION` |

### Luồng chính
1. User chọn cơ sở lưu trú và phòng/căn.
2. User nhập check-in, check-out, số khách, số lượng, hình thức thanh toán và voucher nếu có.
3. Hệ thống validate ngày, số lượng phòng/căn, voucher và giới hạn tối thiểu VNPAY.
4. Hệ thống tạo Booking `PENDING_PAYMENT`, Payment `PENDING_PAYMENT` và giữ phòng tạm.
5. User được chuyển sang VNPAY Sandbox.
6. User thanh toán thành công.
7. VNPAY redirect về `/payment/vnpay-return`.
8. Hệ thống xác thực chữ ký và mã phản hồi.
9. Nếu hợp lệ, hệ thống tự cập nhật:
   - Payment `APPROVED`
   - Booking `CONFIRMED`
   - Partner status `PENDING_PARTNER_CONFIRMATION`
10. User thấy kết quả "Đã thanh toán / Chờ đối tác xác nhận giữ phòng".
11. Đối tác nhận booking mới để xác nhận giữ phòng.

### Luồng thay thế
- Phòng/căn không đủ: hiển thị lỗi và yêu cầu chọn lại.
- Voucher không hợp lệ: hiển thị lý do cụ thể.
- VNPAY thất bại/hủy/hết hạn: booking bị hủy hoặc hết hạn, phòng/căn được mở lại.
- Chữ ký hoặc dữ liệu VNPAY bất thường: booking chuyển luồng đối soát ngoại lệ, Admin kiểm tra thủ công.

### Quy tắc nghiệp vụ
- `DEPOSIT_30`: thu 30% online, 70% còn lại trả tại cơ sở.
- `FULL_PAYMENT`: thu 100% online.
- Admin không duyệt thủ công giao dịch VNPAY thành công thông thường.

---

## UC-07: Admin Đối Soát Giao Dịch Ngoại Lệ

| Trường | Giá trị |
|---|---|
| Actor chính | Admin |
| Pre-condition | Booking/Payment đang `PENDING_ADMIN_APPROVAL` do cần kiểm tra thủ công |
| Post-condition | Booking được xác nhận, từ chối hoặc chuyển hoàn tiền tùy kết quả đối soát |

### Luồng chính
1. Admin vào `/admin/bookings` và lọc "Cần đối soát".
2. Admin kiểm tra thông tin booking, payment, mã giao dịch và ghi chú hệ thống.
3. Nếu giao dịch hợp lệ:
   - Payment `APPROVED`
   - Booking `CONFIRMED`
   - Partner status `PENDING_PARTNER_CONFIRMATION`
   - Gửi thông báo cho đối tác.
4. Nếu giao dịch không hợp lệ:
   - Booking `CANCELLED`
   - Payment `REJECTED` hoặc trạng thái phù hợp
   - Phòng/căn được mở lại.

### Ghi chú
Ca sử dụng này chỉ dành cho ngoại lệ, không thay thế luồng VNPAY auto-confirm.

---

## UC-08: Partner Xác Nhận Giữ Phòng

| Trường | Giá trị |
|---|---|
| Actor chính | Partner |
| Pre-condition | Booking `CONFIRMED`, Partner status `PENDING_PARTNER_CONFIRMATION` |
| Post-condition | Partner status `PARTNER_CONFIRMED` hoặc `PARTNER_CANCELLED` |

### Luồng chính
1. Partner vào `/partner/bookings`.
2. Hệ thống hiển thị booking đã thanh toán và đang chờ xác nhận giữ phòng.
3. Partner bấm "Xác nhận giữ phòng".
4. Hệ thống cập nhật `PARTNER_CONFIRMED` và thông báo cho User.

### Luồng thay thế
- Đối tác từ chối giữ phòng: `PARTNER_CANCELLED`, Admin xử lý hủy/hoàn tiền/trao đổi với khách.

---

## UC-09: Check-In, Thu 70% Và Check-Out

| Trường | Giá trị |
|---|---|
| Actor chính | Partner |
| Pre-condition | Booking đã được Partner xác nhận |
| Post-condition | Booking `CHECKED_IN` hoặc `COMPLETED` |

### Luồng chính
1. Partner check-in khách khi khách đến.
2. Booking chuyển `CHECKED_IN`.
3. Nếu booking `DEPOSIT_30`, Partner xác nhận đã thu 70% còn lại tại cơ sở.
4. Khi khách trả phòng, Partner bấm check-out.
5. Booking chuyển `COMPLETED`.

---

## UC-10: No-Show

| Trường | Giá trị |
|---|---|
| Actor chính | Partner/Admin |
| Pre-condition | Booking đã xác nhận nhưng khách không đến |

### DEPOSIT_30
1. Hệ thống/đối tác đánh dấu khách không đến.
2. Booking `NO_SHOW`.
3. Payment `DEPOSIT_FORFEITED`.
4. Cọc 30% được giữ lại và có thể đưa vào settlement.
5. Phòng/căn được mở lại theo chính sách.

### FULL_PAYMENT
Booking đã thanh toán 100% không xử lý như cọc mất; Admin/đối tác xử lý theo chính sách lưu trú, hoàn tiền hoặc khiếu nại.

---

## UC-11: Voucher

| Trường | Giá trị |
|---|---|
| Actor chính | Admin, Partner, User |

### Luồng Admin
1. Admin tạo voucher và chọn phạm vi áp dụng.
2. Admin chọn `costBearer = ADMIN` hoặc `PARTNER`.
3. Voucher được đưa vào kho toàn hệ thống hoặc kho dành cho Partner/Room.

### Luồng Partner
1. Partner vào `/partner/vouchers`.
2. Partner xem voucher được Admin cấp.
3. Partner gắn voucher vào phòng/căn thuộc quyền quản lý nếu được phép.
4. Nếu cần chương trình riêng, Partner gửi yêu cầu hỗ trợ/khuyến mãi.

### Luồng User
1. User nhập voucher khi đặt phòng.
2. Hệ thống validate và tính giảm giá.
3. Booking lưu thông tin voucher để quyết toán.

---

## UC-12: Admin Tạo Quyết Toán Tháng

| Trường | Giá trị |
|---|---|
| Actor chính | Admin |
| Pre-condition | Có booking đủ điều kiện trong kỳ tháng |
| Post-condition | PartnerSettlement `PENDING` được tạo |

### Điều kiện booking được tính
- `ONLINE + APPROVED + COMPLETED`
- `ONLINE + DEPOSIT_FORFEITED + NO_SHOW`
- Không tính `DIRECT`, `MANUAL_BLOCK`, booking chưa hoàn tất, booking hủy hoặc đang check-in.

### Công thức
```text
payout = onlineAmount - commission - partnerVoucherDeduction
```

Trong đó:
- `onlineAmount` là số tiền TravelMate đã thu online.
- `commission` tính theo loại cơ sở/phòng hoặc override.
- `partnerVoucherDeduction` chỉ trừ khi `costBearer = PARTNER`.

---

## UC-13: Ví Và Rút Tiền Partner

| Trường | Giá trị |
|---|---|
| Actor chính | Partner, Admin |

### Luồng chính
1. Admin xác nhận settlement đã chuyển khoản.
2. Hệ thống cộng payout vào ví nội bộ Partner và ghi transaction.
3. Partner cập nhật tài khoản ngân hàng nếu thiếu.
4. Partner gửi yêu cầu rút tiền nhỏ hơn hoặc bằng số dư khả dụng.
5. Hệ thống phong tỏa tiền rút: giảm available, tăng pending withdrawal.
6. Admin chuyển khoản ngoài hệ thống.
7. Admin cập nhật yêu cầu rút tiền thành `PAID` hoặc `REJECTED`.
8. Nếu từ chối, tiền được hoàn lại về số dư khả dụng.
