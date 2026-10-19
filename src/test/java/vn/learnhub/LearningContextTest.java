package vn.learnhub;

import vn.learnhub.course.LearningContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.*;

class LearningContextTest {
    @Test
    void englishPreferencePersistsBetweenRequests() {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest first = new MockHttpServletRequest();
        first.setSession(session);
        first.setParameter("lang", "en");
        assertEquals("en", new LearningContext(first).code());
        MockHttpServletRequest next = new MockHttpServletRequest();
        next.setSession(session);
        next.setRequestURI("/lo-trinh");
        assertEquals("en", new LearningContext(next).code());
    }

    @Test
    void legacyHskRouteSelectsChineseAndDirectContentCanSelectEnglish() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/hsk");
        request.getSession().setAttribute("learningLanguage", "en");
        LearningContext context = new LearningContext(request);
        assertEquals("zh", context.code());
        ExtendedModelMap model = new ExtendedModelMap();
        context.select("en", model);
        assertEquals("en", model.get("languageCode"));
        assertEquals("en-US", model.get("languageVoice"));
        assertEquals("en", request.getSession().getAttribute("learningLanguage"));
    }

    @Test
    void unknownPreferenceFallsBackToChinese() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("lang", "unexpected");
        assertEquals("zh", new LearningContext(request).code());
    }
}
