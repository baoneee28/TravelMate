# Bao cao ra soat TravelMate - Codex

Ngay ra soat: 27/05/2026

## Ket luan nhanh

Trang thai hien tai: PASS cho cac nhom core demo da ban voi ben ra soat.

- Full test suite: 234 tests, 0 failures, 0 errors, 0 skipped.
- Browser smoke test: PASS tren `http://localhost:18080` cho login, news filter empty-state, listing mac dinh Da Lat.
- Khong con app test chay nen sau khi kiem tra: `NO_JAVA_PROCESS`, `LOCAL_18080_HTTP=000`.
- Grep cac cum loi thoi theo checklist da sach trong source/test/doc demo chinh.
  Bao cao nay khong copy nguyen van cac cum do de tranh bi grep false positive khi gui di kiem tra.

## Nhung phan da bo sung trong lan cuoi

### 1. Google Login that, nhung an toan cho demo

Da them `spring-boot-starter-oauth2-client` vao `pom.xml`.

Luon co code OAuth2 that, nhung mac dinh tat de may khac khong co Google Client ID/Secret van chay binh thuong:

- `travelmate.oauth2.google.enabled=${GOOGLE_OAUTH_ENABLED:false}`
- `GoogleOAuthClientConfig` chi tao client Google khi co du `GOOGLE_OAUTH_ENABLED=true`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- Khi thieu bat ky gia tri nao, app van khoi dong va nut Google giu trang thai disabled
- Khi muon demo that: dat ba bien moi truong tren, khong can bat profile rieng
- Redirect URI Google: `http://localhost:8080/login/oauth2/code/google`

Security rule:

- Google Login chi tao/dang nhap tai khoan `USER`
- Neu email Google trung `ADMIN` hoac `PARTNER`, he thong chan va redirect ve login voi `oauth2Error=role`
- Tai khoan USER bi khoa/khong ACTIVE bi chan voi `oauth2Error=inactive`
- Password random cua user Google duoc BCrypt encode
- Avatar Google duoc luu vao `avatarUrl` neu co

File chinh:

- `src/main/java/com/travelmate/security/OAuth2LoginSuccessHandler.java`
- `src/main/java/com/travelmate/security/GoogleOAuthClientConfig.java`
- `src/main/java/com/travelmate/security/SecurityConfig.java`
- `src/main/java/com/travelmate/controller/page/AuthPageController.java`
- `src/main/resources/templates/auth/login.html`
- `src/main/resources/templates/auth/register.html`
- `src/main/resources/application.properties`

Test moi:

- `src/test/java/com/travelmate/security/OAuth2LoginSuccessHandlerTest.java`

### 2. I18N header va man hinh xac thuc khach hang

Browser da phat hien loi header hien placeholder i18n tren news/listing.

Da sua bang cach them bundle mac dinh:

- `src/main/resources/messages.properties`
- `src/main/resources/messages_vi.properties`
- `src/main/resources/messages_en.properties`
- `src/main/resources/templates/auth/login.html`
- `src/main/resources/templates/auth/register.html`
- `src/main/resources/templates/auth/forgot-password.html`
- `src/main/resources/templates/auth/reset-password.html`

Ket qua browser re-test:

- News/listing khong con placeholder i18n
- Header hien dung tieng Viet: `CHUC NANG`, `TRANG CHU`, `TIN TUC`, `DANG NHAP`
- Cac man hinh login, dang ky, quen mat khau va dat lai mat khau render duoc bang `?lang=en`
- Thong bao redirect cua dang ky va quen/dat lai mat khau lay theo ngon ngu dang chon
- Khong bi tran ngang layout

Pham vi da xac minh la header va nhom auth customer-facing; khong tuyen bo toan bo trang Admin/Partner da duoc dich.

### 3. Excel doi soat dung streaming

`ExcelExportService` da chuyen tu `XSSFWorkbook` sang `SXSSFWorkbook`:

- Giam rui ro ton RAM khi export nhieu dong
- Co `trackAllColumnsForAutoSizing()` de van auto-size cot duoc
- Co `dispose()` de don temp file sau khi ghi

File chinh:

- `src/main/java/com/travelmate/service/ExcelExportService.java`

## Doi soat cac nhom nghiep vu loi

### Payment / cancel / refund / deposit

PASS.

Bang chung code/test:

