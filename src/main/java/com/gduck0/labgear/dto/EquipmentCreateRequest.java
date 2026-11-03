package com.gduck0.labgear.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;

public class EquipmentCreateRequest {

    @Positive private int id;

    @NotBlank
    @Size(max = 100)
    private String name;

    public EquipmentCreateRequest(int id, String name) {
        this.id = id;
        this.name = name;
    }

    protected EquipmentCreateRequest() {}

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
