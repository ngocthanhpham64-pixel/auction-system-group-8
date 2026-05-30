package vn.edu.vnu.uet.group8.common.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vn.edu.vnu.uet.group8.common.enums.AdminLevel;
import vn.edu.vnu.uet.group8.common.enums.UserRole;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;

/** Test NÂNG CAO cho User entity hierarchy (User → UserMember / UserAdmin). */
@DisplayName("User Advanced - Business Logic Tests")
class UserAdvancedTest {

  private UserMember createMember() {
    return UserMember.builder("alice", "alice@example.com", "$2a$hash")
        .phone("0901111111")
        .fullname("Alice Nguyen")
        .balance(new BigDecimal("1000000"))
        .build();
  }

  private UserAdmin createModerator() {
    return new UserAdmin.Builder("mod1", "mod@example.com", "$2a$hash", AdminLevel.MODERATOR)
        .build();
  }

  private UserAdmin createSuperAdmin() {
    return new UserAdmin.Builder("super", "super@example.com", "$2a$hash", AdminLevel.SUPER_ADMIN)
        .build();
  }

  @Nested
  @DisplayName("recordLogin() - ghi nhận thời điểm đăng nhập")
  class RecordLogin {

    @Test
    @DisplayName("lastLogin = null trước khi login lần đầu")
    void lastLoginNullTruocKhiLogin() {
      UserMember m = createMember();
      assertNull(m.getLastLogin(), "Chưa login → lastLogin phải null");
    }

    @Test
    @DisplayName("recordLogin() cập nhật lastLogin thành công")
    void recordLoginCapNhatLastLogin() {
      UserMember m = createMember();
      Instant before = Instant.now().minusSeconds(1);
      m.recordLogin();
      Instant after = Instant.now().plusSeconds(1);

      assertNotNull(m.getLastLogin());
      assertTrue(m.getLastLogin().isAfter(before));
      assertTrue(m.getLastLogin().isBefore(after));
    }

    @Test
    @DisplayName("recordLogin() nhiều lần - lastLogin luôn cập nhật")
    void recordLoginNhieuLan() throws InterruptedException {
      UserMember m = createMember();
      m.recordLogin();
      Instant firstLogin = m.getLastLogin();

      Thread.sleep(10); // đảm bảo khác nhau
      m.recordLogin();
      Instant secondLogin = m.getLastLogin();

      assertTrue(secondLogin.isAfter(firstLogin), "Login lần 2 phải có lastLogin mới hơn lần 1");
    }

    @Test
    @DisplayName("recordLogin() hoạt động với UserAdmin")
    void recordLoginUserAdmin() {
      UserAdmin admin = createModerator();
      assertNull(admin.getLastLogin());

      admin.recordLogin();
      assertNotNull(admin.getLastLogin());
    }
  }

  @Nested
  @DisplayName("Role Management - addRole/removeRole nâng cao")
  class RoleManagementAdvanced {

    @Test
    @DisplayName("removeRole khi chỉ còn 1 role → ném IllegalStateException")
    void removeRoleConMot() {
      UserMember m = createMember();
      assertEquals(1, m.getRoles().size());
      assertThrows(
          IllegalStateException.class,
          () -> m.removeRole(UserRole.BIDDER),
          "Không được xóa role khi chỉ còn 1");
    }

    @Test
    @DisplayName("addRole SELLER → có cả BIDDER và SELLER")
    void addRoleSeller() {
      UserMember m = createMember();
      m.addRole(UserRole.SELLER);

      assertTrue(m.hasRole(UserRole.BIDDER));
      assertTrue(m.hasRole(UserRole.SELLER));
      assertEquals(2, m.getRoles().size());
    }

    @Test
    @DisplayName("addRole null → ném IllegalArgumentException")
    void addRoleNull() {
      UserMember m = createMember();
      assertThrows(IllegalArgumentException.class, () -> m.addRole(null));
    }

    @Test
    @DisplayName("addRole BIDDER khi đã có BIDDER - không tăng size (EnumSet không trùng)")
    void addRoleKhiDaCo() {
      UserMember m = createMember();
      assertTrue(m.hasRole(UserRole.BIDDER));
      m.addRole(UserRole.BIDDER); // thêm role đã có
      assertEquals(1, m.getRoles().size());
    }

