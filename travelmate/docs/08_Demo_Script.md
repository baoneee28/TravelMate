# TravelMate — Demo Script (Kịch Bản Trình Bày)

**Thời gian:** ~15–20 phút  
**URL:** http://localhost:8080  
**Chuẩn bị:** MySQL import `travelmate_db.sql`, chạy `mvn spring-boot:run`

---

## Chuẩn Bị Trước Demo

1. Chạy ứng dụng:
   ```bash
   cd travelmate && mvn spring-boot:run
   ```
2. Mở **Chrome** (không dùng incognito)
3. Tab 1: http://localhost:8080 (User)
4. Tab 2: http://localhost:8080 (Admin — sẽ login riêng)
5. Chuẩn bị thẻ test VNPAY: số thẻ `9704198526191432198`, tên `NGUYEN VAN A`, ngày `07/15`, OTP `123456`
6. Tài khoản seed riêng để demo ví/quyết toán:
   - `seedpartner_wallet@travelmate.vn` / `partner123`: có bank info, ví, withdrawal PENDING/PAID/REJECTED
   - `seedpartner_nobank@travelmate.vn` / `partner123`: có tiền nhưng thiếu bank info để test chặn rút

---

## Phần 1: Trang Chủ & Tìm Kiếm (2 phút)

**Mục tiêu:** Demo UI, tìm kiếm, lọc

1. Vào `http://localhost:8080` → trang chủ
2. **Điểm nhấn:** Banner, danh sách cơ sở lưu trú nổi bật
3. Tìm kiếm: gõ "Đà Lạt" → xem kết quả (LATA Hotel, Tulip, TM Grand, Mộc Nhiên)
4. Lọc theo loại: chọn "Hotel" → xem thu hẹp kết quả
5. Click vào **LATA Hotel** → trang chi tiết
6. **Điểm nhấn:** Ảnh, mô tả, danh sách phòng với giá và số lượng còn

---

## Phần 2: Chatbot Tư Vấn (2 phút)

**Mục tiêu:** Demo AI chatbot gợi ý theo ngân sách

1. Click icon chatbot (góc dưới phải)
2. Gõ: `"Tôi có 3 triệu đi đâu được?"`
   - Chatbot gợi ý phòng phù hợp ngân sách
3. Gõ: `"5 triệu đi Đà Lạt 3 ngày"`
   - Chatbot tính toán gợi ý cụ thể
4. Gõ: `"Viết code Java cho tôi"`
   - Chatbot từ chối lịch sự, chuyển về tư vấn du lịch
5. **Điểm nhấn:** Intent budget, out-of-scope handling, floating UI

---

## Phần 3: Đặt Phòng & Thanh Toán VNPAY (5 phút)

**Mục tiêu:** Demo full booking flow

### 3.1 Đăng nhập User
1. Vào `/auth/login`
2. Đăng nhập: `user@travelmate.vn` / `user123`

### 3.2 Tìm phòng và đặt
1. Vào `/accommodations` → tìm "Đà Lạt" → chọn **Tulip Hotel 2 Dalat**
2. Click vào phòng **Standard Twin** (480.000đ/đêm)
3. Nhấn **"Đặt phòng"**

### 3.3 Form đặt phòng
1. Chọn check-in: **ngày mai**
2. Chọn check-out: **ngày kia** (2 đêm)
3. Số người lớn: **2**
4. Hình thức: **Cọc 30%** (290.000đ thay vì 960.000đ)
5. Nhập voucher: `SUMMER10` → giảm 10%
6. **Điểm nhấn:** Hiển thị tổng tiền trước/sau giảm, số tiền cọc 30%
7. Nhấn **"Xác nhận đặt phòng"**

### 3.4 Thanh toán VNPAY
1. Trang redirect VNPAY Sandbox
2. Chọn **ATM/Domestic card**
3. Nhập: Số thẻ `9704198526191432198`, Tên `NGUYEN VAN A`, Ngày `07/15`
4. Nhập OTP: `123456`
5. **Điểm nhấn:** Giao diện VNPAY Sandbox thực tế
6. Thanh toán thành công → redirect về TravelMate
7. Hiển thị trang kết quả: **"Thanh toán thành công — Chờ admin duyệt"**

