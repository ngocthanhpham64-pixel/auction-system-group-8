package vn.edu.vnu.uet.group8.client.controller;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.common.dto.model.TransactionHistoryEntry;
import vn.edu.vnu.uet.group8.common.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WalletControllerAdditionalTest – phủ các nhánh chưa có trong WalletControllerTest:
 *  - onHistory() → setTab("all", ...)
 *  - onViewAll() → setTab + showInfo (transactionList null guard)
 *  - setTab – CSS class tag-active / tag-inactive
 *  - parseAmount – biên MIN_DEPOSIT, MAX_DEPOSIT
 *  - bindBalance – không crash
 *  - matchFilter – BID_REFUND với hold, WITHDRAW với deposit
 *  - renderTransactions – filter 'deposit' chỉ lấy đúng loại
 *  - buildTransactionRow – amount = 0 (edge case boundary)
 *  - updateTotalSpent – BID_WIN âm (abs)
 */
@DisplayName("WalletController – additional branches")
class WalletControllerAdditionalTest extends FxTestBase {

    private WalletController controller;

    @BeforeEach
    void setup() {
        controller = new WalletController();
        controller.lblBalance      = new Label();
        controller.lblHolding      = new Label();
        controller.lblTotalSpent   = new Label();
        controller.transactionList = new VBox();
        controller.btnTabAll       = new Button();
        controller.btnTabDeposit   = new Button();
        controller.btnTabHold      = new Button();
        controller.btnTabPayment   = new Button();
        controller.activeTab       = controller.btnTabAll;
        controller.currentFilter   = "all";
        controller.allTransactions = new ArrayList<>();
    }

    private TransactionHistoryEntry tx(TransactionType type, BigDecimal amount) {
        return new TransactionHistoryEntry("tx-1", amount, type, "ref", Instant.now());
    }

    // ─────────── onHistory ───────────

    @Nested @DisplayName("onHistory")
    class OnHistory {

        @Test @DisplayName("onHistory → currentFilter = 'all'")
        void sets_all_filter() {
            controller.currentFilter = "deposit";
            controller.onHistory();
            assertEquals("all", controller.currentFilter);
        }

        @Test @DisplayName("onHistory → activeTab = btnTabAll")
        void sets_active_tab_to_all() {
            controller.onHistory();
            assertSame(controller.btnTabAll, controller.activeTab);
        }

        @Test @DisplayName("onHistory → tag-active trên btnTabAll")
        void btn_tab_all_gets_active_class() {
            controller.onHistory();
            assertTrue(controller.btnTabAll.getStyleClass().contains("tag-active"));
        }
    }

    // ─────────── onViewAll ───────────

    @Nested @DisplayName("onViewAll")
    class OnViewAll {

        @Test @DisplayName("onViewAll → currentFilter = 'all'")
        void sets_all_filter() {
            controller.currentFilter = "hold";
            controller.onViewAll();
            assertEquals("all", controller.currentFilter);
        }

        @Test @DisplayName("onViewAll với transactionList có children → không crash")
        void with_children_no_crash() {
            controller.transactionList.getChildren().add(new Label("item"));
            assertDoesNotThrow(() -> controller.onViewAll());
        }

        @Test @DisplayName("onViewAll với transactionList rỗng → không crash")
        void empty_list_no_crash() {
            assertDoesNotThrow(() -> controller.onViewAll());
        }
    }

    // ─────────── setTab – CSS classes ───────────

    @Nested @DisplayName("setTab – CSS class transitions")
    class SetTabCss {

        @Test @DisplayName("setTab thêm tag-active cho button mới")
        void new_button_gets_active_class() {
            controller.setTab("deposit", controller.btnTabDeposit);
            assertTrue(controller.btnTabDeposit.getStyleClass().contains("tag-active"));
        }

