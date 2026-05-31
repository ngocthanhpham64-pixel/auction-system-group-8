package vn.edu.vnu.uet.group8.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.vnu.uet.group8.client.service.AdminService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.common.dto.model.UserAdminDTO;
import vn.edu.vnu.uet.group8.common.dto.model.UserProfileDTO;
import javafx.scene.control.IndexedCell;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test bao phủ 100% instruction và branch của AdminUserListController.
 *
 * ══ NGUYÊN NHÂN GỐC RỄ CỦA CÁC LỖI CŨ ══
 *
 * 1. getTableView() trả về null:
 *    TableCell cần được attach vào TableView TRƯỚC khi updateItem() được gọi.
 *    Cách fix: gọi updateTableView() + updateTableColumn() + updateIndex() TRƯỚC updateItem().
 *    Nhưng vì TableCell.updateItem() là protected và class là anonymous,
 *    cần dùng superclass Method thay vì class cụ thể.
 *
 * 2. NoSuchMethod TableCell.updateItem(Object, boolean):
 *    TableCell anonymous override updateItem(Void, boolean) nhưng reflection tìm
 *    trên superclass cần đúng signature. Dùng getDeclaredMethod trên superclass
 *    TableCell với (Object.class, boolean.class) là public API.
 *    Fix: Dùng table.setItems(), table.layout(), sau đó lấy cell từ skin.
 *
 * 3. MockedStatic trên sai thread → mọi mock mở TRÊN FX THREAD bên trong runFx().
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserListController – 100% instruction & branch coverage")
class AdminUserListControllerTest {

