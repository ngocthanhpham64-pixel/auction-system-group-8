package vn.edu.vnu.uet.group8.client.util;

import java.util.Optional;
import java.util.logging.Logger;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;


/**
 * Tiện ích hiển thị các loại thông báo (Info,Error,Warning,Confirm).
 * - Thread-safe: có thể gọi từ bất kỳ thread nào ( tự động chuyển về FX Thread ).
 * - Final class, private constructor - không thể khởi tạo.
 *
 * <h3>FIX MEMORY LEAK:</h3>
 * Bản cũ dùng {@code ModalUtil.showSuccessModal()} cho mỗi lần showInfo/showError.
 * Mỗi lần gọi tạo 1 Stage mới + Scene mới + 2 FXML loads + copy 33KB CSS.
 * {@code Stage.initOwner()} giữ strong reference parent→child → child Stage KHÔNG
 * BAO GIỜ bị GC cho đến khi parent đóng → 50-60MB/lần × 10 OUTBID notifications = 500MB+ leak.
 *
 * <p>Bản mới dùng <b>global lightweight toast overlay</b>: 1 Label duy nhất được gắn vào
 * root scene, reuse mỗi lần gọi. Không tạo Stage/Scene mới. Auto-dismiss sau 4 giây.
 */
public final class AlertUtil {
    private static final Logger LOGGER = Logger.getLogger(AlertUtil.class.getName());

    /** Toast label toàn cục — reuse qua mọi lần gọi showInfo/showError */
    private static Label globalToast;
    /** Timer auto-dismiss đang chạy — reset nếu có toast mới đến trước khi hết hạn */
    private static PauseTransition dismissTimer;

    private AlertUtil(){}

    /**
     * Hiển thị thông báo thành công dạng toast nhẹ, tự ẩn sau 4 giây.
     * KHÔNG tạo Stage/Scene mới — reuse 1 Label trên root scene.
     */
    public static void showInfo(String message){
        runOnFX(() -> showToast(message, true));
    }
    
    /**
     * Hiển thị thông báo lỗi dạng toast nhẹ, tự ẩn sau 4 giây.
     * KHÔNG tạo Stage/Scene mới — reuse 1 Label trên root scene.
     */
    public static void showError(String message){
        runOnFX(() -> showToast(message, false));
    }

    public static void showWarning(String message){
        runOnFX(()->show(AlertType.WARNING,"Cảnh báo",message));
    }
    /**
     * Hiển thị hộp thoại xác nhận (YES/NO).
     * @ param title tiêu đề
     * @ param message nội dung
     * @ return true nếu người dùng chọn YES, ngược lại
     * @throws IllegalStateException nếu gọi từ thread không phải FX( vì cần trả về kết quả đồng bộ)
     */
    public static boolean showConfirm(String title,String message){
        if(!Platform.isFxApplicationThread()){
            LOGGER.severe("showConfirm() phải gọi từ FX Thread — trả về false");
            return false;
        }
        Alert alert = new Alert(AlertType.CONFIRMATION,message,ButtonType.YES,ButtonType.NO);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get()==ButtonType.YES;
    }

    // ========================================
    // GLOBAL TOAST — Lightweight notification
    // ========================================

    /**
     * Hiển thị toast overlay nhẹ trên root scene.
     * <ul>
     *   <li>Reuse 1 Label duy nhất — không tạo Node mới mỗi lần</li>
     *   <li>Tự ẩn sau 4 giây với fade-out animation</li>
     *   <li>Nếu toast mới đến trong khi toast cũ đang hiện, thay thế nội dung và reset timer</li>
     *   <li>Gắn vào root StackPane của SceneManager — không cần Stage riêng</li>
     * </ul>
     */
    private static void showToast(String message, boolean isSuccess) {
        if (SceneManager.getStage() == null || SceneManager.getStage().getScene() == null) {
            // Fallback nếu chưa có scene (ví dụ: trước khi login)
            show(isSuccess ? AlertType.INFORMATION : AlertType.ERROR,
                    isSuccess ? "Thông báo" : "Lỗi", message);
            return;
        }

        // Lấy root của scene — phải là StackPane (SceneManager dùng rootWrapper)
        javafx.scene.Parent sceneRoot = SceneManager.getStage().getScene().getRoot();
        if (!(sceneRoot instanceof StackPane rootPane)) {
            // Fallback nếu root không phải StackPane
            show(isSuccess ? AlertType.INFORMATION : AlertType.ERROR,
                    isSuccess ? "Thông báo" : "Lỗi", message);
            return;
        }

        // Lazy-init toast label (chỉ 1 lần duy nhất trong suốt đời app)
        if (globalToast == null) {
            globalToast = new Label();
            globalToast.setWrapText(true);
            globalToast.setMaxWidth(500);
            globalToast.setMouseTransparent(true); // Không chặn click chuột
            StackPane.setAlignment(globalToast, Pos.TOP_CENTER);
            globalToast.setTranslateY(20); // Cách đỉnh 20px
            globalToast.setVisible(false);
            globalToast.setManaged(false);
        }

        // Đảm bảo toast nằm trong root (có thể bị remove khi switchTo)
        if (!rootPane.getChildren().contains(globalToast)) {
            rootPane.getChildren().add(globalToast);
        }

        // Style theo loại thông báo
        if (isSuccess) {
            globalToast.setStyle(
                "-fx-background-color: linear-gradient(to right, #16a34a, #15803d);"
                + "-fx-text-fill: white; -fx-padding: 14 24; -fx-background-radius: 12;"
                + "-fx-font-size: 14px; -fx-font-weight: bold;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 5);"
            );
            globalToast.setText("✅  " + message);
        } else {
            globalToast.setStyle(
                "-fx-background-color: linear-gradient(to right, #dc2626, #b91c1c);"
                + "-fx-text-fill: white; -fx-padding: 14 24; -fx-background-radius: 12;"
                + "-fx-font-size: 14px; -fx-font-weight: bold;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 5);"
            );
            globalToast.setText("❌  " + message);
        }

        // Hiện toast
        globalToast.setOpacity(1.0);
        globalToast.setVisible(true);
        globalToast.setManaged(false); // Không ảnh hưởng layout

        // Đưa toast lên trên cùng
        globalToast.toFront();

        // Cancel timer cũ nếu đang chạy (toast mới thay thế toast cũ)
        if (dismissTimer != null) {
            dismissTimer.stop();
        }

        // Auto-dismiss sau 4 giây
        dismissTimer = new PauseTransition(Duration.seconds(4));
        dismissTimer.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), globalToast);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(ev -> {
                globalToast.setVisible(false);
            });
            fadeOut.play();
        });
        dismissTimer.play();
    }

    private static void show(AlertType type,String title,String msg){
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }
    // Đảm bảo action chạy trên FX Application Thread
    private static void runOnFX(Runnable action){
        if(Platform.isFxApplicationThread()){
            action.run();
        } else{
            Platform.runLater(action);
        }
    }
}
