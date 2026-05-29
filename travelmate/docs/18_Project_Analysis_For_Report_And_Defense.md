# TravelMate - Phan tich source code phuc vu bao cao va bao ve

Tai lieu nay duoc lap dua tren source code hien co cua project `TravelMate` tai thu muc `travelmate`. Muc tieu la ho tro sinh vien viet bao cao Word, chuan bi slide va bao ve do an co so nganh Cong nghe phan mem mot cach co can cu.

Ghi chu quan trong:

- Noi dung duoi day chi dua tren nhung gi tim thay trong source code, file cau hinh, template, entity, repository, service, controller, test va tai lieu san co.
- Neu mot noi dung duoc ghi la "chua du can cu tu source code", nghia la chua thay bang chung truc tiep trong project va can kiem chung them truoc khi dua vao bao cao.
- Tai lieu khong nham tao noi dung gian doi hoc thuat; muc dich la giup hieu, dien giai va hoan thien project mot cach trung thuc.

## 1. Tom tat project trong 10 dong

1. TravelMate la ung dung web dat phong/noi luu tru du lich, xay dung theo mo hinh Spring Boot MVC ket hop Thymeleaf va MySQL.
2. He thong co bon nhom actor chinh: khach vang lai, nguoi dung da dang nhap, doi tac cung cap noi luu tru va quan tri vien.
3. Chuc nang nguoi dung gom tim kiem noi luu tru, xem chi tiet, dat phong, thanh toan VNPAY, quan ly booking, danh gia, su dung voucher, ho so ca nhan va lien he ho tro.
4. Chuc nang doi tac gom tao noi luu tru, tao phong, gan tien nghi, quan ly trang thai ban phong, booking, doanh thu, doi soat, vi doi tac, yeu cau rut tien va ho tro.
5. Chuc nang quan tri gom dashboard, duyet noi luu tru/phong, quan ly booking, user, voucher, bai viet du lich, ho tro, review, doanh thu, settlement, withdrawal va audit log.
6. Database duoc mo hinh hoa bang JPA Entity va file seed `src/main/resources/travelmate_db.sql`, voi cac bang ve user, accommodation, room, booking, payment, voucher, review, support, notification va settlement.
7. Kien truc tong the la monolithic server-side rendering: controller tra ve trang Thymeleaf, service xu ly nghiep vu, repository truy cap MySQL.
8. Project co them mot so REST API nho cho chatbot, notification, travel post va kiem tra voucher.
9. Cac logic dang chu y gom chong overbooking, tinh tien dat phong, voucher, thanh toan VNPAY, tinh hoa hong, settlement, vi doi tac, danh gia sau booking va chatbot goi y.
10. He thong da co BCrypt, Spring Security va test service kha day du, nhung con rui ro ve CSRF dang tat, cau hinh demo trong `application.properties`, chua thay bang chung Docker/CI-CD va chua thay e2e test day du.

Co the dua vao bao cao nhu sau:

> TravelMate la mot ung dung web ho tro nguoi dung tim kiem va dat noi luu tru trong cac chuyen du lich. He thong duoc xay dung bang Spring Boot, Thymeleaf va MySQL theo mo hinh MVC ket hop layered architecture. Ngoai cac chuc nang danh cho khach hang nhu tim kiem phong, dat phong, thanh toan, danh gia va quan ly ho so, TravelMate con ho tro doi tac quan ly noi luu tru, phong, doanh thu va vi, dong thoi cung cap trang quan tri de kiem duyet du lieu va van hanh he thong.

## 2. Bang cong nghe su dung

| Cong nghe | Vai tro trong project | File/thanh phan lien quan | Nhan xet |
| --- | --- | --- | --- |
| Java 21 | Ngon ngu lap trinh backend | `pom.xml` | Phu hop voi Spring Boot 3.x, can moi truong Java 21 khi build/chay. |
| Spring Boot 3.5.14 | Nen tang chinh cua ung dung | `pom.xml`, `TravelMateApplication.java` | Ho tro web MVC, security, JPA, mail, validation. |
| Spring MVC | Dieu huong request va tra ve view/API | `controller/page`, `controller/api` | Tach tuong doi ro controller trang va controller API. |
| Thymeleaf | Render giao dien server-side | `src/main/resources/templates` | Khong phai SPA; HTML duoc render tu server. |
| Spring Security | Dang nhap, phan quyen, route protection | `security/SecurityConfig.java` | Co role USER, ADMIN, PARTNER; mat khau dung BCrypt. |
| OAuth2 Client | Dang nhap Google neu cau hinh | `security`, `application.properties` | Optional, phu thuoc bien moi truong Google OAuth. |
| Spring Data JPA/Hibernate | ORM va truy cap du lieu | `entity`, `repository` | Entity va repository duoc dung rong rai, giam SQL thu cong. |
| MySQL | He quan tri co so du lieu | `application.properties`, `travelmate_db.sql` | Cau hinh DB dang la demo local; can tach bang `.env` khi trien khai that. |
| Lombok | Giam boilerplate getter/setter | `pom.xml`, entity/service | Can plugin hoac annotation processing trong IDE. |
| Spring Mail | Nen tang gui email/reset password | `EmailService.java`, `PasswordResetService.java`, `application.properties` | SMTP lay tu bien moi truong; PasswordResetService da dau noi EmailService de gui mail reset that khi cau hinh du. |
| Apache POI | Xuat Excel/bao cao | `pom.xml`, controller/service doanh thu/settlement | Dung cho cac chuc nang export trong admin/partner. |
| VNPAY Sandbox | Thanh toan truc tuyen | `PaymentController.java`, `PaymentService.java`, `VnpayService.java` | Cau hinh TMN code/hash secret qua bien moi truong. |
| Groq-compatible API | Chatbot AI optional | `ChatbotService.java`, `application.properties` | Co fallback rule-based neu khong co API key. |
| HTML/CSS/JavaScript | Tuong tac giao dien | `static/assets`, template Thymeleaf | JS dung cho modal, toast, notification, voucher check, filter. |
| JUnit 5, AssertJ, Mockito, Spring Security Test | Kiem thu | `src/test/java` | Co nhieu test service va template; chua du can cu ve e2e test trinh duyet. |
| Maven Wrapper | Build/test project dong nhat | `mvnw`, `mvnw.cmd`, `pom.xml` | Nen dung `mvnw.cmd test` tren Windows. |

Co the dua vao bao cao nhu sau:

> Project TravelMate su dung Java 21 va Spring Boot lam nen tang backend, ket hop Thymeleaf de render giao dien phia server va MySQL lam he quan tri co so du lieu. Cac thanh phan bao mat, phan quyen va dang nhap duoc xay dung bang Spring Security, trong khi tang truy cap du lieu su dung Spring Data JPA/Hibernate. Mot so dich vu ngoai nhu VNPAY, SMTP va Groq AI duoc cau hinh thong qua bien moi truong, giup he thong co the mo rong ma khong can hardcode secret trong code.

## 3. Cau truc thu muc va vai tro

| Thanh phan | Vai tro | File/thu muc lien quan | Nhan xet |
| --- | --- | --- | --- |
| Entry point | Khoi dong ung dung Spring Boot | `src/main/java/com/travelmate/TravelMateApplication.java` | Lop main cua he thong. |
| Page controllers | Xu ly request tra ve trang Thymeleaf | `controller/page` | Bao gom home, auth, booking, payment, admin, partner, profile. |
| API controllers | Tra ve JSON cho cac tinh nang dong | `controller/api` | Gom chatbot, notification, travel post, voucher. |
| Entity | Mo hinh bang du lieu | `entity` | Anh xa cac bang MySQL bang JPA annotation. |
| Repository | Truy van database | `repository` | Dung Spring Data JPA, co mot so query tuy bien cho booking/availability. |
| Service | Xu ly nghiep vu | `service` | Noi dat logic chinh nhu booking, voucher, payment, review, settlement. |
| Security | Cau hinh dang nhap, phan quyen, OAuth | `security` | Quan ly role va route protection. |
| Templates | Giao dien HTML server-side | `src/main/resources/templates` | Chia theo `user`, `admin`, `partner`, `auth`, `fragments`. |
| Static assets | CSS, JS, hinh anh | `src/main/resources/static/assets` | Ho tro giao dien, toast, modal, notification, filter. |
| Database seed/schema | Khoi tao du lieu demo | `src/main/resources/travelmate_db.sql` | Co account demo, accommodation, room, booking, payment, voucher, review. |
| Config | Cau hinh ung dung | `src/main/resources/application.properties` | Chua DB demo, cau hinh VNPAY, SMTP, OAuth, upload, logging. |
| Tests | Kiem thu don vi/tich hop nhe | `src/test/java` | Co test cho booking, voucher, payment, password reset, settlement, template. |
| Docs | Tai lieu phan tich/thiet ke | `docs` | Da co SRS, ERD/PUML, API docs; tai lieu nay bo sung phan tich bao ve. |

Co the dua vao bao cao nhu sau:

> Source code cua TravelMate duoc to chuc theo cac tang ro rang. Tang controller tiep nhan request va dieu huong den view hoac API, tang service xu ly cac quy tac nghiep vu, tang repository truy cap database va tang entity mo hinh hoa du lieu. Giao dien duoc dat trong thu muc template Thymeleaf va tai nguyen tinh duoc dat trong static assets. Cach to chuc nay giup project de bao tri va phu hop voi mo hinh MVC trong ung dung web Spring Boot.

## 4. Phan tich chuc nang

