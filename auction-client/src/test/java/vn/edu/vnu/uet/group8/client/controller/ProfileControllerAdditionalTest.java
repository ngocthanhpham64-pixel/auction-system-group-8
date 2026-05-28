package vn.edu.vnu.uet.group8.client.controller;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.model.NotificationDTO;
import vn.edu.vnu.uet.group8.common.dto.model.UserBidHistoryDTO;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

// =========================================================================
//  ProfileController – untested branches
// =========================================================================
@DisplayName("ProfileController – additional branches")
class ProfileControllerAdditionalTest extends FxTestBase {

    private ProfileController controller;

    @BeforeEach
    void setup() {
        SessionManager.clearSession();
        controller = new ProfileController();
        controller.lblName        = new Label();
        controller.lblEmail       = new Label();
        controller.lblAvatar      = new Label();
        controller.lblBalance     = new Label();
        controller.lblRating      = new Label();
        controller.lblTotalBids   = new Label();
        controller.lblActiveBids  = new Label();
        controller.lblWonBids     = new Label();
        controller.bidHistoryList = new VBox();
        controller.btnTabActive   = new Button();
        controller.btnTabWon      = new Button();
        controller.btnTabLost     = new Button();
        controller.activeTab      = controller.btnTabActive;
        controller.currentFilter  = "active";
        controller.allBids        = List.of();
    }

    private UserBidHistoryDTO bid(
            int id,
            BigDecimal myAmount,
            BigDecimal sessionPrice,
            Instant endTime
    ) {
        return UserBidHistoryDTO.of(
                1,                  // bidId
                100,                // sessionId
                id,
                "Item " + id,
                myAmount,
                sessionPrice,
                Instant.now(),      // bidTime
                endTime
        );
    }

    // ─── setTab – CSS transitions ───

    @Nested @DisplayName("setTab – CSS class transitions")
    class SetTabCss {

        @Test @DisplayName("setTab thêm tag-active cho button mới")
        void new_button_gets_active() {
            controller.setTab("won", controller.btnTabWon);
            assertTrue(controller.btnTabWon.getStyleClass().contains("tag-active"));
        }

        @Test @DisplayName("setTab xóa tag-active khỏi tab cũ")
        void old_tab_loses_active() {
            controller.btnTabActive.getStyleClass().add("tag-active");
            controller.setTab("won", controller.btnTabWon);
            assertFalse(controller.btnTabActive.getStyleClass().contains("tag-active"));
        }

        @Test @DisplayName("setTab với activeTab null → không crash")
        void null_active_tab_no_crash() {
            controller.activeTab = null;
            assertDoesNotThrow(() -> controller.setTab("won", controller.btnTabWon));
        }

        @Test @DisplayName("setTab cập nhật activeTab")
        void updates_active_tab() {
            controller.setTab("won", controller.btnTabWon);
            assertSame(controller.btnTabWon, controller.activeTab);
        }

        @Test @DisplayName("setTab cập nhật currentFilter")
        void updates_current_filter() {
            controller.setTab("lost", controller.btnTabLost);
            assertEquals("lost", controller.currentFilter);
        }
    }

    // ─── onLogout – confirm false → không logout ───

    @Nested @DisplayName("onLogout")
    class OnLogout {

        @Test @DisplayName("từ non-FX thread → showConfirm=false → session giữ nguyên")
        void confirm_false_keeps_session() {
            SessionManager.setSession("tok", 1, "u", "f", "MEMBER");
            controller.onLogout();
            assertTrue(SessionManager.isLoggedIn());
        }

        @Test @DisplayName("onLogout không crash")
        void no_crash() {
            assertDoesNotThrow(() -> controller.onLogout());
        }
    }

    // ─── onDeposit – MainController null ───

    @Nested @DisplayName("onDeposit – MainController null")
    class OnDeposit {

        @Test @DisplayName("MainController.getInstance() null → không crash (showWarning)")
        void main_null_no_crash() {
            // MainController.instance chưa được set → null → showWarning
            assertDoesNotThrow(() -> controller.onDeposit());
        }
    }

    // ─── onSettings – MainController null ───

    @Nested @DisplayName("onSettings – MainController null")
    class OnSettings {

        @Test @DisplayName("MainController.getInstance() null → không crash")
        void main_null_no_crash() {
            assertDoesNotThrow(() -> controller.onSettings());
        }
    }

    // ─── matchTab – bidAmount null / sessionPrice null ───

    @Nested @DisplayName("matchTab – null fields")
    class MatchTabNullFields {

