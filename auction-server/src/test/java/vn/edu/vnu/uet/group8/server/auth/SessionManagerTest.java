package vn.edu.vnu.uet.group8.server.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test cho {@link SessionManager} (server-side).
 */
class SessionManagerTest {

  private SessionManager sm;

  @BeforeEach
  void setUp() {
    sm = new SessionManager();
  }

  @Nested
  @DisplayName("createSession()")
  class CreateSession {

    @Test
    @DisplayName("Tạo session mới → trả token UUID không trống")
    void taoSessionMoi() {
      String token = sm.createSession(1);
      assertNotNull(token);
      assertFalse(token.isBlank());
      // UUID format có 36 ký tự (4 dấu gạch)
      assertEquals(36, token.length());
    }

    @Test
    @DisplayName("Hai user khác nhau → hai token khác nhau")
    void haiUserKhacToken() {
      String t1 = sm.createSession(1);
      String t2 = sm.createSession(2);
      assertNotEquals(t1, t2);
    }

    @Test
    @DisplayName("Cùng user gọi createSession 2 lần → token cũ bị invalidate")
    void singleSessionPerUser() {
      String tokenCu = sm.createSession(1);
      String tokenMoi = sm.createSession(1);

      assertNotEquals(tokenCu, tokenMoi, "Token mới phải khác token cũ");
      assertEquals(-1, sm.validateToken(tokenCu),
          "Token cũ phải bị invalidate sau khi login lại");
      assertEquals(1, sm.validateToken(tokenMoi),
          "Token mới hợp lệ");
    }

    @Test
    @DisplayName("Hai lần createSession liên tiếp → 2 UUID khác nhau (uniqueness)")
    void uuidUnique() {
      String t1 = sm.createSession(1);
      String t2 = sm.createSession(2);
      String t3 = sm.createSession(3);
      assertNotEquals(t1, t2);
      assertNotEquals(t2, t3);
      assertNotEquals(t1, t3);
    }
  }

  @Nested
  @DisplayName("validateToken()")
  class ValidateToken {

    @Test
    @DisplayName("Token hợp lệ → trả đúng userId")
    void hopLe() {
      String token = sm.createSession(42);
      assertEquals(42, sm.validateToken(token));
    }

    @Test
    @DisplayName("Token null → trả -1")
    void tokenNull() {
      assertEquals(-1, sm.validateToken(null));
    }

    @Test
    @DisplayName("Token rỗng → trả -1")
    void tokenRong() {
      assertEquals(-1, sm.validateToken(""));
    }

    @Test
    @DisplayName("Token toàn whitespace → trả -1")
    void tokenWhitespace() {
      assertEquals(-1, sm.validateToken("   "));
    }

    @Test
    @DisplayName("Token không tồn tại trong map → trả -1")
    void tokenKhongTonTai() {
      assertEquals(-1, sm.validateToken("fake-token-xyz"));
    }
  }

  @Nested
  @DisplayName("invalidateToken()")
  class InvalidateToken {

    @Test
    @DisplayName("Sau invalidate → validateToken trả -1")
    void sauInvalidateLaInvalid() {
      String token = sm.createSession(5);
      sm.invalidateToken(token);
      assertEquals(-1, sm.validateToken(token));
    }

    @Test
    @DisplayName("invalidateToken(null) → không lỗi")
    void invalidateNull() {
      assertDoesNotThrow(() -> sm.invalidateToken(null));
    }

    @Test
    @DisplayName("invalidate token không tồn tại → không lỗi")
    void invalidateKhongTonTai() {
      assertDoesNotThrow(() -> sm.invalidateToken("fake"));
    }

    @Test
    @DisplayName("Sau invalidateToken → user có thể tạo session mới")
    void sauInvalidateTaoMoi() {
      sm.createSession(1);
      sm.invalidateToken(sm.createSession(1)); // recreate + invalidate
      String tokenMoi = sm.createSession(1);
      assertEquals(1, sm.validateToken(tokenMoi));
    }
  }

  @Nested
  @DisplayName("invalidateUser() - admin force logout")
  class InvalidateUser {

    @Test
    @DisplayName("Force logout → token user đó bị xoá")
    void forceLogout() {
      String token = sm.createSession(100);
      sm.invalidateUser(100);
      assertEquals(-1, sm.validateToken(token));
    }

    @Test
    @DisplayName("invalidateUser khi user chưa login → không lỗi")
    void invalidateUserChuaLogin() {
      assertDoesNotThrow(() -> sm.invalidateUser(999));
    }

    @Test
    @DisplayName("invalidateUser chỉ ảnh hưởng user đó, các user khác vẫn login")
    void chiAnhHuongUserDo() {
      String t1 = sm.createSession(1);
      String t2 = sm.createSession(2);
      sm.invalidateUser(1);
      assertEquals(-1, sm.validateToken(t1));
      assertEquals(2, sm.validateToken(t2), "User 2 vẫn login");
    }
  }

  @Nested
  @DisplayName("activeSessionCount()")
  class ActiveSessionCount {

    @Test
    @DisplayName("Ban đầu count = 0")
    void countBanDau() {
      assertEquals(0, sm.activeSessionCount());
    }

    @Test
    @DisplayName("Sau 3 user login khác nhau → count = 3")
    void count3User() {
      sm.createSession(1);
      sm.createSession(2);
      sm.createSession(3);
      assertEquals(3, sm.activeSessionCount());
    }

    @Test
    @DisplayName("Cùng user login 2 lần → count vẫn = 1 (single session)")
    void cungUserLogin2Lan() {
      sm.createSession(1);
      sm.createSession(1);
      assertEquals(1, sm.activeSessionCount());
    }

    @Test
    @DisplayName("Sau logout → count giảm")
    void sauLogout() {
      String token = sm.createSession(1);
      assertEquals(1, sm.activeSessionCount());
      sm.invalidateToken(token);
      assertEquals(0, sm.activeSessionCount());
    }
  }
}