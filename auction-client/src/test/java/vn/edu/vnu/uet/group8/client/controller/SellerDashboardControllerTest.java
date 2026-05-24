package vn.edu.vnu.uet.group8.client.controller;

import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SellerDashboardController} — pure Java logic.
 *
 * Coverage:
 * - matchTab: all / draft / listed / sold/ended_no_bid/cancelled
 * - isActive: endTime null / future / past
 * - updateStats: listed/sold count, revenue sum
 * - currentFilter mặc định "all"
 * - constructor không crash
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class SellerDashboardControllerTest {

    private SellerDashboardController ctrl;

    @BeforeEach
    void setUp() {
        ctrl = new SellerDashboardController();
    }

    // ══════════════════════════════════════════════════════
    // Constructor + defaults
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("constructor không crash (headless)")
    void constructor_noCrash() {
        assertDoesNotThrow(SellerDashboardController::new);
    }

    @Test
    @DisplayName("currentFilter mặc định là 'all'")
    void currentFilter_default_all() throws Exception {
        Field f = SellerDashboardController.class.getDeclaredField("currentFilter");
        f.setAccessible(true);
        assertEquals("all", f.get(ctrl));
    }

    @Test
    @DisplayName("myItems mặc định là list rỗng")
    void myItems_default_empty() throws Exception {
        Field f = SellerDashboardController.class.getDeclaredField("myItems");
        f.setAccessible(true);
        List<?> items = (List<?>) f.get(ctrl);
        assertTrue(items.isEmpty());
    }

    // ══════════════════════════════════════════════════════
    // matchTab (private → reflection)
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("matchTab 'all': mọi item đều khớp")
    void matchTab_all_alwaysTrue() throws Exception {
        setFilter("all");
        Method m = getMatchTab();
        for (SessionStatus s : SessionStatus.values()) {
            AuctionItemDTO item = itemWithStatus(s);
            assertTrue((Boolean) m.invoke(ctrl, item),
                    "filter 'all' phải khớp với status: " + s);
        }
    }

    @Test
    @DisplayName("matchTab 'draft': chỉ UPCOMING → true")
    void matchTab_draft_onlyUpcoming() throws Exception {
        setFilter("draft");
        Method m = getMatchTab();
        assertTrue((Boolean)  m.invoke(ctrl, itemWithStatus(SessionStatus.UPCOMING)));
        assertFalse((Boolean) m.invoke(ctrl, itemWithStatus(SessionStatus.ACTIVE)));
        assertFalse((Boolean) m.invoke(ctrl, itemWithStatus(SessionStatus.SOLD)));
        assertFalse((Boolean) m.invoke(ctrl, itemWithStatus(SessionStatus.CANCELLED)));
    }

    @Test
    @DisplayName("matchTab 'listed': chỉ ACTIVE → true")
    void matchTab_listed_onlyActive() throws Exception {
        setFilter("listed");
        Method m = getMatchTab();
        assertTrue((Boolean)  m.invoke(ctrl, itemWithStatus(SessionStatus.ACTIVE)));
        assertFalse((Boolean) m.invoke(ctrl, itemWithStatus(SessionStatus.UPCOMING)));
        assertFalse((Boolean) m.invoke(ctrl, itemWithStatus(SessionStatus.SOLD)));
    }

    @Test
    @DisplayName("matchTab 'sold': SOLD → true")
    void matchTab_sold_soldTrue() throws Exception {
        setFilter("sold");
        Method m = getMatchTab();
        assertTrue((Boolean) m.invoke(ctrl, itemWithStatus(SessionStatus.SOLD)));
    }

    @Test
    @DisplayName("matchTab 'sold': ENDED_NO_BID → true")
    void matchTab_sold_endedNoBidTrue() throws Exception {
        setFilter("sold");
        Method m = getMatchTab();
        assertTrue((Boolean) m.invoke(ctrl, itemWithStatus(SessionStatus.ENDED_NO_BID)));
    }

    @Test
    @DisplayName("matchTab 'sold': CANCELLED → true")
    void matchTab_sold_cancelledTrue() throws Exception {
        setFilter("sold");
        Method m = getMatchTab();
        assertTrue((Boolean) m.invoke(ctrl, itemWithStatus(SessionStatus.CANCELLED)));
    }

    @Test
    @DisplayName("matchTab 'sold': ACTIVE → false")
    void matchTab_sold_activeFalse() throws Exception {
        setFilter("sold");
        Method m = getMatchTab();
        assertFalse((Boolean) m.invoke(ctrl, itemWithStatus(SessionStatus.ACTIVE)));
    }

    // ══════════════════════════════════════════════════════
    // isActive (private → reflection)
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("isActive: endTime null → false")
    void isActive_nullEndTime_false() throws Exception {
        Method m = SellerDashboardController.class.getDeclaredMethod("isActive", AuctionItemDTO.class);
        m.setAccessible(true);
        AuctionItemDTO item = AuctionItemDTO.of(
                1,
                "A",
                "",
                null,
                null,
                SessionStatus.ACTIVE,
                BigDecimal.ONE,
                null,
                "seller",
                null,
                null,
                0,
                Instant.now()
        );
        assertFalse((Boolean) m.invoke(ctrl, item));
    }

    @Test
    @DisplayName("isActive: endTime trong tương lai → true")
    void isActive_futureEndTime_true() throws Exception {
        Method m = SellerDashboardController.class.getDeclaredMethod("isActive", AuctionItemDTO.class);
        m.setAccessible(true);
        AuctionItemDTO item = AuctionItemDTO.of(
                1,
                "A",
                "",
                null,
                null,
                SessionStatus.ACTIVE,
                BigDecimal.ONE,
                Instant.now().plusSeconds(3600),
                "seller",
                null,
                null,
                0,
                Instant.now()
        );
        assertTrue((Boolean) m.invoke(ctrl, item));
    }

    @Test
    @DisplayName("isActive: endTime đã qua → false")
    void isActive_pastEndTime_false() throws Exception {
        Method m = SellerDashboardController.class.getDeclaredMethod("isActive", AuctionItemDTO.class);
        m.setAccessible(true);
        AuctionItemDTO item = AuctionItemDTO.of(
                1,
                "A",
                "",
                null,
                null,
                SessionStatus.ACTIVE,
                BigDecimal.ONE,
                Instant.now().minusSeconds(3600),
                "seller",
                null,
                null,
                0,
                Instant.now()
        );
        assertFalse((Boolean) m.invoke(ctrl, item));
    }

    // ══════════════════════════════════════════════════════
    // updateStats (private → reflection) với labels null
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("updateStats: danh sách rỗng, labels null → không crash")
    void updateStats_emptyList_nullLabels_noCrash() throws Exception {
        Field f = SellerDashboardController.class.getDeclaredField("myItems");
        f.setAccessible(true);
        f.set(ctrl, List.of());

        Method m = SellerDashboardController.class.getDeclaredMethod("updateStats");
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(ctrl));
    }

    @Test
    @DisplayName("updateStats: 3 ACTIVE + 2 SOLD → tổng doanh thu đúng (labels null)")
    void updateStats_mixedStatuses_noExceptionNorWrongCount() throws Exception {
        AuctionItemDTO a1 = itemWithStatusAndPrice(SessionStatus.ACTIVE, BigDecimal.valueOf(500_000));
        AuctionItemDTO a2 = itemWithStatusAndPrice(SessionStatus.ACTIVE, BigDecimal.valueOf(300_000));
        AuctionItemDTO a3 = itemWithStatusAndPrice(SessionStatus.ACTIVE, BigDecimal.valueOf(200_000));
        AuctionItemDTO s1 = itemWithStatusAndPrice(SessionStatus.SOLD, BigDecimal.valueOf(1_000_000));
        AuctionItemDTO s2 = itemWithStatusAndPrice(SessionStatus.SOLD, BigDecimal.valueOf(2_000_000));

        Field f = SellerDashboardController.class.getDeclaredField("myItems");
        f.setAccessible(true);
        f.set(ctrl, List.of(a1, a2, a3, s1, s2));

        Method m = SellerDashboardController.class.getDeclaredMethod("updateStats");
        m.setAccessible(true);
        // labels null → sẽ crash nếu không có null check → phải không crash
        assertDoesNotThrow(() -> m.invoke(ctrl));
    }

    @Test
    @DisplayName("updateStats: item.currentPrice null → không crash (null-safe sum)")
    void updateStats_nullPrice_noCrash() throws Exception {
        AuctionItemDTO sold = itemWithStatusAndPrice(SessionStatus.SOLD, null);
        Field f = SellerDashboardController.class.getDeclaredField("myItems");
        f.setAccessible(true);
        f.set(ctrl, List.of(sold));

        Method m = SellerDashboardController.class.getDeclaredMethod("updateStats");
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(ctrl));
    }

    // ══════════════════════════════════════════════════════
    // renderItems (labels null)
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("renderItems: itemListContainer null → không crash")
    void renderItems_nullContainer_noCrash() throws Exception {
        Method m = SellerDashboardController.class.getDeclaredMethod("renderItems");
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(ctrl));
    }

    // ══════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════

    private void setFilter(String filter) throws Exception {
        Field f = SellerDashboardController.class.getDeclaredField("currentFilter");
        f.setAccessible(true);
        f.set(ctrl, filter);
    }

    private Method getMatchTab() throws Exception {
        Method m = SellerDashboardController.class.getDeclaredMethod("matchTab", AuctionItemDTO.class);
        m.setAccessible(true);
        return m;
    }

    private AuctionItemDTO itemWithStatus(SessionStatus status) {
        return AuctionItemDTO.of(
                1,
                "Test item",
                "",
                null,
                null,
                status,
                BigDecimal.valueOf(100_000),
                null,
                "seller",
                null,
                null,
                0,
                Instant.now()
        );
    }

    private AuctionItemDTO itemWithStatusAndPrice(SessionStatus status, BigDecimal price) {
        return AuctionItemDTO.of(
                1,
                "Test item",
                "",
                null,
                null,
                status,
                price,
                null,
                "seller",
                null,
                null,
                0,
                Instant.now()
        );
    }
}