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
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;
import vn.edu.vnu.uet.group8.common.exception.ItemNotFoundException;

@ExtendWith(MockitoExtension.class)
class ItemDAOTest {

  @Mock private DatabaseConnection dbConn;
  @Mock private Connection conn;
  @Mock private PreparedStatement ps;
  @Mock private ResultSet rs;
  @Mock private ResultSet keyRs;

  private ItemDAO dao;
  private MockedStatic<DatabaseConnection> staticMock;

  @BeforeEach
  void setUp() throws SQLException {
    staticMock = mockStatic(DatabaseConnection.class);
    staticMock.when(DatabaseConnection::getInstance).thenReturn(dbConn);
    when(dbConn.getConnection()).thenReturn(conn);
    dao = new ItemDAO();
  }

  @AfterEach
  void tearDown() {
    staticMock.close();
  }

  private void setupItemRs(int itemId, String category, String condition, String status)
      throws SQLException {
    when(rs.getInt("item_id")).thenReturn(itemId);
    when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.now()));
    when(rs.getBoolean("is_deleted")).thenReturn(false);
    when(rs.getInt("seller_id")).thenReturn(1);
    when(rs.getString("title")).thenReturn("Test Item " + itemId);
    when(rs.getString("description")).thenReturn("Mô tả");
    when(rs.getString("category")).thenReturn(category);
    when(rs.getString("condition_type")).thenReturn(condition);
    when(rs.getString("image_urls")).thenReturn(null);
    when(rs.getString("status")).thenReturn(status);
  }

  // ─────────────────────────────────────────────────────────────
  // insert
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("insert()")
  class InsertTest {

    @Test
    @DisplayName("insert thành công - gán id từ generated key")
    void insertThanhCong() throws SQLException {
      when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
          .thenReturn(ps);
      when(ps.getGeneratedKeys()).thenReturn(keyRs);
      when(keyRs.next()).thenReturn(true);
      when(keyRs.getInt(1)).thenReturn(88);

      Item item = new Item.Builder(1, "iPhone 17", ItemCategory.ELECTRONICS)
          .condition(ItemCondition.NEW)
          .status(ItemStatus.LISTED)
          .build();

      dao.insert(item);

      assertEquals(88, item.getId());
      verify(ps).setInt(1, 1);
      verify(ps).setString(2, "iPhone 17");
    }

    @Test
    @DisplayName("insert - condition mặc định USED")
    void insertConditionDefaultUsed() throws SQLException {

      when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
          .thenReturn(ps);

      when(ps.getGeneratedKeys()).thenReturn(keyRs);
      when(keyRs.next()).thenReturn(true);
      when(keyRs.getInt(1)).thenReturn(1);

      Item item = new Item.Builder(1, "Item", ItemCategory.OTHER).build();

      dao.insert(item);

      verify(ps).setString(5, "USED");
    }

    @Test
    @DisplayName("insert - imageUrls rỗng → setString(6, null)")
    void insertImageUrlsEmpty() throws SQLException {
      when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
          .thenReturn(ps);
      when(ps.getGeneratedKeys()).thenReturn(keyRs);
      when(keyRs.next()).thenReturn(true);
      when(keyRs.getInt(1)).thenReturn(1);

      Item item = new Item.Builder(1, "Item", ItemCategory.OTHER).build();

      dao.insert(item);

      verify(ps).setString(eq(6), isNull());
    }

    @Test
    @DisplayName("insert - status null → set DRAFT")
    void insertStatusNull() throws SQLException {
      when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
          .thenReturn(ps);
      when(ps.getGeneratedKeys()).thenReturn(keyRs);
      when(keyRs.next()).thenReturn(true);
      when(keyRs.getInt(1)).thenReturn(1);

      Item item = new Item.Builder(1, "Item", ItemCategory.OTHER).build();

      dao.insert(item);

      verify(ps).setString(eq(7), eq("DRAFT"));
    }

    @Test
    @DisplayName("insert - không lấy được generated key → SQLException")
    void insertNoKey() throws SQLException {
      when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
          .thenReturn(ps);
      when(ps.getGeneratedKeys()).thenReturn(keyRs);
      when(keyRs.next()).thenReturn(false);

      assertThrows(
          SQLException.class,
          () -> dao.insert(new Item.Builder(1, "x", ItemCategory.OTHER).build()));
    }

    @Test
    @DisplayName("insert - SQLException → ném lên")
    void insertSQLException() throws SQLException {
      when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
          .thenThrow(new SQLException("DB lỗi"));

      assertThrows(
          SQLException.class,
          () -> dao.insert(new Item.Builder(1, "x", ItemCategory.OTHER).build()));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // findById
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("findById()")
  class FindByIdTest {

    @Test
    @DisplayName("Tìm thấy → trả Optional.of(item)")
    void timThay() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      setupItemRs(5, "ELECTRONICS", "NEW", "LISTED");

      Optional<Item> result = dao.findById(5);

      assertTrue(result.isPresent());
      assertEquals(5, result.get().getId());
      assertEquals(ItemCategory.ELECTRONICS, result.get().getCategory());
      verify(ps).setInt(1, 5);
    }

    @Test
    @DisplayName("Không tìm thấy → Optional.empty()")
    void khongTimThay() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      assertTrue(dao.findById(999).isEmpty());
    }

    @Test
    @DisplayName("category không hợp lệ → fallback OTHER")
    void categoryFallback() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      setupItemRs(1, "UNKNOWN_CAT", "NEW", "LISTED");

      Optional<Item> result = dao.findById(1);

      assertTrue(result.isPresent());
      assertEquals(ItemCategory.OTHER, result.get().getCategory());
    }

    @Test
    @DisplayName("category null → fallback OTHER")
    void categoryNull() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      setupItemRs(1, null, "NEW", "LISTED");

      Optional<Item> result = dao.findById(1);

      assertTrue(result.isPresent());
      assertEquals(ItemCategory.OTHER, result.get().getCategory());
    }

    @Test
    @DisplayName("condition null → null trong entity")
    void conditionNull() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      setupItemRs(1, "ELECTRONICS", null, "LISTED");

      Optional<Item> result = dao.findById(1);

      assertTrue(result.isPresent());
      assertNull(result.get().getCondition());
    }

    @Test
    @DisplayName("status null → fallback DRAFT")
    void statusNull() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      setupItemRs(1, "ELECTRONICS", "NEW", null);

      Optional<Item> result = dao.findById(1);

      assertTrue(result.isPresent());
      assertEquals(ItemStatus.DRAFT, result.get().getStatus());
    }

    @Test
    @DisplayName("SQLException → ném lên")
    void sqlException() throws SQLException {
      when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));
      assertThrows(SQLException.class, () -> dao.findById(1));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // findBySeller
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("findBySeller()")
  class FindBySellerTest {

    @Test
    @DisplayName("Có items → trả list đủ")
    void coItems() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true, true, false);
      setupItemRs(1, "ELECTRONICS", "NEW", "LISTED");
      when(rs.getInt("item_id")).thenReturn(1, 2);

      List<Item> result = dao.findBySeller(1);

      assertEquals(2, result.size());
      verify(ps).setInt(1, 1);
    }

    @Test
    @DisplayName("Không có items → list rỗng")
    void listRong() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      assertTrue(dao.findBySeller(99).isEmpty());
    }
  }

  // ─────────────────────────────────────────────────────────────
  // findBySellerAndStatus
  // ─────────────────────────────────────────────────────────────
  @Test
  @DisplayName("findBySellerAndStatus() - gọi đúng tham số")
  void findBySellerAndStatus() throws SQLException {
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(false);

    dao.findBySellerAndStatus(1, ItemStatus.LISTED);

    verify(ps).setInt(1, 1);
    verify(ps).setString(2, "LISTED");
  }

  // ─────────────────────────────────────────────────────────────
  // update
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("update()")
  class UpdateTest {

    @Test
    @DisplayName("update thành công - 1 row bị ảnh hưởng")
    void updateThanhCong() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeUpdate()).thenReturn(1);

      Item item = new Item.Builder(1, "Updated", ItemCategory.ELECTRONICS)
          .condition(ItemCondition.USED)
          .status(ItemStatus.LISTED)
          .build();
      item.assignId(5);

      assertDoesNotThrow(() -> dao.update(item));
      verify(ps).setString(1, "Updated");
      verify(ps).setInt(6, 5);
    }

    @Test
    @DisplayName("update 0 rows → ItemNotFoundException")
    void zeroRows() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeUpdate()).thenReturn(0);

      Item item = new Item.Builder(1, "X", ItemCategory.OTHER).build();
      item.assignId(999);

      assertThrows(ItemNotFoundException.class, () -> dao.update(item));
    }

    @Test
    @DisplayName("update - condition null → setNull")
    void conditionNull() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeUpdate()).thenReturn(1);

      Item item = new Item.Builder(1, "X", ItemCategory.OTHER).build();
      item.assignId(1);

      dao.update(item);
      verify(ps).setString(3, "USED");
    }

    @Test
    @DisplayName("update - imageUrls rỗng → setString(4, null)")
    void imageUrlsEmpty() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeUpdate()).thenReturn(1);

      Item item = new Item.Builder(1, "X", ItemCategory.OTHER).build();
      item.assignId(1);

      dao.update(item);
      verify(ps).setString(eq(4), isNull());
    }
  }

  // ─────────────────────────────────────────────────────────────
  // softDelete
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("softDelete()")
  class SoftDeleteTest {

    @Test
    @DisplayName("softDelete thành công")
    void thanhCong() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeUpdate()).thenReturn(1);

      assertDoesNotThrow(() -> dao.softDelete(3));
      verify(ps).setInt(1, 3);
    }

    @Test
    @DisplayName("softDelete 0 rows → ItemNotFoundException")
    void zeroRows() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeUpdate()).thenReturn(0);

      assertThrows(ItemNotFoundException.class, () -> dao.softDelete(999));
    }

    @Test
    @DisplayName("SQLException → ném lên")
    void sqlException() throws SQLException {
      when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));
      assertThrows(SQLException.class, () -> dao.softDelete(1));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // isOwnedBy
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("isOwnedBy()")
  class IsOwnedByTest {

    @Test
    @DisplayName("item thuộc về seller → trả true")
    void isOwnedTrue() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      when(rs.getBoolean(1)).thenReturn(true);

      assertTrue(dao.isOwnedBy(5, 1));
      verify(ps).setInt(1, 5);
      verify(ps).setInt(2, 1);
    }

    @Test
    @DisplayName("item không thuộc về seller → trả false")
    void isOwnedFalse() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      when(rs.getBoolean(1)).thenReturn(false);

      assertFalse(dao.isOwnedBy(5, 99));
    }

    @Test
    @DisplayName("rs.next() = false → trả false")
    void rsNextFalse() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      assertFalse(dao.isOwnedBy(1, 1));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // countBySellerAndStatus
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("countBySellerAndStatus()")
  class CountBySellerAndStatusTest {

    @Test
    @DisplayName("Trả về đúng count từ DB")
    void traVeCount() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      when(rs.getInt(1)).thenReturn(5);

      int count = dao.countBySellerAndStatus(1, ItemStatus.LISTED);

      assertEquals(5, count);
      verify(ps).setInt(1, 1);
      verify(ps).setString(2, "LISTED");
    }

    @Test
    @DisplayName("rs.next() = false → trả 0")
    void rsNextFalse() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      assertEquals(0, dao.countBySellerAndStatus(1, ItemStatus.DRAFT));
    }
  }
}