| Chuc nang | Actor | Mo ta | Input | Xu ly chinh | Output | File lien quan |
| --- | --- | --- | --- | --- | --- | --- |
| Trang chu | Guest/User | Hien thi voucher noi bat, diem den, noi dung tong quan | Request `GET /` | Lay voucher/destination dang active | Trang home | `HomePageController.java`, `templates/user/index.html` |
| Tim kiem noi luu tru | Guest/User | Tim hotel/villa/homestay/resort theo loai, tu khoa, ngay, so khach | `type`, `keyword`, `checkIn`, `checkOut`, `adults`, `children`, `rooms` | Chuan hoa diem den, loc accommodation approved, tinh availability | Danh sach noi luu tru | `AccommodationPageController.java`, `AccommodationService.java`, `AvailabilityService.java` |
| Xem chi tiet noi luu tru | Guest/User | Xem phong, anh, tien nghi, review, ngay da duoc dat | `id`, ngay o, so khach | Kiem tra accommodation approved, lay rooms, gallery, review, booking ranges | Trang chi tiet | `hotel-detail.html`, `RoomImageService.java`, `ReviewService.java` |
| Bai viet du lich/tin tuc | Guest/User | Xem cam nang, diem den, bai viet theo category/destination | `category`, `destination`, `keyword` | Loc bai viet visible, sap xep/noi bat | Trang travel/news | `TravelPostService.java`, `travel.html`, `news.html` |
| Lien he/bao loi | Guest/User/Partner | Tao ticket ho tro hoac report | Ho ten, email, phone, category, subject, content | Tao support ticket voi trang thai ban dau | Thong bao gui thanh cong | `SupportTicketService.java`, `HomePageController.java`, `contact.html` |
| Dang ky | Guest | Tao tai khoan user moi | Email, password, ho ten, phone | Validate, hash password BCrypt, luu user | Tai khoan USER | `AuthPageController.java`, `UserService.java`, `register.html` |
| Dang nhap | Guest | Dang nhap form bang email/password | Email, password | Spring Security xac thuc, redirect theo role | Session dang nhap | `SecurityConfig.java`, `CustomAuthSuccessHandler.java`, `login.html` |
| Quen/dat lai mat khau | Guest | Gui link reset neu SMTP duoc dau noi, hoac log link demo khi cau hinh cho phep | Email, token, password moi | Tao token UUID het han 30 phut, invalidate token cu, cap nhat password moi da hash | Mat khau moi | `PasswordResetService.java`, `EmailService.java`, `forgot-password.html`, `reset-password.html` |
| Dat phong | User | Dat phong voi ngay o, so phong, so khach, thong tin lien he | Room, ngay, quantity, adults/children, contact, voucher | Validate, lock room, check availability, tinh tong tien, tao booking/payment | Redirect thanh toan VNPAY | `BookingPageController.java`, `BookingService.java`, `booking.html` |
| Thanh toan VNPAY | User/VNPAY | Tao payment URL, nhan ket qua return/IPN | `bookingId`, VNPAY params | Ky/verify HMAC SHA512, doi trang thai booking/payment | Trang ket qua thanh toan | `PaymentController.java`, `PaymentService.java`, `VnpayService.java` |
| Quan ly booking ca nhan | User | Xem lich su, huy booking, danh gia sau khi hoan thanh | Booking id, ly do huy, rating/comment | Kiem tra chu so huu, trang thai hop le, cap nhat booking/review | Danh sach/trang thai moi | `BookingService.java`, `ReviewService.java`, `mybooking.html` |
| Ho so ca nhan | User | Cap nhat ten, phone, avatar | Form profile, file upload | Validate va luu thong tin/avatar | Profile cap nhat | `UserProfileController.java`, `FileStorageService.java`, `profile.html` |
| Kiem tra voucher | User | Kiem tra ma voucher truoc khi dat phong | `code`, `roomId`, `totalAmount` | Kiem tra active, ngay hieu luc, min order, scope, discount | JSON ket qua giam gia | `VoucherApiController.java`, `VoucherService.java` |
| Chatbot | Guest/User | Tra loi cau hoi ve dat phong, chinh sach, goi y phong | Message text | Nhan dien intent rule-based, co fallback Groq neu cau hinh | JSON reply/quick replies | `ChatbotApiController.java`, `ChatbotService.java` |
| Notification | User | Lay thong bao va danh dau da doc | Current user, notification id | Lay danh sach, dem chua doc, update read flag | JSON notification | `NotificationApiController.java`, `NotificationService.java` |
| Tao noi luu tru | Partner | Doi tac tao accommodation cho loai duoc phep | Ten, dia chi, city, loai, anh, mo ta | Kiem tra role/status/property type, luu PENDING | Accommodation cho admin duyet | `PartnerPageController.java`, `AccommodationService.java`, `accommodation-form.html` |
| Tao phong | Partner | Tao phong thuoc accommodation da approved | Room code/name, bed, capacity, price, quantity, image | Validate owner, room code unique, commission override, luu PENDING | Room cho admin duyet | `Room.java`, `AccommodationService.java`, `room-form.html` |
| Quan ly tien nghi/trang thai ban phong | Partner | Cap nhat amenity va bat/tat selling | Room id, amenity ids, selling flag | Kiem tra quyen so huu, cap nhat room/amenity, co the dua phong ve PENDING neu thay doi lon | Trang thai moi | `PartnerPageController.java`, `AccommodationService.java`, `amenities.html` |
| Quan ly booking doi tac | Partner | Xac nhan giu cho, check-in, check-out, no-show, direct booking, block room | Booking id, ghi chu, ngay, thong tin khach | Kiem tra owner, doi trang thai booking/phong, cap nhat so luong | Booking/trang thai moi | `BookingService.java`, `PartnerPageController.java`, `bookings.html` |
| Doanh thu, settlement, vi doi tac | Partner | Theo doi doanh thu, doi soat, so du, rut tien | Khoang thoi gian, amount, bank info | Tinh gross/commission/payout, sync wallet, tao withdrawal | Bao cao/yeu cau rut tien | `SettlementService.java`, `PartnerWalletService.java`, `PartnerPageController.java` |
| Admin duyet noi luu tru/phong | Admin | Approve/reject accommodation, room, image | Id, approve/reject reason | Cap nhat approval status, availability | Du lieu duoc cong khai/an | `AdminPageController.java`, `AccommodationService.java`, `RoomImageService.java` |
| Admin quan ly user | Admin | Khoa/mo khoa nguoi dung | User id | Cap nhat status | User active/locked | `AdminPageController.java`, `UserService.java`, `users.html` |
| Admin quan ly booking | Admin | Duyet, tu choi, check-in, complete, refund, no-show | Booking id, action, note | Cap nhat booking/payment theo quy trinh | Trang thai moi | `BookingService.java`, `AdminPageController.java` |
| Admin quan ly voucher | Admin | Tao/bat/tat voucher global hoac catalog partner room | Code, discount, scope, dates | Validate voucher, luu/cap nhat active | Voucher moi/trang thai moi | `VoucherService.java`, `vouchers.html` |
| Admin quan ly bai viet | Admin | CRUD travel post va toggle visible/hidden | Title, destination, source URL, content | Validate URL, status, category | Bai viet du lich | `TravelPostApiController.java`, `TravelPostService.java`, `travel-posts.html` |
| Admin ho tro/review/audit | Admin | Xu ly support ticket, an/hien review, xem audit | Ticket/review/action id | Respond/close/reopen, hide/unhide, ghi log | Trang thai xu ly | `SupportTicketService.java`, `ReviewService.java`, `AdminActionLogService.java` |

Rang buoc nghiep vu va validation can luu y:

- Booking yeu cau ngay check-in/check-out hop le, so dem toi thieu 1, so phong tu 1 den 20, nguoi lon toi thieu 1, email/phone hop le.
- Chi phong va accommodation co trang thai `APPROVED`, owner con active va phong con available moi duoc dat.
- Doi tac chi duoc tao accommodation dung loai property duoc gan trong ho so partner.
- Review chi duoc tao sau booking online da `COMPLETED`, moi booking chi co mot review.
- Voucher phai con active, nam trong ngay hieu luc, dat gia tri don hang toi thieu va dung scope phong/loai noi luu tru.
- VNPAY return/IPN phai verify chu ky va so tien truoc khi cap nhat thanh toan.

Co the dua vao bao cao nhu sau:

> Cac chuc nang cua TravelMate duoc thiet ke theo nhom actor ro rang. Khach vang lai co the tim kiem va xem thong tin du lich, nguoi dung da dang nhap co the dat phong, thanh toan, quan ly booking va danh gia. Doi tac co the quan ly tai san luu tru, phong, don dat phong va doanh thu. Quan tri vien dam nhan vai tro kiem duyet du lieu, xu ly booking, quan ly user, voucher, bai viet va cac van de van hanh. Cach phan chia nay giup he thong dap ung duoc ca nghiep vu nguoi dung cuoi lan nghiep vu quan tri noi bo.

## 5. Phan tich kien truc

### 5.1 Mo hinh kien truc

| Nhan dinh | Bang chung trong source code | Danh gia |
| --- | --- | --- |
| MVC | `controller/page`, `templates`, `entity/service/repository` | Controller xu ly request, model la entity/service data, view la Thymeleaf. |
| Layered architecture | Controller -> Service -> Repository -> Entity -> MySQL | Logic nghiep vu phan lon nam trong service, repository chi truy van. |
| Client-server server-side rendering | Browser gui request den Spring Boot va nhan HTML render tu Thymeleaf | Khong thay frontend React/Vue rieng. |
| Monolithic | Frontend templates, backend, API, security cung trong mot Spring Boot app | Don gian khi lam do an, de demo, nhung kho scale doc lap tung thanh phan. |
| RESTful API mot phan | `controller/api` co chatbot, notification, travel post, voucher | Co API JSON nhung phan lon nghiep vu van la MVC form post. |
| Role-based access control | `SecurityConfig.java` cau hinh `/admin/**`, `/partner/**`, `/profile/**` | Phan quyen ro theo role USER/ADMIN/PARTNER. |

### 5.2 Vai tro cac thanh phan

| Thanh phan | Trach nhiem | Vi du file |
| --- | --- | --- |
| Frontend Thymeleaf | Hien thi man hinh, form, bang du lieu, nut hanh dong, goi API nho bang JS | `templates/user/booking.html`, `templates/admin/dashboard.html`, `templates/partner/bookings.html` |
| Controller | Nhan request, doc param/form, goi service, chon view/redirect/API response | `BookingPageController.java`, `AdminPageController.java`, `TravelPostApiController.java` |
| Service | Xu ly nghiep vu chinh, validation, tinh tien, cap nhat trang thai | `BookingService.java`, `VoucherService.java`, `PaymentService.java`, `AvailabilityService.java` |
| Repository | Truy van database bang JPA | `BookingRepository.java`, `RoomRepository.java`, `AccommodationRepository.java` |
| Entity | Mo hinh du lieu va quan he bang | `Booking.java`, `Room.java`, `User.java`, `Payment.java` |
| Database | Luu user, phong, booking, thanh toan, voucher, review, support, settlement | `travelmate_db.sql`, MySQL `travelmate_db` |
| Security | Authentication, authorization, password encoder, OAuth login | `SecurityConfig.java`, `CustomUserDetailsService.java` |

### 5.3 Luong du lieu dat phong

1. Nguoi dung chon phong tai trang chi tiet va mo trang `/booking`.
2. `BookingPageController` nhan thong tin room, ngay o, so khach va render `booking.html`.
3. Khi nguoi dung submit, controller goi `BookingService.createBooking`.
4. `BookingService` validate du lieu, lock room, kiem tra availability, tinh tong tien, ap dung voucher neu co.
5. Service tao `Booking` va `Payment` o trang thai cho thanh toan.
6. He thong redirect sang `/payment/vnpay/create/{bookingId}`.
7. `PaymentController` tao URL VNPAY thong qua `VnpayService`.
8. Khi VNPAY return/IPN, `PaymentService` verify signature/amount va cap nhat payment/booking.
9. Database luu trang thai moi; UI hien thi ket qua thanh toan va lich su booking.

### 5.4 Separation of concerns va diem can refactor

| Hạng muc | Hien trang | Nhan xet/cai thien |
| --- | --- | --- |
| Controller-service-repository | Da tach lop tuong doi ro | Phu hop voi do an; logic chinh nam trong service. |
| Controller admin/partner | Mot so controller kha lon va gom nhieu nghiep vu | Co the tach thanh controller nho theo domain: booking, settlement, voucher, support. |
| Validation | Co validation trong service va mot phan trong controller/template | Nen chuan hoa DTO + Bean Validation cho cac form quan trong. |
| API va MVC | REST API chi dung cho mot so tinh nang dong | Chap nhan duoc voi SSR, nhung can giai thich ro day khong phai SPA. |
| Database migration | Dung `ddl-auto=update` va seed SQL | Nen dung Flyway/Liquibase neu muon chuyen sang production. |
| Cau hinh | Secret chinh lay env, nhung DB demo nam trong properties | Nen bo sung `.env.example` va profile dev/prod ro rang. |

