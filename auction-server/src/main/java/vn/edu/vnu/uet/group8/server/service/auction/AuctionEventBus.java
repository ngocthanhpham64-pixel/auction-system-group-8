package vn.edu.vnu.uet.group8.server.service.auction;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event bus đơn giản, type-safe, không dùng reflection.
 *
 * <p>Subscriber đăng ký theo Class của event — không phải String.
 * Compile-time check: sai kiểu event → lỗi ngay khi build.
 *
 * <p>Thread-safe:
 * <ul>
 *   <li>{@code ConcurrentHashMap} cho map listener theo type
 *   <li>{@code CopyOnWriteArrayList} cho danh sách listener
 *       của mỗi type — tránh {@code ConcurrentModificationException}
 *       khi publish đang duyệt mà có subscriber mới đăng ký
 * </ul>
 *
 * <p>Publish là synchronous — gọi listener trong cùng thread.
 * Nếu listener cần async (gửi socket), subscriber tự wrap
 * bằng {@code CompletableFuture} hoặc executor.
 */
public class AuctionEventBus {

  private static final Logger logger =
      LoggerFactory.getLogger(AuctionEventBus.class);

  // Class của event → danh sách listener
  private final Map<Class<?>, List<EventListener<?>>> listeners =
      new ConcurrentHashMap<>();

  /**
   * Interface cho subscriber — type-safe, có thể ném SQLException.
   *
   * @param <E> kiểu event subscriber quan tâm
   */
  @FunctionalInterface
  public interface EventListener<E> {
    void onEvent(E event) throws Exception;
  }

  /**
   * Đăng ký subscriber lắng nghe một loại event.
   *
   * <p>Gọi khi server khởi động, trước khi nhận bid đầu tiên.
   *
   * @param eventType Class của event muốn lắng nghe
   * @param listener  hàm xử lý event
   * @param <E>       kiểu event
   */
  public <E> void subscribe(Class<E> eventType, EventListener<E> listener) {
    listeners
        .computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
        .add(listener);

    logger.debug("Subscriber đăng ký: event={}, listener={}",
        eventType.getSimpleName(),
        listener.getClass().getSimpleName());
  }

  /**
   * Hủy đăng ký subscriber.
   *
   * @param eventType Class của event
   * @param listener  listener cần hủy
   * @param <E>       kiểu event
   */
  public <E> void unsubscribe(Class<E> eventType, EventListener<E> listener) {
    List<EventListener<?>> list = listeners.get(eventType);
    if (list != null) {
      list.remove(listener);
    }
  }

  /**
   * Publish event đến tất cả subscriber đã đăng ký.
   *
   * <p>Gọi listener tuần tự trong cùng thread.
   * Lỗi của một listener được log và bỏ qua — không dừng
   * các listener còn lại.
   *
   * @param event sự kiện cần phát đi
   * @param <E>   kiểu event
   */
  @SuppressWarnings("unchecked")
  public <E> void publish(E event) {
    List<EventListener<?>> list = listeners.get(event.getClass());

    if (list == null || list.isEmpty()) {
      logger.warn("Không có subscriber nào cho event: {}",
          event.getClass().getSimpleName());
      return;
    }

    logger.debug("Publish event: {}, subscribers={}",
        event.getClass().getSimpleName(), list.size());

    for (EventListener<?> rawListener : list) {
      try {
        // Cast an toàn vì chúng ta chỉ lưu
        // EventListener<E> vào list của Class<E>
        ((EventListener<E>) rawListener).onEvent(event);
      } catch (Exception e) {
        // Log lỗi nhưng không ném — subscriber khác vẫn nhận được
        logger.error(
            "Lỗi trong subscriber khi xử lý event {}: {}",
            event.getClass().getSimpleName(), e.getMessage());
      }
    }
  }
}