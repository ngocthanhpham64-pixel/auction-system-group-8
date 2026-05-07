// ── Nạp tiền trùng lặp (Idempotency) ─────────────────────
package vn.edu.vnu.uet.group8.common.exception;

public class DuplicateTransactionException
        extends UserServiceException {
  public DuplicateTransactionException(String transactionId) {
    super("Giao dịch '" + transactionId
        + "' đã được xử lý trước đó");
  }
}