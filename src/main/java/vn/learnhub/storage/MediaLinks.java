package vn.learnhub.storage;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

/** Link validation only. Never fetch provider URLs on the application server. */
public final class MediaLinks {
    private static final Pattern DRIVE_FILE = Pattern.compile("^/file/d/([A-Za-z0-9_-]+)(?:/.*)?$");

    private MediaLinks() { }

    public static String checked(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty() || value.length() > 2048 || value.chars().anyMatch(c -> Character.isISOControl(c) || c == '\\'))
            throw new IllegalArgumentException("Dán liên kết chia sẻ đầy đủ của video hoặc tài liệu.");
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getRawUserInfo() != null
                    || uri.getPort() != -1 && uri.getPort() != 443)
                throw new IllegalArgumentException();
            String normalized = uri.toASCIIString();
            if (normalized.length() > 2048) throw new IllegalArgumentException();
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Liên kết chưa hợp lệ. Hãy sao chép liên kết chia sẻ bắt đầu bằng https://.");
        }
    }

    public static String embed(String sharedLink, String suppliedEmbed) {
        String shared = checked(sharedLink);
        if (suppliedEmbed != null && !suppliedEmbed.isBlank()) {
            String candidate = checked(suppliedEmbed);
            URI uri = URI.create(candidate);
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            boolean drive = host.equals("drive.google.com") && DRIVE_FILE.matcher(uri.getPath()).matches() && uri.getPath().endsWith("/preview");
            boolean oneDrive = host.equals("onedrive.live.com") && uri.getPath().equals("/embed");
            boolean sharePoint = host.endsWith(".sharepoint.com") && uri.getPath().endsWith("/_layouts/15/embed.aspx");
            if (!drive && !oneDrive && !sharePoint)
                throw new IllegalArgumentException("Liên kết xem trong bài cần lấy từ Google Drive hoặc mục Nhúng của OneDrive.");
            return candidate;
        }
        URI uri = URI.create(shared);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.equals("onedrive.live.com") && uri.getPath().equals("/embed")) return shared;
        if (!host.equals("drive.google.com")) return "";
        var match = DRIVE_FILE.matcher(uri.getPath());
        Map<String, String> query = query(uri.getRawQuery());
        String id = match.matches() ? match.group(1)
                : (uri.getPath().equals("/open") || uri.getPath().equals("/uc")) ? query.getOrDefault("id", "") : "";
        if (!id.matches("[A-Za-z0-9_-]+")) return "";
        String key = query.getOrDefault("resourcekey", "");
        return checked("https://drive.google.com/file/d/" + id + "/preview"
                + (key.isEmpty() ? "" : "?resourcekey=" + URLEncoder.encode(key, StandardCharsets.UTF_8)));
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> values = new LinkedHashMap<>();
        if (raw != null)
            for (String pair : raw.split("&")) {
                String[] item = pair.split("=", 2);
                values.put(URLDecoder.decode(item[0], StandardCharsets.UTF_8), item.length == 2 ? URLDecoder.decode(item[1], StandardCharsets.UTF_8) : "");
            }
        return values;
    }
}
