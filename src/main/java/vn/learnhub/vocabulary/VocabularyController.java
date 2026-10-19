package vn.learnhub.vocabulary;

import vn.learnhub.course.LearningContext;
import vn.learnhub.auth.Access;
import vn.learnhub.course.Lesson;
import vn.learnhub.shared.json.JsonTools;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.user.Account;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.http.*;
import java.security.Principal;
import java.time.*;
import java.util.*;

@Controller
public class VocabularyController {
    private final LearningContext context;
    private final Store db;
    private final Access access;
    private final VocabularyService study;
    private final JsonTools json;

    public VocabularyController(Store db, Access access, VocabularyService study, JsonTools json, LearningContext context) {
        this.context = context;
        this.db = db;
        this.access = access;
        this.study = study;
        this.json = json;
    }

    @GetMapping("/bai-hoc/{id}/tu-vung")
    String words(@PathVariable Long id, Principal p, Model m) {
        Account user = access.current(p);
        Lesson l = db.get(Lesson.class, id);
        access.requireLesson(user, l);
        context.select(l.chapter.course.language, m);
        m.addAttribute("lesson", l);
        m.addAttribute("words", db.list(Word.class, "select w from Word w where w.lesson.id=:id order by w.position,w.id", "id", id));
        m.addAttribute("saved", user == null ? Set.of() : new HashSet<>(db.list(Long.class, "select p.word.id from WordProgress p where p.user.id=:u and p.saved=true", "u", user.id)));
        return "vocabulary/words";
    }

    @PostMapping("/tu-vung/{id}/luu")
    String bookmark(@PathVariable Long id, Principal p, @RequestParam String saved, @RequestParam(defaultValue = "") String origin) {
        if (!Set.of("0", "1").contains(saved)) throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        Account user = access.required(p);
        study.bookmark(user, id, saved.equals("1"));
        return origin.equals("saved") || saved.equals("0") ? "redirect:/tu-da-luu" : "redirect:/bai-hoc/" + db.get(Word.class, id).lesson.id + "/tu-vung";
    }

    @GetMapping("/tu-da-luu")
    String saved(Principal p, @RequestParam(defaultValue = "") String q, Model m) {
        Account user = access.required(p);
        String query = q.length() > 100 ? q.substring(0, 100) : q;
        var savedWords = db.list(Word.class, "select p.word from WordProgress p where p.user.id=:u and p.saved=true and p.word.lesson.chapter.course.language=:language order by p.word.id", "u", user.id, "language", context.code());
        var words = savedWords.stream().filter(w -> access.lesson(user, w.lesson)).filter(w -> query.isBlank() || (w.hanzi + " " + w.pinyin + " " + w.meaning).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).toList();
        m.addAttribute("unavailableWordIds", savedWords.stream().filter(w -> !access.lesson(user, w.lesson)).map(w -> w.id).toList());
        m.addAttribute("words", words);
        m.addAttribute("query", query);
        return "vocabulary/saved";
    }

    @GetMapping("/flashcards")
    String flashcards(Principal p, @RequestParam(required = false) Long lesson, @RequestParam(defaultValue = "saved") String mode, @RequestParam(required = false) Long after, Model m) {
        Account user = access.required(p);
        if (lesson != null) {
            Lesson selected = db.get(Lesson.class, lesson);
            access.requireLesson(user, selected);
            context.select(selected.chapter.course.language, m);
        }
        List<Map<String, Object>> deck = new ArrayList<>();
        var page = study.deckPage(user, lesson, mode, context.code(), after);
        for (Word w : page.words())
            deck.add(Map.of("id", w.id, "term", w.getHanzi(), "pronunciation", w.getPinyin(), "meaning", w.meaning, "example", w.getExampleZh(), "translation", w.exampleVi, "language", w.speechLanguage(), "audio", w.audioUrl, "review_url", "/tu-vung/" + w.id + "/on-tap"));
        m.addAttribute("deckJson", json.html(deck));
        m.addAttribute("hasCards", !deck.isEmpty());
        m.addAttribute("after", after);
        m.addAttribute("nextAfter", page.nextAfter());
        m.addAttribute("mode", lesson != null ? "lesson" : mode);
        m.addAttribute("lesson", lesson == null ? null : db.get(Lesson.class, lesson));
        return "vocabulary/flashcards";
    }

    @PostMapping("/tu-vung/{id}/on-tap")
    @ResponseBody
    Map<String, Object> review(@PathVariable Long id, Principal p, @RequestParam String remembered) {
        if (!Set.of("0", "1").contains(remembered)) throw new IllegalArgumentException("Lựa chọn không hợp lệ.");
        WordProgress progress = study.review(access.required(p), id, remembered.equals("1"));
        return Map.of("ok", true, "stage", progress.stage, "next_review", progress.nextReview.toString());
    }
}
