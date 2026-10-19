package vn.learnhub.course;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "learning_path")
@Proxy(lazy = false)
@Getter
@Setter
public class LearningPath extends BaseEntity {
    @Column(name = "language", nullable = false, length = 2)
    @NotBlank
    @Pattern(regexp = "zh|en")
    public String language = "zh";

    @Column(name = "slug", nullable = false, length = 100)
    @Size(max = 100)
    @NotBlank
    public String slug = "";
    @Column(name = "title", nullable = false, length = 160)
    @Size(max = 160)
    @NotBlank
    public String title = "";
    @Column(name = "symbol", nullable = false, length = 4)
    @Size(max = 4)
    public String symbol = "学";
    @Column(name = "category", nullable = false, length = 20)
    @Size(max = 20)
    public String category = "hsk";
    @Column(name = "description", nullable = false, columnDefinition = "text")
    public String description = "";
    @Column(name = "published", nullable = false)
    public boolean published = false;
    @Column(name = "position", nullable = false)
    public int position = 1;

}
