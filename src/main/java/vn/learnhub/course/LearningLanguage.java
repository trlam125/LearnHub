package vn.learnhub.course;

import lombok.Getter;

public enum LearningLanguage {
    CHINESE("zh", "Tiếng Trung", "zh-CN", "汉"),
    ENGLISH("en", "Tiếng Anh", "en-US", "EN");

    @Getter
    private final String code;
    @Getter
    private final String label;
    @Getter
    private final String voice;
    @Getter
    private final String symbol;

    LearningLanguage(String code, String label, String voice, String symbol) {
        this.code = code;
        this.label = label;
        this.voice = voice;
        this.symbol = symbol;
    }

    public static LearningLanguage fromCode(String code) {
        for (LearningLanguage language : values())
            if (language.code.equals(code)) return language;
        throw new IllegalArgumentException("Chọn tiếng Anh hoặc tiếng Trung.");
    }
}
