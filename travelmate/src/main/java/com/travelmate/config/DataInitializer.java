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
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

/**
 * DataInitializer — Seed dữ liệu mẫu khi app khởi động.
 *
 * Rule:
 *   - partner1 (HOTEL)    → sở hữu Hotel
 *   - partner2 (RESORT)   → sở hữu Resort
 *   - partner3 (VILLA)    → sở hữu Villa
 *   - partner4 (HOMESTAY) → sở hữu Homestay
 */
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initDemoData(UserRepository userRepository,
                                          AccommodationRepository accommodationRepository,
                                          RoomRepository roomRepository,
                                          PartnerSettlementRepository settlementRepository,
                                          PasswordEncoder passwordEncoder) {
        return args -> {

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

            // ── SEED HOTEL (nếu chưa có) ─────────────────────────────────────
            long hotelCount = accommodationRepository.countByPropertyType(PropertyType.HOTEL);
            if (hotelCount == 0) {
                seedHotels(accommodationRepository, partnerHotel);
            }

            // ── SEED VILLA (nếu chưa có) ─────────────────────────────────────
            long villaCount = accommodationRepository.countByPropertyType(PropertyType.VILLA);
            if (villaCount == 0) {
                seedVillas(accommodationRepository, partnerVilla);
            }

            // ── SEED HOMESTAY (nếu chưa có) ──────────────────────────────────
            long homestayCount = accommodationRepository.countByPropertyType(PropertyType.HOMESTAY);
            if (homestayCount == 0) {
                seedHomestays(accommodationRepository, partnerHomestay);
            }

            // ── SEED RESORT (nếu chưa có) ────────────────────────────────────
            long resortCount = accommodationRepository.countByPropertyType(PropertyType.RESORT);
            if (resortCount == 0) {
                seedResorts(accommodationRepository, partnerResort);
            }

            // ── FIX: Cập nhật settlement notes khớp đúng với partner ─────────
            fixSettlementNotes(settlementRepository, partnerHotel, partnerResort, partnerVilla, partnerHomestay);

            System.out.println("🎉 [DataInitializer] Hoàn tất seed data!");
        };
    }

    // ── SEED METHODS ────────────────────────────────────────────────────────

    private void seedHotels(AccommodationRepository repo, User owner) {
        Accommodation h1 = new Accommodation();
        h1.setName("LATA Hotel & Apartments"); h1.setAddress("15 Phan Bội Châu, P1");
        h1.setCity("Đà Lạt"); h1.setPropertyType(PropertyType.HOTEL);
        h1.setApprovalStatus(ApprovalStatus.APPROVED); h1.setOwner(owner);
        h1.setThumbnailUrl("/assets/images/resort.jpg"); h1.setRating(8.6);
        h1.setReviewCount(771); h1.setStarRating(4);
        h1.setDescription("Khách sạn hiện đại trung tâm Đà Lạt, cách chợ đêm 500m.");
        h1.setRooms(List.of(
            createRoom("R101","Phòng Tiêu Chuẩn King","1 giường King",2,new BigDecimal("650000"),5,"/assets/images/resort.jpg","Phòng tiêu chuẩn view thành phố.",h1,RoomCategory.STANDARD,null),
            createRoom("R102","Phòng Deluxe Đôi","2 giường đơn",3,new BigDecimal("850000"),4,"/assets/images/homestay-o-da-lat.jpg","Phòng Deluxe ban công, minibar.",h1,RoomCategory.DELUXE,null),
            createRoom("R103","Phòng Gia Đình","King + đơn",4,new BigDecimal("1200000"),3,"/assets/images/homestay-sapa.jpg","Phòng rộng view núi, bồn tắm.",h1,RoomCategory.FAMILY,new BigDecimal("12.00")),
            createRoom("R104","Suite Cao Cấp","1 King size",2,new BigDecimal("1800000"),2,"/assets/images/villa-don-lap.jpg","Suite jacuzzi, view toàn cảnh.",h1,RoomCategory.SUITE,new BigDecimal("20.00"))
        ));
        repo.save(h1);

        Accommodation h2 = new Accommodation();
        h2.setName("Tulip Hotel 2 Dalat"); h2.setAddress("56 Bùi Thị Xuân, P2");
        h2.setCity("Đà Lạt"); h2.setPropertyType(PropertyType.HOTEL);
        h2.setApprovalStatus(ApprovalStatus.APPROVED); h2.setOwner(owner);
        h2.setThumbnailUrl("/assets/images/homestay-o-da-lat.jpg"); h2.setRating(8.2);
        h2.setReviewCount(456); h2.setStarRating(3);
        h2.setDescription("Khách sạn 3 sao phong cách Châu Âu, gần hồ Xuân Hương.");
        h2.setRooms(List.of(
            createRoom("R201","Standard Twin","2 giường đơn",2,new BigDecimal("480000"),6,"/assets/images/resort.jpg","Phòng sạch sẽ, wifi.",h2,RoomCategory.STANDARD,null),
            createRoom("R202","Superior Double","1 giường đôi",2,new BigDecimal("620000"),5,"/assets/images/homestay-o-da-lat.jpg","Ban công nhỏ nhìn ra phố.",h2,RoomCategory.DELUXE,null),
            createRoom("R203","Gia Đình Rộng","Đôi + 2 đơn",5,new BigDecimal("1050000"),3,"/assets/images/homestay-sapa.jpg","Phòng lớn 4-5 người.",h2,RoomCategory.FAMILY,new BigDecimal("12.00")),
            createRoom("R204","VIP Panorama","1 King",2,new BigDecimal("1500000"),2,"/assets/images/villa-don-lap.jpg","View 360, bồn tắm nóng.",h2,RoomCategory.VIP,new BigDecimal("18.00"))
        ));
        repo.save(h2);

        Accommodation h3 = new Accommodation();
        h3.setName("TravelMate Grand Hotel"); h3.setAddress("88 Nguyễn Chí Thanh, P6");
        h3.setCity("Đà Lạt"); h3.setPropertyType(PropertyType.HOTEL);
        h3.setApprovalStatus(ApprovalStatus.APPROVED); h3.setOwner(owner);
        h3.setThumbnailUrl("/assets/images/villa-don-lap.jpg"); h3.setRating(9.2);
        h3.setReviewCount(1205); h3.setStarRating(5);
        h3.setDescription("Khách sạn 5 sao sang trọng trên đồi thông, spa cao cấp.");
        h3.setRooms(List.of(
            createRoom("R301","Deluxe Garden View","1 King",2,new BigDecimal("1200000"),8,"/assets/images/resort.jpg","View vườn thông, ban công.",h3,RoomCategory.DELUXE,null),
            createRoom("R302","Premium Valley View","King/2đơn",3,new BigDecimal("1650000"),5,"/assets/images/homestay-o-da-lat.jpg","View thung lũng, phòng tắm kính.",h3,RoomCategory.VIP,new BigDecimal("18.00")),
            createRoom("R303","Family Grand Suite","2 King",5,new BigDecimal("2800000"),3,"/assets/images/homestay-sapa.jpg","2 phòng ngủ, bếp đầy đủ.",h3,RoomCategory.FAMILY,new BigDecimal("12.00")),
            createRoom("R304","Presidential Suite","1 King lớn",2,new BigDecimal("5500000"),1,"/assets/images/villa-don-lap.jpg","Suite lớn nhất, jacuzzi, xông hơi.",h3,RoomCategory.SUITE,new BigDecimal("20.00"))
        ));
        repo.save(h3);
        System.out.println("✅ [DataInitializer] Seed 3 Hotels cho partner HOTEL");
    }

    private void seedVillas(AccommodationRepository repo, User owner) {
        Accommodation v1 = new Accommodation();
        v1.setName("Ba Na Hills Forest Villa"); v1.setAddress("Núi Chúa, Hòa Ninh");
        v1.setCity("Đà Nẵng"); v1.setPropertyType(PropertyType.VILLA);
        v1.setApprovalStatus(ApprovalStatus.APPROVED); v1.setOwner(owner);
        v1.setThumbnailUrl("/assets/images/villa-don-lap.jpg"); v1.setRating(9.0);
        v1.setReviewCount(320); v1.setStarRating(5);
        v1.setDescription("Villa rừng thông tuyệt đẹp tại Bà Nà Hills, view núi hùng vĩ.");
        v1.setRooms(List.of(
            createRoom("V101","Phòng ngủ đôi Villa","1 King",2,new BigDecimal("2500000"),3,"/assets/images/villa-don-lap.jpg","Phòng ngủ riêng view rừng.",v1,RoomCategory.DELUXE,null),
            createRoom("V102","Villa Master Suite","1 King lớn",4,new BigDecimal("4500000"),2,"/assets/images/villa-don-lap.jpg","Suite riêng hồ bơi, spa.",v1,RoomCategory.SUITE,new BigDecimal("10.00"))
        ));
        repo.save(v1);
        System.out.println("✅ [DataInitializer] Seed 1 Villa cho partner VILLA");
    }

    private void seedHomestays(AccommodationRepository repo, User owner) {
        Accommodation hs1 = new Accommodation();
        hs1.setName("Hoa Lu Riverside Homestay"); hs1.setAddress("Thôn Hoa Lư, xã Ninh Hải");
        hs1.setCity("Ninh Bình"); hs1.setPropertyType(PropertyType.HOMESTAY);
        hs1.setApprovalStatus(ApprovalStatus.APPROVED); hs1.setOwner(owner);
        hs1.setThumbnailUrl("/assets/images/homestay-sapa.jpg"); hs1.setRating(8.8);
        hs1.setReviewCount(215); hs1.setStarRating(3);
        hs1.setDescription("Homestay ven sông Hoàng Long, không khí trong lành, ăn sáng miễn phí.");
        hs1.setRooms(List.of(
            createRoom("HS101","Phòng Deluxe Riêng","1 Queen",2,new BigDecimal("650000"),4,"/assets/images/homestay-sapa.jpg","Phòng riêng view sông.",hs1,RoomCategory.DELUXE,null),
            createRoom("HS102","Phòng Gia Đình","2 tầng",5,new BigDecimal("950000"),2,"/assets/images/homestay-o-da-lat.jpg","Phòng gia đình bếp nhỏ.",hs1,RoomCategory.FAMILY,null)
        ));
        repo.save(hs1);
        System.out.println("✅ [DataInitializer] Seed 1 Homestay cho partner HOMESTAY");
    }

    private void seedResorts(AccommodationRepository repo, User owner) {
        Accommodation r1 = new Accommodation();
        r1.setName("Furama Resort Đà Nẵng"); r1.setAddress("68 Hồ Xuân Hương, Mỹ An");
        r1.setCity("Đà Nẵng"); r1.setPropertyType(PropertyType.RESORT);
        r1.setApprovalStatus(ApprovalStatus.APPROVED); r1.setOwner(owner);
        r1.setThumbnailUrl("/assets/images/resort.jpg"); r1.setRating(9.1);
        r1.setReviewCount(890); r1.setStarRating(5);
        r1.setDescription("Resort 5 sao bên bờ biển Mỹ Khê, hồ bơi vô cực, spa đẳng cấp.");
        r1.setRooms(List.of(
            createRoom("RS101","Deluxe Ocean View","1 King",2,new BigDecimal("3200000"),6,"/assets/images/resort.jpg","View biển, bãi tắm riêng.",r1,RoomCategory.DELUXE,null),
            createRoom("RS102","Premium Pool Villa","1 King",2,new BigDecimal("5800000"),3,"/assets/images/villa-don-lap.jpg","Villa hồ bơi riêng.",r1,RoomCategory.VIP,new BigDecimal("12.00"))
        ));
        repo.save(r1);
        System.out.println("✅ [DataInitializer] Seed 1 Resort cho partner RESORT");
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
            System.out.println("🔧 [DataInitializer] Backfill owner cho " + unowned.size() + " " + type.name());
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
            System.out.println("🔧 [DataInitializer] Reassign " + wrong.size() + " " + type.name()
                    + " từ " + wrongOwner.getEmail() + " → " + correctOwner.getEmail());
        }
    }

    private void backfillRoomApprovalStatusIfMissing(RoomRepository roomRepository) {
        List<Room> nullStatusRooms = roomRepository.findByApprovalStatusIsNull();
        if (!nullStatusRooms.isEmpty()) {
            nullStatusRooms.forEach(r -> r.setApprovalStatus(ApprovalStatus.APPROVED));
            roomRepository.saveAll(nullStatusRooms);
            System.out.println("🔧 [DataInitializer] Backfill APPROVED cho " + nullStatusRooms.size() + " phòng.");
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
                if (r.getCommissionRateOverride() == null) r.setCommissionRateOverride(new BigDecimal("18.00"));
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
            System.out.println("🔧 [DataInitializer] Backfill RoomCategory cho " + count + " phòng.");
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
            "Sunrise Grand Hotel", "Mường Thanh Grand"
        });
        // Tên đúng cho Resort
        partnerAccomNames.put(partnerResort, new String[]{
            "Vinpearl Resort", "Furama Resort", "Blue Ocean Resort", "InterContinental"
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
                // Thay note bằng ghi chú hợp lệ cho partner này
                String rep = partnerRepresentative.getOrDefault(partner, "booking thuộc cơ sở");
                String newNote = note
                    // Giữ lại phần tuần số nếu có
                    .replaceAll("Tuần \\d+:", "").trim();
                // Tạo note mới sạch
                String typeName = partner.getPartnerPropertyType() != null
                    ? partner.getPartnerPropertyType().name() : "property";
                s.setNote("Đã thanh toán cho partner " + partner.getName() + " (" + typeName + ").");
                fixCount++;
            }
        }

        if (fixCount > 0) {
            repo.saveAll(all);
            System.out.println("🔧 [DataInitializer] Đã sửa " + fixCount + " settlement note bị lẫn dữ liệu sai.");
        }
    }
}

