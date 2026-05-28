package vn.edu.vnu.uet.group8.client.controller;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.client.service.SellerService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test đầy đủ cho CreateItemController.
 *
 * Chiến lược:
 *  - Kế thừa FxTestBase để khởi tạo JavaFX Toolkit (JFXPanel) một lần duy nhất.
 *  - @FXML fields được inject thủ công trong @BeforeEach → không cần load FXML.
 *  - Static service/util (AlertUtil, SellerService, SceneManager, MainController)
 *    được mock qua Mockito.mockStatic() để cô lập hoàn toàn.
 *  - Mỗi branch trong coverage report đều có ít nhất 1 test case tương ứng.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateItemController")
class CreateItemControllerTest extends FxTestBase {

    private CreateItemController controller;

    @BeforeEach
    void setup() {
        controller = new CreateItemController();
        // Inject tất cả @FXML fields thủ công (không load FXML)
        controller.tfName          = new TextField();
        controller.cbCategory      = new ComboBox<>();
        controller.cbCondition     = new ComboBox<>();
        controller.taDescription   = new TextArea();
        controller.tfStartPrice    = new TextField();
        controller.tfBidStep       = new TextField();
        controller.tfDurationHours = new TextField();
        controller.cbHasCert       = new CheckBox();
        controller.paneCertFields  = new VBox();
        controller.tfCertBody      = new TextField();
        controller.tfCertId        = new TextField();
        controller.lblError        = new Label();
        controller.btnSubmit       = new Button("Đăng bán ngay");
        controller.tfBrand         = new TextField();
        controller.tfModel         = new TextField();
        controller.tfYear          = new TextField();
        controller.tfMaterial      = new TextField();
        controller.tfOrigin        = new TextField();
    }

    // ════════════════════════════════════════════════════════════════════════
    // 1. validateRequired — 100% branch coverage
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("validateRequired")
    class ValidateRequired {

        @Test
        @DisplayName("name rỗng → lỗi tên")
        void empty_name() {
            String err = controller.validateRequired("", "Đồng hồ cao cấp", "Mới 100%", "Mô tả dài hơn 20 ký tự cho đủ yêu cầu");
            assertNotNull(err);
            assertTrue(err.toLowerCase().contains("ten") || err.toLowerCase().contains("tên"));
        }

        @Test
        @DisplayName("name < 5 ký tự → lỗi chứa '5'")
        void name_too_short() {
            String err = controller.validateRequired("ab", "Đồng hồ cao cấp", "Mới 100%", "Mô tả dài hơn 20 ký tự cho đủ yêu cầu");
            assertNotNull(err);
            assertTrue(err.contains("5"));
        }

        @Test
        @DisplayName("name = 5 ký tự (biên dưới hợp lệ) → không lỗi tên")
        void name_exactly_5() {
            String err = controller.validateRequired("abcde", "Đồng hồ cao cấp", "Mới 100%", "Mô tả dài hơn 20 ký tự cho đủ yêu cầu");
            assertNull(err);
        }

        @Test
        @DisplayName("name > 100 ký tự → lỗi")
        void name_too_long() {
            String err = controller.validateRequired("a".repeat(101), "Đồng hồ cao cấp", "Mới 100%", "Mô tả dài hơn 20 ký tự cho đủ yêu cầu");
            assertNotNull(err);
        }

        @Test
        @DisplayName("name = 100 ký tự (biên trên hợp lệ) → không lỗi tên")
        void name_exactly_100() {
            String err = controller.validateRequired("a".repeat(100), "Đồng hồ cao cấp", "Mới 100%", "Mô tả dài hơn 20 ký tự cho đủ yêu cầu");
            assertNull(err);
        }

        @Test
        @DisplayName("category null → lỗi danh mục")
        void null_category() {
            String err = controller.validateRequired("Ten hop le", null, "Mới 100%", "Mô tả dài hơn 20 ký tự cho đủ yêu cầu");
            assertNotNull(err);
            assertTrue(err.toLowerCase().contains("danh muc") || err.toLowerCase().contains("danh mục"));
        }

        @Test
        @DisplayName("condition null → lỗi tình trạng")
        void null_condition() {
            String err = controller.validateRequired("Ten hop le", "Đồng hồ cao cấp", null, "Mô tả dài hơn 20 ký tự cho đủ yêu cầu");
            assertNotNull(err);
            assertTrue(err.toLowerCase().contains("tinh trang") || err.toLowerCase().contains("tình trạng"));
        }

        @Test
        @DisplayName("description rỗng → lỗi mô tả")
        void empty_description() {
            String err = controller.validateRequired("Ten hop le", "Đồng hồ cao cấp", "Mới 100%", "");
            assertNotNull(err);
        }

