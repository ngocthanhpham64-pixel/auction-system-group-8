package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    // ===== FXML BINDINGS =====
    @FXML private Label lblAvatar;
    @FXML private Label lblName;
    @FXML private Label lblEmail;
    @FXML private Label lblJoinDate;
    @FXML private Label lblTotalBids;
    @FXML private Label lblWonBids;
    @FXML private Label lblActiveBids;
    @FXML private Label lblRating;
    @FXML private Label lblBalance;

    // Tabs lịch sử
    @FXML private Button btnTabActive;
    @FXML private Button btnTabWon;
    @FXML private Button btnTabLost;
    @FXML private VBox bidHistoryList;

    // ===== STATE =====
    private Button activeTab;
    /** "active" | "won" | "lost" */
    private String currentFilter = "active";

    // ===== INIT =====
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeTab = btnTabActive;
        // TODO: load dữ liệu user từ session/server
        loadProfileData();
        loadBidHistory();
    }

    // ===== LOAD DATA =====

    private void loadProfileData() {
        // TODO: thay bằng dữ liệu thật từ session
        lblName.setText("Nguyễn Văn A");
        lblEmail.setText("nguyenvana@email.com");
        lblJoinDate.setText("Tham gia tháng 3/2024");
        lblAvatar.setText("N");
        lblTotalBids.setText("234");
        lblWonBids.setText("45");
        lblActiveBids.setText("12");
        lblRating.setText("4.8");
        lblBalance.setText("50.000.000 đ");
    }

    // ===== TABS LỊCH SỬ =====

    @FXML private void onTabActive() { setTab(btnTabActive, "active"); loadBidHistory(); }
    @FXML private void onTabWon()    { setTab(btnTabWon,    "won");    loadBidHistory(); }
    @FXML private void onTabLost()   { setTab(btnTabLost,   "lost");   loadBidHistory(); }

    private void setTab(Button target, String filter) {
        currentFilter = filter;
        if (activeTab != null) {
            activeTab.getStyleClass().remove("tag-active");
            if (!activeTab.getStyleClass().contains("tag-inactive")) {
                activeTab.getStyleClass().add("tag-inactive");
            }
        }
        target.getStyleClass().remove("tag-inactive");
        if (!target.getStyleClass().contains("tag-active")) {
            target.getStyleClass().add("tag-active");
        }
        activeTab = target;
    }

    private void loadBidHistory() {
        bidHistoryList.getChildren().clear();
        // TODO: load dữ liệu thật từ server theo currentFilter
        System.out.println("[ProfileController] Load lịch sử filter=" + currentFilter);
    }

    // ===== ACTIONS =====

    @FXML
    private void onDeposit() {
        // TODO: điều hướng sang WalletView
        System.out.println("[ProfileController] Nạp tiền");
    }

    @FXML
    private void onSettings() {
        // TODO: điều hướng sang SettingsView qua MainController
        System.out.println("[ProfileController] Mở cài đặt");
    }

    @FXML
    private void onLogout() {
        try {
            URL resource = getClass().getResource("/fxml/LoginView.fxml");
            if (resource == null) return;
            Parent login = FXMLLoader.load(resource);
            Stage stage = (Stage) lblName.getScene().getWindow();
            stage.setScene(new Scene(login));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
