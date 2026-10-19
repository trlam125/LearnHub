package vn.learnhub.course;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "chapter")
@Proxy(lazy = false)
@Getter
@Setter
public class Chapter extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    @NotNull
    public Course course;
    @Column(name = "title", nullable = false, length = 180)
    @Size(max = 180)
    @NotBlank
    public String title = "";
    @Column(name = "position", nullable = false)
    public int position = 1;

}
