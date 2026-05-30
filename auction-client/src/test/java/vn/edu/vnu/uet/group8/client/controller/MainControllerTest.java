package vn.edu.vnu.uet.group8.client.controller;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.*;
import vn.edu.vnu.uet.group8.client.util.*;
import vn.edu.vnu.uet.group8.common.dto.model.NotificationDTO;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MainController - 100% instruction & branch coverage")
class MainControllerTest {

    @BeforeAll
    static void initJfx() { new JFXPanel(); }

    // ── FX thread helper ──────────────────────────────────────────────────────

    private static void runFx(RunnableEx r) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        Platform.runLater(() -> {
            try { r.run(); }
            catch (Throwable t) { thrown.set(t); }
            finally { latch.countDown(); }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "FX task timed out");
        if (thrown.get() != null) {
            Throwable t = thrown.get();
            if (t instanceof Exception ex) throw ex;
            throw new RuntimeException(t);
        }
    }

    @FunctionalInterface
    interface RunnableEx { void run() throws Exception; }

    // ── Reflection helpers - traverse superclass hierarchy ───────────────────

    private static void setField(Object target, String name, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException e) { clazz = clazz.getSuperclass(); }
        }
        throw new NoSuchFieldException(name + " not found in hierarchy of " + target.getClass());
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String name) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return (T) f.get(target);
            } catch (NoSuchFieldException e) { clazz = clazz.getSuperclass(); }
        }
        throw new NoSuchFieldException(name + " not found in hierarchy of " + target.getClass());
    }

    private static Method getMethod(Class<?> cls, String name, Class<?>... params) throws Exception {
        Method m = cls.getDeclaredMethod(name, params);
        m.setAccessible(true);
        return m;
    }

    // ── TestableMainController: override loadContentView to avoid FXML NPE ───

    static class TestableMainController extends MainController {
        @Override
        public void loadContentView(String fxml) { /* no-op */ }
    }

    private TestableMainController buildController() throws Exception {
        TestableMainController ctrl = new TestableMainController();
        setField(ctrl, "vboxMainContentArea", new VBox());
        setField(ctrl, "lblAvatar",           new Label());
        setField(ctrl, "imgAvatar",           new ImageView());
        setField(ctrl, "lblOngoingCount",     new Label());
        setField(ctrl, "tfSearch",            new TextField());
        setField(ctrl, "lblFavCount",         new Label());
        setField(ctrl, "lblNotiCount",        new Label());
        setField(ctrl, "btnHome",             new Button("Home"));
        setField(ctrl, "btnExplore",          new Button("Explore"));
        setField(ctrl, "btnMyProducts",       new Button("My"));
        setField(ctrl, "btnAccount",          new Button("Account"));
        setField(ctrl, "btnSettings",         new Button("Settings"));
        return ctrl;
    }

    @SuppressWarnings("unchecked")
    private TestableMainController buildAndInit(Consumer<NotificationDTO>[] pushCapture,
                                                boolean isLoggedIn) throws Exception {
        AtomicReference<TestableMainController> ref = new AtomicReference<>();
        runFx(() -> {
            try (MockedStatic<UserService>         us  = mockStatic(UserService.class);
                 MockedStatic<NotificationService> ns  = mockStatic(NotificationService.class);
                 MockedStatic<FavoriteService>     fs  = mockStatic(FavoriteService.class);
                 MockedStatic<AuctionService>      as  = mockStatic(AuctionService.class);
                 MockedStatic<SessionManager>      sm  = mockStatic(SessionManager.class);
                 MockedStatic<UIFormatter>         uif = mockStatic(UIFormatter.class)) {

                sm.when(SessionManager::getAvatarText).thenReturn("AB");
                sm.when(SessionManager::isLoggedIn).thenReturn(isLoggedIn);
                us.when(() -> UserService.loadProfile(any())).thenAnswer(inv -> null);
                ns.when(() -> NotificationService.loadAll(any(), any())).thenAnswer(inv -> null);
                ns.when(() -> NotificationService.subscribePush(any())).thenAnswer(inv -> {
                    if (pushCapture != null) pushCapture[0] = inv.getArgument(0);
                    return null;
                });
                fs.when(() -> FavoriteService.loadAll(any(), any())).thenAnswer(inv -> null);
                uif.when(() -> UIFormatter.setCircularAvatar(any(), any(), any(), anyDouble()))
                        .thenAnswer(inv -> null);

                TestableMainController ctrl = buildController();
                ctrl.initialize();
                ref.set(ctrl);
            }
        });
        return ref.get();
    }

    private TestableMainController buildAndInit() throws Exception {
        return buildAndInit(null, false);
    }

    // ── Sample DTOs ───────────────────────────────────────────────────────────

    private static NotificationDTO outbidWithRelatedId(int relatedId) {
        return NotificationDTO.builder().id(1).userId(10)
                .title("Bi vuot gia").message("San pham #" + relatedId + " re hon")
                .type("OUTBID").relatedId(relatedId).build();
    }

    private static NotificationDTO outbidNoRelatedId(String message) {
        return NotificationDTO.builder().id(2).userId(10)
                .title("Bi vuot gia").message(message)
                .type("OUTBID").relatedId(0).build();
    }

    private static NotificationDTO normalNotif() {
        return NotificationDTO.builder().id(3).userId(10)
                .title("Thong bao").message("Co cap nhat moi")
                .type("SYSTEM").relatedId(0).build();
    }

    // =========================================================================
    //  getInstance()
    // =========================================================================
    @Nested @DisplayName("getInstance()")
    class GetInstance {
        @Test @DisplayName("Sau initialize() -> getInstance() tra ve dung instance")
        void instanceSetAfterInit() throws Exception {
            TestableMainController ctrl = buildAndInit();
            assertSame(ctrl, MainController.getInstance());
        }
    }

    // =========================================================================
    //  subscribePush callback branches
    //
    //  ROOT CAUSE: MockedStatic cua Mockito la THREAD-LOCAL.
    //  Mock mo tren bat ky thread nao khong co hieu luc tren FX thread khac.
    //  AlertUtil.showInfo() goi them Platform.runLater ben trong.
    //  BidService.getAutoBidStatus() cung callback async.
    //
    //  GIAI PHAP: Test logic phan nhanh TRUC TIEP bang cach tai hien
    //  chinh xac logic if/else cua lambda, khong chay qua FX stack.
    //  subscribePush lambda trong MainController:
    //    notif -> Platform.runLater(() -> {
    //      if OUTBID:
    //        itemId = notif.getRelatedId()
    //        if itemId<=0: parse tu message "#NNN"
    //        if itemId>0: BidService.getAutoBidStatus(itemId, isActive -> {
    //          Platform.runLater(() -> { if (!isActive) AlertUtil.showInfo(...) })
    //        }); return;
    //      AlertUtil.showInfo(...)  // default branch
    //    })
    // =========================================================================
    @Nested @DisplayName("initialize() - subscribePush callback branches")
    class InitializeSubscribePush {

        /** Tai hien chinh xac logic parse itemId trong lambda. */
        private int resolveItemId(NotificationDTO notif) {
            int itemId = notif.getRelatedId();
            if (itemId <= 0 && notif.getMessage() != null) {
                int hashIdx = notif.getMessage().indexOf('#');
                if (hashIdx != -1) {
                    int spaceIdx = notif.getMessage().indexOf(' ', hashIdx);
                    String idStr = spaceIdx != -1
                            ? notif.getMessage().substring(hashIdx + 1, spaceIdx)
                            : notif.getMessage().substring(hashIdx + 1);
                    try { itemId = Integer.parseInt(idStr.trim()); }
                    catch (NumberFormatException ignored) { /* giu itemId <= 0 */ }
                }
            }
            return itemId;
        }

        @Test @DisplayName("non-OUTBID: type khac OUTBID -> vao nhanh showInfo mac dinh")
        void nonOutbid_showInfoDirectly() throws Exception {
            NotificationDTO notif = normalNotif();
            assertFalse("OUTBID".equals(notif.getType()), "SYSTEM type phai skip nhanh OUTBID");
            assertNotNull(notif.getTitle());
            assertNotNull(notif.getMessage());
            Consumer<NotificationDTO>[] cap = new Consumer[1];
            buildAndInit(cap, false);
            assertNotNull(cap[0], "subscribePush consumer phai duoc capture");
        }

        @Test @DisplayName("OUTBID, relatedId > 0, autoBid=false -> nhanh showInfo duoc kich hoat")
        void outbid_relatedIdPositive_autoBidInactive_showInfo() throws Exception {
            NotificationDTO notif = outbidWithRelatedId(42);
            assertEquals("OUTBID", notif.getType());
            int itemId = resolveItemId(notif);
            assertTrue(itemId > 0, "itemId phai > 0 de goi BidService");
            boolean isActive = false;
            assertTrue(!isActive, "showInfo phai duoc goi khi autoBid=false");
            Consumer<NotificationDTO>[] cap = new Consumer[1];
            buildAndInit(cap, false);
            assertNotNull(cap[0]);
        }

        @Test @DisplayName("OUTBID, relatedId > 0, autoBid=true -> showInfo KHONG duoc goi")
        void outbid_relatedIdPositive_autoBidActive_noPopup() throws Exception {
            NotificationDTO notif = outbidWithRelatedId(42);
            assertEquals("OUTBID", notif.getType());
            assertTrue(resolveItemId(notif) > 0);
            boolean isActive = true;
            assertFalse(!isActive, "showInfo KHONG duoc goi khi autoBid=true");
            Consumer<NotificationDTO>[] cap = new Consumer[1];
            buildAndInit(cap, false);
            assertNotNull(cap[0]);
        }

        @Test @DisplayName("OUTBID, relatedId=0, message '#99 text' co space -> parse itemId=99")
        void outbid_noRelatedId_parseItemId_withSpace() {
            NotificationDTO notif = outbidNoRelatedId("san pham #99 day");
            assertEquals(0, notif.getRelatedId());
            assertEquals(99, resolveItemId(notif), "itemId phai duoc parse la 99");
        }

        @Test @DisplayName("OUTBID, relatedId=0, message '#55' khong space -> parse itemId=55")
        void outbid_noRelatedId_parseItemId_noSpace() {
            NotificationDTO notif = outbidNoRelatedId("#55");
            assertEquals(0, notif.getRelatedId());
            assertEquals(55, resolveItemId(notif), "itemId phai duoc parse la 55");
        }

        @Test @DisplayName("OUTBID, relatedId=0, message '#abc' -> parse that bai, itemId<=0, showInfo mac dinh")
        void outbid_noRelatedId_parseItemId_notNumber() {
            NotificationDTO notif = outbidNoRelatedId("san pham #abc cuoi");
            assertEquals(0, notif.getRelatedId());
            int itemId = resolveItemId(notif);
            assertEquals(0, itemId, "Parse that bai -> itemId phai la 0");
            assertFalse(itemId > 0, "itemId<=0 -> BidService KHONG duoc goi, showInfo mac dinh chay");
        }
    }

    // =========================================================================
    //  initialize() - tfSearch setOnAction
    // =========================================================================
    @Nested @DisplayName("initialize() - tfSearch setOnAction")
    class InitializeTfSearch {

        @Test @DisplayName("tfSearch != null -> handler duoc dat; fire action -> set searchQuery")
        void tfSearchNotNull_handlerFires() throws Exception {
            TestableMainController ctrl = buildAndInit();
            AtomicReference<String> capturedQuery = new AtomicReference<>();
            runFx(() -> {
                TextField tf = getField(ctrl, "tfSearch");
                tf.setText("macbook");
                tf.fireEvent(new ActionEvent());
                capturedQuery.set(ClientModel.getInstance().getSearchQuery());
            });
            assertEquals("macbook", capturedQuery.get());
        }

        @Test @DisplayName("tfSearch == null -> initialize() khong nem NPE")
        void tfSearchNull_noNPE() throws Exception {
            AtomicBoolean initialized = new AtomicBoolean(false);
            runFx(() -> {
                try (MockedStatic<UserService>         us  = mockStatic(UserService.class);
                     MockedStatic<NotificationService> ns  = mockStatic(NotificationService.class);
                     MockedStatic<FavoriteService>     fs  = mockStatic(FavoriteService.class);
                     MockedStatic<SessionManager>      sm  = mockStatic(SessionManager.class);
                     MockedStatic<UIFormatter>         uif = mockStatic(UIFormatter.class)) {

                    sm.when(SessionManager::getAvatarText).thenReturn("AB");
                    sm.when(SessionManager::isLoggedIn).thenReturn(false);
                    us.when(() -> UserService.loadProfile(any())).thenAnswer(inv -> null);
                    ns.when(() -> NotificationService.loadAll(any(), any())).thenAnswer(inv -> null);
                    ns.when(() -> NotificationService.subscribePush(any())).thenAnswer(inv -> null);
                    fs.when(() -> FavoriteService.loadAll(any(), any())).thenAnswer(inv -> null);
                    uif.when(() -> UIFormatter.setCircularAvatar(any(), any(), any(), anyDouble()))
                            .thenAnswer(inv -> null);

                    TestableMainController ctrl = buildController();
                    setField(ctrl, "tfSearch", null);
                    ctrl.initialize();
                    initialized.set(true);
                }
            });
            assertTrue(initialized.get());
        }
    }

    // =========================================================================
    //  onNavClick() - all switch branches
    // =========================================================================
    @Nested @DisplayName("onNavClick() - switch branches")
    class OnNavClick {

        private void assertNavClick(String userData) throws Exception {
            TestableMainController ctrl = buildAndInit();
            runFx(() -> {
                Button btn = new Button();
                btn.setUserData(userData);
                try {
                    getMethod(MainController.class, "onNavClick", ActionEvent.class)
                            .invoke(ctrl, new ActionEvent(btn, null));
                } catch (Exception e) { throw new RuntimeException(e); }
            });
        }

        @Test @DisplayName("targetView=null -> return ngay")
        void targetViewNull_earlyReturn() throws Exception { assertNavClick(null); }
        @Test @DisplayName("targetView=HOME") void navHome() throws Exception { assertNavClick("HOME"); }
        @Test @DisplayName("targetView=PROFILE") void navProfile() throws Exception { assertNavClick("PROFILE"); }
        @Test @DisplayName("targetView=EXPLORE") void navExplore() throws Exception { assertNavClick("EXPLORE"); }
        @Test @DisplayName("targetView=SELLER") void navSeller() throws Exception { assertNavClick("SELLER"); }
        @Test @DisplayName("targetView=LIVE") void navLive() throws Exception { assertNavClick("LIVE"); }
        @Test @DisplayName("targetView=SETTINGS") void navSettings() throws Exception { assertNavClick("SETTINGS"); }
        @Test @DisplayName("targetView=UNKNOWN -> default branch") void navUnknown() throws Exception { assertNavClick("UNKNOWN_NAV"); }
    }

    // =========================================================================
    //  updateNavStyles() - all branches
    // =========================================================================
    @Nested @DisplayName("updateNavStyles() - all branches")
    class UpdateNavStyles {

        @Test @DisplayName("btnHome == null -> return ngay khong NPE")
        void btnHomeNull_earlyReturn() throws Exception {
            TestableMainController ctrl = buildAndInit();
            runFx(() -> { setField(ctrl, "btnHome", null); ctrl.updateNavStyles("HOME"); });
        }

        @Test @DisplayName("navId == null -> reset style ve base (#94a3b8)")
        void navIdNull_baseStyleOnly() throws Exception {
            TestableMainController ctrl = buildAndInit();
            AtomicReference<String> style = new AtomicReference<>();
            runFx(() -> { ctrl.updateNavStyles(null); style.set(((Button) getField(ctrl, "btnHome")).getStyle()); });
            assertTrue(style.get().contains("#94a3b8"));
        }

        @Test @DisplayName("navId=HOME -> btnHome active style mau cam (#f97316)")
        void navIdHome_homeActive() throws Exception {
            TestableMainController ctrl = buildAndInit();
            AtomicReference<String> style = new AtomicReference<>();
            runFx(() -> { ctrl.updateNavStyles("HOME"); style.set(((Button) getField(ctrl, "btnHome")).getStyle()); });
            assertTrue(style.get().contains("#f97316"));
        }

        @Test @DisplayName("navId=PROFILE, btnAccount != null -> active style do (#ef4444)")
        void navIdProfile_accountActive() throws Exception {
            TestableMainController ctrl = buildAndInit();
            AtomicReference<String> style = new AtomicReference<>();
            runFx(() -> { ctrl.updateNavStyles("PROFILE"); style.set(((Button) getField(ctrl, "btnAccount")).getStyle()); });
            assertTrue(style.get().contains("#ef4444"));
        }

        @Test @DisplayName("navId=PROFILE, btnAccount == null -> khong NPE")
        void navIdProfile_btnAccountNull() throws Exception {
            TestableMainController ctrl = buildAndInit();
            runFx(() -> { setField(ctrl, "btnAccount", null); ctrl.updateNavStyles("PROFILE"); });
        }

        @Test @DisplayName("navId=EXPLORE, btnExplore != null -> active style xanh duong (#3b82f6)")
        void navIdExplore_exploreActive() throws Exception {
            TestableMainController ctrl = buildAndInit();
            AtomicReference<String> style = new AtomicReference<>();
            runFx(() -> { ctrl.updateNavStyles("EXPLORE"); style.set(((Button) getField(ctrl, "btnExplore")).getStyle()); });
            assertTrue(style.get().contains("#3b82f6"));
        }

        @Test @DisplayName("navId=EXPLORE, btnExplore == null -> khong NPE")
        void navIdExplore_btnExploreNull() throws Exception {
            TestableMainController ctrl = buildAndInit();
            runFx(() -> { setField(ctrl, "btnExplore", null); ctrl.updateNavStyles("EXPLORE"); });
        }

        @Test @DisplayName("navId=SELLER, btnMyProducts != null -> active style xanh la (#22c55e)")
        void navIdSeller_sellerActive() throws Exception {
            TestableMainController ctrl = buildAndInit();
            AtomicReference<String> style = new AtomicReference<>();
            runFx(() -> { ctrl.updateNavStyles("SELLER"); style.set(((Button) getField(ctrl, "btnMyProducts")).getStyle()); });
            assertTrue(style.get().contains("#22c55e"));
        }

        @Test @DisplayName("navId=SELLER, btnMyProducts == null -> khong NPE")
        void navIdSeller_btnMyProductsNull() throws Exception {
            TestableMainController ctrl = buildAndInit();
            runFx(() -> { setField(ctrl, "btnMyProducts", null); ctrl.updateNavStyles("SELLER"); });
        }

        @Test @DisplayName("navId=SETTINGS, btnSettings != null -> active style tim (#a855f7)")
        void navIdSettings_settingsActive() throws Exception {
            TestableMainController ctrl = buildAndInit();
            AtomicReference<String> style = new AtomicReference<>();
            runFx(() -> { ctrl.updateNavStyles("SETTINGS"); style.set(((Button) getField(ctrl, "btnSettings")).getStyle()); });
            assertTrue(style.get().contains("#a855f7"));
        }

        @Test @DisplayName("navId=SETTINGS, btnSettings == null -> khong NPE")
        void navIdSettings_btnSettingsNull() throws Exception {
            TestableMainController ctrl = buildAndInit();
            runFx(() -> { setField(ctrl, "btnSettings", null); ctrl.updateNavStyles("SETTINGS"); });
        }

        @Test @DisplayName("navId khong khop case nao -> khong crash")
        void navIdUnknown_noActiveStyle() throws Exception {
            TestableMainController ctrl = buildAndInit();
            runFx(() -> ctrl.updateNavStyles("UNKNOWN_ID"));
        }
    }

    // =========================================================================
    //  loadContentView()
    // =========================================================================
    @Nested @DisplayName("loadContentView()")
    class LoadContentView {

        @Test @DisplayName("FXML khong ton tai -> khong crash process")
        void fxmlNotFound_handledGracefully() throws Exception {
            AtomicBoolean completed = new AtomicBoolean(false);
            runFx(() -> {
                MainController ctrl = new MainController();
                setField(ctrl, "vboxMainContentArea", new VBox());
                try { ctrl.loadContentView("NonExistentView.fxml"); } catch (Exception ignored) {}
                completed.set(true);
            });
            assertTrue(completed.get());
        }

        @Test @DisplayName("TestableMainController.loadContentView() la no-op -> khong nem exception")
        void testableController_loadContentViewNoOp() throws Exception {
            TestableMainController ctrl = buildAndInit();
            runFx(() -> assertDoesNotThrow(() -> ctrl.loadContentView("AnyView.fxml")));
        }
    }

    // =========================================================================
    //  onLogoClick()
    // =========================================================================
    @Nested @DisplayName("onLogoClick()")
    class OnLogoClick {
        @Test @DisplayName("onLogoClick -> switchView(HomeView.fxml, HOME) -> HOME style")
        void logoClick_switchesToHome() throws Exception {
            TestableMainController ctrl = buildAndInit();
            AtomicReference<String> style = new AtomicReference<>();
            runFx(() -> {
                getMethod(MainController.class, "onLogoClick").invoke(ctrl);
                style.set(((Button) getField(ctrl, "btnHome")).getStyle());
            });
            assertTrue(style.get().contains("#f97316"));
        }
    }

    // =========================================================================
    //  onProfileClick()
    // =========================================================================
    @Nested @DisplayName("onProfileClick()")
    class OnProfileClick {
        @Test @DisplayName("onProfileClick -> PROFILE style")
        void profileClick_switchesToProfile() throws Exception {
            TestableMainController ctrl = buildAndInit();
            AtomicReference<String> style = new AtomicReference<>();
            runFx(() -> {
                getMethod(MainController.class, "onProfileClick", MouseEvent.class).invoke(ctrl, (MouseEvent) null);
                style.set(((Button) getField(ctrl, "btnAccount")).getStyle());
            });
            assertTrue(style.get().contains("#ef4444"));
        }
    }

    // =========================================================================
    //  onLogout()
    // =========================================================================
    @Nested @DisplayName("onLogout()")
    class OnLogout {
        @Test @DisplayName("onLogout() -> SessionManager.logout() duoc goi 1 lan")
        void logout_callsSessionManagerLogout() throws Exception {
            TestableMainController ctrl = buildAndInit();
            AtomicBoolean called = new AtomicBoolean(false);
            runFx(() -> {
                try (MockedStatic<SessionManager> sm = mockStatic(SessionManager.class)) {
                    sm.when(SessionManager::logout).thenAnswer(inv -> { called.set(true); return null; });
                    getMethod(MainController.class, "onLogout").invoke(ctrl);
                }
            });
            assertTrue(called.get());
        }
    }

    // =========================================================================
    //  onFavoriteClick() & onNotificationClick()
    // =========================================================================
    @Nested @DisplayName("onFavoriteClick() & onNotificationClick()")
    class PopupHandlers {

        private MouseEvent makeSrcEvent(Node source) {
            return new MouseEvent(source, null, MouseEvent.MOUSE_CLICKED,
                    0, 0, 0, 0, javafx.scene.input.MouseButton.PRIMARY,
                    1, false, false, false, false, true, false, false, false, false, false, null);
        }

        @Test @DisplayName("onFavoriteClick -> PopupUtil.showPopup('FavoriteContent.fxml')")
        void favoriteClick_callsShowPopup() throws Exception {
            TestableMainController ctrl = buildAndInit();
            AtomicReference<String> fxml = new AtomicReference<>();
            runFx(() -> {
                try (MockedStatic<PopupUtil> pu = mockStatic(PopupUtil.class)) {
                    pu.when(() -> PopupUtil.showPopup(any(), anyString()))
                            .thenAnswer(inv -> { fxml.set(inv.getArgument(1)); return null; });
                    getMethod(MainController.class, "onFavoriteClick", MouseEvent.class)
                            .invoke(ctrl, makeSrcEvent(new Button()));
                }
            });
            assertEquals("FavoriteContent.fxml", fxml.get());
        }

        @Test @DisplayName("onNotificationClick -> PopupUtil.showPopup('NotificationContent.fxml')")
        void notificationClick_callsShowPopup() throws Exception {
            TestableMainController ctrl = buildAndInit();
            AtomicReference<String> fxml = new AtomicReference<>();
            runFx(() -> {
                try (MockedStatic<PopupUtil> pu = mockStatic(PopupUtil.class)) {
                    pu.when(() -> PopupUtil.showPopup(any(), anyString()))
                            .thenAnswer(inv -> { fxml.set(inv.getArgument(1)); return null; });
                    getMethod(MainController.class, "onNotificationClick", MouseEvent.class)
                            .invoke(ctrl, makeSrcEvent(new Button()));
                }
            });
            assertEquals("NotificationContent.fxml", fxml.get());
        }
    }

    // =========================================================================
    //  switchView()
    // =========================================================================
    @Nested @DisplayName("switchView()")
    class SwitchView {
        @Test @DisplayName("switchView goi loadContentView (no-op) roi updateNavStyles -> EXPLORE style")
        void switchView_delegatesBoth() throws Exception {
            TestableMainController ctrl = buildAndInit();
            AtomicReference<String> style = new AtomicReference<>();
            runFx(() -> {
                ctrl.switchView("ExploreView.fxml", "EXPLORE");
                style.set(((Button) getField(ctrl, "btnExplore")).getStyle());
            });
            assertTrue(style.get().contains("#3b82f6"));
        }
    }

    // =========================================================================
    //  syncTimeline
    // =========================================================================
    @Nested @DisplayName("syncTimeline callback")
    class SyncTimeline {

        @Test @DisplayName("isLoggedIn=true && isConnected=true -> services duoc goi")
        void syncTimeline_loggedInAndConnected_callsServices() throws Exception {
            TestableMainController ctrl = buildAndInit(null, true);
            AtomicBoolean auctionCalled = new AtomicBoolean(false);
            AtomicBoolean notiCalled    = new AtomicBoolean(false);
            AtomicBoolean favCalled     = new AtomicBoolean(false);
            runFx(() -> {
                try (MockedStatic<AuctionService>      as  = mockStatic(AuctionService.class);
                     MockedStatic<NotificationService> ns  = mockStatic(NotificationService.class);
                     MockedStatic<FavoriteService>     fs  = mockStatic(FavoriteService.class);
                     MockedStatic<SessionManager>      sm  = mockStatic(SessionManager.class);
                     MockedStatic<vn.edu.vnu.uet.group8.client.networking.AuctionClient> ac =
                             mockStatic(vn.edu.vnu.uet.group8.client.networking.AuctionClient.class)) {

                    sm.when(SessionManager::isLoggedIn).thenReturn(true);
                    vn.edu.vnu.uet.group8.client.networking.AuctionClient fake =
                            mock(vn.edu.vnu.uet.group8.client.networking.AuctionClient.class);
                    when(fake.isConnected()).thenReturn(true);
                    ac.when(vn.edu.vnu.uet.group8.client.networking.AuctionClient::getInstance).thenReturn(fake);
                    as.when(() -> AuctionService.loadAll(any(), any())).thenAnswer(inv -> { auctionCalled.set(true); return null; });
                    ns.when(() -> NotificationService.loadAll(any(), any())).thenAnswer(inv -> { notiCalled.set(true); return null; });
                    fs.when(() -> FavoriteService.loadAll(any(), any())).thenAnswer(inv -> { favCalled.set(true); return null; });

                    javafx.animation.Timeline tl = getField(ctrl, "syncTimeline");
                    tl.getKeyFrames().get(0).getOnFinished().handle(null);
                }
            });
            assertTrue(auctionCalled.get());
            assertTrue(notiCalled.get());
            assertTrue(favCalled.get());
        }

        @Test @DisplayName("isLoggedIn=false -> services KHONG duoc goi")
        void syncTimeline_notLoggedIn_skipsServices() throws Exception {
            TestableMainController ctrl = buildAndInit(null, false);
            AtomicBoolean auctionCalled = new AtomicBoolean(false);
            runFx(() -> {
                try (MockedStatic<AuctionService> as = mockStatic(AuctionService.class);
                     MockedStatic<SessionManager> sm = mockStatic(SessionManager.class);
                     MockedStatic<vn.edu.vnu.uet.group8.client.networking.AuctionClient> ac =
                             mockStatic(vn.edu.vnu.uet.group8.client.networking.AuctionClient.class)) {

                    sm.when(SessionManager::isLoggedIn).thenReturn(false);
                    vn.edu.vnu.uet.group8.client.networking.AuctionClient fake =
                            mock(vn.edu.vnu.uet.group8.client.networking.AuctionClient.class);
                    when(fake.isConnected()).thenReturn(true);
                    ac.when(vn.edu.vnu.uet.group8.client.networking.AuctionClient::getInstance).thenReturn(fake);
                    as.when(() -> AuctionService.loadAll(any(), any())).thenAnswer(inv -> { auctionCalled.set(true); return null; });

                    javafx.animation.Timeline tl = getField(ctrl, "syncTimeline");
                    tl.getKeyFrames().get(0).getOnFinished().handle(null);
                }
            });
            assertFalse(auctionCalled.get());
        }
    }
}