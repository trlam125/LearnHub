package vn.learnhub.quiz;

import vn.learnhub.course.LearningContext;
import vn.learnhub.auth.Access;
import vn.learnhub.shared.json.JsonTools;
import vn.learnhub.user.Account;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import com.fasterxml.jackson.core.type.TypeReference;
import java.security.Principal;
import java.time.*;
import java.util.*;

@Controller
public class QuizController {
    private final LearningContext context;
    private final Access access;
    private final QuizService quizzes;
    private final JsonTools json;

    public QuizController(Access access, QuizService quizzes, JsonTools json, LearningContext context) {
        this.context = context;
        this.access = access;
        this.quizzes = quizzes;
        this.json = json;
    }

    @PostMapping("/quizzes/{id}/start")
    String start(@PathVariable Long id, Principal p) {
        return "redirect:/attempts/" + quizzes.start(access.required(p), id).id;
    }

    @GetMapping("/attempts/{id}")
    String attempt(@PathVariable UUID id, Principal p, Model m, jakarta.servlet.http.HttpServletResponse res) {
        Account user = access.required(p);
        Attempt a = quizzes.owned(user, id);
        if (a.submittedAt == null) {
            access.requireLesson(user, a.quiz.lesson);
            if (!a.deadline.isAfter(Instant.now())) a = quizzes.finishExpired(id);
        }
        context.select(a.quiz.lesson.chapter.course.language, m);
        m.addAttribute("attempt", a);
        m.addAttribute("remainingMillis", Math.max(0, Duration.between(Instant.now(), a.deadline).toMillis()));
        m.addAttribute("questions", QuizEngine.publicView(json.questions(a.snapshotJson), json.answers(a.answersJson)));
        m.addAttribute("result", json.read(a.resultJson, new TypeReference<List<QuizEngine.Detail>>() {
        }));
        res.setHeader("Cache-Control", "private, no-store");
        return "quiz/attempt";
    }

    @PostMapping("/attempts/{id}")
    String submit(@PathVariable UUID id, Principal p, @RequestParam MultiValueMap<String, String> form) {
        Account user = access.required(p);
        Attempt a = quizzes.owned(user, id);
        Map<String, List<Long>> answers = new LinkedHashMap<>();
        for (var q : json.questions(a.snapshotJson))
            answers.put(Long.toString(q.id()), form.getOrDefault("q" + q.id(), List.of()).stream().map(Long::valueOf).toList());
        quizzes.save(user, id, answers, true);
        return "redirect:/attempts/" + id;
    }

    @PostMapping("/attempts/{id}/save")
    @ResponseBody
    Map<String, Object> save(@PathVariable UUID id, Principal p, @RequestBody Map<String, List<Long>> answers) {
        Attempt a = quizzes.save(access.required(p), id, answers, false);
        return Map.of("submitted", a.submittedAt != null, "saved_at", Instant.now().toString());
    }
}
