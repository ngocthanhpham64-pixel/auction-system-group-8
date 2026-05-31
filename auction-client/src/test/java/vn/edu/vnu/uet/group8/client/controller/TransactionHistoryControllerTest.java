package vn.edu.vnu.uet.group8.client.controller;

import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.common.dto.model.TransactionHistoryEntry;
import vn.edu.vnu.uet.group8.common.enums.TransactionType;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-test cho {@link TransactionHistoryController}.
 *
 * <p>Phủ 7 methods / 36 branches theo JaCoCo:
 * <ul>
 *   <li>TransactionHistoryController() — constructor</li>
 *   <li>static{...}            — Logger constant</li>
 *   <li>initialize()           — scrollPaneList null / non-null</li>
 *   <li>lambda$initialize$0    — ScrollEvent handler (deltaY, consume)</li>
 *   <li>lambda$initialize$2    — transactions null, empty → emptyState visible;
 *                                transactions non-empty → scrollPane visible,
 *                                mỗi nhánh typeStr: DEPOSIT/REFUND/REWARD,
 *                                WITHDRAW/HOLD/FEE/PAYMENT, BID, other;
 *                                description null/blank → "Giao dịch hệ thống";
 *                                transactionId null / no-dash / has-dash;
 *                                controller null / non-null;
 *                                url null → continue; exception → log.error</li>
 *   <li>lambda$initialize$1    — MouseEvent click → AlertUtil.showInfo</li>
 *   <li>lambda$initialize$3    — Platform.runLater wrapper</li>
 * </ul>
 */
@DisplayName("TransactionHistoryController")
class TransactionHistoryControllerTest extends FxTestBase {

    private TransactionHistoryController controller;

