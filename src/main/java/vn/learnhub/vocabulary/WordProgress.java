package vn.learnhub.vocabulary;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.BaseEntity;
import vn.learnhub.user.Account;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "word_progress")
@Proxy(lazy = false)
@Getter
@Setter
public class WordProgress extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    public Account user;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "word_id", nullable = false)
    @NotNull
    public Word word;
    @Column(name = "saved", nullable = false)
    public boolean saved = false;
    @Column(name = "stage", nullable = false)
    public int stage = 0;
    @Column(name = "next_review", nullable = false)
    public LocalDate nextReview = LocalDate.now(vn.learnhub.shared.study.StudyRules.ZONE);
    @Column(name = "last_reviewed", nullable = true)
    public Instant lastReviewed;

}