    @Test
    @DisplayName("removeRole SELLER sau khi có SELLER + BIDDER → chỉ còn BIDDER")
    void removeRoleSellerConBidder() {
      UserMember m = createMember();
      m.addRole(UserRole.SELLER);
      m.removeRole(UserRole.SELLER);

      assertFalse(m.hasRole(UserRole.SELLER));
      assertTrue(m.hasRole(UserRole.BIDDER));
      assertEquals(1, m.getRoles().size());
    }

    @Test
    @DisplayName("removeRole role không tồn tại")
    void removeRoleKhongTonTai() {
      UserMember user = createMember();
      assertThrows(IllegalStateException.class, () -> user.removeRole(UserRole.BIDDER));
    }

    @ParameterizedTest
    @EnumSource(
        value = UserRole.class,
        names = {"BIDDER", "SELLER"})
    @DisplayName("addRole non-ADMIN hoạt động với UserMember")
    void addRoleNonAdmin(UserRole role) {
      UserMember m = createMember();
      m.addRole(role);
      assertTrue(m.hasRole(role));
    }
  }

  @Nested
  @DisplayName("isActive() - kết hợp status + soft delete")
  class IsActiveAdvanced {

    @Test
    @DisplayName("ACTIVE nhưng isDeleted = true → isActive() = false")
    void activeButSoftDeleted() {
      UserMember m = createMember();
      assertTrue(m.isActive());

      m.markAsDeleted();
      assertFalse(
          m.isActive(), "User bị xóa mềm không được tính là active dù status = ACTIVE");
    }

    @Test
    @DisplayName("Restore sau soft delete + ACTIVE → isActive() = true")
    void restoreAndActive() {
      UserMember m = createMember();
      m.markAsDeleted();
      assertFalse(m.isActive());

      m.restore();
      assertTrue(m.isActive(), "Sau restore + ACTIVE → phải active lại");
    }

    @Test
    @DisplayName("SUSPENDED + không bị xóa → isActive() = false")
    void suspendedNotDeleted() {
      UserMember m = createMember();
      m.setStatus(UserStatus.SUSPENDED);
      assertFalse(m.isActive());
    }

    @Test
    @DisplayName("BANNED + không bị xóa → isActive() = false")
    void bannedNotDeleted() {
      UserMember m = createMember();
      m.setStatus(UserStatus.BANNED);
      assertFalse(m.isActive());
    }

    @Test
    @DisplayName("ACTIVE → SUSPENDED → ACTIVE lại → isActive() cycle")
    void statusCycle() {
      UserMember m = createMember();
      assertTrue(m.isActive());

      m.setStatus(UserStatus.SUSPENDED);
      assertFalse(m.isActive());

      m.setStatus(UserStatus.ACTIVE);
      assertTrue(m.isActive(), "Kích hoạt lại từ SUSPENDED → phải active");
    }
  }

  @Nested
  @DisplayName("getDisplayRole() - tất cả kịch bản UserMember")
  class GetDisplayRoleAdvanced {

    @Test
    @DisplayName("Chỉ có BIDDER → 'Người mua'")
    void onlyBidder() {
      UserMember m = createMember();
      assertEquals("Người mua", m.getDisplayRole());
    }

    @Test
    @DisplayName("Có cả BIDDER + SELLER → 'Người mua & Người bán'")
    void bidderAndSeller() {
      UserMember m = createMember();
      m.addRole(UserRole.SELLER);
      assertEquals("Người mua & Người bán", m.getDisplayRole());
    }

    @Test
    @DisplayName("Chỉ có SELLER (sau khi remove BIDDER) → 'Người bán'")
    void onlySeller() {
      UserMember m = createMember();
      m.addRole(UserRole.SELLER); // BIDDER + SELLER
      m.removeRole(UserRole.BIDDER); // chỉ còn SELLER
      assertEquals("Người bán", m.getDisplayRole());
    }

    @Test
    @DisplayName("UserAdmin MODERATOR → 'MODERATOR'")
    void adminModeratorDisplay() {
      assertEquals("MODERATOR", createModerator().getDisplayRole());
    }

    @Test
    @DisplayName("UserAdmin SUPER_ADMIN → 'SUPER_ADMIN'")
    void adminSuperDisplay() {
      assertEquals("SUPER_ADMIN", createSuperAdmin().getDisplayRole());
    }
  }

