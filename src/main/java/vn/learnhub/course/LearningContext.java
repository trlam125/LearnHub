package vn.learnhub.course;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class LearningContext {
    private final HttpServletRequest request;
    private LearningLanguage selected;

    public LearningContext(HttpServletRequest request) {
        this.request = request;
    }

    public LearningLanguage current() {
        if (selected == null) {
            String code = request.getParameter("lang");
            if (code == null && request.getRequestURI().startsWith("/hsk")) code = "zh";
            var session = request.getSession(false);
            if (code == null && session != null) code = (String) session.getAttribute("learningLanguage");
            selected = "en".equals(code) ? LearningLanguage.ENGLISH : LearningLanguage.CHINESE;
            if ("en".equals(code) || "zh".equals(code))
                request.getSession().setAttribute("learningLanguage", selected.getCode());
        }
        return selected;
    }

    public String code() {
        return current().getCode();
    }

    public void select(String code, Model model) {
        selected = LearningLanguage.fromCode(code);
        request.getSession().setAttribute("learningLanguage", code);
        populate(model);
    }

    public void populate(Model model) {
        LearningLanguage language = current();
        model.addAttribute("languageCode", language.getCode());
        model.addAttribute("languageName", language.getLabel());
        model.addAttribute("languageVoice", language.getVoice());
        model.addAttribute("languageSymbol", language.getSymbol());
        model.addAttribute("isEnglish", language == LearningLanguage.ENGLISH);
        model.addAttribute("languagePath", "/lo-trinh?lang=" + language.getCode());
    }
}
