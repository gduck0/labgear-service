package com.gduck0.labgear.dto;

import jakarta.validation.constraints.NotBlank;

public class EquipmentNameUpdateRequest {

    @NotBlank
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
