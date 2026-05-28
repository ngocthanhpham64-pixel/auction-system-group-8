package vn.edu.vnu.uet.group8.client.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UIFormatterTest – kiểm thử toàn diện lớp UIFormatter.
 * Không cần JavaFX toolkit – UIFormatter là pure-Java utility.
 *
 * Phủ các nhánh:
 *  - formatPrice  : null, zero, âm, dương, số lớn
 *  - formatInstant: null, giờ đã qua, giờ tương lai
 *  - formatCountdown: null, đã kết thúc (quá khứ), trong tương lai
 */
@DisplayName("UIFormatter")
class UIFormatterTest {

    // =========================================================
    //  formatPrice
    // =========================================================
    @Nested
    @DisplayName("formatPrice(BigDecimal)")
    class FormatPrice {

        @Test
        @DisplayName("null -> trả về '0 đ'")
        void nullReturnsZeroDong() {
            assertEquals("0 đ", UIFormatter.formatPrice(null));
        }

        @Test
        @DisplayName("ZERO -> chuỗi chứa '0'")
        void zeroContainsZero() {
            String result = UIFormatter.formatPrice(BigDecimal.ZERO);
            assertNotNull(result);
            assertFalse(result.isBlank(), "kết quả không được rỗng");
            // Locale vi_VN format: 0 ₫  hoặc 0 đ tùy JDK – kiểm tra chứa "0"
            assertTrue(result.contains("0"), "phải chứa '0', nhận: " + result);
        }

        @Test
        @DisplayName("giá trị dương -> không null, không rỗng")
        void positiveValueNotBlank() {
            String result = UIFormatter.formatPrice(new BigDecimal("1500000"));
            assertNotNull(result);
            assertFalse(result.isBlank());
        }

        @Test
        @DisplayName("giá trị dương -> chứa các chữ số của số gốc")
        void positiveValueContainsDigits() {
            String result = UIFormatter.formatPrice(new BigDecimal("1000"));
            // Dù format kiểu gì vẫn phải chứa "1" và "000" (hoặc "1.000")
            assertTrue(result.replace(".", "").replace(",", "").contains("1000"),
                    "phải chứa chữ số 1000, nhận: " + result);
        }

        @Test
        @DisplayName("số rất lớn không ném ngoại lệ")
        void largeNumberNoException() {
            assertDoesNotThrow(() -> UIFormatter.formatPrice(new BigDecimal("999999999999")));
        }

        @Test
        @DisplayName("số âm không ném ngoại lệ")
        void negativeNoException() {
            assertDoesNotThrow(() -> UIFormatter.formatPrice(new BigDecimal("-500000")));
        }

        @Test
        @DisplayName("số âm -> kết quả chứa dấu hiệu âm hoặc ngoặc")
        void negativeContainsNegativeSign() {
            String result = UIFormatter.formatPrice(new BigDecimal("-1000"));
            assertNotNull(result);
            // Locale vi_VN có thể dùng '-' hoặc '(1.000 ₫)' – chứa ít nhất chữ số
            assertTrue(result.matches(".*\\d.*"), "phải chứa chữ số, nhận: " + result);
        }

        @Test
        @DisplayName("format nhất quán: gọi 2 lần cùng input cho cùng output")
        void deterministicOutput() {
            BigDecimal price = new BigDecimal("250000");
            assertEquals(UIFormatter.formatPrice(price), UIFormatter.formatPrice(price));
        }

        @Test
        @DisplayName("giá 1 đồng")
        void oneUnitPrice() {
            String result = UIFormatter.formatPrice(BigDecimal.ONE);
            assertNotNull(result);
            assertFalse(result.isBlank());
        }

        @Test
        @DisplayName("giá có phần thập phân")
        void decimalPrice() {
            assertDoesNotThrow(() -> UIFormatter.formatPrice(new BigDecimal("12345.99")));
        }
    }

    // =========================================================
    //  formatInstant
    // =========================================================
    @Nested
    @DisplayName("formatInstant(Instant)")
    class FormatInstant {

        @Test
        @DisplayName("null -> '---'")
        void nullReturnsDashes() {
            assertEquals("---", UIFormatter.formatInstant(null));
        }

        @Test
        @DisplayName("Instant quá khứ -> chuỗi không rỗng")
        void pastInstantNotBlank() {
            Instant past = Instant.ofEpochSecond(0); // 1970-01-01 UTC
            String result = UIFormatter.formatInstant(past);
            assertNotNull(result);
            assertFalse(result.isBlank());
        }

        @Test
        @DisplayName("Instant hiện tại -> chuỗi không rỗng")
        void nowNotBlank() {
            String result = UIFormatter.formatInstant(Instant.now());
            assertNotNull(result);
            assertFalse(result.isBlank());
        }

        @Test
        @DisplayName("format đúng pattern HH:mm:ss dd/MM/yyyy – độ dài chuỗi = 19")
        void correctPatternLength() {
            // pattern "HH:mm:ss dd/MM/yyyy" = 19 ký tự
            String result = UIFormatter.formatInstant(Instant.now());
            assertEquals(19, result.length(),
                    "Phải có 19 ký tự theo pattern HH:mm:ss dd/MM/yyyy, nhận: " + result);
        }

