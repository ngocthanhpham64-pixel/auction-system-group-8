package vn.edu.vnu.uet.group8.client.service;

import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.client.util.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

// =========================================================
//  FavoriteService
// =========================================================
@DisplayName("FavoriteService (client-side validation)")
class FavoriteServiceTest {

    @BeforeEach void clear() { SessionManager.clearSession(); }

    @Nested @DisplayName("loadAll")
    class LoadAll {
        @Test @DisplayName("chưa đăng nhập -> onFailure gọi với thông báo đăng nhập")
        void notLoggedInCallsFailure() {
            List<String> errors = new ArrayList<>();
            FavoriteService.loadAll(items -> {}, errors::add);
            assertFalse(errors.isEmpty());
            assertTrue(errors.get(0).contains("đăng nhập"));
        }

        @Test @DisplayName("không kết nối -> onFailure gọi")
        void noConnectionCallsFailure() {
            SessionManager.setSession("tok", 1, "u", "f", "MEMBER");
            List<String> errors = new ArrayList<>();
            FavoriteService.loadAll(items -> {}, errors::add);
            assertFalse(errors.isEmpty());
        }

        @Test @DisplayName("null callbacks không ném ngoại lệ")
        void nullCallbacksNoException() {
            SessionManager.setSession("tok", 1, "u", "f", "MEMBER");
            assertDoesNotThrow(() -> FavoriteService.loadAll(null, null));
        }
    }

    @Nested @DisplayName("add")
    class Add {
        @Test @DisplayName("chưa đăng nhập -> onFailure gọi")
        void notLoggedInCallsFailure() {
            List<String> errors = new ArrayList<>();
            FavoriteService.add(1, () -> {}, errors::add);
            assertFalse(errors.isEmpty());
        }
    }

    @Nested @DisplayName("remove")
    class Remove {
        @Test @DisplayName("chưa đăng nhập -> onFailure gọi")
        void notLoggedInCallsFailure() {
            List<String> errors = new ArrayList<>();
            FavoriteService.remove(1, () -> {}, errors::add);
            assertFalse(errors.isEmpty());
        }
    }
}