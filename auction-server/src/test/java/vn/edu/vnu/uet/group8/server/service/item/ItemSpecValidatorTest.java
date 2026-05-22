package vn.edu.vnu.uet.group8.server.service.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test cho {@link ItemSpecValidator}.
 *
 * <p>Test validate specs theo từng category dựa trên {@code CategorySpecConfig}.
 *
 * <h3>Required specs theo category (theo CategorySpecConfig):</h3>
 * <ul>
 *   <li>ELECTRONICS: CONDITION, BRAND, WARRANTY_MONTHS</li>
 *   <li>VEHICLES: VEHICLE_MAKE, VEHICLE_YEAR, MILEAGE_KM, FUEL_TYPE</li>
 *   <li>REAL_ESTATE: AREA_M2, LAND_TYPE, LEGAL_STATUS, LOCATION_CITY</li>
 *   <li>ANTIQUES: CONDITION, ERA, COUNTRY_ORIGIN</li>
 *   <li>FASHION: CONDITION, SIZE, GENDER</li>
 *   <li>OTHER: CONDITION</li>
 * </ul>
 *
 * <p>Pure logic - không phụ thuộc DB/service nào khác.
 */
class ItemSpecValidatorTest {

    private ItemSpecValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ItemSpecValidator();
    }

    // ════════════════════════════════════════════════════
    // NULL CATEGORY
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("Category null phải ném ValidationException")
    void categoryNullPhaiNemException() {
        Map<String, String> specs = new HashMap<>();
        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(null, specs));
        assertTrue(ex.getMessage().contains("Category"));
    }

    // ════════════════════════════════════════════════════
    // ELECTRONICS - required: CONDITION, BRAND, WARRANTY_MONTHS
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Validate Electronics")
    class ValidateElectronics {

        @Test
        @DisplayName("Đầy đủ required specs phải pass")
        void dayDuRequiredPhaiPass() {
            Map<String, String> specs = new HashMap<>();
            specs.put("CONDITION", "Mới 99%");
            specs.put("BRAND", "Apple");
            specs.put("WARRANTY_MONTHS", "12");
            assertDoesNotThrow(() -> validator.validate(ItemCategory.ELECTRONICS, specs));
        }

        @Test
        @DisplayName("Thêm optional specs cũng phải pass")
        void themOptionalCungPass() {
            Map<String, String> specs = new HashMap<>();
            specs.put("CONDITION", "Mới");
            specs.put("BRAND", "Samsung");
            specs.put("WARRANTY_MONTHS", "24");
            specs.put("MODEL", "Galaxy S24");
            specs.put("STORAGE_GB", "256");
            specs.put("COLOR", "Black");
            assertDoesNotThrow(() -> validator.validate(ItemCategory.ELECTRONICS, specs));
        }

        @Test
        @DisplayName("Thiếu BRAND phải ném exception kèm tên field")
        void thieuBrandPhaiNem() {
            Map<String, String> specs = new HashMap<>();
            specs.put("CONDITION", "Mới");
            specs.put("WARRANTY_MONTHS", "12");
            ValidationException ex = assertThrows(ValidationException.class,
                    () -> validator.validate(ItemCategory.ELECTRONICS, specs));
            assertTrue(ex.getMessage().contains("BRAND"));
        }

        @Test
        @DisplayName("Thiếu cả 3 required phải liệt kê đủ 3 trong message")
        void thieuCa3RequiredPhaiLietKeDu3() {
            Map<String, String> specs = new HashMap<>();
            ValidationException ex = assertThrows(ValidationException.class,
                    () -> validator.validate(ItemCategory.ELECTRONICS, specs));
            assertTrue(ex.getMessage().contains("CONDITION"));
            assertTrue(ex.getMessage().contains("BRAND"));
            assertTrue(ex.getMessage().contains("WARRANTY_MONTHS"));
        }

        @Test
        @DisplayName("Spec có giá trị rỗng/blank phải coi như thiếu")
        void specRongPhaiCoiNhuThieu() {
            Map<String, String> specs = new HashMap<>();
            specs.put("CONDITION", "Mới");
            specs.put("BRAND", "");           // empty
            specs.put("WARRANTY_MONTHS", "   "); // whitespace
            ValidationException ex = assertThrows(ValidationException.class,
                    () -> validator.validate(ItemCategory.ELECTRONICS, specs));
            assertTrue(ex.getMessage().contains("BRAND"));
            assertTrue(ex.getMessage().contains("WARRANTY_MONTHS"));
        }

        @Test
        @DisplayName("Spec có giá trị null phải coi như thiếu")
        void specNullPhaiCoiNhuThieu() {
            Map<String, String> specs = new HashMap<>();
            specs.put("CONDITION", "Mới");
            specs.put("BRAND", null);
            specs.put("WARRANTY_MONTHS", "12");
            assertThrows(ValidationException.class,
                    () -> validator.validate(ItemCategory.ELECTRONICS, specs));
        }
    }

    // ════════════════════════════════════════════════════
    // VEHICLES - required: VEHICLE_MAKE, VEHICLE_YEAR, MILEAGE_KM, FUEL_TYPE
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Validate Vehicles")
    class ValidateVehicles {

        @Test
        @DisplayName("Đầy đủ 4 required phải pass")
        void dayDu4RequiredPhaiPass() {
            Map<String, String> specs = new HashMap<>();
            specs.put("VEHICLE_MAKE", "Toyota");
            specs.put("VEHICLE_YEAR", "2020");
            specs.put("MILEAGE_KM", "50000");
            specs.put("FUEL_TYPE", "Xăng");
            assertDoesNotThrow(() -> validator.validate(ItemCategory.VEHICLES, specs));
        }

        @Test
        @DisplayName("Thiếu MILEAGE_KM phải ném exception")
        void thieuMileagePhaiNem() {
            Map<String, String> specs = new HashMap<>();
            specs.put("VEHICLE_MAKE", "Honda");
            specs.put("VEHICLE_YEAR", "2022");
            specs.put("FUEL_TYPE", "Xăng");
            ValidationException ex = assertThrows(ValidationException.class,
                    () -> validator.validate(ItemCategory.VEHICLES, specs));
            assertTrue(ex.getMessage().contains("MILEAGE_KM"));
        }
    }

    // ════════════════════════════════════════════════════
    // OTHER - chỉ required CONDITION
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Validate Category Other")
    class ValidateOther {

        @Test
        @DisplayName("Có CONDITION phải pass")
        void coConditionPhaiPass() {
            Map<String, String> specs = new HashMap<>();
            specs.put("CONDITION", "Mới");
            assertDoesNotThrow(() -> validator.validate(ItemCategory.OTHER, specs));
        }

        @Test
        @DisplayName("Thiếu CONDITION phải ném exception")
        void thieuConditionPhaiNem() {
            Map<String, String> specs = new HashMap<>();
            ValidationException ex = assertThrows(ValidationException.class,
                    () -> validator.validate(ItemCategory.OTHER, specs));
            assertTrue(ex.getMessage().contains("CONDITION"));
        }
    }

    // ════════════════════════════════════════════════════
    // MAP NULL
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("Specs map null - mọi category đều phải ném exception")
    void specsMapNullPhaiNem() {
        assertThrows(ValidationException.class,
                () -> validator.validate(ItemCategory.ELECTRONICS, null));
    }

    @Test
    @DisplayName("Specs map rỗng cho category cần required phải ném exception")
    void specsMapRongPhaiNem() {
        assertThrows(ValidationException.class,
                () -> validator.validate(ItemCategory.FASHION, new HashMap<>()));
    }

    // ════════════════════════════════════════════════════
    // MESSAGE FORMAT
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("Message exception phải chứa tên category")
    void messagePhaiChuaTenCategory() {
        Map<String, String> specs = new HashMap<>();
        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(ItemCategory.ANTIQUES, specs));
        assertTrue(ex.getMessage().contains("ANTIQUES"),
                "Message phải chứa tên category để user biết đang validate cho cái gì");
    }
}