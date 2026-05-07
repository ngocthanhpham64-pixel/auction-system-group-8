package vn.edu.vnu.uet.group8.server.network;

import com.google.gson.Gson;
import vn.edu.vnu.uet.group8.common.dto.ItemDTO;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.UserRole;
import vn.edu.vnu.uet.group8.server.dao.ItemDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.service.AuctionService;
import vn.edu.vnu.uet.group8.server.service.BidObserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Xử lý toàn bộ giao tiếp với MỘT client cụ thể.
 *
 * <p>Chạy trên luồng riêng (Thread per client).
 * Implements {@link BidObserver} để nhận thông báo giá mới
 * từ {@link AuctionService} và đẩy ngay xuống client qua socket.</p>
 *
 * <h3>Protocol (text-based, mỗi lệnh một dòng):</h3>
 * <pre>
 * Client → Server:
 *   LOGIN|email|password
 *   REGISTER|username|email|password
 *   GET_ITEMS
 *   GET_ITEMS_BY_CATEGORY|ELECTRONICS
 *   GET_AUCTION|auctionId
 *   BID|auctionId|price
 *   QUIT
 *
 * Server → Client:
 *   OK|payload                  – thành công, payload là JSON hoặc văn bản
 *   ERROR|mã lỗi|mô tả          – thất bại
 *   PUSH_BID|auctionId|price|winner  – server chủ động đẩy khi có bid mới
 * </pre>
 */
public class ClientHandler implements Runnable, BidObserver {

  // ─── Dependencies (inject qua constructor) ───────────────────────────────
  private static final Gson GSON       = new Gson();
  private static final UserDAO    userDAO    = new UserDAO();
  private static final ItemDAO    itemDAO    = new ItemDAO();
  // AuctionService là singleton dùng chung toàn server
  private static final AuctionService auctionService = AuctionService.getInstance();

  // ─── Trạng thái của handler này ─────────────────────────────────────────
  private final Socket      socket;
  private PrintWriter       out;
  private BufferedReader    in;

  /** User đang đăng nhập trên kết nối này. Null nếu chưa LOGIN. */
  private User currentUser = null;

  // ════════════════════════════════════════════════════════════════════════
  // CONSTRUCTOR
  // ════════════════════════════════════════════════════════════════════════

  public ClientHandler(Socket socket) {
    this.socket = socket;
  }

  // ════════════════════════════════════════════════════════════════════════
  // VÒNG LẶP CHÍNH — đọc lệnh từ client, dispatch sang handler tương ứng
  // ════════════════════════════════════════════════════════════════════════

  @Override
  public void run() {
    try {
      out = new PrintWriter(socket.getOutputStream(), true);
      in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      // Đăng ký để nhận thông báo bid từ AuctionService
      auctionService.addObserver(this);

      String line;
      while ((line = in.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty()) continue;

        System.out.printf("[ClientHandler] %s → %s%n",
                socket.getInetAddress(), line);

        if (line.equals("QUIT")) {
          sendOk("BYE");
          break;
        }

        dispatch(line);
      }

    } catch (IOException e) {
      System.err.println("[ClientHandler] Mất kết nối: " + e.getMessage());
    } finally {
      cleanup();
    }
  }

  /**
   * Phân tích lệnh và gọi handler tương ứng.
   * Mọi lỗi bất ngờ đều bị bắt ở đây — không để crash luồng.
   */
  private void dispatch(String rawLine) {
    // Protocol: lệnh|tham1|tham2|...
    String[] parts = rawLine.split("\\|", -1);
    String cmd = parts[0].toUpperCase();

    try {
      switch (cmd) {
        case "LOGIN"                -> handleLogin(parts);
        case "REGISTER"             -> handleRegister(parts);
        case "GET_ITEMS"            -> handleGetItems();
        case "GET_ITEMS_BY_CATEGORY"-> handleGetItemsByCategory(parts);
        case "GET_AUCTION"          -> handleGetAuction(parts);
        case "BID"                  -> handleBid(parts);
        default                     -> sendError("UNKNOWN_CMD",
                "Lệnh không hợp lệ: " + cmd);
      }
    } catch (SQLException e) {
      System.err.println("[ClientHandler] DB error: " + e.getMessage());
      sendError("DB_ERROR", "Lỗi cơ sở dữ liệu, vui lòng thử lại");
    } catch (Exception e) {
      System.err.println("[ClientHandler] Unexpected: " + e.getMessage());
      sendError("SERVER_ERROR", "Lỗi máy chủ nội bộ");
    }
  }

