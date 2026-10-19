package vn.learnhub.payment;

import vn.learnhub.config.AppProperties;
import vn.learnhub.shared.json.JsonTools;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.shared.security.Hmac;

import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;
import java.net.URI;

@Service
public class PayosService {
    private final AppProperties app;
    private final JsonTools json;
    private final Store db;
    private final PaymentService payments;
    private final RestClient client;
    private final TransactionTemplate tx;

    public PayosService(AppProperties app, JsonTools json, Store db, PaymentService payments, PlatformTransactionManager manager) {
        this.app = app;
        this.json = json;
        this.db = db;
        this.payments = payments;
        this.tx = new TransactionTemplate(manager);
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(20000);
        client = RestClient.builder().requestFactory(factory).baseUrl("https://api-merchant.payos.vn/v2/payment-requests")
                .defaultHeader("x-client-id", app.payosClientId()).defaultHeader("x-api-key", app.payosApiKey()).build();
    }

    @SuppressWarnings("unchecked")
    private Object sorted(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new TreeMap<>();
            map.forEach((k, v) -> result.put(k.toString(), sorted(v)));
            return result;
        }
        if (value instanceof List<?> list) return list.stream().map(this::sorted).toList();
        return value;
    }

    public Map<String, String> canonical(Map<String, Object> data) {
        Map<String, String> result = new TreeMap<>();
        data.forEach((k, v) -> result.put(k, v == null || v.equals("null") || v.equals("undefined") ? "" : v instanceof Map<?, ?> || v instanceof List<?> ? json.write(sorted(v)) : v.toString()));
        return result;
    }

    public boolean valid(Map<String, Object> data, String signature) {
        return Hmac.verify(canonical(data), signature, app.payosChecksumKey());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> response(Map<String, Object> result) {
        if (result == null || !"00".equals(result.get("code")) || !(result.get("data") instanceof Map<?, ?>))
            throw new IllegalArgumentException("Nhà cung cấp chưa xác nhận yêu cầu.");
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        if (!valid(data, (String) result.get("signature")))
            throw new IllegalArgumentException("Chưa xác minh được thanh toán. Vui lòng thử lại hoặc liên hệ hỗ trợ.");
        return data;
    }

    private Map<String, Object> fetch(long code) {
        return response(client.get().uri("/" + code).retrieve().body(new ParameterizedTypeReference<Map<String, Object>>() {
        }));
    }

    private long number(Object value) {
        if (!(value instanceof Number n)) throw new IllegalArgumentException("Dữ liệu thanh toán không hợp lệ.");
        return n.longValue();
    }

    public String link(PurchaseOrder order) {
        requireConfigured();
        if (!"payos".equals(order.provider)) throw new IllegalArgumentException("Đơn không dùng payOS.");
        if (!order.status.isAwaitingPayment()) throw new IllegalArgumentException("Đơn không còn chờ thanh toán.");
        if (order.status == OrderStatus.UNDERPAID) {
            reconcile(order);
            return "";
        }
        if (!order.checkoutUrl.isBlank()) {
            Map<String, Object> current = fetch(order.code);
            checkOrder(order, current);
            if (stopsCheckout(current)) {
                applyStatus(order, current);
                return "";
            }
            requirePending(current);
            return order.checkoutUrl;
        }
        String target = app.baseUrl() + "/orders/" + order.id;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", order.amount);
        body.put("cancelUrl", target);
        body.put("returnUrl", target);
        body.put("description", Long.toString(order.code).substring(Long.toString(order.code).length() - 9));
        body.put("orderCode", order.code);
        body.put("signature", Hmac.sign(canonical(body), app.payosChecksumKey()));
        Map<String, Object> data;
        try {
            data = response(client.post().body(body).retrieve().body(new ParameterizedTypeReference<Map<String, Object>>() {
            }));
        } catch (RestClientException | IllegalArgumentException e) {
            data = fetch(order.code);
            checkOrder(order, data);
            if (stopsCheckout(data)) {
                applyStatus(order, data);
                return "";
            }
            requirePending(data);
            String id = Objects.toString(data.get("id"), "");
            if (!id.matches("[A-Za-z0-9-]+")) throw new IllegalArgumentException("Chưa tạo được link thanh toán.");
            data.put("paymentLinkId", id);
            data.put("checkoutUrl", "https://pay.payos.vn/web/" + id);
        }
        checkOrder(order, data);
        if (stopsCheckout(data)) {
            applyStatus(order, data);
            return "";
        }
        requirePending(data);
        String url = Objects.toString(data.get("checkoutUrl"), "");
        URI uri = URI.create(url);
        if (!"https".equals(uri.getScheme()) || !"pay.payos.vn".equals(uri.getHost()) || uri.getUserInfo() != null)
            throw new IllegalArgumentException("Link thanh toán không hợp lệ.");
        String id = Objects.toString(data.get("paymentLinkId"), "");
        Map<String, Object> checkoutData = data;
        Boolean pending = tx.execute(status -> {
            PurchaseOrder locked = db.lock(PurchaseOrder.class, order.id);
            db.entityManager().refresh(locked, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            checkOrder(locked, checkoutData);
            if (locked.status == OrderStatus.PENDING) {
                locked.checkoutUrl = url;
                locked.linkId = id;
                return true;
            }
            return false;
        });
        return Boolean.TRUE.equals(pending) ? url : "";
    }

    public void reconcile(PurchaseOrder order) {
        requireConfigured();
        if (!"payos".equals(order.provider)) throw new IllegalArgumentException("Đơn không dùng payOS.");
        Map<String, Object> data = fetch(order.code);
        checkOrder(order, data);
        applyStatus(order, data);
    }

    private void checkOrder(PurchaseOrder order, Map<String, Object> data) {
        if (number(data.get("orderCode")) != order.code || number(data.get("amount")) != order.amount || !order.linkId.isBlank() && !order.linkId.equals(linkId(data)))
            throw new IllegalArgumentException("Thông tin giao dịch không khớp.");
    }

    private String linkId(Map<String, Object> data) {
        String id = Objects.toString(data.get("id"), "");
        String paymentLinkId = Objects.toString(data.get("paymentLinkId"), "");
        if (!id.isBlank() && !paymentLinkId.isBlank() && !id.equals(paymentLinkId))
            throw new IllegalArgumentException("Mã link thanh toán không khớp.");
        return id.isBlank() ? paymentLinkId : id;
    }

    private boolean stopsCheckout(Map<String, Object> data) {
        return Set.of("PAID", "CANCELLED", "EXPIRED", "FAILED", "UNDERPAID").contains(Objects.toString(data.get("status"), ""))
                || "PENDING".equals(data.get("status")) && data.get("amountPaid") instanceof Number n && n.longValue() > 0;
    }

    private void requirePending(Map<String, Object> data) {
        if (!"PENDING".equals(data.get("status")))
            throw new IllegalArgumentException("Link thanh toán không còn chờ thanh toán. Hãy kiểm tra trạng thái đơn.");
    }

    private void applyStatus(PurchaseOrder order, Map<String, Object> data) {
        String remoteStatus = Objects.toString(data.get("status"), "");
        if (!Set.of("PENDING", "UNDERPAID", "PAID", "CANCELLED", "EXPIRED", "FAILED").contains(remoteStatus))
            throw new IllegalArgumentException("Trạng thái payOS chưa được hỗ trợ. Vui lòng liên hệ hỗ trợ.");
        if ("PAID".equals(remoteStatus)) {
            if (number(data.get("amountPaid")) < order.amount)
                throw new IllegalArgumentException("Nhà cung cấp chưa xác nhận đủ số tiền.");
            String id = linkId(data);
            if (id.isBlank()) throw new IllegalArgumentException("Thiếu mã giao dịch.");
            payments.confirm(order.id, order.amount, "payos:" + id, null);
            return;
        }
        boolean underpaid = "UNDERPAID".equals(remoteStatus)
                || "PENDING".equals(remoteStatus) && data.get("amountPaid") instanceof Number n && n.longValue() > 0;
        if (underpaid) {
            long paid = number(data.get("amountPaid"));
            if (paid <= 0 || paid >= order.amount)
                throw new IllegalArgumentException("Số tiền thanh toán thiếu không hợp lệ.");
        }
        if (underpaid || Set.of("CANCELLED", "EXPIRED", "FAILED").contains(remoteStatus)) {
            tx.executeWithoutResult(status -> {
                PurchaseOrder locked = db.lock(PurchaseOrder.class, order.id);
                // The order may already be cached by the controller or another caller.
                db.entityManager().refresh(locked, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
                checkOrder(locked, data);
                if (locked.status.isAwaitingPayment()) {
                    if (underpaid) {
                        locked.status = OrderStatus.UNDERPAID;
                        // Keep the same pendingKey: never create another order for money already received.
                    } else {
                        locked.status = "FAILED".equals(remoteStatus) ? OrderStatus.FAILED : OrderStatus.CANCELLED;
                        locked.pendingKey = null;
                        locked.checkoutUrl = "";
                    }
                }
            });
        }
    }

    private void requireConfigured() {
        // PAYMENT_MODE selects the provider for new orders. Existing payOS orders
        // must remain usable and reconcilable after switching to manual payments.
        if (!app.hasPayosCredentials()) throw new IllegalArgumentException("Thiếu cấu hình payOS để xử lý đơn này. Vui lòng liên hệ hỗ trợ.");
    }

    public void deferReconciliation(UUID orderId) {
        tx.executeWithoutResult(status -> {
            PurchaseOrder locked = db.lock(PurchaseOrder.class, orderId);
            if (locked.status.isAwaitingPayment())
                locked.nextReconcileAt = java.time.Instant.now().plus(java.time.Duration.ofMinutes(5));
        });
    }
}
