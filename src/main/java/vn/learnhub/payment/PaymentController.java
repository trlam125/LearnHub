package vn.learnhub.payment;

import vn.learnhub.course.LearningContext;
import vn.learnhub.auth.Access;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.user.Account;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.time.*;
import java.util.*;

@Controller
public class PaymentController {
    private final LearningContext context;
    private final Store db;
    private final Access access;
    private final PaymentService payments;
    private final PayosService payos;

    public PaymentController(Store db, Access access, PaymentService payments, PayosService payos, LearningContext context) {
        this.context = context;
        this.db = db;
        this.access = access;
        this.payments = payments;
        this.payos = payos;
    }

    @PostMapping("/courses/{id}/buy")
    String buy(@PathVariable Long id, Principal p) {
        return "redirect:/orders/" + payments.order(access.required(p), id).id;
    }

    private PurchaseOrder ownedOrder(Principal p, UUID id) {
        Account user = access.required(p);
        PurchaseOrder order = db.get(PurchaseOrder.class, id);
        if (!order.user.id.equals(user.id)) throw new AccessDeniedException("Không được xem đơn khác.");
        return order;
    }

    @GetMapping("/orders/{id}")
    String order(@PathVariable UUID id, Principal p, Model m) {
        PurchaseOrder order = ownedOrder(p, id);
        context.select(order.course.language, m);
        m.addAttribute("order", order);
        return "payment/order";
    }

    @PostMapping("/orders/{id}/checkout")
    String checkout(@PathVariable UUID id, Principal p) {
        var order = ownedOrder(p, id);
        if (!order.provider.equals("payos")) throw new IllegalArgumentException("Đơn này dùng chuyển khoản thủ công.");
        String url = payos.link(order);
        return url.isBlank() ? "redirect:/orders/" + id : "redirect:" + url;
    }

    @PostMapping("/orders/{id}/refresh")
    String refresh(@PathVariable UUID id, Principal p, RedirectAttributes f) {
        var order = ownedOrder(p, id);
        if (order.provider.equals("payos") && order.status.isAwaitingPayment()) payos.reconcile(order);
        f.addFlashAttribute("notice", "Đã kiểm tra trạng thái thanh toán.");
        return "redirect:/orders/" + id;
    }

    @PostMapping("/orders/{id}/cancel")
    String cancel(@PathVariable UUID id, Principal p) {
        payments.cancel(access.required(p), id);
        return "redirect:/orders/" + id;
    }
}
