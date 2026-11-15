package com.ormcourse.platform.dto.enrollment;

import com.ormcourse.platform.entity.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;

public record EnrollmentStatusRequest(@NotNull EnrollmentStatus status) {
}
