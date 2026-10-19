package vn.learnhub.community;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import java.util.*;

@Controller
public class RankingController {
    private final StudyStatsService study;

    public RankingController(StudyStatsService study) {
        this.study = study;
    }

    @GetMapping({"/bang-xep-hang", "/bang-xep-hang/"})
    String ranking(@RequestParam(defaultValue = "week") String period, Model m) {
        if (!Set.of("day", "week", "all").contains(period)) period = "week";
        m.addAttribute("period", period);
        m.addAttribute("ranking", study.ranking(period, 50));
        return "community/leaderboard";
    }
}
