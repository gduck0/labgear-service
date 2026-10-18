package com.gduck0.labgear.controller;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;

import com.gduck0.labgear.dto.EquipmentCreateRequest;
import com.gduck0.labgear.dto.EquipmentResponse;
import com.gduck0.labgear.dto.EquipmentNameUpdateRequest;
import com.gduck0.labgear.entity.EquipmentItem;
import com.gduck0.labgear.service.EquipmentService;

import jakarta.validation.Valid;

@RestController
class EquipmentController {
    private EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping("/equipment")
    public List<EquipmentResponse> getAllEquipment() {
        List<EquipmentResponse> responses = new ArrayList<>();
        for (EquipmentItem item : equipmentService.getAllEquipment()) {
            EquipmentResponse response = new EquipmentResponse(item.getId(), item.getName());
            responses.add(response);
        }
        return responses;
    }

    @PostMapping("/equipment")
    public void addEquipment(@Valid @RequestBody EquipmentCreateRequest request) {
        EquipmentItem item = new EquipmentItem(request.getId(), request.getName());
        equipmentService.addEquipment(item);
    }

    @GetMapping("/equipment/{id}")
    public EquipmentResponse getEquipmentById(@PathVariable int id) {
        EquipmentItem item = equipmentService.getEquipmentById(id);
        EquipmentResponse response = new EquipmentResponse(item.getId(), item.getName());
        return response;
    }

    @DeleteMapping("/equipment/{id}")
    public void deleteEquipment(@PathVariable int id) {
        equipmentService.deleteEquipment(id);
    }

    @PatchMapping("/equipment/{id}/name")
    public void changeEquipmentName(@PathVariable int id, @Valid @RequestBody EquipmentNameUpdateRequest request) {
        equipmentService.changeEquipmentName(id, request.getName());
    }
}