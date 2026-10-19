package vn.learnhub.quiz;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.UuidEntity;
import vn.learnhub.user.Account;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;
import java.math.BigDecimal;

@Entity
@Table(name = "quiz_attempt")
@Proxy(lazy = false)
@Getter
@Setter
public class Attempt extends UuidEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    public Account user;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    @NotNull
    public Quiz quiz;
    @Column(name = "started_at", nullable = false)
    public Instant startedAt = Instant.now();
    @Column(name = "deadline", nullable = false)
    public Instant deadline = Instant.now();
    @Column(name = "submitted_at", nullable = true)
    public Instant submittedAt;
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "text")
    public String snapshotJson = "[]";
    @Column(name = "answers_json", nullable = false, columnDefinition = "text")
    public String answersJson = "{}";
    @Column(name = "result_json", nullable = false, columnDefinition = "text")
    public String resultJson = "[]";
    @Column(name = "score", nullable = true, precision = 4, scale = 2)
    @DecimalMin("0")
    @DecimalMax("10")
    public BigDecimal score;
    @Column(name = "pass_score", nullable = false, precision = 4, scale = 2)
    @DecimalMin("0")
    @DecimalMax("10")
    public BigDecimal passScore = BigDecimal.valueOf(5);
    @Column(name = "open_key", nullable = true, length = 100)
    @Size(max = 100)
    public String openKey;

}
