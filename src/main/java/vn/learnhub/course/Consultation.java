package vn.learnhub.course;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "consultation")
@Proxy(lazy = false)
@Getter
@Setter
public class Consultation extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "offering_id", nullable = false)
    @NotNull
    public StudyClass offering;
    @Column(name = "name", nullable = false, length = 120)
    @Size(max = 120)
    @NotBlank
    public String name = "";
    @Column(name = "email", nullable = false, length = 150)
    @Size(max = 150)
    public String email = "";
    @Column(name = "phone", nullable = false, length = 30)
    @Size(max = 30)
    public String phone = "";
    @Column(name = "message", nullable = false, length = 2000)
    @Size(max = 2000)
    public String message = "";
    @Column(name = "status", nullable = false, length = 20)
    @Size(max = 20)
    public String status = "new";
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

}
