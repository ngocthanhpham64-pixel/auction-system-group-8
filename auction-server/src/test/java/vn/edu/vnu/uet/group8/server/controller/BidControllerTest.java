package vn.edu.vnu.uet.group8.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import vn.edu.vnu.uet.group8.server.service.auction.BidResult;

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

      when(auctionService.placeBid(eq(100), eq(10), eq(new BigDecimal("500"))))
          .thenReturn(new BidResult(1L, 10, 1, 100, "user100", new BigDecimal("500"), 1, Instant.now(), null));

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

  @Nested
  @DisplayName("handleAutoBid()")
  class AutoBidTest {
    @Test
    @DisplayName("Thiết lập Auto-bid thành công")
    void success() throws Exception {
      JsonObject payload = new JsonObject();
      payload.addProperty("itemId", 1);
      payload.addProperty("maxPrice", "2000");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      when(auctionService.placeAutoBid(anyInt(), anyInt(), any()))
          .thenReturn(new BidResult(1L, 1, 1, 100, "user100", new BigDecimal("1000"), 1, Instant.now(), null));

      ServerResponse res = controller.handleAutoBid(req, "req-auto", 100);

      assertTrue(res.isSuccess());
      assertEquals("Thiết lập giá tự động thành công", res.getMessage());
    }

    @Test
    @DisplayName("Hủy Auto-bid khi maxPrice <= 0")
    void cancelAutoBid() throws Exception {
      JsonObject payload = new JsonObject();
      payload.addProperty("itemId", 1);
      payload.addProperty("maxPrice", "0");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      ServerResponse res = controller.handleAutoBid(req, "req-cancel-auto", 100);

      assertTrue(res.isSuccess());
      assertEquals("Đã hủy tính năng Auto-bid", res.getMessage());
      verify(auctionService).cancelAutoBid(100, 1);
    }

    @Test
    @DisplayName("Auto-bid thất bại - Bị vượt giá bởi hệ thống")
    void outbidBySystem() throws Exception {
      JsonObject payload = new JsonObject();
      payload.addProperty("itemId", 1);
      payload.addProperty("maxPrice", "1500");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      // Trả về bidderId khác với authenticatedUserId
      when(auctionService.placeAutoBid(anyInt(), anyInt(), any()))
          .thenReturn(new BidResult(1L, 1, 1, 200, "user200", new BigDecimal("1600"), 1, Instant.now(), null));

      ServerResponse res = controller.handleAutoBid(req, "req-auto-fail", 100);

      assertFalse(res.isSuccess());
      assertTrue(res.getMessage().contains("hệ thống phòng thủ"));
    }
  }

  @Nested
  @DisplayName("handleGetAutoBidStatus()")
  class AutoBidStatusTest {
    @Test
    @DisplayName("Lấy trạng thái Auto-bid")
    void success() throws Exception {
      JsonObject req = new JsonObject();
      req.addProperty("itemId", 1);

      when(auctionService.hasActiveAutoBid(100, 1)).thenReturn(true);
      ServerResponse res = controller.handleGetAutoBidStatus(req, "req-status", 100);

      assertTrue(res.isSuccess());
      assertTrue(((JsonObject) res.getData()).get("isActive").getAsBoolean());
    }
  }
}