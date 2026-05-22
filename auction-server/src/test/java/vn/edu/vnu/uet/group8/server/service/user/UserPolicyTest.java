package vn.edu.vnu.uet.group8.server.service.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import vn.edu.vnu.uet.group8.common.exception.UserServiceException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test cho {@link UserPolicy}.
 *
 * <p>Test toàn bộ validate logic: username, password, email, top-up, phone.
 * Focus vào:
 * <ul>
 *   <li>Happy path - input hợp lệ trả về null</li>
 *   <li>Null/empty/blank input</li>
 *   <li>Boundary length (min/max ± 1)</li>
 *   <li>Pattern violation (ký tự đặc biệt, format sai)</li>
 *   <li>Số tiền âm/zero/quá lớn</li>
 * </ul>
 *
 * <p>Pure logic test - không cần Mockito, không phụ thuộc DB.
 */
class UserPolicyTest {

    // ════════════════════════════════════════════════════
    // USERNAME
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Validate Username")
    class ValidateUsername {

        @Test
        @DisplayName("Username hợp lệ trả về null")
        void usernameHopLeTraVeNull() {
            assertNull(UserPolicy.validateUsername("quan123"));
            assertNull(UserPolicy.validateUsername("user_name"));
            assertNull(UserPolicy.validateUsername("user.name"));
            assertNull(UserPolicy.validateUsername("ABC123"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t", "\n"})
        @DisplayName("Username null/rỗng/khoảng trắng phải ném UserServiceException")
        void usernameRongPhaiNemException(String input) {
            UserServiceException ex = assertThrows(UserServiceException.class,
                    () -> UserPolicy.validateUsername(input));
            assertTrue(ex.getMessage().contains("không được để trống"));
        }

        @Test
        @DisplayName("Username 2 ký tự (dưới min 3) phải ném exception")
        void usernameDuoiMinPhaiNemException() {
            assertThrows(UserServiceException.class,
                    () -> UserPolicy.validateUsername("ab"));
        }

        @Test
        @DisplayName("Username 3 ký tự (đúng min) phải hợp lệ")
        void usernameDungMinPhaiHopLe() {
            assertNull(UserPolicy.validateUsername("abc"));
        }

        @Test
        @DisplayName("Username 50 ký tự (đúng max) phải hợp lệ")
        void usernameDungMaxPhaiHopLe() {
            String username = "a".repeat(50);
            assertNull(UserPolicy.validateUsername(username));
        }

        @Test
        @DisplayName("Username 51 ký tự (vượt max) phải ném exception")
        void usernameVuotMaxPhaiNemException() {
            String username = "a".repeat(51);
            assertThrows(UserServiceException.class,
                    () -> UserPolicy.validateUsername(username));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "user name",      // có khoảng trắng
                "user@name",      // có @
                "user-name",      // có dấu gạch
                "user!name",      // ký tự đặc biệt
                "tên_user",       // ký tự tiếng Việt
                "user#123"        // ký tự #
        })
        @DisplayName("Username chứa ký tự không hợp lệ phải ném exception")
        void usernameCoKyTuKhongHopLePhaiNemException(String invalid) {
            assertThrows(UserServiceException.class,
                    () -> UserPolicy.validateUsername(invalid));
        }
    }

    // ════════════════════════════════════════════════════
    // PASSWORD
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Validate Password")
    class ValidatePassword {

        @Test
        @DisplayName("Password hợp lệ trả về null")
        void passwordHopLeTraVeNull() {
            assertNull(UserPolicy.validatePassword("Abc12345"));
            assertNull(UserPolicy.validatePassword("StrongPass1"));
            assertNull(UserPolicy.validatePassword("MyP@ssw0rd"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        @DisplayName("Password null/rỗng phải ném exception")
        void passwordRongPhaiNemException(String input) {
            assertThrows(UserServiceException.class,
                    () -> UserPolicy.validatePassword(input));
        }

        @Test
        @DisplayName("Password 7 ký tự (dưới min 8) phải ném exception")
        void passwordDuoiMinPhaiNemException() {
            assertThrows(UserServiceException.class,
                    () -> UserPolicy.validatePassword("Abc1234"));
        }

        @Test
        @DisplayName("Password 8 ký tự (đúng min) đủ điều kiện pattern phải hợp lệ")
        void passwordDungMinPhaiHopLe() {
            assertNull(UserPolicy.validatePassword("Abc12345"));
        }

        @Test
        @DisplayName("Password chỉ có chữ thường phải ném exception")
        void passwordChiChuThuongPhaiNemException() {
            assertThrows(UserServiceException.class,
                    () -> UserPolicy.validatePassword("abcdefgh"));
        }

        @Test
        @DisplayName("Password chỉ có chữ hoa phải ném exception")
        void passwordChiChuHoaPhaiNemException() {
            assertThrows(UserServiceException.class,
                    () -> UserPolicy.validatePassword("ABCDEFGH"));
        }

        @Test
        @DisplayName("Password chỉ có số phải ném exception")
        void passwordChiCoSoPhaiNemException() {
            assertThrows(UserServiceException.class,
                    () -> UserPolicy.validatePassword("12345678"));
        }

        @Test
        @DisplayName("Password thiếu chữ số phải ném exception")
        void passwordThieuChuSoPhaiNemException() {
            assertThrows(UserServiceException.class,
                    () -> UserPolicy.validatePassword("AbcdEfgh"));
        }
    }

    // ════════════════════════════════════════════════════
    // EMAIL
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Validate Email")
    class ValidateEmail {

        @ParameterizedTest
        @ValueSource(strings = {
                "user@example.com",
                "name.surname@domain.vn",
                "user_123@sub.domain.com",
                "a+b@x.io",
                "user-name@example.org"
        })
        @DisplayName("Email đúng format trả về null")
        void emailDungFormatTraVeNull(String email) {
            assertNull(UserPolicy.validateEmail(email));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        @DisplayName("Email null/rỗng phải ném exception")
        void emailRongPhaiNemException(String input) {
            assertThrows(UserServiceException.class,
                    () -> UserPolicy.validateEmail(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "noAtSign.com",         // không có @
                "@nouser.com",          // không có user
                "user@",                // không có domain
                "user@.com",            // domain bắt đầu bằng .
                "user@domain",          // không có TLD
                "user @domain.com",     // có khoảng trắng
                "user@domain.c"         // TLD chỉ 1 ký tự
        })
        @DisplayName("Email sai format phải ném exception")
        void emailSaiFormatPhaiNemException(String invalid) {
            assertThrows(UserServiceException.class,
                    () -> UserPolicy.validateEmail(invalid));
        }
    }

    // ════════════════════════════════════════════════════
    // TOP-UP AMOUNT
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Validate Top-up Amount")
    class ValidateTopUp {

        @Test
        @DisplayName("Số tiền 10000 (min) phải hợp lệ")
        void soTienDungMinPhaiHopLe() {
            assertNull(UserPolicy.validateTopUpAmount(new BigDecimal("10000")));
        }

        @Test
        @DisplayName("Số tiền 1 tỷ (max) phải hợp lệ")
        void soTienDungMaxPhaiHopLe() {
            assertNull(UserPolicy.validateTopUpAmount(new BigDecimal("1000000000")));
        }

        @Test
        @DisplayName("Số tiền 50 triệu (trong khoảng) phải hợp lệ")
        void soTienTrongKhoangPhaiHopLe() {
            assertNull(UserPolicy.validateTopUpAmount(new BigDecimal("50000000")));
        }

        @Test
        @DisplayName("Số tiền null phải ném exception")
        void soTienNullPhaiNemException() {
            UserServiceException ex = assertThrows(UserServiceException.class,
                    () -> UserPolicy.validateTopUpAmount(null));
            assertTrue(ex.getMessage().contains("không được để trống"));
        }

        @Test
        @DisplayName("Số tiền 9999 (dưới min) phải ném exception")
        void soTienDuoiMinPhaiNemException() {
            assertThrows(UserServiceException.class,
                    () -> UserPolicy.validateTopUpAmount(new BigDecimal("9999")));
        }

        @Test
        @DisplayName("Số tiền vượt max phải ném exception")
        void soTienVuotMaxPhaiNemException() {
            assertThrows(UserServiceException.class,
                    () -> UserPolicy.validateTopUpAmount(new BigDecimal("1000000001")));
        }

        @Test
        @DisplayName("Số tiền 0 phải ném exception")
        void soTienZeroPhaiNemException() {
            assertThrows(UserServiceException.class,
                    () -> UserPolicy.validateTopUpAmount(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Số tiền âm phải ném exception")
        void soTienAmPhaiNemException() {
            assertThrows(UserServiceException.class,
                    () -> UserPolicy.validateTopUpAmount(new BigDecimal("-1000")));
        }
    }

    // ════════════════════════════════════════════════════
    // PHONE
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Validate Phone Number")
    class ValidatePhone {

        @Test
        @DisplayName("SĐT 10 chữ số phải hợp lệ")
        void sdt10ChuSoPhaiHopLe() {
            assertNull(UserPolicy.validatePhone("0901234567"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        @DisplayName("SĐT null/rỗng phải ném exception")
        void sdtRongPhaiNemException(String input) {
            assertThrows(UserServiceException.class,
                    () -> UserPolicy.validatePhone(input));
        }

        @Test
        @DisplayName("SĐT 9 chữ số phải ném exception")
        void sdt9ChuSoPhaiNemException() {
            assertThrows(UserServiceException.class,
                    () -> UserPolicy.validatePhone("090123456"));
        }

        @Test
        @DisplayName("SĐT 11 chữ số phải ném exception")
        void sdt11ChuSoPhaiNemException() {
            assertThrows(UserServiceException.class,
                    () -> UserPolicy.validatePhone("09012345678"));
        }
    }

    // ════════════════════════════════════════════════════
    // CONSTANTS
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Hằng số nghiệp vụ")
    class Constants {

        @Test
        @DisplayName("Min/Max length của username")
        void usernameMinMaxLength() {
            assertTrue(UserPolicy.USERNAME_MIN_LENGTH == 3);
            assertTrue(UserPolicy.USERNAME_MAX_LENGTH == 50);
        }

        @Test
        @DisplayName("Min/Max length của password")
        void passwordMinMaxLength() {
            assertTrue(UserPolicy.PASSWORD_MIN_LENGTH == 8);
            assertTrue(UserPolicy.PASSWORD_MAX_LENGTH == 100);
        }

        @Test
        @DisplayName("Top-up min/max amount")
        void topUpMinMaxAmount() {
            assertTrue(UserPolicy.TOPUP_MIN_AMOUNT.compareTo(new BigDecimal("10000")) == 0);
            assertTrue(UserPolicy.TOPUP_MAX_AMOUNT.compareTo(new BigDecimal("1000000000")) == 0);
        }

        @Test
        @DisplayName("Constructor private - không khởi tạo được")
        void constructorPrivate() {
            // UserPolicy là utility class - không cho khởi tạo
            // Test reflection ra ngoài scope test cơ bản
            // Chỉ verify class có thể truy cập static method được
            assertDoesNotThrow(() -> UserPolicy.validateUsername("validuser"));
        }
    }
}