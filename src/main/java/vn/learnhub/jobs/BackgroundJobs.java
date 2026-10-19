package vn.learnhub.jobs;

import vn.learnhub.config.AppProperties;
import vn.learnhub.notification.OutboxService;
import vn.learnhub.payment.OrderStatus;
import vn.learnhub.payment.PayosService;
import vn.learnhub.payment.PurchaseOrder;
import vn.learnhub.quiz.Attempt;
import vn.learnhub.quiz.QuizService;
import vn.learnhub.shared.persistence.Store;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.*;

import java.time.*;

@Component
@ConditionalOnProperty(name = "app.jobs-enabled", havingValue = "true", matchIfMissing = true)
public class BackgroundJobs {
    private static final Logger log = LoggerFactory.getLogger(BackgroundJobs.class);
    private final Store db;
    private final QuizService quizzes;
    private final OutboxService outbox;
    private final PayosService payos;
    private final AppProperties app;

    public BackgroundJobs(Store db, QuizService quizzes, OutboxService outbox, PayosService payos, AppProperties app) {
        this.db = db;
        this.quizzes = quizzes;
        this.outbox = outbox;
        this.payos = payos;
        this.app = app;
    }

    @Scheduled(fixedDelay = 10000, initialDelay = 10000)
    public void tick() {
        for (var id : db.query(java.util.UUID.class, "select a.id from Attempt a where a.submittedAt is null and a.deadline<=:now", "now", Instant.now()).setMaxResults(100).getResultList())
            try {
                quizzes.finishExpired(id);
            } catch (Exception e) {
                log.warn("Unable to finalize attempt {}", id);
            }
        for (int i = 0; i < 20; i++) {
            try {
                if (!outbox.deliverOne()) break;
            } catch (Exception e) {
                log.warn("Outbox tick failed");
                break;
            }
        }
    }

    @Scheduled(fixedDelay = 300000, initialDelay = 30000)
    public void reconcile() {
        if (!app.paymentMode().equals("payos") && !app.hasPayosCredentials()) return;
        for (PurchaseOrder order : db.query(PurchaseOrder.class, "select o from PurchaseOrder o join fetch o.user join fetch o.course where o.provider='payos' and o.status in :statuses and o.nextReconcileAt<=:now order by o.nextReconcileAt,o.createdAt,o.id", "statuses", java.util.List.of(OrderStatus.PENDING, OrderStatus.UNDERPAID), "now", Instant.now()).setMaxResults(100).getResultList())
            try {
                payos.reconcile(order);
            } catch (Exception e) {
                log.warn("Unable to reconcile order {}", order.id);
            } finally {
                try {
                    payos.deferReconciliation(order.id);
                } catch (Exception e) {
                    log.warn("Unable to reschedule reconciliation for order {}", order.id);
                }
            }
    }
}
