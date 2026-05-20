# TravelMate — Đặc Tả Ca Sử Dụng Chi Tiết

**Phiên bản:** 1.0 | **Ngày:** 2026-05-20

---

## UC-06: Đặt Phòng & Thanh Toán VNPAY

### Thông tin chung
| Trường | Giá trị |
|---|---|
| ID | UC-06 |
| Tên | Đặt phòng và thanh toán qua VNPAY |
| Actor chính | User (đã đăng nhập) |
| Mức độ | Primary |
| Pre-condition | User đã đăng nhập; phòng đang APPROVED và còn phòng trống |
| Post-condition | Booking được tạo với trạng thái PENDING_ADMIN_APPROVAL; Payment PENDING_ADMIN_APPROVAL |

### Luồng chính (Basic Flow)
1. User vào trang chi tiết accommodation → chọn phòng → nhấn "Đặt phòng"
2. Hệ thống hiển thị form đặt phòng: check-in, check-out, số người, số phòng, hình thức thanh toán
3. User điền thông tin, chọn paymentOption (DEPOSIT_30 / FULL_PAYMENT), nhập mã voucher (tuỳ chọn)
4. User nhấn "Xác nhận đặt phòng"
5. Hệ thống validate: ngày hợp lệ, phòng đủ số lượng, voucher hợp lệ
6. Hệ thống tính tiền: totalAmount → discountAmount → paidAmount
7. Hệ thống tạo Booking (PENDING_PAYMENT) và Payment (PENDING_PAYMENT)
8. Hệ thống redirect đến VNPAY Sandbox
9. User hoàn tất thanh toán trên VNPAY
10. VNPAY redirect về `/payment/vnpay-return`
11. Hệ thống xác nhận giao dịch → Booking/Payment → PENDING_ADMIN_APPROVAL
12. Hiển thị trang kết quả thanh toán thành công

### Luồng thay thế
- **5a. Phòng hết:** Hệ thống thông báo "Phòng không đủ số lượng" → User chọn lại
- **5b. Voucher không hợp lệ:** Thông báo lỗi cụ thể → User nhập lại hoặc bỏ qua
- **9a. User hủy thanh toán VNPAY:** VNPAY trả về mã lỗi → Booking bị CANCELLED, phòng mở lại
- **9b. Thanh toán thất bại:** Booking CANCELLED, phòng mở lại

### Quy tắc nghiệp vụ
- DEPOSIT_30: paidAmount = floor(totalAmount × 30%)
- FULL_PAYMENT: paidAmount = totalAmount
- Sau discount, paidAmount phải >= 5.000đ (VNPAY minimum)
- Booking hết hạn trong 15 phút nếu chưa thanh toán

---

## UC-07: Admin Duyệt Booking

### Thông tin chung
| Trường | Giá trị |
|---|---|
| ID | UC-07 |
| Tên | Admin duyệt hoặc từ chối đơn đặt phòng |
| Actor chính | Admin |
| Pre-condition | Booking đang PENDING_ADMIN_APPROVAL |
| Post-condition | Booking CONFIRMED + partner_status = PENDING_PARTNER_CONFIRMATION (nếu approve) |

### Luồng chính
1. Admin vào `/admin/bookings` → xem danh sách PENDING
2. Admin nhấn "Duyệt" hoặc "Từ chối" trên một booking
3. **Nếu Duyệt:**
   - Booking → CONFIRMED
   - Payment → APPROVED
   - partner_status → PENDING_PARTNER_CONFIRMATION
   - Gửi notification cho partner
4. **Nếu Từ chối:**
   - Booking → CANCELLED
   - Payment → REJECTED
   - Mở lại phòng (availableQuantity += roomQuantity)
   - Ghi admin action log

### Quy tắc nghiệp vụ
- Chỉ booking PENDING_ADMIN_APPROVAL mới được duyệt/từ chối
- Admin action được ghi vào AdminActionLog

---

## UC-08: Partner Xác Nhận Booking

### Thông tin chung
| Trường | Giá trị |
|---|---|
| ID | UC-08 |
| Tên | Partner xác nhận giữ phòng cho khách |
| Actor chính | Partner |
| Pre-condition | Booking CONFIRMED, partner_status = PENDING_PARTNER_CONFIRMATION |
| Post-condition | partner_status = PARTNER_CONFIRMED |

