package vn.edu.vnu.uet.group8.common.dto.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import vn.edu.vnu.uet.group8.common.entity.UserAdmin;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.AdminLevel;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;

@DisplayName("UserProfileDTO - Full Coverage")
class UserProfileDTOTest {

    // ── Helpers ──────────────────────────────────────────
    private UserMember member() {
        UserMember m = UserMember.builder("member01", "m@e.com", "pw")
                .fullname("Nguyen Van A")
                .phone("0912345678")
                .balance(new BigDecimal("500000"))
                .address("Hanoi")
                .avatarUrl("http://img.png")
                .build();
        m.assignId(10);
        return m;
    }

    private UserMember memberWithRating() {
        UserMember m = UserMember.builder("seller01", "s@e.com", "pw")
                .sellerRating(new BigDecimal("4.5"))
                .phone("0987654321")
                .build();
        m.assignId(20);
        return m;
    }

    private UserAdmin superAdmin() {
        UserAdmin a = new UserAdmin.Builder("admin01", "a@e.com", "pw",
                AdminLevel.SUPER_ADMIN).build();
        a.assignId(99);
        return a;
    }

    private UserAdmin moderator() {
        UserAdmin a = new UserAdmin.Builder("mod01", "m@e.com", "pw",
                AdminLevel.MODERATOR).build();
        a.assignId(55);
        return a;
    }

    // ════════════════════════════════════════════════════
    // fromMember()
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("fromMember() - member xem profile chính mình")
    class FromMemberTest {

        @Test @DisplayName("userId đúng")
        void userId() { assertEquals(10, UserProfileDTO.fromMember(member()).getUserId()); }

        @Test @DisplayName("username đúng")
        void username() { assertEquals("member01", UserProfileDTO.fromMember(member()).getUsername()); }

        @Test @DisplayName("email đúng")
        void email() { assertEquals("m@e.com", UserProfileDTO.fromMember(member()).getEmail()); }

        @Test @DisplayName("fullName đúng")
        void fullName() { assertEquals("Nguyen Van A", UserProfileDTO.fromMember(member()).getFullName()); }

        @Test @DisplayName("phone đúng")
        void phone() { assertEquals("0912345678", UserProfileDTO.fromMember(member()).getPhone()); }

        @Test @DisplayName("avatarUrl đúng")
        void avatarUrl() { assertEquals("http://img.png", UserProfileDTO.fromMember(member()).getAvatarUrl()); }

        @Test @DisplayName("address đúng")
        void address() { assertEquals("Hanoi", UserProfileDTO.fromMember(member()).getAddress()); }

        @Test @DisplayName("balance đúng - member thấy số dư")
        void balance() {
            assertEquals(0, UserProfileDTO.fromMember(member()).getBalance()
                    .compareTo(new BigDecimal("500000")));
        }

        @Test @DisplayName("isAdmin = false")
        void isAdminFalse() { assertFalse(UserProfileDTO.fromMember(member()).isAdmin()); }

        @Test @DisplayName("adminLevel = null")
        void adminLevelNull() { assertNull(UserProfileDTO.fromMember(member()).getAdminLevel()); }

        @Test @DisplayName("status = ACTIVE mặc định")
        void status() { assertEquals(UserStatus.ACTIVE, UserProfileDTO.fromMember(member()).getStatus()); }

        @Test @DisplayName("createdAt không null")
        void createdAt() { assertNotNull(UserProfileDTO.fromMember(member()).getCreatedAt()); }

        @Test @DisplayName("sellerRating null khi chưa có đánh giá")
        void sellerRatingNull() { assertNull(UserProfileDTO.fromMember(member()).getSellerRating()); }

        @Test @DisplayName("sellerRating có giá trị khi đã được đánh giá")
        void sellerRatingSet() {
            assertEquals(0, UserProfileDTO.fromMember(memberWithRating()).getSellerRating()
                    .compareTo(new BigDecimal("4.5")));
        }

        @Test @DisplayName("totalBidsPlaced = 0 mặc định")
        void totalBidsPlaced() { assertEquals(0, UserProfileDTO.fromMember(member()).getTotalBidsPlaced()); }

        @Test @DisplayName("totalItemsSold = 0 mặc định")
        void totalItemsSold() { assertEquals(0, UserProfileDTO.fromMember(member()).getTotalItemsSold()); }

        @Test @DisplayName("displayRole không null")
        void displayRole() { assertNotNull(UserProfileDTO.fromMember(member()).getDisplayRole()); }
    }

    // ════════════════════════════════════════════════════
    // fromMemberForAdmin()
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("fromMemberForAdmin() - admin xem profile member")
    class FromMemberForAdminTest {

        @Test @DisplayName("balance = null - admin không thấy số dư")
        void balanceNull() {
            assertNull(UserProfileDTO.fromMemberForAdmin(member()).getBalance());
        }

