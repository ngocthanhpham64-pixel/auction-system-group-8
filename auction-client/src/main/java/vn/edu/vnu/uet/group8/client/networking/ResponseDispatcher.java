package vn.edu.vnu.uet.group8.client.networking;

import javafx.application.Platform;
import vn.edu.vnu.uet.group8.common.dto.ServerResponse;
import vn.edu.vnu.uet.group8.common.enums.EventType;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Điều phối toàn bộ phản hồi(ServerResponse) nhận được từ server
 * - Hỗ trợ hai cơ chế phân phối:
 *      One-shot callback - Đăng ký theo requestId(Callback chỉ được gọi đúng 1 lần, tự động bị xóa sau khi gọi hoặc bị timeout không có phản hồi
 *      Broadcast listener - Đăng ký theo eventType do server gán, tất cả listener đã subscribe eventType đều được gọi
 */
public final class ResponseDispatcher {
    private static final Logger LOGGER = Logger.getLogger(ResponseDispatcher.class.getName());
    // One-shot pending callbacks: gon vào 1 map với wrapper
    private static class PendingCallback{
        final Consumer<ServerResponse> callback;
        final long registeredAt;
        PendingCallback(Consumer<ServerResponse> cb){
            this.callback = cb;
            this.registeredAt = System.currentTimeMillis();
        }
    }

    /**
     * Map lưu các one-shot callback đang chờ phản hồi.
     * Key = requestID, Value = PendingCallback
     * ConcurrentHashMap đảm bảo thread-safe khi đọc/ ghi đồng thời
     */
    private static final ConcurrentHashMap<String,PendingCallback> pending = new ConcurrentHashMap<>();
    /**
     * Map lưu danh sách listener theo eventType
     * Key = eventType(String do server định nghĩa, ví dụ"auction_update)
     * Value = CopyOnWriteArrayList để thread-safe khi subscribe/unsubscribe
     */
    private static final ConcurrentHashMap<EventType, CopyOnWriteArrayList<Consumer<ServerResponse>>> broadcastListeners = new ConcurrentHashMap<>();
    private static final long CALLBACK_TIMEOUT_MS = 30_000;
    //Scheduler dọn dẹp callback hết hạn
    /**
     * ScheduledExecutorService chạy 1 thread daemon để định kỳ dọn các callback đã timeout ra khỏi
     * Daemon thread tự kết thúc khi JVM shutdown - không cần shutdown thủ công
     */
    private static final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r->{
        Thread t = new Thread(r,"dispatcher-cleaner");
        t.setDaemon(true);
        return t;
    });
    // Khởi chạy cleaner ngay khi class được load
    // Chạy mỗi 10 giây, bắt đầu sau 10 giây đầu tiên
    static {
        cleaner.scheduleAtFixedRate(ResponseDispatcher::evictExpiredCallbacks,10,10,TimeUnit.SECONDS);
    }
    // Utility class - không cho khởi tạo
    private ResponseDispatcher(){}
    // Public API - one-shot
    /**
     * Đăng ký một callback one-shot cho một request cụ thể
     * - Callback sẽ được gọi đúng một lần trên FX Thread khi server trả về ServerResponse có dùng requestId, sau đó tự động xóa.
     * Nếu không có phản hồi trong timeout callback bị evict bởi cleaner
     */
    public static void register(String requestId,Consumer<ServerResponse> callback){
        if(requestId == null || callback == null){
            LOGGER.warning("register() bỏ qua: requestId hoặc callback null");
            return;
        }
        pending.put(requestId,new PendingCallback(callback));
        LOGGER.fine(()->"Registered one-shot callback | requestId=" + requestId);
    }
    public static void unregister(String requestId){
        if(requestId == null) return;
        pending.remove(requestId);
        LOGGER.fine(()->"Unregistered one-shot | requestid= " + requestId);}
    //Public API - BroadCast
    /**
     * Đăng kí một listener để nhận tất cả broadcast theo eventType
     * -Listener được gọi trên FX Thread mỗi khi server push một event có eventType tương ứng. Nhiều listener có thể đăng ký cùng eventType
     */
    public static void subscribe(EventType eventType,Consumer<ServerResponse> listener){
        if(eventType == null || listener == null){
            LOGGER.warning("subscribe() bỏ qua: eventType hoặc listener null");
            return;
        }
        broadcastListeners.computeIfAbsent(eventType, k ->new CopyOnWriteArrayList<>()).add(listener);
        LOGGER.fine(()->"Subscribed listener | eventType=" + eventType);
    }
    /**
     * Hủy đăng ký một broadcast listener
     * -Nên gọi trong cleanup của controller để tránh giữ reference và nhận event không mong muốn
     */
    public static void unsubscribe(EventType eventType,Consumer<ServerResponse> listener){
        if(eventType == null || listener == null) return;
        CopyOnWriteArrayList<Consumer<ServerResponse>> list = broadcastListeners.get(eventType);
        if(list != null && list.remove(listener)){
            LOGGER.fine(() -> "Unsubscribed listener | eventType=" + eventType);
            // Dọn key rỗng
            if (list.isEmpty()) {
                broadcastListeners.remove(eventType, list);
            }
        }
    }
    // Core dispatch
    /**
     * Điểm vào chính - nhận ServerResponse từ AuctionClient và phân phối
     * -Nếu response có resquestId -> tìm one-shot callback và gọi
     *      -Dù tìm thấy hay không đều return một response không thể vừa là reply vừa là broadcast event
     * -Nếu response không có requestId -> tìm broadcast listeners theo eventType và gọi tất cả
     * Gọi từ background thread (AuctionClient reader thread) — an toàn.
     * Callback/listener được đẩy sang FX Thread qua {@code Platform.runLater}.
     */
    public static void dispatch(ServerResponse response) {
        if (response == null) {
            LOGGER.warning("dispatch() nhận response null — bỏ qua");
            return;
        }
        // Bước 1: Xử lý one-shot callback
        //Response có requestId -> đây là reply cho một request cụ thể
        String reqId = response.getRequestId();
        if (reqId != null) {
            PendingCallback pc = pending.remove(reqId);
            if (pc != null) {
                //Tìm thấy callback -> chạy trên FX Thread
                Platform.runLater(() -> {
                    try {
                        pc.callback.accept(response);
                        LOGGER.fine(() -> "One-shot callback OK | requestId=" + reqId);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Lỗi trong one-shot callback | requestId=" + reqId, e);
                    }
                });
            } else {
                // Không tìm thấy callback: đã timeout hoặc requestId không hợp lệ
                // Đây là trường hợp bình thường khi mạng chậm - chỉ log FINE
                LOGGER.fine(() -> "Orphan response (no callback) | requestId=" + reqId);
            }
            return;
        }
        // Bước 2: Xử lý broadcast theo eventType
        // Response không có requestId -> đây là server-push event
        EventType eventType = response.getEventType();
        if (eventType == null) {
            LOGGER.warning(() -> "Response không có requestId lẫn eventType: " + response);
            return;
        }
        CopyOnWriteArrayList<Consumer<ServerResponse>> listeners = broadcastListeners.get(eventType);
        if (listeners == null || listeners.isEmpty()) {
            LOGGER.fine(() -> "Không có listener nào cho eventType=" + eventType);
            return;
        }
        // Tạo snapshot trước khi đẩy vào FX Thread.
        // Mặc dù CopyOnWriteArrayList đã thread-safe khi iterate,
        // snapshot đảm bảo danh sách không thay đổi trong suốt vòng lặp
        // ngay cả khi listener tự unsubscribe trong callback.
        List<Consumer<ServerResponse>> snapshot = List.copyOf(listeners);
        Platform.runLater(() -> {
            for (Consumer<ServerResponse> listener : snapshot) {
                try {
                    listener.accept(response);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Lỗi trong broadcast listener và eventType= " + eventType);
                }
            }
            LOGGER.fine(() -> "Broadcast OK | eventType=" + eventType
                    + " | listeners=" + snapshot.size());
        });
    }

    // Internal Cleanup
    /**
     * Xóa các callback đã quá hạn khỏi pending
     * - Được gọi định kỳ mỗi 10 giây
     * Dùng removeIf để duyệt và xóa atomic trên ConcurrentHashMap-an toàn khi dispatch đang chạy đồng thời
     */
    private static void evictExpiredCallbacks() {
        long now = System.currentTimeMillis();
        // int[] thay int vì lambda không capture biến non-effectively-final
        int[] removed = {0};

        pending.entrySet().removeIf(entry -> {
            if (now - entry.getValue().registeredAt > CALLBACK_TIMEOUT_MS) {
                removed[0]++;
                return true; // xoá khỏi map
            }
            return false;
        });

        if (removed[0] > 0) {
            LOGGER.fine(() -> "Evicted " + removed[0] + " expired callbacks");
        }
    }
}