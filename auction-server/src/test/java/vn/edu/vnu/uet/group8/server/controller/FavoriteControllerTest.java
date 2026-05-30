package vn.edu.vnu.uet.group8.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;
import vn.edu.vnu.uet.group8.server.service.item.FavoriteService;

@ExtendWith(MockitoExtension.class)
class FavoriteControllerTest {

  @Mock private FavoriteService favoriteService;

  private FavoriteController controller;

  @BeforeEach
  void setUp() {
    controller = new FavoriteController(favoriteService);
  }

  private AuctionItemDTO buildDto(int id) {
    return AuctionItemDTO.of(id, "Item " + id, "desc",
        ItemCategory.OTHER, ItemCondition.NEW, SessionStatus.ACTIVE,
        new BigDecimal("500000"),
        Instant.now().plus(1, ChronoUnit.HOURS),
        1, "seller", Collections.emptyMap(), Collections.emptyList(),
        0, Instant.now());
  }

  private JsonObject reqWithPayloadItemId(int itemId) {
    JsonObject req = new JsonObject();
    JsonObject payload = new JsonObject();
    payload.addProperty("itemId", itemId);
    req.add("payload", payload);
    return req;
  }

  private JsonObject reqRootItemId(int itemId) {
    JsonObject req = new JsonObject();
    req.addProperty("itemId", itemId);
    return req;
  }

  // ─────────────────────────────────────────────────────────────
  // handleGetList
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("handleGetList()")
  class GetListTest {

    @Test
    @DisplayName("Success - trả về danh sách yêu thích")
    void success() throws SQLException {
      List<AuctionItemDTO> items = List.of(buildDto(1), buildDto(2));
      when(favoriteService.getFavoriteItems(5)).thenReturn(items);

      ServerResponse resp = controller.handleGetList("req-1", 5);

      assertTrue(resp.isSuccess());
      assertEquals("FAVORITE_LIST", resp.getAction());
      verify(favoriteService).getFavoriteItems(5);
    }

    @Test
    @DisplayName("Success - list rỗng vẫn trả success")
    void successEmpty() throws SQLException {
      when(favoriteService.getFavoriteItems(anyInt()))
          .thenReturn(Collections.emptyList());

      ServerResponse resp = controller.handleGetList("req-empty", 1);

      assertTrue(resp.isSuccess());
    }

    @Test
    @DisplayName("Lỗi SQLException → error response")
    void sqlError() throws SQLException {
      when(favoriteService.getFavoriteItems(anyInt()))
          .thenThrow(new SQLException("DB down"));

      ServerResponse resp = controller.handleGetList("req-err", 1);

      assertFalse(resp.isSuccess());
      assertEquals("FAVORITE_LIST", resp.getAction());
    }

    @Test
    @DisplayName("Lỗi RuntimeException → error response")
    void runtimeError() throws SQLException {
      when(favoriteService.getFavoriteItems(anyInt()))
          .thenThrow(new RuntimeException("unexpected"));

      ServerResponse resp = controller.handleGetList("req-rt", 1);

      assertFalse(resp.isSuccess());
    }
  }

  // ─────────────────────────────────────────────────────────────
  // handleAdd
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("handleAdd()")
  class AddTest {

    @Test
    @DisplayName("Success với payload chứa itemId")
    void successVoiPayload() throws SQLException {
      ServerResponse resp = controller.handleAdd(reqWithPayloadItemId(10), "req-add", 3);

      assertTrue(resp.isSuccess());
      assertEquals("FAVORITE_ADD", resp.getAction());
      verify(favoriteService).addFavorite(3, 10);
    }

    @Test
    @DisplayName("Success khi itemId ở root (không có payload wrapper)")
    void successItemIdRoot() throws SQLException {
      ServerResponse resp = controller.handleAdd(reqRootItemId(20), "req-add-root", 2);

      assertTrue(resp.isSuccess());
      verify(favoriteService).addFavorite(2, 20);
    }

    @Test
    @DisplayName("Thiếu itemId → error response")
    void thieuItemId() throws SQLException {
      JsonObject req = new JsonObject();
      req.add("payload", new JsonObject());

      ServerResponse resp = controller.handleAdd(req, "req-no-id", 1);

      assertFalse(resp.isSuccess());
      assertEquals("FAVORITE_ADD", resp.getAction());
    }

    @Test
    @DisplayName("DAO lỗi → error response")
    void daoError() throws SQLException {
      doThrow(new SQLException("duplicate")).when(favoriteService).addFavorite(anyInt(), anyInt());

      ServerResponse resp = controller.handleAdd(reqWithPayloadItemId(5), "req-fail", 1);

      assertFalse(resp.isSuccess());
    }

    @Test
    @DisplayName("RuntimeException từ service → error response")
    void runtimeError() throws SQLException {
      doThrow(new RuntimeException("lỗi")).when(favoriteService).addFavorite(anyInt(), anyInt());

      ServerResponse resp = controller.handleAdd(reqWithPayloadItemId(5), "req-rt", 1);

      assertFalse(resp.isSuccess());
    }
  }

  // ─────────────────────────────────────────────────────────────
  // handleRemove
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("handleRemove()")
  class RemoveTest {

    @Test
    @DisplayName("Success với payload chứa itemId")
    void successVoiPayload() throws SQLException {
      ServerResponse resp = controller.handleRemove(reqWithPayloadItemId(15), "req-rem", 4);

      assertTrue(resp.isSuccess());
      assertEquals("FAVORITE_REMOVE", resp.getAction());
      verify(favoriteService).removeFavorite(4, 15);
    }

    @Test
    @DisplayName("Success khi itemId ở root")
    void successItemIdRoot() throws SQLException {
      ServerResponse resp = controller.handleRemove(reqRootItemId(25), "req-rem-root", 5);

      assertTrue(resp.isSuccess());
      verify(favoriteService).removeFavorite(5, 25);
    }

    @Test
    @DisplayName("Thiếu itemId → error response")
    void thieuItemId() {
      JsonObject req = new JsonObject();
      req.add("payload", new JsonObject());

      ServerResponse resp = controller.handleRemove(req, "req-no-id", 1);

      assertFalse(resp.isSuccess());
      assertEquals("FAVORITE_REMOVE", resp.getAction());
    }

    @Test
    @DisplayName("DAO lỗi → error response")
    void daoError() throws SQLException {
      doThrow(new SQLException("not found")).when(favoriteService).removeFavorite(anyInt(), anyInt());

      ServerResponse resp = controller.handleRemove(reqWithPayloadItemId(7), "req-fail-rem", 1);

      assertFalse(resp.isSuccess());
    }
  }
}