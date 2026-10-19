package vn.learnhub.learning;

import vn.learnhub.auth.Access;
import vn.learnhub.course.Course;
import vn.learnhub.shared.audit.AuditService;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.user.Account;
import vn.learnhub.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class EnrollmentService {
    private final Store db;
    private final UserRepository users;
    private final Access access;
    private final AuditService audit;

    public EnrollmentService(Store db, UserRepository users, Access access, AuditService audit) {
        this.db = db;
        this.users = users;
        this.access = access;
        this.audit = audit;
    }

    @Transactional
    public void grant(Account actor, Long userId, Long courseId, int days, String reason) {
        access.requireAdmin(actor);
        if (days < 1 || days > 3650 || reason.isBlank())
            throw new IllegalArgumentException("Nhập thời hạn và lý do hợp lệ.");
        Account user = users.lockById(userId).orElseThrow();
        Course c = db.get(Course.class, courseId);
        Enrollment e = db.first(Enrollment.class, "select e from Enrollment e where e.user.id=:u and e.course.id=:c", "u", user.id, "c", c.id).orElseGet(() -> {
            Enrollment n = new Enrollment();
            n.user = user;
            n.course = c;
            return db.add(n);
        });
        e.active = true;
        e.startsAt = Instant.now();
        e.expiresAt = e.startsAt.plus(Duration.ofDays(days));
        e.sourceOrder = null;
        audit.record(actor, "access_granted", user.id + ":" + c.id, reason);
    }
}
