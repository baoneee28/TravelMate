# TravelMate - Checklist kiểm thử browser trước bảo vệ

**Ngày lập:** 2026-06-04  
**Mục tiêu:** giúp nhóm tự bấm lại các luồng quan trọng sau khi đã rà soát finance, voucher, rating/review, rebook, settlement và phân quyền.

## 0. Cách ghi kết quả

| Cột | Cách dùng |
|---|---|
| Test ID | Mã case bên dưới |
| Actor | Guest, User, Partner, Admin |
| Dữ liệu | Tài khoản/phòng/voucher/booking dùng để test |
| Expected | Kết quả mong muốn |
| Actual | Kết quả thực tế khi bấm |
| Status | PASS, FAIL, BLOCKED, NOT TESTED |
| Note | Ghi ảnh chụp, lỗi, URL hoặc dữ liệu cần kiểm tra lại |

Quy ước ưu tiên:
- **P0:** phải pass trước buổi bảo vệ vì ảnh hưởng trực tiếp demo hoặc nghiệp vụ chính.
- **P1:** nên pass để demo mượt và tránh bị hỏi sâu.
- **P2:** kiểm tra thêm nếu còn thời gian.

## 1. Tài khoản và dữ liệu chuẩn

| Role | Email | Password | Dùng để test |
|---|---|---|---|
| Admin | `admin@travelmate.vn` | `admin123` | Revenue, settlement, voucher, refund, duyệt dữ liệu |
| User chính | `user@travelmate.vn` | `user123` | Đặt phòng, thanh toán, rebook, review |
| User phụ | `user2@travelmate.vn` | `user123` | Ownership, review/booking người khác |
| Partner Hotel | `partner@travelmate.vn` | `partner123` | Booking, room, revenue, settlement |
| Partner Resort | `partner2@travelmate.vn` | `partner123` | Kiểm tra partner khác loại |
| Partner Villa | `partner3@travelmate.vn` | `partner123` | Villa/unit flow |
| Partner Homestay | `partner4@travelmate.vn` | `partner123` | Homestay flow |

Voucher nên kiểm tra:
- Partner voucher không vượt 10% cho cọc 30%: hợp lệ.
- Partner voucher vượt 10% cho cọc 30%: phải bị chặn.
- Admin voucher: không trừ vào payout Partner.
- Voucher hết hạn/sai scope/sai phòng: phải bị chặn.

## 2. Smoke test bắt buộc

| ID | Ưu tiên | Actor | Bước test | Expected |
|---|---|---|---|---|
| SMK-01 | P0 | Dev | Chạy `mvnw.cmd test` trong thư mục `travelmate` | `279/279` pass, không fail/error |
| SMK-02 | P0 | Dev | Import `travelmate_db.sql` vào MySQL sạch | Không lỗi SQL |
| SMK-03 | P0 | Guest | Mở trang chủ | Không trắng màn hình, CSS/ảnh load đủ |
| SMK-04 | P0 | User | Login user demo | Vào đúng khu vực User |
| SMK-05 | P0 | Partner | Login partner demo | Vào dashboard đối tác |
| SMK-06 | P0 | Admin | Login admin demo | Vào dashboard admin |
| SMK-07 | P1 | Mọi role | Reload Home, Detail, Admin, Partner | Không vỡ CSS/JS/image |
| SMK-08 | P1 | Mọi role | Zoom 100% và 125%, màn 1366px | Menu/bảng chính không vỡ layout |
| SMK-09 | P1 | Mọi role | Mở các màn booking/revenue | Không hiện enum kỹ thuật thô như `DEPOSIT_30` nếu màn đã có label |

## 3. Auth, role và ownership

| ID | Ưu tiên | Actor | Bước test | Expected |
|---|---|---|---|---|
| AUTH-01 | P0 | Guest | Vào `/booking` hoặc `/my-bookings` | Redirect login |
| AUTH-02 | P0 | User | Vào `/admin/dashboard` | Bị chặn hoặc vào trang access denied |
| AUTH-03 | P0 | User | Vào `/partner/dashboard` | Bị chặn |
| AUTH-04 | P0 | Partner | Vào `/admin/revenue` | Bị chặn |
| AUTH-05 | P0 | Partner | Vào `/my-bookings` | Bị chặn |
| AUTH-06 | P0 | Admin | Vào `/partner/bookings` | Bị chặn nếu admin không có role Partner |
| AUTH-07 | P1 | Mọi role | Logout rồi bấm Back browser | Không vào lại trang cần đăng nhập |
| OWN-01 | P0 | User B | Đổi URL booking của User A | Không xem/sửa được |
| OWN-02 | P0 | User B | Thử hủy/review booking của User A | Bị chặn |
| OWN-03 | P0 | Partner A | Đổi URL booking/room của Partner B | Bị chặn |
| OWN-04 | P0 | Partner A | Gắn voucher vào room của Partner B | Bị chặn |
| OWN-05 | P1 | Admin | Mở booking/revenue toàn hệ thống | Được phép |

