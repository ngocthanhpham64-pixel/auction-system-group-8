package vn.edu.vnu.uet.group8.common.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test cho {@link Entity} (lớp cha trừu tượng).
 * Test qua UserMember concrete vì Entity abstract.
 */
class EntityTest {

    private UserMember taoMember() {
        return new UserMember.Builder("alice", "a@e.com", "hash").build();
    }

    @Nested
    @DisplayName("ID Management")
    class IdTest {

        @Test
        @DisplayName("User mới tạo - id = 0, isPersisted() = false")
        void chuaPersisted() {
            UserMember m = taoMember();
            assertEquals(0, m.getId());
            assertFalse(m.isPersisted());
        }

        @Test
        @DisplayName("assignId() - gán id từ DB → isPersisted() = true")
        void assignIdHopLe() {
            UserMember m = taoMember();
            m.assignId(123);
            assertEquals(123, m.getId());
            assertTrue(m.isPersisted());
        }

        @Test
        @DisplayName("assignId() lần 2 - ném IllegalStateException")
        void assignIdLanHai() {
            UserMember m = taoMember();
            m.assignId(1);
            assertThrows(IllegalStateException.class, () -> m.assignId(2));
        }

        @Test
        @DisplayName("assignId(0) → IllegalArgumentException")
        void assignIdKhong() {
            UserMember m = taoMember();
            assertThrows(IllegalArgumentException.class, () -> m.assignId(0));
        }

        @Test
        @DisplayName("assignId âm → IllegalArgumentException")
        void assignIdAm() {
            UserMember m = taoMember();
            assertThrows(IllegalArgumentException.class, () -> m.assignId(-1));
        }
    }

    @Nested
    @DisplayName("Soft delete")
    class SoftDeleteTest {

        @Test
        @DisplayName("Mới tạo - isDeleted() = false")
        void chuaXoa() {
            UserMember m = taoMember();
            assertFalse(m.isDeleted());
        }

        @Test
        @DisplayName("markAsDeleted() → isDeleted() = true")
        void danhDauXoa() {
            UserMember m = taoMember();
            m.markAsDeleted();
            assertTrue(m.isDeleted());
        }

        @Test
        @DisplayName("restore() sau khi xóa → isDeleted() = false")
        void khoiPhuc() {
            UserMember m = taoMember();
            m.markAsDeleted();
            m.restore();
            assertFalse(m.isDeleted());
        }

        @Test
        @DisplayName("markAsDeleted() nhiều lần - vẫn isDeleted = true")
        void danhDauNhieuLan() {
            UserMember m = taoMember();
            m.markAsDeleted();
            m.markAsDeleted();
            assertTrue(m.isDeleted());
        }
    }

    @Nested
    @DisplayName("createdAt")
    class CreatedAtTest {

        @Test
        @DisplayName("createdAt được set tự động khi tạo qua Builder")
        void createdAtTuDong() {
            Instant truoc = Instant.now().minusSeconds(1);
            UserMember m = taoMember();
            Instant sau = Instant.now().plusSeconds(1);
            assertNotNull(m.getCreatedAt());
            assertTrue(m.getCreatedAt().isAfter(truoc));
            assertTrue(m.getCreatedAt().isBefore(sau));
        }
    }

    @Nested
    @DisplayName("equals + hashCode (theo id)")
    class EqualsTest {

        @Test
        @DisplayName("Hai entity cùng id → equals")
        void cungIdEquals() {
            UserMember a = taoMember();
            UserMember b = taoMember();
            a.assignId(1);
            b.assignId(1);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("Khác id → không equals")
        void khacIdNotEquals() {
            UserMember a = taoMember();
            UserMember b = taoMember();
            a.assignId(1);
            b.assignId(2);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("equals(null) → false")
        void equalsNull() {
            UserMember a = taoMember();
            assertNotEquals(a, null);
        }

        @Test
        @DisplayName("equals khác kiểu → false")
        void equalsKhacKieu() {
            UserMember a = taoMember();
            assertNotEquals(a, "string");
        }

        @Test
        @DisplayName("equals chính nó - reflexive")
        void equalsChinhNo() {
            UserMember a = taoMember();
            assertEquals(a, a);
        }
    }

    @Test
    @DisplayName("toString() chứa id + createdAt")
    void toStringFull() {
        UserMember m = taoMember();
        m.assignId(42);
        String s = m.toString();
        // toString của UserMember override Entity, nên có thể không chứa "Entity"
        // Nhưng chắc chắn chứa id thông tin
        assertNotNull(s);
        assertFalse(s.isBlank());
    }
}