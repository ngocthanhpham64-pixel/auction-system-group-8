package vn.edu.vnu.uet.group8.server.dao;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import vn.edu.vnu.uet.group8.common.entity.UserAdmin;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.enums.UserRole;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;
import vn.edu.vnu.uet.group8.server.dao.DatabaseConnection;
import vn.edu.vnu.uet.group8.common.utilclass.PasswordUtil;
import vn.edu.vnu.uet.group8.common.enums.AdminLevel;

public class UserDAO {
  private Connection getConn() throws SQLException {
    return DatabaseConnection.getInstance().getConnection();
  }
  // ═══════════════════════════════════════════════════
  // PHẦN 1 — mapRow: ResultSet → Entity
  //
  // Dùng Reconstructor thay vì setter rời rạc.
  // Reconstructor không validate logic — tin tưởng DB.
  // ═══════════════════════════════════════════════════

  private User mapRow(ResultSet rs) throws SQLException {

    // ── Đọc các trường chung của User ───────────────
    int        id                = rs.getInt("user_id");
    Instant    createdAt         = rs.getTimestamp("created_at")
                                      .toInstant();
    boolean    isDeleted         = rs.getBoolean("is_deleted");
    String     username          = rs.getString("username");
    String     email             = rs.getString("email");
    String     encryptedPassword = rs.getString("password_hash");
    UserStatus status            = UserStatus.valueOf(
                                      rs.getString("status"));
    Instant    lastLogin         = toInstant(
                                      rs.getTimestamp("last_login_at"));

    // ── Phân nhánh: Admin hay Member? ───────────────
    String adminLvl = rs.getString("admin_level");

    String fullName = rs.getString("full_name");
    

    if (adminLvl != null) {
      return UserAdmin.reconstructor()
          .id(id)
          .createdAt(createdAt)
          .isDeleted(isDeleted)
          .username(username)
          .email(email)
          .fullName(fullName)
          .encryptedPassword(encryptedPassword)
          .status(status)
          .lastLogin(lastLogin)
          .roles(Set.of(UserRole.ADMIN))
          .adminLevel(AdminLevel.valueOf(adminLvl))
          .build();
    }

    // ── Đọc thêm trường đặc thù của UserMember ──────
    Set<UserRole> roles = parseRoles(rs.getString("roles"));
    BigDecimal balance  = rs.getBigDecimal("balance");

    return UserMember.reconstructor()
        .id(id)
        .createdAt(createdAt)
        .isDeleted(isDeleted)
        .username(username)
        .email(email)
        .encryptedPassword(encryptedPassword)
        .status(status)
        .roles(roles)
        .lastLogin(lastLogin)
        .balance(balance)
        .fullName(fullName)
        .phone(rs.getString("phone"))
        .address(rs.getString("address"))
        .sellerRating(rs.getBigDecimal("seller_rating"))
        .avatarUrl(rs.getString("avatar_url"))
        .totalBidsPlaced(rs.getInt("total_bids_placed"))
        .totalItemsSold(rs.getInt("total_items_sold"))
        .build();
  }

  // ═══════════════════════════════════════════════════
  // PHẦN 2 — CRUD CƠ BẢN
  // ═══════════════════════════════════════════════════

