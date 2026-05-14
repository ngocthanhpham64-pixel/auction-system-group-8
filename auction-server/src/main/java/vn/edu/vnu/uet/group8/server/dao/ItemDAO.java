package vn.edu.vnu.uet.group8.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;
import vn.edu.vnu.uet.group8.common.exception.ItemNotFoundException;

public class ItemDAO {

  private static final Gson GSON = new Gson();

  private Connection getConn() throws SQLException {
    return DatabaseConnection.getInstance().getConnection();
  }

  // ═══════════════════════════════════════════════════
  // PHẦN 1 — mapRow: ResultSet → Item
  // ═══════════════════════════════════════════════════

  private Item mapRow(ResultSet rs) throws SQLException {
    return Item.reconstructor()
        .id(rs.getInt("item_id"))
        .createdAt(rs.getTimestamp("created_at").toInstant())
        .isDeleted(rs.getBoolean("is_deleted"))
        .sellerId(rs.getInt("seller_id"))
        .title(rs.getString("title"))
        .description(rs.getString("description"))
        .category(ItemCategory.valueOf(rs.getString("category")))
        .condition(parseCondition(rs.getString("condition_type")))
        .specs(parseSpecs(rs.getString("specs")))
        .status(parseStatus(rs.getString("status")))
        .build();
  }

  // ═══════════════════════════════════════════════════
  // PHẦN 2 — CRUD CƠ BẢN
  // ═══════════════════════════════════════════════════

  public void insert(Item item) throws SQLException {
    String sql = """
        INSERT INTO item
          (seller_id, title, description, category, condition_type, specs, status, is_deleted, created_at)
        VALUES (?,?,?,?,?,?,?,?,?)
        """;

    try (Connection conn = getConn(); 
          PreparedStatement ps = conn.prepareStatement(
            sql, Statement.RETURN_GENERATED_KEYS)) {

      ps.setInt(1,         item.getSellerId());
      ps.setString(2,      item.getTitle());
      ps.setString(3,      item.getDescription());
      ps.setString(4,      item.getCategory().name());

      if (item.getCondition() != null)
          ps.setString(5, item.getCondition().name());
      else
          ps.setNull(5, Types.VARCHAR);

      if (item.getSpecs() == null || item.getSpecs().isEmpty()) {
        ps.setString(6, null);
      } else {
        ps.setString(6, serializeSpecs(item.getSpecs()));
      }

      if (item.getStatus() != null)
          ps.setString(7, item.getStatus().name());
      else
          ps.setString(7, ItemStatus.DRAFT.name());

      ps.setBoolean(8, item.isDeleted());
      ps.setTimestamp(9, Timestamp.from(item.getCreatedAt()));

      ps.executeUpdate();

      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next()) {
          item.assignId(keys.getInt(1));
        } else {
          throw new SQLException("INSERT thành công nhưng không lấy được generated key.");
        }
      }
    }
  }

  public Optional<Item> findById(int itemId) throws SQLException {
    String sql = """
        SELECT * FROM item
        WHERE item_id = ? AND is_deleted = false
        """;

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return Optional.of(mapRow(rs));
      }
    }
    return Optional.empty();
  }

  public List<Item> findBySeller(int sellerId) throws SQLException {
    String sql = """
        SELECT * FROM item
        WHERE seller_id = ? AND is_deleted = false
        ORDER BY created_at DESC
        """;

    return queryList(sql, ps -> ps.setInt(1, sellerId));
  }

  public void updateSpecs(int itemId, Map<String, String> specs) throws SQLException {
    String sql = """
        UPDATE item
        SET specs = ?
        WHERE item_id = ? AND is_deleted = false
        """;

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, serializeSpecs(specs));
      ps.setInt(2, itemId);
      int affected = ps.executeUpdate();
      if (affected == 0)
        throw new IllegalStateException("Không thể cập nhật specs. itemId=" + itemId);
    }
  }

  public void update(Item item) throws SQLException {
    String sql = """
        UPDATE item
        SET title = ?, description = ?, condition_type = ?, specs = ?, status = ?
        WHERE item_id = ? AND is_deleted = false
        """;

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, item.getTitle());
      ps.setString(2, item.getDescription());

      if (item.getCondition() != null)
          ps.setString(3, item.getCondition().name());
      else
          ps.setNull(3, Types.VARCHAR);

      if (item.getSpecs() == null || item.getSpecs().isEmpty()) {
        ps.setString(4, null);
      } else {
        ps.setString(4, serializeSpecs(item.getSpecs()));
      }

      if (item.getStatus() != null)
          ps.setString(5, item.getStatus().name());
      else
          ps.setString(5, ItemStatus.DRAFT.name());

      ps.setInt(6, item.getId());

      int affected = ps.executeUpdate();
      if (affected == 0)
        throw new ItemNotFoundException(item.getId());
    }
  }

  public void softDelete(int itemId) throws SQLException {
    String sql = """
        UPDATE item
        SET is_deleted = true
        WHERE item_id = ?
        """;

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      int affected = ps.executeUpdate();
      if (affected == 0)
        throw new ItemNotFoundException(itemId);
    }
  }

  public boolean isOwnedBy(int itemId, int sellerId) throws SQLException {
    String sql = """
        SELECT EXISTS(
            SELECT 1 FROM item
            WHERE item_id = ? AND seller_id = ? AND is_deleted = false
        )
        """;

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      ps.setInt(2, sellerId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getBoolean(1);
      }
    }
  }

  public int countBySellerAndStatus(int sellerId, ItemStatus status) throws SQLException {
    String sql = """
        SELECT COUNT(*) 
        FROM item
        WHERE seller_id = ? 
          AND status = ?
      """;

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sellerId);
      ps.setString(2, status.name());
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt(1) : 0;
      }
    }
  }

  // ═══════════════════════════════════════════════════
  // PRIVATE HELPERS
  // ═══════════════════════════════════════════════════

  private List<Item> queryList(String sql, SqlConsumer<PreparedStatement> binder)
          throws SQLException {
    List<Item> result = new ArrayList<>();
    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
      binder.accept(ps);
      try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) result.add(mapRow(rs));
      }
    }
    return result;
  }

  private Map<String, String> parseSpecs(String json) {
    if (json == null || json.isBlank())
      return new HashMap<>();
    return GSON.fromJson(json, new TypeToken<Map<String, String>>() {}.getType());
  }

  private String serializeSpecs(Map<String, String> specs) {
      if (specs == null || specs.isEmpty()) return null;
      return GSON.toJson(specs);
  }

  private ItemCondition parseCondition(String raw) {
      if (raw == null || raw.isBlank()) return null;
      return ItemCondition.valueOf(raw);
  }

  private ItemStatus parseStatus(String raw) {
      if (raw == null || raw.isBlank()) return ItemStatus.DRAFT;
      return ItemStatus.valueOf(raw);
  }

  @FunctionalInterface
  private interface SqlConsumer<T> {
      void accept(T t) throws SQLException;
  }
}