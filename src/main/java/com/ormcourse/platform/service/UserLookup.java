package com.ormcourse.platform.service;

import com.ormcourse.platform.entity.Role;
import com.ormcourse.platform.entity.User;
import com.ormcourse.platform.exception.BusinessRuleException;
import com.ormcourse.platform.exception.NotFoundException;
import com.ormcourse.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserLookup {

    private final UserRepository userRepository;

    public User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> NotFoundException.of("User", id));
    }

    public User getStudent(Long id) {
        return getWithRole(id, Role.STUDENT);
    }

    public User getTeacher(Long id) {
        return getWithRole(id, Role.TEACHER);
    }

    private User getWithRole(Long id, Role role) {
        User user = getUser(id);

        if (!user.hasRole(role)) {
            throw new BusinessRuleException("User " + id + " must have role " + role + " but has " + user.getRole());
        }

        return user;
    }
}
