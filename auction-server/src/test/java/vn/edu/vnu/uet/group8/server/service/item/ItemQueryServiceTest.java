package vn.edu.vnu.uet.group8.server.service.item;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.request.GetAuctionsRequest;
import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.exception.ItemNotFoundException;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO;
import vn.edu.vnu.uet.group8.server.dao.CommentDAO;
import vn.edu.vnu.uet.group8.server.dao.ItemDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;

/**

 * Test cho {@link ItemQueryService}.
 */
@ExtendWith(MockitoExtension.class)
class ItemQueryServiceTest {

    @Mock private ItemDAO itemDAO;
    @Mock private AuctionSessionDAO sessionDAO;
    @Mock private UserDAO userDAO;
    @Mock private BidTransactionDAO bidTransactionDAO;
    @Mock private CommentDAO commentDAO;

    private ItemQueryService service;

    @BeforeEach
    void setUp() {
        service = new ItemQueryService(
                itemDAO,
                sessionDAO,
                userDAO,
                bidTransactionDAO,
                commentDAO
        );
    }

    private Item taoItem(int id, int sellerId) {
        Item item = new Item.Builder(sellerId, "iPhone", ItemCategory.ELECTRONICS)
                .condition(ItemCondition.NEW)
                .description("desc")
                .build();
        item.assignId(id);
        return item;
    }

    private AuctionSession taoSession(int id, int itemId) {
        AuctionSession s = new AuctionSession.Builder(itemId,
                new BigDecimal("100"),
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(3600))
                .build();
        s.assignId(id);
        return s;
    }