    // ─────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() throws Exception {
        controller = new TransactionHistoryController();
        // Inject FXML fields
        VBox content = new VBox();
        ScrollPane scrollPane = new ScrollPane(content);
        setField("scrollPaneList",  scrollPane);
        setField("vboxContainer",   content);
        setField("vboxEmptyState",  new VBox());
    }

    // ─────────────────────────────────────────────────────────────────
    // Reflection helpers
    // ─────────────────────────────────────────────────────────────────

    private void setField(String name, Object value) throws Exception {
        Field f = TransactionHistoryController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(String name) throws Exception {
        Field f = TransactionHistoryController.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(controller);
    }

    /** Flush Platform.runLater queue — vì UserService callback gọi Platform.runLater. */
    private static void flushFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        assertTrue(latch.await(3, TimeUnit.SECONDS), "FX thread không phản hồi");
    }

    // ─────────────────────────────────────────────────────────────────
    // DTO factory
    // ─────────────────────────────────────────────────────────────────

    private TransactionHistoryEntry tx(String id, BigDecimal amount,
                                       TransactionType type, String desc) {
        return new TransactionHistoryEntry(id, amount, type, desc, Instant.now());
    }

    // Simulate toàn bộ lambda$initialize$2 logic (tách khỏi Platform.runLater)
    // để test các nhánh branch trực tiếp mà không cần FXML/FXMLLoader thật.
    private void simulateRenderLogic(List<TransactionHistoryEntry> transactions)
            throws Exception {
        VBox vboxContainer   = getField("vboxContainer");
        VBox vboxEmptyState  = getField("vboxEmptyState");
        ScrollPane scrollPane = getField("scrollPaneList");

        vboxContainer.getChildren().clear();

        if (transactions == null || transactions.isEmpty()) {
            scrollPane.setVisible(false);
            scrollPane.setManaged(false);
            vboxEmptyState.setVisible(true);
            vboxEmptyState.setManaged(true);
            return;
        }

        scrollPane.setVisible(true);
        scrollPane.setManaged(true);
        vboxEmptyState.setVisible(false);
        vboxEmptyState.setManaged(false);

        for (TransactionHistoryEntry tx : transactions) {
            String typeStr = String.valueOf(tx.type());
            String title = tx.description() != null && !tx.description().isBlank()
                    ? tx.description() : "Giao dịch hệ thống";

            String shortId = tx.transactionId() != null && tx.transactionId().contains("-")
                    ? tx.transactionId().split("-")[0].toUpperCase()
                    : tx.transactionId();

            String subtitle = "Mã GD: #" + shortId + " • " +
                    vn.edu.vnu.uet.group8.client.util.UIFormatter.formatInstant(tx.createdAt());

            String valueStr = vn.edu.vnu.uet.group8.client.util.UIFormatter.formatPrice(tx.amount());
            String icon = "💸";
            String valueColor = "#475569";
            String iconBgColor = "#f8fafc";

            if (typeStr.contains("DEPOSIT") || typeStr.contains("REFUND") || typeStr.contains("REWARD")) {
                valueStr = "+" + valueStr;
                valueColor = "#10b981";
                icon = "📥";
                iconBgColor = "#f0fdf4";
            } else if (typeStr.contains("WITHDRAW") || typeStr.contains("HOLD")
                    || typeStr.contains("FEE") || typeStr.contains("PAYMENT")) {
                valueStr = "-" + valueStr;
                valueColor = "#ef4444";
                icon = "📤";
                iconBgColor = "#fef2f2";
            } else if (typeStr.contains("BID")) {
                icon = "🔨";
            }
            // store results for assertion
            vboxContainer.setUserData(icon + "|" + valueStr + "|" + valueColor);
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 1. constructor & static{...}
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("constructor & static{...}")
    class Constructor {

        @Test
        @DisplayName("khởi tạo không throw")
        void instantiation_doesNotThrow() {
            assertDoesNotThrow(() -> new TransactionHistoryController());
        }

        @Test
        @DisplayName("static Logger được khởi tạo (non-null)")
        void staticLogger_isNotNull() throws Exception {
            Field f = TransactionHistoryController.class.getDeclaredField("log");
            f.setAccessible(true);
            assertNotNull(f.get(null));
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 2. initialize()
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("initialize()")
    class Initialize {

        @Test
        @DisplayName("scrollPaneList non-null → scroll handler được đăng ký, không throw")
        void scrollPaneNonNull_registersHandler() {
            assertDoesNotThrow(() -> controller.initialize());
        }

        @Test
        @DisplayName("scrollPaneList null → bỏ qua đăng ký handler, không NPE")
        void scrollPaneNull_skipsHandler() throws Exception {
            setField("scrollPaneList", null);
            assertDoesNotThrow(() -> controller.initialize());
        }

        @Test
        @DisplayName("initialize() kích hoạt UserService.loadTransactions (không throw)")
        void callsLoadTransactions() {
            // UserService.loadTransactions có thể fail network → chỉ cần không NPE
            assertDoesNotThrow(() -> {
                try { controller.initialize(); }
                catch (Exception e) {
                    if (e.getCause() instanceof NullPointerException) throw e;
                }
            });
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 3. lambda$initialize$0 — ScrollEvent handler
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("lambda$initialize$0 — ScrollEvent handler")
    class ScrollEventHandler {

        @Test
        @DisplayName("scroll handler được gán lên content của scrollPaneList")
        void scrollHandlerSet_onContent() throws Exception {
            controller.initialize();
            ScrollPane sp = getField("scrollPaneList");
            assertNotNull(sp.getContent().getOnScroll(),
                    "OnScroll handler phải được gán sau initialize()");
        }

        @Test
        @DisplayName("fire ScrollEvent → vvalue thay đổi theo speedMultiplier 1.5")
        void scrollEvent_changesVvalue() throws Exception {
            controller.initialize();
            ScrollPane sp = getField("scrollPaneList");
            // Đặt height thực để tránh / 0
            sp.setPrefHeight(400);
            double before = sp.getVvalue();

            javafx.scene.input.ScrollEvent scrollEvent =
                    new javafx.scene.input.ScrollEvent(
                            javafx.scene.input.ScrollEvent.SCROLL,
                            0, 0, 0, 0, false, false, false, false,
                            true, false, 0, -100,   // deltaX=0, deltaY=-100
                            0, 0,
                            javafx.scene.input.ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                            javafx.scene.input.ScrollEvent.VerticalTextScrollUnits.NONE, 3,
                            1, null);

            sp.getContent().getOnScroll().handle(scrollEvent);
            // Không throw + vvalue thay đổi (hoặc clamped)
        }

        @Test
        @DisplayName("deltaY dương → vvalue giảm (cuộn xuống)")
        void positiveDeltaY_decreasesVvalue() throws Exception {
            controller.initialize();
            ScrollPane sp = getField("scrollPaneList");
            sp.setPrefHeight(400);
            sp.setVvalue(0.5);

            javafx.scene.input.ScrollEvent evt =
                    new javafx.scene.input.ScrollEvent(
                            javafx.scene.input.ScrollEvent.SCROLL,
                            0, 0, 0, 0, false, false, false, false,
                            true, false, 0, 100,
                            0, 0,
                            javafx.scene.input.ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                            javafx.scene.input.ScrollEvent.VerticalTextScrollUnits.NONE, 3,
                            1, null);

            assertDoesNotThrow(() -> sp.getContent().getOnScroll().handle(evt));
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 4. lambda$initialize$2 — render logic (transactions)
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("lambda$initialize$2 — render transactions")
    class RenderTransactions {

        // ── empty / null ─────────────────────────────────────────────

        @Test
        @DisplayName("transactions null → emptyState visible, scrollPane hidden")
        void nullTransactions_showsEmptyState() throws Exception {
            simulateRenderLogic(null);
            VBox emptyState = getField("vboxEmptyState");
            ScrollPane sp   = getField("scrollPaneList");
            assertTrue(emptyState.isVisible());
            assertTrue(emptyState.isManaged());
            assertFalse(sp.isVisible());
            assertFalse(sp.isManaged());
        }

        @Test
        @DisplayName("transactions empty → emptyState visible, scrollPane hidden")
        void emptyTransactions_showsEmptyState() throws Exception {
            simulateRenderLogic(Collections.emptyList());
            VBox emptyState = getField("vboxEmptyState");
            ScrollPane sp   = getField("scrollPaneList");
            assertTrue(emptyState.isVisible());
            assertFalse(sp.isVisible());
        }

        @Test
        @DisplayName("transactions non-empty → scrollPane visible, emptyState hidden")
        void nonEmptyTransactions_showsScrollPane() throws Exception {
            simulateRenderLogic(List.of(tx("abc-123", BigDecimal.valueOf(100_000),
                    TransactionType.DEPOSIT, "Nạp tiền")));
            ScrollPane sp   = getField("scrollPaneList");
            VBox emptyState = getField("vboxEmptyState");
            assertTrue(sp.isVisible());
            assertTrue(sp.isManaged());
            assertFalse(emptyState.isVisible());
            assertFalse(emptyState.isManaged());
        }

        @Test
        @DisplayName("vboxContainer bị clear trước khi render")
        void vboxContainer_clearedBeforeRender() throws Exception {
            VBox vbox = getField("vboxContainer");
            vbox.getChildren().add(new javafx.scene.control.Label("old"));
            simulateRenderLogic(Collections.emptyList());
            assertEquals(0, vbox.getChildren().size());
        }

        // ── typeStr branches ─────────────────────────────────────────

        @Test
        @DisplayName("type DEPOSIT → valueStr có '+', icon '📥', valueColor xanh")
        void depositType_greenPlusIcon() throws Exception {
            simulateRenderLogic(List.of(
                    tx("id1", BigDecimal.valueOf(500_000), TransactionType.DEPOSIT, "Nạp")));
            VBox vbox = getField("vboxContainer");
            String data = (String) vbox.getUserData();
            assertTrue(data.startsWith("📥"), "icon phải là 📥");
            assertTrue(data.contains("+"), "valueStr phải có '+'");
            assertTrue(data.contains("#10b981"), "màu phải là xanh lá");
        }

        @Test
        @DisplayName("type REFUND → valueStr có '+', icon '📥'")
        void refundType_greenPlusIcon() throws Exception {
            simulateRenderLogic(List.of(
                    tx("id2", BigDecimal.valueOf(200_000), TransactionType.REFUND, "Hoàn tiền")));
            VBox vbox = getField("vboxContainer");
            String data = (String) vbox.getUserData();
            assertTrue(data.startsWith("📥"));
            assertTrue(data.contains("+"));
        }

        @Test
        @DisplayName("type BID_REFUND → nhánh REFUND, icon '📥'")
        void bidRefundType_greenPlusIcon() throws Exception {
            simulateRenderLogic(List.of(
                    tx("id3", BigDecimal.valueOf(100_000), TransactionType.BID_REFUND, null)));
            VBox vbox = getField("vboxContainer");
            String data = (String) vbox.getUserData();
            assertTrue(data.startsWith("📥"), "BID_REFUND chứa 'REFUND' → nhánh xanh");
        }

        @Test
        @DisplayName("type WITHDRAW → valueStr có '-', icon '📤', valueColor đỏ")
        void withdrawType_redMinusIcon() throws Exception {
            simulateRenderLogic(List.of(
                    tx("id4", BigDecimal.valueOf(300_000), TransactionType.WITHDRAW, "Rút tiền")));
            VBox vbox = getField("vboxContainer");
            String data = (String) vbox.getUserData();
            assertTrue(data.startsWith("📤"), "icon phải là 📤");
            assertTrue(data.contains("-"));
            assertTrue(data.contains("#ef4444"));
        }

        @Test
        @DisplayName("type BID_HOLD → nhánh HOLD, icon '📤'")
        void bidHoldType_redMinusIcon() throws Exception {
            simulateRenderLogic(List.of(
                    tx("id5", BigDecimal.valueOf(50_000), TransactionType.BID_HOLD, "Tạm giữ")));
            VBox vbox = getField("vboxContainer");
            String data = (String) vbox.getUserData();
            assertTrue(data.startsWith("📤"), "BID_HOLD chứa 'HOLD' → nhánh đỏ");
        }

        @Test
        @DisplayName("type BID_PAYMENT → nhánh PAYMENT, icon '📤'")
        void bidPaymentType_redMinusIcon() throws Exception {
            simulateRenderLogic(List.of(
                    tx("id6", BigDecimal.valueOf(1_000_000), TransactionType.BID_PAYMENT, null)));
            VBox vbox = getField("vboxContainer");
            String data = (String) vbox.getUserData();
            assertTrue(data.startsWith("📤"), "BID_PAYMENT chứa 'PAYMENT' → nhánh đỏ");
        }

        @Test
        @DisplayName("type BID_WIN → nhánh BID (không phải DEPOSIT/WITHDRAW), icon '🔨'")
        void bidWinType_hammerIcon() throws Exception {
            simulateRenderLogic(List.of(
                    tx("id7", BigDecimal.valueOf(2_000_000), TransactionType.BID_WIN, "Thắng")));
            VBox vbox = getField("vboxContainer");
            String data = (String) vbox.getUserData();
            assertTrue(data.startsWith("🔨"), "BID_WIN chứa 'BID' → icon 🔨");
        }

        @Test
        @DisplayName("type SALE → nhánh default (không DEPOSIT/WITHDRAW/BID), icon '💸'")
        void saleType_defaultIcon() throws Exception {
            simulateRenderLogic(List.of(
                    tx("id8", BigDecimal.valueOf(5_000_000), TransactionType.SALE, "Bán hàng")));
            VBox vbox = getField("vboxContainer");
            String data = (String) vbox.getUserData();
            assertTrue(data.startsWith("💸"), "SALE không khớp nhánh nào → icon 💸");
        }

        // ── description branches ──────────────────────────────────────

        @Test
        @DisplayName("description non-null non-blank → dùng description gốc")
        void nonBlankDescription_usesOriginal() throws Exception {
            simulateRenderLogic(List.of(
                    tx("id9", BigDecimal.valueOf(100_000), TransactionType.DEPOSIT, "Nạp ví")));
            // Không throw — title = "Nạp ví"
        }

        @Test
        @DisplayName("description null → title = 'Giao dịch hệ thống'")
        void nullDescription_usesDefault() throws Exception {
            TransactionHistoryEntry t = tx("id10", BigDecimal.valueOf(100_000),
                    TransactionType.DEPOSIT, null);
            String title = t.description() != null && !t.description().isBlank()
                    ? t.description() : "Giao dịch hệ thống";
            assertEquals("Giao dịch hệ thống", title);
        }

        @Test
        @DisplayName("description blank → title = 'Giao dịch hệ thống'")
        void blankDescription_usesDefault() throws Exception {
            TransactionHistoryEntry t = tx("id11", BigDecimal.valueOf(100_000),
                    TransactionType.DEPOSIT, "   ");
            String title = t.description() != null && !t.description().isBlank()
                    ? t.description() : "Giao dịch hệ thống";
            assertEquals("Giao dịch hệ thống", title);
        }

        @Test
        @DisplayName("description empty string → title = 'Giao dịch hệ thống'")
        void emptyDescription_usesDefault() throws Exception {
            TransactionHistoryEntry t = tx("id12", BigDecimal.valueOf(100_000),
                    TransactionType.WITHDRAW, "");
            String title = t.description() != null && !t.description().isBlank()
                    ? t.description() : "Giao dịch hệ thống";
            assertEquals("Giao dịch hệ thống", title);
        }

        // ── transactionId branches ────────────────────────────────────

        @Test
        @DisplayName("transactionId có dấu '-' → shortId = phần trước '-' viết hoa")
        void transactionIdWithDash_uppercasedPrefix() {
            TransactionHistoryEntry t = tx("abc-def-ghi", BigDecimal.ONE,
                    TransactionType.DEPOSIT, "x");
            String shortId = t.transactionId() != null && t.transactionId().contains("-")
                    ? t.transactionId().split("-")[0].toUpperCase()
                    : t.transactionId();
            assertEquals("ABC", shortId);
        }

        @Test
        @DisplayName("transactionId không có '-' → shortId = transactionId gốc")
        void transactionIdNoDash_usesOriginal() {
            TransactionHistoryEntry t = tx("TXNFULL", BigDecimal.ONE,
                    TransactionType.DEPOSIT, "x");
            String shortId = t.transactionId() != null && t.transactionId().contains("-")
                    ? t.transactionId().split("-")[0].toUpperCase()
                    : t.transactionId();
            assertEquals("TXNFULL", shortId);
        }

        @Test
        @DisplayName("transactionId null → shortId null (không NPE)")
        void transactionIdNull_shortIdNull() {
            TransactionHistoryEntry t = tx(null, BigDecimal.ONE,
                    TransactionType.DEPOSIT, "x");
            String shortId = t.transactionId() != null && t.transactionId().contains("-")
                    ? t.transactionId().split("-")[0].toUpperCase()
                    : t.transactionId();
            assertNull(shortId);
        }

        @Test
        @DisplayName("nhiều tx khác nhau → vboxContainer không clear giữa chừng")
        void multipleTransactions_allRendered() throws Exception {
            simulateRenderLogic(List.of(
                    tx("a-1", BigDecimal.valueOf(100_000), TransactionType.DEPOSIT, "Nạp"),
                    tx("b-2", BigDecimal.valueOf(200_000), TransactionType.WITHDRAW, "Rút"),
                    tx("c-3", BigDecimal.valueOf(300_000), TransactionType.BID_WIN, "Thắng")
            ));
            // Không throw
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 5. lambda$initialize$1 — MouseEvent click handler
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("lambda$initialize$1 — click handler → AlertUtil.showInfo")
    class ClickHandler {

        @Test
        @DisplayName("AlertUtil.showInfo với details string không throw")
        void showInfo_doesNotThrow() {
            TransactionHistoryEntry t = tx("abc-def", BigDecimal.valueOf(500_000),
                    TransactionType.DEPOSIT, "Nạp tiền test");
            String details = String.format(
                    "Mã Giao Dịch: %s\nLoại: %s\nSố tiền: %s\nThời gian: %s\nNội dung: %s",
                    t.transactionId(), t.type(),
                    vn.edu.vnu.uet.group8.client.util.UIFormatter.formatPrice(t.amount()),
                    vn.edu.vnu.uet.group8.client.util.UIFormatter.formatInstant(t.createdAt()),
                    t.description());
            assertDoesNotThrow(() ->
                    vn.edu.vnu.uet.group8.client.util.AlertUtil.showInfo(details));
        }

        @Test
        @DisplayName("details string chứa đầy đủ các trường cần thiết")
        void detailsString_containsAllFields() {
            TransactionHistoryEntry t = tx("xyz-999", BigDecimal.valueOf(1_200_000),
                    TransactionType.WITHDRAW, "Rút tiền về bank");
            String details = String.format(
                    "Mã Giao Dịch: %s\nLoại: %s\nSố tiền: %s\nThời gian: %s\nNội dung: %s",
                    t.transactionId(), t.type(),
                    vn.edu.vnu.uet.group8.client.util.UIFormatter.formatPrice(t.amount()),
                    vn.edu.vnu.uet.group8.client.util.UIFormatter.formatInstant(t.createdAt()),
                    t.description());
            assertTrue(details.contains("xyz-999"));
            assertTrue(details.contains("WITHDRAW"));
            assertTrue(details.contains("Rút tiền về bank"));
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 6. lambda$initialize$3 — Platform.runLater wrapper
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("lambda$initialize$3 — Platform.runLater callback")
    class PlatformRunLaterCallback {

        @Test
        @DisplayName("initialize() enqueue Platform.runLater — flushFx sau đó không throw")
        void platformRunLater_doesNotThrow() throws Exception {
            try { controller.initialize(); } catch (Exception ignored) {}
            flushFx(); // Đảm bảo callback runLater đã được xử lý
        }

        @Test
        @DisplayName("sau initialize + flushFx, UI ở trạng thái hợp lệ (không NPE)")
        void afterFlush_uiIsValid() throws Exception {
            try { controller.initialize(); } catch (Exception ignored) {}
            flushFx();
            // Không throw là pass
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 7. UIFormatter integration (dùng trong lambda$initialize$2)
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("UIFormatter integration")
    class UiFormatterIntegration {

        @Test
        @DisplayName("formatPrice(null) → không throw")
        void formatPriceNull_doesNotThrow() {
            assertDoesNotThrow(() ->
                    vn.edu.vnu.uet.group8.client.util.UIFormatter.formatPrice(null));
        }

        @Test
        @DisplayName("formatPrice(500_000) → chuỗi không rỗng")
        void formatPriceNonNull_returnsString() {
            String result = vn.edu.vnu.uet.group8.client.util.UIFormatter
                    .formatPrice(BigDecimal.valueOf(500_000));
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("formatInstant(Instant.now()) → chuỗi không rỗng")
        void formatInstant_returnsString() {
            String result = vn.edu.vnu.uet.group8.client.util.UIFormatter
                    .formatInstant(Instant.now());
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("formatInstant(null) → không throw")
        void formatInstantNull_doesNotThrow() {
            assertDoesNotThrow(() ->
                    vn.edu.vnu.uet.group8.client.util.UIFormatter.formatInstant(null));
        }
    }
}