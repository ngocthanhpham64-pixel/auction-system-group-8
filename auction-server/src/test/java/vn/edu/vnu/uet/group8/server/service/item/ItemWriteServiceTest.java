package vn.edu.vnu.uet.group8.server.service.item;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.entity.UserAdmin;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.AdminLevel;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;
import vn.edu.vnu.uet.group8.common.exception.AuctionException;
import vn.edu.vnu.uet.group8.common.exception.ItemNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.UnauthorizedException;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
import vn.edu.vnu.uet.group8.server.dao.ItemDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;

/**
 * Test cho {@link ItemWriteService}.
 *
 * <p>Chỉ test {@code deleteItem()} vì createItem + updateItem dùng FileUtil.saveBase64Images
 * (static, khó mock).
 */
@ExtendWith(MockitoExtension.class)
class ItemWriteServiceTest {

  @Mock private ItemDAO itemDAO;
  @Mock private UserDAO userDAO;
  @Mock private AuctionSessionDAO sessionDAO;
  @Mock private ItemSpecValidator specValidator;

  private ItemWriteService service;

  @BeforeEach
  void setUp() {
    service = new ItemWriteService(itemDAO, userDAO, sessionDAO, specValidator);
  }

  private Item taoItem(int id, int sellerId) {
    Item item = new Item.Builder(sellerId, "iPhone", ItemCategory.ELECTRONICS)
        .condition(ItemCondition.NEW)
        .description("desc")
        .build();
    item.assignId(id);
    return item;
  }

  private UserMember taoMember(int id) {
    UserMember m = new UserMember.Builder("user" + id, "u" + id + "@e.com", "hash").build();
    m.assignId(id);
    return m;
  }

  private UserAdmin taoAdmin(int id, AdminLevel level) {
    return UserAdmin.reconstructor()
        .id(id)
        .createdAt(java.time.Instant.now())
        .isDeleted(false)
        .username("admin" + id)
        .email("a" + id + "@e.com")
        .fullname("Admin " + id)
        .encryptedPassword("hash")
        .status(vn.edu.vnu.uet.group8.common.enums.UserStatus.ACTIVE)
        .roles(java.util.Set.of(vn.edu.vnu.uet.group8.common.enums.UserRole.ADMIN))
        .lastLogin(java.time.Instant.now())
        .adminLevel(level)
        .build();
  }

  private AuctionSession taoSession(int id, int itemId, SessionStatus status) {
    AuctionSession s = new AuctionSession.Builder(itemId,
        new BigDecimal("100"),
        Instant.now().minusSeconds(60),
        Instant.now().plusSeconds(3600))
        .build();
    s.assignId(id);
    if (status != null && status != SessionStatus.UPCOMING) {
      s.transitionStatus(SessionStatus.UPCOMING, SessionStatus.ACTIVE);
      if (status != SessionStatus.ACTIVE) {
        s.transitionStatus(SessionStatus.ACTIVE, status);
      }
    }
    return s;
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("deleteItem - validate")
  class DeleteValidate {

    @Test
    @DisplayName("Item không tồn tại → ItemNotFoundException")
    void itemKhongTonTai() throws SQLException {
      when(itemDAO.findById(99)).thenReturn(Optional.empty());

      assertThrows(ItemNotFoundException.class, () -> service.deleteItem(1, 99));
    }

    @Test
    @DisplayName("Requester không tồn tại → UnauthorizedException")
    void requesterKhongTonTai() throws SQLException {
      Item item = taoItem(10, 5);
      when(itemDAO.findById(10)).thenReturn(Optional.of(item));
      when(userDAO.findById(999)).thenReturn(Optional.empty());

      assertThrows(UnauthorizedException.class, () -> service.deleteItem(999, 10));
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("deleteItem - quyền hạn")
  class DeleteAuth {

    @Test
    @DisplayName("Owner xóa item của mình → OK")
    void ownerXoa() throws SQLException {
      Item item = taoItem(10, 5);
      UserMember owner = taoMember(5);
      when(itemDAO.findById(10)).thenReturn(Optional.of(item));
      when(userDAO.findById(5)).thenReturn(Optional.of(owner));
      when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());

      service.deleteItem(5, 10);

      verify(itemDAO).softDelete(10);
    }

    @Test
    @DisplayName("Admin xóa item của user khác → OK")
    void adminXoa() throws SQLException {
      Item item = taoItem(10, 5);
      UserAdmin admin = taoAdmin(99, AdminLevel.SUPER_ADMIN);
      when(itemDAO.findById(10)).thenReturn(Optional.of(item));
      when(userDAO.findById(99)).thenReturn(Optional.of(admin));
      when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());

      service.deleteItem(99, 10);

      verify(itemDAO).softDelete(10);
    }

    @Test
    @DisplayName("Member khác (không phải owner, không admin) → UnauthorizedException")
    void nguoiLaXoa() throws SQLException {
      Item item = taoItem(10, 5);
      UserMember other = taoMember(50);
      when(itemDAO.findById(10)).thenReturn(Optional.of(item));
      when(userDAO.findById(50)).thenReturn(Optional.of(other));

      assertThrows(UnauthorizedException.class, () -> service.deleteItem(50, 10));

      verify(itemDAO, never()).softDelete(anyInt());
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("deleteItem - session ACTIVE")
  class DeleteWithActiveSession {

    @Test
    @DisplayName("Item có session ACTIVE → AuctionException, không xóa")
    void coActiveSession() throws SQLException {
      Item item = taoItem(10, 5);
      UserMember owner = taoMember(5);
      AuctionSession activeSession = taoSession(20, 10, SessionStatus.ACTIVE);

      when(itemDAO.findById(10)).thenReturn(Optional.of(item));
      when(userDAO.findById(5)).thenReturn(Optional.of(owner));
      when(sessionDAO.findByItemId(10)).thenReturn(List.of(activeSession));

      assertThrows(AuctionException.class, () -> service.deleteItem(5, 10));

      verify(itemDAO, never()).softDelete(anyInt());
    }

    @Test
    @DisplayName("Item có session ENDED → cho phép xóa")
    void coEndedSession() throws SQLException {
      Item item = taoItem(10, 5);
      UserMember owner = taoMember(5);
      AuctionSession endedSession = taoSession(20, 10, SessionStatus.SOLD);

      when(itemDAO.findById(10)).thenReturn(Optional.of(item));
      when(userDAO.findById(5)).thenReturn(Optional.of(owner));
      when(sessionDAO.findByItemId(10)).thenReturn(List.of(endedSession));

      assertDoesNotThrow(() -> service.deleteItem(5, 10));

      verify(itemDAO).softDelete(10);
    }

    @Test
    @DisplayName("Item có session UPCOMING (chưa active) → cho phép xóa")
    void coUpcomingSession() throws SQLException {
      Item item = taoItem(10, 5);
      UserMember owner = taoMember(5);
      AuctionSession upcoming = taoSession(20, 10, SessionStatus.UPCOMING);

      when(itemDAO.findById(10)).thenReturn(Optional.of(item));
      when(userDAO.findById(5)).thenReturn(Optional.of(owner));
      when(sessionDAO.findByItemId(10)).thenReturn(List.of(upcoming));

      assertDoesNotThrow(() -> service.deleteItem(5, 10));

      verify(itemDAO).softDelete(10);
    }
  }
}