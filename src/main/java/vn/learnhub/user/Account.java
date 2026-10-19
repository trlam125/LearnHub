package vn.learnhub.user;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Proxy;
import org.hibernate.type.SqlTypes;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "app_user")
@Proxy(lazy = false)
@Getter
@Setter
public class Account extends BaseEntity {
    @Column(name = "name", nullable = false, length = 150)
    @Size(max = 150)
    @NotBlank
    public String name = "";
    @Column(name = "email", nullable = false, length = 150)
    @Size(max = 150)
    @Email
    @NotBlank
    public String email = "";
    @Column(name = "password", nullable = false, length = 255)
    @Size(max = 255)
    public String password = "";
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role", nullable = false, length = 20)
    public Role role = Role.STUDENT;
    @Column(name = "verified", nullable = false)
    public boolean verified = false;
    @Column(name = "session_version", nullable = false)
    public long sessionVersion = 0;
    @Column(name = "active", nullable = false)
    public boolean active = true;
    @Column(name = "ranking_visible", nullable = false)
    public boolean rankingVisible = false;
    @Column(name = "public_name", nullable = false, length = 40)
    @Size(max = 40)
    public String publicName = "";
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

}
