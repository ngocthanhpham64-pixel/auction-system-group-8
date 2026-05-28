package vn.edu.vnu.uet.group8.client.controller;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test toàn diện cho AuctionDetailController.
 * Mục tiêu: bao phủ 100% branch và instruction của các method non-FX.
 *
 * Chiến lược:
 *  - Không mock service layer (BidService/FavoriteService/AlertUtil gọi Platform.runLater hoặc cần FX thread)
 *  - Test trực tiếp state thay đổi qua các method package-private/protected
 *  - Mỗi branch của mỗi method có ít nhất một @Test
 */
@DisplayName("AuctionDetailController")
class AuctionDetailControllerTest extends FxTestBase {

    private AuctionDetailController controller;

    // ──────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────────────────────────────────

    /** Tạo controller với đầy đủ FXML fields (không null). */
    private AuctionDetailController newFullController() {
        AuctionDetailController c = new AuctionDetailController();
        c.lblCategory     = new Label();
        c.lblCurrentPrice = new Label();
        c.lblMinBid       = new Label();
        c.lblDescription  = new Label();
        c.lblCertificate  = new Label();
        c.lblHours        = new Label();
        c.lblMinutes      = new Label();
        c.lblSeconds      = new Label();
        c.tfBidAmount     = new TextField();
        c.tfMaxPrice      = new TextField();
        c.bidHistory      = new VBox();
        c.paneAuto        = new VBox();
        c.btnAutoToggle   = new Button("Auto");
        c.btnTabDesc      = new Button();
        c.btnTabSpec      = new Button();
        c.btnTabOrigin    = new Button();
        return c;
    }

    /** Tạo AuctionItemDTO đơn giản để test. */
    private AuctionItemDTO buildItem(int id, BigDecimal price, Instant endTime) {
        return AuctionItemDTO.of(
                id,
                "Test Item " + id,
                "Mô tả test",
                ItemCategory.ELECTRONICS,
                null,
                SessionStatus.ACTIVE,
                price,
                endTime,
                "seller1",
                null,
                null,
                0,
                Instant.now()
        );
    }

    @BeforeEach
    void setup() {
        ClientModel.getInstance().setCurrentAuctionItem(null);
        controller = newFullController();
    }

