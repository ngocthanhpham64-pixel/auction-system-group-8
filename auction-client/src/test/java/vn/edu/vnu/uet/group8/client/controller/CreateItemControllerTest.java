package vn.edu.vnu.uet.group8.client.controller;

import javafx.scene.control.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CreateItemController")
class CreateItemControllerTest extends FxTestBase {

    private CreateItemController controller;

    @BeforeEach
    void setup() {
        controller = new CreateItemController();
        controller.tfName         = new TextField();
        controller.cbCategory     = new ComboBox<>();
        controller.cbCondition    = new ComboBox<>();
        controller.taDescription  = new TextArea();
        controller.tfStartPrice   = new TextField();
        controller.tfBidStep      = new TextField();
        controller.tfDurationHours = new TextField();
        controller.cbHasCert      = new CheckBox();
        controller.paneCertFields = new javafx.scene.layout.VBox();
        controller.tfCertBody     = new TextField();
        controller.tfCertId       = new TextField();
        controller.lblError       = new Label();
        controller.btnSubmit      = new Button("Đăng bán ngay");
        controller.tfBrand        = new TextField();
        controller.tfModel        = new TextField();
        controller.tfYear         = new TextField();
        controller.tfMaterial     = new TextField();
        controller.tfOrigin       = new TextField();
    }

    // ─────────── validateRequired ───────────

    @Nested @DisplayName("validateRequired")
    class ValidateRequired {

        @Test @DisplayName("name rỗng → lỗi")
        void empty_name() {
            String err = controller.validateRequired("", "Đồng hồ", "Mới", "Mô tả dài hơn 20 ký tự cho đủ yêu cầu");
            assertNotNull(err);
            assertTrue(err.contains("tên") || err.contains("ten"));
        }

        @Test @DisplayName("name < 5 ký tự → lỗi")
        void name_too_short() {
            String err = controller.validateRequired("ab", "Đồng hồ", "Mới", "Mô tả dài hơn 20 ký tự cho đủ yêu cầu");
            assertNotNull(err);
            assertTrue(err.contains("5"));
        }

        @Test @DisplayName("name > 100 ký tự → lỗi")
        void name_too_long() {
            String err = controller.validateRequired("a".repeat(101), "Đồng hồ", "Mới", "Mô tả dài hơn 20 ký tự cho đủ yêu cầu");
            assertNotNull(err);
        }

        @Test @DisplayName("category null → lỗi")
        void null_category() {
            String err = controller.validateRequired("Sản phẩm test", null, "Mới", "Mô tả dài hơn 20 ký tự cho đủ yêu cầu");
            assertNotNull(err);
            assertTrue(err.contains("danh muc") || err.contains("danh mục"));
        }

        @Test @DisplayName("condition null → lỗi")
        void null_condition() {
            String err = controller.validateRequired("Sản phẩm test", "Đồng hồ", null, "Mô tả dài hơn 20 ký tự cho đủ yêu cầu");
            assertNotNull(err);
            assertTrue(err.contains("tinh trang") || err.contains("tình trạng"));
        }

        @Test @DisplayName("description rỗng → lỗi")
        void empty_description() {
            String err = controller.validateRequired("Sản phẩm test", "Đồng hồ", "Mới", "");
            assertNotNull(err);
        }

        @Test @DisplayName("description < 20 ký tự → lỗi")
        void description_too_short() {
            String err = controller.validateRequired("Sản phẩm test", "Đồng hồ", "Mới", "Ngắn");
            assertNotNull(err);
            assertTrue(err.contains("20"));
        }

        @Test @DisplayName("description > 2000 ký tự → lỗi")
        void description_too_long() {
            String err = controller.validateRequired("Sản phẩm test", "Đồng hồ", "Mới", "a".repeat(2001));
            assertNotNull(err);
            assertTrue(err.contains("2000"));
        }

        @Test @DisplayName("tất cả hợp lệ → null")
        void all_valid() {
            String err = controller.validateRequired(
                    "Sản phẩm hợp lệ",
                    "Đồng hồ",
                    "Mới",
                    "Mô tả sản phẩm dài hơn 20 ký tự rất dài"
            );
            assertNull(err);
        }
    }

    // ─────────── validateNumbers ───────────

    @Nested @DisplayName("validateNumbers")
    class ValidateNumbers {

