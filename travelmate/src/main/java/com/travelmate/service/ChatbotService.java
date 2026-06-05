package com.travelmate.service;

import com.travelmate.dto.RoomAvailabilityDto;
import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Booking;
import com.travelmate.entity.Room;
import com.travelmate.entity.TravelPost;
import com.travelmate.entity.User;
import com.travelmate.entity.Voucher;
import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.entity.enums.DiscountType;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.entity.enums.VoucherScope;
import com.travelmate.repository.AccommodationRepository;
import com.travelmate.repository.BookingRepository;
import com.travelmate.repository.TravelPostRepository;
import com.travelmate.repository.UserRepository;
import com.travelmate.repository.VoucherRepository;
import com.travelmate.util.DestinationAliasUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.text.NumberFormat;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    private final AccommodationRepository accommodationRepository;
    private final TravelPostRepository travelPostRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final VoucherRepository voucherRepository;
    private final AvailabilityService availabilityService;

    @Value("${travelmate.chatbot.groq.enabled:true}")
    private boolean groqEnabled = true;

    @Value("${travelmate.chatbot.groq.api-key:}")
    private String groqApiKey = "";

    @Value("${travelmate.chatbot.groq.base-url:https://api.groq.com/openai/v1}")
    private String groqBaseUrl = "https://api.groq.com/openai/v1";

    @Value("${travelmate.chatbot.groq.model:llama-3.3-70b-versatile}")
    private String groqModel = "llama-3.3-70b-versatile";

    public ChatbotService(AccommodationRepository accommodationRepository,
                          TravelPostRepository travelPostRepository,
                          UserRepository userRepository,
                          BookingRepository bookingRepository,
                          VoucherRepository voucherRepository,
                          AvailabilityService availabilityService) {
        this.accommodationRepository = accommodationRepository;
        this.travelPostRepository = travelPostRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.voucherRepository = voucherRepository;
        this.availabilityService = availabilityService;
    }

    public record ChatbotResponse(String intent, String reply, List<String> quickReplies) {}

    private record TravelPreference(String key, String label, String icon,
                                    List<String> destinations, String note) {}
    private record GroqMessage(String role, String content) {}
    private record GroqChoice(GroqMessage message) {}
    private record GroqChatCompletion(List<GroqChoice> choices) {}
    private record StayDates(LocalDate checkIn, LocalDate checkOut, boolean suppliedByUser) {}
    private record AvailableRoomSuggestion(Accommodation accommodation, RoomAvailabilityDto room) {}

    // ── intent name constants ─────────────────────────────────
    private static final String I_GREETING         = "GREETING";
    private static final String I_MY_BOOKING       = "MY_BOOKING";
    private static final String I_BOOKING_GUIDE    = "BOOKING_GUIDE";
    private static final String I_BOOKING_POLICY   = "BOOKING_POLICY";
    private static final String I_PAYMENT          = "PAYMENT_POLICY";
    private static final String I_VOUCHER          = "VOUCHER_INFO";
    private static final String I_CONTACT          = "CONTACT_SUPPORT";
    private static final String I_FIND             = "FIND_ACCOMMODATION";
    private static final String I_TRAVEL           = "TRAVEL_SUGGESTION";
    private static final String I_OUT_OF_SCOPE     = "OUT_OF_SCOPE";
    private static final String I_FALLBACK         = "FALLBACK";
    private static final String I_BUDGET_PLAN       = "BUDGET_TRAVEL_PLAN";
    private static final String I_AI_BUSINESS       = "AI_BUSINESS";

    private static final NumberFormat VND_FORMAT =
            NumberFormat.getIntegerInstance(Locale.of("vi", "VN"));
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<String> DEFAULT_QR =
            List.of("Tìm khách sạn", "Gợi ý điểm đến", "Hướng dẫn đặt phòng", "Liên hệ hỗ trợ");
    private static final TravelPreference BEACH_PREF = new TravelPreference(
            "BEACH", "du lịch biển", "🏖️",
            List.of("Nha Trang", "Đà Nẵng", "Phú Quốc", "Vũng Tàu", "Mũi Né"),
            "hợp với lịch trình nghỉ dưỡng, tắm biển và ăn hải sản");
    private static final TravelPreference MOUNTAIN_PREF = new TravelPreference(
            "MOUNTAIN", "du lịch núi", "⛰️",
            List.of("Sa Pa", "Đà Lạt", "Hà Giang", "Ninh Bình"),
            "hợp với săn mây, trekking nhẹ và không khí mát mẻ");
    private static final TravelPreference RELAX_PREF = new TravelPreference(
            "RELAX", "nghỉ dưỡng", "🌿",
            List.of("Phú Quốc", "Nha Trang", "Đà Nẵng", "Đà Lạt"),
            "hợp với resort, khách sạn tiện nghi và lịch trình thư giãn");
    private static final TravelPreference FAMILY_PREF = new TravelPreference(
            "FAMILY", "du lịch gia đình", "👨‍👩‍👧‍👦",
            List.of("Đà Nẵng", "Đà Lạt", "Nha Trang", "Hội An", "Phú Quốc"),
            "ưu tiên nơi dễ di chuyển, nhiều lựa chọn phòng gia đình");
    private static final TravelPreference SAVING_PREF = new TravelPreference(
            "SAVING", "du lịch tiết kiệm", "💡",
            List.of("Đà Lạt", "Sa Pa", "Hội An", "Ninh Bình", "Mũi Né"),
            "dễ tìm homestay/khách sạn tầm trung và lịch trình gọn");

    // ── public entry point ────────────────────────────────────

    @Transactional(readOnly = true)
    public ChatbotResponse processMessage(String message, String username) {
        if (message == null || message.isBlank()) {
            return greeting(username);
        }

        String norm = DestinationAliasUtil.normalizeText(message);

        // Chỉ chặn các chủ đề rủi ro/không phù hợp rõ ràng. Các câu đời thường
        // sẽ được AI bẻ lái mềm sang nhu cầu du lịch, đặt phòng, đổi không khí.
        if (isHardOutOfScope(norm)) {
            return outOfScope();
        }

        // ── GREETING ──────────────────────────────────────────
        if (isGreeting(norm)) {
            return greeting(username);
        }

        // ── MY BOOKING ────────────────────────────────────────
        if (matches(norm, "booking cua toi", "dat phong cua toi", "don cua toi",
                "lich su dat", "xem don", "phong da dat", "don hang cua toi",
                "don gan nhat", "don moi nhat", "kiem tra don",
                "trang thai don", "don dang")) {
            return myBooking(username);
        }

        // ── BOOKING GUIDE ─────────────────────────────────────
        if (matches(norm, "huong dan dat", "cach dat phong", "dat phong nhu the nao",
                "huong dan dung", "cach su dung")) {
            return bookingGuide();
        }

        // ── BOOKING POLICY ────────────────────────────────────
        if (matches(norm, "chinh sach dat phong", "dat coc", "coc 30", "no show",
                "no-show", "huy dat phong", "huy don", "huy phong",
                "phi huy", "chinh sach huy", "quy dinh dat phong",
                "hoan tien", "mat coc", "co duoc hoan")) {
            return bookingPolicy();
        }

        // ── PAYMENT ───────────────────────────────────────────
        if (matches(norm, "thanh toan", "vnpay", "tra tien", "phuong thuc thanh toan",
                "cach thanh toan", "thanh toan 100")) {
            return paymentInfo();
        }

        // ── VOUCHER ───────────────────────────────────────────
        if (matches(norm, "voucher", "ma giam gia", "khuyen mai", "coupon",
                "giam gia", "nhap ma", "nhap voucher", "su dung voucher",
                "uu dai", "ma uu dai")) {
            return voucherInfo();
        }

        // ── CONTACT & REPORT ──────────────────────────────────
        if (matches(norm, "lien he", "ho tro", "support", "cham soc khach hang",
                "bao cao", "khieu nai", "to cao", "phan anh",
                "gui yeu cau", "gui phan hoi")) {
            return contactInfo();
        }

        // ── ALL ACCOMMODATIONS ────────────────────────────────
        if (matches(norm, "xem tat ca", "danh sach khach san", "tat ca khach san",
                "tat ca noi luu tru")) {
            return allHotels();
        }

        // ── BUDGET TRAVEL PLAN ────────────────────────────────
        // Ưu tiên ngân sách trước các intent tìm nơi lưu trú/gợi ý du lịch.
        Long budgetVnd = extractBudgetVnd(message);
        if (budgetVnd != null && budgetVnd >= 100_000L) {
            return budgetTravelPlan(message, norm, budgetVnd, username);
        }

        // ── PREFERENCE-BASED TRAVEL SUGGESTION ───────────────
        TravelPreference preference = extractTravelPreference(norm);
        if (preference != null
                && !matches(norm, "khach san", "tim phong", "book phong", "dat phong", "homestay", "villa")
                && matches(norm, "muon di", "di dau", "nen di", "goi y", "du lich", "bien", "nui",
                        "nghi duong", "gia dinh", "tiet kiem", "thu gian")) {
            return travelPreferenceSuggestion(preference);
        }

        // ── TRAVEL SUGGESTION ─────────────────────────────────
        if (matches(norm, "goi y", "kham pha", "diem den", "nen di", "nen tham",
                "diem du lich", "co gi choi", "choi gi", "tham quan",
                "an gi", "an o dau", "lich trinh", "kinh nghiem du lich")
                && !matches(norm, "khach san", "resort", "villa", "homestay")) {
            String dest = extractDestination(norm);
            if (dest != null && matches(norm, "lich trinh")) {
                return shortItinerary(dest, extractNights(norm));
            }
            return dest != null ? travelSuggestionForDest(dest) : travelDestinations();
        }

        // ── SEARCH BY PROPERTY TYPE ───────────────────────────
        if (matches(norm, "villa", "biet thu")) {
            String dest = extractDestination(norm);
            return dest != null ? searchAccommodations(dest, PropertyType.VILLA, message)
                    : askDestinationForType("Villa / Biệt thự");
        }
        if (matches(norm, "homestay")) {
            String dest = extractDestination(norm);
            return dest != null ? searchAccommodations(dest, PropertyType.HOMESTAY, message)
                    : askDestinationForType("Homestay");
        }
        if (matches(norm, "resort")) {
            String dest = extractDestination(norm);
            return dest != null ? searchAccommodations(dest, PropertyType.RESORT, message)
                    : askDestinationForType("Resort");
        }
        if (matches(norm, "khach san", "nha nghi", "dat phong", "tim phong", "book phong")) {
            String dest = extractDestination(norm);
            return dest != null ? searchAccommodations(dest, PropertyType.HOTEL, message) : askDestination();
        }

        // ── DESTINATION-ONLY MENTION ──────────────────────────
        String extractedDest = extractDestination(norm);
        if (extractedDest != null) {
            return searchAccommodations(extractedDest, null, message);
        }
        if (DestinationAliasUtil.isKnownDestination(message)) {
            return searchAccommodations(message, null, message);
        }

        Optional<ChatbotResponse> aiResponse = groqBusinessFallback(message, norm, username);
        if (aiResponse.isPresent()) {
            return aiResponse.get();
        }
        if (shouldRedirectToTravelMate(norm)) {
            return redirectToTravelMate(message, norm);
        }

        return fallback();
    }

    // ── intent handlers ──────────────────────────────────────

    private ChatbotResponse greeting(String username) {
        String displayName = resolveFirstNameFromEmail(username);
        String greetLine = (displayName != null)
                ? "👋 Chào mừng trở lại, <strong>" + esc(displayName) + "</strong>! Tôi là <strong>TravelBot</strong>."
                : "👋 Xin chào! Tôi là <strong>TravelBot</strong> — trợ lý du lịch của TravelMate.";
        String accountLine = (username != null)
                ? "<li>📦 Xem <strong>đặt phòng của bạn</strong> và trạng thái booking</li>"
                : "<li>🔑 <a href='/auth/login' class='bot-link'>Đăng nhập</a> để xem lịch sử đặt phòng</li>";
        String reply = "<div>"
                + "<p>" + greetLine + "</p>"
                + "<p>Tôi có thể giúp bạn:</p>"
                + "<ul>"
                + "<li>🏨 Tìm khách sạn, villa, homestay, resort theo điểm đến</li>"
                + "<li>💰 Tư vấn chuyến đi theo ngân sách, ví dụ: \"3 triệu đi Sa Pa\"</li>"
                + "<li>🗺️ Gợi ý điểm du lịch hấp dẫn</li>"
                + "<li>📋 Hướng dẫn đặt phòng &amp; chính sách cọc</li>"
                + "<li>🎫 Voucher &amp; ưu đãi hiện có</li>"
                + accountLine
                + "</ul>"
                + "<p>Bạn muốn tôi giúp gì hôm nay?</p>"
                + "</div>";
        List<String> qr = (username != null)
                ? List.of("Đặt phòng của tôi", "Tìm khách sạn", "Chính sách cọc 30%", "Voucher")
                : List.of("Tìm khách sạn", "Gợi ý điểm đến", "Chính sách cọc 30%", "Đặt phòng của tôi");
        return new ChatbotResponse(I_GREETING, reply, qr);
    }

    private ChatbotResponse myBooking(String username) {
        if (username == null) {
            String reply = "<div>"
                    + "<p>🔐 Bạn cần <strong>đăng nhập</strong> để xem lịch sử đặt phòng.</p>"
                    + "<p><a href='/auth/login' class='bot-link'>→ Đăng nhập ngay</a></p>"
                    + "<p>Chưa có tài khoản? <a href='/auth/register' class='bot-link'>Đăng ký miễn phí</a></p>"
                    + "</div>";
            return new ChatbotResponse(I_MY_BOOKING, reply,
                    List.of("Tìm khách sạn", "Hướng dẫn đặt phòng"));
        }

        Optional<User> userOpt = userRepository.findByEmail(username);
        if (userOpt.isEmpty()) return fallback();

        List<Booking> bookings = bookingRepository.findByUserOrderByCreatedAtDesc(userOpt.get());

        if (bookings.isEmpty()) {
            String reply = "<div>"
                    + "<p>📭 Bạn chưa có đặt phòng nào trên TravelMate.</p>"
                    + "<p>Hãy <a href='/accommodations' class='bot-link'>tìm khách sạn</a> và bắt đầu hành trình!</p>"
                    + "</div>";
            return new ChatbotResponse(I_MY_BOOKING, reply,
                    List.of("Tìm khách sạn", "Gợi ý điểm đến"));
        }

        List<Booking> recent = bookings.stream().limit(3).collect(Collectors.toList());
        StringBuilder sb = new StringBuilder("<div>");
        sb.append("<p>📦 <strong>Đặt phòng gần đây của bạn:</strong></p>");

        for (Booking b : recent) {
            sb.append("<div class='bot-booking-card'>");
            sb.append("<div class='bot-bk-row'>")
              .append("<span class='bot-bk-code'>").append(esc(b.getBookingCode())).append("</span>")
              .append(statusBadge(b.getBookingStatus()))
              .append("</div>");
            if (b.getAccommodation() != null) {
                sb.append("<p class='bot-bk-hotel'>🏨 ")
                  .append(esc(b.getAccommodation().getName())).append("</p>");
            }
            sb.append("<p class='bot-bk-dates'>📅 ")
              .append(b.getCheckIn().format(DATE_FMT))
              .append(" → ").append(b.getCheckOut().format(DATE_FMT)).append("</p>");
            sb.append("<p class='bot-bk-amount'>💰 <strong>")
              .append(fmtPrice(b.getTotalAmount().doubleValue())).append("</strong></p>");
            sb.append("<a href='/my-bookings' class='bot-link' style='font-size:.75rem;margin-top:2px'>Xem chi tiết →</a>");
            sb.append("</div>");
        }

        if (bookings.size() > 3) {
            sb.append("<a href='/my-bookings' class='bot-see-all'>Xem tất cả ")
              .append(bookings.size()).append(" đặt phòng →</a>");
        }
        sb.append("</div>");

        return new ChatbotResponse(I_MY_BOOKING, sb.toString(),
                List.of("Tìm khách sạn", "Chính sách cọc 30%", "Liên hệ hỗ trợ"));
    }

    private ChatbotResponse bookingGuide() {
        String reply = "<div>"
                + "<p>📋 <strong>Hướng dẫn đặt phòng TravelMate:</strong></p>"
                + "<ol>"
                + "<li>🔍 Nhập điểm đến, ngày check-in/out vào thanh tìm kiếm trang chủ</li>"
                + "<li>🏨 Chọn khách sạn phù hợp từ danh sách kết quả</li>"
                + "<li>🛏 Chọn loại phòng và số lượng phòng cần đặt</li>"
                + "<li>👤 Đăng nhập hoặc tạo tài khoản (nếu chưa có)</li>"
                + "<li>💳 Chọn <strong>đặt cọc 30%</strong> hoặc <strong>thanh toán 100%</strong> qua VNPAY</li>"
                + "<li>✅ Nhận mã đặt phòng và theo dõi trạng thái trong mục <strong>Đặt phòng của tôi</strong></li>"
                + "</ol>"
                + "<p>💡 <em>Xem lại đặt phòng tại mục \"Đặt phòng của tôi\" sau khi đăng nhập.</em></p>"
                + "</div>";
        return new ChatbotResponse(I_BOOKING_GUIDE, reply,
                List.of("Chính sách cọc 30%", "Thông tin thanh toán", "Liên hệ hỗ trợ"));
    }

    private ChatbotResponse bookingPolicy() {
        String reply = "<div>"
                + "<p>📋 <strong>Chính sách đặt phòng TravelMate:</strong></p>"
                + "<p><strong>💳 Hình thức thanh toán:</strong></p>"
                + "<ul>"
                + "<li><strong>Đặt cọc 30%</strong> qua VNPAY → đơn được ghi nhận và TravelMate giữ phòng/căn trên hệ thống</li>"
                + "<li>Thanh toán <strong>70% còn lại</strong> trực tiếp tại cơ sở khi nhận phòng</li>"
                + "<li>Hoặc <strong>thanh toán 100%</strong> trực tuyến để được ưu tiên xác nhận nhanh</li>"
                + "</ul>"
                + "<p><strong>🚫 Chính sách hủy &amp; no-show:</strong></p>"
                + "<ul>"
                + "<li><strong>Cọc 30%</strong>: nếu hủy hoặc không đến nhận phòng, tiền cọc không được hoàn lại</li>"
                + "<li><strong>Thanh toán 100%</strong>: hủy trước ngày check-in sẽ gửi yêu cầu hoàn dự kiến 70%; phí hủy 30%</li>"
                + "<li>Phòng/căn được mở lại sau khi ghi nhận hủy để khách khác có thể đặt</li>"
                + "</ul>"
                + "<p>📞 Cần hỗ trợ hủy đơn? <a href='/contact' class='bot-link'>Liên hệ ngay</a></p>"
                + "</div>";
        return new ChatbotResponse(I_BOOKING_POLICY, reply,
                List.of("Thông tin thanh toán", "Hướng dẫn đặt phòng", "Liên hệ hỗ trợ"));
    }

    private ChatbotResponse paymentInfo() {
        String reply = "<div>"
                + "<p>💳 <strong>Thanh toán TravelMate:</strong></p>"
                + "<ul>"
                + "<li>✅ Thanh toán trực tuyến qua <strong>VNPAY Sandbox</strong> trong bản demo</li>"
                + "<li>🏦 Hỗ trợ mô phỏng thanh toán bằng thẻ test do VNPAY cung cấp</li>"
                + "<li>🔒 Mã hóa SSL — bảo mật tuyệt đối</li>"
                + "<li>⏱ Sau khi thanh toán/cọc thành công, TravelMate tự xác nhận kết quả VNPAY và giữ phòng/căn trên hệ thống</li>"
                + "</ul>"
                + "<p>⚠️ Gặp sự cố? <a href='/contact' class='bot-link'>Liên hệ hỗ trợ ngay</a>.</p>"
                + "</div>";
        return new ChatbotResponse(I_PAYMENT, reply,
                List.of("Chính sách cọc 30%", "Hướng dẫn đặt phòng", "Liên hệ hỗ trợ"));
    }

    private ChatbotResponse voucherInfo() {
        List<Voucher> active = voucherRepository
                .findByVoucherScopeOrderByCreatedAtDesc(VoucherScope.USER_GLOBAL)
                .stream()
                .filter(Voucher::isCurrentlyValid)
                .limit(5)
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder("<div>");
        sb.append("<p>🎫 <strong>Voucher ưu đãi đang hoạt động:</strong></p>");

        if (active.isEmpty()) {
            sb.append("<p>Hiện chưa có voucher toàn sàn nào đang hoạt động.</p>")
              .append("<p>Kiểm tra lại sau hoặc hỏi đối tác về voucher riêng của cơ sở.</p>");
        } else {
            sb.append("<div class='bot-voucher-list'>");
            for (Voucher v : active) {
                String discountStr = (v.getDiscountType() == DiscountType.PERCENT)
                        ? "Giảm " + v.getDiscountValue().stripTrailingZeros().toPlainString() + "%"
                        + (v.getMaxDiscountAmount() != null
                                ? " (tối đa " + fmtPrice(v.getMaxDiscountAmount().doubleValue()) + ")"
                                : "")
                        : "Giảm " + fmtPrice(v.getDiscountValue().doubleValue());
                String expireStr = (v.getEndDate() != null)
                        ? "HSD: " + v.getEndDate().format(DATE_FMT) : "Không giới hạn";
                String minStr = (v.getMinOrderAmount() != null
                        && v.getMinOrderAmount().compareTo(java.math.BigDecimal.ZERO) > 0)
                        ? "Đơn tối thiểu: " + fmtPrice(v.getMinOrderAmount().doubleValue())
                        : null;
                sb.append("<div class='bot-voucher-item'>")
                  .append("<span class='bot-v-code'>").append(esc(v.getCode())).append("</span>");
                if (v.getName() != null && !v.getName().isBlank()) {
                    sb.append("<span class='bot-v-name'>").append(esc(v.getName())).append("</span>");
                }
                sb.append("<span class='bot-v-discount'>").append(esc(discountStr)).append("</span>");
                if (minStr != null) {
                    sb.append("<span class='bot-v-expire'>").append(esc(minStr)).append("</span>");
                }
                sb.append("<span class='bot-v-expire'>").append(esc(expireStr)).append("</span>")
                  .append("</div>");
            }
            sb.append("</div>");
            sb.append("<p style='font-size:.78rem;color:#475569;margin-top:6px'>"
                    + "💡 Nhập mã tại bước xác nhận đặt phòng để được giảm giá.</p>");
        }
        sb.append("<a href='/vouchers' class='bot-link' style='font-size:.78rem'>Xem tất cả voucher →</a>")
          .append("</div>");

        return new ChatbotResponse(I_VOUCHER, sb.toString(),
                List.of("Tìm khách sạn", "Hướng dẫn đặt phòng", "Liên hệ hỗ trợ"));
    }

    private ChatbotResponse contactInfo() {
        String reply = "<div>"
                + "<p>📞 <strong>Liên hệ &amp; Hỗ trợ TravelMate:</strong></p>"
                + "<ul>"
                + "<li>📧 Email: <strong>support@travelmate.vn</strong></li>"
                + "<li>☎️ Hotline: <strong>1800 6868</strong> (8:00–22:00 hằng ngày)</li>"
                + "<li>📝 Gửi yêu cầu / báo cáo cơ sở: <a href='/contact' class='bot-link'>Trang Liên hệ</a></li>"
                + "</ul>"
                + "<p>🕐 Phản hồi trong vòng <strong>2 giờ</strong> trong giờ làm việc.</p>"
                + "</div>";
        return new ChatbotResponse(I_CONTACT, reply,
                List.of("Tìm khách sạn", "Hướng dẫn đặt phòng", "Chính sách cọc 30%"));
    }

    private ChatbotResponse allHotels() {
        String reply = "<p>🏨 Xem toàn bộ danh sách nơi lưu trú trên TravelMate:</p>"
                + "<p><a href='/accommodations' class='bot-link'>→ Danh sách khách sạn &amp; villa</a></p>"
                + "<p>Hoặc nhập tên điểm đến để tôi tìm nhanh cho bạn!</p>";
        return new ChatbotResponse(I_FIND, reply,
                List.of("Đà Lạt", "Nha Trang", "Đà Nẵng", "Phú Quốc"));
    }

    private ChatbotResponse travelDestinations() {
        String reply = "<div>"
                + "<p>🗺️ <strong>Điểm đến nổi bật tại Việt Nam:</strong></p>"
                + "<div class='bot-dest-grid'>"
                + "<span class='bot-dest-chip'>🌸 Đà Lạt</span>"
                + "<span class='bot-dest-chip'>🏖️ Nha Trang</span>"
                + "<span class='bot-dest-chip'>🌊 Đà Nẵng</span>"
                + "<span class='bot-dest-chip'>🏮 Hội An</span>"
                + "<span class='bot-dest-chip'>⛰️ Sa Pa</span>"
                + "<span class='bot-dest-chip'>🏝️ Phú Quốc</span>"
                + "<span class='bot-dest-chip'>🏛️ Hà Nội</span>"
                + "<span class='bot-dest-chip'>🌆 TP. HCM</span>"
                + "<span class='bot-dest-chip'>🛥️ Hạ Long</span>"
                + "<span class='bot-dest-chip'>🌅 Vũng Tàu</span>"
                + "</div>"
                + "<p>Gõ tên điểm đến để tôi tìm khách sạn phù hợp!</p>"
                + "</div>";
        return new ChatbotResponse(I_TRAVEL, reply,
                List.of("Đà Lạt", "Nha Trang", "Phú Quốc", "Hội An"));
    }

    private ChatbotResponse travelSuggestionForDest(String destination) {
        String displayName = resolveDisplayName(destination);
        Set<String> slugs = DestinationAliasUtil.searchSlugs(destination);
        String slug = DestinationAliasUtil.normalizeSlug(destination);

        List<TravelPost> posts = slugs.isEmpty()
                ? travelPostRepository.findTop3ByDestinationSlugAndStatusOrderByCreatedAtDesc(
                        slug, TravelPost.Status.VISIBLE)
                : travelPostRepository.findTop3ByDestinationSlugInAndStatusOrderByCreatedAtDesc(
                        slugs, TravelPost.Status.VISIBLE);

        if (posts.isEmpty()) {
            return searchAccommodations(destination, null, destination);
        }

        StringBuilder sb = new StringBuilder("<div>");
        sb.append("<p>🗺️ <strong>Gợi ý du lịch ").append(esc(displayName)).append(":</strong></p>");
        for (TravelPost p : posts) {
            sb.append("<div class='bot-post-card'>");
            if (p.getThumbnailUrl() != null && !p.getThumbnailUrl().isBlank()) {
                sb.append("<img src='").append(p.getThumbnailUrl())
                  .append("' alt='' class='bot-post-img' loading='lazy'/>");
            }
            sb.append("<div class='bot-post-body'>")
              .append("<p class='bot-post-title'>").append(esc(p.getTitle())).append("</p>");
            if (p.getSummary() != null && !p.getSummary().isBlank()) {
                String s = p.getSummary();
                if (s.length() > 90) s = s.substring(0, 87) + "...";
                sb.append("<p class='bot-post-summary'>").append(esc(s)).append("</p>");
            }
            sb.append("<a href='").append(esc(p.getSourceUrl()))
              .append("' class='bot-link' target='_blank' rel='noopener'>Đọc thêm →</a>")
              .append("</div></div>");
        }
        sb.append("</div>");

        return new ChatbotResponse(I_TRAVEL, sb.toString(),
                List.of("Tìm khách sạn " + displayName, "Xem thêm bài viết", "Điểm đến khác"));
    }

    private ChatbotResponse travelPreferenceSuggestion(TravelPreference preference) {
        String firstDest = preference.destinations().get(0);
        StringBuilder sb = new StringBuilder("<div>");
        sb.append("<p>").append(preference.icon()).append(" Nếu bạn muốn ")
          .append("<strong>").append(esc(preference.label())).append("</strong>, TravelMate gợi ý:</p>")
          .append(destinationChips(preference.destinations()))
          .append("<p style='font-size:.8rem;color:#475569;margin-top:6px'>")
          .append(esc(preference.note())).append(".</p>")
          .append(botActionRow(firstDest));
        sb.append("</div>");

        return new ChatbotResponse(I_TRAVEL, sb.toString(),
                List.of("Tôi có 3 triệu", "Xem nơi lưu trú " + firstDest,
                        "Xem gợi ý du lịch", "Xem voucher"));
    }

    private ChatbotResponse shortItinerary(String destination, int nights) {
        String displayName = resolveDisplayName(destination);
        int safeNights = Math.max(1, nights);
        int days = safeNights + 1;
        TravelPreference preference = preferenceForDestination(displayName);

        StringBuilder sb = new StringBuilder("<div>");
        sb.append("<p>🗓️ <strong>Lịch trình tham khảo ")
          .append(esc(displayName)).append(" ")
          .append(days).append(" ngày ").append(safeNights).append(" đêm:</strong></p>")
          .append("<ol>")
          .append("<li>Ngày 1: di chuyển, nhận phòng, đi dạo khu trung tâm và ăn tối nhẹ.</li>");
        if (days >= 3) {
            sb.append("<li>Ngày 2: tham quan điểm nổi bật, chụp ảnh, thử món địa phương.</li>")
              .append("<li>Ngày ").append(days)
              .append(": nghỉ ngơi, mua quà, trả phòng và về lại.</li>");
        } else {
            sb.append("<li>Ngày 2: tham quan điểm nổi bật, mua quà, trả phòng và về lại.</li>");
        }
        sb.append("</ol>")
          .append("<p style='font-size:.8rem;color:#475569'>TravelMate gợi ý lịch trình ngắn để bạn dễ demo. Chi phí vé xe/máy bay chưa nằm trong hệ thống nên mình không cam kết phần đó.</p>");
        if (preference != null) {
            sb.append("<p style='font-size:.8rem;color:#475569'>Phong cách phù hợp: ")
              .append(preference.icon()).append(" ").append(esc(preference.label()))
              .append(" - ").append(esc(preference.note())).append(".</p>");
        }
        sb.append(botActionRow(displayName)).append("</div>");

        return new ChatbotResponse(I_TRAVEL, sb.toString(),
                List.of("Tìm khách sạn " + displayName, "Tôi có 3 triệu",
                        "Xem voucher", "Điểm đến khác"));
    }

    private ChatbotResponse searchAccommodations(String destination, PropertyType propertyType, String queryText) {
        String displayName = resolveDisplayName(destination);
        String typeLabel = typeLabel(propertyType);
        String typeParam = (propertyType != null) ? "&type=" + propertyType.name() : "";
        String norm = DestinationAliasUtil.normalizeText(queryText);
        int guests = extractGuests(norm);
        int rooms = extractRooms(norm);
        StayDates dates = extractStayDates(queryText, extractNights(norm));

        List<Accommodation> pool = (propertyType != null)
                ? accommodationRepository.findByPropertyTypeAndApprovalStatus(
                        propertyType, ApprovalStatus.APPROVED)
                : accommodationRepository.findByApprovalStatusOrderByCreatedAtDesc(
                        ApprovalStatus.APPROVED);

        List<Accommodation> matching = pool.stream()
                .filter(a -> DestinationAliasUtil.matchesTextOrDestination(a.getCity(), destination)
                          || DestinationAliasUtil.matchesTextOrDestination(a.getName(), destination))
                .collect(Collectors.toList());

        if (matching.isEmpty()) {
            String reply = "<p>😔 Chưa có <strong>" + esc(typeLabel) + "</strong> nào ở <strong>"
                    + esc(displayName) + "</strong> trong hệ thống.</p>"
                    + "<p><a href='/accommodations?keyword=" + encUrl(displayName) + typeParam
                    + "' class='bot-link'>Tìm kiếm trên trang danh sách</a> hoặc chọn điểm đến khác.</p>";
            return new ChatbotResponse(I_FIND, reply,
                    List.of("Đà Lạt", "Nha Trang", "Phú Quốc", "Xem tất cả"));
        }

        List<AvailableRoomSuggestion> available = findAvailableRooms(
                matching, dates, rooms, guests, 0L).stream().limit(5).toList();
        if (available.isEmpty()) {
            String reply = "<p>Hiện chưa còn <strong>" + esc(typeLabel) + "</strong> phù hợp tại <strong>"
                    + esc(displayName) + "</strong> cho kỳ " + dates.checkIn() + " đến " + dates.checkOut() + ".</p>"
                    + "<p><a href='/accommodations?keyword=" + encUrl(displayName) + typeParam
                    + "' class='bot-link'>Xem thêm lựa chọn</a> hoặc đổi ngày lưu trú.</p>";
            return new ChatbotResponse(I_FIND, reply,
                    List.of("Đà Lạt", "Nha Trang", "Phú Quốc", "Xem tất cả"));
        }

        StringBuilder sb = new StringBuilder("<div class='bot-hotel-results'>");
        sb.append("<p>🏨 Phòng/căn đang còn trống tại <strong>").append(esc(displayName)).append("</strong>:</p>")
          .append("<p class='bot-budget-note'>Kiểm tra cho <strong>")
          .append(dates.checkIn()).append(" - ").append(dates.checkOut())
          .append("</strong>, ").append(guests).append(" khách, ").append(rooms).append(" phòng")
          .append(dates.suppliedByUser() ? ".</p>" : " (ngày mặc định gần nhất; bạn có thể nhập ngày cụ thể).</p>");

        for (AvailableRoomSuggestion suggestion : available) {
            sb.append(roomCard(suggestion, dates, guests, rooms));
        }

        sb.append("<a href='/accommodations?keyword=").append(encUrl(displayName)).append(typeParam)
          .append("' class='bot-see-all'>Xem tất cả ").append(esc(typeLabel)).append(" →</a>")
          .append("</div>");

        return new ChatbotResponse(I_FIND, sb.toString(),
                List.of("Gợi ý du lịch " + displayName, "Tìm điểm đến khác", "Hướng dẫn đặt phòng"));
    }

    private ChatbotResponse askDestination() {
        String reply = "<p>🏨 Bạn muốn tìm <strong>khách sạn</strong> ở đâu?</p>"
                + "<p>Nhập tên thành phố hoặc điểm đến bạn muốn đặt phòng nhé!</p>";
        return new ChatbotResponse(I_FIND, reply,
                List.of("Đà Lạt", "Nha Trang", "Đà Nẵng", "Phú Quốc"));
    }

    private ChatbotResponse askDestinationForType(String typeDisplay) {
        String reply = "<p>🏡 Bạn muốn tìm <strong>" + esc(typeDisplay) + "</strong> ở đâu?</p>"
                + "<p>Nhập tên thành phố hoặc điểm đến nhé!</p>";
        return new ChatbotResponse(I_FIND, reply,
                List.of("Đà Lạt", "Nha Trang", "Đà Nẵng", "Phú Quốc"));
    }

    private ChatbotResponse outOfScope() {
        String suggest = "Đà Lạt";
        String reply = "<div>"
                + "<p>🤖 Câu này nằm ngoài phạm vi đồ án, nên mình sẽ kéo về <strong>du lịch và đặt phòng trên TravelMate</strong>.</p>"
                + "<p>Nếu bạn muốn đổi không khí, mình có thể gợi ý <strong>Đà Lạt</strong>, "
                + "<strong>Sa Pa</strong>, <strong>Hội An</strong> hoặc <strong>Nha Trang</strong> theo ngân sách của bạn.</p>"
                + botActionRow(suggest)
                + "</div>";
        return new ChatbotResponse(I_OUT_OF_SCOPE, reply,
                List.of("Tôi có 3 triệu", "Gợi ý " + suggest, "Tìm homestay yên tĩnh", "Liên hệ hỗ trợ"));
    }

    private ChatbotResponse fallback() {
        String reply = "<p>🤔 Tôi chưa hiểu câu hỏi của bạn. Bạn có thể:</p><ul>"
                + "<li>Nhập tên <strong>điểm đến</strong> (VD: \"Đà Lạt\", \"Nha Trang\")</li>"
                + "<li>Hỏi về <strong>đặt phòng</strong>, <strong>chính sách</strong>, <strong>voucher</strong></li>"
                + "<li>Nhấn các nút gợi ý bên dưới</li>"
                + "</ul>";
        return new ChatbotResponse(I_FALLBACK, reply, DEFAULT_QR);
    }

    private ChatbotResponse redirectToTravelMate(String message, String norm) {
        String lead;
        String primaryDestination;
        List<String> destinations;

        if (isCurrentNewsQuestion(norm)) {
            primaryDestination = "Đà Lạt";
            destinations = List.of("Đà Lạt", "Sa Pa", "Hội An", "Nha Trang");
            lead = "Mình không cập nhật thời sự theo thời gian thực trong TravelMate. Nếu bạn muốn đổi chủ đề thành một chuyến đi, TravelMate có thể gợi ý các điểm đến dễ demo và dễ đặt phòng.";
        } else if (isFoodContext(norm)) {
            primaryDestination = "Hội An";
            destinations = List.of("Hội An", "Đà Nẵng", "TP. HCM", "Nha Trang");
            lead = "Nếu bạn đang đói hoặc muốn đi ăn ngon, mình sẽ kéo về du lịch ẩm thực: ưu tiên nơi lưu trú gần phố đi bộ, chợ đêm hoặc khu trung tâm để tiện khám phá.";
        } else if (isTravelMoodCandidate(norm)) {
            primaryDestination = "Đà Lạt";
            destinations = List.of("Đà Lạt", "Hội An", "Đà Nẵng", "Nha Trang", "Vũng Tàu");
            lead = "Nếu bạn muốn đổi không khí, TravelMate có thể gợi ý điểm đến nhẹ nhàng, nhiều quán cafe, photowalk và hoạt động nhóm. Mình không tư vấn chuyện tình cảm sâu, chỉ kéo về lựa chọn du lịch phù hợp.";
        } else if (isRestContext(norm)) {
            primaryDestination = "Phú Quốc";
            destinations = List.of("Phú Quốc", "Đà Lạt", "Sa Pa", "Nha Trang");
            lead = "Nếu bạn đang mệt, stress hoặc cần nghỉ ngơi, mình sẽ chuyển thành nhu cầu nghỉ dưỡng: ưu tiên resort, homestay yên tĩnh hoặc phòng có view đẹp trên TravelMate.";
        } else if (isWeatherContext(norm)) {
            primaryDestination = "Đà Lạt";
            destinations = List.of("Đà Lạt", "Sa Pa", "Nha Trang", "Đà Nẵng");
            lead = "TravelMate chưa có dự báo thời tiết trực tiếp, nhưng mình có thể bẻ câu này thành gợi ý điểm đến: trời nóng thì đi biển, muốn mát mẻ thì chọn Đà Lạt hoặc Sa Pa.";
        } else {
            primaryDestination = "Đà Lạt";
            destinations = List.of("Đà Lạt", "Sa Pa", "Hội An", "Nha Trang");
            lead = "Mình sẽ kéo câu này về TravelMate: nếu bạn muốn đổi không khí, mình có thể gợi ý điểm đến, loại nơi lưu trú và cách đặt phòng theo ngân sách của bạn.";
        }

        String reply = "<div class='bot-ai-response'>"
                + "<p>" + esc(lead) + "</p>"
                + destinationChips(destinations)
                + "<p>Trên TravelMate, bạn có thể lọc khách sạn, homestay, villa hoặc resort theo điểm đến, ngày nhận - trả phòng và dùng voucher nếu có.</p>"
                + botActionRow(primaryDestination)
                + "</div>";
        return new ChatbotResponse(I_AI_BUSINESS, reply,
                List.of("Gợi ý " + primaryDestination, "Tìm khách sạn " + primaryDestination,
                        "Xem voucher", "Hướng dẫn đặt phòng"));
    }

    private Optional<ChatbotResponse> groqBusinessFallback(String message, String norm, String username) {
        if (!groqEnabled || isBlank(groqApiKey) || !isBusinessFallbackCandidate(message, norm)) {
            return Optional.empty();
        }

        String context = buildGroqBusinessContext(message, norm, username);
        try {
            GroqChatCompletion completion = RestClient.builder()
                    .baseUrl(trimTrailingSlash(groqBaseUrl))
                    .defaultHeader("Authorization", "Bearer " + groqApiKey.trim())
                    .defaultHeader("Content-Type", "application/json")
                    .build()
                    .post()
                    .uri("/chat/completions")
                    .body(Map.of(
                            "model", groqModel,
                            "temperature", 0.1,
                            "top_p", 0.7,
                            "max_completion_tokens", 260,
                            "messages", List.of(
                                    Map.of("role", "system", "content", groqSystemPrompt()),
                                    Map.of("role", "user", "content",
                                            "Câu hỏi của khách:\n" + message
                                                    + "\n\nDữ liệu TravelMate được phép dùng:\n" + context)
                            )
                    ))
                    .retrieve()
                    .body(GroqChatCompletion.class);

            String content = normalizeKnownVietnameseFallbacks(extractGroqContent(completion));
            if (isBlank(content)) {
                return Optional.empty();
            }
            if (shouldRedirectToTravelMate(norm) && isMissingTravelMateDataReply(content)) {
                return Optional.of(redirectToTravelMate(message, norm));
            }

            return Optional.of(new ChatbotResponse(
                    I_AI_BUSINESS,
                    formatAiReply(content),
                    List.of("Tìm khách sạn", "Hướng dẫn đặt phòng", "Voucher", "Liên hệ hỗ trợ")
            ));
        } catch (RestClientException | IllegalArgumentException ex) {
            log.warn("Groq chatbot fallback failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String groqSystemPrompt() {
        return """
                Bạn là TravelBot của TravelMate.
                Quy tắc bắt buộc:
                - Luôn trả lời bằng tiếng Việt có dấu chuẩn Unicode, tối đa 80 từ. Không dùng tiếng Việt không dấu.
                - Chỉ dùng thông tin trong phần "Dữ liệu TravelMate được phép dùng".
                - Không bịa giá, chính sách, địa điểm, voucher, hotline, link hay trạng thái đặt phòng.
                - Nếu thiếu dữ liệu TravelMate cho câu hỏi đúng phạm vi, nói ngắn gọn: "Mình chưa có dữ liệu TravelMate để trả lời phần này."
                - Nếu tin nhắn đời thường hoặc lạc đề, không trả lời trực tiếp chủ đề đó; hãy suy ra tình cảnh của khách rồi bẻ lái sang gợi ý điểm đến, hoạt động du lịch, loại nơi lưu trú, voucher hoặc cách đặt phòng trên TravelMate.
                - Ví dụ: đói bụng thì gợi ý điểm đến hợp ẩm thực/phố đi bộ/chợ đêm; mệt, stress thì gợi ý nghỉ dưỡng; buồn, cô đơn, thất tình thì gợi ý đổi không khí, cafe, photowalk, hoạt động nhóm.
                - Không hứa hẹn sẽ tìm được người yêu, không tư vấn tình cảm sâu, không đưa lời khuyên y tế/pháp lý/tài chính/lập trình.
                - Không trả lời lan man, không markdown, không HTML.
                """;
    }

    private String buildGroqBusinessContext(String message, String norm, String username) {
        String destination = extractDestination(norm);
        TravelPreference preference = extractTravelPreference(norm);
        PropertyType propertyType = extractPropertyTypePreference(norm);

        StringBuilder sb = new StringBuilder();
        sb.append("Phạm vi chatbot: tìm nơi lưu trú, gợi ý điểm đến trong TravelMate, tư vấn ngân sách dựa trên giá phòng trong hệ thống, hướng dẫn đặt phòng, thanh toán, voucher, lịch sử đặt phòng khi đã đăng nhập, liên hệ hỗ trợ.\n");
        sb.append("Tin nhắn lạc đề vẫn được xử lý bằng cách suy ra tình cảnh và nói về du lịch/lưu trú/hoạt động trong TravelMate, không trả lời trực tiếp ngoài phạm vi.\n");
        sb.append("Không hỗ trợ: vé máy bay/xe, giá ngoài hệ thống, nhà hàng ngoài dữ liệu, y tế, pháp lý, tài chính, lập trình, chính trị.\n");
        sb.append("Loại hình lưu trú: khách sạn, villa, homestay, resort.\n");
        if (isContextualTravelBridgeCandidate(norm)) {
            sb.append("Bảng gợi ý theo tình cảnh:\n");
            sb.append("- Đói bụng/thèm ăn: Hội An, Đà Nẵng, Nha Trang, TP. HCM; ưu tiên lưu trú gần trung tâm, phố đi bộ, chợ đêm, khu ẩm thực.\n");
            sb.append("- Mệt/stress/cần nghỉ: Đà Lạt, Phú Quốc, Nha Trang, Sa Pa; ưu tiên resort, homestay yên tĩnh, phòng có view.\n");
            sb.append("- Buồn/cô đơn/thất tình/muốn gặp người mới: Đà Lạt, Hội An, Đà Nẵng, Nha Trang, Vũng Tàu; gợi ý cafe, photowalk, biển, tour/hoạt động nhóm; không cam kết tìm được người yêu.\n");
            sb.append("- Nóng bức: biển như Nha Trang, Đà Nẵng, Phú Quốc, Vũng Tàu. Mưa lạnh: Đà Lạt, Sa Pa, cafe/khách sạn gần trung tâm.\n");
            sb.append("- Ăn mừng/sinh nhật/đi cùng bạn bè: Đà Nẵng, Nha Trang, TP. HCM, Hội An; ưu tiên villa/homestay/khách sạn gần khu vui chơi.\n");
        }
        sb.append("Quy trình đặt phòng: tìm điểm đến và ngày ở, chọn nơi lưu trú/phòng, đăng nhập hoặc đăng ký, chọn đặt cọc 30% hoặc thanh toán 100% qua VNPAY, theo dõi trong Đặt phòng của tôi.\n");
        sb.append("Thanh toán: demo dùng VNPAY Sandbox. Đặt cọc 30% qua VNPAY, 70% còn lại thanh toán trực tiếp tại cơ sở khi nhận phòng. Thanh toán 100% trực tuyến được ưu tiên xác nhận nhanh.\n");
        sb.append("Hủy/no-show: đặt cọc 30% đã thanh toán nếu hủy hoặc no-show thì mất toàn bộ tiền cọc. Thanh toán 100% nếu hủy trước check-in thì dự kiến hoàn 70% và giữ 30% phí hủy. Hệ thống mở lại quota sau khi ghi nhận hủy.\n");
        sb.append("Hỗ trợ: email support@travelmate.vn, hotline 1800 6868 từ 8:00 đến 22:00, trang /contact.\n");
        sb.append(username == null
                ? "Người dùng chưa đăng nhập, không được nói có thể xem lịch sử đặt phòng trực tiếp nếu chưa đăng nhập.\n"
                : "Người dùng đã đăng nhập, có thể hướng dẫn xem Đặt phòng của tôi.\n");

        appendGroqAccommodationContext(sb, destination, preference, propertyType);
        appendGroqTravelPostContext(sb, destination);
        appendGroqVoucherContext(sb);
        return sb.toString();
    }

    private void appendGroqAccommodationContext(StringBuilder sb, String destination,
                                                TravelPreference preference, PropertyType propertyType) {
        List<Accommodation> pool = (propertyType != null)
                ? accommodationRepository.findByPropertyTypeAndApprovalStatus(propertyType, ApprovalStatus.APPROVED)
                : accommodationRepository.findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus.APPROVED);

        List<Accommodation> matches = pool.stream()
                .filter(a -> matchesDestinationOrPreference(a, destination, preference))
                .filter(a -> a.getName() != null && !a.getName().isBlank())
                .limit(5)
                .collect(Collectors.toList());

        if (matches.isEmpty()) {
            sb.append("Nơi lưu trú phù hợp trong DB: chưa có kết quả rõ ràng.\n");
            return;
        }

        sb.append("Nơi lưu trú phù hợp trong DB:\n");
        for (Accommodation a : matches) {
            sb.append("- ").append(a.getName());
            if (a.getCity() != null) sb.append(", ").append(a.getCity());
            if (a.getPropertyType() != null) sb.append(", loại ").append(typeLabel(a.getPropertyType()));
            if (a.getMinPrice() != null) sb.append(", từ ").append(fmtPrice(a.getMinPrice())).append("/đêm");
            if (a.getRating() != null) sb.append(", điểm ").append(String.format("%.1f", a.getRating()));
            sb.append(", link /accommodations/").append(a.getId()).append("\n");
        }
    }

    private void appendGroqTravelPostContext(StringBuilder sb, String destination) {
        if (destination == null || destination.isBlank()) {
            return;
        }

        Set<String> slugs = DestinationAliasUtil.searchSlugs(destination);
        String slug = DestinationAliasUtil.normalizeSlug(destination);
        List<TravelPost> posts = slugs.isEmpty()
                ? travelPostRepository.findTop3ByDestinationSlugAndStatusOrderByCreatedAtDesc(
                        slug, TravelPost.Status.VISIBLE)
                : travelPostRepository.findTop3ByDestinationSlugInAndStatusOrderByCreatedAtDesc(
                        slugs, TravelPost.Status.VISIBLE);

        if (posts.isEmpty()) {
            return;
        }

        sb.append("Bài gợi ý du lịch trong DB:\n");
        for (TravelPost post : posts) {
            sb.append("- ").append(post.getTitle());
            if (post.getSummary() != null && !post.getSummary().isBlank()) {
                sb.append(": ").append(trimTo(post.getSummary(), 140));
            }
            if (post.getSourceUrl() != null && !post.getSourceUrl().isBlank()) {
                sb.append(" (").append(post.getSourceUrl()).append(")");
            }
            sb.append("\n");
        }
    }

    private void appendGroqVoucherContext(StringBuilder sb) {
        List<Voucher> active = voucherRepository
                .findByVoucherScopeOrderByCreatedAtDesc(VoucherScope.USER_GLOBAL)
                .stream()
                .filter(Voucher::isCurrentlyValid)
                .limit(5)
                .collect(Collectors.toList());

        if (active.isEmpty()) {
            sb.append("Voucher toàn sàn đang hoạt động: chưa có voucher hợp lệ.\n");
            return;
        }

        sb.append("Voucher toàn sàn đang hoạt động:\n");
        for (Voucher v : active) {
            sb.append("- Mã ").append(v.getCode());
            if (v.getName() != null && !v.getName().isBlank()) sb.append(", ").append(v.getName());
            if (v.getDiscountType() == DiscountType.PERCENT) {
                sb.append(", giảm ").append(v.getDiscountValue().stripTrailingZeros().toPlainString()).append("%");
                if (v.getMaxDiscountAmount() != null) {
                    sb.append(" tối đa ").append(fmtPrice(v.getMaxDiscountAmount().doubleValue()));
                }
            } else {
                sb.append(", giảm ").append(fmtPrice(v.getDiscountValue().doubleValue()));
            }
            if (v.getEndDate() != null) sb.append(", HSD ").append(v.getEndDate().format(DATE_FMT));
            sb.append("\n");
        }
    }

    private boolean isBusinessFallbackCandidate(String message, String norm) {
        return !isBlank(message)
                && !isHardOutOfScope(norm)
                && (extractDestination(norm) != null
                || DestinationAliasUtil.isKnownDestination(message)
                || extractTravelPreference(norm) != null
                || isContextualTravelBridgeCandidate(norm)
                || matches(norm,
                        "travelmate", "du lich", "di choi", "di dau", "lich trinh", "diem den",
                        "khach san", "homestay", "villa", "resort", "noi luu tru", "phong",
                        "dat phong", "booking", "check in", "check-in", "check out", "check-out",
                        "nhan phong", "tra phong", "doi lich", "doi ngay", "huy phong", "huy don",
                        "huy booking", "no show",
                        "thanh toan", "dat coc", "vnpay", "hoan tien", "voucher", "ma giam gia",
                        "uu dai", "tai khoan", "dang nhap", "dang ky", "ho tro", "lien he",
                        "gia phong", "gia tien", "bang gia", "gia re", "chi phi", "ngan sach")
                || norm.length() >= 3);
    }

    private String extractGroqContent(GroqChatCompletion completion) {
        if (completion == null || completion.choices() == null || completion.choices().isEmpty()) {
            return null;
        }
        GroqChoice choice = completion.choices().get(0);
        return choice == null || choice.message() == null ? null : choice.message().content();
    }

    private String formatAiReply(String content) {
        String normalized = normalizeKnownVietnameseFallbacks(content);
        if (isBlank(normalized)) {
            return fallback().reply();
        }
        String text = trimTo(normalized.replace("\r", "").trim(), 700);
        if (isBlank(text)) {
            return fallback().reply();
        }
        String escaped = esc(text);
        String html = escaped.replaceAll("\\n{2,}", "</p><p>")
                .replace("\n", "<br>");
        return "<div class='bot-ai-response'><p>" + html + "</p></div>";
    }

    private String normalizeKnownVietnameseFallbacks(String content) {
        if (content == null) {
            return null;
        }
        String normalized = content
                .replace("Minh chua co du lieu TravelMate de tra loi phan nay.",
                        "Mình chưa có dữ liệu TravelMate để trả lời phần này.")
                .replace("Minh chua co du lieu TravelMate de tra loi phan nay",
                        "Mình chưa có dữ liệu TravelMate để trả lời phần này")
                .replace("Minh chua co du lieu de tra loi phan nay.",
                        "Mình chưa có dữ liệu để trả lời phần này.")
                .replace("Minh chua co du lieu de tra loi phan nay",
                        "Mình chưa có dữ liệu để trả lời phần này");
        normalized = replaceKnownTerm(normalized, "Da Lat", "Đà Lạt");
        normalized = replaceKnownTerm(normalized, "Dalat", "Đà Lạt");
        normalized = replaceKnownTerm(normalized, "Da Nang", "Đà Nẵng");
        normalized = replaceKnownTerm(normalized, "Danang", "Đà Nẵng");
        normalized = replaceKnownTerm(normalized, "Hoi An", "Hội An");
        normalized = replaceKnownTerm(normalized, "Hoian", "Hội An");
        normalized = replaceKnownTerm(normalized, "Phu Quoc", "Phú Quốc");
        normalized = replaceKnownTerm(normalized, "Phuquoc", "Phú Quốc");
        normalized = replaceKnownTerm(normalized, "Vung Tau", "Vũng Tàu");
        normalized = replaceKnownTerm(normalized, "Vungtau", "Vũng Tàu");
        normalized = replaceKnownTerm(normalized, "Mui Ne", "Mũi Né");
        normalized = replaceKnownTerm(normalized, "Muine", "Mũi Né");
        normalized = replaceKnownTerm(normalized, "Can Tho", "Cần Thơ");
        normalized = replaceKnownTerm(normalized, "Cantho", "Cần Thơ");
        normalized = replaceKnownTerm(normalized, "Ha Noi", "Hà Nội");
        normalized = replaceKnownTerm(normalized, "Hanoi", "Hà Nội");
        normalized = replaceKnownTerm(normalized, "Ha Long", "Hạ Long");
        normalized = replaceKnownTerm(normalized, "Halong", "Hạ Long");
        normalized = replaceKnownTerm(normalized, "Ha Giang", "Hà Giang");
        normalized = replaceKnownTerm(normalized, "Hagiang", "Hà Giang");
        normalized = replaceKnownTerm(normalized, "Ninh Binh", "Ninh Bình");
        normalized = replaceKnownTerm(normalized, "Ninhbinh", "Ninh Bình");
        normalized = replaceKnownTerm(normalized, "Quang Ninh", "Quảng Ninh");
        normalized = replaceKnownTerm(normalized, "Quangninh", "Quảng Ninh");
        normalized = replaceKnownTerm(normalized, "Yen Bai", "Yên Bái");
        normalized = replaceKnownTerm(normalized, "Yenbai", "Yên Bái");
        normalized = replaceKnownTerm(normalized, "Lao Cai", "Lào Cai");
        normalized = replaceKnownTerm(normalized, "Laocai", "Lào Cai");
        normalized = replaceKnownTerm(normalized, "Sapa", "Sa Pa");
        normalized = replaceKnownTerm(normalized, "khach san", "khách sạn");
        normalized = replaceKnownTerm(normalized, "luu tru", "lưu trú");
        normalized = replaceKnownTerm(normalized, "nghi duong", "nghỉ dưỡng");
        normalized = replaceKnownTerm(normalized, "yen tinh", "yên tĩnh");
        normalized = replaceKnownTerm(normalized, "diem den", "điểm đến");
        normalized = replaceKnownTerm(normalized, "du lich", "du lịch");
        normalized = replaceKnownTerm(normalized, "dat phong", "đặt phòng");
        normalized = replaceKnownTerm(normalized, "goi y", "gợi ý");
        normalized = replaceKnownTerm(normalized, "am thuc", "ẩm thực");
        normalized = replaceKnownTerm(normalized, "pho di bo", "phố đi bộ");
        normalized = replaceKnownTerm(normalized, "cho dem", "chợ đêm");
        normalized = replaceKnownTerm(normalized, "doi khong khi", "đổi không khí");
        return normalized;
    }

    private String replaceKnownTerm(String content, String source, String replacement) {
        return Pattern.compile("(?iu)(?<!\\p{L})" + Pattern.quote(source) + "(?!\\p{L})")
                .matcher(content)
                .replaceAll(Matcher.quoteReplacement(replacement));
    }

    private boolean isMissingTravelMateDataReply(String content) {
        if (isBlank(content)) {
            return false;
        }
        String norm = DestinationAliasUtil.normalizeText(content);
        return norm.contains("minh chua co du lieu travelmate")
                || norm.contains("chua co du lieu travelmate")
                || norm.contains("khong co du lieu travelmate")
                || norm.contains("chua co thong tin travelmate")
                || norm.contains("khong co thong tin travelmate")
                || norm.contains("khong du du lieu")
                || norm.contains("du lieu khong du");
    }

    private String trimTo(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimTrailingSlash(String value) {
        if (isBlank(value)) {
            return "https://api.groq.com/openai/v1";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    // ── helpers ──────────────────────────────────────────────


    private boolean matches(String norm, String... keywords) {
        for (String kw : keywords) {
            if (norm.contains(kw)) return true;
        }
        return false;
    }

    private boolean isGreeting(String norm) {
        String padded = " " + norm + " ";
        return matches(padded,
                " xin chao ", " chao ban ", " hello ", " helo ", " hi ", " hey ",
                " chao ", " alo ", " halo ", " hallo ");
    }

    private boolean isTravelMoodCandidate(String norm) {
        return matches(norm,
                "that tinh", "buon tinh", "chia tay", "co don", "doc than",
                "tim nguoi yeu", "tim tinh yeu", "muon co nguoi yeu",
                "muon yeu", "gap nguoi moi", "hen ho", "crush");
    }

    private boolean isContextualTravelBridgeCandidate(String norm) {
        return isTravelMoodCandidate(norm)
                || matches(norm,
                        "toi doi", "dang doi", "doi bung", "them an", "muon an", "an gi", "do an", "am thuc",
                        "met", "stress", "ap luc", "cang thang", "can nghi", "muon nghi",
                        "chan", "buon", "doi gio", "doi khong khi", "khong biet lam gi",
                        "hom nay toi", "toi dang", "toi muon", "cuoi tuan",
                        "nong", "lanh", "mua", "nang", "ngu khong ngon",
                        "sinh nhat", "ky niem", "an mung", "di voi ban", "di cung ban",
                        "gia dinh", "tre em", "cap doi", "mot minh");
    }

    private boolean shouldRedirectToTravelMate(String norm) {
        return norm != null
                && norm.length() >= 3
                && !isHardOutOfScope(norm)
                && (isContextualTravelBridgeCandidate(norm)
                    || isSoftOutOfScope(norm)
                    || !hasTravelMateDomainSignal(norm));
    }

    private boolean hasTravelMateDomainSignal(String norm) {
        return extractDestination(norm) != null
                || extractBudgetVnd(norm) != null
                || extractTravelPreference(norm) != null
                || extractPropertyTypePreference(norm) != null
                || matches(norm,
                        "travelmate", "du lich", "di choi", "di dau", "lich trinh", "diem den",
                        "kham pha", "tham quan", "an gi", "am thuc",
                        "khach san", "homestay", "villa", "resort", "noi luu tru", "phong",
                        "dat phong", "booking", "check in", "check-in", "check out", "check-out",
                        "nhan phong", "tra phong", "doi lich", "doi ngay", "huy phong", "huy don",
                        "huy booking", "no show",
                        "thanh toan", "dat coc", "vnpay", "hoan tien", "voucher", "ma giam gia",
                        "uu dai", "tai khoan", "dang nhap", "dang ky", "ho tro", "lien he",
                        "gia phong", "gia tien", "bang gia", "gia re", "chi phi", "ngan sach");
    }

    private boolean isSoftOutOfScope(String norm) {
        return isCurrentNewsQuestion(norm)
                || matches(norm,
                        "thoi tiet", "du bao", "tin nong", "tin moi", "showbiz", "giai tri",
                        "phim", "game", "the thao", "bong da", "facebook", "tiktok", "youtube",
                        "mang xa hoi", "hoc bai", "bai tap", "nau an", "cong thuc",
                        "mua hang", "dien thoai", "iphone", "may tinh", "laptop", "cong nghe");
    }

    private boolean isCurrentNewsQuestion(String norm) {
        return matches(norm, "thoi su", "tin tuc hom nay", "tin moi hom nay", "tin nong hom nay")
                || (matches(norm, "tin tuc", "tin moi", "tin nong")
                    && !matches(norm, "du lich", "travelmate", "diem den", "khach san"));
    }

    private boolean isFoodContext(String norm) {
        return matches(norm, "toi doi", "dang doi", "doi bung", "them an", "muon an",
                "an gi", "do an", "am thuc", "cho dem", "pho di bo");
    }

    private boolean isRestContext(String norm) {
        return matches(norm, "met", "stress", "ap luc", "cang thang", "can nghi",
                "muon nghi", "ngu khong ngon", "thu gian", "nghi duong");
    }

    private boolean isWeatherContext(String norm) {
        return matches(norm, "thoi tiet", "du bao", "nong", "lanh", "mua", "nang");
    }

    private boolean isHardOutOfScope(String norm) {
        return matches(norm,
                // programming
                "viet code", "lap trinh", "debug code", "code java", "code python",
                // medical
                "dau bung", "uong thuoc", "thuoc gi", "kham benh", "bi benh",
                // politics
                "chinh tri", "bau cu", "quoc hoi",
                // sports / entertainment (non-travel)
                "ket qua bong da", "giai vo dich bong da",
                "bai hat moi", "ca si noi tieng",
                // finance / crypto / gambling
                "chung khoan", "co phieu", "tien ao", "bitcoin", "crypto",
                "gia vang", "ty gia", "forex", "lai suat",
                "xo so", "ca cuoc"
        );
    }

    private String extractDestination(String norm) {
        String[] spacedDests = {
            "da lat", "nha trang", "da nang", "hoi an", "sa pa",
            "phu quoc", "ha noi", "ho chi minh", "sai gon",
            "ha long", "vung tau", "mui ne", "can tho", "ninh binh",
            "ha giang", "yen bai", "quang ninh", "lao cai"
        };
        for (String d : spacedDests) {
            if (norm.contains(d)) return d;
        }

        String compact = DestinationAliasUtil.compact(norm);
        String[] compactDests = {
            "dalat", "nhatrang", "danang", "hoian", "sapa", "phuquoc",
            "hanoi", "hochiminh", "saigon", "tphcm", "halong",
            "vungtau", "muine", "cantho", "ninhbinh", "hagiang"
        };
        for (String d : compactDests) {
            if (compact.contains(d)) return d;
        }

        // short aliases
        if (compact.contains("hcm") || compact.contains("tphcm")) return "ho chi minh";

        return null;
    }

    private String resolveDisplayName(String destination) {
        String name = DestinationAliasUtil.displayName(destination);
        if (name == null || name.isBlank() || name.equals(destination.trim())) {
            String slug = DestinationAliasUtil.normalizeSlug(destination);
            name = DestinationAliasUtil.displayName(slug);
        }
        return (name == null || name.isBlank()) ? destination.trim() : name;
    }

    private String resolveFirstNameFromEmail(String email) {
        if (email == null) return null;
        return userRepository.findByEmail(email)
                .map(u -> {
                    String name = u.getName();
                    if (name == null || name.isBlank()) return null;
                    String[] parts = name.trim().split("\\s+");
                    return parts[parts.length - 1];
                })
                .orElse(null);
    }

    private String typeLabel(PropertyType type) {
        if (type == null) return "nơi lưu trú";
        return switch (type) {
            case HOTEL    -> "khách sạn";
            case VILLA    -> "villa";
            case HOMESTAY -> "homestay";
            case RESORT   -> "resort";
        };
    }

    private String statusBadge(BookingStatus status) {
        return switch (status) {
            case PENDING_PAYMENT ->
                "<span style='background:#fef3c7;color:#92400e;padding:2px 8px;border-radius:999px;font-size:.7rem;font-weight:700'>⏳ Chờ thanh toán</span>";
            case PENDING_ADMIN_APPROVAL ->
                "<span style='background:#fef3c7;color:#92400e;padding:2px 8px;border-radius:999px;font-size:.7rem;font-weight:700'>⚠ Cần đối soát</span>";
            case CONFIRMED ->
                "<span style='background:#dcfce7;color:#166534;padding:2px 8px;border-radius:999px;font-size:.7rem;font-weight:700'>✅ VNPAY đã ghi nhận</span>";
            case CHECKED_IN ->
                "<span style='background:#ccfbf1;color:#065f46;padding:2px 8px;border-radius:999px;font-size:.7rem;font-weight:700'>🏨 Đang ở</span>";
            case COMPLETED ->
                "<span style='background:#f1f5f9;color:#475569;padding:2px 8px;border-radius:999px;font-size:.7rem;font-weight:700'>✔ Hoàn thành</span>";
            case CANCELLED ->
                "<span style='background:#fee2e2;color:#991b1b;padding:2px 8px;border-radius:999px;font-size:.7rem;font-weight:700'>✗ Đã hủy</span>";
            case NO_SHOW ->
                "<span style='background:#ffedd5;color:#9a3412;padding:2px 8px;border-radius:999px;font-size:.7rem;font-weight:700'>🚫 No-show</span>";
        };
    }

    private String fmtPrice(Double price) {
        return VND_FORMAT.format(Math.round(price)) + "đ";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String encUrl(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }

    private List<AvailableRoomSuggestion> findAvailableRooms(
            List<Accommodation> accommodations, StayDates dates, int requestedRooms,
            int guests, long maxPricePerNight) {
        return accommodations.stream()
                .flatMap(accommodation -> {
                    List<RoomAvailabilityDto> availability = availabilityService.checkAvailabilityForAccommodation(
                            accommodation, dates.checkIn(), dates.checkOut());
                    return availability == null ? java.util.stream.Stream.empty()
                            : availability.stream().map(room -> new AvailableRoomSuggestion(accommodation, room));
                })
                .filter(s -> s.room().getAvailableQuantity() >= requestedRooms)
                .filter(s -> s.room().getPricePerNight() != null)
                .filter(s -> maxPricePerNight <= 0
                        || s.room().getPricePerNight().longValue() <= maxPricePerNight)
                .filter(s -> roomCanHostGuests(s.accommodation(), s.room().getRoomId(), guests, requestedRooms))
                .sorted(Comparator.comparing(s -> s.room().getPricePerNight()))
                .toList();
    }

    private boolean roomCanHostGuests(
            Accommodation accommodation, Long roomId, int guests, int requestedRooms) {
        if (accommodation.getRooms() == null || accommodation.getRooms().isEmpty()) {
            return true;
        }
        return accommodation.getRooms().stream()
                .filter(room -> roomId.equals(room.getId()))
                .findFirst()
                .map(Room::getCapacity)
                .map(capacity -> capacity * requestedRooms >= guests)
                .orElse(true);
    }

    private String roomCard(AvailableRoomSuggestion suggestion, StayDates dates, int guests, int rooms) {
        Accommodation accommodation = suggestion.accommodation();
        RoomAvailabilityDto room = suggestion.room();
        StringBuilder sb = new StringBuilder("<div class='bot-hotel-card'>");
        if (accommodation.getThumbnailUrl() != null && !accommodation.getThumbnailUrl().isBlank()) {
            sb.append("<img src='").append(accommodation.getThumbnailUrl())
              .append("' alt='' class='bot-hotel-img' loading='lazy'/>");
        } else {
            sb.append("<div class='bot-hotel-img-ph'><i class='fas fa-hotel'></i></div>");
        }
        sb.append("<div class='bot-hotel-info'>")
          .append("<p class='bot-hotel-name'>").append(esc(accommodation.getName())).append("</p>")
          .append("<p>").append(esc(room.getRoomName())).append("</p>")
          .append("<p class='bot-hotel-city'>📍 ").append(esc(accommodation.getCity())).append("</p>")
          .append("<p class='bot-hotel-price'><strong>")
          .append(fmtPrice(room.getPricePerNight().doubleValue())).append("</strong>/đêm · còn ")
          .append(room.getAvailableQuantity()).append(" phòng</p>")
          .append("<a href='/accommodations/").append(accommodation.getId())
          .append("?checkIn=").append(dates.checkIn())
          .append("&amp;checkOut=").append(dates.checkOut())
          .append("&amp;adults=").append(guests)
          .append("&amp;children=0&amp;rooms=").append(rooms)
          .append("#room-").append(room.getRoomId())
          .append("' class='bot-hotel-btn' target='_blank'>Xem phòng này</a>")
          .append("</div></div>");
        return sb.toString();
    }

    private String hotelCard(Accommodation h) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='bot-hotel-card'>");
        if (h.getThumbnailUrl() != null && !h.getThumbnailUrl().isBlank()) {
            sb.append("<img src='").append(h.getThumbnailUrl())
              .append("' alt='' class='bot-hotel-img' loading='lazy'/>");
        } else {
            sb.append("<div class='bot-hotel-img-ph'><i class='fas fa-hotel'></i></div>");
        }
        sb.append("<div class='bot-hotel-info'>")
          .append("<p class='bot-hotel-name'>").append(esc(h.getName())).append("</p>")
          .append("<p class='bot-hotel-city'>📍 ").append(esc(h.getCity())).append("</p>");
        if (h.getMinPrice() != null) {
            sb.append("<p class='bot-hotel-price'>Từ <strong>")
              .append(fmtPrice(h.getMinPrice())).append("</strong>/đêm</p>");
        }
        if (h.getRating() != null) {
            sb.append("<p class='bot-hotel-rating'>⭐ ")
              .append(String.format("%.1f", h.getRating())).append("</p>");
        }
        sb.append("<a href='/accommodations/").append(h.getId())
          .append("' class='bot-hotel-btn' target='_blank'>Xem chi tiết</a>")
          .append("</div></div>");
        return sb.toString();
    }

    private String botActionRow(String destination) {
        String keyword = (destination == null || destination.isBlank()) ? "" : "?keyword=" + encUrl(destination);
        return "<div class='bot-action-row'>"
                + "<a href='/accommodations" + keyword + "' class='bot-action-btn'>Xem nơi lưu trú</a>"
                + "<a href='/travel' class='bot-action-btn secondary'>Xem gợi ý du lịch</a>"
                + "<a href='/vouchers' class='bot-action-btn ghost'>Xem voucher</a>"
                + "</div>";
    }

    private String destinationChips(List<String> destinations) {
        StringBuilder sb = new StringBuilder("<div class='bot-dest-grid'>");
        for (String destination : destinations) {
            sb.append("<a class='bot-dest-chip' href='/accommodations?keyword=")
              .append(encUrl(destination)).append("'>")
              .append(esc(destination)).append("</a>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private void appendTravelPostHint(StringBuilder sb, String destination, String displayName) {
        if (destination == null || destination.isBlank()) return;
        String slug = DestinationAliasUtil.normalizeSlug(destination);
        Set<String> slugs = DestinationAliasUtil.searchSlugs(destination);
        List<TravelPost> posts = slugs.isEmpty()
                ? travelPostRepository.findTop3ByDestinationSlugAndStatusOrderByCreatedAtDesc(
                        slug, TravelPost.Status.VISIBLE)
                : travelPostRepository.findTop3ByDestinationSlugInAndStatusOrderByCreatedAtDesc(
                        slugs, TravelPost.Status.VISIBLE);
        if (!posts.isEmpty()) {
            TravelPost p = posts.get(0);
            sb.append("<div class='bot-budget-note'>📖 <a href='").append(esc(p.getSourceUrl()))
              .append("' class='bot-link' target='_blank' rel='noopener'>Kinh nghiệm đi ")
              .append(esc(displayName)).append(" →</a></div>");
        }
    }

    private boolean matchesDestinationOrPreference(Accommodation accommodation, String destination,
                                                   TravelPreference preference) {
        if (destination != null) {
            return DestinationAliasUtil.matchesTextOrDestination(accommodation.getCity(), destination)
                    || DestinationAliasUtil.matchesTextOrDestination(accommodation.getName(), destination);
        }
        if (preference == null) {
            return true;
        }
        return preference.destinations().stream().anyMatch(d ->
                DestinationAliasUtil.matchesTextOrDestination(accommodation.getCity(), d)
                        || DestinationAliasUtil.matchesTextOrDestination(accommodation.getName(), d));
    }

    private TravelPreference extractTravelPreference(String norm) {
        if (matches(norm, "bien", "bai bien", "tam bien", "hai san", "dao")) return BEACH_PREF;
        if (matches(norm, "nui", "san may", "trekking", "leo nui", "doi thong")) return MOUNTAIN_PREF;
        if (matches(norm, "nghi duong", "thu gian", "resort", "spa")) return RELAX_PREF;
        if (matches(norm, "gia dinh", "tre em", "con nho", "nhom ban")) return FAMILY_PREF;
        if (matches(norm, "tiet kiem", "gia re", "binh dan", "re nhat", "budget")) return SAVING_PREF;
        return null;
    }

    private TravelPreference preferenceForDestination(String destination) {
        if (matchesAnyDestination(destination, BEACH_PREF.destinations())) return BEACH_PREF;
        if (matchesAnyDestination(destination, MOUNTAIN_PREF.destinations())) return MOUNTAIN_PREF;
        if (matchesAnyDestination(destination, RELAX_PREF.destinations())) return RELAX_PREF;
        return null;
    }

    private boolean matchesAnyDestination(String destination, List<String> destinations) {
        return destinations.stream().anyMatch(d -> DestinationAliasUtil.matchesTextOrDestination(destination, d));
    }

    private Comparator<Accommodation> budgetAccommodationComparator(String destination, TravelPreference preference) {
        Comparator<Accommodation> priceComparator = Comparator.comparing(Accommodation::getMinPrice);
        if (destination != null || preference == null) {
            return priceComparator;
        }
        return Comparator.comparingInt((Accommodation a) -> preferenceDestinationIndex(a, preference))
                .thenComparing(priceComparator);
    }

    private int preferenceDestinationIndex(Accommodation accommodation, TravelPreference preference) {
        for (int i = 0; i < preference.destinations().size(); i++) {
            String destination = preference.destinations().get(i);
            if (DestinationAliasUtil.matchesTextOrDestination(accommodation.getCity(), destination)
                    || DestinationAliasUtil.matchesTextOrDestination(accommodation.getName(), destination)) {
                return i;
            }
        }
        return preference.destinations().size();
    }

    // ── Budget Travel Plan ───────────────────────────────

    private ChatbotResponse budgetTravelPlan(String message, String norm, long budgetVnd, String username) {
        int requestedNights = extractNights(norm);
        int rooms = extractRooms(norm);
        int guests = extractGuests(norm);
        StayDates dates = extractStayDates(message, requestedNights);
        int nights = (int) (dates.checkOut().toEpochDay() - dates.checkIn().toEpochDay());
        PropertyType preferredType = extractPropertyTypePreference(norm);
        TravelPreference preference = extractTravelPreference(norm);
        String dest = extractDestination(norm);

        boolean nightlyRoomBudget = isNightlyRoomBudget(norm);
        long lodgingBudget = nightlyRoomBudget ? budgetVnd * nights * rooms : Math.round(budgetVnd * 0.6);
        long maxPerNight = nightlyRoomBudget ? budgetVnd : Math.max(1L, lodgingBudget / nights / rooms);

        List<Accommodation> pool = (preferredType != null)
                ? accommodationRepository.findByPropertyTypeAndApprovalStatus(preferredType, ApprovalStatus.APPROVED)
                : accommodationRepository.findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus.APPROVED);

        List<Accommodation> matching = pool.stream()
                .filter(a -> matchesDestinationOrPreference(a, dest, preference))
                .sorted(budgetAccommodationComparator(dest, preference))
                .collect(Collectors.toList());
        List<AvailableRoomSuggestion> filtered = findAvailableRooms(
                matching, dates, rooms, guests, maxPerNight).stream().limit(3).toList();

        String destDisplay = dest != null ? resolveDisplayName(dest) : null;
        String contextLabel = destDisplay != null
                ? destDisplay
                : preference != null ? preference.label() : "chuyến đi";
        String typeLabel = typeLabel(preferredType);
        String actionDestination = destDisplay != null
                ? destDisplay
                : preference != null ? preference.destinations().get(0) : "Đà Lạt";

        StringBuilder sb = new StringBuilder("<div>");

        sb.append("<p>💰 Với ngân sách khoảng <strong>")
          .append(fmtPrice((double) budgetVnd))
          .append("</strong>");
        if (destDisplay != null) {
            sb.append(" cho <strong>").append(esc(destDisplay)).append("</strong>");
        } else if (preference != null) {
            sb.append(" và nhu cầu <strong>").append(esc(preference.label())).append("</strong>");
        }
        sb.append(", TravelMate gợi ý bạn nên chọn ")
          .append(preferredType != null ? "<strong>" + esc(typeLabel) + "</strong>" : "homestay hoặc khách sạn tầm trung")
          .append(".</p>");

        sb.append("<div class='bot-budget-box'>")
          .append("<p><strong>TravelMate tạm tính:</strong></p>")
          .append("<p>👥 ").append(guests).append(" người · ")
          .append(rooms).append(" phòng · ").append(nights).append(" đêm</p>")
          .append("<p>📅 Kiểm tra phòng trống: <strong>").append(dates.checkIn())
          .append(" - ").append(dates.checkOut()).append("</strong>")
          .append(dates.suppliedByUser() ? "</p>" : " (ngày mặc định gần nhất)</p>")
          .append(nightlyRoomBudget
                  ? "<p>🏨 Mức giá phòng/đêm bạn yêu cầu: <strong>"
                  : "<p>🏨 60% ngân sách dành cho lưu trú: <strong>")
          .append(fmtPrice(nightlyRoomBudget ? (double) maxPerNight : (double) lodgingBudget)).append("</strong></p>")
          .append("<p>💵 Giá phù hợp khoảng <strong>")
          .append(fmtPrice((double) maxPerNight)).append("</strong>/phòng/đêm</p>")
          .append("</div>")
          .append("<p style='font-size:.8rem;color:#475569'>Mình chưa tính vé xe/máy bay vì TravelMate chưa quản lý phần đó trong bản demo.</p>");

        if (preference != null && dest == null) {
            sb.append("<p>").append(preference.icon()).append(" Điểm đến hợp nhu cầu: </p>")
              .append(destinationChips(preference.destinations()));
        }

        if (filtered.isEmpty()) {
            sb.append("<p>😔 Chưa tìm thấy <strong>").append(esc(typeLabel))
              .append("</strong> phù hợp cho <strong>").append(esc(contextLabel))
              .append("</strong> trong mức <strong>").append(fmtPrice((double) maxPerNight))
              .append("</strong>/phòng/đêm.</p>")
              .append("<p>Gợi ý: thử chọn homestay, giảm số đêm hoặc xem các điểm đến chi phí mềm hơn như ")
              .append("<strong>Đà Lạt</strong>, <strong>Sa Pa</strong>, <strong>Hội An</strong>.</p>")
              .append(botActionRow(actionDestination));
        } else {
            sb.append("<p>🎯 Gợi ý <strong>").append(esc(typeLabel))
              .append("</strong> phù hợp ngân sách:</p>");

            for (AvailableRoomSuggestion suggestion : filtered) {
                sb.append(roomCard(suggestion, dates, guests, rooms));
            }

            appendTravelPostHint(sb, destDisplay != null ? dest : actionDestination, destDisplay != null ? destDisplay : actionDestination);
            sb.append(botActionRow(actionDestination));
        }
        sb.append("</div>");

        List<String> qr = dest != null
                ? List.of("Tìm khách sạn " + resolveDisplayName(dest), "Gợi ý lịch trình " + resolveDisplayName(dest),
                          "Xem voucher", "Chính sách cọc 30%")
                : List.of("4 triệu muốn đi biển", "5 triệu đi Đà Lạt 3 ngày 2 đêm",
                          "2tr tìm homestay Đà Lạt", "Xem voucher");
        return new ChatbotResponse(I_BUDGET_PLAN, sb.toString(), qr);
    }

    private boolean isNightlyRoomBudget(String norm) {
        return matches(norm, "phong duoi", "gia phong duoi", "moi dem", "/dem", "mot dem")
                || (matches(norm, "homestay", "khach san", "hotel", "resort", "villa")
                    && matches(norm, "duoi"));
    }

    private Long extractBudgetVnd(String norm) {
        String moneyText = normalizeMoneyText(norm);
        Matcher m;

        m = Pattern.compile("\\b(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|m)\\b").matcher(moneyText);
        if (m.find()) return parseVndDecimal(m.group(1), 1_000_000L);

        m = Pattern.compile("\\b(\\d+(?:[.,]\\d+)?)\\s*(nghin|ngan|k)\\b").matcher(moneyText);
        if (m.find()) return parseVndDecimal(m.group(1), 1_000L);

        m = Pattern.compile("\\b\\d{1,3}(?:[.,]\\d{3}){1,3}\\b").matcher(moneyText);
        if (m.find()) return parsePlainVnd(m.group());

        m = Pattern.compile("\\b(\\d{6,9})\\b").matcher(moneyText);
        if (m.find()) return parsePlainVnd(m.group(1));

        return null;
    }

    private String normalizeMoneyText(String input) {
        if (input == null) return "";
        return Normalizer.normalize(
                        input.toLowerCase(Locale.ROOT)
                                .replace("đ", "d"),
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private long parseVndDecimal(String numStr, long multiplier) {
        try { return Math.round(Double.parseDouble(numStr.replace(",", ".")) * multiplier); }
        catch (Exception e) { return 0L; }
    }

    private long parsePlainVnd(String numStr) {
        try { return Long.parseLong(numStr.replaceAll("[.,\\s]", "")); }
        catch (Exception e) { return 0L; }
    }

    private StayDates extractStayDates(String text, int nights) {
        String normalizedDateText = normalizeMoneyText(text);
        Matcher matcher = Pattern.compile(
                "(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?\\s*(?:den|toi|-)\\s*"
                        + "(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?")
                .matcher(normalizedDateText);
        if (matcher.find()) {
            LocalDate checkIn = parseStayDate(matcher.group(1), matcher.group(2), matcher.group(3));
            LocalDate checkOut = parseStayDate(matcher.group(4), matcher.group(5), matcher.group(6));
            if (checkIn != null && checkOut != null && checkOut.isAfter(checkIn)) {
                return new StayDates(checkIn, checkOut, true);
            }
        }
        LocalDate checkIn = LocalDate.now().plusDays(1);
        return new StayDates(checkIn, checkIn.plusDays(Math.max(1, nights)), false);
    }

    private LocalDate parseStayDate(String day, String month, String year) {
        try {
            int parsedYear = year == null ? LocalDate.now().getYear() : Integer.parseInt(year);
            if (parsedYear < 100) {
                parsedYear += 2000;
            }
            return LocalDate.of(parsedYear, Integer.parseInt(month), Integer.parseInt(day));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private int extractNights(String norm) {
        Matcher m = Pattern.compile("(\\d+)\\s*dem").matcher(norm);
        if (m.find()) { int n = Integer.parseInt(m.group(1)); return clamp(n, 1, 30); }
        m = Pattern.compile("(\\d+)\\s*ngay").matcher(norm);
        if (m.find()) { int days = Integer.parseInt(m.group(1)); return days > 1 ? days - 1 : 1; }
        return 1;
    }

    private int extractRooms(String norm) {
        Matcher m = Pattern.compile("(\\d+)\\s*(phong|can|room)").matcher(norm);
        if (m.find()) return clamp(Integer.parseInt(m.group(1)), 1, 10);
        return 1;
    }

    private int extractGuests(String norm) {
        Matcher m = Pattern.compile("(\\d+)\\s*(nguoi|khach|ng)").matcher(norm);
        if (m.find()) return clamp(Integer.parseInt(m.group(1)), 1, 30);
        return 2;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private PropertyType extractPropertyTypePreference(String norm) {
        if (matches(norm, "villa", "biet thu")) return PropertyType.VILLA;
        if (matches(norm, "homestay"))          return PropertyType.HOMESTAY;
        if (matches(norm, "resort"))            return PropertyType.RESORT;
        if (matches(norm, "khach san", "nha nghi")) return PropertyType.HOTEL;
        return null;
    }
}
