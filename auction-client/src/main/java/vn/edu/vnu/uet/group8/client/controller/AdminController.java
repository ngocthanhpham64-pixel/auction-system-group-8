package vn.edu.vnu.uet.group8.client.controller;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import vn.edu.vnu.uet.group8.client.service.AuthService;
import vn.edu.vnu.uet.group8.client.util.SceneManager;

public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @FXML private StackPane contentPane;

    // FIX: Inject các nav button để quản lý active state động
    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnAuctions;

    @FXML
    public void initialize() {
        showDashboard();
    }

    @FXML
    public void showDashboard() {
        setActiveNav(btnDashboard);
        loadContent("AdminDashboardView.fxml");
    }

    @FXML
    public void showUsers() {
        setActiveNav(btnUsers);
        loadContent("AdminUserListView.fxml");
    }

    @FXML
    public void showAuctions() {
        setActiveNav(btnAuctions);
        loadContent("AdminAuctionListView.fxml");
    }

    @FXML
    public void exitAdmin() {
        AuthService.logout();
        SceneManager.switchTo("LoginView.fxml");
    }

    /**
     * FIX: Quản lý active state nav button theo cách động.
     * Gỡ nav-item-active khỏi tất cả button, rồi gán lại cho button đang chọn.
     * Tránh hardcode active state tĩnh trong FXML.
     */
    private void setActiveNav(Button activeButton) {
        Button[] navButtons = {btnDashboard, btnUsers, btnAuctions};
        for (Button btn : navButtons) {
            if (btn == null) continue;
            btn.getStyleClass().removeAll("nav-item-active");
            if (!btn.getStyleClass().contains("nav-item")) {
                btn.getStyleClass().add("nav-item");
            }
        }
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-item");
            if (!activeButton.getStyleClass().contains("nav-item-active")) {
                activeButton.getStyleClass().add("nav-item-active");
            }
        }
    }

    private void loadContent(String fxml) {
        try {
            var resource = getClass().getResource("/fxml/" + fxml);
            if (resource == null) throw new IOException("Không tìm thấy file FXML: " + fxml);

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            contentPane.getChildren().setAll(root);
        } catch (IOException e) {
            log.error("Failed to load admin content: {}", e.getMessage());
            showErrorCard(fxml, e.getMessage());
        }
    }

    private void showErrorCard(String fxml, String message) {
        VBox errorCard = new VBox(16);
        errorCard.setAlignment(Pos.CENTER);
        errorCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; -fx-padding: 40; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 20, 0, 0, 8);");
        errorCard.setMaxWidth(480);
        errorCard.setMaxHeight(260);

        Label iconLabel = new Label("⚠️");
        iconLabel.setStyle("-fx-font-size: 40px;");

        Label titleLabel = new Label("Không thể tải giao diện");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        Label msgLabel = new Label("Tệp: " + fxml + "\nChi tiết: " + message);
        msgLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-text-alignment: center;");
        msgLabel.setWrapText(true);

        Button btnRetry = new Button("Thử lại");
        btnRetry.setStyle("-fx-background-color: #F97316; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-padding: 10 24; -fx-background-radius: 8; -fx-cursor: hand;");
        btnRetry.setOnAction(evt -> loadContent(fxml));

        errorCard.getChildren().addAll(iconLabel, titleLabel, msgLabel, btnRetry);

        StackPane container = new StackPane(errorCard);
        container.setStyle("-fx-background-color: #F8FAFC;");
        contentPane.getChildren().setAll(container);
    }
}
