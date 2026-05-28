package vn.edu.vnu.uet.group8.client;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;

public abstract class TestFXSetup {

    private static boolean initialized = false;

    @BeforeAll
    static void initJfx() {
        if (!initialized) {
            initialized = true;

            try {
                Platform.startup(() -> {});
            } catch (IllegalStateException ignored) {
            }
        }
    }
}