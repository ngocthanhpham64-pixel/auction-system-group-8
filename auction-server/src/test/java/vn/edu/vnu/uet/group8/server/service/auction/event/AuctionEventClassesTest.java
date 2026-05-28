package vn.edu.vnu.uet.group8.server.service.auction.event;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test cho 4 event class trong package {@code service.auction.event}:
 * {@link AuctionCancelledEvent}, {@link AuctionEndedEvent},
 * {@link AuctionOpenedEvent}, {@link BidPlacedEvent}.
 *
 * <p>Đều là immutable POJO — test constructor + getter.
 */
class AuctionEventClassesTest {

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("AuctionCancelledEvent")
    class CancelledTest {

        @Test
        @DisplayName("Constructor có prevBidder + refund → needsRefund() = true")
        void coRefund() {
            AuctionCancelledEvent e = new AuctionCancelledEvent(
                    10, 20, 99, new BigDecimal("500"));
            assertEquals(10, e.getItemId());
            assertEquals(20, e.getSessionId());
            assertEquals(99, e.getPrevBidderId());
            assertEquals(new BigDecimal("500"), e.getRefundAmount());
            assertTrue(e.needsRefund());
        }

        @Test
        @DisplayName("prevBidderId = null → needsRefund() = false")
        void khongRefund() {
            AuctionCancelledEvent e = new AuctionCancelledEvent(
                    10, 20, null, null);
            assertNull(e.getPrevBidderId());
            assertNull(e.getRefundAmount());
            assertFalse(e.needsRefund());
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("AuctionEndedEvent")
    class EndedTest {

        @Test
        @DisplayName("sold() factory - đầy đủ thông tin winner")
        void soldFactory() {
            AuctionEndedEvent e = AuctionEndedEvent.sold(
                    10, "iPhone 17", new BigDecimal("25000000"), 50, "alice", 99);
            assertEquals(10, e.getItemId());
            assertEquals("iPhone 17", e.getItemTitle());
            assertEquals(AuctionEndedEvent.Outcome.SOLD, e.getOutcome());
            assertEquals(new BigDecimal("25000000"), e.getFinalPrice());
            assertEquals(50, e.getWinnerId());
            assertEquals("alice", e.getWinnerUsername());
            assertEquals(99, e.getSellerId());
            assertTrue(e.hasSold());
        }

        @Test
        @DisplayName("noBid() factory - không có winner")
        void noBidFactory() {
            AuctionEndedEvent e = AuctionEndedEvent.noBid(10, "Watch", 99);
            assertEquals(10, e.getItemId());
            assertEquals("Watch", e.getItemTitle());
            assertEquals(AuctionEndedEvent.Outcome.NO_BID, e.getOutcome());
            assertNull(e.getFinalPrice());
            assertNull(e.getWinnerId());
            assertNull(e.getWinnerUsername());
            assertEquals(99, e.getSellerId());
            assertFalse(e.hasSold());
        }

        @Test
        @DisplayName("Outcome enum có 2 giá trị SOLD và NO_BID")
        void outcomeEnum() {
            assertEquals(2, AuctionEndedEvent.Outcome.values().length);
            assertNotNull(AuctionEndedEvent.Outcome.valueOf("SOLD"));
            assertNotNull(AuctionEndedEvent.Outcome.valueOf("NO_BID"));
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("AuctionOpenedEvent")
    class OpenedTest {

        @Test
        @DisplayName("Constructor + getter trả đúng giá trị")
        void taoVaTruyCap() {
            Instant endTime = Instant.now().plusSeconds(3600);
            AuctionOpenedEvent e = new AuctionOpenedEvent(
                    10, 20, new BigDecimal("100"), endTime);
            assertEquals(10, e.getItemId());
            assertEquals(20, e.getSessionId());
            assertEquals(new BigDecimal("100"), e.getStartingPrice());
            assertEquals(endTime, e.getEndTime());
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("BidPlacedEvent")
    class BidPlacedTest {

        @Test
        @DisplayName("Constructor có prevBidder → hasPrevBidder() = true")
        void hasPrevBidder() {
            Instant endTime = Instant.now().plusSeconds(3600);
            BidPlacedEvent e = new BidPlacedEvent(
                    10, 50, "alice",
                    new BigDecimal("500"), 5,
                    endTime, false, 99);

            assertEquals(10, e.getItemId());
            assertEquals(50, e.getBidderId());
            assertEquals("alice", e.getBidderUsername());
            assertEquals(new BigDecimal("500"), e.getNewPrice());
            assertEquals(5, e.getTotalBids());
            assertEquals(endTime, e.getNewEndTime());
            assertFalse(e.isExtended());
            assertEquals(99, e.getPrevBidderId());
            assertTrue(e.hasPrevBidder());
        }

        @Test
        @DisplayName("Bid đầu tiên (prevBidderId = null) → hasPrevBidder() = false")
        void khongCoPrevBidder() {
            BidPlacedEvent e = new BidPlacedEvent(
                    10, 50, "alice",
                    new BigDecimal("100"), 1,
                    Instant.now(), false, null);
            assertNull(e.getPrevBidderId());
            assertFalse(e.hasPrevBidder());
        }

        @Test
        @DisplayName("isExtended = true (anti-sniping kích hoạt)")
        void isExtendedTrue() {
            BidPlacedEvent e = new BidPlacedEvent(
                    10, 50, "alice",
                    new BigDecimal("500"), 5,
                    Instant.now(), true, 99);
            assertTrue(e.isExtended());
        }
    }
}