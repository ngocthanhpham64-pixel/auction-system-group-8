package vn.edu.vnu.uet.group8.server.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.request.GetAuctionsRequest;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;
import vn.edu.vnu.uet.group8.common.exception.ItemNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.UnauthorizedException;
import vn.edu.vnu.uet.group8.server.service.item.ItemQueryService;
import vn.edu.vnu.uet.group8.server.service.item.ItemWriteService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử toàn diện ItemController — mục tiêu: 100% branch coverage.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ItemController — Full Branch Coverage")
class ItemControllerTest {

  @Mock
  private ItemQueryService itemQueryService;

  @Mock
  private ItemWriteService itemWriteService;

  @InjectMocks
  private ItemController itemController;

  private static final String REQUEST_ID = "req-001";
  private static final int USER_ID = 42;

  // ════════════════════════════════════════════════════════════════════════
  // HELPERS
  // ════════════════════════════════════════════════════════════════════════

  private static JsonObject rootRequest(String... kvPairs) {
    JsonObject obj = new JsonObject();
    for (int i = 0; i < kvPairs.length - 1; i += 2) {
      obj.addProperty(kvPairs[i], kvPairs[i + 1]);
    }
    return obj;
  }

  private static JsonObject wrappedRequest(JsonObject payload) {
    JsonObject obj = new JsonObject();
    obj.add("payload", payload);
    return obj;
  }

  private static JsonObject payloadWithItemId(int itemId) {
    JsonObject p = new JsonObject();
    p.addProperty("itemId", itemId);
    return p;
  }

  private static AuctionItemDTO stubItemDTO() {
    return mock(AuctionItemDTO.class);
  }

  // ════════════════════════════════════════════════════════════════════════
  // 1. handleGetAll
  // ════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("handleGetAll")
  class HandleGetAllTests {

    @Test
    @DisplayName("Không có payload → filter null → trả list thành công")
    void noPayload_returnsItems() throws Exception {
      JsonObject req = new JsonObject();
      List<AuctionItemDTO> items = List.of(stubItemDTO(), stubItemDTO());
      when(itemQueryService.getAuctions(null)).thenReturn(items);

      ServerResponse res = itemController.handleGetAll(req, REQUEST_ID);

      assertTrue(res.isSuccess());
      assertSame(items, res.getData());
      verify(itemQueryService).getAuctions(null);
    }

    @Test
    @DisplayName("Có payload filter → truyền đúng filter vào service")
    void withFilter_callsGetAuctions() throws Exception {
      JsonObject payload = new JsonObject();
      payload.addProperty("category", "ELECTRONICS");
      payload.addProperty("minPrice", "100");
      payload.addProperty("maxPrice", "500");
      payload.addProperty("sortBy", "PRICE_ASC");
      JsonObject req = wrappedRequest(payload);

      List<AuctionItemDTO> items = List.of(stubItemDTO());
      when(itemQueryService.getAuctions(any())).thenReturn(items);

      ServerResponse res = itemController.handleGetAll(req, REQUEST_ID);

      assertTrue(res.isSuccess());
      verify(itemQueryService).getAuctions(any(GetAuctionsRequest.class));
    }

    @Test
    @DisplayName("Service ném exception → trả về response lỗi")
    void serviceThrows_returnsErrorResponse() throws Exception {
      JsonObject req = new JsonObject();
      when(itemQueryService.getAuctions(any()))
          .thenThrow(new SQLException("DB down"));

      ServerResponse res = itemController.handleGetAll(req, REQUEST_ID);

      assertFalse(res.isSuccess());
      assertTrue(res.getMessage().contains("DB down"));
    }
  }

  // ════════════════════════════════════════════════════════════════════════
  // 2. handleGetDetail
  // ════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("handleGetDetail")
  class HandleGetDetailTests {

