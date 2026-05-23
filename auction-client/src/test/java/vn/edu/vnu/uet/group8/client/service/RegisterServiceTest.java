package vn.edu.vnu.uet.group8.client.service;

import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.ServerResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

// =========================================================
//  RegisterService
// =========================================================
@DisplayName("RegisterService (client-side validation)")
class RegisterServiceTest {

    @BeforeEach void clear() { SessionManager.clearSession(); }

    @Nested @DisplayName("register – no connection")
    class Register {
        @Test @DisplayName("không kết nối -> onFailure với thông báo kết nối")
        void noConnectionCallsFailure() {
            List<String> errors = new ArrayList<>();
            RegisterService.register("user", "a@b.com", "pass123", "Full Name", "0123456789",
                    () -> {}, errors::add);
            assertFalse(errors.isEmpty());
        }

        @Test @DisplayName("không kết nối -> onSuccess KHÔNG được gọi")
        void noConnectionDoesNotCallSuccess() {
            AtomicBoolean success = new AtomicBoolean(false);
            RegisterService.register("user", "a@b.com", "pass123", "Full Name", "0123456789",
                    () -> success.set(true), e -> {});
            assertFalse(success.get());
        }
    }
}