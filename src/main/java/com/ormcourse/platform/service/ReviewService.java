package com.ormcourse.platform.service;

import com.ormcourse.platform.dto.review.ReviewRequest;
import com.ormcourse.platform.dto.review.ReviewResponse;
import com.ormcourse.platform.entity.Course;
import com.ormcourse.platform.entity.CourseReview;
import com.ormcourse.platform.entity.User;
import com.ormcourse.platform.exception.ConflictException;
import com.ormcourse.platform.exception.NotFoundException;
import com.ormcourse.platform.repository.CourseReviewRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final CourseReviewRepository reviewRepository;
    private final CourseService courseService;
    private final UserLookup userLookup;
    private final EnrollmentGuard enrollmentGuard;

    @Transactional
    public ReviewResponse add(Long courseId, ReviewRequest request) {
        Course course = courseService.find(courseId);
        User student = userLookup.getStudent(request.studentId());
        enrollmentGuard.requireEnrollment(student.getId(), courseId);

        if (reviewRepository.existsByCourseIdAndStudentId(courseId, student.getId())) {
            throw new ConflictException("Student " + student.getId() + " has already reviewed course " + courseId);
        }

        CourseReview review = reviewRepository.save(new CourseReview(course, student, request.rating(), request.comment()));
        log.info("Student id={} reviewed course id={} rating={}", student.getId(), courseId, request.rating());
        return ReviewResponse.from(review);
    }

    public List<ReviewResponse> list(Long courseId) {
        courseService.find(courseId);
        return reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId).stream().map(ReviewResponse::from).toList();
    }

    @Transactional
    public void delete(Long reviewId) {
        CourseReview review = reviewRepository.findById(reviewId).orElseThrow(() -> NotFoundException.of("Review", reviewId));
        reviewRepository.delete(review);
        log.info("Deleted review id={}", reviewId);
    }
}
