package vn.edu.vnu.uet.group8.server.network;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.common.dto.model.NotificationDTO;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.enums.NotificationType;
import vn.edu.vnu.uet.group8.server.service.auction.AuctionEventBus;
import vn.edu.vnu.uet.group8.server.service.auction.event.AuctionEndedEvent;
import vn.edu.vnu.uet.group8.server.service.auction.event.BidPlacedEvent;
import vn.edu.vnu.uet.group8.server.service.user.NotificationService;

@ExtendWith(MockitoExtension.class)
class AuctionEventSubscriberTest {

    @Mock private BroadcastChannel channel;
    @Mock private NotificationService notificationService;
    @Mock private AuctionEventBus eventBus;

    private AuctionEventSubscriber subscriber;

    @BeforeEach
    void setUp() {
        subscriber = new AuctionEventSubscriber(channel, notificationService);
    }

    // =========================================================
    // registerTo
    // =========================================================

    @Test
    @DisplayName("registerTo() đăng ký 2 event handler vào EventBus")
    void registerTo() {

        subscriber.registerTo(eventBus);

        verify(eventBus)
                .subscribe(eq(BidPlacedEvent.class), any());

        verify(eventBus)
                .subscribe(eq(AuctionEndedEvent.class), any());
    }

    // =========================================================
    // onBidPlaced
    // =========================================================

    @Nested
    @DisplayName("onBidPlaced()")
    class OnBidPlacedTest {

        private BidPlacedEvent buildEvent(Integer prevBidderId) {

            return new BidPlacedEvent(
                    10,
                    3,
                    "bidder01",
                    new BigDecimal("2000000"),
                    4,
                    Instant.now().plus(1, ChronoUnit.HOURS),
                    false,
                    prevBidderId
            );
        }

        @SuppressWarnings("unchecked")
        private AuctionEventBus.EventListener<BidPlacedEvent> getBidHandler() {

            subscriber.registerTo(eventBus);

            ArgumentCaptor<AuctionEventBus.EventListener<BidPlacedEvent>> captor =
                    ArgumentCaptor.forClass((Class) AuctionEventBus.EventListener.class);

            verify(eventBus)
                    .subscribe(eq(BidPlacedEvent.class), captor.capture());

            return captor.getValue();
        }

        @Test
        @DisplayName("BidPlacedEvent → broadcast PRICE_UPDATE")
        void broadcastPriceUpdate() throws Exception {

            AuctionEventBus.EventListener<BidPlacedEvent> handler =
                    getBidHandler();

            handler.onEvent(buildEvent(null));

            ArgumentCaptor<ServerResponse> respCaptor =
                    ArgumentCaptor.forClass(ServerResponse.class);

            verify(channel).broadcast(respCaptor.capture());

            assertEquals(
                    "PRICE_UPDATE",
                    respCaptor.getValue().getMessage()
            );
        }

        @Test
        @DisplayName("prevBidderId != bidderId → tạo OUTBID notification + sendToUser")
        void outbidNotification() throws Exception {

            NotificationDTO notifDto =
                    NotificationDTO.builder()
                            .id(1)
                            .userId(7)
                            .title("T")
                            .message("M")
                            .type("OUTBID")
                            .isRead(false)
                            .createdAt(Instant.now())
                            .build();

            when(notificationService.createNotification(
                    eq(7),
                    anyString(),
                    anyString(),
                    eq(NotificationType.OUTBID)
            )).thenReturn(notifDto);

            AuctionEventBus.EventListener<BidPlacedEvent> handler =
                    getBidHandler();

            handler.onEvent(buildEvent(7));

            verify(notificationService)
                    .createNotification(
                            eq(7),
                            contains("vượt giá"),
                            anyString(),
                            eq(NotificationType.OUTBID)
                    );

            verify(channel)
                    .sendToUser(eq(7), any(ServerResponse.class));
        }

        @Test
        @DisplayName("prevBidderId == bidderId → KHÔNG tạo OUTBID notification")
        void sameBidderNoOutbid() throws Exception {

            AuctionEventBus.EventListener<BidPlacedEvent> handler =
                    getBidHandler();

            handler.onEvent(buildEvent(3));

            verify(notificationService, never())
                    .createNotification(
                            anyInt(),
                            anyString(),
                            anyString(),
                            any()
                    );

            verify(channel, never())
                    .sendToUser(anyInt(), any());
        }

        @Test
        @DisplayName("prevBidderId null → KHÔNG tạo OUTBID notification")
        void noPrevBidderNoOutbid() throws Exception {

            AuctionEventBus.EventListener<BidPlacedEvent> handler =
                    getBidHandler();

            handler.onEvent(buildEvent(null));

            verify(notificationService, never())
                    .createNotification(
                            anyInt(),
                            anyString(),
                            anyString(),
                            any()
                    );
        }

        @Test
        @DisplayName("notificationService lỗi khi tạo OUTBID → không ném, broadcast vẫn xảy ra")
        void notifServiceErrorSilent() throws Exception {

            when(notificationService.createNotification(
                    anyInt(),
                    anyString(),
                    anyString(),
                    any()
            )).thenThrow(new RuntimeException("DB lỗi"));

            AuctionEventBus.EventListener<BidPlacedEvent> handler =
                    getBidHandler();

            assertDoesNotThrow(() ->
                    handler.onEvent(buildEvent(7)));

            verify(channel).broadcast(any());
        }

