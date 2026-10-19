package com.gduck0.labgear.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.AssertTrue;

public class ReservationCreateRequest {
    @Positive
    private int id;

    @Positive
    private int studentId;

    @Positive
    private int equipmentId;

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

    protected ReservationCreateRequest() {
    }

    public ReservationCreateRequest(int id, int studentId, int equipmentId, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.studentId = studentId;
        this.equipmentId = equipmentId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public int getId() {
        return id;
    }

    public int getStudentId() {
        return studentId;
    }

    public int getEquipmentId() {
        return equipmentId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
