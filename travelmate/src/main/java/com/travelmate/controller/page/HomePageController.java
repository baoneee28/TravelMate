package com.travelmate.controller.page;

import com.travelmate.entity.TravelDestination;
import com.travelmate.entity.TravelPost;
import com.travelmate.entity.User;
import com.travelmate.entity.Voucher;
import com.travelmate.repository.UserRepository;
import com.travelmate.security.CustomUserDetails;
import com.travelmate.service.SupportTicketService;
import com.travelmate.service.TravelDestinationService;
import com.travelmate.service.TravelPostService;
import com.travelmate.service.VoucherService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class HomePageController {

    private final VoucherService voucherService;
    private final SupportTicketService supportTicketService;
    private final UserRepository userRepository;
    private final TravelPostService travelPostService;
    private final TravelDestinationService travelDestinationService;

    public HomePageController(VoucherService voucherService,
                              SupportTicketService supportTicketService,
                              UserRepository userRepository,
                              TravelPostService travelPostService,
                              TravelDestinationService travelDestinationService) {
        this.voucherService = voucherService;
        this.supportTicketService = supportTicketService;
        this.userRepository = userRepository;
        this.travelPostService = travelPostService;
        this.travelDestinationService = travelDestinationService;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<Voucher> publicVouchers = voucherService.getPublicVouchers();

        model.addAttribute("publicVouchers", publicVouchers.stream().limit(3).toList());
        model.addAttribute("heroVoucher", publicVouchers.isEmpty() ? null : publicVouchers.get(0));
        model.addAttribute("northDestinations",
                travelDestinationService.getActiveByRegion(TravelDestination.Region.NORTH));
        model.addAttribute("centralDestinations",
                travelDestinationService.getActiveByRegion(TravelDestination.Region.CENTRAL));
        model.addAttribute("southDestinations",
                travelDestinationService.getActiveByRegion(TravelDestination.Region.SOUTH));

        return "user/index";
    }

    @GetMapping("/travel")
    public String travel(@AuthenticationPrincipal CustomUserDetails principal,
                         @RequestParam(required = false, defaultValue = "") String destination,
                         @RequestParam(required = false, defaultValue = "") String keyword,
                         @RequestParam(required = false, defaultValue = "") String checkIn,
                         @RequestParam(required = false, defaultValue = "") String checkOut,
                         @RequestParam(required = false, defaultValue = "2") int adults,
                         @RequestParam(required = false, defaultValue = "0") int children,
                         @RequestParam(required = false, defaultValue = "1") int rooms,
                         Model model) {
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String requestedDestination = !destination.isBlank() ? destination : keyword;
        boolean destinationMode = !requestedDestination.isBlank();
        List<TravelPost> destinationPosts = destinationMode
                ? travelPostService.getVisibleByDestination(requestedDestination)
                : List.of();

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("destinationMode", destinationMode);
        model.addAttribute("destination", requestedDestination);
        model.addAttribute("destinationLabel", travelPostService.getDestinationDisplayName(requestedDestination));
        model.addAttribute("destinationSlug", travelPostService.normalizeDestination(requestedDestination));
        model.addAttribute("destinationPosts", destinationPosts);
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("adults", adults);
        model.addAttribute("children", children);
        model.addAttribute("rooms", rooms);
        model.addAttribute("guidePosts",      travelPostService.getVisibleByCategory(TravelPost.Category.GUIDE));
        model.addAttribute("attractionPosts", travelPostService.getVisibleByCategory(TravelPost.Category.ATTRACTION));
        model.addAttribute("essentialPosts",  travelPostService.getVisibleByCategory(TravelPost.Category.ESSENTIAL));
        return "user/travel";
    }

    @GetMapping("/news")
    public String news(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        List<TravelPost> newsPosts = travelPostService.getVisiblePosts();

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("newsPosts", newsPosts);
        model.addAttribute("featuredPost", newsPosts.isEmpty() ? null : newsPosts.get(0));
        model.addAttribute("sidebarPosts", newsPosts.stream().skip(1).limit(5).toList());
        model.addAttribute("gridPosts", newsPosts.stream().skip(6).toList());
        model.addAttribute("newsLocations", newsPosts.stream()
                .map(TravelPost::getDestination)
                .filter(destination -> destination != null && !destination.isBlank())
                .distinct()
                .limit(8)
                .toList());
        return "user/news";
    }

    /**
     * GET /contact — trang liên hệ/hỗ trợ cho User và Guest.
     * Nếu đã đăng nhập thì pre-fill thông tin vào model để Thymeleaf render.
     */
    @GetMapping("/contact")
    public String contact(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("prefillName",  userDetails.getFullName());
            model.addAttribute("prefillEmail", userDetails.getEmail());
            model.addAttribute("prefillPhone", userDetails.getPhone() != null ? userDetails.getPhone() : "");
            model.addAttribute("isLoggedIn", true);
        } else {
            model.addAttribute("isLoggedIn", false);
        }
        addSupportContactInfo(model);
        return "user/contact";
    }

    /**
     * POST /contact/submit — xử lý form gửi liên hệ.
     * Phân biệt USER (đăng nhập) và GUEST (khách vãng lai).
     */
    @PostMapping("/contact/submit")
    public String submitContact(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String contactName,
            @RequestParam String contactEmail,
            @RequestParam(required = false, defaultValue = "") String contactPhone,
            @RequestParam String contactCategory,
            @RequestParam String contactSubject,
            @RequestParam(required = false, defaultValue = "Trung bình") String priority,
            @RequestParam String contactMessage,
            RedirectAttributes ra) {
        try {
            if (userDetails != null) {
                // User đã đăng nhập
                User user = userRepository.findByEmail(userDetails.getEmail()).orElse(null);
                if (user != null) {
                    supportTicketService.createUserContact(
                            user, contactPhone, contactCategory,
                            contactSubject, priority, contactMessage);
                } else {
                    // Fallback về guest nếu không tìm thấy user
                    supportTicketService.createGuestContact(
                            contactName, contactEmail, contactPhone,
                            contactCategory, contactSubject, priority, contactMessage);
                }
            } else {
                // Guest chưa đăng nhập
                supportTicketService.createGuestContact(
                        contactName, contactEmail, contactPhone,
                        contactCategory, contactSubject, priority, contactMessage);
            }
            ra.addFlashAttribute("contactSuccess", true);
        } catch (Exception e) {
            ra.addFlashAttribute("contactError", "Có lỗi xảy ra. Vui lòng thử lại.");
        }
        return "redirect:/contact";
    }

    @PostMapping("/contact/report")
    public String submitHotelReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String reportHotel,
            @RequestParam String reportLocation,
            @RequestParam String reportType,
            @RequestParam String reportDetail,
            RedirectAttributes ra) {
        try {
            String subject = "Báo cáo cơ sở lưu trú: " + reportHotel.trim();
            String description = "Địa điểm: " + reportLocation.trim()
                    + "\nLoại vấn đề: " + reportType.trim()
                    + "\nChi tiết: " + reportDetail.trim();

            if (userDetails != null) {
                User user = userRepository.findByEmail(userDetails.getEmail()).orElse(null);
                if (user != null) {
                    supportTicketService.createUserContact(
                            user,
                            user.getPhone() != null ? user.getPhone() : "",
                            "Báo cáo cơ sở lưu trú",
                            subject,
                            "Cao",
                            description);
                } else {
                    supportTicketService.createGuestContact(
                            "Khách vãng lai", "", "",
                            "Báo cáo cơ sở lưu trú",
                            subject,
                            "Cao",
                            description);
                }
            } else {
                supportTicketService.createGuestContact(
                        "Khách vãng lai", "", "",
                        "Báo cáo cơ sở lưu trú",
                        subject,
                        "Cao",
                        description);
            }
            ra.addFlashAttribute("reportSuccess", true);
        } catch (Exception e) {
            ra.addFlashAttribute("reportError", "Có lỗi xảy ra khi gửi báo cáo. Vui lòng thử lại.");
        }
        return "redirect:/contact#reportSection";
    }

    /** GET /vouchers — hiển thị voucher USER_GLOBAL đang hoạt động từ DB */
    @GetMapping("/vouchers")
    public String voucher(Model model) {
        model.addAttribute("publicVouchers", voucherService.getPublicVouchers());
        return "user/voucher";
    }

    private void addSupportContactInfo(Model model) {
        model.addAttribute("supportHotline", "+84 123 456 789");
        model.addAttribute("supportEmail", "travelmatek23@gmail.com");
        model.addAttribute("supportWorkingTime", "08:00 - 22:00 hằng ngày");
        model.addAttribute("supportAddress",
                "123 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh");
    }
}
