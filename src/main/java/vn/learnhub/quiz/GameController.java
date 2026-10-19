package vn.learnhub.quiz;

import vn.learnhub.course.LearningContext;
import vn.learnhub.shared.persistence.Store;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import java.util.*;

@Controller
public class GameController {
    private final LearningContext context;
    private final Store db;

    public GameController(Store db, LearningContext context) {
        this.context = context;
        this.db = db;
    }

    @GetMapping({"/tro-choi", "/tro-choi/"})
    String games(Model m) {
        m.addAttribute("topics", db.list(GameTopic.class, "select t from GameTopic t where t.published=true and t.quiz.published=true and t.quiz.lesson.preview=true and t.quiz.lesson.published=true and t.quiz.lesson.chapter.course.published=true and t.quiz.lesson.chapter.course.language=:language order by t.position,t.id", "language", context.code()));
        return "quiz/games";
    }
}
