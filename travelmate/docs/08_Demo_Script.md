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

## Kịch Bản Ưu Tiên Khi Demo Trước Giảng Viên

### Mở đầu 1 phút: định vị project trước khi demo

| Câu hỏi dễ bị hỏi | Câu trả lời nên nói |
|---|---|
| TravelMate là gì? | TravelMate là website đặt phòng/lưu trú dạng OTA-mini cho đồ án: User đặt phòng, Partner vận hành cơ sở/phòng, Admin quản trị và đối soát. |
| Đây là REST API hay web render server? | Đây là Spring Boot MVC + Thymeleaf server-rendered web; backend vẫn chia Controller, Service, Repository, Entity rõ ràng. |
| Project demo hay production? | Project ở mức demo/local/sandbox. Có guard nghiệp vụ chính nhưng production cần thêm CSRF, migration, secret manager, monitoring, load test và tích hợp thanh toán/hoàn tiền thật. |
| Điểm khác web giới thiệu khách sạn? | TravelMate có login, phân quyền, booking, payment, availability, voucher, partner workflow, settlement/wallet, withdrawal và test tự động; không chỉ hiển thị thông tin tĩnh. |
| Chức năng quan trọng nhất? | Luồng booking-payment-partner-confirm-settlement vì nó nối User, Partner, Admin, DB, trạng thái nghiệp vụ và VNPAY Sandbox. |

### Demo 1: Flow ngắn, an toàn, ít lỗi

| Mục | Nội dung |
|---|---|
| Mục tiêu | Chứng minh hệ thống có dữ liệu thật, UI chạy ổn, search/chi tiết lưu trú hoạt động, phân quyền cơ bản rõ ràng và không phụ thuộc VNPAY. |
| Account | Guest + `user@travelmate.vn / user123`. |
| Steps | Trang chủ -> tìm Đà Lạt -> mở chi tiết LATA/Tulip/TM Grand -> đăng nhập user -> mở `my-bookings` -> mở chatbot hỏi trong phạm vi. |
| Câu nói | Đây là luồng xem và tra cứu thông tin; booking/payment là flow tiếp theo, nhóm em tách riêng để demo rõ từng phần. |
| Rủi ro | Thiếu data/ảnh/alias; chatbot API hoặc mạng yếu. |
| Cứu demo | Dùng dữ liệu seed có sẵn; nếu Groq lỗi thì nói chatbot có fallback intent-based trong phạm vi TravelMate. |

### Demo 2: Flow nghiệp vụ mạnh nhất

| Mục | Nội dung |
|---|---|
| Mục tiêu | User đặt phòng -> VNPAY -> TravelMate tự giữ phòng/căn -> Partner check-in/check-out -> User review; sau đó giải thích settlement. |
| Account | `user@travelmate.vn`, `partner@travelmate.vn`, `admin@travelmate.vn`. |
| Steps | User login -> chọn room còn trống -> chọn `FULL_PAYMENT` hoặc `DEPOSIT_30` -> VNPAY success -> my-bookings -> Partner thấy đơn đã giữ -> check-in -> check-out -> User review. |
| Câu nói | Sau VNPAY, hệ thống tự ghi nhận payment `APPROVED`, booking `CONFIRMED` và partner status `PARTNER_CONFIRMED`; Partner không cần bấm giữ phòng thủ công, chỉ check-in khi khách đến. |
| Rủi ro | VNPAY env/mạng/gateway lỗi; room vừa hết chỗ hoặc ngày chọn overlap seed. |
| Cứu demo | Chuẩn bị booking seed đã thanh toán; nếu VNPAY fail, mở my-bookings/partner bookings sẵn để demo trạng thái. |

### Demo 3: Flow quản trị, doanh thu và đối soát

| Mục | Nội dung |
|---|---|
| Mục tiêu | Chứng minh không chỉ CRUD: có revenue, settlement, wallet, withdrawal và export Excel. |
| Account | `admin@travelmate.vn`; Partner có ví mẫu trong SQL hiện tại. |
| Steps | Admin revenue -> settlements -> tạo tháng trước nếu chưa có -> mark paid -> Partner wallet -> request withdrawal -> Admin paid/reject -> export Excel. |
| Câu nói | Ví đối tác trong TravelMate là sổ quyết toán nội bộ; Admin chuyển khoản ngoài hệ thống rồi cập nhật trạng thái. |
| Rủi ro | Máy demo chưa import đúng SQL nên thiếu ví/settlement mẫu; settlement tháng trước đã tạo rồi nên bấm tạo lại không sinh mới. |
| Cứu demo | Nếu thiếu data, mở settlement/revenue và giải thích flow dựa trên test report/source; không cố tạo số liệu tài chính mới tại chỗ. |

