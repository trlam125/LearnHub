package vn.learnhub.community;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.BaseEntity;
import vn.learnhub.user.Account;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "feedback")
@Proxy(lazy = false)
@Getter
@Setter
public class Feedback extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    public Account user;
    @Column(name = "rating", nullable = false)
    public int rating = 5;
    @Column(name = "message", nullable = false, length = 2000)
    @Size(max = 2000)
    public String message = "";
    @Column(name = "resolved", nullable = false)
    public boolean resolved = false;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

}
