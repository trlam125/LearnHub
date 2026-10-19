package vn.learnhub;

import vn.learnhub.admin.AdminService;
import vn.learnhub.auth.Access;
import vn.learnhub.auth.AuthService;
import vn.learnhub.auth.AuthToken;
import vn.learnhub.config.AppProperties;
import vn.learnhub.course.*;
import vn.learnhub.jobs.BackgroundJobs;
import vn.learnhub.notification.OutboxService;
import vn.learnhub.payment.*;
import vn.learnhub.quiz.*;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.shared.security.Hmac;
import vn.learnhub.user.Account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties = {"spring.profiles.active=test", "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:learnhublogic;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=DAY;DB_CLOSE_DELAY=-1}", "spring.datasource.username=${TEST_DB_USER:sa}", "spring.datasource.password=${TEST_DB_PASSWORD:}", "spring.flyway.locations=${TEST_FLYWAY_LOCATIONS:classpath:db/migration}", "app.jobs-enabled=false", "app.seed-demo=false", "app.bootstrap-email=admin@example.test", "app.bootstrap-password=Admin-test-password-2026!", "app.mail-mode=console", "app.payment-mode=manual", "app.payos-client-id=test-client", "app.payos-api-key=test-api", "app.payos-checksum-key=test-checksum"})
@AutoConfigureMockMvc
class LogicTransactionTest {
    @Autowired Store db;
    @Autowired MockMvc mvc;
    @Autowired PlatformTransactionManager manager;
    @Autowired PasswordEncoder passwords;
    @Autowired AdminService admin;
    @Autowired AuthService auth;
    @Autowired QuizService quizzes;
    @Autowired PayosService payos;
    @MockitoSpyBean Access access;

    record Fixture(Account student, Account owner, Course course, Quiz quiz, String resetToken) { }

    private Fixture fixture() {
        return new TransactionTemplate(manager).execute(status -> {
            String unique = UUID.randomUUID().toString();
            Account student = new Account(); student.name = "Before profile update";
            student.email = unique + "@example.test"; student.password = passwords.encode("Original-password-2026!");
            student.verified = true; db.add(student);
            Account owner = db.first(Account.class, "select u from Account u where u.email='admin@example.test'").orElseThrow();
            Course course = new Course(); course.slug = "transaction-" + unique; course.title = "Transaction course";
            course.published = true; db.add(course);
            Chapter chapter = new Chapter(); chapter.course = course; chapter.title = "Chapter"; db.add(chapter);
            Lesson lesson = new Lesson(); lesson.chapter = chapter; lesson.title = "Lesson";
            lesson.published = true; lesson.preview = true; db.add(lesson);
            Quiz quiz = new Quiz(); quiz.lesson = lesson; quiz.title = "Transaction quiz";
            quiz.published = true; quiz.maxAttempts = 1; db.add(quiz);
            Question question = new Question(); question.quiz = quiz; question.text = "2+2?"; db.add(question);
            Choice right = new Choice(); right.question = question; right.text = "4"; right.correct = true; db.add(right);
            Choice wrong = new Choice(); wrong.question = question; wrong.text = "5"; db.add(wrong);
            String raw = UUID.randomUUID().toString();
            AuthToken token = new AuthToken(); token.user = student; token.kind = "reset";
            token.tokenHash = Hmac.sha256(raw); token.expiresAt = Instant.now().plusSeconds(3600); db.add(token);
            return new Fixture(student, owner, course, quiz, raw);
        });
    }

