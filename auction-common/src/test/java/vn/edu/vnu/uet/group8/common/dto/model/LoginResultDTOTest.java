package vn.edu.vnu.uet.group8.common.dto.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import vn.edu.vnu.uet.group8.common.entity.UserMember;

class LoginResultDTOTest {

    private UserSummaryDTO buildSummary(int id) {
        UserMember m = UserMember.builder("user" + id, "u" + id + "@e.com", "pw").build();
        if (id > 0) m.assignId(id);
        return UserSummaryDTO.from(m);
    }

    // ════════════════════════════════════════════════════
    // Constructor 2 tham số
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("Constructor (UserSummaryDTO, String)")
    class ConstructorTest {

        @Test
        @DisplayName("user và token đều hợp lệ")
        void userVaTokenHopLe() {
            UserSummaryDTO summary = buildSummary(5);
            LoginResultDTO dto = new LoginResultDTO(summary, "tok-abc");

            assertSame(summary, dto.user());
            assertEquals("tok-abc", dto.token());
        }

        @Test
        @DisplayName("user null → user() = null, getUserId() = 0")
        void userNull() {
            LoginResultDTO dto = new LoginResultDTO(null, "tok");
            assertNull(dto.user());
            assertEquals(0, dto.getUserId(),
                    "user null → getUserId() phải trả 0");
        }

        @Test
        @DisplayName("token null được phép")
        void tokenNull() {
            LoginResultDTO dto = new LoginResultDTO(buildSummary(1), null);
            assertNull(dto.token());
        }

        @Test
        @DisplayName("token rỗng được phép")
        void tokenEmpty() {
            LoginResultDTO dto = new LoginResultDTO(buildSummary(1), "");
            assertEquals("", dto.token());
        }

        @Test
        @DisplayName("token rất dài hợp lệ (JWT dài ~500 char)")
        void tokenLong() {
            String jwt = "e".repeat(500);
            LoginResultDTO dto = new LoginResultDTO(buildSummary(1), jwt);
            assertEquals(500, dto.token().length());
        }
    }

    // ════════════════════════════════════════════════════
    // No-arg constructor
    // ════════════════════════════════════════════════════
    @Test
    @DisplayName("No-arg constructor → user null, token null")
    void noArgConstructor() {
        LoginResultDTO dto = new LoginResultDTO();
        assertNull(dto.user());
        assertNull(dto.token());
        assertEquals(0, dto.getUserId());
    }

    // ════════════════════════════════════════════════════
    // Static factory from()
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("from(UserSummaryDTO, String)")
    class FromTest {

        @Test
        @DisplayName("from() tạo đúng object")
        void fromHopLe() {
            UserSummaryDTO summary = buildSummary(10);
            LoginResultDTO dto = LoginResultDTO.from(summary, "token-xyz");

            assertSame(summary, dto.user());
            assertEquals("token-xyz", dto.token());
        }

        @Test
        @DisplayName("from() với user null")
        void fromUserNull() {
            LoginResultDTO dto = LoginResultDTO.from(null, "tok");
            assertNull(dto.user());
            assertEquals(0, dto.getUserId());
        }

        @Test
        @DisplayName("from() với token null")
        void fromTokenNull() {
            LoginResultDTO dto = LoginResultDTO.from(buildSummary(1), null);
            assertNull(dto.token());
        }
    }

    // ════════════════════════════════════════════════════
    // getUserId() - branch coverage
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("getUserId() - null branch")
    class GetUserIdTest {

        @Test
        @DisplayName("user != null → trả đúng userId")
        void userNotNull() {
            UserSummaryDTO summary = buildSummary(7);
            assertEquals(7, new LoginResultDTO(summary, "tok").getUserId());
        }

        @Test
        @DisplayName("user = null → trả 0")
        void userNull() {
            assertEquals(0, new LoginResultDTO(null, "tok").getUserId());
        }

        @Test
        @DisplayName("userId = 0 khi user chưa persist")
        void userIdZeroUnpersisted() {
            // UserMember chưa assignId → userId = 0
            UserMember m = UserMember.builder("u", "u@e.com", "pw").build();
            LoginResultDTO dto = LoginResultDTO.from(UserSummaryDTO.from(m), "tok");
            assertEquals(0, dto.getUserId());
        }

        @Test
        @DisplayName("userId âm không thể xảy ra (entity assignId luôn > 0)")
        void userIdPositive() {
            UserSummaryDTO s = buildSummary(100);
            assertTrue(new LoginResultDTO(s, "tok").getUserId() > 0);
        }
    }
}