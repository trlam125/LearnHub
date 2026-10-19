package vn.learnhub.payment;

import vn.learnhub.auth.Access;
import vn.learnhub.config.AppProperties;
import vn.learnhub.course.Course;
import vn.learnhub.learning.Enrollment;
import vn.learnhub.notification.MailQueueService;
import vn.learnhub.shared.audit.AuditService;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.user.Account;
import vn.learnhub.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.security.SecureRandom;
import java.util.*;

@Service
public class PaymentService {
    private final Store db;
    private final UserRepository users;
    private final Access access;
    private final AppProperties app;
    private final AuditService audit;
    private final MailQueueService mail;

    public PaymentService(Store db, UserRepository users, Access access, AppProperties app, AuditService audit, MailQueueService mail) {
        this.db = db;
        this.users = users;
        this.access = access;
        this.app = app;
        this.audit = audit;
        this.mail = mail;
    }

    @Transactional
    public PurchaseOrder order(Account user, Long courseId) {
        users.lockById(user.id).orElseThrow();
        Course course = db.get(Course.class, courseId);
        if (!course.published || course.kind.equals("game"))
            throw new IllegalArgumentException("Khóa học không nhận thanh toán.");
        if (access.course(user, course)) throw new IllegalArgumentException("Bạn đã có quyền học khóa này.");
        String key = user.id + ":" + course.id;
        var existing = db.first(PurchaseOrder.class, "select o from PurchaseOrder o where o.pendingKey=:key", "key", key);
        if (existing.isPresent()) return existing.get();
        PurchaseOrder o = new PurchaseOrder();
        o.user = user;
        o.course = course;
        o.amount = course.price;
        o.accessDays = course.accessDays;
        o.pendingKey = key;
        o.provider = app.paymentMode();
        o.code = new SecureRandom().nextLong(1_000_000_000_000L, 9_000_000_000_000L);
        return db.add(o);
    }

    @Transactional
    public PurchaseOrder confirm(UUID id, long amount, String reference, Account actor) {
        // A caller may already have loaded this order in the same transaction.
        // Flush new orders before refresh; the row lock alone does not refresh cached state.
        db.entityManager().flush();
        PurchaseOrder order = db.lock(PurchaseOrder.class, id);
        db.entityManager().refresh(order, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (order.status == OrderStatus.PAID || order.status == OrderStatus.REFUNDED) {
            Payment p = db.first(Payment.class, "select p from Payment p where p.order.id=:id", "id", id).orElseThrow();
            if (amount != order.amount || p.amount != amount || !Objects.equals(p.reference, reference))
                throw new IllegalArgumentException("Giao dịch không khớp.");
            return order;
        }
        if (!order.status.isAwaitingPayment() || amount != order.amount || reference == null || reference.isBlank() || reference.length() > 150)
            throw new IllegalArgumentException("Trạng thái hoặc giao dịch không hợp lệ.");
        if (db.count("select count(p) from Payment p where p.reference=:ref", "ref", reference) > 0)
            throw new IllegalArgumentException("Mã giao dịch đã được dùng.");
        users.lockById(order.user.id).orElseThrow();
        Payment p = new Payment();
        p.order = order;
        p.reference = reference;
        p.amount = amount;
        db.add(p);
        Enrollment e = db.first(Enrollment.class, "select e from Enrollment e where e.user.id=:u and e.course.id=:c", "u", order.user.id, "c", order.course.id).orElseGet(() -> {
            Enrollment n = new Enrollment();
            n.user = order.user;
            n.course = order.course;
            n.active = false;
            return db.add(n);
        });
        Instant now = Instant.now();
        order.priorAccessActive = e.active;
        order.priorAccessStartsAt = e.startsAt;
        order.priorAccessExpiresAt = e.expiresAt;
        order.priorAccessSourceOrder = e.sourceOrder;
        order.accessSnapshotCaptured = true;
        Instant base = e.active && e.expiresAt.isAfter(now) ? e.expiresAt : now;
        e.startsAt = now;
        e.expiresAt = base.plus(Duration.ofDays(order.accessDays));
        e.active = true;
        e.sourceOrder = order.id;
        order.status = OrderStatus.PAID;
        order.paidAt = now;
        order.pendingKey = null;
        audit.record(actor, "payment_confirmed", id.toString(), amount + " VND; ref=" + reference);
        mail.enqueue("payment:" + id, order.user, "Khóa học đã được mở", "Bạn đã mua " + order.course.title + ". Vào học: " + app.baseUrl() + "/my-learning");
        return order;
    }

    @Transactional
    public void manualConfirm(Account actor, UUID id, String reference) {
        access.requireAdmin(actor);
        PurchaseOrder order = db.lock(PurchaseOrder.class, id);
        if (!order.provider.equals("manual")) throw new IllegalArgumentException("Hãy dùng nút kiểm tra thanh toán cho đơn trực tuyến.");
        confirm(id, order.amount, "manual:" + reference.trim(), actor);
    }

    @Transactional
    public void cancel(Account user, UUID id) {
        PurchaseOrder o = db.lock(PurchaseOrder.class, id);
        if (!o.user.id.equals(user.id) || !o.provider.equals("manual") || o.status != OrderStatus.PENDING)
            throw new IllegalArgumentException("Không hủy được đơn này.");
        o.status = OrderStatus.CANCELLED;
        o.pendingKey = null;
        audit.record(user, "order_cancelled", id.toString(), "Huỷ đơn chuyển khoản chưa xác nhận; nếu đã chuyển tiền hãy liên hệ hỗ trợ.");
    }

    @Transactional
    public void refundRecorded(Account actor, UUID id, String reference, String reason) {
        access.requireAdmin(actor);
        PurchaseOrder o = db.lock(PurchaseOrder.class, id);
        if (o.status != OrderStatus.PAID || reference.isBlank() || reason.isBlank())
            throw new IllegalArgumentException("Cần đơn đã thanh toán, mã hoàn và lý do.");
        users.lockById(o.user.id).orElseThrow();
        Enrollment e = db.first(Enrollment.class, "select e from Enrollment e where e.user.id=:u and e.course.id=:c", "u", o.user.id, "c", o.course.id).orElseThrow();
        if (!o.id.equals(e.sourceOrder))
            throw new IllegalArgumentException("Quyền học có nguồn khác; cần rà soát riêng trước khi thu hồi.");
        if (!o.accessSnapshotCaptured || o.priorAccessStartsAt == null || o.priorAccessExpiresAt == null)
            throw new IllegalArgumentException("Đơn cũ chưa lưu quyền học trước thanh toán. Cần đối chiếu lịch sử và điều chỉnh quyền học thủ công.");
        e.active = o.priorAccessActive;
        e.startsAt = o.priorAccessStartsAt;
        e.expiresAt = o.priorAccessExpiresAt;
        e.sourceOrder = o.priorAccessSourceOrder;
        o.status = OrderStatus.REFUNDED;
        audit.record(actor, "external_refund_recorded", id.toString(), "ref=" + reference + "; " + reason);
    }
}
