package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML
    private TextField tfEmail;
    @FXML
    private PasswordField tfPassword;
    @FXML
    private void onLogin() {
        String email = tfEmail.getText();
        String password = tfPassword.getText();
        if (email.isEmpty() || password.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Loi");
            alert.setHeaderText(null);
            alert.setContentText("Vui long nhap tai khoan va mat khau");
            alert.showAndWait();
            return;
        }
        if (email.equals("admin@auctiva.com") && password.equals("123456")) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thanh Cong");
            alert.setHeaderText(null);
            alert.setContentText("Chao mung ban den voi Auctiva!");
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Loi");
            alert.setHeaderText(null);
            alert.setContentText("Loi dang nhap");
            alert.showAndWait();
        }

    }

}