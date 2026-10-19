package vn.learnhub.community;

import vn.learnhub.shared.persistence.Store;
import vn.learnhub.shared.study.StudyRules;
import vn.learnhub.user.Account;
import vn.learnhub.vocabulary.WordProgress;

import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class StudyStatsService {
    private final Store db;

    public StudyStatsService(Store db) {
        this.db = db;
    }

    public record Day(String label, boolean active, boolean today) {
    }

    public record Stats(int streak, long xp, long todayCount, long saved, long reviewed, List<Day> week) {
    }

    public record Ranking(Long userId, String name, long xp) {
    }

    public Stats stats(Account user) {
        if (user == null) return new Stats(0, 0, 0, 0, 0, List.of());
        LocalDate today = LocalDate.now(StudyRules.ZONE);
        var days = new HashSet<>(db.list(LocalDate.class, "select distinct a.day from Activity a where a.user.id=:u", "u", user.id));
        Long xp = db.query(Long.class, "select sum(a.xp) from Activity a where a.user.id=:u", "u", user.id).getSingleResult();
        long count = db.count("select count(a) from Activity a where a.user.id=:u and a.day=:d", "u", user.id, "d", today);
        long saved = db.count("select count(p) from WordProgress p where p.user.id=:u and p.saved=true", "u", user.id);
        long reviewed = db.count("select count(p) from WordProgress p where p.user.id=:u and p.lastReviewed is not null", "u", user.id);
        List<Day> week = new ArrayList<>();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        String[] labels = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            week.add(new Day(labels[i], days.contains(d), today.equals(d)));
        }
        return new Stats(StudyRules.streak(days, today), xp == null ? 0 : xp, count, saved, reviewed, week);
    }

    public List<Ranking> ranking(String period, int limit) {
        LocalDate today = LocalDate.now(StudyRules.ZONE);
        LocalDate start = period.equals("day") ? today : period.equals("week") ? today.minusDays(today.getDayOfWeek().getValue() - 1) : LocalDate.of(1970, 1, 1);
        return db.query(Object[].class, "select a.user.id,a.user.publicName,sum(a.xp) from Activity a where a.user.rankingVisible=true and a.user.active=true and a.day>=:start group by a.user.id,a.user.publicName having sum(a.xp)>0 order by sum(a.xp) desc,a.user.id", "start", start)
                .setMaxResults(limit).getResultList().stream().map(r -> new Ranking((Long) r[0], (String) r[1], ((Number) r[2]).longValue())).toList();
    }
}
