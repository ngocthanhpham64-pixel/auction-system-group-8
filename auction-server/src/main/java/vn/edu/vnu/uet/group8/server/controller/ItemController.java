package vn.edu.vnu.uet.group8.server.controller;

import java.math.BigDecimal;
import java.util.List;
// import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.model.CommentDTO;
import vn.edu.vnu.uet.group8.common.dto.request.GetAuctionsRequest;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;
import vn.edu.vnu.uet.group8.server.network.RequestParser;
import vn.edu.vnu.uet.group8.server.service.item.ItemQueryService;
import vn.edu.vnu.uet.group8.server.service.item.ItemWriteService;

/**
 * Controller cho domain Item: GET_ALL, GET_DETAIL.
 *
 * <p>
 * Phần CREATE/UPDATE/DELETE item (ItemWriteService) chưa có action
 * trong ActionType enum — tạm thời chưa expose. Khi cần, mở rộng enum
 * + thêm handler ở đây.
 */
public class ItemController {
  private static final Logger log = LoggerFactory.getLogger(ItemController.class);

  private final ItemQueryService itemQueryService;
  private final ItemWriteService itemWriteService;

  public ItemController(ItemQueryService itemQueryService, ItemWriteService itemWriteService) {
    this.itemQueryService = itemQueryService;
    this.itemWriteService = itemWriteService;
  }

  // ═══════════════════════════════════════════════════
  // Truy vấn
  // ═══════════════════════════════════════════════════
  /**
   * ITEM_GET_ALL — lấy danh sách item đang đấu giá, có thể có filter.
   *
   * <p>
   * Nếu payload có GetAuctionsRequest với category/minPrice/maxPrice
   * → dùng searchItems(). Không có filter → dùng getActiveItems().
   */
  public ServerResponse handleGetAll(JsonObject request, String requestId) {
    try {
      GetAuctionsRequest filter = RequestParser.getPayload(request, GetAuctionsRequest.class);

      List<AuctionItemDTO> items = itemQueryService.getAuctions(filter);

      log.debug("ITEM_GET_ALL trả về {} items", items.size());
      return ServerResponse.reply("ITEM_GET_ALL", requestId)
          .success(true)
          .message("Lấy danh sách thành công")
          .data(items)
          .build();

    } catch (Exception e) {
      log.error("Lỗi ITEM_GET_ALL", e);
      return ServerResponse.replyError("ITEM_GET_ALL", requestId,
          "Lỗi: " + e.getMessage());
    }
  }

  /**
   * ITEM_GET_DETAIL — lấy chi tiết 1 item.
   * Payload đơn giản: chỉ cần itemId.
   */
  public ServerResponse handleGetDetail(JsonObject request, String requestId) {
    try {
      // itemId có thể nằm trong payload hoặc trực tiếp ở root
      JsonObject payload = request.has("payload")
          && request.get("payload").isJsonObject()
              ? request.getAsJsonObject("payload")
              : request;

      int itemId = RequestParser.requireInt(payload, "itemId");
      AuctionItemDTO item = itemQueryService.getItemDetail(itemId);

      return ServerResponse.reply("ITEM_GET_DETAIL", requestId)
          .success(true)
          .message("Lấy chi tiết thành công")
          .data(item)
          .build();

    } catch (Exception e) {
      log.error("Lỗi ITEM_GET_DETAIL", e);
      return ServerResponse.replyError("ITEM_GET_DETAIL", requestId,
          "Lỗi: " + e.getMessage());
    }
  }

  public ServerResponse handleGetPurchaseHistory(JsonObject request, String requestId, int authenticatedUserId) {
    try {
      List<AuctionItemDTO> items = itemQueryService.getWonItems(authenticatedUserId);
      return ServerResponse.reply("USER_PURCHASE_HISTORY", requestId)
          .success(true)
          .message("Lấy lịch sử mua hàng thành công")
          .data(items)
          .build();
    } catch (Exception e) {
      log.error("Lỗi USER_PURCHASE_HISTORY", e);
      return ServerResponse.replyError("USER_PURCHASE_HISTORY", requestId, "Lỗi: " + e.getMessage());
    }
  }

  // ═══════════════════════════════════════════════════
  // Quản lý sản phẩm
  // ═══════════════════════════════════════════════════
  public ServerResponse handleCreateItem(
      JsonObject request, String requestId, int authenticatedUserId) {
    try {
      JsonObject payload = request.has("payload")
          && request.get("payload").isJsonObject()
              ? request.getAsJsonObject("payload")
              : request;

      String title = RequestParser.requireString(payload, "title");
      String description = RequestParser.requireString(payload, "description");

      ItemCategory category = ItemCategory.valueOf(
          RequestParser.requireString(payload, "category"));
      ItemCondition condition = ItemCondition.valueOf(
          RequestParser.requireString(payload, "condition"));
      // Map<String, String> specs = RequestParser.optionalMap(payload, "specs");

      List<String> imageUrls = null;
      if (payload.has("imageUrls") && payload.get("imageUrls").isJsonArray()) {
        imageUrls = GsonUtil.toList(payload.get("imageUrls"), String.class);
      }

      // Lấy thêm thông tin đấu giá (nếu có)
      BigDecimal startPrice = null;
      if (payload.has("startPrice") && !payload.get("startPrice").isJsonNull()) {
        startPrice = new BigDecimal(payload.get("startPrice").getAsString());
      }
      Integer durationHours = RequestParser.optionalInt(payload, "durationHours");
      
      Long startTime = null;
      if (payload.has("startTime") && !payload.get("startTime").isJsonNull()) {
        startTime = payload.get("startTime").getAsLong();
      }

      Item item = itemWriteService.createItem(
          authenticatedUserId, title, description, category, condition, imageUrls,
          startPrice, durationHours, startTime);

      return ServerResponse.reply("ITEM_CREATE", requestId)
          .success(true)
          .message("Tạo item thành công")
          .data(item)
          .build();

    } catch (Exception e) {
      log.error("Lỗi ITEM_CREATE", e);
      return ServerResponse.replyError("ITEM_CREATE", requestId,
          "Lỗi: " + e.getMessage());

    }
  }

