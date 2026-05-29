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
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;

public class AdminAuctionListController {
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbFilterStatus;
    @FXML private TableView<AuctionItemDTO> auctionTable;
    @FXML private TableColumn<AuctionItemDTO, Integer> colId;
    @FXML private TableColumn<AuctionItemDTO, String> colTitle;
    @FXML private TableColumn<AuctionItemDTO, String> colPrice; // Khớp chính xác với fx:id="colPrice"
    @FXML private TableColumn<AuctionItemDTO, String> colStatus;
    @FXML private TableColumn<AuctionItemDTO, String> colEndTime;
    @FXML private TableColumn<AuctionItemDTO, Void> colAction;

    private final ObservableList<AuctionItemDTO> items = FXCollections.observableArrayList();
    private FilteredList<AuctionItemDTO> filtered;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @FXML
    public void initialize() {
        cbFilterStatus.getItems().addAll("Tất cả", "ACTIVE", "SOLD", "CANCELLED", "UPCOMING", "ENDED_NO_BID");
        cbFilterStatus.getSelectionModel().selectFirst();

        colId.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("itemId"));
        colTitle.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("title"));

        colPrice.setCellValueFactory(cell -> {
            var price = cell.getValue().getCurrentPrice();
            String s;
            if (price != null) {
                java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
                s = nf.format(price) + " đ";
            } else {
                s = "0 đ";
            }
            return new javafx.beans.property.SimpleStringProperty(s);
        });

        colStatus.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getStatus() != null ? cell.getValue().getStatus().name() : ""));

        // ==========================================
        // CẢI TIẾN 1: TỐI ƯU HIỆU NĂNG RENDER CELL FACTORY CHO BADGE STATUS
        // ==========================================
        colStatus.setCellFactory(column -> new TableCell<>() {
            private final Label badge = new Label();
            private final String baseStyle = "-fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 12;";

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setGraphic(null);
                } else {
                    switch (item.toUpperCase()) {
                        case "ACTIVE":
                            badge.setText("Đang diễn ra");
                            badge.setStyle(baseStyle + "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D;");
                            break;
                        case "SOLD":
                            badge.setText("Đã bán");
                            badge.setStyle(baseStyle + "-fx-background-color: #E0F2FE; -fx-text-fill: #0369A1;");
                            break;
                        case "CANCELLED":
                            badge.setText("Đã hủy");
                            badge.setStyle(baseStyle + "-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C;");
                            break;
                        case "UPCOMING":
                            badge.setText("Sắp diễn ra");
                            badge.setStyle(baseStyle + "-fx-background-color: #FEF3C7; -fx-text-fill: #B45309;");
                            break;
                        case "ENDED_NO_BID":
                            badge.setText("Không có lượt đặt");
                            badge.setStyle(baseStyle + "-fx-background-color: #F1F5F9; -fx-text-fill: #64748B;");
                            break;
                        default:
                            badge.setText(item);
                            badge.setStyle(baseStyle + "-fx-background-color: #F1F5F9; -fx-text-fill: #475569;");
                            break;
                    }
                    setGraphic(badge);
                    setAlignment(Pos.CENTER); // Ép huy hiệu ra chính giữa ô lưới
                }
            }
        });

        colEndTime.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getEndTime() != null ? dtf.format(cell.getValue().getEndTime()) : ""));

        colAction.setCellFactory(createActionCellFactory());

        filtered = new FilteredList<>(items, p -> true);
        SortedList<AuctionItemDTO> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(auctionTable.comparatorProperty());
        auctionTable.setItems(sorted);

        tfSearch.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        cbFilterStatus.valueProperty().addListener((obs, oldV, newV) -> applyFilters());

        loadAuctions();
    }

    private void loadAuctions() {
        AdminService.getAuctions(list -> {
            items.setAll(list != null ? list : List.of());
            applyFilters();
        });
    }

    private void applyFilters() {
        String q = tfSearch.getText() != null ? tfSearch.getText().trim().toLowerCase() : "";
        String status = cbFilterStatus.getValue();
        filtered.setPredicate(a -> {
            if (a == null) return false;
            boolean matchesQ = q.isEmpty()
                    || (a.getTitle() != null && a.getTitle().toLowerCase().contains(q))
                    || (a.getSellerUsername() != null && a.getSellerUsername().toLowerCase().contains(q));
            boolean matchesStatus = status == null || status.equals("Tất cả")
                    || (a.getStatus() != null && a.getStatus().name().equalsIgnoreCase(status));
            return matchesQ && matchesStatus;
        });
    }

    // ==========================================
    // CẢI TIẾN 2: THAY THẾ HBOX BUTTONS BẰNG PREMIUM DROPDOWN MENUBUTTON
    // ==========================================
    private Callback<TableColumn<AuctionItemDTO, Void>, TableCell<AuctionItemDTO, Void>> createActionCellFactory() {
        return col -> new TableCell<>() {
            private final MenuButton menuBtn = new MenuButton("Thao tác ⚙️");
            private final MenuItem itemBidders = new MenuItem("👥  Lịch sử đặt giá");
            private final MenuItem itemCancel = new MenuItem("❌  Hủy phiên đấu giá");

            {
                // Khóa cứng độ rộng tối thiểu để không bao giờ bị đứt chữ "Thao tác"
                menuBtn.setMinWidth(115);
                menuBtn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-font-weight: bold; "
                        + "-fx-background-radius: 6; -fx-padding: 5 10; -fx-cursor: hand; -fx-font-size: 12px;");

                itemBidders.setStyle("-fx-text-fill: #1E293B; -fx-font-size: 12px;");
                itemCancel.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-font-size: 12px;");

                menuBtn.getItems().addAll(itemBidders, itemCancel);

                itemCancel.setOnAction(e -> {
                    AuctionItemDTO a = getTableView().getItems().get(getIndex());
                    if (a != null) cancelAuction(a);
                });
                itemBidders.setOnAction(e -> {
                    AuctionItemDTO a = getTableView().getItems().get(getIndex());
                    if (a != null) showBiddersForItem(a.getItemId(), a.getTitle());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    AuctionItemDTO a = getTableView().getItems().get(getIndex());

                    boolean isEnded = a.getStatus() != null &&
                            (a.getStatus().name().equals("SOLD")
                                    || a.getStatus().name().equals("CANCELLED")
                                    || a.getStatus().name().equals("ENDED_NO_BID"));
                    itemCancel.setDisable(isEnded);

                    setGraphic(menuBtn);
                    setAlignment(Pos.CENTER); // Đảm bảo nút Thao tác nằm chính giữa cột Hành động
                }
            }
        };
    }

    private void cancelAuction(AuctionItemDTO a) {
        if (a == null) return;

        // FIX: getSessionId() trả Integer (nullable) — phải kiểm tra trước khi unbox
        Integer sessionId = a.getSessionId();
        if (sessionId == null) {
            AlertUtil.showError("Không thể huỷ: phiên đấu giá chưa có ID.");
            return;
        }

        boolean ok = AlertUtil.showConfirm("Xác nhận", "Bạn có chắc muốn huỷ phiên đấu giá: '" + a.getTitle() + "'?\n(Lưu ý: thao tác này có thể hoàn tiền cho người đã đặt giá)");
        if (!ok) return;
        AdminService.cancelAuction(sessionId, success -> {
            if (success) {
                AlertUtil.showInfo("Đã huỷ phiên đấu giá");
                loadAuctions();
            } else {
                AlertUtil.showError("Không thể huỷ phiên. Vui lòng thử lại.");
            }
        });
    }

    @FXML
    public void cancelSelected() {
        AuctionItemDTO sel = auctionTable.getSelectionModel().getSelectedItem();
        if (sel == null) { AlertUtil.showWarning("Vui lòng chọn phiên đấu giá"); return; }
        cancelAuction(sel);
    }

    private void showBiddersForItem(int itemId, String title) {
        vn.edu.vnu.uet.group8.client.service.AuctionService.getItemBidHistory(itemId, list -> {
            if (list == null || list.isEmpty()) {
                AlertUtil.showInfo("Không có lịch sử đặt giá cho '" + title + "'");
                return;
            }

            // Khởi tạo bộ format số chuẩn Việt Nam an toàn cho BigDecimal
            java.text.NumberFormat currencyFormat = java.text.NumberFormat.getNumberInstance(new java.util.Locale("vi", "VN"));

            StringBuilder sb = new StringBuilder();
            sb.append("Lịch sử đặt giá - ").append(title).append("\n\n");
            for (var r : list) {
                sb.append("• Người dùng: ").append(r.getDisplayName())
                        .append(" (ID: ").append(r.getUserId()).append(")\n")
                        // FIX: Sử dụng currencyFormat thay vì String.format("%,d")
                        .append("  ↳ Mức giá: ").append(currencyFormat.format(r.getAmount())).append(" đ\n")
                        .append("  ↳ Thời gian: ").append(r.getPlacedAt()).append("\n\n");
            }

            // Cần bọc Platform.runLater phòng khi callback chạy ngầm
            javafx.application.Platform.runLater(() -> AlertUtil.showInfo(sb.toString()));
        });
    }
}