package vn.edu.vnu.uet.group8.client.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import vn.edu.vnu.uet.group8.client.service.AuthService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;
import vn.edu.vnu.uet.group8.client.util.SessionManager;

/**
 * AdminController — khung chính Admin Console.
 *
 * Tính năng:
 *  - Routing giữa Dashboard / User Management / Auction Management
 *  - Exit về MainLayout (cho admin quay lại trang user)
 *  - Logout về Login
 *  - Singleton pattern để Controller con gọi loadView()
 *  - Thread-safe loadView
 *
 * FXML handlers (giữ nguyên theo AdminLayout.fxml):
 *  - showDashboard, showUsers, showAuctions, exitAdmin
 */
public class AdminController implements Initializable {

    protected static final Logger LOGGER = Logger.getLogger(AdminController.class.getName());

    protected static AdminController instance;
    public static AdminController getInstance() { return instance; }

    @FXML StackPane contentPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        verifyAdminAccess();
        // Mặc định mở Dashboard
        loadView("AdminDashboardView.fxml");
    }

    /**
     * Bảo vệ — nếu không phải admin thì kick về Login.
     */
    void verifyAdminAccess() {
        if (!SessionManager.isAdmin()) {
            LOGGER.warning("Khong phai admin nhung vao duoc AdminLayout - kick ve Login");
            AlertUtil.showError("Ban khong co quyen truy cap Admin Console");
            SceneManager.switchTo(SceneManager.VIEW_LOGIN);
        }
    }

    // ===== ROUTING (FXML calls) =====

    @FXML
    public void showDashboard() {
        LOGGER.fine("Admin: showDashboard");
        loadView("AdminDashboardView.fxml");
    }

    @FXML
    public void showUsers() {
        LOGGER.fine("Admin: showUsers");
        loadView("AdminUserListView.fxml");
    }

    @FXML
    public void showAuctions() {
        LOGGER.fine("Admin: showAuctions");
        loadView("AdminAuctionListView.fxml");
    }

    @FXML
    public void exitAdmin() {
        boolean ok = AlertUtil.showConfirm("Thoát Admin Console",
                "Quay lại giao diện người dùng?");
        if (!ok) return;
        LOGGER.info("Admin thoat ve user UI");
        SceneManager.switchTo(SceneManager.VIEW_MAIN);
    }

    @FXML
    public void onLogout() {
        boolean ok = AlertUtil.showConfirm("Đăng xuất",
                "Bạn có chắc muốn đăng xuất?");
        if (!ok) return;
        LOGGER.info("Admin dang xuat");
        AuthService.logout();
    }

    // ===== LOAD VIEW =====

    /**
     * Load view con vào contentPane.
     * Thread-safe — tự chuyển về FX Thread nếu cần.
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
                LOGGER.warning("Khong tim thay admin view: " + fxmlFile);
                AlertUtil.showError("Không tìm thấy: " + fxmlFile);
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Node view = loader.load();
            contentPane.getChildren().setAll(view);
            LOGGER.fine(() -> "Admin loaded view: " + fxmlFile);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Loi load admin view: " + fxmlFile, e);
            AlertUtil.showError("Lỗi khi load: " + e.getMessage());
        }
    }
}