package vn.edu.vnu.uet.group8.server.service.auction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test cho phần PURE LOGIC của {@link BidValidator}.
 *
 * <p>Tập trung vào method {@code calculateMinNextBid()} - không phụ thuộc DAO.
 *
 * <h3>Công thức nghiệp vụ:</h3>
 * <pre>
 *   minNextBid = currentPrice + max(currentPrice * 1%, 1000 VND)
 * </pre>
 *
 * <h3>2 cases chính:</h3>
 * <ul>
 *   <li>Giá &gt;= 100,000: increment = 1% của giá (vì 1% &gt;= 1000)</li>
 *   <li>Giá &lt; 100,000: increment = 1000 VND tuyệt đối</li>
 * </ul>
 *
 * <p>Lưu ý: method là package-private nên cùng package mới gọi được.
 * Phần test full {@code validate()} cần Mockito - sẽ làm ở đợt sau.
 */
class BidValidatorTest {

    /**
     * Helper gọi method package-private calculateMinNextBid qua reflection
     * để code test gọn hơn (vì chỉ test 1 method, không cần khởi tạo BidValidator).
     */
    private BigDecimal callCalculateMinNextBid(BigDecimal currentPrice) throws Exception {
        Method method = BidValidator.class.getDeclaredMethod("calculateMinNextBid", BigDecimal.class);
        method.setAccessible(true);
        return (BigDecimal) method.invoke(null, currentPrice);
    }

    // ════════════════════════════════════════════════════
    // CASE 1: Giá lớn - increment 1%
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("Giá 1 triệu - increment 1% = 10.000 → minNext = 1.010.000")
    void giaTrieuIncrement1Phan() throws Exception {
        BigDecimal result = callCalculateMinNextBid(new BigDecimal("1000000"));
        assertEquals(0, result.compareTo(new BigDecimal("1010000")),
                "Min next bid của 1 triệu phải là 1.010.000");
    }

    @Test
    @DisplayName("Giá 100 triệu - increment 1% = 1.000.000 → minNext = 101.000.000")
    void gia100TrieuIncrement1Trieu() throws Exception {
        BigDecimal result = callCalculateMinNextBid(new BigDecimal("100000000"));
        assertEquals(0, result.compareTo(new BigDecimal("101000000")));
    }

    @Test
    @DisplayName("Giá 1 tỷ - increment 1% = 10 triệu → minNext = 1.010.000.000")
    void gia1TyIncrement10Trieu() throws Exception {
        BigDecimal result = callCalculateMinNextBid(new BigDecimal("1000000000"));
        assertEquals(0, result.compareTo(new BigDecimal("1010000000")));
    }

    // ════════════════════════════════════════════════════
    // CASE 2: Giá nhỏ - increment 1000 (tuyệt đối)
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("Giá 50.000 - 1% chỉ 500 → dùng 1000 → minNext = 51.000")
    void giaNhoIncrement1000() throws Exception {
        BigDecimal result = callCalculateMinNextBid(new BigDecimal("50000"));
        assertEquals(0, result.compareTo(new BigDecimal("51000")),
                "Giá nhỏ hơn 100k thì increment phải là 1000 (không phải 1%)");
    }

    @Test
    @DisplayName("Giá 99.999 (vẫn dưới ngưỡng 100k) → increment = 1000")
    void giaDuoiNguong1000() throws Exception {
        BigDecimal result = callCalculateMinNextBid(new BigDecimal("99999"));
        // 1% của 99999 = 999.99 < 1000 → dùng 1000
        assertEquals(0, result.compareTo(new BigDecimal("100999")));
    }

    // ════════════════════════════════════════════════════
    // CASE 3: Boundary - đúng 100.000
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("Giá 100.000 - 1% = 1000 (đúng ngưỡng) → minNext = 101.000")
    void giaDungNguongIncrement1000() throws Exception {
        BigDecimal result = callCalculateMinNextBid(new BigDecimal("100000"));
        // 1% của 100000 = 1000 = MIN_BID_INCREMENT_ABS → dùng cái nào cũng được
        assertEquals(0, result.compareTo(new BigDecimal("101000")));
    }

    // ════════════════════════════════════════════════════
    // CASE 4: Edge cases
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("Giá 0 → increment = 1000 (1% của 0 = 0, dùng 1000) → minNext = 1000")
    void giaZeroIncrement1000() throws Exception {
        BigDecimal result = callCalculateMinNextBid(BigDecimal.ZERO);
        assertEquals(0, result.compareTo(new BigDecimal("1000")));
    }

    @Test
    @DisplayName("Giá có số lẻ: 1.234.567 → 1% = 12.345,67 → minNext = 1.246.912,67")
    void giaCoSoLeIncrement1Phan() throws Exception {
        BigDecimal result = callCalculateMinNextBid(new BigDecimal("1234567"));
        // 1% = 12345.67 > 1000 → dùng 1%
        assertEquals(0, result.compareTo(new BigDecimal("1246912.67")));
    }

    // ════════════════════════════════════════════════════
    // PARAMETERIZED - bảng dữ liệu
    // ════════════════════════════════════════════════════

    @ParameterizedTest(name = "currentPrice={0} → minNextBid={1}")
    @CsvSource({
            // currentPrice, expectedMinNextBid
            "10000,      11000",       // < 100k → +1000
            "50000,      51000",       // < 100k → +1000
            "100000,     101000",      // boundary → +1000
            "200000,     202000",      // > 100k → +1% = 2000
            "500000,     505000",      // > 100k → +1% = 5000
            "1000000,    1010000",     // 1 triệu → +10000
            "10000000,   10100000",    // 10 triệu → +100000
            "100000000,  101000000"    // 100 triệu → +1 triệu
    })
    @DisplayName("Bảng test các mức giá → min next bid tương ứng")
    void bangTestMinNextBid(String price, String expected) throws Exception {
        BigDecimal result = callCalculateMinNextBid(new BigDecimal(price));
        assertEquals(0, result.compareTo(new BigDecimal(expected)),
                "Với giá " + price + " min next phải là " + expected
                        + " nhưng tính ra " + result);
    }

    // ════════════════════════════════════════════════════
    // PROPERTIES - bất biến nghiệp vụ
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("Min next bid luôn lớn hơn current price")
    void minNextLuonLonHonCurrent() throws Exception {
        BigDecimal[] testPrices = {
                new BigDecimal("0"),
                new BigDecimal("1000"),
                new BigDecimal("99999"),
                new BigDecimal("100000"),
                new BigDecimal("999999999")
        };
        for (BigDecimal price : testPrices) {
            BigDecimal next = callCalculateMinNextBid(price);
            assertTrue(next.compareTo(price) > 0,
                    "Min next " + next + " phải > current " + price);
        }
    }

    @Test
    @DisplayName("Increment tối thiểu luôn >= 1000")
    void incrementToiThieuLonHon1000() throws Exception {
        BigDecimal[] testPrices = {
                BigDecimal.ZERO,
                new BigDecimal("1"),
                new BigDecimal("100"),
                new BigDecimal("99999")
        };
        for (BigDecimal price : testPrices) {
            BigDecimal next = callCalculateMinNextBid(price);
            BigDecimal increment = next.subtract(price);
            assertTrue(increment.compareTo(new BigDecimal("1000")) >= 0,
                    "Increment cho giá " + price + " phải >= 1000, thực tế: " + increment);
        }
    }
}