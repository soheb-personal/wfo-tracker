package com.wfotracker.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role extends BaseEntity implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;
}
