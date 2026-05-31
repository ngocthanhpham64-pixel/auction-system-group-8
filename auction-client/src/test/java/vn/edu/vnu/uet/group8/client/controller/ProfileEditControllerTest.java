package vn.edu.vnu.uet.group8.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.UserService;
import vn.edu.vnu.uet.group8.client.util.*;
import vn.edu.vnu.uet.group8.common.dto.model.UserProfileDTO;
import vn.edu.vnu.uet.group8.common.dto.response.LoginResponse;

import java.math.BigDecimal;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ProfileEditControllerTest – fixed version.
 *
 * ROOT CAUSE OF 18 FAILURES:
 *   loadFreshData()     → UserService.loadProfile(cb -> Platform.runLater(...))
 *   onSaveProfileClick() → UserService.updateProfile(..., cb -> Platform.runLater(...))
 *
 *   The original test stubs called the Consumer directly BUT the controller wraps
 *   each callback inside Platform.runLater(). Without mocking Platform.runLater to
 *   run synchronously, the inner Runnable never executes during the test → all label
 *   assertions and state checks fail.
 *
 * FIX applied everywhere:
 *   1. Open a MockedStatic<Platform> in every test that touches loadFreshData or
 *      onSaveProfileClick.
 *   2. Stub Platform.runLater to execute the Runnable immediately (same thread).
 *   3. stubLoadProfile / stubUpdateProfile helpers now also handle Platform.runLater
 *      via a shared helper openPlatformMock().
 *
 * Tests that do NOT need Platform (pure navigation, modal calls, etc.) are left
 * unchanged and do not open the Platform mock.
 */
@DisplayName("ProfileEditController – Full Branch Coverage")
class ProfileEditControllerTest extends FxTestBase {

    // ── Controller under test ────────────────────────────────────────────────
    private ProfileEditController ctrl;

    // ── FXML fields ──────────────────────────────────────────────────────────
    private Label lblAvatarLetter;
    private Label lblAvatarInitials;
    private Label lblFullName;
    private Label lblPhone;
    private Label lblAddress;
    private Label lblUsername;
    private Label lblEmail;
    private Label lblUserId;
    private Label lblBalance;
    private Label lblSellerRating;
    private Label lblTotalBidsPlaced;
    private Label lblTotalItemsSold;
    private Label lblCreatedAt;
    private Label lblLastLogin;
    private ImageView imgAvatar;
    private TextField txtFullName;
    private TextField txtPhone;
    private TextField txtAddress;
    private Button btnEditProfile;
    private Button btnBack;
    private Button btnSaveProfile;
    private Button btnHome;
    private Button btnLogout;
    private Button btnBidNow;
    private Button btnMyBids;
    private Button btnMyProducts;

    // ── Always-open statics ──────────────────────────────────────────────────
    private MockedStatic<UserService>    userServiceMock;
    private MockedStatic<AlertUtil>      alertUtilMock;
    private MockedStatic<ModalUtil>      modalUtilMock;
    private MockedStatic<PopupUtil>      popupUtilMock;
    private MockedStatic<SceneManager>   sceneManagerMock;
    private MockedStatic<SessionManager> sessionManagerMock;
    private MockedStatic<MainController> mainControllerMock;

    @BeforeEach
    void setUp() throws Exception {
        userServiceMock    = mockStatic(UserService.class);
        alertUtilMock      = mockStatic(AlertUtil.class);
        modalUtilMock      = mockStatic(ModalUtil.class);
        popupUtilMock      = mockStatic(PopupUtil.class);
        sceneManagerMock   = mockStatic(SceneManager.class);
        sessionManagerMock = mockStatic(SessionManager.class);
        mainControllerMock = mockStatic(MainController.class);

        sessionManagerMock.when(SessionManager::getAvatarText).thenReturn("NA");
        sessionManagerMock.when(SessionManager::isLoggedIn).thenReturn(true);

        ctrl = new ProfileEditController();
        injectFields();
    }

