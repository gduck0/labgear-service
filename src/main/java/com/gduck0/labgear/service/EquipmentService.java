package com.gduck0.labgear.service;

import com.gduck0.labgear.entity.Equipment;
import com.gduck0.labgear.exception.ResourceConflictException;
import com.gduck0.labgear.exception.ResourceNotFoundException;
import com.gduck0.labgear.repository.EquipmentRepository;
import com.gduck0.labgear.repository.ReservationRepository;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EquipmentService {
    private final EquipmentRepository equipmentRepository;
    private final ReservationRepository reservationRepository;
    private final EntityManager entityManager;

    public EquipmentService(
            EquipmentRepository equipmentRepository,
            ReservationRepository reservationRepository,
            EntityManager entityManager) {
        this.equipmentRepository = equipmentRepository;
        this.reservationRepository = reservationRepository;
        this.entityManager = entityManager;
    }

    public List<Equipment> getAllEquipment() {
        return equipmentRepository.findAll();
    }

    @Transactional
    public void createEquipment(Equipment item) {
        if (equipmentRepository.existsById(item.getId())) {
            throw new ResourceConflictException("이미 존재하는 기자재 ID입니다.");
        }
        entityManager.persist(item);
    }

    public Equipment getEquipment(int id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("기자재를 찾을 수 없습니다. ID: " + id));
    }

    @Transactional
    public void deleteEquipment(int id) {
        Equipment item = equipmentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("기자재를 찾을 수 없습니다. ID: " + id));
        if (reservationRepository.existsByEquipment_Id(id)) {
            throw new ResourceConflictException("예약이 존재하는 기자재 ID입니다");
        }
        equipmentRepository.delete(item);
    }

    @Transactional
    public void updateEquipmentName(int id, String name) {
        Equipment item = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("기자재를 찾을 수 없습니다. ID: " + id));
        item.updateName(name);
    }
}
