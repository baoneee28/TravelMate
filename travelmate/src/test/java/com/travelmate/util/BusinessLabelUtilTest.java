package com.travelmate.util;

import com.travelmate.entity.enums.PaymentStatus;
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
}
