package vn.edu.vnu.uet.group8.client.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

import java.util.Optional;
import java.util.logging.Logger;


/**
 * Tiện ích hiển thị các loại thông báo (Info,Error,Warning,Confirm).
 * - Thread-safe: có thể gọi từ bất kỳ thread nào ( tự động chuyển vể FX Thread ).
 * - Final class, private constructor - không thể khởi tạo.
 */
public final class AlertUtil {
    private static final Logger LOGGER = Logger.getLogger(AlertUtil.class.getName());

    private AlertUtil(){}

    public static void showInfo(String message){
        runOnFX(()->show(AlertType.INFORMATION,"Thông báo",message));
    }
    public static void showError(String message){
        runOnFX(()->show(AlertType.ERROR,"Lỗi",message));
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
    private static void show(AlertType type,String title,String msg){
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
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
