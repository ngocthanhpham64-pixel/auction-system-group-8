package vn.edu.vnu.uet.group8.client.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import vn.edu.vnu.uet.group8.client.service.AuthService;

public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @FXML private StackPane contentPane;

    @FXML
    public void initialize() {
        showDashboard();
    }

    @FXML
    public void showDashboard() {
        loadContent("AdminDashboardView.fxml");
    }

    @FXML
    public void showUsers() {
        loadContent("AdminUserListView.fxml");
    }

    @FXML
    public void showAuctions() {
        loadContent("AdminAuctionListView.fxml");
    }

    @FXML
    public void exitAdmin() {
        AuthService.logout();
    }

    private void loadContent(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxml));
            Parent root = loader.load();
            contentPane.getChildren().setAll(root);
        } catch (IOException e) {
            log.error("Failed to load admin content: {}", e.getMessage());
        }
    }
}
