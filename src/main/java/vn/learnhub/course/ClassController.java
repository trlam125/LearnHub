package vn.learnhub.course;

import vn.learnhub.notification.MailJob;
import vn.learnhub.shared.persistence.Store;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;

@Controller
public class ClassController {
    private final LearningContext context;
    private final Store db;

    public ClassController(Store db, LearningContext context) {
        this.context = context;
        this.db = db;
    }

    @GetMapping({"/khoa-hoc", "/khoa-hoc/"})
    String classes(Model m) {
        m.addAttribute("classes", db.list(StudyClass.class, "select c from StudyClass c where c.published=true and c.teacher.published=true and c.course.published=true and c.course.language=:language order by c.id", "language", context.code()));
        return "course/classes";
    }

    private StudyClass offering(Long id) {
        var c = db.get(StudyClass.class, id);
        if (!c.published || !c.accepting || !c.teacher.published || !c.course.published)
            throw new IllegalArgumentException("Lớp chưa nhận yêu cầu tư vấn.");
        return c;
    }

    @GetMapping("/lop/{id}/tu-van")
    String consultation(@PathVariable Long id, Model m) {
        StudyClass selected = offering(id);
        context.select(selected.course.language, m);
        m.addAttribute("offering", selected);
        return "course/consultation";
    }

    @PostMapping("/lop/{id}/tu-van")
    @Transactional
    String consultation(@PathVariable Long id, @RequestParam String name, @RequestParam String email, @RequestParam(defaultValue = "") String phone, @RequestParam(defaultValue = "") String message, @RequestParam(defaultValue = "false") boolean consent, RedirectAttributes flash) {
        if (!consent || name.isBlank() || name.length() > 120 || email.length() > 150 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || phone.length() > 30 || message.length() > 2000)
            throw new IllegalArgumentException("Thông tin tư vấn chưa hợp lệ hoặc chưa có sự đồng ý.");
        Consultation c = new Consultation();
        c.offering = offering(id);
        c.name = name.trim();
        c.email = email.trim();
        c.phone = phone.trim();
        c.message = message.trim();
        db.add(c);
        MailJob j = new MailJob();
        j.eventKey = "consultation:" + c.id;
        j.recipient = c.email;
        j.subject = "Đã nhận yêu cầu tư vấn";
        j.body = "Đã nhận yêu cầu tư vấn lớp " + c.offering.title + ". Đây chưa phải xác nhận giữ chỗ hoặc thanh toán.";
        db.add(j);
        flash.addFlashAttribute("notice", "Đã nhận yêu cầu tư vấn. Chúng tôi sẽ liên hệ theo thông tin bạn cung cấp.");
        return "redirect:/khoa-hoc";
    }
}
