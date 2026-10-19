package vn.learnhub.learning;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.course.Lesson;
import vn.learnhub.shared.persistence.BaseEntity;
import vn.learnhub.user.Account;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "lesson_progress")
@Proxy(lazy = false)
@Getter
@Setter
public class Progress extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    public Account user;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    @NotNull
    public Lesson lesson;
    @Column(name = "completed_at", nullable = false)
    public Instant completedAt = Instant.now();

}
