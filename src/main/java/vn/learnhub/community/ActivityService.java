package vn.learnhub.community;

import vn.learnhub.shared.persistence.Store;
import vn.learnhub.user.Account;

import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class ActivityService {
    private final Store db;

    public ActivityService(Store db) {
        this.db = db;
    }

    public boolean award(Account user, String key, String kind, int xp) {
        if (db.count("select count(a) from Activity a where a.user.id=:u and a.eventKey=:key", "u", user.id, "key", key) > 0)
            return false;
        Activity activity = new Activity();
        activity.user = user;
        activity.eventKey = key;
        activity.kind = kind;
        activity.xp = xp;
        db.add(activity);
        return true;
    }
}