Co the dua vao bao cao nhu sau:

> TravelMate ap dung mo hinh MVC ket hop layered architecture. Tang giao dien duoc xay dung bang Thymeleaf va JavaScript, tang controller tiep nhan request tu nguoi dung, tang service xu ly cac quy tac nghiep vu, tang repository truy cap MySQL thong qua Spring Data JPA, va tang entity mo hinh hoa cac bang du lieu. Mo hinh nay giup tach biet trach nhiem giua giao dien, xu ly nghiep vu va luu tru du lieu. Tuy nhien, mot so controller quan tri va doi tac con gom nhieu chuc nang, co the tiep tuc tach nho de tang kha nang bao tri.

## 6. Phan tich database

### 6.1 Danh sach bang/collection chinh

| Bang/Collection | Muc dich | Truong quan trong | Quan he | Ghi chu |
| --- | --- | --- | --- | --- |
| `users` | Luu tai khoan nguoi dung, admin, partner | `id`, `email`, `password`, `full_name`, `role`, `status`, `partner_property_type`, bank info | 1-n voi booking, review, accommodation owner, notification | Password trong seed la BCrypt hash; account demo co trong SQL. |
| `password_reset_tokens` | Luu token reset mat khau | `token`, `user_id`, `expires_at`, `used` | n-1 voi `users` | Token het han sau 30 phut theo service. |
| `accommodations` | Luu noi luu tru | `id`, `name`, `address`, `city`, `property_type`, `approval_status`, `owner_id`, `rating` | n-1 user partner, 1-n rooms/bookings/reviews | Chi approved moi hien thi cong khai. |
| `rooms` | Luu phong | `id`, `room_code`, `room_name`, `capacity`, `price_per_night`, `available_quantity`, `approval_status` | n-1 accommodation, n-n amenities, 1-n bookings | Room code unique; co trang thai ban phong. |
| `room_images` | Luu anh phong | `room_id`, `image_url`, `caption`, `sort_order`, `is_primary` | n-1 room | Dung cho gallery phong. |
| `amenities` | Luu tien nghi | `name`, `icon`, `category` | n-n voi rooms qua `room_amenities` | Ho tro gan tien nghi cho phong. |
| `room_amenities` | Bang lien ket phong-tien nghi | `room_id`, `amenity_id` | n-n | Sinh tu quan he many-to-many. |
| `bookings` | Luu don dat phong | `booking_code`, `user_id`, `room_id`, `check_in`, `check_out`, `total_amount`, `booking_status`, `payment_status` | n-1 user/accommodation/room, 1-n payments, 1-1 review | Co nhieu truong snapshot tai chinh de doi soat. |
| `payments` | Luu giao dich thanh toan | `booking_id`, `amount`, `payment_status`, `vnp_txn_ref`, `transaction_no`, VNPAY fields | n-1 booking | Dung cho VNPAY return/IPN va doi trang thai. |
| `reviews` | Luu danh gia sau booking | `user_id`, `accommodation_id`, `booking_id`, `rating`, `comment`, `is_hidden` | n-1 user/accommodation, 1-1 booking | Rating hop le 1-10, admin co the an/hien. |
| `vouchers` | Luu ma giam gia | `code`, `discount_type`, `discount_value`, `max_discount`, `min_order`, `voucher_scope`, `cost_bearer` | Co the gan user global, accommodation/room/partner | Ho tro voucher global va partner room catalog. |
| `room_voucher_assignments` | Gan voucher vao phong | `voucher_id`, `room_id`, `active`, `assigned_at` | n-1 voucher/room | Co rang buoc unique room-voucher. |
| `support_tickets` | Luu yeu cau ho tro | requester info, `category`, `subject`, `priority`, `status`, `admin_response` | Co the lien ket user/partner | Dung cho contact, partner support va admin support. |
| `notifications` | Luu thong bao | `user_id`, `title`, `message`, `type`, `target_url`, `is_read` | n-1 user | API lay danh sach va danh dau da doc. |
| `partner_settlements` | Luu ky doi soat doi tac | `partner_id`, `period_start`, `period_end`, `gross`, `commission`, `payout`, `settlement_status` | n-1 partner | Dung cho doanh thu/settlement hang thang. |
| `partner_wallets` | Luu so du vi doi tac | `partner_id`, `available_balance`, `pending_withdrawal_amount`, `total_earned_amount` | 1-1 partner | Dong bo voi settlement da thanh toan. |
| `partner_withdrawal_requests` | Luu yeu cau rut tien | `request_code`, `partner_id`, `amount`, bank info, `withdrawal_status` | n-1 partner | Admin co the mark paid/reject. |
| `partner_wallet_transactions` | Luu lich su bien dong vi | `transaction_code`, `transaction_type`, `direction`, `amount`, balances | n-1 partner, optional settlement/withdrawal | Dung de audit tai chinh doi tac. |
| `admin_action_logs` | Luu lich su hanh dong admin | `admin_email`, `action_type`, `target_type`, `target_id`, `description` | Doc lap hoac tham chieu logic | Huu ich khi bao ve ve audit. |
| `travel_posts` | Luu bai viet du lich | `title`, `destination_name`, `destination_slug`, `category`, `status`, `content` | Co `created_by` | API/admin quan ly bai viet. |
| `travel_destinations` | Luu diem den hien thi | `name`, `slug`, `region`, `image_url`, `active`, `display_order` | Doc lap voi accommodation city | Dung cho trang chu/travel. |

### 6.2 Mo ta ERD bang loi

- Mot `User` co the co nhieu `Booking`, nhieu `Review`, nhieu `Notification`. Neu user co role PARTNER, user do co the so huu nhieu `Accommodation`.
- Mot `Accommodation` thuoc mot partner owner va co nhieu `Room`, nhieu `Booking`, nhieu `Review`.
- Mot `Room` thuoc mot `Accommodation`, co nhieu `RoomImage`, co the duoc gan nhieu `Amenity` thong qua bang trung gian `room_amenities`, va co nhieu `Booking`.
- Mot `Booking` gan voi mot `User`, mot `Accommodation`, mot `Room`; booking co the co nhieu `Payment`, va toi da mot `Review`.
- Mot `Payment` thuoc mot `Booking`, luu thong tin VNPAY de doi soat va xac nhan thanh toan.
- Mot `Voucher` co the la voucher global hoac voucher lien quan den phong/doi tac; voi voucher phong, quan he gan duoc quan ly bang `room_voucher_assignments`.
- Mot partner co mot `PartnerWallet`, nhieu `PartnerSettlement`, nhieu `PartnerWithdrawalRequest` va nhieu `PartnerWalletTransaction`.
- `SupportTicket` co the do guest, user hoac partner tao; vi vay quan he voi user/partner co the optional.

### 6.3 Diem manh va diem yeu

| Hạng muc | Diem manh | Han che/rui ro |
| --- | --- | --- |
| Mo hinh domain | Bao quat day du booking, payment, voucher, review, partner finance | So bang va so truong nhieu, can giai thich tap trung khi bao ve. |
| Quan he du lieu | Co FK logic giua user, accommodation, room, booking, payment | Can kiem tra migration/constraint that trong DB neu khong dung schema SQL day du. |
| Snapshot tai chinh | Booking luu commission/voucher/payout snapshot, giup doi soat khong bi thay doi theo config moi | Lam entity booking phuc tap hon. |
| Seed data | Co du lieu demo phuc vu demo nhanh | Khong nen coi seed la du lieu production. |
| Migration | Dung JPA update va SQL seed | Chua thay Flyway/Liquibase; nen de xuat cai thien. |

Co the dua vao bao cao nhu sau:

> Co so du lieu cua TravelMate duoc thiet ke xoay quanh cac thuc the chinh la User, Accommodation, Room, Booking, Payment, Voucher va Review. Quan he cot loi cua he thong la mot doi tac so huu nhieu noi luu tru, moi noi luu tru co nhieu phong, nguoi dung tao booking cho phong va booking phat sinh giao dich thanh toan. Ngoai cac bang nghiep vu chinh, he thong con bo sung cac bang ho tro nhu notification, support ticket, travel post, settlement va partner wallet de phuc vu van hanh thuc te.

## 7. Phan tich API/backend

### 7.1 Cac endpoint/chuc nang backend chinh

