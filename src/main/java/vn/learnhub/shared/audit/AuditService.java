package vn.learnhub.shared.audit;

import vn.learnhub.shared.persistence.Store;
import vn.learnhub.user.Account;

import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class AuditService {
    private final Store db;

    public AuditService(Store db) {
        this.db = db;
    }

    public void record(Account actor, String action, String target, String detail) {
        Audit a = new Audit();
        a.actor = actor;
        a.action = action;
        a.target = target;
        a.detail = detail;
        db.add(a);
    }
}
