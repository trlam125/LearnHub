package vn.learnhub.payment;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "payment")
@Proxy(lazy = false)
@Getter
@Setter
public class Payment extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @NotNull
    public PurchaseOrder order;
    @Column(name = "reference", nullable = false, length = 150)
    @Size(max = 150)
    public String reference = "";
    @Column(name = "amount", nullable = false)
    public long amount = 0;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

}
