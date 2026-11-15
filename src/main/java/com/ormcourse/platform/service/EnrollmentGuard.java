package com.ormcourse.platform.service;

import com.ormcourse.platform.entity.EnrollmentStatus;
import com.ormcourse.platform.exception.BusinessRuleException;
import com.ormcourse.platform.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnrollmentGuard {

    private final EnrollmentRepository enrollmentRepository;

    public void requireActiveEnrollment(Long studentId, Long courseId) {
        if (!enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(studentId, courseId, EnrollmentStatus.ACTIVE)) {
            throw new BusinessRuleException("Student " + studentId + " has no active enrollment in course " + courseId);
        }
    }

    public void requireEnrollment(Long studentId, Long courseId) {
        if (!enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new BusinessRuleException("Student " + studentId + " is not enrolled in course " + courseId);
        }
    }
}