| Method | Endpoint | Chuc nang | Input | Output | File xu ly | Ghi chu |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/` | Trang chu | None | HTML home | `HomePageController.java` | Public. |
| GET | `/accommodations` | Tim kiem/list noi luu tru | Query type/keyword/date/guest | HTML list | `AccommodationPageController.java` | Public, chi hien approved. |
| GET | `/accommodations/{id}` | Chi tiet noi luu tru | Path id, query booking info | HTML detail | `AccommodationPageController.java` | Block accommodation chua approved. |
| GET | `/booking` | Trang dat phong | `roomId`, dates, guest info | HTML form | `BookingPageController.java` | USER. |
| POST | `/booking/confirm` | Tao booking | Form booking | Redirect payment | `BookingPageController.java`, `BookingService.java` | USER. |
| GET | `/my-bookings` | Lich su booking | Current user | HTML list | `BookingPageController.java` | USER. |
| POST | `/my-bookings/{id}/cancel` | Huy booking | Booking id, reason | Redirect/result | `BookingService.java` | USER, kiem tra chu so huu. |
| POST | `/my-bookings/{bookingId}/review` | Tao review | Rating/comment | Redirect | `ReviewService.java` | Chi booking completed. |
| GET | `/payment/vnpay/create/{bookingId}` | Tao URL VNPAY | Booking id | Redirect VNPAY | `PaymentController.java`, `VnpayService.java` | USER. |
| GET | `/payment/vnpay-return` | Nhan ket qua thanh toan | VNPAY params | HTML result | `PaymentController.java`, `PaymentService.java` | Verify signature/amount. |
| GET | `/payment/vnpay-ipn` | Nhan IPN VNPAY | VNPAY params | JSON/text VNPAY response | `PaymentController.java`, `PaymentService.java` | Public endpoint nhung co verify. |
| GET/POST | `/auth/register` | Dang ky user | Form register | HTML/redirect | `AuthPageController.java`, `UserService.java` | Password duoc hash. |
| GET/POST | `/auth/forgot-password` | Yeu cau reset password | Email | HTML message | `AuthPageController.java`, `PasswordResetService.java` | Chong enumerate tai khoan bang message chung. |
| GET/POST | `/auth/reset-password` | Dat lai mat khau | Token, password moi | HTML/redirect | `PasswordResetService.java` | Token 30 phut, one-time. |
| GET/POST | `/profile` | Xem/cap nhat ho so | Form profile, avatar | HTML profile | `UserProfileController.java` | USER. |
| GET/POST | `/partner/**` | Nghiep vu doi tac | Form/path/query tuy chuc nang | HTML/redirect | `PartnerPageController.java` | PARTNER. |
| GET/POST | `/admin/**` | Nghiep vu quan tri | Form/path/query tuy chuc nang | HTML/redirect | `AdminPageController.java` | ADMIN. |
| POST | `/api/chatbot/message` | Chatbot hoi dap | JSON `{message}` | JSON `{intent, reply, quickReplies}` | `ChatbotApiController.java`, `ChatbotService.java` | Public. |
| GET | `/api/notifications` | Lay notification | Current user | JSON list | `NotificationApiController.java` | Can authenticated theo security. |
| GET | `/api/notifications/count` | Dem notification chua doc | Current user | JSON count | `NotificationApiController.java` | Tra 0 neu chua dang nhap. |
| POST | `/api/notifications/{id}/read` | Danh dau da doc | Notification id | JSON success | `NotificationApiController.java` | USER da dang nhap. |
| POST | `/api/notifications/read-all` | Danh dau tat ca da doc | Current user | JSON success | `NotificationApiController.java` | USER da dang nhap. |
| GET | `/api/travel-posts/{id}` | Lay bai viet | Post id | JSON post | `TravelPostApiController.java` | Public voi post visible, admin thay hidden. |
| POST | `/api/travel-posts` | Tao bai viet | JSON post | JSON created | `TravelPostApiController.java` | ADMIN. |
| PUT | `/api/travel-posts/{id}` | Cap nhat bai viet | JSON post | JSON updated | `TravelPostApiController.java` | ADMIN. |
| POST | `/api/travel-posts/{id}/toggle` | An/hien bai viet | Post id | JSON status | `TravelPostApiController.java` | ADMIN. |
| DELETE | `/api/travel-posts/{id}` | Xoa bai viet | Post id | JSON result | `TravelPostApiController.java` | ADMIN. |
| GET | `/api/voucher/check` | Kiem tra voucher | `code`, `roomId`, `totalAmount` | JSON valid/discount/error | `VoucherApiController.java`, `VoucherService.java` | Thuong tra HTTP 200 voi `valid=false` khi loi nghiep vu. |

### 7.2 Danh gia API

| Tieu chi | Danh gia | Giai thich |
| --- | --- | --- |
| RESTful | Dat mot phan | `travel-posts` dung GET/POST/PUT/DELETE kha dung chuan; nhieu nghiep vu con la MVC form post nen khong phai REST API thuan. |
| Naming endpoint | Kha ro | `/api/chatbot/message`, `/api/voucher/check`, `/api/notifications` de hieu; admin/partner page endpoints theo domain. |
| Status code | Kha on, nhung chua dong nhat | `TravelPostApiController` va notification co 401/403/404; voucher check tra body `valid=false` thay vi status loi nghiep vu. |
| Authentication | Co | Spring Security bao ve route theo role; mot so API con kiem tra admin trong controller. |
| Authorization | Co, nhung can tiep tuc test | Service co nhieu check owner/role; nen co test them cho IDOR. |
| Validation | Co nhung phan tan | Service validate nhieu quy tac; travel post validate URL; booking validate form. Nen chuan hoa DTO. |
| Error handling | Tuong doi | Co try/catch va message, nhung chua thay global exception handler day du. |
| Bao mat | Co nen tang, con rui ro | BCrypt va route protection tot; CSRF dang disable la diem can cai thien. |

Co the dua vao bao cao nhu sau:

> Backend cua TravelMate ket hop hai kieu endpoint: page endpoint tra ve trang Thymeleaf va REST API tra ve JSON cho cac tinh nang dong. Cac endpoint dat phong, thanh toan, quan ly booking, admin va partner chu yeu duoc xu ly theo mo hinh MVC. Ben canh do, he thong cung cap API cho chatbot, notification, voucher checking va travel post management. Cach tiep can nay phu hop voi ung dung server-side rendering, dong thoi van dap ung cac thao tac bat dong bo tren giao dien.

## 8. Phan tich frontend/UI

### 8.1 Tong quan frontend

| Noi dung | Hien trang trong project | File/thanh phan |
| --- | --- | --- |
| Framework UI | Thymeleaf + HTML/CSS/JavaScript | `templates`, `static/assets` |
| Routing | Do Spring MVC controller dieu huong | `controller/page` |
| State management | Khong co state management SPA; trang thai den tu server/session/form | Thymeleaf model, Spring Security session |
| Goi API | JavaScript goi API notification, chatbot, voucher, admin travel post | `static/assets/js`, inline JS trong template |
| Validate form | Co ca frontend JS va backend service validation | `booking.html`, `BookingService.java`, `VoucherService.java` |
| Loading/error | Co toast/modal/message o nhieu template | `main.js`, template fragments |
| Layout dung chung | Header/footer/sidebar/fragments | `templates/fragments` |

### 8.2 Man hinh/component chinh

| Man hinh/Component | Chuc nang | Du lieu su dung | API lien quan | File lien quan |
| --- | --- | --- | --- | --- |
| Trang chu | Gioi thieu, voucher, diem den | Voucher active, destination active | Khong ro API rieng | `templates/user/index.html`, `HomePageController.java` |
| Login/Register | Dang nhap/dang ky | Email, password, user info | Spring Security form login | `templates/auth/login.html`, `templates/auth/register.html` |
| Forgot/Reset Password | Reset mat khau | Email, token, password moi | MVC form post | `templates/auth/forgot-password.html`, `templates/auth/reset-password.html` |
| Accommodation list | Tim kiem/filter noi luu tru | Accommodation approved, availability | Query MVC | `templates/user/hotels.html` |
| Accommodation detail | Xem phong, review, gallery | Room, image, review, date range | MVC; voucher/booking link | `templates/user/hotel-detail.html` |
| Booking page | Nhap thong tin dat phong, voucher | Room, user, price, voucher | `/api/voucher/check`, `/booking/confirm` | `templates/user/booking.html` |
| My bookings | Xem/huy/danh gia booking | Booking cua user | MVC form post | `templates/user/mybooking.html` |
| Payment result | Hien thi ket qua VNPAY | Booking/payment status | VNPAY return | `templates/user/payment-result.html` |
| Travel/News | Bai viet du lich | Travel posts/destinations | `GET /api/travel-posts/{id}` mot phan | `travel.html`, `news.html` |
| Voucher page | Danh sach voucher | Voucher active | `/api/voucher/check` khi dat phong | `voucher.html` |
| Contact | Gui support ticket/report | Form contact | MVC form post | `contact.html` |
| Profile | Cap nhat thong tin/avatar | User profile | MVC form post | `profile.html` |
| Partner dashboard | Tong quan doi tac | Booking/revenue/room status | MVC | `templates/partner/dashboard.html` |
| Partner accommodations/rooms | Tao va quan ly noi luu tru/phong | Accommodation, room, amenities | MVC form post | `partner/accommodations.html`, `room-form.html`, `amenities.html` |
| Partner bookings | Quan ly booking doi tac | Booking theo partner | MVC form post | `partner/bookings.html`, `booking-detail.html` |
| Partner wallet/settlements | Doanh thu, vi, rut tien | Settlement, wallet, withdrawal | MVC form post/export | `partner/wallet.html`, `settlements.html` |
| Admin dashboard | Tong quan he thong | Metrics booking/user/revenue | MVC | `admin/dashboard.html` |
| Admin approvals | Duyet accommodation/room/image | Pending records | MVC form post | `admin/accommodations.html`, `admin/rooms.html` |
| Admin users/vouchers | Quan ly user va voucher | Users, vouchers | MVC/API mot phan | `admin/users.html`, `admin/vouchers.html` |
| Admin travel posts | Quan ly bai viet | TravelPost | `/api/travel-posts/**` | `admin/travel-posts.html` |
| Admin support/reviews/audit | Van hanh va kiem soat | Ticket, review, log | MVC | `admin/support.html`, `admin/reviews.html`, `admin/audit-log.html` |

### 8.3 Mo ta giao dien cho bao cao

- Giao dien dang nhap/dang ky: duoc tach trong `templates/auth`, su dung form email/password va tich hop Spring Security. Dang nhap Google co cau hinh optional trong security.
- Giao dien trang chu: `index.html` hien thi du lieu tu controller nhu voucher va diem den, phu hop vai tro landing/home cua ung dung.
- Giao dien tim kiem: `hotels.html` cho phep loc theo loai noi luu tru, tu khoa, ngay va so khach; du lieu duoc xu ly o backend.
- Giao dien chi tiet: `hotel-detail.html` hien thi thong tin accommodation, danh sach room, hinh anh, review va thong tin ngay dat.
- Giao dien dat phong: `booking.html` co tinh tong tien, kiem tra voucher bang API va submit ve backend tao booking.
- Giao dien quan tri/doi tac: co nhieu trang bang du lieu va form thao tac, phu hop tinh chat operational dashboard.
- Chua du can cu tu source code ve anh chup giao dien that neu khong mo chay ung dung; phan mo ta tren dua vao template/page trong code.

Co the dua vao bao cao nhu sau:

> Giao dien TravelMate duoc xay dung bang Thymeleaf, trong do moi man hinh tuong ung voi mot template HTML duoc render boi Spring MVC. Cac trang phia nguoi dung tap trung vao tim kiem, xem chi tiet va dat phong; cac trang doi tac va admin tap trung vao bang du lieu, form quan ly va thao tac van hanh. JavaScript duoc su dung cho cac tuong tac nho nhu hien thi thong bao, kiem tra voucher, chatbot va cap nhat notification.

## 9. Phan tich logic/thuat toan

| Logic | Muc dich | Cach hoat dong | File lien quan | Do phuc tap neu xac dinh duoc | Nhan xet |
| --- | --- | --- | --- | --- | --- |
| Chuan hoa diem den/tim kiem | Tim accommodation theo tu khoa tieng Viet/alias | Normalize text, slug, alias nhu Da Lat/Nha Trang/TPHCM, loc approved | `DestinationAliasUtil.java`, `AccommodationService.java` | Gan O(n) theo so accommodation neu loc tren danh sach; query DB ho tro mot phan | Logic phu hop demo du lich Viet Nam. |
| Availability/overlap booking | Chong dat trung phong | Tinh tong so phong, so booking active overlap theo dieu kien `checkIn < requestedCheckOut` va `checkOut > requestedCheckIn` | `AvailabilityService.java`, `BookingRepository.java` | Phu thuoc query DB; ve logic la dem booking overlap | Day la logic quan trong nhat khi bao ve. |
| Lock room khi tao booking | Giam nguy co race condition khi nhieu user dat cung phong | Repository lock room, kiem tra lai quantity/overlap, tao booking/payment | `BookingService.java`, `RoomRepository.java` | Phu thuoc transaction/DB lock | Can giai thich de tranh overbooking. |
| Tinh tien booking | Tinh tong tien va so tien can thanh toan | Nights = days between check-in/check-out, total = price * nights * roomQuantity, deposit 30% hoac full | `BookingService.java` | O(1) | Co snapshot tai chinh de doi soat. |
| Voucher | Tinh giam gia va kiem tra dieu kien ap dung | Kiem active/date/minOrder/scope/property type/room assignment, tinh percent/fixed va max cap | `VoucherService.java`, `VoucherApiController.java` | O(1) + truy van DB | Co phan cost bearer admin/partner. |
| VNPAY signature | Dam bao request thanh toan hop le | Sap xep params, tao/verify HMAC SHA512, doi chieu amount/status | `VnpayService.java`, `PaymentService.java` | O(k log k) voi k la so param | Can noi ro public endpoint van an toan nho verify signature. |
| Expire pending payment | Huy booking chua thanh toan qua han | Scheduler quet booking/payment pending het han, restore room quota | `PaymentService.java`, scheduler config | O(n) theo so pending het han | Giam giu phong ao. |
| Review eligibility | Chi danh gia booking hop le | Kiem user owns booking, booking online, status completed, chua review | `ReviewService.java` | O(1) + query | Tang do tin cay cua review. |
| Rating recalculation | Cap nhat diem accommodation | Tinh lai trung binh review khong bi hidden | `ReviewService.java` | O(r) theo so review cua accommodation | Co admin hide/unhide review. |
| Commission/settlement | Tinh hoa hong va payout doi tac | Lay ty le theo property type/override, gross - commission - voucher partner | `CommissionService.java`, `SettlementService.java` | Phu thuoc so payment trong ky | Co snapshot de tranh sai khi ty le thay doi sau nay. |
| Partner wallet/withdrawal | Quan ly so du va rut tien | Sync settlement paid, credit wallet, validate bank/amount, tao request | `PartnerWalletService.java` | O(n) khi sync settlements | Logic tai chinh can demo can than. |
| Password reset | Reset mat khau an toan | Tao token UUID, het han 30 phut, invalidate token cu, hash password moi; gui mail that qua EmailService khi SMTP cau hinh du; khong tiet lo email ton tai hay khong | `PasswordResetService.java`, `EmailService.java` | O(1) + query | Tot cho bao mat co ban, co fallback link local neu bat demo link. |
| Chatbot | Hoi dap va goi y phong | Rule-based intent, extract budget/date/guest/preference, tim room phu hop; optional Groq fallback | `ChatbotService.java` | Phu thuoc so room/accommodation duoc loc | Nen trinh bay la rule-based + AI optional, khong noi la recommendation AI nang cao neu chua nang cap. |
| Upload file | Luu avatar/thumbnail/anh phong | Co `FileStorageService` va cau hinh upload size | `FileStorageService.java`, `application.properties` | Chua du can cu neu khong doc sau ve whitelist chi tiet | Khi bao ve nen nam ro gioi han file va loai file. |

Co the dua vao bao cao nhu sau:

> Cac xu ly chinh cua TravelMate tap trung vao dam bao booking hop le va tranh tinh trang overbooking. Khi nguoi dung dat phong, he thong validate ngay, so khach, so phong, trang thai phong va tinh kha dung trong khoang thoi gian duoc chon. Sau do, he thong tinh tong tien, ap dung voucher neu co, tao booking/payment va chuyen nguoi dung sang VNPAY. Ngoai ra, he thong con co cac xu ly bo tro nhu reset mat khau bang token, tinh hoa hong doi tac, doi soat doanh thu, vi doi tac, danh gia sau booking va chatbot ho tro nguoi dung.

## 10. Phan tich bao mat va chat luong phan mem

| Hang muc | Hien trang trong project | Rui ro | De xuat cai thien |
| --- | --- | --- | --- |
| Hash mat khau | Dung BCrypt trong Spring Security/UserService | Thap neu dung dung cach | Giu BCrypt, dam bao khong log password. |
| Authentication | Form login bang Spring Security, optional Google OAuth | Phu thuoc cau hinh OAuth/SMTP moi truong | Bo sung huong dan cau hinh `.env.example`. |
| Authorization | Route theo role USER/ADMIN/PARTNER trong `SecurityConfig.java` | Co the con thieu test cho mot so ownership edge case | Them test truy cap trai phep cho booking/partner/admin. |
| CSRF | `csrf.disable()` | Cao voi form POST MVC neu public web | Bat CSRF cho form, hoac giai thich day la han che demo va lap ke hoach cai thien. |
| Secret/API key | VNPAY/Groq/SMTP/Google dung bien moi truong | Tot hon hardcode; nhung DB demo `root/root` nam trong properties | Tach profile dev/prod, them `.env.example`, khong commit secret production. |
| Validate input | Booking/voucher/travel post/password reset co validation | Validation phan tan, de thieu o form moi | Dung DTO + Bean Validation + binding error dong nhat. |
| SQL injection | Dung JPA repository/query parameter | Rui ro thap neu khong noi chuoi SQL thu cong | Tiep tuc dung parameterized query. |
| XSS | Thymeleaf mac dinh escape, chatbot co ham escape | Can canh giac voi noi dung HTML/bai viet/URL anh | Validate/sanitize rich content neu cho admin nhap HTML. |
| CSRF/IDOR | IDOR duoc giam bang check current user/partner ownership trong service | Can test them cac endpoint path id | Them integration test cho user khong duoc sua/xem booking cua user khac. |
| Payment security | VNPAY return/IPN verify chu ky va amount | Neu cau hinh secret sai co the fail; endpoint public can logging | Log giao dich va verify idempotency day du. |
| Logging | Co file log `logs/travelmate.log` | Can tranh log thong tin nhay cam | Chuan hoa log level production. |
| Error handling | Co try/catch va message o nhieu noi | Chua thay global exception handler ro | Them `@ControllerAdvice` cho loi chung. |
| Test | Co nhieu test service/template/SQL | Chua du can cu ve e2e/browser test va CI | Bo sung test API/security/e2e va pipeline CI. |
| Deployment | Chua thay Dockerfile/docker-compose trong source da ra soat | Kho demo tren may khac neu thieu huong dan | Bo sung README trien khai, Docker neu co thoi gian. |

Co the dua vao bao cao nhu sau:

> Ve bao mat, TravelMate da ap dung cac co che nen tang nhu Spring Security, BCrypt password encoder va phan quyen theo role. Cac endpoint quan tri va doi tac duoc gioi han theo vai tro, dong thoi cac thao tac nhay cam nhu thanh toan VNPAY co kiem tra chu ky va so tien. Tuy nhien, project hien dang tat CSRF va con cau hinh demo database trong file properties, vi vay day la nhung han che can neu trung thuc trong bao cao va co ke hoach cai thien truoc khi trien khai thuc te.

## 11. Yeu cau chuc nang

| Ma yeu cau | Ten yeu cau | Mo ta | Actor | Muc uu tien | Trang thai trong project |
| --- | --- | --- | --- | --- | --- |
| FR-01 | Dang ky tai khoan | Cho phep guest tao tai khoan user bang email/password | Guest | Cao | Da co |
| FR-02 | Dang nhap/dang xuat | Xac thuc user va redirect theo role | Guest/User/Partner/Admin | Cao | Da co |
| FR-03 | Reset mat khau | Gui token va cap nhat mat khau moi | Guest | Trung binh | Da co |
| FR-04 | Tim kiem noi luu tru | Loc theo loai, diem den, ngay, so khach | Guest/User | Cao | Da co |
| FR-05 | Xem chi tiet noi luu tru | Hien thi phong, gia, review, anh va availability | Guest/User | Cao | Da co |
| FR-06 | Dat phong | Tao booking va payment pending | User | Cao | Da co |
| FR-07 | Thanh toan VNPAY | Chuyen den VNPAY va cap nhat ket qua | User | Cao | Da co |
| FR-08 | Quan ly booking ca nhan | Xem lich su, huy booking, xem trang thai | User | Cao | Da co |
| FR-09 | Danh gia sau chuyen di | Tao review cho booking completed | User | Trung binh | Da co |
| FR-10 | Ap dung voucher | Kiem tra va tinh giam gia | User | Trung binh | Da co |
| FR-11 | Quan ly ho so | Cap nhat thong tin ca nhan/avatar | User | Trung binh | Da co |
| FR-12 | Chatbot ho tro | Tra loi cau hoi va goi y co ban | Guest/User | Trung binh | Da co, AI optional |
| FR-13 | Doi tac tao noi luu tru | Tao accommodation cho admin duyet | Partner | Cao | Da co |
| FR-14 | Doi tac tao/quan ly phong | Tao phong, tien nghi, trang thai ban | Partner | Cao | Da co |
| FR-15 | Doi tac quan ly booking | Xac nhan, check-in, check-out, no-show, direct booking | Partner | Cao | Da co |
| FR-16 | Doi tac xem doanh thu/vi | Xem settlement, wallet, yeu cau rut tien | Partner | Trung binh | Da co |
| FR-17 | Admin duyet noi luu tru/phong | Approve/reject accommodation/room/image | Admin | Cao | Da co |
| FR-18 | Admin quan ly user | Khoa/mo khoa user | Admin | Trung binh | Da co |
| FR-19 | Admin quan ly voucher | Tao/bat/tat voucher | Admin | Trung binh | Da co |
| FR-20 | Admin quan ly booking | Duyet, tu choi, refund, no-show, complete | Admin | Cao | Da co |
| FR-21 | Admin quan ly bai viet du lich | CRUD/toggle travel posts | Admin | Trung binh | Da co |
| FR-22 | Ho tro/ticket | Guest/user/partner gui ticket, admin xu ly | Guest/User/Partner/Admin | Trung binh | Da co |
| FR-23 | Notification | Hien thi va danh dau thong bao | User | Thap | Da co |
| FR-24 | Deployment production | Chay production voi Docker/CI/CD | Admin/Dev | Thap | Chua du can cu tu source code |

Co the dua vao bao cao nhu sau:

> Yeu cau chuc nang cua TravelMate duoc chia thanh bon nhom theo actor: khach vang lai, nguoi dung, doi tac va quan tri vien. Cac chuc nang cot loi nhu tim kiem noi luu tru, dat phong, thanh toan, quan ly booking, duyet phong va quan ly doi tac da co bang chung trong source code. Mot so yeu cau lien quan den trien khai production nhu Docker hay CI/CD chua co can cu trong source, do do chi nen dua vao muc huong phat trien.

## 12. Yeu cau phi chuc nang

| Ma yeu cau | Nhom yeu cau | Mo ta | Cach project dap ung | Ghi chu |
| --- | --- | --- | --- | --- |
| NFR-01 | Hieu nang | Tim kiem va load trang phai phan hoi hop ly | Dung JPA query, server-side rendering, filter theo approved/status | Nen them pagination/cache neu du lieu lon. |
| NFR-02 | Bao mat | Bao ve route theo role va hash mat khau | Spring Security, BCrypt, route protection | CSRF dang tat la han che quan trong. |
| NFR-03 | Kha dung | He thong co the dung cho nhieu role | Trang rieng cho user/partner/admin | Chua du can cu ve monitoring/uptime production. |
| NFR-04 | De su dung | UI chia theo luong ro rang: tim kiem, dat phong, quan ly | Thymeleaf templates, toast/modal, dashboard | Can anh giao dien de chung minh trong bao cao. |
| NFR-05 | Kha nang mo rong | Co service/repository rieng theo domain | Layered architecture | Monolith se kho scale doc lap neu du lieu lon. |
| NFR-06 | Kha nang bao tri | Code chia package controller/service/repository/entity | Cau truc ro | Controller admin/partner nen tach nho hon. |
| NFR-07 | Tuong thich | Web app chay tren browser, backend Java 21 | HTML/CSS/JS + Spring Boot | Can test responsive/browser neu dua vao bao cao. |
| NFR-08 | Do tin cay | Co test cho nhieu service va logic nghiep vu | `src/test/java` | Nen them e2e va security tests. |
| NFR-09 | Kha nang phuc hoi loi | Payment pending co expire, VNPAY co IPN/return verify | Scheduler va payment status | Can them retry/log/monitoring production. |
| NFR-10 | Audit/kiem soat | Co admin action log va wallet transaction | `AdminActionLogService`, wallet transaction | Tot cho nghiep vu quan tri va tai chinh. |

Co the dua vao bao cao nhu sau:

> Ngoai yeu cau chuc nang, TravelMate dap ung mot so yeu cau phi chuc nang nhu bao mat co ban, kha nang bao tri, do tin cay nghiep vu va kha nang mo rong o muc do do an. He thong su dung Spring Security de phan quyen, BCrypt de hash mat khau, va co cac test cho nhieu service quan trong. Tuy nhien, de san sang trien khai thuc te, project can bo sung CSRF protection, logging/monitoring, CI/CD, e2e test va cau hinh production ro rang.

## 13. Test case de xuat

| Ma test | Chuc nang | Dieu kien dau vao | Cac buoc thuc hien | Ket qua mong doi | Loai test |
| --- | --- | --- | --- | --- | --- |
| TC-01 | Dang ky thanh cong | Email moi, password hop le | Mo register, nhap thong tin, submit | Tao user moi, password duoc hash, redirect/login message | Functional |
| TC-02 | Dang ky trung email | Email da ton tai | Submit form register | Hien thi loi email da duoc su dung | Negative/Validation |
| TC-03 | Dang nhap sai mat khau | Email dung, password sai | Submit login | Khong dang nhap, hien thong bao loi | Security |
| TC-04 | Reset password token het han | Token qua 30 phut | Mo link reset va submit password | Tu choi reset, yeu cau token moi | Security |
| TC-05 | Tim kiem Da Lat | Keyword "Da Lat", type HOTEL | Goi `/accommodations` | Danh sach accommodation approved lien quan Da Lat | Functional/UI |
| TC-06 | Xem accommodation chua approved | Accommodation status PENDING | Truy cap `/accommodations/{id}` | Khong hien thi cong khai/redirect loi | Authorization |
| TC-07 | Dat phong thanh cong | Room approved, con trong, ngay hop le | Submit booking | Tao booking/payment pending, redirect VNPAY | Functional |
| TC-08 | Dat phong ngay khong hop le | Check-out truoc check-in | Submit booking | Hien loi validate, khong tao booking | Validation |
| TC-09 | Dat qua suc chua | Adults/children vuot capacity | Submit booking | Tu choi dat phong | Business rule |
| TC-10 | Chong overbooking | Room da het trong khoang ngay | Dat tiep cung khoang ngay | Tu choi hoac bao het phong | Business rule |
| TC-11 | Voucher hop le | Code active, dat min order | Goi `/api/voucher/check` | `valid=true`, discount/newTotal dung | API |
| TC-12 | Voucher het han | Code expired | Goi `/api/voucher/check` | `valid=false`, co errorMsg | API/Negative |
| TC-13 | VNPAY signature sai | Params bi sua | Goi return/IPN | Khong approve payment/booking | Security/API |
| TC-14 | VNPAY amount mismatch | Amount khac booking/payment | Goi return/IPN | Tu choi cap nhat thanh toan | Security/API |
| TC-15 | Huy booking cua user khac | User A truy cap booking User B | Submit cancel | Bi tu choi | Authorization/IDOR |
| TC-16 | Review thanh cong | Booking online completed, chua review | Submit rating/comment | Tao review va cap nhat rating accommodation | Functional |
| TC-17 | Review lan 2 | Booking da co review | Submit review nua | Tu choi duplicate review | Negative |
| TC-18 | Partner tao accommodation sai property type | Partner HOTEL tao VILLA | Submit form | Tu choi theo rule partner property type | Business rule |
| TC-19 | Partner thao tac room khong so huu | Partner A sua room Partner B | Submit action | Tu choi truy cap | Authorization |
| TC-20 | Admin approve room | Room PENDING | Admin click approve | Room thanh APPROVED/available theo logic | Admin functional |
| TC-21 | Rut tien khi chua co bank info | Partner wallet co tien nhung thieu bank | Submit withdrawal | Bao loi thieu thong tin ngan hang | Validation |
| TC-22 | Admin travel post URL noi bo | Source URL localhost/127.0.0.1 | Tao/cap nhat post | Tu choi URL khong hop le | Security/Validation |
| TC-23 | Notification chua dang nhap | Guest goi `/api/notifications` | Goi API | Tra 401 hoac bi redirect theo security | API/Security |
| TC-24 | Template render | Load cac template chinh | Chay template tests | Template khong loi binding co ban | UI test |

Co the dua vao bao cao nhu sau:

> Bo test case duoc thiet ke xoay quanh cac luong quan trong cua he thong: dang nhap, tim kiem, dat phong, thanh toan, voucher, phan quyen, review va quan tri. Ngoai cac truong hop thanh cong, can kiem thu cac tinh huong that bai nhu ngay dat phong khong hop le, voucher het han, chu ky VNPAY sai, user truy cap booking cua nguoi khac va doi tac thao tac du lieu khong thuoc quyen so huu. Cac test nay giup chung minh he thong khong chi chay dung o truong hop binh thuong ma con xu ly duoc loi va rui ro bao mat.

## 14. Noi dung bao cao Word chi tiet

### 14.1 Loi mo dau

Co the dua vao bao cao nhu sau:

> Trong boi canh nhu cau du lich va dat cho truc tuyen ngay cang pho bien, cac he thong ho tro tim kiem va dat noi luu tru dong vai tro quan trong trong viec ket noi khach du lich voi cac don vi cung cap dich vu. Do an TravelMate duoc thuc hien nham xay dung mot ung dung web ho tro nguoi dung tim kiem noi luu tru, dat phong, thanh toan truc tuyen va quan ly lich su dat phong. Ben canh do, he thong con cung cap cac cong cu quan ly cho doi tac va quan tri vien, giup mo phong mot quy trinh van hanh gan voi thuc te.

### 14.2 Ly do chon de tai

Co the dua vao bao cao nhu sau:

> De tai TravelMate duoc lua chon vi bai toan dat noi luu tru du lich co tinh thuc tien cao, gan voi cac nghiep vu pho bien trong phat trien phan mem nhu quan ly nguoi dung, tim kiem du lieu, dat lich, thanh toan, phan quyen, danh gia va quan tri noi dung. Thong qua de tai nay, sinh vien co co hoi ap dung kien thuc ve phan tich yeu cau, thiet ke co so du lieu, lap trinh web voi Spring Boot, bao mat ung dung va kiem thu phan mem.

### 14.3 Muc tieu de tai

Co the dua vao bao cao nhu sau:

> Muc tieu cua de tai la xay dung mot ung dung web cho phep nguoi dung tim kiem va dat noi luu tru, dong thoi ho tro doi tac quan ly phong va don dat, quan tri vien kiem duyet va van hanh he thong. He thong can dam bao cac chuc nang cot loi nhu dang ky/dang nhap, tim kiem noi luu tru, dat phong, thanh toan VNPAY, danh gia, voucher, quan ly booking va phan quyen theo vai tro.

### 14.4 Pham vi de tai

Co the dua vao bao cao nhu sau:

> Pham vi cua TravelMate tap trung vao ung dung web server-side rendering dung Spring Boot va Thymeleaf. He thong mo phong quy trinh dat phong/noi luu tru, thanh toan qua VNPAY sandbox, quan ly doi tac va quan tri. De tai chua tap trung vao trien khai production quy mo lon, mobile app rieng, ban do thoi gian thuc, CI/CD hoan chinh hoac he thong goi y AI nang cao. Nhung noi dung nay co the duoc de xuat trong huong phat trien.

### 14.5 Doi tuong su dung

Co the dua vao bao cao nhu sau:

> He thong co bon nhom nguoi dung chinh. Khach vang lai co the xem thong tin va tim kiem noi luu tru. Nguoi dung da dang nhap co the dat phong, thanh toan, quan ly booking va danh gia. Doi tac co the quan ly noi luu tru, phong, booking va doanh thu. Quan tri vien co quyen kiem duyet du lieu, quan ly user, voucher, booking, bai viet, support ticket va cac hoat dong van hanh.

### 14.6 Cong nghe su dung

Co the dua vao bao cao nhu sau:

> TravelMate su dung Java 21, Spring Boot 3.5.x, Spring MVC, Spring Data JPA, Spring Security, Thymeleaf va MySQL. He thong tich hop BCrypt de hash mat khau, VNPAY sandbox cho thanh toan, Spring Mail/EmailService cho luong reset mat khau gui email that khi co SMTP, va Groq-compatible API cho chatbot optional. Qua do, project the hien duoc cach xay dung mot ung dung web full-stack theo huong monolithic MVC voi cac cong nghe pho bien trong he sinh thai Java.

### 14.7 Khao sat hien trang/bai toan

Co the dua vao bao cao nhu sau:

> Trong thuc te, nguoi di du lich thuong can tra cuu noi luu tru theo dia diem, ngay o, so khach, gia phong va danh gia. Doi voi nha cung cap dich vu, nhu cau quan ly phong, tinh trang dat, doanh thu va doi soat cung rat quan trong. TravelMate giai quyet bai toan nay bang cach cung cap mot nen tang trung gian giua nguoi dung va doi tac, dong thoi bo sung vai tro quan tri de kiem soat chat luong du lieu va xu ly van hanh.

### 14.8 Phan tich yeu cau chuc nang

Co the dua vao bao cao nhu sau:

> Cac yeu cau chuc nang cua TravelMate bao gom: quan ly tai khoan, tim kiem noi luu tru, xem chi tiet, dat phong, thanh toan, quan ly booking, danh gia, voucher, chatbot, support ticket, quan ly noi luu tru/phong cho doi tac va quan tri du lieu cho admin. Moi nhom yeu cau duoc gan voi actor cu the va da co bang chung trong cac controller, service, template va entity cua source code.

### 14.9 Phan tich yeu cau phi chuc nang

Co the dua vao bao cao nhu sau:

> Ve phi chuc nang, he thong can dam bao bao mat dang nhap, phan quyen theo vai tro, hieu nang tim kiem chap nhan duoc, giao dien de su dung, code de bao tri va du lieu booking dang tin cay. Source code hien da dap ung mot phan thong qua Spring Security, BCrypt, layered architecture va cac test service. Tuy nhien, he thong can cai thien CSRF, deployment, monitoring va e2e test neu muon dua vao moi truong san pham that.

### 14.10 So do use case

Co the dua vao bao cao nhu sau:

> So do use case nen gom bon actor: Guest, User, Partner va Admin. Guest co use case xem trang chu, tim kiem noi luu tru, xem bai viet, dang ky, dang nhap va lien he. User ke thua cac use case cua Guest va bo sung dat phong, thanh toan, quan ly booking, danh gia, quan ly ho so, su dung voucher va xem notification. Partner co cac use case quan ly accommodation, room, amenity, booking, doanh thu, settlement, wallet, withdrawal va support. Admin co use case duyet accommodation/room, quan ly user, booking, voucher, travel post, support, review, audit log va bao cao doanh thu.

### 14.11 Mo ta use case

Co the dua vao bao cao nhu sau:

> Use case "Dat phong" bat dau khi nguoi dung chon mot phong con trong va nhap thong tin ngay o, so khach, so phong cung thong tin lien he. He thong validate ngay, suc chua, so luong phong va trang thai phong, sau do tinh tong tien, ap dung voucher neu co va tao booking o trang thai cho thanh toan. Neu nguoi dung thanh toan thanh cong qua VNPAY, booking duoc cap nhat sang trang thai da xac nhan; neu thanh toan that bai hoac qua han, he thong huy booking pending va khoi phuc so luong phong.

### 14.12 Phan tich kien truc he thong

Co the dua vao bao cao nhu sau:

> Kien truc cua TravelMate la monolithic MVC. Frontend va backend nam trong cung mot ung dung Spring Boot. Controller nhan request va tra ve view Thymeleaf hoac JSON API. Service chua cac quy tac nghiep vu, repository truy cap database, entity mo ta bang du lieu. Kien truc nay giup project de phat trien va demo trong pham vi do an, dong thoi van du ro rang de mo rong thanh cac module rieng neu can trong tuong lai.

### 14.13 Thiet ke co so du lieu

Co the dua vao bao cao nhu sau:

> Co so du lieu duoc thiet ke quanh cac thuc the User, Accommodation, Room, Booking va Payment. Mot partner co the so huu nhieu accommodation, mot accommodation co nhieu room, mot user co nhieu booking va moi booking gan voi mot room cu the. Booking co cac truong trang thai va snapshot tai chinh de phuc vu thanh toan va doi soat. Cac bang bo sung nhu Voucher, Review, SupportTicket, Notification va PartnerWallet giup hoan thien cac nghiep vu phu tro.

### 14.14 Thiet ke giao dien

Co the dua vao bao cao nhu sau:

> Giao dien nguoi dung duoc thiet ke theo luong tu tim kiem den dat phong: trang chu, danh sach noi luu tru, chi tiet, dat phong, ket qua thanh toan va lich su booking. Giao dien doi tac va admin tap trung vao quan ly du lieu bang bang, form va cac nut thao tac. Do project su dung Thymeleaf, du lieu hien thi tren giao dien duoc render truc tiep tu backend thong qua model.

### 14.15 Cai dat he thong

Co the dua vao bao cao nhu sau:

> He thong duoc cai dat bang Spring Boot voi Maven. Cac package chinh gom controller, service, repository, entity, security va template. Cau hinh ung dung nam trong `application.properties`, ket noi den MySQL database `travelmate_db`. Khi chay project, can khoi tao database bang file seed SQL va cau hinh cac bien moi truong neu muon su dung SMTP, Google OAuth, Groq AI hoac VNPAY sandbox.

### 14.16 Kiem thu

Co the dua vao bao cao nhu sau:

> Project da co cac test cho nhieu service quan trong nhu tinh tien booking, overlap availability, voucher, payment, password reset, review, settlement, wallet, chatbot va render template. Ngoai cac test hien co, bao cao de xuat them cac test truy cap trai phep, test API, test VNPAY edge case va e2e test cho luong dat phong tu giao dien de tang do tin cay truoc khi trien khai thuc te.

### 14.17 Danh gia ket qua dat duoc

Co the dua vao bao cao nhu sau:

> TravelMate da dat duoc muc tieu xay dung mot ung dung web dat noi luu tru co phan quyen theo vai tro. He thong co day du luong nguoi dung, doi tac va quan tri, co thanh toan VNPAY sandbox, co voucher, review, support ticket, dashboard va cac chuc nang tai chinh doi tac. Kien truc code duoc chia thanh cac tang tuong doi ro va co bo test cho nhieu nghiep vu quan trong.

### 14.18 Han che

Co the dua vao bao cao nhu sau:

> Mot so han che hien tai cua project gom: CSRF dang bi tat, chua thay cau hinh Docker/CI-CD, controller admin va partner con lon, validation chua duoc chuan hoa bang DTO, migration database chua dung Flyway/Liquibase, va chua co bang chung ve e2e test trinh duyet. Ngoai ra, chatbot hien nen duoc trinh bay la ho tro rule-based co AI optional, khong phai he thong recommendation nang cao hoan chinh.

### 14.19 Huong phat trien

Co the dua vao bao cao nhu sau:

> Trong tuong lai, TravelMate co the duoc nang cap bang cach bat CSRF protection, bo sung Docker va CI/CD, them unit/integration/e2e test, cai thien logging va monitoring, toi uu database, them ban do, bo loc nang cao, caching, va phat trien he thong goi y lich trinh/noi luu tru dua tren hanh vi nguoi dung. Ngoai ra, co the tach frontend rieng hoac tach cac module payment, booking, notification thanh service rieng neu can mo rong quy mo.

### 14.20 Ket luan

Co the dua vao bao cao nhu sau:

> Do an TravelMate giup van dung nhieu kien thuc cot loi cua nganh Cong nghe phan mem, tu phan tich yeu cau, thiet ke co so du lieu, xay dung ung dung web, bao mat, thanh toan den kiem thu. Mac du con mot so han che can cai thien, project da mo phong duoc nhieu nghiep vu thuc te cua mot nen tang dat noi luu tru va tao nen co so tot de tiep tuc phat trien trong cac giai doan sau.

### 14.21 Tai lieu tham khao

Co the dua vao bao cao nhu sau:

> Tai lieu tham khao nen gom tai lieu chinh thuc cua Spring Boot, Spring Security, Thymeleaf, Spring Data JPA, MySQL, VNPAY Sandbox va cac tai lieu noi bo cua project nhu SRS, ERD, API documentation, source code va file seed database. Khi ghi tai lieu tham khao, can uu tien nguon chinh thuc va ghi ro ngay truy cap neu quy dinh cua truong yeu cau.

## 15. Cau hoi bao ve va cau tra loi goi y

| Cau hoi | Y do cua giang vien | Cau tra loi goi y | Phan code/bang chung lien quan |
| --- | --- | --- | --- |
| Vi sao chon de tai TravelMate? | Kiem tra tinh thuc tien | Em chon vi bai toan dat noi luu tru co nhieu nghiep vu gan thuc te: tim kiem, dat phong, thanh toan, phan quyen, review va quan tri doi tac. | Tong the controller/service/entity |
| He thong co nhung actor nao? | Kiem tra phan tich yeu cau | Co Guest, User, Partner va Admin. Moi actor co tap chuc nang rieng, duoc the hien qua route public, `/profile`, `/partner/**`, `/admin/**`. | `SecurityConfig.java` |
| Project theo kien truc gi? | Kiem tra kien truc | Project theo MVC va layered architecture: controller nhan request, service xu ly nghiep vu, repository truy cap DB, view la Thymeleaf. | `controller`, `service`, `repository`, `templates` |
| Day co phai REST API hoan chinh khong? | Kiem tra kha nang phan biet | Khong hoan toan. Project chu yeu la MVC server-side rendering, chi co mot so REST API cho chatbot, notification, voucher va travel post. | `controller/api` |
| Tai sao dung Thymeleaf thay vi React? | Kiem tra lua chon cong nghe | Vi pham vi do an can demo nhanh ung dung web co backend manh, Thymeleaf giup render truc tiep tu Spring MVC va giam do phuc tap frontend. | `templates` |
| Password duoc luu nhu the nao? | Kiem tra bao mat | Password khong luu plain text ma duoc hash bang BCrypt trong Spring Security/UserService. | `SecurityConfig.java`, `UserService.java` |
| CSRF dang xu ly ra sao? | Kiem tra rui ro bao mat | Hien tai CSRF dang disable, day la han che can cai thien. Neu trien khai thuc te can bat CSRF token cho form POST. | `SecurityConfig.java` |
| Lam sao tranh overbooking? | Kiem tra logic cot loi | Khi tao booking, service validate phong, dung lock room va tinh so booking active overlap theo khoang ngay. Neu so phong con lai khong du thi tu choi booking. | `BookingService.java`, `AvailabilityService.java`, `BookingRepository.java` |
| Dieu kien overlap ngay la gi? | Kiem tra hieu logic | Hai khoang ngay bi overlap khi booking check-in nho hon requested check-out va booking check-out lon hon requested check-in. | Query trong `BookingRepository.java` |
| Booking co nhung trang thai nao? | Kiem tra workflow | Co cac trang thai nhu pending payment, confirmed, checked-in, completed, cancelled/no-show tuy luong; payment co status rieng de tach nghiep vu thanh toan. | `Booking.java`, enum status |
| VNPAY duoc verify nhu the nao? | Kiem tra tich hop payment | He thong tao chu ky HMAC SHA512 khi tao URL va verify chu ky/amount khi nhan return hoac IPN truoc khi approve payment. | `VnpayService.java`, `PaymentService.java` |
| Voucher duoc tinh ra sao? | Kiem tra xu ly gia | Voucher kiem tra active, ngay hieu luc, min order, scope va room assignment; discount co the la percent hoac fixed amount, co max cap. | `VoucherService.java` |
| Vi sao booking luu snapshot tai chinh? | Kiem tra thiet ke database | De khi ty le hoa hong hoac voucher thay doi sau nay, booking cu van giu du lieu doi soat tai thoi diem dat phong. | `Booking.java`, `CommissionService.java` |
| Partner co the tao moi loai accommodation khong? | Kiem tra rule doi tac | Khong. Partner chi duoc tao loai property khop voi `partner_property_type` trong ho so. | `AccommodationService.java`, `User.java` |
| Review co the bi spam khong? | Kiem tra rule review | Review chi duoc tao khi booking online da completed, user so huu booking va moi booking chi co mot review. | `ReviewService.java` |
| Chatbot co phai AI hoan toan khong? | Kiem tra noi qua hay khong | Khong nen noi nhu vay. Chatbot co rule-based intent va goi y dua tren du lieu phong; neu co API key thi co fallback Groq optional. | `ChatbotService.java` |
| Database co quan he n-n nao? | Kiem tra ERD | Room va Amenity co quan he n-n qua `room_amenities`; voucher va room co assignment rieng qua `room_voucher_assignments`. | `Room.java`, `Amenity.java`, SQL |
| Tai sao can settlement va wallet? | Kiem tra nghiep vu doi tac | Settlement tinh tien doi tac theo ky, tru hoa hong/voucher; wallet luu so du co the rut va lich su giao dich. | `SettlementService.java`, `PartnerWalletService.java` |
| Project co test khong? | Kiem tra chat luong | Co nhieu test trong `src/test/java` cho booking, availability, voucher, payment, password reset, review, settlement, wallet, chatbot va template. | `src/test/java` |
| Han che lon nhat cua project la gi? | Kiem tra trung thuc | Theo em, han che lon nhat la CSRF dang tat, chua co deployment/CI-CD ro, mot so controller con lon va validation chua chuan hoa DTO. | `SecurityConfig.java`, cau truc controller |
| Neu co them thoi gian em cai thien gi truoc? | Kiem tra uu tien ky thuat | Em se bat CSRF, bo sung `.env.example`/README, them test phan quyen va e2e cho luong dat phong-thanh toan, sau do tach controller lon. | Security/docs/tests |

Co the dua vao bao cao nhu sau:

> Khi bao ve, can tap trung giai thich duoc cac luong nghiep vu cot loi nhu tim kiem, dat phong, thanh toan, phan quyen va doi soat doi tac. Sinh vien nen tra loi dua tren bang chung trong source code, tranh noi qua muc ve cac tinh nang chua hoan thien. Dac biet, can chuan bi ky cac cau hoi ve overbooking, VNPAY, database relationship, CSRF, password hashing va ranh gioi giua MVC page endpoint voi REST API.

## 16. Rui ro khi bao ve va cach xu ly

| Rui ro | Vi sao giang vien co the hoi sau | Cach xu ly hop le | File/khu vuc lien quan |
| --- | --- | --- | --- |
| CSRF dang disable | Day la loi bao mat pho bien voi form POST | Thua nhan day la han che demo, neu production se bat CSRF token va cap nhat form/API | `SecurityConfig.java` |
| DB demo credential trong properties | De bi hoi ve hardcode secret | Giai thich day la cau hinh local/demo; production can bien moi truong/profile rieng | `application.properties` |
| Controller admin/partner lon | Co the bi danh gia kho bao tri | De xuat tach theo domain: booking, room, settlement, support | `AdminPageController.java`, `PartnerPageController.java` |
| Dung `ddl-auto=update` | Khong phu hop production | Giai thich phu hop dev/demo; huong phat trien la Flyway/Liquibase | `application.properties` |
| Chatbot bi goi la AI qua muc | Giang vien co the hoi thuat toan AI | Noi ro chatbot hien co rule-based va optional Groq; recommendation nang cao la huong phat trien | `ChatbotService.java` |
| Bao cao neu chuc nang khong co trong code | De bi hoi demo khong duoc | Chi dua chuc nang co bang chung; muc chua co dua vao huong phat trien | Toan source |
| Thieu e2e test | Khong chung minh luong UI hoan chinh | Neu trong source chua co, ghi la han che va de xuat Playwright/Selenium | `src/test/java` |
| Payment public endpoints | Co the bi hoi ve gia mao request | Giai thich endpoint public theo mo hinh gateway, nhung co verify signature/amount/idempotency | `PaymentController.java`, `VnpayService.java` |
| Nhieu bang tai chinh phuc tap | De bi hoi kho hieu | Tap trung giai thich gross, commission, voucher deduction, payout, wallet | `SettlementService.java`, `PartnerWalletService.java` |
| Upload file | Co the bi hoi ve file doc hai | Can doc/nam ro `FileStorageService`; neu chua chac, noi can bo sung whitelist va size check | `FileStorageService.java` |
| Validation phan tan | De bi hoi vi sao khong dung DTO | Giai thich hien service la authoritative; huong cai thien la DTO + Bean Validation | service/controller |
| Thieu Docker/CI | Bao cao trien khai neu viet qua manh se bi bat be | Ghi ro chua du can cu, de vao huong phat trien | Root project |

Co the dua vao bao cao nhu sau:

> Cac rui ro khi bao ve chu yeu nam o nhung diem chua san sang production nhu CSRF, cau hinh demo, controller lon, migration database va thieu e2e test. Cach xu ly phu hop la khong che giau cac han che nay, ma trinh bay chung nhu nhung diem da nhan dien duoc va co ke hoach cai thien. Dieu nay the hien sinh vien hieu project va co tu duy ky thuat trung thuc.

## 17. De xuat cai thien

### 17.1 Muc 1: Cai thien nhanh truoc khi nop

| De xuat | Loi ich | Do kho | Co nen lam truoc khi nop khong | File/khu vuc lien quan |
| --- | --- | --- | --- | --- |
| Bo sung README chay project | Giup giang vien/demo de setup | Thap | Nen | `README.md` |
| Tao `.env.example` | Giai thich cac bien SMTP/VNPAY/OAuth/Groq | Thap | Nen | Root/resources |
| Kiem tra khong commit secret that | Giam rui ro bao mat | Thap | Nen | `application.properties` |
| Chup anh giao dien dua vao docs | Bao cao Word ro rang hon | Thap | Nen | `docs/screenshots` |
| Chay lai test va luu ket qua | Chung minh chat luong | Thap | Nen | `src/test/java` |
| Ghi chu han che CSRF | Tranh bi hoi bat ngo | Thap | Nen | Bao cao/security |
| Kiem tra message validate booking/voucher | Demo muot hon | Trung binh | Nen neu con thoi gian | `BookingService.java`, templates |
| Xoa code/template thua neu co | Giam nhieu | Trung binh | Can can trong | Toan project |

### 17.2 Muc 2: Cai thien de bao cao dep hon

| De xuat | Loi ich | Do kho | Co nen lam truoc khi nop khong | File/khu vuc lien quan |
| --- | --- | --- | --- | --- |
| Ve so do kien truc | Giai thich luong controller-service-repository | Thap | Nen | `docs` |
| Ve ERD bang draw.io/PlantUML | Lam ro database | Trung binh | Nen | `docs`, entity/SQL |
| Ve flowchart dat phong-thanh toan | Giai thich nghiep vu cot loi | Trung binh | Nen | Booking/payment services |
| Bo sung bang API | Giup bao ve backend | Thap | Nen | `docs` |
| Bo sung bang test case | The hien quy trinh kiem thu | Thap | Nen | `docs` |
| Bo sung screenshot dashboard admin/partner | Bao cao truc quan | Thap | Nen | `docs/screenshots` |
| Viet phan rui ro va huong phat trien trung thuc | Tang diem thuyet phuc | Thap | Nen | Bao cao Word |

### 17.3 Muc 3: Cai thien nang cao

| De xuat | Loi ich | Do kho | Co nen lam truoc khi nop khong | File/khu vuc lien quan |
| --- | --- | --- | --- | --- |
| Bat CSRF va cap nhat form/API | Tang bao mat thuc te | Trung binh/Cao | Nen neu co thoi gian test | `SecurityConfig.java`, templates |
| Them Docker Compose | Setup DB/app nhanh hon | Trung binh | Co neu can demo may khac | Root project |
| Them CI/CD | Tu dong chay test | Trung binh | Khong bat buoc | `.github/workflows` |
| Dung Flyway/Liquibase | Quan ly schema chuan hon | Trung binh | Khong bat buoc | `resources/db/migration` |
| Them e2e test booking-payment | Chung minh luong UI | Cao | Lam neu co thoi gian | Test/e2e |
| Tach controller lon | Tang maintainability | Trung binh/Cao | Sau khi nop neu rui ro lon | `AdminPageController.java`, `PartnerPageController.java` |
| Them rate limiting | Giam brute force/chatbot abuse | Trung binh | Huong phat trien | Security/filter |
| Cai thien recommendation | Goi y dua tren hanh vi/lich su | Cao | Huong phat trien | `ChatbotService.java`, recommendation module |
| Tich hop ban do | Nang UX du lich | Trung binh/Cao | Huong phat trien | Accommodation UI/API |
| Cache diem den/noi luu tru pho bien | Tang hieu nang | Trung binh | Huong phat trien | Service/cache |

Co the dua vao bao cao nhu sau:

> Cac de xuat cai thien duoc chia thanh ba muc. Muc ngan han tap trung vao tai lieu, anh giao dien, cau hinh va test de dam bao san sang nop bai. Muc trung han tap trung vao so do, ERD, flowchart va bang API/test case de bao cao ro rang hon. Muc nang cao tap trung vao bao mat, deployment, CI/CD, migration database, e2e test, cache va recommendation de dua project gan hon voi moi truong san pham.

## 18. Checklist truoc khi nop

### 18.1 Checklist source code

- [ ] Chay thanh cong ung dung tren may local.
- [ ] Chay test bang Maven va luu lai ket qua.
- [ ] Kiem tra account demo trong SQL dang nhap duoc.
- [ ] Kiem tra luong tim kiem -> chi tiet -> dat phong -> VNPAY sandbox -> my booking.
- [ ] Kiem tra luong admin approve accommodation/room.
- [ ] Kiem tra luong partner tao accommodation/room va quan ly booking.
- [ ] Kiem tra khong co secret production hardcode.
- [ ] Kiem tra file upload/avatar/room image neu demo.
- [ ] Kiem tra khong co exception tren console khi demo cac trang chinh.

### 18.2 Checklist bao cao Word

- [ ] Co loi mo dau, ly do chon de tai, muc tieu, pham vi.
- [ ] Co bang actor/use case va yeu cau chuc nang.
- [ ] Co yeu cau phi chuc nang.
- [ ] Co so do use case.
- [ ] Co so do kien truc controller-service-repository-database.
- [ ] Co ERD va mo ta quan he bang.
- [ ] Co mo ta API/backend.
- [ ] Co anh giao dien cac man hinh chinh.
- [ ] Co bang test case.
- [ ] Co phan han che va huong phat trien trung thuc.
- [ ] Tat ca noi dung bam source code, khong dua chuc nang chua co vao ket qua dat duoc.

### 18.3 Checklist slide thuyet trinh

- [ ] Slide 1: ten de tai, thanh vien, giang vien huong dan.
- [ ] Slide 2: ly do chon de tai va bai toan.
- [ ] Slide 3: actor va chuc nang chinh.
- [ ] Slide 4: kien truc he thong.
- [ ] Slide 5: database/ERD rut gon.
- [ ] Slide 6: luong dat phong va thanh toan.
- [ ] Slide 7: demo giao dien nguoi dung.
- [ ] Slide 8: demo giao dien partner/admin.
- [ ] Slide 9: kiem thu va danh gia.
- [ ] Slide 10: han che, huong phat trien, ket luan.

### 18.4 Checklist demo

- [ ] Mo san MySQL va ung dung Spring Boot.
- [ ] Dang nhap bang user demo.
- [ ] Tim kiem mot diem den co du lieu.
- [ ] Dat mot phong con trong.
- [ ] Kiem tra voucher hop le/khong hop le.
- [ ] Chuyen sang VNPAY sandbox va quay ve ket qua.
- [ ] Xem booking trong my bookings.
- [ ] Dang nhap partner de quan ly booking/phong.
- [ ] Dang nhap admin de duyet hoac xem dashboard.
- [ ] Chuan bi san phuong an neu VNPAY/SMTP/OAuth khong co internet/cau hinh.

### 18.5 Checklist bao ve van dap

- [ ] Nam duoc kien truc MVC/layered.
- [ ] Giai thich duoc quan he User-Accommodation-Room-Booking-Payment.
- [ ] Giai thich duoc cach chong overbooking.
- [ ] Giai thich duoc cach tinh tong tien/deposit/voucher.
- [ ] Giai thich duoc VNPAY signature va trang thai payment.
- [ ] Giai thich duoc BCrypt va phan quyen role.
- [ ] Thua nhan va de xuat cai thien cho CSRF.
- [ ] Biet vi tri file controller/service/entity quan trong.
- [ ] Biet test nao da co va test nao can bo sung.

### 18.6 Checklist tai lieu bo sung

- [ ] README setup.
- [ ] `.env.example`.
- [ ] API table.
- [ ] ERD.
- [ ] Use case diagram.
- [ ] Flowchart booking-payment.
- [ ] Screenshot UI.
- [ ] Test report.
- [ ] Danh sach account demo.
- [ ] Danh sach han che va huong phat trien.

Co the dua vao bao cao nhu sau:

> Truoc khi nop bai, can kiem tra dong bo ca source code, bao cao, slide va demo. Trong do, quan trong nhat la dam bao luong demo chinh chay on dinh, cac so do trong bao cao khop voi source code, va sinh vien co the giai thich duoc cac quyet dinh ky thuat quan trong nhu kien truc MVC, thiet ke database, xu ly booking, thanh toan, voucher va bao mat.
