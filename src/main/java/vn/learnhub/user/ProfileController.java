package vn.learnhub.user;

import vn.learnhub.auth.Access;
import vn.learnhub.shared.persistence.Store;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.util.*;

@Controller
public class ProfileController {
    private final Store db;
    private final Access access;

    public ProfileController(Store db, Access access) {
        this.db = db;
        this.access = access;
    }

    @GetMapping("/tai-khoan")
    String profile() {
        return "user/profile";
    }

    @PostMapping("/tai-khoan")
    @Transactional
    String profile(Principal principal, @RequestParam String name, @RequestParam(defaultValue = "") String publicName, @RequestParam(defaultValue = "false") boolean rankingVisible, RedirectAttributes flash) {
        Account user = access.required(principal);
        if (name.isBlank() || name.length() > 150 || publicName.length() > 40 || rankingVisible && publicName.isBlank())
            throw new IllegalArgumentException("Nhập tên và bí danh hợp lệ khi tham gia bảng xếp hạng.");
        int changed = db.entityManager().createQuery("update Account u set u.name=:name,u.publicName=:publicName,u.rankingVisible=:visible where u.id=:id and u.sessionVersion=:version and u.active=true and u.verified=true")
                .setParameter("name", name.trim()).setParameter("publicName", publicName.trim())
                .setParameter("visible", rankingVisible).setParameter("id", user.id)
                .setParameter("version", user.sessionVersion).executeUpdate();
        if (changed != 1) throw new org.springframework.security.access.AccessDeniedException("Tài khoản đã thay đổi. Hãy đăng nhập lại.");
        flash.addFlashAttribute("notice", "Đã lưu thông tin và lựa chọn riêng tư.");
        return "redirect:/tai-khoan";
    }
}
