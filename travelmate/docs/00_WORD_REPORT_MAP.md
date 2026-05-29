# TravelMate - Mục Lục Viết Báo Cáo Word

File này là bản đồ để bạn chuyển sang viết Word nhanh, không phải tài liệu nộp trực tiếp. Khi viết báo cáo, ưu tiên copy và biên tập từ các file bên dưới.

---

## 1. Bộ tài liệu chính

| Mục cần viết | File nên dùng |
|---|---|
| Tổng quan đề tài, công nghệ, nghiệp vụ, câu trả lời bảo vệ | `docs/18_Project_Analysis_For_Report_And_Defense.md` |
| Đặc tả yêu cầu phần mềm | `docs/01_SRS.md` |
| Đặc tả use case | `docs/02_UseCase_Specification.md` |
| Sơ đồ Use Case | `docs/03_UseCase_Diagram.puml` |
| Sơ đồ Class | `docs/04_Class_Diagram.puml` |
| Sơ đồ Sequence đặt phòng/thanh toán | `docs/05_Sequence_Booking_Payment.puml` |
| Sơ đồ Activity cọc 30% và thanh toán 100% | `docs/06_Activity_Deposit30_FullPayment.puml` |
| Sơ đồ trạng thái Booking | `docs/16_State_Booking_Diagram.puml` |
| Sơ đồ triển khai local | `docs/17_Deployment_Diagram.puml` |
| Báo cáo kiểm thử chính thức | `docs/07_Test_Report.md` |
| Kịch bản demo | `docs/08_Demo_Script.md` |
| Checklist sẵn sàng demo/Word | `docs/15_Demo_Word_Readiness_Checklist.md` |
| Cấu hình biến môi trường demo | `docs/demo-env.example` |

---

## 2. Cấu trúc báo cáo Word đề xuất

### Chương 1 - Tổng quan đề tài

- Lý do chọn đề tài TravelMate.
- Mục tiêu xây dựng hệ thống đặt phòng lưu trú trực tuyến.
- Đối tượng sử dụng: Guest, User, Partner, Admin.
- Phạm vi chức năng: tìm kiếm lưu trú, đặt phòng, thanh toán VNPAY, voucher, review, chatbot, quản lý đối tác, ví và quyết toán.

### Chương 2 - Cơ sở lý thuyết và công nghệ

- Kiến trúc Spring Boot MVC kết hợp Layered Architecture.
- Spring Security, BCrypt, phân quyền role.
- Spring Data JPA/Hibernate và MySQL.
- Thymeleaf, HTML/CSS/JavaScript.
- VNPAY Sandbox, Spring Mail SMTP, Google OAuth2, Apache POI.

### Chương 3 - Phân tích và thiết kế hệ thống

- Actor và use case.
- Đặc tả use case chính.
- Thiết kế entity/class.
- Luồng đặt phòng/thanh toán.
- Vòng đời booking.
- Mô hình triển khai demo local.

### Chương 4 - Cài đặt chức năng

- Khách hàng: đăng ký, đăng nhập, Google Login, quên mật khẩu qua email, tìm phòng, đặt phòng, thanh toán, đánh giá.
- Đối tác: quản lý cơ sở/phòng, xác nhận booking, check-in/check-out, xem ví, yêu cầu rút tiền.
- Admin: duyệt dữ liệu, quản lý voucher/user/review, xử lý booking, tạo quyết toán, xuất Excel.
- Bảo mật: reset token 30 phút dùng một lần, không lộ email tồn tại, Google OAuth chỉ cho USER.

### Chương 5 - Kiểm thử và đánh giá

- Ghi đúng số hiện tại: `247` test cases.
- Kết quả: `247 PASS, 0 FAIL, 0 ERROR, 0 SKIPPED`.
- Nhóm test chính: booking, payment, voucher, settlement, wallet, review, Google OAuth, forgot password, template UI.
- Nêu hạn chế trung thực: CSRF đang tắt cho demo local, secret cần đưa ra biến môi trường khi production.

---

## 3. Số liệu chốt để ghi

| Hạng mục | Giá trị |
|---|---|
| Tên hệ thống | TravelMate |
| Backend | Java 21, Spring Boot 3.5 |
| Frontend | Thymeleaf, HTML, CSS, JavaScript |
| Database | MySQL |
| Bảo mật | Spring Security, BCrypt, Role-based authorization |
| Google Login | OAuth2, chỉ tạo/đăng nhập tài khoản USER |
| Quên mật khẩu | Spring Mail SMTP, token 30 phút, dùng một lần |
| Thanh toán | VNPAY Sandbox |
| Xuất báo cáo | Apache POI Excel |
| Test tự động | 247 test cases PASS |

---

## 4. Lưu ý trước khi viết Word

- Không dùng số test cũ `234` hoặc `242`; số đúng sau khi chạy lại là `247`.
- Không đưa Gmail app password, Google Client Secret, VNPAY secret vào báo cáo.
- Không cần đổi khóa chính `id` thành `user_id`; `id` làm PK và `user_id`, `booking_id` làm FK là convention bình thường trong Spring Boot/JPA.
- Nên chụp hình các màn: login, Google login, forgot password, email reset, đặt phòng, VNPAY, ví Partner, quyết toán Admin, kết quả test pass.
