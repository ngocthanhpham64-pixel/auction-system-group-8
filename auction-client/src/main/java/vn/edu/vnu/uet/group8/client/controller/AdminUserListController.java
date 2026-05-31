package vn.edu.vnu.uet.group8.client.controller;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Callback;
import vn.edu.vnu.uet.group8.client.service.AdminService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.common.dto.model.UserAdminDTO;
import vn.edu.vnu.uet.group8.common.dto.model.UserProfileDTO;

public class AdminUserListController {
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbFilterRole;
    @FXML private ComboBox<String> cbFilterStatus;
    @FXML private TableView<UserAdminDTO> userTable;
    @FXML private TableColumn<UserAdminDTO, Integer> colId;
    @FXML private TableColumn<UserAdminDTO, String> colUsername;
    @FXML private TableColumn<UserAdminDTO, String> colEmail;
    @FXML private TableColumn<UserAdminDTO, String> colRole;
    @FXML private TableColumn<UserAdminDTO, String> colStatus;
    @FXML private TableColumn<UserAdminDTO, Void> colAction;

    private final ObservableList<UserAdminDTO> users = FXCollections.observableArrayList();
    private FilteredList<UserAdminDTO> filtered;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @FXML
    public void initialize() {
        cbFilterRole.getItems().addAll("Tất cả", UserAdminDTO.ROLE_MEMBER, UserAdminDTO.ROLE_SELLER, UserAdminDTO.ROLE_ADMIN, UserAdminDTO.ROLE_SUPER_ADMIN);
        cbFilterRole.getSelectionModel().selectFirst();
        cbFilterStatus.getItems().addAll("Tất cả", UserAdminDTO.STATUS_ACTIVE, UserAdminDTO.STATUS_SUSPENDED, UserAdminDTO.STATUS_BANNED);
        cbFilterStatus.getSelectionModel().selectFirst();

        colId.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("status"));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.styleProperty().set("-fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 10 4 10; -fx-background-radius: 12;");

                    if (item.equalsIgnoreCase(UserAdminDTO.STATUS_ACTIVE)) {
                        badge.setStyle(badge.getStyle() + "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D;");
                    } else if (item.equalsIgnoreCase(UserAdminDTO.STATUS_SUSPENDED)) {
                        badge.setStyle(badge.getStyle() + "-fx-background-color: #FEF3C7; -fx-text-fill: #B45309;");
                    } else if (item.equalsIgnoreCase(UserAdminDTO.STATUS_BANNED)) {
                        badge.setStyle(badge.getStyle() + "-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C;");
                    } else {
                        badge.setStyle(badge.getStyle() + "-fx-background-color: #F1F5F9; -fx-text-fill: #475569;");
                    }

