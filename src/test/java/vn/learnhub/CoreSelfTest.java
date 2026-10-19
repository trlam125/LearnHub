package vn.learnhub;

import vn.learnhub.auth.PasswordRules;
import vn.learnhub.course.LearningLanguage;
import vn.learnhub.quiz.QuizEngine;
import vn.learnhub.shared.security.Hmac;
import vn.learnhub.shared.study.StudyRules;

import java.time.*;
import java.math.*;
import java.util.*;

public final class CoreSelfTest {
    private static int checks;

    private static void equal(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) throw new AssertionError("Expected " + expected + " but was " + actual);
    }

    private static void rejects(Runnable action) {
        checks++;
        try {
            action.run();
        } catch (IllegalArgumentException e) {
            return;
        }
        throw new AssertionError("Expected rejection");
    }

    public static void main(String[] args) {
        equal(LearningLanguage.ENGLISH, LearningLanguage.fromCode("en"));
        equal(LearningLanguage.CHINESE, LearningLanguage.fromCode("zh"));
        equal("en-US", LearningLanguage.ENGLISH.getVoice());
        equal("zh-CN", LearningLanguage.CHINESE.getVoice());
        equal("Tiếng Anh", LearningLanguage.ENGLISH.getLabel());
        equal("zh", LearningLanguage.CHINESE.getCode());
        rejects(() -> LearningLanguage.fromCode("other"));
        rejects(() -> LearningLanguage.fromCode(null));
        equal(false, PasswordRules.valid(null));
        equal(false, PasswordRules.valid(""));
        equal(false, PasswordRules.valid("1234567"));
        equal(true, PasswordRules.valid("12345678"));
        equal(true, PasswordRules.valid("abcdefgh"));
        equal(true, PasswordRules.valid("a".repeat(72)));
        equal(false, PasswordRules.valid("a".repeat(73)));
        equal(true, PasswordRules.valid("中".repeat(24)));
        equal(false, PasswordRules.valid("中".repeat(25)));
        var questions = List.of(new QuizEngine.Question(10, "2x + 6 = 0", "single", 1, "x = -3", List.of(new QuizEngine.Choice(1, "-3", true), new QuizEngine.Choice(2, "3", false))), new QuizEngine.Question(20, "Số dương", "multiple", 2, "1 và 2", List.of(new QuizEngine.Choice(3, "1", true), new QuizEngine.Choice(4, "2", true), new QuizEngine.Choice(5, "0", false))));
        equal(new BigDecimal("10.00"), QuizEngine.grade(questions, Map.of("10", List.of(1L), "20", List.of(3L, 4L))).score());
        equal(new BigDecimal("0.00"), QuizEngine.grade(questions, Map.of()).score());
        equal(new BigDecimal("0.00"), QuizEngine.grade(questions, Map.of("20", List.of(3L))).score());
        equal(new BigDecimal("0.00"), QuizEngine.grade(questions, Map.of("20", List.of(3L, 4L, 5L))).score());
        equal(new BigDecimal("3.33"), QuizEngine.grade(questions, Map.of("10", List.of(1L))).score());
        equal(new BigDecimal("6.67"), QuizEngine.grade(questions, Map.of("20", List.of(4L, 3L))).score());
        equal(List.of(3L, 4L), QuizEngine.clean(questions, Map.of("20", List.of(4L, 3L, 3L))).get("20"));
        rejects(() -> QuizEngine.clean(questions, Map.of("10", List.of(999L))));
        rejects(() -> QuizEngine.clean(questions, Map.of("10", List.of(1L, 2L))));
        rejects(() -> QuizEngine.clean(questions, null));
        rejects(() -> QuizEngine.grade(List.of(), Map.of()));
        rejects(() -> QuizEngine.validate(List.of(new QuizEngine.Question(1, "Invalid", "single", 1, "", List.of(new QuizEngine.Choice(1, "A", true), new QuizEngine.Choice(2, "B", true))))));
        equal(List.of("id", "text", "selected"), Arrays.stream(QuizEngine.PublicChoice.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName).toList());
        equal(true, QuizEngine.publicView(questions, Map.of("10", List.of(1L))).get(0).choices().get(0).selected());
        equal(50, QuizEngine.reward(BigDecimal.TEN, BigDecimal.ZERO));
        equal(0, QuizEngine.reward(BigDecimal.TEN, BigDecimal.TEN));
        equal(25, QuizEngine.reward(BigDecimal.TEN, BigDecimal.valueOf(5)));
        equal(0, QuizEngine.reward(BigDecimal.valueOf(4), BigDecimal.valueOf(7)));
        LocalDate today = LocalDate.of(2026, 10, 18);
        equal(0, StudyRules.streak(List.of(), today));
        equal(1, StudyRules.streak(List.of(today), today));
        equal(3, StudyRules.streak(List.of(today.minusDays(1), today.minusDays(2), today.minusDays(3)), today));
        equal(1, StudyRules.streak(List.of(today, today.minusDays(2)), today));
        equal(0, StudyRules.streak(List.of(today.minusDays(2)), today));
        equal(2, StudyRules.streak(List.of(today, today, today.minusDays(1)), today));
        equal(2, StudyRules.streak(List.of(LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 1)), LocalDate.of(2026, 1, 1)));
        equal(new StudyRules.Review(0, 1), StudyRules.review(5, false));
        equal(new StudyRules.Review(1, 1), StudyRules.review(0, true));
        equal(List.of(1, 3, 7, 14, 30, 60), java.util.stream.IntStream.range(0, 6).mapToObj(i -> StudyRules.review(i, true).days()).toList());
        equal(new StudyRules.Review(6, 60), StudyRules.review(6, true));
        rejects(() -> StudyRules.review(-1, true));
        rejects(() -> StudyRules.review(7, true));
        equal("\"'=SUM(A1)\"", StudyRules.safeCsv("=SUM(A1)"));
        equal("\"'@a\"", StudyRules.safeCsv("@a"));
        equal("\"a\"\"b\"", StudyRules.safeCsv("a\"b"));
        equal("", StudyRules.safeCsv(null));
        equal("\"'  =1+1\"", StudyRules.safeCsv("  =1+1"));
        equal("\\u003c/script\\u003e\\u0026", StudyRules.safeJsonForHtml("</script>&"));
        String signed = Hmac.sign(Map.of("b", "two", "a", "one"), "key");
        equal(signed, Hmac.sign(Map.of("a", "one", "b", "two"), "key"));
        equal(true, Hmac.verify(Map.of("a", "one", "b", "two"), signed, "key"));
        equal(false, Hmac.verify(Map.of("a", "tampered", "b", "two"), signed, "key"));
        equal(false, Hmac.verify(Map.of(), null, "key"));
        equal(false, Hmac.verify(Map.of(), "anything", ""));
        equal("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Hmac.sha256("abc"));
        System.out.println("PASS: " + checks + " independent core assertions");
    }
}