  @Nested
  @DisplayName("setEncryptedPassword() - đổi mật khẩu có kiểm soát")
  class SetEncryptedPassword {

    @Test
    @DisplayName("setEncryptedPassword() hợp lệ - cập nhật thành công")
    void setPasswordHopLe() {
      UserMember m = createMember();
      String newHash = "$2a$12$newHashedPassword";
      m.setEncryptedPassword(newHash);
      assertEquals(newHash, m.getEncryptedPassword());
    }

    @Test
    @DisplayName("setEncryptedPassword(null) - ném IllegalArgumentException")
    void setPasswordNull() {
      UserMember m = createMember();
      assertThrows(IllegalArgumentException.class, () -> m.setEncryptedPassword(null));
    }

    @Test
    @DisplayName("setEncryptedPassword('') - ném IllegalArgumentException")
    void setPasswordRong() {
      UserMember m = createMember();
      assertThrows(IllegalArgumentException.class, () -> m.setEncryptedPassword(""));
    }

    @Test
    @DisplayName("setEncryptedPassword('   ') blank - ném IllegalArgumentException")
    void setPasswordBlank() {
      UserMember m = createMember();
      assertThrows(IllegalArgumentException.class, () -> m.setEncryptedPassword("   "));
    }

    @Test
    @DisplayName("Đổi password nhiều lần - giữ giá trị cuối")
    void setPasswordNhieuLan() {
      UserMember m = createMember();
      m.setEncryptedPassword("$hash1");
      m.setEncryptedPassword("$hash2");
      m.setEncryptedPassword("$hash3");
      assertEquals("$hash3", m.getEncryptedPassword());
    }

    @Test
    @DisplayName("setEncryptedPassword cũng hoạt động với UserAdmin")
    void setPasswordUserAdmin() {
      UserAdmin admin = createModerator();
      admin.setEncryptedPassword("$2a$new_admin_hash");
      assertEquals("$2a$new_admin_hash", admin.getEncryptedPassword());
    }
  }

  @Nested
  @DisplayName("UserMember Reconstructor - nạp từ DB nâng cao")
  class UserMemberReconstructorAdvanced {

    private UserMember.Reconstructor validRecon() {
      return UserMember.reconstructor()
          .id(1)
          .createdAt(Instant.now())
          .isDeleted(false)
          .username("alice")
          .email("alice@example.com")
          .encryptedPassword("$hash")
          .status(UserStatus.ACTIVE)
          .roles(EnumSet.of(UserRole.BIDDER))
          .balance(new BigDecimal("500000"))
          .phone("0901234567");
    }

    @Test
    @DisplayName("Reconstructor đầy đủ tất cả field - build thành công")
    void reconstructorDayDu() {
      Instant now = Instant.now();
      UserMember m =
          UserMember.reconstructor()
              .id(50)
              .createdAt(now)
              .isDeleted(false)
              .username("bob")
              .email("bob@example.com")
              .encryptedPassword("$hash")
              .status(UserStatus.ACTIVE)
              .roles(EnumSet.of(UserRole.BIDDER, UserRole.SELLER))
              .balance(new BigDecimal("2000000"))
              .phone("0987654321")
              .fullname("Bob Tran")
              .address("123 Trần Hưng Đạo, Hà Nội")
              .avatarUrl("https://cdn.example.com/avatar.jpg")
              .totalBidsPlaced(10)
              .totalItemsSold(3)
              .sellerRating(new BigDecimal("4.5"))
              .lastLogin(now)
              .build();

      assertEquals(50, m.getId());
      assertEquals("bob", m.getUsername());
      assertEquals("bob@example.com", m.getEmail());
      assertEquals("Bob Tran", m.getFullname());
      assertEquals("0987654321", m.getPhone());
      assertEquals("123 Trần Hưng Đạo, Hà Nội", m.getAddress());
      assertEquals(0, m.getSellerRating().compareTo(new BigDecimal("4.5")));
      assertEquals(10, m.getTotalBidsPlaced());
      assertEquals(3, m.getTotalItemsSold());
      assertTrue(m.hasRole(UserRole.SELLER));
    }

    @Test
    @DisplayName("Reconstructor thiếu balance - ném IllegalStateException")
    void reconstructorThieuBalance() {
      assertThrows(
          IllegalStateException.class,
          () ->
              UserMember.reconstructor()
                  .id(1)
                  .createdAt(Instant.now())
                  .isDeleted(false)
                  .username("x")
                  .email("x@x.com")
                  .encryptedPassword("$h")
                  .status(UserStatus.ACTIVE)
                  .roles(EnumSet.of(UserRole.BIDDER))
                  .build());
    }

