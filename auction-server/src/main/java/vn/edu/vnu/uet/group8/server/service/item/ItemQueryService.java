package vn.edu.vnu.uet.group8.server.service.item;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.exception.ItemNotFoundException;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
import vn.edu.vnu.uet.group8.server.dao.ItemDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;

/**
 * Xử lý các truy vấn đọc thông tin Item.
 *
 * <p>Không thay đổi DB — tất cả method đều read-only.
 * Kết hợp {@link Item} và {@link AuctionSession} để build
 * {@link AuctionItemDTO} đầy đủ trước khi trả về.
 *
 * <p>Dùng {@link AuctionSessionDAO} thay vì BidTransactionDAO
 * để lấy bidCount — session đã có sẵn trường này, tránh
 * COUNT(*) thừa mỗi lần xem chi tiết.
 */
public class ItemQueryService {

  private static final Logger logger =
      LoggerFactory.getLogger(ItemQueryService.class);

  private final ItemDAO           itemDAO;
  private final AuctionSessionDAO sessionDAO;
  private final UserDAO           userDAO;

  public ItemQueryService(ItemDAO itemDAO,
                          AuctionSessionDAO sessionDAO,
                          UserDAO userDAO) {
    this.itemDAO    = itemDAO;
    this.sessionDAO = sessionDAO;
    this.userDAO    = userDAO;
  }

  // ════════════════════════════════════════════════════
  // XEM CHI TIẾT MỘT ITEM
  // ════════════════════════════════════════════════════

  /**
   * Lấy thông tin chi tiết một item kèm trạng thái phiên đấu giá.
   *
   * <p>Tìm phiên ACTIVE hoặc UPCOMING của item — Client dùng để
   * hiển thị giá hiện tại, thời gian còn lại, số lượt bid.
   * Nếu không có phiên nào → item chưa được đưa lên sàn.
   *
   * @param itemId ID item cần xem
   * @return {@link AuctionItemDTO} đầy đủ thông tin
   * @throws ItemNotFoundException nếu item không tồn tại
   * @throws SQLException          nếu lỗi DB
   */
  public AuctionItemDTO getItemDetail(int itemId)
      throws SQLException {

    Item item = itemDAO.findById(itemId)
        .orElseThrow(() -> new ItemNotFoundException(itemId));

    // Tìm phiên đang chạy hoặc sắp chạy của item này
    Optional<AuctionSession> sessionActive =
        sessionDAO.findActiveSessionByItemId(itemId);
    
    Optional<AuctionSession> sessionComing =
        sessionDAO.findUpcomingByItemId(itemId);

    String sellerUsername = resolveSellerUsername(
        item.getSellerId());

    if (!sessionActive.isPresent() && !sessionComing.isPresent()) {
      // Item chưa có phiên — trả DTO không có giá và thời gian
      return AuctionItemDTO.from(null, item, sellerUsername, 0);
    }

    AuctionSession session = sessionActive.isPresent() ? sessionActive.get() : sessionComing.get();

    logger.debug(
        "getItemDetail: itemId={}, sessionId={}, bidCount={}",
        itemId, session.getId(), session.getBidCount());

    // bidCount lấy từ session — không cần query BidTransactionDAO
    return AuctionItemDTO.from(
        session, item, sellerUsername, session.getBidCount());
  }

  // ════════════════════════════════════════════════════
  // DANH SÁCH ITEM
  // ════════════════════════════════════════════════════

  /**
   * Lấy tất cả item đang ACTIVE để hiển thị trang chủ.
   * Sắp xếp theo endTime tăng dần — "sắp hết giờ" lên đầu.
   *
   * @return danh sách {@link AuctionItemDTO} đang active
   * @throws SQLException nếu lỗi DB
   */
  public List<AuctionItemDTO> getActiveItems() throws SQLException {
    List<AuctionSession> activeSessions =
        sessionDAO.findAllActive();

    return buildDtoList(activeSessions);
  }

