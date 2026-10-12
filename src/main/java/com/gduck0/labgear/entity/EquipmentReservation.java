package com.gduck0.labgear.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

@Entity
@Table(name = "equipment_reservations")
public class EquipmentReservation {
    @Id
    private int id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "equipment_item_id")
    private EquipmentItem equipmentItem;
    private LocalDate startDate;
    private LocalDate endDate;

    public EquipmentReservation(int id, Student student, EquipmentItem equipmentItem, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.student = student;
        this.equipmentItem = equipmentItem;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    protected EquipmentReservation() {
    }

    public int getId() {
        return this.id;
    }

    public Student getStudent() {
        return this.student;
    }

    public EquipmentItem getEquipmentItem() {
        return this.equipmentItem;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}