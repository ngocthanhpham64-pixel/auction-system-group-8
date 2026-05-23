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

import vn.edu.vnu.uet.group8.common.dto.model.UserProfileDTO;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.UserAdmin;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.AdminLevel;
import vn.edu.vnu.uet.group8.common.exception.UnauthorizedException;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;

/**
 * Test cho {@link ProfileService}.
 *
 * <p>Phạm vi:
 * <ul>
 *   <li>getProfile: 4 case (self, admin xem member, member xem member, member xem admin)
 *   <li>updateProfile: validate, kiểm quyền, delete account
 * </ul>
 *
 * <p>Lưu ý: Không test path liên quan FileUtil.saveBase64Images (static method khó mock).
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private ProfileService profileService;

    private UserMember taoMember(int id) {
        UserMember m = new UserMember.Builder("user" + id, "u" + id + "@e.com", "hash")
                .fullname("User " + id)
                .phone("0912345678")
                .build();
        m.assignId(id);
        return m;
    }

    private UserAdmin taoAdmin(int id, AdminLevel level) {
        UserAdmin a = new UserAdmin.Builder("admin" + id, "admin" + id + "@e.com", "hash", level).build();
        a.assignId(id);
        return a;
    }

    @Nested
    @DisplayName("getProfile - các case")
    class GetProfile {

        @Test
        @DisplayName("Member tự xem profile chính mình → fromMember (full info)")
        void tuXemMinh() throws SQLException {
            UserMember m = taoMember(1);
            when(userDAO.findById(1)).thenReturn(Optional.of(m));

            UserProfileDTO dto = profileService.getProfile(1, 1);
            assertNotNull(dto);
            assertNotNull(dto.getBalance(), "Tự xem - có balance");
            assertFalse(dto.isAdmin());
        }

        @Test
        @DisplayName("Admin xem Member khác → fromMemberForAdmin (ẩn balance)")
        void adminXemMember() throws SQLException {
            UserMember target = taoMember(5);
            UserAdmin requester = taoAdmin(99, AdminLevel.SUPER_ADMIN);
            when(userDAO.findById(5)).thenReturn(Optional.of(target));
            when(userDAO.findById(99)).thenReturn(Optional.of(requester));

            UserProfileDTO dto = profileService.getProfile(99, 5);
            assertNotNull(dto);
            assertNull(dto.getBalance(), "Admin xem - balance bị ẩn");
        }

        @Test
        @DisplayName("Member xem Member khác → fromMemberForOther (public profile)")
        void memberXemMember() throws SQLException {
            UserMember target = taoMember(5);
            UserMember requester = taoMember(10);
            when(userDAO.findById(5)).thenReturn(Optional.of(target));
            when(userDAO.findById(10)).thenReturn(Optional.of(requester));

            UserProfileDTO dto = profileService.getProfile(10, 5);
            assertNotNull(dto);
            assertNull(dto.getEmail(), "Public profile - không có email");
            assertNull(dto.getBalance(), "Public profile - không có balance");
        }

        @Test
        @DisplayName("Member cố xem profile Admin → UserNotFoundException (ẩn admin)")
        void memberXemAdmin() throws SQLException {
            UserAdmin target = taoAdmin(99, AdminLevel.SUPER_ADMIN);
            UserMember requester = taoMember(10);
            when(userDAO.findById(99)).thenReturn(Optional.of(target));
            when(userDAO.findById(10)).thenReturn(Optional.of(requester));

            // Hide Admin to regular user → UserNotFoundException
            assertThrows(UserNotFoundException.class,
                    () -> profileService.getProfile(10, 99));
        }

        @Test
        @DisplayName("MODERATOR cố xem profile SUPER_ADMIN → UserNotFoundException")
        void modXemSuperAdmin() throws SQLException {
            UserAdmin target = taoAdmin(99, AdminLevel.SUPER_ADMIN);
            UserAdmin requester = taoAdmin(50, AdminLevel.MODERATOR);
            when(userDAO.findById(99)).thenReturn(Optional.of(target));
            when(userDAO.findById(50)).thenReturn(Optional.of(requester));

            assertThrows(UserNotFoundException.class,
                    () -> profileService.getProfile(50, 99));
        }

        @Test
        @DisplayName("Target không tồn tại → UserNotFoundException")
        void targetKhongTonTai() throws SQLException {
            when(userDAO.findById(999)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> profileService.getProfile(1, 999));
        }
    }

    @Nested
    @DisplayName("updateProfile - quyền hạn")
    class UpdateAuth {

        @Test
        @DisplayName("Tự sửa profile chính mình → OK")
        void tuSua() throws SQLException {
            UserMember m = taoMember(1);
            when(userDAO.findById(1)).thenReturn(Optional.of(m));

            assertDoesNotThrow(() -> profileService.updateProfile(
                    1, 1, "New Name", "0912345678", "Hanoi", null));

            verify(userDAO).updateProfile(any(UserMember.class));
        }

        @Test
        @DisplayName("Member cố sửa Member khác → UnauthorizedException")
        void coSuaNguoiKhac() throws SQLException {
            UserMember requester = taoMember(1);
            when(userDAO.findById(1)).thenReturn(Optional.of(requester));

            assertThrows(UnauthorizedException.class,
                    () -> profileService.updateProfile(
                            1, 2, "Name", "0912345678", "Address", null));
        }

        @Test
        @DisplayName("SUPER_ADMIN sửa Member khác → OK")
        void superAdminSua() throws SQLException {
            UserAdmin admin = taoAdmin(99, AdminLevel.SUPER_ADMIN);
            UserMember target = taoMember(5);
            when(userDAO.findById(99)).thenReturn(Optional.of(admin));
            when(userDAO.findById(5)).thenReturn(Optional.of(target));

            assertDoesNotThrow(() -> profileService.updateProfile(
                    99, 5, "New Name", "0912345678", "Hanoi", null));
            verify(userDAO).updateProfile(any(UserMember.class));
        }

        @Test
        @DisplayName("MODERATOR sửa Member khác → UnauthorizedException")
        void modSua() throws SQLException {
            UserAdmin mod = taoAdmin(50, AdminLevel.MODERATOR);
            when(userDAO.findById(50)).thenReturn(Optional.of(mod));

            assertThrows(UnauthorizedException.class,
                    () -> profileService.updateProfile(
                            50, 5, "Name", "0912345678", "Address", null));
        }
    }

    @Nested
    @DisplayName("updateProfile - validate")
    class UpdateValidate {

        @Test
        @DisplayName("Fullname null → ValidationException")
        void fullnameNull() throws SQLException {
            UserMember m = taoMember(1);
            when(userDAO.findById(1)).thenReturn(Optional.of(m));

            assertThrows(ValidationException.class,
                    () -> profileService.updateProfile(
                            1, 1, null, "0912345678", "Address", null));
        }

        @Test
        @DisplayName("Fullname rỗng → ValidationException")
        void fullnameRong() throws SQLException {
            UserMember m = taoMember(1);
            when(userDAO.findById(1)).thenReturn(Optional.of(m));

            assertThrows(ValidationException.class,
                    () -> profileService.updateProfile(
                            1, 1, "   ", "0912345678", "Address", null));
        }

        @Test
        @DisplayName("Fullname > 50 ký tự → ValidationException")
        void fullnameDaiQua() throws SQLException {
            UserMember m = taoMember(1);
            when(userDAO.findById(1)).thenReturn(Optional.of(m));

            String tooLong = "A".repeat(51);
            assertThrows(ValidationException.class,
                    () -> profileService.updateProfile(
                            1, 1, tooLong, "0912345678", "Address", null));
        }
    }

    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccount {

        @Test
        @DisplayName("Soft delete thành công → true")
        void deleteSuccess() throws SQLException {
            assertTrue(profileService.deleteAccount(1));
            verify(userDAO).softDelete(1);
        }

        @Test
        @DisplayName("User không tồn tại → false (catch UserNotFoundException)")
        void deleteUserNotFound() throws SQLException {
            doThrow(new UserNotFoundException(999)).when(userDAO).softDelete(anyInt());
            assertFalse(profileService.deleteAccount(999));
        }

        @Test
        @DisplayName("Lỗi DB → false (catch SQLException)")
        void deleteSqlError() throws SQLException {
            doThrow(new SQLException("DB lỗi")).when(userDAO).softDelete(anyInt());
            assertFalse(profileService.deleteAccount(1));
        }
    }
}