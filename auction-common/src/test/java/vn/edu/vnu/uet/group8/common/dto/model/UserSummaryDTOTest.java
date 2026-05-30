package vn.edu.vnu.uet.group8.common.dto.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import vn.edu.vnu.uet.group8.common.entity.UserAdmin;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.AdminLevel;

/**
 * Test cho {@link UserSummaryDTO}.
 */
class UserSummaryDTOTest {

    private UserMember memberCoFullName() {
        UserMember m = new UserMember.Builder("alice", "a@e.com", "hash")
                .fullname("Alice Nguyen")
                .build();
        m.assignId(10);
        return m;
    }

    private UserMember memberKhongFullName() {
        UserMember m = new UserMember.Builder("bob", "b@e.com", "hash").build();
        m.assignId(20);
        return m;
    }

    private UserAdmin adminUser() {
        UserAdmin a = new UserAdmin.Builder("admin", "admin@e.com", "hash",
                AdminLevel.SUPER_ADMIN).build();
        a.assignId(99);
        return a;
    }

    @Test
    @DisplayName("from(UserMember có fullname) - displayName = fullname")
    void fromMemberCoFullname() {
        UserSummaryDTO dto = UserSummaryDTO.from(memberCoFullName());
        assertEquals(10, dto.getUserId());
        assertEquals("alice", dto.getUsername());
        assertEquals("Alice Nguyen", dto.getDisplayName());
        assertFalse(dto.isAdmin());
    }

    @Test
    @DisplayName("from(UserMember không fullname) - fallback về username")
    void fromMemberKhongFullname() {
        UserSummaryDTO dto = UserSummaryDTO.from(memberKhongFullName());
        assertEquals("bob", dto.getDisplayName(),
                "Khi fullname null → fallback về username");
    }

    @Test
    @DisplayName("from(UserAdmin) - isAdmin = true, displayName fallback username")
    void fromAdmin() {
        UserSummaryDTO dto = UserSummaryDTO.from(adminUser());
        assertEquals(99, dto.getUserId());
        assertEquals("admin", dto.getUsername());
        assertEquals("admin", dto.getDisplayName(), "Admin không có fullname");
        assertTrue(dto.isAdmin());
    }

    @Test
    @DisplayName("displayRole đúng - Member vs Admin")
    void displayRolePhanBiet() {
        UserSummaryDTO mem = UserSummaryDTO.from(memberCoFullName());
        UserSummaryDTO adm = UserSummaryDTO.from(adminUser());
        assertNotEquals(mem.getDisplayRole(), adm.getDisplayRole(),
                "Member và Admin có displayRole khác nhau");
    }

    @Test
    @DisplayName("toString() chứa userId + username")
    void toStringFull() {
        UserSummaryDTO dto = UserSummaryDTO.from(memberCoFullName());
        String s = dto.toString();
        assertTrue(s.contains("10"));
        assertTrue(s.contains("alice"));
    }
}