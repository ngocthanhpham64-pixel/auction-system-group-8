package vn.edu.vnu.uet.group8.server.service.item;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;
import vn.edu.vnu.uet.group8.common.enums.UserRole;
import vn.edu.vnu.uet.group8.common.exception.AuctionException;
import vn.edu.vnu.uet.group8.common.exception.ItemNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.UnauthorizedException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
import vn.edu.vnu.uet.group8.server.dao.ItemDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.util.FileUtil;

/**
 * Xử lý các thao tác ghi liên quan đến Item.
 *
 * <p>
 * Ba hàm chính: tạo mới, cập nhật, xóa mềm.
 * Thời gian đấu giá ({@code startTime}, {@code endTime}) và giá
 * khởi điểm ({@code startingPrice}) KHÔNG thuộc về class này —
 * chúng là thuộc tính của {@code AuctionSession}, được quản lý
 * bởi {@code AuctionSessionService}.
 */
public class ItemWriteService {

  private static final Logger logger = LoggerFactory.getLogger(ItemWriteService.class);

  // Giới hạn số item LISTED đồng thời mỗi seller
  private static final int MAX_LISTED_ITEMS_PER_SELLER = 10;

  private final ItemDAO itemDAO;
  private final UserDAO userDAO;
  private final AuctionSessionDAO sessionDAO;
  private final ItemSpecValidator specValidator;

  public ItemWriteService(ItemDAO itemDAO,
      UserDAO userDAO,
      AuctionSessionDAO sessionDAO,
      ItemSpecValidator specValidator) {
    this.itemDAO = itemDAO;
    this.userDAO = userDAO;
    this.sessionDAO = sessionDAO;
    this.specValidator = specValidator;
  }

  // ════════════════════════════════════════════════════
  // TẠO ITEM MỚI
  // ════════════════════════════════════════════════════

  /**
   * Tạo item mới với trạng thái DRAFT.
   *
   * <p>
   * Chỉ lưu thông tin vật lý của sản phẩm. Thời gian và
   * giá khởi điểm được thiết lập sau bởi
   * {@code AuctionSessionService.createSession()}.
   *
   * @param sellerId    ID người tạo item
   * @param title       tiêu đề sản phẩm
   * @param description mô tả chi tiết
   * @param category    danh mục sản phẩm
   * @param condition   tình trạng sản phẩm
   * @param imageUrls   mảng đường dẫn hình ảnh sản phẩm
   * @return {@link Item} đã được lưu vào DB và có ID hợp lệ
   * @throws ValidationException   nếu input hoặc specs không hợp lệ
   * @throws UnauthorizedException nếu tài khoản không có quyền bán
   * @throws SQLException          nếu lỗi DB
   */
  public Item createItem(
      int sellerId,
      String title,
      String description,
      ItemCategory category,
      ItemCondition condition,
      // Map<String, String> specs,
      List<String> imageUrls,
      BigDecimal startPrice,
      Integer durationHours,
      Long startTime) throws SQLException {

    // -- Kiểm tra seller tồn tại và đang ACTIVE
    User seller = userDAO.findById(sellerId)
        .orElseThrow(() -> new ValidationException(
            "Không tìm thấy người dùng id=" + sellerId));

    if (!seller.isActive()) {
      throw new UnauthorizedException(
          "đăng bán sản phẩm — tài khoản không hoạt động");
    }

    // -- Kiểm tra giới hạn số item LISTED đồng thời
    int listedCount = itemDAO.countBySellerAndStatus(sellerId, ItemStatus.LISTED);
    if (listedCount >= MAX_LISTED_ITEMS_PER_SELLER) {
      throw new ValidationException(
          "Bạn chỉ có thể đăng bán tối đa "
              + MAX_LISTED_ITEMS_PER_SELLER
              + " sản phẩm cùng lúc");
    }

    // -- Validate title
    if (title == null || title.isBlank()) {
      throw new ValidationException("Tiêu đề không được trống");
    }

    // -- Validate specs theo category
    specValidator.validate(category);

    // -- Xác định trạng thái ban đầu: Nếu có thông tin đấu giá thì đưa lên sàn luôn
    boolean isPublishing = (startPrice != null && durationHours != null && durationHours > 0);
    ItemStatus initStatus = isPublishing ? ItemStatus.LISTED : ItemStatus.DRAFT;

    // -- Chuyển đổi Base64 thành file cứng và lấy link
    List<String> savedImageUrls = FileUtil.saveBase64Images(imageUrls);

    // -- Tạo entity Item
    Item item = new Item.Builder(sellerId, title, category)
        .description(description)
        .condition(condition)
        // .specs(specs)
        .imageUrls(savedImageUrls)
        .status(initStatus)
        .build();

    itemDAO.insert(item);
    // item.getId() > 0 sau khi insert

    // -- Tạo phiên đấu giá nếu đang Publish
    if (isPublishing) {
      Instant now = Instant.now();
      Instant startInstant = (startTime != null) ? Instant.ofEpochMilli(startTime) : now;
      if (startInstant.isBefore(now)) {
        startInstant = now;
      }
      Instant endTime = startInstant.plus(durationHours, ChronoUnit.MINUTES);

      AuctionSession session = new AuctionSession.Builder(item.getId(), startPrice, startInstant, endTime).build();
      
      // Mở phiên ngay nếu thời gian bắt đầu đã đến, nếu chưa thì để UPCOMING
      if (!startInstant.isAfter(now)) {
          session.transitionStatus(SessionStatus.UPCOMING, SessionStatus.ACTIVE);
      }

      sessionDAO.insert(session);
      logger.info("Đã tạo và mở phiên đấu giá cho item {}", item.getId());
    }

    // -- Tự động thêm role SELLER nếu chưa có
    if (!seller.hasRole(UserRole.SELLER)) {
      userDAO.addRole(sellerId, UserRole.SELLER);
      logger.info("Thêm role SELLER cho userId={}", sellerId);
    }

    logger.info(
        "Tạo item thành công: itemId={}, sellerId={}, category={}",
        item.getId(), sellerId, category);

    return item;
  }

