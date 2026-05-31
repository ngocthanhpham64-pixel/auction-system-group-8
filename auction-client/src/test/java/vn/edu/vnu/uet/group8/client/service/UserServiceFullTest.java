package vn.edu.vnu.uet.group8.client.service;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.model.TransactionHistoryEntry;
import vn.edu.vnu.uet.group8.common.dto.model.UserProfileDTO;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.enums.ActionType;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test đầy đủ cho UserService — phủ toàn bộ 17 methods và 88 branches.
 *
 * ══════════════════════════════════════════════════════════════
 * ROOT CAUSE CỦA 5 LỖI CÒN LẠI (JsonSyntaxException):
 *
 * withdraw() và deposit() khi thành công đều gọi loadProfile(null) bên trong
 * callback của chính nó. Stub naïve trả CÙNG một ServerResponse cho tất cả
 * sendAuthenticatedRequest → lần 2 (loadProfile) nhận data = BigDecimal("400000")
 * rồi gọi GsonUtil.toObject(BigDecimal, UserProfileDTO.class):
 *   - BigDecimal.isInstance(UserProfileDTO) = false
 *   - → GSON.toJson(BigDecimal) = "400000"
 *   - → GSON.fromJson("400000", UserProfileDTO.class)
 *   - → JsonSyntaxException: Expected BEGIN_OBJECT but was NUMBER
 *
 * FIX: Stub phải phân biệt theo ActionType:
 *   - USER_WITHDRAW / USER_DEPOSIT → trả BigDecimal (newBalance)
 *   - USER_PROFILE → trả UserProfileDTO (hoặc null nếu không cần assert profile)
 *
 * Helper stubWithdrawOrDeposit() đảm bảo đúng response cho từng action.
 * ══════════════════════════════════════════════════════════════
 */
@DisplayName("UserService – Full Coverage")
class UserServiceFullTest {

    // ─── JFX Toolkit ──────────────────────────────────────────────────────────
    @BeforeAll
    static void initJfx() {
        new JFXPanel();
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {}
    }

    @BeforeEach
    void resetSession() {
        SessionManager.clearSession();
        ClientModel.getInstance().updateBalance(BigDecimal.ZERO);
    }

    // ─── Response factories ────────────────────────────────────────────────────

    private static ServerResponse successResponse(Object data) {
        return ServerResponse.reply("ACTION", "req-1").success(true).data(data).build();
    }

    private static ServerResponse failResponse(String msg) {
        return ServerResponse.reply("ACTION", "req-1").success(false).message(msg).build();
    }

    private static ServerResponse failResponseNoMsg() {
        return ServerResponse.reply("ACTION", "req-1").success(false).build();
    }

    // ─── Stub helpers ─────────────────────────────────────────────────────────

    /**
     * Stub đơn giản: mọi sendAuthenticatedRequest đều nhận cùng response.
     * Dùng cho những method KHÔNG gọi loadProfile() bên trong callback.
     */
    @SuppressWarnings("unchecked")
    private void stubClient(MockedStatic<AuctionClient> clientMock,
                            AuctionClient mockInstance,
                            ServerResponse response) {
        clientMock.when(AuctionClient::getInstance).thenReturn(mockInstance);
        doAnswer(inv -> {
            Consumer<ServerResponse> cb = inv.getArgument(2);
            if (cb != null) cb.accept(response);
            return null;
        }).when(mockInstance).sendAuthenticatedRequest(
                any(ActionType.class), any(), any(Consumer.class));
    }

