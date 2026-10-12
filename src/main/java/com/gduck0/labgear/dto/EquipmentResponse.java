package com.gduck0.labgear.dto;

public class EquipmentResponse {
    private int id;
    private String name;

    public EquipmentResponse(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
