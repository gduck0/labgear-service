package com.gduck0.labgear.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.AssertTrue;

public class EquipmentReservationCreateRequest {
    @Positive
    private int id;

    @Positive
    private int studentId;

    @Positive
    private int equipmentItemId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @AssertTrue
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true;
        }
        if (startDate.isAfter(endDate)) {
            return false;
        }
        return true;
    }

    protected EquipmentReservationCreateRequest() {
    }

    public EquipmentReservationCreateRequest(int id, int studentId, int equipmentItemId, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.studentId = studentId;
        this.equipmentItemId = equipmentItemId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public int getId() {
        return id;
    }

    public int getStudentId() {
        return studentId;
    }

    public int getEquipmentItemId() {
        return equipmentItemId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
