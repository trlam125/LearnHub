package vn.learnhub.auth;

import vn.learnhub.course.Course;
import vn.learnhub.course.Lesson;
import vn.learnhub.learning.Enrollment;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.user.Account;
import vn.learnhub.user.Role;
import vn.learnhub.user.UserRepository;

import java.security.Principal;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

@Service
public class Access {
    private final UserRepository users;
    private final Store db;

    public Access(UserRepository users, Store db) {
        this.users = users;
        this.db = db;
    }

    public Account current(Principal principal) {
        if (principal == null) return null;
        Account user = users.findByEmailIgnoreCase(principal.getName()).orElseThrow(() -> new AccessDeniedException("Tài khoản không hợp lệ."));
        if (principal instanceof org.springframework.security.core.Authentication auth && auth.getPrincipal() instanceof vn.learnhub.auth.SessionUser session && session.version != user.sessionVersion)
            throw new AccessDeniedException("Phiên đăng nhập đã hết hiệu lực. Hãy đăng xuất và đăng nhập lại.");
        if (!user.active || !user.verified) throw new AccessDeniedException("Tài khoản chưa được kích hoạt.");
        return user;
    }

    public Account required(Principal principal) {
        Account user = current(principal);
        if (user == null) throw new AccessDeniedException("Hãy đăng nhập.");
        return user;
    }

    public boolean staff(Account user) {
        return user != null && (user.role == Role.ADMIN || user.role == Role.TEACHER);
    }

    public void requireStaff(Account user) {
        if (!staff(user)) throw new AccessDeniedException("Không đủ quyền.");
    }

    public void requireAdmin(Account user) {
        if (user == null || user.role != Role.ADMIN)
            throw new AccessDeniedException("Chỉ quản trị viên được thao tác.");
    }

    public boolean course(Account user, Course course) {
        if (staff(user)) return true;
        if (user == null) return false;
        return db.count("select count(e) from Enrollment e where e.user.id=:u and e.course.id=:c and e.active=true and e.startsAt<=:now and e.expiresAt>:now", "u", user.id, "c", course.id, "now", Instant.now()) > 0;
    }

    public boolean lesson(Account user, Lesson lesson) {
        return staff(user) || (lesson.published && lesson.chapter.course.published && (lesson.preview || course(user, lesson.chapter.course)));
    }

    public void requireLesson(Account user, Lesson lesson) {
        if (!lesson(user, lesson)) throw new AccessDeniedException("Bạn chưa có quyền học hoặc quyền đã hết hạn.");
    }
}