        @Test @DisplayName("bidAmount null, filter 'won' → không crash, không match")
        void null_bid_amount_won() {
            controller.currentFilter = "won";
            UserBidHistoryDTO b = bid(1, null, new BigDecimal("100"), Instant.now().minusSeconds(3600));
            assertFalse(controller.matchTab(b));
        }

        @Test @DisplayName("sessionPrice null, filter 'won' → không crash, không match")
        void null_session_price_won() {
            controller.currentFilter = "won";
            UserBidHistoryDTO b = bid(1, new BigDecimal("100"), null, Instant.now().minusSeconds(3600));
            assertFalse(controller.matchTab(b));
        }

        @Test @DisplayName("cả hai null, filter 'active' → isEnded=false → match active")
        void both_null_active_filter() {
            controller.currentFilter = "active";
            UserBidHistoryDTO b = bid(1, null, null, null);
            assertTrue(controller.matchTab(b));
        }
    }

    // ─── renderBids – filter won/lost ───

    @Nested @DisplayName("renderBids – filter won/lost")
    class RenderBidsFilter {

        @Test @DisplayName("filter 'won' → chỉ render won bids")
        void filter_won_renders_winners() {
            controller.currentFilter = "won";
            UserBidHistoryDTO won = bid(1, new BigDecimal("200"), new BigDecimal("200"),
                    Instant.now().minusSeconds(3600));
            UserBidHistoryDTO lost = bid(2, new BigDecimal("100"), new BigDecimal("200"),
                    Instant.now().minusSeconds(3600));
            controller.allBids = List.of(won, lost);
            controller.renderBids();
            assertEquals(1, controller.bidHistoryList.getChildren().size());
        }

        @Test @DisplayName("filter 'lost' → chỉ render lost bids")
        void filter_lost_renders_losers() {
            controller.currentFilter = "lost";
            UserBidHistoryDTO won = bid(1, new BigDecimal("200"), new BigDecimal("200"),
                    Instant.now().minusSeconds(3600));
            UserBidHistoryDTO lost = bid(2, new BigDecimal("100"), new BigDecimal("200"),
                    Instant.now().minusSeconds(3600));
            controller.allBids = List.of(won, lost);
            controller.renderBids();
            assertEquals(1, controller.bidHistoryList.getChildren().size());
        }
    }

    // ─── updateBalance – binding ───

    @Nested @DisplayName("updateBalance")
    class UpdateBalance {

        @Test @DisplayName("BigDecimal.ZERO → '0'")
        void zero_balance() {
            controller.updateBalance(BigDecimal.ZERO);
            assertTrue(controller.lblBalance.getText().contains("0"));
        }

        @Test @DisplayName("âm → không crash")
        void negative_no_crash() {
            assertDoesNotThrow(() -> controller.updateBalance(new BigDecimal("-500000")));
        }

        @Test @DisplayName("lblBalance null → không crash")
        void null_label_no_crash() {
            controller.lblBalance = null;
            assertDoesNotThrow(() -> controller.updateBalance(new BigDecimal("1000")));
        }
    }
}


// =========================================================================
//  NotificationController – untested branches
// =========================================================================
@DisplayName("NotificationController – additional branches")
class NotificationControllerAdditionalTest extends FxTestBase {

    private NotificationController controller;

    @BeforeEach
    void setup() {
        controller = new NotificationController();
        controller.notificationList = new VBox();
        controller.lblNewCount      = new Label();
        controller.btnTabAll        = new Button();
        controller.btnTabUnread     = new Button();
        controller.btnTabAuction    = new Button();
        controller.btnTabSystem     = new Button();
        controller.activeTab        = controller.btnTabAll;
        controller.currentFilter    = "all";
    }

    private NotificationDTO notif(int id, boolean read, String type) {
        return NotificationDTO.builder()
                .id(id).title("Notif " + id).isRead(read)
                .type(type).createdAt(Instant.now()).build();
    }

    // ─── setTab – CSS ───

    @Nested @DisplayName("setTab – CSS class")
    class SetTabCss {

        @Test @DisplayName("setTab thêm tag-active cho button mới")
        void new_button_gets_active() {
            controller.setTab("unread", controller.btnTabUnread);
            assertTrue(controller.btnTabUnread.getStyleClass().contains("tag-active"));
        }

