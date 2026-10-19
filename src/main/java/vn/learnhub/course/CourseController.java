package vn.learnhub.course;

import vn.learnhub.auth.Access;
import vn.learnhub.shared.persistence.Store;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.http.*;
import java.security.Principal;
import java.time.*;
import java.util.*;

@Controller
public class CourseController {
    private final LearningContext context;
    private final Store db;
    private final CourseRepository courses;
    private final Access access;

    public CourseController(Store db, CourseRepository courses, Access access, LearningContext context) {
        this.context = context;
        this.db = db;
        this.courses = courses;
        this.access = access;
    }

    public record ChapterRow(Chapter chapter, List<Lesson> lessons) {
    }

    @GetMapping({"/courses", "/courses/"})
    String catalog(@RequestParam(defaultValue = "") String q, Model m) {
        if (q.length() > 100) q = q.substring(0, 100);
        m.addAttribute("query", q);
        m.addAttribute("courses", q.isBlank() ? courses.findByPublishedTrueAndKindAndLanguageOrderByIdDesc("course", context.code()) : db.list(Course.class, "select c from Course c where c.published=true and c.kind='course' and c.language=:language and (lower(c.title) like :q or lower(c.subject) like :q) order by c.id desc", "language", context.code(), "q", "%" + q.toLowerCase(Locale.ROOT) + "%"));
        return "course/catalog";
    }

    @GetMapping("/courses/{slug}")
    String course(@PathVariable String slug, Principal principal, Model m) {
        Course c = courses.findBySlugAndPublishedTrue(slug).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND));
        if (c.kind.equals("game")) return "redirect:/tro-choi";
        context.select(c.language, m);
        m.addAttribute("course", c);
        m.addAttribute("access", access.course(access.current(principal), c));
        List<ChapterRow> rows = new ArrayList<>();
        for (var ch : db.list(Chapter.class, "select c from Chapter c where c.course.id=:id order by c.position,c.id", "id", c.id))
            rows.add(new ChapterRow(ch, db.list(Lesson.class, "select l from Lesson l where l.chapter.id=:id and l.published=true order by l.position,l.id", "id", ch.id)));
        m.addAttribute("chapters", rows);
        return "course/detail";
    }
}
