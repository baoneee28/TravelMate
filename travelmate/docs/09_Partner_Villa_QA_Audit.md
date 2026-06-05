# TravelMate - Partner VILLA QA Audit

**Ngay cap nhat:** 2026-06-02
**Pham vi:** Partner VILLA, user flow dat Villa, guard backend, seed SQL, test report

> Tai lieu nay dinh chinh ban phan bien cu co so va ket luan "hoan hao 100%". Theo source hien tai trong workspace, ket qua dung la **279/279 tests pass**. Khong nen ghi so test cu hoac 100% tuyet doi trong bao cao nop, vi se lech voi Maven/Surefire.

---

## 1. Ket Luan Nhanh

| Hang muc | Trang thai dung hien tai | Ghi chu QA |
|---|---|---|
| Maven test suite | **279/279 PASS** | Xac nhan tu Maven/Surefire ngay 2026-06-05 |
| Guard owner + `partnerPropertyType` | **Da phu cac luong chinh Partner** | Booking, direct booking, manual block, voucher, room/amenity, support prefill |
| Partner VILLA wording | **Da polish manh** | Partner side dung "can", "dat can", "giu can", "chan can", "tien nghi can" |
| User VILLA wording | **Da doi cac diem de thay** | Detail/booking/mybooking dung "can" o cac nhan chinh; cac text khong render cho Villa hoac noi dung hospitality chung co the con "phong" |
| SQL seed Villa | **Da sach hon va dung owner** | Villa thuoc `partner3@travelmate.vn`, co online completed, auto-held confirmed booking, checked-in, no-show demo, direct booking, manual block |
| Bao cao test cu | **Sai voi source hien tai** | Phai dung 279/279 neu nop/bao cao tu workspace nay |

---

## 2. Dinh Chinh Cac Claim Trong Prompt Cu

| Claim cu | Dung/Sai | Dinh chinh nen dung |
|---|---:|---|
| Claim so test cu | Sai | Hien tai la **279/279 pass**, 0 fail, 0 error, 0 skipped |
| "Security Guard Rails HOAN HAO 100%" | Qua manh | Nen ghi: **Da bo sung guard owner + property type cho cac luong Partner trong pham vi audit** |
| "User side co tinh giu nguyen Nhan phong/Tra phong" | Khong con dung | User side da doi nhieu nhan chinh sang **Nhan can/Tra can/Dat can/Het can** khi `isVilla=true` |
| "SQL seed hoan toan sach 100%" | Qua manh | Nen ghi: **Da ra soat va don cac dong Villa de gay hieu nham owner/comment** |
| "File ZIP thieu wallet la do AI doc sai" | Co the dung voi snapshot cu | Neu bao cao chinh thuc, chi nen noi: source hien tai co day du file wallet/withdrawal/export |

---

## 3. Minh Chung Guard Backend

### AccommodationService

- `canManageAccommodation(...)` tra ve true khi:
  - accommodation co owner;
  - owner id trung partner id;
  - `partner.partnerPropertyType` khong null;
  - `accommodation.propertyType == partner.partnerPropertyType`.
- `updateRoomAmenities(...)` goi `validateManagedAccommodation(...)`.
- `createRoom(...)` goi `validateManagedAccommodation(...)` sau khi lay accommodation theo owner.

### BookingService

- `confirmBookingHoldByPartner(...)` da goi `validatePartnerOwnership(...)`.
- `validatePartnerOwnership(...)` goi tiep `validatePartnerCanManageAccommodation(...)`.
- `createDirectBooking(...)` va `blockRoom(...)` goi `validatePartnerCanManageAccommodation(...)` ngay dau luong.
- `getAllBookingsForPartner(...)` loc lai bang `isPartnerManagedAccommodation(...)` de tranh lo booking sai `propertyType` neu du lieu bi gan nham.

### VoucherService

- `createPartnerVoucherForAccommodation(...)` goi `validatePartnerCanManageAccommodation(...)`.
- `createPartnerVoucherForRoom(...)` goi `validatePartnerCanManageAccommodation(...)`.
- `toggleActiveForPartner(...)` cung kiem tra voucher thuoc accommodation/room dung owner va dung `partnerPropertyType`.

### PartnerPageController

- `newRoomForm` dung `accommodationService.canManageAccommodation(...)`.
- `bookingDetail` dung `accommodationService.canManageAccommodation(...)`.
- support prefill "Yeu cau chinh sua/ngung ban" chi dien san neu partner quan ly dung accommodation.

---

## 4. Wording Villa Da Fix

Partner VILLA hien thi theo huong:

- `Danh sach Villa cua ban`
- `Them can`
- `Tien nghi can villa`
- `Don dat can`
- `Can kiem tra giu can` cho du lieu legacy/ngoai le
- `Da giu can`
- `Chan can` / `Chan ban noi bo`
- `Tinh trang can hom nay`
- `Theo can` trong revenue/voucher

User side khi xem/dang dat Villa da doi cac nhan de thay:

