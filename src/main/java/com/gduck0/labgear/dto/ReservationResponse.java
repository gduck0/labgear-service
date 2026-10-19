package com.gduck0.labgear.dto;

import java.time.LocalDate;

public class ReservationResponse {
    private int id;
    private int studentId;
    private int equipmentId;
    private LocalDate startDate;
    private LocalDate endDate;

    public ReservationResponse(int id, int studentId, int equipmentId, LocalDate startDate, LocalDate endDate) {
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
