package com.travelmate.controller.page;

import com.travelmate.entity.TravelPost;
import com.travelmate.entity.User;
import com.travelmate.repository.UserRepository;
import com.travelmate.security.CustomUserDetails;
import com.travelmate.service.SupportTicketService;
import com.travelmate.service.TravelPostService;
import com.travelmate.service.VoucherService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomePageController {

    private final VoucherService voucherService;
    private final SupportTicketService supportTicketService;
    private final UserRepository userRepository;
    private final TravelPostService travelPostService;

    public HomePageController(VoucherService voucherService,
                              SupportTicketService supportTicketService,
                              UserRepository userRepository,
                              TravelPostService travelPostService) {
        this.voucherService = voucherService;
        this.supportTicketService = supportTicketService;
        this.userRepository = userRepository;
        this.travelPostService = travelPostService;
    }

    @GetMapping("/")
    public String home() { return "user/index"; }

    @GetMapping("/travel")
    public String travel(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("guidePosts",      travelPostService.getVisibleByCategory(TravelPost.Category.GUIDE));
        model.addAttribute("attractionPosts", travelPostService.getVisibleByCategory(TravelPost.Category.ATTRACTION));
        model.addAttribute("essentialPosts",  travelPostService.getVisibleByCategory(TravelPost.Category.ESSENTIAL));
        return "user/travel";
    }

    @GetMapping("/news")
    public String news() { return "user/news"; }

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

    /** GET /vouchers — hiển thị voucher USER_GLOBAL đang hoạt động từ DB */
    @GetMapping("/vouchers")
    public String voucher(Model model) {
        model.addAttribute("publicVouchers", voucherService.getPublicVouchers());
        return "user/voucher";
    }
}
