package vn.edu.vnu.uet.group8.common.dto.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import vn.edu.vnu.uet.group8.common.enums.ActionType;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RequestAdvancedTest {

    // ═══════════════════════════════════════════════════
    // GetAuctionsRequest
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("GetAuctionsRequest")
    class GetAuctionsRequestTest {

        @Test
        @DisplayName("Builder mặc định - defaults đúng")
        void builderMacDinh() {
            GetAuctionsRequest req = GetAuctionsRequest.builder().build();

            assertNull(req.getCategory());
            assertNull(req.getMinPrice());
            assertNull(req.getMaxPrice());
            assertEquals(GetAuctionsRequest.SortOption.NEWEST, req.getSortBy());
            assertEquals(GetAuctionsRequest.DEFAULT_PAGE, req.getPage());
            assertEquals(GetAuctionsRequest.DEFAULT_PAGE_SIZE, req.getPageSize());
        }

        @Test
        @DisplayName("Builder đầy đủ tham số - getters đúng")
        void builderDayDu() {
            GetAuctionsRequest req = GetAuctionsRequest.builder()
                    .category(ItemCategory.ELECTRONICS)
                    .minPrice(new BigDecimal("100000"))
                    .maxPrice(new BigDecimal("5000000"))
                    .sortBy(GetAuctionsRequest.SortOption.PRICE_ASC)
                    .page(2)
                    .pageSize(10)
                    .build();

            assertEquals(ItemCategory.ELECTRONICS, req.getCategory());
            assertEquals(0, req.getMinPrice().compareTo(new BigDecimal("100000")));
            assertEquals(0, req.getMaxPrice().compareTo(new BigDecimal("5000000")));
            assertEquals(GetAuctionsRequest.SortOption.PRICE_ASC, req.getSortBy());
            assertEquals(2, req.getPage());
            assertEquals(10, req.getPageSize());
        }

        @Test
        @DisplayName("Tất cả SortOption đều hợp lệ")
        void allSortOptions() {
            for (GetAuctionsRequest.SortOption opt : GetAuctionsRequest.SortOption.values()) {
                assertDoesNotThrow(() -> GetAuctionsRequest.builder().sortBy(opt).build());
            }
        }

        @Test
        @DisplayName("page = 0 → IllegalArgumentException")
        void pageZero() {
            assertThrows(IllegalArgumentException.class, () ->
                    GetAuctionsRequest.builder().page(0).build());
        }

        @Test
        @DisplayName("page âm → IllegalArgumentException")
        void pageNegative() {
            assertThrows(IllegalArgumentException.class, () ->
                    GetAuctionsRequest.builder().page(-1).build());
        }

        @Test
        @DisplayName("pageSize = 0 → IllegalArgumentException")
        void pageSizeZero() {
            assertThrows(IllegalArgumentException.class, () ->
                    GetAuctionsRequest.builder().pageSize(0).build());
        }

        @Test
        @DisplayName("pageSize > MAX_PAGE_SIZE → IllegalArgumentException")
        void pageSizeOverMax() {
            assertThrows(IllegalArgumentException.class, () ->
                    GetAuctionsRequest.builder()
                            .pageSize(GetAuctionsRequest.MAX_PAGE_SIZE + 1).build());
        }

        @Test
        @DisplayName("pageSize = MAX_PAGE_SIZE biên trên hợp lệ")
        void pageSizeMaxValid() {
            assertDoesNotThrow(() -> GetAuctionsRequest.builder()
                    .pageSize(GetAuctionsRequest.MAX_PAGE_SIZE).build());
        }

        @Test
        @DisplayName("pageSize = 1 biên dưới hợp lệ")
        void pageSizeMinValid() {
            assertDoesNotThrow(() -> GetAuctionsRequest.builder().pageSize(1).build());
        }

        @Test
        @DisplayName("minPrice > maxPrice → IllegalArgumentException")
        void minPriceLonHonMaxPrice() {
            assertThrows(IllegalArgumentException.class, () ->
                    GetAuctionsRequest.builder()
                            .minPrice(new BigDecimal("5000000"))
                            .maxPrice(new BigDecimal("1000000"))
                            .build());
        }

        @Test
        @DisplayName("minPrice = maxPrice → hợp lệ")
        void minPriceBangMaxPrice() {
            assertDoesNotThrow(() -> GetAuctionsRequest.builder()
                    .minPrice(new BigDecimal("1000000"))
                    .maxPrice(new BigDecimal("1000000"))
                    .build());
        }

        @Test
        @DisplayName("minPrice âm → IllegalArgumentException")
        void minPriceNegative() {
            assertThrows(IllegalArgumentException.class, () ->
                    GetAuctionsRequest.builder()
                            .minPrice(new BigDecimal("-1")).build());
        }

        @Test
        @DisplayName("maxPrice âm → IllegalArgumentException")
        void maxPriceNegative() {
            assertThrows(IllegalArgumentException.class, () ->
                    GetAuctionsRequest.builder()
                            .maxPrice(new BigDecimal("-100")).build());
        }

        @Test
        @DisplayName("sortBy null → IllegalArgumentException")
        void sortByNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    GetAuctionsRequest.builder().sortBy(null).build());
        }

        @Test
        @DisplayName("minPrice null, maxPrice có giá trị → hợp lệ")
        void minPriceNullMaxPriceCo() {
            assertDoesNotThrow(() -> GetAuctionsRequest.builder()
                    .maxPrice(new BigDecimal("5000000")).build());
        }

        @Test
        @DisplayName("minPrice có giá trị, maxPrice null → hợp lệ")
        void minPriceCoMaxPriceNull() {
            assertDoesNotThrow(() -> GetAuctionsRequest.builder()
                    .minPrice(new BigDecimal("100000")).build());
        }

        @Test
        @DisplayName("toString chứa category và sortBy")
        void toStringCoInfo() {
            GetAuctionsRequest req = GetAuctionsRequest.builder()
                    .category(ItemCategory.WATCHES)
                    .sortBy(GetAuctionsRequest.SortOption.ENDING_SOON)
                    .build();
            String s = req.toString();
            assertTrue(s.contains("WATCHES"));
            assertTrue(s.contains("ENDING_SOON"));
        }
    }

    // ═══════════════════════════════════════════════════
    // LoginRequest
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("LoginRequest")
    class LoginRequestTest {

        @Test
        @DisplayName("of() hợp lệ - email trim+lowercase, password giữ nguyên")
        void ofHopLe() {
            LoginRequest req = LoginRequest.of("  USER@EMAIL.COM  ", "Passw0rd!");
            assertEquals("user@email.com", req.getEmail());
            assertEquals("Passw0rd!", req.getPassword());
        }

        @Test
        @DisplayName("Email đã lowercase không thay đổi")
        void emailDaLowercase() {
            LoginRequest req = LoginRequest.of("user@email.com", "pw");
            assertEquals("user@email.com", req.getEmail());
        }

        @Test
        @DisplayName("email null → IllegalArgumentException")
        void emailNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    LoginRequest.of(null, "password"));
        }

        @Test
        @DisplayName("email blank → IllegalArgumentException")
        void emailBlank() {
            assertThrows(IllegalArgumentException.class, () ->
                    LoginRequest.of("   ", "password"));
        }

        @Test
        @DisplayName("email empty → IllegalArgumentException")
        void emailEmpty() {
            assertThrows(IllegalArgumentException.class, () ->
                    LoginRequest.of("", "password"));
        }

        @Test
        @DisplayName("password null → IllegalArgumentException")
        void passwordNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    LoginRequest.of("user@e.com", null));
        }

        @Test
        @DisplayName("password empty → IllegalArgumentException")
        void passwordEmpty() {
            assertThrows(IllegalArgumentException.class, () ->
                    LoginRequest.of("user@e.com", ""));
        }

        @Test
        @DisplayName("toString không lộ password")
        void toStringKhongLoPassword() {
            LoginRequest req = LoginRequest.of("user@e.com", "SecretPass123");
            String s = req.toString();
            assertFalse(s.contains("SecretPass123"), "Password phải bị ẩn trong toString");
            assertTrue(s.contains("[HIDDEN]") || s.contains("HIDDEN"));
            assertTrue(s.contains("user@e.com"));
        }
    }

    // ═══════════════════════════════════════════════════
    // RegisterRequest
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("RegisterRequest")
    class RegisterRequestTest {

        private RegisterRequest.Builder validBuilder() {
            return RegisterRequest.builder()
                    .username("user01")
                    .password("Password123")
                    .email("user01@email.com")
                    .fullname("Nguyen Van A")
                    .phone("0912345678")
                    .address("Hanoi");
        }

        @Test
        @DisplayName("Builder hợp lệ - tất cả getter đúng")
        void builderHopLe() {
            RegisterRequest req = validBuilder().build();

            assertEquals("user01", req.getUsername());
            assertEquals("Password123", req.getPassword());
            assertEquals("user01@email.com", req.getEmail());
            assertEquals("Nguyen Van A", req.getFullname());
            assertEquals("0912345678", req.getPhone());
            assertEquals("Hanoi", req.getAddress());
        }

        @Test
        @DisplayName("username trim + lowercase")
        void usernameTrimLower() {
            RegisterRequest req = validBuilder().username("  USER01  ").build();
            assertEquals("user01", req.getUsername());
        }

        @Test
        @DisplayName("email trim + lowercase")
        void emailTrimLower() {
            RegisterRequest req = validBuilder().email("  USER@EMAIL.COM  ").build();
            assertEquals("user@email.com", req.getEmail());
        }

        @Test
        @DisplayName("fullname trim, không lowercase")
        void fullnameTrimNotLower() {
            RegisterRequest req = validBuilder().fullname("  Nguyen Van B  ").build();
            assertEquals("Nguyen Van B", req.getFullname());
        }

        @Test
        @DisplayName("address null → null")
        void addressNull() {
            RegisterRequest req = validBuilder().address(null).build();
            assertNull(req.getAddress());
        }

        @Test
        @DisplayName("username null → IllegalArgumentException")
        void usernameNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    validBuilder().username(null).build());
        }

        @Test
        @DisplayName("username blank → IllegalArgumentException")
        void usernameBlank() {
            assertThrows(IllegalArgumentException.class, () ->
                    validBuilder().username("   ").build());
        }

        @Test
        @DisplayName("username quá ngắn (< 3) → IllegalArgumentException")
        void usernameTooShort() {
            assertThrows(IllegalArgumentException.class, () ->
                    validBuilder().username("ab").build());
        }

        @Test
        @DisplayName("username đúng 3 ký tự - biên hợp lệ")
        void usernameMinLength() {
            assertDoesNotThrow(() -> validBuilder().username("abc").build());
        }

        @Test
        @DisplayName("password null → IllegalArgumentException")
        void passwordNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    validBuilder().password(null).build());
        }

        @Test
        @DisplayName("password quá ngắn (< 6) → IllegalArgumentException")
        void passwordTooShort() {
            assertThrows(IllegalArgumentException.class, () ->
                    validBuilder().password("12345").build());
        }

        @Test
        @DisplayName("password đúng 6 ký tự - biên hợp lệ")
        void passwordMinLength() {
            assertDoesNotThrow(() -> validBuilder().password("123456").build());
        }

        @Test
        @DisplayName("email null → IllegalArgumentException")
        void emailNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    validBuilder().email(null).build());
        }

        @Test
        @DisplayName("email sai định dạng → IllegalArgumentException")
        void emailInvalid() {
            assertThrows(IllegalArgumentException.class, () ->
                    validBuilder().email("not-an-email").build());
        }

        @Test
        @DisplayName("email thiếu domain → IllegalArgumentException")
        void emailNoDomain() {
            assertThrows(IllegalArgumentException.class, () ->
                    validBuilder().email("user@").build());
        }

        @Test
        @DisplayName("phone null → IllegalArgumentException")
        void phoneNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    validBuilder().phone(null).build());
        }

        @Test
        @DisplayName("phone sai định dạng (chữ cái) → IllegalArgumentException")
        void phoneInvalidLetters() {
            assertThrows(IllegalArgumentException.class, () ->
                    validBuilder().phone("09abc12345").build());
        }

        @Test
        @DisplayName("phone thiếu số → IllegalArgumentException")
        void phoneTooShort() {
            assertThrows(IllegalArgumentException.class, () ->
                    validBuilder().phone("09123").build());
        }

        @Test
        @DisplayName("phone đầu +84 hợp lệ")
        void phoneStartWith84() {
            assertDoesNotThrow(() -> validBuilder().phone("+84912345678").build());
        }

        @Test
        @DisplayName("phone đầu 0 hợp lệ")
        void phoneStartWith0() {
            assertDoesNotThrow(() -> validBuilder().phone("0912345678").build());
        }

        @Test
        @DisplayName("toString ẩn password, hiển thị username và email")
        void toStringAnPassword() {
            RegisterRequest req = validBuilder().build();
            String s = req.toString();
            assertFalse(s.contains("Password123"), "Password phải bị ẩn");
            assertTrue(s.contains("user01"));
            assertTrue(s.contains("user01@email.com"));
        }
    }

    // ═══════════════════════════════════════════════════
    // ServerRequest
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("ServerRequest")
    class ServerRequestTest {

        @Test
        @DisplayName("Builder đầy đủ - tất cả getter đúng")
        void builderDayDu() {
            ServerRequest<String> req = ServerRequest.<String>builder(ActionType.BID_PLACE)
                    .requestId("req-001")
                    .userId(5)
                    .token("my-token")
                    .payload("test-payload")
                    .build();

            assertEquals("req-001", req.getRequestId());
            assertEquals(ActionType.BID_PLACE, req.getAction());
            assertEquals(5, req.getUserId());
            assertEquals("my-token", req.getToken());
            assertEquals("test-payload", req.getPayload());
            assertNotNull(req.getTimestamp());
        }

        @Test
        @DisplayName("authenticated() - isAuthenticated = true")
        void authenticated() {
            ServerRequest<Void> req = ServerRequest.authenticated(ActionType.USER_PROFILE, 3, "tok");
            assertTrue(req.isAuthenticated());
            assertFalse(req.isAnonymous());
            assertEquals(3, req.getUserId());
            assertEquals("tok", req.getToken());
        }

        @Test
        @DisplayName("anonymous() - isAnonymous = true")
        void anonymous() {
            ServerRequest<Void> req = ServerRequest.anonymous(ActionType.LOGIN);
            assertTrue(req.isAnonymous());
            assertFalse(req.isAuthenticated());
            assertNull(req.getUserId());
            assertNull(req.getToken());
        }

        @Test
        @DisplayName("isAuthenticated() = false khi userId null")
        void isAuthenticatedUserIdNull() {
            ServerRequest<Void> req = ServerRequest.<Void>builder(ActionType.LOGIN)
                    .token("tok").build();
            assertFalse(req.isAuthenticated());
        }

        @Test
        @DisplayName("isAuthenticated() = false khi token null")
        void isAuthenticatedTokenNull() {
            ServerRequest<Void> req = ServerRequest.<Void>builder(ActionType.LOGIN)
                    .userId(1).build();
            assertFalse(req.isAuthenticated());
        }

        @Test
        @DisplayName("isAuthenticated() = false khi userId = 0")
        void isAuthenticatedUserIdZero() {
            ServerRequest<Void> req = ServerRequest.<Void>builder(ActionType.LOGIN)
                    .userId(0).token("tok").build();
            assertFalse(req.isAuthenticated());
        }

        @Test
        @DisplayName("action null → IllegalArgumentException")
        void actionNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    ServerRequest.builder(null));
        }

        @Test
        @DisplayName("requestId tự sinh UUID nếu không set")
        void requestIdAutoGenerate() {
            ServerRequest<Void> req = ServerRequest.anonymous(ActionType.LOGIN);
            assertNotNull(req.getRequestId());
            assertFalse(req.getRequestId().isBlank());
        }

        @Test
        @DisplayName("requestId blank bị bỏ qua, tự sinh UUID")
        void requestIdBlankIgnored() {
            ServerRequest<Void> req = ServerRequest.<Void>builder(ActionType.LOGIN)
                    .requestId("   ")
                    .build();
            // blank requestId không được set → vẫn dùng UUID mặc định
            assertNotNull(req.getRequestId());
        }

        @Test
        @DisplayName("token blank → null")
        void tokenBlank() {
            ServerRequest<Void> req = ServerRequest.<Void>builder(ActionType.LOGIN)
                    .token("   ").build();
            assertNull(req.getToken());
        }

        @Test
        @DisplayName("token null → null")
        void tokenNull() {
            ServerRequest<Void> req = ServerRequest.<Void>builder(ActionType.LOGIN)
                    .token(null).build();
            assertNull(req.getToken());
        }

        @Test
        @DisplayName("timestamp tự sinh nếu không set")
        void timestampAutoSet() {
            ServerRequest<Void> req = ServerRequest.anonymous(ActionType.REGISTER);
            assertNotNull(req.getTimestamp());
        }

        @Test
        @DisplayName("payload null được phép")
        void payloadNull() {
            ServerRequest<String> req = ServerRequest.<String>builder(ActionType.USER_PROFILE)
                    .userId(1).token("tok").build();
            assertNull(req.getPayload());
        }

        @Test
        @DisplayName("payload LoginRequest - generic type hoạt động")
        void payloadLoginRequest() {
            LoginRequest loginReq = LoginRequest.of("u@e.com", "pw123456");
            ServerRequest<LoginRequest> req = ServerRequest.<LoginRequest>builder(ActionType.LOGIN)
                    .payload(loginReq).build();

            assertSame(loginReq, req.getPayload());
            assertEquals("u@e.com", req.getPayload().getEmail());
        }

        @Test
        @DisplayName("tất cả ActionType đều tạo được ServerRequest")
        void allActionTypes() {
            for (ActionType action : ActionType.values()) {
                assertDoesNotThrow(() -> ServerRequest.anonymous(action));
            }
        }
    }
}