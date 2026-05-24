package vn.edu.vnu.uet.group8.client.controller;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.common.dto.model.TransactionHistoryEntry;
import vn.edu.vnu.uet.group8.common.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WalletController")
class WalletControllerTest extends FxTestBase {

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
        controller.allTransactions = new java.util.ArrayList<>();
    }

    private TransactionHistoryEntry tx(TransactionType type, BigDecimal amount) {
        return new TransactionHistoryEntry(
                "tx-1",
                amount,
                type,
                "ref",
                Instant.now()
        );
    }

    // ─────────── parseAmount ───────────

    @Nested @DisplayName("parseAmount")
    class ParseAmount {

        @Test @DisplayName("số thuần → BigDecimal đúng")
        void plain_number() {
            assertEquals(new BigDecimal("500000"), controller.parseAmount("500000"));
        }

        @Test @DisplayName("có dấu phẩy → vẫn parse được")
        void formatted_number() {
            assertEquals(new BigDecimal("500000"), controller.parseAmount("500,000"));
        }

        @Test @DisplayName("null → null")
        void null_input() {
            assertNull(controller.parseAmount(null));
        }

        @Test @DisplayName("blank → null")
        void blank_input() {
            assertNull(controller.parseAmount("   "));
        }

        @Test @DisplayName("chỉ chữ → null")
        void letters_only() {
            assertNull(controller.parseAmount("abc"));
        }

        @Test @DisplayName("rỗng → null")
        void empty_input() {
            assertNull(controller.parseAmount(""));
        }

        @Test @DisplayName("có ký tự đặc biệt → lấy chữ số")
        void special_chars() {
            assertEquals(new BigDecimal("1000"), controller.parseAmount("1.000đ"));
        }
    }

    // ─────────── formatVnd ───────────

    @Nested @DisplayName("formatVnd")
    class FormatVnd {

        @Test @DisplayName("1000000 → chứa '1,000,000'")
        void one_million() {
            String r = controller.formatVnd(new BigDecimal("1000000"));
            assertTrue(r.contains("1,000,000"), "Nhận: " + r);
        }

        @Test @DisplayName("ZERO → chứa '0'")
        void zero() {
            String r = controller.formatVnd(BigDecimal.ZERO);
            assertTrue(r.contains("0"));
        }
    }

    // ─────────── updateBalanceLabel ───────────

    @Nested @DisplayName("updateBalanceLabel")
    class UpdateBalanceLabel {

        @Test @DisplayName("balance hợp lệ → label được set")
        void valid_balance() {
            controller.updateBalanceLabel(new BigDecimal("2000000"));
            assertTrue(controller.lblBalance.getText().contains("2,000,000"));
        }

        @Test @DisplayName("null balance → hiển thị 0")
        void null_balance_shows_zero() {
            controller.updateBalanceLabel(null);
            assertTrue(controller.lblBalance.getText().contains("0"));
        }

        @Test @DisplayName("lblBalance null → không crash")
        void null_label_no_crash() {
            controller.lblBalance = null;
            assertDoesNotThrow(() -> controller.updateBalanceLabel(new BigDecimal("1000")));
        }
    }

    // ─────────── matchFilter ───────────

    @Nested @DisplayName("matchFilter")
    class MatchFilter {

        @Test @DisplayName("filter 'all' → match mọi loại")
        void all_filter_matches_all() {
            controller.currentFilter = "all";
            assertTrue(controller.matchFilter(tx(TransactionType.DEPOSIT, BigDecimal.ONE)));
            assertTrue(controller.matchFilter(tx(TransactionType.BID_WIN, BigDecimal.ONE)));
            assertTrue(controller.matchFilter(tx(TransactionType.BID_HOLD, BigDecimal.ONE)));
        }

        @Test @DisplayName("filter 'deposit' → match DEPOSIT")
        void deposit_filter_matches_deposit() {
            controller.currentFilter = "deposit";
            assertTrue(controller.matchFilter(tx(TransactionType.DEPOSIT, BigDecimal.ONE)));
        }

        @Test @DisplayName("filter 'deposit' → match WITHDRAW")
        void deposit_filter_matches_withdraw() {
            controller.currentFilter = "deposit";
            assertTrue(controller.matchFilter(tx(TransactionType.WITHDRAW, BigDecimal.ONE)));
        }

        @Test @DisplayName("filter 'deposit' → không match BID_WIN")
        void deposit_filter_no_bid_win() {
            controller.currentFilter = "deposit";
            assertFalse(controller.matchFilter(tx(TransactionType.BID_WIN, BigDecimal.ONE)));
        }

        @Test @DisplayName("filter 'hold' → match BID_HOLD")
        void hold_filter_matches_bid_hold() {
            controller.currentFilter = "hold";
            assertTrue(controller.matchFilter(tx(TransactionType.BID_HOLD, BigDecimal.ONE)));
        }

        @Test @DisplayName("filter 'hold' → match BID_REFUND")
        void hold_filter_matches_bid_refund() {
            controller.currentFilter = "hold";
            assertTrue(controller.matchFilter(tx(TransactionType.BID_REFUND, BigDecimal.ONE)));
        }

        @Test @DisplayName("filter 'hold' → không match DEPOSIT")
        void hold_filter_no_deposit() {
            controller.currentFilter = "hold";
            assertFalse(controller.matchFilter(tx(TransactionType.DEPOSIT, BigDecimal.ONE)));
        }

        @Test @DisplayName("filter 'payment' → match BID_WIN")
        void payment_filter_matches_bid_win() {
            controller.currentFilter = "payment";
            assertTrue(controller.matchFilter(tx(TransactionType.BID_WIN, BigDecimal.ONE)));
        }

        @Test @DisplayName("filter 'payment' → không match DEPOSIT")
        void payment_filter_no_deposit() {
            controller.currentFilter = "payment";
            assertFalse(controller.matchFilter(tx(TransactionType.DEPOSIT, BigDecimal.ONE)));
        }
    }

    // ─────────── renderTransactions ───────────

    @Nested @DisplayName("renderTransactions")
    class RenderTransactions {

        @Test @DisplayName("list rỗng → hiển thị empty label")
        void empty_list_shows_empty_label() {
            controller.allTransactions = new java.util.ArrayList<>();
            controller.renderTransactions();
            assertEquals(1, controller.transactionList.getChildren().size());
            assertTrue(controller.transactionList.getChildren().get(0) instanceof Label);
        }

        @Test @DisplayName("có transaction → render rows")
        void has_transactions_renders_rows() {
            controller.allTransactions = new java.util.ArrayList<>(List.of(
                    tx(TransactionType.DEPOSIT, new BigDecimal("100000")),
                    tx(TransactionType.BID_WIN, new BigDecimal("500000"))
            ));
            controller.renderTransactions();
            assertEquals(2, controller.transactionList.getChildren().size());
        }

        @Test @DisplayName("transactionList null → không crash")
        void null_list_no_crash() {
            controller.transactionList = null;
            assertDoesNotThrow(() -> controller.renderTransactions());
        }
    }

    // ─────────── updateTotalSpent ───────────

    @Nested @DisplayName("updateTotalSpent")
    class UpdateTotalSpent {

        @Test @DisplayName("chỉ BID_WIN → sum đúng")
        void only_bid_win_summed() {
            controller.allTransactions = new java.util.ArrayList<>(List.of(
                    tx(TransactionType.BID_WIN, new BigDecimal("200000")),
                    tx(TransactionType.BID_WIN, new BigDecimal("300000")),
                    tx(TransactionType.DEPOSIT, new BigDecimal("100000"))
            ));
            controller.updateTotalSpent();
            assertTrue(controller.lblTotalSpent.getText().contains("500,000"));
        }

        @Test @DisplayName("không có BID_WIN → hiển thị 0")
        void no_bid_win_shows_zero() {
            controller.allTransactions = new java.util.ArrayList<>(List.of(
                    tx(TransactionType.DEPOSIT, new BigDecimal("100000"))
            ));
            controller.updateTotalSpent();
            assertTrue(controller.lblTotalSpent.getText().contains("0"));
        }

        @Test @DisplayName("lblTotalSpent null → không crash")
        void null_label_no_crash() {
            controller.lblTotalSpent = null;
            assertDoesNotThrow(() -> controller.updateTotalSpent());
        }
    }

    // ─────────── setTab ───────────

    @Nested @DisplayName("setTab")
    class SetTab {

        @Test @DisplayName("onTabDeposit → currentFilter = 'deposit'")
        void tab_deposit() {
            controller.onTabDeposit();
            assertEquals("deposit", controller.currentFilter);
        }

        @Test @DisplayName("onTabHold → currentFilter = 'hold'")
        void tab_hold() {
            controller.onTabHold();
            assertEquals("hold", controller.currentFilter);
        }

        @Test @DisplayName("onTabPayment → currentFilter = 'payment'")
        void tab_payment() {
            controller.onTabPayment();
            assertEquals("payment", controller.currentFilter);
        }

        @Test @DisplayName("onTabAll → currentFilter = 'all'")
        void tab_all() {
            controller.onTabDeposit();
            controller.onTabAll();
            assertEquals("all", controller.currentFilter);
        }

        @Test @DisplayName("activeTab được cập nhật sau setTab")
        void active_tab_updated() {
            controller.setTab("deposit", controller.btnTabDeposit);
            assertSame(controller.btnTabDeposit, controller.activeTab);
        }
    }

    // ─────────── buildTransactionRow ───────────

    @Nested @DisplayName("buildTransactionRow")
    class BuildTransactionRow {

        @Test @DisplayName("amount dương → '+' prefix")
        void positive_amount_has_plus() {
            var row = controller.buildTransactionRow(tx(TransactionType.DEPOSIT, new BigDecimal("100000")));
            assertNotNull(row);
            // row chứa label amount với "+"
            var labels = row.getChildren().stream()
                    .filter(n -> n instanceof Label)
                    .map(n -> ((Label) n).getText())
                    .toList();
            assertTrue(labels.stream().anyMatch(t -> t.startsWith("+")));
        }

        @Test @DisplayName("amount âm → không có '+' prefix")
        void negative_amount_no_plus() {
            var row = controller.buildTransactionRow(tx(TransactionType.WITHDRAW, new BigDecimal("-100000")));
            assertNotNull(row);
            var labels = row.getChildren().stream()
                    .filter(n -> n instanceof Label)
                    .map(n -> ((Label) n).getText())
                    .toList();
            assertFalse(labels.stream().anyMatch(t -> t.startsWith("+")));
        }
    }
}