  // ═══════════════════════════════════════════════════
  // Truy suất một số item
  // ═══════════════════════════════════════════════════
  /**
   * ITEM_MY_LISTINGS — Lấy danh sách sản phẩm mà user này đã đăng bán.
   * Gửi kèm targetUserId qua token (authenticatedUserId).
   */
  public ServerResponse handleGetMyListings(JsonObject request, String requestId, int authenticatedUserId) {
    try {
      JsonObject payload = request.has("payload")
          && request.get("payload").isJsonObject()
              ? request.getAsJsonObject("payload")
              : request;

      String statusStr = RequestParser.optionalString(payload, "status");
      List<AuctionItemDTO> items;
      if (statusStr != null && !statusStr.isBlank()) {
        ItemStatus status = ItemStatus.valueOf(statusStr.toUpperCase().trim());
        items = itemQueryService.getMyItemsByStatus(authenticatedUserId, status);
      } else {
        items = itemQueryService.getMyItems(authenticatedUserId);
      }

      log.debug("ITEM_MY_LISTINGS trả về {} items cho user {}", items.size(), authenticatedUserId);
      return ServerResponse.reply("ITEM_MY_LISTINGS", requestId)
          .success(true)
          .message("Lấy danh sách sản phẩm đã đăng thành công")
          .data(items)
          .build();

    } catch (Exception e) {
      log.error("Lỗi ITEM_MY_LISTINGS", e);
      return ServerResponse.replyError("ITEM_MY_LISTINGS", requestId,
          "Lỗi: " + e.getMessage());
    }
  }

  /**
   * ITEM_UPDATE — Cập nhật thông tin sản phẩm.
   * Chỉ cho phép cập nhật khi item đang ở trạng thái DRAFT (chưa đấu giá).
   */
  public ServerResponse handleUpdateItem(JsonObject request, String requestId, int authenticatedUserId) {
    try {
      JsonObject payload = request.has("payload")
          && request.get("payload").isJsonObject()
              ? request.getAsJsonObject("payload")
              : request;

      int itemId = RequestParser.requireInt(payload, "itemId");

      // Các trường tùy chọn (chỉ cập nhật những gì client gửi lên)
      String title = RequestParser.optionalString(payload, "title");
      String description = RequestParser.optionalString(payload, "description");

      ItemCondition condition = null;
      if (payload.has("condition")) {
        condition = ItemCondition.valueOf(RequestParser.requireString(payload, "condition"));
      }

      // Map<String, String> specs = null;
      // if (payload.has("specs")) {
      // specs = RequestParser.optionalMap(payload, "specs");
      // }

      List<String> imageUrls = null;
      if (payload.has("imageUrls") && payload.get("imageUrls").isJsonArray()) {
        imageUrls = vn.edu.vnu.uet.group8.common.util.GsonUtil.toList(payload.get("imageUrls"), String.class);
      }

      itemWriteService.updateItem(authenticatedUserId, itemId, title, description, condition, imageUrls);

      return ServerResponse.reply("ITEM_UPDATE", requestId)
          .success(true)
          .message("Cập nhật sản phẩm thành công")
          .build();

    } catch (Exception e) {
      log.error("Lỗi ITEM_UPDATE", e);
      return ServerResponse.replyError("ITEM_UPDATE", requestId,
          "Lỗi: " + e.getMessage());
    }
  }

  /**
   * ITEM_DELETE — Xóa (ẩn) sản phẩm.
   */
  public ServerResponse handleDeleteItem(JsonObject request, String requestId, int authenticatedUserId) {
    try {
      JsonObject payload = request.has("payload")
          && request.get("payload").isJsonObject()
              ? request.getAsJsonObject("payload")
              : request;

      int itemId = RequestParser.requireInt(payload, "itemId");

      itemWriteService.deleteItem(authenticatedUserId, itemId);

      return ServerResponse.reply("ITEM_DELETE", requestId)
          .success(true)
          .message("Xóa sản phẩm thành công")
          .build();

    } catch (Exception e) {
      log.error("Lỗi ITEM_DELETE", e);
      return ServerResponse.replyError("ITEM_DELETE", requestId,
          "Lỗi: " + e.getMessage());
    }
  }

  /**
   * Lấy danh sách comment của 1 vật phẩm
   */
  public ServerResponse handleItemComment(JsonObject request, String requestId) {
    try {
      JsonObject payload = request.has("payload")
          && request.get("payload").isJsonObject()
              ? request.getAsJsonObject("payload")
              : request;

      int itemId = RequestParser.requireInt(payload, "itemId");

      List<CommentDTO> comments = itemQueryService.getCommentsForAnItemId(itemId);

      return ServerResponse.reply("ITEM_COMMENT", requestId)
          .success(true)
          .message("Lấy danh sách comment thành công")
          .data(comments)
          .build();
    } catch (Exception e) {
      log.error("Lỗi ITEM_COMMENT", e);
      return ServerResponse.replyError("ITEM_COMMENT", requestId,
          "Lỗi: " + e.getMessage());
    }
  }
}
