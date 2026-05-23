package vn.edu.vnu.uet.group8.server.service.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.vnu.uet.group8.common.exception.UserServiceException;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.exception.InvalidCredentialsException;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.util.PasswordUtil;

/**
 * Test cho {@link PasswordService}.
 *
 * <p>PasswordUtil dùng thật (BCrypt hash) — không mock vì là static method.
 *
 * <p>Phạm vi:
 * <ul>
 *   <li>changePassword</li>
 *   <li>requestOtpForPasswordReset</li>
 *   <li>resetPasswordWithOtp</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private PasswordService passwordService;

    /** Helper - tạo UserMember với password đã hash. */
    private UserMember taoMember(int id, String plainPassword) {
        UserMember m = new UserMember.Builder(
                "alice",
                "a@e.com",
                PasswordUtil.hash(plainPassword))
                .build();

        m.assignId(id);
        return m;
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("changePassword()")
    class ChangePasswordTest {

        @Test
        @DisplayName("User không tồn tại → UserNotFoundException")
        void userKhongTonTai() throws SQLException {

            when(userDAO.findById(999))
                    .thenReturn(Optional.empty());

            assertThrows(
                    UserNotFoundException.class,
                    () -> passwordService.changePassword(
                            999,
                            "old",
                            "newPass123"));
        }

        @Test
        @DisplayName("Mật khẩu cũ sai → InvalidCredentialsException")
        void matKhauCuSai() throws SQLException {

            User user = taoMember(1, "rightOld");

            when(userDAO.findById(1))
                    .thenReturn(Optional.of(user));

            assertThrows(
                    InvalidCredentialsException.class,
                    () -> passwordService.changePassword(
                            1,
                            "wrongOld",
                            "newPass123"));
        }

        @Test
        @DisplayName("Mật khẩu mới giống mật khẩu cũ → ValidationException")
        void matKhauMoiTrungCu() throws SQLException {

            User user = taoMember(1, "samePass123");

            when(userDAO.findById(1))
                    .thenReturn(Optional.of(user));

            ValidationException ex = assertThrows(
                    ValidationException.class,
                    () -> passwordService.changePassword(
                            1,
                            "samePass123",
                            "samePass123"));

            assertTrue(ex.getMessage().toLowerCase().contains("khác"));
        }

        @Test
        @DisplayName("Mật khẩu mới quá ngắn → ValidationException")
        void matKhauMoiNgan() throws SQLException {

            User user = taoMember(1, "rightOld");

            when(userDAO.findById(1))
                    .thenReturn(Optional.of(user));

            assertThrows(
                    UserServiceException.class,
                    () -> passwordService.changePassword(
                            1,
                            "rightOld",
                            "12"));
        }

        @Test
        @DisplayName("Đổi mật khẩu thành công → gọi updatePassword")
        void doiThanhCong() throws SQLException {

            User user = taoMember(1, "oldPass123");

            when(userDAO.findById(1))
                    .thenReturn(Optional.of(user));

            passwordService.changePassword(
                    1,
                    "oldPass123",
                    "newPass456");

            verify(userDAO).updatePassword(
                    eq(1),
                    eq("newPass456")
            );


        }

        // ───────────────── merge thêm từ file 2 ─────────────────

        @Test
        @DisplayName("oldPassword khác hoa thường → vẫn sai")
        void oldPasswordKhacHoaThuong() throws SQLException {

            User user = taoMember(1, "OldPass123");

            when(userDAO.findById(1))
                    .thenReturn(Optional.of(user));

            assertThrows(
                    InvalidCredentialsException.class,
                    () -> passwordService.changePassword(
                            1,
                            "OLDPASS123",
                            "NewPass456"));
        }

        @Test
        @DisplayName("Mật khẩu mới invalid → không update DB")
        void matKhauMoiInvalidKhongUpdate() throws SQLException {

            User user = taoMember(1, "oldPass123");

            when(userDAO.findById(1))
                    .thenReturn(Optional.of(user));

            try {
                passwordService.changePassword(
                        1,
                        "oldPass123",
                        "123");
            } catch (UserServiceException ignored) {
            }

            verify(userDAO, never())
                    .updatePassword(anyInt(), anyString());
        }

        @Test
        @DisplayName("SQLException từ DAO → propagate")
        void sqlExceptionPropagate() throws SQLException {

            when(userDAO.findById(1))
                    .thenThrow(new SQLException("DB error"));

            assertThrows(
                    SQLException.class,
                    () -> passwordService.changePassword(
                            1,
                            "old",
                            "newPass123"));
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("requestOtpForPasswordReset()")
    class RequestOtpTest {

        @Test
        @DisplayName("Email không tồn tại → ValidationException")
        void emailKhongTonTai() throws SQLException {

            when(userDAO.findByEmail("notexist@e.com"))
                    .thenReturn(Optional.empty());

            assertThrows(
                    ValidationException.class,
                    () -> passwordService.requestOtpForPasswordReset(
                            "notexist@e.com"));
        }

        @Test
        @DisplayName("Email tồn tại → trả OTP 6 chữ số")
        void otpSinhRa() throws SQLException {

            User user = taoMember(1, "pass");

            when(userDAO.findByEmail("a@e.com"))
                    .thenReturn(Optional.of(user));

            String otp =
                    passwordService.requestOtpForPasswordReset("a@e.com");

            assertNotNull(otp);

            assertEquals(
                    6,
                    otp.length(),
                    "OTP phải đúng 6 ký tự");

            assertTrue(
                    otp.matches("\\d{6}"),
                    "OTP phải toàn số");
        }

        @Test
        @DisplayName("Email được normalize trước khi query")
        void emailNormalize() throws SQLException {

            User user = taoMember(1, "pass");

            when(userDAO.findByEmail("a@e.com"))
                    .thenReturn(Optional.of(user));

            passwordService.requestOtpForPasswordReset(
                    "  A@E.COM  ");

            verify(userDAO)
                    .findByEmail("a@e.com");
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("resetPasswordWithOtp()")
    class ResetWithOtpTest {

        @Test
        @DisplayName("OTP chưa được request → ValidationException")
        void otpChuaRequest() {

            assertThrows(
                    ValidationException.class,
                    () -> passwordService.resetPasswordWithOtp(
                            "a@e.com",
                            "123456",
                            "newPass"));
        }

        @Test
        @DisplayName("OTP sai → ValidationException")
        void otpSai() throws SQLException {

            User user = taoMember(1, "pass");

            when(userDAO.findByEmail("a@e.com"))
                    .thenReturn(Optional.of(user));

            String otp =
                    passwordService.requestOtpForPasswordReset(
                            "a@e.com");

            String wrongOtp =
                    otp.equals("000000")
                            ? "111111"
                            : "000000";

            ValidationException ex = assertThrows(
                    ValidationException.class,
                    () -> passwordService.resetPasswordWithOtp(
                            "a@e.com",
                            wrongOtp,
                            "newPass456"));

            assertTrue(
                    ex.getMessage().toLowerCase().contains("otp")
                            || ex.getMessage().toLowerCase().contains("chính xác"));
        }

        @Test
        @DisplayName("OTP đúng → reset password thành công")
        void otpDung() throws SQLException {

            User user = taoMember(1, "pass");

            when(userDAO.findByEmail("a@e.com"))
                    .thenReturn(Optional.of(user));

            String otp =
                    passwordService.requestOtpForPasswordReset(
                            "a@e.com");

            passwordService.resetPasswordWithOtp(
                    "a@e.com",
                    otp,
                    "newValid123");

            verify(userDAO)
                    .updatePassword(eq(1), anyString());
        }

        @Test
        @DisplayName("Mật khẩu mới không hợp lệ → ValidationException")
        void newPasswordKhongHopLe() throws SQLException {

            User user = taoMember(1, "pass");

            when(userDAO.findByEmail("a@e.com"))
                    .thenReturn(Optional.of(user));

            String otp =
                    passwordService.requestOtpForPasswordReset(
                            "a@e.com");

            assertThrows(
                    UserServiceException.class,
                    () -> passwordService.resetPasswordWithOtp(
                            "a@e.com",
                            otp,
                            "ab"));
        }
    }
}