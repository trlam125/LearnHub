package vn.learnhub;

import vn.learnhub.community.Activity;
import vn.learnhub.community.ActivityService;
import vn.learnhub.course.Chapter;
import vn.learnhub.course.Course;
import vn.learnhub.course.Lesson;
import vn.learnhub.course.LearningPath;
import vn.learnhub.admin.AdminService;
import vn.learnhub.course.Teacher;
import vn.learnhub.learning.Enrollment;
import vn.learnhub.learning.LearningProgressService;
import vn.learnhub.payment.OrderStatus;
import vn.learnhub.payment.PaymentService;
import vn.learnhub.payment.PurchaseOrder;
import vn.learnhub.quiz.Attempt;
import vn.learnhub.quiz.Choice;
import vn.learnhub.quiz.Question;
import vn.learnhub.quiz.Quiz;
import vn.learnhub.quiz.QuizService;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.storage.Resource;
import vn.learnhub.storage.FileService;
import vn.learnhub.user.Account;
import vn.learnhub.vocabulary.VocabularyService;
import vn.learnhub.vocabulary.Word;
import vn.learnhub.vocabulary.WordProgress;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties = {"spring.profiles.active=test", "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:learnhubtest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=DAY;DB_CLOSE_DELAY=-1}", "spring.datasource.username=${TEST_DB_USER:sa}", "spring.datasource.password=${TEST_DB_PASSWORD:}", "spring.flyway.locations=${TEST_FLYWAY_LOCATIONS:classpath:db/migration}", "app.jobs-enabled=false", "app.seed-demo=true", "app.bootstrap-email=admin@example.test", "app.bootstrap-password=Admin-test-password-2026!", "app.mail-mode=console", "app.payment-mode=manual", "app.payos-client-id=test-client", "app.payos-api-key=test-api", "app.payos-checksum-key=test-checksum"})
@AutoConfigureMockMvc
@Transactional
class PlatformIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    Store db;
    @Autowired
    LearningProgressService progress;
    @Autowired
    PaymentService payments;
    @Autowired
    ActivityService activities;
    @Autowired
    VocabularyService study;
    @Autowired
    QuizService quizzes;
    @Autowired
    PasswordEncoder passwords;
    @Autowired
    AdminService admin;
    @Autowired
    FileService files;
    @Autowired
    vn.learnhub.learning.EnrollmentService grants;
    Account student, other;
    Course course;
    Lesson preview, privateLesson;
    Quiz quiz;
    Choice right;
    Word word, secret;

    @BeforeEach
    void setup() {
        student = new Account();
        student.name = "Private real name";
        student.email = "student@example.test";
        student.password = passwords.encode("Student-test-password-2026!");
        student.verified = true;
        db.add(student);
        other = new Account();
        other.name = "Other";
        other.email = "other@example.test";
        other.password = student.password;
        other.verified = true;
        db.add(other);
        course = new Course();
        course.slug = "integration-course";
        course.title = "Integration course";
        course.summary = "Summary";
        course.description = "Description";
        course.teacherName = "Teacher";
        course.published = true;
        db.add(course);
        Chapter ch = new Chapter();
        ch.course = course;
        ch.title = "Chapter";
        db.add(ch);
        preview = new Lesson();
        preview.chapter = ch;
        preview.title = "Preview";
        preview.preview = true;
        preview.published = true;
        db.add(preview);
        privateLesson = new Lesson();
        privateLesson.chapter = ch;
        privateLesson.title = "Private lesson";
        privateLesson.content = "secret-content";
        privateLesson.published = true;
        db.add(privateLesson);
        quiz = new Quiz();
        quiz.lesson = preview;
        quiz.title = "Quiz";
        quiz.published = true;
        db.add(quiz);
        Question q = new Question();
        q.quiz = quiz;
        q.text = "2+2?";
        q.explanation = "secret-explanation";
        db.add(q);
        right = new Choice();
        right.question = q;
        right.text = "4";
        right.correct = true;
        db.add(right);
        Choice wrong = new Choice();
        wrong.question = q;
        wrong.text = "5";
        db.add(wrong);
        word = new Word();
        word.lesson = preview;
        word.hanzi = "你好";
        word.pinyin = "nǐ hǎo";
        word.meaning = "Xin chào";
        db.add(word);
        secret = new Word();
        secret.lesson = privateLesson;
        secret.hanzi = "秘密";
        secret.pinyin = "mìmì";
        secret.meaning = "private-word-secret";
        db.add(secret);
    }

    @Test
    void publicPagesAndTemplates() throws Exception {
        for (String path : List.of("/", "/giao-vien", "/hsk", "/khoa-hoc", "/courses", "/tro-choi", "/bang-xep-hang", "/dang-phat-trien", "/login", "/register", "/policies"))
            mvc.perform(get(path)).andExpect(status().isOk());
    }

    @Test
    void csrfRequiredForMutations() throws Exception {
        mvc.perform(post("/lessons/" + preview.id + "/complete").with(user(student.email))).andExpect(status().isForbidden());
    }

    @Test
    void privateLessonDenied() throws Exception {
        mvc.perform(get("/lessons/" + privateLesson.id).with(user(student.email))).andExpect(status().isForbidden());
    }

    @Test
    void bookmarksAndPrivateWords() throws Exception {
        study.bookmark(student, word.id, true);
        study.bookmark(student, word.id, true);
        assertEquals(1, db.count("select count(p) from WordProgress p where p.user.id=:u", "u", student.id));
        mvc.perform(post("/tu-vung/" + secret.id + "/luu").with(user(student.email)).with(csrf()).param("saved", "1")).andExpect(status().isForbidden());
    }

    @Test
    void expiredSavedWordHidden() throws Exception {
        Enrollment e = new Enrollment();
        e.user = student;
        e.course = course;
        e.expiresAt = Instant.now().minusSeconds(1);
        db.add(e);
        WordProgress p = new WordProgress();
        p.user = student;
        p.word = secret;
        p.saved = true;
        db.add(p);
        mvc.perform(get("/tu-da-luu").with(user(student.email))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-word-secret"))));
    }

    @Test
    void reviewOncePerDay() {
        study.review(student, word.id, true);
        study.review(student, word.id, true);
        assertEquals(1, db.count("select count(a) from Activity a where a.user.id=:u", "u", student.id));
    }

    @Test
    void paymentSnapshotAndReplay() {
        PurchaseOrder o = payments.order(student, course.id);
        long amount = o.amount;
        course.price += 100000;
        payments.confirm(o.id, amount, "manual:test-one", null);
        Instant expiry = db.first(Enrollment.class, "select e from Enrollment e where e.user.id=:u", "u", student.id).orElseThrow().expiresAt;
        payments.confirm(o.id, amount, "manual:test-one", null);
        assertEquals(expiry, db.first(Enrollment.class, "select e from Enrollment e where e.user.id=:u", "u", student.id).orElseThrow().expiresAt);
    }

    @Test
    void wrongAmountRejected() {
        PurchaseOrder o = payments.order(student, course.id);
        assertThrows(IllegalArgumentException.class, () -> payments.confirm(o.id, 1, "manual:bad", null));
    }

    @Test
    void pendingOrderReused() {
        assertEquals(payments.order(student, course.id).id, payments.order(student, course.id).id);
    }

    @Test
    void browserReturnCannotPay() throws Exception {
        PurchaseOrder o = payments.order(student, course.id);
        mvc.perform(get("/orders/" + o.id + "?status=PAID").with(user(student.email))).andExpect(status().isOk());
        assertEquals(OrderStatus.PENDING, db.get(PurchaseOrder.class, o.id).status);
    }

    @Test
    void orderOwnerOnly() throws Exception {
        PurchaseOrder o = payments.order(other, course.id);
        mvc.perform(get("/orders/" + o.id).with(user(student.email))).andExpect(status().isForbidden());
    }

    @Test
    void attemptResumesAndNoAnswerLeak() throws Exception {
        Attempt a = quizzes.start(student, quiz.id);
        assertEquals(a.id, quizzes.start(student, quiz.id).id);
        mvc.perform(get("/attempts/" + a.id).with(user(student.email))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-explanation"))));
    }

    @Test
    void immutableQuestionAndDuplicateSubmit() {
        Attempt a = quizzes.start(student, quiz.id);
        right.correct = false;
        var answers = Map.of(right.question.id.toString(), List.of(right.id));
        assertEquals(0, java.math.BigDecimal.TEN.compareTo(quizzes.save(student, a.id, answers, true).score));
        assertEquals(0, java.math.BigDecimal.TEN.compareTo(quizzes.save(student, a.id, Map.of(), true).score));
    }

    @Test
    void deadlineRejectsLateAnswers() {
        Attempt a = quizzes.start(student, quiz.id);
        a.deadline = Instant.now().minusSeconds(1);
        assertEquals(0, java.math.BigDecimal.ZERO.compareTo(quizzes.save(student, a.id, Map.of(right.question.id.toString(), List.of(right.id)), true).score));
    }

    @Test
    void submittedResultsRender() throws Exception {
        Attempt a = quizzes.start(student, quiz.id);
        quizzes.save(student, a.id, Map.of(right.question.id.toString(), List.of(right.id)), true);
        mvc.perform(get("/attempts/" + a.id).with(user(student.email))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("secret-explanation")));
    }

    @Test
    void adminFinancialRoutesBlockedToStudent() throws Exception {
        mvc.perform(get("/admin/finance").with(user(student.email))).andExpect(status().isForbidden());
    }

    @Test
    void adminPagesRender() throws Exception {
        for (String path : List.of("/admin", "/admin/content/teachers", "/admin/content/words/new", "/admin/upload", "/admin/results", "/admin/inbox", "/admin/finance", "/admin/users", "/admin/operations"))
            mvc.perform(get(path).with(user("admin@example.test").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void leaderboardOptInUsesAlias() throws Exception {
        activities.award(student, "test", "lesson", 10);
        mvc.perform(get("/bang-xep-hang")).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Private real name"))));
        student.rankingVisible = true;
        student.publicName = "Alias only";
        mvc.perform(get("/bang-xep-hang")).andExpect(content().string(org.hamcrest.Matchers.containsString("Alias only"))).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Private real name"))));
    }

    @Test
    void completionOnlyAwardsOnce() {
        progress.complete(student, preview.id);
        progress.complete(student, preview.id);
        assertEquals(1, db.count("select count(a) from Activity a where a.user.id=:u and a.kind='lesson'", "u", student.id));
    }

    @Test
    void flashcardScriptEscaped() throws Exception {
        word.meaning = "</script><script>alert(1)</script>";
        mvc.perform(get("/flashcards?lesson=" + preview.id).with(user(student.email))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("</script><script>alert(1)</script>"))));
    }

    @Test
    void englishHomeAndCatalogExcludeChineseContent() throws Exception {
        mvc.perform(get("/").param("lang", "en")).andExpect(status().isOk())
                .andExpect(model().attribute("languageCode", "en"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Greetings · Chào hỏi")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("HSK · Nền tảng Hán ngữ"))));
        mvc.perform(get("/courses").param("lang", "en")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tiếng Anh giao tiếp · Khóa mẫu")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Integration course"))));
    }

    @Test
    void englishSelectionPersistsAndChineseCanBeRestored() throws Exception {
        var result = mvc.perform(get("/").param("lang", "en")).andExpect(status().isOk()).andReturn();
        var session = (org.springframework.mock.web.MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/lo-trinh").session(session)).andExpect(status().isOk())
                .andExpect(model().attribute("languageCode", "en"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tiếng Anh · Lộ trình A1–C2")));
        mvc.perform(get("/hsk").session(session)).andExpect(status().isOk())
                .andExpect(model().attribute("languageCode", "zh"));
        mvc.perform(get("/tieng-anh")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/?lang=en"));
        mvc.perform(get("/tieng-trung")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/?lang=zh"));
    }

    @Test
    void englishVocabularyAndDueCardsUseTheirOwnLanguage() throws Exception {
        Course english = db.first(Course.class, "select c from Course c where c.slug='english-a1-mau'").orElseThrow();
        Word englishWord = db.first(Word.class, "select w from Word w where w.lesson.chapter.course.id=:id and w.hanzi='hello'", "id", english.id).orElseThrow();
        study.bookmark(student, word.id, true);
        study.bookmark(student, englishWord.id, true);
        assertEquals(List.of(englishWord.id), study.deck(student, null, "saved", "en").stream().map(w -> w.id).toList());
        assertEquals(List.of(word.id), study.deck(student, null, "due", "zh").stream().map(w -> w.id).toList());
        mvc.perform(get("/tu-da-luu").param("lang", "en").with(user(student.email))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-language=\"en-US\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("你好"))));
        mvc.perform(get("/flashcards").param("lesson", englishWord.lesson.id.toString()).param("lang", "zh").with(user(student.email)))
                .andExpect(status().isOk()).andExpect(model().attribute("languageCode", "en"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"language\":\"en-US\"")));
    }

    @Test
    void languageCatalogsAndGamesRenderInBothLanguages() throws Exception {
        for (String language : List.of("en", "zh"))
            for (String route : List.of("/lo-trinh", "/courses", "/khoa-hoc", "/giao-vien", "/tro-choi", "/dang-phat-trien"))
                mvc.perform(get(route).param("lang", language)).andExpect(status().isOk())
                        .andExpect(model().attribute("languageCode", language));
        mvc.perform(get("/giao-vien").param("lang", "en")).andExpect(content().string(org.hamcrest.Matchers.containsString("Giáo viên tiếng Anh · Hồ sơ mẫu")));
        mvc.perform(get("/tro-choi").param("lang", "en")).andExpect(content().string(org.hamcrest.Matchers.containsString("Everyday English")));
    }

    @Test
    void adminOffersLanguageSelectionAndRejectsCrossLanguageLevel() throws Exception {
        mvc.perform(get("/admin/content/courses/new").with(user("admin@example.test").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Ngôn ngữ giảng dạy")));
        Account owner = db.first(Account.class, "select u from Account u where u.email='admin@example.test'").orElseThrow();
        LearningPath chinese = db.first(LearningPath.class, "select p from LearningPath p where p.slug='hsk'").orElseThrow();
        Course english = db.first(Course.class, "select c from Course c where c.slug='english-a1-mau'").orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> admin.save(owner, "levels", null,
                Map.of("path", chinese.id.toString(), "number", "9", "title", "Invalid mixed level", "description", "", "course", english.id.toString(), "published", "true")));
    }

    @Test
    void privateVideoLinksRequireEnrollmentAndAreNotLeakedByLessonPage() throws Exception {
        Resource video = new Resource();
        video.lesson = privateLesson;
        video.title = "Private video";
        video.materialType = "video";
        video.externalUrl = "https://drive.google.com/file/d/private-video-ref/view";
        video.embedUrl = "https://drive.google.com/file/d/private-video-ref/preview";
        video.reviewed = true;
        db.add(video);
        mvc.perform(get("/resources/" + video.id).with(user(student.email))).andExpect(status().isForbidden());
        mvc.perform(get("/lessons/" + privateLesson.id).with(user(student.email))).andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-video-ref"))));
        Enrollment enrollment = new Enrollment();
        enrollment.user = student;
        enrollment.course = course;
        enrollment.expiresAt = Instant.now().plusSeconds(3600);
        db.add(enrollment);
        mvc.perform(get("/resources/" + video.id).with(user(student.email))).andExpect(status().isFound())
                .andExpect(redirectedUrl(video.externalUrl)).andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/lessons/" + privateLesson.id).with(user(student.email))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(video.embedUrl)));
        enrollment.expiresAt = Instant.now().minusSeconds(1);
        mvc.perform(get("/resources/" + video.id).with(user(student.email))).andExpect(status().isForbidden());
    }

    @Test
    void unpublishedMaterialStaysHiddenAndLinkChangesAreRestricted() throws Exception {
        Account owner = db.first(Account.class, "select u from Account u where u.email='admin@example.test'").orElseThrow();
        files.link(owner, null, preview.id, "Draft material", "document", "https://example.com/private-document", "", false);
        Resource document = db.first(Resource.class, "select r from Resource r where r.title='Draft material'").orElseThrow();
        mvc.perform(get("/resources/" + document.id)).andExpect(status().isForbidden());
        mvc.perform(get("/lessons/" + preview.id)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Draft material"))));
        mvc.perform(post("/admin/materials/link").with(user(student.email)).with(csrf())
                .param("lessonId", preview.id.toString()).param("title", "Bad").param("materialType", "video").param("externalUrl", "https://example.com/video"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/materials/link").with(user(owner.email).roles("ADMIN"))
                .param("lessonId", preview.id.toString()).param("title", "Bad").param("materialType", "video").param("externalUrl", "https://example.com/video"))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCanAddUpdateAndHideExternalVideos() throws Exception {
        String owner = "admin@example.test";
        mvc.perform(post("/admin/materials/link").with(user(owner).roles("ADMIN")).with(csrf())
                .param("lessonId", preview.id.toString()).param("title", "Lecture video").param("materialType", "video")
                .param("externalUrl", "https://drive.google.com/file/d/lecture-one/view").param("reviewed", "true"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/upload"));
        Resource video = db.first(Resource.class, "select r from Resource r where r.title='Lecture video'").orElseThrow();
        assertEquals("https://drive.google.com/file/d/lecture-one/preview", video.embedUrl);
        mvc.perform(get("/admin/materials/" + video.id).with(user(owner).roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(post("/admin/materials/link").with(user(owner).roles("ADMIN")).with(csrf())
                .param("entityId", video.id.toString()).param("lessonId", preview.id.toString()).param("title", "Updated video")
                .param("materialType", "video").param("externalUrl", "https://1drv.ms/v/updated"))
                .andExpect(status().is3xxRedirection());
        assertFalse(video.reviewed);
        assertEquals("", video.embedUrl);
        mvc.perform(get("/resources/" + video.id)).andExpect(status().isForbidden());
    }
    @Test
    void localOfficeDownloadNeedsPermissionEvenWithoutDownloadParameter() throws Exception {
        Account owner = db.first(Account.class, "select u from Account u where u.email='admin@example.test'").orElseThrow();
        files.upload(owner, preview.id, "Restricted text", new org.springframework.mock.web.MockMultipartFile(
                "file", "notes.txt", "text/plain", "lesson notes".getBytes()), true, false);
        Resource r = db.first(Resource.class, "select r from Resource r where r.title='Restricted text'").orElseThrow();
        mvc.perform(get("/resources/" + r.id)).andExpect(status().isForbidden());
        mvc.perform(get("/resources/" + r.id).param("download", "true")).andExpect(status().isForbidden());
        files.details(owner, r.id, r.title, true, true);
        mvc.perform(get("/resources/" + r.id)).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment;")));
    }

    @Test
    void pdfInlineRemainsAvailableWithoutAttachmentPermission() throws Exception {
        Account owner = db.first(Account.class, "select u from Account u where u.email='admin@example.test'").orElseThrow();
        files.upload(owner, preview.id, "Inline PDF", new org.springframework.mock.web.MockMultipartFile(
                "file", "notes.pdf", "application/pdf", "%PDF-1.7\n%%EOF".getBytes()), true, false);
        Resource r = db.first(Resource.class, "select r from Resource r where r.title='Inline PDF'").orElseThrow();
        mvc.perform(get("/resources/" + r.id)).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("inline;")));
        mvc.perform(get("/resources/" + r.id).param("download", "true")).andExpect(status().isForbidden());
    }

    @Test
    void refundRestoresAccessThatExistedBeforePayment() {
        Account owner = db.first(Account.class, "select u from Account u where u.email='admin@example.test'").orElseThrow();
        PurchaseOrder order = payments.order(student, course.id);
        grants.grant(owner, student.id, course.id, 90, "Scholarship");
        Enrollment enrollment = db.first(Enrollment.class, "select e from Enrollment e where e.user.id=:u and e.course.id=:c", "u", student.id, "c", course.id).orElseThrow();
        Instant starts = enrollment.startsAt, expires = enrollment.expiresAt;
        payments.confirm(order.id, order.amount, "manual:refund-preserve", owner);
        assertTrue(enrollment.expiresAt.isAfter(expires));
        payments.refundRecorded(owner, order.id, "refund-preserve", "Test refund");
        assertTrue(enrollment.active);
        assertEquals(starts, enrollment.startsAt);
        assertEquals(expires, enrollment.expiresAt);
        assertNull(enrollment.sourceOrder);
        assertEquals(OrderStatus.REFUNDED, order.status);
    }

    @Test
    void refundWithoutEarlierAccessRevokesAndUnknownLegacySnapshotIsRejected() {
        Account owner = db.first(Account.class, "select u from Account u where u.email='admin@example.test'").orElseThrow();
        PurchaseOrder order = payments.order(student, course.id);
        payments.confirm(order.id, order.amount, "manual:refund-no-base", owner);
        payments.refundRecorded(owner, order.id, "refund-no-base", "Test refund");
        assertFalse(db.first(Enrollment.class, "select e from Enrollment e where e.user.id=:u", "u", student.id).orElseThrow().active);
        PurchaseOrder legacy = payments.order(other, course.id);
        payments.confirm(legacy.id, legacy.amount, "manual:legacy", owner);
        legacy.accessSnapshotCaptured = false;
        assertThrows(IllegalArgumentException.class, () -> payments.refundRecorded(owner, legacy.id, "refund-legacy", "Legacy"));
        assertTrue(db.first(Enrollment.class, "select e from Enrollment e where e.user.id=:u", "u", other.id).orElseThrow().active);
    }

    @Test
    void unverifiedAdminDoesNotAllowRemovingLastUsableAdmin() {
        Account owner = db.first(Account.class, "select u from Account u where u.email='admin@example.test'").orElseThrow();
        other.role = vn.learnhub.user.Role.ADMIN;
        other.verified = false;
        db.entityManager().flush();
        assertThrows(IllegalArgumentException.class, () -> admin.updateUser(owner, owner.id, "STUDENT", true, "Demote"));
        assertEquals(vn.learnhub.user.Role.ADMIN, owner.role);
    }

    @Test
    void editingAnswersWithdrawsPublishedQuizUntilReviewed() {
        Account owner = db.first(Account.class, "select u from Account u where u.email='admin@example.test'").orElseThrow();
        admin.save(owner, "choices", right.id, Map.of("question", right.question.id.toString(), "text", "4", "correct", "false"));
        assertFalse(quiz.published);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> quizzes.start(student, quiz.id));
    }

    @Test
    void movingChoiceWithdrawsBothAffectedQuizzes() {
        Account owner = db.first(Account.class, "select u from Account u where u.email='admin@example.test'").orElseThrow();
        Quiz destination = new Quiz();
        destination.lesson = privateLesson;
        destination.title = "Destination";
        destination.published = true;
        db.add(destination);
        Question question = new Question(); question.quiz = destination; question.text = "Destination question"; db.add(question);
        admin.save(owner, "choices", right.id, Map.of("question", question.id.toString(), "text", "4", "correct", "true"));
        assertFalse(quiz.published);
        assertFalse(destination.published);
    }

    @Test
    void unbookmarkExpiredWordOnlyChangesOwnersExistingBookmark() throws Exception {
        WordProgress own = new WordProgress(); own.user = student; own.word = secret; own.saved = true; db.add(own);
        WordProgress theirs = new WordProgress(); theirs.user = other; theirs.word = secret; theirs.saved = true; db.add(theirs);
        mvc.perform(get("/tu-da-luu").with(user(student.email)))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Từ trong bài học chưa truy cập được")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-word-secret"))));
        mvc.perform(post("/tu-vung/" + secret.id + "/luu").with(user(student.email)).with(csrf()).param("saved", "0"))
                .andExpect(redirectedUrl("/tu-da-luu"));
        assertFalse(own.saved);
        assertTrue(theirs.saved);
        study.bookmark(student, 999999999L, false);
        assertEquals(2, db.count("select count(p) from WordProgress p where p.word.id=:id", "id", secret.id));
    }

    @Test
    void savedDeckCanReachCardsAfterFirstHundred() throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 105; i++) {
            Word w = new Word(); w.lesson = preview; w.hanzi = "card-" + i; w.meaning = "Meaning"; w.pinyin = "test"; db.add(w);
            study.bookmark(student, w.id, true); ids.add(w.id);
        }
        var first = study.deckPage(student, null, "saved", "zh", null);
        assertEquals(ids.subList(0, 100), first.words().stream().map(w -> w.id).toList());
        var second = study.deckPage(student, null, "saved", "zh", first.nextAfter());
        assertEquals(ids.subList(100, 105), second.words().stream().map(w -> w.id).toList());
        assertNull(second.nextAfter());
        mvc.perform(get("/flashcards").with(user(student.email))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bộ tiếp theo")));
        mvc.perform(get("/flashcards").param("after", first.nextAfter().toString()).with(user(student.email)))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("card-104")));
    }

    @Test
    void dueDeckCursorDoesNotSkipCardsAfterFirstBatchIsReviewed() {
        for (int i = 0; i < 105; i++) {
            Word w = new Word(); w.lesson = preview; w.hanzi = "due-" + i; w.meaning = "Meaning"; w.pinyin = "test"; db.add(w);
            study.bookmark(student, w.id, true);
        }
        var first = study.deckPage(student, null, "due", "zh", null);
        for (Word w : first.words()) study.review(student, w.id, true);
        var next = study.deckPage(student, null, "due", "zh", first.nextAfter());
        assertEquals(5, next.words().size());
    }

    @Test
    void profileUpdateChangesOnlyPersonalFields() throws Exception {
        String originalPassword = student.password;
        long version = student.sessionVersion;
        mvc.perform(post("/tai-khoan").with(user(student.email)).with(csrf())
                .param("name", "Updated name").param("publicName", "Alias").param("rankingVisible", "true"))
                .andExpect(redirectedUrl("/tai-khoan"));
        db.entityManager().refresh(student);
        assertEquals("Updated name", student.name);
        assertEquals("Alias", student.publicName);
        assertTrue(student.rankingVisible);
        assertTrue(student.active);
        assertEquals(originalPassword, student.password);
        assertEquals(version, student.sessionVersion);
        assertEquals(vn.learnhub.user.Role.STUDENT, student.role);
    }

    @Test
    void lessonDeckCursorRespectsPositionBeforeId() {
        for (int i = 0; i < 105; i++) {
            Word w = new Word(); w.lesson = preview; w.hanzi = "position-" + i; w.pinyin = "test";
            w.meaning = "Meaning"; w.position = 105 - i; db.add(w);
        }
        var all = db.list(Word.class, "select w from Word w where w.lesson.id=:id order by w.position,w.id", "id", preview.id);
        var first = study.deckPage(student, preview.id, "lesson", "zh", null);
        var next = study.deckPage(student, preview.id, "lesson", "zh", first.nextAfter());
        var actual = new ArrayList<Long>();
        first.words().forEach(w -> actual.add(w.id)); next.words().forEach(w -> actual.add(w.id));
        assertEquals(all.stream().map(w -> w.id).toList(), actual);
    }

}
