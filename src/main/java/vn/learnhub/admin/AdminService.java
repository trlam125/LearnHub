package vn.learnhub.admin;

import vn.learnhub.auth.Access;
import vn.learnhub.community.Feedback;
import vn.learnhub.course.Chapter;
import vn.learnhub.course.Consultation;
import vn.learnhub.course.Course;
import vn.learnhub.course.LearningPath;
import vn.learnhub.course.LearningLanguage;
import vn.learnhub.course.Lesson;
import vn.learnhub.course.Level;
import vn.learnhub.course.StudyClass;
import vn.learnhub.course.Teacher;
import vn.learnhub.portal.PlannedProduct;
import vn.learnhub.quiz.Choice;
import vn.learnhub.quiz.GameTopic;
import vn.learnhub.quiz.Question;
import vn.learnhub.quiz.Quiz;
import vn.learnhub.shared.audit.AuditService;
import vn.learnhub.shared.persistence.BaseEntity;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.user.Account;
import vn.learnhub.user.Role;
import vn.learnhub.vocabulary.Word;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Validator;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class AdminService {
    public record Spec(String key, String title, Class<? extends BaseEntity> type, List<String> fields) {
    }

    public record Option(String value, String label) {
    }

    public record Input(String name, String label, String type, Object value, List<Option> options, boolean required) {
    }

    private final Store db;
    private final Access access;
    private final Validator validator;
    private final AuditService audit;
    private final Map<String, Spec> specs = new LinkedHashMap<>();

    public AdminService(Store db, Access access, Validator validator, AuditService audit) {
        this.db = db;
        this.access = access;
        this.validator = validator;
        this.audit = audit;
        add("teachers", "Giáo viên", Teacher.class, "name language roleText bio education certificates experience philosophy portraitUrl facebook zalo community contactEmail published position");
        add("courses", "Khóa học", Course.class, "title slug language summary description teacherName kind price accessDays published");
        add("chapters", "Chương", Chapter.class, "course title position");
        add("lessons", "Bài học", Lesson.class, "chapter title content position preview published");
        add("paths", "Lộ trình", LearningPath.class, "slug title language symbol category description published position");
        add("levels", "Cấp độ", Level.class, "path number title description course published");
        add("words", "Từ vựng", Word.class, "lesson hanzi pinyin meaning sinoVietnamese exampleZh exampleVi audioUrl position");
        add("quizzes", "Bài kiểm tra", Quiz.class, "lesson title minutes maxAttempts passScore published");
        add("questions", "Câu hỏi", Question.class, "quiz text kind points explanation position");
        add("choices", "Lựa chọn", Choice.class, "question text correct");
        add("classes", "Lớp học", StudyClass.class, "course teacher title schedule startsOn sessions capacity features accepting published");
        add("topics", "Trò chơi", GameTopic.class, "title description symbol quiz published position");
        add("products", "Sắp ra mắt", PlannedProduct.class, "title language symbol description published position");
    }

    private void add(String key, String title, Class<? extends BaseEntity> type, String fields) {
        specs.put(key, new Spec(key, title, type, List.of(fields.split(" "))));
    }

    public Collection<Spec> kinds() {
        return specs.values();
    }

    public Spec spec(String key) {
        var s = specs.get(key);
        if (s == null) throw new IllegalArgumentException("Mục quản trị không hợp lệ.");
        return s;
    }

    public List<?> rows(String key, int page) {
        var s = spec(key);
        if (page < 0 || page > 100000) throw new IllegalArgumentException("Trang không hợp lệ.");
        return db.query(s.type(), "select x from " + s.type().getSimpleName() + " x order by x.id desc").setFirstResult(page * 50).setMaxResults(50).getResultList();
    }

    public String label(Object object) {
        for (String name : List.of("title", "name", "hanzi", "text", "email"))
            try {
                Object value = object.getClass().getField(name).get(object);
                if (value != null && !value.toString().isBlank()) {
                    String text = value.toString();
                    return text.length() > 100 ? text.substring(0, 100) + "…" : text;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        return object.getClass().getSimpleName();
    }

    private String fieldLabel(String name) {
        if (name.equals("facebook")) return "Liên kết Facebook";
        if (name.equals("zalo")) return "Liên kết Zalo";
        if (name.equals("community")) return "Nhóm học tập";
        if (name.equals("contactEmail")) return "Email liên hệ";
        if (name.equals("language")) return "Ngôn ngữ giảng dạy";
        if (name.equals("hanzi")) return "Từ / cụm từ tiếng Anh hoặc tiếng Trung";
        if (name.equals("pinyin")) return "Phiên âm (IPA / Pinyin)";
        if (name.equals("exampleZh")) return "Ví dụ bằng ngôn ngữ đang học";
        if (name.equals("category")) return "Nhóm lộ trình";
        if (name.equals("symbol")) return "Ký hiệu hiển thị";
        if (name.equals("number")) return "Thứ tự cấp độ (1–9)";
        return Map.ofEntries(Map.entry("title", "Tiêu đề"), Map.entry("name", "Họ tên"), Map.entry("published", "Xuất bản"), Map.entry("position", "Thứ tự"), Map.entry("preview", "Học thử miễn phí"), Map.entry("price", "Học phí VND"), Map.entry("accessDays", "Số ngày học"), Map.entry("teacherName", "Tên giáo viên hiển thị"), Map.entry("kind", "Loại nội dung"), Map.entry("roleText", "Chức danh"), Map.entry("bio", "Giới thiệu"), Map.entry("education", "Học vấn, mỗi dòng một mục"), Map.entry("certificates", "Chứng chỉ, mỗi dòng một mục"), Map.entry("experience", "Kinh nghiệm"), Map.entry("philosophy", "Phương pháp giảng dạy"), Map.entry("portraitUrl", "Liên kết ảnh"), Map.entry("course", "Khóa học"), Map.entry("chapter", "Chương"), Map.entry("lesson", "Bài học"), Map.entry("quiz", "Bài kiểm tra"), Map.entry("question", "Câu hỏi"), Map.entry("text", "Nội dung"), Map.entry("correct", "Lựa chọn đúng"), Map.entry("hanzi", "Chữ Hán"), Map.entry("pinyin", "Pinyin"), Map.entry("meaning", "Nghĩa tiếng Việt"), Map.entry("sinoVietnamese", "Hán Việt"), Map.entry("exampleZh", "Ví dụ tiếng Trung"), Map.entry("exampleVi", "Dịch ví dụ"), Map.entry("audioUrl", "Liên kết bản ghi âm"), Map.entry("minutes", "Thời gian phút"), Map.entry("maxAttempts", "Số lượt tối đa"), Map.entry("passScore", "Điểm đạt /10"), Map.entry("points", "Trọng số"), Map.entry("explanation", "Giải thích"), Map.entry("path", "Lộ trình"), Map.entry("number", "Cấp số 1–9"), Map.entry("teacher", "Giáo viên"), Map.entry("schedule", "Lịch học"), Map.entry("startsOn", "Ngày khai giảng"), Map.entry("sessions", "Số buổi"), Map.entry("capacity", "Sĩ số mô tả"), Map.entry("features", "Quyền lợi, mỗi dòng một mục"), Map.entry("accepting", "Nhận tư vấn"), Map.entry("content", "Nội dung văn bản"), Map.entry("description", "Mô tả"), Map.entry("summary", "Giới thiệu ngắn"), Map.entry("subject", "Môn học"), Map.entry("category", "Nhóm: hsk, advanced, other"), Map.entry("symbol", "Ký hiệu chữ Hán"), Map.entry("slug", "Tên đường dẫn")).getOrDefault(name, name);
    }

    public List<Input> inputs(String key, Long id) {
        Spec s = spec(key);
        Object entity = id == null ? instantiate(s.type()) : db.get(s.type(), id);
        List<Input> result = new ArrayList<>();
        for (String name : s.fields())
            try {
                Field f = s.type().getField(name);
                Object value = f.get(entity);
                String type = "text";
                List<Option> options = List.of();
                if (name.equals("language")) {
                    type = "select";
                    List<Option> languages = new ArrayList<>();
                    for (LearningLanguage language : LearningLanguage.values())
                        languages.add(new Option(language.getCode(), language.getLabel()));
                    if (s.type() == Teacher.class) languages.add(new Option("both", "Cả tiếng Anh và tiếng Trung"));
                    options = languages;
                } else if (name.equals("kind")) {
                    type = "select";
                    options = s.type() == Course.class
                            ? List.of(new Option("course", "Khóa học"), new Option("selfstudy", "Tự học"), new Option("game", "Trò chơi"))
                            : List.of(new Option("single", "Chọn một đáp án"), new Option("multiple", "Chọn nhiều đáp án"));
                } else if (name.equals("category")) {
                    type = "select";
                    options = List.of(new Option("hsk", "HSK nền tảng"), new Option("advanced", "HSK 7–9"),
                            new Option("cefr", "Tiếng Anh A1–C2"), new Option("ielts", "IELTS"), new Option("toeic", "TOEIC"), new Option("other", "Khác"));
                } else if (BaseEntity.class.isAssignableFrom(f.getType())) {
                    type = "relation";
                    @SuppressWarnings("unchecked") Class<? extends BaseEntity> relation = (Class<? extends BaseEntity>) f.getType();
                    options = new ArrayList<>(db.query(relation, "select x from " + relation.getSimpleName() + " x order by x.id desc").setMaxResults(2000).getResultList().stream().map(x -> new Option(x.id.toString(), "#" + x.id + " · " + label(x))).toList());
                    if (value instanceof BaseEntity selected && options.stream().noneMatch(o -> o.value().equals(selected.id.toString())))
                        options.addFirst(new Option(selected.id.toString(), "#" + selected.id + " · " + label(selected)));
                    value = value == null ? "" : ((BaseEntity) value).id.toString();
                } else if (f.getType() == boolean.class) type = "checkbox";
                else if (f.getType() == LocalDate.class) type = "date";
                else if (f.getType() == int.class || f.getType() == long.class || f.getType() == BigDecimal.class)
                    type = "number";
                else if (Set.of("content", "description", "bio", "education", "certificates", "experience", "philosophy", "features", "explanation", "text").contains(name))
                    type = "textarea";
                boolean required = f.isAnnotationPresent(jakarta.validation.constraints.NotBlank.class) || f.isAnnotationPresent(jakarta.validation.constraints.NotNull.class);
                result.add(new Input(name, fieldLabel(name), type, value, options, required));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        return result;
    }

    public List<Option> relationOptions(String key, String fieldName, String search) {
        Spec s = spec(key);
        if (!s.fields().contains(fieldName) || search.length() > 100)
            throw new IllegalArgumentException("Mục tìm kiếm không hợp lệ.");
        try {
            Class<?> relation = s.type().getField(fieldName).getType();
            if (!BaseEntity.class.isAssignableFrom(relation)) throw new IllegalArgumentException("Mục tìm kiếm không hợp lệ.");
            String query = search.trim();
            String number = query.startsWith("#") ? query.substring(1) : query;
            Long id = number.matches("[0-9]{1,18}") ? Long.valueOf(number) : null;
            String labelField = null;
            for (String name : List.of("title", "name", "hanzi", "text", "email")) {
                try { relation.getField(name); labelField = name; break; }
                catch (NoSuchFieldException ignored) { }
            }
            String jpql = "select x from " + relation.getSimpleName() + " x";
            List<Object> parameters = new ArrayList<>();
            if (!query.isBlank()) {
                jpql += labelField == null ? " where x.id=:id" : " where (x.id=:id or lower(x." + labelField + ") like :q)";
                parameters.add("id"); parameters.add(id == null ? -1L : id);
                if (labelField != null) { parameters.add("q"); parameters.add("%" + query.toLowerCase(Locale.ROOT) + "%"); }
            }
            return db.query(relation, jpql + " order by x.id desc", parameters.toArray()).setMaxResults(50).getResultList().stream()
                    .map(x -> new Option(((BaseEntity) x).id.toString(), "#" + ((BaseEntity) x).id + " · " + label(x))).toList();
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Mục tìm kiếm không hợp lệ.");
        }
    }

    private BaseEntity instantiate(Class<? extends BaseEntity> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public Long save(Account actor, String key, Long id, Map<String, String> values) {
        access.requireStaff(actor);
        Spec s = spec(key);
        BaseEntity entity = id == null ? instantiate(s.type()) : db.lock(s.type(), id);
        if (id != null && entity instanceof Quiz quiz) {
            db.entityManager().refresh(quiz, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            String lessonId = values.getOrDefault("lesson", "").trim();
            if (!Objects.equals(quiz.lesson.id.toString(), lessonId)
                    // A scalar current read avoids stale repeatable-read snapshots and
                    // does not lock eagerly fetched users while holding the quiz lock.
                    && !db.entityManager().createNativeQuery(
                            "select id from quiz_attempt where quiz_id=:id and submitted_at is null limit 1 for update")
                            .setParameter("id", id).getResultList().isEmpty())
                throw new IllegalArgumentException("Không thể chuyển bài kiểm tra sang bài học khác khi còn lượt chưa nộp. Hãy chờ các lượt làm kết thúc.");
        }
        Quiz previousQuiz = relatedQuiz(entity);
        Long previousQuizId = previousQuiz == null ? null : previousQuiz.id;
        for (String name : s.fields())
            try {
                Field field = s.type().getField(name);
                String value = values.getOrDefault(name, "").trim();
                Class<?> type = field.getType();
                Object converted;
                if (BaseEntity.class.isAssignableFrom(type))
                    converted = value.isBlank() ? null : db.get(type, Long.valueOf(value));
                else if (type == boolean.class) converted = value.equals("true") || value.equals("on");
                else if (type == int.class) converted = Integer.valueOf(value);
                else if (type == long.class) converted = Long.valueOf(value);
                else if (type == BigDecimal.class) converted = new BigDecimal(value);
                else if (type == LocalDate.class) converted = value.isBlank() ? null : LocalDate.parse(value);
                else converted = value;
                field.set(entity, converted);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Giá trị số không hợp lệ.");
            }
        var errors = validator.validate(entity);
        if (!errors.isEmpty())
            throw new IllegalArgumentException("Vui lòng kiểm tra lại: " + fieldLabel(errors.iterator().next().getPropertyPath().toString()));
        validate(entity);
        if (id == null) db.add(entity);
        Quiz currentQuiz = relatedQuiz(entity);
        if (previousQuizId != null || currentQuiz != null) {
            // Both ends of a moved question/choice must be reviewed again.
            Set<Long> quizIds = new TreeSet<>();
            if (previousQuizId != null) quizIds.add(previousQuizId);
            if (currentQuiz != null) quizIds.add(currentQuiz.id);
            for (Long quizId : quizIds) {
                Quiz affected = db.lock(Quiz.class, quizId);
                db.entityManager().refresh(affected, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
                affected.published = false;
            }
            // Ensure refresh() in QuizService.start reads the unpublished state
            // even when both service calls share one transaction (for example, in tests).
            db.entityManager().flush();
        }
        audit.record(actor, "content_saved", key + ":" + entity.id, "Đã cập nhật nội dung.");
        return entity.id;
    }

    private void https(String url) {
        if (!url.isBlank() && (!url.startsWith("https://") || java.net.URI.create(url).getHost() == null))
            throw new IllegalArgumentException("Kiểm tra lại liên kết ảnh, bản ghi âm hoặc liên hệ.");
    }

    private Quiz relatedQuiz(BaseEntity entity) {
        if (entity instanceof Question question) return question.quiz;
        if (entity instanceof Choice choice && choice.question != null) return choice.question.quiz;
        return null;
    }

    private void validate(BaseEntity e) {
        if (e instanceof Course c) {
            c.subject = LearningLanguage.fromCode(c.language).getLabel();
            if (db.count("select count(l) from Level l where l.course.id=:id and l.path.language<>:language", "id", c.id, "language", c.language) > 0)
                throw new IllegalArgumentException("Khóa học và lộ trình liên kết phải cùng ngôn ngữ.");
            if (db.count("select count(s) from StudyClass s where s.course.id=:id and s.teacher.language<>'both' and s.teacher.language<>:language", "id", c.id, "language", c.language) > 0)
                throw new IllegalArgumentException("Khóa học đang có lớp với giáo viên thuộc ngôn ngữ khác.");
            if (!Set.of("course", "selfstudy", "game").contains(c.kind) || !c.slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*"))
                throw new IllegalArgumentException("Chọn nhóm khóa học và nhập tên đường dẫn bằng chữ thường không dấu, số và dấu gạch ngang.");
        }
        if (e instanceof Question q && (!Set.of("single", "multiple").contains(q.kind) || q.text.isBlank()))
            throw new IllegalArgumentException("Loại/câu hỏi không hợp lệ.");
        if (e instanceof Choice c && c.text.isBlank()) throw new IllegalArgumentException("Nhập nội dung lựa chọn.");
        if (e instanceof Lesson l && !l.preview && l.id != null)
            for (GameTopic topic : db.list(GameTopic.class, "select t from GameTopic t where t.quiz.lesson.id=:id", "id", l.id))
                topic.published = false;
        if (e instanceof Teacher t) {
            if (!t.language.equals("both") && db.count("select count(s) from StudyClass s where s.teacher.id=:id and s.course.language<>:language", "id", t.id, "language", t.language) > 0)
                throw new IllegalArgumentException("Giáo viên đang có lớp thuộc ngôn ngữ khác. Chọn cả hai tiếng hoặc sửa lớp trước.");
            https(t.portraitUrl);
            https(t.facebook);
            https(t.zalo);
            https(t.community);
        }
        if (e instanceof Word w) https(w.audioUrl);
        if (e instanceof Level l && (l.number < 1 || l.number > 9 || l.published && (l.course == null || !l.course.published)))
            throw new IllegalArgumentException("Cấp 1–9 và khóa đã xuất bản là bắt buộc khi mở cấp độ.");
        if (e instanceof Level l && l.course != null && !l.path.language.equals(l.course.language))
            throw new IllegalArgumentException("Cấp độ và khóa học phải cùng ngôn ngữ.");
        if (e instanceof LearningPath p) {
            boolean validCategory = p.language.equals("en") ? Set.of("cefr", "ielts", "toeic", "other").contains(p.category) : Set.of("hsk", "advanced", "other").contains(p.category);
            if (!validCategory) throw new IllegalArgumentException("Nhóm lộ trình không phù hợp với ngôn ngữ đã chọn.");
            if (db.count("select count(l) from Level l where l.path.id=:id and l.course is not null and l.course.language<>:language", "id", p.id, "language", p.language) > 0)
                throw new IllegalArgumentException("Lộ trình đang có khóa học thuộc ngôn ngữ khác.");
        }
        if (e instanceof StudyClass s && !s.teacher.language.equals("both") && !s.teacher.language.equals(s.course.language))
            throw new IllegalArgumentException("Chọn giáo viên giảng dạy ngôn ngữ của khóa học.");
        if (e instanceof GameTopic t && t.published && (!t.quiz.published || !t.quiz.lesson.preview))
            throw new IllegalArgumentException("Trò chơi cần bài kiểm tra đã công bố và bài học thử miễn phí.");
        if (e instanceof Quiz q && q.published) {
            List<vn.learnhub.quiz.QuizEngine.Question> list = new ArrayList<>();
            for (Question x : db.list(Question.class, "select x from Question x where x.quiz.id=:q", "q", q.id)) {
                var choices = db.list(Choice.class, "select x from Choice x where x.question.id=:q", "q", x.id).stream().map(c -> new vn.learnhub.quiz.QuizEngine.Choice(c.id, c.text, c.correct)).toList();
                list.add(new vn.learnhub.quiz.QuizEngine.Question(x.id, x.text, x.kind, x.points, x.explanation, choices));
            }
            vn.learnhub.quiz.QuizEngine.validate(list);
        }
    }

    @Transactional
    public void updateUser(Account actor, Long id, String role, boolean active, String reason) {
        access.requireAdmin(actor);
        if (reason.isBlank()) throw new IllegalArgumentException("Nhập lý do.");
        // Serialize role changes across different accounts, including demotions.
        db.entityManager().createNativeQuery("select id from admin_role_guard where id=1 for update").getSingleResult();
        Account target = db.lock(Account.class, id);
        db.entityManager().refresh(target, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        Role selected = Role.valueOf(role);
        if (target.active && target.verified && target.role == Role.ADMIN && (!active || selected != Role.ADMIN) && db.count("select count(u) from Account u where u.role=:role and u.active=true and u.verified=true", "role", Role.ADMIN) <= 1)
            throw new IllegalArgumentException("Không thể tắt quản trị viên cuối cùng.");
        target.role = selected;
        target.active = active;
        target.sessionVersion++;
        audit.record(actor, "user_changed", id.toString(), reason);
    }

    @Transactional
    public void contactStatus(Account actor, Long id, String status) {
        access.requireStaff(actor);
        if (!Set.of("new", "contacted", "closed").contains(status))
            throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        db.lock(Consultation.class, id).status = status;
        audit.record(actor, "consultation_status", id.toString(), status);
    }

    @Transactional
    public void resolve(Account actor, Long id) {
        access.requireStaff(actor);
        db.lock(Feedback.class, id).resolved = true;
    }
}
