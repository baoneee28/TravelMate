# TravelMate - Huong Dan Chay Demo Nhanh

## 1. Yeu cau moi truong

- Java 17+
- MySQL 8.x
- Chrome/Edge de test VNPAY Sandbox

## 2. Tao database demo

Tao database `travelmate_db`, sau do import:

```sql
src/main/resources/travelmate_db.sql
src/main/resources/fix_seed_amenities.sql
```

## 3. Cau hinh local

Mac dinh app dung MySQL local:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/travelmate_db
spring.datasource.username=root
spring.datasource.password=root
```

Neu may demo dung mat khau khac, sua trong `src/main/resources/application.properties` hoac cau hinh bien moi truong rieng.

Khong nop kem file `.env.local` vi file nay co the chua secret local.

## 4. Chay build va test

```powershell
mvnw.cmd clean test
```

Neu da cai Maven global:

```powershell
mvn clean test
```

## 5. Chay app

```powershell
mvnw.cmd spring-boot:run
```

Mo trinh duyet:

```text
http://localhost:8080
```

## 6. Route can kiem tra truoc buoi bao ve

- `/`
- `/auth/login`
- `/accommodations`
- `/vouchers`
- `/my-bookings`
- `/partner/dashboard`
- `/partner/bookings`
- `/admin/dashboard`
- `/admin/bookings`
- `/admin/revenue`

## 7. Flow regression can test nhanh

- User dat phong `FULL_PAYMENT`.
- User dat phong `DEPOSIT_30`.
- VNPAY success/fail/cancel.
- Partner check-in don coc va xac nhan thu 70% con lai.
- Partner check-out.
- User review sau khi booking `COMPLETED`.
- Admin xem revenue/settlement.
- Voucher Partner 10% dung duoc voi coc 30%.
- Voucher Partner 15% bi chan voi coc 30%.
- Rebook don `CANCELLED` hoac `NO_SHOW`.