        @Test
        @DisplayName("extended=true vẫn broadcast bình thường")
        void extendedBroadcast() throws Exception {

            AuctionEventBus.EventListener<BidPlacedEvent> handler =
                    getBidHandler();

            BidPlacedEvent event =
                    new BidPlacedEvent(
                            10,
                            3,
                            "u",
                            new BigDecimal("3000000"),
                            5,
                            Instant.now().plus(10, ChronoUnit.MINUTES),
                            true,
                            null
                    );

            handler.onEvent(event);

            verify(channel).broadcast(any());
        }
    }

    // =========================================================
    // onAuctionEnded
    // =========================================================

    @Nested
    @DisplayName("onAuctionEnded()")
    class OnAuctionEndedTest {

        private AuctionEndedEvent buildSoldEvent(int winnerId) {

            return AuctionEndedEvent.sold(
                    10,
                    "iPhone 17 Pro",
                    new BigDecimal("25000000"),
                    winnerId,
                    "winner_user"
            );
        }

        private AuctionEndedEvent buildNoBidEvent() {

            return AuctionEndedEvent.noBid(
                    10,
                    "Đồng hồ cổ"
            );
        }

        @SuppressWarnings("unchecked")
        private AuctionEventBus.EventListener<AuctionEndedEvent>
        getEndedHandler() {

            subscriber.registerTo(eventBus);

            ArgumentCaptor<AuctionEventBus.EventListener<AuctionEndedEvent>> captor =
                    ArgumentCaptor.forClass((Class) AuctionEventBus.EventListener.class);

            verify(eventBus)
                    .subscribe(eq(AuctionEndedEvent.class), captor.capture());

            return captor.getValue();
        }

        @Test
        @DisplayName("SOLD event → broadcast AUCTION_ENDED với message chứa giá")
        void soldBroadcast() throws Exception {

            var handler = getEndedHandler();

            handler.onEvent(buildSoldEvent(7));

            ArgumentCaptor<ServerResponse> captor =
                    ArgumentCaptor.forClass(ServerResponse.class);

            verify(channel).broadcast(captor.capture());

            assertTrue(
                    captor.getValue()
                            .getMessage()
                            .contains("25000000")
            );
        }

        @Test
        @DisplayName("SOLD + winnerId hợp lệ → tạo AUCTION_WON notification + sendToUser")
        void soldWinnerNotification() throws Exception {

            NotificationDTO notifDto =
                    NotificationDTO.builder()
                            .id(2)
                            .userId(7)
                            .title("T")
                            .message("M")
                            .type("AUCTION_WON")
                            .isRead(false)
                            .createdAt(Instant.now())
                            .build();

            when(notificationService.createNotification(
                    eq(7),
                    anyString(),
                    anyString(),
                    eq(NotificationType.AUCTION_WON)
            )).thenReturn(notifDto);

            var handler = getEndedHandler();

            handler.onEvent(buildSoldEvent(7));

            verify(notificationService)
                    .createNotification(
                            eq(7),
                            contains("Thắng"),
                            anyString(),
                            eq(NotificationType.AUCTION_WON)
                    );

            verify(channel)
                    .sendToUser(eq(7), any(ServerResponse.class));
        }

        @Test
        @DisplayName("SOLD + winnerId = 0 → KHÔNG tạo notification")
        void soldWinnerIdZeroNoNotif() throws Exception {

            var handler = getEndedHandler();

            handler.onEvent(buildSoldEvent(0));

            verify(notificationService, never())
                    .createNotification(
                            anyInt(),
                            anyString(),
                            anyString(),
                            any()
                    );
        }

        @Test
        @DisplayName("NO_BID event → broadcast AUCTION_ENDED với message không có giá")
        void noBidBroadcast() throws Exception {

            var handler = getEndedHandler();

            handler.onEvent(buildNoBidEvent());

            ArgumentCaptor<ServerResponse> captor =
                    ArgumentCaptor.forClass(ServerResponse.class);

            verify(channel).broadcast(captor.capture());

            assertTrue(
                    captor.getValue()
                            .getMessage()
                            .contains("không có người đặt giá")
            );
        }

        @Test
        @DisplayName("NO_BID event → KHÔNG tạo notification")
        void noBidNoNotification() throws Exception {

            var handler = getEndedHandler();

            handler.onEvent(buildNoBidEvent());

            verify(notificationService, never())
                    .createNotification(
                            anyInt(),
                            anyString(),
                            anyString(),
                            any()
                    );

            verify(channel, never())
                    .sendToUser(anyInt(), any());
        }

        @Test
        @DisplayName("notificationService lỗi khi AUCTION_WON → silent, broadcast vẫn xảy ra")
        void notifServiceErrorSilent() throws Exception {

            when(notificationService.createNotification(
                    anyInt(),
                    anyString(),
                    anyString(),
                    any()
            )).thenThrow(new RuntimeException("DB lỗi"));

            var handler = getEndedHandler();

            assertDoesNotThrow(() ->
                    handler.onEvent(buildSoldEvent(7)));

            verify(channel).broadcast(any());
        }
    }
}