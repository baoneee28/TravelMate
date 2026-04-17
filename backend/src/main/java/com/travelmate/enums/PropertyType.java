package com.travelmate.enums;

/**
 * Enum dai dien cho loai hinh luu tru trong he thong TravelMate.
 *
 * TravelMate ho tro nhieu loai luu tru, khong chi rieng khach san:
 * - HOTEL: khach san truyen thong
 * - HOMESTAY: nha o chia se, mang tinh dia phuong
 * - VILLA: biet thu nghi duong, thuong co ho boi/san vuon
 * - APARTMENT: can ho cho thue ngan ngay
 *
 * Moi listing (noi luu tru) do partner tao se phai chon 1 trong 4 loai nay.
 * Enum nay giup tranh hard-code string va dam bao tinh nhat quan trong DB.
 */
public enum PropertyType {

    HOTEL,      // Khach san
    HOMESTAY,   // Homestay
    VILLA,      // Biet thu nghi duong
    APARTMENT   // Can ho cho thue
}
