package vn.edu.vnu.uet.group8.server.service.auction;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test cho {@link AuctionEventBus}.
 *
 * <p>Không cần mock - test pure event dispatcher.
 *
 * <p>Phạm vi:
 * <ul>
 *   <li>subscribe / publish: nhận đúng event đã đăng ký
 *   <li>unsubscribe: không nhận event nữa sau khi huỷ
 *   <li>Multi-subscriber: 2 listener cùng nhận 1 event
 *   <li>Multi-type: subscribe type A không nhận event type B
 *   <li>publish event không có subscriber: không lỗi
 *   <li>Listener ném exception: các listener khác vẫn chạy
 *   <li>Type-safe: cast đúng kiểu
 * </ul>
 */
class AuctionEventBusTest {

    /** Event class dummy để test. */
    static class DummyEventA {
        final String message;
        DummyEventA(String m) { this.message = m; }
    }

    static class DummyEventB {
        final int value;
        DummyEventB(int v) { this.value = v; }
    }

    private AuctionEventBus bus;

    @BeforeEach
    void setUp() {
        bus = new AuctionEventBus();
    }

    @Nested
    @DisplayName("subscribe + publish")
    class SubscribePublish {

        @Test
        @DisplayName("Subscriber nhận đúng event đã subscribe")
        void nhanDungEvent() {
            AtomicReference<String> received = new AtomicReference<>();
            bus.subscribe(DummyEventA.class, e -> received.set(e.message));

            bus.publish(new DummyEventA("hello"));
            assertEquals("hello", received.get());
        }

        @Test
        @DisplayName("Type-safe: event type khác → listener KHÔNG nhận")
        void typeSafe() {
            AtomicInteger countA = new AtomicInteger();
            bus.subscribe(DummyEventA.class, e -> countA.incrementAndGet());

            bus.publish(new DummyEventB(42));
            assertEquals(0, countA.get(),
                    "Listener của DummyEventA không được nhận DummyEventB");
        }

        @Test
        @DisplayName("Nhiều subscriber cùng type → tất cả đều nhận")
        void nhieuSubscriber() {
            AtomicInteger count = new AtomicInteger();
            bus.subscribe(DummyEventA.class, e -> count.incrementAndGet());
            bus.subscribe(DummyEventA.class, e -> count.incrementAndGet());
            bus.subscribe(DummyEventA.class, e -> count.incrementAndGet());

            bus.publish(new DummyEventA("x"));
            assertEquals(3, count.get(), "3 subscriber đều nhận được event");
        }

        @Test
        @DisplayName("Subscriber nhận nhiều event liên tiếp")
        void nhanNhieuEvent() {
            AtomicInteger count = new AtomicInteger();
            bus.subscribe(DummyEventA.class, e -> count.incrementAndGet());

            bus.publish(new DummyEventA("a"));
            bus.publish(new DummyEventA("b"));
            bus.publish(new DummyEventA("c"));

            assertEquals(3, count.get());
        }
    }

    @Nested
    @DisplayName("unsubscribe")
    class Unsubscribe {

        @Test
        @DisplayName("Sau unsubscribe → không nhận event nữa")
        void sauUnsubscribe() {
            AtomicInteger count = new AtomicInteger();
            AuctionEventBus.EventListener<DummyEventA> listener = e -> count.incrementAndGet();

            bus.subscribe(DummyEventA.class, listener);
            bus.publish(new DummyEventA("x"));
            assertEquals(1, count.get());

            bus.unsubscribe(DummyEventA.class, listener);
            bus.publish(new DummyEventA("y"));
            assertEquals(1, count.get(), "Sau unsubscribe, count không tăng");
        }

        @Test
        @DisplayName("Unsubscribe khi không có subscriber - không lỗi")
        void unsubscribeKhongCo() {
            AuctionEventBus.EventListener<DummyEventA> listener = e -> {};
            assertDoesNotThrow(() -> bus.unsubscribe(DummyEventA.class, listener));
        }

        @Test
        @DisplayName("Unsubscribe 1 listener, các listener khác vẫn nhận")
        void unsubscribeMot() {
            AtomicInteger c1 = new AtomicInteger();
            AtomicInteger c2 = new AtomicInteger();
            AuctionEventBus.EventListener<DummyEventA> l1 = e -> c1.incrementAndGet();
            AuctionEventBus.EventListener<DummyEventA> l2 = e -> c2.incrementAndGet();

            bus.subscribe(DummyEventA.class, l1);
            bus.subscribe(DummyEventA.class, l2);
            bus.unsubscribe(DummyEventA.class, l1);

            bus.publish(new DummyEventA("x"));
            assertEquals(0, c1.get(), "l1 đã unsubscribe");
            assertEquals(1, c2.get(), "l2 vẫn nhận");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Publish event không có subscriber → không lỗi")
        void publishKhongCoSubscriber() {
            assertDoesNotThrow(() -> bus.publish(new DummyEventA("no listener")));
        }

        @Test
        @DisplayName("Listener ném exception → các listener khác vẫn chạy")
        void listenerThrowKhongChanLai() {
            AtomicInteger goodCount = new AtomicInteger();

            bus.subscribe(DummyEventA.class, e -> {
                throw new RuntimeException("boom");
            });
            bus.subscribe(DummyEventA.class, e -> goodCount.incrementAndGet());

            // Không ném ra ngoài
            assertDoesNotThrow(() -> bus.publish(new DummyEventA("x")));
            assertEquals(1, goodCount.get(),
                    "Listener tốt vẫn chạy dù listener trước ném exception");
        }

        @Test
        @DisplayName("Subscribe trong khi publish (CopyOnWriteArrayList) - không lỗi")
        void subscribeKhiPublish() {
            AtomicInteger count = new AtomicInteger();

            bus.subscribe(DummyEventA.class, e -> {
                count.incrementAndGet();
                // Subscriber mới đăng ký khi đang publish
                bus.subscribe(DummyEventA.class, ev -> count.incrementAndGet());
            });

            assertDoesNotThrow(() -> bus.publish(new DummyEventA("x")),
                    "Không bị ConcurrentModificationException");
            assertEquals(1, count.get(),
                    "Subscriber mới chỉ nhận event ở lần publish sau");

            // Publish lần 2 → cả 2 listener đều chạy
            bus.publish(new DummyEventA("y"));
            assertTrue(count.get() >= 2);
        }
    }
}