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
@DisplayName("AdminUserListController – filter logic")
class AdminUserListControllerTest extends FxTestBase {

    private AdminUserListController controller;

    @BeforeEach
    void setup() {
        SessionManager.clearSession();
        controller = new AdminUserListController();
        controller.userTable      = new TableView<>();
        controller.colId          = new TableColumn<>();
        controller.colUsername    = new TableColumn<>();
        controller.colEmail       = new TableColumn<>();
        controller.colRole        = new TableColumn<>();
        controller.colStatus      = new TableColumn<>();
        controller.colAction      = null; // không test FX cell
        controller.tfSearch       = new TextField();
        controller.cbFilterRole   = new ComboBox<>();
        controller.cbFilterStatus = new ComboBox<>();

        // Seed data thủ công
        controller.allUsers.setAll(makeUsers());
    }

    private List<UserAdminDTO> makeUsers() {
        UserAdminDTO alice = new UserAdminDTO(1, "alice", "alice@x.com", "MEMBER", "ACTIVE");
        UserAdminDTO bob   = new UserAdminDTO(2, "bob",   "bob@x.com",   "MEMBER", "BANNED");
        UserAdminDTO carol = new UserAdminDTO(3, "carol", "carol@x.com", "ADMIN",  "ACTIVE");
        return List.of(alice, bob, carol);
    }

    @Nested @DisplayName("matchSearch")
    class MatchSearch {

        @Test @DisplayName("search rỗng → match tất cả")
        void empty_search_matches_all() {
            for (var u : makeUsers()) {
                assertTrue(controller.matchSearch(u, ""));
            }
        }

        @Test @DisplayName("search 'alice' → match alice, không match bob")
        void search_username() {
            var alice = makeUsers().get(0);
            var bob   = makeUsers().get(1);
            assertTrue(controller.matchSearch(alice, "alice"));
            assertFalse(controller.matchSearch(bob, "alice"));
        }

        @Test @DisplayName("search by email partial → match")
        void search_email_partial() {
            var alice = makeUsers().get(0);
            assertTrue(controller.matchSearch(alice, "alice@"));
        }

        @Test @DisplayName("search không phân biệt hoa thường")
        void search_case_insensitive() {
            var alice = makeUsers().get(0);
            assertTrue(controller.matchSearch(alice, "ALICE"));
        }

        @Test @DisplayName("username null → không crash")
        void null_username_no_crash() {
            UserAdminDTO noName = new UserAdminDTO(99, null, null, "MEMBER", "ACTIVE");
            assertDoesNotThrow(() -> controller.matchSearch(noName, "alice"));
        }
    }

    @Nested @DisplayName("matchRole")
    class MatchRole {

        @Test @DisplayName("null filter → match tất cả")
        void null_filter_matches_all() {
            for (var u : makeUsers()) assertTrue(controller.matchRole(u, null));
        }

        @Test @DisplayName("'Tất cả vai trò' → match tất cả")
        void all_roles_filter() {
            for (var u : makeUsers()) assertTrue(controller.matchRole(u, "Tất cả vai trò"));
        }

        @Test @DisplayName("'ADMIN' filter → chỉ match carol (isAdmin=true)")
        void admin_filter_matches_admin() {
            var alice = makeUsers().get(0);
            var carol = makeUsers().get(2);
            assertFalse(controller.matchRole(alice, "ADMIN"));
            assertTrue(controller.matchRole(carol, "ADMIN"));
        }

        @Test @DisplayName("'MEMBER' filter → match alice, bob không match carol")
        void member_filter() {
            var alice = makeUsers().get(0);
            var carol = makeUsers().get(2);
            assertTrue(controller.matchRole(alice, "MEMBER"));
            assertFalse(controller.matchRole(carol, "MEMBER"));
        }
    }

    @Nested @DisplayName("matchStatus")
    class MatchStatus {

        @Test @DisplayName("null filter → match tất cả")
        void null_filter() {
            for (var u : makeUsers()) assertTrue(controller.matchStatus(u, null));
        }

        @Test @DisplayName("'Tất cả trạng thái' → match tất cả")
        void all_status_filter() {
            for (var u : makeUsers()) assertTrue(controller.matchStatus(u, "Tất cả trạng thái"));
        }

        @Test @DisplayName("'ACTIVE' → chỉ match ACTIVE users")
        void active_filter() {
            var alice = makeUsers().get(0); // ACTIVE
            var bob   = makeUsers().get(1); // BANNED
            assertTrue(controller.matchStatus(alice, "ACTIVE"));
            assertFalse(controller.matchStatus(bob, "ACTIVE"));
        }

        @Test @DisplayName("'BANNED' → chỉ match bob")
        void banned_filter() {
            var alice = makeUsers().get(0);
            var bob   = makeUsers().get(1);
            assertFalse(controller.matchStatus(alice, "BANNED"));
            assertTrue(controller.matchStatus(bob, "BANNED"));
        }
    }

    @Nested @DisplayName("suspendSelected – không có selection")
    class SuspendSelected {

        @Test @DisplayName("không có item được select → không crash")
        void no_selection_no_crash() {
            // userTable không có item được select → getSelectedItem() = null
            // showWarning sẽ được gọi (AlertUtil.showWarning không crash khi không có FX stage)
            assertDoesNotThrow(() -> controller.suspendSelected());
        }
    }

    @Nested @DisplayName("banSelected – quyền super admin")
    class BanSelected {

        @Test @DisplayName("không phải super admin → không crash (showError được gọi)")
        void not_super_admin_no_crash() {
            SessionManager.setSession("t", 1, "u", "f", "MEMBER");
            assertDoesNotThrow(() -> controller.banSelected());
        }
    }
}


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