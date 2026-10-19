package vn.learnhub.shared.web;

import org.springframework.web.multipart.MaxUploadSizeExceededException;

import vn.learnhub.auth.Access;
import vn.learnhub.community.StudyStatsService;
import vn.learnhub.config.AppProperties;
import vn.learnhub.course.LearningContext;
import vn.learnhub.user.Account;

import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletResponse;

import java.security.Principal;

@ControllerAdvice
public class GlobalAdvice {
    private final Access access;
    private final StudyStatsService study;
    private final AppProperties app;
    private final LearningContext context;

    public GlobalAdvice(Access access, StudyStatsService study, AppProperties app, LearningContext context) {
        this.access = access;
        this.study = study;
        this.app = app;
        this.context = context;
    }

    @ModelAttribute
    public void common(Principal principal, Model model, jakarta.servlet.http.HttpServletRequest request) {
        context.populate(model);
        model.addAttribute("settings", app);
        model.addAttribute("year", java.time.Year.now().getValue());
        model.addAttribute("requestPath", request.getRequestURI());
        model.addAttribute("me", null);
        model.addAttribute("study", study.stats(null));
        Account me;
        try {
            me = access.current(principal);
        } catch (AccessDeniedException e) {
            var session = request.getSession(false);
            if (session != null) session.invalidate();
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            throw e;
        }
        model.addAttribute("me", me);
        model.addAttribute("study", study.stats(request.getMethod().equals("GET") ? me : null));
    }

    private void errorContext(Model model) {
        // Exception-handler models do not inherit normal controller attributes.
        context.populate(model);
        model.addAttribute("settings", app);
        model.addAttribute("year", java.time.Year.now().getValue());
        model.addAttribute("requestPath", "");
        model.addAttribute("me", null);
        model.addAttribute("study", study.stats(null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String invalid(IllegalArgumentException e, Model m, HttpServletResponse res) {
        errorContext(m);
        res.setStatus(400);
        m.addAttribute("status", 400);
        m.addAttribute("message", e.getMessage());
        return "error";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge(Model m, HttpServletResponse res) {
        errorContext(m);
        res.setStatus(413);
        m.addAttribute("status", 413);
        m.addAttribute("message", vn.learnhub.storage.DocumentRules.SIZE_MESSAGE);
        return "error";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String denied(AccessDeniedException e, Model m, HttpServletResponse res) {
        errorContext(m);
        res.setStatus(403);
        m.addAttribute("status", 403);
        m.addAttribute("message", e.getMessage());
        return "error";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String conflict(Model m, HttpServletResponse res) {
        errorContext(m);
        res.setStatus(409);
        m.addAttribute("status", 409);
        m.addAttribute("message", "Dữ liệu trùng hoặc đang được sử dụng. Kiểm tra lại trước khi lưu.");
        return "error";
    }

    @ExceptionHandler(ResponseStatusException.class)
    public String status(ResponseStatusException e, Model m, HttpServletResponse res) {
        errorContext(m);
        res.setStatus(e.getStatusCode().value());
        m.addAttribute("status", e.getStatusCode().value());
        m.addAttribute("message", "Nội dung không tồn tại hoặc không còn sẵn sàng.");
        return "error";
    }
}
