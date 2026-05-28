package com.travelmate.config;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.entity.PartnerSettlement;
import com.travelmate.entity.enums.RoomCategory;
import com.travelmate.repository.AccommodationRepository;
import com.travelmate.repository.PartnerSettlementRepository;
import com.travelmate.repository.RoomRepository;
import com.travelmate.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DataInitializer — Khởi tạo dữ liệu mẫu khi app khởi động.
 *
 * Rule:
 *   - partner1 (HOTEL)    → sở hữu Hotel
 *   - partner2 (RESORT)   → sở hữu Resort
 *   - partner3 (VILLA)    → sở hữu Villa
 *   - partner4 (HOMESTAY) → sở hữu Homestay
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initDefaultData(UserRepository userRepository,
                                          AccommodationRepository accommodationRepository,
                                          RoomRepository roomRepository,
                                          PartnerSettlementRepository settlementRepository,
                                          JdbcTemplate jdbcTemplate,
                                          PasswordEncoder passwordEncoder) {
        return args -> {
            cleanupLegacySeedWalletData(jdbcTemplate);
            deactivateLegacyPartnerIssuedVouchers(jdbcTemplate);
            deduplicateAmenities(jdbcTemplate);
            ensureDatabaseConstraints(jdbcTemplate);
            normalizeLegacyPaymentMethods(jdbcTemplate);
            normalizeVisibleSeedLabels(jdbcTemplate);
            normalizeOnlineDepositCommissionSnapshots(jdbcTemplate);

            // ── ADMIN ────────────────────────────────────────────────────────
            var adminOpt = userRepository.findByEmail("admin@travelmate.vn");
            if (adminOpt.isEmpty()) {
                User admin = new User();
                admin.setEmail("admin@travelmate.vn");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setName("Admin TravelMate");
                admin.setShortName("Admin");
                admin.setPhone("0901 234 567");
                admin.setRole(User.Role.ADMIN);
                admin.setStatus("ACTIVE");
                userRepository.save(admin);
            } else {
                // ❗ Chỉ cập nhật role để đảm bảo quyền admin, KHÔNG reset status/password
                User admin = adminOpt.get();
                admin.setRole(User.Role.ADMIN);
                // Giữ nguyên status — admin không nên bị reset ACTIVE mỗi khi restart
                userRepository.save(admin);
            }

            // ── USER ─────────────────────────────────────────────────────────
            var userOpt = userRepository.findByEmail("user@travelmate.vn");
            if (userOpt.isEmpty()) {
                User user = new User();
                user.setEmail("user@travelmate.vn");
                user.setPassword(passwordEncoder.encode("user123"));
                user.setName("Nguyễn Văn An");
                user.setShortName("An");
                user.setPhone("0912 345 678");
                user.setRole(User.Role.USER);
                user.setStatus("ACTIVE");
                userRepository.save(user);
            }
            // ❗ Không cập nhật user đã tồn tại — giữ nguyên status (tránh reset sau khi admin khóa)

            // ── PARTNER 1 — HOTEL ────────────────────────────────────────────
            User partnerHotel;
            var p1Opt = userRepository.findByEmail("partner@travelmate.vn");
            if (p1Opt.isEmpty()) {
                partnerHotel = new User();
                partnerHotel.setEmail("partner@travelmate.vn");
                partnerHotel.setPassword(passwordEncoder.encode("partner123"));
                partnerHotel.setName("Sunrise Sapa Lodge");
                partnerHotel.setShortName("Sunrise");
                partnerHotel.setPhone("0933 456 789");
                partnerHotel.setRole(User.Role.PARTNER);
                partnerHotel.setStatus("ACTIVE");
                partnerHotel.setPartnerPropertyType(PropertyType.HOTEL);
                partnerHotel.setBankAccountNumber("0123456789");
                partnerHotel.setBankName("MB Bank");
                partnerHotel.setBankAccountHolder("NGUYEN VAN A");
                partnerHotel.setBankBranch("TP.HCM");
                partnerHotel = userRepository.save(partnerHotel);
            } else {
                // Chỉ đảm bảo role + bank info, KHÔNG reset status
                partnerHotel = p1Opt.get();
                partnerHotel.setRole(User.Role.PARTNER);
                partnerHotel.setPartnerPropertyType(PropertyType.HOTEL);
                if (partnerHotel.getBankAccountNumber() == null) {
                    partnerHotel.setBankAccountNumber("0123456789");
                    partnerHotel.setBankName("MB Bank");
                    partnerHotel.setBankAccountHolder("NGUYEN VAN A");
                    partnerHotel.setBankBranch("TP.HCM");
                }
                partnerHotel = userRepository.save(partnerHotel);
            }

            // ── PARTNER 2 — RESORT ───────────────────────────────────────────
            User partnerResort;
            var p2Opt = userRepository.findByEmail("partner2@travelmate.vn");
            if (p2Opt.isEmpty()) {
                partnerResort = new User();
                partnerResort.setEmail("partner2@travelmate.vn");
                partnerResort.setPassword(passwordEncoder.encode("partner123"));
                partnerResort.setName("Blue Ocean Resort");
                partnerResort.setShortName("BlueOcean");
                partnerResort.setPhone("0944 567 890");
                partnerResort.setRole(User.Role.PARTNER);
                partnerResort.setStatus("ACTIVE");
                partnerResort.setPartnerPropertyType(PropertyType.RESORT);
                partnerResort.setBankAccountNumber("9876543210");
                partnerResort.setBankName("Vietcombank");
                partnerResort.setBankAccountHolder("TRAN THI B");
                partnerResort.setBankBranch("Đà Nẵng");
                partnerResort = userRepository.save(partnerResort);
            } else {
                // Chỉ đảm bảo role + bank info, KHÔNG reset status
                partnerResort = p2Opt.get();
                partnerResort.setRole(User.Role.PARTNER);
                partnerResort.setPartnerPropertyType(PropertyType.RESORT);
                if (partnerResort.getBankAccountNumber() == null) {
                    partnerResort.setBankAccountNumber("9876543210");
                    partnerResort.setBankName("Vietcombank");
                    partnerResort.setBankAccountHolder("TRAN THI B");
                    partnerResort.setBankBranch("Đà Nẵng");
                }
                partnerResort = userRepository.save(partnerResort);
            }

            // ── PARTNER 3 — VILLA ────────────────────────────────────────────
            User partnerVilla;
            var p3Opt = userRepository.findByEmail("partner3@travelmate.vn");
            if (p3Opt.isEmpty()) {
                partnerVilla = new User();
                partnerVilla.setEmail("partner3@travelmate.vn");
                partnerVilla.setPassword(passwordEncoder.encode("partner123"));
                partnerVilla.setName("Green Hills Villa");
                partnerVilla.setShortName("GreenHills");
                partnerVilla.setPhone("0955 678 901");
                partnerVilla.setRole(User.Role.PARTNER);
                partnerVilla.setStatus("ACTIVE");
                partnerVilla.setPartnerPropertyType(PropertyType.VILLA);
                partnerVilla.setBankAccountNumber("1122334455");
                partnerVilla.setBankName("Techcombank");
                partnerVilla.setBankAccountHolder("LE VAN C");
                partnerVilla.setBankBranch("Hà Nội");
                partnerVilla = userRepository.save(partnerVilla);
            } else {
                // Chỉ đảm bảo role + bank info, KHÔNG reset status
                partnerVilla = p3Opt.get();
                partnerVilla.setRole(User.Role.PARTNER);
                partnerVilla.setPartnerPropertyType(PropertyType.VILLA);
                if (partnerVilla.getBankAccountNumber() == null) {
                    partnerVilla.setBankAccountNumber("1122334455");
                    partnerVilla.setBankName("Techcombank");
                    partnerVilla.setBankAccountHolder("LE VAN C");
                    partnerVilla.setBankBranch("Hà Nội");
                }
                partnerVilla = userRepository.save(partnerVilla);
            }

            // ── PARTNER 4 — HOMESTAY ─────────────────────────────────────────
            User partnerHomestay;
            var p4Opt = userRepository.findByEmail("partner4@travelmate.vn");
            if (p4Opt.isEmpty()) {
                partnerHomestay = new User();
                partnerHomestay.setEmail("partner4@travelmate.vn");
                partnerHomestay.setPassword(passwordEncoder.encode("partner123"));
                partnerHomestay.setName("Mekong Homestay");
                partnerHomestay.setShortName("Mekong");
                partnerHomestay.setPhone("0966 789 012");
                partnerHomestay.setRole(User.Role.PARTNER);
                partnerHomestay.setStatus("ACTIVE");
                partnerHomestay.setPartnerPropertyType(PropertyType.HOMESTAY);
                partnerHomestay.setBankAccountNumber("5544332211");
                partnerHomestay.setBankName("Agribank");
                partnerHomestay.setBankAccountHolder("PHAM THI D");
                partnerHomestay.setBankBranch("Cần Thơ");
                partnerHomestay = userRepository.save(partnerHomestay);
            } else {
                // Chỉ đảm bảo role + bank info, KHÔNG reset status
                partnerHomestay = p4Opt.get();
                partnerHomestay.setRole(User.Role.PARTNER);
                partnerHomestay.setPartnerPropertyType(PropertyType.HOMESTAY);
                if (partnerHomestay.getBankAccountNumber() == null) {
                    partnerHomestay.setBankAccountNumber("5544332211");
                    partnerHomestay.setBankName("Agribank");
                    partnerHomestay.setBankAccountHolder("PHAM THI D");
                    partnerHomestay.setBankBranch("Cần Thơ");
                }
                partnerHomestay = userRepository.save(partnerHomestay);
            }

            // ── BACKFILL: gán đúng owner theo propertyType ────────────────────
            backfillOwnerByType(accommodationRepository, partnerHotel,    PropertyType.HOTEL);
            backfillOwnerByType(accommodationRepository, partnerResort,   PropertyType.RESORT);
            backfillOwnerByType(accommodationRepository, partnerVilla,    PropertyType.VILLA);
            backfillOwnerByType(accommodationRepository, partnerHomestay, PropertyType.HOMESTAY);

            // ── FIX: reassign accommodation bị gán nhầm owner (tất cả combo) ──
            // partnerHotel có thể sở hữu Villa/Homestay/Resort sai → chuyển lại
            reassignWrongOwner(accommodationRepository, partnerHotel, partnerVilla,    PropertyType.VILLA);
            reassignWrongOwner(accommodationRepository, partnerHotel, partnerHomestay, PropertyType.HOMESTAY);
            reassignWrongOwner(accommodationRepository, partnerHotel, partnerResort,   PropertyType.RESORT);
            // partnerResort có thể sở hữu Hotel/Villa/Homestay sai → chuyển lại
            reassignWrongOwner(accommodationRepository, partnerResort, partnerHotel,    PropertyType.HOTEL);
            reassignWrongOwner(accommodationRepository, partnerResort, partnerVilla,    PropertyType.VILLA);
            reassignWrongOwner(accommodationRepository, partnerResort, partnerHomestay, PropertyType.HOMESTAY);
            // partnerVilla có thể sở hữu Hotel/Resort/Homestay sai → chuyển lại
            reassignWrongOwner(accommodationRepository, partnerVilla, partnerHotel,    PropertyType.HOTEL);
            reassignWrongOwner(accommodationRepository, partnerVilla, partnerResort,   PropertyType.RESORT);
            reassignWrongOwner(accommodationRepository, partnerVilla, partnerHomestay, PropertyType.HOMESTAY);
            // partnerHomestay có thể sở hữu Hotel/Resort/Villa sai → chuyển lại
            reassignWrongOwner(accommodationRepository, partnerHomestay, partnerHotel,  PropertyType.HOTEL);
            reassignWrongOwner(accommodationRepository, partnerHomestay, partnerResort, PropertyType.RESORT);
            reassignWrongOwner(accommodationRepository, partnerHomestay, partnerVilla,  PropertyType.VILLA);

            // ── BACKFILL: approval status cho room cũ ────────────────────────
            backfillRoomApprovalStatusIfMissing(roomRepository);

            // ── BACKFILL: room category + commission ─────────────────────────
            backfillRoomCategoryAndCommission(roomRepository);

            // ── KHỞI TẠO HOTEL (nếu chưa có) ──────────────────────────────────
            long hotelCount = accommodationRepository.countByPropertyType(PropertyType.HOTEL);
            if (hotelCount == 0) {
                initHotels(accommodationRepository, partnerHotel);
            }

            // ── KHỞI TẠO VILLA (nếu chưa có) ──────────────────────────────────
            long villaCount = accommodationRepository.countByPropertyType(PropertyType.VILLA);
            if (villaCount == 0) {
                initVillas(accommodationRepository, partnerVilla);
            }

            // ── KHỞI TẠO HOMESTAY (nếu chưa có) ───────────────────────────────
            long homestayCount = accommodationRepository.countByPropertyType(PropertyType.HOMESTAY);
            if (homestayCount == 0) {
                initHomestays(accommodationRepository, partnerHomestay);
            }

            // ── KHỞI TẠO RESORT (nếu chưa có) ─────────────────────────────────
            long resortCount = accommodationRepository.countByPropertyType(PropertyType.RESORT);
            if (resortCount == 0) {
                initResorts(accommodationRepository, partnerResort);
            }

            // ── KHỞI TẠO BỔ SUNG: đủ điểm lưu trú theo loại hình, idempotent theo name/roomCode ──
            initMissingDefaultAccommodations(accommodationRepository, roomRepository,
                    partnerHotel, partnerResort, partnerVilla, partnerHomestay);

            // ── FIX: ảnh demo phải tách riêng theo từng nơi lưu trú và từng phòng/căn ──
            normalizeSeedImageUrls(jdbcTemplate);

            // ── FIX: Cập nhật settlement notes khớp đúng với partner ─────────
            fixSettlementNotes(settlementRepository, partnerHotel, partnerResort, partnerVilla, partnerHomestay);

            log.info("[DataInitializer] Initial data completed.");
        };
    }

    // ── INITIALIZATION METHODS ───────────────────────────────────────────────

    private void cleanupLegacySeedWalletData(JdbcTemplate jdbcTemplate) {
        String seedEmails = "('seeduser_wallet@travelmate.vn','seedpartner_wallet@travelmate.vn','seedpartner_nobank@travelmate.vn')";
        try {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            int deleted = 0;
            deleted += updateQuietly(jdbcTemplate,
                    "DELETE FROM partner_wallet_transactions " +
                            "WHERE transaction_code LIKE 'WTX-SEED-%' " +
                            "OR partner_id IN (SELECT id FROM users WHERE email IN " + seedEmails + ") " +
                            "OR settlement_id IN (SELECT ps.id FROM partner_settlements ps JOIN users u ON u.id = ps.partner_id WHERE u.email IN " + seedEmails + ") " +
                            "OR withdrawal_request_id IN (SELECT wr.id FROM partner_withdrawal_requests wr WHERE wr.request_code LIKE 'WD-SEED-%')");
            deleted += updateQuietly(jdbcTemplate,
                    "DELETE FROM partner_withdrawal_requests " +
                            "WHERE request_code LIKE 'WD-SEED-%' " +
                            "OR partner_id IN (SELECT id FROM users WHERE email IN " + seedEmails + ")");
            deleted += updateQuietly(jdbcTemplate,
                    "DELETE FROM partner_wallets WHERE partner_id IN (SELECT id FROM users WHERE email IN " + seedEmails + ")");
            deleted += updateQuietly(jdbcTemplate,
                    "DELETE FROM partner_settlements WHERE partner_id IN (SELECT id FROM users WHERE email IN " + seedEmails + ") " +
                            "OR note LIKE 'SEED:%'");
            deleted += updateQuietly(jdbcTemplate,
                    "DELETE FROM payments WHERE transaction_code LIKE 'TXN-SEED-WALLET-%' " +
                            "OR booking_id IN (SELECT id FROM bookings WHERE booking_code LIKE 'BK-SEED-WALLET-%')");
            deleted += updateQuietly(jdbcTemplate,
                    "DELETE FROM reviews WHERE booking_id IN (SELECT id FROM bookings WHERE booking_code LIKE 'BK-SEED-WALLET-%') " +
                            "OR user_id IN (SELECT id FROM users WHERE email IN " + seedEmails + ")");
            deleted += updateQuietly(jdbcTemplate,
                    "DELETE FROM bookings WHERE booking_code LIKE 'BK-SEED-WALLET-%' " +
                            "OR customer_email IN ('seeduser_wallet@travelmate.vn')");
            deleted += updateQuietly(jdbcTemplate,
                    "DELETE FROM room_voucher_assignments WHERE room_id IN (SELECT id FROM rooms WHERE room_code = 'SEED-WALLET-STD')");
            deleted += updateQuietly(jdbcTemplate,
                    "DELETE FROM room_images WHERE room_id IN (SELECT id FROM rooms WHERE room_code = 'SEED-WALLET-STD')");
            deleted += updateQuietly(jdbcTemplate,
                    "DELETE FROM room_amenities WHERE room_id IN (SELECT id FROM rooms WHERE room_code = 'SEED-WALLET-STD')");
            deleted += updateQuietly(jdbcTemplate,
                    "DELETE FROM vouchers WHERE code LIKE 'SEED%' " +
                            "OR owner_id IN (SELECT id FROM users WHERE email IN " + seedEmails + ") " +
                            "OR accommodation_id IN (SELECT id FROM accommodations WHERE name LIKE '[SEED WALLET]%' OR name LIKE '%Settlement Hotel%') " +
                            "OR room_id IN (SELECT id FROM rooms WHERE room_code = 'SEED-WALLET-STD')");
            deleted += updateQuietly(jdbcTemplate,
                    "DELETE FROM rooms WHERE room_code = 'SEED-WALLET-STD' " +
                            "OR room_name LIKE '%Seed%Settlement%Room%'");
            deleted += updateQuietly(jdbcTemplate,
                    "DELETE FROM accommodations WHERE name LIKE '[SEED WALLET]%' OR name LIKE '%Settlement Hotel%'");
            deleted += updateQuietly(jdbcTemplate,
                    "DELETE FROM notifications WHERE user_id IN (SELECT id FROM users WHERE email IN " + seedEmails + ")");
            deleted += updateQuietly(jdbcTemplate,
                    "DELETE FROM support_tickets WHERE requester_email IN " + seedEmails +
                            " OR partner_id IN (SELECT id FROM users WHERE email IN " + seedEmails + ")" +
                            " OR user_id IN (SELECT id FROM users WHERE email IN " + seedEmails + ")");
            deleted += updateQuietly(jdbcTemplate,
                    "DELETE FROM admin_action_logs WHERE details LIKE '%SEED-WALLET%' OR details LIKE '%Settlement Room%'");
            deleted += updateQuietly(jdbcTemplate,
                    "DELETE FROM users WHERE email IN " + seedEmails);
            if (deleted > 0) {
                log.info("[DataInitializer] Removed {} legacy seed wallet/settlement rows.", deleted);
            }
        } catch (Exception e) {
            log.warn("[DataInitializer] Could not cleanup legacy seed wallet data: {}", e.getMessage());
        } finally {
            try {
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            } catch (Exception ignored) {
            }
        }
    }

    private void deduplicateAmenities(JdbcTemplate jdbcTemplate) {
        try {
            jdbcTemplate.update("UPDATE amenities SET name = TRIM(name), category = COALESCE(NULLIF(TRIM(category), ''), 'Khác')");
            jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS tm_amenity_keep");
            jdbcTemplate.execute(
                    "CREATE TEMPORARY TABLE tm_amenity_keep AS " +
                            "SELECT MIN(id) AS keep_id, LOWER(TRIM(name)) AS normalized_name, " +
                            "LOWER(TRIM(COALESCE(category, ''))) AS normalized_category " +
                            "FROM amenities " +
                            "GROUP BY LOWER(TRIM(name)), LOWER(TRIM(COALESCE(category, '')))");
            int restoredLinks = jdbcTemplate.update(
                    "INSERT IGNORE INTO room_amenities (room_id, amenity_id) " +
                            "SELECT DISTINCT ra.room_id, k.keep_id " +
                            "FROM room_amenities ra " +
                            "JOIN amenities a ON a.id = ra.amenity_id " +
                            "JOIN tm_amenity_keep k ON k.normalized_name = LOWER(TRIM(a.name)) " +
                            "AND k.normalized_category = LOWER(TRIM(COALESCE(a.category, ''))) " +
                            "WHERE ra.amenity_id <> k.keep_id");
            int removedLinks = jdbcTemplate.update(
                    "DELETE ra FROM room_amenities ra " +
                            "JOIN amenities a ON a.id = ra.amenity_id " +
                            "JOIN tm_amenity_keep k ON k.normalized_name = LOWER(TRIM(a.name)) " +
                            "AND k.normalized_category = LOWER(TRIM(COALESCE(a.category, ''))) " +
                            "WHERE a.id <> k.keep_id");
            int removedAmenities = jdbcTemplate.update(
                    "DELETE a FROM amenities a " +
                            "JOIN tm_amenity_keep k ON k.normalized_name = LOWER(TRIM(a.name)) " +
                            "AND k.normalized_category = LOWER(TRIM(COALESCE(a.category, ''))) " +
                            "WHERE a.id <> k.keep_id");
            jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS tm_amenity_keep");
            if (restoredLinks + removedLinks + removedAmenities > 0) {
                log.info("[DataInitializer] Amenity cleanup: kept links={}, removed duplicate links={}, removed duplicate amenities={}.",
                        restoredLinks, removedLinks, removedAmenities);
            }
        } catch (Exception e) {
            log.warn("[DataInitializer] Could not deduplicate amenities: {}", e.getMessage());
        }
    }

    private void deactivateLegacyPartnerIssuedVouchers(JdbcTemplate jdbcTemplate) {
        int updated = updateQuietly(jdbcTemplate,
                "UPDATE vouchers SET active = 0 " +
                        "WHERE owner_id IS NOT NULL " +
                        "AND voucher_scope IN ('PARTNER_ROOM', 'PARTNER_ACCOMMODATION')");
        if (updated > 0) {
            log.info("[DataInitializer] Deactivated {} legacy partner-issued vouchers; Admin catalog assignments remain supported.",
                    updated);
        }
    }

    private void ensureDatabaseConstraints(JdbcTemplate jdbcTemplate) {
        updateQuietly(jdbcTemplate,
                "ALTER TABLE amenities ADD UNIQUE KEY uk_amenity_name_category (name, category)");
        updateQuietly(jdbcTemplate,
                "ALTER TABLE rooms ADD UNIQUE KEY uk_rooms_room_code (room_code)");
    }

    private int updateQuietly(JdbcTemplate jdbcTemplate, String sql) {
        try {
            return jdbcTemplate.update(sql);
        } catch (Exception e) {
            log.debug("[DataInitializer] Skip SQL maintenance step: {}", e.getMessage());
            return 0;
        }
    }

    private int updateQuietly(JdbcTemplate jdbcTemplate, String sql, Object... args) {
        try {
            return jdbcTemplate.update(sql, args);
        } catch (Exception e) {
            log.debug("[DataInitializer] Skip SQL maintenance step: {}", e.getMessage());
            return 0;
        }
    }

    private void normalizeLegacyPaymentMethods(JdbcTemplate jdbcTemplate) {
        try {
            String oldVnpayMethod = "VNPAY_" + "DEMO";
            int updated = jdbcTemplate.update(
                    "UPDATE payments SET payment_method = ? WHERE payment_method = ?",
                    "VNPAY", oldVnpayMethod);
            if (updated > 0) {
                log.info("[DataInitializer] Normalized {} old payment method rows.", updated);
            }
        } catch (Exception e) {
            log.debug("[DataInitializer] Skip payment method normalization: {}", e.getMessage());
        }
    }

    private void normalizeVisibleSeedLabels(JdbcTemplate jdbcTemplate) {
        try {
            int updated = 0;
            updated += jdbcTemplate.update(
                    "UPDATE accommodations SET name = 'Da Lat Mountain Boutique Hotel' " +
                            "WHERE name = '[PENDING DEMO] Da Lat Mountain Boutique Hotel'");
            updated += jdbcTemplate.update(
                    "UPDATE accommodations SET name = 'Da Lat Sunrise Guesthouse', " +
                            "description = 'Listing bị Admin từ chối do ảnh đại diện và thông tin pháp lý chưa đạt yêu cầu.', " +
                            "address = '12 Đường Hoa Ban, Phường 3' " +
                            "WHERE name = '[REJECTED DEMO] Da Lat Fake Hotel' OR name = 'Da Lat Fake Hotel'");
            updated += jdbcTemplate.update(
                    "UPDATE rooms SET commission_rate_override = 20.00 " +
                            "WHERE room_code IN ('TLP-VIP', 'TMG-PRE') AND (commission_rate_override IS NULL OR commission_rate_override <> 20.00)");
            updated += jdbcTemplate.update(
                    "UPDATE admin_action_logs SET details = REPLACE(details, 'Da Lat Fake Hotel', 'Da Lat Sunrise Guesthouse') " +
                            "WHERE details LIKE '%Da Lat Fake Hotel%'");
            updated += jdbcTemplate.update(
                    "UPDATE support_tickets SET subject = 'Muốn gắn voucher nhưng không thấy phòng/căn của tôi', " +
                            "description = 'Tôi vào /partner/vouchers để gắn voucher do Admin phát hành, nhưng không thấy phòng/căn phù hợp. ' " +
                            "WHERE subject = 'Muốn tạo voucher nhưng không thấy Cơ sở lưu trú của tôi' " +
                            "OR description LIKE '%Voucher theo cơ sở%'");
            if (updated > 0) {
                log.info("[DataInitializer] Normalized {} visible demo labels/commission examples.", updated);
            }
        } catch (Exception e) {
            log.debug("[DataInitializer] Skip visible label normalization: {}", e.getMessage());
        }
    }

    private void normalizeSeedImageUrls(JdbcTemplate jdbcTemplate) {
        try {
            int updated = 0;
            List<SeedRoomGallery> roomGalleries = seedRoomImageGalleries();
            for (SeedImagePatch patch : seedAccommodationImagePatches()) {
                updated += jdbcTemplate.update(
                        "UPDATE accommodations SET thumbnail_url = ? " +
                                "WHERE name = ? AND (thumbnail_url IS NULL OR thumbnail_url <> ?)",
                        patch.imageUrl(), patch.key(), patch.imageUrl());
            }
            for (SeedRoomGallery gallery : roomGalleries) {
                String primaryImageUrl = gallery.imageUrls().get(0);
                updated += jdbcTemplate.update(
                        "UPDATE rooms SET image_url = ? " +
                                "WHERE room_code = ? AND (image_url IS NULL OR image_url <> ?)",
                        primaryImageUrl, gallery.key(), primaryImageUrl);
            }

            String roomCodes = sqlInListForRoomGalleries(roomGalleries);
            updated += updateQuietly(jdbcTemplate,
                    "DELETE ri FROM room_images ri " +
                            "JOIN rooms r ON r.id = ri.room_id " +
                            "WHERE r.room_code IN (" + roomCodes + ")");
            for (SeedRoomGallery gallery : roomGalleries) {
                for (int i = 0; i < gallery.imageUrls().size(); i++) {
                    updated += updateQuietly(jdbcTemplate,
                            "INSERT INTO room_images (room_id, image_url, caption, sort_order, is_primary, created_at, updated_at) " +
                                    "SELECT r.id, ?, CONCAT(r.room_name, ' - ảnh ', ?), ?, ?, NOW(), NOW() " +
                                    "FROM rooms r WHERE r.room_code = ?",
                            gallery.imageUrls().get(i), i + 1, i, i == 0 ? 1 : 0, gallery.key());
                }
            }
            if (updated > 0) {
                log.info("[DataInitializer] Normalized {} seed accommodation/room gallery image links.", updated);
            }
        } catch (Exception e) {
            log.debug("[DataInitializer] Skip seed image normalization: {}", e.getMessage());
        }
    }

    private List<SeedImagePatch> seedAccommodationImagePatches() {
        return List.of(
                new SeedImagePatch("Tulip Hotel 2 Dalat", "/assets/images/accommodations/hotel/tulip-hotel-2-dalat/cover.jpg"),
                new SeedImagePatch("TravelMate Grand Hotel", "/assets/images/accommodations/hotel/travelmate-grand-hotel/cover.jpg"),
                new SeedImagePatch("Da Lat Mountain Boutique Hotel", "/assets/images/accommodations/hotel/da-lat-mountain-boutique-hotel/cover.jpg"),
                new SeedImagePatch("Da Lat Sunrise Guesthouse", "/assets/images/accommodations/hotel/da-lat-sunrise-guesthouse/cover.jpg"),
                new SeedImagePatch("InterContinental Nha Trang", "/assets/images/accommodations/hotel/intercontinental-nha-trang/cover.jpg"),
                new SeedImagePatch("Novotel Đà Nẵng Premier", "/assets/images/accommodations/hotel/novotel-da-nang-premier/cover.jpg"),
                new SeedImagePatch("La Siesta Hội An Resort & Spa", "/assets/images/accommodations/hotel/la-siesta-hoi-an-resort-spa/cover.jpg"),
                new SeedImagePatch("Sofitel Legend Metropole Hà Nội", "/assets/images/accommodations/hotel/sofitel-legend-metropole-ha-noi/cover.jpg"),
                new SeedImagePatch("Sapa Cloud Valley Hotel", "/assets/images/accommodations/hotel/sapa-cloud-valley-hotel/cover.jpg"),
                new SeedImagePatch("New World Sài Gòn Hotel", "/assets/images/accommodations/hotel/new-world-sai-gon-hotel/cover.jpg"),
                new SeedImagePatch("Pullman Vũng Tàu", "/assets/images/accommodations/hotel/pullman-vung-tau/cover.jpg"),
                new SeedImagePatch("The Anam Villa Nha Trang", "/assets/images/accommodations/villa/the-anam-villa-nha-trang/cover.jpg"),
                new SeedImagePatch("Ba Na Hills Forest Villa", "/assets/images/accommodations/villa/ba-na-hills-forest-villa/cover.jpg"),
                new SeedImagePatch("Pine Hill Villa Đà Lạt", "/assets/images/accommodations/villa/pine-hill-villa-da-lat/cover.jpg"),
                new SeedImagePatch("Sunset Beach Villa Phú Quốc", "/assets/images/accommodations/villa/sunset-beach-villa-phu-quoc/cover.jpg"),
                new SeedImagePatch("Hoa Lư Riverside Homestay", "/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/cover.jpg"),
                new SeedImagePatch("Hoa Lu Riverside Homestay", "/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/cover.jpg"),
                new SeedImagePatch("Mộc Nhiên Garden Homestay Đà Lạt", "/assets/images/accommodations/homestay/moc-nhien-garden-homestay-da-lat/cover.jpg"),
                new SeedImagePatch("Tam Cốc Garden Homestay", "/assets/images/accommodations/homestay/tam-coc-garden-homestay/cover.jpg"),
                new SeedImagePatch("Sapa Valley Homestay", "/assets/images/accommodations/homestay/sapa-valley-homestay/cover.jpg"),
                new SeedImagePatch("Vinpearl Resort & Spa Nha Trang", "/assets/images/accommodations/resort/vinpearl-resort-spa-nha-trang/cover.jpg"),
                new SeedImagePatch("Furama Resort Đà Nẵng", "/assets/images/accommodations/resort/furama-resort-da-nang/cover.jpg"),
                new SeedImagePatch("Sunset Pearl Resort Phú Quốc", "/assets/images/accommodations/resort/sunset-pearl-resort-phu-quoc/cover.jpg"),
                new SeedImagePatch("Legacy Bay Resort Hạ Long", "/assets/images/accommodations/resort/legacy-bay-resort-ha-long/cover.jpg"),
                new SeedImagePatch("Azerai Cần Thơ Resort", "/assets/images/accommodations/resort/azerai-can-tho-resort/cover.jpg"),
                new SeedImagePatch("TTC Resort Mũi Né", "/assets/images/accommodations/resort/ttc-resort-mui-ne/cover.jpg")
        );
    }

    private List<SeedImagePatch> seedRoomImagePatches() {
        return List.of(
                new SeedImagePatch("TLP-STD", "/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/tlp-std.jpg"),
                new SeedImagePatch("TLP-SUP", "/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/tlp-sup.jpg"),
                new SeedImagePatch("TLP-FAM", "/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/tlp-fam.jpg"),
                new SeedImagePatch("TLP-VIP", "/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/tlp-vip.jpg"),
                new SeedImagePatch("R201", "/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/r201.jpg"),
                new SeedImagePatch("R202", "/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/r202.jpg"),
                new SeedImagePatch("R203", "/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/r203.jpg"),
                new SeedImagePatch("R204", "/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/r204.jpg"),
                new SeedImagePatch("TMG-DLX", "/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/tmg-dlx.jpg"),
                new SeedImagePatch("TMG-PRE", "/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/tmg-pre.jpg"),
                new SeedImagePatch("TMG-FAM", "/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/tmg-fam.jpg"),
                new SeedImagePatch("TMG-PRE2", "/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/tmg-pre2.jpg"),
                new SeedImagePatch("R301", "/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/r301.jpg"),
                new SeedImagePatch("R302", "/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/r302.jpg"),
                new SeedImagePatch("R303", "/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/r303.jpg"),
                new SeedImagePatch("R304", "/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/r304.jpg"),
                new SeedImagePatch("ICN-STD", "/assets/images/accommodations/hotel/intercontinental-nha-trang/rooms/icn-std.jpg"),
                new SeedImagePatch("ICN-DLX", "/assets/images/accommodations/hotel/intercontinental-nha-trang/rooms/icn-dlx.jpg"),
                new SeedImagePatch("ICN-SUI", "/assets/images/accommodations/hotel/intercontinental-nha-trang/rooms/icn-sui.jpg"),
                new SeedImagePatch("NVD-STD", "/assets/images/accommodations/hotel/novotel-da-nang-premier/rooms/nvd-std.jpg"),
                new SeedImagePatch("NVD-DLX", "/assets/images/accommodations/hotel/novotel-da-nang-premier/rooms/nvd-dlx.jpg"),
                new SeedImagePatch("NVD-FAM", "/assets/images/accommodations/hotel/novotel-da-nang-premier/rooms/nvd-fam.jpg"),
                new SeedImagePatch("LSH-STD", "/assets/images/accommodations/hotel/la-siesta-hoi-an-resort-spa/rooms/lsh-std.jpg"),
                new SeedImagePatch("LSH-DLX", "/assets/images/accommodations/hotel/la-siesta-hoi-an-resort-spa/rooms/lsh-dlx.jpg"),
                new SeedImagePatch("LSH-SUI", "/assets/images/accommodations/hotel/la-siesta-hoi-an-resort-spa/rooms/lsh-sui.jpg"),
                new SeedImagePatch("SLM-PRE", "/assets/images/accommodations/hotel/sofitel-legend-metropole-ha-noi/rooms/slm-pre.jpg"),
                new SeedImagePatch("SLM-GRA", "/assets/images/accommodations/hotel/sofitel-legend-metropole-ha-noi/rooms/slm-gra.jpg"),
                new SeedImagePatch("SLM-FAM", "/assets/images/accommodations/hotel/sofitel-legend-metropole-ha-noi/rooms/slm-fam.jpg"),
                new SeedImagePatch("SPC-STD", "/assets/images/accommodations/hotel/sapa-cloud-valley-hotel/rooms/spc-std.jpg"),
                new SeedImagePatch("SPC-DLX", "/assets/images/accommodations/hotel/sapa-cloud-valley-hotel/rooms/spc-dlx.jpg"),
                new SeedImagePatch("NWS-STD", "/assets/images/accommodations/hotel/new-world-sai-gon-hotel/rooms/nws-std.jpg"),
                new SeedImagePatch("NWS-DLX", "/assets/images/accommodations/hotel/new-world-sai-gon-hotel/rooms/nws-dlx.jpg"),
                new SeedImagePatch("PVT-STD", "/assets/images/accommodations/hotel/pullman-vung-tau/rooms/pvt-std.jpg"),
                new SeedImagePatch("PVT-DLX", "/assets/images/accommodations/hotel/pullman-vung-tau/rooms/pvt-dlx.jpg"),
                new SeedImagePatch("ANM-GDN", "/assets/images/accommodations/villa/the-anam-villa-nha-trang/rooms/anm-gdn.jpg"),
                new SeedImagePatch("ANM-BCH", "/assets/images/accommodations/villa/the-anam-villa-nha-trang/rooms/anm-bch.jpg"),
                new SeedImagePatch("ANM-FAM", "/assets/images/accommodations/villa/the-anam-villa-nha-trang/rooms/anm-fam.jpg"),
                new SeedImagePatch("BNH-BNG", "/assets/images/accommodations/villa/ba-na-hills-forest-villa/rooms/bnh-bng.jpg"),
                new SeedImagePatch("BNH-TWN", "/assets/images/accommodations/villa/ba-na-hills-forest-villa/rooms/bnh-twn.jpg"),
                new SeedImagePatch("BNH-SUI", "/assets/images/accommodations/villa/ba-na-hills-forest-villa/rooms/bnh-sui.jpg"),
                new SeedImagePatch("V101", "/assets/images/accommodations/villa/ba-na-hills-forest-villa/rooms/v101.jpg"),
                new SeedImagePatch("V102", "/assets/images/accommodations/villa/ba-na-hills-forest-villa/rooms/v102.jpg"),
                new SeedImagePatch("PHV-DLX", "/assets/images/accommodations/villa/pine-hill-villa-da-lat/rooms/phv-dlx.jpg"),
                new SeedImagePatch("PHV-FAM", "/assets/images/accommodations/villa/pine-hill-villa-da-lat/rooms/phv-fam.jpg"),
                new SeedImagePatch("SBV-SEA", "/assets/images/accommodations/villa/sunset-beach-villa-phu-quoc/rooms/sbv-sea.jpg"),
                new SeedImagePatch("SBV-POOL", "/assets/images/accommodations/villa/sunset-beach-villa-phu-quoc/rooms/sbv-pool.jpg"),
                new SeedImagePatch("HLR-STD", "/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/rooms/hlr-std.jpg"),
                new SeedImagePatch("HLR-DLX", "/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/rooms/hlr-dlx.jpg"),
                new SeedImagePatch("HLR-FAM", "/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/rooms/hlr-fam.jpg"),
                new SeedImagePatch("HS101", "/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/rooms/hs101.jpg"),
                new SeedImagePatch("HS102", "/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/rooms/hs102.jpg"),
                new SeedImagePatch("MND-STD", "/assets/images/accommodations/homestay/moc-nhien-garden-homestay-da-lat/rooms/mnd-std.jpg"),
                new SeedImagePatch("MND-ATT", "/assets/images/accommodations/homestay/moc-nhien-garden-homestay-da-lat/rooms/mnd-att.jpg"),
                new SeedImagePatch("MND-FAM", "/assets/images/accommodations/homestay/moc-nhien-garden-homestay-da-lat/rooms/mnd-fam.jpg"),
                new SeedImagePatch("TCG-STD", "/assets/images/accommodations/homestay/tam-coc-garden-homestay/rooms/tcg-std.jpg"),
                new SeedImagePatch("TCG-FAM", "/assets/images/accommodations/homestay/tam-coc-garden-homestay/rooms/tcg-fam.jpg"),
                new SeedImagePatch("SVH-STD", "/assets/images/accommodations/homestay/sapa-valley-homestay/rooms/svh-std.jpg"),
                new SeedImagePatch("SVH-DLX", "/assets/images/accommodations/homestay/sapa-valley-homestay/rooms/svh-dlx.jpg"),
                new SeedImagePatch("VNT-DLX", "/assets/images/accommodations/resort/vinpearl-resort-spa-nha-trang/rooms/vnt-dlx.jpg"),
                new SeedImagePatch("VNT-SUI", "/assets/images/accommodations/resort/vinpearl-resort-spa-nha-trang/rooms/vnt-sui.jpg"),
                new SeedImagePatch("VNT-VIL", "/assets/images/accommodations/resort/vinpearl-resort-spa-nha-trang/rooms/vnt-vil.jpg"),
                new SeedImagePatch("FDN-DLX", "/assets/images/accommodations/resort/furama-resort-da-nang/rooms/fdn-dlx.jpg"),
                new SeedImagePatch("FDN-BCH", "/assets/images/accommodations/resort/furama-resort-da-nang/rooms/fdn-bch.jpg"),
                new SeedImagePatch("FDN-FAM", "/assets/images/accommodations/resort/furama-resort-da-nang/rooms/fdn-fam.jpg"),
                new SeedImagePatch("RS101", "/assets/images/accommodations/resort/furama-resort-da-nang/rooms/rs101.jpg"),
                new SeedImagePatch("RS102", "/assets/images/accommodations/resort/furama-resort-da-nang/rooms/rs102.jpg"),
                new SeedImagePatch("SPQ-DLX", "/assets/images/accommodations/resort/sunset-pearl-resort-phu-quoc/rooms/spq-dlx.jpg"),
                new SeedImagePatch("SPQ-SEA", "/assets/images/accommodations/resort/sunset-pearl-resort-phu-quoc/rooms/spq-sea.jpg"),
                new SeedImagePatch("LBR-DLX", "/assets/images/accommodations/resort/legacy-bay-resort-ha-long/rooms/lbr-dlx.jpg"),
                new SeedImagePatch("LBR-SUI", "/assets/images/accommodations/resort/legacy-bay-resort-ha-long/rooms/lbr-sui.jpg"),
                new SeedImagePatch("AZC-DLX", "/assets/images/accommodations/resort/azerai-can-tho-resort/rooms/azc-dlx.jpg"),
                new SeedImagePatch("AZC-SUI", "/assets/images/accommodations/resort/azerai-can-tho-resort/rooms/azc-sui.jpg"),
                new SeedImagePatch("TTC-DLX", "/assets/images/accommodations/resort/ttc-resort-mui-ne/rooms/ttc-dlx.jpg"),
                new SeedImagePatch("TTC-FAM", "/assets/images/accommodations/resort/ttc-resort-mui-ne/rooms/ttc-fam.jpg")
        );
    }

    private List<SeedRoomGallery> seedRoomImageGalleries() {
        List<SeedRoomGallery> galleries = new ArrayList<>();
        galleries.addAll(List.of(
                new SeedRoomGallery("LATA-STD", defaultGalleryUrls("/assets/images/accommodations/lata/rooms/standard-king/interior.jpg")),
                new SeedRoomGallery("LATA-DLX", defaultGalleryUrls("/assets/images/accommodations/lata/rooms/deluxe-double/bedroom.jpg")),
                new SeedRoomGallery("LATA-FAM", defaultGalleryUrls("/assets/images/accommodations/lata/rooms/family/main.jpg")),
                new SeedRoomGallery("LATA-SUI", defaultGalleryUrls("/assets/images/accommodations/lata/rooms/loft-suite/view-lounge.jpg")),
                new SeedRoomGallery("R101", defaultGalleryUrls("/assets/images/accommodations/lata/rooms/standard-king/interior.jpg")),
                new SeedRoomGallery("R102", defaultGalleryUrls("/assets/images/accommodations/lata/rooms/deluxe-double/bedroom.jpg")),
                new SeedRoomGallery("R103", defaultGalleryUrls("/assets/images/accommodations/lata/rooms/family/main.jpg")),
                new SeedRoomGallery("R104", defaultGalleryUrls("/assets/images/accommodations/lata/rooms/loft-suite/view-lounge.jpg"))
        ));
        for (SeedImagePatch patch : seedRoomImagePatches()) {
            galleries.add(new SeedRoomGallery(patch.key(), defaultGalleryUrls(patch.imageUrl())));
        }
        return galleries;
    }

    private List<String> defaultGalleryUrls(String primaryImageUrl) {
        return List.of(
                primaryImageUrl,
                detailImageUrl(primaryImageUrl, 2),
                detailImageUrl(primaryImageUrl, 3)
        );
    }

    private String detailImageUrl(String imageUrl, int index) {
        int extensionIndex = imageUrl.lastIndexOf('.');
        if (extensionIndex < 0) {
            return imageUrl + "-detail-" + index;
        }
        return imageUrl.substring(0, extensionIndex) + "-detail-" + index + imageUrl.substring(extensionIndex);
    }

    private String sqlInListForRoomGalleries(List<SeedRoomGallery> galleries) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < galleries.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(sqlLiteral(galleries.get(i).key()));
        }
        return builder.toString();
    }

    private String sqlLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private record SeedImagePatch(String key, String imageUrl) {
    }

    private record SeedRoomGallery(String key, List<String> imageUrls) {
    }

    /**
     * Chuyen cac snapshot cu cua don coc ve quy tac Hướng A:
     * hoa hong chi tinh tren tien online TravelMate da thu.
     * Rate da snapshot van duoc giu nguyen de khong thay doi thoa thuan cu.
     */
    private void normalizeOnlineDepositCommissionSnapshots(JdbcTemplate jdbcTemplate) {
        String effectiveRate =
                "COALESCE(b.commission_rate_snapshot, " +
                "COALESCE(r.commission_rate_override / 100, " +
                "CASE a.property_type WHEN 'HOTEL' THEN 0.15 WHEN 'RESORT' THEN 0.18 " +
                "WHEN 'VILLA' THEN 0.12 WHEN 'HOMESTAY' THEN 0.10 ELSE 0.10 END))";
        String partnerVoucher =
                "CASE WHEN b.voucher_cost_bearer = 'PARTNER' THEN COALESCE(b.discount_amount, 0) ELSE 0 END";
        String remaining =
                "GREATEST(COALESCE(b.total_amount, 0) - COALESCE(b.paid_amount, 0), 0)";
        String noOnsiteCollection =
                "b.payment_status = 'DEPOSIT_FORFEITED' OR b.booking_status IN ('CANCELLED', 'NO_SHOW')";
        String normalizedRemaining =
                "CASE WHEN " + noOnsiteCollection + " THEN COALESCE(b.remaining_amount, 0) " +
                "ELSE " + remaining + " END";
        String onsite =
                "CASE WHEN " + noOnsiteCollection + " THEN 0 ELSE " + remaining + " END";
        String commission = "ROUND(COALESCE(b.paid_amount, 0) * " + effectiveRate + ", 0)";
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE bookings b " +
                    "JOIN rooms r ON r.id = b.room_id " +
                    "JOIN accommodations a ON a.id = b.accommodation_id " +
                    "SET b.commission_rate_snapshot = " + effectiveRate + ", " +
                    "b.commission_source_snapshot = COALESCE(b.commission_source_snapshot, " +
                    "CASE WHEN r.commission_rate_override IS NOT NULL THEN 'ROOM_OVERRIDE' ELSE 'PROPERTY_TYPE_DEFAULT' END), " +
                    "b.commission_base_amount = COALESCE(b.paid_amount, 0), " +
                    "b.commission_amount_snapshot = " + commission + ", " +
                    "b.partner_voucher_amount_snapshot = " + partnerVoucher + ", " +
                    "b.admin_voucher_amount_snapshot = CASE WHEN b.voucher_cost_bearer = 'ADMIN' " +
                    "THEN COALESCE(b.discount_amount, 0) ELSE 0 END, " +
                    "b.partner_payout_snapshot = GREATEST(COALESCE(b.paid_amount, 0) - " +
                    commission + " - " + partnerVoucher + ", 0), " +
                    "b.remaining_amount = " + normalizedRemaining + ", " +
                    "b.online_paid_amount_snapshot = COALESCE(b.paid_amount, 0), " +
                    "b.onsite_amount_snapshot = " + onsite + " " +
                    "WHERE b.payment_option = 'DEPOSIT_30' " +
                    "AND (b.booking_source IS NULL OR b.booking_source = 'ONLINE') " +
                    "AND (b.commission_rate_snapshot IS NULL " +
                    "OR COALESCE(b.commission_base_amount, -1) <> COALESCE(b.paid_amount, 0) " +
                    "OR COALESCE(b.commission_amount_snapshot, -1) <> " + commission + " " +
                    "OR COALESCE(b.partner_payout_snapshot, -1) <> GREATEST(COALESCE(b.paid_amount, 0) - " +
                    commission + " - " + partnerVoucher + ", 0) " +
                    "OR COALESCE(b.remaining_amount, -1) <> " + normalizedRemaining + " " +
                    "OR COALESCE(b.online_paid_amount_snapshot, -1) <> COALESCE(b.paid_amount, 0) " +
                    "OR COALESCE(b.onsite_amount_snapshot, -1) <> " + onsite + ")");
            if (updated > 0) {
                log.info("[DataInitializer] Converted {} online deposit snapshots to online-paid commission rule.", updated);
            }
        } catch (Exception e) {
            log.debug("[DataInitializer] Skip online deposit snapshot normalization: {}", e.getMessage());
        }
    }

    private void initHotels(AccommodationRepository repo, User owner) {
        Accommodation h1 = new Accommodation();
        h1.setName("LATA Hotel & Apartments"); h1.setAddress("15 Phan Bội Châu, P1");
        h1.setCity("Đà Lạt"); h1.setPropertyType(PropertyType.HOTEL);
        h1.setApprovalStatus(ApprovalStatus.APPROVED); h1.setOwner(owner);
        h1.setThumbnailUrl("/assets/images/accommodations/lata/cover/room-hero.jpg"); h1.setRating(8.6);
        h1.setReviewCount(771); h1.setStarRating(4);
        h1.setDescription("Khách sạn hiện đại trung tâm Đà Lạt, cách chợ đêm 500m.");
        h1.setRooms(List.of(
            createRoom("R101","Phòng Tiêu Chuẩn King","1 giường King",2,new BigDecimal("650000"),5,"/assets/images/accommodations/lata/rooms/standard-king/interior.jpg","Phòng tiêu chuẩn view thành phố.",h1,RoomCategory.STANDARD,null),
            createRoom("R102","Phòng Deluxe Đôi","2 giường đơn",3,new BigDecimal("850000"),4,"/assets/images/accommodations/lata/rooms/deluxe-double/bedroom.jpg","Phòng Deluxe ban công, minibar.",h1,RoomCategory.DELUXE,null),
            createRoom("R103","Phòng Gia Đình","King + đơn",4,new BigDecimal("1200000"),3,"/assets/images/accommodations/lata/rooms/family/main.jpg","Phòng rộng view núi, bồn tắm.",h1,RoomCategory.FAMILY,new BigDecimal("12.00")),
            createRoom("R104","Suite Cao Cấp","1 King size",2,new BigDecimal("1800000"),2,"/assets/images/accommodations/lata/rooms/loft-suite/view-lounge.jpg","Suite jacuzzi, view toàn cảnh.",h1,RoomCategory.SUITE,new BigDecimal("20.00"))
        ));
        repo.save(h1);

        Accommodation h2 = new Accommodation();
        h2.setName("Tulip Hotel 2 Dalat"); h2.setAddress("56 Bùi Thị Xuân, P2");
        h2.setCity("Đà Lạt"); h2.setPropertyType(PropertyType.HOTEL);
        h2.setApprovalStatus(ApprovalStatus.APPROVED); h2.setOwner(owner);
        h2.setThumbnailUrl("/assets/images/accommodations/catalog/hotel-exterior.jpg"); h2.setRating(8.2);
        h2.setReviewCount(456); h2.setStarRating(3);
        h2.setDescription("Khách sạn 3 sao phong cách Châu Âu, gần hồ Xuân Hương.");
        h2.setRooms(List.of(
            createRoom("R201","Standard Twin","2 giường đơn",2,new BigDecimal("480000"),6,"/assets/images/accommodations/amenities/hotel/bedroom.jpg","Phòng sạch sẽ, wifi.",h2,RoomCategory.STANDARD,null),
            createRoom("R202","Superior Double","1 giường đôi",2,new BigDecimal("620000"),5,"/assets/images/accommodations/amenities/hotel/bedroom.jpg","Ban công nhỏ nhìn ra phố.",h2,RoomCategory.DELUXE,null),
            createRoom("R203","Gia Đình Rộng","Đôi + 2 đơn",5,new BigDecimal("1050000"),3,"/assets/images/accommodations/amenities/hotel/bedroom.jpg","Phòng lớn 4-5 người.",h2,RoomCategory.FAMILY,new BigDecimal("12.00")),
            createRoom("R204","VIP Panorama","1 King",2,new BigDecimal("1500000"),2,"/assets/images/accommodations/amenities/hotel/bedroom.jpg","View 360, bồn tắm nóng.",h2,RoomCategory.VIP,new BigDecimal("20.00"))
        ));
        repo.save(h2);

        Accommodation h3 = new Accommodation();
        h3.setName("TravelMate Grand Hotel"); h3.setAddress("88 Nguyễn Chí Thanh, P6");
        h3.setCity("Đà Lạt"); h3.setPropertyType(PropertyType.HOTEL);
        h3.setApprovalStatus(ApprovalStatus.APPROVED); h3.setOwner(owner);
        h3.setThumbnailUrl("/assets/images/accommodations/catalog/hotel-exterior.jpg"); h3.setRating(9.2);
        h3.setReviewCount(1205); h3.setStarRating(5);
        h3.setDescription("Khách sạn 5 sao sang trọng trên đồi thông, spa cao cấp.");
        h3.setRooms(List.of(
            createRoom("R301","Deluxe Garden View","1 King",2,new BigDecimal("1200000"),8,"/assets/images/accommodations/amenities/hotel/bedroom.jpg","View vườn thông, ban công.",h3,RoomCategory.DELUXE,null),
            createRoom("R302","Premium Valley View","King/2đơn",3,new BigDecimal("1650000"),5,"/assets/images/accommodations/amenities/hotel/bedroom.jpg","View thung lũng, phòng tắm kính.",h3,RoomCategory.VIP,new BigDecimal("20.00")),
            createRoom("R303","Family Grand Suite","2 King",5,new BigDecimal("2800000"),3,"/assets/images/accommodations/amenities/hotel/bedroom.jpg","2 phòng ngủ, bếp đầy đủ.",h3,RoomCategory.FAMILY,new BigDecimal("12.00")),
            createRoom("R304","Presidential Suite","1 King lớn",2,new BigDecimal("5500000"),1,"/assets/images/accommodations/amenities/hotel/bedroom.jpg","Suite lớn nhất, jacuzzi, xông hơi.",h3,RoomCategory.SUITE,new BigDecimal("20.00"))
        ));
        repo.save(h3);
        log.info("[DataInitializer] Initialized 3 hotels for HOTEL partner.");
    }

    private void initVillas(AccommodationRepository repo, User owner) {
        Accommodation v1 = new Accommodation();
        v1.setName("Ba Na Hills Forest Villa"); v1.setAddress("Núi Chúa, Hòa Ninh");
        v1.setCity("Đà Nẵng"); v1.setPropertyType(PropertyType.VILLA);
        v1.setApprovalStatus(ApprovalStatus.APPROVED); v1.setOwner(owner);
        v1.setThumbnailUrl("/assets/images/accommodations/catalog/villa-exterior.jpg"); v1.setRating(9.0);
        v1.setReviewCount(320); v1.setStarRating(5);
        v1.setDescription("Villa rừng thông tuyệt đẹp tại Bà Nà Hills, view núi hùng vĩ.");
        v1.setRooms(List.of(
            createRoom("V101","Phòng ngủ đôi Villa","1 King",2,new BigDecimal("2500000"),3,"/assets/images/accommodations/amenities/villa/bedroom.jpg","Phòng ngủ riêng view rừng.",v1,RoomCategory.DELUXE,null),
            createRoom("V102","Villa Master Suite","1 King lớn",4,new BigDecimal("4500000"),2,"/assets/images/accommodations/amenities/villa/bedroom.jpg","Suite riêng hồ bơi, spa.",v1,RoomCategory.SUITE,new BigDecimal("10.00"))
        ));
        repo.save(v1);
        log.info("[DataInitializer] Initialized 1 villa for VILLA partner.");
    }

    private void initHomestays(AccommodationRepository repo, User owner) {
        Accommodation hs1 = new Accommodation();
        hs1.setName("Hoa Lu Riverside Homestay"); hs1.setAddress("Thôn Hoa Lư, xã Ninh Hải");
        hs1.setCity("Ninh Bình"); hs1.setPropertyType(PropertyType.HOMESTAY);
        hs1.setApprovalStatus(ApprovalStatus.APPROVED); hs1.setOwner(owner);
        hs1.setThumbnailUrl("/assets/images/accommodations/catalog/homestay-exterior.jpg"); hs1.setRating(8.8);
        hs1.setReviewCount(215); hs1.setStarRating(3);
        hs1.setDescription("Homestay ven sông Hoàng Long, không khí trong lành, ăn sáng miễn phí.");
        hs1.setRooms(List.of(
            createRoom("HS101","Phòng Deluxe Riêng","1 Queen",2,new BigDecimal("650000"),4,"/assets/images/accommodations/amenities/homestay/bedroom.jpg","Phòng riêng view sông.",hs1,RoomCategory.DELUXE,null),
            createRoom("HS102","Phòng Gia Đình","2 tầng",5,new BigDecimal("950000"),2,"/assets/images/accommodations/amenities/homestay/bedroom.jpg","Phòng gia đình bếp nhỏ.",hs1,RoomCategory.FAMILY,null)
        ));
        repo.save(hs1);
        log.info("[DataInitializer] Initialized 1 homestay for HOMESTAY partner.");
    }

    private void initResorts(AccommodationRepository repo, User owner) {
        Accommodation r1 = new Accommodation();
        r1.setName("Furama Resort Đà Nẵng"); r1.setAddress("68 Hồ Xuân Hương, Mỹ An");
        r1.setCity("Đà Nẵng"); r1.setPropertyType(PropertyType.RESORT);
        r1.setApprovalStatus(ApprovalStatus.APPROVED); r1.setOwner(owner);
        r1.setThumbnailUrl("/assets/images/accommodations/catalog/resort-exterior.jpg"); r1.setRating(9.1);
        r1.setReviewCount(890); r1.setStarRating(5);
        r1.setDescription("Resort 5 sao bên bờ biển Mỹ Khê, hồ bơi vô cực, spa đẳng cấp.");
        r1.setRooms(List.of(
            createRoom("RS101","Deluxe Ocean View","1 King",2,new BigDecimal("3200000"),6,"/assets/images/accommodations/catalog/resort-room.jpg","View biển, bãi tắm riêng.",r1,RoomCategory.DELUXE,null),
            createRoom("RS102","Premium Pool Villa","1 King",2,new BigDecimal("5800000"),3,"/assets/images/accommodations/catalog/resort-room.jpg","Villa hồ bơi riêng.",r1,RoomCategory.VIP,new BigDecimal("12.00"))
        ));
        repo.save(r1);
        log.info("[DataInitializer] Initialized 1 resort for RESORT partner.");
    }

    private void initMissingDefaultAccommodations(AccommodationRepository accommodationRepository,
                                               RoomRepository roomRepository,
                                               User partnerHotel,
                                               User partnerResort,
                                               User partnerVilla,
                                               User partnerHomestay) {
        initDefaultAccommodationIfMissing(accommodationRepository, roomRepository, partnerHotel,
                "InterContinental Nha Trang",
                "32-34 Trần Phú, Lộc Thọ", "Nha Trang",
                "Khách sạn 5 sao quốc tế tọa lạc ngay trung tâm bãi biển Trần Phú, view biển toàn cảnh.",
                "/assets/images/accommodations/catalog/hotel-exterior.jpg",
                PropertyType.HOTEL, 5, 9.0, 1456,
                List.of(
                        new RoomSpec("ICN-STD", "Superior City View", "1 giường King", 2, "1850000", 10,
                                "Phòng 32m² hướng thành phố, tiện nghi chuẩn 5 sao.", "/assets/images/accommodations/amenities/hotel/bedroom.jpg", RoomCategory.STANDARD),
                        new RoomSpec("ICN-DLX", "Deluxe Ocean Front", "1 giường King cỡ lớn", 2, "3200000", 6,
                                "Phòng 42m² hướng biển, ban công riêng ngắm bình minh trên vịnh Nha Trang.", "/assets/images/accommodations/amenities/hotel/bedroom.jpg", RoomCategory.DELUXE)
                ));

        initDefaultAccommodationIfMissing(accommodationRepository, roomRepository, partnerHotel,
                "Novotel Đà Nẵng Premier",
                "36 Bạch Đằng, Hải Châu", "Đà Nẵng",
                "Khách sạn 4 sao hiện đại tọa lạc bên sông Hàn, cách bãi biển Mỹ Khê 5 phút.",
                "/assets/images/accommodations/catalog/hotel-exterior.jpg",
                PropertyType.HOTEL, 4, 8.7, 2103,
                List.of(
                        new RoomSpec("NVD-STD", "Standard River View", "1 giường Queen", 2, "1100000", 12,
                                "Phòng 28m² hướng sông Hàn, view cầu Rồng lung linh về đêm.", "/assets/images/accommodations/amenities/hotel/bedroom.jpg", RoomCategory.STANDARD),
                        new RoomSpec("NVD-DLX", "Deluxe Premium", "1 giường King", 2, "1650000", 8,
                                "Phòng 35m², tầng cao, bao gồm bữa sáng buffet.", "/assets/images/accommodations/amenities/hotel/bedroom.jpg", RoomCategory.DELUXE)
                ));

        initDefaultAccommodationIfMissing(accommodationRepository, roomRepository, partnerHotel,
                "Sofitel Legend Metropole Hà Nội",
                "15 Ngô Quyền, Tràng Tiền, Hoàn Kiếm", "Hà Nội",
                "Khách sạn lịch sử 5 sao nằm tại trung tâm quận Hoàn Kiếm, kiến trúc Pháp cổ điển.",
                "/assets/images/accommodations/catalog/hotel-exterior.jpg",
                PropertyType.HOTEL, 5, 9.4, 3201,
                List.of(
                        new RoomSpec("SLM-PRE", "Premium Room", "1 giường King", 2, "4500000", 8,
                                "Phòng 32m² khu Historical Wing, nội thất gỗ cổ điển.", "/assets/images/accommodations/amenities/hotel/bedroom.jpg", RoomCategory.DELUXE),
                        new RoomSpec("SLM-FAM", "Family Heritage", "2 giường Queen", 4, "7800000", 2,
                                "Phòng gia đình 52m², hai phòng ngủ, view Hồ Hoàn Kiếm.", "/assets/images/accommodations/amenities/hotel/bedroom.jpg", RoomCategory.FAMILY)
                ));

        initDefaultAccommodationIfMissing(accommodationRepository, roomRepository, partnerVilla,
                "Pine Hill Villa Đà Lạt",
                "12 Hoàng Hoa Thám, Phường 10", "Đà Lạt",
                "Villa riêng giữa đồi thông Đà Lạt, có bếp, sân BBQ và phòng khách rộng cho nhóm gia đình.",
                "/assets/images/accommodations/catalog/villa-exterior.jpg",
                PropertyType.VILLA, 4, 8.9, 216,
                List.of(
                        new RoomSpec("PHV-DLX", "Deluxe Pine Villa", "2 giường Queen", 4, "2400000", 3,
                                "Căn villa 2 phòng ngủ nhìn ra đồi thông, phù hợp gia đình nhỏ.", "/assets/images/accommodations/amenities/villa/bedroom.jpg", RoomCategory.DELUXE),
                        new RoomSpec("PHV-FAM", "Family BBQ Villa", "3 giường Queen", 6, "3600000", 2,
                                "Căn villa sân vườn, bếp riêng và khu BBQ ngoài trời.", "/assets/images/accommodations/amenities/villa/bedroom.jpg", RoomCategory.FAMILY)
                ));

        initDefaultAccommodationIfMissing(accommodationRepository, roomRepository, partnerVilla,
                "Sunset Beach Villa Phú Quốc",
                "Bãi Trường, Dương Tơ", "Phú Quốc",
                "Villa biển phía tây Phú Quốc, thích hợp nhóm bạn và gia đình muốn nghỉ dưỡng riêng tư.",
                "/assets/images/accommodations/catalog/villa-exterior.jpg",
                PropertyType.VILLA, 5, 9.1, 334,
                List.of(
                        new RoomSpec("SBV-SEA", "Sea Breeze Villa", "2 giường King", 4, "4200000", 3,
                                "Villa hai phòng ngủ gần biển, có ban công ngắm hoàng hôn.", "/assets/images/accommodations/amenities/villa/bedroom.jpg", RoomCategory.VIP),
                        new RoomSpec("SBV-POOL", "Private Pool Villa", "3 giường King", 6, "6800000", 1,
                                "Villa hồ bơi riêng cho nhóm lớn, có bếp và phòng khách.", "/assets/images/accommodations/amenities/villa/bedroom.jpg", RoomCategory.SUITE)
                ));

        initDefaultAccommodationIfMissing(accommodationRepository, roomRepository, partnerHomestay,
                "Tam Cốc Garden Homestay",
                "Đội 3, Văn Lâm, Ninh Hải", "Ninh Bình",
                "Homestay gần bến Tam Cốc, view núi đá vôi, có xe đạp miễn phí và bữa sáng địa phương.",
                "/assets/images/accommodations/catalog/homestay-exterior.jpg",
                PropertyType.HOMESTAY, 3, 8.8, 189,
                List.of(
                        new RoomSpec("TCG-STD", "Phòng Vườn Tiêu Chuẩn", "1 giường Queen", 2, "420000", 5,
                                "Phòng riêng nhìn ra vườn, phù hợp khách đi cặp đôi.", "/assets/images/accommodations/amenities/homestay/bedroom.jpg", RoomCategory.STANDARD),
                        new RoomSpec("TCG-FAM", "Phòng Gia Đình Tam Cốc", "2 giường Queen", 4, "720000", 3,
                                "Phòng gia đình rộng, có ban công nhìn núi đá vôi.", "/assets/images/accommodations/amenities/homestay/bedroom.jpg", RoomCategory.FAMILY)
                ));

        initDefaultAccommodationIfMissing(accommodationRepository, roomRepository, partnerHomestay,
                "Sapa Valley Homestay",
                "Lao Chải, Sa Pa", "Sa Pa",
                "Homestay bản làng nhìn ra thung lũng Mường Hoa, phù hợp du khách thích trekking và trải nghiệm địa phương.",
                "/assets/images/accommodations/catalog/homestay-exterior.jpg",
                PropertyType.HOMESTAY, 3, 8.7, 241,
                List.of(
                        new RoomSpec("SVH-STD", "Phòng Gỗ View Núi", "1 giường đôi", 2, "380000", 6,
                                "Phòng gỗ đơn giản, có cửa sổ nhìn ruộng bậc thang.", "/assets/images/accommodations/amenities/homestay/bedroom.jpg", RoomCategory.STANDARD),
                        new RoomSpec("SVH-DLX", "Deluxe Valley Room", "1 giường King", 2, "620000", 4,
                                "Phòng có ban công riêng, bao gồm bữa sáng địa phương.", "/assets/images/accommodations/amenities/homestay/bedroom.jpg", RoomCategory.DELUXE)
                ));

        initDefaultAccommodationIfMissing(accommodationRepository, roomRepository, partnerResort,
                "Sunset Pearl Resort Phú Quốc",
                "Bãi Trường, Dương Tơ", "Phú Quốc",
                "Resort ven biển phía tây đảo Phú Quốc, có hồ bơi ngoài trời và nhà hàng hải sản.",
                "/assets/images/accommodations/catalog/resort-exterior.jpg",
                PropertyType.RESORT, 5, 9.2, 876,
                List.of(
                        new RoomSpec("SPQ-DLX", "Deluxe Garden Room", "1 giường King", 2, "2100000", 8,
                                "Phòng 38m² hướng vườn nhiệt đới, bao gồm bữa sáng buffet.", "/assets/images/accommodations/catalog/resort-room.jpg", RoomCategory.DELUXE),
                        new RoomSpec("SPQ-SEA", "Ocean Sunset Suite", "1 giường King cỡ lớn", 2, "3900000", 4,
                                "Suite 58m² hướng biển, ban công riêng ngắm hoàng hôn.", "/assets/images/accommodations/catalog/resort-room.jpg", RoomCategory.SUITE)
                ));

        initDefaultAccommodationIfMissing(accommodationRepository, roomRepository, partnerResort,
                "Legacy Bay Resort Hạ Long",
                "Bãi Cháy, Hạ Long", "Quảng Ninh",
                "Resort nghỉ dưỡng bên vịnh Hạ Long, phù hợp khách tìm Quảng Ninh hoặc Hạ Long.",
                "/assets/images/accommodations/catalog/resort-exterior.jpg",
                PropertyType.RESORT, 5, 9.0, 512,
                List.of(
                        new RoomSpec("LBR-DLX", "Deluxe Bay View", "1 giường King", 2, "2600000", 7,
                                "Phòng hướng vịnh, ban công riêng và bữa sáng buffet.", "/assets/images/accommodations/catalog/resort-room.jpg", RoomCategory.DELUXE),
                        new RoomSpec("LBR-SUI", "Heritage Bay Suite", "1 giường King cỡ lớn", 2, "4800000", 3,
                                "Suite tầng cao nhìn toàn cảnh vịnh Hạ Long, có phòng khách riêng.", "/assets/images/accommodations/catalog/resort-room.jpg", RoomCategory.SUITE)
                ));
    }

    private void initDefaultAccommodationIfMissing(AccommodationRepository accommodationRepository,
                                                RoomRepository roomRepository,
                                                User owner,
                                                String name,
                                                String address,
                                                String city,
                                                String description,
                                                String thumbnailUrl,
                                                PropertyType propertyType,
                                                Integer starRating,
                                                Double rating,
                                                Integer reviewCount,
                                                List<RoomSpec> roomSpecs) {
        Accommodation accommodation = accommodationRepository.findFirstByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Accommodation acc = new Accommodation();
                    acc.setName(name);
                    acc.setAddress(address);
                    acc.setCity(city);
                    acc.setDescription(description);
                    acc.setThumbnailUrl(thumbnailUrl);
                    acc.setPropertyType(propertyType);
                    acc.setApprovalStatus(ApprovalStatus.APPROVED);
                    acc.setOwner(owner);
                    acc.setStarRating(starRating);
                    acc.setRating(rating);
                    acc.setReviewCount(reviewCount);
                    return accommodationRepository.save(acc);
                });

        for (RoomSpec roomSpec : roomSpecs) {
            if (roomRepository.existsByRoomCode(roomSpec.code())) {
                continue;
            }
            Room room = createRoom(
                    roomSpec.code(),
                    roomSpec.name(),
                    roomSpec.bedType(),
                    roomSpec.capacity(),
                    new BigDecimal(roomSpec.price()),
                    roomSpec.availableQuantity(),
                    roomSpec.imageUrl(),
                    roomSpec.description(),
                    accommodation,
                    roomSpec.category(),
                    null);
            roomRepository.save(room);
        }
    }

    private record RoomSpec(String code, String name, String bedType, int capacity,
                            String price, int availableQuantity, String description,
                            String imageUrl, RoomCategory category) {
    }

    // ── BACKFILL HELPERS ────────────────────────────────────────────────────

    /**
     * Gán owner cho accommodation chưa có owner, theo đúng propertyType.
     */
    private void backfillOwnerByType(AccommodationRepository repo, User owner, PropertyType type) {
        List<Accommodation> unowned = repo.findByOwnerIsNullAndPropertyType(type);
        if (!unowned.isEmpty()) {
            unowned.forEach(a -> a.setOwner(owner));
            repo.saveAll(unowned);
            log.info("[DataInitializer] Backfill owner for {} {} accommodations.", unowned.size(), type.name());
        }
    }

    /**
     * Reassign accommodation bị gán nhầm owner (sai loại) về đúng partner.
     * VD: Villa bị gán vào partnerHotel → chuyển sang partnerVilla.
     */
    private void reassignWrongOwner(AccommodationRepository repo,
                                     User wrongOwner, User correctOwner, PropertyType type) {
        List<Accommodation> all = repo.findByOwner(wrongOwner);
        List<Accommodation> wrong = all.stream()
                .filter(a -> a.getPropertyType() == type)
                .toList();
        if (!wrong.isEmpty()) {
            wrong.forEach(a -> a.setOwner(correctOwner));
            repo.saveAll(wrong);
            log.info("[DataInitializer] Reassign {} {} accommodations from {} to {}.",
                    wrong.size(), type.name(), wrongOwner.getEmail(), correctOwner.getEmail());
        }
    }

    private void backfillRoomApprovalStatusIfMissing(RoomRepository roomRepository) {
        List<Room> nullStatusRooms = roomRepository.findByApprovalStatusIsNull();
        if (!nullStatusRooms.isEmpty()) {
            nullStatusRooms.forEach(r -> r.setApprovalStatus(ApprovalStatus.APPROVED));
            roomRepository.saveAll(nullStatusRooms);
            log.info("[DataInitializer] Backfill APPROVED status for {} rooms.", nullStatusRooms.size());
        }
    }

    private void backfillRoomCategoryAndCommission(RoomRepository roomRepository) {
        List<Room> rooms = roomRepository.findAll();
        long count = 0;
        for (Room r : rooms) {
            if (r.getRoomCategory() != null) continue;
            String name = r.getRoomName() != null ? r.getRoomName().toLowerCase() : "";
            if (name.contains("vip")) {
                r.setRoomCategory(RoomCategory.VIP);
                if (r.getCommissionRateOverride() == null) r.setCommissionRateOverride(new BigDecimal("20.00"));
            } else if (name.contains("suite") || name.contains("presidential")) {
                r.setRoomCategory(RoomCategory.SUITE);
                if (r.getCommissionRateOverride() == null) r.setCommissionRateOverride(new BigDecimal("20.00"));
            } else if (name.contains("gia đình") || name.contains("family") || name.contains("grand")) {
                r.setRoomCategory(RoomCategory.FAMILY);
            } else if (name.contains("deluxe") || name.contains("premium") || name.contains("superior")) {
                r.setRoomCategory(RoomCategory.DELUXE);
            } else {
                r.setRoomCategory(RoomCategory.STANDARD);
            }
            count++;
        }
        if (count > 0) {
            roomRepository.saveAll(rooms);
            log.info("[DataInitializer] Backfill RoomCategory for {} rooms.", count);
        }
    }

    private Room createRoom(String roomCode, String roomName, String bedType,
                             int capacity, BigDecimal pricePerNight, int availableQuantity,
                             String imageUrl, String description, Accommodation accommodation,
                             RoomCategory roomCategory, BigDecimal commissionRateOverride) {
        Room room = new Room();
        room.setRoomCode(roomCode);
        room.setRoomName(roomName);
        room.setBedType(bedType);
        room.setCapacity(capacity);
        room.setPricePerNight(pricePerNight);
        room.setAvailableQuantity(availableQuantity);
        room.setImageUrl(imageUrl);
        room.setDescription(description);
        room.setAccommodation(accommodation);
        room.setApprovalStatus(ApprovalStatus.APPROVED);
        room.setRoomCategory(roomCategory != null ? roomCategory : RoomCategory.STANDARD);
        room.setCommissionRateOverride(commissionRateOverride);
        return room;
    }

    /**
     * Fix settlement notes: cập nhật ghi chú quyết toán PAID theo đúng partner.
     * Loại bỏ tình trạng note ghi "Hoa Lu Homestay" nhưng partner là RESORT.
     */
    private void fixSettlementNotes(PartnerSettlementRepository repo,
                                    User partnerHotel, User partnerResort,
                                    User partnerVilla, User partnerHomestay) {
        java.util.Map<User, String[]> partnerAccomNames = new java.util.HashMap<>();

        // Tên đúng cho từng partner (Hotel)
        partnerAccomNames.put(partnerHotel, new String[]{
            "LATA Hotel", "Tulip Hotel", "TravelMate Grand Hotel",
            "Sunrise Grand Hotel", "Mường Thanh Grand", "InterContinental", "Sofitel"
        });
        // Tên đúng cho Resort
        partnerAccomNames.put(partnerResort, new String[]{
            "Vinpearl Resort", "Furama Resort", "Blue Ocean Resort", "Sunset Pearl Resort",
            "Legacy Bay Resort", "Azerai Cần Thơ Resort", "TTC Resort"
        });
        // Tên đúng cho Villa
        partnerAccomNames.put(partnerVilla, new String[]{
            "Anam Villas", "La Siesta Villa", "Ba Na Hills Forest Villa", "Green Hills Villa"
        });
        // Tên đúng cho Homestay
        partnerAccomNames.put(partnerHomestay, new String[]{
            "Hoa Lu Riverside Homestay", "Mộc Nhiên Homestay", "Mekong Homestay", "Sapa Valley Homestay"
        });

        // Tên gợi ý tốt nhất để dùng trong note cho từng partner type
        java.util.Map<User, String> partnerRepresentative = new java.util.HashMap<>();
        partnerRepresentative.put(partnerHotel, "LATA Hotel 1 booking");
        partnerRepresentative.put(partnerResort, "Vinpearl Resort 1 booking");
        partnerRepresentative.put(partnerVilla, "Anam Villas 1 booking");
        partnerRepresentative.put(partnerHomestay, "Hoa Lu Riverside Homestay 1 booking");

        java.util.List<PartnerSettlement> all = repo.findAllByOrderByCreatedAtDesc();
        int fixCount = 0;

        for (PartnerSettlement s : all) {
            User partner = s.getPartner();
            if (partner == null) continue;

            String[] correctNames = partnerAccomNames.get(partner);
            if (correctNames == null) continue;

            String note = s.getNote();
            if (note == null || note.isBlank()) continue;

            // Kiểm tra note có tên của loại lưu trú KHÁC không
            boolean contaminated = false;
            for (java.util.Map.Entry<User, String[]> entry : partnerAccomNames.entrySet()) {
                if (entry.getKey().getId().equals(partner.getId())) continue;
                for (String wrongName : entry.getValue()) {
                    if (note.contains(wrongName)) {
                        contaminated = true;
                        break;
                    }
                }
                if (contaminated) break;
            }

            if (contaminated) {
                // Tạo note mới sạch, thay thế note sai bằng ghi chú hợp lệ cho đối tác này
                String typeName = partner.getPartnerPropertyType() != null
                    ? partner.getPartnerPropertyType().name() : "property";
                s.setNote("Đã thanh toán cho đối tác " + partner.getName() + " (" + typeName + ").");
                fixCount++;
            }
        }

        if (fixCount > 0) {
            repo.saveAll(java.util.Collections.unmodifiableList(all));
            log.info("[DataInitializer] Fixed {} settlement notes with mismatched property data.", fixCount);
        }
    }
}

