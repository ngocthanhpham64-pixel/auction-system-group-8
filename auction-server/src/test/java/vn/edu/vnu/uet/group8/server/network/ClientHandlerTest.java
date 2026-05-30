package vn.edu.vnu.uet.group8.server.network;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.vnu.uet.group8.common.dto.model.LoginResultDTO;
import vn.edu.vnu.uet.group8.common.dto.model.UserSummaryDTO;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.entity.UserMember;

@ExtendWith(MockitoExtension.class)
class ClientHandlerTest {

  @Mock private Socket socket;
  @Mock private AppDispatcher dispatcher;
  @Mock private BroadcastChannel broadcastChannel;
  @Mock private InetAddress inetAddress;

  @BeforeEach
  void setUp() throws IOException {
    lenient().when(socket.getInetAddress()).thenReturn(inetAddress);
    lenient().when(inetAddress.getHostAddress()).thenReturn("127.0.0.1");
  }

  // =========================================================
  // Helpers
  // =========================================================

  private ServerResponse resp(String action) {
    return ServerResponse.reply(action, "req-test")
        .success(true)
        .message("ok")
        .build();
  }

  private byte[] encodeMessage(String json) throws IOException {
    byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);

    dos.writeInt(jsonBytes.length);
    dos.write(jsonBytes);