    @Test
    @DisplayName("Payload là JsonObject → đọc itemId từ payload")
    void payloadIsJsonObject_readsItemIdFromPayload() throws Exception {
      JsonObject payload = payloadWithItemId(10);
      JsonObject req = wrappedRequest(payload);
      AuctionItemDTO dto = stubItemDTO();
      when(itemQueryService.getItemDetail(10)).thenReturn(dto);

      ServerResponse res = itemController.handleGetDetail(req, REQUEST_ID);

      assertTrue(res.isSuccess());
      assertSame(dto, res.getData());
      verify(itemQueryService).getItemDetail(10);
    }

    @Test
    @DisplayName("Không có payload → đọc itemId từ root request")
    void noPayload_readsItemIdFromRoot() throws Exception {
      JsonObject req = rootRequest("itemId", "7");
      AuctionItemDTO dto = stubItemDTO();
      when(itemQueryService.getItemDetail(7)).thenReturn(dto);

      ServerResponse res = itemController.handleGetDetail(req, REQUEST_ID);

      assertTrue(res.isSuccess());
      verify(itemQueryService).getItemDetail(7);
    }

    @Test
    @DisplayName("Payload tồn tại nhưng KHÔNG phải JsonObject → fallback root")
    void payloadNotJsonObject_fallbackRoot() throws Exception {
      JsonObject req = new JsonObject();
      req.add("payload", new JsonPrimitive("not-an-object"));
      req.addProperty("itemId", 99);

      AuctionItemDTO dto = stubItemDTO();
      when(itemQueryService.getItemDetail(99)).thenReturn(dto);

      ServerResponse res = itemController.handleGetDetail(req, REQUEST_ID);

      assertTrue(res.isSuccess());
      verify(itemQueryService).getItemDetail(99);
    }

    @Test
    @DisplayName("Item không tồn tại → ItemNotFoundException → response lỗi")
    void itemNotFound_returnsErrorResponse() throws Exception {
      JsonObject req = wrappedRequest(payloadWithItemId(999));
      when(itemQueryService.getItemDetail(999))
          .thenThrow(new ItemNotFoundException(999));

      ServerResponse res = itemController.handleGetDetail(req, REQUEST_ID);

      assertFalse(res.isSuccess());
    }

    @Test
    @DisplayName("Service ném SQLException → response lỗi")
    void sqlException_returnsErrorResponse() throws Exception {
      JsonObject req = wrappedRequest(payloadWithItemId(5));
      when(itemQueryService.getItemDetail(5))
          .thenThrow(new SQLException("connection timeout"));

      ServerResponse res = itemController.handleGetDetail(req, REQUEST_ID);

      assertFalse(res.isSuccess());
      assertTrue(res.getMessage().contains("connection timeout"));
    }
  }

  // ════════════════════════════════════════════════════════════════════════
  // 3. handleGetPurchaseHistory
  // ════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("handleGetPurchaseHistory")
  class HandleGetPurchaseHistoryTests {

    @Test
    @DisplayName("Thành công → trả list item đã mua")
    void success_returnsWonItems() throws Exception {
      List<AuctionItemDTO> wonItems = List.of(stubItemDTO());
      when(itemQueryService.getWonItems(USER_ID)).thenReturn(wonItems);

      ServerResponse res = itemController.handleGetPurchaseHistory(
          new JsonObject(), REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
      assertSame(wonItems, res.getData());
      assertEquals("USER_PURCHASE_HISTORY", res.getAction());
    }

    @Test
    @DisplayName("Service ném exception → response lỗi")
    void serviceThrows_returnsErrorResponse() throws Exception {
      when(itemQueryService.getWonItems(USER_ID))
          .thenThrow(new RuntimeException("unexpected error"));

      ServerResponse res = itemController.handleGetPurchaseHistory(
          new JsonObject(), REQUEST_ID, USER_ID);

      assertFalse(res.isSuccess());
      assertTrue(res.getMessage().contains("unexpected error"));
    }
  }

  // ════════════════════════════════════════════════════════════════════════
  // 4. handleCreateItem
  // ════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("handleCreateItem")
  class HandleCreateItemTests {

