package vn.edu.vnu.uet.group8.client.networking;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.request.ServerRequest;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.enums.ActionType;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;

/**
 * AuctionClient - Singleton quản lý kết nối     Socket tới server.
 * <p>
 * Chịu trách nhiệm:
 * - Duy trì một kết nối TCP socket tới server.
 * - Gửi các request (ServerRequest) dưới dạng JSON.
 * - Nhận response (ServerResponse) và chuyển cho ResponseDispatcher.
 * - Tự động reconnect? (hiện tại thì không, chỉ báo mất kết nối và redirect về login).
 * <p>
 * Thread-safe: sử dụng AtomicBoolean cho trạng thái kết nối,
 * và synchronized trên OutputStream khi ghi để tránh xung đột.
 */
public final class AuctionClient {

    private static final AuctionClient INSTANCE = new AuctionClient();
    public static AuctionClient getInstance() {
        return INSTANCE;
    }
    // Fields
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    // Dùng AtomicBoolean thay cho boolean thường để thread-safe (đọc/ghi không bị race condition)
    private final AtomicBoolean connected = new AtomicBoolean(false);
    // Thread pool
    private ExecutorService receiverExecutor;
    private ScheduledExecutorService heartbeatExecutor;
    private String host;
    private int port;

    // Logger thay vì System.out/err - có timestamp, cấp độ log, dễ debug
    private static final Logger LOGGER = Logger.getLogger(AuctionClient.class.getName());

    //Hearbeat interval (giây)
    private static final int HEARTBEAT_INTERVAL_SEC = 30;