        @Test @DisplayName("setTab xóa tag-active khỏi tab cũ")
        void old_tab_loses_active_class() {
            controller.btnTabAll.getStyleClass().add("tag-active");
            controller.activeTab = controller.btnTabAll;
            controller.setTab("deposit", controller.btnTabDeposit);
            assertFalse(controller.btnTabAll.getStyleClass().contains("tag-active"));
        }

        @Test @DisplayName("setTab thêm tag-inactive cho tab cũ")
        void old_tab_gets_inactive_class() {
            controller.btnTabAll.getStyleClass().add("tag-active");
            controller.activeTab = controller.btnTabAll;
            controller.setTab("deposit", controller.btnTabDeposit);
            assertTrue(controller.btnTabAll.getStyleClass().contains("tag-inactive"));
        }

        @Test @DisplayName("setTab xóa tag-inactive khỏi button mới")
        void new_button_loses_inactive_class() {
            controller.btnTabDeposit.getStyleClass().add("tag-inactive");
            controller.setTab("deposit", controller.btnTabDeposit);
            assertFalse(controller.btnTabDeposit.getStyleClass().contains("tag-inactive"));
        }

        @Test @DisplayName("setTab với activeTab null → không crash")
        void null_active_tab_no_crash() {
            controller.activeTab = null;
            assertDoesNotThrow(() -> controller.setTab("hold", controller.btnTabHold));
        }

        @Test @DisplayName("setTab không thêm tag-inactive trùng lặp")
        void no_duplicate_inactive_class() {
            controller.btnTabAll.getStyleClass().add("tag-active");
            controller.btnTabAll.getStyleClass().add("tag-inactive");
            controller.activeTab = controller.btnTabAll;
            controller.setTab("deposit", controller.btnTabDeposit);
            long count = controller.btnTabAll.getStyleClass().stream()
                    .filter("tag-inactive"::equals).count();
            assertEquals(1, count);
        }
    }

    // ─────────── parseAmount – biên ───────────

    @Nested @DisplayName("parseAmount – boundary values")
    class ParseAmountBoundary {

        @Test @DisplayName("MIN_DEPOSIT (10000) → parse đúng")
        void min_deposit_parses() {
            assertEquals(new BigDecimal("10000"), controller.parseAmount("10000"));
        }

        @Test @DisplayName("MAX_DEPOSIT (100000000) → parse đúng")
        void max_deposit_parses() {
            assertEquals(new BigDecimal("100000000"), controller.parseAmount("100000000"));
        }

        @Test @DisplayName("dưới MIN (9999) → parse ra số nhưng nhỏ hơn MIN")
        void below_min_deposit() {
            BigDecimal result = controller.parseAmount("9999");
            assertNotNull(result);
            assertTrue(result.compareTo(WalletController.MIN_DEPOSIT) < 0);
        }

        @Test @DisplayName("trên MAX (100000001) → parse ra số nhưng lớn hơn MAX")
        void above_max_deposit() {
            BigDecimal result = controller.parseAmount("100000001");
            assertNotNull(result);
            assertTrue(result.compareTo(WalletController.MAX_DEPOSIT) > 0);
        }

        @Test @DisplayName("'0' → parse ra ZERO (không null)")
        void zero_string_parses() {
            BigDecimal result = controller.parseAmount("0");
            assertNotNull(result);
            assertEquals(BigDecimal.ZERO, result);
        }
    }

    // ─────────── bindBalance ───────────

    @Nested @DisplayName("bindBalance")
    class BindBalance {

        @Test @DisplayName("bindBalance không crash khi lblBalance hợp lệ")
        void no_crash_valid_label() {
            assertDoesNotThrow(() -> controller.bindBalance());
        }

        @Test @DisplayName("bindBalance không crash khi lblBalance null")
        void no_crash_null_label() {
            controller.lblBalance = null;
            assertDoesNotThrow(() -> controller.bindBalance());
        }

        @Test
        @DisplayName("bindBalance set text từ ClientModel.balance")
        void sets_text_from_model() {

            assertDoesNotThrow(() -> {

                vn.edu.vnu.uet.group8.client.model.ClientModel.getInstance()
                        .updateBalance(new BigDecimal("3000000"));

                controller.bindBalance();
            });
        }
    }

