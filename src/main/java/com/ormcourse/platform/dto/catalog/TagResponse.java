package com.ormcourse.platform.dto.catalog;

import com.ormcourse.platform.entity.Tag;

public record TagResponse(Long id, String name) {

    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
