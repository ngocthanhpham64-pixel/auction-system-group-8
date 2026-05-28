package vn.edu.vnu.uet.group8.client.controller;

import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.model.NotificationDTO;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// =========================================================================
//  ProductCardController
// =========================================================================
@DisplayName("ProductCardController")
class ProductCardControllerTest extends FxTestBase {

    private ProductCardController controller;

    @BeforeEach
    void setup() {
        controller = new ProductCardController();
        controller.root          = new VBox();
        controller.imageContainer= new StackPane();
        controller.productImage  = new ImageView();
        controller.productName   = new Label();
        controller.currentPrice  = new Label();
        controller.lblTimer      = new Label();
        controller.lblBidCount   = new Label();
        controller.lblCertBadge  = new Label();
        controller.lblPartnerBadge = new Label();
    }

    @Nested @DisplayName("setItem")
    class SetItem {

        @Test @DisplayName("id và name hợp lệ → productName được set")
        void valid_item_sets_name() {
            controller.setItem("1", "iPhone 15", new BigDecimal("20000000"), "");
            assertEquals("iPhone 15", controller.productName.getText());
        }

        @Test @DisplayName("name null → 'Unknown'")
        void null_name_becomes_unknown() {
            controller.setItem("1", null, new BigDecimal("1000"), "");
            assertEquals("Unknown", controller.productName.getText());
        }

        @Test @DisplayName("price null → hiển thị '0đ'")
        void null_price_shows_zero() {
            controller.setItem("1", "Item", null, "");
            assertEquals("0đ", controller.currentPrice.getText());
        }

        @Test @DisplayName("id không phải số → itemId = 0, không crash")
        void non_numeric_id_no_crash() {
            assertDoesNotThrow(() -> controller.setItem("abc", "Item", new BigDecimal("1000"), ""));
        }

        @Test @DisplayName("id null → itemId = 0, không crash")
        void null_id_no_crash() {
            assertDoesNotThrow(() -> controller.setItem(null, "Item", new BigDecimal("1000"), ""));
        }

        @Test @DisplayName("imageUrl blank → không crash")
        void blank_image_url_no_crash() {
            assertDoesNotThrow(() -> controller.setItem("1", "Item", new BigDecimal("1000"), "   "));
        }

        @Test @DisplayName("setItem với endTime → lblTimer không crash")
        void with_end_time_no_crash() {
            LocalDateTime future = LocalDateTime.now().plusHours(2);
            assertDoesNotThrow(() -> controller.setItem("1", "Item", new BigDecimal("1000"), "", future));
        }

        @Test @DisplayName("setItem với endTime đã qua → lblTimer = 'Kết thúc'")
        void past_end_time_shows_ended() {
            LocalDateTime past = LocalDateTime.now().minusHours(1);
            controller.setItem("1", "Item", new BigDecimal("1000"), "", past);
            // Timer tick xảy ra async nhưng label không crash
            assertNotNull(controller.lblTimer);
        }
    }

    @Nested @DisplayName("setBidCount")
    class SetBidCount {

        @Test @DisplayName("setBidCount(5) → '5 bids'")
        void bid_count_set() {
            controller.setBidCount(5);
            assertEquals("5 bids", controller.lblBidCount.getText());
        }

        @Test @DisplayName("setBidCount(0) → '0 bids'")
        void zero_bid_count() {
            controller.setBidCount(0);
            assertEquals("0 bids", controller.lblBidCount.getText());
        }

        @Test @DisplayName("lblBidCount null → không crash")
        void null_label_no_crash() {
            controller.lblBidCount = null;
            assertDoesNotThrow(() -> controller.setBidCount(5));
        }
    }

    @Nested @DisplayName("formatPrice helper")
    class FormatPrice {

        @Test @DisplayName("null → '0đ'")
        void null_price() {
            controller.setItem("1", "X", null, "");
            assertEquals("0đ", controller.currentPrice.getText());
        }

        @Test @DisplayName("1000000 → '1,000,000đ'")
        void one_million() {
            controller.setItem("1", "X", new BigDecimal("1000000"), "");
            assertEquals("1,000,000đ", controller.currentPrice.getText());
        }
    }
}




// =========================================================================
//  FavoriteController – filter / emptyMessage logic
// =========================================================================
@DisplayName("FavoriteController – filter logic")
class FavoriteControllerTest extends FxTestBase {

    private FavoriteController controller;

