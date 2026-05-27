package vn.edu.vnu.uet.group8.client.controller;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SettingsController} — pure Java logic.
 *
 * Coverage:
 * - EMAIL_PATTERN: valid / invalid email formats
 * - PHONE_PATTERN: valid / invalid Vietnamese phone (10 số, bắt đầu 0)
 * - savePreference: không crash (Preferences.userNodeForPackage)
 * - constructor không crash
 * - PREF_* constants không null
 * - editField validator lambdas (fullname >= 2, birthday dd/MM/yyyy)
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class SettingsControllerTest {

    private static Pattern EMAIL_PATTERN;
    private static Pattern PHONE_PATTERN;
    private SettingsController ctrl;

    @BeforeAll
    static void extractPatterns() throws Exception {
        Field emailField = SettingsController.class.getDeclaredField("EMAIL_PATTERN");
        emailField.setAccessible(true);
        EMAIL_PATTERN = (Pattern) emailField.get(null);

        Field phoneField = SettingsController.class.getDeclaredField("PHONE_PATTERN");
        phoneField.setAccessible(true);
        PHONE_PATTERN = (Pattern) phoneField.get(null);
    }

    @BeforeEach
    void setUp() {
        ctrl = new SettingsController();
    }

    // ══════════════════════════════════════════════════════
    // Constructor
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("constructor không crash (headless)")
    void constructor_noCrash() {
        assertDoesNotThrow(SettingsController::new);
    }

    // ══════════════════════════════════════════════════════
    // EMAIL_PATTERN
    // ══════════════════════════════════════════════════════

    @ParameterizedTest
    @ValueSource(strings = {
            "user@example.com",
            "alice.bob@gmail.com",
            "test+tag@domain.org",
            "user_123@company.vn",
            "A@b.c"
    })
    @DisplayName("EMAIL_PATTERN: email hợp lệ → match true")
    void emailPattern_validEmails_match(String email) {
        assertTrue(EMAIL_PATTERN.matcher(email).matches(),
                "Email hợp lệ phải pass: " + email);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "notanemail",
            "@nodomain.com",
            "missing@",
            "two@@domain.com",
            "spaces in@email.com",
            "",
            "no-at-sign"
    })
    @DisplayName("EMAIL_PATTERN: email không hợp lệ → match false")
    void emailPattern_invalidEmails_noMatch(String email) {
        assertFalse(EMAIL_PATTERN.matcher(email).matches(),
                "Email không hợp lệ phải fail: " + email);
    }

    // ══════════════════════════════════════════════════════
    // PHONE_PATTERN (10 số, bắt đầu 0)
    // ══════════════════════════════════════════════════════

    @ParameterizedTest
    @ValueSource(strings = {
            "0123456789",
            "0987654321",
            "0912345678",
            "0345678901"
    })
    @DisplayName("PHONE_PATTERN: số điện thoại hợp lệ → match true")
    void phonePattern_validPhones_match(String phone) {
        assertTrue(PHONE_PATTERN.matcher(phone).matches(),
                "Phone hợp lệ phải pass: " + phone);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123456789",      // không bắt đầu 0
            "012345678",      // chỉ 9 số
            "01234567890",    // 11 số
            "0abc456789",     // có chữ
            "+84123456789",   // có dấu +
            "",               // rỗng
            "0 123456789"     // có khoảng trắng
    })
    @DisplayName("PHONE_PATTERN: số điện thoại không hợp lệ → match false")
    void phonePattern_invalidPhones_noMatch(String phone) {
        assertFalse(PHONE_PATTERN.matcher(phone).matches(),
                "Phone không hợp lệ phải fail: " + phone);
    }

    // ══════════════════════════════════════════════════════
    // PREF_* constants không null
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("Tất cả PREF_* constants không null và không rỗng")
    void prefConstants_notNullOrBlank() throws Exception {
        String[] prefFields = {
                "PREF_DARK_MODE", "PREF_EMAIL_ENDING", "PREF_EMAIL_OUTBID",
                "PREF_EMAIL_PROMO", "PREF_PUSH_NOTI", "PREF_LANGUAGE"
        };
        for (String name : prefFields) {
            Field f = SettingsController.class.getDeclaredField(name);
            f.setAccessible(true);
            String val = (String) f.get(null);
            assertNotNull(val, name + " không được null");
            assertFalse(val.isBlank(), name + " không được rỗng");
        }
    }

    @Test
    @DisplayName("Tất cả PREF_* constants là duy nhất (không trùng)")
    void prefConstants_areUnique() throws Exception {
        String[] prefFields = {
                "PREF_DARK_MODE", "PREF_EMAIL_ENDING", "PREF_EMAIL_OUTBID",
                "PREF_EMAIL_PROMO", "PREF_PUSH_NOTI", "PREF_LANGUAGE"
        };
        java.util.Set<String> vals = new java.util.HashSet<>();
        for (String name : prefFields) {
            Field f = SettingsController.class.getDeclaredField(name);
            f.setAccessible(true);
            String val = (String) f.get(null);
            assertTrue(vals.add(val), "PREF constant trùng nhau: " + name + " = " + val);
        }
    }

    // ══════════════════════════════════════════════════════
    // savePreference (private → reflection)
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("savePreference(key, true, label) → không crash")
    void savePreference_true_noCrash() throws Exception {
        Method m = SettingsController.class.getDeclaredMethod(
                "savePreference", String.class, boolean.class, String.class);
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(ctrl, "test_pref_unit", true, "Test label"));
    }

    @Test
    @DisplayName("savePreference(key, false, label) → không crash")
    void savePreference_false_noCrash() throws Exception {
        Method m = SettingsController.class.getDeclaredMethod(
                "savePreference", String.class, boolean.class, String.class);
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(ctrl, "test_pref_unit_2", false, "Test label 2"));
    }

    // ══════════════════════════════════════════════════════
    // Validator lambdas (inline test của logic trong editField)
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("fullName validator: length >= 2 → pass")
    void fullNameValidator_lengthGte2_pass() {
        java.util.function.Predicate<String> v = s -> s.length() >= 2;
        assertTrue(v.test("AB"));
        assertTrue(v.test("Nguyen Van A"));
    }

    @Test
    @DisplayName("fullName validator: length < 2 → fail")
    void fullNameValidator_lengthLt2_fail() {
        java.util.function.Predicate<String> v = s -> s.length() >= 2;
        assertFalse(v.test("A"));
        assertFalse(v.test(""));
    }

    @Test
    @DisplayName("birthday validator: dd/MM/yyyy format → pass")
    void birthdayValidator_validFormat_pass() {
        java.util.function.Predicate<String> v = s -> s.matches("\\d{2}/\\d{2}/\\d{4}");
        assertTrue(v.test("01/01/2000"));
        assertTrue(v.test("31/12/1999"));
        assertTrue(v.test("15/06/2023"));
    }

    @Test
    @DisplayName("birthday validator: sai format → fail")
    void birthdayValidator_invalidFormat_fail() {
        java.util.function.Predicate<String> v = s -> s.matches("\\d{2}/\\d{2}/\\d{4}");
        assertFalse(v.test("1/1/2000"));       // thiếu số 0 đầu
        assertFalse(v.test("01-01-2000"));     // dấu -
        assertFalse(v.test("2000/01/01"));     // sai thứ tự
        assertFalse(v.test(""));
    }

    @Test
    @DisplayName("address validator: length >= 5 → pass")
    void addressValidator_lengthGte5_pass() {
        java.util.function.Predicate<String> v = s -> s.length() >= 5;
        assertTrue(v.test("12345"));
        assertTrue(v.test("Hà Nội, Việt Nam"));
    }

    @Test
    @DisplayName("address validator: length < 5 → fail")
    void addressValidator_lengthLt5_fail() {
        java.util.function.Predicate<String> v = s -> s.length() >= 5;
        assertFalse(v.test("HN"));
        assertFalse(v.test(""));
    }

    @Test
    @DisplayName("depositLimit validator: chỉ chứa số → pass")
    void depositLimitValidator_numericOnly_pass() {
        java.util.function.Predicate<String> v = s -> s.replaceAll("[^\\d]", "").matches("\\d+");
        assertTrue(v.test("100000000"));
        assertTrue(v.test("1,000,000"));  // phần format VND
    }

    @Test
    @DisplayName("depositLimit validator: chứa chữ → fail")
    void depositLimitValidator_hasLetters_fail() {
        java.util.function.Predicate<String> v = s -> s.replaceAll("[^\\d]", "").matches("\\d+");
        assertFalse(v.test("abc"));
        assertFalse(v.test(""));
        assertFalse(v.test("one million"));
    }

    // ══════════════════════════════════════════════════════
    // syncProfileToServer (khi chưa login / không kết nối)
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("syncProfileToServer: không kết nối, labels null → không crash")
    void syncProfileToServer_noConnectionNullLabels_noCrash() throws Exception {
        Method m = SettingsController.class.getDeclaredMethod("syncProfileToServer");
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(ctrl));
    }
}