package com.gduck0.labgear.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gduck0.labgear.entity.EquipmentItem;
import com.gduck0.labgear.repository.EquipmentRepository;

@Service
public class EquipmentService {
    private EquipmentRepository equipmentRepository;

    public EquipmentService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    public List<EquipmentItem> getAllEquipment() {
        return equipmentRepository.getAll();
    }

    public void addEquipment(EquipmentItem item) {
        equipmentRepository.save(item);
    }

    public Optional<EquipmentItem> getEquipmentById(int id) {
        return equipmentRepository.findById(id);
    }

    public void deleteEquipment(int id) {
        equipmentRepository.deleteById(id);
    }

    @Transactional
    public void changeEquipmentName(int id, String name) {
        EquipmentItem item = equipmentRepository.findById(id).orElseThrow();
        item.changeName(name);
    }
}