package vn.edu.vnu.uet.group8.server.service.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.PaymentMethod;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.UserServiceException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.TransactionDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

  @Mock private UserDAO userDAO;

  @Mock private TransactionDAO transactionDAO;

  @InjectMocks private BalanceService balanceService;

  private UserMember taoMember(BigDecimal balance) {
    UserMember m = new UserMember.Builder("alice", "a@e.com", "hash").balance(balance).build();
    m.assignId(1);
    return m;
  }

  @Nested
  @DisplayName("topUpBalance - validate input")
  class TopUpValidate {

    @Test
    @DisplayName("transactionId null")
    void txIdNull() {
      assertThrows(
          ValidationException.class,
          () ->
              balanceService.topUpBalance(
                  1, new BigDecimal("10000"), null, PaymentMethod.COD));
    }

    @Test
    @DisplayName("transactionId blank")
    void txIdBlank() {
      assertThrows(
          ValidationException.class,
          () ->
              balanceService.topUpBalance(
                  1, new BigDecimal("10000"), "   ", PaymentMethod.COD));
    }

    @Test
    @DisplayName("Amount không hợp lệ")
    void amountKhongHopLe() {
      assertThrows(
          UserServiceException.class,
          () -> balanceService.topUpBalance(1, BigDecimal.ZERO, "tx-1", PaymentMethod.COD));
    }
  }

  @Nested
  @DisplayName("topUpBalance - user check")
  class TopUpUserCheck {

    @Test
    @DisplayName("User không tồn tại")
    void userKhongTonTai() throws SQLException {
      when(userDAO.findById(999)).thenReturn(Optional.empty());

      assertThrows(
          UserNotFoundException.class,
          () ->
              balanceService.topUpBalance(
                  999, new BigDecimal("10000"), "tx-1", PaymentMethod.COD));
    }

    @Test
    @DisplayName("Vượt balance max")
    void vuotBalanceMax() throws SQLException {
      UserMember member = taoMember(new BigDecimal("9999000000"));
      when(userDAO.findById(1)).thenReturn(Optional.of(member));

      assertThrows(
          ValidationException.class,
          () ->
              balanceService.topUpBalance(
                  1, new BigDecimal("2000000"), "tx-1", PaymentMethod.COD));
    }
  }

  @Nested
  @DisplayName("topUpBalance - success")
  class TopUpSuccess {

    @Test
    @DisplayName("Top up thành công")
    void thanhCong() throws SQLException {
      UserMember member = taoMember(new BigDecimal("100000"));
      when(userDAO.findById(1)).thenReturn(Optional.of(member));

      when(userDAO.insertTransactionAndUpdateBalance(
              eq("tx-1"), eq(1), eq(new BigDecimal("50000")), any(), any()))
          .thenReturn(new BigDecimal("150000"));

      BigDecimal newBalance =
          balanceService.topUpBalance(1, new BigDecimal("50000"), "tx-1", PaymentMethod.COD);

      assertEquals(new BigDecimal("150000"), newBalance);

      verify(userDAO)
          .insertTransactionAndUpdateBalance(
              eq("tx-1"), eq(1), eq(new BigDecimal("50000")), any(), any());
    }
  }

  @Nested
  @DisplayName("withdrawBalance - validate")
  class WithdrawValidate {

    @Test
    @DisplayName("transactionId null")
    void txIdNull() {
      assertThrows(
          ValidationException.class,
          () ->
              balanceService.withdrawBalance(
                  1, new BigDecimal("10000"), null, PaymentMethod.BANK_TRANSFER));
    }

    @Test
    @DisplayName("User không tồn tại")
    void userKhongTonTai() throws SQLException {
      when(userDAO.findById(999)).thenReturn(Optional.empty());

      assertThrows(
          UserNotFoundException.class,
          () ->
              balanceService.withdrawBalance(
                  999, new BigDecimal("10000"), "tx-1", PaymentMethod.BANK_TRANSFER));
    }

    @Test
    @DisplayName("Rút quá số dư")
    void rutQuaSoDuId() throws SQLException {
      UserMember member = taoMember(new BigDecimal("10000"));
      when(userDAO.findById(1)).thenReturn(Optional.of(member));

      assertThrows(
          ValidationException.class,
          () ->
              balanceService.withdrawBalance(
                  1, new BigDecimal("50000"), "tx-1", PaymentMethod.BANK_TRANSFER));
    }
  }

  @Nested
  @DisplayName("withdrawBalance - success")
  class WithdrawSuccess {

    @Test
    @DisplayName("Withdraw thành công")
    void thanhCong() throws SQLException {
      UserMember member = taoMember(new BigDecimal("100000"));
      when(userDAO.findById(1)).thenReturn(Optional.of(member));

      when(userDAO.insertTransactionAndUpdateBalance(
              eq("tx-1"), eq(1), eq(new BigDecimal("-50000")), any(), any()))
          .thenReturn(new BigDecimal("50000"));

      BigDecimal newBalance =
          balanceService.withdrawBalance(
              1, new BigDecimal("50000"), "tx-1", PaymentMethod.BANK_TRANSFER);

      assertEquals(new BigDecimal("50000"), newBalance);

      verify(userDAO)
          .insertTransactionAndUpdateBalance(
              eq("tx-1"), eq(1), eq(new BigDecimal("-50000")), any(), any());
    }
  }

  @Nested
  @DisplayName("getTransactionRecordByUserId")
  class GetTransactions {

    @Test
    @DisplayName("Delegate xuống transactionDAO")
    void delegateDAO() throws SQLException {
      when(transactionDAO.getTransactionsByUserId(1)).thenReturn(java.util.List.of());

      balanceService.getTransactionRecordByUserId(1);

      verify(transactionDAO).getTransactionsByUserId(1);
    }
  }
}