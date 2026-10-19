package vn.learnhub.bootstrap;

import vn.learnhub.course.*;
import vn.learnhub.portal.PlannedProduct;
import vn.learnhub.quiz.*;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.vocabulary.Word;
import org.springframework.stereotype.Component;

@Component
public class EnglishDemoData {
    private final Store db;

    public EnglishDemoData(Store db) {
        this.db = db;
    }

    private Course course(String slug, String title, String kind, Teacher teacher) {
        return db.first(Course.class, "select c from Course c where c.slug=:slug", "slug", slug).orElseGet(() -> {
            Course course = new Course();
            course.slug = slug;
            course.title = title;
            course.kind = kind;
            course.language = "en";
            course.subject = "Tiếng Anh";
            course.teacherName = teacher.name;
            course.summary = "Bài học mẫu giúp bạn làm quen với từ vựng, thẻ ôn tập và bài luyện tiếng Anh.";
            course.description = "Khóa học mẫu gồm các bài luyện tiếng Anh cơ bản. Chương trình CEFR và IELTS đầy đủ được giáo viên công bố riêng.";
            course.published = true;
            return db.add(course);
        });
    }

    private Lesson lesson(Course course, String title, boolean preview, int position) {
        Chapter chapter = new Chapter();
        chapter.course = course;
        chapter.title = "Getting started · Khởi đầu";
        chapter.position = position;
        db.add(chapter);
        Lesson lesson = new Lesson();
        lesson.chapter = chapter;
        lesson.title = title;
        lesson.preview = preview;
        lesson.published = true;
        lesson.content = "Bài học mẫu: đọc từ và ví dụ, nghe phát âm rồi luyện tập.";
        return db.add(lesson);
    }

    private void word(Lesson lesson, String term, String pronunciation, String meaning, String example, String translation, int position) {
        Word word = new Word();
        word.lesson = lesson;
        word.hanzi = term;
        word.pinyin = pronunciation;
        word.meaning = meaning;
        word.exampleZh = example;
        word.exampleVi = translation;
        word.position = position;
        db.add(word);
    }

    private Quiz quiz(Lesson lesson, String title) {
        Quiz quiz = new Quiz();
        quiz.lesson = lesson;
        quiz.title = title;
        quiz.minutes = 10;
        quiz.maxAttempts = 20;
        quiz.published = true;
        return db.add(quiz);
    }

    private void question(Quiz quiz, String text, String explanation, String... options) {
        Question question = new Question();
        question.quiz = quiz;
        question.text = text;
        question.explanation = explanation;
        db.add(question);
        for (int i = 0; i < options.length; i++) {
            Choice choice = new Choice();
            choice.question = question;
            choice.text = options[i];
            choice.correct = i == 0;
            db.add(choice);
        }
    }

