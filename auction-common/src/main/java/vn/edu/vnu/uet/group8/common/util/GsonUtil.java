package vn.edu.vnu.uet.group8.common.util;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
/**
 * Gson instance dùng chung toàn bộ project, đã cấu hình adapter cho Instant
 */
public final class GsonUtil {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class,
                    (com.google.gson.JsonSerializer<Instant>) (src, typeOfSrc,context) -> context.serialize(src.toString()))
            .registerTypeAdapter(Instant.class,
                    (com.google.gson.JsonDeserializer<Instant>) (json,typeOfT,context)-> Instant.parse(json.getAsString()))
            .create();
    private GsonUtil(){}
    /**
     * Chuyển đổi data (có thể có là LinkTreeMap, sau khi Gson parse) về đối tượng cần
     * @param data dữ liệu thô từ (ServerResponse.getData())
     * @param clazz class của đối tượng cần parse
     * @param <T> kiểu đối tượng
     * @return instance của T, hoặc (null) nếu data là null
     */
    public static <T> T toObject(Object data, Class<T> clazz){
        if(data == null){
            return null;
        }
        //Nếu data đã đúng kiểu, trả về luôn
        if(clazz.isInstance(data)){
            return clazz.cast(data);
        }
        //Còn không thì serialize rồi deserialize lại để ép kiểu đúng
        return GSON.fromJson(GSON.toJson(data),clazz);
    }
    /**
     * Chuyển đổi data thành danh sách đối tượng
     * @param data dữ liệu thô
     * @param clazz class của phần tử trong danh sách
     * @param <T> kiểu phần tử
     * @return danh sách đối tượng, có thể rỗng nhưng không null
     */
    public static <T> List<T> toList(Object data,Class<T> clazz){
        if(data == null){
            return List.of();
        }
        Type listType = TypeToken.getParameterized(List.class,clazz).getType();
        return GSON.fromJson(GSON.toJson(data), listType);
    }
}