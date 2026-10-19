package vn.learnhub.admin;

import vn.learnhub.auth.Access;
import vn.learnhub.community.Feedback;
import vn.learnhub.course.Consultation;
import vn.learnhub.course.Course;
import vn.learnhub.course.Lesson;
import vn.learnhub.learning.Enrollment;
import vn.learnhub.learning.EnrollmentService;
import vn.learnhub.notification.MailJob;
import vn.learnhub.payment.OrderStatus;
import vn.learnhub.payment.PaymentService;
import vn.learnhub.payment.PayosService;
import vn.learnhub.payment.PurchaseOrder;
import vn.learnhub.quiz.Attempt;
import vn.learnhub.shared.audit.Audit;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.shared.study.StudyRules;
import vn.learnhub.storage.FileService;
import vn.learnhub.storage.Resource;
import vn.learnhub.user.Account;
import vn.learnhub.user.Role;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final Access access;
    private final AdminService admin;
    private final Store db;
    private final FileService files;
    private final PaymentService payments;
    private final EnrollmentService enrollments;
    private final PayosService payos;

    public AdminController(Access access, AdminService admin, Store db, FileService files, PaymentService payments, EnrollmentService enrollments, PayosService payos) {
        this.access = access;
        this.admin = admin;
        this.db = db;
        this.files = files;
        this.payments = payments;
        this.enrollments = enrollments;
        this.payos = payos;
    }

    private Account staff(Principal p) {
        var u = access.required(p);
        access.requireStaff(u);
        return u;
    }

    private Account owner(Principal p) {
        var u = access.required(p);
        access.requireAdmin(u);
        return u;
    }

    @GetMapping({"", "/"})
    String home(Principal p, Model m) {
        staff(p);
        m.addAttribute("kinds", admin.kinds());
        m.addAttribute("students", db.count("select count(u) from Account u where u.role=:role", "role", Role.STUDENT));
        m.addAttribute("pending", db.count("select count(o) from PurchaseOrder o where o.status in :statuses", "statuses", List.of(OrderStatus.PENDING, OrderStatus.UNDERPAID)));
        m.addAttribute("consultations", db.count("select count(c) from Consultation c where c.status='new'"));
        return "admin/index";
    }

    @GetMapping("/content/{kind}")
    String list(@PathVariable String kind, @RequestParam(defaultValue = "0") int page, Principal p, Model m) {
        staff(p);
        m.addAttribute("spec", admin.spec(kind));
        m.addAttribute("rows", admin.rows(kind, page));
        m.addAttribute("adminTools", admin);
        m.addAttribute("page", page);
        return "admin/list";
    }

    @GetMapping("/content/{kind}/new")
    String create(@PathVariable String kind, Principal p, Model m) {
        staff(p);
        m.addAttribute("spec", admin.spec(kind));
        m.addAttribute("inputs", admin.inputs(kind, null));
        m.addAttribute("entityId", null);
        return "admin/edit";
    }

    @GetMapping("/content/{kind}/options/{field}")
    @ResponseBody
    List<AdminService.Option> options(@PathVariable String kind, @PathVariable String field,
                                     @RequestParam(defaultValue = "") String q, Principal p) {
        staff(p);
        return admin.relationOptions(kind, field, q);
    }

    @GetMapping("/content/{kind}/{id}")
    String edit(@PathVariable String kind, @PathVariable Long id, Principal p, Model m) {
        staff(p);
        m.addAttribute("spec", admin.spec(kind));
        m.addAttribute("inputs", admin.inputs(kind, id));
        m.addAttribute("entityId", id);
        return "admin/edit";
    }

    @PostMapping("/content/{kind}")
    String save(@PathVariable String kind, @RequestParam(required = false) Long entityId, @RequestParam Map<String, String> values, Principal p, RedirectAttributes f) {
        admin.save(staff(p), kind, entityId, values);
        f.addFlashAttribute("notice", Set.of("questions", "choices").contains(kind)
                ? "Đã lưu. Bài kiểm tra liên quan đã chuyển về bản nháp. Hãy kiểm tra đáp án và xuất bản lại khi hoàn tất."
                : "Đã lưu nội dung.");
        return "redirect:/admin/content/" + kind;
    }

    @GetMapping("/upload")
    String upload(Principal p, Model m) {
        staff(p);
        m.addAttribute("lessons", db.list(Lesson.class, "select l from Lesson l order by l.id desc"));
        m.addAttribute("materials", db.query(Resource.class, "select r from Resource r order by r.id desc").setMaxResults(200).getResultList());
        m.addAttribute("editing", null);
        return "admin/upload";
    }

    @PostMapping("/upload")
    String upload(Principal p, @RequestParam Long lessonId, @RequestParam String title, @RequestParam MultipartFile file, @RequestParam(defaultValue = "false") boolean reviewed, @RequestParam(defaultValue = "false") boolean allowDownload, RedirectAttributes f) throws java.io.IOException {
        files.upload(staff(p), lessonId, title, file, reviewed, allowDownload);
        f.addFlashAttribute("notice", reviewed ? "Đã thêm tài liệu vào bài học." : "Đã lưu tài liệu. Bạn có thể cho học viên xem sau khi kiểm tra.");
        return "redirect:/admin/upload";
    }

    @GetMapping("/materials/{id}")
    String material(@PathVariable Long id, Principal p, Model m) {
        upload(p, m);
        m.addAttribute("editing", db.get(Resource.class, id));
        return "admin/upload";
    }

    @PostMapping("/materials/link")
    String link(Principal p, @RequestParam(required = false) Long entityId, @RequestParam Long lessonId,
                @RequestParam String title, @RequestParam String materialType, @RequestParam String externalUrl,
                @RequestParam(defaultValue = "") String embedUrl, @RequestParam(defaultValue = "false") boolean reviewed,
                RedirectAttributes f) {
        files.link(staff(p), entityId, lessonId, title, materialType, externalUrl, embedUrl, reviewed);
        f.addFlashAttribute("notice", "Đã lưu video hoặc tài liệu.");
        return "redirect:/admin/upload";
    }

    @PostMapping("/materials/{id}/details")
    String materialDetails(@PathVariable Long id, Principal p, @RequestParam String title,
                          @RequestParam(defaultValue = "false") boolean reviewed,
                          @RequestParam(defaultValue = "false") boolean allowDownload, RedirectAttributes f) {
        files.details(staff(p), id, title, reviewed, allowDownload);
        f.addFlashAttribute("notice", "Đã cập nhật tài liệu.");
        return "redirect:/admin/upload";
    }

    @GetMapping("/finance")
    String finance(@RequestParam(defaultValue = "0") int page,
                   @RequestParam(defaultValue = "") String status,
                   @RequestParam(defaultValue = "") String code, Principal p, Model m) {
        owner(p);
        if (page < 0 || page > Integer.MAX_VALUE / 50) throw new IllegalArgumentException("Trang không hợp lệ.");
        status = status.trim();
        code = code.trim();
        String where = " where 1=1";
        List<Object> params = new ArrayList<>();
        if (!status.isBlank()) {
            OrderStatus selected;
            try { selected = OrderStatus.valueOf(status); }
            catch (IllegalArgumentException e) { throw new IllegalArgumentException("Trạng thái không hợp lệ."); }
            where += " and o.status=:status";
            params.add("status"); params.add(selected);
        }
        if (!code.isBlank()) {
            long orderCode;
            try {
                if (!code.matches("[0-9]{1,19}")) throw new NumberFormatException();
                orderCode = Long.parseLong(code);
            } catch (NumberFormatException e) { throw new IllegalArgumentException("Mã đơn phải là số hợp lệ."); }
            where += " and o.code=:code";
            params.add("code"); params.add(orderCode);
        }
        long total = db.count("select count(o) from PurchaseOrder o" + where, params.toArray());
        long totalPages = Math.max(1, (total + 49) / 50);
        page = (int) Math.min(page, totalPages - 1);
        m.addAttribute("orders", db.query(PurchaseOrder.class,
                "select o from PurchaseOrder o" + where + " order by o.createdAt desc,o.id desc", params.toArray())
                .setFirstResult(page * 50).setMaxResults(50).getResultList());
        m.addAttribute("page", page);
        m.addAttribute("totalOrders", total);
        m.addAttribute("totalPages", totalPages);
        m.addAttribute("selectedStatus", status);
        m.addAttribute("orderCode", code);
        m.addAttribute("orderStatuses", OrderStatus.values());
        m.addAttribute("enrollments", db.query(Enrollment.class, "select e from Enrollment e order by e.id desc").setMaxResults(100).getResultList());
        m.addAttribute("users", db.list(Account.class, "select u from Account u order by u.email"));
        m.addAttribute("courses", db.list(Course.class, "select c from Course c order by c.title"));
        return "admin/finance";
    }

    @PostMapping("/finance/{id}/confirm")
    String confirm(@PathVariable UUID id, @RequestParam String reference, Principal p, RedirectAttributes f) {
        if (reference.isBlank()) throw new IllegalArgumentException("Nhập mã giao dịch ngân hàng thực nhận.");
        payments.manualConfirm(owner(p), id, reference);
        f.addFlashAttribute("notice", "Đã xác nhận tiền thực nhận và mở khóa học.");
        return "redirect:/admin/finance?code=" + db.get(PurchaseOrder.class, id).code;
    }

    @PostMapping("/finance/{id}/refresh")
    String refresh(@PathVariable UUID id, Principal p, RedirectAttributes f) {
        owner(p);
        var o = db.get(PurchaseOrder.class, id);
        if (!o.provider.equals("payos")) throw new IllegalArgumentException("Đơn không dùng payOS.");
        payos.reconcile(o);
        f.addFlashAttribute("notice", "Đã kiểm tra trạng thái thanh toán.");
        return "redirect:/admin/finance?code=" + o.code;
    }

    @PostMapping("/finance/grant")
    String grant(@RequestParam Long userId, @RequestParam Long courseId, @RequestParam int days, @RequestParam String reason, Principal p) {
        enrollments.grant(owner(p), userId, courseId, days, reason);
        return "redirect:/admin/finance";
    }

    @PostMapping("/finance/{id}/refund-recorded")
    String refund(@PathVariable UUID id, @RequestParam String reference, @RequestParam String reason, Principal p, RedirectAttributes f) {
        payments.refundRecorded(owner(p), id, reference, reason);
        f.addFlashAttribute("notice", "Đã ghi nhận khoản hoàn bên ngoài. Hệ thống không thực hiện chuyển tiền.");
        return "redirect:/admin/finance?code=" + db.get(PurchaseOrder.class, id).code;
    }

    @GetMapping("/users")
    String users(Principal p, Model m) {
        owner(p);
        m.addAttribute("users", db.list(Account.class, "select u from Account u order by u.id desc"));
        return "admin/users";
    }

    @PostMapping("/users/{id}")
    String user(@PathVariable Long id, Principal p, @RequestParam String role, @RequestParam(defaultValue = "false") boolean active, @RequestParam String reason) {
        admin.updateUser(owner(p), id, role, active, reason);
        return "redirect:/admin/users";
    }

    @GetMapping("/results")
    String results(Principal p, Model m) {
        staff(p);
        m.addAttribute("attempts", db.query(Attempt.class, "select a from Attempt a order by a.startedAt desc").setMaxResults(100).getResultList());
        return "admin/results";
    }

    @GetMapping("/results.csv")
    ResponseEntity<String> csv(Principal p) {
        staff(p);
        StringBuilder out = new StringBuilder("\ufeffEmail,Họ tên,Bài kiểm tra,Điểm,Ngày nộp\r\n");
        for (var a : db.query(Attempt.class, "select a from Attempt a where a.submittedAt is not null order by a.submittedAt desc").setMaxResults(10000).getResultList())
            out.append(String.join(",", StudyRules.safeCsv(a.user.email), StudyRules.safeCsv(a.user.name), StudyRules.safeCsv(a.quiz.title), StudyRules.safeCsv(a.score.toString()), StudyRules.safeCsv(a.submittedAt.toString()))).append("\r\n");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bang-diem.csv").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(out.toString());
    }

    @GetMapping("/inbox")
    String inbox(Principal p, Model m) {
        staff(p);
        m.addAttribute("consultations", db.query(Consultation.class, "select c from Consultation c order by c.createdAt desc").setMaxResults(100).getResultList());
        m.addAttribute("feedback", db.query(Feedback.class, "select f from Feedback f order by f.createdAt desc").setMaxResults(100).getResultList());
        return "admin/inbox";
    }

    @PostMapping("/inbox/consultations/{id}")
    String contact(@PathVariable Long id, @RequestParam String status, Principal p) {
        admin.contactStatus(staff(p), id, status);
        return "redirect:/admin/inbox";
    }

    @PostMapping("/inbox/feedback/{id}")
    String resolve(@PathVariable Long id, Principal p) {
        admin.resolve(staff(p), id);
        return "redirect:/admin/inbox";
    }

    @GetMapping("/operations")
    String operations(Principal p, Model m) {
        owner(p);
        m.addAttribute("jobs", db.query(MailJob.class, "select j from MailJob j order by j.id desc").setMaxResults(100).getResultList());
        m.addAttribute("audit", db.query(Audit.class, "select a from Audit a order by a.createdAt desc").setMaxResults(100).getResultList());
        return "admin/operations";
    }
}
