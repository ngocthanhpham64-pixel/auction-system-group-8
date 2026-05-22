package vn.edu.vnu.uet.group8.server.service.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.exception.InvalidCredentialsException;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.UserServiceException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.util.PasswordUtil;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link PasswordService}.
 *
 * <p>Test method {@code changePassword} với 4 nhánh:
 * <ul>
 *   <li><b>Happy path:</b> user tồn tại, oldPassword đúng, newPassword hợp lệ → update DB</li>
 *   <li><b>UserNotFoundException:</b> userId không tồn tại trong DB</li>
 *   <li><b>InvalidCredentialsException:</b> oldPassword không khớp</li>
 *   <li><b>ValidationException:</b> newPassword không hợp lệ HOẶC trùng password cũ</li>
 * </ul>
 *
 * <p>Dùng <b>Mockito</b> để mock {@link UserDAO} - không cần DB thật.
 * Dùng {@link PasswordUtil} thật để hash/verify - đảm bảo logic BCrypt chạy đúng.
 */
@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private PasswordService passwordService;

    // Constants test
    private static final int USER_ID = 100;
    private static final String OLD_PASSWORD = "OldPass123";
    private static final String NEW_PASSWORD = "NewPass456";
    private static final String VALID_USERNAME = "testuser";
    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_PHONE = "0901234567";

    private String oldPasswordHash;
    private User existingUser;

    @BeforeEach
    void setUp() {
        // Hash thật (không mock PasswordUtil)
        oldPasswordHash = PasswordUtil.hash(OLD_PASSWORD);

        // Tạo UserMember thật làm test data
        existingUser = UserMember.builder(VALID_USERNAME, VALID_EMAIL, oldPasswordHash)
                .phone(VALID_PHONE)
                .build();
    }

    // ════════════════════════════════════════════════════
    // HAPPY PATH
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("Old password đúng + new password hợp lệ → đổi thành công")
        void doiPasswordThanhCong() throws SQLException {
            // GIVEN: DB trả về user với hash của oldPassword
            when(userDAO.findById(USER_ID)).thenReturn(Optional.of(existingUser));

            // WHEN: gọi changePassword
            assertDoesNotThrow(
                    () -> passwordService.changePassword(USER_ID, OLD_PASSWORD, NEW_PASSWORD));

            // THEN: DAO.updatePassword được gọi đúng 1 lần với newPassword
            verify(userDAO, times(1)).updatePassword(USER_ID, NEW_PASSWORD);
        }

        @Test
        @DisplayName("Tham số đúng → findById được gọi đúng 1 lần")
        void findByIdGoiDung1Lan() throws SQLException {
            when(userDAO.findById(USER_ID)).thenReturn(Optional.of(existingUser));

            passwordService.changePassword(USER_ID, OLD_PASSWORD, NEW_PASSWORD);

            verify(userDAO, times(1)).findById(USER_ID);
        }
    }

    // ════════════════════════════════════════════════════
    // USER NOT FOUND
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("User không tồn tại")
    class UserNotFound {

        @Test
        @DisplayName("findById trả Optional.empty → ném UserNotFoundException")
        void userKhongTonTaiNemException() throws SQLException {
            // GIVEN: DB trả về empty
            when(userDAO.findById(USER_ID)).thenReturn(Optional.empty());

            // WHEN + THEN
            assertThrows(UserNotFoundException.class,
                    () -> passwordService.changePassword(USER_ID, OLD_PASSWORD, NEW_PASSWORD));
        }

        @Test
        @DisplayName("User không tồn tại → KHÔNG gọi updatePassword")
        void userKhongTonTaiKhongUpdateDB() throws SQLException {
            when(userDAO.findById(USER_ID)).thenReturn(Optional.empty());

            // Bắt exception để continue verify
            try {
                passwordService.changePassword(USER_ID, OLD_PASSWORD, NEW_PASSWORD);
            } catch (UserNotFoundException ignored) {
                // expected
            }

            // updatePassword không được gọi
            verify(userDAO, never()).updatePassword(anyInt(), anyString());
        }
    }

    // ════════════════════════════════════════════════════
    // INVALID OLD PASSWORD
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Old password sai")
    class WrongOldPassword {

        @Test
        @DisplayName("oldPassword sai → ném InvalidCredentialsException")
        void oldPasswordSaiNemException() throws SQLException {
            when(userDAO.findById(USER_ID)).thenReturn(Optional.of(existingUser));

            // Gửi password sai
            assertThrows(InvalidCredentialsException.class,
                    () -> passwordService.changePassword(USER_ID, "WrongPassword", NEW_PASSWORD));
        }

        @Test
        @DisplayName("oldPassword sai → KHÔNG gọi updatePassword")
        void oldPasswordSaiKhongUpdate() throws SQLException {
            when(userDAO.findById(USER_ID)).thenReturn(Optional.of(existingUser));

            try {
                passwordService.changePassword(USER_ID, "WrongPassword", NEW_PASSWORD);
            } catch (InvalidCredentialsException ignored) {
                // expected
            }

            verify(userDAO, never()).updatePassword(anyInt(), anyString());
        }

        @Test
        @DisplayName("oldPassword chỉ khác hoa thường → vẫn bị reject (case-sensitive)")
        void oldPasswordKhacHoaThuong() throws SQLException {
            when(userDAO.findById(USER_ID)).thenReturn(Optional.of(existingUser));

            // OLD_PASSWORD = "OldPass123", gửi "OLDPASS123"
            assertThrows(InvalidCredentialsException.class,
                    () -> passwordService.changePassword(USER_ID, "OLDPASS123", NEW_PASSWORD));
        }
    }

    // ════════════════════════════════════════════════════
    // INVALID NEW PASSWORD
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("New password không hợp lệ")
    class InvalidNewPassword {

        @Test
        @DisplayName("newPassword quá ngắn (< 8 ký tự) → ném UserServiceException")
        void newPasswordQuaNgan() throws SQLException {
            when(userDAO.findById(USER_ID)).thenReturn(Optional.of(existingUser));

            assertThrows(UserServiceException.class,
                    () -> passwordService.changePassword(USER_ID, OLD_PASSWORD, "Abc12"));
        }

        @Test
        @DisplayName("newPassword không có chữ hoa → ném exception")
        void newPasswordThieuChuHoa() throws SQLException {
            when(userDAO.findById(USER_ID)).thenReturn(Optional.of(existingUser));

            assertThrows(UserServiceException.class,
                    () -> passwordService.changePassword(USER_ID, OLD_PASSWORD, "abcdef12"));
        }

        @Test
        @DisplayName("newPassword null → ném exception")
        void newPasswordNull() throws SQLException {
            when(userDAO.findById(USER_ID)).thenReturn(Optional.of(existingUser));

            assertThrows(UserServiceException.class,
                    () -> passwordService.changePassword(USER_ID, OLD_PASSWORD, null));
        }

        @Test
        @DisplayName("newPassword rỗng → ném exception")
        void newPasswordRong() throws SQLException {
            when(userDAO.findById(USER_ID)).thenReturn(Optional.of(existingUser));

            assertThrows(UserServiceException.class,
                    () -> passwordService.changePassword(USER_ID, OLD_PASSWORD, ""));
        }

        @Test
        @DisplayName("newPassword giống oldPassword → ném ValidationException")
        void newPasswordTrungOldPassword() throws SQLException {
            when(userDAO.findById(USER_ID)).thenReturn(Optional.of(existingUser));

            // Gửi newPassword = OLD_PASSWORD (sau khi pass validate UserPolicy)
            ValidationException ex = assertThrows(ValidationException.class,
                    () -> passwordService.changePassword(USER_ID, OLD_PASSWORD, OLD_PASSWORD));
            assertTrue(ex.getMessage().contains("khác mật khẩu cũ"));
        }

        @Test
        @DisplayName("newPassword invalid → KHÔNG gọi updatePassword")
        void newPasswordInvalidKhongUpdate() throws SQLException {
            when(userDAO.findById(USER_ID)).thenReturn(Optional.of(existingUser));

            try {
                passwordService.changePassword(USER_ID, OLD_PASSWORD, "weak");
            } catch (UserServiceException ignored) {
                // expected
            }

            verify(userDAO, never()).updatePassword(anyInt(), anyString());
        }
    }

    // ════════════════════════════════════════════════════
    // SQL EXCEPTION
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("findById ném SQLException → propagate ra ngoài")
    void findByIdSQLExceptionPropagate() throws SQLException {
        when(userDAO.findById(USER_ID))
                .thenThrow(new SQLException("DB connection lost"));

        assertThrows(SQLException.class,
                () -> passwordService.changePassword(USER_ID, OLD_PASSWORD, NEW_PASSWORD));
    }
}