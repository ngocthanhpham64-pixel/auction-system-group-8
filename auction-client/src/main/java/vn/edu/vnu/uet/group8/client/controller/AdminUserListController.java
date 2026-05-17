package vn.edu.vnu.uet.group8.client.controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import vn.edu.vnu.uet.group8.client.service.AdminService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.model.UserAdminDTO;

/**
 * AdminUserListController — quản lý người dùng.
 *
 * Tính năng PRO:
 *  - ObservableList + FilteredList → tự update UI khi data thay đổi
 *  - Search live (theo username/email khi user gõ)
 *  - Filter role (MEMBER/SELLER/ADMIN) + status (ACTIVE/SUSPENDED/BANNED)
 *  - Status cell color-coded (xanh/vàng/đỏ)
 *  - colAction render 3 nút Suspend / Ban / Unban
 *  - Buttons tự enable/disable dựa theo current status
 *  - Super admin only check cho Ban
 *  - Confirm dialog mỗi action
 *
 * FXML handlers (giữ nguyên): suspendSelected, banSelected
 * fx:id: userTable, colId, colUsername, colEmail, colRole, colStatus, colAction
 */
public class AdminUserListController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(AdminUserListController.class.getName());

    @FXML private TableView<UserAdminDTO> userTable;
    @FXML private TableColumn<UserAdminDTO, Integer> colId;
    @FXML private TableColumn<UserAdminDTO, String> colUsername;
    @FXML private TableColumn<UserAdminDTO, String> colEmail;
    @FXML private TableColumn<UserAdminDTO, String> colRole;
    @FXML private TableColumn<UserAdminDTO, String> colStatus;
    @FXML private TableColumn<UserAdminDTO, Void> colAction;

    // Optional FXML fields - chỉ render nếu FXML có
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbFilterRole;
    @FXML private ComboBox<String> cbFilterStatus;

    private final ObservableList<UserAdminDTO> allUsers = FXCollections.observableArrayList();
    private FilteredList<UserAdminDTO> filteredUsers;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupActionColumn();
        setupStatusColorCoding();
        setupFilters();
        loadUsers();
    }

    /** Setup các cột data cơ bản. */
    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        filteredUsers = new FilteredList<>(allUsers, u -> true);
        userTable.setItems(filteredUsers);
    }

    /** Status color-coded: green/orange/red. */
    private void setupStatusColorCoding() {
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(status);
                setStyle(switch (status) {
                    case "ACTIVE"    -> "-fx-text-fill: #22C55E; -fx-font-weight: bold;";
                    case "SUSPENDED" -> "-fx-text-fill: #F59E0B; -fx-font-weight: bold;";
                    case "BANNED"    -> "-fx-text-fill: #EF4444; -fx-font-weight: bold;";
                    default -> "";
                });
            }
        });
    }

    /** colAction render 3 button per row. */
    private void setupActionColumn() {
        if (colAction == null) return;  // FXML cũ có thể không có colAction

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnSuspend = new Button("Khóa tạm");
            private final Button btnBan = new Button("Cấm");
            private final Button btnUnban = new Button("Mở khóa");
            private final HBox box = new HBox(6, btnSuspend, btnBan, btnUnban);

            {
                btnSuspend.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-font-size: 11px;");
                btnBan.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 11px;");
                btnUnban.setStyle("-fx-background-color: #22C55E; -fx-text-fill: white; -fx-font-size: 11px;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                UserAdminDTO user = (UserAdminDTO) getTableRow().getItem();
                String status = user.getStatus();

                // Enable/disable theo status hiện tại
                btnSuspend.setDisable("SUSPENDED".equals(status) || "BANNED".equals(status));
                btnBan.setDisable("BANNED".equals(status));
                btnUnban.setDisable("ACTIVE".equals(status));

                btnSuspend.setOnAction(e -> changeStatus(user, "SUSPENDED", "khóa tạm thời"));
                btnBan.setOnAction(e -> {
                    if (!SessionManager.isSuperAdmin()) {
                        AlertUtil.showError("Chỉ Super Admin mới có quyền Ban người dùng");
                        return;
                    }
                    changeStatus(user, "BANNED", "cấm vĩnh viễn");
                });
                btnUnban.setOnAction(e -> changeStatus(user, "ACTIVE", "kích hoạt lại"));

                setGraphic(box);
            }
        });
    }

    /** Setup filter UI nếu FXML có. */
    private void setupFilters() {
        if (cbFilterRole != null) {
            cbFilterRole.getItems().setAll("Tất cả vai trò", "MEMBER", "SELLER", "ADMIN");
            cbFilterRole.getSelectionModel().selectFirst();
            cbFilterRole.setOnAction(e -> applyFilters());
        }
        if (cbFilterStatus != null) {
            cbFilterStatus.getItems().setAll("Tất cả trạng thái", "ACTIVE", "SUSPENDED", "BANNED");
            cbFilterStatus.getSelectionModel().selectFirst();
            cbFilterStatus.setOnAction(e -> applyFilters());
        }
        if (tfSearch != null) {
            tfSearch.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        }
    }

    private void applyFilters() {
        if (filteredUsers == null) return;
        String search = tfSearch != null && tfSearch.getText() != null
                ? tfSearch.getText().toLowerCase().trim() : "";
        String role = cbFilterRole != null ? cbFilterRole.getValue() : null;
        String status = cbFilterStatus != null ? cbFilterStatus.getValue() : null;

        filteredUsers.setPredicate(u -> matchSearch(u, search)
                && matchRole(u, role)
                && matchStatus(u, status));
    }

    private boolean matchSearch(UserAdminDTO u, String search) {
        if (search.isEmpty()) return true;
        String username = u.getUsername() != null ? u.getUsername().toLowerCase() : "";
        String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
        return username.contains(search) || email.contains(search);
    }

    private boolean matchRole(UserAdminDTO u, String filter) {
        if (filter == null || filter.startsWith("Tất cả")) return true;
        if ("ADMIN".equals(filter)) return u.isAdmin();
        return filter.equals(u.getRole());
    }

    private boolean matchStatus(UserAdminDTO u, String filter) {
        if (filter == null || filter.startsWith("Tất cả")) return true;
        return filter.equals(u.getStatus());
    }

    // ===== LOAD =====

    private void loadUsers() {
        AdminService.getUsers(users -> Platform.runLater(() -> {
            allUsers.setAll(users != null ? users : java.util.List.of());
            LOGGER.info(() -> "Loaded " + allUsers.size() + " users");
        }));
    }

    // ===== FXML HANDLERS (giữ nguyên signature) =====

    /** Suspend user đang select trong table. */
    @FXML
    public void suspendSelected() {
        UserAdminDTO user = userTable.getSelectionModel().getSelectedItem();
        if (user == null) {
            AlertUtil.showWarning("Vui lòng chọn một người dùng");
            return;
        }
        if ("SUSPENDED".equals(user.getStatus()) || "BANNED".equals(user.getStatus())) {
            AlertUtil.showInfo("Tài khoản đã ở trạng thái " + user.getStatus());
            return;
        }
        changeStatus(user, "SUSPENDED", "khóa tạm thời");
    }

    /** Ban user đang select - chỉ super admin. */
    @FXML
    public void banSelected() {
        if (!SessionManager.isSuperAdmin()) {
            AlertUtil.showError("Chỉ Super Admin mới có quyền Ban người dùng");
            return;
        }
        UserAdminDTO user = userTable.getSelectionModel().getSelectedItem();
        if (user == null) {
            AlertUtil.showWarning("Vui lòng chọn một người dùng");
            return;
        }
        if ("BANNED".equals(user.getStatus())) {
            AlertUtil.showInfo("Tài khoản đã bị cấm");
            return;
        }
        changeStatus(user, "BANNED", "cấm vĩnh viễn");
    }

    /** Generic change status với confirm + reload. */
    private void changeStatus(UserAdminDTO user, String newStatus, String action) {
        boolean ok = AlertUtil.showConfirm("Xác nhận",
                "Bạn có chắc muốn " + action + " tài khoản '" + user.getUsername() + "'?");
        if (!ok) return;

        LOGGER.info(() -> "Change status " + user.getUsername() + " -> " + newStatus);

        AdminService.updateUserStatus(user.getId(), newStatus,
                success -> Platform.runLater(() -> {
                    if (success) {
                        AlertUtil.showInfo("Đã " + action + " tài khoản " + user.getUsername());
                        loadUsers();
                    } else {
                        AlertUtil.showError("Thao tác thất bại");
                    }
                })
        );
    }
}