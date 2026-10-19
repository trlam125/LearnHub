package vn.learnhub.notification;

import vn.learnhub.config.AppProperties;
import vn.learnhub.shared.persistence.Store;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.persistence.LockModeType;
import org.slf4j.*;

import java.time.*;

@Service
public class OutboxService {
    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);
    private final Store db;
    private final AppProperties app;
    private final JavaMailSender mail;

    public OutboxService(Store db, AppProperties app, JavaMailSender mail) {
        this.db = db;
        this.app = app;
        this.mail = mail;
    }

    @Transactional
    public boolean deliverOne() {
        var list = db.query(MailJob.class, "select j from MailJob j where j.sentAt is null and j.availableAt<=:now and j.tries<8 order by j.id", "now", Instant.now()).setMaxResults(1).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        if (list.isEmpty()) return false;
        MailJob job = list.get(0);
        try {
            if (app.mailMode().equals("console"))
                log.info("DEV EMAIL to={} subject={} body={}", job.recipient, job.subject, job.body);
            else {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(app.fromEmail());
                message.setTo(job.recipient);
                message.setSubject(job.subject);
                message.setText(job.body);
                mail.send(message);
            }
            job.sentAt = Instant.now();
        } catch (Exception e) {
            log.warn("Email job {} failed; retry scheduled", job.id);
        }
        job.tries++;
        job.availableAt = Instant.now().plusSeconds(Math.min(3600, 30L * (1L << job.tries)));
        return true;
    }
}
