package vn.learnhub.quiz;

import vn.learnhub.auth.Access;
import vn.learnhub.community.ActivityService;
import vn.learnhub.config.AppProperties;
import vn.learnhub.notification.MailQueueService;
import vn.learnhub.shared.json.JsonTools;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.user.Account;
import vn.learnhub.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class QuizService {
    private final Store db;
    private final UserRepository users;
    private final Access access;
    private final JsonTools json;
    private final ActivityService activities;
    private final MailQueueService mail;
    private final AppProperties app;

    public QuizService(Store db, UserRepository users, Access access, JsonTools json, ActivityService activities, MailQueueService mail, AppProperties app) {
        this.db = db;
        this.users = users;
        this.access = access;
        this.json = json;
        this.activities = activities;
        this.mail = mail;
        this.app = app;
    }

    // Commit an expired attempt even when starting another one is denied.
    @Transactional(noRollbackFor = AttemptLimitReachedException.class)
    public Attempt start(Account user, Long quizId) {
        users.lockById(user.id).orElseThrow();
        // Lock the quiz row before loading its eager lesson association.
        // This avoids an inconsistent entity read when another transaction moves the quiz.
        if (db.entityManager().createNativeQuery("select id from quiz where id=:id for update")
                .setParameter("id", quizId).getResultList().isEmpty())
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        Quiz quiz = db.get(Quiz.class, quizId);
        db.entityManager().refresh(quiz);
        access.requireLesson(user, quiz.lesson);
        if (!quiz.published) throw new AccessDeniedException("Bài kiểm tra chưa xuất bản.");
        String key = user.id + ":" + quiz.id;
        var open = db.first(Attempt.class, "select a from Attempt a where a.openKey=:key", "key", key);
        if (open.isPresent()) {
            if (open.get().deadline.isAfter(Instant.now())) return open.get();
            finish(open.get().id);
        }
        if (db.count("select count(a) from Attempt a where a.user.id=:u and a.quiz.id=:q", "u", user.id, "q", quiz.id) >= quiz.maxAttempts)
            throw new AttemptLimitReachedException();
        List<QuizEngine.Question> snapshot = new ArrayList<>();
        for (Question q : db.list(Question.class, "select q from Question q where q.quiz.id=:q order by q.position,q.id", "q", quiz.id)) {
            var choices = db.list(Choice.class, "select c from Choice c where c.question.id=:q order by c.id", "q", q.id).stream().map(c -> new QuizEngine.Choice(c.id, c.text, c.correct)).toList();
            snapshot.add(new QuizEngine.Question(q.id, q.text, q.kind, q.points, q.explanation, choices));
        }
        QuizEngine.validate(snapshot);
        Attempt a = new Attempt();
        a.user = user;
        a.quiz = quiz;
        a.snapshotJson = json.write(snapshot);
        a.passScore = quiz.passScore;
        a.openKey = key;
        a.deadline = Instant.now().plus(Duration.ofMinutes(quiz.minutes));
        return db.add(a);
    }

    @Transactional
    public Attempt save(Account user, UUID id, Map<String, List<Long>> answers, boolean submit) {
        Attempt a = db.lock(Attempt.class, id);
        if (!a.user.id.equals(user.id)) throw new AccessDeniedException("Không được xem bài làm khác.");
        if (a.submittedAt != null) return a;
        access.requireLesson(user, a.quiz.lesson);
        if (!a.deadline.isAfter(Instant.now())) return finish(id);
        a.answersJson = json.write(QuizEngine.clean(json.questions(a.snapshotJson), answers));
        return submit ? finish(id) : a;
    }

    @Transactional
    public Attempt finish(UUID id) {
        Attempt a = db.lock(Attempt.class, id);
        if (a.submittedAt != null) return a;
        var result = QuizEngine.grade(json.questions(a.snapshotJson), json.answers(a.answersJson));
        a.score = result.score();
        a.resultJson = json.write(result.details());
        a.submittedAt = Instant.now();
        a.openKey = null;
        BigDecimal previous = db.query(BigDecimal.class, "select max(x.score) from Attempt x where x.user.id=:u and x.quiz.id=:q and x.id<>:id and x.submittedAt is not null", "u", a.user.id, "q", a.quiz.id, "id", a.id).getSingleResult();
        int gain = QuizEngine.reward(a.score, previous == null ? BigDecimal.ZERO : previous);
        activities.award(a.user, "attempt:" + a.id, "quiz", gain);
        mail.enqueue("result:" + a.id, a.user, "Kết quả bài tập", a.quiz.title + ": " + a.score + "/10. Xem: " + app.baseUrl() + "/attempts/" + a.id);
        return a;
    }

    @Transactional
    public Attempt finishExpired(UUID id) {
        Attempt a = db.lock(Attempt.class, id);
        if (a.submittedAt == null && !a.deadline.isAfter(Instant.now())) return finish(id);
        return a;
    }

    public Attempt owned(Account user, UUID id) {
        Attempt a = db.get(Attempt.class, id);
        if (!a.user.id.equals(user.id)) throw new AccessDeniedException("Không được xem bài làm khác.");
        return a;
    }

    public static class AttemptLimitReachedException extends IllegalArgumentException {
        public AttemptLimitReachedException() {
            super("Bạn đã dùng hết lượt làm bài.");
        }
    }
}