    //  PRIVATE CONSTRUCTOR (Singleton)
    private AuctionClient() {
        // Không cho tạo instance bên ngoài
    }
    /**
     * Kết nối tới server. Nếu đã kết nối thì bỏ qua. Nếu có kết nối cũ, dọn dẹp trước.
     * @param host server host (ví dụ "localhost")
     * @param port server port (ví dụ 12345)
     * @throws IOException nếu không thể kết nối
     */
    public synchronized void connect(String host, int port) throws IOException {
        if (connected.get()) {
            LOGGER.info("Already connected, ignoring connect request.");
            return;
        }
        this.host = host;
        this.port = port;
        disconnect();// dọn dẹp kết nối cũ
        socket = new Socket(host, port);
        // Dùng BufferedOutputStream để tăng hiệu suất (giảm số lần write system call)
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        connected.set(true);
        LOGGER.info("Connected to server at " + host + ":" + port);
        // Khởi tạo thread pool đơn luồng để lắng nghe response từ server
        receiverExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AuctionClient-Receiver");
            t.setDaemon(true);  // Cho phép JVM thoát khi không còn thread user nào
            return t;
        });
        receiverExecutor.submit(this::listenLoop);
        // Khởi động heartbeat định kỳ
        startHeartbeat();
    }

    /**
     * Thử kết nối lại với thông số cũ nếu đã mất kết nối.
     */
    public synchronized boolean reconnect() {
        if (connected.get()) return true;
        if (host == null || port == 0) return false;
        try {
            connect(host, port);
            return true;
        } catch (IOException e) {
            LOGGER.warning("Reconnect failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gửi một request tới server và đăng ký callback nhận response.
     *
     *  payload  object dữ liệu request (sẽ được gói trong ServerRequest)
     * @param callback hàm xử lý response (có thể null nếu không cần)
     */
    public void sendRequest(ServerRequest<?> request, Consumer<ServerResponse> callback) {
    if (!connected.get() || out == null) {
      LOGGER.warning("Cannot send request: not connected to server");
      if (callback != null) {
        Platform.runLater(() -> callback.accept(ServerResponse.replyError(
            "ERROR", request.getRequestId(), "Không có kết nối server")));
      }
      return;
    }

    String requestId = request.getRequestId();
    if (callback != null) {
      ResponseDispatcher.register(requestId, callback);
    }
    try {
      String json = GsonUtil.GSON.toJson(request);
      byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      synchronized (out) {
        out.writeInt(bytes.length);
        out.write(bytes);
        out.flush();
      }
      LOGGER.fine("Sent request: " + requestId + " - " + request.getAction());
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error sending request: " + requestId, e);
      if (callback != null) {
        ResponseDispatcher.unregister(requestId);
        // TRÁNH TREO UI: Báo ngay lỗi mạng cho callback nếu gửi thất bại
        Platform.runLater(() -> callback.accept(ServerResponse.replyError(
            "ERROR", requestId, "Lỗi mạng khi gửi: " + e.getMessage())));
      }
      handleDisconnect(e);
    }
  }

    /**
     * Gửi request không cần callback.
     */
    public void sendRequest(ServerRequest<?> request) {
        sendRequest(request, null);
    }
    /**
     * Tiện ích ta request đã xác thực( có userId và token từ SessionManager) và gửi.
     * @param action loại hành động
     * @param payload dữ liệu nghiệp vụ
     * @param callback xử lý response
     */
    public <T> void sendAuthenticatedRequest(ActionType action, T payload, Consumer<ServerResponse> callback) {
        ServerRequest<T> req = ServerRequest.<T>builder(action)
                .userId(SessionManager.getUserId())
                .token(SessionManager.getAuthToken())
                .payload(payload)
                .build();
        sendRequest(req, callback);
    }
    /**
     * Gửi request xác thực không cần callback
     */
    public<T> void sendAuthenticatedRequest(ActionType action, T payload){
        sendAuthenticatedRequest(action,payload,null);}
    /**
     * Ngắt kết nối chủ động (khi logout hoặc tắt app).
     */
    public synchronized void disconnect() {
        if (!connected.getAndSet(false)) {
            return; // đã ngắt rồi
        }
        LOGGER.info("Disconnecting from server...");
        stopHeartbeat();
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing socket streams", e);
        }
        if (receiverExecutor != null) {
            receiverExecutor.shutdownNow();
        }
        LOGGER.info("Disconnected from server");
    }
    /**
     * Kiểm tra trạng thái kết nối.
     */
    public boolean isConnected() {
        return connected.get();
    }
    public String getHost() {
        return host;
    }

    // ======================== PRIVATE HELPERS ========================

    /**
     * Vòng lặp nhận dữ liệu từ server (chạy trên thread riêng).
     */
    private void listenLoop() {
        LOGGER.info("Listener loop started");
        try {
            while (connected.get() && !socket.isClosed()) {
                int length = in.readInt();
                if (length <= 0 || length > 64 * 1024 * 1024) {
                    throw new IOException("Độ dài gói tin response không hợp lệ hoặc quá lớn: " + length);
                }
                byte[] bytes = new byte[length];
                in.readFully(bytes);
                String json = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                try {
                    ServerResponse response = GsonUtil.GSON.fromJson(json, ServerResponse.class);
                    // Chuyển response cho dispatcher xử lý (trên FX thread nếu cần)
                    ResponseDispatcher.dispatch(response);
                } catch (Exception parseEx) {
                    LOGGER.log(Level.SEVERE, "Lỗi khi xử lý response từ server: " + parseEx.getMessage());
                }
            }
        } catch (EOFException e) {
            LOGGER.info("Server closed connection (EOF).");
        } catch (IOException e) {
            if (connected.get()) {
                LOGGER.log(Level.WARNING, "IO error in listenLoop", e);
            }
        } finally {
            // Khi loop thoát, xử lý mất kết nối
            handleDisconnect(null);
        }
    }
    /**
     * Gửi gói tin PING định kỳ để giữ kết nối sống.*/
    private void startHeartbeat(){
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r->{
            Thread t = new Thread(r,"heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatExecutor.scheduleAtFixedRate(()->{
            if(connected.get()) {
                try {
                    ServerRequest<Void> ping = ServerRequest.anonymous(ActionType.HEARTBEAT);
                    sendRequest(ping);
                } catch (Exception e) {
                    //Bảo vệ thread heartbeat, không để crash
                }
        }
        },HEARTBEAT_INTERVAL_SEC,HEARTBEAT_INTERVAL_SEC, TimeUnit.SECONDS);
    }
    private void stopHeartbeat(){
        if(heartbeatExecutor != null){
            heartbeatExecutor.shutdownNow();
        }
    }

    /**
     * Xử lý khi mất kết nối đột ngột:
     * - Chỉ xử lý một lần (dùng compareAndSet)
     * - Log lỗi
     * - Nếu đã đăng nhập, thông báo cho user bằng Alert (trên FX thread) và chuyển về login
     */
    private void handleDisconnect(Exception cause) {
        // Chỉ xử lý nếu trước đó connected đang true -> false
        if (!connected.compareAndSet(true, false)) {
            return; // đã xử lý rồi hoặc chưa kết nối
        }

        if (cause != null) {
            LOGGER.log(Level.WARNING, "Connection lost due to exception", cause);
        } else {
            LOGGER.warning("Connection lost (EOF or socket closed).");
        }

        // Dọn dẹp tài nguyên (đã đánh dấu connected = false)
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            LOGGER.log(Level.FINEST, "Error closing resources on disconnect", e);
        }
        if (receiverExecutor != null) {
            receiverExecutor.shutdownNow();
        }
        stopHeartbeat();

        // Cập nhật UI trên FX thread
        Platform.runLater(() -> {
            // Kiểm tra nếu đã đăng nhập thì mới hiện alert và redirect
            if (SessionManager.isLoggedIn()) {
                String msg = (cause != null) ? cause.getMessage() : "Mất kết nối đến server";
                AlertUtil.showError("Mất kết nối: " + msg);
                // Xoá session và quay về màn hình login
                ClientModel.getInstance().clearSession();
                SessionManager.logout(); // logout cũng xoá token, nhưng tránh gọi disconnect lại (đã ngắt)
                // Đảm bảo chuyển scene (nếu chưa ở login)
                SceneManager.switchTo(SceneManager.VIEW_LOGIN);
            } else {
                // Chưa đăng nhập, chỉ đơn thuần mất kết nối, không cần alert
                LOGGER.fine("Disconnected before login, no alert shown.");
            }
        });
    }
}
