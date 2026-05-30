package vn.edu.vnu.uet.group8.common.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

/** Unit test cho {@link AuctionSession} entity. */
class AuctionSessionTest {

  /** Helper: tạo session sạch với endTime tương lai. */
  private AuctionSession createSession(Instant endTime) {
    return new AuctionSession.Builder(1, new BigDecimal("100000"), Instant.now(), endTime).build();
  }

  /** Helper: set status qua reflection (để test các state khác UPCOMING). */
  private void setStatus(AuctionSession session, SessionStatus status) throws Exception {
    Field field = AuctionSession.class.getDeclaredField("status");
    field.setAccessible(true);
    field.set(session, status);
  }

  @Nested
  @DisplayName("Builder pattern")
  class BuilderTest {

    @Test
    @DisplayName("Builder hợp lệ tạo session với status UPCOMING")
    void builderHopLeStatusUpcoming() {
      Instant start = Instant.now();
      Instant end = start.plus(1, ChronoUnit.DAYS);
      AuctionSession session =
          new AuctionSession.Builder(1, new BigDecimal("100000"), start, end).build();

      assertEquals(SessionStatus.UPCOMING, session.getStatus());
      assertEquals(1, session.getItemId());
      assertEquals(0, session.getStartingPrice().compareTo(new BigDecimal("100000")));
      assertEquals(
          0,
          session.getCurrentPrice().compareTo(new BigDecimal("100000")),
          "currentPrice ban đầu = startingPrice");
      assertEquals(0, session.getBidCount());
    }

    @Test
    @DisplayName("itemId <= 0 phải ném exception")
    void itemIdInvalidPhaiNem() {
      Instant end = Instant.now().plus(1, ChronoUnit.HOURS);
      assertThrows(
          IllegalArgumentException.class,
          () -> new AuctionSession.Builder(0, BigDecimal.TEN, Instant.now(), end));
      assertThrows(
          IllegalArgumentException.class,
          () -> new AuctionSession.Builder(-1, BigDecimal.TEN, Instant.now(), end));
    }

    @Test
    @DisplayName("startingPrice null phải ném exception")
    void startingPriceNullPhaiNem() {
      Instant end = Instant.now().plus(1, ChronoUnit.HOURS);
      assertThrows(
          IllegalArgumentException.class,
          () -> new AuctionSession.Builder(1, null, Instant.now(), end));
    }

    @Test
    @DisplayName("startingPrice âm phải ném exception")
    void startingPriceAmPhaiNem() {
      Instant end = Instant.now().plus(1, ChronoUnit.HOURS);
      assertThrows(
          IllegalArgumentException.class,
          () -> new AuctionSession.Builder(1, new BigDecimal("-100"), Instant.now(), end));
    }

    @Test
    @DisplayName("endTime trong quá khứ phải ném exception")
    void endTimeQuaKhuPhaiNem() {
      Instant pastEnd = Instant.now().minus(1, ChronoUnit.HOURS);
      assertThrows(
          IllegalArgumentException.class,
          () -> new AuctionSession.Builder(1, BigDecimal.TEN, Instant.now(), pastEnd));
    }

    @Test
    @DisplayName("endTime trước startTime phải ném exception")
    void endTimeTruocStartTimePhaiNem() {
      Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
      Instant end = Instant.now().plus(1, ChronoUnit.HOURS);
      assertThrows(
          IllegalArgumentException.class,
          () -> new AuctionSession.Builder(1, BigDecimal.TEN, start, end));
    }
  }

  @Nested
  @DisplayName("Time logic")
  class TimeLogic {

    @Test
    @DisplayName("isExpired = false khi endTime ở tương lai")
    void isExpiredFalseTuongLai() {
      AuctionSession session = createSession(Instant.now().plus(1, ChronoUnit.HOURS));
      assertFalse(session.isExpired());
    }

    @Test
    @DisplayName("isInSnipingWindow = false khi còn nhiều thời gian")
    void isInSnipingWindowFalseConNhieuThoiGian() {
      AuctionSession session = createSession(Instant.now().plus(1, ChronoUnit.HOURS));
      assertFalse(session.isInSnipingWindow(5));
    }

    @Test
    @DisplayName("isInSnipingWindow = true khi endTime gần (1 phút sau, window 5 phút)")
    void isInSnipingWindowTrueGanEnd() {
      AuctionSession session = createSession(Instant.now().plus(1, ChronoUnit.MINUTES));
      assertTrue(session.isInSnipingWindow(5));
    }
  }

  @Nested
  @DisplayName("State transition")
  class StateTransition {

    @Test
    @DisplayName("UPCOMING → ACTIVE hợp lệ")
    void upcomingToActive() {
      AuctionSession session = createSession(Instant.now().plus(1, ChronoUnit.HOURS));
      assertTrue(session.transitionStatus(SessionStatus.UPCOMING, SessionStatus.ACTIVE));
      assertEquals(SessionStatus.ACTIVE, session.getStatus());
    }

    @Test
    @DisplayName("UPCOMING → CANCELLED hợp lệ")
    void upcomingToCancelled() {
      AuctionSession session = createSession(Instant.now().plus(1, ChronoUnit.HOURS));
      assertTrue(session.transitionStatus(SessionStatus.UPCOMING, SessionStatus.CANCELLED));
      assertEquals(SessionStatus.CANCELLED, session.getStatus());
    }

