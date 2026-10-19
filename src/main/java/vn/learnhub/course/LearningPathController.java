package vn.learnhub.course;

import vn.learnhub.auth.Access;
import vn.learnhub.learning.Progress;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.user.Account;
import vn.learnhub.vocabulary.Word;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import java.security.Principal;
import java.util.*;

@Controller
public class LearningPathController {
    private final LearningContext context;
    private final Store db;
    private final Access access;

    public LearningPathController(Store db, Access access, LearningContext context) {
        this.context = context;
        this.db = db;
        this.access = access;
    }

    public record LevelCard(Level level, long lessons, long words) {
    }

    public record PathCard(LearningPath path, List<LevelCard> levels) {
    }

    public record LessonCard(Lesson lesson, long words, boolean access, boolean completed) {
    }

    @GetMapping({"/hsk", "/hsk/", "/lo-trinh", "/lo-trinh/"})
    String paths(@RequestParam(defaultValue = "") String category, Model m) {
        List<PathCard> rows = new ArrayList<>();
        for (var path : db.list(LearningPath.class, "select p from LearningPath p where p.published=true and p.language=:language order by p.position,p.id", "language", context.code())) {
            if (!category.isBlank() && !category.equals(path.category)) continue;
            List<LevelCard> levels = new ArrayList<>();
            for (var l : db.list(Level.class, "select l from Level l where l.path.id=:p order by l.number", "p", path.id))
                levels.add(new LevelCard(l, l.course == null ? 0 : db.count("select count(x) from Lesson x where x.chapter.course.id=:c and x.published=true", "c", l.course.id), l.course == null ? 0 : db.count("select count(w) from Word w where w.lesson.chapter.course.id=:c and w.lesson.published=true", "c", l.course.id)));
            rows.add(new PathCard(path, levels));
        }
        m.addAttribute("rows", rows);
        m.addAttribute("category", category);
        return "course/paths";
    }

    @GetMapping({"/hsk/levels/{id}", "/lo-trinh/levels/{id}"})
    String level(@PathVariable Long id, Principal principal, Model m) {
        Level level = db.get(Level.class, id);
        if (!level.published || !level.path.published || level.course == null || !level.course.published)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        context.select(level.path.language, m);
        Account user = access.current(principal);
        List<LessonCard> rows = new ArrayList<>();
        for (var l : db.list(Lesson.class, "select l from Lesson l where l.chapter.course.id=:c and l.published=true order by l.chapter.position,l.position,l.id", "c", level.course.id))
            rows.add(new LessonCard(l, db.count("select count(w) from Word w where w.lesson.id=:l", "l", l.id), access.lesson(user, l), user != null && db.count("select count(p) from Progress p where p.user.id=:u and p.lesson.id=:l", "u", user.id, "l", l.id) > 0));
        m.addAttribute("level", level);
        m.addAttribute("rows", rows);
        return "course/level";
    }
}