                    // Đảm bảo huy hiệu luôn ra giữa
                    setAlignment(Pos.CENTER);
                    setGraphic(badge);
                }
            }
        });

        colAction.setCellFactory(createActionCellFactory());

        filtered = new FilteredList<>(users, p -> true);
        SortedList<UserAdminDTO> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(userTable.comparatorProperty());
        userTable.setItems(sorted);

        tfSearch.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        cbFilterRole.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        cbFilterStatus.valueProperty().addListener((obs, oldV, newV) -> applyFilters());

        loadUsers();
    }

    private void loadUsers() {
        AdminService.getUsers(list -> {
            users.setAll(list != null ? list : List.of());
            applyFilters();
        });
    }

    private void applyFilters() {
        String q = tfSearch.getText() != null ? tfSearch.getText().trim().toLowerCase() : "";
        String role = cbFilterRole.getValue();
        String status = cbFilterStatus.getValue();

        filtered.setPredicate(u -> {
            if (u == null) return false;
            boolean matchesQ = q.isEmpty() || u.getUsername().toLowerCase().contains(q) || u.getEmail().toLowerCase().contains(q);
            boolean matchesRole = role == null || role.equals("Tất cả") || u.getRole().equalsIgnoreCase(role);
            boolean matchesStatus = status == null || status.equals("Tất cả") || u.getStatus().equalsIgnoreCase(status);
            return matchesQ && matchesRole && matchesStatus;
        });
    }

    private Callback<TableColumn<UserAdminDTO, Void>, TableCell<UserAdminDTO, Void>> createActionCellFactory() {
        return col -> new TableCell<>() {
            private final MenuButton menuBtn = new MenuButton("Thao tác ⚙️");
            private final MenuItem itemDetail = new MenuItem("👁  Xem thống kê");
            private final MenuItem itemRestore = new MenuItem("🔓  Mở khóa tài khoản");
            private final MenuItem itemSuspend = new MenuItem("🔒  Khóa tạm thời");
            private final MenuItem itemBan = new MenuItem("🚫  Cấm vĩnh viễn");

            {
                // Mở rộng độ rộng nút để không bị đứt chữ
                menuBtn.setPrefWidth(120);
                menuBtn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-font-weight: bold; "
                        + "-fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand; -fx-font-size: 12px;");

                itemDetail.setStyle("-fx-text-fill: #1E293B;");
                itemRestore.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
                itemSuspend.setStyle("-fx-text-fill: #D97706;");
                itemBan.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");

                menuBtn.getItems().addAll(itemDetail, itemRestore, itemSuspend, itemBan);

                itemRestore.setOnAction(e -> {
                    UserAdminDTO u = getTableView().getItems().get(getIndex());
                    if (u != null) changeUserStatus(u, UserAdminDTO.STATUS_ACTIVE);
                });
                itemSuspend.setOnAction(e -> {
                    UserAdminDTO u = getTableView().getItems().get(getIndex());
                    if (u != null) changeUserStatus(u, UserAdminDTO.STATUS_SUSPENDED);
                });
                itemBan.setOnAction(e -> {
                    UserAdminDTO u = getTableView().getItems().get(getIndex());
                    if (u != null) changeUserStatus(u, UserAdminDTO.STATUS_BANNED);
                });
                itemDetail.setOnAction(e -> {
                    UserAdminDTO u = getTableView().getItems().get(getIndex());
                    if (u != null) showUserStats(u.getId());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    UserAdminDTO u = getTableView().getItems().get(getIndex());

                    itemSuspend.setDisable(u.isSuspended() || u.isBanned());
                    itemBan.setDisable(u.isBanned());
                    itemRestore.setDisable(!u.isSuspended());

                    setGraphic(menuBtn);
                    // LỆNH QUAN TRỌNG: Ép nút Thao Tác ra giữa ô
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }

    private void showUserStats(int userId) {
        AdminService.getUserProfile(userId, stats -> {
            if (stats == null) { AlertUtil.showError("Không tải được thông tin người dùng"); return; }
            StringBuilder sb = new StringBuilder();
            sb.append("Người dùng: ").append(stats.getUsername()).append(" (ID: ").append(stats.getUserId()).append(")\n");
            if (stats.getEmail() != null) sb.append("Email: ").append(stats.getEmail()).append("\n");
            sb.append("Vai trò: ").append(stats.getDisplayRole()).append("\n\n");
            sb.append("Tổng lượt đặt giá: ").append(stats.getTotalBidsPlaced()).append("\n");
            sb.append("Tổng vật phẩm bán được: ").append(stats.getTotalItemsSold()).append("\n");
            if (stats.getSellerRating() != null) sb.append("Đánh giá: ").append(stats.getSellerRating()).append(" / 5\n");
            java.time.Instant li = stats.getLastLogin();
            AlertUtil.showInfo(sb.toString());
        });
    }

    private void changeUserStatus(UserAdminDTO user, String newStatus) {
        if (user == null) return;
        boolean ok = AlertUtil.showConfirm("Xác nhận", "Bạn có chắc muốn đổi trạng thái của '" + user.getUsername() + "' thành " + newStatus + "?");
        if (!ok) return;
        AdminService.updateUserStatus(user.getId(), newStatus, success -> {
            if (success) {
                AlertUtil.showInfo("Cập nhật thành công");
                loadUsers();
            } else {
                AlertUtil.showError("Không thể cập nhật trạng thái. Vui lòng thử lại.");
            }
        });
    }

    @FXML
    public void suspendSelected() {
        UserAdminDTO sel = userTable.getSelectionModel().getSelectedItem();
        if (sel == null) { AlertUtil.showWarning("Vui lòng chọn người dùng"); return; }
        changeUserStatus(sel, UserAdminDTO.STATUS_SUSPENDED);
    }

    @FXML
    public void banSelected() {
        UserAdminDTO sel = userTable.getSelectionModel().getSelectedItem();
        if (sel == null) { AlertUtil.showWarning("Vui lòng chọn người dùng"); return; }
        changeUserStatus(sel, UserAdminDTO.STATUS_BANNED);
    }

    @FXML
    public void restoreSelected() {
        UserAdminDTO sel = userTable.getSelectionModel().getSelectedItem();
        if (sel == null) { AlertUtil.showWarning("Vui lòng chọn người dùng"); return; }
        changeUserStatus(sel, UserAdminDTO.STATUS_ACTIVE);
    }
}