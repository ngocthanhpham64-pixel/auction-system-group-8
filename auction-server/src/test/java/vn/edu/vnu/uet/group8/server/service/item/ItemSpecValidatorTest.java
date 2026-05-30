package vn.edu.vnu.uet.group8.server.service.item;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;

/**
 * Test cho {@link ItemSpecValidator}.
 *
 * <p>Code hiện tại: validate(ItemCategory category)
 */
class ItemSpecValidatorTest {

  private ItemSpecValidator validator;

  @BeforeEach
  void setUp() {
    validator = new ItemSpecValidator();
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("Trường hợp lỗi")
  class InvalidInputTest {

    @Test
    @DisplayName("Category null → ValidationException")
    void categoryNull() {
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> validator.validate(null)
      );

      assertTrue(ex.getMessage().toLowerCase().contains("category"));
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("Category hợp lệ")
  class ValidCategoryTest {

    @Test
    @DisplayName("ELECTRONICS hợp lệ")
    void electronics() {
      assertDoesNotThrow(() -> validator.validate(ItemCategory.ELECTRONICS));
    }

    @Test
    @DisplayName("VEHICLES hợp lệ")
    void vehicles() {
      assertDoesNotThrow(() -> validator.validate(ItemCategory.VEHICLES));
    }

    @Test
    @DisplayName("REAL_ESTATE hợp lệ")
    void realEstate() {
      assertDoesNotThrow(() -> validator.validate(ItemCategory.REAL_ESTATE));
    }

    @Test
    @DisplayName("ANTIQUES hợp lệ")
    void antiques() {
      assertDoesNotThrow(() -> validator.validate(ItemCategory.ANTIQUES));
    }

    @Test
    @DisplayName("FASHION hợp lệ")
    void fashion() {
      assertDoesNotThrow(() -> validator.validate(ItemCategory.FASHION));
    }

    @Test
    @DisplayName("OTHER hợp lệ")
    void other() {
      assertDoesNotThrow(() -> validator.validate(ItemCategory.OTHER));
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("Tất cả category")
  class AllCategoriesTest {

    @Test
    @DisplayName("Mọi ItemCategory đều validate được")
    void allCategories() {
      for (ItemCategory cat : ItemCategory.values()) {
        assertDoesNotThrow(
            () -> validator.validate(cat),
            "Category " + cat + " không nên throw"
        );
      }
    }
  }
}