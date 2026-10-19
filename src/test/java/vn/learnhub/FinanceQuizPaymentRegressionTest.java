package vn.learnhub;

import vn.learnhub.admin.AdminService;
import vn.learnhub.config.AppProperties;
import vn.learnhub.course.*;
import vn.learnhub.jobs.BackgroundJobs;
import vn.learnhub.learning.Enrollment;
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

@SpringBootTest(properties = {"spring.profiles.active=test", "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:financequizpayment;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=DAY;DB_CLOSE_DELAY=-1}", "spring.datasource.username=${TEST_DB_USER:sa}", "spring.datasource.password=${TEST_DB_PASSWORD:}", "spring.flyway.locations=${TEST_FLYWAY_LOCATIONS:classpath:db/migration}", "app.jobs-enabled=false", "app.seed-demo=false", "app.bootstrap-email=admin@example.test", "app.bootstrap-password=Admin-test-password-2026!", "app.mail-mode=console", "app.payment-mode=manual", "app.payos-client-id=test-client", "app.payos-api-key=test-api", "app.payos-checksum-key=test-checksum"})
@AutoConfigureMockMvc
class FinanceQuizPaymentRegressionTest {
    private final List<Long> fixtureUsers=new ArrayList<>();
    @Autowired Store db;
    @Autowired MockMvc mvc;
    @Autowired PlatformTransactionManager manager;
    @Autowired PaymentService payments;
    @Autowired AdminService admin;
    @Autowired JsonTools json;
    @Autowired QuizService quizzes;

