package com.gduck0.labgear.dto;

import com.gduck0.labgear.entity.ReservationStatus;

import java.time.LocalDate;

public class ReservationResponse {

    private final Long id;
    private final int equipmentId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final ReservationStatus status;

    public ReservationResponse(
            Long id,
            int equipmentId,
            LocalDate startDate,
            LocalDate endDate,
            ReservationStatus status) {
        this.id = id;
        this.equipmentId = equipmentId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public Long getId() {
        return id;
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

    public ReservationStatus getStatus() {
        return status;
    }
}
