package com.ormcourse.platform.dto.user;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record ProfileRequest(
        @Size(max = 2000) String bio,
        @URL @Size(max = 500) String avatarUrl,
        @Size(max = 100) String city,
        @URL @Size(max = 255) String website
) {
}
