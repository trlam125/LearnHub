package vn.learnhub.notification;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "mail_job")
@Proxy(lazy = false)
@Getter
@Setter
public class MailJob extends BaseEntity {
    @Column(name = "event_key", nullable = false, length = 180)
    @Size(max = 180)
    public String eventKey = "";
    @Column(name = "recipient", nullable = false, length = 150)
    @Size(max = 150)
    public String recipient = "";
    @Column(name = "subject", nullable = false, length = 180)
    @Size(max = 180)
    public String subject = "";
    @Column(name = "body", nullable = false, columnDefinition = "text")
    public String body = "";
    @Column(name = "tries", nullable = false)
    public int tries = 0;
    @Column(name = "available_at", nullable = false)
    public Instant availableAt = Instant.now();
    @Column(name = "sent_at", nullable = true)
    public Instant sentAt;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

}
