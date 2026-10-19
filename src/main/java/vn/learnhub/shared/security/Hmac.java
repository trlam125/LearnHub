package vn.learnhub.shared.security;

import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;

public final class Hmac {
    private Hmac() {
    }

    public static String sign(Map<String, String> data, String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Thiếu checksum key.");
        String text = new TreeMap<>(data).entrySet().stream().map(e -> e.getKey() + "=" + (e.getValue() == null ? "" : e.getValue())).collect(java.util.stream.Collectors.joining("&"));
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean verify(Map<String, String> data, String signature, String key) {
        return key != null && !key.isBlank() && signature != null && MessageDigest.isEqual(sign(data, key).getBytes(StandardCharsets.US_ASCII), signature.getBytes(StandardCharsets.US_ASCII));
    }

    public static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
