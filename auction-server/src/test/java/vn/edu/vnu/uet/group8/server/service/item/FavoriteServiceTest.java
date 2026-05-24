package vn.edu.vnu.uet.group8.server.service.item;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;
import vn.edu.vnu.uet.group8.server.dao.FavoriteDAO;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock private FavoriteDAO favoriteDAO;
    @Mock private ItemQueryService itemQueryService;

    private FavoriteService service;

    @BeforeEach
    void setUp() {
        service = new FavoriteService(favoriteDAO, itemQueryService);
    }

    private AuctionItemDTO buildDto(int itemId) {
        return AuctionItemDTO.of(itemId, "Item " + itemId, "Mô tả",
                ItemCategory.ELECTRONICS, ItemCondition.NEW, SessionStatus.ACTIVE,
                new BigDecimal("1000000"),
                Instant.now().plus(1, ChronoUnit.HOURS),
                "seller", Collections.emptyMap(), Collections.emptyList(),
                0, Instant.now());
    }

    // ─────────────────────────────────────────────────────────────
    // addFavorite
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("addFavorite()")
    class AddFavoriteTest {

        @Test
        @DisplayName("Gọi DAO đúng tham số")
        void goiDAODung() throws SQLException {
            service.addFavorite(3, 10);
            verify(favoriteDAO).addFavorite(3, 10);
        }

        @Test
        @DisplayName("Ném SQLException khi DAO lỗi")
        void nemSQLException() throws SQLException {
            doThrow(new SQLException("lỗi")).when(favoriteDAO).addFavorite(anyInt(), anyInt());
            assertThrows(SQLException.class, () -> service.addFavorite(1, 1));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // removeFavorite
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("removeFavorite()")
    class RemoveFavoriteTest {

        @Test
        @DisplayName("Gọi DAO đúng tham số")
        void goiDAODung() throws SQLException {
            service.removeFavorite(5, 20);
            verify(favoriteDAO).removeFavorite(5, 20);
        }

        @Test
        @DisplayName("Ném SQLException khi DAO lỗi")
        void nemSQLException() throws SQLException {
            doThrow(new SQLException("lỗi")).when(favoriteDAO).removeFavorite(anyInt(), anyInt());
            assertThrows(SQLException.class, () -> service.removeFavorite(1, 1));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getFavoriteItems
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getFavoriteItems()")
    class GetFavoriteItemsTest {

        @Test
        @DisplayName("Trả về danh sách item từ các itemId yêu thích")
        void traVeDanhSachItem() throws Exception {
            when(favoriteDAO.getFavoriteItemIds(1)).thenReturn(List.of(10, 20, 30));
            when(itemQueryService.getItemDetail(10)).thenReturn(buildDto(10));
            when(itemQueryService.getItemDetail(20)).thenReturn(buildDto(20));
            when(itemQueryService.getItemDetail(30)).thenReturn(buildDto(30));

            List<AuctionItemDTO> result = service.getFavoriteItems(1);

            assertEquals(3, result.size());
            assertEquals(10, result.get(0).getItemId());
        }

        @Test
        @DisplayName("Trả về list rỗng khi không có item yêu thích")
        void traVeRongKhiKhongCoYeuThich() throws SQLException {
            when(favoriteDAO.getFavoriteItemIds(anyInt())).thenReturn(Collections.emptyList());

            List<AuctionItemDTO> result = service.getFavoriteItems(99);

            assertTrue(result.isEmpty());
            verifyNoInteractions(itemQueryService);
        }

        @Test
        @DisplayName("Bỏ qua item lỗi - vẫn trả các item còn lại")
        void boQuaItemLoi() throws Exception {
            when(favoriteDAO.getFavoriteItemIds(1)).thenReturn(List.of(10, 99, 20));
            when(itemQueryService.getItemDetail(10)).thenReturn(buildDto(10));
            when(itemQueryService.getItemDetail(99)).thenThrow(new RuntimeException("item not found"));
            when(itemQueryService.getItemDetail(20)).thenReturn(buildDto(20));

            List<AuctionItemDTO> result = service.getFavoriteItems(1);

            // item 99 bị lỗi → bỏ qua, còn lại 2
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("Tất cả item lỗi → trả list rỗng")
        void tatCaItemLoi() throws Exception {
            when(favoriteDAO.getFavoriteItemIds(1)).thenReturn(List.of(1, 2, 3));
            when(itemQueryService.getItemDetail(anyInt())).thenThrow(new RuntimeException("lỗi"));

            List<AuctionItemDTO> result = service.getFavoriteItems(1);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Ném SQLException khi getFavoriteItemIds lỗi")
        void nemSQLException() throws SQLException {
            when(favoriteDAO.getFavoriteItemIds(anyInt())).thenThrow(new SQLException("DB lỗi"));

            assertThrows(SQLException.class, () -> service.getFavoriteItems(1));
        }
    }
}