package com.gduck0.labgear.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import com.gduck0.labgear.entity.Equipment;
import com.gduck0.labgear.repository.EquipmentRepository;
import com.gduck0.labgear.repository.ReservationRepository;
import com.gduck0.labgear.exception.ResourceConflictException;
import com.gduck0.labgear.exception.ResourceNotFoundException;

@Service
public class EquipmentService {
    private final EquipmentRepository equipmentRepository;
    private final ReservationRepository reservationRepository;
    private final EntityManager entityManager;

    public EquipmentService(EquipmentRepository equipmentRepository, ReservationRepository reservationRepository, EntityManager entityManager) {
        this.equipmentRepository = equipmentRepository;
        this.reservationRepository = reservationRepository;
        this.entityManager = entityManager;
    }

    public List<Equipment> getAllEquipment() {
        return equipmentRepository.findAll();
    }

    @Transactional
    public void createEquipment(Equipment item) {
        if(equipmentRepository.existsById(item.getId())) {
            throw new ResourceConflictException("이미 존재하는 기자재 ID입니다.");
        }
        entityManager.persist(item);
    }

    public Equipment getEquipment(int id) {
        return equipmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("기자재를 찾을 수 없습니다. ID: " + id));
    }

    @Transactional
    public void deleteEquipment(int id) {
        getEquipment(id);
        if(reservationRepository.existsByEquipment_Id(id)) {
            throw new ResourceConflictException("예약이 존재하는 기자재 ID입니다");
        }
        equipmentRepository.deleteById(id);
    }

    @Transactional
    public void updateEquipmentName(int id, String name) {
        Equipment item = equipmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("기자재를 찾을 수 없습니다. ID: " + id));
        item.updateName(name);
    }
}