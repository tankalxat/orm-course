package com.ormcourse.platform.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    ENROLLED("Запись на курс"),
    NEW_ASSIGNMENT("Новое задание"),
    SUBMISSION_GRADED("Работа оценена"),
    QUIZ_COMPLETED("Результат теста");

    private final String subject;
}
