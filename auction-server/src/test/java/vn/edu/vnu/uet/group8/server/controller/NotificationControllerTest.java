package vn.edu.vnu.uet.group8.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.gson.JsonObject;

import vn.edu.vnu.uet.group8.common.dto.model.NotificationDTO;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.server.service.user.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock private NotificationService notificationService;

    private NotificationController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationController(notificationService);
    }

    private NotificationDTO buildDto(int id) {
        return NotificationDTO.builder()
                .id(id).userId(1).title("T").message("M")
                .type("OUTBID").isRead(false)
                .createdAt(Instant.now()).build();
    }

    private JsonObject payloadWithNotifId(int notifId) {
        JsonObject req = new JsonObject();
        JsonObject payload = new JsonObject();
        payload.addProperty("notificationId", notifId);
        req.add("payload", payload);
        return req;
    }

    // ─────────────────────────────────────────────────────────────
    // handleGetAll
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("handleGetAll()")
    class GetAllTest {

        @Test
        @DisplayName("Success - trả về list thông báo")
        void success() throws SQLException {
            List<NotificationDTO> list = List.of(buildDto(1), buildDto(2));
            when(notificationService.getUserNotifications(5)).thenReturn(list);

            ServerResponse resp = controller.handleGetAll("req-1", 5);

            assertTrue(resp.isSuccess());
            assertEquals("NOTIF_GET_ALL", resp.getAction());
            verify(notificationService).getUserNotifications(5);
        }

        @Test
        @DisplayName("Success - list rỗng vẫn trả success")
        void successEmptyList() throws SQLException {
            when(notificationService.getUserNotifications(anyInt()))
                    .thenReturn(Collections.emptyList());

            ServerResponse resp = controller.handleGetAll("req-2", 1);

            assertTrue(resp.isSuccess());
        }

        @Test
        @DisplayName("Lỗi SQLException → trả error response")
        void dbError() throws SQLException {
            when(notificationService.getUserNotifications(anyInt()))
                    .thenThrow(new SQLException("DB lỗi"));

            ServerResponse resp = controller.handleGetAll("req-err", 1);

            assertFalse(resp.isSuccess());
            assertEquals("NOTIF_GET_ALL", resp.getAction());
            assertNotNull(resp.getMessage());
        }

        @Test
        @DisplayName("Lỗi RuntimeException → trả error response")
        void runtimeError() throws SQLException {
            when(notificationService.getUserNotifications(anyInt()))
                    .thenThrow(new RuntimeException("lỗi bất ngờ"));

            ServerResponse resp = controller.handleGetAll("req-runtime", 1);

            assertFalse(resp.isSuccess());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // handleMarkRead
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("handleMarkRead()")
    class MarkReadTest {

        @Test
        @DisplayName("Success với payload có notificationId")
        void successVoiPayload() throws SQLException {
            JsonObject req = payloadWithNotifId(10);

            ServerResponse resp = controller.handleMarkRead(req, "req-mark", 5);

            assertTrue(resp.isSuccess());
            assertEquals("NOTIF_MARK_READ", resp.getAction());
            verify(notificationService).markAsRead(10, 5);
        }

        @Test
        @DisplayName("Success khi notificationId ở root (không có payload wrapper)")
        void successKhongCoPayloadWrapper() throws SQLException {
            JsonObject req = new JsonObject();
            req.addProperty("notificationId", 7);

            ServerResponse resp = controller.handleMarkRead(req, "req-root", 3);

            assertTrue(resp.isSuccess());
            verify(notificationService).markAsRead(7, 3);
        }

        @Test
        @DisplayName("Thiếu notificationId → error response")
        void thieuNotifId() throws SQLException {
            JsonObject req = new JsonObject();
            req.add("payload", new JsonObject()); // payload rỗng

            ServerResponse resp = controller.handleMarkRead(req, "req-missing", 1);

            assertFalse(resp.isSuccess());
            assertEquals("NOTIF_MARK_READ", resp.getAction());
        }

        @Test
        @DisplayName("DAO lỗi → error response")
        void daoError() throws SQLException {
            doThrow(new SQLException("DB lỗi"))
                    .when(notificationService).markAsRead(anyInt(), anyInt());
            JsonObject req = payloadWithNotifId(10);

            ServerResponse resp = controller.handleMarkRead(req, "req-fail", 1);

            assertFalse(resp.isSuccess());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // handleDelete
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("handleDelete()")
    class DeleteTest {

        @Test
        @DisplayName("Success với payload có notificationId")
        void successVoiPayload() throws SQLException {
            JsonObject req = payloadWithNotifId(15);

            ServerResponse resp = controller.handleDelete(req, "req-del", 2);

            assertTrue(resp.isSuccess());
            assertEquals("NOTIF_DELETE", resp.getAction());
            verify(notificationService).deleteNotification(15, 2);
        }

        @Test
        @DisplayName("Success khi notificationId ở root")
        void successRootLevel() throws SQLException {
            JsonObject req = new JsonObject();
            req.addProperty("notificationId", 20);

            ServerResponse resp = controller.handleDelete(req, "req-del-root", 4);

            assertTrue(resp.isSuccess());
            verify(notificationService).deleteNotification(20, 4);
        }

        @Test
        @DisplayName("Thiếu notificationId → error response")
        void thieuNotifId() {
            JsonObject req = new JsonObject();
            req.add("payload", new JsonObject());

            ServerResponse resp = controller.handleDelete(req, "req-no-id", 1);

            assertFalse(resp.isSuccess());
            assertEquals("NOTIF_DELETE", resp.getAction());
        }

        @Test
        @DisplayName("DAO lỗi → error response")
        void daoError() throws SQLException {
            doThrow(new RuntimeException("unexpected"))
                    .when(notificationService).deleteNotification(anyInt(), anyInt());
            JsonObject req = payloadWithNotifId(5);

            ServerResponse resp = controller.handleDelete(req, "req-fail-del", 1);

            assertFalse(resp.isSuccess());
        }
    }
}