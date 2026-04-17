package com.travelmate.enums;

/**
 * Enum dai dien cho vai tro (role) cua nguoi dung trong he thong TravelMate.
 *
 * He thong co 3 role co dinh:
 * - USER: nguoi dung binh thuong, co the tim kiem va dat cho
 * - ADMIN: quan tri vien, co quyen duyet listing, quan ly he thong
 * - PARTNER: doi tac (chu khach san, homestay...), co quyen tao va quan ly listing
 *
 * Role duoc luu truc tiep trong bang User duoi dang enum (khong tach bang rieng)
 * vi he thong chi co 3 role co dinh, don gian va de hieu.
 */
public enum Role {

    USER,       // Nguoi dung binh thuong - dat phong, review, thanh toan
    ADMIN,      // Quan tri vien - duyet listing, quan ly he thong
    PARTNER     // Doi tac - tao listing, quan ly noi luu tru
}
