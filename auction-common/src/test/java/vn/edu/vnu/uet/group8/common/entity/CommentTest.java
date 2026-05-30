package vn.edu.vnu.uet.group8.common.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test toàn diện cho {@link Comment} entity.
 *
 * <p>Phạm vi:
 * <ul>
 *   <li><b>Constructor</b>: tạo Comment với đầy đủ field</li>
 *   <li><b>Getters</b>: itemId, userId, username, content trả về đúng</li>
 *   <li><b>id = 0</b>: chưa persist khi mới tạo</li>
 *   <li><b>createdAt</b>: được set tự động</li>
 *   <li><b>Soft delete</b>: markAsDeleted, restore (kế thừa Entity)</li>
 *   <li><b>assignId</b>: gán ID sau khi lưu DB (kế thừa Entity)</li>
 *   <li><b>Nhiều comment</b>: độc lập nhau, không giao thoa</li>
 * </ul>
 *
 * <p>Lưu ý: {@code Comment} không có Builder/Reconstructor riêng,
 * dùng constructor trực tiếp.
 */
@DisplayName("Comment - Unit Tests")
class CommentTest {

    // ═══════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════

    private Comment buildDefault() {
        return new Comment(1, 10, "alice_nguyen", "Sản phẩm trông rất đẹp!");
    }

    // ═══════════════════════════════════════════════════
    // CONSTRUCTOR & GETTERS
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Constructor và Getters")
    class ConstructorGetters {

        @Test
        @DisplayName("Constructor hợp lệ - tạo Comment thành công")
        void constructorHopLe() {
            Comment c = buildDefault();
            assertNotNull(c);
        }

        @Test
        @DisplayName("getItemId() trả về đúng itemId đã truyền vào")
        void getItemIdDung() {
            Comment c = new Comment(42, 10, "user1", "content");
            assertEquals(42, c.getItemId());
        }

        @Test
        @DisplayName("getUserId() trả về đúng userId đã truyền vào")
        void getUserIdDung() {
            Comment c = new Comment(1, 99, "user1", "content");
            assertEquals(99, c.getUserId());
        }

        @Test
        @DisplayName("getUsername() trả về đúng username đã truyền vào")
        void getUsernameDung() {
            Comment c = new Comment(1, 1, "tran_thi_b", "content");
            assertEquals("tran_thi_b", c.getUsername());
        }

        @Test
        @DisplayName("getContent() trả về đúng nội dung comment")
        void getContentDung() {
            Comment c = new Comment(1, 1, "user1", "Hàng chất lượng cao, đáng mua!");
            assertEquals("Hàng chất lượng cao, đáng mua!", c.getContent());
        }

        @Test
        @DisplayName("Comment với nội dung tiếng Việt có dấu - không bị mất")
        void contentTiengViet() {
            String viet = "Sản phẩm rất tốt, tôi rất hài lòng với chất lượng!";
            Comment c = new Comment(1, 1, "nguyen_van_a", viet);
            assertEquals(viet, c.getContent());
        }

        @Test
        @DisplayName("Comment với nội dung rất dài - không bị cắt bớt")
        void contentDai() {
            String longContent = "A".repeat(5000);
            Comment c = new Comment(1, 1, "user", longContent);
            assertEquals(longContent, c.getContent());
            assertEquals(5000, c.getContent().length());
        }

