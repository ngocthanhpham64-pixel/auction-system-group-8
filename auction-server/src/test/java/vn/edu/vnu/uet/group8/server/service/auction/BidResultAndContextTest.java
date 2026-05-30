package vn.edu.vnu.uet.group8.server.service.auction;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;

/**
 * Test cho 2 DTO server-side: {@link BidResult} và {@link BidContext}.
 */
class BidResultAndContextTest {

    @Nested
    @DisplayName("BidResult")
    class BidResultTest {

        @Test
        @DisplayName("Constructor đầy đủ field + getters")
        void taoVaTruyCap() {
            Instant end = Instant.now().plusSeconds(3600);
            BidResult r = new BidResult(
                    100L, 10, 20, 30, "alice",
                    new BigDecimal("500"), 5,
                    end, 99);
            assertEquals(100L, r.getTransactionId());
            assertEquals(10, r.getItemId());
            assertEquals(20, r.getAuctionSessionId());
            assertEquals(30, r.getBidderId());
            assertEquals("alice", r.getBidderUsername());
            assertEquals(new BigDecimal("500"), r.getNewPrice());
            assertEquals(5, r.getTotalBids());
            assertEquals(end, r.getCurrentEndTime());
            assertEquals(99, r.getPrevBidderId());
        }

        @Test
        @DisplayName("hasPreviousBidder() = true khi prevBidderId != null")
        void hasPreviousBidderTrue() {
            BidResult r = new BidResult(
                    1L, 1, 1, 1, "u",
                    BigDecimal.TEN, 1, Instant.now(), 42);
            assertTrue(r.hasPreviousBidder());
            assertEquals(42, r.getPrevBidderId());
        }

        @Test
        @DisplayName("hasPreviousBidder() = false khi prevBidderId == null (bid đầu)")
        void hasPreviousBidderFalse() {
            BidResult r = new BidResult(
                    1L, 1, 1, 1, "u",
                    BigDecimal.TEN, 1, Instant.now(), null);
            assertFalse(r.hasPreviousBidder());
            assertNull(r.getPrevBidderId());
        }
    }

    @Nested
    @DisplayName("BidContext")
    class BidContextTest {

        private UserMember taoBidder() {
            UserMember m = new UserMember.Builder("alice", "a@e.com", "hash").build();
            m.assignId(10);
            return m;
        }

        private Item taoItem() {
            return new Item.Builder(1, "iPhone", ItemCategory.ELECTRONICS)
                    .condition(ItemCondition.NEW)
                    .description("Test")
                    .build();
        }

        private AuctionSession taoSession() {
            return new AuctionSession.Builder(1, new BigDecimal("100"),
                    Instant.now(), Instant.now().plusSeconds(3600))
                    .build();
        }

        @Test
        @DisplayName("Constructor + getter trả đúng object đã set")
        void truyCap() {
            UserMember bidder = taoBidder();
            Item item = taoItem();
            AuctionSession session = taoSession();
            BigDecimal amount = new BigDecimal("150");

            BidContext ctx = new BidContext(bidder, item, session, amount, false);
            assertSame(bidder, ctx.getBidder());
            assertSame(item, ctx.getItem());
            assertSame(session, ctx.getAuctionSession());
            assertEquals(amount, ctx.getBidAmount());
            assertFalse(ctx.isTieBreaker());
        }
    }
}