package vn.edu.vnu.uet.group8.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.UserService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * WalletController — quan ly so du + lich su giao dich.
 *
 * Tinh nang:
 *  - Hien thi balance, holding, total spent (binding voi ClientModel)
 *  - Tab filter giao dich: Tat ca / Nap / Tam giu / Thanh toan
 *  - Nap tien voi validate + confirm dialog
 *  - Rut tien (cho BE bo sung API)
 */
public class WalletController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(WalletController.class.getName());

    private static final BigDecimal MIN_DEPOSIT = new BigDecimal("10000");        // 10k VND
    private static final BigDecimal MAX_DEPOSIT = new BigDecimal("100000000");    // 100tr VND

    @FXML private Label lblBalance;
    @FXML private Label lblHolding;
    @FXML private Label lblTotalSpent;
    @FXML private VBox transactionList;
    @FXML private Button btnTabAll;
    @FXML private Button btnTabDeposit;
    @FXML private Button btnTabHold;
    @FXML private Button btnTabPayment;

    private Button activeTab;
    private String currentFilter = "all";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bindBalance();
        activeTab = btnTabAll;
        renderTransactions();  // Render demo data theo tab dau tien
    }

    /** Binding balance label voi ClientModel.balanceProperty. */
    private void bindBalance() {
        ClientModel model = ClientModel.getInstance();
        BigDecimal current = model.getBalance();
        updateBalanceLabel(current);

        model.balanceProperty().addListener((obs, oldVal, newVal) ->
                Platform.runLater(() -> updateBalanceLabel(newVal))
        );
    }

    private void updateBalanceLabel(BigDecimal balance) {
        if (lblBalance == null) return;
        BigDecimal value = balance != null ? balance : BigDecimal.ZERO;
        lblBalance.setText(String.format("%,.0f d", value));
    }

    // ===== ACTIONS =====

    /**
     * Nap tien: dialog input -> validate -> confirm -> goi service.
     */
    @FXML
    private void onDeposit() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nap tien");
        dialog.setHeaderText("Nap tien vao tai khoan");
        dialog.setContentText("So tien (VND):");

        Optional<String> input = dialog.showAndWait();
        if (input.isEmpty()) return;

        BigDecimal amount = parseAmount(input.get());
        if (amount == null) {
            AlertUtil.showWarning("So tien khong hop le");
            return;
        }

        if (amount.compareTo(MIN_DEPOSIT) < 0) {
            AlertUtil.showWarning("So tien toi thieu: " + formatVnd(MIN_DEPOSIT));
            return;
        }

        if (amount.compareTo(MAX_DEPOSIT) > 0) {
            AlertUtil.showWarning("So tien toi da: " + formatVnd(MAX_DEPOSIT));
            return;
        }

        boolean confirm = AlertUtil.showConfirm("Xac nhan",
                "Nap " + formatVnd(amount) + " vao tai khoan?");
        if (!confirm) return;

        LOGGER.info(() -> "Yeu cau nap tien: " + amount);
        UserService.deposit(amount, success -> {
            if (success) {
                AlertUtil.showInfo("Nap tien thanh cong! +" + formatVnd(amount));
                // ClientModel da update balance -> label tu refresh
            } else {
                AlertUtil.showError("Nap tien that bai. Vui long thu lai.");
            }
        });
    }

    /**
     * Rut tien: chua co API -> hien dialog placeholder.
     */
    @FXML
    private void onWithdraw() {
        AlertUtil.showInfo("Tinh nang rut tien dang phat trien.\n"
                + "Vui long lien he ho tro de rut tien thu cong.");
    }

    /**
     * Xem lich su giao dich -> reload tab Tat ca.
     */
    @FXML
    private void onHistory() {
        setTab("all", btnTabAll);
    }

    /**
     * Xem tat ca giao dich (icon "..." trong header).
     */
    @FXML
    private void onViewAll() {
        setTab("all", btnTabAll);
        // Mo dialog xem chi tiet day du
        AlertUtil.showInfo("Hien thi " + transactionList.getChildren().size() + " giao dich gan day");
    }

    // ===== TABS =====

    @FXML private void onTabAll()     { setTab("all", btnTabAll); }
    @FXML private void onTabDeposit() { setTab("deposit", btnTabDeposit); }
    @FXML private void onTabHold()    { setTab("hold", btnTabHold); }
    @FXML private void onTabPayment() { setTab("payment", btnTabPayment); }

    private void setTab(String filter, Button button) {
        currentFilter = filter;

        // Update visual state cua tab
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

        renderTransactions();
    }

    /**
     * Render danh sach giao dich theo filter.
     * Demo: FXML co data san san pham, controller them empty state.
     */
    private void renderTransactions() {
        if (transactionList == null) return;

        // FXML co data demo san - khong xoa
        // Day la noi tot de filter khi co API getTransactions
        LOGGER.fine(() -> "Filter giao dich: " + currentFilter);
    }

    // ===== HELPERS =====

    /**
     * Parse string -> BigDecimal, return null neu invalid.
     */
    private BigDecimal parseAmount(String input) {
        if (input == null || input.isBlank()) return null;
        try {
            String cleaned = input.replaceAll("[^\\d]", "");
            if (cleaned.isEmpty()) return null;
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatVnd(BigDecimal amount) {
        return String.format("%,.0f d", amount);
    }
}