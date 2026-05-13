package vn.edu.vnu.uet.group8.server.auth;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quản lý session tokens cho user đã đăng nhập.
 *
 * <p>Mỗi lần login thành công, sinh 1 token UUID và map về userId.
 * Mỗi request authenticated sau đó phải kèm token, được validate qua đây.
 *
 * <p>Thread-safe nhờ ConcurrentHashMap.
 */
public class SessionManager {
  private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

  /** Map token → userId */
  private final ConcurrentHashMap<String, Integer> tokenToUserId = new ConcurrentHashMap<>();

  /** Map userId → token (để invalidate khi user login lại) */
  private final ConcurrentHashMap<Integer, String> userIdToToken = new ConcurrentHashMap<>();

  /**
   * Sinh token mới cho user vừa login.
   * Nếu user đã có token cũ → xóa token cũ trước (single session per user).
   *
   * @param userId id user vừa login
   * @return token UUID mới
   */
  public String createSession(int userId) {
    // Nếu user đã có session cũ, invalidate nó
    String oldToken = userIdToToken.remove(userId);
    if (oldToken != null) {
      tokenToUserId.remove(oldToken);
      log.debug("Invalidate old token cho userId={}", userId);
    }

    String newToken = UUID.randomUUID().toString();
    tokenToUserId.put(newToken, userId);
    userIdToToken.put(userId, newToken);
    log.info("Tạo session mới cho userId={}", userId);
    return newToken;
  }

  /**
   * Validate token và trả userId tương ứng.
   *
   * @param token token client gửi lên
   * @return userId nếu token hợp lệ, -1 nếu không
   */
  public int validateToken(String token) {
    if (token == null || token.isBlank()) return -1;
    Integer userId = tokenToUserId.get(token);
    return userId != null ? userId : -1;
  }

  /**
   * Xóa session khi user logout hoặc bị kick.
   *
   * @param token token cần xóa
   */
  public void invalidateToken(String token) {
    if (token == null) return;
    Integer userId = tokenToUserId.remove(token);
    if (userId != null) {
      userIdToToken.remove(userId);
      log.info("Logout userId={}", userId);
    }
  }

  /**
   * Xóa session theo userId (admin force logout).
   */
  public void invalidateUser(int userId) {
    String token = userIdToToken.remove(userId);
    if (token != null) {
      tokenToUserId.remove(token);
      log.info("Force logout userId={}", userId);
    }
  }

  /** Số session đang active — phục vụ monitoring. */
  public int activeSessionCount() {
    return tokenToUserId.size();
  }
}
