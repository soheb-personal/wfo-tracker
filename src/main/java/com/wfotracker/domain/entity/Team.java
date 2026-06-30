package com.wfotracker.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "team")
@Getter
@Setter
public class Team extends BaseEntity {

    @Column(name = "team_name", nullable = false, length = 100)
    private String teamName;

    @OneToMany(mappedBy = "team")
    private List<User> members = new ArrayList<>();
}
