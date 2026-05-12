package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
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
 *
 * Trách nhiệm:
 *   - Hiển thị thông tin user từ SessionManager + ClientModel
 *   - Routing giữa các view (Explore, Profile, Wallet, ...)
 *   - Logout qua AuthService thật
 *   - Toggle sidebar
 *
 * Pattern: Load view con vào contentArea (center của BorderPane).
 */
public class MainController implements Initializable {

    // ===== FXML =====
    @FXML private BorderPane root;
    @FXML private VBox sidebar;
    @FXML private BorderPane contentArea;

    @FXML private Label lblUserName;
    @FXML private Label lblUserAvatar;
    @FXML private Label lblFavCount;
    @FXML private Label lblNotifCount;
    @FXML private Label lblBalance;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bindUserInfo();
        // Mặc định mở Explore
        loadView(SceneManager.VIEW_EXPLORE);
    }

    /**
     * Hiển thị thông tin user lên header (tên, avatar, số dư).
     */
    private void bindUserInfo() {
        // Tên user
        if (lblUserName != null) {
            String name = SessionManager.getFullName();
            lblUserName.setText(name != null && !name.isBlank() ? name : SessionManager.getUsername());
        }

        // Avatar (chữ cái đầu)
        if (lblUserAvatar != null) {
            lblUserAvatar.setText(SessionManager.getAvatarText());
        }

        // Số dư — listen theo ClientModel để tự update khi balance đổi
        if (lblBalance != null) {
            ClientModel.getInstance().balanceProperty().addListener((obs, old, newVal) -> {
                if (newVal != null) {
                    lblBalance.setText(String.format("%,.0f đ", newVal));
                }
            });
        }

        // Số yêu thích
        if (lblFavCount != null) {
            ClientModel.getInstance().favCountProperty().addListener((obs, old, newVal) ->
                    lblFavCount.setText(String.valueOf(newVal.intValue()))
            );
        }

        // Số thông báo chưa đọc
        if (lblNotifCount != null) {
            int unread = ClientModel.getInstance().getUnreadNotificationCount();
            lblNotifCount.setText(String.valueOf(unread));
        }
    }

    // ===== NAVIGATION =====

    /**
     * Load 1 view con vào center area của BorderPane.
     * Đây là method public để các Controller con có thể gọi từ ngoài.
     */
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
            contentArea.setCenter(view);
        } catch (IOException e) {
            AlertUtil.showError("Lỗi khi load view: " + e.getMessage());
        }
    }

    // ===== SIDEBAR ACTIONS =====

    @FXML private void onExploreClick()       { loadView(SceneManager.VIEW_EXPLORE); }
    @FXML private void onLiveClick()          { loadView(SceneManager.VIEW_LIVE_AUCTION); }
    @FXML private void onFavoritesClick()     { loadView(SceneManager.VIEW_FAVORITES); }
    @FXML private void onWalletClick()        { loadView(SceneManager.VIEW_WALLET); }
    @FXML private void onProfileClick()       { loadView(SceneManager.VIEW_PROFILE); }
    @FXML private void onNotificationClick()  { loadView(SceneManager.VIEW_NOTIFICATIONS); }
    @FXML private void onSettingsClick()      { loadView("SettingsView.fxml"); }

    @FXML
    private void onLogout() {
        boolean confirm = AlertUtil.showConfirm("Đăng xuất",
                "Bạn có chắc muốn đăng xuất?");
        if (!confirm) return;

        // AuthService.logout() đã handle: gửi server + clear session + navigate login
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
        // Generic handler — đọc userData để biết route nào
        Object source = event.getSource();
        if (source instanceof Button btn) {
            Object route = btn.getUserData();
            if (route instanceof String routeStr) {
                loadView(routeStr);
            }
        }
    }
}