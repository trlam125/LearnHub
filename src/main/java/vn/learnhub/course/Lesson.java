package vn.learnhub.course;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "lesson")
@Proxy(lazy = false)
@Getter
@Setter
public class Lesson extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    @NotNull
    public Chapter chapter;
    @Column(name = "title", nullable = false, length = 180)
    @Size(max = 180)
    @NotBlank
    public String title = "";
    @Column(name = "content", nullable = false, columnDefinition = "text")
    public String content = "";
    @Column(name = "position", nullable = false)
    public int position = 1;
    @Column(name = "preview", nullable = false)
    public boolean preview = false;
    @Column(name = "published", nullable = false)
    public boolean published = false;

}
