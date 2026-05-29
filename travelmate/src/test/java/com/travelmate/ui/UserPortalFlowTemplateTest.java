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
    @DisplayName("Booking page hiển thị preview phòng và voucher áp dụng")
    void bookingPageShowsRoomPreviewAndApplicableVouchers() throws IOException {
        String booking = read("user/booking.html");

        assertThat(booking).contains("applicableVouchers");
        assertThat(booking).contains("Dùng mã");
        assertThat(booking).contains("Đã thanh toán 100% qua VNPAY");
        assertThat(booking).contains("70% còn lại thanh toán tại cơ sở");
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
    @DisplayName("Partner revenue hiển thị rõ cơ sở hoa hồng online theo Hướng A")
    void partnerRevenueMakesOnlineCommissionBaseVisible() throws IOException {
        String revenue = read("partner/revenue.html");

        assertThat(revenue).contains(
                "Cơ sở tính",
                "item.commissionBase",
                "item.commissionBaseLabel",
                "hoa hồng chỉ tính trên phần cọc 30%",
                "70% khách trả tại cơ sở không tính hoa hồng");
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
                "Số tiền dự kiến hoàn: <strong>70%</strong>",
                "tiền cọc <strong>không được hoàn lại</strong>",
                "Cọc bị giữ lại (Hủy/no-show)");
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
        assertThat(roomStatus).contains(
                "@{/partner/rooms/{roomId}/toggle-selling",
                "Tạm ngừng bán",
                "Mở bán lại",
                "không đổi quota gốc");
    }

    @Test
    @DisplayName("Header có đổi ngôn ngữ gọn và login dẫn tới quên mật khẩu thật")
    void headerSupportsLocaleSwitchAndForgotPasswordFlowIsVisible() throws IOException {
        String header = read("fragments/user-header.html");
        String login = read("auth/login.html");
        String register = read("auth/register.html");
        String forgot = read("auth/forgot-password.html");
        String reset = read("auth/reset-password.html");
        String defaultMessages = Files.readString(Path.of("src/main/resources/messages.properties"));
        String englishMessages = Files.readString(Path.of("src/main/resources/messages_en.properties"));

        assertThat(header).contains("language-switch", "?lang=vi", "?lang=en", "#{nav.home}");
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

    private static String read(String relative) throws IOException {
        return Files.readString(TEMPLATES.resolve(relative));
    }
}
