package vn.edu.vnu.uet.group8.common.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import vn.edu.vnu.uet.group8.common.enums.AdminLevel;
import vn.edu.vnu.uet.group8.common.enums.UserRole;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;

/**
 * Test cho {@link UserAdmin}.
 */
class UserAdminTest {

    private UserAdmin taoModerator() {
        return new UserAdmin.Builder("mod1", "mod@e.com", "hash",
                AdminLevel.MODERATOR).build();
    }

    private UserAdmin taoSuperAdmin() {
        return new UserAdmin.Builder("super", "super@e.com", "hash",
                AdminLevel.SUPER_ADMIN).build();
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("Build MODERATOR thành công")
        void buildModerator() {
            UserAdmin a = taoModerator();
            assertEquals("mod1", a.getUsername());
            assertEquals("mod@e.com", a.getEmail());
            assertEquals(AdminLevel.MODERATOR, a.getAdminLevel());
            assertTrue(a.isAdmin());
            assertTrue(a.hasRole(UserRole.ADMIN));
        }

        @Test
        @DisplayName("Build SUPER_ADMIN thành công")
        void buildSuperAdmin() {
            UserAdmin a = taoSuperAdmin();
            assertEquals(AdminLevel.SUPER_ADMIN, a.getAdminLevel());
        }

        @Test
        @DisplayName("Admin chỉ có role ADMIN - set role khác → ném")
        void setRoleKhacAdminBiTu() {
            UserAdmin.Builder b = new UserAdmin.Builder(
                    "admin", "a@e.com", "hash", AdminLevel.MODERATOR);
            // Builder.roles() override để chỉ chấp nhận EnumSet.of(ADMIN)
            assertThrows(IllegalArgumentException.class,
                    () -> b.roles(EnumSet.of(UserRole.BIDDER)));
            assertThrows(IllegalArgumentException.class,
                    () -> b.roles(EnumSet.of(UserRole.SELLER)));
            assertThrows(IllegalArgumentException.class,
                    () -> b.roles(EnumSet.of(UserRole.ADMIN, UserRole.BIDDER)));
        }

        @Test
        @DisplayName("Builder.roles(EnumSet.of(ADMIN)) → hợp lệ")
        void setRoleAdminOK() {
            UserAdmin.Builder b = new UserAdmin.Builder(
                    "admin", "a@e.com", "hash", AdminLevel.MODERATOR);
            assertDoesNotThrow(() -> b.roles(EnumSet.of(UserRole.ADMIN)));
        }
    }

    @Nested
    @DisplayName("Override từ User")
    class OverrideTest {

        @Test
        @DisplayName("isAdmin() = true với mọi UserAdmin")
        void isAdminLuonTrue() {
            assertTrue(taoModerator().isAdmin());
            assertTrue(taoSuperAdmin().isAdmin());
        }

        @Test
        @DisplayName("getDisplayRole() phân biệt MODERATOR vs SUPER_ADMIN")
        void getDisplayRolePhanBiet() {
            assertEquals("MODERATOR", taoModerator().getDisplayRole());
            assertEquals("SUPER_ADMIN", taoSuperAdmin().getDisplayRole());
        }
    }

    @Nested
    @DisplayName("Quyền (canBanUser, canDeleteItem, canApproveItem)")
    class QuyenTest {

        @Test
        @DisplayName("MODERATOR KHÔNG được ban user")
        void moderatorKhongBan() {
            assertFalse(taoModerator().canBanUser());
        }

        @Test
        @DisplayName("SUPER_ADMIN được ban user")
        void superAdminDuocBan() {
            assertTrue(taoSuperAdmin().canBanUser());
        }

        @Test
        @DisplayName("Cả MODERATOR và SUPER_ADMIN đều xóa được item")
        void caHaiXoaDuocItem() {
            assertTrue(taoModerator().canDeleteItem());
            assertTrue(taoSuperAdmin().canDeleteItem());
        }

        @Test
        @DisplayName("Cả 2 cấp đều duyệt được item")
        void caHaiDuyetDuocItem() {
            assertTrue(taoModerator().canApproveItem());
            assertTrue(taoSuperAdmin().canApproveItem());
        }
    }

    @Nested
    @DisplayName("Reconstructor")
    class ReconstructorTest {

        @Test
        @DisplayName("Reconstructor đầy đủ field - build thành công")
        void buildHopLe() {
            Instant now = Instant.now();
            Set<UserRole> roles = EnumSet.of(UserRole.ADMIN);
            UserAdmin a = UserAdmin.reconstructor()
                    .id(100)
                    .createdAt(now)
                    .isDeleted(false)
                    .username("admin")
                    .email("a@e.com")
                    .fullname("Admin Full Name")
                    .encryptedPassword("hash")
                    .status(UserStatus.ACTIVE)
                    .roles(roles)
                    .lastLogin(now)
                    .adminLevel(AdminLevel.SUPER_ADMIN)
                    .build();
            assertEquals(100, a.getId());
            assertEquals(AdminLevel.SUPER_ADMIN, a.getAdminLevel());
            assertEquals("admin", a.getUsername());
        }

        @Test
        @DisplayName("Reconstructor thiếu field bắt buộc → IllegalStateException")
        void reconstructorThieuField() {
            assertThrows(IllegalStateException.class,
                    () -> UserAdmin.reconstructor()
                            .id(1)
                            .username("admin")
                            // thiếu nhiều field
                            .build());
        }

        @Test
        @DisplayName("Reconstructor thiếu adminLevel → ném")
        void thieuAdminLevel() {
            Instant now = Instant.now();
            assertThrows(IllegalStateException.class,
                    () -> UserAdmin.reconstructor()
                            .id(1)
                            .createdAt(now)
                            .isDeleted(false)
                            .username("admin")
                            .email("a@e.com")
                            .encryptedPassword("hash")
                            .status(UserStatus.ACTIVE)
                            .roles(EnumSet.of(UserRole.ADMIN))
                            // thiếu adminLevel
                            .build());
        }
    }

    @Test
    @DisplayName("toString() chứa username + adminLevel")
    void toStringFull() {
        UserAdmin a = taoModerator();
        a.assignId(5);
        String s = a.toString();
        assertTrue(s.contains("mod1"));
        assertTrue(s.contains("MODERATOR"));
    }
}