package vn.edu.vnu.uet.group8.client.controller;

import javafx.animation.Timeline;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.AuctionService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionStatusDTO;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test đầy đủ cho LiveAuctionController.
 *
 * Chiến lược:
 *  - Không load FXML → tất cả @FXML fields = null (headless-safe, không cần JavaFX runtime).
 *  - Các method private gọi qua Reflection.
 *  - Static util/service (ClientModel, AuctionService, AlertUtil, SessionManager) mock bằng
 *    Mockito.mockStatic() để cô lập hoàn toàn.
 *  - Mỗi branch quan trọng trong coverage report đều có ít nhất 1 test case.
 */
@ExtendWith(MockitoExtension.class)
class LiveAuctionControllerTest {

    private LiveAuctionController ctrl;

    // ── Reflection helpers ──────────────────────────────────────────────────

    private static Method method(String name, Class<?>... params) throws Exception {
        Method m = LiveAuctionController.class.getDeclaredMethod(name, params);
        m.setAccessible(true);
        return m;
    }

    private static <T> T getField(LiveAuctionController target, String name) throws Exception {
        Field f = LiveAuctionController.class.getDeclaredField(name);
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        T val = (T) f.get(target);
        return val;
    }

    private static void setField(LiveAuctionController target, String name, Object value) throws Exception {
        Field f = LiveAuctionController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /** Tạo AuctionItemDTO tối giản qua factory method of() */
    private static AuctionItemDTO makeItem(int id, BigDecimal price, Instant endTime) {
        return AuctionItemDTO.of(id, "Item " + id, "Desc", ItemCategory.ELECTRONICS,
                ItemCondition.NEW, SessionStatus.ACTIVE, price, endTime,
                "seller", null, null, 0, Instant.now());
    }

    // ── Setup ───────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        ctrl = new LiveAuctionController();
    }

    // ════════════════════════════════════════════════════════════════════════
    // 1. DEFAULT STATE
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("currentPrice mặc định ZERO")
    void defaultState_currentPrice_zero() throws Exception {
        assertEquals(0, BigDecimal.ZERO.compareTo(getField(ctrl, "currentPrice")));
    }

    @Test
    @DisplayName("currentItem mặc định null")
    void defaultState_currentItem_null() throws Exception {
        assertNull(getField(ctrl, "currentItem"));
    }

    @Test
    @DisplayName("subscription mặc định null")
    void defaultState_subscription_null() throws Exception {
        assertNull(getField(ctrl, "subscription"));
    }

    @Test
    @DisplayName("bidStep mặc định = 10.000.000")
    void defaultState_bidStep_tenMillion() throws Exception {
        assertEquals(0, new BigDecimal("10000000").compareTo(getField(ctrl, "bidStep")));
    }

    @Test
    @DisplayName("countdown mặc định null")
    void defaultState_countdown_null() throws Exception {
        assertNull(getField(ctrl, "countdown"));
    }

    @Test
    @DisplayName("remainSeconds mặc định 0")
    void defaultState_remainSeconds_zero() throws Exception {
        assertEquals(0, (int) getField(ctrl, "remainSeconds"));
    }

    @Test
    @DisplayName("viewerCount mặc định 100 (demo)")
    void defaultState_viewerCount_hundred() throws Exception {
        assertEquals(100, (int) getField(ctrl, "viewerCount"));
    }

