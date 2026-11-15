package com.ormcourse.platform.service;

import com.ormcourse.platform.dto.auth.LoginRequest;
import com.ormcourse.platform.dto.auth.RegisterRequest;
import com.ormcourse.platform.dto.auth.TokenResponse;
import com.ormcourse.platform.dto.user.UserCreateRequest;
import com.ormcourse.platform.dto.user.UserResponse;
import com.ormcourse.platform.entity.Role;
import com.ormcourse.platform.entity.User;
import com.ormcourse.platform.repository.UserRepository;
import com.ormcourse.platform.security.JwtTokenService;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final UserLookup userLookup;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final String unknownUserHash;

    public AuthService(
            UserRepository userRepository,
            UserService userService,
            UserLookup userLookup,
            PasswordEncoder passwordEncoder,
            JwtTokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.userLookup = userLookup;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.unknownUserHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public TokenResponse login(LoginRequest request) {
        Optional<User> user = userRepository.findByEmailIgnoreCase(UserService.normalizeEmail(request.email()));
        String hash = user.map(User::getPasswordHash).orElse(unknownUserHash);
        boolean matches = passwordEncoder.matches(request.password(), hash);

        if (user.isEmpty() || !matches) {
            throw new BadCredentialsException("Invalid email or password");
        }

        log.info("User id={} logged in", user.get().getId());
        return tokenService.issue(user.get());
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        UserCreateRequest student = new UserCreateRequest(
                request.name(),
                request.email(),
                request.password(),
                Role.STUDENT,
                request.phone()
        );
        return userService.create(student);
    }

    public UserResponse currentUser(Long userId) {
        return UserResponse.from(userLookup.getUser(userId));
    }
}
