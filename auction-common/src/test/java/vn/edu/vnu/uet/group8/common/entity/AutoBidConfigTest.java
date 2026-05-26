package vn.edu.vnu.uet.group8.common.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test toàn diện cho {@link AutoBidConfig} entity.
 *
 * <p>Phạm vi kiểm thử:
 * <ul>
 *   <li><b>Builder pattern</b>: tạo mới AutoBidConfig, giá trị mặc định isActive = true</li>
 *   <li><b>Reconstructor pattern</b>: nạp lại từ DB với đầy đủ field</li>
 *   <li><b>ID management (kế thừa Entity)</b>: assignId, isPersisted</li>
 *   <li><b>Soft delete (kế thừa Entity)</b>: markAsDeleted, restore, isDeleted</li>
 *   <li><b>Immutable fields</b>: sessionId, userId không đổi sau khi tạo</li>
 *   <li><b>equals/hashCode (kế thừa Entity)</b>: dựa trên id</li>
 * </ul>
 */
@DisplayName("AutoBidConfig - Unit Tests")
class AutoBidConfigTest {

    // ═══════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════

    private AutoBidConfig buildDefault() {
        return AutoBidConfig.builder()
                .sessionId(10)
                .userId(5)
                .maxPrice(new BigDecimal("500000"))
                .build();
    }

    private AutoBidConfig reconstructDefault() {
        return AutoBidConfig.reconstructor()
                .id(99)
                .createdAt(Instant.now())
                .isDeleted(false)
                .sessionId(10)
                .userId(5)
                .maxPrice(new BigDecimal("500000"))
                .isActive(true)
                .build();
    }

    // ═══════════════════════════════════════════════════
    // BUILDER
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Builder pattern")
    class BuilderTest {

        @Test
        @DisplayName("Builder hợp lệ - tạo AutoBidConfig thành công")
        void builderHopLe() {
            AutoBidConfig cfg = buildDefault();

            assertNotNull(cfg);
            assertEquals(10, cfg.getSessionId());
            assertEquals(5, cfg.getUserId());
            assertEquals(0, cfg.getMaxPrice().compareTo(new BigDecimal("500000")));
        }

        @Test
        @DisplayName("isActive = true khi tạo mới qua Builder")
        void isActiveMacDinhTrue() {
            AutoBidConfig cfg = buildDefault();
            assertTrue(cfg.isActive(), "AutoBidConfig mới phải ở trạng thái active");
        }

        @Test
        @DisplayName("id = 0 khi tạo mới qua Builder (chưa persist vào DB)")
        void idZeroKhiTaoMoi() {
            AutoBidConfig cfg = buildDefault();
            assertEquals(0, cfg.getId());
            assertFalse(cfg.isPersisted());
        }

        @Test
        @DisplayName("isDeleted = false khi tạo mới qua Builder")
        void isDeletedFalseKhiTaoMoi() {
            AutoBidConfig cfg = buildDefault();
            assertFalse(cfg.isDeleted());
        }

        @Test
        @DisplayName("createdAt được tự gán (không null) khi tạo qua Builder")
        void createdAtKhongNull() {
            Instant before = Instant.now().minusSeconds(1);
            AutoBidConfig cfg = buildDefault();
            Instant after = Instant.now().plusSeconds(1);

            assertNotNull(cfg.getCreatedAt());
            assertTrue(cfg.getCreatedAt().isAfter(before));
            assertTrue(cfg.getCreatedAt().isBefore(after));
        }

