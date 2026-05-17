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
      int activeAuctions = countSingle(conn, "SELECT COUNT(*) FROM auction_session WHERE status = 'ACTIVE' AND is_deleted = FALSE");
      int totalUsers = countSingle(conn, "SELECT COUNT(*) FROM users WHERE is_deleted = FALSE");
      int totalBids = countSingle(conn, "SELECT COUNT(*) FROM bid_transaction");

      BigDecimal totalRevenue = BigDecimal.ZERO;
      try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(current_price), 0) FROM auction_session WHERE status = 'SOLD' AND is_deleted = FALSE");
           ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          BigDecimal sum = rs.getBigDecimal(1);
          if (sum != null) totalRevenue = sum;
        }
      }
      return new AdminStatsDTO(activeAuctions, totalUsers, totalRevenue, totalBids);
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
    String sql = "SELECT i.item_id, i.title, i.description, i.category, i.condition_type, "
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
    return rolesCsv.toUpperCase().contains("SELLER") ? UserAdminDTO.ROLE_SELLER : UserAdminDTO.ROLE_MEMBER;
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