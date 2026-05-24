package vn.edu.vnu.uet.group8.server.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.UserAdmin;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.UserRole;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserDAOTest {

    @Mock private DatabaseConnection dbConn;
    @Mock private Connection conn;
    @Mock private PreparedStatement ps;
    @Mock private PreparedStatement ps2; // dùng cho addRole (2 PreparedStatement)
    @Mock private ResultSet rs;
    @Mock private ResultSet keyRs;

    private UserDAO dao;
    private MockedStatic<DatabaseConnection> staticMock;

    @BeforeEach
    void setUp() throws SQLException {
        staticMock = mockStatic(DatabaseConnection.class);
        staticMock.when(DatabaseConnection::getInstance).thenReturn(dbConn);
        when(dbConn.getConnection()).thenReturn(conn);
        dao = new UserDAO();
    }

    @AfterEach
    void tearDown() {
        staticMock.close();
    }

    // ─── mapRow helpers ──────────────────────────────────────────
    private void setupMemberRs(int id, String username, String status) throws SQLException {
        when(rs.getInt("user_id")).thenReturn(id);
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.now()));
        when(rs.getBoolean("is_deleted")).thenReturn(false);
        when(rs.getString("username")).thenReturn(username);
        when(rs.getString("email")).thenReturn(username + "@test.com");
        when(rs.getString("password_hash")).thenReturn("hashed");
        when(rs.getString("status")).thenReturn(status);
        when(rs.getTimestamp("last_login_at")).thenReturn(null);
        when(rs.getString("admin_level")).thenReturn(null); // → UserMember branch
        when(rs.getString("full_name")).thenReturn("Nguyen Van A");
        when(rs.getString("roles")).thenReturn("BIDDER");
        when(rs.getBigDecimal("balance")).thenReturn(new BigDecimal("100000"));
        when(rs.getString("phone")).thenReturn("0900000000");
        when(rs.getString("address")).thenReturn("Hanoi");
        when(rs.getBigDecimal("seller_rating")).thenReturn(null);
        when(rs.getString("avatar_url")).thenReturn(null);
        when(rs.getInt("total_bids_placed")).thenReturn(0);
        when(rs.getInt("total_items_sold")).thenReturn(0);
    }

    private void setupAdminRs(int id, String username, String adminLevel) throws SQLException {
        when(rs.getInt("user_id")).thenReturn(id);
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.now()));
        when(rs.getBoolean("is_deleted")).thenReturn(false);
        when(rs.getString("username")).thenReturn(username);
        when(rs.getString("email")).thenReturn(username + "@admin.com");
        when(rs.getString("password_hash")).thenReturn("hashed_admin");
        when(rs.getString("status")).thenReturn("ACTIVE");
        when(rs.getTimestamp("last_login_at")).thenReturn(null);
        when(rs.getString("admin_level")).thenReturn(adminLevel); // → UserAdmin branch
        when(rs.getString("full_name")).thenReturn("Admin User");
    }

    // ─────────────────────────────────────────────────────────────
    // insert
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("insert()")
    class InsertTest {

        @Test
        @DisplayName("insert UserMember thành công - gán id từ generated key")
        void insertMemberThanhCong() throws SQLException {
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
        }

        @Test
        @DisplayName("insert UserAdmin thành công")
        void insertAdminThanhCong() throws SQLException {
            when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenReturn(ps);
            when(ps.getGeneratedKeys()).thenReturn(keyRs);
            when(keyRs.next()).thenReturn(true);
            when(keyRs.getInt(1)).thenReturn(99);

            UserAdmin admin = new UserAdmin.Builder(
                    "admin01", "a@e.com", "hashedpw",
                    vn.edu.vnu.uet.group8.common.enums.AdminLevel.SUPER_ADMIN).build();
            dao.insert(admin);

            assertEquals(99, admin.getId());
        }

        @Test
        @DisplayName("insert - không lấy được key → SQLException")
        void insertNoKey() throws SQLException {
            when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenReturn(ps);
            when(ps.getGeneratedKeys()).thenReturn(keyRs);
            when(keyRs.next()).thenReturn(false);

            assertThrows(SQLException.class,
                    () -> dao.insert(UserMember.builder("u", "e@e.com", "pw").build()));
        }

        @Test
        @DisplayName("insert - SQLException từ DB → ném lên")
        void insertSQLException() throws SQLException {
            when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenThrow(new SQLException("DB lỗi"));

            assertThrows(SQLException.class,
                    () -> dao.insert(UserMember.builder("u", "e@e.com", "pw").build()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // findById
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("findById()")
    class FindByIdTest {

        @Test
        @DisplayName("Tìm thấy UserMember → mapRow nhánh member")
        void timThayMember() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupMemberRs(5, "member01", "ACTIVE");

            Optional<User> result = dao.findById(5);

            assertTrue(result.isPresent());
            assertInstanceOf(UserMember.class, result.get());
            assertEquals("member01", result.get().getUsername());
            verify(ps).setInt(1, 5);
        }

        @Test
        @DisplayName("Tìm thấy UserAdmin → mapRow nhánh admin")
        void timThayAdmin() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupAdminRs(99, "admin01", "SUPER_ADMIN");

            Optional<User> result = dao.findById(99);

            assertTrue(result.isPresent());
            assertInstanceOf(UserAdmin.class, result.get());
        }

        @Test
        @DisplayName("Không tìm thấy → Optional.empty()")
        void khongTimThay() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            assertTrue(dao.findById(999).isEmpty());
        }

        @Test
        @DisplayName("User SUSPENDED vẫn map đúng status")
        void userSuspended() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupMemberRs(3, "suspended_user", "SUSPENDED");

            Optional<User> result = dao.findById(3);

            assertTrue(result.isPresent());
            assertEquals(UserStatus.SUSPENDED, result.get().getStatus());
        }

        @Test
        @DisplayName("roles = null → default BIDDER")
        void rolesNull() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupMemberRs(1, "u", "ACTIVE");
            when(rs.getString("roles")).thenReturn(null); // override

            Optional<User> result = dao.findById(1);

            assertTrue(result.isPresent());
            UserMember m = (UserMember) result.get();
            assertTrue(m.getRoles().contains(UserRole.BIDDER));
        }

        @Test
        @DisplayName("roles = BIDDER,SELLER → cả hai role")
        void rolesBidderSeller() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupMemberRs(1, "u", "ACTIVE");
            when(rs.getString("roles")).thenReturn("BIDDER,SELLER");

            Optional<User> result = dao.findById(1);

            UserMember m = (UserMember) result.get();
            assertTrue(m.getRoles().contains(UserRole.BIDDER));
            assertTrue(m.getRoles().contains(UserRole.SELLER));
        }

        @Test
        @DisplayName("SQLException → ném lên")
        void sqlException() throws SQLException {
            when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));
            assertThrows(SQLException.class, () -> dao.findById(1));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // findByEmail
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("findByEmail()")
    class FindByEmailTest {

        @Test
        @DisplayName("Tìm thấy → trả Optional.of(user)")
        void timThay() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupMemberRs(2, "user02", "ACTIVE");

            Optional<User> result = dao.findByEmail("  USER02@TEST.COM  "); // trim + lower

            assertTrue(result.isPresent());
            verify(ps).setString(1, "user02@test.com");
        }

        @Test
        @DisplayName("Không tìm thấy → Optional.empty()")
        void khongTimThay() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            assertTrue(dao.findByEmail("notexist@e.com").isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // findByUsername
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("findByUsername()")
    class FindByUsernameTest {

        @Test
        @DisplayName("Tìm thấy → trả Optional.of(user)")
        void timThay() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupMemberRs(3, "user03", "ACTIVE");

            Optional<User> result = dao.findByUsername("  USER03  "); // trim + lower

            assertTrue(result.isPresent());
            verify(ps).setString(1, "user03");
        }

        @Test
        @DisplayName("Không tìm thấy → Optional.empty()")
        void khongTimThay() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            assertTrue(dao.findByUsername("ghost").isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // updateStatus
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatusTest {

        @Test
        @DisplayName("Update ACTIVE thành công")
        void updateActive() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> dao.updateStatus(5, UserStatus.ACTIVE));
            verify(ps).setString(1, "ACTIVE");
            verify(ps).setInt(2, 5);
        }

        @Test
        @DisplayName("Update SUSPENDED thành công")
        void updateSuspended() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> dao.updateStatus(5, UserStatus.SUSPENDED));
            verify(ps).setString(1, "SUSPENDED");
        }

        @Test
        @DisplayName("Update BANNED thành công")
        void updateBanned() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> dao.updateStatus(5, UserStatus.BANNED));
        }

        @Test
        @DisplayName("0 rows affected → UserNotFoundException")
        void zeroRows() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(0);

            assertThrows(UserNotFoundException.class, () -> dao.updateStatus(999, UserStatus.ACTIVE));
        }

        @Test
        @DisplayName("SQLException → ném lên")
        void sqlException() throws SQLException {
            when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));
            assertThrows(SQLException.class, () -> dao.updateStatus(1, UserStatus.ACTIVE));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // softDelete
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("softDelete()")
    class SoftDeleteTest {

        @Test
        @DisplayName("Soft delete thành công")
        void thanhCong() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> dao.softDelete(3));
            verify(ps).setInt(1, 3);
        }

        @Test
        @DisplayName("0 rows affected → UserNotFoundException")
        void zeroRows() throws SQLException {
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

    // ─────────────────────────────────────────────────────────────
    // existsByEmail / existsByUsername / existsByPhone
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("existsByEmail/Username/Phone()")
    class ExistsTest {

        @Test
        @DisplayName("existsByEmail = true khi tồn tại")
        void existsByEmailTrue() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getBoolean(1)).thenReturn(true);

            assertTrue(dao.existsByEmail("test@e.com"));
            verify(ps).setString(1, "test@e.com");
        }

        @Test
        @DisplayName("existsByEmail = false khi không tồn tại")
        void existsByEmailFalse() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getBoolean(1)).thenReturn(false);

            assertFalse(dao.existsByEmail("notexist@e.com"));
        }

        @Test
        @DisplayName("existsByEmail - rs.next() = false → false")
        void existsByEmailRsEmpty() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            assertFalse(dao.existsByEmail("x@e.com"));
        }

        @Test
        @DisplayName("existsByEmail trim + lowercase")
        void existsByEmailTrimLower() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            dao.existsByEmail("  TEST@E.COM  ");
            verify(ps).setString(1, "test@e.com");
        }

        @Test
        @DisplayName("existsByUsername = true khi tồn tại")
        void existsByUsernameTrue() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getBoolean(1)).thenReturn(true);

            assertTrue(dao.existsByUsername("user01"));
        }

        @Test
        @DisplayName("existsByUsername trim + lowercase")
        void existsByUsernameTrimLower() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            dao.existsByUsername("  USER01  ");
            verify(ps).setString(1, "user01");
        }

        @Test
        @DisplayName("existsByPhone = true")
        void existsByPhoneTrue() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getBoolean(1)).thenReturn(true);

            assertTrue(dao.existsByPhone("0900000000"));
            verify(ps).setString(1, "0900000000");
        }

        @Test
        @DisplayName("existsByPhone = false")
        void existsByPhoneFalse() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getBoolean(1)).thenReturn(false);

            assertFalse(dao.existsByPhone("0000000000"));
        }

        @Test
        @DisplayName("existsByPhone trim giữ đúng số")
        void existsByPhoneTrim() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            dao.existsByPhone("  0912345678  ");
            verify(ps).setString(1, "0912345678");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // updateLastLogin
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("updateLastLogin() gọi đúng SQL")
    void updateLastLogin() throws SQLException {
        when(conn.prepareStatement(anyString())).thenReturn(ps);

        dao.updateLastLogin(7);

        verify(ps).setTimestamp(eq(1), any(Timestamp.class));
        verify(ps).setInt(2, 7);
        verify(ps).executeUpdate();
    }

    // ─────────────────────────────────────────────────────────────
    // updateSellerRating
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("updateSellerRating() gọi đúng SQL với sellerId ở cả 2 params")
    void updateSellerRating() throws SQLException {
        when(conn.prepareStatement(anyString())).thenReturn(ps);

        dao.updateSellerRating(10);

        verify(ps).setInt(1, 10);
        verify(ps).setInt(2, 10);
        verify(ps).executeUpdate();
    }

    // ─────────────────────────────────────────────────────────────
    // addRole
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("addRole()")
    class AddRoleTest {

        @Test
        @DisplayName("Thêm SELLER role thành công")
        void addSellerRole() throws SQLException {
            // findById call
            when(conn.prepareStatement(anyString()))
                    .thenReturn(ps)   // SELECT for findById
                    .thenReturn(ps2); // UPDATE for addRole
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            setupMemberRs(5, "user05", "ACTIVE");

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
            when(rs.next()).thenReturn(false); // findById returns empty

            assertThrows(UserNotFoundException.class, () -> dao.addRole(999, UserRole.SELLER));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // findAllSellers
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("findAllSellers()")
    class FindAllSellersTest {

        @Test
        @DisplayName("Trả về danh sách UserMember (chỉ member)")
        void traMember() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true, false);
            setupMemberRs(1, "seller01", "ACTIVE");
            when(rs.getString("roles")).thenReturn("BIDDER,SELLER");

            List<UserMember> sellers = dao.findAllSellers();

            assertEquals(1, sellers.size());
            assertInstanceOf(UserMember.class, sellers.get(0));
        }

        @Test
        @DisplayName("Admin bị loại - chỉ lấy UserMember")
        void adminBiLoai() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true, false);
            // mapRow returns UserAdmin (adminLevel != null) → không add vào list
            setupAdminRs(99, "admin_seller", "SUPER_ADMIN");

            List<UserMember> sellers = dao.findAllSellers();

            assertTrue(sellers.isEmpty(), "Admin không được trả về trong findAllSellers");
        }

        @Test
        @DisplayName("Không có seller → list rỗng")
        void listRong() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            assertTrue(dao.findAllSellers().isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // updateProfile
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfileTest {

        @Test
        @DisplayName("avatarUrl rỗng → setNull")
        void avatarUrlEmpty() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            UserMember m = UserMember.builder("u", "e@e.com", "pw")
                    .avatarUrl("").build();
            m.assignId(1);

            dao.updateProfile(m);

            verify(ps).setNull(eq(3), anyInt());
        }

        @Test
        @DisplayName("avatarUrl có giá trị → setString")
        void avatarUrlSet() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            UserMember m = UserMember.builder("u", "e@e.com", "pw")
                    .avatarUrl("http://img.com/a.png").build();
            m.assignId(1);

            dao.updateProfile(m);

            verify(ps).setString(3, "http://img.com/a.png");
        }

        @Test
        @DisplayName("address null → setNull")
        void addressNull() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            UserMember m = UserMember.builder("u", "e@e.com", "pw").build();
            m.assignId(1);

            dao.updateProfile(m);

            verify(ps).setNull(eq(4), anyInt());
        }

        @Test
        @DisplayName("address có giá trị → setString")
        void addressSet() throws SQLException {
            when(conn.prepareStatement(anyString())).thenReturn(ps);
            UserMember m = UserMember.builder("u", "e@e.com", "pw")
                    .address("Hanoi, Vietnam").build();
            m.assignId(1);

            dao.updateProfile(m);

            verify(ps).setString(4, "Hanoi, Vietnam");
        }
    }
}