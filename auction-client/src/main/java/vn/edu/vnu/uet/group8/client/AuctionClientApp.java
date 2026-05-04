package vn.edu.vnu.uet.group8.client;

import javafx.application.Application;
import javafx.stage.Stage;
import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.client.util.SceneManager;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;

/**
 * Ứng dụng khách hàng Auction.
 * Khởi tạo kết nối socket và hiển thị màn hình đăng nhập.
 */
public class AuctionClientApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneManager.init(primaryStage);
        primaryStage.setTitle("Auctiva - Live Online Auction");
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(700);

        try {
            // Kết nối tới server (có thể đọc từ file cấu hình)
                AuctionClient.getInstance().connect("localhost", 12345);
        } catch (Exception e) {
            AlertUtil.showError("Cannot connect to server: " + e.getMessage());
            System.exit(1);
        }

        SceneManager.switchTo("login.fxml");
    }

    public static void main(String[] args) {
        launch(args);
    }
}