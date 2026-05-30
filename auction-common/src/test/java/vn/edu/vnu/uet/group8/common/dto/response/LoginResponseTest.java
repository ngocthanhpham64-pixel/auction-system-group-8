package vn.edu.vnu.uet.group8.common.dto.response;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test cho {@link LoginResponse} - 2 factory method (success, failure).
 */
class LoginResponseTest {

    @Test
    @DisplayName("success() - thành công với đủ thông tin")
    void taoSuccess() {
        LoginResponse res = LoginResponse.success(
                1, "quan", "Pham Anh Quan", "quan@example.com", "Member", "token-abc");
        assertTrue(res.isSuccess());
        assertEquals("Đăng nhập thành công", res.getMessage());
        assertEquals(1, res.getUserId());
        assertEquals("quan", res.getUsername());
        assertEquals("Pham Anh Quan", res.getFullName());
        assertEquals("quan@example.com", res.getEmail());
        assertEquals("Member", res.getRole());
        assertEquals("token-abc", res.getAuthToken());
    }

    @Test
    @DisplayName("failure() - thất bại với reason")
    void taoFailure() {
        LoginResponse res = LoginResponse.failure("Sai mật khẩu");
        assertFalse(res.isSuccess());
        assertEquals("Sai mật khẩu", res.getMessage());
        assertEquals(0, res.getUserId());
        assertNull(res.getUsername());
        assertNull(res.getFullName());
        assertNull(res.getEmail());
        assertNull(res.getRole());
        assertNull(res.getAuthToken());
    }

    @Test
    @DisplayName("toString() KHÔNG chứa authToken (bảo mật)")
    void toStringKhongCoToken() {
        LoginResponse res = LoginResponse.success(
                1, "quan", "Quan", "q@e.com", "Member", "VERY-SECRET-TOKEN");
        String s = res.toString();
        assertFalse(s.contains("VERY-SECRET-TOKEN"),
                "toString() KHÔNG được lộ authToken");
    }

    @Test
    @DisplayName("toString() chứa userId, username, role")
    void toStringDayDu() {
        LoginResponse res = LoginResponse.success(
                99, "alice", "Alice", "a@e.com", "Admin", "token");
        String s = res.toString();
        assertTrue(s.contains("99"));
        assertTrue(s.contains("alice"));
        assertTrue(s.contains("Admin"));
    }
}