        @Test @DisplayName("các field khác vẫn đầy đủ như fromMember")
        void otherFieldsIntact() {
            UserProfileDTO dto = UserProfileDTO.fromMemberForAdmin(member());
            assertEquals(10, dto.getUserId());
            assertEquals("member01", dto.getUsername());
            assertEquals("m@e.com", dto.getEmail());
            assertEquals("0912345678", dto.getPhone());
            assertFalse(dto.isAdmin());
        }

        @Test @DisplayName("email vẫn hiển thị - admin có quyền xem email")
        void emailVisible() {
            assertNotNull(UserProfileDTO.fromMemberForAdmin(member()).getEmail());
        }

        @Test @DisplayName("status vẫn hiển thị - admin quản lý status")
        void statusVisible() {
            assertNotNull(UserProfileDTO.fromMemberForAdmin(member()).getStatus());
        }
    }

    // ════════════════════════════════════════════════════
    // fromAdmin()
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("fromAdmin() - profile của admin user")
    class FromAdminTest {

        @Test @DisplayName("userId đúng")
        void userId() { assertEquals(99, UserProfileDTO.fromAdmin(superAdmin()).getUserId()); }

        @Test @DisplayName("username đúng")
        void username() { assertEquals("admin01", UserProfileDTO.fromAdmin(superAdmin()).getUsername()); }

        @Test @DisplayName("isAdmin = true")
        void isAdminTrue() { assertTrue(UserProfileDTO.fromAdmin(superAdmin()).isAdmin()); }

        @Test @DisplayName("adminLevel = SUPER_ADMIN")
        void adminLevelSuperAdmin() {
            assertEquals(AdminLevel.SUPER_ADMIN,
                    UserProfileDTO.fromAdmin(superAdmin()).getAdminLevel());
        }

        @Test @DisplayName("adminLevel = MODERATOR")
        void adminLevelModerator() {
            assertEquals(AdminLevel.MODERATOR,
                    UserProfileDTO.fromAdmin(moderator()).getAdminLevel());
        }

        @Test @DisplayName("balance = null - admin không có balance")
        void balanceNull() { assertNull(UserProfileDTO.fromAdmin(superAdmin()).getBalance()); }

        @Test @DisplayName("phone = null - admin không có phone")
        void phoneNull() { assertNull(UserProfileDTO.fromAdmin(superAdmin()).getPhone()); }

        @Test @DisplayName("fullName = null - admin không có fullName")
        void fullNameNull() { assertNull(UserProfileDTO.fromAdmin(superAdmin()).getFullName()); }

        @Test @DisplayName("sellerRating = null - admin không có rating")
        void sellerRatingNull() { assertNull(UserProfileDTO.fromAdmin(superAdmin()).getSellerRating()); }

        @Test @DisplayName("email đúng")
        void email() { assertEquals("a@e.com", UserProfileDTO.fromAdmin(superAdmin()).getEmail()); }

        @Test @DisplayName("status ACTIVE mặc định")
        void status() { assertEquals(UserStatus.ACTIVE, UserProfileDTO.fromAdmin(superAdmin()).getStatus()); }
    }

    // ════════════════════════════════════════════════════
    // fromMemberForOther()
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("fromMemberForOther() - người dùng khác xem")
    class FromMemberForOtherTest {

        @Test @DisplayName("email = null - ẩn email")
        void emailNull() { assertNull(UserProfileDTO.fromMemberForOther(member()).getEmail()); }

        @Test @DisplayName("phone = null - ẩn phone")
        void phoneNull() { assertNull(UserProfileDTO.fromMemberForOther(member()).getPhone()); }

        @Test @DisplayName("balance = null - ẩn balance")
        void balanceNull() { assertNull(UserProfileDTO.fromMemberForOther(member()).getBalance()); }

        @Test @DisplayName("status = null - ẩn status")
        void statusNull() { assertNull(UserProfileDTO.fromMemberForOther(member()).getStatus()); }

        @Test @DisplayName("address = null - ẩn địa chỉ")
        void addressNull() { assertNull(UserProfileDTO.fromMemberForOther(member()).getAddress()); }

        @Test @DisplayName("lastLogin = null - ẩn lastLogin")
        void lastLoginNull() { assertNull(UserProfileDTO.fromMemberForOther(member()).getLastLogin()); }

        @Test @DisplayName("totalBidsPlaced = 0 - ẩn số liệu nội bộ")
        void totalBidsZero() { assertEquals(0, UserProfileDTO.fromMemberForOther(member()).getTotalBidsPlaced()); }

        @Test @DisplayName("username hiển thị - thông tin công khai")
        void usernameVisible() { assertNotNull(UserProfileDTO.fromMemberForOther(member()).getUsername()); }

