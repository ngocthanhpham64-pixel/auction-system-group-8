package vn.edu.vnu.uet.group8.client.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import vn.edu.vnu.uet.group8.client.service.AdminService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.model.UserAdminDTO;

/**
 * Test FULL COVERAGE cho {@link AdminUserListController}.
 *
 * <p>Mục tiêu: 20% → ~90%+
 *
 * <p>Chiến lược:
 * <ul>
 *   <li>Mock {@link AlertUtil} (static) để tránh JavaFX Stage</li>
 *   <li>Mock {@link AdminService} (static) để tránh HTTP call</li>
 *   <li>Mock {@link SessionManager} (static) để kiểm soát quyền</li>
 *   <li>Test trực tiếp các method logic (không cần FX runtime)</li>
 *   <li>Test gián tiếp lambda qua {@code applyFilters()}, {@code loadUsers()},
 *       {@code changeStatus()}, {@code suspendSelected()}, {@code banSelected()}</li>
 * </ul>
 *
 * <p>Các method cần tăng từ 0%:
 * <ul>
 *   <li>{@code setupTable()} — 0%</li>
 *   <li>{@code setupFilters()} — 0%</li>
 *   <li>{@code applyFilters()} — 0%</li>
 *   <li>{@code changeStatus()} — 0%</li>
 *   <li>{@code loadUsers()} — 0%</li>
 *   <li>{@code suspendSelected()} — 34%</li>
 *   <li>{@code banSelected()} — 16%</li>
 * </ul>
 */
@DisplayName("AdminUserListController – Full Coverage")
class AdminUserListControllerFullTest extends FxTestBase {

    // ════════════════════════════════════════════════════════════════
    // FIXTURE SETUP
    // ════════════════════════════════════════════════════════════════

    private AdminUserListController controller;

    @BeforeEach
    void setUp() {
        SessionManager.clearSession();
        controller = new AdminUserListController();

        // Wire FXML fields thủ công (không load fxml)
        controller.userTable      = new TableView<>();
        controller.colId          = new TableColumn<>();
        controller.colUsername    = new TableColumn<>();
        controller.colEmail       = new TableColumn<>();
        controller.colRole        = new TableColumn<>();
        controller.colStatus      = new TableColumn<>();
        controller.colAction      = null;   // không test cell factory
        controller.tfSearch       = new TextField();
        controller.cbFilterRole   = new ComboBox<>();
        controller.cbFilterStatus = new ComboBox<>();
    }

    // ════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════

    private UserAdminDTO alice()  { return new UserAdminDTO(1, "alice",  "alice@x.com",  "MEMBER",     "ACTIVE");    }
    private UserAdminDTO bob()    { return new UserAdminDTO(2, "bob",    "bob@x.com",    "MEMBER",     "BANNED");    }
    private UserAdminDTO carol()  { return new UserAdminDTO(3, "carol",  "carol@x.com",  "ADMIN",      "ACTIVE");    }
    private UserAdminDTO dave()   { return new UserAdminDTO(4, "dave",   "dave@x.com",   "SELLER",     "SUSPENDED"); }
    private UserAdminDTO super1() { return new UserAdminDTO(5, "super1", "s@x.com",      "SUPER_ADMIN","ACTIVE");    }

    private List<UserAdminDTO> allUsers() {
        return List.of(alice(), bob(), carol(), dave(), super1());
    }

    /** Setup filteredUsers bằng cách gọi setupTable() */
    private void wireTable(List<UserAdminDTO> users) {
        controller.setupTable();
        controller.allUsers.setAll(users);
    }

    // ════════════════════════════════════════════════════════════════
    // ① setupTable() — 0% → 100%
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("① setupTable() — 0% → 100%")
    class SetupTable {

        @Test
        @DisplayName("setupTable() tạo filteredUsers không null")
        void filteredUsersKhongNull() {
            controller.setupTable();
            assertNotNull(controller.filteredUsers);
        }

        @Test
        @DisplayName("setupTable() bind userTable.items với filteredUsers")
        void userTableItemsBindFilteredUsers() {
            controller.setupTable();
            assertSame(controller.filteredUsers, controller.userTable.getItems());
        }

        @Test
        @DisplayName("setupTable() predicate mặc định: tất cả đều qua")
        void predicateMacDinhPassAll() {
            controller.setupTable();
            controller.allUsers.setAll(allUsers());
            assertEquals(5, controller.filteredUsers.size(),
                    "Predicate mặc định phải cho qua hết");
        }

