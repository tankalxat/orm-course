package com.ormcourse.platform.dto.analytics;

public record LeaderboardEntry(Long studentId, String studentName, Long totalScore, Long gradedSubmissions) {
}
