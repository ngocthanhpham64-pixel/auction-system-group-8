package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.UserService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * WalletController — wire UserService.deposit() thật.
 *
 * Hiển thị:
 *   - Số dư từ ClientModel (auto-update khi balance đổi)
 *   - Lịch sử giao dịch (TODO: BE chưa có API getTransactions)
 *   - Nút Nạp tiền → mở dialog nhập số tiền → gọi UserService.deposit()
 */
public class WalletController implements Initializable {

    @FXML private Label lblBalance;
    @FXML private Label lblHolding;
    @FXML private Label lblTotalSpent;
    @FXML private VBox transactionContainer;
    @FXML private Button btnTabAll;
    @FXML private Button btnTabDeposit;
    @FXML private Button btnTabBid;
    @FXML private Button btnTabRefund;

    private Button activeTab;
    private String currentFilter = "all";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeTab = btnTabAll;
        bindBalance();
        loadTransactions();
    }

    private void bindBalance() {
        // Listen ClientModel.balance → auto update label
        ClientModel.getInstance().balanceProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && lblBalance != null) {
                lblBalance.setText(String.format("%,.0f đ", newVal));
            }
        });

        // Set giá trị hiện tại
        BigDecimal current = ClientModel.getInstance().getBalance();
        if (current != null && lblBalance != null) {
            lblBalance.setText(String.format("%,.0f đ", current));
        }
    }

    private void loadTransactions() {
        // TODO: BE chưa có API getTransactions
        // Khi có → UserService.loadTransactions(...);
        transactionContainer.getChildren().clear();
        Label placeholder = new Label("Chưa có giao dịch nào");
        placeholder.getStyleClass().add("label-info");
        transactionContainer.getChildren().add(placeholder);
    }

    // ===== ACTIONS =====

    @FXML
    private void onDeposit() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nạp tiền");
        dialog.setHeaderText(null);
        dialog.setContentText("Nhập số tiền muốn nạp (VND):");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        try {
            String input = result.get().replaceAll("[^\\d]", "");
            BigDecimal amount = new BigDecimal(input);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                AlertUtil.showWarning("Số tiền phải > 0");
                return;
            }

            UserService.deposit(amount, success -> {
                if (success) {
                    AlertUtil.showInfo("Nạp tiền thành công!");
                    // ClientModel.balance đã update tự động
                } else {
                    AlertUtil.showError("Nạp tiền thất bại");
                }
            });
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("Số tiền không hợp lệ");
        }
    }

    @FXML
    private void onWithdraw() {
        // TODO: BE chưa có API withdraw
        AlertUtil.showInfo("Tính năng đang phát triển");
    }

    @FXML
    private void onHistory() {
        loadTransactions();
    }
    @FXML
    private void onViewAll() {
        // TODO: hiện toàn bộ giao dịch (load page mới hoặc mở rộng list)
        System.out.println("[Wallet] Xem tất cả giao dịch");
    }

    // ===== TABS =====

    @FXML private void onTabAll()     { setTab("all", btnTabAll); }
    @FXML private void onTabDeposit() { setTab("deposit", btnTabDeposit); }
    @FXML private void onTabBid()     { setTab("bid", btnTabBid); }
    @FXML private void onTabRefund()  { setTab("refund", btnTabRefund); }

    private void setTab(String filter, Button button) {
        currentFilter = filter;
        if (activeTab != null) {
            activeTab.getStyleClass().remove("tag-active");
            activeTab.getStyleClass().add("tag-inactive");
        }
        button.getStyleClass().remove("tag-inactive");
        button.getStyleClass().add("tag-active");
        activeTab = button;
        loadTransactions();
    }
}