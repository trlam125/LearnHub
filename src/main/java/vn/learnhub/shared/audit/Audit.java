package vn.learnhub.shared.audit;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.BaseEntity;
import vn.learnhub.user.Account;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "audit_log")
@Proxy(lazy = false)
@Getter
@Setter
public class Audit extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "actor_id", nullable = true)
    public Account actor;
    @Column(name = "action", nullable = false, length = 80)
    @Size(max = 80)
    public String action = "";
    @Column(name = "target", nullable = false, length = 180)
    @Size(max = 180)
    public String target = "";
    @Column(name = "detail", nullable = false, columnDefinition = "text")
    public String detail = "";
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

}
