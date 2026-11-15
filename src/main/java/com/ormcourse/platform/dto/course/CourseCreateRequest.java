package com.ormcourse.platform.dto.course;

import com.ormcourse.platform.entity.CourseStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record CourseCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        @NotNull Long categoryId,
        @NotNull Long teacherId,
        @Positive Integer durationHours,
        LocalDate startDate,
        CourseStatus status,
        Set<@NotNull Long> tagIds,
        List<@Valid ModuleRequest> modules
) {

    public Set<Long> tagIdsOrEmpty() {
        return tagIds == null ? Set.of() : tagIds;
    }

    public List<ModuleRequest> modulesOrEmpty() {
        return modules == null ? List.of() : modules;
    }
}
