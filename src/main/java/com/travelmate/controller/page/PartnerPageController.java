package com.travelmate.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * PartnerPageController - Controller xu ly cac trang PARTNER cua TravelMate.
 *
 * Tat ca URL bat dau bang /partner/** chi cho phep role PARTNER truy cap.
 * Partner la doi tac (khach san, homestay, villa) dang ky listing tren TravelMate.
 *
 * Cac trang partner gom:
 * - Rooms: danh sach phong dang quan ly
 * - Add Room: them phong moi
 * - Pending Room: phong dang cho admin duyet
 * - Add Voucher: them ma giam gia
 * - Add Furniture: them noi that/tien ich cho phong
 *
 * Annotation:
 * - @Controller: tra ve view name
 * - @RequestMapping("/partner"): prefix cho tat ca URL partner
 *   (Luu y: dung "partner" so it, khong phai "partners")
 */
@Controller
@RequestMapping("/partner")
public class PartnerPageController {

    // ============================================================
    // DANH SACH PHONG CUA PARTNER
    // URL: http://localhost:8080/partner/rooms
    // Hien thi tat ca phong ma Partner dang quan ly
    // ============================================================
    @GetMapping("/rooms")
    public String roomsPage() {
        // Sau nay: model.addAttribute("rooms", roomService.getRoomsByPartner(partnerId))
        return "partner/rooms";
    }

    // ============================================================
    // THEM PHONG MOI
    // URL: http://localhost:8080/partner/add-room
    // Form de Partner tao phong moi (se gui len server de admin duyet)
    // ============================================================
    @GetMapping("/add-room")
    public String addRoomPage() {
        return "partner/add-room";
    }

    // ============================================================
    // PHONG DANG CHO DUYET
    // URL: http://localhost:8080/partner/pending-room
    // Hien thi cac phong Partner da gui nhung chua duoc Admin duyet
    // ============================================================
    @GetMapping("/pending-room")
    public String pendingRoomPage() {
        return "partner/pending-room";
    }

    // ============================================================
    // THEM VOUCHER / MA GIAM GIA
    // URL: http://localhost:8080/partner/add-voucher
    // Partner tao ma giam gia cho khach hang
    // ============================================================
    @GetMapping("/add-voucher")
    public String addVoucherPage() {
        return "partner/add-voucher";
    }

    // ============================================================
    // THEM NOI THAT / TIEN ICH
    // URL: http://localhost:8080/partner/add-furniture
    // Partner them thong tin noi that, tien ich cho phong
    // ============================================================
    @GetMapping("/add-furniture")
    public String addFurniturePage() {
        return "partner/add-furniture";
    }
}
