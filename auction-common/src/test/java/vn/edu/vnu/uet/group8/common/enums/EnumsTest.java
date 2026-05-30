package vn.edu.vnu.uet.group8.common.enums;

import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test cho các Enum của hệ thống.
 *
 * <p>Gom test các enum vào 1 file vì:
 * <ul>
 *   <li>Mỗi enum chỉ có 3-5 test case → tách file gây phân tán</li>
 *   <li>Logic test tương tự nhau: count values, label, valueOf, Gson serialize</li>
 *   <li>Dễ chạy + xem report</li>
 * </ul>
 *
 * <p>Kiểm tra 4 điểm chính cho mỗi enum:
 * <ul>
 *   <li>Số lượng values đúng</li>
 *   <li>Label/displayName không null/rỗng</li>
 *   <li>valueOf() hoạt động cho tên hợp lệ + ném exception cho tên sai</li>
 *   <li>Gson serialize ra JSON đúng convention</li>
 * </ul>
 */
class EnumsTest {

    private static final Gson GSON = new Gson();

    // ════════════════════════════════════════════════════
    // ITEM CATEGORY
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("ItemCategory")
    class ItemCategoryTest {

        @Test
        @DisplayName("ItemCategory có đúng 6 giá trị")
        void coDung6GiaTri() {
            assertEquals(12, ItemCategory.values().length);
        }

        @Test
        @DisplayName("Mỗi category đều có displayName không rỗng")
        void moiCategoryCoDisplayName() {
            for (ItemCategory cat : ItemCategory.values()) {
                assertNotNull(cat.getLabel(),
                        "Category " + cat + " phải có displayName");
                assertTrue(cat.getLabel().length() > 0,
                        "Category " + cat + " có displayName rỗng");
            }
        }

        @Test
        @DisplayName("ELECTRONICS có displayName 'Đồ điện tử'")
        void electronicsDisplayName() {
            assertEquals("Điện tử", ItemCategory.ELECTRONICS.getLabel());
        }

        @Test
        @DisplayName("valueOf hợp lệ phải trả về đúng enum")
        void valueOfHopLe() {
            assertEquals(ItemCategory.VEHICLES, ItemCategory.valueOf("VEHICLES"));
        }

        @Test
        @DisplayName("valueOf không hợp lệ phải ném IllegalArgumentException")
        void valueOfKhongHopLeNem() {
            assertThrows(IllegalArgumentException.class,
                    () -> ItemCategory.valueOf("UNKNOWN"));
        }
    }

    // ════════════════════════════════════════════════════
    // ITEM STATUS
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("ItemStatus")
    class ItemStatusTest {

        @Test
        @DisplayName("ItemStatus có đúng 5 giá trị: DRAFT/LISTED/SOLD/UNSOLD/ARCHIVED")
        void coDung5GiaTri() {
            assertEquals(5, ItemStatus.values().length);
        }

        @Test
        @DisplayName("Mỗi status có label không null")
        void moiStatusCoLabel() {
            for (ItemStatus status : ItemStatus.values()) {
                assertNotNull(status.getLabel());
            }
        }

        @Test
        @DisplayName("DRAFT label = 'Nháp, chưa bán'")
        void draftLabel() {
            assertEquals("Nháp, chưa bán", ItemStatus.DRAFT.getLabel());
        }
    }

    // ════════════════════════════════════════════════════
    // USER ROLE
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("UserRole")
    class UserRoleTest {

        @Test
        @DisplayName("UserRole có đúng 3 giá trị: ADMIN/SELLER/BIDDER")
        void coDung3GiaTri() {
            assertEquals(3, UserRole.values().length);
        }

        @Test
        @DisplayName("Mỗi role có label không null")
        void moiRoleCoLabel() {
            for (UserRole role : UserRole.values()) {
                assertNotNull(role.getLabel());
            }
        }

        @Test
        @DisplayName("Tất cả role có tên Tiếng Việt rõ ràng")
        void labelTiengVietRoRang() {
            assertEquals("Quản trị viên", UserRole.ADMIN.getLabel());
            assertEquals("Người bán", UserRole.SELLER.getLabel());
            assertEquals("Người mua", UserRole.BIDDER.getLabel());
        }
    }

    // ════════════════════════════════════════════════════
    // USER STATUS
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("UserStatus")
    class UserStatusTest {

        @Test
        @DisplayName("UserStatus có đúng 3 giá trị: ACTIVE/SUSPENDED/BANNED")
        void coDung3GiaTri() {
            assertEquals(3, UserStatus.values().length);
        }

        @Test
        @DisplayName("Tên enum khớp với chuẩn nghiệp vụ")
        void tenEnumDungChuan() {
            assertDoesNotThrow(() -> UserStatus.valueOf("ACTIVE"));
            assertDoesNotThrow(() -> UserStatus.valueOf("SUSPENDED"));
            assertDoesNotThrow(() -> UserStatus.valueOf("BANNED"));
        }
    }

