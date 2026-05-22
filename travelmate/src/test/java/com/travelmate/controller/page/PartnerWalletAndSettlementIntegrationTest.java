package com.travelmate.controller.page;

import com.travelmate.entity.PartnerSettlement;
import com.travelmate.entity.User;
import com.travelmate.repository.PartnerSettlementRepository;
import com.travelmate.repository.UserRepository;
import com.travelmate.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Partner Wallet & Settlement Integration Tests")
class PartnerWalletAndSettlementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PartnerSettlementRepository settlementRepository;

    @Test
    @DisplayName("ROLE_USER truy cập wallet của Partner bị 403 Forbidden")
    @WithMockUser(username = "user@travelmate.vn", roles = "USER")
    void userAccessingPartnerWallet_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/partner/wallet"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_USER truy cập settlements của Admin bị 403 Forbidden")
    @WithMockUser(username = "user@travelmate.vn", roles = "USER")
    void userAccessingAdminSettlements_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/admin/settlements"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_USER truy cập withdrawals của Admin bị 403 Forbidden")
    @WithMockUser(username = "user@travelmate.vn", roles = "USER")
    void userAccessingAdminWithdrawals_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/admin/withdrawals"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Chưa đăng nhập truy cập admin/settlements bị redirect về trang login")
    void anonymousAccessingAdminSettlements_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/settlements"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
    }

    @Test
    @DisplayName("ROLE_ADMIN truy cập trang quản lý settlements thành công")
    @WithMockUser(username = "admin@travelmate.vn", roles = "ADMIN")
    void adminAccessingSettlements_shouldSucceed() throws Exception {
        mockMvc.perform(get("/admin/settlements"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/settlements"))
                .andExpect(model().attributeExists("settlements"));
    }

    @Test
    @DisplayName("ROLE_ADMIN truy cập trang quản lý withdrawals thành công")
    @WithMockUser(username = "admin@travelmate.vn", roles = "ADMIN")
    void adminAccessingWithdrawals_shouldSucceed() throws Exception {
        mockMvc.perform(get("/admin/withdrawals"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/withdrawals"))
                .andExpect(model().attributeExists("withdrawals"));
    }

    @Test
    @DisplayName("ROLE_ADMIN xuất Excel danh sách rút tiền trả về content type xlsx")
    @WithMockUser(username = "admin@travelmate.vn", roles = "ADMIN")
    void adminExportWithdrawalsExcel_shouldReturnExcelFile() throws Exception {
        mockMvc.perform(get("/admin/withdrawals/export-excel"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"partner-withdrawals.xlsx\""))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8"));
    }

    @Test
    @DisplayName("ROLE_ADMIN xuất Excel chi tiết settlement trả về đúng file xlsx")
    @WithMockUser(username = "admin@travelmate.vn", roles = "ADMIN")
    void adminExportSettlementExcel_shouldReturnExcelFile() throws Exception {
        PartnerSettlement settlement = settlementRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Seed data settlement not found"));

        mockMvc.perform(get("/admin/settlements/{id}/export-excel", settlement.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"settlement-" + settlement.getId() + ".xlsx\""))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8"));
    }

    @Test
    @DisplayName("ROLE_ADMIN xuất Excel settlement với ID không tồn tại trả về 404")
    @WithMockUser(username = "admin@travelmate.vn", roles = "ADMIN")
    void adminExportMissingSettlementExcel_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/admin/settlements/{id}/export-excel", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PARTNER truy cập trang ví thành công và xem thông tin")
    void partnerAccessingWallet_shouldSucceed() throws Exception {
        User partner = userRepository.findByEmail("partner@travelmate.vn")
                .orElseThrow(() -> new AssertionError("Seed data partner not found"));
        CustomUserDetails userDetails = new CustomUserDetails(partner);

        mockMvc.perform(get("/partner/wallet").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("partner/wallet"))
                .andExpect(model().attributeExists("wallet"))
                .andExpect(model().attributeExists("transactions"))
                .andExpect(model().attributeExists("withdrawals"));
    }

    @Test
    @DisplayName("PARTNER cập nhật tài khoản ngân hàng thành công")
    void partnerUpdatingBankInfo_shouldSucceed() throws Exception {
        User partner = userRepository.findByEmail("partner@travelmate.vn")
                .orElseThrow(() -> new AssertionError("Seed data partner not found"));
        CustomUserDetails userDetails = new CustomUserDetails(partner);

        mockMvc.perform(post("/partner/wallet/bank")
                        .param("bankName", "VietinBank")
                        .param("bankAccountNumber", "10283746592")
                        .param("bankAccountHolder", "NGUYEN VAN PARTNER TEST")
                        .param("bankBranch", "Ha Noi Branch")
                        .with(user(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/partner/wallet"))
                .andExpect(flash().attributeExists("successMessage"));

        // Verify state is saved in DB
        User updatedPartner = userRepository.findByEmail("partner@travelmate.vn").get();
        assertThat(updatedPartner.getBankName()).isEqualTo("VietinBank");
        assertThat(updatedPartner.getBankAccountNumber()).isEqualTo("10283746592");
        assertThat(updatedPartner.getBankAccountHolder()).isEqualTo("NGUYEN VAN PARTNER TEST");
    }

    @Test
    @DisplayName("PARTNER cập nhật tài khoản ngân hàng không hợp lệ bị báo lỗi")
    void partnerUpdatingInvalidBankInfo_shouldFail() throws Exception {
        User partner = userRepository.findByEmail("partner@travelmate.vn")
                .orElseThrow(() -> new AssertionError("Seed data partner not found"));
        CustomUserDetails userDetails = new CustomUserDetails(partner);

        // Account number with letters (invalid)
        mockMvc.perform(post("/partner/wallet/bank")
                        .param("bankName", "VietinBank")
                        .param("bankAccountNumber", "INVALID123")
                        .param("bankAccountHolder", "NGUYEN VAN PARTNER TEST")
                        .param("bankBranch", "Ha Noi Branch")
                        .with(user(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/partner/wallet"))
                .andExpect(flash().attribute("errorMessage", "❌ Số tài khoản chỉ được gồm chữ số, từ 6 đến 30 ký tự."));
    }

    @Test
    @DisplayName("PARTNER yêu cầu rút tiền vượt số dư khả dụng bị chặn")
    void partnerRequestingOverdraftWithdrawal_shouldFail() throws Exception {
        User partner = userRepository.findByEmail("partner@travelmate.vn")
                .orElseThrow(() -> new AssertionError("Seed data partner not found"));
        CustomUserDetails userDetails = new CustomUserDetails(partner);

        // Standard wallet starts with 0 available balance
        mockMvc.perform(post("/partner/wallet/withdrawals")
                        .param("amount", "999999999")
                        .with(user(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/partner/wallet"))
                .andExpect(flash().attribute("errorMessage", "❌ Số dư ví không đủ để rút!"));
    }
}
