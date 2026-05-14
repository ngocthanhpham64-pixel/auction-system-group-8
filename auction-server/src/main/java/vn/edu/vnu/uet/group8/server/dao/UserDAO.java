package vn.edu.vnu.uet.group8.server.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.UserAdmin;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.AdminLevel;
import vn.edu.vnu.uet.group8.common.enums.PaymentMethod;
import vn.edu.vnu.uet.group8.common.enums.TransactionStatus;
import vn.edu.vnu.uet.group8.common.enums.TransactionType;
import vn.edu.vnu.uet.group8.common.enums.UserRole;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;
import vn.edu.vnu.uet.group8.common.exception.InsufficientBalanceException;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;
import vn.edu.vnu.uet.group8.server.util.PasswordUtil;

public class UserDAO {
  private static final Logger log = LoggerFactory.getLogger(UserDAO.class);
  private static final TransactionDAO transactionDAO = new TransactionDAO();

  private Connection getConn() throws SQLException {
    return DatabaseConnection.getInstance().getConnection();
  }
  // ═══════════════════════════════════════════════════
  // PHẦN 1 — mapRow: ResultSet → Entity
  //
  // Dùng Reconstructor thay vì setter rời rạc.
  // Reconstructor không validate logic — tin tưởng DB.
  // Có 2 mapRow , 1 cái để đổ dữ liệu đủ 1 cái để xem profile người khác
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
          .fullname(fullName)
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
        .fullname(fullName)
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
          (username, email, password_hash,        -- 1, 2, 3
          full_name, phone, roles,               -- 4, 5, 6
          status, balance, admin_level,          -- 7, 8, 9
          address, is_deleted, created_at,       -- 10, 11, 12
          last_login_at, avatar_url,             -- 13, 14
          total_bids_placed, total_items_sold,   -- 15, 16
          seller_rating)                         -- 17
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;

    try (Connection conn = getConn();
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      
      // ── Nhóm 1: Các trường chung ───────────────────────
      ps.setString(1, user.getUsername());
      ps.setString(2, user.getEmail());
      ps.setString(3, user.getEncryptedPassword());
      ps.setString(7, user.getStatus().name());
      ps.setBoolean(11, user.isDeleted());
      ps.setTimestamp(12, Timestamp.from(user.getCreatedAt()));
      
      // Mặc định null khi tạo mới
      ps.setNull(13, Types.TIMESTAMP); // last_login_at
      ps.setNull(14, Types.VARCHAR);   // avatar_url
      
      // ── Nhóm 2: Phân nhánh Member vs Admin ────────────
      if (user instanceof UserMember m) {
        ps.setString(4, m.getFullname());    // full_name
        ps.setString(5, m.getPhone());
        ps.setString(6, serializeRoles(m.getRoles()));
        ps.setBigDecimal(8, m.getBalance());
        ps.setNull(9, Types.VARCHAR); // admin_level
        ps.setString(10, m.getAddress());
        ps.setInt(15, 0);   // total_bids_placed
        ps.setInt(16, 0);   // total_items_sold
        ps.setBigDecimal(17, BigDecimal.ZERO);   // seller_rating
        
      } else if (user instanceof UserAdmin a) {
        ps.setNull(4, Types.VARCHAR);
        ps.setNull(5, Types.VARCHAR);
        ps.setString(6, serializeRoles(Set.of(UserRole.ADMIN)));
        ps.setBigDecimal(8, BigDecimal.ZERO);
        ps.setString(9, a.getAdminLevel().name());
        ps.setNull(10, Types.VARCHAR);
        ps.setNull(15, Types.INTEGER);
        ps.setNull(16, Types.INTEGER);
        ps.setNull(17, Types.NUMERIC);
      }

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

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
      
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

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
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

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
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

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, user.getFullname());
        ps.setString(2, user.getPhone());
        
