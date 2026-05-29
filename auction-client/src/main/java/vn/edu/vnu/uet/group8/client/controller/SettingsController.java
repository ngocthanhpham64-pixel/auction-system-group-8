package vn.edu.vnu.uet.group8.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import vn.edu.vnu.uet.group8.client.util.SceneManager;

public class SettingsController {

    @FXML private Button btnLightMode;
    @FXML private Button btnDarkMode;

    @FXML
    public void initialize() {
        // Xác định theme đang chạy lúc bật màn Cài đặt để tô sáng đúng nút
        updateButtonStyles("light-theme.css".equals(SceneManager.getCurrentTheme()));
    }

    @FXML
    public void onLightModeClick(ActionEvent event) {
        SceneManager.setTheme("light-theme.css");
        updateButtonStyles(true);
    }

    @FXML
    public void onDarkModeClick(ActionEvent event) {
        SceneManager.setTheme("dark-theme.css");
        updateButtonStyles(false);
    }

    private void updateButtonStyles(boolean isLight) {
        btnLightMode.getStyleClass().removeAll("toggle-btn-active", "toggle-btn-inactive");
        btnDarkMode.getStyleClass().removeAll("toggle-btn-active", "toggle-btn-inactive");
        if (isLight) {
            btnLightMode.getStyleClass().add("toggle-btn-active");
            btnDarkMode.getStyleClass().add("toggle-btn-inactive");
        } else {
            btnLightMode.getStyleClass().add("toggle-btn-inactive");
            btnDarkMode.getStyleClass().add("toggle-btn-active");
        }
    }
}
