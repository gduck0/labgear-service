package com.gduck0.labgear.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

@Entity
@Table(name = "equipment_reservations")
public class Reservation {
    @Id
    private int id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "equipment_item_id")
    private Equipment equipment;
    private LocalDate startDate;
    private LocalDate endDate;

    public Reservation(int id, Student student, Equipment equipment, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.student = student;
        this.equipment = equipment;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    protected Reservation() {
    }

    public int getId() {
        return this.id;
    }

    public Student getStudent() {
        return this.student;
    }

    public Equipment getEquipment() {
        return this.equipment;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}