    @Test
    @DisplayName("Reconstructor thiếu phone - ném IllegalStateException")
    void reconstructorThieuPhone() {
      assertThrows(
          IllegalStateException.class,
          () ->
              UserMember.reconstructor()
                  .id(1)
                  .createdAt(Instant.now())
                  .isDeleted(false)
                  .username("x")
                  .email("x@x.com")
                  .encryptedPassword("$h")
                  .status(UserStatus.ACTIVE)
                  .roles(EnumSet.of(UserRole.BIDDER))
                  .balance(BigDecimal.ZERO)
                  .build());
    }

    @Test
    @DisplayName("Reconstructor lastLogin = null - user chưa login lần nào")
    void reconstructorLastLoginNull() {
      UserMember m = validRecon().lastLogin(null).build();
      assertNull(m.getLastLogin());
    }

    @Test
    @DisplayName("Reconstructor sellerRating = null - chưa có đánh giá")
    void reconstructorSellerRatingNull() {
      UserMember m = validRecon().sellerRating(null).build();
      assertNull(m.getSellerRating());
    }

    @Test
    @DisplayName("Reconstructor avatarUrl = null - không có ảnh đại diện")
    void reconstructorAvatarUrlNull() {
      UserMember m = validRecon().avatarUrl(null).build();
      assertNull(m.getAvatarUrl());
    }

    @Test
    @DisplayName("Reconstructor isDeleted = true - user đã bị xóa mềm")
    void reconstructorSoftDeleted() {
      UserMember m = validRecon().isDeleted(true).build();
      assertTrue(m.isDeleted());
      assertFalse(m.isActive(), "User xóa mềm không được active");
    }

    @Test
    @DisplayName("Reconstructor status SUSPENDED - user bị tạm khóa")
    void reconstructorSuspended() {
      UserMember m = validRecon().status(UserStatus.SUSPENDED).build();
      assertEquals(UserStatus.SUSPENDED, m.getStatus());
      assertFalse(m.isActive());
    }
  }

  @Nested
  @DisplayName("UserMember - setAddress và setAvatarUrl")
  class AddressAndAvatar {

    @Test
    @DisplayName("setAddress hợp lệ - cập nhật và trim")
    void setAddressHopLe() {
      UserMember m = createMember();
      m.setAddress("  123 Hà Nội  ");
      assertEquals("123 Hà Nội", m.getAddress());
    }

    @Test
    @DisplayName("setAddress null - set thành chuỗi rỗng")
    void setAddressNull() {
      UserMember m = createMember();
      m.setAddress(null);
      assertEquals("", m.getAddress());
    }

    @Test
    @DisplayName("setAddress rỗng - set thành chuỗi rỗng")
    void setAddressRong() {
      UserMember m = createMember();
      m.setAddress("");
      assertEquals("", m.getAddress());
    }

    @Test
    @DisplayName("setAvatarUrl hợp lệ - cập nhật URL")
    void setAvatarUrlHopLe() {
      UserMember m = createMember();
      m.setAvatarUrl("https://cdn.example.com/avatar.png");
      assertEquals("https://cdn.example.com/avatar.png", m.getAvatarUrl());
    }

    @Test
    @DisplayName("setAvatarUrl null - không ném exception")
    void setAvatarUrlNull() {
      UserMember m = createMember();
      assertDoesNotThrow(() -> m.setAvatarUrl(null));
      assertNull(m.getAvatarUrl());
    }
  }

  @Nested
  @DisplayName("Đa hình (Polymorphism) - Sealed class User")
  class Polymorphism {

    @Test
    @DisplayName("User reference trỏ UserMember - isAdmin() = false")
    void userRefMember() {
      User user = createMember();
      assertFalse(user.isAdmin());
      assertInstanceOf(UserMember.class, user);
    }

    @Test
    @DisplayName("User reference trỏ UserAdmin - isAdmin() = true")
    void userRefAdmin() {
      User user = createModerator();
      assertTrue(user.isAdmin());
      assertInstanceOf(UserAdmin.class, user);
    }