        @Test @DisplayName("setTab xóa tag-active khỏi tab cũ")
        void old_tab_loses_active() {
            controller.btnTabAll.getStyleClass().add("tag-active");
            controller.setTab("unread", controller.btnTabUnread);
            assertFalse(controller.btnTabAll.getStyleClass().contains("tag-active"));
        }

        @Test @DisplayName("setTab cập nhật currentFilter")
        void updates_filter() {
            controller.setTab("auction", controller.btnTabAuction);
            assertEquals("auction", controller.currentFilter);
        }

        @Test @DisplayName("activeTab null → không crash")
        void null_active_tab_no_crash() {
            controller.activeTab = null;
            assertDoesNotThrow(() -> controller.setTab("system", controller.btnTabSystem));
        }
    }

    // ─── onMarkAllRead – no unread ───

    @Nested @DisplayName("onMarkAllRead")
    class OnMarkAllRead {

        @Test @DisplayName("không có notif chưa đọc → showInfo, không crash")
        void no_unread_shows_info() {
            ClientModel.getInstance().setNotifications(
                    List.of(notif(1, true, "AUCTION"), notif(2, true, "SYSTEM")));
            assertDoesNotThrow(() -> controller.onMarkAllRead());
        }

        @Test @DisplayName("có unread nhưng confirm false (non-FX) → không crash")
        void has_unread_confirm_false_no_crash() {
            ClientModel.getInstance().setNotifications(
                    List.of(notif(1, false, "AUCTION")));
            assertDoesNotThrow(() -> controller.onMarkAllRead());
        }
    }

    // ─── onClearRead ───

    @Nested @DisplayName("onClearRead")
    class OnClearRead {

        @Test @DisplayName("confirm false (non-FX) → không crash")
        void confirm_false_no_crash() {
            assertDoesNotThrow(() -> controller.onClearRead());
        }
    }

    // ─── onDeleteNotification / onViewDetail / onBidNow ───

    @Nested @DisplayName("informational FXML handlers")
    class InformationalHandlers {

        @Test @DisplayName("onDeleteNotification → không crash")
        void delete_notification_no_crash() {
            assertDoesNotThrow(() -> controller.onDeleteNotification());
        }

        @Test @DisplayName("onViewDetail → không crash")
        void view_detail_no_crash() {
            assertDoesNotThrow(() -> controller.onViewDetail());
        }

        @Test @DisplayName("onBidNow → không crash")
        void bid_now_no_crash() {
            assertDoesNotThrow(() -> controller.onBidNow());
        }
    }

    // ─── renderEmpty ───

    @Nested @DisplayName("renderEmpty")
    class RenderEmpty {

        @Test @DisplayName("renderEmpty → 1 Label trong notificationList")
        void renders_one_empty_label() {
            controller.renderEmpty();
            assertEquals(1, controller.notificationList.getChildren().size());
            assertInstanceOf(Label.class, controller.notificationList.getChildren().get(0));
        }

        @Test @DisplayName("renderEmpty với notificationList null → không crash")
        void null_list_no_crash() {
            controller.notificationList = null;
            assertDoesNotThrow(() -> controller.renderEmpty());
        }
    }

    // ─── matchFilter – case-insensitive ───

    @Nested @DisplayName("matchFilter – case-insensitive")
    class MatchFilterCase {

        @Test @DisplayName("'unread' + type null → không crash")
        void unread_null_type_no_crash() {
            controller.currentFilter = "unread";
            NotificationDTO n = NotificationDTO.builder()
                    .id(1).title("T").isRead(false).type(null).createdAt(Instant.now()).build();
            assertDoesNotThrow(() -> controller.matchFilter(n));
        }

        @Test @DisplayName("'auction' + read=true → không match unread filter")
        void auction_read_no_match_unread() {
            controller.currentFilter = "unread";
            assertFalse(controller.matchFilter(notif(1, true, "AUCTION")));
        }
    }
}


// =========================================================================
//  SellerDashboardController – untested branches
// =========================================================================
@DisplayName("SellerDashboardController – additional branches")
class SellerDashboardControllerAdditionalTest extends FxTestBase {

    private SellerDashboardController controller;

    @BeforeEach
    void setup() {
        controller = new SellerDashboardController();
        controller.lblListedCount   = new Label();
        controller.lblSoldCount     = new Label();
        controller.lblTotalRevenue  = new Label();
        controller.lblRating        = new Label();
        controller.itemListContainer = new VBox();
        controller.lblEmpty         = new Label();
        controller.btnTabAll        = new Button();
        controller.btnTabDraft      = new Button();
        controller.btnTabListed     = new Button();
        controller.btnTabSold       = new Button();
        controller.activeTab        = controller.btnTabAll;
        controller.currentFilter    = "all";
        controller.myItems          = List.of();
    }

