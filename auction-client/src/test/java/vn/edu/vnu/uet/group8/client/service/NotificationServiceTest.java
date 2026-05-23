package vn.edu.vnu.uet.group8.client.service;

import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.ServerResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NotificationService (client-side validation)")
class NotificationServiceTest {

    @BeforeEach void clear() { SessionManager.clearSession(); }

    @Nested @DisplayName("loadAll")
    class LoadAll {
        @Test @DisplayName("chưa đăng nhập -> onFailure")
        void notLoggedInCallsFailure() {
            List<String> errors = new ArrayList<>();
            NotificationService.loadAll(n -> {}, errors::add);
            assertFalse(errors.isEmpty());
            assertTrue(errors.get(0).contains("đăng nhập"));
        }

        @Test @DisplayName("null onSuccess không ném ngoại lệ khi chưa login")
        void nullOnSuccessNoException() {
            assertDoesNotThrow(() -> NotificationService.loadAll(null, e -> {}));
        }

        @Test @DisplayName("null onFailure không ném ngoại lệ khi chưa login")
        void nullOnFailureNoException() {
            assertDoesNotThrow(() -> NotificationService.loadAll(n -> {}, null));
        }
    }

    @Nested @DisplayName("markRead")
    class MarkRead {
        @Test @DisplayName("chưa đăng nhập -> onFailure")
        void notLoggedInCallsFailure() {
            List<String> errors = new ArrayList<>();
            NotificationService.markRead(1, () -> {}, errors::add);
            assertFalse(errors.isEmpty());
        }

        @Test @DisplayName("null callbacks khi chưa login không ném ngoại lệ")
        void nullCallbacksNoException() {
            assertDoesNotThrow(() -> NotificationService.markRead(1, null, null));
        }
    }


    @Nested @DisplayName("subscribePush / unsubscribePush")
    class Subscribe {
        @Test @DisplayName("subscribePush null listener -> trả về null không ném ngoại lệ")
        void nullListenerReturnsNull() {
            Consumer<ServerResponse> wrapper = NotificationService.subscribePush(null);
            assertNull(wrapper);
        }

        @Test @DisplayName("subscribePush hợp lệ -> trả về wrapper không null")
        void validListenerReturnsWrapper() {
            Consumer<ServerResponse> wrapper = NotificationService.subscribePush(n -> {});
            assertNotNull(wrapper);
            // cleanup
            NotificationService.unsubscribePush(wrapper);
        }

        @Test @DisplayName("unsubscribePush null không ném ngoại lệ")
        void unsubscribeNullNoException() {
            assertDoesNotThrow(() -> NotificationService.unsubscribePush(null));
        }
    }
}