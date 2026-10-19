package vn.learnhub.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findBySlugAndPublishedTrue(String slug);

    List<Course> findByPublishedTrueAndKindAndLanguageOrderByIdDesc(String kind, String language);
}
