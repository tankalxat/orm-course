package com.ormcourse.platform.web;

import java.net.URI;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

final class Locations {

    private Locations() {
    }

    static URI of(String path, Object... variables) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(path).buildAndExpand(variables).toUri();
    }
}