## 4. Star rating và review

| ID | Ưu tiên | Actor | Bước test | Expected |
|---|---|---|---|---|
| STAR-01 | P0 | Partner | Mở form thêm/sửa nơi lưu trú | Không còn field nhập hạng sao tự khai |
| STAR-02 | P0 | Partner | Submit nơi lưu trú không có `starRating` | Tạo/lưu được |
| STAR-03 | P0 | Partner | Inject param `starRating=5` khi submit | Backend bỏ qua, UI không dùng sao tự khai |
| STAR-04 | P0 | User | Mở listing/detail nơi chưa có review | Hiện “Chưa có đánh giá”, không fallback 3 sao |
| STAR-05 | P1 | Admin | Mở danh sách/duyệt cơ sở lưu trú | Không còn yêu cầu duyệt hạng sao tự khai |
| REV-01 | P0 | User | Mở booking chưa completed | Không có nút đánh giá |
| REV-02 | P0 | User | Mở booking completed | Có nút đánh giá |
| REV-03 | P0 | User | Gửi review hợp lệ | Lưu thành công |
| REV-04 | P0 | User | Gửi review lần 2 cùng booking | Bị chặn |
| REV-05 | P0 | User B | Review booking của User A | Bị chặn |
| REV-06 | P1 | User | Gửi `<script>alert(1)</script>` trong comment | Script không chạy khi hiển thị |
| RATE-01 | P0 | User | Nơi có 2 review 10/10 và 2/10 | Rating public hiển thị trung bình 6.0/10 |
| RATE-02 | P1 | Admin | Ẩn review nếu có chức năng | Review ẩn không còn hiện public |

## 5. Voucher store và voucher trong booking

| ID | Ưu tiên | Actor | Bước test | Expected |
|---|---|---|---|---|
| STORE-01 | P1 | User | Mở Kho voucher | Danh sách voucher load đúng, card dễ đọc |
| STORE-02 | P1 | User | Xem voucher Partner <=10% | Ghi rõ áp dụng cho cọc 30% |
| STORE-03 | P1 | User | Xem voucher Partner >10% | Ghi rõ không áp dụng cho cọc 30% nếu policy chặn |
| STORE-04 | P1 | User | Xem voucher Admin | Ghi rõ ưu đãi TravelMate tài trợ |
| STORE-05 | P1 | Guest | Click đặt phòng từ voucher | Redirect login hoặc đi đúng luồng |
| VCH-01 | P0 | User | Apply Partner voucher 5% với cọc 30% | Pass |
| VCH-02 | P0 | User | Apply Partner voucher 10% với cọc 30% | Pass |
| VCH-03 | P0 | User | Apply Partner voucher 11% với cọc 30% | Bị chặn trước payment |
| VCH-04 | P0 | User | Apply Partner voucher 15% với cọc 30% | Bị chặn trước payment |
| VCH-05 | P0 | User | Apply fixed voucher đúng ngưỡng 10% | Pass |
| VCH-06 | P0 | User | Apply fixed voucher vượt 10% với cọc 30% | Bị chặn |
| VCH-07 | P0 | User | Apply Admin voucher với cọc 30% | Pass nếu đúng scope, không trừ payout Partner |
| VCH-08 | P0 | User | Apply Partner voucher 15% rồi đổi sang cọc 30% | Voucher bị remove/block |
| VCH-09 | P0 | User | Dùng voucher sai phòng/sai loại/hết hạn | Bị chặn |
| VCH-10 | P0 | User | Sửa discount bằng DevTools | Backend tự tính lại, không tin client |
| VCH-11 | P1 | User | Message lỗi voucher vượt ngưỡng | Dễ hiểu, không stack trace |

