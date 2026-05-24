package vn.edu.vnu.uet.group8.client.controller;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.model.UserBidHistoryDTO;
import vn.edu.vnu.uet.group8.common.dto.model.UserProfileDTO;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProfileController")
class ProfileControllerTest extends FxTestBase {

    private ProfileController controller;

    @BeforeEach
    void setup() {

        SessionManager.clearSession();

        controller = new ProfileController();

        controller.lblName        = new Label();
        controller.lblEmail       = new Label();
        controller.lblAvatar      = new Label();
        controller.lblBalance     = new Label();
        controller.lblRating      = new Label();
        controller.lblTotalBids   = new Label();
        controller.lblActiveBids  = new Label();
        controller.lblWonBids     = new Label();

        controller.bidHistoryList = new VBox();

        controller.btnTabActive   = new Button();
        controller.btnTabWon      = new Button();
        controller.btnTabLost     = new Button();

        controller.activeTab      = controller.btnTabActive;
        controller.currentFilter  = "active";
        controller.allBids        = List.of();
    }

    private UserBidHistoryDTO bid(
            int itemId,
            BigDecimal amount,
            BigDecimal sessionPrice,
            Instant endTime
    ) {

        return UserBidHistoryDTO.of(
                1,
                1,
                itemId,
                "Item " + itemId,
                amount,
                sessionPrice,
                Instant.now(),
                endTime
        );
    }

    private UserProfileDTO profile(
            String fullName,
            BigDecimal rating
    ) {

        UserProfileDTO dto = mock(UserProfileDTO.class);

        when(dto.getFullName()).thenReturn(fullName);
        when(dto.getSellerRating()).thenReturn(rating);

        return dto;
    }

    // ─────────────────────────────────────
    // displayUser
    // ─────────────────────────────────────

    @Nested
    @DisplayName("displayUser")
    class DisplayUser {

        @Test
        @DisplayName("user hợp lệ → labels được set")
        void valid_user_sets_labels() {

            UserProfileDTO user =
                    profile("Nguyen Van A", new BigDecimal("4.5"));

            controller.displayUser(user);

            assertEquals(
                    "Nguyen Van A",
                    controller.lblName.getText()
            );

            assertTrue(
                    controller.lblRating.getText().contains("4.5")
            );
        }

        @Test
        @DisplayName("user null → không crash")
        void null_user_no_crash() {

            assertDoesNotThrow(
                    () -> controller.displayUser(null)
            );
        }

        @Test
        @DisplayName("fullName null → label không bị overwrite")
        void null_fullname_label_unchanged() {

            UserProfileDTO user = profile(null, null);

            controller.lblName.setText("Cũ");

            controller.displayUser(user);

            assertEquals(
                    "Cũ",
                    controller.lblName.getText()
            );
        }

        @Test
        @DisplayName("rating null → lblRating không bị overwrite")
        void null_rating_label_unchanged() {

            UserProfileDTO user =
                    profile("ABC", null);

            controller.lblRating.setText("--");

            controller.displayUser(user);

            assertEquals(
                    "--",
                    controller.lblRating.getText()
            );
        }
    }

    // ─────────────────────────────────────
    // updateBalance
    // ─────────────────────────────────────

    @Nested
    @DisplayName("updateBalance")
    class UpdateBalance {

        @Test
        @DisplayName("value hợp lệ → label được set")
        void valid_value() {

            controller.updateBalance(
                    new BigDecimal("1500000")
            );

            assertTrue(
                    controller.lblBalance
                            .getText()
                            .contains("1,500,000")
            );
        }

        @Test
        @DisplayName("null → hiển thị 0")
        void null_shows_zero() {

            controller.updateBalance(null);

            assertTrue(
                    controller.lblBalance
                            .getText()
                            .contains("0")
            );
        }
    }

    // ─────────────────────────────────────
    // matchTab
    // ─────────────────────────────────────

    @Nested
    @DisplayName("matchTab")
    class MatchTab {

        @Test
        @DisplayName("active + chưa kết thúc → match")
        void active_filter_match() {

            controller.currentFilter = "active";

            assertTrue(
                    controller.matchTab(
                            bid(
                                    1,
                                    new BigDecimal("100"),
                                    new BigDecimal("90"),
                                    null
                            )
                    )
            );
        }

        @Test
        @DisplayName("active + đã kết thúc → không match")
        void active_filter_no_match() {

            controller.currentFilter = "active";

            assertFalse(
                    controller.matchTab(
                            bid(
                                    1,
                                    new BigDecimal("100"),
                                    new BigDecimal("90"),
                                    Instant.now().minusSeconds(3600)
                            )
                    )
            );
        }

