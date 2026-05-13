package vn.edu.vnu.uet.group8.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.AuthService;
import vn.edu.vnu.uet.group8.client.service.UserService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.BidRecord;
import vn.edu.vnu.uet.group8.common.dto.LoginResponse;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * ProfileController — trang ho so + lich su dau gia.
 *
 * Tinh nang:
 *  - Load profile tu UserService.loadProfile()
 *  - Binding voi ClientModel.balance -> update real-time
 *  - Load bid history tu UserService.loadMyBids()
 *  - Tab filter: Dang dau gia / Da thang / Da thua (filter bang BidRecord field)
 */
public class ProfileController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(ProfileController.class.getName());

    @FXML private Label lblName;
    @FXML private Label lblEmail;
    @FXML private Label lblAvatar;
    @FXML private Label lblJoinDate;
    @FXML private Label lblBalance;
    @FXML private Label lblRating;
    @FXML private Label lblTotalBids;
    @FXML private Label lblActiveBids;
    @FXML private Label lblWonBids;

    @FXML private VBox bidHistoryList;
    @FXML private Button btnTabActive;
    @FXML private Button btnTabWon;
    @FXML private Button btnTabLost;

    private Button activeTab;
    private String currentFilter = "active";
    private List<BidRecord> allBids = List.of();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeTab = btnTabActive;
        loadProfile();
        bindBalance();
        loadBidHistory();
    }

    /** Load thong tin user. */
    private void loadProfile() {
        UserService.loadProfile(user -> {
            if (user != null) {
                displayUser(user);
            }
        });

        // Fallback tu SessionManager
        if (lblName != null && SessionManager.getFullName() != null) {
            lblName.setText(SessionManager.getFullName());
        }
        if (lblAvatar != null && SessionManager.getAvatarText() != null) {
            lblAvatar.setText(SessionManager.getAvatarText());
        }
    }

    private void displayUser(LoginResponse user) {
        if (user == null) return;
        if (lblName != null && user.getFullName() != null) {
            lblName.setText(user.getFullName());
        }
        if (lblEmail != null && user.getEmail() != null) {
            lblEmail.setText(user.getEmail());
        }
    }

    /** Binding balance. */
    private void bindBalance() {
        if (lblBalance == null) return;
        BigDecimal current = ClientModel.getInstance().getBalance();
        updateBalance(current);
        ClientModel.getInstance().balanceProperty().addListener((obs, old, val) ->
                Platform.runLater(() -> updateBalance(val))
        );
    }

    private void updateBalance(BigDecimal value) {
        BigDecimal v = value != null ? value : BigDecimal.ZERO;
        lblBalance.setText(String.format("%,.0f d", v));
    }

    /** Load bid history. */
    private void loadBidHistory() {
        UserService.loadMyBids(bids -> {
            allBids = bids != null ? bids : List.of();
            LOGGER.info(() -> "Tai " + allBids.size() + " bid records");
            renderBids();
            updateStats();
        });
    }

    private void renderBids() {
        if (bidHistoryList == null) return;
        bidHistoryList.getChildren().clear();

        List<BidRecord> filtered = allBids.stream()
                .filter(this::matchTab)
                .toList();

        if (filtered.isEmpty()) {
            Label empty = new Label("Khong co bid trong muc nay");
            empty.getStyleClass().add("label-info");
            bidHistoryList.getChildren().add(empty);
            return;
        }

        for (BidRecord b : filtered) {
            bidHistoryList.getChildren().add(buildBidRow(b));
        }
    }

    private boolean matchTab(BidRecord b) {
        // BidRecord chua co field status -> tam dung gia tri
        // TODO: khi BE them status (ACTIVE/WON/LOST), filter chinh xac
        return switch (currentFilter) {
            case "active" -> true;   // tat ca tam coi la active
            case "won"    -> false;
            case "lost"   -> false;
            default       -> true;
        };
    }

    private HBox buildBidRow(BidRecord b) {
        HBox row = new HBox(12);
        row.getStyleClass().add("card-soft");
        row.setStyle("-fx-padding: 12; -fx-background-radius: 8;");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label itemLabel = new Label("Item #" + getBidItemId(b));
        itemLabel.getStyleClass().add("h3");

        Label amount = new Label(String.format("%,.0f d", getBidAmount(b)));
        amount.setStyle("-fx-text-fill: #F97316; -fx-font-weight: bold;");

        info.getChildren().addAll(itemLabel, amount);
        row.getChildren().add(info);
        return row;
    }

    /** Helper - dung reflection-safe access (BidRecord co the la record hoac class). */
    private int getBidItemId(BidRecord b) {
        try {
            // Thu method itemId() (record style) truoc
            return (int) BidRecord.class.getMethod("itemId").invoke(b);
        } catch (Exception e) {
            try {
                return (int) BidRecord.class.getMethod("getItemId").invoke(b);
            } catch (Exception e2) {
                return 0;
            }
        }
    }

    private BigDecimal getBidAmount(BidRecord b) {
        try {
            return (BigDecimal) BidRecord.class.getMethod("amount").invoke(b);
        } catch (Exception e) {
            try {
                return (BigDecimal) BidRecord.class.getMethod("getAmount").invoke(b);
            } catch (Exception e2) {
                return BigDecimal.ZERO;
            }
        }
    }

    private void updateStats() {
        if (lblTotalBids != null) {
            lblTotalBids.setText(String.valueOf(allBids.size()));
        }
        if (lblActiveBids != null) {
            lblActiveBids.setText(String.valueOf(allBids.size()));  // tam = total
        }
        if (lblWonBids != null) {
            lblWonBids.setText("0");  // chua co field status
        }
    }

    // ===== ACTIONS =====

    @FXML
    private void onDeposit() {
        MainController main = MainController.getInstance();
        if (main != null) {
            main.loadView(SceneManager.VIEW_WALLET);
        } else {
            AlertUtil.showWarning("Khong the chuyen view");
        }
    }

    @FXML
    private void onSettings() {
        MainController main = MainController.getInstance();
        if (main != null) {
            main.loadView(SceneManager.VIEW_SETTINGS);
        } else {
            AlertUtil.showWarning("Khong the chuyen view");
        }
    }
    @FXML
    private void onLogout() {
        boolean ok = AlertUtil.showConfirm("Dang xuat", "Ban co chac muon dang xuat?");
        if (!ok) return;
        LOGGER.info("Nguoi dung dang xuat tu Profile");
        AuthService.logout();
    }

    // ===== TABS =====

    @FXML private void onTabActive() { setTab("active", btnTabActive); }
    @FXML private void onTabWon()    { setTab("won", btnTabWon); }
    @FXML private void onTabLost()   { setTab("lost", btnTabLost); }

    private void setTab(String filter, Button button) {
        currentFilter = filter;
        if (activeTab != null) {
            activeTab.getStyleClass().remove("tag-active");
            if (!activeTab.getStyleClass().contains("tag-inactive")) {
                activeTab.getStyleClass().add("tag-inactive");
            }
        }
        button.getStyleClass().remove("tag-inactive");
        if (!button.getStyleClass().contains("tag-active")) {
            button.getStyleClass().add("tag-active");
        }
        activeTab = button;
        renderBids();
    }
}