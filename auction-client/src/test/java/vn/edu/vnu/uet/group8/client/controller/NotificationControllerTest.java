package vn.edu.vnu.uet.group8.client.controller;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.common.dto.model.NotificationDTO;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NotificationController")
class NotificationControllerTest extends FxTestBase {

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
                .id(id)
                .title("Thông báo " + id)
                .isRead(read)
                .type(type)
                .createdAt(Instant.now())
                .build();
    }

    // ─────────── matchFilter ───────────

    @Nested @DisplayName("matchFilter")
    class MatchFilter {

        @Test @DisplayName("filter 'all' → match tất cả")
        void all_matches_all() {
            controller.currentFilter = "all";
            assertTrue(controller.matchFilter(notif(1, true, "AUCTION")));
            assertTrue(controller.matchFilter(notif(2, false, "SYSTEM")));
        }

        @Test @DisplayName("filter 'unread' → chỉ match chưa đọc")
        void unread_matches_unread_only() {
            controller.currentFilter = "unread";
            assertTrue(controller.matchFilter(notif(1, false, "AUCTION")));
            assertFalse(controller.matchFilter(notif(2, true, "AUCTION")));
        }

        @Test @DisplayName("filter 'auction' → chỉ match type AUCTION")
        void auction_matches_auction_type() {
            controller.currentFilter = "auction";
            assertTrue(controller.matchFilter(notif(1, false, "AUCTION")));
            assertFalse(controller.matchFilter(notif(2, false, "SYSTEM")));
        }

        @Test @DisplayName("filter 'auction' case-insensitive")
        void auction_case_insensitive() {
            controller.currentFilter = "auction";
            assertTrue(controller.matchFilter(notif(1, false, "auction")));
        }

        @Test @DisplayName("filter 'system' → chỉ match type SYSTEM")
        void system_matches_system_type() {
            controller.currentFilter = "system";
            assertTrue(controller.matchFilter(notif(1, true, "SYSTEM")));
            assertFalse(controller.matchFilter(notif(2, true, "AUCTION")));
        }

        @Test @DisplayName("filter 'system' case-insensitive")
        void system_case_insensitive() {
            controller.currentFilter = "system";
            assertTrue(controller.matchFilter(notif(1, false, "system")));
        }
    }

    // ─────────── updateNewCount ───────────

    @Nested @DisplayName("updateNewCount")
    class UpdateNewCount {

        @Test @DisplayName("count > 0 → visible=true, text đúng")
        void positive_count_visible() {
            controller.updateNewCount(5);
            assertTrue(controller.lblNewCount.isVisible());
            assertTrue(controller.lblNewCount.isManaged());
            assertEquals("5 moi", controller.lblNewCount.getText());
        }

        @Test @DisplayName("count = 0 → visible=false")
        void zero_count_hidden() {
            controller.updateNewCount(0);
            assertFalse(controller.lblNewCount.isVisible());
            assertFalse(controller.lblNewCount.isManaged());
        }

        @Test @DisplayName("lblNewCount null → không crash")
        void null_label_no_crash() {
            controller.lblNewCount = null;
            assertDoesNotThrow(() -> controller.updateNewCount(3));
        }
    }

    // ─────────── render ───────────

    @Nested @DisplayName("render")
    class Render {

        @Test @DisplayName("null list → renderEmpty (1 label)")
        void null_list_renders_empty() {
            controller.render(null);
            assertEquals(1, controller.notificationList.getChildren().size());
        }

        @Test @DisplayName("empty list → renderEmpty")
        void empty_list_renders_empty() {
            controller.render(List.of());
            assertEquals(1, controller.notificationList.getChildren().size());
        }

        @Test @DisplayName("có notif → render các rows")
        void has_notifs_renders_rows() {
            controller.currentFilter = "all";
            controller.render(List.of(
                    notif(1, false, "AUCTION"),
                    notif(2, true, "SYSTEM")
            ));
            assertEquals(2, controller.notificationList.getChildren().size());
        }

        @Test @DisplayName("filter 'unread' + tất cả đã đọc → hiển thị empty label")
        void all_read_with_unread_filter_shows_empty() {
            controller.currentFilter = "unread";
            controller.render(List.of(
                    notif(1, true, "AUCTION"),
                    notif(2, true, "SYSTEM")
            ));
            assertEquals(1, controller.notificationList.getChildren().size());
            assertTrue(controller.notificationList.getChildren().get(0) instanceof Label);
        }

        @Test @DisplayName("notificationList null → không crash")
        void null_list_no_crash() {
            controller.notificationList = null;
            assertDoesNotThrow(() -> controller.render(List.of(notif(1, false, "AUCTION"))));
        }
    }

    // ─────────── buildItem ───────────

    @Nested @DisplayName("buildItem")
    class BuildItem {

        @Test @DisplayName("notif chưa đọc → btnRead enabled")
        void unread_notif_btn_enabled() {
            HBox row = controller.buildItem(notif(1, false, "AUCTION"));
            assertNotNull(row);
            var buttons = row.getChildren().stream()
                    .filter(n -> n instanceof Button)
                    .map(n -> (Button) n)
                    .toList();
            assertFalse(buttons.isEmpty());
            // Nút đầu tiên (Danh dau da doc) phải enabled
            assertFalse(buttons.get(0).isDisable());
        }

        @Test @DisplayName("notif đã đọc → btnRead disabled")
        void read_notif_btn_disabled() {
            HBox row = controller.buildItem(notif(1, true, "SYSTEM"));
            assertNotNull(row);
            var buttons = row.getChildren().stream()
                    .filter(n -> n instanceof Button)
                    .map(n -> (Button) n)
                    .toList();
            assertTrue(buttons.get(0).isDisable());
        }

        @Test @DisplayName("title null → fallback 'Thong bao'")
        void null_title_uses_fallback() {
            NotificationDTO n = NotificationDTO.builder()
                    .id(1).title(null).isRead(false).type("AUCTION").build();
            HBox row = controller.buildItem(n);
            assertNotNull(row);
        }
    }

    // ─────────── tab switching ───────────

    @Nested @DisplayName("tab switching")
    class TabSwitching {

        @Test @DisplayName("onTabAll → currentFilter = 'all'")
        void tab_all() {
            controller.onTabAll();
            assertEquals("all", controller.currentFilter);
            assertSame(controller.btnTabAll, controller.activeTab);
        }

        @Test @DisplayName("onTabUnread → currentFilter = 'unread'")
        void tab_unread() {
            controller.onTabUnread();
            assertEquals("unread", controller.currentFilter);
        }

        @Test @DisplayName("onTabAuction → currentFilter = 'auction'")
        void tab_auction() {
            controller.onTabAuction();
            assertEquals("auction", controller.currentFilter);
        }

        @Test @DisplayName("onTabSystem → currentFilter = 'system'")
        void tab_system() {
            controller.onTabSystem();
            assertEquals("system", controller.currentFilter);
        }
    }
}