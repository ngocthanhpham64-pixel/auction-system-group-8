package vn.edu.vnu.uet.group8.client.controller;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import vn.edu.vnu.uet.group8.client.service.RatingService;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.model.ReviewDTO;

public class BuyerReviewsController {

    @FXML private VBox vboxBuyerReviewsList;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    @FXML
    public void initialize() {
        int currentUserId = SessionManager.getUserId();
        
        RatingService.loadSellerReviews(currentUserId, reviews -> Platform.runLater(() -> {
            vboxBuyerReviewsList.getChildren().clear();
            
            if (reviews == null || reviews.isEmpty()) {
                Label emptyLabel = new Label("Chưa có đánh giá nào từ người mua.");
                emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic; -fx-padding: 10;");
                vboxBuyerReviewsList.getChildren().add(emptyLabel);
                return;
            }

            for (ReviewDTO review : reviews) {
                vboxBuyerReviewsList.getChildren().add(createReviewCard(review));
            }
        }));
    }

    private HBox createReviewCard(ReviewDTO review) {
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setSpacing(15.0);
        hbox.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-border-color: #e2e8f0; -fx-border-radius: 12px; -fx-padding: 12px;");

        StackPane avatarPane = new StackPane();
        avatarPane.setMinHeight(45.0);
        avatarPane.setMinWidth(45.0);
        avatarPane.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 10px;");
        HBox.setHgrow(avatarPane, Priority.NEVER);

        String username = review.getRaterUsername() != null ? review.getRaterUsername() : "Anonymous";
        String avatarText = username.isEmpty() ? "U" : username.substring(0, 1).toUpperCase();
        Label avatarLabel = new Label(avatarText);
        avatarLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 18px;");
        avatarPane.getChildren().add(avatarLabel);

        VBox contentVBox = new VBox(2.0);
        HBox.setHgrow(contentVBox, Priority.ALWAYS);

        Label usernameLabel = new Label(username);
        usernameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-font-size: 13px;");
        
        Label reviewLabel = new Label(review.getComment() != null ? review.getComment() : "");
        reviewLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #10b981; -fx-wrap-text: true;");

        // Optional: show stars
        Label starsLabel = new Label("⭐".repeat(Math.max(0, review.getScore())));
        starsLabel.setStyle("-fx-text-fill: #eab308; -fx-font-size: 11px; -fx-font-weight: bold;");

        contentVBox.getChildren().addAll(usernameLabel, starsLabel, reviewLabel);

        Region spacer = new Region();
        spacer.setMinWidth(10.0);
        HBox.setHgrow(spacer, Priority.NEVER);

        VBox timeVBox = new VBox();
        String dateText = review.getCreatedAt() != null
                ? DATE_FORMATTER.format(review.getCreatedAt())
                : "Không rõ thời gian";
        Label timeLabel = new Label(dateText);
        timeLabel.setAlignment(Pos.CENTER_RIGHT);
        timeLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        timeVBox.getChildren().add(timeLabel);

        hbox.getChildren().addAll(avatarPane, contentVBox, spacer, timeVBox);
        return hbox;
    }
}
