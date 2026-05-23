package vn.edu.vnu.uet.group8.common.config_mapping;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.SpecKey;

/**
 * Test cho {@link CategorySpecConfig}.
 */
class CategorySpecConfigTest {

    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getRequiredSpecs()")
    class GetRequiredSpecsTest {

        @Test
        @DisplayName("Category null → empty list")
        void categoryNull() {

            List<SpecKey> result =
                    CategorySpecConfig.getRequiredSpecs(null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("WATCHES có BRAND + MODEL")
        void watches() {

            List<SpecKey> specs =
                    CategorySpecConfig.getRequiredSpecs(ItemCategory.WATCHES);

            assertTrue(specs.contains(SpecKey.BRAND));
            assertTrue(specs.contains(SpecKey.MODEL));
        }

        @Test
        @DisplayName("ELECTRONICS có optional merged")
        void electronics() {

            List<SpecKey> specs =
                    CategorySpecConfig.getRequiredSpecs(ItemCategory.ELECTRONICS);

            assertTrue(specs.contains(SpecKey.BRAND));
            assertTrue(specs.contains(SpecKey.MODEL));

            // optional merged
            assertTrue(specs.contains(SpecKey.DIMENSIONS));
            assertTrue(specs.contains(SpecKey.WEIGHT));
        }

        @Test
        @DisplayName("Category không config → empty")
        void categoryKhongConfig() {

            List<SpecKey> specs =
                    CategorySpecConfig.getRequiredSpecs(ItemCategory.OTHER);

            assertTrue(specs.isEmpty());
        }

        @Test
        @DisplayName("Tất cả category không null")
        void tatCaCategory() {

            for (ItemCategory cat : ItemCategory.values()) {

                assertNotNull(
                        CategorySpecConfig.getRequiredSpecs(cat)
                );
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getSpecFields()")
    class GetSpecFieldsTest {

        @Test
        @DisplayName("WATCHES trả về tên field")
        void watchesFields() {

            List<String> fields =
                    CategorySpecConfig.getSpecFields(ItemCategory.WATCHES);

            assertTrue(fields.contains("BRAND"));
            assertTrue(fields.contains("MODEL"));
        }

        @Test
        @DisplayName("Category null → empty")
        void categoryNull() {

            List<String> fields =
                    CategorySpecConfig.getSpecFields(null);

            assertTrue(fields.isEmpty());
        }

        @Test
        @DisplayName("OTHER → empty")
        void other() {

            List<String> fields =
                    CategorySpecConfig.getSpecFields(ItemCategory.OTHER);

            assertTrue(fields.isEmpty());
        }
    }
}