package vn.learnhub.course;

import vn.learnhub.shared.persistence.Store;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import java.util.*;

@Controller
public class TeacherController {
    private final LearningContext context;
    private final Store db;

    public TeacherController(Store db, LearningContext context) {
        this.context = context;
        this.db = db;
    }

    @GetMapping({"/giao-vien", "/giao-vien/"})
    String teachers(Model m) {
        m.addAttribute("teachers", db.list(Teacher.class, "select t from Teacher t where t.published=true and t.language in :languages order by t.position,t.id", "languages", List.of(context.code(), "both")));
        return "course/teachers";
    }
}