  /**
   * Lưu user mới vào DB.
   * Sau khi INSERT thành công, gọi assignId() để gán
   * AUTO_INCREMENT key từ DB vào Entity.
   * Password phải được hash TRƯỚC khi gọi hàm này.
   */
  public void insert(User user) throws SQLException {
    String sql = """
        INSERT INTO users
          (username, email, password_hash,
            full_name, phone,
            roles, status, balance,
            admin_level, address,
            is_deleted, created_at,
            last_login_at, avatar_url,
            total_bids_placed, total_items_sold,
            seller_rating)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;

    // Lastlogin, avatarUrl, totalBidsPlaced,
    // totalItemsSold, sellerRating 

    try (PreparedStatement ps = getConn().prepareStatement(
          sql, Statement.RETURN_GENERATED_KEYS)) {
    // ── Trường chung ─────────────────────────────
      ps.setString(1, user.getUsername());
      ps.setString(2, user.getEmail());
      ps.setString(3, user.getEncryptedPassword());

      // ── Trường đặc thù theo loại User ────────────
      if (user instanceof UserMember m) {
          ps.setString(4, m.getFullName());
          ps.setString(5, m.getPhone());
          ps.setString(6, serializeRoles(m.getRoles()));
          ps.setBigDecimal(8, m.getBalance());
          ps.setNull(9, Types.VARCHAR);    // admin_level
          ps.setString(10, m.getAddress());
          ps.setNull(11, Types.VARCHAR);   // avatar_url
          ps.setNull(12, Types.INTEGER);   // total_bids_placed
          ps.setNull(13, Types.INTEGER);   // total_items_sold
          ps.setNull(14, Types.NUMERIC);   // seller_rating
      } else if (user instanceof UserAdmin a) {
          ps.setNull(4, Types.VARCHAR);    // full_name
          ps.setNull(5, Types.VARCHAR);    // phone
          ps.setString(6, serializeRoles(
              Set.of(UserRole.ADMIN)));
          ps.setBigDecimal(8, BigDecimal.ZERO);
          ps.setString(9, a.getAdminLevel().name());
          ps.setNull(10, Types.VARCHAR);
          ps.setNull(11, Types.VARCHAR);
          ps.setNull(12, Types.INTEGER);
          ps.setNull(13, Types.INTEGER);
          ps.setNull(14, Types.NUMERIC);
      }

      ps.setString(7, user.getStatus().name());
      ps.setBoolean(11, user.isDeleted());
      ps.setTimestamp(12, Timestamp.from(user.getCreatedAt()));

      ps.executeUpdate();

      // ── Gán ID từ DB về Entity ───────────────────
      // assignId() chỉ được gọi đúng 1 lần sau INSERT
      try (ResultSet keys = ps.getGeneratedKeys()) {
          if (keys.next()) {
            user.assignId(keys.getInt(1));
          } else {
            throw new SQLException(
                "INSERT thành công nhưng không lấy được "
                + "generated key cho user: "
                + user.getUsername());
          }
        }
      }
    }

  /**
   * Tìm theo ID — trả Optional để tầng Service tự quyết định
   * xử lý "không tìm thấy" như thế nào.
   */
  public Optional<User> findById(int userId) throws SQLException {
    String sql = """
        SELECT * FROM users
        WHERE user_id = ?
          AND is_deleted = false
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
        ps.setInt(1, userId);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return Optional.of(mapRow(rs));
        }
    }
    return Optional.empty();
  }

  public Optional<User> findByEmail(String email) throws SQLException {
    String sql = """
        SELECT * FROM users
        WHERE email = ?
          AND is_deleted = false
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setString(1, email.trim().toLowerCase());
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return Optional.of(mapRow(rs));
      }
    }
    return Optional.empty();
  }

  public Optional<User> findByUsername(String username)
          throws SQLException {
    String sql = """
        SELECT * FROM users
        WHERE username = ?
          AND is_deleted = false
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
        ps.setString(1, username.trim().toLowerCase());
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return Optional.of(mapRow(rs));
        }
    }
    return Optional.empty();
  }

  /**
   * Update profile — chỉ dành cho UserMember.
   * Không update password, balance, roles ở đây.
   */
  public void updateProfile(UserMember user) throws SQLException {
    String sql = """
        UPDATE users
        SET full_name = ?,
            phone     = ?,
            avatar_url = ?,
            address = ?
        WHERE user_id   = ?
          AND is_deleted = false
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
        ps.setString(1, user.getFullName());
        ps.setString(2, user.getPhone());
        ps.setString(3, user.getAvatarUrl());
        ps.setString(4, user.getAddress());
        ps.setInt(5, user.getId());
        ps.executeUpdate();
    }
  }

  // ═══════════════════════════════════════════════════
  // PHẦN 3 — UPDATE TỪNG TRƯỜNG NHẠY CẢM
  // ═══════════════════════════════════════════════════

  /**
   * Admin khoá / mở tài khoản.
   * Soft delete dùng markAsDeleted() — không dùng updateStatus().
   */
  public void updateStatus(int userId, UserStatus status)
          throws SQLException {
    String sql = """
        UPDATE users
        SET status  = ?
        WHERE user_id = ?
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setString(1, status.name());
      ps.setInt(2, userId);
      int affected = ps.executeUpdate();
      if (affected == 0)
        throw new UserNotFoundException(userId);
    }
  }

  /**
   * Soft delete — đánh dấu is_deleted thay vì DELETE.
   * Giữ nguyên lịch sử giao dịch, FK không bị vỡ.
   */
  public void softDelete(int userId) throws SQLException {
    String sql = """
        UPDATE users
        SET is_deleted = true
        WHERE user_id  = ?
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setInt(1, userId);
      int affected = ps.executeUpdate();
      if (affected == 0)
        throw new UserNotFoundException(userId);
    }
  }

  /**
   * Cộng/trừ balance — để DB tính toán, tránh race condition.
   * delta > 0: nạp tiền | delta < 0: trừ tiền.
   */
  public void updateBalance(int userId, BigDecimal delta)
          throws SQLException {
    String sql = """
        UPDATE users
        SET balance = balance + ?
        WHERE user_id     = ?
          AND is_deleted   = false
          AND balance + ?  >= 0
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setBigDecimal(1, delta);
      ps.setInt(2, userId);
      ps.setBigDecimal(3, delta);

      int affected = ps.executeUpdate();
      if (affected == 0)
        throw new IllegalStateException(
            "Số dư không đủ hoặc user không tồn tại. "
            + "userId=" + userId
            + ", delta=" + delta);
    }
  }

  /**
   * Đổi mật khẩu — hash tại đây, không nhận hash từ bên ngoài.
   * Đây là nơi DUY NHẤT gọi PasswordUtil.hash() cho update.
   */
  public void updatePassword(int userId, String newPlainPassword)
          throws SQLException {
    String hashed = PasswordUtil.hash(newPlainPassword);
    String sql    = """
        UPDATE users
        SET password_hash = ?
        WHERE user_id     = ?
          AND is_deleted   = false
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setString(1, hashed);
      ps.setInt(2, userId);
      int affected = ps.executeUpdate();
      if (affected == 0)
        throw new UserNotFoundException(userId);
    }
  }

  /**
   * Gọi ngay sau authenticate() thành công.
   * Dùng Instant.now() — nhất quán với serverTimezone=UTC.
   */
  public void updateLastLogin(int userId) throws SQLException {
    String sql = """
        UPDATE users
        SET last_login_at = ?
        WHERE user_id     = ?
          AND is_deleted   = false
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setTimestamp(1, Timestamp.from(Instant.now()));
      ps.setInt(2, userId);
      ps.executeUpdate();
    }
  }

  /**
   * Thêm SELLER role khi user đăng item đầu tiên.
   * Đọc roles hiện tại → thêm → ghi lại — thêm transaction
   * nếu môi trường concurrent cao.
   */
  public void addRole(int userId, UserRole role) throws SQLException {
    User user = findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    // getRoles() trả về UnmodifiableSet nên phải copy
    Set<UserRole> updated = new HashSet<>(user.getRoles());
    updated.add(role);

    String sql = """
        UPDATE users
        SET roles   = ?
        WHERE user_id = ?
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setString(1, serializeRoles(updated));
      ps.setInt(2, userId);
      ps.executeUpdate();
    }
  }

  /**
   * Cập nhật sellerRating sau khi có đánh giá mới.
   * AVG tính thẳng trong SQL — không kéo tất cả rating lên Java.
   */
  public void updateSellerRating(int sellerId) throws SQLException {
    String sql = """
        UPDATE users
        SET seller_rating = (
            SELECT AVG(score)
            FROM ratings
            WHERE seller_id = ?
        )
        WHERE user_id   = ?
          AND is_deleted  = false
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setInt(1, sellerId);
      ps.setInt(2, sellerId);
      ps.executeUpdate();
    }
  }

  // ═══════════════════════════════════════════════════
  // PHẦN 4 — NGHIỆP VỤ ĐẶC THÙ
  // ═══════════════════════════════════════════════════

  /**
   * Xác thực đăng nhập.
   * Trả Optional.empty() cho cả "email không tồn tại" lẫn "sai mật khẩu"
   * — tránh lộ thông tin "email này có trong hệ thống không".
   */
  public Optional<User> authenticate(String email, String plainPassword)
          throws SQLException {
            
    Optional<User> opt = findByEmail(email);
    if (opt.isEmpty()) return Optional.empty();

    User user = opt.get();

    if (!PasswordUtil.verify(plainPassword, user.getEncryptedPassword()))
        return Optional.empty();

    if (!user.isActive())
        return Optional.empty();

    return Optional.of(user);
  }

  /**
   * Kiểm tra trùng trước khi đăng ký.
   * EXISTS nhanh hơn COUNT(*) — DB dừng ngay khi tìm thấy.
   */
  public boolean existsByEmail(String email) throws SQLException {
    String sql = """
        SELECT EXISTS(
            SELECT 1 FROM users
            WHERE email      = ?
              AND is_deleted  = false
        )
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setString(1, email.trim().toLowerCase());
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getBoolean(1);
      }
    }
  }

  public boolean existsByUsername(String username) throws SQLException {
    String sql = """
        SELECT EXISTS(
            SELECT 1 FROM users
            WHERE username   = ?
              AND is_deleted  = false
        )
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setString(1, username.trim().toLowerCase());
      try (ResultSet rs = ps.executeQuery()) {
          return rs.next() && rs.getBoolean(1);
      }
    }
  }

  public List<UserMember> findAllSellers() throws SQLException {
    String sql = """
        SELECT * FROM users
        WHERE roles      LIKE '%SELLER%'
          AND status      = 'ACTIVE'
          AND is_deleted  = false
        ORDER BY seller_rating DESC NULLS LAST
        """;

    List<UserMember> sellers = new ArrayList<>();
    try (PreparedStatement ps = getConn().prepareStatement(sql);
          ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        User u = mapRow(rs);
        // findAllSellers chỉ trả UserMember — UserAdmin không bán
        if (u instanceof UserMember m) sellers.add(m);
      }
    }
    return sellers;
  }

  // ═══════════════════════════════════════════════════
  // PRIVATE HELPERS
  // ═══════════════════════════════════════════════════

  /** "BIDDER,SELLER" → EnumSet<UserRole> */
  private Set<UserRole> parseRoles(String rolesStr) {
    if (rolesStr == null || rolesStr.isBlank())
        return EnumSet.of(UserRole.BIDDER);

    return Arrays.stream(rolesStr.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(UserRole::valueOf)
        .collect(Collectors.toCollection(
            () -> EnumSet.noneOf(UserRole.class)));
  }

  /** EnumSet<UserRole> → "BIDDER,SELLER" */
  private String serializeRoles(Set<UserRole> roles) {
    return roles.stream()
        .map(UserRole::name)
        .collect(Collectors.joining(","));
  }

  /** Timestamp nullable → Instant nullable */
  private Instant toInstant(Timestamp ts) {
    return ts != null ? ts.toInstant() : null;
  }
}