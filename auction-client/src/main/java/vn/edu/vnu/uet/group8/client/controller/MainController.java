package vn.edu.vnu.uet.group8.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.AuthService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;
import vn.edu.vnu.uet.group8.client.util.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MainController — khung chinh sau khi login.
 *
 * Trach nhiem:
 *  - Hien thi user info (avatar, fav count, noti count) tu SessionManager + ClientModel
 *  - Binding property -> tu dong update khi data thay doi
 *  - Routing giua cac view bang loadView()
 *  - Thread-safe loadView (chay tren FX Thread)
 *
 * Cap nhat realtime:
 *  - lblFavCount tu binding voi favCountProperty
 *  - lblNotiCount tu binding voi unreadNotificationCountProperty
 *  - lblAvatar tu binding voi currentUser
 */
public class MainController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    @FXML private Button btnToggle;
    @FXML private TextField tfSearch;
    @FXML private Button btnExplore;
    @FXML private Button btnLive;
    @FXML private Label lblFavCount;
    @FXML private Label lblNotiCount;
    @FXML private Label lblAvatar;
    @FXML private VBox sidebar;
    @FXML private Button btnHome;
    @FXML private Button btnMyAuctions;
    @FXML private Button btnWallet;
    @FXML private Button btnSettings;
    @FXML private StackPane contentPane;

    private Button activeNav;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bindUserInfo();
        bindBadges();
        setupSearch();
        setActiveNav(btnHome);
        loadView(SceneManager.VIEW_EXPLORE);
    }

    // ===== BINDING =====

    /** Hien thi avatar tu SessionManager. */
    private void bindUserInfo() {
        if (lblAvatar != null) {
            String avatarText = SessionManager.getAvatarText();
            if (avatarText != null && !avatarText.isBlank()) {
                lblAvatar.setText(avatarText);
            }
        }
    }

    /** Binding badge -> tu cap nhat khi ClientModel thay doi. */
    private void bindBadges() {
        ClientModel model = ClientModel.getInstance();

        if (lblFavCount != null) {
            // Set lan dau + listen thay doi
            updateFavBadge(model.getFavCount());
            model.favCountProperty().addListener((obs, oldVal, newVal) ->
                    Platform.runLater(() -> updateFavBadge(newVal.intValue()))
            );
        }

        if (lblNotiCount != null) {
            updateNotiBadge(model.getUnreadNotificationCount());
            model.unreadNotificationCountProperty().addListener((obs, oldVal, newVal) ->
                    Platform.runLater(() -> updateNotiBadge(newVal.intValue()))
            );
        }
    }

    private void updateFavBadge(int count) {
        lblFavCount.setText(String.valueOf(count));
        lblFavCount.setVisible(count > 0);
        lblFavCount.setManaged(count > 0);
    }

    private void updateNotiBadge(int count) {
        lblNotiCount.setText(String.valueOf(count));
        lblNotiCount.setVisible(count > 0);
        lblNotiCount.setManaged(count > 0);
    }

    /** Search debounce — Enter de submit. */
    private void setupSearch() {
        if (tfSearch == null) return;
        tfSearch.setOnAction(e -> {
            String query = tfSearch.getText().trim();
            if (query.isEmpty()) return;
            LOGGER.info(() -> "Tim kiem: " + query);
            loadView(SceneManager.VIEW_EXPLORE);
            // TODO: pass query to ExploreController
        });
    }

    // ===== NAVIGATION =====

    /**
     * Load view con vao contentPane.
     * Thread-safe: tu chuyen ve FX Thread neu can.
     */
    public void loadView(String fxmlFile) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> loadView(fxmlFile));
            return;
        }
        try {
            String path = "/fxml/" + fxmlFile;
            URL resource = getClass().getResource(path);
            if (resource == null) {
                LOGGER.warning("Khong tim thay view: " + fxmlFile);
                AlertUtil.showError("Khong tim thay view: " + fxmlFile);
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Node view = loader.load();
            contentPane.getChildren().setAll(view);
            LOGGER.fine(() -> "Loaded view: " + fxmlFile);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Loi khi load view: " + fxmlFile, e);
            AlertUtil.showError("Loi khi load view: " + e.getMessage());
        }
    }

    /** Set active state cho nav button. */
    private void setActiveNav(Button target) {
        if (target == null) return;
        if (activeNav != null) {
            activeNav.getStyleClass().remove("nav-item-active");
            if (!activeNav.getStyleClass().contains("nav-item")) {
                activeNav.getStyleClass().add("nav-item");
            }
        }
        target.getStyleClass().remove("nav-item");
        if (!target.getStyleClass().contains("nav-item-active")) {
            target.getStyleClass().add("nav-item-active");
        }
        activeNav = target;
    }

    // ===== onAction =====

    @FXML
    private void onExploreClick() {
        setActiveNav(btnHome);
        loadView(SceneManager.VIEW_EXPLORE);
    }

    @FXML
    private void onLiveClick() {
        loadView(SceneManager.VIEW_LIVE_AUCTION);
    }

    @FXML
    private void onLogout() {
        boolean ok = AlertUtil.showConfirm("Dang xuat", "Ban co chac muon dang xuat?");
        if (!ok) return;
        LOGGER.info("Nguoi dung dang xuat");
        AuthService.logout();
    }

    @FXML
    private void onToggleSidebar() {
        boolean visible = sidebar.isVisible();
        sidebar.setVisible(!visible);
        sidebar.setManaged(!visible);
        LOGGER.fine(() -> "Sidebar visible: " + !visible);
    }

    @FXML
    private void onNavClick(javafx.event.ActionEvent event) {
        Object source = event.getSource();
        if (!(source instanceof Button btn)) return;

        Object route = btn.getUserData();
        if (!(route instanceof String routeStr)) return;

        setActiveNav(btn);
        switch (routeStr) {
            case "HOME"        -> loadView(SceneManager.VIEW_EXPLORE);
            case "MY_AUCTIONS" -> loadView(SceneManager.VIEW_PROFILE);
            case "WALLET"      -> loadView(SceneManager.VIEW_WALLET);
            case "SETTINGS"    -> loadView(SceneManager.VIEW_SETTINGS);
            default            -> loadView(routeStr);
        }
    }

    // ===== onMouseClicked =====

    @FXML private void onLogoClick()         { onExploreClick(); }
    @FXML private void onFavoriteClick()     { loadView(SceneManager.VIEW_FAVORITES); }
    @FXML private void onNotificationClick() { loadView(SceneManager.VIEW_NOTIFICATIONS); }
    @FXML private void onProfileClick()      { loadView(SceneManager.VIEW_PROFILE); }
}