    private AuctionItemDTO item(
            int id,
            SessionStatus status,
            BigDecimal price
    ) {
        return AuctionItemDTO.of(
                id,
                "Item " + id,
                "Test description",
                ItemCategory.ELECTRONICS,
                ItemCondition.USED,
                status,
                price,
                Instant.now().plusSeconds(3600),
                "seller",
                null,
                List.of(),
                0,
                Instant.now()
        );
    }

    // ─── setTab – CSS ───

    @Nested @DisplayName("setTab – CSS class")
    class SetTabCss {

        @Test @DisplayName("setTab thêm tag-active cho button mới")
        void new_button_gets_active() {
            controller.setTab("draft", controller.btnTabDraft);
            assertTrue(controller.btnTabDraft.getStyleClass().contains("tag-active"));
        }

        @Test @DisplayName("setTab xóa tag-active khỏi tab cũ")
        void old_tab_loses_active() {
            controller.btnTabAll.getStyleClass().add("tag-active");
            controller.setTab("draft", controller.btnTabDraft);
            assertFalse(controller.btnTabAll.getStyleClass().contains("tag-active"));
        }

        @Test @DisplayName("activeTab null → không crash")
        void null_active_tab_no_crash() {
            controller.activeTab = null;
            assertDoesNotThrow(() -> controller.setTab("listed", controller.btnTabListed));
        }

        @Test @DisplayName("setTab cập nhật currentFilter")
        void updates_filter() {
            controller.setTab("sold", controller.btnTabSold);
            assertEquals("sold", controller.currentFilter);
        }
    }

    // ─── isActive ───

    @Nested @DisplayName("isActive")
    class IsActive {

        @Test @DisplayName("ACTIVE → true")
        void active_is_active() {
            assertTrue(controller.isActive(item(1, SessionStatus.ACTIVE, BigDecimal.ONE)));
        }

        @Test @DisplayName("SOLD → false")
        void sold_not_active() {
            assertFalse(controller.isActive(item(1, SessionStatus.SOLD, BigDecimal.ONE)));
        }

        @Test
        @DisplayName("null status → false")
        void null_status_not_active() {

            AuctionItemDTO it = AuctionItemDTO.of(
                    1,
                    "Test Item",
                    "desc",
                    ItemCategory.ELECTRONICS,
                    ItemCondition.USED,
                    null, // status = null
                    BigDecimal.TEN,
                    Instant.now(),
                    "seller",
                    null,
                    List.of(),
                    0,
                    Instant.now()
            );

            assertFalse(controller.isActive(it));
        }
    }

    // ─── onCreateNew ───

    @Nested @DisplayName("onCreateNew")
    class OnCreateNew {

        @Test @DisplayName("onCreateNew → không crash (SceneManager.switchTo log + return)")
        void no_crash() {
            assertDoesNotThrow(() -> controller.onCreateNew());
        }
    }

    // ─── showEmptyState ───

    @Nested @DisplayName("showEmptyState")
    class ShowEmptyState {

        @Test @DisplayName("lblEmpty null → không crash")
        void null_label_no_crash() {
            controller.lblEmpty = null;
            assertDoesNotThrow(() -> controller.showEmptyState());
        }

        @Test @DisplayName("itemListContainer null → không crash")
        void null_container_no_crash() {
            controller.itemListContainer = null;
            assertDoesNotThrow(() -> controller.showEmptyState());
        }

        @Test @DisplayName("showEmptyState → lblEmpty visible=true")
        void label_becomes_visible() {
            controller.lblEmpty.setVisible(false);
            controller.showEmptyState();
            assertTrue(controller.lblEmpty.isVisible());
        }
    }

    // ─── renderItems – empty vs items ───

    @Nested @DisplayName("renderItems")
    class RenderItems {

        @Test @DisplayName("myItems rỗng → showEmptyState, không crash")
        void empty_items_shows_empty() {
            controller.myItems = List.of();
            assertDoesNotThrow(() -> controller.renderItems());
        }

        @Test @DisplayName("có items, filter 'all' → itemListContainer có children")
        void has_items_renders_rows() {
            controller.currentFilter = "all";
            controller.myItems = List.of(
                    item(1, SessionStatus.ACTIVE, new BigDecimal("1000000")),
                    item(2, SessionStatus.SOLD, new BigDecimal("2000000"))
            );
            controller.renderItems();
            assertFalse(controller.itemListContainer.getChildren().isEmpty());
        }