## 6. Payment và VNPAY

| ID | Ưu tiên | Actor | Bước test | Expected |
|---|---|---|---|---|
| FULL-01 | P0 | User | Chọn thanh toán 100% | VNPAY amount đúng theo tổng sau voucher hợp lệ |
| FULL-02 | P0 | User | VNPAY success | Payment `APPROVED`, Booking `CONFIRMED` |
| FULL-03 | P0 | User | Mở my bookings | Hiện “Đã thanh toán 100%” |
| FULL-04 | P0 | Admin | Mở revenue | Hoa hồng tính theo tổng đơn gốc trước voucher |
| FULL-05 | P1 | User | Email success | Có email xác nhận 100%, không lộ commission/payout nội bộ |
| FULL-B01 | P0 | User | VNPAY fail/cancel | Booking cancel, phòng mở lại |
| FULL-B02 | P0 | User | Refresh return URL sau success | Không double revenue/email |
| FULL-B03 | P1 | Dev | Tắt SMTP/sai SMTP | Booking vẫn success, chỉ log cảnh báo |
| DEP-01 | P0 | User | Chọn cọc 30% | VNPAY amount bằng phần cọc online |
| DEP-02 | P0 | User | VNPAY success | Payment `APPROVED`, Booking `CONFIRMED` |
| DEP-03 | P0 | User | My bookings sau cọc | Hiện “Đã cọc 30%”, còn lại tại cơ sở |
| DEP-04 | P0 | Admin | Revenue đơn cọc | Hoa hồng tính theo tổng đơn gốc, không tính trên cọc |
| DEP-05 | P0 | Admin/Partner | Revenue/settlement | 70% tại cơ sở hiển thị riêng, không vào ví TravelMate |
| DEP-B01 | P0 | User | Voucher Partner >10% với cọc | Bị chặn trước payment |
| DEP-B02 | P0 | User | Payment expired/fail/cancel | Booking cancel, phòng restore |
| PAY-IDEM-01 | P0 | User/Admin | Gọi/refresh callback success nhiều lần | Không double revenue/email |
| PAY-IDEM-02 | P0 | User/Admin | Callback fail sau success | Không downgrade payment approved |

## 7. Revenue, settlement, wallet và withdrawal

| ID | Ưu tiên | Actor | Bước test | Expected |
|---|---|---|---|---|
| REV-UI-01 | P0 | Admin | Mở `/admin/revenue` | Có wording “tổng đơn gốc trước voucher” |
| REV-UI-02 | P0 | Admin | Xem booking cọc | Có tổng đơn, cọc online, 70% tại cơ sở |
| REV-UI-03 | P0 | Admin | Xem commission base | Là tổng đơn gốc, không phải cọc/tiền sau voucher |
| REV-UI-04 | P0 | Admin | Xem Partner voucher | Hiển thị riêng phần Partner chịu |
| REV-UI-05 | P0 | Admin | Xem Admin voucher | Không trừ payout Partner |
| REV-UI-06 | P0 | Admin | Xem direct/manual block | Không vào revenue online |
| REV-UI-07 | P1 | Admin | Export Excel nếu có | Số khớp UI |
| SET-01 | P0 | Admin | Generate settlement tháng | Chỉ booking eligible/completed hoặc giữ cọc hợp lệ |
| SET-02 | P0 | Admin | Mở settlement detail | Payout khớp Admin revenue |
| SET-03 | P0 | Admin | Tạo settlement trùng kỳ | Không tạo trùng |
| SET-04 | P0 | Admin | Mark settlement PAID 1 lần | Wallet credit đúng |
| SET-05 | P0 | Admin | Mark settlement PAID lần 2 | Không credit trùng |
| WAL-01 | P0 | Partner | Mở `/partner/wallet` | Balance, pending withdrawal và history rõ |
| WAL-02 | P0 | Partner | Request withdrawal hợp lệ | Hold/deduct đúng logic ví |
| WAL-03 | P0 | Admin | Mark withdrawal PAID/REJECTED | Không xử lý trùng, số dư nhất quán |
| WAL-04 | P1 | Partner | Thiếu bank info | Có cảnh báo hoặc chặn rút tiền |

## 8. Partner operations và no-show

