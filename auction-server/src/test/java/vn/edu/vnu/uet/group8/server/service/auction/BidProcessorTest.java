package vn.edu.vnu.uet.group8.server.service.auction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO.BidExecutionResult;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO.LeaderInfo;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

@ExtendWith(MockitoExtension.class)
class BidProcessorTest {

    @Mock
    private BidTransactionDAO bidTransactionDAO;

    @InjectMocks
    private BidProcessor processor;

    private UserMember bidder;
    private Item item;
    private AuctionSession session;

    @BeforeEach
    void setUp() {
        bidder = new UserMember.Builder("alice", "a@e.com", "Hash123")
                .build();
        bidder.assignId(10);

        item = new Item.Builder(99, "iPhone 17", ItemCategory.ELECTRONICS)
                .condition(ItemCondition.NEW)
                .description("test")
                .build();
        item.assignId(20);

        session = new AuctionSession.Builder(
                20,
                new BigDecimal("100"),
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(3600)
        ).build();

        session.assignId(30);

        // QUAN TRỌNG
        session.transitionStatus(
                SessionStatus.UPCOMING,
                SessionStatus.ACTIVE
        );
    }
    @Nested
    @DisplayName("Process bid thành công")
    class ProcessSuccess {

        @Test
        @DisplayName("Bid đầu tiên -> prevBidderId = null")
        void bidDauTien() throws SQLException {

            BidContext ctx = new BidContext(
                    bidder,
                    item,
                    session,
                    new BigDecimal("150")
            );

            // process() gọi bằng sessionId = 30
            when(bidTransactionDAO.findCurrentLeader(30))
                    .thenReturn(Optional.empty());

            when(bidTransactionDAO.executeBid(
                    eq(10),
                    eq(30),
                    eq(new BigDecimal("150")),
                    any()))
                    .thenReturn(new BidExecutionResult(1000L, 1));

            BidResult result = processor.process(ctx);

            assertEquals(1000L, result.getTransactionId());
            assertEquals(20, result.getItemId());
            assertEquals(10, result.getBidderId());
            assertEquals("alice", result.getBidderUsername());

            assertEquals(new BigDecimal("150"), result.getNewPrice());

            assertEquals(1, result.getTotalBids());

            assertNull(result.getPrevBidderId());
            assertFalse(result.hasPreviousBidder());
        }

        @Test
        @DisplayName("Bid vượt leader cũ")
        void bidVuotLeaderCu() throws SQLException {

            BidContext ctx = new BidContext(
                    bidder,
                    item,
                    session,
                    new BigDecimal("200")
            );

            LeaderInfo prevLeader =
                    new LeaderInfo(77, new BigDecimal("150"));

            when(bidTransactionDAO.findCurrentLeader(30))
                    .thenReturn(Optional.of(prevLeader));

            when(bidTransactionDAO.executeBid(
                    anyInt(),
                    anyInt(),
                    any(),
                    any()))
                    .thenReturn(new BidExecutionResult(2000L, 2));

            BidResult result = processor.process(ctx);

            assertEquals(77, result.getPrevBidderId());
            assertTrue(result.hasPreviousBidder());

            assertEquals(2, result.getTotalBids());
        }

        @Test
        @DisplayName("Sync AuctionSession memory")
        void syncSessionMemory() throws SQLException {

            BidContext ctx = new BidContext(
                    bidder,
                    item,
                    session,
                    new BigDecimal("250")
            );

            BigDecimal giaTruoc = session.getCurrentPrice();
            int bidCountTruoc = session.getBidCount();

            when(bidTransactionDAO.findCurrentLeader(30))
                    .thenReturn(Optional.empty());

            when(bidTransactionDAO.executeBid(
                    anyInt(),
                    anyInt(),
                    any(),
                    any()))
                    .thenReturn(new BidExecutionResult(1L, 1));

            processor.process(ctx);

            // currentPrice giờ thường = bid amount mới
            assertEquals(
                    new BigDecimal("250"),
                    session.getCurrentPrice()
            );

            assertEquals(
                    bidCountTruoc + 1,
                    session.getBidCount()
            );

            assertEquals(
                    10,
                    session.getHighestBidderId()
            );
        }
    }

    @Nested
    @DisplayName("Lỗi DB")
    class DbError {

        @Test
        @DisplayName("findCurrentLeader lỗi")
        void findLeaderLoi() throws SQLException {

            BidContext ctx = new BidContext(
                    bidder,
                    item,
                    session,
                    new BigDecimal("150")
            );

            when(bidTransactionDAO.findCurrentLeader(anyInt()))
                    .thenThrow(new SQLException("DB down"));

            assertThrows(
                    SQLException.class,
                    () -> processor.process(ctx)
            );
        }

        @Test
        @DisplayName("executeBid lỗi -> không sync session")
        void executeBidLoi() throws SQLException {

            BidContext ctx = new BidContext(
                    bidder,
                    item,
                    session,
                    new BigDecimal("150")
            );

            BigDecimal giaTruoc = session.getCurrentPrice();

            when(bidTransactionDAO.findCurrentLeader(30))
                    .thenReturn(Optional.empty());

            when(bidTransactionDAO.executeBid(
                    anyInt(),
                    anyInt(),
                    any(),
                    any()))
                    .thenThrow(new SQLException("Insert fail"));

            assertThrows(
                    SQLException.class,
                    () -> processor.process(ctx)
            );

            assertEquals(
                    giaTruoc,
                    session.getCurrentPrice()
            );
        }
    }

    @Test
    @DisplayName("BidResult.currentEndTime = session.endTime")
    void endTimeFromSession() throws SQLException {

        BidContext ctx = new BidContext(
                bidder,
                item,
                session,
                new BigDecimal("150")
        );

        when(bidTransactionDAO.findCurrentLeader(30))
                .thenReturn(Optional.empty());

        when(bidTransactionDAO.executeBid(
                anyInt(),
                anyInt(),
                any(),
                any()))
                .thenReturn(new BidExecutionResult(1L, 1));

        BidResult result = processor.process(ctx);

        assertEquals(
                session.getEndTime(),
                result.getCurrentEndTime()
        );
    }
}