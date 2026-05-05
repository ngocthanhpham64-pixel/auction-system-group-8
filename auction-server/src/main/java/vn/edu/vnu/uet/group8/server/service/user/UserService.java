package vn.edu.vnu.uet.group8.server.service.user;

import vn.edu.vnu.uet.group8.common.dto.model.UserProfileDTO;
import vn.edu.vnu.uet.group8.common.dto.model.UserSummaryDTO;
import vn.edu.vnu.uet.group8.common.entity.UserAdmin;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;
import vn.edu.vnu.uet.group8.common.exception.*;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.util.PasswordUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Service xử lý nghiệp vụ liên quan đến User.
 *
 * Nguyên tắc thiết kế:
 *
 *   1. KHÔNG trả ResponseDTO — ném exception khi có lỗi.
 *      ClientHandler bắt exception rồi tự build ResponseDTO.
 *      Service không biết gì về giao thức truyền tin.
 *
 *   2. Mọi lỗi đều là exception có type rõ ràng.
 *      ClientHandler không cần kiểm tra isSuccess() — chỉ cần
 *      catch đúng exception để build message phù hợp.
 *
 *   3. Validation tập trung trong UserPolicy.
 *      Service không chứa if-else kiểm tra format.
 */
public class UserService {

    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // ════════════════════════════════════════════════════
    // ĐĂNG KÝ
    // ════════════════════════════════════════════════════

    /**
     * Đăng ký tài khoản mới.
     *
     * @return UserSummaryDTO để Client tự động login sau đăng ký
     * @throws ValidationException     input không hợp lệ
     * @throws DuplicateUserException  username hoặc email đã tồn tại
     * @throws SQLException            lỗi DB
     */
    public UserSummaryDTO register(
            String username, String email,
            String password, String fullName)
            throws SQLException {

        // ── Validate format qua UserPolicy ───────────────
        // Mỗi method trả null nếu hợp lệ, message nếu không
        rejectIfInvalid(UserPolicy.validateUsername(username));
        rejectIfInvalid(UserPolicy.validateEmail(email));
        rejectIfInvalid(UserPolicy.validatePassword(password));

        // ── Normalize input ───────────────────────────────
        String normalUsername = username.trim().toLowerCase();
        String normalEmail    = email.trim().toLowerCase();

        // ── Kiểm tra trùng ───────────────────────────────
        if (userDAO.existsByUsername(normalUsername))
            throw new DuplicateUserException("Username", normalUsername);

        if (userDAO.existsByEmail(normalEmail))
            throw new DuplicateUserException("Email", normalEmail);

        // ── Hash password ────────────────────────────────
        String hashedPassword = PasswordUtil.hash(password);

        // ── Tạo Entity qua Builder ───────────────────────
        UserMember newUser = UserMember.builder(
                normalUsername, normalEmail, hashedPassword)
            .fullName(fullName != null ? fullName.trim() : "")
            .build();

        // ── Lưu DB — DAO gán ID về Entity ────────────────
        userDAO.insert(newUser);

        return UserSummaryDTO.from(newUser);
    }

    // ════════════════════════════════════════════════════
    // ĐĂNG NHẬP
    // ════════════════════════════════════════════════════

    /**
     * Đăng nhập bằng email + password.
     *
     * @return UserSummaryDTO nếu thành công
     * @throws ValidationException        input trống
     * @throws InvalidCredentialsException email hoặc password sai
     * @throws AccountLockedException      tài khoản bị khoá/cấm
     * @throws SQLException               lỗi DB
     */
    public UserSummaryDTO login(String email, String password)
            throws SQLException {

        // Validate cơ bản — không dùng UserPolicy vì đây
        // là kiểm tra "có trống không", không phải format
        if (email == null || email.isBlank())
            throw new ValidationException(
                "Email không được để trống");

        if (password == null || password.isBlank())
            throw new ValidationException(
                "Mật khẩu không được để trống");

        // ── Tìm user và verify password ──────────────────
        // authenticate() trả empty nếu email sai HOẶC password sai
        // Không phân biệt hai trường hợp — tránh lộ thông tin
        Optional<User> opt = userDAO.authenticate(
            email.trim().toLowerCase(), password);

        if (opt.isEmpty())
            throw new InvalidCredentialsException();

        User user = opt.get();

        // ── Kiểm tra trạng thái tài khoản ────────────────
        // Kiểm tra sau khi verify password thành công
        // để không lộ "email này có tồn tại không"
        if (user.getStatus() != UserStatus.ACTIVE)
            throw new AccountLockedException(user.getStatus());

        // ── Ghi nhận lastLogin ────────────────────────────
        // Không fail login nếu update lastLogin bị lỗi
        try {
            userDAO.updateLastLogin(user.getId());
            user.recordLogin();
        } catch (SQLException e) {
            System.err.println("[WARN] Không ghi được lastLogin "
                + "cho userId=" + user.getId()
                + ": " + e.getMessage());
        }

        return UserSummaryDTO.from(user);
    }

