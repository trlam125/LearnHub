package vn.learnhub.course;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "learning_level")
@Proxy(lazy = false)
@Getter
@Setter
public class Level extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "path_id", nullable = false)
    @NotNull
    public LearningPath path;
    @Column(name = "number", nullable = false)
    public int number = 1;
    @Column(name = "title", nullable = false, length = 120)
    @Size(max = 120)
    @NotBlank
    public String title = "";
    @Column(name = "description", nullable = false, length = 300)
    @Size(max = 300)
    public String description = "";
    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "course_id", nullable = true)
    public Course course;
    @Column(name = "published", nullable = false)
    public boolean published = false;

}
