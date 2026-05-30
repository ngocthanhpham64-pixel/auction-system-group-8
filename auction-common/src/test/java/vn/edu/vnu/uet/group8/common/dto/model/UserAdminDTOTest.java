package vn.edu.vnu.uet.group8.common.dto.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserAdminDTOTest {

    private UserAdminDTO build(String role, String status) {
        return new UserAdminDTO(1, "admin01", "a@e.com", role, status);
    }

    // ════════════════════════════════════════════════════
    // Constructor
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("Constructor 5 tham số")
    class ConstructorTest {

        @Test
        @DisplayName("Tất cả field được set đúng")
        void tatCaFieldDung() {
            UserAdminDTO dto = new UserAdminDTO(7, "user07", "u7@e.com", "SELLER", "ACTIVE");
            assertEquals(7, dto.getId());
            assertEquals("user07", dto.getUsername());
            assertEquals("u7@e.com", dto.getEmail());
            assertEquals("SELLER", dto.getRole());
            assertEquals("ACTIVE", dto.getStatus());
        }

        @Test
        @DisplayName("No-arg constructor - id=0, các String trả empty (null-safe)")
        void noArgConstructor() {
            UserAdminDTO dto = new UserAdminDTO();
            assertEquals(0, dto.getId());
            assertEquals("", dto.getUsername(), "null-safe → trả empty string");
            assertEquals("", dto.getEmail());
            assertEquals("", dto.getRole());
            assertEquals("", dto.getStatus());
        }
    }

    // ════════════════════════════════════════════════════
    // Setters
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("Setters")
    class SettersTest {

        @Test
        @DisplayName("setId thay đổi đúng")
        void setId() {
            UserAdminDTO dto = new UserAdminDTO();
            dto.setId(99);
            assertEquals(99, dto.getId());
        }

        @Test
        @DisplayName("setUsername thay đổi đúng")
        void setUsername() {
            UserAdminDTO dto = new UserAdminDTO();
            dto.setUsername("new_user");
            assertEquals("new_user", dto.getUsername());
        }

        @Test
        @DisplayName("setEmail thay đổi đúng")
        void setEmail() {
            UserAdminDTO dto = new UserAdminDTO();
            dto.setEmail("new@e.com");
            assertEquals("new@e.com", dto.getEmail());
        }

        @Test
        @DisplayName("setRole thay đổi đúng")
        void setRole() {
            UserAdminDTO dto = new UserAdminDTO();
            dto.setRole("SUPER_ADMIN");
            assertEquals("SUPER_ADMIN", dto.getRole());
        }

        @Test
        @DisplayName("setStatus thay đổi đúng")
        void setStatus() {
            UserAdminDTO dto = new UserAdminDTO();
            dto.setStatus("BANNED");
            assertEquals("BANNED", dto.getStatus());
        }
    }

    // ════════════════════════════════════════════════════
    // Null-safe getters
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("Null-safe getters → trả empty string")
    class NullSafeGettersTest {

        @Test
        @DisplayName("username null → getUsername() = empty")
        void usernameNull() {
            UserAdminDTO dto = new UserAdminDTO(1, null, "e", "r", "s");
            assertEquals("", dto.getUsername());
        }

        @Test
        @DisplayName("email null → getEmail() = empty")
        void emailNull() {
            UserAdminDTO dto = new UserAdminDTO(1, "u", null, "r", "s");
            assertEquals("", dto.getEmail());
        }

        @Test
        @DisplayName("role null → getRole() = empty")
        void roleNull() {
            UserAdminDTO dto = new UserAdminDTO(1, "u", "e", null, "s");
            assertEquals("", dto.getRole());
        }

        @Test
        @DisplayName("status null → getStatus() = empty")
        void statusNull() {
            UserAdminDTO dto = new UserAdminDTO(1, "u", "e", "r", null);
            assertEquals("", dto.getStatus());
        }
    }

    // ════════════════════════════════════════════════════
    // isAdmin() - tất cả branch
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("isAdmin()")
    class IsAdminTest {

        @Test @DisplayName("ADMIN → true")
        void roleAdmin() { assertTrue(build("ADMIN", "ACTIVE").isAdmin()); }

        @Test @DisplayName("SUPER_ADMIN → true")
        void roleSuperAdmin() { assertTrue(build("SUPER_ADMIN", "ACTIVE").isAdmin()); }

        @Test @DisplayName("MEMBER → false")
        void roleMember() { assertFalse(build("MEMBER", "ACTIVE").isAdmin()); }

        @Test @DisplayName("SELLER → false")
        void roleSeller() { assertFalse(build("SELLER", "ACTIVE").isAdmin()); }

        @Test @DisplayName("null → false")
        void roleNull() { assertFalse(build(null, "ACTIVE").isAdmin()); }

        @Test @DisplayName("ignoreCase 'admin' → true")
        void roleAdminLower() { assertTrue(build("admin", "ACTIVE").isAdmin()); }

        @Test @DisplayName("ignoreCase 'super_admin' → true")
        void roleSuperAdminLower() { assertTrue(build("super_admin", "ACTIVE").isAdmin()); }

        @Test @DisplayName("ignoreCase 'Admin' mixedcase → true")
        void roleAdminMixed() { assertTrue(build("Admin", "ACTIVE").isAdmin()); }
    }

    // ════════════════════════════════════════════════════
    // isSuperAdmin() - tất cả branch
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("isSuperAdmin()")
    class IsSuperAdminTest {

        @Test @DisplayName("SUPER_ADMIN → true")
        void superAdmin() { assertTrue(build("SUPER_ADMIN", "ACTIVE").isSuperAdmin()); }

        @Test @DisplayName("ADMIN → false")
        void admin() { assertFalse(build("ADMIN", "ACTIVE").isSuperAdmin()); }

        @Test @DisplayName("MEMBER → false")
        void member() { assertFalse(build("MEMBER", "ACTIVE").isSuperAdmin()); }

        @Test @DisplayName("null → false")
        void roleNull() { assertFalse(build(null, "ACTIVE").isSuperAdmin()); }

        @Test @DisplayName("ignoreCase 'super_admin' → true")
        void lower() { assertTrue(build("super_admin", "ACTIVE").isSuperAdmin()); }
    }

    // ════════════════════════════════════════════════════
    // isSeller() - tất cả branch
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("isSeller()")
    class IsSellerTest {

        @Test @DisplayName("SELLER → true")
        void seller() { assertTrue(build("SELLER", "ACTIVE").isSeller()); }

        @Test @DisplayName("MEMBER → false")
        void member() { assertFalse(build("MEMBER", "ACTIVE").isSeller()); }

        @Test @DisplayName("ADMIN → false")
        void admin() { assertFalse(build("ADMIN", "ACTIVE").isSeller()); }

        @Test @DisplayName("null → false")
        void roleNull() { assertFalse(build(null, "ACTIVE").isSeller()); }

        @Test @DisplayName("ignoreCase 'seller' → true")
        void lower() { assertTrue(build("seller", "ACTIVE").isSeller()); }
    }

    // ════════════════════════════════════════════════════
    // Status helpers - tất cả branch
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("isActive / isSuspended / isBanned / canLogin")
    class StatusHelpersTest {

        @Test @DisplayName("ACTIVE → isActive=true, isSuspended=false, isBanned=false")
        void active() {
            UserAdminDTO dto = build("M", "ACTIVE");
            assertTrue(dto.isActive());
            assertFalse(dto.isSuspended());
            assertFalse(dto.isBanned());
            assertTrue(dto.canLogin());
        }

        @Test @DisplayName("SUSPENDED → isActive=false, isSuspended=true, isBanned=false")
        void suspended() {
            UserAdminDTO dto = build("M", "SUSPENDED");
            assertFalse(dto.isActive());
            assertTrue(dto.isSuspended());
            assertFalse(dto.isBanned());
            assertFalse(dto.canLogin());
        }

        @Test @DisplayName("BANNED → isActive=false, isSuspended=false, isBanned=true")
        void banned() {
            UserAdminDTO dto = build("M", "BANNED");
            assertFalse(dto.isActive());
            assertFalse(dto.isSuspended());
            assertTrue(dto.isBanned());
            assertFalse(dto.canLogin());
        }

        @Test @DisplayName("null status → tất cả false")
        void statusNull() {
            UserAdminDTO dto = build("M", null);
            assertFalse(dto.isActive());
            assertFalse(dto.isSuspended());
            assertFalse(dto.isBanned());
            assertFalse(dto.canLogin());
        }

        @Test @DisplayName("ignoreCase 'active' → isActive=true")
        void activeIgnoreCase() {
            assertTrue(build("M", "active").isActive());
        }

        @Test @DisplayName("ignoreCase 'Suspended' → isSuspended=true")
        void suspendedIgnoreCase() {
            assertTrue(build("M", "Suspended").isSuspended());
        }

        @Test @DisplayName("ignoreCase 'BANNED' → isBanned=true")
        void bannedUppercase() {
            assertTrue(build("M", "BANNED").isBanned());
        }
    }

    // ════════════════════════════════════════════════════
    // equals / hashCode
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("equals / hashCode")
    class EqualsHashCodeTest {

        @Test @DisplayName("Cùng id → equals")
        void sameId() {
            UserAdminDTO a = new UserAdminDTO(5, "u1", "e1", "MEMBER", "ACTIVE");
            UserAdminDTO b = new UserAdminDTO(5, "u2", "e2", "ADMIN", "BANNED");
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test @DisplayName("Khác id → không equals")
        void diffId() {
            UserAdminDTO a = new UserAdminDTO(1, "u", "e", "M", "A");
            UserAdminDTO b = new UserAdminDTO(2, "u", "e", "M", "A");
            assertNotEquals(a, b);
        }

        @Test @DisplayName("id = 0 (no-arg) → equals với no-arg khác")
        void idZeroEquals() {
            assertEquals(new UserAdminDTO(), new UserAdminDTO());
        }

        @Test @DisplayName("equals với chính nó → true (reflexive)")
        void reflexive() {
            UserAdminDTO dto = build("ADMIN", "ACTIVE");
            assertEquals(dto, dto);
        }

        @Test @DisplayName("equals với null → false")
        void equalsNull() {
            assertNotEquals(build("M", "A"), null);
        }

        @Test @DisplayName("equals với kiểu khác → false")
        void equalsOtherType() {
            assertNotEquals(build("M", "A"), "string");
        }
    }

    // ════════════════════════════════════════════════════
    // toString
    // ════════════════════════════════════════════════════
    @Test
    @DisplayName("toString chứa id, username, email, role, status")
    void toStringContent() {
        UserAdminDTO dto = new UserAdminDTO(3, "seller01", "s@e.com", "SELLER", "ACTIVE");
        String s = dto.toString();
        assertTrue(s.contains("id=3"));
        assertTrue(s.contains("seller01"));
        assertTrue(s.contains("s@e.com"));
        assertTrue(s.contains("SELLER"));
        assertTrue(s.contains("ACTIVE"));
    }

    @Test
    @DisplayName("toString khi field null → không NPE")
    void toStringNullFields() {
        UserAdminDTO dto = new UserAdminDTO(1, null, null, null, null);
        assertDoesNotThrow(dto::toString);
        // null-safe getter trả empty, toString dùng getter nên OK
    }
}