package com.ormcourse.platform.service;

import com.ormcourse.platform.dto.course.ModuleRequest;
import com.ormcourse.platform.dto.course.ModuleResponse;
import com.ormcourse.platform.entity.Course;
import com.ormcourse.platform.entity.CourseModule;
import com.ormcourse.platform.exception.NotFoundException;
import com.ormcourse.platform.repository.CourseModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModuleService {

    private final CourseModuleRepository moduleRepository;
    private final CourseService courseService;

    @Transactional
    public ModuleResponse addModule(Long courseId, ModuleRequest request) {
        Course course = courseService.find(courseId);
        int nextOrder = moduleRepository.findMaxOrderIndex(courseId) + 1;
        CourseModule module = CourseStructureFactory.buildModule(request, nextOrder);
        course.addModule(module);
        moduleRepository.save(module);
        log.info("Added module id={} to course id={}", module.getId(), courseId);
        return ModuleResponse.from(module);
    }

    public ModuleResponse get(Long moduleId) {
        return ModuleResponse.from(find(moduleId));
    }

    @Transactional
    public ModuleResponse update(Long moduleId, ModuleRequest request) {
        CourseModule module = find(moduleId);
        module.setTitle(request.title());
        module.setDescription(request.description());

        if (request.orderIndex() != null) {
            module.setOrderIndex(request.orderIndex());
        }

        return ModuleResponse.from(module);
    }

    @Transactional
    public void delete(Long moduleId) {
        CourseModule module = find(moduleId);
        module.getCourse().removeModule(module);
        log.info("Deleted module id={}", moduleId);
    }

    CourseModule find(Long moduleId) {
        return moduleRepository.findWithCourseById(moduleId).orElseThrow(() -> NotFoundException.of("Module", moduleId));
    }
}
