package vn.learnhub.course;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "course")
@Proxy(lazy = false)
@Getter
@Setter
public class Course extends BaseEntity {
    @Column(name = "language", nullable = false, length = 2)
    @NotBlank
    @Pattern(regexp = "zh|en")
    public String language = "zh";

    @Column(name = "title", nullable = false, length = 180)
    @Size(max = 180)
    @NotBlank
    public String title = "";
    @Column(name = "slug", nullable = false, length = 100)
    @Size(max = 100)
    @NotBlank
    public String slug = "";
    @Column(name = "summary", nullable = false, length = 300)
    @Size(max = 300)
    public String summary = "";
    @Column(name = "description", nullable = false, columnDefinition = "text")
    public String description = "";
    @Column(name = "subject", nullable = false, length = 60)
    @Size(max = 60)
    public String subject = "Tiếng Trung";
    @Column(name = "teacher_name", nullable = false, length = 120)
    @Size(max = 120)
    public String teacherName = "";
    @Column(name = "kind", nullable = false, length = 12)
    @Size(max = 12)
    public String kind = "course";
    @Column(name = "price", nullable = false)
    @Min(1)
    @Min(1000)
    public long price = 299000;
    @Column(name = "access_days", nullable = false)
    @Min(1)
    public int accessDays = 180;
    @Column(name = "published", nullable = false)
    public boolean published = false;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

}
