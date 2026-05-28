package vn.edu.vnu.uet.group8.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class SuccessContentController {
    @FXML private Button btnDone;
    @FXML private javafx.scene.control.Label lblSuccessTitle;
    @FXML private javafx.scene.control.Label lblSuccessMessage;

    public void setData(String title, String message) {
        if (lblSuccessTitle != null && title != null) {
            lblSuccessTitle.setText(title);
        }
        if (lblSuccessMessage != null && message != null) {
            lblSuccessMessage.setText(message);
        }
    }

    @FXML
    public void onDoneClick(ActionEvent event) {
        if (btnDone.getScene() != null) btnDone.getScene().getWindow().hide();
    }
}