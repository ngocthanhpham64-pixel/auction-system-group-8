package vn.edu.vnu.uet.group8.client.controller;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ModalWrapperController {

    @FXML
    private Label lblModalTitle;
    
    @FXML
    private StackPane contentArea;
    
    @FXML
    private StackPane windowRoot;

    private Stage modalStage;

    public void setTitle(String title) {
        lblModalTitle.setText(title != null ? title : "Thông báo");
    }

    public void setContent(Node content) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    public void setModalStage(Stage stage) {
        this.modalStage = stage;
    }

    @FXML
    public void onCloseModal() {
        if (modalStage != null) {
            // Animation mờ dần trước khi đóng
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), windowRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> modalStage.close());
            fadeOut.play();
        }
    }

    @FXML
    public void onBackgroundClick(MouseEvent event) {
        // Chỉ đóng nếu click trúng đúng vùng nền xám (không dính vào content VBox bên trong)
        if (event.getTarget() == windowRoot) {
            onCloseModal();
        }
    }
}
