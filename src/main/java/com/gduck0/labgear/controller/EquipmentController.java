package com.gduck0.labgear.controller;

import java.util.List;
import java.util.ArrayList;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

import com.gduck0.labgear.dto.EquipmentCreateRequest;
import com.gduck0.labgear.dto.EquipmentResponse;
import com.gduck0.labgear.dto.EquipmentNameUpdateRequest;
import com.gduck0.labgear.entity.Equipment;
import com.gduck0.labgear.service.EquipmentService;

import jakarta.validation.Valid;

@RestController
public class EquipmentController {
    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping("/equipment")
    public List<EquipmentResponse> getAllEquipment() {
        List<EquipmentResponse> responses = new ArrayList<>();
        for (Equipment item : equipmentService.getAllEquipment()) {
            EquipmentResponse response = new EquipmentResponse(item.getId(), item.getName());
            responses.add(response);
        }
        return responses;
    }

    @PostMapping("/equipment")
    @ResponseStatus(HttpStatus.CREATED)
    public void createEquipment(@Valid @RequestBody EquipmentCreateRequest request) {
        Equipment item = new Equipment(request.getId(), request.getName());
        equipmentService.createEquipment(item);
    }

    @GetMapping("/equipment/{id}")
    public EquipmentResponse getEquipment(@PathVariable int id) {
        Equipment item = equipmentService.getEquipment(id);
        EquipmentResponse response = new EquipmentResponse(item.getId(), item.getName());
        return response;
    }

    @DeleteMapping("/equipment/{id}")
    public void deleteEquipment(@PathVariable int id) {
        equipmentService.deleteEquipment(id);
    }

    @PatchMapping("/equipment/{id}/name")
    public void updateEquipmentName(@PathVariable int id, @Valid @RequestBody EquipmentNameUpdateRequest request) {
        equipmentService.updateEquipmentName(id, request.getName());
    }
}