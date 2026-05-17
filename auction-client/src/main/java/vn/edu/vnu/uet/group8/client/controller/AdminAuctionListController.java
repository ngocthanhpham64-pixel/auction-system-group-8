package vn.edu.vnu.uet.group8.client.controller;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import vn.edu.vnu.uet.group8.client.service.AdminService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.UIFormatter;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

/**
 * AdminAuctionListController — quản lý phiên đấu giá.
 *
 * Tính năng PRO:
 *  - ObservableList + FilteredList
 *  - Search live by title
 *  - Filter status (ACTIVE/SOLD/ENDED_NO_BID/CANCELLED)
 *  - Price + EndTime formatted via UIFormatter
 *  - Status color-coded
 *  - colAction render nút "Hủy phiên"
 *  - Cancel với dialog nhập lý do (optional)
 *
 * FXML handlers: cancelSelected
 * fx:id: auctionTable, colId, colTitle, colPrice, colStatus, colEndTime, colAction
 */
public class AdminAuctionListController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(AdminAuctionListController.class.getName());

    @FXML private TableView<AuctionItemDTO> auctionTable;
    @FXML private TableColumn<AuctionItemDTO, Integer> colId;
    @FXML private TableColumn<AuctionItemDTO, String> colTitle;
    @FXML private TableColumn<AuctionItemDTO, String> colPrice;
    @FXML private TableColumn<AuctionItemDTO, String> colStatus;
    @FXML private TableColumn<AuctionItemDTO, String> colEndTime;
    @FXML private TableColumn<AuctionItemDTO, Void> colAction;

    // Optional fields
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbFilterStatus;

    private final ObservableList<AuctionItemDTO> allAuctions = FXCollections.observableArrayList();
    private FilteredList<AuctionItemDTO> filteredAuctions;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupStatusColorCoding();
        setupActionColumn();
        setupFilters();
        loadAuctions();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("itemId"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colPrice.setCellValueFactory(cell ->
                new SimpleStringProperty(UIFormatter.formatPrice(cell.getValue().getCurrentPrice())));
        colStatus.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getStatus() != null ? cell.getValue().getStatus().name() : ""));
        colEndTime.setCellValueFactory(cell ->
                new SimpleStringProperty(UIFormatter.formatInstant(cell.getValue().getEndTime())));

        filteredAuctions = new FilteredList<>(allAuctions, a -> true);
        auctionTable.setItems(filteredAuctions);
    }

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
                    case "ACTIVE"        -> "-fx-text-fill: #22C55E; -fx-font-weight: bold;";
                    case "SOLD"          -> "-fx-text-fill: #1E3A6E; -fx-font-weight: bold;";
                    case "ENDED_NO_BID"  -> "-fx-text-fill: #9CA3AF;";
                    case "CANCELLED"     -> "-fx-text-fill: #EF4444; -fx-font-weight: bold;";
                    default -> "";
                });
            }
        });
    }

    private void setupActionColumn() {
        if (colAction == null) return;

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnCancel = new Button("Hủy phiên");

            {
                btnCancel.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 11px;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                AuctionItemDTO auction = (AuctionItemDTO) getTableRow().getItem();
                SessionStatus status = auction.getStatus();

                // Chỉ cho phép hủy phiên đang ACTIVE
                btnCancel.setDisable(SessionStatus.ACTIVE != status);

                btnCancel.setOnAction(e -> cancelAuction(auction));

                setGraphic(btnCancel);
            }
        });
    }

    private void setupFilters() {
        if (cbFilterStatus != null) {
            cbFilterStatus.getItems().setAll("Tất cả trạng thái",
                    "ACTIVE", "SOLD", "ENDED_NO_BID", "CANCELLED");
            cbFilterStatus.getSelectionModel().selectFirst();
            cbFilterStatus.setOnAction(e -> applyFilters());
        }
        if (tfSearch != null) {
            tfSearch.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        }
    }

    private void applyFilters() {
        if (filteredAuctions == null) return;
        String search = tfSearch != null && tfSearch.getText() != null
                ? tfSearch.getText().toLowerCase().trim() : "";
        String status = cbFilterStatus != null ? cbFilterStatus.getValue() : null;

        filteredAuctions.setPredicate(a -> matchSearch(a, search) && matchStatus(a, status));
    }

    private boolean matchSearch(AuctionItemDTO a, String search) {
        if (search.isEmpty()) return true;
        String title = a.getTitle() != null ? a.getTitle().toLowerCase() : "";
        return title.contains(search);
    }

    private boolean matchStatus(AuctionItemDTO a, String filter) {
        if (filter == null || filter.startsWith("Tất cả")) return true;
        return a.getStatus() != null && filter.equals(a.getStatus().name());
    }

    // ===== LOAD =====

    private void loadAuctions() {
        AdminService.getAuctions(auctions -> Platform.runLater(() -> {
            allAuctions.setAll(auctions != null ? auctions : java.util.List.of());
            LOGGER.info(() -> "Loaded " + allAuctions.size() + " auctions");
        }));
    }

    // ===== FXML HANDLER =====

    /** Hủy phiên đang select. */
    @FXML
    public void cancelSelected() {
        AuctionItemDTO auction = auctionTable.getSelectionModel().getSelectedItem();
        if (auction == null) {
            AlertUtil.showWarning("Vui lòng chọn một phiên đấu giá");
            return;
        }
        if (SessionStatus.ACTIVE != auction.getStatus()) {
            AlertUtil.showWarning("Chỉ có thể hủy phiên đang ACTIVE.\n"
                    + "Phiên này đang ở trạng thái: " + auction.getStatus());
            return;
        }
        cancelAuction(auction);
    }

    /** Logic chung — dialog nhập lý do + confirm + gọi service. */
    private void cancelAuction(AuctionItemDTO auction) {
        // Dialog nhập lý do
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Hủy phiên đấu giá");
        dialog.setHeaderText("Hủy phiên: " + auction.getTitle());
        dialog.setContentText("Lý do hủy (tùy chọn):");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;  // user nhấn Cancel

        String reason = result.get().trim();

        boolean ok = AlertUtil.showConfirm("Xác nhận",
                "Bạn có chắc muốn hủy phiên '" + auction.getTitle() + "'?"
                        + (reason.isEmpty() ? "" : "\n\nLý do: " + reason));
        if (!ok) return;

        LOGGER.info(() -> "Cancel auction " + auction.getItemId() + " - reason: " + reason);

        AdminService.cancelAuction(auction.getItemId(), success -> Platform.runLater(() -> {
            if (success) {
                AlertUtil.showInfo("Đã hủy phiên: " + auction.getTitle());
                loadAuctions();
            } else {
                AlertUtil.showError("Thao tác thất bại");
            }
        }));
    }
}