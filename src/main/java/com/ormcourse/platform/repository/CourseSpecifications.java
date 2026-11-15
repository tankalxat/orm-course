package com.ormcourse.platform.repository;

import com.ormcourse.platform.entity.Course;
import com.ormcourse.platform.entity.CourseStatus;
import com.ormcourse.platform.entity.Tag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

public final class CourseSpecifications {

    private CourseSpecifications() {
    }

    public static Specification<Course> anyCourse() {
        return (root, query, cb) -> null;
    }

    public static Specification<Course> hasId(Long id) {
        return (root, query, cb) -> cb.equal(root.get("id"), id);
    }

    public static Specification<Course> visibleTo(Long teacherId) {
        return (root, query, cb) -> {
            Predicate notDraft = cb.notEqual(root.get("status"), CourseStatus.DRAFT);

            if (teacherId == null) {
                return notDraft;
            }

            return cb.or(notDraft, cb.equal(root.get("teacher").get("id"), teacherId));
        };
    }

    public static Specification<Course> hasCategory(Long categoryId) {
        return (root, query, cb) -> categoryId == null ? null : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Course> hasTeacher(Long teacherId) {
        return (root, query, cb) -> teacherId == null ? null : cb.equal(root.get("teacher").get("id"), teacherId);
    }

    public static Specification<Course> hasStatus(CourseStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Course> titleContains(String text) {
        return (root, query, cb) -> text == null || text.isBlank()
                ? null
                : cb.like(cb.lower(root.get("title")), "%" + text.trim().toLowerCase() + "%");
    }

    public static Specification<Course> hasTag(String tagName) {
        return (root, query, cb) -> {
            if (tagName == null || tagName.isBlank()) {
                return null;
            }

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Course> subRoot = subquery.from(Course.class);
            Join<Course, Tag> tags = subRoot.join("tags");
            subquery.select(subRoot.get("id")).where(cb.equal(cb.lower(tags.get("name")), tagName.trim().toLowerCase()));
            return root.get("id").in(subquery);
        };
    }
}
