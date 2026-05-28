package vn.edu.vnu.uet.group8.server.service.user;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.dto.model.AdminStatsDTO;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.model.UserAdminStatsDTO;
import vn.edu.vnu.uet.group8.common.dto.model.UserAdminDTO;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.UserAdmin;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;
import vn.edu.vnu.uet.group8.common.exception.UnauthorizedException;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.DatabaseConnection;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.service.auction.AuctionService;

/**
 * Xử lý nghiệp vụ quản trị tài khoản — chỉ Admin được gọi.
 */
public class AdminUserService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserService.class);
  private final UserDAO userDAO;
  private final AuctionService auctionService;

  public AdminUserService(UserDAO userDAO, AuctionService auctionService) {
    this.userDAO = userDAO;
    this.auctionService = auctionService;
  }

  public AdminStatsDTO getDashboardStats(int adminId) throws SQLException {
    checkAdminAccess(adminId);
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      int activeAuctions    = countSingle(conn, "SELECT COUNT(*) FROM auction_session WHERE status = 'ACTIVE'       AND is_deleted = FALSE");
      int soldAuctions      = countSingle(conn, "SELECT COUNT(*) FROM auction_session WHERE status = 'SOLD'         AND is_deleted = FALSE");
      int cancelledAuctions = countSingle(conn, "SELECT COUNT(*) FROM auction_session WHERE status = 'CANCELLED'    AND is_deleted = FALSE");
      int upcomingAuctions  = countSingle(conn, "SELECT COUNT(*) FROM auction_session WHERE status = 'UPCOMING'     AND is_deleted = FALSE");
      int endedNoBidAuctions= countSingle(conn, "SELECT COUNT(*) FROM auction_session WHERE status = 'ENDED_NO_BID' AND is_deleted = FALSE");
      int totalUsers        = countSingle(conn, "SELECT COUNT(*) FROM users WHERE is_deleted = FALSE");
      int totalBids         = countSingle(conn, "SELECT COUNT(*) FROM bid_transaction");

      BigDecimal totalRevenue = BigDecimal.ZERO;
      try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(current_price), 0) FROM auction_session WHERE status = 'SOLD' AND is_deleted = FALSE");
           ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          BigDecimal sum = rs.getBigDecimal(1);
          if (sum != null) totalRevenue = sum;
        }
      }
      return new AdminStatsDTO(activeAuctions, totalUsers, totalRevenue, totalBids,
              soldAuctions, cancelledAuctions, upcomingAuctions, endedNoBidAuctions);
    }
  }

  public List<UserAdminDTO> getUsers(int adminId) throws SQLException {
    checkAdminAccess(adminId);
    List<UserAdminDTO> users = new ArrayList<>();
    String sql = "SELECT user_id, username, email, roles, admin_level, status FROM users WHERE is_deleted = FALSE ORDER BY user_id ASC";
    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        users.add(new UserAdminDTO(
            rs.getInt("user_id"),
            rs.getString("username"),
            rs.getString("email"),
            resolveRole(rs.getString("roles"), rs.getString("admin_level")),
            rs.getString("status")
        ));
      }
    }
    return users;
  }

  public List<AuctionItemDTO> getAuctions(int adminId) throws SQLException {
    checkAdminAccess(adminId);
    List<AuctionItemDTO> list = new ArrayList<>();
    String sql = "SELECT s.session_id , i.item_id, i.title, i.description, i.category, i.condition_type, "
               + "       i.created_at, "
               + "       s.current_price, s.end_time, s.status AS session_status, "
               + "       u.username AS seller_username "
               + "FROM auction_session s "
               + "JOIN item i ON s.item_id = i.item_id "
               + "JOIN users u ON i.seller_id = u.user_id "
               + "WHERE s.is_deleted = FALSE AND i.is_deleted = FALSE "
               + "ORDER BY s.created_at DESC";

    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        list.add(buildItemDTO(rs));
      }
    }
    return list;
  }

  /**
   * Lấy số liệu thống kê cho 1 user cụ thể (để admin xem chi tiết).
   */
  public UserAdminStatsDTO getUserStats(int adminId, int targetUserId) throws SQLException {
    checkAdminAccess(adminId);
    try (var conn = DatabaseConnection.getInstance().getConnection()) {
      UserAdminStatsDTO stats = new UserAdminStatsDTO();
      // Basic user info
      try (var ps = conn.prepareStatement("SELECT user_id, username, last_login_at FROM users WHERE user_id = ? AND is_deleted = FALSE")) {
        ps.setInt(1, targetUserId);
        try (var rs = ps.executeQuery()) {
          if (rs.next()) {
            stats.setUserId(rs.getInt("user_id"));
            stats.setUsername(rs.getString("username"));
            java.sql.Timestamp ts = rs.getTimestamp("last_login_at");
            if (ts != null) stats.setLastLogin(ts.toInstant());
          } else {
            throw new vn.edu.vnu.uet.group8.common.exception.UserNotFoundException(targetUserId);
          }
        }
      }

      // total auctions created
      try (var ps = conn.prepareStatement("SELECT COUNT(*) FROM item WHERE seller_id = ? AND is_deleted = FALSE")) {
        ps.setInt(1, targetUserId);
        try (var rs = ps.executeQuery()) { if (rs.next()) stats.setTotalAuctionsCreated(rs.getInt(1)); }
      }

      // active auctions
      try (var ps = conn.prepareStatement("SELECT COUNT(*) FROM auction_session s JOIN item i ON s.item_id = i.item_id WHERE i.seller_id = ? AND s.status = 'ACTIVE' AND s.is_deleted = FALSE AND i.is_deleted = FALSE")) {
        ps.setInt(1, targetUserId);
        try (var rs = ps.executeQuery()) { if (rs.next()) stats.setActiveAuctions(rs.getInt(1)); }
      }

      // auctions sold and total earned
      try (var ps = conn.prepareStatement("SELECT COUNT(*), COALESCE(SUM(s.current_price), 0) FROM auction_session s JOIN item i ON s.item_id = i.item_id WHERE i.seller_id = ? AND s.status = 'SOLD' AND s.is_deleted = FALSE AND i.is_deleted = FALSE")) {
        ps.setInt(1, targetUserId);
        try (var rs = ps.executeQuery()) {
          if (rs.next()) {
            stats.setAuctionsSold(rs.getInt(1));
            stats.setTotalEarned(rs.getBigDecimal(2));
          }
        }
      }

      // total bids placed by user
      try (var ps = conn.prepareStatement("SELECT COUNT(*) FROM bid_transaction WHERE bidder_id = ?")) {
        ps.setInt(1, targetUserId);
        try (var rs = ps.executeQuery()) { if (rs.next()) stats.setTotalBidsPlaced(rs.getInt(1)); }
      }

      // total items sold (may equal auctionsSold)
      stats.setTotalItemsSold(stats.getAuctionsSold());

      // total spent: sum of negative wallet transactions (payments)
      try (var ps = conn.prepareStatement("SELECT COALESCE(SUM(CASE WHEN amount < 0 THEN -amount ELSE 0 END), 0) FROM wallet_transaction WHERE user_id = ?")) {
        ps.setInt(1, targetUserId);
        try (var rs = ps.executeQuery()) { if (rs.next()) stats.setTotalSpent(rs.getBigDecimal(1)); }
      }

      return stats;
    }
  }

  public void cancelAuction(int adminId, int sessionId) throws SQLException {
    checkAdminAccess(adminId);
    // Ủy quyền cho AuctionService xử lý Lock và Hoàn tiền (Refund)
    auctionService.cancelSession(sessionId, adminId);
  }

  /**
   * Thay đổi trạng thái tài khoản (khoá / mở / cấm vĩnh viễn).
   */
  public void updateUserStatus(int adminId, int targetUserId, UserStatus newStatus)
      throws SQLException {
      
    LOGGER.info("Admin {} yêu cầu đổi trạng thái của User {} thành {}", adminId, targetUserId, newStatus);

    // 1. Kiểm tra quyền Admin
    User admin = checkAdminAccess(adminId);

    if (adminId == targetUserId) {
      throw new ValidationException("Không thể tự thay đổi trạng thái của chính mình.");
    }

    // 2. Tải Mục tiêu (Target) ngay lập tức để kiểm tra bảo mật
    User targetUser = userDAO.findById(targetUserId)
        .orElseThrow(() -> new UserNotFoundException(targetUserId));

    // -- Bịt lỗ hổng: Không cho phép Admin cấp thấp thao tác lên Admin cấp cao
    if (targetUser.isAdmin()) {
      if (!(admin instanceof UserAdmin adminUser && adminUser.canBanUser())) {
          LOGGER.warn("Admin {} (Moderator) cố gắng thao tác lên Admin khác: {}", adminId, targetUserId);
          throw new UnauthorizedException("thay đổi trạng thái của Quản trị viên khác (chỉ Super Admin mới có quyền).");
      }
    }

    // 3. Phân quyền lệnh BANNED
    if (newStatus == UserStatus.BANNED) {
      if (!(admin instanceof UserAdmin adminUser && adminUser.canBanUser())) {
        LOGGER.warn("Admin {} cố gắng dùng lệnh BANNED trái quyền", adminId);
        throw new UnauthorizedException("cấm vĩnh viễn tài khoản (chỉ Super Admin mới được phép).");
      }
    }

    // 4. Tối ưu: Nếu trạng thái không đổi thì không cần gọi Database
    // (Giả sử Entity User của bạn có hàm getStatus())
    if (targetUser.getStatus() == newStatus) {
      LOGGER.debug("User {} đã ở trạng thái {}. Bỏ qua cập nhật DB.", targetUserId, newStatus);
      return; 
    }

    // 5. Thực thi
    userDAO.updateStatus(targetUserId, newStatus);
    LOGGER.info("Thành công: Admin {} đã đổi trạng thái User {} thành {}", adminId, targetUserId, newStatus);
  }

  // ===========================================================================
  // PRIVATE HELPERS
  // ===========================================================================

  private User checkAdminAccess(int adminId) throws SQLException {
    User admin = userDAO.findById(adminId)
        .orElseThrow(() -> new UserNotFoundException(adminId));
    if (!admin.isAdmin()) {
      LOGGER.warn("Cảnh báo bảo mật: User {} cố gắng dùng quyền Admin trái phép!", adminId);
      throw new UnauthorizedException("thực hiện thao tác quản trị");
    }
    return admin;
  }

  private int countSingle(Connection conn, String sql) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      if (rs.next()) return rs.getInt(1);
      return 0;
    }
  }

  private String resolveRole(String rolesCsv, String adminLevel) {
    if (adminLevel != null && !adminLevel.isBlank()) {
      return "SUPER_ADMIN".equalsIgnoreCase(adminLevel) ? UserAdminDTO.ROLE_SUPER_ADMIN : UserAdminDTO.ROLE_ADMIN;
    }
    if (rolesCsv == null) return UserAdminDTO.ROLE_MEMBER;
    String csv = rolesCsv.toUpperCase();
    if(csv.contains("SUPER_ADMIN")) return UserAdminDTO.ROLE_SUPER_ADMIN;
    if(csv.contains("ADMIN")) return UserAdminDTO.ROLE_ADMIN;
    if(csv.contains("SELLER")) return UserAdminDTO.ROLE_SELLER;
    return UserAdminDTO.ROLE_MEMBER;
  }

    private AuctionItemDTO buildItemDTO(ResultSet rs) throws SQLException {
    return AuctionItemDTO.of(
        rs.getInt("item_id"),
        rs.getString("title"),
        rs.getString("description"),
        parseEnum(ItemCategory.class, rs.getString("category"), ItemCategory.OTHER),
        parseEnum(ItemCondition.class, rs.getString("condition_type"), null),
        parseEnum(SessionStatus.class, rs.getString("session_status"), SessionStatus.UPCOMING),
        rs.getBigDecimal("current_price"),
        toInstant(rs.getTimestamp("end_time")),
        rs.getString("seller_username"),
        rs.getInt("session_id"),
        Collections.emptyMap(),
        Collections.emptyList(),
        0,
        toInstant(rs.getTimestamp("created_at"))
    );
  }

  private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
    if (value == null || value.isBlank()) return fallback;
    try { return Enum.valueOf(type, value.toUpperCase()); } catch (IllegalArgumentException ex) { return fallback; }
  }

  private static Instant toInstant(Timestamp ts) {
    return ts != null ? ts.toInstant() : null;
  }
}