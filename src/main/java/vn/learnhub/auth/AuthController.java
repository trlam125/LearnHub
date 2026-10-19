package vn.learnhub.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @GetMapping("/login")
    String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    String register() {
        return "auth/register";
    }

    @PostMapping("/register")
    String register(@RequestParam String name, @RequestParam String email, @RequestParam String password, @RequestParam String confirm, @RequestParam(defaultValue = "false") boolean consent, RedirectAttributes flash) {
        auth.register(name, email, password, confirm, consent);
        flash.addFlashAttribute("notice", "Đã tạo tài khoản. Mở email xác minh trước khi đăng nhập.");
        return "redirect:/login";
    }

    @GetMapping("/verify")
    String verify(@RequestParam String token, Model m) {
        auth.inspect(token, "verify");
        m.addAttribute("token", token);
        return "auth/verify";
    }

    @PostMapping("/verify")
    String verify(@RequestParam String token, RedirectAttributes flash) {
        auth.verify(token);
        flash.addFlashAttribute("notice", "Email đã xác minh. Bạn có thể đăng nhập.");
        return "redirect:/login";
    }

    @GetMapping({"/resend", "/forgot-password"})
    String request(jakarta.servlet.http.HttpServletRequest req, Model m) {
        m.addAttribute("reset", req.getRequestURI().equals("/forgot-password"));
        return "auth/request";
    }

    @PostMapping({"/resend", "/forgot-password"})
    String request(@RequestParam String email, jakarta.servlet.http.HttpServletRequest req, RedirectAttributes flash) {
        auth.requestToken(email, req.getRequestURI().equals("/forgot-password") ? "reset" : "verify");
        flash.addFlashAttribute("notice", "Nếu tài khoản phù hợp, email hướng dẫn sẽ được gửi. Vui lòng kiểm tra hộp thư.");
        return "redirect:/login";
    }

    @GetMapping("/reset-password")
    String reset(@RequestParam String token, Model m) {
        auth.inspect(token, "reset");
        m.addAttribute("token", token);
        return "auth/reset";
    }

    @PostMapping("/reset-password")
    String reset(@RequestParam String token, @RequestParam String password, @RequestParam String confirm, RedirectAttributes flash) {
        auth.reset(token, password, confirm);
        flash.addFlashAttribute("notice", "Đã đổi mật khẩu. Hãy đăng nhập lại.");
        return "redirect:/login";
    }
}
