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
@Table(name = "study_activity")
@Proxy(lazy = false)
@Getter
@Setter
public class Activity extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    public Account user;
    @Column(name = "event_key", nullable = false, length = 150)
    @Size(max = 150)
    public String eventKey = "";
    @Column(name = "day", nullable = false)
    public LocalDate day = LocalDate.now(vn.learnhub.shared.study.StudyRules.ZONE);
    @Column(name = "xp", nullable = false)
    public int xp = 0;
    @Column(name = "kind", nullable = false, length = 20)
    @Size(max = 20)
    public String kind = "";
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

}
