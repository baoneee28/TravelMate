package com.travelmate.util;

import com.travelmate.entity.enums.PaymentStatus;
import com.travelmate.entity.enums.SettlementStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessLabelUtilTest {

    @Test
    void expiredPaymentUsesUserFacingOverduePaymentWording() {
        BusinessLabelUtil labels = new BusinessLabelUtil();

        assertThat(labels.paymentStatusLabel(PaymentStatus.EXPIRED))
                .isEqualTo("⌛ Quá hạn thanh toán");
        assertThat(labels.paymentStatusLabelShort(PaymentStatus.EXPIRED))
                .isEqualTo("Quá hạn thanh toán");
    }

    @Test
    void refundLabelsDescribeInternalRecordingInsteadOfBankTransfer() {
        BusinessLabelUtil labels = new BusinessLabelUtil();

        assertThat(labels.paymentStatusLabel(PaymentStatus.REFUND_PENDING))
                .isEqualTo("🔄 Chờ Admin xử lý hoàn tiền");
        assertThat(labels.paymentStatusLabel(PaymentStatus.REFUNDED))
                .isEqualTo("✅ Đã ghi nhận hoàn tiền");
    }

    @Test
    void settlementLabelsUseInternalPayoutRecordingWording() {
        BusinessLabelUtil labels = new BusinessLabelUtil();

        assertThat(labels.settlementStatusLabel(SettlementStatus.PENDING))
                .isEqualTo("⏳ Chờ ghi nhận chi trả");
        assertThat(labels.settlementStatusLabel(SettlementStatus.PAID))
                .isEqualTo("✅ Đã ghi nhận chi trả");
    }
}
