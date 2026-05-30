package vn.edu.vnu.uet.group8.server.service.item;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.request.GetAuctionsRequest;
import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.entity.UserAdmin;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.AdminLevel;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;
import vn.edu.vnu.uet.group8.common.exception.ItemNotFoundException;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO;
import vn.edu.vnu.uet.group8.server.dao.ItemDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;

/**
 * Test FULL COVERAGE cho {@link ItemQueryService}.
 *
 * <p>Mục tiêu coverage: 56% → ~95%
 *
 * <p>Các method CẦN tăng (từ 0%):
 * <ul>
 *   <li>{@code getMyItemsByStatus()}  — 0%</li>
 *   <li>{@code resolveSellerUsername()} — 0% (private, test gián tiếp)</li>
 *   <li>{@code getWonItems()}          — 0%</li>
 *   <li>{@code getActiveItems()}       — 0%</li>
 *   <li>{@code getCommentsForAnItemId()} — 0%</li>
 *   <li>{@code buildDtoList()}         — 14% (nhiều branch chưa đi qua)</li>
 *   <li>{@code findRelevantSession()}  — 77% (branch cuối chưa cover)</li>
 * </ul>
 *
 * <p>Các method GIỮ VỮNG (đã có, bổ sung thêm branch):
 * <ul>
 *   <li>{@code getItemDetail()}  — 97%</li>
 *   <li>{@code getMyItems()}     — 96%</li>
 *   <li>{@code getAuctions()}    — 100%</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ItemQueryService - Full Coverage Tests")
class ItemQueryServiceTest {

    @Mock private ItemDAO           itemDAO;
    @Mock private AuctionSessionDAO sessionDAO;
    @Mock private UserDAO           userDAO;
    @Mock private BidTransactionDAO bidTransactionDAO;

    private ItemQueryService service;

    @BeforeEach
    void setUp() { 
        service = new ItemQueryService(
                itemDAO, sessionDAO, userDAO, bidTransactionDAO);
    }

    // ════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════

    private Item item(int id, int sellerId) {
        Item i = new Item.Builder(sellerId, "iPhone 17", ItemCategory.ELECTRONICS)
                .condition(ItemCondition.NEW)
                .description("Mô tả sản phẩm")
                .build();
        i.assignId(id);
        return i;
    }

    private Item item(int id, int sellerId, ItemCategory cat) {
        Item i = new Item.Builder(sellerId, "Sản phẩm " + cat, cat)
                .condition(ItemCondition.NEW)
                .description("Mô tả")
                .build();
        i.assignId(id);
        return i;
    }

    /** Tạo AuctionSession với status cụ thể qua reconstructor */
    private AuctionSession session(int id, int itemId, SessionStatus status) {
        AuctionSession s = AuctionSession.reconstructor()
                .id(id)
                .createdAt(Instant.now())
                .isDeleted(false)
                .itemId(itemId)
                .startingPrice(new BigDecimal("100000"))
                .currentPrice(new BigDecimal("150000"))
                .status(status)
                .startTime(Instant.now().minusSeconds(60))
                .endTime(Instant.now().plusSeconds(3600))
                .bidCount(3)
                .build();
        return s;
    }

    private AuctionSession sessionWithBidCount(int id, int itemId, int bidCount) {
        AuctionSession s = AuctionSession.reconstructor()
                .id(id)
                .createdAt(Instant.now())
                .isDeleted(false)
                .itemId(itemId)
                .startingPrice(new BigDecimal("100000"))
                .currentPrice(new BigDecimal("200000"))
                .status(SessionStatus.ACTIVE)
                .startTime(Instant.now().minusSeconds(60))
                .endTime(Instant.now().plusSeconds(3600))
                .bidCount(bidCount)
                .build();
        return s;
    }

    /** UserMember với sellerRating và totalItemsSold */
    private UserMember seller(int id, BigDecimal rating, int sold) {
        UserMember m = UserMember.builder("seller" + id, "s" + id + "@mail.com", "$hash")
                .phone("09000000" + id)
                .build();
        m.assignId(id);
        // dùng reconstructor để gán rating + sold
        return UserMember.reconstructor()
                .id(id)
                .createdAt(Instant.now())
                .isDeleted(false)
                .username("seller" + id)
                .email("s" + id + "@mail.com")
                .encryptedPassword("$hash")
                .status(vn.edu.vnu.uet.group8.common.enums.UserStatus.ACTIVE)
                .roles(java.util.EnumSet.of(
                        vn.edu.vnu.uet.group8.common.enums.UserRole.SELLER))
                .balance(new BigDecimal("500000"))
                .phone("09000000" + id)
                .sellerRating(rating)
                .totalItemsSold(sold)
                .build();
    }

    /** UserAdmin – để test nhánh seller instanceof UserMember = false */
    private UserAdmin adminSeller(int id) {
        UserAdmin a = new UserAdmin.Builder(
                "admin" + id, "admin" + id + "@mail.com",
                "$hash", AdminLevel.MODERATOR).build();
        a.assignId(id);
        return a;
    }

    // ════════════════════════════════════════════════════════════════
    // ① getItemDetail()  — tăng branch coverage từ 80% → 100%
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("① getItemDetail() — tất cả nhánh")
    class GetItemDetailFull {

        @Test
        @DisplayName("Item không tồn tại → ItemNotFoundException")
        void itemKhongTonTai() throws SQLException {
            when(itemDAO.findById(99)).thenReturn(Optional.empty());

            assertThrows(ItemNotFoundException.class,
                    () -> service.getItemDetail(99));
        }

        @Test
        @DisplayName("Session ACTIVE → dùng active, bidCount từ session")
        void coActiveSession() throws SQLException {
            Item item = item(10, 5);
            AuctionSession active = session(20, 10, SessionStatus.ACTIVE);
            UserMember sellerUser = seller(5, new BigDecimal("4.8"), 12);

            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findActiveSessionByItemId(10)).thenReturn(Optional.of(active));
            when(sessionDAO.findUpcomingByItemId(10)).thenReturn(Optional.empty());
            when(userDAO.findById(5)).thenReturn(Optional.of(sellerUser));

            AuctionItemDTO dto = service.getItemDetail(10);

            assertNotNull(dto);
            assertEquals("seller5", dto.getSellerUsername());
            assertEquals(3, dto.getBidCount());
            // sellerRating được map
            assertEquals(0, dto.getSellerRating().compareTo(new BigDecimal("4.8")));
        }

        @Test
        @DisplayName("Session UPCOMING (active=empty) → dùng upcoming")
        void coUpcomingKhongActive() throws SQLException {
            Item item = item(10, 5);
            AuctionSession upcoming = session(21, 10, SessionStatus.UPCOMING);
            UserMember sellerUser = seller(5, null, 0);

            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findActiveSessionByItemId(10)).thenReturn(Optional.empty());
            when(sessionDAO.findUpcomingByItemId(10)).thenReturn(Optional.of(upcoming));
            when(userDAO.findById(5)).thenReturn(Optional.of(sellerUser));

            AuctionItemDTO dto = service.getItemDetail(10);

            assertNotNull(dto);
            assertEquals(SessionStatus.UPCOMING, dto.getStatus());
        }

        @Test
        @DisplayName("Không có session nào → DTO với giá null")
        void khongCoSession() throws SQLException {
            Item item = item(10, 5);
            UserMember sellerUser = seller(5, null, 0);

            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findActiveSessionByItemId(10)).thenReturn(Optional.empty());
            when(sessionDAO.findUpcomingByItemId(10)).thenReturn(Optional.empty());
            when(userDAO.findById(5)).thenReturn(Optional.of(sellerUser));

            AuctionItemDTO dto = service.getItemDetail(10);

            assertNotNull(dto);
            assertNull(dto.getCurrentPrice(), "Không có session → currentPrice phải null");
            assertNull(dto.getEndTime(),       "Không có session → endTime phải null");
        }

        @Test
        @DisplayName("seller không tồn tại trong DB → username = 'seller#id'")
        void sellerKhongTonTai() throws SQLException {
            Item item = item(10, 999);

            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findActiveSessionByItemId(10)).thenReturn(Optional.empty());
            when(sessionDAO.findUpcomingByItemId(10)).thenReturn(Optional.empty());
            when(userDAO.findById(999)).thenReturn(Optional.empty());

            AuctionItemDTO dto = service.getItemDetail(10);

            assertEquals("seller#999", dto.getSellerUsername(),
                    "Seller không tồn tại → fallback 'seller#id'");
        }

        @Test
        @DisplayName("seller là UserAdmin (không phải UserMember) → rating=null, sold=0")
        void sellerLaAdmin() throws SQLException {
            Item item = item(10, 7);
            UserAdmin admin = adminSeller(7);

            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findActiveSessionByItemId(10)).thenReturn(Optional.empty());
            when(sessionDAO.findUpcomingByItemId(10)).thenReturn(Optional.empty());
            when(userDAO.findById(7)).thenReturn(Optional.of(admin));

            AuctionItemDTO dto = service.getItemDetail(10);

            assertNotNull(dto);
            assertNull(dto.getSellerRating(),
                    "UserAdmin không có sellerRating → phải null");
            assertEquals(0, dto.getTotalItemsSold());
        }

        @Test
        @DisplayName("seller là UserMember có rating → sellerRating đúng trong DTO")
        void sellerMemberCoRating() throws SQLException {
            Item item = item(10, 5);
            UserMember m = seller(5, new BigDecimal("4.5"), 20);

            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findActiveSessionByItemId(10)).thenReturn(Optional.empty());
            when(sessionDAO.findUpcomingByItemId(10)).thenReturn(Optional.empty());
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            AuctionItemDTO dto = service.getItemDetail(10);

            assertEquals(0, dto.getSellerRating().compareTo(new BigDecimal("4.5")));
            assertEquals(20, dto.getTotalItemsSold());
        }

        @Test
        @DisplayName("bidCount lấy từ session, không query BidTransactionDAO")
        void bidCountTuSessionKhongQueryBidDAO() throws SQLException {
            Item item = item(10, 5);
            AuctionSession s = sessionWithBidCount(20, 10, 17);
            UserMember m = seller(5, null, 0);

            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findActiveSessionByItemId(10)).thenReturn(Optional.of(s));
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            AuctionItemDTO dto = service.getItemDetail(10);

            assertEquals(17, dto.getBidCount());
            // BidTransactionDAO không được gọi
            verifyNoInteractions(bidTransactionDAO);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ② getActiveItems() — 0% → 100%
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("② getActiveItems() — 0% → 100%")
    class GetActiveItemsFull {

        @Test
        @DisplayName("Không có session active nào → trả empty list")
        void khongCoSessionActive() throws SQLException {
            when(sessionDAO.findAllActive()).thenReturn(Collections.emptyList());

            List<AuctionItemDTO> result = service.getActiveItems();

            assertTrue(result.isEmpty());
            verify(sessionDAO).findAllActive();
        }

        @Test
        @DisplayName("Có 1 session active + item hợp lệ → trả 1 DTO")
        void motSessionActive() throws SQLException {
            AuctionSession s = session(20, 10, SessionStatus.ACTIVE);
            Item item = item(10, 5);
            UserMember m = seller(5, new BigDecimal("4.0"), 5);

            when(sessionDAO.findAllActive()).thenReturn(List.of(s));
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            List<AuctionItemDTO> result = service.getActiveItems();

            assertEquals(1, result.size());
            assertEquals("seller5", result.get(0).getSellerUsername());
        }

        @Test
        @DisplayName("Có 3 session active khác nhau → trả 3 DTO theo thứ tự")
        void baSessionActive() throws SQLException {
            AuctionSession s1 = session(1, 10, SessionStatus.ACTIVE);
            AuctionSession s2 = session(2, 11, SessionStatus.ACTIVE);
            AuctionSession s3 = session(3, 12, SessionStatus.ACTIVE);
            Item i1 = item(10, 5); Item i2 = item(11, 5); Item i3 = item(12, 5);
            UserMember m = seller(5, null, 0);

            when(sessionDAO.findAllActive()).thenReturn(List.of(s1, s2, s3));
            when(itemDAO.findById(10)).thenReturn(Optional.of(i1));
            when(itemDAO.findById(11)).thenReturn(Optional.of(i2));
            when(itemDAO.findById(12)).thenReturn(Optional.of(i3));
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            List<AuctionItemDTO> result = service.getActiveItems();

            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("Session active nhưng item bị xóa (không tồn tại) → bỏ qua, log warn")
        void sessionCoItemBiXoa() throws SQLException {
            AuctionSession s1 = session(1, 10, SessionStatus.ACTIVE);  // item tồn tại
            AuctionSession s2 = session(2, 99, SessionStatus.ACTIVE);  // item không tồn tại
            Item item = item(10, 5);
            UserMember m = seller(5, null, 0);

            when(sessionDAO.findAllActive()).thenReturn(List.of(s1, s2));
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(itemDAO.findById(99)).thenReturn(Optional.empty()); // bị xóa
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            List<AuctionItemDTO> result = service.getActiveItems();

            // Session 2 bị skip → chỉ 1 kết quả
            assertEquals(1, result.size(),
                    "Session có item không tồn tại phải bị bỏ qua");
        }

        @Test
        @DisplayName("Tất cả session active đều có item bị xóa → trả empty list")
        void tatCaItemBiXoa() throws SQLException {
            AuctionSession s1 = session(1, 91, SessionStatus.ACTIVE);
            AuctionSession s2 = session(2, 92, SessionStatus.ACTIVE);

            when(sessionDAO.findAllActive()).thenReturn(List.of(s1, s2));
            when(itemDAO.findById(91)).thenReturn(Optional.empty());
            when(itemDAO.findById(92)).thenReturn(Optional.empty());

            List<AuctionItemDTO> result = service.getActiveItems();

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Cache seller: 2 item cùng sellerId → userDAO.findById chỉ gọi 1 lần")
        void cacheSellerTranhNPlusOne() throws SQLException {
            // 2 session, 2 item, cùng seller = 5
            AuctionSession s1 = session(1, 10, SessionStatus.ACTIVE);
            AuctionSession s2 = session(2, 11, SessionStatus.ACTIVE);
            Item i1 = item(10, 5); Item i2 = item(11, 5);
            UserMember m = seller(5, null, 0);

            when(sessionDAO.findAllActive()).thenReturn(List.of(s1, s2));
            when(itemDAO.findById(10)).thenReturn(Optional.of(i1));
            when(itemDAO.findById(11)).thenReturn(Optional.of(i2));
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            service.getActiveItems();

            // Cache hoạt động → chỉ gọi 1 lần dù có 2 item
            verify(userDAO, times(1)).findById(5);
        }

        @Test
        @DisplayName("Cache seller: seller null → userDAO.findById vẫn chỉ gọi 1 lần")
        void cacheSellerNull() throws SQLException {
            AuctionSession s1 = session(1, 10, SessionStatus.ACTIVE);
            AuctionSession s2 = session(2, 11, SessionStatus.ACTIVE);
            Item i1 = item(10, 99); Item i2 = item(11, 99); // seller 99 không tồn tại

            when(sessionDAO.findAllActive()).thenReturn(List.of(s1, s2));
            when(itemDAO.findById(10)).thenReturn(Optional.of(i1));
            when(itemDAO.findById(11)).thenReturn(Optional.of(i2));
            when(userDAO.findById(99)).thenReturn(Optional.empty());

            List<AuctionItemDTO> result = service.getActiveItems();

            assertEquals(2, result.size());
            // seller không tìm thấy → fallback username
            assertEquals("seller#99", result.get(0).getSellerUsername());
            // Cache: chỉ gọi 1 lần dù có 2 item cùng seller
            verify(userDAO, times(1)).findById(99);
        }

        @Test
        @DisplayName("buildDtoList: seller là UserAdmin → sellerRating=null, sold=0")
        void buildDtoListSellerAdmin() throws SQLException {
            AuctionSession s = session(1, 10, SessionStatus.ACTIVE);
            Item item = item(10, 7);
            UserAdmin admin = adminSeller(7);

            when(sessionDAO.findAllActive()).thenReturn(List.of(s));
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(userDAO.findById(7)).thenReturn(Optional.of(admin));

            List<AuctionItemDTO> result = service.getActiveItems();

            assertEquals(1, result.size());
            assertNull(result.get(0).getSellerRating());
            assertEquals(0, result.get(0).getTotalItemsSold());
        }

        @Test
        @DisplayName("bidCount trong DTO lấy từ session.getBidCount()")
        void bidCountTuSession() throws SQLException {
            AuctionSession s = sessionWithBidCount(1, 10, 42);
            Item item = item(10, 5);
            UserMember m = seller(5, null, 0);

            when(sessionDAO.findAllActive()).thenReturn(List.of(s));
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            List<AuctionItemDTO> result = service.getActiveItems();

            assertEquals(42, result.get(0).getBidCount());
            verifyNoInteractions(bidTransactionDAO);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ③ getWonItems() — 0% → 100%
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("③ getWonItems() — 0% → 100%")
    class GetWonItemsFull {

        @Test
        @DisplayName("User chưa thắng phiên nào → trả empty list")
        void chuaThang() throws SQLException {
            when(sessionDAO.findWonSessionsByUserId(10))
                    .thenReturn(Collections.emptyList());

            List<AuctionItemDTO> result = service.getWonItems(10);

            assertTrue(result.isEmpty());
            verify(sessionDAO).findWonSessionsByUserId(10);
        }

        @Test
        @DisplayName("User thắng 1 phiên → trả 1 DTO với đúng thông tin")
        void thangMotPhien() throws SQLException {
            AuctionSession won = session(20, 10, SessionStatus.SOLD);
            Item item = item(10, 5);
            UserMember m = seller(5, new BigDecimal("4.9"), 30);

            when(sessionDAO.findWonSessionsByUserId(7)).thenReturn(List.of(won));
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            List<AuctionItemDTO> result = service.getWonItems(7);

            assertEquals(1, result.size());
            assertEquals("seller5", result.get(0).getSellerUsername());
            assertEquals(0,
                    result.get(0).getSellerRating().compareTo(new BigDecimal("4.9")));
        }

        @Test
        @DisplayName("User thắng nhiều phiên → trả đúng số lượng DTO")
        void thangNhieuPhien() throws SQLException {
            AuctionSession w1 = session(1, 10, SessionStatus.SOLD);
            AuctionSession w2 = session(2, 11, SessionStatus.SOLD);
            AuctionSession w3 = session(3, 12, SessionStatus.SOLD);
            Item i1 = item(10, 5); Item i2 = item(11, 5); Item i3 = item(12, 5);
            UserMember m = seller(5, null, 0);

            when(sessionDAO.findWonSessionsByUserId(7))
                    .thenReturn(List.of(w1, w2, w3));
            when(itemDAO.findById(10)).thenReturn(Optional.of(i1));
            when(itemDAO.findById(11)).thenReturn(Optional.of(i2));
            when(itemDAO.findById(12)).thenReturn(Optional.of(i3));
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            List<AuctionItemDTO> result = service.getWonItems(7);

            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("Won session nhưng item đã bị xóa → bỏ qua (log warn)")
        void wonSessionItemBiXoa() throws SQLException {
            AuctionSession w1 = session(1, 10, SessionStatus.SOLD); // item OK
            AuctionSession w2 = session(2, 99, SessionStatus.SOLD); // item bị xóa
            Item item = item(10, 5);
            UserMember m = seller(5, null, 0);

            when(sessionDAO.findWonSessionsByUserId(7))
                    .thenReturn(List.of(w1, w2));
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(itemDAO.findById(99)).thenReturn(Optional.empty());
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            List<AuctionItemDTO> result = service.getWonItems(7);

            assertEquals(1, result.size(), "Item bị xóa phải bị bỏ qua");
        }

        @Test
        @DisplayName("findWonSessionsByUserId ném SQLException → propagate lên")
        void sqlExceptionPropagate() throws SQLException {
            when(sessionDAO.findWonSessionsByUserId(anyInt()))
                    .thenThrow(new SQLException("DB lỗi"));

            assertThrows(SQLException.class, () -> service.getWonItems(10));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ④ getMyItemsByStatus() — 0% → 100%
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("④ getMyItemsByStatus() — 0% → 100%")
    class GetMyItemsByStatusFull {

        @Test
        @DisplayName("Seller không có item với status này → trả empty list")
        void khongCoItemVoiStatus() throws SQLException {
            when(itemDAO.findBySellerAndStatus(5, ItemStatus.DRAFT))
                    .thenReturn(Collections.emptyList());

            List<AuctionItemDTO> result =
                    service.getMyItemsByStatus(5, ItemStatus.DRAFT);

            assertTrue(result.isEmpty());
        }

        @ParameterizedTest
        @EnumSource(ItemStatus.class)
        @DisplayName("Tất cả ItemStatus đều hoạt động")
        void tatCaStatus(ItemStatus status) throws SQLException {
            when(itemDAO.findBySellerAndStatus(5, status))
                    .thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> service.getMyItemsByStatus(5, status));

            verify(itemDAO).findBySellerAndStatus(5, status);
        }

        @Test
        @DisplayName("Lọc LISTED: item có session ACTIVE → trả DTO với session")
        void listedCoSession() throws SQLException {
            Item item = item(10, 5);
            AuctionSession active = session(20, 10, SessionStatus.ACTIVE);
            UserMember m = seller(5, new BigDecimal("3.5"), 8);

            when(itemDAO.findBySellerAndStatus(5, ItemStatus.LISTED))
                    .thenReturn(List.of(item));
            when(sessionDAO.findActiveSessionByItemId(10))
                    .thenReturn(Optional.of(active));
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            List<AuctionItemDTO> result =
                    service.getMyItemsByStatus(5, ItemStatus.LISTED);

            assertEquals(1, result.size());
            assertEquals(SessionStatus.ACTIVE, result.get(0).getStatus());
            assertEquals(3, result.get(0).getBidCount());
        }

        @Test
        @DisplayName("Lọc DRAFT: item không có session → trả DTO với price null")
        void draftKhongCoSession() throws SQLException {
            Item item = item(10, 5);
            UserMember m = seller(5, null, 0);

            when(itemDAO.findBySellerAndStatus(5, ItemStatus.DRAFT))
                    .thenReturn(List.of(item));
            when(sessionDAO.findActiveSessionByItemId(10))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findUpcomingByItemId(10))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findByItemId(10))
                    .thenReturn(Collections.emptyList());
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            List<AuctionItemDTO> result =
                    service.getMyItemsByStatus(5, ItemStatus.DRAFT);

            assertEquals(1, result.size());
            assertNull(result.get(0).getCurrentPrice());
        }

        @Test
        @DisplayName("Lọc SOLD: item có session cuối (findByItemId) → trả DTO với session đó")
        void soldCoSessionCuoi() throws SQLException {
            Item item = item(10, 5);
            AuctionSession sold = session(20, 10, SessionStatus.SOLD);
            UserMember m = seller(5, null, 0);

            when(itemDAO.findBySellerAndStatus(5, ItemStatus.SOLD))
                    .thenReturn(List.of(item));
            when(sessionDAO.findActiveSessionByItemId(10))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findUpcomingByItemId(10))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findByItemId(10))
                    .thenReturn(List.of(sold));
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            List<AuctionItemDTO> result =
                    service.getMyItemsByStatus(5, ItemStatus.SOLD);

            assertEquals(1, result.size());
            assertEquals(SessionStatus.SOLD, result.get(0).getStatus());
        }

        @Test
        @DisplayName("Nhiều item với cùng status → trả đúng số lượng")
        void nhieuItem() throws SQLException {
            Item i1 = item(10, 5); Item i2 = item(11, 5); Item i3 = item(12, 5);
            UserMember m = seller(5, null, 0);

            when(itemDAO.findBySellerAndStatus(5, ItemStatus.DRAFT))
                    .thenReturn(List.of(i1, i2, i3));
            when(sessionDAO.findActiveSessionByItemId(anyInt()))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findUpcomingByItemId(anyInt()))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findByItemId(anyInt()))
                    .thenReturn(Collections.emptyList());
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            List<AuctionItemDTO> result =
                    service.getMyItemsByStatus(5, ItemStatus.DRAFT);

            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("seller là UserAdmin trong getMyItemsByStatus → rating=null")
        void sellerAdminTrongMyItemsByStatus() throws SQLException {
            Item item = item(10, 7);
            UserAdmin admin = adminSeller(7);

            when(itemDAO.findBySellerAndStatus(7, ItemStatus.DRAFT))
                    .thenReturn(List.of(item));
            when(sessionDAO.findActiveSessionByItemId(10))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findUpcomingByItemId(10))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findByItemId(10))
                    .thenReturn(Collections.emptyList());
            when(userDAO.findById(7)).thenReturn(Optional.of(admin));

            List<AuctionItemDTO> result =
                    service.getMyItemsByStatus(7, ItemStatus.DRAFT);

            assertEquals(1, result.size());
            assertNull(result.get(0).getSellerRating());
        }

        @Test
        @DisplayName("itemDAO.findBySellerAndStatus ném SQLException → propagate")
        void sqlExceptionPropagate() throws SQLException {
            when(itemDAO.findBySellerAndStatus(anyInt(), any()))
                    .thenThrow(new SQLException("DB lỗi"));

            assertThrows(SQLException.class,
                    () -> service.getMyItemsByStatus(5, ItemStatus.DRAFT));
        }
    }



    // ════════════════════════════════════════════════════════════════
    // ⑥ findRelevantSession() — 77% → 100% (test gián tiếp qua getMyItems)
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("⑥ findRelevantSession() — branch coverage qua getMyItems")
    class FindRelevantSessionFull {

        @Test
        @DisplayName("Branch 1: có ACTIVE session → trả active ngay, không query upcoming/byItemId")
        void branch1Active() throws SQLException {
            Item item = item(10, 5);
            AuctionSession active = session(20, 10, SessionStatus.ACTIVE);
            UserMember m = seller(5, null, 0);

            when(itemDAO.findBySeller(5)).thenReturn(List.of(item));
            when(sessionDAO.findActiveSessionByItemId(10))
                    .thenReturn(Optional.of(active));
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            service.getMyItems(5);

            // Không cần query upcoming hay byItemId
            verify(sessionDAO, never()).findUpcomingByItemId(10);
            verify(sessionDAO, never()).findByItemId(10);
        }

        @Test
        @DisplayName("Branch 2: không có ACTIVE, có UPCOMING → trả upcoming, không query byItemId")
        void branch2Upcoming() throws SQLException {
            Item item = item(10, 5);
            AuctionSession upcoming = session(21, 10, SessionStatus.UPCOMING);
            UserMember m = seller(5, null, 0);

            when(itemDAO.findBySeller(5)).thenReturn(List.of(item));
            when(sessionDAO.findActiveSessionByItemId(10))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findUpcomingByItemId(10))
                    .thenReturn(Optional.of(upcoming));
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            List<AuctionItemDTO> result = service.getMyItems(5);

            assertEquals(1, result.size());
            assertEquals(SessionStatus.UPCOMING, result.get(0).getStatus());
            // Không cần query byItemId
            verify(sessionDAO, never()).findByItemId(10);
        }

        @Test
        @DisplayName("Branch 3: không ACTIVE/UPCOMING, có session cũ (SOLD/CANCELLED) → trả session[0]")
        void branch3SessionCu() throws SQLException {
            Item item = item(10, 5);
            AuctionSession sold = session(22, 10, SessionStatus.SOLD);
            UserMember m = seller(5, null, 0);

            when(itemDAO.findBySeller(5)).thenReturn(List.of(item));
            when(sessionDAO.findActiveSessionByItemId(10))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findUpcomingByItemId(10))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findByItemId(10)).thenReturn(List.of(sold));
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            List<AuctionItemDTO> result = service.getMyItems(5);

            assertEquals(1, result.size());
            assertEquals(SessionStatus.SOLD, result.get(0).getStatus());
        }

        @Test
        @DisplayName("Branch 4: không có bất kỳ session nào → Optional.empty()")
        void branch4KhongCoSession() throws SQLException {
            Item item = item(10, 5);
            UserMember m = seller(5, null, 0);

            when(itemDAO.findBySeller(5)).thenReturn(List.of(item));
            when(sessionDAO.findActiveSessionByItemId(10))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findUpcomingByItemId(10))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            List<AuctionItemDTO> result = service.getMyItems(5);

            assertEquals(1, result.size());
            assertNull(result.get(0).getCurrentPrice(),
                    "Không có session → currentPrice null");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ⑦ getMyItems() — tăng từ 96% → 100% (bổ sung branch còn thiếu)
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("⑦ getMyItems() — bổ sung branch còn thiếu")
    class GetMyItemsExtra {

        @Test
        @DisplayName("Seller không có item nào → trả empty list")
        void khongCoItem() throws SQLException {
            when(itemDAO.findBySeller(5)).thenReturn(Collections.emptyList());

            assertTrue(service.getMyItems(5).isEmpty());
        }

        @Test
        @DisplayName("seller không tồn tại → sellerUsername = 'seller#id'")
        void sellerKhongTonTai() throws SQLException {
            Item item = item(10, 888);

            when(itemDAO.findBySeller(888)).thenReturn(List.of(item));
            when(sessionDAO.findActiveSessionByItemId(10))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findUpcomingByItemId(10))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findByItemId(10))
                    .thenReturn(Collections.emptyList());
            when(userDAO.findById(888)).thenReturn(Optional.empty());

            List<AuctionItemDTO> result = service.getMyItems(888);

            assertEquals(1, result.size());
            assertEquals("seller#888", result.get(0).getSellerUsername());
        }

        @Test
        @DisplayName("seller là UserAdmin → rating=null, sold=0")
        void sellerAdmin() throws SQLException {
            Item item = item(10, 7);
            UserAdmin admin = adminSeller(7);

            when(itemDAO.findBySeller(7)).thenReturn(List.of(item));
            when(sessionDAO.findActiveSessionByItemId(10))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findUpcomingByItemId(10))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findByItemId(10))
                    .thenReturn(Collections.emptyList());
            when(userDAO.findById(7)).thenReturn(Optional.of(admin));

            List<AuctionItemDTO> result = service.getMyItems(7);

            assertEquals(1, result.size());
            assertNull(result.get(0).getSellerRating());
            assertEquals(0, result.get(0).getTotalItemsSold());
        }

        @Test
        @DisplayName("Nhiều item với session hỗn hợp → tất cả đều có trong kết quả")
        void nhieuItemHonHop() throws SQLException {
            Item i1 = item(10, 5); // có ACTIVE session
            Item i2 = item(11, 5); // có UPCOMING session
            Item i3 = item(12, 5); // không có session
            UserMember m = seller(5, new BigDecimal("4.2"), 15);

            AuctionSession active   = session(20, 10, SessionStatus.ACTIVE);
            AuctionSession upcoming = session(21, 11, SessionStatus.UPCOMING);

            when(itemDAO.findBySeller(5)).thenReturn(List.of(i1, i2, i3));
            when(sessionDAO.findActiveSessionByItemId(10))
                    .thenReturn(Optional.of(active));
            when(sessionDAO.findActiveSessionByItemId(11))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findActiveSessionByItemId(12))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findUpcomingByItemId(11))
                    .thenReturn(Optional.of(upcoming));
            when(sessionDAO.findUpcomingByItemId(12))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findByItemId(12))
                    .thenReturn(Collections.emptyList());
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            List<AuctionItemDTO> result = service.getMyItems(5);

            assertEquals(3, result.size());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ⑧ getAuctions() — 100% (giữ vững + bổ sung sortBy)
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("⑧ getAuctions() — giữ 100% + thêm sortBy")
    class GetAuctionsFull {

        @Test
        @DisplayName("filter null → gọi searchItems với NEWEST")
        void filterNull() throws SQLException {
            when(sessionDAO.findByPriceRange(any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            service.getAuctions(null);

            verify(sessionDAO).findByPriceRange(
                    isNull(), isNull(), isNull(),
                    eq(GetAuctionsRequest.SortOption.NEWEST));
        }

        @Test
        @DisplayName("filter với sortBy = ENDING_SOON → forward đúng")
        void filterEndingSoon() throws SQLException {
            GetAuctionsRequest filter = GetAuctionsRequest.builder()
                    .sortBy(GetAuctionsRequest.SortOption.ENDING_SOON)
                    .build();
            when(sessionDAO.findByPriceRange(any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            service.getAuctions(filter);

            verify(sessionDAO).findByPriceRange(
                    isNull(), isNull(), isNull(),
                    eq(GetAuctionsRequest.SortOption.ENDING_SOON));
        }

        @Test
        @DisplayName("filter với sortBy = PRICE_ASC → forward đúng")
        void filterPriceAsc() throws SQLException {
            GetAuctionsRequest filter = GetAuctionsRequest.builder()
                    .sortBy(GetAuctionsRequest.SortOption.PRICE_ASC)
                    .build();
            when(sessionDAO.findByPriceRange(any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            service.getAuctions(filter);

            verify(sessionDAO).findByPriceRange(
                    any(), any(), any(),
                    eq(GetAuctionsRequest.SortOption.PRICE_ASC));
        }

        @Test
        @DisplayName("filter với tất cả field → forward đúng tất cả")
        void filterDayDu() throws SQLException {
            GetAuctionsRequest filter = GetAuctionsRequest.builder()
                    .category(ItemCategory.FASHION)
                    .minPrice(new BigDecimal("500000"))
                    .maxPrice(new BigDecimal("10000000"))
                    .sortBy(GetAuctionsRequest.SortOption.HOT)
                    .build();
            when(sessionDAO.findByPriceRange(any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            service.getAuctions(filter);

            verify(sessionDAO).findByPriceRange(
                    eq(ItemCategory.FASHION),
                    eq(new BigDecimal("500000")),
                    eq(new BigDecimal("10000000")),
                    eq(GetAuctionsRequest.SortOption.HOT));
        }

        @Test
        @DisplayName("sessionDAO trả danh sách sessions → buildDtoList được gọi")
        void sessionsDaoTraList() throws SQLException {
            AuctionSession s = session(1, 10, SessionStatus.ACTIVE);
            Item item = item(10, 5);
            UserMember m = seller(5, null, 0);

            when(sessionDAO.findByPriceRange(any(), any(), any(), any()))
                    .thenReturn(List.of(s));
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            List<AuctionItemDTO> result = service.getAuctions(null);

            assertEquals(1, result.size());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ⑨ resolveSellerUsername() — 0% (private, test gián tiếp)
    //    (Method này tồn tại trong source nhưng hiện không được
    //     gọi trong code sản xuất — test để đảm bảo nhánh fallback
    //     hoạt động đúng qua các method public)
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("⑨ resolveSellerUsername() — gián tiếp qua getItemDetail")
    class ResolveSellerUsernameFull {

        @Test
        @DisplayName("Seller tồn tại → trả username thật")
        void sellerTonTai() throws SQLException {
            Item item = item(10, 5);
            UserMember m = seller(5, null, 0);

            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findActiveSessionByItemId(10))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findUpcomingByItemId(10))
                    .thenReturn(Optional.empty());
            when(userDAO.findById(5)).thenReturn(Optional.of(m));

            AuctionItemDTO dto = service.getItemDetail(10);

            assertEquals("seller5", dto.getSellerUsername());
        }

        @Test
        @DisplayName("Seller không tồn tại → trả 'seller#id' (không throw)")
        void sellerKhongTonTai() throws SQLException {
            Item item = item(10, 777);

            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findActiveSessionByItemId(10))
                    .thenReturn(Optional.empty());
            when(sessionDAO.findUpcomingByItemId(10))
                    .thenReturn(Optional.empty());
            when(userDAO.findById(777)).thenReturn(Optional.empty());

            AuctionItemDTO dto = service.getItemDetail(10);

            assertEquals("seller#777", dto.getSellerUsername(),
                    "Fallback phải là 'seller#id' khi không tìm thấy user");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ⑩ SQL EXCEPTION PROPAGATION — tất cả method đều test
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("⑩ SQLException propagation — tất cả method")
    class SqlExceptionPropagation {

        @Test
        @DisplayName("getItemDetail: itemDAO ném SQL → propagate")
        void getItemDetailSQL() throws SQLException {
            when(itemDAO.findById(anyInt()))
                    .thenThrow(new SQLException("DB lỗi"));
            assertThrows(SQLException.class, () -> service.getItemDetail(1));
        }

        @Test
        @DisplayName("getActiveItems: sessionDAO ném SQL → propagate")
        void getActiveItemsSQL() throws SQLException {
            when(sessionDAO.findAllActive())
                    .thenThrow(new SQLException("DB lỗi"));
            assertThrows(SQLException.class, () -> service.getActiveItems());
        }

        @Test
        @DisplayName("getMyItems: itemDAO ném SQL → propagate")
        void getMyItemsSQL() throws SQLException {
            when(itemDAO.findBySeller(anyInt()))
                    .thenThrow(new SQLException("DB lỗi"));
            assertThrows(SQLException.class, () -> service.getMyItems(5));
        }

        @Test
        @DisplayName("getMyItemsByStatus: sessionDAO ném SQL khi findActive → propagate")
        void getMyItemsByStatusSQL() throws SQLException {
            Item item = item(10, 5);
            when(itemDAO.findBySellerAndStatus(5, ItemStatus.LISTED))
                    .thenReturn(List.of(item));
            when(sessionDAO.findActiveSessionByItemId(10))
                    .thenThrow(new SQLException("DB lỗi"));

            assertThrows(SQLException.class,
                    () -> service.getMyItemsByStatus(5, ItemStatus.LISTED));
        }

        @Test
        @DisplayName("getWonItems: sessionDAO ném SQL → propagate")
        void getWonItemsSQL() throws SQLException {
            when(sessionDAO.findWonSessionsByUserId(anyInt()))
                    .thenThrow(new SQLException("DB lỗi"));
            assertThrows(SQLException.class, () -> service.getWonItems(5));
        }

        @Test
        @DisplayName("getAuctions: sessionDAO ném SQL → propagate")
        void getAuctionsSQL() throws SQLException {
            when(sessionDAO.findByPriceRange(any(), any(), any(), any()))
                    .thenThrow(new SQLException("DB lỗi"));
            assertThrows(SQLException.class, () -> service.getAuctions(null));
        }
    }
}