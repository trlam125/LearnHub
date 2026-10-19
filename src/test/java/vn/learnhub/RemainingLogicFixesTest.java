package vn.learnhub;

import vn.learnhub.admin.AdminService;
import vn.learnhub.auth.AuthService;
import vn.learnhub.auth.AuthToken;
import vn.learnhub.bootstrap.*;
import vn.learnhub.config.AppProperties;
import vn.learnhub.course.*;
import vn.learnhub.jobs.BackgroundJobs;
import vn.learnhub.notification.*;
import vn.learnhub.payment.*;
import vn.learnhub.quiz.*;
import vn.learnhub.shared.json.JsonTools;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.shared.security.Hmac;
import vn.learnhub.user.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.http.*;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@SpringBootTest(properties = {"spring.profiles.active=test", "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:remaininglogic;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=DAY;DB_CLOSE_DELAY=-1}", "spring.datasource.username=${TEST_DB_USER:sa}", "spring.datasource.password=${TEST_DB_PASSWORD:}", "spring.flyway.locations=${TEST_FLYWAY_LOCATIONS:classpath:db/migration}", "app.jobs-enabled=false", "app.seed-demo=false", "app.bootstrap-email=admin@example.test", "app.bootstrap-password=Admin-test-password-2026!", "app.mail-mode=console", "app.payment-mode=manual", "app.payos-client-id=test-client", "app.payos-api-key=test-api", "app.payos-checksum-key=test-checksum"})
@AutoConfigureMockMvc
class RemainingLogicFixesTest {
    private final List<Long> fixtureUsers=new ArrayList<>();
    @MockitoSpyBean Store db;
    @Autowired MockMvc mvc;
    @Autowired PlatformTransactionManager manager;
    @Autowired PaymentService payments;
    @Autowired AdminService admin;
    @Autowired JsonTools json;
    @Autowired AuthService auth;
    @Autowired QuizService quizzes;
    @Autowired UserRepository users;

    record Fixture(Account student, Account owner, Course course, Lesson lesson, Quiz quiz) { }
    private Fixture fixture() {
        Fixture fixture=new TransactionTemplate(manager).execute(status -> {
            String unique = UUID.randomUUID().toString();
            Account student = new Account(); student.name="Student"; student.email=unique+"@example.test";
            student.verified=true; db.add(student);
            Account owner = db.first(Account.class,"select u from Account u where u.email='admin@example.test'").orElseThrow();
            Course course=new Course(); course.slug="fix-"+unique; course.title="Regression course";
            course.published=true; db.add(course);
            Chapter ch=new Chapter(); ch.course=course; ch.title="Chapter"; db.add(ch);
            Lesson l=new Lesson(); l.chapter=ch; l.title="Lesson"; l.published=true; l.preview=true; db.add(l);
            Quiz q=new Quiz(); q.lesson=l; q.title="Quiz"; q.published=true; db.add(q);
            Question question=new Question(); question.quiz=q; question.text="2+2?"; db.add(question);
            Choice right=new Choice(); right.question=question; right.text="4"; right.correct=true; db.add(right);
            Choice wrong=new Choice(); wrong.question=question; wrong.text="5"; db.add(wrong);
            return new Fixture(student,owner,course,l,q);
        });
        fixtureUsers.add(fixture.student().id);
        return fixture;
    }

    @AfterEach void settleTestOnlyPendingOrders() {
        if(!fixtureUsers.isEmpty()) new TransactionTemplate(manager).executeWithoutResult(status ->
                db.entityManager().createQuery("update PurchaseOrder o set o.status=:cancelled,o.pendingKey=null where o.user.id in :users and o.status=:pending")
                        .setParameter("cancelled",OrderStatus.CANCELLED).setParameter("pending",OrderStatus.PENDING)
                        .setParameter("users",fixtureUsers).executeUpdate());
    }

    private AppProperties properties(String mode, boolean credentials) {
        return new AppProperties("https://example.test", Path.of("target/test-media"), false, "", "", "console",
                "sender@example.test", "support@example.test", mode, "", "", "",
                credentials ? "client" : "", credentials ? "api" : "", credentials ? "review-key" : "", false);
    }

    private PurchaseOrder payosOrder(Fixture f) {
        PurchaseOrder order=payments.order(f.student(), f.course().id);
        new TransactionTemplate(manager).executeWithoutResult(status -> db.get(PurchaseOrder.class,order.id).provider="payos");
        order.provider="payos";
        return order;
    }

    record Gateway(PayosService service, MockRestServiceServer server) { }
    private Gateway gateway(AppProperties app) {
        PayosService service=new PayosService(app,json,db,payments,manager);
        RestClient.Builder builder=RestClient.builder().baseUrl("https://gateway.test");
        MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(service,"client",builder.build());
        return new Gateway(service,server);
    }