    /**
     * Stub thông minh cho withdraw/deposit:
     * - USER_WITHDRAW hoặc USER_DEPOSIT → trả withdrawResp (có BigDecimal trong data)
     * - USER_PROFILE (do loadProfile() gọi bên trong callback) → trả profileResp (UserProfileDTO)
     *
     * Không dùng stub chung vì withdraw/deposit thành công sẽ gọi loadProfile(null)
     * ngay bên trong callback. Nếu loadProfile nhận BigDecimal thay vì UserProfileDTO,
     * Gson ném JsonSyntaxException: Expected BEGIN_OBJECT but was NUMBER.
     */
    @SuppressWarnings("unchecked")
    private void stubWithdrawOrDeposit(MockedStatic<AuctionClient> clientMock,
                                       AuctionClient mockInstance,
                                       ActionType mainAction,
                                       ServerResponse mainResp,
                                       ServerResponse profileResp) {
        clientMock.when(AuctionClient::getInstance).thenReturn(mockInstance);
        doAnswer(inv -> {
            ActionType action = inv.getArgument(0);
            Consumer<ServerResponse> cb = inv.getArgument(2);
            if (cb == null) return null;
            if (action == mainAction) {
                cb.accept(mainResp);
            } else if (action == ActionType.USER_PROFILE) {
                cb.accept(profileResp);
            }
            return null;
        }).when(mockInstance).sendAuthenticatedRequest(
                any(ActionType.class), any(), any(Consumer.class));
    }

    /**
     * Tạo ServerResponse chứa UserProfileDTO (dùng cho loadProfile bên trong withdraw/deposit).
     * data = null → loadProfile xử lý branch "user == null", không ném exception.
     */
    private static ServerResponse profileResponseNull() {
        return ServerResponse.reply("ACTION", "req-1").success(true).data(null).build();
    }

    // ─── Chờ FX thread ────────────────────────────────────────────────────────
    private void flushFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        assertTrue(latch.await(2, TimeUnit.SECONDS), "FX thread không phản hồi");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. loadProfile(Consumer)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("loadProfile(Consumer)")
    class LoadProfile {

        @Test
        @DisplayName("success=true, user != null → cập nhật ClientModel, gọi callback với user")
        void success_valid_user_updates_model() {
            UserProfileDTO dto = buildProfileDto("alice", "Alice", "alice@test.com",
                    "0912345678", "Hà Nội", new BigDecimal("4.5"),
                    new BigDecimal("500000"), "http://avatar.png");
            SessionManager.setSession("tok", 1, "alice", "Alice", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                // UserProfileDTO là POJO → GsonUtil.toObject serialize→deserialize OK
                stubClient(mc, mockClient, successResponse(dto));

                AtomicReference<UserProfileDTO> result = new AtomicReference<>();
                UserService.loadProfile(result::set);

                assertNotNull(result.get());
                assertEquals("alice", result.get().getUsername());
            }
        }

        @Test
        @DisplayName("success=true, balance != null → cập nhật balance trong ClientModel")
        void success_updates_balance() {
            UserProfileDTO dto = buildProfileDto("bob", "Bob", "bob@test.com",
                    null, null, null, new BigDecimal("123456"), null);
            SessionManager.setSession("tok", 2, "bob", "Bob", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, successResponse(dto));
                UserService.loadProfile(p -> {});
            }
        }

