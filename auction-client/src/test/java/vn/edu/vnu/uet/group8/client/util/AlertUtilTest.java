package vn.edu.vnu.uet.group8.client.util;

import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AlertUtilTest – phủ các nhánh có thể test không cần hiển thị dialog thật.
 *
 * showInfo / showError / showWarning:
 *   - Gọi từ non-FX thread → Platform.runLater (async) → không crash, không block.
 *
 * showConfirm:
 *   - Gọi từ non-FX thread → log severe + return false (không mở dialog).
 *
 * Lưu ý: showInfo/showError/showWarning không block trên non-FX thread
 * (dùng Platform.runLater) nên dialog không thực sự hiện ra trong headless test.
 */
@DisplayName("AlertUtil")
class AlertUtilTest {

    @BeforeAll
    static void initToolkit() {
        new JFXPanel();
    }

    // ─────────── showInfo ───────────

    @Nested @DisplayName("showInfo")
    class ShowInfo {

        @Test @DisplayName("message hợp lệ → không crash")
        void valid_message_no_crash() {
            assertDoesNotThrow(() -> AlertUtil.showInfo("Thông báo test"));
        }

        @Test @DisplayName("message null → không crash")
        void null_message_no_crash() {
            assertDoesNotThrow(() -> AlertUtil.showInfo(null));
        }

        @Test @DisplayName("message blank → không crash")
        void blank_message_no_crash() {
            assertDoesNotThrow(() -> AlertUtil.showInfo("   "));
        }

        @Test @DisplayName("message rất dài → không crash")
        void long_message_no_crash() {
            assertDoesNotThrow(() -> AlertUtil.showInfo("a".repeat(1000)));
        }

        @Test @DisplayName("gọi nhiều lần liên tiếp → không crash")
        void multiple_calls_no_crash() {
            assertDoesNotThrow(() -> {
                AlertUtil.showInfo("Msg 1");
                AlertUtil.showInfo("Msg 2");
                AlertUtil.showInfo("Msg 3");
            });
        }
    }

    // ─────────── showError ───────────

    @Nested @DisplayName("showError")
    class ShowError {

        @Test @DisplayName("message hợp lệ → không crash")
        void valid_message_no_crash() {
            assertDoesNotThrow(() -> AlertUtil.showError("Lỗi test"));
        }

        @Test @DisplayName("message null → không crash")
        void null_message_no_crash() {
            assertDoesNotThrow(() -> AlertUtil.showError(null));
        }

        @Test @DisplayName("message blank → không crash")
        void blank_message_no_crash() {
            assertDoesNotThrow(() -> AlertUtil.showError(""));
        }

        @Test @DisplayName("message unicode tiếng Việt → không crash")
        void unicode_message_no_crash() {
            assertDoesNotThrow(() -> AlertUtil.showError("Không tìm thấy sản phẩm đấu giá!"));
        }
    }

    // ─────────── showWarning ───────────

    @Nested @DisplayName("showWarning")
    class ShowWarning {

        @Test @DisplayName("message hợp lệ → không crash")
        void valid_message_no_crash() {
            assertDoesNotThrow(() -> AlertUtil.showWarning("Cảnh báo test"));
        }

        @Test @DisplayName("message null → không crash")
        void null_message_no_crash() {
            assertDoesNotThrow(() -> AlertUtil.showWarning(null));
        }

        @Test @DisplayName("xen kẽ showError và showWarning → không crash")
        void interleaved_calls_no_crash() {
            assertDoesNotThrow(() -> {
                AlertUtil.showWarning("W1");
                AlertUtil.showError("E1");
                AlertUtil.showWarning("W2");
            });
        }
    }

    // ─────────── showConfirm ───────────

    @Nested @DisplayName("showConfirm")
    class ShowConfirm {

        @Test @DisplayName("gọi từ non-FX thread → trả về false (không crash)")
        void non_fx_thread_returns_false() {
            // Đang chạy trên JUnit thread (không phải FX) → log severe + return false
            boolean result = AlertUtil.showConfirm("Test Title", "Test Message");
            assertFalse(result);
        }

        @Test @DisplayName("gọi với title null từ non-FX → trả về false")
        void null_title_non_fx_returns_false() {
            boolean result = AlertUtil.showConfirm(null, "message");
            assertFalse(result);
        }

        @Test @DisplayName("gọi với message null từ non-FX → trả về false")
        void null_message_non_fx_returns_false() {
            boolean result = AlertUtil.showConfirm("Title", null);
            assertFalse(result);
        }

        @Test @DisplayName("gọi với cả hai null từ non-FX → trả về false")
        void both_null_non_fx_returns_false() {
            boolean result = AlertUtil.showConfirm(null, null);
            assertFalse(result);
        }

        @Test @DisplayName("gọi nhiều lần từ non-FX → luôn trả về false")
        void multiple_calls_always_false() {
            for (int i = 0; i < 5; i++) {
                assertFalse(AlertUtil.showConfirm("T" + i, "M" + i));
            }
        }
    }
}