# TravelMate - Partner Resort Demo Checklist

**Ngày cập nhật:** 2026-06-02
**Phạm vi:** Partner Resort, kiểm soát cọc 30%, dữ liệu demo và các màn hình giảng viên có thể quan sát

## 1. Quy ước nghiệp vụ Resort

| Nội dung | Cách thể hiện trong hệ thống |
|---|---|
| Tài khoản demo | `partner2@travelmate.vn` / `partner123` - Blue Ocean Resort |
| Cơ sở quản lý | Vinpearl Resort & Spa Nha Trang, Furama Resort Đà Nẵng |
| Đơn vị bán | **Loại phòng / suite**, không dùng nghiệp vụ "thuê nguyên căn Homestay" |
| Hoa hồng mặc định | **18%** cho booking online Resort |
| Booking trực tiếp / chặn phòng | Chỉ làm giảm lịch trống, không tính doanh thu hoặc quyết toán TravelMate |
| Đơn đặt cọc 30% | Khi check-in, Partner phải xác nhận đã thu 70% còn lại tại Resort |

## 2. Dữ liệu để trình bày trực quan

| Mã booking | Màn hình cần mở | Điều giảng viên nhìn thấy |
|---|---|---|
| `BK-VNT-DLX-0001` | Đơn đặt phòng | Đơn online cọc 30%, TravelMate đã tự giữ phòng/căn; Partner dùng để demo check-in khi khách đến |
| `BK-FDN-FAM-RESORT-CHECKIN-01` | Đơn đặt phòng | Đơn cọc đã giữ phòng/căn; khi bấm check-in phải xác nhận thu **7.280.000đ** tại Resort |
| `BK-VNT-DLX-RESORT-DEP-COMPLETE-01` | Đơn đặt phòng / Doanh thu | Đơn cọc đã hoàn thành và đã ghi nhận thu phần còn lại tại cơ sở |
| `BK-FDN-DLX-0001` | Đơn đặt phòng / Doanh thu | Khách không đến, tiền cọc được xử lý theo chính sách và hoa hồng Resort |
| `BK-VNT-DLX-DIRECT-01` | Lịch trống | Booking trực tiếp chiếm quota nhưng không vào doanh thu TravelMate |
| `BK-FDN-BCH-BLOCK-03` | Lịch trống | Partner chặn phòng vận hành, không phát sinh doanh thu |

## 3. Kịch bản bấm màn hình Partner Resort

1. Đăng nhập tài khoản Resort và mở `/partner/dashboard`: kiểm tra loại đối tác Resort, booking, doanh thu và quota phòng.
2. Mở `/partner/accommodations`: chỉ thấy các Resort thuộc Blue Ocean Resort; nhãn quản lý hiển thị theo phòng / suite.
3. Mở `/partner/amenities`: chọn tiện nghi theo phòng hoặc suite, không thay đổi hạng sao của cơ sở.
4. Mở `/partner/bookings`: dùng bộ lọc **Cọc 30%** và mở `BK-FDN-FAM-RESORT-CHECKIN-01`.
5. Bấm **Check-in khách** cho đơn cọc: hộp thoại phải yêu cầu xác nhận đã thu 70% còn lại; không xác nhận thì hệ thống không check-in.
6. Mở `/partner/availability`: thấy giải thích booking trực tiếp và chặn phòng chỉ ảnh hưởng lịch trống.
7. Mở `/partner/direct-booking`: biểu mẫu dùng chữ **loại phòng / suite** và ghi rõ tiền thu tại Resort không vào quyết toán TravelMate.
8. Mở `/partner/revenue`, `/partner/settlements`, `/partner/wallet`: đối chiếu doanh thu online, hoa hồng 18% và ví nội bộ.
9. Mở `/partner/vouchers`: voucher theo phòng / suite chỉ áp dụng cho tài sản thuộc Partner Resort hiện tại.

## 4. Điểm kiểm tra an toàn đã bổ sung

| Kiểm tra | Kết quả mong đợi |
|---|---|
| Gọi check-in thường cho đơn cọc chưa thu 70% | Bị chặn tại service, không chỉ chặn bằng giao diện |
| Check-in kèm xác nhận thu 70% | Ghi nhận `PAID_AT_PROPERTY` và cho phép check-in |
| Khoản 70% đã được ghi nhận trước | Check-in được tiếp tục, không yêu cầu thu hoặc ghi đè lại |
| Partner Resort truy cập cơ sở khác loại hoặc khác chủ | Bị chặn bởi guard owner và loại cơ sở |

## 5. Kiểm thử kỹ thuật

Chạy từ thư mục `travelmate`:

```powershell
.\mvnw.cmd test
```

**Kết quả xác minh lịch sử ngày 2026-05-25:** `128/128` test pass, `0` fail, `0` error, `0` skipped. File SQL đã được rà mã booking seed: `91` mã, không trùng mã `VALUES`.

**Kết quả suite toàn dự án chạy lại ngày 2026-06-05:** `279/279` test pass, `0` fail, `0` error, `0` skipped. Đây là con số hiện tại cần dùng trong báo cáo và buổi demo.

Dữ liệu demo Resort đã được đưa vào `src/main/resources/travelmate_db.sql` để có thể import lại trên máy trình bày.
