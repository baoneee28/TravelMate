package com.travelmate.service;

import com.travelmate.entity.Booking;
import com.travelmate.entity.enums.PaymentOption;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String smtpHost;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String fromAddress;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        @Value("${spring.mail.host:}") String smtpHost,
                        @Value("${spring.mail.username:}") String smtpUsername,
                        @Value("${spring.mail.password:}") String smtpPassword,
                        @Value("${travelmate.mail.from:${spring.mail.username:}}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.smtpHost = smtpHost;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
        this.fromAddress = fromAddress;
    }

    public boolean isConfigured() {
        return mailSenderProvider.getIfAvailable() != null
                && StringUtils.hasText(smtpHost)
                && StringUtils.hasText(smtpUsername)
                && StringUtils.hasText(smtpPassword);
    }

    public void sendPasswordResetEmail(String to, String resetUrl) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("JavaMailSender is not available.");
        }
        if (!isConfigured()) {
            throw new IllegalStateException("SMTP is not configured.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setFrom(StringUtils.hasText(fromAddress) ? fromAddress : smtpUsername);
            helper.setSubject("Đặt lại mật khẩu TravelMate");
            helper.setText(plainText(resetUrl), htmlBody(resetUrl));
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Cannot build password reset email.", ex);
        }
    }

    public void sendBookingConfirmationEmail(Booking booking) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("JavaMailSender is not available.");
        }
        if (!isConfigured()) {
            throw new IllegalStateException("SMTP is not configured.");
        }
        if (booking == null) {
            throw new IllegalArgumentException("Booking is required.");
        }

        String to = StringUtils.hasText(booking.getCustomerEmail())
                ? booking.getCustomerEmail()
                : (booking.getUser() != null ? booking.getUser().getEmail() : null);
        if (!StringUtils.hasText(to)) {
            throw new IllegalArgumentException("Booking không có email người nhận.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setFrom(StringUtils.hasText(fromAddress) ? fromAddress : smtpUsername);
            helper.setSubject("TravelMate xác nhận đặt phòng " + booking.getBookingCode());
            helper.setText(buildBookingPlainText(booking), buildBookingHtmlBody(booking));
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Cannot build booking confirmation email.", ex);
        }
    }

    private String plainText(String resetUrl) {
        return """
                TravelMate nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.

                Mở liên kết sau để tạo mật khẩu mới:
                %s

                Liên kết chỉ sử dụng một lần và hết hạn sau 30 phút.
                Nếu bạn không yêu cầu thao tác này, hãy bỏ qua email.
                """.formatted(resetUrl);
    }

    private String htmlBody(String resetUrl) {
        return """
                <!doctype html>
                <html lang="vi">
                <body style="margin:0;background:#f4f6fb;font-family:Arial,sans-serif;color:#0f172a;">
                  <div style="max-width:560px;margin:0 auto;padding:28px 16px;">
                    <div style="background:#ffffff;border-radius:12px;padding:28px;border:1px solid #e5e7eb;">
                      <p style="margin:0 0 8px;color:#2563eb;font-weight:700;">TRAVELMATE</p>
                      <h1 style="font-size:22px;margin:0 0 12px;">Đặt lại mật khẩu</h1>
                      <p style="font-size:15px;line-height:1.6;margin:0 0 22px;">
                        TravelMate nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.
                      </p>
                      <p style="margin:0 0 24px;">
                        <a href="%s" style="display:inline-block;background:#0f172a;color:#ffffff;text-decoration:none;
                           padding:12px 18px;border-radius:8px;font-weight:700;">Tạo mật khẩu mới</a>
                      </p>
                      <p style="font-size:13px;line-height:1.6;color:#64748b;margin:0;">
                        Liên kết chỉ sử dụng một lần và hết hạn sau 30 phút. Nếu bạn không yêu cầu thao tác này,
                        hãy bỏ qua email.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(resetUrl);
    }

    private String buildBookingPlainText(Booking booking) {
        return """
                TravelMate đã xác nhận đặt phòng của bạn.

                Mã booking: %s
                Cơ sở: %s
                Hình thức thanh toán: %s
                Số tiền đã thanh toán online: %s VND
                Số tiền còn lại tại cơ sở: %s VND

                Phòng/căn của bạn đã được TravelMate giữ trên hệ thống.
                """.formatted(
                safeBookingCode(booking),
                safeAccommodationName(booking),
                booking.getPaymentOption() != null ? booking.getPaymentOption().name() : "N/A",
                formatMoney(booking.getPaidAmount()),
                formatMoney(booking.getRemainingAmount()));
    }

    private String buildBookingHtmlBody(Booking booking) {
        String paymentLabel = booking.getPaymentOption() == PaymentOption.DEPOSIT_30
                ? "Cọc 30% qua VNPAY"
                : "Thanh toán 100% qua VNPAY";
        return """
                <!doctype html>
                <html lang="vi">
                <body style="margin:0;background:#f4f6fb;font-family:Arial,sans-serif;color:#0f172a;">
                  <div style="max-width:600px;margin:0 auto;padding:28px 16px;">
                    <div style="background:#ffffff;border-radius:12px;padding:28px;border:1px solid #e5e7eb;">
                      <p style="margin:0 0 8px;color:#2563eb;font-weight:700;">TRAVELMATE</p>
                      <h1 style="font-size:22px;margin:0 0 12px;">Đặt phòng đã được xác nhận</h1>
                      <p style="font-size:15px;line-height:1.6;margin:0 0 18px;">
                        Chúng tôi đã ghi nhận thanh toán thành công cho booking <strong>%s</strong>.
                      </p>
                      <table style="width:100%%;border-collapse:collapse;font-size:14px;margin:0 0 20px;">
                        <tr><td style="padding:8px 0;color:#64748b;">Cơ sở</td><td style="padding:8px 0;font-weight:700;text-align:right;">%s</td></tr>
                        <tr><td style="padding:8px 0;color:#64748b;">Hình thức</td><td style="padding:8px 0;font-weight:700;text-align:right;">%s</td></tr>
                        <tr><td style="padding:8px 0;color:#64748b;">Đã thanh toán online</td><td style="padding:8px 0;font-weight:700;text-align:right;">%s VND</td></tr>
                        <tr><td style="padding:8px 0;color:#64748b;">Còn lại tại cơ sở</td><td style="padding:8px 0;font-weight:700;text-align:right;">%s VND</td></tr>
                      </table>
                      <p style="font-size:13px;line-height:1.6;color:#64748b;margin:0;">
                        Phòng/căn của bạn đã được TravelMate giữ trên hệ thống. Bạn có thể theo dõi trạng thái trong phần đặt phòng của bạn.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                safeBookingCode(booking),
                safeAccommodationName(booking),
                paymentLabel,
                formatMoney(booking.getPaidAmount()),
                formatMoney(booking.getRemainingAmount()));
    }

    private String safeBookingCode(Booking booking) {
        return booking != null && StringUtils.hasText(booking.getBookingCode())
                ? booking.getBookingCode() : "N/A";
    }

    private String safeAccommodationName(Booking booking) {
        return booking != null && booking.getAccommodation() != null
                && StringUtils.hasText(booking.getAccommodation().getName())
                ? booking.getAccommodation().getName() : "N/A";
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("%,.0f", amount != null ? amount : BigDecimal.ZERO);
    }
}