        // AvatarUrl: DB cho phép null, nhưng nếu user xóa thì ta lưu null hoặc rỗng.
        // Trong entity UserMember của bạn, avatarUrl luôn được gán là "" (rỗng) nếu null,
        // nên ta cứ lưu thẳng getAvatarUrl().
        if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
          ps.setNull(3, Types.VARCHAR);
        } else {
          ps.setString(3, user.getAvatarUrl());
        }

        // Address: Nếu rỗng thì lưu NULL vào DB để tiết kiệm dung lượng
        if (user.getAddress() == null || user.getAddress().isBlank()) {
          ps.setNull(4, Types.VARCHAR);
        } else {
          ps.setString(4, user.getAddress());
        }
        
        ps.setInt(5, user.getId());
        ps.executeUpdate();
    }
  }

  // ═══════════════════════════════════════════════════
  // PHẦN 3 — UPDATE TỪNG TRƯỜNG NHẠY CẢM
  // ═══════════════════════════════════════════════════

  /**
   * Admin khoá / mở tài khoản.
   * Soft delete dùng softDelete() — không dùng updateStatus().
   */
  public void updateStatus(int userId, UserStatus status)
          throws SQLException {
    String sql = """
        UPDATE users
        SET status  = ?
        WHERE user_id = ?
        """;

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
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

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, userId);
      int affected = ps.executeUpdate();
      if (affected == 0)
        throw new UserNotFoundException(userId);
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

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
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

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
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

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
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

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sellerId);
      ps.setInt(2, sellerId);
      ps.executeUpdate();
    }
  }

  /**
   * Lưu lịch sử giao dịch VÀ thưc hiện giao dịch vào ví trong cùng 1 SQL transaction.
   * <p>Đây là điểm duy nhất đảm bảo tính nguyên tử (atomicity):
   * cả hai thao tác thành công hoặc cả hai rollback cùng nhau.
   *
   * @param transactionId ID duy nhất của giao dịch (chống nạp đúp)
   * @param userId        ID người dùng
   * @param amount        Số tiền cần nạp
   * @return BigDecimal   Số dư mới nhất sau khi nạp
   * @throws InsufficientBalanceException nếu số dư không đủ hoặc lỗi liên quan tới tài khoản
   * @throws SQLException Nếu có lỗi Database hoặc lỗi toàn vẹn dữ liệu
   */
  public BigDecimal insertTransactionAndUpdateBalance(
      String transactionId, int userId, BigDecimal amount, TransactionType transactionType,
      PaymentMethod paymentMethod) 
        throws SQLException {

    final String updateSql = """
        UPDATE users
        SET balance = balance + ?
        WHERE user_id      = ?
          AND is_deleted   = false
          AND balance + ? >= 0
        """;

    final String selectSql = """
        SELECT balance
        FROM users
        WHERE user_id   = ?
          AND is_deleted = false
        """;

    Connection conn = getConn();
    boolean originalAutoCommit = conn.getAutoCommit();

    if (transactionDAO.existsTransaction(conn, transactionId)) {
      try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
        psSelect.setInt(1, userId);
        try (ResultSet rs = psSelect.executeQuery()) {
          if (rs.next()) {
            BigDecimal balance = rs.getBigDecimal("balance");
            return balance;
          }
        }
      }
    }
    
    try {
      conn.setAutoCommit(false);

      // Bước 1: Cập nhật số dư một cách an toàn
      try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
        psUpdate.setBigDecimal(1, amount);
        psUpdate.setInt(2, userId);
        psUpdate.setBigDecimal(3, amount);

        int affectedRows = psUpdate.executeUpdate();
        if (affectedRows == 0) {
          throw new InsufficientBalanceException(
              "Giao dịch thất bại: Tài khoản không hợp lệ (ID: " + userId + ")");
        }
      }

      // Bước 2: Ghi lịch sử
      transactionDAO.insertTransaction(
            conn, 
            transactionId, 
            userId, 
            amount, 
            transactionType, 
            TransactionStatus.SUCCESS, 
            "Giao dịch thành công qua " + paymentMethod.name(), 
            null
        );

      // Bước 3: Lấy số dư mới nhất
      try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
        psSelect.setInt(1, userId);
        try (ResultSet rs = psSelect.executeQuery()) {
          if (rs.next()) {
            BigDecimal newBalance = rs.getBigDecimal("balance");
            conn.commit();
            return newBalance;
          } else {
            throw new SQLException(
                "Lỗi hệ thống: Không đọc được dữ liệu sau update (ID: " + userId + ")");
          }
        }
      }

    } catch (SQLException e) {
      try {
        conn.rollback();
      } catch (SQLException rollbackEx) {
        log.error("Rollback thất bại cho transactionId: "
                + transactionId
                + " - "
                + rollbackEx.getMessage());
      }
      throw e;

    } finally {
      try {
        conn.setAutoCommit(originalAutoCommit);
      } catch (SQLException ex) {
        log.error("Không thể khôi phục trạng thái AutoCommit cho userId: " + userId, ex);
      } finally {
        try {
          conn.close();
        } catch (SQLException ex) {
          log.error("Không thể đóng kết nối cho userId: " + userId, ex);
        }
      }
    }
  }

  public void settleAuctionPayment(String winnerTxId, String sellerTxId, int winnerId,
    int sellerId, int sessionId, BigDecimal amount, 
    TransactionType transactionType) throws SQLException {
    String buyerSlq = """
        UPDATE users
        SET frozen_balance = frozen_balance - ?
        WHERE user_id      = ?
          AND is_deleted   = false
          AND frozen_balance - ? >= 0
        """;
      
    String sellerSlq = """
        UPDATE users
        SET balance = balance + ?
        WHERE user_id      = ?
          AND is_deleted   = false
        """;
    
    Connection conn = getConn();
    boolean originalAutoCommit = conn.getAutoCommit();

    try {
      conn.setAutoCommit(false);

      try (PreparedStatement psBuyer = conn.prepareStatement(buyerSlq)) {
        psBuyer.setBigDecimal(1, amount);
        psBuyer.setInt(2, winnerId);
        psBuyer.setBigDecimal(3, amount);
        int affectedRows = psBuyer.executeUpdate();
        if (affectedRows == 0) {
          throw new InsufficientBalanceException(
              "Giao dịch thất bại: Tài khoản không hợp lệ (ID: " + winnerId + ")");
        }
      }

      transactionDAO.insertTransaction(
        conn, 
        winnerTxId, 
        winnerId, 
        amount.negate(), 
        transactionType, 
        TransactionStatus.SUCCESS,
        "Thanh toán phiên đấu giá", 
        sessionId
      );
      
      try (PreparedStatement psBuyer = conn.prepareStatement(sellerSlq)) {
        psBuyer.setBigDecimal(1, amount);
        psBuyer.setInt(2, sellerId);
        int affectedRows = psBuyer.executeUpdate();
        if (affectedRows == 0) {
          throw new SQLException("Lỗi không thể chuyển tiền cho seller");
        }
      }

      transactionDAO.insertTransaction(
        conn,
        sellerTxId,
        sellerId,
        amount,
        transactionType,
        TransactionStatus.SUCCESS,
        "Tiền bán vật phẩm đấu giá",
        sessionId
      );

      conn.commit();
    } catch (SQLException e) {
      try {
        conn.rollback();
      } catch (SQLException rollbackEx) {
        log.error("Rollback thất bại cho transactionId: "
                + sellerTxId
                + " - "
                + rollbackEx.getMessage());
      }
      throw e;

    } finally {
      try {
        conn.setAutoCommit(originalAutoCommit);
      } catch (SQLException ex) {
        log.error("Không thể khôi phục trạng thái AutoCommit cho userId: " + winnerId, ex);
      } finally {
        try {
          conn.close();
        } catch (SQLException ex) {
          log.error("Không thể đóng kết nối cho userId: " + winnerId, ex);
        }
      }
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
  public Optional<User> authenticate(String username, String plainPassword)
          throws SQLException {
            
    Optional<User> opt = findByUsername(username);
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

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
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

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, username.trim().toLowerCase());
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getBoolean(1);
      }
    }
  }

  public boolean existsByPhone(String phone) throws SQLException {
    String sql = """
        SELECT EXISTS(
            SELECT 1 FROM users
            WHERE phone      = ?
              AND is_deleted  = false
        )
        """;

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, phone.trim());
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
    try (Connection conn = getConn(); 
          PreparedStatement ps = conn.prepareStatement(sql);
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