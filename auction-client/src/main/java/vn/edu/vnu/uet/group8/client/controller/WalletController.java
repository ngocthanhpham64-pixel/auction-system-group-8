package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

public class WalletController implements Initializable {

    // ===== FXML BINDINGS =====
    @FXML private Label lblBalance;
    @FXML private Label lblHolding;
    @FXML private Label lblTotalSpent;
    @FXML private VBox transactionList;

    // Tab buttons
    @FXML private Button btnTabAll;
    @FXML private Button btnTabDeposit;
    @FXML private Button btnTabHold;
    @FXML private Button btnTabPayment;

    // ===== STATE =====
    private Button activeTab;
    private BigDecimal balance    = new BigDecimal("50000000");
    private BigDecimal holding    = new BigDecimal("12000000");
    private BigDecimal totalSpent = new BigDecimal("238000000");
    /** "all" | "deposit" | "hold" | "payment" */
    private String currentFilter = "all";

    // ===== INIT =====
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeTab = btnTabAll;
        updateBalanceDisplay();
        loadTransactions();
    }

    // ===== ACTIONS =====

    @FXML
    private void onDeposit() {
        // TODO: mở dialog nạp tiền
        System.out.println("[WalletController] Nạp tiền");
    }

    @FXML
    private void onWithdraw() {
        // TODO: mở dialog rút tiền
        System.out.println("[WalletController] Rút tiền");
    }

    @FXML
    private void onHistory() {
        // Scroll xuống phần lịch sử
        transactionList.requestFocus();
    }

    @FXML
    private void onViewAll() {
        System.out.println("[WalletController] Xem tất cả giao dịch");
    }

    // ===== TABS =====

    @FXML private void onTabAll()     { setTab(btnTabAll,     "all");     loadTransactions(); }
    @FXML private void onTabDeposit() { setTab(btnTabDeposit, "deposit"); loadTransactions(); }
    @FXML private void onTabHold()    { setTab(btnTabHold,    "hold");    loadTransactions(); }
    @FXML private void onTabPayment() { setTab(btnTabPayment, "payment"); loadTransactions(); }

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

    // ===== LOAD DATA =====

    private void updateBalanceDisplay() {
        lblBalance.setText(String.format("%,.0f đ", balance));
        lblHolding.setText(String.format("%,.0f đ", holding));
        lblTotalSpent.setText(String.format("%,.0f đ", totalSpent));
    }

    private void loadTransactions() {
        // TODO: load dữ liệu thật từ server theo currentFilter
        // Hiện tại giữ nguyên dữ liệu tĩnh trong FXML
        System.out.println("[WalletController] Load giao dịch filter=" + currentFilter);
    }
}
