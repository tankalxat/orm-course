package com.ormcourse.platform.bootstrap;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.demo-data")
public record DemoDataProperties(boolean enabled, @NotBlank @Size(min = 8, max = 72) String password) {
}
