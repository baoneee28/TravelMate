package com.travelmate.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User Portal UI — Checklist trình bày trực quan")
class UserPortalFlowTemplateTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    @Test
    @DisplayName("Trang chủ search rỗng mặc định Đà Lạt và footer mở listing Đà Lạt")
    void homepageAndFooterDefaultToDaLatForDemoSearch() throws IOException {
        String index = read("user/index.html");
        String footer = read("fragments/user-footer.html");

        assertThat(index).contains("mặc định gợi ý <strong>Đà Lạt</strong>");
        assertThat(index).contains("destInput.value = 'Đà Lạt'");
        assertThat(footer).contains("type='HOTEL',keyword='Đà Lạt'");
        assertThat(footer).contains("Khách sạn Đà Lạt");
    }

    @Test
    @DisplayName("TravelBot giữ điểm đến vừa tìm để khách có thể nói tiếp là muốn đặt")
    void travelBotRemembersLastDestinationForBookingFollowUp() throws IOException {
        String floatingActions = read("fragments/user-floating-actions.html");

        assertThat(floatingActions).contains(
                "var lastDestination = '';",
                "rememberBookingDestination(qrs)",
                "var bookingPrefix = 'Đặt phòng ';",
                "lastDestination = qrs[i].slice(bookingPrefix.length).trim();",
                "JSON.stringify({ message: message, lastDestination: lastDestination })");
    }

    @Test
    @DisplayName("Detail có modal xem chi tiết phòng/căn và wording Villa dùng căn")
    void hotelDetailHasRoomDetailModalAndVillaWording() throws IOException {
        String detail = read("user/hotel-detail.html");

        assertThat(detail).contains("openRoomDetailModal");
        assertThat(detail).contains("room-detail-modal");
        assertThat(detail).contains("window.location.hash", "scrollIntoView", "data-room-id");
        assertThat(detail).contains("Xem chi tiết căn");
        assertThat(detail).contains("Đặt căn");
        assertThat(detail).contains("Hết căn");
        assertThat(detail).contains("gallery-grid", "gallery-thumbs");
        assertThat(detail).contains("stat.index > 0 and stat.index < 5");
        assertThat(detail).contains("stat.index == 4");
        assertThat(detail).contains("Xem tất cả hình ảnh");
        assertThat(detail).contains("gallery-image-source", "source.dataset.src");
        assertThat(detail).containsOnlyOnce("id=\"galleryImageSources\"");
        assertThat(detail.indexOf("id=\"galleryImageSources\"")).isLessThan(detail.indexOf("id=\"galleryModal\""));
    }

    @Test
    @DisplayName("Detail ưu tiên đúng ảnh phòng upload và không bù ảnh mẫu khi đã có ảnh thật")
    void hotelDetailGalleryUsesUploadedRoomImagesBeforeFallback() throws IOException {
        String controller = Files.readString(Path.of(
                "src/main/java/com/travelmate/controller/page/AccommodationPageController.java"));
        String detail = read("user/hotel-detail.html");

        assertThat(controller).contains(
                "private static final int DETAIL_ROOM_UPLOAD_LIMIT = 3;",
                "if (gallery.size() >= DETAIL_ROOM_UPLOAD_LIMIT) {",
                "return gallery;",
                "addGalleryImage(gallery, usedUrls, hotel.getThumbnailUrl(), hotel.getName())");
        assertThat(controller).contains(
                "if (!gallery.isEmpty()) {\n" +
                "            return gallery;\n" +
                "        }\n\n" +
                "        for (Room room : galleryRooms)");
        assertThat(detail).contains("ưu tiên ảnh phòng/căn đã upload");
    }

    @Test
    @DisplayName("Booking page hiển thị preview phòng và voucher áp dụng")
    void bookingPageShowsRoomPreviewAndApplicableVouchers() throws IOException {
        String booking = read("user/booking.html");

        assertThat(booking).contains("applicableVouchers");
        assertThat(booking).contains("Dùng mã");
        assertThat(booking).contains("Đã thanh toán 100% qua VNPAY");
        assertThat(booking).contains("70% còn lại thanh toán tại cơ sở");
        assertThat(booking).contains(
                "@media (max-width:900px)",
                ".bk-main",
                "flex-direction: column",
                ".bk-right",
                "position: static",
                ".bk-price-total",
                ".deposit-remainder-row");
    }

    @Test
    @DisplayName("Admin quản lý ảnh có preview, upload và validate JPG/PNG/WEBP")
    void adminRoomsImageEditorSupportsPreviewAndValidatedUpload() throws IOException {
        String rooms = read("admin/rooms.html");

        assertThat(rooms).contains("openRoomImageGallery");
        assertThat(rooms).contains("Duyệt ảnh hiển thị phòng/căn");
        assertThat(rooms).contains("room-code-link");
        assertThat(rooms).contains("room-action-icon--detail", "room-action-icon--images");
        assertThat(rooms).contains("enctype=\"multipart/form-data\"");
        assertThat(rooms).contains("name=\"imageFiles\"");
        assertThat(rooms).contains("Chỉ hỗ trợ JPG, PNG, WEBP");
        assertThat(rooms).contains("refreshUploadedImagePreview");
        assertThat(rooms).contains(
                "room-commission-chip",
                "room-commission-chip__rate",
                "room-commission-chip__label",
                "Riêng theo phòng");
    }

    @Test
    @DisplayName("Admin settlement tách cột trạng thái và ngày chuyển để không đè UI")
    void adminSettlementsSeparateStatusAndTransferDateColumns() throws IOException {
        String settlements = read("admin/settlements.html");

        assertThat(settlements).contains(
                ".admin-topbar__actions",
                "gap: 10px",
                "min-width: 1460px",
                "<th style=\"width: 150px;\">Trạng thái</th>",
                "<th style=\"width: 115px;\">Ngày chuyển</th>",
                "class=\"set-status-cell\"",
                "class=\"set-transfer-cell\"");
        assertThat(settlements).doesNotContain("style=\"font-size: 11.5px;\"");
    }

    @Test
    @DisplayName("Partner thêm phòng/căn bằng ảnh upload từ máy và có preview")
    void partnerRoomFormUsesLocalImageUploadInsteadOfRequiredUrl() throws IOException {
        String roomForm = read("partner/room-form.html");

        assertThat(roomForm).contains(
                "partner-room-form-page",
                "room-form-card",
                "room-form-grid",
                "room-form-wide",
                "name=\"roomCode\"",
                "enctype=\"multipart/form-data\"",
                "name=\"roomImages\"",
                "accept=\"image/png,image/jpeg,image/webp\"",
                "previewRoomImages",
                "Tải lên tối đa 3 ảnh JPG, PNG hoặc WEBP",
                "Ảnh đầu tiên sẽ làm ảnh đại diện");
        assertThat(roomForm).doesNotContain(
                "URL ảnh phòng",
                "Nhập URL ảnh phòng",
                "<span style=\"color:#ef4444;font-size:13px;\">*</span>");
    }

    @Test
    @DisplayName("Partner chỉ gắn voucher từ kho, không có form tự tạo voucher")
    void partnerVoucherPageOnlyAllowsAssigningCatalogVouchers() throws IOException {
        String vouchers = read("partner/vouchers.html");
        String adminVouchers = read("admin/vouchers.html");
        String partnerController = Files.readString(Path.of(
                "src/main/java/com/travelmate/controller/page/PartnerPageController.java"));

        assertThat(vouchers).contains("Admin phát hành");
        assertThat(vouchers).contains("Kho voucher có thể gắn");
        assertThat(vouchers).contains("áp dụng cho tất cả loại", "TẤT CẢ LOẠI");
        assertThat(vouchers).contains("@{/partner/vouchers/assign}");
        assertThat(vouchers).doesNotContain("@{/partner/vouchers/create}");
        assertThat(vouchers).doesNotContain("name=\"code\"");
        assertThat(partnerController).doesNotContain(
                "/vouchers/create-for-accommodation",
                "/vouchers/create-for-room");
        assertThat(adminVouchers).contains("Tất cả loại lưu trú", "propertyType.required = false");
    }

    @Test
    @DisplayName("Partner revenue hiển thị rõ cơ sở hoa hồng theo tổng đơn gốc")
    void partnerRevenueMakesOnlineCommissionBaseVisible() throws IOException {
        String revenue = read("partner/revenue.html");

        assertThat(revenue).contains(
                "Cơ sở tính",
                "item.commissionBase",
                "item.commissionBaseLabel",
                "Hoa hồng của mọi booking online được tính theo tổng đơn gốc trước voucher",
                "70% khách trả tại cơ sở được hiển thị riêng");
    }

    @Test
    @DisplayName("Partner UI không mở check-in/no-show trước ngày nhận phòng")
    void partnerBookingUiGuardsActionsBeforeCheckInDate() throws IOException {
        String bookings = read("partner/bookings.html");
        String detail = read("partner/booking-detail.html");

        assertThat(bookings).contains(
                "checkInReady",
                "Chưa tới ngày nhận",
                "Check-in / No-show mở từ",
                "Check-in mở từ",
                "✅ Đã giữ ");
        assertThat(detail).contains(
                "checkInReady",
                "Chưa tới ngày nhận",
                "thao tác check-in/no-show sẽ mở từ",
                "booking.partnerStatus.name()=='PARTNER_CONFIRMED'");
        assertThat(bookings).doesNotContain(
                "Đã giữ phòng — chờ khách đến",
                "Đã thanh toán 100% — liên hệ Admin",
                "Xác nhận giữ phòng");
        assertThat(detail).doesNotContain(
                "Đã giữ phòng — chờ khách đến",
                "Khách đã thanh toán 100%. Nếu khách không đến",
                "Xác nhận giữ phòng");
    }

    @Test
    @DisplayName("My bookings hiển thị hoàn 70% cho thanh toán đủ và mất cọc cho cọc 30%")
    void myBookingsAllowsUserCancellationBeforeCheckIn() throws IOException {
        String myBookings = read("user/mybooking.html");

        assertThat(myBookings).contains(
                "b.bookingStatus.name() == 'PENDING_PAYMENT'",
                "b.bookingStatus.name() == 'PENDING_ADMIN_APPROVAL'",
                "b.bookingStatus.name() == 'CONFIRMED'",
                "openCancelModal",
                "b.paymentStatus.name()",
                "Hủy đơn chưa thanh toán",
                "Không phát sinh yêu cầu hoàn tiền",
                "Quá hạn thanh toán",
                "Còn lại tại cơ sở",
                "Số tiền dự kiến hoàn: <strong>70%</strong>",
                "tiền cọc <strong>không được hoàn lại</strong>",
                "Cọc bị giữ lại (Hủy/no-show)",
                "@{/my-bookings/{id}/rebook",
                "Đặt lại phòng này",
                "Đơn mới dùng lại ngày, số khách và số lượng nếu còn phù hợp",
                "@media (max-width: 640px)",
                ".mb-rebook-btn { grid-column: 1 / -1; }",
                "scroll-margin-top: 112px",
                ".mb-rebook-note",
                "max-width: none");
    }

    @Test
    @DisplayName("News tag dùng layout kết quả riêng và có empty-state rõ ràng")
    void newsTagFilterUsesFilteredGridAndEmptyState() throws IOException {
        String news = read("user/news.html");

        assertThat(news).contains("filteredNewsSection");
        assertThat(news).contains("@{/news(keyword=${location})", "newsFiltered", "newsFilterLabel");
        assertThat(news).contains("newsLayout.style.display = 'none'");
        assertThat(news).contains("Chưa có bài viết cho chủ đề này");
        assertThat(news).contains("backAllNewsBtn");
    }

    @Test
    @DisplayName("Partner có nút tạm ngừng/mở bán phòng trực quan, không xóa cứng")
    void partnerRoomStatusHasQuickStopSellingAction() throws IOException {
        String accommodations = read("partner/accommodations.html");
        String roomStatus = read("partner/room-status.html");

        assertThat(accommodations).contains("Ngừng bán nhanh", "Tình trạng phòng");
        assertThat(accommodations).contains(
                "approvedRoomCountMap",
                "pendingRoomCountMap",
                "Chờ duyệt phòng",
                "Chưa public phòng",
                "Khách hàng chưa thấy phòng/căn mới cho tới khi Admin duyệt");
        assertThat(accommodations).doesNotContain("Listing đã duyệt — chờ duyệt phòng");
        assertThat(roomStatus).contains(
                "@{/partner/rooms/{roomId}/toggle-selling",
                "Tạm ngừng bán",
                "Mở bán lại",
                "không đổi quota gốc");
    }

    @Test
    @DisplayName("Admin xem trang user không còn thanh preview và không đặt phòng như User")
    void adminPreviewModeDoesNotShowStickyBarOrEnterUserBookingFlow() throws IOException {
        String header = read("fragments/user-header.html");
        String footer = read("fragments/user-footer.html");
        String detail = read("user/hotel-detail.html");
        String style = Files.readString(Path.of("src/main/resources/static/assets/css/style.css"));

        assertThat(header).doesNotContain("admin-preview-bar", "Đang xem trang người dùng với vai trò Admin");
        assertThat(style).doesNotContain("admin-preview-bar");
        assertThat(header).contains("QUAY VỀ ADMIN", "@{/admin/dashboard}", "@{/admin/bookings}", "@{/admin/vouchers}");
        assertThat(footer).contains(
                "th:if=\"${!isAdmin}\"",
                "@{/my-bookings}",
                "th:if=\"${isAdmin}\"",
                "@{/admin/bookings}",
                "Quản lý booking");
        assertThat(detail).contains(
                "btn-choose--disabled",
                "Đang xem trước",
                "Dùng tài khoản User để đặt phòng thử.",
                "Admin đang xem trước");
    }

    @Test
    @DisplayName("Header có đổi ngôn ngữ gọn và login dẫn tới quên mật khẩu thật")
    void headerSupportsLocaleSwitchAndForgotPasswordFlowIsVisible() throws IOException {
        String header = read("fragments/user-header.html");
        String login = read("auth/login.html");
        String register = read("auth/register.html");
        String profile = read("user/profile.html");
        String forgot = read("auth/forgot-password.html");
        String reset = read("auth/reset-password.html");
        String defaultMessages = Files.readString(Path.of("src/main/resources/messages.properties"));
        String englishMessages = Files.readString(Path.of("src/main/resources/messages_en.properties"));

        assertThat(header).contains("language-switch", "?lang=vi", "?lang=en", "#{nav.home}");
        assertThat(header).contains(
                "header-avatar-fallback",
                "referrerpolicy=\"no-referrer\"",
                "onerror=\"this.remove();this.closest('.header-avatar').classList.remove('has-photo');\"");
        assertThat(profile).contains("referrerpolicy=\"no-referrer\"", "avatarPreviewWrap", "avatarIcon");
        assertThat(defaultMessages).contains("nav.function=Chức năng", "auth.login=ĐĂNG NHẬP",
                "auth.forgot.heading=Quên mật khẩu", "auth.reset.heading=Đặt lại mật khẩu");
        assertThat(englishMessages).contains("auth.login.heading=Log in", "auth.register.heading=Create an account",
                "auth.forgot.heading=Forgot password", "auth.reset.heading=Reset password");
        assertThat(login).contains(
                "/auth/forgot-password",
                "googleOAuthConfigured",
                "/oauth2/authorization/google",
                "#{auth.login.heading}",
                "#{auth.google.disabled}",
                "disabled");
        assertThat(register).contains("#{auth.register.heading}", "#{auth.register.error.passwordMismatch}");
        assertThat(forgot).contains("reset-result", "resetMessageType", "#{auth.forgot.heading}")
                .doesNotContain("demoResetLink");
        assertThat(reset).contains("tokenValid", "/auth/reset-password", "Liên kết không còn hiệu lực",
                "#{auth.reset.heading}");
    }

    @Test
    @DisplayName("Review/support user input dùng th:text để tránh render HTML/script")
    void userGeneratedReviewAndSupportContentUsesEscapedTextRendering() throws IOException {
        String hotelDetail = read("user/hotel-detail.html");
        String adminReviews = read("admin/reviews.html");
        String partnerSupport = read("partner/support.html");
        String adminSupport = read("admin/support.html");

        assertThat(hotelDetail).contains("th:text=\"${review.comment}\"");
        assertThat(adminReviews).contains("th:text=\"${r.comment}\"");
        assertThat(partnerSupport).contains(
                "th:text=\"${t.subject}\"",
                "th:text=\"${t.description}\"",
                "th:text=\"${t.adminResponse}\"");
        assertThat(adminSupport).contains(
                "th:text=\"${t.subject}\"",
                "th:text=\"${t.description}\"");

        assertThat(hotelDetail).doesNotContain("th:utext");
        assertThat(adminReviews).doesNotContain("th:utext");
        assertThat(partnerSupport).doesNotContain("th:utext");
        assertThat(adminSupport).doesNotContain("th:utext");
    }

    private static String read(String relative) throws IOException {
        return Files.readString(TEMPLATES.resolve(relative));
    }
}
