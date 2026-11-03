package com.gduck0.labgear.dto;

import com.gduck0.labgear.entity.ReservationStatus;

import java.time.LocalDate;

public class ReservationResponse {

    private final Long id;
    private final String accountEmail;
    private final int equipmentId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final ReservationStatus status;

    public ReservationResponse(
            Long id,
            String accountEmail,
            int equipmentId,
            LocalDate startDate,
            LocalDate endDate,
            ReservationStatus status) {
        this.id = id;
        this.accountEmail = accountEmail;
        this.equipmentId = equipmentId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getAccountEmail() {
        return accountEmail;
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