    // ── JavaFX Toolkit init (once per JVM) ──────────────────────────────────
    @BeforeAll
    static void initJfx() {
        new JFXPanel();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static void runFx(RunnableEx r) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                r.run();
            } catch (Throwable t) {
                thrown.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "FX task timed out");
        if (thrown.get() != null) {
            Throwable t = thrown.get();
            if (t instanceof Exception ex) throw ex;
            throw new RuntimeException(t);
        }
    }

    @FunctionalInterface
    interface RunnableEx { void run() throws Exception; }

    private AdminUserListController buildController() throws Exception {
        AdminUserListController ctrl = new AdminUserListController();

        setField(ctrl, "tfSearch",       new TextField());
        setField(ctrl, "cbFilterRole",   new ComboBox<>());
        setField(ctrl, "cbFilterStatus", new ComboBox<>());

        TableView<UserAdminDTO> table = new TableView<>();
        TableColumn<UserAdminDTO, Integer> colId       = new TableColumn<>("ID");
        TableColumn<UserAdminDTO, String>  colUsername = new TableColumn<>("Username");
        TableColumn<UserAdminDTO, String>  colEmail    = new TableColumn<>("Email");
        TableColumn<UserAdminDTO, String>  colRole     = new TableColumn<>("Role");
        TableColumn<UserAdminDTO, String>  colStatus   = new TableColumn<>("Status");
        TableColumn<UserAdminDTO, Void>    colAction   = new TableColumn<>("Action");

        table.getColumns().addAll(colId, colUsername, colEmail, colRole, colStatus, colAction);

        setField(ctrl, "userTable",   table);
        setField(ctrl, "colId",       colId);
        setField(ctrl, "colUsername", colUsername);
        setField(ctrl, "colEmail",    colEmail);
        setField(ctrl, "colRole",     colRole);
        setField(ctrl, "colStatus",   colStatus);
        setField(ctrl, "colAction",   colAction);

        return ctrl;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = findField(target.getClass(), name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        try {
            return cls.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            if (cls.getSuperclass() != null) return findField(cls.getSuperclass(), name);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String name) throws Exception {
        Field f = findField(target.getClass(), name);
        f.setAccessible(true);
        return (T) f.get(target);
    }

    // ── Sample DTOs ─────────────────────────────────────────────────────────

    private static UserAdminDTO activeUser() {
        return new UserAdminDTO(1, "alice", "alice@example.com",
                UserAdminDTO.ROLE_MEMBER, UserAdminDTO.STATUS_ACTIVE);
    }

    private static UserAdminDTO suspendedUser() {
        return new UserAdminDTO(2, "bob", "bob@example.com",
                UserAdminDTO.ROLE_SELLER, UserAdminDTO.STATUS_SUSPENDED);
    }

    private static UserAdminDTO bannedUser() {
        return new UserAdminDTO(3, "carol", "carol@example.com",
                UserAdminDTO.ROLE_ADMIN, UserAdminDTO.STATUS_BANNED);
    }

    private static UserProfileDTO buildProfile(String username, int userId,
                                               String email,
                                               java.math.BigDecimal rating,
                                               String displayRole,
                                               java.time.Instant lastLogin,
                                               int totalBids,
                                               int totalSold) throws Exception {
        java.lang.reflect.Constructor<UserProfileDTO> ctor =
                UserProfileDTO.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        UserProfileDTO dto = ctor.newInstance();
        setField(dto, "username",       username);
        setField(dto, "userId",         userId);
        setField(dto, "email",          email);
        setField(dto, "sellerRating",   rating);
        setField(dto, "displayRole",    displayRole);
        setField(dto, "lastLogin",      lastLogin);
        setField(dto, "totalBidsPlaced", totalBids);
        setField(dto, "totalItemsSold", totalSold);
        return dto;
    }

    // ── Cell factory helpers ─────────────────────────────────────────────────

    /**
     * Tạo status cell và gọi updateItem thông qua protected method.
     * Dùng Method trên anonymous subclass (getClass()) để tránh NoSuchMethod.
     */
    @SuppressWarnings("unchecked")
    private TableCell<UserAdminDTO, String> makeStatusCell(AdminUserListController ctrl)
            throws Exception {
        TableColumn<UserAdminDTO, String> colStatus = getField(ctrl, "colStatus");
        return colStatus.getCellFactory().call(colStatus);
    }

    private void callStatusUpdateItem(TableCell<UserAdminDTO, String> cell,
                                      String item, boolean empty) throws Exception {
        // updateItem là protected – truy cập qua declared method của chính class ẩn danh
        Method m = cell.getClass().getDeclaredMethod("updateItem", String.class, boolean.class);
        m.setAccessible(true);
        m.invoke(cell, item, empty);
    }

    /**
     * Tạo action cell và attach vào TableView TRƯỚC khi gọi updateItem.
     *
     * ── FIX NPE getTableView() null ──
     * TableCell cần biết TableView của nó thông qua updateTableView().
     * Sau đó updateTableColumn() để bind column, rồi updateIndex() để set index.
     * Chỉ SAU các bước này thì getTableView().getItems() mới hoạt động.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private TableCell<UserAdminDTO, Void> makeActionCell(
            AdminUserListController ctrl,
            ObservableList<UserAdminDTO> items,
            int index) throws Exception {

        TableView<UserAdminDTO> table = getField(ctrl, "userTable");
        table.setItems(items);

        TableColumn<UserAdminDTO, Void> colAction = getField(ctrl, "colAction");
        TableCell<UserAdminDTO, Void> cell = colAction.getCellFactory().call(colAction);

        // Bước 1: Attach TableView vào cell
        Method updateTableView = findSuperMethod(cell.getClass(), "updateTableView", TableView.class);
        updateTableView.setAccessible(true);
        updateTableView.invoke(cell, table);

        // Bước 2: Attach TableColumn vào cell
        Method updateTableColumn = findSuperMethod(cell.getClass(), "updateTableColumn", TableColumn.class);
        updateTableColumn.setAccessible(true);
        updateTableColumn.invoke(cell, colAction);

        // Bước 3: Set index
        Method updateIndex = findSuperMethod(cell.getClass(), "updateIndex", int.class);
        updateIndex.setAccessible(true);
        updateIndex.invoke(cell, index);

        // Bước 4: Gọi updateItem – dùng declared method trên anonymous class
        Method updateItem = cell.getClass().getDeclaredMethod("updateItem", Void.class, boolean.class);
        updateItem.setAccessible(true);
        updateItem.invoke(cell, null, false);

        return cell;
    }

    /** Tìm method trên class hoặc superclass (hỗ trợ class ẩn danh). */
    private static Method findSuperMethod(Class<?> cls, String name, Class<?>... params) {
        try {
            Method m = cls.getDeclaredMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            if (cls.getSuperclass() != null) return findSuperMethod(cls.getSuperclass(), name, params);
            throw new RuntimeException("Method not found: " + name, e);
        }
    }

    // =========================================================================
    // initialize()
    // =========================================================================
    @Nested
    @DisplayName("initialize()")
    class Initialize {

        @Test
        @DisplayName("Khởi tạo đầy đủ ComboBox, cột bảng, listener, và gọi loadUsers()")
        void initializeFullFlow() throws Exception {
            AtomicReference<AdminUserListController> ctrlRef = new AtomicReference<>();
            AtomicBoolean getUsersCalled = new AtomicBoolean(false);

            runFx(() -> {
                try (MockedStatic<AdminService> svc = mockStatic(AdminService.class)) {
                    svc.when(() -> AdminService.getUsers(any()))
                            .thenAnswer(inv -> {
                                getUsersCalled.set(true);
                                Consumer<List<UserAdminDTO>> cb = inv.getArgument(0);
                                cb.accept(List.of(activeUser()));
                                return null;
                            });

                    AdminUserListController ctrl = buildController();
                    ctrl.initialize();
                    ctrlRef.set(ctrl);
                }
            });

            assertTrue(getUsersCalled.get(), "AdminService.getUsers() phải được gọi");

            AdminUserListController ctrl = ctrlRef.get();
            ComboBox<String> cbRole   = getField(ctrl, "cbFilterRole");
            ComboBox<String> cbStatus = getField(ctrl, "cbFilterStatus");

            assertTrue(cbRole.getItems().contains("Tất cả"));
            assertTrue(cbRole.getItems().contains(UserAdminDTO.ROLE_MEMBER));
            assertTrue(cbRole.getItems().contains(UserAdminDTO.ROLE_SELLER));
            assertTrue(cbRole.getItems().contains(UserAdminDTO.ROLE_ADMIN));
            assertTrue(cbRole.getItems().contains(UserAdminDTO.ROLE_SUPER_ADMIN));
            assertEquals("Tất cả", cbRole.getValue());

            assertTrue(cbStatus.getItems().contains("Tất cả"));
            assertTrue(cbStatus.getItems().contains(UserAdminDTO.STATUS_ACTIVE));
            assertTrue(cbStatus.getItems().contains(UserAdminDTO.STATUS_SUSPENDED));
            assertTrue(cbStatus.getItems().contains(UserAdminDTO.STATUS_BANNED));
            assertEquals("Tất cả", cbStatus.getValue());
        }
    }

    // =========================================================================
    // loadUsers()
    // =========================================================================
    @Nested
    @DisplayName("loadUsers()")
    class LoadUsers {

        @Test
        @DisplayName("Server trả về list không null → users được set")
        void serverReturnsNonNullList() throws Exception {
            AtomicReference<Integer> size = new AtomicReference<>();
            runFx(() -> {
                try (MockedStatic<AdminService> svc = mockStatic(AdminService.class)) {
                    svc.when(() -> AdminService.getUsers(any()))
                            .thenAnswer(inv -> {
                                Consumer<List<UserAdminDTO>> cb = inv.getArgument(0);
                                cb.accept(List.of(activeUser(), suspendedUser()));
                                return null;
                            });
                    AdminUserListController ctrl = buildController();
                    ctrl.initialize();
                    ObservableList<UserAdminDTO> users = getField(ctrl, "users");
                    size.set(users.size());
                }
            });
            assertEquals(2, size.get());
        }

        @Test
        @DisplayName("Server trả về null → users được set thành List.of()")
        void serverReturnsNull() throws Exception {
            AtomicReference<Integer> size = new AtomicReference<>();
            runFx(() -> {
                try (MockedStatic<AdminService> svc = mockStatic(AdminService.class)) {
                    svc.when(() -> AdminService.getUsers(any()))
                            .thenAnswer(inv -> {
                                Consumer<List<UserAdminDTO>> cb = inv.getArgument(0);
                                cb.accept(null);
                                return null;
                            });
                    AdminUserListController ctrl = buildController();
                    ctrl.initialize();
                    ObservableList<UserAdminDTO> users = getField(ctrl, "users");
                    size.set(users.size());
                }
            });
            assertEquals(0, size.get());
        }
    }

    // =========================================================================
    // applyFilters()
    // =========================================================================
    @Nested
    @DisplayName("applyFilters() – predicate branches")
    class ApplyFilters {

        private AdminUserListController prepareWithUsers(List<UserAdminDTO> userList)
                throws Exception {
            AtomicReference<AdminUserListController> ref = new AtomicReference<>();
            runFx(() -> {
                try (MockedStatic<AdminService> svc = mockStatic(AdminService.class)) {
                    svc.when(() -> AdminService.getUsers(any()))
                            .thenAnswer(inv -> {
                                Consumer<List<UserAdminDTO>> cb = inv.getArgument(0);
                                cb.accept(userList);
                                return null;
                            });
                    AdminUserListController ctrl = buildController();
                    ctrl.initialize();
                    ref.set(ctrl);
                }
            });
            return ref.get();
        }

        @Test
        @DisplayName("Tìm kiếm rỗng, role/status = Tất cả → trả về tất cả user")
        void noFilter_allUsersShown() throws Exception {
            AdminUserListController ctrl = prepareWithUsers(
                    List.of(activeUser(), suspendedUser(), bannedUser()));

            AtomicReference<Integer> size = new AtomicReference<>();
            runFx(() -> {
                TableView<UserAdminDTO> table = getField(ctrl, "userTable");
                size.set(table.getItems().size());
            });
            assertEquals(3, size.get());
        }

        @Test
        @DisplayName("Từ khóa tìm theo username (chứa) → chỉ trả về user khớp")
        void searchByUsername_match() throws Exception {
            AdminUserListController ctrl = prepareWithUsers(
                    List.of(activeUser(), suspendedUser(), bannedUser()));

            AtomicReference<Integer> size  = new AtomicReference<>();
            AtomicReference<String>  found = new AtomicReference<>();
            runFx(() -> {
                TextField tf = getField(ctrl, "tfSearch");
                tf.setText("ali");
                TableView<UserAdminDTO> table = getField(ctrl, "userTable");
                size.set(table.getItems().size());
                if (!table.getItems().isEmpty())
                    found.set(table.getItems().get(0).getUsername());
            });
            assertEquals(1, size.get());
            assertEquals("alice", found.get());
        }

        @Test
        @DisplayName("Từ khóa tìm theo email (chứa) → chỉ trả về user khớp")
        void searchByEmail_match() throws Exception {
            AdminUserListController ctrl = prepareWithUsers(
                    List.of(activeUser(), suspendedUser(), bannedUser()));

            AtomicReference<Integer> size  = new AtomicReference<>();
            AtomicReference<String>  found = new AtomicReference<>();
            runFx(() -> {
                TextField tf = getField(ctrl, "tfSearch");
                tf.setText("carol@");
                TableView<UserAdminDTO> table = getField(ctrl, "userTable");
                size.set(table.getItems().size());
                if (!table.getItems().isEmpty())
                    found.set(table.getItems().get(0).getUsername());
            });
            assertEquals(1, size.get());
            assertEquals("carol", found.get());
        }

        @Test
        @DisplayName("Từ khóa không khớp → bảng trống")
        void searchNoMatch() throws Exception {
            AdminUserListController ctrl = prepareWithUsers(
                    List.of(activeUser(), suspendedUser()));

            AtomicReference<Integer> size = new AtomicReference<>();
            runFx(() -> {
                TextField tf = getField(ctrl, "tfSearch");
                tf.setText("xyz_not_found");
                TableView<UserAdminDTO> table = getField(ctrl, "userTable");
                size.set(table.getItems().size());
            });
            assertEquals(0, size.get());
        }

        @Test
        @DisplayName("Lọc theo role cụ thể (SELLER) → chỉ trả về seller")
        void filterByRole() throws Exception {
            AdminUserListController ctrl = prepareWithUsers(
                    List.of(activeUser(), suspendedUser(), bannedUser()));

            AtomicReference<Integer> size  = new AtomicReference<>();
            AtomicReference<String>  found = new AtomicReference<>();
            runFx(() -> {
                ComboBox<String> cb = getField(ctrl, "cbFilterRole");
                cb.setValue(UserAdminDTO.ROLE_SELLER);
                TableView<UserAdminDTO> table = getField(ctrl, "userTable");
                size.set(table.getItems().size());
                if (!table.getItems().isEmpty())
                    found.set(table.getItems().get(0).getUsername());
            });
            assertEquals(1, size.get());
            assertEquals("bob", found.get());
        }

        @Test
        @DisplayName("Lọc theo status cụ thể (BANNED) → chỉ trả về banned")
        void filterByStatus() throws Exception {
            AdminUserListController ctrl = prepareWithUsers(
                    List.of(activeUser(), suspendedUser(), bannedUser()));

            AtomicReference<Integer> size  = new AtomicReference<>();
            AtomicReference<String>  found = new AtomicReference<>();
            runFx(() -> {
                ComboBox<String> cb = getField(ctrl, "cbFilterStatus");
                cb.setValue(UserAdminDTO.STATUS_BANNED);
                TableView<UserAdminDTO> table = getField(ctrl, "userTable");
                size.set(table.getItems().size());
                if (!table.getItems().isEmpty())
                    found.set(table.getItems().get(0).getUsername());
            });
            assertEquals(1, size.get());
            assertEquals("carol", found.get());
        }

        @Test
        @DisplayName("Role = null → không lọc theo role (coi như Tất cả)")
        void roleNull_treatAsAll() throws Exception {
            AdminUserListController ctrl = prepareWithUsers(
                    List.of(activeUser(), suspendedUser()));

            AtomicReference<Integer> size = new AtomicReference<>();
            runFx(() -> {
                ComboBox<String> cb = getField(ctrl, "cbFilterRole");
                cb.setValue(null);
                TableView<UserAdminDTO> table = getField(ctrl, "userTable");
                size.set(table.getItems().size());
            });
            assertEquals(2, size.get());
        }

        @Test
        @DisplayName("Status = null → không lọc theo status")
        void statusNull_treatAsAll() throws Exception {
            AdminUserListController ctrl = prepareWithUsers(
                    List.of(activeUser(), suspendedUser()));

            AtomicReference<Integer> size = new AtomicReference<>();
            runFx(() -> {
                ComboBox<String> cb = getField(ctrl, "cbFilterStatus");
                cb.setValue(null);
                TableView<UserAdminDTO> table = getField(ctrl, "userTable");
                size.set(table.getItems().size());
            });
            assertEquals(2, size.get());
        }

        @Test
        @DisplayName("tfSearch.getText() = '' → q rỗng → matchesQ luôn true")
        void searchTextEmpty_noNPE() throws Exception {
            AdminUserListController ctrl = prepareWithUsers(List.of(activeUser()));

            AtomicReference<Integer> size = new AtomicReference<>();
            runFx(() -> {
                Method m = AdminUserListController.class.getDeclaredMethod("applyFilters");
                m.setAccessible(true);
                m.invoke(ctrl);
                TableView<UserAdminDTO> table = getField(ctrl, "userTable");
                size.set(table.getItems().size());
            });
            assertEquals(1, size.get());
        }

        @Test
        @DisplayName("Predicate nhận user == null → trả về false (không crash)")
        void predicateHandlesNullUser() throws Exception {
            AdminUserListController ctrl = prepareWithUsers(List.of());

            AtomicBoolean result = new AtomicBoolean(true);
            runFx(() -> {
                var filtered = (javafx.collections.transformation.FilteredList<UserAdminDTO>)
                        getField(ctrl, "filtered");
                result.set(filtered.getPredicate().test(null));
            });
            assertFalse(result.get(), "Predicate(null) phải trả false");
        }
    }

    // =========================================================================
    // Status cell factory – updateItem branches
    // =========================================================================
    @Nested
    @DisplayName("colStatus CellFactory – updateItem branches")
    class StatusCellFactory {

        private AdminUserListController prepareCtrl() throws Exception {
            AtomicReference<AdminUserListController> ref = new AtomicReference<>();
            runFx(() -> {
                try (MockedStatic<AdminService> svc = mockStatic(AdminService.class)) {
                    svc.when(() -> AdminService.getUsers(any()))
                            .thenAnswer(inv -> {
                                Consumer<List<UserAdminDTO>> cb = inv.getArgument(0);
                                cb.accept(List.of());
                                return null;
                            });
                    AdminUserListController c = buildController();
                    c.initialize();
                    ref.set(c);
                }
            });
            return ref.get();
        }

        @Test
        @DisplayName("empty=true → setGraphic(null)")
        void emptyTrue_graphicNull() throws Exception {
            AdminUserListController ctrl = prepareCtrl();
            AtomicReference<javafx.scene.Node> graphic = new AtomicReference<>();
            runFx(() -> {
                TableCell<UserAdminDTO, String> cell = makeStatusCell(ctrl);
                callStatusUpdateItem(cell, null, true);
                graphic.set(cell.getGraphic());
            });
            assertNull(graphic.get());
        }

        @Test
        @DisplayName("item=null, empty=false → setGraphic(null)")
        void itemNull_graphicNull() throws Exception {
            AdminUserListController ctrl = prepareCtrl();
            AtomicReference<javafx.scene.Node> graphic = new AtomicReference<>();
            runFx(() -> {
                TableCell<UserAdminDTO, String> cell = makeStatusCell(ctrl);
                callStatusUpdateItem(cell, null, false);
                graphic.set(cell.getGraphic());
            });
            assertNull(graphic.get());
        }

        @Test
        @DisplayName("item=ACTIVE → badge xanh lá (#15803D)")
        void itemActive_greenBadge() throws Exception {
            AdminUserListController ctrl = prepareCtrl();
            AtomicReference<String> style = new AtomicReference<>();
            runFx(() -> {
                TableCell<UserAdminDTO, String> cell = makeStatusCell(ctrl);
                callStatusUpdateItem(cell, UserAdminDTO.STATUS_ACTIVE, false);
                style.set(((Label) cell.getGraphic()).getStyle());
            });
            assertNotNull(style.get());
            assertTrue(style.get().contains("#15803D"));
        }

        @Test
        @DisplayName("item=SUSPENDED → badge vàng (#B45309)")
        void itemSuspended_yellowBadge() throws Exception {
            AdminUserListController ctrl = prepareCtrl();
            AtomicReference<String> style = new AtomicReference<>();
            runFx(() -> {
                TableCell<UserAdminDTO, String> cell = makeStatusCell(ctrl);
                callStatusUpdateItem(cell, UserAdminDTO.STATUS_SUSPENDED, false);
                style.set(((Label) cell.getGraphic()).getStyle());
            });
            assertTrue(style.get().contains("#B45309"));
        }

        @Test
        @DisplayName("item=BANNED → badge đỏ (#B91C1C)")
        void itemBanned_redBadge() throws Exception {
            AdminUserListController ctrl = prepareCtrl();
            AtomicReference<String> style = new AtomicReference<>();
            runFx(() -> {
                TableCell<UserAdminDTO, String> cell = makeStatusCell(ctrl);
                callStatusUpdateItem(cell, UserAdminDTO.STATUS_BANNED, false);
                style.set(((Label) cell.getGraphic()).getStyle());
            });
            assertTrue(style.get().contains("#B91C1C"));
        }

        @Test
        @DisplayName("item=UNKNOWN → badge xám (#F1F5F9, nhánh else)")
        void itemUnknown_grayBadge() throws Exception {
            AdminUserListController ctrl = prepareCtrl();
            AtomicReference<String> style = new AtomicReference<>();
            runFx(() -> {
                TableCell<UserAdminDTO, String> cell = makeStatusCell(ctrl);
                callStatusUpdateItem(cell, "UNKNOWN_STATUS", false);
                style.set(((Label) cell.getGraphic()).getStyle());
            });
            assertTrue(style.get().contains("#F1F5F9"));
        }
    }

    // =========================================================================
    // Action cell factory – updateItem branches
    //
    // FIX: Dùng makeActionCell() đã attach TableView trước khi updateItem().
    //      Signature method là (Void.class, boolean.class) trên anonymous subclass.
    // =========================================================================
    @Nested
    @DisplayName("colAction CellFactory – updateItem branches")
    class ActionCellFactory {

        private AdminUserListController prepareCtrl() throws Exception {
            AtomicReference<AdminUserListController> ref = new AtomicReference<>();
            runFx(() -> {
                try (MockedStatic<AdminService> svc = mockStatic(AdminService.class)) {
                    svc.when(() -> AdminService.getUsers(any()))
                            .thenAnswer(inv -> {
                                Consumer<List<UserAdminDTO>> cb = inv.getArgument(0);
                                cb.accept(List.of());
                                return null;
                            });
                    AdminUserListController c = buildController();
                    c.initialize();
                    ref.set(c);
                }
            });
            return ref.get();
        }

        @Test
        @DisplayName("empty=true → setGraphic(null)")
        void emptyTrue_graphicNull() throws Exception {
            AdminUserListController ctrl = prepareCtrl();
            AtomicReference<javafx.scene.Node> graphic = new AtomicReference<>();
            runFx(() -> {
                // Tạo cell rỗng bằng cách không set items, truyền empty=true
                TableColumn<UserAdminDTO, Void> colAction = getField(ctrl, "colAction");
                TableCell<UserAdminDTO, Void> cell = colAction.getCellFactory().call(colAction);

                // Attach TableView (items rỗng) trước
                TableView<UserAdminDTO> table = getField(ctrl, "userTable");
                table.setItems(FXCollections.observableArrayList());

                Method updateTableView = findSuperMethod(cell.getClass(), "updateTableView", TableView.class);
                updateTableView.invoke(cell, table);
                Method updateTableColumn = findSuperMethod(cell.getClass(), "updateTableColumn", TableColumn.class);
                updateTableColumn.invoke(cell, colAction);

                // Gọi updateItem(null, true) – empty=true
                Method updateItem = cell.getClass().getDeclaredMethod("updateItem", Void.class, boolean.class);
                updateItem.setAccessible(true);
                updateItem.invoke(cell, null, true);

                graphic.set(cell.getGraphic());
            });
            assertNull(graphic.get());
        }

        @Test
        @DisplayName("index >= items.size() → setGraphic(null)")
        void indexOutOfBounds_graphicNull() throws Exception {
            AdminUserListController ctrl = prepareCtrl();
            AtomicReference<javafx.scene.Node> graphic = new AtomicReference<>();
            runFx(() -> {
                // Items rỗng, index=0 >= size=0 → graphic null
                TableCell<UserAdminDTO, Void> cell = makeActionCell(
                        ctrl, FXCollections.observableArrayList(), 0);
                graphic.set(cell.getGraphic());
            });
            assertNull(graphic.get());
        }

        @Test
        @DisplayName("user ACTIVE → suspend enabled, ban enabled, restore disabled")
        void activeUser_menuItemStates() throws Exception {
            AdminUserListController ctrl = prepareCtrl();
            AtomicBoolean suspendDisabled = new AtomicBoolean();
            AtomicBoolean banDisabled     = new AtomicBoolean();
            AtomicBoolean restoreDisabled = new AtomicBoolean();

            runFx(() -> {
                ObservableList<UserAdminDTO> items =
                        FXCollections.observableArrayList(activeUser());
                TableCell<UserAdminDTO, Void> cell = makeActionCell(ctrl, items, 0);

                MenuButton menu = (MenuButton) cell.getGraphic();
                suspendDisabled.set(menu.getItems().get(2).isDisable());
                banDisabled    .set(menu.getItems().get(3).isDisable());
                restoreDisabled.set(menu.getItems().get(1).isDisable());
            });

            assertFalse(suspendDisabled.get(), "Suspend phải enabled khi ACTIVE");
            assertFalse(banDisabled.get(),     "Ban phải enabled khi ACTIVE");
            assertTrue(restoreDisabled.get(),  "Restore phải disabled khi ACTIVE");
        }

        @Test
        @DisplayName("user SUSPENDED → suspend disabled, ban enabled, restore enabled")
        void suspendedUser_menuItemStates() throws Exception {
            AdminUserListController ctrl = prepareCtrl();
            AtomicBoolean suspendDisabled = new AtomicBoolean();
            AtomicBoolean banDisabled     = new AtomicBoolean();
            AtomicBoolean restoreDisabled = new AtomicBoolean();

            runFx(() -> {
                ObservableList<UserAdminDTO> items =
                        FXCollections.observableArrayList(suspendedUser());
                TableCell<UserAdminDTO, Void> cell = makeActionCell(ctrl, items, 0);

                MenuButton menu = (MenuButton) cell.getGraphic();
                suspendDisabled.set(menu.getItems().get(2).isDisable());
                banDisabled    .set(menu.getItems().get(3).isDisable());
                restoreDisabled.set(menu.getItems().get(1).isDisable());
            });

            assertTrue(suspendDisabled.get(),  "Suspend phải disabled khi SUSPENDED");
            assertFalse(banDisabled.get(),     "Ban phải enabled khi SUSPENDED");
            assertFalse(restoreDisabled.get(), "Restore phải enabled khi SUSPENDED");
        }

        @Test
        @DisplayName("user BANNED → suspend disabled, ban disabled, restore disabled")
        void bannedUser_menuItemStates() throws Exception {
            AdminUserListController ctrl = prepareCtrl();
            AtomicBoolean suspendDisabled = new AtomicBoolean();
            AtomicBoolean banDisabled     = new AtomicBoolean();
            AtomicBoolean restoreDisabled = new AtomicBoolean();

            runFx(() -> {
                ObservableList<UserAdminDTO> items =
                        FXCollections.observableArrayList(bannedUser());
                TableCell<UserAdminDTO, Void> cell = makeActionCell(ctrl, items, 0);

                MenuButton menu = (MenuButton) cell.getGraphic();
                suspendDisabled.set(menu.getItems().get(2).isDisable());
                banDisabled    .set(menu.getItems().get(3).isDisable());
                restoreDisabled.set(menu.getItems().get(1).isDisable());
            });

            assertTrue(suspendDisabled.get(), "Suspend phải disabled khi BANNED");
            assertTrue(banDisabled.get(),     "Ban phải disabled khi BANNED");
            assertTrue(restoreDisabled.get(), "Restore phải disabled khi BANNED");
        }
    }

    // =========================================================================
    // changeUserStatus() – private method branches
    // =========================================================================
    @Nested
    @DisplayName("changeUserStatus() branches")
    class ChangeUserStatus {

        private AdminUserListController prepareCtrl(List<UserAdminDTO> userList)
                throws Exception {
            AtomicReference<AdminUserListController> ref = new AtomicReference<>();
            runFx(() -> {
                try (MockedStatic<AdminService> svc = mockStatic(AdminService.class)) {
                    svc.when(() -> AdminService.getUsers(any()))
                            .thenAnswer(inv -> {
                                Consumer<List<UserAdminDTO>> cb = inv.getArgument(0);
                                cb.accept(userList);
                                return null;
                            });
                    AdminUserListController ctrl = buildController();
                    ctrl.initialize();
                    ref.set(ctrl);
                }
            });
            return ref.get();
        }

        @Test
        @DisplayName("user == null → return sớm, không gọi AlertUtil.showConfirm")
        void userNull_earlyReturn() throws Exception {
            AdminUserListController ctrl = prepareCtrl(List.of());
            AtomicBoolean confirmCalled = new AtomicBoolean(false);

            runFx(() -> {
                try (MockedStatic<AlertUtil> alert = mockStatic(AlertUtil.class)) {
                    alert.when(() -> AlertUtil.showConfirm(any(), any()))
                            .thenAnswer(inv -> { confirmCalled.set(true); return false; });

                    Method m = ctrl.getClass().getDeclaredMethod(
                            "changeUserStatus", UserAdminDTO.class, String.class);
                    m.setAccessible(true);
                    m.invoke(ctrl, null, UserAdminDTO.STATUS_ACTIVE);
                }
            });
            assertFalse(confirmCalled.get(), "showConfirm không được gọi khi user=null");
        }

        @Test
        @DisplayName("ok == false (user chọn No) → không gọi updateUserStatus")
        void confirmFalse_noUpdate() throws Exception {
            AdminUserListController ctrl = prepareCtrl(List.of(activeUser()));
            AtomicBoolean updateCalled = new AtomicBoolean(false);

            runFx(() -> {
                try (MockedStatic<AdminService> svc  = mockStatic(AdminService.class);
                     MockedStatic<AlertUtil>    alert = mockStatic(AlertUtil.class)) {
                    alert.when(() -> AlertUtil.showConfirm(any(), any())).thenReturn(false);
                    svc.when(() -> AdminService.updateUserStatus(anyInt(), any(), any()))
                            .thenAnswer(inv -> { updateCalled.set(true); return null; });

                    Method m = ctrl.getClass().getDeclaredMethod(
                            "changeUserStatus", UserAdminDTO.class, String.class);
                    m.setAccessible(true);
                    m.invoke(ctrl, activeUser(), UserAdminDTO.STATUS_SUSPENDED);
                }
            });
            assertFalse(updateCalled.get(), "updateUserStatus không được gọi khi user chọn No");
        }

        @Test
        @DisplayName("ok == true, success == true → showInfo('Cập nhật thành công') + loadUsers()")
        void confirmTrue_successTrue_showInfo() throws Exception {
            AdminUserListController ctrl = prepareCtrl(List.of(activeUser()));
            AtomicBoolean showInfoCalled = new AtomicBoolean(false);
            AtomicReference<Integer> getUsersCallCount = new AtomicReference<>(0);

            runFx(() -> {
                try (MockedStatic<AdminService> svc  = mockStatic(AdminService.class);
                     MockedStatic<AlertUtil>    alert = mockStatic(AlertUtil.class)) {

                    svc.when(() -> AdminService.getUsers(any()))
                            .thenAnswer(inv -> {
                                getUsersCallCount.set(getUsersCallCount.get() + 1);
                                Consumer<List<UserAdminDTO>> cb = inv.getArgument(0);
                                cb.accept(List.of(activeUser()));
                                return null;
                            });

                    alert.when(() -> AlertUtil.showConfirm(any(), any())).thenReturn(true);
                    alert.when(() -> AlertUtil.showInfo(eq("Cập nhật thành công")))
                            .thenAnswer(inv -> { showInfoCalled.set(true); return null; });

                    svc.when(() -> AdminService.updateUserStatus(anyInt(), any(), any()))
                            .thenAnswer(inv -> {
                                Consumer<Boolean> cb = inv.getArgument(2);
                                cb.accept(true);
                                return null;
                            });

                    Method m = ctrl.getClass().getDeclaredMethod(
                            "changeUserStatus", UserAdminDTO.class, String.class);
                    m.setAccessible(true);
                    m.invoke(ctrl, activeUser(), UserAdminDTO.STATUS_SUSPENDED);
                }
            });

            assertTrue(showInfoCalled.get(), "showInfo('Cập nhật thành công') phải được gọi");
            assertEquals(1, getUsersCallCount.get(), "getUsers phải được gọi 1 lần để reload");
        }

        @Test
        @DisplayName("ok == true, success == false → showError('Không thể cập nhật...')")
        void confirmTrue_successFalse_showError() throws Exception {
            AdminUserListController ctrl = prepareCtrl(List.of(activeUser()));
            AtomicReference<String> errorMsg = new AtomicReference<>();

            runFx(() -> {
                try (MockedStatic<AdminService> svc  = mockStatic(AdminService.class);
                     MockedStatic<AlertUtil>    alert = mockStatic(AlertUtil.class)) {

                    alert.when(() -> AlertUtil.showConfirm(any(), any())).thenReturn(true);
                    alert.when(() -> AlertUtil.showError(any()))
                            .thenAnswer(inv -> { errorMsg.set(inv.getArgument(0)); return null; });

                    svc.when(() -> AdminService.updateUserStatus(anyInt(), any(), any()))
                            .thenAnswer(inv -> {
                                Consumer<Boolean> cb = inv.getArgument(2);
                                cb.accept(false);
                                return null;
                            });

                    Method m = ctrl.getClass().getDeclaredMethod(
                            "changeUserStatus", UserAdminDTO.class, String.class);
                    m.setAccessible(true);
                    m.invoke(ctrl, activeUser(), UserAdminDTO.STATUS_BANNED);
                }
            });

            assertNotNull(errorMsg.get(), "showError phải được gọi");
            assertTrue(errorMsg.get().contains("Không thể cập nhật"),
                    "Error message phải chứa 'Không thể cập nhật'");
        }
    }

    // =========================================================================
    // showUserStats() – private method branches
    // =========================================================================
    @Nested
    @DisplayName("showUserStats() branches")
    class ShowUserStats {

        private AdminUserListController prepareCtrl() throws Exception {
            AtomicReference<AdminUserListController> ref = new AtomicReference<>();
            runFx(() -> {
                try (MockedStatic<AdminService> svc = mockStatic(AdminService.class)) {
                    svc.when(() -> AdminService.getUsers(any()))
                            .thenAnswer(inv -> {
                                Consumer<List<UserAdminDTO>> cb = inv.getArgument(0);
                                cb.accept(List.of());
                                return null;
                            });
                    AdminUserListController ctrl = buildController();
                    ctrl.initialize();
                    ref.set(ctrl);
                }
            });
            return ref.get();
        }

        @Test
        @DisplayName("stats == null → showError chứa 'Không tải được'")
        void statsNull_showError() throws Exception {
            AdminUserListController ctrl = prepareCtrl();
            AtomicReference<String> errorMsg = new AtomicReference<>();

            runFx(() -> {
                try (MockedStatic<AdminService> svc  = mockStatic(AdminService.class);
                     MockedStatic<AlertUtil>    alert = mockStatic(AlertUtil.class)) {

                    svc.when(() -> AdminService.getUserProfile(anyInt(), any()))
                            .thenAnswer(inv -> {
                                Consumer<UserProfileDTO> cb = inv.getArgument(1);
                                cb.accept(null);
                                return null;
                            });
                    alert.when(() -> AlertUtil.showError(any()))
                            .thenAnswer(inv -> { errorMsg.set(inv.getArgument(0)); return null; });

                    Method m = ctrl.getClass().getDeclaredMethod("showUserStats", int.class);
                    m.setAccessible(true);
                    m.invoke(ctrl, 99);
                }
            });

            assertNotNull(errorMsg.get(), "showError phải được gọi");
            assertTrue(errorMsg.get().contains("Không tải được"),
                    "Error message phải chứa 'Không tải được'");
        }

        @Test
        @DisplayName("stats != null, email != null, sellerRating != null → showInfo đầy đủ")
        void statsFullInfo_showInfo() throws Exception {
            AdminUserListController ctrl = prepareCtrl();
            AtomicReference<String> infoMsg = new AtomicReference<>();

            UserProfileDTO profile = buildProfile("alice", 1,
                    "alice@example.com",
                    new java.math.BigDecimal("4.5"), "MEMBER",
                    java.time.Instant.now(), 5, 10);

            runFx(() -> {
                try (MockedStatic<AdminService> svc  = mockStatic(AdminService.class);
                     MockedStatic<AlertUtil>    alert = mockStatic(AlertUtil.class)) {

                    svc.when(() -> AdminService.getUserProfile(anyInt(), any()))
                            .thenAnswer(inv -> {
                                Consumer<UserProfileDTO> cb = inv.getArgument(1);
                                cb.accept(profile);
                                return null;
                            });
                    alert.when(() -> AlertUtil.showInfo(any()))
                            .thenAnswer(inv -> { infoMsg.set(inv.getArgument(0)); return null; });

                    Method m = ctrl.getClass().getDeclaredMethod("showUserStats", int.class);
                    m.setAccessible(true);
                    m.invoke(ctrl, 1);
                }
            });

            assertNotNull(infoMsg.get(), "showInfo phải được gọi");
            assertTrue(infoMsg.get().contains("alice"),             "Phải chứa username");
            assertTrue(infoMsg.get().contains("alice@example.com"), "Phải chứa email");
            assertTrue(infoMsg.get().contains("4.5"),               "Phải chứa rating");
        }

        @Test
        @DisplayName("stats != null, email == null, sellerRating == null → showInfo không có email/rating")
        void statsWithoutEmailAndRating_showInfo() throws Exception {
            AdminUserListController ctrl = prepareCtrl();
            AtomicReference<String> infoMsg = new AtomicReference<>();

            UserProfileDTO profile = buildProfile("bob", 2,
                    null, null, "SELLER", null, 3, 0);

            runFx(() -> {
                try (MockedStatic<AdminService> svc  = mockStatic(AdminService.class);
                     MockedStatic<AlertUtil>    alert = mockStatic(AlertUtil.class)) {

                    svc.when(() -> AdminService.getUserProfile(anyInt(), any()))
                            .thenAnswer(inv -> {
                                Consumer<UserProfileDTO> cb = inv.getArgument(1);
                                cb.accept(profile);
                                return null;
                            });
                    alert.when(() -> AlertUtil.showInfo(any()))
                            .thenAnswer(inv -> { infoMsg.set(inv.getArgument(0)); return null; });

                    Method m = ctrl.getClass().getDeclaredMethod("showUserStats", int.class);
                    m.setAccessible(true);
                    m.invoke(ctrl, 2);
                }
            });

            assertNotNull(infoMsg.get(), "showInfo phải được gọi");
            assertTrue(infoMsg.get().contains("bob"), "Phải chứa username");
            assertFalse(infoMsg.get().contains("Email:"),  "Không được chứa dòng Email khi null");
            assertFalse(infoMsg.get().contains("/ 5"),     "Không được chứa dòng rating khi null");
        }
    }

    // =========================================================================
    // suspendSelected() / banSelected() / restoreSelected()
    // =========================================================================
    @Nested
    @DisplayName("suspendSelected / banSelected / restoreSelected")
    class SelectedActions {

        private AdminUserListController prepareWithSelection(UserAdminDTO selection)
                throws Exception {
            AtomicReference<AdminUserListController> ref = new AtomicReference<>();
            List<UserAdminDTO> list = selection != null ? List.of(selection) : List.of();

            runFx(() -> {
                try (MockedStatic<AdminService> svc = mockStatic(AdminService.class)) {
                    svc.when(() -> AdminService.getUsers(any()))
                            .thenAnswer(inv -> {
                                Consumer<List<UserAdminDTO>> cb = inv.getArgument(0);
                                cb.accept(list);
                                return null;
                            });
                    AdminUserListController ctrl = buildController();
                    ctrl.initialize();
                    if (selection != null) {
                        TableView<UserAdminDTO> table = getField(ctrl, "userTable");
                        table.getSelectionModel().select(selection);
                    }
                    ref.set(ctrl);
                }
            });
            return ref.get();
        }

        @Test
        @DisplayName("suspendSelected: không chọn user → showWarning chứa 'chọn người dùng'")
        void suspendSelected_noSelection_showWarning() throws Exception {
            AdminUserListController ctrl = prepareWithSelection(null);
            AtomicReference<String> warnMsg = new AtomicReference<>();

            runFx(() -> {
                try (MockedStatic<AlertUtil> alert = mockStatic(AlertUtil.class)) {
                    alert.when(() -> AlertUtil.showWarning(any()))
                            .thenAnswer(inv -> { warnMsg.set(inv.getArgument(0)); return null; });
                    ctrl.suspendSelected();
                }
            });

            assertNotNull(warnMsg.get(), "showWarning phải được gọi");
            assertTrue(warnMsg.get().contains("chọn người dùng"));
        }

        @Test
        @DisplayName("suspendSelected: có chọn user → gọi changeUserStatus với SUSPENDED")
        void suspendSelected_withSelection_callsChangeStatus() throws Exception {
            AdminUserListController ctrl = prepareWithSelection(activeUser());
            AtomicReference<String> confirmMsg = new AtomicReference<>();

            runFx(() -> {
                try (MockedStatic<AlertUtil> alert = mockStatic(AlertUtil.class)) {
                    alert.when(() -> AlertUtil.showConfirm(any(), any()))
                            .thenAnswer(inv -> {
                                confirmMsg.set(inv.getArgument(1));
                                return false;
                            });
                    ctrl.suspendSelected();
                }
            });

            assertNotNull(confirmMsg.get(), "showConfirm phải được gọi");
            assertTrue(confirmMsg.get().contains(UserAdminDTO.STATUS_SUSPENDED));
        }

        @Test
        @DisplayName("banSelected: không chọn user → showWarning chứa 'chọn người dùng'")
        void banSelected_noSelection_showWarning() throws Exception {
            AdminUserListController ctrl = prepareWithSelection(null);
            AtomicReference<String> warnMsg = new AtomicReference<>();

            runFx(() -> {
                try (MockedStatic<AlertUtil> alert = mockStatic(AlertUtil.class)) {
                    alert.when(() -> AlertUtil.showWarning(any()))
                            .thenAnswer(inv -> { warnMsg.set(inv.getArgument(0)); return null; });
                    ctrl.banSelected();
                }
            });

            assertNotNull(warnMsg.get(), "showWarning phải được gọi");
            assertTrue(warnMsg.get().contains("chọn người dùng"));
        }

        @Test
        @DisplayName("banSelected: có chọn user → gọi changeUserStatus với BANNED")
        void banSelected_withSelection_callsChangeStatus() throws Exception {
            AdminUserListController ctrl = prepareWithSelection(activeUser());
            AtomicReference<String> confirmMsg = new AtomicReference<>();

            runFx(() -> {
                try (MockedStatic<AlertUtil> alert = mockStatic(AlertUtil.class)) {
                    alert.when(() -> AlertUtil.showConfirm(any(), any()))
                            .thenAnswer(inv -> {
                                confirmMsg.set(inv.getArgument(1));
                                return false;
                            });
                    ctrl.banSelected();
                }
            });

            assertNotNull(confirmMsg.get(), "showConfirm phải được gọi");
            assertTrue(confirmMsg.get().contains(UserAdminDTO.STATUS_BANNED));
        }

        @Test
        @DisplayName("restoreSelected: không chọn user → showWarning chứa 'chọn người dùng'")
        void restoreSelected_noSelection_showWarning() throws Exception {
            AdminUserListController ctrl = prepareWithSelection(null);
            AtomicReference<String> warnMsg = new AtomicReference<>();

            runFx(() -> {
                try (MockedStatic<AlertUtil> alert = mockStatic(AlertUtil.class)) {
                    alert.when(() -> AlertUtil.showWarning(any()))
                            .thenAnswer(inv -> { warnMsg.set(inv.getArgument(0)); return null; });
                    ctrl.restoreSelected();
                }
            });

            assertNotNull(warnMsg.get(), "showWarning phải được gọi");
            assertTrue(warnMsg.get().contains("chọn người dùng"));
        }

        @Test
        @DisplayName("restoreSelected: có chọn user → gọi changeUserStatus với ACTIVE")
        void restoreSelected_withSelection_callsChangeStatus() throws Exception {
            AdminUserListController ctrl = prepareWithSelection(suspendedUser());
            AtomicReference<String> confirmMsg = new AtomicReference<>();

            runFx(() -> {
                try (MockedStatic<AlertUtil> alert = mockStatic(AlertUtil.class)) {
                    alert.when(() -> AlertUtil.showConfirm(any(), any()))
                            .thenAnswer(inv -> {
                                confirmMsg.set(inv.getArgument(1));
                                return false;
                            });
                    ctrl.restoreSelected();
                }
            });

            assertNotNull(confirmMsg.get(), "showConfirm phải được gọi");
            assertTrue(confirmMsg.get().contains(UserAdminDTO.STATUS_ACTIVE));
        }
    }

    // =========================================================================
    // createActionCellFactory() – MenuItem event handlers
    //
    // FIX: makeCell() đã attach TableView trước. Mock mở TRÊN FX THREAD.
    // =========================================================================
    @Nested
    @DisplayName("Action cell – MenuItem event handlers")
    class ActionCellMenuHandlers {

        private AdminUserListController prepareCtrl() throws Exception {
            AtomicReference<AdminUserListController> ref = new AtomicReference<>();
            runFx(() -> {
                try (MockedStatic<AdminService> svc = mockStatic(AdminService.class)) {
                    svc.when(() -> AdminService.getUsers(any()))
                            .thenAnswer(inv -> {
                                Consumer<List<UserAdminDTO>> cb = inv.getArgument(0);
                                cb.accept(List.of(activeUser()));
                                return null;
                            });
                    AdminUserListController c = buildController();
                    c.initialize();
                    ref.set(c);
                }
            });
            return ref.get();
        }

        @Test
        @DisplayName("itemRestore.onAction → changeUserStatus với STATUS_ACTIVE")
        void itemRestore_firesChangeStatusActive() throws Exception {
            AdminUserListController ctrl = prepareCtrl();
            AtomicReference<String> confirmMsg = new AtomicReference<>();

            runFx(() -> {
                try (MockedStatic<AlertUtil> alert = mockStatic(AlertUtil.class)) {
                    alert.when(() -> AlertUtil.showConfirm(any(), any()))
                            .thenAnswer(inv -> {
                                confirmMsg.set(inv.getArgument(1));
                                return false;
                            });

                    TableCell<UserAdminDTO, Void> cell = makeActionCell(
                            ctrl, FXCollections.observableArrayList(suspendedUser()), 0);
                    ((MenuButton) cell.getGraphic()).getItems().get(1).fire(); // itemRestore
                }
            });

            assertNotNull(confirmMsg.get(), "showConfirm phải được gọi khi fire itemRestore");
            assertTrue(confirmMsg.get().contains(UserAdminDTO.STATUS_ACTIVE));
        }

        @Test
        @DisplayName("itemSuspend.onAction → changeUserStatus với STATUS_SUSPENDED")
        void itemSuspend_firesChangeStatusSuspended() throws Exception {
            AdminUserListController ctrl = prepareCtrl();
            AtomicReference<String> confirmMsg = new AtomicReference<>();

            runFx(() -> {
                try (MockedStatic<AlertUtil> alert = mockStatic(AlertUtil.class)) {
                    alert.when(() -> AlertUtil.showConfirm(any(), any()))
                            .thenAnswer(inv -> {
                                confirmMsg.set(inv.getArgument(1));
                                return false;
                            });

                    TableCell<UserAdminDTO, Void> cell = makeActionCell(
                            ctrl, FXCollections.observableArrayList(activeUser()), 0);
                    ((MenuButton) cell.getGraphic()).getItems().get(2).fire(); // itemSuspend
                }
            });

            assertNotNull(confirmMsg.get(), "showConfirm phải được gọi khi fire itemSuspend");
            assertTrue(confirmMsg.get().contains(UserAdminDTO.STATUS_SUSPENDED));
        }

        @Test
        @DisplayName("itemBan.onAction → changeUserStatus với STATUS_BANNED")
        void itemBan_firesChangeStatusBanned() throws Exception {
            AdminUserListController ctrl = prepareCtrl();
            AtomicReference<String> confirmMsg = new AtomicReference<>();

            runFx(() -> {
                try (MockedStatic<AlertUtil> alert = mockStatic(AlertUtil.class)) {
                    alert.when(() -> AlertUtil.showConfirm(any(), any()))
                            .thenAnswer(inv -> {
                                confirmMsg.set(inv.getArgument(1));
                                return false;
                            });

                    TableCell<UserAdminDTO, Void> cell = makeActionCell(
                            ctrl, FXCollections.observableArrayList(activeUser()), 0);
                    ((MenuButton) cell.getGraphic()).getItems().get(3).fire(); // itemBan
                }
            });

            assertNotNull(confirmMsg.get(), "showConfirm phải được gọi khi fire itemBan");
            assertTrue(confirmMsg.get().contains(UserAdminDTO.STATUS_BANNED));
        }

        @Test
        @DisplayName("itemDetail.onAction → showUserStats gọi getUserProfile, stats=null → showError")
        void itemDetail_firesShowUserStats() throws Exception {
            AdminUserListController ctrl = prepareCtrl();
            AtomicBoolean getUserProfileCalled = new AtomicBoolean(false);
            AtomicBoolean showErrorCalled      = new AtomicBoolean(false);

            runFx(() -> {
                try (MockedStatic<AdminService> svc  = mockStatic(AdminService.class);
                     MockedStatic<AlertUtil>    alert = mockStatic(AlertUtil.class)) {

                    svc.when(() -> AdminService.getUserProfile(anyInt(), any()))
                            .thenAnswer(inv -> {
                                getUserProfileCalled.set(true);
                                Consumer<UserProfileDTO> cb = inv.getArgument(1);
                                cb.accept(null);
                                return null;
                            });
                    alert.when(() -> AlertUtil.showError(any()))
                            .thenAnswer(inv -> { showErrorCalled.set(true); return null; });

                    TableCell<UserAdminDTO, Void> cell = makeActionCell(
                            ctrl, FXCollections.observableArrayList(activeUser()), 0);
                    ((MenuButton) cell.getGraphic()).getItems().get(0).fire(); // itemDetail
                }
            });

            assertTrue(getUserProfileCalled.get(),
                    "AdminService.getUserProfile() phải được gọi khi fire itemDetail");
            assertTrue(showErrorCalled.get(),
                    "AlertUtil.showError() phải được gọi khi stats == null");
        }
    }
}