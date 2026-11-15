package com.ormcourse.platform.repository;

import com.ormcourse.platform.entity.Role;
import com.ormcourse.platform.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    Optional<User> findByEmailIgnoreCase(String email);

    Page<User> findByRole(Role role, Pageable pageable);

    List<User> findByRole(Role role);
}