  /**
   * Tìm kiếm item theo category và khoảng giá.
   * Filter xảy ra ở tầng DB — không kéo toàn bộ lên Java.
   *
   * @param category category muốn lọc, null = tất cả category
   * @param minPrice giá tối thiểu, null = không giới hạn dưới
   * @param maxPrice giá tối đa, null = không giới hạn trên
   * @return danh sách {@link AuctionItemDTO} khớp điều kiện
   * @throws SQLException nếu lỗi DB
   */
  public List<AuctionItemDTO> searchItems(
      ItemCategory category, BigDecimal minPrice,
      BigDecimal maxPrice) throws SQLException {

    List<AuctionSession> sessions =
        sessionDAO.findByPriceRange(category, minPrice, maxPrice);

    return buildDtoList(sessions);
  }

  /**
   * Lấy tất cả item của một seller — trang quản lý của người bán.
   * Bao gồm mọi trạng thái: DRAFT, LISTED, SOLD, ARCHIVED.
   *
   * @param sellerId ID người bán
   * @return danh sách {@link AuctionItemDTO} của seller
   * @throws SQLException nếu lỗi DB
   */
  public List<AuctionItemDTO> getMyItems(int sellerId)
      throws SQLException {

    List<Item> items = itemDAO.findBySeller(sellerId);

    List<AuctionItemDTO> result = new ArrayList<>();
    for (Item item : items) {
      Optional<AuctionSession> sessionActive =
        sessionDAO.findActiveSessionByItemId(item.getId());
    
      Optional<AuctionSession> sessionComing =
          sessionDAO.findUpcomingByItemId(item.getId());

      String sellerUsername =
          resolveSellerUsername(item.getSellerId());

      if (sessionActive.isPresent() || sessionComing.isPresent()) {
        AuctionSession session = sessionActive.isPresent() ? 
                  sessionActive.get() : sessionComing.get();
        result.add(AuctionItemDTO.from(
            session, item, sellerUsername,
            session.getBidCount()));
      } else {
        result.add(AuctionItemDTO.from(
            null, item, sellerUsername, 0));
      }
    }

    return result;
  }

  // ── Private helpers ─────────────────────────────────

  /**
   * Convert danh sách session → danh sách DTO.
   * Cache sellerUsername theo sellerId — tránh query DB
   * lặp lại khi nhiều item cùng seller (N+1 query problem).
   */
  private List<AuctionItemDTO> buildDtoList(
      List<AuctionSession> sessions) throws SQLException {

    // Cache: sellerId → username — tránh query lặp lại
    Map<Integer, String> sellerCache = new HashMap<>();
    List<AuctionItemDTO> result = new ArrayList<>();

    for (AuctionSession session : sessions) {
      Optional<Item> itemOpt =
          itemDAO.findById(session.getItemId());

      if (itemOpt.isEmpty()) {
        // Session tồn tại nhưng item bị xóa — bỏ qua, log cảnh báo
        logger.warn(
            "Session {} tham chiếu item {} không tồn tại",
            session.getId(), session.getItemId());
        continue;
      }

      Item item = itemOpt.get();
      int sellerId = item.getSellerId();

      // Dùng cache, chỉ query DB khi chưa có
      String sellerUsername = sellerCache.computeIfAbsent(
          sellerId, id -> {
            try {
              return resolveSellerUsername(id);
            } catch (SQLException e) {
              logger.warn(
                  "Không lấy được username sellerId={}: {}",
                  id, e.getMessage());
              return "seller#" + id;
            }
          });

      result.add(AuctionItemDTO.from(
          session, item, sellerUsername,
          session.getBidCount()));
    }

    return result;
  }

  /**
   * Lấy username của seller — trả "seller#id" nếu không tìm thấy.
   * Không ném exception — không để lỗi username làm hỏng toàn bộ list.
   */
  private String resolveSellerUsername(int sellerId)
      throws SQLException {
    return userDAO.findById(sellerId)
        .map(User::getUsername)
        .orElse("seller#" + sellerId);
  }
}