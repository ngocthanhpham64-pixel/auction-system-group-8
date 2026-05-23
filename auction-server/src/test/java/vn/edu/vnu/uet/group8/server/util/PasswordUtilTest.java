package vn.edu.vnu.uet.group8.server.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test cho {@link PasswordUtil} - BCrypt hashing.
 *
 * <h3>Đặc tính của BCrypt cần verify:</h3>
 * <ul>
 *   <li><b>One-way:</b> không reverse hash thành plain text</li>
 *   <li><b>Salt ngẫu nhiên:</b> hash 2 lần cùng password ra 2 chuỗi khác nhau</li>
 *   <li><b>Verify deterministic:</b> với cùng (plain, hash) verify luôn trả cùng kết quả</li>
 *   <li><b>Format chuẩn:</b> chuỗi 60 ký tự, bắt đầu bằng {@code $2a$12$}</li>
 * </ul>
 *
 * <p><b>Lưu ý:</b> BCrypt cost=12 chạy khá chậm (~250ms/lần)
 * → test này chạy ~3-5 giây tổng, đó là bình thường.
 */
class PasswordUtilTest {

    // ════════════════════════════════════════════════════
    // HAPPY PATH
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("Hash xong verify đúng password phải trả về true")
    void hashRoiVerifyDungPhaiTrue() {
        String plain = "MyP@ssw0rd";
        String hash = PasswordUtil.hash(plain);
        assertTrue(PasswordUtil.verify(plain, hash));
    }

    @Test
    @DisplayName("Hash xong verify sai password phải trả về false")
    void hashRoiVerifySaiPhaiFalse() {
        String plain = "MyP@ssw0rd";
        String hash = PasswordUtil.hash(plain);
        assertFalse(PasswordUtil.verify("WrongPassword", hash));
    }

    // ════════════════════════════════════════════════════
    // BCRYPT FORMAT
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("Hash phải có độ dài 60 ký tự theo chuẩn BCrypt")
    void hashPhaiCo60KyTu() {
        String hash = PasswordUtil.hash("AnyPassword123");
        assertEquals(60, hash.length(), "BCrypt hash chuẩn dài đúng 60 ký tự");
    }

    @Test
    @DisplayName("Hash phải bắt đầu bằng prefix BCrypt $2a$ hoặc $2b$")
    void hashPhaiBatDauBangPrefix() {
        String hash = PasswordUtil.hash("AnyPassword123");
        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"),
                "BCrypt hash phải bắt đầu bằng $2a$, $2b$ hoặc $2y$. Thực tế: " + hash);
    }

    @Test
    @DisplayName("Hash phải chứa cost factor 12")
    void hashPhaiChuaCostFactor12() {
        String hash = PasswordUtil.hash("AnyPassword123");
        // Format: $2a$12$... → ký tự thứ 4-6 là "$12"
        assertTrue(hash.startsWith("$2a$12$") || hash.startsWith("$2b$12$") || hash.startsWith("$2y$12$"),
                "Cost factor 12 phải nằm trong hash. Thực tế: " + hash);
    }

    // ════════════════════════════════════════════════════
    // SALT RANDOMNESS
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("Hash 2 lần cùng password phải ra 2 chuỗi khác nhau (do salt ngẫu nhiên)")
    void hash2LanCungPasswordKhacNhau() {
        String plain = "SamePassword123";
        String hash1 = PasswordUtil.hash(plain);
        String hash2 = PasswordUtil.hash(plain);
        assertNotEquals(hash1, hash2,
                "BCrypt sinh salt ngẫu nhiên → 2 hash của cùng password phải khác");
    }

    @Test
    @DisplayName("Cả 2 hash khác nhau đều verify đúng với password gốc")
    void caHaiHashDeuVerifyDung() {
        String plain = "SamePassword123";
        String hash1 = PasswordUtil.hash(plain);
        String hash2 = PasswordUtil.hash(plain);
        assertTrue(PasswordUtil.verify(plain, hash1));
        assertTrue(PasswordUtil.verify(plain, hash2));
    }

    // ════════════════════════════════════════════════════
    // CASE SENSITIVITY
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("Password phân biệt hoa thường")
    void passwordPhanBietHoaThuong() {
        String hash = PasswordUtil.hash("MyPassword");
        assertTrue(PasswordUtil.verify("MyPassword", hash));
        assertFalse(PasswordUtil.verify("mypassword", hash));
        assertFalse(PasswordUtil.verify("MYPASSWORD", hash));
    }

    // ════════════════════════════════════════════════════
    // SPECIAL CHARACTERS
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("Password có ký tự đặc biệt phải hash + verify đúng")
    void passwordKyTuDacBiet() {
        String plain = "P@ssw0rd!#$%^&*()";
        String hash = PasswordUtil.hash(plain);
        assertTrue(PasswordUtil.verify(plain, hash));
    }

    @Test
    @DisplayName("Password có ký tự Unicode (tiếng Việt) phải hash + verify đúng")
    void passwordUnicode() {
        String plain = "Mật_khẩu_2026";
        String hash = PasswordUtil.hash(plain);
        assertTrue(PasswordUtil.verify(plain, hash));
    }

    // ════════════════════════════════════════════════════
    // EDGE CASES
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("Hash null phải ném exception")
    void passwordNullHashPhaiNem() {

        assertThrows(
                Exception.class,
                () -> PasswordUtil.verify("123", null)
        );
    }
    @Test
    @DisplayName("Verify với hash không hợp lệ phải ném exception")
    void verifyHashKhongHopLePhaiNem() {
        // BCrypt.checkpw() ném IllegalArgumentException nếu hash sai format
        assertThrows(Exception.class,
                () -> PasswordUtil.verify("password", "not-a-bcrypt-hash"));
    }

    @Test
    @DisplayName("Password rỗng phải hash được và verify được")
    void passwordRongVanHashVerifyDuoc() {
        // BCrypt không cấm password rỗng - business logic validate ở UserPolicy
        String hash = PasswordUtil.hash("");
        assertTrue(PasswordUtil.verify("", hash));
        assertFalse(PasswordUtil.verify("anything", hash));
    }

    @Test
    @DisplayName("Password rất dài (200 ký tự) phải hash + verify đúng")
    void passwordRatDai() {
        String plain = "a".repeat(200);
        String hash = PasswordUtil.hash(plain);
        assertTrue(PasswordUtil.verify(plain, hash));
    }
}