package com.gduck0.labgear.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;

public class EquipmentCreateRequest {

    @Positive
    private int id;

    @NotBlank
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
