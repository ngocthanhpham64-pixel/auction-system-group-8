package vn.edu.vnu.uet.group8.client.service;

import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.client.TestFXSetup;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.UserAdminDTO;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AdminService (client-side validation)")
class AdminServiceTest extends TestFXSetup {

    @BeforeEach
    void setup() {
        SessionManager.clearSession();
    }

    // =========================================================
    //  updateUserStatus – validation
    // =========================================================
    @Nested
    @DisplayName("updateUserStatus – validate")
    class UpdateUserStatus {

        @Test
        @DisplayName("userId <= 0 -> callback không được gọi")
        void invalidUserIdReturnsFalse() {
            AtomicBoolean called = new AtomicBoolean(false);

            AdminService.updateUserStatus(
                    0,
                    UserAdminDTO.STATUS_ACTIVE,
                    r -> called.set(true)
            );

            assertFalse(called.get());
        }

        @Test
        @DisplayName("userId âm -> callback không được gọi")
        void negativeUserIdReturnsFalse() {
            AtomicBoolean called = new AtomicBoolean(false);

            AdminService.updateUserStatus(
                    -1,
                    UserAdminDTO.STATUS_ACTIVE,
                    r -> called.set(true)
            );

            assertFalse(called.get());
        }

        @Test
        @DisplayName("status null -> ném NullPointerException")
        void nullStatusReturnsFalse() {
            assertThrows(NullPointerException.class, () ->
                    AdminService.updateUserStatus(
                            1,
                            null,
                            r -> {}
                    ));
        }

        @Test
        @DisplayName("status không hợp lệ -> callback không được gọi")
        void invalidStatusReturnsFalse() {
            AtomicBoolean called = new AtomicBoolean(false);

            AdminService.updateUserStatus(
                    1,
                    "UNKNOWN_STATUS",
                    r -> called.set(true)
            );

            assertFalse(called.get());
        }

        @Test
        @DisplayName("STATUS_ACTIVE hợp lệ")
        void activeStatusIsValid() {
            assertDoesNotThrow(() ->
                    AdminService.updateUserStatus(
                            1,
                            UserAdminDTO.STATUS_ACTIVE,
                            r -> {}
                    ));
        }

        @Test
        @DisplayName("STATUS_SUSPENDED hợp lệ")
        void suspendedStatusIsValid() {
            assertDoesNotThrow(() ->
                    AdminService.updateUserStatus(
                            1,
                            UserAdminDTO.STATUS_SUSPENDED,
                            r -> {}
                    ));
        }

        @Test
        @DisplayName("STATUS_BANNED hợp lệ")
        void bannedStatusIsValid() {
            assertDoesNotThrow(() ->
                    AdminService.updateUserStatus(
                            1,
                            UserAdminDTO.STATUS_BANNED,
                            r -> {}
                    ));
        }

        @Test
        @DisplayName("callback null -> ném NullPointerException")
        void nullCallbackThrowsNPE() {
            assertThrows(NullPointerException.class, () ->
                    AdminService.updateUserStatus(
                            1,
                            UserAdminDTO.STATUS_ACTIVE,
                            null
                    ));
        }
    }

    // =========================================================
    //  cancelAuction – validation
    // =========================================================
    @Nested
    @DisplayName("cancelAuction – validate")
    class CancelAuction {

        @Test
        @DisplayName("sessionId <= 0 -> callback không được gọi")
        void invalidSessionIdReturnsFalse() {
            AtomicBoolean called = new AtomicBoolean(false);

            AdminService.cancelAuction(
                    0,
                    r -> called.set(true)
            );

            assertFalse(called.get());
        }

        @Test
        @DisplayName("sessionId âm -> callback không được gọi")
        void negativeSessionIdReturnsFalse() {
            AtomicBoolean called = new AtomicBoolean(false);

            AdminService.cancelAuction(
                    -5,
                    r -> called.set(true)
            );

            assertFalse(called.get());
        }

        @Test
        @DisplayName("callback null -> ném NullPointerException")
        void nullCallbackThrowsNPE() {
            assertThrows(NullPointerException.class, () ->
                    AdminService.cancelAuction(1, null));
        }
    }

    // =========================================================
    //  null callback guard
    // =========================================================
    @Nested
    @DisplayName("null callback guard")
    class NullCallbackGuard {

        @Test
        @DisplayName("getStats null callback -> NullPointerException")
        void getStatsNullCallbackThrowsNPE() {
            assertThrows(NullPointerException.class,
                    () -> AdminService.getStats(null));
        }

        @Test
        @DisplayName("getUsers null callback -> NullPointerException")
        void getUsersNullCallbackThrowsNPE() {
            assertThrows(NullPointerException.class,
                    () -> AdminService.getUsers(null));
        }

        @Test
        @DisplayName("getAuctions null callback -> NullPointerException")
        void getAuctionsNullCallbackThrowsNPE() {
            assertThrows(NullPointerException.class,
                    () -> AdminService.getAuctions(null));
        }
    }
}