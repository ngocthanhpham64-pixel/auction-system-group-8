package vn.edu.vnu.uet.group8.server.service.auction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
import vn.edu.vnu.uet.group8.server.dao.AutoBidDAO;
import vn.edu.vnu.uet.group8.server.dao.DatabaseConnection;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO;

@ExtendWith(MockitoExtension.class)
class BidProcessorTest {

    @Mock private BidTransactionDAO bidTransactionDAO;
    @Mock private AutoBidDAO autoBidDAO;
    @Mock private AuctionSessionDAO sessionDAO;
    @Mock private DatabaseConnection dbConn;
    @Mock private Connection conn;

    private MockedStatic<DatabaseConnection> staticMock;

    @InjectMocks
    private BidProcessor processor;

    private UserMember bidder;
    private Item item;
    private AuctionSession session;

    @BeforeEach
    void setUp() throws SQLException {
        staticMock = mockStatic(DatabaseConnection.class);
        staticMock.when(DatabaseConnection::getInstance).thenReturn(dbConn);
        lenient().when(dbConn.getConnection()).thenReturn(conn);

        bidder = new UserMember.Builder("alice", "a@e.com", "Hash123").build();
        bidder.assignId(10);

        item = new Item.Builder(99, "iPhone 17", ItemCategory.ELECTRONICS)
                .condition(ItemCondition.NEW)
                .description("test")
                .build();
        item.assignId(20);

        session = new AuctionSession.Reconstructor()
                .id(30)
                .createdAt(Instant.now())
                .isDeleted(false)
                .itemId(20)
                .startingPrice(new BigDecimal("100"))
                .currentPrice(new BigDecimal("90"))
                .status(SessionStatus.ACTIVE)
                .startTime(Instant.now().minusSeconds(60))
                .endTime(Instant.now().plusSeconds(3600))
                .bidCount(0)
                .build();
    }

    @AfterEach
    void tearDown() {
        staticMock.close();
    }

    @Nested
    @DisplayName("Process bid thành công")
    class ProcessSuccess {

        @Test
        @DisplayName("Bid đầu tiên -> prevBidderId = null")
        void bidDauTien() throws SQLException {
            BidContext ctx = new BidContext(bidder, item, session, new BigDecimal("150"), false);

            when(sessionDAO.lockSessionForUpdate(any(), eq(30)))
                    .thenReturn(Optional.of(session));

            when(bidTransactionDAO.holdBalance(any(), eq(10), eq(new BigDecimal("150")), eq(30)))
                    .thenReturn(true);

            when(bidTransactionDAO.insertBidTransaction(any(), eq(10), eq(30), eq(new BigDecimal("100"))))
                    .thenReturn(1000L);

            when(bidTransactionDAO.updateSessionAfterFight(any(), eq(30), eq(new BigDecimal("100")), eq(10), eq(1)))
                    .thenReturn(1);

            when(bidTransactionDAO.countByItemInTx(any(), eq(30)))
                    .thenReturn(1);

            BidResult result = processor.resolveFight(ctx);

            assertEquals(1000L, result.getTransactionId());
            assertEquals(20, result.getItemId());
            assertEquals(10, result.getBidderId());
            assertEquals("alice", result.getBidderUsername());
            assertEquals(new BigDecimal("100"), result.getNewPrice());
            assertEquals(1, result.getTotalBids());
            assertNull(result.getPrevBidderId());
            assertFalse(result.hasPreviousBidder());
        }

        @Test
        @DisplayName("Bid vượt leader cũ")
        void bidVuotLeaderCu() throws SQLException {
            // Set up session with leader 77 and current price 150
            session = new AuctionSession.Reconstructor()
                    .id(30).createdAt(Instant.now()).isDeleted(false)
                    .itemId(20).startingPrice(new BigDecimal("100"))
                    .currentPrice(new BigDecimal("150"))
                    .status(SessionStatus.ACTIVE)
                    .startTime(Instant.now().minusSeconds(60))
                    .endTime(Instant.now().plusSeconds(3600))
                    .highestBidderId(77)
                    .bidCount(1)
                    .build();

            // Bidder bids 1200 (> minNextBid of 150 which is 1150)
            BidContext ctx = new BidContext(bidder, item, session, new BigDecimal("1200"), false);

            when(sessionDAO.lockSessionForUpdate(any(), eq(30)))
                    .thenReturn(Optional.of(session));

            when(autoBidDAO.findActiveBySession(any(), eq(30)))
                    .thenReturn(Collections.emptyList());

            when(bidTransactionDAO.holdBalance(any(), eq(10), eq(new BigDecimal("1200")), eq(30)))
                    .thenReturn(true);

            when(bidTransactionDAO.insertBidTransaction(any(), anyInt(), anyInt(), any()))
                    .thenReturn(2000L);

            when(bidTransactionDAO.updateSessionAfterFight(any(), eq(30), eq(new BigDecimal("1150")), eq(10), eq(2)))
                    .thenReturn(1);

            when(bidTransactionDAO.countByItemInTx(any(), eq(30)))
                    .thenReturn(2);

            BidResult result = processor.resolveFight(ctx);

            assertEquals(77, result.getPrevBidderId());
            assertTrue(result.hasPreviousBidder());
            assertEquals(2, result.getTotalBids());
            assertEquals(new BigDecimal("1150"), result.getNewPrice());
        }