    public void seed() {
        Teacher teacher = db.first(Teacher.class, "select t from Teacher t where t.name=:name and t.language='en'", "name", "Giáo viên tiếng Anh · Hồ sơ mẫu").orElseGet(() -> {
            Teacher value = new Teacher();
            value.name = "Giáo viên tiếng Anh · Hồ sơ mẫu";
            value.language = "en";
            value.roleText = "Thông tin minh họa — cập nhật hồ sơ thật trước khi tuyển sinh";
            value.bio = "Hồ sơ minh họa trong khu học tiếng Anh.";
            value.philosophy = "Học theo tình huống, thực hành từ vựng và ôn tập đều đặn.";
            value.published = true;
            return db.add(value);
        });
        Course foundation = course("english-a1-mau", "Tiếng Anh A1 · Bài học mẫu", "selfstudy", teacher);
        if (db.count("select count(l) from Lesson l where l.chapter.course.id=:course", "course", foundation.id) == 0) {
            Lesson first = lesson(foundation, "Greetings · Chào hỏi", true, 1);
            first.content = "Hello! My name is Linh. Nice to meet you.\nXin chào! Mình tên là Linh. Rất vui được gặp bạn.\n\nDùng hello để chào, please khi nhờ giúp đỡ và thank you để cảm ơn.";
            word(first, "hello", "/həˈloʊ/", "xin chào", "Hello, my name is Linh.", "Xin chào, mình tên là Linh.", 1);
            word(first, "goodbye", "/ˌɡʊdˈbaɪ/", "tạm biệt", "Goodbye! See you tomorrow.", "Tạm biệt! Hẹn gặp bạn ngày mai.", 2);
            word(first, "please", "/pliːz/", "làm ơn; xin vui lòng", "Please open your book.", "Vui lòng mở sách của bạn.", 3);
            word(first, "thank you", "/ˈθæŋk juː/", "cảm ơn", "Thank you for your help.", "Cảm ơn bạn đã giúp đỡ.", 4);
            word(first, "friend", "/frend/", "người bạn", "This is my friend.", "Đây là bạn của mình.", 5);
            word(first, "book", "/bʊk/", "quyển sách", "I have a book.", "Mình có một quyển sách.", 6);
            Quiz practice = quiz(first, "Greetings · Luyện tập chào hỏi");
            question(practice, "Từ nào dùng để nói cảm ơn?", "Thank you dùng để bày tỏ lời cảm ơn.", "Thank you", "Goodbye", "Hello");
            question(practice, "Chọn câu đúng: She ___ a student.", "Chủ ngữ she đi với is ở thì hiện tại đơn của động từ be.", "is", "am", "are");
            Lesson second = lesson(foundation, "People · Giới thiệu mọi người", false, 2);
            word(second, "teacher", "/ˈtiːtʃər/", "giáo viên", "She is a teacher.", "Cô ấy là giáo viên.", 1);
            word(second, "student", "/ˈstuːdənt/", "học sinh; sinh viên", "I am a student.", "Mình là sinh viên.", 2);
        }
        LearningPath path = db.first(LearningPath.class, "select p from LearningPath p where p.slug='english-cefr'").orElseGet(() -> {
            LearningPath value = new LearningPath();
            value.slug = "english-cefr";
            value.title = "Tiếng Anh · Lộ trình A1–C2";
            value.language = "en";
            value.symbol = "EN";
            value.category = "cefr";
            value.description = "Học theo cấp độ. Hiện mới có bài mẫu A1; các cấp tiếp theo đang được chuẩn bị.";
            value.published = true;
            return db.add(value);
        });
        String[] levels = {"A1", "A2", "B1", "B2", "C1", "C2"};
        for (int i = 0; i < levels.length; i++) {
            int number = i + 1;
            if (db.count("select count(l) from Level l where l.path.id=:path and l.number=:number", "path", path.id, "number", number) == 0) {
                Level level = new Level();
                level.path = path;
                level.number = number;
                level.title = levels[i];
                level.description = i == 0 ? "Bài học khởi đầu minh họa." : "Giáo viên đang chuẩn bị nội dung.";
                level.course = i == 0 ? foundation : null;
                level.published = i == 0;
                db.add(level);
            }
        }
        Course paid = course("english-giao-tiep-mau", "Tiếng Anh giao tiếp · Khóa mẫu", "course", teacher);
        if (db.count("select count(s) from StudyClass s where s.course.id=:id", "id", paid.id) == 0) {
            StudyClass offering = new StudyClass();
            offering.course = paid;
            offering.teacher = teacher;
            offering.title = "Lớp tiếng Anh giao tiếp · Mẫu";
            offering.schedule = "Liên hệ giáo viên để thống nhất lịch học";
            offering.features = "Thông tin lớp học mẫu";
            offering.published = true;
            db.add(offering);
        }
        Course games = course("english-games-mau", "English challenges · Thử thách mẫu", "game", teacher);
        if (db.count("select count(l) from Lesson l where l.chapter.course.id=:id", "id", games.id) == 0) {
            Quiz game = quiz(lesson(games, "Everyday English · Tiếng Anh hằng ngày", true, 1), "Everyday English");
            question(game, "Chọn lời chào phù hợp vào buổi sáng.", "Good morning là lời chào vào buổi sáng.", "Good morning", "Good night", "Goodbye");
            question(game, "Từ nào có nghĩa là quyển sách?", "Book có nghĩa là quyển sách.", "Book", "Chair", "Window");
            GameTopic topic = new GameTopic();
            topic.title = "Everyday English";
            topic.description = "Thử sức với từ vựng và tình huống giao tiếp quen thuộc.";
            topic.symbol = "EN";
            topic.quiz = game;
            topic.published = true;
            db.add(topic);
        }
        if (db.count("select count(p) from PlannedProduct p where p.language='en' and p.title=:title", "title", "Góc luyện IELTS / TOEIC") == 0) {
            PlannedProduct product = new PlannedProduct();
            product.title = "Góc luyện IELTS / TOEIC";
            product.language = "en";
            product.symbol = "EN";
            product.description = "Nội dung dự kiến, giáo viên cần biên soạn trước khi xuất bản bài học.";
            product.published = true;
            db.add(product);
        }
    }
}
