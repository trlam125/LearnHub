package vn.learnhub.course;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "study_class")
@Proxy(lazy = false)
@Getter
@Setter
public class StudyClass extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    @NotNull
    public Course course;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    @NotNull
    public Teacher teacher;
    @Column(name = "title", nullable = false, length = 160)
    @Size(max = 160)
    @NotBlank
    public String title = "";
    @Column(name = "schedule", nullable = false, length = 250)
    @Size(max = 250)
    public String schedule = "";
    @Column(name = "starts_on", nullable = true)
    public LocalDate startsOn;
    @Column(name = "sessions", nullable = false)
    @Min(1)
    public int sessions = 20;
    @Column(name = "capacity", nullable = false)
    @Min(1)
    public int capacity = 8;
    @Column(name = "features", nullable = false, columnDefinition = "text")
    public String features = "";
    @Column(name = "accepting", nullable = false)
    public boolean accepting = true;
    @Column(name = "published", nullable = false)
    public boolean published = false;

}
