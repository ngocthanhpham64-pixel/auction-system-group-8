package vn.edu.vnu.uet.group8.client.controller;

import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class khởi tạo JavaFX toolkit một lần duy nhất cho tất cả controller tests.
 * Kế thừa class này để dùng TextField, Label, Button... mà không bị "Toolkit not initialized".
 */
public abstract class FxTestBase {

    @BeforeAll
    static void initToolkit() {
        new JFXPanel(); // Khởi tạo JavaFX runtime - đủ để tạo FX controls
    }
}