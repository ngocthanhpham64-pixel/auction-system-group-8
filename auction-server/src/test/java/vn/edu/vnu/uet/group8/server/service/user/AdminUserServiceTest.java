package vn.edu.vnu.uet.group8.server.service.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.UserAdmin;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.AdminLevel;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;
import vn.edu.vnu.uet.group8.common.exception.UnauthorizedException;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.service.auction.AuctionService;

/**
 * Test cho {@link AdminUserService}.
 *
 * <p>Test các method không phụ thuộc DatabaseConnection static: updateUserStatus, cancelAuction.
 * Các method khác (getDashboardStats, getUsers, getAuctions) dùng
 * DatabaseConnection.getInstance() static → khó mock.
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

  @Mock private UserDAO userDAO;
  @Mock private AuctionService auctionService;

  private AdminUserService service;

  @BeforeEach
  void setUp() {
    service = new AdminUserService(userDAO, auctionService);
  }

  private UserMember taoMember(int id, UserStatus status) {
    UserMember m = new UserMember.Builder("user" + id, "u" + id + "@e.com", "hash").build();
    m.assignId(id);
    if (status != UserStatus.ACTIVE) {
      m.setStatus(status);
    }
    return m;
  }

  private UserAdmin taoAdmin(int id, AdminLevel level) {
    return UserAdmin.reconstructor()
        .id(id)
        .createdAt(java.time.Instant.now())
        .isDeleted(false)
        .username("admin" + id)
        .email("adm" + id + "@e.com")
        .fullname("Admin " + id)
        .encryptedPassword("hash")
        .status(UserStatus.ACTIVE)
        .roles(java.util.Set.of(vn.edu.vnu.uet.group8.common.enums.UserRole.ADMIN))
        .lastLogin(java.time.Instant.now())
        .adminLevel(level)
        .build();
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("updateUserStatus() - check quyền")
  class CheckAdminAccess {

    @Test
    @DisplayName("adminId không tồn tại → UserNotFoundException")
    void adminKhongTonTai() throws SQLException {
      when(userDAO.findById(999)).thenReturn(Optional.empty());

      assertThrows(
          UserNotFoundException.class,
          () -> service.updateUserStatus(999, 1, UserStatus.SUSPENDED)
      );
    }

    @Test
    @DisplayName("adminId không phải admin → UnauthorizedException")
    void khongPhaiAdmin() throws SQLException {
      UserMember member = taoMember(50, UserStatus.ACTIVE);
      when(userDAO.findById(50)).thenReturn(Optional.of(member));

      assertThrows(
          UnauthorizedException.class,
          () -> service.updateUserStatus(50, 1, UserStatus.SUSPENDED)
      );
    }

    @Test
    @DisplayName("Admin tự đổi trạng thái mình → ValidationException")
    void tuDoiMinh() throws SQLException {
      UserAdmin admin = taoAdmin(99, AdminLevel.SUPER_ADMIN);
      when(userDAO.findById(99)).thenReturn(Optional.of(admin));

      assertThrows(
          ValidationException.class,
          () -> service.updateUserStatus(99, 99, UserStatus.SUSPENDED)
      );
    }

    @Test
    @DisplayName("Target user không tồn tại → UserNotFoundException")
    void targetKhongTonTai() throws SQLException {
      UserAdmin admin = taoAdmin(99, AdminLevel.SUPER_ADMIN);
      when(userDAO.findById(99)).thenReturn(Optional.of(admin));
      when(userDAO.findById(999)).thenReturn(Optional.empty());

      assertThrows(
          UserNotFoundException.class,
          () -> service.updateUserStatus(99, 999, UserStatus.SUSPENDED)
      );
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("updateUserStatus() - phân quyền MODERATOR vs SUPER_ADMIN")
  class PhanQuyen {

    @Test
    @DisplayName("MODERATOR cố thay đổi Admin khác → UnauthorizedException")
    void modCoThayDoiAdmin() throws SQLException {
      UserAdmin mod = taoAdmin(50, AdminLevel.MODERATOR);
      UserAdmin targetAdmin = taoAdmin(60, AdminLevel.MODERATOR);
      when(userDAO.findById(50)).thenReturn(Optional.of(mod));
      when(userDAO.findById(60)).thenReturn(Optional.of(targetAdmin));

      assertThrows(
          UnauthorizedException.class,
          () -> service.updateUserStatus(50, 60, UserStatus.SUSPENDED)
      );
    }

    @Test
    @DisplayName("SUPER_ADMIN có quyền thay đổi Admin khác")
    void superAdminThayDoiAdmin() throws SQLException {
      UserAdmin superAdmin = taoAdmin(99, AdminLevel.SUPER_ADMIN);
      UserAdmin targetMod = taoAdmin(60, AdminLevel.MODERATOR);
      when(userDAO.findById(99)).thenReturn(Optional.of(superAdmin));
      when(userDAO.findById(60)).thenReturn(Optional.of(targetMod));

      assertDoesNotThrow(
          () -> service.updateUserStatus(99, 60, UserStatus.SUSPENDED)
      );
    }

    @Test
    @DisplayName("MODERATOR cố dùng lệnh BANNED → UnauthorizedException")
    void modBanned() throws SQLException {
      UserAdmin mod = taoAdmin(50, AdminLevel.MODERATOR);
      UserMember target = taoMember(10, UserStatus.ACTIVE);
      when(userDAO.findById(50)).thenReturn(Optional.of(mod));
      when(userDAO.findById(10)).thenReturn(Optional.of(target));

      assertThrows(
          UnauthorizedException.class,
          () -> service.updateUserStatus(50, 10, UserStatus.BANNED)
      );
    }

    @Test
    @DisplayName("SUPER_ADMIN dùng lệnh BANNED → hợp lệ")
    void superAdminBanned() throws SQLException {
      UserAdmin superAdmin = taoAdmin(99, AdminLevel.SUPER_ADMIN);
      UserMember target = taoMember(10, UserStatus.ACTIVE);
      when(userDAO.findById(99)).thenReturn(Optional.of(superAdmin));
      when(userDAO.findById(10)).thenReturn(Optional.of(target));

      assertDoesNotThrow(
          () -> service.updateUserStatus(99, 10, UserStatus.BANNED)
      );
    }

    @Test
    @DisplayName("MODERATOR đổi status Member → hợp lệ (SUSPENDED)")
    void modSuspendMember() throws SQLException {
      UserAdmin mod = taoAdmin(50, AdminLevel.MODERATOR);
      UserMember target = taoMember(10, UserStatus.ACTIVE);
      when(userDAO.findById(50)).thenReturn(Optional.of(mod));
      when(userDAO.findById(10)).thenReturn(Optional.of(target));

      assertDoesNotThrow(
          () -> service.updateUserStatus(50, 10, UserStatus.SUSPENDED)
      );
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("cancelAuction()")
  class CancelAuctionTest {

    @Test
    @DisplayName("Admin hợp lệ → delegate sang auctionService.cancelSession")
    void adminCancelOK() throws SQLException {
      UserAdmin admin = taoAdmin(99, AdminLevel.SUPER_ADMIN);
      when(userDAO.findById(99)).thenReturn(Optional.of(admin));

      service.cancelAuction(99, 5);

      verify(auctionService).cancelSession(5, 99);
    }

    @Test
    @DisplayName("Không phải admin → UnauthorizedException, không gọi service")
    void khongPhaiAdmin() throws SQLException {
      UserMember member = taoMember(50, UserStatus.ACTIVE);
      when(userDAO.findById(50)).thenReturn(Optional.of(member));

      assertThrows(
          UnauthorizedException.class,
          () -> service.cancelAuction(50, 5)
      );

      verifyNoInteractions(auctionService);
    }
  }
}