package vn.learnhub.vocabulary;

import vn.learnhub.auth.Access;
import vn.learnhub.community.ActivityService;
import vn.learnhub.course.Lesson;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.shared.study.StudyRules;
import vn.learnhub.user.Account;
import vn.learnhub.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class VocabularyService {
    private final Store db;
    private final UserRepository users;
    private final Access access;
    private final ActivityService activities;

    public VocabularyService(Store db, UserRepository users, Access access, ActivityService activities) {
        this.db = db;
        this.users = users;
        this.access = access;
        this.activities = activities;
    }

    @Transactional
    public void bookmark(Account user, Long wordId, boolean saved) {
        users.lockById(user.id).orElseThrow();
        if (!saved) {
            db.first(WordProgress.class, "select p from WordProgress p where p.user.id=:u and p.word.id=:w", "u", user.id, "w", wordId)
                    .ifPresent(p -> p.saved = false);
            return;
        }
        Word word = db.get(Word.class, wordId);
        access.requireLesson(user, word.lesson);
        WordProgress p = progress(user, word);
        p.saved = saved;
    }

    private WordProgress progress(Account user, Word word) {
        return db.first(WordProgress.class, "select p from WordProgress p where p.user.id=:u and p.word.id=:w", "u", user.id, "w", word.id).orElseGet(() -> {
            WordProgress p = new WordProgress();
            p.user = user;
            p.word = word;
            return db.add(p);
        });
    }

    @Transactional
    public WordProgress review(Account user, Long wordId, boolean remembered) {
        users.lockById(user.id).orElseThrow();
        Word word = db.get(Word.class, wordId);
        access.requireLesson(user, word.lesson);
        WordProgress p = progress(user, word);
        LocalDate today = LocalDate.now(StudyRules.ZONE);
        if (p.lastReviewed != null && (p.lastReviewed.atZone(StudyRules.ZONE).toLocalDate().equals(today) || p.nextReview.isAfter(today)))
            return p;
        var result = StudyRules.review(p.stage, remembered);
        p.stage = result.stage();
        p.nextReview = today.plusDays(result.days());
        p.lastReviewed = Instant.now();
        activities.award(user, "word:" + word.id + ":" + today, "vocabulary", 2);
        return p;
    }

    public List<Word> deck(Account user, Long lessonId, String mode, String language) {
        return deckPage(user, lessonId, mode, language, null).words();
    }

    public record DeckPage(List<Word> words, Long nextAfter) { }

    public DeckPage deckPage(Account user, Long lessonId, String mode, String language, Long after) {
        if (after != null && after < 1) throw new IllegalArgumentException("Bộ thẻ không hợp lệ.");
        List<Word> words;
        if (lessonId != null) {
            Lesson l = db.get(Lesson.class, lessonId);
            access.requireLesson(user, l);
            words = db.list(Word.class, "select w from Word w where w.lesson.id=:l order by w.position,w.id", "l", lessonId);
        } else if (mode.equals("due"))
            words = db.list(Word.class, "select p.word from WordProgress p where p.user.id=:u and p.nextReview<=:day and p.word.lesson.chapter.course.language=:language order by p.word.id", "u", user.id, "language", language, "day", LocalDate.now(StudyRules.ZONE));
        else
            words = db.list(Word.class, "select p.word from WordProgress p where p.user.id=:u and p.saved=true and p.word.lesson.chapter.course.language=:language order by p.word.id", "u", user.id, "language", language);
        Word anchor = lessonId != null && after != null ? db.get(Word.class, after) : null;
        if (anchor != null && !anchor.lesson.id.equals(lessonId))
            throw new IllegalArgumentException("Bộ thẻ không thuộc bài học này.");
        List<Word> batch = words.stream()
                .filter(w -> after == null || (anchor == null ? w.id > after
                        : w.position > anchor.position || w.position == anchor.position && w.id > anchor.id))
                .filter(w -> access.lesson(user, w.lesson)).limit(101).toList();
        boolean more = batch.size() > 100;
        List<Word> page = more ? batch.subList(0, 100) : batch;
        return new DeckPage(page, more ? page.getLast().id : null);
    }
}
