package vn.learnhub.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {
    private static final class TestClock extends Clock {
        long now=1000000;
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return Instant.ofEpochMilli(now); }
        public long millis() { return now; }
    }

    private MockHttpServletResponse request(RateLimitFilter filter, String path, String ip) throws Exception {
        MockHttpServletRequest req=new MockHttpServletRequest("POST",path); req.setRemoteAddr(ip);
        MockHttpServletResponse res=new MockHttpServletResponse();
        filter.doFilter(req,res,(request,response) -> ((MockHttpServletResponse)response).setStatus(204));
        return res;
    }

    @Test void fullCacheStillAllowsNewIpAndRetainsRecentlyLimitedIp() throws Exception {
        RateLimitFilter filter=new RateLimitFilter(new TestClock(),3);
        assertEquals(204,request(filter,"/login","old-a").getStatus());
        assertEquals(204,request(filter,"/login","old-b").getStatus());
        for(int i=0;i<15;i++) assertEquals(204,request(filter,"/login","hot").getStatus());
        assertEquals(429,request(filter,"/login","hot").getStatus());
        assertEquals(204,request(filter,"/login","new-ip").getStatus());
        assertEquals(429,request(filter,"/login","hot").getStatus());
        assertEquals(3,((Map<?,?>)ReflectionTestUtils.getField(filter,"buckets")).size());
    }

    @Test void expiredLoginBucketsAreReclaimedBeforeAnActiveHourBucket() throws Exception {
        TestClock clock=new TestClock(); RateLimitFilter filter=new RateLimitFilter(clock,2);
        for(int i=0;i<10;i++) request(filter,"/register","long-window");
        request(filter,"/login","short-window"); clock.now+=60000;
        assertEquals(204,request(filter,"/login","new-ip").getStatus());
        assertEquals(429,request(filter,"/register","long-window").getStatus());
    }

    @Test void retryAfterUsesRemainingWindowAndBudgetResetsAtBoundary() throws Exception {
        TestClock clock=new TestClock(); RateLimitFilter filter=new RateLimitFilter(clock,10);
        for(int i=0;i<15;i++) request(filter,"/login","ip");
        clock.now+=59000;
        var denied=request(filter,"/login","ip"); assertEquals(429,denied.getStatus()); assertEquals("1",denied.getHeader("Retry-After"));
        clock.now+=1000;
        assertEquals(204,request(filter,"/login","ip").getStatus());
    }

    @Test void changingOrderIdCannotBypassBudgetOrFillCache() throws Exception {
        RateLimitFilter filter=new RateLimitFilter(new TestClock(),10);
        for(int i=0;i<15;i++) assertEquals(204,request(filter,"/orders/"+i+"/checkout","ip").getStatus());
        assertEquals(429,request(filter,"/orders/another/checkout","ip").getStatus());
        assertEquals(1,((Map<?,?>)ReflectionTestUtils.getField(filter,"buckets")).size());
        assertEquals(204,request(filter,"/login","ip").getStatus());
    }

    @Test void concurrentRequestsCannotExceedLoginBudget() throws Exception {
        RateLimitFilter filter=new RateLimitFilter(new TestClock(),10);
        ExecutorService pool=Executors.newFixedThreadPool(8);
        try {
            List<Callable<Integer>> tasks=new ArrayList<>();
            for(int i=0;i<64;i++) tasks.add(() -> request(filter,"/login","same-ip").getStatus());
            int allowed=0;
            for(Future<Integer> result:pool.invokeAll(tasks)) if(result.get()==204) allowed++;
            assertEquals(15,allowed);
        } finally { pool.shutdownNow(); }
    }
}
