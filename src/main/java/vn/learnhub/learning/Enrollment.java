package vn.learnhub.learning;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.course.Course;
import vn.learnhub.shared.persistence.BaseEntity;
import vn.learnhub.user.Account;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "enrollment")
@Proxy(lazy = false)
@Getter
@Setter
public class Enrollment extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    public Account user;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    @NotNull
    public Course course;
    @Column(name = "starts_at", nullable = false)
    public Instant startsAt = Instant.now();
    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt = Instant.now();
    @Column(name = "active", nullable = false)
    public boolean active = true;
    @Column(name = "source_order", nullable = true)
    public UUID sourceOrder;

}
