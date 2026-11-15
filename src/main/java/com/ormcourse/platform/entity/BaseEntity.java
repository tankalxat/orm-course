package com.ormcourse.platform.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.Hibernate;

@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }

        Long otherId = ((BaseEntity) other).getId();
        return getId() != null && getId().equals(otherId);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
