package com.wfotracker.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "monthly_configuration")
@Getter
@Setter
public class MonthlyConfiguration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @Column(name = "config_month", nullable = false)
    private int month;

    @Column(name = "config_year", nullable = false)
    private int year;

    @Column(name = "working_days", nullable = false)
    private int workingDays;

    @Column(name = "leaves", nullable = false)
    private int leaves = 0;

    @Column(name = "public_holidays", nullable = false)
    private int publicHolidays = 0;

    @Column(name = "exception_days", nullable = false)
    private int exceptionDays = 0;

    @Column(name = "required_office_days", nullable = false)
    private int requiredOfficeDays = 0;
}