    @Test
    @DisplayName("ACTIVE → SOLD hợp lệ")
    void activeToSold() throws Exception {
      AuctionSession session = createSession(Instant.now().plus(1, ChronoUnit.HOURS));
      setStatus(session, SessionStatus.ACTIVE);

      assertTrue(session.transitionStatus(SessionStatus.ACTIVE, SessionStatus.SOLD));
      assertEquals(SessionStatus.SOLD, session.getStatus());
    }

    @Test
    @DisplayName("ACTIVE → ENDED_NO_BID hợp lệ")
    void activeToEndedNoBid() throws Exception {
      AuctionSession session = createSession(Instant.now().plus(1, ChronoUnit.HOURS));
      setStatus(session, SessionStatus.ACTIVE);

      assertTrue(session.transitionStatus(SessionStatus.ACTIVE, SessionStatus.ENDED_NO_BID));
      assertEquals(SessionStatus.ENDED_NO_BID, session.getStatus());
    }

    @Test
    @DisplayName("UPCOMING → SOLD KHÔNG hợp lệ - phải skip transition")
    void upcomingToSoldKhongHopLe() {
      AuctionSession session = createSession(Instant.now().plus(1, ChronoUnit.HOURS));
      assertFalse(session.transitionStatus(SessionStatus.UPCOMING, SessionStatus.SOLD));
      assertEquals(
          SessionStatus.UPCOMING,
          session.getStatus(),
          "Transition không hợp lệ phải giữ status cũ");
    }

    @Test
    @DisplayName("SOLD → bất kỳ trạng thái nào đều KHÔNG hợp lệ")
    void soldKhongTransitionDuoc() throws Exception {
      AuctionSession session = createSession(Instant.now().plus(1, ChronoUnit.HOURS));
      setStatus(session, SessionStatus.SOLD);

      assertFalse(session.transitionStatus(SessionStatus.SOLD, SessionStatus.ACTIVE));
      assertFalse(session.transitionStatus(SessionStatus.SOLD, SessionStatus.CANCELLED));
      assertEquals(SessionStatus.SOLD, session.getStatus());
    }
  }

  @Nested
  @DisplayName("Raise current price")
  class RaisePrice {

    @Test
    @DisplayName("Status không ACTIVE phải ném IllegalStateException")
    void priceUpdateKhiKhongActive() {
      AuctionSession session = createSession(Instant.now().plus(1, ChronoUnit.HOURS));
      assertThrows(
          IllegalStateException.class, () -> session.raiseCurrentPrice(new BigDecimal("200000")));
    }

    @Test
    @DisplayName("Status ACTIVE + giá mới cao hơn phải pass")
    void priceUpdateActiveGiaCaoHonPass() throws Exception {
      AuctionSession session = createSession(Instant.now().plus(1, ChronoUnit.HOURS));
      setStatus(session, SessionStatus.ACTIVE);

      session.raiseCurrentPrice(new BigDecimal("200000"));
      assertEquals(0, session.getCurrentPrice().compareTo(new BigDecimal("200000")));
    }

    @Test
    @DisplayName("Giá mới <= giá hiện tại phải ném exception")
    void priceUpdateGiaThapPhaiNem() throws Exception {
      AuctionSession session = createSession(Instant.now().plus(1, ChronoUnit.HOURS));
      setStatus(session, SessionStatus.ACTIVE);

      assertThrows(
          IllegalStateException.class, () -> session.raiseCurrentPrice(new BigDecimal("100000")));
      assertThrows(
          IllegalStateException.class, () -> session.raiseCurrentPrice(new BigDecimal("50000")));
    }

    @Test
    @DisplayName("Giá mới null phải ném exception")
    void priceUpdateNullPhaiNem() throws Exception {
      AuctionSession session = createSession(Instant.now().plus(1, ChronoUnit.HOURS));
      setStatus(session, SessionStatus.ACTIVE);

      assertThrows(IllegalStateException.class, () -> session.raiseCurrentPrice(null));
    }
  }

  @Test
  @DisplayName("incrementBidCount tăng bid count mỗi lần")
  void incrementBidCountTang() {
    AuctionSession session = createSession(Instant.now().plus(1, ChronoUnit.HOURS));
    assertEquals(0, session.getBidCount());

    session.incrementBidCount();
    session.incrementBidCount();
    session.incrementBidCount();

    assertEquals(3, session.getBidCount());
  }

  @Test
  @DisplayName("setHighestBidderId hợp lệ phải cập nhật")
  void setHighestBidderIdHopLe() {
    AuctionSession session = createSession(Instant.now().plus(1, ChronoUnit.HOURS));
    session.setHighestBidderId(42);
    assertEquals(42, session.getHighestBidderId());
  }

  @Test
  @DisplayName("setHighestBidderId <= 0 phải ném exception")
  void setHighestBidderIdInvalidPhaiNem() {
    AuctionSession session = createSession(Instant.now().plus(1, ChronoUnit.HOURS));
    assertThrows(IllegalArgumentException.class, () -> session.setHighestBidderId(0));
    assertThrows(IllegalArgumentException.class, () -> session.setHighestBidderId(-1));
  }
}