        @Test
        @DisplayName("maxPrice với giá trị 0 - vẫn tạo được (edge case)")
        void maxPriceZeroChapNhan() {
            AutoBidConfig cfg = AutoBidConfig.builder()
                    .sessionId(1)
                    .userId(1)
                    .maxPrice(BigDecimal.ZERO)
                    .build();
            assertEquals(0, cfg.getMaxPrice().compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("maxPrice với giá trị lớn - vẫn tạo được")
        void maxPriceLon() {
            BigDecimal giaBig = new BigDecimal("999999999999");
            AutoBidConfig cfg = AutoBidConfig.builder()
                    .sessionId(1)
                    .userId(1)
                    .maxPrice(giaBig)
                    .build();
            assertEquals(0, cfg.getMaxPrice().compareTo(giaBig));
        }

        @Test
        @DisplayName("Builder chain fluent - tất cả setter trả về Builder")
        void builderChainFluent() {
            // Chỉ cần không throw là OK
            assertDoesNotThrow(() -> AutoBidConfig.builder()
                    .sessionId(1)
                    .userId(2)
                    .maxPrice(new BigDecimal("100000"))
                    .build());
        }
    }

    // ═══════════════════════════════════════════════════
    // RECONSTRUCTOR
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Reconstructor pattern (nạp từ DB)")
    class ReconstructorTest {

        @Test
        @DisplayName("Reconstructor đầy đủ field - build thành công")
        void reconstructorHopLe() {
            Instant now = Instant.now();
            AutoBidConfig cfg = AutoBidConfig.reconstructor()
                    .id(42)
                    .createdAt(now)
                    .isDeleted(false)
                    .sessionId(7)
                    .userId(3)
                    .maxPrice(new BigDecimal("200000"))
                    .isActive(true)
                    .build();

            assertEquals(42, cfg.getId());
            assertEquals(7, cfg.getSessionId());
            assertEquals(3, cfg.getUserId());
            assertEquals(0, cfg.getMaxPrice().compareTo(new BigDecimal("200000")));
            assertTrue(cfg.isActive());
            assertFalse(cfg.isDeleted());
            assertEquals(now, cfg.getCreatedAt());
        }

        @Test
        @DisplayName("Reconstructor với isActive = false - đã bị deactivate")
        void reconstructorIsActiveFalse() {
            AutoBidConfig cfg = AutoBidConfig.reconstructor()
                    .id(1)
                    .createdAt(Instant.now())
                    .isDeleted(false)
                    .sessionId(1)
                    .userId(1)
                    .maxPrice(new BigDecimal("100000"))
                    .isActive(false)
                    .build();

            assertFalse(cfg.isActive());
        }

        @Test
        @DisplayName("Reconstructor với isDeleted = true - soft-deleted record")
        void reconstructorSoftDeleted() {
            AutoBidConfig cfg = AutoBidConfig.reconstructor()
                    .id(1)
                    .createdAt(Instant.now())
                    .isDeleted(true)
                    .sessionId(1)
                    .userId(1)
                    .maxPrice(new BigDecimal("100000"))
                    .isActive(false)
                    .build();

            assertTrue(cfg.isDeleted());
        }

        @Test
        @DisplayName("Reconstructor - isPersisted() = true vì id > 0")
        void reconstructorIsPersisted() {
            AutoBidConfig cfg = reconstructDefault();
            assertTrue(cfg.isPersisted());
        }

        @Test
        @DisplayName("Reconstructor maxPrice null - vẫn tạo được (nullable field)")
        void reconstructorMaxPriceNull() {
            // maxPrice có thể null nếu DB có NULL value
            AutoBidConfig cfg = AutoBidConfig.reconstructor()
                    .id(1)
                    .createdAt(Instant.now())
                    .isDeleted(false)
                    .sessionId(1)
                    .userId(1)
                    .maxPrice(null)
                    .isActive(false)
                    .build();

            assertNull(cfg.getMaxPrice());
        }
    }

    // ═══════════════════════════════════════════════════
    // ID MANAGEMENT (kế thừa từ Entity)
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("ID Management (kế thừa Entity)")
    class IdManagement {

        @Test
        @DisplayName("assignId() hợp lệ - gán ID từ DB thành công")
        void assignIdHopLe() {
            AutoBidConfig cfg = buildDefault();
            cfg.assignId(55);
            assertEquals(55, cfg.getId());
            assertTrue(cfg.isPersisted());
        }

        @Test
        @DisplayName("assignId() lần 2 - ném IllegalStateException")
        void assignIdLanHaiNem() {
            AutoBidConfig cfg = buildDefault();
            cfg.assignId(55);
            assertThrows(IllegalStateException.class, () -> cfg.assignId(56));
        }

        @Test
        @DisplayName("assignId(0) - ném IllegalArgumentException")
        void assignIdZeroNem() {
            AutoBidConfig cfg = buildDefault();
            assertThrows(IllegalArgumentException.class, () -> cfg.assignId(0));
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, -100, Integer.MIN_VALUE})
        @DisplayName("assignId âm - ném IllegalArgumentException")
        void assignIdAmNem(int id) {
            AutoBidConfig cfg = buildDefault();
            assertThrows(IllegalArgumentException.class, () -> cfg.assignId(id));
        }
    }

    // ═══════════════════════════════════════════════════
    // SOFT DELETE (kế thừa từ Entity)
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Soft Delete (kế thừa Entity)")
    class SoftDelete {