        @Test @DisplayName("itemListContainer null → không crash")
        void null_container_no_crash() {
            controller.itemListContainer = null;
            assertDoesNotThrow(() -> controller.renderItems());
        }
    }
}


// =========================================================================
//  ExploreController – untested branches
// =========================================================================
@DisplayName("ExploreController – additional branches")
class ExploreControllerAdditionalTest extends FxTestBase {

    private ExploreController controller;

    @BeforeEach
    void setup() {
        controller = new ExploreController();

        controller.cbCategory = new ComboBox<>();
        controller.cbSort = new ComboBox<>();
        controller.productContainer = new TilePane();
        controller.lblResultCount = new Label();

        controller.currentTagFilter = "ALL";

        // KHÔNG gán mới vì allItems là final
        controller.allItems.clear();
    }

    private AuctionItemDTO item(
            int id,
            String title,
            ItemCategory cat,
            BigDecimal price
    ) {
        return AuctionItemDTO.of(
                id,
                title,
                "Test description",
                cat,
                ItemCondition.USED,
                SessionStatus.ACTIVE,
                price,
                Instant.now().plusSeconds(3600),
                "seller",
                null,
                List.of(),
                0,
                Instant.now()
        );
    }

    // ─── applySorting – 'Mới nhất' và null ───

    @Nested @DisplayName("applySorting – additional sort modes")
    class ApplySortingAdditional {

        @Test @DisplayName("'Mới nhất' → không crash, trả về list")
        void newest_sort_no_crash() {
            controller.cbSort.getItems().add("Mới nhất");
            controller.cbSort.setValue("Mới nhất");
            var result = controller.applySorting(List.of(
                    item(1, "A", ItemCategory.ELECTRONICS, new BigDecimal("100")),
                    item(2, "B", ItemCategory.ELECTRONICS, new BigDecimal("200"))
            ));
            assertEquals(2, result.size());
        }

        @Test @DisplayName("sort value null → không crash")
        void null_sort_value_no_crash() {
            controller.cbSort.setValue(null);
            assertDoesNotThrow(() -> controller.applySorting(List.of(
                    item(1, "A", ItemCategory.ELECTRONICS, BigDecimal.ONE))));
        }

        @Test @DisplayName("list rỗng → trả về rỗng không crash")
        void empty_list_returns_empty() {
            controller.cbSort.setValue("Giá thấp -> cao");
            var result = controller.applySorting(List.of());
            assertTrue(result.isEmpty());
        }
    }

    // ─── matchCategory – exact match ───

    @Nested @DisplayName("matchCategory – exact label match")
    class MatchCategoryExact {

        @Test @DisplayName("WATCHES label → match WATCHES item")
        void watches_label_matches() {
            String label = ItemCategory.WATCHES.getLabel();
            assertTrue(controller.matchCategory(
                    item(1, "X", ItemCategory.WATCHES, BigDecimal.ONE), label));
        }

        @Test @DisplayName("WATCHES label → không match ELECTRONICS")
        void watches_label_no_match_electronics() {
            String label = ItemCategory.WATCHES.getLabel();
            assertFalse(controller.matchCategory(
                    item(1, "X", ItemCategory.ELECTRONICS, BigDecimal.ONE), label));
        }
    }

    // ─── renderEmptyState ───

    @Nested @DisplayName("renderEmptyState")
    class RenderEmptyState {

        @Test @DisplayName("productContainer null → không crash")
        void null_container_no_crash() {
            controller.productContainer = null;
            assertDoesNotThrow(() -> controller.renderEmptyState("Không có sản phẩm"));
        }

        @Test @DisplayName("message null → không crash")
        void null_message_no_crash() {
            assertDoesNotThrow(() -> controller.renderEmptyState(null));
        }

        @Test @DisplayName("message hợp lệ → container có 1 child")
        void valid_message_adds_child() {
            controller.renderEmptyState("Không có sản phẩm");
            assertEquals(1, controller.productContainer.getChildren().size());
        }
    }

    // ─── onTagClick ───

    @Nested @DisplayName("onTagClick")
    class OnTagClick {

        @Test @DisplayName("userData null → không crash")
        void null_user_data_no_crash() {
            Button btn = new Button();
            btn.setUserData(null);
            ActionEvent event = new ActionEvent(btn, null);
            assertDoesNotThrow(() -> controller.onTagClick(event));
        }