    // ════════════════════════════════════════════════════
    // ĐỔI MẬT KHẨU
    // ════════════════════════════════════════════════════

    /**
     * Đổi mật khẩu — yêu cầu nhập mật khẩu cũ để xác nhận.
     *
     * @throws UserNotFoundException       user không tồn tại
     * @throws InvalidCredentialsException mật khẩu cũ sai
     * @throws ValidationException         mật khẩu mới không hợp lệ
     * @throws SQLException                lỗi DB
     */
    public void changePassword(
            int userId, String oldPassword,
            String newPassword) throws SQLException {

        User user = findUserOrThrow(userId);

        // ── Verify mật khẩu cũ ───────────────────────────
        if (!PasswordUtil.verify(oldPassword,
                user.getEncryptedPassword()))
            throw new InvalidCredentialsException();

        // ── Validate mật khẩu mới qua UserPolicy ─────────
        rejectIfInvalid(UserPolicy.validatePassword(newPassword));

        // ── Không cho đặt lại mật khẩu giống cũ ─────────
        if (PasswordUtil.verify(newPassword,
                user.getEncryptedPassword()))
            throw new ValidationException(
                "Mật khẩu mới phải khác mật khẩu cũ");

        // ── Lưu mật khẩu mới — DAO tự hash bên trong ────
        userDAO.updatePassword(userId, newPassword);
    }

    // ════════════════════════════════════════════════════
    // NẠP TIỀN — Có Idempotency Key
    // ════════════════════════════════════════════════════

    /**
     * Nạp tiền vào ví.
     *
     * Idempotency: Client tạo transactionId duy nhất mỗi lần
     * bấm nút. Nếu cùng transactionId được gửi lần 2 (do lag,
     * retry), Server trả lại balance hiện tại mà không cộng thêm.
     *
     * @param transactionId ID duy nhất do Client tạo (UUID)
     * @return BigDecimal balance sau khi nạp
     * @throws ValidationException           số tiền không hợp lệ
     * @throws DuplicateTransactionException giao dịch đã xử lý
     * @throws UserNotFoundException         user không tồn tại
     * @throws SQLException                  lỗi DB
     */
    public BigDecimal topUpBalance(
            int userId, BigDecimal amount,
            String transactionId) throws SQLException {

        // ── Validate số tiền qua UserPolicy ──────────────
        rejectIfInvalid(UserPolicy.validateTopUpAmount(amount));

        // ── Idempotency check ─────────────────────────────
        // Kiểm tra transactionId đã được xử lý chưa
        if (transactionId == null || transactionId.isBlank())
            throw new ValidationException(
                "transactionId không được để trống");

        if (userDAO.existsTransaction(transactionId))
            throw new DuplicateTransactionException(transactionId);

        // ── Kiểm tra user tồn tại ────────────────────────
        // findUserOrThrow chỉ cần để check tồn tại
        // không cần load toàn bộ User object
        findUserOrThrow(userId);

        // ── Kiểm tra balance tổng không vượt giới hạn ────
        BigDecimal currentBalance =
            userDAO.getBalanceById(userId);

        if (currentBalance.add(amount)
                .compareTo(UserPolicy.BALANCE_MAX_TOTAL) > 0)
            throw new ValidationException(
                "Số dư ví không được vượt quá "
                + UserPolicy.BALANCE_MAX_TOTAL + " VND");

        // ── Lưu transaction trước khi update balance ──────
        // Lưu trước để tránh race condition:
        // nếu update balance thành công nhưng lưu transaction thất bại
        // lần sau vẫn có thể xử lý lại
        userDAO.insertTransaction(transactionId, userId, amount);

        // ── Update balance — trả về giá trị mới từ SQL ───
        // Không đọc lại bằng findById() — tránh Read-after-Write
        // SQL: UPDATE SET balance = balance + ? RETURNING balance
        BigDecimal newBalance =
            userDAO.updateBalanceAndReturn(userId, amount);

        return newBalance;
    }

    // ════════════════════════════════════════════════════
    // XEM PROFILE
    // ════════════════════════════════════════════════════

