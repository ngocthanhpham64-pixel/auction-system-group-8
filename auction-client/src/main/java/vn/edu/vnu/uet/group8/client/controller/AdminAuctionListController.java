package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class AdminAuctionListController {
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbFilterStatus;
    @FXML private TableView<?> auctionTable;
    @FXML private TableColumn<?, ?> colId;
    @FXML private TableColumn<?, ?> colTitle;
    @FXML private TableColumn<?, ?> colPrice;
    @FXML private TableColumn<?, ?> colStatus;
    @FXML private TableColumn<?, ?> colEndTime;
    @FXML private TableColumn<?, ?> colAction;

    @FXML
    public void initialize() {
        cbFilterStatus.getItems().addAll("Tất cả", "Đang diễn ra", "Đã kết thúc", "Đã hủy");
    }

    @FXML
    public void cancelSelected() {
        // Handle cancel logic here
    }
}
