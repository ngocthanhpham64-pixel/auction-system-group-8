package vn.edu.vnu.uet.group8.common.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;
import vn.edu.vnu.uet.group8.common.enums.SpecKey;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test cho {@link Item} entity.
 *
 * <p>Test 3 phần chính:
 * <ul>
 *   <li><b>Builder pattern</b>: tạo item mới, validate constructor</li>
 *   <li><b>Reconstructor pattern</b>: tái tạo item từ DB</li>
 *   <li><b>Helpers</b>: hasSpec, getSpecs, setter validate</li>
 * </ul>
 *
 * <p>Pure logic - không phụ thuộc DB hay service.
 */
class ItemTest {

    // ════════════════════════════════════════════════════
    // BUILDER
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Builder pattern")
    class BuilderTest {

        @Test
        @DisplayName("Builder hợp lệ tạo Item thành công")
        void builderHopLeTaoItem() {
            Item item = new Item.Builder(1, "iPhone 15", ItemCategory.ELECTRONICS)
                    .description("Hàng mới 99%")
                    .condition(ItemCondition.NEW)
                    .build();

            assertNotNull(item);
            assertEquals(1, item.getSellerId());
            assertEquals("iPhone 15", item.getTitle());
            assertEquals(ItemCategory.ELECTRONICS, item.getCategory());
            assertEquals("Hàng mới 99%", item.getDescription());
            assertEquals(ItemCondition.NEW, item.getCondition());
        }

        @Test
        @DisplayName("Title bị trim khoảng trắng")
        void titleBiTrim() {
            Item item = new Item.Builder(1, "   iPhone 15   ", ItemCategory.ELECTRONICS).build();
            assertEquals("iPhone 15", item.getTitle());
        }

        @Test
        @DisplayName("Status mặc định là DRAFT nếu không set")
        void statusMacDinhDraft() {
            Item item = new Item.Builder(1, "iPhone", ItemCategory.ELECTRONICS).build();
            assertEquals(ItemStatus.DRAFT, item.getStatus());
        }

        @Test
        @DisplayName("Condition mặc định là USED nếu không set")
        void conditionMacDinhUsed() {
            Item item = new Item.Builder(1, "iPhone", ItemCategory.ELECTRONICS).build();
            assertEquals(ItemCondition.USED, item.getCondition());
        }

        @Test
        @DisplayName("Description rỗng nếu không set")
        void descriptionRongNeuKhongSet() {
            Item item = new Item.Builder(1, "iPhone", ItemCategory.ELECTRONICS).build();
            assertEquals("", item.getDescription());
        }

