# TravelMate — Audit TC061-TC110

**Ngày rà soát:** 2026-06-02  
**Phạm vi:** Admin, settlement/wallet/withdrawal, phân quyền, dữ liệu seed, lỗi dịch vụ ngoài và các câu hỏi production.

---

## 1. Kết Luận Ngắn

Các case TC061-TC110 không nên trình bày như một nhóm tính năng mới hoàn toàn. Cách hợp lý là chia thành hai lớp:

- **Đã có code/test bảo vệ:** duyệt listing/phòng, booking không lấy giá từ client, chống overbooking, phân quyền `/admin/**` và `/partner/**`, ví Partner không cho rút quá số dư, withdrawal không xử lý lặp, settlement không cộng ví hai lần, VNPAY verify và idempotency cơ bản, SMTP/OAuth thiếu cấu hình vẫn không làm app sập.
- **Cần nói trung thực là hướng production:** bật CSRF, tách profile/secret production, migration bằng Flyway/Liquibase, monitoring/logging, e2e browser và kiểm thử VNPAY/SMTP/OAuth thật trên môi trường có internet.

Trong phiên rà soát này đã vá thêm guard nhỏ cho Admin duyệt phòng: nếu phòng thiếu accommodation cha hoặc cha chưa được duyệt, hệ thống trả lỗi nghiệp vụ rõ ràng và không duyệt nhầm.

---

## 2. Bằng Chứng Code/Test Chính

| Nhóm case | Trạng thái | Bằng chứng |
|---|---|---|
| TC061-TC064 Admin listing/room | Đã có guard, bổ sung test duyệt phòng | `AccommodationService.approveRoom`, `AccommodationServiceTest` |
| TC065-TC070 Admin booking/refund/no-show | Đã có service test cho trạng thái và hoàn tiền ngoài hệ thống | `BookingCalculationTest`, `PaymentServiceTest` |
| TC071-TC080 Settlement/revenue/wallet | Đã có test tính payout, không cộng direct/manual block, chống double credit | `SettlementServiceTest`, `SettlementEligibilityTest`, `RevenueServiceHomestayTest`, `PartnerWalletServiceTest` |
| TC081-TC085 Withdrawal | Đã có lock, validate bank info, chặn overdraft, PAID/REJECTED idempotent | `PartnerWalletService`, `PartnerWalletAndSettlementIntegrationTest` |
| TC086-TC090 Route security | Đã có Spring Security role guard và MockMvc test 403/redirect | `SecurityConfig`, `PartnerWalletAndSettlementIntegrationTest` |
| TC091-TC095 SQL/import/seed | Đã có static test cho SQL seed và tài liệu health check | `UserPortalSqlSeedTest`, `docs/sql/*` |
| TC096-TC100 Dịch vụ ngoài | Có fallback cấu hình và test cho OAuth/SMTP; VNPAY có test return/IPN | `GoogleOAuthClientConfigTest`, `PasswordResetServiceTest`, `PaymentControllerTest`, `PaymentServiceTest` |
| TC101-TC110 Production edge | Một phần đã có guard; phần còn lại là hạn chế cần nêu rõ | `Security-Notes.md`, `18_Project_Analysis_For_Report_And_Defense.md` |

---

## 3. Các Điểm Nên Demo

1. **Admin duyệt listing/phòng:** tạo hoặc dùng dữ liệu pending, duyệt listing trước, sau đó phòng mới được duyệt và mở bán online.
2. **Wallet/withdrawal:** Partner cập nhật tài khoản ngân hàng, gửi yêu cầu rút trong giới hạn số dư; Admin đánh dấu đã chuyển hoặc từ chối.
3. **Security route:** user thường vào `/admin/settlements` hoặc `/partner/wallet` bị 403; guest bị redirect login.
4. **VNPAY exception:** return/IPN public nhưng không tin dữ liệu trần; hệ thống verify chữ ký, amount và trạng thái trước khi cập nhật booking.
5. **Demo limitation:** CSRF hiện tắt cho local demo; production phải bật token cho form POST.

---

## 4. Câu Trả Lời Bảo Vệ Nên Dùng

**Nếu hỏi rút tiền có chuyển khoản thật không?**  
Không. Đây là ví quyết toán nội bộ. Admin chuyển khoản ngoài hệ thống, sau đó ghi nhận `PAID` hoặc `REJECTED` để hệ thống cập nhật số dư và lịch sử.

**Nếu hỏi Partner nhập số tài khoản sai thì sao?**  
Hệ thống yêu cầu đủ ngân hàng, số tài khoản và chủ tài khoản; số tài khoản chỉ nhận chữ số 6-30 ký tự. Thiếu hoặc sai định dạng thì không gửi được yêu cầu rút tiền.

**Nếu hỏi có chống user sửa giá/roomId không?**  
Có. Backend lấy giá từ `Room` trong database, kiểm tra room thuộc accommodation đã chọn, kiểm tra trạng thái `APPROVED`, sức chứa và tồn phòng theo ngày trước khi tạo booking.

**Nếu hỏi VNPAY/SMTP/OAuth ngoài đời bị lỗi thì sao?**  
VNPAY fail/cancel/expired không approve booking. SMTP thiếu cấu hình trả thông báo chung và không lộ email. Google OAuth chỉ bật khi có đủ Client ID/Secret; thiếu cấu hình thì app vẫn chạy bằng form login.

**Nếu hỏi production đã sẵn sàng chưa?**  
Chưa nên nói là production-ready. Nên nói project đạt mức demo/đồ án với guard nghiệp vụ quan trọng; để production cần bật CSRF, tách secret/profile, thêm migration chuẩn, monitoring, CI/CD và e2e test.

---

## 5. Checklist Trước Khi Bảo Vệ

- Chạy lại `mvnw.cmd test` và dùng đúng số test mới nhất.
- Import SQL trên máy demo, đăng nhập đủ user/admin/partner.
- Chuẩn bị dữ liệu pending listing/room, wallet có số dư, withdrawal pending.
- Chuẩn bị câu trả lời cho CSRF, `ddl-auto=update`, DB `root/root`, VNPAY Sandbox và SMTP/OAuth thiếu cấu hình.
- Tránh nói "hoàn hảo 100%" hoặc "production-ready"; thay bằng "đã nhận diện hạn chế và có hướng nâng cấp".