    @Test
    @DisplayName("instanceof check - UserMember không phải UserAdmin")
    void instanceOfCheck() {
      User m = createMember();
      User a = createModerator();

      assertFalse(m instanceof UserAdmin);
      assertFalse(a instanceof UserMember);

      assertTrue(m instanceof User);
      assertTrue(a instanceof User);
    }

    @Test
    @DisplayName("Danh sách User gồm cả Member và Admin - phân biệt được")
    void danhSachHonHop() {
      User[] users = {createMember(), createModerator(), createMember(), createSuperAdmin()};

      long adminCount = 0, memberCount = 0;
      for (User u : users) {
        if (u.isAdmin()) {
          adminCount++;
        } else {
          memberCount++;
        }
      }

      assertEquals(2, adminCount);
      assertEquals(2, memberCount);
    }

    @Test
    @DisplayName("User.hasRole() hoạt động đúng cho cả hai kiểu")
    void hasRoleChoHaiKieu() {
      User member = createMember();
      User admin = createModerator();

      assertTrue(member.hasRole(UserRole.BIDDER));
      assertFalse(member.hasRole(UserRole.ADMIN));

      assertTrue(admin.hasRole(UserRole.ADMIN));
      assertFalse(admin.hasRole(UserRole.BIDDER));
    }
  }

  @Nested
  @DisplayName("Username và Email normalization")
  class Normalization {

    @Test
    @DisplayName("Username có khoảng trắng và hoa → trim + lowercase")
    void usernameNormalize() {
      UserMember m =
          UserMember.builder("  ALICE_DEV  ", "a@e.com", "$hash").phone("0901234567").build();
      assertEquals("alice_dev", m.getUsername());
    }

    @Test
    @DisplayName("Email có khoảng trắng và hoa → trim + lowercase")
    void emailNormalize() {
      UserMember m =
          UserMember.builder("alice", "  ALICE@EXAMPLE.COM  ", "$hash").phone("0901234567").build();
      assertEquals("alice@example.com", m.getEmail());
    }

    @Test
    @DisplayName("Username đã lowercase sẵn - không bị thay đổi")
    void usernameKhongBiThayDoiNeudaLower() {
      UserMember m =
          UserMember.builder("alice_123", "a@e.com", "$hash").phone("0901234567").build();
      assertEquals("alice_123", m.getUsername());
    }

    @Test
    @DisplayName("Email không có @ → ném IllegalArgumentException")
    void emailKhongCoAt() {
      assertThrows(
          IllegalArgumentException.class,
          () -> UserMember.builder("user", "invalidemail.com", "$hash"));
    }
  }

  @Nested
  @DisplayName("Balance - UserMember")
  class BalanceTest {

    @Test
    @DisplayName("balance mặc định = 0 khi không set")
    void balanceMacDinhZero() {
      UserMember m = UserMember.builder("u", "u@e.com", "$h").phone("0901234567").build();
      assertEquals(0, m.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("balance được set qua Builder")
    void balanceDuocSet() {
      UserMember m =
          UserMember.builder("u", "u@e.com", "$h")
              .phone("0901234567")
              .balance(new BigDecimal("2000000"))
              .build();
      assertEquals(0, m.getBalance().compareTo(new BigDecimal("2000000")));
    }

    @Test
    @DisplayName("balance âm trong Builder → bị reject, giữ ZERO")
    void balanceAmBiReject() {
      UserMember m =
          UserMember.builder("u", "u@e.com", "$h")
              .phone("0901234567")
              .balance(new BigDecimal("-500"))
              .build();
      assertEquals(
          0, m.getBalance().compareTo(BigDecimal.ZERO), "Balance âm phải bị reject, giữ mặc định ZERO");
    }

    @Test
    @DisplayName("balance null trong Builder → giữ ZERO mặc định")
    void balanceNullGiuZero() {
      UserMember m =
          UserMember.builder("u", "u@e.com", "$h")
              .phone("0901234567")
              .balance((BigDecimal) null)
              .build();
      assertEquals(0, m.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("balance = 0 trong Builder → không reject (biên hợp lệ)")
    void balanceZeroBienHopLe() {
      UserMember m =
          UserMember.builder("u", "u@e.com", "$h")
              .phone("0901234567")
              .balance(BigDecimal.ZERO)
              .build();
      assertEquals(0, m.getBalance().compareTo(BigDecimal.ZERO));
    }
  }
}