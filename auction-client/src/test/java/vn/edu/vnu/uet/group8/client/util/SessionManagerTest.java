package vn.edu.vnu.uet.group8.client.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SessionManager")
class SessionManagerTest {

    @BeforeEach
    void clear() { SessionManager.clearSession(); }

    @Nested @DisplayName("setSession / getters")
    class SetSession {
        @Test void tokenStored() {
            SessionManager.setSession("tok123", 1, "alice", "Alice", "MEMBER");
            assertEquals("tok123", SessionManager.getAuthToken());
        }
        @Test void userIdStored() {
            SessionManager.setSession("t", 42, "u", "F", "MEMBER");
            assertEquals(42, SessionManager.getUserId());
        }
        @Test void usernameStored() {
            SessionManager.setSession("t", 1, "bob", "Bob", "MEMBER");
            assertEquals("bob", SessionManager.getUsername());
        }
        @Test void fullNameStored() {
            SessionManager.setSession("t", 1, "u", "Nguyen Van A", "MEMBER");
            assertEquals("Nguyen Van A", SessionManager.getFullName());
        }
        @Test void roleStored() {
            SessionManager.setSession("t", 1, "u", "f", "ADMIN");
            assertEquals("ADMIN", SessionManager.getRole());
        }
        @Test void nullUsernameDefaultsEmpty() {
            SessionManager.setSession("t", 1, null, null, null);
            assertEquals("", SessionManager.getUsername());
            assertEquals("", SessionManager.getFullName());
            assertEquals("", SessionManager.getRole());
        }
    }

    @Nested @DisplayName("isLoggedIn")
    class IsLoggedIn {
        @Test void falseWhenNoSession() { assertFalse(SessionManager.isLoggedIn()); }
        @Test void trueAfterSetSession() {
            SessionManager.setSession("tok", 1, "u", "f", "MEMBER");
            assertTrue(SessionManager.isLoggedIn());
        }
        @Test void falseAfterClear() {
            SessionManager.setSession("tok", 1, "u", "f", "MEMBER");
            SessionManager.clearSession();
            assertFalse(SessionManager.isLoggedIn());
        }
    }

    @Nested @DisplayName("Admin flags")
    class AdminFlags {
        @Test void memberNotAdmin() {
            SessionManager.setSession("t", 1, "u", "f", "MEMBER");
            assertFalse(SessionManager.isAdmin());
            assertFalse(SessionManager.isSuperAdmin());
            assertFalse(SessionManager.isModerator());
        }
        @Test void superAdminFlags() {
            SessionManager.setSession("t", 1, "u", "f", "SUPER_ADMIN");
            assertTrue(SessionManager.isAdmin());
            assertTrue(SessionManager.isSuperAdmin());
            assertFalse(SessionManager.isModerator());
        }
        @Test void adminRoleIsModerator() {
            SessionManager.setSession("t", 1, "u", "f", "ADMIN");
            assertTrue(SessionManager.isAdmin());
            assertFalse(SessionManager.isSuperAdmin());
            assertTrue(SessionManager.isModerator());
        }
        @Test void setAdminManually() {
            SessionManager.setAdmin(true, "SUPER_ADMIN");
            assertTrue(SessionManager.isAdmin());
            assertTrue(SessionManager.isSuperAdmin());
        }
        @Test void caseInsensitiveSuperAdmin() {
            SessionManager.setSession("t", 1, "u", "f", "super_admin");
            assertTrue(SessionManager.isSuperAdmin());
        }
    }

    @Nested @DisplayName("clearSession")
    class ClearSession {
        @Test void tokenNullAfterClear() {
            SessionManager.setSession("tok", 1, "u", "f", "MEMBER");
            SessionManager.clearSession();
            assertNull(SessionManager.getAuthToken());
        }
        @Test void userIdZeroAfterClear() {
            SessionManager.setSession("tok", 5, "u", "f", "MEMBER");
            SessionManager.clearSession();
            assertEquals(0, SessionManager.getUserId());
        }
        @Test void adminFalsAfterClear() {
            SessionManager.setSession("tok", 1, "u", "f", "SUPER_ADMIN");
            SessionManager.clearSession();
            assertFalse(SessionManager.isAdmin());
        }
    }

    @Nested @DisplayName("getAvatarText")
    class AvatarText {
        @Test void fullNameTwoWords() {
            SessionManager.setSession("t", 1, "u", "Nguyen Van A", "MEMBER");
            assertEquals("NA", SessionManager.getAvatarText());
        }
        @Test void fullNameOneWord() {
            SessionManager.setSession("t", 1, "u", "Alice", "MEMBER");
            assertEquals("AL", SessionManager.getAvatarText());
        }
        @Test void noFullNameUsesUsername() {
            SessionManager.setSession("t", 1, "bob", "", "MEMBER");
            assertEquals("BO", SessionManager.getAvatarText());
        }
        @Test void emptyEverythingReturnsQuestion() {
            SessionManager.clearSession();
            assertEquals("?", SessionManager.getAvatarText());
        }
        @Test void multipleWordsTakeFirstAndLast() {
            SessionManager.setSession("t", 1, "u", "Le Thi Bich Hang", "MEMBER");
            assertEquals("LH", SessionManager.getAvatarText());
        }
        @Test void uppercaseOutput() {
            SessionManager.setSession("t", 1, "u", "an binh", "MEMBER");
            assertEquals("AB", SessionManager.getAvatarText());
        }
    }
}