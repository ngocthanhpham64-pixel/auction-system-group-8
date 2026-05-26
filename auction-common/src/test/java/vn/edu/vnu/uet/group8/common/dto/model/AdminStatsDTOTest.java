package vn.edu.vnu.uet.group8.common.dto.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test toàn diện cho {@link AdminStatsDTO}.
 *
 * <p>Phạm vi kiểm thử:
 * <ul>
 *   <li><b>Constructor đầy đủ</b>: truyền tất cả tham số</li>
 *   <li><b>No-arg constructor</b>: Gson deserialization</li>
 *   <li><b>empty() factory</b>: giá trị mặc định hợp lệ</li>
 *   <li><b>getTotalRevenue() null-safe</b>: trả ZERO khi null</li>
 *   <li><b>Setters</b>: cập nhật từng field</li>
 *   <li><b>equals() và hashCode()</b>: đúng theo logic nghiệp vụ</li>
 *   <li><b>toString()</b>: chứa thông tin đủ để debug</li>
 *   <li><b>Edge cases</b>: giá trị âm, rất lớn, zero</li>
 * </ul>
 */
@DisplayName("AdminStatsDTO - Unit Tests")
class AdminStatsDTOTest {

    // ═══════════════════════════════════════════════════
    // CONSTRUCTORS
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Constructors")
    class Constructors {

        @Test
        @DisplayName("Constructor đầy đủ 4 tham số - map đúng tất cả field")
        void constructorDayDu() {
            BigDecimal revenue = new BigDecimal("150000000");
            AdminStatsDTO dto = new AdminStatsDTO(12, 500, revenue, 1800);

            assertEquals(12, dto.getActiveAuctions());
            assertEquals(500, dto.getTotalUsers());
            assertEquals(0, dto.getTotalRevenue().compareTo(revenue));
            assertEquals(1800, dto.getTotalBids());
        }

