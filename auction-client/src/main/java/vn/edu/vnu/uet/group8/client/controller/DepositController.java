package vn.edu.vnu.uet.group8.client.controller;

import java.math.BigDecimal;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import vn.edu.vnu.uet.group8.client.service.UserService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.ModalUtil;

public class DepositController {

    @FXML private ComboBox<String> cbDepositMethod;
    @FXML private TextField txtDepositAmount;
    @FXML private VBox vboxQrCodeArea;
    @FXML private Button btnConfirmDeposit;

    @FXML
    public void onPaymentMethodChange(ActionEvent event) {
        String method = cbDepositMethod.getValue();
        if (method != null && !method.isEmpty()) {
            vboxQrCodeArea.setVisible(true);
            vboxQrCodeArea.setManaged(true);
        } else {
            vboxQrCodeArea.setVisible(false);
            vboxQrCodeArea.setManaged(false);
        }
    }

    @FXML
    public void onQuickAmountClick(ActionEvent event) {
        Button btn = (Button) event.getSource();
        txtDepositAmount.setText(btn.getText().replace(".", ""));
    }

    @FXML
    public void onConfirmDepositClick(ActionEvent event) {
        String amountStr = txtDepositAmount.getText().trim();
        try {
            BigDecimal amount = new BigDecimal(amountStr);
            UserService.deposit(amount, success -> Platform.runLater(() -> {
                closeModal();
                Platform.runLater(() -> {
                    if (success) {
                        ModalUtil.showModal("NẠP TIỀN THÀNH CÔNG", "SuccessContent.fxml");
                    } else {
                        ModalUtil.showModal("GIAO DỊCH THẤT BẠI", "FailureContent.fxml");
                    }
                });
            }));
        } catch (NumberFormatException e) {
            AlertUtil.showError("Số tiền nạp không hợp lệ!");
        }
    }

    private void closeModal() {
        if (btnConfirmDeposit.getScene() != null && btnConfirmDeposit.getScene().getWindow() != null) {
            btnConfirmDeposit.getScene().getWindow().hide();
        }
    }
}