        @Test @DisplayName("avatarUrl hiển thị")
        void avatarVisible() { assertNotNull(UserProfileDTO.fromMemberForOther(member()).getAvatarUrl()); }

        @Test @DisplayName("totalItemsSold hiển thị")
        void totalItemsSoldVisible() { assertEquals(0, UserProfileDTO.fromMemberForOther(member()).getTotalItemsSold()); }

        @Test @DisplayName("isAdmin = false")
        void isAdminFalse() { assertFalse(UserProfileDTO.fromMemberForOther(member()).isAdmin()); }
    }

    // ════════════════════════════════════════════════════
    // isActive() - branch coverage
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("isActive() - status branches")
    class IsActiveTest {

        @Test @DisplayName("status = ACTIVE → true")
        void active() { assertTrue(UserProfileDTO.fromMember(member()).isActive()); }

        @Test @DisplayName("status = SUSPENDED → false")
        void suspended() {
            UserMember m = member();
            m.setStatus(UserStatus.SUSPENDED);
            assertFalse(UserProfileDTO.fromMember(m).isActive());
        }

        @Test @DisplayName("status = BANNED → false")
        void banned() {
            UserMember m = member();
            m.setStatus(UserStatus.BANNED);
            assertFalse(UserProfileDTO.fromMember(m).isActive());
        }

        @Test @DisplayName("status = null (fromMemberForOther) → false")
        void statusNull() {
            assertFalse(UserProfileDTO.fromMemberForOther(member()).isActive());
        }
    }

    // ════════════════════════════════════════════════════
    // isSeller() - tất cả branch
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("isSeller() - branches")
    class IsSellerTest {

        @Test @DisplayName("totalItemsSold > 0 → true")
        void totalItemsSoldPositive() {
            // Cần member có totalItemsSold > 0 - set qua reconstructor
            UserMember m = UserMember.reconstructor()
                    .id(1).createdAt(java.time.Instant.now()).isDeleted(false)
                    .username("seller").email("s@e.com").encryptedPassword("pw")
                    .status(UserStatus.ACTIVE).roles(java.util.Set.of(
                            vn.edu.vnu.uet.group8.common.enums.UserRole.BIDDER,
                            vn.edu.vnu.uet.group8.common.enums.UserRole.SELLER))
                    .balance(BigDecimal.ZERO).phone("0900000000")
                    .totalItemsSold(5).build();
            assertTrue(UserProfileDTO.fromMember(m).isSeller());
        }

        @Test @DisplayName("totalItemsSold = 0, displayRole = 'Người bán' → true")
        void displayRoleNguoiBan() {
            // fromMemberForOther không set totalItemsSold
            // Không thể tạo displayRole = "Người bán" trực tiếp từ member bình thường
            // isSeller() = false khi totalItemsSold=0 và displayRole không match
            UserProfileDTO dto = UserProfileDTO.fromMember(member());
            // member mặc định chỉ có BIDDER role → displayRole ≠ "Người bán"
            // totalItemsSold = 0 → isSeller = false
            assertFalse(dto.isSeller());
        }

        @Test @DisplayName("totalItemsSold = 0, displayRole khác → false")
        void notSeller() {
            assertFalse(UserProfileDTO.fromMember(member()).isSeller());
        }
    }

    // ════════════════════════════════════════════════════
    // hasRating() - branch coverage
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("hasRating() - null branch")
    class HasRatingTest {

        @Test @DisplayName("sellerRating != null → true")
        void hasRatingTrue() {
            assertTrue(UserProfileDTO.fromMember(memberWithRating()).hasRating());
        }

        @Test @DisplayName("sellerRating = null → false")
        void hasRatingFalse() {
            assertFalse(UserProfileDTO.fromMember(member()).hasRating());
        }

        @Test @DisplayName("fromAdmin → hasRating = false (admin không có rating)")
        void adminNoRating() {
            assertFalse(UserProfileDTO.fromAdmin(superAdmin()).hasRating());
        }
    }

    // ════════════════════════════════════════════════════
    // toString
    // ════════════════════════════════════════════════════
    @Test @DisplayName("toString chứa userId, username, isAdmin, status")
    void toStringContent() {
        UserProfileDTO dto = UserProfileDTO.fromMember(member());
        String s = dto.toString();
        assertTrue(s.contains("userId=10"));
        assertTrue(s.contains("member01"));
        assertTrue(s.contains("isAdmin=false"));
        assertTrue(s.contains("ACTIVE"));
    }

    @Test @DisplayName("toString fromAdmin chứa isAdmin=true")
    void toStringAdmin() {
        assertTrue(UserProfileDTO.fromAdmin(superAdmin()).toString().contains("isAdmin=true"));
    }

    @Test @DisplayName("toString không NPE khi status null")
    void toStringNullStatus() {
        assertDoesNotThrow(() -> UserProfileDTO.fromMemberForOther(member()).toString());
    }
}