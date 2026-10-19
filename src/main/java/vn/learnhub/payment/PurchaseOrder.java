package vn.learnhub.payment;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Proxy;
import org.hibernate.type.SqlTypes;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.course.Course;
import vn.learnhub.shared.persistence.UuidEntity;
import vn.learnhub.user.Account;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "purchase_order")
@Proxy(lazy = false)
@Getter
@Setter
public class PurchaseOrder extends UuidEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    public Account user;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    @NotNull
    public Course course;
    @Column(name = "amount", nullable = false)
    public long amount = 0;
    @Column(name = "access_days", nullable = false)
    @Min(1)
    public int accessDays = 180;
    @Column(name = "code", nullable = false)
    public long code = 0;
    @Column(name = "provider", nullable = false, length = 20)
    @Size(max = 20)
    public String provider = "manual";
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    public OrderStatus status = OrderStatus.PENDING;
    @Column(name = "link_id", nullable = false, length = 100)
    @Size(max = 100)
    public String linkId = "";
    @Column(name = "checkout_url", nullable = false, length = 500)
    @Size(max = 500)
    public String checkoutUrl = "";
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
    @Column(name = "paid_at", nullable = true)
    public Instant paidAt;
    @Column(name = "pending_key", nullable = true, length = 100)
    @Size(max = 100)
    public String pendingKey;

    // Restore only this order's contribution when recording a refund.
    @Column(name = "access_snapshot_captured", nullable = false)
    public boolean accessSnapshotCaptured = false;
    @Column(name = "prior_access_active", nullable = false)
    public boolean priorAccessActive = false;
    @Column(name = "prior_access_starts_at")
    public Instant priorAccessStartsAt;
    @Column(name = "prior_access_expires_at")
    public Instant priorAccessExpiresAt;
    @Column(name = "prior_access_source_order")
    public java.util.UUID priorAccessSourceOrder;
    @Column(name = "next_reconcile_at", nullable = false)
    public Instant nextReconcileAt = Instant.now();

}