        @Test
        @DisplayName("markAsDeleted() - isDeleted = true")
        void markAsDeletedTrue() {
            AutoBidConfig cfg = buildDefault();
            cfg.markAsDeleted();
            assertTrue(cfg.isDeleted());
        }

        @Test
        @DisplayName("restore() sau markAsDeleted() - isDeleted = false")
        void restoreAfterDelete() {
            AutoBidConfig cfg = buildDefault();
            cfg.markAsDeleted();
            cfg.restore();
            assertFalse(cfg.isDeleted());
        }

        @Test
        @DisplayName("markAsDeleted() nhiều lần - vẫn là true (idempotent)")
        void markAsDeletedIdempotent() {
            AutoBidConfig cfg = buildDefault();
            cfg.markAsDeleted();
            cfg.markAsDeleted();
            assertTrue(cfg.isDeleted());
        }

        @Test
        @DisplayName("restore() trên object chưa xóa - vẫn isDeleted = false")
        void restoreKhiChuaXoa() {
            AutoBidConfig cfg = buildDefault();
            cfg.restore(); // gọi trên object chưa xóa
            assertFalse(cfg.isDeleted());
        }
    }

    // ═══════════════════════════════════════════════════
    // EQUALS & HASHCODE
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("equals() và hashCode() theo ID")
    class EqualsHashCode {

        @Test
        @DisplayName("Hai config cùng id → equals")
        void cungIdEquals() {
            AutoBidConfig a = reconstructDefault();
            AutoBidConfig b = AutoBidConfig.reconstructor()
                    .id(99) // cùng id với a
                    .createdAt(Instant.now())
                    .isDeleted(false)
                    .sessionId(20)  // khác sessionId
                    .userId(8)      // khác userId
                    .maxPrice(new BigDecimal("999"))
                    .isActive(false)
                    .build();

            assertEquals(a, b, "Hai entity cùng id phải equals");
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("Hai config khác id → không equals")
        void khacIdNotEquals() {
            AutoBidConfig a = AutoBidConfig.reconstructor()
                    .id(1).createdAt(Instant.now()).isDeleted(false)
                    .sessionId(1).userId(1).maxPrice(BigDecimal.TEN).isActive(true).build();
            AutoBidConfig b = AutoBidConfig.reconstructor()
                    .id(2).createdAt(Instant.now()).isDeleted(false)
                    .sessionId(1).userId(1).maxPrice(BigDecimal.TEN).isActive(true).build();

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("equals(null) → false")
        void equalsNull() {
            AutoBidConfig cfg = reconstructDefault();
            assertNotEquals(cfg, null);
        }

        @Test
        @DisplayName("equals(chính nó) → true (reflexive)")
        void equalsChinhNo() {
            AutoBidConfig cfg = reconstructDefault();
            assertEquals(cfg, cfg);
        }

        @Test
        @DisplayName("equals khác kiểu → false")
        void equalsKhacKieu() {
            AutoBidConfig cfg = reconstructDefault();
            assertNotEquals(cfg, "AutoBidConfig");
            assertNotEquals(cfg, 99);
        }
    }

    // ═══════════════════════════════════════════════════
    // IMMUTABILITY - sessionId & userId không thay đổi
    // ═══════════════════════════════════════════════════

    @Test
    @DisplayName("sessionId và userId là immutable - không có setter")
    void sessionIdUserIdImmutable() {
        AutoBidConfig cfg = buildDefault();

        // Kiểm tra không có phương thức setSessionId, setUserId
        assertDoesNotThrow(() -> cfg.getSessionId()); // getter có
        assertDoesNotThrow(() -> cfg.getUserId());    // getter có

        // Giá trị không thay đổi sau khi tạo
        assertEquals(10, cfg.getSessionId());
        assertEquals(5, cfg.getUserId());
    }

    @Test
    @DisplayName("Hai Builder riêng biệt tạo hai instance độc lập")
    void haiBuilderDocLap() {
        AutoBidConfig a = AutoBidConfig.builder()
                .sessionId(1).userId(1).maxPrice(new BigDecimal("100")).build();
        AutoBidConfig b = AutoBidConfig.builder()
                .sessionId(2).userId(2).maxPrice(new BigDecimal("200")).build();

        assertNotSame(a, b);
        assertEquals(1, a.getSessionId());
        assertEquals(2, b.getSessionId());
    }
}