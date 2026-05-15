package com.travelmate.controller.api;

import com.travelmate.entity.Room;
import com.travelmate.entity.Voucher;
import com.travelmate.entity.enums.DiscountType;
import com.travelmate.entity.enums.PaymentOption;
import com.travelmate.service.AccommodationService;
import com.travelmate.service.BookingService;
import com.travelmate.service.VoucherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * VoucherApiController — Real-time voucher check for booking page.
 *
 * GET /api/voucher/check?code=SUMMER10&roomId=1&totalAmount=1300000
 * Returns JSON: { valid, discountAmount, newTotal, newFull, newDeposit, discountDesc, errorMsg }
 */
@RestController
@RequestMapping("/api/voucher")
public class VoucherApiController {

    private final VoucherService voucherService;
    private final AccommodationService accommodationService;
    private final BookingService bookingService;

    public VoucherApiController(VoucherService voucherService,
                                AccommodationService accommodationService,
                                BookingService bookingService) {
        this.voucherService = voucherService;
        this.accommodationService = accommodationService;
        this.bookingService = bookingService;
    }

    @GetMapping("/check")
    public Map<String, Object> checkVoucher(
            @RequestParam String code,
            @RequestParam Long roomId,
            @RequestParam BigDecimal totalAmount) {

        Map<String, Object> result = new HashMap<>();

        try {
            Room room = accommodationService.getRoomById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Phòng không tồn tại!"));

            Voucher voucher = voucherService.validateVoucher(code, totalAmount, room);
            BigDecimal discount = voucherService.calculateDiscount(voucher, totalAmount);
            BigDecimal newTotal = totalAmount.subtract(discount).max(BigDecimal.ZERO);

            BigDecimal newFull    = bookingService.calculatePaidAmount(newTotal, PaymentOption.FULL_PAYMENT);
            BigDecimal newDeposit = bookingService.calculatePaidAmount(newTotal, PaymentOption.DEPOSIT_30);

            result.put("valid",          true);
            result.put("discountAmount", discount.longValue());
            result.put("newTotal",       newTotal.longValue());
            result.put("newFull",        newFull.longValue());
            result.put("newDeposit",     newDeposit.longValue());
            result.put("discountDesc",   buildDiscountDesc(voucher, discount));
            result.put("errorMsg",       null);

        } catch (Exception e) {
            result.put("valid",    false);
            result.put("errorMsg", e.getMessage());
        }

        return result;
    }

    private String buildDiscountDesc(Voucher voucher, BigDecimal discount) {
        String val = voucher.getDiscountType() == DiscountType.PERCENT
                ? String.format("%.0f%%", voucher.getDiscountValue())
                : String.format("%,.0f₫", voucher.getDiscountValue());
        return "Giảm " + val + " → tiết kiệm "
               + String.format("%,.0f₫", discount);
    }
}