        @Test
        @DisplayName("startPrice < 100000 → lỗi tối thiểu")
        void start_price_below_min() {
            String err = controller.validateNumbers(
                    new BigDecimal("50000"),
                    new BigDecimal("10000"),
                    24
            );

            assertNotNull(err);
            assertFalse(err.isBlank());
        }

        @Test @DisplayName("startPrice > 100 tỷ → lỗi tối đa")
        void start_price_above_max() {
            String err = controller.validateNumbers(new BigDecimal("100000000001"), new BigDecimal("10000"), 24);
            assertNotNull(err);
        }

        @Test @DisplayName("bidStep <= 0 → lỗi")
        void bid_step_zero() {
            String err = controller.validateNumbers(new BigDecimal("1000000"), BigDecimal.ZERO, 24);
            assertNotNull(err);
            assertTrue(err.contains("> 0") || err.contains("0"));
        }

        @Test @DisplayName("bidStep âm → lỗi")
        void bid_step_negative() {
            String err = controller.validateNumbers(new BigDecimal("1000000"), new BigDecimal("-1"), 24);
            assertNotNull(err);
        }

        @Test @DisplayName("bidStep >= startPrice → lỗi")
        void bid_step_gte_start_price() {
            String err = controller.validateNumbers(new BigDecimal("500000"), new BigDecimal("500000"), 24);
            assertNotNull(err);
            assertTrue(err.contains("< gia") || err.contains("< giá"));
        }

        @Test @DisplayName("duration < 1 → lỗi")
        void duration_below_min() {
            String err = controller.validateNumbers(new BigDecimal("1000000"), new BigDecimal("10000"), 0);
            assertNotNull(err);
        }

        @Test @DisplayName("duration > 168 → lỗi")
        void duration_above_max() {
            String err = controller.validateNumbers(new BigDecimal("1000000"), new BigDecimal("10000"), 169);
            assertNotNull(err);
            assertTrue(err.contains("168"));
        }

        @Test @DisplayName("tất cả hợp lệ → null")
        void all_valid() {
            String err = controller.validateNumbers(new BigDecimal("1000000"), new BigDecimal("100000"), 24);
            assertNull(err);
        }

        @Test @DisplayName("duration = 1 → hợp lệ (biên dưới)")
        void duration_min_valid() {
            String err = controller.validateNumbers(new BigDecimal("500000"), new BigDecimal("50000"), 1);
            assertNull(err);
        }

        @Test @DisplayName("duration = 168 → hợp lệ (biên trên)")
        void duration_max_valid() {
            String err = controller.validateNumbers(new BigDecimal("500000"), new BigDecimal("50000"), 168);
            assertNull(err);
        }
    }

    // ─────────── parseAmount / parseInt ───────────

    @Nested @DisplayName("parseAmount / parseInt helpers")
    class ParseHelpers {

        @Test @DisplayName("parseAmount '1000000' → BigDecimal 1000000")
        void parse_plain_number() {
            controller.tfStartPrice.setText("1000000");
            BigDecimal result = controller.parseAmount(controller.tfStartPrice);
            assertEquals(new BigDecimal("1000000"), result);
        }

        @Test @DisplayName("parseAmount '1,000,000' → BigDecimal 1000000 (strip dấu phẩy)")
        void parse_formatted_number() {
            controller.tfStartPrice.setText("1,000,000");
            BigDecimal result = controller.parseAmount(controller.tfStartPrice);
            assertEquals(new BigDecimal("1000000"), result);
        }

        @Test @DisplayName("parseAmount rỗng → null")
        void parse_empty_returns_null() {
            controller.tfStartPrice.setText("");
            assertNull(controller.parseAmount(controller.tfStartPrice));
        }

        @Test @DisplayName("parseAmount chỉ chữ → null")
        void parse_text_only_returns_null() {
            controller.tfStartPrice.setText("abc");
            assertNull(controller.parseAmount(controller.tfStartPrice));
        }

        @Test @DisplayName("parseAmount null field → null")
        void parse_null_field_returns_null() {
            assertNull(controller.parseAmount(null));
        }

        @Test @DisplayName("parseInt '24' → 24")
        void parse_int_valid() {
            controller.tfDurationHours.setText("24");
            assertEquals(24, controller.parseInt(controller.tfDurationHours));
        }

        @Test @DisplayName("parseInt rỗng → null")
        void parse_int_empty_returns_null() {
            controller.tfDurationHours.setText("");
            assertNull(controller.parseInt(controller.tfDurationHours));
        }