        @Test
        @DisplayName("No-arg constructor (Gson) - tất cả field là giá trị mặc định Java")
        void noArgConstructor() {
            AdminStatsDTO dto = new AdminStatsDTO();

            assertEquals(0, dto.getActiveAuctions());
            assertEquals(0, dto.getTotalUsers());
            assertEquals(0, dto.getTotalBids());
            // totalRevenue = null qua no-arg, nhưng getter null-safe trả ZERO
            assertEquals(0, dto.getTotalRevenue().compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Constructor với revenue = null - getTotalRevenue() trả ZERO (null-safe)")
        void constructorRevenueNull() {
            AdminStatsDTO dto = new AdminStatsDTO(0, 0, null, 0);
            assertNotNull(dto.getTotalRevenue(), "getTotalRevenue() không được trả null");
            assertEquals(0, dto.getTotalRevenue().compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Constructor với revenue = 0 - getTotalRevenue() trả ZERO")
        void constructorRevenueZero() {
            AdminStatsDTO dto = new AdminStatsDTO(0, 0, BigDecimal.ZERO, 0);
            assertEquals(0, dto.getTotalRevenue().compareTo(BigDecimal.ZERO));
        }
    }

    // ═══════════════════════════════════════════════════
    // empty() FACTORY
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("empty() factory method")
    class EmptyFactory {

        @Test
        @DisplayName("empty() trả về object không null")
        void emptyKhongNull() {
            assertNotNull(AdminStatsDTO.empty());
        }

        @Test
        @DisplayName("empty() - activeAuctions = 0")
        void emptyActiveAuctionsZero() {
            assertEquals(0, AdminStatsDTO.empty().getActiveAuctions());
        }

        @Test
        @DisplayName("empty() - totalUsers = 0")
        void emptyTotalUsersZero() {
            assertEquals(0, AdminStatsDTO.empty().getTotalUsers());
        }

        @Test
        @DisplayName("empty() - totalRevenue = ZERO (không null)")
        void emptyTotalRevenueZero() {
            BigDecimal revenue = AdminStatsDTO.empty().getTotalRevenue();
            assertNotNull(revenue);
            assertEquals(0, revenue.compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("empty() - totalBids = 0")
        void emptyTotalBidsZero() {
            assertEquals(0, AdminStatsDTO.empty().getTotalBids());
        }

        @Test
        @DisplayName("Mỗi lần gọi empty() trả về instance mới (không shared)")
        void emptyTraInstanceMoi() {
            AdminStatsDTO a = AdminStatsDTO.empty();
            AdminStatsDTO b = AdminStatsDTO.empty();
            assertNotSame(a, b);
        }
    }

    // ═══════════════════════════════════════════════════
    // getTotalRevenue() NULL-SAFE
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("getTotalRevenue() - null-safe behavior")
    class TotalRevenueNullSafe {

        @Test
        @DisplayName("Khi totalRevenue được set null qua setter → getter trả ZERO")
        void setterNullGiaTri() {
            AdminStatsDTO dto = new AdminStatsDTO(1, 10, new BigDecimal("1000"), 5);
            dto.setTotalRevenue(null);
            assertEquals(0, dto.getTotalRevenue().compareTo(BigDecimal.ZERO),
                    "Setter null → getter phải trả ZERO");
        }

        @Test
        @DisplayName("Revenue hợp lệ - getter trả đúng giá trị (không bị thay bằng ZERO)")
        void revenueHopLeKhongBiThay() {
            BigDecimal expected = new BigDecimal("9999999.99");
            AdminStatsDTO dto = new AdminStatsDTO(0, 0, expected, 0);
            assertEquals(0, dto.getTotalRevenue().compareTo(expected));
        }
    }

    // ═══════════════════════════════════════════════════
    // SETTERS
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Setters - cập nhật từng field")
    class Setters {

        @Test
        @DisplayName("setActiveAuctions() cập nhật đúng")
        void setActiveAuctions() {
            AdminStatsDTO dto = AdminStatsDTO.empty();
            dto.setActiveAuctions(25);
            assertEquals(25, dto.getActiveAuctions());
        }

        @Test
        @DisplayName("setTotalUsers() cập nhật đúng")
        void setTotalUsers() {
            AdminStatsDTO dto = AdminStatsDTO.empty();
            dto.setTotalUsers(1500);
            assertEquals(1500, dto.getTotalUsers());
        }

        @Test
        @DisplayName("setTotalRevenue() với giá trị hợp lệ")
        void setTotalRevenue() {
            AdminStatsDTO dto = AdminStatsDTO.empty();
            BigDecimal newRevenue = new BigDecimal("500000000");
            dto.setTotalRevenue(newRevenue);
            assertEquals(0, dto.getTotalRevenue().compareTo(newRevenue));
        }

        @Test
        @DisplayName("setTotalBids() cập nhật đúng")
        void setTotalBids() {
            AdminStatsDTO dto = AdminStatsDTO.empty();
            dto.setTotalBids(9999);
            assertEquals(9999, dto.getTotalBids());
        }

        @Test
        @DisplayName("Setter chain - set tất cả field liên tiếp")
        void setterChain() {
            AdminStatsDTO dto = new AdminStatsDTO();
            dto.setActiveAuctions(5);
            dto.setTotalUsers(200);
            dto.setTotalRevenue(new BigDecimal("1000000"));
            dto.setTotalBids(300);

            assertEquals(5, dto.getActiveAuctions());
            assertEquals(200, dto.getTotalUsers());
            assertEquals(0, dto.getTotalRevenue().compareTo(new BigDecimal("1000000")));
            assertEquals(300, dto.getTotalBids());
        }

        @Test
        @DisplayName("setActiveAuctions nhiều lần - giữ giá trị cuối")
        void setterNhieuLan() {
            AdminStatsDTO dto = AdminStatsDTO.empty();
            dto.setActiveAuctions(1);
            dto.setActiveAuctions(5);
            dto.setActiveAuctions(10);
            assertEquals(10, dto.getActiveAuctions());
        }
    }

    // ═══════════════════════════════════════════════════
    // equals() & hashCode()
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("equals() và hashCode()")
    class EqualsHashCode {

        @Test
        @DisplayName("Hai DTO cùng giá trị → equals")
        void cungGiaTriEquals() {
            BigDecimal rev = new BigDecimal("1000000");
            AdminStatsDTO a = new AdminStatsDTO(5, 100, rev, 50);
            AdminStatsDTO b = new AdminStatsDTO(5, 100, rev, 50);

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("Hai DTO khác activeAuctions → không equals")
        void khacActiveAuctionsNotEquals() {
            AdminStatsDTO a = new AdminStatsDTO(5, 100, BigDecimal.TEN, 50);
            AdminStatsDTO b = new AdminStatsDTO(6, 100, BigDecimal.TEN, 50);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("Hai DTO khác totalUsers → không equals")
        void khacTotalUsersNotEquals() {
            AdminStatsDTO a = new AdminStatsDTO(5, 100, BigDecimal.TEN, 50);
            AdminStatsDTO b = new AdminStatsDTO(5, 200, BigDecimal.TEN, 50);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("Hai DTO khác totalRevenue → không equals")
        void khacRevenueNotEquals() {
            AdminStatsDTO a = new AdminStatsDTO(5, 100, new BigDecimal("100"), 50);
            AdminStatsDTO b = new AdminStatsDTO(5, 100, new BigDecimal("200"), 50);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("Hai DTO khác totalBids → không equals")
        void khacTotalBidsNotEquals() {
            AdminStatsDTO a = new AdminStatsDTO(5, 100, BigDecimal.TEN, 50);
            AdminStatsDTO b = new AdminStatsDTO(5, 100, BigDecimal.TEN, 99);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("equals(null) → false")
        void equalsNull() {
            assertNotEquals(AdminStatsDTO.empty(), null);
        }

        @Test
        @DisplayName("equals(chính nó) → true (reflexive)")
        void equalsChinhNo() {
            AdminStatsDTO dto = AdminStatsDTO.empty();
            assertEquals(dto, dto);
        }

        @Test
        @DisplayName("equals khác kiểu → false")
        void equalsKhacKieu() {
            AdminStatsDTO dto = AdminStatsDTO.empty();
            assertNotEquals(dto, "AdminStatsDTO");
            assertNotEquals(dto, 0);
        }

        @Test
        @DisplayName("revenue null vs ZERO - equals nhờ null-safe getter")
        void revenueNullVsZeroEquals() {
            AdminStatsDTO withNull = new AdminStatsDTO(0, 0, null, 0);
            AdminStatsDTO withZero = new AdminStatsDTO(0, 0, BigDecimal.ZERO, 0);
            // Cả hai getTotalRevenue() đều trả ZERO → equals
            assertEquals(withNull, withZero,
                    "DTO với revenue=null và revenue=ZERO phải equals nhờ null-safe getter");
        }
    }

    // ═══════════════════════════════════════════════════
    // toString()
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("toString()")
    class ToStringTest {

        @Test
        @DisplayName("toString() không trả null và không blank")
        void toStringKhongNull() {
            AdminStatsDTO dto = new AdminStatsDTO(3, 100, new BigDecimal("500000"), 50);
            String s = dto.toString();
            assertNotNull(s);
            assertFalse(s.isBlank());
        }

        @Test
        @DisplayName("toString() chứa activeAuctions")
        void toStringChuaActiveAuctions() {
            AdminStatsDTO dto = new AdminStatsDTO(7, 0, BigDecimal.ZERO, 0);
            assertTrue(dto.toString().contains("7"),
                    "toString phải chứa giá trị activeAuctions");
        }

        @Test
        @DisplayName("toString() chứa totalUsers")
        void toStringChuaTotalUsers() {
            AdminStatsDTO dto = new AdminStatsDTO(0, 999, BigDecimal.ZERO, 0);
            assertTrue(dto.toString().contains("999"));
        }

        @Test
        @DisplayName("toString() chứa totalBids")
        void toStringChuaTotalBids() {
            AdminStatsDTO dto = new AdminStatsDTO(0, 0, BigDecimal.ZERO, 888);
            assertTrue(dto.toString().contains("888"));
        }

        @Test
        @DisplayName("toString() chứa totalRevenue")
        void toStringChuaRevenue() {
            AdminStatsDTO dto = new AdminStatsDTO(0, 0, new BigDecimal("12345"), 0);
            assertTrue(dto.toString().contains("12345"));
        }
    }

    // ═══════════════════════════════════════════════════
    // EDGE CASES
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge cases - giá trị biên")
    class EdgeCases {

        @Test
        @DisplayName("Giá trị Integer.MAX_VALUE - không bị overflow")
        void intMaxValue() {
            AdminStatsDTO dto = new AdminStatsDTO(
                    Integer.MAX_VALUE, Integer.MAX_VALUE,
                    new BigDecimal("9999999999999"), Integer.MAX_VALUE);

            assertEquals(Integer.MAX_VALUE, dto.getActiveAuctions());
            assertEquals(Integer.MAX_VALUE, dto.getTotalUsers());
            assertEquals(Integer.MAX_VALUE, dto.getTotalBids());
        }

        @Test
        @DisplayName("activeAuctions = 0 - edge case khi không có phiên nào")
        void activeAuctionsZero() {
            AdminStatsDTO dto = new AdminStatsDTO(0, 1000, new BigDecimal("5000000"), 100);
            assertEquals(0, dto.getActiveAuctions());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 100, 10000})
        @DisplayName("Nhiều giá trị totalBids hợp lệ")
        void totalBidsNhieuGiaTri(int bids) {
            AdminStatsDTO dto = new AdminStatsDTO(0, 0, BigDecimal.ZERO, bids);
            assertEquals(bids, dto.getTotalBids());
        }

        @Test
        @DisplayName("Revenue với nhiều chữ số thập phân - không mất độ chính xác")
        void revenueThanSoThapPhan() {
            BigDecimal precise = new BigDecimal("123456789.99");
            AdminStatsDTO dto = new AdminStatsDTO(0, 0, precise, 0);
            assertEquals(0, dto.getTotalRevenue().compareTo(precise));
        }

        @Test
        @DisplayName("empty() rồi set lại tất cả - giống constructor đầy đủ")
        void emptyRoiSetLai() {
            AdminStatsDTO fromEmpty = AdminStatsDTO.empty();
            fromEmpty.setActiveAuctions(10);
            fromEmpty.setTotalUsers(500);
            fromEmpty.setTotalRevenue(new BigDecimal("1000000"));
            fromEmpty.setTotalBids(200);

            AdminStatsDTO fromConstructor = new AdminStatsDTO(10, 500,
                    new BigDecimal("1000000"), 200);

            assertEquals(fromConstructor, fromEmpty);
        }
    }
}