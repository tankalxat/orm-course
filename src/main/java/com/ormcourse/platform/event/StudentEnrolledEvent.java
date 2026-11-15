package com.ormcourse.platform.event;

public record StudentEnrolledEvent(Long studentId, String courseTitle) {
}
