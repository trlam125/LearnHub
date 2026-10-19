package vn.learnhub.auth;

import vn.learnhub.config.AppProperties;
import vn.learnhub.notification.MailJob;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.shared.security.Hmac;
import vn.learnhub.user.Account;
import vn.learnhub.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.validation.Validator;

import java.time.*;
import java.security.SecureRandom;
import java.util.*;

@Service
public class AuthService {
    private final Store db;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AppProperties app;
    private final Validator validator;

    public AuthService(Store db, UserRepository users, PasswordEncoder encoder, AppProperties app, Validator validator) {
        this.db = db;
        this.users = users;
        this.encoder = encoder;
        this.app = app;
        this.validator = validator;
    }

    private String email(String email) {
        String result = email.trim().toLowerCase(Locale.ROOT);
        if (result.length() > 150) throw new IllegalArgumentException("Email quá dài.");
        return result;
    }

    private void password(String password, String confirm) {
        if (!PasswordRules.valid(password) || !password.equals(confirm))
            throw new IllegalArgumentException("Hai mật khẩu cần giống nhau và có ít nhất 8 ký tự. Nếu mật khẩu quá dài, hãy rút ngắn rồi thử lại.");
    }

    @Transactional
    public void register(String name, String email, String password, String confirm, boolean consent) {
        password(password, confirm);
        if (!consent) throw new IllegalArgumentException("Hãy đọc và đồng ý điều khoản.");
        String normalized = email(email);
        if (users.findByEmailIgnoreCase(normalized).isPresent())
            throw new IllegalArgumentException("Email đã được dùng. Hãy đăng nhập hoặc đặt lại mật khẩu.");
        Account user = new Account();
        user.name = name.trim();
        user.email = normalized;
        user.password = encoder.encode(password);
        if (!validator.validate(user).isEmpty()) throw new IllegalArgumentException("Tên hoặc email không hợp lệ.");
        db.add(user);
        issue(user, "verify");
    }

    @Transactional
    public void requestToken(String email, String kind) {
        users.findByEmailIgnoreCase(email(email)).filter(u -> u.active && (!kind.equals("verify") || !u.verified)).ifPresent(u -> {
            db.lock(Account.class, u.id);
            boolean recent = db.count("select count(t) from AuthToken t where t.user.id=:u and t.kind=:kind and t.expiresAt>:until and t.usedAt is null", "u", u.id, "kind", kind, "until", Instant.now().plus(Duration.ofHours(23)).plus(Duration.ofMinutes(50))) > 0;
            if (!recent) issue(u, kind);
        });
    }

    private void issue(Account user, String kind) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        AuthToken token = new AuthToken();
        token.user = user;
        token.kind = kind;
        token.tokenHash = Hmac.sha256(raw);
        token.expiresAt = Instant.now().plus(Duration.ofHours(24));
        db.add(token);
        MailJob job = new MailJob();
        job.eventKey = "token:" + token.tokenHash;
        job.recipient = user.email;
        job.subject = kind.equals("verify") ? "Xác minh email LearnHub Languages" : "Đặt lại mật khẩu LearnHub Languages";
        job.body = "Mở liên kết trong vòng 24 giờ: " + app.baseUrl() + (kind.equals("verify") ? "/verify?token=" : "/reset-password?token=") + raw + "\nNếu không yêu cầu, hãy bỏ qua thư này.";
        db.add(job);
    }

    public AuthToken inspect(String raw, String kind) {
        return db.first(AuthToken.class, "select t from AuthToken t where t.tokenHash=:hash and t.kind=:kind and t.usedAt is null and t.expiresAt>:now", "hash", Hmac.sha256(raw), "kind", kind, "now", Instant.now()).orElseThrow(() -> new IllegalArgumentException("Liên kết không hợp lệ hoặc đã hết hạn."));
    }

    @Transactional
    public void verify(String raw) {
        AuthToken token = inspect(raw, "verify");
        Account lockedUser = db.lock(Account.class, token.user.id);
        db.entityManager().refresh(lockedUser, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        token = db.lock(AuthToken.class, token.id);
        db.entityManager().refresh(token, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (token.usedAt != null || !token.expiresAt.isAfter(Instant.now()))
            throw new IllegalArgumentException("Liên kết đã dùng.");
        token.user.verified = true;
        token.usedAt = Instant.now();
    }

    // Own this transaction so an outer REPEATABLE_READ snapshot cannot hide
    // reset links issued while we wait for the account lock.
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW,
            isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void reset(String raw, String password, String confirm) {
        password(password, confirm);
        AuthToken token = inspect(raw, "reset");
        Account lockedUser = db.lock(Account.class, token.user.id);
        db.entityManager().refresh(lockedUser, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        token = db.lock(AuthToken.class, token.id);
        db.entityManager().refresh(token, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (token.usedAt != null || !token.expiresAt.isAfter(Instant.now()))
            throw new IllegalArgumentException("Liên kết đã dùng.");
        token.user.password = encoder.encode(password);
        token.user.sessionVersion++;
        // Invalidate all current rows, including links issued while we waited
        // for the user lock. Token issuance also takes this account lock.
        db.entityManager().createQuery("update AuthToken t set t.usedAt=:now where t.user.id=:u and t.kind='reset' and t.usedAt is null")
                .setParameter("now", Instant.now()).setParameter("u", token.user.id).executeUpdate();
        // Keep the already-managed token consistent with the bulk update.
        db.entityManager().refresh(token);
    }
}