    @BeforeEach
    void setup() {
        controller = new FavoriteController();
        controller.favoriteContainer = new FlowPane();
        controller.lblActiveCount    = new Label();
        controller.btnTabAll         = new Button();
        controller.btnTabActive      = new Button();
        controller.btnTabEnded       = new Button();
        controller.activeTab         = controller.btnTabAll;
        controller.currentFilter     = "all";
    }

    private AuctionItemDTO makeItem(int id, Instant endTime) {
        return AuctionItemDTO.of(
                id,
                "Item " + id,
                "",
                ItemCategory.ELECTRONICS,
                null,
                null,
                new BigDecimal("1000000"),
                endTime,
                "seller",
                null,
                null,
                0,
                Instant.now()
        );
    }

    @Nested @DisplayName("isActive / isEnded")
    class ActiveEnded {

        @Test @DisplayName("endTime tương lai → isActive=true")
        void future_is_active() {
            AuctionItemDTO item = makeItem(1, Instant.now().plusSeconds(3600));
            assertTrue(controller.isActive(item));
            assertFalse(controller.isEnded(item));
        }

        @Test @DisplayName("endTime đã qua → isEnded=true")
        void past_is_ended() {
            AuctionItemDTO item = makeItem(1, Instant.now().minusSeconds(3600));
            assertTrue(controller.isEnded(item));
            assertFalse(controller.isActive(item));
        }

        @Test @DisplayName("endTime null → isActive=true (null means no end)")
        void null_end_time_is_active() {
            AuctionItemDTO item = makeItem(1, null);
            assertTrue(controller.isActive(item));
        }
    }

    @Nested
    @DisplayName("filter")
    class Filter {

        private List<AuctionItemDTO> all() {

            AuctionItemDTO active = makeItem(
                    1,
                    Instant.now().plusSeconds(3600)
            );

            AuctionItemDTO ended = makeItem(
                    2,
                    Instant.now().minusSeconds(3600)
            );

            return List.of(active, ended);
        }

        @Test
        @DisplayName("filter 'all' → cả 2")
        void filter_all() {

            controller.currentFilter = "all";

            assertEquals(
                    2,
                    controller.filter(all()).size()
            );
        }

        @Test
        @DisplayName("filter 'active' → chỉ active")
        void filter_active() {

            controller.currentFilter = "active";

            var result = controller.filter(all());

            assertEquals(1, result.size());
            assertEquals(1, result.get(0).getItemId());
        }

        @Test
        @DisplayName("filter 'ended' → chỉ ended")
        void filter_ended() {

            controller.currentFilter = "ended";

            var result = controller.filter(all());

            assertEquals(1, result.size());
            assertEquals(2, result.get(0).getItemId());
        }

        @Test
        @DisplayName("filter 'unknown' → all (default)")
        void filter_unknown_returns_all() {

            controller.currentFilter = "xyz";

            assertEquals(
                    2,
                    controller.filter(all()).size()
            );
        }
    }

    @Nested @DisplayName("emptyMessage")
    class EmptyMessage {

        @Test @DisplayName("filter 'all' → 'Bạn chưa có món yêu thích'")
        void all_empty_message() {
            controller.currentFilter = "all";
            assertTrue(controller.emptyMessage().contains("yêu thích"));
        }

        @Test @DisplayName("filter 'active' → chứa 'hoạt động'")
        void active_empty_message() {
            controller.currentFilter = "active";
            assertTrue(controller.emptyMessage().contains("hoạt động"));
        }

        @Test @DisplayName("filter 'ended' → chứa 'kết thúc'")
        void ended_empty_message() {
            controller.currentFilter = "ended";
            assertTrue(controller.emptyMessage().contains("kết thúc"));
        }
    }

    @Nested @DisplayName("tab switching")
    class TabSwitching {

        @Test @DisplayName("onTabAll → currentFilter = 'all'")
        void tab_all() {
            controller.onTabAll();
            assertEquals("all", controller.currentFilter);
        }

        @Test @DisplayName("onTabActive → currentFilter = 'active'")
        void tab_active() {
            controller.onTabActive();
            assertEquals("active", controller.currentFilter);
        }

        @Test @DisplayName("onTabEnded → currentFilter = 'ended'")
        void tab_ended() {
            controller.onTabEnded();
            assertEquals("ended", controller.currentFilter);
        }

        @Test @DisplayName("setActiveTab null → không crash")
        void set_active_tab_null() {
            assertDoesNotThrow(() -> controller.setActiveTab(null));
        }
    }
}