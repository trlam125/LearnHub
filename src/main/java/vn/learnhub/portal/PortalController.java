package vn.learnhub.portal;

import vn.learnhub.course.LearningContext;
import vn.learnhub.community.StudyStatsService;
import vn.learnhub.course.LearningPath;
import vn.learnhub.course.Lesson;
import vn.learnhub.course.Teacher;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.user.Account;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import java.util.*;

@Controller
public class PortalController {
    private final LearningContext context;
    private final Store db;
    private final StudyStatsService study;

    public PortalController(Store db, StudyStatsService study, LearningContext context) {
        this.context = context;
        this.db = db;
        this.study = study;
    }

    @GetMapping("/")
    String home(Model m) {
        m.addAttribute("teacher", db.first(Teacher.class, "select t from Teacher t where t.published=true and t.language in :languages order by t.position,t.id", "languages", List.of(context.code(), "both")).orElse(null));
        m.addAttribute("paths", db.list(LearningPath.class, "select p from LearningPath p where p.published=true and p.language=:language order by p.position,p.id", "language", context.code()));
        m.addAttribute("suggested", db.query(Lesson.class, "select l from Lesson l where l.published=true and l.preview=true and l.chapter.course.published=true and l.chapter.course.kind='selfstudy' and l.chapter.course.language=:language order by l.position,l.id", "language", context.code()).setMaxResults(3).getResultList());
        m.addAttribute("ranking", study.ranking("all", 3));
        return "portal/home";
    }

    @GetMapping({"/dang-phat-trien", "/dang-phat-trien/"})
    String roadmap(Model m) {
        m.addAttribute("products", db.list(PlannedProduct.class, "select p from PlannedProduct p where p.published=true and p.language=:language order by p.position,p.id", "language", context.code()));
        return "portal/roadmap";
    }

    @GetMapping("/tieng-anh")
    String english() {
        return "redirect:/?lang=en";
    }

    @GetMapping("/tieng-trung")
    String chinese() {
        return "redirect:/?lang=zh";
    }

    @GetMapping("/policies")
    String policies() {
        return "portal/policies";
    }

    @GetMapping("/health")
    @ResponseBody
    Map<String, String> health() {
        db.count("select count(u) from Account u");
        return Map.of("status", "ok");
    }
}
