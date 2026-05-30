package vn.edu.vnu.uet.group8.server.service.auction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;

/**
 * Test cho {@link AntiSnipingService}.
 *
 * <p>Phạm vi:
 * <ul>
 *   <li>Bid trong cửa sổ 5 phút cuối → gia hạn +5 phút
 *   <li>Bid ngoài cửa sổ → KHÔNG gia hạn, trả endTime nguyên
 *   <li>Lỗi DB khi update → propagate SQLException
 *   <li>AntiSnipingResult factory methods + getter
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AntiSnipingServiceTest {

    @Mock
    private AuctionSessionDAO sessionDAO;

    @InjectMocks
    private AntiSnipingService service;

    /** Helper - tạo session với endTime cụ thể. */
    private AuctionSession taoSession(Instant endTime) {
        AuctionSession s = new AuctionSession.Builder(99,
                new BigDecimal("100"),
                Instant.now().minusSeconds(3600),
                endTime)
                .build();
        s.assignId(1);
        return s;
    }

    @Nested
    @DisplayName("checkAndExtend - gia hạn khi sniping")
    class GiaHan {

        @Test
        @DisplayName("Bid trong 5 phút cuối → gia hạn +5 phút, update DB")
        void biCanThiep() throws SQLException {
            // endTime cách hiện tại 2 phút → đang trong sniping window
            Instant endTimeCu = Instant.now().plus(2, ChronoUnit.MINUTES);
            AuctionSession session = taoSession(endTimeCu);

            AntiSnipingService.AntiSnipingResult result = service.checkAndExtend(session);

            assertTrue(result.isExtended(), "Bid sát giờ kết thúc → phải gia hạn");
            assertEquals(endTimeCu.plus(5, ChronoUnit.MINUTES), result.getNewEndTime(),
                    "endTime mới = endTime cũ + 5 phút");

            verify(sessionDAO).updateEndTime(eq(1), eq(endTimeCu.plus(5, ChronoUnit.MINUTES)));
        }

        @Test
        @DisplayName("Bid đúng thời điểm kết thúc")
        void bidNgayEndTime() throws SQLException {

            Instant endTimeCu =
                    Instant.now().plusSeconds(1);

            AuctionSession session =
                    taoSession(endTimeCu);

            AntiSnipingService.AntiSnipingResult result =
                    service.checkAndExtend(session);

            assertTrue(result.isExtended());
        }

    @Nested
    @DisplayName("checkAndExtend - không gia hạn")
    class KhongGiaHan {

        @Test
        @DisplayName("Bid khi còn 1 tiếng → KHÔNG gia hạn, KHÔNG gọi DB")
        void conNhieuThoiGian() throws SQLException {
            Instant endTimeCu = Instant.now().plus(1, ChronoUnit.HOURS);
            AuctionSession session = taoSession(endTimeCu);

            AntiSnipingService.AntiSnipingResult result = service.checkAndExtend(session);

            assertFalse(result.isExtended(), "Còn 1 tiếng → không gia hạn");
            assertEquals(endTimeCu, result.getNewEndTime(),
                    "endTime giữ nguyên khi không gia hạn");

            // KHÔNG gọi update DB
            verify(sessionDAO, never()).updateEndTime(anyInt(), any());
        }

        @Test
        @DisplayName("Bid khi còn 10 phút (ngoài cửa sổ 5 phút) → KHÔNG gia hạn")
        void con10Phut() throws SQLException {
            Instant endTimeCu = Instant.now().plus(10, ChronoUnit.MINUTES);
            AuctionSession session = taoSession(endTimeCu);

            AntiSnipingService.AntiSnipingResult result = service.checkAndExtend(session);
            assertFalse(result.isExtended());
        }
    }

    @Nested
    @DisplayName("Resilience - DB error")
    class DbError {

        @Test
        @DisplayName("DAO update ném SQLException → propagate")
        void daoLoi() throws SQLException {
            Instant endTimeCu = Instant.now().plus(2, ChronoUnit.MINUTES);
            AuctionSession session = taoSession(endTimeCu);

            doThrow(new SQLException("DB lỗi"))
                    .when(sessionDAO).updateEndTime(anyInt(), any());

            assertThrows(SQLException.class, () -> service.checkAndExtend(session));
        }
    }

    @Nested
    @DisplayName("AntiSnipingResult - static factory")
    class ResultFactory {

        @Test
        @DisplayName("notExtended() - isExtended = false, newEndTime giữ nguyên")
        void notExtended() {
            Instant t = Instant.now();
            AntiSnipingService.AntiSnipingResult r =
                    AntiSnipingService.AntiSnipingResult.notExtended(t);
            assertFalse(r.isExtended());
            assertEquals(t, r.getNewEndTime());
        }

        @Test
        @DisplayName("extended() - isExtended = true, newEndTime mới")
        void extended() {
            Instant old = Instant.now();
            Instant newEnd = old.plus(5, ChronoUnit.MINUTES);
            AntiSnipingService.AntiSnipingResult r =
                    AntiSnipingService.AntiSnipingResult.extended(newEnd, old);
            assertTrue(r.isExtended());
            assertEquals(newEnd, r.getNewEndTime());
        }
    }
}}