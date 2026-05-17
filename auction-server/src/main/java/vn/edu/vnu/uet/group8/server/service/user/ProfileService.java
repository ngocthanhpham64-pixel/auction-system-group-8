package vn.edu.vnu.uet.group8.server.service.user;

import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.dto.model.UserProfileDTO;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.UserAdmin;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.exception.UnauthorizedException;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.util.FileUtil;

/**
 * Xử lý nghiệp vụ xem và cập nhật thông tin cá nhân.
 */
public class ProfileService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProfileService.class);
  private final UserDAO userDAO;

  public ProfileService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  /**
   * Lấy thông tin profile.
   */
  public UserProfileDTO getProfile(int requesterId, int targetId) throws SQLException {
    LOGGER.debug(
      "Bắt đầu xử lý yêu cầu xem profile - Requester: {}, Target: {}", requesterId, targetId);

    User target = userDAO.findById(targetId)
        .orElseThrow(() -> {
          LOGGER.warn("Không tìm thấy profile mục tiêu: {}", targetId);
          return new UserNotFoundException(targetId);
        });
    User requester = userDAO.findById(requesterId)
          .orElseThrow(() -> {
          LOGGER.warn("Không tìm thấy profile người yêu cầu: {}", targetId);
          return new UserNotFoundException(requesterId);
      });

    if (target instanceof UserAdmin targetAdmin) {
      // 1. Target là Admin: Chỉ Admin khác mới được xem (và phải có cấp bậc >=)
      if (!(requester instanceof UserAdmin requesterAdmin)) {
        LOGGER.warn(
          "Cảnh báo bảo mật: User {} (thường) cố gắng xem profile của Admin {}", 
          requesterId, targetId);
        throw new UserNotFoundException(targetId); // Ẩn hoàn toàn Admin với người dùng thường
      }
      
      // So sánh cấp bậc Admin (Dựa vào Enum ordinal: MODERATOR < SUPER_ADMIN)
      if (targetAdmin.getAdminLevel().compareTo(requesterAdmin.getAdminLevel()) > 0) {
        LOGGER.warn(
          "Cảnh báo bảo mật: Admin {} (cấp thấp) cố gắng xem profile Admin {} (cấp cao)", 
          requesterId, targetId);
        throw new UserNotFoundException(targetId);
      }
      
      return UserProfileDTO.fromAdmin(targetAdmin);
      
    } else if (target instanceof UserMember targetMember) {
      // 2. Target là Member
      if (requesterId == targetId) {
        return UserProfileDTO.fromMember(targetMember); // Tự xem chính mình
      }
      
      if (requester instanceof UserAdmin) {
        return UserProfileDTO.fromMemberForAdmin(targetMember); // Admin xem Member
      } else {
        return UserProfileDTO.fromMemberForOther(targetMember); // Member xem Member khác (Public Profile)
      }
    }

    LOGGER.error("Lỗi hệ thống: Không nhận diện được kiểu dữ liệu của User {}", targetId);
    throw new IllegalStateException("Unknown user type: " + target.getClass().getSimpleName());
  }

  /**
   * Cập nhật thông tin cá nhân.
   */
  public void updateProfile(int requesterId, int targetId, 
            String fullname, String phone, String address, String avatarUrl) throws SQLException {
    LOGGER.info(
      "Bắt đầu xử lý yêu cầu cập nhật profile - Requester: {}, Target: {}", requesterId, targetId);

    // 1. Kiểm tra quyền hạn: Chỉ chính chủ HOẶC Super Admin mới được phép sửa
    User requester = userDAO.findById(requesterId)
        .orElseThrow(() -> new UserNotFoundException(requesterId));

    boolean isSuperAdmin = requester instanceof UserAdmin admin 
        && admin.getAdminLevel() == vn.edu.vnu.uet.group8.common.enums.AdminLevel.SUPER_ADMIN;

    if (requesterId != targetId && !isSuperAdmin) {
      LOGGER.warn(
        "Cảnh báo bảo mật: User {} cố gắng sửa profile của User {}", requesterId, targetId);
      throw new UnauthorizedException("sửa profile của người dùng khác");
    }

    // 2. Bịt lỗ hổng dữ liệu đầu vào (Validation) trước khi gọi DB
    if (fullname == null || fullname.trim().isEmpty()) {
      throw new ValidationException("Họ tên không được để trống.");
    }
    if (fullname.length() > 50) {
      throw new ValidationException("Họ tên không được vượt quá 50 ký tự.");
    }

    UserPolicy.validatePhone(phone.trim());

    // 3. Tải User và thực thi
    User targetUser = userDAO.findById(targetId)
        .orElseThrow(() -> new UserNotFoundException(targetId));

    if (!(targetUser instanceof UserMember member)) {
      throw new ValidationException("Admin không có thông tin profile cá nhân để cập nhật.");
    }

    // Cập nhật thực thể (Nhớ trim() khoảng trắng thừa do người dùng lỡ nhập)
    member.setFullname(fullname.trim());
    member.setPhone(phone);
    if (address != null && !address.trim().isEmpty()) {
      member.setAddress(address.trim());
    }
    if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
      String url = avatarUrl.trim();
      if (!url.startsWith("http")) {
        List<String> savedUrls = FileUtil.saveBase64Images(List.of(url));
        if (!savedUrls.isEmpty()) {
          url = savedUrls.get(0);
        }
      }
      member.setAvatarUrl(url);
    }

    userDAO.updateProfile(member);
    LOGGER.info(
      "Cập nhật profile thành công cho User: {} bởi Requester: {}", targetId, requesterId);
  }

  public boolean deleteAccount(int userId) {
    try {
      userDAO.softDelete(userId);
      return true;
    } catch (UserNotFoundException e) {
      LOGGER.warn("Không tìm thấy tài khoản tài khoản, userId=" + userId);
      return false;
    }catch (SQLException e) {
      LOGGER.warn("Có lỗi khi cố xóa tài khoản + " + e.getMessage());
      return false;
    }
  }
}