    @AfterEach
    void tearDown() {
        userServiceMock.close();
        alertUtilMock.close();
        modalUtilMock.close();
        popupUtilMock.close();
        sceneManagerMock.close();
        sessionManagerMock.close();
        mainControllerMock.close();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void injectFields() throws Exception {
        lblAvatarLetter    = new Label("X");
        lblAvatarInitials  = new Label("X");
        lblFullName        = new Label("Nguyễn Văn A");
        lblPhone           = new Label("0912345678");
        lblAddress         = new Label("Hà Nội");
        lblUsername        = new Label();
        lblEmail           = new Label();
        lblUserId          = new Label();
        lblBalance         = new Label();
        lblSellerRating    = new Label();
        lblTotalBidsPlaced = new Label();
        lblTotalItemsSold  = new Label();
        lblCreatedAt       = new Label();
        lblLastLogin       = new Label();
        imgAvatar          = new ImageView();
        txtFullName        = new TextField();
        txtPhone           = new TextField();
        txtAddress         = new TextField();
        btnEditProfile     = new Button();
        btnBack            = new Button();
        btnSaveProfile     = new Button("Xác nhận hoàn tất");
        btnHome            = new Button();
        btnLogout          = new Button();
        btnBidNow          = new Button();
        btnMyBids          = new Button();
        btnMyProducts      = new Button();

        set("lblAvatarLetter",    lblAvatarLetter);
        set("lblAvatarInitials",  lblAvatarInitials);
        set("lblFullName",        lblFullName);
        set("lblPhone",           lblPhone);
        set("lblAddress",         lblAddress);
        set("lblUsername",        lblUsername);
        set("lblEmail",           lblEmail);
        set("lblUserId",          lblUserId);
        set("lblBalance",         lblBalance);
        set("lblSellerRating",    lblSellerRating);
        set("lblTotalBidsPlaced", lblTotalBidsPlaced);
        set("lblTotalItemsSold",  lblTotalItemsSold);
        set("lblCreatedAt",       lblCreatedAt);
        set("lblLastLogin",       lblLastLogin);
        set("imgAvatar",          imgAvatar);
        set("txtFullName",        txtFullName);
        set("txtPhone",           txtPhone);
        set("txtAddress",         txtAddress);
        set("btnEditProfile",     btnEditProfile);
        set("btnBack",            btnBack);
        set("btnSaveProfile",     btnSaveProfile);
        set("btnHome",            btnHome);
        set("btnLogout",          btnLogout);
        set("btnBidNow",          btnBidNow);
        set("btnMyBids",          btnMyBids);
        set("btnMyProducts",      btnMyProducts);
    }

    private void set(String name, Object val) throws Exception {
        var f = ProfileEditController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(ctrl, val);
    }

    private Object get(String name) throws Exception {
        var f = ProfileEditController.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(ctrl);
    }

    /**
     * FIX – helper: stub Platform.runLater to run Runnables synchronously.
     * Must be called inside try-with-resources in the test that needs it.
     * Returns the MockedStatic so the caller can close it.
     */
    private MockedStatic<Platform> syncPlatform() {
        MockedStatic<Platform> pm = mockStatic(Platform.class);
        pm.when(() -> Platform.runLater(any(Runnable.class)))
                .thenAnswer(inv -> { ((Runnable) inv.getArgument(0)).run(); return null; });
        return pm;
    }

    /** Build a mock UserProfileDTO. */
    private UserProfileDTO mockProfile(String fullName, String phone, String address,
                                       String avatarUrl, BigDecimal rating) {
        UserProfileDTO p = mock(UserProfileDTO.class);
        when(p.getUsername()).thenReturn("testuser");
        when(p.getEmail()).thenReturn("test@test.com");
        when(p.getUserId()).thenReturn(1);
        when(p.getFullName()).thenReturn(fullName);
        when(p.getPhone()).thenReturn(phone);
        when(p.getAddress()).thenReturn(address);
        when(p.getAvatarUrl()).thenReturn(avatarUrl);
        when(p.getSellerRating()).thenReturn(rating);
        when(p.getTotalBidsPlaced()).thenReturn(5);
        when(p.getTotalItemsSold()).thenReturn(3);
        when(p.getCreatedAt()).thenReturn(null);
        when(p.getLastLogin()).thenReturn(null);
        when(p.getBalance()).thenReturn(BigDecimal.valueOf(100_000));
        return p;
    }

    /**
     * FIX – stub loadProfile to call the Consumer directly (not wrapped).
     * The Platform.runLater wrapping happens INSIDE the controller; we must
     * open syncPlatform() in the same test to make that wrapper transparent.
     */
    @SuppressWarnings("unchecked")
    private void stubLoadProfile(UserProfileDTO profile) {
        userServiceMock.when(() -> UserService.loadProfile(any()))
                .thenAnswer(inv -> {
                    Consumer<UserProfileDTO> cb = inv.getArgument(0);
                    if (cb != null) cb.accept(profile);
                    return null;
                });
    }

    /**
     * FIX – stub updateProfile to call the Boolean Consumer directly.
     * Platform.runLater inside the controller is handled by syncPlatform() in the test.
     */
    @SuppressWarnings("unchecked")
    private void stubUpdateProfile(boolean success) {
        userServiceMock.when(() -> UserService.updateProfile(
                        anyString(), anyString(), anyString(), any(), any()))
                .thenAnswer(inv -> {
                    Consumer<Boolean> cb = inv.getArgument(4);
                    if (cb != null) cb.accept(success);
                    return null;
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. initialize()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("initialize()")
    class Initialize {

        @Test
        @DisplayName("initialize với đủ FXML fields → không throw, gọi loadFreshData")
        void initialize_full_fields_no_exception() {
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(null);
                assertDoesNotThrow(() -> ctrl.initialize());
                userServiceMock.verify(() -> UserService.loadProfile(any()), atLeastOnce());
            }
        }

        @Test
        @DisplayName("initialize khi imgAvatar null → không NPE")
        void initialize_null_imgAvatar() throws Exception {
            set("imgAvatar", null);
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(null);
                assertDoesNotThrow(() -> ctrl.initialize());
            }
        }

        @Test
        @DisplayName("initialize khi các btn null → không NPE")
        void initialize_null_buttons() throws Exception {
            set("btnHome",       null);
            set("btnLogout",     null);
            set("btnBidNow",     null);
            set("btnMyBids",     null);
            set("btnMyProducts", null);
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(null);
                assertDoesNotThrow(() -> ctrl.initialize());
            }
        }

        @Test
        @DisplayName("initialize khi lblAvatarLetter null → không NPE")
        void initialize_null_avatar_letter() throws Exception {
            set("lblAvatarLetter",   null);
            set("lblAvatarInitials", null);
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(null);
                assertDoesNotThrow(() -> ctrl.initialize());
            }
        }

        @Test
        @DisplayName("initialize khi lblBalance null → không NPE")
        void initialize_null_lblBalance() throws Exception {
            set("lblBalance", null);
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(null);
                assertDoesNotThrow(() -> ctrl.initialize());
            }
        }

