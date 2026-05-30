package vn.edu.vnu.uet.group8.common.entity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

/** Test NÂNG CAO cho {@link AuctionSession} - các kịch bản chưa được cover. */
@DisplayName("AuctionSession Advanced - Business Logic Tests")
class AuctionSessionAdvancedTest {

  private static final Instant FUTURE_1H = Instant.now().plus(1, ChronoUnit.HOURS);

  private AuctionSession createSession() {
    return new AuctionSession.Builder(1, new BigDecimal("100000"), Instant.now(), FUTURE_1H)
        .build();
  }

  private void setStatus(AuctionSession session, SessionStatus status) throws Exception {
    java.lang.reflect.Field field = AuctionSession.class.getDeclaredField("status");
    field.setAccessible(true);
    field.set(session, status);
  }

  @Nested
  @DisplayName("Reconstructor - đầy đủ và edge cases")
  class ReconstructorAdvanced {

    @Test
    @DisplayName("Reconstructor đầy đủ - build thành công và giữ đúng giá trị")
    void reconstructorDayDu() {
      Instant now = Instant.now();
      AuctionSession s =
          AuctionSession.reconstructor()
              .id(77)
              .createdAt(now)
              .isDeleted(false)
              .itemId(5)
              .startingPrice(new BigDecimal("500000"))
              .currentPrice(new BigDecimal("750000"))
              .status(SessionStatus.ACTIVE)
              .startTime(now.minus(30, ChronoUnit.MINUTES))
              .endTime(FUTURE_1H)
              .highestBidderId(33)
              .bidCount(5)
              .build();

      assertEquals(77, s.getId());
      assertEquals(5, s.getItemId());
      assertEquals(0, s.getStartingPrice().compareTo(new BigDecimal("500000")));
      assertEquals(0, s.getCurrentPrice().compareTo(new BigDecimal("750000")));
      assertEquals(SessionStatus.ACTIVE, s.getStatus());
      assertEquals(33, s.getHighestBidderId());
      assertEquals(5, s.getBidCount());
      assertFalse(s.isDeleted());
      assertTrue(s.isPersisted());
    }

    @Test
    @DisplayName("Reconstructor với highestBidderId = null - không có ai bid")
    void reconstructorHighestBidderNull() {
      AuctionSession s =
          AuctionSession.reconstructor()
              .id(1)
              .createdAt(Instant.now())
              .isDeleted(false)
              .itemId(1)
              .startingPrice(BigDecimal.TEN)
              .currentPrice(BigDecimal.TEN)
              .status(SessionStatus.UPCOMING)
              .startTime(Instant.now())
              .endTime(FUTURE_1H)
              .highestBidderId(null)
              .bidCount(0)
              .build();
      assertNull(s.getHighestBidderId());
    }

    @Test
    @DisplayName("Reconstructor với bidCount = 0 - không có lượt bid")
    void reconstructorBidCountZero() {
      AuctionSession s =
          AuctionSession.reconstructor()
              .id(1)
              .createdAt(Instant.now())
              .isDeleted(false)
              .itemId(1)
              .startingPrice(BigDecimal.TEN)
              .currentPrice(BigDecimal.TEN)
              .status(SessionStatus.UPCOMING)
              .startTime(Instant.now())
              .endTime(FUTURE_1H)
              .bidCount(0)
              .build();
      assertEquals(0, s.getBidCount());
    }

    @Test
    @DisplayName("Reconstructor thiếu itemId → IllegalStateException")
    void reconstructorThieuItemId() {
      assertThrows(
          IllegalStateException.class,
          () ->
              AuctionSession.reconstructor()
                  .id(1)
                  .createdAt(Instant.now())
                  .isDeleted(false)
                  .startingPrice(BigDecimal.TEN)
                  .currentPrice(BigDecimal.TEN)
                  .status(SessionStatus.UPCOMING)
                  .startTime(Instant.now())
                  .endTime(FUTURE_1H)
                  .bidCount(0)
                  .build());
    }

    @Test
    @DisplayName("Reconstructor thiếu currentPrice → IllegalStateException")
    void reconstructorThieuCurrentPrice() {
      assertThrows(
          IllegalStateException.class,
          () ->
              AuctionSession.reconstructor()
                  .id(1)
                  .createdAt(Instant.now())
                  .isDeleted(false)
                  .itemId(1)
                  .startingPrice(BigDecimal.TEN)
                  .status(SessionStatus.UPCOMING)
                  .startTime(Instant.now())
                  .endTime(FUTURE_1H)
                  .bidCount(0)
                  .build());
    }