        @Test @DisplayName("source không phải Button → không crash")
        void non_button_source_no_crash() {
            ActionEvent event = new ActionEvent(new Label(), null);
            assertDoesNotThrow(() -> controller.onTagClick(event));
        }

        @Test @DisplayName("userData 'ELECTRONICS' → currentTagFilter = 'ELECTRONICS'")
        void sets_tag_filter() {
            Button btn = new Button();
            btn.setUserData("ELECTRONICS");
            ActionEvent event = new ActionEvent(btn, null);
            controller.onTagClick(event);
            assertEquals("ELECTRONICS", controller.currentTagFilter);
        }
    }
}


// =========================================================================
//  FavoriteController – untested branches
// =========================================================================
@DisplayName("FavoriteController – additional branches")
class FavoriteControllerAdditionalTest extends FxTestBase {

    private FavoriteController controller;

    @BeforeEach
    void setup() {
        ClientModel.getInstance().setFavoriteItems(List.of());
        controller = new FavoriteController();
        controller.favoriteContainer = new FlowPane();
        controller.lblActiveCount    = new Label();
        controller.btnTabAll         = new Button();
        controller.btnTabActive      = new Button();
        controller.btnTabEnded       = new Button();
        controller.activeTab         = controller.btnTabAll;
        controller.currentFilter     = "all";
    }

    private AuctionItemDTO item(int id, Instant endTime) {
        return AuctionItemDTO.of(
                id,
                "Item " + id,
                "Test description",
                ItemCategory.ELECTRONICS,
                ItemCondition.USED,
                SessionStatus.ACTIVE,
                BigDecimal.valueOf(1_000_000),
                endTime,
                "seller",
                null,
                List.of(),
                0,
                Instant.now()
        );
    }

    // ─── onClearEnded – empty ended list ───

    @Nested @DisplayName("onClearEnded")
    class OnClearEnded {

        @Test @DisplayName("không có item ended → showInfo, không crash")
        void no_ended_items_shows_info() {
            ClientModel.getInstance().setFavoriteItems(
                    List.of(item(1, Instant.now().plusSeconds(3600))));
            assertDoesNotThrow(() -> controller.onClearEnded());
        }

        @Test
        @DisplayName("có ended items → không crash")
        void has_ended_items_no_crash() {

            ClientModel.getInstance().setFavoriteItems(
                    List.of(item(1, Instant.now().minusSeconds(3600)))
            );

            assertDoesNotThrow(() -> controller.onClearEnded());
        }
    }

    // ─── setActiveTab – CSS ───

    @Nested @DisplayName("setActiveTab – CSS class")
    class SetActiveTabCss {

        @Test @DisplayName("target → nhận tag-active")
        void target_gets_active() {
            controller.setActiveTab(controller.btnTabActive);
            assertTrue(controller.btnTabActive.getStyleClass().contains("tag-active"));
        }

        @Test @DisplayName("cũ → mất tag-active")
        void old_loses_active() {
            controller.btnTabAll.getStyleClass().add("tag-active");
            controller.activeTab = controller.btnTabAll;
            controller.setActiveTab(controller.btnTabActive);
            assertFalse(controller.btnTabAll.getStyleClass().contains("tag-active"));
        }

        @Test @DisplayName("null target → không crash")
        void null_target_no_crash() {
            assertDoesNotThrow(() -> controller.setActiveTab(null));
        }

        @Test @DisplayName("activeTab null → không crash")
        void null_active_tab_no_crash() {
            controller.activeTab = null;
            assertDoesNotThrow(() -> controller.setActiveTab(controller.btnTabAll));
        }
    }

    // ─── emptyMessage – tất cả nhánh ───

    @Nested @DisplayName("emptyMessage – all filters")
    class EmptyMessage {

        @Test @DisplayName("'all' → chứa 'yêu thích'")
        void all_message() {
            controller.currentFilter = "all";
            assertTrue(controller.emptyMessage().contains("yêu thích"));
        }

        @Test @DisplayName("'active' → chứa 'hoạt động'")
        void active_message() {
            controller.currentFilter = "active";
            assertTrue(controller.emptyMessage().contains("hoạt động"));
        }

        @Test @DisplayName("'ended' → chứa 'kết thúc'")
        void ended_message() {
            controller.currentFilter = "ended";
            assertTrue(controller.emptyMessage().contains("kết thúc"));
        }

