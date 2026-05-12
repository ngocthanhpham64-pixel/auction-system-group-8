package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import vn.edu.vnu.uet.group8.client.service.AuthService;
import vn.edu.vnu.uet.group8.client.service.UserService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.BidRecord;
import vn.edu.vnu.uet.group8.common.dto.LoginResponse;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * ProfileController — wire UserService thật.
 *
 * Hiển thị:
 *   - Profile: tên, email, role
 *   - Số dư từ ClientModel
 *   - Tabs: Đang đấu giá / Đã thắng / Đã thua — load từ BidService
 */
public class ProfileController implements Initializable {

    @FXML private Label lblFullName;
    @FXML private Label lblUsername;
    @FXML private Label lblEmail;
    @FXML private Label lblPhone;
    @FXML private Label lblRole;
    @FXML private Label lblBalance;
    @FXML private Label lblTotalBids;
    @FXML private Label lblWonAuctions;
    @FXML private Label lblRating;

    @FXML private VBox bidHistoryContainer;
    @FXML private Button btnTabActive;
    @FXML private Button btnTabWon;
    @FXML private Button btnTabLost;

    private Button activeTab;
    private String currentFilter = "active";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeTab = btnTabActive;
        loadProfile();
        loadMyBids();
    }

    private void loadProfile() {
        UserService.loadProfile(user -> {
            if (user == null) {
                AlertUtil.showError("Không tải được thông tin");
                return;
            }
            displayUser(user);
        });
    }

    private void displayUser(LoginResponse user) {
        if (lblFullName != null) lblFullName.setText(user.getFullName());
        if (lblUsername != null) lblUsername.setText("@" + user.getUsername());
        if (lblEmail != null && user.getEmail() != null) lblEmail.setText(user.getEmail());
        if (lblRole != null) lblRole.setText(user.getRole());
    }

    private void loadMyBids() {
        UserService.loadMyBids(bids -> {
            if (bids == null) {
                bidHistoryContainer.getChildren().clear();
                return;
            }
            renderBids(bids);
            updateStats(bids);
        });
    }

    private void renderBids(List<BidRecord> bids) {
        bidHistoryContainer.getChildren().clear();
        List<BidRecord> filtered = bids.stream()
                .filter(this::matchTab)
                .toList();
        for (BidRecord b : filtered) {
            Label entry = new Label(String.format(
                    "Item %d — %,.0f đ — %s",
                    b.getItemId(), b.getAmount(), b.getPlacedAt()
            ));
            bidHistoryContainer.getChildren().add(entry);
        }
    }

    private boolean matchTab(BidRecord b) {
        // TODO: BidRecord chưa có status — tạm return true cho mọi tab
        // Khi BE bổ sung status (WON/LOST/ACTIVE), filter ở đây
        return switch (currentFilter) {
            case "active" -> true;
            case "won"    -> false;  // chưa có field status
            case "lost"   -> false;
            default       -> true;
        };
    }

    private void updateStats(List<BidRecord> bids) {
        if (lblTotalBids != null) lblTotalBids.setText(String.valueOf(bids.size()));
        // Won/Lost: cần BidRecord.status từ BE
    }

    // ===== TABS =====

    @FXML private void onTabActive() { setTab("active", btnTabActive); }
    @FXML private void onTabWon()    { setTab("won", btnTabWon); }
    @FXML private void onTabLost()   { setTab("lost", btnTabLost); }

    private void setTab(String filter, Button button) {
        currentFilter = filter;
        if (activeTab != null) {
            activeTab.getStyleClass().remove("tag-active");
            activeTab.getStyleClass().add("tag-inactive");
        }
        button.getStyleClass().remove("tag-inactive");
        button.getStyleClass().add("tag-active");
        activeTab = button;
        loadMyBids();
    }

    // ===== ACTIONS =====

    @FXML
    private void onSettings() {
        // MainController sẽ load SettingsView
        // Hiện tại ProfileController không có ref đến MainController
        // → dùng SceneManager.switchTo (load full screen)
        // hoặc emit event lên cha
    }

    @FXML
    private void onDeposit() {
        // Navigate sang WalletView qua main controller
        // Tạm thời mở Wallet view trực tiếp
    }

    @FXML
    private void onLogout() {
        boolean ok = AlertUtil.showConfirm("Đăng xuất", "Bạn có chắc muốn đăng xuất?");
        if (!ok) return;
        AuthService.logout();
    }
}