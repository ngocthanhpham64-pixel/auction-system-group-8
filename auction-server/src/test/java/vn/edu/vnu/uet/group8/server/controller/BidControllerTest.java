package vn.edu.vnu.uet.group8.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.gson.JsonObject;

import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.server.service.auction.AuctionService;

/**
 * Test cho {@link BidController}.
 */
@ExtendWith(MockitoExtension.class)
class BidControllerTest {

    @Mock private AuctionService auctionService;

    private BidController controller;

    @BeforeEach
    void setUp() {
        controller = new BidController(auctionService);
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("handlePlaceBid()")
    class PlaceBidTest {

        @Test
        @DisplayName("Place bid thành công")
        void placeBidThanhCong() throws SQLException {
            JsonObject payload = new JsonObject();
            payload.addProperty("itemId", 10);
            payload.addProperty("bidderId", 5);  // sẽ bị bỏ qua, dùng authenticatedUserId
            payload.addProperty("amount", "500");
            JsonObject req = new JsonObject();
            req.add("payload", payload);

            ServerResponse res = controller.handlePlaceBid(req, "req-1", 100);

            assertTrue(res.isSuccess());
            assertEquals("BID_PLACE", res.getAction());
            // Verify dùng authenticatedUserId = 100, KHÔNG dùng bidderId = 5 từ payload
            verify(auctionService).placeBid(eq(100), eq(10), eq(new BigDecimal("500")));
        }

        @Test
        @DisplayName("Place bid thiếu payload → error")
        void placeBidThieuPayload() {
            JsonObject req = new JsonObject();
            ServerResponse res = controller.handlePlaceBid(req, "req-1", 100);
            assertFalse(res.isSuccess());
            assertTrue(res.getMessage().contains("Thiếu payload"));
        }

        @Test
        @DisplayName("AuctionService ném SQLException → response error")
        void placeBidSqlError() throws SQLException {
            JsonObject payload = new JsonObject();
            payload.addProperty("itemId", 10);
            payload.addProperty("bidderId", 100);
            payload.addProperty("amount", "500");
            JsonObject req = new JsonObject();
            req.add("payload", payload);

            doThrow(new SQLException("DB lỗi")).when(auctionService)
                    .placeBid(anyInt(), anyInt(), any());

            ServerResponse res = controller.handlePlaceBid(req, "req-1", 100);

            assertFalse(res.isSuccess());
            assertTrue(res.getMessage().contains("Đặt giá thất bại"));
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("handleItemBidHistory()")
    class ItemBidHistoryTest {

        @Test
        @DisplayName("Có sessionId → trả về history")
        void coSessionId() throws SQLException {
            JsonObject payload = new JsonObject();
            payload.addProperty("itemId", 99);

            JsonObject req = new JsonObject();
            req.add("payload", payload);

            when(auctionService.getItemBidHistory(99))
                    .thenReturn(Collections.emptyList());

            ServerResponse res = controller.handleItemBidHistory(req, "req-1");

            assertTrue(res.isSuccess());
            assertEquals("BID_HISTORY", res.getAction());
            verify(auctionService).getItemBidHistory(99);
        }

        @Test
        @DisplayName("Thiếu sessionId → error")
        void thieuSessionId() {
            JsonObject req = new JsonObject();

            ServerResponse res = controller.handleItemBidHistory(req, "req-1");

            assertFalse(res.isSuccess());
            assertTrue(res.getMessage().contains("itemId"));
        }

        @Test
        @DisplayName("Service ném SQLException → error")
        void serviceLoi() throws SQLException {
            JsonObject payload = new JsonObject();
            payload.addProperty("itemId", 99);

            JsonObject req = new JsonObject();
            req.add("payload", payload);

            when(auctionService.getItemBidHistory(anyInt()))
                    .thenThrow(new SQLException("DB lỗi"));

            ServerResponse res = controller.handleItemBidHistory(req, "req-1");

            assertFalse(res.isSuccess());

        }

    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("handleUserBidHistory()")
    class UserBidHistoryTest {

        @Test
        @DisplayName("Trả về danh sách bid history của user")
        void thanhCong() throws SQLException {
            when(auctionService.getUserBidHistory(100))
                    .thenReturn(Collections.emptyList());

            JsonObject req = new JsonObject();
            ServerResponse res = controller.handleUserBidHistory(req, "req-1", 100);

            assertTrue(res.isSuccess());
            assertEquals("USER_BIDS", res.getAction());
            verify(auctionService).getUserBidHistory(100);
        }

        @Test
        @DisplayName("Service lỗi → error response")
        void serviceLoi() throws SQLException {
            when(auctionService.getUserBidHistory(anyInt()))
                    .thenThrow(new SQLException("DB lỗi"));

            JsonObject req = new JsonObject();
            ServerResponse res = controller.handleUserBidHistory(req, "req-1", 100);
            assertFalse(res.isSuccess());
        }
    }
}