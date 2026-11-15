package com.ormcourse.platform.repository;

import com.ormcourse.platform.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
}
