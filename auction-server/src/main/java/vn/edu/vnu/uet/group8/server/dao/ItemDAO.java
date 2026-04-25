package vn.edu.vnu.uet.group8.server.dao;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;
import vn.edu.vnu.uet.group8.common.exception.ItemNotFoundException;
import vn.edu.vnu.uet.group8.server.dao.DatabaseConnection;

public class ItemDAO {

  private static final Gson GSON = new Gson();

  private Connection getConn() throws SQLException {
    return DatabaseConnection.getInstance().getConnection();
  }

  // ═══════════════════════════════════════════════════
  // PHẦN 1 — mapRow: ResultSet → Item
  //
  // Dùng Reconstructor — không validate logic,
  // chỉ check not-null field. Tin tưởng dữ liệu từ DB.
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
        .status(ItemStatus.valueOf(rs.getString("status")))
        .condition(parseCondition(rs.getString("condition_type")))
        .startingPrice(rs.getBigDecimal("starting_price"))
        .currentPrice(rs.getBigDecimal("current_price"))
        .endTime(toInstant(rs.getTimestamp("end_time")))
        .specs(parseSpecs(rs.getString("specs")))
        .build();
  }

  // ═══════════════════════════════════════════════════
  // PHẦN 2 — CRUD CƠ BẢN
  // ═══════════════════════════════════════════════════

  /**
   * Lưu item mới vào DB.
   * Gọi assignId() sau INSERT để gán AUTO_INCREMENT về Entity.
   * Status mặc định là UPCOMING — không cần truyền vào.
   */
  public void insert(Item item) throws SQLException {
    String sql = """
        INSERT INTO item
          (seller_id, title, description, category,
            status, condition_type,
            starting_price, current_price,
            end_time, is_deleted, created_at, specs)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
        """;

    try (PreparedStatement ps = getConn().prepareStatement(
            sql, Statement.RETURN_GENERATED_KEYS)) {

      ps.setInt(1,         item.getSellerId());
      ps.setString(2,      item.getTitle());
      ps.setString(3,      item.getDescription());
      ps.setString(4,      item.getCategory().name());
      ps.setString(5,      item.getStatus().name());

      // condition nullable — item dịch vụ có thể không có
      if (item.getCondition() != null)
          ps.setString(6, item.getCondition().name());
      else
          ps.setNull(6, Types.VARCHAR);

      ps.setBigDecimal(7,  item.getStartingPrice());
      ps.setBigDecimal(8,  item.getCurrentPrice());

      // endTime nullable — UPCOMING chưa cần set
      if (item.getEndTime() != null)
        ps.setTimestamp(9, Timestamp.from(item.getEndTime()));
      else
        ps.setNull(9, Types.TIMESTAMP);

      ps.setBoolean(10,    item.isDeleted());
      ps.setTimestamp(11,  Timestamp.from(item.getCreatedAt()));

      // Map<String,String> → JSON string
      ps.setString(12, serializeSpecs(item.getSpecs()));

      ps.executeUpdate();

      // Gán ID từ DB về Entity — chỉ gọi đúng 1 lần
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next()) {
          item.assignId(keys.getInt(1));
        } else {
          throw new SQLException(
              "INSERT thành công nhưng không lấy được "
              + "generated key cho item: " + item.getTitle());
        }
      }
    }
  }

  /**
   * Tìm theo ID — trả Optional, không ném exception.
   * Luôn lọc is_deleted = false — soft delete có hiệu lực.
   */
  public Optional<Item> findById(int itemId) throws SQLException {
    String sql = """
        SELECT * FROM item
        WHERE item_id  = ?
          AND is_deleted = false
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setInt(1, itemId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return Optional.of(mapRow(rs));
      }
    }
    return Optional.empty();
  }

  /**
   * Tất cả item đang ACTIVE của một category.
   * Đây là query phổ biến nhất — index (category, status) phát huy.
   */
  public List<Item> findActiveByCategory(ItemCategory category)
          throws SQLException {
      String sql = """
          SELECT * FROM item
          WHERE category  = ?
            AND status     = 'ACTIVE'
            AND is_deleted = false
          ORDER BY end_time ASC
          """;

    return queryList(sql, ps -> ps.setString(1, category.name()));
  }

  /**
   * Tất cả item đang ACTIVE, sắp xếp theo end_time.
   * Trang chủ dùng query này để hiển thị "sắp hết giờ".
   */
  public List<Item> findAllActive() throws SQLException {
    String sql = """
        SELECT * FROM item
        WHERE status    = 'ACTIVE'
          AND is_deleted = false
        ORDER BY end_time ASC
        """;

    return queryList(sql, ps -> {});
  }

  /**
   * Tất cả item của một seller — trang quản lý của người bán.
   * Trả cả UPCOMING, ACTIVE, SOLD — không lọc status.
   */
  public List<Item> findBySeller(int sellerId) throws SQLException {
    String sql = """
        SELECT * FROM item
        WHERE seller_id = ?
          AND is_deleted = false
        ORDER BY created_at DESC
        """;

    return queryList(sql, ps -> ps.setInt(1, sellerId));
  }

  /**
   * Tìm item theo khoảng giá và category.
   * Index (category, status, current_price) phát huy ở đây.
   * JSON specs không tham gia filter — Hybrid design.
   */
  public List<Item> findByPriceRange(ItemCategory category,
                                      BigDecimal minPrice,
                                      BigDecimal maxPrice)
          throws SQLException {
    String sql = """
        SELECT * FROM item
        WHERE category     = ?
          AND status        = 'ACTIVE'
          AND current_price BETWEEN ? AND ?
          AND is_deleted    = false
        ORDER BY current_price ASC
        """;

    return queryList(sql, ps -> {
      ps.setString(1, category.name());
      ps.setBigDecimal(2, minPrice);
      ps.setBigDecimal(3, maxPrice);
    });
  }

  /**
   * Item đã hết giờ nhưng chưa được đóng — scheduler gọi định kỳ.
   * AuctionService dùng để chuyển ACTIVE → SOLD / ENDED_NO_BID.
   */
  public List<Item> findExpiredActive() throws SQLException {
    String sql = """
        SELECT * FROM item
        WHERE status    = 'ACTIVE'
          AND end_time  <= ?
          AND is_deleted = false
        """;

    return queryList(sql,
      ps -> ps.setTimestamp(1, Timestamp.from(Instant.now())));
  }

  /**
   * Item sắp hết giờ trong N phút — Anti-sniping service dùng.
   * Khi có bid trong window này → gia hạn endTime thêm N phút.
   */
  public List<Item> findInSnipingWindow(int minutes)
          throws SQLException {
    Instant windowStart = Instant.now();
    Instant windowEnd   = Instant.now()
        .plusSeconds((long) minutes * 60);

    String sql = """
        SELECT * FROM item
        WHERE status    = 'ACTIVE'
          AND end_time  BETWEEN ? AND ?
          AND is_deleted = false
        """;

    return queryList(sql, ps -> {
      ps.setTimestamp(1, Timestamp.from(windowStart));
      ps.setTimestamp(2, Timestamp.from(windowEnd));
    });
  }

  // ═══════════════════════════════════════════════════
  // PHẦN 3 — UPDATE TỪNG TRƯỜNG
  //
  // Mỗi method chỉ làm đúng 1 việc.
  // Không có updateAll() — tránh ghi đè nhầm.
  // ═══════════════════════════════════════════════════

  /**
   * Nâng giá sau khi có bid mới.
   * Dùng điều kiện current_price < ? để tránh race condition:
   * nếu 2 bid đến cùng lúc, chỉ bid cao hơn giá hiện tại thắng.
   */
  public void updateCurrentPrice(int itemId, BigDecimal newPrice)
          throws SQLException {
    String sql = """
        UPDATE item
        SET current_price = ?
        WHERE item_id       = ?
          AND current_price  < ?
          AND status         = 'ACTIVE'
          AND is_deleted     = false
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setBigDecimal(1, newPrice);
      ps.setInt(2, itemId);
      ps.setBigDecimal(3, newPrice);

      int affected = ps.executeUpdate();
      if (affected == 0)
        throw new IllegalStateException(
          "Không thể cập nhật giá. Item không ACTIVE "
          + "hoặc đã có giá cao hơn. itemId=" + itemId
          + ", newPrice=" + newPrice);
    }
  }

  /**
   * Chuyển trạng thái item — AuctionService gọi sau khi
   * validate state machine trong Item.transitionStatus().
   */
  public void updateStatus(int itemId, ItemStatus newStatus)
          throws SQLException {
    String sql = """
        UPDATE item
        SET status  = ?
        WHERE item_id = ?
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setString(1, newStatus.name());
      ps.setInt(2, itemId);
      int affected = ps.executeUpdate();
      if (affected == 0)
        throw new ItemNotFoundException(itemId);
    }
  }

  /**
   * Gia hạn thời gian kết thúc — Anti-sniping gọi khi
   * có bid trong 5 phút cuối.
   */
  public void updateEndTime(int itemId, Instant newEndTime)
          throws SQLException {
    String sql = """
        UPDATE item
        SET end_time = ?
        WHERE item_id  = ?
          AND status    = 'ACTIVE'
          AND is_deleted = false
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setTimestamp(1, Timestamp.from(newEndTime));
      ps.setInt(2, itemId);
      int affected = ps.executeUpdate();
      if (affected == 0)
          throw new ItemNotFoundException(itemId);
    }
  }

  /**
   * Cập nhật specs — chỉ cho phép khi UPCOMING.
   * Item.putSpec() đã validate ở tầng Entity trước khi gọi đây.
   */
  public void updateSpecs(int itemId, Map<String, String> specs)
          throws SQLException {
    String sql = """
        UPDATE item
        SET specs   = ?
        WHERE item_id = ?
          AND status   = 'UPCOMING'
          AND is_deleted = false
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setString(1, serializeSpecs(specs));
      ps.setInt(2, itemId);
      int affected = ps.executeUpdate();
      if (affected == 0)
        throw new IllegalStateException(
            "Không thể sửa specs — item không ở trạng thái "
            + "UPCOMING hoặc không tồn tại. itemId=" + itemId);
    }
  }

  /**
   * Soft delete — đánh dấu is_deleted thay vì DELETE.
   * BidTransaction vẫn tham chiếu được item_id.
   */
  public void softDelete(int itemId) throws SQLException {
    String sql = """
        UPDATE item
        SET is_deleted = true
        WHERE item_id  = ?
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setInt(1, itemId);
      int affected = ps.executeUpdate();
      if (affected == 0)
        throw new ItemNotFoundException(itemId);
    }
  }

  // ═══════════════════════════════════════════════════
  // PHẦN 4 — NGHIỆP VỤ ĐẶC THÙ
  // ═══════════════════════════════════════════════════

  /**
   * Kiểm tra item có thuộc về seller không.
   * AuctionService gọi trước khi cho phép seller sửa/xóa item.
   */
  public boolean isOwnedBy(int itemId, int sellerId)
          throws SQLException {
    String sql = """
        SELECT EXISTS(
            SELECT 1 FROM item
            WHERE item_id   = ?
              AND seller_id  = ?
              AND is_deleted = false
        )
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setInt(1, itemId);
      ps.setInt(2, sellerId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getBoolean(1);
      }
    }
  }

  /**
   * Đếm số item ACTIVE của một seller.
   * Dùng để kiểm tra giới hạn số lượng item đồng thời.
   */
  public int countActiveBySeller(int sellerId) throws SQLException {
    String sql = """
        SELECT COUNT(*) FROM item
        WHERE seller_id = ?
          AND status     = 'ACTIVE'
          AND is_deleted = false
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setInt(1, sellerId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt(1) : 0;
      }
    }
  }

  // ═══════════════════════════════════════════════════
  // PRIVATE HELPERS
  // ═══════════════════════════════════════════════════

  /**
   * Template method để tránh lặp boilerplate PreparedStatement.
   * Mọi query trả List<Item> đều đi qua đây.
   */
  private List<Item> queryList(String sql, SqlConsumer<PreparedStatement> binder)
          throws SQLException {
    List<Item> result = new ArrayList<>();
    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      binder.accept(ps);
      try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) result.add(mapRow(rs));
      }
    }
    return result;
  }

  /** JSON string từ DB → Map<String, String> */
  private Map<String, String> parseSpecs(String json) {
    if (json == null || json.isBlank())
      return new HashMap<>();
    return GSON.fromJson(json,
      new TypeToken<Map<String, String>>() {}.getType());
  }

  /** Map<String, String> → JSON string để lưu DB */
  private String serializeSpecs(Map<String, String> specs) {
      if (specs == null || specs.isEmpty()) return null;
      return GSON.toJson(specs);
  }

  /** condition_type nullable trong DB */
  private ItemCondition parseCondition(String raw) {
      if (raw == null || raw.isBlank()) return null;
      return ItemCondition.valueOf(raw);
  }

  /** Timestamp nullable → Instant nullable */
  private Instant toInstant(Timestamp ts) {
      return ts != null ? ts.toInstant() : null;
  }

  /**
   * Functional interface để binder trong queryList()
   * có thể ném SQLException.
   */
  @FunctionalInterface
  private interface SqlConsumer<T> {
      void accept(T t) throws SQLException;
  }
}