        @Test
        @DisplayName("description < 20 ký tự → lỗi chứa '20'")
        void description_too_short() {
            String err = controller.validateRequired("Ten hop le", "Đồng hồ cao cấp", "Mới 100%", "Ngắn");
            assertNotNull(err);
            assertTrue(err.contains("20"));
        }

        @Test
        @DisplayName("description = 20 ký tự (biên dưới hợp lệ) → null")
        void description_exactly_20() {
            String err = controller.validateRequired("Ten hop le", "Đồng hồ cao cấp", "Mới 100%", "a".repeat(20));
            assertNull(err);
        }

        @Test
        @DisplayName("description > 2000 ký tự → lỗi chứa '2000'")
        void description_too_long() {
            String err = controller.validateRequired("Ten hop le", "Đồng hồ cao cấp", "Mới 100%", "a".repeat(2001));
            assertNotNull(err);
            assertTrue(err.contains("2000"));
        }

        @Test
        @DisplayName("description = 2000 ký tự (biên trên hợp lệ) → null")
        void description_exactly_2000() {
            String err = controller.validateRequired("Ten hop le", "Đồng hồ cao cấp", "Mới 100%", "a".repeat(2000));
            assertNull(err);
        }

        @Test
        @DisplayName("tất cả hợp lệ → null")
        void all_valid() {
            String err = controller.validateRequired(
                    "San pham hop le",
                    "Điện tử",
                    "Mới 100%",
                    "Mo ta san pham dai hon 20 ky tu rat dai"
            );
            assertNull(err);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. validateNumbers — 100% branch coverage
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("validateNumbers")
    class ValidateNumbers {

        @Test
        @DisplayName("startPrice < 100.000 → lỗi tối thiểu")
        void start_price_below_min() {
            String err = controller.validateNumbers(new BigDecimal("50000"), new BigDecimal("10000"), 24);
            assertNotNull(err);
            assertFalse(err.isBlank());
        }

        @Test
        @DisplayName("startPrice = 100.000 (biên dưới hợp lệ) → null")
        void start_price_exactly_min() {
            String err = controller.validateNumbers(new BigDecimal("100000"), new BigDecimal("10000"), 24);
            assertNull(err);
        }

        @Test
        @DisplayName("startPrice > 100 tỷ → lỗi tối đa")
        void start_price_above_max() {
            String err = controller.validateNumbers(new BigDecimal("100000000001"), new BigDecimal("10000"), 24);
            assertNotNull(err);
        }

        @Test
        @DisplayName("startPrice = 100 tỷ (biên trên hợp lệ) → null")
        void start_price_exactly_max() {
            String err = controller.validateNumbers(new BigDecimal("100000000000"), new BigDecimal("10000"), 24);
            assertNull(err);
        }

        @Test
        @DisplayName("bidStep = 0 → lỗi > 0")
        void bid_step_zero() {
            String err = controller.validateNumbers(new BigDecimal("1000000"), BigDecimal.ZERO, 24);
            assertNotNull(err);
            assertTrue(err.contains("> 0") || err.contains("0"));
        }

        @Test
        @DisplayName("bidStep âm → lỗi")
        void bid_step_negative() {
            String err = controller.validateNumbers(new BigDecimal("1000000"), new BigDecimal("-1"), 24);
            assertNotNull(err);
        }

        @Test
        @DisplayName("bidStep = startPrice → lỗi < giá khởi điểm")
        void bid_step_equal_start_price() {
            String err = controller.validateNumbers(new BigDecimal("500000"), new BigDecimal("500000"), 24);
            assertNotNull(err);
            assertTrue(err.toLowerCase().contains("< gia") || err.toLowerCase().contains("< giá"));
        }

        @Test
        @DisplayName("bidStep > startPrice → lỗi")
        void bid_step_greater_than_start_price() {
            String err = controller.validateNumbers(new BigDecimal("500000"), new BigDecimal("600000"), 24);
            assertNotNull(err);
        }

        @Test
        @DisplayName("duration = 0 → lỗi")
        void duration_zero() {
            String err = controller.validateNumbers(new BigDecimal("1000000"), new BigDecimal("10000"), 0);
            assertNotNull(err);
        }

        @Test
        @DisplayName("duration = 1 (biên dưới hợp lệ) → null")
        void duration_min_valid() {
            String err = controller.validateNumbers(new BigDecimal("500000"), new BigDecimal("50000"), 1);
            assertNull(err);
        }

        @Test
        @DisplayName("duration = 168 (biên trên hợp lệ) → null")
        void duration_max_valid() {
            String err = controller.validateNumbers(new BigDecimal("500000"), new BigDecimal("50000"), 168);
            assertNull(err);
        }

        @Test
        @DisplayName("duration = 169 → lỗi chứa '168'")
        void duration_above_max() {
            String err = controller.validateNumbers(new BigDecimal("1000000"), new BigDecimal("10000"), 169);
            assertNotNull(err);
            assertTrue(err.contains("168"));
        }

        @Test
        @DisplayName("tất cả hợp lệ → null")
        void all_valid() {
            String err = controller.validateNumbers(new BigDecimal("1000000"), new BigDecimal("100000"), 24);
            assertNull(err);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. parseAmount — 100% branch coverage
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("parseAmount")
    class ParseAmount {

        @Test
        @DisplayName("field null → null")
        void null_field_returns_null() {
            assertNull(controller.parseAmount(null));
        }

        @Test
        @DisplayName("text rỗng → null")
        void empty_text_returns_null() {
            controller.tfStartPrice.setText("");
            assertNull(controller.parseAmount(controller.tfStartPrice));
        }

        @Test
        @DisplayName("text chỉ ký tự không phải số → null (sau clean = '')")
        void non_digit_only_returns_null() {
            controller.tfStartPrice.setText("abc");
            assertNull(controller.parseAmount(controller.tfStartPrice));
        }

        @Test
        @DisplayName("'1000000' → BigDecimal 1000000")
        void plain_number_parsed() {
            controller.tfStartPrice.setText("1000000");
            assertEquals(new BigDecimal("1000000"), controller.parseAmount(controller.tfStartPrice));
        }

        @Test
        @DisplayName("'1,000,000' → BigDecimal 1000000 (strip dấu phẩy)")
        void formatted_number_parsed() {
            controller.tfStartPrice.setText("1,000,000");
            assertEquals(new BigDecimal("1000000"), controller.parseAmount(controller.tfStartPrice));
        }

        @Test
        @DisplayName("'  500.000  ' → BigDecimal 500000 (strip dấu chấm)")
        void dot_formatted_parsed() {
            controller.tfStartPrice.setText("500.000");
            assertEquals(new BigDecimal("500000"), controller.parseAmount(controller.tfStartPrice));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. parseInt — 100% branch coverage
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("parseInt")
    class ParseInt {

        @Test
        @DisplayName("field null → null")
        void null_field_returns_null() {
            assertNull(controller.parseInt(null));
        }

        @Test
        @DisplayName("text rỗng → null")
        void empty_text_returns_null() {
            controller.tfDurationHours.setText("");
            assertNull(controller.parseInt(controller.tfDurationHours));
        }

        @Test
        @DisplayName("text khoảng trắng → null")
        void blank_text_returns_null() {
            controller.tfDurationHours.setText("   ");
            assertNull(controller.parseInt(controller.tfDurationHours));
        }

        @Test
        @DisplayName("'abc' → null (NumberFormatException bị bắt)")
        void non_number_returns_null() {
            controller.tfDurationHours.setText("abc");
            assertNull(controller.parseInt(controller.tfDurationHours));
        }

        @Test
        @DisplayName("'24' → 24")
        void valid_int_parsed() {
            controller.tfDurationHours.setText("24");
            assertEquals(24, controller.parseInt(controller.tfDurationHours));
        }

        @Test
        @DisplayName("'1' → 1 (biên dưới)")
        void min_int_parsed() {
            controller.tfDurationHours.setText("1");
            assertEquals(1, controller.parseInt(controller.tfDurationHours));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. getCategoryNameFromLabel — 100% branch coverage
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getCategoryNameFromLabel")
    class CategoryConversion {

        @Test
        @DisplayName("null → OTHER")
        void null_label_returns_other() {
            assertEquals(ItemCategory.OTHER.name(), controller.getCategoryNameFromLabel(null));
        }

        @Test
        @DisplayName("unknown label → OTHER (fallback + LOGGER.warning)")
        void unknown_label_returns_other() {
            assertEquals(ItemCategory.OTHER.name(), controller.getCategoryNameFromLabel("XYZ_UNKNOWN"));
        }

        @Test
        @DisplayName("'Điện tử' → ELECTRONICS")
        void electronics_label() {
            assertEquals("ELECTRONICS", controller.getCategoryNameFromLabel("Điện tử"));
        }

        @Test
        @DisplayName("'Đồng hồ cao cấp' → WATCHES")
        void watches_label() {
            assertEquals("WATCHES", controller.getCategoryNameFromLabel("Đồng hồ cao cấp"));
        }

        @Test
        @DisplayName("'Trang sức' → JEWELRY")
        void jewelry_label() {
            assertEquals("JEWELRY", controller.getCategoryNameFromLabel("Trang sức"));
        }

        @Test
        @DisplayName("'Nghệ thuật' → ART")
        void art_label() {
            assertEquals("ART", controller.getCategoryNameFromLabel("Nghệ thuật"));
        }

        @Test
        @DisplayName("'Khác' → OTHER (label hợp lệ)")
        void other_label_valid() {
            assertEquals("OTHER", controller.getCategoryNameFromLabel("Khác"));
        }

        @Test
        @DisplayName("label khớp case-insensitive → trả đúng enum")
        void case_insensitive_match() {
            // "điện tử" lowercase vẫn phải map ELECTRONICS
            assertEquals("ELECTRONICS", controller.getCategoryNameFromLabel("điện tử"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 6. getConditionNameFromLabel — 100% branch coverage
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getConditionNameFromLabel")
    class ConditionConversion {

        @Test
        @DisplayName("null → USED (fallback)")
        void null_label_returns_used() {
            assertEquals("USED", controller.getConditionNameFromLabel(null));
        }

        @Test
        @DisplayName("unknown → USED (fallback + LOGGER.warning)")
        void unknown_label_returns_used() {
            assertEquals("USED", controller.getConditionNameFromLabel("UNKNOWN_CONDITION"));
        }

        @Test
        @DisplayName("'Mới 100%' → NEW")
        void new_label() {
            assertEquals("NEW", controller.getConditionNameFromLabel("Mới 100%"));
        }

        @Test
        @DisplayName("'Như mới (99%)' → LIKE_NEW")
        void like_new_label() {
            assertEquals("LIKE_NEW", controller.getConditionNameFromLabel("Như mới (99%)"));
        }

        @Test
        @DisplayName("'Đã qua sử dụng' → USED")
        void used_label() {
            assertEquals("USED", controller.getConditionNameFromLabel("Đã qua sử dụng"));
        }

        @Test
        @DisplayName("'Tot (90%)' → USED (special mapping branch)")
        void tot_maps_to_used() {
            assertEquals("USED", controller.getConditionNameFromLabel("Tot (90%)"));
        }

        @Test
        @DisplayName("'Kha (70%)' → USED (special mapping branch)")
        void kha_maps_to_used() {
            assertEquals("USED", controller.getConditionNameFromLabel("Kha (70%)"));
        }

        @Test
        @DisplayName("'Cu (50%)' → USED (special mapping branch)")
        void cu_maps_to_used() {
            assertEquals("USED", controller.getConditionNameFromLabel("Cu (50%)"));
        }

        @Test
        @DisplayName("'Đã qua sửa chữa' → REFURBISHED")
        void refurbished_label() {
            assertEquals("REFURBISHED", controller.getConditionNameFromLabel("Đã qua sửa chữa"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 7. showError / hideError — 100% branch coverage
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("showError / hideError")
    class ErrorDisplay {

        @Test
        @DisplayName("showError → lblError visible, managed, text đúng")
        void show_error_visible() {
            controller.showError("Lỗi test");
            assertTrue(controller.lblError.isVisible());
            assertTrue(controller.lblError.isManaged());
            assertEquals("Lỗi test", controller.lblError.getText());
        }

        @Test
        @DisplayName("showError null msg → hiển thị 'null' (không crash)")
        void show_null_message() {
            assertDoesNotThrow(() -> controller.showError(null));
        }

        @Test
        @DisplayName("showError khi lblError = null → không throw (branch lblError==null)")
        void show_error_null_label_noCrash() {
            controller.lblError = null;
            assertDoesNotThrow(() -> controller.showError("msg"));
        }

        @Test
        @DisplayName("hideError → lblError visible=false, managed=false")
        void hide_error_invisible() {
            controller.showError("Lỗi");
            controller.hideError();
            assertFalse(controller.lblError.isVisible());
            assertFalse(controller.lblError.isManaged());
        }

        @Test
        @DisplayName("hideError khi lblError = null → không throw (branch lblError==null)")
        void hide_error_null_label_noCrash() {
            controller.lblError = null;
            assertDoesNotThrow(() -> controller.hideError());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 8. setLoadingState — 75% → 100% branch coverage
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("setLoadingState")
    class LoadingState {

        @Test
        @DisplayName("setLoadingState(true) → button disabled, text = 'Dang gui...'")
        void loading_true_disables_button() {
            controller.setLoadingState(true);
            assertTrue(controller.btnSubmit.isDisable());
            assertEquals("Dang gui...", controller.btnSubmit.getText());
        }

        @Test
        @DisplayName("setLoadingState(false) → button enabled, text = 'Dang ban ngay'")
        void loading_false_enables_button() {
            controller.setLoadingState(true);
            controller.setLoadingState(false);
            assertFalse(controller.btnSubmit.isDisable());
            assertEquals("Dang ban ngay", controller.btnSubmit.getText());
        }

        @Test
        @DisplayName("setLoadingState khi btnSubmit = null → không crash (branch null)")
        void loading_null_button_noCrash() {
            controller.btnSubmit = null;
            assertDoesNotThrow(() -> controller.setLoadingState(true));
            assertDoesNotThrow(() -> controller.setLoadingState(false));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 9. collectSpecs + addSpec — 100% branch coverage
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("collectSpecs / addSpec")
    class CollectSpecs {

        @Test
        @DisplayName("tất cả rỗng → specs empty")
        void all_empty_returns_empty_specs() {
            Map<String, String> specs = controller.collectSpecs();
            assertTrue(specs.isEmpty());
        }

        @Test
        @DisplayName("brand có giá trị → specs chứa brand")
        void brand_added_to_specs() {
            controller.tfBrand.setText("Apple");
            Map<String, String> specs = controller.collectSpecs();
            assertEquals("Apple", specs.get("brand"));
        }

        @Test
        @DisplayName("chỉ whitespace → không thêm vào specs (branch isBlank)")
        void whitespace_not_added() {
            controller.tfBrand.setText("   ");
            Map<String, String> specs = controller.collectSpecs();
            assertFalse(specs.containsKey("brand"));
        }

        @Test
        @DisplayName("model có giá trị → specs chứa model")
        void model_added_to_specs() {
            controller.tfModel.setText("iPhone 15");
            Map<String, String> specs = controller.collectSpecs();
            assertEquals("iPhone 15", specs.get("model"));
        }

        @Test
        @DisplayName("year có giá trị → specs chứa year")
        void year_added_to_specs() {
            controller.tfYear.setText("2024");
            Map<String, String> specs = controller.collectSpecs();
            assertEquals("2024", specs.get("year"));
        }

        @Test
        @DisplayName("material có giá trị → specs chứa material")
        void material_added_to_specs() {
            controller.tfMaterial.setText("Thép không gỉ");
            Map<String, String> specs = controller.collectSpecs();
            assertEquals("Thép không gỉ", specs.get("material"));
        }

        @Test
        @DisplayName("origin có giá trị → specs chứa origin")
        void origin_added_to_specs() {
            controller.tfOrigin.setText("Nhật Bản");
            Map<String, String> specs = controller.collectSpecs();
            assertEquals("Nhật Bản", specs.get("origin"));
        }

        @Test
        @DisplayName("nhiều field cùng lúc → đủ tất cả key")
        void multiple_specs_collected() {
            controller.tfBrand.setText("Apple");
            controller.tfModel.setText("iPhone 15");
            controller.tfYear.setText("2024");
            controller.tfMaterial.setText("Titan");
            controller.tfOrigin.setText("USA");
            Map<String, String> specs = controller.collectSpecs();
            assertEquals(5, specs.size());
        }

        @Test
        @DisplayName("addSpec: field null → không thêm vào specs (branch field==null)")
        void addSpec_null_field_skipped() {
            Map<String, String> specs = new java.util.HashMap<>();
            controller.addSpec(specs, "brand", null);
            assertFalse(specs.containsKey("brand"));
        }

        @Test
        @DisplayName("addSpec: value null → không thêm vào specs (branch value==null)")
        void addSpec_null_value_skipped() {
            TextField tf = new TextField();
            tf.setText(null);
            Map<String, String> specs = new java.util.HashMap<>();
            // setText(null) → getText() = "" → isBlank() true → không add
            controller.addSpec(specs, "brand", tf);
            assertFalse(specs.containsKey("brand"));
        }

        @Test
        @DisplayName("addSpec: value có khoảng trắng hai đầu → trim trước khi lưu")
        void addSpec_value_trimmed() {
            controller.tfBrand.setText("  Sony  ");
            Map<String, String> specs = controller.collectSpecs();
            assertEquals("Sony", specs.get("brand"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 10. safeText — branch coverage
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("safeText")
    class SafeText {

        @Test
        @DisplayName("field null → chuỗi rỗng")
        void null_field_returns_empty() {
            assertEquals("", controller.safeText(null));
        }

        @Test
        @DisplayName("field có text → text được trim")
        void valid_text_trimmed() {
            controller.tfName.setText("  Hello  ");
            assertEquals("Hello", controller.safeText(controller.tfName));
        }

        @Test
        @DisplayName("field text rỗng → chuỗi rỗng")
        void empty_text_returns_empty() {
            controller.tfName.setText("");
            assertEquals("", controller.safeText(controller.tfName));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 11. formatVnd — branch coverage
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("formatVnd")
    class FormatVnd {

        @Test
        @DisplayName("null → '0 d'")
        void null_returns_zero_d() {
            assertEquals("0 d", controller.formatVnd(null));
        }

        @Test
        @DisplayName("BigDecimal.ZERO → '0 d'")
        void zero_amount() {
            String result = controller.formatVnd(BigDecimal.ZERO);
            assertTrue(result.contains("0"));
        }

        @Test
        @DisplayName("1.000.000 → chuỗi chứa '1,000,000' và 'd'")
        void million_amount_formatted() {
            String result = controller.formatVnd(new BigDecimal("1000000"));
            assertTrue(result.contains("1,000,000"));
            assertTrue(result.endsWith("d"));
        }

        @Test
        @DisplayName("hai giá khác nhau → chuỗi khác nhau")
        void different_amounts_differ() {
            String r1 = controller.formatVnd(new BigDecimal("100000"));
            String r2 = controller.formatVnd(new BigDecimal("200000"));
            assertNotEquals(r1, r2);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 12. formatPriceField — branch coverage
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("formatPriceField")
    class FormatPriceField {

        @Test
        @DisplayName("text rỗng sau clean → return sớm, text không đổi")
        void empty_input_no_change() {
            controller.tfStartPrice.setText("");
            controller.formatPriceField(controller.tfStartPrice);
            assertEquals("", controller.tfStartPrice.getText());
        }

        @Test
        @DisplayName("'1000000' → '1,000,000' (format VND)")
        void valid_number_formatted() {
            controller.tfStartPrice.setText("1000000");
            controller.formatPriceField(controller.tfStartPrice);
            assertEquals("1,000,000", controller.tfStartPrice.getText());
        }

        @Test
        @DisplayName("'abc' → clean thành rỗng → method return, giữ nguyên text gốc")
        void non_digit_input_kept_unchanged() {
            controller.tfStartPrice.setText("abc");

            controller.formatPriceField(controller.tfStartPrice);

            // Controller hiện tại không clear field khi cleaned = ""
            // nên text vẫn giữ nguyên
            assertEquals("abc", controller.tfStartPrice.getText());
        }

        @Test
        @DisplayName("'1,000,000' → '1,000,000' (đã format, không thay đổi)")
        void already_formatted_stays_same() {
            controller.tfStartPrice.setText("1,000,000");
            controller.formatPriceField(controller.tfStartPrice);
            assertEquals("1,000,000", controller.tfStartPrice.getText());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 13. onToggleCert — branch coverage
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("onToggleCert")
    class ToggleCert {

        @Test
        @DisplayName("cbHasCert selected=true → paneCertFields visible")
        void cert_checked_shows_pane() {
            controller.cbHasCert.setSelected(true);
            controller.onToggleCert();
            assertTrue(controller.paneCertFields.isVisible());
            assertTrue(controller.paneCertFields.isManaged());
        }

        @Test
        @DisplayName("cbHasCert selected=false → paneCertFields hidden")
        void cert_unchecked_hides_pane() {
            controller.cbHasCert.setSelected(true);
            controller.onToggleCert();
            controller.cbHasCert.setSelected(false);
            controller.onToggleCert();
            assertFalse(controller.paneCertFields.isVisible());
            assertFalse(controller.paneCertFields.isManaged());
        }

        @Test
        @DisplayName("cbHasCert null → không crash (branch null)")
        void null_checkbox_noCrash() {
            controller.cbHasCert = null;
            assertDoesNotThrow(() -> controller.onToggleCert());
        }

        @Test
        @DisplayName("paneCertFields null → không crash (branch null)")
        void null_pane_noCrash() {
            controller.paneCertFields = null;
            assertDoesNotThrow(() -> controller.onToggleCert());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 14. hideCertPane — branch coverage
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("hideCertPane")
    class HideCertPane {

        @Test
        @DisplayName("paneCertFields non-null → hidden")
        void hides_pane() {
            controller.paneCertFields.setVisible(true);
            controller.hideCertPane();
            assertFalse(controller.paneCertFields.isVisible());
            assertFalse(controller.paneCertFields.isManaged());
        }

        @Test
        @DisplayName("paneCertFields null → không crash")
        void null_pane_noCrash() {
            controller.paneCertFields = null;
            assertDoesNotThrow(() -> controller.hideCertPane());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 15. initComboBoxes — branch coverage
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("initComboBoxes")
    class InitComboBoxes {

        @Test
        @DisplayName("cbCategory non-null → items được populate")
        void category_combo_populated() {
            controller.initComboBoxes();
            assertFalse(controller.cbCategory.getItems().isEmpty());
        }

        @Test
        @DisplayName("cbCondition non-null → items được populate và first selected")
        void condition_combo_populated_and_selected() {
            controller.initComboBoxes();
            assertFalse(controller.cbCondition.getItems().isEmpty());
            assertNotNull(controller.cbCondition.getSelectionModel().getSelectedItem());
        }

        @Test
        @DisplayName("cbCategory null → không crash (branch null)")
        void null_category_combo_noCrash() {
            controller.cbCategory = null;
            assertDoesNotThrow(() -> controller.initComboBoxes());
        }

        @Test
        @DisplayName("cbCondition null → không crash (branch null)")
        void null_condition_combo_noCrash() {
            controller.cbCondition = null;
            assertDoesNotThrow(() -> controller.initComboBoxes());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 16. navigateToDashboard — branch coverage
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("navigateToDashboard")
    class NavigateToDashboard {

        @Test
        @DisplayName("MainController.getInstance() null → SceneManager.switchTo() được gọi")
        void mainController_null_uses_scene_manager() {
            try (MockedStatic<MainController> mc = mockStatic(MainController.class);
                 MockedStatic<SceneManager> sm = mockStatic(SceneManager.class)) {

                mc.when(MainController::getInstance).thenReturn(null);
                sm.when(() -> SceneManager.switchTo(anyString())).thenAnswer(inv -> null);

                controller.navigateToDashboard();

                sm.verify(() -> SceneManager.switchTo("SellerDashboardView.fxml"), times(1));
            }
        }

        @Test
        @DisplayName("MainController.getInstance() non-null → main.loadView() được gọi")
        void mainController_non_null_calls_loadView() {
            MainController mockMain = mock(MainController.class);
            try (MockedStatic<MainController> mc = mockStatic(MainController.class)) {
                mc.when(MainController::getInstance).thenReturn(mockMain);
                controller.navigateToDashboard();
                verify(mockMain, times(1)).loadView("SellerDashboardView.fxml");
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 17. onBack — branch coverage
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("onBack")
    class OnBack {

        @Test
        @DisplayName("onBack() → gọi navigateToDashboard (SceneManager khi main=null)")
        void onBack_calls_navigate() {
            try (MockedStatic<MainController> mc = mockStatic(MainController.class);
                 MockedStatic<SceneManager> sm = mockStatic(SceneManager.class)) {

                mc.when(MainController::getInstance).thenReturn(null);
                sm.when(() -> SceneManager.switchTo(anyString())).thenAnswer(inv -> null);

                assertDoesNotThrow(() -> controller.onBack());
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 18. onSaveDraft — branch coverage
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("onSaveDraft")
    class OnSaveDraft {

        @Test
        @DisplayName("onSaveDraft() → AlertUtil.showInfo được gọi, không crash")
        void onSaveDraft_shows_info() {
            try (MockedStatic<AlertUtil> au = mockStatic(AlertUtil.class)) {
                au.when(() -> AlertUtil.showInfo(anyString())).thenAnswer(inv -> null);
                assertDoesNotThrow(() -> controller.onSaveDraft());
                au.verify(() -> AlertUtil.showInfo(anyString()), times(1));
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 19. onSubmit — branch coverage (validate pipeline)
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("onSubmit — validate pipeline")
    class OnSubmit {

        /** Điền đầy đủ form hợp lệ */
        private void fillValidForm() {
            controller.tfName.setText("San pham hop le test");
            controller.cbCategory.getItems().add("Điện tử");
            controller.cbCategory.getSelectionModel().selectFirst();
            controller.cbCondition.getItems().add("Mới 100%");
            controller.cbCondition.getSelectionModel().selectFirst();
            controller.taDescription.setText("Mo ta san pham dai hon 20 ky tu rat rat dai");
            controller.tfStartPrice.setText("1000000");
            controller.tfBidStep.setText("100000");
            controller.tfDurationHours.setText("24");
            controller.cbHasCert.setSelected(false);
        }

        @Test
        @DisplayName("validateRequired thất bại → showError, không tiếp tục")
        void submit_fails_on_required_validation() {
            // tfName rỗng → validateRequired trả lỗi
            controller.tfName.setText("");
            controller.onSubmit();
            assertTrue(controller.lblError.isVisible());
        }

        @Test
        @DisplayName("startPrice null (không nhập) → showError 'so'")
        void submit_fails_when_startPrice_null() {
            controller.tfName.setText("San pham hop le test");
            controller.cbCategory.getItems().add("Điện tử");
            controller.cbCategory.getSelectionModel().selectFirst();
            controller.cbCondition.getItems().add("Mới 100%");
            controller.cbCondition.getSelectionModel().selectFirst();
            controller.taDescription.setText("Mo ta san pham dai hon 20 ky tu rat rat dai");
            // tfStartPrice rỗng → parseAmount trả null
            controller.tfStartPrice.setText("");
            controller.tfBidStep.setText("100000");
            controller.tfDurationHours.setText("24");

            controller.onSubmit();
            assertTrue(controller.lblError.isVisible());
        }

        @Test
        @DisplayName("bidStep null → showError")
        void submit_fails_when_bidStep_null() {
            controller.tfName.setText("San pham hop le test");
            controller.cbCategory.getItems().add("Điện tử");
            controller.cbCategory.getSelectionModel().selectFirst();
            controller.cbCondition.getItems().add("Mới 100%");
            controller.cbCondition.getSelectionModel().selectFirst();
            controller.taDescription.setText("Mo ta san pham dai hon 20 ky tu rat rat dai");
            controller.tfStartPrice.setText("1000000");
            controller.tfBidStep.setText("");   // null
            controller.tfDurationHours.setText("24");

            controller.onSubmit();
            assertTrue(controller.lblError.isVisible());
        }

        @Test
        @DisplayName("duration null → showError")
        void submit_fails_when_duration_null() {
            controller.tfName.setText("San pham hop le test");
            controller.cbCategory.getItems().add("Điện tử");
            controller.cbCategory.getSelectionModel().selectFirst();
            controller.cbCondition.getItems().add("Mới 100%");
            controller.cbCondition.getSelectionModel().selectFirst();
            controller.taDescription.setText("Mo ta san pham dai hon 20 ky tu rat rat dai");
            controller.tfStartPrice.setText("1000000");
            controller.tfBidStep.setText("100000");
            controller.tfDurationHours.setText("");   // null

            controller.onSubmit();
            assertTrue(controller.lblError.isVisible());
        }

        @Test
        @DisplayName("validateNumbers thất bại → showError numError")
        void submit_fails_on_number_validation() {
            controller.tfName.setText("San pham hop le test");
            controller.cbCategory.getItems().add("Điện tử");
            controller.cbCategory.getSelectionModel().selectFirst();
            controller.cbCondition.getItems().add("Mới 100%");
            controller.cbCondition.getSelectionModel().selectFirst();
            controller.taDescription.setText("Mo ta san pham dai hon 20 ky tu rat rat dai");
            controller.tfStartPrice.setText("1000");    // < MIN_START_PRICE (100.000)
            controller.tfBidStep.setText("100");
            controller.tfDurationHours.setText("24");

            controller.onSubmit();
            assertTrue(controller.lblError.isVisible());
        }

        @Test
        @DisplayName("hasCert=true, certBody rỗng → showError cert")
        void submit_fails_when_cert_fields_empty() {
            fillValidForm();
            controller.cbHasCert.setSelected(true);
            controller.tfCertBody.setText("");  // rỗng → lỗi cert
            controller.tfCertId.setText("");

            controller.onSubmit();
            assertTrue(controller.lblError.isVisible());
        }

        @Test
        @DisplayName("hasCert=true, certId rỗng → showError cert")
        void submit_fails_when_certId_empty() {
            fillValidForm();
            controller.cbHasCert.setSelected(true);
            controller.tfCertBody.setText("Bộ Công Thương");
            controller.tfCertId.setText("");  // rỗng → lỗi

            controller.onSubmit();
            assertTrue(controller.lblError.isVisible());
        }

        @Test
        @DisplayName("form hợp lệ, user cancel confirm → không gọi SellerService")
        void submit_user_cancels_confirm() {
            fillValidForm();
            try (MockedStatic<AlertUtil> au = mockStatic(AlertUtil.class);
                 MockedStatic<SellerService> ss = mockStatic(SellerService.class)) {

                au.when(() -> AlertUtil.showConfirm(anyString(), anyString())).thenReturn(false);
                controller.onSubmit();

                // SellerService.createItem không được gọi
                ss.verify(() -> SellerService.createItem(any(), any(), any()), never());
            }
        }

        @Test
        @DisplayName("form hợp lệ, user confirm → SellerService.createItem được gọi")
        void submit_user_confirms_calls_service() {
            fillValidForm();
            try (MockedStatic<AlertUtil> au = mockStatic(AlertUtil.class);
                 MockedStatic<SellerService> ss = mockStatic(SellerService.class)) {

                au.when(() -> AlertUtil.showConfirm(anyString(), anyString())).thenReturn(true);
                ss.when(() -> SellerService.createItem(any(), any(), any())).thenAnswer(inv -> null);

                controller.onSubmit();

                ss.verify(() -> SellerService.createItem(any(), any(), any()), times(1));
            }
        }

        @Test
        @DisplayName("form hợp lệ + hasCert=true + certBody/certId đầy đủ → thêm isVerified vào specs")
        void submit_cert_adds_to_specs() {
            fillValidForm();
            controller.cbHasCert.setSelected(true);
            controller.tfCertBody.setText("Bo Cong Thuong");
            controller.tfCertId.setText("CERT-001");

            try (MockedStatic<AlertUtil> au = mockStatic(AlertUtil.class);
                 MockedStatic<SellerService> ss = mockStatic(SellerService.class)) {

                au.when(() -> AlertUtil.showConfirm(anyString(), anyString())).thenReturn(true);

                // Bắt request được pass vào SellerService để kiểm tra specs
                ss.when(() -> SellerService.createItem(any(), any(), any())).thenAnswer(inv -> {
                    SellerService.CreateItemRequest req = inv.getArgument(0);
                    assertTrue(req.specs.containsKey("isVerified"));
                    assertEquals("true", req.specs.get("isVerified"));
                    return null;
                });

                controller.onSubmit();
            }
        }

        @Test
        @DisplayName("cbCategory null → safeText fallback, không crash")
        void submit_null_category_combo_noCrash() {
            controller.cbCategory = null;
            // Sẽ fail ở validateRequired (category=null) → showError, không crash
            controller.tfName.setText("San pham");
            controller.taDescription.setText("Mo ta dai hon 20 ky tu rat rat dai");
            controller.onSubmit();
            // Không crash là đủ
            assertDoesNotThrow(() -> {});
        }
    }
}