    @Test
    void deniedRetryCommitsExpiredLastAttemptWithoutWaitingForScheduler() {
        Fixture f = fixture();
        UUID id = quizzes.start(f.student(), f.quiz().id).id;
        new TransactionTemplate(manager).executeWithoutResult(status -> db.get(Attempt.class, id).deadline = Instant.now().minusSeconds(1));
        assertThrows(QuizService.AttemptLimitReachedException.class, () -> quizzes.start(f.student(), f.quiz().id));
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            Attempt persisted = db.get(Attempt.class, id);
            assertNotNull(persisted.submittedAt);
            assertNull(persisted.openKey);
            assertEquals(0, persisted.score.signum());
        });
    }

    private Fixture blockedProfile(Consumer<Fixture> concurrentChange) throws Exception {
        Fixture f = fixture();
        CountDownLatch read = new CountDownLatch(1), release = new CountDownLatch(1);
        doAnswer(invocation -> {
            Account user = (Account) invocation.callRealMethod();
            if (Thread.currentThread().getName().equals("profile-request") && user.id.equals(f.student().id)) {
                read.countDown();
                if (!release.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for concurrent update");
            }
            return user;
        }).when(access).required(any());
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "profile-request"));
        try {
            Future<Integer> response = executor.submit(() -> mvc.perform(post("/tai-khoan")
                    .with(user(f.student().email)).with(csrf()).param("name", "Changed name")
                    .param("publicName", "Changed alias")).andReturn().getResponse().getStatus());
            assertTrue(read.await(15, TimeUnit.SECONDS));
            concurrentChange.accept(f);
            release.countDown();
            assertEquals(403, response.get(15, TimeUnit.SECONDS));
        } finally {
            release.countDown(); executor.shutdownNow();
        }
        return f;
    }

    @Test
    void concurrentProfileCannotReactivateAccountAfterAdminDisablesIt() throws Exception {
        Fixture f = blockedProfile(x -> admin.updateUser(x.owner(), x.student().id, "STUDENT", false, "Disable test"));
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            Account persisted = db.get(Account.class, f.student().id);
            assertFalse(persisted.active);
            assertEquals(1, persisted.sessionVersion);
            assertEquals("Before profile update", persisted.name);
        });
    }

    @Test
    void concurrentProfileCannotRestorePasswordBeforeReset() throws Exception {
        Fixture f = blockedProfile(x -> auth.reset(x.resetToken(), "Replacement-password-2026!", "Replacement-password-2026!"));
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            Account persisted = db.get(Account.class, f.student().id);
            assertTrue(passwords.matches("Replacement-password-2026!", persisted.password));
            assertFalse(passwords.matches("Original-password-2026!", persisted.password));
            assertEquals(1, persisted.sessionVersion);
        });
    }

    @Test
    void reconciliationRotatesBeyondHundredFailingOrdersIncludingOlderOrders() {
        Fixture f = fixture();
        Set<UUID> expected = new TransactionTemplate(manager).execute(status -> {
            Set<UUID> ids = new HashSet<>();
            for (int i = 0; i < 105; i++) {
                PurchaseOrder order = new PurchaseOrder(); order.user = db.get(Account.class, f.student().id);
                order.course = db.get(Course.class, f.course().id); order.provider = "payos";
                order.code = ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 9_000_000_000_000L);
                order.createdAt = Instant.now().minus(Duration.ofDays(3)); order.nextReconcileAt = order.createdAt;
                db.add(order); ids.add(order.id);
            }
            return ids;
        });
        PayosService gateway = mock(PayosService.class);
        Set<UUID> seen = new HashSet<>();
        doAnswer(invocation -> {
            PurchaseOrder order = invocation.getArgument(0); seen.add(order.id);
            throw new IllegalStateException("Provider temporarily unavailable");
        }).when(gateway).reconcile(any());
        doAnswer(invocation -> { payos.deferReconciliation(invocation.getArgument(0)); return null; })
                .when(gateway).deferReconciliation(any());
        AppProperties app = mock(AppProperties.class); when(app.paymentMode()).thenReturn("payos");
        BackgroundJobs jobs = new BackgroundJobs(db, quizzes, mock(OutboxService.class), gateway, app);
        jobs.reconcile();
        assertEquals(100, seen.size());
        jobs.reconcile();
        assertEquals(expected, seen);
        verify(gateway, times(105)).reconcile(any());
    }
    @Test
    void concurrentDemotionsOfDifferentAdminsKeepOneUsableAdmin() throws Exception {
        Fixture f = fixture();
        new TransactionTemplate(manager).executeWithoutResult(status -> db.get(Account.class, f.student().id).role = vn.learnhub.user.Role.ADMIN);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> outcomes = new ArrayList<>();
            for (Long id : List.of(f.owner().id, f.student().id))
                outcomes.add(pool.submit(() -> {
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    try { admin.updateUser(f.owner(), id, "STUDENT", true, "Concurrent demotion"); return true; }
                    catch (IllegalArgumentException expected) { return false; }
                }));
            start.countDown();
            int successes = 0;
            for (Future<Boolean> outcome : outcomes) if (outcome.get(15, TimeUnit.SECONDS)) successes++;
            assertEquals(1, successes);
            new TransactionTemplate(manager).executeWithoutResult(status -> assertEquals(1, db.count(
                    "select count(u) from Account u where u.id in :ids and u.role=:role and u.active=true and u.verified=true",
                    "ids", List.of(f.owner().id, f.student().id), "role", vn.learnhub.user.Role.ADMIN)));
        } finally {
            start.countDown(); pool.shutdownNow();
            new TransactionTemplate(manager).executeWithoutResult(status -> {
                db.get(Account.class, f.owner().id).role = vn.learnhub.user.Role.ADMIN;
                db.get(Account.class, f.student().id).role = vn.learnhub.user.Role.STUDENT;
            });
        }
    }

}
