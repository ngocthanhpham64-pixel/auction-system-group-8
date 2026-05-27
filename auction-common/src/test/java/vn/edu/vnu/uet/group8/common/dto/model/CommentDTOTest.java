package vn.edu.vnu.uet.group8.common.dto.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CommentDTOTest {

    // ════════════════════════════════════════════════════
    // Constructor 5 tham số (itemId, userId, username, content, createdAt)
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("Constructor 5 tham số")
    class Ctor5Test {

        @Test
        @DisplayName("Tất cả field được set đúng")
        void tatCaFieldDung() {
            Instant now = Instant.now();
            CommentDTO dto = new CommentDTO(10, 5, "nguyen_a", "Sản phẩm tốt!", now);

            assertEquals(10, dto.getItemId());
            assertEquals(5, dto.getUserId());
            assertEquals("nguyen_a", dto.getUsername());
            assertEquals("Sản phẩm tốt!", dto.getContent());
            assertEquals(now, dto.getCreatedAt());
        }

        @Test
        @DisplayName("itemId = 0 được phép (DTO không validate)")
        void itemIdZero() {
            CommentDTO dto = new CommentDTO(0, 1, "u", "c", Instant.now());
            assertEquals(0, dto.getItemId());
        }

        @Test
        @DisplayName("itemId âm được phép (DTO không validate)")
        void itemIdNegative() {
            CommentDTO dto = new CommentDTO(-1, 1, "u", "c", Instant.now());
            assertEquals(-1, dto.getItemId());
        }

        @Test
        @DisplayName("userId = 0 được phép")
        void userIdZero() {
            CommentDTO dto = new CommentDTO(1, 0, "u", "c", Instant.now());
            assertEquals(0, dto.getUserId());
        }

        @Test
        @DisplayName("username null được phép (DTO không validate)")
        void usernameNull() {
            CommentDTO dto = new CommentDTO(1, 1, null, "c", Instant.now());
            assertNull(dto.getUsername());
        }

        @Test
        @DisplayName("content null được phép")
        void contentNull() {
            CommentDTO dto = new CommentDTO(1, 1, "u", null, Instant.now());
            assertNull(dto.getContent());
        }

        @Test
        @DisplayName("content rỗng được phép")
        void contentEmpty() {
            CommentDTO dto = new CommentDTO(1, 1, "u", "", Instant.now());
            assertEquals("", dto.getContent());
        }

        @Test
        @DisplayName("createdAt null được phép")
        void createdAtNull() {
            CommentDTO dto = new CommentDTO(1, 1, "u", "c", null);
            assertNull(dto.getCreatedAt());
        }

        @Test
        @DisplayName("content rất dài hợp lệ")
        void contentVeryLong() {
            String long500 = "A".repeat(500);
            CommentDTO dto = new CommentDTO(1, 1, "u", long500, Instant.now());
            assertEquals(500, dto.getContent().length());
        }

        @Test
        @DisplayName("createdAt ở xa quá khứ hợp lệ")
        void createdAtFarPast() {
            Instant past = Instant.ofEpochSecond(0); // 1970
            CommentDTO dto = new CommentDTO(1, 1, "u", "c", past);
            assertEquals(past, dto.getCreatedAt());
        }

        @Test
        @DisplayName("createdAt ở tương lai hợp lệ")
        void createdAtFuture() {
            Instant future = Instant.now().plusSeconds(86400);
            CommentDTO dto = new CommentDTO(1, 1, "u", "c", future);
            assertEquals(future, dto.getCreatedAt());
        }
    }

    // ════════════════════════════════════════════════════
    // Constructor 4 tham số (username, userId, content, createdAt)
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("Constructor 4 tham số (không có itemId)")
    class Ctor4Test {

        @Test
        @DisplayName("Tất cả field được set đúng, itemId = 0 mặc định")
        void tatCaFieldDung() {
            Instant now = Instant.now();
            CommentDTO dto = new CommentDTO("tran_b", 7, "Cảm ơn bạn!", now);

            assertEquals("tran_b", dto.getUsername());
            assertEquals(7, dto.getUserId());
            assertEquals("Cảm ơn bạn!", dto.getContent());
            assertEquals(now, dto.getCreatedAt());
            assertEquals(0, dto.getItemId(), "itemId không được set → phải là 0 (default int)");
        }

        @Test
        @DisplayName("username null được phép")
        void usernameNull() {
            CommentDTO dto = new CommentDTO(null, 1, "c", Instant.now());
            assertNull(dto.getUsername());
        }

        @Test
        @DisplayName("content null được phép")
        void contentNull() {
            CommentDTO dto = new CommentDTO("u", 1, null, Instant.now());
            assertNull(dto.getContent());
        }

        @Test
        @DisplayName("createdAt null được phép")
        void createdAtNull() {
            CommentDTO dto = new CommentDTO("u", 1, "c", null);
            assertNull(dto.getCreatedAt());
        }

        @Test
        @DisplayName("userId âm được phép")
        void userIdNegative() {
            CommentDTO dto = new CommentDTO("u", -5, "c", Instant.now());
            assertEquals(-5, dto.getUserId());
        }
    }

    // ════════════════════════════════════════════════════
    // Phân biệt 2 constructor
    // ════════════════════════════════════════════════════
    @Test
    @DisplayName("Ctor 5 vs Ctor 4 - itemId khác nhau")
    void phanBiet2Constructor() {
        Instant now = Instant.now();
        CommentDTO c5 = new CommentDTO(99, 1, "u", "c", now);
        CommentDTO c4 = new CommentDTO("u", 1, "c", now);

        assertEquals(99, c5.getItemId());
        assertEquals(0, c4.getItemId(), "Ctor 4 không set itemId → phải là 0");
        assertEquals(c5.getUsername(), c4.getUsername());
        assertEquals(c5.getContent(), c4.getContent());
    }
}