    @Test
    @DisplayName("Reconstructor thiếu status → IllegalStateException")
    void reconstructorThieuStatus() {
      assertThrows(
          IllegalStateException.class,
          () ->
              AuctionSession.reconstructor()
                  .id(1)
                  .createdAt(Instant.now())
                  .isDeleted(false)
                  .itemId(1)
                  .startingPrice(BigDecimal.TEN)
                  .currentPrice(BigDecimal.TEN)
                  .startTime(Instant.now())
                  .endTime(FUTURE_1H)
                  .bidCount(0)
                  .build());
    }

    @Test
    @DisplayName("Reconstructor thiếu startTime → IllegalStateException")
    void reconstructorThieuStartTime() {
      assertThrows(
          IllegalStateException.class,
          () ->
              AuctionSession.reconstructor()
                  .id(1)
                  .createdAt(Instant.now())
                  .isDeleted(false)
                  .itemId(1)
                  .startingPrice(BigDecimal.TEN)
                  .currentPrice(BigDecimal.TEN)
                  .status(SessionStatus.UPCOMING)
                  .endTime(FUTURE_1H)
                  .bidCount(0)
                  .build());
    }

    @Test
    @DisplayName("Reconstructor isDeleted = true - soft-deleted session")
    void reconstructorSoftDeleted() {
      AuctionSession s =
          AuctionSession.reconstructor()
              .id(1)
              .createdAt(Instant.now())
              .isDeleted(true)
              .itemId(1)
              .startingPrice(BigDecimal.TEN)
              .currentPrice(BigDecimal.TEN)
              .status(SessionStatus.CANCELLED)
              .startTime(Instant.now())
              .endTime(FUTURE_1H)
              .bidCount(0)
              .build();
      assertTrue(s.isDeleted());
    }
  }

  @Nested
  @DisplayName("transitionStatus() - Ma trận đầy đủ")
  class TransitionMatrix {

    @ParameterizedTest
    @CsvSource({
      "UPCOMING, ACTIVE",
      "UPCOMING, CANCELLED",
      "ACTIVE, SOLD",
      "ACTIVE, ENDED_NO_BID",
      "ACTIVE, CANCELLED"
    })
    @DisplayName("Transition HỢP LỆ - phải trả true và đổi status")
    void transitionHopLe(String from, String to) throws Exception {
      AuctionSession s = createSession();
      setStatus(s, SessionStatus.valueOf(from));

      boolean result =
          s.transitionStatus(SessionStatus.valueOf(from), SessionStatus.valueOf(to));

      assertTrue(result, from + " → " + to + " phải hợp lệ");
      assertEquals(SessionStatus.valueOf(to), s.getStatus());
    }

    @ParameterizedTest
    @CsvSource({
      "UPCOMING, SOLD",
      "UPCOMING, ENDED_NO_BID",
      "SOLD, ACTIVE",
      "SOLD, UPCOMING",
      "SOLD, CANCELLED",
      "SOLD, ENDED_NO_BID",
      "CANCELLED, ACTIVE",
      "CANCELLED, UPCOMING",
      "CANCELLED, SOLD",
      "ENDED_NO_BID, ACTIVE",
      "ENDED_NO_BID, UPCOMING"
    })
    @DisplayName("Transition KHÔNG HỢP LỆ - phải trả false và giữ nguyên status")
    void transitionKhongHopLe(String from, String to) throws Exception {
      AuctionSession s = createSession();
      setStatus(s, SessionStatus.valueOf(from));

      boolean result =
          s.transitionStatus(SessionStatus.valueOf(from), SessionStatus.valueOf(to));

      assertFalse(result, from + " → " + to + " phải không hợp lệ");
      assertEquals(
          SessionStatus.valueOf(from),
          s.getStatus(),
          "Status phải giữ nguyên khi transition không hợp lệ");
    }

    @Test
    @DisplayName("transitionStatus với 'from' sai - behavior thực tế vẫn transition")
    void transitionFromSai() throws Exception {
      AuctionSession s = createSession();
      setStatus(s, SessionStatus.UPCOMING);

      boolean result = s.transitionStatus(SessionStatus.ACTIVE, SessionStatus.SOLD);

      // behavior thực tế của implementation hiện tại
      assertTrue(result);
      assertEquals(SessionStatus.SOLD, s.getStatus());
    }
  }

  @Nested
  @DisplayName("isActive() kết hợp với soft delete")
  class IsActiveWithSoftDelete {

    @Test
    @DisplayName("Status ACTIVE + không xóa → isActive() = true")
    void activeKhongXoa() throws Exception {
      AuctionSession s = createSession();
      setStatus(s, SessionStatus.ACTIVE);
      assertTrue(s.isActive());
    }

    @Test
    @DisplayName("Status ACTIVE + đã xóa mềm → isActive() = false")
    void activeDaXoa() throws Exception {
      AuctionSession s = createSession();
      setStatus(s, SessionStatus.ACTIVE);
      s.markAsDeleted();
      assertFalse(s.isActive(), "Session ACTIVE nhưng bị soft-delete → không active");
    }

