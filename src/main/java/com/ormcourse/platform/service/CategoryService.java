package com.ormcourse.platform.service;

import com.ormcourse.platform.dto.catalog.CategoryRequest;
import com.ormcourse.platform.dto.catalog.CategoryResponse;
import com.ormcourse.platform.entity.Category;
import com.ormcourse.platform.exception.ConflictException;
import com.ormcourse.platform.exception.NotFoundException;
import com.ormcourse.platform.repository.CategoryRepository;
import com.ormcourse.platform.repository.CourseRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;

    public List<CategoryResponse> list() {
        return categoryRepository.findAllByOrderByNameAsc().stream().map(CategoryResponse::from).toList();
    }

    public CategoryResponse get(Long id) {
        return CategoryResponse.from(find(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Category '" + name + "' already exists");
        }

        Category category = categoryRepository.save(new Category(name, request.description()));
        log.info("Created category id={} name={}", category.getId(), name);
        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = find(id);
        String name = request.name().trim();

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Category '" + name + "' already exists");
        }

        category.setName(name);
        category.setDescription(request.description());
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = find(id);

        if (courseRepository.existsByCategoryId(id)) {
            throw new ConflictException("Category " + id + " has courses and cannot be deleted");
        }

        categoryRepository.delete(category);
        log.info("Deleted category id={}", id);
    }

    Category find(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> NotFoundException.of("Category", id));
    }
}
