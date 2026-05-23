package vn.edu.vnu.uet.group8.common.dto.response;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test cho {@link AuctionEndedBroadcastResponse} - 2 factory (sold/noBid).
 */
class AuctionEndedBroadcastResponseTest {

    @Test
    @DisplayName("sold() - phiên có người thắng")
    void taoSold() {
        AuctionEndedBroadcastResponse res = AuctionEndedBroadcastResponse.sold(
                10, "iPhone 17", new BigDecimal("25000000"), "alice");
        assertEquals(10, res.getItemId());
        assertEquals("iPhone 17", res.getItemTitle());
        assertEquals("SOLD", res.getFinalStatus());
        assertEquals(new BigDecimal("25000000"), res.getFinalPrice());
        assertEquals("alice", res.getWinnerUsername());
        assertTrue(res.hasSold());
    }

    @Test
    @DisplayName("noBid() - phiên không có người đấu giá")
    void taoNoBid() {
        AuctionEndedBroadcastResponse res = AuctionEndedBroadcastResponse.noBid(
                20, "Watch");
        assertEquals(20, res.getItemId());
        assertEquals("Watch", res.getItemTitle());
        assertEquals("ENDED_NO_BID", res.getFinalStatus());
        assertNull(res.getFinalPrice());
        assertNull(res.getWinnerUsername());
        assertFalse(res.hasSold());
    }

    @Test
    @DisplayName("getType() = AUCTION_ENDED")
    void getType() {
        AuctionEndedBroadcastResponse res = AuctionEndedBroadcastResponse.noBid(1, "X");
        assertEquals("AUCTION_ENDED", res.getType());
    }

    @Test
    @DisplayName("hasSold() phân biệt SOLD vs ENDED_NO_BID")
    void hasSoldPhanBiet() {
        AuctionEndedBroadcastResponse sold = AuctionEndedBroadcastResponse.sold(
                1, "X", BigDecimal.TEN, "winner");
        AuctionEndedBroadcastResponse noBid = AuctionEndedBroadcastResponse.noBid(1, "Y");
        assertTrue(sold.hasSold());
        assertFalse(noBid.hasSold());
    }
}