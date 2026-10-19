package vn.learnhub.learning;

import vn.learnhub.auth.Access;
import vn.learnhub.course.Lesson;
import vn.learnhub.payment.PurchaseOrder;
import vn.learnhub.quiz.Attempt;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.user.Account;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.http.*;
import java.security.Principal;
import java.time.*;
import java.util.*;

@Controller
public class LearningDashboardController {
    private final Store db;
    private final Access access;

    public LearningDashboardController(Store db, Access access) {
        this.db = db;
        this.access = access;
    }

    public record EnrollmentRow(Enrollment enrollment, long completed, long total, int percent, boolean valid) {
    }

    @GetMapping({"/my-learning", "/my-learning/"})
    String dashboard(Principal principal, Model m) {
        Account user = access.required(principal);
        List<EnrollmentRow> rows = new ArrayList<>();
        for (var e : db.list(Enrollment.class, "select e from Enrollment e where e.user.id=:u order by e.startsAt desc", "u", user.id)) {
            long total = db.count("select count(l) from Lesson l where l.chapter.course.id=:c and l.published=true", "c", e.course.id);
            long done = db.count("select count(p) from Progress p where p.user.id=:u and p.lesson.chapter.course.id=:c and p.lesson.published=true", "u", user.id, "c", e.course.id);
            rows.add(new EnrollmentRow(e, done, total, total == 0 ? 0 : (int) (done * 100 / total), e.active && !e.startsAt.isAfter(Instant.now()) && e.expiresAt.isAfter(Instant.now())));
        }
        m.addAttribute("enrollments", rows);
        m.addAttribute("attempts", db.query(Attempt.class, "select a from Attempt a where a.user.id=:u order by a.startedAt desc", "u", user.id).setMaxResults(20).getResultList());
        m.addAttribute("orders", db.query(PurchaseOrder.class, "select o from PurchaseOrder o where o.user.id=:u order by o.createdAt desc", "u", user.id).setMaxResults(20).getResultList());
        return "learning/dashboard";
    }
}
