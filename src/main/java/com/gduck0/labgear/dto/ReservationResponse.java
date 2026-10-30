
package com.gduck0.labgear.dto;

import java.time.LocalDate;

public class ReservationResponse {

    private final Long id;
    private final int equipmentId;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public ReservationResponse(
            Long id, int equipmentId,
            LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.equipmentId = equipmentId;
        this.startDate = startDate;
        this.endDate = endDate;
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
}