- `Chon can`, `Kiem tra can`, `Het can`
- `Ngay nhan can`, `Ngay tra can`
- `So can muon dat`
- `Dat can`
- `Lich dat can`
- `Chinh sach huy can`
- `Hoan tat thanh toan de giu can`

Ghi chu: Trong `hotel-detail.html`, mot so chu "phong" van con trong cac block Hotel/Homestay/Resort hoac noi dung mo ta tien nghi chung. Neu block do khong render cho Villa thi khong tinh la loi demo Villa.

---

## 5. SQL Seed Villa

Tai khoan demo Villa:

```text
partner3@travelmate.vn / partner123
Green Hills Villa
partnerPropertyType = VILLA
```

Du lieu Villa chinh:

| Accommodation | Owner | Muc dich demo |
|---|---|---|
| The Anam Villa Nha Trang | partner3 | Online booking, voucher ANAM15, calendar availability, no-show demo |
| Ba Na Hills Forest Villa | partner3 | Checked-in/completed booking, direct booking, manual block |

Trang thai demo nen co:

- Online completed de tinh revenue/settlement.
- Auto-held confirmed booking de demo "check-in can" sau khi TravelMate da giu can.
- Checked-in de demo "check-out/hoan tat".
- Deposit 30% no-show de demo giu coc va mo lai quota.
- Direct booking khong tinh settlement.
- Manual block/chan ban noi bo khong tinh settlement.

---

## 6. Ket Qua Test

Lenh da chay:

```bash
./mvnw.cmd test
```

Ket qua:

```text
Tests run: 279, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Bao cao da dong bo:

- `docs/07_Test_Report.md`
- `travelmate_test_report.md`
- `README_DEMO.md`
- ban copy o root `E:\TravelMate\travelmate_test_report.md`
- ban copy o root `E:\TravelMate\README_DEMO.md`

---

## 7. Ket Qua Checklist VILLA 2026-05-25 12:14

| Nhom test | Trang thai | Ghi chu |
|---|---:|---|
| Maven test | PASS | Surefire reports xanh 279/279 |
| `mvn clean test` | BLOCKED moi truong | Windows/Java IDE dang giu file `target/classes/application.properties`; khong phai loi code |
| Partner3 la VILLA | PASS | `travelmate_db.sql` gan `partner3@travelmate.vn` = `PARTNER`, `VILLA`, `ACTIVE` |
| Villa thuoc partner3 | PASS | The Anam Villa va Ba Na Hills Forest Villa owner_id = partner3 |
| Booking demo Villa | PASS | Co online completed, auto-held confirmed booking, checked-in, direct booking, manual block, no-show/deposit demo |
| Guard owner + property type | PASS | Booking, room/amenity, direct booking, manual block, voucher deu co guard BE |
| Partner VILLA main UI wording | PASS | Dashboard, accommodations, amenities, bookings, room-status, availability, direct booking, revenue, settlements, wallet, vouchers |
| Partner booking detail wording | PASS | Da polish them trang `/partner/bookings/{id}` de hien `can/giu can/chan can/mo lai can` cho Villa |
| User VILLA visible wording | PASS co pham vi | Cac nhan chinh detail/booking/mybooking da doi dong theo Villa; text hospitality chung co the giu trung tinh |
| Revenue/settlement/wallet rules | PASS by code/test | Direct booking/manual block khong tinh settlement; wallet/withdrawal co test integration |
| Responsive UI | CAN TEST THU CONG | Can mo app voi MySQL de xem Chrome 100%/80%; chua chay browser trong lan audit nay |

Ket luan: Partner VILLA du dieu kien chuyen sang Partner Resort sau khi chay them 1 vong click UI tren app that neu can chup minh chung demo.

---

## 8. Cach Tra Loi Khi Bao Ve Do An

**Hoi:** Tai sao Villa co quota > 1?

**Tra loi:** Villa duoc quan ly theo nhom can cung cau hinh. Vi du mot khu co 3 can `Garden Pool Villa` giong nhau ve gia, suc chua va tien nghi thi he thong khai bao 1 dong san pham voi quota 3. Khi co booking, TravelMate tru quota theo overlap ngay de tranh overbooking.

**Hoi:** Direct booking va manual block co tinh doanh thu khong?

**Tra loi:** Khong. Direct booking la khach dat/tra tien truc tiep tai co so, TravelMate chi giup giu quota de tranh ban trung. Manual block la giu can noi bo/bao tri, khong phat sinh dong tien online, khong commission va khong settlement.

**Hoi:** Vi Partner co phai vi ngan hang that khong?

**Tra loi:** Khong. Day la vi quyet toan noi bo. Khi Admin xac nhan da chuyen khoan ben ngoai, he thong cap nhat trang thai rut tien va lich su giao dich trong TravelMate.

**Hoi:** Hoa hong Villa 12% co tinh dong khong?

**Tra loi:** Co. He thong co default commission theo loai luu tru, Villa mac dinh 12%. Neu room/can co `commission_rate_override`, settlement uu tien override; neu khong co thi dung default theo property type.
