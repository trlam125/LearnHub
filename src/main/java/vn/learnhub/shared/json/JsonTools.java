package vn.learnhub.shared.json;

import vn.learnhub.quiz.QuizEngine;
import vn.learnhub.shared.study.StudyRules;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class JsonTools {
    private final ObjectMapper mapper;

    public JsonTools(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Không mã hóa được dữ liệu.", e);
        }
    }

    public <T> T read(String json, TypeReference<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Dữ liệu lưu trữ không hợp lệ.", e);
        }
    }

    public List<QuizEngine.Question> questions(String json) {
        return read(json, new TypeReference<>() {
        });
    }

    public Map<String, List<Long>> answers(String json) {
        return read(json, new TypeReference<>() {
        });
    }

    public String html(Object value) {
        return StudyRules.safeJsonForHtml(write(value));
    }
}
