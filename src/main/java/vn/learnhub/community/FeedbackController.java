package vn.learnhub.community;

import vn.learnhub.auth.Access;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.user.Account;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.util.*;

@Controller
public class FeedbackController {
    private final Store db;
    private final Access access;

    public FeedbackController(Store db, Access access) {
        this.db = db;
        this.access = access;
    }

    @PostMapping("/gop-y")
    @Transactional
    String feedback(Principal principal, @RequestParam int rating, @RequestParam String message, RedirectAttributes flash) {
        Account user = access.required(principal);
        if (rating < 1 || rating > 5 || message.isBlank() || message.length() > 2000)
            throw new IllegalArgumentException("Đánh giá chưa hợp lệ.");
        Feedback f = new Feedback();
        f.user = user;
        f.rating = rating;
        f.message = message.trim();
        db.add(f);
        flash.addFlashAttribute("notice", "Góp ý đã được gửi riêng tới quản trị viên.");
        return "redirect:/";
    }
}
