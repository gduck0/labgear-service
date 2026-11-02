package com.gduck0.labgear.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public class ReservationCreateRequest {

    @Positive private int equipmentId;

    @NotNull private LocalDate startDate;

    @NotNull private LocalDate endDate;

    public ReservationCreateRequest() {}

    @AssertTrue
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !startDate.isAfter(endDate);
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

    public void setEquipmentId(int equipmentId) {
        this.equipmentId = equipmentId;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