- `DEPOSIT_30` huy/no-show -> `PaymentStatus.DEPOSIT_FORFEITED`
- `FULL_PAYMENT` huy -> `PaymentStatus.REFUND_PENDING`, refund 70%, cancellation fee 30%
- UI my-bookings hien dung wording mat coc / hoan 70%
- Test lien quan nam trong `BookingCalculationTest`, `NoShowDepositTest`, `PaymentServiceTest`

### Revenue / settlement / commission snapshot

PASS.

Bang chung code/test:

- `Booking` co snapshot commission: `commissionRateSnapshot`, `commissionSourceSnapshot`, `commissionAmountSnapshot`
- Revenue/Settlement dung rate snapshot, nhung commission base luon la khoan TravelMate da thu online
- `DEPOSIT_30` hoan tat hoac mat coc: chi tinh hoa hong tren 30% coc online, khong tinh 70% thu tai co so
- Du lieu coc cu bi thieu so tien tai co so duoc `DataInitializer` khoi phuc tu `total_amount - paid_amount`; don huy/no-show van ghi nhan tai co so bang 0
- Settlement `PENDING` cu tu dong dong bo lai tong tien theo breakdown truoc khi chi tra
- `DEPOSIT_FORFEITED` duoc dua vao settlement khi booking `CANCELLED` hoac `NO_SHOW`
- Test lien quan: `SettlementServiceTest`, `SettlementEligibilityTest`, `RevenueServiceHomestayTest`, `CommissionServiceTest`

### Voucher scope / cost bearer / partner visibility

PASS.

Bang chung code/test:

- Voucher co `costBearer`
- Partner-room voucher co guard theo loai luu tru va assignment
- Partner khong tu tao voucher
- Settlement chi tru voucher `PARTNER`, khong tru voucher `ADMIN`
- Test lien quan: `VoucherCalculationTest`

### Chatbot search that tu DB

PASS theo test hien co.

Bang chung code/test:

- Chatbot dung `AvailabilityService.checkAvailabilityForAccommodation`
- Link tra ve co `#room-{roomId}`
- Co fallback khi Groq API loi qua `RestClientException`
- Test lien quan: `ChatbotServiceTest`

### Search / news / footer / default Da Lat

PASS.

Bang chung code/browser:

- Search rong default Da Lat
- Alias destination dung `DestinationAliasUtil`
- News filter co empty-state, khong vo layout
- Browser screenshot da luu:
  - `target/browser-news-empty-fixed.png`
  - `target/browser-listing-dalat-fixed.png`

### Room detail / image / stop-selling

PASS theo code/test.

Bang chung:

- User detail co modal `room-detail-modal`
- Hash `#room-{id}` scroll/mo modal
- Partner co route toggle ban phong: `/partner/rooms/{roomId}/toggle-selling`
- `availableForBooking=false` khong doi quota goc, chi dung ngung ban online
- Test lien quan: `UserPortalFlowTemplateTest`, `AccommodationServiceSearchTest`, `AvailabilityServicePartnerTypeGuardTest`

### Phan bien ha tang / hoc thuat

PASS ve mat co so code de tra loi.

- VNPAY cham/sap: `BookingExpiryScheduler` xu ly `PENDING_PAYMENT` het han
- AI mat mang: `ChatbotService` bat `RestClientException` va fallback
- SMTP chua tich hop trong ban local: Forgot password chi ghi reset link vao console/log, khong hien link tren UI
- Overbooking: `RoomRepository` dung `PESSIMISTIC_WRITE`
- N+1 admin booking: `BookingRepository.findAllForAdminPage()` dung `JOIN FETCH`
- Excel lon: da chuyen sang `SXSSFWorkbook`

## Lenh kiem tra da chay

```bash
./mvnw.cmd test
```

Ket qua:

```text
Tests run: 234, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Browser smoke test:

```text
login: PASS, Google button disabled khi chua cau hinh OAuth, khong tran ngang
news-empty: PASS, co empty-state, header i18n dung, khong tran ngang
listing-dalat: PASS, search rong hien Da Lat, header i18n dung, khong tran ngang
```

## Luu y khi demo Google Login that

Neu muon bam Google Login that tren may bao ve:

1. Tao Google OAuth Client voi redirect URI:
   `http://localhost:8080/login/oauth2/code/google`
2. Dat bien moi truong:
   - `GOOGLE_OAUTH_ENABLED=true`
   - `GOOGLE_CLIENT_ID=...`
   - `GOOGLE_CLIENT_SECRET=...`
3. Chay app lai.

Neu khong dat du cac bien nay, nut Google hien disabled va app van khoi dong. Day la co chu dich de demo core booking/settlement khong bi crash tren may khac.
