package com.gduck0.labgear.dto;

import java.time.LocalDate;

public class EquipmentReservationResponse {
    private int id;
    private int studentId;
    private int equipmentItemId;
    private LocalDate startDate;
    private LocalDate endDate;

    public EquipmentReservationResponse(int id, int studentId, int equipmentItemId, LocalDate startDate, LocalDate endDate) {
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