    private UserMember taoSeller(int id) {
        UserMember m = new UserMember.Builder(
                "seller" + id,
                "s" + id + "@e.com",
                "hash")
                .build();


        m.assignId(id);
        return m;

    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getItemDetail()")
    class GetItemDetailTest {


        @Test
        @DisplayName("Item không tồn tại → ItemNotFoundException")
        void itemKhongTonTai() throws SQLException {

            when(itemDAO.findById(99))
                    .thenReturn(Optional.empty());

            assertThrows(
                    ItemNotFoundException.class,
                    () -> service.getItemDetail(99)
            );
        }

        @Test
        @DisplayName("Item có session ACTIVE → trả DTO có giá")
        void coActiveSession() throws SQLException {

            Item item = taoItem(10, 5);
            AuctionSession session = taoSession(20, 10);

            when(itemDAO.findById(10))
                    .thenReturn(Optional.of(item));

            when(sessionDAO.findActiveSessionByItemId(10))
                    .thenReturn(Optional.of(session));

            when(sessionDAO.findUpcomingByItemId(10))
                    .thenReturn(Optional.empty());

            when(userDAO.findById(5))
                    .thenReturn(Optional.of(taoSeller(5)));

            AuctionItemDTO dto = service.getItemDetail(10);

            assertNotNull(dto);
        }

        @Test
        @DisplayName("Item chưa có session → vẫn trả DTO (giá null)")
        void chuaCoSession() throws SQLException {

            Item item = taoItem(10, 5);

            when(itemDAO.findById(10))
                    .thenReturn(Optional.of(item));

            when(sessionDAO.findActiveSessionByItemId(10))
                    .thenReturn(Optional.empty());

            when(sessionDAO.findUpcomingByItemId(10))
                    .thenReturn(Optional.empty());

            when(userDAO.findById(5))
                    .thenReturn(Optional.of(taoSeller(5)));

            AuctionItemDTO dto = service.getItemDetail(10);

            assertNotNull(dto);
        }

        @Test
        @DisplayName("Có upcoming session (chưa active) → dùng upcoming")
        void coUpcomingSession() throws SQLException {

            Item item = taoItem(10, 5);
            AuctionSession session = taoSession(20, 10);

            when(itemDAO.findById(10))
                    .thenReturn(Optional.of(item));

            when(sessionDAO.findActiveSessionByItemId(10))
                    .thenReturn(Optional.empty());

            when(sessionDAO.findUpcomingByItemId(10))
                    .thenReturn(Optional.of(session));

            when(userDAO.findById(5))
                    .thenReturn(Optional.of(taoSeller(5)));

            AuctionItemDTO dto = service.getItemDetail(10);

            assertNotNull(dto);
        }


    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getMyItems()")
    class GetMyItemsTest {


        @Test
        @DisplayName("Seller không có item nào → trả empty list")
        void sellerKhongCoItem() throws SQLException {

            when(itemDAO.findBySeller(5))
                    .thenReturn(Collections.emptyList());

            assertTrue(service.getMyItems(5).isEmpty());
        }

        @Test
        @DisplayName("Seller có item, item có session → trả 1 phần tử")
        void coItemCoSession() throws SQLException {

            Item item = taoItem(10, 5);
            AuctionSession session = taoSession(20, 10);

            when(itemDAO.findBySeller(5))
                    .thenReturn(java.util.List.of(item));

            when(sessionDAO.findActiveSessionByItemId(10))
                    .thenReturn(Optional.of(session));

            when(userDAO.findById(5))
                    .thenReturn(Optional.of(taoSeller(5)));

            var result = service.getMyItems(5);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Seller có item nhưng chưa có session → vẫn trả DTO")
        void coItemKhongCoSession() throws SQLException {

            Item item = taoItem(10, 5);

            when(itemDAO.findBySeller(5))
                    .thenReturn(java.util.List.of(item));

            when(sessionDAO.findActiveSessionByItemId(10))
                    .thenReturn(Optional.empty());

            when(sessionDAO.findUpcomingByItemId(10))
                    .thenReturn(Optional.empty());

            when(sessionDAO.findByItemId(10))
                    .thenReturn(Collections.emptyList());

            when(userDAO.findById(5))
                    .thenReturn(Optional.of(taoSeller(5)));

            var result = service.getMyItems(5);

            assertEquals(1, result.size());
        }


    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getAuctions(filter)")
    class GetAuctionsTest {


        @Test
        @DisplayName("Filter null → gọi searchItems với NEWEST")
        void filterNull() throws SQLException {

            when(sessionDAO.findByPriceRange(any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            service.getAuctions(null);

            verify(sessionDAO).findByPriceRange(
                    isNull(),
                    isNull(),
                    isNull(),
                    eq(GetAuctionsRequest.SortOption.NEWEST)
            );

        }

        @Test
        @DisplayName("Filter có category → gọi searchItems")
        void filterCoCategory() throws SQLException {

            GetAuctionsRequest filter = GetAuctionsRequest.builder()
                    .category(ItemCategory.ELECTRONICS)
                    .build();

            when(sessionDAO.findByPriceRange(any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            service.getAuctions(filter);

            verify(sessionDAO).findByPriceRange(
                    eq(ItemCategory.ELECTRONICS),
                    any(),
                    any(),
                    any()
            );
        }

        @Test
        @DisplayName("Filter có price range → gọi searchItems")
        void filterCoPrice() throws SQLException {

            GetAuctionsRequest filter = GetAuctionsRequest.builder()
                    .minPrice(new BigDecimal("100"))
                    .maxPrice(new BigDecimal("1000"))
                    .build();

            when(sessionDAO.findByPriceRange(any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            service.getAuctions(filter);

            verify(sessionDAO).findByPriceRange(
                    any(),
                    eq(new BigDecimal("100")),
                    eq(new BigDecimal("1000")),
                    any()
            );
        }

        @Test
        @DisplayName("Filter empty → gọi searchItems")
        void filterEmpty() throws SQLException {

            GetAuctionsRequest filter =
                    GetAuctionsRequest.builder().build();

            when(sessionDAO.findByPriceRange(any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            service.getAuctions(filter);

            verify(sessionDAO).findByPriceRange(
                    isNull(),
                    isNull(),
                    isNull(),
                    eq(GetAuctionsRequest.SortOption.NEWEST)
            );


        }

    }
}