### Luồng chính
1. Partner vào `/partner/bookings` → thấy booking chờ xác nhận
2. Partner nhấn "Xác nhận giữ phòng"
3. Hệ thống cập nhật: partner_status → PARTNER_CONFIRMED
4. Gửi notification cho User

### Luồng thay thế
- **2a. Partner từ chối:** partner_status → PARTNER_CANCELLED → Admin cần xử lý
- **Admin Override:** Admin có thể tự xác nhận thay Partner nếu cần

---

## UC-09: Nghiệp Vụ No-Show

### Thông tin chung
| Trường | Giá trị |
|---|---|
| ID | UC-09 |
| Tên | Đánh dấu khách không đến check-in (No-Show) |
| Actor chính | Admin |
| Pre-condition | Booking CONFIRMED, đã quá giờ check-in |

### Luồng — Trường hợp DEPOSIT_30
1. Admin nhấn "Đánh dấu No-Show" trên booking CONFIRMED
2. Hệ thống: BookingStatus → NO_SHOW
3. Hệ thống: PaymentStatus → DEPOSIT_FORFEITED (cọc 30% bị giữ)
4. Hệ thống: availableQuantity += roomQuantity (mở lại phòng)
5. Cọc 30% đưa vào quyết toán tháng cho Partner (commission vẫn tính)

### Luồng — Trường hợp FULL_PAYMENT
1. Admin nhấn "Đánh dấu No-Show" trên booking CONFIRMED
2. Hệ thống: BookingStatus → CHECKED_IN (xử lý theo chính sách đã trả đủ)
3. PaymentStatus giữ nguyên APPROVED
4. Phòng KHÔNG mở lại (booking vẫn tính là đã sử dụng)

---

## UC-10: Tạo Quyết Toán Tháng

### Thông tin chung
| Trường | Giá trị |
|---|---|
| ID | UC-10 |
| Tên | Admin tạo quyết toán doanh thu tháng cho Partner |
| Actor chính | Admin |
| Pre-condition | Có ít nhất 1 booking COMPLETED trong tháng trước |
| Post-condition | PartnerSettlement PENDING được tạo cho mỗi partner có doanh thu |

### Luồng chính
1. Admin vào `/admin/settlements` → nhấn "Tạo quyết toán tháng trước"
2. Hệ thống xác định kỳ: ngày 01 → cuối tháng trước
3. Hệ thống lọc payment đủ điều kiện:
   - ONLINE + APPROVED + booking COMPLETED
   - ONLINE + DEPOSIT_FORFEITED + booking NO_SHOW
4. Group by partner
5. Tính cho mỗi partner:
   - gross = tổng payment.amount
   - commission = gross × commissionRate (theo PropertyType + override)
   - voucherDeduct = tổng discountAmount khi costBearer = PARTNER
   - payout = max(0, gross - commission - voucherDeduct)
6. Tạo PartnerSettlement (PENDING) cho mỗi partner

### Commission Rate mặc định
| PropertyType | Commission |
|---|---|
| HOTEL | 15% |
| VILLA | 12% |
| HOMESTAY | 10% |
| RESORT | 18% |

---

## UC-11: Chatbot Tư Vấn Du Lịch

### Thông tin chung
| Trường | Giá trị |
|---|---|
| ID | UC-11 |
| Tên | Chatbot gợi ý du lịch và phòng theo ngân sách |
| Actor chính | Guest / User |
| Pre-condition | Không cần đăng nhập |

### Các Intent hỗ trợ
1. **Budget Intent:** "5 triệu đi đâu?", "3 triệu đi Đà Lạt 3 đêm"
   - Parse tiền → tìm phòng phù hợp ngân sách
2. **Destination Intent:** "Đà Lạt có gì đẹp?", "Nha Trang ăn gì?"
   - Tìm bài viết liên quan điểm đến
3. **Out-of-scope:** "Viết code Java cho tôi", "Dự báo thời tiết"
   - Từ chối lịch sự, chuyển hướng về tư vấn du lịch
