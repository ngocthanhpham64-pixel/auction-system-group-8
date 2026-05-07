package vn.edu.vnu.uet.group8.server.service.user;

import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.uet.group8.common.entity.UserAdmin;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;
import vn.edu.vnu.uet.group8.common.exception.UnauthorizedException;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;

/**
 * Xử lý nghiệp vụ quản trị tài khoản — chỉ Admin được gọi.
 */
public class AdminUserService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserService.class);
  private final UserDAO userDAO;

  public AdminUserService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  /**
   * Thay đổi trạng thái tài khoản (khoá / mở / cấm vĩnh viễn).
   */
  public void updateUserStatus(int adminId, int targetUserId, UserStatus newStatus)
      throws SQLException {
      
    LOGGER.info("Admin {} yêu cầu đổi trạng thái của User {} thành {}", adminId, targetUserId, newStatus);

    // 1. Tải Admin
    User admin = userDAO.findById(adminId)
        .orElseThrow(() -> new UserNotFoundException(adminId));

    if (!admin.isAdmin()) {
      LOGGER.warn("Cảnh báo bảo mật: User {} cố gắng dùng quyền Admin trái phép!", adminId);
      throw new UnauthorizedException("thay đổi trạng thái tài khoản");
    }

    if (adminId == targetUserId) {
      throw new ValidationException("Không thể tự thay đổi trạng thái của chính mình.");
    }

    // 2. Tải Mục tiêu (Target) ngay lập tức để kiểm tra bảo mật
    User targetUser = userDAO.findById(targetUserId)
        .orElseThrow(() -> new UserNotFoundException(targetUserId));

    // -- Bịt lỗ hổng: Không cho phép Admin cấp thấp thao tác lên Admin cấp cao
    if (targetUser.isAdmin()) {
      if (!(admin instanceof UserAdmin UserAdmin && UserAdmin.canBanUser())) {
          LOGGER.warn("Admin {} (Moderator) cố gắng thao tác lên Admin khác: {}", adminId, targetUserId);
          throw new UnauthorizedException("thay đổi trạng thái của Quản trị viên khác (chỉ Super Admin mới có quyền).");
      }
    }

    // 3. Phân quyền lệnh BANNED
    if (newStatus == UserStatus.BANNED) {
      if (!(admin instanceof UserAdmin UserAdmin && UserAdmin.canBanUser())) {
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
}