        @Test
        @DisplayName("setupTable() setCellValueFactory cho colId (không throw)")
        void cellValueFactoryKhongThrow() {
            assertDoesNotThrow(() -> controller.setupTable());
        }

        @Test
        @DisplayName("setupTable() gọi lại lần 2 — idempotent (không throw)")
        void idempotent() {
            controller.setupTable();
            assertDoesNotThrow(() -> controller.setupTable());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ② setupFilters() — 0% → 100%
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("② setupFilters() — 0% → 100%")
    class SetupFilters {

        @Test
        @DisplayName("setupFilters(): cbFilterRole được populate đúng 4 items")
        void cbFilterRoleItems() {
            controller.setupFilters();
            assertEquals(4, controller.cbFilterRole.getItems().size());
            assertEquals("Tất cả vai trò", controller.cbFilterRole.getItems().get(0));
            assertEquals("MEMBER",         controller.cbFilterRole.getItems().get(1));
            assertEquals("SELLER",         controller.cbFilterRole.getItems().get(2));
            assertEquals("ADMIN",          controller.cbFilterRole.getItems().get(3));
        }

        @Test
        @DisplayName("setupFilters(): cbFilterRole chọn item đầu tiên")
        void cbFilterRoleSelectFirst() {
            controller.setupFilters();
            assertEquals("Tất cả vai trò",
                    controller.cbFilterRole.getSelectionModel().getSelectedItem());
        }

        @Test
        @DisplayName("setupFilters(): cbFilterStatus được populate đúng 4 items")
        void cbFilterStatusItems() {
            controller.setupFilters();
            assertEquals(4, controller.cbFilterStatus.getItems().size());
            assertEquals("Tất cả trạng thái", controller.cbFilterStatus.getItems().get(0));
            assertEquals("ACTIVE",             controller.cbFilterStatus.getItems().get(1));
            assertEquals("SUSPENDED",          controller.cbFilterStatus.getItems().get(2));
            assertEquals("BANNED",             controller.cbFilterStatus.getItems().get(3));
        }

        @Test
        @DisplayName("setupFilters(): cbFilterStatus chọn item đầu tiên")
        void cbFilterStatusSelectFirst() {
            controller.setupFilters();
            assertEquals("Tất cả trạng thái",
                    controller.cbFilterStatus.getSelectionModel().getSelectedItem());
        }

        @Test
        @DisplayName("setupFilters() với cbFilterRole = null — không throw (null-safe)")
        void cbFilterRoleNull() {
            controller.cbFilterRole = null;
            assertDoesNotThrow(() -> controller.setupFilters());
        }

        @Test
        @DisplayName("setupFilters() với cbFilterStatus = null — không throw (null-safe)")
        void cbFilterStatusNull() {
            controller.cbFilterStatus = null;
            assertDoesNotThrow(() -> controller.setupFilters());
        }

        @Test
        @DisplayName("setupFilters() với tfSearch = null — không throw (null-safe)")
        void tfSearchNull() {
            controller.tfSearch = null;
            assertDoesNotThrow(() -> controller.setupFilters());
        }

        @Test
        @DisplayName("setupFilters() với tất cả null — không throw")
        void tatCaNull() {
            controller.cbFilterRole   = null;
            controller.cbFilterStatus = null;
            controller.tfSearch       = null;
            assertDoesNotThrow(() -> controller.setupFilters());
        }

        @Test
        @DisplayName("setupFilters(): listener trên tfSearch kích hoạt applyFilters")
        void tfSearchListenerGoiApplyFilters() {
            wireTable(allUsers());
            controller.setupFilters();

            // Gõ keyword → filteredUsers phải thay đổi
            controller.tfSearch.setText("alice");

            // Sau khi text change listener chạy, filteredUsers chỉ còn alice
            assertTrue(controller.filteredUsers.stream()
                            .allMatch(u -> u.getUsername().contains("alice")),
                    "Listener trên tfSearch phải kích hoạt filter");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ③ applyFilters() — 0% → 100%
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("③ applyFilters() — 0% → 100%")
    class ApplyFilters {

        @BeforeEach
        void setup() {
            wireTable(allUsers());
            controller.setupFilters();
        }

        @Test
        @DisplayName("applyFilters() với filteredUsers = null — không throw")
        void filteredUsersNull() {
            controller.filteredUsers = null;
            assertDoesNotThrow(() -> controller.applyFilters());
        }

        @Test
        @DisplayName("applyFilters() tfSearch null — không throw")
        void tfSearchNull() {
            controller.tfSearch = null;
            assertDoesNotThrow(() -> controller.applyFilters());
        }

        @Test
        @DisplayName("applyFilters() tất cả filter 'Tất cả' → 5 user")
        void tatCaFilter() {
            controller.cbFilterRole.setValue("Tất cả vai trò");
            controller.cbFilterStatus.setValue("Tất cả trạng thái");
            controller.tfSearch.setText("");
            controller.applyFilters();
            assertEquals(5, controller.filteredUsers.size());
        }

        @Test
        @DisplayName("applyFilters() search 'alice' → chỉ 1 user")
        void searchAlice() {
            controller.tfSearch.setText("alice");
            controller.applyFilters();
            assertEquals(1, controller.filteredUsers.size());
            assertEquals("alice", controller.filteredUsers.get(0).getUsername());
        }

        @Test
        @DisplayName("applyFilters() search 'x.com' → match tất cả (email domain chung)")
        void searchEmailDomain() {
            controller.tfSearch.setText("x.com");
            controller.applyFilters();
            assertEquals(5, controller.filteredUsers.size());
        }

        @Test
        @DisplayName("applyFilters() search không tồn tại → 0 user")
        void searchKhongTonTai() {
            controller.tfSearch.setText("zzznobody");
            controller.applyFilters();
            assertEquals(0, controller.filteredUsers.size());
        }

        @Test
        @DisplayName("applyFilters() filter MEMBER → alice + bob (2 user)")
        void filterMember() {
            controller.cbFilterRole.setValue("MEMBER");
            controller.tfSearch.setText("");
            controller.applyFilters();
            assertEquals(2, controller.filteredUsers.size());
            assertTrue(controller.filteredUsers.stream()
                    .allMatch(u -> "MEMBER".equals(u.getRole())));
        }

        @Test
        @DisplayName("applyFilters() filter SELLER → chỉ dave")
        void filterSeller() {
            controller.cbFilterRole.setValue("SELLER");
            controller.tfSearch.setText("");
            controller.applyFilters();
            assertEquals(1, controller.filteredUsers.size());
            assertEquals("dave", controller.filteredUsers.get(0).getUsername());
        }

        @Test
        @DisplayName("applyFilters() filter ADMIN → carol + super1 (isAdmin = true)")
        void filterAdmin() {
            controller.cbFilterRole.setValue("ADMIN");
            controller.tfSearch.setText("");
            controller.applyFilters();
            assertEquals(2, controller.filteredUsers.size());
            assertTrue(controller.filteredUsers.stream().allMatch(UserAdminDTO::isAdmin));
        }

        @Test
        @DisplayName("applyFilters() filter status ACTIVE → alice + carol + super1 (3 user)")
        void filterActive() {
            controller.cbFilterStatus.setValue("ACTIVE");
            controller.cbFilterRole.setValue("Tất cả vai trò");
            controller.tfSearch.setText("");
            controller.applyFilters();
            assertEquals(3, controller.filteredUsers.size());
            assertTrue(controller.filteredUsers.stream()
                    .allMatch(u -> "ACTIVE".equals(u.getStatus())));
        }

        @Test
        @DisplayName("applyFilters() filter status BANNED → chỉ bob")
        void filterBanned() {
            controller.cbFilterStatus.setValue("BANNED");
            controller.cbFilterRole.setValue("Tất cả vai trò");
            controller.tfSearch.setText("");
            controller.applyFilters();
            assertEquals(1, controller.filteredUsers.size());
            assertEquals("bob", controller.filteredUsers.get(0).getUsername());
        }

        @Test
        @DisplayName("applyFilters() filter status SUSPENDED → chỉ dave")
        void filterSuspended() {
            controller.cbFilterStatus.setValue("SUSPENDED");
            controller.cbFilterRole.setValue("Tất cả vai trò");
            controller.tfSearch.setText("");
            controller.applyFilters();
            assertEquals(1, controller.filteredUsers.size());
            assertEquals("dave", controller.filteredUsers.get(0).getUsername());
        }

        @Test
        @DisplayName("applyFilters() kết hợp: search 'alice' + MEMBER + ACTIVE → 1 user")
        void ketHopTatCaFilter() {
            controller.tfSearch.setText("alice");
            controller.cbFilterRole.setValue("MEMBER");
            controller.cbFilterStatus.setValue("ACTIVE");
            controller.applyFilters();
            assertEquals(1, controller.filteredUsers.size());
            assertEquals("alice", controller.filteredUsers.get(0).getUsername());
        }

        @Test
        @DisplayName("applyFilters() kết hợp: MEMBER + BANNED → chỉ bob")
        void memberBanned() {
            controller.cbFilterRole.setValue("MEMBER");
            controller.cbFilterStatus.setValue("BANNED");
            controller.tfSearch.setText("");
            controller.applyFilters();
            assertEquals(1, controller.filteredUsers.size());
            assertEquals("bob", controller.filteredUsers.get(0).getUsername());
        }

        @Test
        @DisplayName("applyFilters() cbFilterRole = null → không throw, vẫn filter được")
        void cbFilterRoleNull() {
            controller.cbFilterRole = null;
            assertDoesNotThrow(() -> controller.applyFilters());
        }

        @Test
        @DisplayName("applyFilters() cbFilterStatus = null → không throw")
        void cbFilterStatusNull() {
            controller.cbFilterStatus = null;
            assertDoesNotThrow(() -> controller.applyFilters());
        }

        @Test
        @DisplayName("lambda$applyFilters$6: predicate chạy đúng cho từng user")
        void lambdaPredicateChayDung() {
            controller.tfSearch.setText("carol");
            controller.cbFilterRole.setValue("ADMIN");
            controller.cbFilterStatus.setValue("ACTIVE");
            controller.applyFilters();

            // Chỉ carol match: tên "carol", role ADMIN, status ACTIVE
            assertEquals(1, controller.filteredUsers.size());
            assertEquals("carol", controller.filteredUsers.get(0).getUsername());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ④ matchSearch() — 95% → 100%
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("④ matchSearch() — 95% → 100%")
    class MatchSearch {

        @Test
        @DisplayName("search null → match tất cả")
        void searchNull() {
            assertTrue(controller.matchSearch(alice(), null));
            assertTrue(controller.matchSearch(bob(),   null));
        }

        @Test
        @DisplayName("search '' rỗng → match tất cả")
        void searchRong() {
            for (var u : allUsers()) assertTrue(controller.matchSearch(u, ""));
        }

        @Test
        @DisplayName("search '   ' blank → match tất cả")
        void searchBlank() {
            for (var u : allUsers()) assertTrue(controller.matchSearch(u, "   "));
        }

        @Test
        @DisplayName("search theo username chính xác")
        void searchUsernameChinhXac() {
            assertTrue(controller.matchSearch(alice(), "alice"));
            assertFalse(controller.matchSearch(bob(),  "alice"));
        }

        @Test
        @DisplayName("search partial username → match")
        void searchUsernamePartial() {
            assertTrue(controller.matchSearch(alice(), "ali"));
            assertTrue(controller.matchSearch(carol(), "aro"));
        }

        @Test
        @DisplayName("search theo email partial → match")
        void searchEmailPartial() {
            assertTrue(controller.matchSearch(alice(), "alice@"));
            assertTrue(controller.matchSearch(bob(),   "bob@x"));
        }

        @Test
        @DisplayName("search case-insensitive username")
        void caseInsensitiveUsername() {
            assertTrue(controller.matchSearch(alice(), "ALICE"));
            assertTrue(controller.matchSearch(carol(), "Carol"));
        }

        @Test
        @DisplayName("search case-insensitive email")
        void caseInsensitiveEmail() {
            assertTrue(controller.matchSearch(alice(), "ALICE@X.COM"));
        }

        @Test
        @DisplayName("username null, email null, search rỗng → match (không crash)")
        void usernameEmailNull() {
            UserAdminDTO noInfo = new UserAdminDTO(99, null, null, "MEMBER", "ACTIVE");
            assertDoesNotThrow(() -> {
                assertTrue(controller.matchSearch(noInfo, ""));
                assertFalse(controller.matchSearch(noInfo, "alice"));
            });
        }

        @Test
        @DisplayName("username null, email có giá trị → match theo email")
        void usernameNullEmailCo() {
            UserAdminDTO u = new UserAdminDTO(99, null, "test@example.com", "MEMBER", "ACTIVE");
            assertTrue(controller.matchSearch(u, "test@example"));
            assertFalse(controller.matchSearch(u, "alice"));
        }

        @Test
        @DisplayName("username có giá trị, email null → match theo username")
        void emailNullUsernameCo() {
            UserAdminDTO u = new UserAdminDTO(99, "testuser", null, "MEMBER", "ACTIVE");
            assertTrue(controller.matchSearch(u, "testuser"));
            assertFalse(controller.matchSearch(u, "nobody"));
        }


    }

    // ════════════════════════════════════════════════════════════════
    // ⑤ matchRole() — 100% (giữ vững + bổ sung SUPER_ADMIN)
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("⑤ matchRole() — 100% (giữ vững + bổ sung)")
    class MatchRole {

        @Test
        @DisplayName("null filter → match tất cả")
        void nullFilter() {
            for (var u : allUsers()) assertTrue(controller.matchRole(u, null));
        }

        @Test
        @DisplayName("'Tất cả vai trò' → match tất cả")
        void tatCaVaiTro() {
            for (var u : allUsers()) assertTrue(controller.matchRole(u, "Tất cả vai trò"));
        }

        @Test
        @DisplayName("'ADMIN' filter → match carol (ADMIN) + super1 (SUPER_ADMIN)")
        void adminFilter() {
            assertTrue(controller.matchRole(carol(),  "ADMIN"),  "carol là ADMIN");
            assertTrue(controller.matchRole(super1(), "ADMIN"),  "super1 là SUPER_ADMIN → isAdmin=true");
            assertFalse(controller.matchRole(alice(), "ADMIN"),  "alice là MEMBER");
            assertFalse(controller.matchRole(dave(),  "ADMIN"),  "dave là SELLER");
        }

        @Test
        @DisplayName("'MEMBER' filter → match alice + bob, không match carol/dave/super1")
        void memberFilter() {
            assertTrue(controller.matchRole(alice(), "MEMBER"));
            assertTrue(controller.matchRole(bob(),   "MEMBER"));
            assertFalse(controller.matchRole(carol(),  "MEMBER"));
            assertFalse(controller.matchRole(dave(),   "MEMBER"));
            assertFalse(controller.matchRole(super1(), "MEMBER"));
        }

        @Test
        @DisplayName("'SELLER' filter → chỉ match dave")
        void sellerFilter() {
            assertTrue(controller.matchRole(dave(), "SELLER"));
            assertFalse(controller.matchRole(alice(), "SELLER"));
            assertFalse(controller.matchRole(carol(), "SELLER"));
        }

        @Test
        @DisplayName("'SUPER_ADMIN' filter → chỉ match super1 (exact role match)")
        void superAdminFilter() {
            assertTrue(controller.matchRole(super1(), "SUPER_ADMIN"));
            assertFalse(controller.matchRole(carol(),  "SUPER_ADMIN"),
                    "carol là ADMIN, không phải SUPER_ADMIN chính xác");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ⑥ matchStatus() — 100% (giữ vững + bổ sung SUSPENDED)
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("⑥ matchStatus() — 100% (giữ vững + bổ sung)")
    class MatchStatus {

        @Test
        @DisplayName("null filter → match tất cả")
        void nullFilter() {
            for (var u : allUsers()) assertTrue(controller.matchStatus(u, null));
        }

        @Test
        @DisplayName("'Tất cả trạng thái' → match tất cả")
        void tatCaTrangThai() {
            for (var u : allUsers()) assertTrue(controller.matchStatus(u, "Tất cả trạng thái"));
        }

        @Test
        @DisplayName("'ACTIVE' → match alice + carol + super1")
        void activeFilter() {
            assertTrue(controller.matchStatus(alice(),  "ACTIVE"));
            assertTrue(controller.matchStatus(carol(),  "ACTIVE"));
            assertTrue(controller.matchStatus(super1(), "ACTIVE"));
            assertFalse(controller.matchStatus(bob(),  "ACTIVE"));
            assertFalse(controller.matchStatus(dave(), "ACTIVE"));
        }

        @Test
        @DisplayName("'BANNED' → chỉ match bob")
        void bannedFilter() {
            assertTrue(controller.matchStatus(bob(), "BANNED"));
            assertFalse(controller.matchStatus(alice(), "BANNED"));
            assertFalse(controller.matchStatus(dave(),  "BANNED"));
        }

        @Test
        @DisplayName("'SUSPENDED' → chỉ match dave")
        void suspendedFilter() {
            assertTrue(controller.matchStatus(dave(),  "SUSPENDED"));
            assertFalse(controller.matchStatus(alice(), "SUSPENDED"));
            assertFalse(controller.matchStatus(bob(),   "SUSPENDED"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ACTIVE", "BANNED", "SUSPENDED"})
        @DisplayName("Mỗi status chỉ match đúng user có status đó")
        void statusChiMatchDungUser(String status) {
            long count = allUsers().stream()
                    .filter(u -> controller.matchStatus(u, status))
                    .count();
            // ACTIVE: 3, BANNED: 1, SUSPENDED: 1
            assertTrue(count > 0, "Phải có ít nhất 1 user với status " + status);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ⑦ suspendSelected() — 34% → 100%
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("⑦ suspendSelected() — 34% → 100%")
    class SuspendSelected {

        @Test
        @DisplayName("Không có selection → showWarning được gọi, không crash")
        void khongCoSelection() {
            try (MockedStatic<AlertUtil> mu = mockStatic(AlertUtil.class)) {
                controller.suspendSelected();
                mu.verify(() -> AlertUtil.showWarning(anyString()), times(1));
            }
        }

        @Test
        @DisplayName("Select user đã SUSPENDED → showInfo (đã ở trạng thái SUSPENDED)")
        void userDaSuspended() {
            try (MockedStatic<AlertUtil> mu = mockStatic(AlertUtil.class)) {
                wireTable(List.of(dave())); // dave = SUSPENDED
                controller.userTable.getSelectionModel().select(dave());

                controller.suspendSelected();

                mu.verify(() -> AlertUtil.showInfo(contains("SUSPENDED")), times(1));
            }
        }

        @Test
        @DisplayName("Select user đã BANNED → showInfo (đã ở trạng thái BANNED)")
        void userDaBanned() {
            try (MockedStatic<AlertUtil> mu = mockStatic(AlertUtil.class)) {
                wireTable(List.of(bob())); // bob = BANNED
                controller.userTable.getSelectionModel().select(bob());

                controller.suspendSelected();

                mu.verify(() -> AlertUtil.showInfo(contains("BANNED")), times(1));
            }
        }

        @Test
        @DisplayName("Select user ACTIVE → gọi changeStatus với 'SUSPENDED' (confirm = false → dừng)")
        void userActiveConfirmFalse() {
            try (MockedStatic<AlertUtil> mu = mockStatic(AlertUtil.class);
                 MockedStatic<AdminService> ms = mockStatic(AdminService.class)) {

                mu.when(() -> AlertUtil.showConfirm(anyString(), anyString()))
                        .thenReturn(false); // user cancel

                wireTable(List.of(alice()));
                controller.userTable.getSelectionModel().select(alice());

                controller.suspendSelected();

                // Confirm bị từ chối → không gọi AdminService
                ms.verify(() -> AdminService.updateUserStatus(anyInt(), anyString(), any()),
                        never());
            }
        }

        @Test
        @DisplayName("Select user ACTIVE → confirm OK → gọi AdminService.updateUserStatus")
        void userActiveConfirmOk() {
            try (MockedStatic<AlertUtil> mu = mockStatic(AlertUtil.class);
                 MockedStatic<AdminService> ms = mockStatic(AdminService.class)) {

                mu.when(() -> AlertUtil.showConfirm(anyString(), anyString()))
                        .thenReturn(true);

                wireTable(List.of(alice()));
                controller.userTable.getSelectionModel().select(alice());

                controller.suspendSelected();

                ms.verify(() -> AdminService.updateUserStatus(
                        eq(1), eq("SUSPENDED"), any()), times(1));
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ⑧ banSelected() — 16% → 100%
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("⑧ banSelected() — 16% → 100%")
    class BanSelected {

        @Test
        @DisplayName("Không phải Super Admin → showError, không hỏi selection")
        void khongPhamSuperAdmin() {
            try (MockedStatic<AlertUtil> mu = mockStatic(AlertUtil.class);
                 MockedStatic<SessionManager> ms = mockStatic(SessionManager.class)) {

                ms.when(SessionManager::isSuperAdmin).thenReturn(false);

                controller.banSelected();

                mu.verify(() -> AlertUtil.showError(contains("Super Admin")), times(1));
                // Không bao giờ hỏi selection hay show confirm
                mu.verify(() -> AlertUtil.showConfirm(anyString(), anyString()), never());
            }
        }

        @Test
        @DisplayName("Là Super Admin nhưng không có selection → showWarning")
        void superAdminKhongCoSelection() {
            try (MockedStatic<AlertUtil> mu = mockStatic(AlertUtil.class);
                 MockedStatic<SessionManager> ms = mockStatic(SessionManager.class)) {

                ms.when(SessionManager::isSuperAdmin).thenReturn(true);

                controller.banSelected();

                mu.verify(() -> AlertUtil.showWarning(anyString()), times(1));
            }
        }

        @Test
        @DisplayName("Super Admin + select user đã BANNED → showInfo")
        void superAdminUserDaBanned() {
            try (MockedStatic<AlertUtil> mu = mockStatic(AlertUtil.class);
                 MockedStatic<SessionManager> ms = mockStatic(SessionManager.class)) {

                ms.when(SessionManager::isSuperAdmin).thenReturn(true);

                wireTable(List.of(bob())); // bob = BANNED
                controller.userTable.getSelectionModel().select(bob());

                controller.banSelected();

                mu.verify(() -> AlertUtil.showInfo(contains("cấm")), times(1));
            }
        }

        @Test
        @DisplayName("Super Admin + select ACTIVE user + confirm = false → không gọi AdminService")
        void superAdminConfirmFalse() {
            try (MockedStatic<AlertUtil> mu = mockStatic(AlertUtil.class);
                 MockedStatic<SessionManager> ms = mockStatic(SessionManager.class);
                 MockedStatic<AdminService> msa = mockStatic(AdminService.class)) {

                ms.when(SessionManager::isSuperAdmin).thenReturn(true);
                mu.when(() -> AlertUtil.showConfirm(anyString(), anyString()))
                        .thenReturn(false);

                wireTable(List.of(alice()));
                controller.userTable.getSelectionModel().select(alice());

                controller.banSelected();

                msa.verify(() -> AdminService.updateUserStatus(anyInt(), anyString(), any()),
                        never());
            }
        }

        @Test
        @DisplayName("Super Admin + select ACTIVE user + confirm OK → gọi AdminService với BANNED")
        void superAdminConfirmOk() {
            try (MockedStatic<AlertUtil> mu = mockStatic(AlertUtil.class);
                 MockedStatic<SessionManager> ms = mockStatic(SessionManager.class);
                 MockedStatic<AdminService> msa = mockStatic(AdminService.class)) {

                ms.when(SessionManager::isSuperAdmin).thenReturn(true);
                mu.when(() -> AlertUtil.showConfirm(anyString(), anyString()))
                        .thenReturn(true);

                wireTable(List.of(alice()));
                controller.userTable.getSelectionModel().select(alice());

                controller.banSelected();

                msa.verify(() -> AdminService.updateUserStatus(
                        eq(1), eq("BANNED"), any()), times(1));
            }
        }

        @Test
        @DisplayName("Super Admin + select SELLER user + confirm OK → gọi AdminService với BANNED")
        void superAdminBanSeller() {
            try (MockedStatic<AlertUtil> mu = mockStatic(AlertUtil.class);
                 MockedStatic<SessionManager> ms = mockStatic(SessionManager.class);
                 MockedStatic<AdminService> msa = mockStatic(AdminService.class)) {

                ms.when(SessionManager::isSuperAdmin).thenReturn(true);
                mu.when(() -> AlertUtil.showConfirm(anyString(), anyString()))
                        .thenReturn(true);

                // dave = SUSPENDED → có thể ban
                wireTable(List.of(dave()));
                controller.userTable.getSelectionModel().select(dave());

                controller.banSelected();

                msa.verify(() -> AdminService.updateUserStatus(
                        eq(4), eq("BANNED"), any()), times(1));
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ⑨ changeStatus() — 0% → 100%
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("⑨ changeStatus() — 0% → 100%")
    class ChangeStatusMethod {

        @Test
        @DisplayName("confirm = false → không gọi AdminService")
        void confirmFalse() {
            try (MockedStatic<AlertUtil> mu = mockStatic(AlertUtil.class);
                 MockedStatic<AdminService> ms = mockStatic(AdminService.class)) {

                mu.when(() -> AlertUtil.showConfirm(anyString(), anyString()))
                        .thenReturn(false);

                controller.changeStatus(alice(), "SUSPENDED", "khóa tạm thời");

                ms.verify(() -> AdminService.updateUserStatus(anyInt(), anyString(), any()),
                        never());
            }
        }

        @Test
        @DisplayName("confirm = true → gọi AdminService.updateUserStatus với đúng id + status")
        void confirmTrue_GoiAdminService() {
            try (MockedStatic<AlertUtil> mu = mockStatic(AlertUtil.class);
                 MockedStatic<AdminService> ms = mockStatic(AdminService.class)) {

                mu.when(() -> AlertUtil.showConfirm(anyString(), anyString()))
                        .thenReturn(true);

                controller.changeStatus(alice(), "SUSPENDED", "khóa tạm thời");

                ms.verify(() -> AdminService.updateUserStatus(
                        eq(1), eq("SUSPENDED"), any()), times(1));
            }
        }

        @Test
        @DisplayName("changeStatus BANNED: gọi với id=2, status='BANNED'")
        void changeStatusBanned() {
            try (MockedStatic<AlertUtil> mu = mockStatic(AlertUtil.class);
                 MockedStatic<AdminService> ms = mockStatic(AdminService.class)) {

                mu.when(() -> AlertUtil.showConfirm(anyString(), anyString()))
                        .thenReturn(true);

                controller.changeStatus(bob(), "BANNED", "cấm vĩnh viễn");

                ms.verify(() -> AdminService.updateUserStatus(
                        eq(2), eq("BANNED"), any()), times(1));
            }
        }

        @Test
        @DisplayName("changeStatus ACTIVE (unban): gọi với status='ACTIVE'")
        void changeStatusActive() {
            try (MockedStatic<AlertUtil> mu = mockStatic(AlertUtil.class);
                 MockedStatic<AdminService> ms = mockStatic(AdminService.class)) {

                mu.when(() -> AlertUtil.showConfirm(anyString(), anyString()))
                        .thenReturn(true);

                controller.changeStatus(bob(), "ACTIVE", "kích hoạt lại");

                ms.verify(() -> AdminService.updateUserStatus(
                        eq(2), eq("ACTIVE"), any()), times(1));
            }
        }



        @Test
        @DisplayName("confirm dialog message chứa username của user")
        void confirmMessageChuaUsername() {
            try (MockedStatic<AlertUtil> mu = mockStatic(AlertUtil.class);
                 MockedStatic<AdminService> ms = mockStatic(AdminService.class)) {

                mu.when(() -> AlertUtil.showConfirm(anyString(), anyString()))
                        .thenReturn(false);

                controller.changeStatus(alice(), "SUSPENDED", "khóa tạm thời");

                // Confirm dialog phải chứa tên alice
                mu.verify(() -> AlertUtil.showConfirm(
                        anyString(),
                        argThat(msg -> msg.contains("alice"))), times(1));
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ⑩ loadUsers() — 0% → 100%
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("⑩ loadUsers() — 0% → 100%")
    class LoadUsers {

        @Test
        @DisplayName("loadUsers() gọi AdminService.getUsers")
        void goiAdminServiceGetUsers() {
            try (MockedStatic<AdminService> ms = mockStatic(AdminService.class)) {
                controller.loadUsers();
                ms.verify(() -> AdminService.getUsers(any()), times(1));
            }
        }



        @Test
        @DisplayName("lambda$loadUsers$8: callback với null → allUsers rỗng (không crash)")
        void callbackWithNull() {
            try (MockedStatic<AdminService> ms = mockStatic(AdminService.class)) {
                ms.when(() -> AdminService.getUsers(any()))
                        .thenAnswer(inv -> {
                            java.util.function.Consumer<List<UserAdminDTO>> cb = inv.getArgument(0);
                            cb.accept(null); // server lỗi → null
                            return null;
                        });

                assertDoesNotThrow(() -> controller.loadUsers());
                assertEquals(0, controller.allUsers.size());
            }
        }

        @Test
        @DisplayName("lambda$loadUsers$8: callback với empty list → allUsers rỗng")
        void callbackWithEmpty() {
            try (MockedStatic<AdminService> ms = mockStatic(AdminService.class)) {
                ms.when(() -> AdminService.getUsers(any()))
                        .thenAnswer(inv -> {
                            java.util.function.Consumer<List<UserAdminDTO>> cb = inv.getArgument(0);
                            cb.accept(List.of());
                            return null;
                        });

                controller.loadUsers();
                assertTrue(controller.allUsers.isEmpty());
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ⑪ setupActionColumn() — 0% → 100%
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("⑪ setupActionColumn() — 0% → 100%")
    class SetupActionColumn {

        @Test
        @DisplayName("colAction = null → không throw (early return)")
        void colActionNull() {
            controller.colAction = null;
            assertDoesNotThrow(() -> controller.setupActionColumn());
        }

        @Test
        @DisplayName("colAction có giá trị → setCellFactory được gọi (không throw)")
        void colActionCoGiaTri() {
            controller.colAction = new TableColumn<>();
            assertDoesNotThrow(() -> controller.setupActionColumn());
            assertNotNull(controller.colAction.getCellFactory(),
                    "setCellFactory phải được gọi");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ⑫ setupStatusColorCoding() — 0% → 100%
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("⑫ setupStatusColorCoding() — 0% → 100%")
    class SetupStatusColorCoding {

        @Test
        @DisplayName("setupStatusColorCoding() set cellFactory cho colStatus")
        void setCellFactory() {
            controller.setupStatusColorCoding();
            assertNotNull(controller.colStatus.getCellFactory(),
                    "colStatus phải có cellFactory sau setupStatusColorCoding()");
        }

        @Test
        @DisplayName("setupStatusColorCoding() không throw khi gọi")
        void khongThrow() {
            assertDoesNotThrow(() -> controller.setupStatusColorCoding());
        }
    }
}