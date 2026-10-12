package com.gduck0.labgear.dto;

public class EquipmentCreateRequest {
    private int id;
    private String name;

    public EquipmentCreateRequest(int id, String name) {
        this.id = id;
        this.name = name;
    }

    protected EquipmentCreateRequest() {
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