---

## Phần 4: Admin Dashboard (4 phút)

**Mục tiêu:** Demo toàn bộ quyền admin

### 4.1 Đăng nhập Admin (Tab mới)
1. Mở tab mới → `http://localhost:8080/auth/login`
2. Đăng nhập: `admin@travelmate.vn` / `admin123`
3. Tự động redirect về `/admin`

### 4.2 Duyệt booking
1. Vào `/admin/bookings` → thấy booking vừa tạo ở trên
2. Cũng thấy: **BK-LATA-STD-0001** (PENDING), **BK-TLP-SUP-0001** (PENDING)
3. Click **"Duyệt"** cho booking vừa tạo → CONFIRMED
4. **Điểm nhấn:** Booking chuyển sang CONFIRMED, partner nhận notification
5. Click **"Từ chối"** cho BK-TLP-SUP-0001 → CANCELLED

### 4.3 Xem booking có sẵn
1. Filter **NO_SHOW** → thấy BK-TLP-STD-0001 (DEPOSIT_FORFEITED — cọc bị giữ)
2. Filter **COMPLETED** → thấy BK-LATA-FAM-0001, BK-MND-ATT-0001
3. **Điểm nhấn:** Đa dạng trạng thái booking

### 4.4 Duyệt cơ sở lưu trú
1. Vào `/admin/accommodations`
2. Thấy accommodation **[PENDING DEMO]** đang chờ duyệt
3. Nhấn **"Duyệt"** → APPROVED

### 4.5 Quyết toán tháng
1. Vào `/admin/settlements`
2. Thấy settlement PAID của 4 partner
3. Nhấn **"Tạo quyết toán tháng trước"**
4. Xem cột **Ngày chi trả dự kiến** mùng 10
5. Mở chi tiết một settlement → bấm **"Xuất Excel"** để tải file đối soát
6. Với settlement PENDING, bấm **"Xác nhận + cộng ví"**
7. **Điểm nhấn:** Hệ thống tự tính gross, commission 15/12/10/18%, voucher deduction, payout, xuất Excel chi tiết và cộng vào ví Partner
8. **Seed trực quan:** tìm partner `seedpartner_wallet@travelmate.vn`, settlement mới kỳ tháng trước có:
   - Gross: 2.250.000đ
   - Commission: 337.500đ
   - Voucher partner chịu: 200.000đ
   - Payout: 1.712.500đ

### 4.6 Duyệt yêu cầu rút tiền
1. Vào `/admin/withdrawals`
2. Thấy yêu cầu rút tiền PENDING/PAID/REJECTED
3. Bấm **"Xuất Excel"** để tải danh sách withdrawal phục vụ đối soát nội bộ
4. Với yêu cầu PENDING, nhập mã giao dịch demo → bấm **"Đã chuyển"**
5. Hoặc bấm **"Từ chối"** để hoàn tiền về ví Partner
6. **Điểm nhấn:** TravelMate không chuyển khoản tự động; Admin chuyển ngoài hệ thống rồi cập nhật trạng thái; file Excel mask số tài khoản khi xuất

### 4.7 Báo cáo doanh thu
1. Vào `/admin/revenue`
2. **Điểm nhấn:** Admin xem tổng quan doanh thu, commission, payment online và quyết toán theo tháng

---

## Phần 5: Partner Dashboard (3 phút)

**Mục tiêu:** Demo luồng Partner

### 5.1 Đăng nhập Partner (Tab mới)
1. Mở tab mới → đăng nhập: `partner@travelmate.vn` / `partner123`
2. Redirect về `/partner`

### 5.2 Xác nhận booking
1. Vào `/partner/bookings`
2. Thấy booking Admin vừa duyệt (PENDING_PARTNER_CONFIRMATION)
3. Nhấn **"Xác nhận giữ phòng"**
4. **Điểm nhấn:** partner_status → PARTNER_CONFIRMED

### 5.3 Check-in & Check-out
1. Xem booking **BK-TMG-PRE-0001** (đang CHECKED_IN)
2. Nhấn **"Hoàn tất / Check-out"** → COMPLETED

