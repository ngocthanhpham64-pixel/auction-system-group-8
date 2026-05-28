package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class AdminUserListController {
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbFilterRole;
    @FXML private ComboBox<String> cbFilterStatus;
    @FXML private TableView<?> userTable;
    @FXML private TableColumn<?, ?> colId;
    @FXML private TableColumn<?, ?> colUsername;
    @FXML private TableColumn<?, ?> colEmail;
    @FXML private TableColumn<?, ?> colRole;
    @FXML private TableColumn<?, ?> colStatus;
    @FXML private TableColumn<?, ?> colAction;

    @FXML
    public void initialize() {
        cbFilterRole.getItems().addAll("Tất cả", "ADMIN", "MEMBER");
        cbFilterStatus.getItems().addAll("Tất cả", "Hoạt động", "Bị khóa");
    }

    @FXML
    public void suspendSelected() {
        // Handle suspend logic here
    }

    @FXML
    public void banSelected() {
        // Handle ban logic here
    }
}
