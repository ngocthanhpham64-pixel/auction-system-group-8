package vn.edu.vnu.uet.group8.client.util;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import vn.edu.vnu.uet.group8.client.controller.FailureContentController;
import vn.edu.vnu.uet.group8.client.controller.ModalWrapperController;
import vn.edu.vnu.uet.group8.client.controller.SuccessContentController;

public final class ModalUtil {
    private static final Logger LOGGER = Logger.getLogger(ModalUtil.class.getName());

    private ModalUtil() {}

    public static void showSuccessModal(String title, String message) {
        showModal(title, message, "SuccessContent.fxml");
    }

    public static void showFailureModal(String title, String message) {
        showModal(title, message, "FailureContent.fxml");
    }

    public static void showModal(String title, String contentFxml) {
        showModal(title, null, contentFxml);
    }

    public static void showModal(String title, String message, String contentFxml) {
        try {
            // 1. Tải khung ModalWrapper
            URL wrapperUrl = ModalUtil.class.getResource("/fxml/ModalWrapper.fxml");
            if (wrapperUrl == null) throw new IOException("Cannot find ModalWrapper.fxml");
            FXMLLoader wrapperLoader = new FXMLLoader(wrapperUrl);
            Parent wrapperRoot = wrapperLoader.load();

            ModalWrapperController wrapperController = wrapperLoader.getController();
            if (wrapperController != null) {
                wrapperController.setTitle(title);
            }

            // 2. Tải nội dung truyền vào (Ví dụ: DepositContent.fxml)
            URL contentUrl = ModalUtil.class.getResource("/fxml/" + contentFxml);
            if (contentUrl == null) throw new IOException("Cannot find " + contentFxml);
            FXMLLoader contentLoader = new FXMLLoader(contentUrl);
            Parent contentRoot = contentLoader.load();

            // Nhúng dữ liệu động cho SuccessContent và FailureContent nếu được sử dụng
            if ("SuccessContent.fxml".equals(contentFxml)) {
                SuccessContentController ctrl = contentLoader.getController();
                if (ctrl != null) {
                    ctrl.setData(title, message != null ? message : "Dữ liệu đã được hệ thống xử lý và cập nhật an toàn.");
                }
            } else if ("FailureContent.fxml".equals(contentFxml)) {
                FailureContentController ctrl = contentLoader.getController();
                if (ctrl != null) {
                    ctrl.setData(title, message != null ? message : "Đã xảy ra lỗi trong quá trình xử lý dữ liệu hệ thống.");
                }
            }

            // 3. Nhúng nội dung vào khung
            if (wrapperController != null) {
                wrapperController.setContent(contentRoot);
            }

            // 4. Hiển thị Stage dưới dạng Popup (Modal)
            Stage modalStage = new Stage();
            modalStage.initOwner(SceneManager.getStage()); // Cửa sổ cha
            modalStage.initModality(Modality.APPLICATION_MODAL); // Chặn tương tác cửa sổ cha
            modalStage.initStyle(StageStyle.TRANSPARENT); // Bo góc & trong suốt nền ngoài

            Scene scene = new Scene(wrapperRoot);
            scene.setFill(Color.TRANSPARENT);
            
            // Kế thừa CSS từ màn hình chính
            if (SceneManager.getStage() != null && SceneManager.getStage().getScene() != null) {
                scene.getStylesheets().addAll(SceneManager.getStage().getScene().getStylesheets());
            }

            modalStage.setScene(scene);
            
            // Set kích thước overlay phủ kín màn hình chính
            Stage owner = SceneManager.getStage();
            if (owner != null) {
                modalStage.setX(owner.getX());
                modalStage.setY(owner.getY());
                modalStage.setWidth(owner.getWidth());
                modalStage.setHeight(owner.getHeight());
            }

            // Truyền Stage cho controller để gọi sự kiện Đóng (Close)
            if (wrapperController != null) {
                wrapperController.setModalStage(modalStage);
            }

            // Animation hiện Modal mượt mà
            wrapperRoot.setOpacity(0.0);
            modalStage.setOnShown(e -> {
                FadeTransition fadeIn = new FadeTransition(Duration.millis(250), wrapperRoot);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });

            modalStage.showAndWait();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load modal: " + contentFxml, e);
            // Hủy đệ quy vô hạn: Không dùng AlertUtil/ModalUtil ở đây. Dùng Alert thuần làm fallback.
            javafx.application.Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Lỗi");
                alert.setHeaderText("Không thể tải giao diện hộp thoại");
                alert.setContentText("Không thể nạp " + contentFxml + "\nChi tiết: " + e.getMessage());
                alert.showAndWait();
            });
        }
    }
}
