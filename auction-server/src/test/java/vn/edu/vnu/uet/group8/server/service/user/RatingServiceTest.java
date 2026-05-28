package vn.edu.vnu.uet.group8.server.service.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.CommentDAO;
import vn.edu.vnu.uet.group8.server.dao.RatingDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;

/**
 * Unit test cho {@link RatingService}.
 */
@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

  private static final String RATER_USERNAME = "buyer-1";
  private static final int ITEM_ID = 10;

  @Mock private RatingDAO ratingDAO;
  @Mock private UserDAO userDAO;
  @Mock private CommentDAO commentDAO;

  @InjectMocks private RatingService ratingService;

  // ════════════════════════════════════════════════════
  // VALIDATE INPUT
  // ════════════════════════════════════════════════════

  @Nested
  @DisplayName("Validate input")
  class ValidateInput {

    @Test
    @DisplayName("Tự đánh giá chính mình → ValidationException")
    void tuDanhGia() {
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> ratingService.rateSeller(
              5,
              RATER_USERNAME,
              5,
              ITEM_ID,
              4,
              "comment"
          )
      );

      assertTrue(
          ex.getMessage().toLowerCase().contains("chính mình")
              || ex.getMessage().toLowerCase().contains("yourself")
      );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 6, 10, 100})
    @DisplayName("Điểm ngoài [1,5] → ValidationException")
    void diemKhongHopLe(int score) {
      assertThrows(
          ValidationException.class,
          () -> ratingService.rateSeller(
              1,
              RATER_USERNAME,
              2,
              ITEM_ID,
              score,
              "comment"
          )
      );
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    @DisplayName("Điểm hợp lệ → vượt qua validate")
    void diemHopLe(int score) throws SQLException {
      when(ratingDAO.hasBoughtFrom(1, 2)).thenReturn(true);
      when(ratingDAO.hasRated(1, ITEM_ID)).thenReturn(false);

      assertDoesNotThrow(
          () -> ratingService.rateSeller(
              1,
              RATER_USERNAME,
              2,
              ITEM_ID,
              score,
              "good"
          )
      );
    }
  }

  // ════════════════════════════════════════════════════
  // BUSINESS RULES
  // ════════════════════════════════════════════════════

  @Nested
  @DisplayName("Business rules")
  class BusinessRules {

    @Test
    @DisplayName("Chưa mua/thắng đấu giá → ValidationException")
    void chuaMuaHang() throws SQLException {
      when(ratingDAO.hasBoughtFrom(1, 2)).thenReturn(false);

      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> ratingService.rateSeller(
              1,
              RATER_USERNAME,
              2,
              ITEM_ID,
              5,
              "comment"
          )
      );

      assertTrue(
          ex.getMessage().toLowerCase().contains("thắng")
              || ex.getMessage().toLowerCase().contains("mua")
              || ex.getMessage().toLowerCase().contains("đánh giá")
      );
    }

    @Test
    @DisplayName("Đã đánh giá seller rồi → ValidationException")
    void daDanhGia() throws SQLException {
      when(ratingDAO.hasBoughtFrom(1, 2)).thenReturn(true);
      when(ratingDAO.hasRated(1, ITEM_ID)).thenReturn(true);

      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> ratingService.rateSeller(
              1,
              RATER_USERNAME,
              2,
              ITEM_ID,
              5,
              "comment"
          )
      );

      assertTrue(
          ex.getMessage().toLowerCase().contains("đã")
              || ex.getMessage().toLowerCase().contains("rated")
      );
    }
  }

  // ════════════════════════════════════════════════════
  // SUCCESS PATH
  // ════════════════════════════════════════════════════

  @Nested
  @DisplayName("Success path")
  class SuccessPath {

    @Test
    @DisplayName("Đủ điều kiện → insertRating + updateSellerRating")
    void thanhCong() throws SQLException {
      when(ratingDAO.hasBoughtFrom(1, 2)).thenReturn(true);
      when(ratingDAO.hasRated(1, ITEM_ID)).thenReturn(false);

      ratingService.rateSeller(
          1,
          RATER_USERNAME,
          2,
          ITEM_ID,
          5,
          "Excellent seller"
      );

      verify(ratingDAO).insertRating(
          1,
          RATER_USERNAME,
          2,
          ITEM_ID,
          5,
          "Excellent seller"
      );

      verify(userDAO).updateSellerRating(2);
    }

    @Test
    @DisplayName("Comment null vẫn hợp lệ")
    void commentNull() throws SQLException {
      when(ratingDAO.hasBoughtFrom(1, 2)).thenReturn(true);
      when(ratingDAO.hasRated(1, ITEM_ID)).thenReturn(false);

      assertDoesNotThrow(
          () -> ratingService.rateSeller(
              1,
              RATER_USERNAME,
              2,
              ITEM_ID,
              4,
              null
          )
      );

      verify(ratingDAO).insertRating(
          1,
          RATER_USERNAME,
          2,
          ITEM_ID,
          4,
          null
      );
    }

    @Test
    @DisplayName("insertRating phải chạy trước updateSellerRating")
    void thuTuOperation() throws SQLException {
      when(ratingDAO.hasBoughtFrom(1, 2)).thenReturn(true);
      when(ratingDAO.hasRated(1, ITEM_ID)).thenReturn(false);

      ratingService.rateSeller(
          1,
          RATER_USERNAME,
          2,
          ITEM_ID,
          5,
          "good"
      );

      var inOrder = inOrder(ratingDAO, userDAO);

      inOrder.verify(ratingDAO).insertRating(
          1,
          RATER_USERNAME,
          2,
          ITEM_ID,
          5,
          "good"
      );

      inOrder.verify(userDAO).updateSellerRating(2);
    }
  }
}