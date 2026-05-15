package vn.edu.vnu.uet.group8.client;

import javafx.application.Application;
import javafx.stage.Stage;
import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;

/**
 * Entry point ứng dụng Auctiva.
 *
 * <p>Thứ tự khởi động:
 * <ol>
 *   <li>Init SceneManager với primary stage.</li>
 *   <li>Thử kết nối socket tới server — nếu thất bại vẫn mở app, không tắt.</li>
 *   <li>Hiển thị màn hình đăng nhập.</li>
 * </ol>
 */
public class AuctionClientApp extends Application {

    private static final String SERVER_HOST = "localhost";
    private static final int    SERVER_PORT = 8080;

    @Override
    public void start(Stage primaryStage) {
        // 1. Khởi tạo SceneManager
        SceneManager.init(primaryStage);
        primaryStage.setTitle("Auctiva - Live Online Auction");
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(700);

        // 2. Kết nối server
        // ✅ Fix: bỏ System.exit(1) — app vẫn mở dù server chưa chạy.
        //    LoginController sẽ tự báo lỗi khi user bấm đăng nhập.
        try {
            AuctionClient.getInstance().connect(SERVER_HOST, SERVER_PORT);
        } catch (Exception e) {
            AlertUtil.showWarning(
                    "Không thể kết nối server tại "
                            + SERVER_HOST + ":" + SERVER_PORT
                            + "\n" + e.getMessage()
                            + "\nKiểm tra server đã chạy chưa rồi thử lại."
            );
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