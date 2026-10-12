package com.gduck0.labgear.dto;

public class EquipmentNameUpdateRequest {
    private String name;

    protected EquipmentNameUpdateRequest() {
    }

    public EquipmentNameUpdateRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
