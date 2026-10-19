package vn.learnhub.bootstrap;

import vn.learnhub.course.Chapter;
import vn.learnhub.course.Course;
import vn.learnhub.course.LearningPath;
import vn.learnhub.course.Lesson;
import vn.learnhub.course.Level;
import vn.learnhub.course.StudyClass;
import vn.learnhub.course.Teacher;
import vn.learnhub.portal.PlannedProduct;
import vn.learnhub.quiz.Choice;
import vn.learnhub.quiz.GameTopic;
import vn.learnhub.quiz.Question;
import vn.learnhub.quiz.Quiz;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.vocabulary.Word;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DemoData {
    private final Store db;

    public DemoData(Store db) {
        this.db = db;
    }

    private Course course(String slug, String title, String kind, long price, String teacher) {
        return db.first(Course.class, "select c from Course c where c.slug=:s", "s", slug).orElseGet(() -> {
            Course c = new Course();
            c.slug = slug;
            c.title = title;
            c.kind = kind;
            c.price = price;
            c.teacherName = teacher;
            c.summary = "Bài học mẫu với từ vựng, ví dụ và bài tập luyện tập.";
            c.description = "Khóa học mẫu giúp bạn làm quen với cách học theo từng bài. Chương trình HSK đầy đủ được giáo viên công bố riêng.";
            c.published = true;
            return db.add(c);
        });
    }

    private Chapter chapter(Course c, String title) {
        Chapter x = new Chapter();
        x.course = c;
        x.title = title;
        return db.add(x);
    }

    private Lesson lesson(Chapter c, String title, int pos, boolean preview) {
        Lesson x = new Lesson();
        x.chapter = c;
        x.title = title;
        x.position = pos;
        x.preview = preview;
        x.published = true;
        x.content = "Bài học mẫu: đọc nội dung, luyện từ vựng và làm bài tập để củng cố kiến thức.";
        return db.add(x);
    }

    private Quiz quiz(Lesson lesson, String title, int minutes) {
        Quiz q = new Quiz();
        q.lesson = lesson;
        q.title = title;
        q.minutes = minutes;
        q.maxAttempts = 20;
        q.published = true;
        return db.add(q);
    }

    private void question(Quiz quiz, String text, String right, String explanation, String... options) {
        Question q = new Question();
        q.quiz = quiz;
        q.text = text;
        q.explanation = explanation;
        db.add(q);
        for (String textOption : options) {
            Choice c = new Choice();
            c.question = q;
            c.text = textOption;
            c.correct = textOption.equals(right);
            db.add(c);
        }
    }

    public void seed() {
        Teacher teacher = db.first(Teacher.class, "select t from Teacher t where t.name=:n", "n", "Giáo viên của bạn").orElseGet(() -> {
            Teacher t = new Teacher();
            t.name = "Giáo viên của bạn";
            t.roleText = "Hồ sơ giáo viên mẫu";
            t.bio = "Hồ sơ minh họa trong khu học tiếng Trung.";
            t.philosophy = "Học từng bài ngắn, thực hành qua ví dụ và ôn lại đều đặn.";
            t.published = true;
            return db.add(t);
        });
        LearningPath foundation = db.first(LearningPath.class, "select p from LearningPath p where p.slug='hsk'").orElseGet(() -> {
            LearningPath p = new LearningPath();
            p.slug = "hsk";
            p.title = "HSK · Nền tảng Hán ngữ";
            p.symbol = "汉";
            p.description = "Học theo cấp độ, từ vựng và luyện tập. Nội dung hiện là mẫu.";
            p.published = true;
            return db.add(p);
        });
        LearningPath advanced = db.first(LearningPath.class, "select p from LearningPath p where p.slug='hsk-nang-cao'").orElseGet(() -> {
            LearningPath p = new LearningPath();
            p.slug = "hsk-nang-cao";
            p.title = "HSK 7–9 · Học nâng cao";
            p.symbol = "高";
            p.category = "advanced";
            p.position = 2;
            p.description = "Không gian cho nội dung nâng cao do giáo viên chuẩn bị.";
            p.published = true;
            return db.add(p);
        });
        boolean fresh = db.count("select count(c) from Course c where c.slug='hsk-1-tu-hoc'") == 0;
        Course study = course("hsk-1-tu-hoc", "HSK 1 · Bắt đầu học tiếng Trung", "selfstudy", 299000, teacher.name);
        if (fresh) {
            Chapter ch = chapter(study, "Làm quen với tiếng Trung");
            Lesson first = lesson(ch, "Chào hỏi & làm quen", 1, true);
            Lesson second = lesson(ch, "Một ngày của bạn", 2, false);

            {
                Word w = new Word();
                w.lesson = first;
                w.hanzi = "你好";
                w.pinyin = "nǐ hǎo";
                w.meaning = "xin chào";
                w.sinoVietnamese = "nhĩ hảo";
                w.exampleZh = "你好，很高兴认识你。";
                w.exampleVi = "Xin chào, rất vui được làm quen với bạn.";
                w.position = 1;
                db.add(w);
            }
            {
                Word w = new Word();
                w.lesson = first;
                w.hanzi = "谢谢";
                w.pinyin = "xièxie";
                w.meaning = "cảm ơn";
                w.sinoVietnamese = "";
                w.exampleZh = "谢谢你的帮助。";
                w.exampleVi = "Cảm ơn sự giúp đỡ của bạn.";
                w.position = 2;
                db.add(w);
            }
            {
                Word w = new Word();
                w.lesson = first;
                w.hanzi = "老师";
                w.pinyin = "lǎoshī";
                w.meaning = "giáo viên";
                w.sinoVietnamese = "lão sư";
                w.exampleZh = "她是我的老师。";
                w.exampleVi = "Cô ấy là giáo viên của tôi.";
                w.position = 3;
                db.add(w);
            }
            {
                Word w = new Word();
                w.lesson = first;
                w.hanzi = "学生";
                w.pinyin = "xuésheng";
                w.meaning = "học sinh";
                w.sinoVietnamese = "học sinh";
                w.exampleZh = "我是学生。";
                w.exampleVi = "Tôi là học sinh.";
                w.position = 4;
                db.add(w);
            }
            {
                Word w = new Word();
                w.lesson = first;
                w.hanzi = "学习";
                w.pinyin = "xuéxí";
                w.meaning = "học tập";
                w.sinoVietnamese = "học tập";
                w.exampleZh = "我每天学习汉语。";
                w.exampleVi = "Tôi học tiếng Trung mỗi ngày.";
                w.position = 5;
                db.add(w);
            }
            {
                Word w = new Word();
                w.lesson = second;
                w.hanzi = "中国";
                w.pinyin = "Zhōngguó";
                w.meaning = "Trung Quốc";
                w.sinoVietnamese = "Trung Quốc";
                w.exampleZh = "我想去中国。";
                w.exampleVi = "Tôi muốn đến Trung Quốc.";
                w.position = 1;
                db.add(w);
            }
            {
                Word w = new Word();
                w.lesson = second;
                w.hanzi = "朋友";
                w.pinyin = "péngyou";
                w.meaning = "bạn bè";
                w.sinoVietnamese = "bằng hữu";
                w.exampleZh = "他是我的朋友。";
                w.exampleVi = "Anh ấy là bạn của tôi.";
                w.position = 2;
                db.add(w);
            }
            {
                Word w = new Word();
                w.lesson = second;
                w.hanzi = "今天";
                w.pinyin = "jīntiān";
                w.meaning = "hôm nay";
                w.sinoVietnamese = "kim thiên";
                w.exampleZh = "今天天气很好。";
                w.exampleVi = "Thời tiết hôm nay rất đẹp.";
                w.position = 3;
                db.add(w);
            }
            {
                Word w = new Word();
                w.lesson = second;
                w.hanzi = "喜欢";
                w.pinyin = "xǐhuan";
                w.meaning = "thích";
                w.sinoVietnamese = "";
                w.exampleZh = "我喜欢看书。";
                w.exampleVi = "Tôi thích đọc sách.";
                w.position = 4;
                db.add(w);
            }
            {
                Word w = new Word();
                w.lesson = second;
                w.hanzi = "再见";
                w.pinyin = "zàijiàn";
                w.meaning = "tạm biệt";
                w.sinoVietnamese = "tái kiến";
                w.exampleZh = "老师，再见！";
                w.exampleVi = "Tạm biệt cô giáo!";
                w.position = 5;
                db.add(w);
            }
            Quiz q = quiz(first, "Luyện chào hỏi", 10);
            question(q, "你好 có nghĩa là gì?", "Xin chào", "你好 (nǐ hǎo) là cách chào thông dụng.", "Xin chào", "Tạm biệt", "Xin lỗi");
            question(q, "Từ nào có nghĩa là giáo viên?", "老师", "老师 (lǎoshī) có nghĩa là giáo viên.", "老师", "学生", "朋友");
        }
        for (int n = 1; n <= 9; n++) {
            final int number = n;
            LearningPath path = n <= 6 ? foundation : advanced;
            if (db.count("select count(l) from Level l where l.path.id=:p and l.number=:n", "p", path.id, "n", n) == 0) {
                Level l = new Level();
                l.path = path;
                l.number = n;
                l.title = "HSK " + n;
                l.description = n == 1 ? "Bài học và từ vựng minh họa." : "Giáo viên đang chuẩn bị nội dung.";
                l.course = n == 1 ? study : null;
                l.published = n == 1;
                db.add(l);
            }
        }
        for (int n = 1; n <= 3; n++) {
            Course c = course("lop-hsk-" + n + "-mau", "Lớp HSK " + n + " · Nội dung mẫu", "course", 199000 + n * 100000, teacher.name);
            if (db.count("select count(l) from Lesson l where l.chapter.course.id=:c", "c", c.id) == 0) {
                Chapter ch = chapter(c, "Nội dung khóa học");
                lesson(ch, "Giới thiệu chương trình", 1, true);
                lesson(ch, "Tài liệu dành cho học viên", 2, false);
            }
            if (db.count("select count(s) from StudyClass s where s.course.id=:c", "c", c.id) == 0) {
                StudyClass s = new StudyClass();
                s.course = c;
                s.teacher = teacher;
                s.title = "Lớp HSK " + n;
                s.schedule = "Lịch minh họa — liên hệ để thống nhất";
                s.features = "Thông tin lớp học mẫu\nTài liệu và bài tập theo bài học";
                s.published = true;
                db.add(s);
            }
        }
        boolean gameFresh = db.count("select count(c) from Course c where c.slug='van-hoa-trung-quoc-mau'") == 0;
        Course games = course("van-hoa-trung-quoc-mau", "Khám phá văn hóa Trung Quốc", "game", 1000, teacher.name);
        if (gameFresh) {
            Chapter ch = chapter(games, "Câu hỏi theo chủ đề");

            {
                Lesson l = lesson(ch, "Chữ viết & ngôn ngữ", 1, true);
                Quiz q = quiz(l, "Chữ viết & ngôn ngữ", 5);
                question(q, "Pinyin thường dùng hệ chữ nào để ghi âm tiếng Trung?", "Chữ cái Latin", "Pinyin dùng chữ cái Latin kèm dấu thanh để biểu diễn phát âm.", "Chữ cái Latin", "Chữ cái Hy Lạp", "Chữ số La Mã");
                question(q, "Chữ 人 có nghĩa cơ bản là gì?", "Người", "人 (rén) có nghĩa là người.", "Người", "Núi", "Nước");
                GameTopic t = new GameTopic();
                t.title = "Chữ viết & ngôn ngữ";
                t.symbol = "字";
                t.description = "Khám phá chữ Hán và cách ghi âm.";
                t.quiz = q;
                t.position = 1;
                t.published = true;
                db.add(t);
            }
            {
                Lesson l = lesson(ch, "Lễ hội truyền thống", 2, true);
                Quiz q = quiz(l, "Lễ hội truyền thống", 5);
                question(q, "Bánh trung thu gắn với dịp lễ nào?", "Tết Trung thu", "Bánh trung thu là món bánh truyền thống trong dịp Trung thu.", "Tết Trung thu", "Tết Thanh minh", "Lễ Lao động");
                question(q, "Tết Đoan Ngọ thường gắn với hoạt động nào?", "Đua thuyền rồng", "Đua thuyền rồng là hoạt động truyền thống phổ biến dịp Đoan Ngọ.", "Đua thuyền rồng", "Trượt tuyết", "Săn trứng Phục sinh");
                GameTopic t = new GameTopic();
                t.title = "Lễ hội truyền thống";
                t.symbol = "节";
                t.description = "Những phong tục quen thuộc qua các dịp lễ.";
                t.quiz = q;
                t.position = 2;
                t.published = true;
                db.add(t);
            }
            {
                Lesson l = lesson(ch, "Địa lý & danh thắng", 3, true);
                Quiz q = quiz(l, "Địa lý & danh thắng", 5);
                question(q, "Tử Cấm Thành nằm ở thành phố nào?", "Bắc Kinh", "Tử Cấm Thành tọa lạc tại Bắc Kinh.", "Bắc Kinh", "Thượng Hải", "Thành Đô");
                question(q, "Sông Trường Giang còn được biết với tên nào?", "Dương Tử", "Trường Giang thường được gọi là sông Dương Tử trong tiếng Việt.", "Dương Tử", "Mekong", "Sông Hằng");
                GameTopic t = new GameTopic();
                t.title = "Địa lý & danh thắng";
                t.symbol = "山";
                t.description = "Làm quen những địa danh nổi tiếng.";
                t.quiz = q;
                t.position = 3;
                t.published = true;
                db.add(t);
            }
            {
                Lesson l = lesson(ch, "Văn học", 4, true);
                Quiz q = quiz(l, "Văn học", 5);
                question(q, "Tôn Ngộ Không là nhân vật của tác phẩm nào?", "Tây du ký", "Tôn Ngộ Không là một nhân vật trung tâm của Tây du ký.", "Tây du ký", "Hồng lâu mộng", "Thủy hử");
                question(q, "Lý Bạch được biết đến nhiều nhất ở lĩnh vực nào?", "Thơ ca", "Lý Bạch là nhà thơ nổi tiếng thời Đường.", "Thơ ca", "Thiên văn", "Điện ảnh");
                GameTopic t = new GameTopic();
                t.title = "Văn học";
                t.symbol = "书";
                t.description = "Tác phẩm và nhân vật văn học quen thuộc.";
                t.quiz = q;
                t.position = 4;
                t.published = true;
                db.add(t);
            }
            {
                Lesson l = lesson(ch, "Nghệ thuật", 5, true);
                Quiz q = quiz(l, "Nghệ thuật", 5);
                question(q, "Nghệ thuật viết chữ bằng bút lông được gọi là gì?", "Thư pháp", "Thư pháp coi trọng nét bút, bố cục và tính biểu cảm của chữ viết.", "Thư pháp", "Kiến trúc", "Gốm sứ");
                question(q, "Kinh kịch thuộc loại hình nghệ thuật nào?", "Sân khấu truyền thống", "Kinh kịch kết hợp ca hát, thoại, động tác và biểu diễn sân khấu.", "Sân khấu truyền thống", "Điêu khắc", "Nhiếp ảnh");
                GameTopic t = new GameTopic();
                t.title = "Nghệ thuật";
                t.symbol = "艺";
                t.description = "Nét đẹp từ thư pháp đến sân khấu.";
                t.quiz = q;
                t.position = 5;
                t.published = true;
                db.add(t);
            }
            {
                Lesson l = lesson(ch, "Khoa học & phát minh", 6, true);
                Quiz q = quiz(l, "Khoa học & phát minh", 5);
                question(q, "Dụng cụ nào thường dùng để xác định phương hướng?", "La bàn", "La bàn sử dụng kim nam châm để hỗ trợ xác định phương hướng.", "La bàn", "Cân đĩa", "Nhiệt kế");
                question(q, "Kỹ thuật in giúp ích trực tiếp nhất cho việc gì?", "Nhân bản văn bản", "Kỹ thuật in giúp sao chép và phổ biến văn bản.", "Nhân bản văn bản", "Đo nhiệt độ", "Đo độ sâu biển");
                GameTopic t = new GameTopic();
                t.title = "Khoa học & phát minh";
                t.symbol = "知";
                t.description = "Những kiến thức lịch sử khoa học cơ bản.";
                t.quiz = q;
                t.position = 6;
                t.published = true;
                db.add(t);
            }
            {
                Lesson l = lesson(ch, "Giao tiếp hằng ngày", 7, true);
                Quiz q = quiz(l, "Giao tiếp hằng ngày", 5);
                question(q, "Khi nhận được sự giúp đỡ, bạn có thể nói gì?", "谢谢", "谢谢 (xièxie) là lời cảm ơn.", "谢谢", "再见", "晚安");
                question(q, "Cách chào tạm biệt thông dụng là gì?", "再见", "再见 (zàijiàn) là lời chào tạm biệt.", "再见", "谢谢", "对不起");
                GameTopic t = new GameTopic();
                t.title = "Giao tiếp hằng ngày";
                t.symbol = "语";
                t.description = "Thực hành những cách nói lịch sự.";
                t.quiz = q;
                t.position = 7;
                t.published = true;
                db.add(t);
            }
        }
        for (String[] row : List.of(new String[]{"CSCA", "入", "Không gian dự kiến dành cho nội dung ôn tập tuyển sinh."}, new String[]{"CTCSOL", "教", "Chương trình dự kiến cho định hướng giảng dạy tiếng Trung."}, new String[]{"Góc đọc & tài liệu", "书", "Bài viết và chia sẻ do giáo viên chuẩn bị."}))
            if (db.count("select count(p) from PlannedProduct p where p.title=:t", "t", row[0]) == 0) {
                PlannedProduct p = new PlannedProduct();
                p.title = row[0];
                p.symbol = row[1];
                p.description = row[2];
                db.add(p);
            }
    }
}
