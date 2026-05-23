package vn.edu.vnu.uet.group8.common.dto.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test cho {@link UserDTO} - Builder + getter/setter + equals/hashCode.
 */
class UserDTOTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("Builder full → tất cả field đúng")
        void buildFull() {
            LocalDateTime now = LocalDateTime.now();
            UserDTO dto = UserDTO.builder()
                    .id(1L)
                    .username("quan")
                    .email("q@e.com")
                    .password("secret")
                    .balance(new BigDecimal("100"))
                    .role("BUYER")
                    .createdAt(now)
                    .build();
            assertEquals(1L, dto.getId());
            assertEquals("quan", dto.getUsername());
            assertEquals("q@e.com", dto.getEmail());
            assertEquals("secret", dto.getPassword());
            assertEquals(new BigDecimal("100"), dto.getBalance());
            assertEquals("BUYER", dto.getRole());
            assertEquals(now, dto.getCreatedAt());
        }

        @Test
        @DisplayName("Builder rỗng → tất cả null")
        void buildRong() {
            UserDTO dto = UserDTO.builder().build();
            assertNull(dto.getId());
            assertNull(dto.getUsername());
        }
    }

    @Nested
    @DisplayName("Constructor + Setters")
    class CtorTest {

        @Test
        @DisplayName("Constructor no-arg + setters")
        void noArgVaSet() {
            UserDTO dto = new UserDTO();
            dto.setId(5L);
            dto.setUsername("alice");
            dto.setEmail("a@e.com");
            dto.setPassword("p");
            dto.setBalance(BigDecimal.TEN);
            dto.setRole("SELLER");
            LocalDateTime t = LocalDateTime.now();
            dto.setCreatedAt(t);
            assertEquals(5L, dto.getId());
            assertEquals("alice", dto.getUsername());
            assertEquals("a@e.com", dto.getEmail());
            assertEquals("p", dto.getPassword());
            assertEquals(BigDecimal.TEN, dto.getBalance());
            assertEquals("SELLER", dto.getRole());
            assertEquals(t, dto.getCreatedAt());
        }

        @Test
        @DisplayName("Constructor đầy đủ tham số")
        void ctorDayDu() {
            LocalDateTime t = LocalDateTime.now();
            UserDTO dto = new UserDTO(1L, "u", "e", "p",
                    BigDecimal.TEN, "ADMIN", t);
            assertEquals(1L, dto.getId());
            assertEquals("u", dto.getUsername());
            assertEquals("ADMIN", dto.getRole());
        }
    }

    @Nested
    @DisplayName("equals / hashCode")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("Hai DTO cùng id/username/email/role → equals")
        void equalsHopLe() {
            UserDTO a = UserDTO.builder().id(1L).username("u").email("e").role("R").build();
            UserDTO b = UserDTO.builder().id(1L).username("u").email("e").role("R").build();
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("Khác id → không equals")
        void khacId() {
            UserDTO a = UserDTO.builder().id(1L).build();
            UserDTO b = UserDTO.builder().id(2L).build();
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("equals với null → false")
        void equalsNull() {
            UserDTO a = UserDTO.builder().id(1L).build();
            assertNotEquals(a, null);
        }

        @Test
        @DisplayName("equals với object khác kiểu → false")
        void equalsKhacKieu() {
            UserDTO a = UserDTO.builder().id(1L).build();
            assertNotEquals(a, "string");
        }

        @Test
        @DisplayName("equals với chính nó → true (reflexive)")
        void equalsChinhNo() {
            UserDTO a = UserDTO.builder().id(1L).build();
            assertEquals(a, a);
        }
    }

    @Test
    @DisplayName("toString() chứa các field chính")
    void toStringFull() {
        UserDTO dto = UserDTO.builder()
                .id(1L).username("u").email("e@e.com").role("R")
                .balance(BigDecimal.ONE).build();
        String s = dto.toString();
        assertTrue(s.contains("1"));
        assertTrue(s.contains("u"));
        assertTrue(s.contains("e@e.com"));
    }
}