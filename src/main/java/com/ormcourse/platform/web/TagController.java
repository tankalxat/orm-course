package com.ormcourse.platform.web;

import com.ormcourse.platform.dto.catalog.TagRequest;
import com.ormcourse.platform.dto.catalog.TagResponse;
import com.ormcourse.platform.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tags")
@Tag(name = "Tags", description = "Теги курсов")
public class TagController {

    private final TagService tagService;

    @GetMapping
    @Operation(summary = "Все теги")
    public List<TagResponse> list() {
        return tagService.list();
    }

    @PostMapping
    @Operation(summary = "Создать тег")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<TagResponse> create(@Valid @RequestBody TagRequest request) {
        TagResponse created = tagService.create(request);
        return ResponseEntity.created(Locations.of("/api/tags/{id}", created.id())).body(created);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить тег и отвязать его от курсов")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
