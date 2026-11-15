package com.ormcourse.platform.web;

import com.ormcourse.platform.dto.assignment.StudentAssignmentResponse;
import com.ormcourse.platform.dto.assignment.SubmissionResponse;
import com.ormcourse.platform.dto.enrollment.EnrollmentResponse;
import com.ormcourse.platform.dto.quiz.QuizSubmissionResponse;
import com.ormcourse.platform.service.AssignmentService;
import com.ormcourse.platform.service.EnrollmentService;
import com.ormcourse.platform.service.QuizAttemptService;
import com.ormcourse.platform.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{studentId}")
@PreAuthorize("@access.isSelfOrStaff(authentication, #studentId)")
@Tag(name = "Students", description = "Учебная активность студента")
public class StudentController {

    private final EnrollmentService enrollmentService;
    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final QuizAttemptService quizAttemptService;

    @GetMapping("/enrollments")
    @Operation(summary = "Курсы, на которые записан студент")
    public List<EnrollmentResponse> enrollments(@PathVariable Long studentId) {
        return enrollmentService.listCourses(studentId);
    }

    @GetMapping("/assignments")
    @Operation(summary = "Задания из курсов студента вместе с его решениями")
    public List<StudentAssignmentResponse> assignments(@PathVariable Long studentId) {
        return assignmentService.listForStudent(studentId);
    }

    @GetMapping("/submissions")
    @Operation(summary = "Отправленные решения студента и оценки")
    public List<SubmissionResponse> submissions(@PathVariable Long studentId) {
        return submissionService.listByStudent(studentId);
    }

    @GetMapping("/quiz-submissions")
    @Operation(summary = "Результаты тестов студента")
    public List<QuizSubmissionResponse> quizSubmissions(@PathVariable Long studentId) {
        return quizAttemptService.listByStudent(studentId);
    }
}
