package vn.edu.vnu.uet.group8.server.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FileUtilTest {

  private final List<String> createdFiles = new ArrayList<>();

  @AfterEach
  void cleanUp() {
    for (String url : createdFiles) {
      if (url.startsWith("http://localhost:8081/uploads/")) {
        String fileName = url.substring("http://localhost:8081/uploads/".length());
        File file = new File("uploads" + File.separator + fileName);
        if (file.exists()) {
          file.delete();
        }
      }
    }
    createdFiles.clear();
  }

  @Test
  @DisplayName("saveBase64Images với tham số null -> trả về danh sách rỗng")
  void nullList() {
    List<String> result = FileUtil.saveBase64Images(null);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("saveBase64Images với danh sách rỗng -> trả về danh sách rỗng")
  void emptyList() {
    List<String> result = FileUtil.saveBase64Images(List.of());
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("saveBase64Images với link HTTP có sẵn -> giữ nguyên không thay đổi")
  void httpUrlsKept() {
    List<String> inputs = List.of("http://example.com/image.jpg", "https://foo.bar/pic.png");
    List<String> result = FileUtil.saveBase64Images(inputs);
    assertEquals(2, result.size());
    assertEquals("http://example.com/image.jpg", result.get(0));
    assertEquals("https://foo.bar/pic.png", result.get(1));
  }

  @Test
  @DisplayName("saveBase64Images với chuỗi Base64 hợp lệ -> lưu file thành công")
  void validBase64() {
    String base64Data = Base64.getEncoder().encodeToString("Hello World".getBytes());
    List<String> result = FileUtil.saveBase64Images(List.of(base64Data));
    assertEquals(1, result.size());
    String url = result.get(0);
    assertTrue(url.startsWith("http://localhost:8081/uploads/"));
    createdFiles.add(url);

    String fileName = url.substring("http://localhost:8081/uploads/".length());
    File savedFile = new File("uploads" + File.separator + fileName);
    assertTrue(savedFile.exists());

    assertDoesNotThrow(
        () -> {
          byte[] fileBytes = Files.readAllBytes(savedFile.toPath());
          assertEquals("Hello World", new String(fileBytes));
        });
  }

  @Test
  @DisplayName("saveBase64Images với Data URI Scheme -> bóc tách prefix và lưu thành công")
  void base64WithDataUriScheme() {
    String originalContent = "Some text to encode";
    String base64Content = Base64.getEncoder().encodeToString(originalContent.getBytes());
    String dataUri = "data:image/jpeg;base64," + base64Content;

    List<String> result = FileUtil.saveBase64Images(List.of(dataUri));
    assertEquals(1, result.size());
    String url = result.get(0);
    assertTrue(url.startsWith("http://localhost:8081/uploads/"));
    createdFiles.add(url);

    String fileName = url.substring("http://localhost:8081/uploads/".length());
    File savedFile = new File("uploads" + File.separator + fileName);
    assertTrue(savedFile.exists());

    assertDoesNotThrow(
        () -> {
          byte[] fileBytes = Files.readAllBytes(savedFile.toPath());
          assertEquals(originalContent, new String(fileBytes));
        });
  }

  @Test
  @DisplayName("saveBase64Images với chuỗi Base64 chứa khoảng trắng và xuống dòng -> lưu thành công")
  void base64WithWhitespace() {
    String originalContent = "Text with spaces and newlines";
    String base64Content = Base64.getEncoder().encodeToString(originalContent.getBytes());
    String dataUri =
        "\n data:image/png;base64, \r\n "
            + base64Content.substring(0, 5)
            + " \t\n "
            + base64Content.substring(5)
            + " \n";

    List<String> result = FileUtil.saveBase64Images(List.of(dataUri));
    assertEquals(1, result.size());
    String url = result.get(0);
    assertTrue(url.startsWith("http://localhost:8081/uploads/"));
    createdFiles.add(url);

    String fileName = url.substring("http://localhost:8081/uploads/".length());
    File savedFile = new File("uploads" + File.separator + fileName);
    assertTrue(savedFile.exists());

    assertDoesNotThrow(
        () -> {
          byte[] fileBytes = Files.readAllBytes(savedFile.toPath());
          assertEquals(originalContent, new String(fileBytes));
        });
  }

  @Test
  @DisplayName("saveBase64Images với phần tử null hoặc rỗng -> bỏ qua không ném ngoại lệ")
  void invalidOrNullElements() {
    List<String> inputs = new ArrayList<>();
    inputs.add(null);
    inputs.add("invalid-base64-string-that-causes-decode-exception!!!");

    List<String> result = FileUtil.saveBase64Images(inputs);
    assertTrue(result.isEmpty());
  }
}
