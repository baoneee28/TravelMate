package com.travelmate.controller.page;

import com.travelmate.dto.response.AccommodationResponse;
import com.travelmate.enums.PropertyType;
import com.travelmate.exception.ResourceNotFoundException;
import com.travelmate.service.AccommodationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * HomePageController - Xử lý các trang PUBLIC của TravelMate.
 *
 * Ai cũng truy cập được (kể cả chưa đăng nhập).
 * Dùng @Controller (không phải @RestController) vì trả về TÊN VIEW (HTML),
 * không phải JSON.
 *
 * @RequiredArgsConstructor: Lombok tự tạo constructor inject AccommodationService
 * (thay cho @Autowired — constructor injection là cách khuyến nghị).
 */
@Controller
@RequiredArgsConstructor
public class HomePageController {

    private final AccommodationService accommodationService;

    // ====================================================================
    // TRANG CHỦ
    // URL: http://localhost:8080/
    // ====================================================================
    @GetMapping("/")
    public String homePage() {
        return "user/index";
    }

    // ====================================================================
    // TRANG ĐĂNG NHẬP
    // URL: http://localhost:8080/login
    // ====================================================================
    @GetMapping("/login")
    public String loginPage() {
        return "user/login";
    }

    // ====================================================================
    // TRANG ĐĂNG KÝ
    // URL: http://localhost:8080/register
    // ====================================================================
    @GetMapping("/register")
    public String registerPage() {
        return "user/register";
    }

    // ====================================================================
    // TRANG GỢI Ý DU LỊCH (static)
    // URL: http://localhost:8080/travel
    // ====================================================================
    @GetMapping("/travel")
    public String travelPage() {
        return "user/travel";
    }

    // ====================================================================
    // TRANG TIN TỨC (static)
    // URL: http://localhost:8080/news
    // ====================================================================
    @GetMapping("/news")
    public String newsPage() {
        return "user/news";
    }

    // ====================================================================
    // TRANG LIÊN HỆ (static)
    // URL: http://localhost:8080/contact
    // ====================================================================
    @GetMapping("/contact")
    public String contactPage() {
        return "user/contact";
    }

    // ====================================================================
    // TRANG DANH SÁCH NƠI LƯU TRÚ
    // URL: http://localhost:8080/accommodations
    //      http://localhost:8080/accommodations?city=Đà+Nẵng
    //      http://localhost:8080/accommodations?city=Hà+Nội&type=HOTEL
    //
    // @RequestParam(required = false): tham số URL không bắt buộc
    // Nếu không truyền → giá trị null → service bỏ qua filter đó
    // ====================================================================
    @GetMapping("/accommodations")
    public String accommodationsPage(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String type,
            Model model) {

        // Chuyển chuỗi "type" từ URL thành enum PropertyType
        // Ví dụ: "HOTEL" → PropertyType.HOTEL
        // Nếu type không hợp lệ → giữ null để không filter
        PropertyType propertyType = null;
        if (type != null && !type.isBlank()) {
            try {
                propertyType = PropertyType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // type không hợp lệ (vd: "abc") → bỏ qua, không crash
            }
        }

        // Lấy danh sách từ DB — chỉ trả về accommodation đã APPROVED
        List<AccommodationResponse> accommodations =
                accommodationService.searchApproved(city, propertyType);

        // Truyền data vào Model để Thymeleaf đọc trong template
        model.addAttribute("accommodations", accommodations);
        model.addAttribute("totalCount", accommodations.size());
        model.addAttribute("searchCity",  city != null ? city : "");
        model.addAttribute("searchType",  type != null ? type : "");

        return "user/accommodations";
    }

    // ====================================================================
    // TRANG CHI TIẾT NƠI LƯU TRÚ
    // URL: http://localhost:8080/accommodations/{id}
    //
    // Chỉ hiển thị nếu accommodation có approvalStatus = APPROVED.
    // Nếu không tìm thấy hoặc chưa duyệt → redirect trang lỗi.
    // ====================================================================
    @GetMapping("/accommodations/{id}")
    public String accommodationDetailPage(@PathVariable Long id, Model model) {

        // Try-catch vì GlobalExceptionHandler chỉ xử lý REST controllers.
        // Page controller tự bắt exception và quyết định hiển thị gì.
        try {
            AccommodationResponse accommodation = accommodationService.findApprovedById(id);
            model.addAttribute("accommodation", accommodation);
            return "user/accommodation-detail";

        } catch (ResourceNotFoundException e) {
            // Nơi lưu trú không tồn tại hoặc chưa được duyệt
            model.addAttribute("errorMessage",
                    "Nơi lưu trú không tồn tại hoặc chưa được duyệt.");
            model.addAttribute("errorTitle", "Không tìm thấy nơi lưu trú");
            return "error";
        }
    }
}
