package vn.edu.vnu.uet.group8.client.controller;

import java.math.BigDecimal;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.UserService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.ModalUtil;
import vn.edu.vnu.uet.group8.client.util.UIFormatter;

public class WithdrawController {

    @FXML private ComboBox<String> cbWithdrawBank;
    @FXML private TextField txtAccountNumber;
    @FXML private TextField txtWithdrawAmount;
    @FXML private Label lblAvailable;
    @FXML private Button btnConfirmWithdraw;

    @FXML
    public void initialize() {
        // Hiển thị số dư
        lblAvailable.setText("Khả dụng: " + UIFormatter.formatPrice(ClientModel.getInstance().getBalance()));
        btnConfirmWithdraw.setOnAction(e -> handleWithdraw());
    }

    private void handleWithdraw() {
        try {
            BigDecimal amount = new BigDecimal(txtWithdrawAmount.getText().trim());
            BigDecimal currentBalance = ClientModel.getInstance().getBalance();

            if (amount.compareTo(new BigDecimal("50000")) < 0) {
                AlertUtil.showError("Số tiền rút tối thiểu là 50.000 đ");
                return;
            }
            if (amount.compareTo(currentBalance) > 0) {
                AlertUtil.showError("Số dư không đủ để thực hiện giao dịch!");
                return;
            }

            UserService.withdraw(amount, success -> Platform.runLater(() -> {
                if (btnConfirmWithdraw.getScene() != null) btnConfirmWithdraw.getScene().getWindow().hide();
                Platform.runLater(() -> {
                    if (success) {
                        ModalUtil.showModal("YÊU CẦU THÀNH CÔNG", "SuccessContent.fxml");
                    } else {
                        ModalUtil.showModal("THAO TÁC THẤT BẠI", "FailureContent.fxml");
                    }
                });
            }));
        } catch (NumberFormatException ex) {
            AlertUtil.showError("Vui lòng điền số tiền hợp lệ!");
        }
    }
}