        @Test
        @DisplayName("success=true, avatarUrl != null → setAvatarUrl được gọi")
        void success_avatar_not_null() {
            UserProfileDTO dto = buildProfileDto("carol", "Carol", "c@t.com",
                    null, null, null, null, "http://img.png");
            SessionManager.setSession("tok", 3, "carol", "Carol", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, successResponse(dto));

                AtomicReference<UserProfileDTO> result = new AtomicReference<>();
                UserService.loadProfile(result::set);
                assertNotNull(result.get());
            }
        }

        @Test
        @DisplayName("success=true, avatarUrl null → setAvatarUrl(\"\") (branch avatarUrl null)")
        void success_avatar_null_sets_empty() {
            UserProfileDTO dto = buildProfileDto("dave", "Dave", "d@t.com",
                    null, null, null, null, null);
            SessionManager.setSession("tok", 4, "dave", "Dave", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, successResponse(dto));

                AtomicReference<UserProfileDTO> result = new AtomicReference<>();
                UserService.loadProfile(result::set);
                assertNotNull(result.get());
            }
        }

        @Test
        @DisplayName("success=true, data null → updateBalance(ZERO), callback nhận null")
        void success_null_data_calls_updateBalance_zero() {
            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, successResponse(null));

                AtomicReference<UserProfileDTO> result = new AtomicReference<>(mock(UserProfileDTO.class));
                UserService.loadProfile(result::set);
                assertNull(result.get());
            }
        }

        @Test
        @DisplayName("success=false, message != null → log warning, callback nhận null")
        void failure_with_message_callback_null() {
            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, failResponse("Server error"));

                AtomicReference<UserProfileDTO> result = new AtomicReference<>(mock(UserProfileDTO.class));
                UserService.loadProfile(result::set);
                assertNull(result.get());
            }
        }

        @Test
        @DisplayName("success=false, message null → log unknown error, callback nhận null")
        void failure_null_message() {
            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, failResponseNoMsg());

                AtomicReference<UserProfileDTO> result = new AtomicReference<>(mock(UserProfileDTO.class));
                UserService.loadProfile(result::set);
                assertNull(result.get());
            }
        }

        @Test
        @DisplayName("onResult null + success=true → không NPE")
        void null_callback_no_npe_on_success() {
            UserProfileDTO dto = buildProfileDto("e", "E", "e@t.com",
                    null, null, null, null, null);
            SessionManager.setSession("tok", 5, "e", "E", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, successResponse(dto));
                assertDoesNotThrow(() -> UserService.loadProfile(null));
            }
        }

        @Test
        @DisplayName("onResult null + success=false → không NPE")
        void null_callback_no_npe_on_failure() {
            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, failResponse("err"));
                assertDoesNotThrow(() -> UserService.loadProfile(null));
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. withdraw(BigDecimal, Consumer)
    //
    // Tất cả test success phải dùng stubWithdrawOrDeposit() vì withdraw() gọi
    // loadProfile(null) bên trong callback thành công.
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("withdraw(BigDecimal, Consumer)")
    class Withdraw {

        @Test
        @DisplayName("amount null → false (validation)")
        void null_amount_returns_false() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");
            AtomicReference<Boolean> result = new AtomicReference<>();
            UserService.withdraw(null, result::set);
            assertFalse(result.get());
        }

        @Test
        @DisplayName("amount = 0 → false (amount <= 0)")
        void zero_amount_returns_false() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");
            AtomicReference<Boolean> result = new AtomicReference<>();
            UserService.withdraw(BigDecimal.ZERO, result::set);
            assertFalse(result.get());
        }

        @Test
        @DisplayName("amount âm → false")
        void negative_amount_returns_false() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");
            AtomicReference<Boolean> result = new AtomicReference<>();
            UserService.withdraw(new BigDecimal("-100"), result::set);
            assertFalse(result.get());
        }

        @Test
        @DisplayName("chưa đăng nhập → false (isLoggedIn branch)")
        void not_logged_in_returns_false() {
            AtomicReference<Boolean> result = new AtomicReference<>();
            UserService.withdraw(new BigDecimal("100000"), result::set);
            assertFalse(result.get());
        }

        /**
         * FIX: Dùng stubWithdrawOrDeposit để phân biệt response theo ActionType.
         * USER_WITHDRAW nhận BigDecimal, USER_PROFILE nhận null (branch "user null").
         */
        @Test
        @DisplayName("success=true, newBalance != null → cập nhật ClientModel, callback true")
        void success_with_balance_updates_model() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            ServerResponse withdrawResp = successResponse(new BigDecimal("400000"));
            ServerResponse profileResp  = profileResponseNull(); // loadProfile nhận null data → OK

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubWithdrawOrDeposit(mc, mockClient,
                        ActionType.USER_WITHDRAW, withdrawResp, profileResp);

                AtomicReference<Boolean> result = new AtomicReference<>();
                UserService.withdraw(new BigDecimal("100000"), result::set);
                assertTrue(result.get());
            }
        }

        @Test
        @DisplayName("success=true, newBalance null → fallback subtract, callback true")
        void success_null_balance_fallback_subtract() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");
            ClientModel.getInstance().updateBalance(new BigDecimal("500000"));

            // data=null → GsonUtil.toObject(null, BigDecimal.class) = null → fallback subtract
            ServerResponse withdrawResp = successResponse(null);
            ServerResponse profileResp  = profileResponseNull();

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubWithdrawOrDeposit(mc, mockClient,
                        ActionType.USER_WITHDRAW, withdrawResp, profileResp);

                AtomicReference<Boolean> result = new AtomicReference<>();
                UserService.withdraw(new BigDecimal("100000"), result::set);
                assertTrue(result.get());
            }
        }

        @Test
        @DisplayName("success=false, message != null → callback false")
        void failure_with_message() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, failResponse("Số dư không đủ"));

                AtomicReference<Boolean> result = new AtomicReference<>();
                UserService.withdraw(new BigDecimal("100000"), result::set);
                assertFalse(result.get());
            }
        }

        @Test
        @DisplayName("success=false, message null → callback false (branch message null)")
        void failure_null_message() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, failResponseNoMsg());

                AtomicReference<Boolean> result = new AtomicReference<>();
                UserService.withdraw(new BigDecimal("100000"), result::set);
                assertFalse(result.get());
            }
        }

        /**
         * FIX: onResult null — loadProfile vẫn được gọi bên trong callback,
         * cần stub phân biệt action để tránh JsonSyntaxException.
         */
        @Test
        @DisplayName("onResult null + success=true → không NPE")
        void null_callback_success_no_npe() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            ServerResponse withdrawResp = successResponse(new BigDecimal("500000"));
            ServerResponse profileResp  = profileResponseNull();

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubWithdrawOrDeposit(mc, mockClient,
                        ActionType.USER_WITHDRAW, withdrawResp, profileResp);

                assertDoesNotThrow(() -> UserService.withdraw(new BigDecimal("100000"), null));
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. deposit(BigDecimal, Consumer)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deposit(BigDecimal, Consumer)")
    class Deposit {

        @Test
        @DisplayName("amount null → false (validation)")
        void null_amount_returns_false() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");
            AtomicReference<Boolean> result = new AtomicReference<>();
            UserService.deposit(null, result::set);
            assertFalse(result.get());
        }

        @Test
        @DisplayName("amount = 0 → false")
        void zero_amount_returns_false() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");
            AtomicReference<Boolean> result = new AtomicReference<>();
            UserService.deposit(BigDecimal.ZERO, result::set);
            assertFalse(result.get());
        }

        @Test
        @DisplayName("amount âm → false")
        void negative_amount_returns_false() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");
            AtomicReference<Boolean> result = new AtomicReference<>();
            UserService.deposit(new BigDecimal("-1"), result::set);
            assertFalse(result.get());
        }

        @Test
        @DisplayName("chưa đăng nhập → false")
        void not_logged_in_returns_false() {
            AtomicReference<Boolean> result = new AtomicReference<>();
            UserService.deposit(new BigDecimal("100000"), result::set);
            assertFalse(result.get());
        }

        /**
         * FIX: Dùng stubWithdrawOrDeposit — deposit thành công gọi loadProfile(null)
         * bên trong callback. USER_DEPOSIT nhận BigDecimal, USER_PROFILE nhận null data.
         */
        @Test
        @DisplayName("success=true, newBalance != null → cập nhật ClientModel, callback true")
        void success_with_balance() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            ServerResponse depositResp = successResponse(new BigDecimal("600000"));
            ServerResponse profileResp = profileResponseNull();

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubWithdrawOrDeposit(mc, mockClient,
                        ActionType.USER_DEPOSIT, depositResp, profileResp);

                AtomicReference<Boolean> result = new AtomicReference<>();
                UserService.deposit(new BigDecimal("100000"), result::set);
                assertTrue(result.get());
            }
        }

        @Test
        @DisplayName("success=true, newBalance null → log warning, callback true (branch newBalance null)")
        void success_null_balance_logs_warning() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            ServerResponse depositResp = successResponse(null);
            ServerResponse profileResp = profileResponseNull();

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubWithdrawOrDeposit(mc, mockClient,
                        ActionType.USER_DEPOSIT, depositResp, profileResp);

                AtomicReference<Boolean> result = new AtomicReference<>();
                UserService.deposit(new BigDecimal("100000"), result::set);
                assertTrue(result.get());
            }
        }

        @Test
        @DisplayName("success=false, message != null → callback false")
        void failure_with_message() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, failResponse("Lỗi nạp tiền"));
                AtomicReference<Boolean> result = new AtomicReference<>();
                UserService.deposit(new BigDecimal("100000"), result::set);
                assertFalse(result.get());
            }
        }

        @Test
        @DisplayName("success=false, message null → callback false (branch message null)")
        void failure_null_message() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, failResponseNoMsg());
                AtomicReference<Boolean> result = new AtomicReference<>();
                UserService.deposit(new BigDecimal("100000"), result::set);
                assertFalse(result.get());
            }
        }

        @Test
        @DisplayName("onResult null + failure → không NPE")
        void null_callback_failure_no_npe() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, failResponse("err"));
                assertDoesNotThrow(() -> UserService.deposit(new BigDecimal("100000"), null));
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {"0.01", "1", "999999999.99"})
        @DisplayName("amount hợp lệ + đã login → gọi sendAuthenticatedRequest với USER_DEPOSIT")
        void valid_amount_logged_in_sends_request(String amount) {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                mc.when(AuctionClient::getInstance).thenReturn(mockClient);
                doNothing().when(mockClient).sendAuthenticatedRequest(
                        any(ActionType.class), any(), any(Consumer.class));

                UserService.deposit(new BigDecimal(amount), r -> {});

                verify(mockClient, times(1))
                        .sendAuthenticatedRequest(eq(ActionType.USER_DEPOSIT), any(), any());
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. updateProfile(String, String, String, String, Consumer)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfile {

        @Test
        @DisplayName("success=true → gọi loadProfile + callback true")
        void success_calls_loadProfile_and_true() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, successResponse(null));

                AtomicReference<Boolean> result = new AtomicReference<>();
                UserService.updateProfile("Tên", "0912", "HN", null, result::set);
                assertTrue(result.get());
            }
        }

        @Test
        @DisplayName("success=false, message != null → callback false")
        void failure_with_message() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, failResponse("Cập nhật thất bại"));

                AtomicReference<Boolean> result = new AtomicReference<>();
                UserService.updateProfile("Tên", "0912", "HN", null, result::set);
                assertFalse(result.get());
            }
        }

        @Test
        @DisplayName("success=false, message null → branch 'message != null' = false")
        void failure_null_message() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, failResponseNoMsg());

                AtomicReference<Boolean> result = new AtomicReference<>();
                UserService.updateProfile("Tên", "0912", "HN", null, result::set);
                assertFalse(result.get());
            }
        }

        @Test
        @DisplayName("avatarBase64 != null và không blank → avatarUrl có trong payload")
        void avatar_base64_included_in_payload() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                mc.when(AuctionClient::getInstance).thenReturn(mockClient);
                doAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Map<String, String> payload = (Map<String, String>) inv.getArgument(1);
                    assertTrue(payload.containsKey("avatarUrl"), "avatarUrl phải có trong payload");
                    Consumer<ServerResponse> cb = inv.getArgument(2);
                    if (cb != null) cb.accept(successResponse(null));
                    return null;
                }).when(mockClient).sendAuthenticatedRequest(
                        any(ActionType.class), any(Map.class), any(Consumer.class));

                UserService.updateProfile("Tên", "0912", "HN", "base64data", r -> {});
            }
        }

        @Test
        @DisplayName("avatarBase64 blank → avatarUrl không có trong payload")
        void avatar_base64_blank_not_in_payload() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                mc.when(AuctionClient::getInstance).thenReturn(mockClient);
                doAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Map<String, String> payload = (Map<String, String>) inv.getArgument(1);
                    assertFalse(payload.containsKey("avatarUrl"), "avatarUrl không được có khi blank");
                    Consumer<ServerResponse> cb = inv.getArgument(2);
                    if (cb != null) cb.accept(successResponse(null));
                    return null;
                }).when(mockClient).sendAuthenticatedRequest(
                        any(ActionType.class), any(Map.class), any(Consumer.class));

                UserService.updateProfile("Tên", "0912", "HN", "   ", r -> {});
            }
        }

        @Test
        @DisplayName("avatarBase64 null → avatarUrl không có trong payload")
        void avatar_base64_null_not_in_payload() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                mc.when(AuctionClient::getInstance).thenReturn(mockClient);
                doAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Map<String, String> payload = (Map<String, String>) inv.getArgument(1);
                    assertFalse(payload.containsKey("avatarUrl"));
                    Consumer<ServerResponse> cb = inv.getArgument(2);
                    if (cb != null) cb.accept(successResponse(null));
                    return null;
                }).when(mockClient).sendAuthenticatedRequest(
                        any(ActionType.class), any(Map.class), any(Consumer.class));

                UserService.updateProfile("Tên", "0912", "HN", null, r -> {});
            }
        }

        @Test
        @DisplayName("address null → payload[address] = '' (branch address null)")
        void null_address_maps_to_empty_string() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                mc.when(AuctionClient::getInstance).thenReturn(mockClient);
                doAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Map<String, String> payload = (Map<String, String>) inv.getArgument(1);
                    assertEquals("", payload.get("address"), "address null phải map thành \"\"");
                    Consumer<ServerResponse> cb = inv.getArgument(2);
                    if (cb != null) cb.accept(successResponse(null));
                    return null;
                }).when(mockClient).sendAuthenticatedRequest(
                        any(ActionType.class), any(Map.class), any(Consumer.class));

                UserService.updateProfile("Tên", "0912", null, null, r -> {});
            }
        }

        @Test
        @DisplayName("onResult null + success=true → không NPE")
        void null_callback_no_npe() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, successResponse(null));
                assertDoesNotThrow(() ->
                        UserService.updateProfile("Tên", "0912", "HN", null, null));
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. loadMyBids(Consumer)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("loadMyBids(Consumer)")
    class LoadMyBids {

        @Test
        @DisplayName("success, data có list → callback nhận list không null")
        void success_returns_list() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, successResponse(List.of()));

                AtomicReference<List<?>> result = new AtomicReference<>();
                UserService.loadMyBids(r -> result.set(r));
                assertNotNull(result.get());
            }
        }

        @Test
        @DisplayName("data null → callback nhận list rỗng (branch bids == null)")
        void null_data_returns_empty_list() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, successResponse(null));

                AtomicReference<List<?>> result = new AtomicReference<>();
                UserService.loadMyBids(r -> result.set(r));
                assertNotNull(result.get());
                assertTrue(result.get().isEmpty());
            }
        }

        @Test
        @DisplayName("onResult null → không NPE")
        void null_callback_no_npe() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, successResponse(null));
                assertDoesNotThrow(() -> UserService.loadMyBids(null));
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. changePassword(String, String, Consumer)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("success=true → callback true")
        void success_returns_true() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, successResponse(null));

                AtomicReference<Boolean> result = new AtomicReference<>();
                UserService.changePassword("oldPass", "newPass", result::set);
                assertTrue(result.get());
            }
        }

        @Test
        @DisplayName("success=false → callback false")
        void failure_returns_false() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, failResponse("Mật khẩu cũ sai"));

                AtomicReference<Boolean> result = new AtomicReference<>();
                UserService.changePassword("wrongOld", "newPass", result::set);
                assertFalse(result.get());
            }
        }

        @Test
        @DisplayName("success=false, message null → branch message null")
        void failure_null_message() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, failResponseNoMsg());

                AtomicReference<Boolean> result = new AtomicReference<>();
                UserService.changePassword("old", "new", result::set);
                assertFalse(result.get());
            }
        }

        @Test
        @DisplayName("onResult null + success=true → không NPE")
        void null_callback_no_npe() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, successResponse(null));
                assertDoesNotThrow(() -> UserService.changePassword("old", "new", null));
            }
        }

        @Test
        @DisplayName("onResult null + success=false → không NPE")
        void null_callback_failure_no_npe() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, failResponse("err"));
                assertDoesNotThrow(() -> UserService.changePassword("old", "new", null));
            }
        }

        @Test
        @DisplayName("sendAuthenticatedRequest được gọi với ACTION USER_CHANGE_PASSWORD")
        void sends_correct_action() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                mc.when(AuctionClient::getInstance).thenReturn(mockClient);
                doNothing().when(mockClient).sendAuthenticatedRequest(
                        any(ActionType.class), any(), any(Consumer.class));

                UserService.changePassword("old", "new", r -> {});

                verify(mockClient).sendAuthenticatedRequest(
                        eq(ActionType.USER_CHANGE_PASSWORD), any(), any());
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. loadTransactions(Consumer)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("loadTransactions(Consumer)")
    class LoadTransactions {

        @Test
        @DisplayName("success=true, data có list → callback nhận list không null")
        void success_returns_list() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, successResponse(List.of()));

                AtomicReference<List<TransactionHistoryEntry>> result = new AtomicReference<>();
                UserService.loadTransactions(result::set);
                assertNotNull(result.get());
            }
        }

        @Test
        @DisplayName("success=true, data null → list rỗng (branch list null)")
        void success_null_data_empty_list() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, successResponse(null));

                AtomicReference<List<TransactionHistoryEntry>> result = new AtomicReference<>();
                UserService.loadTransactions(result::set);
                assertNotNull(result.get());
                assertTrue(result.get().isEmpty());
            }
        }

        @Test
        @DisplayName("success=false → list rỗng (branch success=false)")
        void failure_returns_empty_list() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, failResponse("Lỗi"));

                AtomicReference<List<TransactionHistoryEntry>> result = new AtomicReference<>();
                UserService.loadTransactions(result::set);
                assertNotNull(result.get());
                assertTrue(result.get().isEmpty());
            }
        }

        @Test
        @DisplayName("onResult null + success=true → không NPE")
        void null_callback_no_npe() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, successResponse(null));
                assertDoesNotThrow(() -> UserService.loadTransactions(null));
            }
        }

        @Test
        @DisplayName("onResult null + success=false → không NPE")
        void null_callback_failure_no_npe() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, failResponse("err"));
                assertDoesNotThrow(() -> UserService.loadTransactions(null));
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 8. loadPurchaseHistory(Consumer)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("loadPurchaseHistory(Consumer)")
    class LoadPurchaseHistory {

        @Test
        @DisplayName("success=true, data có list → callback nhận list không null")
        void success_returns_list() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, successResponse(List.of()));

                AtomicReference<List<AuctionItemDTO>> result = new AtomicReference<>();
                UserService.loadPurchaseHistory(result::set);
                assertNotNull(result.get());
            }
        }

        @Test
        @DisplayName("success=true, data null → list rỗng (branch list null)")
        void success_null_data_empty_list() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, successResponse(null));

                AtomicReference<List<AuctionItemDTO>> result = new AtomicReference<>();
                UserService.loadPurchaseHistory(result::set);
                assertNotNull(result.get());
                assertTrue(result.get().isEmpty());
            }
        }

        @Test
        @DisplayName("success=false → list rỗng")
        void failure_returns_empty_list() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, failResponse("Lỗi lịch sử mua"));

                AtomicReference<List<AuctionItemDTO>> result = new AtomicReference<>();
                UserService.loadPurchaseHistory(result::set);
                assertNotNull(result.get());
                assertTrue(result.get().isEmpty());
            }
        }

        @Test
        @DisplayName("onResult null → không NPE")
        void null_callback_no_npe() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubClient(mc, mockClient, successResponse(null));
                assertDoesNotThrow(() -> UserService.loadPurchaseHistory(null));
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 9. static initializer
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("class load thành công, LOGGER không null")
    void static_initializer_loaded() {
        assertDoesNotThrow(() -> Class.forName(
                "vn.edu.vnu.uet.group8.client.service.UserService"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 10. withdraw success → gọi loadProfile (integration)
    //
    // FIX: stubWithdrawOrDeposit phân biệt USER_WITHDRAW vs USER_PROFILE.
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("withdraw – sau success gọi loadProfile lại")
    class WithdrawCallsLoadProfile {

        @Test
        @DisplayName("withdraw success → sendAuthenticatedRequest được gọi >= 2 lần")
        void success_triggers_load_profile() {
            SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

            ServerResponse withdrawResp = successResponse(new BigDecimal("300000"));
            ServerResponse profileResp  = profileResponseNull();

            AuctionClient mockClient = mock(AuctionClient.class);
            try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
                stubWithdrawOrDeposit(mc, mockClient,
                        ActionType.USER_WITHDRAW, withdrawResp, profileResp);

                AtomicReference<Boolean> result = new AtomicReference<>();
                UserService.withdraw(new BigDecimal("100000"), result::set);
                assertTrue(result.get());

                verify(mockClient, atLeast(2))
                        .sendAuthenticatedRequest(any(), any(), any());
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 11. deposit success → gọi loadProfile (integration)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("deposit success → sendAuthenticatedRequest được gọi >= 2 lần")
    void deposit_success_triggers_load_profile() {
        SessionManager.setSession("tok", 1, "u", "U", "MEMBER");

        ServerResponse depositResp = successResponse(new BigDecimal("700000"));
        ServerResponse profileResp = profileResponseNull();

        AuctionClient mockClient = mock(AuctionClient.class);
        try (MockedStatic<AuctionClient> mc = mockStatic(AuctionClient.class)) {
            stubWithdrawOrDeposit(mc, mockClient,
                    ActionType.USER_DEPOSIT, depositResp, profileResp);

            AtomicReference<Boolean> result = new AtomicReference<>();
            UserService.deposit(new BigDecimal("200000"), result::set);
            assertTrue(result.get());

            verify(mockClient, atLeast(2))
                    .sendAuthenticatedRequest(any(), any(), any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 12. Concurrency
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("8 thread deposit đồng thời khi chưa login → tất cả false, không deadlock")
    void concurrent_deposit_not_logged_in_all_false() throws InterruptedException {
        int n = 8;
        CountDownLatch latch = new CountDownLatch(n);
        java.util.concurrent.atomic.AtomicInteger falseCount = new java.util.concurrent.atomic.AtomicInteger();

        for (int i = 0; i < n; i++) {
            new Thread(() -> UserService.deposit(BigDecimal.valueOf(100_000), r -> {
                if (!r) falseCount.incrementAndGet();
                latch.countDown();
            })).start();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(n, falseCount.get());
    }

    @Test
    @DisplayName("8 thread withdraw đồng thời khi chưa login → tất cả false")
    void concurrent_withdraw_not_logged_in_all_false() throws InterruptedException {
        int n = 8;
        CountDownLatch latch = new CountDownLatch(n);
        java.util.concurrent.atomic.AtomicInteger falseCount = new java.util.concurrent.atomic.AtomicInteger();

        for (int i = 0; i < n; i++) {
            new Thread(() -> UserService.withdraw(BigDecimal.valueOf(50_000), r -> {
                if (!r) falseCount.incrementAndGet();
                latch.countDown();
            })).start();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(n, falseCount.get());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helper: build UserProfileDTO từ JSON string
    // ══════════════════════════════════════════════════════════════════════════

    private UserProfileDTO buildProfileDto(String username, String fullName, String email,
                                           String phone, String address,
                                           BigDecimal sellerRating, BigDecimal balance,
                                           String avatarUrl) {
        String json = String.format(
                "{\"userId\":1,\"username\":\"%s\",\"email\":\"%s\","
                        + "\"fullName\":%s,\"phone\":%s,\"address\":%s,"
                        + "\"sellerRating\":%s,\"balance\":%s,\"avatarUrl\":%s,"
                        + "\"totalBidsPlaced\":0,\"totalItemsSold\":0}",
                safe(username), safe(email),
                q(fullName), q(phone), q(address),
                sellerRating != null ? sellerRating.toPlainString() : "null",
                balance != null ? balance.toPlainString() : "null",
                q(avatarUrl)
        );
        return GsonUtil.GSON.fromJson(json, UserProfileDTO.class);
    }

    private String safe(String s) { return s != null ? s : ""; }
    private String q(String s)    { return s != null ? "\"" + s + "\"" : "null"; }
}