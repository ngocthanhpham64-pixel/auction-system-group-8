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

    public static void setCircularAvatar(javafx.scene.image.ImageView imageView, javafx.scene.control.Label fallbackLabel, String avatarUrl, double size) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            if (imageView != null) imageView.setVisible(false);
            if (fallbackLabel != null) fallbackLabel.setVisible(true);
            return;
        }

        String resolvedUrl = avatarUrl;
        if (resolvedUrl.startsWith("http://localhost:8081")) {
            String serverHost = vn.edu.vnu.uet.group8.client.networking.AuctionClient.getInstance().getHost();
            if (serverHost != null && !serverHost.equalsIgnoreCase("localhost")) {
                resolvedUrl = resolvedUrl.replace("localhost", serverHost);
            }
        }

        try {
            javafx.scene.image.Image img = new javafx.scene.image.Image(resolvedUrl, true);
            img.errorProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    javafx.application.Platform.runLater(() -> {
                        if (imageView != null) imageView.setVisible(false);
                        if (fallbackLabel != null) fallbackLabel.setVisible(true);
                    });
                }
            });
            img.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() == 1.0 && !img.isError()) {
                    javafx.application.Platform.runLater(() -> {
                        applyImageToImageView(imageView, fallbackLabel, img, size);
                    });
                }
            });

            if (!img.isError() && img.getProgress() == 1.0) {
                applyImageToImageView(imageView, fallbackLabel, img, size);
            }
        } catch (Exception e) {
            if (imageView != null) imageView.setVisible(false);
            if (fallbackLabel != null) fallbackLabel.setVisible(true);
        }
    }

    public static void applyImageToImageView(javafx.scene.image.ImageView imageView, javafx.scene.control.Label fallbackLabel, javafx.scene.image.Image img, double size) {
        if (imageView != null) {
            double w = img.getWidth();
            double h = img.getHeight();
            if (w > 0 && h > 0) {
                double minDim = Math.min(w, h);
                double startX = (w - minDim) / 2;
                double startY = (h - minDim) / 2;
                imageView.setViewport(new javafx.geometry.Rectangle2D(startX, startY, minDim, minDim));
            }
            imageView.setImage(img);
            imageView.setFitWidth(size);
            imageView.setFitHeight(size);
            imageView.setPreserveRatio(true);
            
            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(size / 2, size / 2, size / 2);
            imageView.setClip(clip);
            imageView.setVisible(true);
        }
        if (fallbackLabel != null) fallbackLabel.setVisible(false);
    }
}