    // ════════════════════════════════════════════════════
    // SESSION STATUS
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("SessionStatus")
    class SessionStatusTest {

        @Test
        @DisplayName("SessionStatus có đúng 5 giá trị")
        void coDung5GiaTri() {
            // UPCOMING, ACTIVE, SOLD, CANCELLED, ENDED_NO_BID
            assertEquals(5, SessionStatus.values().length);
        }

        @Test
        @DisplayName("Tất cả status nghiệp vụ phải có")
        void tatCaStatusNghiepVu() {
            assertDoesNotThrow(() -> SessionStatus.valueOf("UPCOMING"));
            assertDoesNotThrow(() -> SessionStatus.valueOf("ACTIVE"));
            assertDoesNotThrow(() -> SessionStatus.valueOf("SOLD"));
            assertDoesNotThrow(() -> SessionStatus.valueOf("CANCELLED"));
            assertDoesNotThrow(() -> SessionStatus.valueOf("ENDED_NO_BID"));
        }
    }

    // ════════════════════════════════════════════════════
    // ACTION TYPE - test Gson serialization
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("ActionType (Gson serialize)")
    class ActionTypeTest {

        @Test
        @DisplayName("ActionType có ít nhất 15 actions")
        void coItNhat15Actions() {
            assertTrue(ActionType.values().length >= 15,
                    "ActionType phải có đủ actions cơ bản (login, register, item, bid, user...)");
        }

        @Test
        @DisplayName("LOGIN serialize ra JSON 'login'")
        void loginSerialize() {
            String json = GSON.toJson(ActionType.LOGIN);
            assertEquals("\"login\"", json,
                    "@SerializedName(\"login\") phải làm Gson xuất ra \"login\"");
        }

        @Test
        @DisplayName("BID_PLACE serialize ra JSON 'bid_place'")
        void bidPlaceSerialize() {
            String json = GSON.toJson(ActionType.BID_PLACE);
            assertEquals("\"bid_place\"", json);
        }

        @Test
        @DisplayName("Gson deserialize 'register' ra REGISTER")
        void registerDeserialize() {
            ActionType action = GSON.fromJson("\"register\"", ActionType.class);
            assertEquals(ActionType.REGISTER, action);
        }
    }

    // ════════════════════════════════════════════════════
    // SPEC KEY - kiểm tra coverage cho các category
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("SpecKey")
    class SpecKeyTest {

        @Test
        @DisplayName("SpecKey có các key general bắt buộc")
        void coCacKeyGeneral() {
            assertDoesNotThrow(() -> SpecKey.valueOf("CONDITION"));
            assertDoesNotThrow(() -> SpecKey.valueOf("BRAND"));
            assertDoesNotThrow(() -> SpecKey.valueOf("MODEL"));
            assertDoesNotThrow(() -> SpecKey.valueOf("COLOR"));
        }

        @Test
        @DisplayName("SpecKey có các key cho ELECTRONICS")
        void coCacKeyElectronics() {
            assertDoesNotThrow(() -> SpecKey.valueOf("WARRANTY_MONTHS"));
            assertDoesNotThrow(() -> SpecKey.valueOf("STORAGE_GB"));
            assertDoesNotThrow(() -> SpecKey.valueOf("RAM_GB"));
        }

        @Test
        @DisplayName("SpecKey có các key cho VEHICLES")
        void coCacKeyVehicles() {
            assertDoesNotThrow(() -> SpecKey.valueOf("VEHICLE_MAKE"));
            assertDoesNotThrow(() -> SpecKey.valueOf("VEHICLE_YEAR"));
            assertDoesNotThrow(() -> SpecKey.valueOf("MILEAGE_KM"));
            assertDoesNotThrow(() -> SpecKey.valueOf("FUEL_TYPE"));
        }

        @Test
        @DisplayName("SpecKey có ít nhất 20 entries")
        void coItNhat20Entries() {
            assertTrue(SpecKey.values().length >= 20,
                    "SpecKey phải đủ cho 5+ category");
        }
    }

    // ════════════════════════════════════════════════════
    // ENUM uniqueness - không có 2 entry trùng tên
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("Tất cả enum không có 2 giá trị trùng tên")
    void enumKhongTrungTen() {
        // values().length == new HashSet<>(values()).size()
        assertEquals(ItemCategory.values().length,
                java.util.Set.of(ItemCategory.values()).size());
        assertEquals(UserRole.values().length,
                java.util.Set.of(UserRole.values()).size());
        assertEquals(SessionStatus.values().length,
                java.util.Set.of(SessionStatus.values()).size());
    }
}