        @Test
        @DisplayName("sellerId <= 0 phải ném IllegalArgumentException")
        void sellerIdInvalidPhaiNem() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Item.Builder(0, "iPhone", ItemCategory.ELECTRONICS));
            assertThrows(IllegalArgumentException.class,
                    () -> new Item.Builder(-1, "iPhone", ItemCategory.ELECTRONICS));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t"})
        @DisplayName("Title null/rỗng/blank phải ném IllegalArgumentException")
        void titleInvalidPhaiNem(String invalid) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Item.Builder(1, invalid, ItemCategory.ELECTRONICS));
        }

        @Test
        @DisplayName("Category null phải ném IllegalArgumentException")
        void categoryNullPhaiNem() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Item.Builder(1, "iPhone", null));
        }

        @Test
        @DisplayName("putSpecs thêm spec key-value")
        void putSpecsThemKeyValue() {
            Item item = new Item.Builder(1, "iPhone", ItemCategory.ELECTRONICS)
                    .putSpecs("BRAND", "Apple")
                    .putSpecs("MODEL", "iPhone 15")
                    .build();

            assertEquals("Apple", item.getSpecs("BRAND"));
            assertEquals("iPhone 15", item.getSpecs("MODEL"));
        }

        @Test
        @DisplayName("putSpecs key null hoặc value rỗng phải bị bỏ qua")
        void putSpecsKhongHopLeBoQua() {
            Item item = new Item.Builder(1, "iPhone", ItemCategory.ELECTRONICS)
                    .putSpecs(null, "value")           // key null
                    .putSpecs("KEY", null)             // value null
                    .putSpecs("KEY", "")               // value rỗng
                    .putSpecs("", "value")             // key rỗng
                    .build();

            assertTrue(item.getSpecs().isEmpty(),
                    "Tất cả putSpecs invalid phải bị bỏ qua");
        }
    }

    // ════════════════════════════════════════════════════
    // RECONSTRUCTOR
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Reconstructor pattern")
    class ReconstructorTest {

        @Test
        @DisplayName("Reconstructor đầy đủ field tạo được Item")
        void reconstructorDayDuTaoItem() {
            Instant now = Instant.now();
            Map<String, String> specs = new HashMap<>();
            specs.put("BRAND", "Apple");

            Item item = Item.reconstructor()
                    .id(100)
                    .createdAt(now)
                    .isDeleted(false)
                    .sellerId(5)
                    .title("MacBook")
                    .description("Laptop")
                    .condition(ItemCondition.NEW)
                    .category(ItemCategory.ELECTRONICS)
                    .specs(specs)
                    .status(ItemStatus.LISTED)
                    .build();

            assertEquals(100, item.getId());
            assertEquals(now, item.getCreatedAt());
            assertFalse(item.isDeleted());
            assertEquals(5, item.getSellerId());
            assertEquals("MacBook", item.getTitle());
            assertEquals("Apple", item.getSpecs("BRAND"));
        }

        @Test
        @DisplayName("Reconstructor thiếu field bắt buộc phải ném IllegalStateException")
        void reconstructorThieuFieldPhaiNem() {
            // Thiếu title
            assertThrows(IllegalStateException.class,
                    () -> Item.reconstructor()
                            .id(1)
                            .createdAt(Instant.now())
                            .isDeleted(false)
                            .sellerId(5)
                            .category(ItemCategory.ELECTRONICS)
                            .specs(new HashMap<>())
                            .status(ItemStatus.LISTED)
                            .build());
        }
    }

    // ════════════════════════════════════════════════════
    // SPEC HELPERS
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Spec helpers")
    class SpecHelpers {

        @Test
        @DisplayName("hasSpec với SpecKey tồn tại trả về true")
        void hasSpecTonTaiTraVeTrue() {
            Item item = new Item.Builder(1, "iPhone", ItemCategory.ELECTRONICS)
                    .putSpecs("BRAND", "Apple")
                    .build();

            assertTrue(item.hasSpec(SpecKey.BRAND));
            assertTrue(item.hasSpec("BRAND"));   // raw key
        }

        @Test
        @DisplayName("hasSpec với SpecKey không tồn tại trả về false")
        void hasSpecKhongTonTaiTraVeFalse() {
            Item item = new Item.Builder(1, "iPhone", ItemCategory.ELECTRONICS).build();

            assertFalse(item.hasSpec(SpecKey.BRAND));
            assertFalse(item.hasSpec("NONEXISTENT"));
        }

        @Test
        @DisplayName("getSpecs trả về chuỗi rỗng nếu key không tồn tại")
        void getSpecsKeyKhongTonTaiTraVeRong() {
            Item item = new Item.Builder(1, "iPhone", ItemCategory.ELECTRONICS).build();

            assertEquals("", item.getSpecs(SpecKey.BRAND));
            assertEquals("", item.getSpecs("NONEXISTENT"));
        }

        @Test
        @DisplayName("getSpecs map trả về UnmodifiableMap - không sửa được từ ngoài")
        void getSpecsMapImmutable() {
            Item item = new Item.Builder(1, "iPhone", ItemCategory.ELECTRONICS)
                    .putSpecs("BRAND", "Apple")
                    .build();

            Map<String, String> specs = item.getSpecs();
            // Cố gắng modify - phải ném UnsupportedOperationException
            assertThrows(UnsupportedOperationException.class,
                    () -> specs.put("HACK", "VALUE"));
        }
    }

    // ════════════════════════════════════════════════════
    // SETTERS VALIDATION
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Setters validation")
    class SettersValidation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        @DisplayName("setTitle null/rỗng phải ném exception")
        void setTitleInvalidPhaiNem(String invalid) {
            Item item = new Item.Builder(1, "iPhone", ItemCategory.ELECTRONICS).build();
            assertThrows(IllegalArgumentException.class, () -> item.setTitle(invalid));
        }

        @Test
        @DisplayName("setTitle hợp lệ phải bị trim")
        void setTitleHopLeBiTrim() {
            Item item = new Item.Builder(1, "iPhone", ItemCategory.ELECTRONICS).build();
            item.setTitle("   Samsung Galaxy   ");
            assertEquals("Samsung Galaxy", item.getTitle());
        }

        @Test
        @DisplayName("setStatus null phải ném exception")
        void setStatusNullPhaiNem() {
            Item item = new Item.Builder(1, "iPhone", ItemCategory.ELECTRONICS).build();
            assertThrows(IllegalArgumentException.class, () -> item.setStatus(null));
        }

        @Test
        @DisplayName("setCondition null phải ném exception")
        void setConditionNullPhaiNem() {
            Item item = new Item.Builder(1, "iPhone", ItemCategory.ELECTRONICS).build();
            assertThrows(IllegalArgumentException.class, () -> item.setCondition(null));
        }

        @Test
        @DisplayName("putSpecs SpecKey null phải bị bỏ qua không crash")
        void putSpecsKeyNullKhongCrash() {
            Item item = new Item.Builder(1, "iPhone", ItemCategory.ELECTRONICS).build();
            assertDoesNotThrow(() -> item.putSpecs((SpecKey) null, "value"));
        }
    }

    // ════════════════════════════════════════════════════
    // TO STRING
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("toString chứa title và category")
    void toStringChuaTitleVaCategory() {
        Item item = new Item.Builder(1, "iPhone 15", ItemCategory.ELECTRONICS).build();
        String str = item.toString();
        assertTrue(str.contains("iPhone 15"));
        assertTrue(str.contains("ELECTRONICS"));
    }
}