package vn.edu.vnu.uet.group8.server.service.user;

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
import vn.edu.vnu.uet.group8.common.dto.model.NotificationDTO;
import vn.edu.vnu.uet.group8.common.enums.NotificationType;
import vn.edu.vnu.uet.group8.server.dao.NotificationDAO;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock private NotificationDAO notificationDAO;

  private NotificationService service;

  @BeforeEach
  void setUp() {
    service = new NotificationService(notificationDAO);
  }

  private NotificationDTO buildDto(int id, boolean isRead) {
    return NotificationDTO.builder()
        .id(id)
        .userId(1)
        .title("T")
        .message("M")
        .type("OUTBID")
        .isRead(isRead)
        .createdAt(Instant.now())
        .build();
  }

  // ─────────────────────────────────────────────────────────────
  // getUserNotifications
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("getUserNotifications()")
  class GetUserNotificationsTest {

    @Test
    @DisplayName("Trả về list từ DAO")
    void traVeListTuDAO() throws SQLException {
      List<NotificationDTO> expected = List.of(buildDto(1, false), buildDto(2, true));
      when(notificationDAO.findByUserId(5)).thenReturn(expected);

      List<NotificationDTO> result = service.getUserNotifications(5);

      assertEquals(2, result.size());
      verify(notificationDAO).findByUserId(5);
    }

    @Test
    @DisplayName("Trả về list rỗng khi không có thông báo")
    void traVeListRong() throws SQLException {
      when(notificationDAO.findByUserId(99)).thenReturn(Collections.emptyList());

      assertTrue(service.getUserNotifications(99).isEmpty());
    }

    @Test
    @DisplayName("Ném SQLException khi DAO lỗi")
    void nemSQLException() throws SQLException {
      when(notificationDAO.findByUserId(anyInt())).thenThrow(new SQLException("lỗi DB"));

      assertThrows(SQLException.class, () -> service.getUserNotifications(1));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // markAsRead
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("markAsRead()")
  class MarkAsReadTest {

    @Test
    @DisplayName("Gọi DAO đúng tham số")
    void goiDAODungThamSo() throws SQLException {
      service.markAsRead(10, 5);

      verify(notificationDAO).markAsRead(10, 5);
    }

    @Test
    @DisplayName("Ném SQLException khi DAO lỗi")
    void nemSQLException() throws SQLException {
      doThrow(new SQLException("lỗi DB")).when(notificationDAO).markAsRead(anyInt(), anyInt());

      assertThrows(SQLException.class, () -> service.markAsRead(1, 1));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // deleteNotification
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("deleteNotification()")
  class DeleteNotificationTest {

    @Test
    @DisplayName("Gọi DAO.delete đúng tham số")
    void goiDAODungThamSo() throws SQLException {
      service.deleteNotification(7, 3);

      verify(notificationDAO).delete(7, 3);
    }

    @Test
    @DisplayName("Ném SQLException khi DAO lỗi")
    void nemSQLException() throws SQLException {
      doThrow(new SQLException("lỗi DB")).when(notificationDAO).delete(anyInt(), anyInt());

      assertThrows(SQLException.class, () -> service.deleteNotification(1, 1));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // createNotification
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("createNotification()")
  class CreateNotificationTest {

    @Test
    @DisplayName("Tạo notification và trả về DTO từ DAO")
    void taoThanhCong() throws SQLException {
      NotificationDTO expected = buildDto(99, false);
      when(notificationDAO.insert(any())).thenReturn(expected);

      NotificationDTO result =
          service.createNotification(1, "Tiêu đề", "Nội dung", NotificationType.OUTBID);

      assertSame(expected, result);
      verify(notificationDAO)
          .insert(
              argThat(
                  n ->
                      n.getUserId() == 1
                          && "Tiêu đề".equals(n.getTitle())
                          && "Nội dung".equals(n.getMessage())
                          && n.getType() == NotificationType.OUTBID
                          && !n.isRead()));
    }

    @Test
    @DisplayName("createNotification với tất cả NotificationType")
    void taoVoiTatCaType() throws SQLException {
      NotificationDTO dto = buildDto(1, false);
      when(notificationDAO.insert(any())).thenReturn(dto);

      for (NotificationType type : NotificationType.values()) {
        assertDoesNotThrow(() -> service.createNotification(1, "T", "M", type));
      }
    }

    @Test
    @DisplayName("Ném SQLException khi DAO.insert lỗi")
    void nemSQLException() throws SQLException {
      when(notificationDAO.insert(any())).thenThrow(new SQLException("insert lỗi"));

      assertThrows(
          SQLException.class,
          () -> service.createNotification(1, "T", "M", NotificationType.SYSTEM));
    }
  }
}