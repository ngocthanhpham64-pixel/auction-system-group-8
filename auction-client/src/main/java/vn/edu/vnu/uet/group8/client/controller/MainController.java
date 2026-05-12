package vn.edu.vnu.uet.group8.client.controller;

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

/**
 * MainController — Khung chính của app sau khi login.
 * Các fx:id phải khớp với MainLayout.fxml.
 */
public class MainController implements Initializable {

    // ===== FXML — khớp với MainLayout.fxml =====
    @FXML private Button btnToggle;
    @FXML private TextField tfSearch;
    @FXML private Button btnExplore;
    @FXML private Button btnLive;

    @FXML private Label lblFavCount;
    @FXML private Label lblNotiCount;   // FXML là "NotiCount" KHÔNG có 'f'
    @FXML private Label lblAvatar;

    @FXML private VBox sidebar;
    @FXML private Button btnHome;
    @FXML private Button btnMyAuctions;
    @FXML private Button btnWallet;
    @FXML private Button btnSettings;

    @FXML private StackPane contentPane;   // FXML là "contentPane" KHÔNG phải "contentArea"

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bindUserInfo();
        // Mặc định mở Explore
        loadView(SceneManager.VIEW_EXPLORE);
    }

    private void bindUserInfo() {
        // Avatar (chữ cái đầu của username)
        if (lblAvatar != null) {
            String avatarText = SessionManager.getAvatarText();
            if (avatarText != null && !avatarText.isBlank()) {
                lblAvatar.setText(avatarText);
            }
        }

        // Số yêu thích
        if (lblFavCount != null) {
            ClientModel.getInstance().favCountProperty().addListener((obs, old, newVal) ->
                    lblFavCount.setText(String.valueOf(newVal.intValue()))
            );
        }

        // Số thông báo chưa đọc
        if (lblNotiCount != null) {
            int unread = ClientModel.getInstance().getUnreadNotificationCount();
            lblNotiCount.setText(String.valueOf(unread));
        }
    }

    // ===== NAVIGATION =====

    public void loadView(String fxmlFile) {
        try {
            String path = "/fxml/" + fxmlFile;
            URL resource = getClass().getResource(path);
            if (resource == null) {
                AlertUtil.showError("Không tìm thấy view: " + fxmlFile);
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Node view = loader.load();
            contentPane.getChildren().setAll(view);   // StackPane dùng getChildren()
        } catch (IOException e) {
            AlertUtil.showError("Lỗi khi load view: " + e.getMessage());
        }
    }

    // ===== HANDLERS =====

    @FXML private void onExploreClick()       { loadView(SceneManager.VIEW_EXPLORE); }
    @FXML private void onLiveClick()          { loadView(SceneManager.VIEW_LIVE_AUCTION); }
    @FXML private void onFavoriteClick()      { loadView(SceneManager.VIEW_FAVORITES); }
    @FXML private void onWalletClick()        { loadView(SceneManager.VIEW_WALLET); }
    @FXML private void onProfileClick()       { loadView(SceneManager.VIEW_PROFILE); }
    @FXML private void onNotificationClick()  { loadView(SceneManager.VIEW_NOTIFICATIONS); }
    @FXML private void onSettingsClick()      { loadView(SceneManager.VIEW_SETTINGS); }
    @FXML private void onLogoClick()          { loadView(SceneManager.VIEW_EXPLORE); }

    @FXML
    private void onLogout() {
        boolean confirm = AlertUtil.showConfirm("Đăng xuất", "Bạn có chắc muốn đăng xuất?");
        if (!confirm) return;
        AuthService.logout();
    }

    @FXML
    private void onToggleSidebar() {
        boolean visible = sidebar.isVisible();
        sidebar.setVisible(!visible);
        sidebar.setManaged(!visible);
    }

    @FXML
    private void onNavClick(javafx.event.ActionEvent event) {
        Object source = event.getSource();
        if (source instanceof Button btn) {
            Object route = btn.getUserData();
            if (route instanceof String routeStr) {
                switch (routeStr) {
                    case "HOME"        -> loadView(SceneManager.VIEW_EXPLORE);
                    case "MY_AUCTIONS" -> loadView(SceneManager.VIEW_PROFILE);
                    case "WALLET"      -> loadView(SceneManager.VIEW_WALLET);
                    case "SETTINGS"    -> loadView(SceneManager.VIEW_SETTINGS);
                    default            -> loadView(routeStr);
                }
            }
        }
    }
}