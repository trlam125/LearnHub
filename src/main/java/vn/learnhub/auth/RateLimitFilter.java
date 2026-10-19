package vn.learnhub.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

public class RateLimitFilter extends OncePerRequestFilter {
    // Access-order eviction bounds memory without denying unrelated IPs at capacity.
    private final Map<String, Bucket> buckets = new LinkedHashMap<>(128, 0.75f, true);
    private final Clock clock;
    private final int capacity;

    public RateLimitFilter() {
        this(Clock.systemUTC(), 10000);
    }

    RateLimitFilter(Clock clock, int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Capacity must be positive");
        this.clock = clock;
        this.capacity = capacity;
    }

    private static class Bucket {
        long start;
        int count;
        final long window;

        Bucket(long start, long window) {
            this.start = start;
            this.window = window;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        if (!req.getMethod().equals("POST")) {
            chain.doFilter(req, res);
            return;
        }
        String path = req.getRequestURI();
        String policy = path;
        int max = 0;
        long window = 3600000;
        if (path.equals("/login")) {
            max = 15;
            window = 60000;
        } else if (path.equals("/register")) max = 10;
        else if (path.equals("/resend") || path.equals("/forgot-password")) max = 5;
        else if (path.matches("/lop/\\d+/tu-van")) {
            max = 5;
            policy = "consultation";
        }
        else if (path.equals("/gop-y")) max = 5;
        else if (path.endsWith("/checkout") || path.endsWith("/refresh")) {
            max = 15;
            window = 600000;
            policy = path.endsWith("/checkout") ? "checkout" : "refresh";
        }
        if (max == 0) {
            chain.doFilter(req, res);
            return;
        }
        long now = clock.millis();
        String key = policy + ":" + req.getRemoteAddr();
        boolean allowed;
        long retryAfter;
        synchronized (buckets) {
            Bucket bucket = buckets.get(key);
            if (bucket == null) {
                if (buckets.size() >= capacity)
                    buckets.entrySet().removeIf(e -> now - e.getValue().start >= e.getValue().window);
                if (buckets.size() >= capacity) {
                    var oldest = buckets.keySet().iterator();
                    oldest.next();
                    oldest.remove();
                }
                bucket = new Bucket(now, window);
                buckets.put(key, bucket);
            }
            if (now - bucket.start >= bucket.window) {
                bucket.start = now;
                bucket.count = 0;
            }
            allowed = bucket.count < max;
            if (allowed) bucket.count++;
            retryAfter = Math.max(1, (bucket.window - (now - bucket.start) + 999) / 1000);
        }
        if (!allowed) {
            res.setStatus(429);
            res.setHeader("Retry-After", Long.toString(retryAfter));
            res.setContentType("text/plain;charset=UTF-8");
            res.getWriter().write("Bạn thao tác quá nhanh. Vui lòng thử lại sau.");
            return;
        }
        chain.doFilter(req, res);
    }
}
