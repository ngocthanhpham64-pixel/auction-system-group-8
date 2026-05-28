package vn.edu.vnu.uet.group8.server.service.auction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.common.entity.AutoBidConfig;
import vn.edu.vnu.uet.group8.server.dao.AutoBidDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;

@ExtendWith(MockitoExtension.class)
class AutoBidServiceTest {

    @Mock private AutoBidDAO autoBidDAO;
    @Mock private BidValidator validator;
    @Mock private HybridBidExecutor executor;
    @Mock private UserDAO userDAO;

    private AutoBidService service;

    @BeforeEach
    void setUp() {
        service = new AutoBidService(autoBidDAO, validator, executor, userDAO);
    }

    @Test
    @DisplayName("hasActiveAutoBid - gọi tới autoBidDAO")
    void testHasActiveAutoBid() throws SQLException {
        when(autoBidDAO.hasActiveAutoBid(1, 10)).thenReturn(true);
        assertTrue(service.hasActiveAutoBid(1, 10));
        verify(autoBidDAO).hasActiveAutoBid(1, 10);
    }

    @Test
    @DisplayName("cancelAutoBid - hủy đúng cấu hình của user")
    void testCancelAutoBid() throws SQLException {
        AutoBidConfig config1 = AutoBidConfig.builder()
                .userId(1)
                .sessionId(10)
                .maxPrice(new BigDecimal("1000"))
                .build();
        config1.assignId(100);
        AutoBidConfig config2 = AutoBidConfig.builder()
                .userId(2)
                .sessionId(10)
                .maxPrice(new BigDecimal("2000"))
                .build();
        config2.assignId(101);

        when(autoBidDAO.findActiveBySession(10)).thenReturn(List.of(config1, config2));

        service.cancelAutoBid(1, 10);

        verify(autoBidDAO).deactivate(100);
        verify(autoBidDAO, never()).deactivate(101);
    }

    @Test
    @DisplayName("saveConfig - lưu cấu hình thành công")
    void testSaveConfig() throws SQLException {
        service.saveConfig(1, 10, new BigDecimal("1000"));
        verify(autoBidDAO).saveConfig(any(AutoBidConfig.class));
    }
}