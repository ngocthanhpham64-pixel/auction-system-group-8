package vn.edu.vnu.uet.group8.server.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.UserAdmin;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.AdminLevel;
import vn.edu.vnu.uet.group8.common.enums.PaymentMethod;
import vn.edu.vnu.uet.group8.common.enums.TransactionType;
import vn.edu.vnu.uet.group8.common.enums.UserRole;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;
import vn.edu.vnu.uet.group8.common.exception.InsufficientBalanceException;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;
import vn.edu.vnu.uet.group8.server.util.PasswordUtil;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDAO - Unit Tests")
public class UserDAOTest {

  @Mock private DatabaseConnection dbConn;
  @Mock private Connection conn;
  @Mock private PreparedStatement ps;
  @Mock private ResultSet rs;
  @Mock private ResultSet keyRs;

  private UserDAO dao;
  private MockedStatic<DatabaseConnection> staticDbMock;
  private MockedStatic<PasswordUtil> staticPasswordMock;

  @BeforeEach
  void setUp() throws SQLException {
    staticDbMock = mockStatic(DatabaseConnection.class);
    staticDbMock.when(DatabaseConnection::getInstance).thenReturn(dbConn);
    lenient().when(dbConn.getConnection()).thenReturn(conn);

    staticPasswordMock = mockStatic(PasswordUtil.class);

    dao = new UserDAO();
  }

  @AfterEach
  void tearDown() {
    staticDbMock.close();
    staticPasswordMock.close();
  }

  // ═══════════════════════════════════════════════════
  // HELPERS
  // ═══════════════════════════════════════════════════

