package vn.learnhub.vocabulary;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.course.Lesson;
import vn.learnhub.shared.persistence.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "word")
@Proxy(lazy = false)
@Getter
@Setter
public class Word extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    @NotNull
    public Lesson lesson;
    @Column(name = "hanzi", nullable = false, length = 80)
    @Size(max = 80)
    @NotBlank
    public String hanzi = "";
    @Column(name = "pinyin", nullable = false, length = 160)
    @Size(max = 160)
    @NotBlank
    public String pinyin = "";
    @Column(name = "meaning", nullable = false, length = 300)
    @Size(max = 300)
    @NotBlank
    public String meaning = "";
    @Column(name = "sino_vietnamese", nullable = false, length = 120)
    @Size(max = 120)
    public String sinoVietnamese = "";
    @Column(name = "example_zh", nullable = false, length = 500)
    @Size(max = 500)
    public String exampleZh = "";
    @Column(name = "example_vi", nullable = false, length = 500)
    @Size(max = 500)
    public String exampleVi = "";
    @Column(name = "audio_url", nullable = false, length = 500)
    @Size(max = 500)
    public String audioUrl = "";
    @Column(name = "position", nullable = false)
    public int position = 1;

    public String speechLanguage() {
        return vn.learnhub.course.LearningLanguage.fromCode(lesson.chapter.course.language).getVoice();
    }

}
