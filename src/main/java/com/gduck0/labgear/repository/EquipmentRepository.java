package com.gduck0.labgear.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gduck0.labgear.entity.Equipment;

public interface EquipmentRepository
        extends JpaRepository<Equipment, Integer> {

}