### 5.4 Ví quyết toán
1. Vào `/partner/wallet`
2. Xem KPI: số dư có thể rút, đang chờ rút, tổng đã nhận, tổng đã rút
3. Cập nhật tài khoản nhận tiền
4. Gửi yêu cầu rút tiền nhỏ hơn số dư
5. **Điểm nhấn:** Lịch sử giao dịch thể hiện tiền vào từ settlement và tiền ra khi rút
6. Đăng nhập thêm `seedpartner_wallet@travelmate.vn` để thấy sẵn:
   - Số dư có thể rút: 2.200.000đ
   - Đang chờ rút: 800.000đ
   - Tổng đã nhận: 4.000.000đ
   - Tổng đã rút: 1.000.000đ
   - Lịch sử có `SETTLEMENT_CREDIT`, `WITHDRAWAL_REQUEST`, `WITHDRAWAL_PAID`, `WITHDRAWAL_REJECTED`
7. Đăng nhập `seedpartner_nobank@travelmate.vn`, thử rút tiền để thấy hệ thống bắt cập nhật tài khoản ngân hàng trước.

### 5.5 Doanh thu
1. Vào `/partner/revenue`
2. **Điểm nhấn:** Partner xem doanh thu vận hành, loại phòng, commission breakdown và số tiền chờ quyết toán

---

## Phần 6: Tính Năng Bổ Sung (2 phút)

### 6.1 Voucher Management
1. Admin vào `/admin/vouchers` → thấy 5 voucher (SUMMER10, WELCOME50, TRAVEL15...)
2. **Điểm nhấn:** Voucher USER_GLOBAL (ADMIN) vs PARTNER_ACCOMMODATION/ROOM

### 6.2 Travel Posts & Destinations
1. Vào `/travel` → bài viết gợi ý du lịch
2. **Điểm nhấn:** API `/api/travel-posts/` public, tích hợp chatbot

### 6.3 Lịch sử đặt phòng (User)
1. User vào `/my-bookings`
2. **Điểm nhấn:** Nhiều trạng thái, nút Đánh giá cho booking COMPLETED

---

## Câu Hỏi & Trả Lời Dự Kiến

**Q: Tại sao dùng Cọc 30% thay vì hoàn tiền?**  
A: Mô phỏng thực tế nghành khách sạn: cọc 30% giữ chỗ, 70% trả tại cơ sở. No-show → mất cọc để bù chi phí cho partner.

**Q: CSRF đang tắt — có an toàn không?**  
A: Trong demo local, tắt để đơn giản test form. Production cần bật và thêm `th:action` Thymeleaf inject token tự động.

**Q: Commission tính như thế nào?**  
A: Mặc định: HOTEL 15%, VILLA 12%, HOMESTAY 10%, RESORT 18%. Partner có thể override cho từng phòng (SUITE 20%, VIP 18%, FAMILY 12% tùy cơ sở).

**Q: Settlement tính gì cho no-show?**
A: DEPOSIT_30 no-show → chỉ phần cọc 30% đã thu online được đưa vào settlement; Partner nhận phần còn lại sau khi trừ commission và voucher do Partner chịu nếu có. FULL_PAYMENT no-show → không settlement theo luồng no-show này, xử lý theo chính sách check-in/hoàn tất riêng.

**Q: Ví Partner có chuyển khoản ngân hàng thật không?**
A: Không. Đây là ví quyết toán nội bộ để ghi nhận số dư, tiền vào/ra và yêu cầu rút tiền. Admin chuyển khoản ngoài hệ thống theo thông tin ngân hàng Partner cung cấp, sau đó cập nhật trạng thái trong TravelMate.

**Q: Xuất Excel dùng để làm gì?**
A: Admin dùng file Excel để đối soát nội bộ: settlement có sheet tổng quan + chi tiết booking; withdrawal có danh sách yêu cầu rút tiền và số tài khoản được mask.

**Q: Overbooking được xử lý thế nào?**
A: `PESSIMISTIC_WRITE` lock khi tạo booking. Query `SUM(room_quantity)` cho khoảng ngày chồng lấp. Nếu tổng + request > availableQty → từ chối.
