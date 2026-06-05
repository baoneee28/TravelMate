-- =======================================================================
-- TRAVELMATE - DỮ LIỆU KHỞI TẠO PHỤC VỤ TRÌNH BÀY VÀ KIỂM THỬ
-- =======================================================================
-- Sử dụng FILE NÀY khi khởi tạo CSDL trình bày, không dùng file export cũ.
-- Last verified: 2026-06-05
--
-- Nội dung bao gồm:
--   • Users       : 1 ADMIN, 5 USER, 8 PARTNER (đủ loại HOTEL/VILLA/HOMESTAY/RESORT)
--   • Accommodations: 40+ cơ sở APPROVED + PENDING/REJECTED phục vụ trình bày nghiệp vụ
--   • Rooms       : 100+ phòng đa dạng với tiện nghi và commission_rate_override theo từng loại phòng
--   • Amenities   : tiện nghi mẫu
--   • Bookings    : 60+ booking bao gồm đủ trạng thái nghiệp vụ
--                   (PENDING_PAYMENT, CONFIRMED, CHECKED_IN,
--                    COMPLETED, NO_SHOW, CANCELLED — với cả DEPOSIT_30 và FULL_PAYMENT)
--   • Payments    : PENDING_PAYMENT, APPROVED, FAILED, CANCELLED, EXPIRED,
--                   DEPOSIT_FORFEITED, REFUND_PENDING, REFUNDED
--   • Vouchers    : ưu đãi toàn hệ thống và kho voucher theo phòng/căn (USER_GLOBAL/PARTNER_ROOM)
--   • Settlements : settlement PAID/PENDING theo tháng, không dùng dữ liệu tạm
--   • Partner Wallets: ví quyết toán nội bộ, lịch sử tiền vào/ra, yêu cầu rút tiền mẫu
--                     (đủ dữ liệu cho màn hình ví và export Excel withdrawal)
--   • Reviews     : 40+ đánh giá sau booking COMPLETED
--   • Support Tickets: 20+ yêu cầu hỗ trợ đa loại
--   • Travel Data : Destinations + Posts
--
-- TÀI KHOẢN TRÌNH BÀY:
--   ADMIN  : admin@travelmate.vn   / admin123
--   USER   : user@travelmate.vn    / user123
--   USER2  : user2@travelmate.vn   / user123
--   USER3  : user3@travelmate.vn   / user123
--   FAMILY : family@travelmate.vn  / user123
--   COUPLE : couple@travelmate.vn  / user123
--   PARTNER: partner@travelmate.vn / partner123  (HOTEL — Đà Lạt)
--   PARTNER2: partner2@travelmate.vn / partner123 (RESORT — Nha Trang/Đà Nẵng)
--   PARTNER3: partner3@travelmate.vn / partner123 (VILLA)
--   PARTNER4: partner4@travelmate.vn / partner123 (HOMESTAY)
--   RESORT : resort@travelmate.vn  / partner123 (RESORT)
--   VILLA  : villa@travelmate.vn   / partner123 (VILLA)
--   HOMESTAY: homestay@travelmate.vn / partner123 (HOMESTAY)
--   NO-BANK: no-bank@travelmate.vn / partner123 (HOTEL, chưa cấu hình ngân hàng)
-- CÁCH IMPORT:
--   mysql -u root -p < travelmate_db.sql
--   hoặc: DBeaver / MySQL Workbench → Run SQL Script → chọn file này
-- =======================================================================

    CREATE DATABASE IF NOT EXISTS travelmate_db
        DEFAULT CHARACTER SET utf8mb4
        DEFAULT COLLATE utf8mb4_unicode_ci;

    USE travelmate_db;
    SET FOREIGN_KEY_CHECKS = 0;
    SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
    SET SQL_SAFE_UPDATES = 0;


    -- =============================================
    -- DROP theo đúng thứ tự (FK phụ thuộc)
    -- =============================================
    DROP TABLE IF EXISTS travel_posts;
    DROP TABLE IF EXISTS travel_destinations;
    DROP TABLE IF EXISTS notifications;
    DROP TABLE IF EXISTS admin_action_logs;
    DROP TABLE IF EXISTS support_tickets;
    DROP TABLE IF EXISTS reviews;
    DROP TABLE IF EXISTS partner_wallet_transactions;
    DROP TABLE IF EXISTS partner_withdrawal_requests;
    DROP TABLE IF EXISTS partner_wallets;
    DROP TABLE IF EXISTS partner_settlements;
    DROP TABLE IF EXISTS payments;
    DROP TABLE IF EXISTS bookings;
    DROP TABLE IF EXISTS room_voucher_assignments;
    DROP TABLE IF EXISTS vouchers;
    DROP TABLE IF EXISTS room_amenities;
    DROP TABLE IF EXISTS amenities;
    DROP TABLE IF EXISTS room_images;
    DROP TABLE IF EXISTS rooms;
    DROP TABLE IF EXISTS accommodations;
    DROP TABLE IF EXISTS password_reset_tokens;
    DROP TABLE IF EXISTS users;

    -- =============================================
    -- 1. BẢNG USERS
    -- =============================================
    CREATE TABLE users (
        id                    BIGINT       NOT NULL AUTO_INCREMENT,
        email                 VARCHAR(255) NOT NULL UNIQUE,
        password              VARCHAR(255) NOT NULL,
        full_name             VARCHAR(255),
        name                  VARCHAR(255),
        phone                 VARCHAR(50),
        role                  VARCHAR(20)  NOT NULL DEFAULT 'USER',
        status                VARCHAR(20)           DEFAULT 'ACTIVE',
        partner_property_type VARCHAR(20)           DEFAULT NULL,
        -- Thông tin ngân hàng dùng cho quyết toán partner
        bank_account_number   VARCHAR(30)           DEFAULT NULL,
        bank_name             VARCHAR(100)          DEFAULT NULL,
        bank_account_holder   VARCHAR(100)          DEFAULT NULL,
        bank_branch           VARCHAR(100)          DEFAULT NULL,
        avatar_url            VARCHAR(255)          DEFAULT NULL,
        created_at            DATETIME(6),
        updated_at            DATETIME(6),
        PRIMARY KEY (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- =============================================
    -- 1a. BẢNG PASSWORD_RESET_TOKENS — Luồng quên mật khẩu
    -- =============================================
    CREATE TABLE password_reset_tokens (
        id         BIGINT       NOT NULL AUTO_INCREMENT,
        user_id    BIGINT       NOT NULL,
        token      VARCHAR(100) NOT NULL UNIQUE,
        expires_at DATETIME(6)  NOT NULL,
        used       TINYINT(1)   NOT NULL DEFAULT 0,
        created_at DATETIME(6)  NOT NULL,
        PRIMARY KEY (id),
        CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id),
        INDEX idx_password_reset_token (token),
        INDEX idx_password_reset_user_used (user_id, used)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- =============================================
    -- 2. BẢNG ACCOMMODATIONS — thêm owner_id
    -- =============================================
    CREATE TABLE accommodations (
        id              BIGINT        NOT NULL AUTO_INCREMENT,
        name            VARCHAR(255)  NOT NULL,
        description     TEXT,
        address         VARCHAR(500),
        city            VARCHAR(255),
        thumbnail_url   VARCHAR(1000),
        star_rating     INT,
        rating          DOUBLE,
        review_count    INT,
        property_type   VARCHAR(50)   DEFAULT 'HOTEL',
        approval_status VARCHAR(50)   DEFAULT 'APPROVED',
        owner_id        BIGINT,
        created_at      DATETIME(6),
        updated_at      DATETIME(6),
        PRIMARY KEY (id),
        CONSTRAINT fk_accommodations_owner FOREIGN KEY (owner_id) REFERENCES users (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- =============================================
    -- 3. BẢNG ROOMS (v2 — thêm room_category + commission_rate_override)
    -- =============================================
    CREATE TABLE rooms (
        id                      BIGINT        NOT NULL AUTO_INCREMENT,
        accommodation_id        BIGINT        NOT NULL,
        room_code               VARCHAR(50),
        room_name               VARCHAR(255)  NOT NULL,
        bed_type                VARCHAR(255),
        capacity                INT           DEFAULT 2,
        price_per_night         DECIMAL(15,0) NOT NULL,
        available_quantity      INT           DEFAULT 1,
        description             TEXT,
        image_url               VARCHAR(1000),
        approval_status         VARCHAR(50)   DEFAULT 'APPROVED',
        --
        -- v2: Room Category & Commission Override
        --
        -- room_category: phân loại phòng theo hạng, dùng để kiểm tra commission linh hoạt
        --   Giá trị: STANDARD | DELUXE | FAMILY | VIP | SUITE | OTHER
        room_category           VARCHAR(30)   DEFAULT 'STANDARD',
        --
        -- available_for_booking: Partner/Admin có thể tạm ngừng mở bán online
        --   1 (TRUE)  = đang mở bán, User thấy phòng này
        --   0 (FALSE) = tạm ngừng, chỉ Partner/Admin thấy
        available_for_booking   TINYINT(1)    NOT NULL DEFAULT 1,
        --
        -- commission_rate_override: tỷ lệ hoa hồng riêng cho phòng này (đơn vị %)
        --   null    → dùng mặc định theo PropertyType (HOTEL=15%, VILLA=12%, HOMESTAY=10%, RESORT=18%)
        --   có giá trị → áp dụng rate này (VD: 18.00 nghĩa là 18%)
        commission_rate_override DECIMAL(5,2) DEFAULT NULL,
        --
        PRIMARY KEY (id),
        UNIQUE KEY uk_rooms_room_code (room_code),
        CONSTRAINT fk_rooms_accommodation FOREIGN KEY (accommodation_id) REFERENCES accommodations (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- =============================================
    -- 3a. BẢNG ROOM_IMAGES — Ảnh phòng/căn đã được Admin duyệt
    -- =============================================
    CREATE TABLE room_images (
        id         BIGINT        NOT NULL AUTO_INCREMENT,
        room_id    BIGINT        NOT NULL,
        image_url  VARCHAR(1000) NOT NULL,
        caption    VARCHAR(255),
        sort_order INT           NOT NULL DEFAULT 0,
        is_primary TINYINT(1)    NOT NULL DEFAULT 0,
        created_at DATETIME(6),
        updated_at DATETIME(6),
        PRIMARY KEY (id),
        CONSTRAINT fk_room_images_room FOREIGN KEY (room_id) REFERENCES rooms (id),
        INDEX idx_room_images_room_order (room_id, is_primary, sort_order, id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- =============================================
    -- 3b. BẢNG AMENITIES — Tiện nghi phòng/căn lưu trú
    -- =============================================
    CREATE TABLE amenities (
        id       BIGINT       NOT NULL AUTO_INCREMENT,
        name     VARCHAR(100) NOT NULL,
        icon     VARCHAR(20),
        category VARCHAR(50),
        PRIMARY KEY (id),
        UNIQUE KEY uk_amenity_name_category (name, category)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- =============================================
    -- 3c. BẢNG ROOM_AMENITIES — Bảng join N-N giữa rooms và amenities
    -- =============================================
    CREATE TABLE room_amenities (
        room_id    BIGINT NOT NULL,
        amenity_id BIGINT NOT NULL,
        PRIMARY KEY (room_id, amenity_id),
        CONSTRAINT fk_ra_room    FOREIGN KEY (room_id)    REFERENCES rooms (id),
        CONSTRAINT fk_ra_amenity FOREIGN KEY (amenity_id) REFERENCES amenities (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- =============================================
    -- 3d. BẢNG VOUCHERS (phải sau rooms vì FK đến accommodations + rooms)
    -- =============================================
    CREATE TABLE vouchers (
        id                  BIGINT        NOT NULL AUTO_INCREMENT,
        code                VARCHAR(50)   NOT NULL UNIQUE,
        name                VARCHAR(255),
        description         TEXT,
        discount_type       VARCHAR(20)   NOT NULL DEFAULT 'PERCENT',
        discount_value      DECIMAL(15,2) NOT NULL DEFAULT 0,
        max_discount_amount DECIMAL(15,0),
        min_order_amount    DECIMAL(15,0) DEFAULT 0,
        start_date          DATE,
        end_date            DATE,
        active              TINYINT(1)    DEFAULT 1,
        voucher_scope       VARCHAR(30)   NOT NULL DEFAULT 'USER_GLOBAL',
        property_type       VARCHAR(20)   DEFAULT NULL,
        cost_bearer         VARCHAR(20)   NOT NULL DEFAULT 'ADMIN',
        owner_id            BIGINT,
        accommodation_id    BIGINT,
        room_id             BIGINT,
        created_at          DATETIME(6),
        PRIMARY KEY (id),
        CONSTRAINT fk_vouchers_owner         FOREIGN KEY (owner_id)         REFERENCES users (id),
        CONSTRAINT fk_vouchers_accommodation FOREIGN KEY (accommodation_id) REFERENCES accommodations (id),
        CONSTRAINT fk_vouchers_room          FOREIGN KEY (room_id)          REFERENCES rooms (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- =============================================
    -- 3e. BẢNG ROOM_VOUCHER_ASSIGNMENTS — Partner gắn voucher vào phòng/căn
    -- =============================================
    CREATE TABLE room_voucher_assignments (
        id                     BIGINT      NOT NULL AUTO_INCREMENT,
        voucher_id             BIGINT      NOT NULL,
        room_id                BIGINT      NOT NULL,
        assigned_by_partner_id BIGINT      NOT NULL,
        active                 TINYINT(1)  NOT NULL DEFAULT 1,
        assigned_at            DATETIME(6),
        PRIMARY KEY (id),
        UNIQUE KEY uk_room_voucher (room_id, voucher_id),
        CONSTRAINT fk_rva_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers (id),
        CONSTRAINT fk_rva_room FOREIGN KEY (room_id) REFERENCES rooms (id),
        CONSTRAINT fk_rva_partner FOREIGN KEY (assigned_by_partner_id) REFERENCES users (id),
        INDEX idx_rva_partner_assigned (assigned_by_partner_id, assigned_at),
        INDEX idx_rva_room_active (room_id, active)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- =============================================
    -- 4. BẢNG BOOKINGS
    -- =============================================
    CREATE TABLE bookings (
        id               BIGINT        NOT NULL AUTO_INCREMENT,
        booking_code     VARCHAR(50)   UNIQUE,
        user_id          BIGINT        NOT NULL,
        accommodation_id BIGINT        NOT NULL,
        room_id          BIGINT        NOT NULL,
        check_in         DATE          NOT NULL,
        check_out        DATE          NOT NULL,
        adults           INT           DEFAULT 1,
        children         INT           DEFAULT 0,
        room_quantity    INT           DEFAULT 1,
        customer_name    VARCHAR(255),
        customer_phone   VARCHAR(50),
        customer_email   VARCHAR(255),
        total_amount          DECIMAL(15,0) DEFAULT 0,
        paid_amount           DECIMAL(15,0) DEFAULT 0,
        remaining_amount      DECIMAL(15,0) DEFAULT 0,
        booking_status        VARCHAR(50)   DEFAULT 'PENDING_PAYMENT',
        payment_option        VARCHAR(50)   DEFAULT 'FULL_PAYMENT',
        payment_status        VARCHAR(50)   DEFAULT 'PENDING_PAYMENT',
        partner_status        VARCHAR(40)   DEFAULT NULL,
        note                  VARCHAR(500),
        -- === VOUCHER FIELDS (Hướng 2) ===
        voucher_code          VARCHAR(50)   DEFAULT NULL,
        discount_amount       DECIMAL(15,0) DEFAULT 0,
        voucher_cost_bearer   VARCHAR(20)   DEFAULT NULL,
        total_before_discount DECIMAL(15,0) DEFAULT 0,
        -- === FINANCIAL SNAPSHOT FIELDS ===
        commission_rate_snapshot       DECIMAL(5,4)  DEFAULT NULL,
        commission_source_snapshot     VARCHAR(30)   DEFAULT NULL,
        commission_base_amount         DECIMAL(15,0) DEFAULT 0,
        commission_amount_snapshot     DECIMAL(15,0) DEFAULT 0,
        partner_voucher_amount_snapshot DECIMAL(15,0) DEFAULT 0,
        admin_voucher_amount_snapshot  DECIMAL(15,0) DEFAULT 0,
        partner_payout_snapshot        DECIMAL(15,0) DEFAULT 0,
        online_paid_amount_snapshot    DECIMAL(15,0) DEFAULT 0,
        onsite_amount_snapshot         DECIMAL(15,0) DEFAULT 0,
        refund_amount                  DECIMAL(15,0) DEFAULT 0,
        cancellation_fee               DECIMAL(15,0) DEFAULT 0,
        -- === BOOKING SOURCE & DIRECT BOOKING FIELDS ===
        booking_source            VARCHAR(20)  DEFAULT 'ONLINE',
        block_reason              VARCHAR(300) DEFAULT NULL,
        remaining_payment_status  VARCHAR(30)  DEFAULT 'NOT_REQUIRED',
        remaining_paid_at         DATETIME     DEFAULT NULL,
        remaining_payment_note    VARCHAR(300) DEFAULT NULL,
        expire_at                 DATETIME(6)  DEFAULT NULL,
        created_at            DATETIME(6),
        updated_at            DATETIME(6),
        PRIMARY KEY (id),
        CONSTRAINT fk_bookings_user          FOREIGN KEY (user_id)          REFERENCES users (id),
        CONSTRAINT fk_bookings_accommodation FOREIGN KEY (accommodation_id) REFERENCES accommodations (id),
        CONSTRAINT fk_bookings_room          FOREIGN KEY (room_id)          REFERENCES rooms (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- =============================================
    -- 5. BẢNG PAYMENTS
    -- =============================================
    CREATE TABLE payments (
        id               BIGINT        NOT NULL AUTO_INCREMENT,
        booking_id       BIGINT        NOT NULL,
        payment_method   VARCHAR(50)   DEFAULT 'VNPAY',
        payment_option   VARCHAR(50)   NOT NULL,
        amount           DECIMAL(15,0) NOT NULL,
        transaction_code VARCHAR(50),
        payment_status   VARCHAR(50)   DEFAULT 'PENDING_PAYMENT',
        paid_at          DATETIME(6),
        approved_at      DATETIME(6),
        note             VARCHAR(500),
        gateway                   VARCHAR(20),
        vnp_txn_ref               VARCHAR(100) DEFAULT NULL UNIQUE,
        vnp_transaction_no        VARCHAR(50),
        vnp_bank_code             VARCHAR(20),
        vnp_bank_tran_no          VARCHAR(50),
        vnp_card_type             VARCHAR(20),
        vnp_response_code         VARCHAR(10),
        vnp_transaction_status    VARCHAR(10),
        vnp_pay_date              VARCHAR(20),
        raw_return_payload        TEXT,
        raw_ipn_payload           TEXT,
        expire_at                 DATETIME(6),
        confirmed_from_gateway_at DATETIME(6),
        PRIMARY KEY (id),
        CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- =============================================
    -- 6. BẢNG REVIEWS — Đánh giá sau booking COMPLETED
    -- =============================================
    CREATE TABLE reviews (
        id                 BIGINT        NOT NULL AUTO_INCREMENT,
        user_id            BIGINT        NOT NULL,
        accommodation_id   BIGINT        NOT NULL,
        booking_id         BIGINT        NOT NULL UNIQUE,
        rating             INT           NOT NULL,
        comment            TEXT,
        is_hidden          TINYINT(1)    NOT NULL DEFAULT 0,
        created_at         DATETIME(6),
        PRIMARY KEY (id),
        CONSTRAINT fk_reviews_user          FOREIGN KEY (user_id)          REFERENCES users (id),
        CONSTRAINT fk_reviews_accommodation FOREIGN KEY (accommodation_id) REFERENCES accommodations (id),
        CONSTRAINT fk_reviews_booking       FOREIGN KEY (booking_id)       REFERENCES bookings (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


    -- =============================================
    -- 7. BẢNG PARTNER_SETTLEMENTS — Quyết toán tháng cho Partner
    -- =============================================
    CREATE TABLE partner_settlements (
        id                       BIGINT        NOT NULL AUTO_INCREMENT,
        partner_id               BIGINT        NOT NULL,
        period_start             DATE          NOT NULL,
        period_end               DATE          NOT NULL,
        gross_amount             DECIMAL(15,0) DEFAULT 0,
        commission_amount        DECIMAL(15,0) DEFAULT 0,
        voucher_deduction_amount DECIMAL(15,0) DEFAULT 0,
        payout_amount            DECIMAL(15,0) DEFAULT 0,
        scheduled_payout_date     DATE,
        settlement_status        VARCHAR(20)   DEFAULT 'PENDING',
        settlement_date          DATETIME(6),
        note                     VARCHAR(500),
        created_at               DATETIME(6),
        PRIMARY KEY (id),
        -- UNIQUE: Mỗi partner chỉ có 1 settlement duy nhất cho 1 kỳ tháng
        UNIQUE KEY uk_settlement_partner_period (partner_id, period_start, period_end),
        CONSTRAINT fk_settlements_partner FOREIGN KEY (partner_id) REFERENCES users (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- =============================================
    -- 7a. BẢNG PARTNER_WALLETS — Ví quyết toán nội bộ của Partner
    -- =============================================
    CREATE TABLE partner_wallets (
        id                         BIGINT        NOT NULL AUTO_INCREMENT,
        partner_id                 BIGINT        NOT NULL UNIQUE,
        available_balance          DECIMAL(15,0) DEFAULT 0,
        pending_withdrawal_amount  DECIMAL(15,0) DEFAULT 0,
        total_earned_amount        DECIMAL(15,0) DEFAULT 0,
        total_withdrawn_amount     DECIMAL(15,0) DEFAULT 0,
        updated_at                 DATETIME(6),
        PRIMARY KEY (id),
        CONSTRAINT fk_wallet_partner FOREIGN KEY (partner_id) REFERENCES users (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- =============================================
    -- 7b. BẢNG PARTNER_WITHDRAWAL_REQUESTS — Partner yêu cầu rút tiền
    -- =============================================
    CREATE TABLE partner_withdrawal_requests (
        id                    BIGINT        NOT NULL AUTO_INCREMENT,
        partner_id            BIGINT        NOT NULL,
        request_code          VARCHAR(50)   NOT NULL UNIQUE,
        amount                DECIMAL(15,0) DEFAULT 0,
        bank_name             VARCHAR(100),
        bank_account_number   VARCHAR(30),
        bank_account_holder   VARCHAR(100),
        bank_branch           VARCHAR(100),
        withdrawal_status     VARCHAR(20)   DEFAULT 'PENDING',
        requested_at          DATETIME(6),
        processed_at          DATETIME(6),
        processed_by_admin_id BIGINT        NULL,
        admin_note            VARCHAR(500),
        PRIMARY KEY (id),
        CONSTRAINT fk_withdraw_partner FOREIGN KEY (partner_id) REFERENCES users (id),
        CONSTRAINT fk_withdraw_admin   FOREIGN KEY (processed_by_admin_id) REFERENCES users (id),
        INDEX idx_withdraw_partner_status (partner_id, withdrawal_status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- =============================================
    -- 7c. BẢNG PARTNER_WALLET_TRANSACTIONS — Lịch sử tiền vào/ra ví
    -- =============================================
    CREATE TABLE partner_wallet_transactions (
        id                    BIGINT        NOT NULL AUTO_INCREMENT,
        partner_id            BIGINT        NOT NULL,
        settlement_id         BIGINT        NULL,
        withdrawal_request_id BIGINT        NULL,
        transaction_code      VARCHAR(50)   NOT NULL UNIQUE,
        transaction_type      VARCHAR(40)   NOT NULL,
        direction             VARCHAR(20)   NOT NULL,
        amount                DECIMAL(15,0) DEFAULT 0,
        balance_before        DECIMAL(15,0) DEFAULT 0,
        balance_after         DECIMAL(15,0) DEFAULT 0,
        description           VARCHAR(500),
        created_at            DATETIME(6),
        created_by_admin_id   BIGINT        NULL,
        PRIMARY KEY (id),
        CONSTRAINT fk_wallet_tx_partner    FOREIGN KEY (partner_id) REFERENCES users (id),
        CONSTRAINT fk_wallet_tx_settlement FOREIGN KEY (settlement_id) REFERENCES partner_settlements (id),
        CONSTRAINT fk_wallet_tx_withdrawal FOREIGN KEY (withdrawal_request_id) REFERENCES partner_withdrawal_requests (id),
        CONSTRAINT fk_wallet_tx_admin      FOREIGN KEY (created_by_admin_id) REFERENCES users (id),
        UNIQUE KEY uk_wallet_tx_settlement_credit (settlement_id, transaction_type),
        UNIQUE KEY uk_wallet_tx_withdrawal_type (withdrawal_request_id, transaction_type),
        INDEX idx_wallet_tx_partner_created (partner_id, created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


    -- =============================================
    -- 8. BẢNG SUPPORT_TICKETS — Yêu cầu hỗ trợ từ Partner, User & Guest
    -- =============================================
    CREATE TABLE support_tickets (
        id              BIGINT        NOT NULL AUTO_INCREMENT,
        -- Người gửi: PARTNER | USER | GUEST
        requester_role  VARCHAR(20)   NOT NULL DEFAULT 'PARTNER',
        -- Partner gửi ticket (nullable nếu role != PARTNER)
        partner_id      BIGINT        NULL,
        -- User đăng nhập gửi contact (nullable nếu role != USER)
        user_id         BIGINT        NULL,
        -- Thông tin Guest hoặc fallback khi partner/user null
        requester_name  VARCHAR(255)  NULL,
        requester_email VARCHAR(255)  NULL,
        requester_phone VARCHAR(50)   NULL,
        -- Nội dung ticket
        category        VARCHAR(100)  NOT NULL,
        subject         VARCHAR(255)  NOT NULL,
        priority        VARCHAR(20)   NOT NULL DEFAULT 'Trung bình',
        description     TEXT,
        status          VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
        admin_response  TEXT,
        created_at      DATETIME(6),
        updated_at      DATETIME(6),
        PRIMARY KEY (id),
        CONSTRAINT fk_tickets_partner FOREIGN KEY (partner_id) REFERENCES users (id),
        CONSTRAINT fk_tickets_user    FOREIGN KEY (user_id)    REFERENCES users (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


    -- =============================================
    -- ========= DỮ LIỆU KHỞI TẠO NGHIỆP VỤ =========
    -- =============================================


    -- ─── USERS ────────────────────────────────────────────────
    -- admin123 → ADMIN | user123 → USER | partner123 → PARTNER1 (HOTEL) | partner123 → PARTNER2 (RESORT)
    INSERT INTO users (email, password, full_name, name, phone, role, status, partner_property_type,
                    bank_account_number, bank_name, bank_account_holder, bank_branch,
                    created_at, updated_at) VALUES
    ('admin@travelmate.vn',    '$2a$10$asNcVD6SMW64TtbgEncgKOu57UF2w.ZtFDDYup10xn74fA0ZJjh3K', 'Admin TravelMate',    'Admin TravelMate',    '0901 234 567', 'ADMIN',   'ACTIVE', NULL,
    NULL, NULL, NULL, NULL, NOW(), NOW()),
    ('user@travelmate.vn',     '$2a$10$FsFOdcKQPqCnlIPA3j82ZeY7R6tMpdhavFO2bfqWUfJqF1Z4nloO2', 'Nguyễn Văn An',       'Nguyễn Văn An',       '0912 345 678', 'USER',    'ACTIVE', NULL,
    NULL, NULL, NULL, NULL, NOW(), NOW()),
    ('partner@travelmate.vn',  '$2a$10$dCikCIiksr/Ne1Xpv40vKOMFwpiY751Cb1kdIVDwobiMzImbLC3oq', 'Sunrise Sapa Lodge',  'Sunrise Sapa Lodge',  '0933 456 789', 'PARTNER', 'ACTIVE', 'HOTEL',
    '0123456789', 'MB Bank', 'NGUYEN VAN A', 'TP.HCM', NOW(), NOW()),
    ('partner2@travelmate.vn', '$2a$10$dCikCIiksr/Ne1Xpv40vKOMFwpiY751Cb1kdIVDwobiMzImbLC3oq', 'Blue Ocean Resort',   'Blue Ocean Resort',   '0944 567 890', 'PARTNER', 'ACTIVE', 'RESORT',
    '9876543210', 'Vietcombank', 'TRAN THI B', 'Đà Nẵng', NOW(), NOW());

    -- ─── USERS BỔ SUNG (user2, user3 — cùng password user123 như user@travelmate.vn) ───
    INSERT INTO users (email, password, full_name, name, phone, role, status, partner_property_type,
                    bank_account_number, bank_name, bank_account_holder, bank_branch,
                    created_at, updated_at) VALUES
    ('user2@travelmate.vn', '$2a$10$FsFOdcKQPqCnlIPA3j82ZeY7R6tMpdhavFO2bfqWUfJqF1Z4nloO2', 'Trần Thị Mai', 'Trần Thị Mai', '0923 456 789', 'USER', 'ACTIVE', NULL,
    NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 90 DAY), DATE_SUB(NOW(), INTERVAL 90 DAY)),
    ('user3@travelmate.vn', '$2a$10$FsFOdcKQPqCnlIPA3j82ZeY7R6tMpdhavFO2bfqWUfJqF1Z4nloO2', 'Lê Văn Đức',   'Lê Văn Đức',   '0934 567 890', 'USER', 'ACTIVE', NULL,
    NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 75 DAY), DATE_SUB(NOW(), INTERVAL 75 DAY));

    -- ─── PARTNERS BỔ SUNG (partner3 VILLA, partner4 HOMESTAY — cùng password partner123) ───
    -- DataInitializer sẽ tạo lại nếu chưa có, nhưng thêm vào SQL để đảm bảo đầy đủ khi import thủ công
    INSERT INTO users (email, password, full_name, name, phone, role, status, partner_property_type,
                    bank_account_number, bank_name, bank_account_holder, bank_branch,
                    created_at, updated_at) VALUES
    ('partner3@travelmate.vn', '$2a$10$dCikCIiksr/Ne1Xpv40vKOMFwpiY751Cb1kdIVDwobiMzImbLC3oq', 'Green Hills Villa',  'Green Hills Villa',  '0955 678 901', 'PARTNER', 'ACTIVE', 'VILLA',
    '1122334455', 'Techcombank', 'LE VAN C', 'Hà Nội', NOW(), NOW()),
    ('partner4@travelmate.vn', '$2a$10$dCikCIiksr/Ne1Xpv40vKOMFwpiY751Cb1kdIVDwobiMzImbLC3oq', 'Homestay Rừng Thông Đà Lạt', 'Homestay Rừng Thông Đà Lạt', '0908 555 666', 'PARTNER', 'ACTIVE', 'HOMESTAY',
    '5544332211', 'Agribank', 'PHAM THI D', 'Cần Thơ', NOW(), NOW());

    -- ─── TÀI KHOẢN BỔ SUNG CHO KỊCH BẢN TRÌNH BÀY ĐỦ VAI TRÒ ───────────────
    -- family/couple dùng password user123; các partner dùng password partner123.
    INSERT INTO users (email, password, full_name, name, phone, role, status, partner_property_type,
                    bank_account_number, bank_name, bank_account_holder, bank_branch,
                    created_at, updated_at) VALUES
    ('family@travelmate.vn', '$2a$10$FsFOdcKQPqCnlIPA3j82ZeY7R6tMpdhavFO2bfqWUfJqF1Z4nloO2', 'Gia đình Minh Anh', 'Gia đình Minh Anh', '0968 111 222', 'USER', 'ACTIVE', NULL,
    NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 45 DAY), DATE_SUB(NOW(), INTERVAL 45 DAY)),
    ('couple@travelmate.vn', '$2a$10$FsFOdcKQPqCnlIPA3j82ZeY7R6tMpdhavFO2bfqWUfJqF1Z4nloO2', 'Linh & Khánh', 'Linh & Khánh', '0979 333 444', 'USER', 'ACTIVE', NULL,
    NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 38 DAY), DATE_SUB(NOW(), INTERVAL 38 DAY)),
    ('resort@travelmate.vn', '$2a$10$dCikCIiksr/Ne1Xpv40vKOMFwpiY751Cb1kdIVDwobiMzImbLC3oq', 'Heritage Resort Group', 'Heritage Resort Group', '0981 555 777', 'PARTNER', 'ACTIVE', 'RESORT',
    '6688990011', 'ACB', 'HERITAGE RESORT GROUP', 'Hà Nội', DATE_SUB(NOW(), INTERVAL 70 DAY), DATE_SUB(NOW(), INTERVAL 70 DAY)),
    ('villa@travelmate.vn', '$2a$10$dCikCIiksr/Ne1Xpv40vKOMFwpiY751Cb1kdIVDwobiMzImbLC3oq', 'Ocean & Hill Villa', 'Ocean & Hill Villa', '0982 666 888', 'PARTNER', 'ACTIVE', 'VILLA',
    '7788990011', 'VPBank', 'OCEAN HILL VILLA', 'Đà Nẵng', DATE_SUB(NOW(), INTERVAL 68 DAY), DATE_SUB(NOW(), INTERVAL 68 DAY)),
    ('homestay@travelmate.vn', '$2a$10$dCikCIiksr/Ne1Xpv40vKOMFwpiY751Cb1kdIVDwobiMzImbLC3oq', 'Bản Mây Homestay', 'Bản Mây Homestay', '0983 777 999', 'PARTNER', 'ACTIVE', 'HOMESTAY',
    '8899001122', 'VietinBank', 'BAN MAY HOMESTAY', 'Lào Cai', DATE_SUB(NOW(), INTERVAL 66 DAY), DATE_SUB(NOW(), INTERVAL 66 DAY)),
    ('no-bank@travelmate.vn', '$2a$10$dCikCIiksr/Ne1Xpv40vKOMFwpiY751Cb1kdIVDwobiMzImbLC3oq', 'North Star Hotel Group', 'North Star Hotel Group', '0984 888 000', 'PARTNER', 'ACTIVE', 'HOTEL',
    NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY));

    -- ─── SUPPORT TICKETS — Yêu cầu hỗ trợ từ Partner, User & Guest ────────
    -- PARTNER tickets: partner1 (id=3): 3 tickets, partner2 (id=4): 2 tickets
    --                  partner3 (id=7): 2 tickets, partner4 (id=8): 2 tickets
    -- USER/GUEST tickets: 2 yêu cầu mẫu
    INSERT INTO support_tickets (requester_role, partner_id, user_id, requester_name, requester_email, requester_phone, category, subject, priority, description, status, admin_response, created_at, updated_at) VALUES

    -- ── PARTNER tickets ──────────────────────────────────────────────────────
    -- partner1 — HOTEL (Sunrise Sapa Lodge, id=3)
    ('PARTNER', 3, NULL, NULL, NULL, NULL, 'Thanh toán & Quyết toán', 'Quyết toán tháng 03/2026 bị sai số tiền', 'Cao',
    'Chào Admin, tôi kiểm tra lại quyết toán tháng 03/2026 của booking BK-LATA-DLX-0002. TravelMate thu online 2.040.000đ, hoa hồng 15% tính trên tổng đơn gốc 2.550.000đ là 382.500đ, voucher LATA20 do partner chịu 510.000đ nên khoản nhận về là 1.147.500đ. Nhờ Admin đối soát giúp tôi.',
    'RESPONDED',
    'Chào bạn, tôi đã kiểm tra lại và xác nhận số liệu đúng theo quy tắc hiện hành: TravelMate thu online 2.040.000đ; hoa hồng 15% tính trên tổng đơn gốc trước voucher là 382.500đ; voucher LATA20 do partner chịu 510.000đ; khoản nhận về là 1.147.500đ. Nếu cần đối chiếu thêm, bạn có thể xem lại trang Chi tiết quyết toán.',
    DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),

    ('PARTNER', 3, NULL, NULL, NULL, NULL, 'Đơn đặt phòng', 'Khách không đến nhưng không thể đánh dấu No-Show', 'Trung bình',
    'Booking BK-TLP-STD-0001, khách Nguyễn Văn An đã không đến check-in ngày hôm qua. Tôi muốn đánh dấu No-Show để hệ thống tự động giữ cọc 30% nhưng không thấy nút này ở giao diện. Mong Admin xử lý giúp.',
    'RESPONDED',
    'Chào bạn, chức năng đánh dấu No-Show hiện chỉ Admin mới thực hiện được để đảm bảo kiểm soát chặt chẽ. Tôi đã cập nhật booking BK-TLP-STD-0001 thành NO_SHOW và cọc 30% (288.000đ) đã được giữ lại. Trong phiên bản tới chúng tôi sẽ cho phép Partner tự đánh dấu sau 4h kể từ giờ check-in.',
    DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),

    ('PARTNER', 3, NULL, NULL, NULL, NULL, 'Kỹ thuật', 'Trang doanh thu không hiển thị biểu đồ', 'Thấp',
    'Khi tôi vào /partner/revenue, trang tải bình thường nhưng phần biểu đồ doanh thu bị trống. Trình duyệt Chrome v124. Xin hỗ trợ.',
    'OPEN', NULL,
    DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

    -- partner2 — RESORT (Blue Ocean Resort, id=4)
    ('PARTNER', 4, NULL, NULL, NULL, NULL, 'Cơ sở lưu trú', 'Muốn thêm ảnh thumbnail cho Vinpearl Resort', 'Thấp',
    'Ảnh thumbnail hiện tại của Vinpearl Resort & Spa Nha Trang (acc id=8) trông hơi tối. Tôi muốn cập nhật ảnh mới đẹp hơn nhưng không thấy chỗ chỉnh sửa trong giao diện Partner. Xin hướng dẫn.',
    'CLOSED',
    'Chào bạn, hiện tại chức năng thay đổi thumbnail cần Admin hỗ trợ. Tôi đã cập nhật ảnh thumbnail mới cho Vinpearl Resort của bạn. Trong phiên bản tới, Partner sẽ tự chỉnh sửa được trực tiếp từ trang Nơi lưu trú của tôi.',
    DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)),

    ('PARTNER', 4, NULL, NULL, NULL, NULL, 'Thanh toán & Quyết toán', 'Chưa nhận được thanh toán tháng 04/2026', 'Cao',
    'Quyết toán tháng 04/2026 có payout 4.592.000đ, trạng thái đã PAID nhưng tài khoản Vietcombank của tôi chưa nhận được tiền. Số TK: 9876543210, chủ TK: TRAN THI B. Đã chờ 3 ngày rồi.',
    'RESPONDED',
    'Chào bạn, tôi đã kiểm tra lại. Giao dịch chuyển khoản 4.592.000đ đã được xử lý ngày hôm qua, thường mất 1-2 ngày làm việc để tiền về tài khoản. Nếu sau 48h nữa vẫn chưa nhận được, vui lòng liên hệ lại với mã giao dịch để tôi xác nhận với bộ phận tài chính.',
    DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),

    -- partner3 — VILLA (Green Hills Villa, id=7)
    ('PARTNER', 7, NULL, NULL, NULL, NULL, 'Đơn đặt phòng', 'Cần xác nhận lịch giữ villa cho BK-ANM-GDN-0001', 'Cao',
    'Booking BK-ANM-GDN-0001 (Anam Villa, khách Hoàng Văn Hùng, check-in +10 ngày) đã được TravelMate ghi nhận cọc 30% và tự động giữ villa. Tôi muốn xác nhận thêm yêu cầu chuẩn bị hồ bơi riêng trước ngày khách đến.',
    'OPEN', NULL,
    DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

    ('PARTNER', 7, NULL, NULL, NULL, NULL, 'Kỹ thuật', 'Không thêm được phòng mới cho Ba Na Hills Villa', 'Trung bình',
    'Tôi vào acc5 Ba Na Hills Forest Villa, bấm nút Thêm phòng nhưng trang báo lỗi "Chỉ có thể thêm phòng cho cơ sở đã được Admin duyệt". Trong khi acc5 đang có trạng thái APPROVED. Xin kiểm tra giúp.',
    'RESPONDED',
    'Chào bạn, tôi đã kiểm tra và xác nhận acc5 Ba Na Hills Forest Villa đang ở trạng thái APPROVED. Lỗi có thể do cache trình duyệt. Hãy thử Ctrl+Shift+R để hard refresh. Nếu vẫn lỗi, hãy chụp màn hình và gửi lại cho tôi.',
    DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),

    -- partner4 — HOMESTAY (Mekong Homestay, id=8)
    ('PARTNER', 8, NULL, NULL, NULL, NULL, 'Voucher', 'Muốn gắn voucher nhưng không thấy phòng/căn của tôi', 'Trung bình',
    'Tôi vào /partner/vouchers để gắn voucher do Admin phát hành, nhưng không thấy phòng/căn phù hợp. Tôi có 2 homestay là Hoa Lư (id=6) và Mộc Nhiên (id=7), cả hai đều đang APPROVED nhưng không hiện trong danh sách.',
    'OPEN', NULL,
    DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),

    ('PARTNER', 8, NULL, NULL, NULL, NULL, 'Thanh toán & Quyết toán', 'Hỏi về lịch quyết toán hàng tháng', 'Thấp',
    'Tôi muốn hỏi TravelMate thanh toán quyết toán cho partner vào ngày nào trong tháng? Và số tiền tối thiểu để được thanh toán là bao nhiêu?',
    'CLOSED',
    'Chào bạn! TravelMate thực hiện quyết toán vào đầu mỗi tháng cho doanh thu tháng trước. Không có số tiền tối thiểu — dù chỉ 1 booking đã hoàn tất cũng sẽ được thanh toán. Tiền chuyển về tài khoản đăng ký trong mục Hồ sơ trong vòng 1-2 ngày làm việc.',
    DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY)),

    -- ── USER/GUEST tickets khởi tạo ───────────────────────────────────────────
    -- user@travelmate.vn (id=2) gửi liên hệ với tư cách USER đã đăng nhập
    ('USER', NULL, 2, 'Nguyễn Văn An', 'user@travelmate.vn', '0912 345 678',
    'Đặt phòng', 'Tôi muốn hỏi về chính sách hủy phòng', 'Thấp',
    'Cho tôi hỏi nếu tôi hủy booking trước 48h thì có hoàn tiền không? Đặc biệt với loại cọc 30%.',
    'RESPONDED',
    'Chào bạn Nguyễn Văn An! Theo chính sách TravelMate: đơn đã thanh toán cọc 30% khi khách tự hủy hoặc không đến sẽ giữ toàn bộ khoản cọc. Với đơn thanh toán đủ, yêu cầu hủy được chuyển cho Admin xử lý hoàn tiền theo chính sách. Bạn có thể thao tác trong mục Lịch sử đặt phòng.',
    DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),

    -- Guest không đăng nhập gửi liên hệ
    ('GUEST', NULL, NULL, 'Khách Vãng Lai', 'guest.visitor@email.com', '0900 000 001',
    'Khác', 'Hỏi về hợp tác đưa cơ sở lên TravelMate', 'Thấp',
    'Xin chào, tôi có một villa nhỏ ở Đà Lạt, muốn hỏi thủ tục để đăng ký làm đối tác trên TravelMate là như thế nào? Chi phí, hoa hồng, điều kiện ra sao?',
    'OPEN', NULL,
    DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));

    -- ─── SUPPORT TICKETS BỔ SUNG — đủ dữ liệu cho màn hình Admin/Partner/User ───
    INSERT INTO support_tickets (requester_role, partner_id, user_id, requester_name, requester_email, requester_phone, category, subject, priority, description, status, admin_response, created_at, updated_at) VALUES
    ('USER', NULL, (SELECT id FROM users WHERE email = 'family@travelmate.vn' LIMIT 1),
    'Gia đình Minh Anh', 'family@travelmate.vn', '0968 111 222',
    'Đặt phòng', 'Cần đổi ngày nhận phòng Phú Quốc cho gia đình 4 người', 'Cao',
    'Gia đình tôi đã đặt phòng cho kỳ nghỉ Phú Quốc nhưng lịch bay bị đổi sang hôm sau. Nhờ TravelMate kiểm tra giúp còn phòng gia đình cùng hạng để đổi ngày không, nếu phát sinh chênh lệch tôi sẽ thanh toán thêm.',
    'OPEN', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

    ('USER', NULL, (SELECT id FROM users WHERE email = 'couple@travelmate.vn' LIMIT 1),
    'Linh & Khánh', 'couple@travelmate.vn', '0979 333 444',
    'Voucher', 'Hỏi mã ưu đãi cho chuyến Đà Lạt 2 đêm', 'Trung bình',
    'Tôi muốn đặt phòng ở Đà Lạt cuối tuần này cho 2 người. Hiện có mã nào dùng được cho khách sạn hoặc homestay không? Nếu có điều kiện tối thiểu đơn hàng, vui lòng hướng dẫn giúp tôi.',
    'RESPONDED',
    'Chào bạn, bạn có thể thử các mã toàn hệ thống còn hiệu lực trong trang Voucher. Khi chọn phòng, hệ thống sẽ tự kiểm tra điều kiện đơn tối thiểu và hiển thị số tiền giảm ở bước xác nhận.',
    DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),

    ('USER', NULL, (SELECT id FROM users WHERE email = 'user2@travelmate.vn' LIMIT 1),
    'Trần Thị Mai', 'user2@travelmate.vn', '0923 456 789',
    'Đánh giá', 'Không thấy nút viết đánh giá sau khi checkout', 'Trung bình',
    'Tôi đã hoàn tất chuyến đi ở villa Nha Trang nhưng vào lịch sử đặt phòng chưa thấy nút đánh giá. Nhờ Admin kiểm tra giúp booking đã chuyển sang trạng thái hoàn tất chưa.',
    'OPEN', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

    ('PARTNER', (SELECT id FROM users WHERE email = 'resort@travelmate.vn' LIMIT 1), NULL,
    NULL, NULL, NULL, 'Thanh toán & Quyết toán', 'Đối soát doanh thu resort trong tháng gần nhất', 'Cao',
    'Tôi cần đối chiếu doanh thu của các phòng resort đã checkout tháng này, đặc biệt các đơn có voucher do sàn chịu và đơn thanh toán cọc 30%. Mong Admin xác nhận công thức payout trên trang quyết toán.',
    'RESPONDED',
    'Chào bạn, payout đang được tính theo tổng tiền phòng trước voucher trừ hoa hồng, sau đó trừ phần voucher do Partner chịu nếu có. Với voucher do TravelMate chịu, khoản giảm không trừ vào payout của Partner.',
    DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),

    ('PARTNER', (SELECT id FROM users WHERE email = 'villa@travelmate.vn' LIMIT 1), NULL,
    NULL, NULL, NULL, 'Phòng & Tồn kho', 'Cập nhật lịch bảo trì villa Mũi Né', 'Trung bình',
    'Villa Mũi Né của tôi cần bảo trì hồ bơi riêng trong 3 ngày. Tôi muốn khóa phòng trên hệ thống để tránh khách đặt nhầm, đồng thời vẫn giữ các ngày khác mở bán bình thường.',
    'OPEN', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

    ('PARTNER', (SELECT id FROM users WHERE email = 'homestay@travelmate.vn' LIMIT 1), NULL,
    NULL, NULL, NULL, 'Tiện nghi', 'Bổ sung tiện nghi bếp chung cho homestay Bắc Hà', 'Thấp',
    'Homestay Bắc Hà có bếp chung, sân vườn và dịch vụ thuê xe máy. Tôi muốn các tiện nghi này hiển thị rõ ở chi tiết phòng để khách dễ lựa chọn.',
    'RESPONDED',
    'Chào bạn, Admin đã ghi nhận. Bạn có thể cập nhật mô tả phòng và ảnh tiện nghi tại trang quản lý cơ sở; phần tiện nghi sẽ được kiểm tra trước khi hiển thị cho khách.',
    DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),

    ('PARTNER', (SELECT id FROM users WHERE email = 'no-bank@travelmate.vn' LIMIT 1), NULL,
    NULL, NULL, NULL, 'Rút tiền', 'Không gửi được yêu cầu rút tiền vì thiếu tài khoản ngân hàng', 'Cao',
    'Tôi thấy ví có số dư nhưng chưa gửi được yêu cầu rút tiền. Tài khoản đối tác của tôi chưa cấu hình ngân hàng, nhờ Admin hướng dẫn thông tin cần bổ sung.',
    'RESPONDED',
    'Chào bạn, để rút tiền cần cập nhật tên ngân hàng, số tài khoản, chủ tài khoản và chi nhánh trong hồ sơ Partner. Sau khi cập nhật, bạn có thể gửi lại yêu cầu rút tiền từ trang Ví.',
    DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

    ('GUEST', NULL, NULL, 'Công ty Ánh Dương', 'booking@anhduongcorp.vn', '0902 222 333',
    'Đặt đoàn', 'Hỏi đặt villa cho đoàn công ty 12 khách', 'Trung bình',
    'Công ty chúng tôi cần đặt villa hoặc resort cho 12 khách trong 2 đêm, ưu tiên khu vực Đà Lạt hoặc Vũng Tàu, có phòng sinh hoạt chung và xuất hóa đơn.',
    'OPEN', NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),

    ('GUEST', NULL, NULL, 'Nguyễn Hà Vy', 'vy.nguyen@email.com', '0905 555 777',
    'Hóa đơn', 'Cần xác nhận TravelMate có hỗ trợ hóa đơn điện tử không', 'Thấp',
    'Tôi muốn đặt khách sạn cho chuyến công tác tại TP.HCM và cần hóa đơn điện tử theo thông tin công ty. Nhờ TravelMate xác nhận cách gửi thông tin hóa đơn.',
    'CLOSED',
    'Chào bạn, sau khi đặt phòng thành công, bạn có thể gửi thông tin xuất hóa đơn qua trang Liên hệ. Bộ phận hỗ trợ sẽ chuyển thông tin cho cơ sở lưu trú để xử lý theo quy định.',
    DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY));

    -- ─── ACCOMMODATIONS (3 Khách sạn — owner_id=3 = partner@travelmate.vn) ───
    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at) VALUES
    (
        'LATA Hotel & Apartments',
        'Khách sạn hiện đại nằm ngay trung tâm thành phố Đà Lạt, cách chợ đêm Đà Lạt 500m. Phòng rộng rãi, view đẹp, tiện nghi đầy đủ.',
        '15 Phan Bội Châu, Phường 1',
        'Đà Lạt',
        '/assets/images/accommodations/lata/cover/room-hero.jpg',
        4, 8.6, 771, 'HOTEL', 'APPROVED', 3, NOW(), NOW()
    ),
    (
        'Tulip Hotel 2 Dalat',
        'Khách sạn 3 sao thiết kế phong cách Châu Âu, gần hồ Xuân Hương và Vườn hoa thành phố. Dịch vụ tận tình, giá cả phải chăng.',
        '56 Bùi Thị Xuân, Phường 2',
        'Đà Lạt',
        '/assets/images/accommodations/hotel/tulip-hotel-2-dalat/cover.jpg',
        3, 8.2, 456, 'HOTEL', 'APPROVED', 3, NOW(), NOW()
    ),
    (
        'TravelMate Grand Hotel',
        'Khách sạn 5 sao sang trọng bậc nhất Đà Lạt, tọa lạc trên đồi thông với tầm nhìn toàn cảnh thung lũng. Spa cao cấp, nhà hàng fine dining, hồ bơi vô cực.',
        '88 Nguyễn Chí Thanh, Phường 6',
        'Đà Lạt',
        '/assets/images/accommodations/hotel/travelmate-grand-hotel/cover.jpg',
        5, 9.2, 1205, 'HOTEL', 'APPROVED', 3, NOW(), NOW()
    );

    -- ─── ROOMS (v2: thêm room_category + commission_rate_override) ────────────
    --
    -- KỊCH BẢN KIỂM TRA COMMISSION:
    --   1. STANDARD → null override → dùng default theo PropertyType (HOTEL=15%)
    --   2. DELUXE   → null override → dùng default theo PropertyType (HOTEL=15%)
    --   3. FAMILY   → override 12% → THẤP HƠN default (ưu đãi gia đình)
    --   4. VIP      → override 20% → CAO HƠN default (phòng premium)
    --   5. SUITE    → override 20% → CAO NHẤT (suite cao cấp)
    --
    -- Admin/Partner thấy rõ sự khác nhau trong bảng Doanh Thu.

    -- Hotel 1: LATA Hotel & Apartments (id=1) — HOTEL (default 15%)
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category, commission_rate_override) VALUES
    (1, 'LATA-STD', 'Phòng Tiêu Chuẩn Giường King',  '1 giường cỡ King',           2, 650000, 5,
    'Phòng tiêu chuẩn 25m², tầm nhìn thành phố, WiFi miễn phí, điều hoà, minibar.',
    '/assets/images/accommodations/lata/rooms/standard-king/interior.jpg',
    'STANDARD', NULL),        -- ← null = dùng HOTEL default 15%
    (1, 'LATA-DLX', 'Phòng Deluxe Giường Đôi',       '2 giường đơn',               3, 850000, 4,
    'Phòng Deluxe 30m² với ban công riêng, view vườn hoa. Bao gồm bữa sáng.',
    '/assets/images/accommodations/lata/rooms/deluxe-double/bedroom.jpg',
    'DELUXE', NULL),           -- ← null = dùng HOTEL default 15%
    (1, 'LATA-FAM', 'Phòng Gia Đình',                 '1 giường King + 1 giường đơn', 4, 1200000, 3,
    'Phòng gia đình 40m², phù hợp gia đình có trẻ nhỏ. Có bồn tắm lớn.',
    '/assets/images/accommodations/lata/rooms/family/main.jpg',
    'FAMILY', 12.00),          -- ← 12% override (ưu đãi gia đình, thấp hơn default 15%)
    (1, 'LATA-SUI', 'Phòng Suite Cao Cấp',            '1 giường King size',         2, 1800000, 1,
    'Suite 55m² sang trọng với phòng khách riêng, view hồ Xuân Hương, bồn tắm jacuzzi.',
    '/assets/images/accommodations/lata/rooms/loft-suite/view-lounge.jpg',
    'SUITE', 20.00);           -- ← 20% override (suite premium, cao hơn default 15%)

    -- Hotel 2: Tulip Hotel 2 Dalat (id=2) — HOTEL (default 15%)
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category, commission_rate_override) VALUES
    (2, 'TLP-STD', 'Phòng Standard Twin',   '2 giường đơn',                  2, 480000, 6,
    'Phòng standard 22m², nội thất đơn giản tiện nghi, WiFi miễn phí.',
    '/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/tlp-std.jpg',
    'STANDARD', NULL),         -- ← null = HOTEL default 15%
    (2, 'TLP-SUP', 'Phòng Superior Double', '1 giường đôi',                  2, 620000, 5,
    'Phòng Superior 28m² với view đồi thông, bao gồm bữa sáng buffet.',
    '/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/tlp-sup.jpg',
    'DELUXE', NULL),            -- ← null = HOTEL default 15%
    (2, 'TLP-FAM', 'Phòng Gia Đình Rộng',  '1 giường đôi + 2 giường đơn',  5, 1050000, 2,
    'Phòng gia đình rộng 45m², lý tưởng cho nhóm bạn hoặc gia đình lớn.',
    '/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/tlp-fam.jpg',
    'FAMILY', NULL),            -- ← null = HOTEL default 15%
    (2, 'TLP-VIP', 'Phòng VIP Panorama',   '1 giường King size',            2, 1500000, 2,
    'Phòng VIP 50m² với ban công rộng, view 360° toàn cảnh Đà Lạt.',
    '/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/tlp-vip.jpg',
    'VIP', 20.00);              -- ← 20% override (VIP Hotel, đúng kịch bản test commission theo phòng)

    -- Hotel 3: TravelMate Grand Hotel (id=3) — HOTEL (default 15%)
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category, commission_rate_override) VALUES
    (3, 'TMG-DLX',  'Deluxe Garden View',    '1 giường King size',                   2, 1200000, 8,
    'Phòng Deluxe 35m², view vườn thông tĩnh lặng, bồn tắm đứng + bồn ngâm riêng.',
    '/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/tmg-dlx.jpg',
    'DELUXE', NULL),            -- ← null = HOTEL default 15%
    (3, 'TMG-PRE',  'Premium Valley View',   '1 giường King hoặc 2 giường đơn',     3, 1650000, 5,
    'Phòng Premium 42m², tầm nhìn thung lũng ngoạn mục, minibar complimentary.',
    '/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/tmg-pre.jpg',
    'VIP', 20.00),              -- ← 20% override (VIP Hotel, đúng kịch bản test commission theo phòng)
    (3, 'TMG-FAM',  'Family Grand Suite',    '2 giường King size',                   5, 2800000, 3,
    'Suite gia đình 65m², 2 phòng ngủ, phòng khách, bếp nhỏ, phù hợp nghỉ dưỡng dài ngày.',
    '/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/tmg-fam.jpg',
    'FAMILY', 12.00),           -- ← 12% override (ưu đãi gia đình)
    (3, 'TMG-PRE2', 'Presidential Suite',   '1 giường King cỡ lớn',                 2, 5500000, 1,
    'Suite Tổng Thống 100m², sang trọng nhất khách sạn. Phòng khách riêng, bàn làm việc, spa tại phòng, butler riêng.',
    '/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/tmg-pre2.jpg',
    'SUITE', 20.00);            -- ← 20% override (Presidential Suite premium)


    -- ─── ACCOMMODATIONS — VILLA (owner_id=7 = partner3@travelmate.vn VILLA) ───────────────
    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at) VALUES
    (
        'The Anam Villa Nha Trang',
        'Biệt thự nghỉ dưỡng phong cách Đông Dương sang trọng tọa lạc ngay trên bãi biển riêng Cam Ranh. Hồ bơi private, butler riêng, view biển vô cực.',
        'Nguyễn Tất Thành, Cam Lâm',
        'Nha Trang',
        '/assets/images/accommodations/villa/the-anam-villa-nha-trang/cover.jpg',
        5, 9.4, 632, 'VILLA', 'APPROVED', 7, NOW(), NOW()
    ),
    (
        'Ba Na Hills Forest Villa',
        'Villa bungalow giữa rừng nguyên sinh núi Bà Nà, thiết kế gỗ tự nhiên ấm áp. Gần cáp treo dài nhất thế giới, khí hậu mát mẻ quanh năm.',
        'Km 20 Huyện Hòa Vang',
        'Đà Nẵng',
        '/assets/images/accommodations/villa/ba-na-hills-forest-villa/cover.jpg',
        4, 8.8, 415, 'VILLA', 'APPROVED', 7, NOW(), NOW()
    );

    -- Rooms – Villa 1: The Anam Villa Nha Trang (id=4) — VILLA (default 12%)
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category, commission_rate_override) VALUES
    (4, 'ANM-GDN', 'Garden Pool Villa',       '1 giường King size',            2, 3500000, 4,
    'Biệt thự 80m² có hồ bơi riêng, vườn nhiệt đới, view núi. Bao gồm bữa sáng đặt tại phòng.',
    '/assets/images/accommodations/villa/the-anam-villa-nha-trang/rooms/anm-gdn.jpg',
    'STANDARD', NULL),    -- ← null = VILLA default 12%
    (4, 'ANM-BCH', 'Beachfront Pool Villa',   '1 giường King cỡ lớn',          2, 5800000, 2,
    'Biệt thự 120m² sát biển, hồ bơi private infinity tràn ra biển. Butler phục vụ 24/7.',
    '/assets/images/accommodations/villa/the-anam-villa-nha-trang/rooms/anm-bch.jpg',
    'VIP', 15.00),        -- ← 15% override (Beachfront VIP)
    (4, 'ANM-FAM', 'Family Grand Villa',      '3 giường King',                  6, 8500000, 1,
    'Biệt thự 200m² hai tầng, 3 phòng ngủ, phòng khách rộng, bếp ăn full, hồ bơi private.',
    '/assets/images/accommodations/villa/the-anam-villa-nha-trang/rooms/anm-fam.jpg',
    'FAMILY', 10.00);     -- ← 10% override (ưu đãi gia đình villa)

    -- Rooms – Villa 2: Ba Na Hills Forest Villa (id=5) — VILLA (default 12%)
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category, commission_rate_override) VALUES
    (5, 'BNH-BNG', 'Forest Bungalow',         '1 giường King size',            2, 2200000, 5,
    'Bungalow 60m² gỗ tự nhiên, sàn kính ngắm rừng, bồn tắm thảo mộc, hơi sương sáng sớm.',
    '/assets/images/accommodations/villa/ba-na-hills-forest-villa/rooms/bnh-bng.jpg',
    'STANDARD', NULL),    -- ← null = VILLA default 12%
    (5, 'BNH-TWN', 'Twin Cabin',              '2 giường đơn',                  3, 1600000, 4,
    'Cabin 45m² dành cho nhóm bạn, view đồi thông, sân hiên ngoài trời có bếp nướng BBQ.',
    '/assets/images/accommodations/villa/ba-na-hills-forest-villa/rooms/bnh-twn.jpg',
    'FAMILY', NULL),      -- ← null = VILLA default 12%
    (5, 'BNH-SUI', 'Treetop Suite',           '1 giường King size',            2, 3800000, 2,
    'Suite trên cây 70m², ban công 360° nhìn toàn rừng, bồn tắm jacuzzi ngoài trời.',
    '/assets/images/accommodations/villa/ba-na-hills-forest-villa/rooms/bnh-sui.jpg',
    'SUITE', 16.00);      -- ← 16% override (Treetop Suite)

    -- ─── ACCOMMODATIONS — HOMESTAY (owner_id=8 = partner4@travelmate.vn HOMESTAY) ─────────────
    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at) VALUES
    (
        'Hoa Lư Riverside Homestay',
        'Nhà dân truyền thống 3 gian mái ngói ven sông Thu Bồn, phố cổ Hội An chỉ 5 phút đi bộ. Bữa sáng bánh mì Hội An tự làm, xe đạp miễn phí.',
        '42 Nguyễn Trung Trực, Cẩm Châu',
        'Hội An',
        '/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/cover.jpg',
        3, 8.9, 289, 'HOMESTAY', 'APPROVED', 8, NOW(), NOW()
    ),
    (
        'Mộc Nhiên Garden Homestay Đà Lạt',
        'Căn nhà gỗ thông Đà Lạt phong cách Pháp cổ giữa vườn hoa dã quỳ. Lò sưởi củi, bếp nấu chung, view đồi thông yên tĩnh tuyệt đối.',
        '18 Đường Vạn Kiếp, Phường 5',
        'Đà Lạt',
        '/assets/images/accommodations/homestay/moc-nhien-garden-homestay-da-lat/cover.jpg',
        3, 8.5, 178, 'HOMESTAY', 'APPROVED', 8, NOW(), NOW()
    );

    -- Rooms – Homestay 1: Hoa Lư Riverside (id=6) — HOMESTAY (default 10%)
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category, commission_rate_override) VALUES
    (6, 'HLR-STD', 'Phòng Truyền Thống',      '1 giường đôi',                  2, 320000,  4,
    'Phòng 20m² trang trí bằng đồ gốm Chu Đậu và tranh lụa Hội An. Nhà tắm chung sạch sẽ.',
    '/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/rooms/hlr-std.jpg',
    'STANDARD', NULL),    -- ← null = HOMESTAY default 10%
    (6, 'HLR-DLX', 'Phòng Deluxe Riêng',     '1 giường đôi',                  2, 480000,  3,
    'Phòng 25m² nhà tắm riêng, cửa sổ nhìn vườn, bao gồm bữa sáng phở và bánh mì.',
    '/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/rooms/hlr-dlx.jpg',
    'DELUXE', NULL),      -- ← null = HOMESTAY default 10%
    (6, 'HLR-FAM', 'Phòng Gia Đình Ven Sông', '2 giường đôi',                  4, 750000,  2,
    'Phòng 35m² ban công nhìn sông Thu Bồn, lý tưởng cho gia đình nhỏ có trẻ em.',
    '/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/rooms/hlr-fam.jpg',
    'FAMILY', 8.00);      -- ← 8% override (ưu đãi gia đình homestay)

    -- Rooms – Homestay 2: Mộc Nhiên Đà Lạt (id=7) — HOMESTAY (default 10%)
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category, commission_rate_override) VALUES
    (7, 'MND-STD', 'Phòng Nhà Gỗ Tiêu Chuẩn', '1 giường đôi',                 2, 390000,  5,
    'Phòng gỗ thông 22m², lò sưởi mini, nội thất vintage, bao gồm bữa sáng bánh mì thịt nướng.',
    '/assets/images/accommodations/homestay/moc-nhien-garden-homestay-da-lat/rooms/mnd-std.jpg',
    'STANDARD', NULL),    -- ← null = HOMESTAY default 10%
    (7, 'MND-ATT', 'Phòng Áp Mái View Đồi',   '1 giường King size',            2, 580000,  3,
    'Phòng áp mái 28m² cửa sổ mái kính, view đồi thông xanh, đặc biệt yên tĩnh.',
    '/assets/images/accommodations/homestay/moc-nhien-garden-homestay-da-lat/rooms/mnd-att.jpg',
    'DELUXE', 12.00),     -- ← 12% override (Áp Mái premium view)
    (7, 'MND-FAM', 'Phòng Gia Đình Vườn Hoa', '1 giường King + 1 đơn',         4, 880000,  2,
    'Phòng 38m² sân thượng riêng nhìn ra vườn hoa dã quỳ, thích hợp gia đình hoặc nhóm nhỏ.',
    '/assets/images/accommodations/homestay/moc-nhien-garden-homestay-da-lat/rooms/mnd-fam.jpg',
    'FAMILY', NULL);      -- ← null = HOMESTAY default 10%

    -- ─── ACCOMMODATIONS — RESORT (owner_id=4 = partner2@travelmate.vn RESORT) ───────────────
    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at) VALUES
    (
        'Vinpearl Resort & Spa Nha Trang',
        'Khu nghỉ dưỡng 5 sao trên đảo Hòn Tre huyền thoại, kết nối bằng cáp treo vượt biển dài nhất thế giới. Công viên nước, sân golf, casino, spa đẳng cấp quốc tế.',
        'Đảo Hòn Tre, Vĩnh Nguyên',
        'Nha Trang',
        '/assets/images/accommodations/resort/vinpearl-resort-spa-nha-trang/cover.jpg',
        5, 9.1, 1847, 'RESORT', 'APPROVED', 4, NOW(), NOW()
    ),
    (
        'Furama Resort Đà Nẵng',
        'Khu nghỉ dưỡng 5 sao hướng biển Mỹ Khê nổi tiếng. Hồ bơi nước ngọt + muối, nhà hàng fine dining, spa phong cách Á Đông, bãi biển riêng 200m.',
        '68 Hồ Xuân Hương, Mỹ An',
        'Đà Nẵng',
        '/assets/images/accommodations/resort/furama-resort-da-nang/cover.jpg',
        5, 9.0, 1203, 'RESORT', 'APPROVED', 4, NOW(), NOW()
    );

    -- ─── TÌNH HUỐNG: accommodation PENDING + REJECTED để kiểm tra luồng duyệt ─
    -- partner1 (id=3) gửi 1 hotel PENDING mới chưa duyệt
    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at) VALUES
    (
        'Da Lat Mountain Boutique Hotel',
        'Khách sạn boutique phong cách núi rừng, view thung lũng Đà Lạt. Vừa được partner đăng ký — chờ Admin duyệt.',
        '101 Triệu Việt Vương, Phường 4',
        'Đà Lạt',
        '/assets/images/accommodations/catalog/hotel-exterior.jpg',
        4, 0.0, 0, 'HOTEL', 'PENDING', 3, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)
    ),
    -- partner1 (id=3) có 1 hotel đã bị từ chối
    (
        'Da Lat Sunrise Guesthouse',
        'Listing bị Admin từ chối do ảnh đại diện và thông tin pháp lý chưa đạt yêu cầu.',
        '12 Đường Hoa Ban, Phường 3',
        'Đà Lạt',
        '',
        2, 0.0, 0, 'HOTEL', 'REJECTED', 3, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
    );

    -- Rooms – Resort 1: Vinpearl Nha Trang (id=8) — RESORT (default 18%)
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category, commission_rate_override) VALUES
    (8, 'VNT-DLX', 'Deluxe Ocean View',       '1 giường King size',            2, 2800000, 10,
    'Phòng Deluxe 42m² view biển, bao gồm vé cáp treo và công viên giải trí Vinpearl Land.',
    '/assets/images/accommodations/resort/vinpearl-resort-spa-nha-trang/rooms/vnt-dlx.jpg',
    'DELUXE', NULL),      -- ← null = RESORT default 18%
    (8, 'VNT-SUI', 'Junior Suite Beachfront', '1 giường King cỡ lớn',          2, 4500000, 5,
    'Suite 65m² ban công hướng biển, bồn tắm jacuzzi trong phòng, dịch vụ butler cao cấp.',
    '/assets/images/accommodations/resort/vinpearl-resort-spa-nha-trang/rooms/vnt-sui.jpg',
    'SUITE', 22.00),      -- ← 22% override (Junior Suite Beachfront premium)
    (8, 'VNT-VIL', 'Pool Villa',              '2 giường King',                  4, 9800000, 2,
    'Villa riêng 150m² với hồ bơi private, 2 phòng ngủ, bếp ăn, phù hợp gia đình hoặc tuần trăng mật.',
    '/assets/images/accommodations/resort/vinpearl-resort-spa-nha-trang/rooms/vnt-vil.jpg',
    'FAMILY', 15.00);     -- ← 15% override (ưu đãi Pool Villa gia đình)

    -- Rooms – Resort 2: Furama Đà Nẵng (id=9) — RESORT (default 18%)
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category, commission_rate_override) VALUES
    (9, 'FDN-DLX', 'Deluxe Garden View',      '1 giường King size',            2, 2400000, 8,
    'Phòng 40m² view vườn nhiệt đới, bao gồm bữa sáng buffet tại nhà hàng La Maison 1888.',
    '/assets/images/accommodations/resort/furama-resort-da-nang/rooms/fdn-dlx.jpg',
    'DELUXE', NULL),      -- ← null = RESORT default 18%
    (9, 'FDN-BCH', 'Beachfront Superior',     '1 giường King size',            2, 3600000, 6,
    'Phòng 48m² view biển Mỹ Khê, bãi tắm riêng 200m, ghế nằm và ô dù phục vụ tận nơi.',
    '/assets/images/accommodations/resort/furama-resort-da-nang/rooms/fdn-bch.jpg',
    'VIP', 20.00),        -- ← 20% override (Beachfront VIP cao cấp)
    (9, 'FDN-FAM', 'Family Suite',            '2 giường đôi',                  5, 5200000, 3,
    'Suite gia đình 80m², 2 phòng ngủ, phòng khách riêng, view biển panorama, bếp nhỏ.',
    '/assets/images/accommodations/resort/furama-resort-da-nang/rooms/fdn-fam.jpg',
    'FAMILY', 15.00);     -- ← 15% override (ưu đãi Family Suite resort)

    -- =============================================
    -- BOOKING + PAYMENT SAMPLE DATA
    -- user_id=2 (user@travelmate.vn)
    -- =============================================

    -- ─── Booking 1: Cọc 30% — CONFIRMED (TravelMate tự giữ phòng) ───────────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-LATA-STD-0001', 2, 1, 1,
        DATE_ADD(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 5 DAY),
        2, 0, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        1300000, 390000, 910000,
        'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'VNPAY ghi nhận thành công — khách đã cọc 30%, TravelMate tự động giữ phòng.',
        NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 390000, CONCAT('TXN-', UNIX_TIMESTAMP()*1000), 'APPROVED', NOW(), 'VNPAY ghi nhận thành công — cọc 30%, TravelMate tự động giữ phòng.');

    -- ─── Booking 2: 100% — CONFIRMED (TravelMate tự giữ phòng) ───
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-TLP-SUP-0001', 2, 2, 6,
        DATE_ADD(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 9 DAY),
        2, 0, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        1240000, 1240000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'VNPAY ghi nhận thành công — khách thanh toán 100%, TravelMate tự động giữ phòng.',
        NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 1240000, CONCAT('TXN-', UNIX_TIMESTAMP()*1001), 'APPROVED', NOW(), 'VNPAY ghi nhận thành công — thanh toán 100%, TravelMate tự động giữ phòng.');

    -- ─── Booking 3: Cọc 30% — CONFIRMED (TravelMate tự giữ phòng) ─
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-TMG-DLX-0001', 2, 3, 9,
        DATE_ADD(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 3 DAY),
        2, 0, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        2400000, 720000, 1680000,
        'CONFIRMED', 'DEPOSIT_30', 'APPROVED', 'PARTNER_CONFIRMED', 'TravelMate tự động giữ phòng sau khi ghi nhận cọc 30%.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 720000, CONCAT('TXN-', UNIX_TIMESTAMP()*1002), 'APPROVED', NOW(), 'VNPAY ghi nhận thành công - cọc 30%');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 9;

    -- ─── Booking 4: 100% — CONFIRMED (partner đã giữ phòng) ─────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-LATA-DLX-0001', 2, 1, 2,
        DATE_ADD(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 4 DAY),
        2, 1, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        1700000, 1700000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Partner đã giữ phòng cho khách.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 1700000, CONCAT('TXN-', UNIX_TIMESTAMP()*1003), 'APPROVED', NOW(), 'VNPAY ghi nhận thành công - thanh toán đủ');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 2;

    -- ─── Booking 5: NO_SHOW — Cọc 30%, mất cọc ────────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        note, created_at, updated_at)
    VALUES (
        'BK-TLP-STD-0001', 2, 2, 5,
        DATE_SUB(CURDATE(), INTERVAL 2 DAY), CURDATE(),
        2, 0, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        960000, 288000, 672000,
        'NO_SHOW', 'DEPOSIT_30', 'DEPOSIT_FORFEITED',
        'Khách không đến check-in. Cọc 30% (288.000đ) bị giữ; hoa hồng tính trên tổng đơn gốc 960.000đ = 144.000đ; Partner nhận 144.000đ.',
        DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 288000, CONCAT('TXN-', UNIX_TIMESTAMP()*1004), 'DEPOSIT_FORFEITED', DATE_SUB(NOW(), INTERVAL 5 DAY), 'Cọc 30% bị giữ lại do khách không đến check-in.');

    -- ─── Booking 6: CHECKED_IN — 100% (partner1 - acc3 TM Grand) ─
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-TMG-PRE-0001', 2, 3, 10,
        DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 2 DAY),
        2, 0, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        4950000, 4950000, 0,
        'CHECKED_IN', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Khách đã check-in. Partner đã giữ phòng.',
        DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 4950000, CONCAT('TXN-', UNIX_TIMESTAMP()*1005), 'APPROVED', DATE_SUB(NOW(), INTERVAL 7 DAY), 'Thanh toán 100% — đã check-in');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 10;

    -- ─── Booking 7: COMPLETED — đã hoàn tất (partner1 - acc1) ────────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-LATA-FAM-0001', 2, 1, 3,
        DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_SUB(CURDATE(), INTERVAL 7 DAY),
        3, 1, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        3600000, 3600000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Khách đã checkout. Đơn đặt phòng hoàn tất. Phòng đã được mở lại.',
        DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 3600000, CONCAT('TXN-', UNIX_TIMESTAMP()*1006), 'APPROVED', DATE_SUB(NOW(), INTERVAL 12 DAY), 'Thanh toán 100% — đã hoàn tất');

    -- ─── Booking 8: CONFIRMED — acc8 Vinpearl (partner2), TravelMate tự giữ phòng ───
    -- Lưu ý: acc8 Vinpearl có rooms: VNT-DLX(id=25), VNT-SUI(id=26), VNT-VIL(id=27)
    -- Dùng room 25 (VNT-DLX), accommodation_id=8
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-VNT-DLX-0001', 2, 8, 25,
        DATE_ADD(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 8 DAY),
        2, 1, 1,
        'Trần Thị Bích', '0988 111 222', 'user@travelmate.vn',
        8400000, 2520000, 5880000,
        'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'TravelMate tự động giữ phòng sau khi ghi nhận cọc 30%.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 2520000, CONCAT('TXN-', UNIX_TIMESTAMP()*1007), 'APPROVED', NOW(), 'Cọc 30% Vinpearl DLX — TravelMate tự động giữ phòng');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 25;

    -- ─── Booking 9: CONFIRMED — acc8 Vinpearl (partner2), đã giữ phòng ───
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-VNT-SUI-0001', 2, 8, 26,
        DATE_ADD(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY),
        2, 0, 1,
        'Lê Minh Đức', '0977 333 444', 'user@travelmate.vn',
        13500000, 13500000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'TravelMate đã giữ phòng/căn trên hệ thống. Partner có thể check-in khi khách đến.',
        DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 13500000, CONCAT('TXN-', UNIX_TIMESTAMP()*1008), 'APPROVED', DATE_SUB(NOW(), INTERVAL 2 DAY), '100% Vinpearl SUI — TravelMate đã giữ phòng/căn trên hệ thống');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 26;

    -- ─── Booking 10: COMPLETED — acc7 Mộc Nhiên Homestay (partner2) ───
    -- acc7 Mộc Nhiên có rooms: MND-STD(id=22), MND-ATT(id=23), MND-FAM(id=24)
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-MND-ATT-0001', 2, 7, 23,
        DATE_SUB(CURDATE(), INTERVAL 8 DAY), DATE_SUB(CURDATE(), INTERVAL 5 DAY),
        2, 0, 1,
        'Phạm Quỳnh Anh', '0966 555 666', 'user@travelmate.vn',
        1740000, 1740000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Khách đã checkout. Đơn Homestay Mộc Nhiên hoàn tất.',
        DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 1740000, CONCAT('TXN-', UNIX_TIMESTAMP()*1009), 'APPROVED', DATE_SUB(NOW(), INTERVAL 10 DAY), 'Homestay Mộc Nhiên — hoàn tất');

    -- ─── Booking 11: CONFIRMED — acc4 Anam Villa (partner2), TravelMate tự giữ villa ───
    -- acc4 Anam Villa có rooms: ANM-GDN(id=13), ANM-BCH(id=14), ANM-FAM(id=15)
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-ANM-GDN-0001', 2, 4, 13,
        DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 13 DAY),
        2, 0, 1,
        'Hoàng Văn Hùng', '0911 777 888', 'user@travelmate.vn',
        10500000, 3150000, 7350000,
        'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'VNPAY ghi nhận thành công — cọc 30% Anam Garden Villa, TravelMate tự động giữ villa.',
        NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 3150000, CONCAT('TXN-', UNIX_TIMESTAMP()*1010), 'APPROVED', NOW(), 'Cọc 30% Anam Garden Villa — VNPAY ghi nhận thành công, TravelMate tự động giữ villa.');

    -- ─── Booking 12: NO_SHOW — acc9 Furama (partner1), mất cọc ───
    -- acc9 Furama có rooms: FDN-DLX(id=28), FDN-BCH(id=29), FDN-FAM(id=30)
    -- NOTE: room IDs thực tế phụ thuộc thứ tự INSERT, xem comment bên dưới
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-FDN-DLX-0001', 2, 9, 28,
        DATE_SUB(CURDATE(), INTERVAL 3 DAY), DATE_SUB(CURDATE(), INTERVAL 1 DAY),
        2, 0, 1,
        'Ngô Thị Lan', '0922 999 000', 'user@travelmate.vn',
        4800000, 1440000, 3360000,
        'NO_SHOW', 'DEPOSIT_30', 'DEPOSIT_FORFEITED',
        'PARTNER_CONFIRMED', 'Khách không đến check-in. Cọc 30% (1.440.000đ) bị giữ; hoa hồng tính trên tổng đơn gốc 4.800.000đ = 864.000đ; Partner nhận 576.000đ.',
        DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 1440000, CONCAT('TXN-', UNIX_TIMESTAMP()*1011), 'DEPOSIT_FORFEITED', DATE_SUB(NOW(), INTERVAL 6 DAY), 'Cọc 30% Furama DLX — no-show mất cọc');

    -- ─── Booking 13: CONFIRMED — acc6 Hoa Lư (partner1), TravelMate tự giữ phòng ───
    -- acc6 Hoa Lư có rooms: HLR-STD(id=19), HLR-DLX(id=20), HLR-FAM(id=21)
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-HLR-DLX-0001', 2, 6, 20,
        DATE_ADD(CURDATE(), INTERVAL 4 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY),
        2, 0, 1,
        'Vũ Thị Mai', '0955 123 456', 'user@travelmate.vn',
        960000, 960000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'TravelMate tự động giữ phòng sau khi ghi nhận thanh toán 100%.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 960000, CONCAT('TXN-', UNIX_TIMESTAMP()*1012), 'APPROVED', NOW(), '100% Homestay Hội An — TravelMate tự động giữ phòng');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 20;

    -- ─── Booking 14: CHECKED_IN — acc5 Ba Na Villa (partner1) ───
    -- acc5 Ba Na Villa có rooms: BNH-BNG(id=16), BNH-TWN(id=17), BNH-SUI(id=18)
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-BNH-BNG-0001', 2, 5, 16,
        DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 2 DAY),
        2, 0, 1,
        'Đinh Văn Tùng', '0944 234 567', 'user@travelmate.vn',
        6600000, 6600000, 0,
        'CHECKED_IN', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Khách đã check-in tại Ba Na Villa.',
        DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 6600000, CONCAT('TXN-', UNIX_TIMESTAMP()*1013), 'APPROVED', DATE_SUB(NOW(), INTERVAL 3 DAY), '100% Ba Na Bungalow — đã check-in');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 16;

    -- ─── Booking 15: CONFIRMED — PARTNER_CANCELLED (partner từ chối, admin cần xử lý) ───
    -- acc2 Tulip Hotel (partner1), room TLP-SUP (id=6): partner báo không thể tiếp nhận khách
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-TLP-SUP-0002', 2, 2, 6,
        DATE_ADD(CURDATE(), INTERVAL 4 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY),
        2, 0, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        1240000, 1240000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CANCELLED', 'Partner từ chối: phòng đã bị double-booking từ kênh khác. Admin cần hủy và hoàn tiền khách.',
        DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 1240000, CONCAT('TXN-', UNIX_TIMESTAMP()*1014), 'APPROVED', DATE_SUB(NOW(), INTERVAL 1 DAY), 'Thanh toán 100% — partner đã hủy, cần admin xử lý hoàn tiền');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 6;

    -- =============================================
    -- REVIEWS SAMPLE DATA — cho booking COMPLETED
    -- =============================================

    -- Review cho Booking #7 (BK-LATA-FAM-0001, acc1 LATA Hotel, COMPLETED)
    -- booking_id = 7, user_id = 2, accommodation_id = 1
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 1, 7, 10, 'Phòng rất sạch sẽ, nhân viên nhiệt tình. View đẹp, gần trung tâm chợ đêm Đà Lạt. Phòng gia đình rộng rãi, rất phù hợp cho gia đình có trẻ nhỏ. Lần sau sẽ quay lại!', DATE_SUB(NOW(), INTERVAL 7 DAY));

    -- Review cho Booking #10 (BK-MND-ATT-0001, acc7 Mộc Nhiên Homestay, COMPLETED)
    -- booking_id = 10, user_id = 2, accommodation_id = 7
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 7, 10, 8, 'Homestay rất yên tĩnh, không gian thơ mộng giữa vườn hoa dã quỳ. Phòng áp mái có cửa sổ kính nhìn đồi thông rất lãng mạn. WiFi buổi tối hơi yếu nhưng nhìn chung rất đáng tiền.', DATE_SUB(NOW(), INTERVAL 5 DAY));

    -- Cập nhật rating + reviewCount cho accommodation sau khi thêm review
    -- acc1 LATA Hotel: 1 review, 10 điểm → rating = 10.0
    UPDATE accommodations SET rating = 10.0, review_count = 1 WHERE id = 1;
    -- acc7 Mộc Nhiên: 1 review, 8 điểm → rating = 8.0
    UPDATE accommodations SET rating = 8.0, review_count = 1 WHERE id = 7;


    -- =============================================
    -- HƯỚNG DẪN TÀI KHOẢN TRÌNH BÀY
    -- =============================================
    -- ┌──────────┬───────────────────────────┬────────────┐
    -- │   ROLE   │          EMAIL            │  PASSWORD  │
    -- ├──────────┼───────────────────────────┼────────────┤
    -- │  ADMIN   │ admin@travelmate.vn       │  admin123  │
    -- │  USER    │ user@travelmate.vn        │  user123   │
    -- │ PARTNER  │ partner@travelmate.vn     │ partner123 │
    -- │ PARTNER2 │ partner2@travelmate.vn    │ partner123 │
    -- └──────────┴───────────────────────────┴────────────┘
    --
    -- ACCOMMODATIONS (11 + 2 PENDING/REJECTED):
    -- Hotel  (id=1,2,3) → owner: partner1 (id=3) — Đà Lạt
    -- Villa  (id=4,5)   → owner: partner2 (id=4), partner1 (id=3)
    -- Homestay (id=6,7) → owner: partner1 (id=3), partner2 (id=4)
    -- Resort (id=8,9)   → owner: partner2 (id=4), partner1 (id=3)
    -- id=10 [PENDING]   → partner1 chưa duyệt
    -- id=11 [REJECTED]  → partner1 bị từ chối
    --
    -- ROOMS (30 rooms):
    -- acc1 (LATA Hotel):    id=1(LATA-STD), 2(LATA-DLX), 3(LATA-FAM), 4(LATA-SUI)
    -- acc2 (Tulip Hotel):   id=5(TLP-STD),  6(TLP-SUP),  7(TLP-FAM),  8(TLP-VIP)
    -- acc3 (TM Grand):      id=9(TMG-DLX), 10(TMG-PRE), 11(TMG-FAM), 12(TMG-PRE2)
    -- acc4 (Anam Villa):    id=13(ANM-GDN), 14(ANM-BCH), 15(ANM-FAM)
    -- acc5 (Ba Na Villa):   id=16(BNH-BNG), 17(BNH-TWN), 18(BNH-SUI)
    -- acc6 (Hoa Lư):        id=19(HLR-STD), 20(HLR-DLX), 21(HLR-FAM)
    -- acc7 (Mộc Nhiên):     id=22(MND-STD), 23(MND-ATT), 24(MND-FAM)
    -- acc8 (Vinpearl):      id=25(VNT-DLX), 26(VNT-SUI), 27(VNT-VIL)
    -- acc9 (Furama):        id=28(FDN-DLX), 29(FDN-BCH), 30(FDN-FAM)
    --
    -- PARTNER OWNERSHIP:
    -- partner1 (id=3): acc1,2,3 (Hotel Đà Lạt) + acc5 (Ba Na Villa) + acc6 (Hoa Lư Homestay) + acc9 (Furama Resort)
    -- partner2 (id=4): acc4 (Anam Villa) + acc7 (Mộc Nhiên Homestay) + acc8 (Vinpearl Resort)
    --
    -- 14 SAMPLE BOOKINGS:
    -- #1  BK-LATA-STD-0001 | acc1/r1  | CONFIRMED | Cọc 30%, TravelMate tự giữ phòng
    -- #2  BK-TLP-SUP-0001  | acc2/r6  | CONFIRMED | Thanh toán 100%, TravelMate tự giữ phòng
    -- #3  BK-TMG-DLX-0001  | acc3/r9  | CONFIRMED | PARTNER_CONFIRMED (TravelMate tự giữ)
    -- #4  BK-LATA-DLX-0001 | acc1/r2  | CONFIRMED | PARTNER_CONFIRMED (partner1 đã giữ phòng)
    -- #5  BK-TLP-STD-0001  | acc2/r5  | NO_SHOW   | DEPOSIT_FORFEITED (mất cọc)
    -- #6  BK-TMG-PRE-0001  | acc3/r10 | CHECKED_IN | PARTNER_CONFIRMED (partner1)
    -- #7  BK-LATA-FAM-0001 | acc1/r3  | COMPLETED  | PARTNER_COMPLETED (partner1, đồng bộ cuối script)
    -- #8  BK-VNT-DLX-0001  | acc8/r25 | CONFIRMED | PARTNER_CONFIRMED (TravelMate tự giữ)
    -- #9  BK-VNT-SUI-0001  | acc8/r26 | CONFIRMED | PARTNER_CONFIRMED (TravelMate đã giữ phòng/căn)
    -- #10 BK-MND-ATT-0001  | acc7/r23 | COMPLETED  | PARTNER_COMPLETED (partner4, đồng bộ cuối script)
    -- #11 BK-ANM-GDN-0001  | acc4/r13 | CONFIRMED | Cọc 30%, TravelMate tự giữ villa
    -- #12 BK-FDN-DLX-0001  | acc9/r28 | NO_SHOW   | DEPOSIT_FORFEITED (partner1 - Furama)
    -- #13 BK-HLR-DLX-0001  | acc6/r20 | CONFIRMED | PARTNER_CONFIRMED (TravelMate tự giữ)
    -- #14 BK-BNH-BNG-0001  | acc5/r16 | CHECKED_IN | PARTNER_CONFIRMED (partner1 - Ba Na Villa)
    --
    -- KỊCH BẢN TRÌNH BÀY:
    -- Admin login → /admin/bookings → thấy 15 bookings
    --   → Ghi nhận #1 (BK-LATA-STD-0001): CONFIRMED, partner_status=PARTNER_CONFIRMED
    --   → Ghi nhận #2 (BK-TLP-SUP-0001): CONFIRMED, partner_status=PARTNER_CONFIRMED
    --   → Xem #15 (BK-TLP-SUP-0002): có nút "🚫 Xử lý partner hủy" vì PARTNER_CANCELLED
    -- Partner1 login → /partner/bookings
    --   → Nút "Check-in khách" ở booking CONFIRMED + PARTNER_CONFIRMED
    --   → Nút "Hoàn tất / Check-out" ở booking CHECKED_IN (#6, #14)
    -- Admin Chi tiết booking → /admin/bookings/{id}
    --   → Phần "Admin Override" (collapsible) ở CONFIRMED/CHECKED_IN
    --   → Chỉ dùng khi partner không thao tác được
    -- Admin → /admin/settlements → "Tạo quyết toán tháng trước" → generate-monthly

    -- =============================================
    -- DỮ LIỆU KIỂM TRA TỒN PHÒNG — Booking cho khoảng +10 → +14 ngày
    -- Mục đích: trình bày trang /admin/availability và /partner/availability
    --
    -- Kịch bản kiểm tra ngày +10 → +14:
    --   LATA-STD    (5 phòng): 0 bị giữ → 5 còn trống  → AVAILABLE
    --   LATA-DLX    (4 phòng): 3 bị giữ → 1 còn trống  → LIMITED
    --   LATA-SUI    (1 phòng): 1 bị giữ → 0 còn trống  → FULL
    --   TMG-DLX     (8 phòng): 7 bị giữ → 1 còn trống  → LIMITED
    --   VNT-DLX    (10 phòng): 9 bị giữ → 1 còn trống  → LIMITED
    --   VNT-SUI     (5 phòng): 4 bị giữ → 1 còn trống  → LIMITED
    --   ANM-BCH     (2 phòng): 2 bị giữ → 0 còn trống  → FULL
    --   HLR-STD     (4 phòng): 3 bị giữ → 1 còn trống  → LIMITED
    -- =============================================

    -- ─── LATA Hotel (acc1): LATA-DLX (r2, hiện còn 3) ────────────────
    -- Booking #15: user2 (id=5), 1 phòng, +10→+14
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-LATA-DLX-0002', 5, 1, 2,
        DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY),
        2, 0, 1,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        3400000, 3400000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Partner1 đã giữ phòng Deluxe.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 3400000, CONCAT('TXN-', UNIX_TIMESTAMP()*2001), 'APPROVED', NOW(), '100% LATA-DLX #2 — partner đã giữ phòng');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 2;

    -- Booking #16: user3 (id=6), 2 phòng, +10→+14
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-LATA-DLX-0003', 6, 1, 2,
        DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY),
        3, 1, 2,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        6800000, 2040000, 4760000,
        'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'Partner1 đã giữ 2 phòng Deluxe cho nhóm.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 2040000, CONCAT('TXN-', UNIX_TIMESTAMP()*2002), 'APPROVED', NOW(), 'Cọc 30% LATA-DLX #3 — 2 phòng');
    UPDATE rooms SET available_quantity = available_quantity - 2 WHERE id = 2;

    -- ─── LATA Hotel: LATA-SUI (r4, 1 phòng → FULL) ───────────────────
    -- Booking #17: user2, 1 phòng, +10→+14
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-LATA-SUI-0001', 5, 1, 4,
        DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY),
        2, 0, 1,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        7200000, 7200000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Suite đặt kín từ +10 đến +14.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 7200000, CONCAT('TXN-', UNIX_TIMESTAMP()*2003), 'APPROVED', NOW(), '100% LATA-SUI — FULL');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 4;

    -- ─── TM Grand (acc3): TMG-DLX (r9, hiện còn 7) → chiếm 7 phòng → LIMITED ──
    -- Booking #18: user3, 2 phòng, +10→+14
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-TMG-DLX-0002', 6, 3, 9,
        DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY),
        3, 1, 2,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        9600000, 9600000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Grand Deluxe — 2 phòng nhóm bạn.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 9600000, CONCAT('TXN-', UNIX_TIMESTAMP()*2004), 'APPROVED', NOW(), '100% TMG-DLX #2 2 phòng');
    UPDATE rooms SET available_quantity = available_quantity - 2 WHERE id = 9;

    -- Booking #19: user2, 2 phòng, +10→+14
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-TMG-DLX-0003', 5, 3, 9,
        DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY),
        2, 2, 2,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        9600000, 2880000, 6720000,
        'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'Grand Deluxe — 2 phòng gia đình.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 2880000, CONCAT('TXN-', UNIX_TIMESTAMP()*2005), 'APPROVED', NOW(), 'Cọc 30% TMG-DLX #3 2 phòng');
    UPDATE rooms SET available_quantity = available_quantity - 2 WHERE id = 9;

    -- Booking #20: user3, 3 phòng, +10→+14 → tổng 7 phòng bị giữ
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-TMG-DLX-0004', 6, 3, 9,
        DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY),
        5, 2, 3,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        14400000, 14400000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Grand Deluxe — 3 phòng đoàn khách.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 14400000, CONCAT('TXN-', UNIX_TIMESTAMP()*2006), 'APPROVED', NOW(), '100% TMG-DLX #4 3 phòng');
    UPDATE rooms SET available_quantity = available_quantity - 3 WHERE id = 9;

    -- ─── Vinpearl (acc8): VNT-DLX (r25, hiện còn 9) → chiếm 9 phòng → LIMITED ──
    -- Booking #21: user2, 3 phòng, +10→+14
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-VNT-DLX-AV21', 5, 8, 25,
        DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY),
        4, 2, 3,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        33600000, 33600000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Vinpearl DLX — 3 phòng đoàn nghỉ dưỡng.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 33600000, CONCAT('TXN-', UNIX_TIMESTAMP()*2007), 'APPROVED', NOW(), '100% VNT-DLX #2 3 phòng');
    UPDATE rooms SET available_quantity = available_quantity - 3 WHERE id = 25;

    -- Booking #22: user3, 3 phòng, +10→+14
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-VNT-DLX-AV22', 6, 8, 25,
        DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY),
        5, 1, 3,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        33600000, 10080000, 23520000,
        'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'Vinpearl DLX — 3 phòng cọc 30%.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 10080000, CONCAT('TXN-', UNIX_TIMESTAMP()*2008), 'APPROVED', NOW(), 'Cọc 30% VNT-DLX #3 3 phòng');
    UPDATE rooms SET available_quantity = available_quantity - 3 WHERE id = 25;

    -- Booking #23: user2, 3 phòng, +10→+14 → tổng 9 phòng bị giữ trong range
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-VNT-DLX-AV23', 5, 8, 25,
        DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY),
        6, 0, 3,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        33600000, 33600000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Vinpearl DLX — 3 phòng cuối tuần.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 33600000, CONCAT('TXN-', UNIX_TIMESTAMP()*2009), 'APPROVED', NOW(), '100% VNT-DLX #4 3 phòng');
    UPDATE rooms SET available_quantity = available_quantity - 3 WHERE id = 25;

    -- ─── Vinpearl: VNT-SUI (r26, hiện còn 4 sau bk#9) → chiếm 4 → LIMITED ──────
    -- Booking #24: user3, 2 phòng, +10→+14
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-VNT-SUI-AV24', 6, 8, 26,
        DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY),
        3, 1, 2,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        36000000, 36000000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Suite Beachfront 2 phòng cao cấp.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 36000000, CONCAT('TXN-', UNIX_TIMESTAMP()*2010), 'APPROVED', NOW(), '100% VNT-SUI #2 2 phòng');
    UPDATE rooms SET available_quantity = available_quantity - 2 WHERE id = 26;

    -- Booking #25: user2, 2 phòng, +10→+14 → tổng 4 bị giữ (từ bk#9 không overlap), 1 còn lại
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-VNT-SUI-AV25', 5, 8, 26,
        DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY),
        4, 0, 2,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        36000000, 10800000, 25200000,
        'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'Suite Beachfront cọc 30%.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 10800000, CONCAT('TXN-', UNIX_TIMESTAMP()*2011), 'APPROVED', NOW(), 'Cọc 30% VNT-SUI #3 2 phòng');
    UPDATE rooms SET available_quantity = available_quantity - 2 WHERE id = 26;

    -- ─── Anam Villa: ANM-BCH (r14, 2 phòng) → FULL ───────────────────
    -- Booking #26: user2, 2 phòng, +10→+14 → FULL
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-ANM-BCH-AV26', 5, 4, 14,
        DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY),
        4, 0, 2,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        46400000, 46400000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Anam Beachfront đặt kín 2 villa — FULL.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 46400000, CONCAT('TXN-', UNIX_TIMESTAMP()*2012), 'APPROVED', NOW(), '100% ANM-BCH 2 phòng — FULL');
    UPDATE rooms SET available_quantity = available_quantity - 2 WHERE id = 14;

    -- ─── Hoa Lư Homestay: HLR-STD (r19, 4 phòng) → LIMITED ──────────
    -- Booking #27: user3, 2 phòng, +10→+14
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-HLR-STD-AV27', 6, 6, 19,
        DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY),
        3, 1, 2,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        2560000, 2560000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Hoa Lư Standard 2 phòng.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 2560000, CONCAT('TXN-', UNIX_TIMESTAMP()*2013), 'APPROVED', NOW(), '100% HLR-STD 2 phòng');
    UPDATE rooms SET available_quantity = available_quantity - 2 WHERE id = 19;

    -- Booking #28: user2, 1 phòng, +10→+14 → tổng 3/4 bị giữ, 1 còn lại = LIMITED
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-HLR-STD-AV28', 5, 6, 19,
        DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY),
        2, 0, 1,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        1280000, 384000, 896000,
        'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'Hoa Lư Standard 1 phòng — TravelMate tự động giữ phòng.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 384000, CONCAT('TXN-', UNIX_TIMESTAMP()*2014), 'APPROVED', NOW(), 'Cọc 30% HLR-STD #2');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 19;

    -- =============================================
    -- BACKFILL — Chạy nếu DB cũ thiếu cột owner_id
    -- =============================================
    -- UPDATE accommodations
    -- SET owner_id = (SELECT id FROM users WHERE email = 'partner@travelmate.vn' LIMIT 1)
    -- WHERE owner_id IS NULL AND property_type = 'HOTEL';
    --
    -- UPDATE bookings SET partner_status = 'PARTNER_CONFIRMED'
    -- WHERE booking_status = 'CONFIRMED' AND partner_status IS NULL;

    -- =============================================
    -- DỮ LIỆU KHỞI TẠO — VOUCHER
    -- =============================================
    -- Voucher do Admin tạo (USER_GLOBAL, costBearer=ADMIN)
    INSERT INTO vouchers (code, name, description, discount_type, discount_value,
        max_discount_amount, min_order_amount, start_date, end_date,
        active, voucher_scope, cost_bearer, owner_id, created_at) VALUES
    ('SUMMER10',  'Ưu đãi Hè 10%',    'Giảm 10% tối đa 500.000đ cho mọi đặt phòng mùa hè.',
        'PERCENT', 10.00, 500000, 500000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 1, 'USER_GLOBAL', 'ADMIN', NULL, NOW()),
    ('WELCOME50', 'Chào mừng 50K',     'Giảm 50.000đ cho đặt phòng đầu tiên — áp dụng đơn tối thiểu 200.000đ.',
        'FIXED_AMOUNT', 50000, NULL, 200000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 1, 'USER_GLOBAL', 'ADMIN', NULL, NOW()),
    ('TRAVEL15',  'Ưu đãi TravelMate', 'Giảm 15% tối đa 1.000.000đ cho đặt phòng Resort hoặc Villa cao cấp.',
        'PERCENT', 15.00, 1000000, 2000000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 60 DAY), 1, 'USER_GLOBAL', 'ADMIN', NULL, NOW());

    -- Voucher do Admin phát hành; Partner chọn gắn vào phòng/căn của mình và chịu chi phí khi sử dụng.
    INSERT INTO vouchers (code, name, description, discount_type, discount_value,
        max_discount_amount, min_order_amount, start_date, end_date,
        active, voucher_scope, property_type, cost_bearer, owner_id, created_at) VALUES
    ('LATA20', 'LATA Hotel Ưu Đãi 20%', 'Voucher kho Hotel: giảm 20% cho phòng được đối tác chọn áp dụng.',
        'PERCENT', 20.00, 800000, 650000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 45 DAY), 1, 'PARTNER_ROOM', 'HOTEL', 'PARTNER', NULL, NOW()),
    ('VNT100K', 'Vinpearl Giảm 100K', 'Voucher kho Resort: giảm 100.000đ cho phòng được đối tác chọn áp dụng.',
        'FIXED_AMOUNT', 100000, NULL, 2800000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 1, 'PARTNER_ROOM', 'RESORT', 'PARTNER', NULL, NOW()),
    ('ANAM15', 'Anam Villa Ưu Đãi 15%', 'Voucher kho Villa: giảm 15% tối đa 1.200.000đ cho căn được chọn áp dụng.',
        'PERCENT', 15.00, 1200000, 3000000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 60 DAY), 1, 'PARTNER_ROOM', 'VILLA', 'PARTNER', NULL, NOW()),
    ('HOALUU50K', 'Hoa Lư Giảm 50K', 'Voucher kho Homestay: giảm 50.000đ cho phòng được chọn áp dụng.',
        'FIXED_AMOUNT', 50000, NULL, 600000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 45 DAY), 1, 'PARTNER_ROOM', 'HOMESTAY', 'PARTNER', NULL, NOW());

    -- Voucher kho do Admin phát hành để Partner chủ động gắn vào từng phòng/căn đã duyệt.
    INSERT INTO vouchers (code, name, description, discount_type, discount_value,
        max_discount_amount, min_order_amount, start_date, end_date,
        active, voucher_scope, property_type, cost_bearer, owner_id, created_at) VALUES
    ('STAYFLEX100', 'Admin phát hành - Hotel 100K', 'Voucher kho cho đối tác Hotel gắn vào phòng phù hợp.',
        'FIXED_AMOUNT', 100000, NULL, 1000000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 1, 'PARTNER_ROOM', 'HOTEL', 'PARTNER', NULL, NOW()),
    ('RESORT150', 'Admin phát hành - Resort 150K', 'Voucher kho cho đối tác Resort gắn vào phòng/căn phù hợp.',
        'FIXED_AMOUNT', 150000, NULL, 1500000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 1, 'PARTNER_ROOM', 'RESORT', 'PARTNER', NULL, NOW()),
    ('VILLA200', 'Admin phát hành - Villa 200K', 'Voucher kho cho đối tác Villa gắn vào căn phù hợp.',
        'FIXED_AMOUNT', 200000, NULL, 2500000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 1, 'PARTNER_ROOM', 'VILLA', 'PARTNER', NULL, NOW()),
    ('HOMESTAY50', 'Admin phát hành - Homestay 50K', 'Voucher kho cho đối tác Homestay gắn vào phòng phù hợp.',
        'FIXED_AMOUNT', 50000, NULL, 500000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 1, 'PARTNER_ROOM', 'HOMESTAY', 'PARTNER', NULL, NOW()),
    ('TMROOM80', 'Admin phát hành - Toàn bộ phòng 80K', 'Voucher kho chung cho mọi loại đối tác.',
        'FIXED_AMOUNT', 80000, NULL, 800000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 1, 'PARTNER_ROOM', NULL, 'ADMIN', NULL, NOW());

    -- Voucher kho Partner bổ sung: dữ liệu chiến dịch thực tế cho trang booking và luồng đặt cọc 30%.
    INSERT INTO vouchers (code, name, description, discount_type, discount_value,
        max_discount_amount, min_order_amount, start_date, end_date,
        active, voucher_scope, property_type, cost_bearer, owner_id, created_at) VALUES
    ('HOTEL10',  'Ưu đãi khách sạn 10%',  'Giảm 10% cho phòng khách sạn được đối tác chọn áp dụng.',
        'PERCENT', 10.00, 500000, 500000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 1, 'PARTNER_ROOM', NULL, 'PARTNER', NULL, NOW()),
    ('EARLY10',  'Đặt sớm tiết kiệm 10%',  'Giảm 10% cho khách đặt phòng trước ngày lưu trú.',
        'PERCENT', 10.00, 500000, 500000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 1, 'PARTNER_ROOM', NULL, 'PARTNER', NULL, NOW()),
    ('STAY10', 'Kỳ nghỉ linh hoạt 10%', 'Giảm 10% cho phòng/căn được đối tác bật khuyến mãi.',
        'PERCENT', 10.00, 500000, 500000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 1, 'PARTNER_ROOM', NULL, 'PARTNER', NULL, NOW()),
    ('WEEKEND15', 'Cuối tuần giảm 15%', 'Giảm 15% cho lịch lưu trú cuối tuần tại phòng/căn được chọn.',
        'PERCENT', 15.00, 600000, 500000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 1, 'PARTNER_ROOM', NULL, 'PARTNER', NULL, NOW()),
    ('FAMILY15', 'Gia đình vui hè 15%', 'Giảm 15% cho nhóm khách gia đình đặt phòng/căn phù hợp.',
        'PERCENT', 15.00, 600000, 500000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 1, 'PARTNER_ROOM', NULL, 'PARTNER', NULL, NOW()),
    ('ROOM100K', 'Giảm 100K đặt phòng', 'Giảm trực tiếp 100.000đ cho đơn đặt phòng đủ điều kiện.',
        'FIXED_AMOUNT', 100000, NULL, 500000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 1, 'PARTNER_ROOM', NULL, 'PARTNER', NULL, NOW()),
    ('STAY150K', 'Ở 2 đêm giảm 150K', 'Giảm trực tiếp 150.000đ cho kỳ nghỉ từ 2 đêm.',
        'FIXED_AMOUNT', 150000, NULL, 500000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 1, 'PARTNER_ROOM', NULL, 'PARTNER', NULL, NOW()),
    ('STAY200K', 'Kỳ nghỉ cao cấp giảm 200K', 'Giảm trực tiếp 200.000đ cho đơn đặt phòng/căn giá trị cao.',
        'FIXED_AMOUNT', 200000, NULL, 500000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 1, 'PARTNER_ROOM', NULL, 'PARTNER', NULL, NOW());

    INSERT INTO room_voucher_assignments
        (voucher_id, room_id, assigned_by_partner_id, active, assigned_at)
    SELECT v.id, r.id, a.owner_id, 1, NOW()
    FROM vouchers v
    JOIN rooms r ON (
        (v.code = 'LATA20' AND r.room_code IN ('LATA-STD', 'LATA-DLX', 'LATA-FAM'))
        OR (v.code = 'VNT100K' AND r.room_code = 'VNT-DLX')
        OR (v.code = 'ANAM15' AND r.room_code IN ('ANM-BCH', 'BNH-SUI'))
        OR (v.code = 'HOALUU50K' AND r.room_code IN ('HLR-DLX', 'MND-FAM'))
        OR (v.code = 'STAYFLEX100' AND r.room_code IN ('LATA-STD', 'LATA-DLX', 'TLP-SUP'))
        OR (v.code = 'RESORT150' AND r.room_code IN ('TMG-DLX', 'TMG-PRE', 'FUR-BCH'))
        OR (v.code = 'VILLA200' AND r.room_code IN ('ANM-BCH', 'BNA-BUN', 'VNT-VIL'))
        OR (v.code = 'HOMESTAY50' AND r.room_code IN ('MNH-STD', 'HLR-STD', 'HAN-STD'))
        OR (v.code = 'TMROOM80' AND r.room_code IN ('LATA-FAM', 'VNT-DLX', 'HLR-DLX'))
        OR (v.code IN ('HOTEL10', 'EARLY10', 'STAY10', 'WEEKEND15', 'FAMILY15',
                       'ROOM100K', 'STAY150K', 'STAY200K')
            AND r.room_code IN ('LATA-STD', 'TLP-VIP', 'TMG-DLX', 'ANM-GDN', 'HLR-STD', 'VNT-DLX'))
    )
    JOIN accommodations a ON a.id = r.accommodation_id
    WHERE v.code IN ('LATA20', 'VNT100K', 'ANAM15', 'HOALUU50K',
                     'STAYFLEX100', 'RESORT150', 'VILLA200', 'HOMESTAY50', 'TMROOM80',
                     'HOTEL10', 'EARLY10', 'STAY10', 'WEEKEND15', 'FAMILY15',
                     'ROOM100K', 'STAY150K', 'STAY200K')
      AND a.owner_id IS NOT NULL;

    INSERT INTO room_images (room_id, image_url, caption, sort_order, is_primary, created_at, updated_at)
    SELECT id, image_url, CONCAT(room_name, ' - ảnh đại diện'), 0, 1, NOW(), NOW()
    FROM rooms
    WHERE image_url IS NOT NULL AND image_url <> '';

    -- Album ảnh thật của LATA Hotel, được phân loại từ bộ tài nguyên trình bày.
    INSERT INTO room_images (room_id, image_url, caption, sort_order, is_primary, created_at, updated_at)
    SELECT r.id, p.image_url, p.caption, p.sort_order, 0, NOW(), NOW()
    FROM rooms r
    JOIN (
        SELECT 'LATA-STD' AS room_code, '/assets/images/accommodations/lata/rooms/standard-king/bathroom.jpg' AS image_url, 'Phòng Tiêu Chuẩn King - phòng tắm' AS caption, 1 AS sort_order
        UNION ALL SELECT 'LATA-STD', '/assets/images/accommodations/lata/rooms/standard-king/loft.jpg', 'Phòng Tiêu Chuẩn King - khu gác lửng', 2
        UNION ALL SELECT 'LATA-STD', '/assets/images/accommodations/lata/rooms/standard-king/lounge.jpg', 'Phòng Tiêu Chuẩn King - góc thư giãn', 3
        UNION ALL SELECT 'LATA-DLX', '/assets/images/accommodations/lata/rooms/deluxe-double/dining-area.jpg', 'Phòng Deluxe Giường Đôi - khu bàn ăn', 1
        UNION ALL SELECT 'LATA-DLX', '/assets/images/accommodations/lata/rooms/deluxe-double/kitchen.jpg', 'Phòng Deluxe Giường Đôi - bếp nhỏ', 2
        UNION ALL SELECT 'LATA-DLX', '/assets/images/accommodations/lata/rooms/deluxe-double/window.jpg', 'Phòng Deluxe Giường Đôi - cửa sổ', 3
        UNION ALL SELECT 'LATA-FAM', '/assets/images/accommodations/lata/rooms/family/bedroom.jpg', 'Phòng Gia Đình - phòng ngủ', 1
        UNION ALL SELECT 'LATA-FAM', '/assets/images/accommodations/lata/rooms/family/bedside.jpg', 'Phòng Gia Đình - khu đầu giường', 2
        UNION ALL SELECT 'LATA-FAM', '/assets/images/accommodations/lata/rooms/family/interior.jpg', 'Phòng Gia Đình - nội thất', 3
        UNION ALL SELECT 'LATA-FAM', '/assets/images/accommodations/lata/rooms/family/tv-area.jpg', 'Phòng Gia Đình - khu TV', 4
        UNION ALL SELECT 'LATA-SUI', '/assets/images/accommodations/lata/rooms/loft-suite/living-room.jpg', 'Suite Cao Cấp - phòng khách gác lửng', 1
        UNION ALL SELECT 'LATA-SUI', '/assets/images/accommodations/lata/rooms/loft-suite/kitchen-lounge.jpg', 'Suite Cao Cấp - bếp và phòng khách', 2
    ) p ON p.room_code = r.room_code;

    -- Voucher đã hết hạn — phục vụ kiểm tra tình huống không thể áp dụng
    INSERT INTO vouchers (code, name, description, discount_type, discount_value,
        max_discount_amount, min_order_amount, start_date, end_date,
        active, voucher_scope, cost_bearer, owner_id, created_at) VALUES
    ('EXPIRED2024', 'Khuyến mãi Tết 2024 (Hết hạn)', 'Voucher giảm 5% đã hết hạn — chỉ dùng kiểm tra bộ lọc.',
        'PERCENT', 5.00, 100000, 100000,
        DATE_SUB(CURDATE(), INTERVAL 120 DAY), DATE_SUB(CURDATE(), INTERVAL 60 DAY), 0, 'USER_GLOBAL', 'ADMIN', NULL, DATE_SUB(NOW(), INTERVAL 120 DAY));

    -- Voucher ngừng hoạt động — phục vụ kiểm tra bật/tắt trên trang Admin
    INSERT INTO vouchers (code, name, description, discount_type, discount_value,
        max_discount_amount, min_order_amount, start_date, end_date,
        active, voucher_scope, cost_bearer, owner_id, created_at) VALUES
    ('INACTIVE01', 'Flash Sale 30K (Tạm ngưng)', 'Voucher giảm 30.000đ tạm ngưng — admin có thể bật lại.',
        'FIXED_AMOUNT', 30000, NULL, 200000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 0, 'USER_GLOBAL', 'ADMIN', NULL, NOW());

    -- Partner settlements được tạo ở block monthly phía dưới để mỗi partner chỉ có 1 kỳ/tháng.
    -- Các booking hoàn tất trong tháng 05/2026 được gộp vào settlement PENDING tháng 05,
    -- không tạo PAID theo kỳ 7 ngày để tránh mâu thuẫn nghiệp vụ quyết toán tháng.

    -- =============================================
    -- KIỂM TRA SAU KHI CHẠY SQL
    -- =============================================
    -- SELECT id, name, property_type, approval_status, owner_id FROM accommodations ORDER BY id;
    -- SELECT id, room_code, accommodation_id FROM rooms ORDER BY id;
    -- SELECT booking_code, booking_status, payment_status, partner_status FROM bookings ORDER BY id;
    -- SELECT id, code, voucher_scope, cost_bearer, active FROM vouchers ORDER BY id;
    -- SELECT id, partner_id, settlement_status, payout_amount FROM partner_settlements ORDER BY id;
    -- Các số lượng tổng thể được kiểm tra ở checklist cuối file sau khi toàn bộ dữ liệu đã được nạp.

    -- =============================================
    -- DỮ LIỆU BỔ SUNG — Lịch sử đặt phòng (Bookings 15–22)
    -- Mục đích: dữ liệu phong phú cho Revenue/Settlement
    -- Trải đều nhiều tháng để biểu đồ revenue có đủ điểm dữ liệu
    -- =============================================

    -- ─── Booking 15: COMPLETED — LATA Hotel LATA-STD, voucher SUMMER10 (ADMIN) ───
    -- Kỳ tháng 04/2026. User được giảm 10%, admin chịu chi phí.
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        voucher_code, discount_amount, voucher_cost_bearer, total_before_discount,
        note, created_at, updated_at)
    VALUES (
        'BK-LATA-STD-0002', 2, 1, 1,
        DATE_SUB(CURDATE(), INTERVAL 15 DAY), DATE_SUB(CURDATE(), INTERVAL 13 DAY),
        2, 0, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        1170000, 1170000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'SUMMER10', 130000, 'ADMIN', 1300000,
        'Áp dụng voucher SUMMER10 giảm 10% (admin chịu).',
        DATE_SUB(NOW(), INTERVAL 17 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 1170000,
        CONCAT('TXN-LATA-STD2-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 17 DAY), 'LATA-STD 2 đêm với SUMMER10 — hoàn tất');

    -- ─── Booking 16: COMPLETED — Vinpearl VNT-DLX (partner2), không voucher ───
    -- Kỳ tháng 04/2026.
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, created_at, updated_at)
    VALUES (
        'BK-VNT-DLX-0002', 2, 8, 25,
        DATE_SUB(CURDATE(), INTERVAL 14 DAY), DATE_SUB(CURDATE(), INTERVAL 12 DAY),
        2, 0, 1,
        'Trần Thị Bích', '0988 111 222', 'user@travelmate.vn',
        5600000, 5600000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'Vinpearl Deluxe 2 đêm — hoàn tất.',
        DATE_SUB(NOW(), INTERVAL 16 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 5600000,
        CONCAT('TXN-VNT-DLX2-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 16 DAY), 'VNT-DLX 2 đêm — hoàn tất');

    -- ─── Booking 17: COMPLETED — LATA Hotel LATA-DLX, voucher LATA20 (PARTNER) ───
    -- Kỳ tháng 03/2026. Partner chịu chi phí voucher → trừ vào settlement.
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        voucher_code, discount_amount, voucher_cost_bearer, total_before_discount,
        note, created_at, updated_at)
    VALUES (
        'BK-LATA-DLX-HST17', 2, 1, 2,
        DATE_SUB(CURDATE(), INTERVAL 22 DAY), DATE_SUB(CURDATE(), INTERVAL 19 DAY),
        2, 1, 1,
        'Lê Thị Hoa', '0901 888 999', 'user@travelmate.vn',
        2040000, 2040000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'LATA20', 510000, 'PARTNER', 2550000,
        'Áp dụng LATA20 giảm 20% — partner chịu chi phí voucher.',
        DATE_SUB(NOW(), INTERVAL 24 DAY), DATE_SUB(NOW(), INTERVAL 24 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 2040000,
        CONCAT('TXN-LATA-DLX2-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 24 DAY), 'LATA-DLX 3 đêm với LATA20 — hoàn tất');

    -- ─── Booking 18: COMPLETED — Mộc Nhiên Homestay MND-STD (partner2), không voucher ───
    -- Kỳ tháng 03/2026.
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, created_at, updated_at)
    VALUES (
        'BK-MND-STD-0001', 2, 7, 22,
        DATE_SUB(CURDATE(), INTERVAL 22 DAY), DATE_SUB(CURDATE(), INTERVAL 20 DAY),
        2, 0, 1,
        'Phạm Văn Bình', '0933 222 333', 'user@travelmate.vn',
        780000, 780000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'Homestay Mộc Nhiên 2 đêm — hoàn tất.',
        DATE_SUB(NOW(), INTERVAL 24 DAY), DATE_SUB(NOW(), INTERVAL 24 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 780000,
        CONCAT('TXN-MND-STD1-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 24 DAY), 'MND-STD 2 đêm — hoàn tất');

    -- ─── Booking 19: COMPLETED — Furama FDN-DLX (partner1), không voucher ───
    -- Kỳ tháng 03/2026.
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, created_at, updated_at)
    VALUES (
        'BK-FDN-DLX-0002', 2, 9, 28,
        DATE_SUB(CURDATE(), INTERVAL 29 DAY), DATE_SUB(CURDATE(), INTERVAL 27 DAY),
        2, 0, 1,
        'Hoàng Thị Lan', '0966 444 555', 'user@travelmate.vn',
        4800000, 4800000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'Furama Resort 2 đêm — hoàn tất.',
        DATE_SUB(NOW(), INTERVAL 31 DAY), DATE_SUB(NOW(), INTERVAL 31 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 4800000,
        CONCAT('TXN-FDN-DLX2-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 31 DAY), 'FDN-DLX 2 đêm — hoàn tất');

    -- ─── Booking 20: COMPLETED — Vinpearl VNT-DLX (partner2), voucher VNT100K (PARTNER) ───
    -- Kỳ tháng 03/2026. Partner chịu chi phí voucher 100K.
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        voucher_code, discount_amount, voucher_cost_bearer, total_before_discount,
        note, created_at, updated_at)
    VALUES (
        'BK-VNT-DLX-0003', 2, 8, 25,
        DATE_SUB(CURDATE(), INTERVAL 28 DAY), DATE_SUB(CURDATE(), INTERVAL 26 DAY),
        2, 0, 1,
        'Vũ Minh Tuấn', '0977 666 777', 'user@travelmate.vn',
        5500000, 5500000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'VNT100K', 100000, 'PARTNER', 5600000,
        'Áp dụng VNT100K giảm 100K — partner chịu chi phí voucher.',
        DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 5500000,
        CONCAT('TXN-VNT-DLX3-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 30 DAY), 'VNT-DLX 2 đêm với VNT100K — hoàn tất');

    -- ─── Booking 21: COMPLETED — TM Grand TMG-DLX (partner1), không voucher ───
    -- Kỳ tháng 03/2026.
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, created_at, updated_at)
    VALUES (
        'BK-TMG-DLX-HST21', 2, 3, 9,
        DATE_SUB(CURDATE(), INTERVAL 36 DAY), DATE_SUB(CURDATE(), INTERVAL 34 DAY),
        2, 0, 1,
        'Đỗ Quang Hải', '0944 888 999', 'user@travelmate.vn',
        2400000, 2400000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'TM Grand 2 đêm — hoàn tất.',
        DATE_SUB(NOW(), INTERVAL 38 DAY), DATE_SUB(NOW(), INTERVAL 38 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 2400000,
        CONCAT('TXN-TMG-DLX2-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 38 DAY), 'TMG-DLX 2 đêm — hoàn tất');

    -- ─── Booking 22: COMPLETED — Anam Villa ANM-GDN (partner2), không voucher ───
    -- Kỳ tháng 03/2026.
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, created_at, updated_at)
    VALUES (
        'BK-ANM-GDN-0002', 2, 4, 13,
        DATE_SUB(CURDATE(), INTERVAL 35 DAY), DATE_SUB(CURDATE(), INTERVAL 33 DAY),
        2, 0, 1,
        'Ngô Thị Phương', '0911 000 111', 'user@travelmate.vn',
        7000000, 7000000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'Anam Villa 2 đêm — hoàn tất.',
        DATE_SUB(NOW(), INTERVAL 37 DAY), DATE_SUB(NOW(), INTERVAL 37 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 7000000,
        CONCAT('TXN-ANM-GDN2-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 37 DAY), 'ANM-GDN 2 đêm — hoàn tất');

    -- =============================================
    -- REVIEWS BỔ SUNG — bookings 15, 17, 21, 22
    -- =============================================

    -- Booking #15 → acc1 LATA Hotel (10 điểm)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 1, 15, 10,
    'Dùng voucher SUMMER10 rất hời! Phòng sạch sẽ, check-in nhanh, vị trí trung tâm Đà Lạt tiện lợi. Nhân viên lễ tân thân thiện và nhiệt tình hỗ trợ hành lý.',
    DATE_SUB(NOW(), INTERVAL 13 DAY));

    -- Booking #17 → acc1 LATA Hotel (8 điểm, voucher LATA20)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 1, 17, 8,
    'Voucher LATA20 giảm được nhiều, phòng Deluxe rộng có ban công nhìn vườn đẹp. Bữa sáng ổn nhưng chưa đa dạng lắm. Nhìn chung rất đáng tiền, sẽ quay lại.',
    DATE_SUB(NOW(), INTERVAL 19 DAY));

    -- Booking #21 → acc3 TM Grand Hotel (10 điểm)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 3, 21, 10,
    'TravelMate Grand Hotel thật sự xứng đáng 5 sao! Spa tuyệt vời, nhà hàng fine dining ngon, phòng view thung lũng cực đẹp. Dịch vụ butler tận tâm, chắc chắn sẽ quay lại.',
    DATE_SUB(NOW(), INTERVAL 34 DAY));

    -- Booking #22 → acc4 Anam Villa (10 điểm)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 4, 22, 10,
    'The Anam Villa là thiên đường nghỉ dưỡng! Butler phục vụ tận tình, hồ bơi private sát biển, bữa sáng đặt tại phòng tuyệt hảo. Giá xứng đáng với đẳng cấp nhận được.',
    DATE_SUB(NOW(), INTERVAL 33 DAY));

    -- Booking #16 → acc8 Vinpearl Resort (10 điểm)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 8, 16, 10,
    'Vinpearl Resort đẳng cấp! Bãi biển riêng tuyệt đẹp, hồ bơi lớn, phòng view biển thoáng mát. Dịch vụ chuyên nghiệp, bữa sáng buffet phong phú. Sẽ quay lại vào dịp khác!',
    DATE_SUB(NOW(), INTERVAL 12 DAY));

    -- Booking #18 → acc7 Mộc Nhiên Homestay (8 điểm)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 7, 18, 8,
    'Homestay Mộc Nhiên yên tĩnh và thơ mộng, không gian xanh mướt giữa núi đồi Đà Lạt. Chủ nhà rất thân thiện, gợi ý nhiều địa điểm hay. WiFi ổn định hơn lần trước!',
    DATE_SUB(NOW(), INTERVAL 20 DAY));

    -- Booking #19 → acc9 Furama Resort (10 điểm)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 9, 19, 10,
    'Furama Resort Đà Nẵng là kỳ nghỉ tuyệt vời nhất! Phòng rộng có ban công nhìn thẳng ra biển Mỹ Khê. Spa tuyệt vời, nhà hàng phục vụ tận tình. Chắc chắn sẽ quay lại!',
    DATE_SUB(NOW(), INTERVAL 27 DAY));

    -- Booking #20 → acc8 Vinpearl Resort (8 điểm)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 8, 20, 8,
    'Kỳ nghỉ thứ hai tại Vinpearl, lần này dùng voucher VNT100K. Phòng Deluxe tiện nghi đầy đủ. Bãi biển đẹp nhưng khá đông vào cuối tuần. Nhìn chung rất đáng tiền.',
    DATE_SUB(NOW(), INTERVAL 26 DAY));

    -- Cập nhật rating + review_count (ghi đè tất cả về đúng giá trị cuối)
    -- acc1: 3 reviews (7=10, 15=10, 17=8) → avg = 9.3
    UPDATE accommodations SET rating = 9.3, review_count = 3 WHERE id = 1;
    -- acc3 TM Grand: 1 review (21=10) → avg = 10.0
    UPDATE accommodations SET rating = 10.0, review_count = 1 WHERE id = 3;
    -- acc4 Anam Villa: 1 review (22=10) → avg = 10.0
    UPDATE accommodations SET rating = 10.0, review_count = 1 WHERE id = 4;
    -- acc7 Mộc Nhiên: 2 reviews (10=8, 18=8) → avg = 8.0
    UPDATE accommodations SET rating = 8.0, review_count = 2 WHERE id = 7;
    -- acc8 Vinpearl: 2 reviews (16=10, 20=8) → avg = 9.0
    UPDATE accommodations SET rating = 9.0, review_count = 2 WHERE id = 8;
    -- acc9 Furama: 1 review (19=10) → avg = 10.0
    UPDATE accommodations SET rating = 10.0, review_count = 1 WHERE id = 9;

    -- =============================================
    -- DỮ LIỆU KHỞI TẠO — PARTNER SETTLEMENTS THEO THÁNG
    -- Mỗi partner chỉ có 1 settlement cho 1 tháng.
    -- period_start = ngày 01, period_end = ngày cuối tháng.
    -- scheduled_payout_date = ngày 10 của tháng sau kỳ quyết toán.
    -- Công thức: payout = gross - commission - voucher_deduction
    -- =============================================
    INSERT INTO partner_settlements
        (partner_id, period_start, period_end,
        gross_amount, commission_amount, voucher_deduction_amount, payout_amount,
        scheduled_payout_date, settlement_status, settlement_date, note, created_at)
    VALUES
    -- Tháng 02/2026 — PAID
    (8, '2026-02-01', '2026-02-28',
    1600000, 160000, 0, 1440000,
    '2026-03-10', 'PAID', '2026-03-10 09:00:00',
    'Tháng 02/2026: Hoa Lư Riverside Homestay 2 booking (HLR-STD + HLR-DLX), không có voucher.',
    '2026-02-28 18:00:00'),

    -- Tháng 03/2026 — PAID, đã gộp các dòng 7 ngày cũ theo partner
    (3, '2026-03-01', '2026-03-31',
    6640000, 1072500, 510000, 5057500,
    '2026-04-10', 'PAID', '2026-04-10 09:00:00',
    'Tháng 03/2026: LATA-DLX + TM Grand + Tulip; voucher LATA20 do Partner chịu 510.000đ.',
    '2026-03-31 18:00:00'),
    (4, '2026-03-01', '2026-03-31',
    10300000, 1872000, 100000, 8328000,
    '2026-04-10', 'PAID', '2026-04-10 09:05:00',
    'Tháng 03/2026: Furama Resort + Vinpearl Resort; voucher VNT100K do Partner chịu 100.000đ.',
    '2026-03-31 18:05:00'),
    (7, '2026-03-01', '2026-03-31',
    22200000, 3120000, 1050000, 18030000,
    '2026-04-10', 'PAID', '2026-04-10 09:10:00',
    'Tháng 03/2026: The Anam Villa + Ba Na Hills Villa; voucher ANAM15 do Partner chịu 1.050.000đ.',
    '2026-03-31 18:10:00'),
    (8, '2026-03-01', '2026-03-31',
    2540000, 259000, 50000, 2231000,
    '2026-04-10', 'PAID', '2026-04-10 09:15:00',
    'Tháng 03/2026: Mộc Nhiên Homestay; voucher HOALUU50K do Partner chịu 50.000đ.',
    '2026-03-31 18:15:00'),

    -- Tháng 04/2026 — PAID
    (3, '2026-04-01', '2026-04-30',
    1170000, 195000, 0, 975000,
    '2026-05-10', 'PAID', '2026-05-10 09:00:00',
    'Tháng 04/2026: LATA Hotel 1 booking, voucher SUMMER10 do Admin chịu.',
    '2026-04-30 18:00:00'),
    (4, '2026-04-01', '2026-04-30',
    5600000, 1008000, 0, 4592000,
    '2026-05-10', 'PAID', '2026-05-10 09:05:00',
    'Tháng 04/2026: Vinpearl Resort 1 booking, không có voucher.',
    '2026-04-30 18:05:00'),
    (7, '2026-04-01', '2026-04-30',
    17400000, 2610000, 0, 14790000,
    '2026-05-10', 'PAID', '2026-05-10 09:10:00',
    'Tháng 04/2026: The Anam Beachfront Pool Villa 3 đêm, không có voucher.',
    '2026-04-30 18:10:00'),
    (8, '2026-04-01', '2026-04-30',
    1500000, 120000, 0, 1380000,
    '2026-05-10', 'PAID', '2026-05-10 09:15:00',
    'Tháng 04/2026: Hoa Lư Family Room 2 đêm, không có voucher.',
    '2026-04-30 18:15:00'),

    -- Tháng 05/2026 — PENDING, chờ chi trả dự kiến 10/06/2026
    (3, '2026-05-01', '2026-05-31',
    10538000, 1729200, 0, 8808800,
    '2026-06-10', 'PENDING', NULL,
    'Tháng 05/2026 đang chờ: LATA-FAM + LATA-DLX + TMG-PRE + no-show TLP-STD, gộp đúng kỳ tháng.',
    '2026-05-31 18:00:00'),
    (4, '2026-05-01', '2026-05-31',
    17460000, 3142800, 0, 14317200,
    '2026-06-10', 'PENDING', NULL,
    'Tháng 05/2026 đang chờ: VNT-SUI + VNT-DLX cọc online + no-show Furama, gộp đúng kỳ tháng.',
    '2026-05-31 18:05:00'),
    (7, '2026-05-01', '2026-05-31',
    24100000, 2892000, 0, 21208000,
    '2026-06-10', 'PENDING', NULL,
    'Tháng 05/2026 đang chờ: ANM-GDN + Ba Na Hills BNH-BNG + Anam Garden, gộp đúng kỳ tháng.',
    '2026-05-31 18:10:00'),
    (8, '2026-05-01', '2026-05-31',
    3860000, 409200, 0, 3450800,
    '2026-06-10', 'PENDING', NULL,
    'Tháng 05/2026 đang chờ: MND-ATT + HLR-DLX + MND-ATT-0002, gộp đúng kỳ tháng.',
    '2026-05-31 18:15:00');

    -- =============================================
    -- DỮ LIỆU BỔ SUNG — BOOKINGS ACC2, ACC5, ACC6
    -- Thêm COMPLETED bookings từ user2 & user3 để tạo review đa dạng
    -- =============================================

    -- ─── Booking 23: COMPLETED — Tulip Hotel TLP-STD (partner1), user2 Trần Thị Mai ───
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, created_at, updated_at)
    VALUES (
        'BK-TLP-STD-0002', 5, 2, 5,
        DATE_SUB(CURDATE(), INTERVAL 43 DAY), DATE_SUB(CURDATE(), INTERVAL 41 DAY),
        2, 0, 1,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        960000, 960000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'Tulip Hotel Standard 2 đêm — hoàn tất.',
        DATE_SUB(NOW(), INTERVAL 45 DAY), DATE_SUB(NOW(), INTERVAL 45 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 960000,
        CONCAT('TXN-TLP-STD1-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 45 DAY), 'TLP-STD 2 đêm — hoàn tất');

    -- ─── Booking 24: COMPLETED — Tulip Hotel TLP-SUP (partner1), user Nguyễn Văn An ───
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, created_at, updated_at)
    VALUES (
        'BK-TLP-SUP-HIST-24', 2, 2, 6,
        DATE_SUB(CURDATE(), INTERVAL 50 DAY), DATE_SUB(CURDATE(), INTERVAL 48 DAY),
        2, 0, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        1240000, 1240000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'Tulip Hotel Superior 2 đêm — hoàn tất.',
        DATE_SUB(NOW(), INTERVAL 52 DAY), DATE_SUB(NOW(), INTERVAL 52 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 1240000,
        CONCAT('TXN-TLP-SUP1-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 52 DAY), 'TLP-SUP 2 đêm — hoàn tất');

    -- ─── Booking 25: COMPLETED — Ba Na Hills BNH-BNG (partner1), user3 Lê Văn Đức ───
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, created_at, updated_at)
    VALUES (
        'BK-BNH-BNG-0002', 6, 5, 16,
        DATE_SUB(CURDATE(), INTERVAL 57 DAY), DATE_SUB(CURDATE(), INTERVAL 55 DAY),
        2, 0, 1,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        4400000, 4400000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'Ba Na Hills Forest Bungalow 2 đêm — hoàn tất.',
        DATE_SUB(NOW(), INTERVAL 59 DAY), DATE_SUB(NOW(), INTERVAL 59 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 4400000,
        CONCAT('TXN-BNH-BNG2-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 59 DAY), 'BNH-BNG 2 đêm — hoàn tất');

    -- ─── Booking 26: COMPLETED — Ba Na Hills BNH-TWN (partner1), user2 Trần Thị Mai ───
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, created_at, updated_at)
    VALUES (
        'BK-BNH-TWN-0001', 5, 5, 17,
        DATE_SUB(CURDATE(), INTERVAL 64 DAY), DATE_SUB(CURDATE(), INTERVAL 62 DAY),
        3, 0, 1,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        3200000, 3200000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'Ba Na Hills Twin Cabin 2 đêm — hoàn tất.',
        DATE_SUB(NOW(), INTERVAL 66 DAY), DATE_SUB(NOW(), INTERVAL 66 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 3200000,
        CONCAT('TXN-BNH-TWN1-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 66 DAY), 'BNH-TWN 2 đêm — hoàn tất');

    -- ─── Booking 27: COMPLETED — Hoa Lư Homestay HLR-STD (partner1), user Nguyễn Văn An ───
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, created_at, updated_at)
    VALUES (
        'BK-HLR-STD-0001', 2, 6, 19,
        DATE_SUB(CURDATE(), INTERVAL 71 DAY), DATE_SUB(CURDATE(), INTERVAL 69 DAY),
        2, 0, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        640000, 640000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'Hoa Lư Riverside Standard 2 đêm — hoàn tất.',
        DATE_SUB(NOW(), INTERVAL 73 DAY), DATE_SUB(NOW(), INTERVAL 73 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 640000,
        CONCAT('TXN-HLR-STD1-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 73 DAY), 'HLR-STD 2 đêm — hoàn tất');

    -- ─── Booking 28: COMPLETED — Hoa Lư Homestay HLR-DLX (partner1), user3 Lê Văn Đức ───
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, created_at, updated_at)
    VALUES (
        'BK-HLR-DLX-0002', 6, 6, 20,
        DATE_SUB(CURDATE(), INTERVAL 78 DAY), DATE_SUB(CURDATE(), INTERVAL 76 DAY),
        2, 0, 1,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        960000, 960000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'Hoa Lư Riverside Deluxe 2 đêm — hoàn tất.',
        DATE_SUB(NOW(), INTERVAL 80 DAY), DATE_SUB(NOW(), INTERVAL 80 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 960000,
        CONCAT('TXN-HLR-DLX2-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 80 DAY), 'HLR-DLX 2 đêm — hoàn tất');

    -- =============================================
    -- REVIEWS BỔ SUNG — bookings 23–28 (Tulip, Ba Na, Hoa Lư)
    -- =============================================

    -- Booking #23 → acc2 Tulip Hotel (10 điểm, user2 Trần Thị Mai)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (5, 2, 23, 10,
    'Tulip Hotel 2 Dalat tuy 3 sao nhưng chất lượng vượt mong đợi! Phòng Standard sạch sẽ thoải mái, view đồi thông Đà Lạt buổi sáng rất đẹp. Nhân viên thân thiện, check-in nhanh chóng. Giá rất phải chăng cho vị trí trung tâm gần hồ Xuân Hương. Chắc chắn sẽ quay lại!',
    DATE_SUB(NOW(), INTERVAL 39 DAY));

    -- Booking #24 → acc2 Tulip Hotel (8 điểm, user Nguyễn Văn An)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 2, 24, 8,
    'Khách sạn phong cách Châu Âu cổ điển rất duyên dáng. Phòng Superior có ban công nhìn hồ Xuân Hương tuyệt đẹp vào buổi sáng. Bữa sáng buffet ổn, WiFi ổn định. Giá xứng đáng với chất lượng, phù hợp cho cặp đôi.',
    DATE_SUB(NOW(), INTERVAL 46 DAY));

    -- Booking #25 → acc5 Ba Na Hills Forest Villa (10 điểm, user3 Lê Văn Đức)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (6, 5, 25, 10,
    'Trải nghiệm đỉnh cao giữa rừng Bà Nà! Bungalow gỗ tự nhiên ấm áp, sàn kính ngắm rừng về đêm cực kỳ ảo diệu. Không khí trong lành, yên tĩnh tuyệt đối. Gần cáp treo và các điểm tham quan nổi tiếng. Đáng từng đồng tiền bỏ ra!',
    DATE_SUB(NOW(), INTERVAL 53 DAY));

    -- Booking #26 → acc5 Ba Na Hills Forest Villa (8 điểm, user2 Trần Thị Mai)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (5, 5, 26, 8,
    'Villa giữa rừng Bà Nà rất thơ mộng và độc đáo. Sân hiên có bếp BBQ cùng nhóm bạn rất vui vẻ. Phòng hơi nhỏ hơn ảnh nhưng trang thiết bị đầy đủ và sạch sẽ. Nhân viên nhiệt tình, đồ ăn ngon. Sẽ giới thiệu cho bạn bè!',
    DATE_SUB(NOW(), INTERVAL 60 DAY));

    -- Booking #27 → acc6 Hoa Lư Riverside Homestay (10 điểm, user Nguyễn Văn An)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 6, 27, 10,
    'Hoa Lư Riverside Homestay là viên ngọc ẩn của Hội An! Nhà cổ 3 gian mái ngói bên sông Thu Bồn, buổi sáng ăn bánh mì do chủ nhà tự làm ngon tuyệt. Được mượn xe đạp miễn phí đi phố cổ chỉ 5 phút. Chủ nhà hiếu khách và nhiệt tình tư vấn địa điểm!',
    DATE_SUB(NOW(), INTERVAL 67 DAY));

    -- Booking #28 → acc6 Hoa Lư Riverside Homestay (10 điểm, user3 Lê Văn Đức)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (6, 6, 28, 10,
    'Homestay truyền thống đậm chất Hội An! Phòng Deluxe nhà tắm riêng sạch sẽ, cửa sổ nhìn vườn xanh mát. Bữa sáng phở và bánh mì tự làm siêu ngon. Vị trí đi bộ ra phố cổ 5 phút. Một trải nghiệm đáng nhớ khác biệt hoàn toàn với khách sạn thông thường!',
    DATE_SUB(NOW(), INTERVAL 74 DAY));

    -- Cập nhật rating + review_count cho acc2, acc5, acc6
    -- acc2 Tulip Hotel: 2 reviews (23=10, 24=8) → avg = 9.0
    UPDATE accommodations SET rating = 9.0, review_count = 2 WHERE id = 2;
    -- acc5 Ba Na Hills Villa: 2 reviews (25=10, 26=8) → avg = 9.0
    UPDATE accommodations SET rating = 9.0, review_count = 2 WHERE id = 5;
    -- acc6 Hoa Lư Homestay: 2 reviews (27=10, 28=10) → avg = 10.0
    UPDATE accommodations SET rating = 10.0, review_count = 2 WHERE id = 6;

    -- Các booking 23–28 đã được gộp vào block partner_settlements monthly phía trên.
    -- Không insert thêm settlement dạng kỳ 7 ngày để tránh trùng unique key theo partner + tháng.

    -- =============================================
    -- KIỂM TRA SAU KHI CHẠY SQL (cập nhật)
    -- =============================================
    -- Các số lượng tổng thể được kiểm tra ở checklist cuối file sau khi toàn bộ dữ liệu đã được nạp.
    --
    -- Kiểm tra acc2, acc5, acc6 đã có reviews nhất quán:
    -- SELECT id, name, rating, review_count FROM accommodations WHERE id IN (2,5,6);
    -- Kỳ vọng: acc2=9.0/2, acc5=9.0/2, acc6=10.0/2
    --
    -- Kiểm tra reviews đa dạng user:
    -- SELECT r.id, u.name user_name, a.name acc_name, r.rating
    --   FROM reviews r JOIN users u ON r.user_id=u.id JOIN accommodations a ON r.accommodation_id=a.id
    --   ORDER BY r.id;
    --
    -- Kiểm tra bookings có voucher:
    -- SELECT booking_code, voucher_code, discount_amount, voucher_cost_bearer
    --   FROM bookings WHERE voucher_code IS NOT NULL ORDER BY id;
    -- Kỳ vọng: #15 SUMMER10/ADMIN, #17 LATA20/PARTNER, #20 VNT100K/PARTNER
    --
    -- Kiểm tra settlement có voucher_deduction > 0:
    -- SELECT partner_id, period_start, voucher_deduction_amount, payout_amount
    --   FROM partner_settlements WHERE voucher_deduction_amount > 0 ORDER BY id;
    -- Kỳ vọng: partner1 tháng 03 (510K), partner2 tháng 03 (100K)

    -- =============================================
    -- DỮ LIỆU BỔ SUNG — DIRECT BOOKING & MANUAL_BLOCK
    -- Mục đích: phân biệt booking_source trực quan
    --   ONLINE   → qua TravelMate → có commission → tính doanh thu
    --   DIRECT   → partner nhận khách walk-in/điện thoại → KHÔNG commission
    --   MANUAL_BLOCK → partner chặn phòng bảo trì/sự kiện riêng → KHÔNG commission
    -- =============================================

    -- ─── DIRECT Booking 1: TLP-FAM (r7, acc2 Tulip, partner1) — walk-in ────────
    -- Khách thuê trực tiếp tại quầy. Partner đặt lịch vào hệ thống để quản lý phòng.
    -- booking_source=DIRECT, không có payment record (thu tiền mặt tại chỗ)
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, booking_source, block_reason,
        note, created_at, updated_at)
    VALUES (
        'BK-PL-DIRECT-001', 2, 2, 7,
        DATE_ADD(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 4 DAY),
        3, 1, 1,
        'Phạm Văn Khoa', '0909 123 456', NULL,
        2100000, 2100000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'NOT_REQUIRED',
        'PARTNER_CONFIRMED', 'DIRECT', NULL,
        'Khách walk-in đặt trực tiếp tại quầy lễ tân — thanh toán tiền mặt. Không qua TravelMate, không tính commission.',
        NOW(), NOW()
    );
    -- BK-PL-DIRECT-001 thu trực tiếp tại quầy: không tạo payment qua TravelMate

    -- ─── DIRECT Booking 2: TMG-FAM (r11, acc3 TM Grand, partner1) — đặt qua điện thoại ──
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, booking_source, block_reason,
        note, created_at, updated_at)
    VALUES (
        'BK-TMG-FAM-DIRECT-02', 2, 3, 11,
        DATE_ADD(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 8 DAY),
        4, 2, 1,
        'Nguyễn Thanh Bình', '0911 654 321', 'binh.nt@example.com',
        8400000, 8400000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'NOT_REQUIRED',
        'PARTNER_CONFIRMED', 'DIRECT', NULL,
        'Khách đặt qua điện thoại — đã chuyển khoản ngân hàng trực tiếp cho khách sạn. Không qua cổng TravelMate, không tính commission.',
        DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
    );

    -- ─── DIRECT Booking 3: VNT-VIL (r27, acc8 Vinpearl, partner2) — đoàn doanh nghiệp ──
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, booking_source, block_reason,
        note, created_at, updated_at)
    VALUES (
        'BK-VNT-VIL-DIRECT-03', 2, 8, 27,
        DATE_ADD(CURDATE(), INTERVAL 8 DAY), DATE_ADD(CURDATE(), INTERVAL 12 DAY),
        4, 0, 1,
        'Công ty TNHH ABC', '028 1234 5678', 'booking@abc-corp.vn',
        39200000, 39200000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'NOT_REQUIRED',
        'PARTNER_CONFIRMED', 'DIRECT', NULL,
        'Đoàn doanh nghiệp 4 đêm — ký hợp đồng trực tiếp với Vinpearl Resort. Không qua TravelMate, không tính commission.',
        DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
    );
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 27;

    -- ─── MANUAL_BLOCK 1: LATA-SUI (r4 đã FULL, dùng LATA-STD r1 cho tình huống bảo trì) ──────
    -- LATA-SUI đã full nên block thêm phòng LATA-STD để thể hiện bảo trì
    -- NOTE: chỉ block phòng khi available_quantity > 0, dùng room 3 (LATA-FAM) còn trống
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, booking_source, block_reason,
        note, created_at, updated_at)
    VALUES (
        'BK-LATA-FAM-BLOCK-01', 3, 1, 3,
        DATE_ADD(CURDATE(), INTERVAL 15 DAY), DATE_ADD(CURDATE(), INTERVAL 18 DAY),
        0, 0, 1,
        'Partner Sunrise Sapa Lodge', '0933 456 789', 'partner@travelmate.vn',
        0, 0, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'NOT_REQUIRED',
        'PARTNER_CONFIRMED', 'MANUAL_BLOCK', 'Bảo trì định kỳ phòng Gia Đình: kiểm tra điều hoà, sơn lại tường, thay nệm mới.',
        'Phòng chặn để bảo trì nội bộ — không mở bán. Partner tự bỏ chặn sau khi hoàn tất bảo trì.',
        NOW(), NOW()
    );

    -- ─── MANUAL_BLOCK 2: BNH-SUI (r18, acc5 Ba Na Hills, partner1) — sự kiện riêng ──
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, booking_source, block_reason,
        note, created_at, updated_at)
    VALUES (
        'BK-BNH-SUI-BLOCK-02', 3, 5, 18,
        DATE_ADD(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 25 DAY),
        0, 0, 1,
        'Partner Ba Na Hills Villa', '0933 456 789', 'partner@travelmate.vn',
        0, 0, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'NOT_REQUIRED',
        'PARTNER_CONFIRMED', 'MANUAL_BLOCK', 'Dành cho sự kiện riêng của đối tác thương mại — không mở bán trực tuyến giai đoạn này.',
        'Chặn phòng Treetop Suite cho sự kiện nội bộ. Partner sẽ bỏ chặn sau ngày 25.',
        DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
    );

    -- ─── MANUAL_BLOCK 3: FDN-BCH (r29, acc9 Furama, partner1) — nâng cấp phòng ──
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, booking_source, block_reason,
        note, created_at, updated_at)
    VALUES (
        'BK-FDN-BCH-BLOCK-03', 3, 9, 29,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 7 DAY),
        0, 0, 1,
        'Partner Furama Resort Đà Nẵng', '0933 456 789', 'partner@travelmate.vn',
        0, 0, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'NOT_REQUIRED',
        'PARTNER_CONFIRMED', 'MANUAL_BLOCK', 'Nâng cấp nội thất phòng Beachfront Superior: lắp đặt máy chiếu, thay sofa mới, sơn lại phòng tắm.',
        'Chặn phòng 7 ngày để thi công nâng cấp. Dự kiến hoàn thành và mở bán lại.',
        NOW(), NOW()
    );

    -- Cập nhật note về bookings DIRECT & MANUAL_BLOCK để dễ đối chiếu trên màn hình:
    -- SELECT booking_code, booking_source, booking_status, partner_status, block_reason
    --   FROM bookings WHERE booking_source IN ('DIRECT','MANUAL_BLOCK') ORDER BY id;
    -- Kỳ vọng: 3 DIRECT + 3 MANUAL_BLOCK = 6 bookings không qua TravelMate

    -- (end of travelmate_db.sql)

    -- =============================================
    -- MIGRATION: Nếu chạy lại trên máy khác / DB cũ
    -- =============================================
    -- Bước 1: Import toàn bộ file này (drop & recreate) HOẶC chạy lệnh ALTER nếu DB đã có:
    --
    -- ALTER TABLE rooms
    --   ADD COLUMN IF NOT EXISTS room_category            VARCHAR(30)  DEFAULT 'STANDARD',
    --   ADD COLUMN IF NOT EXISTS commission_rate_override  DECIMAL(5,2) DEFAULT NULL;
    --
    -- Bước 2: Backfill dữ liệu (DataInitializer tự làm khi restart app).
    -- Hoặc chạy thủ công:
    --
    -- UPDATE rooms SET room_category = 'VIP',   commission_rate_override = 20.00
    --   WHERE LOWER(room_name) LIKE '%vip%' AND (room_category IS NULL OR room_category = 'STANDARD');
    -- UPDATE rooms SET room_category = 'SUITE',  commission_rate_override = 20.00
    --   WHERE (LOWER(room_name) LIKE '%suite%' OR LOWER(room_name) LIKE '%presidential%')
    --     AND (room_category IS NULL OR room_category = 'STANDARD');
    -- UPDATE rooms SET room_category = 'FAMILY'
    --   WHERE (LOWER(room_name) LIKE '%gia %nh%' OR LOWER(room_name) LIKE '%family%')
    --     AND (room_category IS NULL OR room_category = 'STANDARD');
    -- UPDATE rooms SET room_category = 'DELUXE'
    --   WHERE (LOWER(room_name) LIKE '%deluxe%' OR LOWER(room_name) LIKE '%premium%'
    --          OR LOWER(room_name) LIKE '%superior%')
    --     AND (room_category IS NULL OR room_category = 'STANDARD');
    --
    -- Bước 3: Kiểm tra:
    -- SELECT room_name, room_category, commission_rate_override FROM rooms ORDER BY id;

    -- =============================================
    -- DỮ LIỆU BỔ SUNG — HOÀN CHỈNH CHO 4 PARTNER
    -- Mục đích: trình bày đầy đủ các chức năng cốt lõi
    -- partner1(id=3)=HOTEL | partner2(id=4)=RESORT
    -- partner3(id=7)=VILLA  | partner4(id=8)=HOMESTAY
    -- =============================================

    -- ─── Booking 29: COMPLETED — Anam Villa ANM-BCH (partner3), user2 ───
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, created_at, updated_at)
    VALUES (
        'BK-ANM-BCH-0001', 5, 4, 14,
        DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_SUB(CURDATE(), INTERVAL 17 DAY),
        2, 0, 1,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        17400000, 17400000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'Anam Beachfront Villa 3 đêm — khách đã checkout.',
        DATE_SUB(NOW(), INTERVAL 22 DAY), DATE_SUB(NOW(), INTERVAL 22 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 17400000,
        CONCAT('TXN-ANM-BCH1-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 22 DAY), 'ANM-BCH 3 đêm — hoàn tất');

    -- ─── Booking 30: COMPLETED — Ba Na Hills BNH-SUI (partner3), user3 ───
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        voucher_code, discount_amount, voucher_cost_bearer, total_before_discount,
        note, created_at, updated_at)
    VALUES (
        'BK-BNH-SUI-0001', 6, 5, 18,
        DATE_SUB(CURDATE(), INTERVAL 32 DAY), DATE_SUB(CURDATE(), INTERVAL 30 DAY),
        2, 0, 1,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        6650000, 6650000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'ANAM15', 1050000, 'PARTNER', 7600000,
        'Áp dụng voucher ANAM15 giảm 15% — partner3 chịu chi phí.',
        DATE_SUB(NOW(), INTERVAL 34 DAY), DATE_SUB(NOW(), INTERVAL 34 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 6650000,
        CONCAT('TXN-BNH-SUI1-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 34 DAY), 'BNH-SUI 2 đêm với ANAM15 — hoàn tất');

    -- ─── Booking 31: COMPLETED — Hoa Lư HLR-FAM (partner4), user2 ───
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, created_at, updated_at)
    VALUES (
        'BK-HLR-FAM-0001', 5, 6, 21,
        DATE_SUB(CURDATE(), INTERVAL 25 DAY), DATE_SUB(CURDATE(), INTERVAL 23 DAY),
        3, 1, 1,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        1500000, 1500000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'Hoa Lư Family Room 2 đêm — hoàn tất.',
        DATE_SUB(NOW(), INTERVAL 27 DAY), DATE_SUB(NOW(), INTERVAL 27 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 1500000,
        CONCAT('TXN-HLR-FAM1-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 27 DAY), 'HLR-FAM 2 đêm — hoàn tất');

    -- ─── Booking 32: COMPLETED — Mộc Nhiên MND-FAM (partner4), user ───
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        voucher_code, discount_amount, voucher_cost_bearer, total_before_discount,
        note, created_at, updated_at)
    VALUES (
        'BK-MND-FAM-0001', 2, 7, 24,
        DATE_SUB(CURDATE(), INTERVAL 40 DAY), DATE_SUB(CURDATE(), INTERVAL 38 DAY),
        2, 1, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        1710000, 1710000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'HOALUU50K', 50000, 'PARTNER', 1760000,
        'Áp dụng HOALUU50K giảm 50K — partner4 chịu chi phí.',
        DATE_SUB(NOW(), INTERVAL 42 DAY), DATE_SUB(NOW(), INTERVAL 42 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 1710000,
        CONCAT('TXN-MND-FAM1-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 42 DAY), 'MND-FAM 2 đêm với HOALUU50K — hoàn tất');

    -- ─── Booking 33: CONFIRMED — Anam Villa ANM-GDN (partner3), TravelMate tự giữ phòng ───
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, created_at, updated_at)
    VALUES (
        'BK-ANM-GDN-0003', 6, 4, 13,
        DATE_ADD(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 8 DAY),
        2, 0, 1,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        10500000, 10500000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'TravelMate tự động giữ villa sau khi ghi nhận thanh toán 100%.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 10500000,
        CONCAT('TXN-ANM-GDN3-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'ANM-GDN 3 đêm — TravelMate tự động giữ villa');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 13;

    -- ─── Booking 34: CONFIRMED — Mộc Nhiên MND-ATT (partner4), TravelMate tự giữ phòng ───
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, created_at, updated_at)
    VALUES (
        'BK-MND-ATT-0002', 5, 7, 23,
        DATE_ADD(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 5 DAY),
        2, 0, 1,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        1160000, 1160000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'TravelMate tự động giữ phòng sau khi ghi nhận thanh toán 100%.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 1160000,
        CONCAT('TXN-MND-ATT2-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'MND-ATT 2 đêm — TravelMate tự động giữ phòng');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 23;

    -- ─── Booking 35: CHECKED_IN — Furama FDN-BCH (partner2) ───
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, created_at, updated_at)
    VALUES (
        'BK-FDN-BCH-0001', 6, 9, 29,
        DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 2 DAY),
        2, 0, 1,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        10800000, 10800000, 0,
        'CHECKED_IN', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'Khách đã check-in tại Furama Beachfront.',
        DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 10800000,
        CONCAT('TXN-FDN-BCH1-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 4 DAY), 'FDN-BCH 3 đêm — đã check-in');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 29;

    -- =============================================
    -- KỊCH BẢN: BOOKING TRỰC TIẾP (DIRECT) & CHẶN PHÒNG (MANUAL_BLOCK)
    -- Mục đích: minh hoạ tính năng partner tạo booking tại quầy / chặn phòng bảo trì
    -- =============================================

    -- ─── Direct Booking 1: partner1 tạo booking trực tiếp tại LATA Hotel (LATA-FAM, room_id=3) ─
    -- Khách vãng lai đến thẳng khách sạn, partner tạo booking & check-in ngay hôm nay
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        booking_source, remaining_payment_status,
        note, created_at, updated_at)
    VALUES (
        'BK-LATA-FAM-DIRECT-01', 3, 1, 3,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 DAY),
        2, 1, 1,
        'Trương Minh Khoa', '0922 111 222', NULL,
        2400000, 2400000, 0,
        'CHECKED_IN', 'FULL_PAYMENT', 'NOT_REQUIRED', 'PARTNER_CONFIRMED',
        'DIRECT', 'NOT_REQUIRED',
        'Khách vãng lai nhận phòng trực tiếp tại quầy — đã thanh toán tiền mặt.',
        DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)
    );
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 3;

    -- ─── Direct Booking 2: partner2 tạo booking trực tiếp tại Vinpearl (VNT-DLX, room_id=25) ─
    -- Khách đặt qua điện thoại với resort, partner ghi nhận vào hệ thống
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        booking_source, remaining_payment_status,
        note, created_at, updated_at)
    VALUES (
        'BK-VNT-DLX-DIRECT-01', 4, 8, 25,
        DATE_ADD(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 3 DAY),
        2, 0, 1,
        'Ngô Thanh Hương', '0955 333 444', 'huong.ngo@gmail.com',
        5600000, 5600000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'NOT_REQUIRED', 'PARTNER_CONFIRMED',
        'DIRECT', 'NOT_REQUIRED',
        'Khách đặt qua điện thoại trực tiếp với resort, check-in ngày mai.',
        DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR)
    );
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 25;

    -- ─── Direct Booking 3: partner4 tạo booking trực tiếp tại Mộc Nhiên Homestay (MND-STD, room_id=22) ─
    -- Khách đặt trước qua điện thoại, thanh toán tiền mặt toàn bộ khi nhận phòng.
    -- DIRECT: payment_option=FULL_PAYMENT, payment_status=NOT_REQUIRED, remaining_payment_status=NOT_REQUIRED
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        booking_source, remaining_payment_status,
        note, created_at, updated_at)
    VALUES (
        'BK-MND-STD-DIRECT-01', 8, 7, 22,
        DATE_ADD(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 4 DAY),
        2, 0, 1,
        'Hoàng Thị Lan', '0911 555 666', NULL,
        780000, 780000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'NOT_REQUIRED', 'PARTNER_CONFIRMED',
        'DIRECT', 'NOT_REQUIRED',
        'Khách đặt qua điện thoại, thanh toán tiền mặt toàn bộ khi nhận phòng. Không qua TravelMate, không tính commission.',
        DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)
    );
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 22;

    -- ─── Manual Block 1: partner1 chặn phòng TLP-STD (room_id=5) để bảo trì ─────────────────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        booking_source, block_reason, remaining_payment_status,
        note, created_at, updated_at)
    VALUES (
        'BK-TLP-STD-BLOCK-01', 3, 2, 5,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 3 DAY),
        0, 0, 1,
        'Bảo trì nội bộ', NULL, NULL,
        0, 0, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'NOT_REQUIRED', 'PARTNER_CONFIRMED',
        'MANUAL_BLOCK', 'Bảo trì điều hòa và sơn tường, dự kiến hoàn thành sau 3 ngày', 'NOT_REQUIRED',
        NULL, NOW(), NOW()
    );
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 5;

    -- ─── Manual Block 2: partner3 chặn phòng ANM-GDN (room_id=13) — giữ nội bộ ──────────────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        booking_source, block_reason, remaining_payment_status,
        note, created_at, updated_at)
    VALUES (
        'BK-ANM-GDN-BLOCK-01', 7, 4, 13,
        DATE_ADD(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 4 DAY),
        0, 0, 1,
        'Giữ phòng nội bộ', NULL, NULL,
        0, 0, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'NOT_REQUIRED', 'PARTNER_CONFIRMED',
        'MANUAL_BLOCK', 'Giữ phòng cho đoàn khách VIP của chủ villa, không mở bán 3 ngày cuối tuần', 'NOT_REQUIRED',
        NULL, NOW(), NOW()
    );
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 13;

    -- =============================================
    -- REVIEWS BỔ SUNG — bookings 29, 30, 31, 32
    -- =============================================

    -- Booking #29 → acc4 Anam Villa (10 điểm, user2)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (5, 4, 29, 10,
    'Phòng Beachfront Pool Villa tại Anam là trải nghiệm không thể quên! Hồ bơi tràn ra biển, butler phục vụ 24/7, bữa sáng đặt tại phòng hoàn hảo. Không gian riêng tư tuyệt đối, thích hợp cho tuần trăng mật hoặc nghỉ dưỡng cao cấp.',
    DATE_SUB(NOW(), INTERVAL 15 DAY));

    -- Booking #30 → acc5 Ba Na Hills (8 điểm, user3)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (6, 5, 30, 8,
    'Treetop Suite Bà Nà quả thật độc đáo — ban công 360° nhìn toàn rừng thông cực ảo. Dùng voucher ANAM15 được giảm tốt. Bồn tắm jacuzzi ngoài trời về đêm tuyệt vời. Chỉ hơi tiếc dịch vụ ăn uống tại chỗ còn ít lựa chọn.',
    DATE_SUB(NOW(), INTERVAL 28 DAY));

    -- Booking #31 → acc6 Hoa Lư (10 điểm, user2)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (5, 6, 31, 10,
    'Phòng gia đình nhà Hoa Lư cực kỳ thoải mái cho cả nhà! Ban công nhìn sông Thu Bồn thơ mộng, trẻ con rất thích. Chủ nhà nhiệt tình dẫn đi phố cổ và chỉ hàng ăn ngon. Bánh mì tự làm buổi sáng ngon nhất Hội An!',
    DATE_SUB(NOW(), INTERVAL 21 DAY));

    -- Booking #32 → acc7 Mộc Nhiên (10 điểm, user)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 7, 32, 10,
    'Phòng Gia Đình Mộc Nhiên có sân thượng nhìn vườn dã quỳ cực lãng mạn! Dùng voucher HOALUU50K tiết kiệm được 50K. Lò sưởi củi buổi tối ấm áp, không khí Đà Lạt trong lành, bữa sáng thơm ngon. Sẽ quay lại mùa dã quỳ nở!',
    DATE_SUB(NOW(), INTERVAL 36 DAY));

    -- Cập nhật rating + review_count (tổng hợp cuối cùng)
    -- acc4 Anam Villa: reviews 22(10)+29(10) → avg = 10.0
    UPDATE accommodations SET rating = 10.0, review_count = 2 WHERE id = 4;
    -- acc5 Ba Na Hills: reviews 25(10)+26(8)+30(8) → avg = 8.7
    UPDATE accommodations SET rating = 8.7, review_count = 3 WHERE id = 5;
    -- acc6 Hoa Lư: reviews 27(10)+28(10)+31(10) → avg = 10.0
    UPDATE accommodations SET rating = 10.0, review_count = 3 WHERE id = 6;
    -- acc7 Mộc Nhiên: reviews 10(8)+18(8)+32(10) → avg = 8.7
    UPDATE accommodations SET rating = 8.7, review_count = 3 WHERE id = 7;

    -- Các booking #29–#32 đã được gộp vào settlement monthly phía trên.
    -- Không insert settlement bổ sung dạng kỳ 7 ngày để tránh trùng kỳ tháng.

    -- =============================================
    -- PARTNER WALLET — ví quyết toán + lịch sử + rút tiền
    -- Dữ liệu ở block này cũng phục vụ export Excel đối soát:
    --   /admin/settlements/{id}/export-excel và /admin/withdrawals/export-excel
    -- =============================================
    UPDATE partner_settlements
    SET scheduled_payout_date = DATE_ADD(LAST_DAY(period_end), INTERVAL 10 DAY)
    WHERE id > 0 AND scheduled_payout_date IS NULL;

    INSERT INTO partner_wallets
        (partner_id, available_balance, pending_withdrawal_amount,
         total_earned_amount, total_withdrawn_amount, updated_at)
    SELECT
        u.id,
        COALESCE(SUM(CASE WHEN ps.settlement_status = 'PAID' THEN ps.payout_amount ELSE 0 END), 0),
        0,
        COALESCE(SUM(CASE WHEN ps.settlement_status = 'PAID' THEN ps.payout_amount ELSE 0 END), 0),
        0,
        NOW()
    FROM users u
    LEFT JOIN partner_settlements ps ON ps.partner_id = u.id
    WHERE u.role = 'PARTNER'
    GROUP BY u.id;

    -- Partner chưa cấu hình ngân hàng vẫn có số dư để kiểm tra chặn rút tiền đúng nghiệp vụ.
    UPDATE partner_wallets pw
    JOIN users u ON u.id = pw.partner_id
    SET pw.available_balance = 3200000,
        pw.total_earned_amount = 3200000,
        pw.updated_at = NOW()
    WHERE u.email = 'no-bank@travelmate.vn';

    INSERT INTO partner_wallet_transactions
        (partner_id, settlement_id, withdrawal_request_id, transaction_code,
         transaction_type, direction, amount, balance_before, balance_after,
         description, created_at, created_by_admin_id)
    SELECT u.id, NULL, NULL, 'STL-NOBANK-OPENING-001',
           'SETTLEMENT_CREDIT', 'IN', 3200000, 0, 3200000,
           'Cộng số dư quyết toán ban đầu cho tài khoản đối tác chưa cấu hình ngân hàng',
           DATE_SUB(NOW(), INTERVAL 3 DAY), 1
    FROM users u
    WHERE u.email = 'no-bank@travelmate.vn'
      AND NOT EXISTS (
          SELECT 1 FROM partner_wallet_transactions tx
          WHERE tx.transaction_code = 'STL-NOBANK-OPENING-001'
      );

    INSERT INTO partner_withdrawal_requests
        (partner_id, request_code, amount,
         bank_name, bank_account_number, bank_account_holder, bank_branch,
         withdrawal_status, requested_at, processed_at, processed_by_admin_id, admin_note)
    VALUES
    (3, 'WD-OPS-PENDING-001', 1000000,
     'Vietcombank', '0123456789', 'NGUYEN VAN PARTNER', 'CN Đà Lạt',
     'PENDING', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL, NULL),
    (4, 'WD-OPS-PAID-001', 2500000,
     'Techcombank', '0987654321', 'TRAN THI RESORT', 'CN Nha Trang',
     'PAID', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), 1,
     'Admin ghi nhận đã xử lý chuyển khoản ngoài hệ thống, mã GD TCB-2500.'),
    (7, 'WD-OPS-REJECT-001', 1500000,
     'BIDV', '1122334455', 'LE VAN VILLA', 'CN Đà Nẵng',
     'REJECTED', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 1,
     'Từ chối do Partner cần kiểm tra lại thông tin tài khoản.');

    UPDATE partner_wallets
    SET pending_withdrawal_amount = pending_withdrawal_amount + 1000000,
        available_balance = GREATEST(available_balance - 1000000, 0),
        updated_at = NOW()
    WHERE id > 0 AND partner_id = 3;

    UPDATE partner_wallets
    SET total_withdrawn_amount = total_withdrawn_amount + 2500000,
        available_balance = GREATEST(available_balance - 2500000, 0),
        updated_at = NOW()
    WHERE id > 0 AND partner_id = 4;

    INSERT INTO partner_wallet_transactions
        (partner_id, settlement_id, withdrawal_request_id, transaction_code,
         transaction_type, direction, amount, balance_before, balance_after,
         description, created_at, created_by_admin_id)
    SELECT
        ps.partner_id,
        ps.id,
        NULL,
        CONCAT('STL-', LPAD(ps.id, 6, '0')),
        'SETTLEMENT_CREDIT',
        'IN',
        ps.payout_amount,
        ps.balance_before_calc,
        ps.balance_after_calc,
        CONCAT('Cộng tiền quyết toán kỳ ', DATE_FORMAT(ps.period_start, '%d/%m/%Y'), ' - ', DATE_FORMAT(ps.period_end, '%d/%m/%Y')),
        COALESCE(ps.settlement_date, ps.created_at, NOW()),
        1
    FROM (
        SELECT
            paid.*,
            COALESCE(
                SUM(COALESCE(paid.payout_amount, 0)) OVER (
                    PARTITION BY paid.partner_id
                    ORDER BY COALESCE(paid.settlement_date, paid.created_at, NOW()), paid.id
                    ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
                ),
                0
            ) AS balance_before_calc,
            SUM(COALESCE(paid.payout_amount, 0)) OVER (
                PARTITION BY paid.partner_id
                ORDER BY COALESCE(paid.settlement_date, paid.created_at, NOW()), paid.id
                ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
            ) AS balance_after_calc
        FROM partner_settlements paid
        WHERE paid.settlement_status = 'PAID'
    ) ps;

    INSERT INTO partner_wallet_transactions
        (partner_id, settlement_id, withdrawal_request_id, transaction_code,
         transaction_type, direction, amount, balance_before, balance_after,
         description, created_at, created_by_admin_id)
    SELECT
        wr.partner_id,
        NULL,
        wr.id,
        CONCAT('WDREQ-', LPAD(wr.id, 6, '0')),
        'WITHDRAWAL_REQUEST',
        'OUT',
        wr.amount,
        CASE
            WHEN wr.withdrawal_status = 'REJECTED' THEN pw.available_balance
            ELSE pw.available_balance + wr.amount
        END,
        CASE
            WHEN wr.withdrawal_status = 'REJECTED' THEN GREATEST(pw.available_balance - wr.amount, 0)
            ELSE pw.available_balance
        END,
        CONCAT('Partner gửi yêu cầu rút tiền về tài khoản ', CONCAT('****', RIGHT(wr.bank_account_number, 4))),
        wr.requested_at,
        NULL
    FROM partner_withdrawal_requests wr
    JOIN partner_wallets pw ON pw.partner_id = wr.partner_id
    WHERE wr.request_code IN ('WD-OPS-PENDING-001', 'WD-OPS-PAID-001', 'WD-OPS-REJECT-001');

    INSERT INTO partner_wallet_transactions
        (partner_id, settlement_id, withdrawal_request_id, transaction_code,
         transaction_type, direction, amount, balance_before, balance_after,
         description, created_at, created_by_admin_id)
    SELECT
        wr.partner_id,
        NULL,
        wr.id,
        CONCAT('WDPAID-', LPAD(wr.id, 6, '0')),
        'WITHDRAWAL_PAID',
        'INFO',
        wr.amount,
        pw.available_balance,
        pw.available_balance,
        'Admin ghi nhận đã xử lý chuyển khoản ngoài hệ thống',
        wr.processed_at,
        1
    FROM partner_withdrawal_requests wr
    JOIN partner_wallets pw ON pw.partner_id = wr.partner_id
    WHERE wr.request_code = 'WD-OPS-PAID-001';

    INSERT INTO partner_wallet_transactions
        (partner_id, settlement_id, withdrawal_request_id, transaction_code,
         transaction_type, direction, amount, balance_before, balance_after,
         description, created_at, created_by_admin_id)
    SELECT
        wr.partner_id,
        NULL,
        wr.id,
        CONCAT('WDREJ-', LPAD(wr.id, 6, '0')),
        'WITHDRAWAL_REJECTED',
        'IN',
        wr.amount,
        GREATEST(pw.available_balance - wr.amount, 0),
        pw.available_balance,
        'Hoàn lại số dư vì yêu cầu rút tiền bị từ chối',
        wr.processed_at,
        1
    FROM partner_withdrawal_requests wr
    JOIN partner_wallets pw ON pw.partner_id = wr.partner_id
    WHERE wr.request_code = 'WD-OPS-REJECT-001';

    -- =============================================
    -- TIỆN NGHI PHÒNG (AMENITIES) — DỮ LIỆU KHỞI TẠO
    -- 32 tiện nghi phân 5 nhóm
    -- =============================================
    INSERT INTO amenities (name, icon, category) VALUES
    -- Nhóm 1: Tiện ích chung
    ('WiFi miễn phí',      '📶', 'Tiện ích chung'),
    ('Hồ bơi chung',       '🏊', 'Tiện ích chung'),
    ('Bãi đỗ xe',          '🚗', 'Tiện ích chung'),
    ('Nhà hàng',           '🍽️', 'Tiện ích chung'),
    ('Spa / Massage',      '💆', 'Tiện ích chung'),
    ('Phòng gym',          '💪', 'Tiện ích chung'),
    ('Bar / Café',         '☕', 'Tiện ích chung'),
    ('Thang máy',          '🛗', 'Tiện ích chung'),
    -- Nhóm 2: Phòng ngủ
    ('Máy lạnh',           '❄️', 'Phòng ngủ'),
    ('TV màn hình phẳng',  '📺', 'Phòng ngủ'),
    ('Tủ lạnh mini',       '🧊', 'Phòng ngủ'),
    ('Minibar',            '🍾', 'Phòng ngủ'),
    ('Bàn làm việc',       '🖥️', 'Phòng ngủ'),
    ('Két an toàn',        '🔐', 'Phòng ngủ'),
    ('Ổ cắm quốc tế',      '🔌', 'Phòng ngủ'),
    -- Nhóm 3: Phòng tắm
    ('Phòng tắm riêng',    '🚿', 'Phòng tắm'),
    ('Bồn tắm nằm',        '🛁', 'Phòng tắm'),
    ('Vòi sen đứng',       '🚿', 'Phòng tắm'),
    ('Máy sấy tóc',        '💨', 'Phòng tắm'),
    ('Đồ dùng cá nhân',    '🧴', 'Phòng tắm'),
    -- Nhóm 4: Bếp & Ăn uống
    ('Bếp riêng',          '👨‍🍳', 'Bếp & Ăn uống'),
    ('Tủ lạnh đầy đủ',     '🧊', 'Bếp & Ăn uống'),
    ('Máy pha cà phê',     '☕', 'Bếp & Ăn uống'),
    ('Lò vi sóng',         '📡', 'Bếp & Ăn uống'),
    ('Ấm đun nước',        '🫖', 'Bếp & Ăn uống'),
    ('Bữa sáng miễn phí',  '🍳', 'Bếp & Ăn uống'),
    -- Nhóm 5: Đặc biệt
    ('Ban công riêng',     '🌅', 'Đặc biệt'),
    ('View biển',          '🌊', 'Đặc biệt'),
    ('View núi / đồi',     '⛰️', 'Đặc biệt'),
    ('Hồ bơi riêng',       '🏊', 'Đặc biệt'),
    ('BBQ / Bếp nướng',    '🔥', 'Đặc biệt'),
    ('Sân vườn riêng',     '🌿', 'Đặc biệt');

    -- =============================================
    -- ROOM_AMENITIES — DỮ LIỆU KHỞI TẠO
    -- Dùng subquery theo room_code và amenity name để không phụ thuộc vào auto-increment ID
    -- =============================================

    -- ── LATA-STD: Tiêu chuẩn ──────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'LATA-STD' AND a.name IN (
        'WiFi miễn phí', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
        'Tủ lạnh mini', 'Phòng tắm riêng', 'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bàn làm việc');

    -- ── LATA-DLX: Deluxe ──────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'LATA-DLX' AND a.name IN (
        'WiFi miễn phí', 'Nhà hàng', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
        'Minibar', 'Phòng tắm riêng', 'Bồn tắm nằm', 'Máy sấy tóc',
        'Bữa sáng miễn phí', 'Ban công riêng', 'Bàn làm việc');

    -- ── LATA-FAM: Gia đình ────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'LATA-FAM' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
        'Tủ lạnh mini', 'Phòng tắm riêng', 'Bồn tắm nằm', 'Máy sấy tóc', 'Đồ dùng cá nhân');

    -- ── LATA-SUI: Suite ───────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'LATA-SUI' AND a.name IN (
        'WiFi miễn phí', 'Nhà hàng', 'Spa / Massage', 'Thang máy', 'Máy lạnh',
        'TV màn hình phẳng', 'Minibar', 'Két an toàn', 'Ổ cắm quốc tế',
        'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
        'Bữa sáng miễn phí', 'Ban công riêng', 'Bàn làm việc');

    -- ── TLP-STD ───────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'TLP-STD' AND a.name IN (
        'WiFi miễn phí', 'Máy lạnh', 'TV màn hình phẳng',
        'Phòng tắm riêng', 'Máy sấy tóc', 'Đồ dùng cá nhân');

    -- ── TLP-SUP ───────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'TLP-SUP' AND a.name IN (
        'WiFi miễn phí', 'Máy lạnh', 'TV màn hình phẳng', 'Tủ lạnh mini',
        'Phòng tắm riêng', 'Máy sấy tóc', 'Bữa sáng miễn phí',
        'Ban công riêng', 'View núi / đồi');

    -- ── TLP-FAM ───────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'TLP-FAM' AND a.name IN (
        'WiFi miễn phí', 'Máy lạnh', 'TV màn hình phẳng',
        'Tủ lạnh mini', 'Phòng tắm riêng', 'Máy sấy tóc', 'Đồ dùng cá nhân');

    -- ── TLP-VIP ───────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'TLP-VIP' AND a.name IN (
        'WiFi miễn phí', 'Spa / Massage', 'Máy lạnh', 'TV màn hình phẳng',
        'Minibar', 'Két an toàn', 'Phòng tắm riêng', 'Bồn tắm nằm',
        'Máy sấy tóc', 'Ban công riêng', 'View núi / đồi', 'Bàn làm việc');

    -- ── TMG-DLX ───────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'TMG-DLX' AND a.name IN (
        'WiFi miễn phí', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
        'Minibar', 'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng',
        'Bàn làm việc', 'Ổ cắm quốc tế');

    -- ── TMG-PRE ───────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'TMG-PRE' AND a.name IN (
        'WiFi miễn phí', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
        'Minibar', 'Két an toàn', 'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng',
        'Máy sấy tóc', 'Bữa sáng miễn phí', 'Ban công riêng', 'View núi / đồi', 'Bàn làm việc');

    -- ── TMG-FAM ───────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'TMG-FAM' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
        'Tủ lạnh đầy đủ', 'Bếp riêng', 'Phòng tắm riêng', 'Bồn tắm nằm', 'Máy pha cà phê');

    -- ── TMG-PRE2: Presidential Suite ──────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'TMG-PRE2' AND a.name IN (
        'WiFi miễn phí', 'Nhà hàng', 'Spa / Massage', 'Bar / Café', 'Thang máy',
        'Máy lạnh', 'TV màn hình phẳng', 'Minibar', 'Két an toàn', 'Ổ cắm quốc tế',
        'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
        'Bữa sáng miễn phí', 'Ban công riêng', 'View núi / đồi', 'Bàn làm việc');

    -- ── ANM-GDN: Garden Pool Villa ────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'ANM-GDN' AND a.name IN (
        'WiFi miễn phí', 'Máy lạnh', 'TV màn hình phẳng', 'Phòng tắm riêng',
        'Bồn tắm nằm', 'Bữa sáng miễn phí', 'Ban công riêng',
        'Hồ bơi riêng', 'Sân vườn riêng', 'Bếp riêng', 'Máy pha cà phê');

    -- ── ANM-BCH: Beachfront Pool Villa ────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'ANM-BCH' AND a.name IN (
        'WiFi miễn phí', 'Máy lạnh', 'TV màn hình phẳng', 'Minibar',
        'Phòng tắm riêng', 'Bồn tắm nằm', 'Bữa sáng miễn phí',
        'Ban công riêng', 'View biển', 'Hồ bơi riêng', 'Sân vườn riêng',
        'Bếp riêng', 'Tủ lạnh đầy đủ');

    -- ── ANM-FAM: Family Grand Villa ───────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'ANM-FAM' AND a.name IN (
        'WiFi miễn phí', 'Máy lạnh', 'TV màn hình phẳng', 'Phòng tắm riêng',
        'Bồn tắm nằm', 'Ban công riêng', 'Hồ bơi riêng', 'BBQ / Bếp nướng',
        'Sân vườn riêng', 'Bếp riêng', 'Tủ lạnh đầy đủ', 'Lò vi sóng');

    -- ── BNH-BNG: Forest Bungalow ──────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'BNH-BNG' AND a.name IN (
        'WiFi miễn phí', 'Máy lạnh', 'TV màn hình phẳng', 'Phòng tắm riêng',
        'Bồn tắm nằm', 'Ban công riêng', 'View núi / đồi', 'Sân vườn riêng');

    -- ── BNH-TWN: Twin Cabin ───────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'BNH-TWN' AND a.name IN (
        'WiFi miễn phí', 'Máy lạnh', 'TV màn hình phẳng', 'Phòng tắm riêng',
        'Máy sấy tóc', 'Bữa sáng miễn phí', 'Ban công riêng',
        'View núi / đồi', 'BBQ / Bếp nướng');

    -- ── BNH-SUI: Treetop Suite ────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'BNH-SUI' AND a.name IN (
        'WiFi miễn phí', 'Máy lạnh', 'TV màn hình phẳng', 'Minibar',
        'Phòng tắm riêng', 'Bồn tắm nằm', 'Ban công riêng',
        'View núi / đồi', 'Sân vườn riêng');

    -- ── HLR-STD: Phòng Truyền Thống ──────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'HLR-STD' AND a.name IN (
        'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng',
        'Đồ dùng cá nhân', 'Bữa sáng miễn phí', 'Ấm đun nước');

    -- ── HLR-DLX: Phòng Deluxe Riêng ──────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'HLR-DLX' AND a.name IN (
        'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng',
        'Phòng tắm riêng', 'Máy sấy tóc', 'Đồ dùng cá nhân',
        'Bữa sáng miễn phí', 'Ban công riêng');

    -- ── HLR-FAM: Phòng Gia Đình Ven Sông ─────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'HLR-FAM' AND a.name IN (
        'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng',
        'Phòng tắm riêng', 'Bếp riêng', 'Bữa sáng miễn phí', 'Ban công riêng');

    -- ── MND-STD: Phòng Nhà Gỗ Tiêu Chuẩn ────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'MND-STD' AND a.name IN (
        'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng',
        'Đồ dùng cá nhân', 'Bữa sáng miễn phí', 'Ấm đun nước');

    -- ── MND-ATT: Phòng Áp Mái View Đồi ──────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'MND-ATT' AND a.name IN (
        'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng',
        'Phòng tắm riêng', 'Máy sấy tóc', 'Đồ dùng cá nhân',
        'Bữa sáng miễn phí', 'View núi / đồi', 'Ban công riêng');

    -- ── MND-FAM: Phòng Gia Đình Vườn Hoa ────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'MND-FAM' AND a.name IN (
        'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng',
        'Phòng tắm riêng', 'Bếp riêng', 'Bữa sáng miễn phí',
        'Sân vườn riêng', 'Ban công riêng');

    -- ── VNT-DLX: Deluxe Ocean View ────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'VNT-DLX' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Nhà hàng', 'Spa / Massage', 'Thang máy',
        'Máy lạnh', 'TV màn hình phẳng', 'Minibar', 'Phòng tắm riêng',
        'Bồn tắm nằm', 'Vòi sen đứng', 'Bữa sáng miễn phí', 'View biển');

    -- ── VNT-SUI: Junior Suite Beachfront ─────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'VNT-SUI' AND a.name IN (
        'WiFi miễn phí', 'Nhà hàng', 'Spa / Massage', 'Thang máy',
        'Máy lạnh', 'TV màn hình phẳng', 'Minibar', 'Két an toàn',
        'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
        'Bữa sáng miễn phí', 'Ban công riêng', 'View biển');

    -- ── VNT-VIL: Pool Villa ───────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'VNT-VIL' AND a.name IN (
        'WiFi miễn phí', 'Nhà hàng', 'Máy lạnh', 'TV màn hình phẳng',
        'Tủ lạnh đầy đủ', 'Bếp riêng', 'Lò vi sóng', 'Phòng tắm riêng',
        'Bồn tắm nằm', 'Bữa sáng miễn phí', 'Ban công riêng',
        'View biển', 'Hồ bơi riêng', 'Sân vườn riêng');

    -- ── FDN-DLX: Deluxe Garden View ──────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'FDN-DLX' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Nhà hàng', 'Spa / Massage', 'Thang máy',
        'Máy lạnh', 'TV màn hình phẳng', 'Minibar', 'Phòng tắm riêng',
        'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc', 'Bữa sáng miễn phí');

    -- ── FDN-BCH: Beachfront Superior ─────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'FDN-BCH' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Nhà hàng', 'Spa / Massage', 'Thang máy',
        'Máy lạnh', 'TV màn hình phẳng', 'Minibar', 'Két an toàn',
        'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
        'Bữa sáng miễn phí', 'Ban công riêng', 'View biển');

    -- ── FDN-FAM: Family Suite ─────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'FDN-FAM' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Nhà hàng', 'Spa / Massage', 'Thang máy',
        'Máy lạnh', 'TV màn hình phẳng', 'Tủ lạnh đầy đủ', 'Bếp riêng', 'Lò vi sóng',
        'Phòng tắm riêng', 'Bồn tắm nằm', 'Bữa sáng miễn phí',
        'Ban công riêng', 'View biển', 'Sân vườn riêng');

    -- ── PHV-DLX ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'PHV-DLX' AND a.name IN (
        'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng', 'Bếp riêng',
        'Tủ lạnh đầy đủ', 'Lò vi sóng', 'Ấm đun nước', 'Bồn tắm nằm', 'Máy sấy tóc',
        'Ban công riêng', 'View núi / đồi', 'BBQ / Bếp nướng', 'Sân vườn riêng');

    -- ── PHV-FAM ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'PHV-FAM' AND a.name IN (
        'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng', 'Bếp riêng',
        'Tủ lạnh đầy đủ', 'Lò vi sóng', 'Ấm đun nước', 'Bồn tắm nằm', 'Máy sấy tóc',
        'Ban công riêng', 'View núi / đồi', 'BBQ / Bếp nướng', 'Sân vườn riêng');

    -- ── SBV-SEA ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'SBV-SEA' AND a.name IN (
        'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng', 'Bếp riêng',
        'Tủ lạnh đầy đủ', 'Lò vi sóng', 'Bồn tắm nằm', 'Máy sấy tóc', 'Ban công riêng',
        'View biển', 'BBQ / Bếp nướng', 'Sân vườn riêng');

    -- ── SBV-POOL ─────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'SBV-POOL' AND a.name IN (
        'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng', 'Bếp riêng',
        'Tủ lạnh đầy đủ', 'Bồn tắm nằm', 'Máy sấy tóc', 'Ban công riêng', 'View biển',
        'Hồ bơi riêng', 'BBQ / Bếp nướng', 'Sân vườn riêng');

    -- ── TCG-STD ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'TCG-STD' AND a.name IN (
        'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng', 'Tủ lạnh mini',
        'Phòng tắm riêng', 'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bữa sáng miễn phí',
        'Sân vườn riêng');

    -- ── TCG-FAM ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'TCG-FAM' AND a.name IN (
        'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng', 'Tủ lạnh mini',
        'Phòng tắm riêng', 'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bữa sáng miễn phí',
        'Ban công riêng', 'View núi / đồi', 'Sân vườn riêng');

    -- ── SVH-STD ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'SVH-STD' AND a.name IN (
        'WiFi miễn phí', 'Bãi đỗ xe', 'Phòng tắm riêng', 'Máy sấy tóc', 'Đồ dùng cá nhân',
        'Bữa sáng miễn phí', 'View núi / đồi');

    -- ── SVH-DLX ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'SVH-DLX' AND a.name IN (
        'WiFi miễn phí', 'Bãi đỗ xe', 'Máy lạnh', 'TV màn hình phẳng', 'Tủ lạnh mini',
        'Phòng tắm riêng', 'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bữa sáng miễn phí',
        'Ban công riêng', 'View núi / đồi');

    -- ── LBR-DLX ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'LBR-DLX' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Phòng gym', 'Bar / Café', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
        'Minibar', 'Bàn làm việc', 'Két an toàn', 'Phòng tắm riêng', 'Bồn tắm nằm',
        'Vòi sen đứng', 'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bữa sáng miễn phí',
        'Ban công riêng', 'View biển');

    -- ── LBR-SUI ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'LBR-SUI' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Phòng gym', 'Bar / Café', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
        'Minibar', 'Bàn làm việc', 'Két an toàn', 'Ổ cắm quốc tế', 'Phòng tắm riêng',
        'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc', 'Đồ dùng cá nhân',
        'Bữa sáng miễn phí', 'Ban công riêng', 'View biển');

    -- ── ICN-STD ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'ICN-STD' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Phòng gym', 'Bar / Café', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
        'Tủ lạnh mini', 'Minibar', 'Bàn làm việc', 'Két an toàn', 'Ổ cắm quốc tế',
        'Phòng tắm riêng', 'Vòi sen đứng', 'Máy sấy tóc', 'Đồ dùng cá nhân',
        'Bữa sáng miễn phí');

    -- ── ICN-DLX ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'ICN-DLX' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Phòng gym', 'Bar / Café', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
        'Minibar', 'Bàn làm việc', 'Két an toàn', 'Ổ cắm quốc tế',
        'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
        'Đồ dùng cá nhân', 'Bữa sáng miễn phí', 'Ban công riêng', 'View biển');

    -- ── ICN-SUI ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'ICN-SUI' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Phòng gym', 'Bar / Café', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
        'Minibar', 'Bàn làm việc', 'Két an toàn', 'Ổ cắm quốc tế',
        'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
        'Đồ dùng cá nhân', 'Máy pha cà phê', 'Bữa sáng miễn phí',
        'Ban công riêng', 'View biển');

    -- ── NVD-STD ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'NVD-STD' AND a.name IN (
        'WiFi miễn phí', 'Bãi đỗ xe', 'Nhà hàng', 'Phòng gym', 'Bar / Café',
        'Thang máy', 'Máy lạnh', 'TV màn hình phẳng', 'Tủ lạnh mini',
        'Bàn làm việc', 'Két an toàn', 'Phòng tắm riêng', 'Vòi sen đứng',
        'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bữa sáng miễn phí');

    -- ── NVD-DLX ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'NVD-DLX' AND a.name IN (
        'WiFi miễn phí', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage', 'Phòng gym',
        'Bar / Café', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
        'Tủ lạnh mini', 'Minibar', 'Bàn làm việc', 'Két an toàn', 'Ổ cắm quốc tế',
        'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
        'Đồ dùng cá nhân', 'Bữa sáng miễn phí');

    -- ── NVD-FAM ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'NVD-FAM' AND a.name IN (
        'WiFi miễn phí', 'Bãi đỗ xe', 'Nhà hàng', 'Phòng gym', 'Thang máy',
        'Máy lạnh', 'TV màn hình phẳng', 'Tủ lạnh mini', 'Bàn làm việc',
        'Két an toàn', 'Ổ cắm quốc tế', 'Phòng tắm riêng', 'Bồn tắm nằm',
        'Vòi sen đứng', 'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bữa sáng miễn phí');

    -- ── LSH-STD ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'LSH-STD' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Bar / Café', 'Máy lạnh', 'TV màn hình phẳng', 'Tủ lạnh mini',
        'Bàn làm việc', 'Phòng tắm riêng', 'Vòi sen đứng', 'Máy sấy tóc',
        'Đồ dùng cá nhân', 'Bữa sáng miễn phí');

    -- ── LSH-DLX ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'LSH-DLX' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Bar / Café', 'Máy lạnh', 'TV màn hình phẳng', 'Tủ lạnh mini',
        'Minibar', 'Bàn làm việc', 'Két an toàn', 'Phòng tắm riêng',
        'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc', 'Đồ dùng cá nhân',
        'Bữa sáng miễn phí', 'Ban công riêng');

    -- ── LSH-SUI ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'LSH-SUI' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Bar / Café', 'Máy lạnh', 'TV màn hình phẳng', 'Minibar', 'Bàn làm việc',
        'Két an toàn', 'Ổ cắm quốc tế', 'Phòng tắm riêng', 'Bồn tắm nằm',
        'Vòi sen đứng', 'Máy sấy tóc', 'Đồ dùng cá nhân', 'Máy pha cà phê',
        'Bữa sáng miễn phí', 'Ban công riêng');

    -- ── SLM-PRE ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'SLM-PRE' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Phòng gym', 'Bar / Café', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
        'Minibar', 'Bàn làm việc', 'Két an toàn', 'Ổ cắm quốc tế',
        'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
        'Đồ dùng cá nhân', 'Bữa sáng miễn phí');

    -- ── SLM-GRA ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'SLM-GRA' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Phòng gym', 'Bar / Café', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
        'Minibar', 'Bàn làm việc', 'Két an toàn', 'Ổ cắm quốc tế',
        'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
        'Đồ dùng cá nhân', 'Máy pha cà phê', 'Bữa sáng miễn phí', 'Ban công riêng');

    -- ── SLM-FAM ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'SLM-FAM' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Phòng gym', 'Bar / Café', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
        'Tủ lạnh mini', 'Minibar', 'Bàn làm việc', 'Két an toàn',
        'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
        'Đồ dùng cá nhân', 'Bữa sáng miễn phí');

    -- ── SPC-STD ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'SPC-STD' AND a.name IN (
        'WiFi miễn phí', 'Bãi đỗ xe', 'Nhà hàng', 'Bar / Café', 'Thang máy',
        'Máy lạnh', 'TV màn hình phẳng', 'Tủ lạnh mini', 'Bàn làm việc',
        'Phòng tắm riêng', 'Vòi sen đứng', 'Máy sấy tóc', 'Đồ dùng cá nhân',
        'Bữa sáng miễn phí', 'View núi / đồi');

    -- ── SPC-DLX ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'SPC-DLX' AND a.name IN (
        'WiFi miễn phí', 'Bãi đỗ xe', 'Nhà hàng', 'Bar / Café', 'Thang máy',
        'Máy lạnh', 'TV màn hình phẳng', 'Tủ lạnh mini', 'Minibar',
        'Bàn làm việc', 'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng',
        'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bữa sáng miễn phí',
        'Ban công riêng', 'View núi / đồi');

    -- ── SPQ-DLX ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'SPQ-DLX' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Phòng gym', 'Bar / Café', 'Máy lạnh', 'TV màn hình phẳng', 'Minibar',
        'Bàn làm việc', 'Két an toàn', 'Phòng tắm riêng', 'Bồn tắm nằm',
        'Vòi sen đứng', 'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bữa sáng miễn phí',
        'Ban công riêng', 'Sân vườn riêng');

    -- ── SPQ-SEA ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'SPQ-SEA' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Phòng gym', 'Bar / Café', 'Máy lạnh', 'TV màn hình phẳng', 'Minibar',
        'Bàn làm việc', 'Két an toàn', 'Ổ cắm quốc tế', 'Phòng tắm riêng',
        'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc', 'Đồ dùng cá nhân',
        'Bữa sáng miễn phí', 'Ban công riêng', 'View biển');

    -- ── NWS-STD ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'NWS-STD' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Phòng gym', 'Bar / Café', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
        'Tủ lạnh mini', 'Minibar', 'Bàn làm việc', 'Két an toàn', 'Ổ cắm quốc tế',
        'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
        'Đồ dùng cá nhân', 'Bữa sáng miễn phí');

    -- ── NWS-DLX ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'NWS-DLX' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Phòng gym', 'Bar / Café', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
        'Tủ lạnh mini', 'Minibar', 'Bàn làm việc', 'Két an toàn', 'Ổ cắm quốc tế',
        'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
        'Đồ dùng cá nhân', 'Bữa sáng miễn phí', 'Ban công riêng');

    -- ── PVT-STD ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'PVT-STD' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Phòng gym', 'Bar / Café', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
        'Tủ lạnh mini', 'Minibar', 'Bàn làm việc', 'Két an toàn', 'Ổ cắm quốc tế',
        'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
        'Đồ dùng cá nhân', 'Bữa sáng miễn phí', 'Ban công riêng', 'View biển');

    -- ── PVT-DLX ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'PVT-DLX' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Phòng gym', 'Bar / Café', 'Thang máy', 'Máy lạnh', 'TV màn hình phẳng',
        'Tủ lạnh mini', 'Minibar', 'Bàn làm việc', 'Két an toàn', 'Ổ cắm quốc tế',
        'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc',
        'Đồ dùng cá nhân', 'Bữa sáng miễn phí', 'Ban công riêng');

    -- ── AZC-DLX ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'AZC-DLX' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Phòng gym', 'Bar / Café', 'Máy lạnh', 'TV màn hình phẳng', 'Minibar',
        'Bàn làm việc', 'Két an toàn', 'Phòng tắm riêng', 'Bồn tắm nằm',
        'Vòi sen đứng', 'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bữa sáng miễn phí',
        'Ban công riêng', 'Sân vườn riêng');

    -- ── AZC-SUI ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'AZC-SUI' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Phòng gym', 'Bar / Café', 'Máy lạnh', 'TV màn hình phẳng', 'Minibar',
        'Bàn làm việc', 'Két an toàn', 'Ổ cắm quốc tế', 'Phòng tắm riêng',
        'Bồn tắm nằm', 'Vòi sen đứng', 'Máy sấy tóc', 'Đồ dùng cá nhân',
        'Bữa sáng miễn phí', 'Ban công riêng', 'Hồ bơi riêng', 'Sân vườn riêng');

    -- ── TTC-DLX ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'TTC-DLX' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Phòng gym', 'Bar / Café', 'Máy lạnh', 'TV màn hình phẳng', 'Minibar',
        'Bàn làm việc', 'Két an toàn', 'Phòng tắm riêng', 'Bồn tắm nằm',
        'Vòi sen đứng', 'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bữa sáng miễn phí',
        'Ban công riêng', 'View biển');

    -- ── TTC-FAM ──────────────────────────────────────────────────────────────
    INSERT INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id FROM rooms r JOIN amenities a ON 1=1
    WHERE r.room_code = 'TTC-FAM' AND a.name IN (
        'WiFi miễn phí', 'Hồ bơi chung', 'Bãi đỗ xe', 'Nhà hàng', 'Spa / Massage',
        'Phòng gym', 'Bar / Café', 'Máy lạnh', 'TV màn hình phẳng', 'Tủ lạnh đầy đủ',
        'Bếp riêng', 'Lò vi sóng', 'Phòng tắm riêng', 'Bồn tắm nằm', 'Vòi sen đứng',
        'Máy sấy tóc', 'Đồ dùng cá nhân', 'Bữa sáng miễn phí', 'Ban công riêng',
        'Sân vườn riêng');

    -- =============================================
    -- KIỂM TRA SAU KHI IMPORT (mốc tiện nghi phòng/căn)
    -- =============================================
    -- SELECT COUNT(*) FROM amenities;           -- kỳ vọng: 32
    -- SELECT COUNT(DISTINCT room_id) FROM room_amenities; -- kỳ vọng: bằng tổng số rooms đã mở bán
    --
    -- Kiểm tra ownership:
    -- SELECT a.id, a.name, a.property_type, u.email owner_email
    --   FROM accommodations a JOIN users u ON a.owner_id=u.id ORDER BY a.id;
    -- Kỳ vọng:
    --   acc1,2,3 → partner@travelmate.vn (HOTEL)
    --   acc4,5   → partner3@travelmate.vn (VILLA)
    --   acc6,7   → partner4@travelmate.vn (HOMESTAY)
    --   acc8,9   → partner2@travelmate.vn (RESORT)
    --
    -- Kiểm tra settlements theo từng partner:
    -- SELECT u.email, COUNT(*) cnt, SUM(payout_amount) total
    --   FROM partner_settlements ps JOIN users u ON ps.partner_id=u.id
    --   GROUP BY u.email;

    -- =============================================
    -- 9. BẢNG ADMIN_ACTION_LOGS — Nhật ký thao tác Admin
    -- =============================================
    CREATE TABLE IF NOT EXISTS admin_action_logs (
        id           BIGINT       NOT NULL AUTO_INCREMENT,
        admin_email  VARCHAR(100) NOT NULL,
        action_type  VARCHAR(50)  NOT NULL,
        target_type  VARCHAR(30)  NOT NULL,
        target_id    BIGINT,
        description  VARCHAR(500),
        note         VARCHAR(1000),
        created_at   DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6),
        PRIMARY KEY (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


    -- =============================================
    -- BẢNG NOTIFICATIONS — Hệ thống thông báo người dùng
    -- =============================================
    CREATE TABLE IF NOT EXISTS notifications (
        id          BIGINT        NOT NULL AUTO_INCREMENT,
        user_id     BIGINT        NOT NULL,
        title       VARCHAR(200)  NOT NULL,
        message     TEXT,
        type        ENUM('REVIEW_REMINDER','BOOKING_CONFIRMED','BOOKING_CHECKIN_READY','SYSTEM')
                                  NOT NULL DEFAULT 'SYSTEM',
        target_url  VARCHAR(500),
        is_read     TINYINT(1)    NOT NULL DEFAULT 0,
        created_at  DATETIME(6)   DEFAULT CURRENT_TIMESTAMP(6),
        PRIMARY KEY (id),
        KEY idx_notif_user    (user_id),
        KEY idx_notif_unread  (user_id, is_read),
        CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- ─── NHẬT KÝ KHỞI TẠO — đủ loại thao tác cho mọi bộ lọc ────────────────────
    INSERT INTO admin_action_logs (admin_email, action_type, target_type, target_id, description, note, created_at) VALUES

    -- ── BOOKING actions ───────────────────────────────────────────────────────────
    ('admin@travelmate.vn', 'APPROVE_BOOKING',  'BOOKING',  7,  'Duyệt booking BK-LATA-FAM-0001 (Nguyễn Văn An — Phòng Gia Đình LATA)',                       NULL,                                                           DATE_SUB(NOW(), INTERVAL 12 DAY)),
    ('admin@travelmate.vn', 'COMPLETE_BOOKING', 'BOOKING',  7,  'Hoàn tất booking BK-LATA-FAM-0001 — Khách đã checkout',                                       NULL,                                                           DATE_SUB(NOW(), INTERVAL 7  DAY)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING',  'BOOKING',  3,  'Duyệt booking BK-TMG-DLX-0001 (Nguyễn Văn An — TM Grand Deluxe)',                             NULL,                                                           DATE_SUB(NOW(), INTERVAL 6  DAY)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING',  'BOOKING',  4,  'Duyệt booking BK-LATA-DLX-0001 (Nguyễn Văn An — LATA Deluxe)',                                NULL,                                                           DATE_SUB(NOW(), INTERVAL 6  DAY)),
    ('admin@travelmate.vn', 'NOSHOW_BOOKING',   'BOOKING',  5,  'No-show BK-TLP-STD-0001 — cọc giữ 288.000đ | hoa hồng theo tổng đơn 144.000đ | partner nhận 144.000đ',  'Khách đã xác nhận sẽ đến nhưng không xuất hiện đến hết ngày.',  DATE_SUB(NOW(), INTERVAL 5  DAY)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING',  'BOOKING',  6,  'Duyệt booking BK-TMG-PRE-0001 (Nguyễn Văn An — TM Grand Premium)',                            NULL,                                                           DATE_SUB(NOW(), INTERVAL 7  DAY)),
    ('admin@travelmate.vn', 'CHECKIN_BOOKING',  'BOOKING',  6,  'Check-in booking BK-TMG-PRE-0001 — Khách đã vào phòng',                                       NULL,                                                           DATE_SUB(NOW(), INTERVAL 1  DAY)),
    ('admin@travelmate.vn', 'RECORD_BOOKING',   'BOOKING',  2,  'Ghi nhận booking BK-TLP-SUP-0001 — VNPAY thanh toán 100%, TravelMate tự giữ phòng',             NULL, DATE_SUB(NOW(), INTERVAL 8  DAY)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING',  'BOOKING',  8,  'Duyệt booking BK-VNT-DLX-0001 (Trần Thị Bích — Vinpearl Deluxe Ocean)',                       NULL,                                                           DATE_SUB(NOW(), INTERVAL 4  DAY)),
    ('admin@travelmate.vn', 'COMPLETE_BOOKING', 'BOOKING',  10, 'Hoàn tất booking BK-MND-ATT-0001 — Phạm Quỳnh Anh đã checkout Homestay Mộc Nhiên',            NULL,                                                           DATE_SUB(NOW(), INTERVAL 5  DAY)),
    ('admin@travelmate.vn', 'NOSHOW_BOOKING',   'BOOKING',  12, 'No-show BK-FDN-DLX-0001 — cọc giữ 1.440.000đ | hoa hồng theo tổng đơn 864.000đ | partner nhận 576.000đ', 'Ngô Thị Lan không đến Furama Resort. Cọc đã phân bổ theo chính sách no-show.',   DATE_SUB(NOW(), INTERVAL 6  DAY)),

    ('admin@travelmate.vn', 'APPROVE_BOOKING',  'BOOKING',  15, 'Duyet booking BK-TLP-SUP-0002 (Nguyen Van An - Tulip Hotel Superior)', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),

    -- ACCOMMODATION actions
    ('admin@travelmate.vn', 'APPROVE_LISTING',  'ACCOMMODATION', 1,  'Duyệt listing: LATA Hotel & Apartments — 4★ Đà Lạt (partner@travelmate.vn)',              NULL,                                                           DATE_SUB(NOW(), INTERVAL 30 DAY)),
    ('admin@travelmate.vn', 'APPROVE_LISTING',  'ACCOMMODATION', 2,  'Duyệt listing: Tulip Hotel 2 Dalat — 3★ Đà Lạt (partner@travelmate.vn)',                  NULL,                                                           DATE_SUB(NOW(), INTERVAL 30 DAY)),
    ('admin@travelmate.vn', 'APPROVE_LISTING',  'ACCOMMODATION', 3,  'Duyệt listing: TravelMate Grand Hotel — 5★ Đà Lạt (partner@travelmate.vn)',               NULL,                                                           DATE_SUB(NOW(), INTERVAL 30 DAY)),
    ('admin@travelmate.vn', 'APPROVE_LISTING',  'ACCOMMODATION', 4,  'Duyệt listing: The Anam Villa Nha Trang — 5★ (partner3@travelmate.vn)',                   NULL,                                                           DATE_SUB(NOW(), INTERVAL 28 DAY)),
    ('admin@travelmate.vn', 'APPROVE_LISTING',  'ACCOMMODATION', 8,  'Duyệt listing: Vinpearl Resort & Spa Nha Trang — 5★ (partner2@travelmate.vn)',            NULL,                                                           DATE_SUB(NOW(), INTERVAL 25 DAY)),
    ('admin@travelmate.vn', 'REJECT_LISTING',   'ACCOMMODATION', 11, 'Từ chối listing: Da Lat Sunrise Guesthouse — hồ sơ chưa đạt yêu cầu',                     'Thiếu ảnh thumbnail thực tế và giấy phép kinh doanh. Partner cần bổ sung trước khi duyệt.', DATE_SUB(NOW(), INTERVAL 2 DAY)),
    ('admin@travelmate.vn', 'HIDE_LISTING',     'ACCOMMODATION', 11, 'Ẩn listing: Da Lat Sunrise Guesthouse khỏi kết quả tìm kiếm',                            'Listing bị từ chối — tạm ẩn để partner cập nhật lại thông tin.', DATE_SUB(NOW(), INTERVAL 2 DAY)),

    -- ── ROOM actions ──────────────────────────────────────────────────────────────
    ('admin@travelmate.vn', 'APPROVE_ROOM',     'ROOM',  1,  'Duyệt phòng: LATA-STD (Phòng Tiêu Chuẩn Giường King, 650.000đ/đêm)',                            NULL,                                                           DATE_SUB(NOW(), INTERVAL 28 DAY)),
    ('admin@travelmate.vn', 'APPROVE_ROOM',     'ROOM',  2,  'Duyệt phòng: LATA-DLX (Phòng Deluxe Giường Đôi, 850.000đ/đêm)',                                 NULL,                                                           DATE_SUB(NOW(), INTERVAL 28 DAY)),
    ('admin@travelmate.vn', 'APPROVE_ROOM',     'ROOM',  4,  'Duyệt phòng: LATA-SUI (Suite Cao Cấp, 1.800.000đ/đêm, commission 20%)',                          NULL,                                                           DATE_SUB(NOW(), INTERVAL 28 DAY)),
    ('admin@travelmate.vn', 'APPROVE_ROOM',     'ROOM',  25, 'Duyệt phòng: VNT-DLX (Deluxe Ocean View Vinpearl, 2.800.000đ/đêm)',                              NULL,                                                           DATE_SUB(NOW(), INTERVAL 20 DAY)),
    ('admin@travelmate.vn', 'APPROVE_ROOM',     'ROOM',  26, 'Duyệt phòng: VNT-SUI (Junior Suite Beachfront, 4.500.000đ/đêm, commission 22%)',                 NULL,                                                           DATE_SUB(NOW(), INTERVAL 20 DAY)),
    ('admin@travelmate.vn', 'REJECT_ROOM',      'ROOM',  12, 'Từ chối phòng: TMG-PRE2 (Presidential Suite) — chờ bổ sung ảnh thực tế',                        'Partner cần upload ít nhất 5 ảnh thực tế phòng trước khi được duyệt.', DATE_SUB(NOW(), INTERVAL 3 DAY)),

    -- ── USER actions ──────────────────────────────────────────────────────────────
    ('admin@travelmate.vn', 'LOCK_USER',        'USER',  5,  'Khóa tài khoản: user2@travelmate.vn (Trần Thị Mai) — vi phạm chính sách',                       'Tài khoản có dấu hiệu đặt phòng giả mạo, cần điều tra thêm.',  DATE_SUB(NOW(), INTERVAL 10 DAY)),
    ('admin@travelmate.vn', 'UNLOCK_USER',      'USER',  5,  'Mở khóa tài khoản: user2@travelmate.vn — đã xác minh danh tính',                                'Sau khi xác minh, tài khoản không vi phạm, mở khóa bình thường.', DATE_SUB(NOW(), INTERVAL 9 DAY)),
    ('admin@travelmate.vn', 'LOCK_USER',        'USER',  6,  'Khóa tài khoản: user3@travelmate.vn (Lê Văn Đức) — yêu cầu xác minh email',                    'Tài khoản đăng nhập từ thiết bị lạ, áp dụng khóa tạm thời.',   DATE_SUB(NOW(), INTERVAL 3  DAY)),
    ('admin@travelmate.vn', 'UNLOCK_USER',      'USER',  6,  'Mở khóa tài khoản: user3@travelmate.vn — đã xác minh qua email',                                NULL,                                                           DATE_SUB(NOW(), INTERVAL 2  DAY)),

    -- ── REVIEW actions ────────────────────────────────────────────────────────────
    ('admin@travelmate.vn', 'APPROVE_REVIEW',   'REVIEW', 1,  'Duyệt đánh giá #1 — Nguyễn Văn An cho LATA Hotel (10/10)',                                      NULL,                                                           DATE_SUB(NOW(), INTERVAL 7  DAY)),
    ('admin@travelmate.vn', 'HIDE_REVIEW',      'REVIEW', 2,  'Ẩn đánh giá #2 — Nội dung không phù hợp, có chứa thông tin cá nhân của nhân viên',             'Review vi phạm quy định: không được tiết lộ thông tin nhân viên.', DATE_SUB(NOW(), INTERVAL 4 DAY)),
    ('admin@travelmate.vn', 'APPROVE_REVIEW',   'REVIEW', 5,  'Duyệt đánh giá #5 — Lê Văn Đức cho Ba Na Hills Forest Villa (10/10)',                           NULL,                                                           DATE_SUB(NOW(), INTERVAL 14 DAY)),

    -- ── VOUCHER actions ───────────────────────────────────────────────────────────
    ('admin@travelmate.vn', 'CREATE_VOUCHER',   'VOUCHER', NULL, 'Tạo voucher SUMMER10 — giảm 10% toàn sàn (Admin chịu), hiệu lực 01/05–31/05/2026',          'Chương trình khuyến mãi hè 2026 dành cho toàn bộ khách hàng.',  DATE_SUB(NOW(), INTERVAL 35 DAY)),
    ('admin@travelmate.vn', 'CREATE_VOUCHER',   'VOUCHER', NULL, 'Tạo voucher WELCOME50K — giảm 50.000đ cho đơn đầu tiên (Admin chịu)',                         'Voucher chào mừng khách hàng mới đăng ký tài khoản.',           DATE_SUB(NOW(), INTERVAL 30 DAY)),
    ('admin@travelmate.vn', 'TOGGLE_VOUCHER',   'VOUCHER', NULL, 'Tắt voucher WELCOME50K — hết ngân sách chiến dịch chào mừng',                                 'Đã phát 200 voucher, tắt để tránh vượt ngân sách Marketing.',   DATE_SUB(NOW(), INTERVAL 5  DAY)),
    ('admin@travelmate.vn', 'CREATE_VOUCHER',   'VOUCHER', NULL, 'Phát hành voucher LATA20 — kho Hotel, Partner chịu phí và gắn vào phòng phù hợp',               NULL,                                                           DATE_SUB(NOW(), INTERVAL 20 DAY)),
    ('admin@travelmate.vn', 'TOGGLE_VOUCHER',   'VOUCHER', NULL, 'Bật lại voucher SUMMER10 — gia hạn thêm 1 tháng theo yêu cầu Marketing',                     'Voucher được gia hạn đến 30/06/2026 theo kế hoạch hè 2.',       DATE_SUB(NOW(), INTERVAL 2  DAY)),

    -- ── SETTLEMENT actions ────────────────────────────────────────────────────────
    ('admin@travelmate.vn', 'GENERATE_SETTLEMENT', 'SETTLEMENT', NULL, 'Tạo quyết toán tháng 04/2026: partner HOTEL 975.000đ, RESORT 4.592.000đ',           'Tự động tính từ bookings COMPLETED trong kỳ.',                  DATE_SUB(NOW(), INTERVAL 17 DAY)),
    ('admin@travelmate.vn', 'MARK_SETTLEMENT_PAID','SETTLEMENT', NULL, 'Thanh toán quyết toán tháng 04/2026 cho partner HOTEL (Sunrise Sapa Lodge): 975.000đ','Chuyển khoản MB Bank 0123456789, tham chiếu STL-HOTEL-M04.',     DATE_SUB(NOW(), INTERVAL 17 DAY)),
    ('admin@travelmate.vn', 'MARK_SETTLEMENT_PAID','SETTLEMENT', NULL, 'Thanh toán quyết toán tháng 04/2026 cho partner RESORT (Blue Ocean): 4.592.000đ',   'Chuyển khoản Vietcombank 9876543210, tham chiếu STL-RESORT-M04.', DATE_SUB(NOW(), INTERVAL 17 DAY)),
    ('admin@travelmate.vn', 'GENERATE_SETTLEMENT', 'SETTLEMENT', NULL, 'Tạo quyết toán tháng hiện tại: 4 partner, tổng payout 30.497.000đ',                     'PENDING — Admin sẽ chuyển khoản đầu tháng tới.',                   NOW()),

    -- ── SUPPORT_TICKET actions ────────────────────────────────────────────────────
    ('admin@travelmate.vn', 'RESPOND_TICKET',   'TICKET',  1,  'Trả lời ticket #1 — partner@travelmate.vn: "Quyết toán tháng 03/2026 bị sai số tiền"',               'Xác nhận số liệu đúng, giải thích hoa hồng theo tổng đơn gốc trước voucher và voucher Partner chịu.', DATE_SUB(NOW(), INTERVAL 7 DAY)),
    ('admin@travelmate.vn', 'RESPOND_TICKET',   'TICKET',  2,  'Trả lời ticket #2 — partner@travelmate.vn: "Không thể đánh dấu No-Show"',                     'Đánh dấu BK-TLP-STD-0001 là NO_SHOW thay partner, giải thích quy trình.', DATE_SUB(NOW(), INTERVAL 4 DAY)),
    ('admin@travelmate.vn', 'CLOSE_TICKET',     'TICKET',  1,  'Đóng ticket #1 — Đã giải quyết xong vấn đề quyết toán tháng 03/2026',                               NULL,                                                           DATE_SUB(NOW(), INTERVAL 6 DAY)),
    ('admin@travelmate.vn', 'RESPOND_TICKET',   'TICKET',  5,  'Trả lời ticket #5 — user@travelmate.vn: "Không nhận được email xác nhận đặt phòng"',          'Đã kiểm tra log email, resend thủ công. Hướng dẫn check spam.', DATE_SUB(NOW(), INTERVAL 3 DAY));

    -- ── ADMIN ACTION LOGS BỔ SUNG — Booking, Listing, Settlement cho partner3 & partner4 ──
    INSERT INTO admin_action_logs (admin_email, action_type, target_type, target_id, description, note, created_at) VALUES

    -- Duyệt thêm bookings (bk3, 9, 11, 13, 14 — ban đầu chưa log)
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', 9,  'Duyệt booking BK-VNT-SUI-0001 (Lê Minh Đức — Vinpearl Junior Suite)', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    ('admin@travelmate.vn', 'RECORD_BOOKING', 'BOOKING', 11, 'Ghi nhận booking BK-ANM-GDN-0001 (Hoàng Văn Hùng — Anam Garden Villa, cọc 30%)', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', 13, 'Duyệt booking BK-HLR-DLX-0001 (Vũ Thị Mai — Hoa Lư Deluxe)', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', 14, 'Duyệt booking BK-BNH-BNG-0001 (Đinh Văn Tùng — Ba Na Hills Forest Bungalow)', NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('admin@travelmate.vn', 'CHECKIN_BOOKING', 'BOOKING', 14, 'Check-in booking BK-BNH-BNG-0001 — Khách đã nhận phòng Ba Na Hills', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', 29, 'Duyệt booking BK-ANM-BCH-0001 (Trần Thị Mai — Anam Beachfront Pool Villa, 3 đêm)', NULL, DATE_SUB(NOW(), INTERVAL 22 DAY)),
    ('admin@travelmate.vn', 'COMPLETE_BOOKING', 'BOOKING', 29, 'Hoàn tất booking BK-ANM-BCH-0001 — Trần Thị Mai đã checkout Anam Villa', NULL, DATE_SUB(NOW(), INTERVAL 17 DAY)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', 30, 'Duyệt booking BK-BNH-SUI-0001 (Lê Văn Đức — Ba Na Hills Treetop Suite, voucher ANAM15)', NULL, DATE_SUB(NOW(), INTERVAL 34 DAY)),
    ('admin@travelmate.vn', 'COMPLETE_BOOKING', 'BOOKING', 30, 'Hoàn tất booking BK-BNH-SUI-0001 — Lê Văn Đức đã checkout Ba Na Hills', NULL, DATE_SUB(NOW(), INTERVAL 30 DAY)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', 31, 'Duyệt booking BK-HLR-FAM-0001 (Trần Thị Mai — Hoa Lư Family Room)', NULL, DATE_SUB(NOW(), INTERVAL 27 DAY)),
    ('admin@travelmate.vn', 'COMPLETE_BOOKING', 'BOOKING', 31, 'Hoàn tất booking BK-HLR-FAM-0001 — Trần Thị Mai đã checkout Hoa Lư Homestay', NULL, DATE_SUB(NOW(), INTERVAL 23 DAY)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', 32, 'Duyệt booking BK-MND-FAM-0001 (Nguyễn Văn An — Mộc Nhiên Family, voucher HOALUU50K)', NULL, DATE_SUB(NOW(), INTERVAL 42 DAY)),
    ('admin@travelmate.vn', 'COMPLETE_BOOKING', 'BOOKING', 32, 'Hoàn tất booking BK-MND-FAM-0001 — Nguyễn Văn An đã checkout Mộc Nhiên Homestay', NULL, DATE_SUB(NOW(), INTERVAL 38 DAY)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', 33, 'Duyệt booking BK-ANM-GDN-0003 (Lê Văn Đức — Anam Garden Villa, check-in 5 ngày tới)', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', 34, 'Duyệt booking BK-MND-ATT-0002 (Trần Thị Mai — Mộc Nhiên Attic, check-in 3 ngày tới)', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', 35, 'Duyệt booking BK-FDN-BCH-0001 (Lê Văn Đức — Furama Beachfront Superior, đã check-in)', NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),
    ('admin@travelmate.vn', 'CHECKIN_BOOKING', 'BOOKING', 35, 'Check-in booking BK-FDN-BCH-0001 — Khách đã nhận phòng Furama Đà Nẵng', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),

    -- Duyệt listing còn thiếu (acc5, acc6, acc7, acc9 — chưa có log)
    ('admin@travelmate.vn', 'APPROVE_LISTING', 'ACCOMMODATION', 5,  'Duyệt listing: Ba Na Hills Forest Villa Đà Nẵng — 4★ (partner3@travelmate.vn)',  NULL, DATE_SUB(NOW(), INTERVAL 28 DAY)),
    ('admin@travelmate.vn', 'APPROVE_LISTING', 'ACCOMMODATION', 6,  'Duyệt listing: Hoa Lư Riverside Homestay Hội An — 4★ (partner4@travelmate.vn)',  NULL, DATE_SUB(NOW(), INTERVAL 28 DAY)),
    ('admin@travelmate.vn', 'APPROVE_LISTING', 'ACCOMMODATION', 7,  'Duyệt listing: Mộc Nhiên Garden Homestay Đà Lạt — 4★ (partner4@travelmate.vn)', NULL, DATE_SUB(NOW(), INTERVAL 26 DAY)),
    ('admin@travelmate.vn', 'APPROVE_LISTING', 'ACCOMMODATION', 9,  'Duyệt listing: Furama Resort Đà Nẵng — 5★ (partner2@travelmate.vn)',             NULL, DATE_SUB(NOW(), INTERVAL 24 DAY)),
    ('admin@travelmate.vn', 'APPROVE_LISTING', 'ACCOMMODATION', 10, 'Nhận hồ sơ đăng ký listing: Da Lat Mountain Boutique Hotel — đang chờ thẩm định', 'Partner gửi đủ 5 ảnh thực tế, đang xem xét giấy phép kinh doanh.', DATE_SUB(NOW(), INTERVAL 1 HOUR)),

    -- Duyệt phòng cho partner3 & partner4 (acc4 Anam, acc5 Ba Na, acc6 Hoa Lư, acc7 Mộc Nhiên)
    ('admin@travelmate.vn', 'APPROVE_ROOM', 'ROOM', 13, 'Duyệt phòng: ANM-GDN (Garden Pool Villa Anam, 3.500.000đ/đêm, comm12%)', NULL, DATE_SUB(NOW(), INTERVAL 27 DAY)),
    ('admin@travelmate.vn', 'APPROVE_ROOM', 'ROOM', 14, 'Duyệt phòng: ANM-BCH (Beachfront Pool Villa, 5.800.000đ/đêm, comm15% override)', NULL, DATE_SUB(NOW(), INTERVAL 27 DAY)),
    ('admin@travelmate.vn', 'APPROVE_ROOM', 'ROOM', 15, 'Duyệt phòng: ANM-FAM (Family Pool Villa, 6.200.000đ/đêm)', NULL, DATE_SUB(NOW(), INTERVAL 27 DAY)),
    ('admin@travelmate.vn', 'APPROVE_ROOM', 'ROOM', 16, 'Duyệt phòng: BNH-BNG (Forest Bungalow Ba Na, 2.200.000đ/đêm)', NULL, DATE_SUB(NOW(), INTERVAL 27 DAY)),
    ('admin@travelmate.vn', 'APPROVE_ROOM', 'ROOM', 17, 'Duyệt phòng: BNH-TWN (Twin Cabin Ba Na, 1.600.000đ/đêm)', NULL, DATE_SUB(NOW(), INTERVAL 27 DAY)),
    ('admin@travelmate.vn', 'APPROVE_ROOM', 'ROOM', 18, 'Duyệt phòng: BNH-SUI (Treetop Suite Ba Na, 3.800.000đ/đêm, comm16% override)', NULL, DATE_SUB(NOW(), INTERVAL 27 DAY)),
    ('admin@travelmate.vn', 'APPROVE_ROOM', 'ROOM', 19, 'Duyệt phòng: HLR-STD (Riverside Standard Hoa Lư, 320.000đ/đêm)', NULL, DATE_SUB(NOW(), INTERVAL 27 DAY)),
    ('admin@travelmate.vn', 'APPROVE_ROOM', 'ROOM', 20, 'Duyệt phòng: HLR-DLX (Riverside Deluxe Hoa Lư, 480.000đ/đêm)', NULL, DATE_SUB(NOW(), INTERVAL 27 DAY)),
    ('admin@travelmate.vn', 'APPROVE_ROOM', 'ROOM', 22, 'Duyệt phòng: MND-STD (Standard Garden View Mộc Nhiên, 390.000đ/đêm)', NULL, DATE_SUB(NOW(), INTERVAL 25 DAY)),
    ('admin@travelmate.vn', 'APPROVE_ROOM', 'ROOM', 23, 'Duyệt phòng: MND-ATT (Attic Pine View Mộc Nhiên, 580.000đ/đêm)', NULL, DATE_SUB(NOW(), INTERVAL 25 DAY)),
    ('admin@travelmate.vn', 'APPROVE_ROOM', 'ROOM', 24, 'Duyệt phòng: MND-FAM (Family Rooftop Mộc Nhiên, 880.000đ/đêm)', NULL, DATE_SUB(NOW(), INTERVAL 25 DAY)),
    ('admin@travelmate.vn', 'APPROVE_ROOM', 'ROOM', 28, 'Duyệt phòng: FDN-DLX (Deluxe Garden View Furama, 2.400.000đ/đêm, comm18%)', NULL, DATE_SUB(NOW(), INTERVAL 23 DAY)),
    ('admin@travelmate.vn', 'APPROVE_ROOM', 'ROOM', 29, 'Duyệt phòng: FDN-BCH (Beachfront Superior Furama, 3.600.000đ/đêm, comm20% override)', NULL, DATE_SUB(NOW(), INTERVAL 23 DAY)),
    ('admin@travelmate.vn', 'APPROVE_ROOM', 'ROOM', 30, 'Duyệt phòng: FDN-FAM (Family Suite Furama, 5.200.000đ/đêm, comm15% override)', NULL, DATE_SUB(NOW(), INTERVAL 23 DAY)),

    -- Duyệt reviews của partner3 & partner4
    ('admin@travelmate.vn', 'APPROVE_REVIEW', 'REVIEW', 3,  'Duyệt đánh giá #3 — Trần Thị Mai cho The Anam Villa (10/10 Beachfront Pool Villa)', NULL, DATE_SUB(NOW(), INTERVAL 15 DAY)),
    ('admin@travelmate.vn', 'APPROVE_REVIEW', 'REVIEW', 4,  'Duyệt đánh giá #4 — Lê Văn Đức cho Ba Na Hills Treetop Suite (8/10)', NULL, DATE_SUB(NOW(), INTERVAL 28 DAY)),
    ('admin@travelmate.vn', 'APPROVE_REVIEW', 'REVIEW', 6,  'Duyệt đánh giá #6 — Trần Thị Mai cho Hoa Lư Family Room (10/10)', NULL, DATE_SUB(NOW(), INTERVAL 21 DAY)),
    ('admin@travelmate.vn', 'APPROVE_REVIEW', 'REVIEW', 7,  'Duyệt đánh giá #7 — Nguyễn Văn An cho Mộc Nhiên Family Room (10/10 — voucher HOALUU50K)', NULL, DATE_SUB(NOW(), INTERVAL 36 DAY)),
    ('admin@travelmate.vn', 'APPROVE_REVIEW', 'REVIEW', 8,  'Duyệt đánh giá #8 — Nguyễn Văn An cho Hoa Lư Riverside Homestay (10/10)', NULL, DATE_SUB(NOW(), INTERVAL 67 DAY)),
    ('admin@travelmate.vn', 'APPROVE_REVIEW', 'REVIEW', 9,  'Duyệt đánh giá #9 — Nguyễn Văn An cho Furama Resort Đà Nẵng (10/10)', NULL, DATE_SUB(NOW(), INTERVAL 22 DAY)),
    ('admin@travelmate.vn', 'APPROVE_REVIEW', 'REVIEW', 10, 'Duyệt đánh giá #10 — Trần Thị Mai cho Vinpearl Resort & Spa (8/10)', NULL, DATE_SUB(NOW(), INTERVAL 26 DAY)),

    -- Quyết toán thêm cho partner3 & partner4 (log GENERATE + PAID)
    ('admin@travelmate.vn', 'GENERATE_SETTLEMENT', 'SETTLEMENT', NULL, 'Tạo quyết toán tháng 03/2026: partner VILLA (Ba Na) 6.688.000đ', 'Tự động từ 2 bookings Ba Na Hills BNH-BNG & BNH-TWN COMPLETED.', DATE_SUB(NOW(), INTERVAL 38 DAY)),
    ('admin@travelmate.vn', 'MARK_SETTLEMENT_PAID', 'SETTLEMENT', NULL, 'Thanh toán quyết toán tháng 03/2026 cho partner3 VILLA (Bùi Thị Lan Anh): 6.688.000đ', 'Chuyển khoản Techcombank 1234567890, tham chiếu STL-VILLA-M03.', DATE_SUB(NOW(), INTERVAL 37 DAY)),
    ('admin@travelmate.vn', 'GENERATE_SETTLEMENT', 'SETTLEMENT', NULL, 'Tạo quyết toán tháng 02/2026: partner HOMESTAY (Hoa Lư) 1.440.000đ', 'Tự động từ 2 bookings HLR-STD & HLR-DLX COMPLETED.', DATE_SUB(NOW(), INTERVAL 45 DAY)),
    ('admin@travelmate.vn', 'MARK_SETTLEMENT_PAID', 'SETTLEMENT', NULL, 'Thanh toán quyết toán tháng 02/2026 cho partner4 HOMESTAY (Trần Văn Cường): 1.440.000đ', 'Chuyển khoản VPBank 0987654321, tham chiếu STL-HOMESTAY-M02.', DATE_SUB(NOW(), INTERVAL 44 DAY)),
    ('admin@travelmate.vn', 'GENERATE_SETTLEMENT', 'SETTLEMENT', NULL, 'Tạo quyết toán tháng 04/2026: partner VILLA (Anam) 14.790.000đ, partner HOMESTAY (Hoa Lư) 1.380.000đ', 'Tự động tính từ bookings COMPLETED trong kỳ.', DATE_SUB(NOW(), INTERVAL 5 DAY)),
    ('admin@travelmate.vn', 'MARK_SETTLEMENT_PAID', 'SETTLEMENT', NULL, 'Thanh toán quyết toán tháng 04/2026 cho partner3 VILLA (Bùi Thị Lan Anh): 14.790.000đ', 'Chuyển khoản Techcombank 1234567890, tham chiếu STL-VILLA-M04-2026.', DATE_SUB(NOW(), INTERVAL 4 DAY)),
    ('admin@travelmate.vn', 'MARK_SETTLEMENT_PAID', 'SETTLEMENT', NULL, 'Thanh toán quyết toán tháng 04/2026 cho partner4 HOMESTAY (Trần Văn Cường): 1.380.000đ', 'Chuyển khoản VPBank 0987654321, tham chiếu STL-HOMESTAY-M04-2026.', DATE_SUB(NOW(), INTERVAL 4 DAY)),

    -- Thêm ticket support & phản hồi cho partner3, partner4
    ('admin@travelmate.vn', 'RESPOND_TICKET', 'TICKET', 3, 'Trả lời ticket #3 — partner3@travelmate.vn: "Voucher ANAM15 bị tính sai phần trăm"', 'Xác nhận: ANAM15 giảm 15% trên tổng đơn, tối đa 1.200.000đ. Settlement đã tính đúng.', DATE_SUB(NOW(), INTERVAL 11 DAY)),
    ('admin@travelmate.vn', 'CLOSE_TICKET',   'TICKET', 3, 'Đóng ticket #3 — Đã giải thích voucher kho do Admin cấp và Partner gắn vào căn', NULL, DATE_SUB(NOW(), INTERVAL 10 DAY)),
    ('admin@travelmate.vn', 'RESPOND_TICKET', 'TICKET', 4, 'Trả lời ticket #4 — partner4@travelmate.vn: "Phòng MND-FAM hiển thị sai giá trên trang khách"', 'Đã kiểm tra: giá sau voucher HOALUU50K = 1.710.000đ hiển thị đúng. Hướng dẫn xem lịch sử booking.', DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ('admin@travelmate.vn', 'CLOSE_TICKET',   'TICKET', 4, 'Đóng ticket #4 — Đã xác nhận hiển thị giá chính xác', NULL, DATE_SUB(NOW(), INTERVAL 7 DAY)),

    -- Voucher partner3 và partner4
    ('admin@travelmate.vn', 'CREATE_VOUCHER', 'VOUCHER', NULL, 'Phát hành voucher ANAM15 — kho Villa, Partner chịu phí và gắn vào căn phù hợp', NULL, DATE_SUB(NOW(), INTERVAL 25 DAY)),
    ('admin@travelmate.vn', 'CREATE_VOUCHER', 'VOUCHER', NULL, 'Tạo voucher HOALUU50K — giảm 50.000đ phòng MND-FAM Mộc Nhiên (Partner4 chịu, scope: ROOM)', NULL, DATE_SUB(NOW(), INTERVAL 22 DAY)),
    ('admin@travelmate.vn', 'CREATE_VOUCHER', 'VOUCHER', NULL, 'Tạo voucher VNT100K — giảm 100.000đ phòng VNT-DLX Vinpearl (Partner2 chịu, scope: ROOM)', NULL, DATE_SUB(NOW(), INTERVAL 20 DAY));

    -- Gán approved_at cho các payment đã được duyệt
    SET SQL_SAFE_UPDATES = 0;
    UPDATE payments
    SET approved_at = paid_at
    WHERE id > 0 AND payment_status IN ('APPROVED', 'DEPOSIT_FORFEITED');
    -- Giữ SQL_SAFE_UPDATES tắt đến cuối script để Workbench không chặn các block cập nhật vận hành.

    -- =============================================
    -- KỊCH BẢN LISTING PAGE — HOTEL type
    -- Tìm kiếm checkIn = hôm nay+4, checkOut = hôm nay+6 để thấy:
    --   🔴 LATA Hotel (acc1)           → HẾT PHÒNG  (FULL)     — tất cả 13 slot đặt kín
    --   🟡 TravelMate Grand Hotel (acc3) → SẮP HẾT  (LIMITED)  — còn 4/17 phòng (24%)
    --   🟢 Tulip Hotel 2 Dalat (acc2)  → CÒN TRỐNG  (AVAILABLE) — không có booking mới
    --
    -- Cách test: /accommodations?type=HOTEL
    --   → Nhập checkIn = YYYY-MM-DD (hôm nay + 4), checkOut = YYYY-MM-DD (hôm nay + 6)
    --   → Bấm Tìm kiếm → quan sát badge màu trên từng card
    --
    -- Cho VILLA/HOMESTAY/RESORT: tìm checkIn = hôm nay+10, checkOut = hôm nay+14
    --   để thấy các kịch bản đã có sẵn (Anam FULL, Vinpearl LIMITED, Ba Na, Hoa Lư...)
    -- =============================================

    -- === LATA Hotel (acc1) FULL tại khoảng checkIn+4 → checkOut+6 ===

    -- Booking #36: LATA-STD (r1) — đặt kín 5 slot còn lại (r1 total=6, bk#1 đã giữ 1 slot +3→+5)
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-LATA-STD-LST36', 5, 1, 1,
        DATE_ADD(CURDATE(), INTERVAL 4 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY),
        8, 2, 5,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        6500000, 1950000, 4550000,
        'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'Tình huống kín phòng: nhóm 5 phòng STD dịp cuối tuần.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 1950000,
        CONCAT('TXN-LATA-STD-LST1-', UNIX_TIMESTAMP()), 'APPROVED', NOW(),
        'Cọc 30% LATA-STD 5 phòng — trạng thái kín phòng');
    UPDATE rooms SET available_quantity = available_quantity - 5 WHERE id = 1;

    -- Booking #37: LATA-DLX (r2) — đặt kín 4 slot (r2 total=4, hiện occupied=0 tại +4→+6)
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-LATA-DLX-0004', 6, 1, 2,
        DATE_ADD(CURDATE(), INTERVAL 4 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY),
        6, 2, 4,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        6800000, 2040000, 4760000,
        'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'Tình huống kín phòng: đoàn 4 phòng Deluxe.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 2040000,
        CONCAT('TXN-LATA-DLX-LST1-', UNIX_TIMESTAMP()), 'APPROVED', NOW(),
        'Cọc 30% LATA-DLX 4 phòng — trạng thái kín phòng');
    UPDATE rooms SET available_quantity = available_quantity - 4 WHERE id = 2;

    -- Booking #38: LATA-FAM (r3) — đặt kín 3 slot (r3 total=3, hiện không có booking)
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-LATA-FAM-0002', 5, 1, 3,
        DATE_ADD(CURDATE(), INTERVAL 4 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY),
        6, 3, 3,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        7200000, 2160000, 5040000,
        'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'Tình huống kín phòng: 3 phòng Family đặt kín.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 2160000,
        CONCAT('TXN-LATA-FAM-LST1-', UNIX_TIMESTAMP()), 'APPROVED', NOW(),
        'Cọc 30% LATA-FAM 3 phòng — trạng thái kín phòng');
    UPDATE rooms SET available_quantity = available_quantity - 3 WHERE id = 3;

    -- Booking #39: LATA-SUI (r4) — đặt kín 1 slot Suite (r4 total=1)
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-LATA-SUI-0002', 6, 1, 4,
        DATE_ADD(CURDATE(), INTERVAL 4 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY),
        2, 0, 1,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        3600000, 3600000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Tình huống kín phòng: Suite cao cấp đặt kín.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 3600000,
        CONCAT('TXN-LATA-SUI-LST1-', UNIX_TIMESTAMP()), 'APPROVED', NOW(),
        '100% LATA-SUI — trạng thái kín phòng');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 4;
    -- Kết quả LATA tại +4→+6: r1=0, r2=0, r3=0, r4=0 → totalAvail=0 → FULL 🔴

    -- === TravelMate Grand Hotel (acc3) LIMITED tại khoảng checkIn+4 → checkOut+6 ===
    -- Mục tiêu: 4/17 slot còn trống = 23.5% ≤ 30% → LIMITED

    -- Booking #40: TMG-DLX (r9) — chiếm 6/8 slot, còn lại 2
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-TMG-DLX-0005', 5, 3, 9,
        DATE_ADD(CURDATE(), INTERVAL 4 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY),
        10, 2, 6,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        14400000, 4320000, 10080000,
        'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'Tình huống sắp hết phòng: đoàn 6 phòng Deluxe Garden View.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 4320000,
        CONCAT('TXN-TMG-DLX-LST1-', UNIX_TIMESTAMP()), 'APPROVED', NOW(),
        'Cọc 30% TMG-DLX 6 phòng — trạng thái sắp hết phòng');
    UPDATE rooms SET available_quantity = available_quantity - 6 WHERE id = 9;

    -- Booking #41: TMG-PRE (r10) — chiếm 4/5 slot, còn lại 1
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-TMG-PRE-0002', 6, 3, 10,
        DATE_ADD(CURDATE(), INTERVAL 4 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY),
        8, 0, 4,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        13200000, 3960000, 9240000,
        'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'Tình huống sắp hết phòng: đoàn 4 phòng Premium Valley View.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 3960000,
        CONCAT('TXN-TMG-PRE-LST1-', UNIX_TIMESTAMP()), 'APPROVED', NOW(),
        'Cọc 30% TMG-PRE 4 phòng — trạng thái sắp hết phòng');
    UPDATE rooms SET available_quantity = available_quantity - 4 WHERE id = 10;

    -- Booking #42: TMG-FAM (r11) — chiếm tất cả 3 slot Family, còn lại 0
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-TMG-FAM-0001', 5, 3, 11,
        DATE_ADD(CURDATE(), INTERVAL 4 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY),
        6, 2, 3,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        16800000, 5040000, 11760000,
        'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'Tình huống sắp hết phòng: đặt kín 3 phòng Family Grand Suite.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 5040000,
        CONCAT('TXN-TMG-FAM-LST1-', UNIX_TIMESTAMP()), 'APPROVED', NOW(),
        'Cọc 30% TMG-FAM 3 phòng — trạng thái sắp hết phòng');
    UPDATE rooms SET available_quantity = available_quantity - 3 WHERE id = 11;
    -- r12 TMG-PRE2 (Presidential Suite, total=1): không thay đổi → 1 phòng còn trống
    -- Kết quả TM Grand tại +4→+6: r9=2, r10=1, r11=0, r12=1 → totalAvail=4, totalRooms=17 → LIMITED (24%) 🟡

    -- === Tulip Hotel 2 Dalat (acc2) ===
    -- Không thêm booking mới → totalAvail=16, totalRooms=16 → AVAILABLE (100%) 🟢

    -- =============================================
    -- KIỂM TRA KỲ VỌNG SAU IMPORT v7 (bookings #36-42 mới)
    -- Các số lượng tổng thể được kiểm tra ở checklist cuối file.
    -- Kịch bản tìm kiếm HOTEL tại +4→+6:
    --   acc1 LATA Hotel:           totalAvail= 0, totalRooms=14 → FULL
    --   acc3 TM Grand Hotel:       totalAvail= 4, totalRooms=17 → LIMITED
    --   acc2 Tulip Hotel 2 Dalat:  totalAvail=16, totalRooms=16 → AVAILABLE
    -- =============================================

    -- =============================================
    -- CALENDAR RICHNESS DATA — Trải đều tháng 5 & 6 năm 2026
    -- Mục đích: làm lịch trống trực quan với nhiều màu sắc đa dạng
    -- Các booking dưới đây dùng CURDATE() nên chạy được trên mọi máy
    -- =============================================

    -- ─── Tuần +15→+20 (LATA-STD r1, còn 5 phòng → chiếm 3) ─────────────────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status, note, created_at, updated_at)
    VALUES (
        'BK-LATA-STD-CAL01', 5, 1, 1,
        DATE_ADD(CURDATE(), INTERVAL 15 DAY), DATE_ADD(CURDATE(), INTERVAL 18 DAY),
        4, 1, 3, 'Nguyễn Tuấn Anh', '0912 111 222', 'user2@travelmate.vn',
        5850000, 5850000, 0, 'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Lịch giữ phòng +15→+18.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 5850000, CONCAT('TXN-CAL01-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'Lịch giữ phòng +15→+18');
    UPDATE rooms SET available_quantity = available_quantity - 3 WHERE id = 1;

    -- ─── Tuần +18→+22 (LATA-DLX r2 — chiếm 1 phòng) ─────────────────────────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status, note, created_at, updated_at)
    VALUES (
        'BK-LATA-DLX-CAL01', 6, 1, 2,
        DATE_ADD(CURDATE(), INTERVAL 18 DAY), DATE_ADD(CURDATE(), INTERVAL 22 DAY),
        2, 0, 1, 'Lê Văn Minh', '0933 444 555', 'user3@travelmate.vn',
        3400000, 1020000, 2380000, 'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'Lịch giữ phòng +18→+22.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 1020000, CONCAT('TXN-CAL02-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'Lịch giữ phòng +18→+22');

    -- ─── Tuần +20→+24 (TMG-DLX r9 — chiếm 4 phòng → LIMITED) ────────────────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status, note, created_at, updated_at)
    VALUES (
        'BK-TMG-DLX-CAL01', 5, 3, 9,
        DATE_ADD(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 24 DAY),
        6, 2, 4, 'Trần Thị Lan', '0944 666 777', 'user2@travelmate.vn',
        19200000, 19200000, 0, 'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Lịch giữ phòng TMG +20→+24.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 19200000, CONCAT('TXN-CAL03-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'Lịch giữ phòng TMG +20→+24');
    UPDATE rooms SET available_quantity = available_quantity - 4 WHERE id = 9;

    -- ─── Tuần +22→+26 (VNT-DLX r25 — chiếm 5 phòng → LIMITED) ──────────────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status, note, created_at, updated_at)
    VALUES (
        'BK-VNT-DLX-CAL01', 6, 8, 25,
        DATE_ADD(CURDATE(), INTERVAL 22 DAY), DATE_ADD(CURDATE(), INTERVAL 26 DAY),
        8, 2, 5, 'Phạm Hoàng Nam', '0955 888 999', 'user3@travelmate.vn',
        56000000, 56000000, 0, 'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Calendar Vinpearl +22→+26.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 56000000, CONCAT('TXN-CAL04-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'Cal Vinpearl +22→+26');
    UPDATE rooms SET available_quantity = available_quantity - 5 WHERE id = 25;

    -- ─── Tuần +25→+30 (ANM-GDN r13 — chiếm 2 phòng → LIMITED) ──────────────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status, note, created_at, updated_at)
    VALUES (
        'BK-ANM-GDN-CAL01', 5, 4, 13,
        DATE_ADD(CURDATE(), INTERVAL 25 DAY), DATE_ADD(CURDATE(), INTERVAL 30 DAY),
        4, 0, 2, 'Nguyễn Minh Khoa', '0966 000 111', 'user2@travelmate.vn',
        35000000, 10500000, 24500000, 'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'Anam Villa calendar +25→+30.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 10500000, CONCAT('TXN-CAL05-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'Cal Anam +25→+30');

    -- ─── Tuần +28→+33 (BNH-BNG r16 — chiếm 2 phòng → LIMITED) ──────────────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status, note, created_at, updated_at)
    VALUES (
        'BK-BNH-BNG-CAL01', 6, 5, 16,
        DATE_ADD(CURDATE(), INTERVAL 28 DAY), DATE_ADD(CURDATE(), INTERVAL 33 DAY),
        3, 1, 2, 'Đỗ Thị Hương', '0977 222 333', 'user3@travelmate.vn',
        22000000, 22000000, 0, 'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Ba Na Villa calendar +28→+33.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 22000000, CONCAT('TXN-CAL06-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'Cal Ba Na +28→+33');
    UPDATE rooms SET available_quantity = available_quantity - 2 WHERE id = 16;

    -- ─── Tuần +30→+35 (MND-STD r22 — chiếm 3 phòng → LIMITED) ─────────────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status, note, created_at, updated_at)
    VALUES (
        'BK-MND-STD-CAL01', 5, 7, 22,
        DATE_ADD(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 35 DAY),
        4, 1, 3, 'Vũ Anh Khoa', '0988 444 555', 'user2@travelmate.vn',
        5850000, 1755000, 4095000, 'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'Mộc Nhiên calendar +30→+35.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 1755000, CONCAT('TXN-CAL07-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'Cal Mộc Nhiên +30→+35');
    UPDATE rooms SET available_quantity = available_quantity - 3 WHERE id = 22;

    -- ─── Tuần +33→+38 (FDN-BCH r29 — chiếm 3 phòng → LIMITED) ─────────────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status, note, created_at, updated_at)
    VALUES (
        'BK-FDN-BCH-CAL01', 6, 9, 29,
        DATE_ADD(CURDATE(), INTERVAL 33 DAY), DATE_ADD(CURDATE(), INTERVAL 38 DAY),
        5, 1, 3, 'Ngô Thanh Sơn', '0999 666 777', 'user3@travelmate.vn',
        54000000, 54000000, 0, 'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'Furama Beachfront calendar +33→+38.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 54000000, CONCAT('TXN-CAL08-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'Cal Furama BCH +33→+38');
    UPDATE rooms SET available_quantity = available_quantity - 3 WHERE id = 29;

    -- ─── Tuần +37→+42 (HLR-FAM r21 — chiếm 2 phòng → FULL vì total=2) ───────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status, note, created_at, updated_at)
    VALUES (
        'BK-HLR-FAM-CAL01', 5, 6, 21,
        DATE_ADD(CURDATE(), INTERVAL 37 DAY), DATE_ADD(CURDATE(), INTERVAL 42 DAY),
        4, 1, 2, 'Đinh Hoàng Long', '0911 888 999', 'user2@travelmate.vn',
        7500000, 2250000, 5250000, 'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'Hoa Lư Family FULL +37→+42.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 2250000, CONCAT('TXN-CAL09-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'Cal Hoa Lư FAM +37→+42');
    UPDATE rooms SET available_quantity = available_quantity - 2 WHERE id = 21;

    -- ─── Tháng sau +40→+45 (VNT-VIL r27 — chiếm 1 phòng → LIMITED) ──────────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status, note, created_at, updated_at)
    VALUES (
        'BK-VNT-VIL-CAL01', 6, 8, 27,
        DATE_ADD(CURDATE(), INTERVAL 40 DAY), DATE_ADD(CURDATE(), INTERVAL 45 DAY),
        4, 0, 1, 'Lâm Thanh Hà', '0922 000 111', 'user3@travelmate.vn',
        49000000, 14700000, 34300000, 'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'Vinpearl Pool Villa tháng tới — VNPAY ghi nhận cọc 30%, TravelMate tự động giữ villa.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 14700000, CONCAT('TXN-CAL10-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'Lịch Vinpearl VIL +40→+45 — VNPAY ghi nhận thành công, TravelMate tự động giữ villa.');

    -- =============================================
    -- KỊCH BẢN LUỒNG PARTNER — CHECK-IN / CHECK-OUT / NO-SHOW
    -- Thêm các tình huống trực quan để thể hiện đúng nghiệp vụ Partner
    -- Partner thực hiện: Check-in → Check-out → No-show
    -- =============================================

    -- ─── P1A: Tulip Hotel — FULL_PAYMENT — check-in HÔM NAY ────
    -- → partner@travelmate.vn vào Partner > Đơn đặt phòng → nhấn "Check-in khách"
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-TLP-FAM-OPS01', 2, 2, 7,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 DAY),
        2, 1, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        2100000, 2100000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED',
        'Tình huống check-in: khách đến hôm nay — Partner nhấn Check-in để xác nhận.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, approved_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 2100000,
        CONCAT('TXN-OPS-P1-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), NOW(),
        'Thanh toán 100% — Partner check-in hôm nay');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 7;

    -- ─── P1B: Tulip Hotel — DEPOSIT_30 — check-in HÔM NAY ────
    -- → Demo phần cọc 30%: Partner mở modal Check-in, tick "Đã thu đủ 70% tại cơ sở" rồi check-in.
    -- → Dùng CURDATE() để import trên máy nào/ngày nào cũng có đơn cọc sẵn sàng thao tác.
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, remaining_payment_status, note, created_at, updated_at)
    VALUES (
        'BK-TLP-VIP-OPS02', 5, 2, 8,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 1 DAY),
        2, 0, 1,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        1500000, 450000, 1050000,
        'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED', 'UNPAID',
        'Tình huống demo cọc 30%: khách check-in hôm nay, Partner xác nhận đã thu 70% tại cơ sở trước khi check-in.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, approved_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 450000,
        CONCAT('TXN-OPS-P1B-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), NOW(),
        'Cọc 30% qua TravelMate — Partner thu 70% tại cơ sở khi check-in');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 8;

    -- ─── P2: Mộc Nhiên Homestay — CHECKED_IN + PARTNER_CONFIRMED — chờ CHECK-OUT ─
    -- → partner4@travelmate.vn vào Partner > Đơn đặt phòng → nhấn "Check-out / Hoàn tất"
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-MND-STD-OPS01', 5, 7, 22,
        DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 1 DAY),
        2, 0, 1,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        780000, 780000, 0,
        'CHECKED_IN', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED',
        '[Partner] Khách đã đến nhận phòng hôm qua. Check-in lúc 14:00 hôm qua. Partner nhấn Check-out hôm nay.',
        DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, approved_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 780000,
        CONCAT('TXN-OPS-P2-', UNIX_TIMESTAMP()), 'APPROVED', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY),
        'Thanh toán 100% Mộc Nhiên — đang lưu trú, chờ check-out');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 22;

    -- ─── P3: Anam Villa — CONFIRMED + PARTNER_CONFIRMED — check-in NGÀY MAI — tình huống NO-SHOW ─
    -- → partner3@travelmate.vn: chuẩn bị lịch đến ngày mai; chỉ báo No-show khi đã tới ngày nhận căn và khách không đến
    -- → Cọc 30% → khách sẽ mất cọc nếu no-show
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-ANM-GDN-OPS01', 6, 4, 13,
        DATE_ADD(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 3 DAY),
        2, 0, 1,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        7000000, 2100000, 4900000,
        'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED',
        'Tình huống no-show: check-in ngày mai, cọc 30% = 2.100.000đ. Partner chỉ báo No-show khi đã tới ngày nhận căn và khách không đến.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, approved_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'DEPOSIT_30', 2100000,
        CONCAT('TXN-OPS-P3-', UNIX_TIMESTAMP()), 'APPROVED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 12 HOUR),
        'Cọc 30% Anam Garden Villa — check-in ngày mai');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 13;

    -- ─── P4: Vinpearl Resort — CONFIRMED + PARTNER_CONFIRMED ──────────
    -- → partner2@travelmate.vn có thể check-in khi khách đến
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-VNT-DLX-OPS01', 2, 8, 25,
        DATE_ADD(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY),
        2, 0, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        8400000, 8400000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'VNPAY đã ghi nhận thanh toán. TravelMate tự động giữ phòng.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, approved_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY', 'FULL_PAYMENT', 8400000,
        CONCAT('TXN-OPS-P4-', UNIX_TIMESTAMP()), 'APPROVED', DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR),
        'VNPAY ghi nhận thành công — Thanh toán 100% Vinpearl Deluxe Ocean View, TravelMate tự động giữ phòng');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 25;

    -- Nhật ký thao tác cho các booking tình huống trên
    INSERT INTO admin_action_logs (admin_email, action_type, target_type, target_id, description, note, created_at) VALUES
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', NULL,
    'Xác nhận booking BK-TLP-FAM-OPS01 — Nguyễn Văn An check-in hôm nay (luồng partner check-in)',
    'Đơn vận hành minh họa luồng Partner check-in.', DATE_SUB(NOW(), INTERVAL 4 HOUR)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', NULL,
    'Xác nhận booking BK-TLP-VIP-OPS02 — Trần Thị Mai check-in hôm nay, đơn cọc 30%',
    'Đơn vận hành minh họa luồng Partner check-in kèm xác nhận thu 70% tại cơ sở.', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', NULL,
    'Xác nhận booking BK-MND-STD-OPS01 — Trần Thị Mai đã check-in hôm qua (luồng partner check-out)',
    'Đơn vận hành minh họa luồng Partner check-out.', DATE_SUB(NOW(), INTERVAL 25 HOUR)),
    ('partner4@travelmate.vn', 'PARTNER_CONFIRM_HOLD', 'BOOKING', NULL,
    'Partner giữ phòng BK-MND-STD-OPS01 — Mộc Nhiên Homestay', NULL, DATE_SUB(NOW(), INTERVAL 24 HOUR)),
    ('partner4@travelmate.vn', 'PARTNER_CHECK_IN', 'BOOKING', NULL,
    'Partner check-in khách BK-MND-STD-OPS01 — Trần Thị Mai', 'Khách đến lúc 14:00.', DATE_SUB(NOW(), INTERVAL 23 HOUR)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', NULL,
    'Xác nhận booking BK-ANM-GDN-OPS01 — Lê Văn Đức check-in ngày mai (luồng no-show)',
    'Đơn vận hành minh họa luồng Partner báo no-show.', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', NULL,
    'Xác nhận booking BK-VNT-DLX-OPS01 — Nguyễn Văn An (luồng partner check-in)',
    'Đơn vận hành minh họa phòng đã được TravelMate giữ sau thanh toán.', DATE_SUB(NOW(), INTERVAL 2 HOUR));

    -- =============================================
    -- HỆ THỐNG ĐÁNH GIÁ TRAVELMATE — Dual Rating System
    -- =============================================
    -- star_rating (1–5★): Hạng / tiêu chuẩn cơ sở lưu trú
    --   → Áp dụng cho TẤT CẢ loại: HOTEL / VILLA / HOMESTAY / RESORT
    --   → Partner khai báo khi đăng ký listing, Admin xác minh khi duyệt
    --   → Không thay đổi theo review của khách — phản ánh cơ sở vật chất & dịch vụ
    --
    -- rating (0–10): Điểm hài lòng trung bình tính từ user reviews
    --   → Công thức: avg_review_score (vd: review 10, 8, 6 → trung bình trực tiếp trên thang 10)
    --   → Chỉ user có booking COMPLETED mới được viết review
    --   → Cập nhật sau mỗi lần user gửi đánh giá mới
    --
    -- Ví dụ hiển thị trên UI:
    --   LATA Hotel & Apartments
    --   🏨 Hotel  ⭐⭐⭐⭐  (4 sao — hạng cơ sở lưu trú)
    --   9.3/10 Xuất sắc  ·  3 đánh giá  (điểm từ khách đã ở)
    --
    -- Bộ lọc tìm kiếm (2 trục độc lập):
    --   Lọc sao   : ⭐⭐⭐⭐⭐ 5 sao | ⭐⭐⭐⭐ 4 sao trở lên | ⭐⭐⭐ 3 sao trở lên
    --   Lọc điểm  : 9+ Tuyệt vời | 8+ Rất tốt | 7+ Tốt
    --
    -- PHÂN BỐ ĐIỂM MỤC TIÊU (sau import đầy đủ v8):
    --   7.0–7.9 (Tốt)     :  acc2  Tulip Hotel       3★ → 7.5/10   (4 reviews: 10+8+6+6)
    --   8.0–8.9 (Rất tốt) :  acc5  Ba Na Hills Villa 4★ → 8.7/10   (3 reviews)
    --                         acc7  Mộc Nhiên HS      3★ → 8.0/10   (4 reviews: 8+8+10+6)
    --   9.0–9.9 (Xuất sắc):  acc1  LATA Hotel        4★ → 9.3/10   (3 reviews)
    --                         acc8  Vinpearl Resort   5★ → 9.0/10   (2 reviews)
    --   10.0    (Hoàn hảo):  acc3  TM Grand Hotel     5★ → 10.0/10
    --                         acc4  Anam Villa         5★ → 10.0/10
    --                         acc6  Hoa Lư Homestay    3★ → 10.0/10
    --                         acc9  Furama Resort      5★ → 10.0/10
    --
    -- → Lọc "7+ Tốt"     hiển thị: acc2 (7.5) + tất cả 8.x/9.x/10.0
    -- → Lọc "8+ Rất tốt" hiển thị: acc5, acc7, acc1, acc8 + 10.0 (bỏ acc2)
    -- → Lọc "9+ Tuyệt vời" hiển thị: acc1(9.3), acc8(9.0) + các 10.0 (bỏ 7.x/8.x)
    -- =============================================

    -- =============================================
    -- DỮ LIỆU BỔ SUNG v8 — PHỔ ĐIỂM ĐÁNH GIÁ (SCORE DIVERSITY)
    -- Thêm 3 booking COMPLETED + 3 review 6/10 để kéo acc2 xuống 7.5
    -- và acc7 xuống 8.0 — tạo phổ điểm rõ ràng cho bộ lọc
    -- =============================================

    -- ─── Booking EXT01: Tulip TLP-FAM (r7, acc2, partner1), user3, 2 đêm ───
    -- Mục đích: thêm review 6/10 để Tulip Hotel có điểm 7.5 (7+ tốt filter)
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, created_at, updated_at)
    VALUES (
        'BK-TLP-FAM-EXT01', 6, 2, 7,
        DATE_SUB(CURDATE(), INTERVAL 87 DAY), DATE_SUB(CURDATE(), INTERVAL 85 DAY),
        3, 0, 1,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        2100000, 2100000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'Tulip Family Room 2 đêm, khách đánh giá trung bình sau lưu trú.', DATE_SUB(NOW(), INTERVAL 89 DAY), DATE_SUB(NOW(), INTERVAL 89 DAY)
    );
    SET @ext01 = LAST_INSERT_ID();
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, approved_at, note)
    VALUES (@ext01, 'VNPAY', 'FULL_PAYMENT', 2100000,
        CONCAT('TXN-TLP-FAM-EXT1-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 89 DAY), DATE_SUB(NOW(), INTERVAL 89 DAY), 'Thanh toán TLP-FAM 2 đêm đã ghi nhận');

    -- ─── Booking EXT02: Tulip TLP-VIP (r8, acc2, partner1), user2, 2 đêm ───
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, created_at, updated_at)
    VALUES (
        'BK-TLP-VIP-EXT01', 5, 2, 8,
        DATE_SUB(CURDATE(), INTERVAL 93 DAY), DATE_SUB(CURDATE(), INTERVAL 91 DAY),
        2, 0, 1,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        3000000, 3000000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'Tulip VIP Panorama 2 đêm, khách đánh giá trung bình sau lưu trú.', DATE_SUB(NOW(), INTERVAL 95 DAY), DATE_SUB(NOW(), INTERVAL 95 DAY)
    );
    SET @ext02 = LAST_INSERT_ID();
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, approved_at, note)
    VALUES (@ext02, 'VNPAY', 'FULL_PAYMENT', 3000000,
        CONCAT('TXN-TLP-VIP-EXT1-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 95 DAY), DATE_SUB(NOW(), INTERVAL 95 DAY), 'Thanh toán TLP-VIP 2 đêm đã ghi nhận');

    -- ─── Booking EXT03: Mộc Nhiên MND-STD (r22, acc7, partner4), user3, 2 đêm ───
    -- Mục đích: thêm review 6/10 để Mộc Nhiên có điểm 8.0 (thay vì 8.7)
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, created_at, updated_at)
    VALUES (
        'BK-MND-STD-EXT01', 6, 7, 22,
        DATE_SUB(CURDATE(), INTERVAL 84 DAY), DATE_SUB(CURDATE(), INTERVAL 82 DAY),
        2, 0, 1,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        780000, 780000, 0,
        'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'Mộc Nhiên Standard 2 đêm, khách đánh giá trung bình sau lưu trú.', DATE_SUB(NOW(), INTERVAL 86 DAY), DATE_SUB(NOW(), INTERVAL 86 DAY)
    );
    SET @ext03 = LAST_INSERT_ID();
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, approved_at, note)
    VALUES (@ext03, 'VNPAY', 'FULL_PAYMENT', 780000,
        CONCAT('TXN-MND-STD-EXT1-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 86 DAY), DATE_SUB(NOW(), INTERVAL 86 DAY), 'Thanh toán MND-STD 2 đêm đã ghi nhận');

    -- =============================================
    -- BOOKINGS KIỂM TRA v11 — DEPOSIT_30 + COMPLETED + PAID_AT_PROPERTY
    -- Mục đích: Minh hoạ luồng cọc 30% đầy đủ — khách lưu trú, trả 70% tại cơ sở.
    -- Quy tắc commission hiện hành: DEPOSIT_30 tính hoa hồng trên tổng đơn gốc,
    -- còn payout vẫn dựa trên số tiền TravelMate thực thu online.
    -- DEP01: LATA Suite  3 đêm → CK base = 5.400.000 (tổng đơn gốc) | HH 20% = 1.080.000
    -- DEP02: Anam Beach  2 đêm → CK base = 11.600.000 (tổng đơn gốc) | HH 15% = 1.740.000
    -- =============================================

    -- ─── DEP01: LATA Hotel — LATA-SUI (r4) — Lê Văn Đức — COMPLETED + DEPOSIT_30 ────────────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        remaining_payment_status, remaining_paid_at, remaining_payment_note,
        commission_rate_snapshot, commission_source_snapshot, commission_base_amount,
        commission_amount_snapshot, partner_voucher_amount_snapshot, admin_voucher_amount_snapshot,
        partner_payout_snapshot, online_paid_amount_snapshot, onsite_amount_snapshot,
        note, created_at, updated_at)
    VALUES (
        'BK-LATA-SUI-DEP01', 6, 1, 4,
        DATE_SUB(CURDATE(), INTERVAL 58 DAY), DATE_SUB(CURDATE(), INTERVAL 55 DAY),
        2, 0, 1,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        5400000, 1620000, 3780000,
        'COMPLETED', 'DEPOSIT_30', 'APPROVED', 'PARTNER_CONFIRMED',
        'PAID_AT_PROPERTY', DATE_SUB(NOW(), INTERVAL 55 DAY),
        'Khách thanh toán 70% còn lại (3.780.000đ) bằng tiền mặt tại quầy lễ tân khi check-out.',
        0.2000, 'ROOM_OVERRIDE', 5400000, 1080000, 0, 0, 540000, 1620000, 3780000,
        'DEPOSIT_30 COMPLETED + PAID_AT_PROPERTY — commission snapshot tính trên tổng đơn gốc 5.400.000đ (rate phòng Suite 20% = 1.080.000đ); payout từ cọc online 1.620.000đ.',
        DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 55 DAY)
    );
    SET @dep01 = LAST_INSERT_ID();
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, approved_at, note)
    VALUES (@dep01, 'VNPAY', 'DEPOSIT_30', 1620000,
        CONCAT('TXN-LATA-SUI-DEP01-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 59 DAY),
        'Cọc 30% LATA Suite — khách đã lưu trú, hoàn tất check-out');

    -- ─── DEP02: Anam Villa — ANM-BCH (r14) — Trần Thị Mai — COMPLETED + DEPOSIT_30 ───────────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        remaining_payment_status, remaining_paid_at, remaining_payment_note,
        commission_rate_snapshot, commission_source_snapshot, commission_base_amount,
        commission_amount_snapshot, partner_voucher_amount_snapshot, admin_voucher_amount_snapshot,
        partner_payout_snapshot, online_paid_amount_snapshot, onsite_amount_snapshot,
        note, created_at, updated_at)
    VALUES (
        'BK-ANM-BCH-DEP01', 5, 4, 14,
        DATE_SUB(CURDATE(), INTERVAL 50 DAY), DATE_SUB(CURDATE(), INTERVAL 48 DAY),
        2, 0, 1,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        11600000, 3480000, 8120000,
        'COMPLETED', 'DEPOSIT_30', 'APPROVED', 'PARTNER_CONFIRMED',
        'PAID_AT_PROPERTY', DATE_SUB(NOW(), INTERVAL 48 DAY),
        'Khách thanh toán 70% còn lại (8.120.000đ) bằng thẻ tín dụng tại villa khi check-out.',
        0.1500, 'ROOM_OVERRIDE', 11600000, 1740000, 0, 0, 1740000, 3480000, 8120000,
        'DEPOSIT_30 COMPLETED + PAID_AT_PROPERTY — commission snapshot tính trên tổng đơn gốc 11.600.000đ (rate căn VIP 15% = 1.740.000đ); payout từ cọc online 3.480.000đ.',
        DATE_SUB(NOW(), INTERVAL 52 DAY), DATE_SUB(NOW(), INTERVAL 48 DAY)
    );
    SET @dep02 = LAST_INSERT_ID();
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, approved_at, note)
    VALUES (@dep02, 'VNPAY', 'DEPOSIT_30', 3480000,
        CONCAT('TXN-ANM-BCH-DEP01-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 52 DAY), DATE_SUB(NOW(), INTERVAL 51 DAY),
        'Cọc 30% Anam Beachfront Villa — khách đã lưu trú, hoàn tất check-out');

    -- =============================================
    -- REVIEWS BỔ SUNG v8 — EXT01 / EXT02 / EXT03 (đánh giá 6/10)
    -- =============================================

    -- EXT01 → acc2 Tulip Hotel (6 điểm, user3 Lê Văn Đức)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (6, 2, @ext01, 6,
    'Khách sạn ổn nhưng chưa đáp ứng kỳ vọng. Phòng Family hơi tối và nhỏ hơn ảnh chụp. Bữa sáng đơn điệu, chủ yếu là bánh mì và trứng chiên. Vị trí gần hồ Xuân Hương là điểm cộng duy nhất đáng kể. Nhân viên thân thiện nhưng quy trình check-in khá chậm so với mức giá phải trả.',
    DATE_SUB(NOW(), INTERVAL 83 DAY));

    -- EXT02 → acc2 Tulip Hotel (6 điểm, user2 Trần Thị Mai)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (5, 2, @ext02, 6,
    'Phòng VIP Panorama thực tế không đẹp như quảng cáo — ban công hẹp, view bị che bởi tòa nhà bên cạnh. WiFi yếu vào giờ cao điểm buổi tối. Điều hoà có tiếng ồn lớn, khó ngủ. Giá cả chưa tương xứng với chất lượng nhận được, cần cải thiện nhiều hơn.',
    DATE_SUB(NOW(), INTERVAL 89 DAY));

    -- EXT03 → acc7 Mộc Nhiên Homestay (6 điểm, user3 Lê Văn Đức)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (6, 7, @ext03, 6,
    'Không gian nhà gỗ Đà Lạt đẹp mắt nhưng tiện nghi còn hạn chế. Nước nóng hay bị yếu, cửa sổ không kín gió nên lạnh vào ban đêm. Chủ nhà thân thiện nhưng ít có mặt để hỗ trợ. Bữa sáng ngon nhưng phần ăn nhỏ. Cần đầu tư thêm vào cơ sở vật chất để xứng với giá.',
    DATE_SUB(NOW(), INTERVAL 80 DAY));

    -- Cập nhật rating + review_count sau khi thêm 3 review 6/10
    -- acc2 Tulip Hotel: 4 reviews (10+8+6+6) → avg = 7.5
    UPDATE accommodations SET rating = 7.5, review_count = 4 WHERE id = 2;
    -- acc7 Mộc Nhiên Homestay: 4 reviews (8+8+10+6) → avg = 8.0
    UPDATE accommodations SET rating = 8.0, review_count = 4 WHERE id = 7;

    -- =============================================
    -- KIỂM TRA PHÂN BỐ ĐIỂM SAU IMPORT v8
    -- SELECT id, name, star_rating, rating, review_count
    --   FROM accommodations WHERE approval_status = 'APPROVED' ORDER BY rating DESC;
    -- Kỳ vọng:
    --   acc3  TM Grand Hotel       5★  10.0/10  1 review
    --   acc4  Anam Villa           5★  10.0/10  2 reviews
    --   acc6  Hoa Lư Homestay      3★  10.0/10  3 reviews
    --   acc9  Furama Resort        5★  10.0/10  1 review
    --   acc1  LATA Hotel           4★   9.3/10  3 reviews
    --   acc8  Vinpearl Resort      5★   9.0/10  2 reviews
    --   acc5  Ba Na Hills Villa    4★   8.7/10  3 reviews
    --   acc7  Mộc Nhiên Homestay   3★   8.0/10  4 reviews  ← 8+ filter thấy
    --   acc2  Tulip Hotel          3★   7.5/10  4 reviews  ← 7+ filter thấy, 8+ không thấy
    --
    -- Kiểm tra bộ lọc điểm đánh giá:
    --   Filter 9+ "Tuyệt vời"  → 4 kết quả (acc1, acc3, acc4, acc6, acc8, acc9 = 6 nếu 10.0 tính)
    --   Filter 8+ "Rất tốt"    → 8 kết quả (thêm acc5, acc7 — bỏ acc2)
    --   Filter 7+ "Tốt"        → 9 kết quả (tất cả acc đã duyệt, kể cả acc2)
    --   Không lọc             → 9 kết quả + 2 pending/rejected
    --
    -- Kiểm tra bộ lọc sao:
    --   Filter 5★              → 4 kết quả (acc3, acc4, acc8, acc9)
    --   Filter 4★ trở lên      → 6 kết quả (+ acc1, acc5)
    --   Filter 3★ trở lên      → 9 kết quả (tất cả 9 APPROVED)
    -- =============================================

    -- =============================================
    -- KIỂM TRA KỲ VỌNG CUỐI CÙNG (sau import đầy đủ)
    -- SELECT COUNT(*) FROM users;               -- kỳ vọng: 8 (1 admin, 3 user, 4 partner)
    -- SELECT COUNT(*) FROM accommodations;      -- kỳ vọng: 26 (đủ HOTEL/RESORT/VILLA/HOMESTAY + PENDING/REJECTED)
    -- SELECT COUNT(*) FROM rooms;               -- kỳ vọng: 64
    -- SELECT COUNT(*) FROM bookings;            -- kỳ vọng: 60+ (đủ ONLINE/DIRECT/MANUAL_BLOCK và các trạng thái thanh toán)
    -- SELECT COUNT(*) FROM payments;            -- kỳ vọng: 50+ (đủ APPROVED/PENDING/CANCELLED/FAILED/EXPIRED/REFUND)
    -- SELECT COUNT(*) FROM reviews;             -- kỳ vọng: 20+
    -- SELECT COUNT(*) FROM vouchers;            -- kỳ vọng: 22
    -- SELECT COUNT(*) FROM amenities;           -- kỳ vọng: 32
    -- SELECT COUNT(DISTINCT room_id) FROM room_amenities; -- kỳ vọng: 64 phòng/căn đều có tiện nghi
    -- SELECT COUNT(*) FROM partner_settlements; -- kỳ vọng: có dữ liệu theo 4 partner
    -- SELECT COUNT(*) FROM admin_action_logs;   -- kỳ vọng: 100+ (phủ BOOKING, ACCOMMODATION, ROOM, USER, REVIEW, VOUCHER, SETTLEMENT, TICKET)
    -- SELECT COUNT(*) FROM notifications;       -- kỳ vọng: có thông báo cho User/Admin/Partner
    --
    -- Kiểm tra phổ điểm rating:
    -- SELECT id, name, star_rating AS sao, rating AS diem, review_count AS so_review
    --   FROM accommodations WHERE approval_status='APPROVED' ORDER BY rating DESC;
    -- =============================================

    -- =============================================
    -- DỮ LIỆU THÔNG BÁO v10 — hệ thống thông báo đầy đủ
    -- =============================================
    -- user@travelmate.vn  (id=2, Nguyễn Văn An)  → 9 thông báo (6 chưa đọc: checkin×2 + review×4)
    -- user2@travelmate.vn (id=5, Trần Thị Mai)    → 6 thông báo (4 chưa đọc: checkin×1 + review×3)
    -- user3@travelmate.vn (id=6, Lê Văn Đức)      → 6 thông báo (4 chưa đọc: checkin×1 + review×3)
    -- Mục đích: tài khoản trình bày thấy thông báo đủ các loại nghiệp vụ
    -- =============================================

    INSERT INTO notifications (user_id, title, message, type, target_url, is_read, created_at) VALUES

    -- ════════════════════════════════════════════════════════════════════════════
    -- user@travelmate.vn (id=2) — Nguyễn Văn An
    -- 6 chưa đọc + 3 đã đọc = 9 tổng
    -- ════════════════════════════════════════════════════════════════════════════

    -- [UNREAD] BOOKING_CHECKIN_READY: BK-TMG-DLX-0001 — check-in NGÀY MAI (acc3 TM Grand)
    (2,
     'Check-in ngày mai — TravelMate Grand Hotel',
     'Nhắc nhở: Booking BK-TMG-DLX-0001 tại TravelMate Grand Hotel sẽ check-in vào ngày mai. Mang theo CCCD/Passport và số điện thoại đặt phòng để làm thủ tục nhanh!',
     'BOOKING_CHECKIN_READY', '/my-bookings', 0,
     DATE_SUB(NOW(), INTERVAL 1 HOUR)),

    -- [UNREAD] BOOKING_CHECKIN_READY: BK-LATA-DLX-0001 — check-in 2 ngày nữa (acc1 LATA)
    (2,
     'Sắp đến ngày check-in — LATA Hotel & Apartments',
     'Booking BK-LATA-DLX-0001 tại LATA Hotel & Apartments: còn 2 ngày nữa là đến ngày nhận phòng. Chuẩn bị hành lý và lịch trình của bạn nhé!',
     'BOOKING_CHECKIN_READY', '/my-bookings', 0,
     DATE_SUB(NOW(), INTERVAL 2 HOUR)),

    -- [UNREAD] REVIEW_REMINDER: BK-LATA-FAM-0001 (COMPLETED, acc1 LATA Hotel)
    (2,
     'Hãy đánh giá kỳ nghỉ tại LATA Hotel & Apartments',
     'Bạn vừa hoàn thành kỳ lưu trú tại LATA Hotel & Apartments. Hãy chia sẻ trải nghiệm của bạn để giúp những khách du lịch khác!',
     'REVIEW_REMINDER', '/my-bookings?tab=COMPLETED', 0,
     DATE_SUB(NOW(), INTERVAL 7 DAY)),

    -- [UNREAD] REVIEW_REMINDER: BK-MND-ATT-0001 (COMPLETED, acc7 Mộc Nhiên Homestay)
    (2,
     'Hãy đánh giá kỳ nghỉ tại Mộc Nhiên Garden Homestay',
     'Chuyến lưu trú tại Mộc Nhiên Garden Homestay Đà Lạt đã kết thúc. Đánh giá của bạn rất có giá trị với cộng đồng TravelMate!',
     'REVIEW_REMINDER', '/my-bookings?tab=COMPLETED', 0,
     DATE_SUB(NOW(), INTERVAL 5 DAY)),

    -- [UNREAD] REVIEW_REMINDER: BK-MND-FAM-0001 (COMPLETED, acc7 Mộc Nhiên — bk#32)
    (2,
     'Cảm nhận của bạn về Mộc Nhiên Garden Homestay quan trọng với chúng tôi!',
     'Phòng Family Rooftop Garden tại Mộc Nhiên đã hoàn tất. Hàng nghìn khách sẽ đọc đánh giá của bạn trước khi quyết định đặt phòng. Hãy chia sẻ nhé!',
     'REVIEW_REMINDER', '/my-bookings?tab=COMPLETED', 0,
     DATE_SUB(NOW(), INTERVAL 36 DAY)),

    -- [UNREAD] REVIEW_REMINDER: BK-HLR-STD-0001 (COMPLETED, acc6 Hoa Lư Homestay — bk#27)
    (2,
     'Hãy đánh giá kỳ nghỉ tại Hoa Lư Riverside Homestay',
     'Kỳ lưu trú tại Hoa Lư Riverside Homestay Hội An đã kết thúc từ lâu nhưng bạn chưa để lại đánh giá. Chỉ mất 2 phút — giúp các du khách tiếp theo nhé!',
     'REVIEW_REMINDER', '/my-bookings?tab=COMPLETED', 0,
     DATE_SUB(NOW(), INTERVAL 67 DAY)),

    -- [READ] BOOKING_CONFIRMED: BK-VNT-DLX-0001 (acc8 Vinpearl — check-in +5 ngày)
    (2,
     'Đặt phòng đã được ghi nhận — Vinpearl Resort Nha Trang',
     'Booking BK-VNT-DLX-0001 tại Vinpearl Resort & Spa Nha Trang đã được TravelMate ghi nhận. Check-in trong 5 ngày tới. Chúc bạn có chuyến nghỉ dưỡng tuyệt vời!',
     'BOOKING_CONFIRMED', '/my-bookings', 1,
     DATE_SUB(NOW(), INTERVAL 4 DAY)),

    -- [READ] BOOKING_CONFIRMED: BK-LATA-STD-0002 (acc1 LATA)
    (2,
     'Đặt phòng đã được ghi nhận — LATA Hotel & Apartments',
     'Booking BK-LATA-STD-0002 tại LATA Hotel & Apartments đã được TravelMate ghi nhận. Chúc bạn có chuyến đi vui vẻ!',
     'BOOKING_CONFIRMED', '/my-bookings', 1,
     DATE_SUB(NOW(), INTERVAL 17 DAY)),

    -- [READ] SYSTEM: Thông báo hệ thống TravelMate
    (2,
     'TravelMate ra mắt tính năng Voucher cho Partner',
     'Từ hôm nay, Admin phát hành kho voucher ưu đãi; đối tác có thể chọn gắn voucher được phép vào phòng/căn của mình. Khách hàng sẽ thấy voucher phù hợp khi đặt phòng.',
     'SYSTEM', '/accommodations', 1,
     DATE_SUB(NOW(), INTERVAL 20 DAY)),

    -- ════════════════════════════════════════════════════════════════════════════
    -- user2@travelmate.vn (id=5) — Trần Thị Mai
    -- 4 chưa đọc + 2 đã đọc = 6 tổng
    -- ════════════════════════════════════════════════════════════════════════════

    -- [UNREAD] BOOKING_CHECKIN_READY: BK-MND-ATT-0002 — check-in 3 ngày nữa (acc7 Mộc Nhiên)
    (5,
     'Sắp đến ngày check-in — Mộc Nhiên Garden Homestay',
     'Booking BK-MND-ATT-0002 tại Mộc Nhiên Garden Homestay Đà Lạt: còn 3 ngày nữa là đến ngày nhận phòng. Thời tiết Đà Lạt mát mẻ, nhớ mang áo ấm!',
     'BOOKING_CHECKIN_READY', '/my-bookings', 0,
     DATE_SUB(NOW(), INTERVAL 3 HOUR)),

    -- [UNREAD] REVIEW_REMINDER: BK-ANM-BCH-0001 (COMPLETED, acc4 The Anam Villa — bk#29)
    (5,
     'Hãy đánh giá kỳ nghỉ tại The Anam Villa Nha Trang',
     'Kỳ lưu trú 3 đêm tại Beachfront Pool Villa — The Anam đã kết thúc. Chia sẻ cảm nhận về villa hạng sang để giúp cộng đồng lựa chọn tốt hơn nhé!',
     'REVIEW_REMINDER', '/my-bookings?tab=COMPLETED', 0,
     DATE_SUB(NOW(), INTERVAL 15 DAY)),

    -- [UNREAD] REVIEW_REMINDER: BK-HLR-FAM-0001 (COMPLETED, acc6 Hoa Lư Homestay — bk#31)
    (5,
     'Hãy đánh giá kỳ nghỉ tại Hoa Lư Riverside Homestay',
     'Chuyến du lịch tại Hoa Lư Riverside Homestay Hội An đã kết thúc. Phòng gia đình bên sông Thu Bồn — chia sẻ trải nghiệm của bạn nhé!',
     'REVIEW_REMINDER', '/my-bookings?tab=COMPLETED', 0,
     DATE_SUB(NOW(), INTERVAL 21 DAY)),

    -- [UNREAD] REVIEW_REMINDER: BK-TLP-STD-0002 (COMPLETED, acc2 Tulip Hotel)
    (5,
     'Hãy đánh giá kỳ nghỉ tại Tulip Hotel 2 Đà Lạt',
     'Kỳ lưu trú tại Tulip Hotel 2 Đà Lạt của bạn đã kết thúc. Chia sẻ cảm nhận để giúp cộng đồng lựa chọn tốt hơn nhé!',
     'REVIEW_REMINDER', '/my-bookings?tab=COMPLETED', 0,
     DATE_SUB(NOW(), INTERVAL 39 DAY)),

    -- [READ] BOOKING_CONFIRMED: BK-MND-ATT-0002 (acc7 Mộc Nhiên vừa ghi nhận)
    (5,
     'Đặt phòng đã được ghi nhận — Mộc Nhiên Garden Homestay',
     'Booking BK-MND-ATT-0002 tại Mộc Nhiên Garden Homestay Đà Lạt đã được TravelMate ghi nhận. Check-in trong 3 ngày tới. Chúc bạn có chuyến đi vui vẻ!',
     'BOOKING_CONFIRMED', '/my-bookings', 1,
     DATE_SUB(NOW(), INTERVAL 1 DAY)),

    -- [READ] SYSTEM: Thông báo chính sách hoàn tiền cọc
    (5,
     'Cập nhật chính sách huỷ phòng & hoàn tiền cọc',
     'TravelMate đã cập nhật chính sách: Huỷ trước 72h = hoàn 100% cọc. Huỷ trong 24-72h = hoàn 50%. Huỷ trong 24h cuối = không hoàn cọc. Chi tiết tại trang Chính sách.',
     'SYSTEM', '/my-bookings', 1,
     DATE_SUB(NOW(), INTERVAL 14 DAY)),

    -- ════════════════════════════════════════════════════════════════════════════
    -- user3@travelmate.vn (id=6) — Lê Văn Đức
    -- 4 chưa đọc + 2 đã đọc = 6 tổng
    -- ════════════════════════════════════════════════════════════════════════════

    -- [UNREAD] BOOKING_CHECKIN_READY: BK-ANM-GDN-0003 — check-in 5 ngày nữa (acc4 Anam Villa)
    (6,
     'Sắp đến ngày check-in — The Anam Villa Nha Trang',
     'Booking BK-ANM-GDN-0003 tại The Anam Villa Nha Trang: còn 5 ngày nữa là đến ngày nhận phòng. Bãi biển Ninh Vân Bay đang chờ bạn — chuẩn bị đồ bơi và kem chống nắng nhé!',
     'BOOKING_CHECKIN_READY', '/my-bookings', 0,
     DATE_SUB(NOW(), INTERVAL 4 HOUR)),

    -- [UNREAD] REVIEW_REMINDER: BK-BNH-BNG-0002 (COMPLETED, acc5 Ba Na Hills — bk#25)
    (6,
     'Hãy đánh giá kỳ nghỉ tại Ba Na Hills Forest Villa',
     'Kỳ nghỉ dưỡng tại Forest Bungalow — Ba Na Hills Forest Villa Đà Nẵng đã kết thúc. Đánh giá của bạn giúp các du khách khác chọn được kỳ nghỉ tuyệt vời!',
     'REVIEW_REMINDER', '/my-bookings?tab=COMPLETED', 0,
     DATE_SUB(NOW(), INTERVAL 53 DAY)),

    -- [UNREAD] REVIEW_REMINDER: BK-HLR-DLX-0002 (COMPLETED, acc6 Hoa Lư — bk#28)
    (6,
     'Hãy đánh giá kỳ nghỉ tại Hoa Lư Riverside Homestay Hội An',
     'Chuyến lưu trú tại Hoa Lư Riverside Homestay Hội An đã kết thúc. Chia sẻ cảm nhận về phố cổ và homestay truyền thống để giúp du khách tiếp theo!',
     'REVIEW_REMINDER', '/my-bookings?tab=COMPLETED', 0,
     DATE_SUB(NOW(), INTERVAL 74 DAY)),

    -- [UNREAD] REVIEW_REMINDER: BK-BNH-SUI-0001 (COMPLETED, acc5 Ba Na Hills Treetop Suite — bk#30)
    (6,
     'Hãy đánh giá Treetop Suite tại Ba Na Hills Forest Villa',
     'Kỳ nghỉ tại Treetop Suite với voucher ANAM15 đã kết thúc. Bạn đã trải nghiệm bồn tắm ngoài trời và ban công 360° — hãy chia sẻ để người khác biết nhé!',
     'REVIEW_REMINDER', '/my-bookings?tab=COMPLETED', 0,
     DATE_SUB(NOW(), INTERVAL 28 DAY)),

    -- [READ] BOOKING_CONFIRMED: BK-ANM-GDN-0003 (acc4 Anam Villa vừa ghi nhận)
    (6,
     'Đặt phòng đã được ghi nhận — The Anam Villa Nha Trang',
     'Booking BK-ANM-GDN-0003 tại The Anam Villa Nha Trang đã được TravelMate ghi nhận. Check-in trong 5 ngày tới. Tận hưởng villa hạng sang!',
     'BOOKING_CONFIRMED', '/my-bookings', 1,
     DATE_SUB(NOW(), INTERVAL 2 DAY)),

    -- [READ] SYSTEM: Nhắc nhở mang giấy tờ tuỳ thân khi check-in
    (6,
     'Nhắc nhở: Mang theo CCCD/Hộ chiếu khi check-in',
     'Theo quy định mới của Bộ Công An, tất cả khách lưu trú phải xuất trình CCCD hoặc Hộ chiếu khi làm thủ tục nhận phòng. Đảm bảo bạn mang đủ giấy tờ!',
     'SYSTEM', '/my-bookings', 1,
     DATE_SUB(NOW(), INTERVAL 10 DAY));

    -- Kiểm tra notifications v10:
    -- SELECT u.email, n.type, n.is_read, n.title
    --   FROM notifications n JOIN users u ON u.id = n.user_id ORDER BY u.id, n.created_at DESC;
    -- Kỳ vọng: 21 rows tổng
    --   user2 (id=2):  9 rows — 6 unread (badge=6), 3 read
    --   user5 (id=5):  6 rows — 4 unread (badge=4), 2 read
    --   user6 (id=6):  6 rows — 4 unread (badge=4), 2 read
    -- Mọi type đều có: REVIEW_REMINDER, BOOKING_CHECKIN_READY, BOOKING_CONFIRMED, SYSTEM

    -- (kết thúc dữ liệu thông báo đầy đủ)

    -- =============================================

    -- =============================================
    -- TRAVEL_POSTS — DỮ LIỆU KHỞI TẠO (CMS — admin quản lý nội dung du lịch)
    -- Danh muc: GUIDE | ATTRACTION | ESSENTIAL
    -- Trang thai: VISIBLE | HIDDEN
    -- =============================================
    CREATE TABLE IF NOT EXISTS travel_posts (
        id            BIGINT       NOT NULL AUTO_INCREMENT,
        title         VARCHAR(255) NOT NULL,
        destination_name VARCHAR(100),
        destination_slug VARCHAR(120),
        summary       TEXT,
        content       TEXT,
        thumbnail_url VARCHAR(500),
        source_name   VARCHAR(100),
        source_url    VARCHAR(500) NOT NULL,
        category      VARCHAR(20)  NOT NULL DEFAULT 'GUIDE',
        status        VARCHAR(10)  NOT NULL DEFAULT 'VISIBLE',
        created_by    VARCHAR(100),
        created_at    DATETIME(6),
        updated_at    DATETIME(6),
        PRIMARY KEY (id),
        UNIQUE KEY uk_tp_source_url (source_url),
        INDEX idx_tp_category (category),
        INDEX idx_tp_status   (status),
        INDEX idx_tp_destination_status (destination_slug, status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    INSERT INTO travel_posts (title, destination_name, destination_slug, summary, content, thumbnail_url, source_name, source_url, category, status, created_by, created_at, updated_at) VALUES
    ('Cẩm nang du lịch Đà Lạt cho chuyến nghỉ dưỡng ngắn ngày', 'Đà Lạt', 'da-lat',
     'Gợi ý tổng quan về thời tiết, di chuyển và trải nghiệm phù hợp khi khách tìm nơi lưu trú tại Đà Lạt.',
     'Đà Lạt phù hợp với khách muốn nghỉ dưỡng trong không khí mát mẻ, kết hợp tham quan hồ, đồi thông, vườn hoa và các quán cà phê địa phương.',
     '/assets/images/travel-posts/da-lat-guide.jpg', 'Vietnam.travel', 'https://vietnam.travel/places-to-go/central-vietnam/dalat',
     'GUIDE', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),
    ('8 trải nghiệm nên thử khi đến Đà Lạt', 'Đà Lạt', 'da-lat',
     'Các hoạt động nổi bật như khám phá thiên nhiên, thưởng thức cà phê và trải nghiệm khí hậu cao nguyên.',
     'Bài viết giúp người dùng có thêm ý tưởng hoạt động sau khi đã tìm được nơi lưu trú tại Đà Lạt.',
     '/assets/images/travel-posts/da-lat-8-things.jpg', 'Vietnam.travel', 'https://vietnam.travel/things-to-do/8-things-to-do-in-dalat',
     'ATTRACTION', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 29 DAY), NOW()),
    ('Cẩm nang du lịch Nha Trang cho kỳ nghỉ biển', 'Nha Trang', 'nha-trang',
     'Thông tin tổng quan cho khách muốn nghỉ dưỡng biển, tham quan thành phố và chọn resort tại Nha Trang.',
     'Nha Trang phù hợp với nhóm khách muốn kết hợp tắm biển, nghỉ dưỡng, ăn hải sản và tham quan các điểm văn hóa trong thành phố.',
     '/assets/images/travel-posts/nha-trang-guide.jpg', 'Vietnam.travel', 'https://vietnam.travel/places-to-go/central-vietnam/nha-trang',
     'GUIDE', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 28 DAY), NOW()),
    ('Gợi ý trải nghiệm đảo quanh Nha Trang', 'Nha Trang', 'nha-trang',
     'Gợi ý các hoạt động biển đảo phù hợp với khách đặt resort hoặc villa ở khu vực Nha Trang.',
     'Khi khách chọn lưu trú tại Nha Trang, nhóm trải nghiệm đảo là nội dung dễ liên kết với nhu cầu nghỉ dưỡng biển.',
     '/assets/images/travel-posts/nha-trang-island-hopping.jpg', 'Vietnam.travel', 'https://vietnam.travel/things-to-do/where-to-go-when-island-hopping-around-nha-trang',
     'ATTRACTION', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 27 DAY), NOW()),
    ('Cẩm nang khám phá phố cổ Hội An', 'Hội An', 'hoi-an',
     'Tóm tắt trải nghiệm phố cổ, văn hóa địa phương, ẩm thực và các hoạt động nhẹ nhàng quanh khu lưu trú tại Hội An.',
     'Hội An phù hợp cho khách thích phố cổ, ẩm thực địa phương, đạp xe và dạo bộ buổi tối.',
     '/assets/images/travel-posts/hoi-an-guide.jpg', 'Vietnam.travel', 'https://vietnam.travel/places-to-go/central-vietnam/hoi-an',
     'GUIDE', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 26 DAY), NOW()),
    ('Cách khám phá phố cổ Hội An trọn vẹn hơn', 'Hội An', 'hoi-an',
     'Các hoạt động tham khảo cho khách lưu trú ở Hội An như đi phố cổ, thưởng thức ẩm thực, đạp xe và ghé biển gần đó.',
     'Bài viết này hỗ trợ hành trình: khách tìm homestay Hội An, bấm Gợi ý du lịch và xem các hoạt động có thể thực hiện trong chuyến đi.',
     '/assets/images/travel-posts/hoi-an-ancient-town.jpg', 'Vietnam.travel', 'https://vietnam.travel/things-to-do/the-best-ways-to-explore-the-ancient-town-of-hoi-an',
     'ATTRACTION', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 25 DAY), NOW()),
    ('Cẩm nang du lịch Đà Nẵng cho người mới đi lần đầu', 'Đà Nẵng', 'da-nang',
     'Thông tin tổng quan về thành phố biển, khu vực lưu trú, trải nghiệm tham quan và nghỉ dưỡng tại Đà Nẵng.',
     'Đà Nẵng có lợi thế kết hợp biển, trung tâm thành phố và các điểm vui chơi lân cận.',
     '/assets/images/travel-posts/da-nang-guide.jpg', 'Vietnam.travel', 'https://vietnam.travel/places-to-go/central-vietnam/da-nang',
     'GUIDE', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 24 DAY), NOW()),
    ('Gợi ý lịch trình 3 ngày tại Đà Nẵng', 'Đà Nẵng', 'da-nang',
     'Lịch trình tham khảo giúp khách sắp xếp thời gian giữa nghỉ dưỡng, ăn uống và tham quan khi lưu trú ở Đà Nẵng.',
     'Nội dung này phù hợp hiển thị sau khi khách chọn ngày nhận và trả phòng.',
     '/assets/images/travel-posts/da-nang-itinerary.jpg', 'Vietnam.travel', 'https://vietnam.travel/things-to-do/da-nang-itinerary',
     'ESSENTIAL', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 23 DAY), NOW()),
    ('Cẩm nang du lịch Sa Pa', 'Sa Pa', 'sa-pa',
     'Gợi ý tổng quan về khí hậu vùng núi, ruộng bậc thang, trekking và các trải nghiệm phù hợp tại Sa Pa.',
     'Sa Pa là điểm đến phù hợp với khách thích cảnh núi, văn hóa địa phương và lịch trình khám phá thiên nhiên.',
     '/assets/images/travel-posts/sa-pa-guide.jpg', 'Vietnam.travel', 'https://vietnam.travel/places-to-go/northern-vietnam/sapa',
     'GUIDE', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 22 DAY), NOW()),
    ('Sa Pa cho du khách yêu du lịch bền vững', 'Sa Pa', 'sa-pa',
     'Gợi ý trekking, homestay và trải nghiệm địa phương phù hợp với khách tìm nơi lưu trú tại Sa Pa.',
     'Bài viết giúp người dùng có thêm ý tưởng tham quan khi tìm kiếm Sa Pa hoặc Lào Cai.',
     '/assets/images/travel-posts/sapa-sustainable-travellers.jpg', 'Vietnam.travel', 'https://vietnam.travel/things-to-do/sapa-itinerary-sustainable-travellers',
     'ATTRACTION', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 21 DAY), NOW()),
    ('Cẩm nang du lịch Phú Quốc', 'Phú Quốc', 'phu-quoc',
     'Thông tin tổng quan cho khách muốn nghỉ dưỡng biển đảo, chọn resort và khám phá thiên nhiên tại Phú Quốc.',
     'Phú Quốc phù hợp với khách tìm kỳ nghỉ biển, resort, ẩm thực và trải nghiệm thiên nhiên.',
     '/assets/images/travel-posts/phu-quoc-guide.jpg', 'Vietnam.travel', 'https://vietnam.travel/places-to-go/southern-vietnam/phu-quoc',
     'GUIDE', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 20 DAY), NOW()),
    ('Gợi ý lịch trình Phú Quốc 3 ngày 2 đêm', 'Phú Quốc', 'phu-quoc',
     'Lịch trình tham khảo cho khách muốn kết hợp nghỉ dưỡng, tham quan và trải nghiệm đảo trong chuyến đi ngắn ngày.',
     'Bài viết này phù hợp với luồng sau đặt phòng: khách đã thanh toán thành công có thể bấm xem gợi ý du lịch tại điểm đến.',
     '/assets/images/travel-posts/phu-quoc-3-days.jpg', 'Vietnam.travel', 'https://vietnam.travel/things-to-do/explore-phu-quoc-island-3-days-2-nights',
     'ESSENTIAL', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 19 DAY), NOW()),
    ('Cẩm nang du lịch Hà Nội cho chuyến đi đầu tiên', 'Hà Nội', 'ha-noi',
     'Gợi ý tổng quan về phố cổ, ẩm thực, di chuyển và các khu vực phù hợp khi khách tìm nơi lưu trú tại Hà Nội.',
     'Hà Nội phù hợp với khách muốn kết hợp tham quan văn hóa, ẩm thực đường phố và các trải nghiệm đô thị cổ.',
     '/assets/images/travel-posts/ha-noi-guide.jpg', 'Vietnam.travel', 'https://vietnam.travel/places-to-go/northern-vietnam/ha-noi',
     'GUIDE', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 18 DAY), NOW()),
    ('Cẩm nang du lịch Ninh Bình', 'Ninh Bình', 'ninh-binh',
     'Tóm tắt trải nghiệm Tràng An, Tam Cốc, Hang Múa và các điểm tham quan núi đá vôi gần khu lưu trú.',
     'Ninh Bình là điểm đến phù hợp với khách muốn nghỉ ngắn ngày, đi thuyền, leo núi nhẹ và khám phá cảnh quan tự nhiên.',
     '/assets/images/travel-posts/ninh-binh-guide.jpg', 'Vietnam.travel', 'https://vietnam.travel/places-to-go/northern-vietnam/ninh-binh',
     'GUIDE', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 17 DAY), NOW()),
    ('Cẩm nang du lịch Hà Giang', 'Hà Giang', 'ha-giang',
     'Gợi ý cung đường, mùa đi đẹp và những trải nghiệm nổi bật khi khách tìm nơi lưu trú hoặc gợi ý tại Hà Giang.',
     'Hà Giang phù hợp với khách thích cảnh núi, cung đường đèo và văn hóa bản địa.',
     '/assets/images/travel-posts/ha-giang-guide.jpg', 'Vietnam.travel', 'https://vietnam.travel/places-to-go/northern-vietnam/ha-giang',
     'GUIDE', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 16 DAY), NOW()),
    ('Gợi ý khám phá TP. Hồ Chí Minh', 'TP. Hồ Chí Minh', 'ho-chi-minh',
     'Tóm tắt các trải nghiệm đô thị, ẩm thực, mua sắm và tham quan khi người dùng nhập TP.HCM, HCM, Sài Gòn hoặc Hồ Chí Minh.',
     'TP. Hồ Chí Minh là điểm đến có nhiều cách gọi trong thực tế, vì vậy nội dung hỗ trợ tìm kiếm theo tên gọi phổ biến.',
     '/assets/images/travel-posts/ho-chi-minh-city-guide.jpg', 'Vietnam.travel', 'https://vietnam.travel/places-to-go/southern-vietnam/ho-chi-minh-city',
     'GUIDE', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 15 DAY), NOW()),
    ('Cẩm nang Quảng Ninh và vịnh Hạ Long', 'Quảng Ninh', 'quang-ninh',
     'Gợi ý tham quan Hạ Long, trải nghiệm biển đảo và lịch trình phù hợp khi khách tìm Quảng Ninh hoặc Hạ Long.',
     'Quảng Ninh thường được người dùng tìm bằng nhiều cách như Quảng Ninh, quangninh hoặc Hạ Long.',
     '/assets/images/travel-posts/ha-long-guide.jpg', 'Vietnam.travel', 'https://vietnam.travel/places-to-go/northern-vietnam/ha-long',
     'GUIDE', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 14 DAY), NOW()),
    ('Hồ Thác Bà và gợi ý du lịch Yên Bái', 'Yên Bái', 'yen-bai',
     'Gợi ý một điểm đến thiên nhiên tại Yên Bái, phù hợp hiển thị khi người dùng tìm yen bai hoặc yenbai.',
     'Yên Bái giúp danh sách điểm đến không chỉ tập trung vào các thành phố quen thuộc.',
     '/assets/images/travel-posts/yen-bai-thac-ba.jpg', 'Vietnam.travel', 'https://vietnam.travel/things-to-do/thac-ba-lake-emerald-yen-bai',
     'ATTRACTION', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 13 DAY), NOW()),
    ('Mộc Châu - điểm đến thiên nhiên tại Sơn La', 'Sơn La', 'son-la',
     'Gợi ý cao nguyên Mộc Châu, khí hậu mát mẻ và các trải nghiệm xanh khi khách tìm Sơn La hoặc sonla.',
     'Sơn La được bổ sung để hoàn thiện nhóm điểm đến và có nội dung gợi ý đúng khu vực.',
     '/assets/images/travel-posts/son-la-moc-chau.jpg', 'Vietnam.travel', 'https://vietnam.travel/things-to-do/moc-chau-your-one-stop-nature-escape',
     'ATTRACTION', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),
    ('Lào Cai và hành trình lên Sa Pa', 'Lào Cai', 'lao-cai',
     'Gợi ý cách nhìn Lào Cai như cửa ngõ đến Sa Pa, phù hợp cho các keyword lao cai, Lào Cai, Sa Pa và sapa.',
     'Lào Cai thường gắn với hành trình đi Sa Pa, bài viết giúp user nhập Lào Cai vẫn có gợi ý liên quan.',
     '/assets/images/travel-posts/lao-cai-topas-ecolodge.jpg', 'Vietnam.travel', 'https://vietnam.travel/things-to-do/topas-ecolodge',
     'ESSENTIAL', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 11 DAY), NOW())
    ON DUPLICATE KEY UPDATE
        title = VALUES(title),
        destination_name = VALUES(destination_name),
        destination_slug = VALUES(destination_slug),
        summary = VALUES(summary),
        content = VALUES(content),
        thumbnail_url = VALUES(thumbnail_url),
        source_name = VALUES(source_name),
        category = VALUES(category),
        status = VALUES(status),
        created_by = VALUES(created_by),
        updated_at = VALUES(updated_at);

    -- Kiem tra travel_posts:
    -- SELECT category, status, COUNT(*) FROM travel_posts GROUP BY category, status;
    -- Ky vong: toi thieu 20 bai VISIBLE, co destination_slug de loc theo dia diem.

    -- =============================================
    -- BẢNG TRAVEL_DESTINATIONS — Điểm đến yêu thích (homepage grid)
    -- =============================================
    CREATE TABLE IF NOT EXISTS travel_destinations (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        slug VARCHAR(120) NOT NULL UNIQUE,
        region ENUM('NORTH','CENTRAL','SOUTH') NOT NULL,
        image_url VARCHAR(500) DEFAULT NULL,
        short_description VARCHAR(255) DEFAULT NULL,
        active TINYINT(1) NOT NULL DEFAULT 1,
        display_order INT DEFAULT 0,
        created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
        INDEX idx_td_region_active_order (region, active, display_order)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    INSERT INTO travel_destinations (name, slug, region, image_url, short_description, active, display_order) VALUES
    ('Sa Pa',       'sa-pa',       'NORTH',   '/assets/images/MienBac/Sapa.jpg', 'Thị trấn sương mù với ruộng bậc thang và bản làng dân tộc', 1, 1),
    ('Lào Cai',     'lao-cai',     'NORTH',   '/assets/images/MienBac/LC.jpg', 'Cửa ngõ Tây Bắc với cảnh quan hùng vĩ', 1, 2),
    ('Hà Giang',    'ha-giang',    'NORTH',   '/assets/images/MienBac/MB_HG.jpg', 'Cao nguyên đá với đèo Mã Pì Lèng huyền thoại', 1, 3),
    ('Ninh Bình',   'ninh-binh',   'NORTH',   '/assets/images/MienBac/NB.jpg', 'Tràng An, Bái Đính — di sản thiên nhiên thế giới', 1, 4),
    ('Hà Nội',      'ha-noi',      'NORTH',   '/assets/images/MienBac/MB_HN.jpg', 'Thủ đô nghìn năm văn hiến với phố cổ và ẩm thực đường phố', 1, 5),
    ('Quảng Ninh',  'quang-ninh',  'NORTH',   '/assets/images/MienBac/QN.jpg', 'Vịnh Hạ Long — kỳ quan thiên nhiên thế giới', 1, 6),
    ('Yên Bái',     'yen-bai',     'NORTH',   '/assets/images/MienBac/YB.jpg', 'Mù Cang Chải — ruộng bậc thang đẹp nhất Việt Nam', 1, 7),
    ('Sơn La',      'son-la',      'NORTH',   '/assets/images/MienBac/SL.jpg', 'Mộc Châu — cao nguyên xanh với đồi chè bát ngát', 1, 8),
    ('Đà Lạt',      'da-lat',      'CENTRAL', '/assets/images/travel-posts/da-lat-guide.jpg', 'Thành phố ngàn hoa trên cao nguyên Lâm Viên', 1, 1),
    ('Nha Trang',   'nha-trang',   'CENTRAL', '/assets/images/MienTrung/NhaTrang.jpg', 'Thành phố biển nổi tiếng với resort và đảo hoang', 1, 2),
    ('Đà Nẵng',     'da-nang',     'CENTRAL', '/assets/images/MienTrung/DN.jpg', 'Thành phố đáng sống với Bà Nà Hills và bãi biển Mỹ Khê', 1, 3),
    ('Hội An',      'hoi-an',      'CENTRAL', '/assets/images/travel-posts/hoi-an-guide.jpg', 'Phố cổ di sản UNESCO với đèn lồng và ẩm thực Trung Bộ', 1, 4),
    ('Phú Quốc',    'phu-quoc',    'SOUTH',   '/assets/images/travel-posts/phu-quoc-guide.jpg', 'Đảo ngọc với bãi biển hoang sơ và sunset party', 1, 1),
    ('Hồ Chí Minh', 'ho-chi-minh', 'SOUTH',   '/assets/images/MienNam/HCM.jpg', 'Thành phố năng động nhất Việt Nam, trung tâm kinh tế phía Nam', 1, 2)
    ON DUPLICATE KEY UPDATE
        name = VALUES(name),
        region = VALUES(region),
        image_url = VALUES(image_url),
        short_description = VALUES(short_description),
        active = VALUES(active),
        display_order = VALUES(display_order);

    -- =============================================
    -- HOTEL-TYPE ACCOMMODATIONS — Bổ sung cho Nha Trang / Đà Nẵng / Hội An / Hà Nội
    -- Mục đích: khi user search mặc định tab HOTEL, tất cả điểm đến chính đều trả kết quả.
    -- =============================================

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at) VALUES
    ('InterContinental Nha Trang',
     'Khách sạn 5 sao quốc tế tọa lạc ngay trung tâm bãi biển Trần Phú, view biển toàn cảnh.',
     '32-34 Trần Phú, Lộc Thọ', 'Nha Trang',
     '/assets/images/accommodations/catalog/hotel-exterior.jpg',
     5, 9.0, 1456, 'HOTEL', 'APPROVED', 4, NOW(), NOW());
    SET @nt_hotel_id = LAST_INSERT_ID();

    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category) VALUES
    (@nt_hotel_id, 'ICN-STD', 'Superior City View', '1 giường King', 2, 1850000, 10,
     'Phòng 32m² hướng thành phố, tiện nghi chuẩn 5 sao.',
     '/assets/images/accommodations/amenities/hotel/bedroom.jpg', 'STANDARD'),
    (@nt_hotel_id, 'ICN-DLX', 'Deluxe Ocean Front', '1 giường King cỡ lớn', 2, 3200000, 6,
     'Phòng 42m² hướng biển, ban công riêng ngắm bình minh trên vịnh Nha Trang.',
     '/assets/images/accommodations/amenities/hotel/bedroom.jpg', 'DELUXE'),
    (@nt_hotel_id, 'ICN-SUI', 'Premier Suite Ocean', '1 giường King + sofa bed', 3, 5500000, 3,
     'Suite 65m² hướng biển, phòng khách riêng, bồn tắm jacuzzi.',
     '/assets/images/accommodations/amenities/hotel/bedroom.jpg', 'SUITE');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at) VALUES
    ('Novotel Đà Nẵng Premier',
     'Khách sạn 4 sao hiện đại tọa lạc bên sông Hàn, cách bãi biển Mỹ Khê 5 phút.',
     '36 Bạch Đằng, Hải Châu', 'Đà Nẵng',
     '/assets/images/accommodations/catalog/hotel-exterior.jpg',
     4, 8.7, 2103, 'HOTEL', 'APPROVED', 3, NOW(), NOW());
    SET @dn_hotel_id = LAST_INSERT_ID();

    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category) VALUES
    (@dn_hotel_id, 'NVD-STD', 'Standard River View', '1 giường Queen', 2, 1100000, 12,
     'Phòng 28m² hướng sông Hàn, view cầu Rồng lung linh về đêm.',
     '/assets/images/accommodations/amenities/hotel/bedroom.jpg', 'STANDARD'),
    (@dn_hotel_id, 'NVD-DLX', 'Deluxe Premium', '1 giường King', 2, 1650000, 8,
     'Phòng 35m², tầng cao, bao gồm bữa sáng buffet.',
     '/assets/images/accommodations/amenities/hotel/bedroom.jpg', 'DELUXE'),
    (@dn_hotel_id, 'NVD-FAM', 'Family Connecting', '2 giường Queen', 4, 2400000, 4,
     'Hai phòng liên thông 50m², phù hợp gia đình.',
     '/assets/images/accommodations/amenities/hotel/bedroom.jpg', 'FAMILY');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at) VALUES
    ('La Siesta Hội An Resort & Spa',
     'Khách sạn boutique 4 sao phong cách Đông Dương nằm sát phố cổ, cách Chùa Cầu 300m.',
     '132 Hùng Vương, Cẩm Phô', 'Hội An',
     '/assets/images/accommodations/catalog/hotel-exterior.jpg',
     4, 8.8, 987, 'HOTEL', 'APPROVED', 3, NOW(), NOW());
    SET @ha_hotel_id = LAST_INSERT_ID();

    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category) VALUES
    (@ha_hotel_id, 'LSH-STD', 'Classic Room', '1 giường Queen', 2, 950000, 8,
     'Phòng 26m² thiết kế truyền thống, gần phố cổ.',
     '/assets/images/accommodations/amenities/hotel/bedroom.jpg', 'STANDARD'),
    (@ha_hotel_id, 'LSH-DLX', 'Deluxe Pool Access', '1 giường King', 2, 1450000, 5,
     'Phòng 33m² tầng trệt, lối ra hồ bơi trực tiếp.',
     '/assets/images/accommodations/amenities/hotel/bedroom.jpg', 'DELUXE'),
    (@ha_hotel_id, 'LSH-SUI', 'Heritage Suite', '1 giường King cỡ lớn', 2, 2800000, 3,
     'Suite 55m² phong cách cổ điển, phòng khách riêng.',
     '/assets/images/accommodations/amenities/hotel/bedroom.jpg', 'SUITE');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at) VALUES
    ('Sofitel Legend Metropole Hà Nội',
     'Khách sạn lịch sử 5 sao nằm tại trung tâm quận Hoàn Kiếm, kiến trúc Pháp cổ điển.',
     '15 Ngô Quyền, Tràng Tiền, Hoàn Kiếm', 'Hà Nội',
     '/assets/images/accommodations/catalog/hotel-exterior.jpg',
     5, 9.4, 3201, 'HOTEL', 'APPROVED', 4, NOW(), NOW());
    SET @hn_hotel_id = LAST_INSERT_ID();

    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category) VALUES
    (@hn_hotel_id, 'SLM-PRE', 'Premium Room', '1 giường King', 2, 4500000, 8,
     'Phòng 32m² khu Historical Wing, nội thất gỗ cổ điển.',
     '/assets/images/accommodations/amenities/hotel/bedroom.jpg', 'DELUXE'),
    (@hn_hotel_id, 'SLM-GRA', 'Grand Prestige Suite', '1 giường King cỡ lớn', 2, 12000000, 3,
     'Suite 75m² khu Opera Wing, phòng khách riêng, butler 24/7.',
     '/assets/images/accommodations/amenities/hotel/bedroom.jpg', 'SUITE'),
    (@hn_hotel_id, 'SLM-FAM', 'Family Heritage', '2 giường Queen', 4, 7800000, 2,
     'Phòng gia đình 52m², hai phòng ngủ, view Hồ Hoàn Kiếm.',
     '/assets/images/accommodations/amenities/hotel/bedroom.jpg', 'FAMILY');

    -- Sa Pa HOTEL
    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at) VALUES
    ('Sapa Cloud Valley Hotel',
     'Khách sạn boutique hướng thung lũng Mường Hoa, phù hợp nghỉ dưỡng và săn mây.',
     '25 Fansipan, Thị xã Sa Pa', 'Sa Pa',
     '/assets/images/accommodations/catalog/hotel-exterior.jpg',
     4, 8.9, 342, 'HOTEL', 'APPROVED', 3, NOW(), NOW());
    SET @sapa_id = LAST_INSERT_ID();

    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category) VALUES
    (@sapa_id, 'SPC-STD', 'Cloud View Standard', '1 giường Queen', 2, 720000, 6,
     'Phòng 24m² có cửa sổ nhìn thị trấn Sa Pa.',
     '/assets/images/accommodations/amenities/hotel/bedroom.jpg', 'STANDARD'),
    (@sapa_id, 'SPC-DLX', 'Deluxe Valley Balcony', '1 giường King', 2, 980000, 4,
     'Phòng 32m² có ban công nhìn thung lũng, bao gồm bữa sáng.',
     '/assets/images/accommodations/amenities/hotel/bedroom.jpg', 'DELUXE');

    -- Phú Quốc RESORT
    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at) VALUES
    ('Sunset Pearl Resort Phú Quốc',
     'Resort ven biển phía tây đảo Phú Quốc, có hồ bơi ngoài trời và nhà hàng hải sản.',
     'Bãi Trường, Dương Tơ', 'Phú Quốc',
     '/assets/images/accommodations/catalog/resort-exterior.jpg',
     5, 9.2, 876, 'RESORT', 'APPROVED', 4, NOW(), NOW());
    SET @pq_id = LAST_INSERT_ID();

    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category) VALUES
    (@pq_id, 'SPQ-DLX', 'Deluxe Garden Room', '1 giường King', 2, 2100000, 8,
     'Phòng 38m² hướng vườn nhiệt đới, bao gồm bữa sáng buffet.',
     '/assets/images/accommodations/catalog/resort-room.jpg', 'DELUXE'),
    (@pq_id, 'SPQ-SEA', 'Ocean Sunset Suite', '1 giường King cỡ lớn', 2, 3900000, 4,
     'Suite 58m² hướng biển, ban công riêng ngắm hoàng hôn.',
     '/assets/images/accommodations/catalog/resort-room.jpg', 'SUITE');

    -- =============================================
    -- ACCOMMODATION BỔ SUNG VỪA ĐỦ THEO LOẠI HÌNH
    -- Idempotent theo name/room_code: chạy lại không tạo trùng dữ liệu.
    --   rooms               : 36+ (tối thiểu cho search/listing)
    -- =============================================

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Pine Hill Villa Đà Lạt',
           'Villa riêng giữa đồi thông Đà Lạt, có bếp, sân BBQ và phòng khách rộng cho nhóm gia đình.',
           '12 Hoàng Hoa Thám, Phường 10',
           'Đà Lạt',
           '/assets/images/accommodations/catalog/villa-exterior.jpg',
           4, 8.9, 216, 'VILLA', 'APPROVED', 7, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Pine Hill Villa Đà Lạt');
    SET @pine_dalat_villa_id = (SELECT id FROM accommodations WHERE name = 'Pine Hill Villa Đà Lạt' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @pine_dalat_villa_id, 'PHV-DLX', 'Deluxe Pine Villa', '2 giường Queen', 4, 2400000, 3,
           'Căn villa 2 phòng ngủ nhìn ra đồi thông, phù hợp gia đình nhỏ.',
           '/assets/images/accommodations/amenities/villa/bedroom.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'PHV-DLX');
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @pine_dalat_villa_id, 'PHV-FAM', 'Family BBQ Villa', '3 giường Queen', 6, 3600000, 2,
           'Căn villa sân vườn, bếp riêng và khu BBQ ngoài trời.',
           '/assets/images/accommodations/amenities/villa/bedroom.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'PHV-FAM');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Sunset Beach Villa Phú Quốc',
           'Villa biển phía tây Phú Quốc, thích hợp nhóm bạn và gia đình muốn nghỉ dưỡng riêng tư.',
           'Bãi Trường, Dương Tơ',
           'Phú Quốc',
           '/assets/images/accommodations/catalog/villa-exterior.jpg',
           5, 9.1, 334, 'VILLA', 'APPROVED', 7, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Sunset Beach Villa Phú Quốc');
    SET @sunset_pq_villa_id = (SELECT id FROM accommodations WHERE name = 'Sunset Beach Villa Phú Quốc' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @sunset_pq_villa_id, 'SBV-SEA', 'Sea Breeze Villa', '2 giường King', 4, 4200000, 3,
           'Villa hai phòng ngủ gần biển, có ban công ngắm hoàng hôn.',
           '/assets/images/accommodations/amenities/villa/bedroom.jpg', 'VIP'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'SBV-SEA');
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @sunset_pq_villa_id, 'SBV-POOL', 'Private Pool Villa', '3 giường King', 6, 6800000, 1,
           'Villa hồ bơi riêng cho nhóm lớn, có bếp và phòng khách.',
           '/assets/images/accommodations/amenities/villa/bedroom.jpg', 'SUITE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'SBV-POOL');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Tam Cốc Garden Homestay',
           'Homestay gần bến Tam Cốc, view núi đá vôi, có xe đạp miễn phí và bữa sáng địa phương.',
           'Đội 3, Văn Lâm, Ninh Hải',
           'Ninh Bình',
           '/assets/images/accommodations/catalog/homestay-exterior.jpg',
           3, 8.8, 189, 'HOMESTAY', 'APPROVED', 8, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Tam Cốc Garden Homestay');
    SET @tamcoc_hs_id = (SELECT id FROM accommodations WHERE name = 'Tam Cốc Garden Homestay' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @tamcoc_hs_id, 'TCG-STD', 'Phòng Vườn Tiêu Chuẩn', '1 giường Queen', 2, 420000, 5,
           'Phòng riêng nhìn ra vườn, phù hợp khách đi cặp đôi.',
           '/assets/images/accommodations/amenities/homestay/bedroom.jpg', 'STANDARD'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'TCG-STD');
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @tamcoc_hs_id, 'TCG-FAM', 'Phòng Gia Đình Tam Cốc', '2 giường Queen', 4, 720000, 3,
           'Phòng gia đình rộng, có ban công nhìn núi đá vôi.',
           '/assets/images/accommodations/amenities/homestay/bedroom.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'TCG-FAM');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Sapa Valley Homestay',
           'Homestay bản làng nhìn ra thung lũng Mường Hoa, phù hợp du khách thích trekking và trải nghiệm địa phương.',
           'Lao Chải, Sa Pa',
           'Sa Pa',
           '/assets/images/accommodations/catalog/homestay-exterior.jpg',
           3, 8.7, 241, 'HOMESTAY', 'APPROVED', 8, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Sapa Valley Homestay');
    SET @sapa_hs_id = (SELECT id FROM accommodations WHERE name = 'Sapa Valley Homestay' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @sapa_hs_id, 'SVH-STD', 'Phòng Gỗ View Núi', '1 giường đôi', 2, 380000, 6,
           'Phòng gỗ đơn giản, có cửa sổ nhìn ruộng bậc thang.',
           '/assets/images/accommodations/amenities/homestay/bedroom.jpg', 'STANDARD'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'SVH-STD');
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @sapa_hs_id, 'SVH-DLX', 'Deluxe Valley Room', '1 giường King', 2, 620000, 4,
           'Phòng có ban công riêng, bao gồm bữa sáng địa phương.',
           '/assets/images/accommodations/amenities/homestay/bedroom.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'SVH-DLX');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Legacy Bay Resort Hạ Long',
           'Resort nghỉ dưỡng bên vịnh Hạ Long, phù hợp khách tìm Quảng Ninh hoặc Hạ Long.',
           'Bãi Cháy, Hạ Long',
           'Quảng Ninh',
           '/assets/images/accommodations/catalog/resort-exterior.jpg',
           5, 9.0, 512, 'RESORT', 'APPROVED', 4, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Legacy Bay Resort Hạ Long');
    SET @halong_resort_id = (SELECT id FROM accommodations WHERE name = 'Legacy Bay Resort Hạ Long' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @halong_resort_id, 'LBR-DLX', 'Deluxe Bay View', '1 giường King', 2, 2600000, 7,
           'Phòng hướng vịnh, ban công riêng và bữa sáng buffet.',
           '/assets/images/accommodations/catalog/resort-room.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'LBR-DLX');
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @halong_resort_id, 'LBR-SUI', 'Heritage Bay Suite', '1 giường King cỡ lớn', 2, 4800000, 3,
           'Suite tầng cao nhìn toàn cảnh vịnh Hạ Long, có phòng khách riêng.',
           '/assets/images/accommodations/catalog/resort-room.jpg', 'SUITE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'LBR-SUI');

    -- ─── BỔ SUNG ĐIỂM ĐẾN MIỀN NAM (SOUTH) — tăng từ 2 lên 5 ─────────────────
    INSERT INTO travel_destinations (name, slug, region, image_url, short_description, active, display_order) VALUES
    ('Vũng Tàu',  'vung-tau', 'SOUTH', '/assets/images/MienNam/VT.jpg', 'Thành phố biển gần Sài Gòn với hải sản tươi ngon và bãi tắm sạch', 1, 3),
    ('Cần Thơ',   'can-tho',  'SOUTH', '/assets/images/MienNam/CTho.jpg', 'Thủ phủ miền Tây với chợ nổi Cái Răng và ẩm thực sông nước',       1, 4),
    ('Mũi Né',    'mui-ne',   'SOUTH', '/assets/images/travel-posts/mui-ne-must-do.jpg', 'Làng chài biển với đồi cát bay và kite surfing nổi tiếng',          1, 5)
    ON DUPLICATE KEY UPDATE
        name = VALUES(name), region = VALUES(region), image_url = VALUES(image_url),
        short_description = VALUES(short_description), active = VALUES(active), display_order = VALUES(display_order);

    -- ─── HOTEL TẠI HỒ CHÍ MINH ────────────────────────────────────────────────
    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'New World Sài Gòn Hotel',
           'Khách sạn 5 sao biểu tượng tại trung tâm Quận 1, gần chợ Bến Thành và các địa điểm lịch sử.',
           '76 Lê Lai, Bến Thành, Quận 1', 'Hồ Chí Minh',
           '/assets/images/accommodations/catalog/hotel-exterior.jpg',
           5, 9.1, 3421, 'HOTEL', 'APPROVED', 3, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'New World Sài Gòn Hotel');
    SET @hcm_hotel_id = (SELECT id FROM accommodations WHERE name = 'New World Sài Gòn Hotel' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @hcm_hotel_id, 'NWS-STD', 'Superior City Room', '1 giường King', 2, 2200000, 15,
           'Phòng 34m², view thành phố, bao gồm bữa sáng.',
           '/assets/images/accommodations/amenities/hotel/bedroom.jpg', 'STANDARD'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'NWS-STD');
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @hcm_hotel_id, 'NWS-DLX', 'Deluxe Saigon View', '1 giường King cỡ lớn', 2, 3400000, 8,
           'Phòng 42m² góc nhìn toàn cảnh Quận 1 và sông Sài Gòn.',
           '/assets/images/accommodations/amenities/hotel/bedroom.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'NWS-DLX');

    -- ─── HOTEL TẠI VŨNG TÀU ───────────────────────────────────────────────────
    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Pullman Vũng Tàu',
           'Khách sạn 5 sao đẳng cấp nằm ngay bờ biển Vũng Tàu, view biển toàn cảnh.',
           '18 Thùy Vân, Phường 8', 'Vũng Tàu',
           '/assets/images/accommodations/catalog/hotel-exterior.jpg',
           5, 8.9, 1876, 'HOTEL', 'APPROVED', 3, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Pullman Vũng Tàu');
    SET @vt_hotel_id = (SELECT id FROM accommodations WHERE name = 'Pullman Vũng Tàu' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @vt_hotel_id, 'PVT-STD', 'Superior Sea View', '1 giường King', 2, 1650000, 12,
           'Phòng 32m² hướng biển, ban công ngắm hoàng hôn Vũng Tàu.',
           '/assets/images/accommodations/amenities/hotel/bedroom.jpg', 'STANDARD'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'PVT-STD');
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @vt_hotel_id, 'PVT-DLX', 'Deluxe Pool View', '1 giường King cỡ lớn', 2, 2600000, 6,
           'Phòng 40m² nhìn ra hồ bơi tràn bờ và biển, bao gồm bữa sáng.',
           '/assets/images/accommodations/amenities/hotel/bedroom.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'PVT-DLX');

    -- ─── RESORT TẠI CẦN THƠ ───────────────────────────────────────────────────
    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Azerai Cần Thơ Resort',
           'Khu nghỉ dưỡng 5 sao bên sông Hậu, kiến trúc địa phương kết hợp hiện đại, trải nghiệm sông nước miền Tây.',
           '1 Lê Lợi, Ninh Kiều', 'Cần Thơ',
           '/assets/images/accommodations/catalog/resort-exterior.jpg',
           5, 9.2, 987, 'RESORT', 'APPROVED', 4, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Azerai Cần Thơ Resort');
    SET @cantho_id = (SELECT id FROM accommodations WHERE name = 'Azerai Cần Thơ Resort' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @cantho_id, 'AZC-DLX', 'Deluxe River View', '1 giường King', 2, 2800000, 8,
           'Phòng 42m² hướng sông Hậu, bao gồm bữa sáng và kayak miễn phí.',
           '/assets/images/accommodations/catalog/resort-room.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'AZC-DLX');
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @cantho_id, 'AZC-SUI', 'Pool Villa Suite', '1 giường King cỡ lớn', 2, 4500000, 4,
           'Suite riêng với hồ bơi nhỏ, view sông tuyệt đẹp.',
           '/assets/images/accommodations/catalog/resort-room.jpg', 'SUITE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'AZC-SUI');

    -- ─── RESORT TẠI MŨI NÉ ────────────────────────────────────────────────────
    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'TTC Resort Mũi Né',
           'Resort biển phong cách làng chài, nằm trên bãi biển Mũi Né với hồ bơi vô cực và đồi cát gần kề.',
           '56 Nguyễn Đình Chiểu, Hàm Tiến', 'Mũi Né',
           '/assets/images/accommodations/catalog/resort-exterior.jpg',
           4, 8.6, 1234, 'RESORT', 'APPROVED', 4, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'TTC Resort Mũi Né');
    SET @muine_id = (SELECT id FROM accommodations WHERE name = 'TTC Resort Mũi Né' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @muine_id, 'TTC-DLX', 'Deluxe Beach Bungalow', '1 giường King', 2, 1450000, 10,
           'Phòng bungalow nhìn ra biển, có bếp nhỏ và ban công riêng.',
           '/assets/images/accommodations/catalog/resort-room.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'TTC-DLX');
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @muine_id, 'TTC-FAM', 'Family Garden Villa', '2 giường Queen', 4, 2200000, 5,
           'Villa vườn, 2 phòng ngủ, phù hợp gia đình và nhóm bạn 4 người.',
           '/assets/images/accommodations/catalog/resort-room.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'TTC-FAM');

    -- ─── BỔ SUNG 14 CƠ SỞ VÀ 42 PHÒNG — phủ đủ tỉnh/thành và loại hình ─────
    SET @owner_resort_id = (SELECT id FROM users WHERE email = 'resort@travelmate.vn' LIMIT 1);
    SET @owner_villa_id = (SELECT id FROM users WHERE email = 'villa@travelmate.vn' LIMIT 1);
    SET @owner_homestay_id = (SELECT id FROM users WHERE email = 'homestay@travelmate.vn' LIMIT 1);
    SET @owner_nobank_id = (SELECT id FROM users WHERE email = 'no-bank@travelmate.vn' LIMIT 1);

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Lào Cai Central Hotel',
           'Khách sạn trung tâm thành phố Lào Cai, thuận tiện đi Sa Pa, cửa khẩu quốc tế và chợ Cốc Lếu.',
           '86 Hoàng Liên, phường Cốc Lếu', 'Lào Cai',
           '/assets/images/accommodations/hotel/intercontinental-nha-trang/cover.jpg',
           4, 8.6, 184, 'HOTEL', 'APPROVED', @owner_nobank_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Lào Cai Central Hotel');
    SET @laocai_hotel_id = (SELECT id FROM accommodations WHERE name = 'Lào Cai Central Hotel' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @laocai_hotel_id, 'LCH-STD', 'Standard City Room', '1 giường Queen', 2, 620000, 8,
           'Phòng 26m² hướng phố, phù hợp khách công tác hoặc nghỉ ngắn ngày.',
           '/assets/images/accommodations/hotel/intercontinental-nha-trang/rooms/icn-std.jpg', 'STANDARD'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'LCH-STD')
    UNION ALL
    SELECT @laocai_hotel_id, 'LCH-DLX', 'Deluxe River View', '1 giường King', 2, 890000, 5,
           'Phòng 34m² hướng sông Hồng, có bàn làm việc và bữa sáng.',
           '/assets/images/accommodations/hotel/intercontinental-nha-trang/rooms/icn-dlx.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'LCH-DLX')
    UNION ALL
    SELECT @laocai_hotel_id, 'LCH-FAM', 'Family Connecting Room', '2 giường Queen', 4, 1350000, 3,
           'Hai phòng liên thông cho gia đình, có minibar và phòng tắm riêng.',
           '/assets/images/accommodations/hotel/intercontinental-nha-trang/rooms/icn-sui.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'LCH-FAM');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Bắc Hà Valley Homestay',
           'Homestay gần chợ phiên Bắc Hà, nhà sàn gỗ, bếp lửa chung và trải nghiệm ẩm thực địa phương.',
           'Thôn Na Hối, thị trấn Bắc Hà', 'Lào Cai',
           '/assets/images/accommodations/homestay/sapa-valley-homestay/cover.jpg',
           3, 8.8, 96, 'HOMESTAY', 'APPROVED', @owner_homestay_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Bắc Hà Valley Homestay');
    SET @bacha_hs_id = (SELECT id FROM accommodations WHERE name = 'Bắc Hà Valley Homestay' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @bacha_hs_id, 'BHV-STD', 'Phòng Gỗ Na Hối', '1 giường đôi', 2, 360000, 5,
           'Phòng gỗ ấm cúng, cửa sổ nhìn vườn mận và bản làng.',
           '/assets/images/accommodations/homestay/sapa-valley-homestay/rooms/svh-std.jpg', 'STANDARD'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'BHV-STD')
    UNION ALL
    SELECT @bacha_hs_id, 'BHV-DLX', 'Deluxe Valley Balcony', '1 giường King', 2, 520000, 3,
           'Phòng có ban công nhìn thung lũng, bao gồm bữa sáng địa phương.',
           '/assets/images/accommodations/homestay/sapa-valley-homestay/rooms/svh-dlx.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'BHV-DLX')
    UNION ALL
    SELECT @bacha_hs_id, 'BHV-FAM', 'Nhà Gia Đình Bắc Hà', '2 giường Queen', 4, 780000, 2,
           'Phòng gia đình rộng, có khu sinh hoạt chung và ấm đun nước.',
           '/assets/images/accommodations/homestay/tam-coc-garden-homestay/rooms/tcg-fam.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'BHV-FAM');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Đồng Văn Stone Hotel',
           'Khách sạn nhỏ tại phố cổ Đồng Văn, phù hợp khách đi cung Hà Giang loop và nghỉ qua đêm sau chặng đèo.',
           '18 Phố Cổ, thị trấn Đồng Văn', 'Hà Giang',
           '/assets/images/accommodations/hotel/novotel-da-nang-premier/cover.jpg',
           3, 8.5, 128, 'HOTEL', 'APPROVED', @owner_nobank_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Đồng Văn Stone Hotel');
    SET @dongvan_hotel_id = (SELECT id FROM accommodations WHERE name = 'Đồng Văn Stone Hotel' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @dongvan_hotel_id, 'DVS-STD', 'Stone Standard Room', '1 giường Queen', 2, 520000, 7,
           'Phòng tiêu chuẩn tường đá, có máy lạnh, nước nóng và WiFi.',
           '/assets/images/accommodations/hotel/novotel-da-nang-premier/rooms/nvd-std.jpg', 'STANDARD'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'DVS-STD')
    UNION ALL
    SELECT @dongvan_hotel_id, 'DVS-DLX', 'Deluxe Old Quarter View', '1 giường King', 2, 760000, 4,
           'Phòng tầng cao nhìn phố cổ Đồng Văn, có ban công nhỏ.',
           '/assets/images/accommodations/hotel/novotel-da-nang-premier/rooms/nvd-dlx.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'DVS-DLX')
    UNION ALL
    SELECT @dongvan_hotel_id, 'DVS-SUI', 'Panorama Suite', '1 giường King + sofa bed', 3, 1250000, 2,
           'Suite rộng cho nhóm nhỏ, nhìn núi đá và phố cổ về đêm.',
           '/assets/images/accommodations/hotel/novotel-da-nang-premier/rooms/nvd-fam.jpg', 'SUITE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'DVS-SUI');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Mã Pì Lèng View Homestay',
           'Homestay nhìn hẻm Tu Sản và dòng Nho Quế, có sân hiên ngắm bình minh trên đèo Mã Pì Lèng.',
           'Pải Lủng, Mèo Vạc', 'Hà Giang',
           '/assets/images/accommodations/homestay/sapa-valley-homestay/cover.jpg',
           3, 8.9, 142, 'HOMESTAY', 'APPROVED', @owner_homestay_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Mã Pì Lèng View Homestay');
    SET @mapileng_hs_id = (SELECT id FROM accommodations WHERE name = 'Mã Pì Lèng View Homestay' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @mapileng_hs_id, 'MPL-STD', 'Phòng Núi Tiêu Chuẩn', '1 giường đôi', 2, 390000, 6,
           'Phòng riêng đơn giản, cửa sổ nhìn núi đá và thung lũng.',
           '/assets/images/accommodations/homestay/sapa-valley-homestay/rooms/svh-std.jpg', 'STANDARD'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'MPL-STD')
    UNION ALL
    SELECT @mapileng_hs_id, 'MPL-DLX', 'Deluxe Nho Quế Balcony', '1 giường King', 2, 650000, 4,
           'Phòng có ban công riêng nhìn sông Nho Quế từ xa.',
           '/assets/images/accommodations/homestay/sapa-valley-homestay/rooms/svh-dlx.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'MPL-DLX')
    UNION ALL
    SELECT @mapileng_hs_id, 'MPL-FAM', 'Family Mountain Room', '2 giường Queen', 4, 920000, 2,
           'Phòng gia đình có khu ngồi chung và bữa sáng địa phương.',
           '/assets/images/accommodations/homestay/tam-coc-garden-homestay/rooms/tcg-fam.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'MPL-FAM');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Mù Cang Chải Eco Lodge',
           'Khu nghỉ nhà gỗ giữa ruộng bậc thang, phù hợp mùa lúa chín và các chuyến trekking nhẹ.',
           'La Pán Tẩn, Mù Cang Chải', 'Yên Bái',
           '/assets/images/accommodations/homestay/tam-coc-garden-homestay/cover.jpg',
           3, 8.7, 118, 'HOMESTAY', 'APPROVED', @owner_homestay_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Mù Cang Chải Eco Lodge');
    SET @mucangchai_hs_id = (SELECT id FROM accommodations WHERE name = 'Mù Cang Chải Eco Lodge' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @mucangchai_hs_id, 'MCC-STD', 'Bungalow Ruộng Bậc Thang', '1 giường đôi', 2, 480000, 5,
           'Bungalow gỗ riêng, có ban công nhìn ruộng bậc thang.',
           '/assets/images/accommodations/homestay/sapa-valley-homestay/rooms/svh-std.jpg', 'STANDARD'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'MCC-STD')
    UNION ALL
    SELECT @mucangchai_hs_id, 'MCC-DLX', 'Deluxe Harvest View', '1 giường King', 2, 720000, 3,
           'Phòng rộng hơn, ban công riêng và bữa sáng bản địa.',
           '/assets/images/accommodations/homestay/sapa-valley-homestay/rooms/svh-dlx.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'MCC-DLX')
    UNION ALL
    SELECT @mucangchai_hs_id, 'MCC-FAM', 'Family Terrace Lodge', '2 giường Queen', 4, 1050000, 2,
           'Phòng gia đình có sân hiên, phù hợp nhóm săn mùa lúa chín.',
           '/assets/images/accommodations/homestay/tam-coc-garden-homestay/rooms/tcg-fam.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'MCC-FAM');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Tú Lệ Hot Spring Resort',
           'Resort khoáng nóng ở thung lũng Tú Lệ, có bể ngâm ngoài trời và nhà hàng đặc sản Tây Bắc.',
           'Bản Chao, Tú Lệ, Văn Chấn', 'Yên Bái',
           '/assets/images/accommodations/resort/legacy-bay-resort-ha-long/cover.jpg',
           4, 8.8, 164, 'RESORT', 'APPROVED', @owner_resort_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Tú Lệ Hot Spring Resort');
    SET @tule_resort_id = (SELECT id FROM accommodations WHERE name = 'Tú Lệ Hot Spring Resort' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @tule_resort_id, 'TLR-DLX', 'Deluxe Hot Spring Room', '1 giường King', 2, 1380000, 6,
           'Phòng gần khu khoáng nóng, có ban công nhìn thung lũng.',
           '/assets/images/accommodations/resort/legacy-bay-resort-ha-long/rooms/lbr-dlx.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'TLR-DLX')
    UNION ALL
    SELECT @tule_resort_id, 'TLR-SUI', 'Valley Mineral Suite', '1 giường King cỡ lớn', 2, 2400000, 3,
           'Suite riêng có bồn ngâm trong phòng và phòng khách nhỏ.',
           '/assets/images/accommodations/resort/legacy-bay-resort-ha-long/rooms/lbr-sui.jpg', 'SUITE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'TLR-SUI')
    UNION ALL
    SELECT @tule_resort_id, 'TLR-FAM', 'Family Onsen Room', '2 giường Queen', 4, 3100000, 2,
           'Phòng gia đình rộng, gần khu hồ bơi và nhà hàng.',
           '/assets/images/accommodations/resort/sunset-pearl-resort-phu-quoc/rooms/spq-dlx.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'TLR-FAM');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Mộc Châu Tea Hill Hotel',
           'Khách sạn cạnh đồi chè Mộc Châu, thuận tiện đi thác Dải Yếm, rừng thông Bản Áng và trang trại bò sữa.',
           'Tiểu khu 32, thị trấn Nông Trường', 'Sơn La',
           '/assets/images/accommodations/hotel/pullman-vung-tau/cover.jpg',
           4, 8.6, 206, 'HOTEL', 'APPROVED', @owner_nobank_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Mộc Châu Tea Hill Hotel');
    SET @mochau_hotel_id = (SELECT id FROM accommodations WHERE name = 'Mộc Châu Tea Hill Hotel' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @mochau_hotel_id, 'MCT-STD', 'Standard Tea Hill', '1 giường Queen', 2, 680000, 9,
           'Phòng gọn gàng, cửa sổ nhìn đồi chè và khu vườn.',
           '/assets/images/accommodations/hotel/pullman-vung-tau/rooms/pvt-std.jpg', 'STANDARD'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'MCT-STD')
    UNION ALL
    SELECT @mochau_hotel_id, 'MCT-DLX', 'Deluxe Balcony Tea View', '1 giường King', 2, 980000, 5,
           'Phòng có ban công, bàn làm việc và bữa sáng.',
           '/assets/images/accommodations/hotel/pullman-vung-tau/rooms/pvt-dlx.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'MCT-DLX')
    UNION ALL
    SELECT @mochau_hotel_id, 'MCT-FAM', 'Family Green Hill', '2 giường Queen', 4, 1520000, 3,
           'Phòng gia đình rộng, phù hợp nhóm đi Mộc Châu cuối tuần.',
           '/assets/images/accommodations/hotel/new-world-sai-gon-hotel/rooms/nws-dlx.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'MCT-FAM');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Mộc Châu Farmstay',
           'Farmstay có vườn dâu, khu BBQ và xe đạp miễn phí, phù hợp gia đình có trẻ nhỏ.',
           'Bản Áng 2, Đông Sang', 'Sơn La',
           '/assets/images/accommodations/homestay/tam-coc-garden-homestay/cover.jpg',
           3, 8.9, 134, 'HOMESTAY', 'APPROVED', @owner_homestay_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Mộc Châu Farmstay');
    SET @mochau_farm_id = (SELECT id FROM accommodations WHERE name = 'Mộc Châu Farmstay' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @mochau_farm_id, 'MCF-STD', 'Garden Farm Room', '1 giường đôi', 2, 420000, 6,
           'Phòng nhìn vườn, có lối ra sân chung và bếp nhỏ.',
           '/assets/images/accommodations/homestay/sapa-valley-homestay/rooms/svh-std.jpg', 'STANDARD'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'MCF-STD')
    UNION ALL
    SELECT @mochau_farm_id, 'MCF-DLX', 'Deluxe Strawberry View', '1 giường King', 2, 620000, 4,
           'Phòng có ban công nhìn vườn dâu, bao gồm bữa sáng.',
           '/assets/images/accommodations/homestay/sapa-valley-homestay/rooms/svh-dlx.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'MCF-DLX')
    UNION ALL
    SELECT @mochau_farm_id, 'MCF-FAM', 'Family Farm House', '2 giường Queen', 4, 960000, 2,
           'Phòng gia đình có khu sinh hoạt chung và sân BBQ.',
           '/assets/images/accommodations/homestay/tam-coc-garden-homestay/rooms/tcg-fam.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'MCF-FAM');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Tràng An River Villa',
           'Villa ven sông gần khu danh thắng Tràng An, có bếp riêng, sân vườn và không gian yên tĩnh cho gia đình.',
           'Xã Trường Yên, Hoa Lư', 'Ninh Bình',
           '/assets/images/accommodations/villa/pine-hill-villa-da-lat/cover.jpg',
           4, 9.0, 172, 'VILLA', 'APPROVED', @owner_villa_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Tràng An River Villa');
    SET @trangan_villa_id = (SELECT id FROM accommodations WHERE name = 'Tràng An River Villa' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @trangan_villa_id, 'TRV-DLX', 'River Garden Villa', '2 giường Queen', 4, 2600000, 3,
           'Villa 2 phòng ngủ nhìn sông, có bếp và sân vườn riêng.',
           '/assets/images/accommodations/villa/pine-hill-villa-da-lat/rooms/phv-dlx.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'TRV-DLX')
    UNION ALL
    SELECT @trangan_villa_id, 'TRV-FAM', 'Family River House', '3 giường Queen', 6, 3800000, 2,
           'Căn villa cho nhóm gia đình, phòng khách rộng và khu BBQ ngoài trời.',
           '/assets/images/accommodations/villa/pine-hill-villa-da-lat/rooms/phv-fam.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'TRV-FAM')
    UNION ALL
    SELECT @trangan_villa_id, 'TRV-SUI', 'Lotus Pool Villa', '2 giường King', 4, 5200000, 1,
           'Villa cao cấp có hồ bơi riêng và sân ngắm núi đá vôi.',
           '/assets/images/accommodations/villa/sunset-beach-villa-phu-quoc/rooms/sbv-pool.jpg', 'SUITE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'TRV-SUI');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Hạ Long Marina Hotel',
           'Khách sạn gần bến du thuyền Hạ Long, thuận tiện đi tour vịnh và khu vui chơi Bãi Cháy.',
           'Đường Hạ Long, Bãi Cháy', 'Quảng Ninh',
           '/assets/images/accommodations/hotel/intercontinental-nha-trang/cover.jpg',
           4, 8.7, 268, 'HOTEL', 'APPROVED', @owner_nobank_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Hạ Long Marina Hotel');
    SET @halong_hotel_id = (SELECT id FROM accommodations WHERE name = 'Hạ Long Marina Hotel' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @halong_hotel_id, 'HLM-STD', 'Standard Marina Room', '1 giường Queen', 2, 920000, 9,
           'Phòng hướng phố biển, tiện nghi đầy đủ cho kỳ nghỉ ngắn.',
           '/assets/images/accommodations/hotel/intercontinental-nha-trang/rooms/icn-std.jpg', 'STANDARD'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'HLM-STD')
    UNION ALL
    SELECT @halong_hotel_id, 'HLM-DLX', 'Deluxe Bay Window', '1 giường King', 2, 1450000, 6,
           'Phòng có cửa sổ lớn nhìn vịnh, bao gồm bữa sáng buffet.',
           '/assets/images/accommodations/hotel/intercontinental-nha-trang/rooms/icn-dlx.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'HLM-DLX')
    UNION ALL
    SELECT @halong_hotel_id, 'HLM-SUI', 'Bay View Suite', '1 giường King + sofa bed', 3, 2600000, 3,
           'Suite tầng cao nhìn vịnh Hạ Long, có phòng khách riêng.',
           '/assets/images/accommodations/hotel/intercontinental-nha-trang/rooms/icn-sui.jpg', 'SUITE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'HLM-SUI');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Vũng Tàu Beachfront Homestay',
           'Homestay sát biển Bãi Sau, có bếp chung, sân thượng và phòng gia đình cho chuyến đi cuối tuần.',
           '120 Thùy Vân, Phường 2', 'Vũng Tàu',
           '/assets/images/accommodations/homestay/tam-coc-garden-homestay/cover.jpg',
           3, 8.6, 156, 'HOMESTAY', 'APPROVED', @owner_homestay_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Vũng Tàu Beachfront Homestay');
    SET @vungtau_hs_id = (SELECT id FROM accommodations WHERE name = 'Vũng Tàu Beachfront Homestay' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @vungtau_hs_id, 'VBF-STD', 'Phòng Biển Tiêu Chuẩn', '1 giường đôi', 2, 520000, 7,
           'Phòng riêng gần biển, có máy lạnh và phòng tắm riêng.',
           '/assets/images/accommodations/homestay/sapa-valley-homestay/rooms/svh-std.jpg', 'STANDARD'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'VBF-STD')
    UNION ALL
    SELECT @vungtau_hs_id, 'VBF-DLX', 'Deluxe Sea Balcony', '1 giường King', 2, 780000, 4,
           'Phòng ban công hướng biển, có ấm đun nước và bữa sáng.',
           '/assets/images/accommodations/homestay/sapa-valley-homestay/rooms/svh-dlx.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'VBF-DLX')
    UNION ALL
    SELECT @vungtau_hs_id, 'VBF-FAM', 'Family Beach Room', '2 giường Queen', 4, 1180000, 3,
           'Phòng gia đình gần sân thượng, phù hợp nhóm 4 người.',
           '/assets/images/accommodations/homestay/tam-coc-garden-homestay/rooms/tcg-fam.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'VBF-FAM');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Mekong Riverside Homestay Cần Thơ',
           'Homestay ven sông gần chợ nổi Cái Răng, có thuyền nhỏ, bữa sáng miền Tây và vườn cây ăn trái.',
           'Khu vực Cái Răng, Cần Thơ', 'Cần Thơ',
           '/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/cover.jpg',
           3, 8.8, 201, 'HOMESTAY', 'APPROVED', @owner_homestay_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Mekong Riverside Homestay Cần Thơ');
    SET @mekong_hs_id = (SELECT id FROM accommodations WHERE name = 'Mekong Riverside Homestay Cần Thơ' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @mekong_hs_id, 'MRH-STD', 'Phòng Ven Sông', '1 giường đôi', 2, 460000, 6,
           'Phòng hướng sông, có quạt và máy lạnh, phù hợp đi chợ nổi sáng sớm.',
           '/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/rooms/hlr-std.jpg', 'STANDARD'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'MRH-STD')
    UNION ALL
    SELECT @mekong_hs_id, 'MRH-DLX', 'Deluxe Orchard Room', '1 giường King', 2, 680000, 4,
           'Phòng nhìn vườn cây, bao gồm bữa sáng và xe đạp.',
           '/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/rooms/hlr-dlx.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'MRH-DLX')
    UNION ALL
    SELECT @mekong_hs_id, 'MRH-FAM', 'Family Mekong House', '2 giường Queen', 4, 980000, 3,
           'Phòng gia đình có ban công rộng và khu ăn uống chung.',
           '/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/rooms/hlr-fam.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'MRH-FAM');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Grand World Phú Quốc Hotel',
           'Khách sạn gần Grand World và VinWonders, phù hợp gia đình muốn kết hợp vui chơi và nghỉ biển.',
           'Gành Dầu, Phú Quốc', 'Phú Quốc',
           '/assets/images/accommodations/hotel/new-world-sai-gon-hotel/cover.jpg',
           4, 8.7, 392, 'HOTEL', 'APPROVED', @owner_nobank_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Grand World Phú Quốc Hotel');
    SET @grandworld_hotel_id = (SELECT id FROM accommodations WHERE name = 'Grand World Phú Quốc Hotel' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @grandworld_hotel_id, 'GWP-STD', 'Standard Grand Room', '1 giường Queen', 2, 980000, 10,
           'Phòng tiêu chuẩn gần khu vui chơi, phù hợp khách đi cặp đôi.',
           '/assets/images/accommodations/hotel/new-world-sai-gon-hotel/rooms/nws-std.jpg', 'STANDARD'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'GWP-STD')
    UNION ALL
    SELECT @grandworld_hotel_id, 'GWP-DLX', 'Deluxe Park View', '1 giường King', 2, 1460000, 6,
           'Phòng view khu phố lễ hội, có bữa sáng và minibar.',
           '/assets/images/accommodations/hotel/new-world-sai-gon-hotel/rooms/nws-dlx.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'GWP-DLX')
    UNION ALL
    SELECT @grandworld_hotel_id, 'GWP-FAM', 'Family Fun Room', '2 giường Queen', 4, 2250000, 4,
           'Phòng gia đình rộng, thuận tiện di chuyển đến VinWonders.',
           '/assets/images/accommodations/hotel/intercontinental-nha-trang/rooms/icn-sui.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'GWP-FAM');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Mũi Né Sand Dune Villa',
           'Villa gần đồi cát bay Mũi Né, có bếp riêng, hồ bơi nhỏ và sân BBQ cho nhóm bạn.',
           'Hàm Tiến, Phan Thiết', 'Mũi Né',
           '/assets/images/accommodations/villa/sunset-beach-villa-phu-quoc/cover.jpg',
           4, 8.9, 188, 'VILLA', 'APPROVED', @owner_villa_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Mũi Né Sand Dune Villa');
    SET @muine_villa_id = (SELECT id FROM accommodations WHERE name = 'Mũi Né Sand Dune Villa' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @muine_villa_id, 'MSD-DLX', 'Sand Garden Villa', '2 giường Queen', 4, 2400000, 3,
           'Villa 2 phòng ngủ, sân vườn riêng và bếp đầy đủ.',
           '/assets/images/accommodations/villa/pine-hill-villa-da-lat/rooms/phv-dlx.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'MSD-DLX')
    UNION ALL
    SELECT @muine_villa_id, 'MSD-FAM', 'Family Dune Villa', '3 giường Queen', 6, 3600000, 2,
           'Villa rộng cho gia đình hoặc nhóm bạn, có khu BBQ ngoài trời.',
           '/assets/images/accommodations/villa/pine-hill-villa-da-lat/rooms/phv-fam.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'MSD-FAM')
    UNION ALL
    SELECT @muine_villa_id, 'MSD-POOL', 'Private Pool Dune Villa', '2 giường King', 4, 5200000, 1,
           'Villa cao cấp có hồ bơi riêng, phòng khách rộng và ban công.',
           '/assets/images/accommodations/villa/sunset-beach-villa-phu-quoc/rooms/sbv-pool.jpg', 'SUITE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'MSD-POOL');

    -- ─── ĐÀ LẠT: BỔ SUNG ĐỦ RESORT / HOMESTAY / VILLA CHO TAB TÌM KIẾM ─────
    -- Khi người dùng lọc Đà Lạt theo từng loại hình, mỗi tab có ít nhất 3 cơ sở.
    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Ana Mandara Villas Dalat Resort',
           'Khu nghỉ dưỡng biệt thự Pháp cổ giữa rừng thông Đà Lạt, có spa, hồ bơi nước ấm và nhà hàng sân vườn.',
           'Lê Lai, Phường 5', 'Đà Lạt',
           '/assets/images/accommodations/resort/legacy-bay-resort-ha-long/cover.jpg',
           5, 9.1, 428, 'RESORT', 'APPROVED', @owner_resort_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Ana Mandara Villas Dalat Resort');
    SET @ana_dalat_resort_id = (SELECT id FROM accommodations WHERE name = 'Ana Mandara Villas Dalat Resort' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @ana_dalat_resort_id, 'AMR-DLX', 'Deluxe Garden Villa Room', '1 giường King', 2, 2650000, 6,
           'Phòng resort trong biệt thự cổ, nhìn vườn thông và có bữa sáng.',
           '/assets/images/accommodations/resort/legacy-bay-resort-ha-long/rooms/lbr-dlx.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'AMR-DLX')
    UNION ALL
    SELECT @ana_dalat_resort_id, 'AMR-FAM', 'Family Heritage Villa', '2 giường Queen', 4, 4200000, 3,
           'Phòng gia đình trong villa riêng, gần khu spa và hồ bơi.',
           '/assets/images/accommodations/resort/sunset-pearl-resort-phu-quoc/rooms/spq-dlx.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'AMR-FAM')
    UNION ALL
    SELECT @ana_dalat_resort_id, 'AMR-SUI', 'Pine Forest Suite', '1 giường King cỡ lớn', 2, 5600000, 2,
           'Suite có phòng khách riêng, ban công nhìn rừng thông và bồn tắm nằm.',
           '/assets/images/accommodations/resort/legacy-bay-resort-ha-long/rooms/lbr-sui.jpg', 'SUITE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'AMR-SUI');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Terracotta Hotel & Resort Đà Lạt',
           'Resort ven hồ Tuyền Lâm với không gian rộng, phù hợp gia đình nghỉ dưỡng và hội nhóm công ty.',
           'Phân khu chức năng 7.9, Hồ Tuyền Lâm', 'Đà Lạt',
           '/assets/images/accommodations/resort/azerai-can-tho-resort/cover.jpg',
           4, 8.8, 612, 'RESORT', 'APPROVED', @owner_resort_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Terracotta Hotel & Resort Đà Lạt');
    SET @terracotta_resort_id = (SELECT id FROM accommodations WHERE name = 'Terracotta Hotel & Resort Đà Lạt' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @terracotta_resort_id, 'TCR-GDN', 'Garden Deluxe Room', '1 giường King', 2, 1850000, 10,
           'Phòng hướng vườn, gần hồ Tuyền Lâm, bao gồm bữa sáng buffet.',
           '/assets/images/accommodations/resort/azerai-can-tho-resort/rooms/azc-dlx.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'TCR-GDN')
    UNION ALL
    SELECT @terracotta_resort_id, 'TCR-LAK', 'Lake View Suite', '1 giường King cỡ lớn', 2, 3200000, 5,
           'Suite nhìn hồ Tuyền Lâm, có ban công riêng và minibar.',
           '/assets/images/accommodations/resort/azerai-can-tho-resort/rooms/azc-sui.jpg', 'SUITE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'TCR-LAK')
    UNION ALL
    SELECT @terracotta_resort_id, 'TCR-FAM', 'Family Resort Room', '2 giường Queen', 4, 3800000, 4,
           'Phòng gia đình rộng, gần khu vui chơi trẻ em và nhà hàng.',
           '/assets/images/accommodations/resort/sunset-pearl-resort-phu-quoc/rooms/spq-dlx.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'TCR-FAM');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Swiss-Belresort Tuyen Lam Đà Lạt',
           'Resort phong cách châu Âu trên đồi thông, có sân golf, hồ bơi trong nhà và khu nhà hàng nhìn thung lũng.',
           'Khu du lịch hồ Tuyền Lâm', 'Đà Lạt',
           '/assets/images/accommodations/resort/sunset-pearl-resort-phu-quoc/cover.jpg',
           5, 8.9, 537, 'RESORT', 'APPROVED', @owner_resort_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Swiss-Belresort Tuyen Lam Đà Lạt');
    SET @swiss_resort_id = (SELECT id FROM accommodations WHERE name = 'Swiss-Belresort Tuyen Lam Đà Lạt' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @swiss_resort_id, 'SBR-DLX', 'Deluxe Valley Room', '1 giường King', 2, 2200000, 8,
           'Phòng hướng thung lũng, có bàn làm việc và bữa sáng.',
           '/assets/images/accommodations/resort/sunset-pearl-resort-phu-quoc/rooms/spq-dlx.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'SBR-DLX')
    UNION ALL
    SELECT @swiss_resort_id, 'SBR-PRE', 'Premium Golf View', '1 giường King cỡ lớn', 2, 3600000, 4,
           'Phòng premium nhìn sân golf, có bồn tắm nằm và minibar.',
           '/assets/images/accommodations/resort/sunset-pearl-resort-phu-quoc/rooms/spq-sea.jpg', 'VIP'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'SBR-PRE')
    UNION ALL
    SELECT @swiss_resort_id, 'SBR-SUI', 'Executive Pine Suite', '1 giường King + sofa bed', 3, 5200000, 2,
           'Suite rộng có phòng khách riêng, ban công nhìn rừng thông.',
           '/assets/images/accommodations/resort/legacy-bay-resort-ha-long/rooms/lbr-sui.jpg', 'SUITE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'SBR-SUI');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Cầu Đất Farm Homestay',
           'Homestay gần đồi chè Cầu Đất, có khu vườn rau, bếp chung và trải nghiệm săn mây buổi sáng.',
           'Thôn Trường Thọ, Xuân Trường', 'Đà Lạt',
           '/assets/images/homestay-o-da-lat.jpg',
           3, 8.7, 148, 'HOMESTAY', 'APPROVED', @owner_homestay_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Cầu Đất Farm Homestay');
    SET @caudat_hs_id = (SELECT id FROM accommodations WHERE name = 'Cầu Đất Farm Homestay' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @caudat_hs_id, 'CDF-STD', 'Phòng Vườn Cầu Đất', '1 giường đôi', 2, 430000, 6,
           'Phòng riêng nhìn vườn rau, có máy lạnh và bữa sáng.',
           '/assets/images/accommodations/homestay/sapa-valley-homestay/rooms/svh-std.jpg', 'STANDARD'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'CDF-STD')
    UNION ALL
    SELECT @caudat_hs_id, 'CDF-DLX', 'Deluxe Tea Hill Room', '1 giường King', 2, 680000, 4,
           'Phòng có ban công nhìn đồi chè, phù hợp cặp đôi.',
           '/assets/images/accommodations/homestay/sapa-valley-homestay/rooms/svh-dlx.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'CDF-DLX')
    UNION ALL
    SELECT @caudat_hs_id, 'CDF-FAM', 'Family Farm Room', '2 giường Queen', 4, 980000, 3,
           'Phòng gia đình có khu sinh hoạt chung và bếp dùng chung.',
           '/assets/images/accommodations/homestay/tam-coc-garden-homestay/rooms/tcg-fam.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'CDF-FAM');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Hồ Tuyền Lâm Homestay',
           'Homestay nhỏ bên hồ Tuyền Lâm, có sân vườn, bếp chung và dịch vụ thuê xe máy đi các điểm gần hồ.',
           'Đường Hoa Cẩm Tú Cầu, Phường 3', 'Đà Lạt',
           '/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/cover.jpg',
           3, 8.8, 176, 'HOMESTAY', 'APPROVED', @owner_homestay_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Hồ Tuyền Lâm Homestay');
    SET @huyenlam_hs_id = (SELECT id FROM accommodations WHERE name = 'Hồ Tuyền Lâm Homestay' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @huyenlam_hs_id, 'HTL-STD', 'Phòng Hồ Tiêu Chuẩn', '1 giường đôi', 2, 480000, 5,
           'Phòng riêng yên tĩnh, có cửa sổ nhìn sân vườn.',
           '/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/rooms/hlr-std.jpg', 'STANDARD'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'HTL-STD')
    UNION ALL
    SELECT @huyenlam_hs_id, 'HTL-DLX', 'Deluxe Lake Garden', '1 giường King', 2, 760000, 4,
           'Phòng có ban công, bữa sáng tại nhà và ấm đun nước.',
           '/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/rooms/hlr-dlx.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'HTL-DLX')
    UNION ALL
    SELECT @huyenlam_hs_id, 'HTL-FAM', 'Family Lake House', '2 giường Queen', 4, 1120000, 2,
           'Phòng gia đình có khu ăn uống chung và sân vườn riêng.',
           '/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/rooms/hlr-fam.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'HTL-FAM');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Lavender Valley Villa Đà Lạt',
           'Villa riêng trong thung lũng hoa oải hương, có sân BBQ, bếp riêng và phòng khách rộng.',
           'Đường Đống Đa, Phường 3', 'Đà Lạt',
           '/assets/images/accommodations/villa/ba-na-hills-forest-villa/cover.jpg',
           4, 8.9, 203, 'VILLA', 'APPROVED', @owner_villa_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Lavender Valley Villa Đà Lạt');
    SET @lavender_villa_id = (SELECT id FROM accommodations WHERE name = 'Lavender Valley Villa Đà Lạt' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @lavender_villa_id, 'LVV-DLX', 'Lavender Garden Villa', '2 giường Queen', 4, 2800000, 3,
           'Villa 2 phòng ngủ, sân vườn riêng và bếp đầy đủ.',
           '/assets/images/accommodations/villa/ba-na-hills-forest-villa/rooms/bnh-bng.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'LVV-DLX')
    UNION ALL
    SELECT @lavender_villa_id, 'LVV-FAM', 'Family Lavender House', '3 giường Queen', 6, 4200000, 2,
           'Căn villa cho gia đình lớn, có khu BBQ và phòng khách riêng.',
           '/assets/images/accommodations/villa/pine-hill-villa-da-lat/rooms/phv-fam.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'LVV-FAM')
    UNION ALL
    SELECT @lavender_villa_id, 'LVV-SUI', 'Valley Suite Villa', '2 giường King', 4, 5600000, 1,
           'Villa cao cấp nhìn thung lũng, có bồn tắm nằm và ban công.',
           '/assets/images/accommodations/villa/ba-na-hills-forest-villa/rooms/bnh-sui.jpg', 'SUITE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'LVV-SUI');

    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at)
    SELECT 'Tuyền Lâm Lake Villa',
           'Villa ven hồ Tuyền Lâm dành cho nhóm bạn và gia đình, có hồ bơi riêng, bếp và sân ngắm hoàng hôn.',
           'Khu du lịch hồ Tuyền Lâm', 'Đà Lạt',
           '/assets/images/accommodations/villa/sunset-beach-villa-phu-quoc/cover.jpg',
           5, 9.2, 257, 'VILLA', 'APPROVED', @owner_villa_id, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = 'Tuyền Lâm Lake Villa');
    SET @tuyenlam_villa_id = (SELECT id FROM accommodations WHERE name = 'Tuyền Lâm Lake Villa' LIMIT 1);
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category)
    SELECT @tuyenlam_villa_id, 'TLV-DLX', 'Lake Garden Villa', '2 giường Queen', 4, 3600000, 3,
           'Villa 2 phòng ngủ nhìn hồ, có bếp riêng và sân vườn.',
           '/assets/images/accommodations/villa/sunset-beach-villa-phu-quoc/rooms/sbv-sea.jpg', 'DELUXE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'TLV-DLX')
    UNION ALL
    SELECT @tuyenlam_villa_id, 'TLV-FAM', 'Family Lake Villa', '3 giường Queen', 6, 5200000, 2,
           'Villa cho gia đình lớn, phòng khách rộng và sân BBQ.',
           '/assets/images/accommodations/villa/pine-hill-villa-da-lat/rooms/phv-fam.jpg', 'FAMILY'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'TLV-FAM')
    UNION ALL
    SELECT @tuyenlam_villa_id, 'TLV-POOL', 'Private Pool Lake Villa', '2 giường King', 4, 7200000, 1,
           'Villa cao cấp có hồ bơi riêng, ban công nhìn hồ Tuyền Lâm.',
           '/assets/images/accommodations/villa/sunset-beach-villa-phu-quoc/rooms/sbv-pool.jpg', 'SUITE'
    WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'TLV-POOL');

    -- Gắn tiện nghi cho các phòng/căn được tạo ở phần mở rộng cuối file.
    -- Block này chạy sau khi các room_code bên dưới đã tồn tại trong bảng rooms.
    INSERT IGNORE INTO room_amenities (room_id, amenity_id)
    SELECT r.id, a.id
    FROM rooms r
    JOIN accommodations ac ON ac.id = r.accommodation_id
    JOIN amenities a ON 1=1
    WHERE r.room_code IN (
        'ICN-STD','ICN-DLX','ICN-SUI',
        'NVD-STD','NVD-DLX','NVD-FAM',
        'LSH-STD','LSH-DLX','LSH-SUI',
        'SLM-PRE','SLM-GRA','SLM-FAM',
        'SPC-STD','SPC-DLX',
        'SPQ-DLX','SPQ-SEA',
        'PHV-DLX','PHV-FAM',
        'SBV-SEA','SBV-POOL',
        'TCG-STD','TCG-FAM',
        'SVH-STD','SVH-DLX',
        'LBR-DLX','LBR-SUI',
        'NWS-STD','NWS-DLX',
        'PVT-STD','PVT-DLX',
        'AZC-DLX','AZC-SUI',
        'TTC-DLX','TTC-FAM',
        'LCH-STD','LCH-DLX','LCH-FAM',
        'BHV-STD','BHV-DLX','BHV-FAM',
        'DVS-STD','DVS-DLX','DVS-SUI',
        'MPL-STD','MPL-DLX','MPL-FAM',
        'MCC-STD','MCC-DLX','MCC-FAM',
        'TLR-DLX','TLR-SUI','TLR-FAM',
        'MCT-STD','MCT-DLX','MCT-FAM',
        'MCF-STD','MCF-DLX','MCF-FAM',
        'TRV-DLX','TRV-FAM','TRV-SUI',
        'HLM-STD','HLM-DLX','HLM-SUI',
        'VBF-STD','VBF-DLX','VBF-FAM',
        'MRH-STD','MRH-DLX','MRH-FAM',
        'GWP-STD','GWP-DLX','GWP-FAM',
        'MSD-DLX','MSD-FAM','MSD-POOL',
        'AMR-DLX','AMR-FAM','AMR-SUI',
        'TCR-GDN','TCR-LAK','TCR-FAM',
        'SBR-DLX','SBR-PRE','SBR-SUI',
        'CDF-STD','CDF-DLX','CDF-FAM',
        'HTL-STD','HTL-DLX','HTL-FAM',
        'LVV-DLX','LVV-FAM','LVV-SUI',
        'TLV-DLX','TLV-FAM','TLV-POOL'
    )
      AND (
        a.name IN ('WiFi miễn phí','Bãi đỗ xe','Máy lạnh','TV màn hình phẳng',
                   'Phòng tắm riêng','Máy sấy tóc','Đồ dùng cá nhân')
        OR (ac.property_type = 'HOTEL'
            AND a.name IN ('Nhà hàng','Bar / Café','Thang máy','Tủ lạnh mini',
                           'Bàn làm việc','Bữa sáng miễn phí'))
        OR (ac.property_type = 'RESORT'
            AND a.name IN ('Hồ bơi chung','Nhà hàng','Spa / Massage','Phòng gym',
                           'Bar / Café','Minibar','Két an toàn','Bữa sáng miễn phí',
                           'Ban công riêng'))
        OR (ac.property_type = 'VILLA'
            AND a.name IN ('Bếp riêng','Tủ lạnh đầy đủ','Lò vi sóng',
                           'Ấm đun nước','BBQ / Bếp nướng','Sân vườn riêng',
                           'Ban công riêng'))
        OR (ac.property_type = 'HOMESTAY'
            AND a.name IN ('Bữa sáng miễn phí','Ấm đun nước','Sân vườn riêng',
                           'Ban công riêng'))
        OR (r.room_category IN ('DELUXE','VIP','SUITE')
            AND a.name IN ('Minibar','Két an toàn','Ổ cắm quốc tế','Bồn tắm nằm',
                           'Vòi sen đứng','Máy pha cà phê'))
        OR (r.room_category IN ('FAMILY','SUITE')
            AND a.name IN ('Tủ lạnh đầy đủ','Bếp riêng'))
        OR (r.room_code IN ('ICN-DLX','ICN-SUI','SPQ-SEA','PVT-STD',
                            'LBR-DLX','LBR-SUI','TTC-DLX',
                            'HLM-DLX','HLM-SUI','VBF-DLX','VBF-FAM',
                            'GWP-DLX','GWP-FAM','MSD-DLX','MSD-FAM','MSD-POOL')
            AND a.name = 'View biển')
        OR (r.room_code IN ('SPC-STD','SPC-DLX','SVH-STD','SVH-DLX',
                            'TCG-STD','TCG-FAM',
                            'BHV-STD','BHV-DLX','BHV-FAM',
                            'DVS-STD','DVS-DLX','DVS-SUI',
                            'MPL-STD','MPL-DLX','MPL-FAM',
                            'MCC-STD','MCC-DLX','MCC-FAM',
                            'TLR-DLX','TLR-SUI','TLR-FAM',
                            'MCT-STD','MCT-DLX','MCT-FAM',
                            'MCF-STD','MCF-DLX','MCF-FAM',
                            'TRV-DLX','TRV-FAM','TRV-SUI',
                            'AMR-DLX','AMR-FAM','AMR-SUI',
                            'TCR-GDN','TCR-LAK','TCR-FAM',
                            'SBR-DLX','SBR-PRE','SBR-SUI',
                            'CDF-STD','CDF-DLX','CDF-FAM',
                            'HTL-STD','HTL-DLX','HTL-FAM',
                            'LVV-DLX','LVV-FAM','LVV-SUI',
                            'TLV-DLX','TLV-FAM','TLV-POOL')
            AND a.name = 'View núi / đồi')
        OR (r.room_code IN ('SPQ-DLX','AZC-DLX','AZC-SUI','PHV-DLX','PHV-FAM',
                            'SBV-SEA','SBV-POOL','TCG-STD','TCG-FAM','TTC-FAM',
                            'BHV-STD','BHV-DLX','BHV-FAM',
                            'MPL-STD','MPL-DLX','MPL-FAM',
                            'MCC-STD','MCC-DLX','MCC-FAM',
                            'TLR-DLX','TLR-SUI','TLR-FAM',
                            'MCF-STD','MCF-DLX','MCF-FAM',
                            'TRV-DLX','TRV-FAM','TRV-SUI',
                            'VBF-STD','VBF-DLX','VBF-FAM',
                            'MRH-STD','MRH-DLX','MRH-FAM',
                            'MSD-DLX','MSD-FAM','MSD-POOL',
                            'AMR-DLX','AMR-FAM','AMR-SUI',
                            'TCR-GDN','TCR-LAK','TCR-FAM',
                            'SBR-DLX','SBR-PRE','SBR-SUI',
                            'CDF-STD','CDF-DLX','CDF-FAM',
                            'HTL-STD','HTL-DLX','HTL-FAM',
                            'LVV-DLX','LVV-FAM','LVV-SUI',
                            'TLV-DLX','TLV-FAM','TLV-POOL')
            AND a.name = 'Sân vườn riêng')
        OR (r.room_code IN ('SBV-POOL','AZC-SUI','TRV-SUI','MSD-POOL','TLV-POOL')
            AND a.name = 'Hồ bơi riêng')
      );

    -- ─── CHUẨN HÓA ẢNH LƯU TRÚ: mỗi nơi lưu trú/phòng có src local riêng ───
    UPDATE accommodations
    SET thumbnail_url = CASE name
        WHEN 'Tulip Hotel 2 Dalat' THEN '/assets/images/accommodations/hotel/tulip-hotel-2-dalat/cover.jpg'
        WHEN 'TravelMate Grand Hotel' THEN '/assets/images/accommodations/hotel/travelmate-grand-hotel/cover.jpg'
        WHEN 'Da Lat Mountain Boutique Hotel' THEN '/assets/images/accommodations/hotel/da-lat-mountain-boutique-hotel/cover.jpg'
        WHEN 'Da Lat Sunrise Guesthouse' THEN '/assets/images/accommodations/hotel/da-lat-sunrise-guesthouse/cover.jpg'
        WHEN 'InterContinental Nha Trang' THEN '/assets/images/accommodations/hotel/intercontinental-nha-trang/cover.jpg'
        WHEN 'Novotel Đà Nẵng Premier' THEN '/assets/images/accommodations/hotel/novotel-da-nang-premier/cover.jpg'
        WHEN 'La Siesta Hội An Resort & Spa' THEN '/assets/images/accommodations/hotel/la-siesta-hoi-an-resort-spa/cover.jpg'
        WHEN 'Sofitel Legend Metropole Hà Nội' THEN '/assets/images/accommodations/hotel/sofitel-legend-metropole-ha-noi/cover.jpg'
        WHEN 'Sapa Cloud Valley Hotel' THEN '/assets/images/accommodations/hotel/sapa-cloud-valley-hotel/cover.jpg'
        WHEN 'New World Sài Gòn Hotel' THEN '/assets/images/accommodations/hotel/new-world-sai-gon-hotel/cover.jpg'
        WHEN 'Pullman Vũng Tàu' THEN '/assets/images/accommodations/hotel/pullman-vung-tau/cover.jpg'
        WHEN 'The Anam Villa Nha Trang' THEN '/assets/images/accommodations/villa/the-anam-villa-nha-trang/cover.jpg'
        WHEN 'Ba Na Hills Forest Villa' THEN '/assets/images/accommodations/villa/ba-na-hills-forest-villa/cover.jpg'
        WHEN 'Pine Hill Villa Đà Lạt' THEN '/assets/images/accommodations/villa/pine-hill-villa-da-lat/cover.jpg'
        WHEN 'Sunset Beach Villa Phú Quốc' THEN '/assets/images/accommodations/villa/sunset-beach-villa-phu-quoc/cover.jpg'
        WHEN 'Hoa Lư Riverside Homestay' THEN '/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/cover.jpg'
        WHEN 'Hoa Lu Riverside Homestay' THEN '/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/cover.jpg'
        WHEN 'Mộc Nhiên Garden Homestay Đà Lạt' THEN '/assets/images/accommodations/homestay/moc-nhien-garden-homestay-da-lat/cover.jpg'
        WHEN 'Tam Cốc Garden Homestay' THEN '/assets/images/accommodations/homestay/tam-coc-garden-homestay/cover.jpg'
        WHEN 'Sapa Valley Homestay' THEN '/assets/images/accommodations/homestay/sapa-valley-homestay/cover.jpg'
        WHEN 'Vinpearl Resort & Spa Nha Trang' THEN '/assets/images/accommodations/resort/vinpearl-resort-spa-nha-trang/cover.jpg'
        WHEN 'Furama Resort Đà Nẵng' THEN '/assets/images/accommodations/resort/furama-resort-da-nang/cover.jpg'
        WHEN 'Sunset Pearl Resort Phú Quốc' THEN '/assets/images/accommodations/resort/sunset-pearl-resort-phu-quoc/cover.jpg'
        WHEN 'Legacy Bay Resort Hạ Long' THEN '/assets/images/accommodations/resort/legacy-bay-resort-ha-long/cover.jpg'
        WHEN 'Azerai Cần Thơ Resort' THEN '/assets/images/accommodations/resort/azerai-can-tho-resort/cover.jpg'
        WHEN 'TTC Resort Mũi Né' THEN '/assets/images/accommodations/resort/ttc-resort-mui-ne/cover.jpg'
        ELSE thumbnail_url
    END
    WHERE name IN ('Tulip Hotel 2 Dalat', 'TravelMate Grand Hotel', 'Da Lat Mountain Boutique Hotel', 'Da Lat Sunrise Guesthouse', 'InterContinental Nha Trang', 'Novotel Đà Nẵng Premier', 'La Siesta Hội An Resort & Spa', 'Sofitel Legend Metropole Hà Nội', 'Sapa Cloud Valley Hotel', 'New World Sài Gòn Hotel', 'Pullman Vũng Tàu', 'The Anam Villa Nha Trang', 'Ba Na Hills Forest Villa', 'Pine Hill Villa Đà Lạt', 'Sunset Beach Villa Phú Quốc', 'Hoa Lư Riverside Homestay', 'Hoa Lu Riverside Homestay', 'Mộc Nhiên Garden Homestay Đà Lạt', 'Tam Cốc Garden Homestay', 'Sapa Valley Homestay', 'Vinpearl Resort & Spa Nha Trang', 'Furama Resort Đà Nẵng', 'Sunset Pearl Resort Phú Quốc', 'Legacy Bay Resort Hạ Long', 'Azerai Cần Thơ Resort', 'TTC Resort Mũi Né');

    UPDATE rooms
    SET image_url = CASE room_code
        WHEN 'TLP-STD' THEN '/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/tlp-std.jpg'
        WHEN 'TLP-SUP' THEN '/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/tlp-sup.jpg'
        WHEN 'TLP-FAM' THEN '/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/tlp-fam.jpg'
        WHEN 'TLP-VIP' THEN '/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/tlp-vip.jpg'
        WHEN 'R201' THEN '/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/r201.jpg'
        WHEN 'R202' THEN '/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/r202.jpg'
        WHEN 'R203' THEN '/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/r203.jpg'
        WHEN 'R204' THEN '/assets/images/accommodations/hotel/tulip-hotel-2-dalat/rooms/r204.jpg'
        WHEN 'TMG-DLX' THEN '/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/tmg-dlx.jpg'
        WHEN 'TMG-PRE' THEN '/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/tmg-pre.jpg'
        WHEN 'TMG-FAM' THEN '/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/tmg-fam.jpg'
        WHEN 'TMG-PRE2' THEN '/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/tmg-pre2.jpg'
        WHEN 'R301' THEN '/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/r301.jpg'
        WHEN 'R302' THEN '/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/r302.jpg'
        WHEN 'R303' THEN '/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/r303.jpg'
        WHEN 'R304' THEN '/assets/images/accommodations/hotel/travelmate-grand-hotel/rooms/r304.jpg'
        WHEN 'ICN-STD' THEN '/assets/images/accommodations/hotel/intercontinental-nha-trang/rooms/icn-std.jpg'
        WHEN 'ICN-DLX' THEN '/assets/images/accommodations/hotel/intercontinental-nha-trang/rooms/icn-dlx.jpg'
        WHEN 'ICN-SUI' THEN '/assets/images/accommodations/hotel/intercontinental-nha-trang/rooms/icn-sui.jpg'
        WHEN 'NVD-STD' THEN '/assets/images/accommodations/hotel/novotel-da-nang-premier/rooms/nvd-std.jpg'
        WHEN 'NVD-DLX' THEN '/assets/images/accommodations/hotel/novotel-da-nang-premier/rooms/nvd-dlx.jpg'
        WHEN 'NVD-FAM' THEN '/assets/images/accommodations/hotel/novotel-da-nang-premier/rooms/nvd-fam.jpg'
        WHEN 'LSH-STD' THEN '/assets/images/accommodations/hotel/la-siesta-hoi-an-resort-spa/rooms/lsh-std.jpg'
        WHEN 'LSH-DLX' THEN '/assets/images/accommodations/hotel/la-siesta-hoi-an-resort-spa/rooms/lsh-dlx.jpg'
        WHEN 'LSH-SUI' THEN '/assets/images/accommodations/hotel/la-siesta-hoi-an-resort-spa/rooms/lsh-sui.jpg'
        WHEN 'SLM-PRE' THEN '/assets/images/accommodations/hotel/sofitel-legend-metropole-ha-noi/rooms/slm-pre.jpg'
        WHEN 'SLM-GRA' THEN '/assets/images/accommodations/hotel/sofitel-legend-metropole-ha-noi/rooms/slm-gra.jpg'
        WHEN 'SLM-FAM' THEN '/assets/images/accommodations/hotel/sofitel-legend-metropole-ha-noi/rooms/slm-fam.jpg'
        WHEN 'SPC-STD' THEN '/assets/images/accommodations/hotel/sapa-cloud-valley-hotel/rooms/spc-std.jpg'
        WHEN 'SPC-DLX' THEN '/assets/images/accommodations/hotel/sapa-cloud-valley-hotel/rooms/spc-dlx.jpg'
        WHEN 'NWS-STD' THEN '/assets/images/accommodations/hotel/new-world-sai-gon-hotel/rooms/nws-std.jpg'
        WHEN 'NWS-DLX' THEN '/assets/images/accommodations/hotel/new-world-sai-gon-hotel/rooms/nws-dlx.jpg'
        WHEN 'PVT-STD' THEN '/assets/images/accommodations/hotel/pullman-vung-tau/rooms/pvt-std.jpg'
        WHEN 'PVT-DLX' THEN '/assets/images/accommodations/hotel/pullman-vung-tau/rooms/pvt-dlx.jpg'
        WHEN 'ANM-GDN' THEN '/assets/images/accommodations/villa/the-anam-villa-nha-trang/rooms/anm-gdn.jpg'
        WHEN 'ANM-BCH' THEN '/assets/images/accommodations/villa/the-anam-villa-nha-trang/rooms/anm-bch.jpg'
        WHEN 'ANM-FAM' THEN '/assets/images/accommodations/villa/the-anam-villa-nha-trang/rooms/anm-fam.jpg'
        WHEN 'BNH-BNG' THEN '/assets/images/accommodations/villa/ba-na-hills-forest-villa/rooms/bnh-bng.jpg'
        WHEN 'BNH-TWN' THEN '/assets/images/accommodations/villa/ba-na-hills-forest-villa/rooms/bnh-twn.jpg'
        WHEN 'BNH-SUI' THEN '/assets/images/accommodations/villa/ba-na-hills-forest-villa/rooms/bnh-sui.jpg'
        WHEN 'V101' THEN '/assets/images/accommodations/villa/ba-na-hills-forest-villa/rooms/v101.jpg'
        WHEN 'V102' THEN '/assets/images/accommodations/villa/ba-na-hills-forest-villa/rooms/v102.jpg'
        WHEN 'PHV-DLX' THEN '/assets/images/accommodations/villa/pine-hill-villa-da-lat/rooms/phv-dlx.jpg'
        WHEN 'PHV-FAM' THEN '/assets/images/accommodations/villa/pine-hill-villa-da-lat/rooms/phv-fam.jpg'
        WHEN 'SBV-SEA' THEN '/assets/images/accommodations/villa/sunset-beach-villa-phu-quoc/rooms/sbv-sea.jpg'
        WHEN 'SBV-POOL' THEN '/assets/images/accommodations/villa/sunset-beach-villa-phu-quoc/rooms/sbv-pool.jpg'
        WHEN 'HLR-STD' THEN '/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/rooms/hlr-std.jpg'
        WHEN 'HLR-DLX' THEN '/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/rooms/hlr-dlx.jpg'
        WHEN 'HLR-FAM' THEN '/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/rooms/hlr-fam.jpg'
        WHEN 'HS101' THEN '/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/rooms/hs101.jpg'
        WHEN 'HS102' THEN '/assets/images/accommodations/homestay/hoa-lu-riverside-homestay/rooms/hs102.jpg'
        WHEN 'MND-STD' THEN '/assets/images/accommodations/homestay/moc-nhien-garden-homestay-da-lat/rooms/mnd-std.jpg'
        WHEN 'MND-ATT' THEN '/assets/images/accommodations/homestay/moc-nhien-garden-homestay-da-lat/rooms/mnd-att.jpg'
        WHEN 'MND-FAM' THEN '/assets/images/accommodations/homestay/moc-nhien-garden-homestay-da-lat/rooms/mnd-fam.jpg'
        WHEN 'TCG-STD' THEN '/assets/images/accommodations/homestay/tam-coc-garden-homestay/rooms/tcg-std.jpg'
        WHEN 'TCG-FAM' THEN '/assets/images/accommodations/homestay/tam-coc-garden-homestay/rooms/tcg-fam.jpg'
        WHEN 'SVH-STD' THEN '/assets/images/accommodations/homestay/sapa-valley-homestay/rooms/svh-std.jpg'
        WHEN 'SVH-DLX' THEN '/assets/images/accommodations/homestay/sapa-valley-homestay/rooms/svh-dlx.jpg'
        WHEN 'VNT-DLX' THEN '/assets/images/accommodations/resort/vinpearl-resort-spa-nha-trang/rooms/vnt-dlx.jpg'
        WHEN 'VNT-SUI' THEN '/assets/images/accommodations/resort/vinpearl-resort-spa-nha-trang/rooms/vnt-sui.jpg'
        WHEN 'VNT-VIL' THEN '/assets/images/accommodations/resort/vinpearl-resort-spa-nha-trang/rooms/vnt-vil.jpg'
        WHEN 'FDN-DLX' THEN '/assets/images/accommodations/resort/furama-resort-da-nang/rooms/fdn-dlx.jpg'
        WHEN 'FDN-BCH' THEN '/assets/images/accommodations/resort/furama-resort-da-nang/rooms/fdn-bch.jpg'
        WHEN 'FDN-FAM' THEN '/assets/images/accommodations/resort/furama-resort-da-nang/rooms/fdn-fam.jpg'
        WHEN 'RS101' THEN '/assets/images/accommodations/resort/furama-resort-da-nang/rooms/rs101.jpg'
        WHEN 'RS102' THEN '/assets/images/accommodations/resort/furama-resort-da-nang/rooms/rs102.jpg'
        WHEN 'SPQ-DLX' THEN '/assets/images/accommodations/resort/sunset-pearl-resort-phu-quoc/rooms/spq-dlx.jpg'
        WHEN 'SPQ-SEA' THEN '/assets/images/accommodations/resort/sunset-pearl-resort-phu-quoc/rooms/spq-sea.jpg'
        WHEN 'LBR-DLX' THEN '/assets/images/accommodations/resort/legacy-bay-resort-ha-long/rooms/lbr-dlx.jpg'
        WHEN 'LBR-SUI' THEN '/assets/images/accommodations/resort/legacy-bay-resort-ha-long/rooms/lbr-sui.jpg'
        WHEN 'AZC-DLX' THEN '/assets/images/accommodations/resort/azerai-can-tho-resort/rooms/azc-dlx.jpg'
        WHEN 'AZC-SUI' THEN '/assets/images/accommodations/resort/azerai-can-tho-resort/rooms/azc-sui.jpg'
        WHEN 'TTC-DLX' THEN '/assets/images/accommodations/resort/ttc-resort-mui-ne/rooms/ttc-dlx.jpg'
        WHEN 'TTC-FAM' THEN '/assets/images/accommodations/resort/ttc-resort-mui-ne/rooms/ttc-fam.jpg'
        ELSE image_url
    END
    WHERE room_code IN ('TLP-STD', 'TLP-SUP', 'TLP-FAM', 'TLP-VIP', 'R201', 'R202', 'R203', 'R204', 'TMG-DLX', 'TMG-PRE', 'TMG-FAM', 'TMG-PRE2', 'R301', 'R302', 'R303', 'R304', 'ICN-STD', 'ICN-DLX', 'ICN-SUI', 'NVD-STD', 'NVD-DLX', 'NVD-FAM', 'LSH-STD', 'LSH-DLX', 'LSH-SUI', 'SLM-PRE', 'SLM-GRA', 'SLM-FAM', 'SPC-STD', 'SPC-DLX', 'NWS-STD', 'NWS-DLX', 'PVT-STD', 'PVT-DLX', 'ANM-GDN', 'ANM-BCH', 'ANM-FAM', 'BNH-BNG', 'BNH-TWN', 'BNH-SUI', 'V101', 'V102', 'PHV-DLX', 'PHV-FAM', 'SBV-SEA', 'SBV-POOL', 'HLR-STD', 'HLR-DLX', 'HLR-FAM', 'HS101', 'HS102', 'MND-STD', 'MND-ATT', 'MND-FAM', 'TCG-STD', 'TCG-FAM', 'SVH-STD', 'SVH-DLX', 'VNT-DLX', 'VNT-SUI', 'VNT-VIL', 'FDN-DLX', 'FDN-BCH', 'FDN-FAM', 'RS101', 'RS102', 'SPQ-DLX', 'SPQ-SEA', 'LBR-DLX', 'LBR-SUI', 'AZC-DLX', 'AZC-SUI', 'TTC-DLX', 'TTC-FAM');

    UPDATE room_images ri
    JOIN rooms r ON r.id = ri.room_id
    SET ri.image_url = r.image_url,
        ri.caption = CONCAT(r.room_name, ' - ảnh đại diện'),
        ri.updated_at = NOW()
    WHERE ri.is_primary = 1
      AND r.room_code IN ('TLP-STD', 'TLP-SUP', 'TLP-FAM', 'TLP-VIP', 'R201', 'R202', 'R203', 'R204', 'TMG-DLX', 'TMG-PRE', 'TMG-FAM', 'TMG-PRE2', 'R301', 'R302', 'R303', 'R304', 'ICN-STD', 'ICN-DLX', 'ICN-SUI', 'NVD-STD', 'NVD-DLX', 'NVD-FAM', 'LSH-STD', 'LSH-DLX', 'LSH-SUI', 'SLM-PRE', 'SLM-GRA', 'SLM-FAM', 'SPC-STD', 'SPC-DLX', 'NWS-STD', 'NWS-DLX', 'PVT-STD', 'PVT-DLX', 'ANM-GDN', 'ANM-BCH', 'ANM-FAM', 'BNH-BNG', 'BNH-TWN', 'BNH-SUI', 'V101', 'V102', 'PHV-DLX', 'PHV-FAM', 'SBV-SEA', 'SBV-POOL', 'HLR-STD', 'HLR-DLX', 'HLR-FAM', 'HS101', 'HS102', 'MND-STD', 'MND-ATT', 'MND-FAM', 'TCG-STD', 'TCG-FAM', 'SVH-STD', 'SVH-DLX', 'VNT-DLX', 'VNT-SUI', 'VNT-VIL', 'FDN-DLX', 'FDN-BCH', 'FDN-FAM', 'RS101', 'RS102', 'SPQ-DLX', 'SPQ-SEA', 'LBR-DLX', 'LBR-SUI', 'AZC-DLX', 'AZC-SUI', 'TTC-DLX', 'TTC-FAM');

    INSERT INTO room_images (room_id, image_url, caption, sort_order, is_primary, created_at, updated_at)
    SELECT r.id, r.image_url, CONCAT(r.room_name, ' - ảnh đại diện'), 0, 1, NOW(), NOW()
    FROM rooms r
    WHERE r.room_code IN ('TLP-STD', 'TLP-SUP', 'TLP-FAM', 'TLP-VIP', 'R201', 'R202', 'R203', 'R204', 'TMG-DLX', 'TMG-PRE', 'TMG-FAM', 'TMG-PRE2', 'R301', 'R302', 'R303', 'R304', 'ICN-STD', 'ICN-DLX', 'ICN-SUI', 'NVD-STD', 'NVD-DLX', 'NVD-FAM', 'LSH-STD', 'LSH-DLX', 'LSH-SUI', 'SLM-PRE', 'SLM-GRA', 'SLM-FAM', 'SPC-STD', 'SPC-DLX', 'NWS-STD', 'NWS-DLX', 'PVT-STD', 'PVT-DLX', 'ANM-GDN', 'ANM-BCH', 'ANM-FAM', 'BNH-BNG', 'BNH-TWN', 'BNH-SUI', 'V101', 'V102', 'PHV-DLX', 'PHV-FAM', 'SBV-SEA', 'SBV-POOL', 'HLR-STD', 'HLR-DLX', 'HLR-FAM', 'HS101', 'HS102', 'MND-STD', 'MND-ATT', 'MND-FAM', 'TCG-STD', 'TCG-FAM', 'SVH-STD', 'SVH-DLX', 'VNT-DLX', 'VNT-SUI', 'VNT-VIL', 'FDN-DLX', 'FDN-BCH', 'FDN-FAM', 'RS101', 'RS102', 'SPQ-DLX', 'SPQ-SEA', 'LBR-DLX', 'LBR-SUI', 'AZC-DLX', 'AZC-SUI', 'TTC-DLX', 'TTC-FAM')
      AND r.image_url IS NOT NULL AND r.image_url <> ''
      AND NOT EXISTS (
          SELECT 1 FROM room_images ri
          WHERE ri.room_id = r.id AND ri.is_primary = 1
      );

    -- Mỗi phòng/căn dùng đúng 3 ảnh local trong modal "Xem chi tiết phòng".
    DELETE ri FROM room_images ri
    JOIN rooms r ON r.id = ri.room_id
    WHERE r.image_url LIKE '/assets/images/accommodations/%.jpg';

    INSERT INTO room_images (room_id, image_url, caption, sort_order, is_primary, created_at, updated_at)
    SELECT r.id,
           CASE
               WHEN g.sort_order = 0 THEN r.image_url
               ELSE REPLACE(r.image_url, '.jpg', CONCAT(g.detail_suffix, '.jpg'))
           END AS image_url,
           CONCAT(r.room_name, ' - ảnh ', g.sort_order + 1),
           g.sort_order,
           g.is_primary,
           NOW(),
           NOW()
    FROM rooms r
    JOIN (
        SELECT 0 AS sort_order, 1 AS is_primary, '' AS detail_suffix
        UNION ALL SELECT 1, 0, '-detail-2'
        UNION ALL SELECT 2, 0, '-detail-3'
    ) g
    WHERE r.image_url LIKE '/assets/images/accommodations/%.jpg';

    -- ─── TRAVEL POSTS BỔ SUNG CHO MIỀN NAM ──────────────────────────────────
    INSERT INTO travel_posts (title, destination_name, destination_slug, summary, content, thumbnail_url, source_name, source_url, category, status, created_by, created_at, updated_at) VALUES
    ('Khám phá Vũng Tàu trong 2 ngày 1 đêm', 'Vũng Tàu', 'vung-tau',
     'Gợi ý lịch trình cuối tuần đến thành phố biển gần Sài Gòn nhất, kết hợp nghỉ dưỡng và hải sản.',
     'Vũng Tàu cách TP.HCM khoảng 2 tiếng lái xe, phù hợp chuyến đi cuối tuần. Tắm biển, thăm Hải Đăng, ăn hải sản tươi và nghỉ tại khách sạn ven biển Thùy Vân.',
     '/assets/images/travel-posts/vung-tau-guide.jpg',
     'Vietnam.travel', 'https://vietnam.travel/places-to-go/southern-vietnam/vung-tau',
     'GUIDE', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
    ('Trải nghiệm chợ nổi Cái Răng — Cần Thơ', 'Cần Thơ', 'can-tho',
     'Hướng dẫn khám phá chợ nổi nổi tiếng và ẩm thực đặc trưng miền Tây sông nước.',
     'Cần Thơ là thủ phủ miền Tây. Chợ nổi Cái Răng họp từ sáng sớm đến 8-9h. Du khách nên thuê thuyền nhỏ để có trải nghiệm chân thực và chụp ảnh đẹp.',
     '/assets/images/travel-posts/can-tho-guide.jpg',
     'Vietnam.travel', 'https://vietnam.travel/places-to-go/southern-vietnam/can-tho',
     'ATTRACTION', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
    ('Mũi Né — thiên đường kite surf và đồi cát', 'Mũi Né', 'mui-ne',
     'Điểm đến biển nổi tiếng cho thể thao nước và kỳ nghỉ biển khác biệt.',
     'Mũi Né nổi tiếng với đồi cát đỏ, đồi cát trắng và làn gió lý tưởng cho kitesurfing. Resort tập trung trên đường Nguyễn Đình Chiểu dài 10km ven biển.',
     '/assets/images/travel-posts/mui-ne-must-do.jpg',
     'Vietnam.travel', 'https://vietnam.travel/places-to-go/south-central-coast/mui-ne',
     'GUIDE', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),
    ('Sài Gòn về đêm — lịch trình ăn uống và khám phá', 'TP. Hồ Chí Minh', 'ho-chi-minh',
     'Những điểm ăn ngon và vui chơi về đêm tại thành phố năng động nhất Việt Nam.',
     'TP.HCM về đêm sống động với phố đi bộ Nguyễn Huệ, chợ Bến Thành, ẩm thực đường phố Bùi Viện và các quán cà phê sân thượng hiện đại.',
     '/assets/images/travel-posts/ho-chi-minh-nightlife.jpg',
     'Vietnam.travel', 'https://vietnam.travel/things-to-do/ho-chi-minh-city-at-night',
     'ESSENTIAL', 'VISIBLE', 'admin@travelmate.vn', DATE_SUB(NOW(), INTERVAL 6 DAY), NOW())
    ON DUPLICATE KEY UPDATE
        title = VALUES(title),
        destination_name = VALUES(destination_name),
        destination_slug = VALUES(destination_slug),
        summary = VALUES(summary),
        content = VALUES(content),
        thumbnail_url = VALUES(thumbnail_url),
        source_name = VALUES(source_name),
        category = VALUES(category),
        status = VALUES(status),
        created_by = VALUES(created_by),
        updated_at = VALUES(updated_at);

    -- =============================================
    -- DỮ LIỆU VẬN HÀNH — COVERAGE TRẠNG THÁI VNPAY / REFUND
    -- Mục đích: các màn hình booking, payment và chatbot có đủ trạng thái để đối chiếu.
    -- =============================================

    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        remaining_payment_status, expire_at,
        note, created_at, updated_at)
    VALUES (
        'BK-OPS-PENDPAY-01', 2, 1, 1,
        DATE_ADD(CURDATE(), INTERVAL 14 DAY), DATE_ADD(CURDATE(), INTERVAL 16 DAY),
        2, 0, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        1300000, 390000, 910000,
        'PENDING_PAYMENT', 'DEPOSIT_30', 'PENDING_PAYMENT',
        'UNPAID', DATE_ADD(NOW(), INTERVAL 3 MINUTE),
        'Khách đang ở cổng VNPAY; hệ thống giữ phòng tạm trong thời hạn thanh toán.',
        NOW(), NOW()
    ) ON DUPLICATE KEY UPDATE updated_at = NOW();

    INSERT INTO payments (booking_id, payment_method, payment_option, amount,
        transaction_code, payment_status, gateway, vnp_txn_ref, expire_at, note)
    SELECT b.id, 'VNPAY', 'DEPOSIT_30', 390000,
           'TXN-OPS-PENDPAY-01', 'PENDING_PAYMENT', 'VNPAY', 'TM-OPS-PENDPAY-01',
           b.expire_at, 'Payment mới tạo, chờ khách hoàn tất giao dịch VNPAY.'
    FROM bookings b
    WHERE b.booking_code = 'BK-OPS-PENDPAY-01'
      AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.booking_id = b.id);

    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        remaining_payment_status, expire_at,
        note, created_at, updated_at)
    VALUES (
        'BK-OPS-CANCEL-01', 2, 2, 5,
        DATE_ADD(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 9 DAY),
        2, 0, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        960000, 288000, 672000,
        'CANCELLED', 'DEPOSIT_30', 'CANCELLED',
        'UNPAID', NULL,
        'Khách hủy giao dịch trên cổng VNPAY (mã 24).',
        DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 15 MINUTE)
    ) ON DUPLICATE KEY UPDATE updated_at = NOW();

    INSERT INTO payments (booking_id, payment_method, payment_option, amount,
        transaction_code, payment_status, gateway, vnp_txn_ref, vnp_response_code, raw_return_payload, note)
    SELECT b.id, 'VNPAY', 'DEPOSIT_30', 288000,
           'TXN-OPS-CANCEL-01', 'CANCELLED', 'VNPAY', 'TM-OPS-CANCEL-01', '24',
           'vnp_ResponseCode=24', 'Khách hủy giao dịch trên cổng VNPAY; booking đã mở lại quota.'
    FROM bookings b
    WHERE b.booking_code = 'BK-OPS-CANCEL-01'
      AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.booking_id = b.id);

    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        remaining_payment_status, expire_at,
        note, created_at, updated_at)
    VALUES (
        'BK-OPS-FAILED-01', 5, 2, 6,
        DATE_ADD(CURDATE(), INTERVAL 8 DAY), DATE_ADD(CURDATE(), INTERVAL 10 DAY),
        2, 0, 1,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        1240000, 1240000, 0,
        'CANCELLED', 'FULL_PAYMENT', 'FAILED',
        'NOT_REQUIRED', NULL,
        'VNPAY báo giao dịch thất bại; booking bị hủy và phòng được mở lại.',
        DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE)
    ) ON DUPLICATE KEY UPDATE updated_at = NOW();

    INSERT INTO payments (booking_id, payment_method, payment_option, amount,
        transaction_code, payment_status, gateway, vnp_txn_ref, vnp_response_code, raw_return_payload, note)
    SELECT b.id, 'VNPAY', 'FULL_PAYMENT', 1240000,
           'TXN-OPS-FAILED-01', 'FAILED', 'VNPAY', 'TM-OPS-FAILED-01', '99',
           'vnp_ResponseCode=99', 'VNPAY báo thất bại; hệ thống hủy booking và trả quota.'
    FROM bookings b
    WHERE b.booking_code = 'BK-OPS-FAILED-01'
      AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.booking_id = b.id);

    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        remaining_payment_status, expire_at,
        note, created_at, updated_at)
    VALUES (
        'BK-OPS-EXPIRED-01', 6, 1, 1,
        DATE_ADD(CURDATE(), INTERVAL 9 DAY), DATE_ADD(CURDATE(), INTERVAL 11 DAY),
        2, 0, 1,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        1300000, 390000, 910000,
        'CANCELLED', 'DEPOSIT_30', 'EXPIRED',
        'UNPAID', NULL,
        'Phiên VNPAY quá hạn; scheduler hủy booking và trả lại quota.',
        DATE_SUB(NOW(), INTERVAL 45 MINUTE), DATE_SUB(NOW(), INTERVAL 45 MINUTE)
    ) ON DUPLICATE KEY UPDATE updated_at = NOW();

    INSERT INTO payments (booking_id, payment_method, payment_option, amount,
        transaction_code, payment_status, gateway, vnp_txn_ref, vnp_response_code, raw_return_payload, note)
    SELECT b.id, 'VNPAY', 'DEPOSIT_30', 390000,
           'TXN-OPS-EXPIRED-01', 'EXPIRED', 'VNPAY', 'TM-OPS-EXPIRED-01', '11',
           'vnp_ResponseCode=11', 'Giao dịch hết hạn; hệ thống hủy giữ phòng tạm.'
    FROM bookings b
    WHERE b.booking_code = 'BK-OPS-EXPIRED-01'
      AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.booking_id = b.id);

    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        refund_amount, cancellation_fee, remaining_payment_status,
        note, created_at, updated_at)
    VALUES (
        'BK-OPS-REFUND-PENDING-01', 5, 1, 2,
        DATE_ADD(CURDATE(), INTERVAL 12 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY),
        2, 0, 1,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        1700000, 1700000, 0,
        'CANCELLED', 'FULL_PAYMENT', 'REFUND_PENDING',
        1190000, 510000, 'NOT_REQUIRED',
        'Khách hủy sau khi thanh toán 100%; TravelMate đã ghi nhận yêu cầu hoàn tiền, chờ Admin xử lý ngoài hệ thống.',
        DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)
    ) ON DUPLICATE KEY UPDATE updated_at = NOW();

    INSERT INTO payments (booking_id, payment_method, payment_option, amount,
        transaction_code, payment_status, gateway, vnp_txn_ref, paid_at, approved_at, note)
    SELECT b.id, 'VNPAY', 'FULL_PAYMENT', 1700000,
           'TXN-OPS-REFUND-PENDING-01', 'REFUND_PENDING', 'VNPAY', 'TM-OPS-REFUND-PENDING-01',
           DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR),
           'Thanh toán đã ghi nhận; đang chờ Admin ghi nhận hoàn tiền.'
    FROM bookings b
    WHERE b.booking_code = 'BK-OPS-REFUND-PENDING-01'
      AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.booking_id = b.id);

    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        refund_amount, cancellation_fee, remaining_payment_status,
        note, created_at, updated_at)
    VALUES (
        'BK-OPS-REFUNDED-01', 6, 3, 9,
        DATE_ADD(CURDATE(), INTERVAL 13 DAY), DATE_ADD(CURDATE(), INTERVAL 15 DAY),
        2, 0, 1,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        2400000, 2400000, 0,
        'CANCELLED', 'FULL_PAYMENT', 'REFUNDED',
        1680000, 720000, 'NOT_REQUIRED',
        'Admin đã ghi nhận kết quả hoàn tiền ngoài hệ thống cho booking thanh toán 100%.',
        DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
    ) ON DUPLICATE KEY UPDATE updated_at = NOW();

    INSERT INTO payments (booking_id, payment_method, payment_option, amount,
        transaction_code, payment_status, gateway, vnp_txn_ref, paid_at, approved_at, note)
    SELECT b.id, 'VNPAY', 'FULL_PAYMENT', 2400000,
           'TXN-OPS-REFUNDED-01', 'REFUNDED', 'VNPAY', 'TM-OPS-REFUNDED-01',
           DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY),
           'Admin đã ghi nhận kết quả hoàn tiền ngoài hệ thống theo chính sách hủy booking thanh toán 100%.'
    FROM bookings b
    WHERE b.booking_code = 'BK-OPS-REFUNDED-01'
      AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.booking_id = b.id);

    -- Lịch sử booking đã checkout và review bổ sung cho các tài khoản user mới.
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        note, booking_source, remaining_payment_status,
        total_before_discount, created_at, updated_at)
    SELECT d.booking_code, u.id, ac.id, r.id,
           DATE_SUB(CURDATE(), INTERVAL d.checkin_days DAY),
           DATE_SUB(CURDATE(), INTERVAL d.checkout_days DAY),
           d.adults, d.children, 1,
           d.customer_name, d.customer_phone, d.customer_email,
           d.total_amount, d.total_amount, 0,
           'COMPLETED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_COMPLETED',
           d.note, 'ONLINE', 'NOT_REQUIRED',
           d.total_amount,
           DATE_SUB(NOW(), INTERVAL d.created_days DAY),
           DATE_SUB(NOW(), INTERVAL d.checkout_days DAY)
    FROM (
        SELECT 'BK-REV-LCH-001' AS booking_code, 'family@travelmate.vn' AS email, 'LCH-FAM' AS room_code, 82 AS checkin_days, 80 AS checkout_days, 104 AS created_days, 2 AS adults, 2 AS children, 'Gia đình Minh Anh' AS customer_name, '0968 111 222' AS customer_phone, 'family@travelmate.vn' AS customer_email, 2700000 AS total_amount, 'Gia đình đi Lào Cai kết hợp Sa Pa, đã checkout và gửi đánh giá.' AS note
        UNION ALL SELECT 'BK-REV-BHV-001', 'couple@travelmate.vn', 'BHV-DLX', 79, 77, 101, 2, 0, 'Linh & Khánh', '0979 333 444', 'couple@travelmate.vn', 1040000, 'Chuyến nghỉ cuối tuần ở Bắc Hà, thanh toán đủ qua VNPAY.'
        UNION ALL SELECT 'BK-REV-DVS-001', 'user3@travelmate.vn', 'DVS-SUI', 76, 74, 96, 2, 0, 'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn', 2500000, 'Khách hoàn tất cung Hà Giang và nghỉ tại Đồng Văn.'
        UNION ALL SELECT 'BK-REV-MPL-001', 'family@travelmate.vn', 'MPL-FAM', 73, 71, 93, 2, 2, 'Gia đình Minh Anh', '0968 111 222', 'family@travelmate.vn', 1840000, 'Gia đình nghỉ homestay nhìn Mã Pì Lèng, đã checkout đúng lịch.'
        UNION ALL SELECT 'BK-REV-MCC-001', 'user2@travelmate.vn', 'MCC-DLX', 70, 68, 90, 2, 0, 'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn', 1440000, 'Khách đi mùa lúa Mù Cang Chải, đặt phòng deluxe hai đêm.'
        UNION ALL SELECT 'BK-REV-TLR-001', 'family@travelmate.vn', 'TLR-FAM', 67, 65, 87, 2, 2, 'Gia đình Minh Anh', '0968 111 222', 'family@travelmate.vn', 6200000, 'Gia đình sử dụng phòng khoáng nóng Tú Lệ, thanh toán đủ.'
        UNION ALL SELECT 'BK-REV-MCT-001', 'couple@travelmate.vn', 'MCT-DLX', 64, 62, 84, 2, 0, 'Linh & Khánh', '0979 333 444', 'couple@travelmate.vn', 1960000, 'Cặp đôi nghỉ cuối tuần ở Mộc Châu, đã checkout.'
        UNION ALL SELECT 'BK-REV-MCF-001', 'family@travelmate.vn', 'MCF-FAM', 61, 59, 81, 2, 2, 'Gia đình Minh Anh', '0968 111 222', 'family@travelmate.vn', 1920000, 'Gia đình trải nghiệm farmstay có sân BBQ và vườn dâu.'
        UNION ALL SELECT 'BK-REV-TRV-001', 'couple@travelmate.vn', 'TRV-DLX', 58, 56, 78, 2, 0, 'Linh & Khánh', '0979 333 444', 'couple@travelmate.vn', 5200000, 'Cặp đôi nghỉ villa ven sông Tràng An, thanh toán đủ.'
        UNION ALL SELECT 'BK-REV-HLM-001', 'user@travelmate.vn', 'HLM-SUI', 55, 53, 75, 2, 1, 'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn', 5200000, 'Khách đi Hạ Long cùng gia đình nhỏ, đã checkout suite nhìn vịnh.'
        UNION ALL SELECT 'BK-REV-VBF-001', 'couple@travelmate.vn', 'VBF-DLX', 52, 50, 72, 2, 0, 'Linh & Khánh', '0979 333 444', 'couple@travelmate.vn', 1560000, 'Cặp đôi đặt homestay biển Vũng Tàu hai đêm.'
        UNION ALL SELECT 'BK-REV-MRH-001', 'family@travelmate.vn', 'MRH-FAM', 49, 47, 69, 2, 2, 'Gia đình Minh Anh', '0968 111 222', 'family@travelmate.vn', 1960000, 'Gia đình đi chợ nổi Cái Răng và nghỉ homestay ven sông.'
        UNION ALL SELECT 'BK-REV-GWP-001', 'family@travelmate.vn', 'GWP-FAM', 46, 44, 66, 2, 2, 'Gia đình Minh Anh', '0968 111 222', 'family@travelmate.vn', 4500000, 'Gia đình đặt phòng gần Grand World Phú Quốc, đã checkout.'
        UNION ALL SELECT 'BK-REV-MSD-001', 'couple@travelmate.vn', 'MSD-DLX', 43, 41, 63, 2, 0, 'Linh & Khánh', '0979 333 444', 'couple@travelmate.vn', 4800000, 'Cặp đôi nghỉ villa gần đồi cát Mũi Né, thanh toán đủ.'
        UNION ALL SELECT 'BK-REV-BHV-002', 'user2@travelmate.vn', 'BHV-FAM', 40, 38, 60, 2, 1, 'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn', 1560000, 'Khách đi cùng người thân, nghỉ phòng gia đình Bắc Hà.'
        UNION ALL SELECT 'BK-REV-HLM-002', 'user3@travelmate.vn', 'HLM-DLX', 37, 35, 57, 2, 0, 'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn', 2900000, 'Khách đặt phòng Hạ Long Marina, hoàn tất thanh toán và checkout.'
        UNION ALL SELECT 'BK-REV-LCH-002', 'user@travelmate.vn', 'LCH-STD', 34, 32, 54, 1, 0, 'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn', 1240000, 'Chuyến công tác Lào Cai hai đêm, đã hoàn tất.'
    ) d
    JOIN users u ON u.email = d.email
    JOIN rooms r ON r.room_code = d.room_code
    JOIN accommodations ac ON ac.id = r.accommodation_id
    WHERE NOT EXISTS (SELECT 1 FROM bookings b WHERE b.booking_code = d.booking_code);

    INSERT INTO payments (booking_id, payment_method, payment_option, amount,
        transaction_code, payment_status, paid_at, approved_at,
        gateway, vnp_txn_ref, vnp_response_code, vnp_transaction_status, note)
    SELECT b.id, 'VNPAY', 'FULL_PAYMENT', b.total_amount,
           REPLACE(b.booking_code, 'BK-', 'TXN-'), 'APPROVED',
           DATE_SUB(b.check_in, INTERVAL 7 DAY),
           DATE_SUB(b.check_in, INTERVAL 7 DAY),
           'VNPAY', REPLACE(b.booking_code, 'BK-', 'TM-'), '00', '00',
           'VNPAY ghi nhận thành công, đơn đã hoàn tất sau khi khách checkout.'
    FROM bookings b
    WHERE b.booking_code LIKE 'BK-REV-%'
      AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.booking_id = b.id);

    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at)
    SELECT b.user_id, b.accommodation_id, b.id, d.rating, d.comment,
           DATE_ADD(b.check_out, INTERVAL 1 DAY)
    FROM (
        SELECT 'BK-REV-LCH-001' AS booking_code, 9 AS rating, 'Khách sạn sạch, vị trí thuận tiện để hôm sau đi Sa Pa. Phòng gia đình rộng và nhân viên hỗ trợ gửi hành lý rất nhanh.' AS comment
        UNION ALL SELECT 'BK-REV-BHV-001', 9, 'Homestay yên tĩnh, bữa sáng địa phương ngon. Ban công nhìn thung lũng đúng như mô tả.'
        UNION ALL SELECT 'BK-REV-DVS-001', 8, 'Phòng suite rộng, nằm gần phố cổ Đồng Văn nên đi bộ buổi tối rất tiện. Nước nóng ổn định.'
        UNION ALL SELECT 'BK-REV-MPL-001', 10, 'View núi rất đẹp, chủ nhà thân thiện và hỗ trợ đặt thuyền Nho Quế. Gia đình tôi rất hài lòng.'
        UNION ALL SELECT 'BK-REV-MCC-001', 9, 'Bungalow sạch, ngắm ruộng bậc thang từ ban công. Đường vào hơi quanh co nhưng đáng trải nghiệm.'
        UNION ALL SELECT 'BK-REV-TLR-001', 9, 'Khu khoáng nóng sạch, phòng gia đình rộng, trẻ con rất thích hồ bơi. Nhà hàng phục vụ nhanh.'
        UNION ALL SELECT 'BK-REV-MCT-001', 8, 'Vị trí gần đồi chè, phòng có ban công thoáng. Bữa sáng đơn giản nhưng vừa miệng.'
        UNION ALL SELECT 'BK-REV-MCF-001', 9, 'Farmstay có sân rộng, khu BBQ tiện cho gia đình. Chủ nhà chuẩn bị xe đạp miễn phí.'
        UNION ALL SELECT 'BK-REV-TRV-001', 10, 'Villa ven sông đẹp, bếp đầy đủ, không gian riêng tư. Rất hợp chuyến nghỉ ngắn ở Ninh Bình.'
        UNION ALL SELECT 'BK-REV-HLM-001', 9, 'Suite nhìn vịnh đẹp, gần bến du thuyền. Gia đình di chuyển đi tour Hạ Long rất thuận tiện.'
        UNION ALL SELECT 'BK-REV-VBF-001', 8, 'Homestay gần biển, phòng có ban công và máy lạnh tốt. Cuối tuần hơi đông nhưng đáng tiền.'
        UNION ALL SELECT 'BK-REV-MRH-001', 9, 'Chủ nhà chuẩn bị thuyền đi chợ nổi đúng giờ, phòng gia đình sạch và thoáng.'
        UNION ALL SELECT 'BK-REV-GWP-001', 8, 'Phòng gần khu vui chơi nên rất tiện cho trẻ nhỏ. Buổi tối hơi náo nhiệt nhưng dịch vụ tốt.'
        UNION ALL SELECT 'BK-REV-MSD-001', 9, 'Villa riêng tư, sân BBQ đẹp, đi đồi cát rất gần. Phù hợp cặp đôi hoặc nhóm nhỏ.'
        UNION ALL SELECT 'BK-REV-BHV-002', 8, 'Phòng gia đình đủ rộng, có khu sinh hoạt chung. Bữa sáng nóng và chủ nhà nhiệt tình.'
        UNION ALL SELECT 'BK-REV-HLM-002', 9, 'Phòng deluxe sạch, nhìn vịnh đẹp. Nhân viên lễ tân hỗ trợ check-in nhanh.'
        UNION ALL SELECT 'BK-REV-LCH-002', 8, 'Phòng tiêu chuẩn gọn, phù hợp công tác. Vị trí trung tâm giúp đi lại thuận tiện.'
    ) d
    JOIN bookings b ON b.booking_code = d.booking_code
    WHERE NOT EXISTS (SELECT 1 FROM reviews rv WHERE rv.booking_id = b.id);

    -- Đồng bộ trạng thái xử lý phía Partner cho các đơn đã check-out.
    -- Code runtime khi Partner bấm "Hoàn tất / Check-out" sẽ set PARTNER_COMPLETED;
    -- block này giúp dữ liệu import máy khác cũng hiển thị đủ trạng thái đó trên Admin/Partner/User.
    UPDATE bookings
    SET partner_status = 'PARTNER_COMPLETED'
    WHERE booking_status = 'COMPLETED'
      AND (booking_source IS NULL OR booking_source = 'ONLINE')
      AND partner_status = 'PARTNER_CONFIRMED';

    -- Chuẩn hóa snapshot doanh thu cho đơn đặt cọc 30%:
    -- Hoa hồng tính trên tổng đơn gốc trước voucher, còn payout vẫn dựa trên số tiền hệ thống thực thu online.
    UPDATE bookings b
    JOIN rooms r ON r.id = b.room_id
    JOIN accommodations a ON a.id = b.accommodation_id
    SET b.commission_rate_snapshot = COALESCE(b.commission_rate_snapshot,
            COALESCE(r.commission_rate_override / 100,
                CASE a.property_type
                    WHEN 'HOTEL' THEN 0.15
                    WHEN 'RESORT' THEN 0.18
                    WHEN 'VILLA' THEN 0.12
                    WHEN 'HOMESTAY' THEN 0.10
                    ELSE 0.10
                END)),
        b.commission_source_snapshot = COALESCE(b.commission_source_snapshot,
            CASE WHEN r.commission_rate_override IS NOT NULL THEN 'ROOM_OVERRIDE' ELSE 'PROPERTY_TYPE_DEFAULT' END),
        b.commission_base_amount = COALESCE(b.total_before_discount, b.total_amount, COALESCE(b.paid_amount, 0)),
        b.commission_amount_snapshot = ROUND(
            COALESCE(b.total_before_discount, b.total_amount, COALESCE(b.paid_amount, 0))
            * COALESCE(b.commission_rate_snapshot,
                COALESCE(r.commission_rate_override / 100,
                    CASE a.property_type
                        WHEN 'HOTEL' THEN 0.15
                        WHEN 'RESORT' THEN 0.18
                        WHEN 'VILLA' THEN 0.12
                        WHEN 'HOMESTAY' THEN 0.10
                        ELSE 0.10
                    END)),
            0),
        b.partner_voucher_amount_snapshot = CASE WHEN b.voucher_cost_bearer = 'PARTNER' THEN COALESCE(b.discount_amount, 0) ELSE 0 END,
        b.admin_voucher_amount_snapshot = CASE WHEN b.voucher_cost_bearer = 'ADMIN' THEN COALESCE(b.discount_amount, 0) ELSE 0 END,
        b.partner_payout_snapshot =
            COALESCE(b.paid_amount, 0) - ROUND(
                COALESCE(b.total_before_discount, b.total_amount, COALESCE(b.paid_amount, 0))
                * COALESCE(b.commission_rate_snapshot,
                    COALESCE(r.commission_rate_override / 100,
                        CASE a.property_type
                            WHEN 'HOTEL' THEN 0.15
                            WHEN 'RESORT' THEN 0.18
                            WHEN 'VILLA' THEN 0.12
                            WHEN 'HOMESTAY' THEN 0.10
                            ELSE 0.10
                        END)),
                0)
            - CASE WHEN b.voucher_cost_bearer = 'PARTNER' THEN COALESCE(b.discount_amount, 0) ELSE 0 END,
        b.remaining_amount = CASE
            WHEN b.payment_status = 'DEPOSIT_FORFEITED' OR b.booking_status IN ('CANCELLED', 'NO_SHOW')
                THEN COALESCE(b.remaining_amount, 0)
            ELSE GREATEST(COALESCE(b.total_before_discount, b.total_amount, 0) - COALESCE(b.paid_amount, 0), 0)
        END,
        b.online_paid_amount_snapshot = COALESCE(b.paid_amount, 0),
        b.onsite_amount_snapshot = CASE
            WHEN b.payment_status = 'DEPOSIT_FORFEITED' OR b.booking_status IN ('CANCELLED', 'NO_SHOW')
                THEN 0
            ELSE GREATEST(COALESCE(b.total_before_discount, b.total_amount, 0) - COALESCE(b.paid_amount, 0), 0)
        END
    WHERE b.payment_option = 'DEPOSIT_30'
      AND (b.booking_source IS NULL OR b.booking_source = 'ONLINE');

    -- Chuẩn hóa snapshot doanh thu cho đơn thanh toán 100%:
    -- Hoa hồng cũng tính trên tổng đơn gốc trước voucher; phần đã thu online vẫn là số tiền khách thanh toán qua VNPAY.
    UPDATE bookings b
    JOIN rooms r ON r.id = b.room_id
    JOIN accommodations a ON a.id = b.accommodation_id
    SET b.commission_rate_snapshot = COALESCE(b.commission_rate_snapshot,
            COALESCE(r.commission_rate_override / 100,
                CASE a.property_type
                    WHEN 'HOTEL' THEN 0.15
                    WHEN 'RESORT' THEN 0.18
                    WHEN 'VILLA' THEN 0.12
                    WHEN 'HOMESTAY' THEN 0.10
                    ELSE 0.10
                END)),
        b.commission_source_snapshot = COALESCE(b.commission_source_snapshot,
            CASE WHEN r.commission_rate_override IS NOT NULL THEN 'ROOM_OVERRIDE' ELSE 'PROPERTY_TYPE_DEFAULT' END),
        b.commission_base_amount = COALESCE(b.total_before_discount, b.total_amount, COALESCE(b.paid_amount, 0)),
        b.commission_amount_snapshot = ROUND(
            COALESCE(b.total_before_discount, b.total_amount, COALESCE(b.paid_amount, 0))
            * COALESCE(b.commission_rate_snapshot,
                COALESCE(r.commission_rate_override / 100,
                    CASE a.property_type
                        WHEN 'HOTEL' THEN 0.15
                        WHEN 'RESORT' THEN 0.18
                        WHEN 'VILLA' THEN 0.12
                        WHEN 'HOMESTAY' THEN 0.10
                        ELSE 0.10
                    END)),
            0),
        b.partner_voucher_amount_snapshot = CASE WHEN b.voucher_cost_bearer = 'PARTNER' THEN COALESCE(b.discount_amount, 0) ELSE 0 END,
        b.admin_voucher_amount_snapshot = CASE WHEN b.voucher_cost_bearer = 'ADMIN' THEN COALESCE(b.discount_amount, 0) ELSE 0 END,
        b.partner_payout_snapshot =
            COALESCE(b.paid_amount, 0) - ROUND(
                COALESCE(b.total_before_discount, b.total_amount, COALESCE(b.paid_amount, 0))
                * COALESCE(b.commission_rate_snapshot,
                    COALESCE(r.commission_rate_override / 100,
                        CASE a.property_type
                            WHEN 'HOTEL' THEN 0.15
                            WHEN 'RESORT' THEN 0.18
                            WHEN 'VILLA' THEN 0.12
                            WHEN 'HOMESTAY' THEN 0.10
                            ELSE 0.10
                        END)),
                0)
            - CASE WHEN b.voucher_cost_bearer = 'PARTNER' THEN COALESCE(b.discount_amount, 0) ELSE 0 END,
        b.remaining_amount = 0,
        b.online_paid_amount_snapshot = COALESCE(b.paid_amount, 0),
        b.onsite_amount_snapshot = 0
    WHERE b.payment_option = 'FULL_PAYMENT'
      AND COALESCE(b.paid_amount, 0) > 0
      AND (b.booking_source IS NULL OR b.booking_source = 'ONLINE');

    SET FOREIGN_KEY_CHECKS = 1;
    SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

    -- =============================================
    -- END OF travelmate_db.sql v19 — TravelMate Initial Data
    -- =============================================


