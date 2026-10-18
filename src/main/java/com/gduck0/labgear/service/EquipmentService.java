package com.gduck0.labgear.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gduck0.labgear.entity.EquipmentItem;
import com.gduck0.labgear.repository.SpringDataEquipmentRepository;
import com.gduck0.labgear.repository.SpringDataEquipmentReservationRepository;
import com.gduck0.labgear.exception.DuplicateResourceException;
import com.gduck0.labgear.exception.ReservationConflictException;
import com.gduck0.labgear.exception.ResourceNotFoundException;

@Service
public class EquipmentService {
    private SpringDataEquipmentRepository equipmentRepository;
    private SpringDataEquipmentReservationRepository springDataEquipmentReservationRepository;

    public EquipmentService(SpringDataEquipmentRepository equipmentRepository, SpringDataEquipmentReservationRepository springDataEquipmentReservationRepository) {
        this.equipmentRepository = equipmentRepository;
        this.springDataEquipmentReservationRepository = springDataEquipmentReservationRepository;
    }

    public List<EquipmentItem> getAllEquipment() {
        return equipmentRepository.findAll();
    }

    public void addEquipment(EquipmentItem item) {
        if(equipmentRepository.existsById(item.getId())) {
            throw new DuplicateResourceException("이미 존재하는 기자재 ID입니다.");
        }
        equipmentRepository.save(item);
    }

    public EquipmentItem getEquipmentById(int id) {
        return equipmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("기자재를 찾을 수 없습니다. ID: " + id));
    }

    @Transactional
    public void deleteEquipment(int id) {
        getEquipmentById(id);
        if(springDataEquipmentReservationRepository.existsByEquipmentItem_Id(id)) {
            throw new ReservationConflictException("예약이 존재하는 기자재 ID입니다");
        }
        equipmentRepository.deleteById(id);
    }

    @Transactional
    public void changeEquipmentName(int id, String name) {
        EquipmentItem item = equipmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("기자재를 찾을 수 없습니다. ID: " + id));
        item.changeName(name);
    }
}