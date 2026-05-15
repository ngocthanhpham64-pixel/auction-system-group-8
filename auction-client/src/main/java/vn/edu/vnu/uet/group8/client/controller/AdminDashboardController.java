package vn.edu.vnu.uet.group8.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import vn.edu.vnu.uet.group8.client.service.AdminService;
import vn.edu.vnu.uet.group8.client.util.UIFormatter;
import vn.edu.vnu.uet.group8.common.dto.AdminStatsDTO;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * AdminDashboardController — hiển thị 4 chỉ số tổng quan.
 *
 * Stats:
 *  - activeAuctions: số phiên đang diễn ra
 *  - totalUsers: tổng người dùng đăng ký
 *  - totalRevenue: tổng doanh thu (format VND đẹp)
 *  - totalBids: tổng lượt đặt giá
 *
 * fx:id từ FXML: lblActiveAuctions, lblTotalUsers, lblRevenue, lblTotalBids
 *
 * Cải thiện so với version cũ:
 *  - Logger thay vì silent fail
 *  - Null-safe: stats == null không crash
 *  - Empty state: hiển thị "--" khi chưa có data
 *  - Tách logic load và display
 */
public class AdminDashboardController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(AdminDashboardController.class.getName());

    @FXML private Label lblActiveAuctions;
    @FXML private Label lblTotalUsers;
    @FXML private Label lblRevenue;
    @FXML private Label lblTotalBids;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showLoading();
        loadStats();
    }

    /** Hiển thị placeholder khi đang load. */
    private void showLoading() {
        setText(lblActiveAuctions, "--");
        setText(lblTotalUsers, "--");
        setText(lblRevenue, "--");
        setText(lblTotalBids, "--");
    }

    /** Load stats async qua AdminService. */
    private void loadStats() {
        AdminService.getStats(stats -> Platform.runLater(() -> {
            if (stats == null) {
                LOGGER.warning("Admin stats null - giu placeholder");
                return;
            }
            displayStats(stats);
            LOGGER.fine(() -> "Loaded admin stats: " + stats.getActiveAuctions() + " active, "
                    + stats.getTotalUsers() + " users");
        }));
    }

    /** Render stats lên UI. */
    private void displayStats(AdminStatsDTO stats) {
        setText(lblActiveAuctions, String.valueOf(stats.getActiveAuctions()));
        setText(lblTotalUsers, String.valueOf(stats.getTotalUsers()));
        setText(lblTotalBids, String.valueOf(stats.getTotalBids()));

        if (lblRevenue != null) {
            // UIFormatter có sẵn từ project
            lblRevenue.setText(UIFormatter.formatPrice(stats.getTotalRevenue()));
        }
    }

    private void setText(Label label, String value) {
        if (label != null) label.setText(value);
    }
}