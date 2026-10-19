package vn.learnhub.learning;

import vn.learnhub.course.LearningContext;
import vn.learnhub.auth.Access;
import vn.learnhub.course.Lesson;
import vn.learnhub.quiz.Attempt;
import vn.learnhub.quiz.Quiz;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.storage.Resource;
import vn.learnhub.user.Account;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.http.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.time.*;
import java.util.*;

@Controller
public class LessonController {
    private final LearningContext context;
    private final Store db;
    private final Access access;
    private final LearningProgressService progress;

    public LessonController(Store db, Access access, LearningProgressService progress, LearningContext context) {
        this.context = context;
        this.db = db;
        this.access = access;
        this.progress = progress;
    }

    @GetMapping("/lessons/{id}")
    String lesson(@PathVariable Long id, Principal p, Model m, jakarta.servlet.http.HttpServletResponse response) {
        Lesson l = db.get(Lesson.class, id);
        Account user = access.current(p);
        access.requireLesson(user, l);
        response.setHeader("Cache-Control", "no-store");
        context.select(l.chapter.course.language, m);
        m.addAttribute("lesson", l);
        var materials = db.list(Resource.class, "select r from Resource r where r.lesson.id=:id and r.reviewed=true order by r.id", "id", id);
        m.addAttribute("videos", materials.stream().filter(Resource::isVideo).toList());
        m.addAttribute("resources", materials.stream().filter(r -> !r.isVideo()).toList());
        m.addAttribute("quiz", db.first(Quiz.class, "select q from Quiz q where q.lesson.id=:id and q.published=true", "id", id).orElse(null));
        m.addAttribute("completed", user != null && db.count("select count(p) from Progress p where p.user.id=:u and p.lesson.id=:l", "u", user.id, "l", id) > 0);
        m.addAttribute("attempts", user == null ? List.of() : db.list(Attempt.class, "select a from Attempt a where a.user.id=:u and a.quiz.lesson.id=:l order by a.startedAt desc", "u", user.id, "l", id));
        return "learning/lesson";
    }

    @PostMapping("/lessons/{id}/complete")
    String complete(@PathVariable Long id, Principal p, RedirectAttributes f) {
        progress.complete(access.required(p), id);
        f.addFlashAttribute("notice", "Đã lưu tiến độ bài học.");
        return "redirect:/lessons/" + id;
    }
}
