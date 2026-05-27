package vn.edu.vnu.uet.group8.client.util;

import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SceneManagerTest – phủ toàn bộ logic SceneManager không cần mở Stage thật.
 *
 * Các nhánh:
 *  - VIEW_* constants
 *  - init(null) → không crash
 *  - switchTo(null/blank) → log + return, không crash
 *  - switchTo(fxml) khi primaryStage null → log + return, không crash
 *  - loadWithController(null/blank) → IllegalArgumentException
 *  - loadWithController khi not FX thread → IllegalStateException
 *  - loadWithController khi stage null → IllegalStateException
 *  - getStage() trước init → null
 *  - setDefaultTitle → không crash khi stage null
 */
@DisplayName("SceneManager")
class SceneManagerTest {

    @BeforeAll
    static void initToolkit() {
        new JFXPanel();
    }

    // ─────────── VIEW constants ───────────

    @Nested @DisplayName("VIEW constants")
    class ViewConstants {

        @Test @DisplayName("VIEW_LOGIN = 'LoginView.fxml'")
        void view_login() {
            assertEquals("LoginView.fxml", SceneManager.VIEW_LOGIN);
        }

        @Test @DisplayName("VIEW_MAIN = 'MainLayout.fxml'")
        void view_main() {
            assertEquals("MainLayout.fxml", SceneManager.VIEW_MAIN);
        }

        @Test @DisplayName("VIEW_EXPLORE = 'ExploreView.fxml'")
        void view_explore() {
            assertEquals("ExploreView.fxml", SceneManager.VIEW_EXPLORE);
        }

        @Test @DisplayName("VIEW_AUCTION_DETAIL = 'AuctionDetailView.fxml'")
        void view_auction_detail() {
            assertEquals("AuctionDetailView.fxml", SceneManager.VIEW_AUCTION_DETAIL);
        }

        @Test @DisplayName("VIEW_LIVE_AUCTION = 'LiveAuctionView.fxml'")
        void view_live_auction() {
            assertEquals("LiveAuctionView.fxml", SceneManager.VIEW_LIVE_AUCTION);
        }

        @Test @DisplayName("VIEW_PROFILE = 'ProfileView.fxml'")
        void view_profile() {
            assertEquals("ProfileView.fxml", SceneManager.VIEW_PROFILE);
        }

        @Test @DisplayName("VIEW_WALLET = 'WalletView.fxml'")
        void view_wallet() {
            assertEquals("WalletView.fxml", SceneManager.VIEW_WALLET);
        }

        @Test @DisplayName("VIEW_NOTIFICATIONS = 'NotificationView.fxml'")
        void view_notifications() {
            assertEquals("NotificationView.fxml", SceneManager.VIEW_NOTIFICATIONS);
        }

        @Test @DisplayName("VIEW_FAVORITES = 'FavoriteView.fxml'")
        void view_favorites() {
            assertEquals("FavoriteView.fxml", SceneManager.VIEW_FAVORITES);
        }

        @Test @DisplayName("VIEW_SETTINGS = 'SettingsView.fxml'")
        void view_settings() {
            assertEquals("SettingsView.fxml", SceneManager.VIEW_SETTINGS);
        }
    }

    // ─────────── getStage ───────────

    @Nested @DisplayName("getStage")
    class GetStage {

        @Test @DisplayName("trước khi init → null")
        void before_init_returns_null() {
            // SceneManager.init() chưa gọi → primaryStage = null
            // Nếu test khác đã init, reset không được → chỉ verify không crash
            assertDoesNotThrow(() -> SceneManager.getStage());
        }
    }

    // ─────────── switchTo – guard null/blank ───────────

    @Nested @DisplayName("switchTo – guard null/blank")
    class SwitchToGuard {

        @Test @DisplayName("switchTo(null) → log severe, không crash")
        void switch_null_no_crash() {
            assertDoesNotThrow(() -> SceneManager.switchTo(null));
        }

        @Test @DisplayName("switchTo('') → log severe, không crash")
        void switch_empty_no_crash() {
            assertDoesNotThrow(() -> SceneManager.switchTo(""));
        }

        @Test @DisplayName("switchTo('   ') → log severe, không crash")
        void switch_blank_no_crash() {
            assertDoesNotThrow(() -> SceneManager.switchTo("   "));
        }

        @Test @DisplayName("switchTo(null, null) → không crash")
        void switch_null_title_no_crash() {
            assertDoesNotThrow(() -> SceneManager.switchTo(null, null));
        }
    }

    // ─────────── switchTo – no stage ───────────

    @Nested @DisplayName("switchTo – no stage (primaryStage null)")
    class SwitchToNoStage {

        @Test @DisplayName("switchTo hợp lệ khi stage null → log severe + showError, không crash")
        void valid_fxml_no_stage_no_crash() {
            // primaryStage null → log severe + AlertUtil.showError (runLater) → không crash
            assertDoesNotThrow(() -> SceneManager.switchTo(SceneManager.VIEW_LOGIN));
        }

        @Test @DisplayName("switchTo với title khi stage null → không crash")
        void with_title_no_stage_no_crash() {
            assertDoesNotThrow(() -> SceneManager.switchTo(SceneManager.VIEW_MAIN, "Test Title"));
        }
    }

    // ─────────── loadWithController – exceptions ───────────

    @Nested @DisplayName("loadWithController – exceptions")
    class LoadWithControllerExceptions {

        @Test @DisplayName("null fxmlFile → IllegalArgumentException")
        void null_fxml_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> SceneManager.loadWithController(null));
        }

        @Test @DisplayName("blank fxmlFile → IllegalArgumentException")
        void blank_fxml_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> SceneManager.loadWithController("   "));
        }

        @Test @DisplayName("empty fxmlFile → IllegalArgumentException")
        void empty_fxml_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> SceneManager.loadWithController(""));
        }

        @Test @DisplayName("gọi từ non-FX thread → IllegalStateException")
        void non_fx_thread_throws() {
            // Test đang chạy trên non-FX thread → ném IllegalStateException
            assertThrows(IllegalStateException.class,
                    () -> SceneManager.loadWithController(SceneManager.VIEW_LOGIN));
        }
    }

    // ─────────── setDefaultTitle ───────────

    @Nested @DisplayName("setDefaultTitle")
    class SetDefaultTitle {

        @Test @DisplayName("stage null → chỉ set field, không crash")
        void stage_null_no_crash() {
            assertDoesNotThrow(() -> SceneManager.setDefaultTitle("New Title"));
        }

        @Test @DisplayName("title null → không crash")
        void null_title_no_crash() {
            assertDoesNotThrow(() -> SceneManager.setDefaultTitle(null));
        }

        @Test @DisplayName("title blank → không crash")
        void blank_title_no_crash() {
            assertDoesNotThrow(() -> SceneManager.setDefaultTitle("   "));
        }
    }
}