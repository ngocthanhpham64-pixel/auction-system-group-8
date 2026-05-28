package vn.edu.vnu.uet.group8.server.service.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.vnu.uet.group8.common.dto.model.AdminStatsDTO;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.model.UserAdminDTO;
import vn.edu.vnu.uet.group8.common.entity.UserAdmin;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.AdminLevel;
import vn.edu.vnu.uet.group8.common.exception.UnauthorizedException;
import vn.edu.vnu.uet.group8.server.dao.DatabaseConnection;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.service.auction.AuctionService;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceExtendedTest {

  @Mock private UserDAO userDAO;
  @Mock private AuctionService auctionService;
  @Mock private DatabaseConnection dbConn;
  @Mock private Connection conn;
  @Mock private PreparedStatement ps;
  @Mock private PreparedStatement ps2;
  @Mock private PreparedStatement ps3;
  @Mock private ResultSet rs;
  @Mock private ResultSet rs2;

  private AdminUserService service;
  private MockedStatic<DatabaseConnection> staticMock;

  @BeforeEach
  void setUp() throws SQLException {
    staticMock = mockStatic(DatabaseConnection.class);
    staticMock.when(DatabaseConnection::getInstance).thenReturn(dbConn);
    lenient().when(dbConn.getConnection()).thenReturn(conn);
    service = new AdminUserService(userDAO, auctionService);
  }

  @AfterEach
  void tearDown() {
    staticMock.close();
  }

  private UserAdmin taoSuperAdmin(int id) {
    UserAdmin a = new UserAdmin.Builder("super", "s@e.com", "pw", AdminLevel.SUPER_ADMIN).build();
    a.assignId(id);
    return a;
  }

  private UserAdmin taoModerator(int id) {
    UserAdmin a = new UserAdmin.Builder("mod", "m@e.com", "pw", AdminLevel.MODERATOR).build();
    a.assignId(id);
    return a;
  }

  // ─────────────────────────────────────────────────────────────
  // getDashboardStats
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("getDashboardStats()")
  class GetDashboardStatsTest {

    private void setupCounts(int activeAuctions, int totalUsers, int totalBids,
                             BigDecimal revenue) throws SQLException {
      // 3 COUNT queries + 1 SUM query → 4 PreparedStatement calls
      PreparedStatement ps1 = mock(PreparedStatement.class);
      PreparedStatement ps2 = mock(PreparedStatement.class);
      PreparedStatement ps3 = mock(PreparedStatement.class);
      PreparedStatement ps4 = mock(PreparedStatement.class);

      ResultSet rs1 = mock(ResultSet.class);
      ResultSet rs2 = mock(ResultSet.class);
      ResultSet rs3 = mock(ResultSet.class);
      ResultSet rs4 = mock(ResultSet.class);

      when(conn.prepareStatement(anyString())).thenReturn(ps1, ps2, ps3, ps4);

      when(ps1.executeQuery()).thenReturn(rs1);
      when(ps2.executeQuery()).thenReturn(rs2);
      when(ps3.executeQuery()).thenReturn(rs3);
      when(ps4.executeQuery()).thenReturn(rs4);

      when(rs1.next()).thenReturn(true);
      when(rs1.getInt(1)).thenReturn(activeAuctions);
      when(rs2.next()).thenReturn(true);
      when(rs2.getInt(1)).thenReturn(totalUsers);
      when(rs3.next()).thenReturn(true);
      when(rs3.getInt(1)).thenReturn(totalBids);
      when(rs4.next()).thenReturn(true);
      when(rs4.getBigDecimal(1)).thenReturn(revenue);
    }

    @Test
    @DisplayName("Success - admin hợp lệ → trả AdminStatsDTO đúng")
    void successAdminHopLe() throws SQLException {
      when(userDAO.findById(1)).thenReturn(Optional.of(taoSuperAdmin(1)));
      setupCounts(3, 100, 500, new BigDecimal("9000000"));

      AdminStatsDTO stats = service.getDashboardStats(1);

      assertEquals(3, stats.getActiveAuctions());
      assertEquals(100, stats.getTotalUsers());
      assertEquals(500, stats.getTotalBids());
      assertEquals(0, stats.getTotalRevenue().compareTo(new BigDecimal("9000000")));
    }

    @Test
    @DisplayName("Revenue = null từ DB → fallback ZERO")
    void revenueNullFallbackZero() throws SQLException {
      when(userDAO.findById(1)).thenReturn(Optional.of(taoSuperAdmin(1)));

      PreparedStatement p1 = mock(PreparedStatement.class);
      PreparedStatement p2 = mock(PreparedStatement.class);
      PreparedStatement p3 = mock(PreparedStatement.class);
      PreparedStatement p4 = mock(PreparedStatement.class);
      ResultSet r1 = mock(ResultSet.class);
      ResultSet r2 = mock(ResultSet.class);
      ResultSet r3 = mock(ResultSet.class);
      ResultSet r4 = mock(ResultSet.class);

      when(conn.prepareStatement(anyString())).thenReturn(p1, p2, p3, p4);
      when(p1.executeQuery()).thenReturn(r1);
      when(p2.executeQuery()).thenReturn(r2);
      when(p3.executeQuery()).thenReturn(r3);
      when(p4.executeQuery()).thenReturn(r4);
      when(r1.next()).thenReturn(true);
      when(r1.getInt(1)).thenReturn(0);
      when(r2.next()).thenReturn(true);
      when(r2.getInt(1)).thenReturn(0);
      when(r3.next()).thenReturn(true);
      when(r3.getInt(1)).thenReturn(0);
      when(r4.next()).thenReturn(true);
      when(r4.getBigDecimal(1)).thenReturn(null); // null SUM

      AdminStatsDTO stats = service.getDashboardStats(1);

      assertEquals(0, stats.getTotalRevenue().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Không phải admin → UnauthorizedException")
    void khongPhaiAdmin() throws SQLException {
      UserMember member = UserMember.builder("u", "e@e.com", "pw").build();
      member.assignId(5);
      when(userDAO.findById(5)).thenReturn(Optional.of(member));

      assertThrows(UnauthorizedException.class, () -> service.getDashboardStats(5));
      verify(conn, never()).prepareStatement(anyString());
    }

    @Test
    @DisplayName("Moderator cũng có quyền xem dashboard")
    void moderatorCoQuyen() throws SQLException {
      when(userDAO.findById(2)).thenReturn(Optional.of(taoModerator(2)));
      setupCounts(1, 50, 100, new BigDecimal("500000"));

      assertDoesNotThrow(() -> service.getDashboardStats(2));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // getUsers
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("getUsers()")
  class GetUsersTest {

    @Test
    @DisplayName("Success - trả về danh sách users từ DB")
    void successTraVeDanhSach() throws SQLException {
      when(userDAO.findById(1)).thenReturn(Optional.of(taoSuperAdmin(1)));
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true, true, false);
      when(rs.getInt("user_id")).thenReturn(10, 11);
      when(rs.getString("username")).thenReturn("user10", "user11");
      when(rs.getString("email")).thenReturn("u10@e.com", "u11@e.com");
      when(rs.getString("roles")).thenReturn("BIDDER", "BIDDER,SELLER");
      when(rs.getString("admin_level")).thenReturn(null, null);
      when(rs.getString("status")).thenReturn("ACTIVE", "ACTIVE");

      List<UserAdminDTO> users = service.getUsers(1);

      assertEquals(2, users.size());
      assertEquals("user10", users.get(0).getUsername());
    }

    @Test
    @DisplayName("resolveRole - admin_level = SUPER_ADMIN → ROLE_SUPER_ADMIN")
    void resolveRoleSuperAdmin() throws SQLException {
      when(userDAO.findById(1)).thenReturn(Optional.of(taoSuperAdmin(1)));
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true, false);
      when(rs.getInt("user_id")).thenReturn(99);
      when(rs.getString("username")).thenReturn("super_admin");
      when(rs.getString("email")).thenReturn("sa@e.com");
      when(rs.getString("roles")).thenReturn(null);
      when(rs.getString("admin_level")).thenReturn("SUPER_ADMIN");
      when(rs.getString("status")).thenReturn("ACTIVE");

      List<UserAdminDTO> users = service.getUsers(1);

      assertEquals(UserAdminDTO.ROLE_SUPER_ADMIN, users.get(0).getRole());
    }

    @Test
    @DisplayName("resolveRole - admin_level = MODERATOR → ROLE_ADMIN")
    void resolveRoleModerator() throws SQLException {
      when(userDAO.findById(1)).thenReturn(Optional.of(taoSuperAdmin(1)));
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true, false);
      when(rs.getInt("user_id")).thenReturn(55);
      when(rs.getString("username")).thenReturn("mod01");
      when(rs.getString("email")).thenReturn("mod@e.com");
      when(rs.getString("roles")).thenReturn(null);
      when(rs.getString("admin_level")).thenReturn("MODERATOR");
      when(rs.getString("status")).thenReturn("ACTIVE");

      List<UserAdminDTO> users = service.getUsers(1);

      assertEquals(UserAdminDTO.ROLE_ADMIN, users.get(0).getRole());
    }

    @Test
    @DisplayName("resolveRole - roles = BIDDER,SELLER → ROLE_SELLER")
    void resolveRoleSeller() throws SQLException {
      when(userDAO.findById(1)).thenReturn(Optional.of(taoSuperAdmin(1)));
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true, false);
      when(rs.getInt("user_id")).thenReturn(20);
      when(rs.getString("username")).thenReturn("seller01");
      when(rs.getString("email")).thenReturn("sel@e.com");
      when(rs.getString("roles")).thenReturn("BIDDER,SELLER");
      when(rs.getString("admin_level")).thenReturn(null);
      when(rs.getString("status")).thenReturn("ACTIVE");

      List<UserAdminDTO> users = service.getUsers(1);

      assertEquals(UserAdminDTO.ROLE_SELLER, users.get(0).getRole());
    }

    @Test
    @DisplayName("resolveRole - roles chỉ BIDDER → ROLE_MEMBER")
    void resolveRoleMember() throws SQLException {
      when(userDAO.findById(1)).thenReturn(Optional.of(taoSuperAdmin(1)));
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true, false);
      when(rs.getInt("user_id")).thenReturn(30);
      when(rs.getString("username")).thenReturn("member01");
      when(rs.getString("email")).thenReturn("mem@e.com");
      when(rs.getString("roles")).thenReturn("BIDDER");
      when(rs.getString("admin_level")).thenReturn(null);
      when(rs.getString("status")).thenReturn("ACTIVE");

      List<UserAdminDTO> users = service.getUsers(1);

      assertEquals(UserAdminDTO.ROLE_MEMBER, users.get(0).getRole());
    }

    @Test
    @DisplayName("resolveRole - roles = null, adminLevel = null → ROLE_MEMBER")
    void resolveRoleNullNull() throws SQLException {
      when(userDAO.findById(1)).thenReturn(Optional.of(taoSuperAdmin(1)));
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true, false);
      when(rs.getInt("user_id")).thenReturn(40);
      when(rs.getString("username")).thenReturn("u");
      when(rs.getString("email")).thenReturn("u@e.com");
      when(rs.getString("roles")).thenReturn(null);
      when(rs.getString("admin_level")).thenReturn(null);
      when(rs.getString("status")).thenReturn("ACTIVE");

      List<UserAdminDTO> users = service.getUsers(1);

      assertEquals(UserAdminDTO.ROLE_MEMBER, users.get(0).getRole());
    }

    @Test
    @DisplayName("Không phải admin → UnauthorizedException")
    void khongPhaiAdmin() throws SQLException {
      UserMember member = UserMember.builder("u", "e@e.com", "pw").build();
      member.assignId(5);
      when(userDAO.findById(5)).thenReturn(Optional.of(member));

      assertThrows(UnauthorizedException.class, () -> service.getUsers(5));
    }

    @Test
    @DisplayName("Danh sách rỗng → list rỗng")
    void danhSachRong() throws SQLException {
      when(userDAO.findById(1)).thenReturn(Optional.of(taoSuperAdmin(1)));
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      assertTrue(service.getUsers(1).isEmpty());
    }
  }

  // ─────────────────────────────────────────────────────────────
  // getAuctions
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("getAuctions()")
  class GetAuctionsTest {

    private void setupAuctionRs() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true, false);
      when(rs.getInt("item_id")).thenReturn(10);
      when(rs.getString("title")).thenReturn("iPhone 17 Pro");
      when(rs.getString("description")).thenReturn("Mô tả");
      when(rs.getString("category")).thenReturn("ELECTRONICS");
      when(rs.getString("condition_type")).thenReturn("NEW");
      when(rs.getString("session_status")).thenReturn("ACTIVE");
      when(rs.getBigDecimal("current_price")).thenReturn(new BigDecimal("1500000"));
      when(rs.getTimestamp("end_time")).thenReturn(
          Timestamp.from(Instant.now().plusSeconds(3600))
      );
      when(rs.getString("seller_username")).thenReturn("seller01");
      when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.now()));
    }

    @Test
    @DisplayName("Success - trả về danh sách AuctionItemDTO")
    void successTraVeDanhSach() throws SQLException {
      when(userDAO.findById(1)).thenReturn(Optional.of(taoSuperAdmin(1)));
      setupAuctionRs();

      List<AuctionItemDTO> auctions = service.getAuctions(1);

      assertEquals(1, auctions.size());
      assertEquals("iPhone 17 Pro", auctions.get(0).getTitle());
      assertEquals("seller01", auctions.get(0).getSellerUsername());
    }

    @Test
    @DisplayName("category không hợp lệ → fallback OTHER")
    void categoryFallback() throws SQLException {
      when(userDAO.findById(1)).thenReturn(Optional.of(taoSuperAdmin(1)));
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true, false);
      when(rs.getInt("item_id")).thenReturn(5);
      when(rs.getString("title")).thenReturn("T");
      when(rs.getString("description")).thenReturn("D");
      when(rs.getString("category")).thenReturn("INVALID_CATEGORY");
      when(rs.getString("condition_type")).thenReturn(null);
      when(rs.getString("session_status")).thenReturn("INVALID_STATUS");
      when(rs.getBigDecimal("current_price")).thenReturn(null);
      when(rs.getTimestamp("end_time")).thenReturn(null);
      when(rs.getString("seller_username")).thenReturn("s");
      when(rs.getTimestamp("created_at")).thenReturn(null);

      List<AuctionItemDTO> auctions = service.getAuctions(1);

      assertEquals(1, auctions.size());
      assertEquals(
          vn.edu.vnu.uet.group8.common.enums.ItemCategory.OTHER,
          auctions.get(0).getCategory()
      );
    }

    @Test
    @DisplayName("Không phải admin → UnauthorizedException")
    void khongPhaiAdmin() throws SQLException {
      UserMember member = UserMember.builder("u", "e@e.com", "pw").build();
      member.assignId(5);
      when(userDAO.findById(5)).thenReturn(Optional.of(member));

      assertThrows(UnauthorizedException.class, () -> service.getAuctions(5));
    }

    @Test
    @DisplayName("Danh sách rỗng → list rỗng")
    void danhSachRong() throws SQLException {
      when(userDAO.findById(1)).thenReturn(Optional.of(taoSuperAdmin(1)));
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      assertTrue(service.getAuctions(1).isEmpty());
    }
  }
}