    private JsonObject fullPayload() {
      JsonObject p = new JsonObject();
      p.addProperty("title", "Laptop cũ");
      p.addProperty("description", "Máy ngon");
      p.addProperty("category", "ELECTRONICS");
      p.addProperty("condition", "USED");
      return p;
    }

    @Test
    @DisplayName("Payload là JsonObject, không có imageUrls, không có startPrice → success")
    void noImageUrls_noStartPrice_success() throws Exception {
      JsonObject payload = fullPayload();
      JsonObject req = wrappedRequest(payload);

      Item mockItem = mock(Item.class);
      when(itemWriteService.createItem(
          eq(USER_ID), eq("Laptop cũ"), eq("Máy ngon"),
          eq(ItemCategory.ELECTRONICS), eq(ItemCondition.USED),
          isNull(), isNull(), isNull(), isNull()))
          .thenReturn(mockItem);

      ServerResponse res = itemController.handleCreateItem(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
      assertSame(mockItem, res.getData());
      assertEquals("ITEM_CREATE", res.getAction());
    }

    @Test
    @DisplayName("Có imageUrls là JsonArray → parse thành List<String>")
    void withImageUrlsArray_parsedCorrectly() throws Exception {
      JsonObject payload = fullPayload();
      JsonArray arr = new JsonArray();
      arr.add("http://img1.jpg");
      arr.add("http://img2.jpg");
      payload.add("imageUrls", arr);
      JsonObject req = wrappedRequest(payload);

      Item mockItem = mock(Item.class);
      when(itemWriteService.createItem(
          eq(USER_ID), anyString(), anyString(),
          any(), any(),
          argThat(list -> list != null && list.size() == 2),
          isNull(), isNull(), isNull()))
          .thenReturn(mockItem);

      ServerResponse res = itemController.handleCreateItem(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
    }

    @Test
    @DisplayName("imageUrls hiện diện nhưng KHÔNG phải JsonArray → imageUrls null")
    void imageUrlsNotArray_imageUrlsNull() throws Exception {
      JsonObject payload = fullPayload();
      payload.addProperty("imageUrls", "single-url");
      JsonObject req = wrappedRequest(payload);

      Item mockItem = mock(Item.class);
      when(itemWriteService.createItem(
          eq(USER_ID), anyString(), anyString(),
          any(), any(),
          isNull(), isNull(), isNull(), isNull()))
          .thenReturn(mockItem);

      ServerResponse res = itemController.handleCreateItem(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
    }

    @Test
    @DisplayName("Có startPrice hợp lệ → parse thành BigDecimal")
    void withStartPrice_parsedCorrectly() throws Exception {
      JsonObject payload = fullPayload();
      payload.addProperty("startPrice", "1500000");
      payload.addProperty("durationHours", 24);
      JsonObject req = wrappedRequest(payload);

      Item mockItem = mock(Item.class);
      when(itemWriteService.createItem(
          eq(USER_ID), anyString(), anyString(),
          any(), any(), isNull(),
          eq(new BigDecimal("1500000")),
          eq(24), isNull()))
          .thenReturn(mockItem);

      ServerResponse res = itemController.handleCreateItem(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
    }

    @Test
    @DisplayName("startPrice là JsonNull → startPrice null")
    void startPriceIsJsonNull_startPriceNull() throws Exception {
      JsonObject payload = fullPayload();
      payload.add("startPrice", JsonNull.INSTANCE);
      JsonObject req = wrappedRequest(payload);

      Item mockItem = mock(Item.class);
      when(itemWriteService.createItem(
          eq(USER_ID), anyString(), anyString(),
          any(), any(), isNull(), isNull(), isNull(), isNull()))
          .thenReturn(mockItem);

      ServerResponse res = itemController.handleCreateItem(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
    }

    @Test
    @DisplayName("Không có payload key → đọc từ root request")
    void noPayloadKey_readsFromRoot() throws Exception {
      JsonObject req = new JsonObject();
      req.addProperty("title", "Điện thoại");
      req.addProperty("description", "Máy mới");
      req.addProperty("category", "ELECTRONICS");
      req.addProperty("condition", "NEW");

      Item mockItem = mock(Item.class);
      when(itemWriteService.createItem(
          eq(USER_ID), eq("Điện thoại"), eq("Máy mới"),
          eq(ItemCategory.ELECTRONICS), eq(ItemCondition.NEW),
          isNull(), isNull(), isNull(), isNull()))
          .thenReturn(mockItem);

      ServerResponse res = itemController.handleCreateItem(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
    }

    @Test
    @DisplayName("Service ném UnauthorizedException → response lỗi")
    void serviceThrowsUnauthorized_returnsErrorResponse() throws Exception {
      JsonObject req = wrappedRequest(fullPayload());
      when(itemWriteService.createItem(
          anyInt(),
          anyString(),
          anyString(),
          any(ItemCategory.class),
          any(ItemCondition.class),
          nullable(List.class),
          nullable(BigDecimal.class),
          nullable(Integer.class),
          nullable(Long.class)
      )).thenThrow(new UnauthorizedException("tài khoản bị khóa"));
      ServerResponse res = itemController.handleCreateItem(req, REQUEST_ID, USER_ID);

      assertFalse(res.isSuccess());
      assertTrue(res.getMessage().contains("tài khoản bị khóa"));
    }

    @Test
    @DisplayName("Service ném RuntimeException → response lỗi")
    void serviceThrowsRuntime_returnsErrorResponse() throws Exception {
      JsonObject req = wrappedRequest(fullPayload());
      when(itemWriteService.createItem(
          anyInt(),
          anyString(),
          anyString(),
          any(ItemCategory.class),
          any(ItemCondition.class),
          nullable(List.class),
          nullable(BigDecimal.class),
          nullable(Integer.class),
          nullable(Long.class)
      )).thenThrow(new RuntimeException("lỗi không xác định"));

      ServerResponse res = itemController.handleCreateItem(req, REQUEST_ID, USER_ID);

      assertFalse(res.isSuccess());
    }
  }

  // ════════════════════════════════════════════════════════════════════════
  // 5. handleGetMyListings
  // ════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("handleGetMyListings")
  class HandleGetMyListingsTests {

    @Test
    @DisplayName("statusStr có giá trị → gọi getMyItemsByStatus")
    void withStatus_callsGetByStatus() throws Exception {
      JsonObject payload = new JsonObject();
      payload.addProperty("status", "DRAFT");
      JsonObject req = wrappedRequest(payload);

      List<AuctionItemDTO> items = List.of(stubItemDTO());
      when(itemQueryService.getMyItemsByStatus(USER_ID, ItemStatus.DRAFT))
          .thenReturn(items);

      ServerResponse res = itemController.handleGetMyListings(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
      assertSame(items, res.getData());
      verify(itemQueryService).getMyItemsByStatus(USER_ID, ItemStatus.DRAFT);
      verify(itemQueryService, never()).getMyItems(anyInt());
    }

    @Test
    @DisplayName("statusStr null → gọi getMyItems")
    void statusNull_callsGetMyItems() throws Exception {
      JsonObject payload = new JsonObject();
      JsonObject req = wrappedRequest(payload);

      List<AuctionItemDTO> items = List.of(stubItemDTO(), stubItemDTO());
      when(itemQueryService.getMyItems(USER_ID)).thenReturn(items);

      ServerResponse res = itemController.handleGetMyListings(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
      verify(itemQueryService).getMyItems(USER_ID);
      verify(itemQueryService, never()).getMyItemsByStatus(anyInt(), any());
    }

    @Test
    @DisplayName("statusStr là chuỗi rỗng → gọi getMyItems")
    void statusBlank_callsGetMyItems() throws Exception {
      JsonObject payload = new JsonObject();
      payload.addProperty("status", "   ");
      JsonObject req = wrappedRequest(payload);

      List<AuctionItemDTO> items = List.of();
      when(itemQueryService.getMyItems(USER_ID)).thenReturn(items);

      ServerResponse res = itemController.handleGetMyListings(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
      verify(itemQueryService).getMyItems(USER_ID);
    }

    @Test
    @DisplayName("Không có payload key → đọc từ root request")
    void noPayloadKey_readsFromRoot() throws Exception {
      JsonObject req = new JsonObject();
      List<AuctionItemDTO> items = List.of();
      when(itemQueryService.getMyItems(USER_ID)).thenReturn(items);

      ServerResponse res = itemController.handleGetMyListings(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
      verify(itemQueryService).getMyItems(USER_ID);
    }

    @Test
    @DisplayName("Status uppercase conversion: 'listed' → ItemStatus.LISTED")
    void statusLowercase_convertedToUppercase() throws Exception {
      JsonObject payload = new JsonObject();
      payload.addProperty("status", "listed");
      JsonObject req = wrappedRequest(payload);

      List<AuctionItemDTO> items = List.of();
      when(itemQueryService.getMyItemsByStatus(USER_ID, ItemStatus.LISTED))
          .thenReturn(items);

      ServerResponse res = itemController.handleGetMyListings(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
      verify(itemQueryService).getMyItemsByStatus(USER_ID, ItemStatus.LISTED);
    }

    @Test
    @DisplayName("Status không hợp lệ → IllegalArgumentException → response lỗi")
    void invalidStatus_returnsErrorResponse() throws Exception {
      JsonObject payload = new JsonObject();
      payload.addProperty("status", "INVALID_STATUS");
      JsonObject req = wrappedRequest(payload);

      ServerResponse res = itemController.handleGetMyListings(req, REQUEST_ID, USER_ID);

      assertFalse(res.isSuccess());
    }

    @Test
    @DisplayName("Service ném SQLException → response lỗi")
    void serviceThrows_returnsErrorResponse() throws Exception {
      JsonObject req = wrappedRequest(new JsonObject());
      when(itemQueryService.getMyItems(USER_ID))
          .thenThrow(new SQLException("db error"));

      ServerResponse res = itemController.handleGetMyListings(req, REQUEST_ID, USER_ID);

      assertFalse(res.isSuccess());
      assertTrue(res.getMessage().contains("db error"));
    }
  }

  // ════════════════════════════════════════════════════════════════════════
  // 6. handleUpdateItem
  // ════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("handleUpdateItem")
  class HandleUpdateItemTests {

    @Test
    @DisplayName("Chỉ có itemId, không có title/description/condition/imageUrls → cập nhật với null")
    void minimalPayload_allOptionalNull() throws Exception {
      JsonObject payload = payloadWithItemId(5);
      JsonObject req = wrappedRequest(payload);

      doNothing().when(itemWriteService)
          .updateItem(USER_ID, 5, null, null, null, null);

      ServerResponse res = itemController.handleUpdateItem(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
      verify(itemWriteService).updateItem(USER_ID, 5, null, null, null, null);
    }

    @Test
    @DisplayName("Có condition → parse sang ItemCondition")
    void withCondition_conditionParsed() throws Exception {
      JsonObject payload = payloadWithItemId(5);
      payload.addProperty("condition", "NEW");
      JsonObject req = wrappedRequest(payload);

      doNothing().when(itemWriteService)
          .updateItem(eq(USER_ID), eq(5), isNull(), isNull(),
              eq(ItemCondition.NEW), isNull());

      ServerResponse res = itemController.handleUpdateItem(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
      verify(itemWriteService).updateItem(USER_ID, 5, null, null, ItemCondition.NEW, null);
    }

    @Test
    @DisplayName("Không có condition → condition null")
    void noCondition_conditionNull() throws Exception {
      JsonObject payload = payloadWithItemId(5);
      payload.addProperty("title", "New Title");
      JsonObject req = wrappedRequest(payload);

      doNothing().when(itemWriteService)
          .updateItem(eq(USER_ID), eq(5), eq("New Title"), isNull(), isNull(), isNull());

      ServerResponse res = itemController.handleUpdateItem(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
    }

    @Test
    @DisplayName("Có imageUrls là JsonArray → parse thành List")
    void withImageUrlsArray_parsedToList() throws Exception {
      JsonObject payload = payloadWithItemId(5);
      JsonArray arr = new JsonArray();
      arr.add("url1.jpg");
      arr.add("url2.jpg");
      payload.add("imageUrls", arr);
      JsonObject req = wrappedRequest(payload);

      doNothing().when(itemWriteService)
          .updateItem(eq(USER_ID), eq(5), isNull(), isNull(), isNull(),
              argThat(list -> list != null && list.size() == 2));

      ServerResponse res = itemController.handleUpdateItem(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
    }

    @Test
    @DisplayName("imageUrls không phải JsonArray → imageUrls null")
    void imageUrlsNotArray_remainsNull() throws Exception {
      JsonObject payload = payloadWithItemId(5);
      payload.addProperty("imageUrls", "http://only-one.jpg");
      JsonObject req = wrappedRequest(payload);

      doNothing().when(itemWriteService)
          .updateItem(eq(USER_ID), eq(5), isNull(), isNull(), isNull(), isNull());

      ServerResponse res = itemController.handleUpdateItem(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
    }

    @Test
    @DisplayName("Không có payload key → đọc từ root request")
    void noPayloadKey_readsFromRoot() throws Exception {
      JsonObject req = new JsonObject();
      req.addProperty("itemId", 5);
      req.addProperty("title", "Updated Title");

      doNothing().when(itemWriteService)
          .updateItem(eq(USER_ID), eq(5), eq("Updated Title"),
              isNull(), isNull(), isNull());

      ServerResponse res = itemController.handleUpdateItem(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
    }

    @Test
    @DisplayName("Service ném UnauthorizedException → response lỗi")
    void serviceThrowsUnauthorized_returnsErrorResponse() throws Exception {
      JsonObject req = wrappedRequest(payloadWithItemId(5));
      doThrow(new UnauthorizedException("không có quyền"))
          .when(itemWriteService)
          .updateItem(anyInt(), anyInt(), any(), any(), any(), any());

      ServerResponse res = itemController.handleUpdateItem(req, REQUEST_ID, USER_ID);

      assertFalse(res.isSuccess());
      assertTrue(res.getMessage().contains("không có quyền"));
    }

    @Test
    @DisplayName("Service ném RuntimeException → response lỗi")
    void serviceThrowsRuntime_returnsErrorResponse() throws Exception {
      JsonObject req = wrappedRequest(payloadWithItemId(5));
      doThrow(new RuntimeException("lỗi hệ thống"))
          .when(itemWriteService)
          .updateItem(anyInt(), anyInt(), any(), any(), any(), any());

      ServerResponse res = itemController.handleUpdateItem(req, REQUEST_ID, USER_ID);

      assertFalse(res.isSuccess());
    }
  }

  // ════════════════════════════════════════════════════════════════════════
  // 7. handleDeleteItem
  // ════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("handleDeleteItem")
  class HandleDeleteItemTests {

    @Test
    @DisplayName("Payload là JsonObject → đọc itemId từ payload")
    void payloadIsJsonObject_success() throws Exception {
      JsonObject req = wrappedRequest(payloadWithItemId(10));
      doNothing().when(itemWriteService).deleteItem(USER_ID, 10);

      ServerResponse res = itemController.handleDeleteItem(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
      assertEquals("ITEM_DELETE", res.getAction());
      verify(itemWriteService).deleteItem(USER_ID, 10);
    }

    @Test
    @DisplayName("Không có payload key → đọc itemId từ root")
    void noPayloadKey_readsFromRoot() throws Exception {
      JsonObject req = rootRequest("itemId", "10");
      doNothing().when(itemWriteService).deleteItem(USER_ID, 10);

      ServerResponse res = itemController.handleDeleteItem(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
      verify(itemWriteService).deleteItem(USER_ID, 10);
    }

    @Test
    @DisplayName("payload tồn tại nhưng không phải JsonObject → fallback root")
    void payloadNotJsonObject_fallbackRoot() throws Exception {
      JsonObject req = new JsonObject();
      req.add("payload", new JsonPrimitive("not-an-object"));
      req.addProperty("itemId", 15);
      doNothing().when(itemWriteService).deleteItem(USER_ID, 15);

      ServerResponse res = itemController.handleDeleteItem(req, REQUEST_ID, USER_ID);

      assertTrue(res.isSuccess());
      verify(itemWriteService).deleteItem(USER_ID, 15);
    }

    @Test
    @DisplayName("Item không tồn tại → ItemNotFoundException → response lỗi")
    void itemNotFound_returnsErrorResponse() throws Exception {
      JsonObject req = wrappedRequest(payloadWithItemId(999));
      doThrow(new ItemNotFoundException(999))
          .when(itemWriteService).deleteItem(USER_ID, 999);

      ServerResponse res = itemController.handleDeleteItem(req, REQUEST_ID, USER_ID);

      assertFalse(res.isSuccess());
    }

    @Test
    @DisplayName("Không có quyền xóa → UnauthorizedException → response lỗi")
    void unauthorized_returnsErrorResponse() throws Exception {
      JsonObject req = wrappedRequest(payloadWithItemId(5));
      doThrow(new UnauthorizedException("xóa item của người khác"))
          .when(itemWriteService).deleteItem(USER_ID, 5);

      ServerResponse res = itemController.handleDeleteItem(req, REQUEST_ID, USER_ID);

      assertFalse(res.isSuccess());
      assertTrue(res.getMessage().contains("xóa item của người khác"));
    }

    @Test
    @DisplayName("Item đang đấu giá → RuntimeException → response lỗi")
    void itemInActiveAuction_returnsErrorResponse() throws Exception {
      JsonObject req = wrappedRequest(payloadWithItemId(5));
      doThrow(new RuntimeException("Không thể xóa sản phẩm đang trong phiên đấu giá"))
          .when(itemWriteService).deleteItem(USER_ID, 5);

      ServerResponse res = itemController.handleDeleteItem(req, REQUEST_ID, USER_ID);

      assertFalse(res.isSuccess());
    }

    @Test
    @DisplayName("Service ném SQLException → response lỗi")
    void sqlException_returnsErrorResponse() throws Exception {
      JsonObject req = wrappedRequest(payloadWithItemId(5));
      doThrow(new SQLException("db error"))
          .when(itemWriteService).deleteItem(USER_ID, 5);

      ServerResponse res = itemController.handleDeleteItem(req, REQUEST_ID, USER_ID);

      assertFalse(res.isSuccess());
    }
  }
  // 9. Kiểm tra cấu trúc ServerResponse chung
  // ════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ServerResponse structure validation")
  class ServerResponseStructureTests {

    @Test
    @DisplayName("Response thành công phải có action đúng và success=true")
    void successResponse_hasCorrectActionAndSuccess() throws Exception {
      when(itemQueryService.getAuctions(any())).thenReturn(List.of());
      ServerResponse res = itemController.handleGetAll(new JsonObject(), "r-1");

      assertAll(
          () -> assertEquals("ITEM_GET_ALL", res.getAction()),
          () -> assertTrue(res.isSuccess()),
          () -> assertNotNull(res.getMessage()),
          () -> assertNotNull(res.getTimestamp())
      );
    }

    @Test
    @DisplayName("Response lỗi phải có action đúng, success=false, message chứa thông tin lỗi")
    void errorResponse_hasCorrectStructure() throws Exception {
      when(itemQueryService.getAuctions(any()))
          .thenThrow(new RuntimeException("test error"));
      ServerResponse res = itemController.handleGetAll(new JsonObject(), "r-2");

      assertAll(
          () -> assertEquals("ITEM_GET_ALL", res.getAction()),
          () -> assertFalse(res.isSuccess()),
          () -> assertTrue(res.getMessage().startsWith("Lỗi: ")),
          () -> assertEquals("r-2", res.getRequestId())
      );
    }
  }
}