package vn.edu.vnu.uet.group8.client.service;

import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.client.util.SessionManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SellerService (client-side validation)")
class SellerServiceTest {

    @BeforeEach void clear() { SessionManager.clearSession(); }

    private SellerService.CreateItemRequest sampleRequest() {
        return new SellerService.CreateItemRequest(
                "iPhone 15",
                "ELECTRONICS",
                "NEW",
                "Mô tả sản phẩm",
                new BigDecimal("20000000"),
                new BigDecimal("500000"),
                24,
                Map.of("brand", "Apple"),
                List.of(),
                false,
                null,
                null
        );
    }

    @Nested @DisplayName("createItem – no connection")
    class CreateItem {
        @Test @DisplayName("không kết nối -> onFailure được gọi")
        void noConnectionCallsFailure() {
            List<String> errors = new ArrayList<>();
            SellerService.createItem(sampleRequest(), item -> {}, errors::add);
            assertFalse(errors.isEmpty());
            assertTrue(errors.get(0).contains("kết nối"));
        }

        @Test @DisplayName("không kết nối -> onResult KHÔNG được gọi")
        void noConnectionDoesNotCallOnResult() {
            AtomicBoolean called = new AtomicBoolean(false);
            SellerService.createItem(sampleRequest(), item -> called.set(true), e -> {});
            assertFalse(called.get());
        }
    }

    @Nested @DisplayName("getMyListings – no connection")
    class GetMyListings {
        @Test @DisplayName("không kết nối -> onFailure được gọi")
        void noConnectionCallsFailure() {
            List<String> errors = new ArrayList<>();
            SellerService.getMyListings(items -> {}, errors::add);
            assertFalse(errors.isEmpty());
        }
    }

    @Nested @DisplayName("CreateItemRequest – construction")
    class CreateItemRequestConstruction {
        @Test @DisplayName("tạo request đầy đủ không ném ngoại lệ")
        void constructionNoException() {
            assertDoesNotThrow(() -> sampleRequest());
        }

        @Test @DisplayName("fields được gán đúng")
        void fieldsAssigned() {
            SellerService.CreateItemRequest req = sampleRequest();
            assertEquals("iPhone 15", req.name);
            assertEquals("ELECTRONICS", req.category);
            assertEquals("NEW", req.condition);
            assertEquals(new BigDecimal("20000000"), req.startPrice);
            assertEquals(24, req.durationHours);
        }
    }
}