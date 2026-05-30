package vn.edu.vnu.uet.group8.common.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.vnu.uet.group8.common.enums.UserRole;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;

/** Unit test cho {@link UserMember} entity. */
class UserMemberTest {

  private static final String VALID_USERNAME = "quan_dev";
  private static final String VALID_EMAIL = "quan@example.com";
  private static final String VALID_HASH = "$2a$12$abcdefghijklmnopqrstuv";
  private static final String VALID_PHONE = "0901234567";

  /**
   * Helper: tạo UserMember mẫu để test. Note: dùng .fullname() (n thường) - method của User.Builder
   * (lớp cha). Method .fullName() (N hoa) ở UserMember.Builder có BUG không truyền value lên super.
   */
  private UserMember createMember() {
    return UserMember.builder(VALID_USERNAME, VALID_EMAIL, VALID_HASH)
        .fullname("Phạm Quân") // .fullname() - User.Builder, hoạt động OK
        .phone(VALID_PHONE)
        .balance(new BigDecimal("500000"))
        .build();
  }

  @Nested
  @DisplayName("Builder pattern")
  class BuilderTest {

    @Test
    @DisplayName("Builder hợp lệ tạo UserMember thành công")
    void builderHopLeTaoMember() {
      UserMember member = createMember();
      assertNotNull(member);
      assertEquals(VALID_USERNAME, member.getUsername());
      assertEquals(VALID_EMAIL, member.getEmail());
      assertEquals(VALID_PHONE, member.getPhone());
      assertEquals("Phạm Quân", member.getFullname());
    }

    @Test
    @DisplayName("Username bị normalize - trim + lowercase")
    void usernameDuocNormalize() {
      UserMember member =
          UserMember.builder("  QUAN_Dev  ", VALID_EMAIL, VALID_HASH).phone(VALID_PHONE).build();
      assertEquals("quan_dev", member.getUsername(), "Username phải bị trim + lowercase");
    }

    @Test
    @DisplayName("Email bị normalize - trim + lowercase")
    void emailDuocNormalize() {
      UserMember member =
          UserMember.builder(VALID_USERNAME, "  QUAN@Example.COM  ", VALID_HASH)
              .phone(VALID_PHONE)
              .build();
      assertEquals("quan@example.com", member.getEmail());
    }

    @Test
    @DisplayName("Status mặc định = ACTIVE")
    void statusMacDinhActive() {
      UserMember member = createMember();
      assertEquals(UserStatus.ACTIVE, member.getStatus());
    }