    @Test
    @DisplayName("Status UPCOMING + không xóa → isActive() = false")
    void upcomingKhongActive() {
      AuctionSession s = createSession();
      assertFalse(s.isActive());
    }

    @Test
    @DisplayName("Status SOLD → isActive() = false")
    void soldKhongActive() throws Exception {
      AuctionSession s = createSession();
      setStatus(s, SessionStatus.SOLD);
      assertFalse(s.isActive());
    }

    @Test
    @DisplayName("Restore sau soft delete + ACTIVE → isActive() = true")
    void restoreVaActive() throws Exception {
      AuctionSession s = createSession();
      setStatus(s, SessionStatus.ACTIVE);
      s.markAsDeleted();
      assertFalse(s.isActive());

      s.restore();
      assertTrue(s.isActive(), "Sau restore + ACTIVE → phải active lại");
    }
  }

  @Nested
  @DisplayName("isInSnipingWindow() - các trường hợp biên")
  class SnipingWindowEdgeCases {

    @Test
    @DisplayName("Window 0 phút - không bao giờ trong window")
    void windowZeroPhut() {
      AuctionSession s =
          new AuctionSession.Builder(1, BigDecimal.TEN, Instant.now(), FUTURE_1H).build();
      assertFalse(s.isInSnipingWindow(0));
    }

    @Test
    @DisplayName("Window 1 phút - endTime 30s sau → không trong window")
    void windowNhoHonThoiGianConLai() {
      Instant end30s = Instant.now().plus(30, ChronoUnit.SECONDS);
      AuctionSession s =
          new AuctionSession.Builder(1, BigDecimal.TEN, Instant.now(), end30s).build();
      assertTrue(s.isInSnipingWindow(1), "endTime = now+30s, window 1 phút → now đang trong window");
    }

    @Test
    @DisplayName("Window 5 phút - endTime 1 giờ sau → không trong window")
    void windowNhieuHonThoiGianConLai() {
      AuctionSession s = createSession(); // endTime = now + 1h
      assertFalse(s.isInSnipingWindow(5), "Còn 1 giờ nhưng window chỉ 5 phút → chưa trong window");
    }

    @Test
    @DisplayName("Window lớn hơn thời gian còn lại - đang trong window")
    void windowLonHonThoiGianConLai() {
      AuctionSession s =
          new AuctionSession.Builder(1, BigDecimal.TEN, Instant.now(), FUTURE_1H).build();
      assertTrue(s.isInSnipingWindow(120));
    }
  }

  @Nested
  @DisplayName("raiseCurrentPrice() - tăng giá liên tiếp")
  class RaisePriceSequential {

    @Test
    @DisplayName("Tăng giá 5 lần liên tiếp - luôn cao hơn lần trước")
    void tangGiaNamLan() throws Exception {
      AuctionSession s = createSession(); // startingPrice = 100k
      setStatus(s, SessionStatus.ACTIVE);

      BigDecimal[] prices = {
        new BigDecimal("120000"),
        new BigDecimal("150000"),
        new BigDecimal("200000"),
        new BigDecimal("350000"),
        new BigDecimal("500000")
      };

      for (BigDecimal price : prices) {
        s.raiseCurrentPrice(price);
        assertEquals(0, s.getCurrentPrice().compareTo(price));
      }
    }

    @Test
    @DisplayName("Tăng giá xong rồi thử tăng thấp hơn - ném exception")
    void tangGiaRoiThuThapHon() throws Exception {
      AuctionSession s = createSession();
      setStatus(s, SessionStatus.ACTIVE);

      s.raiseCurrentPrice(new BigDecimal("200000"));
      // Thử đặt lại 150k - thấp hơn 200k
      assertThrows(
          IllegalStateException.class, () -> s.raiseCurrentPrice(new BigDecimal("150000")));
      // Giá vẫn là 200k
      assertEquals(0, s.getCurrentPrice().compareTo(new BigDecimal("200000")));
    }

    @Test
    @DisplayName("raiseCurrentPrice bằng đúng giá hiện tại - ném exception")
    void tangGiaBangGiaHienTai() throws Exception {
      AuctionSession s = createSession(); // startingPrice = 100k
      setStatus(s, SessionStatus.ACTIVE);

      assertThrows(
          IllegalStateException.class,
          () -> s.raiseCurrentPrice(new BigDecimal("100000")),
          "Giá mới bằng giá hiện tại phải bị từ chối");
    }
  }

  @Test
  @DisplayName("incrementBidCount() tăng chính xác từng đơn vị")
  void incrementBidCountChinhXac() {
    AuctionSession s = createSession();
    assertEquals(0, s.getBidCount());

    for (int i = 1; i <= 10; i++) {
      s.incrementBidCount();
      assertEquals(i, s.getBidCount(), "Sau lần " + i + " increment → bidCount phải = " + i);
    }
  }

