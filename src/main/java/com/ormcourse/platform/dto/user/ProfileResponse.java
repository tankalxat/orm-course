package com.ormcourse.platform.dto.user;

import com.ormcourse.platform.entity.Profile;

public record ProfileResponse(Long userId, String bio, String avatarUrl, String city, String website) {

    public static ProfileResponse from(Long userId, Profile profile) {
        return new ProfileResponse(userId, profile.getBio(), profile.getAvatarUrl(), profile.getCity(), profile.getWebsite());
    }
}
