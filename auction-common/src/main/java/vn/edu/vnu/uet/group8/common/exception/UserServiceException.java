// ── Base exception cho toàn bộ nghiệp vụ User ────────────
package vn.edu.vnu.uet.group8.common.exception;

/**
 * Base exception cho mọi lỗi nghiệp vụ của User.
 * ClientHandler bắt exception này để build ResponseDTO.fail()
 */
public class UserServiceException extends RuntimeException {
    public UserServiceException(String message) {
        super(message);
    }
}