**Thứ tự khuyến nghị:** Nếu mạng không chắc, đi Demo 1 trước. Nếu VNPAY Sandbox đã cấu hình ổn, đi Demo 1 -> Demo 2 -> Demo 3.

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
   - Booking đã được TravelMate giữ phòng/căn.

**Điểm nhấn khi bảo vệ:** Không còn flow cũ phải đợi Admin sau khi VNPAY thành công. Admin chỉ xử lý đối soát ngoại lệ, hoàn tiền hoặc khiếu nại.

---

## 4. User Xem Lịch Sử Đặt Phòng

1. Vào `/my-bookings`.
2. Lọc tab "Đã thanh toán".
3. Chỉ rõ các nhãn:
   - `Đã thanh toán`
   - `Đã giữ phòng/căn`
   - `Đang lưu trú`
   - `Hoàn thành`
4. Với booking cọc 30%, chỉ rõ dòng `Còn 70% thanh toán tại cơ sở`.
5. Mở chi tiết/hóa đơn để thấy trạng thái thanh toán và trạng thái đối tác.

---

## 5. Partner Check-In, Check-Out

1. Đăng nhập `partner@travelmate.vn / partner123`.
2. Vào `/partner/bookings`.
3. Lọc hoặc quan sát booking đang `Đã giữ phòng/căn`.
4. Với booking đã giữ:
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

### 7.1 Availability/overbooking nên nói thế nào

1. Availability tính theo khoảng ngày, không phải trừ tồn kho vĩnh viễn.
2. Quy tắc overlap: booking cũ giữ phòng nếu `oldCheckIn < newCheckOut` và `oldCheckOut > newCheckIn`.
3. Source hiện coi `PENDING_PAYMENT`, `PENDING_ADMIN_APPROVAL`, `CONFIRMED`, `CHECKED_IN` là trạng thái giữ phòng.
4. `CANCELLED`, `NO_SHOW`, `COMPLETED` không còn giữ availability sau khi đã xử lý; đây là điểm cần nói đúng khi bị hỏi xoắn.
5. Direct booking và manual block vẫn ảnh hưởng availability nhưng không có payment online, không commission, không settlement.

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

### 8.4 Câu nói bắt buộc khi demo tiền
- Settlement là kỳ đối soát nội bộ theo tháng, không phải lệnh chuyển khoản ngân hàng tự động.
- `PAID` nghĩa là Admin đã ghi nhận xử lý chi trả ngoài hệ thống; ví Partner chỉ được credit một lần.
- Partner wallet là sổ nội bộ, không phải ví điện tử hoặc tài khoản ngân hàng thật.
- Withdrawal `PENDING` là tiền đang được giữ khỏi số dư có thể rút; reject sẽ trả lại balance.
- Refund trong bản demo là `REFUND_PENDING -> REFUNDED`, chưa tích hợp API refund production.

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
Tests run: 279
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

### 10.1 Cách nói về test count
- Số hiện tại của source này là `279/279` test pass, không dùng lại số cũ `247/247`, `255/255`, `257/257`, `259/259`, `267/267`, `269/269`, `272/272`, `274/274` hoặc `278/278`.
- Chỉ nói "vừa chạy pass" nếu đã chạy trên máy demo hoặc có ảnh/terminal `BUILD SUCCESS`.
- Test tự động không thay thế test thủ công: VNPAY Sandbox, UI, upload, Excel, OAuth/SMTP và browser session vẫn cần kiểm tra trước buổi bảo vệ.

