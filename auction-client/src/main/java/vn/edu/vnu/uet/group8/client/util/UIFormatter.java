package vn.edu.vnu.uet.group8.client.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Định dạng số tiền (BigDecimal) và thời gian (instant)
 *  - Tiền: Dùng NumberFormat với locale vi_Vn
 *  - Thời gian: Chuyển Instant từ UTC sang múi giờ Asia
 */
public class UIFormatter {
    private static final ZoneId ZONE_VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("vi","VN"));

    public static String formatPrice(BigDecimal price){
        if(price == null) return "0 đ";
        return CURRENCY_FORMAT.format(price);
    }
    public static String formatInstant(Instant instant){
        if(instant == null) return "---";
        ZonedDateTime local = instant.atZone(ZONE_VIETNAM);
        return local.format(DATE_FORMATTER);
    }
    /** Tạo chuỗi đếm ngược HH:MM:SS từ thời điểm kết thúc */
    public static String formatCountdown(Instant endTime){
        if (endTime == null) return "Kết thúc";
        long seconds = Math.max(0,endTime.getEpochSecond() - Instant.now().getEpochSecond());
        long hours = seconds/3600;
        long minutes = (seconds % 3600)/60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours,minutes,secs);
    }
}