  // ════════════════════════════════════════════════════
  // CẬP NHẬT ITEM
  // ════════════════════════════════════════════════════

  /**
   * Cập nhật thông tin vật lý của item.
   *
   * <p>
   * Chỉ cho phép khi item đang ở trạng thái DRAFT —
   * không cho sửa khi đã LISTED (đang có phiên đấu giá).
   * Nguyên tắc: người mua đã xem thông tin rồi thì không
   * được phép thay đổi để tránh gian lận.
   *
   * @param requesterId ID người thực hiện — phải là seller của item
   * @param itemId      ID item cần cập nhật
   * @param title       tiêu đề mới
   * @param description mô tả mới
   * @param condition   tình trạng mới
   * @param specs       specs mới — validate lại hoàn toàn
   * @throws UnauthorizedException nếu không phải chủ sở hữu
   * @throws AuctionException      nếu item đang trong phiên đấu giá
   * @throws ItemNotFoundException nếu item không tồn tại
   * @throws ValidationException   nếu specs mới không hợp lệ
   * @throws SQLException          nếu lỗi DB
   */
  public void updateItem(
      int requesterId,
      int itemId,
      String title,
      String description,
      vn.edu.vnu.uet.group8.common.enums.ItemCondition condition,
      // Map<String, String> specs,
      List<String> imageUrls) throws SQLException {

    Item item = itemDAO.findById(itemId)
        .orElseThrow(() -> new ItemNotFoundException(itemId));

    // -- Chỉ seller của item mới được sửa
    if (item.getSellerId() != requesterId) {
      throw new UnauthorizedException(
          "chỉnh sửa item của người khác");
    }

    // -- Không cho sửa khi đang có phiên ACTIVE hoặc UPCOMING
    boolean hasRunningSession = sessionDAO
        .findByItemId(itemId).stream()
        .anyMatch(s -> s.getStatus() == SessionStatus.ACTIVE
            || s.getStatus() == SessionStatus.UPCOMING);

    if (hasRunningSession) {
      throw new AuctionException(
          "Không thể chỉnh sửa sản phẩm đang trong phiên đấu giá");
    }

    // -- Validate specs mới theo category của item (category không đổi)
    specValidator.validate(item.getCategory());

    // -- Áp dụng thay đổi
    if (title != null && !title.isBlank()) {
      item.setTitle(title);
    }
    if (description != null) {
      item.setDescription(description);
    }
    if (condition != null) {
      item.setCondition(condition);
    }
    // if (specs != null) {
    // item.setSpecs(specs);
    // }
    if (imageUrls != null) {
      item.setImageUrls(FileUtil.saveBase64Images(imageUrls));
    }

    itemDAO.update(item);

    logger.info(
        "Cập nhật item thành công: itemId={}, requesterId={}",
        itemId, requesterId);
  }

  // ════════════════════════════════════════════════════
  // XÓA ITEM (SOFT DELETE)
  // ════════════════════════════════════════════════════

  /**
   * Xóa mềm item — đánh dấu {@code is_deleted = true}.
   *
   * <p>
   * Logic theo đúng hướng bạn đề xuất:
   * <ol>
   * <li>Kiểm tra chủ sở hữu
   * <li>Query session — nếu có bất kỳ session ACTIVE → từ chối
   * <li>Soft delete
   * </ol>
   *
   * <p>
   * Không xóa vật lý vì lịch sử đấu giá và bid_transaction
   * vẫn tham chiếu đến itemId — xóa vật lý gây vỡ FK.
   *
   * @param requesterId ID người thực hiện — phải là seller hoặc admin
   * @param itemId      ID item cần xóa
   * @throws UnauthorizedException nếu không có quyền
   * @throws AuctionException      nếu item đang trong phiên ACTIVE
   * @throws ItemNotFoundException nếu item không tồn tại
   * @throws SQLException          nếu lỗi DB
   */
  public void deleteItem(int requesterId, int itemId)
      throws SQLException {

    Item item = itemDAO.findById(itemId)
        .orElseThrow(() -> new ItemNotFoundException(itemId));

    // -- Kiểm tra quyền: seller của item hoặc admin
    User requester = userDAO.findById(requesterId)
        .orElseThrow(() -> new UnauthorizedException(
            "xóa item — người dùng không tồn tại"));

    boolean isOwner = item.getSellerId() == requesterId;
    boolean isAdmin = requester.isAdmin();

    if (!isOwner && !isAdmin) {
      throw new UnauthorizedException(
          "xóa item của người khác");
    }

    // -- Từ chối nếu có session ACTIVE
    // Theo đúng hướng giải quyết của bạn:
    // gọi AuctionSessionDAO.findByItemId() → kiểm tra ACTIVE
    List<vn.edu.vnu.uet.group8.common.entity.AuctionSession> sessions = sessionDAO.findByItemId(itemId);

    boolean hasActiveSession = sessions.stream()
        .anyMatch(s -> s.getStatus() == SessionStatus.ACTIVE);

    if (hasActiveSession) {
      throw new AuctionException(
          "Không thể xóa sản phẩm đang trong phiên đấu giá. "
              + "Hủy phiên trước khi xóa sản phẩm");
    }

    // -- Soft delete
    itemDAO.softDelete(itemId);

    logger.info(
        "Soft delete item: itemId={}, requesterId={}, isAdmin={}",
        itemId, requesterId, isAdmin);
  }
}