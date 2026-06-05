package com.travelmate.controller.page;

import com.travelmate.entity.Payment;
import com.travelmate.entity.enums.PaymentStatus;
import com.travelmate.repository.BookingRepository;
import com.travelmate.repository.PaymentRepository;
import com.travelmate.service.PaymentService;
import com.travelmate.service.TravelPostService;
import com.travelmate.service.VnpayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentService paymentService;
    @Mock private TravelPostService travelPostService;
    @Mock private VnpayService vnpayService;

    @InjectMocks private PaymentController paymentController;

    @Test
    void vnpayReturnSuccessWithUnknownTxnRefShowsAuditErrorInsteadOfSuccess() {
        Map<String, String> params = Map.of(
                "vnp_TxnRef", "TM-MISSING",
                "vnp_ResponseCode", "00",
                "vnp_TransactionStatus", "00",
                "vnp_Amount", "10000000",
                "vnp_SecureHash", "signed");
        Model model = new ConcurrentModel();

        when(vnpayService.verifySignature(params)).thenReturn(true);
        when(paymentRepository.findByVnpTxnRef("TM-MISSING")).thenReturn(Optional.empty());

        String view = paymentController.vnpayReturn(params, model, null);

        assertThat(view).isEqualTo("user/payment-result");
        assertThat(model.asMap()).containsEntry("isSuccess", false);
        assertThat(model.asMap()).containsEntry("txnRef", "TM-MISSING");
        assertThat(model.asMap()).containsEntry("errorCode", "ORDER_NOT_FOUND");
        verify(paymentService, never()).markGatewaySuccess("TM-MISSING", params);
    }

    @Test
    void vnpayReturnLateSuccessAfterExpiredPaymentDoesNotShowSuccess() {
        Map<String, String> params = Map.of(
                "vnp_TxnRef", "TM-EXPIRED",
                "vnp_ResponseCode", "00",
                "vnp_TransactionStatus", "00",
                "vnp_Amount", "10000000",
                "vnp_SecureHash", "signed");
        Model model = new ConcurrentModel();
        Payment payment = new Payment();
        payment.setPaymentStatus(PaymentStatus.EXPIRED);
        payment.setAmount(new BigDecimal("100000"));

        when(vnpayService.verifySignature(params)).thenReturn(true);
        when(paymentRepository.findByVnpTxnRef("TM-EXPIRED")).thenReturn(Optional.of(payment));

        String view = paymentController.vnpayReturn(params, model, null);

        assertThat(view).isEqualTo("user/payment-result");
        assertThat(model.asMap()).containsEntry("isSuccess", false);
        assertThat(model.asMap()).containsEntry("txnRef", "TM-EXPIRED");
        assertThat(model.asMap()).containsEntry("errorCode", "STALE_PAYMENT_STATUS");
        assertThat(model.asMap().get("message").toString()).contains("hết hạn");
        verify(paymentService, never()).markGatewaySuccess("TM-EXPIRED", params);
    }
}
