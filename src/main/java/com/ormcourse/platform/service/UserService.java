package com.ormcourse.platform.service;

import com.ormcourse.platform.dto.common.PageResponse;
import com.ormcourse.platform.dto.user.ProfileRequest;
import com.ormcourse.platform.dto.user.ProfileResponse;
import com.ormcourse.platform.dto.user.UserCreateRequest;
import com.ormcourse.platform.dto.user.UserResponse;
import com.ormcourse.platform.dto.user.UserUpdateRequest;
import com.ormcourse.platform.entity.Profile;
import com.ormcourse.platform.entity.Role;
import com.ormcourse.platform.entity.User;
import com.ormcourse.platform.exception.ConflictException;
import com.ormcourse.platform.exception.NotFoundException;
import com.ormcourse.platform.repository.CourseRepository;
import com.ormcourse.platform.repository.ProfileRepository;
import com.ormcourse.platform.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final ProfileRepository profileRepository;
    private final UserLookup userLookup;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("User with email " + email + " already exists");
        }

        User user = new User(request.name().trim(), email, request.role(), passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        userRepository.save(user);
        log.info("Created user id={} role={}", user.getId(), user.getRole());
        return UserResponse.from(user);
    }

    public UserResponse getById(Long id) {
        return UserResponse.from(userLookup.getUser(id));
    }

    public PageResponse<UserResponse> list(Role role, Pageable pageable) {
        Page<User> page = role == null ? userRepository.findAll(pageable) : userRepository.findByRole(role, pageable);
        return PageResponse.from(page, UserResponse::from);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = userLookup.getUser(id);
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("User with email " + email + " already exists");
        }

        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        log.info("Updated user id={}", id);
        return UserResponse.from(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = userLookup.getUser(id);

        if (courseRepository.existsByTeacherId(id)) {
            throw new ConflictException("User " + id + " teaches courses; reassign them before deleting the user");
        }

        userRepository.delete(user);
        log.info("Deleted user id={}", id);
    }

    public ProfileResponse getProfile(Long userId) {
        userLookup.getUser(userId);
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Profile for user " + userId + " not found"));
        return ProfileResponse.from(userId, profile);
    }

    @Transactional
    public ProfileResponse upsertProfile(Long userId, ProfileRequest request) {
        User user = userLookup.getUser(userId);
        Profile profile = profileRepository.findById(userId).orElseGet(() -> profileRepository.save(new Profile(user)));
        profile.setBio(request.bio());
        profile.setAvatarUrl(request.avatarUrl());
        profile.setCity(request.city());
        profile.setWebsite(request.website());
        return ProfileResponse.from(userId, profile);
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
