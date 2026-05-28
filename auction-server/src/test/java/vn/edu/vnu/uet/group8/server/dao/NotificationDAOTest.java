package vn.edu.vnu.uet.group8.server.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.vnu.uet.group8.common.dto.model.NotificationDTO;
import vn.edu.vnu.uet.group8.common.entity.Notification;
import vn.edu.vnu.uet.group8.common.enums.NotificationType;

@ExtendWith(MockitoExtension.class)
class NotificationDAOTest {

  @Mock private DatabaseConnection dbConn;
  @Mock private Connection conn;
  @Mock private PreparedStatement ps;
  @Mock private ResultSet rs;
  @Mock private ResultSet keyRs;

  private NotificationDAO dao;
  private MockedStatic<DatabaseConnection> staticMock;

  @BeforeEach
  void setUp() throws SQLException {
    staticMock = mockStatic(DatabaseConnection.class);
    staticMock.when(DatabaseConnection::getInstance).thenReturn(dbConn);
    when(dbConn.getConnection()).thenReturn(conn);
    dao = new NotificationDAO();
  }

  @AfterEach
  void tearDown() {
    staticMock.close();
  }

  // ─────────────────────────────────────────────────────────────
  // findByUserId
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("findByUserId()")
  class FindByUserIdTest {

    @Test
    @DisplayName("Trả về danh sách khi có kết quả")
    void traVeDanhSach() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);

      Instant now = Instant.now();
      when(rs.next()).thenReturn(true, true, false);
      when(rs.getInt("notification_id")).thenReturn(1, 2);
      when(rs.getInt("user_id")).thenReturn(5, 5);
      when(rs.getString("title")).thenReturn("T1", "T2");
      when(rs.getString("message")).thenReturn("M1", "M2");
      when(rs.getString("type")).thenReturn("OUTBID", "SYSTEM");
      when(rs.getBoolean("is_read")).thenReturn(false, true);
      when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(now));

      List<NotificationDTO> result = dao.findByUserId(5);

      assertEquals(2, result.size());
      assertEquals(1, result.get(0).getId());
      assertEquals("T1", result.get(0).getTitle());
      assertFalse(result.get(0).isRead());
      assertTrue(result.get(1).isRead());
      verify(ps).setInt(1, 5);
    }

    @Test
    @DisplayName("Trả về list rỗng khi không có dữ liệu")
    void traVeListRong() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      List<NotificationDTO> result = dao.findByUserId(99);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Ném SQLException khi DB lỗi")
    void nemSQLException() throws SQLException {
      when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB down"));

      assertThrows(SQLException.class, () -> dao.findByUserId(1));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // insert
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("insert()")
  class InsertTest {

    private Notification buildNotif() {
      return Notification.builder()
          .userId(3)
          .title("Bị vượt giá")
          .message("Ai đó đặt giá cao hơn")
          .type(NotificationType.OUTBID)
          .build();
    }

    @Test
    @DisplayName("insert thành công - trả về NotificationDTO với id từ DB")
    void insertThanhCong() throws SQLException {
      when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
          .thenReturn(ps);
      when(ps.executeUpdate()).thenReturn(1);
      when(ps.getGeneratedKeys()).thenReturn(keyRs);
      when(keyRs.next()).thenReturn(true);
      when(keyRs.getInt(1)).thenReturn(42);

      Notification notif = buildNotif();
      NotificationDTO result = dao.insert(notif);

      assertEquals(42, result.getId());
      assertEquals(3, result.getUserId());
      assertEquals("Bị vượt giá", result.getTitle());
      assertEquals("Ai đó đặt giá cao hơn", result.getMessage());
      assertEquals("OUTBID", result.getType());
      assertFalse(result.isRead());

      verify(ps).setInt(1, 3);
      verify(ps).setString(2, "Bị vượt giá");
      verify(ps).setString(3, "Ai đó đặt giá cao hơn");
      verify(ps).setString(4, "OUTBID");
      verify(ps).setBoolean(5, false);
    }

    @Test
    @DisplayName("insert thất bại khi không lấy được generated key → SQLException")
    void insertKhongLayDuocKey() throws SQLException {
      when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
          .thenReturn(ps);
      when(ps.executeUpdate()).thenReturn(1);
      when(ps.getGeneratedKeys()).thenReturn(keyRs);
      when(keyRs.next()).thenReturn(false);

      assertThrows(SQLException.class, () -> dao.insert(buildNotif()));
    }

    @Test
    @DisplayName("insert ném SQLException khi DB lỗi")
    void insertNemSQLException() throws SQLException {
      when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
          .thenThrow(new SQLException("insert failed"));

      assertThrows(SQLException.class, () -> dao.insert(buildNotif()));
    }

    @Test
    @DisplayName("insert với các NotificationType khác nhau")
    void insertCacType() throws SQLException {
      for (NotificationType type : NotificationType.values()) {
        when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
            .thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);
        when(ps.getGeneratedKeys()).thenReturn(keyRs);
        when(keyRs.next()).thenReturn(true);
        when(keyRs.getInt(1)).thenReturn(1);

        Notification n = Notification.builder()
            .userId(1).title("T").message("M").type(type).build();
        NotificationDTO dto = dao.insert(n);
        assertEquals(type.name(), dto.getType());
      }
    }
  }

  // ─────────────────────────────────────────────────────────────
  // markAsRead
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("markAsRead()")
  class MarkAsReadTest {

    @Test
    @DisplayName("markAsRead gọi đúng SQL với đúng tham số")
    void markAsReadDungThamSo() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);

      dao.markAsRead(10, 5);

      verify(ps).setInt(1, 10);
      verify(ps).setInt(2, 5);
      verify(ps).executeUpdate();
    }

    @Test
    @DisplayName("markAsRead ném SQLException khi DB lỗi")
    void markAsReadNemException() throws SQLException {
      when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB lỗi"));

      assertThrows(SQLException.class, () -> dao.markAsRead(1, 1));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // delete
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("delete()")
  class DeleteTest {

    @Test
    @DisplayName("delete gọi đúng SQL với đúng tham số")
    void deleteDungThamSo() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);

      dao.delete(7, 3);

      verify(ps).setInt(1, 7);
      verify(ps).setInt(2, 3);
      verify(ps).executeUpdate();
    }

    @Test
    @DisplayName("delete ném SQLException khi DB lỗi")
    void deleteNemException() throws SQLException {
      when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB lỗi"));

      assertThrows(SQLException.class, () -> dao.delete(1, 1));
    }
  }
}