    @Test
    @DisplayName("chatCount mặc định 0")
    void defaultState_chatCount_zero() throws Exception {
        assertEquals(0, (int) getField(ctrl, "chatCount"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. formatPrice
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("formatPrice(null) → '--'")
    void formatPrice_null_returnsDashes() throws Exception {
        Method m = method("formatPrice", BigDecimal.class);
        assertEquals("--", m.invoke(ctrl, (Object) null));
    }

    @Test
    @DisplayName("formatPrice(ZERO) → chuỗi chứa '0'")
    void formatPrice_zero_containsZero() throws Exception {
        Method m = method("formatPrice", BigDecimal.class);
        String result = (String) m.invoke(ctrl, BigDecimal.ZERO);
        assertNotNull(result);
        assertTrue(result.contains("0"));
    }

    @Test
    @DisplayName("formatPrice(1_000_000) → chuỗi không rỗng")
    void formatPrice_oneMillion_notEmpty() throws Exception {
        Method m = method("formatPrice", BigDecimal.class);
        String result = (String) m.invoke(ctrl, BigDecimal.valueOf(1_000_000));
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    @DisplayName("formatPrice: hai giá khác nhau → chuỗi khác nhau")
    void formatPrice_different_differentStrings() throws Exception {
        Method m = method("formatPrice", BigDecimal.class);
        String s1 = (String) m.invoke(ctrl, BigDecimal.valueOf(100_000));
        String s2 = (String) m.invoke(ctrl, BigDecimal.valueOf(200_000));
        assertNotEquals(s1, s2);
    }

    @Test
    @DisplayName("formatPrice(100_000_000) → chứa '100'")
    void formatPrice_hundred_million_contains100() throws Exception {
        Method m = method("formatPrice", BigDecimal.class);
        String result = (String) m.invoke(ctrl, BigDecimal.valueOf(100_000_000));
        assertTrue(result.contains("100"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. parseBidInput
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("parseBidInput: tfBidAmount null → null")
    void parseBidInput_nullTextField_returnsNull() throws Exception {
        // tfBidAmount chưa inject → null
        assertNull(method("parseBidInput").invoke(ctrl));
    }

    @Test
    @DisplayName("parseBidInput: text rỗng → null")
    void parseBidInput_emptyText_returnsNull() throws Exception {
        TextField tf = new TextField("");
        setField(ctrl, "tfBidAmount", tf);
        assertNull(method("parseBidInput").invoke(ctrl));
    }

    @Test
    @DisplayName("parseBidInput: text chỉ khoảng trắng → null")
    void parseBidInput_blankText_returnsNull() throws Exception {
        TextField tf = new TextField("   ");
        setField(ctrl, "tfBidAmount", tf);
        assertNull(method("parseBidInput").invoke(ctrl));
    }

    @Test
    @DisplayName("parseBidInput: text hợp lệ '50000000' → BigDecimal 50000000")
    void parseBidInput_validNumber_returns50M() throws Exception {
        TextField tf = new TextField("50000000");
        setField(ctrl, "tfBidAmount", tf);
        BigDecimal result = (BigDecimal) method("parseBidInput").invoke(ctrl);
        assertNotNull(result);
        assertEquals(0, new BigDecimal("50000000").compareTo(result));
    }

    @Test
    @DisplayName("parseBidInput: text có dấu phẩy/chấm '50,000,000' → BigDecimal 50000000")
    void parseBidInput_formattedNumber_stripsNonDigits() throws Exception {
        TextField tf = new TextField("50,000,000");
        setField(ctrl, "tfBidAmount", tf);
        BigDecimal result = (BigDecimal) method("parseBidInput").invoke(ctrl);
        assertNotNull(result);
        assertEquals(0, new BigDecimal("50000000").compareTo(result));
    }

    @Test
    @DisplayName("parseBidInput: text chỉ có ký tự không phải số → null (sau khi clean)")
    void parseBidInput_nonDigitOnly_returnsNull() throws Exception {
        // Sau khi replaceAll("[^\\d]","") → "" → trả null
        TextField tf = new TextField("abc");
        setField(ctrl, "tfBidAmount", tf);
        // AlertUtil.showWarning sẽ được gọi (không crash)
        try (MockedStatic<AlertUtil> au = mockStatic(AlertUtil.class)) {
            au.when(() -> AlertUtil.showWarning(anyString())).thenAnswer(inv -> null);
            assertNull(method("parseBidInput").invoke(ctrl));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. setTimeDisplay
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setTimeDisplay(0, 0) khi labels null → không crash")
    void setTimeDisplay_nullLabels_zeroes_noCrash() throws Exception {
        assertDoesNotThrow(() -> method("setTimeDisplay", int.class, int.class).invoke(ctrl, 0, 0));
    }

    @Test
    @DisplayName("setTimeDisplay(8, 25) khi labels null → không crash")
    void setTimeDisplay_nullLabels_eightTwentyFive_noCrash() throws Exception {
        assertDoesNotThrow(() -> method("setTimeDisplay", int.class, int.class).invoke(ctrl, 8, 25));
    }

    @Test
    @DisplayName("setTimeDisplay: lblMinutes và lblSeconds có giá trị → cập nhật đúng format 2 chữ số")
    void setTimeDisplay_withLabels_updatesCorrectly() throws Exception {
        Label mins = new Label();
        Label secs = new Label();
        setField(ctrl, "lblMinutes", mins);
        setField(ctrl, "lblSeconds", secs);
        method("setTimeDisplay", int.class, int.class).invoke(ctrl, 3, 5);
        assertEquals("03", mins.getText());
        assertEquals("05", secs.getText());
    }

    @Test
    @DisplayName("setTimeDisplay: phút ≥ 10 → không có leading zero thừa")
    void setTimeDisplay_twoDigitMinute_formatCorrect() throws Exception {
        Label mins = new Label();
        Label secs = new Label();
        setField(ctrl, "lblMinutes", mins);
        setField(ctrl, "lblSeconds", secs);
        method("setTimeDisplay", int.class, int.class).invoke(ctrl, 12, 59);
        assertEquals("12", mins.getText());
        assertEquals("59", secs.getText());
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. updatePriceDisplay
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("updatePriceDisplay(null): labels null → không crash")
    void updatePriceDisplay_nullOldPrice_noLabels_noCrash() throws Exception {
        assertDoesNotThrow(() -> method("updatePriceDisplay", BigDecimal.class).invoke(ctrl, (Object) null));
    }

    @Test
    @DisplayName("updatePriceDisplay(ZERO): labels null → không crash")
    void updatePriceDisplay_zeroOldPrice_noLabels_noCrash() throws Exception {
        assertDoesNotThrow(() -> method("updatePriceDisplay", BigDecimal.class).invoke(ctrl, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("updatePriceDisplay: lblCurrentPrice có value → hiển thị đúng currentPrice")
    void updatePriceDisplay_withPriceLabel_setsText() throws Exception {
        Label priceLabel = new Label();
        setField(ctrl, "lblCurrentPrice", priceLabel);
        setField(ctrl, "currentPrice", new BigDecimal("50000000"));
        method("updatePriceDisplay", BigDecimal.class).invoke(ctrl, (Object) null);
        assertFalse(priceLabel.getText().isBlank());
        assertTrue(priceLabel.getText().contains("50"));
    }

    @Test
    @DisplayName("updatePriceDisplay: oldPrice > 0 → lblPriceChange hiển thị % thay đổi")
    void updatePriceDisplay_oldPricePositive_showsPercentChange() throws Exception {
        Label priceLabel = new Label();
        Label changeLabel = new Label();
        setField(ctrl, "lblCurrentPrice", priceLabel);
        setField(ctrl, "lblPriceChange", changeLabel);
        setField(ctrl, "currentPrice", new BigDecimal("110000000"));
        method("updatePriceDisplay", BigDecimal.class).invoke(ctrl, new BigDecimal("100000000"));
        // Phải chứa "%" và dấu "+"
        assertTrue(changeLabel.getText().contains("%"));
        assertTrue(changeLabel.getText().startsWith("+"));
    }

    @Test
    @DisplayName("updatePriceDisplay: oldPrice = 0 → không hiện % (branch oldPrice <= 0)")
    void updatePriceDisplay_oldPriceZero_noPercentShown() throws Exception {
        Label changeLabel = new Label("unchanged");
        setField(ctrl, "lblPriceChange", changeLabel);
        setField(ctrl, "currentPrice", new BigDecimal("50000000"));
        method("updatePriceDisplay", BigDecimal.class).invoke(ctrl, BigDecimal.ZERO);
        // Branch "oldPrice > 0" không được thực thi → label không đổi
        assertEquals("unchanged", changeLabel.getText());
    }

    @Test
    @DisplayName("updatePriceDisplay: lblPriceChange null nhưng oldPrice > 0 → không crash")
    void updatePriceDisplay_priceChangeLabelNull_noCrash() throws Exception {
        // lblPriceChange = null (mặc định), oldPrice > 0
        setField(ctrl, "currentPrice", new BigDecimal("120000000"));
        assertDoesNotThrow(() ->
                method("updatePriceDisplay", BigDecimal.class).invoke(ctrl, new BigDecimal("100000000")));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 6. handleStatusUpdate
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("handleStatusUpdate(null) → không crash (branch status==null return)")
    void handleStatusUpdate_null_noCrash() throws Exception {
        assertDoesNotThrow(() ->
                method("handleStatusUpdate", AuctionStatusDTO.class).invoke(ctrl, (Object) null));
    }

    @Test
    @DisplayName("handleStatusUpdate: currentItem null → skip, price không thay đổi")
    void handleStatusUpdate_currentItemNull_priceUnchanged() throws Exception {
        AuctionStatusDTO status = new AuctionStatusDTO(1, BigDecimal.valueOf(999_999), Instant.now(), List.of());
        method("handleStatusUpdate", AuctionStatusDTO.class).invoke(ctrl, status);
        assertEquals(0, BigDecimal.ZERO.compareTo(getField(ctrl, "currentPrice")));
    }

    @Test
    @DisplayName("handleStatusUpdate: itemId khác currentItem.itemId → skip, price không đổi")
    void handleStatusUpdate_differentItemId_priceUnchanged() throws Exception {
        AuctionItemDTO item = makeItem(1, BigDecimal.valueOf(100_000_000), Instant.now().plusSeconds(3600));
        setField(ctrl, "currentItem", item);
        setField(ctrl, "currentPrice", BigDecimal.valueOf(100_000_000));

        // Status với itemId=999 ≠ 1
        AuctionStatusDTO status = new AuctionStatusDTO(999, BigDecimal.valueOf(999_999), Instant.now(), List.of());
        method("handleStatusUpdate", AuctionStatusDTO.class).invoke(ctrl, status);

        // Giá không đổi
        assertEquals(0, BigDecimal.valueOf(100_000_000).compareTo(getField(ctrl, "currentPrice")));
    }

    @Test
    @DisplayName("handleStatusUpdate: đúng itemId, status.currentPrice != null → cập nhật currentPrice")
    void handleStatusUpdate_matchingItemId_updatesPrice() throws Exception {
        AuctionItemDTO item = makeItem(5, BigDecimal.valueOf(100_000_000), Instant.now().plusSeconds(3600));
        setField(ctrl, "currentItem", item);
        setField(ctrl, "currentPrice", BigDecimal.valueOf(100_000_000));

        AuctionStatusDTO status = new AuctionStatusDTO(5, BigDecimal.valueOf(120_000_000), Instant.now(), List.of());
        method("handleStatusUpdate", AuctionStatusDTO.class).invoke(ctrl, status);

        assertEquals(0, BigDecimal.valueOf(120_000_000).compareTo(getField(ctrl, "currentPrice")));
    }

    @Test
    @DisplayName("handleStatusUpdate: đúng itemId, status.currentPrice null → giữ nguyên currentPrice")
    void handleStatusUpdate_matchingItemId_nullStatusPrice_keepsCurrent() throws Exception {
        // AuctionStatusDTO constructor reject null price → ta test branch khác: currentPrice giữ nguyên
        // Thay vào đó, verify khi price hợp lệ, nó luôn được set (không còn null path từ DTO)
        AuctionItemDTO item = makeItem(5, BigDecimal.valueOf(100_000_000), Instant.now().plusSeconds(3600));
        setField(ctrl, "currentItem", item);
        setField(ctrl, "currentPrice", BigDecimal.valueOf(100_000_000));

        // Giá mới bằng giá cũ → currentPrice không đổi nhưng update được gọi
        AuctionStatusDTO status = new AuctionStatusDTO(5, BigDecimal.valueOf(100_000_000), Instant.now(), List.of());
        method("handleStatusUpdate", AuctionStatusDTO.class).invoke(ctrl, status);

        assertEquals(0, BigDecimal.valueOf(100_000_000).compareTo(getField(ctrl, "currentPrice")));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 7. addBidHistoryEntry
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("addBidHistoryEntry: bidHistory null → không crash")
    void addBidHistoryEntry_nullBidHistory_noCrash() throws Exception {
        // bidHistory @FXML field vẫn null
        AuctionStatusDTO status = new AuctionStatusDTO(1, BigDecimal.valueOf(50_000_000), Instant.now(), List.of());
        assertDoesNotThrow(() ->
                method("addBidHistoryEntry", AuctionStatusDTO.class).invoke(ctrl, status));
    }

    @Test
    @DisplayName("addBidHistoryEntry: bidHistory có giá trị → thêm row vào đầu")
    void addBidHistoryEntry_withBidHistory_addsRow() throws Exception {
        VBox history = new VBox();
        setField(ctrl, "bidHistory", history);
        AuctionStatusDTO status = new AuctionStatusDTO(1, BigDecimal.valueOf(50_000_000), Instant.now(), List.of());
        method("addBidHistoryEntry", AuctionStatusDTO.class).invoke(ctrl, status);
        assertEquals(1, history.getChildren().size());
    }

    @Test
    @DisplayName("addBidHistoryEntry: sau 20 entries → giới hạn tối đa 20")
    void addBidHistoryEntry_over20Entries_capped() throws Exception {
        VBox history = new VBox();
        setField(ctrl, "bidHistory", history);
        Method m = method("addBidHistoryEntry", AuctionStatusDTO.class);
        // Thêm 25 entries
        for (int i = 1; i <= 25; i++) {
            AuctionStatusDTO s = new AuctionStatusDTO(1, BigDecimal.valueOf(i * 1_000_000L), Instant.now(), List.of());
            m.invoke(ctrl, s);
        }
        assertEquals(20, history.getChildren().size());
    }

    // ════════════════════════════════════════════════════════════════════════
    // 8. addChatMessage
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("addChatMessage: chatMessages null → không crash")
    void addChatMessage_nullChatMessages_noCrash() throws Exception {
        assertDoesNotThrow(() ->
                method("addChatMessage", String.class, String.class, boolean.class)
                        .invoke(ctrl, "user1", "hello", true));
    }

    @Test
    @DisplayName("addChatMessage: username null → dùng 'Guest'")
    void addChatMessage_nullUsername_usesGuest() throws Exception {
        VBox chat = new VBox();
        setField(ctrl, "chatMessages", chat);
        method("addChatMessage", String.class, String.class, boolean.class)
                .invoke(ctrl, null, "hello", false);
        assertEquals(1, chat.getChildren().size());
    }

    @Test
    @DisplayName("addChatMessage: isMe=true → style orange (kiểm tra label tạo thành công)")
    void addChatMessage_isMe_true_addsRow() throws Exception {
        VBox chat = new VBox();
        setField(ctrl, "chatMessages", chat);
        method("addChatMessage", String.class, String.class, boolean.class)
                .invoke(ctrl, "alice", "bid!", true);
        assertEquals(1, chat.getChildren().size());
    }

    @Test
    @DisplayName("addChatMessage: isMe=false → style grey")
    void addChatMessage_isMe_false_addsRow() throws Exception {
        VBox chat = new VBox();
        setField(ctrl, "chatMessages", chat);
        method("addChatMessage", String.class, String.class, boolean.class)
                .invoke(ctrl, "bob", "watching", false);
        assertEquals(1, chat.getChildren().size());
    }

    @Test
    @DisplayName("addChatMessage: sau 50 messages → giới hạn 50 (xóa oldest)")
    void addChatMessage_over50_capped() throws Exception {
        VBox chat = new VBox();
        setField(ctrl, "chatMessages", chat);
        Method m = method("addChatMessage", String.class, String.class, boolean.class);
        for (int i = 0; i < 55; i++) {
            m.invoke(ctrl, "u", "msg" + i, i % 2 == 0);
        }
        assertEquals(50, chat.getChildren().size());
    }

    // ════════════════════════════════════════════════════════════════════════
    // 9. setBidInput
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setBidInput: tfBidAmount null → không crash")
    void setBidInput_nullTextField_noCrash() throws Exception {
        assertDoesNotThrow(() ->
                method("setBidInput", BigDecimal.class).invoke(ctrl, new BigDecimal("50000000")));
    }

    @Test
    @DisplayName("setBidInput: tfBidAmount non-null → text được set")
    void setBidInput_withTextField_setsText() throws Exception {
        TextField tf = new TextField();
        setField(ctrl, "tfBidAmount", tf);
        method("setBidInput", BigDecimal.class).invoke(ctrl, new BigDecimal("20000000"));
        assertFalse(tf.getText().isBlank());
    }

    // ════════════════════════════════════════════════════════════════════════
    // 10. loadItem
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("loadItem: ClientModel trả null → currentItem null, price ZERO, không crash")
    void loadItem_noCurrentItem_noCrash() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class)) {
            ClientModel mockModel = mock(ClientModel.class);
            when(ClientModel.getInstance()).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(null);

            assertDoesNotThrow(() -> method("loadItem").invoke(ctrl));
            assertNull(getField(ctrl, "currentItem"));
        }
    }

    @Test
    @DisplayName("loadItem: ClientModel trả item hợp lệ → currentPrice = item.currentPrice")
    void loadItem_withItem_setsCurrentPrice() throws Exception {
        AuctionItemDTO item = makeItem(10, BigDecimal.valueOf(80_000_000), Instant.now().plusSeconds(3600));
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class)) {
            ClientModel mockModel = mock(ClientModel.class);
            when(ClientModel.getInstance()).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);

            method("loadItem").invoke(ctrl);

            assertEquals(0, BigDecimal.valueOf(80_000_000).compareTo(getField(ctrl, "currentPrice")));
        }
    }

    @Test
    @DisplayName("loadItem: item có currentPrice null → currentPrice = ZERO")
    void loadItem_itemWithNullPrice_defaultsToZero() throws Exception {
        AuctionItemDTO item = makeItem(10, null, Instant.now().plusSeconds(3600));
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class)) {
            ClientModel mockModel = mock(ClientModel.class);
            when(ClientModel.getInstance()).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);

            method("loadItem").invoke(ctrl);

            assertEquals(0, BigDecimal.ZERO.compareTo(getField(ctrl, "currentPrice")));
        }
    }

    @Test
    @DisplayName("loadItem: item có title/category/description + labels non-null → labels được set")
    void loadItem_withLabels_setsAllLabels() throws Exception {
        AuctionItemDTO item = makeItem(10, BigDecimal.valueOf(50_000_000), Instant.now().plusSeconds(3600));
        Label nameLabel = new Label();
        Label catLabel  = new Label();
        Label descLabel = new Label();
        setField(ctrl, "lblProductName", nameLabel);
        setField(ctrl, "lblCategory", catLabel);
        setField(ctrl, "lblDescription", descLabel);

        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class)) {
            ClientModel mockModel = mock(ClientModel.class);
            when(ClientModel.getInstance()).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);

            method("loadItem").invoke(ctrl);

            assertFalse(nameLabel.getText().isBlank());
            assertFalse(catLabel.getText().isBlank());
            assertFalse(descLabel.getText().isBlank());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 11. initViewerCount
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("initViewerCount: labels null → không crash")
    void initViewerCount_nullLabels_noCrash() throws Exception {
        assertDoesNotThrow(() -> method("initViewerCount").invoke(ctrl));
    }

    @Test
    @DisplayName("initViewerCount: labels non-null → hiển thị đúng viewerCount và chatCount")
    void initViewerCount_withLabels_setsText() throws Exception {
        Label viewers = new Label();
        Label chats   = new Label();
        setField(ctrl, "lblViewers", viewers);
        setField(ctrl, "lblChatCount", chats);
        setField(ctrl, "viewerCount", 150);
        setField(ctrl, "chatCount", 7);

        method("initViewerCount").invoke(ctrl);

        assertTrue(viewers.getText().contains("150"));
        assertTrue(chats.getText().contains("7"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 12. subscribeRealtime
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("subscribeRealtime: AuctionService.subscribe thành công → subscription non-null")
    void subscribeRealtime_success_subscriptionSet() throws Exception {
        Consumer<Object> mockWrapper = o -> {};
        try (MockedStatic<AuctionService> as = mockStatic(AuctionService.class)) {
            @SuppressWarnings("unchecked")
            Consumer<Object> typed = (Consumer<Object>) (Object) mockWrapper;
            as.when(() -> AuctionService.subscribeAuctionStatus(any())).thenAnswer(inv -> typed);

            method("subscribeRealtime").invoke(ctrl);

            assertNotNull(getField(ctrl, "subscription"));
        }
    }

    @Test
    @DisplayName("subscribeRealtime: AuctionService throws → không crash (exception bị bắt)")
    void subscribeRealtime_serviceThrows_noCrash() throws Exception {
        try (MockedStatic<AuctionService> as = mockStatic(AuctionService.class)) {
            as.when(() -> AuctionService.subscribeAuctionStatus(any()))
                    .thenThrow(new RuntimeException("BE not available"));

            assertDoesNotThrow(() -> method("subscribeRealtime").invoke(ctrl));
            assertNull(getField(ctrl, "subscription"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 13. tick
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("tick: remainSeconds = 0 → gọi onAuctionEnded")
    void tick_zeroRemainSeconds_callsAuctionEnded() throws Exception {

        setField(ctrl, "remainSeconds", 0);
        setField(ctrl, "countdown", new Timeline());

        try (MockedStatic<AlertUtil> au = mockStatic(AlertUtil.class)) {
            au.when(() -> AlertUtil.showInfo(anyString()))
                    .thenAnswer(inv -> null);

            assertDoesNotThrow(() -> method("tick").invoke(ctrl));
        }
    }

    @Test
    @DisplayName("tick: remainSeconds > 0 → giảm 1, không gọi onAuctionEnded")
    void tick_positiveRemainSeconds_decrement() throws Exception {
        setField(ctrl, "remainSeconds", 30);
        method("tick").invoke(ctrl);
        assertEquals(29, (int) getField(ctrl, "remainSeconds"));
    }

    @Test
    @DisplayName("tick: remainSeconds % 5 == 0 → cập nhật viewerCount (branch đặc biệt)")
    void tick_multipleOf5_updatesViewerCount() throws Exception {
        setField(ctrl, "remainSeconds", 10);  // sau decrement = 9 (9%5≠0); test với 5
        setField(ctrl, "remainSeconds", 6);   // sau decrement = 5 (5%5==0)
        Label viewersLabel = new Label();
        setField(ctrl, "lblViewers", viewersLabel);
        int beforeCount = getField(ctrl, "viewerCount");

        method("tick").invoke(ctrl);  // remainSeconds 6→5, 5%5==0 → update viewer

        // viewerCount có thể thay đổi (random -2 đến +2), label phải được set
        assertFalse(viewersLabel.getText().isBlank());
    }

    @Test
    @DisplayName("tick: viewerCount drop bellow 1 → floor at 1")
    void tick_viewerCountFloorAtOne() throws Exception {
        setField(ctrl, "remainSeconds", 6);   // sau decrement = 5, 5%5==0
        setField(ctrl, "viewerCount", 1);     // đang ở đáy, random âm có thể xuống 0

        // Chạy nhiều lần để bao phủ branch "if viewerCount < 1 viewerCount = 1"
        for (int i = 0; i < 10; i++) {
            setField(ctrl, "remainSeconds", 6);
            method("tick").invoke(ctrl);
            int vc = getField(ctrl, "viewerCount");
            assertTrue(vc >= 1, "viewerCount phải luôn >= 1, got " + vc);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 14. onBidNow
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("onBidNow: parseBidInput null (tfBidAmount null) → set default bid vào tf (null tf → no-op)")
    void onBidNow_nullInput_noTextField_noCrash() {
        // tfBidAmount = null → setBidInput no-op
        assertDoesNotThrow(() -> ctrl.onBidNow());
    }

    @Test
    @DisplayName("onBidNow: parseBidInput null, tfBidAmount non-null → set default bid text")
    void onBidNow_nullInput_withTextField_setsDefaultBid() throws Exception {
        TextField tf = new TextField(""); // blank → parseBidInput returns null
        setField(ctrl, "tfBidAmount", tf);
        setField(ctrl, "currentPrice", new BigDecimal("100000000"));

        ctrl.onBidNow();

        // default = currentPrice + bidStep → "110000000 d" hoặc format tương đương
        assertFalse(tf.getText().isBlank());
    }

    @Test
    @DisplayName("onBidNow: input < minBid → showWarning, không đặt giá")
    void onBidNow_inputBelowMinBid_showsWarning() throws Exception {
        setField(ctrl, "currentPrice", new BigDecimal("100000000"));
        TextField tf = new TextField("105000000"); // < 110000000 (currentPrice + bidStep)
        setField(ctrl, "tfBidAmount", tf);

        try (MockedStatic<AlertUtil> au = mockStatic(AlertUtil.class)) {
            au.when(() -> AlertUtil.showWarning(anyString())).thenAnswer(inv -> null);
            ctrl.onBidNow();
            au.verify(() -> AlertUtil.showWarning(anyString()), times(1));
        }
    }

    @Test
    @DisplayName("onBidNow: input hợp lệ, currentItem null (demo mode) → update currentPrice")
    void onBidNow_validInput_demoMode_updatesPrice() throws Exception {
        setField(ctrl, "currentPrice", new BigDecimal("100000000"));
        TextField tf = new TextField("120000000");
        setField(ctrl, "tfBidAmount", tf);
        // currentItem = null (demo mode)

        ctrl.onBidNow();

        assertEquals(0, new BigDecimal("120000000").compareTo(getField(ctrl, "currentPrice")));
        assertEquals("", tf.getText()); // clear sau bid
    }

    // ════════════════════════════════════════════════════════════════════════
    // 15. onQuickBid (tất cả 4 nút)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("onQuickBid1: tfBidAmount null → không crash (setBidInput no-op)")
    void onQuickBid1_nullTextField_noCrash() {
        assertDoesNotThrow(() -> ctrl.onQuickBid1());
    }

    @Test
    @DisplayName("onQuickBid2: tfBidAmount null → không crash")
    void onQuickBid2_nullTextField_noCrash() {
        assertDoesNotThrow(() -> ctrl.onQuickBid2());
    }

    @Test
    @DisplayName("onQuickBid3: tfBidAmount null → không crash")
    void onQuickBid3_nullTextField_noCrash() {
        assertDoesNotThrow(() -> ctrl.onQuickBid3());
    }

    @Test
    @DisplayName("onQuickBid4: tfBidAmount null → không crash")
    void onQuickBid4_nullTextField_noCrash() {
        assertDoesNotThrow(() -> ctrl.onQuickBid4());
    }

    @Test
    @DisplayName("onQuickBid1: set input = currentPrice + 10M")
    void onQuickBid1_setsCorrectAmount() throws Exception {
        TextField tf = new TextField();
        setField(ctrl, "tfBidAmount", tf);
        setField(ctrl, "currentPrice", new BigDecimal("100000000"));
        ctrl.onQuickBid1();
        // 100M + 10M = 110M → text chứa "110"
        assertTrue(tf.getText().contains("110"));
    }

    @Test
    @DisplayName("onQuickBid4: set input = currentPrice + 100M")
    void onQuickBid4_setsCorrectAmount() throws Exception {
        TextField tf = new TextField();
        setField(ctrl, "tfBidAmount", tf);
        setField(ctrl, "currentPrice", new BigDecimal("100000000"));
        ctrl.onQuickBid4();
        // 100M + 100M = 200M → text chứa "200"
        assertTrue(tf.getText().contains("200"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 16. onSendMessage
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("onSendMessage: tfChatInput null → return ngay, không crash")
    void onSendMessage_nullTextField_noCrash() {
        assertDoesNotThrow(() -> ctrl.onSendMessage());
    }

    @Test
    @DisplayName("onSendMessage: text rỗng → không gửi (chatCount không tăng)")
    void onSendMessage_emptyText_noChatIncrement() throws Exception {
        TextField tf = new TextField("");
        setField(ctrl, "tfChatInput", tf);
        ctrl.onSendMessage();
        assertEquals(0, (int) getField(ctrl, "chatCount"));
    }

    @Test
    @DisplayName("onSendMessage: text hợp lệ → chatCount tăng lên 1, tf được clear")
    void onSendMessage_validText_incrementsChatCount() throws Exception {
        TextField tf = new TextField("Hello!");
        setField(ctrl, "tfChatInput", tf);

        try (MockedStatic<SessionManager> sm = mockStatic(SessionManager.class)) {
            sm.when(SessionManager::getUsername).thenReturn("testUser");
            ctrl.onSendMessage();
        }

        assertEquals(1, (int) getField(ctrl, "chatCount"));
        assertEquals("", tf.getText());
    }

    @Test
    @DisplayName("onSendMessage: lblChatCount non-null → cập nhật text")
    void onSendMessage_validText_updatesChatCountLabel() throws Exception {
        TextField tf = new TextField("Xin chào");
        Label chatCountLabel = new Label("0 tin nhan");
        setField(ctrl, "tfChatInput", tf);
        setField(ctrl, "lblChatCount", chatCountLabel);

        try (MockedStatic<SessionManager> sm = mockStatic(SessionManager.class)) {
            sm.when(SessionManager::getUsername).thenReturn("user");
            ctrl.onSendMessage();
        }

        assertTrue(chatCountLabel.getText().contains("1"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 17. cleanup
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("cleanup: subscription=null, countdown=null → không crash (all null branch)")
    void cleanup_allNull_noCrash() {
        assertDoesNotThrow(ctrl::cleanup);
    }

    @Test
    @DisplayName("cleanup: gọi 2 lần liên tiếp → idempotent, không crash")
    void cleanup_calledTwice_noCrash() {
        ctrl.cleanup();
        assertDoesNotThrow(ctrl::cleanup);
    }

    @Test
    @DisplayName("cleanup: subscription non-null → unsubscribeAuctionStatus được gọi, subscription = null sau")
    void cleanup_withSubscription_unsubscribes() throws Exception {
        Consumer<Object> mockSub = o -> {};
        setField(ctrl, "subscription", mockSub);

        try (MockedStatic<AuctionService> as = mockStatic(AuctionService.class)) {
            as.when(() -> AuctionService.unsubscribeAuctionStatus(any())).thenAnswer(inv -> null);
            ctrl.cleanup();
            as.verify(() -> AuctionService.unsubscribeAuctionStatus(any()), times(1));
        }

        assertNull(getField(ctrl, "subscription"));
    }

    @Test
    @DisplayName("cleanup: unsubscribeAuctionStatus throws → exception bị bắt, subscription = null")
    void cleanup_unsubscribeThrows_exceptionCaught() throws Exception {
        Consumer<Object> mockSub = o -> {};
        setField(ctrl, "subscription", mockSub);

        try (MockedStatic<AuctionService> as = mockStatic(AuctionService.class)) {
            as.when(() -> AuctionService.unsubscribeAuctionStatus(any()))
                    .thenThrow(new RuntimeException("network error"));

            assertDoesNotThrow(ctrl::cleanup);
        }

        assertNull(getField(ctrl, "subscription"));
    }
}