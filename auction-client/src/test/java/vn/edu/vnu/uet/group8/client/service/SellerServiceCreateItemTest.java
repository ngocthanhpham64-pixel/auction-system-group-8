package vn.edu.vnu.uet.group8.client.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.vnu.uet.group8.client.util.SessionManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SellerService} focusing on:
 * - CreateItemRequest construction & field validation
 * - createItem: no-connection guard, null callbacks
 * - updateItem: no-connection guard
 * - deleteItem: no-connection guard, boundary itemId
 * - getMyListings: auth guard, no-connection guard
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class SellerServiceCreateItemTest {

    @BeforeEach
    void reset() { SessionManager.clearSession(); }

    // ══════════════════════════════════════════════════════
    // CreateItemRequest — construction
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("CreateItemRequest: khởi tạo đầy đủ → các field được gán đúng")
    void createItemRequest_fullConstruction_fieldsCorrect() {
        SellerService.CreateItemRequest req = new SellerService.CreateItemRequest(
                "Đồng hồ Rolex", "WATCHES", "NEW",
                "Mô tả chi tiết", BigDecimal.valueOf(50_000_000),
                BigDecimal.valueOf(1_000_000), 48,
                Map.of("brand", "Rolex", "model", "Submariner"),
                List.of("http://img1.jpg", "http://img2.jpg"),
                true, "GIA Laboratories", "GIA-12345"
        );

        assertEquals("Đồng hồ Rolex",        req.name);
        assertEquals("WATCHES",               req.category);
        assertEquals("NEW",                   req.condition);
        assertEquals("Mô tả chi tiết",        req.description);
        assertEquals(0, BigDecimal.valueOf(50_000_000).compareTo(req.startPrice));
        assertEquals(0, BigDecimal.valueOf(1_000_000).compareTo(req.bidStep));
        assertEquals(48,                      req.durationHours);
        assertTrue(req.hasCert);
        assertEquals("GIA Laboratories",      req.certBody);
        assertEquals("GIA-12345",             req.certId);
        assertEquals(2,                       req.imageUrls.size());
    }

    @Test
    @DisplayName("CreateItemRequest: không có cert → hasCert false")
    void createItemRequest_noCert_hasCertFalse() {
        SellerService.CreateItemRequest req = new SellerService.CreateItemRequest(
                "Item", "OTHER", "USED", "Desc",
                BigDecimal.valueOf(100_000), BigDecimal.valueOf(10_000), 24,
                null, null, false, null, null
        );
        assertFalse(req.hasCert);
        assertNull(req.certBody);
        assertNull(req.certId);
    }

    @Test
    @DisplayName("CreateItemRequest: specs null → không crash")
    void createItemRequest_nullSpecs_noCrash() {
        assertDoesNotThrow(() -> new SellerService.CreateItemRequest(
                "X", "OTHER", "USED", "Desc",
                BigDecimal.ONE, BigDecimal.ONE, 1,
                null, List.of(), false, null, null
        ));
    }

    @Test
    @DisplayName("CreateItemRequest: imageUrls null → không crash")
    void createItemRequest_nullImageUrls_noCrash() {
        assertDoesNotThrow(() -> new SellerService.CreateItemRequest(
                "X", "OTHER", "USED", "Desc",
                BigDecimal.ONE, BigDecimal.ONE, 1,
                Map.of(), null, false, null, null
        ));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 6, 12, 24, 48, 72, 168})
    @DisplayName("CreateItemRequest: durationHours các giá trị hợp lệ → không crash")
    void createItemRequest_validDurations_noCrash(int hours) {
        assertDoesNotThrow(() -> new SellerService.CreateItemRequest(
                "X", "OTHER", "USED", "Desc",
                BigDecimal.valueOf(100_000), BigDecimal.valueOf(10_000), hours,
                null, null, false, null, null
        ));
    }

    // ══════════════════════════════════════════════════════
    // createItem — no-connection guard
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("createItem: không kết nối → onFailure 'Không có kết nối server'")
    void createItem_noConnection_callsOnFailure() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        SellerService.CreateItemRequest req = minimalRequest();
        AtomicReference<String> error = new AtomicReference<>();
        SellerService.createItem(req, item -> fail("Không được gọi"), error::set);
        assertNotNull(error.get());
        assertFalse(error.get().isBlank());
    }

    @Test
    @DisplayName("createItem: onResult null, không kết nối → không crash")
    void createItem_nullOnResult_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        assertDoesNotThrow(() ->
                SellerService.createItem(minimalRequest(), null, err -> {}));
    }

    @Test
    @DisplayName("createItem: onFailure null, không kết nối → không crash")
    void createItem_nullOnFailure_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        assertDoesNotThrow(() ->
                SellerService.createItem(minimalRequest(), item -> {}, null));
    }

    // ══════════════════════════════════════════════════════
    // updateItem — no-connection guard
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("updateItem: không kết nối → onResult(false)")
    void updateItem_noConnection_returnsFalse() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        AtomicBoolean result = new AtomicBoolean(true);
        SellerService.updateItem(1, minimalRequest(), result::set);
        assertFalse(result.get());
    }

    @Test
    @DisplayName("updateItem: onResult null → không crash")
    void updateItem_nullCallback_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        assertDoesNotThrow(() ->
                SellerService.updateItem(1, minimalRequest(), null));
    }

    @Test
    @DisplayName("updateItem: itemId = 0 → không crash")
    void updateItem_zeroItemId_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        assertDoesNotThrow(() ->
                SellerService.updateItem(0, minimalRequest(), r -> {}));
    }

    // ══════════════════════════════════════════════════════
    // deleteItem — boundary itemId
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("deleteItem: không kết nối → onResult(false)")
    void deleteItem_noConnection_returnsFalse() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        AtomicBoolean result = new AtomicBoolean(true);
        SellerService.deleteItem(5, result::set);
        assertFalse(result.get());
    }

    @Test
    @DisplayName("deleteItem: onResult null → không crash")
    void deleteItem_nullCallback_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        assertDoesNotThrow(() -> SellerService.deleteItem(1, null));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -999, Integer.MIN_VALUE})
    @DisplayName("deleteItem: itemId <= 0 → không crash")
    void deleteItem_invalidItemId_noCrash(int itemId) {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        assertDoesNotThrow(() -> SellerService.deleteItem(itemId, r -> {}));
    }

    // ══════════════════════════════════════════════════════
    // getMyListings — auth + connection guard
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("getMyListings: chưa login → onFailure được gọi")
    void getMyListings_notLoggedIn_callsOnFailure() {
        AtomicReference<String> error = new AtomicReference<>();
        SellerService.getMyListings(
                list -> fail("onResult không được gọi"),
                error::set);
        assertNotNull(error.get());
        assertFalse(error.get().isBlank());
    }

    @Test
    @DisplayName("getMyListings: đã login, không kết nối → onFailure được gọi")
    void getMyListings_loggedIn_noConnection_callsOnFailure() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        AtomicReference<String> error = new AtomicReference<>();
        SellerService.getMyListings(list -> fail("Không được gọi"), error::set);
        assertNotNull(error.get());
    }

    @Test
    @DisplayName("getMyListings: onFailure null, chưa login → không crash")
    void getMyListings_nullOnFailure_noCrash() {
        assertDoesNotThrow(() -> SellerService.getMyListings(list -> {}, null));
    }

    @Test
    @DisplayName("getMyListings: onResult null, đã login → không crash")
    void getMyListings_nullOnResult_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        assertDoesNotThrow(() -> SellerService.getMyListings(null, err -> {}));
    }

    // ══════════════════════════════════════════════════════
    // Helper
    // ══════════════════════════════════════════════════════

    private SellerService.CreateItemRequest minimalRequest() {
        return new SellerService.CreateItemRequest(
                "Test Item", "OTHER", "USED", "Description",
                BigDecimal.valueOf(100_000), BigDecimal.valueOf(10_000), 24,
                Map.of(), List.of(), false, null, null
        );
    }
}