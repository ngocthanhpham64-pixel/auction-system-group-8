package vn.edu.vnu.uet.group8.server.service.auction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import vn.edu.vnu.uet.group8.server.service.auction.AntiSnipingService.AntiSnipingResult;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test cho inner class {@link AntiSnipingResult}.
 *
 * <p>Test factory methods + immutable properties của result object.
 *
 * <h3>2 trạng thái kết quả:</h3>
 * <ul>
 *   <li>{@code notExtended}: không gia hạn - {@code isExtended() == false}</li>
 *   <li>{@code extended}: có gia hạn - {@code isExtended() == true}</li>
 * </ul>
 *
 * <p>Vì 2 factory method là package-private nên dùng reflection để gọi.
 * Đây là pattern test inner class - giữ encapsulation của production code.
 */
class AntiSnipingResultTest {

    // Helper: gọi static method package-private qua reflection
    private AntiSnipingResult callNotExtended(Instant currentEndTime) throws Exception {
        Method method = AntiSnipingResult.class.getDeclaredMethod("notExtended", Instant.class);
        method.setAccessible(true);
        return (AntiSnipingResult) method.invoke(null, currentEndTime);
    }

    private AntiSnipingResult callExtended(Instant newEndTime, Instant prevEndTime) throws Exception {
        Method method = AntiSnipingResult.class.getDeclaredMethod("extended", Instant.class, Instant.class);
        method.setAccessible(true);
        return (AntiSnipingResult) method.invoke(null, newEndTime, prevEndTime);
    }

    // ════════════════════════════════════════════════════
    // FACTORY: notExtended
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Factory notExtended")
    class FactoryNotExtended {

        @Test
        @DisplayName("notExtended phải tạo result với isExtended = false")
        void notExtendedPhaiCoIsExtendedFalse() throws Exception {
            Instant now = Instant.now();
            AntiSnipingResult result = callNotExtended(now);
            assertFalse(result.isExtended(),
                    "notExtended factory phải set isExtended = false");
        }

        @Test
        @DisplayName("notExtended phải giữ nguyên endTime truyền vào")
        void notExtendedPhaiGiuNguyenEndTime() throws Exception {
            Instant currentEndTime = Instant.parse("2026-12-31T23:59:59Z");
            AntiSnipingResult result = callNotExtended(currentEndTime);
            assertEquals(currentEndTime, result.getNewEndTime(),
                    "Khi không gia hạn, newEndTime phải = endTime hiện tại");
        }
    }

    // ════════════════════════════════════════════════════
    // FACTORY: extended
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Factory extended")
    class FactoryExtended {

        @Test
        @DisplayName("extended phải tạo result với isExtended = true")
        void extendedPhaiCoIsExtendedTrue() throws Exception {
            Instant prev = Instant.now();
            Instant next = prev.plus(5, ChronoUnit.MINUTES);
            AntiSnipingResult result = callExtended(next, prev);
            assertTrue(result.isExtended(),
                    "extended factory phải set isExtended = true");
        }

        @Test
        @DisplayName("extended phải lưu newEndTime đã gia hạn")
        void extendedPhaiLuuNewEndTime() throws Exception {
            Instant prev = Instant.parse("2026-12-31T20:00:00Z");
            Instant next = Instant.parse("2026-12-31T20:05:00Z");
            AntiSnipingResult result = callExtended(next, prev);
            assertEquals(next, result.getNewEndTime());
        }

        @Test
        @DisplayName("newEndTime sau gia hạn phải sau previousEndTime")
        void newEndTimePhaiSauPrev() throws Exception {
            Instant prev = Instant.now();
            Instant next = prev.plus(5, ChronoUnit.MINUTES);
            AntiSnipingResult result = callExtended(next, prev);
            assertTrue(result.getNewEndTime().isAfter(prev),
                    "Sau gia hạn newEndTime phải > previousEndTime");
        }
    }

    // ════════════════════════════════════════════════════
    // TOSTRING
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("toString format")
    class ToStringFormat {

        @Test
        @DisplayName("toString của notExtended phải chứa 'extended=false'")
        void toStringNotExtendedChuaFalse() throws Exception {
            AntiSnipingResult result = callNotExtended(Instant.now());
            String str = result.toString();
            assertNotNull(str);
            assertTrue(str.contains("extended=false"),
                    "toString phải hiển thị extended=false. Thực tế: " + str);
        }

        @Test
        @DisplayName("toString của extended phải chứa 'extended=true'")
        void toStringExtendedChuaTrue() throws Exception {
            Instant prev = Instant.now();
            AntiSnipingResult result = callExtended(prev.plus(5, ChronoUnit.MINUTES), prev);
            String str = result.toString();
            assertTrue(str.contains("extended=true"));
        }

        @Test
        @DisplayName("toString phải chứa newEndTime")
        void toStringChuaNewEndTime() throws Exception {
            Instant endTime = Instant.parse("2026-12-31T23:59:59Z");
            AntiSnipingResult result = callNotExtended(endTime);
            String str = result.toString();
            assertTrue(str.contains("newEndTime"),
                    "toString phải có thông tin newEndTime. Thực tế: " + str);
        }
    }

    // ════════════════════════════════════════════════════
    // IMMUTABILITY
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("AntiSnipingResult là immutable - getter trả về giá trị nhất quán")
    void resultImmutable() throws Exception {
        Instant endTime = Instant.now();
        AntiSnipingResult result = callNotExtended(endTime);

        // Gọi getter nhiều lần phải trả cùng giá trị
        boolean ext1 = result.isExtended();
        boolean ext2 = result.isExtended();
        Instant time1 = result.getNewEndTime();
        Instant time2 = result.getNewEndTime();

        assertEquals(ext1, ext2, "isExtended phải nhất quán qua nhiều lần gọi");
        assertEquals(time1, time2, "getNewEndTime phải nhất quán qua nhiều lần gọi");
    }
}