package com.travelmate.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * HomePageController - Controller xu ly cac trang PUBLIC cua TravelMate.
 *
 * Day la controller chinh phuc vu giao dien cho tat ca nguoi dung
 * (ke ca chua dang nhap). Dung @Controller (khong phai @RestController)
 * de tra ve TEN VIEW thay vi JSON.
 *
 * Cach hoat dong:
 * 1. User truy cap URL (vd: http://localhost:8080/)
 * 2. Spring MVC goi method tuong ung (vd: homePage())
 * 3. Method tra ve ten view (vd: "user/index")
 * 4. Thymeleaf tim file templates/user/index.html va render
 * 5. Tra HTML ve trinh duyet
 *
 * Annotation giai thich:
 * - @Controller: danh dau day la MVC controller, tra ve view name
 *   (khac voi @RestController tra ve JSON)
 */
@Controller
public class HomePageController {

    // ============================================================
    // TRANG CHU - Trang dau tien khi user truy cap TravelMate
    // URL: http://localhost:8080/
    // ============================================================
    @GetMapping("/")
    public String homePage() {
        // Tra ve "user/index" => Thymeleaf se tim file:
        // src/main/resources/templates/user/index.html
        return "user/index";
    }

    // ============================================================
    // TRANG DANG NHAP
    // URL: http://localhost:8080/login
    // Ai cung truy cap duoc (permitAll trong SecurityConfig)
    // ============================================================
    @GetMapping("/login")
    public String loginPage() {
        return "user/login";
    }

    // ============================================================
    // TRANG DANG KY
    // URL: http://localhost:8080/register
    // Ai cung truy cap duoc
    // ============================================================
    @GetMapping("/register")
    public String registerPage() {
        return "user/register";
    }

    // ============================================================
    // TRANG GOI Y DU LICH
    // URL: http://localhost:8080/travel
    // Trang cong khai, hien thi dia diem du lich noi tieng
    // ============================================================
    @GetMapping("/travel")
    public String travelPage() {
        return "user/travel";
    }

    // ============================================================
    // TRANG TIN TUC
    // URL: http://localhost:8080/news
    // Trang cong khai, hien thi tin tuc du lich
    // ============================================================
    @GetMapping("/news")
    public String newsPage() {
        return "user/news";
    }

    // ============================================================
    // TRANG LIEN HE
    // URL: http://localhost:8080/contact
    // Trang cong khai, form lien he voi TravelMate
    // ============================================================
    @GetMapping("/contact")
    public String contactPage() {
        return "user/contact";
    }

    // ============================================================
    // TRANG DANH SACH NƠI LUU TRU (Hotels, Homestay, Villa...)
    // URL: http://localhost:8080/accommodations
    // Trang cong khai, hien thi danh sach noi luu tru
    // ============================================================
    @GetMapping("/accommodations")
    public String accommodationsPage() {
        // Tam thoi dung file hotels.html (se rename sau khi du tien do)
        return "user/hotels";
    }

    // ============================================================
    // TRANG CHI TIET NOI LUU TRU
    // URL: http://localhost:8080/accommodations/{id}
    // Trang cong khai, hien thi chi tiet 1 noi luu tru
    // Sau nay se truyen id tu URL vao Model de query DB
    // ============================================================
    @GetMapping("/accommodations/{id}")
    public String accommodationDetailPage() {
        return "user/hotel-detail";
    }
}
