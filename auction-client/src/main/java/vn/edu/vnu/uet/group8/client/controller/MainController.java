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
 */
public class MainController implements Initializable {

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
        loadView(SceneManager.VIEW_EXPLORE);
    }

    private void bindUserInfo() {
        if (lblUserName != null) {
            String name = SessionManager.getFullName();
            lblUserName.setText(name != null && !name.isBlank() ? name : SessionManager.getUsername());
        }

        if (lblUserAvatar != null) {
            lblUserAvatar.setText(SessionManager.getAvatarText());
        }

        if (lblBalance != null) {
            ClientModel.getInstance().balanceProperty().addListener((obs, old, newVal) -> {
                if (newVal != null) {
                    lblBalance.setText(String.format("%,.0f đ", newVal));
                }
            });
        }

        if (lblFavCount != null) {
            ClientModel.getInstance().favCountProperty().addListener((obs, old, newVal) ->
                    lblFavCount.setText(String.valueOf(newVal.intValue()))
            );
        }

        if (lblNotifCount != null) {
            int unread = ClientModel.getInstance().getUnreadNotificationCount();
            lblNotifCount.setText(String.valueOf(unread));
        }
    }

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

    // ===== HANDLERS =====

    @FXML private void onExploreClick()       { loadView(SceneManager.VIEW_EXPLORE); }
    @FXML private void onLiveClick()          { loadView(SceneManager.VIEW_LIVE_AUCTION); }
    @FXML private void onFavoritesClick()     { loadView(SceneManager.VIEW_FAVORITES); }
    @FXML private void onFavoriteClick()      { loadView(SceneManager.VIEW_FAVORITES); }
    @FXML private void onWalletClick()        { loadView(SceneManager.VIEW_WALLET); }
    @FXML private void onProfileClick()       { loadView(SceneManager.VIEW_PROFILE); }
    @FXML private void onNotificationClick()  { loadView(SceneManager.VIEW_NOTIFICATIONS); }
    @FXML private void onSettingsClick()      { loadView("SettingsView.fxml"); }
    @FXML private void onLogoClick()          { loadView(SceneManager.VIEW_EXPLORE); }

    @FXML
    private void onLogout() {
        boolean confirm = AlertUtil.showConfirm("Đăng xuất",
                "Bạn có chắc muốn đăng xuất?");
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
                loadView(routeStr);
            }
        }
    }
}