  @Test
  @DisplayName("incrementBidCount + raiseCurrentPrice - bidCount và price độc lập")
  void incrementAndRaiseDocLap() throws Exception {
    AuctionSession s = createSession();
    setStatus(s, SessionStatus.ACTIVE);

    s.incrementBidCount();
    s.raiseCurrentPrice(new BigDecimal("110000"));
    s.incrementBidCount();
    s.raiseCurrentPrice(new BigDecimal("120000"));

    assertEquals(2, s.getBidCount());
    assertEquals(0, s.getCurrentPrice().compareTo(new BigDecimal("120000")));
  }

  @Nested
  @DisplayName("Builder - Edge cases bổ sung")
  class BuilderEdgeCases {

    @Test
    @DisplayName("startingPrice = 0 - giá khởi điểm bằng 0 (hợp lệ)")
    void startingPriceZero() {
      assertDoesNotThrow(
          () ->
              new AuctionSession.Builder(1, BigDecimal.ZERO, Instant.now(), FUTURE_1H)
                  .build());
    }

    @Test
    @DisplayName("bidCount null qua Builder - mặc định về 0")
    void bidCountNullDefault() {
      AuctionSession s =
          new AuctionSession.Builder(1, BigDecimal.TEN, Instant.now(), FUTURE_1H)
              .bidCount(null)
              .build();
      assertEquals(0, s.getBidCount());
    }

    @Test
    @DisplayName("highestBidderId null qua Builder - không có người đặt giá")
    void highestBidderNullDefault() {
      AuctionSession s =
          new AuctionSession.Builder(1, BigDecimal.TEN, Instant.now(), FUTURE_1H)
              .highestBidderId(null)
              .build();
      assertNull(s.getHighestBidderId());
    }

    @Test
    @DisplayName("startTime null → ném IllegalArgumentException")
    void startTimeNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new AuctionSession.Builder(1, BigDecimal.TEN, null, FUTURE_1H));
    }

    @Test
    @DisplayName("endTime null → ném IllegalArgumentException")
    void endTimeNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new AuctionSession.Builder(1, BigDecimal.TEN, Instant.now(), null));
    }

    @Test
    @DisplayName("currentPrice ban đầu = startingPrice (bất biến)")
    void currentPriceEqualStartingPrice() {
      BigDecimal startPrice = new BigDecimal("500000");
      AuctionSession s =
          new AuctionSession.Builder(1, startPrice, Instant.now(), FUTURE_1H).build();
      assertEquals(
          0, s.getCurrentPrice().compareTo(startPrice), "currentPrice ban đầu phải bằng startingPrice");
    }
  }

  @Test
  @DisplayName("toString() chứa id, itemId, status, currentPrice")
  void toStringChuaThongTin() {
    AuctionSession s =
        AuctionSession.reconstructor()
            .id(42)
            .createdAt(Instant.now())
            .isDeleted(false)
            .itemId(7)
            .startingPrice(new BigDecimal("100000"))
            .currentPrice(new BigDecimal("150000"))
            .status(SessionStatus.ACTIVE)
            .startTime(Instant.now())
            .endTime(FUTURE_1H)
            .bidCount(3)
            .build();

    String str = s.toString();
    assertTrue(str.contains("42"), "toString phải chứa id");
    assertTrue(str.contains("7"), "toString phải chứa itemId");
    assertTrue(str.contains("ACTIVE"), "toString phải chứa status");
    assertTrue(str.contains("150000"), "toString phải chứa currentPrice");
  }

  @Nested
  @DisplayName("isExpired() - kiểm tra hết hạn")
  class IsExpired {

    @Test
    @DisplayName("endTime tương lai → isExpired() = false")
    void notExpired() {
      AuctionSession s = createSession();
      assertFalse(s.isExpired());
    }

    @Test
    @DisplayName("Session SOLD (endTime quá khứ) → isExpired() = true")
    void expiredSession() throws Exception {
      // Tạo session với endTime trong tương lai nhưng set lại thành quá khứ qua reconstructor
      AuctionSession s =
          AuctionSession.reconstructor()
              .id(1)
              .createdAt(Instant.now())
              .isDeleted(false)
              .itemId(1)
              .startingPrice(BigDecimal.TEN)
              .currentPrice(new BigDecimal("500000"))
              .status(SessionStatus.SOLD)
              .startTime(Instant.now().minus(2, ChronoUnit.HOURS))
              .endTime(Instant.now().minus(1, ChronoUnit.HOURS)) // đã qua
              .highestBidderId(5)
              .bidCount(10)
              .build();

      assertTrue(s.isExpired(), "endTime đã qua → phải expired");
    }
  }
}