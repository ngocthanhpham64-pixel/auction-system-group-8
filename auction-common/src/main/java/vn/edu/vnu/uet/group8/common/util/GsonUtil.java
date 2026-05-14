package vn.edu.vnu.uet.group8.common.util;

import java.time.Instant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

/**
 * GsonUtil - Trung tâm xử lý JSON của cả hệ thống.
 * Đảm bảo mọi nơi đều dùng chung một quy tắc format dữ liệu.
 */
public class GsonUtil {

  // Tạo một instance duy nhất và dùng hằng số để tối ưu hiệu suất
  public static final Gson GSON = new GsonBuilder()
          // Đăng ký adapter để xử lý kiểu Instant (mặc định Gson không hiểu Java 8 Time)
          .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>)
                  (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
          .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>)
                  (json, typeOfT, context) -> Instant.parse(json.getAsString()))

          // Nếu bạn muốn JSON khi in ra log trông đẹp, dễ đọc thì bật dòng dưới:
          // .setPrettyPrinting()

          .create();

  // Không cho phép khởi tạo object này
  private GsonUtil() {}
}