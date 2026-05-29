package vn.edu.vnu.uet.group8.client;

import javafx.application.Application;
import javafx.stage.Stage;
import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;

/**
 * Entry point ứng dụng Auctiva.
 *
 * <p>
 * Thứ tự khởi động:
 * <ol>
 * <li>Init SceneManager với primary stage.</li>
 * <li>Thử kết nối socket tới server — nếu thất bại vẫn mở app, không tắt.</li>
 * <li>Hiển thị màn hình đăng nhập.</li>
 * </ol>
 */
public class AuctionClientApp extends Application {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;

    @Override
    public void start(Stage primaryStage) {
        // 1. Khởi tạo SceneManager
        SceneManager.init(primaryStage);
        // Set icon cho app (hiện trên taskbar + title bar)
        try {
            primaryStage.getIcons().add(
                    new javafx.scene.image.Image(
                            getClass().getResourceAsStream("/image/logo.png")));
        } catch (Exception e) {
            // Bỏ qua nếu không tìm thấy logo - app vẫn chạy
        }
        primaryStage.setTitle("Auctiva - Live Online Auction");

        // Set kích thước cụ thể, không chỉ MIN
        primaryStage.setWidth(1280);
        primaryStage.setHeight(800);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(700);

        // Maximized: mở full màn hình ngay
        primaryStage.setMaximized(true);

        // Center on screen
        primaryStage.centerOnScreen();

        // 2. Kết nối server
        try {
            AuctionClient.getInstance().connect(SERVER_HOST, SERVER_PORT);
        } catch (Exception e) {
            AlertUtil.showWarning(
                    "Không thể kết nối server tại "
                            + SERVER_HOST + ":" + SERVER_PORT
                            + "\n" + e.getMessage()
                            + "\nKiểm tra server đã chạy chưa rồi thử lại.");
            // Tiếp tục mở app — không System.exit()
        }

        // 3. Hiển thị màn hình đăng nhập
        // ✅ Fix: file thực tế là "LoginView.fxml", không phải "login.fxml"
        SceneManager.switchTo(SceneManager.VIEW_LOGIN);
    }

    /**
     * ✅ Fix: JavaFX gọi stop() khi user đóng cửa sổ.
     * Dọn socket trước khi JVM thoát, tránh thread daemon bị treo.
     */
    @Override
    public void stop() {
        AuctionClient.getInstance().disconnect();
    }

    public static void main(String[] args) {
        launch(args);
    }
}