    record Fixture(Account student, Account owner, Course course, Lesson lesson, Quiz quiz) { }
    private Fixture fixture() {
        Fixture fixture=new TransactionTemplate(manager).execute(status -> {
            String unique = UUID.randomUUID().toString();
            Account student = new Account(); student.name="Student"; student.email=unique+"@example.test";
            student.verified=true; db.add(student);
            Account owner = db.first(Account.class,"select u from Account u where u.email='admin@example.test'").orElseThrow();
            Course course=new Course(); course.slug="fix-"+unique; course.title="Regression course";
            course.price=100000; course.published=true; db.add(course);
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
                db.entityManager().createQuery("update PurchaseOrder o set o.status=:cancelled,o.pendingKey=null where o.user.id in :users and o.status in :pending")
                        .setParameter("cancelled",OrderStatus.CANCELLED).setParameter("pending",List.of(OrderStatus.PENDING,OrderStatus.UNDERPAID))
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

    @Test void oldOrdersRemainReachableThroughPagesAndFilters() throws Exception {
        Fixture f=fixture(); PurchaseOrder old=payments.order(f.student(),f.course().id);
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            db.get(PurchaseOrder.class,old.id).createdAt=Instant.parse("2000-01-01T00:00:00Z");
            for (int i=1;i<=125;i++) {
                PurchaseOrder newer=new PurchaseOrder(); newer.user=db.get(Account.class,f.student().id);
                newer.course=db.get(Course.class,f.course().id); newer.code=old.code+i;
                newer.status=OrderStatus.CANCELLED; db.add(newer);
            }
        });
        var first=mvc.perform(get("/admin/finance").with(user(f.owner().email).roles("ADMIN"))).andReturn();
        assertEquals(200,first.getResponse().getStatus());
        Map<String,Object> model=first.getModelAndView().getModel();
        assertEquals(50,((List<?>)model.get("orders")).size());
        long pages=((Number)model.get("totalPages")).longValue();
        assertTrue(pages>=3);
        var last=mvc.perform(get("/admin/finance").param("page",Long.toString(pages-1))
                .with(user(f.owner().email).roles("ADMIN"))).andReturn();
        assertEquals(200,last.getResponse().getStatus());
        assertTrue(((List<?>)last.getModelAndView().getModel().get("orders")).stream()
                .anyMatch(o -> ((PurchaseOrder)o).id.equals(old.id)));
        var found=mvc.perform(get("/admin/finance").param("code",Long.toString(old.code)).param("status","PENDING")
                .with(user(f.owner().email).roles("ADMIN"))).andReturn();
        assertEquals(200,found.getResponse().getStatus());
        assertEquals(1,((List<?>)found.getModelAndView().getModel().get("orders")).size());
        assertTrue(found.getResponse().getContentAsString().contains("/admin/finance/"+old.id+"/confirm"));
        var confirmed=mvc.perform(post("/admin/finance/"+old.id+"/confirm").param("reference","old-"+old.id)
                .with(user(f.owner().email).roles("ADMIN")).with(csrf())).andReturn();
        assertEquals(302,confirmed.getResponse().getStatus());
        assertEquals("/admin/finance?code="+old.code,confirmed.getResponse().getRedirectedUrl());
        assertEquals(OrderStatus.PAID,db.get(PurchaseOrder.class,old.id).status);
        var empty=mvc.perform(get("/admin/finance").param("code",Long.toString(old.code)).param("status","PENDING")
                .with(user(f.owner().email).roles("ADMIN"))).andReturn();
        assertEquals(0,((List<?>)empty.getModelAndView().getModel().get("orders")).size());
    }

    private Lesson privateLesson(Fixture f) {
        return new TransactionTemplate(manager).execute(tx -> {
            Lesson lesson=new Lesson(); lesson.title="Private lesson";
            lesson.chapter=db.get(Chapter.class,f.lesson().chapter.id); lesson.published=true;
            lesson.preview=false; return db.add(lesson);
        });
    }

    private void moveQuiz(Fixture f, Lesson lesson) {
        admin.save(f.owner(),"quizzes",f.quiz().id,Map.of("lesson",lesson.id.toString(),"title","Quiz",
                "minutes","10","maxAttempts","3","passScore","5","published","true"));
    }

    @Test void quizCannotMoveUntilAttemptIsSubmittedButOtherEditsAreAllowed() {
        Fixture f=fixture(); Lesson target=privateLesson(f); Attempt attempt=quizzes.start(f.student(),f.quiz().id);
        assertThrows(IllegalArgumentException.class,() -> moveQuiz(f,target));
        assertEquals(f.lesson().id,db.get(Quiz.class,f.quiz().id).lesson.id);
        moveQuiz(f,f.lesson());
        assertNull(quizzes.save(f.student(),attempt.id,Map.of(),false).submittedAt);
        assertNotNull(quizzes.save(f.student(),attempt.id,Map.of(),true).submittedAt);
        moveQuiz(f,target);
        assertEquals(target.id,db.get(Quiz.class,f.quiz().id).lesson.id);
    }

    @Test void expiredButUnfinalizedAttemptAlsoBlocksMovingQuiz() {
        Fixture f=fixture(); Lesson target=privateLesson(f); Attempt a=quizzes.start(f.student(),f.quiz().id);
        new TransactionTemplate(manager).executeWithoutResult(tx -> db.get(Attempt.class,a.id).deadline=Instant.now().minusSeconds(1));
        assertThrows(IllegalArgumentException.class,() -> moveQuiz(f,target));
        quizzes.finishExpired(a.id);
        moveQuiz(f,target);
        assertEquals(target.id,db.get(Quiz.class,f.quiz().id).lesson.id);
    }

    @Test void simultaneousStartAndMoveCannotCreateAnAttemptWithRevokedAccess() throws Exception {
        Fixture f=fixture(); Lesson target=privateLesson(f);
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch go=new CountDownLatch(1);
        try {
            Future<Boolean> started=pool.submit(() -> {
                go.await();
                try { quizzes.start(f.student(),f.quiz().id); return true; }
                catch (org.springframework.security.access.AccessDeniedException expected) { return false; }
            });
            Future<Boolean> moved=pool.submit(() -> {
                go.await();
                try { moveQuiz(f,target); return true; }
                catch (IllegalArgumentException expected) { return false; }
            });
            go.countDown();
            assertNotEquals(started.get(20,TimeUnit.SECONDS),moved.get(20,TimeUnit.SECONDS));
        } finally { go.countDown(); pool.shutdownNow(); }
    }

    @Test void refundedPaymentReplayIsIdempotentAndMismatchesStillFail() {
        Fixture f=fixture(); PurchaseOrder order=payosOrder(f); String ref="payos:link-"+order.code;
        payments.confirm(order.id,order.amount,ref,null);
        payments.refundRecorded(f.owner(),order.id,"refund-"+order.code,"Refund requested");
        Enrollment before=db.first(Enrollment.class,"select e from Enrollment e where e.user.id=:id","id",f.student().id).orElseThrow();
        assertEquals(OrderStatus.REFUNDED,payments.confirm(order.id,order.amount,ref,null).status);
        assertThrows(IllegalArgumentException.class,() -> payments.confirm(order.id,order.amount+1,ref,null));
        assertThrows(IllegalArgumentException.class,() -> payments.confirm(order.id,order.amount,ref+"wrong",null));
        Enrollment after=db.get(Enrollment.class,before.id);
        assertEquals(before.active,after.active); assertEquals(before.expiresAt,after.expiresAt);
        assertEquals(before.sourceOrder,after.sourceOrder);
        assertEquals(1,db.count("select count(p) from Payment p where p.order.id=:id","id",order.id));
        assertEquals(1,db.count("select count(m) from MailJob m where m.eventKey=:key","key","payment:"+order.id));
    }

    @Test void signedWebhookReplayAfterRefundReturns200WithoutRestoringAccess() throws Exception {
        Fixture f=fixture(); PurchaseOrder order=payosOrder(f); Gateway g=gateway(properties("payos",true));
        payments.confirm(order.id,order.amount,"payos:link-"+order.code,null);
        payments.refundRecorded(f.owner(),order.id,"refund-"+order.code,"Refund requested");
        g.server().expect(requestTo("https://gateway.test/"+order.code))
                .andRespond(withSuccess(signed(g,data(order,"PAID")),MediaType.APPLICATION_JSON));
        MockMvc webhook=MockMvcBuilders.standaloneSetup(new PayosWebhookController(db,g.service(),properties("payos",true))).build();
        assertEquals(200,webhook.perform(post("/payments/payos/webhook").contentType(MediaType.APPLICATION_JSON)
                .content(signed(g,Map.of("code","00","orderCode",order.code)))).andReturn().getResponse().getStatus());
        assertEquals(OrderStatus.REFUNDED,db.get(PurchaseOrder.class,order.id).status);
        assertFalse(db.first(Enrollment.class,"select e from Enrollment e where e.user.id=:id","id",f.student().id).orElseThrow().active);
        g.server().verify();
    }

    @Test void underpaidKeepsSameOrderAndCanBecomePaid() {
        for (String remote:List.of("UNDERPAID","PENDING")) {
            Fixture f=fixture(); PurchaseOrder order=payosOrder(f); Gateway g=gateway(properties("payos",true));
            Map<String,Object> partial=data(order,remote); partial.put("amountPaid",order.amount/2);
            g.server().expect(requestTo("https://gateway.test/"+order.code)).andRespond(withSuccess(signed(g,partial),MediaType.APPLICATION_JSON));
            g.server().expect(requestTo("https://gateway.test/"+order.code)).andRespond(withSuccess(signed(g,data(order,"PAID")),MediaType.APPLICATION_JSON));
            g.service().reconcile(order);
            PurchaseOrder persisted=db.get(PurchaseOrder.class,order.id);
            assertEquals(OrderStatus.UNDERPAID,persisted.status); assertNotNull(persisted.pendingKey);
            assertEquals(order.id,payments.order(f.student(),f.course().id).id);
            assertEquals(0,db.count("select count(p) from Payment p where p.order.id=:id","id",order.id));
            g.service().deferReconciliation(order.id);
            assertTrue(db.get(PurchaseOrder.class,order.id).nextReconcileAt.isAfter(Instant.now()));
            g.service().reconcile(persisted);
            assertEquals(OrderStatus.PAID,db.get(PurchaseOrder.class,order.id).status);
            g.server().verify();
        }
    }

    @Test void partialAndFailedStatesRenderOnBothScreensAndPartialIsPolled() throws Exception {
        Fixture f=fixture(); PurchaseOrder order=payosOrder(f);
        new TransactionTemplate(manager).executeWithoutResult(tx -> db.get(PurchaseOrder.class,order.id).status=OrderStatus.UNDERPAID);
        var student=mvc.perform(get("/orders/"+order.id).with(user(f.student().email))).andReturn();
        assertEquals(200,student.getResponse().getStatus());
        assertTrue(student.getResponse().getContentAsString().contains("/orders/"+order.id+"/refresh"));
        assertFalse(student.getResponse().getContentAsString().contains("/orders/"+order.id+"/checkout"));
        var screen=mvc.perform(get("/admin/finance").param("status","UNDERPAID").param("code",Long.toString(order.code))
                .with(user(f.owner().email).roles("ADMIN"))).andReturn();
        assertEquals(200,screen.getResponse().getStatus());
        assertTrue(screen.getResponse().getContentAsString().contains("/admin/finance/"+order.id+"/refresh"));
        PayosService gateway=mock(PayosService.class);
        new BackgroundJobs(db,quizzes,mock(OutboxService.class),gateway,properties("manual",true)).reconcile();
        verify(gateway).reconcile(argThat(o -> o.id.equals(order.id)));
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            PurchaseOrder o=db.get(PurchaseOrder.class,order.id); o.status=OrderStatus.FAILED; o.pendingKey=null;
        });
        assertEquals(200,mvc.perform(get("/orders/"+order.id).with(user(f.student().email))).andReturn().getResponse().getStatus());
    }

    @Test void signedFailedWebhookReconcilesEvenWithNonSuccessEventCode() throws Exception {
        Fixture f=fixture(); PurchaseOrder order=payosOrder(f); Gateway g=gateway(properties("payos",true));
        g.server().expect(requestTo("https://gateway.test/"+order.code))
                .andRespond(withSuccess(signed(g,data(order,"FAILED")),MediaType.APPLICATION_JSON));
        MockMvc webhook=MockMvcBuilders.standaloneSetup(new PayosWebhookController(db,g.service(),properties("payos",true))).build();
        assertEquals(200,webhook.perform(post("/payments/payos/webhook").contentType(MediaType.APPLICATION_JSON)
                .content(signed(g,Map.of("code","01","orderCode",order.code)))).andReturn().getResponse().getStatus());
        PurchaseOrder stored=db.get(PurchaseOrder.class,order.id);
        assertEquals(OrderStatus.FAILED,stored.status); assertNull(stored.pendingKey);
        assertTrue(stored.checkoutUrl.isBlank());
        assertNotEquals(order.id,payments.order(f.student(),f.course().id).id);
        g.server().verify();
    }

    @Test void failedCheckoutRecoveryAndPartialRecoveryDoNotRedirect() {
        for (String state:List.of("FAILED","UNDERPAID")) {
            Fixture f=fixture(); PurchaseOrder order=payosOrder(f); Gateway g=gateway(properties("payos",true));
            Map<String,Object> result=data(order,state); if (state.equals("UNDERPAID")) result.put("amountPaid",order.amount/2);
            g.server().expect(requestTo("https://gateway.test")).andRespond(withStatus(HttpStatus.CONFLICT));
            g.server().expect(requestTo("https://gateway.test/"+order.code)).andRespond(withSuccess(signed(g,result),MediaType.APPLICATION_JSON));
            assertEquals("",g.service().link(order));
            assertEquals(OrderStatus.valueOf(state),db.get(PurchaseOrder.class,order.id).status);
            g.server().verify();
        }
    }

    @Test void staleFailureCannotOverwritePaidOrRefundedOrder() {
        for (boolean refund:List.of(false,true)) {
            Fixture f=fixture(); PurchaseOrder stale=payosOrder(f); Gateway g=gateway(properties("payos",true));
            payments.confirm(stale.id,stale.amount,"payos:link-"+stale.code,null);
            if(refund) payments.refundRecorded(f.owner(),stale.id,"refund-"+stale.code,"Requested");
            g.server().expect(requestTo("https://gateway.test/"+stale.code)).andRespond(withSuccess(signed(g,data(stale,"FAILED")),MediaType.APPLICATION_JSON));
            g.service().reconcile(stale);
            assertEquals(refund?OrderStatus.REFUNDED:OrderStatus.PAID,db.get(PurchaseOrder.class,stale.id).status);
            g.server().verify();
        }
    }

    @Test void unknownAndMismatchedGatewayStatesNeverChangeAnOrder() {
        for(String state:List.of("UNRECOGNIZED","FAILED","UNDERPAID")) {
            Fixture f=fixture(); PurchaseOrder order=payosOrder(f); Gateway g=gateway(properties("payos",true));
            Map<String,Object> invalid=data(order,state);
            if(state.equals("FAILED")) invalid.put("orderCode",order.code+1);
            if(state.equals("UNDERPAID")) invalid.put("amountPaid",order.amount+1);
            g.server().expect(requestTo("https://gateway.test/"+order.code)).andRespond(withSuccess(signed(g,invalid),MediaType.APPLICATION_JSON));
            assertThrows(IllegalArgumentException.class,() -> g.service().reconcile(order));
            assertEquals(OrderStatus.PENDING,db.get(PurchaseOrder.class,order.id).status);
            g.server().verify();
        }
    }
}
