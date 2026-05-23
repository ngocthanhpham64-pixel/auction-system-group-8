package vn.edu.vnu.uet.group8.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.dto.model.CommentDTO;

public class CommentDAO {

  private static final Logger log = LoggerFactory.getLogger(CommentDAO.class);

  private Connection getConn() throws SQLException {
    return DatabaseConnection.getInstance().getConnection();
  }

  // ═══════════════════════════════════════════════════
  // Nghiệp vụ chính 
  // ═══════════════════════════════════════════════════
  public void insert(int item_id, int user_id, String username, String content) {
    String sql = """
        INSERT INTO comments (item_id, user_id, username, content, created_at)
        VALUES (?, ?, ?, ?, NOW())
        """;

    try (Connection conn = getConn(); 
          PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, item_id);
      ps.setInt(2, user_id);
      ps.setString(3, username);
      ps.setString(4, content);
      ps.executeUpdate();
    } catch (SQLException e) {
      log.warn("Không thể thêm comment mới");
    }
  }

  // ═══════════════════════════════════════════════════
  // Tìm kiếm
  // ═══════════════════════════════════════════════════
  /**
   * Lấy danh sách comment của 1 vật phẩm 
   * @param item_id
   * @return
   */
  public List<CommentDTO> getCommentsForAnItemId(int item_id) {
    String sql = """
        SELECT c.username, c.user_id, c.content, c.created_at
        FROM comments c
        WHERE c.item_id = ?
        ORDER BY c.created_at DESC
        """;
    
    try (Connection conn = getConn();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, item_id);
      try (ResultSet rs = ps.executeQuery()) {
        List<CommentDTO> list = new ArrayList<>();
        while (rs.next()) {
          list.add(new CommentDTO(
            rs.getString("username"),
            rs.getInt("user_id"),
            rs.getString("content"),
            rs.getTimestamp("created_at").toInstant()
          ));
        }
        return list;
      }
    } catch (SQLException e) {
      log.warn("Lấy danh sách comment của item_id={} thất bại", item_id, e);
      return new ArrayList<>();
    }
  }

  /**
   * Lấy danh sách comment của 1 user
   * @param user_id
   * @return
   */
  public List<CommentDTO> getCommentsForUser(int user_id) {
    String sql = """
        SELECT c.username, c.user_id, c.content, c.created_at
        FROM comments c
        WHERE c.user_id = ?
        ORDER BY c.created_at DESC
        """;
    
    try (Connection conn = getConn();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, user_id);
      try (ResultSet rs = ps.executeQuery()) {
        List<CommentDTO> list = new ArrayList<>();
        while (rs.next()) {
          list.add(new CommentDTO(
            rs.getString("username"),
            rs.getInt("user_id"),
            rs.getString("content"),
            rs.getTimestamp("created_at").toInstant()
          ));
        }
        return list;
      }
    } catch (SQLException e) {
      log.warn("Lấy danh sách comment của user_id={} thất bại", user_id, e);
      return new ArrayList<>();
    }
  }
}
