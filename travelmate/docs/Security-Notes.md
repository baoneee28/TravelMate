# TravelMate — Ghi Chú Bảo Mật (Demo)

## 1. CSRF

### Trạng thái hiện tại
CSRF **đang tắt** trong `SecurityConfig.java`:
```java
.csrf(csrf -> csrf.disable())
```

### Lý do tắt trong demo
- Đơn giản hóa test form Thymeleaf khi chạy local
- Tránh vỡ luồng POST (login, booking, admin approve, partner confirm)
- Phù hợp với phạm vi đồ án cơ sở chạy demo local

### Hướng triển khai production
Khi triển khai production, bật CSRF và thêm `th:action` vào mọi form:
```html
<!-- Thymeleaf tự inject _csrf token khi dùng th:action -->
<form th:action="@{/booking/create}" method="post">
    ...
</form>
```
Không cần thêm hidden field thủ công — Thymeleaf + Spring Security tự xử lý.

### Câu trả lời bảo vệ (nếu giảng viên hỏi)
> *"Trong phạm vi demo local của đồ án cơ sở, CSRF đang tắt để đơn giản hóa  
> kiểm thử và trình bày. Trong môi trường production, nhóm sẽ bật lại CSRF.  
> Spring Security + Thymeleaf tự động inject `_csrf` token vào mọi form POST  
> qua thuộc tính `th:action`, nên không cần thêm code thủ công."*

---

## 2. Phân Quyền Route (SecurityConfig)

### Kết luận phân quyền hiện tại

| Route pattern | Quyền | Ghi chú |
|---|---|---|
| `/admin/**` | `ROLE_ADMIN` | Toàn bộ trang quản trị |
| `/partner/**` | `ROLE_PARTNER` | Toàn bộ trang đối tác |
| `/booking/**`, `/my-bookings/**` | `ROLE_USER` | Đặt phòng & lịch sử |
| `/profile/**` | `ROLE_USER` | Hồ sơ cá nhân |
| `/payment/vnpay/create/**` | `ROLE_USER` | Tạo link VNPAY |
| `/payment/vnpay-return` | Public | VNPAY redirect về sau thanh toán |
| `/payment/vnpay-ipn` | Public | VNPAY server callback (không có session) |
| `GET /api/travel-posts/**` | Public | Xem bài viết du lịch |
| `POST/PUT/DELETE /api/travel-posts/**` | `ROLE_ADMIN` | Quản lý bài viết |
| `/api/chatbot/**` | Public | Chatbot không yêu cầu đăng nhập |
| `/`, `/accommodations/**` | Public | Trang chủ và tìm kiếm |
| `/travel`, `/news`, `/contact/**`, `/vouchers` | Public | Các trang nội dung |
| `/auth/**` | Public | Đăng nhập / đăng ký |
| `/assets/**`, `/uploads/**` | Public | Static resources |
| Tất cả còn lại | `authenticated()` | Phải đăng nhập |

### Các endpoint cần test manual khi bảo vệ
1. Truy cập `/admin/` khi không đăng nhập → redirect về `/auth/login`
2. Đăng nhập USER rồi truy cập `/admin/` → redirect về `/auth/login` (403 → login)
3. Đăng nhập PARTNER rồi truy cập `/admin/` → bị chặn
4. Đăng nhập USER rồi truy cập `/partner/` → bị chặn
5. `GET /api/travel-posts/` không đăng nhập → trả về dữ liệu bình thường
6. `POST /api/travel-posts/` không đăng nhập → redirect login (403)
7. `/payment/vnpay-return` không có session → nhận được response (không bị chặn)

---

## 3. Secrets trong application.properties

### Tình trạng hiện tại
```properties
# Sandbox local — lấy từ biến môi trường, không hardcode secret trong source
vnpay.tmn-code=${VNPAY_TMN_CODE:}
vnpay.hash-secret=${VNPAY_HASH_SECRET:}
spring.datasource.password=root
```

### Chính sách
- Đây là **Sandbox + local dev** — không phải production
- Không commit secret thật lên GitHub public
- Production: load từ biến môi trường hoặc secret manager

### Câu trả lời bảo vệ
> *"Cấu hình VNPAY trong `application.properties` chỉ giữ key dạng placeholder.  
> Khi chạy demo local, nhóm set biến môi trường theo `docs/demo-env.example`;  
> khi triển khai thật, secret sẽ đi qua secret manager hoặc cấu hình môi trường."*

---

## 4. BCrypt Password

Tất cả mật khẩu được mã hóa BCrypt (strength 10):
```
admin123  → $2a$10$asNcVD6SMW64TtbgEncgKO...
user123   → $2a$10$FsFOdcKQPqCnlIPA3j82Ze...
partner123 → $2a$10$dCikCIiksr/Ne1Xpv40vKO...
```

BCrypt tự động thêm **salt ngẫu nhiên** → cùng password, mỗi lần hash ra kết quả khác nhau.  
Không bao giờ lưu plain text trong DB.

---

## 5. Session & Authentication Flow

```
User → POST /auth/login
     ↓
Spring Security → UserDetailsServiceImpl.loadUserByUsername(email)
     ↓
BCryptPasswordEncoder.matches(input, stored)
     ↓
CustomAuthSuccessHandler.onAuthenticationSuccess()
     ↓
Redirect theo role: ADMIN→/admin, PARTNER→/partner, USER→/
```

Session được lưu server-side (HttpSession). Logout:
- Xóa session (`invalidateHttpSession(true)`)
- Xóa authentication (`clearAuthentication(true)`)
- Redirect về `/auth/login?logout`