        @Test
        @DisplayName("Comment với username có ký tự đặc biệt - giữ nguyên")
        void usernameKyTuDacBiet() {
            Comment c = new Comment(1, 1, "user_123@special", "ok");
            assertEquals("user_123@special", c.getUsername());
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 5, 100, 9999})
        @DisplayName("getItemId() đúng với nhiều giá trị itemId khác nhau")
        void getItemIdNhieuGiaTri(int itemId) {
            Comment c = new Comment(itemId, 1, "u", "c");
            assertEquals(itemId, c.getItemId());
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 50, 1000})
        @DisplayName("getUserId() đúng với nhiều giá trị userId khác nhau")
        void getUserIdNhieuGiaTri(int userId) {
            Comment c = new Comment(1, userId, "u", "c");
            assertEquals(userId, c.getUserId());
        }
    }

    // ═══════════════════════════════════════════════════
    // TRẠNG THÁI MẶC ĐỊNH (kế thừa Entity)
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Trạng thái mặc định khi tạo mới (kế thừa Entity)")
    class DefaultState {

        @Test
        @DisplayName("id = 0 khi vừa tạo (chưa lưu DB)")
        void idZeroKhiTaoMoi() {
            Comment c = buildDefault();
            assertEquals(0, c.getId());
        }

        @Test
        @DisplayName("isPersisted() = false khi vừa tạo")
        void isPersistedFalse() {
            Comment c = buildDefault();
            assertFalse(c.isPersisted());
        }

        @Test
        @DisplayName("isDeleted() = false khi vừa tạo")
        void isDeletedFalse() {
            Comment c = buildDefault();
            assertFalse(c.isDeleted());
        }

        @Test
        @DisplayName("createdAt không null và gần thời điểm tạo")
        void createdAtKhongNull() {
            Instant before = Instant.now().minusSeconds(1);
            Comment c = buildDefault();
            Instant after = Instant.now().plusSeconds(1);

            assertNotNull(c.getCreatedAt());
            assertTrue(c.getCreatedAt().isAfter(before),
                    "createdAt phải sau thời điểm trước khi tạo");
            assertTrue(c.getCreatedAt().isBefore(after),
                    "createdAt phải trước thời điểm sau khi tạo");
        }
    }

    // ═══════════════════════════════════════════════════
    // ID MANAGEMENT (kế thừa Entity)
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("ID Management - assignId (kế thừa Entity)")
    class IdManagement {

        @Test
        @DisplayName("assignId() hợp lệ - isPersisted() = true")
        void assignIdHopLe() {
            Comment c = buildDefault();
            c.assignId(123);

            assertEquals(123, c.getId());
            assertTrue(c.isPersisted());
        }

        @Test
        @DisplayName("assignId() lần 2 - ném IllegalStateException")
        void assignIdLanHaiNem() {
            Comment c = buildDefault();
            c.assignId(1);
            assertThrows(IllegalStateException.class, () -> c.assignId(2),
                    "Không được gán ID lần thứ hai");
        }

        @Test
        @DisplayName("assignId(0) - ném IllegalArgumentException")
        void assignIdZeroNem() {
            Comment c = buildDefault();
            assertThrows(IllegalArgumentException.class, () -> c.assignId(0));
        }

        @Test
        @DisplayName("assignId âm - ném IllegalArgumentException")
        void assignIdAmNem() {
            Comment c = buildDefault();
            assertThrows(IllegalArgumentException.class, () -> c.assignId(-1));
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 100, 9999, Integer.MAX_VALUE})
        @DisplayName("assignId với các giá trị dương hợp lệ")
        void assignIdDuong(int id) {
            Comment c = buildDefault();
            c.assignId(id);
            assertEquals(id, c.getId());
            assertTrue(c.isPersisted());
        }
    }

    // ═══════════════════════════════════════════════════
    // SOFT DELETE (kế thừa Entity)
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Soft Delete (kế thừa Entity)")
    class SoftDelete {

        @Test
        @DisplayName("markAsDeleted() → isDeleted() = true")
        void markAsDeleted() {
            Comment c = buildDefault();
            c.markAsDeleted();
            assertTrue(c.isDeleted());
        }

        @Test
        @DisplayName("restore() sau markAsDeleted() → isDeleted() = false")
        void restoreAfterDelete() {
            Comment c = buildDefault();
            c.markAsDeleted();
            c.restore();
            assertFalse(c.isDeleted());
        }

        @Test
        @DisplayName("markAsDeleted() nhiều lần - vẫn true (idempotent)")
        void markAsDeletedIdempotent() {
            Comment c = buildDefault();
            c.markAsDeleted();
            c.markAsDeleted();
            assertTrue(c.isDeleted());
        }

        @Test
        @DisplayName("Soft delete không thay đổi content hay username")
        void softDeleteKhongDoiContent() {
            Comment c = buildDefault();
            c.markAsDeleted();

            assertEquals("alice_nguyen", c.getUsername());
            assertEquals("Sản phẩm trông rất đẹp!", c.getContent());
            assertEquals(1, c.getItemId());
            assertEquals(10, c.getUserId());
        }

        @Test
        @DisplayName("Soft delete và restore không thay đổi id")
        void softDeleteKhongDoiId() {
            Comment c = buildDefault();
            c.assignId(77);
            c.markAsDeleted();
            c.restore();

            assertEquals(77, c.getId());
        }
    }

    // ═══════════════════════════════════════════════════
    // EQUALS & HASHCODE (kế thừa Entity)
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("equals() và hashCode() theo ID (kế thừa Entity)")
    class EqualsHashCode {

        @Test
        @DisplayName("Hai Comment cùng id → equals")
        void cungIdEquals() {
            Comment a = new Comment(1, 1, "user1", "content A");
            Comment b = new Comment(2, 2, "user2", "content B");
            a.assignId(50);
            b.assignId(50); // cùng id

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("Hai Comment khác id → không equals")
        void khacIdNotEquals() {
            Comment a = new Comment(1, 1, "u", "c");
            Comment b = new Comment(1, 1, "u", "c");
            a.assignId(1);
            b.assignId(2);

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("equals(null) → false")
        void equalsNull() {
            Comment c = buildDefault();
            assertNotEquals(c, null);
        }

        @Test
        @DisplayName("equals(chính nó) → true (reflexive)")
        void equalsChinhNo() {
            Comment c = buildDefault();
            assertEquals(c, c);
        }

        @Test
        @DisplayName("equals khác kiểu → false")
        void equalsKhacKieu() {
            Comment c = buildDefault();
            assertNotEquals(c, "comment");
            assertNotEquals(c, 42);
        }
    }

    // ═══════════════════════════════════════════════════
    // EDGE CASES & NHIỀU COMMENT CÙNG ITEM
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge cases và nhiều Comment cho cùng item")
    class EdgeCases {

        @Test
        @DisplayName("Nhiều comment cùng itemId - mỗi comment độc lập")
        void nhieuCommentCungItem() {
            int itemId = 10;
            Comment c1 = new Comment(itemId, 1, "user1", "Comment 1");
            Comment c2 = new Comment(itemId, 2, "user2", "Comment 2");
            Comment c3 = new Comment(itemId, 3, "user3", "Comment 3");

            // Xóa c1 không ảnh hưởng c2, c3
            c1.markAsDeleted();

            assertTrue(c1.isDeleted());
            assertFalse(c2.isDeleted());
            assertFalse(c3.isDeleted());

            assertEquals("Comment 2", c2.getContent());
            assertEquals("Comment 3", c3.getContent());
        }

        @Test
        @DisplayName("Nhiều comment từ cùng user - mỗi comment độc lập")
        void nhieuCommentCungUser() {
            int userId = 5;
            Comment c1 = new Comment(1, userId, "alice", "Lần 1");
            Comment c2 = new Comment(2, userId, "alice", "Lần 2");

            c1.assignId(101);
            c2.assignId(102);

            assertNotEquals(c1, c2);
            assertEquals("Lần 1", c1.getContent());
            assertEquals("Lần 2", c2.getContent());
        }

        @Test
        @DisplayName("Comment với content là chuỗi rỗng - không ném exception")
        void contentRong() {
            // Comment không validate content (không có validation trong constructor)
            assertDoesNotThrow(() -> new Comment(1, 1, "user", ""));
        }

        @Test
        @DisplayName("Comment với username là chuỗi rỗng - không ném exception")
        void usernameRong() {
            assertDoesNotThrow(() -> new Comment(1, 1, "", "content"));
        }

        @Test
        @DisplayName("Comment với content null - không ném exception khi tạo")
        void contentNull() {
            // Comment không validate, chấp nhận null
            assertDoesNotThrow(() -> new Comment(1, 1, "user", null));
        }

        @Test
        @DisplayName("createdAt của hai Comment tạo liên tiếp - không bị dùng chung")
        void createdAtDocLap() throws InterruptedException {
            Comment c1 = buildDefault();
            Thread.sleep(5); // đảm bảo khác nhau
            Comment c2 = buildDefault();

            // c2 tạo sau c1 → createdAt của c2 >= c1
            assertFalse(c2.getCreatedAt().isBefore(c1.getCreatedAt()),
                    "Comment sau phải có createdAt >= comment trước");
        }
    }
}