package vn.edu.vnu.uet.group8.server.network;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;

@ExtendWith(MockitoExtension.class)
class BroadcastChannelImplTest {

  @Mock private ClientHandler h1;
  @Mock private ClientHandler h2;
  @Mock private ClientHandler h3;

  private BroadcastChannelImpl impl;

  private ServerResponse resp(String action) {
    return ServerResponse.reply(action, "req-test")
        .success(true)
        .message("ok")
        .build();
  }

  @BeforeEach
  void setUp() {
    impl = new BroadcastChannelImpl();
  }

  // ─────────────────────────────────────────────────────────────
  // addClient / getConnectedClientCount
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("addClient()")
  class AddClientTest {

    @Test
    @DisplayName("addClient null → count không tăng")
    void nullIgnored() {
      impl.addClient(null);
      assertEquals(0, impl.getConnectedClientCount());
    }

    @Test
    @DisplayName("addClient 1 client → count = 1")
    void oneClient() {
      impl.addClient(h1);
      assertEquals(1, impl.getConnectedClientCount());
    }

    @Test
    @DisplayName("addClient 3 clients → count = 3")
    void threeClients() {
      impl.addClient(h1);
      impl.addClient(h2);
      impl.addClient(h3);
      assertEquals(3, impl.getConnectedClientCount());
    }

    @Test
    @DisplayName("addClient cùng handler 2 lần → count = 1 (Set)")
    void duplicateIgnored() {
      impl.addClient(h1);
      impl.addClient(h1);
      assertEquals(1, impl.getConnectedClientCount());
    }
  }

  // ─────────────────────────────────────────────────────────────
  // removeClient
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("removeClient()")
  class RemoveClientTest {

    @Test
    @DisplayName("removeClient null → không throw")
    void nullIgnored() {
      impl.addClient(h1);
      assertDoesNotThrow(() -> impl.removeClient(null));
      assertEquals(1, impl.getConnectedClientCount());
    }

    @Test
    @DisplayName("removeClient handler có trong set → count giảm")
    void removeExisting() {
      impl.addClient(h1);
      impl.addClient(h2);
      impl.removeClient(h1);
      assertEquals(1, impl.getConnectedClientCount());
    }

    @Test
    @DisplayName("removeClient handler không có → count không đổi")
    void removeNotExisting() {
      impl.addClient(h1);
      impl.removeClient(h2); // h2 chưa add
      assertEquals(1, impl.getConnectedClientCount());
    }

    @Test
    @DisplayName("removeClient có userId → unregister khỏi userSessions")
    void removeWithUserId() {
      when(h1.getUserId()).thenReturn(5);
      impl.addClient(h1);
      impl.registerUser(5, h1);
      impl.removeClient(h1);
      assertEquals(0, impl.getConnectedClientCount());
      // sendToUser(5) → sessions rỗng → không gửi
      assertDoesNotThrow(() -> impl.sendToUser(5, resp("T")));
      verify(h1, never()).send(any());
    }

    @Test
    @DisplayName("removeClient userId null → không throw")
    void removeWithUserIdNull() {
      when(h1.getUserId()).thenReturn(null);
      impl.addClient(h1);
      assertDoesNotThrow(() -> impl.removeClient(h1));
    }