### 10.2 Checklist môi trường local
- App chạy mặc định ở `http://localhost:8080`.
- DB cần import `travelmate/src/main/resources/travelmate_db.sql`.
- VNPAY dùng Sandbox; cần `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, return URL và IPN URL phù hợp.
- IPN server-to-server cần public URL/tunnel nếu muốn gateway gọi vào máy local; không nói localhost tự nhận IPN thật từ internet.
- SMTP/Google OAuth/chatbot AI chỉ demo khi đã cấu hình và test chắc; nếu không, dùng form login và flow fallback.
- Không trình chiếu secret thật trong `application.properties`, `.env` hoặc terminal.

### 10.3 Smoke test trong phiên rà soát 2026-06-02
- Full Maven suite: `279/279` pass, `0` fail, `0` error, `0` skipped.
- App local start được ở `http://127.0.0.1:18080`; DataInitializer hoàn tất.
- HTTP/session smoke test pass `23/23` checks: public pages, login User/Partner/Admin, User my-bookings/booking form/payment-result, Partner dashboard/bookings/wallet/settlements/support, Admin dashboard/bookings/settlements/withdrawals/reviews/support và cross-role guard.
- Session 2026-06-03 đã chạy được Chrome headless cho luồng User đặt lại đơn đã hủy/no-show trên desktop và mobile: login thành công, bấm/tap nút đặt lại, redirect sang form booking có prefill và banner đơn mới độc lập. Trước buổi bảo vệ vẫn nên mở browser thật trên máy demo để chạy lại các flow lớn như VNPAY, Partner và Admin.

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

**Full payment no-show có demo giống cọc 30% không?**
Không. Full payment no-show là chính sách hoàn tiền/giữ phí phức tạp hơn. Trong bản hiện tại, hệ thống không tự chuyển full payment no-show thành check-in; Admin xử lý theo chính sách riêng.

**Completed/no-show có còn giữ phòng không?**
Không còn giữ availability sau khi đã xử lý. Booking `COMPLETED` là lịch sử lưu trú đã checkout, còn `NO_SHOW` là khách không đến và quota được mở lại; các trạng thái này không nằm trong nhóm active giữ phòng của `AvailabilityService`.

**Partner có tự tạo voucher không?**
Không. Admin tạo voucher; Partner chỉ gắn voucher kho `PARTNER_ROOM` được cấp vào phòng/căn thuộc quyền quản lý và đã được duyệt.

**Admin có thay Partner check-in/check-out không?**
Không phải flow chính. Partner vận hành lưu trú thực tế; Admin chỉ có nút override cho tình huống ngoại lệ hoặc khi Partner không thao tác được.

**User thường gõ thẳng `/admin` thì sao?**
Spring Security chặn bằng rule `/admin/**` chỉ role `ADMIN`. Không chỉ dựa vào việc ẩn menu trên giao diện.

**Quên mật khẩu có gửi lại mật khẩu cũ không?**
Không. Mật khẩu đã hash bằng BCrypt; forgot password chỉ tạo token/link đặt lại mật khẩu mới.

**CSRF đang tắt có nên nói là an toàn production không?**
Không. Bản demo tắt CSRF để test/form local thuận tiện; production phải bật CSRF token cho các request thay đổi dữ liệu.

**User sửa hidden price bằng DevTools thì sao?**
Backend lấy giá phòng từ database và tự tính lại tổng tiền trong `BookingService`, nên không tin giá từ client.

**Callback VNPAY hoặc bấm settlement paid bị trùng thì sao?**
Payment callback chỉ xử lý khi payment còn `PENDING_PAYMENT`; settlement paid/credit wallet có transaction guard và unique key để tránh cộng ví trùng.

**VNPAY success nhưng DB lỗi thì sao?**
Đây là tình huống production cần đối soát. Bản demo có transaction, verify, raw payload và trạng thái payment/booking; nếu triển khai thật cần retry job, log callback, màn Admin reconciliation và API kiểm tra lại giao dịch từ gateway.

**Nếu không chạy lại test trên máy bảo vệ thì có nên đọc số không?**
Không nên nói như kết quả vừa chạy nếu chưa chạy lại trên máy trình bày. Cách an toàn là: "Theo test report hiện tại của source là 279/279 pass; trước demo nhóm sẽ chạy lại trên máy trình bày để xác nhận."

**Unit test có gọi VNPAY thật không?**
Không. Unit/integration test giả lập callback hoặc kiểm service/controller; VNPAY Sandbox thật phụ thuộc mạng và secret nên kiểm bằng manual demo.

**`ddl-auto=update` có nên dùng production không?**
Không. Nó tiện cho dev/demo, nhưng production nên dùng Flyway/Liquibase hoặc schema migration có kiểm soát.