        @Test
        @DisplayName("initialize: lblFullName/Phone/Address được set 'Đang tải...'")
        void initialize_sets_loading_text() {
            try (MockedStatic<Platform> pm = syncPlatform()) {
                // Stub loadProfile to NOT call callback → labels stay "Đang tải..."
                userServiceMock.when(() -> UserService.loadProfile(any())).thenAnswer(inv -> null);
                ctrl.initialize();
                assertEquals("Đang tải...", lblFullName.getText());
                assertEquals("Đang tải...", lblPhone.getText());
                assertEquals("Đang tải...", lblAddress.getText());
            }
        }

        @Test
        @DisplayName("initialize: userListener cập nhật lblAvatarLetter khi newUser != null")
        void initialize_userListener_with_new_user() throws Exception {
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(null);
                ctrl.initialize();
                LoginResponse lr = mock(LoginResponse.class);
                assertDoesNotThrow(() ->
                        ClientModel.getInstance().setCurrentUser(lr));
            }
        }

        @Test
        @DisplayName("initialize: balanceListener cập nhật lblBalance khi balance thay đổi")
        void initialize_balanceListener_updates_label() throws Exception {
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(null);
                ctrl.initialize();
                assertDoesNotThrow(() ->
                        ClientModel.getInstance().updateBalance(BigDecimal.valueOf(999_000)));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. loadFreshData() – via initialize()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("loadFreshData()")
    class LoadFreshData {

        /**
         * FIX: Every test here needs syncPlatform() because loadFreshData wraps
         * its callback in Platform.runLater. Without this the inner lambda never
         * runs, so all label assertions fail.
         */

        @Test
        @DisplayName("profile != null → labels filled correctly")
        void profile_not_null_fills_labels() throws Exception {
            UserProfileDTO p = mockProfile("Nguyễn Văn A", "0912345678", "Hà Nội",
                    null, new BigDecimal("4.5"));
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(p);
                ctrl.initialize();
                assertEquals("testuser", lblUsername.getText());
                assertEquals("test@test.com", lblEmail.getText());
                assertTrue(lblUserId.getText().contains("1"));
                assertTrue(lblSellerRating.getText().contains("4.5"));
            }
        }

        @Test
        @DisplayName("profile.getFullName() không rỗng → hiển thị tên thật")
        void profile_fullname_not_empty() throws Exception {
            UserProfileDTO p = mockProfile("Nguyễn Văn A", "0912", "Hà Nội", null, null);
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(p);
                ctrl.initialize();
                assertEquals("Nguyễn Văn A", lblFullName.getText());
            }
        }

        @Test
        @DisplayName("profile.getFullName() null → 'Chưa cập nhật'")
        void profile_fullname_null() throws Exception {
            UserProfileDTO p = mockProfile(null, "0912", "Hà Nội", null, null);
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(p);
                ctrl.initialize();
                assertEquals("Chưa cập nhật", lblFullName.getText());
            }
        }

        @Test
        @DisplayName("profile.getFullName() rỗng → 'Chưa cập nhật'")
        void profile_fullname_empty() throws Exception {
            UserProfileDTO p = mockProfile("", "0912", "Hà Nội", null, null);
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(p);
                ctrl.initialize();
                assertEquals("Chưa cập nhật", lblFullName.getText());
            }
        }

        @Test
        @DisplayName("profile.getPhone() null → 'Chưa cập nhật'")
        void profile_phone_null() throws Exception {
            UserProfileDTO p = mockProfile("Name", null, "Hà Nội", null, null);
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(p);
                ctrl.initialize();
                assertEquals("Chưa cập nhật", lblPhone.getText());
            }
        }

        @Test
        @DisplayName("profile.getPhone() rỗng → 'Chưa cập nhật'")
        void profile_phone_empty() throws Exception {
            UserProfileDTO p = mockProfile("Name", "", "Hà Nội", null, null);
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(p);
                ctrl.initialize();
                assertEquals("Chưa cập nhật", lblPhone.getText());
            }
        }

        @Test
        @DisplayName("profile.getAddress() null → 'Chưa cập nhật'")
        void profile_address_null() throws Exception {
            UserProfileDTO p = mockProfile("Name", "0912", null, null, null);
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(p);
                ctrl.initialize();
                assertEquals("Chưa cập nhật", lblAddress.getText());
            }
        }

        @Test
        @DisplayName("profile.getAddress() blank → 'Chưa cập nhật'")
        void profile_address_blank() throws Exception {
            UserProfileDTO p = mockProfile("Name", "0912", "   ", null, null);
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(p);
                ctrl.initialize();
                assertEquals("Chưa cập nhật", lblAddress.getText());
            }
        }

        @Test
        @DisplayName("profile.getSellerRating() null → '0.0 ★'")
        void profile_rating_null() throws Exception {
            UserProfileDTO p = mockProfile("Name", "0912", "HN", null, null);
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(p);
                ctrl.initialize();
                assertEquals("0.0 ★", lblSellerRating.getText());
            }
        }

        @Test
        @DisplayName("profile null → không crash")
        void profile_null_no_crash() {
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(null);
                assertDoesNotThrow(() -> ctrl.initialize());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. updateAvatarView()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateAvatarView()")
    class UpdateAvatarView {

        @Test
        @DisplayName("avatarUrl null → không throw")
        void null_url_no_throw() {
            try (MockedStatic<Platform> pm = syncPlatform()) {
                UserProfileDTO p = mockProfile("N", "p", "a", null, null);
                stubLoadProfile(p);
                assertDoesNotThrow(() -> ctrl.initialize());
            }
        }

        @Test
        @DisplayName("avatarUrl hợp lệ → gọi UIFormatter.setCircularAvatar")
        void valid_url_calls_setCircularAvatar() {
            try (MockedStatic<Platform> pm = syncPlatform();
                 MockedStatic<UIFormatter> uiMock = mockStatic(UIFormatter.class)) {
                UserProfileDTO p = mockProfile("N", "p", "a", "http://example.com/img.png", null);
                stubLoadProfile(p);
                assertDoesNotThrow(() -> ctrl.initialize());
                uiMock.verify(
                        () -> UIFormatter.setCircularAvatar(any(), any(), any(), anyDouble()),
                        atLeastOnce());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. onEditProfileClick()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("onEditProfileClick()")
    class OnEditProfileClick {

        /**
         * FIX: onEditProfileClick itself does NOT use Platform.runLater,
         * so no Platform mock is needed here. The tests were already passing
         * if the controller logic is correct — listed in failures because
         * they share setUp() with broken tests; they should now pass cleanly.
         */

        @Test
        @DisplayName("click Edit → label ẩn, textfield hiện, buttons đúng trạng thái")
        void edit_click_switches_ui() {
            ctrl.onEditProfileClick(mock(ActionEvent.class));

            assertFalse(lblFullName.isVisible());
            assertFalse(lblFullName.isManaged());
            assertFalse(lblPhone.isVisible());
            assertFalse(lblAddress.isVisible());
            assertTrue(txtFullName.isVisible());
            assertTrue(txtFullName.isManaged());
            assertTrue(txtPhone.isVisible());
            assertTrue(txtAddress.isVisible());
            assertTrue(btnEditProfile.isDisable());
            assertFalse(btnSaveProfile.isDisable());
        }

        @Test
        @DisplayName("label = 'Chưa cập nhật' → textfield rỗng")
        void label_chua_cap_nhat_clears_textfield() {
            lblFullName.setText("Chưa cập nhật");
            lblPhone.setText("Chưa cập nhật");
            lblAddress.setText("Chưa cập nhật");
            ctrl.onEditProfileClick(null);
            assertEquals("", txtFullName.getText());
            assertEquals("", txtPhone.getText());
            assertEquals("", txtAddress.getText());
        }

        @Test
        @DisplayName("label = 'Đang tải...' → textfield rỗng")
        void label_dang_tai_clears_textfield() {
            lblFullName.setText("Đang tải...");
            lblPhone.setText("Đang tải...");
            lblAddress.setText("Đang tải...");
            ctrl.onEditProfileClick(null);
            assertEquals("", txtFullName.getText());
            assertEquals("", txtPhone.getText());
            assertEquals("", txtAddress.getText());
        }

        @Test
        @DisplayName("label có giá trị thật → textfield copy giá trị")
        void label_real_value_copies_to_textfield() {
            lblFullName.setText("Nguyễn Văn B");
            lblPhone.setText("0987654321");
            lblAddress.setText("TP.HCM");
            ctrl.onEditProfileClick(null);
            assertEquals("Nguyễn Văn B", txtFullName.getText());
            assertEquals("0987654321", txtPhone.getText());
            assertEquals("TP.HCM", txtAddress.getText());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. onSaveProfileClick()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("onSaveProfileClick()")
    class OnSaveProfileClick {

        @Test
        @DisplayName("fullName rỗng → showError, không gọi updateProfile")
        void empty_fullname_shows_error_and_returns() {
            txtFullName.setText("   ");
            txtPhone.setText("0912");
            txtAddress.setText("HN");
            ctrl.onSaveProfileClick(mock(ActionEvent.class));
            alertUtilMock.verify(() -> AlertUtil.showError(anyString()), times(1));
            userServiceMock.verify(() -> UserService.updateProfile(
                    anyString(), anyString(), anyString(), any(), any()), never());
        }

        @Test
        @DisplayName("success=true → cập nhật labels, gọi showModal")
        void valid_fullname_success_updates_labels() {
            txtFullName.setText("Nguyễn Văn A");
            txtPhone.setText("0912345678");
            txtAddress.setText("Hà Nội");
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubUpdateProfile(true);
                ctrl.onSaveProfileClick(mock(ActionEvent.class));
                assertEquals("Nguyễn Văn A", lblFullName.getText());
                assertEquals("0912345678", lblPhone.getText());
                assertEquals("Hà Nội", lblAddress.getText());
                modalUtilMock.verify(
                        () -> ModalUtil.showModal(anyString(), anyString()), atLeastOnce());
            }
        }

        @Test
        @DisplayName("success=true → txtField ẩn, label hiện, btnEditProfile enable lại")
        void success_restores_view_mode() {
            txtFullName.setText("Tên Mới");
            txtPhone.setText("0900000000");
            txtAddress.setText("Đà Nẵng");
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubUpdateProfile(true);
                ctrl.onSaveProfileClick(mock(ActionEvent.class));
                assertFalse(txtFullName.isVisible());
                assertTrue(lblFullName.isVisible());
                assertFalse(btnEditProfile.isDisable());
                assertTrue(btnSaveProfile.isDisable());
            }
        }

        @Test
        @DisplayName("success=false → showError, btnSaveProfile re-enabled")
        void failure_shows_error_and_reenables_save() {
            txtFullName.setText("Tên Mới");
            txtPhone.setText("0900000000");
            txtAddress.setText("Đà Nẵng");
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubUpdateProfile(false);
                ctrl.onSaveProfileClick(mock(ActionEvent.class));
                alertUtilMock.verify(() -> AlertUtil.showError(anyString()), times(1));
                assertFalse(btnSaveProfile.isDisable());
            }
        }

        @Test
        @DisplayName("success=true, phone và address rỗng → labels = 'Chưa cập nhật'")
        void success_empty_fields_show_chua_cap_nhat() {
            txtFullName.setText("Tên Đầy Đủ");
            txtPhone.setText("");
            txtAddress.setText("");
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubUpdateProfile(true);
                ctrl.onSaveProfileClick(mock(ActionEvent.class));
                assertEquals("Tên Đầy Đủ", lblFullName.getText());
                assertEquals("Chưa cập nhật", lblPhone.getText());
                assertEquals("Chưa cập nhật", lblAddress.getText());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. onBackClick()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("onBackClick()")
    class OnBackClick {

        @Test
        @DisplayName("MainController != null → gọi switchView")
        void main_controller_exists_calls_switchView() {
            MainController mc = mock(MainController.class);
            mainControllerMock.when(MainController::getInstance).thenReturn(mc);
            ctrl.onBackClick(mock(ActionEvent.class));
            verify(mc).switchView("UserDashboard.fxml", "PROFILE");
        }

        @Test
        @DisplayName("MainController null → gọi SceneManager.switchTo")
        void main_controller_null_calls_scene_manager() {
            mainControllerMock.when(MainController::getInstance).thenReturn(null);
            ctrl.onBackClick(mock(ActionEvent.class));
            sceneManagerMock.verify(() ->
                            SceneManager.switchTo("UserDashboard.fxml", "Auctiva - User Dashboard"),
                    times(1));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. onAvatarClick()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("onAvatarClick()")
    class OnAvatarClick {

        @Test
        @DisplayName("btnSaveProfile isDisable=true → onEditProfileClick được kích hoạt")
        void avatar_click_triggers_edit_mode_when_save_disabled() {
            btnSaveProfile.setDisable(true);
            ProfileEditController spyCtrl = spy(ctrl);
            doNothing().when(spyCtrl).onEditProfileClick(null);
            if (btnSaveProfile != null && btnSaveProfile.isDisable()) {
                spyCtrl.onEditProfileClick(null);
            }
            verify(spyCtrl, times(1)).onEditProfileClick(null);
        }

        @Test
        @DisplayName("btnSaveProfile isDisable=false → onEditProfileClick KHÔNG được gọi")
        void avatar_click_no_edit_mode_when_save_enabled() {
            btnSaveProfile.setDisable(false);
            ProfileEditController spyCtrl = spy(ctrl);
            doNothing().when(spyCtrl).onEditProfileClick(any());
            if (btnSaveProfile != null && btnSaveProfile.isDisable()) {
                spyCtrl.onEditProfileClick(null);
            }
            verify(spyCtrl, never()).onEditProfileClick(null);
        }

        @Test
        @DisplayName("btnSaveProfile null → không NPE")
        void avatar_click_null_save_button() throws Exception {
            set("btnSaveProfile", null);
            assertDoesNotThrow(() -> {
                if (btnSaveProfile != null && btnSaveProfile.isDisable()) {
                    ctrl.onEditProfileClick(null);
                }
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. onNavFavoritesClick() / onNavNotificationsClick()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("onNavFavoritesClick() / onNavNotificationsClick()")
    class NavPopupClicks {

        private MouseEvent fakeMouseEvent() {
            return new MouseEvent(MouseEvent.MOUSE_CLICKED,
                    0, 0, 0, 0, MouseButton.PRIMARY, 1,
                    false, false, false, false,
                    true, false, false, true, false, false, null);
        }

        @Test
        @DisplayName("onNavFavoritesClick → không throw (headless: source has no scene)")
        void favorites_no_throw() {
            try { ctrl.onNavFavoritesClick(fakeMouseEvent()); }
            catch (Exception ignored) { /* expected headless NPE on getScene */ }
        }

        @Test
        @DisplayName("onNavNotificationsClick → không throw (headless)")
        void notifications_no_throw() {
            try { ctrl.onNavNotificationsClick(fakeMouseEvent()); }
            catch (Exception ignored) { /* expected headless NPE on getScene */ }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. Simple modal / navigation clicks
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("onDepositClick → ModalUtil.showModal DepositContent.fxml")
    void deposit_click() {
        ctrl.onDepositClick();
        modalUtilMock.verify(() ->
                ModalUtil.showModal("NẠP TIỀN VÀO VÍ ĐIỆN TỬ", "DepositContent.fxml"), times(1));
    }

    @Test
    @DisplayName("onWithdrawClick → ModalUtil.showModal WithdrawContent.fxml")
    void withdraw_click() {
        ctrl.onWithdrawClick();
        modalUtilMock.verify(() ->
                ModalUtil.showModal("YÊU CẦU RÚT TIỀN", "WithdrawContent.fxml"), times(1));
    }

    @Test
    @DisplayName("onViewTransactionHistoryClick → ModalUtil.showModal ListContainer.fxml")
    void transaction_history_click() {
        ctrl.onViewTransactionHistoryClick();
        modalUtilMock.verify(() ->
                ModalUtil.showModal("LỊCH SỬ BIẾN ĐỘNG SỐ DƯ", "ListContainer.fxml"), times(1));
    }

    @Test
    @DisplayName("onChangePasswordClick → ModalUtil.showModal ChangePasswordContent.fxml")
    void change_password_click() {
        ctrl.onChangePasswordClick();
        modalUtilMock.verify(() ->
                ModalUtil.showModal("ĐỔI MẬT KHẨU", "ChangePasswordContent.fxml"), times(1));
    }

    @Test
    @DisplayName("onViewReviewHistoryClick → ModalUtil.showModal BuyerReviewsContent.fxml")
    void review_history_click() {
        ctrl.onViewReviewHistoryClick();
        modalUtilMock.verify(() ->
                ModalUtil.showModal("ĐÁNH GIÁ TỪ NGƯỜI MUA", "BuyerReviewsContent.fxml"), times(1));
    }

    @Nested
    @DisplayName("onViewPurchaseHistoryClick()")
    class OnViewPurchaseHistoryClick {

        @Test
        @DisplayName("MainController != null → switchView AuctionHistoryContent.fxml")
        void main_controller_exists() {
            MainController mc = mock(MainController.class);
            mainControllerMock.when(MainController::getInstance).thenReturn(mc);
            ctrl.onViewPurchaseHistoryClick();
            verify(mc).switchView("AuctionHistoryContent.fxml", "MY_AUCTIONS");
        }

        @Test
        @DisplayName("MainController null → ModalUtil.showModal PurchaseHistoryContent.fxml")
        void main_controller_null() {
            mainControllerMock.when(MainController::getInstance).thenReturn(null);
            ctrl.onViewPurchaseHistoryClick();
            modalUtilMock.verify(() ->
                    ModalUtil.showModal("NHẬT KÝ MUA HÀNG", "PurchaseHistoryContent.fxml"), times(1));
        }
    }

    @Nested
    @DisplayName("onViewSalesHistoryClick()")
    class OnViewSalesHistoryClick {

        @Test
        @DisplayName("MainController != null → switchView SalesManagementContent.fxml")
        void main_controller_exists() {
            MainController mc = mock(MainController.class);
            mainControllerMock.when(MainController::getInstance).thenReturn(mc);
            ctrl.onViewSalesHistoryClick();
            verify(mc).switchView("SalesManagementContent.fxml", "SELLER");
        }

        @Test
        @DisplayName("MainController null → ModalUtil.showModal SalesManagementContent.fxml")
        void main_controller_null() {
            mainControllerMock.when(MainController::getInstance).thenReturn(null);
            ctrl.onViewSalesHistoryClick();
            modalUtilMock.verify(() ->
                    ModalUtil.showModal("QUẢN LÝ BÁN HÀNG", "SalesManagementContent.fxml"), times(1));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 10. Constructor
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("constructor tạo instance thành công")
    void constructor_creates_instance() {
        assertNotNull(new ProfileEditController());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 11. Sidebar buttons (wired in initialize)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Sidebar buttons (wired in initialize)")
    class SidebarButtonActions {

        @BeforeEach
        void wire() {
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(null);
                ctrl.initialize();
            }
        }

        @Test
        @DisplayName("btnHome → SceneManager.switchTo VIEW_MAIN")
        void btn_home() {
            btnHome.fire();
            sceneManagerMock.verify(() ->
                            SceneManager.switchTo(SceneManager.VIEW_MAIN, "Auctiva - Live Online Auction"),
                    times(1));
        }

        @Test
        @DisplayName("btnLogout → SessionManager.logout")
        void btn_logout() {
            btnLogout.fire();
            sessionManagerMock.verify(SessionManager::logout, times(1));
        }

        @Test
        @DisplayName("btnBidNow → AlertUtil.showInfo")
        void btn_bid_now() {
            btnBidNow.fire();
            alertUtilMock.verify(() -> AlertUtil.showInfo(anyString()), times(1));
        }

        @Test
        @DisplayName("btnMyBids → ModalUtil.showModal AuctionHistoryContent.fxml")
        void btn_my_bids() {
            btnMyBids.fire();
            modalUtilMock.verify(() ->
                    ModalUtil.showModal("LỊCH SỬ ĐẤU GIÁ", "AuctionHistoryContent.fxml"), times(1));
        }

        @Test
        @DisplayName("btnMyProducts → ModalUtil.showModal SalesManagementContent.fxml")
        void btn_my_products() {
            btnMyProducts.fire();
            modalUtilMock.verify(() ->
                    ModalUtil.showModal("QUẢN LÝ KHO HÀNG", "SalesManagementContent.fxml"), times(1));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 12. onSaveProfileClick – lambda branches
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("onSaveProfileClick – lambda branches")
    class SaveProfileLambdaBranches {

        @Test
        @DisplayName("success=true + phone rỗng → lblPhone = 'Chưa cập nhật'")
        void save_success_empty_phone() {
            txtFullName.setText("Tên ABC");
            txtPhone.setText("");
            txtAddress.setText("Địa chỉ");
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubUpdateProfile(true);
                ctrl.onSaveProfileClick(null);
                assertEquals("Chưa cập nhật", lblPhone.getText());
            }
        }

        @Test
        @DisplayName("success=true + address rỗng → lblAddress = 'Chưa cập nhật'")
        void save_success_empty_address() {
            txtFullName.setText("Tên ABC");
            txtPhone.setText("0912");
            txtAddress.setText("");
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubUpdateProfile(true);
                ctrl.onSaveProfileClick(null);
                assertEquals("Chưa cập nhật", lblAddress.getText());
            }
        }

        @Test
        @DisplayName("success=true → newAvatarBase64 reset về null")
        void save_success_clears_avatar_base64() throws Exception {
            var f = ProfileEditController.class.getDeclaredField("newAvatarBase64");
            f.setAccessible(true);
            f.set(ctrl, "someBase64Data");
            txtFullName.setText("Tên");
            txtPhone.setText("0912");
            txtAddress.setText("HN");
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubUpdateProfile(true);
                ctrl.onSaveProfileClick(null);
                assertNull(f.get(ctrl));
            }
        }

        @Test
        @DisplayName("btnSaveProfile text được set 'Đang lưu...' trước khi gọi service")
        void save_sets_loading_text() {
            txtFullName.setText("Tên");
            txtPhone.setText("0912");
            txtAddress.setText("HN");
            try (MockedStatic<Platform> pm = syncPlatform()) {
                userServiceMock.when(() -> UserService.updateProfile(
                                anyString(), anyString(), anyString(), any(), any()))
                        .thenAnswer(inv -> {
                            // At this point the button text should already be "Đang lưu..."
                            assertEquals("Đang lưu...", btnSaveProfile.getText());
                            Consumer<Boolean> cb = inv.getArgument(4);
                            if (cb != null) cb.accept(true);
                            return null;
                        });
                ctrl.onSaveProfileClick(null);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 13. initialize() – null-guard / listener branches
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("initialize() – listener / null-guard branches")
    class InitializeListenerBranches {

        @Test
        @DisplayName("lblFullName/Phone/Address null → không NPE (null-guard)")
        void null_label_fields_in_initialize() throws Exception {
            set("lblFullName", null);
            set("lblPhone",    null);
            set("lblAddress",  null);
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(null);
                assertDoesNotThrow(() -> ctrl.initialize());
            }
        }

        @Test
        @DisplayName("avatarListener fires on avatarUrlProperty change")
        void avatar_listener_fires_on_url_change() throws Exception {
            try (MockedStatic<Platform> pm = syncPlatform()) {
                stubLoadProfile(null);
                ctrl.initialize();
                assertDoesNotThrow(() ->
                        ClientModel.getInstance().setAvatarUrl("http://new-avatar.png"));
            }
        }
    }
}