    private Map<String,Object> data(PurchaseOrder order, String status) {
        Map<String,Object> data=new LinkedHashMap<>();
        data.put("id","link-"+order.code); data.put("paymentLinkId","link-"+order.code);
        data.put("checkoutUrl","https://pay.payos.vn/web/link-"+order.code);
        data.put("orderCode",order.code); data.put("amount",order.amount); data.put("status",status);
        if (status.equals("PAID")) data.put("amountPaid",order.amount);
        return data;
    }

    private String signed(Gateway gateway, Map<String,Object> data) {
        return json.write(Map.of("code","00","data",data,"signature",Hmac.sign(gateway.service().canonical(data),"review-key")));
    }

    @Test void cancellationCommittedBeforeConfirmationLockCannotBeResurrected() throws Exception {
        Fixture f=fixture(); PurchaseOrder order=payments.order(f.student(),f.course().id);
        CountDownLatch aboutToLock=new CountDownLatch(1), release=new CountDownLatch(1);
        doAnswer(inv -> {
            if (Thread.currentThread().getName().equals("manual-confirm")) {
                aboutToLock.countDown(); assertTrue(release.await(15,TimeUnit.SECONDS));
            }
            return inv.callRealMethod();
        }).when(db).lock(PurchaseOrder.class,order.id);
        ExecutorService pool=Executors.newSingleThreadExecutor(r -> new Thread(r,"manual-confirm"));
        try {
            Future<?> confirmation=pool.submit(() -> assertThrows(IllegalArgumentException.class,
                    () -> payments.manualConfirm(f.owner(),order.id,"cancelled-race")));
            assertTrue(aboutToLock.await(15,TimeUnit.SECONDS));
            payments.cancel(f.student(),order.id);
            release.countDown(); confirmation.get(15,TimeUnit.SECONDS);
            assertEquals(OrderStatus.CANCELLED,db.get(PurchaseOrder.class,order.id).status);
            assertEquals(0,db.count("select count(p) from Payment p where p.order.id=:id","id",order.id));
        } finally {release.countDown(); pool.shutdownNow();}
    }

