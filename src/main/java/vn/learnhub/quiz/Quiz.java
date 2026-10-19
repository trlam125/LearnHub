package vn.learnhub.quiz;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.course.Lesson;
import vn.learnhub.shared.persistence.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;
import java.math.BigDecimal;

@Entity
@Table(name = "quiz")
@Proxy(lazy = false)
@Getter
@Setter
public class Quiz extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    @NotNull
    public Lesson lesson;
    @Column(name = "title", nullable = false, length = 180)
    @Size(max = 180)
    @NotBlank
    public String title = "";
    @Column(name = "minutes", nullable = false)
    @Min(1)
    @Max(180)
    public int minutes = 15;
    @Column(name = "max_attempts", nullable = false)
    @Min(1)
    public int maxAttempts = 3;
    @Column(name = "pass_score", nullable = false, precision = 4, scale = 2)
    @DecimalMin("0")
    @DecimalMax("10")
    public BigDecimal passScore = BigDecimal.valueOf(5);
    @Column(name = "published", nullable = false)
    public boolean published = false;

}