    return baos.toByteArray();
  }

  private InputStream buildInputStream(byte[]... messages) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    for (byte[] msg : messages) {
      baos.write(msg);
    }

    return new ByteArrayInputStream(baos.toByteArray());
  }

  // =========================================================
  // getUserId()
  // =========================================================

  @Test
  @DisplayName("getUserId() = null trước khi login")
  void getUserIdNullBeforeLogin() {
    ClientHandler handler = new ClientHandler(socket, dispatcher, broadcastChannel);

    assertNull(handler.getUserId());
  }

  // =========================================================
  // run() setup errors
  // =========================================================

  @Test
  @DisplayName("run() - IOException khi getInputStream")
  void runIOExceptionOnSetup() throws Exception {
    when(socket.getInputStream()).thenThrow(new IOException("connect failed"));
    when(socket.isClosed()).thenReturn(false);

    ClientHandler handler = new ClientHandler(socket, dispatcher, broadcastChannel);

    handler.run();

    verify(broadcastChannel).removeClient(handler);
  }

  // =========================================================
  // run() request processing
  // =========================================================

  @Nested
  @DisplayName("run() - request processing")
  class RunRequestTest {

    @Test
    @DisplayName("Request hợp lệ → dispatcher.dispatch() được gọi")
    void validRequest() throws Exception {
      String json = "{\"action\":\"HEARTBEAT\",\"requestId\":\"r1\"}";

      InputStream is = buildInputStream(encodeMessage(json));

      when(socket.getInputStream()).thenReturn(is);
      when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

      when(socket.isClosed()).thenReturn(false, false, true);

      when(dispatcher.dispatch(any(), any())).thenReturn(resp("HEARTBEAT"));

      ClientHandler handler = new ClientHandler(socket, dispatcher, broadcastChannel);

      handler.run();

      verify(dispatcher, atLeastOnce()).dispatch(any(), any());
      verify(broadcastChannel).addClient(handler);
    }

    @Test
    @DisplayName("LOGIN thành công → registerUser() được gọi")
    void loginSuccessRegistersUser() throws Exception {
      String json = "{\"action\":\"LOGIN\",\"requestId\":\"r1\"}";

      InputStream is = buildInputStream(encodeMessage(json));

      when(socket.getInputStream()).thenReturn(is);
      when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

      when(socket.isClosed()).thenReturn(false, false, true);

      UserMember member = UserMember.builder("user01", "u@e.com", "pw").build();
      UserSummaryDTO summary = UserSummaryDTO.from(member);
      LoginResultDTO loginResult = new LoginResultDTO(summary, "token-xyz");

      ServerResponse loginResp = ServerResponse.reply("LOGIN", "req-login")
          .success(true)
          .message("ok")
          .data(loginResult)
          .build();

      when(dispatcher.dispatch(any(), any())).thenReturn(loginResp);

      ClientHandler handler = new ClientHandler(socket, dispatcher, broadcastChannel);

      handler.run();

      verify(broadcastChannel).registerUser(eq(loginResult.getUserId()), eq(handler));
      assertEquals(loginResult.getUserId(), handler.getUserId());
    }

    @Test
    @DisplayName("LOGIN response data sai kiểu → không register")
    void loginResponseDataWrongType() throws Exception {
      String json = "{\"action\":\"LOGIN\",\"requestId\":\"r1\"}";

      InputStream is = buildInputStream(encodeMessage(json));

      when(socket.getInputStream()).thenReturn(is);
      when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

      when(socket.isClosed()).thenReturn(false, false, true);

      ServerResponse loginResp = ServerResponse.reply("LOGIN", "req-login")
          .success(true)
          .message("ok")
          .data("wrong-type")
          .build();

      when(dispatcher.dispatch(any(), any())).thenReturn(loginResp);

      ClientHandler handler = new ClientHandler(socket, dispatcher, broadcastChannel);

      handler.run();

      verify(broadcastChannel, never()).registerUser(anyInt(), any());
      assertNull(handler.getUserId());
    }

    @Test
    @DisplayName("LOGOUT sau LOGIN → unregisterUser()")
    void logoutAfterLogin() throws Exception {
      String loginJson = "{\"action\":\"LOGIN\",\"requestId\":\"r1\"}";
      String logoutJson = "{\"action\":\"LOGOUT\",\"requestId\":\"r2\"}";

      InputStream is = buildInputStream(
          encodeMessage(loginJson),
          encodeMessage(logoutJson)
      );

      when(socket.getInputStream()).thenReturn(is);
      when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

      when(socket.isClosed()).thenReturn(false, false, false, false, true);

      UserMember member = UserMember.builder("user01", "u@e.com", "pw").build();
      LoginResultDTO loginResult = new LoginResultDTO(UserSummaryDTO.from(member), "token");

      ServerResponse loginResp = ServerResponse.reply("LOGIN", "req-login")
          .success(true)
          .data(loginResult)
          .message("ok")
          .build();

      ServerResponse logoutResp = ServerResponse.reply("LOGOUT", "req-logout")
          .success(true)
          .message("ok")
          .build();

      when(dispatcher.dispatch(any(), any()))
          .thenReturn(loginResp)
          .thenReturn(logoutResp);

      ClientHandler handler = new ClientHandler(socket, dispatcher, broadcastChannel);

      handler.run();

      verify(broadcastChannel).unregisterUser(anyInt(), eq(handler));
      assertNull(handler.getUserId());
    }

    @Test
    @DisplayName("LOGOUT khi chưa login")
    void logoutWithoutLogin() throws Exception {
      String json = "{\"action\":\"LOGOUT\",\"requestId\":\"r1\"}";

      InputStream is = buildInputStream(encodeMessage(json));

      when(socket.getInputStream()).thenReturn(is);
      when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

      when(socket.isClosed()).thenReturn(false, false, true);

      ServerResponse logoutResp = ServerResponse.reply("LOGOUT", "req-logout")
          .success(true)
          .message("ok")
          .build();

      when(dispatcher.dispatch(any(), any())).thenReturn(logoutResp);

      ClientHandler handler = new ClientHandler(socket, dispatcher, broadcastChannel);

      handler.run();

      verify(broadcastChannel, never()).unregisterUser(anyInt(), any());
    }

    @Test
    @DisplayName("JSON không hợp lệ")
    void invalidJson() throws Exception {
      // EOF ngay lập tức
      InputStream is = new ByteArrayInputStream(new byte[0]);

      when(socket.getInputStream()).thenReturn(is);
      when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

      when(socket.isClosed()).thenReturn(false);

      ClientHandler handler = new ClientHandler(socket, dispatcher, broadcastChannel);

      assertDoesNotThrow(handler::run);

      verify(dispatcher, never()).dispatch(any(), any());
    }

    @Test
    @DisplayName("Length <= 0")
    void zeroLength() throws Exception {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      new DataOutputStream(baos).writeInt(0);

      InputStream is = new ByteArrayInputStream(baos.toByteArray());

      when(socket.getInputStream()).thenReturn(is);
      when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

      when(socket.isClosed()).thenReturn(false);

      ClientHandler handler = new ClientHandler(socket, dispatcher, broadcastChannel);

      handler.run();

      verify(dispatcher, never()).dispatch(any(), any());
    }

    @Test
    @DisplayName("Length quá lớn")
    void tooLargeLength() throws Exception {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      new DataOutputStream(baos).writeInt(16 * 1024 * 1024 + 1);

      InputStream is = new ByteArrayInputStream(baos.toByteArray());

      when(socket.getInputStream()).thenReturn(is);
      when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

      when(socket.isClosed()).thenReturn(false);

      ClientHandler handler = new ClientHandler(socket, dispatcher, broadcastChannel);

      handler.run();

      verify(dispatcher, never()).dispatch(any(), any());
    }

    @Test
    @DisplayName("EOFException → cleanup")
    void eofException() throws Exception {
      InputStream is = new ByteArrayInputStream(new byte[0]);

      when(socket.getInputStream()).thenReturn(is);
      when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

      when(socket.isClosed()).thenReturn(false);

      ClientHandler handler = new ClientHandler(socket, dispatcher, broadcastChannel);

      handler.run();

      verify(broadcastChannel).removeClient(handler);
    }
  }

  // =========================================================
  // send()
  // =========================================================

  @Nested
  @DisplayName("send()")
  class SendTest {

    @Test
    @DisplayName("send() khi out null")
    void outNull() {
      ClientHandler handler = new ClientHandler(socket, dispatcher, broadcastChannel);

      ServerResponse resp = ServerResponse.reply("T", "req-send")
          .success(true)
          .message("m")
          .build();

      assertDoesNotThrow(() -> handler.send(resp));
    }

    @Test
    @DisplayName("send() khi socket closed")
    void socketClosed() throws Exception {
      ClientHandler handler = new ClientHandler(socket, dispatcher, broadcastChannel);

      assertDoesNotThrow(() ->
          handler.send(
              ServerResponse.reply("T", "req-send")
                  .success(true)
                  .message("m")
                  .build()
          )
      );
    }
  }

  // =========================================================
  // cleanup()
  // =========================================================

  @Test
  @DisplayName("cleanup() - socket đã closed")
  void cleanupSocketAlreadyClosed() throws Exception {
    InputStream is = new ByteArrayInputStream(new byte[0]);

    when(socket.getInputStream()).thenReturn(is);
    when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    when(socket.isClosed()).thenReturn(false, true);

    ClientHandler handler = new ClientHandler(socket, dispatcher, broadcastChannel);

    handler.run();

    verify(socket, never()).close();
    verify(broadcastChannel).removeClient(handler);
  }
}