  // ════════════════════════════════════════════════════════════════════════
  // HANDLERS — mỗi lệnh một method
  // ════════════════════════════════════════════════════════════════════════

  /**
   * LOGIN|email|password
   *
   * Thành công → OK|{"username":"...","balance":...}
   * Thất bại   → ERROR|AUTH_FAILED|Sai email hoặc mật khẩu
   */
  private void handleLogin(String[] parts) throws SQLException {
    if (parts.length < 3) {
      sendError("BAD_ARGS", "Cú pháp: LOGIN|email|password");
      return;
    }

    String email    = parts[1].trim();
    String password = parts[2].trim();

    Optional<User> opt = userDAO.authenticate(email, password);

    if (opt.isEmpty()) {
      sendError("AUTH_FAILED", "Sai email hoặc mật khẩu");
      return;
    }

    currentUser = opt.get();
    userDAO.updateLastLogin(currentUser.getId());

    // Chỉ trả về thông tin an toàn, không kèm passwordHash
    String payload = GSON.toJson(buildUserPayload(currentUser));
    sendOk(payload);

    System.out.printf("[ClientHandler] User đã đăng nhập: %s%n",
            currentUser.getUsername());
  }

  /**
   * REGISTER|username|email|password
   *
   * Thành công → OK|Đăng ký thành công
   * Thất bại   → ERROR|DUPLICATE_EMAIL|Email đã tồn tại
   */
  private void handleRegister(String[] parts) throws SQLException {
    if (parts.length < 4) {
      sendError("BAD_ARGS", "Cú pháp: REGISTER|username|email|password");
      return;
    }

    String username = parts[1].trim();
    String email    = parts[2].trim();
    String password = parts[3].trim();

    if (userDAO.existsByEmail(email)) {
      sendError("DUPLICATE_EMAIL", "Email đã được sử dụng");
      return;
    }
    if (userDAO.existsByUsername(username)) {
      sendError("DUPLICATE_USERNAME", "Tên đăng nhập đã tồn tại");
      return;
    }

    // Hash password và tạo UserMember mới
    String hashedPw = vn.edu.vnu.uet.group8.common.utilclass.PasswordUtil.hash(password);
    User newUser = new UserMember.Builder(username, email, hashedPw).build();
    userDAO.insert(newUser);

    sendOk("Đăng ký thành công");
  }

  /**
   * GET_ITEMS
   *
   * Trả về JSON array các ItemDTO đang ACTIVE.
   * Thành công → OK|[{...},{...}]
   */
  private void handleGetItems() throws SQLException {
    requireLogin();

    List<ItemDTO> items = itemDAO.findAllActive()
            .stream()
            .map(ItemDTO::from)
            .toList();

    sendOk(GSON.toJson(items));
  }

  /**
   * GET_ITEMS_BY_CATEGORY|ELECTRONICS
   *
   * Thành công → OK|[{...}]
   * Thất bại   → ERROR|INVALID_CATEGORY|...
   */
  private void handleGetItemsByCategory(String[] parts) throws SQLException {
    requireLogin();

    if (parts.length < 2) {
      sendError("BAD_ARGS", "Cú pháp: GET_ITEMS_BY_CATEGORY|CATEGORY");
      return;
    }

    try {
      var category = vn.edu.vnu.uet.group8.common.enums.ItemCategory
              .valueOf(parts[1].trim().toUpperCase());

      List<ItemDTO> items = itemDAO.findActiveByCategory(category)
              .stream()
              .map(ItemDTO::from)
              .toList();

      sendOk(GSON.toJson(items));

    } catch (IllegalArgumentException e) {
      sendError("INVALID_CATEGORY", "Danh mục không tồn tại: " + parts[1]);
    }
  }

  /**
   * GET_AUCTION|auctionId
   *
   * Trả về trạng thái phiên đấu giá đang chạy in-memory.
   * Thành công → OK|{"id":"...","currentPrice":...,"highestBidder":"...","status":"..."}
   * Thất bại   → ERROR|NOT_FOUND|...
   */
  private void handleGetAuction(String[] parts) throws SQLException {
    requireLogin();

    if (parts.length < 2) {
      sendError("BAD_ARGS", "Cú pháp: GET_AUCTION|auctionId");
      return;
    }

    String auctionId = parts[1].trim();
    var auction = auctionService.getAuction(auctionId);

    if (auction == null) {
      sendError("NOT_FOUND", "Không tìm thấy phiên đấu giá: " + auctionId);
      return;
    }

    sendOk(GSON.toJson(auction));
  }

