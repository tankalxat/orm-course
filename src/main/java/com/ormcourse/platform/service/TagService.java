package com.ormcourse.platform.service;

import com.ormcourse.platform.dto.catalog.TagRequest;
import com.ormcourse.platform.dto.catalog.TagResponse;
import com.ormcourse.platform.entity.Course;
import com.ormcourse.platform.entity.Tag;
import com.ormcourse.platform.exception.ConflictException;
import com.ormcourse.platform.exception.NotFoundException;
import com.ormcourse.platform.repository.TagRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;

    public List<TagResponse> list() {
        return tagRepository.findAllByOrderByNameAsc().stream().map(TagResponse::from).toList();
    }

    @Transactional
    public TagResponse create(TagRequest request) {
        String name = request.name().trim();

        if (tagRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Tag '" + name + "' already exists");
        }

        Tag tag = tagRepository.save(new Tag(name));
        log.info("Created tag id={} name={}", tag.getId(), name);
        return TagResponse.from(tag);
    }

    @Transactional
    public void delete(Long id) {
        Tag tag = tagRepository.findById(id).orElseThrow(() -> NotFoundException.of("Tag", id));

        for (Course course : List.copyOf(tag.getCourses())) {
            course.removeTag(tag);
        }

        tagRepository.delete(tag);
        log.info("Deleted tag id={}", id);
    }
}
