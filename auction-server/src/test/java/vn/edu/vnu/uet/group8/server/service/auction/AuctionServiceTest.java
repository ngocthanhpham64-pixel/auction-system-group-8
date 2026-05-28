package vn.edu.vnu.uet.group8.server.service.auction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.common.dto.model.BidRecord;
import vn.edu.vnu.uet.group8.common.dto.model.UserBidHistoryDTO;
import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;
import vn.edu.vnu.uet.group8.common.exception.AuctionException;
import vn.edu.vnu.uet.group8.common.exception.ItemNotFoundException;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO;
import vn.edu.vnu.uet.group8.server.service.auction.AuctionEventBus;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock private AuctionSessionDAO sessionDAO;
    @Mock private BidValidator validator;
    @Mock private HybridBidExecutor executor;
    @Mock private AntiSnipingService antiSniping;
    @Mock private AuctionEventBus eventBus;
    @Mock private BidTransactionDAO bidTransactionDAO;
    @Mock private AutoBidService autoBidService;

    private AuctionService service;

    @BeforeEach
    void setUp() {
        service = new AuctionService(
                sessionDAO, validator, executor,
                antiSniping, eventBus, bidTransactionDAO, autoBidService);
    }

    // Helper: tạo AuctionSession đang ACTIVE
    private AuctionSession sessionActive(int sessionId, int itemId) {
        Instant now = Instant.now();
        AuctionSession s = new AuctionSession.Builder(
                itemId, new BigDecimal("1000000"),
                now.minus(1, ChronoUnit.HOURS),
                now.plus(2, ChronoUnit.HOURS)).build();
        s.assignId(sessionId);
        s.transitionStatus(SessionStatus.UPCOMING, SessionStatus.ACTIVE);
        return s;
    }

    private AuctionSession sessionUpcoming(int sessionId, int itemId) {
        Instant now = Instant.now();
        AuctionSession s = new AuctionSession.Builder(
                itemId, new BigDecimal("500000"),
                now.plus(1, ChronoUnit.HOURS),
                now.plus(3, ChronoUnit.HOURS)).build();
        s.assignId(sessionId);
        return s;
    }

    // ─────────────────────────────────────────────────────────────
    // placeBid
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("placeBid()")
    class PlaceBidTest {

        @Test
        @DisplayName("Success - đặt giá hợp lệ, không extend")
        void successKhongExtend() throws Exception {
            AuctionSession session = sessionActive(5, 10);
            when(sessionDAO.findActiveSessionByItemId(10)).thenReturn(Optional.of(session));

            BidContext ctx = mock(BidContext.class);
            when(ctx.getAuctionSession()).thenReturn(session);
            when(validator.validate(1, 5, new BigDecimal("2000000"), false)).thenReturn(ctx);

            BidResult bidResult = mock(BidResult.class);
            when(bidResult.getNewPrice()).thenReturn(new BigDecimal("2000000"));
            when(executor.execute(ctx, false)).thenReturn(bidResult);

            AntiSnipingService.AntiSnipingResult snipingResult = mock(AntiSnipingService.AntiSnipingResult.class);
            when(snipingResult.isExtended()).thenReturn(false);
            when(antiSniping.checkAndExtend(session)).thenReturn(snipingResult);

            assertDoesNotThrow(() ->
                    service.placeBid(1, 10, new BigDecimal("2000000")));

            verify(validator).validate(1, 5, new BigDecimal("2000000"), false);
            verify(executor).execute(ctx, false);
            verify(antiSniping).checkAndExtend(session);
            verify(eventBus).publish(any());
        }

        @Test
        @DisplayName("Success - đặt giá hợp lệ, có anti-sniping extend")
        void successCoExtend() throws Exception {
            AuctionSession session = sessionActive(5, 10);
            when(sessionDAO.findActiveSessionByItemId(10)).thenReturn(Optional.of(session));

            BidContext ctx = mock(BidContext.class);
            when(ctx.getAuctionSession()).thenReturn(session);
            when(validator.validate(anyInt(), anyInt(), any(), anyBoolean())).thenReturn(ctx);

            BidResult bidResult = mock(BidResult.class);
            when(bidResult.getNewPrice()).thenReturn(new BigDecimal("3000000"));
            when(executor.execute(ctx, false)).thenReturn(bidResult);

            AntiSnipingService.AntiSnipingResult snipingResult = mock(AntiSnipingService.AntiSnipingResult.class);
            when(snipingResult.isExtended()).thenReturn(true);
            when(antiSniping.checkAndExtend(any())).thenReturn(snipingResult);

            assertDoesNotThrow(() ->
                    service.placeBid(1, 10, new BigDecimal("3000000")));
        }

        @Test
        @DisplayName("Không có phiên ACTIVE cho item → AuctionException")
        void khongCoPhienActive() throws SQLException {
            when(sessionDAO.findActiveSessionByItemId(99)).thenReturn(Optional.empty());

            assertThrows(AuctionException.class,
                    () -> service.placeBid(1, 99, new BigDecimal("1000000")));
            verifyNoInteractions(validator, executor);
        }

        @Test
        @DisplayName("Validator ném InvalidBidException → ném lên, không process")
        void validatorNemException() throws Exception {
            AuctionSession session = sessionActive(5, 10);
            when(sessionDAO.findActiveSessionByItemId(10)).thenReturn(Optional.of(session));
            when(validator.validate(anyInt(), anyInt(), any(), anyBoolean()))
                    .thenThrow(new vn.edu.vnu.uet.group8.common.exception.InvalidBidException("Giá quá thấp"));

            assertThrows(vn.edu.vnu.uet.group8.common.exception.InvalidBidException.class,
                    () -> service.placeBid(1, 10, new BigDecimal("100")));
            verifyNoInteractions(executor);
        }

        @Test
        @DisplayName("Processor ném SQLException → lock luôn được release")
        void processorNemSQLException() throws Exception {
            AuctionSession session = sessionActive(5, 10);
            when(sessionDAO.findActiveSessionByItemId(10)).thenReturn(Optional.of(session));

            BidContext ctx = mock(BidContext.class);
            lenient().when(ctx.getAuctionSession()).thenReturn(session);
            when(validator.validate(anyInt(), anyInt(), any(), anyBoolean())).thenReturn(ctx);
            when(executor.execute(ctx, false)).thenThrow(new SQLException("DB lỗi"));

            assertThrows(SQLException.class,
                    () -> service.placeBid(1, 10, new BigDecimal("2000000")));
            // antiSniping không được gọi khi executor lỗi
            verifyNoInteractions(antiSniping);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // openSession
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("openSession()")
    class OpenSessionTest {

        @Test
        @DisplayName("Success - chuyển UPCOMING → ACTIVE")
        void successUpcomingToActive() throws SQLException {
            AuctionSession session = sessionUpcoming(3, 7);
            when(sessionDAO.findById(3)).thenReturn(Optional.of(session));

            service.openSession(3);

            assertEquals(SessionStatus.ACTIVE, session.getStatus());
            verify(sessionDAO).updateStatus(3, SessionStatus.ACTIVE);
            verify(eventBus).publish(any());
        }

        @Test
        @DisplayName("Phiên không tồn tại → ItemNotFoundException")
        void phienKhongTonTai() throws SQLException {
            when(sessionDAO.findById(99)).thenReturn(Optional.empty());

            assertThrows(ItemNotFoundException.class, () -> service.openSession(99));
            verify(sessionDAO, never()).updateStatus(anyInt(), any());
        }


        @Test
        @DisplayName("ACTIVE -> vẫn gọi update ACTIVE")
        void phienDaActive() throws SQLException {
            AuctionSession session = sessionActive(5, 10);

            when(sessionDAO.findById(5))
                    .thenReturn(Optional.of(session));

            assertDoesNotThrow(() -> service.openSession(5));

            verify(sessionDAO)
                    .updateStatus(5, SessionStatus.ACTIVE);
        }

        @Test
        @DisplayName("CANCELLED -> vẫn mở lại")
        void phienCancelled() throws SQLException {
            AuctionSession session = sessionUpcoming(8, 11);

            session.transitionStatus(
                    SessionStatus.UPCOMING,
                    SessionStatus.CANCELLED);

            when(sessionDAO.findById(8))
                    .thenReturn(Optional.of(session));

            assertDoesNotThrow(() -> service.openSession(8));

            verify(sessionDAO)
                    .updateStatus(8, SessionStatus.ACTIVE);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // cancelSession
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("cancelSession()")
    class CancelSessionTest {

        @Test
        @DisplayName("Success - hủy phiên UPCOMING (không cần hoàn tiền)")
        void cancelUpcoming() throws SQLException {
            AuctionSession session = sessionUpcoming(2, 5);
            when(sessionDAO.findById(2)).thenReturn(Optional.of(session));

            service.cancelSession(2, 99);

            assertEquals(SessionStatus.CANCELLED, session.getStatus());
            verify(sessionDAO).updateStatus(2, SessionStatus.CANCELLED);
            verify(bidTransactionDAO, never()).refundBidderExternal(anyInt(), any(), anyInt());
            verify(eventBus).publish(any());
        }

        @Test
        @DisplayName("Success - hủy phiên ACTIVE có bid → hoàn tiền cho bidder")
        void cancelActiveVoiBid() throws SQLException {
            AuctionSession session = sessionActive(4, 8);
            // Giả lập có highest bidder
            session = new AuctionSession.Reconstructor()
                    .id(4).createdAt(Instant.now()).isDeleted(false)
                    .itemId(8).startingPrice(new BigDecimal("1000000"))
                    .currentPrice(new BigDecimal("2000000"))
                    .startTime(Instant.now().minus(1, ChronoUnit.HOURS))
                    .endTime(Instant.now().plus(2, ChronoUnit.HOURS))
                    .status(SessionStatus.ACTIVE)
                    .bidCount(3)
                    .highestBidderId(7)
                    .build();
            when(sessionDAO.findById(4)).thenReturn(Optional.of(session));

            service.cancelSession(4, 99);

            verify(bidTransactionDAO).refundBidderExternal(7, new BigDecimal("2000000"), 4);
            verify(sessionDAO).updateStatus(4, SessionStatus.CANCELLED);
        }

        @Test
        @DisplayName("Success - hủy phiên ACTIVE không có bid (highestBidderId null)")
        void cancelActiveKhongCoBid() throws SQLException {
            AuctionSession session = sessionActive(6, 9);
            // highestBidderId = null (chưa ai đặt giá)
            when(sessionDAO.findById(6)).thenReturn(Optional.of(session));

            service.cancelSession(6, 99);

            verify(bidTransactionDAO, never()).refundBidderExternal(anyInt(), any(), anyInt());
            verify(sessionDAO).updateStatus(6, SessionStatus.CANCELLED);
        }


        @Test
        @DisplayName("Phiên SOLD vẫn có thể CANCELLED")
        void phienDaSold() throws SQLException {
            AuctionSession session = new AuctionSession.Reconstructor()
                    .id(7)
                    .createdAt(Instant.now())
                    .isDeleted(false)
                    .itemId(12)
                    .startingPrice(new BigDecimal("1000000"))
                    .currentPrice(new BigDecimal("2000000"))
                    .startTime(Instant.now().minus(1, ChronoUnit.HOURS))
                    .endTime(Instant.now().plus(1, ChronoUnit.HOURS))
                    .status(SessionStatus.SOLD)
                    .bidCount(3)
                    .highestBidderId(1)
                    .build();

            when(sessionDAO.findById(7))
                    .thenReturn(Optional.of(session));

            // arg1 = sessionId, arg2 = adminId
            service.cancelSession(7, 1);

            verify(sessionDAO)
                    .updateStatus(7, SessionStatus.CANCELLED);
        }

        @Test
        @DisplayName("Phiên không tồn tại → ItemNotFoundException")
        void phienKhongTonTai() throws SQLException {
            when(sessionDAO.findById(999)).thenReturn(Optional.empty());
            assertThrows(ItemNotFoundException.class, () -> service.cancelSession(999, 1));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // releaseSessionLock
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("releaseSessionLock()")
    class ReleaseLockTest {

        @Test
        @DisplayName("Release lock tồn tại - không ném exception")
        void releaseTonTai() {
            // Phải tạo lock trước bằng cách gọi computeIfAbsent
            // Gọi trực tiếp releaseSessionLock trên session chưa có lock vẫn OK
            assertDoesNotThrow(() -> service.releaseSessionLock(1));
        }

        @Test
        @DisplayName("Release lock không tồn tại - không ném exception")
        void releaseKhongTonTai() {
            assertDoesNotThrow(() -> service.releaseSessionLock(9999));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getItemBidHistory
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getItemBidHistory()")
    class GetItemBidHistoryTest {

        @Test
        @DisplayName("Có phiên ACTIVE → trả lịch sử đặt giá")
        void coPhienActive() throws Exception {
            AuctionSession session = sessionActive(5, 10);
            when(sessionDAO.findActiveSessionByItemId(10)).thenReturn(Optional.of(session));

            BidTransactionDAO.BidHistoryEntry entry =
                    new BidTransactionDAO.BidHistoryEntry(
                            1,
                            10,
                            5,
                            "user01",
                            new BigDecimal("1500000"),
                            "LEADER",
                            Instant.now());
            when(bidTransactionDAO.findHistoryByItem(5)).thenReturn(List.of(entry));

            List<BidRecord> result = service.getItemBidHistory(10);

            assertEquals(1, result.size());
            assertEquals(10, result.get(0).getItemId());
            assertEquals("user01", result.get(0).getDisplayName());
        }

        @Test
        @DisplayName("Không có phiên ACTIVE, có phiên UPCOMING → dùng upcoming")
        void coPhienUpcoming() throws Exception {
            when(sessionDAO.findActiveSessionByItemId(10)).thenReturn(Optional.empty());
            AuctionSession session = sessionUpcoming(3, 10);
            when(sessionDAO.findUpcomingByItemId(10)).thenReturn(Optional.of(session));
            when(bidTransactionDAO.findHistoryByItem(3)).thenReturn(Collections.emptyList());

            List<BidRecord> result = service.getItemBidHistory(10);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Không có phiên nào → list rỗng")
        void khongCoPhien() throws Exception {
            when(sessionDAO.findActiveSessionByItemId(10)).thenReturn(Optional.empty());
            when(sessionDAO.findUpcomingByItemId(10)).thenReturn(Optional.empty());

            List<BidRecord> result = service.getItemBidHistory(10);

            assertTrue(result.isEmpty());
            verifyNoInteractions(bidTransactionDAO);
        }

        @Test
        @DisplayName("Nhiều bid records → map đầy đủ")
        void nhieuBidRecords() throws Exception {
            AuctionSession session = sessionActive(5, 10);
            when(sessionDAO.findActiveSessionByItemId(10)).thenReturn(Optional.of(session));

            Instant now = Instant.now();
            List<BidTransactionDAO.BidHistoryEntry> entries = List.of(
                    new BidTransactionDAO.BidHistoryEntry(1, 10, 5, "user01", new BigDecimal("1500000"), "LEADER", now),
                    new BidTransactionDAO.BidHistoryEntry(2, 10, 6, "user02", new BigDecimal("2000000"), "LEADER", now),
                    new BidTransactionDAO.BidHistoryEntry(3, 10, 7, "user03", new BigDecimal("2500000"), "LEADER", now));
            when(bidTransactionDAO.findHistoryByItem(5)).thenReturn(entries);

            List<BidRecord> result = service.getItemBidHistory(10);

            assertEquals(3, result.size());
            assertEquals("user03", result.get(2).getDisplayName());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getUserBidHistory
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getUserBidHistory()")
    class GetUserBidHistoryTest {

        @Test
        @DisplayName("Có lịch sử → map sang UserBidHistoryDTO")
        void coLichSu() throws SQLException {
            Instant now = Instant.now();
            BidTransactionDAO.UserBidRecord rec = new BidTransactionDAO.UserBidRecord(
                    1, 5, 10, "iPhone 17", new BigDecimal("2000000"),
                    new BigDecimal("2500000"), now, now.plus(1, ChronoUnit.HOURS));
            when(bidTransactionDAO.findHistoryByUser(3)).thenReturn(List.of(rec));

            List<UserBidHistoryDTO> result = service.getUserBidHistory(3);

            assertEquals(1, result.size());
            assertEquals(1, result.get(0).getBidId());
            assertEquals("iPhone 17", result.get(0).getItemTitle());
            verify(bidTransactionDAO).findHistoryByUser(3);
        }

        @Test
        @DisplayName("Không có lịch sử → list rỗng")
        void khongCoLichSu() throws SQLException {
            when(bidTransactionDAO.findHistoryByUser(anyInt()))
                    .thenReturn(Collections.emptyList());

            assertTrue(service.getUserBidHistory(99).isEmpty());
        }

        @Test
        @DisplayName("SQLException từ DAO → ném lên")
        void sqlException() throws SQLException {
            when(bidTransactionDAO.findHistoryByUser(anyInt()))
                    .thenThrow(new SQLException("DB lỗi"));

            assertThrows(SQLException.class, () -> service.getUserBidHistory(1));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // placeAutoBid
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("placeAutoBid()")
    class PlaceAutoBidTest {

        @Test
        @DisplayName("Success - cấu hình auto bid, có bidResult → publish event")
        void successCoResult() throws Exception {
            AuctionSession session = sessionActive(5, 10);
            when(sessionDAO.findActiveSessionByItemId(10)).thenReturn(Optional.of(session));

            BidContext ctx = mock(BidContext.class);
            when(ctx.getAuctionSession()).thenReturn(session);
            when(validator.validate(1, 5, new BigDecimal("5000000"), false)).thenReturn(ctx);

            BidResult autoBidResult = mock(BidResult.class);
            when(autoBidResult.getNewPrice()).thenReturn(new BigDecimal("2000000"));
            when(executor.execute(ctx, true)).thenReturn(autoBidResult);

            AntiSnipingService.AntiSnipingResult snipingResult = mock(AntiSnipingService.AntiSnipingResult.class);
            when(snipingResult.isExtended()).thenReturn(false);
            when(antiSniping.checkAndExtend(session)).thenReturn(snipingResult);

            assertDoesNotThrow(() ->
                    service.placeAutoBid(1, 10, new BigDecimal("5000000")));

            verify(validator).validate(1, 5, new BigDecimal("5000000"), false);
            verify(executor).execute(ctx, true);
            verify(antiSniping).checkAndExtend(session);
        }

        @Test
        @DisplayName("Executor ném SQLException → không gọi antiSniping")
        void executorNemSQLException() throws Exception {
            AuctionSession session = sessionActive(5, 10);
            when(sessionDAO.findActiveSessionByItemId(10)).thenReturn(Optional.of(session));

            BidContext ctx = mock(BidContext.class);
            when(validator.validate(1, 5, new BigDecimal("5000000"), false)).thenReturn(ctx);
            when(executor.execute(ctx, true)).thenThrow(new SQLException("DB error"));

            assertThrows(SQLException.class, () ->
                    service.placeAutoBid(1, 10, new BigDecimal("5000000")));

            verifyNoInteractions(antiSniping);
        }

        @Test
        @DisplayName("Không có phiên ACTIVE → AuctionException")
        void khongCoPhien() throws SQLException {
            when(sessionDAO.findActiveSessionByItemId(99)).thenReturn(Optional.empty());

            assertThrows(AuctionException.class,
                    () -> service.placeAutoBid(1, 99, new BigDecimal("5000000")));
        }

        @Test
        @DisplayName("Phiên expired → AuctionException")
        void phienExpired() throws Exception {
            // Tạo session đã hết hạn
            Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
            AuctionSession session = new AuctionSession.Reconstructor()
                    .id(5).createdAt(Instant.now()).isDeleted(false)
                    .itemId(10).startingPrice(new BigDecimal("1000000"))
                    .currentPrice(new BigDecimal("1000000"))
                    .startTime(past.minus(1, ChronoUnit.HOURS))
                    .endTime(past)
                    .status(SessionStatus.ACTIVE)
                    .bidCount(0).highestBidderId(null).build();
            when(sessionDAO.findActiveSessionByItemId(10)).thenReturn(Optional.of(session));
            when(validator.validate(1, 5, new BigDecimal("5000000"), false))
                    .thenThrow(new AuctionException("Phiên đấu giá đã kết thúc"));

            assertThrows(AuctionException.class,
                    () -> service.placeAutoBid(1, 10, new BigDecimal("5000000")));
        }
    }
}