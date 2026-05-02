package vn.edu.vnu.uet.group8.client.util;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quản lý chuyển màn hình(Scene) trong JavaFX
 * Thread-safe: switchTo() tự chuyển về FX Thread nếu cần.
 * Không cache root node-mỗi lần load mới để controller reset sạch.
 * Dùng switchTo() -> Chuyển màn, không cần truyền data vào controller
 * Dùng loadWithController() -> Chuyển màn và lấy controller để set data trước khi hiện
 */
public final class SceneManager {

    private static final Logger LOGGER = Logger.getLogger(SceneManager.class.getName());
    private static Stage primaryStage;
    private static String defaultTitle = "Auctiva - Live Online Auction";

    private SceneManager(){}

    public static void init(Stage stage){
        primaryStage = stage;
        if(defaultTitle != null) primaryStage.setTitle(defaultTitle);
    }
    /**
     * Chuyển sang màn hình mới, giữ nguyên title hiện tại
     */
    public static void switchTo(String fxmlFile){
        switchTo(fxmlFile,null); // null = không đổi title
    }
    /**
     * Chuyển sang màn hình mới và đổi title cho Stage.
     * @param fxmlFile tên file FXML( ví dụ "login.fxml")
     * @param title tiêu đề mới cho cửa sổ; nếu null thì giữ title cũ
     */
    public static void switchTo(String fxmlFile,String title){
        // Guard null/blank
        if(fxmlFile == null || fxmlFile.isBlank()){
            LOGGER.severe("fxmlFile không được null hoặc rỗng");
            return;
        }
        if(!Platform.isFxApplicationThread()){
            Platform.runLater(()-> switchTo(fxmlFile,title));
            return;
        }
        if(primaryStage == null){
            LOGGER.severe("SceneManager.init() chưa được gọi!");
            AlertUtil.showError("Lỗi hệ thống: không thể hiển thị màn hình. Vui lòng khởi động lại ứng dụng.");
            return;
        }
        try{
            URL url = SceneManager.class.getResource("/fxml/" + fxmlFile);
            if(url == null) throw new IOException("Không tìm thấy: /fxml/" + fxmlFile);
            Parent root = FXMLLoader.load(url);
            Scene scene = new Scene(root);
            inheritStylesheets(scene);
            primaryStage.setScene(scene);
            if(title != null) primaryStage.setTitle(title);
            primaryStage.show();
        } catch(IOException e){
            LOGGER.log(Level.SEVERE, "Lỗi tải FXML: "+fxmlFile,e);
            AlertUtil.showError("Không thể tải màn hình: "+fxmlFile);
        }
    }
    /**
     * Load FXML và trả về FXMLLoader để caller lấy controller/
     * - Chỉ gọi từ FX Thread-ném IllegalStateException nếu sai thread
     * Ví dụ:
     * FXMLLoader loader = SceneManager.loadWithController("main.fxml");
     * MainController ctrl = loader.getController();
     * ctrl.initData(someData);
     *
     * @param fxmlFile tên file FXML
     * @return FXMLLoader đã load ( có thể lấy controller)
     * @throws IOException nếu không tìm thấy file hoặc load thất bại
     * @throws IllegalStateException nếu gọi ngoiaf FX Thread hoặc chu init stage
     */
    public static FXMLLoader loadWithController(String fxmlFile) throws IOException{
        if(fxmlFile == null || fxmlFile.isBlank()){
            throw new IllegalArgumentException("fxmlFile không được null hoặc rỗng");
        }
        if(!Platform.isFxApplicationThread()){
            throw new IllegalStateException("loadWithController() phải gọi từ FX Thread");
        }
        if(primaryStage == null){
            throw new IllegalStateException("SceneManager chưa được init. Gọi SceneManager.init(stage) trước.");
        }
        URL url = SceneManager.class.getResource("/fxml/" + fxmlFile);
        if(url == null) throw new IOException("Không tìm thấy: /fxml/" + fxmlFile);
        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();
        Scene scene = new Scene(root);
        inheritStylesheets(scene);
        primaryStage.setScene(scene);
        primaryStage.show();
        // Lưu ý: overload set title không hỗ trợ ở đây vì caller cần set data trước khi hiển thị
        // Caller có th tự set title sau: SceneManager.getStage().setTitle("..")
        return loader;
    }
    public static Stage getStage() { return primaryStage;}

    public static void setDefaultTitle(String title){
        defaultTitle = title;
        if(primaryStage != null && primaryStage.getScene() != null){
            primaryStage.setTitle(title);
        }
    }
    // Kế thừa CSS từ scene cũ
    private static void inheritStylesheets(Scene newScene){
        if(primaryStage.getScene() != null){
            newScene.getStylesheets().addAll(primaryStage.getScene().getStylesheets());
        }
    }
}

