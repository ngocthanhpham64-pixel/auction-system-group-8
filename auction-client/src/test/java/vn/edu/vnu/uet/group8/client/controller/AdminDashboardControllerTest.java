package vn.edu.vnu.uet.group8.client.controller;

import javafx.application.Platform;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import vn.edu.vnu.uet.group8.client.service.AdminService;
import vn.edu.vnu.uet.group8.common.dto.model.AdminStatsDTO;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test toàn diện cho AdminDashboardController.
 * Mục tiêu: 100% instruction coverage và 100% branch coverage.
 *
 * Chiến lược:
 * - Inject @FXML fields bằng reflection (không cần FXML loader).
 * - Mock AdminService.getStats (static method) bằng MockedStatic.
 * - flushFxEvents() để drain Platform.runLater queue trước khi assert.
 * - Gọi trực tiếp private methods (applyCardsData, updateChartsData,
 *   applyPieColors, createNewSlice) để cover từng branch độc lập.
 *
 * Branches cần cover:
 *  applyCardsData   : revenue != null (true) | revenue == null (false)
 *  updateChartsData : empty-state (sum == 0) | có data (sum > 0)
 *                     endedNoBid > 0 (5 slices) | endedNoBid == 0 (4 slices)
 *                     maxRev > 0 | maxRev == 0  (via totalRevenue=0 + bids=0)
 *                     maxBid > 0 | maxBid == 0
 *  refreshData      : stats callback null → AdminStatsDTO.empty()
 *                     stats callback không null → dùng stats thật
 *  initialize       : flow thẳng (toàn bộ setup)
 */
@DisplayName("AdminDashboardController")
class AdminDashboardControllerTest extends FxTestBase {

    private AdminDashboardController controller;

    // FXML controls
    private Label lblActiveAuctions;
    private Label lblTotalUsers;
    private Label lblRevenue;
    private Label lblTotalBids;
    private AreaChart<String, Number> revenueChart;
    private PieChart statusPieChart;

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

