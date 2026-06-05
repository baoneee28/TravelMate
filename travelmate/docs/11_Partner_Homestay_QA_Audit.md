# Partner Homestay - Kịch bản demo và kết quả kiểm tra

## Tài khoản trình diễn

| Vai trò | Tài khoản | Mật khẩu | Mục đích |
| --- | --- | --- | --- |
| Partner Homestay | `partner4@travelmate.vn` | `partner123` | Dashboard, bookings, lịch trống, doanh thu, quyết toán, ví, voucher |
| User | `user2@travelmate.vn` | `user123` | Đặt Homestay và xem hành trình |
| Admin | `admin@travelmate.vn` | `admin123` | Đối soát ngoại lệ, xử lý quyết toán, hoàn tiền và rút tiền |

## Quy tắc đã chốt

| Nghiệp vụ | Quy tắc trình bày |
| --- | --- |
| Mô hình Homestay | Demo theo từng phòng. Không bán đồng thời phòng riêng và nguyên căn khi chưa có logic chặn toàn bộ phòng cùng cơ sở. |
| Đơn online sau VNPAY | VNPAY thành công thì TravelMate tự xác nhận thanh toán và tự giữ phòng/căn; Partner vào thẳng bước check-in khi khách đến. |
| Cọc 30% | Khi check-in, đối tác bắt buộc xác nhận đã thu 70% còn lại tại cơ sở. TravelMate chỉ ghi nhận doanh thu online đã thu. |
| Booking trực tiếp/chặn phòng | Có ảnh hưởng lịch trống, không tính doanh thu online và không vào quyết toán. |
| Commission Homestay | Mặc định `10%` trên khoản tiền TravelMate thực thu online. |
| Voucher đối tác chịu | Voucher do Admin phát hành và đối tác gắn vào phòng/căn; nếu `costBearer = PARTNER` thì trừ khỏi khoản đối tác nhận. |
| Ví quyết toán | Là ví nội bộ TravelMate; yêu cầu rút tiền chờ Admin xử lý trước khi ghi nhận đã chuyển khoản. |

## Dữ liệu nhìn thấy ngay sau khi import SQL

File gốc để mang sang máy khác: `src/main/resources/travelmate_db.sql`.

| Chức năng demo | Mã dữ liệu | Điều cần trình bày |
| --- | --- | --- |
| Ngoại lệ đối soát | `BK-HLR-STD-HS-PENDING-01` | Admin thấy đơn cần đối soát thủ công; đối tác chưa thao tác cho đến khi TravelMate xác nhận xong. |
| Check-in phòng/căn đã giữ | `BK-MND-ATT-0002` | Booking full payment đã được VNPAY xác nhận, TravelMate đã tự giữ phòng/căn; Partner dùng để demo check-in. |
| Check-in cọc 30% | `BK-MND-ATT-HS-CHECKIN-01` | Nhấn check-in phải xác nhận đã thu `812.000đ` còn lại tại Homestay. |
| No-show cọc 30% | `BK-HLR-STD-HS-NOSHOW-01` | Báo no-show giữ cọc online `192.000đ` và mở lại quota. |
| Check-out thanh toán đủ | `BK-MND-STD-DEMO1` | Đơn đang lưu trú để trình diễn trả phòng/hoàn thành. |
| Booking trực tiếp | `BK-MND-STD-DIRECT-01` | Hiện nguồn Trực tiếp, giữ phòng nhưng không cộng revenue/settlement. |
| Chặn phòng | `BK-MND-FAM-HS-BLOCK-01` | Hiện nguồn Chặn phòng; hủy chặn sẽ mở lại quota, không có doanh thu. |
| Doanh thu cọc đã hoàn tất | `BK-HLR-DLX-HS-DEP-COMPLETE-01` | TravelMate thu online `288.000đ`; commission 10% là `28.800đ`; trước voucher đối tác nhận `259.200đ`. |
| Voucher đối tác chịu | `MOCNHIEN50K` trên `BK-MND-FAM-0001` | Voucher phòng Mộc Nhiên giảm `50.000đ`, được trừ phía đối tác. |
| Ví/rút tiền | `WD-HS-PENDING-001` | Yêu cầu rút `500.000đ` đang chờ Admin xử lý. |

