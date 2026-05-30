package vn.edu.vnu.uet.group8.client.controller;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.SellerService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.ModalUtil;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test for CreateItemsController.
 *
 * BUG FIX: txtDescription is declared as TextArea in the controller (not TextField).
 * - injectAllFxmlFields() now injects new TextArea() for txtDescription
 * - setTextArea() is used instead of setTextField() for txtDescription
 * - setValidBasicFields() calls setTextArea("txtDescription", ...) correctly
 */
@DisplayName("CreateItemsController – full coverage")
@ExtendWith(MockitoExtension.class)
class CreateItemsControllerTest extends FxTestBase {

    private CreateItemsController controller;

    @BeforeEach
    void setup() throws Exception {
        controller = new CreateItemsController();
        injectAllFxmlFields();
    }

    /**
     * Inject all @FXML fields with real JavaFX control instances.
     *
     * FIX: txtDescription must be a TextArea (matches controller declaration line 52).
     * Previously injected as TextField -> ClassCastException on txtDescription.getText().
     *
     * FIX: btnDeleteDraft starts visible=false, managed=false to mirror FXML default.
     */
    private void injectAllFxmlFields() throws Exception {
        // Image section
        set("paneImageUpload", new VBox());
        set("vboxUploadPlaceholder", new VBox());
        set("vboxImagePreview", new VBox());
        set("imgProduct", new ImageView());
        set("btnChooseImage", new Button("Chọn ảnh"));
        set("lblImageCount", new Label());

        // Product info
        set("txtTitle", new TextField());
        set("cbCategory", buildComboBox());
        set("cbCondition", buildComboBox());
        // FIX: txtDescription is TextArea in controller, must inject TextArea here
        set("txtDescription", new TextArea());
        set("vboxDynamicSpecs", new VBox());

        // Auction info
        set("txtStartPrice", new TextField());
        set("dpStartDate", new DatePicker());
        set("cbStartHour", buildComboBox());
        set("cbStartMinute", buildComboBox());
        set("dpEndDate", new DatePicker());
        set("cbEndHour", buildComboBox());
        set("cbEndMinute", buildComboBox());

        // Buttons
        set("btnSubmit", new Button("Đăng bài"));
        set("btnSaveDraft", new Button("Lưu nháp"));

        // FIX: btnDeleteDraft starts hidden (mirrors FXML default)
        Button btnDeleteDraft = new Button("Xóa nháp");
        btnDeleteDraft.setVisible(false);
        btnDeleteDraft.setManaged(false);
        set("btnDeleteDraft", btnDeleteDraft);
    }

    private ComboBox<String> buildComboBox() {
        return new ComboBox<>();
    }

    @SuppressWarnings("unchecked")
    private <T> T get(String fieldName) throws Exception {
        Field f = CreateItemsController.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (T) f.get(controller);
    }

    private void set(String fieldName, Object value) throws Exception {
        Field f = CreateItemsController.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(controller, value);
    }

    private Object callMethod(String methodName, Class<?>[] types, Object... args) throws Exception {
        Method m = CreateItemsController.class.getDeclaredMethod(methodName, types);
        m.setAccessible(true);
        return m.invoke(controller, args);
    }

    /** Add images to both internal lists via reflection. */
    private void addImages(int count) throws Exception {
        ArrayList<String> base64 = get("productBase64Images");
        ArrayList<Image> locals = get("localImages");
        for (int i = 0; i < count; i++) {
            base64.add("base64_img_" + i);
            locals.add(new Image("https://via.placeholder.com/1", true));
        }
    }

    // =========================================================
    // Constructor / static block
    // =========================================================

    @Test
    @DisplayName("static{} – logger field initialized")
    void staticBlock_loggerNotNull() throws Exception {
        Field log = CreateItemsController.class.getDeclaredField("log");
        log.setAccessible(true);
        assertNotNull(log.get(null));
    }

    @Test
    @DisplayName("constructor – instance created, default state")
    void constructor_defaultState() throws Exception {
        CreateItemsController c = new CreateItemsController();
        assertNotNull(c);
        Field editingItemId = CreateItemsController.class.getDeclaredField("editingItemId");
        editingItemId.setAccessible(true);
        assertNull(editingItemId.get(c));
    }

    // =========================================================
    // populateTimeComboBoxes()
    // =========================================================

    @Nested
    @DisplayName("populateTimeComboBoxes()")
    class PopulateTimeComboBoxes {

        @BeforeEach
        void invokePopulate() throws Exception {
            callMethod("populateTimeComboBoxes", new Class<?>[]{});
        }

        @Test
        @DisplayName("cbStartHour has 24 entries (00–23)")
        void startHour_24Entries() throws Exception {
            ComboBox<String> cb = get("cbStartHour");
            assertEquals(24, cb.getItems().size());
        }

        @Test
        @DisplayName("cbEndHour has 24 entries (00–23)")
        void endHour_24Entries() throws Exception {
            ComboBox<String> cb = get("cbEndHour");
            assertEquals(24, cb.getItems().size());
        }

        @Test
        @DisplayName("cbStartMinute has 60 entries (00–59)")
        void startMinute_60Entries() throws Exception {
            ComboBox<String> cb = get("cbStartMinute");
            assertEquals(60, cb.getItems().size());
        }

        @Test
        @DisplayName("cbEndMinute has 60 entries (00–59)")
        void endMinute_60Entries() throws Exception {
            ComboBox<String> cb = get("cbEndMinute");
            assertEquals(60, cb.getItems().size());
        }

        @Test
        @DisplayName("hours are zero-padded two-digit strings")
        void hours_zeroPadded() throws Exception {
            ComboBox<String> cb = get("cbStartHour");
            assertEquals("00", cb.getItems().get(0));
            assertEquals("09", cb.getItems().get(9));
            assertEquals("23", cb.getItems().get(23));
        }

        @Test
        @DisplayName("minutes are zero-padded two-digit strings")
        void minutes_zeroPadded() throws Exception {
            ComboBox<String> cb = get("cbStartMinute");
            assertEquals("00", cb.getItems().get(0));
            assertEquals("05", cb.getItems().get(5));
            assertEquals("59", cb.getItems().get(59));
        }
    }

    // =========================================================
    // setDefaultDateTime()
    // =========================================================

    @Nested
    @DisplayName("setDefaultDateTime()")
    class SetDefaultDateTime {

        @BeforeEach
        void invokeSetDefault() throws Exception {
            callMethod("populateTimeComboBoxes", new Class<?>[]{});
            callMethod("setDefaultDateTime", new Class<?>[]{});
        }

        @Test
        @DisplayName("start date is today")
        void startDate_isToday() throws Exception {
            DatePicker dp = get("dpStartDate");
            assertEquals(LocalDate.now(), dp.getValue());
        }

