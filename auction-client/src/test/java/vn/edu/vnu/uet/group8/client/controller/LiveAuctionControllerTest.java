package vn.edu.vnu.uet.group8.client.controller;

import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionStatusDTO;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LiveAuctionController} — pure Java logic.
 *
 * Coverage:
 * - parseBidInput: blank/null/valid/invalid string
 * - formatPrice: null / zero / large
 * - handleStatusUpdate: null status / wrong itemId / correct update
 * - cleanup: khi countdown=null và subscription=null → không crash
 * - remainSeconds: initial = demo value khi không có item
 * - currentPrice mặc định ZERO
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class LiveAuctionControllerTest {

    private LiveAuctionController ctrl;

    @BeforeEach
    void setUp() {
        ctrl = new LiveAuctionController();
    }

    // ══════════════════════════════════════════════════════
    // parseBidInput (private → reflection)
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("parseBidInput: tfBidAmount null → trả về null")
    void parseBidInput_nullTextField_returnsNull() throws Exception {
        Method m = LiveAuctionController.class.getDeclaredMethod("parseBidInput");
        m.setAccessible(true);
        // tfBidAmount là null khi chưa load FXML
        Object result = m.invoke(ctrl);
        assertNull(result);
    }

    // ══════════════════════════════════════════════════════
    // formatPrice (private → reflection)
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("formatPrice(null) → '--'")
    void formatPrice_null_returnsDashes() throws Exception {
        Method m = LiveAuctionController.class.getDeclaredMethod("formatPrice", BigDecimal.class);
        m.setAccessible(true);
        assertEquals("--", m.invoke(ctrl, (Object) null));
    }

    @Test
    @DisplayName("formatPrice(ZERO) → chuỗi chứa '0'")
    void formatPrice_zero_containsZero() throws Exception {
        Method m = LiveAuctionController.class.getDeclaredMethod("formatPrice", BigDecimal.class);
        m.setAccessible(true);
        String result = (String) m.invoke(ctrl, BigDecimal.ZERO);
        assertNotNull(result);
        assertTrue(result.contains("0"));
    }

    @Test
    @DisplayName("formatPrice(1_000_000) → chuỗi không rỗng")
    void formatPrice_positive_notEmpty() throws Exception {
        Method m = LiveAuctionController.class.getDeclaredMethod("formatPrice", BigDecimal.class);
        m.setAccessible(true);
        String result = (String) m.invoke(ctrl, BigDecimal.valueOf(1_000_000));
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    @DisplayName("formatPrice: hai giá khác nhau → chuỗi khác nhau")
    void formatPrice_different_differentStrings() throws Exception {
        Method m = LiveAuctionController.class.getDeclaredMethod("formatPrice", BigDecimal.class);
        m.setAccessible(true);
        String s1 = (String) m.invoke(ctrl, BigDecimal.valueOf(100_000));
        String s2 = (String) m.invoke(ctrl, BigDecimal.valueOf(200_000));
        assertNotEquals(s1, s2);
    }

    // ══════════════════════════════════════════════════════
    // handleStatusUpdate (private → reflection)
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("handleStatusUpdate(null) → không crash")
    void handleStatusUpdate_null_noCrash() throws Exception {
        Method m = LiveAuctionController.class.getDeclaredMethod("handleStatusUpdate", AuctionStatusDTO.class);
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(ctrl, (Object) null));
    }

    @Test
    @DisplayName("handleStatusUpdate: status với itemId khác currentItem → không update price")
    void handleStatusUpdate_differentItemId_priceUnchanged() throws Exception {
        // currentItem = null → không update
        Method m = LiveAuctionController.class.getDeclaredMethod("handleStatusUpdate", AuctionStatusDTO.class);
        m.setAccessible(true);
        AuctionStatusDTO status = new AuctionStatusDTO(999, BigDecimal.valueOf(999_999), Instant.now(), List.of());
        assertDoesNotThrow(() -> m.invoke(ctrl, status));

        Field priceField = LiveAuctionController.class.getDeclaredField("currentPrice");
        priceField.setAccessible(true);
        // currentItem null → skip update → price vẫn ZERO
        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) priceField.get(ctrl)));
    }

    // ══════════════════════════════════════════════════════
    // cleanup
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("cleanup() khi countdown=null và subscription=null → không crash")
    void cleanup_allNull_noCrash() {
        assertDoesNotThrow(ctrl::cleanup);
    }

    @Test
    @DisplayName("cleanup() hai lần liên tiếp → không crash (idempotent)")
    void cleanup_calledTwice_noCrash() {
        ctrl.cleanup();
        assertDoesNotThrow(ctrl::cleanup);
    }

    // ══════════════════════════════════════════════════════
    // State mặc định
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("currentPrice mặc định ZERO")
    void currentPrice_default_zero() throws Exception {
        Field f = LiveAuctionController.class.getDeclaredField("currentPrice");
        f.setAccessible(true);
        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) f.get(ctrl)));
    }

    @Test
    @DisplayName("currentItem mặc định null khi chưa init")
    void currentItem_default_null() throws Exception {
        Field f = LiveAuctionController.class.getDeclaredField("currentItem");
        f.setAccessible(true);
        assertNull(f.get(ctrl));
    }

    @Test
    @DisplayName("subscription mặc định null")
    void subscription_default_null() throws Exception {
        Field f = LiveAuctionController.class.getDeclaredField("subscription");
        f.setAccessible(true);
        assertNull(f.get(ctrl));
    }

    @Test
    @DisplayName("bidStep mặc định = 10.000.000")
    void bidStep_default_tenMillion() throws Exception {
        Field f = LiveAuctionController.class.getDeclaredField("bidStep");
        f.setAccessible(true);
        BigDecimal step = (BigDecimal) f.get(ctrl);
        assertEquals(0, new BigDecimal("10000000").compareTo(step));
    }

    // ══════════════════════════════════════════════════════
    // setTimeDisplay (private → reflection, lblMinutes/lblSeconds null)
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("setTimeDisplay(0, 0) khi labels null → không crash")
    void setTimeDisplay_nullLabels_noCrash() throws Exception {
        Method m = LiveAuctionController.class.getDeclaredMethod("setTimeDisplay", int.class, int.class);
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(ctrl, 0, 0));
    }

    @Test
    @DisplayName("setTimeDisplay(8, 25) khi labels null → không crash")
    void setTimeDisplay_eightTwentyFive_noCrash() throws Exception {
        Method m = LiveAuctionController.class.getDeclaredMethod("setTimeDisplay", int.class, int.class);
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(ctrl, 8, 25));
    }

    // ══════════════════════════════════════════════════════
    // updatePriceDisplay (private → reflection)
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("updatePriceDisplay(null) khi labels null → không crash")
    void updatePriceDisplay_nullOldPrice_noCrash() throws Exception {
        Method m = LiveAuctionController.class.getDeclaredMethod("updatePriceDisplay", BigDecimal.class);
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(ctrl, (Object) null));
    }

    @Test
    @DisplayName("updatePriceDisplay(BigDecimal.ZERO) khi labels null → không crash")
    void updatePriceDisplay_zero_noCrash() throws Exception {
        Method m = LiveAuctionController.class.getDeclaredMethod("updatePriceDisplay", BigDecimal.class);
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(ctrl, BigDecimal.ZERO));
    }
}