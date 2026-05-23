package vn.edu.vnu.uet.group8.server.service.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Updated test cho ItemSpecValidator theo signature mới:
 * validate(ItemCategory category)
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
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> validator.validate(null)
        );

        assertTrue(ex.getMessage().contains("Category"));
    }

    // ════════════════════════════════════════════════════
    // VALID CATEGORY
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("Validate ELECTRONICS không được ném exception")
    void validateElectronics() {
        assertDoesNotThrow(() ->
                validator.validate(ItemCategory.ELECTRONICS)
        );
    }

    @Test
    @DisplayName("Validate VEHICLES không được ném exception")
    void validateVehicles() {
        assertDoesNotThrow(() ->
                validator.validate(ItemCategory.VEHICLES)
        );
    }

    @Test
    @DisplayName("Validate REAL_ESTATE không được ném exception")
    void validateRealEstate() {
        assertDoesNotThrow(() ->
                validator.validate(ItemCategory.REAL_ESTATE)
        );
    }

    @Test
    @DisplayName("Validate ANTIQUES không được ném exception")
    void validateAntiques() {
        assertDoesNotThrow(() ->
                validator.validate(ItemCategory.ANTIQUES)
        );
    }

    @Test
    @DisplayName("Validate FASHION không được ném exception")
    void validateFashion() {
        assertDoesNotThrow(() ->
                validator.validate(ItemCategory.FASHION)
        );
    }

    @Test
    @DisplayName("Validate OTHER không được ném exception")
    void validateOther() {
        assertDoesNotThrow(() ->
                validator.validate(ItemCategory.OTHER)
        );
    }

    // ════════════════════════════════════════════════════
    // MESSAGE FORMAT
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("Exception message phải chứa chữ Category")
    void messagePhaiChuaCategory() {
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> validator.validate(null)
        );

        assertTrue(
                ex.getMessage().toLowerCase().contains("category")
        );
    }
}