**Nếu MySQL lỗi khi demo thì xử lý sao?**
Kiểm MySQL service, database name, user/password, import lại SQL backup và dùng ảnh/video dự phòng. Không sửa code tại chỗ nếu lỗi nằm ở môi trường.

**Project đã production-ready chưa?**
Chưa. TravelMate đạt mức đồ án/demo với các guard nghiệp vụ chính; production cần HTTPS, domain, backup, monitoring, CSRF, rate limit, secret manager, migration, load test và đối soát payment thật.

**Refund thật đã có chưa?**
Chưa. Hệ thống chỉ ghi nhận `REFUND_PENDING -> REFUNDED`; hoàn tiền thật cần API refund/cổng thanh toán production và quy trình đối soát.

**Dữ liệu mẫu có đủ cho kinh doanh thật không?**
Không. Dữ liệu mẫu đủ demo luồng nghiệp vụ chính, nhưng production cần dữ liệu thật, ảnh/chính sách thật, load test và quy trình vận hành/kiểm duyệt đầy đủ hơn.

---

## Hard Defense Mở Rộng

### Nếu bị hỏi mở rộng production
- Không nói TravelMate ngang Booking/Agoda/Airbnb.
- Câu an toàn: TravelMate là OTA-mini/demo; production cần HTTPS, cloud/server, backup, monitoring, secret manager, CSRF, rate limit, migration DB, load test, payment/refund thật, fraud/dispute và dữ liệu thật.
- Nếu hỏi microservices: monolith MVC phù hợp đồ án; chỉ tách Payment, Notification/Email, Chatbot/AI, Search/Recommendation hoặc Settlement khi hệ thống/team đủ lớn.
- Nếu hỏi mobile: cần REST API/JSON, auth token/JWT hoặc cơ chế mobile phù hợp, CORS/API security, API versioning và upload/push notification qua API.

### Nếu bị hỏi tính năng thực tế
- MoMo: reuse booking/payment/status/tính tiền, nhưng cần provider riêng để tạo URL/QR, verify signature và xử lý callback. Hiện `MOMO_DEMO` chỉ là enum dự phòng, chưa phải MoMo production.
- Bản đồ: hiện chưa có tọa độ trong `Accommodation`; muốn làm thật cần latitude/longitude, form chọn vị trí và map provider.
- Recommendation AI: hiện chatbot là rule-based + Groq optional; AI gợi ý thật cần dữ liệu search/click/booking/review/wishlist lớn hơn.
- Đa ngôn ngữ: source có `messages_vi/en` và đổi `lang` cơ bản; dịch toàn bộ UI + dữ liệu DB cần key/i18n và bảng/cột bản dịch riêng.

### Nếu thầy yêu cầu mở code ngay
- Booking: mở `BookingService.createBooking`, nói validate -> lock room -> availability -> voucher -> tính tiền từ DB -> tạo booking/payment `PENDING_PAYMENT`.
- Payment: mở `PaymentService` và `VnpayService`, nói verify signature, validate amount, idempotency, callback success/fail.
- Security: mở `SecurityConfig`, nói `/admin/**`, `/partner/**`, BCrypt, OAuth optional và CSRF đang tắt cho demo.
- Settlement/wallet: mở `SettlementService` và `PartnerWalletService`, nói payout, `PAID` credit một lần, withdrawal hold/reject/paid.

### Câu trả lời nhanh hard defense

**Nếu AI hỗ trợ code thì em hiểu gì?**
Nhóm có dùng công cụ hỗ trợ, nhưng đã đọc lại source, chạy test, chuẩn bị demo và có thể mở code giải thích các luồng booking, payment, security, partner và settlement.

**Nếu hỏi sao chưa bật CSRF?**
Demo local tắt để test form/payment thuận tiện hơn; production phải bật CSRF token cho các request thay đổi dữ liệu.

**Nếu hỏi IPN localhost có chạy không?**
Return URL có thể quay về localhost qua browser; IPN là server-to-server nên cần tunnel/public HTTPS nếu muốn gateway gọi vào máy local.

**Nếu hỏi overbooking production thì sao?**
Source có overlap check và lock ghi ở mức demo. Production cần concurrency/load test, transaction isolation, retry strategy và monitoring.

**Nếu hỏi wallet có phải ví thật không?**
Không. Đây là sổ quyết toán nội bộ; hệ thống chưa tích hợp ngân hàng hoặc ví điện tử thật.
