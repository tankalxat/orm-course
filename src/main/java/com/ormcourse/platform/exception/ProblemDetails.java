package com.ormcourse.platform.exception;

import java.net.URI;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class ProblemDetails {

    public static final String ERRORS_PROPERTY = "errors";
    public static final String TIMESTAMP_PROPERTY = "timestamp";

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatus status, String title, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(title);
        detail.setType(URI.create("about:blank"));
        detail.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return detail;
    }
}
