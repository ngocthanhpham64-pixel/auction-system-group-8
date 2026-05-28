package vn.edu.vnu.uet.group8.client.model;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.client.TestFXSetup;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionStatusDTO;
import vn.edu.vnu.uet.group8.common.dto.model.NotificationDTO;
import vn.edu.vnu.uet.group8.common.dto.response.LoginResponse;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClientModel")
class ClientModelTest extends TestFXSetup {

    @BeforeAll
    static void initToolkit() {
        new JFXPanel();
    }

    private ClientModel model;

    @BeforeEach
    void setup() {
        model = ClientModel.getInstance();
        model.clearSession();
        waitFx();
    }

    private static void waitFx() {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(latch::countDown);

        try {
            assertTrue(latch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("FX thread interrupted");
        }
    }

    private AuctionItemDTO item(int id) {
        return AuctionItemDTO.of(
                id,
                "Item " + id,
                "Test item",
                ItemCategory.ELECTRONICS,
                null,
                null,
                new BigDecimal("1000000"),
                null,
                "seller",
                null,
                null,
                0,
                null
        );
    }

    private AuctionStatusDTO status(int itemId, BigDecimal price) {
        return new AuctionStatusDTO(
                itemId,
                price,
                Instant.now().plusSeconds(3600),
                List.of()
        );
    }

    private NotificationDTO notif(int id, boolean read) {
        return NotificationDTO.builder()
                .id(id)
                .title("Notif " + id)
                .isRead(read)
                .type("AUCTION")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void setCurrentUser_valid() {
        LoginResponse lr = LoginResponse.success(
                1,
                "user1",
                "User One",
                "user1@email.com",
                "Member",
                "tok"
        );

        model.setCurrentUser(lr);
        waitFx();

        assertNotNull(model.getCurrentUser());
    }

    @Test
    void setAuctionItems_valid() {
        model.setAuctionItems(List.of(item(1), item(2)));
        waitFx();

        assertEquals(2, model.getAuctionItems().size());
    }

    @Test
    void replaceAuctionItems() {
        model.setAuctionItems(List.of(item(1), item(2)));
        waitFx();

        model.setAuctionItems(List.of(item(3)));
        waitFx();

        assertEquals(1, model.getAuctionItems().size());
    }

    @Test
    void setNotifications_valid() {
        model.setNotifications(List.of(notif(1, false), notif(2, true)));
        waitFx();

        assertEquals(2, model.getNotifications().size());
    }

    @Test
    void setFavoriteItems_valid() {
        model.setFavoriteItems(List.of(item(10), item(20)));
        waitFx();

        assertEquals(2, model.getFavoriteItems().size());
    }

    @Test
    void addFavorite_thenIsFavorite() {
        model.addFavorite(item(5));
        waitFx();

        assertTrue(model.isFavorite(5));
    }

    @Test
    void addFavorite_duplicateNotAdded() {
        model.addFavorite(item(5));
        waitFx();

        model.addFavorite(item(5));
        waitFx();

        assertEquals(1, model.getFavCount());
    }

    @Test
    void removeFavorite_success() {
        model.addFavorite(item(5));
        waitFx();

        model.removeFavorite(item(5));
        waitFx();

        assertFalse(model.isFavorite(5));
    }

    @Test
    void updateItemCurrentPrice_updatesAuctionItem() {
        model.setAuctionItems(List.of(item(1)));
        waitFx();

        model.updateItemCurrentPrice(1, new BigDecimal("2000000"));
        waitFx();

        assertEquals(
                new BigDecimal("2000000"),
                model.getAuctionItems().get(0).getCurrentPrice()
        );
    }

    @Test
    void updateItemCurrentPrice_updatesFavoriteItem() {
        model.setFavoriteItems(List.of(item(2)));
        waitFx();

        model.updateItemCurrentPrice(2, new BigDecimal("3000000"));
        waitFx();

        assertEquals(
                new BigDecimal("3000000"),
                model.getFavoriteItems().get(0).getCurrentPrice()
        );
    }

    @Test
    void updateItemCurrentPrice_updatesStatus() {
        model.setCurrentAuctionStatus(status(1, BigDecimal.ONE));
        waitFx();

        model.updateItemCurrentPrice(1, new BigDecimal("5000000"));
        waitFx();

        assertEquals(
                new BigDecimal("5000000"),
                model.getCurrentAuctionStatus().getCurrentPrice()
        );
    }

    @Test
    void updateBalance_valid() {
        model.updateBalance(new BigDecimal("5000000"));
        waitFx();

        assertEquals(
                new BigDecimal("5000000"),
                model.getBalance()
        );
    }

    @Test
    void addNotification_sizeIncrease() {
        model.setUnreadNotificationCount(0);
        waitFx();

        model.addNotification(notif(99, false));
        waitFx();

        assertEquals(1, model.getNotifications().size());
    }

    @Test
    void addNotification_unreadIncrease() {
        model.setUnreadNotificationCount(2);
        waitFx();

        model.addNotification(notif(100, false));
        waitFx();

        assertEquals(3, model.getUnreadNotificationCount());
    }

    @Test
    void markNotificationRead_success() {
        model.setNotifications(List.of(notif(1, false)));
        waitFx();

        model.setUnreadNotificationCount(1);
        waitFx();

        model.markNotificationRead(1);
        waitFx();

        assertEquals(0, model.getUnreadNotificationCount());
    }

    @Test
    void setCurrentAuctionItem_valid() {
        model.setCurrentAuctionItem(item(7));
        waitFx();

        assertEquals(7, model.getCurrentAuctionItem().getItemId());
    }

    @Test
    void setCurrentAuctionStatus_valid() {
        model.setCurrentAuctionStatus(status(1, BigDecimal.ONE));
        waitFx();

        assertNotNull(model.getCurrentAuctionStatus());
    }

    @Test
    void searchQuery_valid() {
        model.setSearchQuery("iphone");
        waitFx();

        assertEquals("iphone", model.getSearchQuery());
    }

    @Test
    void searchQuery_blank() {
        model.setSearchQuery("   ");
        waitFx();

        assertEquals("   ", model.getSearchQuery());
    }

    @Test
    void setLoggedIn_true() {
        model.setLoggedIn(true);
        waitFx();

        assertTrue(model.isLoggedIn());
    }

    @Test
    void unreadCount_valid() {
        model.setUnreadNotificationCount(5);
        waitFx();

        assertEquals(5, model.getUnreadNotificationCount());
    }

    @Test
    void clearSession_success() {
        model.setCurrentUser(
                LoginResponse.success(
                        1,
                        "user",
                        "User",
                        "u@email.com",
                        "Member",
                        "tok"
                )
        );

        model.setAuctionItems(List.of(item(1)));
        model.setNotifications(List.of(notif(1, false)));
        model.addFavorite(item(1));
        model.setUnreadNotificationCount(5);
        model.setLoggedIn(true);
        model.setSearchQuery("iphone");
        model.setCurrentAuctionItem(item(1));
        model.setCurrentAuctionStatus(status(1, BigDecimal.ONE));

        waitFx();

        model.clearSession();
        waitFx();

        assertNull(model.getCurrentUser());
        assertTrue(model.getAuctionItems().isEmpty());
        assertTrue(model.getNotifications().isEmpty());
        assertTrue(model.getFavoriteItems().isEmpty());
        assertEquals(0, model.getUnreadNotificationCount());
        assertFalse(model.isLoggedIn());
        assertEquals("", model.getSearchQuery());
        assertNull(model.getCurrentAuctionItem());
        assertNull(model.getCurrentAuctionStatus());
    }
}
