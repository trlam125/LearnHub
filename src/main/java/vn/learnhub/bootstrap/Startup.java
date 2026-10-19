package vn.learnhub.bootstrap;

import vn.learnhub.auth.PasswordRules;

import vn.learnhub.config.AppProperties;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.user.Account;
import vn.learnhub.user.Role;
import vn.learnhub.user.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.env.*;

import java.util.*;

@Component
public class Startup implements CommandLineRunner {
    private final AppProperties app;
    private final Environment env;
    private final Store db;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final DemoData demo;
    private final EnglishDemoData englishDemo;

    public Startup(AppProperties app, Environment env, Store db, UserRepository users, PasswordEncoder encoder, DemoData demo, EnglishDemoData englishDemo) {
        this.app = app;
        this.env = env;
        this.db = db;
        this.users = users;
        this.encoder = encoder;
        this.demo = demo;
        this.englishDemo = englishDemo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!Set.of("manual", "payos").contains(app.paymentMode()) || !Set.of("console", "smtp").contains(app.mailMode()))
            throw new IllegalStateException("PAYMENT_MODE hoặc MAIL_MODE không hợp lệ.");
        if (app.paymentMode().equals("payos") && !app.hasPayosCredentials())
            throw new IllegalStateException("Thiếu credentials payOS.");
        if (!app.hasPayosCredentials() && db.count("select count(o) from PurchaseOrder o where o.provider='payos' and o.status in :statuses", "statuses", List.of(vn.learnhub.payment.OrderStatus.PENDING, vn.learnhub.payment.OrderStatus.UNDERPAID)) > 0)
            throw new IllegalStateException("Còn đơn payOS chờ xử lý. Giữ PAYOS_CLIENT_ID, PAYOS_API_KEY và PAYOS_CHECKSUM_KEY để tiếp tục đối soát dù PAYMENT_MODE=manual.");
        if (env.acceptsProfiles(Profiles.of("prod"))) {
            if (!app.baseUrl().startsWith("https://") || !Objects.toString(env.getProperty("spring.datasource.url"), "").startsWith("jdbc:mysql:") || app.seedDemo() || !app.mailMode().equals("smtp"))
                throw new IllegalStateException("Production cần SITE_URL HTTPS, MySQL, MAIL_MODE=smtp và SEED_DEMO=false.");
        }
        if (!app.bootstrapEmail().isBlank() && users.findByEmailIgnoreCase(app.bootstrapEmail().trim()).isEmpty()) {
            String email = app.bootstrapEmail().trim().toLowerCase(Locale.ROOT);
            String password = app.bootstrapPassword();
            if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length() > 150 || !PasswordRules.valid(password))
                throw new IllegalStateException("ADMIN_EMAIL cần hợp lệ, ADMIN_PASSWORD dài ít nhất 8 ký tự, tối đa 72 byte UTF-8.");
            Account admin = new Account();
            admin.email = email;
            admin.name = "Quản trị viên";
            admin.password = encoder.encode(password);
            admin.role = Role.ADMIN;
            admin.verified = true;
            db.add(admin);
        }
        if (app.seedDemo()) {
            demo.seed();
            englishDemo.seed();
        }
    }
}
