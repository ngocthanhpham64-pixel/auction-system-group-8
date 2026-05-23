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
import vn.edu.vnu.uet.group8.common.dto.model.CommentDTO;
import vn.edu.vnu.uet.group8.common.dto.request.GetAuctionsRequest;
import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.exception.ItemNotFoundException;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO;
import vn.edu.vnu.uet.group8.server.dao.CommentDAO;
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
  private final BidTransactionDAO bidTransactionDAO;
  private final CommentDAO commentDAO;

  public ItemQueryService(ItemDAO itemDAO,
                          AuctionSessionDAO sessionDAO,
                          UserDAO userDAO,
                          BidTransactionDAO bidTransactionDAO,
                          CommentDAO commentDAO) {
    this.itemDAO    = itemDAO;
    this.sessionDAO = sessionDAO;
    this.userDAO    = userDAO;
    this.bidTransactionDAO = bidTransactionDAO;
    this.commentDAO = commentDAO;
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

    User seller = userDAO.findById(item.getSellerId()).orElse(null);
    String sellerUsername = seller != null ? seller.getUsername() : "seller#" + item.getSellerId();
    BigDecimal sellerRating = null;
    int totalItemsSold = 0;
    if (seller instanceof vn.edu.vnu.uet.group8.common.entity.UserMember) {
        vn.edu.vnu.uet.group8.common.entity.UserMember sm = (vn.edu.vnu.uet.group8.common.entity.UserMember) seller;
        sellerRating = sm.getSellerRating();
        totalItemsSold = sm.getTotalItemsSold();
    }

    if (!sessionActive.isPresent() && !sessionComing.isPresent()) {
      // Item chưa có phiên — trả DTO không có giá và thời gian
      return AuctionItemDTO.from(null, item, sellerUsername, 0, sellerRating, totalItemsSold);
    }

    AuctionSession session = sessionActive.isPresent() ? sessionActive.get() : sessionComing.get();

    logger.debug(
        "getItemDetail: itemId={}, sessionId={}, bidCount={}",
        itemId, session.getId(), session.getBidCount());

    // bidCount lấy từ session — không cần query BidTransactionDAO
    return AuctionItemDTO.from(
        session, item, sellerUsername, session.getBidCount(), sellerRating, totalItemsSold);
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
      BigDecimal maxPrice, GetAuctionsRequest.SortOption sortBy) throws SQLException {

    List<AuctionSession> sessions =
        sessionDAO.findByPriceRange(category, minPrice, maxPrice, sortBy);

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
      Optional<AuctionSession> sessionOpt = findRelevantSession(item.getId());
      User seller = userDAO.findById(item.getSellerId()).orElse(null);
      String sellerUsername = seller != null ? seller.getUsername() : "seller#" + item.getSellerId();
      BigDecimal sellerRating = null;
      int totalItemsSold = 0;
      if (seller instanceof UserMember) {
          UserMember sm = (UserMember) seller;
          sellerRating = sm.getSellerRating();
          totalItemsSold = sm.getTotalItemsSold();
      }

      if (sessionOpt.isPresent()) {
        AuctionSession session = sessionOpt.get();
        result.add(AuctionItemDTO.from(
            session, item, sellerUsername,
            session.getBidCount(), sellerRating, totalItemsSold));
      } else {
        result.add(AuctionItemDTO.from(
            null, item, sellerUsername, 0, sellerRating, totalItemsSold));
      }
    }

    return result;
  }

  public List<AuctionItemDTO> getMyItemsByStatus(int sellerId, vn.edu.vnu.uet.group8.common.enums.ItemStatus status)
      throws SQLException {

    List<Item> items = itemDAO.findBySellerAndStatus(sellerId, status);

    List<AuctionItemDTO> result = new ArrayList<>();
    for (Item item : items) {
      Optional<AuctionSession> sessionOpt = findRelevantSession(item.getId());
      User seller = userDAO.findById(item.getSellerId()).orElse(null);
      String sellerUsername = seller != null ? seller.getUsername() : "seller#" + item.getSellerId();
      BigDecimal sellerRating = null;
      int totalItemsSold = 0;
      if (seller instanceof UserMember) {
          UserMember sm = (UserMember) seller;
          sellerRating = sm.getSellerRating();
          totalItemsSold = sm.getTotalItemsSold();
      }

      if (sessionOpt.isPresent()) {
        AuctionSession session = sessionOpt.get();
        result.add(AuctionItemDTO.from(
            session, item, sellerUsername,
            session.getBidCount(), sellerRating, totalItemsSold));
      } else {
        result.add(AuctionItemDTO.from(
            null, item, sellerUsername, 0, sellerRating, totalItemsSold));
      }
    }

    return result;
  }

  private Optional<AuctionSession> findRelevantSession(int itemId) throws SQLException {
    Optional<AuctionSession> sessionActive = sessionDAO.findActiveSessionByItemId(itemId);
    if (sessionActive.isPresent()) return sessionActive;

    Optional<AuctionSession> sessionComing = sessionDAO.findUpcomingByItemId(itemId);
    if (sessionComing.isPresent()) return sessionComing;

    List<AuctionSession> sessions = sessionDAO.findByItemId(itemId);
    if (!sessions.isEmpty()) {
      return Optional.of(sessions.get(0));
    }

    return Optional.empty();
  }

  // ════════════════════════════════════════════════════
  // Comment
  // ════════════════════════════════════════════════════
  public List<CommentDTO> getCommentsForAnItemId(int item_id) {
    return commentDAO.getCommentsForAnItemId(item_id);
  }

  // ════════════════════════════════════════════════════
  // Logic Kiểm tra
  // ════════════════════════════════════════════════════
  public List<AuctionItemDTO> getAuctions(GetAuctionsRequest filter) throws SQLException {
    if (filter == null) {
      return searchItems(null, null, null, GetAuctionsRequest.SortOption.NEWEST);
    }
    return searchItems(filter.getCategory(), filter.getMinPrice(), filter.getMaxPrice(), filter.getSortBy());
  }

  public List<AuctionItemDTO> getWonItems(int winnerId) throws SQLException {
    List<AuctionSession> wonSessions = sessionDAO.findWonSessionsByUserId(winnerId);
    return buildDtoList(wonSessions);
  }

  // ════════════════════════════════════════════════════
  // PRIVATE HELPERS
  // ════════════════════════════════════════════════════

  /**
   * Convert danh sách session → danh sách DTO.
   * Cache sellerUsername theo sellerId — tránh query DB
   * lặp lại khi nhiều item cùng seller (N+1 query problem).
   */
  private List<AuctionItemDTO> buildDtoList(
      List<AuctionSession> sessions) throws SQLException {

    // Cache: sellerId → User — tránh query lặp lại
    Map<Integer, User> userCache = new HashMap<>();
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
      User seller = userCache.get(sellerId);
      if (seller == null && !userCache.containsKey(sellerId)) {
        seller = userDAO.findById(sellerId).orElse(null);
        userCache.put(sellerId, seller);
      }

      String sellerUsername = seller != null ? seller.getUsername() : "seller#" + sellerId;
      BigDecimal sellerRating = null;
      int totalItemsSold = 0;
      if (seller instanceof UserMember) {
          UserMember sm = (UserMember) seller;
          sellerRating = sm.getSellerRating();
          totalItemsSold = sm.getTotalItemsSold();
      }

      result.add(AuctionItemDTO.from(
          session, item, sellerUsername,
          session.getBidCount(), sellerRating, totalItemsSold));
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