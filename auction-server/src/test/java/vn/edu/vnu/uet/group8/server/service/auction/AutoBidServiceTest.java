package vn.edu.vnu.uet.group8.server.service.auction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.common.entity.AutoBidConfig;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.UserAdmin;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.AdminLevel;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.AutoBidDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;

/**
 * Test cho {@link AutoBidService}.
 *
 * <p>Chỉ test {@code configureAutoBid()}. {@code resolveAutoBids()} có
 * logic đệ quy phức tạp, fixture quá heavy nên bỏ qua.
 */
@ExtendWith(MockitoExtension.class)
class AutoBidServiceTest {

    @Mock private AutoBidDAO autoBidDAO;
    @Mock private BidValidator validator;
    @Mock private BidProcessor processor;
    @Mock private UserDAO userDAO;

    private AutoBidService service;

    @BeforeEach
    void setUp() {
        service = new AutoBidService(autoBidDAO, validator, processor, userDAO);
    }

    private UserMember taoMemberCoBalance(int id, BigDecimal balance) {
        UserMember m = new UserMember.Builder("user" + id, "u" + id + "@e.com", "hash")
                .balance(balance)
                .build();
        m.assignId(id);
        return m;
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("configureAutoBid()")
    class ConfigureTest {

        @Test
        @DisplayName("User không tồn tại → UserNotFoundException")
        void userKhongTonTai() throws SQLException {
            when(userDAO.findById(999)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> service.configureAutoBid(999, 1, new BigDecimal("1000")));
        }

        @Test
        @DisplayName("User là Admin (không phải Member) → ValidationException")
        void userLaAdmin() throws SQLException {
            UserAdmin admin = new UserAdmin.Builder("admin", "a@e.com", "hash",
                    AdminLevel.SUPER_ADMIN).build();
            admin.assignId(99);
            when(userDAO.findById(99)).thenReturn(Optional.of((User) admin));

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.configureAutoBid(99, 1, new BigDecimal("1000")));
            assertTrue(ex.getMessage().toLowerCase().contains("không được phép")
                    || ex.getMessage().toLowerCase().contains("đấu giá"));
        }

        @Test
        @DisplayName("Member không đủ số dư → ValidationException")
        void khongDuSoDu() throws SQLException {
            UserMember member = taoMemberCoBalance(10, new BigDecimal("500"));
            when(userDAO.findById(10)).thenReturn(Optional.of((User) member));

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.configureAutoBid(10, 1, new BigDecimal("1000")));
            assertTrue(ex.getMessage().toLowerCase().contains("không đủ")
                    || ex.getMessage().toLowerCase().contains("số dư"));
        }

        @Test
        @DisplayName("Đủ điều kiện → gọi autoBidDAO.saveConfig")
        void thanhCong() throws SQLException {
            UserMember member = taoMemberCoBalance(10, new BigDecimal("5000"));
            when(userDAO.findById(10)).thenReturn(Optional.of((User) member));

            service.configureAutoBid(10, 1, new BigDecimal("1000"));

            verify(autoBidDAO).saveConfig(any(AutoBidConfig.class));
        }

        @Test
        @DisplayName("Balance = maxPrice (đúng giới hạn) → vẫn hợp lệ")
        void balanceBangMaxPrice() throws SQLException {
            UserMember member = taoMemberCoBalance(10, new BigDecimal("1000"));
            when(userDAO.findById(10)).thenReturn(Optional.of((User) member));

            assertDoesNotThrow(
                    () -> service.configureAutoBid(10, 1, new BigDecimal("1000")));
        }
    }
}