## Các sửa lỗi bắt buộc đã thực hiện

| Vấn đề | Cách xử lý |
| --- | --- |
| Partner đoán URL để xem đơn chưa được chuyển sang mình | Mọi truy cập chi tiết Partner đi qua cùng điều kiện hiển thị như danh sách: online chỉ lộ khi booking đã `CONFIRMED` và thuộc đúng loại Homestay. |
| Doanh thu cộng nhầm payment của booking trực tiếp cũ | Revenue chỉ nhận payment thuộc booking `ONLINE`; Direct/Manual Block bị loại ở backend. |
| Check-in đơn cọc không thể hiện nghĩa vụ thu 70% | Backend buộc xác nhận thu phần còn lại; modal hiển thị rõ đây là bước bắt buộc. |
| Gợi ý bán Homestay nguyên căn dễ gây overbooking | Giao diện ghi rõ phạm vi demo theo phòng, không khuyến khích bán trộn nguyên căn và phòng riêng. |
| Voucher mẫu gắn sai phòng | Đổi thành `MOCNHIEN50K`, gắn đúng phòng `MND-FAM` đang sử dụng voucher. |

## Kết quả kiểm tra tự động

Ngày kiểm tra: `02/06/2026`.

| Nhóm kiểm tra | Kết quả |
| --- | --- |
| Toàn bộ unit/integration test hiện có | `279/279` PASS, 0 fail, 0 error, 0 skipped |
| Homestay ẩn booking chưa được chuyển sang Partner và chặn mở URL trực tiếp | PASS |
| Check-in cọc/đủ tiền và no-show Homestay | PASS |
| Revenue Homestay 10%, trừ voucher Partner, loại Direct | PASS |
| Ownership: availability và voucher đúng loại `HOMESTAY` | PASS |
| Kiểm tra lỗi khoảng trắng của các file sửa | PASS |
| Seed mới: mã booking/withdrawal mới không bị lặp | PASS |

Lệnh test đã chạy:

```powershell
.\mvnw.cmd '-Dmaven.resources.skip=true' test
```

Tham số bỏ qua sao chép resource chỉ dùng vì bản `target/classes/travelmate_db.sql` đang bị ứng dụng Java mở giữ trên máy kiểm tra. File SQL nguồn đã được cập nhật đầy đủ; không ảnh hưởng logic của test tự động.

## Checklist bấm demo trên trình duyệt

| Bước | Thao tác | Kết quả cần nhìn thấy |
| --- | --- | --- |
| 1 | Partner mở `/partner/bookings` khi `BK-HLR-STD-HS-PENDING-01` còn là ngoại lệ đối soát | Không thấy đơn như đơn xử lý chính thức. |
| 2 | Admin xác nhận đối soát ngoại lệ, Partner tải lại trang | Đơn xuất hiện với trạng thái đã giữ phòng/căn và có thao tác check-in khi tới ngày nhận. |
| 3 | Partner check-in `BK-MND-ATT-HS-CHECKIN-01` | Modal buộc tick xác nhận thu `812.000đ`; sau đó hiện đang lưu trú. |
| 4 | Partner báo no-show `BK-HLR-STD-HS-NOSHOW-01` | Đơn thành no-show, cọc được giữ, availability tăng lại. |
| 5 | Partner xem revenue/settlement | Direct và chặn phòng không cộng tiền; cọc hoàn tất chỉ tính khoản online. |
| 6 | Partner xem wallet | Có yêu cầu rút `WD-HS-PENDING-001` đang chờ xử lý. |
| 7 | Kiểm tra zoom `100%`, `80%`, màn hình trình chiếu | Sidebar, bảng và nút thao tác không chồng nhau. |

Các bước thao tác giao diện và responsive cần bấm trực tiếp trong trình duyệt để chụp ảnh minh chứng; kiểm thử backend không thể thay thế phần quan sát này.
