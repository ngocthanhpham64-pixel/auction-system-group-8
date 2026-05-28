package vn.edu.vnu.uet.group8.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.model.UserAdminDTO;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// =========================================================================
//  AdminUserListController – filter logic (pure Java, không cần server)
// =========================================================================
// =========================================================================
//  AdminAuctionListController – filter logic
// =========================================================================
@DisplayName("AdminAuctionListController – filter logic")
class AdminAuctionListControllerTest extends FxTestBase {

    private AdminAuctionListController controller;

    private AuctionItemDTO makeItem(int id, String title, SessionStatus status) {
        return AuctionItemDTO.of(
                id,
                title,
                "desc",
                ItemCategory.ELECTRONICS,
                null,
                status,
                new BigDecimal("1000000"),
                null,
                "seller",
                null,
                null,
                0,
                null
        );
    }

    @BeforeEach
    void setup() {
        controller = new AdminAuctionListController();
        controller.auctionTable = new TableView<>();
        controller.colId        = new TableColumn<>();
        controller.colTitle     = new TableColumn<>();
        controller.colPrice     = new TableColumn<>();
        controller.colStatus    = new TableColumn<>();
        controller.colEndTime   = new TableColumn<>();
        controller.colAction    = null;
        controller.tfSearch     = new TextField();
        controller.cbFilterStatus = new ComboBox<>();
    }

    @Nested @DisplayName("matchSearch")
    class MatchSearch {

        @Test @DisplayName("rỗng → match")
        void empty_search_matches() {
            assertTrue(controller.matchSearch(makeItem(1, "iPhone 15", SessionStatus.ACTIVE), ""));
        }

        @Test @DisplayName("title chứa keyword → match")
        void title_contains_keyword() {
            assertTrue(controller.matchSearch(makeItem(1, "iPhone 15", SessionStatus.ACTIVE), "iphone"));
        }

        @Test @DisplayName("title không chứa keyword → no match")
        void title_no_match() {
            assertFalse(controller.matchSearch(makeItem(1, "iPhone 15", SessionStatus.ACTIVE), "samsung"));
        }

        @Test @DisplayName("title null → không crash, trả false khi có keyword")
        void null_title_no_crash() {
            AuctionItemDTO item = makeItem(1, null, SessionStatus.ACTIVE);
            assertFalse(controller.matchSearch(item, "keyword"));
        }

        @Test @DisplayName("keyword case-insensitive")
        void case_insensitive() {
            assertTrue(controller.matchSearch(makeItem(1, "iPhone 15", SessionStatus.ACTIVE), "IPHONE"));
        }
    }

    @Nested @DisplayName("matchStatus")
    class MatchStatus {

        @Test @DisplayName("null filter → match")
        void null_filter_matches() {
            assertTrue(controller.matchStatus(makeItem(1, "X", SessionStatus.ACTIVE), null));
        }

        @Test @DisplayName("'Tất cả' → match")
        void all_filter_matches() {
            assertTrue(controller.matchStatus(makeItem(1, "X", SessionStatus.ACTIVE), "Tất cả trạng thái"));
        }

        @Test @DisplayName("'ACTIVE' → match ACTIVE")
        void active_matches() {
            assertTrue(controller.matchStatus(makeItem(1, "X", SessionStatus.ACTIVE), "ACTIVE"));
        }

        @Test @DisplayName("'ACTIVE' → no match CANCELLED")
        void active_no_match_cancelled() {
            assertFalse(controller.matchStatus(makeItem(1, "X", SessionStatus.CANCELLED), "ACTIVE"));
        }

        @Test @DisplayName("status null → no match với non-null filter")
        void null_status_no_match() {
            AuctionItemDTO item = makeItem(1, "X", null);
            assertFalse(controller.matchStatus(item, "ACTIVE"));
        }
    }

    @Nested @DisplayName("cancelSelected – no selection")
    class CancelSelected {

        @Test @DisplayName("không có selection → không crash")
        void no_selection_no_crash() {
            assertDoesNotThrow(() -> controller.cancelSelected());
        }
    }
}


// =========================================================================
//  AdminDashboardController – display logic
// =========================================================================
@DisplayName("AdminDashboardController")
class AdminDashboardControllerTest extends FxTestBase {

    private AdminDashboardController controller;

    @BeforeEach
    void setup() {
        controller = new AdminDashboardController();
        controller.lblActiveAuctions = new Label();
        controller.lblTotalUsers     = new Label();
        controller.lblRevenue        = new Label();
        controller.lblTotalBids      = new Label();
    }

    @Nested @DisplayName("showLoading (via setText)")
    class ShowLoading {

        @Test @DisplayName("setText null-safe với label hợp lệ")
        void set_text_valid_label() {
            controller.setText(controller.lblActiveAuctions, "100");
            assertEquals("100", controller.lblActiveAuctions.getText());
        }

        @Test @DisplayName("setText null label → không throw")
        void set_text_null_label() {
            assertDoesNotThrow(() -> controller.setText(null, "value"));
        }
    }

    @Nested @DisplayName("displayStats")
    class DisplayStats {

        @Test @DisplayName("stats hợp lệ → label được update")
        void valid_stats_updates_labels() {
            var stats = new vn.edu.vnu.uet.group8.common.dto.model.AdminStatsDTO(
                    10, 200, new BigDecimal("5000000"), 500
            );
            controller.displayStats(stats);
            assertEquals("10", controller.lblActiveAuctions.getText());
            assertEquals("200", controller.lblTotalUsers.getText());
            assertEquals("500", controller.lblTotalBids.getText());
        }

        @Test @DisplayName("stats với totalRevenue null → không crash")
        void null_revenue_no_crash() {
            var stats = new vn.edu.vnu.uet.group8.common.dto.model.AdminStatsDTO(
                    0, 0, null, 0
            );
            assertDoesNotThrow(() -> controller.displayStats(stats));
        }

        @Test @DisplayName("stats với lblRevenue null → không crash")
        void null_label_revenue_no_crash() {
            controller.lblRevenue = null;
            var stats = new vn.edu.vnu.uet.group8.common.dto.model.AdminStatsDTO(
                    5, 100, new BigDecimal("1000000"), 50
            );
            assertDoesNotThrow(() -> controller.displayStats(stats));
        }
    }
}