        @Test
        @DisplayName("end date is today + 3 days")
        void endDate_isTodayPlus3() throws Exception {
            DatePicker dp = get("dpEndDate");
            assertEquals(LocalDate.now().plusDays(3), dp.getValue());
        }

        @Test
        @DisplayName("start hour is 08")
        void startHour_is08() throws Exception {
            ComboBox<String> cb = get("cbStartHour");
            assertEquals("08", cb.getValue());
        }

        @Test
        @DisplayName("start minute is 00")
        void startMinute_is00() throws Exception {
            ComboBox<String> cb = get("cbStartMinute");
            assertEquals("00", cb.getValue());
        }

        @Test
        @DisplayName("end hour is 20")
        void endHour_is20() throws Exception {
            ComboBox<String> cb = get("cbEndHour");
            assertEquals("20", cb.getValue());
        }

        @Test
        @DisplayName("end minute is 00")
        void endMinute_is00() throws Exception {
            ComboBox<String> cb = get("cbEndMinute");
            assertEquals("00", cb.getValue());
        }
    }

    // =========================================================
    // initialize()
    // =========================================================

    @Nested
    @DisplayName("initialize()")
    class Initialize {

        @Test
        @DisplayName("no draft in model – ComboBoxes populated, btnDeleteDraft hidden")
        void noDraft_comboBoxesPopulated() throws Exception {
            try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class)) {
                ClientModel mockModel = mock(ClientModel.class);
                cm.when(ClientModel::getInstance).thenReturn(mockModel);
                when(mockModel.getCurrentAuctionItem()).thenReturn(null);

                callMethod("initialize", new Class<?>[]{});

                ComboBox<String> catCb = get("cbCategory");
                ComboBox<String> condCb = get("cbCondition");
                assertEquals(ItemCategory.values().length, catCb.getItems().size());
                assertEquals(ItemCondition.values().length, condCb.getItems().size());

                Button delBtn = get("btnDeleteDraft");
                assertFalse(delBtn.isVisible());
            }
        }

        @Test
        @DisplayName("draft exists – fields populated, btnDeleteDraft visible")
        void draftWithStatus_fieldsPopulated() throws Exception {
            AuctionItemDTO draft = buildDraftItem("Điện thoại test");

            try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class)) {
                ClientModel mockModel = mock(ClientModel.class);
                cm.when(ClientModel::getInstance).thenReturn(mockModel);
                when(mockModel.getCurrentAuctionItem()).thenReturn(draft);

                callMethod("initialize", new Class<?>[]{});

                TextField titleField = get("txtTitle");
                assertEquals("Điện thoại test", titleField.getText());

                Button delBtn = get("btnDeleteDraft");
                assertTrue(delBtn.isVisible());
                assertTrue(delBtn.isManaged());

                verify(mockModel).setCurrentAuctionItem(null);
            }
        }

        @Test
        @DisplayName("draft with null status and null endTime treated as draft")
        void draftNullStatusNullEndTime_treatedAsDraft() throws Exception {
            AuctionItemDTO draft = buildDraftItemNullFields("Title nháp");

            try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class)) {
                ClientModel mockModel = mock(ClientModel.class);
                cm.when(ClientModel::getInstance).thenReturn(mockModel);
                when(mockModel.getCurrentAuctionItem()).thenReturn(draft);

                callMethod("initialize", new Class<?>[]{});

                TextField titleField = get("txtTitle");
                assertEquals("Title nháp", titleField.getText());
            }
        }

        @Test
        @DisplayName("draft with category and condition – ComboBoxes pre-selected")
        void draftWithCategoryCondition_preSelected() throws Exception {
            AuctionItemDTO draft = buildDraftItemWithCategoryCondition(
                    "Test item", ItemCategory.ELECTRONICS, ItemCondition.NEW);

            try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class)) {
                ClientModel mockModel = mock(ClientModel.class);
                cm.when(ClientModel::getInstance).thenReturn(mockModel);
                when(mockModel.getCurrentAuctionItem()).thenReturn(draft);

                callMethod("initialize", new Class<?>[]{});

                ComboBox<String> catCb = get("cbCategory");
                ComboBox<String> condCb = get("cbCondition");
                assertEquals(ItemCategory.ELECTRONICS.getLabel(), catCb.getValue());
                assertEquals(ItemCondition.NEW.getLabel(), condCb.getValue());
            }
        }

        @Test
        @DisplayName("draft with imageUrls – images loaded into lists")
        void draftWithImages_imagesLoaded() throws Exception {
            List<String> urls = List.of(
                    "https://via.placeholder.com/100",
                    "https://via.placeholder.com/200"
            );
            AuctionItemDTO draft = buildDraftItemWithImages("Item with images", urls);

            try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class)) {
                ClientModel mockModel = mock(ClientModel.class);
                cm.when(ClientModel::getInstance).thenReturn(mockModel);
                when(mockModel.getCurrentAuctionItem()).thenReturn(draft);

                callMethod("initialize", new Class<?>[]{});

                Field idx = CreateItemsController.class.getDeclaredField("currentImageIndex");
                idx.setAccessible(true);
                assertTrue((int) idx.get(controller) >= 0);
            }
        }

        @Test
        @DisplayName("btnDeleteDraft null safe – no NPE when null")
        void btnDeleteDraftNull_noNpe() throws Exception {
            set("btnDeleteDraft", null);
            AuctionItemDTO draft = buildDraftItem("Test");

            try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class)) {
                ClientModel mockModel = mock(ClientModel.class);
                cm.when(ClientModel::getInstance).thenReturn(mockModel);
                when(mockModel.getCurrentAuctionItem()).thenReturn(draft);

                assertDoesNotThrow(() -> callMethod("initialize", new Class<?>[]{}));
            }
        }
    }

    // =========================================================
    // updateImagePreview()
    // =========================================================

    @Nested
    @DisplayName("updateImagePreview()")
    class UpdateImagePreview {

        @Test
        @DisplayName("empty images – placeholder shown, preview hidden")
        void emptyImages_placeholderVisible() throws Exception {
            callMethod("updateImagePreview", new Class<?>[]{});

            VBox placeholder = get("vboxUploadPlaceholder");
            VBox preview = get("vboxImagePreview");

            assertTrue(placeholder.isVisible());
            assertTrue(placeholder.isManaged());
            assertFalse(preview.isVisible());
            assertFalse(preview.isManaged());
        }

        @Test
        @DisplayName("has images – preview shown, placeholder hidden")
        void hasImages_previewVisible() throws Exception {
            addImages(2);
            set("currentImageIndex", 0);

            callMethod("updateImagePreview", new Class<?>[]{});

            VBox placeholder = get("vboxUploadPlaceholder");
            VBox preview = get("vboxImagePreview");

            assertFalse(placeholder.isVisible());
            assertFalse(placeholder.isManaged());
            assertTrue(preview.isVisible());
            assertTrue(preview.isManaged());
        }

        @Test
        @DisplayName("has images – lblImageCount shows correct count")
        void hasImages_labelCountCorrect() throws Exception {
            addImages(3);
            set("currentImageIndex", 1);

            callMethod("updateImagePreview", new Class<?>[]{});

            Label lbl = get("lblImageCount");
            assertEquals("2 / 3", lbl.getText());
        }
    }

    // =========================================================
    // nextImage()
    // =========================================================

    @Nested
    @DisplayName("nextImage()")
    class NextImage {

        @Test
        @DisplayName("at last image – index does NOT increment (boundary)")
        void atLastImage_noIncrement() throws Exception {
            addImages(2);
            set("currentImageIndex", 1);

            controller.nextImage();

            Field idx = CreateItemsController.class.getDeclaredField("currentImageIndex");
            idx.setAccessible(true);
            assertEquals(1, idx.get(controller));
        }

        @Test
        @DisplayName("not at last image – index increments")
        void notAtLast_increments() throws Exception {
            addImages(3);
            set("currentImageIndex", 0);

            controller.nextImage();

            Field idx = CreateItemsController.class.getDeclaredField("currentImageIndex");
            idx.setAccessible(true);
            assertEquals(1, idx.get(controller));
        }

        @Test
        @DisplayName("empty images – no action")
        void emptyImages_noAction() throws Exception {
            set("currentImageIndex", 0);
            assertDoesNotThrow(() -> controller.nextImage());
        }
    }

    // =========================================================
    // prevImage()
    // =========================================================

    @Nested
    @DisplayName("prevImage()")
    class PrevImage {

        @Test
        @DisplayName("at first image – index does NOT decrement (boundary)")
        void atFirstImage_noDecrement() throws Exception {
            addImages(2);
            set("currentImageIndex", 0);

            controller.prevImage();

            Field idx = CreateItemsController.class.getDeclaredField("currentImageIndex");
            idx.setAccessible(true);
            assertEquals(0, idx.get(controller));
        }

        @Test
        @DisplayName("not at first image – index decrements")
        void notAtFirst_decrements() throws Exception {
            addImages(3);
            set("currentImageIndex", 2);

            controller.prevImage();

            Field idx = CreateItemsController.class.getDeclaredField("currentImageIndex");
            idx.setAccessible(true);
            assertEquals(1, idx.get(controller));
        }

        @Test
        @DisplayName("empty images – no action")
        void emptyImages_noAction() throws Exception {
            set("currentImageIndex", 0);
            assertDoesNotThrow(() -> controller.prevImage());
        }
    }

    // =========================================================
    // removeImage()
    // =========================================================

    @Nested
    @DisplayName("removeImage()")
    class RemoveImage {

        @Test
        @DisplayName("empty list – no action")
        void emptyList_noAction() throws Exception {
            assertDoesNotThrow(() -> controller.removeImage());
        }

        @Test
        @DisplayName("single image removed – list empty, preview hides")
        void singleImage_removedAndPreviewHidden() throws Exception {
            addImages(1);
            set("currentImageIndex", 0);

            controller.removeImage();

            ArrayList<String> base64 = get("productBase64Images");
            assertTrue(base64.isEmpty());

            VBox placeholder = get("vboxUploadPlaceholder");
            assertTrue(placeholder.isVisible());
        }

        @Test
        @DisplayName("remove last image of multiple – index adjusts")
        void removeLastOfMultiple_indexAdjusts() throws Exception {
            addImages(3);
            set("currentImageIndex", 2);

            controller.removeImage();

            ArrayList<String> base64 = get("productBase64Images");
            assertEquals(2, base64.size());

            Field idx = CreateItemsController.class.getDeclaredField("currentImageIndex");
            idx.setAccessible(true);
            assertEquals(1, (int) idx.get(controller));
        }

        @Test
        @DisplayName("remove middle image – index stays same (still valid)")
        void removeMiddle_indexUnchanged() throws Exception {
            addImages(3);
            set("currentImageIndex", 1);

            controller.removeImage();

            Field idx = CreateItemsController.class.getDeclaredField("currentImageIndex");
            idx.setAccessible(true);
            assertEquals(1, (int) idx.get(controller));
        }
    }

    // =========================================================
    // chooseImage()
    // =========================================================

    @Nested
    @DisplayName("chooseImage()")
    class ChooseImage {

        @Test
        @DisplayName("isChoosing guard – second call while choosing is ignored")
        void isChoosing_secondCallIgnored() throws Exception {
            set("isChoosing", true);
            assertDoesNotThrow(() -> controller.chooseImage());
        }

        @Test
        @DisplayName("not choosing – isChoosing reset to false in finally block even if error")
        void notChoosing_resetAfterCall() throws Exception {
            Button btn = new Button();
            set("btnSubmit", btn);

            try {
                controller.chooseImage();
            } catch (Exception ignored) {}

            Field isChoosing = CreateItemsController.class.getDeclaredField("isChoosing");
            isChoosing.setAccessible(true);
            assertFalse((boolean) isChoosing.get(controller));
        }
    }

    // =========================================================
    // onBackClick()
    // =========================================================

    @Nested
    @DisplayName("onBackClick()")
    class OnBackClick {

        @Test
        @DisplayName("MainController null – no exception")
        void mainControllerNull_noException() {
            try (MockedStatic<MainController> mc = mockStatic(MainController.class)) {
                mc.when(MainController::getInstance).thenReturn(null);
                assertDoesNotThrow(() -> controller.onBackClick());
            }
        }

        @Test
        @DisplayName("MainController not null – switchView called")
        void mainControllerNotNull_switchViewCalled() {
            try (MockedStatic<MainController> mc = mockStatic(MainController.class)) {
                MainController mockMC = mock(MainController.class);
                mc.when(MainController::getInstance).thenReturn(mockMC);

                controller.onBackClick();

                verify(mockMC).switchView("ItemDashboard.fxml", "SELLER");
            }
        }
    }

    // =========================================================
    // handleSaveDraft() / handleCreateAuction()
    // =========================================================

    @Nested
    @DisplayName("handleSaveDraft() and handleCreateAuction()")
    class HandleSaveAndCreate {

        @Test
        @DisplayName("handleSaveDraft – calls submitData(true), shows error on empty title")
        void handleSaveDraft_callsSubmitDataTrue() throws Exception {
            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class)) {
                controller.handleSaveDraft();
                alertUtil.verify(() -> AlertUtil.showError(anyString()), atLeastOnce());
            }
        }

        @Test
        @DisplayName("handleCreateAuction – calls submitData(false), shows error on empty title")
        void handleCreateAuction_callsSubmitDataFalse() throws Exception {
            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class)) {
                controller.handleCreateAuction();
                alertUtil.verify(() -> AlertUtil.showError(anyString()), atLeastOnce());
            }
        }
    }

    // =========================================================
    // handleDeleteDraft()
    // =========================================================

    @Nested
    @DisplayName("handleDeleteDraft()")
    class HandleDeleteDraft {

        @Test
        @DisplayName("editingItemId null – returns immediately, no confirm dialog")
        void editingItemIdNull_noConfirm() throws Exception {
            set("editingItemId", null);
            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class)) {
                controller.handleDeleteDraft();
                alertUtil.verify(
                        () -> AlertUtil.showConfirm(anyString(), anyString()),
                        never()
                );
            }
        }

        @Test
        @DisplayName("editingItemId set, user cancels confirm – SellerService NOT called")
        void userCancels_sellerServiceNotCalled() throws Exception {
            set("editingItemId", 42);
            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class);
                 MockedStatic<SellerService> seller = mockStatic(SellerService.class)) {
                alertUtil.when(() -> AlertUtil.showConfirm(anyString(), anyString())).thenReturn(false);

                controller.handleDeleteDraft();

                seller.verify(
                        () -> SellerService.deleteItem(anyInt(), any()),
                        never()
                );
            }
        }

        @Test
        @DisplayName("user confirms delete – buttons disabled, SellerService.deleteItem called")
        void userConfirms_deleteItemCalled() throws Exception {
            set("editingItemId", 99);
            Button btnSubmit = get("btnSubmit");
            Button btnSaveDraft = get("btnSaveDraft");
            Button btnDeleteDraft = get("btnDeleteDraft");

            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class);
                 MockedStatic<SellerService> seller = mockStatic(SellerService.class)) {

                alertUtil.when(() -> AlertUtil.showConfirm(anyString(), anyString())).thenReturn(true);
                seller.when(() -> SellerService.deleteItem(eq(99), any(Consumer.class)))
                        .thenAnswer(inv -> null);

                controller.handleDeleteDraft();

                assertTrue(btnSubmit.isDisable());
                assertTrue(btnSaveDraft.isDisable());
                assertTrue(btnDeleteDraft.isDisable());
                seller.verify(() -> SellerService.deleteItem(eq(99), any(Consumer.class)));
            }
        }

        @Test
        @DisplayName("deleteItem callback success=true -> showInfo + switchView")
        void lambdaDeleteDraft_successTrue() throws Exception {
            set("editingItemId", 99);

            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class);
                 MockedStatic<SellerService> seller = mockStatic(SellerService.class);
                 MockedStatic<MainController> mc = mockStatic(MainController.class);
                 MockedStatic<Platform> platform = mockStatic(Platform.class)) {

                alertUtil.when(() -> AlertUtil.showConfirm(anyString(), anyString())).thenReturn(true);
                MainController mockMC = mock(MainController.class);
                mc.when(MainController::getInstance).thenReturn(mockMC);

                platform.when(() -> Platform.runLater(any(Runnable.class)))
                        .thenAnswer(inv -> {
                            ((Runnable) inv.getArgument(0)).run();
                            return null;
                        });

                AtomicReference<Consumer<Boolean>> callbackRef = new AtomicReference<>();
                seller.when(() -> SellerService.deleteItem(eq(99), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            callbackRef.set(inv.getArgument(1));
                            return null;
                        });

                controller.handleDeleteDraft();
                callbackRef.get().accept(true);

                alertUtil.verify(() -> AlertUtil.showInfo(contains("thành công")));
            }
        }

        @Test
        @DisplayName("deleteItem callback success=false -> showError")
        void lambdaDeleteDraft_successFalse() throws Exception {
            set("editingItemId", 99);

            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class);
                 MockedStatic<SellerService> seller = mockStatic(SellerService.class);
                 MockedStatic<Platform> platform = mockStatic(Platform.class)) {

                alertUtil.when(() -> AlertUtil.showConfirm(anyString(), anyString())).thenReturn(true);

                platform.when(() -> Platform.runLater(any(Runnable.class)))
                        .thenAnswer(inv -> {
                            ((Runnable) inv.getArgument(0)).run();
                            return null;
                        });

                AtomicReference<Consumer<Boolean>> callbackRef = new AtomicReference<>();
                seller.when(() -> SellerService.deleteItem(eq(99), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            callbackRef.set(inv.getArgument(1));
                            return null;
                        });

                controller.handleDeleteDraft();
                callbackRef.get().accept(false);

                alertUtil.verify(() -> AlertUtil.showError(contains("thất bại")));
            }
        }

        @Test
        @DisplayName("buttons re-enabled after delete callback completes")
        void lambdaDeleteDraft_buttonsReEnabled() throws Exception {
            set("editingItemId", 55);
            Button btnSubmit = get("btnSubmit");
            Button btnSaveDraft = get("btnSaveDraft");
            Button btnDeleteDraft = get("btnDeleteDraft");

            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class);
                 MockedStatic<SellerService> seller = mockStatic(SellerService.class);
                 MockedStatic<MainController> mc = mockStatic(MainController.class);
                 MockedStatic<Platform> platform = mockStatic(Platform.class)) {

                alertUtil.when(() -> AlertUtil.showConfirm(anyString(), anyString())).thenReturn(true);
                mc.when(MainController::getInstance).thenReturn(null);

                platform.when(() -> Platform.runLater(any(Runnable.class)))
                        .thenAnswer(inv -> {
                            ((Runnable) inv.getArgument(0)).run();
                            return null;
                        });

                AtomicReference<Consumer<Boolean>> callbackRef = new AtomicReference<>();
                seller.when(() -> SellerService.deleteItem(eq(55), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            callbackRef.set(inv.getArgument(1));
                            return null;
                        });

                controller.handleDeleteDraft();
                callbackRef.get().accept(true);

                assertFalse(btnSubmit.isDisable());
                assertFalse(btnSaveDraft.isDisable());
                assertFalse(btnDeleteDraft.isDisable());
            }
        }

        @Test
        @DisplayName("btnSaveDraft/btnDeleteDraft null-safe during handleDeleteDraft")
        void nullButtons_noNpe() throws Exception {
            set("editingItemId", 77);
            set("btnSaveDraft", null);
            set("btnDeleteDraft", null);

            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class);
                 MockedStatic<SellerService> seller = mockStatic(SellerService.class)) {

                alertUtil.when(() -> AlertUtil.showConfirm(anyString(), anyString())).thenReturn(true);
                seller.when(() -> SellerService.deleteItem(anyInt(), any())).thenAnswer(inv -> null);

                assertDoesNotThrow(() -> controller.handleDeleteDraft());
            }
        }
    }

    // =========================================================
    // submitData(boolean) – validation branches
    // =========================================================

    @Nested
    @DisplayName("submitData() – validation")
    class SubmitDataValidation {

        @Test
        @DisplayName("empty title – error shown, no service call")
        void emptyTitle_showsError() throws Exception {
            setTextField("txtTitle", "");
            setComboValue("cbCategory", ItemCategory.ELECTRONICS.getLabel());
            setComboValue("cbCondition", ItemCondition.NEW.getLabel());

            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class)) {
                controller.handleSaveDraft();
                alertUtil.verify(() -> AlertUtil.showError(contains("Tên")));
            }
        }

        @Test
        @DisplayName("null category – error shown")
        void nullCategory_showsError() throws Exception {
            setTextField("txtTitle", "Test Title");

            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class)) {
                controller.handleSaveDraft();
                alertUtil.verify(() -> AlertUtil.showError(anyString()));
            }
        }

        @Test
        @DisplayName("null condition – error shown")
        void nullCondition_showsError() throws Exception {
            setTextField("txtTitle", "Test Title");
            setComboValue("cbCategory", ItemCategory.ELECTRONICS.getLabel());

            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class)) {
                controller.handleSaveDraft();
                alertUtil.verify(() -> AlertUtil.showError(anyString()));
            }
        }

        @Test
        @DisplayName("isDraft=false with empty startPrice – error shown")
        void notDraft_emptyStartPrice_showsError() throws Exception {
            setValidBasicFields();
            setTextField("txtStartPrice", "");

            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class)) {
                controller.handleCreateAuction();
                alertUtil.verify(() -> AlertUtil.showError(contains("Giá khởi điểm")));
            }
        }

        @Test
        @DisplayName("isDraft=false with zero price – error shown")
        void notDraft_zeroPrice_showsError() throws Exception {
            setValidBasicFields();
            setTextField("txtStartPrice", "0");

            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class)) {
                controller.handleCreateAuction();
                alertUtil.verify(() -> AlertUtil.showError(contains("lớn hơn 0")));
            }
        }

        @Test
        @DisplayName("isDraft=false with negative price – error shown")
        void notDraft_negativePrice_showsError() throws Exception {
            setValidBasicFields();
            setTextField("txtStartPrice", "-100");

            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class)) {
                controller.handleCreateAuction();
                alertUtil.verify(() -> AlertUtil.showError(contains("lớn hơn 0")));
            }
        }

        @Test
        @DisplayName("isDraft=false with non-numeric price – NumberFormatException caught, error shown")
        void notDraft_invalidPrice_showsError() throws Exception {
            setValidBasicFields();
            setTextField("txtStartPrice", "abc");

            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class)) {
                controller.handleCreateAuction();
                alertUtil.verify(() -> AlertUtil.showError(contains("không hợp lệ")));
            }
        }

        @Test
        @DisplayName("isDraft=false with null startDate – error shown")
        void notDraft_nullStartDate_showsError() throws Exception {
            setValidBasicFields();
            setTextField("txtStartPrice", "100000");

            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class)) {
                controller.handleCreateAuction();
                alertUtil.verify(() -> AlertUtil.showError(contains("thời gian")));
            }
        }

        @Test
        @DisplayName("isDraft=false with null start hour – error shown")
        void notDraft_nullStartHour_showsError() throws Exception {
            setValidBasicFields();
            setTextField("txtStartPrice", "100000");
            setDatePickerValue("dpStartDate", LocalDate.now().plusDays(1));
            setDatePickerValue("dpEndDate", LocalDate.now().plusDays(4));

            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class)) {
                controller.handleCreateAuction();
                alertUtil.verify(() -> AlertUtil.showError(contains("thời gian")));
            }
        }

        @Test
        @DisplayName("isDraft=false with start time in past – error shown")
        void notDraft_startTimeInPast_showsError() throws Exception {
            setValidBasicFields();
            setTextField("txtStartPrice", "100000");
            setDatePickerValue("dpStartDate", LocalDate.now().minusDays(10));
            setDatePickerValue("dpEndDate", LocalDate.now().plusDays(1));
            setComboValue("cbStartHour", "08");
            setComboValue("cbStartMinute", "00");
            setComboValue("cbEndHour", "20");
            setComboValue("cbEndMinute", "00");

            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class)) {
                controller.handleCreateAuction();
                alertUtil.verify(() -> AlertUtil.showError(contains("quá khứ")));
            }
        }

        @Test
        @DisplayName("isDraft=false with end time in past – error shown")
        void notDraft_endTimeInPast_showsError() throws Exception {
            setValidBasicFields();
            setTextField("txtStartPrice", "100000");
            setDatePickerValue("dpStartDate", LocalDate.now().minusDays(2));
            setDatePickerValue("dpEndDate", LocalDate.now().minusDays(1));
            setComboValue("cbStartHour", "08");
            setComboValue("cbStartMinute", "00");
            setComboValue("cbEndHour", "20");
            setComboValue("cbEndMinute", "00");

            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class)) {
                controller.handleCreateAuction();
                alertUtil.verify(() -> AlertUtil.showError(contains("quá khứ")));
            }
        }

        @Test
        @DisplayName("isDraft=false with endTime <= startTime – error shown")
        void notDraft_endNotAfterStart_showsError() throws Exception {
            setValidBasicFields();
            setTextField("txtStartPrice", "100000");
            LocalDate future = LocalDate.now().plusDays(5);
            setDatePickerValue("dpStartDate", future);
            setDatePickerValue("dpEndDate", future);
            setComboValue("cbStartHour", "20");
            setComboValue("cbStartMinute", "00");
            setComboValue("cbEndHour", "08");
            setComboValue("cbEndMinute", "00");

            try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class)) {
                controller.handleCreateAuction();
                alertUtil.verify(() -> AlertUtil.showError(contains("sau thời gian bắt đầu")));
            }
        }

        @Test
        @DisplayName("isDraft=true – valid basic fields reach SellerService.createItem (new item)")
        void draft_validFields_callsCreateItem() throws Exception {
            setValidBasicFields();

            try (MockedStatic<SellerService> seller = mockStatic(SellerService.class)) {
                seller.when(() -> SellerService.createItem(any(), any(Consumer.class), any(Consumer.class)))
                        .thenAnswer(inv -> null);

                controller.handleSaveDraft();

                seller.verify(() ->
                        SellerService.createItem(any(), any(Consumer.class), any(Consumer.class))
                );
            }
        }
    }

    // =========================================================
    // submitData – new item callbacks
    // =========================================================

    @Nested
    @DisplayName("submitData() – new item callbacks")
    class SubmitDataNewItemCallbacks {

        @Test
        @DisplayName("createItem success -> modal + switchView")
        void newItemCreateSuccess_modalAndSwitch() throws Exception {
            setValidBasicFields();

            try (MockedStatic<SellerService> seller = mockStatic(SellerService.class);
                 MockedStatic<ModalUtil> modalUtil = mockStatic(ModalUtil.class);
                 MockedStatic<MainController> mc = mockStatic(MainController.class);
                 MockedStatic<Platform> platform = mockStatic(Platform.class)) {

                MainController mockMC = mock(MainController.class);
                mc.when(MainController::getInstance).thenReturn(mockMC);
                platform.when(() -> Platform.runLater(any(Runnable.class)))
                        .thenAnswer(inv -> {
                            ((Runnable) inv.getArgument(0)).run();
                            return null;
                        });

                AtomicReference<Consumer<vn.edu.vnu.uet.group8.common.entity.Item>> onResult = new AtomicReference<>();
                seller.when(() -> SellerService.createItem(any(), any(Consumer.class), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            onResult.set(inv.getArgument(1));
                            return null;
                        });

                controller.handleSaveDraft();
                onResult.get().accept(null);

                modalUtil.verify(() -> ModalUtil.showModal(anyString(), eq("SuccessContent.fxml")));
                verify(mockMC).switchView("ItemDashboard.fxml", "SELLER");
            }
        }

        @Test
        @DisplayName("createItem error -> showError with error message")
        void newItemCreateError_showsError() throws Exception {
            setValidBasicFields();

            try (MockedStatic<SellerService> seller = mockStatic(SellerService.class);
                 MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class);
                 MockedStatic<Platform> platform = mockStatic(Platform.class)) {

                platform.when(() -> Platform.runLater(any(Runnable.class)))
                        .thenAnswer(inv -> {
                            ((Runnable) inv.getArgument(0)).run();
                            return null;
                        });

                AtomicReference<Consumer<String>> onFailure = new AtomicReference<>();
                seller.when(() -> SellerService.createItem(any(), any(Consumer.class), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            onFailure.set(inv.getArgument(2));
                            return null;
                        });

                controller.handleSaveDraft();
                onFailure.get().accept("Server lỗi");

                alertUtil.verify(() -> AlertUtil.showError(contains("Server lỗi")));
            }
        }

        @Test
        @DisplayName("createItem success re-enables buttons")
        void newItemCreateSuccess_buttonsReEnabled() throws Exception {
            setValidBasicFields();
            Button btnSubmit = get("btnSubmit");
            Button btnSaveDraft = get("btnSaveDraft");

            try (MockedStatic<SellerService> seller = mockStatic(SellerService.class);
                 MockedStatic<ModalUtil> modalUtil = mockStatic(ModalUtil.class);
                 MockedStatic<MainController> mc = mockStatic(MainController.class);
                 MockedStatic<Platform> platform = mockStatic(Platform.class)) {

                mc.when(MainController::getInstance).thenReturn(null);
                platform.when(() -> Platform.runLater(any(Runnable.class)))
                        .thenAnswer(inv -> {
                            ((Runnable) inv.getArgument(0)).run();
                            return null;
                        });

                AtomicReference<Consumer<vn.edu.vnu.uet.group8.common.entity.Item>> onResult = new AtomicReference<>();
                seller.when(() -> SellerService.createItem(any(), any(Consumer.class), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            onResult.set(inv.getArgument(1));
                            return null;
                        });

                controller.handleSaveDraft();
                onResult.get().accept(null);

                assertFalse(btnSubmit.isDisable());
                assertFalse(btnSaveDraft.isDisable());
            }
        }

        @Test
        @DisplayName("createItem error callback – no exception thrown")
        void errorStringLambda_noOp() throws Exception {
            setValidBasicFields();

            try (MockedStatic<SellerService> seller = mockStatic(SellerService.class);
                 MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class);
                 MockedStatic<Platform> platform = mockStatic(Platform.class)) {

                platform.when(() -> Platform.runLater(any(Runnable.class)))
                        .thenAnswer(inv -> {
                            ((Runnable) inv.getArgument(0)).run();
                            return null;
                        });

                AtomicReference<Consumer<String>> onFailure = new AtomicReference<>();
                seller.when(() -> SellerService.createItem(any(), any(Consumer.class), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            onFailure.set(inv.getArgument(2));
                            return null;
                        });

                controller.handleSaveDraft();
                assertDoesNotThrow(() -> onFailure.get().accept("error"));
            }
        }
    }

    // =========================================================
    // submitData – editing existing draft (updateItem)
    // =========================================================

    @Nested
    @DisplayName("submitData() – editing draft (updateItem)")
    class SubmitDataUpdateDraft {

        @BeforeEach
        void setEditingId() throws Exception {
            set("editingItemId", 10);
        }

        @Test
        @DisplayName("isDraft=true, editingItemId set – updateItem called")
        void updateItemCalled() throws Exception {
            setValidBasicFields();

            try (MockedStatic<SellerService> seller = mockStatic(SellerService.class)) {
                seller.when(() -> SellerService.updateItem(eq(10), any(), any(Consumer.class)))
                        .thenAnswer(inv -> null);

                controller.handleSaveDraft();

                seller.verify(() -> SellerService.updateItem(eq(10), any(), any(Consumer.class)));
            }
        }

        @Test
        @DisplayName("updateItem success -> modal + switchView")
        void updateSuccess_modalAndSwitch() throws Exception {
            setValidBasicFields();

            try (MockedStatic<SellerService> seller = mockStatic(SellerService.class);
                 MockedStatic<ModalUtil> modalUtil = mockStatic(ModalUtil.class);
                 MockedStatic<MainController> mc = mockStatic(MainController.class);
                 MockedStatic<Platform> platform = mockStatic(Platform.class)) {

                MainController mockMC = mock(MainController.class);
                mc.when(MainController::getInstance).thenReturn(mockMC);
                platform.when(() -> Platform.runLater(any(Runnable.class)))
                        .thenAnswer(inv -> {
                            ((Runnable) inv.getArgument(0)).run();
                            return null;
                        });

                AtomicReference<Consumer<Boolean>> cb = new AtomicReference<>();
                seller.when(() -> SellerService.updateItem(eq(10), any(), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            cb.set(inv.getArgument(2));
                            return null;
                        });

                controller.handleSaveDraft();
                cb.get().accept(true);

                modalUtil.verify(() -> ModalUtil.showModal(anyString(), eq("SuccessContent.fxml")));
                verify(mockMC).switchView("ItemDashboard.fxml", "SELLER");
            }
        }

        @Test
        @DisplayName("updateItem failure -> showError")
        void updateFailure_showsError() throws Exception {
            setValidBasicFields();

            try (MockedStatic<SellerService> seller = mockStatic(SellerService.class);
                 MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class);
                 MockedStatic<Platform> platform = mockStatic(Platform.class)) {

                platform.when(() -> Platform.runLater(any(Runnable.class)))
                        .thenAnswer(inv -> {
                            ((Runnable) inv.getArgument(0)).run();
                            return null;
                        });

                AtomicReference<Consumer<Boolean>> cb = new AtomicReference<>();
                seller.when(() -> SellerService.updateItem(eq(10), any(), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            cb.set(inv.getArgument(2));
                            return null;
                        });

                controller.handleSaveDraft();
                cb.get().accept(false);

                alertUtil.verify(() -> AlertUtil.showError(contains("thất bại")));
            }
        }

        @Test
        @DisplayName("updateItem callback re-enables buttons")
        void updateLambda_buttonsReEnabled() throws Exception {
            setValidBasicFields();
            Button btnSubmit = get("btnSubmit");

            try (MockedStatic<SellerService> seller = mockStatic(SellerService.class);
                 MockedStatic<ModalUtil> modalUtil = mockStatic(ModalUtil.class);
                 MockedStatic<MainController> mc = mockStatic(MainController.class);
                 MockedStatic<Platform> platform = mockStatic(Platform.class)) {

                mc.when(MainController::getInstance).thenReturn(null);
                platform.when(() -> Platform.runLater(any(Runnable.class)))
                        .thenAnswer(inv -> {
                            ((Runnable) inv.getArgument(0)).run();
                            return null;
                        });

                AtomicReference<Consumer<Boolean>> cb = new AtomicReference<>();
                seller.when(() -> SellerService.updateItem(anyInt(), any(), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            cb.set(inv.getArgument(2));
                            return null;
                        });

                controller.handleSaveDraft();
                cb.get().accept(true);

                assertFalse(btnSubmit.isDisable());
            }
        }
    }

    // =========================================================
    // submitData – publish from draft (deleteItem + createItem)
    // =========================================================

    @Nested
    @DisplayName("submitData() – publish from draft (deleteItem + createItem)")
    class SubmitDataPublishFromDraft {

        @BeforeEach
        void setEditingId() throws Exception {
            set("editingItemId", 20);
        }

        @Test
        @DisplayName("isDraft=false, editingItemId set – deleteItem called first")
        void deleteItemCalledFirst() throws Exception {
            setValidAuctionFields();

            try (MockedStatic<SellerService> seller = mockStatic(SellerService.class)) {
                seller.when(() -> SellerService.deleteItem(eq(20), any(Consumer.class)))
                        .thenAnswer(inv -> null);

                controller.handleCreateAuction();

                seller.verify(() -> SellerService.deleteItem(eq(20), any(Consumer.class)));
            }
        }

        @Test
        @DisplayName("deleteItem success -> createItem called")
        void deleteSuccess_createItemCalled() throws Exception {
            setValidAuctionFields();

            try (MockedStatic<SellerService> seller = mockStatic(SellerService.class)) {
                AtomicReference<Consumer<Boolean>> deleteCb = new AtomicReference<>();
                seller.when(() -> SellerService.deleteItem(eq(20), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            deleteCb.set(inv.getArgument(1));
                            return null;
                        });
                seller.when(() -> SellerService.createItem(any(), any(Consumer.class), any(Consumer.class)))
                        .thenAnswer(inv -> null);

                controller.handleCreateAuction();
                deleteCb.get().accept(true);

                seller.verify(() -> SellerService.createItem(any(), any(Consumer.class), any(Consumer.class)));
            }
        }

        @Test
        @DisplayName("deleteItem failure -> showError (cannot convert draft)")
        void deleteFailure_showsError() throws Exception {
            setValidAuctionFields();

            try (MockedStatic<SellerService> seller = mockStatic(SellerService.class);
                 MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class);
                 MockedStatic<Platform> platform = mockStatic(Platform.class)) {

                platform.when(() -> Platform.runLater(any(Runnable.class)))
                        .thenAnswer(inv -> {
                            ((Runnable) inv.getArgument(0)).run();
                            return null;
                        });

                AtomicReference<Consumer<Boolean>> deleteCb = new AtomicReference<>();
                seller.when(() -> SellerService.deleteItem(eq(20), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            deleteCb.set(inv.getArgument(1));
                            return null;
                        });

                controller.handleCreateAuction();
                deleteCb.get().accept(false);

                alertUtil.verify(() -> AlertUtil.showError(contains("chuyển đổi bản nháp")));
            }
        }

        @Test
        @DisplayName("delete+create success -> buttons re-enabled")
        void deleteAndCreateSuccess_buttonsReEnabled() throws Exception {
            setValidAuctionFields();
            Button btnSubmit = get("btnSubmit");

            try (MockedStatic<SellerService> seller = mockStatic(SellerService.class);
                 MockedStatic<ModalUtil> modalUtil = mockStatic(ModalUtil.class);
                 MockedStatic<MainController> mc = mockStatic(MainController.class);
                 MockedStatic<Platform> platform = mockStatic(Platform.class)) {

                mc.when(MainController::getInstance).thenReturn(null);
                platform.when(() -> Platform.runLater(any(Runnable.class)))
                        .thenAnswer(inv -> {
                            ((Runnable) inv.getArgument(0)).run();
                            return null;
                        });

                AtomicReference<Consumer<Boolean>> deleteCb = new AtomicReference<>();
                AtomicReference<Consumer<vn.edu.vnu.uet.group8.common.entity.Item>> createCb = new AtomicReference<>();

                seller.when(() -> SellerService.deleteItem(eq(20), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            deleteCb.set(inv.getArgument(1));
                            return null;
                        });
                seller.when(() -> SellerService.createItem(any(), any(Consumer.class), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            createCb.set(inv.getArgument(1));
                            return null;
                        });

                controller.handleCreateAuction();
                deleteCb.get().accept(true);
                createCb.get().accept(null);

                assertFalse(btnSubmit.isDisable());
            }
        }

        @Test
        @DisplayName("delete+create error -> showError with error message")
        void deleteAndCreateError_showsError() throws Exception {
            setValidAuctionFields();

            try (MockedStatic<SellerService> seller = mockStatic(SellerService.class);
                 MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class);
                 MockedStatic<Platform> platform = mockStatic(Platform.class)) {

                platform.when(() -> Platform.runLater(any(Runnable.class)))
                        .thenAnswer(inv -> {
                            ((Runnable) inv.getArgument(0)).run();
                            return null;
                        });

                AtomicReference<Consumer<Boolean>> deleteCb = new AtomicReference<>();
                AtomicReference<Consumer<String>> createErrCb = new AtomicReference<>();

                seller.when(() -> SellerService.deleteItem(eq(20), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            deleteCb.set(inv.getArgument(1));
                            return null;
                        });
                seller.when(() -> SellerService.createItem(any(), any(Consumer.class), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            createErrCb.set(inv.getArgument(2));
                            return null;
                        });

                controller.handleCreateAuction();
                deleteCb.get().accept(true);
                createErrCb.get().accept("Lỗi mạng");

                alertUtil.verify(() -> AlertUtil.showError(contains("Lỗi mạng")));
            }
        }

        @Test
        @DisplayName("Item success consumer – invoked without exception")
        void itemSuccessLambda_invoked() throws Exception {
            setValidAuctionFields();

            try (MockedStatic<SellerService> seller = mockStatic(SellerService.class);
                 MockedStatic<ModalUtil> modalUtil = mockStatic(ModalUtil.class);
                 MockedStatic<MainController> mc = mockStatic(MainController.class);
                 MockedStatic<Platform> platform = mockStatic(Platform.class)) {

                mc.when(MainController::getInstance).thenReturn(null);
                platform.when(() -> Platform.runLater(any(Runnable.class)))
                        .thenAnswer(inv -> {
                            ((Runnable) inv.getArgument(0)).run();
                            return null;
                        });

                AtomicReference<Consumer<Boolean>> deleteCb = new AtomicReference<>();
                AtomicReference<Consumer<vn.edu.vnu.uet.group8.common.entity.Item>> createCb = new AtomicReference<>();

                seller.when(() -> SellerService.deleteItem(anyInt(), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            deleteCb.set(inv.getArgument(1));
                            return null;
                        });
                seller.when(() -> SellerService.createItem(any(), any(Consumer.class), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            createCb.set(inv.getArgument(1));
                            return null;
                        });

                controller.handleCreateAuction();
                deleteCb.get().accept(true);
                assertDoesNotThrow(() -> createCb.get().accept(null));
            }
        }

        @Test
        @DisplayName("error string lambda – invoked without exception")
        void errorStringLambda_noException() throws Exception {
            setValidAuctionFields();

            try (MockedStatic<SellerService> seller = mockStatic(SellerService.class);
                 MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class);
                 MockedStatic<Platform> platform = mockStatic(Platform.class)) {

                platform.when(() -> Platform.runLater(any(Runnable.class)))
                        .thenAnswer(inv -> {
                            ((Runnable) inv.getArgument(0)).run();
                            return null;
                        });

                AtomicReference<Consumer<Boolean>> deleteCb = new AtomicReference<>();
                AtomicReference<Consumer<String>> createErrCb = new AtomicReference<>();

                seller.when(() -> SellerService.deleteItem(anyInt(), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            deleteCb.set(inv.getArgument(1));
                            return null;
                        });
                seller.when(() -> SellerService.createItem(any(), any(Consumer.class), any(Consumer.class)))
                        .thenAnswer(inv -> {
                            createErrCb.set(inv.getArgument(2));
                            return null;
                        });

                controller.handleCreateAuction();
                deleteCb.get().accept(true);
                assertDoesNotThrow(() -> createErrCb.get().accept("some error"));
            }
        }
    }

    // =========================================================
    // submitData – exception / edge cases
    // =========================================================

    @Test
    @DisplayName("submitData – unexpected exception caught and shown as system error")
    void submitData_unexpectedException_showsSystemError() throws Exception {
        setValidBasicFields();
        ComboBox<String> cb = get("cbCategory");
        cb.setValue("INVALID_CATEGORY_NOT_IN_ENUM_$$");

        try (MockedStatic<AlertUtil> alertUtil = mockStatic(AlertUtil.class)) {
            assertDoesNotThrow(() -> controller.handleSaveDraft());
        }
    }

    @Test
    @DisplayName("submitData – btnSaveDraft null does not throw")
    void submitDataBtnSaveDraftNull_noNpe() throws Exception {
        set("btnSaveDraft", null);
        setValidBasicFields();

        try (MockedStatic<SellerService> seller = mockStatic(SellerService.class)) {
            seller.when(() -> SellerService.createItem(any(), any(), any())).thenAnswer(inv -> null);
            assertDoesNotThrow(() -> controller.handleSaveDraft());
        }
    }

    // =========================================================
    // Helper methods
    // =========================================================

    /** Set text on a TextField field. */
    private void setTextField(String fieldName, String text) throws Exception {
        TextField tf = get(fieldName);
        tf.setText(text);
    }

    /**
     * Set text on a TextArea field.
     * FIX: txtDescription is TextArea in the controller – use this method for it,
     * never setTextField("txtDescription", ...).
     */
    private void setTextArea(String fieldName, String text) throws Exception {
        TextArea ta = get(fieldName);
        ta.setText(text);
    }

    private void setComboValue(String fieldName, String value) throws Exception {
        ComboBox<String> cb = get(fieldName);
        if (!cb.getItems().contains(value)) cb.getItems().add(value);
        cb.setValue(value);
    }

    private void setDatePickerValue(String fieldName, LocalDate date) throws Exception {
        DatePicker dp = get(fieldName);
        dp.setValue(date);
    }

    /**
     * Minimum fields for a valid draft (isDraft=true) submission.
     *
     * FIX: txtDescription must be set via setTextArea(), not setTextField().
     * The old code called setTextField("txtDescription", ...) which caused
     * ClassCastException because the field is declared as TextArea in the controller.
     */
    private void setValidBasicFields() throws Exception {
        setTextField("txtTitle", "iPhone 15 Test");
        setComboValue("cbCategory", ItemCategory.ELECTRONICS.getLabel());
        setComboValue("cbCondition", ItemCondition.NEW.getLabel());
        // FIX: use setTextArea here, not setTextField
        setTextArea("txtDescription", "Mô tả test");
    }

    /**
     * All required fields for a valid auction (isDraft=false) submission.
     */
    private void setValidAuctionFields() throws Exception {
        setValidBasicFields();
        setTextField("txtStartPrice", "1000000");
        setDatePickerValue("dpStartDate", LocalDate.now().plusDays(1));
        setDatePickerValue("dpEndDate", LocalDate.now().plusDays(4));
        setComboValue("cbStartHour", "08");
        setComboValue("cbStartMinute", "00");
        setComboValue("cbEndHour", "20");
        setComboValue("cbEndMinute", "00");
    }

    // =========================================================
    // DTO builder helpers
    // =========================================================

    private AuctionItemDTO buildDraftItem(String title) {
        return AuctionItemDTO.of(
                1, title, "desc",
                ItemCategory.ELECTRONICS, ItemCondition.NEW,
                null, null, null,
                1, "seller", null, null, 0, null
        );
    }

    private AuctionItemDTO buildDraftItemNullFields(String title) {
        return AuctionItemDTO.of(
                2, title, "desc",
                ItemCategory.ELECTRONICS, ItemCondition.NEW,
                null, null, null,
                1, "seller", null, null, 0, null
        );
    }

    private AuctionItemDTO buildDraftItemWithCategoryCondition(
            String title, ItemCategory cat, ItemCondition cond) {
        return AuctionItemDTO.of(
                3, title, "desc",
                cat, cond,
                null, null, null,
                1, "seller", null, null, 0, null
        );
    }

    private AuctionItemDTO buildDraftItemWithImages(String title, List<String> urls) {
        return AuctionItemDTO.of(
                4, title, "desc",
                ItemCategory.ELECTRONICS, ItemCondition.NEW,
                null, null, null,
                1, "seller", null, urls, 0, null
        );
    }
}