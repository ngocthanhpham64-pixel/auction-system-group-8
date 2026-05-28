package vn.edu.vnu.uet.group8.client.util;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Popup;

public final class PopupUtil {
    private static final Logger LOGGER = Logger.getLogger(PopupUtil.class.getName());

    private PopupUtil() {}

    public static void showPopup(Node anchorNode, String fxmlPath) {
        try {
            URL url = PopupUtil.class.getResource("/fxml/" + fxmlPath);
            if (url == null) throw new IOException("Cannot find " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            Popup popup = new Popup();
            popup.getContent().add(root);
            
            // Tự động đóng popup khi nhấn ra ngoài nền màn hình
            popup.setAutoHide(true);
            // Tự động điều chỉnh vị trí để không bị tràn khỏi mép màn hình
            popup.setAutoFix(true);
            
            // Kế thừa CSS từ màn hình chính
            if (anchorNode.getScene() != null && anchorNode.getScene().getStylesheets() != null) {
                root.getStylesheets().addAll(anchorNode.getScene().getStylesheets());
            }

            // Lấy toạ độ tuyệt đối của icon trên màn hình
            Bounds bounds = anchorNode.localToScreen(anchorNode.getBoundsInLocal());
            if (bounds != null) {
                // Đặt Y ngay bên dưới mép icon, X lùi về trái một chút để canh giữa box
                popup.show(anchorNode.getScene().getWindow(), bounds.getMinX() - 320, bounds.getMaxY() + 10);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tải popup: " + fxmlPath, e);
            AlertUtil.showError("Không thể hiển thị tính năng này.");
        }
    }
}