        @Test
        @DisplayName("Sync AuctionSession memory")
        void syncSessionMemory() throws SQLException {
            BidContext ctx = new BidContext(bidder, item, session, new BigDecimal("250"), false);

            int bidCountTruoc = session.getBidCount();

            when(sessionDAO.lockSessionForUpdate(any(), eq(30)))
                    .thenReturn(Optional.of(session));

            when(bidTransactionDAO.holdBalance(any(), eq(10), eq(new BigDecimal("250")), eq(30)))
                    .thenReturn(true);

            when(bidTransactionDAO.insertBidTransaction(any(), eq(10), eq(30), eq(new BigDecimal("100"))))
                    .thenReturn(1L);

            when(bidTransactionDAO.updateSessionAfterFight(any(), eq(30), eq(new BigDecimal("100")), eq(10), eq(1)))
                    .thenReturn(1);

            when(bidTransactionDAO.countByItemInTx(any(), eq(30)))
                    .thenReturn(1);

            processor.resolveFight(ctx);

            assertEquals(new BigDecimal("100"), session.getCurrentPrice());
            assertEquals(bidCountTruoc + 1, session.getBidCount());
            assertEquals(10, session.getHighestBidderId());
        }
    }

    @Nested
    @DisplayName("Lỗi DB")
    class DbError {

        @Test
        @DisplayName("lockSessionForUpdate lỗi")
        void lockSessionLoi() throws SQLException {
            BidContext ctx = new BidContext(bidder, item, session, new BigDecimal("150"), false);

            when(sessionDAO.lockSessionForUpdate(any(), anyInt()))
                    .thenThrow(new SQLException("DB down"));

            assertThrows(SQLException.class, () -> processor.resolveFight(ctx));
        }

        @Test
        @DisplayName("insertBidTransaction lỗi -> không sync session")
        void insertBidLoi() throws SQLException {
            BidContext ctx = new BidContext(bidder, item, session, new BigDecimal("150"), false);

            BigDecimal giaTruoc = session.getCurrentPrice();

            when(sessionDAO.lockSessionForUpdate(any(), eq(30)))
                    .thenReturn(Optional.of(session));

            when(bidTransactionDAO.holdBalance(any(), eq(10), eq(new BigDecimal("150")), eq(30)))
                    .thenReturn(true);

            when(bidTransactionDAO.insertBidTransaction(any(), eq(10), eq(30), eq(new BigDecimal("100"))))
                    .thenThrow(new SQLException("Insert fail"));

            assertThrows(SQLException.class, () -> processor.resolveFight(ctx));
            assertEquals(giaTruoc, session.getCurrentPrice());
        }
    }

    @Test
    @DisplayName("BidResult.currentEndTime = session.endTime")
    void endTimeFromSession() throws SQLException {
        BidContext ctx = new BidContext(bidder, item, session, new BigDecimal("150"), false);

        when(sessionDAO.lockSessionForUpdate(any(), eq(30)))
                .thenReturn(Optional.of(session));

        when(bidTransactionDAO.holdBalance(any(), eq(10), eq(new BigDecimal("150")), eq(30)))
                .thenReturn(true);

        when(bidTransactionDAO.insertBidTransaction(any(), eq(10), eq(30), eq(new BigDecimal("100"))))
                .thenReturn(1L);

        when(bidTransactionDAO.updateSessionAfterFight(any(), eq(30), eq(new BigDecimal("100")), eq(10), eq(1)))
                .thenReturn(1);

        when(bidTransactionDAO.countByItemInTx(any(), eq(30)))
                .thenReturn(1);

        BidResult result = processor.resolveFight(ctx);

        assertEquals(session.getEndTime(), result.getCurrentEndTime());
    }
}