| ID | Ưu tiên | Actor | Bước test | Expected |
|---|---|---|---|---|
| PRT-01 | P0 | Partner | Mở booking mới sau payment success | Thấy trạng thái đã giữ phòng/căn |
| PRT-02 | P0 | Partner | Check-in khách đúng ngày | Booking chuyển sang đang lưu trú |
| PRT-03 | P0 | Partner | Check-in trước ngày nhận phòng | Bị chặn |
| PRT-04 | P0 | Partner | Check-in booking cọc chưa xác nhận 70% | Bị chặn/cảnh báo |
| PRT-05 | P0 | Partner | Check-in booking cọc có xác nhận 70% | Hiện đã thu tại cơ sở |
| PRT-06 | P0 | Partner | Check-out trước check-in | Bị chặn |
| PRT-07 | P0 | Partner | Check-out sau check-in | Booking `COMPLETED`, User review được |
| NS-01 | P0 | Partner | Mark no-show đơn cọc | Booking `NO_SHOW`, cọc bị giữ |
| NS-02 | P0 | Partner/User | No-show đơn cọc | 70% không phát sinh, phòng mở lại |
| NS-03 | P1 | User | My bookings no-show | Có thông tin cọc bị giữ và nút đặt lại nếu phù hợp |

## 9. Rebook và đặt lại

| ID | Ưu tiên | Actor | Bước test | Expected |
|---|---|---|---|---|
| RB-01 | P0 | User | Bấm “Đặt lại phòng này” từ booking cancelled | Sang form booking hợp lệ |
| RB-02 | P0 | User | Rebook booking mất cọc/no-show | Có note đơn mới độc lập, cọc cũ không chuyển |
| RB-03 | P0 | User | Rebook ngày cũ đã qua | Bắt chọn ngày mới hoặc prefill ngày mới rõ |
| RB-04 | P0 | User | Rebook phòng bị ngừng bán/ẩn | Không cho đặt lại |
| RB-05 | P0 | User | Tạo booking từ rebook | Booking code và payment mới |
| RB-06 | P0 | User | Rebook không reuse voucher cũ | Voucher cũ không tự apply nếu hết hạn/sai điều kiện |
| RB-07 | P0 | User B | Rebook booking của User A | Bị chặn |
| RB-08 | P1 | User | UX message | Dễ hiểu: đơn mới không ảnh hưởng đơn cũ |

## 10. Direct booking, manual block và availability

| ID | Ưu tiên | Actor | Bước test | Expected |
|---|---|---|---|---|
| DM-01 | P0 | Partner | Tạo direct booking | Source `DIRECT`, không cần payment online |
| DM-02 | P0 | Partner/Admin | Kiểm revenue/settlement direct booking | Không vào revenue online/settlement |
| DM-03 | P0 | Partner | Tạo manual block | Source `MANUAL_BLOCK`, chiếm phòng theo ngày |
| DM-04 | P0 | Partner/Admin | Kiểm revenue/settlement manual block | Không vào revenue online/settlement |
| DM-05 | P0 | Partner | Hủy direct/manual block | Availability mở lại |
| DM-06 | P0 | Partner A | Block room Partner B | Bị chặn |
| AVL-01 | P0 | User | Đặt sát ngày checkout booking cũ | Được phép nếu checkout = check-in mới |
| AVL-02 | P0 | User | Đặt trùng/đè ngày booking active | Bị tính overlap |
| AVL-03 | P0 | User | Pending expired | Không giữ phòng sau expire |
| AVL-04 | P0 | User | Cancelled/no-show/completed | Không giữ phòng tương lai |
| AVL-05 | P1 | User | Đặt vượt quota phòng cùng ngày | Bị chặn |

## 11. Cancel, refund và wording

| ID | Ưu tiên | Actor | Bước test | Expected |
|---|---|---|---|---|
| CAN-01 | P0 | User | Hủy `PENDING_PAYMENT` | Cancel, paidAmount 0, không refund |
| CAN-02 | P0 | User | Hủy đơn cọc sau thanh toán | `DEPOSIT_FORFEITED`, wording mất cọc |
| CAN-03 | P0 | User | Hủy full payment trước check-in | `REFUND_PENDING`, refund theo policy |
| CAN-04 | P0 | Admin | Approve refund | Refund amount/fee backend tự tính |
| CAN-05 | P0 | User/Admin | Spam cancel/refund | Không tạo request trùng |
| CAN-06 | P1 | User | Hủy cọc | Không hiện wording “quá hạn thanh toán” |
| WORD-01 | P0 | User | My bookings cọc/full/no-show/cancelled | Label dễ hiểu, không enum thô |
| WORD-02 | P0 | Partner | Check-in cọc đã thu 70% | Hiện gọn “Đã thu tại cơ sở” |
| WORD-03 | P1 | Admin | Booking detail/revenue | Không còn wording cũ “hoa hồng chỉ tính trên cọc” |
| WORD-04 | P1 | Voucher card | Xem điều kiện áp dụng | Nói rõ cọc 30%/full payment/scope |

