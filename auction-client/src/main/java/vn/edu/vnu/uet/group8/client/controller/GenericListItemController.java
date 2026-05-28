package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class GenericListItemController {

    @FXML private HBox rowRoot;
    @FXML private StackPane iconContainer;
    @FXML private Label lblIcon;
    @FXML private Label lblTitle;
    @FXML private Label lblSubtitle;
    @FXML private Label lblValue;

    public void setData(String icon, String title, String subtitle, String value, String valueColor, String iconBgColor) {
        if (lblIcon != null) lblIcon.setText(icon);
        if (lblTitle != null) lblTitle.setText(title);
        if (lblSubtitle != null) lblSubtitle.setText(subtitle);
        
        if (lblValue != null) {
            lblValue.setText(value);
            if (valueColor != null) {
                lblValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + valueColor + ";");
            }
        }
        
        if (iconContainer != null && iconBgColor != null) {
            iconContainer.setStyle("-fx-background-color: " + iconBgColor + "; -fx-background-radius: 10px;");
        }
    }
}