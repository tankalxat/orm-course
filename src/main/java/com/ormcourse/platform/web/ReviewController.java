package com.ormcourse.platform.web;

import com.ormcourse.platform.dto.review.ReviewRequest;
import com.ormcourse.platform.dto.review.ReviewResponse;
import com.ormcourse.platform.security.AccessPolicy;
import com.ormcourse.platform.service.CourseService;
import com.ormcourse.platform.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Отзывы о курсах")
public class ReviewController {

    private final ReviewService reviewService;
    private final CourseService courseService;
    private final AccessPolicy access;

    @GetMapping("/courses/{courseId}/reviews")
    @Operation(summary = "Отзывы о курсе")
    public List<ReviewResponse> list(
            @PathVariable Long courseId,
            @CurrentSecurityContext(expression = "authentication") Authentication authentication
    ) {
        courseService.requireVisible(courseId, access.visibleCourses(authentication));
        return reviewService.list(courseId);
    }

    @PostMapping("/courses/{courseId}/reviews")
    @Operation(summary = "Оставить отзыв (только записанный студент, один раз)")
    @PreAuthorize("@access.isSelf(authentication, #request.studentId())")
    public ResponseEntity<ReviewResponse> add(@PathVariable Long courseId, @Valid @RequestBody ReviewRequest request) {
        ReviewResponse created = reviewService.add(courseId, request);
        return ResponseEntity.created(Locations.of("/api/courses/{id}/reviews", courseId)).body(created);
    }

    @DeleteMapping("/reviews/{id}")
    @Operation(summary = "Удалить отзыв")
    @PreAuthorize("@access.ownsReview(authentication, #id)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
