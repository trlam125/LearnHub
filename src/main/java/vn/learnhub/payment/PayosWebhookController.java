package vn.learnhub.payment;

import vn.learnhub.config.AppProperties;
import vn.learnhub.shared.persistence.Store;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.time.*;
import java.util.*;

@Controller
public class PayosWebhookController {
    private final Store db;
    private final PayosService payos;
    private final AppProperties app;

    public PayosWebhookController(Store db, PayosService payos, AppProperties app) {
        this.db = db;
        this.payos = payos;
        this.app = app;
    }

    @PostMapping("/payments/payos/webhook")
    @ResponseBody
    ResponseEntity<Map<String, Object>> webhook(@RequestBody Map<String, Object> payload) {
        if (!app.hasPayosCredentials()) return ResponseEntity.notFound().build();
        if (!(payload.get("data") instanceof Map<?, ?> raw) || !(payload.get("signature") instanceof String signature))
            return ResponseEntity.badRequest().body(Map.of("error", "invalid event"));
        @SuppressWarnings("unchecked") Map<String, Object> data = (Map<String, Object>) raw;
        if (!payos.valid(data, signature)) return ResponseEntity.badRequest().body(Map.of("error", "signature"));
        // Reconcile signed failure/partial notifications too; only the fetched state is authoritative.
        if (data.get("orderCode") instanceof Number code) {
            var order = db.first(PurchaseOrder.class, "select o from PurchaseOrder o where o.code=:code and o.provider='payos'", "code", code.longValue());
            try {
                order.ifPresent(payos::reconcile);
            } catch (org.springframework.web.client.RestClientException e) {
                return ResponseEntity.status(503).body(Map.of("error", "retry later"));
            }
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