  private void setupUserMemberRs() throws SQLException {
    when(rs.getInt("user_id")).thenReturn(1);
    when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.now()));
    when(rs.getBoolean("is_deleted")).thenReturn(false);
    when(rs.getString("username")).thenReturn("member1");
    when(rs.getString("email")).thenReturn("member1@mail.com");
    when(rs.getString("password_hash")).thenReturn("hashedPass");
    when(rs.getString("status")).thenReturn("ACTIVE");
    when(rs.getTimestamp("last_login_at")).thenReturn(Timestamp.from(Instant.now()));
    when(rs.getString("admin_level")).thenReturn(null);
    when(rs.getString("roles")).thenReturn("BIDDER,SELLER");
    when(rs.getBigDecimal("balance")).thenReturn(new BigDecimal("100.50"));
    when(rs.getString("full_name")).thenReturn("Member One");
    when(rs.getString("phone")).thenReturn("0912345678");
    when(rs.getString("address")).thenReturn("Hanoi");
    when(rs.getBigDecimal("seller_rating")).thenReturn(new BigDecimal("4.8"));
    when(rs.getString("avatar_url")).thenReturn("avatar.png");
    when(rs.getInt("total_bids_placed")).thenReturn(5);
    when(rs.getInt("total_items_sold")).thenReturn(2);
  }

  private void setupUserAdminRs() throws SQLException {
    when(rs.getInt("user_id")).thenReturn(2);
    when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.now()));
    when(rs.getBoolean("is_deleted")).thenReturn(false);
    when(rs.getString("username")).thenReturn("admin1");
    when(rs.getString("email")).thenReturn("admin1@mail.com");
    when(rs.getString("password_hash")).thenReturn("hashedPassAdmin");
    when(rs.getString("status")).thenReturn("ACTIVE");
    when(rs.getTimestamp("last_login_at")).thenReturn(null);
    when(rs.getString("admin_level")).thenReturn("SUPER_ADMIN");
    when(rs.getString("full_name")).thenReturn("Admin One");
  }

  // ═══════════════════════════════════════════════════
  // CRUD & BASIC QUERIES
  // ═══════════════════════════════════════════════════

  @Nested
  @DisplayName("CRUD Operations")
  class CRUDTest {

    @Test
    @DisplayName("insert() - UserMember success")
    void insertUserMemberSuccess() throws SQLException {
      when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(ps);
      when(ps.getGeneratedKeys()).thenReturn(keyRs);
      when(keyRs.next()).thenReturn(true);
      when(keyRs.getInt(1)).thenReturn(99);

      UserMember member = UserMember.builder("testmember", "member@test.com", "pass")
          .fullname("Test Member")
          .phone("0987654321")
          .address("HCMC")
          .build();

      dao.insert(member);

      assertEquals(99, member.getId());
      verify(ps).setString(1, "testmember");
      verify(ps).setString(2, "member@test.com");
      verify(ps).setString(4, "Test Member");
      verify(ps).setString(5, "0987654321");
      verify(ps).setBigDecimal(8, BigDecimal.ZERO);
      verify(ps).executeUpdate();
    }

    @Test
    @DisplayName("insert() - UserAdmin success")
    void insertUserAdminSuccess() throws SQLException {
      when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(ps);
      when(ps.getGeneratedKeys()).thenReturn(keyRs);
      when(keyRs.next()).thenReturn(true);
      when(keyRs.getInt(1)).thenReturn(100);

      UserAdmin admin = new UserAdmin.Builder("testadmin", "admin@test.com", "pass", AdminLevel.MODERATOR).build();

      dao.insert(admin);

      assertEquals(100, admin.getId());
      verify(ps).setString(1, "testadmin");
      verify(ps).setString(2, "admin@test.com");
      verify(ps).setString(9, "MODERATOR");
      verify(ps).executeUpdate();
    }

    @Test
    @DisplayName("insert() - no generated keys throws SQLException")
    void insertNoKeysThrows() throws SQLException {
      when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(ps);
      when(ps.getGeneratedKeys()).thenReturn(keyRs);
      when(keyRs.next()).thenReturn(false);

      UserMember member = UserMember.builder("testmember", "member@test.com", "pass").build();

      assertThrows(SQLException.class, () -> dao.insert(member));
    }

    @Test
    @DisplayName("findById() - Member found")
    void findByIdMemberFound() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      setupUserMemberRs();

      Optional<User> resultOpt = dao.findById(1);

      assertTrue(resultOpt.isPresent());
      User result = resultOpt.get();
      assertTrue(result instanceof UserMember);
      UserMember m = (UserMember) result;
      assertEquals(1, m.getId());
      assertEquals("member1", m.getUsername());
      assertEquals("member1@mail.com", m.getEmail());
      assertEquals("Member One", m.getFullname());
      assertEquals("0912345678", m.getPhone());
      assertEquals("Hanoi", m.getAddress());
      assertEquals(0, m.getSellerRating().compareTo(new BigDecimal("4.8")));
      assertEquals("avatar.png", m.getAvatarUrl());
      assertEquals(5, m.getTotalBidsPlaced());
      assertEquals(2, m.getTotalItemsSold());
      assertTrue(m.getRoles().contains(UserRole.SELLER));
      assertTrue(m.getRoles().contains(UserRole.BIDDER));
    }

    @Test
    @DisplayName("findById() - Admin found")
    void findByIdAdminFound() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      setupUserAdminRs();

      Optional<User> resultOpt = dao.findById(2);

      assertTrue(resultOpt.isPresent());
      User result = resultOpt.get();
      assertTrue(result instanceof UserAdmin);
      UserAdmin a = (UserAdmin) result;
      assertEquals(2, a.getId());
      assertEquals("admin1", a.getUsername());
      assertEquals(AdminLevel.SUPER_ADMIN, a.getAdminLevel());
    }

    @Test
    @DisplayName("findById() - not found")
    void findByIdNotFound() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      Optional<User> result = dao.findById(999);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByEmail() - success")
    void findByEmailSuccess() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      setupUserMemberRs();

      Optional<User> result = dao.findByEmail("member1@mail.com");
      assertTrue(result.isPresent());
      assertEquals("member1@mail.com", result.get().getEmail());
    }

    @Test
    @DisplayName("findByUsername() - success")
    void findByUsernameSuccess() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      setupUserMemberRs();

      Optional<User> result = dao.findByUsername("member1");
      assertTrue(result.isPresent());
      assertEquals("member1", result.get().getUsername());
    }

    @Test
    @DisplayName("updateProfile() - values set correctly")
    void updateProfileValuesSet() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);

      UserMember member = UserMember.reconstructor()
          .id(1)
          .createdAt(Instant.now())
          .isDeleted(false)
          .username("member1")
          .email("member1@mail.com")
          .encryptedPassword("hash")
          .status(UserStatus.ACTIVE)
          .roles(EnumSet.of(UserRole.BIDDER))
          .balance(BigDecimal.ZERO)
          .fullname("New Name")
          .phone("0987654321")
          .address("New Address")
          .avatarUrl("new_avatar.png")
          .build();

      dao.updateProfile(member);

      verify(ps).setString(1, "New Name");
      verify(ps).setString(2, "0987654321");
      verify(ps).setString(3, "new_avatar.png");
      verify(ps).setString(4, "New Address");
      verify(ps).setInt(5, 1);
      verify(ps).executeUpdate();
    }

    @Test
    @DisplayName("updateProfile() - null / empty fields serialize to database NULL")
    void updateProfileNullFields() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);

      UserMember member = UserMember.reconstructor()
          .id(1)
          .createdAt(Instant.now())
          .isDeleted(false)
          .username("member1")
          .email("member1@mail.com")
          .encryptedPassword("hash")
          .status(UserStatus.ACTIVE)
          .roles(EnumSet.of(UserRole.BIDDER))
          .balance(BigDecimal.ZERO)
          .fullname("New Name")
          .phone("0987654321")
          .address("")
          .avatarUrl(null)
          .build();

      dao.updateProfile(member);

      verify(ps).setString(1, "New Name");
      verify(ps).setString(2, "0987654321");
      verify(ps).setNull(3, Types.VARCHAR);
      verify(ps).setNull(4, Types.VARCHAR);
      verify(ps).setInt(5, 1);
      verify(ps).executeUpdate();
    }
  }

  // ═══════════════════════════════════════════════════
  // SENSITIVE FIELDS & MODIFICATIONS
  // ═══════════════════════════════════════════════════

  @Nested
  @DisplayName("Modify Sensitive Fields")
  class ModifySensitiveFieldsTest {

    @Test
    @DisplayName("updateStatus() - success")
    void updateStatusSuccess() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeUpdate()).thenReturn(1);

      assertDoesNotThrow(() -> dao.updateStatus(1, UserStatus.BANNED));
      verify(ps).setString(1, "BANNED");
      verify(ps).setInt(2, 1);
    }

    @Test
    @DisplayName("updateStatus() - user not found throws Exception")
    void updateStatusNotFoundThrows() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeUpdate()).thenReturn(0);

      assertThrows(UserNotFoundException.class, () -> dao.updateStatus(999, UserStatus.ACTIVE));
    }

    @Test
    @DisplayName("softDelete() - success")
    void softDeleteSuccess() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeUpdate()).thenReturn(1);

      assertDoesNotThrow(() -> dao.softDelete(1));
      verify(ps).setInt(1, 1);
    }

    @Test
    @DisplayName("softDelete() - user not found throws Exception")
    void softDeleteNotFoundThrows() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeUpdate()).thenReturn(0);

      assertThrows(UserNotFoundException.class, () -> dao.softDelete(999));
    }

    @Test
    @DisplayName("updatePassword() - success")
    void updatePasswordSuccess() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeUpdate()).thenReturn(1);
      staticPasswordMock.when(() -> PasswordUtil.hash("newpass")).thenReturn("hashedNewPass");

      assertDoesNotThrow(() -> dao.updatePassword(1, "newpass"));
      verify(ps).setString(1, "hashedNewPass");
      verify(ps).setInt(2, 1);
    }

    @Test
    @DisplayName("updatePassword() - user not found throws Exception")
    void updatePasswordNotFoundThrows() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeUpdate()).thenReturn(0);
      staticPasswordMock.when(() -> PasswordUtil.hash("newpass")).thenReturn("hashedNewPass");

      assertThrows(UserNotFoundException.class, () -> dao.updatePassword(999, "newpass"));
    }

    @Test
    @DisplayName("updateLastLogin() - success")
    void updateLastLoginSuccess() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeUpdate()).thenReturn(1);

      assertDoesNotThrow(() -> dao.updateLastLogin(1));
      verify(ps).setTimestamp(eq(1), any(Timestamp.class));
      verify(ps).setInt(eq(2), eq(1));
    }

    @Test
    @DisplayName("addRole() - success")
    void addRoleSuccess() throws SQLException {
      // Setup finding user
      PreparedStatement psFind = mock(PreparedStatement.class);
      ResultSet rsFind = mock(ResultSet.class);
      when(conn.prepareStatement(argThat(s -> s != null && s.contains("SELECT * FROM users") && s.contains("user_id = ?")))).thenReturn(psFind);
      when(psFind.executeQuery()).thenReturn(rsFind);
      when(rsFind.next()).thenReturn(true);
      
      // Setup ResultSet fields for user matching member 1
      when(rsFind.getInt("user_id")).thenReturn(1);
      when(rsFind.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.now()));
      when(rsFind.getBoolean("is_deleted")).thenReturn(false);
      when(rsFind.getString("username")).thenReturn("member1");
      when(rsFind.getString("email")).thenReturn("member1@mail.com");
      when(rsFind.getString("password_hash")).thenReturn("hash");
      when(rsFind.getString("status")).thenReturn("ACTIVE");
      when(rsFind.getTimestamp("last_login_at")).thenReturn(null);
      when(rsFind.getString("admin_level")).thenReturn(null);
      when(rsFind.getString("roles")).thenReturn("BIDDER"); // currently only bidder
      when(rsFind.getBigDecimal("balance")).thenReturn(BigDecimal.ZERO);
      when(rsFind.getString("full_name")).thenReturn("name");
      when(rsFind.getString("phone")).thenReturn("0123456789");
      
      // Setup update roles statement
      PreparedStatement psUpdate = mock(PreparedStatement.class);
      when(conn.prepareStatement(argThat(s -> s != null && s.contains("UPDATE users") && s.contains("roles")))).thenReturn(psUpdate);

      dao.addRole(1, UserRole.SELLER);

      verify(psUpdate).setString(eq(1), argThat(s -> s != null && s.contains("BIDDER") && s.contains("SELLER")));
      verify(psUpdate).setInt(2, 1);
      verify(psUpdate).executeUpdate();
    }

    @Test
    @DisplayName("addRole() - user not found throws Exception")
    void addRoleNotFoundThrows() throws SQLException {
      PreparedStatement psFind = mock(PreparedStatement.class);
      ResultSet rsFind = mock(ResultSet.class);
      lenient().when(conn.prepareStatement(argThat(s -> s != null && s.contains("SELECT * FROM users")))).thenReturn(psFind);
      lenient().when(psFind.executeQuery()).thenReturn(rsFind);
      lenient().when(rsFind.next()).thenReturn(false);

      assertThrows(UserNotFoundException.class, () -> dao.addRole(999, UserRole.SELLER));
    }

    @Test
    @DisplayName("updateSellerRating() - success")
    void updateSellerRatingSuccess() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);

      dao.updateSellerRating(5);

      verify(ps).setInt(1, 5);
      verify(ps).setInt(2, 5);
      verify(ps).executeUpdate();
    }
  }

  // ═══════════════════════════════════════════════════
  // TRANSACTIONS & MONEY OPERATIONS
  // ═══════════════════════════════════════════════════

  @Nested
  @DisplayName("Wallet Transactions & Balance Operations")
  class BalanceOperationsTest {

    @Test
    @DisplayName("insertTransactionAndUpdateBalance() - transaction already exists")
    void transactionAlreadyExists() throws SQLException {
      // exist check returns true
      PreparedStatement psExists = mock(PreparedStatement.class);
      ResultSet rsExists = mock(ResultSet.class);
      when(conn.prepareStatement(argThat(s -> s != null && s.contains("SELECT EXISTS")))).thenReturn(psExists);
      when(psExists.executeQuery()).thenReturn(rsExists);
      when(rsExists.next()).thenReturn(true);
      when(rsExists.getBoolean(1)).thenReturn(true);

      // balance check setup
      PreparedStatement psSelect = mock(PreparedStatement.class);
      ResultSet rsSelect = mock(ResultSet.class);
      when(conn.prepareStatement(argThat(s -> s != null && s.contains("SELECT balance") && s.contains("FROM users")))).thenReturn(psSelect);
      when(psSelect.executeQuery()).thenReturn(rsSelect);
      when(rsSelect.next()).thenReturn(true);
      when(rsSelect.getBigDecimal("balance")).thenReturn(new BigDecimal("250.00"));

      BigDecimal balance = dao.insertTransactionAndUpdateBalance(
          "tx-exist-123", 1, new BigDecimal("100.00"), TransactionType.DEPOSIT, PaymentMethod.BANK_TRANSFER);

      assertEquals(0, balance.compareTo(new BigDecimal("250.00")));
      // verify no update statement is executed
      verify(conn, never()).setAutoCommit(anyBoolean());
    }

    @Test
    @DisplayName("insertTransactionAndUpdateBalance() - success execution")
    void insertTransactionAndBalanceSuccess() throws SQLException {
      // transaction exists check returns false
      PreparedStatement psExists = mock(PreparedStatement.class);
      ResultSet rsExists = mock(ResultSet.class);
      lenient().when(conn.prepareStatement(argThat(s -> s != null && s.contains("SELECT EXISTS")))).thenReturn(psExists);
      lenient().when(psExists.executeQuery()).thenReturn(rsExists);
      lenient().when(rsExists.next()).thenReturn(true);
      lenient().when(rsExists.getBoolean(1)).thenReturn(false);

      // update balance setup
      PreparedStatement psUpdate = mock(PreparedStatement.class);
      lenient().when(conn.prepareStatement(argThat(s -> s != null && s.contains("UPDATE users") && s.contains("SET balance")))).thenReturn(psUpdate);
      lenient().when(psUpdate.executeUpdate()).thenReturn(1);

      // insert transaction setup
      PreparedStatement psInsertTx = mock(PreparedStatement.class);
      lenient().when(conn.prepareStatement(argThat(s -> s != null && s.contains("INSERT INTO wallet_transaction")))).thenReturn(psInsertTx);

      // select balance setup
      PreparedStatement psSelect = mock(PreparedStatement.class);
      ResultSet rsSelect = mock(ResultSet.class);
      lenient().when(conn.prepareStatement(argThat(s -> s != null && s.contains("SELECT balance") && s.contains("FROM users")))).thenReturn(psSelect);
      lenient().when(psSelect.executeQuery()).thenReturn(rsSelect);
      when(rsSelect.next()).thenReturn(true);
      when(rsSelect.getBigDecimal("balance")).thenReturn(new BigDecimal("150.00"));

      when(conn.getAutoCommit()).thenReturn(true);

      BigDecimal result = dao.insertTransactionAndUpdateBalance(
          "tx-new-123", 1, new BigDecimal("50.00"), TransactionType.DEPOSIT, PaymentMethod.BANK_TRANSFER);

      assertEquals(0, result.compareTo(new BigDecimal("150.00")));
      verify(conn).setAutoCommit(false);
      verify(psUpdate).setBigDecimal(1, new BigDecimal("50.00"));
      verify(psUpdate).setInt(2, 1);
      verify(psUpdate).setBigDecimal(3, new BigDecimal("50.00"));
      verify(psInsertTx).setString(1, "tx-new-123");
      verify(conn).commit();
      verify(conn).setAutoCommit(true);
    }

    @Test
    @DisplayName("insertTransactionAndUpdateBalance() - affectedRows = 0 throws InsufficientBalanceException")
    void insertTransactionInsufficientBalance() throws SQLException {
      PreparedStatement psExists = mock(PreparedStatement.class);
      ResultSet rsExists = mock(ResultSet.class);
      lenient().when(conn.prepareStatement(argThat(s -> s != null && s.contains("SELECT EXISTS")))).thenReturn(psExists);
      lenient().when(psExists.executeQuery()).thenReturn(rsExists);
      lenient().when(rsExists.next()).thenReturn(true);
      lenient().when(rsExists.getBoolean(1)).thenReturn(false);

      PreparedStatement psUpdate = mock(PreparedStatement.class);
      lenient().when(conn.prepareStatement(argThat(s -> s != null && s.contains("UPDATE users") && s.contains("SET balance")))).thenReturn(psUpdate);
      lenient().when(psUpdate.executeUpdate()).thenReturn(0); // 0 rows affected (e.g. balance + amount < 0)

      when(conn.getAutoCommit()).thenReturn(true);

      assertThrows(InsufficientBalanceException.class, () -> dao.insertTransactionAndUpdateBalance(
          "tx-fail-123", 1, new BigDecimal("-500.00"), TransactionType.WITHDRAW, PaymentMethod.BANK_TRANSFER));
    }

    @Test
    @DisplayName("settleAuctionPayment() - connection version with refund")
    void settleAuctionPaymentWithRefund() throws SQLException {
      PreparedStatement psBuyer = mock(PreparedStatement.class);
      PreparedStatement psSeller = mock(PreparedStatement.class);
      PreparedStatement psInsertTx = mock(PreparedStatement.class);

      lenient().when(conn.prepareStatement(argThat(s -> s != null && s.contains("frozen_balance")))).thenReturn(psBuyer);
      lenient().when(conn.prepareStatement(argThat(s -> s != null && s.contains("balance = balance +") && !s.contains("frozen_balance")))).thenReturn(psSeller);
      lenient().when(conn.prepareStatement(argThat(s -> s != null && s.contains("INSERT INTO wallet_transaction")))).thenReturn(psInsertTx);

      lenient().when(psBuyer.executeUpdate()).thenReturn(1);
      lenient().when(psSeller.executeUpdate()).thenReturn(1);

      // heldAmount = 100, currentPrice = 80 (refund = 20)
      dao.settleAuctionPayment(conn, "win-tx", "refund-tx", "sell-tx", 1, 2, 10, new BigDecimal("100.00"), new BigDecimal("80.00"));

      // Verify buyer balance updates
      verify(psBuyer).setBigDecimal(1, new BigDecimal("100.00"));
      verify(psBuyer).setBigDecimal(2, new BigDecimal("20.00")); // refund amount
      verify(psBuyer).setInt(3, 1); // winnerId
      verify(psBuyer).setBigDecimal(4, new BigDecimal("100.00")); // heldAmount

      // Verify seller balance updates
      verify(psSeller).setBigDecimal(1, new BigDecimal("80.00")); // currentPrice
      verify(psSeller).setInt(2, 2); // sellerId

      // Verify transaction insertions (winner payment, winner refund, seller sale)
      verify(psInsertTx).setString(1, "win-tx");
      verify(psInsertTx).setString(1, "refund-tx");
      verify(psInsertTx).setString(1, "sell-tx");
    }

    @Test
    @DisplayName("settleAuctionPayment() - connection version buyer update fails throws InsufficientBalanceException")
    void settleAuctionPaymentBuyerFails() throws SQLException {
      PreparedStatement psBuyer = mock(PreparedStatement.class);
      lenient().when(conn.prepareStatement(argThat(s -> s != null && s.contains("frozen_balance")))).thenReturn(psBuyer);
      lenient().when(psBuyer.executeUpdate()).thenReturn(0); // buyer update fails

      assertThrows(InsufficientBalanceException.class, () -> dao.settleAuctionPayment(
          conn, "win-tx", "refund-tx", "sell-tx", 1, 2, 10, new BigDecimal("100.00"), new BigDecimal("80.00")));
    }

    @Test
    @DisplayName("settleAuctionPayment() - connection version seller update fails throws SQLException")
    void settleAuctionPaymentSellerFails() throws SQLException {
      PreparedStatement psBuyer = mock(PreparedStatement.class);
      PreparedStatement psSeller = mock(PreparedStatement.class);
      PreparedStatement psInsertTx = mock(PreparedStatement.class);

      lenient().when(conn.prepareStatement(argThat(s -> s != null && s.contains("frozen_balance")))).thenReturn(psBuyer);
      lenient().when(conn.prepareStatement(argThat(s -> s != null && s.contains("balance = balance +") && !s.contains("frozen_balance")))).thenReturn(psSeller);
      lenient().when(conn.prepareStatement(argThat(s -> s != null && s.contains("INSERT INTO wallet_transaction")))).thenReturn(psInsertTx);

      lenient().when(psBuyer.executeUpdate()).thenReturn(1);
      lenient().when(psSeller.executeUpdate()).thenReturn(0); // seller update fails

      assertThrows(SQLException.class, () -> dao.settleAuctionPayment(
          conn, "win-tx", "refund-tx", "sell-tx", 1, 2, 10, new BigDecimal("100.00"), new BigDecimal("80.00")));
    }

    @Test
    @DisplayName("settleAuctionPayment() - non-connection version success")
    void settleAuctionPaymentNonConnSuccess() throws SQLException {
      PreparedStatement psBuyer = mock(PreparedStatement.class);
      PreparedStatement psSeller = mock(PreparedStatement.class);
      PreparedStatement psInsertTx = mock(PreparedStatement.class);

      lenient().when(conn.prepareStatement(argThat(s -> s != null && s.contains("frozen_balance")))).thenReturn(psBuyer);
      lenient().when(conn.prepareStatement(argThat(s -> s != null && s.contains("balance = balance +") && !s.contains("frozen_balance")))).thenReturn(psSeller);
      lenient().when(conn.prepareStatement(argThat(s -> s != null && s.contains("INSERT INTO wallet_transaction")))).thenReturn(psInsertTx);

      lenient().when(psBuyer.executeUpdate()).thenReturn(1);
      lenient().when(psSeller.executeUpdate()).thenReturn(1);
      when(conn.getAutoCommit()).thenReturn(true);

      dao.settleAuctionPayment("win-tx", "sell-tx", 1, 2, 10, new BigDecimal("50.00"), TransactionType.BID_WIN);

      verify(conn).setAutoCommit(false);
      verify(psBuyer).setBigDecimal(1, new BigDecimal("50.00"));
      verify(psBuyer).setInt(2, 1);
      verify(psSeller).setBigDecimal(1, new BigDecimal("50.00"));
      verify(psSeller).setInt(2, 2);
      verify(conn).commit();
      verify(conn).setAutoCommit(true);
    }

    @Test
    @DisplayName("settleAuctionPayment() - non-connection version SQLException triggers rollback")
    void settleAuctionPaymentNonConnRollback() throws SQLException {
      PreparedStatement psBuyer = mock(PreparedStatement.class);
      when(conn.prepareStatement(argThat(s -> s != null && s.contains("frozen_balance = frozen_balance -")))).thenReturn(psBuyer);
      when(psBuyer.executeUpdate()).thenThrow(new SQLException("DB write error"));
      when(conn.getAutoCommit()).thenReturn(true);

      assertThrows(SQLException.class, () -> dao.settleAuctionPayment(
          "win-tx", "sell-tx", 1, 2, 10, new BigDecimal("50.00"), TransactionType.BID_WIN));

      verify(conn).rollback();
    }
  }

  // ═══════════════════════════════════════════════════
  // OTHER SPECIAL METHODS & AUTH
  // ═══════════════════════════════════════════════════

  @Nested
  @DisplayName("Authentication & Helpers")
  class AuthenticationAndHelpersTest {

    @Test
    @DisplayName("authenticate() - user not found")
    void authenticateUserNotFound() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      Optional<User> auth = dao.authenticate("unknown_user", "password");

      assertTrue(auth.isEmpty());
    }

    @Test
    @DisplayName("authenticate() - password mismatch")
    void authenticatePasswordMismatch() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      setupUserMemberRs();
      staticPasswordMock.when(() -> PasswordUtil.verify("wrong_pass", "hashedPass")).thenReturn(false);

      Optional<User> auth = dao.authenticate("member1", "wrong_pass");

      assertTrue(auth.isEmpty());
    }

    @Test
    @DisplayName("authenticate() - user is BANNED / inactive")
    void authenticateUserInactive() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      
      setupUserMemberRs();
      when(rs.getString("status")).thenReturn("BANNED"); // set status to BANNED

      staticPasswordMock.when(() -> PasswordUtil.verify("password", "hashedPass")).thenReturn(true);

      Optional<User> auth = dao.authenticate("member1", "password");

      assertTrue(auth.isEmpty());
    }

    @Test
    @DisplayName("authenticate() - active user success")
    void authenticateSuccess() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      setupUserMemberRs();
      staticPasswordMock.when(() -> PasswordUtil.verify("password", "hashedPass")).thenReturn(true);

      Optional<User> auth = dao.authenticate("member1", "password");

      assertTrue(auth.isPresent());
      assertEquals("member1", auth.get().getUsername());
    }

    @Test
    @DisplayName("existsByEmail() - true")
    void existsByEmailTrue() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      when(rs.getBoolean(1)).thenReturn(true);

      assertTrue(dao.existsByEmail("test@mail.com"));
    }

    @Test
    @DisplayName("existsByEmail() - false")
    void existsByEmailFalse() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      when(rs.getBoolean(1)).thenReturn(false);

      assertFalse(dao.existsByEmail("test@mail.com"));
    }

    @Test
    @DisplayName("existsByUsername() - true")
    void existsByUsernameTrue() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      when(rs.getBoolean(1)).thenReturn(true);

      assertTrue(dao.existsByUsername("username"));
    }

    @Test
    @DisplayName("existsByPhone() - true")
    void existsByPhoneTrue() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      when(rs.getBoolean(1)).thenReturn(true);

      assertTrue(dao.existsByPhone("0912345678"));
    }

    @Test
    @DisplayName("findAllSellers() - filters and maps active sellers correctly")
    void findAllSellersFilters() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);

      // Mock 3 rows: 1 member active, 1 admin (should be skipped), 1 member active
      when(rs.next()).thenReturn(true, true, true, false);

      // Row 1: Member Active
      when(rs.getInt("user_id")).thenReturn(10, 11, 12);
      when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.now()));
      when(rs.getBoolean("is_deleted")).thenReturn(false);
      when(rs.getString("username")).thenReturn("seller1", "admin_row", "seller2");
      when(rs.getString("email")).thenReturn("s1@mail.com", "admin@mail.com", "s2@mail.com");
      when(rs.getString("password_hash")).thenReturn("hash");
      when(rs.getString("status")).thenReturn("ACTIVE");
      when(rs.getTimestamp("last_login_at")).thenReturn(null);
      when(rs.getString("full_name")).thenReturn("Seller One", "Admin One", "Seller Two");

      // Set different user roles & types for each row iteration
      when(rs.getString("admin_level")).thenReturn(null, "SUPER_ADMIN", null);
      when(rs.getString("roles")).thenReturn("SELLER", "ADMIN", "SELLER");
      when(rs.getBigDecimal("balance")).thenReturn(BigDecimal.ZERO);
      when(rs.getString("phone")).thenReturn("09123");
      when(rs.getString("address")).thenReturn("HN");
      when(rs.getBigDecimal("seller_rating")).thenReturn(new BigDecimal("4.5"));
      when(rs.getString("avatar_url")).thenReturn("img.jpg");
      when(rs.getInt("total_bids_placed")).thenReturn(0);
      when(rs.getInt("total_items_sold")).thenReturn(10);

      List<UserMember> sellers = dao.findAllSellers();

      // Verify that the Admin row was skipped and both active Member sellers are included
      assertEquals(2, sellers.size());
      assertEquals("seller1", sellers.get(0).getUsername());
      assertEquals("seller2", sellers.get(1).getUsername());
    }
  }
}