    @Test
    @DisplayName("Role mặc định có BIDDER")
    void roleMacDinhBidder() {
      UserMember member = createMember();
      assertTrue(member.hasRole(UserRole.BIDDER), "UserMember mặc định phải có role BIDDER");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Username null/rỗng/blank phải ném exception")
    void usernameInvalidPhaiNem(String invalid) {
      assertThrows(
          IllegalArgumentException.class,
          () -> UserMember.builder(invalid, VALID_EMAIL, VALID_HASH));
    }

    @Test
    @DisplayName("Email không chứa @ phải ném exception")
    void emailKhongCoAtPhaiNem() {
      assertThrows(
          IllegalArgumentException.class,
          () -> UserMember.builder(VALID_USERNAME, "no-at-sign.com", VALID_HASH));
    }

    @Test
    @DisplayName("Email null phải ném exception")
    void emailNullPhaiNem() {
      assertThrows(
          IllegalArgumentException.class,
          () -> UserMember.builder(VALID_USERNAME, null, VALID_HASH));
    }

    @Test
    @DisplayName("Password null/rỗng phải ném exception")
    void passwordInvalidPhaiNem() {
      assertThrows(
          IllegalArgumentException.class,
          () -> UserMember.builder(VALID_USERNAME, VALID_EMAIL, null));
      assertThrows(
          IllegalArgumentException.class,
          () -> UserMember.builder(VALID_USERNAME, VALID_EMAIL, ""));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Phone null/rỗng/blank khi setter phải ném exception")
    void phoneInvalidPhaiNem(String invalid) {
      assertThrows(
          IllegalArgumentException.class,
          () -> UserMember.builder(VALID_USERNAME, VALID_EMAIL, VALID_HASH).phone(invalid));
    }

    @Test
    @DisplayName("Balance mặc định = 0")
    void balanceMacDinhZero() {
      UserMember member =
          UserMember.builder(VALID_USERNAME, VALID_EMAIL, VALID_HASH).phone(VALID_PHONE).build();
      assertEquals(0, member.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Balance âm bị reject - giữ giá trị mặc định 0")
    void balanceAmBiReject() {
      UserMember member =
          UserMember.builder(VALID_USERNAME, VALID_EMAIL, VALID_HASH)
              .phone(VALID_PHONE)
              .balance(new BigDecimal("-1000"))
              .build();
      // Theo code: balance âm không được set → giữ mặc định 0
      assertEquals(0, member.getBalance().compareTo(BigDecimal.ZERO));
    }
  }

  @Nested
  @DisplayName("Status check")
  class StatusCheck {

    @Test
    @DisplayName("Status ACTIVE → isActive() = true")
    void statusActiveTrue() {
      UserMember member = createMember();
      assertTrue(member.isActive());
    }

    @Test
    @DisplayName("Status SUSPENDED → isActive() = false")
    void statusSuspendedFalse() {
      UserMember member = createMember();
      member.setStatus(UserStatus.SUSPENDED);
      assertFalse(member.isActive());
    }

    @Test
    @DisplayName("Status BANNED → isActive() = false")
    void statusBannedFalse() {
      UserMember member = createMember();
      member.setStatus(UserStatus.BANNED);
      assertFalse(member.isActive());
    }

    @Test
    @DisplayName("setStatus null phải ném exception")
    void setStatusNullPhaiNem() {
      UserMember member = createMember();
      assertThrows(IllegalArgumentException.class, () -> member.setStatus(null));
    }
  }

  @Nested
  @DisplayName("Role logic")
  class RoleLogic {

    @Test
    @DisplayName("UserMember mặc định KHÔNG có role ADMIN")
    void memberKhongCoRoleAdmin() {
      UserMember member = createMember();
      assertFalse(member.hasRole(UserRole.ADMIN));
    }

    @Test
    @DisplayName("isAdmin() luôn return false cho UserMember")
    void isAdminLuonFalse() {
      UserMember member = createMember();
      assertFalse(member.isAdmin(), "UserMember.isAdmin() phải luôn false dù role gì");
    }

    @Test
    @DisplayName("hasRole(BIDDER) phải true sau khi tạo UserMember")
    void hasRoleBidderMacDinh() {
      UserMember member = createMember();
      assertTrue(
          member.hasRole(UserRole.BIDDER),
          "UserMember mặc định phải có role BIDDER (gán trong constructor)");
    }

    @Test
    @DisplayName("hasRole(SELLER) ban đầu false, sau addRole → true")
    void addRoleSellerHopLe() {
      UserMember member = createMember();
      assertFalse(member.hasRole(UserRole.SELLER), "UserMember mặc định không có role SELLER");

      member.addRole(UserRole.SELLER);
      assertTrue(member.hasRole(UserRole.SELLER), "Sau addRole(SELLER) phải có role SELLER");
    }

    @Test
    @DisplayName("removeRole xóa role thành công")
    void removeRoleHopLe() {
      UserMember member = createMember();
      member.addRole(UserRole.SELLER);
      assertTrue(member.hasRole(UserRole.SELLER));

      member.removeRole(UserRole.SELLER);
      assertFalse(member.hasRole(UserRole.SELLER), "Sau removeRole(SELLER) phải hết role SELLER");
    }

    @Test
    @DisplayName("getDisplayRole 'Người mua' khi chỉ có BIDDER")
    void displayRoleNguoiMua() {
      UserMember member = createMember();
      assertEquals("Người mua", member.getDisplayRole());
    }
  }

  @Nested
  @DisplayName("Setters validation")
  class SettersValidation {

    @Test
    @DisplayName("setFullname null phải ném exception")
    void setFullnameNullPhaiNem() {
      UserMember member = createMember();
      assertThrows(IllegalArgumentException.class, () -> member.setFullname(null));
      assertThrows(IllegalArgumentException.class, () -> member.setFullname(""));
      assertThrows(IllegalArgumentException.class, () -> member.setFullname("   "));
    }

    @Test
    @DisplayName("setFullname hợp lệ phải cập nhật")
    void setFullnameHopLe() {
      UserMember member = createMember();
      member.setFullname("Nguyễn Văn A");
      assertEquals("Nguyễn Văn A", member.getFullname());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("setPhone null/rỗng phải ném exception")
    void setPhoneInvalidPhaiNem(String invalid) {
      UserMember member = createMember();
      assertThrows(IllegalArgumentException.class, () -> member.setPhone(invalid));
    }

    @Test
    @DisplayName("setPhone hợp lệ phải bị trim")
    void setPhoneHopLeBiTrim() {
      UserMember member = createMember();
      member.setPhone("   0987654321   ");
      assertEquals("0987654321", member.getPhone());
    }

    @Test
    @DisplayName("setEncryptedPassword null/rỗng phải ném exception")
    void setEncryptedPasswordInvalidPhaiNem() {
      UserMember member = createMember();
      assertThrows(IllegalArgumentException.class, () -> member.setEncryptedPassword(null));
      assertThrows(IllegalArgumentException.class, () -> member.setEncryptedPassword(""));
    }
  }

  @Test
  @DisplayName("getRoles trả về UnmodifiableSet")
  void getRolesImmutable() {
    UserMember member = createMember();
    assertThrows(UnsupportedOperationException.class, () -> member.getRoles().add(UserRole.ADMIN));
  }

  @Test
  @DisplayName("toString chứa username và status")
  void toStringChuaThongTinChinh() {
    UserMember member = createMember();
    String str = member.toString();
    assertTrue(str.contains(VALID_USERNAME));
    assertTrue(str.contains("ACTIVE"));
  }
}