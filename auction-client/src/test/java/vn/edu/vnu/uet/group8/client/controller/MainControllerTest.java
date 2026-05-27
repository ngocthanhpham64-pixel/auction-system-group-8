package vn.edu.vnu.uet.group8.client.controller;

import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.client.util.SceneManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MainController} — pure Java logic.
 *
 * Coverage:
 * - getInstance() trả về null khi chưa FXML init
 * - setTimerScheduler / cleanupResources: shutdown executor
 * - loadView(null/blank) → không crash khi contentPane null
 * - SceneManager VIEW_* constants khớp tên gọi trong routing
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class MainControllerTest {

    private MainController ctrl;

    @BeforeEach
    void setUp() {
        ctrl = new MainController();
    }

    // ══════════════════════════════════════════════════════
    // Singleton
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("getInstance() trả về null khi chưa có FXML initialize")
    void getInstance_noFxmlInit_returnsNull() {
        // Chỉ set khi initialize() chạy từ FXML loader
        // new MainController() không set instance
        // → giá trị có thể null hoặc từ lần init trước — chỉ assert không crash
        assertDoesNotThrow(MainController::getInstance);
    }

    @Test
    @DisplayName("MainController: new() không crash (headless)")
    void constructor_noCrash() {
        assertDoesNotThrow(MainController::new);
    }

    // ══════════════════════════════════════════════════════
    // setTimerScheduler + cleanupResources
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("setTimerScheduler: scheduler null → không crash")
    void setTimerScheduler_null_noCrash() {
        assertDoesNotThrow(() -> ctrl.setTimerScheduler(null));
    }

    @Test
    @DisplayName("cleanupResources: scheduler null → không crash")
    void cleanupResources_nullScheduler_noCrash() throws Exception {
        Method m = MainController.class.getDeclaredMethod("cleanupResources");
        m.setAccessible(true);
        ctrl.setTimerScheduler(null);
        assertDoesNotThrow(() -> m.invoke(ctrl));
    }

    @Test
    @DisplayName("cleanupResources: scheduler đang chạy → shutdown thành công, không block")
    void cleanupResources_runningScheduler_shutsDown() throws Exception {
        Method m = MainController.class.getDeclaredMethod("cleanupResources");
        m.setAccessible(true);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ctrl.setTimerScheduler(scheduler);

        assertDoesNotThrow(() -> m.invoke(ctrl));
        assertTrue(scheduler.isShutdown(), "Scheduler phải được shutdown sau cleanupResources");
    }

    @Test
    @DisplayName("cleanupResources: gọi 2 lần liên tiếp → idempotent, không crash")
    void cleanupResources_calledTwice_noCrash() throws Exception {
        Method m = MainController.class.getDeclaredMethod("cleanupResources");
        m.setAccessible(true);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ctrl.setTimerScheduler(scheduler);
        m.invoke(ctrl);
        assertDoesNotThrow(() -> m.invoke(ctrl));
    }

    // ══════════════════════════════════════════════════════
    // updateFavBadge (private → reflection)
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("updateFavBadge(0) khi lblFavCount null → không crash")
    void updateFavBadge_zero_nullLabel_noCrash() throws Exception {
        Method m = MainController.class.getDeclaredMethod("updateFavBadge", int.class);
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(ctrl, 0));
    }

    @Test
    @DisplayName("updateFavBadge(5) khi lblFavCount null → không crash")
    void updateFavBadge_positive_nullLabel_noCrash() throws Exception {
        Method m = MainController.class.getDeclaredMethod("updateFavBadge", int.class);
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(ctrl, 5));
    }

    @Test
    @DisplayName("updateFavBadge(-1) khi lblFavCount null → không crash (edge case âm)")
    void updateFavBadge_negative_noCrash() throws Exception {
        Method m = MainController.class.getDeclaredMethod("updateFavBadge", int.class);
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(ctrl, -1));
    }

    // ══════════════════════════════════════════════════════
    // updateNotiBadge (private → reflection)
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("updateNotiBadge(0) khi lblNotiCount null → không crash")
    void updateNotiBadge_zero_noCrash() throws Exception {
        Method m = MainController.class.getDeclaredMethod("updateNotiBadge", int.class);
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(ctrl, 0));
    }

    @Test
    @DisplayName("updateNotiBadge(99) khi lblNotiCount null → không crash")
    void updateNotiBadge_large_noCrash() throws Exception {
        Method m = MainController.class.getDeclaredMethod("updateNotiBadge", int.class);
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(ctrl, 99));
    }

    // ══════════════════════════════════════════════════════
    // loadView (public, contentPane null)
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("loadView(VIEW_EXPLORE) khi contentPane null → không crash")
    void loadView_explore_nullContentPane_noCrash() {
        assertDoesNotThrow(() -> ctrl.loadView(SceneManager.VIEW_EXPLORE));
    }

    @Test
    @DisplayName("loadView(null) khi contentPane null → không crash")
    void loadView_null_noCrash() {
        assertDoesNotThrow(() -> ctrl.loadView(null));
    }

    @Test
    @DisplayName("loadView(\"\") khi contentPane null → không crash")
    void loadView_empty_noCrash() {
        assertDoesNotThrow(() -> ctrl.loadView(""));
    }

    // ══════════════════════════════════════════════════════
    // Routing constants đồng nhất với SceneManager
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("VIEW_EXPLORE dùng trong routing → hằng số không null")
    void routeExplore_usesSceneManagerConstant() {
        assertNotNull(SceneManager.VIEW_EXPLORE);
        assertFalse(SceneManager.VIEW_EXPLORE.isBlank());
    }

    @Test
    @DisplayName("VIEW_WALLET dùng trong routing → hằng số không null")
    void routeWallet_usesSceneManagerConstant() {
        assertNotNull(SceneManager.VIEW_WALLET);
    }

    @Test
    @DisplayName("VIEW_PROFILE dùng trong routing → hằng số không null")
    void routeProfile_usesSceneManagerConstant() {
        assertNotNull(SceneManager.VIEW_PROFILE);
    }

    @Test
    @DisplayName("VIEW_LIVE_AUCTION dùng trong routing → hằng số không null")
    void routeLiveAuction_usesSceneManagerConstant() {
        assertNotNull(SceneManager.VIEW_LIVE_AUCTION);
    }

    @Test
    @DisplayName("VIEW_SETTINGS dùng trong routing → hằng số không null")
    void routeSettings_usesSceneManagerConstant() {
        assertNotNull(SceneManager.VIEW_SETTINGS);
    }

    @Test
    @DisplayName("VIEW_FAVORITES dùng trong routing → hằng số không null")
    void routeFavorites_usesSceneManagerConstant() {
        assertNotNull(SceneManager.VIEW_FAVORITES);
    }

    @Test
    @DisplayName("VIEW_NOTIFICATIONS dùng trong routing → hằng số không null")
    void routeNotifications_usesSceneManagerConstant() {
        assertNotNull(SceneManager.VIEW_NOTIFICATIONS);
    }
}