## 12. Production-risk nhẹ cho đồ án

| ID | Ưu tiên | Actor | Bước test | Expected |
|---|---|---|---|---|
| RISK-01 | P0 | User | Double click nút thanh toán | Không tạo 2 payment/booking |
| RISK-02 | P0 | User | Hai user đặt cùng phòng cùng ngày | Không vượt quota |
| RISK-03 | P1 | Partner/Admin | Upload ảnh sai type/quá lớn | Bị chặn hoặc cảnh báo rõ |
| RISK-04 | P1 | User | XSS review/support | Không chạy script |
| RISK-05 | P1 | Guest/User | SQL injection trong search | Không lỗi server |
| RISK-06 | P1 | Guest | API voucher không login nếu cần auth | Bị chặn hoặc chỉ trả dữ liệu public an toàn |
| RISK-07 | P2 | Dev | CSRF đang tắt | Ghi rõ là hạn chế demo, production cần bật lại |

## 13. Checklist P0 rút gọn trước giờ bảo vệ

- [ ] `mvnw.cmd test` pass `279/279`.
- [ ] Import `travelmate_db.sql` vào MySQL sạch thành công.
- [ ] Login User, Partner, Admin đúng role.
- [ ] Partner không còn nhập hạng sao.
- [ ] Listing/detail chưa review hiển thị “Chưa có đánh giá”.
- [ ] Cọc 30% không voucher thanh toán được.
- [ ] Cọc 30% với voucher Partner 10% dùng được.
- [ ] Cọc 30% với voucher Partner 11%/15% bị chặn.
- [ ] Full payment vẫn dùng voucher hợp lệ nếu payout không âm.
- [ ] VNPAY success chuyển payment approved, booking confirmed.
- [ ] VNPAY fail/cancel hủy booking và mở lại phòng.
- [ ] Refresh callback không double revenue/email.
- [ ] Admin revenue: hoa hồng tính theo tổng đơn gốc trước voucher.
- [ ] Settlement khớp Admin revenue.
- [ ] Payout âm bị chặn, không im lặng đưa về 0.
- [ ] Partner thấy booking đã giữ, check-in, check-out chạy đúng.
- [ ] Đơn cọc check-in phải xác nhận thu 70% tại cơ sở.
- [ ] User review sau completed.
- [ ] Rebook tạo booking/payment mới độc lập.
- [ ] Direct booking/manual block không vào revenue/settlement.
- [ ] User/Partner không xem được dữ liệu của người khác.

## 14. Kịch bản demo nên tập

### Demo A - Cọc 30% + voucher Partner 10%
1. User login.
2. Vào kho voucher hoặc nhập mã voucher Partner 10%.
3. Chọn phòng có tổng đơn dễ đối soát.
4. Chọn cọc 30%.
5. Apply voucher, thanh toán VNPAY.
6. My bookings hiển thị đã cọc và đã giữ phòng.
7. Admin revenue hiển thị tổng đơn gốc, cọc online, 70% tại cơ sở, commission và voucher Partner chịu.

### Demo B - Voucher Partner vượt 10% bị chặn với cọc
1. User chọn voucher Partner 15%.
2. Chọn cọc 30%.
3. Hệ thống báo voucher vượt giới hạn cọc.
4. Đổi sang thanh toán 100% nếu voucher đủ điều kiện.

### Demo C - Partner vận hành
1. Partner login.
2. Check-in, với cọc thì xác nhận đã thu 70% tại cơ sở.
3. Check-out.
4. User review booking completed.

### Demo D - Rebook
1. User mở booking cancelled/no-show.
2. Bấm “Đặt lại phòng này”.
3. Kiểm note đơn mới độc lập.
4. Chọn ngày mới và tạo booking/payment mới.
