package vn.learnhub.auth;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.BaseEntity;
import vn.learnhub.user.Account;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "auth_token")
@Proxy(lazy = false)
@Getter
@Setter
public class AuthToken extends BaseEntity {
    @Column(name = "token_hash", nullable = false, length = 64)
    @Size(max = 64)
    public String tokenHash = "";
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    public Account user;
    @Column(name = "kind", nullable = false, length = 10)
    @Size(max = 10)
    public String kind = "";
    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt = Instant.now();
    @Column(name = "used_at", nullable = true)
    public Instant usedAt;

}
