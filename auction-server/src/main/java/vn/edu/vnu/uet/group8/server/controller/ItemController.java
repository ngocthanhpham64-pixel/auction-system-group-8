package vn.edu.vnu.uet.group8.server.controller;

import com.google.gson.JsonObject;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.request.GetAuctionsRequest;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.server.network.RequestParser;
import vn.edu.vnu.uet.group8.server.service.item.ItemQueryService;

/**
 * Controller cho domain Item: GET_ALL, GET_DETAIL.
 *
 * <p>Phần CREATE/UPDATE/DELETE item (ItemWriteService) chưa có action
 * trong ActionType enum — tạm thời chưa expose. Khi cần, mở rộng enum
 * + thêm handler ở đây.
 */
public class ItemController {
  private static final Logger log = LoggerFactory.getLogger(ItemController.class);

  private final ItemQueryService itemQueryService;

  public ItemController(ItemQueryService itemQueryService) {
    this.itemQueryService = itemQueryService;
  }

  /**
   * ITEM_GET_ALL — lấy danh sách item đang đấu giá, có thể có filter.
   *
   * <p>Nếu payload có GetAuctionsRequest với category/minPrice/maxPrice
   * → dùng searchItems(). Không có filter → dùng getActiveItems().
   */
  public ServerResponse handleGetAll(JsonObject request, String requestId) {
    try {
      GetAuctionsRequest filter =
          RequestParser.getPayload(request, GetAuctionsRequest.class);

      List<AuctionItemDTO> items;
      if (filter != null && hasFilter(filter)) {
        items = itemQueryService.searchItems(
            filter.getCategory(), filter.getMinPrice(), filter.getMaxPrice());
      } else {
        items = itemQueryService.getActiveItems();
      }

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
          ? request.getAsJsonObject("payload") : request;

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

  /** Có ít nhất 1 filter điều kiện thực sự. */
  private boolean hasFilter(GetAuctionsRequest f) {
    return f.getCategory() != null
        || f.getMinPrice() != null
        || f.getMaxPrice() != null;
  }
}