        @Test @DisplayName("unknown filter → trả về non-null non-blank")
        void unknown_filter_fallback() {
            controller.currentFilter = "xyz";
            String msg = controller.emptyMessage();
            assertNotNull(msg);
            assertFalse(msg.isBlank());
        }
    }

    // ─── filter – edge cases ───

    @Nested @DisplayName("filter – edge cases")
    class FilterEdge {

        @Test @DisplayName("list null → không crash, trả về rỗng")
        void null_list_no_crash() {
            controller.currentFilter = "all";
            assertDoesNotThrow(() -> {
                var result = controller.filter(null);
                assertNotNull(result);
            });
        }

        @Test @DisplayName("endTime = now (biên) → isActive hoặc isEnded nhất quán")
        void end_time_now_boundary() {
            AuctionItemDTO it = item(1, Instant.now());
            // Không crash và trả về kết quả hợp lý
            assertDoesNotThrow(() -> {
                boolean active = controller.isActive(it);
                boolean ended  = controller.isEnded(it);
                // Một trong hai phải là true, hoặc cả hai false (race condition 1ms)
                assertFalse(active && ended);
            });
        }
    }
}


// =========================================================================
//  MainController – untested branches
// =========================================================================
@DisplayName("MainController – additional branches")
class MainControllerAdditionalTest extends FxTestBase {

    private MainController controller;

    @BeforeEach
    void setup() {
        SessionManager.clearSession();
        controller = new MainController();
        controller.lblFavCount  = new Label();
        controller.lblNotiCount = new Label();
        controller.lblAvatar    = new Label();
        controller.sidebar      = new VBox();
        controller.sidebar.setVisible(true);
        controller.sidebar.setManaged(true);
        controller.btnHome      = new Button();
        controller.tfSearch     = new TextField();
        controller.contentPane  = new StackPane();
        controller.activeNav    = null;
    }

    // ─── loadView ───

    @Nested @DisplayName("loadView")
    class LoadView {

        @Test @DisplayName("fxml không tồn tại → log severe, không crash")
        void nonexistent_fxml_no_crash() {
            assertDoesNotThrow(() -> controller.loadView("NonExistentView.fxml"));
        }

        @Test @DisplayName("null fxml → không crash")
        void null_fxml_no_crash() {
            assertDoesNotThrow(() -> controller.loadView(null));
        }

        @Test @DisplayName("contentPane null → không crash")
        void null_content_pane_no_crash() {
            controller.contentPane = null;
            assertDoesNotThrow(() -> controller.loadView("ExploreView.fxml"));
        }
    }

    // ─── setActiveNav – CSS ───

    @Nested @DisplayName("setActiveNav – CSS class")
    class SetActiveNavCss {

        @Test @DisplayName("target nhận nav-item-active")
        void target_gets_active() {
            controller.setActiveNav(controller.btnHome);
            assertTrue(controller.btnHome.getStyleClass().contains("nav-item-active"));
        }

        @Test @DisplayName("cũ mất nav-item-active")
        void old_loses_active() {
            Button prev = new Button();
            prev.getStyleClass().add("nav-item-active");
            controller.activeNav = prev;
            controller.setActiveNav(controller.btnHome);
            assertFalse(prev.getStyleClass().contains("nav-item-active"));
        }

        @Test @DisplayName("gọi 3 lần liên tiếp → không crash")
        void three_calls_no_crash() {
            Button b1 = new Button(), b2 = new Button(), b3 = new Button();
            assertDoesNotThrow(() -> {
                controller.setActiveNav(b1);
                controller.setActiveNav(b2);
                controller.setActiveNav(b3);
            });
        }
    }

    // ─── onLogout – confirm false ───

    @Nested @DisplayName("onLogout")
    class OnLogout {

        @Test @DisplayName("confirm false (non-FX) → session giữ nguyên")
        void confirm_false_keeps_session() {
            SessionManager.setSession("tok", 1, "u", "f", "MEMBER");
            controller.onLogout();
            assertTrue(SessionManager.isLoggedIn());
        }
    }

    // ─── onNavClick – các route ───

    @Nested @DisplayName("onNavClick – routing")
    class OnNavClick {

        private ActionEvent eventWithRoute(String route) {
            Button btn = new Button();
            btn.setUserData(route);
            return new ActionEvent(btn, null);
        }

        @Test @DisplayName("source không phải Button → không crash")
        void non_button_source_no_crash() {
            ActionEvent event = new ActionEvent(new Label(), null);
            assertDoesNotThrow(() -> controller.onNavClick(event));
        }

