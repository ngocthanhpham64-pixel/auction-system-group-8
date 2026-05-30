package vn.edu.vnu.uet.group8.server.util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {
  private static final Logger log = LoggerFactory.getLogger(FileUtil.class);

  // Thư mục lưu ảnh trên ổ cứng máy chủ
  private static final String UPLOAD_DIR = "uploads";
  // Tên miền của Mini HTTP Server
  private static final String SERVER_URL = "http://localhost:8081/uploads/";

  static {
    // Tự động tạo thư mục uploads nếu chưa có
    File uploadDir = new File(UPLOAD_DIR);
    if (!uploadDir.exists()) {
      uploadDir.mkdirs();
    }
  }

  public static List<String> saveBase64Images(List<String> base64Images) {
    List<String> savedUrls = new ArrayList<>();
    if (base64Images == null) {
      return savedUrls;
    }

    for (String base64Data : base64Images) {
      if (base64Data == null) {
        continue;
      }
      String cleaned = base64Data.trim();
      if (cleaned.isEmpty()) {
        continue;
      }
      // Nếu data gửi lên đã là Link HTTP (trường hợp Edit giữ nguyên ảnh cũ)
      if (cleaned.startsWith("http")) {
        savedUrls.add(cleaned);
        continue;
      }

      try {
        int base64Idx = cleaned.indexOf("base64,");
        if (base64Idx != -1) {
          cleaned = cleaned.substring(base64Idx + "base64,".length());
        }
        cleaned = cleaned.replaceAll("\\s+", "");

        String fileName = UUID.randomUUID().toString() + ".jpg";
        byte[] decodedBytes = Base64.getDecoder().decode(cleaned);
        try (FileOutputStream fos = new FileOutputStream(UPLOAD_DIR + File.separator + fileName)) {
          fos.write(decodedBytes);
        }
        savedUrls.add(SERVER_URL + fileName);
      } catch (Exception e) {
        log.error("Lỗi khi lưu ảnh: {}", e.getMessage());
      }
    }
    return savedUrls;
  }
}
