package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AdminDashboardController {
    @FXML private Label lblActiveAuctions;
    @FXML private Label lblTotalUsers;
    @FXML private Label lblRevenue;
    @FXML private Label lblTotalBids;

    @FXML
    public void initialize() {
        // Initialize with default values for now.
        // Integration with backend can be added later.
        lblActiveAuctions.setText("0");
        lblTotalUsers.setText("0");
        lblRevenue.setText("0 đ");
        lblTotalBids.setText("0");
    }
}