    @Test void concurrentManualConfirmationIsIdempotent() throws Exception {
        Fixture f=fixture(); PurchaseOrder order=payments.order(f.student(),f.course().id);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);
        try {
            List<Future<?>> results=new ArrayList<>();
            for(int i=0;i<2;i++) results.add(pool.submit(() -> {
                assertTrue(start.await(15,TimeUnit.SECONDS));
                payments.manualConfirm(f.owner(),order.id,"replay-"+order.id); return null;
            }));
            start.countDown();
            for(Future<?> result:results) result.get(15,TimeUnit.SECONDS);
            assertEquals(1,db.count("select count(p) from Payment p where p.order.id=:id","id",order.id));
            assertEquals(1,db.count("select count(j) from MailJob j where j.eventKey=:key","key","payment:"+order.id));
        } finally {start.countDown(); pool.shutdownNow();}
    }

    @Test void gamesAreWithdrawnAndLegacyPrivateGamesStayHidden() throws Exception {
        Fixture f=fixture(); String title="Preview regression "+UUID.randomUUID();
        Long topicId=admin.save(f.owner(),"topics",null,Map.of("title",title,"description","", "symbol","游", "quiz",f.quiz().id.toString(),"published","true","position","1"));
        admin.save(f.owner(),"lessons",f.lesson().id,Map.of("chapter",f.lesson().chapter.id.toString(),"title","Lesson","content","","position","1","published","true"));
        assertFalse(db.get(GameTopic.class,topicId).published);
        new TransactionTemplate(manager).executeWithoutResult(status -> db.get(GameTopic.class,topicId).published=true);
        String html=mvc.perform(get("/tro-choi?lang=zh")).andReturn().getResponse().getContentAsString();
        assertFalse(html.contains(title));
        assertEquals(403,mvc.perform(post("/quizzes/"+f.quiz().id+"/start").with(user(f.student().email)).with(csrf())).andReturn().getResponse().getStatus());
    }

    @Test void cancelledAndExpiredCheckoutRecoveryReleasesPendingOrder() {
        for(String state:List.of("CANCELLED","EXPIRED")) {
            Fixture f=fixture(); PurchaseOrder order=payosOrder(f); Gateway gateway=gateway(properties("payos",true));
            gateway.server().expect(requestTo("https://gateway.test")).andRespond(withStatus(HttpStatus.CONFLICT));
            gateway.server().expect(requestTo("https://gateway.test/"+order.code)).andRespond(withSuccess(signed(gateway,data(order,state)),MediaType.APPLICATION_JSON));
            assertEquals("",gateway.service().link(order));
            PurchaseOrder persisted=db.get(PurchaseOrder.class,order.id);
            assertEquals(OrderStatus.CANCELLED,persisted.status); assertNull(persisted.pendingKey);
            assertTrue(persisted.checkoutUrl.isBlank());
            assertNotEquals(order.id,payments.order(f.student(),f.course().id).id);
            gateway.server().verify();
        }
    }

    @Test void cachedCheckoutChecksRemoteStatusBeforeRedirecting() {
        Fixture f=fixture(); PurchaseOrder order=payosOrder(f); Gateway gateway=gateway(properties("payos",true));
        order.linkId="link-"+order.code; order.checkoutUrl="https://pay.payos.vn/web/"+order.linkId;
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            PurchaseOrder stored=db.get(PurchaseOrder.class,order.id); stored.linkId=order.linkId; stored.checkoutUrl=order.checkoutUrl;
        });
        gateway.server().expect(requestTo("https://gateway.test/"+order.code)).andRespond(withSuccess(signed(gateway,data(order,"CANCELLED")),MediaType.APPLICATION_JSON));
        assertEquals("",gateway.service().link(order));
        assertEquals(OrderStatus.CANCELLED,db.get(PurchaseOrder.class,order.id).status);
        gateway.server().verify();
    }

    @Test void paidCheckoutRecoveryConfirmsWithoutSecondFetch() {
        Fixture f=fixture(); PurchaseOrder order=payosOrder(f); Gateway gateway=gateway(properties("payos",true));
        gateway.server().expect(requestTo("https://gateway.test")).andRespond(withStatus(HttpStatus.CONFLICT));
        gateway.server().expect(requestTo("https://gateway.test/"+order.code)).andRespond(withSuccess(signed(gateway,data(order,"PAID")),MediaType.APPLICATION_JSON));
        assertEquals("",gateway.service().link(order));
        assertEquals(OrderStatus.PAID,db.get(PurchaseOrder.class,order.id).status);
        gateway.server().verify();
    }

    @Test void cancellationForWrongRemoteOrderCannotReleaseLocalOrder() {
        Fixture f=fixture(); PurchaseOrder order=payosOrder(f); Gateway gateway=gateway(properties("payos",true));
        Map<String,Object> wrong=data(order,"CANCELLED"); wrong.put("orderCode",order.code+1);
        gateway.server().expect(requestTo("https://gateway.test")).andRespond(withStatus(HttpStatus.CONFLICT));
        gateway.server().expect(requestTo("https://gateway.test/"+order.code)).andRespond(withSuccess(signed(gateway,wrong),MediaType.APPLICATION_JSON));
        assertThrows(IllegalArgumentException.class,() -> gateway.service().link(order));
        assertEquals(OrderStatus.PENDING,db.get(PurchaseOrder.class,order.id).status);
        assertNotNull(db.get(PurchaseOrder.class,order.id).pendingKey);
        gateway.server().verify();
    }

    @Test void olderQuestionStaysSelectedAndCanBeFoundAfterTwoThousandNewQuestions() throws Exception {
        Fixture f=fixture();
        Choice existing=db.first(Choice.class,"select c from Choice c where c.question.quiz.id=:q and c.correct=true","q",f.quiz().id).orElseThrow();
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            Quiz quiz=db.get(Quiz.class,f.quiz().id);
            for(int i=0;i<2000;i++) {
                Question q=new Question(); q.quiz=quiz; q.text="Newer question "+i; db.add(q);
            }
        });
        AdminService.Input input=admin.inputs("choices",existing.id).stream().filter(x -> x.name().equals("question")).findFirst().orElseThrow();
        assertEquals(existing.question.id.toString(),input.value());
        assertTrue(input.options().stream().anyMatch(x -> x.value().equals(input.value())));
        var matches=admin.relationOptions("choices","question","#"+existing.question.id);
        assertTrue(matches.stream().anyMatch(x -> x.value().equals(input.value())));
        String endpoint="/admin/content/choices/options/question?q=%23"+existing.question.id;
        assertEquals(403,mvc.perform(get(endpoint).with(user(f.student().email))).andReturn().getResponse().getStatus());
        assertEquals(200,mvc.perform(get("/admin/content/choices/"+existing.id).with(user(f.owner().email).roles("ADMIN"))).andReturn().getResponse().getStatus());
    }

    @Test void legacyPayosOrderStillChecksOutAndReconcilesInManualMode() {
        Fixture f=fixture(); PurchaseOrder order=payosOrder(f); Gateway gateway=gateway(properties("manual",true));
        assertEquals(order.id,payments.order(f.student(),f.course().id).id);
        gateway.server().expect(requestTo("https://gateway.test")).andRespond(withSuccess(signed(gateway,data(order,"PENDING")),MediaType.APPLICATION_JSON));
        gateway.server().expect(requestTo("https://gateway.test/"+order.code)).andRespond(withSuccess(signed(gateway,data(order,"PAID")),MediaType.APPLICATION_JSON));
        assertEquals("https://pay.payos.vn/web/link-"+order.code,gateway.service().link(order));
        gateway.service().reconcile(db.get(PurchaseOrder.class,order.id));
        assertEquals(OrderStatus.PAID,db.get(PurchaseOrder.class,order.id).status);
        Fixture other=fixture(); assertEquals("manual",payments.order(other.student(),other.course().id).provider);
        gateway.server().verify();
    }

    @Test void manualModeRetainsLegacyWebhookAndBackgroundReconciliation() throws Exception {
        Fixture f=fixture(); PurchaseOrder order=payosOrder(f); AppProperties app=properties("manual",true);
        PayosService gateway=mock(PayosService.class); when(gateway.valid(anyMap(),anyString())).thenReturn(true);
        MockMvc webhooks=MockMvcBuilders.standaloneSetup(new PayosWebhookController(db,gateway,app)).build();
        assertEquals(200,webhooks.perform(post("/payments/payos/webhook").contentType(MediaType.APPLICATION_JSON)
                .content(json.write(Map.of("signature","signed","data",Map.of("code","00","orderCode",order.code)))))
                .andReturn().getResponse().getStatus());
        verify(gateway).reconcile(argThat(o -> o.id.equals(order.id)));
        clearInvocations(gateway);
        new BackgroundJobs(db,quizzes,mock(OutboxService.class),gateway,app).reconcile();
        verify(gateway).reconcile(argThat(o -> o.id.equals(order.id)));
    }

    @Test void startupRequiresCredentialsUntilOldPayosOrdersAreSettled() {
        Fixture f=fixture(); payosOrder(f);
        Startup startup=new Startup(properties("manual",false),mock(org.springframework.core.env.Environment.class),db,
                users,mock(org.springframework.security.crypto.password.PasswordEncoder.class),mock(DemoData.class),mock(EnglishDemoData.class));
        IllegalStateException error=assertThrows(IllegalStateException.class,startup::run);
        assertTrue(error.getMessage().contains("PAYOS_CHECKSUM_KEY"));
    }

    @Test void concurrentTokenIsInvalidatedByPasswordResetWithRepeatableRead() throws Exception {
        Fixture f=fixture(); String raw=UUID.randomUUID().toString();
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            AuthToken token=new AuthToken(); token.user=db.get(Account.class,f.student().id);
            token.kind="reset"; token.tokenHash=Hmac.sha256(raw); token.expiresAt=Instant.now().plusSeconds(3600); db.add(token);
        });
        CountDownLatch read=new CountDownLatch(1), release=new CountDownLatch(1);
        doAnswer(inv -> {
            if(Thread.currentThread().getName().equals("reset-with-repeatable-read")) {
                read.countDown(); assertTrue(release.await(15,TimeUnit.SECONDS));
            }
            return inv.callRealMethod();
        }).when(db).lock(Account.class,f.student().id);
        ExecutorService pool=Executors.newSingleThreadExecutor(r -> new Thread(r,"reset-with-repeatable-read"));
        try {
            Future<?> reset=pool.submit(() -> {
                TransactionTemplate rr=new TransactionTemplate(manager);
                rr.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
                rr.executeWithoutResult(status -> auth.reset(raw,"New-password-2026!","New-password-2026!"));
            });
            assertTrue(read.await(15,TimeUnit.SECONDS));
            auth.requestToken(f.student().email,"reset");
            release.countDown(); reset.get(15,TimeUnit.SECONDS);
            assertEquals(0,db.count("select count(t) from AuthToken t where t.user.id=:u and t.kind='reset' and t.usedAt is null","u",f.student().id));
            MailJob job=db.first(MailJob.class,"select j from MailJob j where j.recipient=:email","email",f.student().email).orElseThrow();
            var matcher=java.util.regex.Pattern.compile("/reset-password\\?token=([A-Za-z0-9_-]+)").matcher(job.body);
            assertTrue(matcher.find());
            assertThrows(IllegalArgumentException.class,() -> auth.reset(matcher.group(1),"Another-password-2026!","Another-password-2026!"));
            assertEquals(1,db.get(Account.class,f.student().id).sessionVersion);
        } finally {release.countDown(); pool.shutdownNow();}
    }
}
