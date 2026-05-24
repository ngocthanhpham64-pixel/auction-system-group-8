package vn.edu.vnu.uet.group8.common.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResponseDTOTest {

    // ═══════════════════════════════════════════════════
    // ok(T data)
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("ok(T data)")
    class OkWithDataTest {

        @Test
        @DisplayName("success = true, message = OK, data trả đúng")
        void okVoiData() {
            ResponseDTO<String> resp = ResponseDTO.ok("test-data");
            assertTrue(resp.isSuccess());
            assertEquals("OK", resp.getMessage());
            assertEquals("test-data", resp.getData());
        }

        @Test
        @DisplayName("ok với List<String>")
        void okVoiList() {
            List<String> list = List.of("a", "b", "c");
            ResponseDTO<List<String>> resp = ResponseDTO.ok(list);
            assertTrue(resp.isSuccess());
            assertEquals(3, resp.getData().size());
        }

        @Test
        @DisplayName("ok với Integer")
        void okVoiInteger() {
            ResponseDTO<Integer> resp = ResponseDTO.ok(42);
            assertEquals(42, resp.getData());
        }

        @Test
        @DisplayName("ok(null data) - hasData = false")
        void okNullData() {
            ResponseDTO<String> resp = ResponseDTO.ok((String) null);
            assertTrue(resp.isSuccess());
            assertFalse(resp.hasData());
            assertNull(resp.getData());
        }
    }

    // ═══════════════════════════════════════════════════
    // ok(T data, String message)
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("ok(T data, String message)")
    class OkWithMessageTest {

        @Test
        @DisplayName("message tùy chỉnh được dùng")
        void messageTuyChinh() {
            ResponseDTO<String> resp = ResponseDTO.ok("data", "Đặt giá thành công!");
            assertTrue(resp.isSuccess());
            assertEquals("Đặt giá thành công!", resp.getMessage());
            assertEquals("data", resp.getData());
        }

        @Test
        @DisplayName("message null → fallback OK")
        void messageNullFallback() {
            ResponseDTO<String> resp = ResponseDTO.ok("data", null);
            assertEquals("OK", resp.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════
    // ok() - no data, no message
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("ok() - không có data")
    class OkVoidTest {

        @Test
        @DisplayName("success = true, message = Thành công, data = null")
        void okVoid() {
            ResponseDTO<Void> resp = ResponseDTO.ok();
            assertTrue(resp.isSuccess());
            assertEquals("Thành công", resp.getMessage());
            assertNull(resp.getData());
            assertFalse(resp.hasData());
        }
    }

    // ═══════════════════════════════════════════════════
    // fail(String message)
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("fail(String message)")
    class FailTest {

        @Test
        @DisplayName("success = false, message đúng, data = null")
        void failVoiMessage() {
            ResponseDTO<String> resp = ResponseDTO.fail("Item không tồn tại");
            assertFalse(resp.isSuccess());
            assertEquals("Item không tồn tại", resp.getMessage());
            assertNull(resp.getData());
            assertFalse(resp.hasData());
        }

        @Test
        @DisplayName("fail message null → fallback mặc định")
        void failMessageNull() {
            ResponseDTO<String> resp = ResponseDTO.fail(null);
            assertFalse(resp.isSuccess());
            assertNotNull(resp.getMessage());
            assertFalse(resp.getMessage().isBlank());
        }

        @Test
        @DisplayName("fail với các message khác nhau")
        void failVariousMessages() {
            String[] messages = {
                    "Giá đặt quá thấp",
                    "Tài khoản bị khoá",
                    "Không đủ số dư",
                    "Phiên đấu giá đã kết thúc"
            };
            for (String msg : messages) {
                ResponseDTO<Void> resp = ResponseDTO.fail(msg);
                assertFalse(resp.isSuccess());
                assertEquals(msg, resp.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // error()
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("error()")
    class ErrorTest {

        @Test
        @DisplayName("success = false, message chứa 'Lỗi hệ thống'")
        void errorDefault() {
            ResponseDTO<String> resp = ResponseDTO.error();
            assertFalse(resp.isSuccess());
            assertTrue(resp.getMessage().contains("Lỗi hệ thống")
                    || resp.getMessage().contains("lỗi"));
            assertNull(resp.getData());
        }
    }

    // ═══════════════════════════════════════════════════
    // withType()
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("withType()")
    class WithTypeTest {

        @Test
        @DisplayName("withType set requestType đúng")
        void withType() {
            ResponseDTO<String> resp = ResponseDTO.ok("data").withType("GET_ITEM");
            assertEquals("GET_ITEM", resp.getRequestType());
        }

        @Test
        @DisplayName("withType trả về chính nó (fluent chain)")
        void withTypeFluentChain() {
            ResponseDTO<String> resp = ResponseDTO.ok("data");
            assertSame(resp, resp.withType("BID_PLACE"));
        }

        @Test
        @DisplayName("requestType null mặc định khi chưa set")
        void requestTypeNullDefault() {
            ResponseDTO<Void> resp = ResponseDTO.ok();
            assertNull(resp.getRequestType());
        }

        @Test
        @DisplayName("withType sau fail()")
        void withTypeAfterFail() {
            ResponseDTO<Void> resp = ResponseDTO.<Void>fail("lỗi").withType("LOGIN");
            assertEquals("LOGIN", resp.getRequestType());
            assertFalse(resp.isSuccess());
        }
    }

    // ═══════════════════════════════════════════════════
    // hasData()
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("hasData()")
    class HasDataTest {

        @Test
        @DisplayName("hasData = true khi data != null")
        void hasDataTrue() {
            assertTrue(ResponseDTO.ok("something").hasData());
        }

        @Test
        @DisplayName("hasData = false khi data null (ok không có data)")
        void hasDataFalseOk() {
            assertFalse(ResponseDTO.ok().hasData());
        }

        @Test
        @DisplayName("hasData = false khi fail")
        void hasDataFalseFail() {
            assertFalse(ResponseDTO.fail("lỗi").hasData());
        }
    }

    // ═══════════════════════════════════════════════════
    // timestamp
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("timestamp được set tự động khi tạo")
    void timestampAutoSet() {
        long before = System.currentTimeMillis();
        ResponseDTO<Void> resp = ResponseDTO.ok();
        long after = System.currentTimeMillis();
        assertTrue(resp.getTimestamp() >= before);
        assertTrue(resp.getTimestamp() <= after);
    }

    // ═══════════════════════════════════════════════════
    // toString
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("toString()")
    class ToStringTest {

        @Test
        @DisplayName("toString chứa success, message, hasData")
        void toStringCoInfo() {
            ResponseDTO<String> resp = ResponseDTO.ok("data").withType("GET_ITEM");
            String s = resp.toString();
            assertTrue(s.contains("success=true"));
            assertTrue(s.contains("GET_ITEM"));
            assertTrue(s.contains("hasData=true"));
        }

        @Test
        @DisplayName("toString fail chứa success=false")
        void toStringFail() {
            ResponseDTO<Void> resp = ResponseDTO.fail("lỗi");
            assertTrue(resp.toString().contains("success=false"));
        }
    }
}