        @Test @DisplayName("parseInt 'abc' → null")
        void parse_int_text_returns_null() {
            controller.tfDurationHours.setText("abc");
            assertNull(controller.parseInt(controller.tfDurationHours));
        }

        @Test @DisplayName("parseInt null field → null")
        void parse_int_null_field() {
            assertNull(controller.parseInt(null));
        }
    }

    // ─────────── getCategoryNameFromLabel ───────────

    @Nested @DisplayName("getCategoryNameFromLabel")
    class CategoryConversion {

        @Test @DisplayName("null label → OTHER")
        void null_label_returns_other() {
            assertEquals("OTHER", controller.getCategoryNameFromLabel(null));
        }

        @Test @DisplayName("unknown label → OTHER (fallback)")
        void unknown_label_returns_other() {
            assertEquals("OTHER", controller.getCategoryNameFromLabel("XYZ_UNKNOWN_CATEGORY"));
        }

        @Test @DisplayName("hợp lệ label → đúng enum name")
        void valid_label_returns_enum_name() {
            // Mỗi ItemCategory có getLabel() → match equalsIgnoreCase
            // Test với label của ELECTRONICS
            String result = controller.getCategoryNameFromLabel("Điện tử");
            assertNotNull(result);
            assertFalse(result.isBlank());
        }
    }

    // ─────────── getConditionNameFromLabel ───────────

    @Nested @DisplayName("getConditionNameFromLabel")
    class ConditionConversion {

        @Test @DisplayName("null label → USED (fallback)")
        void null_label_returns_used() {
            assertEquals("USED", controller.getConditionNameFromLabel(null));
        }

        @Test @DisplayName("unknown label → USED (fallback)")
        void unknown_returns_used() {
            assertEquals("USED", controller.getConditionNameFromLabel("XYZ"));
        }
    }

    // ─────────── showError / hideError ───────────

    @Nested @DisplayName("showError / hideError")
    class ErrorDisplay {

        @Test @DisplayName("showError → visible=true")
        void show_error_visible() {
            controller.showError("Lỗi test");
            assertTrue(controller.lblError.isVisible());
            assertTrue(controller.lblError.isManaged());
            assertEquals("Lỗi test", controller.lblError.getText());
        }

        @Test @DisplayName("hideError → visible=false")
        void hide_error() {
            controller.showError("X");
            controller.hideError();
            assertFalse(controller.lblError.isVisible());
        }

        @Test @DisplayName("showError null label → không throw")
        void show_null_label() {
            controller.lblError = null;
            assertDoesNotThrow(() -> controller.showError("msg"));
        }

        @Test @DisplayName("setLoadingState(true) → button disabled")
        void loading_state_disabled() {
            controller.setLoadingState(true);
            assertTrue(controller.btnSubmit.isDisable());
            assertEquals("Dang gui...", controller.btnSubmit.getText());
        }

        @Test @DisplayName("setLoadingState(false) → button enabled")
        void loading_state_enabled() {
            controller.setLoadingState(true);
            controller.setLoadingState(false);
            assertFalse(controller.btnSubmit.isDisable());
        }
    }

    // ─────────── collectSpecs ───────────

    @Nested @DisplayName("collectSpecs")
    class CollectSpecs {

        @Test @DisplayName("tất cả rỗng → specs rỗng")
        void all_empty_specs() {
            var specs = controller.collectSpecs();
            assertTrue(specs.isEmpty());
        }

        @Test @DisplayName("brand có giá trị → spec chứa brand")
        void brand_in_specs() {
            controller.tfBrand.setText("Apple");
            var specs = controller.collectSpecs();
            assertEquals("Apple", specs.get("brand"));
        }

        @Test @DisplayName("chỉ whitespace → không thêm vào specs")
        void whitespace_not_added() {
            controller.tfBrand.setText("   ");
            var specs = controller.collectSpecs();
            assertFalse(specs.containsKey("brand"));
        }

        @Test @DisplayName("nhiều field → nhiều spec")
        void multiple_specs() {
            controller.tfBrand.setText("Apple");
            controller.tfModel.setText("iPhone 15");
            controller.tfYear.setText("2024");
            var specs = controller.collectSpecs();
            assertEquals(3, specs.size());
            assertEquals("Apple", specs.get("brand"));
            assertEquals("iPhone 15", specs.get("model"));
            assertEquals("2024", specs.get("year"));
        }
    }
}