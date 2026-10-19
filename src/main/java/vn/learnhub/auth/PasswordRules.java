package vn.learnhub.auth;

import java.nio.charset.StandardCharsets;

public final class PasswordRules {
    private PasswordRules() {
    }

    public static boolean valid(String password) {
        return password != null && password.length() >= 8 && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
