package com.gduck0.labgear.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EquipmentNameUpdateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    protected EquipmentNameUpdateRequest() {}

    public EquipmentNameUpdateRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