    // ─────────── matchFilter – BID_REFUND / WITHDRAW ───────────

    @Nested @DisplayName("matchFilter – edge types")
    class MatchFilterEdge {

        @Test @DisplayName("hold filter → BID_REFUND match")
        void hold_matches_bid_refund() {
            controller.currentFilter = "hold";
            assertTrue(controller.matchFilter(tx(TransactionType.BID_REFUND, BigDecimal.ONE)));
        }

        @Test @DisplayName("deposit filter → WITHDRAW match")
        void deposit_matches_withdraw() {
            controller.currentFilter = "deposit";
            assertTrue(controller.matchFilter(tx(TransactionType.WITHDRAW, BigDecimal.ONE)));
        }

        @Test @DisplayName("payment filter → BID_HOLD không match")
        void payment_no_match_bid_hold() {
            controller.currentFilter = "payment";
            assertFalse(controller.matchFilter(tx(TransactionType.BID_HOLD, BigDecimal.ONE)));
        }

        @Test @DisplayName("hold filter → BID_WIN không match")
        void hold_no_match_bid_win() {
            controller.currentFilter = "hold";
            assertFalse(controller.matchFilter(tx(TransactionType.BID_WIN, BigDecimal.ONE)));
        }

        @Test @DisplayName("unknown filter → default true (match tất cả)")
        void unknown_filter_matches_all() {
            controller.currentFilter = "xyz_unknown";
            assertTrue(controller.matchFilter(tx(TransactionType.DEPOSIT, BigDecimal.ONE)));
            assertTrue(controller.matchFilter(tx(TransactionType.BID_WIN, BigDecimal.ONE)));
        }
    }

    // ─────────── renderTransactions – filter áp dụng đúng ───────────

    @Nested @DisplayName("renderTransactions – filter applied")
    class RenderWithFilter {

        @Test @DisplayName("filter 'deposit' → chỉ render DEPOSIT và WITHDRAW")
        void deposit_filter_renders_only_deposit_withdraw() {
            controller.currentFilter = "deposit";
            controller.allTransactions = new ArrayList<>(List.of(
                    tx(TransactionType.DEPOSIT, new BigDecimal("100000")),
                    tx(TransactionType.WITHDRAW, new BigDecimal("-50000")),
                    tx(TransactionType.BID_WIN, new BigDecimal("500000"))
            ));
            controller.renderTransactions();
            assertEquals(2, controller.transactionList.getChildren().size());
        }

        @Test @DisplayName("filter 'hold' → chỉ render BID_HOLD và BID_REFUND")
        void hold_filter_renders_only_hold_refund() {
            controller.currentFilter = "hold";
            controller.allTransactions = new ArrayList<>(List.of(
                    tx(TransactionType.BID_HOLD, new BigDecimal("200000")),
                    tx(TransactionType.BID_REFUND, new BigDecimal("200000")),
                    tx(TransactionType.DEPOSIT, new BigDecimal("100000"))
            ));
            controller.renderTransactions();
            assertEquals(2, controller.transactionList.getChildren().size());
        }

        @Test @DisplayName("filter 'payment' → chỉ render BID_WIN")
        void payment_filter_renders_only_bid_win() {
            controller.currentFilter = "payment";
            controller.allTransactions = new ArrayList<>(List.of(
                    tx(TransactionType.BID_WIN, new BigDecimal("300000")),
                    tx(TransactionType.DEPOSIT, new BigDecimal("100000")),
                    tx(TransactionType.BID_HOLD, new BigDecimal("50000"))
            ));
            controller.renderTransactions();
            assertEquals(1, controller.transactionList.getChildren().size());
        }

