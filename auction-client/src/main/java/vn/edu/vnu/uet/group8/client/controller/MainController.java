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
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    // ===== FXML BINDINGS =====
    @FXML private VBox sidebar;
    @FXML private StackPane contentPane;
    @FXML private TextField tfSearch;

    // Header
    @FXML private Label lblAvatar;
    @FXML private Label lblFavCount;
    @FXML private Label lblNotiCount;
    @FXML private Button btnExplore;
    @FXML private Button btnLive;

    // Sidebar nav
    @FXML private Button btnHome;
    @FXML private Button btnMyAuctions;
    @FXML private Button btnWallet;
    @FXML private Button btnSettings;

    // ===== STATE =====
    private boolean sidebarVisible = true;
    private Button activeNavBtn;

    // ===== INIT =====
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Đặt nav active mặc định là Home
        activeNavBtn = btnHome;
        // Load trang Explore làm trang chủ mặc định
        loadView("ExploreView.fxml");
    }

    // ===== NAVIGATION =====

    /** Xử lý click các nút sidebar — dùng userData để phân biệt route */
    @FXML
    private void onNavClick(javafx.event.ActionEvent event) {
        Button clicked = (Button) event.getSource();
        setActiveNav(clicked);

        String route = (String) clicked.getUserData();
        switch (route) {
            case "HOME"         -> loadView("ExploreView.fxml");
            case "MY_AUCTIONS"  -> loadView("FavoriteView.fxml");
            case "WALLET"       -> loadView("WalletView.fxml");
            case "SETTINGS"     -> loadView("SettingsView.fxml");
        }
    }

    @FXML
    private void onExploreClick() {
        setActiveNav(btnHome);
        loadView("ExploreView.fxml");
    }

    @FXML
    private void onLiveClick() {
        loadView("LiveAuctionView.fxml");
    }

    @FXML
    private void onLogoClick() {
        setActiveNav(btnHome);
        loadView("ExploreView.fxml");
    }

    @FXML
    private void onFavoriteClick() {
        loadView("FavoriteView.fxml");
    }

    @FXML
    private void onNotificationClick() {
        loadView("NotificationView.fxml");
    }

    @FXML
    private void onProfileClick() {
        loadView("ProfileView.fxml");
    }

    @FXML
    private void onLogout() {
        // TODO: xóa session, quay về Login
        loadLoginScreen();
    }

    // ===== SIDEBAR TOGGLE =====

    @FXML
    private void onToggleSidebar() {
        sidebarVisible = !sidebarVisible;
        sidebar.setVisible(sidebarVisible);
        sidebar.setManaged(sidebarVisible);
    }

    // ===== HELPERS =====

    /**
     * Load một FXML view vào vùng content trung tâm.
     * Tất cả navigation đều đi qua hàm này.
     */
    public void loadView(String fxmlFile) {
        try {
            URL resource = getClass().getResource("/fxml/" + fxmlFile);
            if (resource == null) {
                System.err.println("[MainController] Không tìm thấy FXML: " + fxmlFile);
                return;
            }
            Node view = FXMLLoader.load(resource);
            contentPane.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("[MainController] Lỗi load view " + fxmlFile + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Đặt trạng thái active cho nút sidebar được chọn,
     * reset nút cũ về nav-item bình thường.
     */
    private void setActiveNav(Button target) {
        if (activeNavBtn != null) {
            activeNavBtn.getStyleClass().remove("nav-item-active");
            if (!activeNavBtn.getStyleClass().contains("nav-item")) {
                activeNavBtn.getStyleClass().add("nav-item");
            }
        }
        target.getStyleClass().remove("nav-item");
        if (!target.getStyleClass().contains("nav-item-active")) {
            target.getStyleClass().add("nav-item-active");
        }
        activeNavBtn = target;
    }

    /** Chuyển về màn hình Login (ví dụ khi logout) */
    private void loadLoginScreen() {
        try {
            URL resource = getClass().getResource("/fxml/LoginView.fxml");
            if (resource == null) return;
            Node loginView = FXMLLoader.load(resource);
            // Lấy Scene từ contentPane rồi thay root
            contentPane.getScene().setRoot((javafx.scene.Parent) loginView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
