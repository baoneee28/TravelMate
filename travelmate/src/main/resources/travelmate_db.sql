    -- =============================================
    -- TravelMate Database - Script khởi tạo
    -- Phiên bản: đồ án cơ sở — demo booking flow
    -- Cập nhật: thêm owner_id cho partner ownership
    -- =============================================

    CREATE DATABASE IF NOT EXISTS travelmate_db
        DEFAULT CHARACTER SET utf8mb4
        DEFAULT COLLATE utf8mb4_unicode_ci;

    USE travelmate_db;

    -- =============================================
    -- DROP theo đúng thứ tự (FK phụ thuộc)
    -- =============================================
    DROP TABLE IF EXISTS travel_posts;
    DROP TABLE IF EXISTS notifications;
    DROP TABLE IF EXISTS admin_action_logs;
    DROP TABLE IF EXISTS support_tickets;
    DROP TABLE IF EXISTS reviews;
    DROP TABLE IF EXISTS partner_settlements;
    DROP TABLE IF EXISTS payments;
    DROP TABLE IF EXISTS bookings;
    DROP TABLE IF EXISTS vouchers;
    DROP TABLE IF EXISTS room_amenities;
    DROP TABLE IF EXISTS amenities;
    DROP TABLE IF EXISTS rooms;
    DROP TABLE IF EXISTS accommodations;
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
        created_at            DATETIME(6),
        updated_at            DATETIME(6),
        PRIMARY KEY (id)
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
        -- room_category: phân loại phòng theo hạng, dùng để demo commission linh hoạt
        --   Giá trị: STANDARD | DELUXE | FAMILY | VIP | SUITE | OTHER
        room_category           VARCHAR(30)   DEFAULT 'STANDARD',
        --
        -- commission_rate_override: tỷ lệ hoa hồng riêng cho phòng này (đơn vị %)
        --   null    → dùng mặc định theo PropertyType (HOTEL=15%, VILLA=12%, HOMESTAY=10%, RESORT=18%)
        --   có giá trị → áp dụng rate này (VD: 18.00 nghĩa là 18%)
        commission_rate_override DECIMAL(5,2) DEFAULT NULL,
        --
        PRIMARY KEY (id),
        CONSTRAINT fk_rooms_accommodation FOREIGN KEY (accommodation_id) REFERENCES accommodations (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- =============================================
    -- 3a. BẢNG AMENITIES — Tiện nghi phòng/căn lưu trú
    -- =============================================
    CREATE TABLE amenities (
        id       BIGINT       NOT NULL AUTO_INCREMENT,
        name     VARCHAR(100) NOT NULL,
        icon     VARCHAR(20),
        category VARCHAR(50),
        PRIMARY KEY (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- =============================================
    -- 3aa. BẢNG ROOM_AMENITIES — Bảng join N-N giữa rooms và amenities
    -- =============================================
    CREATE TABLE room_amenities (
        room_id    BIGINT NOT NULL,
        amenity_id BIGINT NOT NULL,
        PRIMARY KEY (room_id, amenity_id),
        CONSTRAINT fk_ra_room    FOREIGN KEY (room_id)    REFERENCES rooms (id),
        CONSTRAINT fk_ra_amenity FOREIGN KEY (amenity_id) REFERENCES amenities (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- =============================================
    -- 3b. BẢNG VOUCHERS (phải sau rooms vì FK đến accommodations + rooms)
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
        booking_status        VARCHAR(50)   DEFAULT 'PENDING_ADMIN_APPROVAL',
        payment_option        VARCHAR(50)   DEFAULT 'FULL_PAYMENT',
        payment_status        VARCHAR(50)   DEFAULT 'PENDING_ADMIN_APPROVAL',
        partner_status        VARCHAR(40)   DEFAULT NULL,
        note                  VARCHAR(500),
        -- === VOUCHER FIELDS (Hướng 2) ===
        voucher_code          VARCHAR(50)   DEFAULT NULL,
        discount_amount       DECIMAL(15,0) DEFAULT 0,
        voucher_cost_bearer   VARCHAR(20)   DEFAULT NULL,
        total_before_discount DECIMAL(15,0) DEFAULT 0,
        -- === BOOKING SOURCE & DIRECT BOOKING FIELDS ===
        booking_source            VARCHAR(20)  DEFAULT 'ONLINE',
        block_reason              VARCHAR(300) DEFAULT NULL,
        remaining_payment_status  VARCHAR(30)  DEFAULT 'NOT_REQUIRED',
        remaining_paid_at         DATETIME     DEFAULT NULL,
        remaining_payment_note    VARCHAR(300) DEFAULT NULL,
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
        payment_method   VARCHAR(50)   DEFAULT 'VNPAY_DEMO',
        payment_option   VARCHAR(50)   NOT NULL,
        amount           DECIMAL(15,0) NOT NULL,
        transaction_code VARCHAR(50),
        payment_status   VARCHAR(50)   DEFAULT 'PENDING_ADMIN_APPROVAL',
        paid_at          DATETIME(6),
        approved_at      DATETIME(6),
        note             VARCHAR(500),
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
    -- 7. BẢNG PARTNER_SETTLEMENTS — Quyết toán tuần cho Partner
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
        settlement_status        VARCHAR(20)   DEFAULT 'PENDING',
        settlement_date          DATETIME(6),
        note                     VARCHAR(500),
        created_at               DATETIME(6),
        PRIMARY KEY (id),
        -- UNIQUE: Mỗi partner chỉ có 1 settlement duy nhất cho 1 kỳ tuần
        UNIQUE KEY uk_settlement_partner_period (partner_id, period_start, period_end),
        CONSTRAINT fk_settlements_partner FOREIGN KEY (partner_id) REFERENCES users (id)
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
    -- ========= DỮ LIỆU MẪU (SEED DATA) =========
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
    ('partner4@travelmate.vn', '$2a$10$dCikCIiksr/Ne1Xpv40vKOMFwpiY751Cb1kdIVDwobiMzImbLC3oq', 'Mekong Homestay',    'Mekong Homestay',    '0966 789 012', 'PARTNER', 'ACTIVE', 'HOMESTAY',
    '5544332211', 'Agribank', 'PHAM THI D', 'Cần Thơ', NOW(), NOW());

    -- ─── SUPPORT TICKETS — Yêu cầu hỗ trợ từ Partner, User & Guest ────────
    -- PARTNER tickets: partner1 (id=3): 3 tickets, partner2 (id=4): 2 tickets
    --                  partner3 (id=7): 2 tickets, partner4 (id=8): 2 tickets
    -- USER/GUEST tickets: 2 mẫu demo
    INSERT INTO support_tickets (requester_role, partner_id, user_id, requester_name, requester_email, requester_phone, category, subject, priority, description, status, admin_response, created_at, updated_at) VALUES

    -- ── PARTNER tickets ──────────────────────────────────────────────────────
    -- partner1 — HOTEL (Sunrise Sapa Lodge, id=3)
    ('PARTNER', 3, NULL, NULL, NULL, NULL, 'Thanh toán & Quyết toán', 'Quyết toán tuần 3 bị sai số tiền', 'Cao',
    'Chào Admin, tôi kiểm tra lại quyết toán tuần 3 thì thấy số tiền payout là 1.224.000đ nhưng theo tính toán của tôi thì phải cao hơn. Booking BK-LATA-DLX-0002 có total 2.040.000đ, commission 15% = 306.000đ, voucher deduction 510.000đ, payout đúng = 1.224.000đ. Thực ra đúng rồi, tôi nhầm. Xin lỗi và cảm ơn đã hỗ trợ!',
    'RESPONDED',
    'Chào bạn, tôi đã kiểm tra lại và xác nhận con số quyết toán tuần 3 là chính xác: gross 2.040.000đ - commission 15% (306.000đ) - voucher LATA20 do partner chịu (510.000đ) = payout 1.224.000đ. Rất vui vì bạn đã tự kiểm tra được! Nếu có thắc mắc gì thêm hãy liên hệ.',
    DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),

    ('PARTNER', 3, NULL, NULL, NULL, NULL, 'Đơn đặt phòng', 'Khách không đến nhưng không thể đánh dấu No-Show', 'Trung bình',
    'Booking BK-TLP-STD-0001, khách Nguyễn Văn An đã không đến check-in ngày hôm qua. Tôi muốn đánh dấu No-Show để hệ thống tự động giữ cọc 30% nhưng không thấy nút này ở giao diện. Mong Admin xử lý giúp.',
    'RESPONDED',
    'Chào bạn, chức năng đánh dấu No-Show hiện chỉ Admin mới thực hiện được để đảm bảo kiểm soát chặt chẽ. Tôi đã cập nhật booking BK-TLP-STD-0001 thành NO_SHOW và cọc 30% (288.000đ) đã được giữ lại. Trong phiên bản tới chúng tôi sẽ cho phép Partner tự đánh dấu sau 4h kể từ giờ check-in.',
    DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),

    ('PARTNER', 3, NULL, NULL, NULL, NULL, 'Kỹ thuật', 'Trang doanh thu không hiển thị biểu đồ', 'Thấp',
    'Khi tôi vào /partner/revenue, trang tải bình thường nhưng phần biểu đồ doanh thu theo tuần bị trống. Trình duyệt Chrome v124. Xin hỗ trợ.',
    'OPEN', NULL,
    DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

    -- partner2 — RESORT (Blue Ocean Resort, id=4)
    ('PARTNER', 4, NULL, NULL, NULL, NULL, 'Cơ sở lưu trú', 'Muốn thêm ảnh thumbnail cho Vinpearl Resort', 'Thấp',
    'Ảnh thumbnail hiện tại của Vinpearl Resort & Spa Nha Trang (acc id=8) trông hơi tối. Tôi muốn cập nhật ảnh mới đẹp hơn nhưng không thấy chỗ chỉnh sửa trong giao diện Partner. Xin hướng dẫn.',
    'CLOSED',
    'Chào bạn, hiện tại chức năng thay đổi thumbnail cần Admin hỗ trợ. Tôi đã cập nhật ảnh thumbnail mới cho Vinpearl Resort của bạn. Trong phiên bản tới, Partner sẽ tự chỉnh sửa được trực tiếp từ trang Nơi lưu trú của tôi.',
    DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)),

    ('PARTNER', 4, NULL, NULL, NULL, NULL, 'Thanh toán & Quyết toán', 'Chưa nhận được thanh toán tuần 2', 'Cao',
    'Quyết toán tuần 2 (Apr 13-19) có payout 4.592.000đ, trạng thái đã PAID nhưng tài khoản Vietcombank của tôi chưa nhận được tiền. Số TK: 9876543210, chủ TK: TRAN THI B. Đã chờ 3 ngày rồi.',
    'RESPONDED',
    'Chào bạn, tôi đã kiểm tra lại. Giao dịch chuyển khoản 4.592.000đ đã được xử lý ngày hôm qua, thường mất 1-2 ngày làm việc để tiền về tài khoản. Nếu sau 48h nữa vẫn chưa nhận được, vui lòng liên hệ lại với mã giao dịch để tôi xác nhận với bộ phận tài chính.',
    DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),

    -- partner3 — VILLA (Green Hills Villa, id=7)
    ('PARTNER', 7, NULL, NULL, NULL, NULL, 'Đơn đặt phòng', 'Booking BK-ANM-GDN-0001 chờ xác nhận quá lâu', 'Cao',
    'Booking BK-ANM-GDN-0001 (Anam Villa, khách Hoàng Văn Hùng, check-in +10 ngày) đang ở trạng thái PENDING_ADMIN_APPROVAL đã hơn 2 ngày. Tôi muốn hỏi Admin khi nào sẽ duyệt để tôi chuẩn bị phòng cho khách.',
    'OPEN', NULL,
    DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

    ('PARTNER', 7, NULL, NULL, NULL, NULL, 'Kỹ thuật', 'Không thêm được phòng mới cho Ba Na Hills Villa', 'Trung bình',
    'Tôi vào acc5 Ba Na Hills Forest Villa, bấm nút Thêm phòng nhưng trang báo lỗi "Chỉ có thể thêm phòng cho cơ sở đã được Admin duyệt". Trong khi acc5 đang có trạng thái APPROVED. Xin kiểm tra giúp.',
    'RESPONDED',
    'Chào bạn, tôi đã kiểm tra và xác nhận acc5 Ba Na Hills Forest Villa đang ở trạng thái APPROVED. Lỗi có thể do cache trình duyệt. Hãy thử Ctrl+Shift+R để hard refresh. Nếu vẫn lỗi, hãy chụp màn hình và gửi lại cho tôi.',
    DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),

    -- partner4 — HOMESTAY (Mekong Homestay, id=8)
    ('PARTNER', 8, NULL, NULL, NULL, NULL, 'Voucher', 'Muốn tạo voucher nhưng không thấy Cơ sở lưu trú của tôi', 'Trung bình',
    'Tôi vào /partner/vouchers, tab "Voucher theo cơ sở" không hiện dropdown để chọn cơ sở. Tôi có 2 homestay là Hoa Lư (id=6) và Mộc Nhiên (id=7), cả hai đều đang APPROVED nhưng không hiện trong danh sách.',
    'OPEN', NULL,
    DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),

    ('PARTNER', 8, NULL, NULL, NULL, NULL, 'Thanh toán & Quyết toán', 'Hỏi về lịch quyết toán hàng tuần', 'Thấp',
    'Tôi muốn hỏi TravelMate thanh toán quyết toán cho partner vào ngày nào trong tuần? Và số tiền tối thiểu để được thanh toán là bao nhiêu?',
    'CLOSED',
    'Chào bạn! TravelMate thực hiện quyết toán vào mỗi Thứ Ba hàng tuần cho kỳ tuần trước (T2-CN). Không có số tiền tối thiểu — dù chỉ 1 booking đã hoàn tất cũng sẽ được thanh toán. Tiền chuyển về tài khoản đăng ký trong mục Hồ sơ trong vòng 1-2 ngày làm việc.',
    DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY)),

    -- ── USER/GUEST demo tickets ───────────────────────────────────────────────
    -- user@travelmate.vn (id=2) gửi liên hệ với tư cách USER đã đăng nhập
    ('USER', NULL, 2, 'Nguyễn Văn An', 'user@travelmate.vn', '0912 345 678',
    'Đặt phòng', 'Tôi muốn hỏi về chính sách hủy phòng', 'Thấp',
    'Cho tôi hỏi nếu tôi hủy booking trước 48h thì có hoàn tiền không? Đặc biệt với loại cọc 30%.',
    'RESPONDED',
    'Chào bạn Nguyễn Văn An! Theo chính sách TravelMate: nếu hủy trước 48h check-in, cọc 30% sẽ được hoàn trả trong 3-5 ngày làm việc. Nếu hủy trong vòng 48h, cọc 30% sẽ không được hoàn lại. Bạn có thể hủy trực tiếp trong mục Lịch sử đặt phòng.',
    DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),

    -- Guest không đăng nhập gửi liên hệ
    ('GUEST', NULL, NULL, 'Khách Vãng Lai', 'guest.visitor@email.com', '0900 000 001',
    'Khác', 'Hỏi về hợp tác đưa cơ sở lên TravelMate', 'Thấp',
    'Xin chào, tôi có một villa nhỏ ở Đà Lạt, muốn hỏi thủ tục để đăng ký làm đối tác trên TravelMate là như thế nào? Chi phí, hoa hồng, điều kiện ra sao?',
    'OPEN', NULL,
    DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));

    -- ─── ACCOMMODATIONS (3 Khách sạn — owner_id=3 = partner@travelmate.vn) ───
    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at) VALUES
    (
        'LATA Hotel & Apartments',
        'Khách sạn hiện đại nằm ngay trung tâm thành phố Đà Lạt, cách chợ đêm Đà Lạt 500m. Phòng rộng rãi, view đẹp, tiện nghi đầy đủ.',
        '15 Phan Bội Châu, Phường 1',
        'Đà Lạt',
        'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&q=80',
        4, 8.6, 771, 'HOTEL', 'APPROVED', 3, NOW(), NOW()
    ),
    (
        'Tulip Hotel 2 Dalat',
        'Khách sạn 3 sao thiết kế phong cách Châu Âu, gần hồ Xuân Hương và Vườn hoa thành phố. Dịch vụ tận tình, giá cả phải chăng.',
        '56 Bùi Thị Xuân, Phường 2',
        'Đà Lạt',
        'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800&q=80',
        3, 8.2, 456, 'HOTEL', 'APPROVED', 3, NOW(), NOW()
    ),
    (
        'TravelMate Grand Hotel',
        'Khách sạn 5 sao sang trọng bậc nhất Đà Lạt, tọa lạc trên đồi thông với tầm nhìn toàn cảnh thung lũng. Spa cao cấp, nhà hàng fine dining, hồ bơi vô cực.',
        '88 Nguyễn Chí Thanh, Phường 6',
        'Đà Lạt',
        'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800&q=80',
        5, 9.2, 1205, 'HOTEL', 'APPROVED', 3, NOW(), NOW()
    );

    -- ─── ROOMS (v2: thêm room_category + commission_rate_override) ────────────
    --
    -- KỊCH BẢN DEMO COMMISSION:
    --   1. STANDARD → null override → dùng default theo PropertyType (HOTEL=15%)
    --   2. DELUXE   → null override → dùng default theo PropertyType (HOTEL=15%)
    --   3. FAMILY   → override 12% → THẤP HƠN default (ưu đãi gia đình)
    --   4. VIP      → override 18% → CAO HƠN default (phòng premium)
    --   5. SUITE    → override 20% → CAO NHẤT (suite cao cấp)
    --
    -- Admin/Partner thấy rõ sự khác nhau trong bảng Doanh Thu.

    -- Hotel 1: LATA Hotel & Apartments (id=1) — HOTEL (default 15%)
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category, commission_rate_override) VALUES
    (1, 'LATA-STD', 'Phòng Tiêu Chuẩn Giường King',  '1 giường cỡ King',           2, 650000, 5,
    'Phòng tiêu chuẩn 25m², tầm nhìn thành phố, WiFi miễn phí, điều hoà, minibar.',
    'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=400&q=70',
    'STANDARD', NULL),        -- ← null = dùng HOTEL default 15%
    (1, 'LATA-DLX', 'Phòng Deluxe Giường Đôi',       '2 giường đơn',               3, 850000, 4,
    'Phòng Deluxe 30m² với ban công riêng, view vườn hoa. Bao gồm bữa sáng.',
    'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=400&q=70',
    'DELUXE', NULL),           -- ← null = dùng HOTEL default 15%
    (1, 'LATA-FAM', 'Phòng Gia Đình',                 '1 giường King + 1 giường đơn', 4, 1200000, 3,
    'Phòng gia đình 40m², phù hợp gia đình có trẻ nhỏ. Có bồn tắm lớn.',
    'https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=400&q=70',
    'FAMILY', 12.00),          -- ← 12% override (ưu đãi gia đình, thấp hơn default 15%)
    (1, 'LATA-SUI', 'Phòng Suite Cao Cấp',            '1 giường King size',         2, 1800000, 1,
    'Suite 55m² sang trọng với phòng khách riêng, view hồ Xuân Hương, bồn tắm jacuzzi.',
    'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=400&q=70',
    'SUITE', 20.00);           -- ← 20% override (suite premium, cao hơn default 15%)

    -- Hotel 2: Tulip Hotel 2 Dalat (id=2) — HOTEL (default 15%)
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category, commission_rate_override) VALUES
    (2, 'TLP-STD', 'Phòng Standard Twin',   '2 giường đơn',                  2, 480000, 6,
    'Phòng standard 22m², nội thất đơn giản tiện nghi, WiFi miễn phí.',
    'https://images.unsplash.com/photo-1540518614846-7eded433c457?w=400&q=70',
    'STANDARD', NULL),         -- ← null = HOTEL default 15%
    (2, 'TLP-SUP', 'Phòng Superior Double', '1 giường đôi',                  2, 620000, 5,
    'Phòng Superior 28m² với view đồi thông, bao gồm bữa sáng buffet.',
    'https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=400&q=70',
    'DELUXE', NULL),            -- ← null = HOTEL default 15%
    (2, 'TLP-FAM', 'Phòng Gia Đình Rộng',  '1 giường đôi + 2 giường đơn',  5, 1050000, 2,
    'Phòng gia đình rộng 45m², lý tưởng cho nhóm bạn hoặc gia đình lớn.',
    'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=400&q=70',
    'FAMILY', NULL),            -- ← null = HOTEL default 15%
    (2, 'TLP-VIP', 'Phòng VIP Panorama',   '1 giường King size',            2, 1500000, 2,
    'Phòng VIP 50m² với ban công rộng, view 360° toàn cảnh Đà Lạt.',
    'https://images.unsplash.com/photo-1591088398332-8a7791972843?w=400&q=70',
    'VIP', 18.00);              -- ← 18% override (VIP, cao hơn HOTEL default 15%)

    -- Hotel 3: TravelMate Grand Hotel (id=3) — HOTEL (default 15%)
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category, commission_rate_override) VALUES
    (3, 'TMG-DLX',  'Deluxe Garden View',    '1 giường King size',                   2, 1200000, 8,
    'Phòng Deluxe 35m², view vườn thông tĩnh lặng, bồn tắm đứng + bồn ngâm riêng.',
    'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=400&q=70',
    'DELUXE', NULL),            -- ← null = HOTEL default 15%
    (3, 'TMG-PRE',  'Premium Valley View',   '1 giường King hoặc 2 giường đơn',     3, 1650000, 5,
    'Phòng Premium 42m², tầm nhìn thung lũng ngoạn mục, minibar complimentary.',
    'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=400&q=70',
    'VIP', 18.00),              -- ← 18% override (VIP, cao hơn default 15%)
    (3, 'TMG-FAM',  'Family Grand Suite',    '2 giường King size',                   5, 2800000, 3,
    'Suite gia đình 65m², 2 phòng ngủ, phòng khách, bếp nhỏ, phù hợp nghỉ dưỡng dài ngày.',
    'https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=400&q=70',
    'FAMILY', 12.00),           -- ← 12% override (ưu đãi gia đình)
    (3, 'TMG-PRE2', 'Presidential Suite',   '1 giường King cỡ lớn',                 2, 5500000, 1,
    'Suite Tổng Thống 100m², sang trọng nhất khách sạn. Phòng khách riêng, bàn làm việc, spa tại phòng, butler riêng.',
    'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=400&q=70',
    'SUITE', 20.00);            -- ← 20% override (Presidential Suite premium)


    -- ─── ACCOMMODATIONS — VILLA (owner_id=7 = partner3@travelmate.vn VILLA) ───────────────
    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at) VALUES
    (
        'The Anam Villa Nha Trang',
        'Biệt thự nghỉ dưỡng phong cách Đông Dương sang trọng tọa lạc ngay trên bãi biển riêng Cam Ranh. Hồ bơi private, butler riêng, view biển vô cực.',
        'Nguyễn Tất Thành, Cam Lâm',
        'Nha Trang',
        'https://images.unsplash.com/photo-1582268611958-ebfd161ef9cf?w=800&q=80',
        5, 9.4, 632, 'VILLA', 'APPROVED', 7, NOW(), NOW()
    ),
    (
        'Ba Na Hills Forest Villa',
        'Villa bungalow giữa rừng nguyên sinh núi Bà Nà, thiết kế gỗ tự nhiên ấm áp. Gần cáp treo dài nhất thế giới, khí hậu mát mẻ quanh năm.',
        'Km 20 Huyện Hòa Vang',
        'Đà Nẵng',
        'https://images.unsplash.com/photo-1510798831971-661eb04b3739?w=800&q=80',
        4, 8.8, 415, 'VILLA', 'APPROVED', 7, NOW(), NOW()
    );

    -- Rooms – Villa 1: The Anam Villa Nha Trang (id=4) — VILLA (default 12%)
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category, commission_rate_override) VALUES
    (4, 'ANM-GDN', 'Garden Pool Villa',       '1 giường King size',            2, 3500000, 4,
    'Biệt thự 80m² có hồ bơi riêng, vườn nhiệt đới, view núi. Bao gồm bữa sáng đặt tại phòng.',
    'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=400&q=70',
    'STANDARD', NULL),    -- ← null = VILLA default 12%
    (4, 'ANM-BCH', 'Beachfront Pool Villa',   '1 giường King cỡ lớn',          2, 5800000, 2,
    'Biệt thự 120m² sát biển, hồ bơi private infinity tràn ra biển. Butler phục vụ 24/7.',
    'https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=400&q=70',
    'VIP', 15.00),        -- ← 15% override (Beachfront VIP)
    (4, 'ANM-FAM', 'Family Grand Villa',      '3 giường King',                  6, 8500000, 1,
    'Biệt thự 200m² hai tầng, 3 phòng ngủ, phòng khách rộng, bếp ăn full, hồ bơi private.',
    'https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=400&q=70',
    'FAMILY', 10.00);     -- ← 10% override (ưu đãi gia đình villa)

    -- Rooms – Villa 2: Ba Na Hills Forest Villa (id=5) — VILLA (default 12%)
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category, commission_rate_override) VALUES
    (5, 'BNH-BNG', 'Forest Bungalow',         '1 giường King size',            2, 2200000, 5,
    'Bungalow 60m² gỗ tự nhiên, sàn kính ngắm rừng, bồn tắm thảo mộc, hơi sương sáng sớm.',
    'https://images.unsplash.com/photo-1470770841072-f978cf4d019e?w=400&q=70',
    'STANDARD', NULL),    -- ← null = VILLA default 12%
    (5, 'BNH-TWN', 'Twin Cabin',              '2 giường đơn',                  3, 1600000, 4,
    'Cabin 45m² dành cho nhóm bạn, view đồi thông, sân hiên ngoài trời có bếp nướng BBQ.',
    'https://images.unsplash.com/photo-1444201983204-c43cbd584d93?w=400&q=70',
    'FAMILY', NULL),      -- ← null = VILLA default 12%
    (5, 'BNH-SUI', 'Treetop Suite',           '1 giường King size',            2, 3800000, 2,
    'Suite trên cây 70m², ban công 360° nhìn toàn rừng, bồn tắm jacuzzi ngoài trời.',
    'https://images.unsplash.com/photo-1537640538966-79f369143f8f?w=400&q=70',
    'SUITE', 16.00);      -- ← 16% override (Treetop Suite)

    -- ─── ACCOMMODATIONS — HOMESTAY (owner_id=8 = partner4@travelmate.vn HOMESTAY) ─────────────
    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at) VALUES
    (
        'Hoa Lư Riverside Homestay',
        'Nhà dân truyền thống 3 gian mái ngói ven sông Thu Bồn, phố cổ Hội An chỉ 5 phút đi bộ. Bữa sáng bánh mì Hội An tự làm, xe đạp miễn phí.',
        '42 Nguyễn Trung Trực, Cẩm Châu',
        'Hội An',
        'https://images.unsplash.com/photo-1586375300773-8384e3e4916f?w=800&q=80',
        3, 8.9, 289, 'HOMESTAY', 'APPROVED', 8, NOW(), NOW()
    ),
    (
        'Mộc Nhiên Garden Homestay Đà Lạt',
        'Căn nhà gỗ thông Đà Lạt phong cách Pháp cổ giữa vườn hoa dã quỳ. Lò sưởi củi, bếp nấu chung, view đồi thông yên tĩnh tuyệt đối.',
        '18 Đường Vạn Kiếp, Phường 5',
        'Đà Lạt',
        'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&q=80',
        3, 8.5, 178, 'HOMESTAY', 'APPROVED', 8, NOW(), NOW()
    );

    -- Rooms – Homestay 1: Hoa Lư Riverside (id=6) — HOMESTAY (default 10%)
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category, commission_rate_override) VALUES
    (6, 'HLR-STD', 'Phòng Truyền Thống',      '1 giường đôi',                  2, 320000,  4,
    'Phòng 20m² trang trí bằng đồ gốm Chu Đậu và tranh lụa Hội An. Nhà tắm chung sạch sẽ.',
    'https://images.unsplash.com/photo-1540518614846-7eded433c457?w=400&q=70',
    'STANDARD', NULL),    -- ← null = HOMESTAY default 10%
    (6, 'HLR-DLX', 'Phòng Deluxe Riêng',     '1 giường đôi',                  2, 480000,  3,
    'Phòng 25m² nhà tắm riêng, cửa sổ nhìn vườn, bao gồm bữa sáng phở và bánh mì.',
    'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=400&q=70',
    'DELUXE', NULL),      -- ← null = HOMESTAY default 10%
    (6, 'HLR-FAM', 'Phòng Gia Đình Ven Sông', '2 giường đôi',                  4, 750000,  2,
    'Phòng 35m² ban công nhìn sông Thu Bồn, lý tưởng cho gia đình nhỏ có trẻ em.',
    'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=400&q=70',
    'FAMILY', 8.00);      -- ← 8% override (ưu đãi gia đình homestay)

    -- Rooms – Homestay 2: Mộc Nhiên Đà Lạt (id=7) — HOMESTAY (default 10%)
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category, commission_rate_override) VALUES
    (7, 'MND-STD', 'Phòng Nhà Gỗ Tiêu Chuẩn', '1 giường đôi',                 2, 390000,  5,
    'Phòng gỗ thông 22m², lò sưởi mini, nội thất vintage, bao gồm bữa sáng bánh mì thịt nướng.',
    'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=400&q=70',
    'STANDARD', NULL),    -- ← null = HOMESTAY default 10%
    (7, 'MND-ATT', 'Phòng Áp Mái View Đồi',   '1 giường King size',            2, 580000,  3,
    'Phòng áp mái 28m² cửa sổ mái kính, view đồi thông xanh, đặc biệt yên tĩnh.',
    'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=400&q=70',
    'DELUXE', 12.00),     -- ← 12% override (Áp Mái premium view)
    (7, 'MND-FAM', 'Phòng Gia Đình Vườn Hoa', '1 giường King + 1 đơn',         4, 880000,  2,
    'Phòng 38m² sân thượng riêng nhìn ra vườn hoa dã quỳ, thích hợp gia đình hoặc nhóm nhỏ.',
    'https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=400&q=70',
    'FAMILY', NULL);      -- ← null = HOMESTAY default 10%

    -- ─── ACCOMMODATIONS — RESORT (owner_id=4 = partner2@travelmate.vn RESORT) ───────────────
    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at) VALUES
    (
        'Vinpearl Resort & Spa Nha Trang',
        'Khu nghỉ dưỡng 5 sao trên đảo Hòn Tre huyền thoại, kết nối bằng cáp treo vượt biển dài nhất thế giới. Công viên nước, sân golf, casino, spa đẳng cấp quốc tế.',
        'Đảo Hòn Tre, Vĩnh Nguyên',
        'Nha Trang',
        'https://images.unsplash.com/photo-1537996194471-e657df975ab4?w=800&q=80',
        5, 9.1, 1847, 'RESORT', 'APPROVED', 4, NOW(), NOW()
    ),
    (
        'Furama Resort Đà Nẵng',
        'Khu nghỉ dưỡng 5 sao hướng biển Mỹ Khê nổi tiếng. Hồ bơi nước ngọt + muối, nhà hàng fine dining, spa phong cách Á Đông, bãi biển riêng 200m.',
        '68 Hồ Xuân Hương, Mỹ An',
        'Đà Nẵng',
        'https://images.unsplash.com/photo-1562790351-d273a961e0e9?w=800&q=80',
        5, 9.0, 1203, 'RESORT', 'APPROVED', 4, NOW(), NOW()
    );

    -- ─── DEMO: PENDING + REJECTED accommodation (để demo flow duyệt) ─
    -- partner1 (id=3) gửi 1 hotel PENDING mới chưa duyệt
    INSERT INTO accommodations (name, description, address, city, thumbnail_url, star_rating, rating, review_count, property_type, approval_status, owner_id, created_at, updated_at) VALUES
    (
        '[PENDING DEMO] Da Lat Mountain Boutique Hotel',
        'Khách sạn boutique phong cách núi rừng, view thung lũng Đà Lạt. Vừa được partner đăng ký — chờ Admin duyệt.',
        '101 Triệu Việt Vương, Phường 4',
        'Đà Lạt',
        'https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=800&q=80',
        4, 0.0, 0, 'HOTEL', 'PENDING', 3, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)
    ),
    -- partner1 (id=3) có 1 hotel đã bị từ chối
    (
        '[REJECTED DEMO] Da Lat Fake Hotel',
        'Listing bị Admin từ chối do thông tin không hợp lệ.',
        'Địa chỉ không rõ ràng',
        'Đà Lạt',
        '',
        2, 0.0, 0, 'HOTEL', 'REJECTED', 3, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
    );

    -- Rooms – Resort 1: Vinpearl Nha Trang (id=8) — RESORT (default 18%)
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category, commission_rate_override) VALUES
    (8, 'VNT-DLX', 'Deluxe Ocean View',       '1 giường King size',            2, 2800000, 10,
    'Phòng Deluxe 42m² view biển, bao gồm vé cáp treo và công viên giải trí Vinpearl Land.',
    'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=400&q=70',
    'DELUXE', NULL),      -- ← null = RESORT default 18%
    (8, 'VNT-SUI', 'Junior Suite Beachfront', '1 giường King cỡ lớn',          2, 4500000, 5,
    'Suite 65m² ban công hướng biển, bồn tắm jacuzzi trong phòng, dịch vụ butler cao cấp.',
    'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=400&q=70',
    'SUITE', 22.00),      -- ← 22% override (Junior Suite Beachfront premium)
    (8, 'VNT-VIL', 'Pool Villa',              '2 giường King',                  4, 9800000, 2,
    'Villa riêng 150m² với hồ bơi private, 2 phòng ngủ, bếp ăn, phù hợp gia đình hoặc tuần trăng mật.',
    'https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=400&q=70',
    'FAMILY', 15.00);     -- ← 15% override (ưu đãi Pool Villa gia đình)

    -- Rooms – Resort 2: Furama Đà Nẵng (id=9) — RESORT (default 18%)
    INSERT INTO rooms (accommodation_id, room_code, room_name, bed_type, capacity, price_per_night, available_quantity, description, image_url, room_category, commission_rate_override) VALUES
    (9, 'FDN-DLX', 'Deluxe Garden View',      '1 giường King size',            2, 2400000, 8,
    'Phòng 40m² view vườn nhiệt đới, bao gồm bữa sáng buffet tại nhà hàng La Maison 1888.',
    'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=400&q=70',
    'DELUXE', NULL),      -- ← null = RESORT default 18%
    (9, 'FDN-BCH', 'Beachfront Superior',     '1 giường King size',            2, 3600000, 6,
    'Phòng 48m² view biển Mỹ Khê, bãi tắm riêng 200m, ghế nằm và ô dù phục vụ tận nơi.',
    'https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=400&q=70',
    'VIP', 20.00),        -- ← 20% override (Beachfront VIP cao cấp)
    (9, 'FDN-FAM', 'Family Suite',            '2 giường đôi',                  5, 5200000, 3,
    'Suite gia đình 80m², 2 phòng ngủ, phòng khách riêng, view biển panorama, bếp nhỏ.',
    'https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=400&q=70',
    'FAMILY', 15.00);     -- ← 15% override (ưu đãi Family Suite resort)

    -- =============================================
    -- BOOKING + PAYMENT SAMPLE DATA
    -- user_id=2 (user@travelmate.vn)
    -- =============================================

    -- ─── Booking 1: Cọc 30% — PENDING_ADMIN_APPROVAL ───────────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        note, created_at, updated_at)
    VALUES (
        'BK-LATA-STD-0001', 2, 1, 1,
        DATE_ADD(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 5 DAY),
        2, 0, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        1300000, 390000, 910000,
        'PENDING_ADMIN_APPROVAL', 'DEPOSIT_30', 'PENDING_ADMIN_APPROVAL',
        NULL, NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 390000, CONCAT('TXN-', UNIX_TIMESTAMP()*1000), 'PENDING_ADMIN_APPROVAL', NOW(), 'Thanh toán demo VNPay - Cọc 30%');

    -- ─── Booking 2: 100% — PENDING_ADMIN_APPROVAL ───
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        note, created_at, updated_at)
    VALUES (
        'BK-TLP-SUP-0001', 2, 2, 6,
        DATE_ADD(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 9 DAY),
        2, 0, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        1240000, 1240000, 0,
        'PENDING_ADMIN_APPROVAL', 'FULL_PAYMENT', 'PENDING_ADMIN_APPROVAL',
        NULL, NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 1240000, CONCAT('TXN-', UNIX_TIMESTAMP()*1001), 'PENDING_ADMIN_APPROVAL', NOW(), 'Thanh toán demo VNPay - Thanh toán 100%');

    -- ─── Booking 3: Cọc 30% — CONFIRMED (partner chờ xác nhận giữ phòng) ─
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
        'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PENDING_PARTNER_CONFIRMATION', NULL, NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 720000, CONCAT('TXN-', UNIX_TIMESTAMP()*1002), 'APPROVED', NOW(), 'Thanh toán demo VNPay - Cọc 30% — admin đã duyệt');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 9;

    -- ─── Booking 4: 100% — CONFIRMED (partner đã xác nhận giữ phòng) ─────
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
        'PARTNER_CONFIRMED', 'Partner đã xác nhận giữ phòng cho khách.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 1700000, CONCAT('TXN-', UNIX_TIMESTAMP()*1003), 'APPROVED', NOW(), 'Thanh toán demo VNPay - 100% — admin đã duyệt');
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
        'Khách không đến check-in. Cọc 30% (288.000đ) bị giữ: hoa hồng HOTEL 15% = 43.200đ → Partner nhận 244.800đ.',
        DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 288000, CONCAT('TXN-', UNIX_TIMESTAMP()*1004), 'DEPOSIT_FORFEITED', DATE_SUB(NOW(), INTERVAL 5 DAY), 'Cọc 30% bị giữ lại do khách không đến check-in.');

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
        'PARTNER_CONFIRMED', 'Khách đã check-in. Partner đã xác nhận giữ phòng.',
        DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 4950000, CONCAT('TXN-', UNIX_TIMESTAMP()*1005), 'APPROVED', DATE_SUB(NOW(), INTERVAL 7 DAY), 'Thanh toán 100% — đã check-in');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 3600000, CONCAT('TXN-', UNIX_TIMESTAMP()*1006), 'APPROVED', DATE_SUB(NOW(), INTERVAL 12 DAY), 'Thanh toán 100% — đã hoàn tất');

    -- ─── Booking 8: CONFIRMED — acc8 Vinpearl (partner2), chờ partner2 xác nhận ───
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
        'PENDING_PARTNER_CONFIRMATION', NULL, NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 2520000, CONCAT('TXN-', UNIX_TIMESTAMP()*1007), 'APPROVED', NOW(), 'Cọc 30% Vinpearl DLX — partner2 chờ xác nhận');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 25;

    -- ─── Booking 9: CONFIRMED — acc8 Vinpearl (partner2), đã xác nhận ───
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
        'PARTNER_CONFIRMED', 'Partner2 đã xác nhận giữ phòng Junior Suite.',
        DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 13500000, CONCAT('TXN-', UNIX_TIMESTAMP()*1008), 'APPROVED', DATE_SUB(NOW(), INTERVAL 2 DAY), '100% Vinpearl SUI — partner2 đã xác nhận');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 1740000, CONCAT('TXN-', UNIX_TIMESTAMP()*1009), 'APPROVED', DATE_SUB(NOW(), INTERVAL 10 DAY), 'Homestay Mộc Nhiên — hoàn tất');

    -- ─── Booking 11: PENDING_ADMIN_APPROVAL — acc4 Anam Villa (partner2) ───
    -- acc4 Anam Villa có rooms: ANM-GDN(id=13), ANM-BCH(id=14), ANM-FAM(id=15)
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        note, created_at, updated_at)
    VALUES (
        'BK-ANM-GDN-0001', 2, 4, 13,
        DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 13 DAY),
        2, 0, 1,
        'Hoàng Văn Hùng', '0911 777 888', 'user@travelmate.vn',
        10500000, 3150000, 7350000,
        'PENDING_ADMIN_APPROVAL', 'DEPOSIT_30', 'PENDING_ADMIN_APPROVAL',
        NULL, NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 3150000, CONCAT('TXN-', UNIX_TIMESTAMP()*1010), 'PENDING_ADMIN_APPROVAL', NOW(), 'Cọc 30% Anam Garden Villa — chờ admin duyệt');

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
        'PARTNER_CONFIRMED', 'Khách không đến check-in. Cọc 30% (1.440.000đ) bị giữ: hoa hồng RESORT 18% = 259.200đ → Partner nhận 1.180.800đ.',
        DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 1440000, CONCAT('TXN-', UNIX_TIMESTAMP()*1011), 'DEPOSIT_FORFEITED', DATE_SUB(NOW(), INTERVAL 6 DAY), 'Cọc 30% Furama DLX — no-show mất cọc');

    -- ─── Booking 13: CONFIRMED — acc6 Hoa Lư (partner1), chờ xác nhận ───
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
        'PENDING_PARTNER_CONFIRMATION', NULL, NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 960000, CONCAT('TXN-', UNIX_TIMESTAMP()*1012), 'APPROVED', NOW(), '100% Homestay Hội An — partner1 chờ xác nhận');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 6600000, CONCAT('TXN-', UNIX_TIMESTAMP()*1013), 'APPROVED', DATE_SUB(NOW(), INTERVAL 3 DAY), '100% Ba Na Bungalow — đã check-in');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 16;

    -- ─── Booking 15: CONFIRMED — PARTNER_CANCELLED (partner từ chối, admin cần xử lý) ───
    -- acc2 Tulip Hotel (partner1), room TLP-SUP (id=6): demo partner từ chối giữ phòng
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 1240000, CONCAT('TXN-', UNIX_TIMESTAMP()*1014), 'APPROVED', DATE_SUB(NOW(), INTERVAL 1 DAY), 'Thanh toán 100% — partner đã hủy, cần admin xử lý hoàn tiền');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 6;

    -- =============================================
    -- REVIEWS SAMPLE DATA — cho booking COMPLETED
    -- =============================================

    -- Review cho Booking #7 (BK-LATA-FAM-0001, acc1 LATA Hotel, COMPLETED)
    -- booking_id = 7, user_id = 2, accommodation_id = 1
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 1, 7, 5, 'Phòng rất sạch sẽ, nhân viên nhiệt tình. View đẹp, gần trung tâm chợ đêm Đà Lạt. Phòng gia đình rộng rãi, rất phù hợp cho gia đình có trẻ nhỏ. Lần sau sẽ quay lại!', DATE_SUB(NOW(), INTERVAL 7 DAY));

    -- Review cho Booking #10 (BK-MND-ATT-0001, acc7 Mộc Nhiên Homestay, COMPLETED)
    -- booking_id = 10, user_id = 2, accommodation_id = 7
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 7, 10, 4, 'Homestay rất yên tĩnh, không gian thơ mộng giữa vườn hoa dã quỳ. Phòng áp mái có cửa sổ kính nhìn đồi thông rất lãng mạn. WiFi buổi tối hơi yếu nhưng nhìn chung rất đáng tiền.', DATE_SUB(NOW(), INTERVAL 5 DAY));

    -- Cập nhật rating + reviewCount cho accommodation sau khi seed review
    -- acc1 LATA Hotel: 1 review, 5 sao → rating = 10.0
    UPDATE accommodations SET rating = 10.0, review_count = 1 WHERE id = 1;
    -- acc7 Mộc Nhiên: 1 review, 4 sao → rating = 8.0
    UPDATE accommodations SET rating = 8.0, review_count = 1 WHERE id = 7;


    -- =============================================
    -- HƯỚNG DẪN TÀI KHOẢN DEMO
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
    -- #1  BK-LATA-STD-0001 | acc1/r1  | PENDING_ADMIN_APPROVAL | Cọc 30% → demo admin duyệt
    -- #2  BK-TLP-SUP-0001  | acc2/r6  | PENDING_ADMIN_APPROVAL | 100%    → demo admin từ chối
    -- #3  BK-TMG-DLX-0001  | acc3/r9  | CONFIRMED | PENDING_PARTNER_CONFIRMATION (partner1 chờ)
    -- #4  BK-LATA-DLX-0001 | acc1/r2  | CONFIRMED | PARTNER_CONFIRMED (partner1 đã xác nhận)
    -- #5  BK-TLP-STD-0001  | acc2/r5  | NO_SHOW   | DEPOSIT_FORFEITED (mất cọc)
    -- #6  BK-TMG-PRE-0001  | acc3/r10 | CHECKED_IN | PARTNER_CONFIRMED (partner1)
    -- #7  BK-LATA-FAM-0001 | acc1/r3  | COMPLETED  | PARTNER_CONFIRMED (partner1)
    -- #8  BK-VNT-DLX-0001  | acc8/r25 | CONFIRMED | PENDING_PARTNER_CONFIRMATION (partner2 chờ)
    -- #9  BK-VNT-SUI-0001  | acc8/r26 | CONFIRMED | PARTNER_CONFIRMED (partner2 xác nhận)
    -- #10 BK-MND-ATT-0001  | acc7/r23 | COMPLETED  | PARTNER_CONFIRMED (partner2)
    -- #11 BK-ANM-GDN-0001  | acc4/r13 | PENDING_ADMIN_APPROVAL | Cọc 30% (partner2)
    -- #12 BK-FDN-DLX-0001  | acc9/r28 | NO_SHOW   | DEPOSIT_FORFEITED (partner1 - Furama)
    -- #13 BK-HLR-DLX-0001  | acc6/r20 | CONFIRMED | PENDING_PARTNER_CONFIRMATION (partner1 chờ)
    -- #14 BK-BNH-BNG-0001  | acc5/r16 | CHECKED_IN | PARTNER_CONFIRMED (partner1 - Ba Na Villa)
    --
    -- DEMO SCENARIOS:
    -- Admin login → /admin/bookings → thấy 15 bookings
    --   → Duyệt #1 (BK-LATA-STD-0001): CONFIRMED, partner_status=PENDING_PARTNER_CONFIRMATION
    --   → Từ chối #2 (BK-TLP-SUP-0001): CANCELLED
    --   → Xem #15 (BK-TLP-SUP-0002): có nút "🚫 Xử lý partner hủy" vì PARTNER_CANCELLED
    -- Partner1 login → /partner/bookings
    --   → Nút "Xác nhận giữ phòng" ở #3 (BK-TMG-DLX-0001) và #13 (BK-HLR-DLX-0001)
    --   → Nút "Check-in khách" ở booking CONFIRMED + PARTNER_CONFIRMED
    --   → Nút "Hoàn tất / Check-out" ở booking CHECKED_IN (#6, #14)
    -- Admin Chi tiết booking → /admin/bookings/{id}
    --   → Phần "Admin Override" (collapsible) ở CONFIRMED/CHECKED_IN
    --   → Chỉ dùng khi partner không thao tác được
    -- Admin → /admin/settlements → "Tạo quyết toán tuần trước" → generate-weekly

    -- =============================================
    -- DEMO AVAILABILITY DATA — Booking cho khoảng +10 → +14 ngày
    -- Mục đích: Demo trang /admin/availability và /partner/availability
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
        'PARTNER_CONFIRMED', 'Partner1 đã xác nhận giữ phòng Deluxe.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 3400000, CONCAT('TXN-', UNIX_TIMESTAMP()*2001), 'APPROVED', NOW(), '100% LATA-DLX #2 — partner xác nhận');
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
        'PARTNER_CONFIRMED', 'Partner1 xác nhận 2 phòng Deluxe cho nhóm.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 2040000, CONCAT('TXN-', UNIX_TIMESTAMP()*2002), 'APPROVED', NOW(), 'Cọc 30% LATA-DLX #3 — 2 phòng');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 7200000, CONCAT('TXN-', UNIX_TIMESTAMP()*2003), 'APPROVED', NOW(), '100% LATA-SUI — FULL');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 9600000, CONCAT('TXN-', UNIX_TIMESTAMP()*2004), 'APPROVED', NOW(), '100% TMG-DLX #2 2 phòng');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 2880000, CONCAT('TXN-', UNIX_TIMESTAMP()*2005), 'APPROVED', NOW(), 'Cọc 30% TMG-DLX #3 2 phòng');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 14400000, CONCAT('TXN-', UNIX_TIMESTAMP()*2006), 'APPROVED', NOW(), '100% TMG-DLX #4 3 phòng');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 33600000, CONCAT('TXN-', UNIX_TIMESTAMP()*2007), 'APPROVED', NOW(), '100% VNT-DLX #2 3 phòng');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 10080000, CONCAT('TXN-', UNIX_TIMESTAMP()*2008), 'APPROVED', NOW(), 'Cọc 30% VNT-DLX #3 3 phòng');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 33600000, CONCAT('TXN-', UNIX_TIMESTAMP()*2009), 'APPROVED', NOW(), '100% VNT-DLX #4 3 phòng');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 36000000, CONCAT('TXN-', UNIX_TIMESTAMP()*2010), 'APPROVED', NOW(), '100% VNT-SUI #2 2 phòng');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 10800000, CONCAT('TXN-', UNIX_TIMESTAMP()*2011), 'APPROVED', NOW(), 'Cọc 30% VNT-SUI #3 2 phòng');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 46400000, CONCAT('TXN-', UNIX_TIMESTAMP()*2012), 'APPROVED', NOW(), '100% ANM-BCH 2 phòng — FULL');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 2560000, CONCAT('TXN-', UNIX_TIMESTAMP()*2013), 'APPROVED', NOW(), '100% HLR-STD 2 phòng');
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
        'PENDING_PARTNER_CONFIRMATION', 'Hoa Lư Standard 1 phòng — partner chờ xác nhận.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 384000, CONCAT('TXN-', UNIX_TIMESTAMP()*2014), 'APPROVED', NOW(), 'Cọc 30% HLR-STD #2');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 19;

    -- =============================================
    -- BACKFILL — Chạy nếu DB cũ thiếu cột owner_id
    -- =============================================
    -- UPDATE accommodations
    -- SET owner_id = (SELECT id FROM users WHERE email = 'partner@travelmate.vn' LIMIT 1)
    -- WHERE owner_id IS NULL AND property_type = 'HOTEL';
    --
    -- UPDATE bookings SET partner_status = 'PENDING_PARTNER_CONFIRMATION'
    -- WHERE booking_status = 'CONFIRMED' AND partner_status IS NULL;

    -- =============================================
    -- SEED DATA — VOUCHERS (Hướng 2)
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

    -- Voucher do Partner1 (id=3) tạo cho accommodation của mình (PARTNER_ACCOMMODATION, costBearer=PARTNER)
    -- partner1 sở hữu acc1 (LATA Hotel, id=1)
    INSERT INTO vouchers (code, name, description, discount_type, discount_value,
        max_discount_amount, min_order_amount, start_date, end_date,
        active, voucher_scope, cost_bearer, owner_id, accommodation_id, created_at) VALUES
    ('LATA20', 'LATA Hotel Ưu Đãi 20%', 'Giảm 20% cho đặt phòng tại LATA Hotel — partner chịu chi phí.',
        'PERCENT', 20.00, 800000, 650000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 45 DAY), 1, 'PARTNER_ACCOMMODATION', 'PARTNER', 3, 1, NOW());

    -- Voucher do Partner2 (id=4) tạo cho room của mình (PARTNER_ROOM, costBearer=PARTNER)
    -- partner2 sở hữu acc8 Vinpearl (room VNT-DLX id=25)
    INSERT INTO vouchers (code, name, description, discount_type, discount_value,
        max_discount_amount, min_order_amount, start_date, end_date,
        active, voucher_scope, cost_bearer, owner_id, room_id, created_at) VALUES
    ('VNT100K', 'Vinpearl Giảm 100K', 'Giảm 100.000đ khi đặt phòng Deluxe Ocean View tại Vinpearl.',
        'FIXED_AMOUNT', 100000, NULL, 2800000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 1, 'PARTNER_ROOM', 'PARTNER', 4, 25, NOW());

    -- Voucher do Partner3 (id=7) tạo cho accommodation của mình (PARTNER_ACCOMMODATION, costBearer=PARTNER)
    -- partner3 sở hữu acc4 (The Anam Villa Nha Trang, id=4)
    INSERT INTO vouchers (code, name, description, discount_type, discount_value,
        max_discount_amount, min_order_amount, start_date, end_date,
        active, voucher_scope, cost_bearer, owner_id, accommodation_id, created_at) VALUES
    ('ANAM15', 'Anam Villa Ưu Đãi 15%', 'Giảm 15% tối đa 1.200.000đ cho đặt phòng tại The Anam Villa Nha Trang.',
        'PERCENT', 15.00, 1200000, 3000000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 60 DAY), 1, 'PARTNER_ACCOMMODATION', 'PARTNER', 7, 4, NOW());

    -- Voucher do Partner4 (id=8) tạo cho room của mình (PARTNER_ROOM, costBearer=PARTNER)
    -- partner4 sở hữu acc6 Hoa Lư (room HLR-DLX id=20)
    INSERT INTO vouchers (code, name, description, discount_type, discount_value,
        max_discount_amount, min_order_amount, start_date, end_date,
        active, voucher_scope, cost_bearer, owner_id, room_id, created_at) VALUES
    ('HOALUU50K', 'Hoa Lư Giảm 50K', 'Giảm 50.000đ khi đặt phòng Deluxe River View tại Hoa Lư Riverside Homestay.',
        'FIXED_AMOUNT', 50000, NULL, 600000,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 45 DAY), 1, 'PARTNER_ROOM', 'PARTNER', 8, 20, NOW());

    -- =============================================
    -- SEED DATA — PARTNER SETTLEMENTS (Tuần 1 hiện tại — PENDING)
    -- partner1 HOTEL: booking #4 LATA-DLX (1.700.000)
    -- partner2 RESORT: booking #9 VNT-SUI (13.500.000)
    -- partner3 VILLA: booking #11 ANM-GDN pending (chưa hoàn tất)
    -- partner4 HOMESTAY: booking #10 MND-ATT (1.740.000)
    -- =============================================
    INSERT INTO partner_settlements
        (partner_id, period_start, period_end,
        gross_amount, commission_amount, voucher_deduction_amount, payout_amount,
        settlement_status, settlement_date, note, created_at)
    VALUES
    -- partner1 (HOTEL): booking #7 LATA-FAM hoàn tất trong tuần 1, gross=3.600.000, comm15%=540.000, payout=3.060.000
    (3,
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+5) DAY),
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())-1) DAY),
    3600000, 540000, 0, 3060000,
    'PAID', DATE_SUB(NOW(), INTERVAL 2 DAY),
    'Admin đã thanh toán cho partner Sunrise Sapa Lodge (HOTEL). Booking LATA-FAM hoàn tất.', DATE_SUB(NOW(), INTERVAL 2 DAY)),
    -- partner2 (RESORT): booking #9 VNT-SUI xác nhận, gross=13.500.000, comm18%=2.430.000, payout=11.070.000
    (4,
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+5) DAY),
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())-1) DAY),
    13500000, 2430000, 0, 11070000,
    'PAID', DATE_SUB(NOW(), INTERVAL 2 DAY),
    'Admin đã thanh toán cho partner Blue Ocean Resort (RESORT). Booking VNT-SUI hoàn tất.', DATE_SUB(NOW(), INTERVAL 2 DAY)),
    -- partner3 (VILLA): booking #22 ANM-GDN hoàn tất, gross=7.000.000, comm12%=840.000, payout=6.160.000
    (7,
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+5) DAY),
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())-1) DAY),
    7000000, 840000, 0, 6160000,
    'PAID', DATE_SUB(NOW(), INTERVAL 2 DAY),
    'Admin đã thanh toán cho partner Green Hills Villa (VILLA). Booking ANM-GDN hoàn tất.', DATE_SUB(NOW(), INTERVAL 2 DAY)),
    -- partner4 (HOMESTAY): booking #10 MND-ATT hoàn tất, gross=1.740.000, comm10%=174.000, payout=1.566.000
    (8,
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+5) DAY),
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())-1) DAY),
    1740000, 174000, 0, 1566000,
    'PAID', DATE_SUB(NOW(), INTERVAL 2 DAY),
    'Admin đã thanh toán cho partner Mekong Homestay (HOMESTAY). Booking MND-ATT hoàn tất.', DATE_SUB(NOW(), INTERVAL 2 DAY));

    -- =============================================
    -- KIỂM TRA SAU KHI CHẠY SQL
    -- =============================================
    -- SELECT id, name, property_type, approval_status, owner_id FROM accommodations ORDER BY id;
    -- SELECT id, room_code, accommodation_id FROM rooms ORDER BY id;
    -- SELECT booking_code, booking_status, payment_status, partner_status FROM bookings ORDER BY id;
    -- SELECT id, code, voucher_scope, cost_bearer, active FROM vouchers ORDER BY id;
    -- SELECT id, partner_id, settlement_status, payout_amount FROM partner_settlements ORDER BY id;
    -- SELECT COUNT(*) FROM bookings;           -- kỳ vọng: 14
    -- SELECT COUNT(*) FROM rooms;              -- kỳ vọng: 30
    -- SELECT COUNT(*) FROM accommodations;     -- kỳ vọng: 11 (+ 2 PENDING/REJECTED = 13 total)
    -- SELECT COUNT(*) FROM vouchers;           -- kỳ vọng: 5
    -- SELECT COUNT(*) FROM partner_settlements;-- kỳ vọng: 2

    -- =============================================
    -- SEED DATA BỔ SUNG — Lịch sử đặt phòng (Bookings 15–22)
    -- Mục đích: dữ liệu phong phú cho Revenue/Settlement demo
    -- Trải đều 5 tuần để biểu đồ revenue có đủ điểm dữ liệu
    -- =============================================

    -- ─── Booking 15: COMPLETED — LATA Hotel LATA-STD, voucher SUMMER10 (ADMIN) ───
    -- Kỳ 2 tuần trước (Apr 13–19). User được giảm 10%, admin chịu chi phí.
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 1170000,
        CONCAT('TXN-LATA-STD2-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 17 DAY), 'LATA-STD 2 đêm với SUMMER10 — hoàn tất');

    -- ─── Booking 16: COMPLETED — Vinpearl VNT-DLX (partner2), không voucher ───
    -- Kỳ 2 tuần trước (Apr 13–19).
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 5600000,
        CONCAT('TXN-VNT-DLX2-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 16 DAY), 'VNT-DLX 2 đêm — hoàn tất');

    -- ─── Booking 17: COMPLETED — LATA Hotel LATA-DLX, voucher LATA20 (PARTNER) ───
    -- Kỳ 3 tuần trước (Apr 6–12). Partner chịu chi phí voucher → trừ vào settlement.
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 2040000,
        CONCAT('TXN-LATA-DLX2-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 24 DAY), 'LATA-DLX 3 đêm với LATA20 — hoàn tất');

    -- ─── Booking 18: COMPLETED — Mộc Nhiên Homestay MND-STD (partner2), không voucher ───
    -- Kỳ 3 tuần trước (Apr 6–12).
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 780000,
        CONCAT('TXN-MND-STD1-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 24 DAY), 'MND-STD 2 đêm — hoàn tất');

    -- ─── Booking 19: COMPLETED — Furama FDN-DLX (partner1), không voucher ───
    -- Kỳ 4 tuần trước (Mar 30–Apr 5).
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 4800000,
        CONCAT('TXN-FDN-DLX2-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 31 DAY), 'FDN-DLX 2 đêm — hoàn tất');

    -- ─── Booking 20: COMPLETED — Vinpearl VNT-DLX (partner2), voucher VNT100K (PARTNER) ───
    -- Kỳ 4 tuần trước (Mar 30–Apr 5). Partner chịu chi phí voucher 100K.
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 5500000,
        CONCAT('TXN-VNT-DLX3-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 30 DAY), 'VNT-DLX 2 đêm với VNT100K — hoàn tất');

    -- ─── Booking 21: COMPLETED — TM Grand TMG-DLX (partner1), không voucher ───
    -- Kỳ 5 tuần trước (Mar 23–29).
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 2400000,
        CONCAT('TXN-TMG-DLX2-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 38 DAY), 'TMG-DLX 2 đêm — hoàn tất');

    -- ─── Booking 22: COMPLETED — Anam Villa ANM-GDN (partner2), không voucher ───
    -- Kỳ 5 tuần trước (Mar 23–29).
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 7000000,
        CONCAT('TXN-ANM-GDN2-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 37 DAY), 'ANM-GDN 2 đêm — hoàn tất');

    -- =============================================
    -- REVIEWS BỔ SUNG — bookings 15, 17, 21, 22
    -- =============================================

    -- Booking #15 → acc1 LATA Hotel (5 sao)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 1, 15, 5,
    'Dùng voucher SUMMER10 rất hời! Phòng sạch sẽ, check-in nhanh, vị trí trung tâm Đà Lạt tiện lợi. Nhân viên lễ tân thân thiện và nhiệt tình hỗ trợ hành lý.',
    DATE_SUB(NOW(), INTERVAL 13 DAY));

    -- Booking #17 → acc1 LATA Hotel (4 sao, voucher LATA20)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 1, 17, 4,
    'Voucher LATA20 giảm được nhiều, phòng Deluxe rộng có ban công nhìn vườn đẹp. Bữa sáng ổn nhưng chưa đa dạng lắm. Nhìn chung rất đáng tiền, sẽ quay lại.',
    DATE_SUB(NOW(), INTERVAL 19 DAY));

    -- Booking #21 → acc3 TM Grand Hotel (5 sao)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 3, 21, 5,
    'TravelMate Grand Hotel thật sự xứng đáng 5 sao! Spa tuyệt vời, nhà hàng fine dining ngon, phòng view thung lũng cực đẹp. Dịch vụ butler tận tâm, chắc chắn sẽ quay lại.',
    DATE_SUB(NOW(), INTERVAL 34 DAY));

    -- Booking #22 → acc4 Anam Villa (5 sao)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 4, 22, 5,
    'The Anam Villa là thiên đường nghỉ dưỡng! Butler phục vụ tận tình, hồ bơi private sát biển, bữa sáng đặt tại phòng tuyệt hảo. Giá xứng đáng với đẳng cấp nhận được.',
    DATE_SUB(NOW(), INTERVAL 33 DAY));

    -- Booking #16 → acc8 Vinpearl Resort (5 sao)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 8, 16, 5,
    'Vinpearl Resort đẳng cấp! Bãi biển riêng tuyệt đẹp, hồ bơi lớn, phòng view biển thoáng mát. Dịch vụ chuyên nghiệp, bữa sáng buffet phong phú. Sẽ quay lại vào dịp khác!',
    DATE_SUB(NOW(), INTERVAL 12 DAY));

    -- Booking #18 → acc7 Mộc Nhiên Homestay (4 sao)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 7, 18, 4,
    'Homestay Mộc Nhiên yên tĩnh và thơ mộng, không gian xanh mướt giữa núi đồi Đà Lạt. Chủ nhà rất thân thiện, gợi ý nhiều địa điểm hay. WiFi ổn định hơn lần trước!',
    DATE_SUB(NOW(), INTERVAL 20 DAY));

    -- Booking #19 → acc9 Furama Resort (5 sao)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 9, 19, 5,
    'Furama Resort Đà Nẵng là kỳ nghỉ tuyệt vời nhất! Phòng rộng có ban công nhìn thẳng ra biển Mỹ Khê. Spa tuyệt vời, nhà hàng phục vụ tận tình. Chắc chắn sẽ quay lại!',
    DATE_SUB(NOW(), INTERVAL 27 DAY));

    -- Booking #20 → acc8 Vinpearl Resort (4 sao)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 8, 20, 4,
    'Kỳ nghỉ thứ hai tại Vinpearl, lần này dùng voucher VNT100K. Phòng Deluxe tiện nghi đầy đủ. Bãi biển đẹp nhưng khá đông vào cuối tuần. Nhìn chung rất đáng tiền.',
    DATE_SUB(NOW(), INTERVAL 26 DAY));

    -- Cập nhật rating + review_count (ghi đè tất cả về đúng giá trị cuối)
    -- acc1: 3 reviews (7=5★, 15=5★, 17=4★) → avg 4.67 × 2 = 9.3
    UPDATE accommodations SET rating = 9.3, review_count = 3 WHERE id = 1;
    -- acc3 TM Grand: 1 review (21=5★) → avg 5.0 × 2 = 10.0
    UPDATE accommodations SET rating = 10.0, review_count = 1 WHERE id = 3;
    -- acc4 Anam Villa: 1 review (22=5★) → avg 5.0 × 2 = 10.0
    UPDATE accommodations SET rating = 10.0, review_count = 1 WHERE id = 4;
    -- acc7 Mộc Nhiên: 2 reviews (10=4★, 18=4★) → avg 4.0 × 2 = 8.0
    UPDATE accommodations SET rating = 8.0, review_count = 2 WHERE id = 7;
    -- acc8 Vinpearl: 2 reviews (16=5★, 20=4★) → avg 4.5 × 2 = 9.0
    UPDATE accommodations SET rating = 9.0, review_count = 2 WHERE id = 8;
    -- acc9 Furama: 1 review (19=5★) → avg 5.0 × 2 = 10.0
    UPDATE accommodations SET rating = 10.0, review_count = 1 WHERE id = 9;

    -- =============================================
    -- SEED DATA BỔ SUNG — PARTNER SETTLEMENTS (4 tuần lịch sử)
    -- Công thức: payout = gross - commission - voucher_deduction
    -- Tỷ lệ commission: HOTEL 15% | VILLA 12% | HOMESTAY 10% | RESORT 18%
    -- =============================================

    -- ─── Tuần n=2 (Apr 13–19) — PAID ─────────────────────────────
    -- partner1 (HOTEL): booking #15 LATA-STD+SUMMER10, payment=1.170.000, comm15%=175.500, voucher_deduction=0(ADMIN), payout=994.500
    -- partner2 (RESORT): booking #16 VNT-DLX, payment=5.600.000, comm18%=1.008.000, payout=4.592.000
    -- partner3 (VILLA): không có booking COMPLETED tuần này
    -- partner4 (HOMESTAY): không có booking COMPLETED tuần này
    INSERT INTO partner_settlements
        (partner_id, period_start, period_end,
        gross_amount, commission_amount, voucher_deduction_amount, payout_amount,
        settlement_status, settlement_date, note, created_at)
    VALUES
    (3,
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+12) DAY),
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+6) DAY),
    1170000, 175500, 0, 994500,
    'PAID', DATE_SUB(NOW(), INTERVAL 3 DAY),
    'Tuần 2: LATA Hotel 1 booking — voucher SUMMER10 do Admin chịu, không trừ partner.',
    DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (4,
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+12) DAY),
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+6) DAY),
    5600000, 1008000, 0, 4592000,
    'PAID', DATE_SUB(NOW(), INTERVAL 3 DAY),
    'Tuần 2: Vinpearl Resort 1 booking — không có voucher.',
    DATE_SUB(NOW(), INTERVAL 3 DAY));

    -- ─── Tuần n=3 (Apr 6–12) — PAID ──────────────────────────────
    -- partner1 (HOTEL): booking #17 LATA-DLX+LATA20, payment=2.040.000, comm15%=306.000, voucher_deduction=510.000(PARTNER), payout=1.224.000
    -- partner2 (RESORT): không có booking tuần này
    -- partner3 (VILLA): không có booking tuần này
    -- partner4 (HOMESTAY): booking #18 MND-STD, payment=780.000, comm10%=78.000, payout=702.000
    INSERT INTO partner_settlements
        (partner_id, period_start, period_end,
        gross_amount, commission_amount, voucher_deduction_amount, payout_amount,
        settlement_status, settlement_date, note, created_at)
    VALUES
    (3,
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+19) DAY),
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+13) DAY),
    2040000, 306000, 510000, 1224000,
    'PAID', DATE_SUB(NOW(), INTERVAL 10 DAY),
    'Tuần 3: LATA Hotel 1 booking — voucher LATA20 do Partner chịu, trừ 510.000đ.',
    DATE_SUB(NOW(), INTERVAL 10 DAY)),
    (8,
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+19) DAY),
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+13) DAY),
    780000, 78000, 0, 702000,
    'PAID', DATE_SUB(NOW(), INTERVAL 10 DAY),
    'Tuần 3: Mộc Nhiên Garden Homestay 1 booking — không có voucher.',
    DATE_SUB(NOW(), INTERVAL 10 DAY));

    -- ─── Tuần n=4 (Mar 30–Apr 5) — PAID ──────────────────────────
    -- partner2 (RESORT): booking #19 FDN-DLX, payment=4.800.000, comm18%=864.000, payout=3.936.000
    --                    booking #20 VNT-DLX+VNT100K, payment=5.500.000, comm18%=990.000, voucher_deduction=100.000(PARTNER), payout=4.410.000
    -- partner2 gộp 2 bookings: gross=10.300.000, comm=1.854.000, deduction=100.000, payout=8.346.000
    INSERT INTO partner_settlements
        (partner_id, period_start, period_end,
        gross_amount, commission_amount, voucher_deduction_amount, payout_amount,
        settlement_status, settlement_date, note, created_at)
    VALUES
    (4,
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+26) DAY),
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+20) DAY),
    10300000, 1854000, 100000, 8346000,
    'PAID', DATE_SUB(NOW(), INTERVAL 17 DAY),
    'Tuần 4: Furama Resort + Vinpearl Resort 2 bookings — voucher VNT100K do Partner chịu, trừ 100.000đ.',
    DATE_SUB(NOW(), INTERVAL 17 DAY));

    -- ─── Tuần n=5 (Mar 23–29) — PAID ─────────────────────────────
    -- partner1 (HOTEL): booking #21 TMG-DLX, payment=2.400.000, comm15%=360.000, payout=2.040.000
    -- partner3 (VILLA): booking #22 ANM-GDN, payment=7.000.000, comm12%=840.000, payout=6.160.000
    INSERT INTO partner_settlements
        (partner_id, period_start, period_end,
        gross_amount, commission_amount, voucher_deduction_amount, payout_amount,
        settlement_status, settlement_date, note, created_at)
    VALUES
    (3,
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+33) DAY),
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+27) DAY),
    2400000, 360000, 0, 2040000,
    'PAID', DATE_SUB(NOW(), INTERVAL 24 DAY),
    'Tuần 5: TM Grand Hotel 1 booking — không có voucher.',
    DATE_SUB(NOW(), INTERVAL 24 DAY)),
    (7,
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+33) DAY),
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+27) DAY),
    7000000, 840000, 0, 6160000,
    'PAID', DATE_SUB(NOW(), INTERVAL 24 DAY),
    'Tuần 5: The Anam Villa 1 booking — không có voucher.',
    DATE_SUB(NOW(), INTERVAL 24 DAY));

    -- =============================================
    -- SEED DATA BỔ SUNG — BOOKINGS ACC2, ACC5, ACC6
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 960000,
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 1240000,
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 4400000,
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 3200000,
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 640000,
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 960000,
        CONCAT('TXN-HLR-DLX2-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 80 DAY), 'HLR-DLX 2 đêm — hoàn tất');

    -- =============================================
    -- REVIEWS BỔ SUNG — bookings 23–28 (Tulip, Ba Na, Hoa Lư)
    -- =============================================

    -- Booking #23 → acc2 Tulip Hotel (5 sao, user2 Trần Thị Mai)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (5, 2, 23, 5,
    'Tulip Hotel 2 Dalat tuy 3 sao nhưng chất lượng vượt mong đợi! Phòng Standard sạch sẽ thoải mái, view đồi thông Đà Lạt buổi sáng rất đẹp. Nhân viên thân thiện, check-in nhanh chóng. Giá rất phải chăng cho vị trí trung tâm gần hồ Xuân Hương. Chắc chắn sẽ quay lại!',
    DATE_SUB(NOW(), INTERVAL 39 DAY));

    -- Booking #24 → acc2 Tulip Hotel (4 sao, user Nguyễn Văn An)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 2, 24, 4,
    'Khách sạn phong cách Châu Âu cổ điển rất duyên dáng. Phòng Superior có ban công nhìn hồ Xuân Hương tuyệt đẹp vào buổi sáng. Bữa sáng buffet ổn, WiFi ổn định. Giá xứng đáng với chất lượng, phù hợp cho cặp đôi.',
    DATE_SUB(NOW(), INTERVAL 46 DAY));

    -- Booking #25 → acc5 Ba Na Hills Forest Villa (5 sao, user3 Lê Văn Đức)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (6, 5, 25, 5,
    'Trải nghiệm đỉnh cao giữa rừng Bà Nà! Bungalow gỗ tự nhiên ấm áp, sàn kính ngắm rừng về đêm cực kỳ ảo diệu. Không khí trong lành, yên tĩnh tuyệt đối. Gần cáp treo và các điểm tham quan nổi tiếng. Đáng từng đồng tiền bỏ ra!',
    DATE_SUB(NOW(), INTERVAL 53 DAY));

    -- Booking #26 → acc5 Ba Na Hills Forest Villa (4 sao, user2 Trần Thị Mai)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (5, 5, 26, 4,
    'Villa giữa rừng Bà Nà rất thơ mộng và độc đáo. Sân hiên có bếp BBQ cùng nhóm bạn rất vui vẻ. Phòng hơi nhỏ hơn ảnh nhưng trang thiết bị đầy đủ và sạch sẽ. Nhân viên nhiệt tình, đồ ăn ngon. Sẽ giới thiệu cho bạn bè!',
    DATE_SUB(NOW(), INTERVAL 60 DAY));

    -- Booking #27 → acc6 Hoa Lư Riverside Homestay (5 sao, user Nguyễn Văn An)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 6, 27, 5,
    'Hoa Lư Riverside Homestay là viên ngọc ẩn của Hội An! Nhà cổ 3 gian mái ngói bên sông Thu Bồn, buổi sáng ăn bánh mì do chủ nhà tự làm ngon tuyệt. Được mượn xe đạp miễn phí đi phố cổ chỉ 5 phút. Chủ nhà hiếu khách và nhiệt tình tư vấn địa điểm!',
    DATE_SUB(NOW(), INTERVAL 67 DAY));

    -- Booking #28 → acc6 Hoa Lư Riverside Homestay (5 sao, user3 Lê Văn Đức)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (6, 6, 28, 5,
    'Homestay truyền thống đậm chất Hội An! Phòng Deluxe nhà tắm riêng sạch sẽ, cửa sổ nhìn vườn xanh mát. Bữa sáng phở và bánh mì tự làm siêu ngon. Vị trí đi bộ ra phố cổ 5 phút. Một trải nghiệm đáng nhớ khác biệt hoàn toàn với khách sạn thông thường!',
    DATE_SUB(NOW(), INTERVAL 74 DAY));

    -- Cập nhật rating + review_count cho acc2, acc5, acc6
    -- acc2 Tulip Hotel: 2 reviews (23=5★, 24=4★) → avg 4.5 × 2 = 9.0
    UPDATE accommodations SET rating = 9.0, review_count = 2 WHERE id = 2;
    -- acc5 Ba Na Hills Villa: 2 reviews (25=5★, 26=4★) → avg 4.5 × 2 = 9.0
    UPDATE accommodations SET rating = 9.0, review_count = 2 WHERE id = 5;
    -- acc6 Hoa Lư Homestay: 2 reviews (27=5★, 28=5★) → avg 5.0 × 2 = 10.0
    UPDATE accommodations SET rating = 10.0, review_count = 2 WHERE id = 6;

    -- =============================================
    -- SETTLEMENTS BỔ SUNG — Tuần 6–11, đúng partner_id theo loại lưu trú
    -- HOTEL 15% | VILLA 12% | HOMESTAY 10% | RESORT 18%
    -- partner1 (id=3)=HOTEL | partner2 (id=4)=RESORT
    -- partner3 (id=7)=VILLA | partner4 (id=8)=HOMESTAY
    -- =============================================

    -- ─── Tuần n=6 (Mar 16–22) — PAID ─────────────────────────────
    -- partner1 (HOTEL): booking #23 TLP-STD (user2), payment=960.000, comm15%=144.000, payout=816.000
    -- partner1 (HOTEL): booking #24 TLP-SUP (user), payment=1.240.000, comm15%=186.000, payout=1.054.000
    -- Gộp 2 bookings Hotel tuần này: gross=2.200.000, comm=330.000, payout=1.870.000
    INSERT INTO partner_settlements
        (partner_id, period_start, period_end,
        gross_amount, commission_amount, voucher_deduction_amount, payout_amount,
        settlement_status, settlement_date, note, created_at)
    VALUES
    (3,
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+40) DAY),
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+34) DAY),
    2200000, 330000, 0, 1870000,
    'PAID', DATE_SUB(NOW(), INTERVAL 31 DAY),
    'Tuần 6: Tulip Hotel 2 bookings (TLP-STD + TLP-SUP) — không có voucher.',
    DATE_SUB(NOW(), INTERVAL 31 DAY));

    -- ─── Tuần n=7 (Mar 9–15) — PAID ──────────────────────────────
    -- partner3 (VILLA): booking #25 BNH-BNG (user3), payment=4.400.000, comm12%=528.000, payout=3.872.000
    -- partner3 (VILLA): booking #26 BNH-TWN (user2), payment=3.200.000, comm12%=384.000, payout=2.816.000
    -- Gộp 2 bookings Villa: gross=7.600.000, comm=912.000, payout=6.688.000
    INSERT INTO partner_settlements
        (partner_id, period_start, period_end,
        gross_amount, commission_amount, voucher_deduction_amount, payout_amount,
        settlement_status, settlement_date, note, created_at)
    VALUES
    (7,
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+47) DAY),
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+41) DAY),
    7600000, 912000, 0, 6688000,
    'PAID', DATE_SUB(NOW(), INTERVAL 38 DAY),
    'Tuần 7: Ba Na Hills Forest Villa 2 bookings — không có voucher.',
    DATE_SUB(NOW(), INTERVAL 38 DAY));

    -- ─── Tuần n=8 (Mar 2–8) — PAID ────────────────────────────────
    -- partner4 (HOMESTAY): booking #27 HLR-STD, payment=640.000, comm10%=64.000, payout=576.000
    -- partner4 (HOMESTAY): booking #28 HLR-DLX, payment=960.000, comm10%=96.000, payout=864.000
    -- Gộp 2 bookings Homestay: gross=1.600.000, comm=160.000, payout=1.440.000
    INSERT INTO partner_settlements
        (partner_id, period_start, period_end,
        gross_amount, commission_amount, voucher_deduction_amount, payout_amount,
        settlement_status, settlement_date, note, created_at)
    VALUES
    (8,
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+54) DAY),
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+48) DAY),
    1600000, 160000, 0, 1440000,
    'PAID', DATE_SUB(NOW(), INTERVAL 45 DAY),
    'Tuần 8: Hoa Lư Riverside Homestay 2 bookings (HLR-STD + HLR-DLX) — không có voucher.',
    DATE_SUB(NOW(), INTERVAL 45 DAY));

    -- ─── Tuần n=9 (Feb 23–Mar 1) — PAID ──────────────────────────
    -- partner1 (HOTEL): không có booking tuần này
    -- partner2 (RESORT): không có booking tuần này
    -- Thêm pending tuần hiện tại cho tất cả 4 partner để demo "Chờ thanh toán"
    -- ─── Tuần n=1 (tuần hiện tại) — PENDING cho 4 partner ─────────
    -- Quy tắc: commission chỉ tính trên tiền thu online (payment.amount), không dùng booking.totalAmount.
    INSERT INTO partner_settlements
        (partner_id, period_start, period_end,
        gross_amount, commission_amount, voucher_deduction_amount, payout_amount,
        settlement_status, settlement_date, note, created_at)
    VALUES
    -- partner1 HOTEL (acc1 LATA, acc2 Tulip, acc3 TM Grand):
    --   booking #4 LATA-DLX  FULL_PAYMENT 1.700.000 → comm 15%=255.000
    --   booking #6 TMG-PRE   FULL_PAYMENT 4.950.000 → comm 18% (room override)=891.000
    --   no-show  #5 TLP-STD  DEPOSIT_30   cọc 288.000 → comm 15%=43.200
    -- Ghi chú: booking #12 FDN-DLX thuộc acc9 Furama (partner2), KHÔNG thuộc partner1.
    -- Gross=6.938.000  Commission=1.189.200  Payout=5.748.800
    (3,
    DATE_SUB(CURDATE(), INTERVAL 7 DAY), CURDATE(),
    6938000, 1189200, 0, 5748800,
    'PENDING', NULL,
    'Tuần hiện tại: LATA-DLX 1.700.000 (comm15%=255.000) + TMG-PRE 4.950.000 (comm18% override=891.000) + No-show TLP-STD cọc 288.000 (comm15%=43.200). Admin chuyển khoản Thứ Ba tới.',
    NOW()),
    -- partner2 RESORT (acc8 Vinpearl, acc9 Furama):
    --   booking #8  VNT-DLX  DEPOSIT_30 cọc online 2.520.000 → comm 18%=453.600
    --   no-show #12 FDN-DLX  DEPOSIT_30 cọc  1.440.000 → comm 18%=259.200
    -- Gross=3.960.000  Commission=712.800  Payout=3.247.200
    (4,
    DATE_SUB(CURDATE(), INTERVAL 7 DAY), CURDATE(),
    3960000, 712800, 0, 3247200,
    'PENDING', NULL,
    'Tuần hiện tại: Vinpearl VNT-DLX cọc 30% online 2.520.000 (comm18%=453.600) + No-show Furama FDN-DLX cọc 1.440.000 (comm18%=259.200). Admin chuyển khoản Thứ Ba tới.',
    NOW()),
    -- partner3 VILLA (acc4 Anam, acc5 Ba Na Hills):
    --   booking #14 BNH-BNG  FULL_PAYMENT 6.600.000 → comm 12%=792.000
    --   booking #33 ANM-GDN-0003 FULL_PAYMENT 10.500.000 → comm 12%=1.260.000
    -- Gross=17.100.000  Commission=2.052.000  Payout=15.048.000
    (7,
    DATE_SUB(CURDATE(), INTERVAL 7 DAY), CURDATE(),
    17100000, 2052000, 0, 15048000,
    'PENDING', NULL,
    'Tuần hiện tại: Ba Na Hills BNH-BNG 6.600.000 (comm12%=792.000) + Anam Garden ANM-GDN-0003 10.500.000 (comm12%=1.260.000). Admin chuyển khoản Thứ Ba tới.',
    NOW()),
    -- partner4 HOMESTAY (acc6 Hoa Lư, acc7 Mộc Nhiên):
    --   booking #13 HLR-DLX     FULL_PAYMENT 960.000   → comm 10%=96.000
    --   booking #34 MND-ATT-0002 FULL_PAYMENT 1.160.000 → comm 12% (room override)=139.200
    -- Gross=2.120.000  Commission=235.200  Payout=1.884.800
    (8,
    DATE_SUB(CURDATE(), INTERVAL 7 DAY), CURDATE(),
    2120000, 235200, 0, 1884800,
    'PENDING', NULL,
    'Tuần hiện tại: Hoa Lư HLR-DLX 960.000 (comm10%=96.000) + Mộc Nhiên MND-ATT-0002 1.160.000 (comm12% override=139.200). Admin chuyển khoản Thứ Ba tới.',
    NOW());

    -- =============================================
    -- KIỂM TRA SAU KHI CHẠY SQL (cập nhật)
    -- =============================================
    -- SELECT COUNT(*) FROM users;               -- kỳ vọng: 6 (1 admin, 3 user, 2 partner)
    -- SELECT COUNT(*) FROM bookings;            -- kỳ vọng: 28
    -- SELECT COUNT(*) FROM payments;            -- kỳ vọng: 28
    -- SELECT COUNT(*) FROM reviews;             -- kỳ vọng: 16
    -- SELECT COUNT(*) FROM partner_settlements; -- kỳ vọng: 17 (10 cũ + 6 mới + 1 pending)
    -- SELECT COUNT(*) FROM vouchers;            -- kỳ vọng: 5
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
    -- Kỳ vọng: partner1 tuần 3 (510K), partner2 tuần 4 (100K)

    -- =============================================
    -- SEED DATA BỔ SUNG — DIRECT BOOKING & MANUAL_BLOCK
    -- Mục đích: Demo phân biệt booking_source trực quan
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
        'BK-TLP-FAM-DIRECT-01', 2, 2, 7,
        DATE_ADD(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 4 DAY),
        3, 1, 1,
        'Phạm Văn Khoa', '0909 123 456', NULL,
        2100000, 2100000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'DIRECT', NULL,
        'Khách walk-in đặt trực tiếp tại quầy lễ tân — thanh toán tiền mặt. Không qua TravelMate, không tính commission.',
        NOW(), NOW()
    );

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
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
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
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'DIRECT', NULL,
        'Đoàn doanh nghiệp 4 đêm — ký hợp đồng trực tiếp với Vinpearl Resort. Không qua TravelMate, không tính commission.',
        DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
    );
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 27;

    -- ─── MANUAL_BLOCK 1: LATA-SUI (r4 đã FULL, dùng LATA-STD r1 cho demo) ──────
    -- Thực ra LATA-SUI đã full nên block thêm phòng LATA-STD demo bảo trì
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
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
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
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
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
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED', 'MANUAL_BLOCK', 'Nâng cấp nội thất phòng Beachfront Superior: lắp đặt máy chiếu, thay sofa mới, sơn lại phòng tắm.',
        'Chặn phòng 7 ngày để thi công nâng cấp. Dự kiến hoàn thành và mở bán lại.',
        NOW(), NOW()
    );

    -- Cập nhật note về bookings DIRECT & MANUAL_BLOCK để giảng viên dễ nhận ra:
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
    -- UPDATE rooms SET room_category = 'VIP',   commission_rate_override = 18.00
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
    -- SEED DATA BỔ SUNG — HOÀN CHỈNH CHO 4 PARTNER
    -- Mục đích: Demo đầy đủ tất cả chức năng cho GV
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 17400000,
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 6650000,
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 1500000,
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 1710000,
        CONCAT('TXN-MND-FAM1-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 42 DAY), 'MND-FAM 2 đêm với HOALUU50K — hoàn tất');

    -- ─── Booking 33: CONFIRMED — Anam Villa ANM-GDN (partner3), chờ xác nhận ───
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
        'PENDING_PARTNER_CONFIRMATION', NULL, NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 10500000,
        CONCAT('TXN-ANM-GDN3-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'ANM-GDN 3 đêm — partner3 chờ xác nhận');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 13;

    -- ─── Booking 34: CONFIRMED — Mộc Nhiên MND-ATT (partner4), chờ xác nhận ───
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
        'PENDING_PARTNER_CONFIRMATION', NULL, NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 1160000,
        CONCAT('TXN-MND-ATT2-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'MND-ATT 2 đêm — partner4 chờ xác nhận');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 10800000,
        CONCAT('TXN-FDN-BCH1-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 4 DAY), 'FDN-BCH 3 đêm — đã check-in');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 29;

    -- =============================================
    -- DEMO: BOOKING TRỰC TIẾP (DIRECT) & CHẶN PHÒNG (MANUAL_BLOCK)
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
        'CHECKED_IN', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
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
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'DIRECT', 'NOT_REQUIRED',
        'Khách đặt qua điện thoại trực tiếp với resort, check-in ngày mai.',
        DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR)
    );
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 25;

    -- ─── Direct Booking 3: partner4 tạo booking trực tiếp tại Mộc Nhiên Homestay (MND-STD, room_id=22) ─
    -- Khách đặt trước qua điện thoại, thanh toán tiền mặt toàn bộ khi nhận phòng.
    -- DIRECT: payment_option=FULL_PAYMENT, payment_status=APPROVED (ghi nhận xác nhận), remaining_payment_status=NOT_REQUIRED
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
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
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
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
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
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED', 'PARTNER_CONFIRMED',
        'MANUAL_BLOCK', 'Giữ phòng cho đoàn khách VIP của chủ villa, không mở bán 3 ngày cuối tuần', 'NOT_REQUIRED',
        NULL, NOW(), NOW()
    );
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 13;

    -- =============================================
    -- REVIEWS BỔ SUNG — bookings 29, 30, 31, 32
    -- =============================================

    -- Booking #29 → acc4 Anam Villa (5★, user2)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (5, 4, 29, 5,
    'Phòng Beachfront Pool Villa tại Anam là trải nghiệm không thể quên! Hồ bơi tràn ra biển, butler phục vụ 24/7, bữa sáng đặt tại phòng hoàn hảo. Không gian riêng tư tuyệt đối, thích hợp cho tuần trăng mật hoặc nghỉ dưỡng cao cấp.',
    DATE_SUB(NOW(), INTERVAL 15 DAY));

    -- Booking #30 → acc5 Ba Na Hills (4★, user3)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (6, 5, 30, 4,
    'Treetop Suite Bà Nà quả thật độc đáo — ban công 360° nhìn toàn rừng thông cực ảo. Dùng voucher ANAM15 được giảm tốt. Bồn tắm jacuzzi ngoài trời về đêm tuyệt vời. Chỉ hơi tiếc dịch vụ ăn uống tại chỗ còn ít lựa chọn.',
    DATE_SUB(NOW(), INTERVAL 28 DAY));

    -- Booking #31 → acc6 Hoa Lư (5★, user2)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (5, 6, 31, 5,
    'Phòng gia đình nhà Hoa Lư cực kỳ thoải mái cho cả nhà! Ban công nhìn sông Thu Bồn thơ mộng, trẻ con rất thích. Chủ nhà nhiệt tình dẫn đi phố cổ và chỉ hàng ăn ngon. Bánh mì tự làm buổi sáng ngon nhất Hội An!',
    DATE_SUB(NOW(), INTERVAL 21 DAY));

    -- Booking #32 → acc7 Mộc Nhiên (5★, user)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (2, 7, 32, 5,
    'Phòng Gia Đình Mộc Nhiên có sân thượng nhìn vườn dã quỳ cực lãng mạn! Dùng voucher HOALUU50K tiết kiệm được 50K. Lò sưởi củi buổi tối ấm áp, không khí Đà Lạt trong lành, bữa sáng thơm ngon. Sẽ quay lại mùa dã quỳ nở!',
    DATE_SUB(NOW(), INTERVAL 36 DAY));

    -- Cập nhật rating + review_count (tổng hợp cuối cùng)
    -- acc4 Anam Villa: reviews 22(5★)+29(5★) → avg 5.0 × 2 = 10.0
    UPDATE accommodations SET rating = 10.0, review_count = 2 WHERE id = 4;
    -- acc5 Ba Na Hills: reviews 25(5★)+26(4★)+30(4★) → avg 4.33 × 2 = 8.7
    UPDATE accommodations SET rating = 8.7, review_count = 3 WHERE id = 5;
    -- acc6 Hoa Lư: reviews 27(5★)+28(5★)+31(5★) → avg 5.0 × 2 = 10.0
    UPDATE accommodations SET rating = 10.0, review_count = 3 WHERE id = 6;
    -- acc7 Mộc Nhiên: reviews 10(4★)+18(4★)+32(5★) → avg 4.33 × 2 = 8.7
    UPDATE accommodations SET rating = 8.7, review_count = 3 WHERE id = 7;

    -- =============================================
    -- SETTLEMENTS BỔ SUNG — Partner3 và Partner4 thêm lịch sử
    -- =============================================

    -- Tuần n=2 (Apr 13-19) — partner3 VILLA: booking #29 ANM-BCH gross=17.400.000 comm15%(override)=2.610.000 payout=14.790.000
    INSERT INTO partner_settlements
        (partner_id, period_start, period_end,
        gross_amount, commission_amount, voucher_deduction_amount, payout_amount,
        settlement_status, settlement_date, note, created_at)
    VALUES
    (7,
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+12) DAY),
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+6) DAY),
    17400000, 2610000, 0, 14790000,
    'PAID', DATE_SUB(NOW(), INTERVAL 5 DAY),
    'Tuần 2: The Anam Beachfront Pool Villa 3 đêm — không voucher.',
    DATE_SUB(NOW(), INTERVAL 5 DAY));

    -- Tuần n=3 (Apr 6-12) — partner3 VILLA: booking #30 BNH-SUI với voucher ANAM15 (partner chịu)
    -- gross=7.600.000, comm16%(override)=1.216.000, voucher_deduction=1.050.000, payout=5.334.000
    INSERT INTO partner_settlements
        (partner_id, period_start, period_end,
        gross_amount, commission_amount, voucher_deduction_amount, payout_amount,
        settlement_status, settlement_date, note, created_at)
    VALUES
    (7,
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+19) DAY),
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+13) DAY),
    7600000, 1216000, 1050000, 5334000,
    'PAID', DATE_SUB(NOW(), INTERVAL 12 DAY),
    'Tuần 3: Ba Na Hills Treetop Suite — voucher ANAM15 do Partner chịu, trừ 1.050.000đ.',
    DATE_SUB(NOW(), INTERVAL 12 DAY));

    -- Tuần n=2 — partner4 HOMESTAY: booking #31 HLR-FAM gross=1.500.000 comm8%(override)=120.000 payout=1.380.000
    INSERT INTO partner_settlements
        (partner_id, period_start, period_end,
        gross_amount, commission_amount, voucher_deduction_amount, payout_amount,
        settlement_status, settlement_date, note, created_at)
    VALUES
    (8,
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+12) DAY),
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+6) DAY),
    1500000, 120000, 0, 1380000,
    'PAID', DATE_SUB(NOW(), INTERVAL 5 DAY),
    'Tuần 2: Hoa Lư Family Room 2 đêm — không voucher.',
    DATE_SUB(NOW(), INTERVAL 5 DAY));

    -- Tuần n=4 — partner4 HOMESTAY: booking #32 MND-FAM với HOALUU50K (partner chịu)
    -- gross=1.760.000, comm10%=176.000, voucher_deduction=50.000, payout=1.534.000
    INSERT INTO partner_settlements
        (partner_id, period_start, period_end,
        gross_amount, commission_amount, voucher_deduction_amount, payout_amount,
        settlement_status, settlement_date, note, created_at)
    VALUES
    (8,
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+26) DAY),
    DATE_SUB(CURDATE(), INTERVAL (DAYOFWEEK(CURDATE())+20) DAY),
    1760000, 176000, 50000, 1534000,
    'PAID', DATE_SUB(NOW(), INTERVAL 19 DAY),
    'Tuần 4: Mộc Nhiên Family Room — voucher HOALUU50K do Partner chịu, trừ 50.000đ.',
    DATE_SUB(NOW(), INTERVAL 19 DAY));

    -- NOTE: Các settlement PENDING tuần hiện tại đã được gộp vào block phía trên (line ~1731)
    -- Không insert lại ở đây để tránh lỗi Duplicate Entry (UNIQUE KEY uk_settlement_partner_period)

    -- =============================================
    -- TIỆN NGHI PHÒNG (AMENITIES) — SEED DATA
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
    -- ROOM_AMENITIES SEED DATA
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

    -- =============================================
    -- KIỂM TRA SAU KHI IMPORT (kỳ vọng cuối cùng)
    -- =============================================
    -- SELECT COUNT(*) FROM users;               -- kỳ vọng: 8 (1 admin, 3 user, 4 partner)
    -- SELECT COUNT(*) FROM accommodations;      -- kỳ vọng: 11 APPROVED + 2 (PENDING+REJECTED) = 13
    -- SELECT COUNT(*) FROM rooms;               -- kỳ vọng: 30
    -- SELECT COUNT(*) FROM bookings;            -- kỳ vọng: 35
    -- SELECT COUNT(*) FROM payments;            -- kỳ vọng: 35
    -- SELECT COUNT(*) FROM reviews;             -- kỳ vọng: 20
    -- SELECT COUNT(*) FROM vouchers;            -- kỳ vọng: 8
    -- SELECT COUNT(*) FROM amenities;           -- kỳ vọng: 32
    -- SELECT COUNT(*) FROM room_amenities;      -- kỳ vọng: ~230
    -- SELECT COUNT(*) FROM partner_settlements; -- kỳ vọng: 26
    -- SELECT COUNT(*) FROM admin_action_logs;    -- kỳ vọng: 50 (phủ đủ 8 loại: BOOKING, ACCOMMODATION, ROOM, USER, REVIEW, VOUCHER, SETTLEMENT, TICKET)
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

    -- ─── SEED: Demo audit log entries — đủ loại action cho mọi filter tab ────────────────
    INSERT INTO admin_action_logs (admin_email, action_type, target_type, target_id, description, note, created_at) VALUES

    -- ── BOOKING actions ───────────────────────────────────────────────────────────
    ('admin@travelmate.vn', 'APPROVE_BOOKING',  'BOOKING',  7,  'Duyệt booking BK-LATA-FAM-0001 (Nguyễn Văn An — Phòng Gia Đình LATA)',                       NULL,                                                           DATE_SUB(NOW(), INTERVAL 12 DAY)),
    ('admin@travelmate.vn', 'COMPLETE_BOOKING', 'BOOKING',  7,  'Hoàn tất booking BK-LATA-FAM-0001 — Khách đã checkout',                                       NULL,                                                           DATE_SUB(NOW(), INTERVAL 7  DAY)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING',  'BOOKING',  3,  'Duyệt booking BK-TMG-DLX-0001 (Nguyễn Văn An — TM Grand Deluxe)',                             NULL,                                                           DATE_SUB(NOW(), INTERVAL 6  DAY)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING',  'BOOKING',  4,  'Duyệt booking BK-LATA-DLX-0001 (Nguyễn Văn An — LATA Deluxe)',                                NULL,                                                           DATE_SUB(NOW(), INTERVAL 6  DAY)),
    ('admin@travelmate.vn', 'NOSHOW_BOOKING',   'BOOKING',  5,  'No-show BK-TLP-STD-0001 — cọc giữ 288.000đ | hoa hồng HOTEL 15%=43.200đ | partner nhận 244.800đ',  'Khách đã xác nhận sẽ đến nhưng không xuất hiện đến hết ngày.',  DATE_SUB(NOW(), INTERVAL 5  DAY)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING',  'BOOKING',  6,  'Duyệt booking BK-TMG-PRE-0001 (Nguyễn Văn An — TM Grand Premium)',                            NULL,                                                           DATE_SUB(NOW(), INTERVAL 7  DAY)),
    ('admin@travelmate.vn', 'CHECKIN_BOOKING',  'BOOKING',  6,  'Check-in booking BK-TMG-PRE-0001 — Khách đã vào phòng',                                       NULL,                                                           DATE_SUB(NOW(), INTERVAL 1  DAY)),
    ('admin@travelmate.vn', 'REJECT_BOOKING',   'BOOKING',  2,  'Từ chối booking BK-TLP-SUP-0001 — Ngày đặt không khả dụng',                                   'Phòng đã được đặt trong khoảng thời gian này qua kênh trực tiếp.',DATE_SUB(NOW(), INTERVAL 8  DAY)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING',  'BOOKING',  8,  'Duyệt booking BK-VNT-DLX-0001 (Trần Thị Bích — Vinpearl Deluxe Ocean)',                       NULL,                                                           DATE_SUB(NOW(), INTERVAL 4  DAY)),
    ('admin@travelmate.vn', 'COMPLETE_BOOKING', 'BOOKING',  10, 'Hoàn tất booking BK-MND-ATT-0001 — Phạm Quỳnh Anh đã checkout Homestay Mộc Nhiên',            NULL,                                                           DATE_SUB(NOW(), INTERVAL 5  DAY)),
    ('admin@travelmate.vn', 'NOSHOW_BOOKING',   'BOOKING',  12, 'No-show BK-FDN-DLX-0001 — cọc giữ 1.440.000đ | hoa hồng RESORT 18%=259.200đ | partner nhận 1.180.800đ', 'Ngô Thị Lan không đến Furama Resort. Cọc đã phân bổ theo tỷ lệ RESORT.',   DATE_SUB(NOW(), INTERVAL 6  DAY)),

    ('admin@travelmate.vn', 'APPROVE_BOOKING',  'BOOKING',  15, 'Duyet booking BK-TLP-SUP-0002 (Nguyen Van An - Tulip Hotel Superior)', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),

    -- ACCOMMODATION actions
    ('admin@travelmate.vn', 'APPROVE_LISTING',  'ACCOMMODATION', 1,  'Duyệt listing: LATA Hotel & Apartments — 4★ Đà Lạt (partner@travelmate.vn)',              NULL,                                                           DATE_SUB(NOW(), INTERVAL 30 DAY)),
    ('admin@travelmate.vn', 'APPROVE_LISTING',  'ACCOMMODATION', 2,  'Duyệt listing: Tulip Hotel 2 Dalat — 3★ Đà Lạt (partner@travelmate.vn)',                  NULL,                                                           DATE_SUB(NOW(), INTERVAL 30 DAY)),
    ('admin@travelmate.vn', 'APPROVE_LISTING',  'ACCOMMODATION', 3,  'Duyệt listing: TravelMate Grand Hotel — 5★ Đà Lạt (partner@travelmate.vn)',               NULL,                                                           DATE_SUB(NOW(), INTERVAL 30 DAY)),
    ('admin@travelmate.vn', 'APPROVE_LISTING',  'ACCOMMODATION', 4,  'Duyệt listing: The Anam Villa Nha Trang — 5★ (partner3@travelmate.vn)',                   NULL,                                                           DATE_SUB(NOW(), INTERVAL 28 DAY)),
    ('admin@travelmate.vn', 'APPROVE_LISTING',  'ACCOMMODATION', 8,  'Duyệt listing: Vinpearl Resort & Spa Nha Trang — 5★ (partner2@travelmate.vn)',            NULL,                                                           DATE_SUB(NOW(), INTERVAL 25 DAY)),
    ('admin@travelmate.vn', 'REJECT_LISTING',   'ACCOMMODATION', 11, 'Từ chối listing: Da Lat Fake Hotel — thông tin không hợp lệ',                             'Thiếu ảnh thumbnail, địa chỉ không rõ ràng, không có giấy phép kinh doanh.', DATE_SUB(NOW(), INTERVAL 2 DAY)),
    ('admin@travelmate.vn', 'HIDE_LISTING',     'ACCOMMODATION', 11, 'Ẩn listing: Da Lat Fake Hotel khỏi kết quả tìm kiếm',                                    'Listing bị từ chối — tạm ẩn để partner cập nhật lại thông tin.', DATE_SUB(NOW(), INTERVAL 2 DAY)),

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
    ('admin@travelmate.vn', 'APPROVE_REVIEW',   'REVIEW', 1,  'Duyệt đánh giá #1 — Nguyễn Văn An cho LATA Hotel (5★)',                                        NULL,                                                           DATE_SUB(NOW(), INTERVAL 7  DAY)),
    ('admin@travelmate.vn', 'HIDE_REVIEW',      'REVIEW', 2,  'Ẩn đánh giá #2 — Nội dung không phù hợp, có chứa thông tin cá nhân của nhân viên',             'Review vi phạm quy định: không được tiết lộ thông tin nhân viên.', DATE_SUB(NOW(), INTERVAL 4 DAY)),
    ('admin@travelmate.vn', 'APPROVE_REVIEW',   'REVIEW', 5,  'Duyệt đánh giá #5 — Lê Văn Đức cho Ba Na Hills Forest Villa (5★)',                             NULL,                                                           DATE_SUB(NOW(), INTERVAL 14 DAY)),

    -- ── VOUCHER actions ───────────────────────────────────────────────────────────
    ('admin@travelmate.vn', 'CREATE_VOUCHER',   'VOUCHER', NULL, 'Tạo voucher SUMMER10 — giảm 10% toàn sàn (Admin chịu), hiệu lực 01/05–31/05/2026',          'Chương trình khuyến mãi hè 2026 dành cho toàn bộ khách hàng.',  DATE_SUB(NOW(), INTERVAL 35 DAY)),
    ('admin@travelmate.vn', 'CREATE_VOUCHER',   'VOUCHER', NULL, 'Tạo voucher WELCOME50K — giảm 50.000đ cho đơn đầu tiên (Admin chịu)',                         'Voucher chào mừng khách hàng mới đăng ký tài khoản.',           DATE_SUB(NOW(), INTERVAL 30 DAY)),
    ('admin@travelmate.vn', 'TOGGLE_VOUCHER',   'VOUCHER', NULL, 'Tắt voucher WELCOME50K — hết ngân sách chiến dịch chào mừng',                                 'Đã phát 200 voucher, tắt để tránh vượt ngân sách Marketing.',   DATE_SUB(NOW(), INTERVAL 5  DAY)),
    ('admin@travelmate.vn', 'CREATE_VOUCHER',   'VOUCHER', NULL, 'Tạo voucher LATA20 — giảm 20% tại LATA Hotel (Partner chịu, scope: ACCOMMODATION)',           NULL,                                                           DATE_SUB(NOW(), INTERVAL 20 DAY)),
    ('admin@travelmate.vn', 'TOGGLE_VOUCHER',   'VOUCHER', NULL, 'Bật lại voucher SUMMER10 — gia hạn thêm 1 tháng theo yêu cầu Marketing',                     'Voucher được gia hạn đến 30/06/2026 theo kế hoạch hè 2.',       DATE_SUB(NOW(), INTERVAL 2  DAY)),

    -- ── SETTLEMENT actions ────────────────────────────────────────────────────────
    ('admin@travelmate.vn', 'GENERATE_SETTLEMENT', 'SETTLEMENT', NULL, 'Tạo quyết toán tuần (Apr 13–19): partner HOTEL 994.500đ, RESORT 4.592.000đ',           'Tự động tính từ bookings COMPLETED trong kỳ.',                  DATE_SUB(NOW(), INTERVAL 17 DAY)),
    ('admin@travelmate.vn', 'MARK_SETTLEMENT_PAID','SETTLEMENT', NULL, 'Thanh toán quyết toán tuần (Apr 13–19) cho partner HOTEL (Sunrise Sapa Lodge): 994.500đ','Chuyển khoản MB Bank 0123456789, tham chiếu STL-HOTEL-W2.',     DATE_SUB(NOW(), INTERVAL 17 DAY)),
    ('admin@travelmate.vn', 'MARK_SETTLEMENT_PAID','SETTLEMENT', NULL, 'Thanh toán quyết toán tuần (Apr 13–19) cho partner RESORT (Blue Ocean): 4.592.000đ',   'Chuyển khoản Vietcombank 9876543210, tham chiếu STL-RESORT-W2.', DATE_SUB(NOW(), INTERVAL 17 DAY)),
    ('admin@travelmate.vn', 'GENERATE_SETTLEMENT', 'SETTLEMENT', NULL, 'Tạo quyết toán tuần hiện tại: 4 partner, tổng payout 30.497.000đ',                     'PENDING — Admin sẽ chuyển khoản vào thứ Ba tuần tới.',           NOW()),

    -- ── SUPPORT_TICKET actions ────────────────────────────────────────────────────
    ('admin@travelmate.vn', 'RESPOND_TICKET',   'TICKET',  1,  'Trả lời ticket #1 — partner@travelmate.vn: "Quyết toán tuần 3 bị sai số tiền"',               'Xác nhận con số đúng, giải thích công thức gross - comm - deduction.', DATE_SUB(NOW(), INTERVAL 7 DAY)),
    ('admin@travelmate.vn', 'RESPOND_TICKET',   'TICKET',  2,  'Trả lời ticket #2 — partner@travelmate.vn: "Không thể đánh dấu No-Show"',                     'Đánh dấu BK-TLP-STD-0001 là NO_SHOW thay partner, giải thích quy trình.', DATE_SUB(NOW(), INTERVAL 4 DAY)),
    ('admin@travelmate.vn', 'CLOSE_TICKET',     'TICKET',  1,  'Đóng ticket #1 — Đã giải quyết xong vấn đề quyết toán tuần 3',                               NULL,                                                           DATE_SUB(NOW(), INTERVAL 6 DAY)),
    ('admin@travelmate.vn', 'RESPOND_TICKET',   'TICKET',  5,  'Trả lời ticket #5 — user@travelmate.vn: "Không nhận được email xác nhận đặt phòng"',          'Đã kiểm tra log email, resend thủ công. Hướng dẫn check spam.', DATE_SUB(NOW(), INTERVAL 3 DAY));

    -- ── ADMIN ACTION LOGS BỔ SUNG — Booking, Listing, Settlement cho partner3 & partner4 ──
    INSERT INTO admin_action_logs (admin_email, action_type, target_type, target_id, description, note, created_at) VALUES

    -- Duyệt thêm bookings (bk3, 9, 11, 13, 14 — ban đầu chưa log)
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', 9,  'Duyệt booking BK-VNT-SUI-0001 (Lê Minh Đức — Vinpearl Junior Suite)', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', 11, 'Duyệt booking BK-ANM-GDN-0001 (Hoàng Văn Hùng — Anam Garden Villa, cọc 30%)', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
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
    ('admin@travelmate.vn', 'APPROVE_REVIEW', 'REVIEW', 3,  'Duyệt đánh giá #3 — Trần Thị Mai cho The Anam Villa (5★ Beachfront Pool Villa)', NULL, DATE_SUB(NOW(), INTERVAL 15 DAY)),
    ('admin@travelmate.vn', 'APPROVE_REVIEW', 'REVIEW', 4,  'Duyệt đánh giá #4 — Lê Văn Đức cho Ba Na Hills Treetop Suite (4★)', NULL, DATE_SUB(NOW(), INTERVAL 28 DAY)),
    ('admin@travelmate.vn', 'APPROVE_REVIEW', 'REVIEW', 6,  'Duyệt đánh giá #6 — Trần Thị Mai cho Hoa Lư Family Room (5★)', NULL, DATE_SUB(NOW(), INTERVAL 21 DAY)),
    ('admin@travelmate.vn', 'APPROVE_REVIEW', 'REVIEW', 7,  'Duyệt đánh giá #7 — Nguyễn Văn An cho Mộc Nhiên Family Room (5★ — voucher HOALUU50K)', NULL, DATE_SUB(NOW(), INTERVAL 36 DAY)),
    ('admin@travelmate.vn', 'APPROVE_REVIEW', 'REVIEW', 8,  'Duyệt đánh giá #8 — Nguyễn Văn An cho Hoa Lư Riverside Homestay (5★)', NULL, DATE_SUB(NOW(), INTERVAL 67 DAY)),
    ('admin@travelmate.vn', 'APPROVE_REVIEW', 'REVIEW', 9,  'Duyệt đánh giá #9 — Nguyễn Văn An cho Furama Resort Đà Nẵng (5★)', NULL, DATE_SUB(NOW(), INTERVAL 22 DAY)),
    ('admin@travelmate.vn', 'APPROVE_REVIEW', 'REVIEW', 10, 'Duyệt đánh giá #10 — Trần Thị Mai cho Vinpearl Resort & Spa (4★)', NULL, DATE_SUB(NOW(), INTERVAL 26 DAY)),

    -- Quyết toán thêm cho partner3 & partner4 (log GENERATE + PAID)
    ('admin@travelmate.vn', 'GENERATE_SETTLEMENT', 'SETTLEMENT', NULL, 'Tạo quyết toán tuần (Mar 9–15): partner VILLA (Ba Na) 6.688.000đ', 'Tự động từ 2 bookings Ba Na Hills BNH-BNG & BNH-TWN COMPLETED.', DATE_SUB(NOW(), INTERVAL 38 DAY)),
    ('admin@travelmate.vn', 'MARK_SETTLEMENT_PAID', 'SETTLEMENT', NULL, 'Thanh toán quyết toán tuần (Mar 9–15) cho partner3 VILLA (Bùi Thị Lan Anh): 6.688.000đ', 'Chuyển khoản Techcombank 1234567890, tham chiếu STL-VILLA-W7.', DATE_SUB(NOW(), INTERVAL 37 DAY)),
    ('admin@travelmate.vn', 'GENERATE_SETTLEMENT', 'SETTLEMENT', NULL, 'Tạo quyết toán tuần (Mar 2–8): partner HOMESTAY (Hoa Lư) 1.440.000đ', 'Tự động từ 2 bookings HLR-STD & HLR-DLX COMPLETED.', DATE_SUB(NOW(), INTERVAL 45 DAY)),
    ('admin@travelmate.vn', 'MARK_SETTLEMENT_PAID', 'SETTLEMENT', NULL, 'Thanh toán quyết toán tuần (Mar 2–8) cho partner4 HOMESTAY (Trần Văn Cường): 1.440.000đ', 'Chuyển khoản VPBank 0987654321, tham chiếu STL-HOMESTAY-W8.', DATE_SUB(NOW(), INTERVAL 44 DAY)),
    ('admin@travelmate.vn', 'GENERATE_SETTLEMENT', 'SETTLEMENT', NULL, 'Tạo quyết toán tuần (Apr 13–19): partner VILLA (Anam) 14.790.000đ, partner HOMESTAY (Hoa Lư) 1.380.000đ', 'Tự động tính từ bookings COMPLETED trong kỳ.', DATE_SUB(NOW(), INTERVAL 5 DAY)),
    ('admin@travelmate.vn', 'MARK_SETTLEMENT_PAID', 'SETTLEMENT', NULL, 'Thanh toán quyết toán tuần (Apr 13–19) cho partner3 VILLA (Bùi Thị Lan Anh): 14.790.000đ', 'Chuyển khoản Techcombank 1234567890, tham chiếu STL-VILLA-W2-2026.', DATE_SUB(NOW(), INTERVAL 4 DAY)),
    ('admin@travelmate.vn', 'MARK_SETTLEMENT_PAID', 'SETTLEMENT', NULL, 'Thanh toán quyết toán tuần (Apr 13–19) cho partner4 HOMESTAY (Trần Văn Cường): 1.380.000đ', 'Chuyển khoản VPBank 0987654321, tham chiếu STL-HOMESTAY-W2-2026.', DATE_SUB(NOW(), INTERVAL 4 DAY)),

    -- Thêm ticket support & phản hồi cho partner3, partner4
    ('admin@travelmate.vn', 'RESPOND_TICKET', 'TICKET', 3, 'Trả lời ticket #3 — partner3@travelmate.vn: "Voucher ANAM15 bị tính sai phần trăm"', 'Xác nhận: ANAM15 giảm 15% trên tổng đơn, tối đa 1.200.000đ. Settlement đã tính đúng.', DATE_SUB(NOW(), INTERVAL 11 DAY)),
    ('admin@travelmate.vn', 'CLOSE_TICKET',   'TICKET', 3, 'Đóng ticket #3 — Đã giải thích rõ công thức tính voucher PARTNER_ACCOMMODATION', NULL, DATE_SUB(NOW(), INTERVAL 10 DAY)),
    ('admin@travelmate.vn', 'RESPOND_TICKET', 'TICKET', 4, 'Trả lời ticket #4 — partner4@travelmate.vn: "Phòng MND-FAM hiển thị sai giá trên trang khách"', 'Đã kiểm tra: giá sau voucher HOALUU50K = 1.710.000đ hiển thị đúng. Hướng dẫn xem lịch sử booking.', DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ('admin@travelmate.vn', 'CLOSE_TICKET',   'TICKET', 4, 'Đóng ticket #4 — Đã xác nhận hiển thị giá chính xác', NULL, DATE_SUB(NOW(), INTERVAL 7 DAY)),

    -- Voucher partner3 và partner4
    ('admin@travelmate.vn', 'CREATE_VOUCHER', 'VOUCHER', NULL, 'Tạo voucher ANAM15 — giảm 15% tại The Anam Villa Nha Trang (Partner3 chịu, scope: ACCOMMODATION)', NULL, DATE_SUB(NOW(), INTERVAL 25 DAY)),
    ('admin@travelmate.vn', 'CREATE_VOUCHER', 'VOUCHER', NULL, 'Tạo voucher HOALUU50K — giảm 50.000đ phòng MND-FAM Mộc Nhiên (Partner4 chịu, scope: ROOM)', NULL, DATE_SUB(NOW(), INTERVAL 22 DAY)),
    ('admin@travelmate.vn', 'CREATE_VOUCHER', 'VOUCHER', NULL, 'Tạo voucher VNT100K — giảm 100.000đ phòng VNT-DLX Vinpearl (Partner2 chịu, scope: ROOM)', NULL, DATE_SUB(NOW(), INTERVAL 20 DAY));

    -- Gán approved_at cho các payment đã được duyệt
    SET SQL_SAFE_UPDATES = 0;
    UPDATE payments SET approved_at = paid_at WHERE payment_status IN ('APPROVED', 'DEPOSIT_FORFEITED');
    SET SQL_SAFE_UPDATES = 1;

    -- =============================================
    -- DEMO KỊCH BẢN LISTING PAGE — HOTEL type
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
        'PARTNER_CONFIRMED', 'Demo listing FULL: nhóm 5 phòng STD dịp cuối tuần.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 1950000,
        CONCAT('TXN-LATA-STD-LST1-', UNIX_TIMESTAMP()), 'APPROVED', NOW(),
        'Cọc 30% LATA-STD 5 phòng — demo listing FULL');
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
        'PARTNER_CONFIRMED', 'Demo listing FULL: đoàn 4 phòng Deluxe.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 2040000,
        CONCAT('TXN-LATA-DLX-LST1-', UNIX_TIMESTAMP()), 'APPROVED', NOW(),
        'Cọc 30% LATA-DLX 4 phòng — demo listing FULL');
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
        'PARTNER_CONFIRMED', 'Demo listing FULL: 3 phòng Family đặt kín.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 2160000,
        CONCAT('TXN-LATA-FAM-LST1-', UNIX_TIMESTAMP()), 'APPROVED', NOW(),
        'Cọc 30% LATA-FAM 3 phòng — demo listing FULL');
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
        'PARTNER_CONFIRMED', 'Demo listing FULL: Suite cao cấp đặt kín.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 3600000,
        CONCAT('TXN-LATA-SUI-LST1-', UNIX_TIMESTAMP()), 'APPROVED', NOW(),
        '100% LATA-SUI — demo listing FULL');
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
        'PARTNER_CONFIRMED', 'Demo listing LIMITED: đoàn 6 phòng Deluxe Garden View.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 4320000,
        CONCAT('TXN-TMG-DLX-LST1-', UNIX_TIMESTAMP()), 'APPROVED', NOW(),
        'Cọc 30% TMG-DLX 6 phòng — demo listing LIMITED');
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
        'PARTNER_CONFIRMED', 'Demo listing LIMITED: đoàn 4 phòng Premium Valley View.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 3960000,
        CONCAT('TXN-TMG-PRE-LST1-', UNIX_TIMESTAMP()), 'APPROVED', NOW(),
        'Cọc 30% TMG-PRE 4 phòng — demo listing LIMITED');
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
        'PARTNER_CONFIRMED', 'Demo listing LIMITED: đặt kín 3 phòng Family Grand Suite.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 5040000,
        CONCAT('TXN-TMG-FAM-LST1-', UNIX_TIMESTAMP()), 'APPROVED', NOW(),
        'Cọc 30% TMG-FAM 3 phòng — demo listing LIMITED');
    UPDATE rooms SET available_quantity = available_quantity - 3 WHERE id = 11;
    -- r12 TMG-PRE2 (Presidential Suite, total=1): không thay đổi → 1 phòng còn trống
    -- Kết quả TM Grand tại +4→+6: r9=2, r10=1, r11=0, r12=1 → totalAvail=4, totalRooms=17 → LIMITED (24%) 🟡

    -- === Tulip Hotel 2 Dalat (acc2) ===
    -- Không thêm booking mới → totalAvail=16, totalRooms=16 → AVAILABLE (100%) 🟢

    -- =============================================
    -- KIỂM TRA KỲ VỌNG SAU IMPORT v7 (bookings #36-42 mới)
    -- SELECT COUNT(*) FROM bookings;  -- kỳ vọng: 42
    -- SELECT COUNT(*) FROM payments;  -- kỳ vọng: 42
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
        'PARTNER_CONFIRMED', 'Calendar demo +15→+18.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 5850000, CONCAT('TXN-CAL01-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'Cal demo +15→+18');
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
        'PARTNER_CONFIRMED', 'Calendar demo +18→+22.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 1020000, CONCAT('TXN-CAL02-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'Cal demo +18→+22');

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
        'PARTNER_CONFIRMED', 'Calendar demo TMG +20→+24.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 19200000, CONCAT('TXN-CAL03-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'Cal demo TMG +20→+24');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 56000000, CONCAT('TXN-CAL04-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'Cal Vinpearl +22→+26');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 10500000, CONCAT('TXN-CAL05-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'Cal Anam +25→+30');

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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 22000000, CONCAT('TXN-CAL06-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'Cal Ba Na +28→+33');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 1755000, CONCAT('TXN-CAL07-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'Cal Mộc Nhiên +30→+35');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 54000000, CONCAT('TXN-CAL08-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'Cal Furama BCH +33→+38');
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
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 2250000, CONCAT('TXN-CAL09-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), 'Cal Hoa Lư FAM +37→+42');
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
        49000000, 14700000, 34300000, 'PENDING_ADMIN_APPROVAL', 'DEPOSIT_30', 'PENDING_ADMIN_APPROVAL',
        NULL, 'Vinpearl Pool Villa tháng tới.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 14700000, CONCAT('TXN-CAL10-', UNIX_TIMESTAMP()), 'PENDING_ADMIN_APPROVAL', NOW(), 'Cal Vinpearl VIL +40→+45');

    -- =============================================
    -- DEMO LUỒNG PARTNER — CHECK-IN / CHECK-OUT / NO-SHOW
    -- Thêm các kịch bản trực quan để demo đúng nghiệp vụ Partner
    -- Partner thực hiện: Xác nhận giữ phòng → Check-in → Check-out → No-show
    -- =============================================

    -- ─── P1: Tulip Hotel — CONFIRMED + PARTNER_CONFIRMED — check-in HÔM NAY ────
    -- → partner@travelmate.vn vào Partner > Đơn đặt phòng → nhấn "Check-in khách"
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-TLP-FAM-DEMO1', 2, 2, 7,
        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 DAY),
        2, 1, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        2100000, 2100000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED',
        'Demo Partner check-in: khách đến HÔM NAY — Partner nhấn Check-in để xác nhận.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, approved_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 2100000,
        CONCAT('TXN-DEMO-P1-', UNIX_TIMESTAMP()), 'APPROVED', NOW(), NOW(),
        'Thanh toán 100% demo — Partner check-in hôm nay');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 7;

    -- ─── P2: Mộc Nhiên Homestay — CHECKED_IN + PARTNER_CONFIRMED — chờ CHECK-OUT ─
    -- → partner4@travelmate.vn vào Partner > Đơn đặt phòng → nhấn "Check-out / Hoàn tất"
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-MND-STD-DEMO1', 5, 7, 22,
        DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 1 DAY),
        2, 0, 1,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        780000, 780000, 0,
        'CHECKED_IN', 'FULL_PAYMENT', 'APPROVED',
        'PARTNER_CONFIRMED',
        '[Partner] Khách đã đến nhận phòng hôm qua. Check-in lúc 14:00 hôm qua. Demo: Partner nhấn Check-out hôm nay.',
        DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, approved_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 780000,
        CONCAT('TXN-DEMO-P2-', UNIX_TIMESTAMP()), 'APPROVED', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY),
        'Thanh toán 100% Mộc Nhiên — đang lưu trú, chờ check-out');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 22;

    -- ─── P3: Anam Villa — CONFIRMED + PARTNER_CONFIRMED — check-in NGÀY MAI — demo NO-SHOW ─
    -- → partner3@travelmate.vn: check-in ngày mai, nếu khách không đến → nhấn "Báo No-show"
    -- → Cọc 30% → khách sẽ mất cọc nếu no-show
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-ANM-GDN-DEMO1', 6, 4, 13,
        DATE_ADD(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 3 DAY),
        2, 0, 1,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        7000000, 2100000, 4900000,
        'CONFIRMED', 'DEPOSIT_30', 'APPROVED',
        'PARTNER_CONFIRMED',
        'Demo no-show: check-in ngày mai, cọc 30% = 2.100.000đ. Nếu khách không đến, Partner nhấn Báo No-show.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, approved_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'DEPOSIT_30', 2100000,
        CONCAT('TXN-DEMO-P3-', UNIX_TIMESTAMP()), 'APPROVED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 12 HOUR),
        'Cọc 30% Anam Garden Villa — chờ khách đến ngày mai');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 13;

    -- ─── P4: Vinpearl Resort — CONFIRMED + PENDING_PARTNER_CONFIRMATION ──────────
    -- → partner2@travelmate.vn cần XÁC NHẬN GIỮ PHÒNG trước khi khách đến
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status,
        partner_status, note, created_at, updated_at)
    VALUES (
        'BK-VNT-DLX-DEMO1', 2, 8, 25,
        DATE_ADD(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY),
        2, 0, 1,
        'Nguyễn Văn An', '0912 345 678', 'user@travelmate.vn',
        8400000, 8400000, 0,
        'CONFIRMED', 'FULL_PAYMENT', 'APPROVED',
        'PENDING_PARTNER_CONFIRMATION',
        'Demo xác nhận giữ phòng: Admin đã duyệt. Partner2 cần nhấn Xác nhận giữ phòng.', NOW(), NOW()
    );
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, approved_at, note)
    VALUES (LAST_INSERT_ID(), 'VNPAY_DEMO', 'FULL_PAYMENT', 8400000,
        CONCAT('TXN-DEMO-P4-', UNIX_TIMESTAMP()), 'APPROVED', DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR),
        'Thanh toán 100% Vinpearl Deluxe Ocean View — chờ partner xác nhận');
    UPDATE rooms SET available_quantity = available_quantity - 1 WHERE id = 25;

    -- Audit log cho các demo booking trên
    INSERT INTO admin_action_logs (admin_email, action_type, target_type, target_id, description, note, created_at) VALUES
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', NULL,
    'Duyệt booking BK-TLP-FAM-DEMO1 — Nguyễn Văn An check-in hôm nay (demo partner check-in)',
    'Đơn demo để giảng viên thấy luồng Partner check-in.', DATE_SUB(NOW(), INTERVAL 4 HOUR)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', NULL,
    'Duyệt booking BK-MND-STD-DEMO1 — Trần Thị Mai đã check-in hôm qua (demo partner check-out)',
    'Đơn demo để giảng viên thấy luồng Partner check-out.', DATE_SUB(NOW(), INTERVAL 25 HOUR)),
    ('partner4@travelmate.vn', 'PARTNER_CONFIRM_HOLD', 'BOOKING', NULL,
    'Partner xác nhận giữ phòng BK-MND-STD-DEMO1 — Mộc Nhiên Homestay', NULL, DATE_SUB(NOW(), INTERVAL 24 HOUR)),
    ('partner4@travelmate.vn', 'PARTNER_CHECK_IN', 'BOOKING', NULL,
    'Partner check-in khách BK-MND-STD-DEMO1 — Trần Thị Mai', 'Khách đến lúc 14:00.', DATE_SUB(NOW(), INTERVAL 23 HOUR)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', NULL,
    'Duyệt booking BK-ANM-GDN-DEMO1 — Lê Văn Đức check-in ngày mai (demo no-show)',
    'Đơn demo để giảng viên thấy luồng Partner báo no-show.', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
    ('admin@travelmate.vn', 'APPROVE_BOOKING', 'BOOKING', NULL,
    'Duyệt booking BK-VNT-DLX-DEMO1 — Nguyễn Văn An (demo partner confirm-hold)',
    'Đơn demo để giảng viên thấy luồng xác nhận giữ phòng.', DATE_SUB(NOW(), INTERVAL 2 HOUR));

    -- =============================================
    -- HỆ THỐNG ĐÁNH GIÁ TRAVELMATE — Dual Rating System
    -- =============================================
    -- star_rating (1–5★): Hạng / tiêu chuẩn cơ sở lưu trú
    --   → Áp dụng cho TẤT CẢ loại: HOTEL / VILLA / HOMESTAY / RESORT
    --   → Partner khai báo khi đăng ký listing, Admin xác minh khi duyệt
    --   → Không thay đổi theo review của khách — phản ánh cơ sở vật chất & dịch vụ
    --
    -- rating (0–10): Điểm hài lòng trung bình tính từ user reviews
    --   → Công thức: avg_review_stars × 2  (vd: avg 4.5★ → 9.0/10)
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
    --   7.0–7.9 (Tốt)     :  acc2  Tulip Hotel       3★ → 7.5/10   (4 reviews: 5+4+3+3 = avg 3.75★)
    --   8.0–8.9 (Rất tốt) :  acc5  Ba Na Hills Villa 4★ → 8.7/10   (3 reviews)
    --                         acc7  Mộc Nhiên HS      3★ → 8.0/10   (4 reviews: 4+4+5+3 = avg 4.0★)
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
    -- SEED DATA BỔ SUNG v8 — PHỔ ĐIỂM ĐÁ GIÁ (SCORE DIVERSITY)
    -- Thêm 3 booking COMPLETED + 3 review 3★ để kéo acc2 xuống 7.5
    -- và acc7 xuống 8.0 — tạo phổ điểm rõ ràng cho bộ lọc
    -- =============================================

    -- ─── Booking EXT01: Tulip TLP-FAM (r7, acc2, partner1), user3, 2 đêm ───
    -- Mục đích: thêm review 3★ để Tulip Hotel có điểm 7.5 (7+ tốt filter)
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
        'Tulip Family Room 2 đêm (seed score diversity).', DATE_SUB(NOW(), INTERVAL 89 DAY), DATE_SUB(NOW(), INTERVAL 89 DAY)
    );
    SET @ext01 = LAST_INSERT_ID();
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, approved_at, note)
    VALUES (@ext01, 'VNPAY_DEMO', 'FULL_PAYMENT', 2100000,
        CONCAT('TXN-TLP-FAM-EXT1-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 89 DAY), DATE_SUB(NOW(), INTERVAL 89 DAY), 'TLP-FAM 2 đêm — score diversity');

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
        'Tulip VIP Panorama 2 đêm (seed score diversity).', DATE_SUB(NOW(), INTERVAL 95 DAY), DATE_SUB(NOW(), INTERVAL 95 DAY)
    );
    SET @ext02 = LAST_INSERT_ID();
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, approved_at, note)
    VALUES (@ext02, 'VNPAY_DEMO', 'FULL_PAYMENT', 3000000,
        CONCAT('TXN-TLP-VIP-EXT1-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 95 DAY), DATE_SUB(NOW(), INTERVAL 95 DAY), 'TLP-VIP 2 đêm — score diversity');

    -- ─── Booking EXT03: Mộc Nhiên MND-STD (r22, acc7, partner4), user3, 2 đêm ───
    -- Mục đích: thêm review 3★ để Mộc Nhiên có điểm 8.0 (thay vì 8.7)
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
        'Mộc Nhiên Standard 2 đêm (seed score diversity).', DATE_SUB(NOW(), INTERVAL 86 DAY), DATE_SUB(NOW(), INTERVAL 86 DAY)
    );
    SET @ext03 = LAST_INSERT_ID();
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, approved_at, note)
    VALUES (@ext03, 'VNPAY_DEMO', 'FULL_PAYMENT', 780000,
        CONCAT('TXN-MND-STD-EXT1-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 86 DAY), DATE_SUB(NOW(), INTERVAL 86 DAY), 'MND-STD 2 đêm — score diversity');

    -- =============================================
    -- DEMO BOOKINGS v11 — DEPOSIT_30 + COMPLETED + PAID_AT_PROPERTY
    -- Mục đích: Minh hoạ luồng cọc 30% đầy đủ — khách lưu trú, trả 70% tại cơ sở.
    -- Quy tắc commission hiện hành: chỉ tính trên tiền thu online (cọc 30%).
    -- DEP01: LATA Suite  3 đêm → CK base = 1.620.000 (cọc online) | tổng đơn 5.400.000
    -- DEP02: Anam Beach  2 đêm → CK base = 3.480.000 (cọc online) | tổng đơn 11.600.000
    -- =============================================

    -- ─── DEP01: LATA Hotel — LATA-SUI (r4) — Lê Văn Đức — COMPLETED + DEPOSIT_30 ────────────
    INSERT INTO bookings (booking_code, user_id, accommodation_id, room_id,
        check_in, check_out, adults, children, room_quantity,
        customer_name, customer_phone, customer_email,
        total_amount, paid_amount, remaining_amount,
        booking_status, payment_option, payment_status, partner_status,
        remaining_payment_status, remaining_paid_at, remaining_payment_note,
        note, created_at, updated_at)
    VALUES (
        'BK-LATA-SUI-DEP01', 6, 1, 4,
        DATE_SUB(CURDATE(), INTERVAL 58 DAY), DATE_SUB(CURDATE(), INTERVAL 55 DAY),
        2, 0, 1,
        'Lê Văn Đức', '0934 567 890', 'user3@travelmate.vn',
        5400000, 1620000, 0,
        'COMPLETED', 'DEPOSIT_30', 'APPROVED', 'PARTNER_CONFIRMED',
        'PAID_AT_PROPERTY', DATE_SUB(NOW(), INTERVAL 55 DAY),
        'Khách thanh toán 70% còn lại (3.780.000đ) bằng tiền mặt tại quầy lễ tân khi check-out.',
        'Demo: DEPOSIT_30 COMPLETED + PAID_AT_PROPERTY — commission tính trên cọc online 1.620.000đ (comm 15%=243.000).',
        DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 55 DAY)
    );
    SET @dep01 = LAST_INSERT_ID();
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, approved_at, note)
    VALUES (@dep01, 'VNPAY_DEMO', 'DEPOSIT_30', 1620000,
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
        note, created_at, updated_at)
    VALUES (
        'BK-ANM-BCH-DEP01', 5, 4, 14,
        DATE_SUB(CURDATE(), INTERVAL 50 DAY), DATE_SUB(CURDATE(), INTERVAL 48 DAY),
        2, 0, 1,
        'Trần Thị Mai', '0923 456 789', 'user2@travelmate.vn',
        11600000, 3480000, 0,
        'COMPLETED', 'DEPOSIT_30', 'APPROVED', 'PARTNER_CONFIRMED',
        'PAID_AT_PROPERTY', DATE_SUB(NOW(), INTERVAL 48 DAY),
        'Khách thanh toán 70% còn lại (8.120.000đ) bằng thẻ tín dụng tại villa khi check-out.',
        'Demo: DEPOSIT_30 COMPLETED + PAID_AT_PROPERTY — commission tính trên cọc online 3.480.000đ (comm 12%=417.600).',
        DATE_SUB(NOW(), INTERVAL 52 DAY), DATE_SUB(NOW(), INTERVAL 48 DAY)
    );
    SET @dep02 = LAST_INSERT_ID();
    INSERT INTO payments (booking_id, payment_method, payment_option, amount, transaction_code, payment_status, paid_at, approved_at, note)
    VALUES (@dep02, 'VNPAY_DEMO', 'DEPOSIT_30', 3480000,
        CONCAT('TXN-ANM-BCH-DEP01-', UNIX_TIMESTAMP()), 'APPROVED',
        DATE_SUB(NOW(), INTERVAL 52 DAY), DATE_SUB(NOW(), INTERVAL 51 DAY),
        'Cọc 30% Anam Beachfront Villa — khách đã lưu trú, hoàn tất check-out');

    -- =============================================
    -- REVIEWS BỔ SUNG v8 — EXT01 / EXT02 / EXT03 (đánh giá 3★)
    -- =============================================

    -- EXT01 → acc2 Tulip Hotel (3★, user3 Lê Văn Đức)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (6, 2, @ext01, 3,
    'Khách sạn ổn nhưng chưa đáp ứng kỳ vọng. Phòng Family hơi tối và nhỏ hơn ảnh chụp. Bữa sáng đơn điệu, chủ yếu là bánh mì và trứng chiên. Vị trí gần hồ Xuân Hương là điểm cộng duy nhất đáng kể. Nhân viên thân thiện nhưng quy trình check-in khá chậm so với mức giá phải trả.',
    DATE_SUB(NOW(), INTERVAL 83 DAY));

    -- EXT02 → acc2 Tulip Hotel (3★, user2 Trần Thị Mai)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (5, 2, @ext02, 3,
    'Phòng VIP Panorama thực tế không đẹp như quảng cáo — ban công hẹp, view bị che bởi tòa nhà bên cạnh. WiFi yếu vào giờ cao điểm buổi tối. Điều hoà có tiếng ồn lớn, khó ngủ. Giá cả chưa tương xứng với chất lượng nhận được, cần cải thiện nhiều hơn.',
    DATE_SUB(NOW(), INTERVAL 89 DAY));

    -- EXT03 → acc7 Mộc Nhiên Homestay (3★, user3 Lê Văn Đức)
    INSERT INTO reviews (user_id, accommodation_id, booking_id, rating, comment, created_at) VALUES
    (6, 7, @ext03, 3,
    'Không gian nhà gỗ Đà Lạt đẹp mắt nhưng tiện nghi còn hạn chế. Nước nóng hay bị yếu, cửa sổ không kín gió nên lạnh vào ban đêm. Chủ nhà thân thiện nhưng ít có mặt để hỗ trợ. Bữa sáng ngon nhưng phần ăn nhỏ. Cần đầu tư thêm vào cơ sở vật chất để xứng với giá.',
    DATE_SUB(NOW(), INTERVAL 80 DAY));

    -- Cập nhật rating + review_count sau khi thêm 3 review 3★
    -- acc2 Tulip Hotel: 4 reviews (5★+4★+3★+3★) → avg 3.75 × 2 = 7.5
    UPDATE accommodations SET rating = 7.5, review_count = 4 WHERE id = 2;
    -- acc7 Mộc Nhiên Homestay: 4 reviews (4★+4★+5★+3★) → avg 4.0 × 2 = 8.0
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
    -- Demo bộ lọc điểm đánh giá:
    --   Filter 9+ "Tuyệt vời"  → 4 kết quả (acc1, acc3, acc4, acc6, acc8, acc9 = 6 nếu 10.0 tính)
    --   Filter 8+ "Rất tốt"    → 8 kết quả (thêm acc5, acc7 — bỏ acc2)
    --   Filter 7+ "Tốt"        → 9 kết quả (tất cả acc đã duyệt, kể cả acc2)
    --   Không lọc             → 9 kết quả + 2 pending/rejected
    --
    -- Demo bộ lọc sao:
    --   Filter 5★              → 4 kết quả (acc3, acc4, acc8, acc9)
    --   Filter 4★ trở lên      → 6 kết quả (+ acc1, acc5)
    --   Filter 3★ trở lên      → 9 kết quả (tất cả 9 APPROVED)
    -- =============================================

    -- =============================================
    -- KIỂM TRA KỲ VỌNG CUỐI CÙNG v11 (sau import đầy đủ)
    -- SELECT COUNT(*) FROM users;               -- kỳ vọng: 8 (1 admin, 3 user, 4 partner)
    -- SELECT COUNT(*) FROM accommodations;      -- kỳ vọng: 13 (11 APPROVED + 2 PENDING/REJECTED)
    -- SELECT COUNT(*) FROM rooms;               -- kỳ vọng: 30
    -- SELECT COUNT(*) FROM bookings;            -- kỳ vọng: ~61 (+2 DEP01/DEP02 v11)
    -- SELECT COUNT(*) FROM payments;            -- kỳ vọng: ~61 (+2 DEP01/DEP02 v11)
    -- SELECT COUNT(*) FROM reviews;             -- kỳ vọng: ~23
    -- SELECT COUNT(*) FROM vouchers;            -- kỳ vọng: 8
    -- SELECT COUNT(*) FROM amenities;           -- kỳ vọng: 32
    -- SELECT COUNT(*) FROM room_amenities;      -- kỳ vọng: ~230
    -- SELECT COUNT(*) FROM partner_settlements; -- kỳ vọng: 26
    -- SELECT COUNT(*) FROM admin_action_logs;   -- kỳ vọng: ~110 (sau bổ sung partner3/4)
    -- SELECT COUNT(*) FROM notifications;       -- kỳ vọng: 21 (user2=9, user5=6, user6=6)
    --
    -- Kiểm tra phổ điểm rating:
    -- SELECT id, name, star_rating AS sao, rating AS diem, review_count AS so_review
    --   FROM accommodations WHERE approval_status='APPROVED' ORDER BY rating DESC;
    -- =============================================

    -- =============================================
    -- SEED DATA NOTIFICATIONS v10 — Demo hệ thống thông báo đầy đủ
    -- =============================================
    -- user@travelmate.vn  (id=2, Nguyễn Văn An)  → 9 thông báo (6 chưa đọc: checkin×2 + review×4)
    -- user2@travelmate.vn (id=5, Trần Thị Mai)    → 6 thông báo (4 chưa đọc: checkin×1 + review×3)
    -- user3@travelmate.vn (id=6, Lê Văn Đức)      → 6 thông báo (4 chưa đọc: checkin×1 + review×3)
    -- Mục đích: giảng viên đăng nhập bất kỳ user đều thấy bell có số badge cao, demo đủ 4 loại type
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
     'Đặt phòng đã được xác nhận — Vinpearl Resort Nha Trang',
     'Booking BK-VNT-DLX-0001 tại Vinpearl Resort & Spa Nha Trang đã được admin xác nhận. Check-in trong 5 ngày tới. Chúc bạn có chuyến nghỉ dưỡng tuyệt vời!',
     'BOOKING_CONFIRMED', '/my-bookings', 1,
     DATE_SUB(NOW(), INTERVAL 4 DAY)),

    -- [READ] BOOKING_CONFIRMED: BK-LATA-STD-0002 (acc1 LATA)
    (2,
     'Đặt phòng đã được xác nhận — LATA Hotel & Apartments',
     'Booking BK-LATA-STD-0002 tại LATA Hotel & Apartments đã được admin xác nhận. Chúc bạn có chuyến đi vui vẻ!',
     'BOOKING_CONFIRMED', '/my-bookings', 1,
     DATE_SUB(NOW(), INTERVAL 17 DAY)),

    -- [READ] SYSTEM: Thông báo hệ thống TravelMate
    (2,
     'TravelMate ra mắt tính năng Voucher cho Partner',
     'Từ hôm nay, các đối tác có thể tạo voucher giảm giá riêng cho cơ sở và từng phòng của mình. Khách hàng sẽ thấy voucher ngay khi đặt phòng. Cập nhật thêm tại mục Khuyến mãi!',
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

    -- [READ] BOOKING_CONFIRMED: BK-MND-ATT-0002 (acc7 Mộc Nhiên vừa xác nhận)
    (5,
     'Đặt phòng đã được xác nhận — Mộc Nhiên Garden Homestay',
     'Booking BK-MND-ATT-0002 tại Mộc Nhiên Garden Homestay Đà Lạt đã được admin xác nhận. Check-in trong 3 ngày tới. Chúc bạn có chuyến đi vui vẻ!',
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

    -- [READ] BOOKING_CONFIRMED: BK-ANM-GDN-0003 (acc4 Anam Villa vừa xác nhận)
    (6,
     'Đặt phòng đã được xác nhận — The Anam Villa Nha Trang',
     'Booking BK-ANM-GDN-0003 tại The Anam Villa Nha Trang đã được admin xác nhận. Check-in trong 5 ngày tới. Tận hưởng villa hạng sang!',
     'BOOKING_CONFIRMED', '/my-bookings', 1,
     DATE_SUB(NOW(), INTERVAL 2 DAY)),

    -- [READ] SYSTEM: Nhắc nhở mang giấy tờ tuỳ thân khi check-in
    (6,
     'Nhắc nhở: Mang theo CCCD/Hộ chiếu khi check-in',
     'Theo quy định mới của Bộ Công An, tất cả khách lưu trú phải xuất trình CCCD hoặc Hộ chiếu khi làm thủ tục nhận phòng. Đảm bảo bạn mang đủ giấy tờ!',
     'SYSTEM', '/my-bookings', 1,
     DATE_SUB(NOW(), INTERVAL 10 DAY));

    -- Kiểm tra seed notifications v10:
    -- SELECT u.email, n.type, n.is_read, n.title
    --   FROM notifications n JOIN users u ON u.id = n.user_id ORDER BY u.id, n.created_at DESC;
    -- Kỳ vọng: 21 rows tổng
    --   user2 (id=2):  9 rows — 6 unread (badge=6), 3 read
    --   user5 (id=5):  6 rows — 4 unread (badge=4), 2 read
    --   user6 (id=6):  6 rows — 4 unread (badge=4), 2 read
    -- Mọi type đều có: REVIEW_REMINDER, BOOKING_CHECKIN_READY, BOOKING_CONFIRMED, SYSTEM

    -- (end of travelmate_db.sql v10 — full notifications demo)

    -- =============================================

    -- =============================================
    -- TRAVEL_POSTS SEED DATA (CMS — admin quan ly noi dung du lich)
    -- Danh muc: GUIDE | ATTRACTION | ESSENTIAL
    -- Trang thai: VISIBLE | HIDDEN
    -- =============================================
    CREATE TABLE IF NOT EXISTS travel_posts (
        id            BIGINT       NOT NULL AUTO_INCREMENT,
        title         VARCHAR(255) NOT NULL,
        summary       TEXT,
        content       TEXT,
        thumbnail_url VARCHAR(500),
        source_url    VARCHAR(500) NOT NULL,
        category      VARCHAR(20)  NOT NULL DEFAULT 'GUIDE',
        status        VARCHAR(10)  NOT NULL DEFAULT 'VISIBLE',
        created_by    VARCHAR(100),
        created_at    DATETIME(6),
        updated_at    DATETIME(6),
        PRIMARY KEY (id),
        INDEX idx_tp_category (category),
        INDEX idx_tp_status   (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    INSERT INTO travel_posts (title, summary, content, thumbnail_url, source_url, category, status, created_by, created_at, updated_at) VALUES

    -- CAM NANG (GUIDE)
    (
        'Kinh nghiem du lich Da Lat tu A den Z cho nguoi lan dau',
        'Tat tan tat nhung gi ban can biet truoc khi dat chan den thanh pho ngan hoa: thoi tiet, phuong tien, an o, diem tham quan va bi kip tiet kiem chi phi.',
        'Da Lat o do cao 1500m tren cao nguyen Lam Vien - thien duong mat me quanh nam cua Viet Nam.\n\nGIAO THONG: Tu TPHCM co xe khach (7-8h), may bay (45 phut). Nen di thu 2-6 tranh ket xe cuoi tuan.\n\nTHOI TIET: Mua kho thang 11 den thang 4 la ly tuong nhat. Nhiet do 20-25 do ban ngay, 10-15 do ban dem - nho mang ao am.\n\nDIEM THAM QUAN NOI BAT:\n- Thung lung Tinh Yeu & Ho Xuan Huong\n- Vuon hoa thanh pho ngam hoa sac mau theo mua\n- Thap Bao Dai - kien truc Phap co dien\n- Lang Cu Lan - ban lang dan toc thieu so\n- Nui Langbiang - leo nui ngam toan canh Da Lat\n\nAM THUC: Banh mi xiu mai, banh trang nuong, ca phe trung, dau tuoi. Gia re dac trung vung cao nguyen.\n\nBI KIP: Dat phong truoc 2-3 tuan vao dip le/tet. Thue xe may 100-150k/ngay la lua chon tiet kiem nhat.',
        'https://images.unsplash.com/photo-1556909172-54557c7e4fb7?w=600&q=80',
        'https://dulich.tuoitre.vn/kinh-nghiem-du-lich-da-lat',
        'GUIDE', 'VISIBLE', 'admin@travelmate.vn',
        DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY)
    ),
    (
        'Cam nang du lich Hoi An: Di dau, an gi, o dau?',
        'Hoi An - pho co den long huyen ao ben dong Thu Bon. Huong dan chi tiet tu cach di chuyen, khung gio dep chup hinh, den nhung quan an ngon nguoi dan dia phuong hay lui toi.',
        'Hoi An - di san van hoa the gioi UNESCO voi kien truc hoa quyen Viet-Trung-Nhat doc dao.\n\nTHOI DIEM THICH HOP: Thang 2-7 ly tuong nhat. Thang 9-12 thuong co mua va lu lut. Dem ram hang thang co Hoi Den Long rat dep.\n\nDI CHUYEN: Tu Da Nang (30km): Taxi ~350k, xe om cong nghe ~120k, xe buyt ~25k.\n\nKHAM PHA PHO CO:\n- Cau Nhat Ban (Lai Vien Kieu): Bieu tuong 500 nam tuoi\n- Hoi quan Phuc Kien: Kien truc Hoa kieu ky vi\n- Nha co Quan Thang: Nguyen ban tu the ky 17\n- Cho Hoi An: Am thuc duong pho da dang\n\nAM THUC NGON: Cao lau, Mi Quang, Banh Mi Phuong (noi tieng the gioi), Com ga Ba Buoi, Banh Vac.\n\nBI KIP: Di bo pho co luc 6-8h sang hoac 17-19h chieu de tranh nong va dong nguoi nhat.',
        'https://images.unsplash.com/photo-1528127269322-539801943592?w=600&q=80',
        'https://vnexpress.net/cam-nang-du-lich-hoi-an',
        'GUIDE', 'VISIBLE', 'admin@travelmate.vn',
        DATE_SUB(NOW(), INTERVAL 22 DAY), DATE_SUB(NOW(), INTERVAL 22 DAY)
    ),
    (
        'Bi kip dat phong khach san gia tot nhat cho ky nghi le',
        'Thoi diem dat phong, cac trang web so sanh gia, meo dung voucher, va cach chon khu vuc o de tiet kiem thoi gian di chuyen trong chuyen di cua ban.',
        'Dat phong gia tot la ky nang co the hoc duoc - khong phai may man. Tiet kiem 20-50% chi phi ma van o noi chat luong.\n\nTHOI DIEM DAT PHONG LY TUONG:\n- Dat truoc 6-8 tuan cho mua cao diem (le, tet, he)\n- Dat truoc 2-4 tuan cho chuyen di binh thuong\n- Flash sale thuong xuat hien vao thu 3, thu 4 hang tuan\n\nCAC TRANG WEB SO SANH GIA UY TIN:\n1. Agoda - thuong co gia tot cho chau A\n2. Booking.com - nhieu lua chon, chinh sach huy linh hoat\n3. Hotels.com - tich diem doi dem mien phi\n4. Traveloka - uu dai cho khach Dong Nam A\n5. Airbnb - lua chon tot cho nhom dong, gia dinh\n\nMEO VOUCHER: Ket hop voucher ngan hang voi ma giam ung dung. Dat qua mobile app thuong giam them 5-10%.\n\nBI KIP: Hoi truc tiep khach san qua dien thoai - doi khi giam hon booking online. Chon Free Cancellation neu lich chua chac chan.',
        'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=600&q=80',
        'https://traveloka.com/meo-dat-phong-gia-tot',
        'GUIDE', 'VISIBLE', 'admin@travelmate.vn',
        DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY)
    ),

    -- DIA DIEM THAM QUAN (ATTRACTION)
    (
        'Top 10 dia diem khong the bo qua tai Nha Trang',
        'Tu dao Hon Tre voi Vinpearl Land huyen thoai, Thap Ba Ponagar ngan nam tuoi, den bai bien Bai Dai trong veo - 10 diem check-in cuc dep dang cho ban kham pha.',
        'Nha Trang - thanh pho bien xanh thuoc Khanh Hoa, noi tieng bai bien dai, nuoc bien trong va nhieu diem vui choi giai tri. Trung tam du lich bien hang dau Dong Nam A.\n\nTOP 10 DIEM THAM QUAN:\n1. Vinpearl Land - cong vien giai tri lon nhat Viet Nam\n2. Thap Ba Ponagar - di tich Cham Pa 1.200 nam tuoi\n3. Bai bien Bai Dai - bai bien hoang so dep nhat\n4. Vien Hang Duong - bao tang bien doc dao\n5. Suoi Ba Ho - thac nuoc tu nhien tuyet dep\n6. Chua Long Son - tuong Phat trang khong lo\n7. Dam Tre - ho nuoc ngot giua dao\n8. Hon Mun - lang bieu san ho da sac\n9. Nha tho Nui - kien truc Phap co kinh\n10. Cho Dam - cho truyen thong san vat bien\n\nAM THUC: Nem nuong Ninh Hoa, cha ca Thu, Bun ca, Banh can, Banh uot.',
        'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&q=80',
        'https://dulich.tuoitre.vn/top-10-dia-diem-nha-trang',
        'ATTRACTION', 'VISIBLE', 'admin@travelmate.vn',
        DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)
    ),
    (
        'Kham pha Vuon quoc gia Phong Nha - Ke Bang: Hang dong ky vi nhat hanh tinh',
        'Son Doong - hang dong lon nhat the gioi, Hang En huyen ao, dong Phong Nha lung linh anh den. Huong dan lich trinh 3 ngay 2 dem kham pha Quang Binh day du nhat.',
        'Phong Nha-Ke Bang - Vuon quoc gia UNESCO voi he thong hang dong phong phu nhat hanh tinh.\n\nCAC HANG DONG CHINH:\n- Son Doong: Lon nhat TG, tour 6 ngay chi 70 nguoi/dot (dat truoc 1-2 nam)\n- Hang En: Lon thu 3 TG, tour 2 ngay dep nhat mua he\n- Dong Phong Nha: Di thuyen 1500m trong hang nuoc co\n- Tien Son: Hang kho voi hinh thu da doc dao\n- Hang Toi: Leo tro, Zip line, boi nuoc ngam\n- Paradise Cave: Dep nhat Dong Nam A\n\nLICH TRINH GOI Y 3N2D:\nNgay 1: Dong Phong Nha + lang Co Viet Ho\nNgay 2: Hang Toi hoac Paradise Cave\nNgay 3: Tien Son + Hang Nuoc Nut + ve\n\nLUU Y: Dat tour truoc 2-3 thang mua he (6-8/). Mua bao hiem. Mang kem chong nang SPF 50+.',
        'https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=600&q=80',
        'https://vietnamtourism.vn/phong-nha-ke-bang',
        'ATTRACTION', 'VISIBLE', 'admin@travelmate.vn',
        DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)
    ),
    (
        'Sa Pa mua lua chin: Ruong bac thang dep nhat Dong Nam A',
        'Thang 9-10 la thoi diem vang de chiem nguong ruong bac thang Sa Pa khi lua chin vang ong. Huong dan thue xe may tu kham pha ban Cat Cat, Lao Chai, Ta Van.',
        'Sa Pa mua lua chin thang 9-10 la mot trong nhung canh dep nhat Dong Nam A. Ruong bac thang nhuom vang ong duoi nang thu - khung canh mo uoc cua moi nhiep anh gia.\n\nTHOI DIEM CHUP ANH DEP NHAT: Cuoi thang 9 dau thang 10. Ngay dep nhat sau khi mua, bau troi quang va ruong phan anh anh nang.\n\nBAN LANG CAN THAM QUAN:\n- Ban Cat Cat: 3km tu trung tam, di bo hoac xe may\n- Ban Lao Chai & Ta Van: Nguoi H Mong va Giay sinh song\n- Ban Ta Phin: Lang nguoi Dao Do voi di san day thuoc\n- Dinh Fansipan: 3143m - noc nha Dong Duong\n\nDI CHUYEN & O: Tu Ha Noi: Tau hoa dem (Hanoi-Lao Cai) + xe buyt/taxi len Sa Pa. Homestay ban lang: 150-300k/dem, trai nghiem van hoa that su.\n\nAM THUC: Thang co nuong, Pho Sa Pa, Ruou Ngo Bac Ha, Ca hoi Sa Pa nuong.',
        'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=600&q=80',
        'https://dulich.tuoitre.vn/sapa-mua-lua-chin',
        'ATTRACTION', 'VISIBLE', 'admin@travelmate.vn',
        DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)
    ),

    -- CAN THIET CHO DU LICH (ESSENTIAL)
    (
        'Checklist do can mang khi di du lich bien mua he',
        'Kem chong nang SPF 50+, ao chong nang, thuoc say song, balo chong tham nuoc - danh sach 25 vat dung thiet yeu giup chuyen di bien cua ban hoan hao tu dau den cuoi.',
        'Chuan bi ky giup chuyen di bien hoan hao. 25 vat dung thiet yeu khong nen bo qua.\n\nBAO VE DA (quan trong nhat):\n1. Kem chong nang SPF 50+ chong nuoc (boi lai sau 2h)\n2. Ao chong nang vat lieu thoang mat\n3. Kinh mat UV400\n4. Non rong vanh\n\nTRANG PHUC: Do boi/bikini (2-3 bo), sandal hoac dep nhua, ao thun nhe cho buoi toi, quan short nhanh kho.\n\nY TE & AN TOAN: Thuoc say song (Nauzin hoac Nospa), thuoc chong di ung, kem sau cat cat, gao rua tay, thuoc giam dau ha sot.\n\nDO DUNG TIEN ICH: Balo chong nuoc, tui chong nuoc cho dien thoai, binh nuoc giu nhiet, tui dung do boi uot, kem duong am va son moi.\n\nDO GIAI TRI: May anh chong nuoc, loa Bluetooth chong nuoc, sach/kindle.\n\nCHO TRE EM (neu di kem): Phao boi, chan cat, kem chong nang chuyen dung cho be.',
        'https://images.unsplash.com/photo-1506197603052-3cc9c3a201bd?w=600&q=80',
        'https://thegioidulich.com/checklist-do-di-bien',
        'ESSENTIAL', 'VISIBLE', 'admin@travelmate.vn',
        DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY)
    ),
    (
        'Huong dan mua bao hiem du lich: Loai nao phu hop voi ban?',
        'Bao hiem hanh ly, y te, huy chuyen - so sanh cac goi bao hiem du lich pho bien hien nay, muc phi, quyen loi va cach claim bao hiem nhanh nhat khi gap su co.',
        'Bao hiem du lich - khoan dau tu khon ngoan cho moi chuyen di dai ngay.\n\nCAC LOAI BAO HIEM CHINH:\n1. Bao hiem y te: Chi tra vien phi, phau thuat tai nuoc ngoai\n2. Bao hiem hanh ly: Den bu mat mat, bi trom\n3. Bao hiem huy chuyen: Boi thuong khi huy do bat kha khang\n4. Bao hiem toan dien: Gop ca 3 loai tren\n\nMUC PHI THAM KHAO:\n- Goi co ban (y te): 50.000-150.000 VND/ngay\n- Goi toan dien: 150.000-400.000 VND/ngay\n- Bao hiem nam (cho nguoi hay di): 2-5 trieu/nam\n\nCONG TY UY TIN: PTI, Bao Viet, PVI (trong nuoc). AXA, Allianz, AIG (quoc te).\n\nCACH CLAIM NHANH:\n1. Giu het hoa don, bien ban\n2. Chup anh bien lai vien phi\n3. Bao canh sat neu bi trom\n4. Lien he hotline 24/7 ngay khi co su co\n5. Nop ho so trong 30 ngay sau khi ve nuoc',
        'https://images.unsplash.com/photo-1450101499163-c8848c66ca85?w=600&q=80',
        'https://baohiemxahoi.gov.vn/bao-hiem-du-lich',
        'ESSENTIAL', 'VISIBLE', 'admin@travelmate.vn',
        DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)
    ),
    (
        'Doi tien va thanh toan khi du lich noi dia: Meo khong bi ho',
        'Nen mang bao nhieu tien mat? ATM o dau? App vi dien tu nao duoc chap nhan rong rai nhat? Tat ca bi kip de ban khong gap rac roi tai chinh trong chuyen di.',
        'Quan ly tai chinh du lich khon ngoan - biet dung tien hop ly, tranh bi lua dao va gap rac roi.\n\nBAO NHIEU TIEN MAT LA DU:\n- Du lich thanh pho: 200-500k/ngay (an uong, di lai)\n- Tham quan bien/nui: 500k-1tr/ngay\n- Tong goi y: 1-2 trieu/ngay, them 30% du phong\n\nATM O DAU: Tat ca ngan hang lon co ATM tai trung tam. Agribank, VietinBank co o huyen/xa. Phi rut ATM ngan hang khac: 5.000-11.000 VND/lan.\n\nCAC APP VI DIEN TU HOT:\n1. MoMo - chap nhan rong rai nhat\n2. ZaloPay - QR toc do cao\n3. VNPay - tich hop nhieu ngan hang\n4. Grab Pay - tien cho dat xe\n5. Shopee Pay - uu dai mua sam\n\nBI KIP TRANH MAT TIEN:\n- Chia tien lam nhieu noi\n- Khong de vi o tui quan phia sau\n- Kiem tra tien tra lai khi nhan\n- Tranh doi tien tai san bay - phi rat cao',
        'https://images.unsplash.com/photo-1580048915913-4f8f5cb481c4?w=600&q=80',
        'https://travelmate.vn/meo-thanh-toan-du-lich',
        'ESSENTIAL', 'VISIBLE', 'admin@travelmate.vn',
        DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)
    );

    -- Kiem tra seed travel_posts:
    -- SELECT category, status, COUNT(*) FROM travel_posts GROUP BY category, status;
    -- Ky vong: GUIDE/VISIBLE=3, ATTRACTION/VISIBLE=3, ESSENTIAL/VISIBLE=3 (tong 9)
