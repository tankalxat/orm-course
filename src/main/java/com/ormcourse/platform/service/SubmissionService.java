package com.ormcourse.platform.service;

import com.ormcourse.platform.dto.assignment.GradeRequest;
import com.ormcourse.platform.dto.assignment.SubmissionResponse;
import com.ormcourse.platform.dto.assignment.SubmitRequest;
import com.ormcourse.platform.entity.Assignment;
import com.ormcourse.platform.entity.Submission;
import com.ormcourse.platform.entity.SubmissionStatus;
import com.ormcourse.platform.entity.User;
import com.ormcourse.platform.event.SubmissionGradedEvent;
import com.ormcourse.platform.exception.BusinessRuleException;
import com.ormcourse.platform.exception.ConflictException;
import com.ormcourse.platform.exception.NotFoundException;
import com.ormcourse.platform.repository.SubmissionRepository;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentService assignmentService;
    private final UserLookup userLookup;
    private final EnrollmentGuard enrollmentGuard;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public SubmissionResponse submit(Long assignmentId, SubmitRequest request) {
        Assignment assignment = assignmentService.find(assignmentId);
        User student = userLookup.getStudent(request.studentId());
        enrollmentGuard.requireActiveEnrollment(student.getId(), assignment.getLesson().getModule().getCourse().getId());

        if (submissionRepository.existsByAssignmentIdAndStudentId(assignmentId, student.getId())) {
            throw new ConflictException("Student " + student.getId() + " has already submitted assignment " + assignmentId);
        }

        Submission submission = submissionRepository.save(new Submission(assignment, student, request.content(), clock.instant()));
        log.info("Student id={} submitted assignment id={} late={}", student.getId(), assignmentId, submission.isLate());
        return SubmissionResponse.from(submission);
    }

    @Transactional
    public SubmissionResponse grade(Long submissionId, GradeRequest request) {
        Submission submission = find(submissionId);
        int maxScore = submission.getAssignment().getMaxScore();

        if (request.score() > maxScore) {
            throw new BusinessRuleException("Score " + request.score() + " exceeds max score " + maxScore);
        }

        submission.grade(request.score(), request.feedback(), clock.instant());
        eventPublisher.publishEvent(new SubmissionGradedEvent(
                submission.getStudent().getId(),
                submission.getAssignment().getTitle(),
                request.score(),
                maxScore
        ));
        log.info("Graded submission id={} score={}/{}", submissionId, request.score(), maxScore);
        return SubmissionResponse.from(submission);
    }

    public SubmissionResponse get(Long submissionId) {
        return SubmissionResponse.from(find(submissionId));
    }

    public List<SubmissionResponse> listByAssignment(Long assignmentId) {
        assignmentService.find(assignmentId);
        return submissionRepository.findByAssignmentIdOrderBySubmittedAtAsc(assignmentId).stream()
                .map(SubmissionResponse::from)
                .toList();
    }

    public List<SubmissionResponse> listByStudent(Long studentId) {
        userLookup.getStudent(studentId);
        return submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
                .map(SubmissionResponse::from)
                .toList();
    }

    @Transactional
    public void withdraw(Long submissionId) {
        Submission submission = find(submissionId);

        if (submission.getStatus() == SubmissionStatus.GRADED) {
            throw new ConflictException("Submission " + submissionId + " is already graded and cannot be withdrawn");
        }

        submissionRepository.delete(submission);
        log.info("Withdrawn submission id={}", submissionId);
    }

    private Submission find(Long submissionId) {
        return submissionRepository.findDetailedById(submissionId).orElseThrow(() -> NotFoundException.of("Submission", submissionId));
    }
}
