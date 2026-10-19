package vn.learnhub.shared.web;

import vn.learnhub.shared.study.StudyRules;

import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.*;

@Component("fmt")
public class Format {
    public String orderStatus(Object value) {
        return switch (String.valueOf(value)) {
            case "PENDING" -> "Chờ thanh toán";
            case "UNDERPAID" -> "Thanh toán thiếu";
            case "FAILED" -> "Thanh toán thất bại";
            case "PAID" -> "Đã thanh toán";
            case "CANCELLED" -> "Đã hủy";
            case "REFUNDED" -> "Đã hoàn tiền";
            default -> "Đang cập nhật";
        };
    }

    public String role(Object value) {
        return switch (String.valueOf(value)) {
            case "ADMIN" -> "Quản lý";
            case "TEACHER" -> "Giáo viên";
            default -> "Học viên";
        };
    }

    public String paymentMethod(String value) {
        return "payos".equals(value) ? "Thanh toán trực tuyến" : "Chuyển khoản";
    }

    public String action(String value) {
        return Map.ofEntries(Map.entry("content_saved", "Sửa nội dung"), Map.entry("user_changed", "Sửa tài khoản"),
                Map.entry("consultation_status", "Cập nhật tư vấn"), Map.entry("enrollment_granted", "Mở quyền học"),
                Map.entry("payment_confirmed", "Xác nhận thanh toán"), Map.entry("refund_recorded", "Ghi nhận hoàn tiền"),
                Map.entry("manual_payment_confirmed", "Xác nhận chuyển khoản")).getOrDefault(value, "Cập nhật thông tin");
    }

    public String target(String value) {
        if (value == null || value.isBlank()) return "";
        String[] parts = value.split(":", 2);
        String label = Map.ofEntries(Map.entry("teachers", "Giáo viên"), Map.entry("courses", "Khóa học"),
                Map.entry("chapters", "Chương"), Map.entry("lessons", "Bài học"), Map.entry("paths", "Lộ trình"),
                Map.entry("levels", "Cấp độ"), Map.entry("words", "Từ vựng"), Map.entry("quizzes", "Bài kiểm tra"),
                Map.entry("questions", "Câu hỏi"), Map.entry("choices", "Lựa chọn"), Map.entry("classes", "Lớp học"),
                Map.entry("topics", "Trò chơi"), Map.entry("products", "Chương trình sắp mở")).getOrDefault(parts[0], "Mục");
        return label + " " + (parts.length == 2 ? parts[1] : value);
    }

    public String auditDetail(String value) {
        return value != null && value.contains("whitelist") ? "Đã cập nhật nội dung." : value;
    }

    public String money(long amount) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " ₫";
    }

    public String time(Instant instant) {
        return instant == null ? "—" : DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(StudyRules.ZONE).format(instant);
    }

    public String date(LocalDate date) {
        return date == null ? "Chưa công bố" : date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public List<String> lines(String text) {
        return text == null ? List.of() : text.lines().filter(s -> !s.isBlank()).toList();
    }

    public String initial(String name) {
        return name == null || name.isBlank() ? "H" : name.substring(0, 1).toUpperCase(Locale.ROOT);
    }
}
