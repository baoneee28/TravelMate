package com.travelmate.enums;

/**
 * Enum dai dien cho phuong thuc thanh toan.
 *
 * TravelMate ho tro cac phuong thuc:
 * - VNPAY: thanh toan qua cong VNPay
 * - MOMO: thanh toan qua vi MoMo
 * - QR_INVOICE: thanh toan bang ma QR hoa don
 * - CASH: thanh toan tien mat tai noi luu tru
 *
 * Luu y: o giai doan dau, VNPay va MoMo se la mock flow (mo phong),
 * chua tich hop that. Khi nao can tich hop that se cap nhat sau.
 */
public enum PaymentMethod {

    VNPAY,          // Thanh toan qua VNPay
    MOMO,           // Thanh toan qua MoMo
    QR_INVOICE,     // Thanh toan bang QR hoa don
    CASH            // Thanh toan tien mat
}
