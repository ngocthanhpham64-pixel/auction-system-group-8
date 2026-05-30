package vn.edu.vnu.uet.group8.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import java.time.Instant;
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

import vn.edu.vnu.uet.group8.common.dto.model.ReviewDTO;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.service.user.RatingService;

@ExtendWith(MockitoExtension.class)
class RatingControllerTest {

  @Mock private RatingService ratingService;

  private RatingController controller;

  @BeforeEach
  void setUp() {
    controller = new RatingController(ratingService);
  }

  // Helper: tạo request với payload
  private JsonObject reqWithPayload(int sellerId, int itemId, String username, int score, String comment) {
    JsonObject req = new JsonObject();
    JsonObject payload = new JsonObject();
    payload.addProperty("sellerId", sellerId);
    payload.addProperty("itemId", itemId);
    payload.addProperty("username", username);
    payload.addProperty("score", score);
    if (comment != null) payload.addProperty("comment", comment);
    req.add("payload", payload);
    return req;
  }

  private JsonObject reqSellerIdPayload(int sellerId) {
    JsonObject req = new JsonObject();
    JsonObject payload = new JsonObject();
    payload.addProperty("sellerId", sellerId);
    req.add("payload", payload);
    return req;
  }

  // ─────────────────────────────────────────────────────────────
  // handleRateSeller
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("handleRateSeller()")
  class RateSellerTest {

    @Test
    @DisplayName("Success - đánh giá hợp lệ")
    void success() throws SQLException {
      JsonObject req = reqWithPayload(2, 50, "seller01", 5, "Xuất sắc");

      ServerResponse resp = controller.handleRateSeller(req, "req-rate", 1);

      assertTrue(resp.isSuccess());
      assertEquals("USER_RATE_SELLER", resp.getAction());
      verify(ratingService).rateSeller(1, "seller01", 2, 50, 5, "Xuất sắc");
    }

    @Test
    @DisplayName("Success - không có comment (null)")
    void successNoComment() throws SQLException {
      JsonObject req = reqWithPayload(2, 50, "seller01", 4, null);

      ServerResponse resp = controller.handleRateSeller(req, "req-no-comment", 1);

      assertTrue(resp.isSuccess());
      verify(ratingService).rateSeller(eq(1), eq("seller01"), eq(2), eq(50), eq(4), isNull());
    }

    @Test
    @DisplayName("payload là root (không wrap)")
    void payloadAtRoot() throws SQLException {
      JsonObject req = new JsonObject();
      req.addProperty("sellerId", 3);
      req.addProperty("itemId", 50);
      req.addProperty("username", "seller_x");
      req.addProperty("score", 3);

      ServerResponse resp = controller.handleRateSeller(req, "req-root", 1);

      assertTrue(resp.isSuccess());
      verify(ratingService).rateSeller(1, "seller_x", 3, 50, 3, null);
    }

    @Test
    @DisplayName("ValidationException từ service → error response")
    void validationError() throws SQLException {
      doThrow(new ValidationException("Không thể tự đánh giá"))
          .when(ratingService).rateSeller(anyInt(), anyString(), anyInt(), anyInt(), anyInt(), any());
      JsonObject req = reqWithPayload(1, 50, "self", 5, null); // raterId == sellerId

      ServerResponse resp = controller.handleRateSeller(req, "req-val-err", 1);

      assertFalse(resp.isSuccess());
      assertEquals("USER_RATE_SELLER", resp.getAction());
      assertTrue(resp.getMessage().contains("Không thể tự đánh giá"));
    }

    @Test
    @DisplayName("SQLException từ service → error response")
    void sqlException() throws SQLException {
      doThrow(new SQLException("DB lỗi"))
          .when(ratingService).rateSeller(anyInt(), anyString(), anyInt(), anyInt(), anyInt(), any());
      JsonObject req = reqWithPayload(2, 50, "s", 4, "ok");

      ServerResponse resp = controller.handleRateSeller(req, "req-sql", 1);

      assertFalse(resp.isSuccess());
    }

    @Test
    @DisplayName("Thiếu sellerId → error response")
    void thieuSellerId() throws SQLException {
      JsonObject req = new JsonObject();
      JsonObject payload = new JsonObject();
      payload.addProperty("itemId", 50);
      payload.addProperty("username", "s");
      payload.addProperty("score", 3);
      req.add("payload", payload); // không có sellerId

      ServerResponse resp = controller.handleRateSeller(req, "req-no-seller", 1);

      assertFalse(resp.isSuccess());
    }

    @Test
    @DisplayName("Thiếu score → error response")
    void thieuScore() {
      JsonObject req = new JsonObject();
      JsonObject payload = new JsonObject();
      payload.addProperty("sellerId", 2);
      payload.addProperty("itemId", 50);
      payload.addProperty("username", "s");
      req.add("payload", payload); // không có score

      ServerResponse resp = controller.handleRateSeller(req, "req-no-score", 1);

      assertFalse(resp.isSuccess());
    }
  }

  // ─────────────────────────────────────────────────────────────
  // handleGetSellerReviews
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("handleGetSellerReviews()")
  class GetSellerReviewsTest {

    @Test
    @DisplayName("Success - trả về danh sách review")
    void success() throws SQLException {
      List<ReviewDTO> reviews = List.of(
          new ReviewDTO("u1", 5, "Tốt", Instant.now()),
          new ReviewDTO("u2", 4, "Ổn", Instant.now()));
      when(ratingService.getSellerReviews(10)).thenReturn(reviews);

      ServerResponse resp = controller.handleGetSellerReviews(
          reqSellerIdPayload(10), "req-reviews");

      assertTrue(resp.isSuccess());
      assertEquals("USER_GET_SELLER_REVIEWS", resp.getAction());
      verify(ratingService).getSellerReviews(10);
    }

    @Test
    @DisplayName("Success - list rỗng")
    void successEmpty() throws SQLException {
      when(ratingService.getSellerReviews(anyInt())).thenReturn(Collections.emptyList());

      ServerResponse resp = controller.handleGetSellerReviews(
          reqSellerIdPayload(99), "req-empty");

      assertTrue(resp.isSuccess());
    }

    @Test
    @DisplayName("sellerId ở root level")
    void sellerIdAtRoot() throws SQLException {
      when(ratingService.getSellerReviews(5)).thenReturn(Collections.emptyList());
      JsonObject req = new JsonObject();
      req.addProperty("sellerId", 5);

      ServerResponse resp = controller.handleGetSellerReviews(req, "req-root");

      assertTrue(resp.isSuccess());
      verify(ratingService).getSellerReviews(5);
    }

    @Test
    @DisplayName("Thiếu sellerId → error response")
    void thieuSellerId() {
      JsonObject req = new JsonObject();
      req.add("payload", new JsonObject());

      ServerResponse resp = controller.handleGetSellerReviews(req, "req-no-seller");

      assertFalse(resp.isSuccess());
    }

    @Test
    @DisplayName("SQLException từ service → error response")
    void sqlException() throws SQLException {
      when(ratingService.getSellerReviews(anyInt())).thenThrow(new SQLException("DB"));

      ServerResponse resp = controller.handleGetSellerReviews(
          reqSellerIdPayload(1), "req-sql");

      assertFalse(resp.isSuccess());
      assertEquals("USER_GET_SELLER_REVIEWS", resp.getAction());
    }
  }
}