    @Test
    @DisplayName("removeClient với userId không có trong userSessions → không throw")
    void removeUserIdNotInSessions() {
      when(h1.getUserId()).thenReturn(99);
      impl.addClient(h1);
      // Không gọi registerUser(99, h1) → userSessions không có key 99
      assertDoesNotThrow(() -> impl.removeClient(h1));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // broadcast
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("broadcast()")
  class BroadcastTest {

    @Test
    @DisplayName("broadcast khi không có client → không throw")
    void noClients() {
      assertDoesNotThrow(() -> impl.broadcast(resp("T")));
    }

    @Test
    @DisplayName("broadcast → gửi tới tất cả 3 client")
    void allClients() {
      impl.addClient(h1);
      impl.addClient(h2);
      impl.addClient(h3);
      ServerResponse r = resp("PRICE_UPDATE");
      impl.broadcast(r);
      verify(h1).send(r);
      verify(h2).send(r);
      verify(h3).send(r);
    }

    @Test
    @DisplayName("broadcast một client lỗi → bỏ qua, gửi tiếp cho các client khác")
    void oneClientFails() {
      impl.addClient(h1);
      impl.addClient(h2);
      doThrow(new RuntimeException("lỗi mạng")).when(h1).send(any());

      assertDoesNotThrow(() -> impl.broadcast(resp("UPDATE")));
      // h2 vẫn nhận được
      verify(h2).send(any());
    }

    @Test
    @DisplayName("broadcast khi tất cả client lỗi → không throw")
    void allClientsFail() {
      impl.addClient(h1);
      impl.addClient(h2);
      doThrow(new RuntimeException("lỗi")).when(h1).send(any());
      doThrow(new RuntimeException("lỗi")).when(h2).send(any());

      assertDoesNotThrow(() -> impl.broadcast(resp("T")));
    }

    @Test
    @DisplayName("broadcast → getConnectedClientCount() dùng để log")
    void connectedCount() {
      impl.addClient(h1);
      impl.addClient(h2);
      assertEquals(2, impl.getConnectedClientCount());
    }
  }

  // ─────────────────────────────────────────────────────────────
  // registerUser / unregisterUser
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("registerUser() / unregisterUser()")
  class RegisterUserTest {

    @Test
    @DisplayName("registerUser null handler → bỏ qua")
    void registerNull() {
      assertDoesNotThrow(() -> impl.registerUser(1, null));
    }

    @Test
    @DisplayName("unregisterUser null handler → bỏ qua")
    void unregisterNull() {
      assertDoesNotThrow(() -> impl.unregisterUser(1, null));
    }

    @Test
    @DisplayName("registerUser 2 handler cùng userId → handler cũ bị kick, chỉ handler mới nhận tin")
    void multipleHandlersSameUser() {
      impl.registerUser(5, h1);
      impl.registerUser(5, h2);
      ServerResponse r = resp("T");
      impl.sendToUser(5, r);
      verify(h1).closeConnection();
      verify(h1, never()).send(r);
      verify(h2).send(r);
    }

    @Test
    @DisplayName("unregisterUser → handler đó không nhận tin nữa")
    void unregisterStopsDelivery() {
      impl.registerUser(5, h1);
      impl.unregisterUser(5, h1);
      ServerResponse r = resp("T");
      impl.sendToUser(5, r);
      verify(h1, never()).send(any());
    }

    @Test
    @DisplayName("unregisterUser khi userId không có trong map → không throw")
    void unregisterMissingKey() {
      assertDoesNotThrow(() -> impl.unregisterUser(999, h1));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // sendToUser
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("sendToUser()")
  class SendToUserTest {

    @Test
    @DisplayName("user không online → không gửi, không throw")
    void userOffline() {
      assertDoesNotThrow(() -> impl.sendToUser(999, resp("T")));
      verify(h1, never()).send(any());
    }

    @Test
    @DisplayName("user có 1 session → gửi đúng")
    void oneSession() {
      impl.registerUser(3, h1);
      ServerResponse r = resp("NOTIF");
      impl.sendToUser(3, r);
      verify(h1).send(r);
    }

    @Test
    @DisplayName("user đăng nhập lại nhiều lần → chỉ session cuối cùng nhận tin")
    void multipleSessions() {
      impl.registerUser(3, h1);
      impl.registerUser(3, h2);
      impl.registerUser(3, h3);
      ServerResponse r = resp("NOTIF");
      impl.sendToUser(3, r);
      verify(h1).closeConnection();
      verify(h2).closeConnection();
      verify(h1, never()).send(r);
      verify(h2, never()).send(r);
      verify(h3).send(r);
    }

    @Test
    @DisplayName("session lỗi khi send → loại khỏi danh sách, không throw")
    void sessionSendFails() {
      impl.addClient(h1);
      impl.registerUser(5, h1);
      doThrow(new RuntimeException("mất kết nối")).when(h1).send(any());

      assertDoesNotThrow(() -> impl.sendToUser(5, resp("T")));
      assertEquals(0, impl.getConnectedClientCount());
    }

    @Test
    @DisplayName("sendToUser sau khi tất cả session bị unregister → không gửi")
    void allSessionsUnregistered() {
      impl.registerUser(5, h1);
      impl.unregisterUser(5, h1);
      impl.sendToUser(5, resp("T"));
      verify(h1, never()).send(any());
    }
  }

  // ─────────────────────────────────────────────────────────────
  // getConnectedClientCount
  // ─────────────────────────────────────────────────────────────
  @Test
  @DisplayName("getConnectedClientCount = 0 ban đầu")
  void initialCount() {
    assertEquals(0, impl.getConnectedClientCount());
  }

  @Test
  @DisplayName("getConnectedClientCount cập nhật đúng sau add/remove")
  void countAfterAddRemove() {
    impl.addClient(h1);
    impl.addClient(h2);
    impl.addClient(h3);
    impl.removeClient(h2);
    assertEquals(2, impl.getConnectedClientCount());
  }
}
