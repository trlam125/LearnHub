package vn.learnhub.learning;

import vn.learnhub.auth.Access;
import vn.learnhub.community.ActivityService;
import vn.learnhub.course.Lesson;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.user.Account;
import vn.learnhub.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class LearningProgressService {
    private final Store db;
    private final UserRepository users;
    private final Access access;
    private final ActivityService activities;

    public LearningProgressService(Store db, UserRepository users, Access access, ActivityService activities) {
        this.db = db;
        this.users = users;
        this.access = access;
        this.activities = activities;
    }

    @Transactional
    public void complete(Account user, Long lessonId) {
        users.lockById(user.id).orElseThrow();
        Lesson lesson = db.get(Lesson.class, lessonId);
        access.requireLesson(user, lesson);
        if (db.count("select count(p) from Progress p where p.user.id=:u and p.lesson.id=:l", "u", user.id, "l", lessonId) == 0) {
            Progress p = new Progress();
            p.user = user;
            p.lesson = lesson;
            db.add(p);
            activities.award(user, "lesson:" + lessonId, "lesson", 10);
        }
    }
}
