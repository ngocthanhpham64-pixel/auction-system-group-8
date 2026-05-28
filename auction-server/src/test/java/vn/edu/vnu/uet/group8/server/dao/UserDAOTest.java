package vn.edu.vnu.uet.group8.server.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.UserAdmin;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.*;
import vn.edu.vnu.uet.group8.common.exception.*;
import vn.edu.vnu.uet.group8.server.util.PasswordUtil;

/**
 * UserDAOTest — Mục tiêu: 100% branch + instruction coverage.
 *
 * Phân tích branch MISSING theo report:
 *  0%  → insertTransactionAndUpdateBalance, settleAuctionPayment,
 *          updatePassword, authenticate
 *  50% → toInstant (ts != null branch chưa có)
 *  50% → lambda$parseRoles$1 (filter s.isEmpty()=true branch chưa có)
 *  62% → findById / findByEmail / findByUsername (try-with-resources + SQLException branch)
 *  75% → updateProfile (avatarUrl blank / address blank branch)
 *  75% → parseRoles (rolesStr.isBlank() branch)
 *  75% → existsByUsername (rs.next()=false → false branch trong &&)
 *  83% → insert (try-with-resources edge branch)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserDAO — Full Branch & Instruction Coverage")
class UserDAOTest {

    // ── Mocks ────────────────────────────────────────────────────────────────
    @Mock private DatabaseConnection dbConn;
    @Mock private Connection conn;

    // Nhiều PS để dùng cho các câu SQL khác nhau trong cùng 1 test
    @Mock private PreparedStatement ps;
    @Mock private PreparedStatement ps2;
    @Mock private PreparedStatement ps3;
    @Mock private PreparedStatement ps4;

    // Nhiều RS để dùng cho các câu query khác nhau
    @Mock private ResultSet rs;
    @Mock private ResultSet rs2;
    @Mock private ResultSet keyRs;

    private UserDAO dao;
    private MockedStatic<DatabaseConnection> staticDbMock;

    // ════════════════════════════════════════════════════════════════════════
    // SETUP / TEARDOWN
    // ════════════════════════════════════════════════════════════════════════

    @BeforeEach
    void setUp() throws SQLException {
        staticDbMock = mockStatic(DatabaseConnection.class);
        staticDbMock.when(DatabaseConnection::getInstance).thenReturn(dbConn);
        when(dbConn.getConnection()).thenReturn(conn);
        dao = new UserDAO();
    }

    @AfterEach
    void tearDown() {
        staticDbMock.close();
    }

    // ════════════════════════════════════════════════════════════════════════
    // HELPERS — thiết lập ResultSet cho mapRow()
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Setup đủ các field cho UserMember.
     * lastLogin = null → phủ toInstant(null) branch.
     */
    private void setupMemberRs(ResultSet target, int id, String username, String status)
            throws SQLException {
        when(target.getInt("user_id")).thenReturn(id);
        when(target.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.now()));
        when(target.getBoolean("is_deleted")).thenReturn(false);
        when(target.getString("username")).thenReturn(username);
        when(target.getString("email")).thenReturn(username + "@test.com");
        when(target.getString("password_hash")).thenReturn("$2a$12$hashed");
        when(target.getString("status")).thenReturn(status);
        when(target.getTimestamp("last_login_at")).thenReturn(null);   // toInstant(null)
        when(target.getString("admin_level")).thenReturn(null);         // → member branch
        when(target.getString("full_name")).thenReturn("Nguyen Van A");
        when(target.getString("roles")).thenReturn("BIDDER");
        when(target.getBigDecimal("balance")).thenReturn(new BigDecimal("100000"));
        when(target.getString("phone")).thenReturn("0900000000");
        when(target.getString("address")).thenReturn("Hanoi");
        when(target.getBigDecimal("seller_rating")).thenReturn(null);
        when(target.getString("avatar_url")).thenReturn(null);
        when(target.getInt("total_bids_placed")).thenReturn(0);
        when(target.getInt("total_items_sold")).thenReturn(0);
    }

    /**
     * Setup UserMember với lastLogin CÓ GIÁ TRỊ → phủ toInstant(ts != null) branch.
     */
    private void setupMemberRsWithLastLogin(ResultSet target, int id, String username)
            throws SQLException {
        setupMemberRs(target, id, username, "ACTIVE");
        // Override last_login_at với giá trị thực → toInstant(ts) branch
        when(target.getTimestamp("last_login_at"))
                .thenReturn(Timestamp.from(Instant.now().minusSeconds(3600)));
    }

    private void setupAdminRs(ResultSet target, int id, String username, String adminLevel)
            throws SQLException {
        when(target.getInt("user_id")).thenReturn(id);
        when(target.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.now()));
        when(target.getBoolean("is_deleted")).thenReturn(false);
        when(target.getString("username")).thenReturn(username);
        when(target.getString("email")).thenReturn(username + "@admin.com");
        when(target.getString("password_hash")).thenReturn("$2a$12$adminhashed");
        when(target.getString("status")).thenReturn("ACTIVE");
        when(target.getTimestamp("last_login_at")).thenReturn(null);
        when(target.getString("admin_level")).thenReturn(adminLevel); // → admin branch
        when(target.getString("full_name")).thenReturn("Admin User");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 1. insert()
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("insert()")
    class InsertTests {

        @Test
        @DisplayName("insert UserMember thành công — gán id từ generated key")
        void insertMemberSuccess() throws SQLException {
            when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenReturn(ps);
            when(ps.getGeneratedKeys()).thenReturn(keyRs);
            when(keyRs.next()).thenReturn(true);
            when(keyRs.getInt(1)).thenReturn(77);

            UserMember member = UserMember.builder("user01", "u@e.com", "hashedpw").build();
            dao.insert(member);

            assertEquals(77, member.getId());
            verify(ps).setString(1, "user01");
            verify(ps).setString(2, "u@e.com");
            verify(ps).setNull(9, Types.VARCHAR); // admin_level null cho member
        }

        @Test
        @DisplayName("insert UserAdmin thành công — gán admin_level")
        void insertAdminSuccess() throws SQLException {
            when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenReturn(ps);
            when(ps.getGeneratedKeys()).thenReturn(keyRs);
            when(keyRs.next()).thenReturn(true);
            when(keyRs.getInt(1)).thenReturn(99);

            UserAdmin admin = new UserAdmin.Builder(
                    "admin01", "a@e.com", "hashedpw",
                    AdminLevel.SUPER_ADMIN).build();
            dao.insert(admin);

            assertEquals(99, admin.getId());
            verify(ps).setString(9, "SUPER_ADMIN");
            verify(ps).setNull(4, Types.VARCHAR); // full_name null cho admin
            verify(ps).setNull(15, Types.INTEGER); // total_bids_placed null cho admin
        }

        @Test
        @DisplayName("insert — getGeneratedKeys().next() = false → SQLException")
        void insertNoGeneratedKey() throws SQLException {
            when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenReturn(ps);
            when(ps.getGeneratedKeys()).thenReturn(keyRs);
            when(keyRs.next()).thenReturn(false);

            assertThrows(SQLException.class,
                    () -> dao.insert(UserMember.builder("u", "e@e.com", "pw").build()));
        }

        @Test
        @DisplayName("insert — prepareStatement ném SQLException → ném lên caller")
        void insertSQLException() throws SQLException {
            when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenThrow(new SQLException("DB lỗi"));

            assertThrows(SQLException.class,
                    () -> dao.insert(UserMember.builder("u", "e@e.com", "pw").build()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. findById()  —  62% branch → cần thêm: toInstant(ts!=null), SQLException
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("Tìm thấy UserMember (lastLogin=null) → branch toInstant(null)")
        void findMember_lastLoginNull() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupMemberRs(rs, 5, "member01", "ACTIVE");

            Optional<User> result = dao.findById(5);

            assertTrue(result.isPresent());
            assertInstanceOf(UserMember.class, result.get());
            assertEquals("member01", result.get().getUsername());
            verify(ps).setInt(1, 5);
        }

        @Test
        @DisplayName("Tìm thấy UserMember (lastLogin có giá trị) → branch toInstant(ts!=null)")
        void findMember_lastLoginNotNull() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupMemberRsWithLastLogin(rs, 6, "member02");

            Optional<User> result = dao.findById(6);

            assertTrue(result.isPresent());
            UserMember m = (UserMember) result.get();
            assertEquals("member02", m.getUsername());
            // lastLogin phải không null vì toInstant(ts != null) trả ts.toInstant()
            assertNotNull(((vn.edu.vnu.uet.group8.common.entity.User) m).getLastLogin());
        }

        @Test
        @DisplayName("Tìm thấy UserAdmin → mapRow nhánh admin")
        void findAdmin() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupAdminRs(rs, 99, "admin01", "SUPER_ADMIN");

            Optional<User> result = dao.findById(99);

            assertTrue(result.isPresent());
            assertInstanceOf(UserAdmin.class, result.get());
        }

        @Test
        @DisplayName("Không tìm thấy → Optional.empty()")
        void notFound() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            assertTrue(dao.findById(999).isEmpty());
        }

        @Test
        @DisplayName("User SUSPENDED — status map đúng")
        void userSuspended() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupMemberRs(rs, 3, "suspended_user", "SUSPENDED");

            Optional<User> result = dao.findById(3);

            assertTrue(result.isPresent());
            assertEquals(UserStatus.SUSPENDED, result.get().getStatus());
        }

        @Test
        @DisplayName("User BANNED — status map đúng")
        void userBanned() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupMemberRs(rs, 4, "banned_user", "BANNED");

            Optional<User> result = dao.findById(4);

            assertTrue(result.isPresent());
            assertEquals(UserStatus.BANNED, result.get().getStatus());
        }

        @Test
        @DisplayName("roles = null → default BIDDER (branch: rolesStr==null)")
        void rolesNull_defaultBidder() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupMemberRs(rs, 1, "u", "ACTIVE");
            when(rs.getString("roles")).thenReturn(null);

            Optional<User> result = dao.findById(1);

            assertTrue(result.isPresent());
            assertTrue(((UserMember) result.get()).getRoles().contains(UserRole.BIDDER));
        }

        @Test
        @DisplayName("roles = '' (blank) → default BIDDER (branch: rolesStr.isBlank())")
        void rolesBlank_defaultBidder() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupMemberRs(rs, 1, "u", "ACTIVE");
            when(rs.getString("roles")).thenReturn("   "); // blank

            Optional<User> result = dao.findById(1);

            assertTrue(result.isPresent());
            assertTrue(((UserMember) result.get()).getRoles().contains(UserRole.BIDDER));
        }

        @Test
        @DisplayName("roles = 'BIDDER,SELLER' → cả hai role")
        void rolesBidderAndSeller() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupMemberRs(rs, 1, "u", "ACTIVE");
            when(rs.getString("roles")).thenReturn("BIDDER,SELLER");

            UserMember m = (UserMember) dao.findById(1).get();

            assertTrue(m.getRoles().contains(UserRole.BIDDER));
            assertTrue(m.getRoles().contains(UserRole.SELLER));
        }

        @Test
        @DisplayName("roles với segment rỗng 'BIDDER,,SELLER' → filter bỏ empty segment")
        void rolesWithEmptySegment_filterWorks() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupMemberRs(rs, 1, "u", "ACTIVE");
            when(rs.getString("roles")).thenReturn("BIDDER,,SELLER"); // empty segment giữa

            UserMember m = (UserMember) dao.findById(1).get();

            // Không có NullPointerException / IllegalArgumentException
            assertTrue(m.getRoles().contains(UserRole.BIDDER));
            assertTrue(m.getRoles().contains(UserRole.SELLER));
        }

        @Test
        @DisplayName("SQLException từ prepareStatement → ném lên")
        void sqlException() throws SQLException {
            when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));
            assertThrows(SQLException.class, () -> dao.findById(1));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. findByEmail()  —  62% branch
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findByEmail()")
    class FindByEmailTests {

        @Test
        @DisplayName("Tìm thấy — trim + toLowerCase áp dụng đúng")
        void found_trimLowercase() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupMemberRs(rs, 2, "user02", "ACTIVE");

            Optional<User> result = dao.findByEmail("  USER02@TEST.COM  ");

            assertTrue(result.isPresent());
            verify(ps).setString(1, "user02@test.com");
        }

        @Test
        @DisplayName("Không tìm thấy → Optional.empty()")
        void notFound() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            assertTrue(dao.findByEmail("notexist@e.com").isEmpty());
        }

        @Test
        @DisplayName("SQLException → ném lên")
        void sqlException() throws SQLException {
            when(conn.prepareStatement(anyString())).thenThrow(new SQLException("timeout"));
            assertThrows(SQLException.class, () -> dao.findByEmail("x@e.com"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. findByUsername()  —  62% branch
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findByUsername()")
    class FindByUsernameTests {

        @Test
        @DisplayName("Tìm thấy — trim + toLowerCase áp dụng đúng")
        void found_trimLowercase() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupMemberRs(rs, 3, "user03", "ACTIVE");

            Optional<User> result = dao.findByUsername("  USER03  ");

            assertTrue(result.isPresent());
            verify(ps).setString(1, "user03");
        }

        @Test
        @DisplayName("Không tìm thấy → Optional.empty()")
        void notFound() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            assertTrue(dao.findByUsername("ghost").isEmpty());
        }

        @Test
        @DisplayName("SQLException → ném lên")
        void sqlException() throws SQLException {
            when(conn.prepareStatement(anyString())).thenThrow(new SQLException("timeout"));
            assertThrows(SQLException.class, () -> dao.findByUsername("someone"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. updateProfile()  —  75% branch → thiếu avatarUrl blank, address blank
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfileTests {

        @Test
        @DisplayName("avatarUrl null → setNull (branch: avatarUrl==null)")
        void avatarUrlNull_setsNull() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            UserMember m = UserMember.builder("u", "e@e.com", "pw")
                    .avatarUrl(null).build();
            m.assignId(1);

            dao.updateProfile(m);

            verify(ps).setNull(eq(3), anyInt());
        }

        @Test
        @DisplayName("avatarUrl blank '  ' → setNull (branch: avatarUrl.isBlank()=true)")
        void avatarUrlBlank_setsNull() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            UserMember m = UserMember.builder("u", "e@e.com", "pw")
                    .avatarUrl("   ").build(); // blank string
            m.assignId(1);

            dao.updateProfile(m);

            verify(ps).setNull(eq(3), anyInt());
        }

        @Test
        @DisplayName("avatarUrl có giá trị → setString (branch: avatarUrl.isBlank()=false)")
        void avatarUrlSet_setsString() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            UserMember m = UserMember.builder("u", "e@e.com", "pw")
                    .avatarUrl("http://img.com/a.png").build();
            m.assignId(1);

            dao.updateProfile(m);

            verify(ps).setString(3, "http://img.com/a.png");
        }

        @Test
        @DisplayName("address null → setNull (branch: address==null)")
        void addressNull_setsNull() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            UserMember m = UserMember.builder("u", "e@e.com", "pw").build(); // address = null
            m.assignId(1);

            dao.updateProfile(m);

            verify(ps).setNull(eq(4), anyInt());
        }

        @Test
        @DisplayName("address blank '  ' → setNull (branch: address.isBlank()=true)")
        void addressBlank_setsNull() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            UserMember m = UserMember.builder("u", "e@e.com", "pw")
                    .address("   ").build(); // blank
            m.assignId(1);

            dao.updateProfile(m);

            verify(ps).setNull(eq(4), anyInt());
        }

        @Test
        @DisplayName("address có giá trị → setString (branch: address.isBlank()=false)")
        void addressSet_setsString() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            UserMember m = UserMember.builder("u", "e@e.com", "pw")
                    .address("Hanoi, Vietnam").build();
            m.assignId(1);

            dao.updateProfile(m);

            verify(ps).setString(4, "Hanoi, Vietnam");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 6. updateStatus()
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatusTests {

        @Test
        @DisplayName("ACTIVE → thành công")
        void updateActive() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> dao.updateStatus(5, UserStatus.ACTIVE));
            verify(ps).setString(1, "ACTIVE");
            verify(ps).setInt(2, 5);
        }

        @Test
        @DisplayName("SUSPENDED → thành công")
        void updateSuspended() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> dao.updateStatus(5, UserStatus.SUSPENDED));
            verify(ps).setString(1, "SUSPENDED");
        }

        @Test
        @DisplayName("BANNED → thành công")
        void updateBanned() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> dao.updateStatus(5, UserStatus.BANNED));
            verify(ps).setString(1, "BANNED");
        }

        @Test
        @DisplayName("0 rows affected → UserNotFoundException")
        void zeroRows_throwsNotFound() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(0);

            assertThrows(UserNotFoundException.class,
                    () -> dao.updateStatus(999, UserStatus.ACTIVE));
        }

        @Test
        @DisplayName("SQLException → ném lên")
        void sqlException() throws SQLException {
            when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));
            assertThrows(SQLException.class, () -> dao.updateStatus(1, UserStatus.ACTIVE));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 7. softDelete()
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("softDelete()")
    class SoftDeleteTests {

        @Test
        @DisplayName("Soft delete thành công")
        void success() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> dao.softDelete(3));
            verify(ps).setInt(1, 3);
        }

        @Test
        @DisplayName("0 rows affected → UserNotFoundException")
        void zeroRows_throwsNotFound() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(0);

            assertThrows(UserNotFoundException.class, () -> dao.softDelete(999));
        }

        @Test
        @DisplayName("SQLException → ném lên")
        void sqlException() throws SQLException {
            when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));
            assertThrows(SQLException.class, () -> dao.softDelete(1));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 8. updatePassword()  —  0% → cần tất cả branches
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updatePassword()")
    class UpdatePasswordTests {

        @Test
        @DisplayName("Thành công — PasswordUtil.hash() được gọi, DB update 1 row")
        void success() throws SQLException {
            try (MockedStatic<PasswordUtil> pwUtil = mockStatic(PasswordUtil.class)) {
                pwUtil.when(() -> PasswordUtil.hash("newPass123"))
                        .thenReturn("$2a$12$newhashedpw");

                when(conn.prepareStatement(anyString())).thenReturn(ps);
                when(ps.executeUpdate()).thenReturn(1);

                assertDoesNotThrow(() -> dao.updatePassword(5, "newPass123"));

                verify(ps).setString(1, "$2a$12$newhashedpw");
                verify(ps).setInt(2, 5);
            }
        }

        @Test
        @DisplayName("0 rows affected → UserNotFoundException")
        void zeroRows_throwsNotFound() throws SQLException {
            try (MockedStatic<PasswordUtil> pwUtil = mockStatic(PasswordUtil.class)) {
                pwUtil.when(() -> PasswordUtil.hash(anyString())).thenReturn("$hashed");
                when(conn.prepareStatement(anyString())).thenReturn(ps);
                when(ps.executeUpdate()).thenReturn(0);

                assertThrows(UserNotFoundException.class,
                        () -> dao.updatePassword(999, "anyPass"));
            }
        }

        @Test
        @DisplayName("SQLException từ DB → ném lên")
        void sqlException() throws SQLException {
            try (MockedStatic<PasswordUtil> pwUtil = mockStatic(PasswordUtil.class)) {
                pwUtil.when(() -> PasswordUtil.hash(anyString())).thenReturn("$hashed");
                when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB lỗi"));

                assertThrows(SQLException.class,
                        () -> dao.updatePassword(1, "anyPass"));
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 9. authenticate()  —  0% → cần tất cả branches
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("authenticate()")
    class AuthenticateTests {

        /** Setup cho findByUsername trả về member */
        private void mockFindUsernameReturnsActive(String username) throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupMemberRs(rs, 10, username, "ACTIVE");
        }

        @Test
        @DisplayName("Username không tồn tại → Optional.empty() (branch: opt.isEmpty())")
        void usernameNotFound_returnsEmpty() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false); // findByUsername trả empty

            Optional<User> result = dao.authenticate("ghost", "anyPass");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Sai mật khẩu → Optional.empty() (branch: !PasswordUtil.verify())")
        void wrongPassword_returnsEmpty() throws SQLException {
            try (MockedStatic<PasswordUtil> pwUtil = mockStatic(PasswordUtil.class)) {
                mockFindUsernameReturnsActive("user10");
                // verify trả false → sai mật khẩu
                pwUtil.when(() -> PasswordUtil.verify(eq("wrongPass"), anyString()))
                        .thenReturn(false);

                Optional<User> result = dao.authenticate("user10", "wrongPass");

                assertTrue(result.isEmpty());
            }
        }

        @Test
        @DisplayName("Đúng mật khẩu nhưng tài khoản SUSPENDED → Optional.empty() (branch: !isActive())")
        void correctPasswordButSuspended_returnsEmpty() throws SQLException {
            try (MockedStatic<PasswordUtil> pwUtil = mockStatic(PasswordUtil.class)) {
                when(conn.prepareStatement(anyString())).thenReturn(ps);
                when(ps.executeQuery()).thenReturn(rs);
                when(rs.next()).thenReturn(true);
                setupMemberRs(rs, 11, "suspended01", "SUSPENDED"); // SUSPENDED → !isActive()

                pwUtil.when(() -> PasswordUtil.verify(anyString(), anyString()))
                        .thenReturn(true); // mật khẩu đúng

                Optional<User> result = dao.authenticate("suspended01", "correctPass");

                assertTrue(result.isEmpty());
            }
        }

        @Test
        @DisplayName("Đúng mật khẩu nhưng tài khoản BANNED → Optional.empty()")
        void correctPasswordButBanned_returnsEmpty() throws SQLException {
            try (MockedStatic<PasswordUtil> pwUtil = mockStatic(PasswordUtil.class)) {
                when(conn.prepareStatement(anyString())).thenReturn(ps);
                when(ps.executeQuery()).thenReturn(rs);
                when(rs.next()).thenReturn(true);
                setupMemberRs(rs, 12, "banned01", "BANNED");

                pwUtil.when(() -> PasswordUtil.verify(anyString(), anyString()))
                        .thenReturn(true);

                Optional<User> result = dao.authenticate("banned01", "correctPass");

                assertTrue(result.isEmpty());
            }
        }

        @Test
        @DisplayName("Đúng mật khẩu, tài khoản ACTIVE → Optional.of(user) (happy path)")
        void success_returnsUser() throws SQLException {
            try (MockedStatic<PasswordUtil> pwUtil = mockStatic(PasswordUtil.class)) {
                mockFindUsernameReturnsActive("user10");
                pwUtil.when(() -> PasswordUtil.verify(eq("correctPass"), anyString()))
                        .thenReturn(true);

                Optional<User> result = dao.authenticate("user10", "correctPass");

                assertTrue(result.isPresent());
                assertEquals("user10", result.get().getUsername());
                assertTrue(result.get().isActive());
            }
        }

        @Test
        @DisplayName("SQLException từ DB → ném lên")
        void sqlException() throws SQLException {
            when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));
            assertThrows(SQLException.class, () -> dao.authenticate("u", "p"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 10. exists*()  —  existsByUsername 75% (rs.next()=false branch)
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("existsByEmail / existsByUsername / existsByPhone")
    class ExistsTests {

        @Test
        @DisplayName("existsByEmail = true khi tồn tại (rs.next()=true, getBoolean=true)")
        void existsByEmail_true() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getBoolean(1)).thenReturn(true);

            assertTrue(dao.existsByEmail("test@e.com"));
            verify(ps).setString(1, "test@e.com");
        }

        @Test
        @DisplayName("existsByEmail = false (rs.next()=true, getBoolean=false)")
        void existsByEmail_false_getBoolean() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getBoolean(1)).thenReturn(false);

            assertFalse(dao.existsByEmail("nobody@e.com"));
        }

        @Test
        @DisplayName("existsByEmail = false (rs.next()=false → short-circuit)")
        void existsByEmail_false_rsEmpty() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            assertFalse(dao.existsByEmail("x@e.com"));
        }

        @Test
        @DisplayName("existsByEmail trim + lowercase áp dụng đúng")
        void existsByEmail_trimLower() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            dao.existsByEmail("  TEST@E.COM  ");
            verify(ps).setString(1, "test@e.com");
        }

        @Test
        @DisplayName("existsByUsername = true")
        void existsByUsername_true() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getBoolean(1)).thenReturn(true);

            assertTrue(dao.existsByUsername("user01"));
        }

        @Test
        @DisplayName("existsByUsername = false (rs.next()=true, getBoolean=false)")
        void existsByUsername_false_getBoolean() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getBoolean(1)).thenReturn(false);

            assertFalse(dao.existsByUsername("nobody"));
        }

        @Test
        @DisplayName("existsByUsername = false (rs.next()=false → short-circuit && branch)")
        void existsByUsername_false_rsEmpty() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            assertFalse(dao.existsByUsername("x"));
        }

        @Test
        @DisplayName("existsByUsername trim + lowercase")
        void existsByUsername_trimLower() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            dao.existsByUsername("  USER01  ");
            verify(ps).setString(1, "user01");
        }

        @Test
        @DisplayName("existsByPhone = true")
        void existsByPhone_true() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getBoolean(1)).thenReturn(true);

            assertTrue(dao.existsByPhone("0900000000"));
            verify(ps).setString(1, "0900000000");
        }

        @Test
        @DisplayName("existsByPhone = false")
        void existsByPhone_false() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getBoolean(1)).thenReturn(false);

            assertFalse(dao.existsByPhone("0000000000"));
        }

        @Test
        @DisplayName("existsByPhone = false (rs.next()=false)")
        void existsByPhone_false_rsEmpty() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            assertFalse(dao.existsByPhone("0999999999"));
        }

        @Test
        @DisplayName("existsByPhone trim giữ đúng số")
        void existsByPhone_trim() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            dao.existsByPhone("  0912345678  ");
            verify(ps).setString(1, "0912345678");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 11. updateLastLogin() / updateSellerRating()
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("updateLastLogin() gọi đúng params")
    void updateLastLogin() throws SQLException {
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        dao.updateLastLogin(7);

        verify(ps).setTimestamp(eq(1), any(Timestamp.class));
        verify(ps).setInt(2, 7);
        verify(ps).executeUpdate();
    }

    @Test
    @DisplayName("updateSellerRating() gọi đúng 2 lần setInt với sellerId")
    void updateSellerRating() throws SQLException {
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        dao.updateSellerRating(10);

        verify(ps).setInt(1, 10);
        verify(ps).setInt(2, 10);
        verify(ps).executeUpdate();
    }

    // ════════════════════════════════════════════════════════════════════════
    // 12. addRole()
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("addRole()")
    class AddRoleTests {

        @Test
        @DisplayName("Thêm SELLER role thành công")
        void addSellerRole() throws SQLException {
            // Call 1: findById (SELECT)
            when(conn.prepareStatement(anyString()))
                    .thenReturn(ps)   // SELECT for findById
                    .thenReturn(ps2); // UPDATE roles
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupMemberRs(rs, 5, "user05", "ACTIVE");

            dao.addRole(5, UserRole.SELLER);

            verify(ps2).setString(eq(1), contains("SELLER"));
            verify(ps2).setInt(2, 5);
            verify(ps2).executeUpdate();
        }

        @Test
        @DisplayName("User không tồn tại → UserNotFoundException")
        void userNotFound() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            assertThrows(UserNotFoundException.class,
                    () -> dao.addRole(999, UserRole.SELLER));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 13. findAllSellers()
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findAllSellers()")
    class FindAllSellersTests {

        @Test
        @DisplayName("Trả đúng UserMember có SELLER role")
        void returnsMemberList() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true, false);
            setupMemberRs(rs, 1, "seller01", "ACTIVE");
            when(rs.getString("roles")).thenReturn("BIDDER,SELLER");

            List<UserMember> sellers = dao.findAllSellers();

            assertEquals(1, sellers.size());
            assertInstanceOf(UserMember.class, sellers.get(0));
        }

        @Test
        @DisplayName("UserAdmin trong kết quả bị lọc ra (chỉ trả UserMember)")
        void adminFiltered_notReturned() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true, false);
            setupAdminRs(rs, 99, "admin_seller", "SUPER_ADMIN"); // admin → bị lọc

            List<UserMember> sellers = dao.findAllSellers();

            assertTrue(sellers.isEmpty(), "Admin không được trả về trong findAllSellers");
        }

        @Test
        @DisplayName("Không có seller → list rỗng")
        void emptyList() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            assertTrue(dao.findAllSellers().isEmpty());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 14. insertTransactionAndUpdateBalance()  —  0% → tất cả branches
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("insertTransactionAndUpdateBalance()")
    class InsertTransactionTests {

        // Thứ tự gọi prepareStatement khi existsTransaction=false:
        //   call 1: existsTransaction   → SELECT EXISTS from wallet_transaction
        //   call 2: UPDATE users balance
        //   call 3: insertTransaction   → INSERT INTO wallet_transaction
        //   call 4: SELECT balance FROM users (sau update)

        // Thứ tự khi existsTransaction=true:
        //   call 1: existsTransaction → SELECT EXISTS
        //   call 2: SELECT balance FROM users (trong if block)

        @Test
        @DisplayName("existsTransaction=true, balance tìm thấy → trả balance hiện tại (duplicate tx)")
        void duplicateTransaction_returnsCurrentBalance() throws SQLException {
            // call 1: existsTransaction SELECT → true
            when(conn.prepareStatement(anyString()))
                    .thenReturn(ps)   // existsTransaction
                    .thenReturn(ps2); // SELECT balance

            when(conn.getAutoCommit()).thenReturn(true);

            // existsTransaction returns true
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getBoolean(1)).thenReturn(true);

            // SELECT balance returns 200000
            when(ps2.executeQuery()).thenReturn(rs2);
            when(rs2.next()).thenReturn(true);
            when(rs2.getBigDecimal("balance")).thenReturn(new BigDecimal("200000"));

            BigDecimal result = dao.insertTransactionAndUpdateBalance(
                    "txn-dup-001", 1, new BigDecimal("50000"),
                    TransactionType.DEPOSIT, PaymentMethod.BANK_TRANSFER);

            assertEquals(new BigDecimal("200000"), result);
            // commit KHÔNG được gọi (giao dịch đã tồn tại)
            verify(conn, never()).commit();
        }

        @Test
        @DisplayName("Normal flow thành công → commit, trả balance mới")
        void normalFlow_success() throws SQLException {
            // Sequence: existsCheck(false) → UPDATE → INSERT_TX → SELECT_balance
            when(conn.prepareStatement(anyString()))
                    .thenReturn(ps)   // 1: existsTransaction
                    .thenReturn(ps2)  // 2: UPDATE balance
                    .thenReturn(ps3)  // 3: insertTransaction
                    .thenReturn(ps4); // 4: SELECT balance

            when(conn.getAutoCommit()).thenReturn(true);

            // existsTransaction → false
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getBoolean(1)).thenReturn(false);

            // UPDATE returns 1 row affected
            when(ps2.executeUpdate()).thenReturn(1);

            // insertTransaction → OK (no setup needed for executeUpdate)

            // SELECT balance → 500000
            when(ps4.executeQuery()).thenReturn(rs2);
            when(rs2.next()).thenReturn(true);
            when(rs2.getBigDecimal("balance")).thenReturn(new BigDecimal("500000"));

            BigDecimal result = dao.insertTransactionAndUpdateBalance(
                    "txn-new-001", 5, new BigDecimal("100000"),
                    TransactionType.DEPOSIT, PaymentMethod.BANK_TRANSFER);

            assertEquals(new BigDecimal("500000"), result);
            verify(conn).setAutoCommit(false);
            verify(conn).commit();
            verify(conn).setAutoCommit(true);
            verify(conn).close();
        }



        @Test
        @DisplayName("SELECT balance sau update rs.next()=false → SQLException + rollback")
        void selectBalanceEmpty_sqlException_rollback() throws SQLException {
            when(conn.prepareStatement(anyString()))
                    .thenReturn(ps)   // existsTransaction
                    .thenReturn(ps2)  // UPDATE balance
                    .thenReturn(ps3)  // insertTransaction
                    .thenReturn(ps4); // SELECT balance

            when(conn.getAutoCommit()).thenReturn(true);

            // existsTransaction → false
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getBoolean(1)).thenReturn(false);

            // UPDATE ok
            when(ps2.executeUpdate()).thenReturn(1);

            // SELECT balance rs.next() = false → hệ thống lỗi
            when(ps4.executeQuery()).thenReturn(rs2);
            when(rs2.next()).thenReturn(false);

            assertThrows(SQLException.class,
                    () -> dao.insertTransactionAndUpdateBalance(
                            "txn-empty-001", 5, new BigDecimal("50000"),
                            TransactionType.DEPOSIT, PaymentMethod.BANK_TRANSFER));

            verify(conn).rollback();
        }

        @Test
        @DisplayName("SQLException trong UPDATE → rollback, ném lại exception gốc")
        void updateThrowsSQLException_rollback() throws SQLException {
            when(conn.prepareStatement(anyString()))
                    .thenReturn(ps)   // existsTransaction
                    .thenReturn(ps2); // UPDATE balance → throw

            when(conn.getAutoCommit()).thenReturn(true);

            // existsTransaction → false
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getBoolean(1)).thenReturn(false);

            // UPDATE ném SQLException
            when(ps2.executeUpdate()).thenThrow(new SQLException("DB update lỗi"));

            SQLException thrown = assertThrows(SQLException.class,
                    () -> dao.insertTransactionAndUpdateBalance(
                            "txn-ex-001", 5, new BigDecimal("50000"),
                            TransactionType.DEPOSIT, PaymentMethod.BANK_TRANSFER));

            assertEquals("DB update lỗi", thrown.getMessage());
            verify(conn).rollback();
        }

    }

    // ════════════════════════════════════════════════════════════════════════
    // 15. settleAuctionPayment()  —  0% → tất cả branches
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("settleAuctionPayment()")
    class SettleAuctionPaymentTests {

        // Thứ tự gọi prepareStatement khi success:
        //   call 1: UPDATE frozen_balance (buyer)
        //   call 2: INSERT INTO wallet_transaction (winnerTx)
        //   call 3: UPDATE balance (seller)
        //   call 4: INSERT INTO wallet_transaction (sellerTx)

        @Test
        @DisplayName("Thành công — cả hai bên được cập nhật, commit")
        void success() throws SQLException {
            when(conn.prepareStatement(anyString()))
                    .thenReturn(ps)   // UPDATE frozen_balance (buyer)
                    .thenReturn(ps2)  // INSERT winner tx
                    .thenReturn(ps3)  // UPDATE balance (seller)
                    .thenReturn(ps4); // INSERT seller tx

            when(conn.getAutoCommit()).thenReturn(true);
            when(ps.executeUpdate()).thenReturn(1);  // buyer ok
            when(ps3.executeUpdate()).thenReturn(1); // seller ok

            assertDoesNotThrow(() -> dao.settleAuctionPayment(
                    "wtx-winner", "wtx-seller",
                    10, 20, 5,
                    new BigDecimal("500000"),
                    TransactionType.BID_PAYMENT));

            verify(conn).setAutoCommit(false);
            verify(conn).commit();
            verify(conn).setAutoCommit(true);
            verify(conn).close();
        }



        @Test
        @DisplayName("UPDATE balance seller = 0 rows → SQLException + rollback")
        void sellerUpdateFailed_rollback() throws SQLException {
            when(conn.prepareStatement(anyString()))
                    .thenReturn(ps)   // buyer UPDATE
                    .thenReturn(ps2)  // INSERT winner tx
                    .thenReturn(ps3); // seller UPDATE → 0 rows

            when(conn.getAutoCommit()).thenReturn(true);
            when(ps.executeUpdate()).thenReturn(1);  // buyer ok
            when(ps3.executeUpdate()).thenReturn(0); // seller thất bại

            assertThrows(SQLException.class,
                    () -> dao.settleAuctionPayment(
                            "wtx-w", "wtx-s",
                            10, 20, 5,
                            new BigDecimal("500000"),
                            TransactionType.BID_PAYMENT));

            verify(conn).rollback();
        }

        @Test
        @DisplayName("SQLException trong flow → rollback, ném lại exception gốc")
        void sqlExceptionInFlow_rollback() throws SQLException {
            when(conn.prepareStatement(anyString()))
                    .thenReturn(ps)
                    .thenReturn(ps2);

            when(conn.getAutoCommit()).thenReturn(true);
            when(ps.executeUpdate()).thenReturn(1); // buyer ok
            // INSERT winner tx ném SQLException
            when(ps2.executeUpdate()).thenThrow(new SQLException("INSERT lỗi"));

            SQLException thrown = assertThrows(SQLException.class,
                    () -> dao.settleAuctionPayment(
                            "wtx-w", "wtx-s",
                            10, 20, 5,
                            new BigDecimal("500000"),
                            TransactionType.BID_PAYMENT));

            assertEquals("INSERT lỗi", thrown.getMessage());
            verify(conn).rollback();
        }


    }
}