package com.ormcourse.platform.web;

import com.ormcourse.platform.dto.analytics.CoursePopularity;
import com.ormcourse.platform.dto.analytics.CourseStatisticsResponse;
import com.ormcourse.platform.dto.analytics.LeaderboardEntry;
import com.ormcourse.platform.dto.analytics.StudentProgressResponse;
import com.ormcourse.platform.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Успеваемость, рейтинги и статистика")
public class AnalyticsController {

    private static final int MAX_LIMIT = 100;

    private final AnalyticsService analyticsService;

    @GetMapping("/courses/{courseId}")
    @Operation(summary = "Статистика курса: записи, отзывы, оценки, тесты")
    @PreAuthorize("@access.canManageCourse(authentication, #courseId)")
    public CourseStatisticsResponse courseStatistics(@PathVariable Long courseId) {
        return analyticsService.courseStatistics(courseId);
    }

    @GetMapping("/courses/{courseId}/leaderboard")
    @Operation(summary = "Рейтинг студентов курса по сумме баллов за задания")
    public List<LeaderboardEntry> leaderboard(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_LIMIT) int limit
    ) {
        return analyticsService.leaderboard(courseId, limit);
    }

    @GetMapping("/courses/popular")
    @Operation(summary = "Самые популярные курсы (без черновиков)")
    public List<CoursePopularity> popular(@RequestParam(defaultValue = "5") @Min(1) @Max(MAX_LIMIT) int limit) {
        return analyticsService.popularCourses(limit);
    }

    @GetMapping("/students/{studentId}/progress")
    @Operation(summary = "Успеваемость студента по всем курсам")
    @PreAuthorize("@access.isSelfOrStaff(authentication, #studentId)")
    public StudentProgressResponse studentProgress(@PathVariable Long studentId) {
        return analyticsService.studentProgress(studentId);
    }
}