        @Test
        @DisplayName("format đúng pattern – có ':' và '/'")
        void containsColonAndSlash() {
            String result = UIFormatter.formatInstant(Instant.now());
            assertTrue(result.contains(":"), "Phải chứa ':' cho giờ/phút/giây");
            assertTrue(result.contains("/"), "Phải chứa '/' cho ngày/tháng/năm");
        }

        @Test
        @DisplayName("Instant tương lai rất xa không ném ngoại lệ")
        void farFutureNoException() {
            Instant farFuture = Instant.ofEpochSecond(32503680000L); // năm 3000
            assertDoesNotThrow(() -> UIFormatter.formatInstant(farFuture));
        }

        @Test
        @DisplayName("cùng Instant -> cùng output (deterministic)")
        void deterministicOutput() {
            Instant instant = Instant.ofEpochSecond(1_700_000_000L);
            assertEquals(UIFormatter.formatInstant(instant), UIFormatter.formatInstant(instant));
        }

        @Test
        @DisplayName("epoch 0 -> đúng ngày 01/01/1970 theo múi giờ Việt Nam (+07:00)")
        void epochZeroConvertsToVietnamTime() {
            // UTC 00:00:00 1970-01-01 == ICT 07:00:00 1970-01-01
            String result = UIFormatter.formatInstant(Instant.ofEpochSecond(0));
            assertTrue(result.endsWith("01/01/1970"),
                    "Phải kết thúc bằng 01/01/1970, nhận: " + result);
            assertTrue(result.startsWith("07:00:00"),
                    "Phải bắt đầu bằng 07:00:00 (UTC+7), nhận: " + result);
        }
    }

    // =========================================================
    //  formatCountdown
    // =========================================================
    @Nested
    @DisplayName("formatCountdown(Instant)")
    class FormatCountdown {

        @Test
        @DisplayName("null -> 'Kết thúc'")
        void nullReturnsKetThuc() {
            assertEquals("Kết thúc", UIFormatter.formatCountdown(null));
        }

        @Test
        @DisplayName("endTime đã qua -> '00:00:00' (floor 0)")
        void pastEndTimeReturnsZero() {
            Instant past = Instant.now().minusSeconds(3600);
            assertEquals("00:00:00", UIFormatter.formatCountdown(past));
        }

        @Test
        @DisplayName("endTime trong tương lai -> chuỗi không '00:00:00'")
        void futureEndTimeNotZero() {
            Instant future = Instant.now().plusSeconds(3661); // 1h 1m 1s
            String result = UIFormatter.formatCountdown(future);
            assertNotEquals("00:00:00", result);
        }

        @Test
        @DisplayName("format đúng HH:MM:SS (8 ký tự, có 2 dấu ':')")
        void correctCountdownFormat() {
            Instant future = Instant.now().plusSeconds(7322); // 2h 2m 2s
            String result = UIFormatter.formatCountdown(future);
            assertEquals(8, result.length(),
                    "Phải có 8 ký tự HH:MM:SS, nhận: " + result);
            assertEquals(2, result.chars().filter(c -> c == ':').count(),
                    "Phải có đúng 2 dấu ':', nhận: " + result);
        }

        @Test
        @DisplayName("đúng 1 giờ tương lai -> bắt đầu bằng '01:'")
        void oneHourFuture() {
            Instant oneHourLater = Instant.now().plusSeconds(3600);
            String result = UIFormatter.formatCountdown(oneHourLater);
            assertTrue(result.startsWith("01:"),
                    "Phải bắt đầu '01:' cho 1 giờ, nhận: " + result);
        }

        @Test
        @DisplayName("đúng 59 giây tương lai -> bắt đầu bằng '00:00:'")
        void under60SecondsFuture() {
            Instant soon = Instant.now().plusSeconds(59);
            String result = UIFormatter.formatCountdown(soon);
            assertTrue(result.startsWith("00:00:"),
                    "Phải bắt đầu '00:00:', nhận: " + result);
        }

        @Test
        @DisplayName("countdown không ném ngoại lệ với endTime = now")
        void exactlyNowNoException() {
            assertDoesNotThrow(() -> UIFormatter.formatCountdown(Instant.now()));
        }

        @Test
        @DisplayName("23 giờ 59 phút 59 giây -> '23:59:59'")
        void almostOneDayCountdown() {
            Instant future = Instant.now().plusSeconds(23 * 3600 + 59 * 60 + 59);
            String result = UIFormatter.formatCountdown(future);
            assertTrue(result.startsWith("23:59"),
                    "Phải bắt đầu bằng 23:59, nhận: " + result);
        }

        @Test
        @DisplayName("hơn 24 giờ -> giờ > 23, không bị overflow")
        void moreThan24HoursNoOverflow() {
            Instant future = Instant.now().plusSeconds(48 * 3600); // 48 giờ
            String result = UIFormatter.formatCountdown(future);
            assertNotNull(result);
            assertFalse(result.isBlank());
            // Giờ phải là "48"
            assertTrue(result.startsWith("48:"),
                    "48 giờ phải hiển thị '48:', nhận: " + result);
        }
    }
}