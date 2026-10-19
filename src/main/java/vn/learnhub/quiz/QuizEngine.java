package vn.learnhub.quiz;

import java.math.*;
import java.util.*;

public final class QuizEngine {
    private QuizEngine() {
    }

    public record Choice(long id, String text, boolean correct) {
    }

    public record Question(long id, String text, String kind, int points, String explanation, List<Choice> choices) {
    }

    public record PublicChoice(long id, String text, boolean selected) {
    }

    public record PublicQuestion(long id, String text, String kind, int points, List<PublicChoice> choices) {
    }

    public record Detail(String question, boolean correct, int points, String explanation, List<Choice> choices,
                         List<Long> selected) {
    }

    public record Result(BigDecimal score, List<Detail> details) {
    }

    public static void validate(List<Question> questions) {
        if (questions.isEmpty()) throw new IllegalArgumentException("Bài kiểm tra chưa có câu hỏi.");
        Set<Long> ids = new HashSet<>();
        for (var q : questions) {
            long count = q.choices().stream().filter(Choice::correct).count();
            if (!ids.add(q.id()) || q.points() < 1 || q.choices().size() < 2 || count == 0 || !(q.kind().equals("single") || q.kind().equals("multiple")) || q.kind().equals("single") && count != 1)
                throw new IllegalArgumentException("Bài kiểm tra chưa hoàn thiện. Vui lòng quay lại sau.");
        }
    }

    public static Map<String, List<Long>> clean(List<Question> questions, Map<String, List<Long>> payload) {
        if (payload == null) throw new IllegalArgumentException("Đáp án không hợp lệ.");
        Map<String, List<Long>> result = new LinkedHashMap<>();
        for (var q : questions) {
            List<Long> raw = payload.getOrDefault(Long.toString(q.id()), List.of());
            if (raw == null || raw.stream().anyMatch(Objects::isNull))
                throw new IllegalArgumentException("Đáp án không hợp lệ.");
            var selected = new TreeSet<>(raw);
            Set<Long> allowed = new HashSet<>();
            q.choices().forEach(c -> allowed.add(c.id()));
            if (!allowed.containsAll(selected) || (q.kind().equals("single") && selected.size() > 1))
                throw new IllegalArgumentException("Đáp án không thuộc câu hỏi.");
            result.put(Long.toString(q.id()), new ArrayList<>(selected));
        }
        return result;
    }

    public static Result grade(List<Question> questions, Map<String, List<Long>> raw) {
        validate(questions);
        var answers = clean(questions, raw);
        long total = 0, earned = 0;
        List<Detail> details = new ArrayList<>();
        for (var q : questions) {
            var selected = answers.get(Long.toString(q.id()));
            var correct = q.choices().stream().filter(Choice::correct).map(Choice::id).sorted().toList();
            boolean ok = selected.equals(correct);
            total += q.points();
            if (ok) earned += q.points();
            details.add(new Detail(q.text(), ok, ok ? q.points() : 0, q.explanation(), q.choices(), selected));
        }
        return new Result(BigDecimal.valueOf(earned).multiply(BigDecimal.TEN).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP), details);
    }

    public static List<PublicQuestion> publicView(List<Question> questions, Map<String, List<Long>> answers) {
        return questions.stream().map(q -> new PublicQuestion(q.id(), q.text(), q.kind(), q.points(), q.choices().stream()
                .map(c -> new PublicChoice(c.id(), c.text(), answers.getOrDefault(Long.toString(q.id()), List.of()).contains(c.id()))).toList())).toList();
    }

    public static int reward(BigDecimal score, BigDecimal best) {
        return Math.max(0, score.multiply(BigDecimal.valueOf(5)).intValue() - best.multiply(BigDecimal.valueOf(5)).intValue());
    }
}