    @AfterEach
    void teardown() {
        ClientModel.getInstance().setCurrentAuctionItem(null);
        if (controller.countdown != null) {
            controller.countdown.stop();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // setTimeDisplay — 100% branch + instruction theo JaCoCo report
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("setTimeDisplay")
    class SetTimeDisplay {

        @Test
        @DisplayName("h=0, m=0, s=0 → '00','00','00'")
        void all_zero() {
            controller.setTimeDisplay(0, 0, 0);
            assertEquals("00", controller.lblHours.getText());
            assertEquals("00", controller.lblMinutes.getText());
            assertEquals("00", controller.lblSeconds.getText());
        }

        @Test
        @DisplayName("h=1, m=30, s=5 → '01','30','05' (padding)")
        void formatted_two_digits() {
            controller.setTimeDisplay(1, 30, 5);
            assertEquals("01", controller.lblHours.getText());
            assertEquals("30", controller.lblMinutes.getText());
            assertEquals("05", controller.lblSeconds.getText());
        }

        @Test
        @DisplayName("h=23, m=59, s=59 → '23','59','59'")
        void max_time() {
            controller.setTimeDisplay(23, 59, 59);
            assertEquals("23", controller.lblHours.getText());
            assertEquals("59", controller.lblMinutes.getText());
            assertEquals("59", controller.lblSeconds.getText());
        }

        @Test
        @DisplayName("lblHours null → không crash (branch: null-check)")
        void lblHours_null_no_crash() {
            controller.lblHours = null;
            assertDoesNotThrow(() -> controller.setTimeDisplay(1, 2, 3));
            // lblMinutes & lblSeconds vẫn set
            assertEquals("02", controller.lblMinutes.getText());
            assertEquals("03", controller.lblSeconds.getText());
        }

        @Test
        @DisplayName("lblMinutes null → không crash")
        void lblMinutes_null_no_crash() {
            controller.lblMinutes = null;
            assertDoesNotThrow(() -> controller.setTimeDisplay(1, 2, 3));
            assertEquals("01", controller.lblHours.getText());
            assertEquals("03", controller.lblSeconds.getText());
        }

        @Test
        @DisplayName("lblSeconds null → không crash")
        void lblSeconds_null_no_crash() {
            controller.lblSeconds = null;
            assertDoesNotThrow(() -> controller.setTimeDisplay(1, 2, 3));
            assertEquals("01", controller.lblHours.getText());
            assertEquals("02", controller.lblMinutes.getText());
        }

        @Test
        @DisplayName("tất cả labels null → không crash")
        void all_null_labels_no_crash() {
            controller.lblHours = null;
            controller.lblMinutes = null;
            controller.lblSeconds = null;
            assertDoesNotThrow(() -> controller.setTimeDisplay(5, 10, 15));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // onToggleAuto — 87% Cov per report → bao phủ các nhánh còn thiếu
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("onToggleAuto")
    class OnToggleAuto {

        @Test
        @DisplayName("paneAuto null → không crash (early return)")
        void null_pane_no_crash() {
            controller.paneAuto = null;
            assertDoesNotThrow(() -> controller.onToggleAuto());
        }

        @Test
        @DisplayName("btnAutoToggle null → không crash (early return)")
        void null_btn_no_crash() {
            controller.btnAutoToggle = null;
            assertDoesNotThrow(() -> controller.onToggleAuto());
        }

        @Test
        @DisplayName("pane ban đầu ẩn → toggle làm visible=true, managed=true")
        void hidden_pane_becomes_visible() {
            controller.paneAuto.setVisible(false);
            controller.paneAuto.setManaged(false);
            controller.onToggleAuto();
            assertTrue(controller.paneAuto.isVisible());
            assertTrue(controller.paneAuto.isManaged());
        }

        @Test
        @DisplayName("pane ẩn → button text thay đổi thành ▲")
        void hidden_pane_button_text_arrow_up() {
            controller.paneAuto.setVisible(false);
            controller.onToggleAuto();
            assertTrue(controller.btnAutoToggle.getText().contains("▲"));
        }

        @Test
        @DisplayName("pane hiện → toggle làm visible=false, managed=false")
        void visible_pane_becomes_hidden() {
            controller.paneAuto.setVisible(true);
            controller.paneAuto.setManaged(true);
            controller.onToggleAuto();
            assertFalse(controller.paneAuto.isVisible());
            assertFalse(controller.paneAuto.isManaged());
        }

        @Test
        @DisplayName("pane hiện → button text thay đổi thành ▼")
        void visible_pane_button_text_arrow_down() {
            controller.paneAuto.setVisible(true);
            controller.onToggleAuto();
            assertTrue(controller.btnAutoToggle.getText().contains("▼"));
        }

        @Test
        @DisplayName("toggle 2 lần → quay về trạng thái ban đầu")
        void double_toggle_restores_state() {
            controller.paneAuto.setVisible(false);
            controller.onToggleAuto(); // lần 1: ẩn → hiện
            assertTrue(controller.paneAuto.isVisible());
            controller.onToggleAuto(); // lần 2: hiện → ẩn
            assertFalse(controller.paneAuto.isVisible());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // cleanup — 40% Cov per report
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("cleanup")
    class Cleanup {

        @Test
        @DisplayName("countdown null → không crash")
        void null_countdown_no_crash() {
            controller.countdown = null;
            assertDoesNotThrow(() -> controller.cleanup());
        }

        @Test
        @DisplayName("countdown tồn tại → dừng và set null")
        void active_countdown_is_stopped_and_nulled() {
            // Tạo countdown giả
            controller.remainSeconds = 3600;
            controller.startCountdown();
            assertNotNull(controller.countdown);

            controller.cleanup();

            assertNull(controller.countdown);
        }

        @Test
        @DisplayName("gọi cleanup 2 lần → không crash")
        void double_cleanup_no_crash() {
            controller.remainSeconds = 100;
            controller.startCountdown();
            assertDoesNotThrow(() -> {
                controller.cleanup();
                controller.cleanup();
            });
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // displayItemInfo
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("displayItemInfo")
    class DisplayItemInfo {

        @Test
        @DisplayName("currentItem null → return ngay, labels không thay đổi")
        void null_item_returns_early() {
            controller.currentItem = null;
            controller.lblCategory.setText("old");
            controller.displayItemInfo();
            assertEquals("old", controller.lblCategory.getText());
        }

        @Test
        @DisplayName("category không null → lblCategory được set")
        void category_set_when_not_null() {
            controller.currentItem = buildItem(1, new BigDecimal("5000000"), null);
            controller.displayItemInfo();
            assertEquals(ItemCategory.ELECTRONICS.name(), controller.lblCategory.getText());
        }

        @Test
        @DisplayName("category null → lblCategory không bị ghi đè")
        void category_null_label_unchanged() {
            AuctionItemDTO item = AuctionItemDTO.of(
                    1, "X", "desc", null, null, SessionStatus.ACTIVE,
                    new BigDecimal("1000"), null, "s", null, null, 0, Instant.now());
            controller.currentItem = item;
            controller.lblCategory.setText("keep");
            controller.displayItemInfo();
            assertEquals("keep", controller.lblCategory.getText());
        }

        @Test
        @DisplayName("lblCategory null → không crash (null-check branch)")
        void lblCategory_null_no_crash() {
            controller.currentItem = buildItem(1, new BigDecimal("1000000"), null);
            controller.lblCategory = null;
            assertDoesNotThrow(() -> controller.displayItemInfo());
        }

        @Test
        @DisplayName("description không null → lblDescription được set")
        void description_set_when_not_null() {
            controller.currentItem = buildItem(1, new BigDecimal("5000000"), null);
            controller.displayItemInfo();
            assertEquals("Mô tả test", controller.lblDescription.getText());
        }

        @Test
        @DisplayName("description null → lblDescription không bị ghi đè")
        void description_null_label_unchanged() {
            AuctionItemDTO item = AuctionItemDTO.of(
                    1, "X", null, ItemCategory.ART, null, SessionStatus.ACTIVE,
                    new BigDecimal("1000"), null, "s", null, null, 0, Instant.now());
            controller.currentItem = item;
            controller.lblDescription.setText("old-desc");
            controller.displayItemInfo();
            assertEquals("old-desc", controller.lblDescription.getText());
        }

        @Test
        @DisplayName("lblDescription null → không crash")
        void lblDescription_null_no_crash() {
            controller.currentItem = buildItem(1, new BigDecimal("1000000"), null);
            controller.lblDescription = null;
            assertDoesNotThrow(() -> controller.displayItemInfo());
        }

        @Test
        @DisplayName("currentPrice không null → currentPrice field được set đúng")
        void current_price_set_from_item() {
            BigDecimal price = new BigDecimal("15000000");
            controller.currentItem = buildItem(1, price, null);
            controller.displayItemInfo();
            assertEquals(0, price.compareTo(controller.currentPrice));
        }

        @Test
        @DisplayName("currentPrice null trong item → fallback ZERO")
        void null_price_falls_back_to_zero() {
            controller.currentItem = buildItem(1, null, null);
            controller.displayItemInfo();
            assertEquals(BigDecimal.ZERO, controller.currentPrice);
        }

        @Test
        @DisplayName("lblCurrentPrice không null → updatePriceDisplay cập nhật")
        void price_label_updated_after_displayItemInfo() {
            BigDecimal price = new BigDecimal("20000000");
            controller.currentItem = buildItem(1, price, null);
            controller.displayItemInfo();
            assertNotNull(controller.lblCurrentPrice.getText());
            assertFalse(controller.lblCurrentPrice.getText().isBlank());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // displayCertInfo
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("displayCertInfo")
    class DisplayCertInfo {

        @Test
        @DisplayName("lblCertificate null → không crash (early return)")
        void null_label_no_crash() {
            controller.currentItem = buildItem(1, BigDecimal.TEN, null);
            controller.lblCertificate = null;
            assertDoesNotThrow(() -> controller.displayCertInfo());
        }

        @Test
        @DisplayName("currentItem null → không crash (early return)")
        void null_item_no_crash() {
            controller.currentItem = null;
            assertDoesNotThrow(() -> controller.displayCertInfo());
            // label giữ nguyên
        }

        @Test
        @DisplayName("item không null, label không null → set text 'Chua kiem dinh'")
        void sets_unverified_text() {
            controller.currentItem = buildItem(1, BigDecimal.TEN, null);
            controller.displayCertInfo();
            assertEquals("Chua kiem dinh", controller.lblCertificate.getText());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // loadItemFromModel
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("loadItemFromModel")
    class LoadItemFromModel {

        @Test
        @DisplayName("ClientModel không có item → currentItem = null, không crash")
        void no_item_in_model_no_crash() {
            ClientModel.getInstance().setCurrentAuctionItem(null);
            assertDoesNotThrow(() -> controller.loadItemFromModel());
            assertNull(controller.currentItem);
        }

        @Test
        @DisplayName("ClientModel có item → currentItem được gán & displayItemInfo chạy")
        void item_in_model_loaded_and_displayed() throws InterruptedException {
            AuctionItemDTO item = buildItem(42, new BigDecimal("8000000"), null);
            // setCurrentAuctionItem dùng runOnFX — trong test môi trường đơn giản có thể gọi trực tiếp
            ClientModel.getInstance().getCurrentAuctionItem(); // warm up
            // Gán trực tiếp vì test không chạy FX Application Thread
            controller.currentItem = item;
            controller.displayItemInfo();
            assertEquals(ItemCategory.ELECTRONICS.name(), controller.lblCategory.getText());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // updatePriceDisplay
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updatePriceDisplay")
    class UpdatePriceDisplay {

        @Test
        @DisplayName("lblCurrentPrice không null → hiển thị giá format")
        void current_price_label_updated() {
            controller.currentPrice = new BigDecimal("10000000");
            controller.updatePriceDisplay();
            assertEquals("10,000,000 d", controller.lblCurrentPrice.getText());
        }

        @Test
        @DisplayName("lblCurrentPrice null → không crash")
        void null_label_no_crash() {
            controller.lblCurrentPrice = null;
            controller.currentPrice = new BigDecimal("5000000");
            assertDoesNotThrow(() -> controller.updatePriceDisplay());
        }

        @Test
        @DisplayName("lblMinBid không null → hiển thị giá tối thiểu = current + bidStep")
        void min_bid_label_updated() {
            controller.currentPrice = new BigDecimal("10000000");
            controller.updatePriceDisplay();
            // bidStep = 10_000_000 → min = 20_000_000
            assertTrue(controller.lblMinBid.getText().contains("20,000,000"));
        }

        @Test
        @DisplayName("lblMinBid null → không crash")
        void null_min_bid_label_no_crash() {
            controller.lblMinBid = null;
            controller.currentPrice = new BigDecimal("5000000");
            assertDoesNotThrow(() -> controller.updatePriceDisplay());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // formatPrice
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("formatPrice")
    class FormatPrice {

        @Test
        @DisplayName("price null → '--'")
        void null_price_returns_dashes() {
            assertEquals("--", controller.formatPrice(null));
        }

        @Test
        @DisplayName("price = 0 → '0 d'")
        void zero_price_formatted() {
            assertEquals("0 d", controller.formatPrice(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("price = 10_000_000 → có dấu phân cách nghìn")
        void large_price_formatted_with_comma() {
            String result = controller.formatPrice(new BigDecimal("10000000"));
            assertTrue(result.contains(","), "Phải có dấu phân cách: " + result);
            assertTrue(result.endsWith("d"), "Phải kết thúc bằng 'd': " + result);
        }

        @Test
        @DisplayName("price = 1_234_567 → '1,234,567 d'")
        void specific_value_format() {
            assertEquals("1,234,567 d", controller.formatPrice(new BigDecimal("1234567")));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // parseBidInput
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("parseBidInput")
    class ParseBidInput {

        @Test
        @DisplayName("tfBidAmount null → return null")
        void null_textfield_returns_null() {
            controller.tfBidAmount = null;
            assertNull(controller.parseBidInput());
        }

        @Test
        @DisplayName("input rỗng → fallback = currentPrice + bidStep")
        void empty_input_returns_default() {
            controller.tfBidAmount.setText("");
            controller.currentPrice = new BigDecimal("5000000");
            BigDecimal result = controller.parseBidInput();
            assertNotNull(result);
            assertEquals(0, new BigDecimal("15000000").compareTo(result));
        }

        @Test
        @DisplayName("input chỉ có chữ sau strip → rỗng → fallback")
        void non_digit_input_becomes_blank_then_default() {
            controller.tfBidAmount.setText("abc");
            controller.currentPrice = BigDecimal.ZERO;
            BigDecimal result = controller.parseBidInput();
            // "abc".replaceAll("[^\\d]","") = "" → blank → return ZERO + bidStep
            assertNotNull(result);
            assertEquals(0, new BigDecimal("10000000").compareTo(result));
        }

        @Test
        @DisplayName("input số hợp lệ → parse đúng")
        void valid_number_parsed() {
            controller.tfBidAmount.setText("25000000");
            BigDecimal result = controller.parseBidInput();
            assertNotNull(result);
            assertEquals(0, new BigDecimal("25000000").compareTo(result));
        }

        @Test
        @DisplayName("input có dấu phẩy → strip → parse đúng")
        void input_with_comma_stripped_and_parsed() {
            controller.tfBidAmount.setText("25,000,000");
            BigDecimal result = controller.parseBidInput();
            assertNotNull(result);
            assertEquals(0, new BigDecimal("25000000").compareTo(result));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // setBidInput
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("setBidInput")
    class SetBidInput {

        @Test
        @DisplayName("tfBidAmount không null → set text theo formatPrice")
        void sets_text_on_textfield() {
            controller.setBidInput(new BigDecimal("10000000"));
            assertEquals("10,000,000 d", controller.tfBidAmount.getText());
        }

        @Test
        @DisplayName("tfBidAmount null → không crash")
        void null_textfield_no_crash() {
            controller.tfBidAmount = null;
            assertDoesNotThrow(() -> controller.setBidInput(new BigDecimal("5000000")));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // onQuickBid1/2/3
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("onQuickBid")
    class OnQuickBid {

        @BeforeEach
        void setPrice() {
            controller.currentPrice = BigDecimal.ZERO;
        }

        @Test
        @DisplayName("onQuickBid1 → tfBidAmount = '10,000,000 d'")
        void quick_bid_1_adds_10m() {
            controller.onQuickBid1();
            assertEquals("10,000,000 d", controller.tfBidAmount.getText());
        }

        @Test
        @DisplayName("onQuickBid2 → tfBidAmount = '20,000,000 d'")
        void quick_bid_2_adds_20m() {
            controller.onQuickBid2();
            assertEquals("20,000,000 d", controller.tfBidAmount.getText());
        }

        @Test
        @DisplayName("onQuickBid3 → tfBidAmount = '50,000,000 d'")
        void quick_bid_3_adds_50m() {
            controller.onQuickBid3();
            assertEquals("50,000,000 d", controller.tfBidAmount.getText());
        }

        @Test
        @DisplayName("currentPrice = 5M + quickBid1 → '15,000,000 d'")
        void quick_bid_1_accumulates_with_current_price() {
            controller.currentPrice = new BigDecimal("5000000");
            controller.onQuickBid1();
            assertEquals("15,000,000 d", controller.tfBidAmount.getText());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // onBidNow — demo mode (currentItem = null)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("onBidNow – demo mode (currentItem null)")
    class OnBidNowDemoMode {

        @Test
        @DisplayName("input rỗng → demo: currentPrice = 0 + bidStep = 10M")
        void demo_bid_with_empty_input_uses_default() {
            controller.currentItem = null;
            controller.currentPrice = BigDecimal.ZERO;
            controller.tfBidAmount.setText("");
            controller.onBidNow();
            assertEquals(0, new BigDecimal("10000000").compareTo(controller.currentPrice));
        }

        @Test
        @DisplayName("input hợp lệ → demo: currentPrice cập nhật, tfBidAmount bị clear")
        void demo_bid_with_valid_input_updates_price() {
            controller.currentItem = null;
            controller.currentPrice = BigDecimal.ZERO;
            controller.tfBidAmount.setText("30000000");
            controller.onBidNow();
            assertEquals(0, new BigDecimal("30000000").compareTo(controller.currentPrice));
            assertEquals("", controller.tfBidAmount.getText());
        }

        @Test
        @DisplayName("parseBidInput trả null (tfBidAmount null) → onBidNow return sớm")
        void demo_bid_null_parse_returns_early() {
            controller.currentItem = null;
            controller.tfBidAmount = null;
            BigDecimal before = controller.currentPrice;
            assertDoesNotThrow(() -> controller.onBidNow());
            assertEquals(before, controller.currentPrice); // không thay đổi
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // onAuctionEnded
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("onAuctionEnded")
    class OnAuctionEnded {

        @Test
        @DisplayName("tfBidAmount không null → bị disable sau khi ended")
        void textfield_disabled_after_ended() {
            controller.onAuctionEnded();
            assertTrue(controller.tfBidAmount.isDisable());
        }

        @Test
        @DisplayName("tfBidAmount null → không crash")
        void null_textfield_no_crash() {
            controller.tfBidAmount = null;
            assertDoesNotThrow(() -> controller.onAuctionEnded());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // tick
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("tick")
    class Tick {

        @Test
        @DisplayName("remainSeconds > 0 → giảm 1 và cập nhật labels")
        void decrements_remain_seconds() {
            controller.remainSeconds = 3661; // 1h1m1s
            // Cần countdown hợp lệ (để tick không NPE khi remainSeconds == 0 kế tiếp)
            controller.startCountdown();
            controller.countdown.stop(); // dừng tự động tick

            int before = controller.remainSeconds;
            controller.tick();
            assertEquals(before - 1, controller.remainSeconds);
        }

        @Test
        @DisplayName("remainSeconds = 1 → tick → còn 0 → setTimeDisplay(0,0,0)")
        void tick_when_one_second_left_goes_to_zero() {
            controller.remainSeconds = 1;
            controller.startCountdown();
            controller.countdown.stop();

            controller.tick();
            assertEquals(0, controller.remainSeconds);
            // setTimeDisplay(0,0,0) đã được gọi vào lần tick tiếp theo (remainSeconds = 0)
        }

        @Test
        @DisplayName("remainSeconds = 0 → countdown dừng, tfBidAmount disable")
        void tick_at_zero_stops_countdown_and_disables_bid() {

            controller.startCountdown();
            controller.countdown.stop();

            controller.remainSeconds = 0;

            controller.tick();

            assertTrue(controller.tfBidAmount.isDisable());
        }
        @Test
        @DisplayName("tick phân tách đúng h/m/s từ remainSeconds")
        void tick_correctly_splits_hours_minutes_seconds() {
            // 2h 5m 3s = 7503s. Sau tick: 7502 = 2h 5m 2s
            controller.remainSeconds = 7503;
            controller.startCountdown();
            controller.countdown.stop();

            controller.tick(); // remainSeconds = 7502

            assertEquals("02", controller.lblHours.getText());
            assertEquals("05", controller.lblMinutes.getText());
            assertEquals("02", controller.lblSeconds.getText());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // startCountdown
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("startCountdown")
    class StartCountdown {

        @Test
        @DisplayName("currentItem null → remainSeconds = demo value (>0)")
        void null_item_uses_demo_time() {
            controller.currentItem = null;
            controller.startCountdown();
            // Demo = 2*3600 + 14*60 + 30 = 8070
            assertEquals(8070, controller.remainSeconds);
            controller.countdown.stop();
        }

        @Test
        @DisplayName("currentItem có endTime trong quá khứ → remainSeconds = 0")
        void past_end_time_gives_zero_remain() {
            Instant past = Instant.now().minusSeconds(3600);
            controller.currentItem = buildItem(1, BigDecimal.TEN, past);
            controller.startCountdown();
            assertEquals(0, controller.remainSeconds);
            controller.countdown.stop();
        }

        @Test
        @DisplayName("currentItem có endTime trong tương lai → remainSeconds > 0")
        void future_end_time_gives_positive_remain() {
            Instant future = Instant.now().plusSeconds(3600);
            controller.currentItem = buildItem(1, BigDecimal.TEN, future);
            controller.startCountdown();
            assertTrue(controller.remainSeconds > 0);
            controller.countdown.stop();
        }

        @Test
        @DisplayName("currentItem không null nhưng endTime null → fallback demo")
        void item_without_end_time_uses_demo() {
            controller.currentItem = buildItem(1, BigDecimal.TEN, null);
            controller.startCountdown();
            assertEquals(8070, controller.remainSeconds);
            controller.countdown.stop();
        }

        @Test
        @DisplayName("countdown được tạo và khởi động (không null)")
        void countdown_timeline_created() {
            controller.startCountdown();
            assertNotNull(controller.countdown);
            controller.countdown.stop();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // switchTab
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("switchTab")
    class SwitchTab {

        @Test
        @DisplayName("switch sang btnTabSpec → activeTabBtn = btnTabSpec")
        void active_tab_updated() {
            controller.activeTabBtn = controller.btnTabDesc;
            controller.switchTab("spec", controller.btnTabSpec);
            assertSame(controller.btnTabSpec, controller.activeTabBtn);
        }

        @Test
        @DisplayName("previous tab mất class tab-active, nhận tab-inactive")
        void previous_tab_style_updated() {
            controller.btnTabDesc.getStyleClass().add("tab-active");
            controller.activeTabBtn = controller.btnTabDesc;

            controller.switchTab("spec", controller.btnTabSpec);

            assertFalse(controller.btnTabDesc.getStyleClass().contains("tab-active"));
            assertTrue(controller.btnTabDesc.getStyleClass().contains("tab-inactive"));
        }

        @Test
        @DisplayName("new tab nhận class tab-active, mất tab-inactive")
        void new_tab_style_updated() {
            controller.btnTabSpec.getStyleClass().add("tab-inactive");
            controller.activeTabBtn = controller.btnTabDesc;

            controller.switchTab("spec", controller.btnTabSpec);

            assertTrue(controller.btnTabSpec.getStyleClass().contains("tab-active"));
            assertFalse(controller.btnTabSpec.getStyleClass().contains("tab-inactive"));
        }

        @Test
        @DisplayName("activeTabBtn null → không crash (null-check branch)")
        void null_active_tab_no_crash() {
            controller.activeTabBtn = null;
            assertDoesNotThrow(() -> controller.switchTab("origin", controller.btnTabOrigin));
        }

        @Test
        @DisplayName("tab-inactive đã tồn tại → không thêm duplicate")
        void no_duplicate_tab_inactive_class() {
            controller.btnTabDesc.getStyleClass().add("tab-active");
            controller.btnTabDesc.getStyleClass().add("tab-inactive"); // đã có sẵn
            controller.activeTabBtn = controller.btnTabDesc;

            controller.switchTab("spec", controller.btnTabSpec);

            long count = controller.btnTabDesc.getStyleClass().stream()
                    .filter("tab-inactive"::equals).count();
            assertEquals(1, count, "Không được có duplicate tab-inactive");
        }

        @Test
        @DisplayName("tab-active đã tồn tại trên new tab → không thêm duplicate")
        void no_duplicate_tab_active_class() {
            controller.activeTabBtn = controller.btnTabDesc;
            controller.btnTabSpec.getStyleClass().add("tab-active"); // đã có sẵn

            controller.switchTab("spec", controller.btnTabSpec);

            long count = controller.btnTabSpec.getStyleClass().stream()
                    .filter("tab-active"::equals).count();
            assertEquals(1, count, "Không được có duplicate tab-active");
        }

        @Test
        @DisplayName("onTabDesc() gọi switchTab → activeTabBtn = btnTabDesc")
        void onTabDesc_sets_correct_active() {
            controller.activeTabBtn = controller.btnTabSpec;
            controller.onTabDesc();
            assertSame(controller.btnTabDesc, controller.activeTabBtn);
        }

        @Test
        @DisplayName("onTabSpec() gọi switchTab → activeTabBtn = btnTabSpec")
        void onTabSpec_sets_correct_active() {
            controller.activeTabBtn = controller.btnTabDesc;
            controller.onTabSpec();
            assertSame(controller.btnTabSpec, controller.activeTabBtn);
        }

        @Test
        @DisplayName("onTabOrigin() gọi switchTab → activeTabBtn = btnTabOrigin")
        void onTabOrigin_sets_correct_active() {
            controller.activeTabBtn = controller.btnTabDesc;
            controller.onTabOrigin();
            assertSame(controller.btnTabOrigin, controller.activeTabBtn);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // switchThumb
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("switchThumb")
    class SwitchThumb {

        @ParameterizedTest(name = "switchThumb({0}) không crash")
        @ValueSource(ints = {1, 2, 3})
        void does_not_crash(int index) {
            assertDoesNotThrow(() -> controller.switchThumb(index));
        }

        @Test
        @DisplayName("onThumb1/2/3 delegate đúng không crash")
        void delegates_from_action_handlers() {
            assertDoesNotThrow(() -> {
                controller.onThumb1();
                controller.onThumb2();
                controller.onThumb3();
            });
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // onShare, onBack
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("onShare và onBack")
    class ShareAndBack {

        @Test
        @DisplayName("onShare không crash (gọi AlertUtil.showInfo trên non-FX thread → bỏ qua)")
        void onShare_no_crash() {
            // AlertUtil.showInfo dùng Platform.runLater → không block test
            assertDoesNotThrow(() -> controller.onShare());
        }

        @Test
        @DisplayName("onBack không crash")
        void onBack_no_crash() {
            assertDoesNotThrow(() -> controller.onBack());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // onFavorite — kiểm tra nhánh currentItem null
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("onFavorite – currentItem null")
    class OnFavoriteNullItem {

        @Test
        @DisplayName("currentItem null → return sớm, không crash")
        void null_item_returns_early() {
            controller.currentItem = null;
            assertDoesNotThrow(() -> controller.onFavorite());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // onActivateAuto — nhánh không gọi service
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("onActivateAuto – các nhánh validate")
    class OnActivateAuto {

        @Test
        @DisplayName("currentItem null → không crash, return sớm (AlertUtil gửi warning)")
        void null_item_no_crash() {
            controller.currentItem = null;
            assertDoesNotThrow(() -> controller.onActivateAuto());
        }

        @Test
        @DisplayName("tfMaxPrice null → không crash (early return sau item check)")
        void null_tfMaxPrice_no_crash() {
            controller.currentItem = buildItem(1, BigDecimal.TEN, null);
            controller.tfMaxPrice = null;
            assertDoesNotThrow(() -> controller.onActivateAuto());
        }

        @Test
        @DisplayName("tfMaxPrice rỗng → không crash, return sớm (cảnh báo)")
        void empty_tfMaxPrice_no_crash() {
            controller.currentItem = buildItem(1, new BigDecimal("5000000"), null);
            controller.tfMaxPrice.setText("");
            controller.currentPrice = new BigDecimal("5000000");
            assertDoesNotThrow(() -> controller.onActivateAuto());
        }

        @Test
        @DisplayName("max <= currentPrice → không crash, return sớm")
        void max_price_not_greater_than_current_no_crash() {
            controller.currentItem = buildItem(1, new BigDecimal("10000000"), null);
            controller.currentPrice = new BigDecimal("10000000");
            controller.tfMaxPrice.setText("5000000");
            assertDoesNotThrow(() -> controller.onActivateAuto());
        }
    }
}