        @Test
        @DisplayName("won + thắng → match")
        void won_match() {

            controller.currentFilter = "won";

            assertTrue(
                    controller.matchTab(
                            bid(
                                    1,
                                    new BigDecimal("200"),
                                    new BigDecimal("200"),
                                    Instant.now().minusSeconds(3600)
                            )
                    )
            );
        }

        @Test
        @DisplayName("won + thua → không match")
        void won_no_match() {

            controller.currentFilter = "won";

            assertFalse(
                    controller.matchTab(
                            bid(
                                    1,
                                    new BigDecimal("100"),
                                    new BigDecimal("200"),
                                    Instant.now().minusSeconds(3600)
                            )
                    )
            );
        }

        @Test
        @DisplayName("lost + thua → match")
        void lost_match() {

            controller.currentFilter = "lost";

            assertTrue(
                    controller.matchTab(
                            bid(
                                    1,
                                    new BigDecimal("100"),
                                    new BigDecimal("200"),
                                    Instant.now().minusSeconds(3600)
                            )
                    )
            );
        }

        @Test
        @DisplayName("lost + thắng → không match")
        void lost_no_match() {

            controller.currentFilter = "lost";

            assertFalse(
                    controller.matchTab(
                            bid(
                                    1,
                                    new BigDecimal("300"),
                                    new BigDecimal("300"),
                                    Instant.now().minusSeconds(3600)
                            )
                    )
            );
        }
    }

    // ─────────────────────────────────────
    // updateStats
    // ─────────────────────────────────────

    @Nested
    @DisplayName("updateStats")
    class UpdateStats {

        @Test
        @DisplayName("empty bids → all labels = 0")
        void empty_bids() {

            controller.allBids = List.of();

            controller.updateStats();

            assertEquals("0", controller.lblTotalBids.getText());
            assertEquals("0", controller.lblActiveBids.getText());
            assertEquals("0", controller.lblWonBids.getText());
        }

        @Test
        @DisplayName("mixed bids → count đúng")
        void mixed_bids() {

            UserBidHistoryDTO active1 =
                    bid(
                            1,
                            new BigDecimal("100"),
                            new BigDecimal("90"),
                            null
                    );

            UserBidHistoryDTO active2 =
                    bid(
                            2,
                            new BigDecimal("200"),
                            new BigDecimal("150"),
                            Instant.now().plusSeconds(3600)
                    );

            UserBidHistoryDTO won =
                    bid(
                            3,
                            new BigDecimal("300"),
                            new BigDecimal("300"),
                            Instant.now().minusSeconds(3600)
                    );

            controller.allBids =
                    List.of(active1, active2, won);

            controller.updateStats();

            assertEquals("3", controller.lblTotalBids.getText());
            assertEquals("2", controller.lblActiveBids.getText());
            assertEquals("1", controller.lblWonBids.getText());
        }
    }

    // ─────────────────────────────────────
    // renderBids
    // ─────────────────────────────────────

    @Nested
    @DisplayName("renderBids")
    class RenderBids {

        @Test
        @DisplayName("empty bids → show empty label")
        void empty_bids_shows_label() {

            controller.allBids = List.of();

            controller.renderBids();

            assertEquals(
                    1,
                    controller.bidHistoryList.getChildren().size()
            );
        }

        @Test
        @DisplayName("has bids → render rows")
        void has_bids() {

            controller.currentFilter = "active";

            controller.allBids =
                    List.of(
                            bid(
                                    1,
                                    new BigDecimal("100"),
                                    new BigDecimal("90"),
                                    null
                            )
                    );

            controller.renderBids();

            assertEquals(
                    1,
                    controller.bidHistoryList.getChildren().size()
            );
        }

        @Test
        @DisplayName("bidHistoryList null → không crash")
        void null_list() {

            controller.bidHistoryList = null;

            assertDoesNotThrow(
                    () -> controller.renderBids()
            );
        }
    }

    // ─────────────────────────────────────
    // tab switching
    // ─────────────────────────────────────

    @Nested
    @DisplayName("tab switching")
    class TabSwitching {

        @Test
        void tab_active() {

            controller.onTabActive();

            assertEquals(
                    "active",
                    controller.currentFilter
            );
        }

        @Test
        void tab_won() {

            controller.onTabWon();

            assertEquals(
                    "won",
                    controller.currentFilter
            );
        }

        @Test
        void tab_lost() {

            controller.onTabLost();

            assertEquals(
                    "lost",
                    controller.currentFilter
            );
        }
    }
}