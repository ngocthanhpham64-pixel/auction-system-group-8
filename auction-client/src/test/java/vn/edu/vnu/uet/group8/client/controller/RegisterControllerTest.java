package vn.edu.vnu.uet.group8.client.controller;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RegisterController")
class RegisterControllerTest extends FxTestBase {

    private RegisterController controller;

    @BeforeEach
    void setup() {

        controller = new RegisterController();

        controller.tfUsername = new TextField();
        controller.tfFullName = new TextField();
        controller.tfEmail = new TextField();
        controller.tfPhone = new TextField();

        controller.pfPassword = new PasswordField();
        controller.pfConfirmPassword = new PasswordField();

        controller.cbTerms = new CheckBox();

        controller.lblError = new Label();

        controller.btnSubmit = new Button("Dang ky");
    }

    // =====================================================
    // VALIDATE
    // =====================================================

    @Nested
    @DisplayName("validate()")
    class ValidateTests {

        private String validate(
                String username,
                String fullName,
                String email,
                String phone,
                String password,
                String confirm
        ) throws Exception {

            Method method = RegisterController.class.getDeclaredMethod(
                    "validate",
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    String.class
            );

            method.setAccessible(true);

            return (String) method.invoke(
                    controller,
                    username,
                    fullName,
                    email,
                    phone,
                    password,
                    confirm
            );
        }

        @Test
        @DisplayName("field rong -> loi")
        void empty_fields() throws Exception {

            String err = validate(
                    "",
                    "Full Name",
                    "a@b.com",
                    "0123456789",
                    "Pass1234",
                    "Pass1234"
            );

            assertNotNull(err);
        }

        @Test
        @DisplayName("username invalid")
        void username_invalid() throws Exception {

            String err = validate(
                    "abc!",
                    "Full Name",
                    "a@b.com",
                    "0123456789",
                    "Pass1234",
                    "Pass1234"
            );

            assertNotNull(err);
        }

        @Test
        @DisplayName("email invalid")
        void email_invalid() throws Exception {

            String err = validate(
                    "alice",
                    "Full Name",
                    "abc",
                    "0123456789",
                    "Pass1234",
                    "Pass1234"
            );

            assertNotNull(err);
        }

        @Test
        @DisplayName("phone invalid")
        void phone_invalid() throws Exception {

            String err = validate(
                    "alice",
                    "Full Name",
                    "a@b.com",
                    "123",
                    "Pass1234",
                    "Pass1234"
            );

            assertNotNull(err);
        }

        @Test
        @DisplayName("password invalid")
        void password_invalid() throws Exception {

            String err = validate(
                    "alice",
                    "Full Name",
                    "a@b.com",
                    "0123456789",
                    "password",
                    "password"
            );

            assertNotNull(err);
        }

        @Test
        @DisplayName("confirm mismatch")
        void confirm_mismatch() throws Exception {

            String err = validate(
                    "alice",
                    "Full Name",
                    "a@b.com",
                    "0123456789",
                    "Pass1234",
                    "Diff1234"
            );

            assertNotNull(err);
        }

        @Test
        @DisplayName("terms unchecked")
        void terms_unchecked() throws Exception {

            controller.cbTerms.setSelected(false);

            String err = validate(
                    "alice",
                    "Full Name",
                    "a@b.com",
                    "0123456789",
                    "Pass1234",
                    "Pass1234"
            );

            assertNotNull(err);
        }

        @Test
        @DisplayName("all valid -> null")
        void all_valid() throws Exception {

            controller.cbTerms.setSelected(true);

            String err = validate(
                    "alice_99",
                    "Full Name",
                    "alice@example.com",
                    "0123456789",
                    "Pass1234",
                    "Pass1234"
            );

            assertNull(err);
        }
    }

    // =====================================================
    // ERROR DISPLAY
    // =====================================================

    @Nested
    @DisplayName("showError / hideError")
    class ErrorTests {

        @Test
        @DisplayName("showError")
        void show_error() throws Exception {

            Method method = RegisterController.class
                    .getDeclaredMethod("showError", String.class);

            method.setAccessible(true);

            method.invoke(controller, "Test error");

            assertTrue(controller.lblError.isVisible());
            assertEquals("Test error", controller.lblError.getText());
        }

        @Test
        @DisplayName("hideError")
        void hide_error() throws Exception {

            Method show = RegisterController.class
                    .getDeclaredMethod("showError", String.class);

            Method hide = RegisterController.class
                    .getDeclaredMethod("hideError");

            show.setAccessible(true);
            hide.setAccessible(true);

            show.invoke(controller, "Error");
            hide.invoke(controller);

            assertFalse(controller.lblError.isVisible());
        }
    }

    // =====================================================
    // LOADING STATE
    // =====================================================

    @Nested
    @DisplayName("setLoadingState")
    class LoadingStateTests {

        @Test
        @DisplayName("loading true")
        void loading_true() throws Exception {

            Method method = RegisterController.class
                    .getDeclaredMethod("setLoadingState", boolean.class);

            method.setAccessible(true);

            method.invoke(controller, true);

            assertTrue(controller.btnSubmit.isDisable());
            assertEquals(
                    "Dang dang ky...",
                    controller.btnSubmit.getText()
            );
        }

        @Test
        @DisplayName("loading false")
        void loading_false() throws Exception {

            Method method = RegisterController.class
                    .getDeclaredMethod("setLoadingState", boolean.class);

            method.setAccessible(true);

            method.invoke(controller, true);
            method.invoke(controller, false);

            assertFalse(controller.btnSubmit.isDisable());
            assertEquals(
                    "Dang ky",
                    controller.btnSubmit.getText()
            );
        }
    }
}