package vn.edu.vnu.uet.group8.client.networking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.Instant;
import javafx.application.Platform;
import vn.edu.vnu.uet.group8.common.dto.ServerRequest;
import vn.edu.vnu.uet.group8.common.dto.ServerResponse;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.util.SceneManager;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;

import java.io.*;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AuctionClient - Singleton quản lý kết nối Socket tới server.
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

    // ======================== SINGLETON (Eager initialization) ========================
    private static final AuctionClient INSTANCE = new AuctionClient();

    /**
     * Trả về instance duy nhất của AuctionClient.
     * Dùng eager initialization (static final) - không cần synchronized,
     * an toàn với đa luồng, hiệu suất cao.
     */
    public static AuctionClient getInstance() {
        return INSTANCE;
    }

    // ======================== FIELDS ========================
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, (com.google.gson.JsonSerializer<Instant>) (src, typeOfSrc, context) ->
                    context.serialize(src.toString()))
            .registerTypeAdapter(Instant.class, (com.google.gson.JsonDeserializer<Instant>) (json, typeOfT, context) ->
                    Instant.parse(json.getAsString()))
            .create();

    // Dùng AtomicBoolean thay cho boolean thường để thread-safe (đọc/ghi không bị race condition)
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private ExecutorService receiverExecutor;

    // Logger thay vì System.out/err - có timestamp, cấp độ log, dễ debug
    private static final Logger LOGGER = Logger.getLogger(AuctionClient.class.getName());

    // ======================== PRIVATE CONSTRUCTOR (Singleton) ========================
    private AuctionClient() {
        // Không cho tạo instance bên ngoài
    }

    // ======================== PUBLIC API ========================

    /**
     * Kết nối tới server.
     *
     * @param host server host (ví dụ "localhost")
     * @param port server port (ví dụ 12345)
     * @throws IOException nếu không thể kết nối
     */
    public void connect(String host, int port) throws IOException {
        if (connected.get()) {
            LOGGER.info("Already connected, ignoring connect request.");
            return;
        }

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
    }

    /**
     * Gửi một request tới server và đăng ký callback nhận response.
     *
     * @param payload  object dữ liệu request (sẽ được gói trong ServerRequest)
     * @param callback hàm xử lý response (có thể null nếu không cần)
     */
    public void sendRequest(Object payload, Consumer<ServerResponse> callback) {
        if (!connected.get() || out == null) {
            LOGGER.warning("Cannot send request: not connected to server");
            if (callback != null) {
                // Có thể tạo một response lỗi giả để callback biết, nhưng tạm thời chỉ log
                callback.accept(createErrorResponse("Không có kết nối server"));
            }
            return;
        }

        String requestId = UUID.randomUUID().toString();
        if (callback != null) {
            ResponseDispatcher.register(requestId, callback);
        }

        ServerRequest req = new ServerRequest(
                requestId,
                SessionManager.getAuthToken(),   // token có thể null nếu chưa login
                payload
        );

        try {
            String json = gson.toJson(req);
            // Synchronized trên out để tránh 2 thread ghi xen kẽ làm hỏng JSON
            synchronized (out) {
                out.writeUTF(json);
                out.flush();
            }
            LOGGER.fine("Sent request: " + requestId + " - " + payload.getClass().getSimpleName());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error sending request: " + requestId, e);
            // Xoá callback nếu đã đăng ký để tránh memory leak
            if (callback != null) {
                ResponseDispatcher.unregister(requestId);
            }
            // Nếu lỗi ghi, coi như mất kết nối
            handleDisconnect(e);
        }
    }

    /**
     * Gửi request không cần callback.
     */
    public void sendRequest(Object payload) {
        sendRequest(payload, null);
    }

    /**
     * Ngắt kết nối chủ động (khi logout hoặc tắt app).
     */
    public synchronized void disconnect() {
        if (!connected.getAndSet(false)) {
            return; // đã ngắt rồi
        }

        LOGGER.info("Disconnecting from server...");
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

    // ======================== PRIVATE HELPERS ========================

    /**
     * Vòng lặp nhận dữ liệu từ server (chạy trên thread riêng).
     */
    private void listenLoop() {
        LOGGER.info("Listener loop started");
        try {
            while (connected.get() && !socket.isClosed()) {
                String json = in.readUTF();
                ServerResponse response = gson.fromJson(json, ServerResponse.class);
                // Chuyển response cho dispatcher xử lý (trên FX thread nếu cần)
                ResponseDispatcher.dispatch(response);
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
                SceneManager.switchTo("login.fxml");
            } else {
                // Chưa đăng nhập, chỉ đơn thuần mất kết nối, không cần alert
                LOGGER.fine("Disconnected before login, no alert shown.");
            }
        });
    }

    /**
     * Tạo một ServerResponse giả để báo lỗi không kết nối (dùng cho callback ngay lập tức).
     */
    private ServerResponse createErrorResponse(String message) {
        ServerResponse resp = new ServerResponse();
        resp.setSuccess(false);
        resp.setMessage(message);
        // Không set requestId, coi như response lỗi
        return resp;
    }
}