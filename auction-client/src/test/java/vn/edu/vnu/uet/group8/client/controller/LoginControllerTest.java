package vn.edu.vnu.uet.group8.client.controller;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.vnu.uet.group8.client.util.SessionManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LoginController")
class LoginControllerTest extends FxTestBase {

    private LoginController controller;

    @BeforeEach
    void setup() throws Exception {

        SessionManager.clearSession();

        controller = new LoginController();

        setField("tfEmail", new TextField());
        setField("pfPassword", new PasswordField());
        setField("lblLoginError", new Label());
        setField("btnLoginSubmit",
                new Button("Đăng nhập"));

        setField("tfRegUsername",
                new TextField());

        setField("tfRegFullName",
                new TextField());

        setField("tfRegEmail",
                new TextField());

        setField("tfRegPhone",
                new TextField());

        setField("pfRegPassword",
                new PasswordField());

        setField("pfRegConfirm",
                new PasswordField());

        setField("cbRegTerms",
                new CheckBox());

        setField("lblRegError",
                new Label());

        setField("btnRegSubmit",
                new Button("Đăng ký"));
    }

    // =====================================================
    // Reflection helpers
    // =====================================================

    private void setField(String name, Object value)
            throws Exception {

        Field field =
                LoginController.class.getDeclaredField(name);

        field.setAccessible(true);

        field.set(controller, value);
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(String name)
            throws Exception {

        Field field =
                LoginController.class.getDeclaredField(name);

        field.setAccessible(true);

        return (T) field.get(controller);
    }

    private Object call(String methodName,
                        Class<?>[] types,
                        Object... args)
            throws Exception {

        Method method =
                LoginController.class.getDeclaredMethod(
                        methodName,
                        types
                );

        method.setAccessible(true);

        return method.invoke(controller, args);
    }

    // =====================================================
    // Login validation
    // =====================================================

    @Test
    void empty_email_shows_error()
            throws Exception {

        TextField email =
                getField("tfEmail");

        PasswordField pass =
                getField("pfPassword");

        Label label =
                getField("lblLoginError");

        email.setText("");
        pass.setText("123456");

        call("onLogin",
                new Class<?>[]{});

        assertTrue(label.isVisible());

        assertEquals(
                "Vui lòng nhập email",
                label.getText()
        );
    }

    @Test
    void invalid_email_shows_error()
            throws Exception {

        TextField email =
                getField("tfEmail");

        PasswordField pass =
                getField("pfPassword");

        Label label =
                getField("lblLoginError");

        email.setText("abc");

        pass.setText("123456");

        call("onLogin",
                new Class<?>[]{});

        assertTrue(label.isVisible());

        assertEquals(
                "Email không hợp lệ",
                label.getText()
        );
    }

    @Test
    void empty_password_shows_error()
            throws Exception {

        TextField email =
                getField("tfEmail");

        PasswordField pass =
                getField("pfPassword");

        Label label =
                getField("lblLoginError");

        email.setText("a@b.com");

        pass.setText("");

        call("onLogin",
                new Class<?>[]{});

        assertTrue(label.isVisible());

        assertEquals(
                "Vui lòng nhập mật khẩu",
                label.getText()
        );
    }

    // =====================================================
    // Mock login
    // =====================================================

    @Test
    void mock_admin_login_sets_session()
            throws Exception {

        TextField email =
                getField("tfEmail");

        PasswordField pass =
                getField("pfPassword");

        email.setText("admin@auctiva.com");

        pass.setText("123456");

        call("onLogin",
                new Class<?>[]{});

        assertTrue(
                SessionManager.isLoggedIn()
        );

        assertTrue(
                SessionManager.isAdmin()
        );

        assertEquals(
                "admin",
                SessionManager.getUsername()
        );
    }

    @Test
    void mock_user_login_sets_member_role()
            throws Exception {

        TextField email =
                getField("tfEmail");

        PasswordField pass =
                getField("pfPassword");

        email.setText("user@auctiva.com");

        pass.setText("123456");

        call("onLogin",
                new Class<?>[]{});

        assertTrue(
                SessionManager.isLoggedIn()
        );

        assertFalse(
                SessionManager.isAdmin()
        );

        assertEquals(
                "user",
                SessionManager.getUsername()
        );
    }

    // =====================================================
    // Loading state
    // =====================================================

    @Test
    void set_login_loading_true()
            throws Exception {

        Button btn =
                getField("btnLoginSubmit");

        call(
                "setLoginLoading",
                new Class<?>[]{boolean.class},
                true
        );

        assertTrue(btn.isDisable());

        assertEquals(
                "Đang đăng nhập...",
                btn.getText()
        );
    }

    @Test
    void set_login_loading_false()
            throws Exception {

        Button btn =
                getField("btnLoginSubmit");

        call(
                "setLoginLoading",
                new Class<?>[]{boolean.class},
                true
        );

        call(
                "setLoginLoading",
                new Class<?>[]{boolean.class},
                false
        );

        assertFalse(btn.isDisable());

        assertEquals(
                "Đăng nhập",
                btn.getText()
        );
    }

    // =====================================================
    // validateRegister
    // =====================================================

    @Test
    void validate_register_success()
            throws Exception {

        CheckBox cb =
                getField("cbRegTerms");

        cb.setSelected(true);

        Object result =
                call(
                        "validateRegister",
                        new Class<?>[]{
                                String.class,
                                String.class,
                                String.class,
                                String.class,
                                String.class,
                                String.class
                        },
                        "alice",
                        "Alice Nguyen",
                        "alice@gmail.com",
                        "0123456789",
                        "Pass1234",
                        "Pass1234"
                );

        assertNull(result);
    }

    @Test
    void validate_register_invalid_email()
            throws Exception {

        CheckBox cb =
                getField("cbRegTerms");

        cb.setSelected(true);

        Object result =
                call(
                        "validateRegister",
                        new Class<?>[]{
                                String.class,
                                String.class,
                                String.class,
                                String.class,
                                String.class,
                                String.class
                        },
                        "alice",
                        "Alice Nguyen",
                        "abc",
                        "0123456789",
                        "Pass1234",
                        "Pass1234"
                );

        assertNotNull(result);
    }

    @Test
    void validate_register_password_mismatch()
            throws Exception {

        CheckBox cb =
                getField("cbRegTerms");

        cb.setSelected(true);

        Object result =
                call(
                        "validateRegister",
                        new Class<?>[]{
                                String.class,
                                String.class,
                                String.class,
                                String.class,
                                String.class,
                                String.class
                        },
                        "alice",
                        "Alice Nguyen",
                        "alice@gmail.com",
                        "0123456789",
                        "Pass1234",
                        "Wrong123"
                );

        assertNotNull(result);
    }

    // =====================================================
    // Error display
    // =====================================================

    @Test
    void show_login_error_sets_label()
            throws Exception {

        Label label =
                getField("lblLoginError");

        call(
                "showLoginError",
                new Class<?>[]{String.class},
                "Test error"
        );

        assertTrue(label.isVisible());

        assertEquals(
                "Test error",
                label.getText()
        );
    }

    @Test
    void hide_login_error_hides_label()
            throws Exception {

        Label label =
                getField("lblLoginError");

        label.setVisible(true);

        call(
                "hideLoginError",
                new Class<?>[]{}
        );

        assertFalse(label.isVisible());
    }
}

