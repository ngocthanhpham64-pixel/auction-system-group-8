package vn.edu.vnu.uet.group8.client.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    @FXML private Button btnSeller;
    @FXML private Button btnSettings;
    @FXML private StackPane contentPane;

    private Button activeNav;
    private static MainController instance;
    private ScheduledExecutorService timerScheduler; // Khai bao de quan ly tap trung

    public static MainController getInstance() {
        return instance;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        bindUserInfo();
        bindBadges();
        setupSearch();
        
        // Mặc định chọn Home
        setActiveNav(btnHome);
        loadView(SceneManager.VIEW_EXPLORE);
    }

    /** 
     * Gan scheduler tu ben ngoai vao de MainController co the shutdown khi logout.
     */
    public void setTimerScheduler(ScheduledExecutorService scheduler) {
        this.timerScheduler = scheduler;
    }

    private void bindUserInfo() {
        ClientModel.getInstance().currentUserProperty().addListener((obs, oldUser, newUser) -> 
            Platform.runLater(() -> {
                if (newUser != null && lblAvatar != null) {
                    String name = newUser.getFullName() != null ? newUser.getFullName() : newUser.getUsername();
                    if (name != null && !name.isEmpty()) {
                        lblAvatar.setText(name.substring(0, 1).toUpperCase());
                    }
                }
            })
        );

        if (lblAvatar != null) {
            String avatarText = SessionManager.getAvatarText();
            if (avatarText != null && !avatarText.isBlank()) {
                lblAvatar.setText(avatarText);
            }
        }
    }

    private void bindBadges() {
        ClientModel model = ClientModel.getInstance();
        if (lblFavCount != null) {
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
        if (lblFavCount == null) return;
        lblFavCount.setText(String.valueOf(count));
        lblFavCount.setVisible(count > 0);
        lblFavCount.setManaged(count > 0);
    }

    private void updateNotiBadge(int count) {
        if (lblNotiCount == null) return;
        lblNotiCount.setText(String.valueOf(count));
        lblNotiCount.setVisible(count > 0);
        lblNotiCount.setManaged(count > 0);
    }

    private void setupSearch() {
        if (tfSearch == null) return;
        tfSearch.setOnAction(e -> {
            String query = tfSearch.getText().trim();
            ClientModel.getInstance().setSearchQuery(query);
            loadView(SceneManager.VIEW_EXPLORE);
        });
    }

    public void loadView(String fxmlFile) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> loadView(fxmlFile));
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
            Node view = loader.load();
            contentPane.getChildren().setAll(view);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Loi khi load view: " + fxmlFile, e);
        }
    }

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

    @FXML
    private void onLogout() {
        boolean ok = AlertUtil.showConfirm("Xác nhận", "Bạn có chắc chắn muốn đăng xuất?");
        if (!ok) return;

        cleanupResources();
        AuthService.logout();
    }

    /** 
     * Don dep tai nguyen truoc khi thoat de tránh Memory Leak.
     */
    private void cleanupResources() {
        if (timerScheduler != null) {
            try {
                timerScheduler.shutdownNow();
                timerScheduler = null; // Tranh goi lan 2
                LOGGER.info("TimerScheduler has been shut down.");
            } catch (Exception e) {
                LOGGER.warning("Error shutting down timerScheduler: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onNavClick(javafx.event.ActionEvent event) {
        Object source = event.getSource();
        if (!(source instanceof Button btn)) return;

        String route = (String) btn.getUserData();
        if (route == null) return;

        setActiveNav(btn);
        switch (route) {
            case "HOME"        -> loadView(SceneManager.VIEW_EXPLORE);
            case "EXPLORE"     -> loadView(SceneManager.VIEW_EXPLORE);
            case "LIVE"        -> loadView(SceneManager.VIEW_LIVE_AUCTION);
            case "MY_AUCTIONS" -> loadView(SceneManager.VIEW_PROFILE);
            case "WALLET"      -> loadView(SceneManager.VIEW_WALLET);
            case "SELLER"      -> loadView("SellerDashboardView.fxml");
            case "SETTINGS"    -> loadView(SceneManager.VIEW_SETTINGS);
            default            -> loadView(route);
        }
    }

    @FXML private void onLogoClick()         { setActiveNav(btnHome); loadView(SceneManager.VIEW_EXPLORE); }
    @FXML private void onFavoriteClick()     { loadView(SceneManager.VIEW_FAVORITES); }
    @FXML private void onNotificationClick() { loadView(SceneManager.VIEW_NOTIFICATIONS); }
    @FXML private void onProfileClick()      { loadView(SceneManager.VIEW_PROFILE); }
    @FXML private void onToggleSidebar()     { sidebar.setVisible(!sidebar.isVisible()); sidebar.setManaged(sidebar.isVisible()); }
}