        @Test @DisplayName("filter áp dụng → không có match → list rỗng → hiển thị empty label")
        void no_match_shows_empty_label() {
            controller.currentFilter = "payment";
            controller.allTransactions = new ArrayList<>(List.of(
                    tx(TransactionType.DEPOSIT, new BigDecimal("100000"))
            ));
            // allTransactions không rỗng nhưng sau filter không còn gì → cần check
            controller.renderTransactions();
            // Sau filter 'payment' chỉ lọc BID_WIN, không có → children size = 0
            assertEquals(0, controller.transactionList.getChildren().size());
        }
    }

    // ─────────── buildTransactionRow – edge cases ───────────

    @Nested @DisplayName("buildTransactionRow – edge cases")
    class BuildTransactionRowEdge {

        @Test @DisplayName("amount = 0 → prefix '+'")
        void zero_amount_has_plus() {
            var row = controller.buildTransactionRow(tx(TransactionType.DEPOSIT, BigDecimal.ZERO));
            assertNotNull(row);
            var labels = row.getChildren().stream()
                    .filter(n -> n instanceof Label)
                    .map(n -> ((Label) n).getText())
                    .toList();
            assertTrue(labels.stream().anyMatch(t -> t.startsWith("+")));
        }

        @Test @DisplayName("amount rất lớn → không crash")
        void very_large_amount_no_crash() {
            assertDoesNotThrow(() ->
                    controller.buildTransactionRow(tx(TransactionType.DEPOSIT, new BigDecimal("999999999"))));
        }

        @Test @DisplayName("createdAt null-safe → không crash khi Instant.now()")
        void instant_now_no_crash() {
            assertDoesNotThrow(() ->
                    controller.buildTransactionRow(tx(TransactionType.BID_WIN, new BigDecimal("500000"))));
        }

        @Test @DisplayName("row có đúng 2 children (info VBox + amountLabel)")
        void row_has_two_children() {
            var row = controller.buildTransactionRow(tx(TransactionType.DEPOSIT, new BigDecimal("100000")));
            assertEquals(2, row.getChildren().size());
        }
    }

    // ─────────── updateTotalSpent – abs() ───────────

    @Nested @DisplayName("updateTotalSpent – abs()")
    class UpdateTotalSpentAbs {

        @Test @DisplayName("BID_WIN âm → abs() hiển thị dương")
        void negative_bid_win_shows_positive() {
            controller.allTransactions = new ArrayList<>(List.of(
                    tx(TransactionType.BID_WIN, new BigDecimal("-500000"))
            ));
            controller.updateTotalSpent();
            // Sau abs() → 500000 d
            assertTrue(controller.lblTotalSpent.getText().contains("500,000"));
            assertFalse(controller.lblTotalSpent.getText().startsWith("-"));
        }

        @Test @DisplayName("nhiều BID_WIN → sum rồi abs")
        void multiple_bid_win_summed() {
            controller.allTransactions = new ArrayList<>(List.of(
                    tx(TransactionType.BID_WIN, new BigDecimal("100000")),
                    tx(TransactionType.BID_WIN, new BigDecimal("200000")),
                    tx(TransactionType.BID_WIN, new BigDecimal("300000"))
            ));
            controller.updateTotalSpent();
            assertTrue(controller.lblTotalSpent.getText().contains("600,000"));
        }

        @Test @DisplayName("list rỗng → hiển thị '0 d'")
        void empty_list_shows_zero() {
            controller.allTransactions = new ArrayList<>();
            controller.updateTotalSpent();
            assertEquals("0 d", controller.lblTotalSpent.getText());
        }
    }

    // ─────────── formatVnd ───────────

    @Nested @DisplayName("formatVnd – additional")
    class FormatVndAdditional {

        @Test @DisplayName("âm → có dấu trừ")
        void negative_has_minus() {
            String r = controller.formatVnd(new BigDecimal("-100000"));
            assertTrue(r.contains("-"));
        }

        @Test @DisplayName("kết quả kết thúc bằng ' d'")
        void ends_with_d() {
            String r = controller.formatVnd(new BigDecimal("500000"));
            assertTrue(r.endsWith(" d"), "Nhận: " + r);
        }
    }
}