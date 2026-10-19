package vn.learnhub.shared.study;

import java.time.*;
import java.util.*;

public final class StudyRules {
    private StudyRules() {
    }

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public static int streak(Collection<LocalDate> dates, LocalDate today) {
        Set<LocalDate> days = new HashSet<>(dates);
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        int count = 0;
        while (days.contains(cursor)) {
            count++;
            cursor = cursor.minusDays(1);
        }
        return count;
    }

    public record Review(int stage, int days) {
    }

    public static Review review(int stage, boolean remembered) {
        if (stage < 0 || stage > 6) throw new IllegalArgumentException("Mức ôn không hợp lệ.");
        int next = remembered ? Math.min(stage + 1, 6) : 0;
        return new Review(next, new int[]{1, 1, 3, 7, 14, 30, 60}[next]);
    }

    public static String safeCsv(String value) {
        if (value == null) return "";
        String result = value;
        String probe = result.stripLeading();
        if ((!probe.isEmpty() && "=+-@".indexOf(probe.charAt(0)) >= 0) || (!result.isEmpty() && "\t\r\n".indexOf(result.charAt(0)) >= 0))
            result = "'" + result;
        return "\"" + result.replace("\"", "\"\"") + "\"";
    }

    public static String safeJsonForHtml(String json) {
        return json.replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026").replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
    }
}
