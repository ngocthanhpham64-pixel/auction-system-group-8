package vn.edu.vnu.uet.group8.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.gson.JsonObject;

import vn.edu.vnu.uet.group8.common.dto.model.UserProfileDTO;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.enums.PaymentMethod;
import vn.edu.vnu.uet.group8.server.service.user.BalanceService;
import vn.edu.vnu.uet.group8.server.service.user.PasswordService;
import vn.edu.vnu.uet.group8.server.service.user.ProfileService;

/**
 * Test cho {@link UserController} - 5 handler.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  @Mock private ProfileService profileService;
  @Mock private BalanceService balanceService;
  @Mock private PasswordService passwordService;

  private UserController controller;

  @BeforeEach
  void setUp() {
    controller = new UserController(profileService, balanceService, passwordService);
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("handleGetProfile()")
  class GetProfileTest {

    @Test
    @DisplayName("Không có targetUserId → lấy profile chính mình")
    void tuXemMinh() throws SQLException {
      JsonObject req = new JsonObject();

      UserProfileDTO profile = mock(UserProfileDTO.class);

      when(profileService.getProfile(100, 100))
          .thenReturn(profile);

      ServerResponse res = controller.handleGetProfile(req, "req-1", 100);

      assertTrue(res.isSuccess());
      verify(profileService).getProfile(100, 100);
    }

    @Test
    @DisplayName("Có targetUserId → lấy profile người khác")
    void xemNguoiKhac() throws SQLException {
      JsonObject payload = new JsonObject();
      payload.addProperty("targetUserId", 5);
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      UserProfileDTO profile = mock(UserProfileDTO.class);

      when(profileService.getProfile(100, 5))
          .thenReturn(profile);

      ServerResponse res = controller.handleGetProfile(req, "req-1", 100);

      assertTrue(res.isSuccess());
      verify(profileService).getProfile(100, 5);
    }

    @Test
    @DisplayName("Service ném exception → error response")
    void serviceLoi() throws SQLException {
      when(profileService.getProfile(anyInt(), anyInt()))
          .thenThrow(new SQLException("DB lỗi"));

      JsonObject req = new JsonObject();
      ServerResponse res = controller.handleGetProfile(req, "req-1", 100);
      assertFalse(res.isSuccess());
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("handleDeposit()")
  class DepositTest {

    @Test
    @DisplayName("Deposit thành công")
    void depositThanhCong() throws SQLException {
      JsonObject payload = new JsonObject();
      payload.addProperty("amount", "1000");
      payload.addProperty("transactionId", "tx-1");
      payload.addProperty("paymentMethod", "COD");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      when(balanceService.topUpBalance(100, new BigDecimal("1000"), "tx-1", PaymentMethod.COD))
          .thenReturn(new BigDecimal("5000"));

      ServerResponse res = controller.handleDeposit(req, "req-1", 100);

      assertTrue(res.isSuccess());
      assertEquals("USER_DEPOSIT", res.getAction());
    }

    @Test
    @DisplayName("Thiếu field amount → error")
    void thieuAmount() {
      JsonObject payload = new JsonObject();
      payload.addProperty("transactionId", "tx-1");
      payload.addProperty("paymentMethod", "COD");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      ServerResponse res = controller.handleDeposit(req, "req-1", 100);
      assertFalse(res.isSuccess());
    }

    @Test
    @DisplayName("PaymentMethod không hợp lệ → error")
    void paymentMethodSai() {
      JsonObject payload = new JsonObject();
      payload.addProperty("amount", "1000");
      payload.addProperty("transactionId", "tx-1");
      payload.addProperty("paymentMethod", "INVALID_METHOD");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      ServerResponse res = controller.handleDeposit(req, "req-1", 100);
      assertFalse(res.isSuccess());
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("handleWithdraw()")
  class WithdrawTest {

    @Test
    @DisplayName("Withdraw thành công")
    void withdrawThanhCong() throws SQLException {
      JsonObject payload = new JsonObject();
      payload.addProperty("amount", "500");
      payload.addProperty("transactionId", "tx-2");
      payload.addProperty("paymentMethod", "BANK_TRANSFER");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      when(balanceService.withdrawBalance(100, new BigDecimal("500"), "tx-2",
          PaymentMethod.BANK_TRANSFER))
          .thenReturn(new BigDecimal("4500"));

      ServerResponse res = controller.handleWithdraw(req, "req-1", 100);

      assertTrue(res.isSuccess());
      assertEquals("USER_WITHDRAW", res.getAction());
    }

    @Test
    @DisplayName("Service ném exception → error")
    void serviceLoi() throws SQLException {
      JsonObject payload = new JsonObject();
      payload.addProperty("amount", "500");
      payload.addProperty("transactionId", "tx-2");
      payload.addProperty("paymentMethod", "BANK_TRANSFER");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      when(balanceService.withdrawBalance(anyInt(), any(), any(), any()))
          .thenThrow(new RuntimeException("Không đủ số dư"));

      ServerResponse res = controller.handleWithdraw(req, "req-1", 100);
      assertFalse(res.isSuccess());
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("handleChangePassword()")
  class ChangePasswordTest {

    @Test
    @DisplayName("Change password thành công")
    void changePasswordThanhCong() throws SQLException {
      JsonObject payload = new JsonObject();
      payload.addProperty("oldPassword", "old123");
      payload.addProperty("newPassword", "new456");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      ServerResponse res = controller.handleChangePassword(req, "req-1", 100);

      assertTrue(res.isSuccess());
      assertEquals("CHANGE_PASSWORD", res.getAction());
      verify(passwordService).changePassword(100, "old123", "new456");
    }

    @Test
    @DisplayName("Service ném SQLException → error")
    void sqlError() throws SQLException {
      JsonObject payload = new JsonObject();
      payload.addProperty("oldPassword", "old");
      payload.addProperty("newPassword", "new");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      doThrow(new SQLException("DB lỗi")).when(passwordService)
          .changePassword(anyInt(), anyString(), anyString());

      ServerResponse res = controller.handleChangePassword(req, "req-1", 100);
      assertFalse(res.isSuccess());
    }

    @Test
    @DisplayName("Service ném RuntimeException → error")
    void runtimeError() throws SQLException {
      JsonObject payload = new JsonObject();
      payload.addProperty("oldPassword", "wrong");
      payload.addProperty("newPassword", "new");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      doThrow(new RuntimeException("Sai mật khẩu cũ")).when(passwordService)
          .changePassword(anyInt(), anyString(), anyString());

      ServerResponse res = controller.handleChangePassword(req, "req-1", 100);
      assertFalse(res.isSuccess());
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("handleGetTransactions()")
  class GetTransactionsTest {

    @Test
    @DisplayName("Trả về danh sách giao dịch")
    void thanhCong() throws SQLException {
      when(balanceService.getTransactionRecordByUserId(100))
          .thenReturn(Collections.emptyList());

      JsonObject req = new JsonObject();
      ServerResponse res = controller.handleGetTransactions(req, "req-1", 100);

      assertTrue(res.isSuccess());
      assertEquals("USER_TRANSACTIONS", res.getAction());
    }

    @Test
    @DisplayName("Service lỗi → error")
    void serviceLoi() throws SQLException {
      when(balanceService.getTransactionRecordByUserId(anyInt()))
          .thenThrow(new SQLException("DB lỗi"));

      JsonObject req = new JsonObject();
      ServerResponse res = controller.handleGetTransactions(req, "req-1", 100);
      assertFalse(res.isSuccess());
    }
  }

  @Nested
  @DisplayName("handleUpdateProfile()")
  class UpdateProfileTest {
    @Test
    @DisplayName("Cập nhật hồ sơ thành công")
    void success() throws Exception {
      JsonObject payload = new JsonObject();
      payload.addProperty("fullname", "New Name");
      payload.addProperty("phone", "0123456789");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      ServerResponse res = controller.handleUpdateProfile(req, "req-update-profile", 100);

      assertTrue(res.isSuccess());
      verify(profileService).updateProfile(eq(100), eq(100), eq("New Name"), eq("0123456789"), any(), any());
    }

    @Test
    @DisplayName("Cập nhật hồ sơ thất bại - Lỗi service")
    void fail() throws Exception {
      JsonObject payload = new JsonObject();
      payload.addProperty("fullname", "New Name");
      payload.addProperty("phone", "0123456789");
      JsonObject req = new JsonObject();
      req.add("payload", payload);
      doThrow(new RuntimeException("Lỗi logic")).when(profileService).updateProfile(anyInt(), anyInt(), any(), any(), any(), any());

      ServerResponse res = controller.handleUpdateProfile(req, "req-update-fail", 100);

      assertFalse(res.isSuccess());
    }
  }
}