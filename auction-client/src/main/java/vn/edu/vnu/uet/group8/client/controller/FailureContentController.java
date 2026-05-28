package vn.edu.vnu.uet.group8.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class FailureContentController {
    @FXML private Button btnRetry;
    @FXML private javafx.scene.control.Label lblFailureTitle;
    @FXML private javafx.scene.control.Label lblFailureMessage;

    public void setData(String title, String message) {
        if (lblFailureTitle != null && title != null) {
            lblFailureTitle.setText(title);
        }
        if (lblFailureMessage != null && message != null) {
            lblFailureMessage.setText(message);
        }
    }

    @FXML
    public void onRetryClick(ActionEvent event) {
        if (btnRetry.getScene() != null) btnRetry.getScene().getWindow().hide();
    }
}
