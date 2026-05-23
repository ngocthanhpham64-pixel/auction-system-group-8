package vn.edu.vnu.uet.group8.client.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.edu.vnu.uet.group8.client.TestFXSetup;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClientModel")
class ClientModelTest extends TestFXSetup {

    private final ClientModel model = ClientModel.getInstance();

    @Nested
    @DisplayName("Singleton")
    class Singleton {

        @Test
        @DisplayName("getInstance luôn trả cùng instance")
        void sameInstance() {
            ClientModel m1 = ClientModel.getInstance();
            ClientModel m2 = ClientModel.getInstance();

            assertSame(m1, m2);
        }
    }

    @Nested
    @DisplayName("Properties")
    class Properties {

        @Test
        void currentUserPropertyNotNull() {
            assertNotNull(model.currentUserProperty());
        }

        @Test
        void auctionItemsPropertyNotNull() {
            assertNotNull(model.auctionItemsProperty());
        }

        @Test
        void notificationsPropertyNotNull() {
            assertNotNull(model.notificationsProperty());
        }

        @Test
        void favoriteItemsPropertyNotNull() {
            assertNotNull(model.favoriteItemsProperty());
        }

        @Test
        void unreadNotificationCountPropertyNotNull() {
            assertNotNull(model.unreadNotificationCountProperty());
        }

        @Test
        void loggedInPropertyNotNull() {
            assertNotNull(model.loggedInProperty());
        }

        @Test
        void currentAuctionItemPropertyNotNull() {
            assertNotNull(model.currentAuctionItemProperty());
        }

        @Test
        void balancePropertyNotNull() {
            assertNotNull(model.balanceProperty());
        }

        @Test
        void favCountPropertyNotNull() {
            assertNotNull(model.favCountProperty());
        }
    }

    @Nested
    @DisplayName("Default state")
    class DefaultState {

        @Test
        void balanceNotNull() {
            assertNotNull(model.getBalance());
        }

        @Test
        void auctionItemsNotNull() {
            assertNotNull(model.getAuctionItems());
        }

        @Test
        void notificationsNotNull() {
            assertNotNull(model.getNotifications());
        }

        @Test
        void favoriteItemsNotNull() {
            assertNotNull(model.getFavoriteItems());
        }

        @Test
        void unreadCountNonNegative() {
            assertTrue(model.getUnreadNotificationCount() >= 0);
        }

        @Test
        void favoriteCountNonNegative() {
            assertTrue(model.getFavCount() >= 0);
        }
    }

    @Nested
    @DisplayName("Favorite operations")
    class FavoriteOperations {

        @Test
        void isFavoriteReturnsFalseWhenEmpty() {
            assertFalse(model.isFavorite(999));
        }

        @Test
        void addFavoriteNullDoesNotThrow() {
            assertDoesNotThrow(() -> model.addFavorite(null));
        }

        @Test
        void removeFavoriteNullDoesNotThrow() {
            assertDoesNotThrow(() -> model.removeFavorite(null));
        }
    }

    @Nested
    @DisplayName("clearSession")
    class ClearSession {

        @Test
        void clearSessionDoesNotThrow() {
            assertDoesNotThrow(model::clearSession);
        }
    }
}