    /**
     * Lấy profile của user.
     * User chỉ xem được của mình, Admin xem được của bất kỳ ai.
     *
     * Dùng if-else instanceof thay vì switch pattern matching
     * để tương thích Java 17.
     *
     * @throws UserNotFoundException  user không tồn tại
     * @throws UnauthorizedException  không có quyền xem
     * @throws SQLException           lỗi DB
     */
    public UserProfileDTO getProfile(
            int requesterId, int targetId)
            throws SQLException {

        // ── Kiểm tra quyền truy cập ───────────────────────
        // Load requester tối thiểu — chỉ cần biết isAdmin()
        User requester = findUserOrThrow(requesterId);

        if (requesterId != targetId && !requester.isAdmin())
            throw new UnauthorizedException(
                "xem profile của người dùng khác");

        // ── Load target user ──────────────────────────────
        // Dùng findProfileById() — chỉ SELECT cột cần thiết
        // Không SELECT password_hash — không cần, tránh lộ data
        User target = userDAO.findProfileById(targetId)
            .orElseThrow(
                () -> new UserNotFoundException(targetId));

        // ── Chọn factory method phù hợp — Java 17 style ──
        // Không dùng switch pattern matching (Java 21)
        // Dùng if-else instanceof (Java 17)
        if (target instanceof UserMember m) {
            if (requesterId == targetId) {
                // Chủ tài khoản xem của mình — có balance
                return UserProfileDTO.fromMember(m);
            } else {
                // Admin xem — không có balance
                return UserProfileDTO.fromMemberForAdmin(m);
            }
        } else if (target instanceof UserAdmin a) {
            return UserProfileDTO.fromAdmin(a);
        } else {
            // Không bao giờ xảy ra — nhưng phải handle
            throw new IllegalStateException(
                "Unknown user type: "
                + target.getClass().getSimpleName());
        }
    }

    // ════════════════════════════════════════════════════
    // CẬP NHẬT PROFILE
    // ════════════════════════════════════════════════════

    /**
     * Cập nhật thông tin cá nhân — chỉ UserMember.
     * Admin không có fullName/phone nên không dùng method này.
     *
     * @throws UserNotFoundException  user không tồn tại
     * @throws UnauthorizedException  không phải chủ tài khoản
     * @throws ValidationException    dữ liệu không hợp lệ
     * @throws SQLException           lỗi DB
     */
    public void updateProfile(
            int requesterId, int targetId,
            String fullName, String phone)
            throws SQLException {

        // Chỉ chính chủ mới được sửa profile của mình
        if (requesterId != targetId)
            throw new UnauthorizedException(
                "sửa profile của người dùng khác");

        User user = findUserOrThrow(targetId);

        // Admin không có profile kiểu UserMember
        if (!(user instanceof UserMember))
            throw new ValidationException(
                "Admin không có thông tin profile");

        UserMember member = (UserMember) user;
        member.setFullname(fullName);
        member.setPhone(phone);

        userDAO.updateProfile(member);
    }

    // ════════════════════════════════════════════════════
    // ADMIN — QUẢN LÝ USER
    // ════════════════════════════════════════════════════

    /**
     * Admin thay đổi trạng thái tài khoản (khoá/mở/cấm).
     *
     * @throws UserNotFoundException  user không tồn tại
     * @throws UnauthorizedException  không phải admin
     * @throws ValidationException    logic không hợp lệ
     * @throws SQLException           lỗi DB
     */
    public void updateUserStatus(
            int adminId, int targetUserId,
            UserStatus newStatus) throws SQLException {

        User admin = findUserOrThrow(adminId);

        // Kiểm tra quyền admin
        if (!admin.isAdmin())
            throw new UnauthorizedException(
                "thay đổi trạng thái tài khoản");

        // Không cho tự khoá chính mình
        if (adminId == targetUserId)
            throw new ValidationException(
                "Không thể thay đổi trạng thái "
                + "của chính mình");

        // Chỉ SUPER_ADMIN mới BAN được vĩnh viễn
        if (newStatus == UserStatus.BANNED) {
            if (!(admin instanceof UserAdmin))
                throw new UnauthorizedException("cấm vĩnh viễn");

            UserAdmin UserAdmin = (UserAdmin) admin;
            if (!UserAdmin.canBanUser())
                throw new UnauthorizedException(
                    "cấm vĩnh viễn — chỉ Super Admin");
        }

        // Kiểm tra target tồn tại trước khi update
        findUserOrThrow(targetUserId);

        userDAO.updateStatus(targetUserId, newStatus);
    }

    // ════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ════════════════════════════════════════════════════

    /**
     * Tìm user theo ID hoặc ném UserNotFoundException.
     * Dùng ở mọi nơi cần đảm bảo user tồn tại.
     */
    private User findUserOrThrow(int userId)
            throws SQLException {
        return userDAO.findById(userId)
            .orElseThrow(
                () -> new UserNotFoundException(userId));
    }

    /**
     * Ném ValidationException nếu message không null.
     * Dùng cùng với UserPolicy.validateXxx() để code gọn hơn.
     *
     * Thay vì:
     *   String err = UserPolicy.validateUsername(u);
     *   if (err != null) throw new ValidationException(err);
     *
     * Viết gọn:
     *   rejectIfInvalid(UserPolicy.validateUsername(u));
     */
    private void rejectIfInvalid(String errorMessage) {
        if (errorMessage != null)
            throw new ValidationException(errorMessage);
    }
}