    @BeforeEach
    void setup() throws Exception {
        controller = new AdminDashboardController();

        lblActiveAuctions = new Label();
        lblTotalUsers     = new Label();
        lblRevenue        = new Label();
        lblTotalBids      = new Label();

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        revenueChart  = new AreaChart<>(xAxis, yAxis);
        statusPieChart = new PieChart();

        injectField("lblActiveAuctions", lblActiveAuctions);
        injectField("lblTotalUsers",     lblTotalUsers);
        injectField("lblRevenue",        lblRevenue);
        injectField("lblTotalBids",      lblTotalBids);
        injectField("revenueChart",      revenueChart);
        injectField("statusPieChart",    statusPieChart);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reflection helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void injectField(String name, Object value) throws Exception {
        Field f = AdminDashboardController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(String name) throws Exception {
        Field f = AdminDashboardController.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(controller);
    }

    /** Gọi private method 1 tham số AdminStatsDTO */
    private void callWithStats(String methodName, AdminStatsDTO stats) throws Exception {
        Method m = AdminDashboardController.class.getDeclaredMethod(methodName, AdminStatsDTO.class);
        m.setAccessible(true);
        m.invoke(controller, stats);
    }

    /** Gọi private method applyPieColors(String...) */
    private void callApplyPieColors(String... colors) throws Exception {
        Method m = AdminDashboardController.class.getDeclaredMethod("applyPieColors", String[].class);
        m.setAccessible(true);
        m.invoke(controller, (Object) colors);
    }

    /** Gọi private method createNewSlice(String, double) */
    private PieChart.Data callCreateNewSlice(String name, double value) throws Exception {
        Method m = AdminDashboardController.class.getDeclaredMethod("createNewSlice", String.class, double.class);
        m.setAccessible(true);
        return (PieChart.Data) m.invoke(controller, name, value);
    }

    /**
     * Drain FX event queue: đảm bảo tất cả Platform.runLater đã enqueue trước đó
     * được execute trước khi assert.
     */
    private static void flushFxEvents() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        latch.await();
    }

    /**
     * Chạy initialize() — cần mock AdminService vì initialize() gọi refreshData()
     * rồi refreshData() gọi AdminService.getStats().
     */
    private void runInitialize(AdminStatsDTO statsToDeliver) throws Exception {
        try (MockedStatic<AdminService> mocked = Mockito.mockStatic(AdminService.class)) {
            mocked.when(() -> AdminService.getStats(any()))
                    .thenAnswer(inv -> {
                        Consumer<AdminStatsDTO> cb = inv.getArgument(0);
                        cb.accept(statsToDeliver);
                        return null;
                    });
            Method init = AdminDashboardController.class.getDeclaredMethod("initialize");
            init.setAccessible(true);
            init.invoke(controller);
            flushFxEvents(); // drain Platform.runLater từ refreshData callback
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests: createNewSlice
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createNewSlice()")
    class CreateNewSliceTests {

        @Test @DisplayName("Tạo slice với name và value đúng")
        void createsSliceWithCorrectData() throws Exception {
            PieChart.Data slice = callCreateNewSlice("Đang diễn ra (5)", 5.0);
            assertEquals("Đang diễn ra (5)", slice.getName());
            assertEquals(5.0, slice.getPieValue(), 0.001);
        }

        @Test @DisplayName("Tạo slice với value = 0")
        void createsSliceWithZeroValue() throws Exception {
            PieChart.Data slice = callCreateNewSlice("Empty", 0.0);
            assertEquals(0.0, slice.getPieValue(), 0.001);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests: applyPieColors
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("applyPieColors()")
    class ApplyPieColorsTests {

        @Test @DisplayName("Một màu → CSS chứa default-color0 với màu đó")
        void singleColor() throws Exception {
            callApplyPieColors("#FF0000");
            String css = statusPieChart.getStyle();
            assertTrue(css.contains(".default-color0.chart-pie { -fx-pie-color: #FF0000; }"),
                    "CSS phải chứa màu cho slice 0");
            assertTrue(css.contains(".default-color0.chart-legend-item-symbol { -fx-background-color: #FF0000; }"),
                    "CSS phải chứa màu legend cho slice 0");
        }

        @Test @DisplayName("Nhiều màu → CSS chứa tất cả default-colorN")
        void multipleColors() throws Exception {
            callApplyPieColors("#F97316", "#22C55E", "#EF4444", "#F59E0B");
            String css = statusPieChart.getStyle();
            assertTrue(css.contains(".default-color0.chart-pie { -fx-pie-color: #F97316; }"));
            assertTrue(css.contains(".default-color1.chart-pie { -fx-pie-color: #22C55E; }"));
            assertTrue(css.contains(".default-color2.chart-pie { -fx-pie-color: #EF4444; }"));
            assertTrue(css.contains(".default-color3.chart-pie { -fx-pie-color: #F59E0B; }"));
        }

        @Test @DisplayName("Năm màu (endedNoBid > 0) → CSS có default-color4")
        void fiveColors() throws Exception {
            callApplyPieColors("#F97316", "#22C55E", "#EF4444", "#F59E0B", "#6B7280");
            String css = statusPieChart.getStyle();
            assertTrue(css.contains(".default-color4.chart-pie { -fx-pie-color: #6B7280; }"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests: applyCardsData — branch revenue null / không null
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("applyCardsData()")
    class ApplyCardsDataTests {

        @Test @DisplayName("revenue không null → hiển thị giá trị đúng")
        void revenueNotNull() throws Exception {
            AdminStatsDTO stats = new AdminStatsDTO(
                    5, 100, new BigDecimal("1500000"), 200,
                    10, 3, 2, 1);

            callWithStats("applyCardsData", stats);

            assertEquals("5",   lblActiveAuctions.getText());
            assertEquals("100", lblTotalUsers.getText());
            assertEquals("200", lblTotalBids.getText());
            assertTrue(lblRevenue.getText().contains("đ"),
                    "Label doanh thu phải kết thúc bằng ' đ'");
            assertTrue(lblRevenue.getText().contains("1"),
                    "Label doanh thu phải chứa giá trị revenue");
        }

        @Test @DisplayName("revenue null (getTotalRevenue trả ZERO) → hiển thị '0 đ'")
        void revenueNull() throws Exception {
            // AdminStatsDTO.getTotalRevenue() trả BigDecimal.ZERO khi field null
            // → nhánh (revenue != null ? revenue : BigDecimal.ZERO) → ZERO
            AdminStatsDTO stats = new AdminStatsDTO(
                    0, 0, null, 0,
                    0, 0, 0, 0);

            callWithStats("applyCardsData", stats);

            assertTrue(lblRevenue.getText().contains("đ"));
            // Dù revenue null, không được throw NPE
            assertDoesNotThrow(() -> callWithStats("applyCardsData", stats));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests: updateChartsData — tất cả branches
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateChartsData()")
    class UpdateChartsDataTests {

        /**
         * initialize() cần chạy trước để khởi tạo revenueSeries/bidSeries.
         * updateChartsData() đọc trực tiếp từ 2 series này.
         */
        @BeforeEach
        void setupChart() throws Exception {
            runInitialize(AdminStatsDTO.empty());
        }

        @Test
        @DisplayName("empty-state (tất cả = 0) → PieChart có 1 slice placeholder màu xám")
        void emptyState() throws Exception {
            AdminStatsDTO emptyStats = AdminStatsDTO.empty(); // all zeros

            callWithStats("updateChartsData", emptyStats);

            assertEquals(1, statusPieChart.getData().size(),
                    "Empty state phải có đúng 1 slice placeholder");
            assertEquals("Chưa có phiên nào (0)",
                    statusPieChart.getData().get(0).getName());
            assertTrue(statusPieChart.getStyle().contains("#9CA3AF"),
                    "Màu placeholder phải là xám #9CA3AF");
        }

        @Test
        @DisplayName("endedNoBid == 0 → 4 slices, mảng màu 4 phần tử")
        void hasDataNoEndedNoBid() throws Exception {
            AdminStatsDTO stats = new AdminStatsDTO(
                    3, 50, new BigDecimal("5000000"), 100,
                    10, 2, 5, 0); // endedNoBid = 0

            callWithStats("updateChartsData", stats);

            assertEquals(4, statusPieChart.getData().size(),
                    "endedNoBid=0 phải có đúng 4 slices");
            // Màu 4 phần tử → không có default-color4
            assertFalse(statusPieChart.getStyle().contains("default-color4"));
        }

        @Test
        @DisplayName("endedNoBid > 0 → 5 slices, mảng màu 5 phần tử")
        void hasDataWithEndedNoBid() throws Exception {
            AdminStatsDTO stats = new AdminStatsDTO(
                    3, 50, new BigDecimal("5000000"), 100,
                    10, 2, 5, 7); // endedNoBid = 7 > 0

            callWithStats("updateChartsData", stats);

            assertEquals(5, statusPieChart.getData().size(),
                    "endedNoBid>0 phải có đúng 5 slices");
            // Tên slice thứ 5 phải chứa "Không có lượt đặt"
            assertTrue(statusPieChart.getData().get(4).getName().contains("Không có lượt đặt"),
                    "Slice thứ 5 phải là 'Không có lượt đặt'");
            // Màu 5 phần tử → có default-color4
            assertTrue(statusPieChart.getStyle().contains("default-color4"));
        }

        @Test
        @DisplayName("maxRev = 0 và maxBid = 0 → tất cả điểm chart = 0.0 (không NaN/exception)")
        void maxRevAndMaxBidZero() throws Exception {
            // totalRevenue=0, totalBids=0 → revDaily & bidDaily toàn 0 → maxRev=max(0s)=0
            // → nhánh (maxRev > 0 ? ... : 0.0) và (maxBid > 0 ? ... : 0.0)
            AdminStatsDTO stats = new AdminStatsDTO(
                    1, 1, BigDecimal.ZERO, 0,
                    1, 0, 0, 0);

            assertDoesNotThrow(() -> callWithStats("updateChartsData", stats));

            XYChart.Series<String, Number> revSeries = getField("revenueSeries");
            for (XYChart.Data<String, Number> d : revSeries.getData()) {
                assertEquals(0.0, d.getYValue().doubleValue(), 0.001,
                        "Khi maxRev=0, tất cả y-value của revenueSeries phải = 0");
            }
            XYChart.Series<String, Number> bSeries = getField("bidSeries");
            for (XYChart.Data<String, Number> d : bSeries.getData()) {
                assertEquals(0.0, d.getYValue().doubleValue(), 0.001,
                        "Khi maxBid=0, tất cả y-value của bidSeries phải = 0");
            }
        }

        @Test
        @DisplayName("maxRev > 0 và maxBid > 0 → y-value được normalize về 0–100%")
        void normalizesBothSeries() throws Exception {
            AdminStatsDTO stats = new AdminStatsDTO(
                    3, 50, new BigDecimal("7000000"), 50,
                    5, 1, 2, 0);

            callWithStats("updateChartsData", stats);

            // Mỗi series phải có đúng 7 điểm (T2..CN)
            XYChart.Series<String, Number> revSeries = getField("revenueSeries");
            assertEquals(7, revSeries.getData().size());

            // Giá trị tối đa phải ~100.0 (sau normalize)
            double maxY = revSeries.getData().stream()
                    .mapToDouble(d -> d.getYValue().doubleValue())
                    .max().orElse(0);
            assertEquals(100.0, maxY, 1.0,
                    "Sau normalize, giá trị max phải xấp xỉ 100%");
        }

        @Test
        @DisplayName("Tên series được cập nhật đúng sau updateChartsData")
        void seriesNamesUpdated() throws Exception {
            AdminStatsDTO stats = new AdminStatsDTO(
                    1, 1, new BigDecimal("1000000"), 10,
                    1, 0, 0, 0);

            callWithStats("updateChartsData", stats);

            XYChart.Series<String, Number> revSeries = getField("revenueSeries");
            XYChart.Series<String, Number> bSeries   = getField("bidSeries");
            assertEquals("Doanh thu (xu hướng %)",     revSeries.getName());
            assertEquals("Lượt đặt giá (xu hướng %)", bSeries.getName());
        }

        @Test
        @DisplayName("Tất cả loại phiên > 0 nhưng endedNoBid = 0 → tên 4 slices đúng")
        void sliceNamesCorrect() throws Exception {
            AdminStatsDTO stats = new AdminStatsDTO(
                    3, 50, new BigDecimal("1000000"), 20,
                    8, 2, 4, 0);

            callWithStats("updateChartsData", stats);

            assertEquals(4, statusPieChart.getData().size());
            assertTrue(statusPieChart.getData().get(0).getName().contains("Đang diễn ra"));
            assertTrue(statusPieChart.getData().get(1).getName().contains("Đã bán"));
            assertTrue(statusPieChart.getData().get(2).getName().contains("Đã hủy"));
            assertTrue(statusPieChart.getData().get(3).getName().contains("Sắp diễn ra"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests: refreshData — branch stats null / không null
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("refreshData()")
    class RefreshDataTests {

        @BeforeEach
        void setupSeriesViaInit() throws Exception {
            runInitialize(AdminStatsDTO.empty());
        }

        @Test
        @DisplayName("Callback trả null → dùng AdminStatsDTO.empty(), labels không còn '...'")
        void callbackNull() throws Exception {
            try (MockedStatic<AdminService> mocked = Mockito.mockStatic(AdminService.class)) {
                mocked.when(() -> AdminService.getStats(any()))
                        .thenAnswer(inv -> {
                            Consumer<AdminStatsDTO> cb = inv.getArgument(0);
                            cb.accept(null); // FIX: null → safeStats = AdminStatsDTO.empty()
                            return null;
                        });

                Method m = AdminDashboardController.class.getDeclaredMethod("refreshData");
                m.setAccessible(true);
                m.invoke(controller);
                flushFxEvents();

                // Sau khi dùng empty(), labels phải hiện "0" không phải "..."
                assertEquals("0", lblActiveAuctions.getText());
                assertEquals("0", lblTotalUsers.getText());
                assertEquals("0", lblTotalBids.getText());
            }
        }

        @Test
        @DisplayName("Callback trả stats thật → labels hiển thị đúng giá trị từ stats")
        void callbackNotNull() throws Exception {
            AdminStatsDTO real = new AdminStatsDTO(
                    7, 250, new BigDecimal("9000000"), 300,
                    15, 4, 8, 2);

            try (MockedStatic<AdminService> mocked = Mockito.mockStatic(AdminService.class)) {
                mocked.when(() -> AdminService.getStats(any()))
                        .thenAnswer(inv -> {
                            Consumer<AdminStatsDTO> cb = inv.getArgument(0);
                            cb.accept(real); // không null → dùng stats thật
                            return null;
                        });

                Method m = AdminDashboardController.class.getDeclaredMethod("refreshData");
                m.setAccessible(true);
                m.invoke(controller);
                flushFxEvents();

                assertEquals("7",   lblActiveAuctions.getText());
                assertEquals("250", lblTotalUsers.getText());
                assertEquals("300", lblTotalBids.getText());
                assertTrue(lblRevenue.getText().contains("đ"));
            }
        }

        @Test
        @DisplayName("refreshData() đặt labels thành '...' ngay trước khi gọi API")
        void setsLoadingLabelsBeforeApi() throws Exception {
            // Đặt labels về giá trị cũ trước
            lblActiveAuctions.setText("old");
            lblTotalUsers.setText("old");

            try (MockedStatic<AdminService> mocked = Mockito.mockStatic(AdminService.class)) {
                // Mock không gọi callback → labels vẫn ở trạng thái loading
                mocked.when(() -> AdminService.getStats(any())).thenAnswer(inv -> null);

                Method m = AdminDashboardController.class.getDeclaredMethod("refreshData");
                m.setAccessible(true);
                m.invoke(controller);

                // Ngay sau khi gọi (trước khi callback chạy), labels phải là "..."
                assertEquals("...", lblActiveAuctions.getText());
                assertEquals("...", lblTotalUsers.getText());
                assertEquals("... đ", lblRevenue.getText());
                assertEquals("...", lblTotalBids.getText());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests: initialize — full flow
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("initialize()")
    class InitializeTests {

        @Test
        @DisplayName("initialize() khởi tạo đủ 2 series với 7 điểm zero cho mỗi series")
        void initializesSeriesWithZeroData() throws Exception {
            runInitialize(AdminStatsDTO.empty());

            XYChart.Series<String, Number> revSeries = getField("revenueSeries");
            XYChart.Series<String, Number> bSeries   = getField("bidSeries");

            assertNotNull(revSeries, "revenueSeries phải được khởi tạo");
            assertNotNull(bSeries,   "bidSeries phải được khởi tạo");
            assertEquals(7, revSeries.getData().size(), "revenueSeries phải có 7 điểm (T2..CN)");
            assertEquals(7, bSeries.getData().size(),   "bidSeries phải có 7 điểm (T2..CN)");

            // Tên ngày đúng thứ tự
            String[] days = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
            for (int i = 0; i < days.length; i++) {
                assertEquals(days[i], revSeries.getData().get(i).getXValue(),
                        "Ngày thứ " + i + " phải là " + days[i]);
            }
        }

        @Test
        @DisplayName("initialize() thêm cả 2 series vào revenueChart")
        void addsBothSeriesToChart() throws Exception {
            runInitialize(AdminStatsDTO.empty());
            assertEquals(2, revenueChart.getData().size(),
                    "revenueChart phải có đúng 2 series sau initialize()");
        }

        @Test
        @DisplayName("initialize() tắt animation của cả 2 chart")
        void disablesAnimations() throws Exception {
            runInitialize(AdminStatsDTO.empty());
            assertFalse(revenueChart.getAnimated(),   "revenueChart animation phải tắt");
            assertFalse(statusPieChart.getAnimated(), "statusPieChart animation phải tắt");
        }

        @Test
        @DisplayName("initialize() với stats thật → labels hiển thị đúng sau flush")
        void initWithRealStats() throws Exception {
            AdminStatsDTO stats = new AdminStatsDTO(
                    4, 80, new BigDecimal("3000000"), 120,
                    6, 1, 3, 0);

            runInitialize(stats);

            assertEquals("4",   lblActiveAuctions.getText());
            assertEquals("80",  lblTotalUsers.getText());
            assertEquals("120", lblTotalBids.getText());
        }
    }
}