  /**
   * BID|auctionId|price
   *
   * Đặt giá — yêu cầu đã đăng nhập.
   * Kiểm tra số dư nếu user là UserMember.
   *
   * Thành công → OK|Đặt giá thành công
   * Thất bại   → ERROR|BID_REJECTED|...
   */
  private void handleBid(String[] parts) throws SQLException {
    requireLogin();

    if (parts.length < 3) {
      sendError("BAD_ARGS", "Cú pháp: BID|auctionId|price");
      return;
    }

    String auctionId = parts[1].trim();
    double price;
    try {
      price = Double.parseDouble(parts[2].trim());
    } catch (NumberFormatException e) {
      sendError("BAD_ARGS", "Giá phải là số hợp lệ");
      return;
    }

    if (price <= 0) {
      sendError("BID_REJECTED", "Giá đặt phải lớn hơn 0");
      return;
    }

    // Kiểm tra số dư nếu là UserMember
    if (currentUser instanceof UserMember member) {
      if (member.getBalance() == null
              || member.getBalance().doubleValue() < price) {
        sendError("INSUFFICIENT_BALANCE",
                "Số dư không đủ để đặt giá " + price);
        return;
      }
    }

    boolean success = auctionService.placeBid(
            auctionId, price, currentUser.getUsername());

    if (success) {
      sendOk("Đặt giá thành công");
    } else {
      sendError("BID_REJECTED",
              "Giá đặt phải cao hơn giá hiện tại hoặc phiên đã kết thúc");
    }
  }

  // ════════════════════════════════════════════════════════════════════════
  // BidObserver — AuctionService gọi method này khi có bid mới
  // ════════════════════════════════════════════════════════════════════════

  /**
   * Được gọi từ AuctionService (trên luồng của người đặt giá).
   * Push ngay xuống client của handler này mà không cần lock thêm
   * vì PrintWriter đã flush=true và write là atomic với từng println.
   */
  @Override
  public void onBidUpdated(String auctionId, double newPrice, String winner) {
    // Không gửi lại thông báo cho chính người vừa thắng bid
    if (currentUser != null
            && currentUser.getUsername().equals(winner)) return;

    if (out != null) {
      out.println("PUSH_BID|" + auctionId + "|" + newPrice + "|" + winner);
    }
  }

  // ════════════════════════════════════════════════════════════════════════
  // HELPERS
  // ════════════════════════════════════════════════════════════════════════

  /** Ném exception nếu client chưa đăng nhập. */
  private void requireLogin() {
    if (currentUser == null) {
      sendError("UNAUTHORIZED", "Vui lòng đăng nhập trước");
      throw new IllegalStateException("Client chưa đăng nhập");
    }
  }

  /** Gửi phản hồi thành công: {@code OK|payload} */
  private void sendOk(String payload) {
    out.println("OK|" + payload);
  }

  /** Gửi phản hồi lỗi: {@code ERROR|code|message} */
  private void sendError(String code, String message) {
    out.println("ERROR|" + code + "|" + message);
  }

  /**
   * Tạo payload JSON an toàn từ User — không kèm passwordHash.
   * Dùng anonymous record cho gọn, tránh tạo thêm DTO class.
   */
  private Object buildUserPayload(User user) {
    record UserPayload(int id, String username, String email,
                       String fullName, String roles, Object balance) {}

    String balance = (user instanceof UserMember m && m.getBalance() != null)
            ? m.getBalance().toPlainString()
            : "0";

    return new UserPayload(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            user.getRoles().toString(),
            balance
    );
  }

  /** Dọn dẹp tài nguyên khi client ngắt kết nối. */
  private void cleanup() {
    auctionService.removeObserver(this);
    AuctionServer.activeClients.remove(this);
    try {
      if (in  != null) in.close();
      if (out != null) out.close();
      if (socket != null && !socket.isClosed()) socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.printf("[ClientHandler] Client ngắt kết nối: %s%n",
            socket.getInetAddress());
  }
}