        @Test @DisplayName("userData null → không crash")
        void null_user_data_no_crash() {
            Button btn = new Button();
            btn.setUserData(null);
            assertDoesNotThrow(() -> controller.onNavClick(new ActionEvent(btn, null)));
        }

        @Test @DisplayName("route 'HOME' → không crash")
        void route_home_no_crash() {
            assertDoesNotThrow(() -> controller.onNavClick(eventWithRoute("HOME")));
        }

        @Test @DisplayName("route 'WALLET' → không crash")
        void route_wallet_no_crash() {
            assertDoesNotThrow(() -> controller.onNavClick(eventWithRoute("WALLET")));
        }

        @Test @DisplayName("route 'LIVE' → không crash")
        void route_live_no_crash() {
            assertDoesNotThrow(() -> controller.onNavClick(eventWithRoute("LIVE")));
        }

        @Test @DisplayName("route 'SELLER' → không crash")
        void route_seller_no_crash() {
            assertDoesNotThrow(() -> controller.onNavClick(eventWithRoute("SELLER")));
        }

        @Test @DisplayName("route 'MY_AUCTIONS' → không crash")
        void route_my_auctions_no_crash() {
            assertDoesNotThrow(() -> controller.onNavClick(eventWithRoute("MY_AUCTIONS")));
        }

        @Test @DisplayName("route 'SETTINGS' → không crash")
        void route_settings_no_crash() {
            assertDoesNotThrow(() -> controller.onNavClick(eventWithRoute("SETTINGS")));
        }

        @Test @DisplayName("route unknown → default branch → không crash")
        void unknown_route_no_crash() {
            assertDoesNotThrow(() -> controller.onNavClick(eventWithRoute("UNKNOWN_ROUTE")));
        }
    }

    // ─── onLogoClick / onFavoriteClick / onNotificationClick / onProfileClick ───

    @Nested @DisplayName("FXML navigation clicks")
    class FxmlNavClicks {

        @Test @DisplayName("onLogoClick → không crash")
        void logo_click_no_crash() {
            assertDoesNotThrow(() -> controller.onLogoClick());
        }

        @Test @DisplayName("onFavoriteClick → không crash")
        void favorite_click_no_crash() {
            assertDoesNotThrow(() -> controller.onFavoriteClick());
        }

        @Test @DisplayName("onNotificationClick → không crash")
        void notification_click_no_crash() {
            assertDoesNotThrow(() -> controller.onNotificationClick());
        }

        @Test @DisplayName("onProfileClick → không crash")
        void profile_click_no_crash() {
            assertDoesNotThrow(() -> controller.onProfileClick());
        }

        @Test @DisplayName("onLogoClick → setActiveNav(btnHome) → btnHome nhận nav-item-active")
        void logo_click_sets_home_active() {
            controller.onLogoClick();
            assertTrue(controller.btnHome.getStyleClass().contains("nav-item-active"));
        }
    }

    // ─── updateFavBadge / updateNotiBadge – edge ───

    @Nested @DisplayName("badge – edge values")
    class BadgeEdge {

        @Test @DisplayName("favBadge count = -1 → hidden (âm coi như 0)")
        void negative_fav_hidden() {
            controller.updateFavBadge(-1);
            assertFalse(controller.lblFavCount.isVisible());
        }

        @Test @DisplayName("notiBadge count = 99 → visible, text '99'")
        void high_noti_count_visible() {
            controller.updateNotiBadge(99);
            assertTrue(controller.lblNotiCount.isVisible());
            assertEquals("99", controller.lblNotiCount.getText());
        }

        @Test @DisplayName("favBadge count = 1 → visible")
        void one_fav_visible() {
            controller.updateFavBadge(1);
            assertTrue(controller.lblFavCount.isVisible());
            assertEquals("1", controller.lblFavCount.getText());
        }
    }

    // ─── cleanupResources – scheduler ───

    @Nested @DisplayName("cleanupResources")
    class CleanupResources {

        @Test @DisplayName("scheduler hợp lệ → shutdown, set null")
        void shuts_down_scheduler() {
            controller.timerScheduler = Executors.newSingleThreadScheduledExecutor();
            controller.cleanupResources();
            assertNull(controller.timerScheduler);
        }

        @Test @DisplayName("gọi 2 lần → không crash (idempotent)")
        void idempotent_no_crash() {
            controller.timerScheduler = Executors.newSingleThreadScheduledExecutor();
            assertDoesNotThrow(() -> {
                controller.cleanupResources();
                controller.cleanupResources();
            });
        }
    }
}