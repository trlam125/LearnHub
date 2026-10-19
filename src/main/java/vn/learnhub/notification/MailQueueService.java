package vn.learnhub.notification;

import vn.learnhub.shared.persistence.Store;
import vn.learnhub.user.Account;

import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class MailQueueService {
    private final Store db;

    public MailQueueService(Store db) {
        this.db = db;
    }

    public void enqueue(String key, Account user, String subject, String body) {
        MailJob job = new MailJob();
        job.eventKey = key;
        job.recipient = user.email;
        job.subject = subject;
        job.body = body;
        db.add(job);
    }
}
